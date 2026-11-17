#!/usr/bin/env python3
# Copyright (C) 2026 Nader Magdy. All rights reserved.
# Proprietary and confidential — not licensed for use, copying, or distribution
# without prior written permission from the copyright holder.
"""إثبات إصدار موقّع (Ed25519) — يربط حزمةً منشورة بسجلٍّ لا يُعدَّل بصمت.

## ما يضيفه على `build_watermark.py`

البصمة وحدها تقول «هذه الحزمة تحمل تعبيرنا». والسجلّ الموقّع يقول أيضًا **«وهذه هي الحزمة
التي أَصدرناها نحن، بهذا التجزئة، ولا تُعدَّل بعده»** — لأن التوقيع يغطّي التجزئة والالتزام
والبصمة معًا. والفرق عمليّ: من ينسخ كودنا ثم يُبقي البصمة يُثبت أنه أخذ التعبير، ومن يزعم أن
**نحن** أَصدرنا نسخته المعدَّلة لا يستطيع، لأن تجزئة حزمته لا تطابق الموقَّع.

## المفاتيح — وما لا يُفعل

* **الخاصّ لا يدخل المستودع ولا الحزمة أبدًا** (طلب المالك، `ADR-95/5`): يعيش في سرّ CI
  (`PROVENANCE_ED25519_KEY`) أو في ملفّ على جهاز الإصدار خارج الشجرة.
* **العامّ يُنشر**: `tools/provenance_pubkey.pem` في المستودع، ويُوزَّع مع السجلّ.
* والتوقيع **لا يمنع** شيئًا: هو دليلٌ يُتحقَّق منه، لا قفلٌ يمنع النسخ.

## وحدوده المُعلَن

* التوقيع يثبت **من أصدر**، لا **من نسخ**. أمّا «من نسخ» فيُستدلّ عليه ببقاء البصمة.
* ولا يحمي المفتاحَ الخاصّ من سُرقة جهاز الإصدار؛ ولا يُثبت أن الكود خالٍ من عيب.
* و`openssl` مطلوب: بغيابه يُعلن «غير مُتحقَّق» ولا يُدَّعى نجاح.

الاستعمال:

    # مرة واحدة، على جهاز الإصدار (لا هنا، ولا يُدفع الناتج):
    python3 tools/provenance_sign.py --keygen --out /path/outside/repo

    # بعد بناء حزمة الإصدار:
    PROVENANCE_ED25519_KEY=... python3 tools/provenance_sign.py \\
        --sign app-release.apk --version V5.0 --commit "$(git rev-parse HEAD)"

    # عند فحص حزمة منشورة:
    python3 tools/provenance_sign.py --verify app-release.apk

    python3 tools/provenance_sign.py --self-test
"""

from __future__ import annotations

import argparse
import datetime
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PUBKEY = os.path.join(HERE, "provenance_pubkey.pem")
RECORDS = os.path.join(ROOT, "provenance")
LEDGER = os.path.join(ROOT, "docs", "ai", "PROVENANCE-LEDGER.md")

sys.path.insert(0, HERE)
import build_watermark  # noqa: E402  — تقرأ البصمة من الحزمة نفسها، فلا مصدر حقيقة ثانٍ


def openssl() -> str | None:
    return shutil.which("openssl")


def run(args: list[str], stdin: bytes | None = None) -> bytes:
    result = subprocess.run(args, input=stdin, capture_output=True)
    if result.returncode != 0:
        raise RuntimeError(
            f"{' '.join(args[:3])} فشل ({result.returncode}): "
            f"{result.stderr.decode('utf-8', 'replace')[:400]}"
        )
    return result.stdout


