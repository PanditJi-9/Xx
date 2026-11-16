#!/usr/bin/env python3
"""بوابة تصليب الإصدار — تمنع أن يعود المخرَج المُوزَّع مقروءًا بعد أن صُعِّب.

لماذا وُجدت: طلب المالك (٢٠٢٦-١١-١٦) حماية التطبيق من السرقة والهندسة العكسية، ومعه
مرجعان: أداة هجوم بالوكيل (`morluto/rea`) وأداة تصليب بالوكيل (`qybaihe/bla`). والحاصل
أنّ الشجرة **صُعِّبت فعلًا** (`core/security/**` ١٦٨٥ سطرًا: تثبيت الموقّع من مرسأتين،
تحقّق توقيع الحزمة، أصول مختومة، مسبار أصلي، سجلّ أدلّة) — والنقص لم يكن طبقةً جديدة بل
**حارسًا يمنع تراجع التصليب**: قاعدة `-keep` واحدة تُكتب على عجل تُلغي تعتيم حزمة كاملة،
وضبط `isMinifyEnabled = false` في تصحيحٍ متأخّر يفعل أكثر من ذلك.

فالبوابة **لا تُصعّب** — تقيس ما يجب أن يبقى صعبًا، وتسمّي الملف والسطر حين يتراجع.

وهي تقيس **الحزمة لا تشغيلها**: من أراد أن يعرف هل الأصناف أُعيدت تسميتها فعلًا فليقرأ
`mapping.txt` من مخرَج البناء، وهذه البوابة تقول فقط إنّ ما يمنع ذلك غير مكتوب.

أمثلة:
    python3 tools/hardening_guard.py --assert   # يفشل على أي تراجع مقيس
    python3 tools/hardening_guard.py --json     # كل ما قيس
    python3 tools/hardening_guard.py --self-test  # يقيس القواعد نفسها على نصوص معروفة
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

GRADLE_REL = os.path.join("manager", "app", "build.gradle.kts")
PROGUARD_REL = os.path.join("manager", "app", "proguard-rules.pro")
MANIFEST_REL = os.path.join("manager", "app", "src", "main", "AndroidManifest.xml")

# ── القواعد ────────────────────────────────────────────────────────────────
#
# كل قاعدة **إشارة إلى نيّة مكتوبة في الشجرة**، لا رأيًا عامًّا: تعليق
# `-repackageclasses 'o'` في `proguard-rules.pro` يقول «تُنقل كل الأصناف إلى حزمة واحدة»؛
# و`-allowaccessmodification` هو ما يجعل ذلك ممكنًا بلا كسر. وقد كان **غائبًا** وقيس
# غيابه قبل كتابة هذه البوابة.

# في `build.gradle.kts`، داخل `buildTypes { release { … } }` وحده.
GRADLE_RELEASE_MUST = {
    "release_minify": ("isMinifyEnabled = true", "التعتيم والحذف معطّلان في نسخة الإصدار"),
    "release_shrink": (
        "isShrinkResources = true",
        "تقليص الموارد معطّل — فيسافر كل مورد لا يقرؤه أحد",
    ),
    "release_vcs": ("vcsInfo.include = false", "بيانات VCS تُكتب في الحزمة بلا داعٍ"),
}
GRADLE_RELEASE_NEEDS = (
    "proguard-android-optimize.txt",
    "proguard-rules.pro",
)

# في `proguard-rules.pro`.
PROGUARD_MUST = {
    "-repackageclasses": "لا تُنقل الأصناف إلى حزمة واحدة، فتبقى أسماء الوحدات خريطةَ طريق",
    "-allowaccessmodification": (
        "غير موجودة: بلاها لا يُدمج ما هو `private`، فيبقى الحدّ بين الوحدات قائمًا في المخرَج"
    ),
    "-optimizationpasses": "عدد مرّات التحسين غير مضبوط، فيبقى الافتراضي الأقلّ",
    "-renamesourcefileattribute": "اسم ملفّ المصدر يبقى في جداول الاستثناءات",
}

# في `proguard-rules.pro`: ما يُبطل التصليب من أصله.
PROGUARD_FORBIDDEN = {
    "-dontobfuscate": "Tعتيم مطفأ صراحةً",
    "-dontoptimize": "تحسين مطفأ صراحةً",
    "-keepnames class nd.max.": "إبقاء الأسماء في شجرتنا يُبطل التعتيم كله",
}

# قاعدة إبقاء شاملة على شجرة المنتج نفسها: `-keep class nd.max.** { *; }` أو ما يشبهها
# (`nd.max.core.**` · `nd.max.ui.**`) — أي عمق، لا الجذر وحده.
#
# **والعضوان لا يُساوَيان (2026-11-16):** `{ *; }` يُجمّد كل عضو فلا يُعاد تسميته ولا يُحذف،
# وهو المرفوض. وأمّا `{ native <methods>; }` فهو **الحدّ الأدنى الصحيح لعقد JNI**: الرمز
# المصدَّر في Rust اسمه `Java_<حزمة>_<صنف>_<دالة>`، فاسم الصنف واسم الدالة الأصلية يلزمهما
# البقاء، وما عداهما يُعاد تسميته بلا كسر ربط. فالبوابة تفرّق بين الصيغتين بدل أن ترفض
# التصليب المشروع مع الشامل.
BLANKET_KEEP = re.compile(
    r"^\s*-keep(?:classeswithmembers|classes|classmembers|class)?\s+"
    r"(?:class\s+)?nd\.max(?:\.\w+)*\.\*\*\s*(?:\{\s*(?P<body>[^}]*)\})?", re.MULTILINE
)

# ما يُبقي عقد JNI وحده — الصيغة الوحيدة المسموح بها مع `nd.max…**`.
JNI_MINIMAL_BODY = "native <methods>;"

# سرّ مكتوب في ملفّ البناء: `storePassword = "…"` / `keyPassword = "…"` بقيمة حرفية.
INLINE_SECRET = re.compile(
    r"(?:storePassword|keyPassword|KS_PWD)\s*=\s*\"(?![$])([^\"]+)\""
)


def rel(path: str) -> str:
    return os.path.relpath(path, ROOT).replace(os.sep, "/")


def line_of(text: str, needle: str) -> int:
    """رقم السطر (1-based) لأول ظهور، أو 0 حين يغيب."""
    index = text.find(needle)
    if index < 0:
        return 0
    return text.count("\n", 0, index) + 1


def extract_block(text: str, header: str, must_contain: str = "") -> str:
    """يُعيد نصّ أول كتلة تبدأ بـ[header] وتنتهي بقوسها المطابق (بما فيها الأقواس).

    و[must_contain] ليس ترفًا: `buildTypes { release { … } }` مكتوبة **في تعليق** أعلى
    الملفّ، فالبحث عن أول ظهور يلتقط التعليق لا الشيفرة — وقد قِيس هذا العطب في أول نسخة
    من البوابة: ستّة عوائق كاذبة على ملفّ سليم. فتُجرَّب كل المواضع ويُختار أول كتلة تحمل
    العلامة المطلوبة.
    """
    cursor = 0
    while True:
        start = text.find(header, cursor)
        if start < 0:
            return ""
        open_at = text.find("{", start)
        if open_at < 0:
            return ""
        depth = 0
        for index in range(open_at, len(text)):
            char = text[index]
            if char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    block = text[open_at : index + 1]
                    if not must_contain or must_contain in block:
                        return block
                    break
        cursor = start + len(header)


def check_gradle(text: str) -> list[dict]:
    findings: list[dict] = []
    # `isMinifyEnabled` علامةُ الكتلة الحقيقية: كتلة `buildTypes` في تعليق الرأس لا تحملها.
    build_types = extract_block(text, "buildTypes {", must_contain="isMinifyEnabled")
    release = extract_block(build_types or text, "release {", must_contain="isMinifyEnabled")
    if not release:
        findings.append(
            {
                "rule": "release_block_missing",
                "where": rel(os.path.join(ROOT, GRADLE_REL)) + ":0",
                "detail": "لا كتلة `release { }` تحمل `isMinifyEnabled` — فكل قواعد التصليب بلا موضع",
            }
        )
        return findings
    release_line = text.count("\n", 0, text.find(release)) + 1
    for rule, (needle, detail) in GRADLE_RELEASE_MUST.items():
        if needle not in release:
            findings.append(
                {
                    "rule": rule,
                    "where": rel(os.path.join(ROOT, GRADLE_REL)) + ":" + str(release_line),
                    "detail": detail,
                }
            )
    for needle in GRADLE_RELEASE_NEEDS:
        if needle not in release:
            findings.append(
                {
                    "rule": "release_proguard_files",
                    "where": rel(os.path.join(ROOT, GRADLE_REL)) + ":" + str(release_line),
                    "detail": "`proguardFiles` لا يحمل `" + needle + "`",
                }
            )
    for match in INLINE_SECRET.finditer(text):
        findings.append(
            {
                "rule": "inline_secret",
                "where": rel(os.path.join(ROOT, GRADLE_REL)) + ":" + str(text.count("\n", 0, match.start()) + 1),
                "detail": "سرّ مكتوب حرفيًّا في ملفّ البناء (`" + match.group(0)[:24] + "…`)",
            }
        )
    return findings


def check_proguard(text: str) -> list[dict]:
    path = rel(os.path.join(ROOT, PROGUARD_REL))
    findings: list[dict] = []
    for needle, detail in PROGUARD_MUST.items():
        if needle not in text:
            findings.append(
                {
                    "rule": "proguard_missing:" + needle.lstrip("-"),
                    "where": path + ":0",
                    "detail": detail,
                }
            )
    for needle, detail in PROGUARD_FORBIDDEN.items():
        if needle in text:
            findings.append(
                {
                    "rule": "proguard_forbidden:" + needle.lstrip("-"),
                    "where": path + ":" + str(line_of(text, needle)),
                    "detail": detail,
                }
            )
    for match in BLANKET_KEEP.finditer(text):
        body = " ".join((match.group("body") or "*;").split())
        if body == JNI_MINIMAL_BODY:
            continue
        findings.append(
            {
                "rule": "proguard_blanket_keep",
                "where": path + ":" + str(text.count("\n", 0, match.start()) + 1),
                "detail": "قاعدة إبقاء شاملة على `nd.max` تُبقي الأسماء كلها: " + match.group(0).strip()[:60],
            }
        )
    return findings


def check_manifest(text: str) -> list[dict]:
    path = rel(os.path.join(ROOT, MANIFEST_REL))
    findings: list[dict] = []
    if re.search(r'android:debuggable\s*=\s*"true"', text):
        findings.append(
            {
                "rule": "manifest_debuggable",
                "where": path + ":" + str(text.count("\n", 0, text.find("android:debuggable")) + 1),
                "detail": "حزمة قابلة للتنقيح: من يفتحها يرى المنطق حيًّا",
            }
        )
    if not re.search(r'android:allowBackup\s*=\s*"false"', text):
        findings.append(
            {
                "rule": "manifest_allow_backup",
                "where": path + ":0",
                "detail": "`allowBackup` غير مضبوط على `false` — نسخ احتياطي يخرج ببيانات التطبيق",
            }
        )
    return findings


def tracked_mapping() -> list[dict]:
    """`mapping.txt` مُتتبَّع في git = خريطة الأسماء الحقيقية منشورة مع المخرَج."""
    findings: list[dict] = []
    try:
        proc = subprocess.run(
            ["git", "ls-files"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=120,
        )
    except Exception:
        return findings
    if proc.returncode != 0:
        return findings
    for line in proc.stdout.splitlines():
        name = os.path.basename(line)
        if name == "mapping.txt" or (name.startswith("mapping") and name.endswith(".txt")):
            findings.append(
                {
                    "rule": "mapping_tracked",
                    "where": line,
                    "detail": "خريطة التعتيم داخل المستودع: تكفي وحدها لفكّ الأسماء",
                }
            )
    return findings


def read(path: str) -> str:
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as handle:
            return handle.read()
    except OSError:
        return ""


def scan() -> dict:
    gradle_text = read(os.path.join(ROOT, GRADLE_REL))
    proguard_text = read(os.path.join(ROOT, PROGUARD_REL))
    manifest_text = read(os.path.join(ROOT, MANIFEST_REL))
    findings: list[dict] = []
    if not gradle_text:
        findings.append({"rule": "missing_file", "where": GRADLE_REL, "detail": "ملفّ البناء غير موجود"})
    else:
        findings += check_gradle(gradle_text)
    if not proguard_text:
        findings.append({"rule": "missing_file", "where": PROGUARD_REL, "detail": "قواعد ProGuard غير موجودة"})
    else:
        findings += check_proguard(proguard_text)
    if not manifest_text:
        findings.append({"rule": "missing_file", "where": MANIFEST_REL, "detail": "البيان غير موجود"})
    else:
        findings += check_manifest(manifest_text)
    findings += tracked_mapping()
    return {
        "obstacles": len(findings),
        "findings": findings,
        "scope": [GRADLE_REL, PROGUARD_REL, MANIFEST_REL],
    }


# ── قياس الأداة نفسها ──────────────────────────────────────────────────────

GOOD_GRADLE = """
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
}
"""

BAD_GRADLE = """
android {
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            vcsInfo.include = true
            storePassword = "hunter2"
        }
    }
}
"""

GOOD_PROGUARD = """
-repackageclasses 'o'
-allowaccessmodification
-optimizationpasses 5
-renamesourcefileattribute SourceFile
-keep class nd.max.AppMonitor { public static void main(java.lang.String[]); }
"""

BAD_PROGUARD = """
-dontobfuscate
-keep class nd.max.** { *; }
-keepnames class nd.max.core.security.SecurityBridge { *; }
"""

GOOD_MANIFEST = '<manifest><application android:allowBackup="false" /></manifest>'
BAD_MANIFEST = '<manifest><application android:allowBackup="true" android:debuggable="true" /></manifest>'


def self_test() -> int:
    failures: list[str] = []

    def expect(name: str, actual: int, want_bad: bool) -> None:
        ok = (actual > 0) if want_bad else (actual == 0)
        if not ok:
            failures.append(f"{name}: {actual} obstacles (want {'>0' if want_bad else '0'})")

    expect("good gradle", len(check_gradle(GOOD_GRADLE)), False)
    expect("bad gradle", len(check_gradle(BAD_GRADLE)), True)
    expect("good proguard", len(check_proguard(GOOD_PROGUARD)), False)
    expect("bad proguard", len(check_proguard(BAD_PROGUARD)), True)
    expect("good manifest", len(check_manifest(GOOD_MANIFEST)), False)
    expect("bad manifest", len(check_manifest(BAD_MANIFEST)), True)

    # القاعدة الشاملة وحدها يجب أن تُرصد حتى لو كان كل شيء آخر سليمًا.
    only_blanket = GOOD_PROGUARD + "-keep class nd.max.core.** { *; }\n"
    expect("blanket keep", len(check_proguard(only_blanket)) - len(check_proguard(GOOD_PROGUARD)), True)

    # ولا تُرصد قاعدة الإبقاء المُبرَّرة بصنفٍ واحد (سببها مكتوب في الشجرة).
    if len(check_proguard(GOOD_PROGUARD)) != 0:
        failures.append("justified keep was flagged")

    # **وحدّ JNI الأدنى مسموح:** `nd.max…**` بعضو `{ native <methods>; }` وحده يُبقي ما يلزم
    # لعقد JNI ولا يُجمّد شيئًا آخر — فأي رفض له كان سيُجبر الشجرة على إبقاء غير لازم.
    jni_minimal = GOOD_PROGUARD + "-keep class nd.max.core.jni.** { native <methods>; }\n"
    expect("jni minimal keep", len(check_proguard(jni_minimal)) - len(check_proguard(GOOD_PROGUARD)), False)
    # و`*;` في الحزمة نفسها يبقى مرفوضًا — الفرق بين الصيغتين هو ما تقيسه البوابة.
    jni_blanket = GOOD_PROGUARD + "-keep class nd.max.core.jni.** { *; }\n"
    expect("jni blanket keep", len(check_proguard(jni_blanket)) - len(check_proguard(GOOD_PROGUARD)), True)

    if failures:
        print("✗ البوابة نفسها معطوبة:")
        for failure in failures:
            print("  - " + failure)
        return 1
    print("✓ قياس الأداة: ٩ حالات معروفة، كلها بالحكم المتوقّع")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="بوابة تصليب الإصدار")
    # `assert` كلمة محجوزة في Python، فلا تُقرأ `args.assert` — الوجهة باسم آخر.
    parser.add_argument("--assert", dest="assert_mode", action="store_true", help="يفشل عند أي تراجع مقيس")
    parser.add_argument("--json", action="store_true", help="مخرَج JSON كامل")
    parser.add_argument("--self-test", action="store_true", help="يقيس الأداة على نصوص معروفة")
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    report = scan()
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print(f"نطاق القياس: {'، '.join(report['scope'])}")
        if report["obstacles"] == 0:
            print("✓ التصليب قائم: التعتيم والتقليص وإعادة الحزم وقواعد الإبقاء كما قُرِّرت")
        else:
            print(f"✗ عوائق: {report['obstacles']}")
            for finding in report["findings"]:
                print(f"  {finding['where']} — {finding['rule']}: {finding['detail']}")
    if args.assert_mode and report["obstacles"]:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
