#!/usr/bin/env node
/*
 * معاينة أيقونات الرموز (MaxIllustration) خارج Compose — أداة تطوير فقط.
 *
 * لماذا: تصميم الأيقونات قبل كتابة Kotlin («Compose ليس دليلًا على وجود تصميم»)،
 * والمعاينة هنا تعكس المُصيّر الحقيقي: نفس الحبر الثلاث (Accent/Outline/Wash)،
 * ونفس قواعد التعبئة (Wash المملوء = لون الحاوية القويّ)، ونفس سماكة الخط
 * المقترحة (نسبة من ضلع الأيقونة لا dp ثابت)، وبنهايات/وصلات دائرية.
 *
 * الاستخدام:
 *   node tools/design/icon_preview.cjs [--out dir] [--cell 240] [--scale 0.055]
 * يكتب SVG وPNG لكل عائلة: ورقة كل الرموز (contact sheet).
 *
 * الرسم بلا أي اعتماد خارجي؛ العرض عبر tools/design/svg_render.cjs (resvg).
 */
'use strict';

const fs = require('fs');
const path = require('path');

// ── الألوان (تقريب سمات داكنة، والبذرة 0xFF607D8F تدرّجها الأساسي الفاتح) ──
const ACCENT = '#A9C2D4';          // Accent
const OUTLINE = '#C9D1D9';         // onSurfaceVariant (يُضرب بألفا 0.80)
const CARD = '#1D232A';            // خلفية بطاقة نموذجية
const BG = '#111418';

// ── DSL مطابق لِـMaxIllustrationModel ──
// line(pts..., {ink, closed, filled}) | box(x,y,w,h,{ink,filled,r}) | ring(x,y,r,{ink,filled})
// والإحداثيات كلها في المربع الواحد 0..1.

function TAU(a0, a1, n) { // عيّنات قوس
  const pts = [];
  for (let i = 0; i <= n; i++) {
    const a = a0 + (a1 - a0) * (i / n);
    pts.push(a);
  }
  return pts;
}
function arcPts(cx, cy, r, a0, a1, n = 10) {
  const out = [];
  for (const a of TAU(a0, a1, n)) out.push(cx + r * Math.cos(a), cy + r * Math.sin(a));
  return out;
}

