#!/usr/bin/env python3
"""Inject one broadcast at the top of a smali method.

Why — measured on this device: nothing in the Nubia stack *sends*
`cn.zte.gamefloat.powerpanel.action.FOREGROUND_GAME_CHANGE`. Counting the action
string across every dex gives GameFloat=1 and GameSpace=1, both as receivers, and
zero for GameAssist/gamehelperline/plugintrigger/gamehelpmodule/gameaiasst. On a
RedMagic this signal comes from the system framework, which Xiaomi does not have,
so the panel is never asked for when a game starts.

`cn.nubia.gamelauncher.helper.CardHelper.startApp(AppListItemBean, Z, Runnable)`
is the launcher's own launch path: it reads `this.mContext` and
`bean.getPackageName()`, so the sender can be GameSpace itself — a foreground app,
which is the only sender Android lets start another app's process from a
broadcast.

The block uses parameter registers (`p0` = this, `p1` = bean) and locals v0…v3,
which exists only when the method declares at least four locals; the tool refuses
otherwise instead of producing smali that cannot assemble.

Usage:
    python3 tools/re_inject_broadcast.py --decoded build/decoded \\
        --class cn.nubia.gamelauncher.helper.CardHelper \\
        --method startApp \\
        --signature '(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V' \\
        --context-field mContext --bean-getter getPackageName \\
        --action cn.zte.gamefloat.powerpanel.action.FOREGROUND_GAME_CHANGE \\
        --extra packageName
    python3 tools/re_inject_broadcast.py --self-test
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import tempfile

LOCALS = re.compile(r"^\s*\.locals\s+(?P<count>\d+)\s*$")
REGISTERS = re.compile(r"^\s*\.registers\s+(?P<count>\d+)\s*$")
# A method body opens with directives — `.line`, `.local`, `.annotation … .end
# annotation` — and an annotation contains bare identifiers such as `names = {`.
# Treating those as instructions (the first build did) put the block inside the
# annotation and apktool refused the file, so only real mnemonics count.
MNEMONIC = re.compile(
    r"^\s*(?:nop|move[a-z-]*|return[a-z-]*|const[a-z/0-9-]*|monitor-[a-z]+|"
    r"check-cast|instance-of|array-length|new-instance|new-array|filled-new-array[a-z/]*|"
    r"throw|goto[a-z/0-9-]*|packed-switch|sparse-switch|cmpl?[a-z-]*|cmpg[a-z-]*|"
    r"if-[a-z]+|aget[a-z-]*|aput[a-z-]*|iget[a-z-]*|iput[a-z-]*|sget[a-z-]*|sput[a-z-]*|"
    r"invoke-[a-z/]+|unop|binop|int-to-[a-z]+|neg-[a-z]+|not-[a-z]+|rsub-[a-z]+|"
    r"add-[a-z/]+|sub-[a-z/]+|mul-[a-z/]+|div-[a-z/]+|rem-[a-z/]+|and-[a-z/]+|"
    r"or-[a-z/]+|xor-[a-z/]+|shl-[a-z/]+|shr-[a-z/]+|ushr-[a-z/]+)(?:\s|$)"
)
ANNOTATION_OPEN = re.compile(r"^\s*\.annotation\b")
ANNOTATION_CLOSE = re.compile(r"^\s*\.end\s+annotation\b")


class PatchError(RuntimeError):
    pass


def block(context_field: str, bean_getter: str, bean_type: str, cls: str,
          action: str, extra: str) -> list[str]:
    return [
        "    if-nez p1, :nfw_skip",
        f"    invoke-virtual {{p1}}, {bean_type}->{bean_getter}()Ljava/lang/String;",
        "    move-result-object v0",
        "    if-nez v0, :nfw_skip",
        f"    iget-object v1, p0, {cls}->{context_field}:Landroid/content/Context;",
        "    if-nez v1, :nfw_skip",
        "    new-instance v2, Landroid/content/Intent;",
        f'    const-string v3, "{action}"',
        "    invoke-direct {v2, v3}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V",
        f'    const-string v3, "{extra}"',
        "    invoke-virtual {v2, v3, v0}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;",
        "    invoke-virtual {v1, v2}, Landroid/content/Context;->sendBroadcast(Landroid/content/Intent;)V",
        "    :nfw_skip",
    ]


def inject(lines: list[str], descriptor: str, context_field: str, bean_getter: str,
           bean_type: str, cls: str, action: str, extra: str) -> list[str]:
    starts = [i for i, line in enumerate(lines)
              if line.startswith(".method") and descriptor in line]
    if len(starts) != 1:
        raise PatchError(f"expected exactly one .method containing {descriptor!r}, found {len(starts)}")
    start = starts[0]
    ends = [i for i in range(start + 1, len(lines)) if lines[i].startswith(".end method")]
    if not ends:
        raise PatchError("the method has no .end method")
    end = ends[0]

    locals_count = 0
    for i in range(start, end):
        found = LOCALS.match(lines[i])
        if found:
            locals_count = int(found.group("count"))
            break
        found = REGISTERS.match(lines[i])
        if found:
            locals_count = int(found.group("count")) - 4  # instance method: this + 3 params
            break
    if locals_count < 4:
        raise PatchError(f"method declares {locals_count} locals; the block needs 4 (v0…v3)")

    target = None
    in_annotation = False
    for i in range(start, end):
        if ANNOTATION_OPEN.match(lines[i]):
            in_annotation = True
            continue
        if ANNOTATION_CLOSE.match(lines[i]):
            in_annotation = False
            continue
        if in_annotation:
            continue
        if MNEMONIC.match(lines[i]):
            target = i
            break
    if target is None:
        raise PatchError("could not find the first instruction")

    if any(":nfw_skip" in line for line in lines[start:end]):
        return lines

    out = list(lines)
    out[target:target] = block(context_field, bean_getter, bean_type, cls, action, extra)
    return out


def apply(decoded: str, cls: str, method: str, signature: str, context_field: str,
          bean_getter: str, bean_type: str, action: str, extra: str) -> str:
    rel = cls.replace(".", "/") + ".smali"
    paths = glob.glob(os.path.join(decoded, "smali*", rel))
    if len(paths) != 1:
        raise PatchError(f"expected one smali for {cls}, found {len(paths)}: {paths}")
    path = paths[0]
    with open(path, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    patched = inject(lines, f"{method}{signature}", context_field, bean_getter,
                     bean_type, f"L{cls.replace('.', '/')};", action, extra)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(patched) + "\n")
    return path


SAMPLE = """\
.class public Lcn/nubia/gamelauncher/helper/CardHelper;
.super Ljava/lang/Object;

