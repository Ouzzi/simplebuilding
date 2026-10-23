"""Hollow Howler voice, mode "eleven" (W24-3, owner 23.10.; docs/ai/PLAN-A-SOUND24-2026-10-23.md).

The owner took the recommended ElevenLabs candidates of a-howler23 (/root/sb-tools-a/eleven23/howler/, LISTE.md):
idle = h_ambient_3, howl = h_roar_1, rage = h_rage_1 (ambient_rage and roar_rage), hurt = h_hurt_2, death = h_death_2.
sounds.json keeps fixed take names for every mode (3-5 takes per event), so every take of an event is the one chosen
candidate with a small pitch step like Vanilla's takes (1.0 / 0.94 / 1.05 / 0.97, resampled like the game's pitch);
ambient_rage 1.5 dB quieter (today's level of that event). Output: mono Ogg Vorbis into
modules/simplemobs/shared/howler-eleven/<event><n>.ogg (committed, outside the resources); the Gradle script
gradle/module-vanilla-derived-sounds.gradle copies them over variants A/B when howler_sounds.json says "eleven".

Run: python3 tools/sounds/eleven_howler_takes.py --src /root/sb-tools-a/eleven23/howler/ogg   (needs ffmpeg)
"""
import argparse
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'modules/simplemobs/shared/howler-eleven'
SOUNDS = ROOT / 'modules/simplemobs/shared/resources/assets/simplemobs/sounds.json'
MAPPING = Path(__file__).resolve().parent / 'howler_sounds.json'
PITCH = (1.0, 0.94, 1.05, 0.97, 1.02)
GAIN_DB = {'ambient_rage': -1.5}


def takes():
    """{event: number of takes} from sounds.json for the events howler_sounds.json maps in "eleven"."""
    eleven = json.loads(MAPPING.read_text(encoding='utf-8'))['eleven']
    snd = json.loads(SOUNDS.read_text(encoding='utf-8'))
    return {e: len(snd['entity.hollow_howler.' + e]['sounds']) for e in eleven}, eleven


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--src', required=True, help='folder with the ElevenLabs candidates (h_*.ogg)')
    a = ap.parse_args()
    counts, eleven = takes()
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob('*.ogg'):
        old.unlink()
    for event, n in counts.items():
        src = Path(a.src) / (eleven[event] + '.ogg')
        for i in range(n):
            out = OUT / f'{event}{i + 1}.ogg'
            af = f'asetrate={44100 * PITCH[i]:.0f},aresample=44100,volume={GAIN_DB.get(event, 0.0)}dB'
            subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(src), '-af', af, '-ac', '1', '-c:a', 'libvorbis',
                            '-q:a', '3', str(out)], check=True, stdin=subprocess.DEVNULL)
            print(f'{out.name}: {eleven[event]} pitch {PITCH[i]} {out.stat().st_size // 1024} KiB')


if __name__ == '__main__':
    main()
