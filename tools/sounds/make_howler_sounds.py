"""Synthesises the Hollow Howler voice (phase 3 of docs/ai/KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md).

Usage: python tools/sounds/make_howler_sounds.py [outdir]   (Python with numpy, scipy and soundfile)

Everything is generated from noise, oscillators and filters - no samples, no Vanilla or third-party audio.
Output: mono 44.1 kHz Ogg Vorbis (libsndfile) into
modules/simplemobs/shared/resources/assets/simplemobs/sounds/entity/hollow_howler/ (the events are declared in
assets/simplemobs/sounds.json). Mono on purpose: Minecraft only places mono sounds in space, and the Howler is
heard before it is seen. Fixed seeds, so a re-run gives the same files.

Design (see the concept, section 2/3): a heavy, wrong-sounding hollow body.
- heartbeat: one thump (the code plays it twice for lub-dub) = pitch-dropping sub sine ~45 Hz, a dull 90-130 Hz knock,
  and a very quiet 40 + 57 Hz layer whose 17 Hz beating is audible as a pulse even on laptop speakers.
- ambient / ambient_hunger / ambient_rage: dry bone-rattling breath -> wet rasp with jaw chatter -> distorted growl with
  sub-octave and a tremor. Each phase is clearly worse than the last.
- hurt, death, step, notice, sniff, roar, roar_rage: same palette (breathy formant voice, bone clicks, sub thump).
"""
import os
import sys

import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt

SR = 44100
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(REPO, "modules", "simplemobs", "shared", "resources", "assets", "simplemobs", "sounds", "entity",
                   "hollow_howler")


def band(x, lo, hi, order=3):
    return sosfilt(butter(order, [lo, hi], btype="band", fs=SR, output="sos"), x)


def low(x, hi, order=3):
    return sosfilt(butter(order, hi, btype="low", fs=SR, output="sos"), x)


def high(x, lo, order=3):
    return sosfilt(butter(order, lo, btype="high", fs=SR, output="sos"), x)


def tvec(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, attack, release, power=1.5):
    """Linear attack (s), then (1-x)^power over the rest, release (s) tail fade."""
    t = np.arange(n) / SR
    e = np.clip(t / max(attack, 1e-4), 0, 1) * (1 - t / (n / SR)) ** power
    return e * np.clip((n / SR - t) / max(release, 1e-4), 0, 1)


def smooth(x, hz):
    return low(x, hz, 2)


def drive(x, amount):
    """Soft clip: more harmonics, a wrongly 'hot' voice."""
    return np.tanh(x * amount) / np.tanh(amount)


def curve(points, n):
    """Piecewise-linear control curve over n samples from (position 0..1, value) points."""
    xs, ys = zip(*points)
    return np.interp(np.linspace(0, 1, n), xs, ys)


def voice(rng, f0c, vowels, rasp=0.3, jitter=0.012, tremor=0.0, tremor_hz=14.0):
    """Glottal saw through moving formants. f0c: f0 per sample; vowels: [(F1,F2,F3)] morphed across the length."""
    n = len(f0c)
    t = np.arange(n) / SR
    jit = smooth(rng.standard_normal(n), 40) * 8 * jitter
    f = f0c * (1 + jit)
    f = f * (1 + 0.012 * np.sin(2 * np.pi * 5.2 * t))
    ph = np.cumsum(f) / SR
    saw = 2 * (ph % 1.0) - 1
    saw += rasp * rng.standard_normal(n) * (0.4 + 0.6 * np.abs(saw))
    out = np.zeros(n)
    k = len(vowels)
    pos = np.linspace(0, k - 1, n)
    for ff in range(3):
        sig = np.zeros(n)
        for i, v in enumerate(vowels):
            w = np.clip(1 - np.abs(pos - i), 0, 1)
            sig += w * band(saw, v[ff] * 0.85, v[ff] * 1.15, 2)
        out += sig * (1.0, 0.7, 0.4)[ff]
    if tremor:
        out *= 1 - tremor + tremor * np.sin(2 * np.pi * tremor_hz * t)
    return out


