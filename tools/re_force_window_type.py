#!/usr/bin/env python3
"""Force an int field (typically the window type) on a freshly built object.

Why this exists: reading `dexdump` output cannot settle which integer in
`WindowManager$LayoutParams.<init>(III)V` is the window type — the printed
argument order is ambiguous, and the observed values (2038, a float bit
pattern, -3) match no public constructor cleanly. Rather than keep guessing
from a disassembly, this tool writes the field after the object is stored, so
the device can answer the question that matters:

    SecurityException: DisplayPolicy: uid … does not have
      android.permission.INTERNAL_SYSTEM_WINDOW

was raised from `PowerPanelDetailsView.initLayoutParams -> addView`. If the
refusal is about the type, forcing `2038` (TYPE_APPLICATION_OVERLAY, allowed
with SYSTEM_ALERT_WINDOW) removes it. If it is not, the refusal stays and the
reading was wrong — which is worth knowing either way.

The insertion is deliberately local: the register that already holds the
object stays live across its `iput-object`, so only one scratch register is
needed, and `.locals` is grown by one to provide it (apktool's `pN` names
follow automatically).

Usage:
    python3 tools/re_force_window_type.py --decoded build/decoded \
        --class cn.zte.gamefloat.powerpanel.PowerPanelDetailsView \
        --method initLayoutParams --field lp --value 2038

    python3 tools/re_force_window_type.py --self-test
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import tempfile

LINE = re.compile(
    r"^\s*iput-object\s+(?P<obj>v\d+),\s*(?P<owner>p\d+|v\d+),\s*"
    r"(?P<owner_cls>L[^;]+;)->(?P<field>[A-Za-z_$][\w$]*):(?P<desc>L[^;]+;)\s*$"
)
METHOD_START = re.compile(r"^\.method\b.*\b(?P<name>[\w$<>]+)\((?P<args>[^)]*)\)")
LOCALS = re.compile(r"^(\s*)\.locals\s+(\d+)\s*$")


class PatchError(RuntimeError):
    pass


def force_field(lines: list[str], method: str, field: str, value: int) -> list[str]:
    """Return new lines with `<field> = value` forced inside `method`."""
    starts = [
        i for i, line in enumerate(lines)
        if line.startswith(".method") and (m := METHOD_START.match(line))
        and m.group("name") == method
    ]
    if len(starts) != 1:
        raise PatchError(f"expected exactly one .method {method}(…), found {len(starts)}")
    start = starts[0]
    ends = [i for i in range(start + 1, len(lines)) if lines[i].startswith(".end method")]
    if not ends:
        raise PatchError(f".method {method} has no .end method")
    end = ends[0]

    hits = []
    for i in range(start, end):
        m = LINE.match(lines[i])
        if m and m.group("field") == field:
            hits.append((i, m))
    if len(hits) != 1:
        raise PatchError(
            f"expected exactly one iput-object …->{field}:L… in {method}, found {len(hits)}"
        )
    at, m = hits[0]

    # Idempotent: a repeat run must not stack a second write.
    marker = f"{m.group('desc')}->type:I"
    if any(marker in line for line in lines[at:at + 4]):
        return lines

    locals_at = [(i, LOCALS.match(lines[i])) for i in range(start, end) if LOCALS.match(lines[i])]
    if not locals_at:
        raise PatchError(f".method {method} uses .registers, not .locals; refusing to guess")
    locals_index, locals_match = locals_at[0]
    assert locals_match is not None
    count = int(locals_match.group(2))
    scratch = f"v{count}"

    out = list(lines)
    out[locals_index] = f"{locals_match.group(1)}.locals {count + 1}"
    # The field is declared by the *stored object's* class, not by the class
    # that holds the reference: LayoutParams declares `type`, the view does not.
    out[at + 1:at + 1] = [
        f"    const/16 {scratch}, 0x{value:x}",
        f"    iput {scratch}, {m.group('obj')}, {m.group('desc')}->type:I",
    ]
    return out


def apply(decoded: str, cls: str, method: str, field: str, value: int) -> str:
    rel = cls.replace(".", "/") + ".smali"
    paths = glob.glob(os.path.join(decoded, "smali*", rel))
    if len(paths) != 1:
        raise PatchError(f"expected one smali for {cls}, found {len(paths)}: {paths}")
    path = paths[0]
    with open(path, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    patched = force_field(lines, method, field, value)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(patched) + "\n")
    return path


SOURCES = {
    "PowerPanelDetailsView.smali": """\
