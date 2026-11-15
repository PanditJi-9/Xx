---
description: "Check that a UI change did not regress existing behaviour, other screens, resources or the contract gates."
agent: build
subtask: true
---
Check for regressions after the change in $ARGUMENTS.

1. Scope the diff: `git diff --stat origin/main...HEAD` and `git diff origin/main...HEAD -- <paths>`. List every file the change touched and every shared file it could have disturbed (tokens, theme, components, navigation, strings, manifest).
2. Reuse check: any component whose signature or default changed — find all its call sites and confirm each still behaves as before (`grep` for the name, not for guesses).
3. Resources: every new key in `values/` exists in `values-ar/`; no orphaned key lost its only consumer; no asset added outside the licence rules (`THIRD_PARTY_NOTICES.md`).
4. Run the contract gates and paste the result lines: `python3 tools/kt_balance.py --assert` · `python3 tools/code_health.py --assert` · `python3 tools/i18n_coverage.py --assert` · `python3 tools/i18n_coverage.py --prune all --assert` · `python3 tools/design_tokens.py --assert` · `python3 tools/resource_compile.py --assert` · `git diff --check`.
5. State the build verdict only if it was read from GitHub Actions (`gh run view <id>`); a local build is not permitted (§0.10). Behaviour, touch and RTL on a device remain unverified here unless actually observed.
