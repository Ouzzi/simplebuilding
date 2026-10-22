"""Technoblade tribute head of EH21 (owner EH21 N-A/TO-1, acceptance 22.10.: "so wie sein skin auch tatsaechlich war";
docs/ai/PLAN-B-EH21-2026-10-22.md).

His skin's motif, drawn anew with own pixels (no pixels of the real skin read or copied): a player-sized pig head (8x8x8,
humanoid head + hat layer on a 64x64 skin sheet, like the Herobrine head) - pink, black-and-white pig eyes, a lighter snout
with two nostrils, darker ear patches on the sides - and on the hat layer a thin golden crown round the top of the head:
single-pixel points, a row of many-coloured jewels (blue, purple, red, green) and a gold band. Vanilla-near (PRINZIPIEN A8 a):
the pink ramp of the Vanilla pig, the gold of the gold ingot, jewels in the colours of lapis, amethyst, redstone and emerald
(colours only, no Vanilla pixels).

Two files (A8 b: each with its layer source):
  technoblade.png  64x64  the head (layers Haut, Gesicht, Ohren, Krone, Steine, Licht)
  crown.png        32x16  the same crown as a 6x4x6 ring for the living pig named Technoblade (TechnobladeCrownLayer)

Pure Python (tools/textures/layers.py writes PNG and layers), runs wherever `gradlew check` runs.
Run: python3 tools/textures/easter_heads_2026_10_22.py            writes both PNGs + layer sources
     python3 tools/textures/easter_heads_2026_10_22.py --check    fails if the committed files differ
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
DIR = ROOT / "modules/simplemobheads/shared/resources/assets/simplemobheads/textures/entity/technoblade"
HEAD = DIR / "technoblade.png"
CROWN = DIR / "crown.png"

PALETTE = {
    # pig pink (Vanilla pig ramp) and the face
    "L": (0xee, 0xa5, 0xa4), "P": (0xf1, 0x9e, 0x98), "M": (0xe6, 0x91, 0x8b), "D": (0xe6, 0x85, 0x83),
    "E": (0xe6, 0x79, 0x73), "e": (0xc6, 0x61, 0x5a), "s": (0xf7, 0xb6, 0xb3), "n": (0x89, 0x47, 0x46),
    "K": (0x14, 0x0c, 0x0c), "W": (0xff, 0xff, 0xff),
    # gold (gold ingot)
    "H": (0xfd, 0xf5, 0x5f), "G": (0xfa, 0xd6, 0x4a), "g": (0xe9, 0xb1, 0x15), "d": (0xb2, 0x64, 0x11),
    # jewels: lapis, amethyst, redstone, emerald
    "b": (0x34, 0x5e, 0xc3), "p": (0x8d, 0x6a, 0xcc), "r": (0xff, 0x00, 0x00), "y": (0x17, 0xdd, 0x62),
}
GEMS = "bpry"

# The head, 8x8 per face. right/left = the head's own right/left side; on the right face the last column touches the front,
# on the left face the first column does (skin sheet order: right, front, left, back).
FRONT = ["LPLLPLLP",
         "PPLPPLPP",
         "MMPPLPMM",
         "KWMPPMWK",
         "MPssssPM",
         "DMnssnMD",
         "DDMsMMDD",
         "EDDDDDDE"]
RIGHT = ["PLPPLPPL",
         "PPPLPPPP",
         "MPPPeePM",
         "MMPPeeMM",
         "MMPMEeMM",
         "DMMMMMMD",
         "DDMMMMDD",
         "EDDDDDDE"]
BACK = ["PLPPLPPL",
        "PPPLPPPP",
        "MPPPPPPM",
        "MMPPMPMM",
        "MMMMMMMM",
        "DMMMMMMD",
        "DDMMMMDD",
        "EDDDDDDE"]
TOP = ["LPLPPLPL",
       "PLPPLPPP",
       "PPLPPPLP",
       "LPPPLPPL",
       "PPLPPLPP",
       "PLPPPPLP",
       "PPPLPPPP",
       "LPPPLPLP"]
BOTTOM = ["DDDDDDDD"] * 8

# UV origin of each face on the 64x64 sheet (head; the hat layer is 32 px to the right).
FACES = {"top": (8, 0), "bottom": (16, 0), "right": (0, 8), "front": (8, 8), "left": (16, 8), "back": (24, 8)}
RING = ("right", "front", "left", "back")
# Layers: (name, characters, on the head faces, on the crown)
HEAD_LAYERS = (("Haut", "LPMDE", True, False), ("Gesicht", "snKW", True, False), ("Ohren", "e", True, False),
               ("Krone", "Ggd", False, True), ("Steine", GEMS, False, True), ("Licht", "H", False, True))


def crown_rows(width: int, faces: int, height: int) -> list[list[str]]:
    """The crown round `faces` sides of `width` px: points on every other pixel, jewels between gold, a gold band (and a
    darker lower band when height is 4). The jewels cycle blue, purple, red, green round the whole ring."""
    out = []
    gem = 0
    for _ in range(faces):
        rows = ["" for _ in range(height)]
        for x in range(width):
            rows[0] += "H" if x % 2 == 0 else "."
            if x % 2 == 0:
                rows[1] += "G"
            else:
                rows[1] += GEMS[gem % len(GEMS)]
                gem += 1
            rows[2] += "g"
            if height > 3:
                rows[3] += "d"
        out.append(rows)
    return out


def check_rows(rows: list[str], width: int) -> None:
    assert all(len(r) == width for r in rows), rows
    assert all(c == "." or c in PALETTE for r in rows for c in r), rows


def put(img: bytearray, w: int, ox: int, oy: int, rows: list[str], keep: str) -> None:
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch in keep:
                i = ((oy + y) * w + ox + x) * 4
                img[i:i + 4] = bytes(PALETTE[ch] + (255,))


def head_layers() -> list[tuple[str, bytes]]:
    faces = {"front": FRONT, "right": RIGHT, "left": [r[::-1] for r in RIGHT], "back": BACK, "top": TOP, "bottom": BOTTOM}
    for rows in faces.values():
        check_rows(rows, 8)
    crown = crown_rows(8, 4, 3)
    out = []
    for name, keep, on_head, on_crown in HEAD_LAYERS:
        img = bytearray(64 * 64 * 4)
        if on_head:
            for face, (ox, oy) in FACES.items():
                put(img, 64, ox, oy, faces[face], keep)
        if on_crown:
            for i, face in enumerate(RING):
                ox, oy = FACES[face]
                put(img, 64, ox + 32, oy, crown[i], keep)
        out.append((name, bytes(img)))
    return out


def crown_layers() -> list[tuple[str, bytes]]:
    """The living pig's crown: a 6x4x6 box in Minecraft box UV (sides right 0..6, front 6..12, left 12..18, back 18..24,
    rows 6..10; top and bottom empty)."""
    crown = crown_rows(6, 4, 4)
    out = []
    for name, keep, _, _ in HEAD_LAYERS[3:]:
        img = bytearray(32 * 16 * 4)
        for i, u in enumerate((0, 6, 12, 18)):
            put(img, 32, u, 6, crown[i], keep)
        out.append((name, bytes(img)))
    return out


def compose(w: int, h: int, parts: list[tuple[str, bytes]]) -> bytes:
    out = bytearray(w * h * 4)
    for _, img in parts:
        for i in range(0, len(img), 4):
            if img[i + 3]:
                out[i:i + 4] = img[i:i + 4]
    return bytes(out)


def build() -> dict:
    """{path: (PNG bytes, layers document)} of both textures."""
    result = {}
    for path, w, h, parts in ((HEAD, 64, 64, head_layers()), (CROWN, 32, 16, crown_layers())):
        doc = layers.from_layers(w, h, [(n, img) for n, img in parts if any(img[3::4])])
        result[path] = (layers.png_write(w, h, compose(w, h, parts)), doc)
    return result


def main() -> int:
    built = build()
    if "--check" in sys.argv:
        stale = []
        for path, (png, doc) in built.items():
            source = layers.layers_path(path)
            if not path.is_file() or bytes(layers.png_read(path.read_bytes())[2]) != bytes(layers.png_read(png)[2]):
                stale.append(str(path.relative_to(ROOT)))
            if not source.is_file() or source.read_text(encoding="utf-8") != layers.dumps(doc):
                stale.append(str(source.relative_to(ROOT)))
        if stale:
            print("technoblade head: stale", stale)
            return 1
        print("technoblade head: current")
        return 0
    DIR.mkdir(parents=True, exist_ok=True)
    for path, (png, doc) in built.items():
        path.write_bytes(png)
        layers.write_beside(path, doc)
    print("wrote", ", ".join(str(p.relative_to(ROOT)) for p in built), "and their layers")
    return 0


if __name__ == "__main__":
    sys.exit(main())
