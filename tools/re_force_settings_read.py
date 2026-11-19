#!/usr/bin/env python3
"""Force the value a smali `Settings.Global.getInt(key, 0)` read returns.

Why this exists — the panel's second gate, measured on the device:

    PowerPanelService$1.run():
        if (Settings.Global.getInt(cr, "nubia_game_scene", 0) == 0
                && powerPanelWindowManager != null) {
            Log.i("Panel_Service", "send stop service");
            sendMessageDelayed(1, 0);   // removePowerPanelView + stopSelf
        } else {
            handler.postDelayed(mClosePowerPanelTimer, …);
        }

`nubia_game_scene` is written by RedMagic's Game Space, so on any other ROM it
reads 0 and the panel is torn down ~80 ms after it was shown — which is exactly
what the logcat showed: `MSG msg.what = 0` (show) then `MSG msg.what = 1`.

The key is a plain `Settings.Global` read (no permission needed to read), so the
fix cannot be "write the setting": an ordinary app has neither
WRITE_SECURE_SETTINGS nor root. Instead the read's result is overwritten with the
answer a game-scene device would give. That keeps every other branch — including
the auto-close timer — exactly as the vendor wrote it.

Usage:
    python3 tools/re_force_settings_read.py --decoded build/decoded \
        --class 'cn.zte.gamefloat.powerpanel.PowerPanelService$1' \
        --method run --string nubia_game_scene --value 1
    python3 tools/re_force_settings_read.py --self-test
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import tempfile

METHOD_START = re.compile(r"^\.method\b.*\b(?P<name>[\w$<>]+)\(")
CONST_STRING = re.compile(r'^\s*const-string(?:/jumbo)?\s+v\d+,\s*"(?P<key>[^"]+)"\s*$')
GET_INT = re.compile(r"^\s*invoke-static\s+\{[^}]*\},\s*Landroid/provider/Settings\$Global;->getInt:")
MOVE_RESULT = re.compile(r"^(?P<indent>\s*)move-result\s+(?P<reg>v\d+)\s*$")


class PatchError(RuntimeError):
    pass


def const_for(value: int) -> tuple[str, str]:
    """Return (opcode, literal) for a small int constant."""
    if -8 <= value <= 7:
        return "const/4", f"0x{value & 0xF:x}"
    if -32768 <= value <= 32767:
        return "const/16", f"0x{value & 0xFFFF:x}"
    return "const", f"0x{value & 0xFFFFFFFF:x}"


def force_read(lines: list[str], method: str, key: str, value: int) -> list[str]:
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

    hits: list[int] = []
    key_seen = False
    get_seen = False
    for i in range(start, end):
        line = lines[i]
        const = CONST_STRING.match(line)
        if const:
            key_seen = const.group("key") == key
            get_seen = False
            continue
        if key_seen and GET_INT.match(line):
            get_seen = True
            continue
        if get_seen and (move := MOVE_RESULT.match(line)):
            hits.append(i)
            key_seen = False
            get_seen = False
    if len(hits) != 1:
        raise PatchError(
            f"expected exactly one getInt(\"{key}\") + move-result in {method}, found {len(hits)}"
        )

    at = hits[0]
    match = MOVE_RESULT.match(lines[at])
    assert match is not None
    reg = match.group("reg")
    opcode, literal = const_for(value)
    injected = f"{match.group('indent')}{opcode} {reg}, {literal}"

    # Idempotent: the same write immediately after the read means it is already done.
    following = lines[at + 1].strip() if at + 1 < len(lines) else ""
    if following == f"{opcode} {reg}, {literal}":
        return lines

    out = list(lines)
    out[at + 1:at + 1] = [injected]
    return out


def apply(decoded: str, cls: str, method: str, key: str, value: int) -> str:
    rel = cls.replace(".", "/") + ".smali"
    paths = glob.glob(os.path.join(decoded, "smali*", rel))
    if len(paths) != 1:
        raise PatchError(f"expected one smali for {cls}, found {len(paths)}: {paths}")
    path = paths[0]
    with open(path, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    patched = force_read(lines, method, key, value)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(patched) + "\n")
    return path


SOURCE = """\
.class public Lcn/zte/gamefloat/powerpanel/PowerPanelService$1;
.super Ljava/lang/Object;

.method public run()V
    .locals 3

    invoke-virtual {p0}, Lcn/zte/gamefloat/powerpanel/PowerPanelService$1;->getApplicationContext()Landroid/content/Context;

    move-result-object v0

    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v0

    const-string v1, "nubia_game_scene"

    const/4 v2, 0x0

    invoke-static {v0, v1, v2}, Landroid/provider/Settings$Global;->getInt:(Landroid/content/ContentResolver;Ljava/lang/String;I)I

    move-result v0

    if-nez v0, :cond_0

    return-void

    :cond_0
    return-void
.end method
"""


def self_test() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        tree = os.path.join(tmp, "decoded", "smali", "cn", "zte", "gamefloat", "powerpanel")
        os.makedirs(tree)
        name = "PowerPanelService$1.smali"
        with open(os.path.join(tree, name), "w", encoding="utf-8") as handle:
            handle.write(SOURCE)

        path = apply(
            os.path.join(tmp, "decoded"),
            "cn.zte.gamefloat.powerpanel.PowerPanelService$1",
            "run",
            "nubia_game_scene",
            1,
        )
        text = open(path, encoding="utf-8").read()
        expected = "    move-result v0\n    const/4 v0, 0x1\n"
        if expected not in text:
            print("SELF-TEST FAILED\n--- got ---\n" + text, file=sys.stderr)
            return 1

        again = apply(
            os.path.join(tmp, "decoded"),
            "cn.zte.gamefloat.powerpanel.PowerPanelService$1",
            "run",
            "nubia_game_scene",
            1,
        )
        if open(again, encoding="utf-8").read().count("const/4 v0, 0x1") != 1:
            print("SELF-TEST FAILED: second run duplicated the write", file=sys.stderr)
            return 1

        try:
            apply(
                os.path.join(tmp, "decoded"),
                "cn.zte.gamefloat.powerpanel.PowerPanelService$1",
                "run",
                "some_other_key",
                1,
            )
        except PatchError:
            pass
        else:
            print("SELF-TEST FAILED: a missing key did not raise", file=sys.stderr)
            return 1

    print("self-test OK: one write after the read, idempotent, fails loudly on a missing key")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", help="apktool output directory")
    parser.add_argument("--class", dest="cls", help="fully qualified class name")
    parser.add_argument("--method", help="method name holding the settings read")
    parser.add_argument("--string", dest="key", help="Settings.Global key that is read")
    parser.add_argument("--value", type=lambda text: int(text, 0), help="value the read must return")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    missing = [name for name in ("decoded", "cls", "method", "key", "value")
               if getattr(args, name) is None]
    if missing:
        parser.error("missing: " + ", ".join(missing))
    try:
        path = apply(args.decoded, args.cls, args.method, args.key, args.value)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"forced getInt(\"{args.key}\") to {args.value} in {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
