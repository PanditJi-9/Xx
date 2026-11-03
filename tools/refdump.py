#!/usr/bin/env python3
"""refdump — read-only static probe for Android APK (DEX + ELF), stdlib only.

Why this exists: this repo's reverse-engineering host is aarch64 PRoot with no
androguard/lief/capstone and no pip guarantee. DEX is parsed directly from the
APK; nothing is extracted or written.

Usage:
  refdump.py <apk> summary [--top N]
  refdump.py <apk> classes [--filter RE]
  refdump.py <apk> api [--top N] [--filter RE]
  refdump.py <apk> search <RE> [--max N]
  refdump.py <apk> strings [--filter RE] [--max N]
  refdump.py <apk> native [--filter RE] [--max N]
  refdump.py <apk> elf [--filter RE] [--max N]     # ELF64 headers + exported functions

It answers "which tables/strings/APIs exist", not "what do the method bodies do":
for bodies use jadx (same engine REA calls for APKs).
"""
import re
import struct
import sys
import zipfile
from collections import Counter

DEX_RE = re.compile(r'classes\d*\.dex\Z')


def uleb(data, off):
    """Decode an unsigned LEB128 at off -> (value, next_off)."""
    result = 0
    shift = 0
    while True:
        byte = data[off]
        off += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, off
        shift += 7


def decode(raw):
    try:
        return raw.decode('utf-8')
    except UnicodeDecodeError:
        return raw.decode('utf-8', 'replace')


class Dex:
    """Minimal DEX reader: string/type/method/field/class tables only."""

    def __init__(self, data, name):
        self.data = data
        self.name = name
        self.n_str, self.o_str = struct.unpack_from('<II', data, 56)
        self.n_typ, self.o_typ = struct.unpack_from('<II', data, 64)
        self.n_fld, self.o_fld = struct.unpack_from('<II', data, 80)
        self.n_mth, self.o_mth = struct.unpack_from('<II', data, 88)
        self.n_cls, self.o_cls = struct.unpack_from('<II', data, 96)
        self._strings = None
        self._types = None

    @property
    def strings(self):
        if self._strings is None:
            out = []
            for i in range(self.n_str):
                off = struct.unpack_from('<I', self.data, self.o_str + 4 * i)[0]
                _, pos = uleb(self.data, off)
                end = self.data.find(b'\0', pos)
                out.append(decode(self.data[pos:end]))
            self._strings = out
        return self._strings

    @property
    def types(self):
        if self._types is None:
            strings = self.strings
            self._types = [
                strings[struct.unpack_from('<I', self.data, self.o_typ + 4 * i)[0]]
                for i in range(self.n_typ)
            ]
        return self._types

    def methods(self):
        strings, types = self.strings, self.types
        for i in range(self.n_mth):
            cls, _proto, name = struct.unpack_from('<HHI', self.data, self.o_mth + 8 * i)
            yield types[cls], strings[name]

    def classes(self):
        types = self.types
        for i in range(self.n_cls):
            base = self.o_cls + 32 * i
            cls = struct.unpack_from('<I', self.data, base)[0]
            sup = struct.unpack_from('<I', self.data, base + 8)[0]
            yield types[cls], (types[sup] if sup != 0xFFFFFFFF else None)


def java_name(desc):
    return desc[1:-1].replace('/', '.') if desc.startswith('L') else desc


def parse_args(argv):
    opts = {'filter': None, 'top': 30, 'max': 200}
    positional = []
    i = 0
    while i < len(argv):
        arg = argv[i]
        if arg in ('--filter', '--top', '--max') and i + 1 < len(argv):
            key = arg[2:]
            opts[key] = argv[i + 1] if key == 'filter' else int(argv[i + 1])
            i += 2
            continue
        positional.append(arg)
        i += 1
    if opts['filter'] is not None:
        opts['filter'] = re.compile(opts['filter'])
    return positional, opts


def cmd_summary(dexes, opts):
    total = Counter()
    for dex in dexes:
        print(f"{dex.name}: strings={dex.n_str} types={dex.n_typ} "
              f"methods={dex.n_mth} fields={dex.n_fld} classes={dex.n_cls}")
        total.update(strings=dex.n_str, types=dex.n_typ,
                     methods=dex.n_mth, fields=dex.n_fld, classes=dex.n_cls)
    print(f"TOTAL dex={len(dexes)} " + " ".join(f"{k}={v}" for k, v in sorted(total.items())))

    packages = Counter()
    for dex in dexes:
        for desc, _super in dex.classes():
            name = java_name(desc)
            parts = name.split('.')
            packages['.'.join(parts[:3]) if len(parts) > 2 else name] += 1
    print("\n== defined packages (first 3 segments) ==")
    for pkg, count in packages.most_common(opts['top']):
        print(f"{count:7d}  {pkg}")


def cmd_classes(dexes, opts):
    pattern = opts['filter']
    for dex in dexes:
        for desc, _super in dex.classes():
            name = java_name(desc)
            if pattern is None or pattern.search(name):
                print(name)


def cmd_api(dexes, opts):
    defined = {desc for dex in dexes for desc, _super in dex.classes()}
    counts = Counter()
    pattern = opts['filter']
    for dex in dexes:
        for desc, name in dex.methods():
            if desc in defined:
                continue
            java = java_name(desc)
            if pattern is not None and not pattern.search(f'{java}.{name}'):
                continue
            counts['.'.join(java.split('.')[:3])] += 1
    print("== external method refs by package (declared classes are both sides of a call, not counted out) ==")
    for pkg, count in counts.most_common(opts['top']):
        print(f"{count:7d}  {pkg}")


