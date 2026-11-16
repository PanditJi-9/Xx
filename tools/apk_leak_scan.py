#!/usr/bin/env python3
"""فحص حزمة الإصدار عن آثار تُسهّل الهندسة العكسية — لا عن أسرار.

**لماذا وُجد (تكملة ٢٠٢٦-١١-١٦):** أداة `morluto/rea` تقرأ الحزمة **ساكنةً** بـJADX،
وأقوى ما يُعطيها اسماً ذا معنى هو ما يبقى في `classes*.dex`: اسم ملفّ المصدر، ومسار
بناء مطلق، واسم ملفّ `proguard-rules.pro`، واسم مخزن المفاتيح. وR8 يحذف هذا كلّه
**إن** مُنع من إعادته؛ لكنّ سطر `-keepattributes SourceFile` واحد يعيده، ولا بوابة
تقيس الحزمة الناتجة — فتُقاس هنا.

**وحدّه المُعلَن:** هذا الفحص يقرأ **جدول النصوص في DEX** ومدخلات الحزمة، لا كل
الشيفرة. فمرورُه يعني «لا هذه الآثار في هذه الطبقات»، ولا يعني «الحزمة غير قابلة
للتحليل» — ولا يُدَّعى ذلك أبدًا.

الاستعمال:
    python3 tools/apk_leak_scan.py --apk app-release.apk
    python3 tools/apk_leak_scan.py --path manager/app/build/outputs/apk/release
    python3 tools/apk_leak_scan.py --assert --apk app-release.apk
    python3 tools/apk_leak_scan.py --self-test

الخروج: 0 سليم · 1 عائق · 2 خطأ استعمال.
"""

from __future__ import annotations

import argparse
import json
import os
import struct
import sys
import tempfile
import zipfile
import zlib
import hashlib

DEX_MAGIC_PREFIX = b"dex\n"
HEADER_SIZE = 0x70

#: نهايات أسماء ملفّات المصدر — بقاء الواحد منها في جدول النصوص يسمّي الملفّ لـJADX.
SOURCE_SUFFIXES = (".kt", ".java")

#: آثار مسار البناء: تكشف بنية المشروع وأسماء الحزم الداخلية بلا تخمين.
BUILD_PATH_MARKERS = (
    "/home/runner/work",
    "/home/runner",
    ".gradle/caches",
    "/app/src/main/",
    "proguard-rules.pro",
)

#: مسارات مخزن المفاتيح — لا تُشحن أبدًا، وظهورها دليل خطأ إعداد لا مجرّد بصمة.
SECRET_PATH_MARKERS = (".jks", ".keystore")

#: مدخلات حزمة لا تُشحن: خرائط R8 ونصوص مصدر ولقطات نسخ.
FORBIDDEN_ENTRY_SUFFIXES = (".kt", ".java", "mapping.txt")


def read_uleb128(data: bytes, offset: int) -> tuple[int, int]:
    """يقرأ عددًا متغيّر الطول في DEX ويُعيد (القيمة، الموضع التالي)."""
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


def dex_strings(data: bytes) -> list[str]:
    """يُعيد كل نصوص `string_ids` في DEX — بلا تفكيك شيفرة وبلا اعتماد خارجي."""
    if len(data) < HEADER_SIZE or not data.startswith(DEX_MAGIC_PREFIX):
        raise ValueError("ليس DEX")
    count, offset = struct.unpack_from("<II", data, 56)
    if count == 0:
        return []
    if offset + count * 4 > len(data):
        raise ValueError("جدول string_ids خارج الملفّ")
    strings: list[str] = []
    for index in range(count):
        (string_offset,) = struct.unpack_from("<I", data, offset + index * 4)
        if string_offset >= len(data):
            continue
        try:
            _, cursor = read_uleb128(data, string_offset)
        except ValueError:
            continue
        end = data.find(b"\x00", cursor)
        if end < 0:
            continue
        strings.append(data[cursor:end].decode("utf-8", errors="replace"))
    return strings


def classify_string(text: str) -> str | None:
    """يُصنّف نصًّا واحدًا؛ `None` يعني «لا أثر»."""
    lowered = text.lower()
    for marker in SECRET_PATH_MARKERS:
        if lowered.endswith(marker):
            return "secret_path"
    for marker in BUILD_PATH_MARKERS:
        if marker in lowered:
            return "build_path"
    for suffix in SOURCE_SUFFIXES:
        if lowered.endswith(suffix):
            return "source_file_name"
    return None


def scan_strings(strings: list[str], origin: str) -> list[dict]:
    findings: list[dict] = []
    seen: set[tuple[str, str]] = set()
    for text in strings:
        kind = classify_string(text)
        if kind is None:
            continue
        key = (kind, text)
        if key in seen:
            continue
        seen.add(key)
        findings.append(
            {
                "rule": f"dex_string:{kind}",
                "where": origin,
                "detail": text[:120],
            }
        )
    return findings