def sha256_file(path: str) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def keygen(out_dir: str) -> int:
    if not openssl():
        print("✗ openssl غير موجود ⇒ التوليد غير مُتحقَّق في هذه البيئة", file=sys.stderr)
        return 2
    os.makedirs(out_dir, exist_ok=True)
    key = os.path.join(out_dir, "provenance_ed25519.key")
    pub = os.path.join(out_dir, "provenance_pubkey.pem")
    if os.path.exists(key):
        print(f"✗ المفتاح موجود سابقًا: {key} — لا يُستبدل صامتًا", file=sys.stderr)
        return 1
    run(["openssl", "genpkey", "-algorithm", "ED25519", "-out", key])
    os.chmod(key, 0o600)
    run(["openssl", "pkey", "-in", key, "-pubout", "-out", pub])
    print(f"✓ المفتاح الخاصّ: {key} (صلاحية ٦٠٠ · **لا يدخل المستودع ولا الحزمة**)")
    print(f"✓ المفتاح العامّ: {pub} — انسخه إلى tools/provenance_pubkey.pem")
    print(f"· بصمة المفتاح العامّ: {fingerprint(pub)}")
    return 0


def fingerprint(pub_path: str) -> str:
    der = run(["openssl", "pkey", "-pubin", "-in", pub_path, "-outform", "DER"])
    return "sha256:" + hashlib.sha256(der).hexdigest()[:16]


def read_key(explicit: str | None) -> str:
    if explicit:
        return explicit
    env = os.environ.get("PROVENANCE_ED25519_KEY", "").strip()
    if not env:
        raise SystemExit("✗ لا مفتاح: مرّر --key أو عيّن PROVENANCE_ED25519_KEY")
    handle = tempfile.NamedTemporaryFile("w", suffix=".pem", delete=False)
    handle.write(env if env.endswith("\n") else env + "\n")
    handle.close()
    os.chmod(handle.name, 0o600)
    return handle.name


def mark_of(apk: str) -> str:
    hits = build_watermark.scan(apk)
    if len(hits) != 1:
        raise SystemExit(
            f"✗ عدد البصمات في الحزمة {len(hits)} — المتوقَّع واحدة. "
            "بناءٌ بلا بصمة أو ببصمات متعدّدة لا يُوقَّع."
        )
    return next(iter(hits))


