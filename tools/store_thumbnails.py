#!/usr/bin/env python3
"""الصور المصغّرة للمتجر — تُبنى من لقطات حقيقية، وتُقاس ولا تُدَّعى.

لماذا وُجد هذا الملف
--------------------
ملاحظة التقييم (البند ③): «الصور المصغّرة اعتمد تصميمًا خاصًّا بالتطبيق بدلًا من أدوات توليد الصور»،
وقرار المالك (ADR-73): **لا يدخل المستودع ملفّ من مولّد صور**. فالبديل هنا ليس بديلًا بالكلام: كل
صورة في `docs/store/` تُبنى من ثلاثة مصادر يملكها المشروع —

  ① **لقطة شاشة حقيقية** من `docs/screenshots/` (بالأبعاد المعلَنة، وتُقاس عند كل فحص)،
  ② **لوحة ألوان التطبيق** نفسها: `DARK` في `tools/gen_readme_assets.py` حرفيًّا — وهي مقيسة بدورها
     على ثوابت `MaxTokens.kt` (`PositiveOnDark` · `PositiveOnLight` هما لونا الوسم)،
  ③ **وسم الصانع** من هندسة `tools/makers_mark.py` الواحدة (نفس الأرقام التي يرسمها التطبيق).

والخطوط **ليست** من عندنا: هي خطوط التطبيق نفسها (`Space Grotesk` عرضًا · `Manrope` متنًا ·
`JetBrains Mono` للقيم — `Fonts.kt`)، وهي تُنزَّل على الجهاز ولا تُشحن في المستودع، فتُثبَّت هنا
**خارجه** على عُرف `tessdata`/`EasyOCR`. والعربية ترسم بخطّ النظام (`Noto`)، لأن الخطّين
اللاتينيّين بلا محارف عربية — وهو ما يراه المستخدم العربي في التطبيق فعلًا، لا تقليد له.

الأوامر
-------
    python3 tools/store_thumbnails.py --generate      # يبني الصور + manifest.json + listing.md (يحتاج Pillow)
    python3 tools/store_thumbnails.py --reproduce     # يعيد التوليد في مجلد مؤقّت ويقارن البايتات (الحتمية)
    python3 tools/store_thumbnails.py --assert        # يقيس كل شيء مقابل العقد (stdlib فقط — يعمل في CI)
    python3 tools/store_thumbnails.py --self-test     # يكسر المدخلات عمدًا ويتوقّع أن الفحص يكشفها
    python3 tools/store_thumbnails.py --json          # تقرير مفصّل

وما يقيسه `--assert` بلا Pillow (وهذا هو سبب كونه stdlib): أن الصور المولّدة لم تنزح عن `spec.json`
(بصمة البايتات)، وأن اللوحة لم تنزح عن لوحة المستودع، وأن هندسة الوسم لم تتغيّر، وأن كل ادّعاء
في الشرائح **منشور فعلًا** في `README.md`/`README.ar.md`، وأن الأرقام في نصّ المتجر مطابقة لما
في الشجرة، وأن `listing.md` هو حرفيًّا ما يولّده العقد. وأما **جمال الصورة** فلا يقيسه شيء —
يُترك لمن يفتحها، ويُقال ذلك في `docs/store/README.md` ولا يُدَّعى غيره.
"""
from __future__ import annotations

import argparse
import ast
import hashlib
import json
import os
import re
import shutil
import struct
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DEFAULT_STORE = os.path.join(ROOT, "docs", "store")

BOARD = os.path.join(HERE, "gen_readme_assets.py")
TOKENS = os.path.join(
    ROOT, "manager", "app", "src", "main", "java", "nd", "max", "ui", "design", "MaxTokens.kt"
)
DESTINATIONS = os.path.join(
    ROOT, "manager", "app", "src", "main", "java", "nd", "max", "ui", "navigation",
    "MaxDestinations.kt",
)
RES_DIR = os.path.join(ROOT, "manager", "app", "src", "main", "res")
READMES = {"en": os.path.join(ROOT, "README.md"), "ar": os.path.join(ROOT, "README.ar.md")}

LANGS = ("en", "ar")
FONT_ROLES = ("display", "body", "mono", "ar", "arBody")
FONT_DIRS = tuple(
    d for d in (
        os.environ.get("MAXMANAGER_FONTS", ""),
        os.path.expanduser("~/.local/share/fonts/maxmanager-store"),
        "/usr/share/fonts/truetype/noto",
        "/usr/share/fonts/truetype",
    ) if d
)


# ── قياسات المستودع (لا تُخمَّن: تُقرأ من مصادرها) ────────────────────────────

def _read(path: str) -> str:
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def board_dark() -> dict | None:
    """لوحة `DARK` من `gen_readme_assets.py` — تُقرأ بـ`ast` فلا يُنفَّذ الملفّ.

    واللوحة مكتوبة `DARK = dict(bg=…, …)` — نداء `dict` بمفاتيح، لا قاموسًا حرفيًّا؛ فالقراءة
    تستخرج المفاتيح من النداء نفسه (وقاموسٌ حرفيّ يُقرأ كما هو، فلا تنكسر الأداة إن تغيّر الشكل).
    """
    try:
        tree = ast.parse(_read(BOARD))
    except (OSError, SyntaxError):
        return None
    for node in tree.body:
        if not isinstance(node, ast.Assign):
            continue
        if not any(isinstance(t, ast.Name) and t.id == "DARK" for t in node.targets):
            continue
        value = node.value
        if isinstance(value, ast.Call) and getattr(value.func, "id", "") == "dict" and not value.args:
            out = {}
            for keyword in value.keywords:
                if keyword.arg is None:
                    return None
                try:
                    out[keyword.arg] = ast.literal_eval(keyword.value)
                except ValueError:
                    return None
            return out
        try:
            return ast.literal_eval(value)
        except ValueError:
            return None
    return None