def scan_apk(path: str) -> tuple[list[dict], dict]:
    """يفحص حزمة: مدخلاتها ثم جدول نصوص كل `classes*.dex` داخلها."""
    findings: list[dict] = []
    stats = {"apk": os.path.basename(path), "dex_files": 0, "strings": 0}
    if not os.path.isfile(path):
        return ([{"rule": "path_missing", "where": path, "detail": "الحزمة غير موجودة"}], stats)
    if not zipfile.is_zipfile(path):
        return ([{"rule": "apk_bad_zip", "where": path, "detail": "ليس ZIP صالحًا"}], stats)
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            lowered = name.lower()
            for suffix in FORBIDDEN_ENTRY_SUFFIXES:
                if lowered.endswith(suffix):
                    findings.append(
                        {
                            "rule": "apk_entry_forbidden",
                            "where": name,
                            "detail": f"مدخل لا يُشحن ({suffix})",
                        }
                    )
            if not (lowered.startswith("classes") and lowered.endswith(".dex")):
                continue
            stats["dex_files"] += 1
            try:
                strings = dex_strings(archive.read(name))
            except (ValueError, struct.error, zlib.error) as exc:
                findings.append({"rule": "dex_unreadable", "where": name, "detail": str(exc)[:100]})
                continue
            stats["strings"] += len(strings)
            findings.extend(scan_strings(strings, name))
    return findings, stats


def scan_dir(path: str) -> tuple[list[dict], dict]:
    apks = sorted(
        os.path.join(path, name)
        for name in os.listdir(path)
        if name.lower().endswith(".apk")
    ) if os.path.isdir(path) else []
    if not apks:
        # مجلّد `*.dex` خام — وهو ما يُنتجه `bla harden` في `.bla-hardened/`.
        # قبل هذا كان يُردّ بـ`dex_absent` فيتعذّر قياس أثر التصليب على النصوص
        # نفسها، ويُقرأ الردّ «لا شيء هنا» وهو «لا أعرف أن أقرأ هذا الشكل».
        dexes = sorted(
            os.path.join(path, name)
            for name in os.listdir(path)
            if name.lower().endswith(".dex")
        ) if os.path.isdir(path) else []
        if not dexes:
            return ([{"rule": "dex_absent", "where": path, "detail": "لا حزمة .apk ولا ملفّات .dex في المسار"}], {})
        findings: list[dict] = []
        stats: dict = {"dex_files": 0, "strings": 0}
        for dex in dexes:
            stats["dex_files"] += 1
            try:
                with open(dex, "rb") as handle:
                    strings = dex_strings(handle.read())
            except (ValueError, struct.error, zlib.error) as exc:
                findings.append({"rule": "dex_unreadable", "where": os.path.basename(dex), "detail": str(exc)[:100]})
                continue
            stats["strings"] += len(strings)
            findings.extend(scan_strings(strings, os.path.basename(dex)))
        return findings, stats
    findings: list[dict] = []
    stats: dict = {"apks": len(apks), "dex_files": 0, "strings": 0}
    for apk in apks:
        part, part_stats = scan_apk(apk)
        findings.extend(part)
        stats["dex_files"] += part_stats.get("dex_files", 0)
        stats["strings"] += part_stats.get("strings", 0)
    return findings, stats


# ─────────────────────────── قياس الأداة نفسها ───────────────────────────


def build_dex(strings: list[str]) -> bytes:
    """DEX صناعيّ بجدول نصوص فقط — يكفي لقياس قارئ النصوص، ويُعاد بناؤه لا يُنزَّل."""
    count = len(strings)
    string_ids_off = HEADER_SIZE
    data_off = string_ids_off + 4 * count
    data = bytearray()
    offsets: list[int] = []
    for text in strings:
        offsets.append(data_off + len(data))
        payload = text.encode("utf-8")
        # uleb128 لطول UTF-16 — تكفي القيم الصغيرة هنا، والطول الحقيقيّ لا يُقرأ.
        length = len(text)
        encoded = bytearray()
        while True:
            byte = length & 0x7F
            length >>= 7
            if length:
                encoded.append(byte | 0x80)
            else:
                encoded.append(byte)
                break
        data.extend(encoded)
        data.extend(payload)
        data.append(0)

    header = bytearray(HEADER_SIZE)
    header[0:8] = b"dex\n035\x00"
    struct.pack_into("<I", header, 32, HEADER_SIZE + 4 * count + len(data))
    struct.pack_into("<I", header, 36, HEADER_SIZE)
    struct.pack_into("<I", header, 40, 0x12345678)
    struct.pack_into("<II", header, 56, count, string_ids_off)
    struct.pack_into("<II", header, 104, len(data), data_off)

    body = bytearray()
    for offset in offsets:
        body.extend(struct.pack("<I", offset))
    blob = bytearray(header) + body + data
    # الترتيب الصحيح: SHA-1 على data[32:] أولًا، ثم Adler32 على data[12:] (يشمله).
    blob[12:32] = hashlib.sha1(bytes(blob[32:])).digest()
    struct.pack_into("<I", blob, 8, zlib.adler32(bytes(blob[12:])) & 0xFFFFFFFF)
    return bytes(blob)


