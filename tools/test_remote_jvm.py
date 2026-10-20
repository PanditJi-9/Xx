#!/usr/bin/env python3
"""قياس نواة الإعداد البعيد (`nd.max.core.remote`) على JVM عادية — بلا Gradle وبلا جهاز.

## لماذا لا `:app:testReleaseUnitTest` — والسبب مُقاس لا مُفترض

`:app:testReleaseUnitTest` تحتاج خطّ تغليف أندرويد كاملًا، و`aapt2` الذي ينزّله AGP 9.2.0
لبِنيّة **x86-64** وحدها لا يعمل على مضيف aarch64 (نفس ما وُثّق في `tools/test_security_jvm.py`
وخلق هذا النمط من الأدوات). ونواة الإعداد البعيد **منطق خالص**: توقيعٌ وترتيب فحصٍ ومقارنة
إصدارات — لا شيئًا من أندرويد. فهي تُصرَّف وتُقاس هنا في ثوانٍ.

## ما تقيسه بالضبط

* **الحالات المضادّة الاثنتا عشرة** من `MAX-CONTROL-01` §٥.٤: توقيع مزوَّر · بايت مقلوب ·
  رجوع إصدار · صيغة أحدث · ذاكرة مقلوبة/مقطوعة · ساعة راجعة · مفتاح غريب · خادم غائب ·
  مشروع موقوف · مسح بيانات · ولا مسار كتابة في العقد.
* **بمفاتيح P-256 حقيقية**: كل دعوى توقيع تولّد زوجها في اللحظة وتوقّع بـ`SHA256withECDSA`
  نفسها التي تُشحن — فلا فرق بين ما يُقاس هنا وما يعمل على الجهاز.
* **وبخادم HTTP حقيقيّ على 127.0.0.1** للنقل: ‏200 · ‏304 بلا جسم · ‏401 · ‏403 · ‏404 ·
  ‏5xx · جسم أكبر من السقف · شكل غريب · خادم غائب.

## حدودها المُعلنة

ليست بديلًا عن بناء CI: لا تُنتج APK ولا تُصرّف موارد، ولا تلمس Supabase الحقيقي. وما يبقى
«يحتاج جهازًا»: سلوك الواجهة عند غياب الخادم، واللمس، وRTL.

```sh
python3 tools/test_remote_jvm.py            # التصريف + الاختبارات
python3 tools/test_remote_jvm.py --json /tmp/remote.json
```
"""

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "manager/app/src/main/java/nd/max/core/remote"
TEST = ROOT / "manager/app/src/test/java/nd/max/core/remote"

# نسخ مثبَّتة كي يُعاد القياس نفسه، لا نسخة «ما وُجد في الذاكرة اليوم».
KOTLIN = "2.3.10"
REFLECT_VERSION = "1.6.10"
COROUTINES = "1.10.2"
SERIALIZATION = "1.11.0"

# المنطق النقيّ — يُصرَّف ويُقاس فعلًا.
PURE = [
    "RemoteConfig.kt",
    "ConfigSignature.kt",
    "../security/P256Signatures.kt",
    "../access/PromoWindow.kt",
    "RemoteFlags.kt",
    "ConfigStore.kt",
    "ConfigClient.kt",
    "ConfigBootGate.kt",
]

TESTS = [
    "RemoteConfigAdmissionTest.kt",
    "RemoteConfigArchitectureTest.kt",
    "ConfigStoreTest.kt",
    "ConfigBootGateTest.kt",
    "RemoteFlagsTest.kt",
    "ConfigClientTest.kt",
    "RemoteConfigTestSupport.kt",
]

TEST_CLASSES = [
    "nd.max.core.remote.RemoteConfigAdmissionTest",
    "nd.max.core.remote.RemoteConfigArchitectureTest",
    "nd.max.core.remote.ConfigStoreTest",
    "nd.max.core.remote.ConfigBootGateTest",
    "nd.max.core.remote.RemoteFlagsTest",
    "nd.max.core.remote.ConfigClientTest",
]

# الملفّات التي تلمس أندرويد (الأسلاك): تُعرَب بمُحلّل Kotlin الحقيقي. تُملأ في P4 عند إضافة
# نقطة التركيب في الإقلاع؛ والقائمة الفارغة مقصودة الآن — لا ادّعاء فحص لملفّ غير موجود.
ANDROID_SOURCES: list[str] = []


