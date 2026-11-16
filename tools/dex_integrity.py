#!/usr/bin/env python3
"""بوابة سلامة DEX داخل الحزمة — تكملة تصليب الإصدار (2026-11-16).

السؤال الذي بُنيت له: `BLA` (‏`qybaihe/bla`، MIT) يزيد كلفة تحليل `DEX` بتكثيف جدول
النصوص/الأنواع، ووثيقته تُصرّح بأنّه **يعيد حساب `SHA-1` و`Adler32`** — وهما الحقلان
اللذان يتحقّق منهما `ART` قبل التحميل. فإن بقي أحدهما قديمًا بعد التعديل، فالحزمة
**لا تُثبَّت** أو تنهار عند أول تحميل لصنف — وكلاهما عطب لا يظهر في `aapt2` ولا في
`apksigner`. أي أنّ الخطر الحقيقي في تصليب `DEX` هو **صحّة المحصّلتين**، لا «هل عُدّل الملف».

فالبوابة تقرأ كل `.dex` (في مجلّد أو داخل `.apk`) وتقيس:
  • السحر `dex\\n035\\0`…`dex\\n039\\0`  → ملفّ ليس DEX
  • `file_size` في الترويسة = الحجم الفعلي  → اقتطاع/حشو
  • `header_size` = 0x70 و`endian_tag` = 0x12345678
  • `checksum` = Adler32 لما بعد الحقل (‏offset 12) → **الحقل الذي يُفسد التصليب الحزمة**
  • `signature` = SHA-1 لما بعد الحقل (‏offset 32)

التحقّق:
    python3 tools/dex_integrity.py --assert manager/app/build/outputs/apk/release
    python3 tools/dex_integrity.py --assert --apk path/to/app-release.apk
    python3 tools/dex_integrity.py --self-test     # يبني DEX صناعيًّا صالحًا ثم يُفسده

وحدّها المعلن: تقيس **بنية الملفّ** لا سلوكه. DEX سليم المحصّلتين قد يكون مع ذلك غير
قابل للتحميل (تعليمة غير صالحة، مرجع ميت) — وهذا لا يُثبته إلا جهاز (§0.10).
"""

from __future__ import annotations

import argparse
import hashlib
import io
import os
import struct
import sys
import zipfile
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

DEX_MAGIC_PREFIX = b"dex\n"
DEX_HEADER_SIZE = 0x70
DEX_ENDIAN_TAG = 0x12345678


def _obstacle(path: str, rule: str, detail: str) -> dict:
    return {"rule": rule, "where": path, "detail": detail}


def check_dex_bytes(data: bytes, where: str) -> list[dict]:
    """يقيس محصّلات DEX في الذاكرة — دالّة واحدة يستعملها المجلّد والـAPK والاختبار الذاتي."""
    findings: list[dict] = []
    if len(data) < DEX_HEADER_SIZE:
        return [_obstacle(where, "dex_truncated", f"أصغر من ترويسة DEX: {len(data)} بايت")]
    if not data.startswith(DEX_MAGIC_PREFIX) or data[7:8] != b"\x00":
        return [_obstacle(where, "dex_magic", "السحر ليس `dex\\n0xx\\0` — ليس ملفّ DEX")]
    if len(data) != struct.unpack_from("<I", data, 32)[0]:
        findings.append(
            _obstacle(
                where,
                "dex_file_size",
                f"`file_size` في الترويسة {struct.unpack_from('<I', data, 32)[0]} ≠ الفعلي {len(data)}",
            )
        )
    header_size, endian_tag = struct.unpack_from("<II", data, 36)
    if header_size != DEX_HEADER_SIZE:
        findings.append(_obstacle(where, "dex_header_size", f"`header_size` = {header_size:#x} ≠ 0x70"))
    if endian_tag != DEX_ENDIAN_TAG:
        findings.append(_obstacle(where, "dex_endian_tag", f"`endian_tag` = {endian_tag:#x} ≠ 0x12345678"))
    stored_checksum = struct.unpack_from("<I", data, 8)[0]
    actual_checksum = zlib.adler32(data[12:]) & 0xFFFFFFFF
    if stored_checksum != actual_checksum:
        findings.append(
            _obstacle(
                where,
                "dex_checksum",
                f"`checksum` مخزون {stored_checksum:#010x} ≠ المحسوب {actual_checksum:#010x} — ART يرفض التحميل",
            )
        )
    stored_signature = data[12:32]
    actual_signature = hashlib.sha1(data[32:]).digest()
    if stored_signature != actual_signature:
        findings.append(
            _obstacle(
                where,
                "dex_signature",
                f"`signature` مخزون {stored_signature.hex()[:16]}… ≠ المحسوب {actual_signature.hex()[:16]}…",
            )
        )
    return findings


