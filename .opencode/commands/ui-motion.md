---
description: "Design and implement advanced but cheap motion for a MaxManager screen using MaxMotion/MaxDuration only, with reduced-motion behaviour and cost check."
agent: build
subtask: true
---
Design the motion for $ARGUMENTS with the `motion-design` agent (or inline following its brief).

1. Produce the state→state table: trigger · duration (`MaxDuration` or `MaxMotion`) · easing or spring · animated property (transform/alpha only) · what stays still.
2. Say what was rejected and why (parallax on text, fade-in on every element, endless rotation, library additions).
3. Under `ANIMATOR_DURATION_SCALE = 0` everything must rest and nothing may stay hidden.
4. Implement only after the table is approved, reusing `MaxMotion.enter()/exit()` and the existing springs. No new animation dependency.
5. Check the cost: no allocation inside `onFrame`/`update`, animation scope limited to the animating composable, no `animateContentSize` on long lists.
6. Never claim smoothness without a device measurement — say "not measured on device" when true.
