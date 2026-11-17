"""Utahraptor voice (simpledinos-2 / release point 8, docs/ai/PLAN-B-REL82-2026-11-17.md): own synthesized sounds instead of
the Vanilla placeholders (parrot, fox, chicken).

Usage: python3 tools/sounds/make_raptor_sounds.py [outdir]   (Python with numpy, scipy and soundfile)

Synthesized only - no samples, no Vanilla or third-party audio - so the files are committed:
modules/simpledinos/shared/resources/assets/simpledinos/sounds/entity/utahraptor/ (events in assets/simpledinos/sounds.json).
Mono 44.1 kHz Ogg Vorbis, fixed seeds: a re-run gives the same files.

The raptor is a slim two-legged hunter about the size of a horse: a bird-like voice in a reptile throat. The tools are the
Hollow Howler toolkit (howler_dsp.py: pulse-by-pulse glottal source with jitter, breath, cascade formants, room, loudness by
active RMS) with a shorter tract (formants about 1.7 times higher than the big-creature table) and a higher pitch:
- ambient: a few quick "kek" chirps and a rolling throat trill, a low clicking purr underneath (0.8-1.1 s);
- hurt: one sharp rising-falling squawk with a rasp (0.3 s);
- death: a long falling screech that breaks into a rattling gurgle (1.3 s);
- angry: a hiss through bared teeth over a growl, ending with a jaw snap (0.75 s);
- step: a light talon tap on the ground (0.12 s).

Post-processing (owner review RAPTOR82: "alle Takes nachbearbeiten - Filter/EQ, Rauheit glaetten, minimales Reverb"): every take
runs through polish() after its own master(): high-pass 80 Hz, high-shelf -4 dB above 5 kHz and a gentle 9 kHz low-pass against
harsh highs, a soft de-esser on 4-9 kHz, a roughness smoother (4 ms vs 25 ms envelope) that evens out the fast grain/fry flutter (voices only, not the
talon taps), a small-room reverb (wet 10 %, steps 6 %, RT60 0.25 s, tail under 0.35 s), then the take's active RMS is restored
to its pre-polish value and the peak soft-limited at -1 dBFS (loudness stays where it was, close to Vanilla mobs).
"""
import os
import sys

import numpy as np
import soundfile as sf
from scipy.signal import lfilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from howler_dsp import (SR, VOWELS, active_rms_db, aspiration, band, curve, drift, formant_cascade, glottal, grains, high, low,  # noqa: E402
                        master, modal, n_of, place, room, swell)

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(REPO, "modules", "simpledinos", "shared", "resources", "assets", "simpledinos", "sounds", "entity", "utahraptor")
#: A shorter throat than the big-creature table of howler_dsp (adult vowels x 0.62): x 1.7 gives about adult x 1.05.
TRACT = 1.7
BW = (110, 140, 220, 300)


def tracks(rng, names, n, wobble=0.04):
    pts = np.array([VOWELS[v] for v in names], dtype=float) * TRACT
    pos = np.linspace(0, len(names) - 1, n)
    return [np.interp(pos, np.arange(len(names)), pts[:, f]) * drift(rng, n, 3.0, wobble) for f in range(4)]


def voice(rng, f0c, vowels, jitter=0.05, shimmer=0.18, fry=0.0, breath=0.4, drive=0.0):
    src, flow = glottal(rng, f0c, jitter, shimmer, fry)
    y = formant_cascade(src + aspiration(rng, flow, breath), tracks(rng, vowels, len(f0c)), BW)
    y = y / (np.max(np.abs(y)) + 1e-9)
    if drive:
        y = np.tanh(y * (1 + 3 * drive))
        y /= np.max(np.abs(y)) + 1e-9
    return y


def chirp(rng, dur, f_start, f_peak, f_end, vowels=("e", "i", "a"), fry=0.15, breath=0.45):
    """One 'kek': fast rise to the peak in the first third, then a fall; short raised-cosine edges."""
    n = n_of(dur)
    f0 = curve([(0, f_start), (0.3, f_peak), (1, f_end)], n)
    return voice(rng, f0, vowels, jitter=0.05, fry=fry, breath=breath) * swell(n, 0.008, dur * 0.45)


def ambient(seed, calls, trill):
    rng = np.random.default_rng(seed)
    total = 0.12 + sum(d + g for d, g, *_ in calls) + trill[0] + 0.2
    n = n_of(total)
    x = np.zeros(n)
    t = 0.06
    for dur, gap, fs, fp, fe in calls:
        place(x, chirp(rng, dur, fs, fp, fe) * rng.uniform(0.75, 1.0), t)
        t += dur + gap
    # rolling trill: the throat flutters (~26 Hz) on a slowly falling note
    td, tf = trill
    m = n_of(td)
    f0 = curve([(0, tf * 1.08), (0.4, tf), (1, tf * 0.82)], m)
    flutter = 0.35 + 0.65 * (0.5 + 0.5 * np.sin(2 * np.pi * 26 * np.arange(m) / SR * drift(rng, m, 4, 0.08)))
    place(x, voice(rng, f0, ("a", "o"), jitter=0.07, fry=0.35, breath=0.5) * flutter * swell(m, 0.03, td * 0.5) * 0.8, t)
    # low clicking purr under the whole call
    purr = voice(rng, np.full(n, 105.0) * drift(rng, n, 2, 0.05), ("o", "u"), jitter=0.12, fry=0.7, breath=0.2)
    x += 0.18 * low(purr, 900) * swell(n, 0.15, 0.25)
    x = room(rng, x, wet=0.12, rt60=0.35)
    return master(x, -18.5, peak_db=-2, lp=9000, hp=120)


