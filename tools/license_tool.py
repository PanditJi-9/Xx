#!/usr/bin/env python3
"""مفتاح التوقيع: فحص المفتاح العامّ ومطابقته للمفتاح الخاصّ — **بأرقام لا بثقة**.

## لماذا هذه الأداة

دالّة `maxctl` توقّع الإعداد بمفتاح EC P-256، وتطبيق MaxManager يتحقّق بمفتاح **عامّ** يُبنى
فيه من سرّ GitHub (`CONFIG_PUBLIC_KEY`). والخطأ في هذا الموضع **صامت**: مفتاحٌ مقصوص أو منحنى
آخر أو زوج غير متطابق ⇒ التطبيق يرفض كل إعداد موقّع ويرجع إلى الافتراضي المحلي، والمالك يرى
لوحته «نشرت بنجاح» — وهو أسوأ أنواع العطب لأنه لا يشكو.

فالأداة تقيس ثلاثة أشياء قبل البناء:
① المفتاح العامّ **DER صالح ومقروء** (لا سطر مقصوص ولا ملفّ خاصّ لُصق مكان العامّ).
② المنحنى **prime256v1 (P-256)** بالاسم — لأن `SHA256withECDSA` على منحنى آخر يفشل على الجهاز.
③ أن العامّ **مشتقٌّ فعلًا من الخاصّ** نفسه (تُشتقّ العامّة من الخاصّة وتُقارن بايتًا ببايت).

وقراءة SPKI **بالمواضع المحسوبة** لا بالبحث عن بايت: `0x03` يظهر داخل إحداثيات النقطة نفسها،
و`rfind` عنه كان يرفض مفاتيح سليمة (عطب حقيقي أمسكه `--self-test`، والحالة ⑤ تثبّته حتميًّا).
والاختبار الذاتي ستّ حالات: زوج صحيح · زوجان مختلفان · base64 مكسور · منحنى P-384 · نقطة كلها
`0x03` · أربعة أزواج عشوائية متتالية.

## الاستعمال

    python3 tools/license_tool.py check --public-file mm-license-public.b64
    python3 tools/license_tool.py check --public-file mm-license-public.b64 \\
        --pkcs8-file mm-license-key.pkcs8.b64      # أو: --private mm-license-key.pem
    python3 tools/license_tool.py --self-test      # تولّد زوجًا مؤقّتًا وتقيس الأداة نفسها

والأداة **لا تطبع المفتاح الخاصّ أبدًا** ولا تنسخه إلى أي ملفّ: تنسخ ملفًا إلى مؤقّتٍ للقراءة
بـ`openssl` ثم تحذفه. والنتيجة إمّا `✓` ببياناتها أو `✗` بسببه.
"""

from __future__ import annotations

import argparse
import base64
import binascii
import hashlib
import os
import re
import subprocess
import sys
import tempfile

# ── ثوابت P-256: تُقرأ من بايتات الملفّ لا من نصّ `openssl` وحده ────────────────────────────────

# OID 1.2.840.10045.2.1 (ecPublicKey) و 1.2.840.10045.3.1.7 (prime256v1) — بالبايتات.
OID_EC_PUBLIC_KEY = bytes.fromhex("06072a8648ce3d0201")
OID_PRIME256V1 = bytes.fromhex("06082a8648ce3d030107")

POINT_BYTES = 65  # 0x04 + X(32) + Y(32) — النقطة غير المضغوطة
DER_HEAD = 0x30  # SEQUENCE