def tokens_positive() -> dict:
    """لونا الوسم من `MaxTokens.kt` — المصدر الذي تقرؤه رسمة التطبيق أيضًا."""
    text = _read(TOKENS)
    found = {}
    for name in ("PositiveOnDark", "PositiveOnLight"):
        match = re.search(r"val %s = Color\(0x([0-9A-Fa-f]{8})\)" % name, text)
        if match:
            found[name] = "#" + match.group(1)[2:].upper()
    return found


def mark_module():
    """هندسة الوسم الواحدة — تُستورد ولا تُنسخ."""
    if HERE not in sys.path:
        sys.path.insert(0, HERE)
    import makers_mark  # noqa: E402  (يُستورد بعد ضبط المسار)
    return makers_mark


def mark_fingerprint() -> dict:
    mark = mark_module()
    raw = ",".join("%g" % v for v in mark.SEGMENTS).encode("utf-8")
    return {
        "view": float(mark.VIEW),
        "stroke": float(mark.STROKE_W),
        "segments_sha256": hashlib.sha256(raw).hexdigest(),
    }


def measure_tree() -> dict:
    """الأرقام التي يجوز أن تظهر في نصّ المتجر — تُقاس على الشجرة، لا تُكتب بيد."""
    destinations = _read(DESTINATIONS)
    screens = len(re.findall(r"data object \w+ : MaxDestination", destinations))
    locales = len([d for d in os.listdir(RES_DIR) if d.startswith("values-")])
    return {"screens": screens, "locales": locales}


# ── ملفّات ───────────────────────────────────────────────────────────────────

def png_size(path: str) -> tuple[int, int] | None:
    """أبعاد PNG من ترويسته وحدها — بلا Pillow."""
    try:
        with open(path, "rb") as fh:
            head = fh.read(33)
    except OSError:
        return None
    if len(head) < 33 or head[:8] != b"\x89PNG\r\n\x1a\n" or head[12:16] != b"IHDR":
        return None
    return struct.unpack(">II", head[16:24])


def sha256_file(path: str) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 16), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical(obj) -> bytes:
    return json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


# ── العقد: ما يجب أن يوجد ────────────────────────────────────────────────────

def image_names(spec: dict) -> list[tuple[str, str, str, dict]]:
    """(اسم الملفّ · النوع · اللغة · مصدره من العقد) بترتيب حتميّ."""
    out: list[tuple[str, str, str, dict]] = []
    for card in spec.get("cards", []):
        for lang in LANGS:
            out.append((f"card-{card['id']}-{lang}.png", "phone", lang, card))
    for lang in LANGS:
        out.append((f"feature-{lang}.png", "feature", lang, spec.get("feature", {})))
    return out


def resolve_font(spec: dict, role: str, override: str | None) -> tuple[str | None, list[str]]:
    """يبحث عن ملفّ الخطّ في المجلّدات المُعلَنة. يُعيد (المسار أو None، ما جُرِّب)."""
    tried: list[str] = []
    name = spec["fonts"][role]["file"]
    if override:
        tried.append(override)
        return (override if os.path.isfile(override) else None), tried
    for folder in FONT_DIRS:
        candidate = os.path.join(folder, name)
        tried.append(candidate)
        if os.path.isfile(candidate):
            return candidate, tried
    return None, tried


def listing_text(spec: dict, measured: dict) -> str:
    """نصّ المتجر — مولَّد من العقد، فالعقد والوصف لا يفترقان."""
    listing = spec["listing"]
    lines: list[str] = []
    lines.append("# Store listing — %s" % listing["title"])
    lines.append("")
    lines.append("> مُولَّد من `docs/store/spec.json` بـ`python3 tools/store_thumbnails.py --generate` —")
    lines.append("> لا يُحرَّر بيد: أي تعديل هنا يُحمرّ `store_thumbnails.py --assert`.")
    lines.append("")
    for lang, heading in (("en", "English"), ("ar", "العربية")):
        title = listing["title"]
        short = listing["short"][lang]
        full = [p.format(**measured) for p in listing["full"][lang]]
        lines.append("## %s" % heading)
        lines.append("")
        lines.append("**Title:** %s" % title if lang == "en" else "**العنوان:** %s" % title)
        lines.append("")
        label = "Short description" if lang == "en" else "الوصف القصير"
        lines.append("**%s** (%d chars)" % (label, len(short)))
        lines.append("")
        lines.append("> %s" % short)
        lines.append("")
        lines.append("**Full description**" if lang == "en" else "**الوصف الكامل**")
        lines.append("")
        for paragraph in full:
            lines.append(paragraph)
            lines.append("")
    return "\n".join(lines).rstrip("\n") + "\n"


# ── الفحص (بلا Pillow) ───────────────────────────────────────────────────────