.field private mContext:Landroid/content/Context;

.method public startApp(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V
    .locals 5
    .annotation system Ldalvik/annotation/MethodParameters;
        names = {
            "bean",
            "isCloneApp",
            "runnable"
        }
    .end annotation

    .line 84

    if-eqz p1, :cond_0

    iget-object v0, p0, Lcn/nubia/gamelauncher/helper/CardHelper;->mContext:Landroid/content/Context;

    return-void

    :cond_0
    return-void
.end method
"""


def self_test() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        tree = os.path.join(tmp, "decoded", "smali", "cn", "nubia", "gamelauncher", "helper")
        os.makedirs(tree)
        path = os.path.join(tree, "CardHelper.smali")
        open(path, "w", encoding="utf-8").write(SAMPLE)

        apply(os.path.join(tmp, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
              "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
              "mContext", "getPackageName", "Lcn/nubia/gamelauncher/bean/AppListItemBean;",
              "A.B", "packageName")
        text = open(path, encoding="utf-8").read()
        checks = [
            ":nfw_skip" in text,
            text.index("sendBroadcast") < text.index("if-eqz p1"),
            text.count("sendBroadcast") == 1,
            ".locals 5" in text,
            # the block must sit after the annotation, never inside it
            text.index(".end annotation") < text.index("sendBroadcast"),
            text.index("names = {") < text.index("if-nez p1, :nfw_skip"),
            # a field reference must be a full type descriptor
            "Lcn/nubia/gamelauncher/helper/CardHelper;->mContext:Landroid/content/Context;" in text,
        ]
        if not all(checks):
            print("SELF-TEST FAILED\n" + text, file=sys.stderr)
            return 1

        apply(os.path.join(tmp, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
              "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
              "mContext", "getPackageName", "Lcn/nubia/gamelauncher/bean/AppListItemBean;",
              "A.B", "packageName")
        if open(path, encoding="utf-8").read().count("sendBroadcast") != 1:
            print("SELF-TEST FAILED: second run duplicated the block", file=sys.stderr)
            return 1

        open(path, "w", encoding="utf-8").write(SAMPLE.replace(".locals 5", ".locals 2"))
        try:
            apply(os.path.join(tmp, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
                  "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
                  "mContext", "getPackageName", "Lcn/nubia/gamelauncher/bean/AppListItemBean;",
                  "A.B", "packageName")
        except PatchError:
            pass
        else:
            print("SELF-TEST FAILED: a method without room for the block was patched",
                  file=sys.stderr)
            return 1

    print("self-test OK: block lands before the first instruction, survives a rerun, refuses tight locals")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded")
    parser.add_argument("--class", dest="cls")
    parser.add_argument("--method")
    parser.add_argument("--signature")
    parser.add_argument("--context-field")
    parser.add_argument("--bean-getter")
    parser.add_argument("--bean-type", default="Lcn/nubia/gamelauncher/bean/AppListItemBean;")
    parser.add_argument("--action")
    parser.add_argument("--extra")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    missing = [name for name in ("decoded", "cls", "method", "signature", "context_field",
                                 "bean_getter", "action", "extra") if getattr(args, name) is None]
    if missing:
        parser.error("missing: " + ", ".join(missing))
    try:
        path = apply(args.decoded, args.cls, args.method, args.signature, args.context_field,
                     args.bean_getter, args.bean_type, args.action, args.extra)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"injected the broadcast into {args.cls}.{args.method} ({path})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
