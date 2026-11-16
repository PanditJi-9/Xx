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
WORKFLOW_REL = os.path.join(".github", "workflows", "build.yml")
CARGO_REL = os.path.join("manager", "src", "main", "rust", "Cargo.toml")

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

# ── بروفايل Rust للإصدار (`manager/src/main/rust/Cargo.toml`) ──
#
# المكتبة الأصلية تُشحن داخل الـAPK، وهي أوّل ما يُقرأ عند فكّه: الرموز تقول أسماء
# الدوال، وجداول التنقيح تقول أرقام الأسطر. فخصائص الإصدار عقدٌ أمنيّ لا تفضيل بناء.
CARGO_MUST = {
    "strip = true": "الرموز تبقى في المكتبة المشحونة — من يفتح الحزمة يقرأ أسماء الدوال",
    'panic = "abort"': "جدول الاسترحاء يُحمل بلا مستخدم ويوسّع سطح التحليل",
    "debug = false": "جداول التنقيح (أرقام الأسطر والملفّات) تعود إلى المكتبة المشحونة",
}
CARGO_FORBIDDEN = {
    "strip = false": "تصليب الرموز مطفأ صراحةً",
    "debug = true": "معلومات تنقيح كاملة في مكتبة تُشحن",
    'panic = "unwind"': "استرحاء عبر حدود JNI: يوسّع السطح ولا يعالجه أحد",
}