def click(rng, n, at, f, ring=0.012, amp=1.0):
    """A dry bone tick: short decaying high band burst, placed in an n-sample buffer at time 'at' (s)."""
    m = int(SR * ring * 4)
    tt = np.arange(m) / SR
    burst = band(rng.standard_normal(m), f * 0.7, f * 1.4, 2) * np.exp(-tt / ring) * amp
    out = np.zeros(n)
    i = int(at * SR)
    k = min(m, n - i)
    if k > 0:
        out[i:i + k] = burst[:k]
    return out


def thump(n, at, f_start, f_end, decay, amp=1.0):
    m = int(SR * decay * 5)
    tt = np.arange(m) / SR
    f = f_end + (f_start - f_end) * np.exp(-tt / (decay * 0.35))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / decay) * amp
    out = np.zeros(n)
    i = int(at * SR)
    k = min(m, n - i)
    if k > 0:
        out[i:i + k] = s[:k]
    return out


def breath(rng, n, lo, hi, a=0.25, r=0.4, wet=0.0):
    x = band(rng.standard_normal(n), lo, hi)
    if wet:
        # wet crackle: slow-random amplitude gating
        gate = smooth(np.abs(rng.standard_normal(n)), 90)
        x *= 0.5 + wet * gate / (gate.max() + 1e-9) * 2
    return x * env(n, a, r, 1.0)


def finish(x, peak=0.8, fade=0.01):
    x = x - np.mean(x)
    x = high(x, 20, 2)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    m = int(SR * fade)
    x[:m] *= np.linspace(0, 1, m)
    x[-m:] *= np.linspace(1, 0, m)
    return x.astype(np.float32)


# ---------------------------------------------------------------------------------------------------------------

def heartbeat(seed):
    rng = np.random.default_rng(seed)
    dur = 0.55
    n = int(SR * dur)
    t = tvec(dur)
    x = thump(n, 0.0, 70, 44, 0.16, 1.0)
    x += 0.45 * thump(n, 0.0, 130, 92, 0.07, 1.0)
    x += 0.25 * low(rng.standard_normal(n), 240, 2) * np.exp(-t / 0.02)
    # pseudo-infrasound: 40 + 57 Hz beat at 17 Hz, quiet and short; the envelope is what the ear picks up
    beat = (np.sin(2 * np.pi * 40 * t) + np.sin(2 * np.pi * 57 * t)) * 0.5
    x += 0.12 * beat * np.exp(-t / 0.22) * np.clip(t / 0.03, 0, 1)
    return finish(x, 0.8)


def ambient(seed, phase):
    rng = np.random.default_rng(seed)
    dur = (2.4, 2.8, 3.0)[phase]
    n = int(SR * dur)
    t = tvec(dur)
    if phase == 0:
        # dry, hollow: slow rasp breath in and out, a few bone ticks, a faint low moan
        a = breath(rng, n, 500, 3500, 0.7, 0.8, 0.0) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.42 * t - 1.0) ** 2)
        f0 = curve([(0, 62), (0.5, 70), (1, 55)], n)
        v = voice(rng, f0, [(300, 800, 2300), (450, 900, 2400)], rasp=0.2) * env(n, 0.9, 0.8, 0.8) * 0.35
        x = a * 0.5 + v
        for at in (0.9, 1.05, 1.62):
            x += click(rng, n, at + rng.uniform(-0.02, 0.02), rng.uniform(2200, 3600), 0.006, 0.5)
    elif phase == 1:
        # wet, hungry: gurgling rasp, jaw chatter run, a rising uneasy moan
        a = breath(rng, n, 300, 4200, 0.5, 0.6, wet=0.9) * 0.7
        f0 = curve([(0, 58), (0.4, 80), (0.75, 92), (1, 60)], n)
        v = voice(rng, f0, [(350, 750, 2250), (600, 1100, 2500), (400, 850, 2300)], rasp=0.45, tremor=0.25) \
            * env(n, 0.6, 0.7, 0.9) * 0.55
        x = a * 0.55 + v
        for i in range(9):  # chatter
            x += click(rng, n, 1.15 + i * rng.uniform(0.045, 0.075), rng.uniform(1800, 3200), 0.007, 0.6 - i * 0.04)
        x += 0.3 * thump(n, 0.05, 60, 40, 0.3)
    else:
        # rage: low distorted growl with a sub-octave, throat tremor and a ripping top layer
        f0 = curve([(0, 48), (0.35, 70), (0.7, 62), (1, 42)], n)
        v = voice(rng, f0, [(280, 700, 2200), (500, 1000, 2400), (320, 760, 2300)], rasp=0.7, jitter=0.03,
                  tremor=0.45, tremor_hz=21.0) * env(n, 0.25, 0.6, 0.7)
        sub = np.sin(2 * np.pi * np.cumsum(f0 * 0.5) / SR) * 0.6 * env(n, 0.3, 0.6, 0.7)
        rip = band(rng.standard_normal(n), 1500, 6500) * env(n, 0.4, 0.5, 0.7) * (0.4 + 0.6 * (np.sin(2 * np.pi * 21 * t) > 0))
        x = drive(v * 1.4 + sub, 2.2) * 0.8 + rip * 0.25
        x += 0.25 * thump(n, 0.0, 55, 36, 0.35)
    return finish(x, 0.8)


