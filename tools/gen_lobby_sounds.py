#!/usr/bin/env python3
"""يولّد أصوات لوبي الألعاب (res/raw/lobby_*.ogg) تركيبًا بالكود — لا عيّنات منقولة.

لماذا سكربت لا ملفات جاهزة: المشروع مغلق المصدر ولا يقبل أصولًا بترخيص مجهول
(AGENTS.md §0.4)، فكل صوت هنا **موجة مُركَّبة من أرقام** (جيوب وضجيج مُرشَّح)
بذرة عشوائية ثابتة، فيُعاد توليد الملفات نفسها بايتًا ببايت ويمكن تعديل أي صوت
بتغيير سطر. الناتج OGG/Vorbis أحاديّ ٤٤٫١ كيلوهرتز.

توقيت صوت الدخول (lobby_intro) مربوط بـ `LobbyEntranceTimeline` في الكود:
    IMPACT_MS = 2100   لحظة الوميض (ذروة الباص + الضربة)
    TOTAL_MS  = 3400   نهاية الذيل
أي تغيير هنا يلزمه تغيير الثابتين في `core/gamespace/LobbyEntranceTimeline.kt`.

الاستعمال:
    python3 tools/gen_lobby_sounds.py            # يكتب في manager/app/src/main/res/raw
    python3 tools/gen_lobby_sounds.py --out /tmp/x --wav   # WAV للفحص بالأذن
    python3 tools/gen_lobby_sounds.py --check    # يتحقق من القمم والمدد فقط
"""

from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
import wave

import numpy as np
from scipy import signal

SR = 44_100
SEED = 20261021
IMPACT_MS = 2100
TOTAL_MS = 3400

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
RAW_DIR = os.path.join(ROOT, "manager/app/src/main/res/raw")


# ───────────────────────── أدوات أساسية ─────────────────────────

def t_axis(ms: float) -> np.ndarray:
    return np.arange(int(SR * ms / 1000.0)) / SR


def env_ad(n: int, attack_ms: float, decay_power: float = 3.0) -> np.ndarray:
    """غلاف هجوم سريع ثم اضمحلال أسّي."""
    a = max(1, int(SR * attack_ms / 1000.0))
    e = np.ones(n)
    e[:a] = np.linspace(0.0, 1.0, a)
    rest = n - a
    if rest > 0:
        e[a:] = (1.0 - np.linspace(0.0, 1.0, rest)) ** decay_power
    return e


def sine(freq, t: np.ndarray) -> np.ndarray:
    """جيب بتردّد ثابت أو متغيّر (مصفوفة ترددات لحظية)."""
    f = np.broadcast_to(np.asarray(freq, dtype=float), t.shape)
    phase = 2.0 * np.pi * np.cumsum(f) / SR
    return np.sin(phase)


def band_noise(rng: np.random.Generator, n: int, lo, hi) -> np.ndarray:
    """ضجيج أبيض مُرشَّح بحزمة؛ lo/hi ثابتان أو مصفوفتان (مسح ترددي بكتل)."""
    noise = rng.standard_normal(n)
    if np.isscalar(lo) and np.isscalar(hi):
        sos = signal.butter(2, [lo, hi], btype="band", fs=SR, output="sos")
        return signal.sosfilt(sos, noise)
    lo = np.broadcast_to(np.asarray(lo, dtype=float), (n,))
    hi = np.broadcast_to(np.asarray(hi, dtype=float), (n,))
    out = np.zeros(n)
    block = 1024
    for s in range(0, n, block):
        e = min(n, s + block)
        l = float(np.clip(lo[s:e].mean(), 20, SR / 2 - 200))
        h = float(np.clip(hi[s:e].mean(), l + 40, SR / 2 - 50))
        sos = signal.butter(2, [l, h], btype="band", fs=SR, output="sos")
        out[s:e] = signal.sosfilt(sos, noise[s:e])
    return out


def lowpass(x: np.ndarray, hz: float) -> np.ndarray:
    return signal.sosfilt(signal.butter(2, hz, btype="low", fs=SR, output="sos"), x)


def fade(x: np.ndarray, in_ms: float = 2.0, out_ms: float = 10.0) -> np.ndarray:
    n_in, n_out = int(SR * in_ms / 1000), int(SR * out_ms / 1000)
    x = x.copy()
    if n_in:
        x[:n_in] *= np.linspace(0, 1, n_in)
    if n_out:
        x[-n_out:] *= np.linspace(1, 0, n_out)
    return x


def finish(x: np.ndarray, peak_db: float) -> np.ndarray:
    """قصّ ناعم ثم تطبيع إلى قمة محدّدة بالديسيبل (تحت الصفر الرقمي)."""
    x = np.tanh(1.4 * x) / np.tanh(1.4)
    m = float(np.max(np.abs(x))) or 1.0
    return x / m * (10 ** (peak_db / 20.0))