def sign(apk: str, version: str, commit: str, key_path: str) -> int:
    if not openssl():
        print("✗ openssl غير موجود ⇒ التوقيع غير مُتحقَّق", file=sys.stderr)
        return 2
    if not os.path.isfile(apk):
        raise SystemExit(f"✗ الحزمة غير موجودة: {apk}")
    os.makedirs(RECORDS, exist_ok=True)
    mark = mark_of(apk)
    record = {
        "version": version,
        "buildMark": mark,
        "apkSha256": sha256_file(apk),
        "apkBytes": os.path.getsize(apk),
        "commit": commit,
        "signedAt": datetime.datetime.now(datetime.timezone.utc)
        .replace(microsecond=0).isoformat(),
        "signer": "ed25519",
        "pubkeyFingerprint": fingerprint(PUBKEY) if os.path.isfile(PUBKEY) else "unknown",
        "note": "الأفكار لا تُحمى؛ التعبير يُنسب. هذا السجلّ يثبت من أصدر هذه الحزمة.",
    }
    body = (json.dumps(record, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode("utf-8")
    base = os.path.join(RECORDS, f"{version}-{mark}")
    with open(base + ".json", "wb") as handle:
        handle.write(body)
    run(["openssl", "pkeyutl", "-sign", "-inkey", key_path, "-rawin",
         "-in", base + ".json", "-out", base + ".sig"])

    # **تحقّق فوريّ بعد التوقيع** — توقيعٌ لم يُتحقَّق منه ليس توقيعًا، بل ملفًّا.
    public = PUBKEY if os.path.isfile(PUBKEY) else None
    if verify(apk, base + ".json", base + ".sig", public, quiet=True) != 0:
        print("✗ التوقيع كُتب لكنه لم يُتحقَّق ⇒ لا يُسلَّم ولا يُسجَّل", file=sys.stderr)
        return 1

    append_ledger(record, os.path.basename(base) + ".json")
    print(f"✓ وُقِّع: {base}.json · {base}.sig")
    print(f"· التجزئة: {record['apkSha256'][:16]} · البصمة: {mark} · المفتاح: {record['pubkeyFingerprint']}")
    return 0


def append_ledger(record: dict, record_name: str) -> None:
    row = (f"| {record['signedAt'][:10]} | {record['version']} | {record['buildMark']} | "
           f"`{record['apkSha256'][:16]}` | `{record['commit'][:12]}` | `provenance/{record_name}` |")
    rows = build_watermark.ledger_rows()
    if any(item["mark"] == record["buildMark"] for item in rows):
        print(f"· البصمة مسجّلة سابقًا في السجلّ: {record['buildMark']}")
        return
    with open(LEDGER, "a", encoding="utf-8") as handle:
        handle.write(row + "\n")


def verify(apk: str, record_path: str, sig_path: str, pubkey: str | None,
           quiet: bool = False) -> int:
    def check(ok: bool, label: str) -> bool:
        if not quiet:
            print(f"  {'✓' if ok else '✗'} {label}")
        return ok

    if not openssl():
        print("✗ openssl غير موجود ⇒ التحقّق غير مُتحقَّق في هذه البيئة", file=sys.stderr)
        return 2
    if pubkey is None or not os.path.isfile(pubkey):
        if not quiet:
            print(f"✗ لا مفتاح عامّ ({PUBKEY}) ⇒ التحقّق غير مُتحقَّق. "
                  "ولا يُقرأ غيابه «توقيعًا صحيحًا».", file=sys.stderr)
        return 2

    ok = True
    try:
        with open(record_path, "rb") as handle:
            raw = handle.read()
        record = json.loads(raw.decode("utf-8"))
    except (OSError, ValueError) as exc:
        print(f"✗ سجلّ غير مقروء: {exc}", file=sys.stderr)
        return 1

    try:
        run(["openssl", "pkeyutl", "-verify", "-pubin", "-inkey", pubkey, "-rawin",
             "-in", record_path, "-sigfile", sig_path])
        ok &= check(True, "التوقيع صحيح على بايتات السجلّ كما هي")
    except RuntimeError:
        ok &= check(False, "التوقيع لا يطابق السجلّ (عُدِّل بعده أو مفتاح آخر)")

    if os.path.isfile(apk):
        digest = sha256_file(apk)
        ok &= check(digest == record.get("apkSha256"),
                    f"تجزئة الحزمة تطابق السجلّ ({digest[:16]})")
        try:
            ok &= check(mark_of(apk) == record.get("buildMark"),
                        f"بصمة الحزمة تطابق السجلّ ({record.get('buildMark')})")
        except SystemExit as exc:
            ok &= check(False, f"تعذّر استخراج البصمة: {exc}")

    if not quiet and ok:
        known = [row for row in build_watermark.ledger_rows()
                 if row["mark"] == record.get("buildMark")]
        check(bool(known), "البصمة مذكورة في سجلّ النسب (PROVENANCE-LEDGER)")
    return 0 if ok else 1


def self_test() -> int:
    failures: list[str] = []

    def check(condition: bool, label: str) -> None:
        print(f"  {'✓' if condition else '✗'} {label}")
        if not condition:
            failures.append(label)

    if not openssl():
        print("· openssl غائب ⇒ الفحص الذاتي **غير مُتحقَّق** في هذه البيئة (لا يُدَّعى نجاح)")
        return 0

    mark = "MMPROV-0123456789abcdef"
    with tempfile.TemporaryDirectory() as tmp:
        keys = os.path.join(tmp, "keys")
        keygen(keys)
        key = os.path.join(keys, "provenance_ed25519.key")
        pub = os.path.join(keys, "provenance_pubkey.pem")

        apk = os.path.join(tmp, "probe.apk")
        with zipfile.ZipFile(apk, "w") as archive:
            archive.writestr("classes.dex", b"\x00" + mark.encode("ascii") + b"\x00")
        check(mark_of(apk) == mark, "تستخرج البصمة من الحزمة لتوقّعها")

        record = os.path.join(tmp, "rec.json")
        sig = os.path.join(tmp, "rec.sig")
        body = json.dumps({"version": "V0", "buildMark": mark, "apkSha256": sha256_file(apk)},
                          sort_keys=True).encode()
        with open(record, "wb") as handle:
            handle.write(body)
        run(["openssl", "pkeyutl", "-sign", "-inkey", key, "-rawin",
             "-in", record, "-out", sig])
        check(verify(apk, record, sig, pub, quiet=True) == 0, "توقيع صحيح يُقبل")

        tampered = os.path.join(tmp, "tampered.json")
        with open(tampered, "wb") as handle:
            handle.write(body.replace(b"V0", b"V1"))
        check(verify(apk, tampered, sig, pub, quiet=True) == 1, "سجلّ مُعدَّل يُرفض")

        other = os.path.join(tmp, "other.apk")
        with zipfile.ZipFile(other, "w") as archive:
            archive.writestr("classes.dex", b"nothing here")
        check(verify(other, record, sig, pub, quiet=True) == 1, "تجزئة حزمة أخرى تُرفض")

        wrong_pub = os.path.join(tmp, "wrong.pem")
        run(["openssl", "genpkey", "-algorithm", "ED25519", "-out", os.path.join(tmp, "k2.pem")])
        run(["openssl", "pkey", "-in", os.path.join(tmp, "k2.pem"), "-pubout", "-out", wrong_pub])
        check(verify(apk, record, sig, wrong_pub, quiet=True) == 1, "مفتاح عامّ آخر يرفض التوقيع")

        check(verify(apk, record, sig, os.path.join(tmp, "absent.pem"), quiet=True) == 2,
              "غياب المفتاح العامّ يُعلن «غير مُتحقَّق» لا «صحيح»")

    if failures:
        print(f"✗ فشل الفحص الذاتي: {len(failures)}")
        return 1
    print("✓ الفحص الذاتي: ٦ حالات، كلها بالحكم المتوقّع")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="إثبات إصدار موقّع (Ed25519)")
    parser.add_argument("--keygen", action="store_true", help="توليد زوج مفاتيح في --out")
    parser.add_argument("--out", help="مجلّد إخراج المفاتيح (خارج المستودع)")
    parser.add_argument("--sign", metavar="APK", help="توقيع سجلّ إصدار لحزمة")
    parser.add_argument("--version", default="", help="اسم الإصدار (عند التوقيع)")
    parser.add_argument("--commit", default="", help="الالتزام الذي بُنيت منه الحزمة")
    parser.add_argument("--key", help="مسار المفتاح الخاصّ (أو PROVENANCE_ED25519_KEY)")
    parser.add_argument("--verify", metavar="APK", help="التحقّق من حزمة وسجلّها")
    parser.add_argument("--record", help="مسار السجلّ (افتراضيّ: يُستنتج من بصمة الحزمة)")
    parser.add_argument("--sig", help="مسار التوقيع")
    parser.add_argument("--pubkey", default=PUBKEY, help="المفتاح العامّ")
    parser.add_argument("--self-test", action="store_true", dest="self_test")
    args = parser.parse_args()

    if args.self_test:
        return self_test()
    if args.keygen:
        if not args.out:
            print("✗ --keygen يحتاج --out (خارج المستودع)", file=sys.stderr)
            return 2
        return keygen(args.out)
    if args.sign:
        if not args.version or not args.commit:
            print("✗ --sign يحتاج --version و--commit (سجلٌّ بلا نسبٍ لا قيمة له)", file=sys.stderr)
            return 2
        return sign(args.sign, args.version, args.commit, read_key(args.key))
    if args.verify:
        mark = None
        try:
            mark = mark_of(args.verify)
        except SystemExit:
            pass
        record = args.record
        sig = args.sig
        if record is None and mark:
            # السجلّ يُسمّى `<الإصدار>-<البصمة>.json`؛ فبلا `--version` يُبحث بالبصمة وحدها —
            # البصمة فريدة لكل إصدار، فلا يلزم تخمين الاسم.
            candidates = []
            if os.path.isdir(RECORDS):
                candidates = sorted(
                    os.path.join(RECORDS, name)
                    for name in os.listdir(RECORDS)
                    if name.endswith(f"-{mark}.json")
                )
            if candidates:
                record = candidates[-1]
                sig = record[:-5] + ".sig"
        if not record or not os.path.isfile(record):
            print(f"✗ لا سجلّ لهذه الحزمة ({record}) ⇒ **غير مُتحقَّق**", file=sys.stderr)
            return 2
        return verify(args.verify, record, sig or record[:-5] + ".sig", args.pubkey)
    parser.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main())
