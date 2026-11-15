---
description: "Audit an existing MaxManager screen without changing code: purpose, hierarchy, edges, states, touch, RTL, accessibility — evidence per finding."
agent: build
subtask: true
---
Audit the screen named in $ARGUMENTS (or the screen of the current conversation) **without editing any file**.

1. Deploy the `ux-audit` agent, and the `ui-architecture` agent when the composable tree has to be mapped first. If subagents are unavailable in this session, perform both roles yourself following their briefs.
2. Read the real code; if the owner attached a screenshot, read it with `python3 tools/read_image_text.py <path|url> --json` — never describe an image you did not read.
3. Report per finding: `path:line` · what happens · what should happen · user impact · severity (high/medium/low). Score purpose, structure, next step, touch, accessibility and honesty 0–2 each.
4. State explicitly what could not be verified here (behaviour, touch, RTL, performance on device) instead of asserting it.
