"""Tier chest textures (Reinforced / Netherite / Enderite), owner style 2026-10-01.

Usage: python tools/textures/tier_chest_textures.py <vanilla entity/chest dir> [preview dir]

Vanilla's oak chest keeps its grain and shading: its luminance is mapped onto the tier's material ramp
(the colors of the owner's furnaces and hoppers), every face gets light corner brackets like the owner's
machines, the latch takes the tier accent (diamond, gold, ender glow) and the enderite chest a few ender
sparkles. Writes entity/chest/<tier>.png, <tier>_left.png, <tier>_right.png and the 64x64 particle copy
block/<tier>_chest.png.
"""
import os
import random
import sys
from PIL import Image
from PIL import ImageDraw, ImageFont
from pathlib import Path

ASSETS = "src/main/resources/assets/simplebuilding/textures"
RAMPS = {
    "reinforced": [(36, 36, 40), (52, 52, 56), (70, 70, 74), (90, 90, 94), (112, 112, 116), (138, 138, 142)],
    "netherite": [(30, 24, 26), (44, 37, 39), (60, 52, 54), (77, 69, 71), (96, 88, 90), (118, 110, 112)],
    "enderite": [(24, 14, 40), (38, 22, 64), (54, 32, 92), (74, 44, 128), (98, 62, 168), (128, 88, 210)],
}
BRACKET = {"reinforced": (196, 196, 200), "netherite": (132, 124, 126), "enderite": (150, 110, 228)}
ACCENT = {"reinforced": [(40, 160, 160), (90, 220, 210)], "netherite": [(150, 96, 30), (230, 170, 60)],
          "enderite": [(200, 90, 210), (250, 180, 255)]}
SPARK = (240, 168, 255)


def faces(width):
    """Vanilla chest box UVs: lid (height 5) and base (height 10), depth 14, front width 14 (single) or 15 (half)."""
    out = []
    for y0, h in ((14, 5), (33, 10)):
        x = 0
        for w in (14, width, 14, width):
            out.append((x, y0, w, h)); x += w
    for y0 in (0, 19):
        out += [(14, y0, width, 14), (14 + width, y0, width, 14)]
    return out


def lum(p):
    return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]


def lock(x, y):
    return x < 6 and y < 5


def ramp_map(img, ramp):
    out = img.copy()
    px = [(x, y) for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3] and not lock(x, y)]
    ls = sorted(lum(img.getpixel(p)) for p in px)
    for p in px:
        i = sum(1 for t in ls if t <= lum(img.getpixel(p))) / len(ls)
        out.putpixel(p, ramp[min(len(ramp) - 1, int(i * len(ramp)))] + (img.getpixel(p)[3],))
    return out


def brackets(img, rect, color, size=2):
    x0, y0, w, h = rect
    for cx, cy, dx, dy in ((x0, y0, 1, 1), (x0 + w - 1, y0, -1, 1), (x0, y0 + h - 1, 1, -1), (x0 + w - 1, y0 + h - 1, -1, -1)):
        for i in range(size):
            for x, y in ((cx + dx * i, cy), (cx, cy + dy * i)):
                if 0 <= x < img.width and 0 <= y < img.height and img.getpixel((x, y))[3]:
                    img.putpixel((x, y), color + (255,))


def chest(vanilla, tier, width, seed):
    img = ramp_map(vanilla, RAMPS[tier])
    for rect in faces(width):
        brackets(img, rect, BRACKET[tier])
    dark, light = ACCENT[tier]
    for y in range(5):
        for x in range(6):
            if vanilla.getpixel((x, y))[3]:
                img.putpixel((x, y), (light if lum(vanilla.getpixel((x, y))) > 150 else dark) + (255,))
    if tier == "enderite":
        rnd = random.Random(seed)
        for x0, y0, w, h in faces(width)[:8]:
            img.putpixel((x0 + rnd.randrange(2, w - 2), y0 + rnd.randrange(1, h - 1)), SPARK + (255,))
    return img


def trapped_texture(base, normal, trapped):
    """Use Vanilla's exact accent mask, leaving the owner's tier texture untouched elsewhere."""
    out = base.copy()
    for y in range(base.height):
        for x in range(base.width):
            if normal.getpixel((x, y)) != trapped.getpixel((x, y)):
                light = lum(base.getpixel((x, y)))
                out.putpixel((x, y), (min(210, round(64 + light * .65)),
                                     round(20 + light * .12), round(18 + light * .10),
                                     base.getpixel((x, y))[3]))
    return out


