#!/usr/bin/env python3
"""Insert one direct call at the top of a method — the "start it ourselves" tool.

Why: some Nubia components wait for a signal from the RedMagic system image
(`cn.nubia.componentcenter`, `com.zte.activityevent`) before they build their UI.
On a non-Nubia phone that signal never comes, so the class that *draws* the panel
is present and healthy but nothing ever asks it to. Patching the caller — the
app's own entry point — keeps one code path and needs no system service.

Contract, and every part of it is asserted by --self-test:
  * the call lands after `.locals`/`.registers` and after any `.annotation` body,
    never inside an annotation (measured bug class: a block injected into
    MethodParameters breaks the assembler);
  * `.locals` is widened when the inserted code needs registers, because
    refusing the method is worse than giving it one more local;
  * running twice inserts once (a marker line is the guard);
  * the target descriptor is emitted verbatim, so a typo fails at assembly
    instead of silently doing nothing.

Usage:
    python3 tools/re_inject_call.py --decoded decoded \
        --class cn.nubia.gameassist.GameAssistApplication \
        --method onCreate --signature '()V' \
        --target 'Lcn/nubia/gameassist/panel/PanelHelper;->show()V'

    python3 tools/re_inject_call.py --self-test
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import sys
import tempfile

MARKER = "# nfw_call"
LOCALS = re.compile(r"^\s*\.locals\s+(?P<count>\d+)\s*$")
REGISTERS = re.compile(r"^\s*\.registers\s+(?P<count>\d+)\s*$")
ANNOTATION_OPEN = re.compile(r"^\s*\.annotation\s")
ANNOTATION_CLOSE = re.compile(r"^\s*\.end annotation\s*$")
MNEMONIC = re.compile(
    r"^\s{4}(?!\.)(?!:)(?:" + "|".join([
        "invoke-", "move", "const", "iget", "iput", "sget", "sput", "new-",
        "if-", "goto", "return", "nop", "check-cast", "instance-of", "array-",
        "filled-new-array", "throw", "monitor-", "aget", "aput", "cmp", "int-",
        "long-", "float-", "double-", "neg-", "not-", "or-", "xor-", "and-",
        "shl", "shr", "ushr", "add-", "sub-", "mul-", "div-", "rem-", "packed-",
        "sparse-", "switch", "fill-array", "and-int", "or-int", "xor-int",
    ]) + r")"
)


class PatchError(Exception):
    pass


def params_width(descriptor: str) -> int:
    inner = descriptor[descriptor.index("(") + 1:descriptor.index(")")]
    width = 0
    k = 0
    while k < len(inner):
        ch = inner[k]
        if ch == "[":
            width += 1
            while k < len(inner) and inner[k] == "[":
                k += 1
            if k < len(inner) and inner[k] in "LZBCSIJFD":
                if inner[k] in "L":
                    semi = inner.find(";", k)
                    if semi < 0:
                        break
                    k = semi + 1
                else:
                    k += 1
            continue
        if ch == "L":
            semi = inner.find(";", k)
            if semi < 0:
                break
            k = semi + 1
        else:
            k += 1
        width += 1
    return width


def parse_target(target: str) -> tuple[str, str, str]:
    """'Lcls;->method(sig)Ret' -> (cls descriptor, method, full method descriptor)."""
    marker = "->"
    if marker not in target:
        raise PatchError(f"target must look like Lcls;->method(sig)Ret, got {target!r}")
    cls, member = target.split(marker, 1)
    if not cls.startswith("L") and not cls.startswith("["):
        raise PatchError(f"target class must be a descriptor, got {cls!r}")
    open_paren = member.find("(")
    if open_paren <= 0 or not member.endswith(")V"):
        raise PatchError(f"target method must be void with a descriptor, got {member!r}")
    return cls, member[:open_paren], member


def block(target: str, argument: str | None) -> list[str]:
    cls, method, descriptor = parse_target(target)
    # `descriptor` already is the full method descriptor (name + signature):
    # emitting method + descriptor again produced `showshow()V` and failed here.
    if argument is None:
        return [
            f"    {MARKER}",
            f"    invoke-static {{}}, {cls}->{descriptor}",
        ]
    # One String argument: v0 carries it and the call passes it.
    return [
        f"    {MARKER}",
        f'    const-string v0, "{argument}"',
        f"    invoke-static {{v0}}, {cls}->{descriptor}",
    ]


def inject(lines: list[str], descriptor: str, target: str, argument: str | None) -> list[str]:
    starts = [i for i, line in enumerate(lines)
              if line.startswith(".method") and descriptor in line]
    if len(starts) != 1:
        raise PatchError(f"expected exactly one .method containing {descriptor!r}, found {len(starts)}")
    start = starts[0]
    ends = [i for i in range(start + 1, len(lines)) if lines[i].startswith(".end method")]
    if not ends:
        raise PatchError("the method has no .end method")
    end = ends[0]

    if any(MARKER in line for line in lines[start:end]):
        return lines

    needed = 1 if argument is not None else 0
    directive_idx = None
    directive_kind = None
    locals_count = 0
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

    if locals_count < needed:
        indent = lines[directive_idx][:len(lines[directive_idx]) - len(lines[directive_idx].lstrip())]
        if directive_kind == "locals":
            lines[directive_idx] = f"{indent}.locals {needed}"
        else:
            this_w = 0 if is_static else 1
            existing = int(REGISTERS.match(lines[directive_idx]).group("count"))
            lines[directive_idx] = f"{indent}.registers {max(existing, needed + this_w + params_w)}"

    target_idx = None
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
            target_idx = i
            break
    if target_idx is None:
        raise PatchError("could not find the first instruction")

    out = list(lines)
    out[target_idx:target_idx] = block(target, argument)
    return out


def apply(decoded: str, cls: str, method: str, signature: str, target: str,
          argument: str | None) -> str:
    rel = cls.replace(".", "/") + ".smali"
    paths = glob.glob(os.path.join(decoded, "smali*", rel))
    if len(paths) != 1:
        raise PatchError(f"expected one smali for {cls}, found {len(paths)}: {paths}")
    path = paths[0]
    with open(path, encoding="utf-8") as handle:
        lines = handle.read().splitlines()
    patched = inject(lines, f"{method}{signature}", target, argument)
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(patched) + "\n")
    return path


SAMPLE = """\
.class public Lcn/nubia/gameassist/GameAssistApplication;
.super Landroid/app/Application;

