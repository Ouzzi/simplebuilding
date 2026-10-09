"""Colour brush v2 (owner 2026-10-09): a variation of the vanilla brush plus the colour palette.

The brush keeps the vanilla brush outline (read at run time from the 26.3 client jar in the Gradle cache, never
checked in) and changes the handle to dark blue lacquer and the copper ferrule to iron. Its bristle tip is a
separate layer: grey shades that the item model tints with the next dye's colour (color_brush_tip.png), or a
rainbow for a palette (color_brush_tip_palette.png). The palette is own pixel art.

Run: python3.12 tools/textures/color_brush_2026_10_09.py [--preview out.png]
"""
from __future__ import annotations

import argparse
import glob
import io
import os
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT_DIRS = [ROOT / "src/main/resources/assets/simplebuilding/textures/item", ROOT / "wiki/assets/textures/item"]

# Vanilla brush colours -> v2 colours. Head (bristles) stays, copper ferrule becomes iron, handle dark blue lacquer.
RECOLOUR = {
    (0xA5, 0x49, 0x26): (0x6E, 0x6E, 0x74),  # ferrule mid
    (0xFC, 0x99, 0x82): (0xD8, 0xD8, 0xDE),  # ferrule light
    (0xFB, 0xC3, 0xB6): (0xF4, 0xF4, 0xF8),  # ferrule glint
    (0x6D, 0x34, 0x21): (0x48, 0x48, 0x50),  # ferrule dark
    (0xC1, 0x5A, 0x36): (0xA6, 0xA6, 0xAE),  # ferrule face
    (0x49, 0x36, 0x15): (0x1E, 0x26, 0x40),  # handle outline
    (0x86, 0x65, 0x26): (0x4C, 0x6A, 0xA4),  # handle highlight
    (0x75, 0x58, 0x21): (0x36, 0x4E, 0x80),  # handle mid
    (0x6B, 0x51, 0x1F): (0x30, 0x45, 0x72),  # handle mid dark
    (0x59, 0x43, 0x19): (0x28, 0x3A, 0x62),  # handle dark
    (0x20, 0x18, 0x0A): (0x10, 0x16, 0x2A),  # handle end
}
# Bristle shades -> grey level of the tinted tip (255 = the dye colour itself).
TIP_GREY = {
    (0xEE, 0xC3, 0x9A): 255,
    (0xDB, 0xA6, 0x91): 222,
    (0xBF, 0x95, 0x7F): 190,
    (0xA5, 0x83, 0x6F): 165,
    (0x87, 0x6E, 0x5B): 130,
    (0x73, 0x5F, 0x4E): 108,
}
TIP_FROM = 2  # tip = bristle pixels with x - y >= TIP_FROM (the far end of the head)

# Vanilla dye diffuse colours (DyeColor.getTextureDiffuseColor), in DyeColor order.
DYES = {
    "white": 0xF9FFFE, "orange": 0xF9801D, "magenta": 0xC74EBD, "light_blue": 0x3AB3DA,
    "yellow": 0xFED83D, "lime": 0x80C71F, "pink": 0xF38BAA, "gray": 0x474F52,
    "light_gray": 0x9D9D97, "cyan": 0x169C9C, "purple": 0x8932B8, "blue": 0x3C44AA,
    "brown": 0x835432, "green": 0x5E7C16, "red": 0xB02E26, "black": 0x1D1D21,
}
RAINBOW = ["red", "orange", "yellow", "lime", "light_blue", "blue", "purple", "magenta"]

