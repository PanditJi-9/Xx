#!/usr/bin/env python3
"""Read an APK's signing identity from its bytes.

REA's pinned Android release answers ``capability_unavailable`` for signature
analysis, so this tool covers that gap with the formats themselves:

* v1 (JAR signing): ``META-INF/*.RSA|DSA|EC`` is PKCS#7; the leaf certificate
  is extracted with ``openssl pkcs7``.
* v2/v3/v3.1 (APK Signing Block): the block is located through the ZIP end of
  central directory, its scheme IDs are listed, and the first signer
  certificate is decoded from the signed-data section.

No APK is executed and nothing is installed. ``openssl`` is used only to render
certificate fields that are already embedded in the file.

Usage:
    python3 tools/re_apk_signature.py TARGET.apk [--json PATH] [--text PATH]
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import struct
import subprocess
import sys
import zipfile

SCHEME_IDS = {
    0x7109871A: "v2",
    0xF05368C0: "v3",
    0x1B93AD61: "v3.1",
    0x2146444E: "SourceStamp",
}


def find_eocd(handle, size: int) -> int | None:
    """Return the offset of the ZIP end-of-central-directory record."""
    tail_len = min(size, 65557 + 22)
    handle.seek(size - tail_len)
    tail = handle.read(tail_len)
    index = tail.rfind(b"PK\x05\x06")
    return size - tail_len + index if index >= 0 else None


def read_signing_block(handle, size: int) -> tuple[int, list[tuple[int, bytes]]] | None:
    """Return (cd_offset, [(scheme_id, value), ...]) or None when unsigned."""
    eocd = find_eocd(handle, size)
    if eocd is None:
        return None
    handle.seek(eocd + 16)
    cd_offset = int.from_bytes(handle.read(4), "little")
    if cd_offset < 32 or cd_offset > size:
        return None
    handle.seek(cd_offset - 16)
    if handle.read(16) != b"APK Sig Block 42":
        return None
    handle.seek(cd_offset - 24)
    block_size = int.from_bytes(handle.read(8), "little")
    block_start = cd_offset - block_size - 8
    if block_start < 0:
        return None
    handle.seek(block_start)
    block = handle.read(block_size + 8)
    pairs: list[tuple[int, bytes]] = []
    position = 8
    end = len(block) - 24
    while position + 12 <= end:
        pair_len = int.from_bytes(block[position:position + 8], "little")
        if pair_len < 4 or position + 8 + pair_len > end:
            break
        scheme_id = int.from_bytes(block[position + 8:position + 12], "little")
        pairs.append((scheme_id, block[position + 12:position + 8 + pair_len]))
        position += 8 + pair_len
    return cd_offset, pairs


def lp_u32(data: bytes, offset: int) -> tuple[bytes, int]:
    """Read a little-endian uint32 length prefix and the bytes that follow."""
    if offset + 4 > len(data):
        return b"", len(data)
    length = int.from_bytes(data[offset:offset + 4], "little")
    start = offset + 4
    return data[start:start + length], start + length


def _is_der_certificate(blob: bytes) -> bool:
    """A certificate is a DER SEQUENCE, so it must start with tag 0x30."""
    return len(blob) > 64 and blob[0] == 0x30


def first_certificate(value: bytes) -> bytes | None:
    """First signer certificate from a v2 or v3 block value.

    v2 signed data is digests, certificates, attributes. v3 inserts the
    min/max SDK pair, and implementations disagree on whether that pair sits
    before or after the certificates, so both layouts are tried and the
    candidate must decode as a DER SEQUENCE. Measured: the first layout alone
    returned a non-certificate for v3, which is why this check exists.
    """
    signers, _ = lp_u32(value, 0)          # sequence of signers
    signer, _ = lp_u32(signers, 0)         # first signer
    signed_data, _ = lp_u32(signer, 0)     # signed data
    for start in (0, 8):
        _, cursor = lp_u32(signed_data, start)   # digests
        certificates, _ = lp_u32(signed_data, cursor)
        certificate, _ = lp_u32(certificates, 0)
        if _is_der_certificate(certificate):
            return certificate
    return None


def pkcs7_certificate(handle, name: str) -> bytes | None:
    """Leaf certificate from a v1 PKCS#7 signature block."""
    with zipfile.ZipFile(handle) as archive:
        try:
            raw = archive.read(name)
        except KeyError:
            return None
    result = subprocess.run(
        ["openssl", "pkcs7", "-inform", "DER", "-print_certs", "-outform", "PEM"],
        input=raw,
        capture_output=True,
    )
    if result.returncode != 0:
        return None
    pem = result.stdout
    match = re.search(rb"-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----", pem, re.S)
    if not match:
        return None
    der = subprocess.run(
        ["openssl", "x509", "-outform", "DER"],
        input=match.group(0),
        capture_output=True,
    )
    return der.stdout or None