def check(store_dir: str, problems: list[str], notes: list[str]) -> None:
    spec_path = os.path.join(store_dir, "spec.json")
    manifest_path = os.path.join(store_dir, "manifest.json")
    listing_path = os.path.join(store_dir, "listing.md")

    try:
        spec = json.loads(_read(spec_path))
    except (OSError, json.JSONDecodeError) as exc:
        problems.append("spec.json لا يُقرأ: %s" % exc)
        return

    manifest = None
    if os.path.isfile(manifest_path):
        try:
            manifest = json.loads(_read(manifest_path))
        except json.JSONDecodeError as exc:
            problems.append("manifest.json لا يُقرأ: %s" % exc)

    # ① العقد نفسه مكتمل
    phone = spec.get("canvas", {}).get("phone")
    feature = spec.get("canvas", {}).get("feature")
    if not (isinstance(phone, list) and len(phone) == 2):
        problems.append("canvas.phone غير معلَن")
    if not (isinstance(feature, list) and len(feature) == 2):
        problems.append("canvas.feature غير معلَن")
    for card in spec.get("cards", []):
        for field in ("headline", "sub"):
            for lang in LANGS:
                if not str(card.get(field, {}).get(lang, "")).strip():
                    problems.append("البطاقة %s: %s.%s فارغ" % (card.get("id"), field, lang))
    for lang in LANGS:
        if not str(spec.get("feature", {}).get("headline", {}).get(lang, "")).strip():
            problems.append("feature.headline.%s فارغ" % lang)
    for lang in LANGS:
        if not str(spec.get("listing", {}).get("short", {}).get(lang, "")).strip():
            problems.append("listing.short.%s فارغ" % lang)
        if not spec.get("listing", {}).get("full", {}).get(lang):
            problems.append("listing.full.%s فارغ" % lang)
    if not spec.get("claims"):
        problems.append("claims فارغة — الشرائح تُنشر ولا تُخترع")

    # ② اللقطات المصدر: موجودة وبالأبعاد المعلَنة
    for entry in list(spec.get("cards", [])) + [spec.get("feature", {})]:
        source = entry.get("source")
        if not source:
            problems.append("مدخل بلا مصدر: %s" % entry.get("id"))
            continue
        size = png_size(os.path.join(ROOT, source))
        if size is None:
            problems.append("لقطة المصدر غائبة أو ليست PNG: %s" % source)
        elif list(size) != list(entry.get("source_size", [])):
            problems.append(
                "لقطة المصدر %s بأبعاد %s والمعلَن %s" % (source, list(size), entry.get("source_size"))
            )

    # ③ الصور المولّدة مقابل البيان
    expected = {name: (kind, lang, entry) for name, kind, lang, entry in image_names(spec)}
    listed = {}
    if manifest is not None:
        for image in manifest.get("images", []):
            listed[image.get("file")] = image
        for name, image in listed.items():
            if name not in expected:
                problems.append("صورة في البيان لا يطلبها العقد: %s" % name)
        for name in expected:
            if name not in listed:
                problems.append("صورة يطلبها العقد وغائبة عن البيان: %s" % name)
        for name, (kind, _lang, entry) in expected.items():
            image = listed.get(name)
            path = os.path.join(store_dir, name)
            size = png_size(path)
            if size is None:
                problems.append("الصورة المولّدة غائبة أو ليست PNG: %s" % name)
                continue
            want = phone if kind == "phone" else feature
            if list(size) != list(want):
                problems.append("%s بأبعاد %s والمطلوب %s" % (name, list(size), list(want)))
            if image:
                if [image.get("width"), image.get("height")] != list(size):
                    problems.append("%s: أبعاد البيان تخالف الملفّ" % name)
                if image.get("sha256") != sha256_file(path):
                    problems.append("%s: البصمة تخالف البيان — أُعيد التوليد؟" % name)
                if image.get("bytes") != os.path.getsize(path):
                    problems.append("%s: الحجم يخالف البيان" % name)
                if image.get("source") != entry.get("source"):
                    problems.append("%s: المصدر في البيان يخالف العقد" % name)
        on_disk = {f for f in os.listdir(store_dir) if f.endswith(".png")}
        for orphan in sorted(on_disk - set(listed)):
            problems.append("صورة على القرص لا يعلنها البيان: %s" % orphan)

    # ④ اللوحة: لوحة المستودع حرفيًّا، ولونا الوسم من MaxTokens
    board = board_dark()
    if board is None:
        problems.append("لوحة gen_readme_assets.DARK لا تُقرأ")
    else:
        declared = spec.get("palette", {}).get("board_dark", {})
        for key, value in declared.items():
            if board.get(key) != value:
                problems.append("اللوحة تنزاح: %s=%s والمعلَن %s" % (key, board.get(key), value))
        if manifest is not None:
            want = sha256_bytes(canonical(board))
            got = manifest.get("palette", {}).get("board_dark_sha256")
            if got != want:
                problems.append("بصمة اللوحة في البيان تخالف لوحة المستودع — أُعيد التوليد؟")
    positive = tokens_positive()
    want_mark = {
        "PositiveOnDark": spec.get("palette", {}).get("mark_dark"),
        "PositiveOnLight": spec.get("palette", {}).get("mark_light"),
    }
    for name, value in want_mark.items():
        if positive.get(name) != value:
            problems.append("لون الوسم %s=%s والمعلَن %s" % (name, positive.get(name), value))

    # ⑤ هندسة الوسم لم تتغيّر
    if manifest is not None:
        current = mark_fingerprint()
        recorded = manifest.get("mark", {})
        for key in ("view", "stroke", "segments_sha256"):
            if recorded.get(key) != current.get(key):
                problems.append("هندسة الوسم تنزاح (%s) — أُعيد التوليد؟" % key)

    # ⑥ الادّعاءات منشورة في الصفحتين
    readme_en = _read(READMES["en"]).lower()
    readme_ar = _read(READMES["ar"])
    for claim in spec.get("claims", []):
        anchor_en = str(claim.get("anchor_en", "")).lower()
        anchor_ar = str(claim.get("anchor_ar", ""))
        if anchor_en and anchor_en not in readme_en:
            problems.append("ادّعاء غير منشور في README.md: %s" % claim.get("en"))
        if anchor_ar and anchor_ar not in readme_ar:
            problems.append("ادّعاء غير منشور في README.ar.md: %s" % claim.get("ar"))

    # ⑦ الخطوط: تُقاس إن كانت مثبَّتة هنا، وتُقال إن غابت (لا تُدَّعى)
    for role in FONT_ROLES:
        declared = spec.get("fonts", {}).get(role, {})
        recorded = (manifest or {}).get("fonts", {}).get(role, {})
        if recorded.get("file") != declared.get("file"):
            problems.append("الخطّ %s في البيان يخالف العقد" % role)
        path, tried = resolve_font(spec, role, None)
        if path is None:
            notes.append("الخطّ %s غير مثبَّت محليًّا (%s) — البصمة المسجّلة لم تُعَد قياسها" % (role, declared.get("file")))
        elif recorded.get("sha256") and sha256_file(path) != recorded["sha256"]:
            problems.append("الخطّ %s تغيّر عن البصمة المسجّلة" % role)

    # ⑧ الأرقام: العقد يطابق الشجرة
    measured = measure_tree()
    declared_measured = {k: v for k, v in spec.get("measured", {}).items() if not k.startswith("_")}
    for key, value in declared_measured.items():
        if measured.get(key) != value:
            problems.append("الرقم المعلَن %s=%s والمقيس %s" % (key, value, measured.get(key)))

    # ⑨ نصّ المتجر هو حرفيًّا ما يولّده العقد
    if os.path.isfile(listing_path):
        want = listing_text(spec, measured)
        if _read(listing_path) != want:
            problems.append("listing.md يخالف ما يولّده العقد — أُعيد التوليد؟")
    else:
        problems.append("listing.md غائب")

    # ⑩ بصمة العقد: أي تعديل في spec يوجب إعادة التوليد
    if manifest is not None and manifest.get("spec_sha256") != sha256_file(spec_path):
        problems.append("بصمة spec.json تخالف البيان — أُعيد التوليد؟")


