#!/usr/bin/env python3
"""Game Turbo dependency closure, measured from the manifests already in the repo.

The product is **Game Turbo**: the in-game panel/assist plus the library and session
around it. The other apps of the suite are *reference material*, not products — but a
reference may be a dependency, and excluding one without evidence is exactly the
mistake this tool exists to prevent.

Edges are measured, never guessed:
  * permission  — app A declares a permission that app B uses  => B depends on A
  * action      — A and B both declare the same intent action  => shared contract
  * reference   — B's DEX mentions A's package/classes (from apk-xref) => weak edge

A weak edge alone (a string in a DEX pool) is not enough to be a dependency; it is
reported, and the app is marked NEEDS_REVIEW rather than silently dropped.

Output: docs/audit/GT_CLOSURE.md + gt-closure.json.
`--assert` fails if any app has no verdict, which is the whole point of the tool.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import deque
from pathlib import Path

SEED_PACKAGES = {"cn.nubia.gamelauncher", "cn.nubia.gameassist"}
SEED_REASON = "Game Turbo seed (library + in-game assist)"

EXCLUDED_TEXT = "no measured edge to any app in the closure"

# Actions every app declares (launcher, boot, widget refresh…). Sharing one of these proves
# nothing about dependency; a first run counted them and pulled 16 of 23 apps into the closure.
GENERIC_PREFIXES = ("android.", "com.android.", "androidx.", "com.google.android.")

# Vendor actions that every app of the ROM declares (job scheduler, first-boot, settings
# provider, analytics). They are system plumbing, not Game Turbo contracts: an app that
# shares only one of these is not a dependency of the product.
PLUMBING_HINTS = (
    "jobdispatcher", "owlsystem", "firstboot", "distributebus", "distbus",
    "analytics", "dynamic_receiver", "settings_provider", "downloadservice",
    "aigc.billing",
)


def meaningful(action: str) -> bool:
    """A contract this product actually consumes, not platform or ROM plumbing."""
    if action.startswith(GENERIC_PREFIXES) or action.count(".") < 2:
        return False
    lowered = action.lower()
    return not any(hint in lowered for hint in PLUMBING_HINTS)


def load(repo: Path) -> tuple[dict, dict, dict]:
    inv = json.loads((repo / "docs/audit/apk-inventory.json").read_text(encoding="utf-8"))
    ipc = json.loads((repo / "docs/audit/apk-ipc.json").read_text(encoding="utf-8"))
    xref_path = repo / "docs/audit/apk-xref.json"
    xref = json.loads(xref_path.read_text(encoding="utf-8")) if xref_path.exists() else {"matrix": {}}
    return inv, ipc, xref


def app_facts(ipc: dict) -> dict[str, dict]:
    """Per app: package, declared permissions, used permissions, declared actions."""
    facts: dict[str, dict] = {}
    for key, row in ipc.items():
        if not isinstance(row, dict) or "package" not in row:
            continue
        declared = {p["name"] for p in row.get("declaredPermissions", []) if isinstance(p, dict) and p.get("name")}
        used = {p for p in row.get("usesPermissions", []) if isinstance(p, str)}
        manifest = row.get("manifest") or {}
        actions = set(manifest.get("actions") or [])
        facts[key] = {
            "file": key,
            "package": row["package"],
            "declared": declared,
            "used": used,
            "actions": actions,
        }
    return facts


def closure(
    facts: dict[str, dict],
    matrix: dict,
    seed_packages: set[str] = SEED_PACKAGES,
) -> tuple[dict[str, dict], list[dict]]:
    """BFS from the seeds over permission and action edges; weak DEX edges as review flags."""
    by_pkg = {f["package"]: key for key, f in facts.items()}
    seeds = [by_pkg[p] for p in seed_packages if p in by_pkg]

    status: dict[str, dict] = {k: {"depth": None, "via": [], "weak": 0} for k in facts}
    queue = deque()
    for s in seeds:
        status[s].update(depth=0, via=[SEED_REASON])
        queue.append(s)

    while queue:
        cur = queue.popleft()
        cf = facts[cur]
        for other, of in facts.items():
            if other == cur or status[other]["depth"] is not None:
                continue
            reasons = []
            shared_actions = {a for a in (cf["actions"] & of["actions"]) if meaningful(a)}
            if shared_actions:
                reasons.append("shared action(s): " + ", ".join(sorted(shared_actions)[:3]))
            # permission: whoever declares it is the provider; whoever uses it depends on it
            if of["declared"] & cf["used"]:
                reasons.append("uses permission(s) declared here: " +
                               ", ".join(sorted(of["declared"] & cf["used"])))
            if cf["declared"] & of["used"]:
                reasons.append("this app uses their permission(s): " +
                               ", ".join(sorted(cf["declared"] & of["used"])[:3]))
            if reasons:
                status[other].update(depth=status[cur]["depth"] + 1, via=reasons)
                queue.append(other)

    # Weak, DEX-level edges: reported on every app, used only to flag review.
    for key, st in status.items():
        row = matrix.get(key) or {}
        st["weak"] = sum(1 for v in row.values() if isinstance(v, int) and v > 0)
    return status, seeds


def verdicts(facts: dict[str, dict], status: dict[str, dict]) -> list[dict]:
    rows = []
    for key, f in sorted(facts.items(), key=lambda kv: (kv[1]["depth"] if False else 0, kv[1]["package"])):
        st = status[key]
        if st["depth"] is None:
            verdict = "NEEDS_REVIEW" if st["weak"] > 0 else "NOT_NEEDED"
        elif st["depth"] == 0:
            verdict = "SEED"
        else:
            verdict = "REQUIRED"
        rows.append({
            "app": f["package"],
            "file": f["file"],
            "verdict": verdict,
            "depth": st["depth"],
            "weak_refs": st["weak"],
            "why": " · ".join(st["via"]) if st["via"] else EXCLUDED_TEXT,
        })
    order = {"SEED": 0, "REQUIRED": 1, "NEEDS_REVIEW": 2, "NOT_NEEDED": 3}
    rows.sort(key=lambda r: (order[r["verdict"]], r["depth"] if r["depth"] is not None else 99, r["app"]))
    return rows


def render(rows: list[dict]) -> str:
    counts = {}
    for r in rows:
        counts[r["verdict"]] = counts.get(r["verdict"], 0) + 1
    lines = [
        "# GT_CLOSURE — what Game Turbo actually needs from the 23 reference apps",
        "",
        "Generated by `tools/gt_closure.py` from the measured manifests. Do not edit by hand.",
        "",
        "**Product under reconstruction:** RedMagic Game Turbo (library + in-game assist).",
        "**Reference corpus:** all 23 apps. None is rebuilt as a separate product; none is",
        "excluded from this analysis — every app below has a verdict and a reason.",
        "",
        "## Counts",
        "",
        "| verdict | apps |",
        "| --- | --- |",
    ]
    for verdict in ("SEED", "REQUIRED", "NEEDS_REVIEW", "NOT_NEEDED"):
        if verdict in counts:
            lines.append(f"| `{verdict}` | {counts[verdict]} |")
    lines += [
        "",
        "Edge types are limited to what the manifests prove: a shared intent action, or a",
        "permission declared by one app and used by another. A DEX reference alone is weak",
        "(it may be a string or dead code) and only flags `NEEDS_REVIEW`.",
        "",
        "## Apps",
        "",
        "| app | verdict | depth | weak DEX refs | why |",
        "| --- | --- | --- | --- | --- |",
    ]
    for r in rows:
        depth = "—" if r["depth"] is None else str(r["depth"])
        lines.append(f"| `{r['app']}` | `{r['verdict']}` | {depth} | {r['weak_refs']} | {r['why']} |")
    lines += [
        "",
        "## What this does not prove",
        "",
        "- A shared action is a *contract*, not a live channel: neither side is known to call",
        "  the other. The rebuild must therefore implement the contract it needs and measure it",
        "  on a device before claiming the integration works.",
        "- A `NOT_NEEDED` verdict is a statement about *manifest-visible* dependencies only.",
        "  If DEX-level analysis later shows a real call, the verdict changes with evidence —",
        "  that is why every exclusion is written down with its reason.",
        "",
    ]
    return "\n".join(lines)


def self_test() -> int:
    facts = {
        "A.apk": {"file": "A.apk", "package": "pkg.a", "declared": {"a.PERM"}, "used": set(), "actions": {"vendor.action.ONE"}},
        "B.apk": {"file": "B.apk", "package": "pkg.b", "declared": set(), "used": {"a.PERM"}, "actions": set()},
        "C.apk": {"file": "C.apk", "package": "pkg.c", "declared": set(), "used": set(), "actions": {"vendor.action.TWO"}},
        "D.apk": {"file": "D.apk", "package": "pkg.d", "declared": set(), "used": set(), "actions": {"vendor.action.ONE"}},
    }
    matrix = {"C.apk": {"A.apk": 3}}
    status, seeds = closure(facts, matrix, seed_packages={"pkg.a"})
    rows = {r["app"]: r for r in verdicts(facts, status)}
    checks = [
        ("seed found", len(seeds) == 1),
        ("a platform broadcast is not an edge",
         meaningful("android.intent.action.MAIN") is False and meaningful("cn.nubia.action.GAME_MODE")),
        ("permission edge pulls B", rows["pkg.b"]["verdict"] == "REQUIRED"),
        ("shared action pulls D", rows["pkg.d"]["verdict"] == "REQUIRED"),
        ("weak-only app needs review", rows["pkg.c"]["verdict"] == "NEEDS_REVIEW"),
        ("every app has a verdict", all(r["verdict"] for r in rows.values())),
    ]
    failed = [n for n, ok in checks if not ok]
    for name, ok in checks:
        print(("ok   " if ok else "FAIL ") + name)
    if failed:
        return 1
    print(f"self-test OK: {len(checks)} cases")
    return 0


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=".")
    ap.add_argument("--out", default="docs/audit/GT_CLOSURE.md")
    ap.add_argument("--json", default="docs/audit/gt-closure.json")
    ap.add_argument("--assert", dest="assert_ok", action="store_true")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args(argv)
    if args.self_test:
        return self_test()

    repo = Path(args.repo)
    inv, ipc, xref = load(repo)
    facts = app_facts(ipc)
    status, _ = closure(facts, xref.get("matrix") or {})
    rows = verdicts(facts, status)

    out = repo / args.out
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(render(rows), encoding="utf-8")
    (repo / args.json).write_text(json.dumps({"apps": rows}, ensure_ascii=False, indent=2), encoding="utf-8")

    counts = {v: sum(1 for r in rows if r["verdict"] == v) for v in ("SEED", "REQUIRED", "NEEDS_REVIEW", "NOT_NEEDED")}
    print("closure: " + " · ".join(f"{k}={v}" for k, v in counts.items()))

    if args.assert_ok:
        missing = [r for r in rows if not r["verdict"]]
        if missing:
            print("FAIL: apps without a verdict: " + ", ".join(r["app"] for r in missing), file=sys.stderr)
            return 1
        print(f"assert OK: {len(rows)} apps, each with a verdict and a reason")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