def hurt(seed, peak):
    rng = np.random.default_rng(seed)
    dur = 0.3
    n = n_of(dur + 0.1)
    m = n_of(dur)
    f0 = curve([(0, peak * 0.62), (0.22, peak), (0.55, peak * 0.85), (1, peak * 0.45)], m)
    y = voice(rng, f0, ("ae", "a", "o"), jitter=0.08, fry=0.25, breath=0.6, drive=0.5) * swell(m, 0.006, 0.12)
    y += 0.3 * band(rng.standard_normal(m), 2500, 7000) * np.exp(-np.arange(m) / SR / 0.035)  # the breath burst at the onset
    x = place(np.zeros(n), y, 0.01)
    x = room(rng, x, wet=0.14, rt60=0.3)
    return master(x, -16, peak_db=-1.5, lp=10000, hp=150)


def death(seed, start):
    rng = np.random.default_rng(seed)
    dur = 1.3
    n = n_of(dur + 0.15)
    scr = n_of(0.8)
    f0 = curve([(0, start * 0.8), (0.12, start), (0.6, start * 0.6), (1, start * 0.3)], scr)
    fry = 0.15
    y = voice(rng, f0, ("a", "ae", "o", "u"), jitter=0.09, fry=fry, breath=0.55, drive=0.35) * swell(scr, 0.02, 0.35)
    x = place(np.zeros(n), y, 0.02)
    # breaking into a rattling gurgle: a low, period-doubled voice and wet grains
    gm = n_of(0.65)
    g0 = curve([(0, 190), (1, 120)], gm)
    gur = voice(rng, g0, ("o", "u"), jitter=0.15, fry=0.85, breath=0.3) * swell(gm, 0.06, 0.4)
    gur += 0.4 * grains(rng, gm, 140, 300, 1600, (0.003, 0.012)) * swell(gm, 0.05, 0.3)
    place(x, 0.6 * gur, 0.62)
    x = room(rng, x, wet=0.16, rt60=0.45)
    return master(x, -18, peak_db=-2, lp=9000, hp=90)


def angry(seed, growl_f):
    rng = np.random.default_rng(seed)
    dur = 0.75
    n = n_of(dur)
    # hiss: noise through an 'i' tract, bright, with a fast flutter of the tongue
    hiss = formant_cascade(high(rng.standard_normal(n), 1800), tracks(rng, ("i", "e"), n, 0.06), (300, 400, 600, 800))
    hiss = hiss / (np.max(np.abs(hiss)) + 1e-9) + 0.5 * band(rng.standard_normal(n), 3500, 9000)
    hiss *= 0.8 + 0.2 * np.sin(2 * np.pi * 17 * np.arange(n) / SR)
    x = hiss * swell(n, 0.05, 0.2) * 0.7
    growl = voice(rng, np.full(n, growl_f) * drift(rng, n, 3, 0.06), ("a", "o"), jitter=0.1, fry=0.6, breath=0.3, drive=0.4)
    x += 0.55 * low(growl, 1500) * swell(n, 0.08, 0.15)
    # jaw snap at the end: a hard, short bony click
    snap = modal(rng, n, dur - 0.09, (1400, 2600, 3900), (0.012, 0.007, 0.004), (1.0, 0.6, 0.35), strike=0.0012, noise_lp=8000)
    x += 0.9 * snap
    x = room(rng, x, wet=0.1, rt60=0.3)
    return master(x, -18, peak_db=-2, lp=11000, hp=140)


def step(seed, f):
    rng = np.random.default_rng(seed)
    n = n_of(0.14)
    tap = modal(rng, n, 0.004, (f, f * 1.83, f * 2.7), (0.006, 0.004, 0.003), (1.0, 0.55, 0.3), strike=0.001, noise_lp=9000)
    thud = modal(rng, n, 0.0, (95, 160), (0.02, 0.012), (1.0, 0.4), strike=0.004, noise_lp=500)
    x = 0.7 * tap + 0.5 * thud + 0.15 * grains(rng, n, 900, 1500, 6000, (0.0006, 0.002)) * swell(n, 0.001, 0.08)
    return master(x, -24, peak_db=-6, lp=10000, hp=60, fade_out=0.02)


