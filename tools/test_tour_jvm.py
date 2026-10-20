#!/usr/bin/env python3
"""Run the first-open tour guards (`HomeShapeContractTest`) without Gradle or Android.

Uses existing Gradle-cached jars; never downloads dependencies. This is not an Android
compilation: the guard reads source files as text and asserts the tour's wiring, which is
exactly the kind of claim a compiler cannot check for us.

What this measures (and why it exists rather than trusting a reading):

  * every `HomeTourTarget` still has an anchor **in its own screen file** — the tour now
    spans four tabs, so an anchor in the wrong file is a step that lights nothing;
  * every declared section is actually walked, in tab order, without repeats, and the walk
    ends in Settings (so "finish" has somewhere to return from);
  * the overlay navigates through the same primary-tab gate the bar uses, is hosted at the
    root of the window, and waits for an anchor **with a timeout** (no frozen tour);
  * stale anchors of other sections are cleared, so the hole cannot follow a screen that
    is no longer composed.

The Android-facing files are parser-checked only (`Kotlin syntax: N files, no parser
errors`) — no type checking, no Compose, no device.
"""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "manager/app/src/main/java/nd/max"
TEST = ROOT / "manager/app/src/test/java/nd/max"

KOTLIN = "2.3.10"
REFLECT_VERSION = "1.6.10"
COROUTINES = "1.10.2"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--work-dir", type=Path, default=Path(tempfile.gettempdir()))
    args = parser.parse_args()
    cache = Path(
        os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")
    ) / "caches/modules-2/files-2.1"

    def jar(group, artifact, version):
        matches = sorted(
            (cache / group / artifact / version).glob(f"*/{artifact}-{version}.jar")
        )
        if not matches:
            parser.error(
                f"Missing cached {group}:{artifact}:{version}; no dependency was downloaded"
            )
        return str(matches[0])

    stdlib = jar("org.jetbrains.kotlin", "kotlin-stdlib", KOTLIN)
    coroutines = jar("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", COROUTINES)
    annotations = jar("org.jetbrains", "annotations", "13.0")
    junit = jar("junit", "junit", "4.13.2")
    hamcrest = jar("org.hamcrest", "hamcrest-core", "1.3")
    compiler = jar("org.jetbrains.kotlin", "kotlin-compiler-embeddable", KOTLIN)
    reflect = jar("org.jetbrains.kotlin", "kotlin-reflect", REFLECT_VERSION)

    compiler_cp = os.pathsep.join([compiler, stdlib, coroutines, annotations, reflect])
    runtime_cp = os.pathsep.join([stdlib, coroutines, annotations, junit, hamcrest])
    # الحرّاس تقرأ الملفّات نصًّا؛ فلا تحتاج Compose ولا Android — وهذا سبب قابليّتها للقياس هنا.
    sources = [TEST / "ui/component/HomeShapeContractTest.kt"]
    tests = ["nd.max.ui.component.HomeShapeContractTest"]
    # الملفّات التي مسّها مدّ الجولة عبر التبويبات: إعراب فقط (لا فحص أنواع).
    touched = [
        "MainActivity.kt",
        "ui/mainscreens/HomeScreen.kt",
        "ui/mainscreens/HomeTourModel.kt",
        "ui/mainscreens/HomeTourOverlay.kt",
        "ui/mainscreens/LegendaryHomeDashboard.kt",
        "ui/mainscreens/ControlScreen.kt",
        "ui/mainscreens/ApplistScreen.kt",
        "ui/mainscreens/SettingsScreen.kt",
    ]

    with tempfile.TemporaryDirectory(prefix="tour-jvm-", dir=args.work_dir) as output:
        syntax = Path(output) / "CheckKotlinSyntax.java"
        syntax.write_text(
            '''
import java.nio.file.*;
import org.jetbrains.kotlin.cli.jvm.compiler.*;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
class CheckKotlinSyntax {
    public static void main(String[] paths) throws Exception {
        var disposable = Disposer.newDisposable();
        try {
            var env = KotlinCoreEnvironment.createForProduction(disposable,
                new CompilerConfiguration(), EnvironmentConfigFiles.JVM_CONFIG_FILES);
            var factory = new KtPsiFactory(env.getProject(), false);
            int errors = 0;
            for (var name : paths) {
                var path = Path.of(name);
                var file = factory.createFile(path.getFileName().toString(), Files.readString(path));
                for (var error : PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class)) {
                    System.err.println(name + ": " + error.getErrorDescription() + " at " + error.getTextOffset());
                    errors++;
                }
            }
            if (errors != 0) throw new AssertionError(errors + " Kotlin syntax errors");
            System.out.println("Kotlin syntax: " + paths.length + " files, no parser errors (not type checking)");
        } finally { Disposer.dispose(disposable); }
    }
}
''',
            encoding="utf-8",
        )
        parse_paths = [SOURCE / name for name in touched]
        present = [path for path in parse_paths if path.exists()]
        absent = [path.name for path in parse_paths if not path.exists()]
        if absent:
            print(f"parse: missing {len(absent)} file(s), skipped: {', '.join(absent)}")
        subprocess.run(
            ["java", "-cp", compiler_cp, str(syntax), *map(str, present)],
            cwd=ROOT,
            check=True,
        )
        subprocess.run(
            [
                "java", "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                "-no-stdlib", "-no-reflect", "-jvm-target", "17",
                "-classpath", runtime_cp,
                "-d", output, *map(str, sources),
            ],
            cwd=ROOT,
            check=True,
        )
        subprocess.run(
            ["java", "-cp", output + os.pathsep + runtime_cp, "org.junit.runner.JUnitCore", *tests],
            cwd=ROOT,
            check=True,
        )


if __name__ == "__main__":
    main()
