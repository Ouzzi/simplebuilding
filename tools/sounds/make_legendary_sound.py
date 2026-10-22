"""Synthesises SimpleLib's legendary fanfare (simplelib:legendary, owner SP22-2, 2026-10-22).

The owner liked Vanilla's challenge jingle ("so etwas oder besser"); this one is bigger and has its own shape, built
only from oscillators and noise (no samples), with the room/loudness tools of the Hollow Howler (howler_dsp):
- a deep gong stroke with a sub boom (the "moment"),
- a brass-like rising arpeggio D4-F#4-A4-D5 whose brightness opens like a horn (harmonics grow with time),
- a held D major chord (D5 F#5 A5 D6) with slow vibrato, swelling and fading,
- bell sparkles from the D major pentatonic high up, and a rising air whoosh into the first hit,
- a large generated hall (rt60 1.8 s), loudness like Vanilla's UI jingles (active RMS -18 dBFS, peak -1 dBFS).
Two takes (different sparkle seeds, take 2 a touch lower) so repeats do not sound identical.

Run with the Python that has numpy/scipy/soundfile, e.g. on sb-test /var/tmp/sb/audio-venv/bin/python
(or SB_PYTHON). Writes modules/simplelib/shared/resources/assets/simplelib/sounds/legendary{1,2}.ogg and, with
--wav DIR, 16-bit WAV copies for previews.

SP22-2 Nachtrag (owner 22.10. evening: "etwas schwach, besser generieren, mehr mystisch"): --variants DIR writes three
new candidates as WAV only (variant_A/B/C.wav; the shipped Ogg files stay untouched until the owner has chosen):
- A "Chorhalle": a wordless choir pad (formant-shaped additive voices) from D minor into D major, a deep D2 bell
  under both chords, long hall (rt60 3.2 s),
- B "Kristall": amethyst-like chimes, the overtone series of A2 rising one partial after another, echo shadows of
  the chimes (dark delayed copies) and a lydian E bell chord at the top,
- C "Portal": a dark beating drone (D1/D2 with Eb and Ab) under swirling portal air that glides into a bright
  D major chord with bells - the End portal's mood resolving into light.
"""
import argparse
import os
import sys

import numpy as np
import soundfile as sf

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import howler_dsp as d  # noqa: E402

SR = d.SR
DUR = 3.4
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(REPO, "modules/simplelib/shared/resources/assets/simplelib/sounds")


def hz(semitones_from_a4):
    return 440.0 * 2 ** (semitones_from_a4 / 12)


D4, FS4, A4, D5, FS5, A5, D6 = (hz(s) for s in (-7, -3, 0, 5, 9, 12, 17))
PENTA = [hz(s) for s in (17, 19, 21, 24, 26, 29, 31, 33)]  # D6 E6 F#6 A6 B6 D7 E7 F#7


def brass(f, n, rng, open_from=0.25, open_to=0.85, vibrato=0.0):
    """Horn-like tone: harmonics 1/k, the upper ones grow as the 'lip' opens (brightness from open_from to open_to)."""
    t = np.arange(n) / SR
    bright = d.curve([(0, open_from), (0.35, open_to), (1, open_to * 0.8)], n)
    vib = 1 + vibrato * np.sin(2 * np.pi * 5.2 * t) * np.clip(t / 0.4, 0, 1)
    phase = 2 * np.pi * np.cumsum(f * vib * d.drift(rng, n, 3, 0.0015)) / SR
    out = np.zeros(n)
    for k in range(1, 14):
        if k * f > 9000:
            break
        out += np.sin(k * phase + rng.uniform(0, 6.28)) * (bright ** (k - 1)) / k
    return out


