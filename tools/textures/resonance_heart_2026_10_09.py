"""Resonance field beside the recipe book (inventory), round 2 (owner 2026-10-09): vanilla style.

Writes three GUI sprites:
- resonance_heart.png: the Vanilla HUD heart (9x9 silhouette and black outline) filled with cobblestone (round 5);
  resonance_heart_max.png: the same heart filled with diamond, shown at maximum resonance.
- resonance_field.png / resonance_field_highlighted.png: the frame of the vanilla recipe-book button (20x18, rounded
  1 px black outline, white bevel top/left, dark bevel bottom/right, fill #C6C6C6; the highlighted one in the blue
  hover colours) without the book, as nine-slice sprites (border 3) so the field can grow with its text.

N49 (owner 2026-10-12): the resonance button beside the off-hand slot shows one of eight tier hearts
(resonance_heart_<tier>.png: cobblestone, stone, copper, iron, gold, diamond, netherite, enderite), each with its own
fill pattern in the Vanilla heart silhouette, and the extra head slot gets its empty-slot icon
(container/slot/extra_head.png, a skull silhouette in the #555555 of Vanilla's empty-slot icons).

Run: python3.12 tools/textures/resonance_heart_2026_10_09.py
"""
import json
from pathlib import Path
from PIL import Image

# Round 5 (owner 2026-10-09 evening): exactly the Vanilla HUD heart (hud/heart/container.png outline + full.png
# fill, 9x9): the same silhouette, the same pure black outline, the same light (highlight at (2, 2), the darker
# fill pixels along the lower edges where full.png has #BB1313). Only the fill is cobblestone (light stones, mid
# stones, dark mortar) or diamond (resonance_heart_max, at maximum resonance).
HEART = [
    "..##.##..",
    ".#lm#lm#.",
    "#lhldmlm#",
    "#mldlmdl#",
    "#smlmdls#",
    ".#sdlms#.",
    "..#sms#..",
    "...#s#...",
    "....#....",
]
HEART_MAX = [
    "..##.##..",
    ".#lh#lm#.",
    "#lhlmllm#",
    "#llmllmd#",
    "#smlmmds#",
    ".#smlds#.",
    "..#sds#..",
    "...#s#...",
    "....#....",
]
HEART_COLOURS = {  # Vanilla black outline; cobblestone greys: highlight, light stone, mid stone, mortar, lower shade
    "#": (0, 0, 0, 255),
    "h": (0xC8, 0xC8, 0xC8, 255),
    "l": (0x9E, 0x9E, 0x9E, 255),
    "m": (0x80, 0x80, 0x80, 255),
    "d": (0x5C, 0x5C, 0x5C, 255),
    "s": (0x5C, 0x5C, 0x5C, 255),
}
HEART_MAX_COLOURS = {  # Vanilla black outline; Vanilla diamond item tones: glint, light, mid, facet, lower shade
    "#": (0, 0, 0, 255),
    "h": (0xD5, 0xFF, 0xF6, 255),
    "l": (0x4A, 0xED, 0xD9, 255),
    "m": (0x20, 0xC5, 0xB5, 255),
    "d": (0x16, 0x9C, 0x96, 255),
    "s": (0x11, 0x72, 0x7A, 255),
}
FIELD = [  # 19x19: the 9x9 heart sits exactly in the middle (5 px on every side)
    "..ooooooooooooooo..",
    ".o" + "H" * 15 + "o.",
] + ["oH" + "f" * 15 + "So"] * 15 + [
    ".o" + "S" * 15 + "o.",
]
FIELD.append("..ooooooooooooooo..")
FIELD_COLOURS = {  # N50: the Vanilla widget button (dark grey, light bevel) so it no longer reads as a disabled slot
    False: {"o": (0, 0, 0, 255), "H": (0xAA, 0xAA, 0xAA, 255), "f": (0x6F, 0x6F, 0x6F, 255), "S": (0x56, 0x56, 0x56, 255)},
    True: {"o": (255, 255, 255, 255), "H": (0xAF, 0xAF, 0xAF, 255), "f": (0x75, 0x75, 0x75, 255), "S": (0x5C, 0x5C, 0x5C, 255)},
}
# N50b (owner 2026-10-13): back to the N49 heart shapes (the owner preferred them), colour faults fixed: no verdigris
# on copper, no light spot in the enderite heart, no foreign colour inside any heart. Every heart uses the same Vanilla
# silhouette (9x9, pure black outline) and a pure ramp h > l > m > d (> s) of its material, derived from the tones of
# the Vanilla ingots (colours only looked at, no pixels copied).
STONE = ["..##.##..", ".#hl#lm#.", "#hllmlmm#", "#lmmmmdm#", "#mmdmmms#", ".#mmmms#.", "..#mss#..", "...#s#...", "....#...."]
COPPER = ["..##.##..", ".#hl#hl#.", "#hllllmm#", "#llmlmmm#", "#lmmmmmd#", ".#mmmdd#.", "..#mdd#..", "...#d#...", "....#...."]
IRON = ["..##.##..", ".#hh#hl#.", "#hhllhlm#", "#hllhlmm#", "#llhlmmd#", ".#lhmmd#.", "..#mmd#..", "...#d#...", "....#...."]
GOLD = ["..##.##..", ".#hl#hl#.", "#lhlllhm#", "#llllmlm#", "#hllmmmd#", ".#lmmhd#.", "..#mdd#..", "...#d#...", "....#...."]
NETHERITE = ["..##.##..", ".#lm#lm#.", "#hlllllm#", "#dddmddd#", "#lmmmmmd#", ".#ddmdd#.", "..#mmd#..", "...#d#...", "....#...."]
ENDERITE = ["..##.##..", ".#hl#lm#.", "#hlmllmm#", "#lmmmmmd#", "#mmmmmdd#", ".#mmmdd#.", "..#mdd#..", "...#d#...", "....#...."]
BLACK = (0, 0, 0, 255)