def cmd_search(dexes, pattern, opts):
    hits = 0
    for dex in dexes:
        for value in dex.strings:
            if not pattern.search(value):
                continue
            print(f"[{dex.name}] {value}")
            hits += 1
            if hits >= opts['max']:
                print(f"... (capped at {opts['max']})")
                return
    print(f"# hits={hits}", file=sys.stderr)


def cmd_native(archive, opts):
    pattern = opts['filter']
    for entry in archive.namelist():
        if not entry.endswith('.so'):
            continue
        blob = archive.read(entry)
        runs = [m.group().decode() for m in re.finditer(rb'[\x20-\x7e]{6,}', blob)]
        if pattern is not None:
            runs = [r for r in runs if pattern.search(r)]
        print(f"### {entry}  ({len(blob)} bytes, {len(runs)} string-runs)")
        for run in runs[:opts['max']]:
            print('   ', run)


ELF_MACHINES = {40: 'ARM', 183: 'AArch64', 62: 'x86-64', 3: 'x86'}


def read_elf64(blob):
    """Parse an ELF64 little-endian object into sections + exported functions."""
    if blob[:4] != b'\x7fELF' or blob[4] != 2 or blob[5] != 1:
        return None
    e_type, e_machine = struct.unpack_from('<HH', blob, 16)
    e_shoff, = struct.unpack_from('<Q', blob, 40)
    e_shentsize, e_shnum, e_shstrndx = struct.unpack_from('<HHH', blob, 58)
    sections = []
    for i in range(e_shnum):
        base = e_shoff + i * e_shentsize
        sh_name, sh_type = struct.unpack_from('<II', blob, base)
        sh_offset, sh_size = struct.unpack_from('<QQ', blob, base + 24)
        sh_link, = struct.unpack_from('<I', blob, base + 40)
        sh_entsize, = struct.unpack_from('<Q', blob, base + 56)
        sections.append({'name_off': sh_name, 'type': sh_type, 'offset': sh_offset,
                         'size': sh_size, 'link': sh_link, 'entsize': sh_entsize})

    def string_at(table, off):
        end = table.find(b'\0', off)
        return table[off:end].decode('utf-8', 'replace')

    shstr = b''
    if e_shstrndx < len(sections):
        sec = sections[e_shstrndx]
        shstr = blob[sec['offset']:sec['offset'] + sec['size']]
    for sec in sections:
        sec['name'] = string_at(shstr, sec['name_off']) if shstr else ''

    exports = []
    for sec in sections:
        if sec['type'] != 11 or sec['entsize'] != 24:  # SHT_DYNSYM
            continue
        strtab = b''
        if sec['link'] < len(sections):
            linked = sections[sec['link']]
            strtab = blob[linked['offset']:linked['offset'] + linked['size']]
        for off in range(sec['offset'], sec['offset'] + sec['size'], 24):
            st_name, = struct.unpack_from('<I', blob, off)
            st_info = blob[off + 4]
            kind, bind = st_info & 0xF, st_info >> 4
            if kind != 2 or bind not in (1, 2):  # STT_FUNC and GLOBAL/WEAK
                continue
            name = string_at(strtab, st_name) if strtab else ''
            if name:
                exports.append(name)
        break
    return {'type': e_type, 'machine': ELF_MACHINES.get(e_machine, str(e_machine)),
            'sections': len(sections), 'exports': exports}


def cmd_elf(archive, opts):
    pattern = opts['filter']
    for entry in sorted(n for n in archive.namelist() if n.endswith('.so')):
        blob = archive.read(entry)
        info = read_elf64(blob)
        if info is None:
            print(f"### {entry}  ({len(blob)} B) — not ELF64/LE (32-bit or packed obj?)")
            continue
        names = info['exports']
        jni = [n for n in names if n.startswith('Java_') or n == 'JNI_OnLoad']
        print(f"### {entry}  ({len(blob)} B · {info['machine']} · type={info['type']} · "
              f"{info['sections']} sections · {len(names)} exported funcs · {len(jni)} JNI)")
        shown = [n for n in jni if pattern is None or pattern.search(n)] or \
                [n for n in names if pattern is None or pattern.search(n)]
        for name in shown[:opts['max']]:
            print('   ', name)
        if len(shown) > opts['max']:
            print(f"    ... +{len(shown) - opts['max']} more")


def main(argv):
    if len(argv) < 3:
        print(__doc__)
        return 2
    apk, command = argv[1], argv[2]
    positional, opts = parse_args(argv[3:])

    archive = zipfile.ZipFile(apk)
    names = sorted((n for n in archive.namelist() if DEX_RE.match(n)),
                   key=lambda n: (len(n), n))
    if not names and command != 'native':
        print(f"refdump: no classes*.dex in {apk}", file=sys.stderr)
        return 1
    dexes = [Dex(archive.read(n), n) for n in names]

    if command == 'summary':
        cmd_summary(dexes, opts)
    elif command == 'classes':
        cmd_classes(dexes, opts)
    elif command == 'api':
        cmd_api(dexes, opts)
    elif command in ('search', 'strings'):
        pattern = re.compile(positional[0]) if positional else opts['filter']
        if pattern is None:
            print("refdump: search needs a pattern", file=sys.stderr)
            return 2
        cmd_search(dexes, pattern, opts)
    elif command == 'native':
        cmd_native(archive, opts)
    elif command == 'elf':
        cmd_elf(archive, opts)
    else:
        print(f"refdump: unknown command {command}", file=sys.stderr)
        return 2
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
