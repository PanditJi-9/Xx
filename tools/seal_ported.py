#!/usr/bin/env python3
"""Seal the units the compiler accepted into the module's own source tree.

A ported unit is only ours once it lives in our repository. Until then it is regenerated from the
original APK on every port run: nobody can read it, review it, or fix it, and any change made to it
is lost at the next run.

This tool copies exactly the units that survived the compiler (`materials/sources` after the
pruning loop) into `apps/<app>/src/ported/java/`, writes `SEALED.json` with their paths so later
runs do not stage the same unit twice, and writes `PORTED.md` next to the module with what was
taken, what was refused and why.

The materials (res/, assets/, jniLibs/) stay out of git on purpose: they are binaries re-decoded
from the original APK, and a copy of them in the repository would only drift.
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
import tempfile
from datetime import date
from pathlib import Path


def normalized(relative: str) -> str:
    """jadx nests its output one `sources/` deep; the sealed tree must not inherit that prefix."""
    while relative.startswith("sources/"):
        relative = relative[len("sources/"):]
    return relative


# `R.java` and `BuildConfig.java` are sealed like any other unit on purpose: the ported sources
# need the decompiled `R` (it is the only place the original's `styleable` constants survive).
# Our own screen reads its strings by name instead of through that table.


def collect(sources: Path) -> dict[str, Path]:
    """`relative -> file` for every `.java` unit, nested prefix removed, collisions refused."""
    found: dict[str, Path] = {}
    for path in sorted(sources.rglob("*.java")):
        relative = normalized(path.relative_to(sources).as_posix())
        if relative in found:
            raise SystemExit(f"two units would seal to {relative}: {found[relative]} and {path}")
        found[relative] = path
    return found


def render(app: str, units: list[str], slice_info: dict, sealed_on: str) -> str:
    rejected = slice_info.get("rejected", {})
    libraries = sum(1 for reason in rejected.values() if reason.startswith("library code"))
    needs = sum(1 for reason in rejected.values() if reason.startswith("library not linked"))
    vendor = sum(1 for reason in rejected.values() if reason.startswith("vendor framework"))
    return "\n".join(
        [
            f"# {app} — ported source",
            "",
            "These files are ours: they are tracked by git and edited here, not regenerated.",
            "They were taken from the original APK (decompiled with jadx, package rewritten once by",
            "`tools/port_materials.py`) and then sealed by `tools/seal_ported.py` **only after the",
            "compiler accepted them** — the pruning loop drops every unit javac refuses.",
            "",
            f"- sealed units: **{len(units)}**",
            f"- sealed on: {sealed_on}",
            f"- corpus at the time: {slice_info.get('corpus', '?')} units",
            f"- rejected then: {len(rejected)} "
            f"(library code {libraries} · needs a dependency {needs} · vendor framework {vendor})",
            "",
            "`SEALED.json` lists the paths so a later port run stages only what is still missing.",
            "",
        ]
    )


def seal(sources: Path, target: Path, app: str, slice_info: dict) -> dict:
    """Add what the compiler accepted this run to what is already sealed.

    Sealing is add-only, and that is a measurement, not a preference: the pruning loop deletes every
    unit javac refuses from `materials/sources`, so a run where all remaining units were refused
    leaves that directory empty. Pruning the sealed tree from it deleted all 278 sealed units
    (run 44859682746). Nothing is removed here; a unit leaves the tree only when a human deletes it.
    """
    units = collect(sources)
    index = target / "SEALED.json"
    previous = json.loads(index.read_text(encoding="utf-8")) if index.is_file() else {}
    owned = set(previous.get("units", []))
    target.mkdir(parents=True, exist_ok=True)
    added = 0
    for relative, path in units.items():
        destination = target / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, destination)
        added += 1
    ordered = sorted(owned | set(units))
    removed: list[str] = []
    # The date is when the tree last grew, not when this tool last ran: otherwise PORTED.md would
    # change every run and every run would commit a file whose content is the same.
    sealed_on = date.today().isoformat() if set(units) - owned else previous.get("sealed", date.today().isoformat())
    index.write_text(
        json.dumps({"app": app, "units": ordered, "sealed": sealed_on}, indent=2) + "\n",
        encoding="utf-8",
    )
    (target.parents[2] / "PORTED.md").write_text(render(app, ordered, slice_info, sealed_on), encoding="utf-8")
    return {"written": added, "removed": removed, "units": ordered}


def self_test() -> int:
    checks = []
    with tempfile.TemporaryDirectory() as temporary:
        root = Path(temporary)
        sources = root / "materials" / "sources"
        (sources / "sources" / "cn" / "nubia" / "a").mkdir(parents=True)
        (sources / "sources" / "cn" / "nubia" / "a" / "A.java").write_text("class A {}\n")
        (sources / "sources" / "cn" / "nubia" / "B.java").write_text("class B {}\n")
        (sources / "sources" / "cn" / "nubia" / "common").mkdir()
        (sources / "sources" / "cn" / "nubia" / "common" / "R.java").write_text("package cn.nubia.common;\n")
        (sources / "sources" / "cn" / "nubia" / "gamelauncher").mkdir()
        (sources / "sources" / "cn" / "nubia" / "gamelauncher" / "R.java").write_text("package dev.redlab.gamespace;\n")
        target = root / "apps" / "demo" / "src" / "ported" / "java"
        (target / "cn" / "nubia").mkdir(parents=True)
        (target / "cn" / "nubia" / "Gone.java").write_text("class Gone {}\n")
        (target / "cn" / "nubia" / "HandEdited.java").write_text("class HandEdited {}\n")
        (target / "SEALED.json").write_text(json.dumps({"units": ["cn/nubia/Gone.java"]}))

        slice_info = {
            "corpus": 9,
            "rejected": {
                "kotlin/x/K.java": "library code: use the real dependency",
                "cn/nubia/C.java": "library not linked: androidx.x.C",
                "cn/nubia/D.java": "vendor framework: cn.nubia.Y",
            },
        }
        result = seal(sources, target, "Demo", slice_info)

        checks.append(("the nested prefix is stripped", (target / "cn/nubia/a/A.java").is_file()))
        checks.append(("a decompiled R is sealed like any unit", (target / "cn/nubia/gamelauncher/R.java").is_file()))
        checks.append(("no duplicate nesting is created", not (target / "sources").exists()))
        checks.append(("a sealed unit is never removed", (target / "cn/nubia/Gone.java").is_file()))
        checks.append(("nothing is reported as pruned", result["removed"] == []))
        checks.append(("a hand-added file is left alone", (target / "cn/nubia/HandEdited.java").is_file()))
        index = json.loads((target / "SEALED.json").read_text())
        checks.append(
            (
                "the index lists what was sealed",
                index["units"]
                == [
                    "cn/nubia/B.java",
                    "cn/nubia/Gone.java",
                    "cn/nubia/a/A.java",
                    "cn/nubia/common/R.java",
                    "cn/nubia/gamelauncher/R.java",
                ],
            )
        )
        report = (root / "apps" / "demo" / "PORTED.md").read_text()
        checks.append(("the report counts the units", "**5**" in report))
        checks.append(("the report separates the reasons", "library code 1" in report))

    failed = [name for name, ok in checks if not ok]
    for name, ok in checks:
        print(("ok   " if ok else "FAIL ") + name)
    if failed:
        print(f"self-test FAILED: {len(failed)}")
        return 1
    print(f"self-test OK: {len(checks)} cases")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--from", dest="sources", type=Path)
    parser.add_argument("--to", dest="target", type=Path)
    parser.add_argument("--app")
    parser.add_argument("--slice", dest="slice_path", type=Path)

    args = parser.parse_args()
    if args.self_test:
        return self_test()
    if not (args.sources and args.target and args.app):
        parser.error("--from, --to and --app are required")
    slice_info = json.loads(args.slice_path.read_text(encoding="utf-8")) if args.slice_path else {}
    result = seal(args.sources, args.target, args.app, slice_info)
    print(f"sealed: {result['written']} units, pruned: {len(result['removed'])}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