.class public Lcn/zte/gamefloat/powerpanel/PowerPanelDetailsView;
.super Landroid/widget/FrameLayout;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 1

    invoke-direct {p0, p1}, Landroid/widget/FrameLayout;-><init>(Landroid/content/Context;)V

    const/4 v0, 0x0

    iput-object v0, p0, Lcn/zte/gamefloat/powerpanel/PowerPanelDetailsView;->lp:Landroid/view/WindowManager$LayoutParams;

    return-void
.end method

.method private initLayoutParams()V
    .locals 3

    new-instance v0, Landroid/view/WindowManager$LayoutParams;

    invoke-direct {v0}, Landroid/view/WindowManager$LayoutParams;-><init>()V

    iput-object v0, p0, Lcn/zte/gamefloat/powerpanel/PowerPanelDetailsView;->lp:Landroid/view/WindowManager$LayoutParams;

    return-void
.end method
""",
}


def self_test() -> int:
    """Rearrange a known method and assert the exact insertion."""
    with tempfile.TemporaryDirectory() as tmp:
        tree = os.path.join(tmp, "decoded", "smali")
        target = os.path.join(tree, "cn", "zte", "gamefloat", "powerpanel")
        os.makedirs(target)
        for name, body in SOURCES.items():
            with open(os.path.join(target, name), "w", encoding="utf-8") as handle:
                handle.write(body)

        path = apply(
            os.path.join(tmp, "decoded"),
            "cn.zte.gamefloat.powerpanel.PowerPanelDetailsView",
            "initLayoutParams",
            "lp",
            2038,
        )
        text = open(path, encoding="utf-8").read()
        expected = (
            "    .locals 4\n"
            "\n"
            "    new-instance v0, Landroid/view/WindowManager$LayoutParams;\n"
            "\n"
            "    invoke-direct {v0}, Landroid/view/WindowManager$LayoutParams;-><init>()V\n"
            "\n"
            "    iput-object v0, p0, "
            "Lcn/zte/gamefloat/powerpanel/PowerPanelDetailsView;->"
            "lp:Landroid/view/WindowManager$LayoutParams;\n"
            "    const/16 v3, 0x7f6\n"
            "    iput v3, v0, Landroid/view/WindowManager$LayoutParams;->type:I\n"
        )
        if expected not in text:
            print("SELF-TEST FAILED\n--- got ---\n" + text, file=sys.stderr)
            return 1

        # The constructor stores the same field: the tool must not pick it, and
        # a second run must not double-insert.
        again = apply(
            os.path.join(tmp, "decoded"),
            "cn.zte.gamefloat.powerpanel.PowerPanelDetailsView",
            "initLayoutParams",
            "lp",
            2038,
        )
        if open(again, encoding="utf-8").read().count("->type:I") != 1:
            print("SELF-TEST FAILED: second run inserted a duplicate", file=sys.stderr)
            return 1

        try:
            apply(
                os.path.join(tmp, "decoded"),
                "cn.zte.gamefloat.powerpanel.PowerPanelDetailsView",
                "initView",
                "lp",
                2038,
            )
        except PatchError:
            pass
        else:
            print("SELF-TEST FAILED: missing method did not raise", file=sys.stderr)
            return 1

    print("self-test OK: single insertion, idempotent, fails loudly on a missing method")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", help="apktool output directory")
    parser.add_argument("--class", dest="cls", help="fully qualified class name")
    parser.add_argument("--method", help="method name holding the iput-object")
    parser.add_argument("--field", help="field name to force")
    parser.add_argument("--value", type=lambda text: int(text, 0), help="int to write")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    missing = [name for name in ("decoded", "cls", "method", "field", "value")
               if getattr(args, name) is None]
    if missing:
        parser.error("missing: " + ", ".join(missing))
    try:
        path = apply(args.decoded, args.cls, args.method, args.field, args.value)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"forced {args.field}.type = 0x{args.value:x} in {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
