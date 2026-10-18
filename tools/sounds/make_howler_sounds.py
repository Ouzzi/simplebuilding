"""Hollow Howler voice, variant A "besser synthetisiert" (the in-game default; concept
docs/ai/KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md, sections 8 and 9).

Usage: python tools/sounds/make_howler_sounds.py [outdir]   (Python with numpy, scipy and soundfile)

Synthesised only - no samples, no Vanilla or third-party audio - so the files are committed:
modules/simplemobs/shared/resources/assets/simplemobs/sounds/entity/hollow_howler/ (events in assets/simplemobs/
sounds.json). Variant B (built from Vanilla sounds at build time, never committed) is
tools/sounds/vanilla_derived_sounds.py, switched on with -PsbHowlerSounds=vanilla.
Mono 44.1 kHz Ogg Vorbis (Minecraft only places mono sounds in space), fixed seeds: a re-run gives the same files.

What makes it less synthetic than claude-howler3 (measured against Warden/Ravager/Zoglin, see howler_dsp.py):
pulse-by-pulse glottal source with 6-12 % jitter, shimmer and period-doubling fry; breath noise inside each glottal
period; a 4-formant cascade tract scaled for a long throat, drifting; granular wet noise and bubbles instead of
band-passed hiss; impacts as struck resonant bodies instead of sine thumps; a generated short cave room on everything;
low-passed like a recording and levelled by active RMS to the Warden's loudness instead of peak-normalised.
"""
import os
import sys

import numpy as np
import soundfile as sf

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from howler_dsp import (SR, band, bone, bubbles, curve, drift, grains, high, low, master, modal, n_of,  # noqa: E402
                        place, room, swell, voice, whisper)

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(REPO, "modules", "simplemobs", "shared", "resources", "assets", "simplemobs", "sounds", "entity",
                   "hollow_howler")


def heart_thump(rng, n, at, amp=1.0):
    """One heavy wet heartbeat: a struck, damped chest (modes 48-140 Hz) + a soft valve 'tuck'."""
    x = modal(rng, n, at, (58, 86, 124, 170), (0.11, 0.08, 0.05, 0.03), (1.0, 0.7, 0.4, 0.2), strike=0.012,
              noise_lp=300)
    x += 0.12 * modal(rng, n, at + 0.012, (380, 610), (0.012, 0.008), (1.0, 0.6), strike=0.002, noise_lp=1500)
    return x * amp


def heartbeat(seed):
    rng = np.random.default_rng(seed)
    n = n_of(0.55)
    t = np.arange(n) / SR
    x = heart_thump(rng, n, 0.0)
    # pseudo-infrasound (concept 3): 40 + 57 Hz beat at 17 Hz, quiet and short; the envelope is what the ear picks up
    x += 0.08 * (np.sin(2 * np.pi * 40 * t) + np.sin(2 * np.pi * 57 * t)) * np.exp(-t / 0.2) * np.clip(t / 0.03, 0, 1)
    return master(room(rng, x, 0.12, 0.4), -17.5, lp=1800)


def breath_cycle(rng, n, inhale, exhale, f0, fry, vowels_in, vowels_out, wet=0.0):
    """One inhale (whisper) and one exhale (creaky breathy voice) inside n samples; times as (start, end) in s."""
    x = np.zeros(n)
    a, b = inhale
    m = n_of(b - a)
    x = place(x, 0.55 * whisper(rng, m, vowels_in) * swell(m, (b - a) * 0.6, (b - a) * 0.3), a)
    a, b = exhale
    m = n_of(b - a)
    f = curve(f0, m)
    v = voice(rng, f, vowels_out, jitter=0.09, shimmer=0.3, fry=fry, breath=0.9 + wet)
    x = place(x, v * swell(m, (b - a) * 0.35, (b - a) * 0.5), a)
    return x