def bell(f, n, rng, decay=1.6):
    """Struck bell: inharmonic partials, each with its own decay."""
    t = np.arange(n) / SR
    out = np.zeros(n)
    for ratio, amp, dec in ((1.0, 1.0, decay), (2.0, 0.55, decay * 0.7), (2.76, 0.4, decay * 0.5),
                            (5.4, 0.22, decay * 0.3), (8.93, 0.12, decay * 0.2)):
        out += amp * np.sin(2 * np.pi * f * ratio * t + rng.uniform(0, 6.28)) * np.exp(-t / dec)
    return out * np.clip(t / 0.002, 0, 1)


def gong(n, rng):
    t = np.arange(n) / SR
    body = np.zeros(n)
    for f, amp, dec in ((73.4, 1.0, 1.4), (110.0, 0.5, 1.1), (146.8, 0.45, 0.9), (196.0, 0.3, 0.7),
                        (233.0, 0.22, 0.5), (311.0, 0.15, 0.4)):
        body += amp * np.sin(2 * np.pi * f * (1 + 0.004 * np.exp(-t * 3)) * t + rng.uniform(0, 6.28)) * np.exp(-t / dec)
    sub = np.sin(2 * np.pi * np.cumsum(55 * np.exp(-t * 1.5) + 36) / SR) * np.exp(-t * 3.2)
    click = d.band(rng.standard_normal(n), 800, 5000) * np.exp(-t * 60)
    return 0.8 * body + 0.7 * sub + 0.15 * click


def make(seed, pitch=1.0):
    rng = np.random.default_rng(seed)
    n = d.n_of(DUR)
    buf = np.zeros(n)
    lead = 0.12  # the whoosh runs into the first hit

    # rising air into the gong
    m = d.n_of(lead + 0.05)
    whoosh = d.band(rng.standard_normal(m), 600, 6000) * d.curve([(0, 0), (0.85, 1), (1, 0)], m) ** 2
    d.place(buf, 0.25 * whoosh, 0.0)

    d.place(buf, 0.9 * gong(d.n_of(2.8), rng), lead)

    # arpeggio: four horn notes, each short and accented
    for i, f in enumerate((D4, FS4, A4, D5)):
        m = d.n_of(0.42)
        note = brass(f * pitch, m, rng, open_from=0.3, open_to=0.7) * d.swell(m, 0.015, 0.25)
        d.place(buf, 0.32 * note, lead + 0.02 + i * 0.15)

    # held chord: swells after the arpeggio, slow vibrato, fades into the hall
    m = d.n_of(DUR - lead - 0.62)
    chord = sum(brass(f * pitch, m, rng, open_from=0.35, open_to=0.8, vibrato=0.006) * w
                for f, w in ((D5, 1.0), (FS5, 0.75), (A5, 0.8), (D6, 0.45), (D4, 0.5)))
    chord *= d.curve([(0, 0), (0.08, 1), (0.45, 0.85), (1, 0)], m) ** 1.3
    d.place(buf, 0.2 * chord, lead + 0.6)

    # bell sparkles from the D major pentatonic, denser at first
    for k in range(14):
        at = lead + 0.6 + rng.beta(1.4, 2.6) * 2.2
        f = PENTA[rng.integers(len(PENTA))] * pitch
        d.place(buf, rng.uniform(0.05, 0.11) * bell(f, d.n_of(1.2), rng, decay=rng.uniform(0.35, 0.7)), at)
    d.place(buf, 0.16 * bell(D6 * pitch, d.n_of(2.0), rng, decay=1.2), lead + 0.62)

    hall = d.room(rng, buf, wet=0.32, rt60=1.8)
    return d.master(hall, -18.0, peak_db=-1.0, lp=12000, hp=30, fade_out=0.35)


# --- SP22-2 Nachtrag: three more mystical candidates (owner choice pending) --------------------------------------------

VOWELS = {"a": ((800, 1.0, 90), (1150, 0.5, 100), (2900, 0.22, 140)),
          "o": ((450, 1.0, 80), (800, 0.5, 90), (2830, 0.12, 120)),
          "u": ((325, 1.0, 70), (700, 0.25, 80), (2530, 0.06, 120))}


