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

The block uses parameter registers (`p0` = this, `p1` = bean) and locals v0…v3.
If the method declares fewer than four locals, the declaration is widened to four
instead of refusing: widening a local frame is always safe and lets us patch
methods that originally declared fewer locals.

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


def params_width(descriptor: str) -> int:
    """Total register width of the parameters in a method descriptor."""
    i = descriptor.find("(")
    if i < 0:
        return 0
    depth = 0
    j = i
    while j < len(descriptor):
        if descriptor[j] == "(":
            depth += 1
        elif descriptor[j] == ")":
            depth -= 1
            if depth == 0:
                break
        j += 1
    params_str = descriptor[i + 1:j]
    width = 0
    k = 0
    n = len(params_str)
    while k < n:
        c = params_str[k]
        if c in "ZBCSFIJD":
            width += 2 if c in "JD" else 1
            k += 1
        elif c == "L":
            semi = params_str.find(";", k)
            if semi < 0:
                break
            width += 1
            k = semi + 1
        elif c == "[":
            while k < n and params_str[k] == "[":
                k += 1
            if k < n and params_str[k] == "L":
                semi = params_str.find(";", k)
                if semi < 0:
                    break
                k = semi + 1
            elif k < n:
                k += 1
            width += 1
        else:
            k += 1
    return width


def block(context_field: str, bean_getter: str, bean_type: str, cls: str,
          action: str, extra: str, component: str | None,
          data: str | None = None, label: str = "nfw_skip",
          activity: str | None = None,
          activity_extra: str | None = None) -> list[str]:
    # The guards must jump past the block when a value is MISSING (if-eqz), not
    # when it is present: an inverted guard still assembles and still leaves the
    # method running, so the only symptom is a broadcast that never happens.
    # Measured on the device 2026-11-19: an earlier build shipped `if-nez p1`,
    # every tap ran startApp and no sendBroadcast ever appeared in logcat.
    if activity:
        # Start an ACTIVITY instead of sending a broadcast.
        # Measured 2026-11-19 19:4x: MIUI refused every injected broadcast with
        #   WakePathChecker: MIUILOG-AutoStart, Service/Provider/Broadcast Reject
        #   BroadcastQueueInjector: process is not permitted to wake path
        # so GameAssist's process never started. A startActivity issued by an app
        # that is in the foreground (GameSpace, at the tap) is allowed, and it is
        # the only wake path MIUI does not police here.
        package_name, activity_name = activity.split("/", 1)
        return [
            f"    iget-object v1, p0, {cls}->{context_field}:Landroid/content/Context;",
            f"    if-eqz v1, :{label}",
            "    new-instance v2, Landroid/content/Intent;",
            "    invoke-direct {v2}, Landroid/content/Intent;-><init>()V",
            "    new-instance v3, Landroid/content/ComponentName;",
            f'    const-string v4, "{package_name}"',
            f'    const-string v5, "{activity_name}"',
            "    invoke-direct {v3, v4, v5}, Landroid/content/ComponentName;-><init>(Ljava/lang/String;Ljava/lang/String;)V",
            "    invoke-virtual {v2, v3}, Landroid/content/Intent;->setComponent(Landroid/content/ComponentName;)Landroid/content/Intent;",
        ] + ([
            # A boolean marker the activity checks before doing anything else:
            # a real MultiSubScreen launch carries no such extra and is untouched.
            f'    const-string v4, "{activity_extra}"',
            "    const/4 v5, 0x1",
            "    invoke-virtual {v2, v4, v5}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;",
        ] if activity_extra else []) + [
            # FLAG_ACTIVITY_NEW_TASK: the Context is the launcher helper, not an Activity.
            "    const v3, 0x10000000",
            "    invoke-virtual {v2, v3}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;",
            "    invoke-virtual {v1, v2}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V",
            f"    :{label}",
        ]

    lines = [
        f"    if-eqz p1, :{label}",
        f"    invoke-virtual {{p1}}, {bean_type}->{bean_getter}()Ljava/lang/String;",
        "    move-result-object v0",
        f"    if-eqz v0, :{label}",
        f"    iget-object v1, p0, {cls}->{context_field}:Landroid/content/Context;",
        f"    if-eqz v1, :{label}",
        "    new-instance v2, Landroid/content/Intent;",
        f'    const-string v3, "{action}"',
        "    invoke-direct {v2, v3}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V",
        f'    const-string v3, "{extra}"',
        "    invoke-virtual {v2, v3, v0}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;",
        # FLAG_RECEIVER_INCLUDE_STOPPED_PACKAGES: the panel package may be in the
        # stopped state after a reinstall or force-stop, and a stopped package is
        # skipped silently by the broadcast queue.
        "    const/16 v3, 0x20",
        "    invoke-virtual {v2, v3}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;",
    ]
    if data:
        # البيانات ليست تفصيلًا: مستقبل GameAssist يفلتر على Scheme: "dispatcher"
        # و Ssp: LITERAL …UPDATE_PLUGIN_CONFIG، فبلا setData لا يطابق الفلتر
        # أصلًا ولا يصل البثّ — وقياسه من dumpsys package (2026-11-19).
        lines += [
            f'    const-string v3, "{data}"',
            "    invoke-static {v3}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;",
            "    move-result-object v3",
            "    invoke-virtual {v2, v3}, Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;",
        ]
    if component:
        # An implicit broadcast is never delivered to a manifest receiver on
        # Android 8+ (measured 2026-11-19: enqueued, then dropped). Naming the
        # component makes the send explicit, which is delivered.
        package, receiver = component.split("/", 1)
        lines += [
            "    new-instance v3, Landroid/content/ComponentName;",
            f'    const-string v4, "{package}"',
            f'    const-string v5, "{receiver}"',
            "    invoke-direct {v3, v4, v5}, Landroid/content/ComponentName;-><init>(Ljava/lang/String;Ljava/lang/String;)V",
            "    invoke-virtual {v2, v3}, Landroid/content/Intent;->setComponent(Landroid/content/ComponentName;)Landroid/content/Intent;",
        ]
    lines += [
        "    invoke-virtual {v1, v2}, Landroid/content/Context;->sendBroadcast(Landroid/content/Intent;)V",
        f"    :{label}",
    ]
    return lines


