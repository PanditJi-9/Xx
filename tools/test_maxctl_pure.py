#!/usr/bin/env python3
"""قياس منطق دالّتَي `maxctl` الخالصتين — **بالكود نفسه** لا بمرآة عنه.

## لماذا هذه الأداة

وعد المالك الصريح: «اذا اردت جعله مجانا لكل المستخدمين لفترة معينه … فيجب أن يتوقف العداد
للمستخدمين المدفوعين». والمنطق الذي يحقّقه دالّة واحدة في TypeScript داخل دالّة Deno — ولا Deno
في هذه البيئة، و`node` هنا **مبنيّ بلا دعم TypeScript** (`ERR_NO_TYPESCRIPT` — مقيس)، ولا مُصرّف
TS في أي حزمة JVM (الدالّة خادمٌ لا تطبيق).

والدالّتان المقيسَتان اليوم، وكلتاهما تعمل على **الخادم** ولا تُقاس بغيرهما:

  ① `addDaysSkipping` — إيقاف العدّاد داخل النوافذ المجانية (وعد المالك أعلاه).
  ② `rateLimitDecision` — سقف المعدّل على الوجهتين العامّتين (`license.redeem` · `license.validate`)،
     وهو البند الذي كان مُعلَنًا في المواصفة «**غير مُنفَّذ**» حتى هذه الجولة.

فالأداة تفعل أربعة أشياء، كلّها معلنة:

1. **تستخرج نصّ الدالّة حرفيًّا** من `supabase/functions/maxctl/index.ts` بموازنة الأقواس
   — فلا نسخة ثانية تتقادم؛ لو تغيّر المنطق في الدالّة تغيّر القياس معه.
2. **تُزيل ترميزات الأنواع وحدها** (‏`: number` · `: RateAttempt[]` …) بإزاحة محسوبة على النصّ
   الأصلي (حذف مقاطع بالمواضع لا استبدال)، **وتطبع كل قطعة أزالتها** ليراها القارئ، وتؤكّد أن
   الناتج خلا من أي ترميز متبقٍّ — فالمقيس هو المنطق لا شكل الكتابة.
3. **تشغّلها بـ`node` خالص** على سائق حالات لكل دالّة:

   • `addDaysSkipping`: بلا نوافذ · نافذة داخلية · تقاطع النهاية · **متلاصقتان** (لا حساب مزدوج
     عند الحدّ `[أ,ب)`+`[ب,ج)`) · متداخلتان (الاتحاد لا الجمع) · سارية قبل التفعيل · بعد الانتهاء ·
     الحتمية.
   • `rateLimitDecision`: بلا محاولات · سبع فشل (مسموح) · ثمانٍ (يُقفل الباب) · **العودة عند خروج
     ما يكفي** لا الأقدم وحده · سقف الساعة · سقف «العنف» على كل المحاولات · النجاح لا يُحتسب فشلًا
     · القديم خارج النافذة · **الحدّ بالضبط** (نافذة نصف مفتوحة) · محاولة مستقبلية لا تُلوّث العدّ
     · السبب المُعلَن عند كسر سقفين (الأبعد خروجًا) · الحتمية.

4. **تفحص بنية الملفّ كاملًا** (توازن الأقواس والنصوص والقوالب والتعليقات): لأنه **الملفّ الوحيد
   في المستودع بلا مُصرّف ولا بوابة** — لا Deno هنا، و`node` مبنيّ بلا دعم TypeScript
   (`ERR_NO_TYPESCRIPT` — مقيس)، و`kt_balance.py` يرى `.kt` و`.xml` وحدهما. وحدّه يُعلن: **لا يرى
   خطأ نوع** ولا مرجعًا غير محلول.

## الاستعمال

    python3 tools/test_maxctl_pure.py --assert
    python3 tools/test_maxctl_pure.py --self-test   # تكسر نسخة كل دالّة عمدًا وتتوقّع أن تُكشف
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
FUNCTION = REPO / "supabase" / "functions" / "maxctl" / "index.ts"
DAY = 86_400_000

# ══════════════════════════════════════════════════════════════════════════════════════════════
# سائق `addDaysSkipping` — النوافذ المجانية
# ══════════════════════════════════════════════════════════════════════════════════════════════

_JS_DAYS = r"""
__BODY__

