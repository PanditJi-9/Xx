#!/usr/bin/env python3
"""Read an APK's binary AndroidManifest.xml and report its real integration surface.

Why this exists: the ecosystem plan needs an inventory of 23 apps, and decoding 23
APKs to smali just to read a package name is hours of work that measures nothing
extra. The manifest and the zip directory already hold the facts that decide
integration: who is exported, what actions they answer, what permissions they
demand, which SDK they target, which ABIs they ship.

So this parser reads the binary XML (AXML) directly: string pool, resource map,
start/end element chunks, typed attribute values. No Java, no apktool, no device.

What it does NOT claim: a label that is a resource reference is reported as a
reference (`@0x7f...`), not resolved — resolving it needs the resource table.
An exported component is not a working component; that needs a device.

Usage:
    python3 tools/apk_inventory.py apps/ --json out.json        # a directory of APKs
    python3 tools/apk_inventory.py --apk a.apk --apk b.apk
    python3 tools/apk_inventory.py --markdown docs/audit/APK_INVENTORY.md apps/
    python3 tools/apk_inventory.py --self-test
"""

from __future__ import annotations

import argparse
import json
import os
import struct
import sys
import zipfile

# ---- chunk types -----------------------------------------------------------
RES_STRING_POOL = 0x0001
RES_XML = 0x0003
RES_XML_START_ELEMENT = 0x0102
RES_XML_END_ELEMENT = 0x0103
RES_XML_RESOURCE_MAP = 0x0180

TYPE_REFERENCE = 0x01
TYPE_STRING = 0x03
TYPE_INT_DEC = 0x10
TYPE_INT_HEX = 0x11
TYPE_INT_BOOLEAN = 0x12

UTF8_FLAG = 1 << 8

# android:* attribute ids that matter for integration, when the name string is empty.
ATTR_IDS = {
    0x01010001: "label",
    0x01010003: "name",
    0x01010006: "permission",
    0x0101000F: "debuggable",
    0x01010010: "exported",
    0x01010011: "process",
    0x01010018: "authorities",
    0x0101001D: "launchMode",
    0x0101001E: "screenOrientation",
    0x0101020C: "minSdkVersion",
    0x0101021B: "versionCode",
    0x0101021C: "versionName",
    0x01010270: "targetSdkVersion",
    0x0101028E: "required",
    0x01010009: "protectionLevel",
}

COMPONENT_TAGS = {
    "activity": "activities",
    "activity-alias": "activities",
    "service": "services",
    "receiver": "receivers",
    "provider": "providers",
}


class ManifestError(RuntimeError):
    pass


class StringPool:
    @classmethod
    def empty(cls) -> "StringPool":
        pool = cls.__new__(cls)
        pool.strings = []
        return pool

    def __init__(self, data: bytes, offset: int):
        (_t, header_size, _size, count, _styles, flags, strings_start, _ss) = struct.unpack_from(
            "<HHIIIIII", data, offset
        )
        if count == 0:
            self.strings: list[str] = []
            return
        offsets = [struct.unpack_from("<I", data, offset + header_size + 4 * i)[0] for i in range(count)]
        base = offset + strings_start
        strings = []
        for rel in offsets:
            pos = base + rel
            try:
                strings.append(self._read(data, pos, flags))
            except (IndexError, struct.error):
                strings.append("")
        self.strings = strings

    @staticmethod
    def _read(data: bytes, pos: int, flags: int) -> str:
        if flags & UTF8_FLAG:
            # utf16 length (1-2 bytes) then utf8 byte length
            n = data[pos]
            if n & 0x80:
                pos += 2
            else:
                pos += 1
            n = data[pos]
            if n & 0x80:
                n = ((n & 0x7F) << 8) | data[pos + 1]
                pos += 2
            else:
                pos += 1
            return data[pos : pos + n].decode("utf-8", "replace")
        n = struct.unpack_from("<H", data, pos)[0]
        pos += 2
        if n & 0x8000:
            n = ((n & 0x7FFF) << 16) | struct.unpack_from("<H", data, pos)[0]
            pos += 2
        return data[pos : pos + 2 * n].decode("utf-16-le", "replace")

    def get(self, index: int) -> str:
        if 0 <= index < len(self.strings):
            return self.strings[index]
        return ""


def _typed(pool: StringPool, data_type: int, value: int) -> str:
    if data_type == TYPE_STRING:
        return pool.get(value)
    if data_type == TYPE_REFERENCE:
        return f"@0x{value:08x}"
    if data_type == TYPE_INT_BOOLEAN:
        return "true" if value else "false"
    if data_type == TYPE_INT_HEX:
        return f"0x{value:x}"
    return str(value)


