#!/usr/bin/env python3
"""Cross-app dependency graph straight from the APKs' DEX string pools.

Why this exists: the ecosystem has 23 apps with hidden coupling, and decoding all of
them to smali to find who calls whom is days of work. The DEX string pool already
contains every type descriptor the app can touch, so scanning strings answers the
question that decides the architecture: which of the 23 packages does each app
reference, and how many sites?

What it does NOT claim: a referenced package is a *dependency*, not a working
channel — a reference can be inside dead code, a string constant or a resource name.
Ranking by count is the honest use; "it will work" is a device question.

Usage:
    python3 tools/apk_xref.py apps/ --json docs/audit/apk-xref.json \
        --markdown docs/audit/APP_DEPENDENCY_GRAPH.md
    python3 tools/apk_xref.py --self-test
"""

from __future__ import annotations

import argparse
import json
import os
import struct
import sys
import zipfile

VENDOR_PREFIXES = ("com/zte/", "cn/zte/", "com/nubia/", "cn/nubia/", "com/redmagic/")


def dex_strings(data: bytes) -> list[str]:
    if data[:4] != b"dex\n":
        return []
    string_ids_size, string_ids_off = struct.unpack_from("<II", data, 0x38)
    out: list[str] = []
    for i in range(string_ids_size):
        off = struct.unpack_from("<I", data, string_ids_off + 4 * i)[0]
        pos = off
        # uleb128 utf16 length, then NUL-terminated MUTF-8
        while pos < len(data) and data[pos] & 0x80:
            pos += 1
        pos += 1
        end = data.find(b"\x00", pos)
        if end < 0:
            continue
        out.append(data[pos:end].decode("utf-8", "replace"))
    return out


def dex_defined_classes(data: bytes) -> list[str]:
    """Type descriptors this dex *defines* (class_defs -> type_ids -> string_ids)."""
    if data[:4] != b"dex\n":
        return []
    n_types, types_off = struct.unpack_from("<II", data, 0x40)
    n_defs, defs_off = struct.unpack_from("<II", data, 0x60)
    if not n_types or not n_defs:
        return []
    type_string_idx = [struct.unpack_from("<I", data, types_off + 4 * i)[0] for i in range(n_types)]
    out = []
    for i in range(n_defs):
        class_idx = struct.unpack_from("<I", data, defs_off + 32 * i)[0]
        if class_idx < len(type_string_idx):
            out.append(type_string_idx[class_idx])
    # resolve through the string pool in one pass
    strings = dex_strings(data)
    return [strings[i] for i in out if i < len(strings)]


def apk_entries(path: str) -> tuple[list[str], set[str], dict[str, int]]:
    """Return (dex strings, defined type descriptors, zip facts) for one APK."""
    strings: list[str] = []
    defined: set[str] = set()
    with zipfile.ZipFile(path) as zf:
        dex = [n for n in zf.namelist() if n.startswith("classes") and n.endswith(".dex")]
        for name in sorted(dex):
            data = zf.read(name)
            strings.extend(dex_strings(data))
            defined.update(dex_defined_classes(data))
    return strings, defined, {"dex": len(dex)}


def count_refs(strings: list[str], targets: list[str]) -> dict[str, int]:
    """Count type descriptors starting with `L<pkg>/` for each target package."""
    out = {t: 0 for t in targets}
    for s in strings:
        if len(s) < 4 or s[0] != "L" or not s.endswith(";"):
            # cheap filter: class descriptors only, but also count slash paths
            if "/" not in s:
                continue
        for t in targets:
            if s.startswith("L" + t.replace(".", "/") + "/") or s.startswith(t.replace(".", "/") + "/"):
                out[t] += 1
                break
    return out


def vendor_counts(strings: list[str]) -> dict[str, int]:
    out = {p: 0 for p in VENDOR_PREFIXES}
    for s in strings:
        for p in VENDOR_PREFIXES:
            if p in s:
                out[p] += 1
                break
    return out