def tones(**kw):
    return {"#": BLACK, **{k: tuple(int(v[i:i + 2], 16) for i in (0, 2, 4)) + (255,) for k, v in kw.items()}}


TIER_HEARTS = {
    "cobblestone": (HEART, HEART_COLOURS),
    "stone": (STONE, tones(h="BEBEBE", l="A4A4A4", m="8F8F8F", d="6E6E6E", s="5E5E5E")),
    "copper": (COPPER, tones(h="FC9982", l="E77C56", m="C15A36", d="8A3E26")),
    "iron": (IRON, tones(h="FFFFFF", l="D8D8D8", m="A8A8A8", d="727272")),
    "gold": (GOLD, tones(h="FFFFB5", l="FDF55F", m="E9B115", d="B26411")),
    "diamond": (HEART_MAX, HEART_MAX_COLOURS),
    "netherite": (NETHERITE, tones(h="7D7579", l="5A5257", m="443D41", d="2B2528")),
    "enderite": (ENDERITE, tones(h="E2C2FF", l="B47FE0", m="8550B8", d="5A2F86")),
}
EXTRA_HEAD_ICON = [  # 16x16 skull silhouette, eyes/nose/teeth cut out
    "................",
    "................",
    ".....######.....",
    "...##########...",
    "...#.##..##.#...",
    "...#.##..##.#...",
    "..###..##..###..",
    "..###..##..###..",
    "...#.##..##.#...",
    "..#####..#####..",
    "...###....###...",
    "....########....",
    "...#.##..##.#...",
    "....########....",
    "................",
    "................",
]
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/simplebuilding/textures/gui/sprites"
NINE_SLICE = {"gui": {"scaling": {"type": "nine_slice", "width": 19, "height": 19, "border": 3}}}


def paint(rows, colours) -> Image.Image:
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colours:
                img.putpixel((x, y), colours[ch])
    return img


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    paint(HEART, HEART_COLOURS).save(OUT / "resonance_heart.png")
    paint(HEART_MAX, HEART_MAX_COLOURS).save(OUT / "resonance_heart_max.png")
    for tier, (rows, colours) in TIER_HEARTS.items():
        paint(rows, colours).save(OUT / f"resonance_heart_{tier}.png")
    (OUT / "container/slot").mkdir(parents=True, exist_ok=True)
    paint(EXTRA_HEAD_ICON, {"#": (0x55, 0x55, 0x55, 255)}).save(OUT / "container/slot/extra_head.png")
    for highlighted, name in ((False, "resonance_field"), (True, "resonance_field_highlighted")):
        paint(FIELD, FIELD_COLOURS[highlighted]).save(OUT / f"{name}.png")
        (OUT / f"{name}.png.mcmeta").write_text(json.dumps(NINE_SLICE, indent=2) + "\n", encoding="utf-8")
    print(OUT)


if __name__ == "__main__":
    main()
