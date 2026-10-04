"""Shared table of the music discs (mirror of com.simplebuilding.util.MusicDiscs#SONGS).

song name -> (owner file stem in the music folder, comparator level). Each disc has up to four tracks:

    track 1  <song>          <stem>.mp3        (always)
    track 2  <song>_b_side   <stem>_alt.mp3    (always, B-side)
    track 3  <song>_track_3  <stem>_3.mp3      (only once imported)
    track 4  <song>_track_4  <stem>_4.mp3      (only once imported)
"""
import os

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RESOURCES = os.path.join(REPO, "mc26_3", "overlay", "resources")
RECORDS = os.path.join(RESOURCES, "assets", "simplebuilding", "sounds", "records")
SOUNDS_JSON = os.path.join(RESOURCES, "assets", "simplebuilding", "sounds.json")
LANG_FILES = [os.path.join(REPO, base, "assets", "simplebuilding", "lang", name)
              for base in ("src/main/resources", "mc26_3/overlay/resources") for name in ("en_us.json", "de_de.json")]
#: Datagen output of the songs (from MusicDiscs.SONGS); import_discs.py patches both.
SONG_DATA = os.path.join(REPO, "mc26_3", "generated", "data", "simplebuilding", "jukebox_song")
MUSIC_DISCS_JAVA = os.path.join(REPO, "common", "src", "shared", "java", "com", "simplebuilding", "util", "MusicDiscs.java")
#: Where the owner drops the real tracks (MP3 or WAV); --source overrides it.
DEFAULT_SOURCE = r"C:\Users\o_o\code\minecraft-mods\music"
#: The owner's file and in-game title per disc track (tools/audio/owner_tracks.json).
OWNER_MAP = os.path.join(os.path.dirname(os.path.abspath(__file__)), "owner_tracks.json")
MAX_TRACKS = 4

#: End, Overworld 1, Overworld 2, Nether - same order as MusicDiscs.SONGS.
SONGS = [
    ("voidline", "end", 14),
    ("driftwood", "overworld1", 6),
    ("daybreak", "overworld2", 12),
    ("brimstone", "nether", 13),
]
TITLES = {"voidline": "Voidline", "driftwood": "Driftwood", "daybreak": "Daybreak", "brimstone": "Brimstone"}


def track_name(song, track):
    return song if track == 1 else song + "_b_side" if track == 2 else f"{song}_track_{track}"


def track_file(stem, track):
    return stem if track == 1 else stem + "_alt" if track == 2 else f"{stem}_{track}"


def all_tracks():
    """(disc song, track number, track song name, owner stem) for every possible track 1..4."""
    out = []
    for song, stem, _ in SONGS:
        for track in range(1, MAX_TRACKS + 1):
            out.append((song, track, track_name(song, track), track_file(stem, track)))
    return out


def comparator(song):
    return next(c for s, _, c in SONGS if s == song)


def ogg_path(song):
    return os.path.join(RECORDS, song + ".ogg")


def song_json(song):
    return os.path.join(SONG_DATA, song + ".json")
