---
description: "Analyse an attached design reference and produce a MaxManager-token specification (positions, spacing, type, colour, layering) with no code changes."
agent: build
subtask: true
---
Analyse the design reference in $ARGUMENTS (image path, URL, or the attachment of this conversation) and produce a specification.

1. Read the image with `python3 tools/read_image_text.py <path|url> --json`; crop with `--crop x0,y0,x1,y1` to inspect regions. What has no text is measured or declared "unread", never guessed.
2. Extract: screen bounds and safe areas · every block's position and size as fractions of width/height · spacing between blocks · type sizes/weights relative to each other · colour roles (background, surface, accent, border) · layering order.
3. **Translate every value into this repository's tokens** (`MaxSpace`, `MaxRadius`, `MaxAlpha`, `MaxTone`, `MaxTypography`). A literal `dp` in the output is a defect: `tools/design_tokens.py --assert` rejects it.
4. State the visual direction in three lines (purpose · dominant element · intended feel), then the component-by-component spec.
5. Never propose copying another product's layout, icons or palette — the reference is information, the design language is MaxManager's (`AGENTS.md` §0.4).
