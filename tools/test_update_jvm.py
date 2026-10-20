#!/usr/bin/env python3
"""Run the update-channel Kotlin/JUnit tests without Gradle, Android, or mocks.

Uses existing Gradle-cached compiler/runtime jars; never downloads dependencies.
This is not an Android compilation: the three pure files are compiled and run,
and the Android-facing files are only checked with Kotlin's real parser.

Why this split exists at all (the update channel is the one that must not lie):

  * "هل يوجد إصدار أحدث؟" and "هل هذا الإصدار محجوب؟" are decisions, not UI. A wrong
    answer either nags a user who is up to date, or worse — lets a block be skipped.
    Both live in `UpdatePolicy`/`UpdateManifest`, which are pure, so they are measured
    here on a plain JVM.
  * The transport is where a truncated download once passed as a valid file. It is
    measured against a real local HTTP server, not a fake: `UpdateTransportTest`.
  * `UpdateStore`/`UpdateInstaller`/`UpdateManager`/`UpdateViewModel`/`UpdateSurface`
    touch Android (Context, FileProvider, Hilt, Compose). Without an SDK in this
    environment they cannot be compiled, so they are parser-checked and the gap is
    declared instead of being papered over with fake Android APIs.

A missing jar is an error, never a silent pass (same contract as test_maxai_jvm.py).
"""

import argparse
import os
from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "manager/app/src/main/java/nd/max"
TEST = ROOT / "manager/app/src/test/java/nd/max"

# نسخ مثبَّتة كي يُعاد القياس نفسه، لا نسخة «ما وُجد في الذاكرة اليوم».
KOTLIN = "2.3.10"
REFLECT_VERSION = "1.6.10"
COROUTINES = "1.10.2"
SERIALIZATION = "1.11.0"


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
    # المانيفست يُقرأ بـkotlinx.serialization، فلا بدّ من الإضافة ومكوّنها في المُصرّف:
    # بدونها يفشل التصريف برسالة غير واضحة بدل أن يفشل بصدق.
    serialization_json = jar(
        "org.jetbrains.kotlinx", "kotlinx-serialization-json-jvm", SERIALIZATION
    )
    serialization_core = jar(
        "org.jetbrains.kotlinx", "kotlinx-serialization-core-jvm", SERIALIZATION
    )
    serialization_plugin = jar(
        "org.jetbrains.kotlin", "kotlin-serialization-compiler-plugin-embeddable", KOTLIN
    )

    compiler_cp = os.pathsep.join([compiler, stdlib, coroutines, annotations, reflect])
    runtime_cp = os.pathsep.join(
        [
            stdlib,
            serialization_json,
            serialization_core,
            coroutines,
            junit,
            hamcrest,
        ]
    )
    sources = [
        SOURCE / "core/update/UpdateManifest.kt",
        SOURCE / "core/update/UpdatePolicy.kt",
        SOURCE / "core/update/UpdateClient.kt",
        TEST / "core/update/UpdateManifestTest.kt",
        TEST / "core/update/UpdatePolicyTest.kt",
        TEST / "core/update/UpdateTransportTest.kt",
        # حرّاس **الملفّ المنشور**: تقرأ `docs/update/manifest.json` بالعقد نفسه، فتكون هي
        # الفرق بين «أداة تحضير كتبت ملفًّا» و«مانيفست لا يرفضه التطبيق على أجهزة لا نراها».
        TEST / "core/update/UpdatePublishedManifestTest.kt",
    ]
    tests = [
        "nd.max.core.update.UpdateManifestTest",
        "nd.max.core.update.UpdatePolicyTest",
        "nd.max.core.update.UpdateTransportTest",
        "nd.max.core.update.UpdatePublishedManifestTest",
    ]
    # الملفات التي تلمس أندرويد: تُعرَب بمُحلّل Kotlin الحقيقي (لا فحص أنواع ولا Android SDK)،
    # لأن الحكم على «هل المانيفست مقبول؟» يجري في الملفات الخالصة أعلاه، وما هنا أسلاك.
    android_sources = [
        "core/update/UpdateStore.kt",
        "core/update/UpdateInstaller.kt",
        "core/update/UpdateManager.kt",
        "ui/viewmodel/UpdateViewModel.kt",
        "ui/component/UpdateSurface.kt",
        # نقطتا التركيب: لو كُسر إعراب فيهما لما ظهرت الميزة إطلاقًا.
        "MainActivity.kt",
        "ui/mainscreens/SettingsScreen.kt",
    ]

    with tempfile.TemporaryDirectory(prefix="update-jvm-", dir=args.work_dir) as output:
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
        subprocess.run(
            ["java", "-cp", compiler_cp, str(syntax), *map(str, [SOURCE / n for n in android_sources])],
            cwd=ROOT,
            check=True,
        )
        subprocess.run(
            [
                "java", "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                "-no-stdlib", "-no-reflect", "-jvm-target", "17",
                f"-Xplugin={serialization_plugin}",
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