PALETTE_ROWS = [
    "................",
    "................",
    ".....OOOOOO.....",
    "...OOllllllOO...",
    "..OlRRllllYYlO..",
    ".OllRrlllllYyoO.",
    ".OllllllBBllloO.",
    "OlWWllllBbllllO.",
    "OlWwlllOOlllooO.",
    "OllllllO.OllodO.",
    ".OlllllOOllodO..",
    ".OlGGlllllloO...",
    "..OGgllllloodO..",
    "...OOoooooddO...",
    ".....OOOOOOO....",
    "................",
]
PALETTE_COLOURS = {
    "O": (0x5A, 0x3E, 0x22), "l": (0xC8, 0x9A, 0x5E), "o": (0xA8, 0x7E, 0x48), "d": (0x86, 0x62, 0x36),
    "R": (0xD8, 0x3A, 0x30), "r": (0x9C, 0x24, 0x1E), "Y": (0xFE, 0xD8, 0x3D), "y": (0xC8, 0xA0, 0x20),
    "B": (0x4A, 0x6C, 0xD8), "b": (0x2C, 0x40, 0x98), "W": (0xF4, 0xF6, 0xF6), "w": (0xBC, 0xC4, 0xC6),
    "G": (0x80, 0xC7, 0x1F), "g": (0x52, 0x86, 0x14),
}


def vanilla_brush() -> Image.Image:
    home = Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle"))
    jars = sorted(glob.glob(str(home / "caches/neoformruntime/artifacts/minecraft_26.3_client.jar")))
    jars += sorted(glob.glob(str(home / "caches/fabric-loom/26.3/minecraft-client.jar")))
    jars += sorted(glob.glob(str(home / "caches/minecraftforge/**/26.3/client.jar"), recursive=True))
    for jar in jars:
        with zipfile.ZipFile(jar) as z:
            try:
                return Image.open(io.BytesIO(z.read("assets/minecraft/textures/item/brush.png"))).convert("RGBA")
            except KeyError:
                continue
    raise SystemExit("26.3 client jar with assets/minecraft/textures/item/brush.png not found in the Gradle cache")


def build() -> dict[str, Image.Image]:
    src = vanilla_brush()
    body = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    tip = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rainbow = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            rgb = (r, g, b)
            body.putpixel((x, y), RECOLOUR.get(rgb, rgb) + (255,))
            if rgb in TIP_GREY and x - y >= TIP_FROM:
                grey = TIP_GREY[rgb]
                tip.putpixel((x, y), (grey, grey, grey, 255))
                band = RAINBOW[((x + y) // 2) % len(RAINBOW)]
                c = DYES[band]
                rainbow.putpixel((x, y), tuple(((c >> s) & 0xFF) * grey // 255 for s in (16, 8, 0)) + (255,))
    palette = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(PALETTE_ROWS):
        for x, ch in enumerate(row):
            if ch in PALETTE_COLOURS:
                palette.putpixel((x, y), PALETTE_COLOURS[ch] + (255,))
    return {"color_brush": body, "color_brush_tip": tip, "color_brush_tip_palette": rainbow, "paint_palette": palette}


def tinted(body: Image.Image, tip: Image.Image, colour: int | None) -> Image.Image:
    out = body.copy()
    if colour is None:
        return out
    for y in range(16):
        for x in range(16):
            g = tip.getpixel((x, y))
            if g[3]:
                out.putpixel((x, y), tuple(((colour >> s) & 0xFF) * g[0] // 255 for s in (16, 8, 0)) + (255,))
    return out


def preview(images: dict[str, Image.Image], path: Path, vanilla: Image.Image) -> None:
    scale, pad = 8, 8
    cells = [vanilla, images["color_brush"]]
    cells += [tinted(images["color_brush"], images["color_brush_tip"], c) for c in DYES.values()]
    cells += [Image.alpha_composite(images["color_brush"], images["color_brush_tip_palette"]), images["paint_palette"]]
    cols = 10
    rows = (len(cells) + cols - 1) // cols
    cell = 16 * scale + pad
    sheet = Image.new("RGBA", (cols * cell + pad, rows * cell + pad), (0x8B, 0x8B, 0x8B, 255))
    for i, img in enumerate(cells):
        x, y = pad + (i % cols) * cell, pad + (i // cols) * cell
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (x, y))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print(path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path)
    args = parser.parse_args()
    images = build()
    for out in OUT_DIRS:
        out.mkdir(parents=True, exist_ok=True)
        for name, img in images.items():
            if out == OUT_DIRS[0] or "_tip" not in name:  # the wiki only shows the items
                img.save(out / f"{name}.png")
    if args.preview:
        preview(images, args.preview, vanilla_brush())


if __name__ == "__main__":
    main()
