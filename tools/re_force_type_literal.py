#!/usr/bin/env python3
"""Force a single integer literal inside one smali file, with an exact count gate.

Why a literal tool and not the LayoutParams one: three of the four `0x7d3`
sites build the params through `invoke-direct {v0, v3, v1, v2},
WindowManager$LayoutParams;-><init>(III)V`, and the fourth writes
`iput v1, v0, LayoutParams;->type:I` — neither is an `iput-object` of a
LayoutParams field, so `re_force_window_type.py` (which rewrites that shape)
cannot see them.

Measured 2026-11-19 17:42:52 (build 44557016911, pid 25889):
    am_crash: android.view.WindowManager$BadTokenException:
      Unable to add window … permission denied for window type 2003
    am_proc_died: [0,25889,cn.nubia.gameassist,905,19]
Type 2003 (TYPE_SYSTEM_ALERT) requires INTERNAL_SYSTEM_WINDOW, which no
repacked app can hold on Android 16, so the process dies the moment one of
these windows is created. 2038 (TYPE_APPLICATION_OVERLAY) is the public
equivalent and is what the panel and the drag handle already use.

Usage:
    python3 tools/re_force_type_literal.py --decoded <dir> \
        --path smali/cn/nubia/plugin/timer/TimerWindow.smali \
        --needle 0x7d3 --value 0x7f6 --expect 1
"""

from __future__ import annotations

import argparse
import sys
import tempfile
from pathlib import Path

SAMPLE = """\
.method private f()V
    .locals 4

    new-instance v0, Landroid/view/WindowManager$LayoutParams;

    const/16 v3, 0x7d3

    invoke-direct {v0, v3, v1, v2}, Landroid/view/WindowManager$LayoutParams;-><init>(III)V

    return-void
.end method
"""


def force(text: str, needle: str, value: str, expect: int) -> tuple[str, int]:
    """Return (rewritten text, hits). Raises when the count is not `expect`."""
    hits = text.count(needle)
    if hits != expect:
        raise SystemExit(
            f"literal {needle}: expected {expect} occurrence(s), found {hits}"
        )
    # Only the literal token changes; the line's indentation and the rest of the
    # instruction stay byte-identical so the diff is the type and nothing else.
    return text.replace(needle, value), hits


def self_test() -> int:
    out, hits = force(SAMPLE, "0x7d3", "0x7f6", 1)
    assert hits == 1, hits
    assert "const/16 v3, 0x7f6" in out, out
    assert "0x7d3" not in out, out
    # A wrong expectation must stop the build, not pass silently.
    try:
        force(SAMPLE, "0x7d3", "0x7f6", 2)
    except SystemExit:
        pass
    else:
        raise AssertionError("expected SystemExit on a count mismatch")
    # Nothing else on the line may move.
    assert out.replace("0x7f6", "0x7d3") == SAMPLE, "rewrite touched more than the literal"
    print("self-test OK: rewrites one literal, refuses a wrong count, leaves the line intact")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", help="apktool output directory")
    parser.add_argument("--path", help="file, relative to --decoded")
    parser.add_argument("--needle", default="0x7d3", help="literal to replace")
    parser.add_argument("--value", default="0x7f6", help="literal to write")
    parser.add_argument("--expect", type=int, default=1, help="exact expected count")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()

    if not (args.decoded and args.path):
        parser.error("--decoded and --path are required")

    target = Path(args.decoded) / args.path
    if not target.is_file():
        raise SystemExit(f"missing file: {target}")

    text = target.read_text(encoding="utf-8")
    if args.value in text and args.needle not in text:
        print(f"already forced: {args.path}")
        return 0

    out, hits = force(text, args.needle, args.value, args.expect)
    target.write_text(out, encoding="utf-8")
    print(f"{args.path}: {hits} × {args.needle} → {args.value}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
