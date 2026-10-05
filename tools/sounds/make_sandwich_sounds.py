"""Synthesises the Simple Sandwiches knife sounds: cutting butter and cutting cheese.

Usage: python tools/sounds/make_sandwich_sounds.py   (Python with numpy, scipy and soundfile)

Everything is generated here from noise and oscillators - no samples, no third-party recordings.
- butter_cut1..3: soft, smooth glide (low-passed swish) with a small sticky smack at the end.
- cheese_cut1..3: firmer, drier cut (band-passed scrape) ending in a light wooden tap on the board.
Three variants each (different seeds and pitch) so repeated cuts do not sound identical.
Output: mono 44.1 kHz Ogg Vorbis (libsndfile) into the module's sounds/block folder; the events are
declared in modules/simplesandwiches/generated/resources/assets/simplesandwiches/sounds.json
(written by modules/simplesandwiches/tools/gen_resources.py).
"""
import os

import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt

SR = 44100
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(REPO, "modules", "simplesandwiches", "shared", "resources", "assets", "simplesandwiches",
                   "sounds", "block")


def band(x, lo, hi, order=3):
    return sosfilt(butter(order, [lo, hi], btype="band", fs=SR, output="sos"), x)


def low(x, hi, order=3):
    return sosfilt(butter(order, hi, btype="low", fs=SR, output="sos"), x)


def envelope(n, attack, release_power):
    t = np.linspace(0, 1, n)
    rise = np.clip(t / max(attack, 1e-4), 0, 1)
    return rise * (1 - t) ** release_power


def butter_cut(seed, pitch):
    rng = np.random.default_rng(seed)
    dur = 0.38
    n = int(SR * dur)
    t = np.arange(n) / SR
    # glide: soft noise, its brightness falls as the knife sinks in
    noise = rng.standard_normal(n)
    glide = low(noise, 1800 * pitch) * 0.6 + band(noise, 400 * pitch, 1100 * pitch) * 0.8
    glide *= envelope(n, 0.18, 1.6) * (0.85 + 0.15 * np.sin(2 * np.pi * 7 * t + rng.uniform(0, 6.3)))
    # sticky smack near the end: a short, low, damped blip
    smack = np.zeros(n)
    i0 = int(n * rng.uniform(0.62, 0.7))
    m = int(0.06 * SR)
    tt = np.arange(m) / SR
    f = 180 * pitch * (1 - 0.35 * tt / tt[-1])
    smack[i0:i0 + m] = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 55) * 0.55
    smack[i0:i0 + m] += low(rng.standard_normal(m), 900 * pitch) * np.exp(-tt * 80) * 0.4
    return glide + smack


def cheese_cut(seed, pitch):
    rng = np.random.default_rng(seed)
    dur = 0.32
    n = int(SR * dur)
    # scrape: drier and brighter, with small grains (the firm cheese resisting the blade)
    noise = rng.standard_normal(n)
    grains = np.zeros(n)
    for _ in range(45):
        i = int(rng.beta(1.4, 2.2) * (n - 400))
        k = int(rng.uniform(80, 300))
        grains[i:i + k] += rng.standard_normal(k) * np.exp(-np.linspace(0, 5, k)) * rng.uniform(0.3, 1.0)
    scrape = band(noise, 900 * pitch, 3400 * pitch) * 0.45 + band(grains, 1200 * pitch, 5200 * pitch) * 0.7
    scrape *= envelope(n, 0.08, 1.2)
    # tap: the blade reaching the board, a short wooden knock
    tap = np.zeros(n)
    i0 = int(n * rng.uniform(0.7, 0.76))
    m = int(0.05 * SR)
    tt = np.arange(m) / SR
    knock = (np.sin(2 * np.pi * 420 * pitch * tt) + 0.5 * np.sin(2 * np.pi * 980 * pitch * tt)) * np.exp(-tt * 90)
    tap[i0:i0 + m] = knock * 0.5 + band(rng.standard_normal(m), 600, 2500) * np.exp(-tt * 140) * 0.35
    return scrape + tap


def finish(x, peak):
    fade = int(0.01 * SR)
    x[:fade] *= np.linspace(0, 1, fade)
    x[-fade:] *= np.linspace(1, 0, fade)
    return (x / np.max(np.abs(x)) * peak).astype(np.float32)


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, (seed, pitch) in enumerate(((11, 1.0), (12, 0.92), (13, 1.08)), start=1):
        sf.write(os.path.join(OUT, f"butter_cut{i}.ogg"), finish(butter_cut(seed, pitch), 0.7), SR, format="OGG",
                 subtype="VORBIS")
        sf.write(os.path.join(OUT, f"cheese_cut{i}.ogg"), finish(cheese_cut(seed + 10, pitch), 0.75), SR, format="OGG",
                 subtype="VORBIS")
    print("written:", OUT)


if __name__ == "__main__":
    main()