def hurt(seed):
    rng = np.random.default_rng(seed)
    dur = 0.6
    n = int(SR * dur)
    f0 = curve([(0, 130), (0.12, 105), (1, 62)], n) * rng.uniform(0.92, 1.08)
    v = voice(rng, f0, [(650, 1100, 2500), (350, 800, 2300)], rasp=0.6) * env(n, 0.012, 0.15, 1.1)
    x = drive(v, 1.8)
    x += click(rng, n, 0.0, 2500, 0.01, 1.2) + click(rng, n, 0.03, 1700, 0.015, 0.9)  # bone crack
    x += 0.5 * thump(n, 0.0, 95, 50, 0.12)
    return finish(x, 0.8)


def death(seed):
    rng = np.random.default_rng(seed)
    dur = 4.2
    n = int(SR * dur)
    t = tvec(dur)
    f0 = curve([(0, 96), (0.18, 80), (0.6, 45), (1, 26)], n)
    v = voice(rng, f0, [(550, 1000, 2400), (350, 800, 2300), (300, 700, 2200)], rasp=0.55, tremor=0.3, tremor_hz=9.0) \
        * env(n, 0.05, 1.2, 1.4)
    x = drive(v, 1.8) * 0.9
    x += breath(rng, n, 400, 3800, 0.05, 1.2, wet=0.6) * 0.35 * np.exp(-t / 1.6)   # last breath
    x += 0.8 * thump(n, 0.0, 90, 36, 0.4)
    x += 0.7 * thump(n, 0.32, 70, 34, 0.45)    # the heart: one more, weaker, then never again
    x += 0.35 * thump(n, 0.85, 55, 32, 0.5)
    for at, f in ((2.3, 2400), (2.38, 1900), (2.55, 2800), (2.62, 1600), (3.05, 2100)):  # bones settling
        x += click(rng, n, at, f, 0.012, 0.55)
    return finish(x, 0.8, 0.05)


def step(seed):
    rng = np.random.default_rng(seed)
    dur = 0.42
    n = int(SR * dur)
    t = tvec(dur)
    x = thump(n, 0.0, 85, 42, 0.11, 1.0)
    x += 0.5 * low(rng.standard_normal(n), 260, 2) * np.exp(-t / 0.05)                          # dull weight
    x += 0.35 * band(rng.standard_normal(n), 500, 2400) * np.exp(-t / 0.07) * np.clip((t - 0.05) / 0.02, 0, 1)  # wet squelch
    x += click(rng, n, 0.01, rng.uniform(1800, 2600), 0.006, 0.4)
    return finish(x, 0.78)


