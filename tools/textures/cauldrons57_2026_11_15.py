"""N57-3 (b-ig57g, owner 14.11.): Netherite and Enderite cauldron - side and item sprite.

Both start from the approved reinforced cauldron (own art, simplecrucibles reinforced_cauldron_side / item reinforced_cauldron):
the grey body takes the netherite (warm dark grey) or Enderite (deep violet) ramp of the crucibles of the same tier, the turquoise
band takes the crucibles' trim (netherite: gold, Enderite: pale lilac). Colours are mapped by brightness rank, so the shading of
the reinforced cauldron stays pixel for pixel. Top, bottom and inside stay the reinforced cauldron's.

    python3.12 tools/textures/cauldrons57_2026_11_15.py            # writes the four PNGs + layer sources
    python3.12 tools/textures/cauldrons57_2026_11_15.py --preview OUT.png
"""
from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

ROOT = HERE.parents[1]
TEX = ROOT / "modules/simplecrucibles/shared/resources/assets/simplecrucibles/textures"

# body ramps (dark -> light) and trim ramps (dark -> light) of the crucibles of each tier
RAMPS = {
    "netherite": {
        "body": [(30, 25, 27), (42, 36, 38), (49, 41, 42), (60, 50, 50), (67, 64, 67), (76, 65, 67), (86, 76, 78), (98, 88, 90)],
        "trim": [(110, 76, 20), (150, 108, 24), (196, 146, 34), (232, 190, 70), (252, 222, 120)],
    },
    "enderite": {
        "body": [(16, 5, 31), (26, 9, 47), (28, 10, 51), (45, 22, 86), (57, 30, 106), (62, 33, 115), (78, 45, 140), (92, 58, 158)],
        "trim": [(123, 81, 201), (172, 132, 236), (207, 178, 251), (224, 193, 235), (244, 210, 255)],
    },
}


def _is_trim(px):
    r, g, b, a = px
    return a > 0 and b > r + 40 and g > r + 30  # the turquoise band (cyan: low red)


def _lum(px):
    return 0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]


def recolour(img, tier):
    """img recoloured to the tier: body and trim colours by brightness rank onto the ramps."""
    img = img.convert("RGBA")
    px = img.load()
    cols = {px[x, y] for y in range(img.height) for x in range(img.width) if px[x, y][3] > 0}
    body = sorted({c for c in cols if not _is_trim(c)}, key=_lum)
    trim = sorted({c for c in cols if _is_trim(c)}, key=_lum)
    ramp_b, ramp_t = RAMPS[tier]["body"], RAMPS[tier]["trim"]

    def pick(ramp, i, n):
        return ramp[0] if n <= 1 else ramp[round(i * (len(ramp) - 1) / (n - 1))]

    lut = {c: pick(ramp_b, i, len(body)) + (c[3],) for i, c in enumerate(body)}
    lut.update({c: pick(ramp_t, i, len(trim)) + (c[3],) for i, c in enumerate(trim)})
    out = img.copy()
    o = out.load()
    for y in range(img.height):
        for x in range(img.width):
            if px[x, y][3] > 0:
                o[x, y] = lut[px[x, y]]
    return out


def outputs():
    from PIL import Image
    side = Image.open(TEX / "block/reinforced_cauldron_side.png")
    item = Image.open(TEX / "item/reinforced_cauldron.png")
    out = {}
    for tier in RAMPS:
        out[TEX / f"block/{tier}_cauldron_side.png"] = recolour(side, tier)
        out[TEX / f"item/{tier}_cauldron.png"] = recolour(item, tier)
    return out


def main(argv):
    from PIL import Image
    if len(argv) > 1 and argv[0] == "--preview":
        imgs = [Image.open(TEX / "block/reinforced_cauldron_side.png").convert("RGBA"), Image.open(TEX / "item/reinforced_cauldron.png").convert("RGBA")]
        imgs += list(outputs().values())
        s = 12
        sheet = Image.new("RGBA", (len(imgs) * (16 * s + 12) + 12, 16 * s + 24), (60, 60, 66, 255))
        for i, im in enumerate(imgs):
            sheet.alpha_composite(im.resize((16 * s, 16 * s), Image.NEAREST), (12 + i * (16 * s + 12), 12))
        sheet.save(argv[1])
        return 0
    for path, img in outputs().items():
        layers.snap(img, "Umfaerbung")
        layers.save(img, path)
        print("wrote", path.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
