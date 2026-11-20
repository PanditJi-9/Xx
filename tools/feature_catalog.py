#!/usr/bin/env python3
"""Build docs/FEATURE_CATALOG.md from the REA evidence already committed.

Why this exists: the master plan's Phase 2 asks for a feature catalog per app, and the
only honest seed is what each APK declares plus what the reference traces measured.
So every row here carries its source and an explicit state:

    DECLARED    the manifest declares it (existence only, never behaviour)
    TRACED      a reference trace over the APK found call sites for it
    UNVERIFIED  nothing beyond the declaration was measured: it needs a device

Nothing in this file is inferred. A component that is declared but never traced stays
UNVERIFIED on purpose, and an app with no readable declaration is listed as such
instead of being dropped.

Usage:
    python3 tools/feature_catalog.py docs/audit/rea/evidence docs/FEATURE_CATALOG.md
    python3 tools/feature_catalog.py --self-test
"""

from __future__ import annotations

import json
import sys
import tempfile
from pathlib import Path

STATES = ("DECLARED", "TRACED", "UNVERIFIED")
KIND_LABEL = {
    "activity": "Activities (screens a user or another app can open)",
    "service": "Services (background surfaces, usually bound or started by other apps)",
    "receiver": "Receivers (broadcasts that wake the app)",
    "provider": "Providers (data handed to other apps)",
    "activity-alias": "Activity aliases",
}


def load_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return None


def traces(ev: Path) -> list[dict]:
    """One row per reference trace REA actually completed for this app."""
    rows = []
    for p in sorted(ev.glob("xref-*.json")):
        d = load_json(p) or {}
        nr = d.get("normalized_result") or {}
        rows.append(
            {
                "class": nr.get("class_name") or p.stem[len("xref-"):].replace("_", "."),
                "total": nr.get("total"),
                "coverage": nr.get("coverage") or "unknown",
                "refs": len(nr.get("references") or []),
            }
        )
    return rows


def component_rows(ev: Path) -> list[dict]:
    data = load_json(ev / "components.json")
    return [r for r in (data or []) if isinstance(r, dict) and r.get("name")]


def one_app(ev: Path) -> list[str]:
    meta = load_json(ev / "meta.json") or {}
    if not meta.get("package"):
        return [f"## {ev.name} — no readable declaration", "",
                "REA produced no package declaration for this APK in the recorded run, so no",
                "surface is claimed here. See `run.txt` in the same directory for the error.", ""]

    out = [f"## {ev.name} — `{meta['package']}`", ""]
    out += ["| measured | value |", "| --- | --- |",
            f"| sdk | min {meta.get('min_sdk')} / target {meta.get('target_sdk')} |",
            f"| classes | {meta.get('classes')} |",
            f"| declared components | {meta.get('components')} |",
            f"| strongest signature scheme | {meta.get('scheme') or '—'} |",
            f"| signer certificate sha256 | `{meta.get('cert') or '—'}` |", ""]

    rows = component_rows(ev)
    tr = traces(ev)
    traced = {t["class"] for t in tr if (t["total"] or 0) > 0}

    if not rows:
        out += ["No component list was committed for this app; surfaces stay unmeasured.", ""]
    for kind in ("activity", "activity-alias", "service", "receiver", "provider"):
        group = [r for r in rows if r.get("kind") == kind]
        if not group:
            continue
        exported = [r for r in group if r.get("exported")]
        out += [f"### {KIND_LABEL.get(kind, kind)}", "",
                f"{len(group)} declared, {len(exported)} exported.", "",
                "| component | exported | permission | filters | state |",
                "| --- | --- | --- | --- | --- |"]
        for r in sorted(group, key=lambda r: (not r.get("exported"), r.get("name") or "")):
            state = "TRACED" if r["name"] in traced else ("DECLARED" if r.get("exported") else "UNVERIFIED")
            out.append("| `{}` | {} | {} | {} | {} |".format(
                r["name"], bool(r.get("exported")), r.get("permission") or "—",
                r.get("filters") or 0, state))
        out.append("")

    if tr:
        out += ["### Reference traces measured inside this APK", "",
                "| target class | using-node sites | recorded references | coverage |",
                "| --- | --- | --- | --- |"]
        for t in tr:
            out.append(f"| `{t['class']}` | {t['total']} | {t['refs']} | {t['coverage']} |")
        out += ["", "A zero site count means no caller inside this APK refers to the class; it does",
                "not mean the class is unused, because the caller may live in another app.", ""]
    else:
        out += ["No reference trace completed for this app, so cross-app use stays unmeasured.", ""]
    return out


