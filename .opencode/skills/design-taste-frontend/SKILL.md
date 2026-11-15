---
name: design-taste-frontend
description: >-
  Anti-generic visual judgement for any UI, including native Android Compose screens. Use when a
  screen looks templated, glossy, crowded or "AI-generated", or when choosing composition,
  typography scale, colour discipline, visual density, edge alignment and motion restraint.
  MaxManager adaptation: only the aesthetic judgement applies. The upstream skill's browser
  checker, HTML/CSS conventions and landing-page anatomy are not used here — this repository's
  MaxTokens/MaxMotion are the source of truth.
---

# Design taste, adapted for MaxManager

Condensed from **tasteskill 2.5.1** (MIT, © 2026 Leonxlnx — see `LICENSE`). The upstream work is a
landing-page methodology for the web; this file keeps only the judgement that transfers to native
Android, and states where the upstream rules are explicitly *overridden* by this project's identity.
Attribution lives in `THIRD_PARTY_NOTICES.md` and `README.md` → Credits.

## What is being judged

A screen is good when it **says plainly what it is for**, **shows the real thing** (real data, real
states, real device behaviour), and **looks made for this app** rather than assembled from a
template. The failure mode this skill exists to catch is the "generated" look: everything centred,
everything the same weight, decorative gradients where a hierarchy should be, and motion that
decorates instead of explaining.

## House rules that transfer

1. **One system, not a kit of parts.** One button family, one card family, one radius scale
   (`MaxRadius`), one type scale (`MaxTypography`), one link/row look. Two nested boxes at most —
   a card inside a card inside a card is a layout that has not been decided yet.
2. **Equal edges.** Every block starts and ends on the same content edge (`MaxSpace.gutter`). A
   single element out to one side reads as a mistake, not as rhythm.
3. **The screen says its purpose in the first read.** The title names the thing, the primary action
   is a verb and an object, and no more than two primary actions compete on one screen.
4. **Real content, never placeholder theatre.** No invented numbers, reviews, quotes or "sample"
   labels shipped as if they were data. Unknown state is `status_unknown`, drawn as unknown.
5. **Density is a decision, not a default.** Either the screen breathes (few elements, large type,
   generous `MaxSpace.section`) or it is deliberately dense (a control panel: tight rows,
   `MaxSpace.row`, tabular numbers). The failure is half-and-half — airy containers holding crowded
   content.
6. **Colour carries meaning, never decoration.** `MaxTone` already encodes intent; a tone wash
   (`MaxAlpha.toneWash`) is a state, not a background. Two accent hues on one screen is already one
   too many.
7. **Motion explains a change of state** (appear, select, confirm, progress). One dealt moment per
   screen at most. Anything longer than `MaxDuration.deliberate` is decoration and is not allowed.
8. **Icons are drawn for this app or are the platform's own; never a hand-drawn imitation of a real
   product's logo.**
9. **The eye check:** nothing tilted, staggered, clipped or overlapping; no screen whose only idea is
   a gradient.

## Where the upstream rules are overridden here

- **"No violet or lavender in the page's own design"** — this is a web house style and does **not**
  apply. The Game Space identity *is* violet (`GameSpacePalette.Violet`, `VioletSoft`, `Lavender`).
  The upstream rule is recorded only so it is never applied by accident.
- **"One section per screen, hero alone on the first screen"** — landing-page anatomy. A settings
  screen or a control panel is allowed to be dense from the first pixel.
- **"Every visual renders at 1440/1024/768/390"** — web breakpoints. Native equivalent: verify at the
  phone's real width, in portrait and landscape, and at the largest system font scale.
- **Hover micro-animations** — there is no pointer. The native equivalents are press, long-press,
  focus and selection state, and they must be visible without motion (`MaxTone` + alpha), because
  reduced-motion users get the static state only.

## Checklist for a MaxManager screen

Each line is answerable from the code and from one screenshot; none of it is a matter of taste.

- **Purpose:** the title names the thing; the primary action is one verb+object; at most two
  competing actions; no action repeated twice on the same screen.
- **Edges:** every block on `MaxSpace.gutter`; grid gutters from `MaxSectionSpec`; nothing spills; a
  split row never leaves one half empty.
- **Hierarchy:** exactly one dominant element per screen; supporting text at
  `MaxAlpha.supportingText`; disabled content at `MaxAlpha.disabledContent` — not a lighter font.
- **Cards:** one `MaxCard` contract (`MaxSectionSpec`) everywhere; radius from `MaxRadius` by role
  (row 14 · group 22 · sheet 28 · tile 18 · inset 16 · chip 10 · deck 20); pills for **state only**,
  never for a primary action.
- **Type:** one heading scale, no invented sizes outside `MaxTypography`; numbers that change are
  tabular; a heading's grey half is its own sentence, not a run-on.
- **Motion:** `MaxMotion.enter()/exit()` for appearance, `MaxDuration.quick/standard` for state,
  springs for direct manipulation; a loop that runs past `MaxDuration.deliberate` has a reason and a
  pause; under `ANIMATOR_DURATION_SCALE = 0` everything rests and nothing stays hidden.
- **States:** empty, loading, error, disabled and unknown each have a designed appearance — not a
  spinner in the middle of a blank card. An empty screen says what would fill it and how.
- **Touch:** every control at `MaxSize.minTouchTarget` or larger; nothing inside the system gesture
  insets; the bottom bar reserve (`MaxSpace.bottomBarReserve`) respected.
- **Copy:** no slop words, no stock phrases, no two-fragment headline tics; one fact, one wording;
  every label in `values/` and `values-ar/` — English and Arabic only.
- **Performance as taste:** no fade-in on every element, no parallax on text, no infinite animation
  that the user did not start; images decoded at the size they are drawn.

## How to use it

1. Read the screen's code and one real screenshot before judging anything.
2. Name the failure concretely ("three radii on one screen", "the primary action is a pill"),
   not as a mood ("feels off").
3. Fix the highest-impact item first, re-screenshot, and only then move to the next.
4. Never claim a screen "matches a reference" without a measured comparison: element positions,
   sizes, spacing, type scale, colour values. `docs/design/VISUAL_QA.md` defines the procedure.
5. This skill judges; it does not license copying. Layout, iconography and palette come from this
   repository's design language, never from a reference app (rule §0.4 of `AGENTS.md`).
