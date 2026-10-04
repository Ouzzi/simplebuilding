"""Usage: python tools/audio/import_discs.py [--source <folder>] [--ffmpeg <path>] [--dry-run]

Imports the owner's music for the eight music discs (docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md).

The owner drops MP3 or WAV files into the musik folder (default
C:\\Users\\o_o\\code\\minecraft-mods\\musik):

    end.mp3         -> Voidline              end_alt.mp3         -> Voidline (B-Side)
    overworld1.mp3  -> Driftwood             overworld1_alt.mp3  -> Driftwood (B-Side)
    overworld2.mp3  -> Daybreak              overworld2_alt.mp3  -> Daybreak (B-Side)
    nether.mp3      -> Brimstone             nether_alt.mp3      -> Brimstone (B-Side)

(.wav, .flac and .ogg work as well.) Every file found is converted to mono Ogg Vorbis (~quality 5,
44.1 kHz - mono, so the jukebox plays it positionally), written to
mc26_3/overlay/resources/assets/simplebuilding/sounds/records/<song>.ogg, and its length (rounded up
to whole seconds, so the jukebox never stops before the end) goes into the song table
MusicDiscs.SONGS (common/src/shared/java/com/simplebuilding/util/MusicDiscs.java, the datagen source)
and into the generated mc26_3/generated/data/simplebuilding/jukebox_song/<song>.json, so it works
before the next datagen run as well. Missing files are skipped: that disc keeps its placeholder.

Encoder: ffmpeg (--ffmpeg, the FFMPEG environment variable, or ffmpeg on PATH); without ffmpeg the
Python package soundfile (libsndfile 1.1+ reads MP3/WAV and writes Vorbis). Without either the
script says how to install one and changes nothing. After importing: run the datagen once
(gradlew :mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263), the game tests, and commit the
.ogg files, MusicDiscs.java and the .json files.
"""
import argparse
import json
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from discs import DEFAULT_SOURCE, MUSIC_DISCS_JAVA, all_tracks, ogg_path, song_json  # noqa: E402

EXTENSIONS = (".mp3", ".wav", ".flac", ".ogg")
SAMPLE_RATE = 44100


def find_ffmpeg(explicit):
    for candidate in (explicit, os.environ.get("FFMPEG"), shutil.which("ffmpeg"),
                      r"C:\ProgramData\ffmpeg\bin\ffmpeg.exe"):
        if candidate and os.path.isfile(candidate):
            return candidate
    return None


def soundfile_module():
    try:
        import soundfile
        return soundfile
    except ImportError:
        return None


def find_source(folder, stem):
    for ext in EXTENSIONS:
        path = os.path.join(folder, stem + ext)
        if os.path.isfile(path):
            return path
    return None


def convert_ffmpeg(ffmpeg, src, dst):
    subprocess.run([ffmpeg, "-y", "-loglevel", "error", "-i", src, "-vn", "-ac", "1", "-ar", str(SAMPLE_RATE),
                    "-c:a", "libvorbis", "-q:a", "5", dst], check=True)


def convert_soundfile(sf, src, dst):
    import numpy as np
    data, rate = sf.read(src, always_2d=True, dtype="float32")
    mono = data.mean(axis=1)
    sf.write(dst, mono, rate, format="OGG", subtype="VORBIS", compression_level=0.5)


def duration(sf, ffmpeg, path):
    if sf is not None:
        info = sf.info(path)
        return info.frames / info.samplerate
    probe = os.path.join(os.path.dirname(ffmpeg), "ffprobe" + (".exe" if ffmpeg.endswith(".exe") else ""))
    out = subprocess.run([probe, "-v", "error", "-show_entries", "format=duration", "-of", "default=nw=1:nk=1", path],
                         check=True, capture_output=True, text=True).stdout.strip()
    return float(out)


SONG_LINE = r'(new Song\("{name}", "[^"]+", \d+, )([0-9.]+)F, ([0-9.]+)F\)'


def set_java_length(song, seconds, dry_run):
    """Rewrites the A- or B-side length in the line new Song("<name>", "<stem>", <cmp>, <a>F, <b>F)."""
    b_side = song.endswith("_b_side")
    name = song[:-len("_b_side")] if b_side else song
    with open(MUSIC_DISCS_JAVA, encoding="utf-8", newline="") as f:
        text = f.read()
    match = re.search(SONG_LINE.format(name=re.escape(name)), text)
    if not match:
        raise SystemExit(f"MusicDiscs.java: no Song line for {name} - update SONG_LINE in import_discs.py")
    old = match.group(3) if b_side else match.group(2)
    a, b = (match.group(2), str(float(seconds))) if b_side else (str(float(seconds)), match.group(3))
    text = text[:match.start()] + f"{match.group(1)}{a}F, {b}F)" + text[match.end():]
    if not dry_run:
        with open(MUSIC_DISCS_JAVA, "w", encoding="utf-8", newline="") as f:
            f.write(text)
    return float(old)


def set_length(song, seconds, dry_run):
    old = set_java_length(song, seconds, dry_run)
    path = song_json(song)
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        data["length_in_seconds"] = float(seconds)
        if not dry_run:
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                json.dump(data, f, indent=2)
                f.write("\n")
    else:
        print(f"  ({os.path.relpath(path)} not generated yet - run the datagen)")
    return old


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--source", default=DEFAULT_SOURCE, help="folder with end.mp3, overworld1.mp3, ...")
    parser.add_argument("--ffmpeg", help="path to ffmpeg(.exe)")
    parser.add_argument("--dry-run", action="store_true", help="only show what would happen")
    args = parser.parse_args()

    if not os.path.isdir(args.source):
        print(f"Source folder not found: {args.source}\nCreate it and put end.mp3, overworld1.mp3, overworld2.mp3, nether.mp3 "
              "(and optional *_alt.mp3) into it.")
        return 1
    jobs = [(song, find_source(args.source, stem), stem) for song, stem in all_tracks()]
    found = [(song, src) for song, src, _ in jobs if src]
    for song, src, stem in jobs:
        if not src:
            print(f"skip {song:18} ({stem}.mp3/.wav not found - placeholder stays)")
    if not found:
        print("Nothing to import.")
        return 0

    ffmpeg = find_ffmpeg(args.ffmpeg)
    sf = soundfile_module()
    if ffmpeg is None and sf is None:
        print("Neither ffmpeg nor the Python package soundfile is available.\n"
              "Install one of them, then run this script again:\n"
              "  winget install Gyan.FFmpeg        (then reopen the terminal, or pass --ffmpeg <path>)\n"
              "  python -m pip install soundfile   (libsndfile with MP3 and Vorbis)")
        return 2
    print(f"encoder: {'ffmpeg ' + ffmpeg if ffmpeg else 'soundfile ' + sf.__version__}")

    for song, src in found:
        dst = ogg_path(song)
        if args.dry_run:
            print(f"would convert {src} -> {dst}")
            continue
        with tempfile.TemporaryDirectory() as tmp:
            out = os.path.join(tmp, song + ".ogg")
            if ffmpeg:
                convert_ffmpeg(ffmpeg, src, out)
            else:
                convert_soundfile(sf, src, out)
            seconds = math.ceil(duration(sf, ffmpeg, out))
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copyfile(out, dst)
        old = set_length(song, seconds, args.dry_run)
        print(f"{song:18} {os.path.basename(src)} -> {os.path.relpath(dst)}  length {old} -> {float(seconds)} s")
    return 0


if __name__ == "__main__":
    sys.exit(main())