const DAY = __DAY__;
const base = Date.parse("2026-01-01T00:00:00Z");
let failures = 0;

function check(name, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures += 1;
  console.log(`${ok ? "✓" : "✗"} ${name} — actual=${actual} expected=${expected}`);
}

// ① بلا نوافذ.
let r = addDaysSkipping(base, 30 * DAY, []);
check("بلا نوافذ: الانتهاء = البداية + المدّة", r.untilMs, base + 30 * DAY);
check("بلا نوافذ: صفر إيقاف", r.pausedMs, 0);

// ② نافذة كاملة داخل المدّة [٥ي,١٠ي).
r = addDaysSkipping(base, 30 * DAY, [{ from: base + 5 * DAY, to: base + 10 * DAY }]);
check("نافذة داخلية: الانتهاء يزيح ٥ أيام", r.untilMs, base + 35 * DAY);
check("نافذة داخلية: الإيقاف ٥ أيام", r.pausedMs, 5 * DAY);

// ③ نافذة تتقاطع مع النهاية: مدّة ١٠ أيام ونافذة [٨ي,٢٠ي) ⇒ انتهاء عند ٢٢ي.
r = addDaysSkipping(base, 10 * DAY, [{ from: base + 8 * DAY, to: base + 20 * DAY }]);
check("تقاطع النهاية: الانتهاء ٢٢ي", r.untilMs, base + 22 * DAY);
check("تقاطع النهاية: الإيقاف ١٢ي", r.pausedMs, 12 * DAY);

// ④ متلاصقتان [٥ي,١٠ي) و[١٠ي,١٥ي) ⇒ ١٠ أيام إيقاف مرّة واحدة (لا حساب مزدوج عند الحدّ).
r = addDaysSkipping(base, 20 * DAY, [
  { from: base + 5 * DAY, to: base + 10 * DAY },
  { from: base + 10 * DAY, to: base + 15 * DAY },
]);
check("متلاصقتان: الانتهاء ٣٠ي", r.untilMs, base + 30 * DAY);
check("متلاصقتان: الإيقاف ١٠ي (لا ٢٠)", r.pausedMs, 10 * DAY);

// ⑤ متداخلتان [٥ي,١٢ي) و[٨ي,١٥ي) ⇒ الاتحاد ١٠ أيام.
r = addDaysSkipping(base, 20 * DAY, [
  { from: base + 5 * DAY, to: base + 12 * DAY },
  { from: base + 8 * DAY, to: base + 15 * DAY },
]);
check("متداخلتان: الانتهاء ٣٠ي", r.untilMs, base + 30 * DAY);
check("متداخلتان: الإيقاف ١٠ي (الاتحاد لا الجمع)", r.pausedMs, 10 * DAY);

// ⑥ نافذة سارية قبل التفعيل [−٥ي, +٣ي) ⇒ تُحسب من التفعيل.
r = addDaysSkipping(base, 30 * DAY, [{ from: base - 5 * DAY, to: base + 3 * DAY }]);
check("سارية قبل التفعيل: الانتهاء ٣٣ي", r.untilMs, base + 33 * DAY);
check("سارية قبل التفعيل: الإيقاف ٣ أيام", r.pausedMs, 3 * DAY);

// ⑦ نافذة بعد الانتهاء ⇒ لا أثر.
r = addDaysSkipping(base, 10 * DAY, [{ from: base + 20 * DAY, to: base + 30 * DAY }]);
check("بعد الانتهاء: لا إزاحة", r.untilMs, base + 10 * DAY);
check("بعد الانتهاء: صفر إيقاف", r.pausedMs, 0);

// ⑧ الحتمية: نفس المدخلات ⇒ نفس المخرجات.
const again = addDaysSkipping(base, 20 * DAY, [
  { from: base + 5 * DAY, to: base + 12 * DAY },
  { from: base + 8 * DAY, to: base + 15 * DAY },
]);
check("الحتمية: الانتهاء", again.untilMs, base + 30 * DAY);
check("الحتمية: الإيقاف", again.pausedMs, 10 * DAY);