def inject(lines: list[str], descriptor: str, context_field: str, bean_getter: str,
           bean_type: str, cls: str, action: str, extra: str,
           component: str | None = None, data: str | None = None,
           label: str = "nfw_skip", activity: str | None = None,
           activity_extra: str | None = None) -> list[str]:
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
    directive_idx = None
    directive_kind = None  # "locals" or "registers"
    is_static = "static" in lines[start].split()
    params_w = params_width(descriptor)
    for i in range(start, end):
        found = LOCALS.match(lines[i])
        if found:
            locals_count = int(found.group("count"))
            directive_idx = i
            directive_kind = "locals"
            break
        found = REGISTERS.match(lines[i])
        if found:
            this_w = 0 if is_static else 1
            locals_count = int(found.group("count")) - this_w - params_w
            directive_idx = i
            directive_kind = "registers"
            break
    if directive_idx is None:
        raise PatchError("method has no .locals/.registers directive")
    # Widen the local frame to fit the block's registers instead of refusing:
    # widening is always safe and lets us patch methods that declared fewer.
    # The activity shape uses v0..v5 (ComponentName needs two strings), exactly
    # like the explicit-component shape; a 5-register frame would put v5 out of
    # range and the reassembly would fail.
    needed = 6 if (component or activity) else 4
    if locals_count < needed:
        indent = lines[directive_idx][:len(lines[directive_idx]) - len(lines[directive_idx].lstrip())]
        if directive_kind == "locals":
            lines[directive_idx] = f"{indent}.locals {needed}"
        else:
            this_w = 0 if is_static else 1
            existing = int(REGISTERS.match(lines[directive_idx]).group("count"))
            new_total = max(existing, needed + this_w + params_w)
            lines[directive_idx] = f"{indent}.registers {new_total}"
        locals_count = needed

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

    if any(f":{label}" in line for line in lines[start:end]):
        return lines

    out = list(lines)
    out[target:target] = block(context_field, bean_getter, bean_type, cls, action, extra,
                               component, data, label, activity, activity_extra)
    return out


