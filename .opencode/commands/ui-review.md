---
description: "Audit visual quality, accessibility and usability of a finished screen and rank the remaining differences by impact."
agent: build
subtask: true
---
Review the finished screen $ARGUMENTS on three axes and rank what remains.

1. **Visual**: one system (radius/type/button/card families) · equal content edges on `MaxSpace.gutter` · one dominant element · density decision · colour carrying meaning through `MaxTone` · no decorative gradient or glow where a hierarchy is missing.
2. **Accessibility**: label on every control · `contentDescription` where there is no text · logical reading order · contrast of supporting text · colour never the only carrier of meaning · touch targets at `MaxSize.minTouchTarget` · nothing inside system insets or under `MaxSpace.bottomBarReserve`.
3. **Usability**: the screen's purpose readable from the title · one primary action (verb+object) · a way back · no dead end · every state designed (empty/loading/error/disabled/unknown).
4. Rank the remaining differences by user impact and give the highest-impact fix first. Use the `design-taste-frontend` skill for the anti-generic judgement and `ux-audit` for the usability pass.
5. No file is edited. Every claim carries `path:line` or a measured number.