# ───────────────────────── الأصوات ─────────────────────────

def tap() -> np.ndarray:
    """نقرة زجاجية قصيرة: لمس زرّ أو تبويب."""
    t = t_axis(85)
    body = sine(1900, t) * 0.7 + sine(3800, t) * 0.2
    click = np.zeros_like(t)
    click[:60] = np.hanning(120)[:60] * np.random.default_rng(SEED).standard_normal(60)
    return finish(fade(body * env_ad(len(t), 1.0, 4.0) + click * 0.5), -9.0)


def tick() -> np.ndarray:
    """نبضة دقيقة جدًّا: عبور بطاقة في الحلقة (تتكرّر كثيرًا فتُبقى خافتة)."""
    t = t_axis(38)
    s = sine(2700, t) * env_ad(len(t), 0.6, 5.0)
    return finish(fade(s), -16.0)


def swish(up: bool = True) -> np.ndarray:
    """مسحة هواء بين الصفحتين: المسرح ⇄ مركز التحكّم."""
    rng = np.random.default_rng(SEED + 1)
    ms = 280
    n = int(SR * ms / 1000)
    sweep = np.linspace(0.0, 1.0, n) ** 1.5
    if not up:
        sweep = sweep[::-1]
    lo = 350 + 1500 * sweep
    hi = 900 + 4200 * sweep
    x = band_noise(rng, n, lo, hi)
    g = np.sin(np.linspace(0, np.pi, n)) ** 2
    return finish(fade(x * g, 6, 30), -13.0)


def toggle(on: bool) -> np.ndarray:
    """نغمتان قصيرتان: صاعدة للتشغيل، هابطة للإيقاف."""
    f1, f2 = (880.0, 1320.0) if on else (1320.0, 880.0)
    parts = []
    for f in (f1, f2):
        t = t_axis(62)
        parts.append(fade((sine(f, t) + 0.3 * sine(2 * f, t)) * env_ad(len(t), 1.0, 3.0), 1, 8))
    return finish(np.concatenate(parts), -11.0)


def start() -> np.ndarray:
    """زرّ «ابدأ»: مسح صاعد سريع + ضربة منخفضة + لمعة."""
    t = t_axis(720)
    n = len(t)
    prog = np.linspace(0, 1, n)
    rise = sine(180 + 1500 * prog ** 2.2, t) * (prog ** 1.2) * np.exp(-3.2 * np.clip(prog - 0.55, 0, 1))
    hit_t = t_axis(420)
    hit = sine(70 * np.exp(-hit_t * 3.0) + 38, hit_t) * env_ad(len(hit_t), 2.0, 2.2)
    out = rise * 0.55
    h0 = int(SR * 0.30)
    out[h0:h0 + len(hit)] += hit[: n - h0] * 1.2
    shimmer = sine(1980, t) * 0.12 * env_ad(n, 220, 2.5)
    shimmer = np.roll(shimmer, h0)
    shimmer[:h0] = 0
    out += shimmer
    return finish(fade(out, 2, 40), -6.0)


