"""Usage: python tools/audio/import_discs.py [--source <folder>] [--ffmpeg <path>] [--dry-run]

Imports the owner's music for the music discs (docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md).

The owner drops MP3 or WAV files into the musik folder (default
C:\\Users\\o_o\\code\\minecraft-mods\\musik), up to four tracks per disc:

    end.mp3         Voidline   track 1     end_alt.mp3         track 2 (B-side)
    end_3.mp3       Voidline   track 3     end_4.mp3           track 4
    overworld1*.mp3 Driftwood  (same scheme)
    overworld2*.mp3 Daybreak   (same scheme)
    nether*.mp3     Brimstone  (same scheme)

(.wav, .flac and .ogg work as well.) Every file found is converted to mono Ogg Vorbis (~quality 5,
44.1 kHz - mono, so the jukebox plays it positionally), written to
mc26_3/overlay/resources/assets/simplebuilding/sounds/records/<song>.ogg, and its length (rounded up
to whole seconds, so the jukebox never stops before the end) goes into the song table
MusicDiscs.SONGS (common/src/shared/java/com/simplebuilding/util/MusicDiscs.java, the datagen source)
and into the generated mc26_3/generated/data/simplebuilding/jukebox_song/<song>.json.

Tracks 1 and 2 always exist (with a placeholder until the real file comes). Tracks 3 and 4 come into
being only through this script: it appends the length to the disc's line in MusicDiscs.SONGS (a 0.0F
keeps a missing track 3 free when only track 4 exists - the sledgehammer then skips it), adds the sound
to the 26.3 sounds.json, the English and German names to both language folders and the song JSON. The
item, its model and the sound event follow from that; textures are already there
(tools/textures/music_disc_textures.py). Missing files are skipped.

Encoder: ffmpeg (--ffmpeg, the FFMPEG environment variable, or ffmpeg on PATH); without ffmpeg the
Python package soundfile (libsndfile 1.1+ reads MP3/WAV and writes Vorbis). Without either the
script says how to install one and changes nothing. After importing: run the datagen once
(gradlew :mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263 - needed for the item models of
new tracks 3/4), the game tests, and commit everything the script and the datagen changed.
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
from discs import (DEFAULT_SOURCE, LANG_FILES, MUSIC_DISCS_JAVA, SOUNDS_JSON, TITLES, all_tracks, comparator,  # noqa: E402
                   ogg_path, song_json)

EXTENSIONS = (".mp3", ".wav", ".flac", ".ogg")
SAMPLE_RATE = 44100
#: new Song("<name>", "<stem>", <comparator>, <length>F, <length>F[, ...])
SONG_LINE = r'(new Song\("{name}", "[^"]+", \d+)((?:, [0-9.]+F)+)\)'


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


def set_java_length(disc, track, seconds, dry_run):
    """Sets the length of {track} in the disc's line of MusicDiscs.SONGS (extending it with 0.0F gaps)."""
    with open(MUSIC_DISCS_JAVA, encoding="utf-8", newline="") as f:
        text = f.read()
    match = re.search(SONG_LINE.format(name=re.escape(disc)), text)
    if not match:
        raise SystemExit(f"MusicDiscs.java: no Song line for {disc} - update SONG_LINE in import_discs.py")
    lengths = [float(v) for v in re.findall(r"([0-9.]+)F", match.group(2))]
    while len(lengths) < track:
        lengths.append(0.0)
    old = lengths[track - 1]
    lengths[track - 1] = float(seconds)
    text = text[:match.start()] + match.group(1) + "".join(f", {v}F" for v in lengths) + ")" + text[match.end():]
    if not dry_run:
        with open(MUSIC_DISCS_JAVA, "w", encoding="utf-8", newline="") as f:
            f.write(text)
    return old


def write_json(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def set_song_json(disc, song, seconds, dry_run):
    path = song_json(song)
    if os.path.isfile(path):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
    else:
        data = {"comparator_output": comparator(disc), "description": {"translate": "jukebox_song.simplebuilding." + song},
                "length_in_seconds": 0.0, "sound_event": "simplebuilding:music_disc." + song}
    data["length_in_seconds"] = float(seconds)
    if not dry_run:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        write_json(path, data)


def add_sound(song, dry_run):
    """Adds music_disc.<song> to the 26.3 sounds.json (tracks 3/4; 1/2 are always there)."""
    with open(SOUNDS_JSON, encoding="utf-8") as f:
        data = json.load(f)
    key = "music_disc." + song
    if key in data:
        return False
    data[key] = {"sounds": [{"name": "simplebuilding:records/" + song, "stream": True}]}
    if not dry_run:
        write_json(SOUNDS_JSON, data)
    return True


def add_lang(disc, song, track, dry_run):
    """Item name and song description of a new track 3/4 in both language folders (EN and DE)."""
    title = TITLES[disc]
    for path in LANG_FILES:
        german = path.endswith("de_de.json")
        entries = {f"item.simplebuilding.music_disc_{song}": "Schallplatte" if german else "Music Disc",
                   f"jukebox_song.simplebuilding.{song}": f"SimpleBuilding - {title} (Track {track})"}
        with open(path, encoding="utf-8", newline="") as f:
            text = f.read()
        data = json.loads(text)
        missing = {k: v for k, v in entries.items() if k not in data}
        if not missing or dry_run:
            continue
        nl = "\r\n" if "\r\n" in text else "\n"
        body = text.rstrip()[:-1].rstrip()
        body += "".join(f",{nl}  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}" for k, v in missing.items())
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(body + nl + "}" + nl)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--source", default=DEFAULT_SOURCE, help="folder with end.mp3, overworld1.mp3, ...")
    parser.add_argument("--ffmpeg", help="path to ffmpeg(.exe)")
    parser.add_argument("--dry-run", action="store_true", help="only show what would happen")
    args = parser.parse_args()

    if not os.path.isdir(args.source):
        print(f"Source folder not found: {args.source}\nCreate it and put end.mp3, overworld1.mp3, overworld2.mp3, nether.mp3 "
              "(optional *_alt.mp3, *_3.mp3, *_4.mp3) into it.")
        return 1
    found = []
    for disc, track, song, stem in all_tracks():
        src = find_source(args.source, stem)
        if src:
            found.append((disc, track, song, src))
        elif track <= 2:
            print(f"skip {song:20} ({stem}.mp3/.wav not found - placeholder stays)")
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

    new_tracks = False
    for disc, track, song, src in found:
        dst = ogg_path(song)
        if args.dry_run:
            print(f"would convert {src} -> {dst}" + (f" (new track {track})" if track > 2 else ""))
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
        old = set_java_length(disc, track, seconds, args.dry_run)
        set_song_json(disc, song, seconds, args.dry_run)
        if track > 2:
            new_tracks |= add_sound(song, args.dry_run)
            add_lang(disc, song, track, args.dry_run)
        print(f"{song:20} {os.path.basename(src)} -> {os.path.relpath(dst)}  length {old} -> {float(seconds)} s")
    if new_tracks:
        print("New tracks 3/4: run the datagen now (item models), then the game tests.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
