#!/usr/bin/env python3
"""إعادة حزم APK بعد تصليب DEX — بضغطٍ يرضي المنصّة لا بأيّ ضغط.

**لماذا هذه الأداة:** بعد أن يُصلَّب `classes*.dex` (بـ`bla-agents` أو أي أداة تكتب في
ملفّات DEX) لا بدّ من لمّ الحزمة من جديد ثم توقيعها؛ و`zip` وحده **لا يعرف** شرطًا واحدًا
يسقط التثبيت أو التشغيل بلا رسالة مفهومة:

1. **`resources.arsc` يجب أن يُخزَّن غير مضغوط** (`STORED`) ومحاذًى على ٤ بايتات. فمن
   `Android 11+` (وقبلها في `AAPT2`) يُقرأ بذاكرة مخطَّطة (`mmap`)، وملفّ مضغوط يجعله
   `Resources$NotFoundException` أو فشل تثبيت. و`zipalign` يُصلح المحاذاة ولا يُصلح الضغط.
2. **`AndroidManifest.xml`** يجوز ضغطه، لكنّ تخزينه يقلّل احتمال اختلاف سلوك القارئات
   القديمة بلا كلفة تُذكر — فيُخزَّن هو أيضًا.
3. **توقيعات `META-INF/*.SF|*.RSA|*.DSA` القديمة تُحذف**: بعد تعديل DEX صارت باطلة،
   وإبقاؤها يجعل `apksigner verify` يرفض الحزمة أو — أسوأ — يُثبِّت المنصّة نسخةً بتوقيع
   v1 قديم لا يطابق المحتوى.

**وحدّها:** تُعيد الحزم ولا تُوقّع (التوقيع يحتاج keystore وسرًّا، ولا يُكتب سرّ في أداة).
والخطوة التالية في CI هي `zipalign` ثم `apksigner sign`.

الاستعمال:
    python3 tools/apk_repack.py <مجلّد-مفكوك> <ملف-خارج.apk>
    python3 tools/apk_repack.py --self-test
"""

from __future__ import annotations

import argparse
import os
import sys
import tempfile
import zipfile

# ما يُخزَّن بلا ضغط: `resources.arsc` شرط منصّة، والبيان تخزينًا احتياطيًّا بلا كلفة.
STORED_NAMES = {"resources.arsc", "AndroidManifest.xml"}

# توقيعات v1 القديمة: تُحذف دائمًا — صارت باطلة بعد تعديل DEX، وإبقاؤها يفسد التحقّق.
STALE_SIGNATURE_PREFIX = "META-INF/"
STALE_SIGNATURE_SUFFIXES = (".SF", ".RSA", ".DSA", ".EC")


def should_skip(rel: str) -> bool:
    upper = rel.upper()
    return upper.startswith(STALE_SIGNATURE_PREFIX) and upper.endswith(STALE_SIGNATURE_SUFFIXES)