def check_directory(path: str) -> list[dict]:
    if not os.path.isdir(path):
        return [_obstacle(path, "path_missing", "المسار ليس مجلّدًا")]
    dex_files = []
    for base, _dirs, files in os.walk(path):
        for name in sorted(files):
            if name.lower().endswith(".dex"):
                dex_files.append(os.path.join(base, name))
    if not dex_files:
        return [_obstacle(path, "dex_absent", "لا ملفّ `.dex` تحت المسار — البوابة لا تُثبت شيئًا")]
    findings: list[dict] = []
    for dex_path in sorted(dex_files):
        with open(dex_path, "rb") as handle:
            findings.extend(check_dex_bytes(handle.read(), os.path.relpath(dex_path, ROOT).replace(os.sep, "/")))
    return findings


def check_apk(path: str) -> list[dict]:
    if not os.path.isfile(path):
        return [_obstacle(path, "path_missing", "الملفّ غير موجود")]
    findings: list[dict] = []
    try:
        with zipfile.ZipFile(path) as archive:
            names = [n for n in archive.namelist() if n.lower().endswith(".dex")]
            if not names:
                return [_obstacle(path, "dex_absent", "الحزمة بلا `classes*.dex` — ليست APK صالحًا")]
            for name in sorted(names):
                findings.extend(check_dex_bytes(archive.read(name), f"{path}!{name}"))
    except zipfile.BadZipFile as error:
        return [_obstacle(path, "apk_bad_zip", f"الحزمة ليست ZIP صالحًا: {error}")]
    return findings


# ── قياس الأداة نفسها ────────────────────────────────────────────────────────
def _synthetic_dex(payload: bytes = b"\x00" * 64) -> bytes:
    """DEX صناعي: ترويسة 0x70 صحيحة الحقول والمحصّلتين، ثم حشو — لا كود، لا تحليل."""
    header = bytearray(DEX_HEADER_SIZE)
    header[0:8] = b"dex\n035\x00"
    body = bytes(header[0x70:]) + payload
    data = bytearray(header) + bytearray(payload)
    total = len(data)
    struct.pack_into("<I", data, 32, total)
    struct.pack_into("<I", data, 36, DEX_HEADER_SIZE)
    struct.pack_into("<I", data, 40, DEX_ENDIAN_TAG)
    # **الترتيب مقصود:** البصمة أولًا ثم المحصّلة — لأن المحصّلة تُحسب على ما بعد offset 12،
    # وهي تشمل حقل البصمة نفسه (12..32). وكتبهما معكوسًا يجعل الملفّ الصالح يبدو تالفًا —
    # وهو العطب نفسه الذي وقع في أول نسخة من هذه الأداة وقاسه `--self-test`.
    data[12:32] = hashlib.sha1(bytes(data[32:])).digest()
    struct.pack_into("<I", data, 8, zlib.adler32(bytes(data[12:])) & 0xFFFFFFFF)
    return bytes(data)


