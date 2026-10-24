#!/usr/bin/env python3
"""Generates the README visual assets with computed geometry, then re-reads each
file to verify it against the claims made about it.

Design rules applied here (from the measured review of the previous assets):
  · every label must survive a phone width — target >= 11px at 390px display;
  · the static state must be complete and correct with SMIL stripped;
  · no internal code names on a product surface;
  · one palette across all assets;
  · SMIL only (GitHub strips scripts and CSS keyframes), keyTimes 0..1;
  · every panel size is computed from its content, never typed as a constant
    (the first gates.svg held a 5-row grid in a 4-row canvas: four tiles were
    never rendered anywhere, and only tools/svg_review.py caught it).

Regenerate with:  python3 tools/gen_readme_assets.py
Then measure what was generated:  python3 tools/svg_review.py --assert
"""
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "assets")

# ── one palette, used by every asset ──────────────────────────────────────────
# اللوحة صارت **عائلة** لا رمزين: `panelTop`/`panelBottom` طرفا تدرّج اللوح، و`raise` سطحٌ
# مرتفع داخل اللوح، و`shimmer` جار `accent` في التدريج (لا لون بعيد عنه — القريب وحده يُنتج
# وسيطًا نظيفًا). وسبب التدرّج **مقيس**: اللوح المسطّح بلون واحد يُقرأ مستطيلًا لا سطحًا، و`rect`
# بحافة واحدة يجعل كل الأصول تبدو مخطّطات تقنية لا واجهة منتج. و`linearGradient` ممّا **يحفظه**
# GitHub في الـSVG داخل الـREADME (بخلاف `filter` الثقيل)، فهو العمق الوحيد المتاح بلا ثمن.
# **والنغمات الثلاث (تنبيه/تحذير/خطر) ليست ألوانًا اخترعتها الصفحة:** هي حرفيًّا قيم
# `MaxTokens.kt` (`PositiveOnDark` · `CautionOnDark` · `CriticalOnDark` والفروع الفاتحة منها).
# أي أنّ لون «الحرارة مرتفعة» في الرسم هو لون «الحرارة مرتفعة» في التطبيق — وهذا ما تقيسه
# القاعدة ⑦ في `tools/design_doc.py`: تطابق اللوحة مع ثوابت الشيفرة، لا تشابهها بالعين.
DARK = dict(bg="#0E1418", panel="#151C22", edge="#243340", ink="#E8F1F7",
            tonePositive="#5FD9AC", toneCaution="#FFB86B", toneCritical="#FF9A90",
            muted="#93A9B8", accent="#5FD9AC", steel="#9BBACB", clay="#D99A6C",
            panelTop="#1B242C", panelBottom="#101820", lift="#151F27",
            shimmer="#A8F0D6", tile="#0C1116",
            # حدّ بلاطة الرمز — وُجد في الشجرة **رقمًا بلا اسم** (`stroke="#2E4150"` في `ICON_TILE`)
            # بينما التعليق فوقه يقول «والألوان من اللوحة نفسها». أي أنّ الملفّ كان يدّعي ما لم يصنعه،
            # وهو بعينه ما كشفته أداة `design_doc.py` أوّل تشغيل. والقيمة **محفوظة كما هي**: صفر
            # تغيير بصريّ، والاسم هو المُكتسَب. ولا نظير لها في `LIGHT` عن قصد: بلاطة الرمز
            # **داكنة دائمًا** (الرمز يُحمَّل في `<img>` فلا يرى الوضع الفاتح)، واللوحة الفاتحة
            # تخدم اللافتة وحدها — ونقص المفتاح مقصود لا منسيّ.
            tileEdge="#2E4150")
LIGHT = dict(bg="#FFFFFF", panel="#F4F7FA", edge="#D3DDE5", ink="#12212B",
             muted="#4C6373", accent="#0E9E74", steel="#3C5A72", clay="#B36A42",
             panelTop="#FFFFFF", panelBottom="#EDF2F6", lift="#F7FAFC",
             shimmer="#0B7A59", tile="#FFFFFF",
             tonePositive="#0B6B4F", toneCaution="#8A5200", toneCritical="#9A1B1B")

# سطح الـREADME له **تمييزه الخاصّ**: القارئ طلب ألّا يسيطر الأخضر على الصور. و`DARK`/`LIGHT` أعلاه
# **مربوطتان بالتطبيق** (نغمة «الإيجابي» الخضراء ثابتة في `MaxTokens.kt` وتقاس بها بوّابة المتجر والوسم)
# فلا تُمسّان؛ وهاتان اللوحتان تأخذانهما كما هما وتبدّلان **التمييز واللمعان فقط** إلى أزرق،
# فتبقى نغمات الحالة (إيجابي · تحذير · حرج) على حالها حيث تعني حالة، ويزول الأخضر حيث كان زينة.
# وكل رسم في `docs/assets/` يستهلك هاتين، وبوّابة `design_doc.py` تقيس ألوانه عليهما.
SURFACE_DARK = dict(DARK, accent="#5AA9FF", shimmer="#BBDCFF")
SURFACE_LIGHT = dict(LIGHT, accent="#1F6FEB", shimmer="#1558C0")

SANS = "-apple-system, 'Segoe UI', Inter, Roboto, Helvetica, Arial, sans-serif"
MONO = "'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace"

# phone-legibility budget: viewBox width is scaled to 390px on a phone
PHONE = 390.0
LEGIBLE = 11.0


def px_at_phone(size, vb_width):
    return size * PHONE / vb_width


def esc(s):
    return (s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))


def text(x, y, s, size, fill, anchor="start", weight=None, family=SANS, opacity=None, rtl=False):
    # عند `direction="rtl"` يُقلب معنى `start`/`end` في SVG (البداية = الطرف الأيمن). والمُستدعي
    # يفكّر بالجهة الفيزيائيّة (`end` = محاذاة لليمين) والمساعد يترجمها، وبوّابة `svg_review`
    # تقرأ `direction` فتقيس بالمعنى نفسه — فلا يُقاس نصّ عربيّ بمعنى اللاتينيّ.
    if rtl:
        anchor = {"start": "end", "end": "start"}.get(anchor, anchor)
    a = f' text-anchor="{anchor}"' if anchor != "start" else ""
    w = f' font-weight="{weight}"' if weight else ""
    o = f' opacity="{opacity}"' if opacity is not None else ""
    d = ' direction="rtl"' if rtl else ""
    return (f'  <text x="{x:.1f}" y="{y:.1f}" font-size="{size}" fill="{fill}"'
            f'{a}{w}{o}{d} font-family="{family}">{esc(s)}</text>')


def circle_arrow(x, y, angle_deg, fill, scale=1.0):
    """Small triangle pointing along the tangent (clockwise, y-down)."""
    return (f'  <path d="M{-5*scale:.1f},{-5*scale:.1f} L{7*scale:.1f},0 '
            f'L{-5*scale:.1f},{5*scale:.1f} Z" fill="{fill}" '
            f'transform="translate({x:.1f},{y:.1f}) rotate({angle_deg:.1f})"/>')


# ═══════════════════════════════════════════════════════════════════════════
# 0. لغة بصرية واحدة — تُستهلك من كل أصل، فلا يكون للأصل أسلوبه الخاص
# ═══════════════════════════════════════════════════════════════════════════
# **العطب الذي أُصلح هنا مقيس لا مُتخيَّل:** كانت الأصول تُرسم بلغتين متعارضتين — المولَّد منها
# (atlas-cycle، max-ai، control-plane، gates، locales) بلوح `#151C22` ونصف قطر ٢٢ وحدّ `#243340`،
# والمكتوب بيد (`domains.svg`، `integration.svg`) بلوح `#12202A` ونصف قطر ١٤ **وبلا حدّ على اللوح
# نفسه**. أي أنّ زائرًا يرى في الصفحة نفسها شكلين لـ«البطاقة» — وهذا بعينه ما يجعل الصفحة تبدو
# كأنها جُمعت من مصادر لا كأنها منتج واحد. والعلاج **ليس** إعادة رسم كل أصل بيد، بل أن يصير
# للشكل الواحد تعريف واحد يستهلكه الجميع — وهو ما يلي.

def defs_panel(uid, pal, top=None, bottom=None):
    """تدرّج اللوح — يُعرَّف مرّة لكل ملفّ (لا مرجع مشترك بين ملفّات، فهي صور مستقلّة)."""
    top = top or pal["panelTop"]
    bottom = bottom or pal["panelBottom"]
    return (f'  <defs>\n'
            f'    <linearGradient id="{uid}" x1="0" y1="0" x2="0" y2="1">\n'
            f'      <stop offset="0" stop-color="{top}"/>\n'
            f'      <stop offset="1" stop-color="{bottom}"/>\n'
            f'    </linearGradient>\n'
            f'  </defs>')


def defs_all(uid, pal, card=None):
    """تدرّجات الأصل: واحد للوح وواحد للبطاقة — يُعرَّفان مرّة واحدة في الملفّ الواحد.

    ولماذا تدرّج أصلًا للبطاقة أيضًا: البطاقة واللوح بلون **واحد** يُلغيان الفرق بينهما، فيقرأ
    القارئ مربّعًا داخل مربّع بدل طبقتين. والتدرّج يعيد الفرق بلا حدّ ثانٍ ولا ظلّ.
    """
    lines = ['  <defs>',
             f'    <linearGradient id="{uid}" x1="0" y1="0" x2="0" y2="1">',
             f'      <stop offset="0" stop-color="{pal["panelTop"]}"/>',
             f'      <stop offset="1" stop-color="{pal["panelBottom"]}"/>',
             '    </linearGradient>']
    if card:
        lines += [f'    <linearGradient id="{card}" x1="0" y1="0" x2="0" y2="1">',
                  f'      <stop offset="0" stop-color="{pal["lift"]}"/>',
                  f'      <stop offset="1" stop-color="{pal["bg"]}"/>',
                  '    </linearGradient>']
    lines.append('  </defs>')
    return "\n".join(lines)


def panel(uid, x, y, w, h, r, pal, stroke=None):
    """اللوح: تدرّج + حدّ شعريّ. كل لوح في كل أصل يمرّ من هنا."""
    return (f'  <rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{r}" '
            f'fill="url(#{uid})" stroke="{stroke or pal["edge"]}" stroke-width="1.5"/>')


def edge_light(x, y, w, pal, opacity=0.10):
    """خطّ ضوء على الحافة العليا — العمق كلّه من هذه الحافة.

    وحدّه معلَن: هذا **لا يُضيف معلومة**، ولذلك `opacity` منخفضة دائمًا ولا يُعتمد عليه في تمييز
    عنصر. والقراءة بلا حركة وبلا عمق تبقى صحيحة — والعمق طبقة فوقها لا تحلّ محلّها.
    """
    return (f'  <rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="1.4" rx="0.7" '
            f'fill="{pal["ink"]}" opacity="{opacity:.2f}"/>')


def rule(x, y, w, pal, accent=None, height=6):
    """الشريط التمييزيّ — وله نمط «املأ واثبت» من كتاب وصفات SVG المتحرّكة: يطول ثم يثبت ثم يعود.

    و**لماذا لا يطول ويعود بلا ثبات:** القيمة نفسها في طرفَي الرحلة تُنتج نبضة لا إشارة؛ والثبات
    في المنتصف هو ما يجعله يُقرأ إيقاعًا لا اهتزازًا. وهو من `patterns.md` §1 حرفيًّا (`values`
    بقيمتين متجاورتين متطابقتين = إطار ثابت).
    """
    tone = accent or pal["accent"]
    return "\n".join([
        f'  <rect x="{x:.1f}" y="{y:.1f}" width="{w:.0f}" height="{height}" '
        f'rx="{height/2:.1f}" fill="{tone}">',
        f'    <animate attributeName="width" values="{w:.0f};{w*2.4:.0f};{w*2.4:.0f};{w:.0f}" '
        f'keyTimes="0;0.16;0.84;1" dur="8s" repeatCount="indefinite"/>',
        f'    <animate attributeName="fill" values="{tone};{pal["shimmer"]};{tone}" '
        f'keyTimes="0;0.5;1" dur="8s" repeatCount="indefinite"/>',
        '  </rect>',
    ])


