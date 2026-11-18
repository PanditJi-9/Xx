#!/usr/bin/env bash
# Static reverse engineering for one APK target.
#
# Runs on a GitHub runner only (AGENTS.md §0.10): this machine never builds,
# decompiles or executes an APK. Every step here reads bytes; no step installs,
# launches or observes the application.
#
# Contract:
#   * Every tool's exit code is written to <evidence>/<step>.exit and its
#     unfiltered stderr to <evidence>/<step>.stderr. A failure is recorded, not
#     hidden: the caller decides what a nonzero code means.
#   * Exit status of this script is 0 whenever the recording itself worked. The
#     workflow fails the job explicitly on a failed core inspection.
#
# Usage: re_apk_analyze.sh EVIDENCE_DIR WORK_DIR TARGET_ASSET RELEASE_TAG

set -uo pipefail

EVIDENCE="${1:?evidence directory}"
WORK="${2:?work directory}"
TARGET="${3:?target asset name}"
RELEASE_TAG="${4:?release tag}"

APK="${WORK}/input/${TARGET}"
DECODE="${WORK}/decode"
JAVA_OUT="${WORK}/jadx"

mkdir -p "$EVIDENCE" "$WORK/input"

if [ ! -f "$APK" ]; then
  echo "::error::target APK not found at ${APK}"
  exit 2
fi

record() { # record STEP STATUS
  printf '%s\n' "$2" > "${EVIDENCE}/$1.exit"
}

explain() { # explain STEP STATUS STARTED_AT
  echo "→ $1 exit=$2 elapsed=$((SECONDS - $3))s"
}

# ── REA: one process per command, complete envelope retained ──────────────────
rea_step() { # rea_step STEP_NAME [rea args...]
  local name="$1"
  shift
  echo "::group::REA ${name}"
  local started=$SECONDS
  rea "$@" --json --full-output \
    > "${EVIDENCE}/${name}.json" 2> "${EVIDENCE}/${name}.stderr"
  local status=$?
  record "$name" "$status"
  explain "$name" "$status" "$started"
  echo "::endgroup::"
  return 0
}

# ── apktool: decoded manifest, resources and smali ───────────────────────────
apktool_step() {
  echo "::group::apktool decode"
  local started=$SECONDS
  rm -rf "$DECODE"
  apktool d --force --output "$DECODE" "$APK" \
    > "${EVIDENCE}/apktool.stdout" 2> "${EVIDENCE}/apktool.stderr"
  local status=$?
  record apktool "$status"
  explain apktool "$status" "$started"
  if [ -f "${DECODE}/AndroidManifest.xml" ]; then
    cp "${DECODE}/AndroidManifest.xml" "${EVIDENCE}/AndroidManifest.xml"
  fi
  if [ -f "${DECODE}/apktool.yml" ]; then
    cp "${DECODE}/apktool.yml" "${EVIDENCE}/apktool.yml"
  fi
  if [ -f "${DECODE}/res/values/strings.xml" ]; then
    cp "${DECODE}/res/values/strings.xml" "${EVIDENCE}/strings-default.xml"
  fi
  echo "::endgroup::"
  return 0
}

# ── jadx: full Java source, resources skipped (apktool owns those) ───────────
jadx_step() {
  echo "::group::jadx full decompile"
  local started=$SECONDS
  rm -rf "$JAVA_OUT"
  JAVA_OPTS="-Xmx8g" jadx --no-res -j "$(nproc)" -d "$JAVA_OUT" "$APK" \
    > "${EVIDENCE}/jadx.stdout" 2> "${EVIDENCE}/jadx.stderr"
  local status=$?
  record jadx "$status"
  explain jadx "$status" "$started"
  echo "::endgroup::"
  return 0
}

# ── Native inventory: ELF header and program-header facts per library ────────
native_inventory() {
  echo "::group::native library inventory"
  python3 - "$DECODE" "${EVIDENCE}/native-libs.tsv" <<'PY'
import glob
import os
import re
import subprocess
import sys

root, out = sys.argv[1], sys.argv[2]
rows = []
for path in sorted(glob.glob(os.path.join(root, "**", "*.so"), recursive=True)):
    rel = os.path.relpath(path, root)
    abi = os.path.basename(os.path.dirname(path))
    size = os.path.getsize(path)
    try:
        header = subprocess.run(
            ["readelf", "-hW", path], capture_output=True, text=True, timeout=120
        ).stdout
        programs = subprocess.run(
            ["readelf", "-lW", path], capture_output=True, text=True, timeout=120
        ).stdout
    except Exception:
        header = programs = ""
    kind = re.search(r"Type:\s+(\w+)", header)
    flags = ["PIE" if kind and kind.group(1) == "DYN" else "no-PIE"]
    flags.append("RELRO" if "GNU_RELRO" in programs else "no-RELRO")
    stack = [line for line in programs.splitlines() if "GNU_STACK" in line]
    if not stack:
        flags.append("NX-unknown")
    elif re.search(r"\bRWE\b", stack[0]):
        flags.append("no-NX")
    else:
        flags.append("NX")
    rows.append((rel, abi, str(size), "+".join(flags)))

with open(out, "w", encoding="utf-8") as handle:
    for row in rows:
        handle.write("\t".join(row) + "\n")
print(f"{len(rows)} native libraries inventoried")
PY
  echo "::endgroup::"
  return 0
}

