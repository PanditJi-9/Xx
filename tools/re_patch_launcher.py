#!/usr/bin/env python3
"""Give one activity a LAUNCHER entry in a decoded (apktool) AndroidManifest.

The measured problem this solves: cn.nubia.gamelauncher declares its activities
with intent.action.MAIN only — no android.intent.category.LAUNCHER anywhere —
so an installed copy has no icon and nothing to open. Adding the category to an
existing entry activity is the smallest edit that produces a launch point.

The tool edits exactly one element and refuses everything else:

* the target activity must exist in the manifest;
* it must already be exported, so the launcher can start it;
* if it already declares LAUNCHER, nothing is written.

Usage:
    python3 tools/re_patch_launcher.py --manifest PATH --activity NAME [--dry-run]
"""

from __future__ import annotations

import argparse
import re
import sys

FILTER = """            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
"""


def patch(xml: str, activity: str) -> tuple[str, str]:
    """Return (new_xml, status). Status is a single line describing the edit."""
    pattern = re.compile(
        r'(<activity\b[^>]*android:name="' + re.escape(activity) + r'"[^>]*>)(.*?)(</activity>)',
        re.S,
    )
    match = pattern.search(xml)
    if not match:
        return xml, f"error: activity {activity} not found"

    head, body, tail = match.groups()
    if 'android:exported="true"' not in head:
        return xml, f"error: {activity} is not exported; the launcher could not start it"
    if "android.intent.category.LAUNCHER" in body:
        return xml, f"no change: {activity} already declares LAUNCHER"

    patched_body = body.rstrip() + "\n" + FILTER
    start, end = match.span()
    new_xml = xml[:start] + head + patched_body + tail + xml[end:]
    return new_xml, f"patched: added LAUNCHER intent-filter to {activity}"


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True)
    parser.add_argument("--activity", required=True)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)

    with open(args.manifest, "r", encoding="utf-8") as handle:
        xml = handle.read()

    patched, status = patch(xml, args.activity)
    print(status)

    if status.startswith("error"):
        return 2
    if patched != xml and not args.dry_run:
        with open(args.manifest, "w", encoding="utf-8") as handle:
            handle.write(patched)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