def header(x, y, title, subtitle, pal, rule_w=90):
    """رأس كل أصل: شريط مميّز، ثم العنوان، ثم سطر توضيحيّ — بترتيب واحد في كل ملفّ."""
    return "\n".join([
        rule(x, y, rule_w, pal),
        text(x, y + 52, title, 34, pal["ink"], weight="800"),
        text(x, y + 88, subtitle, 21, pal["muted"]),
    ])


def chip(x, y, label, size, tone):
    """وسم دائريّ: خلفية مصبوغة + حدّ خفيف + نصّ. يُقاس عرضه من طول نصّه لا يُكتب رقمًا."""
    w = 0.56 * size * len(label) + 28
    h = size + 18
    return "\n".join([
        f'  <rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" '
        f'rx="{h/2:.1f}" fill="{tone}" opacity="0.10"/>',
        f'  <rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" '
        f'rx="{h/2:.1f}" fill="none" stroke="{tone}" stroke-width="1.2" opacity="0.40"/>',
        text(x + w / 2, y + h * 0.70, label, size, tone, anchor="middle", weight="600"),
    ])


def breathe(cx, cy, r, tone, lo=0.06, hi=0.13, dur=9.0):
    """هالة تتنفّس — حركة `opacity` وحدها: لا موضع يتزحزح، فالأصل بلا SMIL يبقى صحيحًا تمامًا."""
    return "\n".join([
        f'  <circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r:.1f}" fill="{tone}" opacity="{lo:.2f}">',
        f'    <animate attributeName="opacity" values="{lo:.2f};{hi:.2f};{lo:.2f}" '
        f'keyTimes="0;0.5;1" dur="{dur:.1f}s" repeatCount="indefinite"/>',
        '  </circle>',
    ])


def tick(cx, cy, r, pal, dur=6.0):
    """نقطة «الحيّ» — نمط `heartbeat` من كتاب وصفات SVG المتحرّكة: صمت طويل، ثم نبضة قصيرة.

    وسمة الأساس `opacity=0` مقصودة: الأصل بلا حركة يُظهر نقطة ثابتة في موضعها لا فراغًا، وهي
    هنا **زينة لا معلومة** (المعلومة في النصّ المجاور لها) فلا تُفقد شيئًا إن غابت الحركة.
    """
    return "\n".join([
        f'  <circle cx="{cx:.1f}" cy="{cy:.1f}" r="{r}" fill="{pal["accent"]}" opacity="0">',
        f'    <animate attributeName="opacity" values="0;0;1;0.55;0" '
        f'keyTimes="0;0.90;0.94;0.97;1" dur="{dur:.1f}s" repeatCount="indefinite"/>',
        '  </circle>',
    ])


def draw_in(d, stroke, width, length, dur=1.6, begin=0.0):
    """خطّ يُرسم — سمة الأساس تُظهره كاملًا، فمن لا يدعم SMIL يرى الرسم كاملًا لا فراغًا.

    و**لا `fill="freeze"` هنا:** الحركة تُعاد كلّ دورة، فالرسم يعود ويُرسم — وهو إيقاع التحقّق
    نفسه (يُقرأ ثم يُثبت). ولو انتهت مرّة واحدة لبقي الأصل بلا معنى بعد الثانية الأولى.
    """
    return (f'  <path d="{d}" fill="none" stroke="{stroke}" stroke-width="{width}" '
            f'stroke-linecap="round" stroke-linejoin="round" '
            f'stroke-dasharray="{length:.0f}" stroke-dashoffset="0">\n'
            f'    <animate attributeName="stroke-dashoffset" '
            f'values="{length:.0f};0;0;{length:.0f}" keyTimes="0;0.14;0.86;1" '
            f'dur="{dur + 8:.1f}s" begin="{begin:.1f}s" repeatCount="indefinite"/>\n'
            f'  </path>')


