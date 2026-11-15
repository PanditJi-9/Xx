---
description: "Implement an approved MaxManager screen blueprint in native Compose, reusing existing components and tokens, then run the light contract gates."
agent: build
subtask: true
---
Implement the approved blueprint for $ARGUMENTS using the `android-engineering` agent (or inline following its brief).

1. Refuse to start if there is no approved blueprint: produce one with `/ui-blueprint` first. Reuse existing components before creating any (§0.5); if a new component is unavoidable, say what was searched and not found.
2. Kotlin + Jetpack Compose only. No new Gradle dependency. New strings go to `values/` and `values-ar/` in the same edit.
3. Preserve existing behaviour and state ownership; no sysfs/root writes from `ui/**`; unknown values stay `status_unknown`.
4. Before reporting, run and paste the exact result lines:
   `python3 tools/kt_balance.py --assert` · `python3 tools/code_health.py --assert` · `python3 tools/i18n_coverage.py --assert` · `python3 tools/design_tokens.py --assert` · `git diff --check`
5. Never build locally (§0.10) and never commit or push. Declare anything unverified in this environment as unverified — compiling is not proof the screen is right.
