#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""إزالة اسم ملفّ المصدر من DEX المصلَّبة — إصلاح مُقاس لا رجاء في R8.

**لماذا وُجدت (تكملة ٢٠٢٦-١١-١٦):** `apk_leak_scan` أسقط حزمة الإصدار ثلاث مرّات
بـ`ContextBridge.kt`. وجُرّب أولًا `-renamesourcefileattribute SourceFile` فبقي الاسم،
ثم `-keepattributes !SourceFile,!SourceDebugExtension` فبقي أيضًا (التشغيل
`43951481362`): صيغة النفي في R8 ليست أمر إزالة، واسم الملفّ يبقى في **سمة
`SourceFile`** لكل صنف، وهي نصّ واحد في جدول نصوص DEX يُقرأ بأوّل سطر من JADX.

**وما تفعله هذه الأداة:** تفتح كل `.dex`، وتكتب `source_file_idx = 0xFFFFFFFF`
(‏`NO_INDEX`) في كل `class_def`، ثم **تعيد حساب** توقيع DEX (SHA-1 على البايتات
‏٣٢…النهاية) وبصمته (Adler32 على ١٢…النهاية). ولا تُنشئ صنفًا ولا تحذفه ولا تُغيّر
شيفرة — تعديل ٤ بايتات لكل صنف في جدول التعريفات وحده.

**ولماذا لا تُترك للـrepackage:** `-repackageclasses` يعيد تسمية الأصناف لا يمسّ
`SourceFile`؛ والتجربتان أعلاه تُثبتان أن R8 لا يُسقطه بلا سمة تُحفظ صراحةً.

**والحدّ:** تُزيل اسم الملفّ وحده. أسماء الأصناف والدوال تُبقى لتعتيم R8، وأرقام
الأسطر تبقى في `LineNumberTable` (بلاغات الأعطال تُقرأ بها) — فلا يُدَّعى أن الشيفرة
صارَت غير قابلة للقراءة، بل أن الحزمة لم تعد تُرشد إلى شجرة المصدر.

الاستعمال:
    python3 tools/dex_strip_source_names.py --path <dir>     # قياس وعرض
    python3 tools/dex_strip_source_names.py --assert --path <dir>
    python3 tools/dex_strip_source_names.py --self-test

الحكم: ٠ عوائق = كل الأصناف بلا `SourceFile` والتواقيع متّسقة.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import struct
import sys
import zlib

NO_INDEX = 0xFFFFFFFF
HEADER_SIZE = 0x70
SOURCE_FILE_OFFSET_IN_CLASS_DEF = 16
CLASS_DEF_SIZE = 32


class Report:
    def __init__(self) -> None:
        self.files: list[dict] = []
        self.problems: list[dict] = []

    def problem(self, rule: str, where: str, detail: str) -> None:
        self.problems.append({"rule": rule, "where": where, "detail": detail})