def ambient(seed, phase):
    rng = np.random.default_rng(seed)
    dur = (2.6, 2.8, 3.0)[phase]
    n = n_of(dur)
    if phase == 0:
        # dry hollow breathing: a slow rasping inhale, a long creaky exhale, a couple of loose bones
        x = breath_cycle(rng, n, (0.0, 0.9), (0.8, 2.55), [(0, 64), (0.4, 72), (1, 52)], 0.45, ['o', 'u'], ['o', 'a', 'u'])
        for at in rng.uniform(1.0, 2.0, 3):
            x += 0.18 * bone(rng, n, at)
        return master(room(rng, x, 0.2), -21.0, lp=4200, body=2.5)
    if phase == 1:
        # hungry: a wet, gurgling exhale, saliva crackle, a short jaw chatter run
        x = breath_cycle(rng, n, (0.0, 0.7), (0.6, 2.75), [(0, 60), (0.35, 86), (0.7, 94), (1, 58)], 0.6,
                         ['u', 'o'], ['o', 'ae', 'o'], wet=0.4)
        dens = swell(n, 0.8, 0.6)
        x += 0.25 * low(grains(rng, n, 160, 400, 3000, density=dens), 3500)
        x += 0.12 * bubbles(rng, n, 30, density=dens)
        t0 = rng.uniform(1.4, 1.7)
        for i in range(7):
            x += (0.3 - i * 0.03) * bone(rng, n, t0 + i * rng.uniform(0.05, 0.085))
        return master(room(rng, x, 0.2), -19.5, lp=4500, body=2.0)
    # rage: a low continuous growl - heavy fry, open throat, a ripping tremor - over a short breath gap
    f0 = curve([(0, 70), (0.3, 92), (0.65, 84), (1, 60)], n)
    v = voice(rng, f0, ['o', 'a', 'ae', 'o'], jitter=0.11, shimmer=0.35, fry=0.85, breath=0.7, drive=0.5,
              bw=(110, 130, 190, 260))
    trem = 0.6 + 0.4 * np.clip(drift(rng, n, 22, 0.8), 0, 2) / 2
    x = v * trem * swell(n, 0.35, 0.7)
    x += 0.15 * whisper(rng, n, ['a', 'ae']) * swell(n, 0.4, 0.8)
    x += 0.08 * grains(rng, n, 120, 500, 3000, density=swell(n, 0.5, 0.5))
    return master(room(rng, x, 0.18), -17.0, lp=5000, body=2.0)


def hurt(seed):
    rng = np.random.default_rng(seed)
    n = n_of(0.75)
    f0 = curve([(0, 150), (0.1, 128), (0.5, 88), (1, 62)], n) * rng.uniform(0.9, 1.1)
    v = voice(rng, f0, ['ae', 'a', 'o'], jitter=0.08, shimmer=0.3, fry=0.5, breath=0.6, drive=0.6)
    x = v * swell(n, 0.012, 0.45, 1.5)
    x += 0.5 * bone(rng, n, 0.0) + 0.35 * bone(rng, n, 0.025)          # a crack in the rib cage
    x += 0.5 * modal(rng, n, 0.0, (70, 115, 180), (0.06, 0.04, 0.025), (1, 0.6, 0.3), strike=0.006, noise_lp=500)
    return master(room(rng, x, 0.16, 0.45), -15.5, lp=5500, body=1.2)


def death(seed):
    rng = np.random.default_rng(seed)
    n = n_of(4.4)
    t = np.arange(n) / SR
    # a falling groan that slides into fry and breath, the heart beating three more times, weaker, then never again
    f0 = curve([(0, 104), (0.15, 88), (0.55, 50), (1, 30)], n)
    v = voice(rng, f0, ['a', 'o', 'u', 'u'], jitter=0.1, shimmer=0.35, fry=0.7, breath=0.8, drive=0.4)
    x = v * swell(n, 0.06, 1.6, 1.4) * np.exp(-t / 2.6)
    m = n_of(1.6)
    x = place(x, 0.35 * whisper(rng, m, ['o', 'u']) * swell(m, 0.2, 1.2), 2.6)            # the last breath out
    x += 0.7 * heart_thump(rng, n, 0.0) + 0.5 * heart_thump(rng, n, 0.36) + 0.25 * heart_thump(rng, n, 0.95)
    for at in (2.2, 2.27, 2.45, 2.52, 2.9, 3.4):                                          # bones settling
        x += 0.3 * bone(rng, n, at + rng.uniform(-0.02, 0.02))
    x += 0.5 * modal(rng, n, 2.15, (55, 90, 140), (0.15, 0.1, 0.06), (1, 0.6, 0.3), strike=0.02, noise_lp=400)  # body down
    return master(room(rng, x, 0.22, 0.7), -19.0, lp=5000, fade_out=0.3, body=1.5)


