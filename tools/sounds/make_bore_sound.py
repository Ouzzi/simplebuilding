"""Synthesises the piston bore sound: a short grinding crunch, ~0.4 s, mono.

Three variants (different random seeds and slightly different pitch) so repeated bores do not
sound identical. Everything is generated here from noise and oscillators - no samples.
Run with the Python that has numpy/scipy/soundfile (Python 3.10 on this machine); needs ffmpeg
for the Vorbis encode.
"""
import os
import subprocess
import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt

SR = 44100
DUR = 0.42
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FFMPEG = os.environ.get("FFMPEG", r"C:\ProgramData\ffmpeg\bin\ffmpeg.exe")
#: Both resource trees carry the same files (26.2/26.3/26.4 and 1.21.11).
TARGETS = [os.path.join(REPO, "src/main/resources/assets/simplebuilding/sounds/block/piston"),
           os.path.join(REPO, "mc1_21_11/fabric/src/main/resources/assets/simplebuilding/sounds/block/piston")]
OUT = os.path.join(REPO, "build", "bore_sound")
os.makedirs(OUT, exist_ok=True)


def bandpass(x, lo, hi, order=4):
    sos = butter(order, [lo, hi], btype="band", fs=SR, output="sos")
    return sosfilt(sos, x)


def lowpass(x, hi, order=4):
    sos = butter(order, hi, btype="low", fs=SR, output="sos")
    return sosfilt(sos, x)


def make(seed, pitch):
    rng = np.random.default_rng(seed)
    n = int(SR * DUR)
    t = np.arange(n) / SR

    # 1) Crunch: many tiny grains of band-passed noise, dense at the start, thinning out.
    crunch = np.zeros(n)
    grains = 140
    for _ in range(grains):
        start = rng.beta(1.3, 3.2) * (DUR - 0.03)
        length = rng.uniform(0.003, 0.018)
        i0 = int(start * SR)
        i1 = min(n, i0 + int(length * SR))
        g = rng.standard_normal(i1 - i0)
        env = np.exp(-np.linspace(0, rng.uniform(3, 7), i1 - i0))
        crunch[i0:i1] += g * env * rng.uniform(0.3, 1.0)
    crunch = bandpass(crunch, 350 * pitch, 4200 * pitch)

    # 2) Grind: a rough low saw with jittering frequency, gated by a ~28 Hz rasp.
    f0 = 78 * pitch * (1 + 0.06 * np.cumsum(rng.standard_normal(n)) / np.sqrt(n))
    phase = np.cumsum(f0) / SR
    saw = 2 * (phase - np.floor(phase + 0.5))
    rasp = 0.55 + 0.45 * np.sign(np.sin(2 * np.pi * 28 * pitch * t + rng.uniform(0, 6.28)))
    grind = lowpass(saw * rasp, 900 * pitch)

    # 3) Thump: the first contact, a falling sine with a click.
    thump_f = 95 * pitch * np.exp(-t * 9)
    thump = np.sin(2 * np.pi * np.cumsum(thump_f) / SR) * np.exp(-t * 28)

    env = np.minimum(1.0, t / 0.004) * np.exp(-t * 6.5)
    tail = np.clip((DUR - t) / 0.06, 0, 1)
    mix = (0.95 * crunch / (np.max(np.abs(crunch)) + 1e-9)
           + 0.45 * grind / (np.max(np.abs(grind)) + 1e-9)
           + 0.7 * thump) * env * tail
    mix = np.tanh(1.6 * mix)  # a little saturation makes it gritty
    mix /= np.max(np.abs(mix)) + 1e-9
    return (mix * 0.89).astype(np.float32)


variants = [(11, 1.0), (23, 0.93), (37, 1.07)]
for idx, (seed, pitch) in enumerate(variants, start=1):
    wav = os.path.join(OUT, f"bore{idx}.wav")
    ogg = os.path.join(OUT, f"bore{idx}.ogg")
    sf.write(wav, make(seed, pitch), SR, subtype="PCM_16")
    subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5", ogg], check=True)
    info = sf.info(ogg)
    print(ogg, info.format, info.subtype, info.channels, info.samplerate, round(info.duration, 3), os.path.getsize(ogg))
    for target in TARGETS:
        os.makedirs(target, exist_ok=True)
        with open(ogg, "rb") as src, open(os.path.join(target, f"bore{idx}.ogg"), "wb") as dst:
            dst.write(src.read())