/** كل رمز: قائمة أجزاء. الإطار البصري 0.08..0.92 تقريبًا في الجميع. */
const KINDS = {
  Device: [
    { t: 'box', x: 0.22, y: 0.08, w: 0.56, h: 0.84, r: 0.12, ink: 'Wash', filled: true },
    { t: 'box', x: 0.22, y: 0.08, w: 0.56, h: 0.84, r: 0.12, ink: 'Outline' },
    { t: 'box', x: 0.31, y: 0.19, w: 0.38, h: 0.52, r: 0.06, ink: 'Accent', filled: true },
    { t: 'line', pts: [0.43, 0.80, 0.57, 0.80], ink: 'Outline' },
  ],
  Cpu: [
    { t: 'box', x: 0.20, y: 0.20, w: 0.60, h: 0.60, r: 0.12, ink: 'Wash', filled: true },
    { t: 'box', x: 0.20, y: 0.20, w: 0.60, h: 0.60, r: 0.12, ink: 'Outline' },
    { t: 'box', x: 0.33, y: 0.33, w: 0.34, h: 0.34, r: 0.08, ink: 'Accent', filled: true },
    ...[0, 1, 2].flatMap(i => {
      const p = 0.30 + i * 0.20;
      return [
        { t: 'line', pts: [p, 0.09, p, 0.19], ink: 'Outline' },
        { t: 'line', pts: [p, 0.81, p, 0.91], ink: 'Outline' },
        { t: 'line', pts: [0.09, p, 0.19, p], ink: 'Outline' },
        { t: 'line', pts: [0.81, p, 0.91, p], ink: 'Outline' },
      ];
    }),
  ],
  Gpu: [
    { t: 'box', x: 0.08, y: 0.26, w: 0.84, h: 0.48, r: 0.10, ink: 'Wash', filled: true },
    { t: 'box', x: 0.08, y: 0.26, w: 0.84, h: 0.48, r: 0.10, ink: 'Outline' },
    { t: 'ring', x: 0.38, y: 0.50, r: 0.19, ink: 'Accent' },
    { t: 'ring', x: 0.38, y: 0.50, r: 0.06, ink: 'Accent', filled: true },
    ...[90, 210, 330].map(deg => {
      const a = deg * Math.PI / 180;
      return { t: 'line', pts: [0.38 + 0.075 * Math.cos(a), 0.50 + 0.075 * Math.sin(a), 0.38 + 0.165 * Math.cos(a), 0.50 + 0.165 * Math.sin(a)], ink: 'Accent' };
    }),
    ...[0, 1, 2].map(i => ({ t: 'line', pts: [0.64 + i * 0.10, 0.76, 0.64 + i * 0.10, 0.90], ink: 'Outline' })),
  ],
  Memory: [
    { t: 'box', x: 0.10, y: 0.28, w: 0.80, h: 0.42, r: 0.08, ink: 'Wash', filled: true },
    { t: 'box', x: 0.10, y: 0.28, w: 0.80, h: 0.42, r: 0.08, ink: 'Outline' },
    { t: 'box', x: 0.17, y: 0.35, w: 0.18, h: 0.20, r: 0.03, ink: 'Accent', filled: true },
    { t: 'box', x: 0.41, y: 0.35, w: 0.18, h: 0.20, r: 0.03, ink: 'Accent', filled: true },
    { t: 'box', x: 0.65, y: 0.35, w: 0.18, h: 0.20, r: 0.03, ink: 'Accent', filled: true },
    ...[0, 1, 2, 3, 4, 5].map(i => ({ t: 'line', pts: [0.15 + i * 0.125, 0.72, 0.15 + i * 0.125, 0.86], ink: 'Outline' })),
  ],
  Storage: [
    { t: 'box', x: 0.10, y: 0.20, w: 0.80, h: 0.60, r: 0.10, ink: 'Wash', filled: true },
    { t: 'box', x: 0.10, y: 0.20, w: 0.80, h: 0.60, r: 0.10, ink: 'Outline' },
    { t: 'ring', x: 0.42, y: 0.50, r: 0.20, ink: 'Accent' },
    { t: 'ring', x: 0.42, y: 0.50, r: 0.06, ink: 'Accent', filled: true },
  ],
  Display: [
    { t: 'box', x: 0.10, y: 0.14, w: 0.80, h: 0.58, r: 0.08, ink: 'Wash', filled: true },
    { t: 'box', x: 0.10, y: 0.14, w: 0.80, h: 0.58, r: 0.08, ink: 'Outline' },
    { t: 'box', x: 0.20, y: 0.24, w: 0.60, h: 0.38, r: 0.05, ink: 'Accent', filled: true },
    { t: 'line', pts: [0.50, 0.72, 0.50, 0.82], ink: 'Outline' },
    { t: 'box', x: 0.34, y: 0.84, w: 0.32, h: 0.06, r: 0.03, ink: 'Outline', filled: true },
  ],
  Power: [
    { t: 'box', x: 0.08, y: 0.36, w: 0.74, h: 0.36, r: 0.08, ink: 'Wash', filled: true },
    { t: 'box', x: 0.08, y: 0.36, w: 0.74, h: 0.36, r: 0.08, ink: 'Outline' },
    { t: 'box', x: 0.84, y: 0.46, w: 0.06, h: 0.16, r: 0.02, ink: 'Outline' },
    { t: 'line', pts: [0.56, 0.10, 0.30, 0.52, 0.46, 0.52, 0.34, 0.90, 0.62, 0.44, 0.48, 0.44], closed: true, ink: 'Accent', filled: true },
  ],
  Thermal: [
    { t: 'box', x: 0.42, y: 0.10, w: 0.16, h: 0.54, r: 0.08, ink: 'Outline' },
    { t: 'box', x: 0.46, y: 0.26, w: 0.08, h: 0.40, r: 0.04, ink: 'Accent', filled: true },
    { t: 'ring', x: 0.50, y: 0.72, r: 0.15, ink: 'Wash', filled: true },
    { t: 'ring', x: 0.50, y: 0.72, r: 0.15, ink: 'Accent' },
    { t: 'ring', x: 0.50, y: 0.72, r: 0.055, ink: 'Accent', filled: true },
    ...[0, 1, 2].map(i => ({ t: 'line', pts: [0.68, 0.18 + i * 0.14, 0.80, 0.18 + i * 0.14], ink: 'Outline' })),
  ],
  Network: [
    { t: 'line', pts: arcPts(0.50, 0.74, 0.18, -Math.PI * 3 / 4, -Math.PI / 4, 8), ink: 'Accent' },
    { t: 'line', pts: arcPts(0.50, 0.74, 0.32, -Math.PI * 3 / 4, -Math.PI / 4, 9), ink: 'Outline' },
    { t: 'line', pts: arcPts(0.50, 0.74, 0.46, -Math.PI * 3 / 4, -Math.PI / 4, 10), ink: 'Outline' },
    { t: 'ring', x: 0.50, y: 0.74, r: 0.05, ink: 'Accent', filled: true },
  ],
  Audio: [
    { t: 'line', pts: [0.14, 0.40, 0.30, 0.40, 0.50, 0.20, 0.50, 0.80, 0.30, 0.60, 0.14, 0.60], closed: true, ink: 'Accent', filled: true },
    { t: 'line', pts: arcPts(0.52, 0.50, 0.20, -Math.PI * 0.28, Math.PI * 0.28, 7), ink: 'Outline' },
    { t: 'line', pts: arcPts(0.52, 0.50, 0.34, -Math.PI * 0.28, Math.PI * 0.28, 8), ink: 'Outline' },
  ],
  Games: [
    { t: 'box', x: 0.08, y: 0.30, w: 0.84, h: 0.44, r: 0.22, ink: 'Wash', filled: true },
    { t: 'box', x: 0.08, y: 0.30, w: 0.84, h: 0.44, r: 0.22, ink: 'Outline' },
    { t: 'box', x: 0.21, y: 0.40, w: 0.07, h: 0.24, r: 0.03, ink: 'Accent', filled: true },
    { t: 'box', x: 0.125, y: 0.485, w: 0.24, h: 0.07, r: 0.03, ink: 'Accent', filled: true },
    { t: 'ring', x: 0.66, y: 0.45, r: 0.05, ink: 'Accent', filled: true },
    { t: 'ring', x: 0.79, y: 0.58, r: 0.05, ink: 'Accent' },
  ],
  Apps: [
    { t: 'box', x: 0.12, y: 0.12, w: 0.34, h: 0.34, r: 0.10, ink: 'Accent', filled: true },
    { t: 'box', x: 0.54, y: 0.12, w: 0.34, h: 0.34, r: 0.10, ink: 'Outline' },
    { t: 'box', x: 0.12, y: 0.54, w: 0.34, h: 0.34, r: 0.10, ink: 'Outline' },
    { t: 'box', x: 0.54, y: 0.54, w: 0.34, h: 0.34, r: 0.10, ink: 'Outline' },
  ],
  System: [
    { t: 'ring', x: 0.50, y: 0.50, r: 0.31, ink: 'Outline' },
    ...Array.from({ length: 8 }, (_, i) => {
      const a = Math.PI / 4 * i;
      return { t: 'line', pts: [0.50 + 0.31 * Math.cos(a), 0.50 + 0.31 * Math.sin(a), 0.50 + 0.43 * Math.cos(a), 0.50 + 0.43 * Math.sin(a)], ink: 'Outline' };
    }),
    { t: 'ring', x: 0.50, y: 0.50, r: 0.12, ink: 'Accent', filled: true },
  ],
  Security: [
    { t: 'line', pts: [0.50, 0.08, 0.84, 0.20, 0.80, 0.56, 0.50, 0.90, 0.20, 0.56, 0.16, 0.20], closed: true, ink: 'Wash', filled: true },
    { t: 'line', pts: [0.50, 0.08, 0.84, 0.20, 0.80, 0.56, 0.50, 0.90, 0.20, 0.56, 0.16, 0.20], closed: true, ink: 'Outline' },
    { t: 'ring', x: 0.50, y: 0.40, r: 0.09, ink: 'Accent' },
    { t: 'line', pts: [0.50, 0.49, 0.50, 0.64], ink: 'Accent' },
  ],
  Controls: [
    ...[0, 1, 2].map(i => {
      const y = 0.22 + i * 0.28;
      const x = [0.32, 0.68, 0.42][i];
      return [
        { t: 'line', pts: [0.12, y, 0.88, y], ink: 'Outline' },
        { t: 'ring', x, y, r: 0.085, ink: 'Accent', filled: true },
      ];
    }).flat(),
  ],
  Tools: [
    { t: 'box', x: 0.38, y: 0.08, w: 0.24, h: 0.32, r: 0.08, ink: 'Accent', filled: true },
    { t: 'box', x: 0.38, y: 0.08, w: 0.24, h: 0.32, r: 0.08, ink: 'Outline' },
    { t: 'line', pts: [0.50, 0.42, 0.50, 0.76], ink: 'Accent' },
    { t: 'line', pts: [0.42, 0.76, 0.58, 0.76], ink: 'Accent' },
  ],
  Camera: [
    { t: 'line', pts: [0.30, 0.30, 0.36, 0.20, 0.64, 0.20, 0.70, 0.30], ink: 'Outline' },
    { t: 'box', x: 0.08, y: 0.30, w: 0.84, h: 0.52, r: 0.10, ink: 'Wash', filled: true },
    { t: 'box', x: 0.08, y: 0.30, w: 0.84, h: 0.52, r: 0.10, ink: 'Outline' },
    { t: 'ring', x: 0.50, y: 0.56, r: 0.16, ink: 'Accent' },
    { t: 'ring', x: 0.50, y: 0.56, r: 0.07, ink: 'Accent', filled: true },
  ],
  Sensors: [
    { t: 'ring', x: 0.50, y: 0.50, r: 0.09, ink: 'Accent', filled: true },
    { t: 'line', pts: arcPts(0.50, 0.50, 0.22, Math.PI * 0.75, Math.PI * 1.25, 7), ink: 'Outline' },
    { t: 'line', pts: arcPts(0.50, 0.50, 0.36, Math.PI * 0.75, Math.PI * 1.25, 8), ink: 'Outline' },
    { t: 'line', pts: arcPts(0.50, 0.50, 0.22, -Math.PI * 0.25, Math.PI * 0.25, 7), ink: 'Outline' },
    { t: 'line', pts: arcPts(0.50, 0.50, 0.36, -Math.PI * 0.25, Math.PI * 0.25, 8), ink: 'Outline' },
  ],
  Intelligence: [
    { t: 'line', pts: [0.50, 0.10, 0.90, 0.50, 0.50, 0.90, 0.10, 0.50], closed: true, ink: 'Wash', filled: true },
    { t: 'line', pts: [0.50, 0.10, 0.90, 0.50, 0.50, 0.90, 0.10, 0.50], closed: true, ink: 'Outline' },
    { t: 'ring', x: 0.50, y: 0.50, r: 0.14, ink: 'Accent' },
    { t: 'ring', x: 0.50, y: 0.50, r: 0.05, ink: 'Accent', filled: true },
  ],
  Island: [
    { t: 'box', x: 0.10, y: 0.38, w: 0.80, h: 0.24, r: 0.12, ink: 'Wash', filled: true },
    { t: 'box', x: 0.10, y: 0.38, w: 0.80, h: 0.24, r: 0.12, ink: 'Outline' },
    { t: 'ring', x: 0.24, y: 0.50, r: 0.06, ink: 'Accent', filled: true },
    { t: 'line', pts: [0.42, 0.50, 0.66, 0.50], ink: 'Outline' },
  ],
  Overlay: [
    { t: 'box', x: 0.10, y: 0.12, w: 0.80, h: 0.62, r: 0.08, ink: 'Wash', filled: true },
    { t: 'box', x: 0.10, y: 0.12, w: 0.80, h: 0.62, r: 0.08, ink: 'Outline' },
    { t: 'box', x: 0.46, y: 0.48, w: 0.38, h: 0.30, r: 0.06, ink: 'Accent', filled: true },
    { t: 'box', x: 0.46, y: 0.48, w: 0.38, h: 0.30, r: 0.06, ink: 'Outline' },
  ],
  Clips: [
    { t: 'box', x: 0.10, y: 0.18, w: 0.80, h: 0.64, r: 0.08, ink: 'Wash', filled: true },
    { t: 'box', x: 0.10, y: 0.18, w: 0.80, h: 0.64, r: 0.08, ink: 'Outline' },
    ...[0, 1, 2].flatMap(i => [
      { t: 'box', x: 0.15, y: 0.26 + i * 0.19, w: 0.07, h: 0.09, r: 0.02, ink: 'Outline' },
      { t: 'box', x: 0.78, y: 0.26 + i * 0.19, w: 0.07, h: 0.09, r: 0.02, ink: 'Outline' },
    ]),
    { t: 'line', pts: [0.42, 0.36, 0.42, 0.64, 0.68, 0.50], closed: true, ink: 'Accent', filled: true },
  ],
  Sessions: [
    { t: 'ring', x: 0.50, y: 0.52, r: 0.30, ink: 'Wash', filled: true },
    { t: 'ring', x: 0.50, y: 0.52, r: 0.30, ink: 'Outline' },
    { t: 'line', pts: [0.50, 0.52, 0.50, 0.34], ink: 'Accent' },
    { t: 'line', pts: [0.50, 0.52, 0.66, 0.60], ink: 'Accent' },
    { t: 'line', pts: arcPts(0.50, 0.52, 0.40, Math.PI * 1.11, Math.PI * 1.56, 6), ink: 'Outline' },
    { t: 'line', pts: [0.13, 0.39, 0.13, 0.53], ink: 'Outline' },
    { t: 'line', pts: [0.13, 0.39, 0.26, 0.35], ink: 'Outline' },
  ],
  Manifest: [
    { t: 'line', pts: [0.26, 0.10, 0.62, 0.10, 0.76, 0.24, 0.76, 0.90, 0.26, 0.90], closed: true, ink: 'Wash', filled: true },
    { t: 'line', pts: [0.26, 0.10, 0.62, 0.10, 0.76, 0.24, 0.76, 0.90, 0.26, 0.90], closed: true, ink: 'Outline' },
    { t: 'line', pts: [0.62, 0.10, 0.62, 0.24, 0.76, 0.24], ink: 'Outline' },
    { t: 'line', pts: [0.34, 0.42, 0.68, 0.42], ink: 'Accent' },
    { t: 'line', pts: [0.34, 0.54, 0.68, 0.54], ink: 'Accent' },
    { t: 'line', pts: [0.34, 0.66, 0.56, 0.66], ink: 'Accent' },
  ],
  Scan: [
    { t: 'ring', x: 0.50, y: 0.50, r: 0.36, ink: 'Outline' },
    { t: 'line', pts: arcPts(0.50, 0.50, 0.36, -Math.PI / 2, -Math.PI / 6, 6), ink: 'Accent' },
    { t: 'line', pts: arcPts(0.50, 0.50, 0.22, -Math.PI / 2, -Math.PI / 6, 5), ink: 'Accent' },
    { t: 'ring', x: 0.50, y: 0.50, r: 0.05, ink: 'Accent', filled: true },
    { t: 'ring', x: 0.64, y: 0.32, r: 0.035, ink: 'Accent', filled: true },
    { t: 'ring', x: 0.34, y: 0.66, r: 0.035, ink: 'Outline' },
  ],
  Usage: [
    { t: 'line', pts: [0.12, 0.84, 0.88, 0.84], ink: 'Outline' },
    { t: 'box', x: 0.20, y: 0.56, w: 0.14, h: 0.28, r: 0.02, ink: 'Accent', filled: true },
    { t: 'box', x: 0.43, y: 0.40, w: 0.14, h: 0.44, r: 0.02, ink: 'Accent', filled: true },
    { t: 'box', x: 0.66, y: 0.22, w: 0.14, h: 0.62, r: 0.02, ink: 'Accent', filled: true },
  ],
  BatteryHistory: [
    { t: 'box', x: 0.08, y: 0.36, w: 0.74, h: 0.36, r: 0.08, ink: 'Wash', filled: true },
    { t: 'box', x: 0.08, y: 0.36, w: 0.74, h: 0.36, r: 0.08, ink: 'Outline' },
    { t: 'box', x: 0.84, y: 0.46, w: 0.06, h: 0.16, r: 0.02, ink: 'Outline' },
    { t: 'line', pts: [0.16, 0.54, 0.32, 0.54, 0.40, 0.42, 0.50, 0.64, 0.58, 0.54, 0.74, 0.54], ink: 'Accent' },
  ],
};

