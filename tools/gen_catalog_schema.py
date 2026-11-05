#!/usr/bin/env python3
"""مخطط JSON لكتالوج التحكّم — العقد نفسه، مكتوبًا بلغة تقرؤها الأدوات.

**المصدر الوحيد:** `manager/app/src/main/assets/control_catalog.json`.
**المخرج:** `docs/control-catalog.schema.json` (JSON Schema draft 2020-12).

لماذا مولّد لا ملفّ يدوي: الكتالوج يُتحقَّق منه دلاليًّا في `gen_control_catalog.py`
(أسماء المفاتيح، البادئات، تفرّد المسارات…)، لكن **شكله** — أيّ حقل إلزاميّ، وأيّ مفردات
مغلقة — لم يكن مكتوبًا في مكان يستطيع محرّر أو أداة خارجية قراءته. فصار المخطط مرآةً
للمفردات نفسها المستوردة من المدقّق، لا نسخةً ثانية تتقادم بصمت.

**وحدّه المعلن:** المخطط يوصف الشكل لا الدلالة. مدخلٌ مكتمل الحقول بقيمة `danger` صحيحة
لكن بمسارات غير موجودة يمرّ من هنا ويرسب هناك — وهذا تقسيم مقصود، لا نقص.

الاستعمال:
    python3 tools/gen_catalog_schema.py --write     # يكتب docs/control-catalog.schema.json
    python3 tools/gen_catalog_schema.py --assert    # يفشل إن كان المخطط قديمًا أو الكتالوج لا يطابقه
    python3 tools/gen_catalog_schema.py --self-test # يحقن عطوبًا ويتأكّد أن المدقّق يرفضها
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from gen_control_catalog import (  # noqa: E402  (المسار يُضاف قبل الاستيراد)
    CATALOG,
    DANGERS,
    KINDS,
    ORIGINS,
    POLICIES,
    REASONS,
    ROOT,
    TRANSPORTS,
)

SCHEMA = ROOT / "docs/control-catalog.schema.json"

# مراتب الامتياز كما تعلنها الكتالوج نفسها. ليست في `gen_control_catalog.py` لأن المدقّق
# لا يحكم عليها (السلوك يُقاس في الـarbiter)، لكن المخطط يجب أن يمنع اختراع مرتبة جديدة.
PRIVILEGES = {"NONE", "MAGISK", "KSU", "APATCH", "SU_BARE", "DAEMON", "SHIZUKU"}

# أسباب بقاء المدخل قراءةً فقط — تُقرأ من المدقّق لأنها مفردات عقد لا حرّية كتابة.
READ_ONLY_REASONS = {
    "EXISTING_WRITER_NOT_ROUTED",
    "AWAITING_CANONICAL_KEY",
    "NO_WRITE_PATH",
    "PROTECTED_BY_DESIGN",
}


def _enum(values: set[str]) -> dict:
    """مفردة مغلقة مرتّبة: الترتيب ثابت فلا يتغيّر الملفّ بين تشغيلين."""
    return {"type": "string", "enum": sorted(values)}


def build() -> dict:
    return {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "urn:maxmanager:control-catalog:v1",
        "title": "MaxManager control catalog",
        "description": (
            "The hardware-control catalogue as the app ships it. Shape only: whether a path "
            "exists on a device is decided at runtime, not here."
        ),
        "type": "object",
        "required": ["version", "entries", "unmapped"],
        "additionalProperties": False,
        "properties": {
            "version": {"type": "integer", "minimum": 1},
            "entries": {
                "type": "array",
                "minItems": 1,
                "items": {"$ref": "#/$defs/entry"},
            },
            "unmapped": {"type": "array", "items": {"$ref": "#/$defs/unmapped"}},
        },
        "$defs": {
            "entry": {
                "type": "object",
                "required": [
                    "key",
                    "transport",
                    "kind",
                    "candidates",
                    "verify",
                    "privilege",
                    "danger",
                ],
                "additionalProperties": False,
                "properties": {
                    "key": {"type": "string", "minLength": 1},
                    "transport": _enum(TRANSPORTS),
                    "kind": _enum(KINDS),
                    "candidates": {
                        # قد تكون فارغة عن قصد: مداخل `AUDIO` تُحلّ مساراتها وقت التشغيل،
                        # فلا مسار sysfs ثابتًا يُعلَن. والمدقّق الدلالي هو من يحكم على ذلك.
                        "type": "array",
                        "items": {"$ref": "#/$defs/candidate"},
                    },
                    "verify": {"$ref": "#/$defs/verify"},
                    "privilege": {"$ref": "#/$defs/privilege"},
                    "danger": _enum(DANGERS),
                    # مدخل قابل للكتابة فيزيائيًّا لكنه مقفل بقرار — السبب مفردة لا نصّ حرّ.
                    "read_only_reason": _enum(READ_ONLY_REASONS),
                    "read_only_until_canonical_key": {"type": "boolean"},
                },
            },
            "candidate": {
                "type": "object",
                "required": ["path", "origin"],
                "additionalProperties": False,
                "properties": {
                    "path": {"type": "string", "minLength": 1},
                    "origin": _enum(ORIGINS),
                },
            },
            "verify": {
                "type": "object",
                "required": ["policy"],
                "additionalProperties": False,
                "properties": {
                    "policy": _enum(POLICIES),
                    "tolerance": {"type": "integer", "minimum": 0},
                },
            },
            "privilege": {
                "type": "object",
                "required": ["read", "write"],
                "additionalProperties": False,
                "properties": {
                    "read": {"type": "array", "items": _enum(PRIVILEGES)},
                    "write": {"type": "array", "items": _enum(PRIVILEGES)},
                },
            },
            "unmapped": {
                "type": "object",
                "required": ["path", "reason"],
                "additionalProperties": False,
                "properties": {
                    "path": {"type": "string", "minLength": 1},
                    "reason": _enum(REASONS),
                    "note": {"type": "string"},
                },
            },
        },
    }


def canonical(schema: dict) -> str:
    """الشكل القانوني الوحيد — `--assert` يقارن به لا بالتنسيق الذي كتبه محرّر."""
    return json.dumps(schema, ensure_ascii=False, indent=2) + "\n"


def _type_ok(value: object, expected: str) -> bool:
    """أنواع JSON السبعة التي نُصدرها فقط. `bool` ليس `integer` هنا (خلافًا لبايثون)."""
    if expected == "object":
        return isinstance(value, dict)
    if expected == "array":
        return isinstance(value, list)
    if expected == "string":
        return isinstance(value, str)
    if expected == "boolean":
        return isinstance(value, bool)
    if expected == "integer":
        return isinstance(value, int) and not isinstance(value, bool)
    if expected == "number":
        return isinstance(value, (int, float)) and not isinstance(value, bool)
    return False


def validate(instance: object, schema: dict, path: str = "$") -> list[str]:
    """مدقّق للمجموعة الجزئية التي نُصدرها — بلا حزمة `jsonschema` خارجية.

    يدعم: `$ref` إلى `#/$defs/…`، `type`، `required`، `properties`،
    `additionalProperties: false`، `items`، `enum`، `minimum`، `minLength`، `minItems`.
    وأيّ كلمة مفتاحية أخرى تُتجاهل عن قصد: المخطط هنا لا يستعملها.
    """
    out: list[str] = []

    ref = schema.get("$ref")
    if ref:
        name = ref.rsplit("/", 1)[-1]
        target = build()["$defs"].get(name)
        if target is None:
            return [f"{path}: $ref غير معروف: {ref}"]
        return validate(instance, target, path)

    expected = schema.get("type")
    if expected and not _type_ok(instance, expected):
        return [f"{path}: النوع {type(instance).__name__} لا يطابق {expected}"]

    if "enum" in schema and instance not in schema["enum"]:
        out.append(f"{path}: القيمة {instance!r} خارج المفردات {schema['enum']}")

    if isinstance(instance, str):
        if "minLength" in schema and len(instance) < schema["minLength"]:
            out.append(f"{path}: النصّ أقصر من {schema['minLength']}")

    if _type_ok(instance, "integer") and "minimum" in schema:
        if instance < schema["minimum"]:
            out.append(f"{path}: {instance} أصغر من {schema['minimum']}")

    if isinstance(instance, list):
        if "minItems" in schema and len(instance) < schema["minItems"]:
            out.append(f"{path}: العناصر {len(instance)} أقل من {schema['minItems']}")
        if isinstance(schema.get("items"), dict):
            for index, item in enumerate(instance):
                out.extend(validate(item, schema["items"], f"{path}[{index}]"))

    if isinstance(instance, dict):
        properties = schema.get("properties") or {}
        for name in schema.get("required") or []:
            if name not in instance:
                out.append(f"{path}: الحقل الإلزامي «{name}» غائب")
        for name, value in instance.items():
            if name in properties:
                out.extend(validate(value, properties[name], f"{path}.{name}"))
            elif schema.get("additionalProperties") is False:
                out.append(f"{path}: حقل غير معلن «{name}»")

    return out


def assert_current(schema: dict, data: dict) -> int:
    expected = canonical(schema)
    if not SCHEMA.exists():
        print(f"✗ {SCHEMA.relative_to(ROOT)} غير موجود — شغّل --write")
        return 1
    if SCHEMA.read_text(encoding="utf-8") != expected:
        print(f"✗ {SCHEMA.relative_to(ROOT)} قديم — أعد التوليد بـ--write")
        return 1
    problems = validate(data, schema)
    if problems:
        print(f"✗ الكتالوج لا يطابق مخططه: {len(problems)} مخالفة")
        for problem in problems[:10]:
            print(f"  {problem}")
        return 1
    print(
        "✓ مخطط الكتالوج مطابق: "
        f"{len(data.get('entries') or [])} مدخلًا · {len(data.get('unmapped') or [])} غير مُخطَّط"
    )
    return 0


def self_test() -> int:
    schema = build()
    good = {
        "version": 1,
        "entries": [
            {
                "key": "demo:{x}",
                "transport": "NODE",
                "kind": "INT",
                "candidates": [{"path": "/sys/demo", "origin": "MEASURED"}],
                "verify": {"policy": "READ_BACK_EQUAL", "tolerance": 0},
                "privilege": {"read": ["NONE"], "write": ["MAGISK"]},
                "danger": "LOW",
            }
        ],
        "unmapped": [{"path": "/proc/demo", "reason": "ROOT_ONLY"}],
    }
    broken = {
        "حقل ناقص": {**good, "entries": [{k: v for k, v in good["entries"][0].items() if k != "danger"}]},
        "مفردة مخترعة": {**good, "entries": [{**good["entries"][0], "danger": "SEVERE"}]},
        "نوع خاطئ": {**good, "version": "one"},
        "حقل إضافي": {**good, "entries": [{**good["entries"][0], "extra": 1}]},
        "مرشّح بلا مسار": {
            **good,
            "entries": [{**good["entries"][0], "candidates": [{"origin": "MEASURED"}]}],
        },
        "قائمة فارغة": {**good, "entries": []},
    }
    checks: dict[str, bool] = {
        "الكتالوج السليم يمرّ": not validate(good, schema),
    }
    for name, fixture in broken.items():
        checks[f"يرفض: {name}"] = bool(validate(fixture, schema))
    if "minItems" in json.dumps(schema):
        checks["minItems مطبَّق"] = bool(validate({**good, "entries": []}, schema))
    failed = [name for name, ok in checks.items() if not ok]
    if failed:
        print("✗ self-test: " + " · ".join(failed))
        return 1
    print(f"✓ self-test: {len(checks)} فحوص ناجحة")
    return 0


def main(argv: list[str]) -> int:
    schema = build()
    data = json.loads(CATALOG.read_text(encoding="utf-8"))
    if "--write" in argv:
        SCHEMA.write_text(canonical(schema), encoding="utf-8")
        print(f"✓ كُتب {SCHEMA.relative_to(ROOT)}: {len(schema['$defs'])} تعريفًا")
        return 0
    if "--assert" in argv:
        return assert_current(schema, data)
    if "--self-test" in argv:
        return self_test()
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