def parse_manifest(raw: bytes) -> dict:
    """Parse binary AndroidManifest.xml into a plain dict."""
    t, header_size, total = struct.unpack_from("<HHI", raw, 0)
    if t != RES_XML or total > len(raw):
        raise ManifestError("not an AXML document")
    pos = header_size
    pool = StringPool.empty()
    res_map: list[int] = []
    stack: list[str] = []
    out: dict = {
        "package": "",
        "versionName": "",
        "versionCode": "",
        "minSdk": "",
        "targetSdk": "",
        "label": "",
        "permissions": [],
        "features": [],
        "components": {v: [] for v in COMPONENT_TAGS.values()},
        "declaredPermissions": [],
        "actions": {},
    }
    current_comp: dict | None = None
    current_actions: list[str] = []

    while pos + 8 <= len(raw):
        ctype, chead, csize = struct.unpack_from("<HHI", raw, pos)
        if csize == 0 or pos + csize > len(raw):
            break
        if ctype == RES_STRING_POOL:
            pool = StringPool(raw, pos)
        elif ctype == RES_XML_RESOURCE_MAP:
            count = (chead - 8) // 4 or (csize - chead) // 4
            res_map = [
                struct.unpack_from("<I", raw, pos + chead + 4 * i)[0] for i in range(count)
            ]
        elif ctype == RES_XML_START_ELEMENT:
            # headerSize covers lineNumber/comment, so the extension starts with ns/name.
            _ns, name_idx = struct.unpack_from("<II", raw, pos + chead)
            tag = pool.get(name_idx)
            # ResXMLTree_attrExt starts right after ns/name, i.e. at pos + chead + 8.
            attr_start = struct.unpack_from("<H", raw, pos + chead + 8)[0]
            attr_size, attr_count = struct.unpack_from("<HH", raw, pos + chead + 10)
            attrs: dict[str, str] = {}
            base = pos + chead + attr_start
            for i in range(attr_count):
                a = base + 20 * i
                ns_idx, name_i, raw_val, vsize, _res0, dtype, dval = struct.unpack_from(
                    "<IIIHBBI", raw, a
                )
                key = pool.get(name_i)
                if key.startswith("android:"):
                    key = key[len("android:"):]
                if not key and 0 <= name_i - 0 < len(res_map):
                    key = ATTR_IDS.get(res_map[name_i], "")
                if not key:
                    continue
                if dtype == TYPE_STRING:
                    attrs[key] = pool.get(dval) or pool.get(raw_val)
                else:
                    attrs[key] = _typed(pool, dtype, dval)

            if tag == "manifest":
                out["package"] = attrs.get("package", "")
                out["versionName"] = attrs.get("versionName", "")
                out["versionCode"] = attrs.get("versionCode", "")
                out["label"] = attrs.get("label", "")
            elif tag in ("uses-sdk",):
                out["minSdk"] = attrs.get("minSdkVersion", "")
                out["targetSdk"] = attrs.get("targetSdkVersion", "")
            elif tag == "uses-permission" or tag == "uses-permission-sdk-23":
                name = attrs.get("name", "")
                if name:
                    out["permissions"].append(name)
            elif tag == "permission":
                name = attrs.get("name", "")
                if name:
                    out["declaredPermissions"].append(
                        {"name": name, "protectionLevel": attrs.get("protectionLevel", "")}
                    )
            elif tag == "uses-feature":
                name = attrs.get("name", "")
                if name:
                    out["features"].append({"name": name, "required": attrs.get("required", "true")})
            elif tag in COMPONENT_TAGS:
                comp = {
                    "name": attrs.get("name", ""),
                    "exported": attrs.get("exported", ""),
                    "permission": attrs.get("permission", ""),
                    "process": attrs.get("process", ""),
                    "actions": [],
                }
                out["components"][COMPONENT_TAGS[tag]].append(comp)
                current_comp = comp
                current_actions = []
            elif tag == "action" and current_comp is not None:
                act = attrs.get("name", "")
                if act:
                    current_actions.append(act)
                    current_comp["actions"].append(act)
                    out["actions"].setdefault(act, []).append(current_comp["name"])
            stack.append(tag)
        elif ctype == RES_XML_END_ELEMENT:
            if stack:
                tag = stack.pop()
                if tag in COMPONENT_TAGS:
                    current_comp = None
        pos += csize
    return out


def apk_facts(path: str) -> dict:
    facts: dict = {"file": os.path.basename(path), "bytes": os.path.getsize(path)}
    with zipfile.ZipFile(path) as zf:
        names = zf.namelist()
        facts["dex"] = len([n for n in names if n.startswith("classes") and n.endswith(".dex")])
        abis = sorted({n.split("/")[1] for n in names if n.startswith("lib/") and n.count("/") > 1})
        facts["abis"] = abis
        facts["resDirs"] = len({n.split("/")[1] for n in names if n.startswith("res/") and n.count("/") > 1})
        facts["assets"] = len([n for n in names if n.startswith("assets/") and not n.endswith("/")])
        facts["hasSplitConfig"] = any(n == "META-INF/com.android.tools.build.libraries/dependencies.pb" for n in names)
        try:
            facts.update(parse_manifest(zf.read("AndroidManifest.xml")))
        except (KeyError, ManifestError) as exc:
            facts["error"] = f"{type(exc).__name__}: {exc}"
    return facts


