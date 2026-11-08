#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""نصوص لاتينية مكتوبة بيد داخل الواجهة — قياسٌ لا حكم.

**لماذا وُجد:** صدق اللغة (§٥.٥ من `MAX_MANAGER_UX_UI_PROMPT.md`) يعني ألّا يقرأ المستخدم
العربي جملةً إنجليزية مضروبة في الكود. والبوابة الحالية (`i18n_coverage.py`) تقيس **الموارد**
(Mفتاح ناقص في لغة، specifier خاطئ)، ولا ترى نصًّا لم يدخل المورد أصلًا. فهذا القياس يسدّ الثغرة:
يجرد `Text("…")` و`title = "…"` وأمثالها في `ui/**`، ويقارن العدد بسقفٍ مكتوب.

**وحدّه المُعلَن:** لا يفهم Kotlin. يقرأ سطورًا، فحرفٌ لاتينيّ في تعليقٍ لا يُحسب (التعليقات
تُسقَط أولًا)، ونصٌّ يُبنى بـ`buildString` لا يُرى، ونصٌّ يُحلّ وقت التشغيل من موردٍ ليس هنا.
فهو **مؤشّر تراجع** لا إثبات تعريب. ولا يُفشل البناء بلا خطّ أساس مكتوب.

الاستعمال:
    python3 tools/hardcoded_ui_text.py                 # يسرد الأعداد والمواضع
    python3 tools/hardcoded_ui_text.py --json          # للقراءة الآلية
    python3 tools/hardcoded_ui_text.py --assert        # يفشل إن زاد العدد عن السقف
    python3 tools/hardcoded_ui_text.py --write-baseline
"""

from __future__ import annotations

import argparse
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
UI = ROOT / "manager" / "app" / "src" / "main" / "java" / "nd" / "max" / "ui"
BASELINE = ROOT / "tools" / "hardcoded_ui_text_baseline.json"

# المواضع التي تُرى: معاملات تُطبع أو تُنطق. لا نطارد كل نصّ في الملف.
PATTERNS = [
    re.compile(r'\bText\(\s*"((?:[^"\\]|\\.)*)"'),
    re.compile(r'\btext\s*=\s*"((?:[^"\\]|\\.)*)"'),
    re.compile(r'\btitle\s*=\s*"((?:[^"\\]|\\.)*)"'),
    re.compile(r'\bsummary\s*=\s*"((?:[^"\\]|\\.)*)"'),
    re.compile(r'\bplaceholder\s*=\s*"((?:[^"\\]|\\.)*)"'),
    re.compile(r'\bcontentDescription\s*=\s*"((?:[^"\\]|\\.)*)"'),
    re.compile(r'\bheadline\s*=\s*"((?:[^"\\]|\\.)*)"'),
    re.compile(r'\bdetail\s*=\s*"((?:[^"\\]|\\.)*)"'),
]

ARG_PLACEHOLDER = re.compile(r"%(\d+\$)?[sdfx]")
LATIN = re.compile(r"[A-Za-z]{2,}")


def strip_comments(source: str) -> str:
    """يُسقط تعليقات السطر والكتلة معًا مع الحفاظ على الأسطر (ليبقى رقم السطر صحيحًا)."""
    out = []
    i = 0
    n = len(source)
    in_line = False
    in_block = False
    in_string = False
    while i < n:
        ch = source[i]
        nxt = source[i + 1] if i + 1 < n else ""
        if in_line:
            if ch == "\n":
                in_line = False
                out.append(ch)
            i += 1
            continue
        if in_block:
            if ch == "*" and nxt == "/":
                in_block = False
                i += 2
                continue
            out.append("\n" if ch == "\n" else " ")
            i += 1
            continue
        if in_string:
            out.append(ch)
            if ch == "\\":
                if i + 1 < n:
                    out.append(source[i + 1])
                i += 2
                continue
            if ch == '"':
                in_string = False
            i += 1
            continue
        if ch == "/" and nxt == "/":
            in_line = True
            i += 2
            continue
        if ch == "/" and nxt == "*":
            in_block = True
            i += 2
            continue
        if ch == '"':
            in_string = True
        out.append(ch)
        i += 1
    return "".join(out)


def looks_like_prose(literal: str) -> bool:
    """هل يُقرأ هذا نصًّا للمستخدم، أم مفتاحًا داخليًّا؟

    «Copied» منشورٌ يُقرأ. و«primary» و«videoFade» و«BannerPidAnim» ليست نصوصًا: الأولى
    مفتاح لون، والأخريان أسماء تسميات رسوم متحركة في Compose — وتضخيم العدّاد بها يجعل
    المقياس يقيس الكود لا الواجهة.
    """
    text = literal.strip()
    if not text:
        return False
    if " " in text:
        return True
    if text.islower():
        return False
    if re.fullmatch(r"[a-z]+[A-Z][A-Za-z]*", text):
        return False
    return bool(re.fullmatch(r"[A-Z][a-z]+[!؟.]?", text))


def offenders() -> list[dict]:
    found = []
    if not UI.is_dir():
        return found
    for path in sorted(UI.rglob("*.kt")):
        try:
            source = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        clean = strip_comments(source)
        for lineno, line in enumerate(clean.splitlines(), start=1):
            for pattern in PATTERNS:
                for match in pattern.finditer(line):
                    literal = match.group(1)
                    # ما ليس نصًّا يُقرأ: قوالب حساب، ومفاتيح، ورموز بلا كلمة لاتينية.
                    if not LATIN.search(literal):
                        continue
                    if ARG_PLACEHOLDER.search(literal):
                        continue
                    if not looks_like_prose(literal):
                        continue
                    found.append(
                        {
                            "file": str(path.relative_to(ROOT)),
                            "line": lineno,
                            "text": literal,
                        }
                    )
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true", help="اخراج آلي")
    parser.add_argument("--assert", dest="assert_", action="store_true", help="يفشل فوق السقف")
    parser.add_argument("--write-baseline", action="store_true", help="يكتب خطّ الأساس الحالي")
    parser.add_argument("--limit", type=int, default=25, help="عدد المواضع المعروضة")
    args = parser.parse_args()

    items = offenders()
    total = len(items)
    by_file: dict[str, int] = {}
    for item in items:
        by_file[item["file"]] = by_file.get(item["file"], 0) + 1

    if args.write_baseline:
        BASELINE.write_text(
            json.dumps({"total": total, "by_file": by_file}, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"خطّ الأساس مكتوب: {total} نصًّا في {len(by_file)} ملفًا → {BASELINE.relative_to(ROOT)}")
        return 0

    if args.json:
        print(json.dumps({"total": total, "by_file": by_file, "items": items}, ensure_ascii=False))
    else:
        print(f"نصوص لاتينية ظاهرة في ui/** : {total} في {len(by_file)} ملفًا")
        for name, count in sorted(by_file.items(), key=lambda kv: -kv[1])[: args.limit]:
            print(f"  {count:4d}  {name}")

    if not args.assert_:
        return 0

    if not BASELINE.exists():
        print("لا خطّ أساس — شغّل --write-baseline مرّة، ثم يُقاس التراجع.", file=sys.stderr)
        return 2

    ceiling = json.loads(BASELINE.read_text(encoding="utf-8")).get("total", 0)
    if total > ceiling:
        print(f"زيادة عن السقف: {total} > {ceiling} — نصٌّ جديد بيدٍ في الواجهة.", file=sys.stderr)
        return 1
    print(f"ضمن السقف: {total} ≤ {ceiling}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
