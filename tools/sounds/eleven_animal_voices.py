"""Simple Animals voices (PLAN40 simpleanimals-2, W4A): turns ElevenLabs sound-generation candidates into the committed takes.

Two candidates per event (<kind>_<event>_<1|2>.mp3, model eleven_text_to_sound_v2, prompt influence 0.5, duration set per event) become
take 1 and 2 of the event: mono 44.1 kHz Ogg Vorbis q3, trailing silence cut, 5 ms fade in and 20 ms fade out. Output:
modules/simpleanimals/shared/resources/assets/simpleanimals/sounds/entity/<kind>/<event><n>.ogg and sounds.json (subtitles
subtitles.simpleanimals.entity.<kind>.<event> live in the lang files). Provenance: CREDITS.md.

Loudness differs from tools/sounds/eleven_dino_voices.py on purpose: the hedgehog clips are only 0.3 to 0.8 s long, and ffmpeg's loudnorm
cannot measure clips that short (it clips them or leaves them far too quiet). So the gain is set from the mean level (volumedetect) of the
trimmed clip: deer -18.5 dB, hedgehog -24 dB (a tiny, quiet animal; the Dino takes sit at -17 to -20 dB), never above -3 dBFS peak.

Prompts (each ends in "close, dry, no music, no background noise"; seconds in brackets):
  deer ambient (1.5)    red deer short low bellow, a deep soft guttural grunt-groan, calm, a single call
  deer hurt (1.0)       deer short startled pained bark-bleat, abrupt, a single call
  deer death (2.0)      deer dying, a weak pained bleat fading into a low groan and a soft exhale
  hedgehog ambient (0.8) tiny hedgehog soft snuffling snort with a quiet little squeak, very small and gentle
  hedgehog hurt (0.5)   tiny hedgehog short high squeal, small and sharp
  hedgehog death (0.8)  tiny hedgehog dying, a small squeal fading into a weak breathy whimper
  hedgehog curl (0.6)   hedgehog rolling into a ball, soft quick rustling of spines with a small huff, short
  hedgehog uncurl (0.5) hedgehog unrolling, soft quick rustling of spines with a small relaxed sniff, short

Run: python3 tools/sounds/eleven_animal_voices.py --src DIR   (needs ffmpeg; DIR holds the candidate mp3 files)
"""
import argparse
import json
import re
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'modules/simpleanimals/shared/resources/assets/simpleanimals'
EVENTS = {'deer': ['ambient', 'hurt', 'death'], 'hedgehog': ['ambient', 'hurt', 'death', 'curl', 'uncurl']}
MEAN_DB = {'deer': -18.5, 'hedgehog': -24.0}
PEAK_DB = -3.0
TAKES = 2
TRIM = 'areverse,silenceremove=start_periods=1:start_threshold=-50dB,areverse'


def ffmpeg(*args):
    return subprocess.run(['ffmpeg', '-nostats', '-v', 'info', '-y', *args], check=True, stdin=subprocess.DEVNULL,
                          capture_output=True, text=True).stderr


def convert(src, out, mean_target):
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / 'trim.wav'
        ffmpeg('-i', str(src), '-af', TRIM, '-ac', '1', '-ar', '44100', str(wav))
        log = ffmpeg('-i', str(wav), '-af', 'volumedetect', '-f', 'null', '-')
        mean = float(re.search(r'mean_volume: (-?[\d.]+) dB', log).group(1))
        peak = float(re.search(r'max_volume: (-?[\d.]+) dB', log).group(1))
        gain = min(mean_target - mean, PEAK_DB - peak)
        dur = float(subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', str(wav)],
                                   check=True, capture_output=True, text=True, stdin=subprocess.DEVNULL).stdout)
        af = f'volume={gain:.2f}dB,afade=t=in:d=0.005,afade=t=out:st={max(dur - 0.02, 0):.3f}:d=0.02'
        ffmpeg('-i', str(wav), '-af', af, '-ac', '1', '-ar', '44100', '-c:a', 'libvorbis', '-q:a', '3', str(out))
    return gain


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
                gain = convert(Path(a.src) / f'{kind}_{event}_{n}.mp3', out, MEAN_DB[kind])
                names.append(f'simpleanimals:entity/{kind}/{event}{n}')
                print(f'{kind}/{out.name}', out.stat().st_size // 1024, 'KiB, gain', f'{gain:+.1f} dB')
            sounds[f'entity.{kind}.{event}'] = {'sounds': names, 'subtitle': f'subtitles.simpleanimals.entity.{kind}.{event}'}
    (ASSETS / 'sounds.json').write_text(json.dumps(sounds, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