def step(seed):
    rng = np.random.default_rng(seed)
    n = n_of(0.6)
    idx = np.arange(n)
    # a heavy padded foot: low struck body, then dirt and sculk grit, a soft wet release, sometimes a bone knock
    x = modal(rng, n, 0.0, (52, 88, 150, 260), (0.07, 0.05, 0.03, 0.02), (1, 0.8, 0.4, 0.25), strike=0.01,
              noise_lp=700)
    x += 0.1 * grains(rng, n, 700, 400, 2500, (0.001, 0.006), density=swell(n, 0.005, 0.25, 2) * (idx < n_of(0.25)))
    x += 0.06 * band(rng.standard_normal(n), 400, 1500) * np.exp(-idx / SR / 0.05) * np.clip((idx / SR - 0.04) / 0.02, 0, 1)
    if rng.random() < 0.6:
        x += 0.06 * bone(rng, n, rng.uniform(0.02, 0.06))
    return master(room(rng, x, 0.15, 0.4), -24.5, lp=4500)


def notice(seed):
    rng = np.random.default_rng(seed)
    n = n_of(1.6)
    # a sharp intake through the teeth, three antenna ticks, then a rising questioning creak
    m = n_of(0.35)
    x = place(np.zeros(n), 0.6 * high(whisper(rng, m, ['i', 'e']), 900) * swell(m, 0.05, 0.25), 0.0)
    for at in (0.05, 0.12, 0.2):
        x += 0.35 * bone(rng, n, at)
    m = n_of(1.2)
    f0 = curve([(0, 86), (0.45, 132), (1, 96)], m)
    v = voice(rng, f0, ['u', 'e', 'o'], jitter=0.09, shimmer=0.3, fry=0.55, breath=0.8)
    x = place(x, 0.8 * v * swell(m, 0.25, 0.6), 0.3)
    return master(room(rng, x, 0.2), -19.0, lp=5500, body=1.2)


def sniff(seed):
    rng = np.random.default_rng(seed)
    n = n_of(1.1)
    x = np.zeros(n)
    # three quick wet nasal inhales and a slower one: nasal formants (high, narrow) + a little snot crackle
    at = 0.0
    for k, d in enumerate((0.13, 0.12, 0.14, 0.3)):
        m = n_of(d)
        s = band(whisper(rng, m, ['i', 'e'], bw=(90, 120, 200, 260)), 500, 5000) * np.hanning(m) ** 0.7
        x = place(x, s * (0.8 if k < 3 else 1.0), at)
        g = grains(rng, m, 300, 1000, 5000, (0.001, 0.004))
        x = place(x, 0.15 * g, at)
        at += d + rng.uniform(0.04, 0.08)
    return master(room(rng, x, 0.15), -21.5, lp=6500)


def roar(seed, rage):
    rng = np.random.default_rng(seed)
    dur = 3.4 if rage else 2.7
    n = n_of(dur)
    if rage:
        f0 = curve([(0, 64), (0.08, 104), (0.4, 118), (0.75, 86), (1, 44)], n)
        vow = ['o', 'a', 'ae', 'a', 'o']
        v = voice(rng, f0, vow, jitter=0.12, shimmer=0.35, fry=0.9, breath=0.8, drive=0.9, bw=(120, 140, 200, 280))
    else:
        f0 = curve([(0, 62), (0.12, 92), (0.5, 104), (1, 56)], n)
        vow = ['o', 'a', 'o']
        v = voice(rng, f0, vow, jitter=0.1, shimmer=0.3, fry=0.7, breath=0.7, drive=0.6, bw=(110, 130, 190, 260))
    x = v * swell(n, 0.18, 0.9 if rage else 0.7, 0.8)
    # a second, lower fold layer an octave down (false vocal folds), rougher and quieter
    v2 = voice(rng, f0 * 0.5, vow, jitter=0.12, shimmer=0.4, fry=0.4, breath=0.4, drive=0.5)
    x += 0.35 * v2 * swell(n, 0.3, 0.8)
    x += (0.22 if rage else 0.15) * whisper(rng, n, ['a', 'ae']) * swell(n, 0.25, 0.7)
    x += 0.4 * modal(rng, n, 0.0, (50, 80, 130), (0.18, 0.12, 0.07), (1, 0.6, 0.3), strike=0.03, noise_lp=300)
    if rage:
        t = np.arange(n) / SR
        x += 0.06 * (np.sin(2 * np.pi * 40 * t) + np.sin(2 * np.pi * 57 * t)) * swell(n, 0.4, 0.9)
    return master(room(rng, x, 0.24 if rage else 0.2, 0.75), -14.5 if rage else -15.5, lp=5500, fade_out=0.12, body=2.0)


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
        print(f"{name}.ogg {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS")


if __name__ == "__main__":
    main()
