#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ملخّص تقرير اختبارات JUnit — **إثبات العدد لا ادّعاؤه**.

### لماذا وُجد (تكملة `PERF-AUDIT-01`)

سجلّ Gradle يقول `BUILD SUCCESSFUL` ولا يقول **كم اختبارًا نُفِّذ**؛ والعدد يُكتب في
`HANDOFF` بلا تقرير موثوق. فصار هذا الملفّ يقرأ XML الذي يكتبه Gradle نفسه
(`manager/app/build/test-results/test*UnitTest/*.xml`) ويكتب جدولًا في
`$GITHUB_STEP_SUMMARY`، فيُقرأ عدد الاختبارات في صفحة التشغيل بلا تنزيل حزمة.

### القاعدة

- **لا يُفشل التشغيل.** التقرير غائب يعني «لم تصل مهمة الاختبار إلى نهايتها» — وهو فشل
  ظاهر في الخطوة السابقة أصلًا؛ فلا تُضاعف الرسالة.
- **لا جمع مزدوج.** تُقرأ مجلّدات `test*UnitTest` وحدها، وكل مجلّد صفّ مستقلّ (Release
  وDebug قد يتعايشان على جهاز مطوّر). والمجموع أسفل الجدول لمجلّدٍ واحد فهو مجموع حقيقي.
- قيم السمات مفقودة أو غير رقمية ⇒ صفر، بلا انفجار؛ والقارئ لا يخترع رقمًا.

الاستعمال:

    python3 tools/test_report_summary.py                 # Markdown إلى المخرَج القياسيّ
    python3 tools/test_report_summary.py --root DIR      # جذر تقارير آخر
    python3 tools/test_report_summary.py --self-test     # يقيس الأداة نفسها
"""
from __future__ import annotations

import argparse
import glob
import os
import sys
import tempfile
import xml.etree.ElementTree as ET

DEFAULT_ROOT = os.path.join("manager", "app", "build", "test-results")
FIELDS = ("tests", "failures", "errors", "skipped")


def _as_int(value: str | None) -> int:
    try:
        return int(value or 0)
    except (TypeError, ValueError):
        return 0


def counts_for_file(path: str) -> tuple[int, int, int, int]:
    """(tests, failures, errors, skipped) لملفّ واحد، أو أصفارًا إن تعذّر قراءته."""
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return (0, 0, 0, 0)
    # Gradle يكتب `<testsuite>` لكل صنف. وحمولة `<testsuites>` (غير المعتادة هنا) تُجمع
    # أبناؤها بدل قراءة سماتها المجمَّعة، حتى لا تُحسب مرّتين.
    nodes = [root] if root.tag == "testsuite" else list(root.iter("testsuite"))
    sums = [0, 0, 0, 0]
    for node in nodes:
        for index, field in enumerate(FIELDS):
            sums[index] += _as_int(node.get(field))
    return (sums[0], sums[1], sums[2], sums[3])


def counts_for_dir(directory: str) -> tuple[int, int, int, int, int]:
    files = sorted(glob.glob(os.path.join(directory, "*.xml")))
    tests = failures = errors = skipped = 0
    for path in files:
        t, f, e, s = counts_for_file(path)
        tests += t
        failures += f
        errors += e
        skipped += s
    return (tests, failures, errors, skipped, len(files))


def suite_dirs(root: str) -> list[str]:
    return sorted(
        path
        for path in glob.glob(os.path.join(root, "test*UnitTest"))
        if os.path.isdir(path)
    )


def render(root: str) -> list[str]:
    lines: list[str] = ["### Unit test evidence (JUnit XML)", ""]
    dirs = suite_dirs(root)
    if not dirs:
        lines += [
            "No `test*UnitTest` report directory was found under `%s`." % root,
            "",
            "The test task did not reach execution (the failing step above is the evidence),",
            "or this workflow ran without producing JUnit XML.",
        ]
        return lines

    lines += [
        "| suite dir | XML files | tests | failures | errors | skipped |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    total = [0, 0, 0, 0, 0]
    for directory in dirs:
        tests, failures, errors, skipped, files = counts_for_dir(directory)
        for index, value in enumerate((tests, failures, errors, skipped, files)):
            total[index] += value
        lines.append(
            "| `%s` | %d | %d | %d | %d | %d |"
            % (os.path.basename(directory), files, tests, failures, errors, skipped)
        )
    if len(dirs) > 1:
        lines.append(
            "| **total** | %d | %d | %d | %d | %d |"
            % (total[4], total[0], total[1], total[2], total[3])
        )
    lines.append("")
    lines.append(
        "Source: Gradle's own JUnit XML under `%s` — the count is read, not claimed." % root
    )
    return lines


def self_test() -> int:
    """يقيس القارئ على XML معروف: ملفّ صحيح، وحمولة `<testsuites>`، وملفّ تالف."""
    with tempfile.TemporaryDirectory() as tmp:
        release = os.path.join(tmp, "testReleaseUnitTest")
        os.makedirs(release)
        with open(os.path.join(release, "One.xml"), "w", encoding="utf-8") as handle:
            handle.write('<testsuite tests="7" failures="1" errors="2" skipped="3"/>')
        with open(os.path.join(release, "Two.xml"), "w", encoding="utf-8") as handle:
            handle.write(
                '<testsuites><testsuite tests="5" failures="0" errors="0" skipped="0"/></testsuites>'
            )
        with open(os.path.join(release, "Broken.xml"), "w", encoding="utf-8") as handle:
            handle.write("<not-xml")

        got = counts_for_dir(release)
        want = (12, 1, 2, 3, 3)
        if got != want:
            print("self-test FAILED: got %r want %r" % (got, want), file=sys.stderr)
            return 1

        lines = render(tmp)
        if not any("| `testReleaseUnitTest` | 3 | 12 | 1 | 2 | 3 |" in line for line in lines):
            print("self-test FAILED: rendered table does not carry the measured row", file=sys.stderr)
            return 1
    print("self-test OK: 12 tests / 1 failure / 2 errors / 3 skipped from 3 XML files")
    return 0


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Summarize Gradle JUnit XML as Markdown.")
    parser.add_argument("--root", default=DEFAULT_ROOT)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)
    if args.self_test:
        return self_test()
    print("\n".join(render(args.root)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
