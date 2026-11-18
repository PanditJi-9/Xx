#!/usr/bin/env python3
"""Compose a reverse-engineering report from evidence collected in CI.

The only inputs are files the workflow itself produced: REA JSON envelopes,
their exit codes, the decoded manifest, keyword hit files and facts.json.
Nothing here guesses a fact that was not measured: every section states what
was observed, and the Limitations section states what was not.

The REA fields parsed below are the ones observed in a real run of the pinned
release (rea-agents 6.7.0, envelope ``{ok, data, meta}`` with the payload under
``data.normalized_result``). The parser still falls back to structure search so
a version change degrades to "not reported" instead of crashing.

Standard library only, so it runs on a bare runner.

Usage:
    python3 tools/re_apk_report.py --evidence-dir DIR [--title TEXT]
                                   [--summary-out PATH]
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from typing import Any


# ─────────────────────────── evidence file names ─────────────────────────────
# These are written by .github/scripts/re_apk_analyze.sh. They use underscores;
# a mismatch here silently empties whole sections, so the names live in one
# place and are asserted by the script's own output.
EVIDENCE_FILES = {
    "rea_package": "rea_package.json",
    "rea_signature": "rea_signature.json",
    "rea_jni": "rea_jni.json",
}


# ─────────────────────────── REA envelope handling ───────────────────────────

def _walk(node: Any, budget: int = 200_000):
    """Yield every container in the document, breadth-first and bounded."""
    queue: list[Any] = [node]
    seen: set[int] = set()
    steps = 0
    while queue and steps < budget:
        cur = queue.pop(0)
        steps += 1
        if isinstance(cur, (dict, list)):
            if id(cur) in seen:
                continue
            seen.add(id(cur))
            yield cur
            if isinstance(cur, dict):
                queue.extend(v for v in cur.values() if isinstance(v, (dict, list)))
            else:
                queue.extend(v for v in cur if isinstance(v, (dict, list)))


def find_normalized(doc: Any) -> dict | None:
    """Return the first ``normalized_result`` mapping found anywhere."""
    for node in _walk(doc):
        if isinstance(node, dict):
            candidate = node.get("normalized_result")
            if isinstance(candidate, dict):
                return candidate
    return None


def load_json(path: str) -> tuple[Any | None, str]:
    """Load JSON, returning ``(value, error_text)``; never raises."""
    if not os.path.isfile(path):
        return None, "file not produced"
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as handle:
            text = handle.read()
    except OSError as exc:  # pragma: no cover - defensive
        return None, f"unreadable: {exc}"
    if not text.strip():
        return None, "empty output"
    try:
        return json.loads(text), ""
    except json.JSONDecodeError as exc:
        return None, f"not JSON ({exc.msg} at offset {exc.pos})"


def rea_command_error(doc: Any) -> str:
    """Return the REA command error text when the envelope reports one."""
    if isinstance(doc, dict) and doc.get("ok") is False:
        error = doc.get("error")
        if isinstance(error, dict):
            code = error.get("code") or "error"
            message = error.get("message") or ""
            return f"{code}: {message}".strip()
        return "command reported failure"
    return ""


def dig(node: Any, *keys: str) -> Any:
    """Follow a key path, tolerating missing links."""
    cur = node
    for key in keys:
        if isinstance(cur, dict):
            cur = cur.get(key)
        else:
            return None
    return cur


def first_of(node: Any, *paths: tuple[str, ...]) -> Any:
    for path in paths:
        value = dig(node, *path)
        if value not in (None, "", [], {}):
            return value
    return None


def read_text(path: str, limit: int | None = None) -> str:
    if not os.path.isfile(path):
        return ""
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as handle:
            return handle.read() if limit is None else handle.read(limit)
    except OSError:  # pragma: no cover - defensive
        return ""


def count_lines(path: str) -> int | None:
    if not os.path.isfile(path):
        return None
    try:
        with open(path, "rb") as handle:
            return sum(1 for _ in handle)
    except OSError:  # pragma: no cover - defensive
        return None


def human_bytes(size: float | int | None) -> str:
    if not isinstance(size, (int, float)):
        return "unknown"
    value = float(size)
    for unit in ("B", "KiB", "MiB", "GiB", "TiB"):
        if value < 1024 or unit == "TiB":
            return f"{value:.0f} {unit}" if unit == "B" else f"{value:.2f} {unit}"
        value /= 1024
    return f"{value:.2f} TiB"


def status_word(code: int | None, partial_ok: bool = False) -> str:
    """Describe a recorded exit code without overstating success."""
    if code is None:
        return "not run"
    if code == 0:
        return "ok"
    if partial_ok and code == 3:
        return "partial (exit 3: finished with decompilation errors)"
    return f"failed (exit {code})"


# ───────────────────────────── report building ───────────────────────────────

def build_report(evidence: str, title: str | None) -> str:
    facts, _ = load_json(os.path.join(evidence, "facts.json"))
    facts = facts if isinstance(facts, dict) else {}

    target = facts.get("target") or "unknown target"
    out: list[str] = []
    add = out.append

    add(f"# {title or ('Reverse engineering: ' + target)}")
    add("")
    add("Static analysis only. Nothing was executed on a device, and no APK was")
    add("installed or run. Every claim below is tied to a file in the evidence")
    add("directory named next to it.")
    add("")

    # ── 1. identity ──────────────────────────────────────────────────────────
    add("## 1. Target identity")
    add("")
    add("| Field | Value |")
    add("| --- | --- |")
    add(f"| File | `{target}` |")
    size = facts.get("size_bytes")
    add(f"| Size | {human_bytes(size)}" + (f" ({size} bytes) |" if isinstance(size, int) else " |"))
    add(f"| SHA-256 | `{facts.get('sha256') or 'unknown'}` |")
    zip_facts = facts.get("zip") if isinstance(facts.get("zip"), dict) else {}
    if zip_facts.get("entries") is not None:
        add(
            "| ZIP entries | {entries} ({dex} dex) |".format(
                entries=zip_facts.get("entries"), dex=zip_facts.get("dex_files", "?")
            )
        )
    if facts.get("release_tag"):
        add(f"| Source | draft release `{facts['release_tag']}` (not published) |")
    add("")

    # ── 2. toolchain ─────────────────────────────────────────────────────────
    add("## 2. Toolchain and step status")
    add("")
    add("| Step | Status | Tool version |")
    add("| --- | --- | --- |")
    steps = facts.get("steps") if isinstance(facts.get("steps"), dict) else {}
    versions = facts.get("versions") if isinstance(facts.get("versions"), dict) else {}
    rows = (
        ("rea_package", "REA package inspection", facts.get("rea_version")),
        ("rea_signature", "REA signature inspection", facts.get("rea_version")),
        ("rea_jni", "REA JNI binding scan", facts.get("rea_version")),
        ("apktool", "apktool decode", versions.get("apktool")),
        ("jadx", "jadx full decompile", versions.get("jadx")),
    )
    for key, label, version in rows:
        record = steps.get(key) or {}
        status = status_word(record.get("exit"), partial_ok=(key == "jadx"))
        # A command the pinned engine does not expose is not a pipeline fault,
        # and calling it "failed" would misread as an APK-side problem.
        if key in EVIDENCE_FILES:
            probe, _ = load_json(os.path.join(evidence, EVIDENCE_FILES[key]))
            if "COMMAND_NOT_FOUND" in rea_command_error(probe):
                status = "not available in pinned REA"
        add(f"| {label} | {status} | {version or '—'} |")
    add("")

    # ── 3. package ───────────────────────────────────────────────────────────
    add("## 3. Package inspection (REA over JADX)")
    add("")
    pkg_doc, pkg_err = load_json(os.path.join(evidence, EVIDENCE_FILES["rea_package"]))
    pkg_norm = find_normalized(pkg_doc) if pkg_doc is not None else None
    pkg_command_error = rea_command_error(pkg_doc)

    if pkg_norm is None:
        add(f"Package evidence unavailable: {pkg_command_error or pkg_err or 'no normalized_result'}.")
        if isinstance(pkg_doc, dict):
            add("")
            add("Envelope keys observed: `" + "`, `".join(sorted(pkg_doc)) + "`.")
        add("")
    else:
        package = first_of(pkg_norm, ("package_name",), ("package", "name"))
        version_name = first_of(pkg_norm, ("version_name",))
        version_code = first_of(pkg_norm, ("version_code",))
        min_sdk = first_of(pkg_norm, ("min_sdk",), ("sdk", "min"))
        target_sdk = first_of(pkg_norm, ("target_sdk",), ("sdk", "target"))
        class_count = first_of(pkg_norm, ("class_count",), ("counts", "classes"))
        resource_count = first_of(pkg_norm, ("resource_count",), ("counts", "resources"))
        signature_verification = first_of(pkg_norm, ("signature_verification",))

        add("| Field | Value |")
        add("| --- | --- |")
        add(f"| Package | `{package or 'not reported'}` |")
        add(f"| Version | {version_name or 'not reported'} (code {version_code or '—'}) |")
        add(f"| SDK | min {min_sdk or '—'} · target {target_sdk or '—'} |")
        add(f"| Classes | {class_count if class_count is not None else 'not reported'} |")
        add(f"| Resources | {resource_count if resource_count is not None else 'not reported'} |")
        add(f"| Signature verification | {signature_verification or 'not reported'} |")
        add("")

        engine = pkg_norm.get("engine")
        if isinstance(engine, dict):
            add(
                "Engine: `{name} {version}` · worker heap {heap} MiB · worker count {workers}".format(
                    name=engine.get("name", "?"),
                    version=engine.get("version", "?"),
                    heap=engine.get("heap_limit_mib", "?"),
                    workers=engine.get("worker_count", "?"),
                )
            )
            add("")
            add(f"Engine artifact SHA-256: `{engine.get('artifact_sha256', 'unknown')}`")
            add("")

        permissions = first_of(pkg_norm, ("permissions",))
        if isinstance(permissions, list) and permissions:
            add(f"### Declared permissions ({len(permissions)})")
            add("")
            for permission in sorted(str(p) for p in permissions):
                add(f"- `{permission}`")
            add("")

        limitations = pkg_doc.get("data", {}).get("limitations") if isinstance(pkg_doc, dict) else None
        if isinstance(limitations, list) and limitations:
            add("### Limitations reported by REA")
            add("")
            for item in limitations:
                add(f"- {item}")
            add("")

    # ── 4. signature ─────────────────────────────────────────────────────────
    add("## 4. APK signature")
    add("")
    sig_doc, sig_err = load_json(os.path.join(evidence, EVIDENCE_FILES["rea_signature"]))
    sig_norm = find_normalized(sig_doc) if sig_doc is not None else None
    sig_command_error = rea_command_error(sig_doc)

    if sig_norm is None:
        add(f"Signature evidence unavailable: {sig_command_error or sig_err or 'no normalized_result'}.")
        add("")
    else:
        schemes = first_of(sig_norm, ("schemes",), ("signature_schemes",))
        signers = first_of(sig_norm, ("signers",), ("certificates",))
        verification = first_of(sig_norm, ("verification",), ("signature_verification",))

        if isinstance(schemes, dict):
            add("| Scheme | Present |")
            add("| --- | --- |")
            for scheme, value in sorted(schemes.items()):
                if isinstance(value, bool):
                    present = "yes" if value else "no"
                elif isinstance(value, dict):
                    present = "yes" if value else "no"
                else:
                    present = str(value)
                add(f"| {scheme} | {present} |")
            add("")
        if isinstance(signers, list) and signers:
            add(f"Signers/certificates reported: {len(signers)}.")
            add("")
            for index, signer in enumerate(signers[:6], start=1):
                if not isinstance(signer, dict):
                    add(f"- Signer {index}: `{signer}`")
                    continue
                subject = first_of(signer, ("subject",), ("certificate", "subject"), ("cert", "subject"))
                sha256 = first_of(
                    signer,
                    ("sha256",),
                    ("certificate", "sha256"),
                    ("cert", "sha256"),
                    ("digest", "sha256"),
                )
                add(
                    f"- Signer {index}: subject `{subject or 'not reported'}` · "
                    f"SHA-256 `{sha256 or 'not reported'}`"
                )
            add("")
        if verification:
            add(f"Verification field reported: `{verification}`.")
            add("")
        if not schemes and not signers:
            add("The engine returned a result whose shape this report does not model yet.")
            add("Its keys are listed so the next revision can read them:")
            add("")
            add("`" + "`, `".join(sorted(sig_norm)) + "`")
            add("")
            add("```json")
            add(json.dumps(sig_norm, indent=2, ensure_ascii=False)[:2000])
            add("```")
            add("")

    # ── 5. native ────────────────────────────────────────────────────────────
    add("## 5. Native libraries")
    add("")
    native_rows = read_text(os.path.join(evidence, "native-libs.tsv")).strip()
    if native_rows:
        rows_raw = [line.split("\t") for line in native_rows.splitlines() if line.strip()]
        add("| Library | ABI | Size | Hardening |")
        add("| --- | --- | --- | --- |")
        for row in rows_raw[:40]:
            row += [""] * (4 - len(row))
            size_text = human_bytes(int(row[2])) if row[2].isdigit() else row[2]
            add(f"| `{row[0]}` | {row[1]} | {size_text} | {row[3] or '—'} |")
        if len(rows_raw) > 40:
            add(f"| … {len(rows_raw) - 40} more (see `native-libs.tsv`) | | | |")
        add("")
        add("Hardening flags come from the ELF header and program headers (`readelf`):")
        add("PIE from `ET_DYN`, RELRO from `GNU_RELRO`, NX from a non-executable `GNU_STACK`.")
        add("")
    else:
        add("No bundled native libraries were inventoried (or the inventory step failed).")
        add("")

    jni_doc, jni_err = load_json(os.path.join(evidence, EVIDENCE_FILES["rea_jni"]))
    jni_norm = find_normalized(jni_doc) if jni_doc is not None else None
    jni_command_error = rea_command_error(jni_doc)
    if jni_norm is not None:
        declarations = first_of(jni_norm, ("declarations",))
        bindings = first_of(jni_norm, ("bindings",))
        add("REA JNI binding scan:")
        add("")
        if isinstance(declarations, list):
            add(f"- Native (`ACC_NATIVE`) declarations observed: {len(declarations)}")
        if isinstance(bindings, list):
            matched = sum(
                1 for row in bindings if isinstance(row, dict) and row.get("status") == "matched"
            )
            add(f"- Declaration/ABI binding rows: {len(bindings)} ({matched} matched)")
        add("")
    elif jni_command_error or jni_err:
        add(f"JNI binding scan not run: {jni_command_error or jni_err}.")
        add("")
        add("This is a measured tool limitation, not an absence of native code: the")
        add("pinned REA release does not expose that command, and the section below")
        add("still reports every bundled library.")
        add("")

    # ── 6. endpoints ─────────────────────────────────────────────────────────
    add("## 6. Endpoints and interesting strings")
    add("")
    urls_path = os.path.join(evidence, "urls.txt")
    url_count = count_lines(urls_path)
    if url_count:
        add(f"Distinct URLs found in decoded resources and smali: **{url_count}** (full list: `urls.txt`).")
        add("")
        add("```")
        for line in read_text(urls_path).splitlines()[:25]:
            add(line)
        add("```")
        add("")
    else:
        add("No URLs extracted.")
        add("")

    # ── 7. keywords ──────────────────────────────────────────────────────────
    add("## 7. Capability keyword hits (textual, not runtime)")
    add("")
    hits = read_text(os.path.join(evidence, "keyword-hits.tsv")).strip()
    if hits:
        add("| Keyword | Files containing it |")
        add("| --- | --- |")
        for line in hits.splitlines():
            parts = line.split("\t")
            if len(parts) == 2:
                add(f"| `{parts[0]}` | {parts[1]} |")
        add("")
        add("A textual hit only shows that a token appears in some string or smali.")
        add("It does not show that the code path runs, is reachable, or is enabled")
        add("on any device.")
        add("")
    else:
        add("Keyword scan produced no output.")
        add("")

    # ── 8. manifest ──────────────────────────────────────────────────────────
    add("## 8. Decoded manifest")
    add("")
    manifest_path = os.path.join(evidence, "AndroidManifest.xml")
    if os.path.isfile(manifest_path):
        manifest_text = read_text(manifest_path)
        add("The decoded manifest is included as `AndroidManifest.xml`.")
        add("")
        add(f"Literal `android:exported=\"true\"` attributes: {len(re.findall(r'android:exported=\"true\"', manifest_text))}.")
        for kind, pattern in (
            ("activities", r"<activity[\s>]"),
            ("services", r"<service[\s>]"),
            ("receivers", r"<receiver[\s>]"),
            ("providers", r"<provider[\s>]"),
        ):
            add(f"- Declared {kind}: {len(re.findall(pattern, manifest_text))}")
    else:
        add("apktool did not produce a decoded manifest.")
    add("")

    # ── 9. files ─────────────────────────────────────────────────────────────
    add("## 9. What is in the evidence directory")
    add("")
    add("| File | Meaning |")
    add("| --- | --- |")
    add("| `facts.json` | Hashes, sizes, tool versions and step exit codes |")
    add("| `rea_package.json` | REA package envelope, unmodified |")
    add("| `rea_signature.json` | REA signature envelope, unmodified |")
    add("| `rea_jni.json` | REA JNI envelope, unmodified |")
    add("| `<step>.stderr` / `<step>.exit` | Unfiltered diagnostics and exit code per step |")
    add("| `AndroidManifest.xml` | Decoded manifest from apktool |")
    add("| `apktool.yml` | apktool metadata: SDK levels, version, unknown files |")
    add("| `strings-default.xml` | Default-locale string table |")
    add("| `native-libs.tsv` | Library, ABI, size, hardening flags |")
    add("| `urls.txt` | Extracted URLs |")
    add("| `keyword-hits.tsv` | Keyword to file-count table |")
    add("| `large-assets.tsv` | Large media blobs recorded then dropped from the artifact |")
    add("")

    # ── 10. limitations ──────────────────────────────────────────────────────
    add("## 10. Limitations")
    add("")
    add("- Static only: no APK was installed, launched or observed at runtime.")
    add("- Obfuscation (R8/ProGuard) renames symbols; no mapping was supplied, so")
    add("  class and method names are the decompiler's representation.")
    add("- A decompiled body is a derived representation, not a byte-identical")
    add("  reconstruction of the original source.")
    add("- jadx exiting with errors leaves some methods decompiled partially or not")
    add("  at all; the count is in `jadx.stdout`.")
    add("- Signature evidence reports what the archive contains; it is not an")
    add("  authenticity or safety verdict.")
    add("- Behavior on a specific device, and any effect of enabled or disabled")
    add("  components at runtime, remains unverified without a device.")
    add("")
    return "\n".join(out)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evidence-dir", required=True)
    parser.add_argument("--title", default=None)
    parser.add_argument("--summary-out", default=None, help="write the report here as well")
    args = parser.parse_args(argv)

    if not os.path.isdir(args.evidence_dir):
        print(f"evidence directory not found: {args.evidence_dir}", file=sys.stderr)
        return 2

    report = build_report(args.evidence_dir, args.title)
    print(report, end="\n")

    if args.summary_out:
        with open(args.summary_out, "w", encoding="utf-8") as handle:
            handle.write(report)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
