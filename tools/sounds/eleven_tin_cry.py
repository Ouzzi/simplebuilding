"""N25 (b-metals25, owner 10.11.: "Zinn (Zinngeschrei!)"): the tin cry takes of Simple Ores.

Tin crackles when a bar is bent (the "tin cry"). Three ElevenLabs sound-generation candidates (0.8 s, prompt below, prompt influence
0.6) were made on 10.11.2026; candidates 2 and 3 have the crackle right at the start (1 had a slow start). This script turns them into
the committed mono Ogg Vorbis takes modules/simpleores/shared/resources/assets/simpleores/sounds/block/tin/cry<n>.ogg: mono, 44.1 kHz,
-4 dB (quiet - it mixes into the tin break/place sounds, see tools/gen_data.py SOUNDS), trailing silence cut. Provenance: CREDITS.md.

Prompt: "tin cry: a short crisp crackling creak of a pure tin bar being bent, fine metallic crunching, close, dry, no music, no reverb"

Run: python3 tools/sounds/eleven_tin_cry.py --src DIR   (DIR holds tincry2.mp3, tincry3.mp3; needs ffmpeg)
"""
import argparse
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/sounds/block/tin'
PICK = ('tincry2', 'tincry3')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--src', required=True)
    a = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    for n, name in enumerate(PICK, 1):
        out = OUT / f'cry{n}.ogg'
        af = 'volume=-4dB,areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse'
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(Path(a.src) / f'{name}.mp3'), '-af', af, '-ac', '1', '-ar', '44100',
                        '-c:a', 'libvorbis', '-q:a', '3', str(out)], check=True, stdin=subprocess.DEVNULL)
        print(out.name, out.stat().st_size // 1024, 'KiB')


if __name__ == '__main__':
    main()
