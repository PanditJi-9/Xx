#!/usr/bin/env python3
"""Delete every call to a named smali method (measured cause, not a guess).

Why this exists — the refusal was never about the window type.

`DisplayPolicy.validateAddingWindowLw` in AOSP refuses a window when

    (attrs.privateFlags & PRIVATE_FLAG_TRUSTED_OVERLAY) != 0

and reports it as

    SecurityException: DisplayPolicy: uid … does not have
      android.permission.INTERNAL_SYSTEM_WINDOW

RedMagic is platform-signed, so its own helper
`WindowManagerWrapper$LayoutParams.addHidePrivateTrustedOverlayFlags(lp)` may set that
bit there. Measured on this device: the panel's LayoutParams already carried
`type = 2038` (`const/16 v3, #int 2038` in the installed dex) and our forced write was
present too, yet the refusal did not move — because the helper runs after the type is
set and right before `addView`.

The helper returns void and only ORs privileged private flags, so deleting the invoke
leaves the panel a plain overlay window (2038) without the platform-only bit. Nothing
else changes, and the call sites are the app's own smali either way.

Usage:
    python3 tools/re_strip_invoke.py --decoded build/decoded \
        --needle addHidePrivateTrustedOverlayFlags
    python3 tools/re_strip_invoke.py --self-test
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import tempfile

INVOKE = re.compile(r"^\s*invoke-(?:virtual|static|direct|interface|super)(?:/range)?\b")


class PatchError(RuntimeError):
    pass


def strip(lines: list[str], needle: str) -> tuple[list[str], list[str]]:
    """Return (lines without matching invokes, removed invokes)."""
    kept: list[str] = []
    removed: list[str] = []
    for line in lines:
        if INVOKE.match(line) and needle in line:
            removed.append(line.strip())
            continue
        kept.append(line)
    return kept, removed


def apply(decoded: str, needle: str) -> tuple[int, int]:
    """Strip `needle` calls under `decoded`; return (files, removed calls)."""
    if not needle or "/" in needle:
        raise PatchError(f"needle must be a bare method name, got {needle!r}")
    paths = sorted(set(glob.glob(os.path.join(decoded, "smali*", "**", "*.smali"), recursive=True)))
    if not paths:
        raise PatchError(f"no smali files under {decoded}")
    files = 0
    calls = 0
    for path in paths:
        with open(path, encoding="utf-8") as handle:
            lines = handle.read().splitlines()
        kept, removed = strip(lines, needle)
        if not removed:
            continue
        files += 1
        calls += len(removed)
        with open(path, "w", encoding="utf-8") as handle:
            handle.write("\n".join(kept) + "\n")
    return files, calls


SOURCE = """\
.class public Lx/Y;
.super Ljava/lang/Object;

.method public static twice(Landroid/view/WindowManager$LayoutParams;)V
    .locals 1

    invoke-static {p0}, Lcom/zte/shared/wrapper/WindowManagerWrapper$LayoutParams;->addHidePrivateTrustedOverlayFlags:(Landroid/view/WindowManager$LayoutParams;)V

    invoke-static {p0}, Lcom/zte/shared/wrapper/WindowManagerWrapper$LayoutParams;->setFitInsetsTypes:(Landroid/view/WindowManager$LayoutParams;)V

    return-void
.end method
"""


def self_test() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        tree = os.path.join(tmp, "decoded", "smali", "x")
        os.makedirs(tree)
        path = os.path.join(tree, "Y.smali")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(SOURCE)

        files, calls = apply(os.path.join(tmp, "decoded"), "addHidePrivateTrustedOverlayFlags")
        text = open(path, encoding="utf-8").read()
        if (files, calls) != (1, 1):
            print(f"SELF-TEST FAILED: expected (1, 1), got ({files}, {calls})", file=sys.stderr)
            return 1
        if "addHidePrivateTrustedOverlayFlags" in text:
            print("SELF-TEST FAILED: the call survived", file=sys.stderr)
            return 1
        if "setFitInsetsTypes" not in text or ".method" not in text:
            print("SELF-TEST FAILED: stripped more than the target call", file=sys.stderr)
            return 1

        # A second pass must find nothing and must not corrupt the file.
        again = apply(os.path.join(tmp, "decoded"), "addHidePrivateTrustedOverlayFlags")
        if again != (0, 0):
            print(f"SELF-TEST FAILED: second pass was not empty: {again}", file=sys.stderr)
            return 1
        if open(path, encoding="utf-8").read() != text:
            print("SELF-TEST FAILED: second pass rewrote the file", file=sys.stderr)
            return 1

        try:
            apply(os.path.join(tmp, "decoded"), "Lcom/x;")
        except PatchError:
            pass
        else:
            print("SELF-TEST FAILED: a descriptor was accepted as a needle", file=sys.stderr)
            return 1

    print("self-test OK: one call removed, neighbours kept, idempotent, rejects a descriptor")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", help="apktool output directory")
    parser.add_argument("--needle", help="bare method name whose invokes are removed")
    parser.add_argument("--min-hits", type=int, default=1, help="fail when fewer calls are removed")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    if not args.decoded or not args.needle:
        parser.error("--decoded and --needle are required")

    try:
        files, calls = apply(args.decoded, args.needle)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"stripped {calls} call(s) to {args.needle} across {files} file(s)")
    if calls < args.min_hits:
        print(
            f"error: expected at least {args.min_hits} call(s), removed {calls} — "
            "the patch would not change anything, refusing to build a lie",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
