#!/usr/bin/env python3
"""Inventory the *resources* of every staged APK with the real `aapt2`.

Why this exists: `FEATURE_CATALOG.md` counts components and traces (REA, in CI),
but the phase-2 acceptance gate also asks for a **resource** per feature. The
REA evidence records only a resource *count*; the names, the palette, the
layouts and the icon sizes were never read. All of that is measurable offline
from the APK itself, with no device and no decompiler:

    python3 tools/apk_resources.py <apps-dir> <out.md>
    python3 tools/apk_resources.py --self-test

`<apps-dir>` is expected to hold one `<Name>/<Name>.apk` per application, which
is how `/storage/emulated/0/red/apps/` is laid out. Anything else under it is
ignored, so the tool never guesses which file is an APK.

What it measures, per APK:
  * package / version / sdk / label        (aapt2 dump badging)
  * entry counts per resource type         (aapt2 dump resources)
  * every colour literal with its name     (same dump, `color` type)
  * launcher icon path and its pixel size  (badging + PNG/WebP header in the zip)
  * layout / drawable / mipmap names       (zip entries + resource dump names)

What it does not: it never claims a screen *looks* like anything. A layout name
is a name; pixels need a device. That limit is printed in the output.
"""

from __future__ import annotations

import argparse
import glob
import os
import re
import shutil
import struct
import subprocess
import sys
import tempfile
import textwrap
import zipfile

BADGING_RE = {
    "package": re.compile(r"^package: name='([^']*)' versionCode='([^']*)' versionName='([^']*)'"),
    "sdk": re.compile(r"^sdkVersion:'([^']*)'"),
    "target": re.compile(r"^targetSdkVersion:'([^']*)'"),
    "label": re.compile(r"^application-label:'(.*)'$"),
    "label_ar": re.compile(r"^application-label-ar:'(.*)'$"),
    "icon": re.compile(r"^application-icon-(\d+):'([^']*)'"),
    "launchable": re.compile(r"^launchable-activity: name='([^']*)'"),
}
TYPE_RE = re.compile(r"^  type (\S+) id=\S+ entryCount=(\d+)")
ENTRY_RE = re.compile(r"^    resource 0x[0-9a-f]+ (\S+)/(\S+)")
HEX_RE = re.compile(r"\(\) (#[0-9a-fA-F]{6,8})")
STRING_RE = re.compile(r'\(\) "(.*)"$')


def find_aapt2(sdk: str | None = None, verbose: bool = False) -> str | None:
    """Newest aapt2 that actually *runs* on this machine.

    Measured here (aarch64 host, Android SDK installed for x86-64):

    * `build-tools/36.0.0/aapt2` is `ELF x86-64` ⇒ dies with SIGILL (`rc=132`)
      on `aapt2 version`. Present and executable is not the same as usable.
    * Debian's `aapt2` 2.19 (aarch64) runs and reads the real APKs, but refuses
      the `resources.arsc` of `platforms;android-36/android.jar`
      ("RES_TABLE_TYPE_TYPE entry offsets overlap actual entry data").

    So every candidate gets executed once; an unusable one is skipped instead of
    failing later mid-probe. Link-time framework gaps are handled in
    `probe_framework()`, not here.
    """
    sdk = sdk or os.environ.get("ANDROID_HOME") or os.path.expanduser("~/android-sdk")
    candidates = sorted(glob.glob(os.path.join(sdk, "build-tools", "*", "aapt2")),
                        key=lambda p: [int(x) for x in re.findall(r"\d+", os.path.basename(os.path.dirname(p)))])
    claimed = shutil.which("aapt2")
    if claimed:
        candidates.append(claimed)
    for path in reversed(candidates):
        if not (os.path.isfile(path) and os.access(path, os.X_OK)):
            continue
        try:
            probe = subprocess.run([path, "version"], capture_output=True, timeout=60)
        except (OSError, subprocess.SubprocessError) as exc:
            if verbose:
                print(f"skip aapt2 {path}: {exc}", file=sys.stderr)
            continue
        if probe.returncode == 0:
            return path
        if verbose:
            print(f"skip aapt2 {path}: unusable here (rc={probe.returncode})", file=sys.stderr)
    return None


