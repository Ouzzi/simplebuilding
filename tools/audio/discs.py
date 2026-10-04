"""Shared table of the music discs (mirror of com.simplebuilding.util.MusicDiscs#SONGS).

song name -> (owner file stem in the musik folder, comparator level). Each disc has a B-side
"<song>_b_side" whose owner file is "<stem>_alt".
"""
import os

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RESOURCES = os.path.join(REPO, "mc26_3", "overlay", "resources")
RECORDS = os.path.join(RESOURCES, "assets", "simplebuilding", "sounds", "records")
#: Datagen output of the songs (from MusicDiscs.SONGS); import_discs.py patches both.
SONG_DATA = os.path.join(REPO, "mc26_3", "generated", "data", "simplebuilding", "jukebox_song")
MUSIC_DISCS_JAVA = os.path.join(REPO, "common", "src", "shared", "java", "com", "simplebuilding", "util", "MusicDiscs.java")
#: Where the owner drops the real tracks (MP3 or WAV).
DEFAULT_SOURCE = r"C:\Users\o_o\code\minecraft-mods\musik"

#: End, Overworld 1, Overworld 2, Nether - same order as MusicDiscs.SONGS.
SONGS = [
    ("voidline", "end", 14),
    ("driftwood", "overworld1", 6),
    ("daybreak", "overworld2", 12),
    ("brimstone", "nether", 13),
]


def all_tracks():
    """(song, owner stem) for every A- and B-side."""
    out = []
    for song, stem, _ in SONGS:
        out.append((song, stem))
        out.append((song + "_b_side", stem + "_alt"))
    return out


def ogg_path(song):
    return os.path.join(RECORDS, song + ".ogg")


def song_json(song):
    return os.path.join(SONG_DATA, song + ".json")
