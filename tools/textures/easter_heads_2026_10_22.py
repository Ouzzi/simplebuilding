"""Crown of the EH21 tribute head (Technoblade, owner EH21 N-A/TO-1; docs/ai/PLAN-B-EH21-2026-10-22.md).

Only the crown is own art: the pig head under it and the giant/giant cow heads show the Vanilla mob textures at runtime
(by reference, no Mojang pixels in the repo). The crown is a 6x4x6 box (CustomHeadModels.crownLayer) on a 32x16 sheet in
Minecraft box UV: top 6..12/0..6 and bottom 12..18/0..6 stay empty, the four sides (right 0..6, front 6..12, left 12..18,
back 18..24, rows 6..10) carry the same 6x4 face. Vanilla-near (PRINZIPIEN A8 a): the gold ramp of the gold ingot / golden
helmet, the red of redstone and the cyan of the diamond (colours only, no pixels), lit from the upper left like Vanilla's
items, points 2 px high on a 2 px band. Layers (A8 b): Band, Zacken (points), Steine (gems), Licht (highlights).

Variants (preview only, CHOSEN goes into the mod):
  A  Zackenkrone mit Rubin   - points at the corners and in the middle, a ruby in the band   <- recommended, built in
  B  Schlicht                - the same points, plain band
  C  Mittelzacke mit Saphir  - one high middle point and two corner points, a sapphire

Pure Python (tools/textures/layers.py writes PNG and layers), runs wherever `gradlew check` runs.
Run: python3 tools/textures/easter_heads_2026_10_22.py            writes crown.png + crown.layers.json
     python3 tools/textures/easter_heads_2026_10_22.py --check    fails if the committed files differ
"""
from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import layers  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "modules/simplemobheads/shared/resources/assets/simplemobheads/textures/entity/technoblade/crown.png"
CHOSEN = "A"
W, H = 32, 16
SIDES = (0, 6, 12, 18)  # u of the four side faces (right, front, left, back), v = 6
FACE_V = 6

PALETTE = {
    "W": (0xff, 0xfd, 0xe0),  # sparkle (gold ingot)
    "H": (0xfd, 0xf5, 0x5f),  # highlight
    "L": (0xfa, 0xd6, 0x4a),  # light
    "M": (0xe9, 0xb1, 0x15),  # mid
    "S": (0xdc, 0x96, 0x13),  # shade
    "D": (0xb2, 0x64, 0x11),  # dark
    "R": (0xff, 0x00, 0x00),  # ruby light (redstone)
    "r": (0xaa, 0x0f, 0x01),  # ruby
    "B": (0x4a, 0xed, 0xd9),  # sapphire light (diamond)
    "b": (0x20, 0xc5, 0xb5),  # sapphire
}

# One 6x4 side face per variant; '.' = transparent. Rows 0-1 points, rows 2-3 band.
VARIANTS = {
    "A": {"name": "Zackenkrone mit Rubin",
          "rows": ["H.LL.M",
                   "L.MM.S",
                   "LMRrMS",
                   "MSSSSD"]},
    "B": {"name": "Schlicht",
          "rows": ["H.HL.L",
                   "L.LM.M",
                   "LLMMMS",
                   "SSSSDD"]},
    "C": {"name": "Mittelzacke mit Saphir",
          "rows": ["..WL..",
                   "H.LM.M",
                   "LLBbMS",
                   "MSSSSD"]},
}

def face_layers(rows: list[str]) -> list[tuple[str, bytes]]:
    """The layers of the whole 32x16 sheet: Band (rows 2-3 gold), Zacken (rows 0-1 gold), Steine, Licht."""
    def sheet(pick) -> bytes:
        out = bytearray(W * H * 4)
        for u in SIDES:
            for y, row in enumerate(rows):
                for x, ch in enumerate(row):
                    if ch != "." and pick(y, ch):
                        i = ((FACE_V + y) * W + u + x) * 4
                        out[i:i + 4] = bytes(PALETTE[ch] + (255,))
        return bytes(out)

    return [
        ("Band", sheet(lambda y, ch: y >= 2 and ch in "LMSD")),
        ("Zacken", sheet(lambda y, ch: y < 2 and ch in "LMSD")),
        ("Steine", sheet(lambda y, ch: ch in "RrBb")),
        ("Licht", sheet(lambda y, ch: ch in "WH")),
    ]


def compose(parts: list[tuple[str, bytes]]) -> bytes:
    out = bytearray(W * H * 4)
    for _, img in parts:
        for i in range(0, len(img), 4):
            if img[i + 3]:
                out[i:i + 4] = img[i:i + 4]
    return bytes(out)


def check_rows(variant: dict) -> None:
    rows = variant["rows"]
    assert len(rows) == 4 and all(len(r) == 6 for r in rows), variant["name"]
    assert all(c == "." or c in PALETTE for r in rows for c in r), variant["name"]


def build(key: str) -> tuple[bytes, dict]:
    """(PNG bytes, layers document) of variant key."""
    variant = VARIANTS[key]
    check_rows(variant)
    parts = face_layers(variant["rows"])
    doc = layers.from_layers(W, H, [(n, img) for n, img in parts if any(img[3::4])])
    return layers.png_write(W, H, compose(parts)), doc


def main() -> int:
    png, doc = build(CHOSEN)
    target_layers = layers.layers_path(OUT)
    if "--check" in sys.argv:
        stale = []
        if not OUT.is_file() or bytes(layers.png_read(OUT.read_bytes())[2]) != bytes(layers.png_read(png)[2]):
            stale.append(str(OUT.relative_to(ROOT)))
        if not target_layers.is_file() or target_layers.read_text(encoding="utf-8") != layers.dumps(doc):
            stale.append(str(target_layers.relative_to(ROOT)))
        if stale:
            print("easter heads crown: stale", stale)
            return 1
        print("easter heads crown: current")
        return 0
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(png)
    layers.write_beside(OUT, doc)
    print("wrote", OUT.relative_to(ROOT), "and its layers (variant", CHOSEN, VARIANTS[CHOSEN]["name"] + ")")
    return 0


if __name__ == "__main__":
    sys.exit(main())
