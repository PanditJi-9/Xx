#!/usr/bin/env python3
"""قياس منظومة الترخيص (`nd.max.core.license` + `core/access`) على JVM عادية — بلا Gradle وبلا جهاز.

## ما تقيسه بالضبط

* **صيغة المفتاح** بمتجهات مرجعية حُسبت خارج Kotlin: التجزيء، والبصمة SHA-256، والصيغة المعروضة،
  وتطبيع ما يلصقه الزبون (فوارز · حروف صغيرة · O/I/L/U · بادئة أو بلا).
* **قبول الشهادة**: توقيعٌ حقيقيّ يمرّ، وبايتٌ واحد مقلوب يرسب، ومفتاح مهاجم يرسب، وشهادةٌ نُسخت
  إلى جهاز آخر تُرفض **باسمها**، وصيغة أحدث، ونوع آخر، ومدى زمنيّ مكسور.
* **الحكم الزمنيّ**: سارٍ/منتهٍ، و**مانع رجوع الساعة** (الزمن الموثوق لا ينزل، ومرساة ردّ الرفض).
* **المخزن**: كتابة ذرّية، ملفّ مشفَّر لا نصّ صريح، تلف = غياب، مفتاحٌ آخر لا يفكّ.
* **هوية الجهاز**: عشوائية ١٢٨ بت، تُستقرّ، وتُستبدل إن كان المحتوى فاسدًا.
* **مصفوفة الوصول**: `on/off/subscribe` × (بلا مفتاح · مفتاح سارٍ · منتهٍ) × (نافذة مجّاني أو لا).
* **النقل**: بخادم HTTP حقيقيّ على ١٢٧.٠.٠.١ — البصمة لا المفتاح، وزمن الرفض، وتصنيف الأعطاب.

## حدودها المُعلنة

ليست بديلًا عن بناء CI: لا تُنتج APK، ولا تشغّل أندرويد، ولا تلمس Supabase الحقيقي. وما يبقى
«يحتاج جهازًا»: سلوك الشاشات، واللمس، وRTL، ومخزن مفاتيح النظام نفسه.

```sh
python3 tools/test_license_jvm.py            # التصريف + الاختبارات
python3 tools/test_license_jvm.py --json /tmp/license.json
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
CORE = ROOT / "manager/app/src/main/java/nd/max/core"
TESTS = ROOT / "manager/app/src/test/java/nd/max/core"

# نسخ مثبَّتة كي يُعاد القياس نفسه، لا نسخة «ما وُجد في الذاكرة اليوم».
KOTLIN = "2.3.10"
REFLECT_VERSION = "1.6.10"
COROUTINES = "1.10.2"
SERIALIZATION = "1.11.0"

# المنطق النقيّ — يُصرَّف ويُقاس فعلًا. و`remote/` مشتركة لأن حكم الوصول يقرأ العقد الموقّع.
PURE = [
    "security/P256Signatures.kt",
    "access/PromoWindow.kt",
    "license/LicenseKey.kt",
    "license/LicenseCertificate.kt",
    "license/LicenseState.kt",
    "license/LicenseStore.kt",
    "license/DeviceIdentity.kt",
    "license/LicenseClient.kt",
    "access/FeatureAccess.kt",
    "remote/ConfigSignature.kt",
    "remote/RemoteConfig.kt",
    "remote/RemoteFlags.kt",
]

TESTS_FILES = [
    "remote/RemoteConfigTestSupport.kt",
    "license/LicenseTestSupport.kt",
    "license/LicenseKeyTest.kt",
    "license/LicenseCertificateTest.kt",
    "license/LicenseStateTest.kt",
    "license/LicenseStoreTest.kt",
    "license/DeviceIdentityTest.kt",
    "license/LicenseClientTest.kt",
    "access/FeatureAccessTest.kt",
]

TEST_CLASSES = [
    "nd.max.core.license.LicenseKeyTest",
    "nd.max.core.license.LicenseCertificateTest",
    "nd.max.core.license.LicenseStateTest",
    "nd.max.core.license.LicenseStoreTest",
    "nd.max.core.license.DeviceIdentityTest",
    "nd.max.core.license.LicenseClientTest",
    "nd.max.core.access.FeatureAccessTest",
]


def cached(args, cache: Path, group: str, artifact: str, version: str) -> str:
    matches = sorted((cache / group / artifact / version).glob(f"*/{artifact}-{version}.jar"))
    if not matches:
        # جارٍ ناقص = خطأ، لا نجاح صامت (نفس عقد `test_remote_jvm.py`).
        args.error(f"Missing cached {group}:{artifact}:{version}; no dependency was downloaded")
    return str(matches[0])


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--work-dir", type=Path, default=Path(tempfile.gettempdir()))
    parser.add_argument("--json", type=Path, help="تقرير مقروء آليًّا")
    args = parser.parse_args()

    missing = [name for name in PURE if not (CORE / name).is_file()]
    missing += [name for name in TESTS_FILES if not (TESTS / name).is_file()]
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

    report: dict[str, object] = {"compiled": 0, "tests": []}
    with tempfile.TemporaryDirectory(prefix="license-jvm-", dir=args.work_dir) as output:
        sources = [CORE / name for name in PURE] + [TESTS / name for name in TESTS_FILES]
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


if __name__ == "__main__":
    sys.exit(main())
