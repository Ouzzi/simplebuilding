"""Colour brush (owner 2026-10-09, round 3): the vanilla brush with a gold ferrule.

The brush keeps the vanilla brush outline (read at run time from the 26.3 client jar in the Gradle cache, never
checked in) and only turns the copper ferrule into gold. Its bristle tip is a separate layer: grey shades that the
item model tints with the next dye's colour (color_brush_tip.png), or a rainbow for a paint box
(color_brush_tip_palette.png). Preview: vanilla | gold brush neutral | some tip colours | paint box tip.

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

# Round 3 (owner 2026-10-09, "reinforced brush"): the vanilla brush unchanged except the copper ferrule, which becomes
# gold (gold nugget colours, matched by brightness). Handle and bristles stay vanilla.
RECOLOUR = {
    (0x6D, 0x34, 0x21): (0x7F, 0x52, 0x0C),  # ferrule dark
    (0xA5, 0x49, 0x26): (0xDC, 0x96, 0x13),  # ferrule mid
    (0xC1, 0x5A, 0x36): (0xE9, 0xB1, 0x15),  # ferrule face
    (0xFC, 0x99, 0x82): (0xF9, 0xF9, 0x69),  # ferrule light
    (0xFB, 0xC3, 0xB6): (0xFF, 0xFD, 0xE0),  # ferrule glint
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
    return {"color_brush": body, "color_brush_tip": tip, "color_brush_tip_palette": rainbow}


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
    cells += [tinted(images["color_brush"], images["color_brush_tip"], DYES[c]) for c in ("red", "yellow", "lime", "blue", "magenta", "black")]
    cells += [Image.alpha_composite(images["color_brush"], images["color_brush_tip_palette"])]
    cols = 9
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
