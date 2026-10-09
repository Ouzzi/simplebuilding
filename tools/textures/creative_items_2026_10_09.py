"""Creative Blueprint and Creative Building Wand (Queue Nachtrag 29, 2026-10-09).

Usage: python tools/textures/creative_items_2026_10_09.py [preview png]

- creative_blueprint{,_edited,_signed}: the three blueprint states (the owner's settled map sheets) with the
  cyanotype blue turned violet - the creative look - and the creative mark, a small gold four-point star, in the
  free upper right of the sheet. The white house and the red wax seal stay as they are.
- creative_building_wand: an own wand, not a recolour - the diagonal shaft of the wand family in white quartz
  with magenta bands, topped by a gold four-point star (the same creative mark) instead of a gem.
Writes into the main tree and the wiki copy and saves a labelled 16x preview next to the existing items.
"""
import colorsys
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ITEM = ROOT / "src/main/resources/assets/simplebuilding/textures/item"
OUT_DIRS = [ITEM, ROOT / "wiki/assets/textures/item"]
PREVIEW = sys.argv[1] if len(sys.argv) > 1 else str(ROOT / "build/creative-items.png")

GOLD_DARK = (150, 96, 18, 255)
GOLD = (246, 196, 58, 255)
GOLD_LIGHT = (255, 244, 186, 255)


def violet(img):
    """Blue pixels (the sheet, its grid and outline) to violet; whites and the red seal unchanged."""
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if 0.5 <= h <= 0.72 and s > 0.25:
                h = 0.79  # cyanotype blue -> violet, same saturation and value
                nr, ng, nb = colorsys.hsv_to_rgb(h, min(1.0, s * 1.05), v)
                out.putpixel((x, y), (round(nr * 255), round(ng * 255), round(nb * 255), a))
    return out


def star_mark(img, cx, cy):
    """The creative mark on the sheet: a 3x3 gold star (plus) with a bright centre."""
    for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
        img.putpixel((cx + dx, cy + dy), GOLD)
    img.putpixel((cx, cy), GOLD_LIGHT)
    return img


WAND = [
    "................",
    "..........o.....",
    ".........oYo....",
    "........oYWYo...",
    "......ooYWWWYoo.",
    "........oYWYo...",
    ".........oYo....",
    ".......jkho.....",
    "......jkl.......",
    ".....jml........",
    "....jcl.........",
    "...jgl..........",
    ".jjcl...........",
    ".jkl............",
    "..ll............",
    "................",
]
WAND_PAL = {
    "o": GOLD_DARK, "Y": GOLD, "W": GOLD_LIGHT,
    "j": (70, 52, 66, 255),     # shaft outline
    "k": (240, 234, 228, 255),  # quartz light
    "m": (205, 196, 190, 255),  # quartz mid
    "h": (255, 250, 240, 255),  # quartz highlight under the star
    "l": (128, 112, 118, 255),  # quartz shadow
    "c": (214, 64, 196, 255),   # magenta band
    "g": (246, 128, 232, 255),  # magenta band light
}


def wand():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(WAND):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), WAND_PAL[ch])
    return img


def build():
    out = {}
    for state in ("", "_edited", "_signed"):
        sheet = violet(Image.open(ITEM / f"blueprint{state}.png").convert("RGBA"))
        out[f"creative_blueprint{state}"] = star_mark(sheet, 12, 3)
    out["creative_building_wand"] = wand()
    return out


def main():
    after = build()
    for d in OUT_DIRS:
        d.mkdir(parents=True, exist_ok=True)
        for name, img in after.items():
            img.save(d / f"{name}.png")
    refs = ["blueprint", "blueprint_edited", "blueprint_signed", "enderite_building_wand"]
    names = list(after)
    s, cell = 10, 16 * 10 + 14
    sheet = Image.new("RGBA", (130 + 4 * cell, 2 * (cell + 22) + 34), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), "Kreativ-Blaupause (3 Zustaende) und Kreativ-Baustab - oben die Vorbilder", fill=(0, 0, 0, 255))
    for row, (label, imgs) in enumerate((("Vorbild", {n: Image.open(ITEM / f"{n}.png").convert("RGBA") for n in refs}),
                                         ("Kreativ", after))):
        y = 30 + row * (cell + 22)
        d.text((10, y + cell // 2), label, fill=(0, 0, 0, 255))
        for k, n in enumerate(refs if row == 0 else names):
            x = 130 + k * cell
            d.text((x, y), n, fill=(0, 0, 0, 255))
            sheet.alpha_composite(imgs[n].resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    sheet.save(PREVIEW)
    print("ok", PREVIEW)


if __name__ == "__main__":
    main()
