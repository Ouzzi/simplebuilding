"""Simple Dinos voices (PLAN40 simpledinos-3, W2C): turns ElevenLabs sound-generation candidates into the committed takes.

Two candidates per event (<kind>_<event>_<1|2>.mp3, prompts: /root/sb-tools-b/w2c-dino-sounds/gen.sh, model eleven_text_to_sound_v2,
prompt influence 0.5) become take 1 and 2 of the event: mono 44.1 kHz Ogg Vorbis q3, trailing silence cut, loudness -18 LUFS.
Output: modules/simpledinos/shared/resources/assets/simpledinos/sounds/entity/<kind>/<event><n>.ogg and sounds.json (subtitles
subtitles.simpledinos.entity.<kind>.<event> live in the lang files). Provenance: sounds/CREDITS.md.

Run: python3 tools/sounds/eleven_dino_voices.py --src DIR   (needs ffmpeg)
"""
import argparse
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'modules/simpledinos/shared/resources/assets/simpledinos'
EVENTS = {'trex': ['ambient', 'hurt', 'death', 'roar'], 'triceratops': ['ambient', 'hurt', 'death', 'angry'],
          'stegosaurus': ['ambient', 'hurt', 'death'], 'parasaurolophus': ['ambient', 'hurt', 'death']}
TAKES = 2


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--src', required=True)
    a = ap.parse_args()
    sounds = {}
    for kind, events in EVENTS.items():
        out_dir = ASSETS / 'sounds/entity' / kind
        out_dir.mkdir(parents=True, exist_ok=True)
        for event in events:
            names = []
            for n in range(1, TAKES + 1):
                out = out_dir / f'{event}{n}.ogg'
                af = 'areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse,loudnorm=I=-18:TP=-2:LRA=7'
                subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(Path(a.src) / f'{kind}_{event}_{n}.mp3'), '-af', af, '-ac', '1',
                                '-ar', '44100', '-c:a', 'libvorbis', '-q:a', '3', str(out)], check=True, stdin=subprocess.DEVNULL)
                names.append(f'simpledinos:entity/{kind}/{event}{n}')
                print(out.name, out.stat().st_size // 1024, 'KiB')
            sounds[f'entity.{kind}.{event}'] = {'sounds': names, 'subtitle': f'subtitles.simpledinos.entity.{kind}.{event}'}
    (ASSETS / 'sounds.json').write_text(json.dumps(sounds, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