def front(img, width=14):
    """Flat front from Vanilla's lid/base UVs; nearest-neighbor previews only."""
    out = Image.new("RGBA", (width, 15))
    x = 28 + width
    out.paste(img.crop((x, 14, x + width, 19)).transpose(Image.Transpose.ROTATE_180), (0, 0))
    out.paste(img.crop((x, 33, x + width, 43)).transpose(Image.Transpose.ROTATE_180), (0, 5))
    # Front face of the separate latch cuboid; double halves carry one pixel each.
    latch_width = 2 if width == 14 else 1
    latch = img.crop((1, 1, 1 + latch_width, 5)).transpose(Image.Transpose.ROTATE_180)
    out.alpha_composite(latch, ((width - latch_width) // 2, 3))
    return out


def trapped_main():
    import argparse
    parser = argparse.ArgumentParser(description="Derive 26.3 trapped chests from the existing owner textures.")
    parser.add_argument("--trapped", action="store_true")
    parser.add_argument("--vanilla", required=True, type=Path, help="Vanilla entity/chest directory")
    parser.add_argument("--preview", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    target = Path("mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest")
    rows = []
    vanilla = {}
    for suffix in ("", "_left", "_right"):
        vanilla[suffix] = tuple(Image.open(args.vanilla / f"{kind}{suffix}.png").convert("RGBA")
                                for kind in ("normal", "trapped"))
    rows.append(("A  Vanilla", vanilla[""][0], vanilla[""][1],
                 vanilla["_left"][0], vanilla["_right"][0], vanilla["_left"][1], vanilla["_right"][1]))
    for index, tier in enumerate(RAMPS):
        originals, outputs = {}, {}
        for suffix in ("", "_left", "_right"):
            source = Path(ASSETS) / "entity/chest" / f"{tier}{suffix}.png"
            overlay = target / source.name
            base = Image.open(overlay if overlay.exists() else source).convert("RGBA")
            result = trapped_texture(base, *vanilla[suffix])
            path = target / f"{tier}_trapped{suffix}.png"
            if args.check:
                assert path.exists() and Image.open(path).convert("RGBA").tobytes() == result.tobytes(), f"stale texture: {path}"
            else:
                target.mkdir(parents=True, exist_ok=True)
                result.save(path)
            originals[suffix], outputs[suffix] = base, result
        rows.append((f"{chr(66 + index)}  {tier.title()}", originals[""], outputs[""],
                     originals["_left"], originals["_right"], outputs["_left"], outputs["_right"]))
    if args.preview:
        scale = 16
        canvas = Image.new("RGB", (1580, 1320), "#20242b")
        draw = ImageDraw.Draw(canvas)
        try:
            font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 22)
        except OSError:
            font = ImageFont.load_default(size=22)
        draw.text((24, 14), "Redstone-Truhen | Original / Rotakzent | 16x, ungefiltert", font=font, fill="white")
        for x, label in [(24, "Einzeltruhe normal"), (280, "Redstone"), (550, "Doppeltruhe normal"), (1060, "Redstone")]:
            draw.text((x, 49), label, font=font, fill="#cbd5e1")
        for row, (label, normal, trapped, left, right, tleft, tright) in enumerate(rows):
            y = 100 + row * 300
            draw.text((24, y), label, font=font, fill="white")
            for x, img in [(24, front(normal)), (280, front(trapped))]:
                preview = img.resize((img.width * scale, img.height * scale), Image.Resampling.NEAREST)
                canvas.paste(preview, (x, y + 34), preview)
            for x, a, b in [(550, left, right), (1060, tleft, tright)]:
                # Latches meet at the seam of the two halves.
                pair = Image.new("RGBA", (30, 15))
                for offset, source in [(0, b), (15, a)]:
                    # Chest LEFT/RIGHT are named from behind; front view reverses their order.
                    face = Image.new("RGBA", (15, 15))
                    face.paste(source.crop((43, 14, 58, 19)).transpose(Image.Transpose.ROTATE_180), (0, 0))
                    face.paste(source.crop((43, 33, 58, 43)).transpose(Image.Transpose.ROTATE_180), (0, 5))
                    pair.paste(face, (offset, 0))
                pair.alpha_composite(a.crop((1, 1, 2, 5)).transpose(Image.Transpose.ROTATE_180), (14, 3))
                pair.alpha_composite(b.crop((1, 1, 2, 5)).transpose(Image.Transpose.ROTATE_180), (15, 3))
                preview = pair.resize((480, 240), Image.Resampling.NEAREST)
                canvas.paste(preview, (x, y + 34), preview)
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        canvas.save(args.preview)
    print("trapped chest textures: 9/9 current" if args.check else "trapped chest textures: 9 generated")


def main():
    src = sys.argv[1]
    preview = sys.argv[2] if len(sys.argv) > 2 else None
    for tier in RAMPS:
        single = chest(Image.open(os.path.join(src, "normal.png")).convert("RGBA"), tier, 14, 7)
        single.save(f"{ASSETS}/entity/chest/{tier}.png")
        for side, seed in (("left", 11), ("right", 13)):
            chest(Image.open(os.path.join(src, f"normal_{side}.png")).convert("RGBA"), tier, 15, seed) \
                .save(f"{ASSETS}/entity/chest/{tier}_{side}.png")
        if os.path.exists(f"{ASSETS}/block/{tier}_chest.png"):
            single.save(f"{ASSETS}/block/{tier}_chest.png")
        if preview:
            os.makedirs(preview, exist_ok=True)
            single.resize((256, 256), Image.NEAREST).save(os.path.join(preview, f"chest_{tier}.png"))
    print("ok")


if __name__ == "__main__":
    trapped_main() if "--trapped" in sys.argv else main()
