#!/usr/bin/env python3
"""Neutralize one smali method that cannot run without a platform permission.

The measured crash on a non-Nubia device was:

    java.lang.SecurityException: Permission denial, must have one of:
        [android.permission.WRITE_SECURE_SETTINGS]
        at cn.nubia.common.util.CommonUtil.notifyHandheldModeChanged(CommonUtil.java:203)
        at cn.nubia.common.CommonApplication.onCreate(CommonApplication.java:21)

The write it performs can never be permitted for an app signed with anything
but the platform key, so the method body is replaced with `return-void` instead
of letting the exception kill `Application.onCreate` and, with it, the whole app.

This tool touches exactly one method per run and refuses anything it cannot
prove is safe:

  * the method name must match exactly one declaration in the searched file,
  * its descriptor must return `void` (a value-returning method would change
    behaviour by returning null/zero silently),
  * the original body is printed to stdout before the edit, so the log carries
    the evidence and the text can be restored.

It writes the new body but does not decide to build; the caller does.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

METHOD_HEADER = re.compile(
    r"^\.method\s+(?P<mods>[^\n]*?)\s*(?P<name>[A-Za-z_$][\w$]*)"
    r"\((?P<params>[^)]*)\)(?P<ret>\S+)\s*$",
    re.MULTILINE,
)


def find_declarations(text: str, name: str) -> list[re.Match[str]]:
    return [
        m
        for m in METHOD_HEADER.finditer(text)
        if m.group("name") == name
    ]


def body_bounds(text: str, match: re.Match[str]) -> tuple[int, int]:
    end = text.find("\n.end method", match.end())
    if end < 0:
        raise SystemExit(f".method {match.group('name')} has no matching .end method")
    return match.start(), end + len("\n.end method")


def locate(decoded: Path, dotted_class: str) -> list[Path]:
    relative = Path(dotted_class.replace(".", "/") + ".smali")
    hits = sorted(decoded.glob(f"smali*/{relative}"))
    if not hits:
        raise SystemExit(f"no smali file for {dotted_class} under {decoded}")
    return hits


def neutralize(decoded: Path, dotted_class: str, method: str) -> None:
    hits = locate(decoded, dotted_class)

    found: list[tuple[Path, re.Match[str]]] = []
    for path in hits:
        text = path.read_text(encoding="utf-8", errors="surrogateescape")
        for match in find_declarations(text, method):
            found.append((path, match))

    if not found:
        raise SystemExit(f"{dotted_class}#{method} is not declared in {[str(h) for h in hits]}")
    if len(found) > 1:
        places = [f"{p}:{m.start()}" for p, m in found]
        raise SystemExit(f"{method} is ambiguous — declared {len(found)} times at {places}")

    path, match = found[0]
    if match.group("ret") != "V":
        raise SystemExit(
            f"refusing {dotted_class}#{method}: it returns {match.group('ret')}, "
            "and a dummy return would silently change behaviour"
        )

    text = path.read_text(encoding="utf-8", errors="surrogateescape")
    start, end = body_bounds(text, match)

    print(f"--- original {dotted_class}#{method} in {path}")
    print(text[start:end])

    replacement = (
        f".method {match.group('mods')} {method}({match.group('params')})V\n"
        f"    .locals 0\n"
        f"\n"
        f"    # Neutralized by tools/re_patch_smali.py: the write below needs\n"
        f"    # WRITE_SECURE_SETTINGS, which no repacked app can hold. Returning\n"
        f"    # early keeps Application.onCreate alive.\n"
        f"    return-void\n"
        f".end method"
    )

    path.write_text(text[:start] + replacement + text[end:], encoding="utf-8", errors="surrogateescape")
    print(f"--- neutralized {dotted_class}#{method} ({end - start} bytes of body removed)")


def scan_put_string(decoded: Path, limit: int) -> None:
    """Show every secure-settings write in the tree, so the next crash is not a surprise."""
    pattern = re.compile(r"Settings\$(Global|System|Secure);->put(String|Int)\(")
    total = 0
    shown = 0
    for path in sorted(decoded.glob("smali*/**/*.smali")):
        text = path.read_text(encoding="utf-8", errors="surrogateescape")
        for match in pattern.finditer(text):
            total += 1
            if shown < limit:
                line = text.count("\n", 0, match.start()) + 1
                print(f"{path}:{line}: {match.group(0)}")
                shown += 1
    print(f"--- secure-settings writes: {total} call sites ({shown} shown)")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", required=True, type=Path)
    parser.add_argument("--class", dest="dotted", help="fully qualified class, e.g. cn.nubia.common.util.CommonUtil")
    parser.add_argument("--method", help="method name to neutralize (must return void)")
    parser.add_argument("--scan-putstring", action="store_true", help="list Settings.Global/System/Secure writes")
    parser.add_argument("--limit", type=int, default=40)
    args = parser.parse_args(argv)

    if not args.decoded.is_dir():
        raise SystemExit(f"not a directory: {args.decoded}")

    if args.dotted and args.method:
        neutralize(args.decoded, args.dotted, args.method)
    elif args.dotted or args.method:
        raise SystemExit("--class and --method must be given together")

    if args.scan_putstring:
        scan_put_string(args.decoded, args.limit)

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