def choir_voice(f, n, rng, vowel, detune_cents=0.0):
    """One sung 'aah/ooh' voice: additive harmonics weighted by the vowel's formants, gentle vibrato and breath."""
    t = np.arange(n) / SR
    vib = 1 + 0.0045 * np.sin(2 * np.pi * rng.uniform(4.6, 5.6) * t + rng.uniform(0, 6.28)) * np.clip(t / 0.6, 0, 1)
    f = f * 2 ** (detune_cents / 1200)
    phase = 2 * np.pi * np.cumsum(f * vib * d.drift(rng, n, 2, 0.002)) / SR
    out = np.zeros(n)
    for k in range(1, 40):
        fk = k * f
        if fk > 6000:
            break
        g = sum(a / (1 + ((fk - fc) / bw) ** 2) for fc, a, bw in VOWELS[vowel])
        out += np.sin(k * phase + rng.uniform(0, 6.28)) * g / k ** 0.6
    breath = d.band(rng.standard_normal(n), 500, 3500) * 0.015
    return out + breath


def choir(freqs, n, rng, vowel, voices=4):
    return sum(choir_voice(f, n, rng, vowel, rng.uniform(-9, 9)) for f in freqs for _ in range(voices)) / voices


def tone(fc, n, rng, bright):
    """Saw-like additive tone with a pitch contour fc (array or float) and brightness contour (0..1)."""
    fc = np.broadcast_to(np.asarray(fc, dtype=float), (n,))
    phase = 2 * np.pi * np.cumsum(fc * d.drift(rng, n, 2, 0.0012)) / SR
    out = np.zeros(n)
    for k in range(1, 30):
        if k * fc.max() > 8000:
            break
        out += np.sin(k * phase + rng.uniform(0, 6.28)) * bright ** (k - 1) / k
    return out


def variant_a(seed=301):
    """A 'Chorhalle': choir D minor -> D major over a deep bell, long hall."""
    rng = np.random.default_rng(seed)
    dur = 4.6
    n = d.n_of(dur)
    buf = np.zeros(n)
    D2, D3, A3, D4, F4, FS4, A4, D5 = (hz(s) for s in (-31, -19, -12, -7, -4, -3, 0, 5))
    m1 = d.n_of(2.1)
    minor = choir((D3, A3, D4, F4, A4), m1, rng, "o") * d.curve([(0, 0), (0.35, 1), (0.8, 0.9), (1, 0)], m1) ** 1.2
    d.place(buf, 0.10 * minor, 0.05)
    m2 = d.n_of(dur - 1.75)
    major = choir((D3, A3, D4, FS4, A4, D5), m2, rng, "a") * d.curve([(0, 0), (0.12, 1), (0.5, 0.8), (1, 0)], m2) ** 1.3
    d.place(buf, 0.11 * major, 1.75)
    # deep bell at the start and on the resolution, a soft high bell over the major chord
    d.place(buf, 0.55 * bell(D2, d.n_of(dur), rng, decay=2.6), 0.0)
    d.place(buf, 0.35 * bell(D3, d.n_of(dur - 1.8), rng, decay=2.0), 1.8)
    d.place(buf, 0.10 * bell(D6, d.n_of(2.4), rng, decay=1.4), 1.85)
    t = np.arange(n) / SR
    sub = np.sin(2 * np.pi * D2 / 2 * t) * d.curve([(0, 0), (0.05, 1), (0.6, 0.6), (1, 0)], n)
    buf += 0.10 * sub
    hall = d.room(rng, buf, wet=0.5, rt60=3.2)
    return d.master(hall, -18.0, peak_db=-1.0, lp=11000, hp=30, fade_out=0.5)


