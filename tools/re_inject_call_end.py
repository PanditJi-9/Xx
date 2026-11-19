#!/usr/bin/env python3
"""Inject one direct ``invoke-static`` call just before a method's final ``return-void``.

Why this exists next to ``re_inject_call.py``: that tool injects at the *start*
of a method, which is wrong for `MultiSubScreenBaseActivity.onCreate` — the
hijack has to run after the activity finished its own setup, otherwise
`onPause`/`onResume` read the `h` lifecycle field before it is assigned
(measured in the decoded class: both read it with no null check).

Measured 2026-11-19 (build 44570122475 + `dumpsys activity broadcasts`):
MIUI rejects every broadcast that would start `cn.nubia.gameassist` with
`Security_WakePath Restrict`, and the MIUI autostart appops we tried
(10001/10002/10021/10053) did not lift it. `startActivity` from a foreground
app is not filtered, so GameSpace starts an exported GameAssist activity and
that activity ends itself through this injected call.

Usage:
    python3 tools/re_inject_call_end.py --decoded <dir> \
        --class cn.nubia.multisubscreen.ui.MultiSubScreenBaseActivity \
        --method onCreate --signature '(Landroid/os/Bundle;)V' \
        --target 'Lcn/nubia/gameassist/MultiSubScreenHijack;->maybeFinish(Landroid/app/Activity;)V'
"""

from __future__ import annotations

import argparse
import sys
import tempfile
from pathlib import Path

SAMPLE = """\
.class public Lcn/nubia/multisubscreen/ui/MultiSubScreenBaseActivity;
.super Landroid/app/Activity;

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 2

    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    invoke-static {p1}, Lcom/zte/gameassist/common/SystemMgr;->y(Landroid/content/Context;)Lcom/zte/gameassist/common/SystemMgr;

    return-void
.end method
"""

TARGET = "Lcn/nubia/gameassist/MultiSubScreenHijack;->maybeFinish(Landroid/app/Activity;)V"


def method_span(lines: list[str], method: str, signature: str | None) -> tuple[int, int]:
    """Return (header_index, end_index) of the one method that matches."""
    hits: list[tuple[int, int]] = []
    # A smali declaration carries no `->`: ".method protected onCreate(Landroid/os/Bundle;)V".
    header = f"{method}("
    for i, line in enumerate(lines):
        if not line.startswith(".method") or header not in line:
            continue
        if signature and f"{method}{signature}" not in line:
            continue
        for j in range(i + 1, len(lines)):
            if lines[j].startswith(".end method"):
                hits.append((i, j))
                break
    if len(hits) != 1:
        raise SystemExit(f"{method}: expected exactly one method, found {len(hits)}")
    return hits[0]


def inject(text: str, method: str, signature: str | None, target: str, arg: str) -> tuple[str, bool]:
    """Return (new text, changed). Raises when the shape is not what we expect."""
    lines = text.split("\n")
    start, end = method_span(lines, method, signature)
    body = lines[start:end]
    if any(target in line for line in body):
        return text, False
    returns = [i for i, line in enumerate(body) if line.strip() == "return-void"]
    if len(returns) != 1:
        raise SystemExit(
            f"{method}: expected exactly one return-void in the body, found {len(returns)}"
        )
    call = f"    invoke-static {{{arg}}}, {target}"
    lines.insert(start + returns[0], call)
    return "\n".join(lines), True


def self_test() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "decoded" / "smali" / "MultiSubScreenBaseActivity.smali"
        path.parent.mkdir(parents=True)
        path.write_text(SAMPLE, encoding="utf-8")

        text, changed = inject(SAMPLE, "onCreate", "(Landroid/os/Bundle;)V", TARGET, "p0")
        assert changed, "first pass must change the file"
        body = text.split("\n")
        call_at = body.index(f"    invoke-static {{p0}}, {TARGET}")
        # The call must sit after the original last instruction and before the return.
        assert body[call_at + 1].strip() == "return-void", body[call_at + 1]
        assert body[call_at - 2].strip().startswith("invoke-static {p1}"), body[call_at - 2]

        again, changed_again = inject(text, "onCreate", "(Landroid/os/Bundle;)V", TARGET, "p0")
        assert not changed_again and again == text, "second pass must be a no-op"

        bad = "".join([SAMPLE.replace("    return-void\n", "")])
        try:
            inject(bad, "onCreate", "(Landroid/os/Bundle;)V", TARGET, "p0")
        except SystemExit:
            pass
        else:
            raise AssertionError("expected SystemExit when there is no return-void")

    print(
        "self-test OK: inserts one call before the single return-void, "
        "is idempotent, refuses a body without a return"
    )
    return 0


def resolve_file(decoded: Path, cls: str) -> Path:
    rel = cls.replace(".", "/") + ".smali"
    hits = sorted(decoded.glob(f"smali*/{rel}"))
    if len(hits) != 1:
        raise SystemExit(f"{cls}: expected one smali file, found {len(hits)}")
    return hits[0]


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--decoded", help="apktool output directory")
    parser.add_argument("--class", dest="cls", help="fully qualified class name")
    parser.add_argument("--method", help="method name")
    parser.add_argument("--signature", help="method signature, e.g. '(Landroid/os/Bundle;)V'")
    parser.add_argument("--target", help="Lclass;->method(sig)ret of the static call")
    parser.add_argument("--arg", default="p0", help="register passed to the call (default p0)")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args(argv)

    if args.self_test:
        return self_test()

    if not (args.decoded and args.cls and args.method and args.target):
        parser.error("--decoded, --class, --method and --target are required")

    target = Path(args.decoded) / ""
    path = resolve_file(target, args.cls)
    text = path.read_text(encoding="utf-8")
    out, changed = inject(text, args.method, args.signature, args.target, args.arg)
    if not changed:
        print(f"already injected: {path.relative_to(target)}")
        return 0
    path.write_text(out, encoding="utf-8")
    print(f"{path.relative_to(target)}: injected {args.target} before return-void")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
