# GameSpace — ported source

These files are ours: they are tracked by git and edited here, not regenerated.
They were taken from the original APK (decompiled with jadx, package rewritten once by
`tools/port_materials.py`) and then sealed by `tools/seal_ported.py` **only after the
compiler accepted them** — the pruning loop drops every unit javac refuses.

- sealed units: **591**
- sealed on: 2026-11-22
- corpus at the time: 6280 units
- rejected then: 5542 (library code 5217 · needs a dependency 11 · vendor framework 7)

`SEALED.json` lists the paths so a later port run stages only what is still missing.
