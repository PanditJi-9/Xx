#!/usr/bin/env node
/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * SVG → PNG renderer for the design-before-Compose workflow.
 *
 * Why this exists (measured 2026-11-14): the host has no browser, no Inkscape,
 * no ImageMagick and no Cairo; the only working vector rasterizer is
 * `@resvg/resvg-js` (native linux-arm64-gnu binary, verified by require()).
 * The design is therefore authored as plain SVG — real vectors, gradients,
 * masks and filters — and rendered here to a previewable PNG. Compose is not
 * evidence that a design exists; this file produces the evidence.
 *
 * Prerequisite (no account, no key): `npm i @resvg/resvg-js` into any directory,
 * then point DESIGN_NODE_MODULES at its node_modules (default below).
 *
 * Usage:
 *   node tools/design/svg_render.cjs <input.svg> <output.png> [width]
 *
 * Fonts: only files that exist on this host are passed, and system fonts stay
 * enabled as a fallback. Arabic shaping is done by resvg's text engine
 * (rustybuzz), so `direction="rtl"` works without any extra tool.
 */
const fs = require('fs');
const path = require('path');
const os = require('os');

// Search order: explicit env, the durable per-user install, then the scratch
// install used while the pipeline was being proven.
const candidates = [
  process.env.DESIGN_NODE_MODULES,
  path.join(os.homedir(), '.local/share/design-tools/node_modules'),
  path.join(process.cwd(), 'node_modules'),
  '/tmp/opencode/designtest/node_modules',
].filter(Boolean);

let Resvg;
let loadedFrom = null;
for (const root of candidates) {
  try {
    ({ Resvg } = require(path.join(root, '@resvg/resvg-js')));
    loadedFrom = root;
    break;
  } catch (error) {
    // try the next candidate
  }
}
if (!Resvg) {
  console.error('Cannot load @resvg/resvg-js from any of:\n  ' + candidates.join('\n  '));
  console.error('Install it with: npm i @resvg/resvg-js  (then set DESIGN_NODE_MODULES)');
  process.exit(2);
}

const FONT_FILES = [
  '/usr/share/fonts/truetype/lato/Lato-Black.ttf',
  '/usr/share/fonts/truetype/lato/Lato-Bold.ttf',
  '/system/fonts/NotoNaskhArabic-Bold.ttf',
  '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',
].filter((file) => fs.existsSync(file));

const [input, output, widthArg] = process.argv.slice(2);
if (!input || !output) {
  console.error('Usage: node tools/design/svg_render.cjs <input.svg> <output.png> [width]');
  process.exit(1);
}

const svg = fs.readFileSync(input, 'utf8');
const width = Number.parseInt(widthArg || '0', 10);
const options = {
  font: {
    fontFiles: FONT_FILES,
    loadSystemFonts: true,
    defaultFontFamily: 'Lato',
  },
  background: '#050318',
};
if (Number.isFinite(width) && width > 0) options.fitTo = { mode: 'width', value: width };

const started = Date.now();
const png = new Resvg(svg, options).render().asPng();
fs.mkdirSync(path.dirname(path.resolve(output)), { recursive: true });
fs.writeFileSync(output, png);
console.log(`rendered ${output} · ${png.length} bytes · ${Date.now() - started} ms · ${FONT_FILES.length} font files · resvg from ${loadedFrom}`);
