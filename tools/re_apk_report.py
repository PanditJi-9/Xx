#!/usr/bin/env python3
"""Compose a reverse-engineering report from evidence collected in CI.

The only inputs are files the workflow itself produced: REA JSON envelopes,
their exit codes, the decoded manifest, keyword hit files and facts.json.
Nothing here guesses a fact that was not measured: every section states what
was observed, and the Limitations section states what was not.

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


# ─────────────────────────── REA envelope handling ───────────────────────────
# REA's CLI envelope shape is version-dependent, so it is located by structure
# rather than by a hardcoded path. If nothing matches, the report says so and
# prints the keys it did see instead of inventing a result.

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


def read_int(path: str) -> int | None:
    text = read_text(path).strip()
    try:
        return int(text)
    except (TypeError, ValueError):
        return None


def count_lines(path: str) -> int | None:
    if not os.path.isfile(path):
        return None
    try:
        with open(path, "rb") as handle:
            return sum(1 for _ in handle)
    except OSError:  # pragma: no cover - defensive
        return None


def human_bytes(size: float | int | None) -> str:
    if size is None:
        return "unknown"
    value = float(size)
    for unit in ("B", "KiB", "MiB", "GiB", "TiB"):
        if value < 1024 or unit == "TiB":
            return f"{value:.0f} {unit}" if unit == "B" else f"{value:.2f} {unit}"
        value /= 1024
    return f"{value:.2f} TiB"


# ───────────────────────────── report building ───────────────────────────────

STATUS_MARK = {0: "ok", None: "not run"}


def status_word(code: int | None) -> str:
    if code is None:
        return "not run"
    return "ok" if code == 0 else f"failed (exit {code})"


def build_report(evidence: str, title: str | None) -> str:
    facts, facts_error = load_json(os.path.join(evidence, "facts.json"))
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

    # ── target identity ──────────────────────────────────────────────────────
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
                entries=zip_facts.get("entries"),
                dex=zip_facts.get("dex_files", "?"),
            )
        )
    if facts.get("release_tag"):
        add(f"| Source | draft release `{facts['release_tag']}` (not published) |")
    add("")

    # ── toolchain ────────────────────────────────────────────────────────────
    add("## 2. Toolchain and step status")
    add("")
    add("| Step | Status | Version reported |")
    add("| --- | --- | --- |")
    steps = facts.get("steps") if isinstance(facts.get("steps"), dict) else {}
    versions = facts.get("versions") if isinstance(facts.get("versions"), dict) else {}
    for step_name in ("rea_package", "rea_signature", "rea_jni", "apktool", "jadx"):
        record = steps.get(step_name) or {}
        add(
            "| {name} | {status} | {version} |".format(
                name=step_name,
                status=status_word(record.get("exit")),
                version=versions.get(step_name, "—"),
            )
        )
    add("")
    if facts.get("jadx_version"):
        add(f"- Full-decompile engine: {facts['jadx_version']}")
    if facts.get("rea_version"):
        add(f"- REA: {facts['rea_version']}")
    add("")

    # ── REA package ──────────────────────────────────────────────────────────
    add("## 3. Package inspection (REA / JADX)")
    add("")
    pkg_doc, pkg_err = load_json(os.path.join(evidence, "rea-package.json"))
    normalized = find_normalized(pkg_doc) if pkg_doc is not None else None

    if normalized is None:
        add(f"Package evidence unavailable: {pkg_err or 'no normalized_result found in envelope'}.")
        if isinstance(pkg_doc, dict):
            add("")
            add("Envelope keys observed (for follow-up): `" + "`, `".join(sorted(pkg_doc)) + "`.")
        add("")
    else:
        package = first_of(
            normalized,
            ("package", "name"),
            ("package_name",),
            ("package",),
            ("summary", "package_name"),
            ("identity", "package_name"),
        )
        version_name = first_of(
            normalized,
            ("package", "version_name"),
            ("version_name",),
            ("summary", "version_name"),
        )
        version_code = first_of(
            normalized,
            ("package", "version_code"),
            ("version_code",),
            ("summary", "version_code"),
        )
        min_sdk = first_of(normalized, ("sdk", "min"), ("min_sdk",), ("summary", "min_sdk"))
        target_sdk = first_of(
            normalized, ("sdk", "target"), ("target_sdk",), ("summary", "target_sdk")
        )
        class_count = first_of(
            normalized,
            ("counts", "classes"),
            ("class_count",),
            ("summary", "class_count"),
        )
        resource_count = first_of(
            normalized,
            ("counts", "resources"),
            ("resource_count",),
            ("summary", "resource_count"),
        )

        add("| Field | Value |")
        add("| --- | --- |")
        add(f"| Package | `{package or 'not reported'}` |")
        add(f"| Version | {version_name or 'not reported'} ({version_code or '—'}) |")
        add(f"| SDK | min {min_sdk or '—'} · target {target_sdk or '—'} |")
        add(f"| Classes | {class_count if class_count is not None else 'not reported'} |")
        add(
            f"| Resources | {resource_count if resource_count is not None else 'not reported'} |"
        )
        add("")

        permissions = first_of(normalized, ("permissions",), ("summary", "permissions"))
        if isinstance(permissions, list) and permissions:
            add(f"### Declared permissions ({len(permissions)})")
            add("")
            for permission in sorted(str(p) for p in permissions):
                add(f"- `{permission}`")
            add("")

        # Component inventory: only literal facts, no inference about runtime.
        components = first_of(
            normalized, ("components",), ("inventory", "components"), ("summary", "components")
        )
        if isinstance(components, dict):
            add("### Declared components")
            add("")
            add("| Kind | Declared | Explicitly exported |")
            add("| --- | --- | --- |")
            for kind in ("activities", "activity_aliases", "services", "receivers", "providers"):
                rows = components.get(kind)
                if not isinstance(rows, list):
                    continue
                exported = sum(1 for row in rows if isinstance(row, dict) and row.get("exported") is True)
                add(f"| {kind} | {len(rows)} | {exported} |")
            add("")
            coverage = components.get("declaration_coverage") or components.get("status")
            if coverage:
                add(f"Component inventory coverage as reported by REA: `{coverage}`.")
                add("")

    # ── signature ────────────────────────────────────────────────────────────
    add("## 4. APK signature")
    add("")
    sig_doc, sig_err = load_json(os.path.join(evidence, "rea-signature.json"))
    sig_norm = find_normalized(sig_doc) if sig_doc is not None else None
    if sig_norm is None:
        add(f"Signature evidence unavailable: {sig_err or 'no normalized_result found in envelope'}.")
        add("")
    else:
        schemes = first_of(sig_norm, ("schemes",), ("signature_schemes",))
        signers = first_of(sig_norm, ("signers",), ("certificates",))
        if isinstance(schemes, dict):
            add("| Scheme | Present |")
            add("| --- | --- |")
            for scheme, value in sorted(schemes.items()):
                present = value if isinstance(value, bool) else bool(value)
                add(f"| {scheme} | {'yes' if present else 'no'} |")
            add("")
        if isinstance(signers, list) and signers:
            add(f"Signers/certificates reported: {len(signers)}.")
            for index, signer in enumerate(signers[:4], start=1):
                if not isinstance(signer, dict):
                    continue
                subject = first_of(
                    signer,
                    ("subject",),
                    ("certificate", "subject"),
                    ("cert", "subject"),
                )
                sha256 = first_of(
                    signer,
                    ("sha256",),
                    ("certificate", "sha256"),
                    ("cert", "sha256"),
                    ("digest", "sha256"),
                )
                add(f"- Signer {index}: subject `{subject or 'not reported'}` · SHA-256 `{sha256 or 'not reported'}`")
            add("")
        if sig_norm.get("verification"):
            add(f"Verification field reported: `{sig_norm['verification']}`.")
            add("")

    # ── native libraries ─────────────────────────────────────────────────────
    add("## 5. Native libraries")
    add("")
    native_rows = read_text(os.path.join(evidence, "native-libs.tsv")).strip()
    jni_doc, jni_err = load_json(os.path.join(evidence, "rea-jni.json"))
    jni_norm = find_normalized(jni_doc) if jni_doc is not None else None

    if native_rows:
        rows = [line.split("\t") for line in native_rows.splitlines() if line.strip()]
        add(f"| Library | ABI | Size | Hardening |")
        add("| --- | --- | --- | --- |")
        for row in rows[:40]:
            row += [""] * (4 - len(row))
            add(f"| `{row[0]}` | {row[1]} | {human_bytes(int(row[2])) if row[2].isdigit() else row[2]} | {row[3] or '—'} |")
        if len(rows) > 40:
            add(f"| … {len(rows) - 40} more (see native-libs.tsv) | | | |")
        add("")
    else:
        add("No bundled native libraries were inventoried (or the inventory step failed).")
        add("")

    if jni_norm is not None:
        inventory = first_of(jni_norm, ("native_inventory",), ("library_inventory",))
        bindings = first_of(jni_norm, ("bindings",), ("summary", "bindings"))
        declarations = first_of(jni_norm, ("declarations",), ("summary", "declarations"))
        add("REA JNI binding scan:")
        add("")
        if isinstance(declarations, list):
            add(f"- Native (`ACC_NATIVE`) declarations observed: {len(declarations)}")
        if isinstance(bindings, list):
            matched = sum(
                1 for row in bindings if isinstance(row, dict) and row.get("status") == "matched"
            )
            add(f"- Declaration/ABI binding rows: {len(bindings)} ({matched} matched)")
        if isinstance(inventory, dict) and inventory.get("manifest_splits") is not None:
            add(f"- Manifest split requirement: `{inventory['manifest_splits']}`")
        add("")
    elif jni_err:
        add(f"JNI binding evidence unavailable: {jni_err}.")
        add("")

    # ── strings / endpoints ──────────────────────────────────────────────────
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

    # ── keyword hits ─────────────────────────────────────────────────────────
    add("## 7. Capability keyword hits (textual, not runtime)")
    add("")
    hits_path = os.path.join(evidence, "keyword-hits.tsv")
    hits = read_text(hits_path).strip()
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

    # ── decoded manifest ─────────────────────────────────────────────────────
    add("## 8. Decoded manifest")
    add("")
    manifest_path = os.path.join(evidence, "AndroidManifest.xml")
    if os.path.isfile(manifest_path):
        add("The decoded manifest is included as `AndroidManifest.xml`.")
        exported = re.findall(r'android:exported="true"', read_text(manifest_path))
        add(f"Literal `android:exported=\"true\"` attributes: {len(exported)}.")
    else:
        add("apktool did not produce a decoded manifest.")
    add("")

    # ── artifacts ────────────────────────────────────────────────────────────
    add("## 9. What is in the evidence directory")
    add("")
    add("| File | Meaning |")
    add("| --- | --- |")
    add("| `facts.json` | Hashes, sizes, tool versions and step exit codes |")
    add("| `rea-package.json` | REA package inspection envelope (unmodified) |")
    add("| `rea-signature.json` | REA APK-signature envelope (unmodified) |")
    add("| `rea-jni.json` | REA JNI binding envelope (unmodified) |")
    add("| `*.stderr` | Unfiltered diagnostics for the matching step |")
    add("| `*.exit` | Exit code for the matching step |")
    add("| `AndroidManifest.xml` | Decoded manifest from apktool |")
    add("| `native-libs.tsv` | Library, ABI, size, hardening flags |")
    add("| `urls.txt` | Extracted URLs |")
    add("| `keyword-hits.tsv` | Keyword to file-count table |")
    add("")

    # ── limitations ──────────────────────────────────────────────────────────
    add("## 10. Limitations")
    add("")
    add("- Static only: no APK was installed, launched or observed at runtime.")
    add("- Obfuscation (R8/ProGuard) renames symbols; an R8 mapping was not supplied,")
    add("  so class and method names are the decompiler's representation.")
    add("- A decompiled body is a derived representation, not a byte-identical")
    add("  reconstruction of the original source.")
    add("- Signature evidence reports what the archive contains; it is not a")
    add("  authenticity or safety verdict.")
    add("- Behavior on a specific device, and any effect of enabled/disabled")
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
