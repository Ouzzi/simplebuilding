"""Usage: python tools/audio/make_placeholder_discs.py [--force]

Since 2026-10-04 the discs carry the owner's real music; existing files are kept unless --force.

Writes the placeholder tracks of the eight music discs (owner 2026-10-03) until the owner's real
music arrives (tools/audio/import_discs.py replaces them): 3 s, mono, 44.1 kHz Ogg Vorbis, quiet
(-18 dBFS) - three beeps at a pitch of its own per disc, rising on the A-side and falling on the
B-side, so nobody mistakes them for music. The song lengths (3 s) live in MusicDiscs.SONGS; the
jukebox_song JSONs come from the datagen.

Needs the Python package soundfile (libsndfile with Vorbis); no ffmpeg required.
"""
import os
import sys

import numpy as np
import soundfile as sf

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from discs import RECORDS, SONGS, ogg_path  # noqa: E402

SR = 44100
LENGTH = 3.0
AMPLITUDE = 0.125  # about -18 dBFS
#: Base pitch per disc (Hz): End, Overworld 1, Overworld 2, Nether.
PITCH = {"voidline": 330.0, "driftwood": 262.0, "daybreak": 392.0, "brimstone": 220.0}


def beep(freq, dur):
    t = np.arange(int(SR * dur)) / SR
    tone = np.sin(2 * np.pi * freq * t) + 0.25 * np.sin(4 * np.pi * freq * t)
    fade = int(SR * 0.012)
    env = np.ones_like(t)
    env[:fade] = np.linspace(0, 1, fade)
    env[-fade:] = np.linspace(1, 0, fade)
    return tone * env / 1.25


def track(base, b_side):
    steps = [1.0, 1.25, 1.5]
    if b_side:
        steps = steps[::-1]
    out = np.zeros(int(SR * LENGTH))
    pos = int(SR * 0.3)
    for step in steps:
        b = beep(base * step, 0.28)
        out[pos:pos + len(b)] += b
        pos += len(b) + int(SR * 0.22)
    return (out * AMPLITUDE).astype(np.float32)


def main():
    os.makedirs(RECORDS, exist_ok=True)
    force = "--force" in sys.argv
    for song, _, _ in SONGS:
        for name, b_side in ((song, False), (song + "_b_side", True)):
            path = ogg_path(name)
            # Echte Musik (seit 2026-10-04 importiert) nie ueberschreiben, nur fehlende Dateien oder mit --force.
            if os.path.exists(path) and not force:
                print(f"{name}: kept ({os.path.getsize(path) // 1024} KiB) - use --force to replace it with a placeholder")
                continue
            sf.write(path, track(PITCH[song], b_side), SR, format="OGG", subtype="VORBIS", compression_level=0.5)
            print(f"{name}: placeholder written")


if __name__ == "__main__":
    main()