# ── التوليد (يحتاج Pillow + الخطوط) ──────────────────────────────────────────

def _hex(color: str) -> tuple[int, int, int]:
    return tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))


def _lerp(a, b, t: float):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _gradient(size: tuple[int, int], top: str, bottom: str):
    from PIL import Image
    strip = Image.new("RGB", (1, size[1]))
    top_rgb, bottom_rgb = _hex(top), _hex(bottom)
    for y in range(size[1]):
        strip.putpixel((0, y), _lerp(top_rgb, bottom_rgb, y / max(1, size[1] - 1)))
    return strip.resize(size, Image.BILINEAR)


def _glow(size: tuple[int, int], color: str, box: tuple[int, int, int, int], strength: int = 30):
    """هالة ناعمة: دوائر متراكبة ثم تمويه — عمق بلا ضجيج."""
    from PIL import Image, ImageDraw, ImageFilter
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    rgb = _hex(color)
    steps = 42
    x0, y0, x1, y1 = box
    for i in range(steps, 0, -1):
        alpha = round(strength * (1 - i / steps) ** 2)
        if alpha <= 0:
            continue
        grow = round((x1 - x0) * 0.5 * i / steps)
        grow_y = round((y1 - y0) * 0.5 * i / steps)
        draw.ellipse([x0 - grow, y0 - grow_y, x1 + grow, y1 + grow_y], fill=rgb + (alpha,))
    return layer.filter(ImageFilter.GaussianBlur(28))


def _font(path: str, size: int, weight: int):
    from PIL import ImageFont
    font = ImageFont.truetype(path, size)
    try:
        font.set_variation_by_axes([weight])
    except Exception:  # خطّ ثابت بلا محاور — لا يُشترط
        pass
    return font


def _wrap(draw, text: str, font, max_width: float, lang: str) -> list[str]:
    direction = "rtl" if lang == "ar" else "ltr"
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        probe = (current + " " + word).strip()
        if current and draw.textlength(probe, font=font, direction=direction) > max_width:
            lines.append(current)
            current = word
        else:
            current = probe
    if current:
        lines.append(current)
    return lines


def _text(draw, xy, text: str, font, fill, anchor: str, lang: str) -> None:
    draw.text(
        xy, text, font=font, fill=fill, anchor=anchor,
        direction="rtl" if lang == "ar" else "ltr",
        language="ar" if lang == "ar" else "en",
    )


def _mark_polyline(segments: list[float], samples: int = 28) -> list[tuple[float, float]]:
    points = [(segments[0], segments[1])]
    i = 2
    while i + 5 < len(segments):
        p0 = points[-1]
        c1 = (segments[i], segments[i + 1])
        c2 = (segments[i + 2], segments[i + 3])
        p1 = (segments[i + 4], segments[i + 5])
        for step in range(1, samples + 1):
            t = step / samples
            u = 1 - t
            x = u * u * u * p0[0] + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t * t * t * p1[0]
            y = u * u * u * p0[1] + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t * t * t * p1[1]
            points.append((x, y))
        i += 6
    return points