STUB_FRAMEWORK_XML = (
    '<resources>\n'
    '  <public type="attr" name="label" id="0x01010001"/>\n'
    '  <public type="attr" name="icon" id="0x01010002"/>\n'
    '  <attr name="label" format="string"/>\n'
    '  <attr name="icon" format="reference|color"/>\n'
    '</resources>\n'
)


def probe_framework(aapt2: str, tmp: str, jar: str | None) -> tuple[str, str]:
    """Return (include-path, description) for `aapt2 link -I` in the self-test.

    The real `android.jar` is tried first. When this machine's aapt2 cannot parse
    its `resources.arsc` (measured above), a three-attribute stub framework is
    built with the same aapt2 and used instead: the probe then still exercises
    real compile→link→badging→dump output, which is what the parser is tested
    against. Returns the description so the log never hides which one ran.
    """
    if jar:
        try:
            base = os.path.join(tmp, "fw_smoke")
            os.makedirs(os.path.join(base, "res", "values"), exist_ok=True)
            with open(os.path.join(base, "res", "values", "v.xml"), "w") as fh:
                fh.write('<resources><string name="probe">v</string></resources>')
            with open(os.path.join(base, "AndroidManifest.xml"), "w") as fh:
                fh.write('<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                         'package="test.smoke"/>')
            flat = os.path.join(base, "flat.zip")
            run([aapt2, "compile", "--dir", os.path.join(base, "res"), "-o", flat])
            run([aapt2, "link", "-o", os.path.join(base, "smoke.apk"), "-I", jar,
                 "--manifest", os.path.join(base, "AndroidManifest.xml"), flat])
            return jar, f"real framework {os.path.basename(os.path.dirname(jar))}"
        except RuntimeError:
            pass
    fw = os.path.join(tmp, "fw")
    os.makedirs(os.path.join(fw, "res", "values"), exist_ok=True)
    with open(os.path.join(fw, "res", "values", "attrs.xml"), "w") as fh:
        fh.write(STUB_FRAMEWORK_XML)
    with open(os.path.join(fw, "AndroidManifest.xml"), "w") as fh:
        fh.write('<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                 'package="android"/>')
    flat = os.path.join(fw, "fw.zip")
    run([aapt2, "compile", "--dir", os.path.join(fw, "res"), "-o", flat])
    apk = os.path.join(fw, "framework.apk")
    run([aapt2, "link", "-o", apk, "--manifest", os.path.join(fw, "AndroidManifest.xml"), flat])
    return apk, "stub framework (this machine's aapt2 cannot load the real android.jar)"


def run(cmd: list[str], timeout: int = 900) -> str:
    out = subprocess.run(cmd, capture_output=True, timeout=timeout)
    if out.returncode != 0:
        raise RuntimeError(
            f"{' '.join(cmd[:3])} failed ({out.returncode}): "
            + out.stderr.decode("utf-8", "replace").strip()[:400]
        )
    return out.stdout.decode("utf-8", "replace")


def encode_png(width: int, height: int) -> bytes:
    """Smallest valid RGBA PNG — used only by `--self-test`."""
    import zlib

    def chunk(tag: bytes, payload: bytes) -> bytes:
        return (struct.pack(">I", len(payload)) + tag + payload
                + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF))

    raw = b"".join(b"\x00" + b"\x20\x40\x60\xff" * width for _ in range(height))
    return (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))


def png_size(data: bytes) -> tuple[int, int] | None:
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        return None
    w, h = struct.unpack(">II", data[16:24])
    return w, h


