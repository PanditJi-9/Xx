#!/usr/bin/env python3
"""Stop privileged settings writes from killing an app that cannot hold the permission.

Measured on the device, in `adb logcat -b crash`, with the repacked APK installed:

    Unable to create application cn.nubia.gamelauncher.GameLauncherApplication:
      SecurityException: Permission denial, must have one of: [WRITE_SECURE_SETTINGS]
        at cn.nubia.common.util.CommonUtil.notifyHandheldModeChanged(CommonUtil.java:203)
        at cn.nubia.common.CommonApplication.onCreate(CommonApplication.java:21)

then, after that method was neutralized, the same exception from

        at cn.nubia.gamelauncher.GameLauncherApplication.doConfig(GameLauncherApplication.java:319)

Both are writes through `Settings.Global`/`Settings.System`, which need
`WRITE_SECURE_SETTINGS` — a permission an app signed with anything but the
platform key can never hold. The write cannot succeed, so the only question is
whether it takes the process down with it. It should not.

Two modes, both of which refuse anything they cannot prove safe:

  wrap  (default) keep the original body and add a `SecurityException` handler
        that returns normally. Behaviour is unchanged wherever the write is
        actually permitted; where it is not, the failure is contained.
        Refused when the method returns a value (a dummy return would invent
        one) and when the body already declares a `.catch`.

  noop  replace the whole body with `return-void`. Only for a method whose
        entire purpose is a write that can never happen.

  --sweep  applies `wrap` to every void method in the tree that performs a
        privileged `Settings.Global|System|Secure.put*` write and has no
        `.catch` of its own. It prints each method it touches, plus the count.

Every edit prints the original bytes before changing them, so the log is the
evidence trail and the patch is reversible by hand.
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path

METHOD_HEADER = re.compile(
    r"^\.method\s+(?P<mods>[^\n]*?)\s*(?P<name>[A-Za-z_$][\w$]*)"
    r"\((?P<params>[^)]*)\)(?P<ret>\S+)\s*$",
    re.MULTILINE,
)

# Settings.Global/System/Secure put*, which is what needs WRITE_SECURE_SETTINGS.
PRIVILEGED_WRITE = re.compile(
    r"Landroid/provider/Settings\$(?:Global|System|Secure);->put(?:String|Int|Long|Float)\("
)


@dataclass
class Method:
    path: Path
    text: str
    match: re.Match[str]
    start: int
    body_end: int  # index of the "\n" that opens ".end method"
    end: int  # index just past ".end method"

    @property
    def name(self) -> str:
        return self.match.group("name")

    @property
    def returns_void(self) -> bool:
        return self.match.group("ret") == "V"

    @property
    def lines(self) -> list[str]:
        header_end = self.text.find("\n", self.match.start())
        return self.text[header_end + 1 : self.body_end].split("\n")

    @property
    def body(self) -> str:
        return "\n".join(self.lines)


def split_methods(path: Path) -> list[Method]:
    text = path.read_text(encoding="utf-8", errors="surrogateescape")
    methods: list[Method] = []
    for match in METHOD_HEADER.finditer(text):
        end = text.find("\n.end method", match.end())
        if end < 0:
            raise SystemExit(f"{path}: .method {match.group('name')} has no matching .end method")
        methods.append(Method(path, text, match, match.start(), end, end + len("\n.end method")))
    return methods


def wrap(method: Method, tag: str) -> str:
    """Return the method text with its body guarded against SecurityException."""
    head_end = method.text.find("\n", method.match.start())
    header = method.text[method.match.start() : head_end]
    lines = method.lines

    # The guarded range must start after .locals/.registers; a label placed in
    # front of those directives makes the assembler unhappy.
    insert_at = 0
    for index, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith(".locals") or stripped.startswith(".registers"):
            insert_at = index + 1
            break

    guarded = lines[insert_at:]
    while guarded and not guarded[-1].strip():
        guarded.pop()
    if not any(line.strip() and not line.strip().startswith((".", ":", "#")) for line in guarded):
        raise SystemExit(f"refusing to wrap {method.name}: its body has no instructions to guard")

    try_start = f":rps_try_{tag}"
    try_end = f":rps_end_{tag}"
    handler = f":rps_catch_{tag}"

    out = [header, *lines[:insert_at], f"    {try_start}", *guarded, f"    {try_end}"]
    out.append(f"    .catch Ljava/lang/SecurityException; {{{try_start} .. {try_end}}} {handler}")
    out.append("    return-void")
    out.append(f"    {handler}")
    out.append("    return-void")
    out.append(".end method")
    return "\n".join(out)


def noop(method: Method) -> str:
    mods = method.match.group("mods")
    return (
        f".method {mods} {method.name}({method.match.group('params')})V\n"
        f"    .locals 0\n"
        f"\n"
        f"    # Neutralized by tools/re_patch_smali.py: this method only performs a\n"
        f"    # write that needs WRITE_SECURE_SETTINGS, which no repacked app can hold.\n"
        f"    return-void\n"
        f".end method"
    )


def apply(path: Path, methods: list[Method], builder, tag_prefix: str, quiet: bool) -> int:
    """Rewrite the file once, replacing the given methods; returns how many changed."""
    text = methods[0].text
    pieces: list[tuple[int, int, str]] = []
    for index, method in enumerate(methods):
        if not method.returns_void:
            raise SystemExit(
                f"refusing {path.name}#{method.name}: it returns {method.match.group('ret')}, "
                "and a dummy return would silently change behaviour"
            )
        replacement = builder(method, f"{tag_prefix}{index}")
        if not quiet:
            print(f"--- {path}#{method.name}")
            print(method.text[method.start : method.end])
        pieces.append((method.start, method.end, replacement))

    for start, end, replacement in sorted(pieces, reverse=True):
        text = text[:start] + replacement + text[end:]
    path.write_text(text, encoding="utf-8", errors="surrogateescape")
    return len(pieces)


def neutralize_one(decoded: Path, dotted_class: str, method_name: str, mode: str, quiet: bool) -> None:
    relative = Path(dotted_class.replace(".", "/") + ".smali")
    candidates = sorted(decoded.glob(f"smali*/{relative}"))
    if not candidates:
        raise SystemExit(f"no smali file for {dotted_class} under {decoded}")

    hits: list[Method] = []
    for path in candidates:
        hits.extend(m for m in split_methods(path) if m.name == method_name)
    if not hits:
        raise SystemExit(f"{dotted_class}#{method_name} not declared in {[str(c) for c in candidates]}")
    if len(hits) > 1:
        raise SystemExit(f"{method_name} is ambiguous — declared {len(hits)} times")

    method = hits[0]
    if mode == "wrap" and ".catch " in method.body:
        raise SystemExit(f"refusing to wrap {dotted_class}#{method_name}: it already declares a .catch")

    builder = (lambda m, tag: wrap(m, tag)) if mode == "wrap" else (lambda m, tag: noop(m))
    apply(method.path, [method], builder, "one_", quiet)


def sweep_putstring(decoded: Path, quiet: bool) -> int:
    touched_files = 0
    touched_methods = 0
    skipped_catch = 0
    skipped_non_void = 0

    for path in sorted(decoded.glob("smali*/**/*.smali")):
        try:
            methods = split_methods(path)
        except SystemExit as error:
            print(f"skip {path}: {error}", file=sys.stderr)
            continue

        targets: list[Method] = []
        for method in methods:
            body = method.body
            if not PRIVILEGED_WRITE.search(body):
                continue
            if ".catch " in body:
                skipped_catch += 1
                continue
            if not method.returns_void:
                skipped_non_void += 1
                continue
            targets.append(method)

        if not targets:
            continue

        touched_files += 1
        touched_methods += apply(path, targets, wrap, f"{touched_files}_", quiet)

    print(
        f"--- sweep: wrapped {touched_methods} methods in {touched_files} files "
        f"(skipped {skipped_catch} with an existing .catch, {skipped_non_void} non-void)"
    )
    return touched_methods


def scan_put_string(decoded: Path, limit: int) -> None:
    total = 0
    shown = 0
    for path in sorted(decoded.glob("smali*/**/*.smali")):
        text = path.read_text(encoding="utf-8", errors="surrogateescape")
        for match in PRIVILEGED_WRITE.finditer(text):
            total += 1
            if shown < limit:
                line = text.count("\n", 0, match.start()) + 1
                print(f"{path}:{line}: {match.group(0)}")
                shown += 1
    print(f"--- privileged settings writes: {total} call sites ({shown} shown)")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--decoded", required=True, type=Path)
    parser.add_argument("--class", dest="dotted", help="fully qualified class, e.g. cn.nubia.common.util.CommonUtil")
    parser.add_argument("--method", help="method to patch (must return void)")
    parser.add_argument("--mode", choices=("wrap", "noop"), default="wrap")
    parser.add_argument("--sweep", action="store_true", help="wrap every void method performing a privileged write")
    parser.add_argument("--scan-putstring", action="store_true", help="list Settings.Global/System/Secure writes")
    parser.add_argument("--quiet", action="store_true", help="do not print the original bodies (not recommended)")
    parser.add_argument("--limit", type=int, default=40)
    args = parser.parse_args(argv)

    if not args.decoded.is_dir():
        raise SystemExit(f"not a directory: {args.decoded}")

    if args.dotted and args.method:
        neutralize_one(args.decoded, args.dotted, args.method, args.mode, args.quiet)
    elif args.dotted or args.method:
        raise SystemExit("--class and --method must be given together")

    if args.sweep:
        if sweep_putstring(args.decoded, args.quiet) == 0:
            raise SystemExit("sweep wrapped nothing — the pattern or the tree is not what was expected")

    if args.scan_putstring:
        scan_put_string(args.decoded, args.limit)

    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