console.log(failures === 0 ? "\nOK — كل الحالات" : `\nFAILED — ${failures} حالة`);
process.exit(failures === 0 ? 0 : 1);
"""

# ══════════════════════════════════════════════════════════════════════════════════════════════
# سائق `rateLimitDecision` — سقف المعدّل
# ══════════════════════════════════════════════════════════════════════════════════════════════

_JS_RATE = r"""
__BODY__

const LIMITS = { minuteMs: 60000, maxFailuresMinute: 8, hourMs: 3600000, maxFailuresHour: 40, maxAttemptsMinute: 30 };
const now = Date.parse("2026-01-01T00:00:00Z");
let failures = 0;

function check(name, actual, expected) {
  const ok = actual === expected;
  if (!ok) failures += 1;
  console.log(`${ok ? "✓" : "✗"} ${name} — actual=${actual} expected=${expected}`);
}

const bad = (ageMs) => ({ at: now - ageMs, ok: false });
const good = (ageMs) => ({ at: now - ageMs, ok: true });
const ages = (list) => list.map((i) => bad(i * 1000));

// ① بلا محاولات ⇒ لا رفض.
let v = rateLimitDecision(now, [], LIMITS);
check("بلا محاولات: مسموح", v.blocked, false);
check("بلا محاولات: بلا انتظار", v.retryAfterSec, 0);
check("بلا محاولات: بلا سبب", v.reason, "");

// ② سبع فشل في الدقيقة ⇒ ما زال مسموحًا (السقف ثمانٍ).
v = rateLimitDecision(now, ages([1, 2, 3, 4, 5, 6, 7]), LIMITS);
check("٧ فشل: مسموح", v.blocked, false);
check("٧ فشل: العدّ ٧", v.failuresMinute, 7);

// ③ ثماني فشل في الدقيقة ⇒ الباب يُقفل، والعودة عند خروج الأقدم (عمره ٨ث) من النافذة: ٦٠−٨.
v = rateLimitDecision(now, ages([1, 2, 3, 4, 5, 6, 7, 8]), LIMITS);
check("٨ فشل: مرفوض", v.blocked, true);
check("٨ فشل: السبب فشل الدقيقة", v.reason, "failures_minute");
check("٨ فشل: الانتظار ٥٢ث", v.retryAfterSec, 52);

// ④ عشرة فشل، أقدمها عمره ٣٠ث ⇒ العودة عند خروج **ما يكفي** (الثالث الأقدم، عمره ٨ث) لا الأقدم:
//    ٦٠−٨ = ٥٢ث. ومن يقيس «الأقدم وحده» يقول ٣٠ث — فيبقى الطلب مرفوضًا بعد ثلاثين ثانية.
const ten = [bad(30000), bad(9000), bad(8000), bad(7000), bad(6000), bad(5000), bad(4000), bad(3000), bad(2000), bad(1000)];
v = rateLimitDecision(now, ten, LIMITS);
check("١٠ فشل: الانتظار ٥٢ث لا ٣٠", v.retryAfterSec, 52);
check("١٠ فشل: العدّ ١٠", v.failuresMinute, 10);

// ⑤ سقف الساعة: ٤٠ فشل خارج الدقيقة وداخل الساعة ⇒ مرفوض بسبب الساعة وحدها.
const hourly = [];
for (let i = 0; i < 40; i++) hourly.push(bad(120000 + i * 1000));
v = rateLimitDecision(now, hourly, LIMITS);
check("٤٠ في الساعة: مرفوض", v.blocked, true);
check("٤٠ في الساعة: السبب فشل الساعة", v.reason, "failures_hour");
check("٤٠ في الساعة: فشل الدقيقة صفر", v.failuresMinute, 0);
check("٤٠ في الساعة: الانتظار حتى خروج الأقدم (٣٤٤١ث)", v.retryAfterSec, 3441);

// ⑥ سقف «العنف»: ٣٠ محاولة **ناجحة** في الدقيقة ⇒ مرفوضة، فمعدّل التطبيق ليس ثلاثين نداءً.
const busy = [];
for (let i = 0; i < 30; i++) busy.push(good(1000 + i * 1000));
v = rateLimitDecision(now, busy, LIMITS);
check("٣٠ نجاحًا في الدقيقة: مرفوض", v.blocked, true);
check("٣٠ نجاحًا: السبب عنف المحاولات", v.reason, "attempts_minute");
check("٣٠ نجاحًا: الانتظار ٣٠ث", v.retryAfterSec, 30);

