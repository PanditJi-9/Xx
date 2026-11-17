#!/usr/bin/env python3
# Copyright (C) 2026 Nader Magdy. All rights reserved.
# Proprietary and confidential — not licensed for use, copying, or distribution
# without prior written permission from the copyright holder.
"""بصمة الأصل — تقرأ `MMPROV-…` من حزمة أو مجلّد، وتقابلها بسجلّ النسب.

## ما تحلّه هذه الأداة، وما لا تحلّه

`ADR-95` فصل بين ثلاثة أهداف: منع القراءة (**مستحيل**)، رفع كلفتها (**ممكن**)، وإثبات الاقتباس
(**ممكن، وهذا موضع هذه الأداة**). والبصمة التي يولّدها `build.gradle.kts`
(`BUILD_MARK`) ثابتة لكل إصدار ومشتقّة من اسمه ورمزه وزمن التزام `git`؛ فإن ظهرت حزمة
مشكوك فيها تحملها، فالنقل **ثابت** لا مُدَّعى. وإن حُذفت فالحذف نفسه يحتاج انتباهًا وعملًا.

## وحدوده المُعلَن

* **لا يمنع شيئًا:** لا تشفير ولا تحقّق؛ قراءة فقط.
* **ولا يكشف «نسخ فكرة»** — بل «نسخ تعبير» (كود/أصل حُزم منّا).
* ومسح UTF-16 تقريبيّ: تُزال البايتات الصفرية ثم يُطلب النمط الصارم (١٦ خانة hex)، فالتصادم
  العابر غير واقعيّ لكنه ليس مستحيلًا — والأداة تقول ما وجدت ولا تستنتج نيّة.

الاستعمال:

    python3 tools/build_watermark.py --scan app-release.apk
    python3 tools/build_watermark.py --scan app/build/intermediates/dex/release/minifyReleaseWithR8
    python3 tools/build_watermark.py --record app-release.apk --version V5.0 --commit 61d26800
    python3 tools/build_watermark.py --self-test
"""

from __future__ import annotations

import argparse
import datetime
import hashlib
import os
import re
import sys
import tempfile
import zipfile

MARK_RE = re.compile(rb"MMPROV-[0-9a-f]{16}")
LEDGER = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "docs", "ai", "PROVENANCE-LEDGER.md")
MAX_ENTRY_BYTES = 96 * 1024 * 1024


def marks_in_blob(blob: bytes) -> set[str]:
    """البصمات في كتلة بايت — بصيغتَي UTF-8 وUTF-16LE.

    DEX يخزّن النصّ MUTF-8 (فالبحث المباشر يعمل)، و`resources.arsc` قد يخزّنه UTF-16:
    فتُزال البايتات الصفرية قبل البحث الثاني. وحدّه: إزالة الصفر تُقرّب لا تُخمّن — والنمط
    صارم (١٦ خانة hex بعد `MMPROV-`) فلا يمرّ نصّ عابر.
    """
    found = {m.decode("ascii") for m in MARK_RE.findall(blob)}
    # UTF-16LE: كلّ محرف ASCII تليه بايت صفرية ⇒ تُزال ثم يُعاد البحث بالنمط الصارم نفسه.
    stripped = blob.replace(b"\x00", b"")
    found |= {m.decode("ascii") for m in MARK_RE.findall(stripped)}
    return found


def scan_apk(path: str) -> dict[str, list[str]]:
    """بصمات كلّ مدخل في الحزمة، مع اسم المدخل — ليُعرف موضعها لا وجودها فقط."""
    hits: dict[str, list[str]] = {}
    with zipfile.ZipFile(path) as archive:
        for info in archive.infolist():
            if info.file_size <= 0 or info.file_size > MAX_ENTRY_BYTES:
                continue
            try:
                blob = archive.read(info.filename)
            except (OSError, zipfile.BadZipFile, RuntimeError):
                continue
            for mark in marks_in_blob(blob):
                hits.setdefault(mark, []).append(info.filename)
    return hits


def scan_dir(path: str) -> dict[str, list[str]]:
    """مجلّد DEX خام (مخرَج R8) أو أي شجرة ملفّات صغيرة."""
    hits: dict[str, list[str]] = {}
    for root, _dirs, files in os.walk(path):
        for name in files:
            full = os.path.join(root, name)
            try:
                if not (os.path.isfile(full) and os.path.getsize(full) <= MAX_ENTRY_BYTES):
                    continue
                with open(full, "rb") as handle:
                    blob = handle.read()
            except OSError:
                continue
            for mark in marks_in_blob(blob):
                hits.setdefault(mark, []).append(os.path.relpath(full, path))
    return hits


def scan(path: str) -> dict[str, list[str]]:
    if os.path.isdir(path):
        return scan_dir(path)
    if not os.path.isfile(path):
        raise SystemExit(f"✗ المسار غير موجود: {path}")
    if zipfile.is_zipfile(path):
        return scan_apk(path)
    with open(path, "rb") as handle:
        blob = handle.read(MAX_ENTRY_BYTES)
    return {mark: [os.path.basename(path)] for mark in marks_in_blob(blob)}


def ledger_rows(path: str = LEDGER) -> list[dict[str, str]]:
    """صفوف السجلّ: `| التاريخ | الإصدار | البصمة | بصمة الحزمة | الالتزام | [السجلّ الموقّع] |`.

    والعمود السادس اختياريّ عن قصد: صفوف ما قبل التوقيع تبقى صحيحة ولا تُعاد كتابتها.
    """
    if not os.path.isfile(path):
        return []
    rows: list[dict[str, str]] = []
    with open(path, "r", encoding="utf-8") as handle:
        for line in handle:
            cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
            if len(cells) not in (5, 6) or not cells[2].startswith("MMPROV-"):
                continue
            rows.append({
                "date": cells[0], "version": cells[1], "mark": cells[2],
                "apk": cells[3], "commit": cells[4],
                "record": cells[5] if len(cells) == 6 else "",
            })
    return rows