def self_test() -> int:
    failures: list[str] = []

    def expect(name: str, findings: list[dict], want_rules: set[str]) -> None:
        got = {f["rule"] for f in findings}
        if got != want_rules:
            failures.append(f"{name}: got {sorted(got)} want {sorted(want_rules)}")

    good = _synthetic_dex()
    expect("dex صالح", check_dex_bytes(good, "synthetic"), set())

    # إفساد بايت داخل الحْمولة: المحصّلتان القديمتان لم تعودا تطابقان ⇒ يجب أن يُرصدا معًا.
    corrupted = bytearray(good)
    corrupted[-1] ^= 0xFF
    expect("بايت مقلوب", check_dex_bytes(bytes(corrupted), "synthetic"), {"dex_checksum", "dex_signature"})

    # حقل حجم كاذب (اقتطاع): يُعاد حساب المحصّلتين بعده حتى يبقى العائق **واحدًا مسمّى**،
    # وإلا اختلط حكمُ الحجم بحكم المحصّلة ولم يُعرف أيّهما سقط.
    wrong_size = bytearray(good)
    struct.pack_into("<I", wrong_size, 32, len(good) + 8)
    wrong_size[12:32] = hashlib.sha1(bytes(wrong_size[32:])).digest()
    struct.pack_into("<I", wrong_size, 8, zlib.adler32(bytes(wrong_size[12:])) & 0xFFFFFFFF)
    expect("file_size كاذب", check_dex_bytes(bytes(wrong_size), "synthetic"), {"dex_file_size"})

    # ليست DEX أصلًا.
    expect("سحر خاطئ", check_dex_bytes(b"PK\x03\x04" + b"\x00" * 200, "synthetic"), {"dex_magic"})
    expect("أقصر من الترويسة", check_dex_bytes(b"dex\n035\x00" + b"\x00" * 8, "synthetic"), {"dex_truncated"})

    # مجلّد بلا DEX: «لا شيء» ليس نجاحًا.
    tmp = os.path.join("/tmp", "dex_integrity_selftest")
    os.makedirs(tmp, exist_ok=True)
    for name in os.listdir(tmp):
        os.remove(os.path.join(tmp, name))
    expect("مجلّد فارغ", check_directory(tmp), {"dex_absent"})
    with open(os.path.join(tmp, "classes.dex"), "wb") as handle:
        handle.write(good)
    expect("مجلّد سليم", check_directory(tmp), set())
    with open(os.path.join(tmp, "classes2.dex"), "wb") as handle:
        handle.write(corrupted)
    expect("مجلّد فيه تالف", check_directory(tmp), {"dex_checksum", "dex_signature"})

    if failures:
        print("✗ البوابة نفسها معطوبة:")
        for failure in failures:
            print("  - " + failure)
        return 1
    print(f"✓ قياس الأداة: ٨ حالات معروفة، كلها بالحكم المتوقّع (ترويسة DEX صناعية، " f"{len(good)} بايت)")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="بوابة سلامة محصّلات DEX")
    parser.add_argument("--assert", dest="assert_mode", action="store_true", help="يفشل عند أي عائق")
    parser.add_argument("--apk", metavar="PATH", help="حزمة APK بدل مجلّد")
    parser.add_argument("--path", metavar="PATH", default="manager/app/build/outputs/apk/release",
                        help="مجلّد فيه .dex (أو فيه APK مفكوك)")
    parser.add_argument("--json", action="store_true", help="مخرَج JSON")
    parser.add_argument("--self-test", action="store_true", help="يقيس الأداة على DEX صناعي")
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    target = args.apk or args.path
    if not os.path.isabs(target):
        target = os.path.join(ROOT, target)
    findings = check_apk(target) if (args.apk or target.lower().endswith(".apk")) else check_directory(target)

    if args.json:
        import json

        print(json.dumps({"target": target, "obstacles": findings}, ensure_ascii=False, indent=2))

    if findings:
        print(f"✗ سلامة DEX: {len(findings)} عائقًا")
        for finding in findings:
            print(f"  {finding['rule']}: {finding['where']} — {finding['detail']}")
        return 1

    print(f"✓ سلامة DEX سليمة: {os.path.relpath(target, ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