# ── Textual signals: URLs and capability keywords (not runtime facts) ────────
text_signals() {
  echo "::group::textual signals"
  if [ -d "$DECODE" ]; then
    grep -rhoE 'https?://[A-Za-z0-9._~:/?#@!$&*+,;=%-]+' "$DECODE" 2>/dev/null \
      | awk '!seen[$0]++' | head -5000 > "${EVIDENCE}/urls.txt"

    : > "${EVIDENCE}/keyword-hits.tsv"
    while IFS= read -r keyword; do
      [ -n "$keyword" ] || continue
      count=$(grep -rl --include='*.smali' -F -- "$keyword" "$DECODE" 2>/dev/null | wc -l)
      printf '%s\t%s\n' "$keyword" "$count" >> "${EVIDENCE}/keyword-hits.tsv"
    done <<'KEYS'
SYSTEM_ALERT_WINDOW
PACKAGE_USAGE_STATS
dumpsys
SurfaceFlinger
thermal
/sys/class/thermal
/proc/
Settings.Global
broadcast
gpu
fps
KEYS
    echo "URLs: $(wc -l < "${EVIDENCE}/urls.txt") · keyword rows: $(wc -l < "${EVIDENCE}/keyword-hits.tsv")"
  else
    echo "apktool produced no tree; textual signals skipped"
  fi
  echo "::endgroup::"
  return 0
}

# ── Keep the artifact reviewable: record then drop large non-native blobs ────
trim_large_media() {
  if [ ! -d "$DECODE" ]; then
    return 0
  fi
  find "$DECODE" -type f -size +4M ! -name '*.so' -printf '%s\t%P\n' \
    | sort -rn > "${EVIDENCE}/large-assets.tsv"
  find "$DECODE" -type f -size +4M ! -name '*.so' -delete
  echo "large media blobs recorded and dropped: $(wc -l < "${EVIDENCE}/large-assets.tsv")"
}

# ── Facts: hashes, sizes, tool versions and every recorded exit code ─────────
write_facts() {
  echo "::group::facts"
  python3 - "$EVIDENCE" "$TARGET" "$RELEASE_TAG" "$APK" <<'PY'
import hashlib
import json
import os
import subprocess
import sys
import zipfile

evidence, target, tag, apk = sys.argv[1:5]


def version(command):
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=180)
        text = (result.stdout or result.stderr).strip().splitlines()
        return text[0].strip() if text else "unknown"
    except Exception:
        return "unknown"


def exit_of(name):
    path = os.path.join(evidence, f"{name}.exit")
    try:
        with open(path, "r", encoding="utf-8") as handle:
            return int(handle.read().strip())
    except Exception:
        return None


digest = hashlib.sha256()
with open(apk, "rb") as handle:
    for chunk in iter(lambda: handle.read(1 << 20), b""):
        digest.update(chunk)

zip_facts = {"entries": None, "dex_files": None}
try:
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
    zip_facts = {
        "entries": len(names),
        "dex_files": sum(1 for n in names if n.startswith("classes") and n.endswith(".dex")),
    }
except Exception:
    pass

facts = {
    "target": target,
    "release_tag": tag,
    "size_bytes": os.path.getsize(apk),
    "sha256": digest.hexdigest(),
    "zip": zip_facts,
    "rea_version": version(["rea", "--version"]),
    "jadx_version": version(["jadx", "--version"]),
    "versions": {
        "rea_package": "",
        "rea_signature": "",
        "rea_jni": "",
        "apktool": version(["apktool", "--version"]),
        "jadx": version(["jadx", "--version"]),
    },
    "steps": {
        name: {"exit": exit_of(name)}
        for name in ("rea_package", "rea_signature", "rea_jni", "apktool", "jadx")
    },
}

with open(os.path.join(evidence, "facts.json"), "w", encoding="utf-8") as handle:
    json.dump(facts, handle, indent=2, sort_keys=True)
print(json.dumps(facts, indent=2, sort_keys=True))
PY
  echo "::endgroup::"
  return 0
}

# ── Pipeline ────────────────────────────────────────────────────────────────
echo "target: ${TARGET}"
echo "sha256 will be computed after analysis; size $(stat -c %s "$APK") bytes"

rea_step rea_package inspect-android-package "$APK"
rea_step rea_signature inspect-apk-signature "$APK"
rea_step rea_jni inspect-android-jni-bindings "$APK"

apktool_step
jadx_step
native_inventory
text_signals
trim_large_media
write_facts

echo "pipeline finished"
exit 0
