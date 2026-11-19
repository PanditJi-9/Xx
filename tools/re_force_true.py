#!/usr/bin/env python3
"""Force the boolean result of a named smali call to true.

Why this exists — the panel's gate, measured in
`cn.zte.gamefloat.receiver.GameForegroundChangeReceiver.onReceive`:

    boolean running = isServiceRunning(ctx, "…PowerPanelService");
    boolean enabled = isAppEnable(ctx, pkg);
    if (enabled && getGameLauncherPkgFromDB().contains(pkg)) {
        PowerPanelService.foregroundPkgChange(ctx, pkg, 0, 1);   // show
    } else {
        PowerPanelService.foregroundPkgChange(ctx, pkg, 1, 1);   // hide
    }

`getGameLauncherPkgFromDB()` reads the launcher's own database, so a game the
launcher has never recorded — or any game at all when that database is empty —
always lands in the hide branch. Measured on the device:

    Common_ForegroundChange : packageName=com.tencent.ig, isAppEnable: true, false
    Panel_Service : MSG msg.what = 1

The first `contains(...)` result is therefore forced to true, which makes the
show branch reachable for any package whose gate is open. Only the first match
inside the method is touched: the same receiver uses a second `contains` for the
counter path, which must keep its real answer.

Usage:
    python3 tools/re_force_true.py --decoded build/decoded \
        --class cn.zte.gamefloat.receiver.GameForegroundChangeReceiver \
        --method onReceive --invoke 'List;->contains' --value 1
    python3 tools/re_force_true.py --self-test
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import tempfile

METHOD_START = re.compile(r"^\.method\b.*\b(?P<name>[\w$<>]+)\(")
INVOKE = re.compile(r"^\s*invoke-(?:virtual|static|direct|interface)(?:/range)?\b")
MOVE = re.compile(r"^(?P<indent>\s*)move-result\s+(?P<reg>v\d+|p\d+)\s*$")


class PatchError(RuntimeError):
    pass


def const_for(value: int) -> tuple[str, str]:
    if -8 <= value <= 7:
        return "const/4", f"0x{value & 0xF:x}"
    if -32768 <= value <= 32767:
        return "const/16", f"0x{value & 0xFFFF:x}"
    return "const", f"0x{value & 0xFFFFFFFF:x}"


def force_first(lines: list[str], method: str, invoke: str, value: int) -> list[str]:
    starts = [
        i for i, line in enumerate(lines)
        if line.startswith(".method") and (m := METHOD_START.match(line)) and m.group("name") == method
    ]
    if len(starts) != 1:
        raise PatchError(f"expected exactly one .method {method}(…), found {len(starts)}")
    start = starts[0]
    ends = [i for i in range(start + 1, len(lines)) if lines[i].startswith(".end method")]
    if not ends:
        raise PatchError(f".method {method} has no .end method")
    end = ends[0]

    hit = None
    for i in range(start, end):
        if INVOKE.match(lines[i]) and invoke in lines[i]:
            hit = i
            break
    if hit is None:
        raise PatchError(f"no invoke matching {invoke!r} in {method}")

    move_at = None
    for i in range(hit + 1, min(hit + 4, end)):
        if MOVE.match(lines[i]):
            move_at = i
            break
    if move_at is None:
        raise PatchError(f"invoke at {hit} is not followed by move-result in {method}")

    match = MOVE.match(lines[move_at])
    assert match is not None
    reg = match.group("reg")
    opcode, literal = const_for(value)
    injected = f"{match.group('indent')}{opcode} {reg}, {literal}"

    following = lines[move_at + 1].strip() if move_at + 1 < len(lines) else ""
    if following == f"{opcode} {reg}, {literal}":
        return lines

    out = list(lines)
    out[move_at + 1:move_at + 1] = [injected]
    return out


def apply(decoded: str, cls: str, method: str, invoke: str, value: int) -> str:
    rel = cls.replace(".", "/") + ".smali"
    paths = glob.glob(os.path.join(decoded, "smali*", rel))
    if len(paths) != 1:
        raise PatchError(f"expected one smali for {cls}, found {len(paths)}: {paths}")
    path = paths[0]
    with open(path, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    patched = force_first(lines, method, invoke, value)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(patched) + "\n")
    return path


SOURCE = """\
.class public Lcn/zte/gamefloat/receiver/R;
.super Ljava/lang/Object;

.method public onReceive(Landroid/content/Context;Landroid/content/Intent;)V
    .locals 2

    invoke-direct {p0}, Lcn/zte/gamefloat/receiver/R;->db()Ljava/util/List;

    move-result-object p0

    invoke-interface {p0, p1}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_0

    return-void

    :cond_0
    return-void
.end method
"""


def self_test() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        tree = os.path.join(tmp, "decoded", "smali", "cn", "zte", "gamefloat", "receiver")
        os.makedirs(tree)
        path = os.path.join(tree, "R.smali")
        with open(path, "w", encoding="utf-8") as handle:
            handle.write(SOURCE)

        apply(
            os.path.join(tmp, "decoded"),
            "cn.zte.gamefloat.receiver.R",
            "onReceive",
            "List;->contains",
            1,
        )
        text = open(path, encoding="utf-8").read()
        if "    move-result p0\n    const/4 p0, 0x1\n" not in text:
            print("SELF-TEST FAILED\n--- got ---\n" + text, file=sys.stderr)
            return 1

        again = apply(
            os.path.join(tmp, "decoded"),
            "cn.zte.gamefloat.receiver.R",
            "onReceive",
            "List;->contains",
            1,
        )
        if open(again, encoding="utf-8").read().count("const/4 p0, 0x1") != 1:
            print("SELF-TEST FAILED: second run duplicated the write", file=sys.stderr)
            return 1

        try:
            apply(
                os.path.join(tmp, "decoded"),
                "cn.zte.gamefloat.receiver.R",
                "onReceive",
                "Map;->get",
                1,
            )
        except PatchError:
            pass
        else:
            print("SELF-TEST FAILED: a missing invoke did not raise", file=sys.stderr)
            return 1

    print("self-test OK: first match forced, neighbours untouched, idempotent, fails loudly")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", help="apktool output directory")
    parser.add_argument("--class", dest="cls", help="fully qualified class name")
    parser.add_argument("--method", help="method holding the invoke")
    parser.add_argument("--invoke", help="substring identifying the invoke line")
    parser.add_argument("--value", type=lambda text: int(text, 0), help="value the result must have")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    missing = [name for name in ("decoded", "cls", "method", "invoke", "value")
               if getattr(args, name) is None]
    if missing:
        parser.error("missing: " + ", ".join(missing))
    try:
        path = apply(args.decoded, args.cls, args.method, args.invoke, args.value)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"forced the first {args.invoke} result to {args.value} in {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