def _draw_mark(draw, mark, x: int, y: int, size: int, color: str) -> None:
    scale = size / float(mark.VIEW)
    pen = max(2, round(float(mark.STROKE_W) * scale))
    points = [(x + px * scale, y + py * scale) for px, py in _mark_polyline(mark.SEGMENTS)]
    draw.line(points, fill=_hex(color), width=pen, joint="curve")
    radius = pen / 2
    for cx, cy in (points[0], points[-1]):
        draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius], fill=_hex(color))


def _panel(img, source_path: str, box: tuple[int, int, int, int], palette: dict, shadow: bool):
    """لقطة حقيقية كما هي — بإطار جهازها، بلا إطار ثانٍ نُضيفه فوق إطارها.

    اللقطات في `docs/screenshots/` تحمل إطار الجهاز (حوافّ سوداء وزوايا مستديرة) مقيسًا عند
    الالتقاط، وإضافة زوايا أو حدّ من عندنا تُنتج **إطارًا مزدوجًا** يفضح أنّ الصورة مُركَّبة. فالوسم
    هنا ظلٌّ ناعم فقط، والصورة تبقى هي هي.
    """
    from PIL import Image, ImageDraw, ImageFilter
    target_w = box[2] - box[0]
    shot = Image.open(source_path).convert("RGB")
    scale = target_w / shot.width
    shot = shot.resize((target_w, max(1, round(shot.height * scale))), Image.LANCZOS)
    shot = shot.crop((0, 0, target_w, min(shot.height, box[3] - box[1])))
    if shadow:
        layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).rectangle(
            [box[0] - 4, box[1] + 16, box[0] + target_w + 4, box[1] + shot.height + 22],
            fill=(0, 0, 0, 140),
        )
        img.alpha_composite(layer.filter(ImageFilter.GaussianBlur(28)))
    img.alpha_composite(shot.convert("RGBA"), (box[0], box[1]))


def _chips(draw, labels: list[str], font, palette: dict, center_x: int, y: int, lang: str) -> int:
    """شرائح الادّعاء — نصوصها من العقد، وهي منشورة في README (يقيسه الفحص)."""
    pad_x, height, gap = 30, 60, 18
    widths = [round(draw.textlength(text, font=font, direction="rtl" if lang == "ar" else "ltr")) + 2 * pad_x
              for text in labels]
    total = sum(widths) + gap * (len(labels) - 1)
    x = center_x - total // 2
    for text, width in zip(labels, widths):
        draw.rounded_rectangle([x, y, x + width, y + height], radius=height // 2,
                               fill=_hex(palette["panel"]), outline=_hex(palette["edge"]), width=1)
        _text(draw, (x + width / 2, y + height / 2), text, font, _hex(palette["steel"]), "mm", lang)
        x += width + gap
    return total