def _b64_bytes(text: str) -> bytes:
    """base64 → بايتات، ويرفض أي محرف غريب (لصقٌ ناقص أو سطر مشروح داخل الملفّ)."""
    cleaned = re.sub(r"\s+", "", text)
    if not cleaned:
        raise ValueError("الملفّ فارغ")
    try:
        return base64.b64decode(cleaned, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise ValueError(f"ليس base64 صالحًا ({exc})") from exc


def _read_tlv(der: bytes, offset: int) -> tuple[int, int, int]:
    """يقرأ ترويسة TLV واحدة: (الوسم · موضع المحتوى · طول المحتوى) — بالطول القصير والطويل."""
    if offset + 2 > len(der):
        raise ValueError("DER مبتور عند ترويسة TLV")
    tag = der[offset]
    offset += 1
    first = der[offset]
    if first < 0x80:
        return tag, offset + 1, first
    count = first & 0x7F
    if count == 0 or offset + 1 + count > len(der):
        raise ValueError("طول DER غير صالح")
    length = int.from_bytes(der[offset + 1 : offset + 1 + count], "big")
    return tag, offset + 1 + count, length


def _point_from_spki(der: bytes) -> bytes:
    """
    يقرأ نقطة المفتاح العامّ من SPKI **بقراءة بنيوية** (SEQUENCE ← AlgorithmIdentifier ← BIT STRING).

    ولماذا لا يكفي «نجح `openssl`»: نجاحه يقول إن الملفّ مفتاح عامّ ما، ولا يقول **أيّ** نقطة
    ولا أيّ طول — والقصد هنا قياس الشكل الذي يقرؤه `P256Signatures` في التطبيق حرفيًّا.

    ولماذا لا يُبحث عن البايت `0x03` بـ`rfind`: لأنه **يظهر داخل إحداثيات النقطة نفسها**
    (٦٤ بايت عشوائية)، فيقع الموضع على بايت في الإحداثي لا على رأس BIT STRING وتُرفض مفاتيح
    سليمة — وهذا وقع فعلًا في أول تشغيل حقيقي للأداة، وأمسكه `--self-test` (الحالة ⑤ أدناه
    تثبّته حتميًّا). فالقراءة هنا بالمواضع المحسوبة لا بالبحث عن بايت.
    """
    if len(der) < 8 or der[0] != DER_HEAD:
        raise ValueError("الملفّ ليس DER يبدأ بـ SEQUENCE (هل لصقت ملفًّا آخر؟)")

    tag, start, length = _read_tlv(der, 0)
    if tag != DER_HEAD:
        raise ValueError("الجذر ليس SEQUENCE")
    end = start + length
    if end > len(der):
        raise ValueError("DER مبتور (الطول المعلَن أكبر من الملفّ)")

    tag, alg_start, alg_len = _read_tlv(der, start)
    if tag != DER_HEAD:
        raise ValueError("لا AlgorithmIdentifier (SEQUENCE) داخل المفتاح")
    algorithm = der[alg_start : alg_start + alg_len]
    if OID_EC_PUBLIC_KEY not in algorithm:
        raise ValueError("لا OID للمفتاح الإهليلجيّ (ecPublicKey) — الملفّ ليس مفتاح EC")
    if OID_PRIME256V1 not in algorithm:
        raise ValueError("المنحنى ليس prime256v1 (P-256) — SHA256withECDSA عليه لا يطابق التطبيق")

    tag, bits_start, bits_len = _read_tlv(der, alg_start + alg_len)
    if tag != 0x03:
        raise ValueError("لا BIT STRING بعد معرّف الخوارزمية (بنية SPKI غير مكتملة)")
    body = der[bits_start : bits_start + bits_len]
    if body[:1] != b"\x00":
        raise ValueError("BIT STRING بلا البايت المبطّن (unused bits) — بنية غير متوقّعة")
    point = body[1:]
    if len(point) != POINT_BYTES or point[0] != 0x04:
        raise ValueError(
            f"النقطة بطول {len(point)} بايت وبداية {point[:1].hex() or '—'} — "
            f"المتوقّع {POINT_BYTES} بايت تبدأ بـ 04 (نقطة غير مضغوطة)"
        )
    return point


def _spki_from_point(point: bytes) -> bytes:
    """SPKI مصنوع لنقطة معلومة — يثبّت الاختبار الذاتي حتميًّا (انظر الحالة ⑤)."""
    algorithm = OID_EC_PUBLIC_KEY + OID_PRIME256V1
    algorithm_tlv = bytes([DER_HEAD, len(algorithm)]) + algorithm
    bits = b"\x00" + point
    bits_tlv = bytes([0x03, len(bits)]) + bits
    inner = algorithm_tlv + bits_tlv
    return bytes([DER_HEAD, len(inner)]) + inner


def _openssl(args: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["openssl", *args],
        capture_output=True,
        text=False,
        timeout=60,
    )


def _derive_public(der_private: bytes) -> bytes:
    """يشتقّ المفتاح العامّ (SPKI DER) من مفتاح خاصّ PKCS#8 DER عبر `openssl`."""
    with tempfile.NamedTemporaryFile(prefix="mm-lic-priv-", suffix=".der", delete=False) as fh:
        fh.write(der_private)
        path = fh.name
    try:
        done = _openssl(["pkey", "-inform", "DER", "-in", path, "-pubout", "-outform", "DER"])
        if done.returncode != 0:
            raise ValueError(
                "المفتاح الخاصّ لا يُقرأ كـ PKCS#8 DER — "
                f"({done.stderr.decode('utf-8', 'replace').strip().splitlines()[-1:] or ['بلا رسالة']})"
            )
        return done.stdout
    finally:
        os.unlink(path)


def _fingerprint(der: bytes) -> str:
    """بصمة SHA-256 للـSPKI — للمقارنة بين المفتاح في السرّ والمفتاح في البناء."""
    digest = hashlib.sha256(der).hexdigest()
    return ":".join(digest[i : i + 8] for i in range(0, 64, 8))


def _public_summary(der: bytes) -> str:
    point = _point_from_spki(der)
    return (
        f"المنحنى: prime256v1 (P-256)\n"
        f"طول المفتاح: {len(der)} بايت SPKI · النقطة: {len(point)} بايت (X=32 · Y=32)\n"
        f"بصمة SHA-256 للمفتاح العامّ: {_fingerprint(der)}\n"
        f"ما يُلصق في السرّ CONFIG_PUBLIC_KEY:\n{base64.b64encode(der).decode('ascii')}"
    )


def command_check(args: argparse.Namespace) -> int:
    try:
        public_der = _b64_bytes(open(args.public_file, encoding="utf-8").read())
    except OSError as exc:
        print(f"✗ لا يُقرأ ملفّ المفتاح العامّ: {exc}")
        return 1
    except ValueError as exc:
        print(f"✗ المفتاح العامّ: {exc}")
        return 1

    try:
        print("✓ المفتاح العامّ صالح")
        print(_public_summary(public_der))
    except ValueError as exc:
        print(f"✗ المفتاح العامّ: {exc}")
        return 1

    # ④ المطابقة — تُفحص فقط إن أُعطي المفتاح الخاصّ، ولا يُطبع منه شيء.
    if not args.private and not args.pkcs8_file:
        print("\n(بلا --private/--pkcs8-file: لم تُفحص المطابقة مع الخاصّ)")
        return 0

    try:
        if args.private:
            done = _openssl(["pkcs8", "-topk8", "-nocrypt", "-in", args.private, "-outform", "DER"])
            if done.returncode != 0:
                print(f"✗ المفتاح الخاصّ لا يُقرأ ({args.private})")
                return 1
            private_der = done.stdout
        else:
            private_der = _b64_bytes(open(args.pkcs8_file, encoding="utf-8").read())
    except (OSError, ValueError) as exc:
        print(f"✗ المفتاح الخاصّ: {exc}")
        return 1

    derived = _derive_public(private_der)
    if derived == public_der:
        print("\n✓ المطابقة: العامّ مشتقٌّ من هذا الخاصّ نفسه (بايتًا ببايت)")
        return 0
    print(
        "\n✗ عدم تطابق: العامّ **ليس** المفتاح المشتقّ من هذا الخاصّ.\n"
        f"  المشتقّ من الخاصّ: {_fingerprint(derived)}\n"
        f"  الملفّ العامّ:      {_fingerprint(public_der)}\n"
        "  النتيجة لو بُني بهذا: التطبيق يرفض كل إعداد موقّع (والخادم يوقّع بالخاصّ)."
    )
    return 1


def self_test() -> int:
    """
    تقيس الأداة نفسها: تولّد زوجًا مؤقّتًا وتتوقّع `✓`، وتولّد آخر وتتوقّع `✗` للمطابقة،
    وتُفسد base64 وتتوقّع الرفض. أداة تمرّ على كل شيء لا تُثبت شيئًا — هذه تُثبت أنها تفرّق.
    """
    with tempfile.TemporaryDirectory(prefix="mm-lic-selftest-") as tmp:
        def keypair(name: str) -> tuple[str, str]:
            pem = os.path.join(tmp, f"{name}.pem")
            pub = os.path.join(tmp, f"{name}-public.b64")
            made = _openssl(["ecparam", "-genkey", "-name", "prime256v1", "-noout", "-out", pem])
            if made.returncode != 0:
                raise RuntimeError("openssl فشل في توليد المفتاح")
            exported = _openssl(["ec", "-in", pem, "-pubout", "-outform", "DER"])
            if exported.returncode != 0:
                raise RuntimeError("openssl فشل في تصدير العامّ")
            with open(pub, "w", encoding="utf-8") as fh:
                fh.write(base64.b64encode(exported.stdout).decode("ascii"))
            return pem, pub

        a_pem, a_pub = keypair("a")
        _, b_pub = keypair("b")

        ok = command_check(argparse.Namespace(public_file=a_pub, private=a_pem, pkcs8_file=None))
        if ok != 0:
            print("✗ فشل الاختبار الذاتي: زوجٌ صحيح لم يمرّ")
            return 1
        print("✓ الاختبار الذاتي ①: زوج صحيح ⇒ ✓ مطابقة")

        mismatch = command_check(argparse.Namespace(public_file=b_pub, private=a_pem, pkcs8_file=None))
        if mismatch == 0:
            print("✗ فشل الاختبار الذاتي: زوجان مختلفان مرّا كمتطابقين")
            return 1
        print("✓ الاختبار الذاتي ②: زوجان مختلفان ⇒ ✗ عدم تطابق")

        broken = os.path.join(tmp, "broken.b64")
        with open(a_pub, encoding="utf-8") as fh:
            text = fh.read()
        with open(broken, "w", encoding="utf-8") as fh:
            fh.write(text[: len(text) // 2] + "!!!")
        rejected = command_check(argparse.Namespace(public_file=broken, private=None, pkcs8_file=None))
        if rejected == 0:
            print("✗ فشل الاختبار الذاتي: base64 مكسور مرّ")
            return 1
        print("✓ الاختبار الذاتي ③: base64 مكسور ⇒ ✗ رُفض")

        # ④ منحنى آخر (P-384): مفتاح سليم تمامًا — ويجب أن يُرفض **لأن التطبيق P-256 وحده**.
        other_curve_pem = os.path.join(tmp, "p384.pem")
        other_curve_pub = os.path.join(tmp, "p384-public.b64")
        made384 = _openssl(["ecparam", "-genkey", "-name", "secp384r1", "-noout", "-out", other_curve_pem])
        exported384 = _openssl(["ec", "-in", other_curve_pem, "-pubout", "-outform", "DER"])
        if made384.returncode != 0 or exported384.returncode != 0:
            print("✗ فشل الاختبار الذاتي: تعذّر توليد مفتاح P-384")
            return 1
        with open(other_curve_pub, "w", encoding="utf-8") as fh:
            fh.write(base64.b64encode(exported384.stdout).decode("ascii"))
        wrong_curve = command_check(argparse.Namespace(public_file=other_curve_pub, private=None, pkcs8_file=None))
        if wrong_curve == 0:
            print("✗ فشل الاختبار الذاتي: مفتاح P-384 مرّ كصالح")
            return 1
        print("✓ الاختبار الذاتي ④: منحنى P-384 ⇒ ✗ رُفض (التطبيق P-256 وحده)")

        # ⑤ حتميّ: نقطة إحداثياتها كلها البايت 0x03 — وهو البايت الذي كان البحث عنه بـ`rfind`
        #    يقع على إحداثي لا على رأس BIT STRING، فتُرفض مفاتيح سليمة. وقع هذا فعلًا في أول
        #    تشغيل حقيقي للأداة (وأمسكه هذا الاختبار)؛ وهذه الحالة تثبّت العطب ميتًا للأبد.
        crafted_point = b"\x04" + bytes([0x03]) * 32 + bytes([0x03]) * 32
        crafted = _spki_from_point(crafted_point)
        try:
            read_back = _point_from_spki(crafted)
        except ValueError as exc:
            print(f"✗ فشل الاختبار الذاتي: نقطة فيها 0x03 لم تُقرأ ({exc})")
            return 1
        if read_back != crafted_point:
            print("✗ فشل الاختبار الذاتي: النقطة المقروءة ليست النقطة المصنوعة")
            return 1
        print("✓ الاختبار الذاتي ⑤: نقطة كلها 0x03 ⇒ تُقرأ كاملة ٦٥ بايت (قراءة بالمواضع لا بالبحث)")

        # ⑥ عشوائيّ مُكرَّر: أربعة أزواج حقيقية من `openssl` — تغطّي أصنافًا لا يصنعها ⑤ وحده.
        for index in range(4):
            pem, pub = keypair(f"loop{index}")
            loop_code = command_check(argparse.Namespace(public_file=pub, private=pem, pkcs8_file=None))
            if loop_code != 0:
                print(f"✗ فشل الاختبار الذاتي: الزوج العشوائيّ {index + 1} لم يمرّ")
                return 1
        print("✓ الاختبار الذاتي ⑥: أربعة أزواج حقيقية متتالية ⇒ كلها ✓")

        print("\n✓ الأداة تفرّق: تمرّ على الصحيح وترسب على الخطأ")
        return 0


def main() -> int:
    parser = argparse.ArgumentParser(
        description="فحص مفتاح التوقيع العامّ (P-256) ومطابقته للمفتاح الخاصّ — بلا طباعة أي سرّ.",
    )
    parser.add_argument("--self-test", action="store_true", help="تقيس الأداة نفسها على زوج مؤقّت")
    sub = parser.add_subparsers(dest="command")

    check = sub.add_parser("check", help="فحص المفتاح العامّ (+ المطابقة إن أُعطي الخاصّ)")
    check.add_argument("--public-file", required=True, help="ملفّ base64 للمفتاح العامّ (SPKI DER)")
    check.add_argument("--private", help="ملفّ PEM للمفتاح الخاصّ (لا يُطبع منه شيء)")
    check.add_argument("--pkcs8-file", help="ملفّ base64 للمفتاح الخاصّ PKCS#8 DER (سرّ Supabase)")

    args = parser.parse_args()
    if args.self_test:
        return self_test()
    if args.command == "check":
        if args.private and args.pkcs8_file:
            print("✗ اختر واحدًا: --private أو --pkcs8-file")
            return 2
        return command_check(args)
    parser.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main())