.method public onCreate()V
    .locals 0
    .annotation system Ldalvik/annotation/MethodParameters;
        names = {
        }
    .end annotation

    .line 20
    invoke-super {p0}, Landroid/app/Application;->onCreate()V

    return-void
.end method
"""


def self_test() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        tree = os.path.join(tmp, "decoded", "smali", "cn", "nubia", "gameassist")
        os.makedirs(tree)
        path = os.path.join(tree, "GameAssistApplication.smali")
        open(path, "w", encoding="utf-8").write(SAMPLE)

        apply(os.path.join(tmp, "decoded"), "cn.nubia.gameassist.GameAssistApplication",
              "onCreate", "()V", "Lcn/nubia/gameassist/Panel;->show()V", None)
        text = open(path, encoding="utf-8").read()
        checks = [
            "invoke-static {}, Lcn/nubia/gameassist/Panel;->show()V" in text,
            text.index(".end annotation") < text.index("invoke-static"),
            text.index("invoke-static") < text.index("invoke-super"),
        ]
        if not all(checks):
            print("SELF-TEST FAILED\n" + text, file=sys.stderr)
            return 1

        apply(os.path.join(tmp, "decoded"), "cn.nubia.gameassist.GameAssistApplication",
              "onCreate", "()V", "Lcn/nubia/gameassist/Panel;->show()V", None)
        if open(path, encoding="utf-8").read().count("invoke-static") != 1:
            print("SELF-TEST FAILED: second run duplicated the call", file=sys.stderr)
            return 1

        open(path, "w", encoding="utf-8").write(SAMPLE)
        apply(os.path.join(tmp, "decoded"), "cn.nubia.gameassist.GameAssistApplication",
              "onCreate", "()V", "Lcn/nubia/gameassist/Panel;->showWith(Ljava/lang/String;)V",
              "a.b")
        text = open(path, encoding="utf-8").read()
        if (".locals 1" not in text
                or 'const-string v0, "a.b"' not in text
                or "invoke-static {v0}, Lcn/nubia/gameassist/Panel;->showWith(Ljava/lang/String;)V" not in text):
            print("SELF-TEST FAILED: argument form\n" + text, file=sys.stderr)
            return 1

    print("self-test OK: call lands after annotations, once only, widens locals, carries one string")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded")
    parser.add_argument("--class", dest="cls")
    parser.add_argument("--method")
    parser.add_argument("--signature")
    parser.add_argument("--target", help="Lcls;->method(sig)Ret, void only")
    parser.add_argument("--argument", help="one String constant passed as the only argument")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    missing = [name for name in ("decoded", "cls", "method", "signature", "target")
               if getattr(args, name) is None]
    if missing:
        parser.error("missing: " + ", ".join(missing))
    try:
        path = apply(args.decoded, args.cls, args.method, args.signature, args.target,
                     args.argument)
    except PatchError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(f"injected the call into {args.cls}.{args.method} ({path})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