def self_test() -> int:
    failures: list[str] = []
    cases = [0]

    def expect(condition: bool, label: str) -> None:
        cases[0] += 1
        if not condition:
            failures.append(label)
        else:
            print(f"  ✓ {label}")

    clean = build_dex(["Lnd/max/Main;", "<init>", "onCreate"])
    bad = build_dex(
        [
            "Lnd/max/Main;",
            "MainActivity.kt",
            "/home/runner/work/Xx/Xx/manager/app/src/main/java",
            "proguard-rules.pro",
            "maxmanager.jks",
        ]
    )

    strings = dex_strings(clean)
    expect(strings == ["Lnd/max/Main;", "<init>", "onCreate"], "قراءة جدول النصوص كما كُتب")
    expect(scan_strings(strings, "classes.dex") == [], "الحزمة النظيفة تمرّ بلا بلاغ")

    found = scan_strings(dex_strings(bad), "classes.dex")
    rules = {item["rule"] for item in found}
    expect("dex_string:source_file_name" in rules, "اسم ملفّ .kt يُبلَّغ")
    expect("dex_string:build_path" in rules, "مسار بناء مطلق يُبلَّغ")
    expect("dex_string:secret_path" in rules, "مسار مخزن مفاتيح يُبلَّغ")
    # أربعة لا ثلاثة: `proguard-rules.pro` أثر مسار بذاته، و`/home/runner/...` أثر آخر،
    # واسم ملفّ المصدر ثالث، ومخزن المفاتيح رابع — والتكرار الممنوع هو تكرار **النصّ نفسه**.
    expect(len({item["detail"] for item in found}) == 4, "لا بلاغ مكرّر للنصّ نفسه")

    expect(classify_string("Lnd/max/core/SecurityGate;") is None, "اسم صنف عادي لا يُبلَّغ")
    expect(classify_string("resources.arsc") is None, "مدخل موارد لا يُبلَّغ")

    with tempfile.TemporaryDirectory() as tmp:
        apk = os.path.join(tmp, "app-release.apk")
        with zipfile.ZipFile(apk, "w") as archive:
            archive.writestr("classes.dex", clean)
            archive.writestr("classes2.dex", bad)
            archive.writestr("mapping.txt", "x")
        findings, stats = scan_apk(apk)
        rules = {item["rule"] for item in findings}
        expect(stats["dex_files"] == 2, "عدّ ملفّات DEX صحيح")
        expect("apk_entry_forbidden" in rules, "mapping.txt داخل الحزمة يُبلَّغ")
        expect("dex_string:source_file_name" in rules, "الفحص يقرأ كل dex لا الأول")
        expect(any(item["where"] == "classes2.dex" for item in findings), "الموضع يُسمّى")

        empty = os.path.join(tmp, "empty")
        os.mkdir(empty)
        findings, _ = scan_dir(empty)
        expect([item["rule"] for item in findings] == ["dex_absent"], "مسار بلا حزمة يُعلن لا يمرّ")

    if failures:
        print("✗ البوابة نفسها معطوبة:")
        for item in failures:
            print(f"  - {item}")
        return 1
    print(f"✓ قياس الأداة: {cases[0]} حالة معروفة، كلها بالحكم المتوقّع")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="آثار الهندسة العكسية في حزمة الإصدار")
    parser.add_argument("--apk", help="مسار حزمة .apk")
    parser.add_argument("--path", help="مجلّد يحوي حزمًا")
    parser.add_argument("--assert", dest="assert_mode", action="store_true")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()
    if not args.apk and not args.path:
        parser.error("مرّر --apk أو --path أو --self-test")

    if args.apk:
        findings, stats = scan_apk(args.apk)
    else:
        findings, stats = scan_dir(args.path)

    if args.json:
        print(json.dumps({"findings": findings, "stats": stats}, ensure_ascii=False, indent=2))
    else:
        for item in findings:
            print(f"✗ {item['rule']}: {item['where']} — {item['detail']}")
        if findings:
            print(f"عوائق: {len(findings)}")
        else:
            print(
                "✓ لا أثر لمسار بناء ولا اسم ملفّ مصدر ولا خريطة R8 في "
                f"{stats.get('dex_files', 0)} ملفّ DEX"
            )
    return 1 if (args.assert_mode and findings) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