def cached(args, cache: Path, group: str, artifact: str, version: str) -> str:
    matches = sorted((cache / group / artifact / version).glob(f"*/{artifact}-{version}.jar"))
    if not matches:
        # جارٍ ناقص = خطأ، لا نجاح صامت (نفس عقد `test_maxai_jvm.py`).
        args.error(f"Missing cached {group}:{artifact}:{version}; no dependency was downloaded")
    return str(matches[0])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--work-dir", type=Path, default=Path(tempfile.gettempdir()))
    parser.add_argument("--json", type=Path, help="تقرير مقروء آليًّا")
    args = parser.parse_args()

    missing = [name for name in PURE + TESTS if not (SOURCE / name).is_file() and not (TEST / name).is_file()]
    if missing:
        args.error(f"ملفّات معلنة وغير موجودة: {missing}")

    cache = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1"
    stdlib = cached(args, cache, "org.jetbrains.kotlin", "kotlin-stdlib", KOTLIN)
    compiler = cached(args, cache, "org.jetbrains.kotlin", "kotlin-compiler-embeddable", KOTLIN)
    reflect = cached(args, cache, "org.jetbrains.kotlin", "kotlin-reflect", REFLECT_VERSION)
    annotations = cached(args, cache, "org.jetbrains", "annotations", "13.0")
    coroutines = cached(args, cache, "org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", COROUTINES)
    junit = cached(args, cache, "junit", "junit", "4.13.2")
    hamcrest = cached(args, cache, "org.hamcrest", "hamcrest-core", "1.3")
    serialization_json = cached(args, cache, "org.jetbrains.kotlinx", "kotlinx-serialization-json-jvm", SERIALIZATION)
    serialization_core = cached(args, cache, "org.jetbrains.kotlinx", "kotlinx-serialization-core-jvm", SERIALIZATION)
    serialization_plugin = cached(args, cache, "org.jetbrains.kotlin", "kotlin-serialization-compiler-plugin-embeddable", KOTLIN)

    compiler_cp = os.pathsep.join([compiler, stdlib, coroutines, annotations, reflect])
    runtime_cp = os.pathsep.join([stdlib, serialization_json, serialization_core, coroutines, junit, hamcrest])

    report: dict[str, object] = {"compiled": 0, "tests": [], "syntax": []}
    with tempfile.TemporaryDirectory(prefix="remote-jvm-", dir=args.work_dir) as output:
        if ANDROID_SOURCES:
            report["syntax"] = check_syntax(args, compiler_cp, ANDROID_SOURCES)
        sources = [SOURCE / name for name in PURE] + [TEST / name for name in TESTS]
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
        report["compiled"] = len(sources)
        completed = subprocess.run(
            ["java", "-cp", output + os.pathsep + runtime_cp, "org.junit.runner.JUnitCore", *TEST_CLASSES],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        print(completed.stdout.strip())
        if completed.stderr.strip():
            print(completed.stderr.strip(), file=sys.stderr)
        report["tests"] = TEST_CLASSES
        if completed.returncode != 0:
            if args.json:
                args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
            return 1

    print(f"\n✓ صُرّف {report['compiled']} ملفًّا · ومرّت {len(TEST_CLASSES)} مجموعة اختبار")
    if args.json:
        args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    return 0


def check_syntax(args: argparse.Namespace, compiler_cp: str, names: list[str]) -> list[str]:
    """يُعرَب ملفّات الأسلاك بمُحلّل Kotlin الحقيقي — لا فحص أنواع ولا أندرويد (لا SDK هنا)."""
    with tempfile.TemporaryDirectory(prefix="remote-syntax-") as folder:
        program = Path(folder) / "CheckKotlinSyntax.java"
        program.write_text(
            """
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
""",
            encoding="utf-8",
        )
        subprocess.run(
            ["java", "-cp", compiler_cp, str(program), *map(str, [SOURCE / name for name in names])],
            cwd=ROOT,
            check=True,
        )
    return names


if __name__ == "__main__":
    sys.exit(main())
