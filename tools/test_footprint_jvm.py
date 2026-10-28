#!/usr/bin/env python3
"""Run footprint policy/wiring tests without a Gradle/NDK build or downloads.

Compiles the real pure policy and JUnit tests with cached Kotlin 2.3.10 jars.
Android-facing sources are checked with Kotlin's parser, not type-checked.
Source wiring guards do not prove FileObserver delivery or battery use on a phone.
"""

import argparse
import os
from pathlib import Path
import re
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "manager/app/src/main/java/nd/max"
TEST = ROOT / "manager/app/src/test/java/nd/max/core/daemon"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--work-dir", type=Path, default=Path("/tmp/opencode"))
    parser.add_argument("--self-test", action="store_true", help="prove the parser and two new wiring guards reject regressions")
    args = parser.parse_args()
    cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"

    def jar(group, artifact, version):
        matches = sorted((cache / group / artifact / version).glob(f"*/{artifact}-{version}.jar"))
        if not matches:
            parser.error(f"Missing cached {group}:{artifact}:{version}; no downloads were attempted")
        return str(matches[0])

    stdlib = jar("org.jetbrains.kotlin", "kotlin-stdlib", "2.3.10")
    compiler = jar("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "2.3.10")
    reflect = jar("org.jetbrains.kotlin", "kotlin-reflect", "1.6.10")
    coroutines = jar("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.10.2")
    annotations = jar("org.jetbrains", "annotations", "13.0")
    junit = jar("junit", "junit", "4.13.2")
    hamcrest = jar("org.hamcrest", "hamcrest-core", "1.3")
    compiler_cp = os.pathsep.join([compiler, stdlib, reflect, coroutines, annotations])
    runtime_cp = os.pathsep.join([stdlib, junit, hamcrest])
    java_home = Path(os.environ.get("JAVA_HOME", "/usr/lib/jvm/java-17-openjdk-arm64"))
    java = str(java_home / "bin/java") if (java_home / "bin/java").is_file() else "java"

    with tempfile.TemporaryDirectory(prefix="footprint-jvm-", dir=args.work_dir) as output:
        syntax = Path(output) / "CheckFootprintSyntax.java"
        syntax.write_text('''
import java.nio.file.*;
import org.jetbrains.kotlin.cli.jvm.compiler.*;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
class CheckFootprintSyntax {
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
            System.out.println("Kotlin parser: " + paths.length + " files, 0 errors (not type checking)");
        } finally { Disposer.dispose(disposable); }
    }
}
''', encoding="utf-8")
        android_sources = [
            "AppMonitor.kt", "ui/util/RootUtil.kt", "ui/viewmodel/HomeViewmodel.kt",
            "ui/mainscreens/HomeScreen.kt", "service/IslandService.kt",
            "core/platform/SensorMonitorUtil.kt",
        ]
        subprocess.run([java, "-cp", compiler_cp, str(syntax),
                        *map(str, [SOURCE / name for name in android_sources])], cwd=ROOT, check=True)
        sources = [SOURCE / "core/daemon/CompanionPollPolicy.kt",
                   TEST / "CompanionPollPolicyTest.kt", TEST / "FootprintContractTest.kt"]
        subprocess.run([
            java, "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
            "-no-stdlib", "-no-reflect", "-jvm-target", "17", "-classpath", runtime_cp,
            "-d", output, *map(str, sources),
        ], cwd=ROOT, check=True)
        junit_command = [java, "-cp", output + os.pathsep + runtime_cp, "org.junit.runner.JUnitCore"]
        subprocess.run([
            *junit_command,
            "nd.max.core.daemon.CompanionPollPolicyTest", "nd.max.core.daemon.FootprintContractTest",
        ], cwd=ROOT, check=True)
        if args.self_test:
            broken = Path(output) / "Broken.kt"
            broken.write_text("fun broken() {\n", encoding="utf-8")
            result = subprocess.run([java, "-cp", compiler_cp, str(syntax), str(broken)],
                                    cwd=ROOT, capture_output=True, text=True)
            if result.returncode == 0 or "Kotlin syntax errors" not in result.stderr:
                raise AssertionError("Kotlin parser failed to reject a known unclosed brace")
            print("Parser self-test: malformed Kotlin rejected", flush=True)

            # Only mutate a temporary source mirror. Run the real compiled JUnit guards;
            # no checkout, mocks, or edits to the working tree are needed for the negative test.
            mirror = Path(output) / "guard-mutations"
            for name in android_sources[:5]:
                text = (SOURCE / name).read_text(encoding="utf-8")
                if name == "AppMonitor.kt":
                    text = text.replace("                    awaitNextMonitorCycle()", "", 1)
                    text = text.replace("        if (screenOn) backgroundAppsWrittenWhileScreenOff = false", "", 1)
                target = mirror / "manager/app/src/main/java/nd/max" / name
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(text, encoding="utf-8")
            result = subprocess.run([*junit_command, "nd.max.core.daemon.FootprintContractTest"],
                                    cwd=mirror, capture_output=True, text=True)
            if result.returncode != 1 or not re.search(r"Tests run: 9,\s+Failures: 2", result.stdout):
                raise AssertionError("Expected exactly two wiring failures:\n" + result.stdout + result.stderr)
            for guard in ("monitor errors still wait", "screen wake invalidates"):
                if guard not in result.stdout:
                    raise AssertionError("The intended regression was not rejected: " + guard)
            print("Wiring self-test: both injected regressions rejected; 7 other guards pass", flush=True)


if __name__ == "__main__":
    main()