def variant_b(seed=302):
    """B 'Kristall': amethyst chimes, the overtone series rising, echo shadows, lydian bell chord on top."""
    rng = np.random.default_rng(seed)
    dur = 4.3
    n = d.n_of(dur)
    buf = np.zeros(n)
    t = np.arange(n) / SR
    # overtone series of A2, one partial after another (2..16), pure sines swelling in and fading out together
    f0 = hz(-24)
    for k in range(1, 17):
        start = 0.05 + (k - 1) * 0.11
        m = n - d.n_of(start)
        env = d.curve([(0, 0), (0.12, 1), (0.55, 0.7), (1, 0)], m)
        part = np.sin(2 * np.pi * f0 * k * np.arange(m) / SR * (1 + 0.0007 * rng.standard_normal())) * env
        d.place(buf, 0.10 / k ** 0.45 * part, start)
    # chimes: short glassy bells from E lydian high up, dense in the middle
    LYD = [hz(s) for s in (19, 21, 23, 25, 26, 28, 31, 33, 35, 37, 38)]  # E6 F#6 G#6 A#6 B6 C#7 E7 ...
    chimes = np.zeros(n)
    for _ in range(34):
        at = 0.1 + rng.beta(2.0, 2.4) * 2.8
        f = LYD[rng.integers(len(LYD))]
        d.place(chimes, rng.uniform(0.04, 0.09) * bell(f, d.n_of(0.9), rng, decay=rng.uniform(0.12, 0.32)), at)
    shadow = np.zeros(n)
    for delay, g in ((0.29, 0.45), (0.58, 0.25), (0.87, 0.12)):
        d.place(shadow, g * d.low(chimes, 2600 - 600 * delay), delay)
    buf += chimes + shadow
    # crowning chord: E5 G#5 B5 D#6 + A#5 (lydian) as bells, and a low E body
    for f, w in ((hz(7), 0.16), (hz(11), 0.12), (hz(14), 0.12), (hz(13), 0.07), (hz(18), 0.10)):
        d.place(buf, w * bell(f, d.n_of(2.3), rng, decay=1.3), 1.95)
    d.place(buf, 0.18 * bell(hz(-17), d.n_of(2.3), rng, decay=1.6), 1.95)
    whoosh = d.band(rng.standard_normal(d.n_of(1.9)), 2000, 9000) * d.curve([(0, 0), (0.9, 1), (1, 0)], d.n_of(1.9)) ** 3
    d.place(buf, 0.05 * whoosh, 0.1)
    hall = d.room(rng, buf, wet=0.38, rt60=2.4)
    return d.master(hall, -18.0, peak_db=-1.0, lp=13000, hp=40, fade_out=0.45)