def self_test() -> int:
    checks = []
    targets = ["com.example.a", "cn.nubia.gameassist"]
    strings = [
        "Lcom/example/a/Foo;",
        "Lcom/example/a/Bar;",
        "Lcn/nubia/gameassist/Baz;",
        "Landroid/app/Activity;",
        "com/example/a",
    ]
    got = count_refs(strings, targets)
    checks.append(("self refs", got["com.example.a"], 2))
    checks.append(("cross refs", got["cn.nubia.gameassist"], 1))
    v = vendor_counts(["Lcn/nubia/gameassist/Baz;", "Lcom/zte/x/Y;", "Landroid/x;"])
    checks.append(("vendor cn/nubia", v["cn/nubia/"], 1))
    checks.append(("vendor com/zte", v["com/zte/"], 1))
    checks.append(("no false positive", vendor_counts(["Landroid/x;"])["com/zte/"], 0))
    bad = [(n, g, e) for n, g, e in checks if g != e]
    for n, g, e in bad:
        print(f"self-test FAIL {n}: got {g} want {e}")
    if not bad:
        print(f"self-test OK: {len(checks)} reference-counting cases")
    return 1 if bad else 0


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("root", nargs="?")
    ap.add_argument("--json")
    ap.add_argument("--markdown")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args(argv)
    if args.self_test:
        return self_test()
    if not args.root:
        print("need a directory of APKs", file=sys.stderr)
        return 2

    apks = []
    for dirpath, _d, files in os.walk(args.root):
        for n in sorted(files):
            if n.endswith(".apk") and "NubiaRedmagickyi_RedMagic" not in n:
                apks.append(os.path.join(dirpath, n))
    apks.sort()
    if not apks:
        print("no APKs", file=sys.stderr)
        return 2

    strings_by_apk: dict[str, list[str]] = {}
    defined_by_apk: dict[str, set[str]] = {}
    for p in apks:
        strings, defined, _facts = apk_entries(p)
        name = os.path.basename(p)
        strings_by_apk[name] = strings
        defined_by_apk[name] = defined

    # packages from each APK's own descriptors: the dominant L<path>/ prefix that
    # looks like a package root and is not a framework namespace.
    packages: dict[str, str] = {}
    for name, strings in strings_by_apk.items():
        counts: dict[str, int] = {}
        for s in strings:
            if not s.startswith("L") or not s.endswith(";"):
                continue
            path = s[1:-1]
            if "/" not in path:
                continue
            if path.startswith(("android/", "java/", "javax/", "kotlin/", "androidx/", "kotlinx/", "org/")):
                continue
            head = "/".join(path.split("/")[:2])
            counts[head] = counts.get(head, 0) + 1
        if counts:
            packages[name] = max(counts.items(), key=lambda kv: kv[1])[0]
    # prefer package names measured from the manifest when available
    inv_json = os.path.join(os.path.dirname(os.path.abspath(args.root.rstrip("/"))) or ".", "docs/audit/apk-inventory.json")
    if os.path.exists(inv_json):
        with open(inv_json, encoding="utf-8") as fh:
            inv = json.load(fh)
        for row in inv.get("apks", []):
            if row.get("package"):
                packages[row["file"]] = row["package"]

    # A package name is not the code namespace: NubiaGameHelperModule lives in
    # cn/nubia/gamehelper/*, so matching on the package alone reports zero coupling.
    # Measure each app's own dominant namespaces and match against those too.
    FRAMEWORK = ("android/", "androidx/", "java/", "javax/", "kotlin/", "kotlinx/",
                 "org/", "com/google/", "com/android/", "dalvik/")
    namespaces: dict[str, list[str]] = {}
    for name, strings in strings_by_apk.items():
        counts: dict[str, int] = {}
        for s in strings:
            if not s.startswith("L") or not s.endswith(";"):
                continue
            path = s[1:-1]
            if "/" not in path or path.startswith(FRAMEWORK):
                continue
            parts = path.split("/")
            # 3 segments: at 2, every cn/nubia app collapses into one bucket and the
            # matrix claims coupling that is really just a shared org prefix.
            key = "/".join(parts[:3])
            counts[key] = counts.get(key, 0) + 1
        namespaces[name] = [k for k, v in sorted(counts.items(), key=lambda kv: -kv[1])[:4] if v >= 25]

    # Keep only namespaces owned by exactly one app: a prefix two apps share
    # (com/zte/common) is not evidence that one depends on the other.
    owners: dict[str, list[str]] = {}
    for name, nss in namespaces.items():
        for ns in nss:
            owners.setdefault(ns, []).append(name)
    unique = {name: [ns for ns in nss if len(owners[ns]) == 1] for name, nss in namespaces.items()}

    def keys_for(name: str) -> list[str]:
        keys = []
        pkg = packages.get(name, "")
        if pkg:
            keys.append(pkg.replace(".", "/") + "/")
        keys += [ns + "/" for ns in unique.get(name, [])]
        return keys

    matrix: dict[str, dict[str, int]] = {}
    vendor: dict[str, dict[str, int]] = {}
    for name, strings in strings_by_apk.items():
        row = {}
        own = defined_by_apk.get(name, set())
        for other, keys in ((n, keys_for(n)) for n in sorted(strings_by_apk)):
            hit = 0
            for s in strings:
                if s in own:
                    continue  # defining a type is not depending on it
                if any(s.startswith("L" + k) or s.startswith(k) for k in keys):
                    hit += 1
            row[other] = hit
        matrix[name] = row
        vendor[name] = vendor_counts(strings)

    report = {"packages": packages, "namespaces": unique, "matrix": matrix,
              "definedCounts": {n: len(v) for n, v in defined_by_apk.items()},
              "vendor": vendor, "sizes": {n: len(s) for n, s in strings_by_apk.items()}}
    if args.json:
        os.makedirs(os.path.dirname(os.path.abspath(args.json)), exist_ok=True)
        with open(args.json, "w", encoding="utf-8") as fh:
            json.dump(report, fh, ensure_ascii=False, indent=1)
        print(f"json: {args.json}")

    if args.markdown:
        os.makedirs(os.path.dirname(os.path.abspath(args.markdown)), exist_ok=True)
        own = {name.replace(".apk", ""): pkg for name, pkg in packages.items()}
        lines = [
            "# APP_DEPENDENCY_GRAPH — measured from DEX string pools",
            "",
            "Generated by `tools/apk_xref.py`. A count is the number of distinct type",
            "descriptors/strings in the app's DEX that name the other package. A reference is a",
            "dependency, not a working channel.",
            "",
            "## Packages (measured)",
            "",
            "| APK | package |",
            "| --- | --- |",
        ]
        for name in sorted(packages):
            lines.append(f"| {name} | `{packages[name]}` |")
        lines += ["", "## Who references whom", "", "Rows reference columns.", ""]
        cols = sorted(matrix)
        labels = {}
        for name in cols:
            pkg = packages.get(name, "?")
            base = ".".join(pkg.split(".")[-2:])
            labels[name] = base
        seen: dict[str, int] = {}
        for name in cols:
            lab = labels[name]
            if lab in seen:
                seen[lab] += 1
                labels[name] = f"{lab}~{seen[lab]}"
            else:
                seen[lab] = 1
        lines.append("| app | " + " | ".join(labels[c] for c in cols) + " |")
        lines.append("| --- | " + " | ".join("---" for _ in cols) + " |")
        for name in sorted(matrix):
            cells = [str(matrix[name][c]) if matrix[name][c] else "·" for c in cols]
            lines.append(f"| {name.replace('.apk', '')} | " + " | ".join(cells) + " |")
        lines += ["", "## Vendor coupling (strings naming a vendor namespace)", "",
                  "| app | " + " | ".join(VENDOR_PREFIXES) + " |",
                  "| --- | " + " | ".join("---" for _ in VENDOR_PREFIXES) + " |"]
        for name in sorted(vendor):
            lines.append(f"| {name.replace('.apk', '')} | " + " | ".join(str(vendor[name][p]) for p in VENDOR_PREFIXES) + " |")
        with open(args.markdown, "w", encoding="utf-8") as fh:
            fh.write("\n".join(lines) + "\n")
        print(f"markdown: {args.markdown}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
