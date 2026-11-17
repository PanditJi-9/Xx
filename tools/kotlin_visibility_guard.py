#!/usr/bin/env python3
"""بوّابة رؤية Kotlin: رمز `private` في ملفّ لا يُستعمل من ملفّ آخر في الحزمة نفسها.

**السبب الذي بُنيت له (مقيس، 2026-11-17):** ثلاث دورات متتالية رسبت في Actions عند
`compileReleaseKotlin`، وكل بوّاباتنا المحلية كانت خضراء. والسبب في أوّلها كان واحدًا:
ملفّ جديد (`GamePanelSkinChooser.kt`) يستعمل `TurboPalette` و`TurboPadMd`… وهي `private`
في `GamePanelTurbo.kt` (٦٠ خطأ). والبوّابات البنيوية لا ترى الرؤية (visibility) لأنها لا
توازن ولا نصّ.

**وحدّه المُعلن:** فحص نصّيّ لا مُصرّف. يكشف الأسماء التي تبدأ بحرف كبير (أنواع/كائنات/دوالّ
بأسلوب `PascalCase`) لأنها أقلّ التباسًا في التحليل النصّيّ؛ ولا يكشف دالّة `private`
باسم صغير ولا أخطاء الأنواع. وليس بديلًا عن Actions — إشارة محلية قبل الدفع فقط.

    python3 tools/kotlin_visibility_guard.py --assert     # يفشل عند أي استعمال عبر الملفّات
    python3 tools/kotlin_visibility_guard.py --self-test  # يقيس الأداة على حالات معروفة
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE_ROOT = ROOT / "manager/app/src/main/java"

PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*$", re.M)
IMPORT = re.compile(r"^\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$", re.M)
STAR_IMPORT = re.compile(r"^\s*import\s+[\w.]+\.[*]\s*$", re.M)
STRING_LITERAL = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'')


def code_only(line: str) -> str:
    """السطر بلا نصوص ولا تعليق — للبحث عن استعمال حقيقيّ لا ذِكر.

    **والسبب مقيس (٢٠٢٦-١١-١٧):** ثلاثة من ست نتائج كاذبة جاءت من مجرّد الذِكر:
    `label = "AppIcon"` (نصّ Crossfade) و`ConfigBackupInventory.Section` و
    `CubeLayoutMath.RAIL_APEX` (استعمال مؤهَّل). والنصّ المؤهَّل أو داخل نصّ ليس
    استعمالًا للرمز الخاصّ، فالفحص النصّيّ يجب أن يرى الشيفرة وحدها.
    """
    without_strings = STRING_LITERAL.sub('""', line)
    return without_strings.split("//", 1)[0]


def bare_use(pattern: re.Pattern[str], code: str) -> bool:
    """استعمال غير مؤهَّل: لا يسبقه `.` ولا `::` (فالمؤهَّل رمز آخر)."""
    for match in pattern.finditer(code):
        before = code[: match.start()].rstrip()
        if before.endswith((".", ":", "$")):
            continue
        return True
    return False

VISIBILITY = ("private", "internal", "public", "protected")
KINDS = ("object", "class", "interface", "typealias", "val", "var", "fun")
MODIFIERS = {
    "const", "inline", "suspend", "data", "enum", "sealed", "abstract", "open",
    "override", "operator", "value", "annotation", "companion", "expect", "actual",
    "external", "tailrec", "infix", "lateinit", "noinline", "crossinline", "vararg",
}
IDENT = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")


def declared_name(line: str) -> tuple[str, str] | None:
    """(الرؤية، الاسم) إن كان السطر إعلانًا — وإلا `None`.

    ويُحلّل الدالّة الامتدادية حلًّا صحيحًا: في `private fun Char.isDigit()` الاسم
    `isDigit` لا `Char`. (هذا الخطأ وحده أنتج ٧٧ نتيجة كاذبة في أول تشغيل.)
    """
    stripped = line.strip()
    if not stripped or stripped.startswith(("//", "*", "/*", "@")):
        return None

    tokens = stripped.split()
    visibility = "public"
    if tokens and tokens[0] in VISIBILITY:
        visibility = tokens.pop(0)

    # تُتخطّى المُعدِّلات بالترتيب الحرّ (private const val، internal suspend fun…).
    while tokens and tokens[0] in MODIFIERS:
        tokens.pop(0)
    if not tokens:
        return None

    head = tokens[0]
    if head not in KINDS:
        return None

    rest = stripped[stripped.find(head) + len(head):].lstrip()

    if head == "fun":
        # <T> ثم اسم قد يسبقه مستقبِل: Type.name( أو pkg.Type.name(
        rest = re.sub(r"^<[^>]*>\s*", "", rest)
        match = re.search(r"([A-Za-z_][A-Za-z0-9_]*)\s*\(", rest)
        if not match:
            return None
        return visibility, match.group(1)

    match = IDENT.match(rest)
    if not match:
        return None
    return visibility, match.group(0)


def top_level_declarations(text: str) -> list[tuple[str, str]]:
    """إعلانات على مستوى الملفّ (العمود صفر) — أي المتاحة لهذا الملفّ وحده إن كانت private."""
    out: list[tuple[str, str]] = []
    for line in text.splitlines():
        if line[:1].isspace() or not line.strip():
            continue
        parsed = declared_name(line)
        if parsed:
            out.append(parsed)
    return out


def all_declared_names(text: str) -> set[str]:
    """كل اسم يُعلنه الملفّ بأيّ رؤية وأيّ إزاحة — للاستثناء (رمز مختلف لا استعمال خاطئ)."""
    names = set()
    for line in text.splitlines():
        parsed = declared_name(line)
        if parsed:
            names.add(parsed[1])
    return names


def analyse(tree: Path) -> list[str]:
    files = sorted(tree.rglob("*.kt"))
    texts: dict[Path, str] = {}
    packages: dict[Path, str] = {}
    declared: dict[Path, set[str]] = {}
    imported: dict[Path, set[str]] = {}
    private: dict[tuple[str, str], Path] = {}

    for path in files:
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        texts[path] = text
        found = PACKAGE.search(text)
        packages[path] = found.group(1) if found else ""
        declared[path] = all_declared_names(text)
        names = set()
        for target, alias in IMPORT.findall(text):
            names.add(alias or target.rsplit(".", 1)[-1])
        imported[path] = names
        for visibility, name in top_level_declarations(text):
            # الأسماء الكبيرة فقط: أقلّها التباسًا في الفحص النصّيّ (حدّ معلن في الترويسة).
            if visibility == "private" and name[:1].isupper() and len(name) >= 4:
                private[(packages[path], name)] = path

    findings: list[str] = []
    for path in files:
        package = packages.get(path)
        if package is None:
            continue
        lines = texts[path].splitlines()
        # **نجم الاستيراد يجعل الحكم النصّيّ كذبًا محتملًا:** `import androidx.compose.material3.*`
        # يجلب `FilterChip` العامّ، ولا سبيل نصيًّا إلى معرفة ما يجلبه. فتُترك هذه الملفّات
        # بلا بلاغ بدل بلاغ كاذب — والحدّ معلن في الترويسة: إشارة لا مُصرّف.
        star = bool(STAR_IMPORT.search(texts[path]))
        for (owner_package, name), owner in private.items():
            if owner_package != package or path == owner:
                continue
            if name in declared.get(path, set()):
                continue  # الملفّ يعرّف الاسم بنفسه: رمز آخر، لا استعمال لرمز غيره.
            if name in imported.get(path, set()):
                continue  # رمز مكتبة بالاسم نفسه (FilterChip، AppIcon…): لا علاقة له بالخاصّ.
            pattern = re.compile(rf"\b{re.escape(name)}\b")
            for number, line in enumerate(lines, 1):
                stripped = line.lstrip()
                if stripped.startswith(("//", "*", "/*", "@")):
                    continue
                if not bare_use(pattern, code_only(line)):
                    continue
                if star:
                    continue  # نجم استيراد: لا يُحكم نصيًّا (انظر التعليق أعلاه)
                if True:
                    try:
                        owner_label = owner.relative_to(ROOT)
                    except ValueError:
                        owner_label = owner
                    findings.append(
                        f"{path}:{number}: «{name}» مُعلَن private في {owner_label} ({package}) ويُستعمل هنا"
                    )
                    break
    return findings


def run_assert(tree: Path) -> int:
    findings = analyse(tree)
    if findings:
        print("✗ رموز private تُستعمل من ملفّات أخرى (المُصرّف سيرفضها):")
        for finding in findings:
            print("  " + finding)
        print(f"المجموع: {len(findings)} — وسّع الرمز إلى internal أو انقله إلى موضعه.")
        return 1
    print("✓ رؤية Kotlin: لا رمز private مُستعملًا خارج ملفّه في الحزمة نفسها")
    return 0


def self_test() -> int:
    import tempfile

    cases: list[tuple[str, dict[str, str], bool]] = [
        (
            "private object يُستعمل من ملفّ آخر",
            {
                "a/A.kt": "package p\nprivate object Palette { val Red = 1 }\n",
                "a/B.kt": "package p\nobject Use { val c = Palette.Red }\n",
            },
            True,
        ),
        (
            "private داخليّ لا يُستعمل",
            {
                "a/A.kt": "package p\nprivate object Palette { val Red = 1 }\n",
                "a/B.kt": "package p\nobject Use { val c = 2 }\n",
            },
            False,
        ),
        (
            "اسم متطابق معرَّف في الملفّ الآخر (رمز آخر)",
            {
                "a/A.kt": "package p\nprivate val TurboPad = 1\n",
                "a/B.kt": "package p\nprivate val TurboPad = 2\nobject Use { val c = TurboPad }\n",
            },
            False,
        ),
        (
            "حزمة مختلفة لا تُحتسب",
            {
                "a/A.kt": "package p\nprivate object Palette { val Red = 1 }\n",
                "b/B.kt": "package q\nobject Use { val c = Palette.Red }\n",
            },
            False,
        ),
        (
            "تعليق يذكر الاسم لا يُحتسب",
            {
                "a/A.kt": "package p\nprivate object Palette { val Red = 1 }\n",
                "a/B.kt": "package p\n// Palette هنا في تعليق فقط\nobject Use { val c = 2 }\n",
            },
            False,
        ),
        (
            "internal يُستعمل من ملفّ آخر — مسموح",
            {
                "a/A.kt": "package p\ninternal object Palette { val Red = 1 }\n",
                "a/B.kt": "package p\nobject Use { val c = Palette.Red }\n",
            },
            False,
        ),
        (
            "دالّة امتدادية صغيرة الاسم: خارج نطاق البوّابة (حدّ معلن)",
            {
                "a/A.kt": "package p\nprivate fun Char.isDigit(): Boolean = this in '0'..'9'\n",
                "a/B.kt": "package p\nobject Use { fun f(c: Char) = c.isDigit() }\n",
            },
            False,
        ),
        (
            "ثابت خاصّ مُعلن داخل كائن في الملفّ الآخر لا يُحتسب",
            {
                "a/A.kt": "package p\nprivate const val PREFS_NAME = \"a\"\n",
                "a/B.kt": (
                    "package p\nobject Prefs {\n"
                    "    private const val PREFS_NAME = \"b\"\n"
                    "    fun f() = PREFS_NAME\n}\n"
                ),
            },
            False,
        ),
        (
            "رمز مكتبة بالاسم نفسه مستورد ⇒ لا يُحتسب",
            {
                "a/A.kt": "package p\nprivate fun FilterChip(x: Int) = x\n",
                "a/B.kt": "package p\nimport androidx.compose.material3.FilterChip\nobject Use { val c = 1 }\n",
            },
            False,
        ),
        (
            "استعمال مؤهَّل أو داخل نصّ لا يُحتسب",
            {
                "a/A.kt": "package p\nprivate const val RAIL_APEX = 1\nprivate fun AppIcon() = 1\n",
                "a/B.kt": (
                    "package p\nobject Use {\n"
                    "    val a = CubeMath.RAIL_APEX\n"
                    "    val b = \"AppIcon\"\n}\n"
                ),
            },
            False,
        ),
        (
            "نجم استيراد مكتبة يُبقي الحكم غير محسوم ⇒ لا بلاغ",
            {
                "a/A.kt": "package p\nprivate fun FilterChip(x: Int) = x\n",
                "a/B.kt": "package p\nimport androidx.compose.material3.*\nobject Use { val c = FilterChip(1) }\n",
            },
            False,
        ),
        (
            "استعمال داخل صنف عبر الملفّات يُكتشف",
            {
                "a/A.kt": "package p\nprivate object TurboPalette { val Cyan = 1 }\n",
                "a/B.kt": "package p\nclass Use { val c = TurboPalette.Cyan }\n",
            },
            True,
        ),
    ]

    failed = 0
    for label, files, expected_fail in cases:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for name, content in files.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(content, encoding="utf-8")
            got = bool(analyse(root))
            ok = got == expected_fail
            failed += 0 if ok else 1
            print(
                f"  {'✓' if ok else '✗'} {label}: متوقَّع "
                f"{'FAIL' if expected_fail else 'PASS'} · حصلنا {'FAIL' if got else 'PASS'}"
            )

    print(f"قياس الأداة: {len(cases)} حالة، فشل {failed}")
    return 1 if failed else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--assert", dest="assert_mode", action="store_true",
                        help="يفشل عند أي استعمال عبر الملفّات")
    parser.add_argument("--self-test", action="store_true", help="يقيس الأداة على حالات معروفة")
    parser.add_argument("--tree", default=str(SOURCE_ROOT), help="جذر المصادر (للتشخيص)")
    args = parser.parse_args()

    if args.self_test:
        return self_test()
    return run_assert(Path(args.tree))


if __name__ == "__main__":
    sys.exit(main())