def variant_c(seed=303):
    """C 'Portal': dark beating drone and portal air resolving into a bright D major with bells."""
    rng = np.random.default_rng(seed)
    dur = 4.6
    n = d.n_of(dur)
    buf = np.zeros(n)
    t = np.arange(n) / SR
    turn = 2.0  # the resolution
    D1, D2, EB2, AB2, A2, D3, FS3, A3, D4, FS4, A4 = (hz(s) for s in (-43, -31, -30, -25, -24, -19, -15, -12, -7, -3, 0))
    bright = d.curve([(0, 0.25), (turn / dur, 0.55), ((turn + 0.4) / dur, 0.75), (1, 0.5)], n)
    drone = 0.6 * tone(D1, n, rng, bright * 0.8) + 0.5 * tone(D2, n, rng, bright)
    drone *= d.curve([(0, 0), (0.15, 1), (0.7, 0.8), (1, 0)], n)
    buf += 0.28 * drone
    # dissonant neighbours Eb2 / Ab2 that glide down/up into D / A at the turn
    g = np.clip((t - turn) / 0.35, 0, 1)
    g = 0.5 - 0.5 * np.cos(np.pi * g)
    for f_from, f_to, w in ((EB2, D2 * 2, 0.20), (AB2, A2, 0.20)):
        fc = f_from * (f_to / f_from) ** g
        buf += w * tone(fc, n, rng, bright * 0.9) * d.curve([(0, 0), (0.2, 1), (0.75, 0.7), (1, 0)], n)
    # portal air: band noise with a slow swirl (tremolo + moving band), fading out after the turn
    air = d.band(rng.standard_normal(n), 180, 1400)
    swirl = 0.6 + 0.4 * np.sin(2 * np.pi * np.cumsum(d.curve([(0, 3), (0.45, 8), (1, 2)], n)) / SR)
    air2 = d.band(rng.standard_normal(n), 2500, 7000) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.7 * t)) * 0.4
    buf += 0.10 * (air * swirl + air2) * d.curve([(0, 0), (0.3, 1), (turn / dur, 1), ((turn + 0.8) / dur, 0.15), (1, 0)], n)
    # eerie shimmer: inharmonic high sines that wobble
    for f in (hz(26.3), hz(30.7), hz(33.2)):
        wob = 1 + 0.01 * np.sin(2 * np.pi * rng.uniform(0.3, 0.9) * t + rng.uniform(0, 6.28))
        buf += 0.012 * np.sin(2 * np.pi * np.cumsum(f * wob) / SR) * d.curve([(0, 0), (0.35, 1), (turn / dur, 0.8), ((turn + 0.4) / dur, 0), (1, 0)], n)
    # the light: D major chord swells in at the turn, bells ring
    m = n - d.n_of(turn - 0.05)
    chord = sum(w * tone(f, m, rng, d.curve([(0, 0.3), (0.2, 0.7), (1, 0.45)], m)) for f, w in
                ((D3, 0.7), (FS3, 0.6), (A3, 0.6), (D4, 0.55), (FS4, 0.4), (A4, 0.35)))
    chord *= d.curve([(0, 0), (0.1, 1), (0.5, 0.75), (1, 0)], m) ** 1.2
    d.place(buf, 0.11 * chord, turn - 0.05)
    whoosh = d.band(rng.standard_normal(d.n_of(0.6)), 900, 7000) * d.curve([(0, 0), (0.9, 1), (1, 0)], d.n_of(0.6)) ** 2
    d.place(buf, 0.12 * whoosh, turn - 0.55)
    for f, w, at in ((D5, 0.16, turn), (A5, 0.10, turn + 0.12), (D6, 0.10, turn + 0.24), (FS5, 0.08, turn + 0.36)):
        d.place(buf, w * bell(f, d.n_of(dur - at), rng, decay=1.3), at)
    hall = d.room(rng, buf, wet=0.4, rt60=2.6)
    return d.master(hall, -18.0, peak_db=-1.0, lp=12000, hp=28, fade_out=0.5)


def write_variants(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for name, fn in (("A", variant_a), ("B", variant_b), ("C", variant_c)):
        x = fn()
        sf.write(os.path.join(out_dir, f"variant_{name}.wav"), x, SR, subtype="PCM_16")
        print(f"variant_{name}.wav {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS "
              f"rms {d.active_rms_db(x):.1f} dBFS")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--wav", help="also write WAV copies into this directory (previews)")
    ap.add_argument("--variants", help="only write the SP22-2 candidates A/B/C as WAV into this directory")
    args = ap.parse_args()
    if args.variants:
        write_variants(args.variants)
        return
    os.makedirs(OUT, exist_ok=True)
    for idx, (seed, pitch) in enumerate(((22, 1.0), (1022, 0.985)), start=1):
        x = make(seed, pitch)
        sf.write(os.path.join(OUT, f"legendary{idx}.ogg"), x, SR, format="OGG", subtype="VORBIS")
        if args.wav:
            os.makedirs(args.wav, exist_ok=True)
            sf.write(os.path.join(args.wav, f"legendary{idx}.wav"), x, SR, subtype="PCM_16")
        print(f"legendary{idx}.ogg {len(x) / SR:.2f}s peak {20 * np.log10(np.max(np.abs(x))):.1f} dBFS "
              f"rms {d.active_rms_db(x):.1f} dBFS")


if __name__ == "__main__":
    main()