def describe(der: bytes) -> dict:
    """Certificate fields rendered by openssl from bytes already in the APK."""
    fields = {
        "sha256": hashlib.sha256(der).hexdigest(),
        "subject": None,
        "issuer": None,
        "serial": None,
        "not_before": None,
        "not_after": None,
    }
    try:
        result = subprocess.run(
            ["openssl", "x509", "-inform", "DER", "-noout", "-subject", "-issuer",
             "-serial", "-dates", "-fingerprint", "-sha256"],
            input=der,
            capture_output=True,
        )
    except OSError as exc:
        fields["openssl_error"] = str(exc)
        return fields
    if result.returncode != 0:
        fields["openssl_error"] = result.stderr.decode("utf-8", "replace").strip()
        return fields
    text = result.stdout.decode("utf-8", "replace")
    for line in text.splitlines():
        if "=" in line:
            key, _, value = line.partition("=")
            key = key.strip().lower()
            value = value.strip()
            if key == "subject":
                fields["subject"] = value
            elif key == "issuer":
                fields["issuer"] = value
            elif key == "serial":
                fields["serial"] = value
            elif key == "notbefore":
                fields["not_before"] = value
            elif key == "notafter":
                fields["not_after"] = value
            elif key.startswith("sha256 fingerprint"):
                fields["openssl_sha256"] = value.replace(":", "").lower()
    return fields


def analyse(path: str) -> dict:
    rev = {"target": path, "schemes": {}, "certificates": [], "notes": []}
    with open(path, "rb") as handle:
        size = handle.seek(0, 2)
        block = read_signing_block(handle, size)
        if block is None:
            rev["notes"].append("No APK Signing Block found before the central directory.")
        else:
            _, pairs = block
            for scheme_id, value in pairs:
                name = SCHEME_IDS.get(scheme_id, f"0x{scheme_id:08x}")
                rev["schemes"][name] = True
                if name in ("v2", "v3"):
                    der = first_certificate(value)
                    if der:
                        info = describe(der)
                        info["from"] = name
                        rev["certificates"].append(info)

        with zipfile.ZipFile(handle) as archive:
            names = [n for n in archive.namelist()
                     if n.startswith("META-INF/") and n.upper().endswith((".RSA", ".DSA", ".EC"))]
        if names:
            rev["schemes"]["v1"] = True
            for name in names[:3]:
                der = pkcs7_certificate(handle, name)
                if der:
                    info = describe(der)
                    info["from"] = f"v1:{name}"
                    rev["certificates"].append(info)
        else:
            rev["notes"].append("No v1 JAR signature files in META-INF/.")
    return rev


def render(rev: dict) -> str:
    lines = [f"target: {rev['target']}", "schemes present: " +
             (", ".join(sorted(k for k, v in rev["schemes"].items() if v)) or "none")]
    if not rev["certificates"]:
        lines.append("certificates: none decoded")
    for index, cert in enumerate(rev["certificates"], start=1):
        lines.append("")
        lines.append(f"signer {index} (from {cert['from']}):")
        lines.append(f"  subject: {cert['subject']}")
        lines.append(f"  issuer: {cert['issuer']}")
        lines.append(f"  serial: {cert['serial']}")
        lines.append(f"  validity: {cert['not_before']} .. {cert['not_after']}")
        lines.append(f"  cert SHA-256: {cert['sha256']}")
    for note in rev["notes"]:
        lines.append(f"note: {note}")
    return "\n".join(lines)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk")
    parser.add_argument("--json", dest="json_path")
    parser.add_argument("--text", dest="text_path")
    args = parser.parse_args(argv)

    try:
        rev = analyse(args.apk)
    except (OSError, zipfile.BadZipFile, struct.error) as exc:
        print(f"could not read {args.apk}: {exc}", file=sys.stderr)
        return 2

    text = render(rev)
    print(text)
    if args.text_path:
        with open(args.text_path, "w", encoding="utf-8") as handle:
            handle.write(text + "\n")
    if args.json_path:
        with open(args.json_path, "w", encoding="utf-8") as handle:
            json.dump(rev, handle, indent=2)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