def render(root: Path) -> str:
    dirs = sorted(p for p in root.iterdir() if p.is_dir())
    out = [
        "# FEATURE_CATALOG — declared surfaces and how far the evidence reaches",
        "",
        "Generated by `tools/feature_catalog.py` from `docs/audit/rea/evidence/`. Regenerate,",
        "don't hand-edit: every row is derived from a recorded REA result.",
        "",
        "States: **DECLARED** the manifest declares it · **TRACED** a reference trace found",
        "call sites · **UNVERIFIED** only the declaration exists, so behaviour needs a device.",
        "",
        "Existence is not behaviour: a declared component may be dead on a given ROM, and a",
        "permission-protected surface only opens for a holder of that signature permission.",
        "",
        f"Apps covered by a saved evidence directory: {len(dirs)}.",
        "",
    ]
    for d in dirs:
        out += one_app(d)
    if not dirs:
        out += ["No evidence directories found — run the `rea-analysis` workflow first.", ""]
    return "\n".join(out).rstrip() + "\n"


def self_test() -> int:
    """Render a synthetic evidence tree; the tool must reproduce the known answers."""
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        a = root / "FakeApp"
        a.mkdir()
        (a / "meta.json").write_text(json.dumps({
            "package": "x.y", "min_sdk": 33, "target_sdk": 37,
            "classes": 10, "components": 3, "scheme": "v3",
            "cert": "aa" * 32}), encoding="utf-8")
        (a / "components.json").write_text(json.dumps([
            {"kind": "service", "name": "x.y.S", "exported": True, "permission": "x.y.P", "filters": 1},
            {"kind": "activity", "name": "x.y.A", "exported": False, "permission": None, "filters": 0},
            {"kind": "receiver", "name": "x.y.R", "exported": True, "permission": None, "filters": 2},
        ]), encoding="utf-8")
        (a / "xref-x.y.S.json").write_text(json.dumps({
            "normalized_result": {"class_name": "x.y.S", "total": 0, "references": [],
                                  "coverage": "complete"}}), encoding="utf-8")
        b = root / "Broken"
        b.mkdir()
        (b / "meta.json").write_text("{}", encoding="utf-8")
        text = render(root)
        checks = {
            "package rendered": "`x.y`" in text,
            "exported traced state": "| `x.y.S` | True | x.y.P | 1 | DECLARED |" in text,
            "non-exported state": "| `x.y.A` | False | — | 0 | UNVERIFIED |" in text,
            "receiver listed": "`x.y.R`" in text,
            "zero-site note": "| `x.y.S` | 0 | 0 | complete |" in text,
            "broken app named": "## Broken — no readable declaration" in text,
            "app count": "Apps covered by a saved evidence directory: 2." in text,
        }
        bad = [k for k, ok in checks.items() if not ok]
        for k, ok in checks.items():
            print(("ok   " if ok else "FAIL ") + k)
        return 1 if bad else 0


def main(argv: list[str]) -> int:
    if argv == ["--self-test"]:
        return self_test()
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    root, dest = Path(argv[0]), Path(argv[1])
    if not root.is_dir():
        print(f"not a directory: {root}", file=sys.stderr)
        return 2
    text = render(root)
    dest.write_text(text, encoding="utf-8")
    print(f"wrote {dest} ({len(text.splitlines())} lines, {len(text)} bytes)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