def draw_phone(spec: dict, card: dict, lang: str, fonts: dict, mark, palette: dict):
    from PIL import Image, ImageDraw
    width, height = spec["canvas"]["phone"]
    img = _gradient((width, height), palette["panelTop"], palette["bg"]).convert("RGBA")
    display = _font(fonts["display"], 46, spec["fonts"]["display"]["weight"])
    headline_font = _font(fonts["ar"] if lang == "ar" else fonts["display"], 62,
                          spec["fonts"]["ar" if lang == "ar" else "display"]["weight"])
    sub_font = _font(fonts["arBody"] if lang == "ar" else fonts["body"], 38,
                     spec["fonts"]["arBody" if lang == "ar" else "body"]["weight"])
    chip_font = _font(fonts["arBody"] if lang == "ar" else fonts["mono"], 28,
                      spec["fonts"]["arBody" if lang == "ar" else "mono"]["weight"])
    draw = ImageDraw.Draw(img)

    # ① الوسم والاسم: هالة ناعمة ثم الوسم (هندسة الأداة الواحدة) ثم الاسم.
    word = "MaxManager"
    word_w = draw.textlength(word, font=display)
    group_w = 96 + 20 + word_w
    x0 = (width - group_w) / 2
    img.alpha_composite(_glow((width, height), palette["accent"], (x0, 96, x0 + 96, 192), 34))
    _draw_mark(draw, mark, round(x0), 96, 96, palette["accent"])
    _text(draw, (x0 + 96 + 20, 144), word, display, _hex(palette["ink"]), "lm", "en")

    # ② العنوان ثم السطر المساند.
    y = 300
    for line in _wrap(draw, card["headline"][lang], headline_font, 900, lang):
        _text(draw, (width / 2, y), line, headline_font, _hex(palette["ink"]), "ma", lang)
        y += round(62 * 1.24)
    y += 14
    for line in _wrap(draw, card["sub"][lang], sub_font, 880, lang):
        _text(draw, (width / 2, y), line, sub_font, _hex(palette["muted"]), "ma", lang)
        y += round(38 * 1.34)
    y += 26

    # ③ شرائح الادّعاء — ثم اللقطة تحتها بلا تلامس.
    _chips(draw, [claim[lang] for claim in spec["claims"]], chip_font, palette, width // 2, y, lang)
    shot_top = max(y + 60 + 46, 700)
    _panel(img, os.path.join(ROOT, card["source"]),
           (100, shot_top, 100 + 880, height), palette, shadow=True)
    return img.convert("RGB")


def draw_feature(spec: dict, lang: str, fonts: dict, mark, palette: dict):
    from PIL import Image, ImageDraw
    width, height = spec["canvas"]["feature"]
    rtl = lang == "ar"
    img = _gradient((width, height), palette["panelTop"], palette["bg"]).convert("RGBA")
    display = _font(fonts["display"], 40, spec["fonts"]["display"]["weight"])
    headline_font = _font(fonts["ar"] if rtl else fonts["display"], 44,
                          spec["fonts"]["ar" if rtl else "display"]["weight"])
    claims_font = _font(fonts["arBody"] if rtl else fonts["mono"], 21,
                        spec["fonts"]["arBody" if rtl else "mono"]["weight"])
    draw = ImageDraw.Draw(img)

    # ① اللقطة في الطرف المقابل للاتجاه: يمين في الإنجليزية، يسار في العربية (RTL حقيقي لا مقلوب بالحيلة).
    panel_w = 420
    panel_x = width - 384 if not rtl else -36
    _panel(img, os.path.join(ROOT, spec["feature"]["source"]),
           (panel_x, 64, panel_x + panel_w, height), palette, shadow=True)

    # ② الوسم والاسم، ثم العنوان، ثم الادّعاءات — كلّها على الطرف الآخر.
    text_x = 56 if not rtl else width - 56
    anchor_edge = "la" if not rtl else "ra"
    word = "MaxManager"
    word_w = draw.textlength(word, font=display)
    mark_x = 56 if not rtl else width - 56 - 84
    img.alpha_composite(_glow((width, height), palette["accent"], (mark_x, 58, mark_x + 84, 142), 26))
    _draw_mark(draw, mark, mark_x, 58, 84, palette["accent"])
    if not rtl:
        _text(draw, (56 + 84 + 18, 100), word, display, _hex(palette["ink"]), "lm", "en")
    else:
        _text(draw, (width - 56 - 84 - 18, 100), word, display, _hex(palette["ink"]), "rm", "en")

    y = 230
    for line in _wrap(draw, spec["feature"]["headline"][lang], headline_font, 570, lang):
        _text(draw, (text_x, y), line, headline_font, _hex(palette["ink"]), anchor_edge, lang)
        y += round(44 * 1.26)
    y += 22
    claims = (" · " if not rtl else "، ").join(claim[lang] for claim in spec["claims"])
    for line in _wrap(draw, claims, claims_font, 556, lang):
        _text(draw, (text_x, y), line, claims_font, _hex(palette["steel"]), anchor_edge, lang)
        y += round(21 * 1.45)
    return img.convert("RGB")


def _glyph_signature(font, char: str) -> bytes:
    """بصمة شكل المحرف: يُرسم في مربّع صغير وتُقارن بقعته.

    القياس بلا مكتبة خطوط: المحرف الغائب يُرسم في FreeType شكلَ `.notdef` (مربّعًا) — وهو الشكل
    نفسه لكل محرف غائب في الخطّ نفسه. فبمقارنة كل محرف بمحرف خاصّ (U+E000) **غير موجود حتمًا**
    نعرف الغائب من الحاضر، بلا تخمين.
    """
    from PIL import Image, ImageDraw
    probe = Image.new("L", (96, 96), 0)
    ImageDraw.Draw(probe).text((12, 12), char, font=font, fill=255)
    return probe.tobytes()


def coverage_problems(spec: dict, fonts: dict) -> list[str]:
    """كل نصّ سيُرسم: أيّ محرف بلا شكل في خطّه؟

    وُجدت هذه الدالّة بعد **عيب مقيس**: أوّل توليد رسم شرائح العربية مربّعات فارغة (الخطّ الأحاديّ
    بلا محارف عربية) والنقطة النهائية في Kufi مربّعًا. فصار التوليد يرفض حفظ صورة بمحرف بلا شكل،
    ويقول المحرف والخطّ والنصّ — لأن العيب الذي يُرى بالعين مرّة لا يُترك ليُرى مرّة أخرى.
    """
    checks: list[tuple[str, str, str]] = []
    for card in spec["cards"]:
        for lang in LANGS:
            head = "ar" if lang == "ar" else "display"
            body = "arBody" if lang == "ar" else "body"
            checks.append((head, card["headline"][lang], "عنوان %s/%s" % (card["id"], lang)))
            checks.append((body, card["sub"][lang], "سطر %s/%s" % (card["id"], lang)))
            for claim in spec["claims"]:
                checks.append(("arBody" if lang == "ar" else "mono", claim[lang],
                               "شريحة %s/%s" % (claim["en"], lang)))
    for lang in LANGS:
        head = "ar" if lang == "ar" else "display"
        checks.append((head, spec["feature"]["headline"][lang], "بانر/عنوان %s" % lang))
        checks.append(("arBody" if lang == "ar" else "mono",
                       (" · " if lang == "en" else "، ").join(c[lang] for c in spec["claims"]),
                       "بانر/ادّعاءات %s" % lang))
    checks.append(("display", "MaxManager", "الاسم"))

    problems: list[str] = []
    cache: dict[str, tuple[object, bytes]] = {}
    for role, text, where in checks:
        if role not in cache:
            # حجم مسبار ثابت: غياب المحرف لا يتغيّر بالحجم، والبصمة تحتاج حجمًا واحدًا للمقارنة.
            font = _font(fonts[role], 44, spec["fonts"][role]["weight"])
            cache[role] = (font, _glyph_signature(font, "\ue000"))
        font, absent = cache[role]
        for char in sorted(set(text)):
            if char in " \n\t":
                continue
            if _glyph_signature(font, char) == absent:
                problems.append(
                    "محرف بلا شكل %r في «%s» — الخطّ %s"
                    % (char, where, os.path.basename(fonts[role]))
                )
    return problems


def generate(store_dir: str, spec: dict, overrides: dict) -> list[str]:
    """يبني الصور + البيان + نصّ المتجر. يُعيد قائمة الملاحظات."""
    from PIL import Image  # noqa: F401  (يُستورد هنا كي يعمل --assert بلا Pillow)

    fonts: dict[str, str] = {}
    missing: list[str] = []
    for role in FONT_ROLES:
        path, tried = resolve_font(spec, role, overrides.get(role))
        if path is None:
            missing.append("%s (%s) — جُرِّب: %s" % (role, spec["fonts"][role]["file"], " · ".join(tried[:3])))
        else:
            fonts[role] = path
    if missing:
        raise SystemExit(
            "خطوط ناقصة — لا تُستبدَل بصمت:\n  " + "\n  ".join(missing)
            + "\n  ثبّتها في ~/.local/share/fonts/maxmanager-store أو مرّر --font-<role>."
        )

    glyphs = coverage_problems(spec, fonts)
    if glyphs:
        raise SystemExit(
            "نصوص بمحارف بلا شكل — لا تُحفظ صورة بمربّع فارغ:\n  " + "\n  ".join(glyphs)
        )

    mark = mark_module()
    palette = spec["palette"]["board_dark"]
    notes: list[str] = []

    for card in spec["cards"]:
        for lang in LANGS:
            image = draw_phone(spec, card, lang, fonts, mark, palette)
            image.save(os.path.join(store_dir, "card-%s-%s.png" % (card["id"], lang)), "PNG", optimize=True)
    for lang in LANGS:
        image = draw_feature(spec, lang, fonts, mark, palette)
        image.save(os.path.join(store_dir, "feature-%s.png" % lang), "PNG", optimize=True)

    images = []
    for name, kind, lang, entry in image_names(spec):
        path = os.path.join(store_dir, name)
        size = png_size(path)
        images.append({
            "file": name,
            "kind": kind,
            "lang": lang,
            "id": entry.get("id"),
            "width": size[0],
            "height": size[1],
            "bytes": os.path.getsize(path),
            "sha256": sha256_file(path),
            "source": entry.get("source"),
        })

    board = board_dark() or {}
    manifest = {
        "_": "بيان الصور المصغّرة — بصمات تُقاس في CI. تغيير أيّ مدخل يوجب إعادة التوليد.",
        "tool": "tools/store_thumbnails.py",
        "spec_sha256": sha256_file(os.path.join(store_dir, "spec.json")),
        "palette": {
            "board_dark_sha256": sha256_bytes(canonical(board)),
            "mark_dark": spec["palette"]["mark_dark"],
            "mark_light": spec["palette"]["mark_light"],
        },
        "mark": mark_fingerprint(),
        "fonts": {
            role: {
                "file": spec["fonts"][role]["file"],
                "sha256": sha256_file(fonts[role]),
                "source": spec["fonts"][role]["source"],
            }
            for role in FONT_ROLES
        },
        "measured": measure_tree(),
        "images": images,
    }
    with open(os.path.join(store_dir, "manifest.json"), "w", encoding="utf-8") as fh:
        fh.write(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")

    measured = measure_tree()
    with open(os.path.join(store_dir, "listing.md"), "w", encoding="utf-8") as fh:
        fh.write(listing_text(spec, measured))

    return notes


# ── الحتمية: نفس المدخلات ⇒ نفس البايتات ─────────────────────────────────────

def reproduce(store_dir: str, spec: dict, overrides: dict) -> int:
    with tempfile.TemporaryDirectory() as tmp:
        for name in ("spec.json",):
            shutil.copy2(os.path.join(store_dir, name), os.path.join(tmp, name))
        generate(tmp, spec, overrides)
        mismatched = []
        for name, _kind, _lang, _entry in image_names(spec):
            first = os.path.join(store_dir, name)
            second = os.path.join(tmp, name)
            if not os.path.isfile(first):
                mismatched.append("%s: غير مولَّدة" % name)
            elif sha256_file(first) != sha256_file(second):
                mismatched.append("%s: البايتات تختلف بين تشغيلين" % name)
        for name in ("manifest.json", "listing.md"):
            first = os.path.join(store_dir, name)
            second = os.path.join(tmp, name)
            if os.path.isfile(first) and _read(first) != _read(second):
                mismatched.append("%s: يختلف بين تشغيلين" % name)
        if mismatched:
            print("الحتمية: %d مخالفة" % len(mismatched))
            for item in mismatched:
                print("  ✗ " + item)
            return 1
        print("الحتمية: إعادة التوليد تعطي البايتات نفسها في %d صورة ✓" % len(image_names(spec)))
        return 0


# ── الفحص الذاتي: يكسر المدخلات ويتوقّع أن الفحص يكشفها ──────────────────────

def self_test(store_dir: str) -> int:
    if not os.path.isfile(os.path.join(store_dir, "manifest.json")):
        print("الفحص الذاتي يحتاج صورًا مولَّدة (شغّل --generate أولًا)")
        return 1

    def run(mutate) -> list[str]:
        with tempfile.TemporaryDirectory() as tmp:
            work = os.path.join(tmp, "store")
            shutil.copytree(store_dir, work)
            spec = json.loads(_read(os.path.join(work, "spec.json")))
            manifest = json.loads(_read(os.path.join(work, "manifest.json")))
            mutate(work, spec, manifest)
            problems: list[str] = []
            check(work, problems, [])
            return problems

    def write_json(path, obj):
        with open(path, "w", encoding="utf-8") as fh:
            fh.write(json.dumps(obj, ensure_ascii=False, indent=2) + "\n")

    first_png = image_names(json.loads(_read(os.path.join(store_dir, "spec.json"))))[0][0]

    def case_broken_image(work, _spec, _manifest):
        with open(os.path.join(work, first_png), "r+b") as fh:
            fh.seek(-1, os.SEEK_END)
            fh.write(b"\x00" if fh.read(1) != b"\x00" else b"\x01")

    def case_source_size(work, spec, _manifest):
        spec["cards"][0]["source_size"] = [1, 1]
        write_json(os.path.join(work, "spec.json"), spec)

    def case_claim(work, spec, _manifest):
        spec["claims"][0]["anchor_en"] = "zzz-not-published"
        write_json(os.path.join(work, "spec.json"), spec)

    def case_orphan(work, _spec, _manifest):
        shutil.copy2(os.path.join(work, first_png), os.path.join(work, "extra-orphan.png"))

    def case_mark(work, _spec, manifest):
        manifest["mark"]["segments_sha256"] = "0" * 64
        write_json(os.path.join(work, "manifest.json"), manifest)

    def case_palette(work, spec, _manifest):
        spec["palette"]["board_dark"]["bg"] = "#000000"
        write_json(os.path.join(work, "spec.json"), spec)

    def case_lang(work, spec, _manifest):
        spec["cards"][0]["headline"].pop("ar")
        write_json(os.path.join(work, "spec.json"), spec)

    def case_measured(work, spec, _manifest):
        spec["measured"]["screens"] = 999
        write_json(os.path.join(work, "spec.json"), spec)

    def case_listing(work, _spec, _manifest):
        with open(os.path.join(work, "listing.md"), "a", encoding="utf-8") as fh:
            fh.write("\nسطر أُضيف بيد.\n")

    def case_missing_entry(work, _spec, manifest):
        manifest["images"] = manifest["images"][:-1]
        write_json(os.path.join(work, "manifest.json"), manifest)

    cases = [
        ("صورة عُدِّلت بايتاتها", case_broken_image),
        ("حجم مصدر منزاح", case_source_size),
        ("ادّعاء غير منشور", case_claim),
        ("صورة يتيمة", case_orphan),
        ("هندسة الوسم منزاحة", case_mark),
        ("لوحة منزاحة", case_palette),
        ("نصّ لغة ناقص", case_lang),
        ("رقم معلَن يخالف الشجرة", case_measured),
        ("نصّ المتجر محرَّر بيد", case_listing),
        ("مدخل غائب عن البيان", case_missing_entry),
    ]

    clean_problems: list[str] = []
    clean_notes: list[str] = []
    check(store_dir, clean_problems, clean_notes)
    failures = 0
    if clean_problems:
        failures += 1
        print("✗ المدخلات السليمة أُعلنت معطوبة (%d) — أوّلًا: %s" % (len(clean_problems), clean_problems[0]))
    for name, mutate in cases:
        problems = run(mutate)
        if problems:
            print("✓ %s — كُشف: %s" % (name, problems[0]))
        else:
            failures += 1
            print("✗ %s — لم يُكشف" % name)
    print("الفحص الذاتي: %d/%d" % (len(cases) - failures + (0 if clean_problems else 1), len(cases) + 1))
    return 1 if failures else 0


# ── الواجهة ──────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(description="الصور المصغّرة للمتجر — توليد وفحص")
    parser.add_argument("--generate", action="store_true", help="يبني الصور + البيان + نصّ المتجر")
    parser.add_argument("--reproduce", action="store_true", help="يقيس الحتمية بإعادة التوليد")
    parser.add_argument("--assert", action="store_true", help="يقيس كل شيء مقابل العقد")
    parser.add_argument("--self-test", action="store_true", help="يكسر المدخلات ويتوقّع الكشف")
    parser.add_argument("--json", action="store_true", help="تقرير مفصّل")
    parser.add_argument("--store", default=DEFAULT_STORE, help="مجلّد المتجر (للاختبار)")
    for role in FONT_ROLES:
        parser.add_argument("--font-%s" % role, default=None, help="مسار خطّ %s" % role)
    args = parser.parse_args()

    overrides = {role: getattr(args, "font_%s" % role) for role in FONT_ROLES}
    store_dir = args.store
    spec = json.loads(_read(os.path.join(store_dir, "spec.json")))

    if args.generate:
        notes = generate(store_dir, spec, overrides)
        for note in notes:
            print("· " + note)
        print("وُلِّدت %d صورة + manifest.json + listing.md" % len(image_names(spec)))
        return 0
    if args.reproduce:
        return reproduce(store_dir, spec, overrides)
    if args.self_test:
        return self_test(store_dir)

    problems: list[str] = []
    notes: list[str] = []
    check(store_dir, problems, notes)
    if args.json:
        print(json.dumps({"problems": problems, "notes": notes,
                          "images": len(image_names(spec))}, ensure_ascii=False, indent=2))
    else:
        for note in notes:
            print("· " + note)
        for problem in problems:
            print("✗ " + problem)
        print("الصور المصغّرة: %d صورة · عوائق %d" % (len(image_names(spec)), len(problems)))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