def notice(seed):
    rng = np.random.default_rng(seed)
    dur = 1.5
    n = int(SR * dur)
    f0 = curve([(0, 110), (0.35, 130), (1, 85)], n)
    v = voice(rng, f0, [(300, 2100, 2800), (350, 1100, 2400)], rasp=0.35) * env(n, 0.25, 0.5, 1.0) * 0.5
    x = v + breath(rng, n, 1500, 6500, 0.18, 0.4) * 0.45
    for at in (0.0, 0.07, 0.15):                       # antenna ticks
        x += click(rng, n, at, 3200, 0.004, 0.9)
    return finish(x, 0.78)


def sniff(seed):
    rng = np.random.default_rng(seed)
    dur = 0.9
    n = int(SR * dur)
    t = tvec(dur)
    x = breath(rng, n, 700, 5200, 0.35, 0.3, wet=0.5) * (0.5 + 0.5 * np.sin(2 * np.pi * 3.1 * t) ** 2)
    x += 0.25 * thump(n, 0.0, 70, 45, 0.2)
    return finish(x, 0.78)


def roar(seed, rage):
    rng = np.random.default_rng(seed)
    dur = 3.4 if rage else 2.6
    n = int(SR * dur)
    t = tvec(dur)
    if rage:
        f0 = curve([(0, 52), (0.08, 90), (0.4, 110), (0.8, 74), (1, 36)], n)
        vow = [(300, 800, 2300), (700, 1200, 2600), (450, 1000, 2500), (320, 760, 2250)]
        v = voice(rng, f0, vow, rasp=0.75, jitter=0.035, tremor=0.5, tremor_hz=19.0)
    else:
        f0 = curve([(0, 55), (0.12, 84), (0.5, 98), (1, 50)], n)
        vow = [(320, 760, 2250), (650, 1100, 2500), (400, 850, 2300)]
        v = voice(rng, f0, vow, rasp=0.55, jitter=0.025, tremor=0.3, tremor_hz=12.0)
    v *= env(n, 0.14, 0.7 if rage else 0.5, 0.55)
    sub = np.sin(2 * np.pi * np.cumsum(f0 * 0.5) / SR) * 0.7 * env(n, 0.2, 0.7, 0.6)
    x = drive(v * (1.8 if rage else 1.3) + sub, 2.4 if rage else 1.8)
    hiss = band(rng.standard_normal(n), 1800, 7500) * env(n, 0.25, 0.6, 0.8)
    x += hiss * (0.28 if rage else 0.15) * (0.55 + 0.45 * np.sin(2 * np.pi * (21 if rage else 11) * t) ** 2)
    x += 0.6 * thump(n, 0.0, 60, 34, 0.5)
    if rage:
        x += 0.35 * thump(n, 0.45, 50, 32, 0.5)
        # pseudo-infrasound underlay (40 + 57 Hz, 17 Hz beating), quiet and for the length of the roar only
        x += 0.1 * (np.sin(2 * np.pi * 40 * t) + np.sin(2 * np.pi * 57 * t)) * env(n, 0.3, 0.8, 0.5)
    return finish(x, 0.82, 0.03)


FILES = {
    "heartbeat": lambda: heartbeat(11),
    "ambient1": lambda: ambient(21, 0), "ambient2": lambda: ambient(22, 0),
    "ambient_hunger1": lambda: ambient(31, 1), "ambient_hunger2": lambda: ambient(32, 1),
    "ambient_rage1": lambda: ambient(41, 2), "ambient_rage2": lambda: ambient(42, 2),
    "hurt1": lambda: hurt(51), "hurt2": lambda: hurt(52), "hurt3": lambda: hurt(53),
    "death": lambda: death(61),
    "step1": lambda: step(71), "step2": lambda: step(72), "step3": lambda: step(73),
    "notice": lambda: notice(81),
    "sniff": lambda: sniff(91),
    "roar": lambda: roar(101, False),
    "roar_rage": lambda: roar(111, True),
}


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else OUT
    os.makedirs(out, exist_ok=True)
    for name, make in FILES.items():
        x = make()
        sf.write(os.path.join(out, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
        print(f"{name}.ogg {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS rms {20 * np.log10(np.sqrt(np.mean(x ** 2))):.1f} dBFS")


if __name__ == "__main__":
    main()
