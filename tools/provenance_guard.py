#!/usr/bin/env python3
"""حراسة الأصل — تمنع دخول شجرة مرجعية أو بصمات أصنافها إلى المستودع.

**المشكلة التي حلّها هذا الملف:** `license_audit.py` يقرأ الترويسة، فيكشف ما **يُصرَّح** بأصله.
و`upstream_similarity.py` يقيس التشابه، فيكشف ما **يُشبه** أصلًا مُمرَّرًا إليه. ويبقى فراغ ثالث:
ملفٌّ **منقول من مرجع بلا ترخيص** لا ترويسة له ولا أصل مُمرَّر في الجولة — فلا بوابة تمسكه.
وهذا الفراغ هو الذي يُقرأ عند المالك «كل شيء أضفته من المشاريع الأخرى ليس بعملي».

**فحصان لا واحد، لأنهما يكشفان حالتين مختلفتين:**

1. **شجرة مرجعية داخل المستودع.** نسخة المرجع تُقرأ من خارج الشجرة (`/tmp/opencode/...` أو
   `build/audit/` المتجاهَل) ولا تُتعقَّب أبدًا. فوجود مسار متعقَّب باسم مرجع = نقل بلا إعلان.
2. **بصمة صنف من المرجع داخل مصدرنا.** أسماء أصناف المرجع ليست مصطلحات عامّة: `FpsWatchSession`
   و`SceneConfigInfo` و`PowerStatSession` لا تُكتب إلا بنقل. فظهورها في `.kt`/`.java`/`.rs`/`.xml`
   دليل نقل — **إلا** في الوثائق، لأن سجلّ الإثبات يجب أن يسمّي ما نفاه.

**وحدّه المُعلَن:** الفحص **اسمي** لا دلالي. من نقل بنية المرجع بأسماء مبدَّلة لا يمسكه هذا الملف؛
يمسكه `upstream_similarity.py` حين يُمرَّر إليه الأصل. فالبوابتان متكاملتان لا بديل إحداهما عن الأخرى،
وغياب الأصل في الجولة يُعلَن «غير مُتحقَّقة» ولا يُدَّعى.

الاستعمال:

    python3 tools/provenance_guard.py --assert      # يفشل عند أي بصمة أو مسار مرجعي
    python3 tools/provenance_guard.py --self-test   # يقيس الحارس على شجرة مصنوعة معلومة النتيجة
    python3 tools/provenance_guard.py --list        # يطبع البصمات المحروسة وأسبابها
"""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
import tempfile

_HERE = os.path.dirname(os.path.abspath(__file__))
_ROOT = os.path.dirname(_HERE)

# ── البصمات المحروسة ────────────────────────────────────────────────────────────
#
# كل مدخل: **الاسم الذي يظهر في المصدر** ثم سبب اختياره. والسبب مكتوب لأن الحارس يُقرأ:
# اسمٌ عامّ مثل `Session` أو `Monitor` يُصيب كودنا المشروع فيُغلق البوابة على نفسها؛ واسمٌ
# مركّب من صنفٍ بعينه لا يظهر إلا بنقل. فالقائمة **ضيِّقة بقصد** وتُوسَّع بقياس لا بحدس.
FINGERPRINTS: dict[str, str] = {
    # مرجع Scene (بلا ترخيص ⇒ صفر كود، أفكار فقط)
    "FpsWatchSession": "صنف جلسة المراقبة في المرجع",
    "SceneConfigInfo": "صنف إعدادات التطبيق في المرجع",
    "PowerStatSession": "صنف جلسة الطاقة في المرجع",
    "MonitorDesigner": "اسم شاشة مصمّم المراقبة في المرجع",
    "fps_clip_transition": "مفتاح نصّي لقصّ الانتقال في المرجع",
    "fps_record_stop_foreground": "مفتاح نصّي لإيقاف التسجيل في المرجع",
    "md_refresh_interval": "مفتاح نصّي لفاصل التحديث في المرجع",
    # مرجع Kernel Manager (بلا ترخيص ⇒ صفر كود)
    "FrancoKernel": "صنف مدير النواة في المرجع",
}

# مجلّدات مرجعية لا تُتعقَّب: نسخة المرجع تعيش خارج الشجرة أو في `build/` المتجاهَل.
REFERENCE_TREES = (
    "scene_n1",
    "scene",
    "fkm",
    "franco-kernel-manager",
    "francokernelmanager",
)

# امتدادات المصدر التي تُفحص بالبصمات. الوثائق (`docs/`) خارج الفحص بقصد: سجلّ الإثبات
# يسمّي ما نفاه، ولو فُحصت لامتنع أن يكتب أحدٌ «هذه الأسماء غير موجودة» بلا أن يُسقط نفسه.
SOURCE_EXTS = (
    ".kt",
    ".java",
    ".rs",
    ".c",
    ".h",
    ".cpp",
    ".aidl",
    ".xml",
    ".gradle",
    ".kts",
    ".toml",
)

SKIP_PREFIXES = ("docs/", "build/", "tools/")


class Finding:
    __slots__ = ("path", "line", "marker", "reason")

    def __init__(self, path: str, line: int, marker: str, reason: str) -> None:
        self.path = path
        self.line = line
        self.marker = marker
        self.reason = reason

    def render(self) -> str:
        where = f"{self.path}:{self.line}" if self.line else self.path
        return f"  ✗ {where} — {self.marker} ({self.reason})"


