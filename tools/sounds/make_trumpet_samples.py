"""Trumpet samples (TROMP-3): the two Ogg files simplemobs/sounds/item/trumpet/{low,high}.ogg from the owner's tone K1
(ElevenLabs take "K1 gleichmaessig, hell" in the 03.11. listening page, embedded there as MP3 "s1"; 0.5-1.8 s is the steady part).
K1 sounds at ~494.8 Hz. The low sample is retuned to note 6, the high to note 18 (note n = 247 Hz * 2^(n/12), n 0..24, two octaves),
so the game's pitch 2^((n-ref)/12) spans an octave around its reference. TROMP-3 (05.11.): the samples are seamless LOOPS (the client plays
them as a looping sound instance while the use key is held): no attack, no release, 0.15 s equal-power crossfade of tail into head at the loop point.
Usage: python3 tools/sounds/make_trumpet_samples.py <k1.wav, mono 44.1 kHz>   (needs numpy and ffmpeg with libvorbis)
   or: python3 tools/sounds/make_trumpet_samples.py --relop   (re-makes both loops from the steady middle of the existing low/high.ogg when K1 is not at hand)"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

OUT = Path(__file__).resolve().parents[2] / 'modules/simplemobs/shared/resources/assets/simplemobs/sounds/item/trumpet'
F_SOURCE, F_BASE = 494.8, 247.0


def loop(y, sr, xf=.15):
    """Seamless loop of y: the last xf seconds are crossfaded into the first xf seconds (equal power), then cut off."""
    c = int(xf * sr)
    ramp = np.sin(np.linspace(0, np.pi / 2, c))
    return np.concatenate([y[c:len(y) - c], y[len(y) - c:] * ramp[::-1] + y[:c] * ramp])


def make(x, sr, ref_note, name):
    ratio = F_BASE * 2 ** (ref_note / 12) / F_SOURCE
    seg = x[int(.5 * sr):int(1.8 * sr)]
    y = np.interp(np.arange(int(len(seg) / ratio)) * ratio, np.arange(len(seg)), seg)
    write(loop(y, sr), sr, name)


def write(y, sr, name):
    y *= 0.85 / np.max(np.abs(y))
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f'{name}.wav'
        with wave.open(str(wav), 'wb') as w:
            w.setnchannels(1), w.setsampwidth(2), w.setframerate(sr)
            w.writeframes((y * 32767).astype(np.int16).tobytes())
        OUT.mkdir(parents=True, exist_ok=True)
        subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', str(wav), '-ac', '1', '-c:a', 'libvorbis', '-q:a', '5', str(OUT / f'{name}.ogg')], check=True)


def relop(name):
    """Loop from the steady middle of the existing <name>.ogg: cut a whole number of periods and tile them to ~1 s (seamless)."""
    src = OUT / f'{name}.ogg'
    raw = subprocess.run(['ffmpeg', '-loglevel', 'error', '-i', str(src), '-ac', '1', '-ar', '44100', '-f', 's16le', '-'], check=True, capture_output=True).stdout
    sr = 44100
    x = np.frombuffer(raw, dtype=np.int16).astype(float) / 32768
    mid = x[int(.14 * sr):int(.36 * sr)]
    ac = np.correlate(mid, mid, 'full')[len(mid) - 1:]
    lo, hi = int(sr / 1000), int(sr / 150)
    period = lo + int(np.argmax(ac[lo:hi]))
    k = period  # parabolic refinement of the autocorrelation peak
    d = ac[k - 1] - 2 * ac[k] + ac[k + 1]
    period = k + (0.5 * (ac[k - 1] - ac[k + 1]) / d if d else 0.0)
    f0 = sr / period
    n = max(1, int(round(.2 * f0)))  # whole periods in the cut
    cut_len = n * period
    t = np.arange(int(round(cut_len * round(1.0 * sr / cut_len)))) % cut_len
    y = np.interp(t, np.arange(len(mid)), mid)
    write(y, sr, name)


if __name__ == '__main__':
    if sys.argv[1] == '--relop':
        for nm in ('low', 'high'): relop(nm)
        sys.exit()
    with wave.open(sys.argv[1]) as w:
        sr = w.getframerate()
        x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(float) / 32768
    make(x, sr, 6, 'low')
    make(x, sr, 18, 'high')
