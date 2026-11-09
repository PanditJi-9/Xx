#!/usr/bin/env python3
"""Run the production artwork geometry on a small JVM, without Gradle or Android.

--download caches a pinned test-only compiler from Maven Central outside the repo.
Each download is checked against Maven's published checksum. Missing dependencies
otherwise fail explicitly. --preview draws the actual production primitives, not
an Android screenshot. No product dependency or signing configuration is changed.
"""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "manager/app/src/main/java/nd/max/ui/design/MaxIllustrationModel.kt"
TEST = ROOT / "manager/app/src/test/java/nd/max/ui/design/MaxIllustrationModelTest.kt"
DEPS = {
    "compiler": ("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "2.3.10"),
    "stdlib": ("org.jetbrains.kotlin", "kotlin-stdlib", "2.3.10"),
    "reflect": ("org.jetbrains.kotlin", "kotlin-reflect", "1.6.10"),
    "annotations": ("org.jetbrains", "annotations", "13.0"),
    "coroutines": ("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.10.2"),
    "junit": ("junit", "junit", "4.13.2"),
    "hamcrest": ("org.hamcrest", "hamcrest-core", "1.3"),
}

PREVIEW = r'''
import nd.max.ui.design.*

fun main() {
    val kinds = MaxIllustrationKind.entries
    val columns = 5
    val rows = (kinds.size + columns - 1) / columns
    val svg = buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1000\" height=\"${rows * 190 + 70}\" viewBox=\"0 0 1000 ${rows * 190 + 70}\">")
        append("<rect width=\"100%\" height=\"100%\" fill=\"#0f1418\"/>")
        append("<text x=\"30\" y=\"42\" fill=\"#e2e8ec\" font-family=\"sans-serif\" font-size=\"22\">MaxManager — domain artwork</text>")
        kinds.forEachIndexed { index, kind ->
            val x = index % columns * 194 + 20
            val y = index / columns * 190 + 66
            append("<rect x=\"$x\" y=\"$y\" width=\"184\" height=\"176\" rx=\"18\" fill=\"#171e23\"/>")
            append("<g transform=\"translate(${x + 28},${y + 8}) scale(128)\" stroke-width=\"0.016\" stroke-linecap=\"round\" stroke-linejoin=\"round\">")
            for (part in maxIllustrationParts(kind)) {
                val ink = when (part.ink) {
                    MaxIllustrationInk.Accent -> "#8cd0f2"
                    MaxIllustrationInk.Outline -> "#a3adb5"
                    MaxIllustrationInk.Wash -> "#223b49"
                }
                when (part) {
                    is MaxIllustrationPart.Line -> {
                        val points = part.points.joinToString(" ") { "${it.x},${it.y}" }
                        val tag = if (part.closed) "polygon" else "polyline"
                        append("<$tag points=\"$points\" fill=\"none\" stroke=\"$ink\"/>")
                    }
                    is MaxIllustrationPart.Box -> append("<rect x=\"${part.x}\" y=\"${part.y}\" width=\"${part.width}\" height=\"${part.height}\" rx=\"${part.radius}\" fill=\"${if (part.filled) ink else "none"}\" stroke=\"${if (part.filled) "none" else ink}\"/>")
                    is MaxIllustrationPart.Ring -> append("<circle cx=\"${part.x}\" cy=\"${part.y}\" r=\"${part.radius}\" fill=\"${if (part.filled) ink else "none"}\" stroke=\"${if (part.filled) "none" else ink}\"/>")
                }
            }
            append("</g><text x=\"${x + 92}\" y=\"${y + 157}\" text-anchor=\"middle\" fill=\"#e2e8ec\" font-family=\"sans-serif\" font-size=\"15\">${kind.name}</text>")
        }
        append("</svg>")
    }
    print(svg)
}
'''


def jar(parser: argparse.ArgumentParser, cache: Path, dependency: tuple[str, str, str], download: bool) -> Path:
    group, artifact, version = dependency
    filename = f"{artifact}-{version}.jar"
    gradle = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"
    matches = sorted((gradle / group / artifact / version).glob(f"*/{filename}"))
    if matches:
        return matches[0]
    target = cache / filename
    if target.is_file():
        return target
    if not download:
        parser.error(f"Missing {filename}; use --download for the test-only toolchain")
    cache.mkdir(parents=True, exist_ok=True)
    url = f"https://repo.maven.apache.org/maven2/{group.replace('.', '/')}/{artifact}/{version}/{filename}"
    algorithm = "sha256"
    try:
        with urllib.request.urlopen(url + ".sha256", timeout=60) as response:
            expected = response.read().decode().strip().split()[0]
    except urllib.error.HTTPError as error:
        if error.code != 404:
            raise
        algorithm = "sha1"
        with urllib.request.urlopen(url + ".sha1", timeout=60) as response:
            expected = response.read().decode().strip().split()[0]
    digest = hashlib.new(algorithm)
    partial = target.with_suffix(".part")
    try:
        with urllib.request.urlopen(url, timeout=90) as response, partial.open("wb") as output:
            while block := response.read(1024 * 1024):
                digest.update(block)
                output.write(block)
        if digest.hexdigest().lower() != expected.lower():
            raise ValueError(f"Checksum mismatch for {filename}")
        partial.replace(target)
    finally:
        if partial.exists():
            partial.unlink()
    return target


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--download", action="store_true")
    parser.add_argument("--assert", dest="assert_", action="store_true", help="Compatibility with contract gates")
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--work-dir", type=Path, default=Path("/tmp/opencode"))
    args = parser.parse_args()
    args.work_dir.mkdir(parents=True, exist_ok=True)
    deps = {name: jar(parser, args.work_dir / "ui-identity-toolchain", spec, args.download) for name, spec in DEPS.items()}
    compiler_cp = os.pathsep.join(str(deps[name]) for name in ("compiler", "stdlib", "reflect", "annotations", "coroutines"))
    runtime_cp = os.pathsep.join(str(deps[name]) for name in ("stdlib", "annotations", "junit", "hamcrest"))
    with tempfile.TemporaryDirectory(prefix="ui-identity-jvm-", dir=args.work_dir) as directory:
        output = Path(directory)
        sources = [SOURCE, TEST]
        if args.preview:
            preview = output / "ArtworkPreview.kt"
            preview.write_text(PREVIEW, encoding="utf-8")
            sources.append(preview)
        subprocess.run([
            "java", "-Xmx384m", "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib", "-no-reflect", "-jvm-target", "17", "-classpath", runtime_cp,
            "-d", str(output), *map(str, sources),
        ], cwd=ROOT, check=True)
        classpath = str(output) + os.pathsep + runtime_cp
        subprocess.run([
            "java", "-Xmx128m", "-cp", classpath, "org.junit.runner.JUnitCore",
            "nd.max.ui.design.MaxIllustrationModelTest",
        ], cwd=ROOT, check=True)
        if args.preview:
            args.preview.parent.mkdir(parents=True, exist_ok=True)
            with args.preview.open("w", encoding="utf-8") as svg:
                subprocess.run(["java", "-Xmx128m", "-cp", classpath, "ArtworkPreviewKt"], stdout=svg, check=True)
            print(f"Artwork preview (not a device screenshot): {args.preview}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
