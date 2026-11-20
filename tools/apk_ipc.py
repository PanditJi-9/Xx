#!/usr/bin/env python3
"""Extract the real integration contract between the ecosystem apps: IPC strings.

Why this exists: the class-level reference graph came out ~zero (each app bundles or
defines what it needs), so the coupling that actually keeps the ecosystem working is
names, not types: intent actions, component names, content authorities, permission
names. Those are exactly the strings an independent rebuild has to preserve, so they
must be measured, not guessed.

What it does NOT claim: a string that looks like an action is not proof that anything
sends or receives it. Which side sends is a code-reading question; this tool only
collects the vocabulary and says where it was found (manifest attribute, DEX string,
or both).

Usage:
    python3 tools/apk_ipc.py apps/ --json docs/audit/apk-ipc.json \
        --markdown docs/IPC_AND_INTEGRATION.md
    python3 tools/apk_ipc.py --self-test
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import zipfile

from apk_inventory import apk_facts
from apk_xref import apk_entries

VENDOR_ROOTS = ("cn.nubia.", "com.nubia.", "cn.zte.", "com.zte.", "com.redmagic.")
ACTION_HINTS = (".action.", ".intent.", "ACTION_")

# `cn.nubia.gameassist/.MainActivity` or `cn.nubia.gameassist/cn.nubia.gameassist.X`
COMPONENT = re.compile(r"^([a-z][\w.]+\.[a-z][\w.]*)/(\.?[\w.$]+)$")
ACTION = re.compile(r"^[a-z][\w]*(\.[\w]+){2,}$")


def vendor_strings(strings: list[str]) -> dict[str, list[str]]:
    actions: set[str] = set()
    components: set[str] = set()
    authorities: set[str] = set()
    for s in strings:
        if not s or s.startswith("L") or " " in s:
            continue
        if s.startswith("content://"):
            authorities.add(s[len("content://"):].split("/")[0])
            continue
        if "/" in s:
            m = COMPONENT.match(s)
            if m and m.group(1).startswith(VENDOR_ROOTS):
                components.add(s)
            continue
        if not s.startswith(VENDOR_ROOTS):
            continue
        if any(h in s for h in ACTION_HINTS) or ACTION.match(s):
            actions.add(s)
    return {
        "actions": sorted(actions),
        "components": sorted(components),
        "authorities": sorted(authorities),
    }


def manifest_surface(facts: dict) -> dict[str, list[str]]:
    actions: set[str] = set()
    authorities: set[str] = set()
    components: list[str] = []
    for kind, items in facts.get("components", {}).items():
        for comp in items:
            name = comp.get("name", "")
            if name:
                components.append(f"{facts.get('package', '?')}/{name.lstrip('.')}")
            for act in comp.get("actions", []):
                actions.add(act)
            for auth in (comp.get("authorities", "") or "").split(";"):
                if auth:
                    authorities.add(auth.split("@")[0])
    for act in facts.get("actions", {}):
        actions.add(act)
    return {"actions": sorted(actions), "components": components, "authorities": sorted(authorities)}


def self_test() -> int:
    strings = [
        "cn.nubia.gameassist.action.OPEN_PANEL",
        "cn.nubia.gameassist/.GameAssistActivity",
        "content://cn.nubia.gameassist.provider/games",
        "Lcn/nubia/gameassist/Foo;",
        "android.intent.action.MAIN",
        "not a vendor thing",
        "com.zte.gamefloat/.PanelService",
    ]
    got = vendor_strings(strings)
    checks = [
        ("action", "cn.nubia.gameassist.action.OPEN_PANEL" in got["actions"], True),
        ("component", "cn.nubia.gameassist/.GameAssistActivity" in got["components"], True),
        ("authority", "cn.nubia.gameassist.provider" in got["authorities"], True),
        ("no class descriptors", any("Lcn/" in a for a in got["actions"]), False),
        ("framework action rejected", "android.intent.action.MAIN" in got["actions"], False),
        ("second pkg", "com.zte.gamefloat/.PanelService" in got["components"], True),
    ]
    bad = [(n, g, e) for n, g, e in checks if g != e]
    for n, g, e in bad:
        print(f"self-test FAIL {n}: got {g} want {e}")
    if not bad:
        print(f"self-test OK: {len(checks)} IPC classification cases")
    return 1 if bad else 0


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("root", nargs="?")
    ap.add_argument("--json")
    ap.add_argument("--markdown")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args(argv)
    if args.self_test:
        return self_test()
    if not args.root:
        print("need a directory of APKs", file=sys.stderr)
        return 2

    apks = []
    for dirpath, _d, files in os.walk(args.root):
        for n in sorted(files):
            if n.endswith(".apk") and "NubiaRedmagickyi_RedMagic" not in n:
                apks.append(os.path.join(dirpath, n))
    if not apks:
        print("no APKs found — refusing to write an empty contract document", file=sys.stderr)
        return 2
    report: dict[str, dict] = {}
    for path in sorted(apks):
        facts = apk_facts(path)
        strings, _defined, _f = apk_entries(path)
        dex_side = vendor_strings(strings)
        man = manifest_surface(facts)
        report[os.path.basename(path)] = {
            "package": facts.get("package", ""),
            "declaredPermissions": facts.get("declaredPermissions", []),
            "usesPermissions": [p for p in facts.get("permissions", []) if p.startswith(VENDOR_ROOTS)],
            "manifest": man,
            "dex": dex_side,
        }

    if args.json:
        os.makedirs(os.path.dirname(os.path.abspath(args.json)), exist_ok=True)
        tmp = args.json + ".tmp"
        with open(tmp, "w", encoding="utf-8") as fh:
            json.dump(report, fh, ensure_ascii=False, indent=1)
        os.replace(tmp, args.json)
        print(f"json: {args.json}")

    if args.markdown:
        os.makedirs(os.path.dirname(os.path.abspath(args.markdown)), exist_ok=True)
        out = [
            "# IPC_AND_INTEGRATION — measured vocabulary",
            "",
            "Generated by `tools/apk_ipc.py`. Two evidence columns per app: what the manifest",
            "declares (authoritative for the component that exists) and what the DEX mentions",
            "(authoritative only for the name existing somewhere in code, not for it being sent).",
            "",
            "## Signature permissions declared by these apps",
            "",
            "| app | permission | protectionLevel |",
            "| --- | --- | --- |",
        ]
        for name in sorted(report):
            for p in report[name]["declaredPermissions"]:
                out.append(f"| {name} | `{p['name']}` | `{p['protectionLevel'] or '?'}` |")
        out += ["", "## Vendor actions", ""]
        for name in sorted(report):
            acts = report[name]["manifest"]["actions"] or []
            if acts:
                out.append(f"### {name} (`{report[name]['package']}`)")
                out += [f"- `{a}`" for a in acts]
                out.append("")
        out += ["## Vendor component names (as referenced in DEX)", ""]
        for name in sorted(report):
            comps = report[name]["dex"]["components"]
            if comps:
                out.append(f"### {name}")
                out += [f"- `{c}`" for c in comps[:40]]
                if len(comps) > 40:
                    out.append(f"- … {len(comps) - 40} more")
                out.append("")
        tmp = args.markdown + ".tmp"
        with open(tmp, "w", encoding="utf-8") as fh:
            fh.write("\n".join(out) + "\n")
        os.replace(tmp, args.markdown)
        print(f"markdown: {args.markdown}")
    return 0


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    sys.exit(main(sys.argv[1:]))