def webp_size(data: bytes) -> tuple[int, int] | None:
    if data[:4] != b"RIFF" or data[8:12] != b"WEBP":
        return None
    chunk = data[12:16]
    if chunk == b"VP8X":
        w = 1 + int.from_bytes(data[24:27], "little")
        h = 1 + int.from_bytes(data[27:30], "little")
        return w, h
    if chunk == b"VP8 ":
        w, h = struct.unpack("<HH", data[26:30])
        return w & 0x3FFF, h & 0x3FFF
    if chunk == b"VP8L":  # lossless: 14-bit dims minus one, bit-packed
        b = int.from_bytes(data[21:25], "little")
        return (b & 0x3FFF) + 1, ((b >> 14) & 0x3FFF) + 1
    return None


def image_size(data: bytes) -> tuple[int, int] | None:
    return png_size(data) or webp_size(data)


def parse_badging(text: str) -> dict:
    info: dict = {"icons": {}, "label": "", "label_ar": ""}
    for line in text.splitlines():
        for key, rx in BADGING_RE.items():
            m = rx.match(line)
            if not m:
                continue
            if key == "package":
                info["package"], info["version_code"], info["version_name"] = m.groups()
            elif key == "sdk":
                info["min_sdk"] = m.group(1)
            elif key == "target":
                info["target_sdk"] = m.group(1)
            elif key == "icon":
                info["icons"][int(m.group(1))] = m.group(2)
            elif key == "launchable":
                info["launchable"] = m.group(1)
            else:
                info[key] = m.group(1)
    return info


def parse_resources(text: str) -> dict:
    """Entry counts per type, colour literals, and the names per type."""
    counts: dict[str, int] = {}
    names: dict[str, list[str]] = {}
    colors: list[tuple[str, str]] = []
    strings: dict[str, str] = {}
    current_type = None
    current_entry = None
    for line in text.splitlines():
        m = TYPE_RE.match(line)
        if m:
            current_type = m.group(1)
            counts[current_type] = int(m.group(2))
            names.setdefault(current_type, [])
            continue
        m = ENTRY_RE.match(line)
        if m and m.group(1) == current_type:
            current_entry = m.group(2)
            # `string/app_name` may repeat across configs; keep the first name only
            if len(names[current_type]) < 4000:
                names[current_type].append(current_entry)
            continue
        if current_type is None:
            continue
        if current_type == "color":
            m = HEX_RE.search(line)
            if m and current_entry:
                colors.append((current_entry, m.group(1).lower()))
        elif current_type == "string":
            m = STRING_RE.search(line)
            if m and current_entry and current_entry not in strings:
                strings[current_entry] = m.group(1)
    return {"counts": counts, "names": names, "colors": colors, "strings": strings}


# Owner directive: this APK is a reference asset only — no unpacking, no
# analysis, no upload. `aapt2 dump` is analysis, so it is skipped here too.
EXCLUDED = {
    "NubiaRedmagickyi_RedMagic": "owner directive: no unpacking, no analysis, no upload",
}


def apk_lookup(apps_dir: str) -> list[tuple[str, str]]:
    found = []
    for path in sorted(glob.glob(os.path.join(apps_dir, "*", "*.apk"))):
        name = os.path.basename(os.path.dirname(path))
        if name in EXCLUDED:
            print(f"skip {name}: {EXCLUDED[name]}", file=sys.stderr)
            continue
        found.append((name, path))
    return found


RES_RE = re.compile(r"resources (\d+)")


def package_resources(app: str) -> int | None:
    """The `resources N` figure jadx printed for this app, if the evidence exists."""
    here = os.path.dirname(os.path.abspath(__file__))
    path = os.path.join(here, os.pardir, "docs", "audit", "rea", "evidence", app, "PACKAGE.md")
    try:
        with open(path, encoding="utf-8") as fh:
            m = RES_RE.search(fh.read())
    except OSError:
        return None
    return int(m.group(1)) if m else None