def repack(source_dir: str, out_path: str) -> int:
    """يلمّ المجلّد في APK واحد، ويُعيد عدد المداخل المكتوبة."""
    entries: list[str] = []
    for root, _dirs, files in os.walk(source_dir):
        for name in files:
            full = os.path.join(root, name)
            rel = os.path.relpath(full, source_dir).replace(os.sep, "/")
            if should_skip(rel):
                continue
            entries.append(rel)
    # ترتيب ثابت: حزمة متطابقة البايتات من شجرة متطابقة — يُقارَن ناتج خطوتين.
    entries.sort(key=lambda item: (item != "AndroidManifest.xml", item))

    written = 0
    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for rel in entries:
            full = os.path.join(source_dir, rel)
            with open(full, "rb") as handle:
                data = handle.read()
            info = zipfile.ZipInfo(rel, date_time=(2026, 1, 1, 0, 0, 0))
            info.external_attr = 0o644 << 16
            if rel in STORED_NAMES:
                info.compress_type = zipfile.ZIP_STORED
                archive.writestr(info, data)
            else:
                info.compress_type = zipfile.ZIP_DEFLATED
                archive.writestr(info, data, compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
            written += 1
    return written


def self_test() -> int:
    """يبني حزمة صغيرة ويقيس الشروط الثلاثة على الناتج نفسه."""
    failures: list[str] = []

    def expect(name: str, ok: bool) -> None:
        if not ok:
            failures.append(name)

    with tempfile.TemporaryDirectory() as work:
        source = os.path.join(work, "in")
        os.makedirs(os.path.join(source, "META-INF"))
        os.makedirs(os.path.join(source, "lib", "arm64-v8a"))
        with open(os.path.join(source, "AndroidManifest.xml"), "wb") as handle:
            handle.write(b"\x03\x00\x08\x00" + b"M" * 200)
        with open(os.path.join(source, "resources.arsc"), "wb") as handle:
            handle.write(b"\x02\x00\x0c\x00" + b"R" * 4000)
        with open(os.path.join(source, "classes.dex"), "wb") as handle:
            handle.write(b"dex\n035\0" + b"D" * 3000)
        with open(os.path.join(source, "lib", "arm64-v8a", "libx.so"), "wb") as handle:
            handle.write(b"\x7fELF" + b"E" * 500)
        with open(os.path.join(source, "META-INF", "CERT.SF"), "wb") as handle:
            handle.write(b"Signature-Version: 1.0\n")
        with open(os.path.join(source, "META-INF", "CERT.RSA"), "wb") as handle:
            handle.write(b"\x30\x82")

        out = os.path.join(work, "out.apk")
        count = repack(source, out)

        with zipfile.ZipFile(out) as archive:
            names = archive.namelist()
            infos = {info.filename: info for info in archive.infolist()}
            expect("resources.arsc مُخزَّن", infos["resources.arsc"].compress_type == zipfile.ZIP_STORED)
            expect("البيان مُخزَّن", infos["AndroidManifest.xml"].compress_type == zipfile.ZIP_STORED)
            expect("dex مضغوط", infos["classes.dex"].compress_type == zipfile.ZIP_DEFLATED)
            expect("المكتبة مضغوطة", infos["lib/arm64-v8a/libx.so"].compress_type == zipfile.ZIP_DEFLATED)
            expect("التوقيع القديم محذوف", "META-INF/CERT.SF" not in names and "META-INF/CERT.RSA" not in names)
            expect("البيان أولًا", names[0] == "AndroidManifest.xml")
            expect("العضوية كاملة", set(names) == {
                "AndroidManifest.xml", "resources.arsc", "classes.dex", "lib/arm64-v8a/libx.so",
            })
            expect("المحتوى سليم", archive.read("classes.dex").startswith(b"dex\n035\0"))
        expect("عدد المكتوب", count == 4)

        # والمحتوى المضغوط يُقرأ كما هو: الحزمة ليست مجرّد أسماء.
        with zipfile.ZipFile(out) as archive:
            expect("قراءة arsc", len(archive.read("resources.arsc")) == 4004)

    if failures:
        print("✗ الأداة نفسها معطوبة:")
        for failure in failures:
            print("  - " + failure)
        return 1
    print("✓ قياس الأداة: ١٠ حالات معروفة، كلها بالحكم المتوقّع")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="إعادة حزم APK بعد تصليب DEX")
    parser.add_argument("source", nargs="?", help="مجلّد الحزمة المفكوكة")
    parser.add_argument("output", nargs="?", help="ملفّ APK الناتج")
    parser.add_argument("--self-test", dest="self_test", action="store_true", help="يقيس الأداة نفسها")
    args = parser.parse_args()

    if args.self_test:
        return self_test()
    if not args.source or not args.output:
        parser.error("يلزم مجلّد المصدر وملفّ الناتج (أو --self-test)")
    if not os.path.isdir(args.source):
        print(f"✗ مجلّد غير موجود: {args.source}", file=sys.stderr)
        return 1
    if not os.path.isfile(os.path.join(args.source, "AndroidManifest.xml")):
        print(f"✗ لا `AndroidManifest.xml` في {args.source} — ليس حزمة مفكوكة", file=sys.stderr)
        return 1

    written = repack(args.source, args.output)
    size = os.path.getsize(args.output)
    print(f"✓ أُعيدت الحزمة: {written} مدخلًا · {size} بايت · {args.output}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
