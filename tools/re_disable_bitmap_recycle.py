#!/usr/bin/env python3
"""Turn every `Bitmap.recycle()` call in a decoded APK into a no-op.

Why — measured, not guessed. GameSpace dies on the start button with
`java.lang.RuntimeException: Canvas: trying to use a recycled bitmap` raised by
`BitmapDrawable.draw` from `ImageView.onDraw` (logcat -b crash, 2026-11-19
03:31). The old note "there are no recycle calls, so it must be native" was
wrong by construction: a dex keeps the type descriptor and the method name in
separate string tables, so `Bitmap;->recycle` can never appear in a dex. On the
decoded tree the call exists 25 times — Glide's pool, Lottie, and GameSpace's
own wallpaper, video-controller and settings views.

Replacing the call with a local sink removes the recycling (a leak, not a
crash) and answers one question with one build: does the start-button crash
disappear? If it does, the cause is proven and can be narrowed per package
instead of guessed.

The sink is written as `Lnd/max/re/PatchBitmap;` inside an existing smali root,
so it ships in the same dex as its callers.

Usage:
    python3 tools/re_disable_bitmap_recycle.py --decoded build/decoded [--dry-run]
    python3 tools/re_disable_bitmap_recycle.py --self-test
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import tempfile

CALL = re.compile(
    r"(?P<indent>[ \t]*)invoke-virtual(?:/range)? \{(?P<regs>[^}]*)\}, "
    r"Landroid/graphics/Bitmap;->recycle\(\)V"
)
SINK = """\
.class public final Lnd/max/re/PatchBitmap;
.super Ljava/lang/Object;

# Written by tools/re_disable_bitmap_recycle.py — diagnostic build only.
# Recycling a bitmap that a live ImageView still draws is what crashes GameSpace
# on the start button; this sink keeps the reference alive instead.

.method public static recycle(Landroid/graphics/Bitmap;)V
    .locals 0
    return-void
.end method
"""


class PatchError(RuntimeError):
    pass


def rewrite(text: str) -> tuple[str, int]:
    """Replace every recycle call; return the new text and how many were changed."""
    count = 0

    def swap(match: re.Match[str]) -> str:
        nonlocal count
        count += 1
        return (
            f"{match.group('indent')}invoke-static {{{match.group('regs')}}}, "
            "Lnd/max/re/PatchBitmap;->recycle(Landroid/graphics/Bitmap;)V"
        )

    return CALL.sub(swap, text), count


def apply(decoded: str, dry_run: bool) -> tuple[int, int]:
    files = 0
    calls = 0
    for root, dirs, names in os.walk(decoded):
        # Walk only the dex roots (`smali`, `smali_classes2`, …); prune res/ and
        # assets/ so a 286 MB tree is not scanned for nothing. The root itself
        # must not be pruned: its relative path is ".", not "smali".
        if root != decoded:
            top = os.path.relpath(root, decoded).split(os.sep)[0]
            if not top.startswith("smali"):
                dirs[:] = []
                continue
        for name in names:
            if not name.endswith(".smali"):
                continue
            path = os.path.join(root, name)
            with open(path, encoding="utf-8") as handle:
                text = handle.read()
            if "Landroid/graphics/Bitmap;->recycle()V" not in text:
                continue
            patched, changed = rewrite(text)
            if not changed:
                continue
            files += 1
            calls += changed
            if not dry_run:
                with open(path, "w", encoding="utf-8") as handle:
                    handle.write(patched)
    if not dry_run and calls:
        sink_dir = os.path.join(decoded, "smali")
        if not os.path.isdir(sink_dir):
            # Fall back to the first smali root the tree actually has.
            for name in sorted(os.listdir(decoded)):
                if name.startswith("smali") and os.path.isdir(os.path.join(decoded, name)):
                    sink_dir = os.path.join(decoded, name)
                    break
        target = os.path.join(sink_dir, "nd", "max", "re", "PatchBitmap.smali")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8") as handle:
            handle.write(SINK)
    return files, calls


def self_test() -> int:
    """A patcher that only proves it runs has proven nothing: assert the output."""
    sample = """\
.class public Ldemo/View;
.super Ljava/lang/Object;

.method public detach()V
    .locals 1
    iget-object v0, p0, Ldemo/View;->bitmap:Landroid/graphics/Bitmap;
    invoke-virtual {v0}, Landroid/graphics/Bitmap;->recycle()V
    return-void
.end method
"""
    with tempfile.TemporaryDirectory() as root:
        decoded = os.path.join(root, "decoded")
        smali = os.path.join(decoded, "smali", "demo")
        os.makedirs(smali)
        path = os.path.join(smali, "View.smali")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(sample)

        files, calls = apply(decoded, dry_run=False)
        text = open(path, encoding="utf-8").read()
        problems = []
        if (files, calls) != (1, 1):
            problems.append(f"expected 1 file and 1 call, got {files} and {calls}")
        if "Landroid/graphics/Bitmap;->recycle()V" in text:
            problems.append("the original call survived")
        if "invoke-static {v0}, Lnd/max/re/PatchBitmap;->recycle(Landroid/graphics/Bitmap;)V" not in text:
            problems.append("the sink call is missing")
        sink = os.path.join(decoded, "smali", "nd", "max", "re", "PatchBitmap.smali")
        if not os.path.isfile(sink):
            problems.append("the sink class was not written")
        elif "return-void" not in open(sink, encoding="utf-8").read():
            problems.append("the sink class does not return")

        # A second run must be a no-op, not a second rewrite of the rewritten call.
        again = apply(decoded, dry_run=False)
        if again != (0, 0):
            problems.append(f"second run changed {again} (expected 0, 0)")

        # Dry run must not touch the file.
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(sample)
        os.remove(sink)
        files, calls = apply(decoded, dry_run=True)
        if (files, calls) != (1, 1):
            problems.append(f"dry run reported {files} and {calls}")
        if "Landroid/graphics/Bitmap;->recycle()V" not in open(path, encoding="utf-8").read():
            problems.append("dry run wrote the file")
        if os.path.exists(sink):
            problems.append("dry run wrote the sink")

        if problems:
            print("SELF-TEST FAILED\n" + "\n".join("  - " + p for p in problems), file=sys.stderr)
            return 1
    print("self-test OK: rewrites the call, writes the sink, is idempotent, dry run writes nothing")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", help="directory apktool decoded the APK into")
    parser.add_argument("--dry-run", action="store_true", help="report without writing")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    if not args.decoded:
        parser.error("--decoded is required unless --self-test is given")
    if not os.path.isdir(args.decoded):
        parser.error(f"not a directory: {args.decoded}")

    try:
        files, calls = apply(args.decoded, dry_run=args.dry_run)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    verb = "would skip" if args.dry_run else "skipped"
    print(f"{verb} {calls} recycle calls in {files} files")
    if not calls:
        print("error: no Bitmap.recycle call found — the premise would be false", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