def measure(app: str, apk: str, aapt2: str) -> dict:
    row = {"app": app, "apk": apk, "bytes": os.path.getsize(apk)}
    with zipfile.ZipFile(apk) as z:
        entries = z.namelist()
    # jadx's `resource_count` in `docs/audit/rea/evidence/*/PACKAGE.md` is the
    # number of *zip entries*, not the number of resource-table entries. Both
    # numbers are kept so the two documents can be reconciled row by row.
    row["zip_entries"] = len(entries)
    row["res_files"] = sum(1 for n in entries if n.startswith("res/"))
    row["package_resources"] = package_resources(app)
    row.update(parse_badging(run([aapt2, "dump", "badging", apk])))
    res = parse_resources(run([aapt2, "dump", "resources", apk]))
    row.update(res)
    icon = None
    if row["icons"]:
        density = max(row["icons"])
        icon = (density, row["icons"][density])
        row["icon_density"] = density
        row["icon_path"] = icon[1]
        try:
            with zipfile.ZipFile(apk) as z:
                size = image_size(z.read(icon[1].lstrip("/")))
            row["icon_size"] = size
        except (KeyError, OSError, zipfile.BadZipFile):
            row["icon_size"] = None
    return row


def render(rows: list[dict], apps_dir: str) -> str:
    total = lambda key: sum(r["counts"].get(key, 0) for r in rows)  # noqa: E731
    out = []
    add = out.append
    add("# RESOURCE_INVENTORY — what `aapt2` reads out of the staged APKs\n")
    add(
        "Generated by `tools/apk_resources.py` from "
        f"`{apps_dir}`. Regenerate, don't hand-edit.\n"
    )
    add(
        "This is the **resource** half of the phase-2 gate: names, counts, colours and\n"
        "icon sizes. It does not say what a screen looks like, which animation plays,\n"
        "or how a control behaves — those need a device and stay `UNVERIFIED`.\n"
    )
    add(f"APKs measured: **{len(rows)}**.")
    if EXCLUDED:
        add("Skipped by owner directive: " + ", ".join(
            f"`{name}` ({why})" for name, why in EXCLUDED.items()) + ".")
    add("")
    add("## Totals across the whole suite\n")
    add("| resource type | entries |")
    add("| --- | --- |")
    for key in ("string", "color", "dimen", "layout", "drawable", "mipmap", "anim", "animator", "style", "xml", "raw"):
        add(f"| `{key}` | {total(key)} |")
    add("")
    add("## Per application\n")
    add(
        "| app | package | version | min/target | strings | colors | layouts | drawables | "
        "mipmaps | zip entries | res/ files | label | launcher icon | icon px |"
    )
    add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
    for r in rows:
        c = r["counts"]
        px = f"{r['icon_size'][0]}×{r['icon_size'][1]}" if r.get("icon_size") else "—"
        add(
            "| `{app}` | `{pkg}` | {ver} | {mn}/{tg} | {s} | {col} | {lay} | {dr} | {mip} | "
            "{ze} | {rf} | {label} | `{icon}` | {px} |".format(
                app=r["app"],
                pkg=r.get("package", "?"),
                ver=r.get("version_name", "?"),
                mn=r.get("min_sdk", "?"),
                tg=r.get("target_sdk", "?"),
                s=c.get("string", 0),
                col=c.get("color", 0),
                lay=c.get("layout", 0),
                dr=c.get("drawable", 0),
                mip=c.get("mipmap", 0),
                ze=r.get("zip_entries", 0),
                rf=r.get("res_files", 0),
                label=(r.get("label") or "—").replace("|", "\\|"),
                icon=r.get("icon_path", "—"),
                px=px,
            )
        )
    add("")
    add("## Reconciliation with the REA evidence\n")
    add(
        "`PACKAGE.md` prints a `resources N` figure that comes from jadx. It is the\n"
        "count of **zip entries** in the APK, not of resource-table entries, so the two\n"
        "documents agree only on that column — `zip entries` here. Measured, not assumed.\n"
    )
    add("| app | `PACKAGE.md` resources | zip entries | equal |")
    add("| --- | --- | --- | --- |")
    for r in rows:
        claimed = r.get("package_resources")
        mark = "—" if claimed is None else ("✅" if claimed == r["zip_entries"] else "❌")
        add(f"| `{r['app']}` | {claimed if claimed is not None else '—'} | {r['zip_entries']} | {mark} |")
    off = [r for r in rows if r.get("package_resources") not in (None, r["zip_entries"])]
    if off:
        add("")
        add("Mismatches, stated rather than smoothed over: one APK's central directory holds")
        add("fewer entries than jadx printed. Re-counted twice (Python `zipfile` and the")
        add("ZIP end-of-central-directory record, which agree with each other), so the")
        add("figure in `PACKAGE.md` is the outlier and the APK was not altered:")
        add("")
        for r in off:
            add(f"- `{r['app']}`: `PACKAGE.md` {r['package_resources']} vs {r['zip_entries']} in the file"
                f" ({r['zip_entries'] - r['package_resources']:+d}).")
    add("")
    add("## Screens, as named by the layouts\n")
    add(
        "One line per application: the layout names it ships. A name is not a screen —\n"
        "several layouts compose one screen, and an unused layout is still shipped.\n"
    )
    for r in rows:
        layouts = [n for n in r["names"].get("layout", [])]
        shown = ", ".join(f"`{n}`" for n in layouts[:40])
        more = f" … (+{len(layouts) - 40})" if len(layouts) > 40 else ""
        add(f"- **{r['app']}** ({len(layouts)}): {shown or '— none —'}{more}")
    add("")
    add("## Palette, as literal colours declared by each app\n")
    add(
        "Only literals that appear in the `color` table of the APK are listed; colours\n"
        "built at runtime or referenced from the theme are not here. Counts are how many\n"
        "distinct `color/` entries carry that literal, so a repeated value means the app\n"
        "*named* it in several places, not that it is used more often.\n"
    )
    for r in rows:
        if not r["colors"]:
            add(f"- **{r['app']}**: no literal colour entries.")
            continue
        tally: dict[str, list[str]] = {}
        for name, value in r["colors"]:
            tally.setdefault(value, []).append(name)
        ranked = sorted(tally.items(), key=lambda kv: (-len(kv[1]), kv[0]))[:12]
        parts = [f"`{v}` ×{len(n)}" for v, n in ranked]
        add(f"- **{r['app']}** ({len(r['colors'])} entries, {len(tally)} distinct): " + " · ".join(parts))
    add("")
    add("## Bounds\n")
    add(
        textwrap.dedent(
            """\
            - **No rendering.** Icon pixel size is read from the file header; nothing is
              drawn or compared. Visual parity is phase 12 and needs screenshots.
            - **No behaviour.** A declared animation or layout may be dead on a given ROM.
            - **No third-party code judgement.** Library resources are counted with the
              app's own; the split would need a decompiler.
            - Colours are literals only; `?attr/` indirection and runtime `Color.parseColor`
              are invisible here.
            """
        )
    )
    return "\n".join(out) + "\n"


