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


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--wav", help="also write WAV copies into this directory (previews)")
    args = ap.parse_args()
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