def apply(decoded: str, cls: str, method: str, signature: str, context_field: str,
          bean_getter: str, bean_type: str, action: str, extra: str,
          component: str | None = None, data: str | None = None,
          label: str = "nfw_skip", activity: str | None = None,
          activity_extra: str | None = None) -> str:
    rel = cls.replace(".", "/") + ".smali"
    paths = glob.glob(os.path.join(decoded, "smali*", rel))
    if len(paths) != 1:
        raise PatchError(f"expected one smali for {cls}, found {len(paths)}: {paths}")
    path = paths[0]
    with open(path, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    patched = inject(lines, f"{method}{signature}", context_field, bean_getter,
                     bean_type, f"L{cls.replace('.', '/')};", action, extra, component, data,
                     label, activity, activity_extra)
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
            # the guards must skip when a value is missing, and must precede the send
            "if-eqz p1, :nfw_skip" in text,
            "if-eqz v0, :nfw_skip" in text,
            "if-eqz v1, :nfw_skip" in text,
            "if-nez" not in text.split(":nfw_skip")[0],
            text.index("if-eqz p1, :nfw_skip") < text.index("sendBroadcast"),
            text.count("sendBroadcast") == 1,
            # a stopped receiver package must still receive it
            "const/16 v3, 0x20" in text,
            ".locals 5" in text,
            # the block must sit after the annotation, never inside it
            text.index(".end annotation") < text.index("sendBroadcast"),
            text.index("names = {") < text.index("if-eqz p1, :nfw_skip"),
            # a field reference must be a full type descriptor
            "Lcn/nubia/gamelauncher/helper/CardHelper;->mContext:Landroid/content/Context;" in text,
        ]
        if not all(checks):
            print("SELF-TEST FAILED\n" + text, file=sys.stderr)
            return 1

        # explicit component: the send must name the receiver, or Android 8+
        # drops it on the floor for being implicit
        open(path, "w", encoding="utf-8").write(SAMPLE)
        apply(os.path.join(tmp, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
              "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
              "mContext", "getPackageName", "Lcn/nubia/gamelauncher/bean/AppListItemBean;",
              "A.B", "packageName", "cn.zte.gamefloat/cn.zte.gamefloat.receiver.GameForegroundChangeReceiver")
        text = open(path, encoding="utf-8").read()
        if ("Landroid/content/ComponentName;-><init>(Ljava/lang/String;Ljava/lang/String;)V" not in text
                or 'const-string v5, "cn.zte.gamefloat.receiver.GameForegroundChangeReceiver"' not in text
                or ".locals 6" not in text):
            print("SELF-TEST FAILED: explicit component not emitted\n" + text, file=sys.stderr)
            return 1

        # data URI: the GameAssist dispatcher receiver filters on
        # Scheme: "dispatcher" + Ssp LITERAL, so an intent without setData is
        # never matched. Measured from dumpsys package (2026-11-19).
        open(path, "w", encoding="utf-8").write(SAMPLE)
        apply(os.path.join(tmp, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
              "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
              "mContext", "getPackageName", "Lcn/nubia/gamelauncher/bean/AppListItemBean;",
              "A.B", "packageName",
              "cn.nubia.gameassist/cn.nubia.gameassist.plugin.config.UpdatePluginConfigReceiver",
              "dispatcher:cn.nubia.gameassist.config.UPDATE_PLUGIN_CONFIG")
        text = open(path, encoding="utf-8").read()
        if ("Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;" not in text
                or "Landroid/content/Intent;->setData(Landroid/net/Uri;)Landroid/content/Intent;" not in text
                or '"dispatcher:cn.nubia.gameassist.config.UPDATE_PLUGIN_CONFIG"' not in text
                or text.index("setData") > text.index("setComponent")
                or text.count("sendBroadcast") != 1):
            print("SELF-TEST FAILED: data URI not emitted before the component\n" + text,
                  file=sys.stderr)
            return 1

        apply(os.path.join(tmp, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
              "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
              "mContext", "getPackageName", "Lcn/nubia/gamelauncher/bean/AppListItemBean;",
              "A.B", "packageName")
        if open(path, encoding="utf-8").read().count("sendBroadcast") != 1:
            print("SELF-TEST FAILED: second run duplicated the block", file=sys.stderr)
            return 1

        open(path, "w", encoding="utf-8").write(SAMPLE.replace(".locals 5", ".locals 2"))
        apply(os.path.join(tmp, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
              "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
              "mContext", "getPackageName", "Lcn/nubia.gamelauncher/bean/AppListItemBean;",
              "A.B", "packageName")
        text = open(path, encoding="utf-8").read()
        if "sendBroadcast" not in text:
            print("SELF-TEST FAILED: tight locals were not patched", file=sys.stderr)
            return 1
        import re as _re
        m = _re.search(r"\.locals\s+(\d+)", text)
        if not m or int(m.group(1)) < 4:
            print("SELF-TEST FAILED: locals not widened to >= 4", file=sys.stderr)
            return 1

    # activity mode: MIUI refuses to wake GameAssist through a broadcast
    # (WakePathChecker, measured 2026-11-19), so the injected wake path is a
    # startActivity issued by the foreground GameSpace instead. Its own temp
    # tree, because the block above already left the shared one.
    with tempfile.TemporaryDirectory() as tmp_act:
        tree_act = os.path.join(tmp_act, "decoded", "smali", "cn", "nubia", "gamelauncher", "helper")
        os.makedirs(tree_act)
        path_act = os.path.join(tree_act, "CardHelper.smali")
        open(path_act, "w", encoding="utf-8").write(SAMPLE)
        apply(os.path.join(tmp_act, "decoded"), "cn.nubia.gamelauncher.helper.CardHelper",
              "startApp", "(Lcn/nubia/gamelauncher/bean/AppListItemBean;ZLjava/lang/Runnable;)V",
              "mContext", "getPackageName", "Lcn/nubia/gamelauncher/bean/AppListItemBean;",
              "", "", label="nfw_act",
              activity="cn.nubia.gameassist/cn.nubia.multisubscreen.ui.MultiSubScreenSourceActivity",
              activity_extra="nfw_handle")
        text_act = open(path_act, encoding="utf-8").read()
        if ("Landroid/content/Context;->startActivity(Landroid/content/Intent;)V" not in text_act
                or "sendBroadcast" in text_act
                or 'const-string v5, "cn.nubia.multisubscreen.ui.MultiSubScreenSourceActivity"' not in text_act
                or 'const-string v4, "nfw_handle"' not in text_act
                or "Landroid/content/Intent;->putExtra(Ljava/lang/String;Z)Landroid/content/Intent;" not in text_act
                or "const v3, 0x10000000" not in text_act
                or ".locals 6" not in text_act):
            print("SELF-TEST FAILED: activity start not emitted\n" + text_act, file=sys.stderr)
            return 1

    print("self-test OK: guards skip on missing values, explicit component, survives a rerun, widens tight locals")
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
    parser.add_argument("--component", help="pkg/cls; makes the send explicit (required on Android 8+)")
    parser.add_argument("--data", help="setData URI, e.g. dispatcher:… (needed when the receiver filters on a scheme)")
    parser.add_argument("--label", default="nfw_skip", help="skip-label suffix, so a second injection in the same method is possible")
    parser.add_argument("--activity-extra",
                        help="boolean extra put on the started activity; the hijacked "
                             "GameAssist activity uses it to tell our launch apart")
    parser.add_argument("--activity", help="pkg/cls; start an ACTIVITY instead of a broadcast "
                                          "(the only wake path MIUI allows on this build)")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    required = ["decoded", "cls", "method", "signature", "context_field"]
    if not args.activity:
        # action/extra/bean only matter for the broadcast shape; the activity
        # shape needs nothing but a context field.
        required += ["action", "extra", "bean_getter"]
    missing = [name for name in required if getattr(args, name) is None]
    if missing:
        parser.error("missing: " + ", ".join(missing))
    try:
        path = apply(args.decoded, args.cls, args.method, args.signature, args.context_field,
                     args.bean_getter, args.bean_type,
                     args.action or "", args.extra or "", args.component,
                     args.data, args.label, args.activity, args.activity_extra)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    kind = "activity start" if args.activity else "broadcast"
    print(f"injected the {kind} into {args.cls}.{args.method} ({path})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