// ── SVG emit ──
function esc(s) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;'); }

function partSvg(p, s, sw, ox, oy) {
  const col = {
    Accent: ACCENT,
    Outline: OUTLINE,
    Wash: ACCENT,
  }[p.ink];
  const strong = p.filled && p.ink === 'Wash';
  const fillAlpha = p.filled ? (p.ink === 'Wash' ? 0.18 : 1) : 0;
  const strokeAlpha = p.filled ? 0 : (p.ink === 'Outline' ? 0.80 : 1);
  const op = a => `opacity="${a}"`;
  if (p.t === 'line') {
    const pts = [];
    for (let i = 0; i < p.pts.length; i += 2) pts.push(`${(ox + p.pts[i] * s).toFixed(2)},${(oy + p.pts[i + 1] * s).toFixed(2)}`);
    const d = `M${pts.join('L')}${p.closed ? 'Z' : ''}`;
    if (p.filled) return `<path d="${d}" fill="${col}" ${op(fillAlpha)} />`;
    return `<path d="${d}" fill="none" stroke="${col}" stroke-opacity="${strokeAlpha}" stroke-width="${sw}" stroke-linecap="round" stroke-linejoin="round" />`;
  }
  if (p.t === 'box') {
    const x = ox + p.x * s, y = oy + p.y * s, w = p.w * s, h = p.h * s, r = p.r * s;
    if (p.filled) return `<rect x="${x.toFixed(2)}" y="${y.toFixed(2)}" width="${w.toFixed(2)}" height="${h.toFixed(2)}" rx="${r.toFixed(2)}" fill="${col}" ${op(fillAlpha)} />`;
    return `<rect x="${x.toFixed(2)}" y="${y.toFixed(2)}" width="${w.toFixed(2)}" height="${h.toFixed(2)}" rx="${r.toFixed(2)}" fill="none" stroke="${col}" stroke-opacity="${strokeAlpha}" stroke-width="${sw}" stroke-linejoin="round" />`;
  }
  // ring
  const cx = ox + p.x * s, cy = oy + p.y * s, r = p.r * s;
  if (p.filled) return `<circle cx="${cx.toFixed(2)}" cy="${cy.toFixed(2)}" r="${r.toFixed(2)}" fill="${col}" ${op(fillAlpha)} />`;
  return `<circle cx="${cx.toFixed(2)}" cy="${cy.toFixed(2)}" r="${r.toFixed(2)}" fill="none" stroke="${col}" stroke-opacity="${strokeAlpha}" stroke-width="${sw}" />`;
}

