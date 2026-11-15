---
description: "Produce a complete implementable blueprint for a screen: composable tree, state ownership, interactions, states, motion, strings — approved before any code is written."
agent: build
subtask: true
---
Produce the implementation blueprint for $ARGUMENTS before any code is written.

1. Map what exists with the `ui-architecture` agent (or inline following its brief): the composable tree, the reusable components, the single owner of each piece of state.
2. Take the visual values from an approved specification or from `docs/design/DESIGN_SYSTEM.md`; every value is a token, never a literal.
3. Specify: component tree · state ownership and inputs · interactions and gestures · every state (empty/loading/error/disabled/unknown) · motion from `MaxMotion` with durations from `MaxDuration` · the exact new string keys with their English and Arabic text.
4. Name what is **not** being built and why, and any token the design needs that does not exist yet (with the proposed addition).
5. End with the acceptance criteria a reviewer can check without asking a question. No file is edited in this step.
