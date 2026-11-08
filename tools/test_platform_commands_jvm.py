#!/usr/bin/env python3
"""قياس حدّ أوامر المنصّة (`PlatformCommandRules`) على JVM عادية — بلا Gradle وبلا جهاز.

## ما تقيسه بالضبط

* **أوامر ADB المشروعة** التي تعتمد عليها الشاشات فعلًا (`wm` · `pm` · `cmd appops` ·
  `cmd deviceidle` · `dumpsys` · `settings` · `service call` · `am`) تمرّ.
* **مسارات النواة والملفّات المحميّة** (`/sys` · `/proc` · `/data/adb` · `/dev/block`) تُرفض
  قبل التنفيذ، حتى لو بدأ الأمر بفعل مسموح مثل `echo`.
* **أفعال الترقية** (`su` · `sudo` · `mount` · `insmod` · `rmmod` · `modprobe` · `chmod` ·
  `chown`) تُرفض: Shizuku يعمل بمستوى ADB ولا يملكها.
* **إعادة التوجيه**: كتم الخطأ (`2>/dev/null` · `2>&1` · `>/dev/null`) يمرّ، وأي `>` آخر
  يُرفض لأنه كتابة ملفّ خارج عقد الطبقة.

## حدودها المُعلنة

ليست بديلًا عن بناء CI: لا تُصرّف الشاشات ولا تُشغّل أندرويد. وما يبقى «يحتاج جهازًا» هو
سلوك Shizuku الحقيقي (ربط الخدمة وتنفيذ الأمر بصلاحية ADB). وهذه القاعدة طبقة صدق لا جدار
أمان؛ الجدار الحقيقي هو صلاحية العملية.

```sh
python3 tools/test_platform_commands_jvm.py
python3 tools/test_platform_commands_jvm.py --json /tmp/platform-commands.json
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

PURE = ["privilege/PlatformCommandRules.kt"]
TESTS_FILES = ["privilege/PlatformCommandRulesTest.kt"]
TEST_CLASSES = ["nd.max.core.privilege.PlatformCommandRulesTest"]


def cached(args, cache: Path, group: str, artifact: str, version: str) -> str:
    matches = sorted((cache / group / artifact / version).glob(f"*/{artifact}-{version}.jar"))
    if not matches:
        # جارٍ ناقص = خطأ، لا نجاح صامت (نفس عقد بقية أجهزة القياس).
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

    compiler_cp = os.pathsep.join([compiler, stdlib, coroutines, annotations, reflect])
    runtime_cp = os.pathsep.join([stdlib, junit, hamcrest])

    report: dict[str, object] = {"compiled": 0, "tests": []}
    with tempfile.TemporaryDirectory(prefix="platform-commands-jvm-", dir=args.work_dir) as output:
        sources = [CORE / name for name in PURE] + [TESTS / name for name in TESTS_FILES]
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