function cellSvg(kind, icon, size, pad) {
  const s = size, sw = SCALE * s;
  const body = icon.map(p => partSvg(p, s, sw, 0, 0)).join('');
  return { size, svg: `<g>${body}</g>` };
}

let SCALE = 0.055;

function sheet(cell, cols, outPath, labels) {
  const kinds = Object.keys(KINDS);
  const rows = Math.ceil(kinds.length / cols);
  const pad = Math.round(cell * 0.28);
  const W = cols * cell + (cols + 1) * pad;
  const H = rows * (cell + pad + 18) + pad;
  let body = `<rect width="${W}" height="${H}" fill="${BG}" />`;
  kinds.forEach((kind, i) => {
    const c = i % cols, r = Math.floor(i / cols);
    const x = pad + c * (cell + pad);
    const y = pad + r * (cell + pad + 18);
    body += `<rect x="${x}" y="${y}" width="${cell}" height="${cell}" rx="${Math.round(cell * 0.12)}" fill="${CARD}" />`;
    const inner = cell * 0.62;
    const { svg } = cellSvg(kind, KINDS[kind], inner, 0);
    const off = (cell - inner) / 2;
    body += `<g transform="translate(${x + off},${y + off})">${svg}</g>`;
    if (labels) body += `<text x="${x + cell / 2}" y="${y + cell + 14}" font-family="sans-serif" font-size="11" fill="#8A939B" text-anchor="middle">${esc(kind)}</text>`;
  });
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">${body}</svg>`;
  fs.writeFileSync(outPath, svg);
  return outPath;
}

