"""Trumpet samples (TROMP-3): the two Ogg files simplemobs/sounds/item/trumpet/{low,high}.ogg from the owner's tone K1
(ElevenLabs take "K1 gleichmaessig, hell" in the 03.11. listening page, embedded there as MP3 "s1"; 0.5-1.8 s is the steady part).
K1 sounds at ~494.8 Hz. The low sample is retuned to note 6, the high to note 18 (note n = 247 Hz * 2^(n/12), n 0..24, two octaves),
so the game's pitch 2^((n-ref)/12) spans an octave around its reference. 0.5 s each, 0.12 s equal-power fades, no attack, no release.
Usage: python3 tools/sounds/make_trumpet_samples.py <k1.wav, mono 44.1 kHz>   (needs numpy and ffmpeg with libvorbis)"""
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

import numpy as np

OUT = Path(__file__).resolve().parents[2] / 'modules/simplemobs/shared/resources/assets/simplemobs/sounds/item/trumpet'
F_SOURCE, F_BASE = 494.8, 247.0


def make(x, sr, ref_note, name):
    ratio = F_BASE * 2 ** (ref_note / 12) / F_SOURCE
    seg = x[int(.5 * sr):int(1.8 * sr)]
    y = np.interp(np.arange(int(len(seg) / ratio)) * ratio, np.arange(len(seg)), seg)
    n = int(.5 * sr)
    start = (len(y) - n) // 2
    y = y[start:start + n].copy()
    fade = int(.12 * sr)
    ramp = np.sin(np.linspace(0, np.pi / 2, fade))
    y[:fade] *= ramp
    y[-fade:] *= ramp[::-1]
    y *= 0.85 / np.max(np.abs(y))
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / f'{name}.wav'
        with wave.open(str(wav), 'wb') as w:
            w.setnchannels(1), w.setsampwidth(2), w.setframerate(sr)
            w.writeframes((y * 32767).astype(np.int16).tobytes())
        OUT.mkdir(parents=True, exist_ok=True)
        subprocess.run(['ffmpeg', '-loglevel', 'error', '-y', '-i', str(wav), '-ac', '1', '-c:a', 'libvorbis', '-q:a', '5', str(OUT / f'{name}.ogg')], check=True)


if __name__ == '__main__':
    with wave.open(sys.argv[1]) as w:
        sr = w.getframerate()
        x = np.frombuffer(w.readframes(w.getnframes()), dtype=np.int16).astype(float) / 32768
    make(x, sr, 6, 'low')
    make(x, sr, 18, 'high')
