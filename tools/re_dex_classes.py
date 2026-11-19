#!/usr/bin/env python3
"""Report whether an APK's dex files actually *define* the classes you name.

Presence of a descriptor string in a dex proves only that something refers to
the type — which is exactly the state that made cn.nubia.gameassist crash:

    NoClassDefFoundError: Failed resolution of:
        Lcom/zte/gameassist/GameAssistWrapper$Callback;

The string was there, the class was not. So a stub is only verified once
`class_defs` contains it, and that is what this reads.

    python3 tools/re_dex_classes.py app.apk --require Lcom/zte/gameassist/GameAssistWrapper;
    python3 tools/re_dex_classes.py app.apk --require X --list | head
"""

from __future__ import annotations

import argparse
import struct
import sys
import zipfile
from pathlib import Path


def uleb128(data: bytes, offset: int) -> tuple[int, int]:
    result = shift = 0
    while True:
        byte = data[offset]
        offset += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, offset
        shift += 7


def defined_classes(dex: bytes) -> list[str]:
    if dex[:4] != b"dex\n":
        raise SystemExit("not a dex file")
    string_ids_size, string_ids_off = struct.unpack_from("<II", dex, 0x38)
    type_ids_size, type_ids_off = struct.unpack_from("<II", dex, 0x40)
    class_defs_size, class_defs_off = struct.unpack_from("<II", dex, 0x60)

    def string(index: int) -> str:
        offset = struct.unpack_from("<I", dex, string_ids_off + 4 * index)[0]
        size, start = uleb128(dex, offset)
        return dex[start : start + size].decode("utf-8", "replace")

    def type_name(index: int) -> str:
        return string(struct.unpack_from("<I", dex, type_ids_off + 4 * index)[0])

    return [
        type_name(struct.unpack_from("<I", dex, class_defs_off + 32 * k)[0])
        for k in range(class_defs_size)
    ]


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--require", action="append", default=[], metavar="LDESC;")
    parser.add_argument("--list", action="store_true", help="print every defined class")
    args = parser.parse_args(argv)

    names: set[str] = set()
    with zipfile.ZipFile(args.apk) as archive:
        for entry in sorted(archive.namelist()):
            if entry.startswith("classes") and entry.endswith(".dex"):
                found = defined_classes(archive.read(entry))
                print(f"{entry}: {len(found)} classes")
                names.update(found)

    if args.list:
        for name in sorted(names):
            print(name)

    missing = [wanted for wanted in args.require if wanted not in names]
    for wanted in args.require:
        print(f"{'DEFINED' if wanted in names else 'MISSING':8} {wanted}")

    if missing:
        print(f"missing classes: {missing}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