// ⑦ النجاح لا يُحتسب فشلًا: ثماني نجاحات ⇒ مسموح.
v = rateLimitDecision(now, ages([1, 2, 3, 4, 5, 6, 7, 8]).map((a) => ({ at: a.at, ok: true })), LIMITS);
check("٨ نجاحات: مسموح", v.blocked, false);
check("٨ نجاحات: لا فشل معدود", v.failuresMinute, 0);

// ⑧ القديم لا يُحتسب: ثماني فشل أعمارها > دقيقة ⇒ مسموح (وسقف الساعة لم يُبلَغ).
v = rateLimitDecision(now, ages([61, 62, 63, 64, 65, 66, 67, 68]), LIMITS);
check("٨ فشل أقدم من دقيقة: مسموح", v.blocked, false);
check("٨ فشل أقدم من دقيقة: فشل الدقيقة صفر", v.failuresMinute, 0);

// ⑨ الحدّ بالضبط (نافذة نصف مفتوحة): فشل عمره ٦٠٠٠٠ms **خرج** منها ⇒ المعدود ٧ لا ٨.
const edge = [bad(60000)];
for (let i = 1; i <= 7; i++) edge.push(bad(i * 1000));
v = rateLimitDecision(now, edge, LIMITS);
check("الحدّ بالضبط: المعدود ٧", v.failuresMinute, 7);
check("الحدّ بالضبط: مسموح", v.blocked, false);

// ⑩ محاولة بطابع مستقبليّ (ساعة متقدّمة) لا تُلوّث العدّ.
v = rateLimitDecision(now, [{ at: now + 5000, ok: false }], LIMITS);
check("مستقبلية: لا تُحتسب", v.failuresMinute, 0);
check("مستقبلية: مسموح", v.blocked, false);

// ⑪ كسر سقفين معًا ⇒ يُعلَن **الأبعد خروجًا** (الساعة: ٣٥١٠ث)، لا الأقرب (الدقيقة: ٣ث).
const both = [];
for (let i = 0; i < 40; i++) both.push(bad(60000 + i * 1000));
for (let i = 0; i < 9; i++) both.push(bad(50000 + i * 1000));
v = rateLimitDecision(now, both, LIMITS);
check("سقفان: السبب الأبعد خروجًا", v.reason, "failures_hour");
check("سقفان: الانتظار ٣٥١٠ث", v.retryAfterSec, 3510);

// ⑫ الحتمية: نفس المدخلات ⇒ نفس القرار حرفيًّا.
check("الحتمية: القرار نفسه", JSON.stringify(rateLimitDecision(now, ten, LIMITS)), JSON.stringify(rateLimitDecision(now, ten, LIMITS)));

