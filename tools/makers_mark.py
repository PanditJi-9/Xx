#!/usr/bin/env python3
"""وسم الصانع — هندسة واحدة يرسمها التطبيق والصفحة، وبوّابة تمنع انزياحهما.

لماذا وُجد هذا الملف
--------------------
ملاحظة التقييم: «استخدام الذكاء الاصطناعي مفرط… وبالنسبة للصور المصغّرة اعتمد تصميمًا خاصًّا
بالتطبيقات بدلًا من أدوات توليد الصور». فالوسم **يُرسم ولا يُولَّد**: خطٌّ واحد متّصل (حرف M)
نقاطه أدناه، تُخرج منه ملفّات الصفحة، وتُقارَن بحرفيّاتها مع رسمة التطبيق في
`MaxSignatureMark.kt`. وثلاثة عارضين لهذه الهندسة — الصفحة، والتطبيق، والصور المصغّرة —
تعني ثلاث فرص للانزياح؛ فالبوّابة تُغلقها كلها:

  ① ملفّا SVG في `docs/assets/` = إخراج هذه الأرقام حرفيًّا (تغيير يدويّ في ملفّ يُحمرّ).
  ② أرقام `MaxSignatureMark.kt` = الأرقام نفسها (رسم التطبيق لا ينزاح عن الصفحة).
  ③ اللونان من `MaxTokens.kt` نفسها (`PositiveOnDark` · `PositiveOnLight`) — لا لون مخترع.

الأوامر:
    python3 tools/makers_mark.py --generate    # يكتب الملفّين من الأرقام أدناه
    python3 tools/makers_mark.py --assert      # يقيس ①②③ ويخرج بـ1 عند أي انزياح
    python3 tools/makers_mark.py --self-test   # يكسر المدخلات عمدًا ويتوقّع أن الفحص يكشفها
    python3 tools/makers_mark.py --json        # تقرير مفصّل
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)

# ── الهندسة الواحدة ──────────────────────────────────────────────────────────
# فضاء [0, VIEW]، والمحور y نحو الأسفل كما في SVG. النقاط كلها داخل [8.5, 91.5] ومع نصف
# سُمك القلم (3.5) يبقى المدى المرئيّ داخل [5, 95] — فلا يلامس حدّ اللوحة ولا يُقصّ.
VIEW = 100.0
STROKE_W = 7.0

# خطّ واحد: move ثم سبع قطع cubicTo — (c1x, c1y, c2x, c2y, x, y) لكل قطعة.
SEGMENTS: list[float] = [
    8.5, 86.0,
    8.6, 61.0, 9.4, 39.0, 11.5, 20.5,      # الساق اليسرى: انحناء خفيف ثم قمّة مستديرة
    12.0, 15.0, 16.5, 14.5, 19.5, 19.0,
    29.5, 43.0, 40.5, 59.5, 48.5, 69.0,    # نزول إلى الوادي (أعمق من مستقيم — نبض اليد)
    57.0, 57.0, 68.0, 34.0, 78.5, 17.5,    # صعود إلى القمّة اليمنى
    81.0, 13.5, 85.0, 14.0, 86.0, 20.0,
    87.5, 39.0, 89.0, 63.0, 91.5, 87.0,    # الساق اليمنى تنساب يمينًا وهي تهبط — خروج القلم
]

SVG_DARK = os.path.join(ROOT, "docs", "assets", "makers-mark-dark.svg")
SVG_LIGHT = os.path.join(ROOT, "docs", "assets", "makers-mark-light.svg")
KOTLIN = os.path.join(ROOT, "manager", "app", "src", "main", "java", "nd", "max", "ui",
                      "component", "MaxSignatureMark.kt")
TOKENS = os.path.join(ROOT, "manager", "app", "src", "main", "java", "nd", "max", "ui",
                      "design", "MaxTokens.kt")


def path_d() -> str:
    """`d` الخاصّ بالمسار — تنسيق ثابت (%g) كي يقيس الفحص البايتات لا الشكل."""
    out = [f"M {SEGMENTS[0]:g} {SEGMENTS[1]:g}"]
    i = 2
    while i + 5 < len(SEGMENTS):
        out.append("C " + " ".join(f"{v:g}" for v in SEGMENTS[i:i + 6]))
        i += 6
    return " ".join(out)


def svg(dark: bool) -> str:
    """ملفّ الصفحة — إخراج حتميّ: المدخل نفسه يعطي البايتات نفسها دائمًا."""
    stroke = "#5FD9AC" if dark else "#0B6B4F"
    scheme = "dark" if dark else "light"
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="96" height="96"\n'
        '     role="img" aria-labelledby="mm-mark-t mm-mark-d">\n'
        "  <title id=\"mm-mark-t\">Maker's mark</title>\n"
        '  <desc id="mm-mark-d">A single-stroke M drawn in one line: the maker\'s mark of MaxManager. '
        f'This file is the {scheme}-scheme rendering of the same geometry the app draws.</desc>\n'
        "  <!-- الوسم يُرسم من tools/makers_mark.py — وهندسته نفسها في التطبيق (MaxSignatureMark.kt)،\n"
        "       والبوّابة تقيس التطابق. ولا يُنقل من هنا إلى هناك يدويًّا: التعديل من الأداة وحدها. -->\n"
        f'  <path d="{path_d()}"\n'
        f'        fill="none" stroke="{stroke}" stroke-width="{STROKE_W:g}" '
        'stroke-linecap="round" stroke-linejoin="round"/>\n'
        "</svg>\n"
    )


def kotlin_numbers(text: str) -> list[float] | None:
    """أرقام `floatArrayOf(...)` داخل الملفّ — و`None` إن غاب التعريف."""
    m = re.search(r"floatArrayOf\(([^)]*)\)", text, re.S)
    if not m:
        return None
    return [float(t.rstrip("f")) for t in re.findall(r"[-+]?\d+(?:\.\d+)?f?", m.group(1))]


def kotlin_const(text: str, name: str) -> float | None:
    m = re.search(rf"\b{name}\s*=\s*([\d.]+)f", text)
    return float(m.group(1)) if m else None


def token_color(tokens_text: str, name: str) -> str | None:
    m = re.search(rf"\b{name}\s*=\s*Color\(0x(FF[0-9A-Fa-f]{{6}})\)", tokens_text)
    return f"#{m.group(1)[2:]}" if m else None


def check(svg_dark: str, svg_light: str, kotlin: str, tokens: str) -> list[str]:
    """فحص خالص على النصوص — كي يقيسه `--self-test` بلا لمس القرص."""
    problems: list[str] = []

    # ① الملفّان = الإخراج الحتميّ حرفيًّا
    for name, text, dark in (("makers-mark-dark.svg", svg_dark, True),
                             ("makers-mark-light.svg", svg_light, False)):
        want = svg(dark)
        if text != want:
            line = next((i + 1 for i, (a, b) in enumerate(zip(text.splitlines(), want.splitlines()))
                         if a != b), None)
            problems.append(f"① {name}: لا يطابق الإخراج من الأداة"
                            + (f" (أوّل اختلاف في السطر {line})" if line else " (طول مختلف)"))
        for tag in ("<title", "<desc"):
            if tag not in text:
                problems.append(f"① {name}: لا {tag}> — الصورة بلا نصّ بديل")

    # ② أرقام التطبيق = الأرقام نفسها
    nums = kotlin_numbers(kotlin)
    if nums is None:
        problems.append("② MaxSignatureMark.kt: لا floatArrayOf — الرسم مفقود")
    elif len(nums) != len(SEGMENTS):
        problems.append(f"② MaxSignatureMark.kt: عدد الأرقام {len(nums)} ≠ {len(SEGMENTS)}")
    else:
        for i, (a, b) in enumerate(zip(nums, SEGMENTS)):
            if abs(a - b) > 1e-9:
                problems.append(f"② MaxSignatureMark.kt: الرقم {i + 1} = {a:g} ≠ {b:g} (انزياح عن الأداة)")
                break
    for name, want in (("MARK_VIEW", VIEW), ("MARK_STROKE_W", STROKE_W)):
        got = kotlin_const(kotlin, name)
        if got is None:
            problems.append(f"② MaxSignatureMark.kt: لا {name}")
        elif abs(got - want) > 1e-9:
            problems.append(f"② MaxSignatureMark.kt: {name} = {got:g} ≠ {want:g}")

    # ③ اللونان من ثوابت التطبيق
    dark_hex = token_color(tokens, "PositiveOnDark")
    light_hex = token_color(tokens, "PositiveOnLight")
    if not dark_hex or not light_hex:
        problems.append("③ MaxTokens.kt: لا PositiveOnDark/PositiveOnLight")
    else:
        if f'stroke="{dark_hex}"' not in svg_dark:
            problems.append(f"③ makers-mark-dark.svg: القلم ليس {dark_hex} (PositiveOnDark)")
        if f'stroke="{light_hex}"' not in svg_light:
            problems.append(f"③ makers-mark-light.svg: القلم ليس {light_hex} (PositiveOnLight)")

    return problems


def _read(path: str) -> str:
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def collect() -> tuple[str, str, str, str]:
    return (_read(SVG_DARK), _read(SVG_LIGHT), _read(KOTLIN), _read(TOKENS))


def self_test() -> int:
    """يقيس الفحص نفسه: المدخل السليم يمرّ، وكل كسر مقصود يُكشف من بابه."""
    ok_dark, ok_light, ok_kt, ok_tok = collect()
    fails = 0

    def case(name: str, expect: str, **overrides: str) -> None:
        nonlocal fails
        args = dict(svg_dark=ok_dark, svg_light=ok_light, kotlin=ok_kt, tokens=ok_tok)
        args.update(overrides)
        probs = check(**args)
        hit = any(expect in p for p in probs)
        print(f"  {'✓' if hit else '✗'} {name}" + ("" if hit else f" — لم يُكشف (المشاكل: {probs})"))
        fails += 0 if hit else 1

    # المدخل السليم أولًا — وإلا فبوّابة تصرخ دائمًا لا تقيس شيئًا
    clean = check(svg_dark=ok_dark, svg_light=ok_light, kotlin=ok_kt, tokens=ok_tok)
    print(f"  {'✓' if not clean else '✗'} المدخل السليم يمرّ" + ("" if not clean else f" — {clean}"))
    fails += 0 if not clean else 1

    case("SVG: رقم مُبدَّل في المسار", "①", svg_dark=ok_dark.replace("48.5", "49.5"))
    case("SVG: لون مُبدَّل", "③", svg_dark=ok_dark.replace("#5FD9AC", "#123456"))
    case("SVG: وسم desc محذوف", "desc", svg_dark=ok_dark.replace("<desc", "<dsc").replace("</desc>", "</dsc>"))
    case("Kotlin: إحداثيّ منزاح", "②", kotlin=ok_kt.replace("91.5f", "92.5f"))
    case("Kotlin: عدد أرقام ناقص", "②", kotlin=ok_kt.replace("87.5f, 39.0f, 89.0f, 63.0f, 91.5f, 87.0f,", ""))
    case("Kotlin: سُمك القلم مُبدَّل", "MARK_STROKE_W", kotlin=ok_kt.replace("MARK_STROKE_W = 7f", "MARK_STROKE_W = 8f"))

    print("الأداة تقيس ما تدّعيه." if not fails else f"الأداة لا تقيس ما تدّعيه ({fails} كسرًا مرّ).")
    return 0 if not fails else 1


def main() -> int:
    ap = argparse.ArgumentParser(description="وسم الصانع: توليد ملفّي الصفحة وفحص التطابق مع التطبيق")
    ap.add_argument("--generate", action="store_true", help="يكتب ملفّي SVG من الأرقام أعلاه")
    ap.add_argument("--assert", dest="assert_", action="store_true", help="يفشل عند أي انزياح")
    ap.add_argument("--self-test", dest="self_test", action="store_true", help="يقيس الفحص نفسه")
    ap.add_argument("--json", dest="as_json", action="store_true")
    args = ap.parse_args()

    if args.self_test:
        return self_test()

    if args.generate:
        for path, dark in ((SVG_DARK, True), (SVG_LIGHT, False)):
            with open(path, "w", encoding="utf-8") as fh:
                fh.write(svg(dark))
            print(f"كُتب {os.path.relpath(path, ROOT)} ({os.path.getsize(path)}B)")
        return 0

    problems = check(*collect())
    if args.as_json:
        print(json.dumps({"problems": problems, "segments": len(SEGMENTS)},
                         ensure_ascii=False, indent=2))
        return 0
    print(f"وسم الصانع: {len(SEGMENTS)} رقمًا · القلم {STROKE_W:g} من {VIEW:g} · عيوب: {len(problems)}")
    for p in problems:
        print(f"  ✗ {p}")
    if problems and args.assert_:
        print("\nبوّابة وسم الصانع: exit 1")
        return 1
    print("بوّابة وسم الصانع: exit 0")
    return 0


if __name__ == "__main__":
    sys.exit(main())