# ═══════════════════════════════════════════════════════════════════════════
# 1. atlas-cycle.svg — the Max Atlas loop, redrawn from scratch
# ═══════════════════════════════════════════════════════════════════════════
def atlas_cycle(pal, dark=True):
    W, H = 720, 690
    cx, cy, R = 360.0, 350.0, 196.0
    stages = ["Discover", "Understand", "Map", "Adapt", "Execute", "Verify", "Learn"]
    n = len(stages)
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="at ac">')
    out.append('  <title id="at">Max Atlas — the loop that learns how this device works</title>')
    out.append('  <desc id="ac">Seven stages on a ring, clockwise from the top: Discover, Understand, '
               'Map, Adapt, Execute, Verify, Learn. The centre states the promise: what works on this '
               'device is proven here, not assumed. Nothing is claimed until it was read back.</desc>')
    out.append(defs_all("at", pal, card="at-card"))
    out.append(panel("at", 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(614, 74, 152, pal["accent"], 0.04, 0.09, 10.0))

    # header
    out.append(text(36, 62, "Max Atlas", 38, pal["ink"], weight="700"))
    out.append(text(36, 96, "It learns how your device works before changing anything.",
                    22, pal["muted"]))

    # the track
    out.append(f'  <circle cx="{cx:.1f}" cy="{cy:.1f}" r="{R:.1f}" fill="none" '
               f'stroke="{pal["edge"]}" stroke-width="2"/>')

    # direction arrowheads at the midpoint of every gap
    for i in range(n):
        th = math.radians(-90 + (i + 0.5) * 360 / n)
        ax = cx + R * math.cos(th)
        ay = cy + R * math.sin(th)
        out.append(circle_arrow(ax, ay, math.degrees(th) + 90, pal["steel"], 0.95))

    # stations + labels
    for i, name in enumerate(stages):
        th = math.radians(-90 + i * 360 / n)
        c, s = math.cos(th), math.sin(th)
        dx, dy = cx + R * c, cy + R * s
        out.append(f'  <circle cx="{dx:.1f}" cy="{dy:.1f}" r="11" fill="{pal["accent"]}">')
        # safe SMIL: only opacity breathes, so the static state is already correct
        out.append(f'    <animate attributeName="opacity" values="1;0.55;1" keyTimes="0;0.5;1" '
                   f'dur="6s" begin="{i*0.6:.1f}s" repeatCount="indefinite"/>')
        out.append('  </circle>')
        out.append(f'  <circle cx="{dx:.1f}" cy="{dy:.1f}" r="4.5" fill="{pal["panel"]}"/>')
        lx, ly = cx + (R + 38) * c, cy + (R + 38) * s
        anchor = "start" if c > 0.28 else ("end" if c < -0.28 else "middle")
        out.append(text(lx, ly + 7, name, 22, pal["ink"], anchor=anchor, weight="600"))

    # centre card — the promise, in product words
    out.append(f'  <rect x="{cx-160:.1f}" y="{cy-72:.1f}" width="320" height="144" rx="18" '
               f'fill="url(#at-card)" stroke="{pal["edge"]}"/>')
    out.append(text(cx, cy - 26, "What works here", 26, pal["ink"], anchor="middle", weight="700"))
    out.append(text(cx, cy + 8, "is proven here.", 26, pal["accent"], anchor="middle", weight="700"))
    out.append(text(cx, cy + 46, "unproven stays unknown", 21, pal["muted"], anchor="middle"))

    # footer
    out.append(f'  <line x1="36" y1="{H-96}" x2="{W-36}" y2="{H-96}" stroke="{pal["edge"]}"/>')
    out.append(text(36, H - 62, "Atlas finds the path — and remembers it.", 21, pal["steel"]))
    out.append(text(36, H - 32, "Max AI decides when to act. Neither crosses into the other.",
                    21, pal["muted"]))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 2. max-ai.svg — what Max AI does, as the user experiences it
# ═══════════════════════════════════════════════════════════════════════════
def max_ai(pal):
    W, H = 720, 660
    steps = [
        ("Notice", "reads", "the device"),
        ("Decide", "one", "change"),
        ("Ask", "safety", "first"),
        ("Verify", "read", "back"),
        ("Remember", "trust", "earned"),
    ]
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="mt md">')
    out.append('  <title id="mt">Max AI — one measured change at a time</title>')
    out.append('  <desc id="md">Five steps in order: Notice, Decide, Ask, Verify, Remember. '
               'Max AI changes one thing at a time, only when the readings say so, and it keeps '
               'a record of what the device actually did afterwards.</desc>')
    out.append(defs_all("ma", pal, card="ma-card"))
    out.append(panel("ma", 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(614, 74, 152, pal["accent"], 0.04, 0.09, 10.0))

    out.append(text(36, 62, "Max AI", 38, pal["ink"], weight="700"))
    out.append(text(36, 96, "One measured change at a time — never a stack of tweaks.", 22, pal["muted"]))

    # five cards
    n = len(steps)
    margin, gap = 30.0, 16.0
    cw = (W - 2 * margin - (n - 1) * gap) / n
    ch, cy0 = 176.0, 132.0
    for i, (label, cap1, cap2) in enumerate(steps):
        x = margin + i * (cw + gap)
        out.append(f'  <rect x="{x:.1f}" y="{cy0:.1f}" width="{cw:.1f}" height="{ch:.0f}" rx="16" '
                   f'fill="url(#ma-card)" stroke="{pal["edge"]}">')
        # safe SMIL: the loop runs once per cycle, and stops dead when SMIL is absent
        out.append(f'    <animate attributeName="opacity" values="1;0.62;1" keyTimes="0;0.5;1" '
                   f'dur="7s" begin="{i*0.8:.1f}s" repeatCount="indefinite"/>')
        out.append('  </rect>')
        out.append(f'  <circle cx="{x + cw/2:.1f}" cy="{cy0 + 40:.1f}" r="17" fill="{pal["accent"]}"/>')
        out.append(text(x + cw / 2, cy0 + 48, str(i + 1), 22, pal["bg"],
                        anchor="middle", weight="700"))
        out.append(text(x + cw / 2, cy0 + 96, label, 26, pal["ink"], anchor="middle", weight="600"))
        out.append(text(x + cw / 2, cy0 + 128, cap1, 21, pal["muted"], anchor="middle"))
        out.append(text(x + cw / 2, cy0 + 156, cap2, 21, pal["muted"], anchor="middle"))
        if i < n - 1:
            ax = x + cw + gap / 2
            out.append(f'  <path d="M{ax-6:.1f},{cy0 + 40:.1f} L{ax+5:.1f},{cy0 + 40:.1f} '
                       f'L{ax:.1f},{cy0 + 34:.1f} Z" fill="{pal["steel"]}"/>')

    # the difference from a preset
    y0 = cy0 + ch + 44
    out.append(text(36, y0, "Why it is not a preset", 26, pal["ink"], weight="700"))
    rows = [
        ("A preset applies the same numbers to every device.", pal["muted"]),
        ("Max AI asks what this device is doing now, changes one thing,", pal["ink"]),
        ("then measures whether it actually helped.", pal["accent"]),
    ]
    for i, (line, col) in enumerate(rows):
        out.append(text(36, y0 + 38 + i * 32, line, 21, col))

    # the promise strip
    ys = y0 + 38 + 3 * 32 + 24
    out.append(f'  <rect x="30" y="{ys:.1f}" width="{W-60:.0f}" height="112" rx="14" '
               f'fill="url(#ma-card)" stroke="{pal["edge"]}"/>')
    out.append(text(52, ys + 38, "It will never", 21, pal["clay"], weight="700"))
    out.append(text(52, ys + 72, "write a protected interface · invent a reading",
                    21, pal["muted"]))
    out.append(text(52, ys + 102, "claim a change it did not measure",
                    21, pal["muted"]))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 3. banners — the hero (v2): product emblem in orbit, product words only
# ═══════════════════════════════════════════════════════════════════════════
def _measured():
    """الأرقام المقيسة من الشجرة — فلا يُكتب رقم بيد في أصل. وإن غابت الأداة (شجرة مصغّرة في\n    فحص ذاتيّ) فالقيم الاحتياطيّة هي المقيسة يوم الكتابة، وبوّابة ⑪ تكشف تقادمها في اللافتة."""
    try:
        import sys as _sys
        _sys.path.insert(0, os.path.join(ROOT, "tools"))
        import readme_assets as _ra
        return _ra.measure_tree()
    except Exception:  # noqa: BLE001 — القياس تحسين لا شرط
        return {"screens": 65, "locales": 84}


def _fit(s, size, maxw, mono=False):
    """يرفض نصًّا يتجاوز عرضه بتقدير البوّابة نفسه (`svg_review.span`) — فيُكتشف العطب عند التوليد."""
    letters = [c for c in s if c.isalpha()]
    caps = sum(1 for c in letters if c.isupper()) / max(1, len(letters))
    adv = 0.6 if mono else (0.62 if caps > 0.7 else 0.52)
    w = adv * size * len(s)
    assert w <= maxw, f"نصّ أعرض من مساحته: {s!r} = {w:.0f} > {maxw:.0f}"
    return s


def _shield(cx, cy, s):
    """درع المنتج — هندسته هي هندسة `ic-shield` مكبَّرة، حول مركز."""
    def P(dx, dy):
        return f"{cx + dx * s:.1f} {cy + dy * s:.1f}"
    return (f"M{P(0, -34)} L{P(30, -23)} L{P(30, -1)} C{P(30, 15)} {P(18, 27)} {P(0, 33)} "
            f"C{P(-18, 27)} {P(-30, 15)} {P(-30, -1)} L{P(-30, -23)} Z")


def _sat(cx, cy, r, deg, size, fill, op=1.0):
    a = math.radians(deg)
    return (f'    <circle cx="{cx + r * math.cos(a):.1f}" cy="{cy + r * math.sin(a):.1f}" '
            f'r="{size}" fill="{fill}" opacity="{op}"/>')


def _spin(cx, cy, dur, reverse=False):
    a, b = ("360", "0") if reverse else ("0", "360")
    return (f'    <animateTransform attributeName="transform" type="rotate" '
            f'values="{a} {cx} {cy};{b} {cx} {cy}" keyTimes="0;1" dur="{dur}s" '
            f'repeatCount="indefinite"/>')


def _twinkle(cx, cy, r, tone, begin, dur=5.0, lo=0.18, hi=0.9):
    return "\n".join([
        f'  <circle cx="{cx}" cy="{cy}" r="{r}" fill="{tone}" opacity="{lo}">',
        f'    <animate attributeName="opacity" values="{lo};{hi};{lo}" keyTimes="0;0.5;1" '
        f'dur="{dur}s" begin="{begin}s" repeatCount="indefinite"/>',
        '  </circle>',
    ])


def banner(pal, dark=True):
    """اللافتة (الإصدار ٢) — اسم المنتج ورسالته على اليسار، وعلامته **في مدار** على اليمين.

    الطبقات: لوح بعمق · هالتان تتنفّسان · **مدار** يدور (حلقة منقّطة بثلاثة توابع، وحلقة داخليّة\n    تدور عكسها) · الدرع والتأشير (نفس مفردات `ic-shield`/`ic-ask`) · لمعان يعبر اللوح مرّة كل\n    دورة · نجوم تومض بإزاحات مختلفة. والنصّ يبقى البطل: يدخل بالكشف ثم بالتلاشي، ولا شيء يمرّ\n    فوقه إلا لمعان بـ١٠٪.\n\n    **والقاعدة الصلبة:** كلّ حركة هنا زينة فوق قراءة صحيحة — بلا SMIL يبقى الاسم والرسالة والوسوم\n    كاملةً في مواضعها، والدرع والتأشير مرسومَين. والأرقام الظاهرة (شاشات) من `measure_tree` لا من اليد.\n    """
    W, H = 960, 360
    uid = "bn"
    cx, cy = 832, 182
    meas = _measured()
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="bt bd">')
    out.append('  <title id="bt">MaxManager — performance control that asks before it acts</title>')
    out.append('  <desc id="bd">MaxManager, adaptive chip-aware performance control for rooted '
               'Android, with Max AI and Max Atlas, in 84 languages, and with no telemetry. '
               'A shield with a tick turns in a slow orbit beside the name.</desc>')
    out.append('  <defs>')
    out.append(f'    <linearGradient id="{uid}" x1="0" y1="0" x2="0" y2="1">')
    out.append(f'      <stop offset="0" stop-color="{pal["panelTop"]}"/>')
    out.append(f'      <stop offset="1" stop-color="{pal["panelBottom"]}"/>')
    out.append('    </linearGradient>')
    out.append(f'    <linearGradient id="{uid}-glare" x1="0" y1="0" x2="1" y2="0">')
    out.append(f'      <stop offset="0" stop-color="{pal["shimmer"]}" stop-opacity="0"/>')
    out.append(f'      <stop offset="0.5" stop-color="{pal["shimmer"]}" stop-opacity="0.11"/>')
    out.append(f'      <stop offset="1" stop-color="{pal["shimmer"]}" stop-opacity="0"/>')
    out.append('    </linearGradient>')
    out.append('    <clipPath id="bn-wipe">')
    out.append('      <rect x="40" y="44" width="520" height="96">')
    out.append('        <animate attributeName="width" values="0;520" keyTimes="0;1" '
               'dur="1.3s" repeatCount="1" fill="freeze"/>')
    out.append('      </rect>')
    out.append('    </clipPath>')
    out.append('  </defs>')
    out.append(panel(uid, 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(cx, cy, 150, pal["accent"], 0.06, 0.14, 9.0))
    if dark:  # على الفاتح يصير القرص الفولاذيّ بقعة رماديّة لا عمقًا
        out.append(breathe(150, 330, 170, pal["steel"], 0.04, 0.09, 12.0))

    # ── المدار ──────────────────────────────────────────────────────────
    out.append(f'  <circle cx="{cx}" cy="{cy}" r="82" fill="none" stroke="{pal["edge"]}" '
               f'stroke-width="1.6"/>')
    out.append(f'  <circle cx="{cx}" cy="{cy}" r="60" fill="{pal["accent"]}" opacity="0.08">')
    out.append('    <animate attributeName="opacity" values="0.06;0.15;0.06" '
               'keyTimes="0;0.5;1" dur="7s" repeatCount="indefinite"/>')
    out.append('  </circle>')
    out.append('  <g>')
    out.append(_spin(cx, cy, 48))
    out.append(f'    <circle cx="{cx}" cy="{cy}" r="116" fill="none" stroke="{pal["steel"]}" '
               f'stroke-width="1.6" stroke-dasharray="2 9" stroke-linecap="round" opacity="0.55"/>')
    out.append(_sat(cx, cy, 116, 205, 7, pal["accent"]))
    out.append(_sat(cx, cy, 116, 325, 5.5, pal["steel"]))
    out.append(_sat(cx, cy, 116, 95, 5, pal["clay"]))
    out.append('  </g>')
    out.append('  <g>')
    out.append(_spin(cx, cy, 34, reverse=True))
    out.append(f'    <circle cx="{cx}" cy="{cy}" r="94" fill="none" stroke="{pal["accent"]}" '
               f'stroke-width="1.8" stroke-dasharray="16 22" stroke-linecap="round" opacity="0.5"/>')
    out.append('  </g>')
    out.append(f'  <path d="{_shield(cx, cy, 1.15)}" fill="{pal["panelBottom"]}" '
               f'fill-opacity="0.55" stroke="{pal["steel"]}" stroke-width="3" '
               f'stroke-linejoin="round" opacity="0.95"/>')
    tk = (f"M{cx - 16:.1f} {cy + 2:.1f} L{cx - 4:.1f} {cy + 14:.1f} L{cx + 19:.1f} {cy - 12:.1f}")
    out.append(draw_in(tk, pal["accent"], 5.2, 62))
    for (x, y, r, tone, b, d) in [(716, 64, 2.6, pal["accent"], 0.0, 5.0),
                                  (926, 78, 2.2, pal["steel"], 1.4, 6.0),
                                  (930, 296, 2.8, pal["accent"], 2.2, 5.5),
                                  (712, 304, 2.2, pal["clay"], 0.8, 6.5),
                                  (790, 36, 2.0, pal["steel"], 3.0, 5.0),
                                  (880, 328, 2.0, pal["accent"], 1.9, 6.0),
                                  (698, 186, 2.4, pal["steel"], 2.6, 5.5)]:
        out.append(_twinkle(x, y, r, tone, b, d))

    # ── اللمعان العابر (يمرّ مرّة كل دورة، وقاعدته داخل اللوح) ─────────
    # القاعدة شفّافة (`opacity="0"`): خارج لحظة العبور لا أثر للمعان، وبلا SMIL لا شريط ثابت على الحافة.
    out.append(f'  <rect x="12" y="18" width="120" height="324" fill="url(#{uid}-glare)" opacity="0">')
    out.append('    <animateTransform attributeName="transform" type="translate" '
               'values="0 0;0 0;820 0;820 0" keyTimes="0;0.55;0.8;1" dur="11s" '
               'repeatCount="indefinite"/>')
    out.append('    <animate attributeName="opacity" values="0;0;1;1;0;0" '
               'keyTimes="0;0.55;0.58;0.77;0.8;1" dur="11s" repeatCount="indefinite"/>')
    out.append('  </rect>')

    # ── النصّ ───────────────────────────────────────────────────────────
    out.append('  <g clip-path="url(#bn-wipe)">')
    out.append(text(40, 112, "MaxManager", 64, pal["ink"], weight="800"))
    out.append('  </g>')
    out.append(rule(40, 136, 96, pal, height=6))
    for y, line, b, ks in [(196, "Performance control that", "0.5", "0;0.4;1"),
                           (240, "asks before it acts.", "0.8", "0;0.4;1")]:
        out.append('  <g>')
        out.append(f'    <animate attributeName="opacity" values="0;0;1" keyTimes="{ks}" '
                   f'dur="1.4s" begin="0s" fill="freeze"/>')
        out.append("  " + text(40, y, _fit(line, 36, 560), 36, pal["steel"], weight="600"))
        out.append('  </g>')

    # ── الوسوم: أرقام مقيسة، ولا قياس عن بُعد ───────────────────────────
    chips = [(f'{meas["screens"]} screens', pal["accent"]), ("84 languages", pal["steel"]),
             ("No telemetry", pal["clay"])]
    x = 40.0
    for label, tone in chips:
        out.append(chip(x, 268, label, 28, tone))
        x += 0.56 * 28 * len(label) + 28 + 14
    assert x - 14 < 700, "الوسوم تدخل منطقة المدار"
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 3b. stats strip — أربعة أرقام، ولكل رقم رمز يتحرّك (EN / AR)
# ═══════════════════════════════════════════════════════════════════════════
def stats_strip(pal, lang="en"):
    """شريط الأرقام: ٦٥ شاشة · ٨٤ لغة · عشرة مجالات · صفر قياس عن بُعد.

    الأرقام **مقيسة** (`measure_tree`) حيث للشجرة قياس، والباقي ادّعاء صفحة معلَن (عشرة مجالات = المجالات\n    في `domains.svg`، والصفر = «بلا قياس عن بُعد»). والنصّ كلّه **مُوسَّط** — فنسخة العربيّة نفس الهندسة\n    بلا مرآة، والاتجاه يُعلَن على النصّ (`direction="rtl"`).
    """
    ar = lang == "ar"
    meas = _measured()
    W, H = 720, 224
    uid = "st"
    labels = ({"screens": "شاشة", "langs": "لغة", "domains": "مجالات ضبط", "telemetry": "قياس عن بُعد"}
              if ar else
              {"screens": "screens", "langs": "languages", "domains": "domains",
               "telemetry": "telemetry"})
    nums = [str(meas["screens"]), str(meas["locales"]), "10", "0"]
    keys = ["screens", "langs", "domains", "telemetry"]
    tw, gap, th, top = 162.0, 14.0, 188.0, 18.0
    x0 = (W - (4 * tw + 3 * gap)) / 2
    xs = [x0 + i * (tw + gap) for i in range(4)]
    if ar:
        xs = xs[::-1]
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" '
               f'height="{H}" role="img" aria-labelledby="{uid}t {uid}d">')
    title = ("MaxManager بالأرقام" if ar else "MaxManager in four numbers")
    desc = (f'{meas["screens"]} screens, {meas["locales"]} languages, ten control domains and '
            'zero telemetry — measured from the source tree where the tree can measure.')
    out.append(f'  <title id="{uid}t">{title}</title>')
    out.append(f'  <desc id="{uid}d">{desc}</desc>')
    out.append(defs_all(uid, pal, card=f"{uid}-card"))
    out.append(panel(uid, 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(W / 2, 30, 190, pal["accent"], 0.03, 0.08, 10.0))
    for i, (x, key, num) in enumerate(zip(xs, keys, nums)):
        # الترتيب المنطقيّ: شاشات · لغات · مجالات · قياس — والعربيّة تقرأ من اليمين
        k = keys.index(key)
        cx = x + tw / 2
        out.append(f'  <rect x="{x:.1f}" y="{top:.0f}" width="{tw:.0f}" height="{th:.0f}" rx="18" '
                   f'fill="url(#{uid}-card)" stroke="{pal["edge"]}" stroke-width="1.4"/>')
        out.extend(_stat_glyph(k, cx, top + 44, pal, i))
        tone = pal["accent"] if k == 3 else pal["ink"]
        out.append(text(cx, top + 118, num, 54, tone, anchor="middle", weight="800"))
        lab = _fit(labels[key], 22, tw - 14)
        out.append(text(cx, top + 164, lab, 22, pal["muted"], anchor="middle", rtl=ar))
    out.append('</svg>')
    return "\n".join(out) + "\n"


def _stat_glyph(k, cx, cy, pal, i):
    steel, acc, clay = pal["steel"], pal["accent"], pal["clay"]
    g = []
    if k == 0:  # ثلاث شاشات: الوسطى مضاءة، والجانبان يتبادلان
        for dx, w, h, dy, b in [(-34, 16, 28, 5, 0.0), (-11, 22, 38, 0, 0.6), (18, 16, 28, 5, 1.2)]:
            g.append(f'  <rect x="{cx + dx:.1f}" y="{cy - 19 + dy:.1f}" width="{w}" height="{h}" '
                     f'rx="4" fill="none" stroke="{steel}" stroke-width="2"/>')
            g.append(f'  <rect x="{cx + dx + 3:.1f}" y="{cy - 16 + dy:.1f}" width="{w - 6}" '
                     f'height="{h - 12}" rx="2" fill="{acc}" opacity="0.30">')
            g.append(f'    <animate attributeName="opacity" values="0.25;0.85;0.25" '
                     f'keyTimes="0;0.5;1" dur="4s" begin="{b}s" repeatCount="indefinite"/>')
            g.append('  </rect>')
    elif k == 1:  # كرة بخطَّي طول يتّسع ويضيق
        g.append(f'  <circle cx="{cx}" cy="{cy}" r="22" fill="none" stroke="{steel}" stroke-width="2"/>')
        g.append(f'  <ellipse cx="{cx}" cy="{cy}" rx="9" ry="22" fill="none" stroke="{acc}" stroke-width="2">')
        g.append('    <animate attributeName="rx" values="9;20;9" keyTimes="0;0.5;1" dur="6s" '
                 'repeatCount="indefinite"/>')
        g.append('  </ellipse>')
        g.append(f'  <path d="M{cx - 22} {cy} H{cx + 22}" fill="none" stroke="{steel}" '
                 f'stroke-width="1.6" stroke-linecap="round"/>')
    elif k == 2:  # عشر نقاط تُضاء بالتتابع
        n = 0
        for r in range(2):
            for c in range(5):
                g.append(f'  <circle cx="{cx - 24 + c * 12:.1f}" cy="{cy - 6 + r * 14:.1f}" r="4.4" '
                         f'fill="{acc}" opacity="0.4">')
                g.append(f'    <animate attributeName="opacity" values="0.35;1;0.35" '
                         f'keyTimes="0;0.5;1" dur="3.6s" begin="{n * 0.22:.2f}s" '
                         f'repeatCount="indefinite"/>')
                g.append('  </circle>')
                n += 1
    else:  # بثّ مشطوب: حلقات متّحدة المركز وخطّ يُرسم فوقها
        for r, op in [(8, 0.85), (16, 0.6), (24, 0.38)]:
            g.append(f'  <circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{steel}" '
                     f'stroke-width="2" opacity="{op}"/>')
        g.append(f'  <circle cx="{cx}" cy="{cy}" r="3.4" fill="{steel}"/>')
        g.append(f'  <path d="M{cx - 25} {cy + 25} L{cx + 25} {cy - 25}" fill="none" stroke="{clay}" '
                 f'stroke-width="3.4" stroke-linecap="round" stroke-dasharray="71" '
                 f'stroke-dashoffset="0">')
        g.append('    <animate attributeName="stroke-dashoffset" values="71;0;0;71" '
                 'keyTimes="0;0.2;0.85;1" dur="7s" repeatCount="indefinite"/>')
        g.append('  </path>')
    return g


# ═══════════════════════════════════════════════════════════════════════════
# 3c. pillars — أربعة مبادئ بطاقات (EN / AR)
# ═══════════════════════════════════════════════════════════════════════════
PILLARS = {
    "en": [
        ("Absent, not disabled",
         ["A control your kernel", "does not expose is not", "on the screen at all."]),
        ("Verified, not assumed",
         ["Every write is read back.", "A change that did not hold", "is reported as a failure."]),
        ("Honest about unknowns",
         ["When it does not know,", "it says status_unknown —", "never a plausible zero."]),
        ("Nothing runs unseen",
         ["Engines stay off until you", "enable them. Every write", "uses one audited path."]),
    ],
    "ar": [
        ("غائب لا معطَّل",
         ["الضابط الذي لا تكشفه", "نواتك لا يظهر على", "الشاشة أصلًا."]),
        ("مُثبَّت لا مُفترَض",
         ["كل كتابة تُقرأ بعد حفظها،", "والتغيير الذي لم يستقرّ", "يُبلَّغ عنه كفشل."]),
        ("صريح حين لا يعلم",
         ["حين لا يعلم يقول", "status_unknown", "لا صفرًا معقولًا قط."]),
        ("لا شيء خلف ظهرك",
         ["المحرّكات مطفأة حتى", "تشغّلها، وكل كتابة تمرّ", "بمسار واحد مُدقَّق."]),
    ],
}


def pillars(pal, lang="en"):
    ar = lang == "ar"
    W = 720
    cw, ch, gap, x0, y0, pad = 342.0, 222.0, 12.0, 12.0, 14.0, 26.0
    H = int(y0 + 2 * ch + gap + 14)
    uid = "pl"
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" '
               f'height="{H}" role="img" aria-labelledby="{uid}t {uid}d">')
    out.append(f'  <title id="{uid}t">' + ("أربعة مبادئ" if ar else "Four rules the app keeps") + '</title>')
    out.append(f'  <desc id="{uid}d">' + (
        "غائب لا معطَّل؛ مُثبَّت لا مُفترَض؛ صريح حين لا يعلم؛ لا شيء خلف ظهرك."
        if ar else
        "Absent, not disabled; verified, not assumed; honest about unknowns; "
        "nothing runs unseen.") + '</desc>')
    out.append(defs_all(uid, pal, card=f"{uid}-card"))
    out.append(panel(uid, 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(W - 90, 40, 170, pal["accent"], 0.03, 0.08, 11.0))
    cards = PILLARS[lang]
    for i, (title, lines) in enumerate(cards):
        r, c = divmod(i, 2)
        if ar:
            c = 1 - c
        x = x0 + c * (cw + gap)
        y = y0 + r * (ch + gap)
        out.append(f'  <rect x="{x:.1f}" y="{y:.1f}" width="{cw:.0f}" height="{ch:.0f}" rx="18" '
                   f'fill="url(#{uid}-card)" stroke="{pal["edge"]}" stroke-width="1.4"/>')
        gx = (x + cw - pad - 24) if ar else (x + pad + 24)
        out.extend(_pillar_glyph(i, gx, y + 48, pal))
        tx = (x + cw - pad) if ar else (x + pad)
        anchor = "end" if ar else "start"
        out.append(text(tx, y + 116, _fit(title, 25, cw - 2 * pad), 25, pal["ink"],
                        anchor=anchor, weight="700", rtl=ar))
        for j, line in enumerate(lines):
            out.append(text(tx, y + 150 + j * 27, _fit(line, 21, cw - 2 * pad), 21, pal["muted"],
                            anchor=anchor, rtl=ar))
    out.append('</svg>')
    return "\n".join(out) + "\n"


def _pillar_glyph(i, gx, gy, pal):
    steel, acc = pal["steel"], pal["accent"]
    g = []
    if i == 0:  # مفتاح يتلاشى: غائب لا معطَّل
        g.append('  <g>')
        g.append('    <animate attributeName="opacity" values="1;0.22;1" keyTimes="0;0.5;1" '
                 'dur="5s" repeatCount="indefinite"/>')
        g.append(f'    <rect x="{gx - 24:.1f}" y="{gy - 13:.1f}" width="48" height="26" rx="13" '
                 f'fill="none" stroke="{steel}" stroke-width="2.2" stroke-dasharray="5 4"/>')
        g.append(f'    <circle cx="{gx - 11:.1f}" cy="{gy:.1f}" r="7.5" fill="{steel}" opacity="0.7"/>')
        g.append('  </g>')
    elif i == 1:  # تأشير يُرسم داخل حلقة: مقروء بعد الكتابة
        g.append(f'  <circle cx="{gx:.1f}" cy="{gy:.1f}" r="22" fill="none" stroke="{steel}" '
                 f'stroke-width="2.2"/>')
        g.append(draw_in(f"M{gx - 10:.1f} {gy:.1f} L{gx - 3:.1f} {gy + 8:.1f} L{gx + 11:.1f} {gy - 9:.1f}",
                         acc, 4, 38))
    elif i == 2:  # علامة استفهام داخل وسم: مجهول لا صفر
        g.append(f'  <rect x="{gx - 24:.1f}" y="{gy - 18:.1f}" width="48" height="36" rx="18" '
                 f'fill="{acc}" opacity="0.10">')
        g.append('    <animate attributeName="opacity" values="0.07;0.2;0.07" keyTimes="0;0.5;1" '
                 'dur="4.5s" repeatCount="indefinite"/>')
        g.append('  </rect>')
        g.append(f'  <rect x="{gx - 24:.1f}" y="{gy - 18:.1f}" width="48" height="36" rx="18" '
                 f'fill="none" stroke="{acc}" stroke-width="1.8" opacity="0.7"/>')
        g.append(text(gx, gy + 9, "?", 26, acc, anchor="middle", weight="800"))
    else:  # قفل: مسار واحد مُدقَّق
        g.append(f'  <rect x="{gx - 15:.1f}" y="{gy - 3:.1f}" width="30" height="24" rx="6" '
                 f'fill="{acc}" fill-opacity="0.12" stroke="{steel}" stroke-width="2.2"/>')
        g.append(f'  <path d="M{gx - 8:.1f} {gy - 3:.1f} V{gy - 10:.1f} Q{gx - 8:.1f} {gy - 21:.1f} '
                 f'{gx:.1f} {gy - 21:.1f} Q{gx + 8:.1f} {gy - 21:.1f} {gx + 8:.1f} {gy - 10:.1f} '
                 f'V{gy - 3:.1f}" fill="none" stroke="{steel}" stroke-width="2.2" '
                 f'stroke-linecap="round"/>')
        g.append(f'  <circle cx="{gx:.1f}" cy="{gy + 9:.1f}" r="3.2" fill="{acc}">')
        g.append('    <animate attributeName="opacity" values="1;0.35;1" keyTimes="0;0.5;1" '
                 'dur="3.5s" repeatCount="indefinite"/>')
        g.append('  </circle>')
    return g


# ═══════════════════════════════════════════════════════════════════════════
# 3d. install-flow — ثلاث خطوات ونقطة تعبرها (EN / AR)
# ═══════════════════════════════════════════════════════════════════════════
INSTALL = {
    "en": [("Flash", ["in Magisk or", "KernelSU"]),
           ("Reboot", ["Fully", "reversible"]),
           ("Grant root", ["Accept the", "root prompt"]),
           ("Open the app", ["Setup reads", "your device"])],
    "ar": [("افلش", ["في Magisk أو", "KernelSU"]),
           ("أعد التشغيل", ["قابل للإزالة", "كاملًا"]),
           ("امنحه الجذر", ["اقبل طلب", "الجذر"]),
           ("افتح التطبيق", ["الإعداد يقرأ", "جهازك"])],
}


def install_flow(pal, lang="en"):
    """أربع خطوات: فلاش ← إعادة تشغيل ← منح الجذر ← فتح التطبيق، ونقطة تعبرها وتقف عند كل واحدة."""
    ar = lang == "ar"
    W, H = 720, 236
    uid = "in"
    xs = [108.0, 276.0, 444.0, 612.0]
    if ar:
        xs = xs[::-1]
    cy, rr = 72.0, 32.0
    steps = INSTALL[lang]
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" '
               f'height="{H}" role="img" aria-labelledby="{uid}t {uid}d">')
    out.append(f'  <title id="{uid}t">' + ("التثبيت في أربع خطوات" if ar else "Install in four steps") + '</title>')
    out.append(f'  <desc id="{uid}d">' + (
        "افلش الوحدة في Magisk أو KernelSU، ثم أعد التشغيل، ثم امنح MaxManager الجذر حين "
        "يطلبه مدير الجذر، ثم افتح التطبيق وأكمل الإعداد."
        if ar else
        "Flash the module in Magisk or KernelSU, reboot, grant MaxManager root when your root "
        "manager asks, then open the app and walk through setup.") + '</desc>')
    out.append(defs_all(uid, pal))
    out.append(panel(uid, 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(W / 2, 80, 200, pal["accent"], 0.03, 0.08, 10.0))
    # الوصلات: خطّ متقطّع يجري من خطوة إلى التالية (الاتجاه = اتجاه الخطّ)
    for a_, b_ in zip(xs, xs[1:]):
        sgn = 1 if b_ > a_ else -1
        out.append(f'  <path d="M{a_ + sgn * (rr + 6):.1f} {cy:.1f} H{b_ - sgn * (rr + 6):.1f}" '
                   f'fill="none" stroke="{pal["steel"]}" stroke-width="2.6" stroke-linecap="round" '
                   f'stroke-dasharray="4 10" stroke-dashoffset="0" opacity="0.8">')
        out.append('    <animate attributeName="stroke-dashoffset" values="0;-28" keyTimes="0;1" '
                   'dur="1.6s" repeatCount="indefinite"/>')
        out.append('  </path>')
    # النقطة العابرة: تبدأ من الخطوة الأولى وتمرّ بالأربع، ثم تبقى لحظة عند الأخيرة
    out.append(f'  <circle cx="{xs[0]:.1f}" cy="{cy:.1f}" r="6.5" fill="{pal["accent"]}">')
    out.append(f'    <animate attributeName="cx" '
               f'values="{xs[0]:.1f};{xs[1]:.1f};{xs[2]:.1f};{xs[3]:.1f};{xs[3]:.1f}" '
               f'keyTimes="0;0.27;0.54;0.81;1" dur="8s" repeatCount="indefinite"/>')
    out.append('  </circle>')
    for i, (x, (title, sub)) in enumerate(zip(xs, steps)):
        out.append(f'  <circle cx="{x:.1f}" cy="{cy:.1f}" r="{rr:.0f}" fill="{pal["panelBottom"]}" '
                   f'stroke="{pal["accent"]}" stroke-width="2.4"/>')
        out.append(f'  <circle cx="{x:.1f}" cy="{cy:.1f}" r="{rr - 4:.0f}" fill="{pal["accent"]}" opacity="0">')
        # قمّة التوهّج عند وصول النقطة: 0.27 × 8 = 2.16s بين خطوة وأخرى، والبدء السالب يُزيح الطور
        out.append(f'    <animate attributeName="opacity" values="0;0.3;0" keyTimes="0;0.5;1" '
                   f'dur="8s" begin="{-4 + 2.16 * i:.2f}s" repeatCount="indefinite"/>')
        out.append('  </circle>')
        out.append(text(x, cy + 11, str(i + 1), 30, pal["ink"], anchor="middle", weight="800"))
        out.append(text(x, 152, _fit(title, 24, 162), 24, pal["ink"], anchor="middle",
                        weight="700", rtl=ar))
        for j, line in enumerate(sub):
            out.append(text(x, 184 + j * 26, _fit(line, 21, 162), 21, pal["muted"],
                            anchor="middle", rtl=ar))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 3e. divider — فاصل بين الأقسام: خطّ ونبضة تعبره
# ═══════════════════════════════════════════════════════════════════════════
def divider(pal):
    W, H = 960, 28
    uid = "dv"
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" '
               f'height="{H}" role="img" aria-labelledby="{uid}t {uid}d">')
    out.append(f'  <title id="{uid}t">Section divider</title>')
    out.append(f'  <desc id="{uid}d">A thin line with a pulse travelling along it and a small '
               'mark in the middle.</desc>')
    out.append('  <defs>')
    out.append(f'    <linearGradient id="{uid}-line" x1="0" y1="0" x2="1" y2="0">')
    out.append(f'      <stop offset="0" stop-color="{pal["steel"]}" stop-opacity="0"/>')
    out.append(f'      <stop offset="0.25" stop-color="{pal["steel"]}" stop-opacity="0.55"/>')
    out.append(f'      <stop offset="0.5" stop-color="{pal["accent"]}" stop-opacity="0.9"/>')
    out.append(f'      <stop offset="0.75" stop-color="{pal["steel"]}" stop-opacity="0.55"/>')
    out.append(f'      <stop offset="1" stop-color="{pal["steel"]}" stop-opacity="0"/>')
    out.append('    </linearGradient>')
    out.append('  </defs>')
    out.append(f'  <rect x="40" y="13" width="880" height="2" rx="1" fill="url(#{uid}-line)"/>')
    out.append(f'  <circle cx="480" cy="14" r="7" fill="none" stroke="{pal["accent"]}" stroke-width="1.8"/>')
    out.append(f'  <circle cx="480" cy="14" r="3" fill="{pal["accent"]}">')
    out.append('    <animate attributeName="opacity" values="1;0.35;1" keyTimes="0;0.5;1" '
               'dur="4s" repeatCount="indefinite"/>')
    out.append('  </circle>')
    out.append(f'  <circle cx="450" cy="14" r="2" fill="{pal["steel"]}" opacity="0.7"/>')
    out.append(f'  <circle cx="510" cy="14" r="2" fill="{pal["steel"]}" opacity="0.7"/>')
    out.append(f'  <circle cx="40" cy="14" r="4.5" fill="{pal["accent"]}" opacity="0">')
    out.append('    <animate attributeName="cx" values="40;920" keyTimes="0;1" dur="7s" '
               'repeatCount="indefinite"/>')
    out.append('    <animate attributeName="opacity" values="0;0.95;0.95;0" '
               'keyTimes="0;0.12;0.88;1" dur="7s" repeatCount="indefinite"/>')
    out.append('  </circle>')
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 4. control-plane.svg — one road for every change, in product words (no file names)
# ═══════════════════════════════════════════════════════════════════════════
def control_plane(pal):
    """طريق واحد لكل تغيير: لمسة ← مالك واحد ← كتابة بالجذر ← قراءة مرتدّة، ثم إحدى نتيجتين.

    **المحتوى كُتب للقارئ لا للمهندس:** كل بطاقة تقول ما يحدث له (لمسته، من يقرّر، أين تُكتب، هل ثبتت)،
    وتنتهي بنتيجتين صريحتين: ثبت فيبقى، أو لم يثبت فيُبلَّغ عنه ويُتراجع. وبلا أسماء ملفّات ولا كلام عن
    الاختبارات. والحركة زينة فوق قراءة صحيحة: نقطة تعبر سكّة فوق البطاقات، وتتوهّج كل بطاقة حين تبلغها.
    """
    W, H = 720, 528
    steps = [("You tap", ["a switch or", "a slider"]),
             ("One owner", ["decides if", "it is safe"]),
             ("Root write", ["one audited", "door"]),
             ("Read back", ["did the", "change hold?"])]
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="ct cd">')
    out.append('  <title id="ct">One road for every change</title>')
    out.append('  <desc id="cd">You tap; one owner decides whether it is safe; a single audited root '
               'write applies it; the device is read back. If the change held it stays; if not it is '
               'reported and undone. No screen can write to the kernel by itself.</desc>')
    out.append(defs_all("cp", pal, card="cp-card"))
    out.append(panel("cp", 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(614, 74, 152, pal["accent"], 0.04, 0.10, 10.0))
    out.append(text(36, 62, "One road for every change", 36, pal["ink"], weight="700"))
    out.append(text(36, 98, "Nothing touches the kernel behind your back.", 22, pal["muted"]))

    bw, gap, bh, by = 156.0, 18.0, 150.0, 140.0
    xs = [21 + i * (bw + gap) for i in range(4)]
    rail_y = by - 14
    out.append(f'  <path d="M{xs[0] + bw / 2:.1f} {rail_y:.1f} H{xs[3] + bw / 2:.1f}" fill="none" '
               f'stroke="{pal["steel"]}" stroke-width="2.2" stroke-linecap="round" '
               f'stroke-dasharray="4 9" opacity="0.7"/>')
    out.append(f'  <circle cx="{xs[0] + bw / 2:.1f}" cy="{rail_y:.1f}" r="6" fill="{pal["accent"]}">')
    out.append(f'    <animate attributeName="cx" values="{";".join(f"{x + bw / 2:.1f}" for x in xs)};'
               f'{xs[3] + bw / 2:.1f}" keyTimes="0;0.27;0.54;0.81;1" dur="7s" '
               f'repeatCount="indefinite"/>')
    out.append('  </circle>')
    for i, (label, caption) in enumerate(steps):
        x = xs[i]
        out.append(f'  <rect x="{x:.1f}" y="{by:.1f}" width="{bw:.1f}" height="{bh:.0f}" rx="16" '
                   f'fill="url(#cp-card)" stroke="{pal["edge"]}"/>')
        out.append(f'  <rect x="{x + 1:.1f}" y="{by + 1:.1f}" width="{bw - 2:.1f}" height="{bh - 2:.0f}" '
                   f'rx="15" fill="{pal["accent"]}" opacity="0">')
        out.append(f'    <animate attributeName="opacity" values="0;0.14;0" keyTimes="0;0.5;1" '
                   f'dur="7s" begin="{-3.5 + 1.89 * i:.2f}s" repeatCount="indefinite"/>')
        out.append('  </rect>')
        out.append(f'  <circle cx="{x + 28:.1f}" cy="{by + 28:.1f}" r="12" fill="none" '
                   f'stroke="{pal["accent"]}" stroke-width="2"/>')
        out.append(text(x + 28, by + 36, str(i + 1), 21, pal["accent"], anchor="middle", weight="700"))
        out.append(text(x + bw / 2, by + 84, _fit(label, 24, bw - 16), 24, pal["ink"],
                        anchor="middle", weight="700"))
        for j, line in enumerate(caption):
            out.append(text(x + bw / 2, by + 114 + j * 25, _fit(line, 21, bw - 16), 21, pal["muted"],
                            anchor="middle"))
        if i < 3:
            ax = x + bw + gap / 2
            out.append(f'  <path d="M{ax - 6:.1f},{by + 28:.1f} L{ax + 5:.1f},{by + 28:.1f} '
                       f'L{ax - 0.5:.1f},{by + 21:.1f} Z" fill="{pal["steel"]}"/>')

    # ── النتيجتان ──
    cy0 = by + bh + 34
    out.append(text(W / 2, cy0, "Then one of two things happens", 21, pal["steel"], anchor="middle"))
    cw, ch = 333.0, 118.0
    cards = [(21.0, "It held", ["The change stays,", "and the app says so."], pal["accent"]),
             (366.0, "It did not hold", ["Reported as a failure,", "then undone."], pal["clay"])]
    for k, (x, title, lines, tone) in enumerate(cards):
        y = cy0 + 20
        out.append(f'  <rect x="{x:.0f}" y="{y:.0f}" width="{cw:.0f}" height="{ch:.0f}" rx="14" '
                   f'fill="url(#cp-card)" stroke="{pal["edge"]}"/>')
        out.append(f'  <rect x="{x + 18:.0f}" y="{y + 20:.0f}" width="5" height="26" rx="2.5" fill="{tone}"/>')
        out.append(text(x + 36, y + 42, _fit(title, 24, cw - 56), 24, tone, weight="700"))
        for j, line in enumerate(lines):
            out.append(text(x + 36, y + 72 + j * 26, _fit(line, 21, cw - 56), 21, pal["muted"]))
    out.append(text(W / 2, H - 28, "No screen can write to the kernel by itself.", 21, pal["steel"],
                    anchor="middle"))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 5. gates.svg — what the checks are for, not what they are called
# ═══════════════════════════════════════════════════════════════════════════
def gates_strip(pal):
    W = 720
    # وسابعَ عشر: «mark geometry» — بوّابة علامة Max AI (تكملة ١٥٣) تقرأ أرقام الرسم من الكود
    # وترسمه بمسح ضوئيّ عند ١× و٢× و٣× و٤×. **ولا تُضف بلاطة إلى هذه القائمة بيد في الـSVG**:
    # كان ذلك قد وقع فعلًا (بلاطة «screenshots» أُضيفت بيد)، فصار المولّد يكتب ١٤ والمنشور فيه ١٥ —
    # والصحيح أن القائمة هنا هي المصدر، والـSVG مُعاد توليدُه منها.
    checks = ["balanced code", "clean tree", "translations", "orphan keys", "JNI contract",
              "unused modules", "design tokens", "packaging", "resources", "RTL layout",
              "licences", "file manifest", "links resolve", "readable assets", "screenshots",
              "mark geometry"]
    cols, cw, chh, gx, gy = 3, 212.0, 62.0, 12.0, 12.0
    top = 132.0
    rows = math.ceil(len(checks) / cols)
    # الارتفاع يُحسب من الصفوف — لا يُكتب رقمًا: النسخة الأولى كانت H=400 وصفّها
    # الأخير ينتهي عند 490، فأربع بلاطات لم تُرَ قطّ في أي محرّك.
    H = int(top + rows * (chh + gy) - gy + 30)
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="gt gd">')
    out.append(f'  <title id="gt">{len(checks)} checks before the compiler runs</title>')
    out.append(f'  <desc id="gd">A grid of the {len(checks)} checks that run in seconds, before any '
               'heavy build step, so a mistake is named early instead of after minutes of compiling.</desc>')
    out.append(defs_all("gt", pal, card="gt-card"))
    out.append(panel("gt", 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(614, 74, 152, pal["accent"], 0.04, 0.09, 10.0))
    out.append(text(36, 62, "Checked before it is built", 38, pal["ink"], weight="700"))
    out.append(text(36, 96, f"{len(checks)} checks run in seconds, ahead of every build.", 22, pal["muted"]))
    for i, label in enumerate(checks):
        r, c = divmod(i, cols)
        x = 30 + c * (cw + gx)
        y = top + r * (chh + gy)
        out.append(f'  <rect x="{x:.1f}" y="{y:.1f}" width="{cw:.1f}" height="{chh:.0f}" rx="12" '
                   f'fill="url(#gt-card)" stroke="{pal["edge"]}"/>')
        out.append(f'  <circle cx="{x+28:.1f}" cy="{y+31:.1f}" r="7" fill="{pal["accent"]}">')
        # بداية متتالية (نمط «staggered begin»): الصفّ لا ينبض في لحظة واحدة، والترتيب يُقرأ
        # «فحوصًا تمرّ» لا وميضًا. والحركة `opacity` وحدها — لا موضع يتحرّك، فالساكن صحيح.
        out.append(f'    <animate attributeName="opacity" values="1;0.45;1" keyTimes="0;0.5;1" '
                   f'dur="6s" begin="{i * 0.35:.2f}s" repeatCount="indefinite"/>')
        out.append('  </circle>')
        out.append(text(x + 48, y + 38, label, 21, pal["ink"]))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 6. locales.svg — languages, as a product fact
# ═══════════════════════════════════════════════════════════════════════════
def locales_art(pal):
    W, H = 720, 300
    names = ["English", "العربية", "简体中文", "Español", "Русский", "Français",
             "Português", "Türkçe", "Indonesia", "Deutsch"]
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="lt ld">')
    out.append('  <title id="lt">84 languages, and right-to-left is first class</title>')
    out.append('  <desc id="ld">The interface ships in 84 languages plus English. Arabic, Farsi, '
               'Hebrew and Urdu are laid out right-to-left and checked on every run.</desc>')
    out.append(defs_all("lo", pal))
    out.append(panel("lo", 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(614, 74, 152, pal["accent"], 0.04, 0.09, 10.0))
    out.append(text(36, 62, "84 languages", 38, pal["ink"], weight="700"))
    out.append(text(36, 96, "Arabic, Farsi, Hebrew and Urdu are laid out right-to-left.", 22, pal["muted"]))
    # two rows of names, evenly spaced
    rows = [names[:5], names[5:]]
    for r, row in enumerate(rows):
        y = 152 + r * 62
        step = (W - 60) / len(row)
        for i, n in enumerate(row):
            # وكل اسم يتنفّس بإزاحة تصاعديّة عبر اللوحتين (`staggered begin`): القراءة لا تحتاج
            # الحركة، فبقيت الأسماء هي النصّ نفسه وحركتها `opacity` وحدها — لا موضع يزحف.
            x = 30 + step * (i + 0.5)
            out.append(f'  <text x="{x:.1f}" y="{y:.1f}" font-size="22" fill="{pal["steel"]}" '
                       f'text-anchor="middle" font-family="{SANS}" opacity="1">')
            out.append(f'    <animate attributeName="opacity" values="1;0.5;1" keyTimes="0;0.5;1" '
                       f'dur="6s" begin="{(r * 5 + i) * 0.3:.1f}s" repeatCount="indefinite"/>')
            out.append(f'    {esc(n)}')
            out.append('  </text>')
    out.append(text(W / 2, 268, "+ 75 more, and English is authored by hand", 21, pal["muted"],
                    anchor="middle"))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 6.5 design-language.svg — لغة التصميم في نظرة واحدة
# ═══════════════════════════════════════════════════════════════════════════
# اللوحة **للقارئ**: ما يراه في التطبيق (لون واحد يتبع ثيمه، لونا تحذير ثابتان، ثلاثة محارف، شكل
# موحّد، حركة قصيرة) لا ما يهمّ من يراجع الشيفرة. فسقطت منها مقاييس المسافات وأرقام التباين وجملة
# «بوّابة تفشل»، وسقطت عيّنة «الإيجابي» الخضراء من الصورة (تبقى نغمة حالة في التطبيق، لا زينة هنا).
# وكل نصّ ≥ 21px في الـviewBox (720) أي ≥ 11px على الهاتف، وهو حدّ `svg_review`.
def design_language(pal, dark=True):
    W, H = 720, 880
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="dl-t dl-d">')
    out.append('  <title id="dl-t">The MaxManager look: one accent, two warnings, three typefaces</title>')
    out.append('  <desc id="dl-d">One accent colour that follows the user theme, two fixed warning '
               'colours, three typefaces with one job each, a small set of corner radii, and motion '
               'that stays between 90 and 360 milliseconds.</desc>')
    out.append(defs_all("dl", pal, card="dl-card"))
    out.append(panel("dl", 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(620, 70, 140, pal["accent"], 0.03, 0.08, 12.0))
    out.append(header(36, 44, "The look",
                      "One accent. Two warnings. Nothing else shouts.", pal))

    def eyebrow(y, s):
        out.append(text(36, y, s, 21, pal["muted"], weight="600"))

    eyebrow(188, "COLOUR")
    swatches = [("Accent", "follows your theme", pal["accent"]),
                ("Caution", pal["toneCaution"], pal["toneCaution"]),
                ("Critical", pal["toneCritical"], pal["toneCritical"])]
    for i, (name, note, tone) in enumerate(swatches):
        x = 36 + i * 224
        out.append(f'  <rect x="{x}" y="206" width="96" height="88" rx="14" fill="{tone}"/>')
        out.append(text(x, 326, name, 22, pal["ink"], weight="700"))
        out.append(text(x, 354, _fit(note, 21, 214), 21, pal["muted"]))
    out.append(text(36, 396, "Readable on dark and on light — only the accent moves.", 21, pal["steel"]))

    eyebrow(452, "TYPE")
    out.append(text(36, 492, "Space Grotesk", 21, pal["muted"]))
    out.append(text(300, 496, "Page title", 34, pal["ink"], weight="700"))
    out.append(text(36, 546, "Manrope", 21, pal["muted"]))
    out.append(text(300, 548, "Supporting copy", 22, pal["ink"]))
    out.append(text(36, 600, "Mono, for live values", 21, pal["muted"]))
    out.append(text(300, 602, "42.5 °C · 3.1 GHz", 30, pal["accent"], weight="600", family=MONO))

    eyebrow(662, "SHAPE")
    for i, radius in enumerate((12, 14, 18, 22, 28)):
        x = 36 + i * 100
        out.append(f'  <rect x="{x}" y="688" width="60" height="60" rx="{radius}" '
                   f'fill="url(#dl-card)" stroke="{pal["edge"]}" stroke-width="1.5"/>')
    out.append(f'  <rect x="544" y="694" width="132" height="48" rx="24" fill="none" '
               f'stroke="{pal["accent"]}" stroke-width="1.5"/>')
    out.append(text(36, 786, "Rows, cards and sheets each get their own corner.", 21, pal["steel"]))

    eyebrow(828, "MOTION")
    x = 150.0
    for i, ms in enumerate((90, 160, 240, 360)):
        w = ms * 0.5
        out.append(f'  <rect x="{x:.0f}" y="816" width="{w:.0f}" height="10" rx="5" '
                   f'fill="{pal["accent"]}" opacity="0.85">')
        out.append(f'    <animate attributeName="opacity" values="0.85;0.45;0.85" keyTimes="0;0.5;1" '
                   f'dur="4.5s" begin="{i * 0.25:.2f}s" repeatCount="indefinite"/>')
        out.append('  </rect>')
        x += w + 14
    out.append(text(36, 858, "Short and quiet: 90 to 360 ms.", 21, pal["steel"]))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 7. domains.svg — كانت مكتوبة بيد، فصارت من المولّد
# ═══════════════════════════════════════════════════════════════════════════
# **لماذا نُقلتا إلى هنا:** كانتا الملفّين الوحيدين في `docs/assets/` لا يولّدهما شيء — فبقيا
# بلغتين أُخريين: لوح `#12202A` لا `#151C22`، ونصف قطر ١٤ لا ٢٢، **وبلا حدّ على اللوح نفسه**
# ولا عمق. أي أنّ قارئًا يمرّ في الصفحة يرى شكلين لـ«البطاقة» بلا سبب، وهو بعينه ما يجعل الأصول
# تبدو مجموعة من مصادر مختلفة. وكون الأصل خارج المولّد يعني أيضًا أنّه خارج أيّ إعادة توليد —
# فقطع أيّ تحسين عليه نصف الشجرة. ونقلُهما هنا يُصلح السبب لا العَرَض.

def domains_art(pal):
    """عشرة مجالات — والرمز داخل كل بلاطة هو **هندسة `ic-*.svg` نفسها** مكبَّرة لا رسمًا ثانيًا.

    وهذا مقصود: القارئ يرى الرمز في قائمة النصّ وفي الصورة، فلوناه مختلفان شكلان لشيء واحد. وموضع
    الرسم يُحسب من مربّع الرمز المحفوظ `[4, 20]` لا يُخمَّن.
    """
    W = 720
    arts = icons_domains()
    captions = ["Cores · governors", "Freqs · policies", "ZRAM · swappiness",
                "Refresh · colour", "Touch · frames", "Heat · throttling",
                "Charge · bypass", "Compiler · health", "Link · scheduling",
                "Devices · effects"]
    cols, tw, th, gx, gy, top = 3, 218.0, 132.0, 17.0, 14.0, 132.0
    rows = math.ceil(len(arts) / cols)
    H = int(top + rows * (th + gy) - gy + 30)
    out = []
    out.append(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
               f'width="{W}" height="{H}" role="img" aria-labelledby="dt dd">')
    out.append('  <title id="dt">What you can control — ten domains</title>')
    out.append('  <desc id="dd">Ten control domains, each with the app\'s own one-line '
               'description: CPU cores and governors, GPU frequencies and policies, memory ZRAM '
               'and swappiness, display refresh and colour, responsiveness touch and frames, '
               'thermal heat and throttling, power charge and bypass, storage and compiler '
               'health, network scheduling and link state, and audio output devices with the '
               'effects the engine declares. A control the device does not expose is not '
               'shown.</desc>')
    out.append(defs_all("do", pal, card="do-card"))
    out.append(panel("do", 0, 0, W, H, 22, pal))
    out.append(edge_light(34, 6, W - 68, pal))
    out.append(breathe(614, 74, 152, pal["accent"], 0.04, 0.09, 10.0))
    out.append(text(36, 62, "What you can control", 34, pal["ink"], weight="700"))
    # والسطر التوضيحيّ **قِيس فقُصر**: البوّابة أبلغت أنّ الصيغة الأطول تنتهي ٤px خارج اللوحة
    # على عرض ٣٩٠px — وهي اللوحة التي تُقاس فعلًا لأنّ الصورة تُعرض بعرض الصفحة على الهاتف.
    out.append(text(36, 96, "Anything your device does not expose is simply not listed.",
                    21, pal["muted"]))
    # والأسماء **قصيرة معلنة** لا `title` الرمز: عنوان الرمز كان يصف فيُطيل («Storage and
    # compiler») — وهو صحيح في `ic-storage.svg` ومصادم داخل بلاطة عرضها ٢١٨. فالإسم المعلَن هنا
    # هو ما يُرسم، والتفصيل في النصّ وفي الرمز.
    names = ["CPU", "GPU", "Memory", "Display", "Responsiveness", "Thermal",
             "Power", "Storage", "Network", "Audio"]
    for i, ((_icon, _title, _desc, body), name, caption) in enumerate(
            zip(arts, names, captions)):
        r, c = divmod(i, cols)
        x = 16 + c * (tw + gx)
        y = top + r * (th + gy)
        out.append(f'  <rect x="{x:.1f}" y="{y:.1f}" width="{tw:.1f}" height="{th:.0f}" '
                   f'rx="18" fill="url(#do-card)" stroke="{pal["edge"]}" stroke-width="1.4"/>')
        # الورقة [4, 20] من شبكة 24 تُوضع عند (x+14.4, y+14.4) بمقياس ١٫٤
        # وكل بلاطة تتنفّس بإزاحة مختلفة (`staggered begin`) — فتقرأها العين "حيّة" لا "وامضة"،
        # وهذا فرق مقصود: الأعمدة المتساوية في الطور تُنتج وميضًا، والإزاحة تُنتج إيقاعًا.
        # و`<animate>` ابن لمجموعة الرمز لا عنصر مستقلّ (القاعدة ⑤)، والساكن صحيح بلا حركة.
        out.append(f'  <g transform="translate({x + 8.4:.1f},{y + 8.4:.1f}) scale(1.4)" opacity="1">')
        out.append(f'    <animate attributeName="opacity" values="1;0.55;1" keyTimes="0;0.5;1" '
                   f'dur="7s" begin="{i * 0.4:.1f}s" repeatCount="indefinite"/>')
        out.extend("    " + frag for frag in body)
        out.append('  </g>')
        out.append(text(x + 20, y + 92, name, 24, pal["ink"], weight="700"))
        out.append(text(x + 20, y + 120, caption, 21, pal["steel"]))
    out.append('</svg>')
    return "\n".join(out) + "\n"


# ═══════════════════════════════════════════════════════════════════════════
# 8. ic-*.svg — أيقونات سياقية: واحدة بجانب كل عنوان، وواحدة بجانب كل وصف
# ═══════════════════════════════════════════════════════════════════════════
# لماذا: الصفحة كانت تُخبر القارئ *ماذا* يقرأ بنصّ فقط، والعين تبحث عن معلَم بصريّ قبل أن تقرأ
# سطرًا. فصار لكل قسم رمزه (المعالج لصفحة المعالج، والنجمة لـMax AI)، ولكل بند في قائمة المجالات
# التسعة رمزه — فيُمسح البند بالعين لا بالقراءة.
#
# وحدودها معلَنة: شبكة 24×24، والفنّ كلّه داخل [4.2, 19.8] فلا يلامس حدًّا ولا يُقصّ، والبلاطة
# كاملة 24×24 (فتُستثنى من فحص القصّ في `svg_review` كما تُستثنى خلفية اللوحة في الأصل الكبير).
# والألوان من اللوحة نفسها: بلاطة `DARK["bg"]` · حدّها `DARK["tileEdge"]` · تمييزيّ `SURFACE_DARK["accent"]`
# (العنصر الأساس) · فولاذي `DARK["steel"]`. والاختيار مقصود: **البلاطة الداكنة تُقرأ في الوضعين**
# — الرمز يُحمَّل في `<img>` فلا يرث ألوان الصفحة، ولو رُسم بخطّ ملوّن وحده لاختفى في الوضع الفاتح.
ICON_TILE = ('<rect x="0" y="0" width="24" height="24" rx="6.5" fill="' + DARK["bg"] + '" '
             'stroke="' + DARK["tileEdge"] + '" stroke-width="1"/>')
ICON_ACCENT, ICON_STEEL = SURFACE_DARK["accent"], SURFACE_DARK["steel"]


def _ink(d, color=ICON_ACCENT, w=1.9):
    """خطّ مرسوم: نهايات وزوايا مدوّرة — فلا تظهر رؤوس حراب في رمز 20px."""
    return (f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{w}" '
            f'stroke-linecap="round" stroke-linejoin="round"/>')


def _solid(d, color=ICON_ACCENT):
    return f'<path d="{d}" fill="{color}"/>'


def _box(x, y, w, h, rx, color=ICON_STEEL, sw=1.8, fill="none"):
    return (f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" '
            f'stroke="{color}" stroke-width="{sw}"/>')


def _disc(cx, cy, r, color=ICON_ACCENT):
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{color}"/>'


def _ring(cx, cy, r, color=ICON_STEEL, sw=1.8):
    return (f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{color}" '
            f'stroke-width="{sw}"/>')


def icons_sections():
    """رماز الأقسام: رمز واحد لكل عنوان في الـREADME، فيُمسح الدليل بالعين لا بالقراءة."""
    return [
        ("ic-timer", "Ten seconds", "A bolt: the whole idea in ten seconds.", [
            _solid("M13.6 4.4 7.2 13.3h3.7l-0.5 6.3 6.4-8.9h-3.7z"),
        ]),
        ("ic-layers", "Three layers", "Three stacked layers: Max Atlas, the control plane, Max AI.", [
            _solid("M12 4.5 19.4 8.4 12 12.3 4.6 8.4z"),
            _ink("M4.6 12.9 12 16.6 19.4 12.9", ICON_STEEL),
            _ink("M4.6 16.2 12 19.9 19.4 16.2", ICON_STEEL),
        ]),
        ("ic-atlas", "Max Atlas", "A route between two points: the path to an interface is discovered.", [
            _ink("M6 18C9.2 18 9 12.4 12 12.4 15 12.4 14.8 6 18 6", ICON_STEEL, 1.5),
            _disc(6, 18, 1.8),
            _disc(18, 6, 1.8),
        ]),
        ("ic-ai", "Max AI", "A four-point sparkle: the Max AI mark, the AI accent.", [
            _solid("M12 4.2Q12.85 11.15 19.8 12Q12.85 12.85 12 19.8"
                   "Q11.15 12.85 4.2 12Q11.15 11.15 12 4.2z"),
        ]),
        ("ic-sliders", "Controls", "Three faders: control you set yourself.", [
            _ink("M5 7.8H19M5 12H19M5 16.2H19", ICON_STEEL),
            _disc(9.8, 7.8, 2),
            _disc(14.6, 12, 2),
            _disc(8.4, 16.2, 2),
        ]),
        ("ic-pulse", "Live readings", "A reading inside a frame: measuring, not guessing.", [
            _box(4.6, 5.4, 14.8, 13.2, 2.6),
            _ink("M6.8 12.4h2.2l1.4-3.4 2 6.4 1.4-3h3.4"),
        ]),
        ("ic-phone", "Screens", "A phone with a lit screen.", [
            _box(6.6, 4.4, 10.8, 15.2, 2.4, sw=1.5),
            _box(8.4, 7.4, 7.2, 8, 1.2, fill=ICON_ACCENT, sw=1),
            _ink("M10.4 17.2h3.2"),
        ]),
        ("ic-globe", "Languages", "A globe with a meridian: the interface travels.", [
            _ring(12, 12, 7.2),
            f'<ellipse cx="12" cy="12" rx="3.3" ry="7.2" fill="none" '
            f'stroke="{ICON_ACCENT}" stroke-width="1.4"/>',
            _ink("M5.2 12h13.6", ICON_STEEL),
        ]),
    ]


def icons_setup():
    """رماز المسار العمليّ: المتطلبات · التثبيت · ما لن يفعله المنتج."""
    return [
        ("ic-checklist", "Requirements", "A clipboard with a tick: what the device must be.", [
            _box(6, 5.4, 12, 14.2, 2.4),
            _box(9.4, 4.2, 5.2, 2.8, 1.2, fill=ICON_ACCENT, sw=1),
            _ink("M8.6 12.4 10.8 14.6 14.8 10.2", ICON_ACCENT, 1.6),
            _ink("M8.6 17h4.6", ICON_STEEL),
        ]),
        ("ic-download", "Install", "An arrow onto a tray: the module is flashed, not compiled by you.", [
            _ink("M12 4.6v8.4M8.6 10.2 12 13.6 15.4 10.2", ICON_ACCENT, 1.7),
            _ink("M5.4 15.2v1.4c0 1.6 1.2 2.8 2.8 2.8h7.6c1.6 0 2.8-1.2 2.8-2.8v-1.4",
                 ICON_STEEL),
        ]),
        ("ic-shield", "Never touched", "A shield you cannot switch off, with a line through it.", [
            _ink("M12 4.2 18.8 6.8v5.2c0 3.6-2.8 6.4-6.8 7.7-4-1.3-6.8-4.1-6.8-7.7V6.8z",
                 ICON_STEEL, 1.5),
            _ink("M9 15.4 15.2 9.2", ICON_ACCENT, 1.8),
        ]),
    ]


def icons_reference():
    """رماز المرجع: الوثائق · الأسئلة · الدعم · الرخصة."""
    return [
        ("ic-doc", "Documentation", "A page with a folded corner: where the detail lives.", [
            _ink("M6.4 4.4h7.2l4 4v11.2H6.4z", ICON_STEEL),
            _ink("M13.6 4.4v4.2h4.2", ICON_STEEL),
            _ink("M8.8 12h5.6M8.8 15.4h5.6"),
        ]),
        ("ic-question", "FAQ", "A question mark in a circle.", [
            _ring(12, 12, 7.2, ICON_STEEL, 1.5),
            _ink("M9.4 10.2c0-2.6 5.4-2.6 5.4 0.2 0 2-2.4 1.8-2.4 4.2", ICON_ACCENT, 1.7),
            _disc(12.4, 17.1, 1.2),
        ]),
        ("ic-bubble", "Support", "A speech bubble with three dots: a report a person reads.", [
            _ink("M6.4 5.6h11.2c1.2 0 2.2 1 2.2 2.2v6.4c0 1.2-1 2.2-2.2 2.2h-6l-3.8 3.2"
                 "v-3.2H6.4c-1.2 0-2.2-1-2.2-2.2V7.8c0-1.2 1-2.2 2.2-2.2z", ICON_STEEL),
            _disc(9.4, 11, 1.2),
            _disc(12.4, 11, 1.2),
            _disc(15.4, 11, 1.2),
        ]),
        ("ic-seal", "Licence", "A seal on a ribbon: proprietary, stated plainly.", [
            _ring(12, 9.6, 5.4, ICON_STEEL, 1.5),
            _ink("M9.8 9.8 11.4 11.4 14.4 8.2"),
            _ink("M9 14.4 8 20 12 18 16 20 15 14.4", ICON_STEEL),
        ]),
        # كأس لا شريط وسام: `ic-seal` يحمل شريطًا تحت حلقة، فلو رُسم الوسام لالتبسا في 20px.
        # والكأس تفرّدت: لا رمز آخر فيه خطّان أفقيّان تحت بعضهما (فالفحص بالعين لا بالقراءة).
        ("ic-credit", "Credits", "A trophy: the work this project thanks.", [
            # الانحناءان مكتوبان صراحةً (`c…c…`) لا بالمنعكس المختصر `s`: أداة القراءة النصّية
            # في هذه البيئة لا تعرف `s`، فرسمٌ لا يُقرأ لا يُتحقّق منه — وتناظر الكأس أصلًا صريح.
            _ink("M9 5.2h6v3.4c0 2.5-1.4 4.4-3 4.4c-1.6 0-3-1.9-3-4.4z", ICON_ACCENT, 1.6),
            _ink("M9 6.9H7.6Q6.4 10 9 11", ICON_STEEL, 1.5),
            _ink("M15 6.9h1.4Q17.6 10 15 11", ICON_STEEL, 1.5),
            _ink("M12 13v2.2", ICON_STEEL, 1.5),
            _ink("M9.4 15.6h5.2", ICON_STEEL, 1.5),
            _ink("M8.4 18.4h7.2", ICON_STEEL, 1.5),
        ]),
    ]


def icons_domains():
    """رماز المجالات العشرة — يأتي بجانب وصف كل مجال في قائمة «ما تتحكم فيه».

    الفرق بينها مقصود ومقيس: تسعة أشكال مختلفة بأيقونة التخزين — فيُعرَف المجال بالعين قبل
    قراءة اسمه. وكلّها خطوط ومضلّعات وبلا `A` (أقواس)، فتُقاس بمشيِّر واحد بسيط.
    """
    return [
        ("ic-cpu", "CPU", "A chip with pins: cores, governor, per-cluster bounds.", [
            _box(7, 7, 10, 10, 2, ICON_ACCENT, 1.6),
            _box(10.2, 10.2, 3.6, 3.6, 0.8, fill=ICON_STEEL, sw=1),
            _ink("M9.4 7V4.4M14.6 7V4.4M9.4 17v2.6M14.6 17v2.6M7 9.4H4.4M7 14.6H4.4"
                 "M17 9.4h2.6M17 14.6h2.6", ICON_STEEL, 1.3),
        ]),
        ("ic-gpu", "GPU", "A graphics card with a fan: frequencies and vendor parameters.", [
            _box(4.6, 7.4, 14.8, 9.2, 2),
            _ring(9.6, 12, 2.8, ICON_ACCENT),
            _ink("M14.4 10.4h3M14.4 13.6h3"),
        ]),
        ("ic-memory", "Memory", "A memory stick: compression, swappiness, reclaim.", [
            _box(4.6, 8, 14.8, 8, 1.6),
            _ink("M7.6 10.4v3.2M10.2 10.4v3.2M12.8 10.4v3.2M15.4 10.4v3.2"),
            _ink("M8 16v2.2M12 16v2.2M16 16v2.2", ICON_STEEL, 1.3),
        ]),
        ("ic-display", "Display", "A lit screen on a stand: refresh, colour, brightness.", [
            _box(4.6, 5.4, 14.8, 10.2, 2),
            _disc(12, 10.5, 2.6),
            _ink("M12 15.6v2.4M8.8 18.8h6.4", ICON_STEEL),
        ]),
        ("ic-touch", "Responsiveness", "A tap with two ripples: sampling, smoothing, frame pacing.", [
            _disc(12, 14, 2.2),
            _ink("M8 11.4C9.4 9.6 14.6 9.6 16 11.4", ICON_STEEL, 1.5),
            _ink("M5.4 9.2C7.6 6.4 16.4 6.4 18.6 9.2", ICON_STEEL, 1.5),
            _ink("M7.4 18.4h9.2", ICON_STEEL),
        ]),
        ("ic-thermal", "Thermal", "A thermometer with heat marks: zones and policy.", [
            _box(10.6, 4.4, 2.8, 10.6, 1.4, ICON_ACCENT, 1.5),
            _disc(12, 16.6, 3.4),
            _ink("M16.4 8.4h3M16.4 12h2M16.4 15.6h3", ICON_STEEL),
        ]),
        ("ic-battery", "Power", "A battery with a bolt: charging, bypass, battery health.", [
            _box(4.6, 8, 13, 8, 2.4),
            _box(17.6, 10.6, 1.8, 2.8, 0.9, fill=ICON_STEEL, sw=1),
            _solid("M11.8 9.6 9.2 13h2.2l-0.6 2.6 2.8-3.6h-2.2z"),
        ]),
        ("ic-storage", "Storage and compiler", "A drive with a pilot light: compilation mode, storage health.", [
            _box(4.8, 6.4, 14.4, 11.2, 2.4),
            _ink("M4.8 12.8h14.4", ICON_STEEL),
            _ink("M7 15.4h5"),
            _disc(16.4, 15.4, 1.1),
        ]),
        # والعاشرة (`AU-01`): الصوت — سمّاعة وموجتان. والموجات منحنيات `Q` لا أقواس،
        # وأبعد نقطة `19.8` (الحدّ ٢٠) لأنّ ورقة الأمان تُقاس على الإحداثيّ نفسه لا على الرسم.
        # وتفرّدت عن `ic-remember`/`ic-pulse` بالمثلّث الوحيد المائل إلى اليمين.
        ("ic-audio", "Audio", "A speaker with two waves: output devices and declared effects.", [
            _ink("M5.2 9.6h3.4l4.6-4.2v13.2l-4.6-4.2H5.2z", ICON_ACCENT, 1.5),
            _ink("M15.6 9.8Q17.2 12 15.6 14.2", ICON_STEEL, 1.5),
            _ink("M18 7.6Q19.8 12 18 16.4", ICON_STEEL, 1.5),
        ]),
        ("ic-network", "Network", "Three nodes on a link: congestion, SACK, scheduler tunables.", [
            _ink("M6.6 17.4 12 6.6M17.4 17.4 12 6.6M6.6 17.4h10.8", ICON_STEEL),
            _disc(12, 6.6, 2.4),
            _disc(6.6, 17.4, 1.9),
            _disc(17.4, 17.4, 1.9),
        ]),
    ]


def icons_ai_steps():
    """رماز حلقة Max AI الخمسة — بجانب كل خطوة في «راقب · قرّر · اسأل · تحقّق · تذكّر».

    والخمسة مختارة لتفترق بلا نصّ: عين (راقب) ≠ هدف (قرّر) ≠ درع بتأشير (اسأل) ≠ عدسة
    بتأشير (تحقّق) ≠ علامة كتاب (تذكّر). والحلقتان الأخيرتان هما الأقرب — فاختير للتحقّق
    مقبض خارج الدائرة وللتذكّر طرف مدبّب، فلا يُخلط بينهما في 20px.
    """
    return [
        ("ic-notice", "Notice", "An eye: it reads this device.", [
            _ink("M4.4 12C6.8 7.8 17.2 7.8 19.6 12 17.2 16.2 6.8 16.2 4.4 12z", ICON_STEEL),
            _disc(12, 12, 2.6),
        ]),
        ("ic-decide", "Decide", "A target: the smallest change that could close the gap.", [
            _ring(12, 12, 6.6),
            _ring(12, 12, 3.2, ICON_ACCENT, 1.4),
            _disc(12, 12, 1.3),
        ]),
        ("ic-ask", "Ask", "A shield with a tick: the safety layer answers first.", [
            _ink("M12 4.2 18.8 6.8v5.2c0 3.6-2.8 6.4-6.8 7.7-4-1.3-6.8-4.1-6.8-7.7V6.8z",
                 ICON_STEEL),
            _ink("M9.4 11.8 11.2 13.6 15 9.6", ICON_ACCENT, 1.7),
        ]),
        ("ic-verify", "Verify", "A lens with a tick: the value is read back.", [
            _ring(10.8, 10.8, 5.4, ICON_STEEL, 1.5),
            _ink("M14.8 14.8 19 19", ICON_STEEL, 1.6),
            _ink("M8.6 10.9 10.4 12.7 13.4 9.4"),
        ]),
        ("ic-remember", "Remember", "A bookmark: the outcome is kept.", [
            _ink("M7.2 4.6h9.6v15l-4.8-4-4.8 4z", ICON_ACCENT, 1.6),
        ]),
    ]


def all_icons():
    """كل الرماز في قائمة واحدة: ما يُولَّد هو ما يُكتب على القرص، وما يُقاس."""
    return (icons_sections() + icons_setup() + icons_reference()
            + icons_domains() + icons_ai_steps())


def icon_svg(name, title, desc, body):
    """ملفّ SVG واحد لكل رمز: `<title>` و`<desc>` كما تشترط البوّابة ⑩، والبلاطة أولا."""
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" '
           f'height="24" role="img" aria-labelledby="{name}-t {name}-d">',
           f'  <title id="{name}-t">{title}</title>',
           f'  <desc id="{name}-d">{desc}</desc>',
           f'  {ICON_TILE}']
    out += ["  " + frag for frag in body]
    out.append("</svg>")
    return "\n".join(out) + "\n"


def write(name, body):
    path = os.path.join(OUT, name)
    with open(path, "w", encoding="utf-8") as f:
        f.write(body)
    return path


if __name__ == "__main__":
    files = [
        ("atlas-cycle.svg", atlas_cycle(SURFACE_DARK)),
        ("max-ai.svg", max_ai(SURFACE_DARK)),
        ("banner-dark.svg", banner(SURFACE_DARK)),
        ("banner-light.svg", banner(SURFACE_LIGHT, dark=False)),
        ("control-plane.svg", control_plane(SURFACE_DARK)),
        ("gates.svg", gates_strip(SURFACE_DARK)),
        ("locales.svg", locales_art(SURFACE_DARK)),
        ("design-language.svg", design_language(SURFACE_DARK)),
        ("design-language-light.svg", design_language(SURFACE_LIGHT, dark=False)),
        ("domains.svg", domains_art(SURFACE_DARK)),
        ("stats.svg", stats_strip(SURFACE_DARK)),
        ("stats-ar.svg", stats_strip(SURFACE_DARK, "ar")),
        ("pillars.svg", pillars(SURFACE_DARK)),
        ("pillars-ar.svg", pillars(SURFACE_DARK, "ar")),
        ("install-flow.svg", install_flow(SURFACE_DARK)),
        ("install-flow-ar.svg", install_flow(SURFACE_DARK, "ar")),
        ("divider.svg", divider(SURFACE_DARK)),
    ]
    # الرماز السياقية: تُولَّد من التعريف نفسه الذي يُقّاس، فلا يُكتب رسم بيد في ملفّ SVG
    files += [(f"{n}.svg", icon_svg(n, t, d, b)) for n, t, d, b in all_icons()]
    for name, body in files:
        p = write(name, body)
        # read it back and verify the claims
        back = open(p, encoding="utf-8").read()
        assert back == body, f"{name}: read-back mismatch"
        assert "<script" not in back and "keyframes" not in back, f"{name}: unsupported construct"
        import re
        assert "<svg" in back and back.rstrip().endswith("</svg>"), f"{name}: not closed"
        for m in re.finditer(r'keyTimes="([^"]+)"', back):
            ks = [float(v) for v in re.split(r'[;\s]+', m.group(1).strip())]
            assert ks[0] == 0 and ks[-1] == 1, f"{name}: keyTimes must start 0 and end 1"
        for m in re.finditer(r'values="([^"]+)"', back):
            assert ';' in m.group(1), f"{name}: single-keyframe values"
        print(f"  ✓ {p:34s} {len(back):>6} bytes · "
              f"{back.count('<text')} text · {back.count('<animate')} SMIL")
    print("all assets regenerated and read back")