# ما يُبقي عقد JNI وحده — الصيغة الوحيدة المسموح بها مع `nd.max…**`.


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
    # **إعادة الخصائص التشخيصية إلى المخرَج (2026-11-16):** `-keepattributes SourceFile`
    # أو `LineNumberTable` تُعيد ما حذفه `-renamesourcefileattribute`، فيصير كل أثر عطل
    # في المخرَج خريطةَ أسطرٍ إلى الشيفرة. ولا سطر في الشجرة اليوم يفعلها — والقاعدة
    # تمنع أن تُضاف في جولة «إصلاح تتبّع» بلا أن يسقط شيء.
    for match in re.finditer(r"(?mi)^\s*-keepattributes[^\n]*", text):
        line = match.group(0)
        if "SourceFile" in line or "LineNumberTable" in line:
            findings.append(
                {
                    "rule": "proguard_attributes_readded",
                    "where": path + ":" + str(text.count("\n", 0, match.start()) + 1),
                    "detail": "خصائص تشخيصية أُعيد حفظها بعد تعتيم الاسم: " + line.strip()[:60],
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


def toml_section(text: str, header: str) -> str:
    """يُعيد نصّ قسم TOML بالعنوان الحرفيّ (من سطره إلى ما قبل القسم التالي).

    ولا يُعاد استعمال `extract_block`: ذاك يقيس أقواس Kotlin، و`Cargo.toml` بلا أقواس —
    فاستعماله كان يعيد «القسم مفقود» على ملفّ سليم (قِيس في القياس الذاتي).
    """
    lines = text.splitlines()
    start = -1
    for index, line in enumerate(lines):
        if line.strip() == header:
            start = index
            break
    if start < 0:
        return ""
    collected: list[str] = []
    for line in lines[start + 1 :]:
        if line.lstrip().startswith("["):
            break
        collected.append(line)
    return "\n".join(collected)


def check_cargo(text: str) -> list[dict]:
    """بروفايل Rust للإصدار: الرموز والاسترحاء والتنقيح تُقاس، لا تُفترض.

    السبب المقيس (جولة ٢٠٢٦-١١-١٦، طلب المالك حمايةً من الهندسة العكسية): ملفّ
    `Cargo.toml` كان يشرح `strip` و`panic` في تعليق طويل — وشرحٌ لا يقيسه أحد يسقط
    بأول تعديل. و`debug = false` **لم تكن مكتوبة أصلًا**، فكانت رهنًا لافتراض المُصرّف.
    ومن يفكّ الـAPK يقرأ المكتبة المشحونة أوّلًا: `strip` و`debug` هما ما يقرّر هل يرى
    رموزًا وتنقيحًا أم لا.
    """
    path = rel(os.path.join(ROOT, CARGO_REL))
    findings: list[dict] = []
    block = toml_section(text, "[profile.release]")
    if not block:
        findings.append(
            {
                "rule": "cargo_profile_missing",
                "where": path + ":0",
                "detail": "لا قسم `[profile.release]` — كل ما بُني صار بخصائص التطوير",
            }
        )
        return findings
    for needle, detail in CARGO_MUST.items():
        if needle not in block:
            findings.append(
                {
                    "rule": "cargo_missing:" + needle.split("=")[0].strip(),
                    "where": path + ":0",
                    "detail": detail,
                }
            )
    for needle, detail in CARGO_FORBIDDEN.items():
        if needle in block:
            findings.append(
                {
                    "rule": "cargo_forbidden:" + needle.split("=")[0].strip(),
                    "where": path + ":" + str(line_of(text, needle)),
                    "detail": detail,
                }
            )
    return findings


def check_mapping_upload(text: str) -> list[dict]:
    """`mapping.txt` تصعد كأثر (artifact) = خريطة الأسماء تُنشر مع المخرَج.

    وهذا أخطر من تسريب الشيفرة وحدها: `rea` نفسه يأخذ `mapping_path` صريحًا كوسيطة
    ويعيد تمثيل الأسماء (`mapped`/`ambiguous`/`unmapped`) فوق الـAPK المقلوب. فحين
    يوجد التمثيل، يسقط أثر R8 كله بملفّ واحد. والقاعدة تمنع `path:` يحمل `mapping`
    في خطوة رفع، وتمنع `-printmapping` في قواعد التعتيم.
    """
    findings: list[dict] = []
    path = rel(os.path.join(ROOT, WORKFLOW_REL))
    for match in re.finditer(r"(?mi)^\s*(?:path|name):\s*.*mapping[^\n]*$", text):
        findings.append(
            {
                "rule": "mapping_uploaded",
                "where": path + ":" + str(text.count("\n", 0, match.start()) + 1),
                "detail": "خطوة رفع تحمل `mapping` — خريطة الأسماء تُنشر فتسقط حماية التعتيم",
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


def check_shield_ci(text: str) -> list[dict]:
    """خطوة تصليب DEX في CI: لا تُحذف بصمت، ولا تُحذف أبوابها.

    السبب: خطوة واحدة في ملفّ سير عمل هي التي تفصل «تصليب مطلوب» عن «تصليب موعود».
    وحذفها لا يُسقط شيئًا آخر في الشجرة — لا تصريف ولا اختبار — فتُمرّ بصمت. فهذه
    القاعدة تربط خمسة أشياء معًا: تشغيل `bla-agents`، وتحقّق `dex_integrity`، وإعادة
    الحزم بـ`apk_repack` (‏`resources.arsc` بلا ضغط)، وإعادة التوقيع بـ`apksigner`،
    وفحص آثار الهندسة العكسية في الحزمة النهائية بـ`apk_leak_scan`.
    """
    findings: list[dict] = []
    path = rel(os.path.join(ROOT, WORKFLOW_REL))
    if not text:
        return [{"rule": "missing_file", "where": WORKFLOW_REL, "detail": "ملفّ سير العمل غير موجود"}]

    required = {
        "bla-agents": "خطوة تصليب DEX محذوفة: لا `bla-agents` في سير العمل",
        "dex_integrity.py --assert": "لا تحقّق بنيوي بعد التصليب (`dex_integrity.py --assert`)",
        "apk_repack.py": "لا إعادة حزم عبر `apk_repack.py` (فقد يُضغط `resources.arsc` فتسقط التثبيت)",
        "apksigner sign": "لا إعادة توقيع بعد التصليب (توقيع Gradle صار باطلًا بتعديل DEX)",
        "apk_leak_scan.py --assert": (
            "لا فحص لآثار الهندسة العكسية في الحزمة النهائية (`apk_leak_scan.py --assert`) — "
            "فاسم ملفّ مصدر أو مسار بناء أو خريطة R8 قد يشحن بلا أن يسقط شيء"
        ),
    }
    for needle, detail in required.items():
        if needle not in text:
            findings.append({"rule": "shield_step:" + needle.split()[0], "where": path + ":0", "detail": detail})
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
    workflow_text = read(os.path.join(ROOT, WORKFLOW_REL))
    findings += check_shield_ci(workflow_text)
    findings += check_mapping_upload(workflow_text)
    cargo_text = read(os.path.join(ROOT, CARGO_REL))
    if not cargo_text:
        findings.append({"rule": "missing_file", "where": CARGO_REL, "detail": "ملفّ Cargo غير موجود"})
    else:
        findings += check_cargo(cargo_text)
    return {
        "obstacles": len(findings),
        "findings": findings,
        "scope": [GRADLE_REL, PROGUARD_REL, MANIFEST_REL, WORKFLOW_REL, CARGO_REL],
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

GOOD_CARGO = """
[profile.release]
lto = "thin"
strip = true
panic = "abort"
debug = false
"""

BAD_CARGO = """
[profile.release]
strip = false
debug = true
panic = "unwind"
"""

GOOD_WORKFLOW = "steps:\n  - uses: actions/upload-artifact@v4\n    with:\n      path: build/reports/\n"
BAD_WORKFLOW = "steps:\n  - uses: actions/upload-artifact@v4\n    with:\n      path: build/outputs/mapping.txt\n"


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

    # بروفايل Rust: السليم يمرّ، وكل مفتاح معطوب يُرصد وحده (لا مجرد «الملفّ سيّئ»).
    expect("good cargo", len(check_cargo(GOOD_CARGO)), False)
    expect("bad cargo", len(check_cargo(BAD_CARGO)), True)
    if len(check_cargo(BAD_CARGO)) != 6:
        # ستة لا ثلاثة: كل مفتاح معطوب يسقط شرطَ وجوده **و**يُخالف صيغته الممنوعة،
        # والبلاغان مقصودان — الأول يقول «الشرط غائب»، والثاني يقول «الصيغة موجودة وممنوعة».
        failures.append(f"bad cargo: expected 6 obstacles, got {len(check_cargo(BAD_CARGO))}")
    no_profile = "".join(line for line in GOOD_CARGO.splitlines(True) if "profile.release" not in line)
    expect("cargo without profile", len(check_cargo(no_profile)), True)

    # خصائص تشخيصية أُعيدت بعد التعتيم: تُرصد ولو كان بقية الملفّ سليمًا.
    readded = GOOD_PROGUARD + "-keepattributes SourceFile,LineNumberTable\n"
    expect("attributes readded", len(check_proguard(readded)) - len(check_proguard(GOOD_PROGUARD)), True)

    # رفع `mapping.txt` كأثر: يُرصد، والقاعدة لا تتوهّم مسارًا عاديًّا.
    expect("good workflow paths", len(check_mapping_upload(GOOD_WORKFLOW)), False)
    expect("mapping artifact", len(check_mapping_upload(BAD_WORKFLOW)), True)

    if failures:
        print("✗ البوابة نفسها معطوبة:")
        for failure in failures:
            print("  - " + failure)
        return 1
    print("✓ قياس الأداة: ١٤ حالات معروفة، كلها بالحكم المتوقّع")
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
