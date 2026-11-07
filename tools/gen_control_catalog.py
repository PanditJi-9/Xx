#!/usr/bin/env python3
"""كاتالوج مفاتيح التحكّم (M1) — مدقّق البيانات المعلنة، لا مولّد كود بلا قارئ.

**المصدر الملتزم:** `manager/app/src/main/assets/control_catalog.json`.
**الحكم:** كل مفتاح يجب أن يبدأ ببادئة من `HardwareControlKey` (١٥ `const val`)، وكل مدخل
`PROTECTED` لا يقبل الكتابة، وكل قبضة `NODE` لها مرشّح واحد على الأقل.

ولماذا مدقّق لا مولّد: الكتالوج يُقرأ في الاختبار النقيّ من JSON مباشرةً، وتوليد Kotlin لا يضيف
قارئًا. وما يُقاس هنا هو **صدق البيانات** — وهذه هي البوّابة التي تُسقط البناء عند أوّل مفتاح مخترع.

الاستعمال:
    python3 tools/gen_control_catalog.py --assert     # الحكم: exit 0 أو تفصيل المخالفات
    python3 tools/gen_control_catalog.py --check      # الصيغة القانونية (بايت-ببايت)
    python3 tools/gen_control_catalog.py --self-test  # يقيس المدقّق نفسه بحقن خمسة عطوب
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "manager/app/src/main/assets/control_catalog.json"
CONTROL_KEY = ROOT / "manager/app/src/main/java/nd/max/core/hardware/HardwareControlKey.kt"

TRANSPORTS = {"NODE", "PROC", "PROP", "AUDIO", "SETTINGS"}
KINDS = {"FREQUENCY_RANGE", "ENUM", "BOOL", "INT", "STRING", "PERCENT"}
POLICIES = {"READ_BACK_EQUAL", "READ_BACK_TOLERANCE", "NONE"}
DANGERS = {"LOW", "MEDIUM", "HIGH", "PROTECTED"}
ORIGINS = {"MEASURED", "DERIVED"}
REASONS = {
    "ROOT_ONLY", "NO_NODE", "NO_PERMISSION", "DENIED_BY_POLICY", "UNKNOWN",
    # مسار يُقرأ لكتالوج العُقد ولا يُقصَد به مقبض تحكّم: الوجود يُقاس، والكتابة لا تُدّعى.
    "READ_ONLY_PROBE",
}

# سبب بقاء مدخلٍ قابلٍ للكتابة فيزيائيًّا **قراءةً فقط** — مفردات مغلقة، فلا يُترك السبب نصًّا حرًّا
# لا يُقاس. (ADR-89: المفتاح لا يُعلَن ما دام كاتبه القائم خارج الكتالوج.)
READ_ONLY_REASONS = {
    "EXISTING_WRITER_NOT_ROUTED",  # كاتب قائم في `ui/` أو backend لا يمرّ بالـarbiter بعد
    "AWAITING_CANONICAL_KEY",      # لا بادئة في HardwareControlKey بعد
    "NO_WRITE_PATH",               # العقدة تُقرأ فقط بحكم النواة
    "PROTECTED_BY_DESIGN",         # محميّة بقرار (حرارة/watchdog)
}


def canonical(data: dict) -> str:
    """الشكل القانوني الوحيد — `--check` يقارن بهذا لا بالتنسيق الذي كتبه محرّر."""
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def prefixes() -> list[str]:
    text = CONTROL_KEY.read_text(encoding="utf-8")
    return re.findall(r'const val [A-Z0-9_]+ = "([^"]*)"', text)


def load(path: Path = CATALOG) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def violations(data: dict, known_prefixes: list[str]) -> list[str]:
    """كل مخالفة بسطرها — لا أوّل عطب فقط، فلا تُصلح واحدة وتُعاد الجولة."""
    out: list[str] = []
    if data.get("version") != 1:
        out.append("version: يجب أن يكون 1")
    entries = data.get("entries") or []
    unmapped = data.get("unmapped") or []
    if not entries:
        out.append("entries: فارغة — كتالوج لا يقيس")
    if not known_prefixes:
        out.append("HardwareControlKey: لم تُقرأ أي بادئة")

    seen: dict[str, int] = {}
    for index, entry in enumerate(entries):
        key = entry.get("key", "")
        where = f"entries[{index}] ({key or 'بلا مفتاح'})"
        if not key:
            out.append(f"{where}: key فارغ")
        elif key in seen:
            out.append(f"{where}: مفتاح مكرّر مع entries[{seen[key]}]")
        else:
            seen[key] = index
        # **القاعدة المحركة:** مفتاح **قابل للكتابة** يجب أن يشتقّ من `HardwareControlKey`،
        # فأي كتابة بمفتاح مخترع تعني مقبضًا له كتابتان. وأما **القراءة** فلها مسارات
        # مراقبة لا مقبض تحكّم لها (`/proc/loadavg` مثلًا)، وتظلّ مُعلنة لأن المسار يُقاس.
        write = (entry.get("privilege") or {}).get("write") or []
        if write and known_prefixes and not any(key.startswith(p) for p in known_prefixes if p):
            out.append(f"{where}: قابل للكتابة خارج HardwareControlKey — لا مقبض ثانٍ بنفس الاسم")
        transport = entry.get("transport")
        if transport not in TRANSPORTS:
            out.append(f"{where}: transport غير معلن ({transport})")
        if entry.get("kind") not in KINDS:
            out.append(f"{where}: kind غير معلن ({entry.get('kind')})")
        policy = (entry.get("verify") or {}).get("policy")
        if policy not in POLICIES:
            out.append(f"{where}: verify.policy غير معلن ({policy})")
        danger = entry.get("danger")
        if danger not in DANGERS:
            out.append(f"{where}: danger غير معلن ({danger})")
        candidates = entry.get("candidates") or []
        if transport == "NODE" and not candidates:
            out.append(f"{where}: عقدة بلا مرشّح واحد (candidates فارغة)")
        for candidate in candidates:
            if candidate.get("origin") not in ORIGINS:
                out.append(f"{where}: candidate origin غير معلن ({candidate.get('origin')})")
            if not str(candidate.get("path", "")).startswith("/"):
                out.append(f"{where}: candidate path ليس مسارًا مطلقًا ({candidate.get('path')})")
        if danger == "PROTECTED" and write:
            out.append(f"{where}: PROTECTED بقائمة كتابة {write} — محظور")
        # قراءة-فقط بلا سبب = قرار مخفيّ. والقاعدة تُقفل الاتجاهين: لا عَلَم بلا سبب، ولا سبب بلا عَلَم.
        read_only = entry.get("read_only_until_canonical_key") is True
        reason = entry.get("read_only_reason")
        if read_only and reason not in READ_ONLY_REASONS:
            out.append(f"{where}: قراءة-فقط بلا سبب معلن ({reason}) — القرار يُكتب لا يُفهم")
        if not read_only and reason is not None:
            out.append(f"{where}: سبب قراءة-فقط بلا عَلَم ({reason}) — سببٌ يتيم")
    # وكل بادئة معلنة يجب أن تظهر — وإلّا فُقد مقبض كامل بلا سطر.
    for prefix in known_prefixes:
        if prefix and not any(str(e.get("key", "")).startswith(prefix) for e in entries):
            out.append(f"{prefix}: بلا مدخل واحد في الكتالوج — مقبض كامل غير معلن")
    for index, row in enumerate(unmapped):
        where = f"unmapped[{index}] ({row.get('path')})"
        if row.get("reason") not in REASONS:
            out.append(f"{where}: reason غير معلن ({row.get('reason')})")
    return out


def self_test() -> int:
    """يقيس المدقّق نفسه: كل عطب محقون يجب أن يُمسك بالاسم."""
    base = load()
    known = prefixes()
    # حرس القياس: الرقم هو عدد ثوابت `HardwareControlKey` المقروءة، لا عددًا سحريًّا.
    # كان ١٥ ثم صار ١٩ بعد إضافة مفاتيح VM/audio/spoof؛ ويجب أن يطابق
    # `ControlCatalogParityTest` حرفيًّا، وإلّا توقّف الحقن قبل أن يقيس شيئًا.
    expected_prefixes = 23
    if len(known) != expected_prefixes:
        print(f"✗ البوادئ المقيسة {len(known)} لا {expected_prefixes} — توقّف القياس قبل الحقن")
        return 1
    cases: list[tuple[str, dict]] = []

    missing = json.loads(json.dumps(base))
    missing["entries"] = [e for e in missing["entries"] if not str(e["key"]).startswith("gpu_frequency:")]
    cases.append(("حذف كل مداخل gpu_frequency", missing))

    invented = json.loads(json.dumps(base))
    invented["entries"].append({"key": "my.custom.knob", "transport": "PROP", "kind": "BOOL",
                                "candidates": [], "verify": {"policy": "NONE"},
                                "privilege": {"read": ["NONE"], "write": ["MAGISK"]}, "danger": "LOW"})
    cases.append(("مفتاح خارج HardwareControlKey", invented))

    protected = json.loads(json.dumps(base))
    for entry in protected["entries"]:
        if entry["danger"] == "PROTECTED":
            entry["privilege"]["write"] = ["MAGISK"]
            break
    cases.append(("PROTECTED بقائمة كتابة", protected))

    empty = json.loads(json.dumps(base))
    empty["entries"][0]["candidates"] = []
    empty["entries"][0]["transport"] = "NODE"
    cases.append(("عقدة بلا مرشّح", empty))

    silent = json.loads(json.dumps(base))
    for entry in silent["entries"]:
        if entry.get("read_only_until_canonical_key") is True:
            entry.pop("read_only_reason", None)
            break
    cases.append(("قراءة-فقط بلا سبب معلن", silent))

    failed = 0
    for name, data in cases:
        found = violations(data, known)
        if not found:
            print(f"✗ لم يُمسك العطب المحقون: {name}")
            failed += 1
        else:
            print(f"✓ أُمسك: {name} — {found[0]}")
    # والحالة السليمة يجب ألا تُمسك.
    if violations(base, known):
        print("✗ الأصل السليم يُبلَّغ عنه — المدقّق لا يميّز")
        failed += 1
    else:
        print("✓ الأصل السليم يمرّ (لا إيجابية كاذبة)")
    return 1 if failed else 0


def main(argv: list[str]) -> int:
    if "--self-test" in argv:
        return self_test()
    known = prefixes()
    data = load()
    found = violations(data, known)
    if "--check" in argv:
        expected = canonical(data)
        actual = CATALOG.read_text(encoding="utf-8")
        if actual != expected:
            print("✗ الصيغة القانونية مختلفة — أعد كتابة الملفّ بـjson.dumps(indent=2)")
            return 1
        print("✓ الصيغة القانونية مطابقة")
    if found:
        print(f"✗ مخالفات: {len(found)}")
        for line in found:
            print("   " + line)
        return 1
    covered = sum(len(e.get("candidates") or []) for e in data["entries"])
    read_only = [e["key"] for e in data["entries"] if e.get("read_only_until_canonical_key") is True]
    print(f"✓ الكتالوج: {len(data['entries'])} مدخلًا · {covered} مرشّحًا · "
          f"{len(data.get('unmapped') or [])} غير مُخطَّط · {len(known)} بادئة · "
          f"{len(read_only)} قراءة-فقط بسبب معلن")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
