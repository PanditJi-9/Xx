#!/usr/bin/env python3
"""List GameSpace smali methods that touch startActivity, for launch-path diagnosis.

Usage: python3 tools/re_diagnose_launch.py --decoded <decoded_dir>
"""
import argparse
import os
import re


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--decoded", required=True)
    ap.add_argument("--root", default="cn/nubia/gamelauncher")
    args = ap.parse_args()
    base = os.path.join(args.decoded, "smali", *args.root.split("/"))
    hits = []
    for root, _, files in os.walk(base):
        for f in files:
            if not f.endswith(".smali"):
                continue
            path = os.path.join(root, f)
            try:
                text = open(path, encoding="utf-8", errors="replace").read()
            except OSError:
                continue
            if "startActivity" not in text:
                continue
            methods = [ln.strip() for ln in text.splitlines() if ln.strip().startswith(".method")]
            rel = os.path.relpath(path, args.decoded)
            hits.append((rel, methods))
    print(f"files touching startActivity: {len(hits)}")
    for rel, methods in hits:
        print(f"\n### {rel}")
        for m in methods:
            print("  " + m)


if __name__ == "__main__":
    main()
PY