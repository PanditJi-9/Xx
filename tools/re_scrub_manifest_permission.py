#!/usr/bin/env python3
"""Remove a permission *name* from a compiled manifest by an equal-length swap.

Why: MIUI's DisplayPolicy refuses the GameFloat panel with

    SecurityException: DisplayPolicy: uid … does not have
      android.permission.INTERNAL_SYSTEM_WINDOW

and the system's own log shows why the check exists at all:

    ScnModule: [GameDetection] requestedPermissions …: cn.zte.gamefloat: […,
      android.permission.INTERNAL_SYSTEM_WINDOW, SYSTEM_OVERLAY_WINDOW, …]

The app declares the privileged permissions it was signed for on Nubia, and it
can never be granted them here. The question this tool answers is whether the
refusal keys off the *declaration* rather than the window's runtime type — the
type was already forced to 2038 (TYPE_APPLICATION_OVERLAY) and the refusal did
not move.

How: the manifest is kept byte-for-byte by `apktool b --copy-original`, so the
permission survives only as a string in the AXML string pool. Swapping it for an
equally long, unknown name keeps every pool offset valid while making the
declaration meaningless — an unknown permission is simply not granted, exactly
as before, but a policy that greps for the name no longer sees it.

Equal length is the whole trick, and it is asserted. Both encodings are handled
because the pool is UTF-8 for the manifest but the same text can appear as
UTF-16LE.

Usage:
    python3 tools/re_scrub_manifest_permission.py --apk unsigned.apk \
        --permission android.permission.INTERNAL_SYSTEM_WINDOW

    python3 tools/re_scrub_manifest_permission.py --self-test
"""

from __future__ import annotations

import argparse
import io
import sys
import tempfile
import zipfile

MANIFEST = "AndroidManifest.xml"


def replacement_for(permission: str) -> str:
    """A same-length name that no platform will ever grant."""
    if not permission:
        raise ValueError("empty permission")
    last = "X" if permission[-1] != "X" else "W"
    return permission[:-1] + last


def scrub(data: bytes, permission: str) -> tuple[bytes, int]:
    """Return (patched bytes, replacements). Raises when lengths would drift."""
    new = replacement_for(permission)
    assert len(new) == len(permission)
    total = 0
    for encoding in ("utf-8", "utf-16-le"):
        old_bytes = permission.encode(encoding)
        new_bytes = new.encode(encoding)
        assert len(old_bytes) == len(new_bytes), (encoding, len(old_bytes), len(new_bytes))
        total += data.count(old_bytes)
        data = data.replace(old_bytes, new_bytes)
    return data, total


def patch_apk(path: str, permission: str) -> int:
    with zipfile.ZipFile(path) as source:
        entries = [(info, source.read(info.filename)) for info in source.infolist()]
    patched = 0
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as target:
        for info, payload in entries:
            if info.filename == MANIFEST:
                payload, patched = scrub(payload, permission)
            target.writestr(info, payload)
    if not patched:
        raise SystemExit(f"error: {permission} not found in {path}/{MANIFEST}")
    with open(path, "wb") as handle:
        handle.write(out.getvalue())
    return patched


def self_test() -> int:
    permission = "android.permission.INTERNAL_SYSTEM_WINDOW"
    with tempfile.TemporaryDirectory() as tmp:
        apk = f"{tmp}/sample.apk"
        manifest = b"<manifest>" + permission.encode("utf-16-le") + b"</manifest>"
        entry = zipfile.ZipInfo(MANIFEST)
        with zipfile.ZipFile(apk, "w") as archive:
            archive.writestr(entry, manifest)
            archive.writestr("classes.dex", b"dex\n035\x00payload")

        count = patch_apk(apk, permission)
        if count != 1:
            print(f"SELF-TEST FAILED: expected 1 replacement, got {count}", file=sys.stderr)
            return 1
        with zipfile.ZipFile(apk) as archive:
            patched = archive.read(MANIFEST)
            if permission.encode() in patched or permission.encode("utf-16-le") in patched:
                print("SELF-TEST FAILED: permission survived", file=sys.stderr)
                return 1
            if len(patched) != len(manifest):
                print("SELF-TEST FAILED: manifest length drifted", file=sys.stderr)
                return 1
            if archive.read("classes.dex") != b"dex\n035\x00payload":
                print("SELF-TEST FAILED: unrelated entry changed", file=sys.stderr)
                return 1

        # A missing permission must fail loudly, not silently sign a bad build.
        try:
            patch_apk(apk, "android.permission.INTERNAL_SYSTEM_WINDOW")
        except SystemExit:
            pass
        else:
            print("SELF-TEST FAILED: absent permission did not fail", file=sys.stderr)
            return 1

    print("self-test OK: equal-length swap, other entries untouched, loud on absence")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", help="APK to patch in place")
    parser.add_argument("--permission", default="android.permission.INTERNAL_SYSTEM_WINDOW")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)
    if args.self_test:
        return self_test()
    if not args.apk:
        parser.error("--apk is required")
    count = patch_apk(args.apk, args.permission)
    print(f"scrubbed {args.permission} ({count} occurrence(s)) from {args.apk}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
