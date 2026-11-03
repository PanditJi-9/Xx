#!/usr/bin/env python3
"""refbundle — transfer manifest with SHA-256 verification (stdlib only).

Why this exists: reference artifacts must be able to move to another host (an x64
Codespace, for JADX/Ghidra runs) without ever trusting the copy. `make` records
size + SHA-256 for every file; `verify` re-hashes a destination tree and fails on
any missing or changed file; `self-test` proves the checker detects tampering.

Usage:
  refbundle.py make   <manifest.json> <file>...     # writes manifest (+ SHA256SUMS)
  refbundle.py verify <manifest.json> [<root>]      # re-hash; exit 1 on mismatch
  refbundle.py self-test                            # negative test, exit 0 only if caught
  refbundle.py sums   <manifest.json>               # print `<sha>  <path>` lines
"""
import hashlib
import json
import os
import sys
import tempfile

CHUNK = 1 << 20


def sha256(path):
    digest = hashlib.sha256()
    with open(path, 'rb') as handle:
        for block in iter(lambda: handle.read(CHUNK), b''):
            digest.update(block)
    return digest.hexdigest()


def make(manifest_path, files):
    entries = []
    for path in files:
        if not os.path.isfile(path):
            print(f"refbundle: not a file: {path}", file=sys.stderr)
            return 2
        entries.append({'path': os.path.abspath(path),
                        'size': os.path.getsize(path),
                        'sha256': sha256(path)})
    payload = {'version': 1, 'count': len(entries), 'files': entries}
    with open(manifest_path, 'w', encoding='utf-8') as handle:
        json.dump(payload, handle, indent=2)
        handle.write('\n')
    sums = '\n'.join(f"{e['sha256']}  {e['path']}" for e in entries) + '\n'
    with open(manifest_path + '.sha256', 'w', encoding='utf-8') as handle:
        handle.write(sums)
    print(f"wrote {manifest_path} ({len(entries)} files) + {manifest_path}.sha256")
    return 0


def verify(manifest_path, root=None):
    with open(manifest_path, encoding='utf-8') as handle:
        payload = json.load(handle)
    bad = 0
    for entry in payload.get('files', []):
        path = entry['path']
        if root:
            path = os.path.join(root, os.path.basename(path))
        if not os.path.exists(path):
            print(f"MISSING  {path}")
            bad += 1
            continue
        size = os.path.getsize(path)
        if size != entry['size']:
            print(f"SIZE     {path}  (expected {entry['size']}, got {size})")
            bad += 1
            continue
        actual = sha256(path)
        if actual != entry['sha256']:
            print(f"CHANGED  {path}\n  expected {entry['sha256']}\n  actual   {actual}")
            bad += 1
        else:
            print(f"OK       {path}")
    total = payload.get('count', len(payload.get('files', [])))
    print(f"\n{total - bad}/{total} verified · {bad} mismatch(es)")
    return 1 if bad else 0


def sums(manifest_path):
    with open(manifest_path, encoding='utf-8') as handle:
        payload = json.load(handle)
    for entry in payload.get('files', []):
        print(f"{entry['sha256']}  {entry['path']}")
    return 0


def self_test():
    with tempfile.TemporaryDirectory() as tmp:
        src = os.path.join(tmp, 'artifact.bin')
        with open(src, 'wb') as handle:
            handle.write(os.urandom(4096))
        manifest = os.path.join(tmp, 'manifest.json')
        assert make(manifest, [src]) == 0, 'make failed'

        copy = os.path.join(tmp, 'copy.bin')
        with open(src, 'rb') as handle:
            data = handle.read()
        with open(copy, 'wb') as handle:
            handle.write(data)

        copied_manifest = os.path.join(tmp, 'copy-manifest.json')
        payload = json.load(open(manifest, encoding='utf-8'))
        payload['files'][0]['path'] = copy
        json.dump(payload, open(copied_manifest, 'w', encoding='utf-8'))

        cases = []
        cases.append(('pristine copy verifies', verify(copied_manifest) == 0))

        with open(copy, 'r+b') as handle:      # flip one byte deep in the file
            handle.seek(2048)
            byte = handle.read(1)
            handle.seek(2048)
            handle.write(bytes([byte[0] ^ 0x01]))
        cases.append(('one flipped byte is caught', verify(copied_manifest) == 1))

        os.truncate(copy, 100)
        cases.append(('truncated file is caught', verify(copied_manifest) == 1))

        os.remove(copy)
        cases.append(('missing file is caught', verify(copied_manifest) == 1))

    passed = sum(1 for _, ok in cases if ok)
    for label, ok in cases:
        print(f"{'PASS' if ok else 'FAIL'}  {label}")
    print(f"\nself-test {passed}/{len(cases)}")
    return 0 if passed == len(cases) else 1


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    command = argv[1]
    if command == 'make' and len(argv) >= 3:
        return make(argv[2], argv[3:])
    if command == 'verify' and len(argv) >= 3:
        return verify(argv[2], argv[3] if len(argv) > 3 else None)
    if command == 'sums' and len(argv) >= 3:
        return sums(argv[2])
    if command == 'self-test':
        return self_test()
    print(__doc__)
    return 2


if __name__ == '__main__':
    sys.exit(main(sys.argv))