def high_shelf(x, f0, gain_db, q=0.707):
    """RBJ-cookbook high shelf (biquad)."""
    a = 10 ** (gain_db / 40)
    w = 2 * np.pi * f0 / SR
    cw, al = np.cos(w), np.sin(w) / (2 * q)
    sa = 2 * np.sqrt(a) * al
    b = [a * ((a + 1) + (a - 1) * cw + sa), -2 * a * ((a - 1) + (a + 1) * cw), a * ((a + 1) + (a - 1) * cw - sa)]
    den = [(a + 1) - (a - 1) * cw + sa, 2 * ((a - 1) - (a + 1) * cw), (a + 1) - (a - 1) * cw - sa]
    return lfilter(np.array(b) / den[0], np.array(den) / den[0], x)


def envelope(x, ms):
    """Rectified signal smoothed by a one-pole low-pass of the given time constant."""
    c = np.exp(-1 / (SR * ms / 1000))
    return lfilter([1 - c], [1, -c], np.abs(x)) + 1e-6


#: polish settings (see the module docstring); steps keep their transients (no smoother) and get less room
POLISH = dict(hp=80, shelf_f=5000, shelf_db=-4.0, lp=9000, ess_lo=4000, ess_hi=9000, ess_ratio=0.3, ess_pow=0.5,
              rough_pow=0.5, rough_range=(0.6, 1.5), wet=0.10, rt60=0.25, tail=0.3, ceiling_db=-1.0)


def polish(x, seed, transient=False):
    p = POLISH
    x = x.astype(np.float64)
    target = active_rms_db(x)
    x = np.concatenate([x, np.zeros(n_of(p["tail"]))])  # room for the reverb tail
    # filter / EQ: rumble out, harsh highs down
    x = high(x, p["hp"], 2)
    x = low(high_shelf(x, p["shelf_f"], p["shelf_db"]), p["lp"], 2)
    # de-esser: the 4-9 kHz band is pulled down where it dominates the full-band level
    s = band(x, p["ess_lo"], p["ess_hi"])
    over = envelope(s, 5) / (envelope(x, 5) * p["ess_ratio"])
    x = x - s + s * np.minimum(1.0, over ** -p["ess_pow"])
    if not transient:
        # roughness smoother: the fast (4 ms) amplitude flutter of fry and grains pulled toward the slower (25 ms) envelope
        g = np.clip((envelope(x, 25) / envelope(x, 4)) ** p["rough_pow"], *p["rough_range"])
        x = x * g
    # minimal small room
    x = room(np.random.default_rng(seed), x, wet=p["wet"] * (0.6 if transient else 1.0), rt60=p["rt60"])
    # keep the loudness, soft-limit the peak, fade the padded tail
    x = x * 10 ** ((target - active_rms_db(x)) / 20)
    lim = 10 ** (p["ceiling_db"] / 20)
    k = lim * 0.7
    x = np.where(np.abs(x) > k, np.sign(x) * (k + (lim - k) * np.tanh((np.abs(x) - k) / (lim - k))), x)
    end = min(len(x), np.nonzero(np.abs(x) > 10 ** (-60 / 20))[0][-1] + n_of(0.02))  # cut the silent rest of the pad
    x = x[:end]
    b = n_of(0.03)
    x[-b:] *= np.linspace(1, 0, b) ** 2
    return x.astype(np.float32)


SOUNDS = {
    "ambient1": lambda: ambient(1101, [(0.07, 0.06, 560, 900, 520), (0.06, 0.05, 600, 960, 540), (0.08, 0.09, 540, 880, 480)], (0.36, 430)),
    "ambient2": lambda: ambient(1102, [(0.06, 0.05, 620, 1000, 600), (0.06, 0.05, 640, 1040, 620)], (0.42, 470)),
    "ambient3": lambda: ambient(1103, [(0.09, 0.07, 500, 820, 440), (0.06, 0.04, 580, 920, 520), (0.06, 0.04, 600, 940, 540),
                                       (0.07, 0.1, 560, 900, 500)], (0.3, 410)),
    "hurt1": lambda: hurt(1201, 1250),
    "hurt2": lambda: hurt(1202, 1120),
    "death1": lambda: death(1301, 1050),
    "death2": lambda: death(1302, 950),
    "angry1": lambda: angry(1401, 165),
    "angry2": lambda: angry(1402, 145),
    "step1": lambda: step(1501, 2100),
    "step2": lambda: step(1502, 2400),
    "step3": lambda: step(1503, 1900),
}


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else OUT
    os.makedirs(out, exist_ok=True)
    for name, make in SOUNDS.items():
        x = polish(make(), 9000 + sum(map(ord, name)), transient=name.startswith("step"))
        sf.write(os.path.join(out, name + ".ogg"), x, SR, format="OGG", subtype="VORBIS")
        print(f"{name}.ogg {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x)) + 1e-9):.1f} dBFS")


if __name__ == "__main__":
    main()