def file_sha256(path: str) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def record(path: str, version: str, commit: str) -> int:
    hits = scan(path)
    if len(hits) != 1:
        print(f"✗ عدد البصمات {len(hits)} في {os.path.basename(path)} — المتوقَّع واحدة. "
              "الميزانية: بناءٌ بلا بصمة أو ببصمات متعدّدة لا يُسجَّل.", file=sys.stderr)
        return 1
    mark = next(iter(hits))
    row = (f"| {datetime.date.today().isoformat()} | {version} | {mark} | "
           f"`{file_sha256(path)[:16]}` | `{commit[:12]}` |")
    existing = ledger_rows()
    if any(item["mark"] == mark for item in existing):
        print(f"· البصمة مسجّلة سابقًا: {mark}")
        return 0
    with open(LEDGER, "a", encoding="utf-8") as handle:
        handle.write(row + "\n")
    print(f"✓ سُجّلت: {mark} · {version} · {row}")
    return 0


def self_test() -> int:
    failures: list[str] = []

    def check(condition: bool, label: str) -> None:
        print(f"  {'✓' if condition else '✗'} {label}")
        if not condition:
            failures.append(label)

    mark_a = "MMPROV-0123456789abcdef"
    mark_b = "MMPROV-fedcba9876543210"
    with tempfile.TemporaryDirectory() as tmp:
        apk = os.path.join(tmp, "probe.apk")
        with zipfile.ZipFile(apk, "w") as archive:
            archive.writestr("classes.dex", b"\x00\x01" + mark_a.encode("ascii") + b"\x00")
            archive.writestr("resources.arsc", mark_b.encode("utf-16-le"))
            archive.writestr("res/raw/plain.bin", b"MMPROV-not-a-mark")
        hits = scan_apk(apk)
        check(set(hits) == {mark_a, mark_b}, "يقرأ UTF-8 وUTF-16LE، ويرفض ما ليس بصمة")
        check(hits.get(mark_a) == ["classes.dex"], "يسمّي الموضع الذي وُجدت فيه")
        check("MMPROV-not-a-mark" not in hits, "النمط الصارم لا يقبل حروفًا غير hex")

        raw = os.path.join(tmp, "dexdir")
        os.makedirs(raw)
        with open(os.path.join(raw, "classes.dex"), "wb") as handle:
            handle.write(b"junk" + mark_a.encode("ascii"))
        check(scan_dir(raw) == {mark_a: ["classes.dex"]}, "يقرأ مجلّد DEX خام (مخرَج R8)")

        empty = os.path.join(tmp, "empty.apk")
        with zipfile.ZipFile(empty, "w") as archive:
            archive.writestr("classes.dex", b"\x00" * 64)
        check(scan_apk(empty) == {}, "حزمة بلا بصمة تُعلن لا تُخمَّن")

        ledger = os.path.join(tmp, "ledger.md")
        with open(ledger, "w", encoding="utf-8") as handle:
            handle.write("| التاريخ | الإصدار | البصمة | بصمة الحزمة | الالتزام |\n")
            handle.write("|---|---|---|---|---|\n")
            handle.write(f"| 2026-11-17 | V5.0 | {mark_a} | `deadbeefdeadbeef` | `61d26800` |\n")
        rows = ledger_rows(ledger)
        check(len(rows) == 1 and rows[0]["mark"] == mark_a, "يقرأ صفّ السجلّ ويستخرج البصمة")
        check(ledger_rows(os.path.join(tmp, "missing.md")) == [], "سجلّ غائب = صفر صفوف لا سقوط")

    if failures:
        print(f"✗ فشل الفحص الذاتي: {len(failures)}")
        return 1
    print("✓ الفحص الذاتي: ٧ حالات، كلها بالحكم المتوقّع")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="بصمة الأصل MMPROV — قراءة ومقابلة بالسجلّ")
    parser.add_argument("--scan", metavar="PATH", help="حزمة APK أو مجلّد DEX أو ملفّ")
    parser.add_argument("--record", metavar="APK", help="تسجيل بصمة حزمة منشورة في السجلّ")
    parser.add_argument("--version", default="", help="اسم الإصدار عند التسجيل (مثل V5.0)")
    parser.add_argument("--commit", default="", help="الالتزام الذي بُنيت منه الحزمة")
    parser.add_argument("--self-test", action="store_true", dest="self_test",
                        help="يقيس الأداة نفسها على مدخلات معروفة")
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    if args.record:
        if not args.version or not args.commit:
            print("✗ --record يحتاج --version و--commit (صفٌّ بلا نسبٍ لا قيمة له)", file=sys.stderr)
            return 2
        return record(args.record, args.version, args.commit)

    if not args.scan:
        parser.print_help()
        return 2

    hits = scan(args.scan)
    if not hits:
        print(f"· لا بصمة في {args.scan} — إمّا بناءٌ سابق للبصمة، أو حُذفت. "
              "هذا **ليس** حكمًا بالنقل ولا نفيه.")
        return 1

    known = {item["mark"]: item for item in ledger_rows()}
    for mark in sorted(hits):
        where = ", ".join(sorted(set(hits[mark]))[:3])
        row = known.get(mark)
        if row:
            print(f"✓ {mark} · في السجلّ: إصدار {row['version']} · {row['date']} · "
                  f"التزام {row['commit']} — الموضع: {where}")
        else:
            print(f"⚠ {mark} · **غير مسجَّل** في PROVENANCE-LEDGER — الموضع: {where}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
