#!/usr/bin/env python3
"""مستند القدرات المولّد من كتالوج التحكّم — نقل فكرة «المخطط يُولّد التوثيق» لا كودها.

**المصدر الوحيد:** `manager/app/src/main/assets/control_catalog.json`.
**المخرج:** `docs/CAPABILITIES.md` — مرآةٌ لما يعلنه الكتالوج، لا وصفًا يدويًّا يتقادم.

لماذا مولّد لا كاتب يدوي: الكتالوج هو العقد المقيس (٤١ مدخلًا · ٦٩ مرشّحًا · ١ غير مُخطَّط)،
وأي إضافة إلى `HardwareControlKey` أو مسار sysfs يجب أن تظهر في المستند تلقائيًّا. المستند
اليدوي يفقد الصفّ الجديد بصمت، ثم يصير «التوثيق» مصدرًا ثانيًا يكذب على البيانات.

الاستعمال:
    python3 tools/gen_capabilities_doc.py --write    # يكتب docs/CAPABILITIES.md
    python3 tools/gen_capabilities_doc.py --assert   # يفشل إن كان المستند قديمًا
    python3 tools/gen_capabilities_doc.py --self-test
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "manager/app/src/main/assets/control_catalog.json"
DOC = ROOT / "docs/CAPABILITIES.md"


def load() -> dict:
    return json.loads(CATALOG.read_text(encoding="utf-8"))


def _cell(value: object) -> str:
    """قيمة داخل جدول Markdown: لا أنابيب ولا أسطر جديدة تكسر الصفّ."""
    text = str(value).replace("|", "\\|").replace("\n", " ")
    return text if text else "—"


def _list(values: object) -> str:
    if not values:
        return "—"
    if isinstance(values, list):
        return "، ".join(str(v) for v in values)
    return str(values)


def render(data: dict) -> str:
    entries = data.get("entries") or []
    unmapped = data.get("unmapped") or []
    candidates = sum(len(e.get("candidates") or []) for e in entries)

    lines: list[str] = [
        "# فهرس القدرات — مولّد من الكتالوج",
        "",
        "> **لا تُعدّل هذا الملفّ يدويًّا.** المصدر `manager/app/src/main/assets/control_catalog.json`،",
        "> والتوليد `python3 tools/gen_capabilities_doc.py --write`، والتحقّق في البوابات",
        "> `python3 tools/gen_capabilities_doc.py --assert`.",
        "",
        "## الخلاصة",
        "",
        f"- إصدار الكتالوج: **{data.get('version', '—')}**",
        f"- المداخل: **{len(entries)}**",
        f"- المرشّحون: **{candidates}**",
        f"- غير المُخطَّط: **{len(unmapped)}**",
        "",
        "## المداخل",
        "",
        "| المفتاح | النوع | النقل | الكتابة | الخطر | المرشّح الأول |",
        "| --- | --- | --- | --- | --- | --- |",
    ]

    for entry in entries:
        key = entry.get("key", "—")
        first = (entry.get("candidates") or [{}])[0]
        write = (entry.get("privilege") or {}).get("write") or []
        lines.append(
            "| `{key}` | {kind} | {transport} | {write} | {danger} | `{path}` |".format(
                key=_cell(key),
                kind=_cell(entry.get("kind", "—")),
                transport=_cell(entry.get("transport", "—")),
                write=_cell(_list(write)),
                danger=_cell(entry.get("danger", "—")),
                path=_cell(first.get("path", "—")),
            )
        )

    lines += ["", "## تفصيل المرشّحين", ""]
    for entry in entries:
        lines.append(f"### `{entry.get('key', '—')}`")
        lines.append("")
        verify = entry.get("verify") or {}
        privilege = entry.get("privilege") or {}
        lines.append(f"- النوع: `{entry.get('kind', '—')}` · النقل: `{entry.get('transport', '—')}`")
        lines.append(f"- القراءة: {_list(privilege.get('read'))}")
        lines.append(f"- الكتابة: {_list(privilege.get('write'))}")
        lines.append(
            "- التحقّق: `{}`{}".format(
                verify.get("policy", "—"),
                f" · تفاوت {verify.get('tolerance')}" if verify.get("tolerance") is not None else "",
            )
        )
        lines.append(f"- الخطر: **{entry.get('danger', '—')}**")
        candidates_for_entry = entry.get("candidates") or []
        if candidates_for_entry:
            lines.append("- المرشّحون:")
            for candidate in candidates_for_entry:
                lines.append(
                    "  - `{}` — `{}`".format(
                        candidate.get("path", "—"), candidate.get("origin", "—")
                    )
                )
        else:
            lines.append("- المرشّحون: — (مفتاح غير عقديّ بعد)")
        lines.append("")

    lines += [
        "## غير المُخطَّط",
        "",
        "| المسار | السبب | ملاحظة |",
        "| --- | --- | --- |",
    ]
    if unmapped:
        for item in unmapped:
            lines.append(
                "| `{}` | {} | {} |".format(
                    _cell(item.get("path", "—")),
                    _cell(item.get("reason", "—")),
                    _cell(item.get("note", "—")),
                )
            )
    else:
        lines.append("| — | — | لا شيء |")

    lines.append("")
    return "\n".join(lines)


def assert_current(data: dict) -> int:
    expected = render(data)
    if not DOC.exists():
        print(f"✗ {DOC.relative_to(ROOT)} غير موجود — شغّل --write")
        return 1
    actual = DOC.read_text(encoding="utf-8")
    if actual != expected:
        print(f"✗ {DOC.relative_to(ROOT)} قديم — أعد التوليد بـ--write")
        return 1
    print(f"✓ مستند القدرات مطابق: {len(data.get('entries') or [])} مدخلًا")
    return 0


def self_test() -> int:
    fixture = {
        "version": 1,
        "entries": [
            {
                "key": "demo:{x}",
                "transport": "NODE",
                "kind": "INT",
                "candidates": [{"path": "/sys/demo", "origin": "MEASURED"}],
                "verify": {"policy": "READ_BACK_EQUAL", "tolerance": 0},
                "privilege": {"read": ["NONE"], "write": ["ROOT"]},
                "danger": "LOW",
            }
        ],
        "unmapped": [{"path": "/proc/demo", "reason": "ROOT_ONLY", "note": "مثال"}],
    }
    text = render(fixture)
    checks = {
        "المدخل": "`demo:{x}`" in text,
        "المرشّح": "/sys/demo" in text,
        "غير المخطّط": "/proc/demo" in text,
        "العدّ": "**1**" in text,
        "التحقّق": "READ_BACK_EQUAL" in text,
    }
    failed = [name for name, ok in checks.items() if not ok]
    if failed:
        print("✗ self-test: " + " · ".join(failed))
        return 1
    print(f"✓ self-test: {len(checks)} فحوص ناجحة")
    return 0


def main(argv: list[str]) -> int:
    data = load()
    if "--write" in argv:
        DOC.write_text(render(data), encoding="utf-8")
        print(f"✓ كُتب {DOC.relative_to(ROOT)}: {len(data.get('entries') or [])} مدخلًا")
        return 0
    if "--assert" in argv:
        return assert_current(data)
    if "--self-test" in argv:
        return self_test()
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