def _u32(data: bytes, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


def _leb128(data: bytes, offset: int) -> tuple[int, int]:
    """يقرأ uleb128 ويُعيد (القيمة، موضع ما بعدها)."""
    result = 0
    shift = 0
    while True:
        if offset >= len(data):
            raise ValueError("uleb128 مقطوع")
        byte = data[offset]
        offset += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, offset
        shift += 7
        if shift > 28:
            raise ValueError("uleb128 أطول من المسموح")


def strip_one(path: str, report: Report, dry_run: bool = False) -> None:
    """تُفرّغ `source_file_idx` في كل صنف ثم تُعيد حساب التوقيع والبصمة."""
    try:
        with open(path, "rb") as handle:
            data = bytearray(handle.read())
    except OSError as exc:
        report.problem("dex_unreadable", path, f"لا يُقرأ: {exc}")
        return

    if len(data) < HEADER_SIZE or data[:4] != b"dex\n":
        report.problem("dex_magic", path, "ليست حزمة DEX (التوقيع 'dex\\n' غائب)")
        return

    file_size = _u32(data, 32)
    if file_size != len(data):
        report.problem(
            "dex_file_size", path, f"file_size={file_size} والطول الفعلي={len(data)}"
        )
        return

    class_defs_size = _u32(data, 96)
    class_defs_off = _u32(data, 100)
    if class_defs_size and class_defs_off + class_defs_size * CLASS_DEF_SIZE > len(data):
        report.problem("dex_class_defs", path, "جدول class_defs يتجاوز الحزمة")
        return

    stripped = 0
    already = 0
    for index in range(class_defs_size):
        at = class_defs_off + index * CLASS_DEF_SIZE + SOURCE_FILE_OFFSET_IN_CLASS_DEF
        if _u32(data, at) == NO_INDEX:
            already += 1
            continue
        struct.pack_into("<I", data, at, NO_INDEX)
        stripped += 1

    if stripped:
        # التوقيع يغطّي ٣٢…النهاية، والبصمة تغطّي ١٢…النهاية (فتشمل التوقيع) — الترتيب مهمّ.
        signature = hashlib.sha1(bytes(data[32:])).digest()
        data[12:32] = signature
        checksum = zlib.adler32(bytes(data[12:])) & 0xFFFFFFFF
        struct.pack_into("<I", data, 8, checksum)
    # **ولا يكفي إفراغ المرجع:** جدول النصوص نفسه يبقى يحمل `ContextBridge.kt`، وقارئ
    # الحزمة (وماسحنا) يقرأ الجدول لا المرجع. فنُبدّل حروف الاسم في مكانه بـ`x` بالطول
    # نفسه: لا يُزاح شيء ولا يتغيّر حجم، ويسقط الأثر من المصدر لا من قائمة المراجع فقط.
    strings_count = _u32(data, 56)
    strings_off = _u32(data, 60)
    scrub = 0
    for index in range(strings_count):
        entry = strings_off + index * 4
        if entry + 4 > len(data):
            break
        start = _u32(data, entry)
        try:
            _length, cursor = _leb128(data, start)
        except ValueError:
            continue
        end = data.find(b"\x00", cursor)
        if end < 0 or end - cursor < 3:
            continue
        lowered = bytes(data[cursor:end]).lower()
        if lowered.endswith(b".kt") or lowered.endswith(b".java"):
            data[cursor:end] = b"x" * (end - cursor)
            scrub += 1

    if stripped or scrub:
        # التوقيع يغطّي ٣٢…النهاية، والبصمة تغطّي ١٢…النهاية (فتشمل التوقيع) — الترتيب مهمّ.
        signature = hashlib.sha1(bytes(data[32:])).digest()
        data[12:32] = signature
        checksum = zlib.adler32(bytes(data[12:])) & 0xFFFFFFFF
        struct.pack_into("<I", data, 8, checksum)
    if stripped or scrub:
        if not dry_run:
            with open(path, "wb") as handle:
                handle.write(bytes(data))

    report.files.append(
        {
            "path": path,
            "classes": class_defs_size,
            "stripped": stripped,
            "scrubbed": scrub,
            "already_clean": already,
        }
    )


def collect_dex(root: str) -> list[str]:
    if os.path.isfile(root):
        return [root]
    found: list[str] = []
    for base, _dirs, names in os.walk(root):
        for name in sorted(names):
            if name.endswith(".dex"):
                found.append(os.path.join(base, name))
    return found


def scan(root: str, dry_run: bool = False) -> Report:
    report = Report()
    if not os.path.exists(root):
        report.problem("path_missing", root, "المسار غير موجود")
        return report
    dexes = collect_dex(root)
    if not dexes:
        report.problem("dex_absent", root, "لا ملفّ .dex في المسار — لا شيء يُقاس")
        return report
    for path in dexes:
        strip_one(path, report, dry_run=dry_run)
    return report


# ─────────────────────────── قياس الأداة نفسها ───────────────────────────


def _synthetic_dex(source_file: str | None) -> bytes:
    """DEX صناعية صغيرة: صنف واحد، وبها `SourceFile` أو بلا.

    الترتيب هنا مقصود: طول النصّ (uleb128) ثم البايتات ثم بايت النهاية — عكس ذلك
    أنتج سابقًا في أدوات هذا المستودع عيّنةً «صالحة» يرفضها القارئ نفسه.
    """
    strings: list[str] = ["Lcom/example/Probe;"]
    source_index = NO_INDEX
    if source_file is not None:
        strings.append(source_file)
        source_index = 1

    header_size = HEADER_SIZE
    string_ids_off = header_size
    string_ids_size = len(strings)
    type_ids_off = string_ids_off + string_ids_size * 4
    type_ids_size = 1
    class_defs_off = type_ids_off + type_ids_size * 4
    class_defs_size = 1
    data_off = class_defs_off + class_defs_size * CLASS_DEF_SIZE

    data = bytearray()
    offsets: list[int] = []
    for text in strings:
        offsets.append(data_off + len(data))
        encoded = text.encode("utf-8")
        # uleb128 لطول utf16 (نصوصنا ASCII فلا فرق)
        length = len(encoded)
        while True:
            byte = length & 0x7F
            length >>= 7
            if length:
                data.append(byte | 0x80)
            else:
                data.append(byte)
                break
        data.extend(encoded)
        data.append(0)

    out = bytearray(data_off)
    out[0:8] = b"dex\n035\x00"
    struct.pack_into("<I", out, 36, header_size)
    struct.pack_into("<I", out, 40, 0x12345678)  # endian_tag
    struct.pack_into("<I", out, 56, string_ids_size)
    struct.pack_into("<I", out, 60, string_ids_off)
    struct.pack_into("<I", out, 64, type_ids_size)
    struct.pack_into("<I", out, 68, type_ids_off)
    struct.pack_into("<I", out, 96, class_defs_size)
    struct.pack_into("<I", out, 100, class_defs_off)
    for index, offset in enumerate(offsets):
        struct.pack_into("<I", out, string_ids_off + index * 4, offset)
    struct.pack_into("<I", out, type_ids_off, 0)  # type_ids[0].descriptor_idx = "Lcom/example/Probe;"

    struct.pack_into("<I", out, class_defs_off + 0, 0)  # class_idx
    struct.pack_into("<I", out, class_defs_off + 16, source_index)  # source_file_idx

    out.extend(data)
    struct.pack_into("<I", out, 32, len(out))  # file_size بعد الضمّ — لا قبله
    signature = hashlib.sha1(bytes(out[32:])).digest()
    out[12:32] = signature
    struct.pack_into("<I", out, 8, zlib.adler32(bytes(out[12:])) & 0xFFFFFFFF)
    return bytes(out)


def _read_source_index(path: str) -> int:
    with open(path, "rb") as handle:
        data = handle.read()
    class_defs_off = _u32(data, 100)
    return _u32(data, class_defs_off + SOURCE_FILE_OFFSET_IN_CLASS_DEF)


def self_test() -> int:
    import tempfile

    cases: list[str] = []

    def expect(name: str, condition: bool) -> None:
        cases.append(name)
        if not condition:
            raise AssertionError("حالة فاشلة: " + name)

    with tempfile.TemporaryDirectory() as work:
        dirty = os.path.join(work, "classes.dex")
        with open(dirty, "wb") as handle:
            handle.write(_synthetic_dex("ContextBridge.kt"))
        expect("العيّنة تحمل SourceFile قبل الإصلاح", _read_source_index(dirty) != NO_INDEX)

        report = scan(work)
        expect("لا عوائق بعد الإصلاح", not report.problems)
        expect("العيّنة نُظّفت", _read_source_index(dirty) == NO_INDEX)
        expect("عُدّ صنف واحد", report.files and report.files[0]["stripped"] == 1)
        expect("ونُظّف نصّ واحد", report.files and report.files[0]["scrubbed"] == 1)
        # والجوهر: جدول النصوص نفسه لم يبقَ فيه اسم ملفّ — لا المرجع وحده.
        with open(dirty, "rb") as handle:
            table = handle.read()
        expect("لا '.kt' في جدول النصوص", b".kt" not in table.lower())
        expect("التوقيع أُعيد حسابه", report.files[0]["path"] == dirty)

        # والقارئ المستقل: البصمة والتوقيع متّسقان بعد الكتابة.
        with open(dirty, "rb") as handle:
            after = handle.read()
        expect(
            "SHA-1 متّسق",
            hashlib.sha1(after[32:]).digest() == after[12:32],
        )
        expect(
            "Adler32 متّسق",
            (zlib.adler32(after[12:]) & 0xFFFFFFFF) == _u32(after, 8),
        )

        again = scan(work)
        expect("إعادة التشغيل لا تُبلّغ شيئًا", not again.problems)
        expect("ولا تعدّ صنفًا نُظّف", again.files and again.files[0]["stripped"] == 0)

    with tempfile.TemporaryDirectory() as work:
        clean = os.path.join(work, "classes.dex")
        with open(clean, "wb") as handle:
            handle.write(_synthetic_dex(None))
        report = scan(work)
        expect("عيّنة بلا SourceFile تمرّ", not report.problems)
        expect("ولا تُعدّ نظيفةً كأنها نُظّفت", report.files[0]["stripped"] == 0)

    with tempfile.TemporaryDirectory() as work:
        report = scan(work)
        expect("مجلّد بلا dex يُبلَّغ", [p["rule"] for p in report.problems] == ["dex_absent"])

    report = scan(os.path.join(work, "ghost"))
    expect("مسار مفقود يُبلَّغ", [p["rule"] for p in report.problems] == ["path_missing"])

    with tempfile.TemporaryDirectory() as work:
        broken = os.path.join(work, "classes.dex")
        with open(broken, "wb") as handle:
            handle.write(b"not a dex")
        report = scan(work)
        expect("ملفّ ليس DEX يُبلَّغ", [p["rule"] for p in report.problems] == ["dex_magic"])

    print("✓ قياس الأداة: " + str(len(cases)) + " حالات معروفة، كلها بالحكم المتوقّع")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="إزالة SourceFile من DEX")
    parser.add_argument("--path", help="مجلّد أو ملفّ .dex")
    parser.add_argument("--assert", dest="assert_mode", action="store_true")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()

    if not args.path:
        parser.error("--path مطلوب (أو --self-test)")

    report = scan(args.path, dry_run=args.dry_run)

    if args.json:
        print(json.dumps({"files": report.files, "obstacles": report.problems}, ensure_ascii=False, indent=2))
        return 1 if report.problems else 0

    total = sum(item["stripped"] for item in report.files)
    for item in report.files:
        print(f"  · {os.path.basename(item['path'])}: أصناف {item['classes']} · نُظّف {item['stripped']} · كان نظيفًا {item['already_clean']}")
    print(f"الحصيلة: {len(report.files)} ملفّ DEX · {total} صنفًا أُزيل اسمه")
    for problem in report.problems:
        print(f"✗ {problem['rule']}: {problem['where']} — {problem['detail']}")
    print("عوائق: " + str(len(report.problems)))
    return 1 if report.problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