console.log(failures === 0 ? "\nOK — كل الحالات" : `\nFAILED — ${failures} حالة`);
process.exit(failures === 0 ? 0 : 1);
"""

# ══════════════════════════════════════════════════════════════════════════════════════════════
# الأهداف: كل دالّة بنمط ترميزاتها وعلاماتها وكسرها المقصود
# ══════════════════════════════════════════════════════════════════════════════════════════════

TARGETS = [
    {
        "name": "addDaysSkipping",
        "why": "إيقاف العدّاد داخل النوافذ المجانية",
        # الترميزان الوحيدان في الدالّة — يُطبع كل ما يُزال منهما، فلا إزالة صامتة.
        "patterns": re.compile(r":\s*\{\s*from:\s*number;\s*to:\s*number\s*\}\[\]|:\s*number"),
        # علامات من **آخر** الجسم: لو وقف الاستخراج عند قوس النوع لعُرفت هنا فورًا.
        "markers": ("pausedMs += w.to - s;", "return { untilMs: cursor, pausedMs };"),
        # الكسر المقصود: تُحتسب النافذة كاملةً بدل تقاطعها ⇒ يجب أن تكشفه حالة «تقاطع النهاية».
        "mutation": ("pausedMs += w.to - s;", "pausedMs += w.to - w.from;"),
        "driver": _JS_DAYS,
        "substitutions": {"__DAY__": str(DAY)},
    },
    {
        "name": "rateLimitDecision",
        "why": "سقف المعدّل على الوجهتين العامّتين",
        "patterns": re.compile(
            r":\s*RateAttempt\[\]|:\s*RateLimits|:\s*RateVerdict|:\s*number\[\]|:\s*number"
        ),
        "markers": ("failuresMinute: failedMinute.length,", "blocked: worst.wait > 0,"),
        # الكسر المقصود: العودة عند خروج **الأقدم** وحده بدل خروج ما يكفي ⇒ يكشفه عدّ ١٠ وسقفان معًا.
        "mutation": ("const leaving = sorted[times.length - max];", "const leaving = sorted[0];"),
        "driver": _JS_RATE,
        "substitutions": {},
    },
]


def extract(source: str, name: str) -> str:
    """نصّ الدالّة كاملًا — بموازنة الأقواس لا بتخمين أسطر.

    وترتيبٌ لا يُختصر: **قوسا المعاملات أولًا** ثم قوس الجسم. فمعاملات `addDaysSkipping` تحوي
    `{ from: number; to: number }[]`، ومن يبدأ من أول `{` بعد الاسم يقف عند قوس النوع ويعيد
    **٩٥ محرفًا** بدل الدالّة كاملة — وهذا وقع هنا فعلًا وقِيس (٤ مطابقات `: number` مكان ٣)،
    فيُحرس عليه الآن بعلامة من آخر الجسم (انظر `prepare`).
    """
    start = source.index(f"function {name}(")
    depth = 0
    cursor = source.index("(", start)
    while cursor < len(source):
        char = source[cursor]
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                break
        cursor += 1
    depth = 0
    cursor = source.index("{", cursor)
    while cursor < len(source):
        char = source[cursor]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return source[start : cursor + 1]
        cursor += 1
    raise SystemExit(f"لم أجد نهاية الدالّة `{name}` في {FUNCTION}")


def strip_types(body: str, pattern: re.Pattern[str]) -> tuple[str, list[str]]:
    """يحذف ترميزات الأنواع **بالمواضع**، ويعيد الناتج وقطعه المحذوفة (للإعلان)."""
    matches = list(pattern.finditer(body))
    stripped = body
    removed: list[str] = []
    for match in reversed(matches):  # من النهاية: المواضع لا تتزحزح
        removed.append(match.group(0))
        stripped = stripped[: match.start()] + stripped[match.end() :]
    return stripped, list(reversed(removed))


# ══════════════════════════════════════════════════════════════════════════════════════════════
# بنية الملفّ كاملًا: توازن الأقواس والنصوص والتعليقات — **البنية لا الأنواع**
# ══════════════════════════════════════════════════════════════════════════════════════════════
#
# ولماذا هنا: `index.ts` هو **الملفّ الوحيد في المستودع بلا مُصرّف ولا بوابة** — لا Deno في هذه
# البيئة، و`node` مبنيّ بلا دعم TypeScript (`ERR_NO_TYPESCRIPT` — مقيس)، و`kt_balance.py` يرى
# `.kt` و`.xml` وحدهما. وتعديلات اليد على هذا الملفّ أخطرُ ما يُخطئ فيه قوسٌ ساقط أو نصّ غير مغلق،
# وهذا الفحص يمسك ذلك كلّه في ثوانٍ. **وحدّه يُعلن:** لا يرى خطأ نوع (`const x: string = 1` يمرّ)
# ولا مرجعًا غير محلول — فما لا يُقاس هنا يُقال، ولا يُدَّعى.

_REGEX_PREFIX = set("=(,:[!&|?{};+-*%~^<>")


def _starts_regex(text: str, at: int) -> bool:
    """هل `/` هنا بداية تعبير نمطي؟ الحكم بما قبله: لا قسمة بعد معرّف ولا بعد قوس مغلق."""
    cursor = at - 1
    while cursor >= 0 and text[cursor] in " \t\r\n":
        cursor -= 1
    if cursor < 0:
        return True
    return text[cursor] in _REGEX_PREFIX


def _skip_regex(text: str, at: int) -> int:
    """يتجاوز `/…/flags` مع أصناف `[...]` والإفلات، ويعيد الموضع بعد العلم الأخير."""
    cursor = at + 1
    in_class = False
    while cursor < len(text):
        char = text[cursor]
        if char == "\\":
            cursor += 2
            continue
        if char == "\n":
            break  # تعبير نمطي لا يمتدّ سطرين: نكمل من الموضع نفسه ولا نتوهّم نهاية
        if char == "[":
            in_class = True
        elif char == "]":
            in_class = False
        elif char == "/" and not in_class:
            cursor += 1
            break
        cursor += 1
    while cursor < len(text) and text[cursor].isalpha():
        cursor += 1
    return cursor


def ts_balance(text: str) -> list[str]:
    """يفحص البنية ويعيد قائمة المشاكل (فارغة = متوازن).

    ويمشي على الملفّ مرّة واحدة بسياق واحد في كل لحظة (شيفرة · نصّ · قالب · تعليق)، فيعرف القوس
    الساقط والنصّ غير المغلق والقالب غير المكتمل — و`${…}` داخل القوالب تُدفع كقوس وتُغلق فتعود
    إلى القالب، فلا تُخلط كتلة قالب بكتلة شيفرة.
    """
    problems: list[str] = []
    stack: list[tuple[str, int]] = []
    pairs = {")": "(", "]": "[", "}": "{"}
    mode = "code"
    line = 1
    cursor = 0
    while cursor < len(text):
        char = text[cursor]
        nxt = text[cursor + 1] if cursor + 1 < len(text) else ""
        if char == "\n":
            line += 1
            if mode == "line":
                mode = "code"
            cursor += 1
            continue
        if mode == "line":
            cursor += 1
            continue
        if mode == "block":
            if char == "*" and nxt == "/":
                mode = "code"
                cursor += 2
                continue
            cursor += 1
            continue
        if mode in ("single", "double"):
            if char == "\\":
                cursor += 2
                continue
            if (char == "'" and mode == "single") or (char == '"' and mode == "double"):
                mode = "code"
            cursor += 1
            continue
        if mode == "template":
            if char == "\\":
                cursor += 2
                continue
            if char == "`":
                mode = "code"
                cursor += 1
                continue
            if char == "$" and nxt == "{":
                stack.append(("${", line))
                mode = "code"
                cursor += 2
                continue
            cursor += 1
            continue
        # ── شيفرة ──
        if char == "/" and nxt == "/":
            mode = "line"
            cursor += 2
            continue
        if char == "/" and nxt == "*":
            mode = "block"
            cursor += 2
            continue
        if char == "'":
            mode = "single"
            cursor += 1
            continue
        if char == '"':
            mode = "double"
            cursor += 1
            continue
        if char == "`":
            mode = "template"
            cursor += 1
            continue
        if char == "/" and _starts_regex(text, cursor):
            cursor = _skip_regex(text, cursor)
            continue
        if char in "([{":
            stack.append((char, line))
            cursor += 1
            continue
        if char in ")]}":
            if not stack:
                problems.append(f"سطر {line}: `{char}` بلا فاتح")
                cursor += 1
                continue
            opener, opened_at = stack.pop()
            if opener == "${":
                if char != "}":
                    problems.append(f"سطر {line}: `{char}` يغلق `${{` المفتوح في سطر {opened_at}")
                mode = "template"
                cursor += 1
                continue
            if opener != pairs[char]:
                problems.append(f"سطر {line}: `{char}` لا يغلق `{opener}` المفتوح في سطر {opened_at}")
            cursor += 1
            continue
        cursor += 1

    if mode in ("single", "double", "template"):
        problems.append(f"نصّ/قالب غير مغلق (`{mode}`) في آخر الملفّ")
    if mode == "block":
        problems.append("تعليق كتلة غير مغلق في آخر الملفّ")
    for opener, opened_at in stack:
        problems.append(f"`{opener}` المفتوح في سطر {opened_at} بلا غلق")
    return problems


def prepare(source: str, target: dict) -> tuple[str, list[str]]:
    body = extract(source, target["name"])
    for marker in target["markers"]:
        if marker not in body:
            raise SystemExit(
                f"الاستخراج مبتور — العلامة `{marker}` غائبة عن نصّ الدالّة `{target['name']}`"
            )
    stripped, removed = strip_types(body, target["patterns"])
    leftovers = target["patterns"].search(stripped)
    if leftovers is not None:
        raise SystemExit(
            f"بقيت ترميزات أنواع بعد الإزاحة: {leftovers.group(0)!r} — توقّف (`{target['name']}`)"
        )
    return stripped, removed


def driver(target: dict, body: str, mutate: bool = False) -> str:
    """سائق الحالات. و`mutate` يكسر النسخة عمدًا لتقيس الأداة نفسها."""
    if mutate:
        old, new = target["mutation"]
        if old not in body:
            raise SystemExit(f"موضع الكسر المقصود غائب عن `{target['name']}`: {old!r}")
        body = body.replace(old, new)
    text = target["driver"].replace("__BODY__", body)
    for key, value in target["substitutions"].items():
        text = text.replace(key, value)
    return text


def run(target: dict, body: str, mutate: bool = False) -> subprocess.CompletedProcess:
    with tempfile.TemporaryDirectory(prefix="mm-maxctl-") as tmp:
        harness = Path(tmp) / "harness.js"
        harness.write_text(driver(target, body, mutate), encoding="utf-8")
        return subprocess.run(["node", str(harness)], capture_output=True, text=True, timeout=120)


def main() -> int:
    parser = argparse.ArgumentParser(description="قياس منطق دالّتَي maxctl الخالصتين")
    parser.add_argument("--assert", action="store_true", dest="assert_flag", help="يخرج بخطأ عند أي حالة فاشلة")
    parser.add_argument("--self-test", action="store_true", help="يقيس الأداة على نسخ مكسورة")
    args = parser.parse_args()

    source = FUNCTION.read_text(encoding="utf-8")
    failed = 0

    if args.self_test:
        # بنية الملفّ: تُقاس على الملفّ الحقيقي (يجب أن يمرّ) وعلى نسخ مكسورة عمدًا (يجب أن تُكشف).
        balance_cases = [
            ("الملفّ كما هو", source, False),
            ("قوس ساقط", source[: source.rindex("}")] + source[source.rindex("}") + 1 :], True),
            ("نصّ غير مغلق", source + '\nconst broken = "unterminated;\n', True),
            ("قالب غير مغلق", source + "\nconst broken = `x ${1};\n", True),
        ]
        print("── بنية الملفّ (توازن الأقواس والنصوص والتعليقات)")
        for name, text, expect_problem in balance_cases:
            problems = ts_balance(text)
            ok = bool(problems) == expect_problem
            detail = problems[0] if problems else "لا مشاكل"
            print(f"   {'✓' if ok else '✗'} {name} — {detail}")
            if not ok:
                failed += 1

        for target in TARGETS:
            body, _ = prepare(source, target)
            good = run(target, body)
            broken = run(target, body, mutate=True)
            ok = good.returncode == 0 and broken.returncode != 0
            print(f"── {target['name']} ({target['why']})")
            print(f"   النسخة الصحيحة: exit={good.returncode} (المتوقّع 0)")
            print(f"   النسخة المكسورة: exit={broken.returncode} (المتوقّع ≠0)")
            if not ok and broken.returncode == 0:
                print(broken.stdout, end="")
            print("   ✓ تفرّق بين الصحيح والمكسور" if ok else "   ✗ لا تفرّق")
            if not ok:
                failed += 1
        return 0 if failed == 0 else 1

    balance = ts_balance(source)
    print("── بنية ملفّ الدالّة — توازن الأقواس والنصوص والتعليقات (البنية لا الأنواع)")
    if balance:
        failed += 1
        print(f"   ✗ {len(balance)} خللًا في البنية:")
        for problem in balance[:10]:
            print(f"     · {problem}")
    else:
        print(f"   ✓ متوازن — {len(source.splitlines())} سطرًا · لا قوس ساقط ولا نصّ/قالب غير مغلق")

    for target in TARGETS:
        body, removed = prepare(source, target)
        result = run(target, body)
        print(f"── {target['name']} — {target['why']}")
        print(result.stdout, end="")
        if result.returncode != 0:
            print(result.stderr, end="")
            failed += 1
        print(
            f"   الملفّ المقيس: {os.path.relpath(FUNCTION, REPO)} — استخراج حرفيّ "
            f"({len(removed)} قطعة نوع أُزيلت: {' · '.join(removed)})"
        )
    return 0 if failed == 0 else (1 if args.assert_flag else 0)


if __name__ == "__main__":
    sys.exit(main())