function main() {
  const argv = process.argv.slice(2);
  const opt = k => { const i = argv.indexOf(k); return i >= 0 ? argv[i + 1] : null; };
  if (argv.includes('--kotlin')) { kotlin(); return; }
  const out = opt('--out') || '/tmp/opencode/icon_preview';
  const cell = Number(opt('--cell') || 150);
  const cols = Number(opt('--cols') || 6);
  const sc = Number(opt('--scale') || 0.055);
  SCALE = sc;
  fs.mkdirSync(out, { recursive: true });
  const p1 = sheet(cell, cols, path.join(out, `sheet_${cell}.svg`), true);
  console.log(p1);
  console.log(JSON.stringify({ cell, scale: sc, kinds: Object.keys(KINDS).length }));
}

// ── توليد Kotlin: نفس KINDS حرفيًّا، بلا إعادة كتابة يدوية ⇒ لا انحراف ──
const f = v => `${Number(v.toFixed(3))}f`;

function kotlinLine(p) {
  const coords = [];
  for (let i = 0; i < p.pts.length; i += 2) coords.push(`${f(p.pts[i])}, ${f(p.pts[i + 1])}`);
  const args = [coords.join(', ')];
  if (p.ink === 'Wash') args.push('ink = wash');
  else if (p.ink === 'Outline') args.push('ink = outline');
  if (p.closed) args.push('closed = true');
  if (p.filled) args.push('filled = true');
  return `            line(${args.join(', ')})`;
}
function kotlinBox(p) {
  const args = [`${f(p.x)}, ${f(p.y)}, ${f(p.w)}, ${f(p.h)}`];
  if (p.ink === 'Wash') args.push('wash');
  else if (p.ink === 'Outline') args.push('outline');
  if (p.filled) args.push('filled = true');
  args.push(`radius = ${f(p.r)}`);
  return `            box(${args.join(', ')})`;
}
function kotlinRing(p) {
  const args = [`${f(p.x)}, ${f(p.y)}, ${f(p.r)}`];
  if (p.ink === 'Wash') args.push('wash');
  else if (p.ink === 'Outline') args.push('outline');
  if (p.filled) args.push('filled = true');
  return `            ring(${args.join(', ')})`;
}

function kotlin() {
  const out = [];
  for (const [kind, parts] of Object.entries(KINDS)) {
    out.push(`        MaxIllustrationKind.${kind} -> {`);
    for (const p of parts) {
      out.push(p.t === 'line' ? kotlinLine(p) : p.t === 'box' ? kotlinBox(p) : kotlinRing(p));
    }
    out.push('        }');
  }
  console.log(out.join('\n'));
}

main();
