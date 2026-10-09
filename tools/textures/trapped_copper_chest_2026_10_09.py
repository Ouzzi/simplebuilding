"""Trapped copper chest overlays (owner N16, 2026-10-09).

Usage: python tools/textures/trapped_copper_chest_2026_10_09.py --vanilla <vanilla entity/chest dir> [--preview out.png] [--check]

The trapped copper chest draws Vanilla's own copper chest sprite and, on top, an overlay that holds only the
pixels Vanilla changes for its trapped chest (the small hook below the latch), with the same subtle red shift as
the mod's trapped tier chests (tier_chest_textures.trapped_texture, owner 2026-10-07: "viel zu auffaellig").
One overlay per oxidation stage and chest shape, so the hint keeps the colour of its copper. No Vanilla texture
is written to the repo: every overlay is transparent except those few shifted pixels.
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

TARGET = Path("mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest")
STAGES = ["", "_exposed", "_weathered", "_oxidized"]
SHAPES = ["", "_left", "_right"]


def overlay(copper, normal, trapped):
    out = Image.new("RGBA", copper.size, (0, 0, 0, 0))
    for y in range(copper.height):
        for x in range(copper.width):
            a, b = normal.getpixel((x, y)), trapped.getpixel((x, y))
            if a == b:
                continue
            shift = max(0, (b[0] - a[0]) - (b[1] - a[1])) * 0.6
            r, g, bl, alpha = copper.getpixel((x, y))
            if not alpha:
                continue
            out.putpixel((x, y), (min(255, round(r + shift)), max(0, round(g - shift / 2)), max(0, round(bl - shift / 2)), 255))
    return out


def front(img, width=14):
    """Flat front view from the chest UVs (lid and base faces, latch), as in tier_chest_textures.front."""
    out = Image.new("RGBA", (width, 15))
    x = 28 + width
    out.paste(img.crop((x, 14, x + width, 19)).transpose(Image.Transpose.ROTATE_180), (0, 0))
    out.paste(img.crop((x, 33, x + width, 43)).transpose(Image.Transpose.ROTATE_180), (0, 5))
    latch_width = 2 if width == 14 else 1
    latch = img.crop((1, 1, 1 + latch_width, 5)).transpose(Image.Transpose.ROTATE_180)
    out.alpha_composite(latch, ((width - latch_width) // 2, 3))
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--vanilla", required=True, type=Path, help="Vanilla entity/chest directory")
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    hints = {s: tuple(Image.open(args.vanilla / f"{k}{s}.png").convert("RGBA") for k in ("normal", "trapped")) for s in SHAPES}
    rows = []
    for stage in STAGES:
        row = []
        for shape in SHAPES:
            copper = Image.open(args.vanilla / f"copper{stage}{shape}.png").convert("RGBA")
            result = overlay(copper, *hints[shape])
            path = TARGET / f"trapped_copper{stage}{shape}.png"
            if args.check:
                assert path.exists() and Image.open(path).convert("RGBA").tobytes() == result.tobytes(), f"stale texture: {path}"
            else:
                TARGET.mkdir(parents=True, exist_ok=True)
                result.save(path)
            composed = copper.copy()
            composed.alpha_composite(result)
            row.append((copper, composed))
        rows.append((stage.strip("_") or "unaffected", row))
    if args.preview:
        scale = 16
        canvas = Image.new("RGB", (1180, 40 + 4 * 300), "#20242b")
        draw = ImageDraw.Draw(canvas)
        try:
            font = ImageFont.truetype("DejaVuSans.ttf", 22)
        except OSError:
            font = ImageFont.load_default(size=22)
        draw.text((24, 10), "Fallen-Kupfertruhe | Kupfertruhe / mit Fallen-Overlay | 16x, ungefiltert", font=font, fill="white")
        for i, (label, row) in enumerate(rows):
            y = 50 + i * 300
            draw.text((24, y), label, font=font, fill="white")
            for x, img in [(24, row[0][0]), (280, row[0][1])]:
                big = front(img).resize((14 * scale, 15 * scale), Image.Resampling.NEAREST)
                canvas.paste(big, (x, y + 34), big)
            pair = Image.new("RGBA", (30, 15))
            left, right = row[1][1], row[2][1]
            for offset, source in [(0, right), (15, left)]:
                face = Image.new("RGBA", (15, 15))
                face.paste(source.crop((43, 14, 58, 19)).transpose(Image.Transpose.ROTATE_180), (0, 0))
                face.paste(source.crop((43, 33, 58, 43)).transpose(Image.Transpose.ROTATE_180), (0, 5))
                pair.paste(face, (offset, 0))
            pair.alpha_composite(left.crop((1, 1, 2, 5)).transpose(Image.Transpose.ROTATE_180), (14, 3))
            pair.alpha_composite(right.crop((1, 1, 2, 5)).transpose(Image.Transpose.ROTATE_180), (15, 3))
            big = pair.resize((480, 240), Image.Resampling.NEAREST)
            canvas.paste(big, (560, y + 34), big)
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        canvas.save(args.preview)
    print("trapped copper chest overlays: 12/12 current" if args.check else "trapped copper chest overlays: 12 generated")


if __name__ == "__main__":
    main()