def intro() -> np.ndarray:
    """الدخول الأسطوري (٣٫٤ث): هدير باص صاعد ← ذروة ووميض عند ٢٫١ث ← ذيل لامع.

    المنحنى مأخوذ من قياس الفيديو المرجعي (طاقة تصعد ~٢ث إلى ذروة ثم تخبو)، أمّا الصوت
    نفسه فمُركَّب هنا من الصفر: جيب منخفض صاعد + مسح ضجيج + ضربة + وتر لامع.
    """
    rng = np.random.default_rng(SEED + 2)
    n = int(SR * TOTAL_MS / 1000)
    t = np.arange(n) / SR
    imp = IMPACT_MS / 1000.0
    out = np.zeros(n)

    # ١) هدير تحت الصوتي يصعد قبل الوميض: ٣٥→٧٠ هرتز مع توافقية ثانية.
    pre = np.clip(t / imp, 0, 1)
    rumble_f = 35 + 38 * pre ** 1.4
    rumble = sine(rumble_f, t) + 0.5 * sine(rumble_f * 2.0, t)
    rumble_env = np.where(t < imp, pre ** 1.8, np.exp(-(t - imp) * 1.7))
    out += rumble * rumble_env * 0.55

    # ٢) صعود (riser): ضجيج مرشّح يمسح من ٢٠٠ إلى ٧٠٠٠ هرتز نحو لحظة الوميض.
    rs = np.clip(t / imp, 0, 1) ** 2.2
    riser = band_noise(rng, n, 200 + 5200 * rs, 450 + 6800 * rs)
    riser_env = np.where(t < imp, np.clip(t / imp, 0, 1) ** 2.6, 0.0)
    out += riser * riser_env * 0.9

    # ٣) نبضات إيقاعية تتسارع (نفق): نقرات خافتة بمسافات تتقلّص حتى الوميض.
    when, step = 0.15, 0.34
    while when < imp - 0.05:
        i0 = int(when * SR)
        k = int(SR * 0.05)
        pulse = sine(95, t_axis(50)) * env_ad(k, 1.0, 4.0)
        gain = 0.12 + 0.30 * (when / imp)
        out[i0:i0 + k] += pulse * gain
        step = max(0.10, step * 0.80)
        when += step

    # ٤) الضربة عند الوميض: ضربة باص قصيرة + فرقعة ضجيج مضيئة.
    i0 = int(imp * SR)
    boom_t = t_axis(1100)
    boom = sine(62 * np.exp(-boom_t * 2.2) + 34, boom_t) * env_ad(len(boom_t), 3.0, 2.0)
    out[i0:i0 + len(boom)] += boom[: n - i0] * 1.25
    crack_t = t_axis(260)
    crack = band_noise(rng, len(crack_t), 1200, 9000) * env_ad(len(crack_t), 1.0, 3.5)
    out[i0:i0 + len(crack)] += crack[: n - i0] * 0.8

    # ٥) ذيل لامع: وتر خامس مرصوص (٣٣٠/٤٩٥/٦٦٠/٩٩٠) يتلألأ ويخبو حتى النهاية.
    tail_len = n - i0
    tt = np.arange(tail_len) / SR
    chord = np.zeros(tail_len)
    for k, f in enumerate((330.0, 495.0, 660.0, 990.0)):
        shimmer = 1.0 + 0.04 * np.sin(2 * np.pi * (5.0 + k) * tt)
        chord += sine(f * shimmer, tt) / (k + 1.2)
    chord_env = np.clip(tt / 0.06, 0, 1) * np.exp(-tt * 2.1)
    out[i0:] += chord * chord_env * 0.34

    return finish(fade(out, 8, 220), -4.5)


SOUNDS = {
    "lobby_tap": tap,
    "lobby_tick": tick,
    "lobby_swish": swish,
    "lobby_on": lambda: toggle(True),
    "lobby_off": lambda: toggle(False),
    "lobby_start": start,
    "lobby_intro": intro,
}


# ───────────────────────── كتابة وفحص ─────────────────────────

def write_wav(path: str, x: np.ndarray) -> None:
    pcm = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def encode_ogg(wav: str, ogg: str) -> None:
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        sys.exit("ffmpeg غير موجود: لا يمكن ترميز OGG (استعمل --wav).")
    subprocess.run(
        [ffmpeg, "-v", "error", "-y", "-i", wav, "-c:a", "libvorbis", "-q:a", "3",
         "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", ogg],
        check=True,
    )


def check(name: str, x: np.ndarray) -> list[str]:
    problems = []
    dur = len(x) / SR * 1000
    peak = float(np.max(np.abs(x)))
    dc = abs(float(np.mean(x)))
    if peak > 0.95:
        problems.append(f"{name}: القمة {peak:.2f} قريبة من القصّ")
    if peak < 0.05:
        problems.append(f"{name}: القمة {peak:.3f} خافتة جدًّا")
    if dc > 0.01:
        problems.append(f"{name}: انحراف تيار مستمرّ {dc:.3f}")
    if abs(x[0]) > 0.02 or abs(x[-1]) > 0.02:
        problems.append(f"{name}: بداية/نهاية غير صفرية (طقطقة محتملة)")
    if name == "lobby_intro" and abs(dur - TOTAL_MS) > 2:
        problems.append(f"{name}: المدّة {dur:.0f}ms ≠ {TOTAL_MS}ms")
    print(f"  {name:<12} {dur:7.0f} ms   peak {20*np.log10(max(peak,1e-9)):6.1f} dBFS")
    return problems


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=RAW_DIR)
    ap.add_argument("--wav", action="store_true", help="اكتب WAV بدل OGG")
    ap.add_argument("--check", action="store_true", help="افحص فقط ولا تكتب")
    args = ap.parse_args()

    problems: list[str] = []
    rendered = {name: fn() for name, fn in SOUNDS.items()}
    print("الأصوات المولَّدة:")
    for name, x in rendered.items():
        problems += check(name, x)
    if args.check:
        for p in problems:
            print("  ✗", p)
        return 1 if problems else 0

    os.makedirs(args.out, exist_ok=True)
    for name, x in rendered.items():
        wav = os.path.join(args.out, name + ".wav")
        write_wav(wav, x)
        if not args.wav:
            encode_ogg(wav, os.path.join(args.out, name + ".ogg"))
            os.remove(wav)
    for p in problems:
        print("  ✗", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