def tracked_files(root: str) -> list[str]:
    """ملفات git المتعقَّبة فقط. وما ليس متعقَّبًا لا يدخل التوزيع فلا يُحرس."""
    try:
        out = subprocess.run(
            ["git", "-C", root, "ls-files", "-z"],
            check=True,
            capture_output=True,
        ).stdout
    except (OSError, subprocess.CalledProcessError):
        return []
    return [p for p in out.decode("utf-8", "replace").split("\0") if p]


def path_findings(files: list[str]) -> list[Finding]:
    hits: list[Finding] = []
    for rel in files:
        parts = rel.lower().replace("\\", "/").split("/")
        for part in parts:
            if part in REFERENCE_TREES:
                hits.append(Finding(rel, 0, part, "مسار مرجعي متعقَّب داخل المستودع"))
                break
    return hits


def content_findings(root: str, files: list[str]) -> list[Finding]:
    hits: list[Finding] = []
    for rel in files:
        if rel.startswith(SKIP_PREFIXES):
            continue
        if not rel.endswith(SOURCE_EXTS):
            continue
        path = os.path.join(root, rel)
        try:
            with open(path, encoding="utf-8", errors="ignore") as fh:
                text = fh.read()
        except OSError:
            continue
        for marker, reason in FINGERPRINTS.items():
            at = text.find(marker)
            if at < 0:
                continue
            line = text.count("\n", 0, at) + 1
            hits.append(Finding(rel, line, marker, reason))
    return hits


def scan(root: str, files: list[str] | None = None) -> list[Finding]:
    """الفحص كاملًا على قائمة ملفات صريحة — والقائمة تُمرَّر في الفحص الذاتي بلا git."""
    if files is None:
        files = tracked_files(root)
    return path_findings(files) + content_findings(root, files)


def self_test() -> int:
    """شجرة مصنوعة معلومة النتيجة: نظيفة تمرّ، ومزروعة تُمسك، ووثيقة تُسمّي ولا تُمسك."""
    cases: list[tuple[str, dict[str, str], int]] = [
        (
            "نظيفة",
            {"app/A.kt": "fun main() { println(1) }\n", "app/B.xml": "<a/>\n"},
            0,
        ),
        (
            "بصمة في مصدر",
            {"app/A.kt": "class X\nval s = FpsWatchSession()\n"},
            1,
        ),
        (
            "بصمة في نصّ موارد",
            {"res/values/strings.xml": '<string name="x">md_refresh_interval</string>\n'},
            1,
        ),
        (
            "الاسم في وثيقة لا يُمسك",
            {"docs/A.md": "FpsWatchSession غير موجود في الشجرة\n"},
            0,
        ),
        (
            "مجلّد مرجعي",
            {"scene_n1/A.java": "class A {}\n"},
            1,
        ),
        (
            "اسم قصير لا يُمسك",
            {"app/C.kt": "class Session(val monitor: Int)\n"},
            0,
        ),
    ]
    failures = 0
    for name, tree, expected in cases:
        with tempfile.TemporaryDirectory() as tmp:
            for rel, body in tree.items():
                path = os.path.join(tmp, rel)
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "w", encoding="utf-8") as fh:
                    fh.write(body)
            got = len(scan(tmp, list(tree)))
        ok = got == expected
        failures += 0 if ok else 1
        print(f"  {'✓' if ok else '✗'} {name}: متوقّع {expected} · مقيس {got}")
    print(f"\nالفحص الذاتي: {len(cases) - failures}/{len(cases)} حالة")
    return 0 if failures == 0 else 1


def main() -> int:
    ap = argparse.ArgumentParser(description="حراسة أصل الشجرة: لا مرجعًا متعقَّبًا ولا بصمة صنف")
    ap.add_argument("--root", default=_ROOT)
    ap.add_argument("--assert", dest="assert_", action="store_true")
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--list", action="store_true")
    args = ap.parse_args()

    if args.self_test:
        return self_test()

    if args.list:
        print(f"البصمات المحروسة: {len(FINGERPRINTS)}")
        for marker, reason in FINGERPRINTS.items():
            print(f"  · {marker} — {reason}")
        print(f"المجلّدات المرجعية الممنوعة: {len(REFERENCE_TREES)}")
        for tree in REFERENCE_TREES:
            print(f"  · {tree}")
        return 0

    files = tracked_files(args.root)
    if not files:
        print("⚠ لا قائمة ملفات متعقَّبة — الفحص غير مُتحقَّق (لا git أو شجرة فارغة)")
        return 0 if not args.assert_ else 1

    hits = scan(args.root, files)
    if not hits:
        print(
            f"✓ الأصل محروس: {len(files)} ملفًا متعقَّبًا · 0 مسار مرجعي · "
            f"0 بصمة من {len(FINGERPRINTS)} بصمة"
        )
        return 0

    print(f"✗ بصمات أصل: {len(hits)}")
    for hit in hits:
        print(hit.render())
    print("\nالحدّ: الفحص اسمي. من نقل بنية المرجع بأسماء مبدَّلة يمسكه `upstream_similarity.py`.")
    return 1 if args.assert_ else 0


if __name__ == "__main__":
    sys.exit(main())