def collect(paths: list[str]) -> list[dict]:
    out = []
    for p in paths:
        try:
            out.append(apk_facts(p))
        except (zipfile.BadZipFile, OSError) as exc:
            out.append({"file": os.path.basename(p), "error": f"{type(exc).__name__}: {exc}"})
    return out


def walk_apks(root: str) -> list[str]:
    found = []
    for dirpath, _dirs, files in os.walk(root):
        for name in sorted(files):
            if name.endswith(".apk"):
                found.append(os.path.join(dirpath, name))
    return sorted(found)


def markdown(rows: list[dict], excluded: str = "") -> str:
    lines = [
        "# APK Inventory — measured",
        "",
        "Generated by `tools/apk_inventory.py` from the APKs themselves (binary manifest read",
        "directly; no apktool, no device). Regenerate, do not hand-edit.",
        "",
        "| APK | package | version | min/target | MB | dex | ABIs | res | perm | act/svc/rcv/prv | exported |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for r in rows:
        if "error" in r:
            lines.append(f"| {r['file']} | ERROR | — | — | — | — | — | — | — | — | — |")
            continue
        c = r["components"]
        counts = "/".join(
            str(len(c[k])) for k in ("activities", "services", "receivers", "providers")
        )
        exported = sum(1 for k in c for comp in c[k] if str(comp["exported"]).lower() == "true")
        lines.append(
            "| {file} | `{pkg}` | {ver} ({code}) | {mn}/{tg} | {mb:.1f} | {dex} | {abis} | {res} | {perm} | {counts} | {exp} |".format(
                file=r["file"],
                pkg=r["package"] or "?",
                ver=r["versionName"] or "?",
                code=r["versionCode"] or "?",
                mn=r["minSdk"] or "?",
                tg=r["targetSdk"] or "?",
                mb=r["bytes"] / 1e6,
                dex=r["dex"],
                abis=",".join(r["abis"]) or "—",
                res=r["resDirs"],
                perm=len(r["permissions"]),
                counts=counts,
                exp=exported,
            )
        )
    if excluded:
        lines += ["", f"> `{excluded}` مستثنى بأمر المالك: لم يُفكّ ولم يُحلَّل، ويُحفظ كما هو.", ""]
    return "\n".join(lines) + "\n"


# ---- self-test ------------------------------------------------------------
def _build_pool(strings: list[str]) -> bytes:
    """Minimal UTF-8 string pool, enough to prove the reader reads what a writer wrote."""
    data = bytearray()
    offsets = []
    for s in strings:
        offsets.append(len(data))
        b = s.encode("utf-8")
        data += bytes([len(s), len(b)]) + b + b"\x00"
    while len(data) % 4:
        data += b"\x00"
    header = 28
    strings_start = header + 4 * len(strings)
    size = strings_start + len(data)
    return (
        struct.pack("<HHI", RES_STRING_POOL, header, size)
        + struct.pack("<IIIII", len(strings), 0, UTF8_FLAG, strings_start, 0)
        + b"".join(struct.pack("<I", o) for o in offsets)
        + bytes(data)
    )


def _start_element(name_idx: int, attrs: list[tuple[int, int, int]]) -> bytes:
    """attrs: (name_idx, data_type, data). name_idx doubles as the resource id slot."""
    body = struct.pack("<IIII", 1, 0xFFFFFFFF, 0xFFFFFFFF, name_idx)
    body += struct.pack("<HHHHHH", 20, 20, len(attrs), 0, 0, 0)
    for a_name, dtype, dval in attrs:
        body += struct.pack("<III", 0xFFFFFFFF, a_name, 0xFFFFFFFF)
        body += struct.pack("<HBBI", 8, 0, dtype, dval)
    size = 8 + len(body)
    return struct.pack("<HHI", RES_XML_START_ELEMENT, 16, size) + body


def _end_element(name_idx: int) -> bytes:
    body = struct.pack("<IIII", 1, 0xFFFFFFFF, 0xFFFFFFFF, name_idx)
    return struct.pack("<HHI", RES_XML_END_ELEMENT, 16, 8 + len(body)) + body


def _synthetic_axml() -> tuple[bytes, dict]:
    strings = [
        "manifest", "package", "versionName", "versionCode", "uses-sdk",
        "minSdkVersion", "targetSdkVersion", "application", "activity",
        "name", "exported", "action", "com.example.probe", "9.9.9",
        "26", "36", "com.example.probe.Main", "android.intent.action.VIEW",
    ]
    idx = {s: i for i, s in enumerate(strings)}
    parts = []
    parts.append(_start_element(idx["manifest"], [
        (idx["package"], TYPE_STRING, idx["com.example.probe"]),
        (idx["versionName"], TYPE_STRING, idx["9.9.9"]),
        (idx["versionCode"], TYPE_INT_DEC, 4242),
    ]))
    parts.append(_start_element(idx["uses-sdk"], [
        (idx["minSdkVersion"], TYPE_INT_DEC, 26),
        (idx["targetSdkVersion"], TYPE_INT_DEC, 36),
    ]))
    parts.append(_end_element(idx["uses-sdk"]))
    parts.append(_start_element(idx["application"], []))
    parts.append(_start_element(idx["activity"], [
        (idx["name"], TYPE_STRING, idx["com.example.probe.Main"]),
        (idx["exported"], TYPE_INT_BOOLEAN, 1),
    ]))
    parts.append(_start_element(idx["action"], [
        (idx["name"], TYPE_STRING, idx["android.intent.action.VIEW"]),
    ]))
    parts.append(_end_element(idx["action"]))
    parts.append(_end_element(idx["activity"]))
    parts.append(_end_element(idx["application"]))
    parts.append(_end_element(idx["manifest"]))
    body = _build_pool(strings) + b"".join(parts)
    header = struct.pack("<HHI", RES_XML, 8, 8 + len(body))
    raw = header + body
    expected = {
        "package": "com.example.probe",
        "versionName": "9.9.9",
        "versionCode": "4242",
        "minSdk": "26",
        "targetSdk": "36",
        "activity": "com.example.probe.Main",
        "action": "android.intent.action.VIEW",
        "exported": "true",
    }
    return raw, expected


def self_test() -> int:
    raw, exp = _synthetic_axml()
    got = parse_manifest(raw)
    checks = [
        ("package", got["package"], exp["package"]),
        ("versionName", got["versionName"], exp["versionName"]),
        ("versionCode", got["versionCode"], exp["versionCode"]),
        ("minSdk", got["minSdk"], exp["minSdk"]),
        ("targetSdk", got["targetSdk"], exp["targetSdk"]),
        ("activity name", got["components"]["activities"][0]["name"], exp["activity"]),
        ("exported", got["components"]["activities"][0]["exported"], exp["exported"]),
        ("action", got["components"]["activities"][0]["actions"][0], exp["action"]),
    ]
    bad = [(n, g, e) for n, g, e in checks if g != e]
    # A parser that "passes" on an empty document proves nothing: assert it is not empty.
    empty, _ = _synthetic_axml()
    if not parse_manifest(empty)["components"]["activities"]:
        bad.append(("non-empty", "no activity parsed", "one activity"))
    for name, got_v, exp_v in bad:
        print(f"self-test FAIL {name}: got {got_v!r} want {exp_v!r}")
    if not bad:
        print(f"self-test OK: {len(checks)} manifest fields round-trip from a synthetic AXML")
    return 1 if bad else 0


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("root", nargs="?")
    ap.add_argument("--apk", action="append", default=[])
    ap.add_argument("--json")
    ap.add_argument("--markdown")
    ap.add_argument("--exclude", default="NubiaRedmagickyi_RedMagic")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args(argv)

    if args.self_test:
        return self_test()

    paths = list(args.apk)
    if args.root:
        paths += walk_apks(args.root)
    paths = [p for p in sorted(set(paths)) if args.exclude not in p]
    if not paths:
        print("no APKs found", file=sys.stderr)
        return 2

    rows = collect(paths)
    errors = [r for r in rows if "error" in r]
    for r in rows:
        if "error" in r:
            print(f"WARN {r['file']}: {r['error']}", file=sys.stderr)
    if args.json:
        os.makedirs(os.path.dirname(os.path.abspath(args.json)), exist_ok=True)
        tmp = args.json + ".tmp"
        with open(tmp, "w", encoding="utf-8") as fh:
            json.dump({"apks": rows, "excluded": args.exclude}, fh, ensure_ascii=False, indent=1)
        os.replace(tmp, args.json)
        print(f"json: {args.json} ({len(rows)} APKs, {len(errors)} errors)")
    if args.markdown:
        os.makedirs(os.path.dirname(os.path.abspath(args.markdown)), exist_ok=True)
        tmp = args.markdown + ".tmp"
        with open(tmp, "w", encoding="utf-8") as fh:
            fh.write(markdown(rows, excluded=f"{args.exclude} (المستثنى)"))
        os.replace(tmp, args.markdown)
        print(f"markdown: {args.markdown}")
    if not args.json and not args.markdown:
        print(json.dumps(rows, ensure_ascii=False, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