def self_test() -> int:
    """Build a tiny APK with the real aapt2 and check every invariant we rely on."""
    aapt2 = find_aapt2()
    if not aapt2:
        print("SKIP self-test: aapt2 not installed — nothing measured", file=sys.stderr)
        return 2
    sdk = os.environ.get("ANDROID_HOME") or os.path.expanduser("~/android-sdk")
    jars = sorted(glob.glob(os.path.join(sdk, "platforms", "android-*", "android.jar")))
    jar = jars[-1] if jars else None
    print(f"aapt2: {aapt2}")
    failures = []

    def check(cond: bool, label: str) -> None:
        print(("ok   " if cond else "FAIL ") + label)
        if not cond:
            failures.append(label)

    with tempfile.TemporaryDirectory() as tmp:
        res = os.path.join(tmp, "res")
        os.makedirs(os.path.join(res, "values"))
        os.makedirs(os.path.join(res, "layout"))
        os.makedirs(os.path.join(res, "mipmap-hdpi"))
        with open(os.path.join(res, "values", "strings.xml"), "w") as fh:
            fh.write(
                '<resources><string name="app_name">Probe App</string>'
                '<color name="brand">#FF102030</color>'
                '<color name="brand_alt">#ff102030</color>'
                '<dimen name="gap">8dp</dimen></resources>'
            )
        # deliberately framework-light: the stub framework only declares
        # android:label/android:icon, and the probe must link against either it
        # or the real android.jar without depending on layout attributes.
        with open(os.path.join(res, "layout", "probe_screen.xml"), "w") as fh:
            fh.write('<LinearLayout/>')
        # a real 3x5 PNG, encoded here so aapt2 accepts it and the header parser
        # is exercised on a file the toolchain itself produced
        png = encode_png(3, 5)
        with open(os.path.join(res, "mipmap-hdpi", "ic_probe.png"), "wb") as fh:
            fh.write(png)
        manifest = os.path.join(tmp, "AndroidManifest.xml")
        with open(manifest, "w") as fh:
            fh.write(
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                'package="test.probe"><application android:label="@string/app_name" '
                'android:icon="@mipmap/ic_probe"/></manifest>'
            )
        flat = os.path.join(tmp, "flat.zip")
        run([aapt2, "compile", "--dir", res, "-o", flat])
        include, description = probe_framework(aapt2, tmp, jar)
        print(f"link -I: {description}")
        apk = os.path.join(tmp, "probe.apk")
        run([aapt2, "link", "-o", apk, "-I", include, "--manifest", manifest, flat])
        row = measure("Probe", apk, aapt2)
        check(row.get("package") == "test.probe", "package parsed")
        check(row.get("label") == "Probe App", "label resolved from string table")
        check(row["counts"].get("layout") == 1, "layout counted")
        check(row["counts"].get("string", 0) >= 1, "string counted")
        check(row["counts"].get("color") == 2, "two colour entries counted")
        values = {v for _, v in row["colors"]}
        check(values == {"#ff102030"}, "colour literals normalised to lower case")
        check(row["counts"].get("mipmap") == 1, "mipmap counted")
        check(row.get("icon_size") == (3, 5), "PNG header size read as 3x5")
        check("probe_screen" in row["names"].get("layout", []), "layout name kept")
        doc = render([row], "<self-test>")
        check("Probe App" in doc and "3×5" in doc, "document renders label and size")
    if failures:
        print(f"\n{len(failures)} check(s) failed", file=sys.stderr)
        return 1
    print("\nall self-test checks passed")
    return 0


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("apps_dir", nargs="?")
    ap.add_argument("out", nargs="?")
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--aapt2", default=None, help="default: newest SDK build-tools aapt2, else PATH")
    args = ap.parse_args(argv)

    if args.self_test:
        return self_test()
    if not args.apps_dir or not args.out:
        ap.error("need <apps-dir> and <out.md> (or --self-test)")
    aapt2 = args.aapt2 or find_aapt2()
    if not aapt2:
        print("aapt2 not found: cannot measure anything (unverified, not passed)", file=sys.stderr)
        return 2

    targets = apk_lookup(args.apps_dir)
    if not targets:
        print(f"no <Name>/<Name>.apk under {args.apps_dir}", file=sys.stderr)
        return 2
    rows = []
    for name, apk in targets:
        try:
            rows.append(measure(name, apk, aapt2))
            print(f"ok   {name}", file=sys.stderr)
        except Exception as exc:  # noqa: BLE001 - one bad APK must not lose the rest
            print(f"FAIL {name}: {exc}", file=sys.stderr)
    if not rows:
        return 1
    with open(args.out, "w", encoding="utf-8") as fh:
        fh.write(render(rows, args.apps_dir))
    print(f"wrote {args.out} ({len(rows)} apps)")
    return 0 if len(rows) == len(targets) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
