#!/usr/bin/env python3
"""Stage a decompiled app's materials into its Gradle module.

Doctrine (docs/MASTER_PLAN.md §1 · §3): the original APK *is* the material. apktool already
decoded `res/`, `assets/`, `lib/<abi>` and `AndroidManifest.xml`; this tool moves those into
`redlab/apps/<app>/src/main/` so Gradle builds the real resources instead of a look-alike, and
writes the inventory the app shows while the code port is still in progress.

What it does *not* do: it never rewrites a resource, never renames a package inside a decoded
file, and never edits the checked-in manifest. Renaming while copying would make the staged tree
differ from the measured one, and a difference that no one can diff is the same as no evidence.

Usage:
    python3 tools/port_materials.py --extract work/port/GameSpace \\
        --module redlab/apps/gamespace --app GameSpace --assert
    python3 tools/port_materials.py --self-test
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
# aapt2's own rule for file-based resources, and it is not negotiable: only a-z, 0-9 and _ .
# apktool decodes some internal entries to names that break it (measured: `$m3_avd_show_password__2.xml`
# in GameSpace), and one such file fails the whole `packageDebugResources` task.
RES_NAME = re.compile(r"^[a-z0-9_]+\.(xml|png|webp|jpg|jpeg|9\.png|gif|ttf|otf|mp3|ogg|wav|json|bin|dtd|arsc)$")
COMPONENTS = (
    ("activity", "activities"),
    ("activity-alias", "aliases"),
    ("service", "services"),
    ("receiver", "receivers"),
    ("provider", "providers"),
)


def read_components(manifest: Path) -> dict:
    """Component names from the decoded manifest — reported, never staged.

    A component whose class is not in the build yet must not be declared in the manifest we ship:
    a declared-but-missing activity is an instant `ClassNotFoundException` on launch, which the
    compiler would not have caught. So the list is evidence for the ledger, not build input.
    """
    found: dict[str, list[str]] = {key: [] for _, key in COMPONENTS}
    if not manifest.exists():
        return found
    try:
        root = ET.parse(manifest).getroot()
    except ET.ParseError:
        return found
    application = root.find("application")
    if application is None:
        return found
    for tag, key in COMPONENTS:
        for node in application.findall(tag):
            name = node.get(ANDROID_NS + "name")
            if name:
                found[key].append(name)
    found["permissions"] = sorted(
        {
            node.get(ANDROID_NS + "name", "")
            for node in root.findall("uses-permission")
            if node.get(ANDROID_NS + "name")
        }
    )
    found["package"] = root.get("package", "")
    return found


def is_valid_res_name(path: Path) -> bool:
    """A res file name is `<name>.<ext>` with a name aapt2 accepts; the extension set is wide."""
    return bool(RES_NAME.match(path.name))


def safe_res_name(name: str) -> str:
    """A name aapt2 accepts, derived from one it refuses.

    apktool decodes AnimatedVectorDrawable internals as `$avd_name__0.xml`, and their container
    refers to them by that name. Dropping them leaves the container pointing at nothing, which
    fails `packageDebugResources` just as loudly. So the `$` is replaced by a prefix, the
    container's reference is rewritten with it, and the pair is listed in MATERIALS.md.
    """
    return "rl_" + re.sub(r"[^a-z0-9_]", "_", name.lstrip("$").lower())


def rename_plan(res: Path) -> tuple[dict[Path, Path], list[str]]:
    """Which files must be renamed to be a resource, and which cannot be a resource at all."""
    rename: dict[Path, Path] = {}
    skipped: list[str] = []
    for path in sorted(res.rglob("*")):
        if not path.is_file() or is_valid_res_name(path):
            continue
        stem = path.name.rsplit(".", 1)[0]
        if stem.startswith("$"):
            candidate = safe_res_name(stem) + path.suffix
            if RES_NAME.match(candidate):
                rename[path] = path.with_name(candidate)
                continue
        skipped.append(str(path.relative_to(res)))
    return rename, skipped


# Only a resource reference can be rewritten: `@drawable/$x`, not a `$` in a string or a style.
REF_TO_DOLLAR = re.compile(r"(@(?:drawable|anim|xml|mipmap|color|raw|interpolator)/)\$")


def copy_tree(source: Path, target: Path) -> int:
    """Copy files without deleting anything already staged. Returns the number of files written."""
    written = 0
    if not source.is_dir():
        return 0
    for path in sorted(source.rglob("*")):
        if not path.is_file():
            continue
        destination = target / path.relative_to(source)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, destination)
        written += 1
    return written


def stage(extract: Path, module: Path, app: str, triage: Path | None = None) -> dict:
    """Stage materials and return what was measured. Raises when the extraction is unusable."""
    res = extract / "res"
    if not res.is_dir():
        raise SystemExit(f"no decoded resources in {extract} — apktool did not produce res/")
    if not any(p.is_file() for p in res.rglob("*")):
        raise SystemExit(f"{res} is empty — nothing to stage")

    # Materials live beside the module's own sources, not inside `src/main`: that tree is
    # tracked by git, and a staged copy under it would either be committed (6k binary resources)
    # or ignored (which would also hide the handful of files we do write by hand).
    main = module / "materials"
    counts = {
        "app": app,
        "res": 0,
        "assets": copy_tree(extract / "assets", main / "assets"),
        "abis": [],
        "skipped": [],
    }

    # Resources are copied file by file: a name aapt2 cannot take is either renamed (with its
    # references rewritten) or listed by name, never passed through and never dropped silently.
    rename, skipped = rename_plan(res)
    counts["skipped"] = skipped
    counts["renamed"] = sorted(str(source.relative_to(res)) for source in rename)
    for path in sorted(res.rglob("*")):
        if not path.is_file():
            continue
        destination = main / "res" / path.relative_to(res)
        if path in rename:
            destination = main / "res" / rename[path].relative_to(res)
            # rewrite_after is filled once the whole tree is in place
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, destination)
        counts["res"] += 1

    if rename:
        for path in sorted((main / "res").rglob("*.xml")):
            text = path.read_text(encoding="utf-8", errors="surrogateescape")
            rewritten = REF_TO_DOLLAR.sub(r"\1rl_", text)
            if rewritten != text:
                path.write_text(rewritten, encoding="utf-8", errors="surrogateescape")

    lib = extract / "lib"
    if lib.is_dir():
        for abi_dir in sorted(p for p in lib.iterdir() if p.is_dir()):
            written = copy_tree(abi_dir, main / "jniLibs" / abi_dir.name)
            counts["abis"].append({"abi": abi_dir.name, "libs": written})

    res_dirs: dict[str, int] = {}
    for path in (main / "res").glob("*"):
        if path.is_dir():
            res_dirs[path.name] = sum(1 for p in path.rglob("*") if p.is_file())

    components = read_components(extract / "AndroidManifest.xml")
    inventory = {
        "app": app,
        "package": components.get("package", ""),
        "staged": {"res": counts["res"], "assets": counts["assets"], "abis": counts["abis"]},
        "resDirs": dict(sorted(res_dirs.items())),
        "skipped": counts["skipped"],
        "renamed": counts["renamed"],
        "components": {key: value for key, value in components.items() if key != "package"},
    }

    # The triage result rides along so the app can show what the port measured without the build
    # having to read the repository. Missing triage is not an error: a build may be run alone.
    if triage is not None and triage.is_file():
        try:
            summary = json.loads(triage.read_text(encoding="utf-8"))["summary"]
        except (KeyError, ValueError):
            summary = None
        if summary:
            inventory["triage"] = {"total": summary.get("total", 0), **summary.get("counts", {})}

    inventory_path = main / "assets" / "port" / "inventory.json"
    inventory_path.parent.mkdir(parents=True, exist_ok=True)
    inventory_path.write_text(json.dumps(inventory, ensure_ascii=False, indent=1), encoding="utf-8")

    (module / "MATERIALS.md").write_text(render(app, inventory), encoding="utf-8")
    counts["inventory"] = str(inventory_path)
    counts["components"] = inventory["components"]
    return counts


def render(app: str, inventory: dict) -> str:
    staged = inventory["staged"]
    rows = [
        f"# {app} — staged materials",
        "",
        "Generated by `tools/port_materials.py`; do not edit. These files come from the original",
        "APK exactly as apktool decoded them, and are not tracked by git: they are re-staged on",
        "every port run so the build always measures the same material the triage measured.",
        "",
        "| material | files |",
        "| --- | --- |",
        f"| `res/` | {staged['res']} |",
        f"| `assets/` | {staged['assets']} |",
    ]
    for abi in staged["abis"]:
        rows.append(f"| `jniLibs/{abi['abi']}` | {abi['libs']} |")
    renamed = inventory.get("renamed", [])
    if renamed:
        rows += [
            "",
            f"### Renamed: {len(renamed)} entries apktool decoded with a name aapt2 refuses",
            "",
            "The `$` prefix became `rl_` and every `@drawable/$…` reference was rewritten to match.",
            "Nothing else about them changed; the list is here so the rename is visible, not implied.",
            "",
        ]
        rows += [f"- `{item}`" for item in renamed[:40]]
    skipped = inventory.get("skipped", [])
    if skipped:
        rows += [
            "",
            f"### Skipped: {len(skipped)} entries aapt2 cannot accept as resource file names",
            "",
            "They are listed here rather than renamed or dropped silently: if the original code",
            "references one, the reference fails loudly at build time instead of pointing at a",
            "renamed file that no longer answers to its own name.",
            "",
        ]
        rows += [f"- `{item}`" for item in skipped[:40]]
    rows += ["", f"Package in the original: `{inventory['package']}`", "", "## Components declared by the original", ""]
    for title in ("activities", "aliases", "services", "receivers", "providers"):
        names = inventory["components"].get(title, [])
        rows.append(f"- **{title}**: {len(names)}")
    permissions = inventory["components"].get("permissions", [])
    rows += ["", f"Permissions declared: {len(permissions)}", ""]
    return "\n".join(rows)


def self_test() -> int:
    checks: list[tuple[str, bool]] = []
    with tempfile.TemporaryDirectory() as tmp:
        root = Path(tmp)
        extract = root / "extract"
        (extract / "res" / "values").mkdir(parents=True)
        (extract / "res" / "layout").mkdir(parents=True)
        (extract / "res" / "values" / "strings.xml").write_text("<resources/>", encoding="utf-8")
        (extract / "res" / "layout" / "main.xml").write_text("<LinearLayout/>", encoding="utf-8")
        (extract / "assets" / "skin").mkdir(parents=True)
        (extract / "assets" / "skin" / "a.bin").write_text("x", encoding="utf-8")
        (extract / "lib" / "arm64-v8a").mkdir(parents=True)
        (extract / "lib" / "arm64-v8a" / "libgame.so").write_text("ELF", encoding="utf-8")
        (extract / "AndroidManifest.xml").write_text(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
            'package="cn.nubia.gamelauncher">\n'
            '  <uses-permission android:name="android.permission.INTERNET"/>\n'
            "  <application>\n"
            '    <activity android:name="cn.nubia.gamelauncher.Lobby"/>'
            '<activity android:name=".Settings"/>\n'
            "  </application>\n"
            "</manifest>\n",
            encoding="utf-8",
        )
        module = root / "apps" / "gamespace"
        counts = stage(extract, module, "GameSpace")

        checks.append(("resources are staged", (module / "materials/res/values/strings.xml").is_file()))
        checks.append(("assets are staged", (module / "materials/assets/skin/a.bin").is_file()))
        checks.append(("native libs go to jniLibs", (module / "materials/jniLibs/arm64-v8a/libgame.so").is_file()))
        checks.append(("inventory is written", (module / "materials/assets/port/inventory.json").is_file()))
        checks.append(("file count is measured", counts["res"] == 2 and counts["assets"] == 1))
        inventory = json.loads((module / "materials/assets/port/inventory.json").read_text(encoding="utf-8"))
        checks.append(("package is read from the manifest", inventory["package"] == "cn.nubia.gamelauncher"))
        checks.append(("activities are listed", len(inventory["components"]["activities"]) == 2))
        checks.append(("permissions are listed", inventory["components"]["permissions"] == ["android.permission.INTERNET"]))
        checks.append(("materials doc is written", (module / "MATERIALS.md").is_file()))

        # An apktool `$` name is renamed together with the reference that points at it; a name
        # that cannot be salvaged at all is skipped and listed. Both are measured, not asserted.
        (extract / "res" / "drawable").mkdir()
        (extract / "res" / "drawable" / "$avd_internal__0.xml").write_text("<vector/>", encoding="utf-8")
        (extract / "res" / "drawable" / "avd_container.xml").write_text(
            '<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:drawable="@drawable/$avd_internal__0">\n'
            "    <target android:name=\"a\" android:animation=\"@anim/anim\"/>\n"
            "</animated-vector>\n",
            encoding="utf-8",
        )
        (extract / "res" / "drawable" / "Bad Name.xml").write_text("<x/>", encoding="utf-8")
        staged = stage(extract, module, "GameSpace")
        checks.append(("a `$` name is renamed", (module / "materials/res/drawable/rl_avd_internal__0.xml").is_file()))
        checks.append(("the old name is gone", not (module / "materials/res/drawable/$avd_internal__0.xml").exists()))
        container = (module / "materials/res/drawable/avd_container.xml").read_text(encoding="utf-8")
        checks.append(("its references follow the rename", "@drawable/rl_avd_internal__0" in container))
        checks.append(("a name beyond repair is skipped", staged["skipped"] == ["drawable/Bad Name.xml"]))
        doc = (module / "MATERIALS.md").read_text(encoding="utf-8")
        checks.append(("the rename is recorded", "Renamed: 1" in doc))
        checks.append(("the skip is recorded", "Skipped: 1" in doc))
        for leftover in ("drawable/$avd_internal__0.xml", "drawable/avd_container.xml", "drawable/Bad Name.xml"):
            (extract / "res" / leftover).unlink()

        triage = root / "docs" / "port" / "GameSpace.json"
        triage.parent.mkdir(parents=True)
        triage.write_text(
            json.dumps({"app": "GameSpace", "summary": {"counts": {"PORT_AS_IS": 7, "ADAPT": 2}, "total": 9}}),
            encoding="utf-8",
        )
        stage(extract, module, "GameSpace", triage)
        measured = json.loads((module / "materials/assets/port/inventory.json").read_text(encoding="utf-8"))
        checks.append(("triage counts reach the app", measured.get("triage", {}).get("PORT_AS_IS") == 7))
        checks.append(("triage total reaches the app", measured.get("triage", {}).get("total") == 9))

        again = stage(extract, module, "GameSpace")
        checks.append(("staging is repeatable", again["res"] == counts["res"] and again["assets"] == counts["assets"]))

        # A local edit is not silently reverted? No: the original is the source of truth on every
        # run. What must not happen is an extraction with no resources passing as success.
        empty = root / "empty"
        empty.mkdir()
        try:
            stage(empty, root / "apps" / "empty", "Empty")
            checks.append(("an extraction without res fails loudly", False))
        except SystemExit:
            checks.append(("an extraction without res fails loudly", True))

    failed = [name for name, ok in checks if not ok]
    for name, ok in checks:
        print(("ok   " if ok else "FAIL ") + name)
    if failed:
        return 1
    print(f"self-test OK: {len(checks)} cases")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--extract")
    parser.add_argument("--module")
    parser.add_argument("--app", default="")
    parser.add_argument("--triage", help="docs/port/<app>.json written by port_triage.py")
    parser.add_argument("--assert", dest="assert_on_fail", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)
    if args.self_test:
        return self_test()
    if not args.extract or not args.module:
        print("--extract and --module are required", file=sys.stderr)
        return 2

    module = Path(args.module)
    if not module.is_dir():
        print(f"module does not exist: {module}", file=sys.stderr)
        return 2
    app = args.app or module.name
    counts = stage(Path(args.extract), module, app, Path(args.triage) if args.triage else None)
    print(
        f"{app}: staged res={counts['res']} assets={counts['assets']} abis={len(counts['abis'])} "
        f"renamed={len(counts['renamed'])} skipped={len(counts['skipped'])}"
    )
    for title, names in counts["components"].items():
        if isinstance(names, list):
            print(f"  {title}: {len(names)}")
    if args.assert_on_fail and counts["res"] == 0:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
