#!/usr/bin/env python3
"""نشر إصدار MaxManager بأمر واحد — يملأ `docs/update/manifest.json` من ملفّ APK حقيقي.

**لماذا هذه الأداة:** المانيفست هو ما يقرأه تطبيقٌ مثبَّت على أجهزة لا نراها، وأخطاؤه صامتة:
بصمةٌ منقولة من بناءٍ آخر تُسقط التحديث عند المستخدم لا عندنا، وحجمٌ ناقص يجعل الزرّ يقود إلى
ملفّ نصفه مُنزَّل، ورقمُ نسخةٍ لا يطابق الحزمة يُعلن تحديثًا لا يوجد. فالأداة تحسب هذه الثلاثة
**من الملفّ نفسه** ولا تقبل أن تُكتب بيد.

**الاستعمال الأقصر** (بعد رفع APK في GitHub Releases):

    python3 tools/publish_update.py \\
        --apk app/build/outputs/apk/release/MaxManager-V2.apk \\
        --release owner/MaxManager --tag V2 \\
        --notes-en "…" --notes-ar "…"

وتقبل البديل الصريح `--url https://…/MaxManager-V2.apk` لمن يستضيف الملفّ في مكان آخر.

**ما لا تفعله:** لا ترفع شيئًا ولا تستعمل أي مفتاح ولا تتّصل بالشبكة. الرفع فعلُ المالك
(أو `gh release upload`)، والأداة تكتب الملفّ وتقول الخطوة التالية بالحرف. ولا سرّ يُكتب في
أي ملفّ من هذه الأداة — ولا يُقرأ لها سرّ أصلًا.

**والحكم النهائي ليس هنا:** هذه أداة تحضير، والذي يقيس صحّة المانيفست هو حرّاس التطبيق:

    python3 tools/test_update_jvm.py     # يقرأ docs/update/manifest.json بالعقد نفسه
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = ROOT / "docs/update/manifest.json"


def find_aapt2() -> str | None:
    """يبحث عن `aapt2` حيث يبحث عنه البناء: SDK من البيئة، ثم أحدث build-tools، ثم PATH."""
    roots = [
        os.environ.get("ANDROID_HOME"),
        os.environ.get("ANDROID_SDK_ROOT"),
        os.environ.get("ANDROID_SDK"),
        os.path.expanduser("~/android-sdk"),
    ]
    candidates: list[str] = []
    for root in roots:
        if not root:
            continue
        base = Path(root) / "build-tools"
        if base.is_dir():
            candidates.extend(str(p) for p in base.glob("*/aapt2"))
            candidates.extend(str(p) for p in base.glob("*/aapt2.exe"))

    def version_of(path: str) -> tuple:
        parent = Path(path).parent.name
        return tuple(int(part) if part.isdigit() else 0 for part in re.split(r"[.\-]", parent))

    usable = [path for path in candidates if os.access(path, os.X_OK)]
    if usable:
        return max(usable, key=version_of)
    return shutil.which("aapt2")


def apk_identity(apk: Path) -> tuple[int, str] | None:
    """`versionCode`/`versionName` من داخل الحزمة — أو `None` إن تعذّر القياس (ولا يُدّعى)."""
    aapt2 = find_aapt2()
    if aapt2 is None:
        return None
    try:
        out = subprocess.run(
            [aapt2, "dump", "badging", str(apk)],
            capture_output=True,
            text=True,
            timeout=120,
        ).stdout
    except (OSError, subprocess.SubprocessError):
        return None
    code = re.search(r"versionCode='(\d+)'", out)
    name = re.search(r"versionName='([^']*)'", out)
    if not code or not name:
        return None
    return int(code.group(1)), name.group(1)


def sha256_of(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_manifest(path: Path) -> dict:
    if not path.is_file():
        return {"schema": 1}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        raise SystemExit(f"مانيفست قائم لا يُقرأ ({path}): {error}") from error


def release_url(args) -> tuple[str, str]:
    """رابط التنزيل و(صفحة الإصدار) — من `--url` صراحةً أو من قواعد GitHub Releases."""
    if args.url:
        return args.url, args.page or ""
    owner_repo = args.release
    tag = args.tag
    if not owner_repo or not tag:
        raise SystemExit("مطلوب: --release owner/repo مع --tag vX.Y (أو --url صريح)")
    if "/" not in owner_repo:
        raise SystemExit(f"صيغة المستودع غير صحيحة: {owner_repo} (المتوقّع owner/repo)")
    filename = Path(args.apk).name
    if args.asset_name:
        filename = args.asset_name
    download = f"https://github.com/{owner_repo}/releases/download/{tag}/{filename}"
    page = args.page or f"https://github.com/{owner_repo}/releases/tag/{tag}"
    return download, page


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--apk", required=True, type=Path, help="ملفّ APK المُوقَّع المنشور")
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST, help="الملفّ الذي يُحدَّث")
    parser.add_argument("--release", help="owner/repo على GitHub (لاشتقاق رابط التنزيل)")
    parser.add_argument("--tag", help="وسم الإصدار، مثل V2")
    parser.add_argument("--url", help="رابط تنزيل https صريح (بديل --release/--tag)")
    parser.add_argument("--page", help="صفحة الإصدار المعروضة للمستخدم")
    parser.add_argument("--asset-name", help="اسم الملفّ في صفحة الإصدار إن خالف اسم الملفّ المحلّيّ")
    parser.add_argument("--code", type=int, help="versionCode (يُقرأ من الحزمة إن أمكن)")
    parser.add_argument("--name", help="versionName (يُقرأ من الحزمة إن أمكن)")
    parser.add_argument("--channel", default="stable", help="القناة: stable (الافتراضية) أو beta أو غيرها")
    parser.add_argument("--notes-en", help="ملاحظات الإصدار بالإنجليزية")
    parser.add_argument("--notes-ar", help="ملاحظات الإصدار بالعربية")
    parser.add_argument("--force", action="store_true", help="تحديث مُلزم (النافذة لا تُغلق)")
    parser.add_argument("--min-code", type=int, help="أرضيّة: من كان تحتها محجوب حتى يحدّث")
    parser.add_argument("--min-notes-en", help="نصّ الحجب بالإنجليزية")
    parser.add_argument("--min-notes-ar", help="نصّ الحجب بالعربية")
    parser.add_argument("--maintenance", choices=["on", "off"], help="تعليق مؤقّت للتطبيق")
    parser.add_argument("--maintenance-blocking", choices=["on", "off"], help="هل التعليق يحجب؟")
    parser.add_argument("--dry-run", action="store_true", help="اطبع الناتج ولا تكتبه")
    args = parser.parse_args()

    apk: Path = args.apk
    if not apk.is_file():
        raise SystemExit(f"لا ملفّ APK هنا: {apk}")

    # (١) القياس من الملفّ نفسه: بصمة وحجم لا يُكتبان بيد أبدًا.
    digest = sha256_of(apk)
    size = apk.stat().st_size
    identity = apk_identity(apk)

    code = args.code
    name = args.name
    if identity is not None:
        apk_code, apk_name = identity
        code = apk_code if code is None else code
        name = apk_name if name is None else name
        if args.code is not None and args.code != apk_code:
            raise SystemExit(f"--code={args.code} لا يطابق الحزمة ({apk_code}) — لا يُنشر رقم مخالف")
        if args.name is not None and args.name != apk_name:
            raise SystemExit(f"--name={args.name} لا يطابق الحزمة ({apk_name})")
    else:
        print("تنبيه: تعذّر قراءة رقم الحزمة (لا aapt2) — يُلزم --code و--name ولا يُدّعى التحقّق.")

    if code is None or name is None:
        raise SystemExit("مطلوب --code و--name (تعذّر قياسهما من الحزمة)")

    url, page = release_url(args)
    if not url.startswith("https://"):
        raise SystemExit(f"رابط التنزيل يجب أن يكون https: {url}")

    # (٢) التعديل على القائم لا استبداله: حقلٌ يضيفه المالك غدًا لا يُمحى بأداة تحضير.
    manifest = load_manifest(args.manifest)
    manifest.setdefault("schema", 1)
    notes = {}
    if args.notes_en:
        notes["en"] = args.notes_en
    if args.notes_ar:
        notes["ar"] = args.notes_ar
    # ‏ADR-14/§0.2: النصّان يُكتبان معًا أو لا يُكتبان — مفتاحٌ واحد يُترك للّغة الأخرى نصًّا ناقصًا.
    if len(notes) == 1:
        raise SystemExit("اكتب --notes-en و--notes-ar معًا (ADR-14)، أو اتركهما ليُحفظ النصّ السابق")

    release = {
        "code": code,
        "name": name,
        "url": url,
        "sha256": digest,
        "size": size,
        "notes": notes or (load_manifest(args.manifest).get("latest", {}).get("notes") or {}),
        "force": bool(args.force),
    }
    if page:
        release["page"] = page

    if args.channel == "stable":
        manifest["latest"] = release
    else:
        channels = manifest.setdefault("channels", {})
        channels[args.channel] = release

    if args.min_code is not None:
        floor_notes = {}
        if args.min_notes_en:
            floor_notes["en"] = args.min_notes_en
        if args.min_notes_ar:
            floor_notes["ar"] = args.min_notes_ar
        if len(floor_notes) == 1:
            raise SystemExit("نصّ الحجب يُكتب بلغتينا معًا: --min-notes-en و--min-notes-ar")
        manifest["minimum"] = {"code": args.min_code, "notes": floor_notes}
    if args.maintenance is not None:
        active = args.maintenance == "on"
        blocking = args.maintenance_blocking == "on" if args.maintenance_blocking else False
        if blocking and not active:
            raise SystemExit("--maintenance-blocking on بلا --maintenance on: حجبٌ لا يسقطه أحد")
        previous = (manifest.get("maintenance") or {}).get("notes") or {}
        manifest["maintenance"] = {
            "active": active,
            "blocking": blocking,
            "notes": previous,
        }

    text = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    if args.dry_run:
        print(text)
        print("(--dry-run: لم يُكتب الملفّ)")
        return 0

    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(text, encoding="utf-8")

    floor = (manifest.get("minimum") or {}).get("code", 0) or 0
    print(f"كُتب {args.manifest.relative_to(ROOT) if args.manifest.is_relative_to(ROOT) else args.manifest}")
    print(f"  القناة: {args.channel} · الإصدار: {code} ({name})")
    print(f"  الحجم: {size} بايت · البصمة: {digest[:16]}…")
    print(f"  التنزيل: {url}")
    if floor:
        print(f"  الأرضيّة: {floor} — من كان تحتها محجوب حتى يحدّث")
    if args.force:
        print("  مُلزم: النافذة لا تُغلق عند المستخدم")
    print()
    print("الخطوة التالية (بيدك — لا ترفع هذه الأداة شيئًا):")
    print(f"  1) ارفع الحزمة إلى الإصدار: gh release upload {args.tag or '<tag>'} {apk}")
    print(f"  2) انشر المانيفست: {args.manifest}")
    print("  3) قِس ما نشرت: python3 tools/test_update_jvm.py")
    print()
    print("وأيّ إصدار لاحق يحتاج بناءً يشير إلى رابط المانيفست:")
    print("  sh gradlew :app:assembleRelease -Pmaxmanager.updateManifestUrl=<رابط المانيفست>")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
