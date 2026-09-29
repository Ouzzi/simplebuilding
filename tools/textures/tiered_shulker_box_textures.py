"""Tier shulker boxes (Reinforced / Netherite / Enderite): the 64x64 entity textures, the palettes that
dye them, and the 16x16 particle textures.

One texture per tier, dyed by the game: the shell is painted only in the seven key colors K0..K6
(textures/palettes/shulker/key.png, vanilla's undyed shulker purple), the tier's plating in colors of
its own. assets/minecraft/atlases/shulker_boxes.json has a paletted_permutations source that paints
<tier>_<color> for all sixteen dye colors from that one texture, each palette taken from vanilla's
dyed shulker texture of that color (the same luminance quantiles as the key). The undyed box is the
texture itself (vanilla's directory source finds it).

The geometry (which texels exist: the notch in the lid, the teeth of the base, the open inner faces)
is vanilla's: the alpha of entity/shulker/shulker.png. Everything visible is new pixel art.

Faces are character grids, top row first, 16 texels wide:
  0..6   shell, key color K0 (darkest) .. K6 (lightest) - dyed by the palette
  o      plating outline
  a..f   plating metal, dark .. light
  x y z  plating accent (diamond / gold / ender glow), dark .. light
  space  no plating: the shell shows

Run from the repo root (reads the vanilla textures out of the Minecraft jar):
  python tools/textures/tiered_shulker_box_textures.py <path to a 26.x client jar> [preview dir]
"""
import io
import os
import sys
import zipfile

from PIL import Image

ASSETS = "src/main/resources/assets/simplebuilding"
TIERS = ("reinforced", "netherite", "enderite")
DYES = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
        "purple", "blue", "brown", "green", "red", "black")

PLATING = {
    # Reinforced: steel with diamond studs, like the Reinforced Chest.
    "reinforced": {"o": (40, 40, 47), "a": (62, 62, 70), "b": (78, 78, 87), "c": (99, 99, 106), "d": (122, 122, 129),
                   "e": (156, 156, 163), "f": (196, 196, 202), "x": (39, 177, 163), "y": (95, 224, 208), "z": (196, 250, 240)},
    # Netherite: dark netherite plates with gold rivets, like the Netherite Chest.
    "netherite": {"o": (17, 12, 13), "a": (36, 29, 30), "b": (51, 43, 44), "c": (66, 58, 59), "d": (86, 79, 81),
                  "e": (113, 107, 109), "f": (140, 134, 137), "x": (122, 79, 24), "y": (196, 140, 44), "z": (243, 213, 122)},
    # Enderite: night-violet plates with a glowing violet rim, like the Enderite Chest.
    "enderite": {"o": (11, 6, 19), "a": (24, 15, 38), "b": (36, 24, 56), "c": (50, 35, 78), "d": (66, 44, 108),
                 "e": (88, 58, 146), "f": (112, 80, 180), "x": (100, 64, 176), "y": (138, 104, 214), "z": (196, 170, 248)},
}

# ---------------------------------------------------------------------------------------------
# Shell (shared by all tiers). Painted per face from these grids; plating goes on top.
# ---------------------------------------------------------------------------------------------
LID_TOP_SHELL = [
    "5666666666666665",
    "6555555555555554",
    "6543333333333424",
    "6532222222223324",
    "6532444443423324",
    "6532433333423324",
    "6532433233423324",
    "6532432223423324",
    "6532432223423324",
    "6532433233423324",
    "6532433333423324",
    "6532444444423324",
    "6533322222333324",
    "6533333333333324",
    "5444444444444443",
    "4333333333333332",
]
LID_SIDE_SHELL = [
    "6666666666666665",
    "5555555555555554",
    "5444444444444443",
    "5433343333343343",
    "5433333433333343",
    "5434333333433343",
    "5433333333333343",
    "5333333333333332",
    "4333222222223332",
    "4332111111112332",
    "4322100000012232",
    "3211000000001121",
]
BASE_SIDE_SHELL = [
    "5444544445444454",
    "5443444444434453",
    "5433333333333343",
    "4333433334333332",
    "4333333333333332",
    "4323333233332332",
    "3222222222222221",
    "2111111111111110",
]
BASE_BOTTOM_SHELL = [
    "3333333333333332",
    "3222222222222221",
    "3211111111111121",
    "3212222222222121",
    "3212111111112121",
    "3212122222212121",
    "3212121111212121",
    "3212121221212121",
    "3212121221212121",
    "3212121111212121",
    "3212122222212121",
    "3212111111112121",
    "3212222222222121",
    "3211111111111121",
    "3222222222222221",
    "2111111111111110",
]
INNER_SHELL = ["1" * 16] * 16

# ---------------------------------------------------------------------------------------------
# Plating per tier (space = shell shows).
# ---------------------------------------------------------------------------------------------
PLATE = {
    "reinforced": {
        "lid_top": [
            "feeeeeeeeeeeeeed",
            "eyb          bxa",
            "eb            ba",
            "e              a",
            "e              a",
            "e              a",
            "e      dd      a",
            "e     dzyb     a",
            "e     dyxb     a",
            "e      bb      a",
            "e              a",
            "e              a",
            "e              a",
            "eb            ba",
            "exb          bxa",
            "daaaaaaaaaaaaaao",
        ],
        "lid_side": [
            "feeeeeeeeeeeeeed",
            "dccccccccccccccb",
            "ec            ba",
            "eb            ba",
            "eb            ba",
            "eb    deed    ba",
            "eb    dyzb    ba",
            "eb    cxyb    ba",
            "eb    bccb    ba",
            "eb            ba",
            "eb            ba",
            "db            ao",
        ],
        "base_side": [
            "eb            ba",
            "eb            ba",
            "eb            ba",
            "eb            ba",
            "eb            ba",
            "edddddddddddddda",
            "dccccccccccccccb",
            "obbbbbbbbbbbbbbo",
        ],
        "base_bottom": [
            "dccccccccccccccb",
            "cy            xb",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "c              b",
            "cx            xa",
            "bbbbbbbbbbbbbbba",
        ],
    },
    "netherite": {
        "lid_top": [
            "dddddddddddddddc",
            "dzb          bxb",
            "db            bb",
            "d              b",
            "d              b",
            "d              b",
            "d      yy      b",
            "d     yzyx     b",
            "d     yyxx     b",
            "d      xx      b",
            "d              b",
            "d              b",
            "d              b",
            "db            bb",
            "dxb          bxb",
            "cbbbbbbbbbbbbbbo",
        ],
        "lid_side": [
            "eddddddddddddddc",
            "dccccccccccccccb",
            "dzc          byb",
            "dcb          bba",
            "dcb          bba",
            "dcb   dddc   bba",
            "dcb   dyzb   bba",
            "dcb   cyxb   bba",
            "dcb   bbba   bba",
            "dcb          bba",
            "dyb          bxa",
            "cbb          bao",
        ],
        "base_side": [
            "dcb          bba",
            "dyb          bxa",
            "dcb          bba",
            "dcb          bba",
            "dcb          bba",
            "dddddddddddddddc",
            "dccccczyyxbbbbba",
            "obbbbbbbbbbbbbao",
        ],
        "base_bottom": [
            "dddddddddddddddc",
            "dzy          yxb",
            "dy            xb",
            "d              b",
            "d              b",
            "d              b",
            "d              b",
            "d              b",
            "d              b",
            "d              b",
            "d              b",
            "d              b",
            "d              b",
            "dy            xb",
            "dxx          xxa",
            "cbbbbbbbbbbbbbba",
        ],
    },
    "enderite": {
        "lid_top": [
            "fyyyyyyyyyyyyyyd",
            "yzc          cxx",
            "yc            cx",
            "y              x",
            "y              x",
            "y              x",
            "y      xx      x",
            "y     xzyx     x",
            "y     xyxx     x",
            "y      xx      x",
            "y              x",
            "y              x",
            "y              x",
            "yc            cx",
            "yxc          cxx",
            "xxxxxxxxxxxxxxxo",
        ],
        "lid_side": [
            "fyyyyyyyyyyyyyyd",
            "yeeeeeeeeeeeeeex",
            "ye            dx",
            "yd            dx",
            "yd            dx",
            "yd    dccd    dx",
            "yd    dzyc    dx",
            "yd    cyxc    dx",
            "yd    cccb    dx",
            "yd            dx",
            "yd            dx",
            "dd            do",
        ],
        "base_side": [
            "yd            dx",
            "yd            dx",
            "yd            dx",
            "yd            dx",
            "yd            dx",
            "yyyyyyyyyyyyyyyx",
            "deeeeeeeeeeeeeed",
            "oxxxxxxxxxxxxxxo",
        ],
        "base_bottom": [
            "yyyyyyyyyyyyyyyx",
            "yz            xx",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "y              x",
            "xx            xx",
            "xxxxxxxxxxxxxxxo",
        ],
    },
}

# Face placement in the 64x64 shulker layout: (x, y, width, height, shell grid, plating key or None).
LID_W, LID_H, BASE_H = 16, 12, 8


def faces():
    out = [(16, 0, 16, 16, LID_TOP_SHELL, "lid_top"), (32, 0, 16, 16, INNER_SHELL, None),
           (16, 28, 16, 16, INNER_SHELL, None), (32, 28, 16, 16, BASE_BOTTOM_SHELL, "base_bottom")]
    for i in range(4):
        out.append((16 * i, 16, 16, LID_H, LID_SIDE_SHELL, "lid_side"))
        out.append((16 * i, 44, 16, BASE_H, BASE_SIDE_SHELL, "base_side"))
    return out


# ---------------------------------------------------------------------------------------------
# Palettes
# ---------------------------------------------------------------------------------------------
def luminance(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def quantile_palette(image, count=7):
    """count colors of the box part of a vanilla shulker texture, spread evenly over its luminance."""
    pixels = []
    for y in range(52):
        for x in range(64):
            p = image.getpixel((x, y))
            if p[3] > 0:
                pixels.append(p[:3])
    pixels.sort(key=luminance)
    # Leave out the extreme 3 % on both ends (single highlight / shadow texels).
    lo, hi = int(len(pixels) * 0.03), int(len(pixels) * 0.97)
    body = pixels[lo:hi]
    return [body[min(len(body) - 1, int((i + 0.5) / count * len(body)))] for i in range(count)]


def unique_key(colors):
    """The key colors must differ from each other; nudge duplicates by one step of green."""
    out = []
    for c in colors:
        c = tuple(c)
        while c in out:
            c = (c[0], min(255, c[1] + 1), c[2])
        out.append(c)
    return out


# ---------------------------------------------------------------------------------------------

def load_vanilla(jar):
    with zipfile.ZipFile(jar) as z:
        def img(name):
            return Image.open(io.BytesIO(z.read("assets/minecraft/textures/entity/shulker/" + name + ".png"))).convert("RGBA")
        return img("shulker"), {dye: img("shulker_" + dye) for dye in DYES}


def paint(tier, key, mask):
    colors = PLATING[tier]
    for c in colors.values():
        assert c not in key, f"{tier}: plating color {c} is a key color and would be dyed"
    image = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x0, y0, w, h, shell, plate_key in faces():
        plate = PLATE[tier][plate_key] if plate_key else None
        assert len(shell) >= h and all(len(r) == 16 for r in shell), shell
        if plate:
            assert len(plate) == h and all(len(r) == 16 for r in plate), (tier, plate_key)
        for y in range(h):
            for x in range(w):
                if mask.getpixel((x0 + x, y0 + y))[3] == 0:
                    continue
                ch = plate[y][x] if plate and plate[y][x] != " " else shell[y][x]
                rgb = key[int(ch)] if ch.isdigit() else colors[ch]
                image.putpixel((x0 + x, y0 + y), rgb + (255,))
    return image


def recolor(image, key, palette):
    mapping = dict(zip(key, palette))
    out = image.copy()
    for y in range(out.height):
        for x in range(out.width):
            p = out.getpixel((x, y))
            if p[3] and p[:3] in mapping:
                out.putpixel((x, y), mapping[p[:3]] + (p[3],))
    return out


def front(image):
    """The closed box seen from one side: the lid side over the base side, the base showing through the notch."""
    face = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    base = image.crop((0, 44, 16, 52))
    face.paste(base, (0, 8))
    lid = image.crop((0, 16, 16, 28))
    face.alpha_composite(lid, (0, 0))
    return face


def particle(image):
    return front(image)


def main():
    jar = sys.argv[1]
    preview = sys.argv[2] if len(sys.argv) > 2 else None
    vanilla, dyed = load_vanilla(jar)
    mask = vanilla
    key = unique_key(quantile_palette(vanilla))
    palettes = {dye: quantile_palette(dyed[dye]) for dye in DYES}

    os.makedirs(f"{ASSETS}/textures/entity/shulker", exist_ok=True)
    os.makedirs(f"{ASSETS}/textures/palettes/shulker", exist_ok=True)
    os.makedirs(f"{ASSETS}/textures/block", exist_ok=True)
    strip = Image.new("RGBA", (len(key), 1))
    for i, c in enumerate(key):
        strip.putpixel((i, 0), c + (255,))
    strip.save(f"{ASSETS}/textures/palettes/shulker/key.png")
    for dye, pal in palettes.items():
        img = Image.new("RGBA", (len(pal), 1))
        for i, c in enumerate(pal):
            img.putpixel((i, 0), c + (255,))
        img.save(f"{ASSETS}/textures/palettes/shulker/{dye}.png")

    textures = {}
    for tier in TIERS:
        tex = paint(tier, key, mask)
        textures[tier] = tex
        tex.save(f"{ASSETS}/textures/entity/shulker/{tier}.png")
        particle(tex).save(f"{ASSETS}/textures/block/{tier}_shulker_box.png")

    if preview:
        os.makedirs(preview, exist_ok=True)
        write_preview(preview, vanilla, dyed, textures, key, palettes)


def write_preview(directory, vanilla, dyed, textures, key, palettes):
    scale = 8
    shown = [None, "red", "blue", "lime", "black", "white", "yellow"]

    def big(img, s=scale):
        return img.resize((img.width * s, img.height * s), Image.NEAREST)

    rows = []
    # Row 0: vanilla undyed/red for comparison.
    rows.append([("vanilla", vanilla), ("vanilla red", dyed["red"])])
    for tier, tex in textures.items():
        row = []
        for dye in shown:
            row.append((f"{tier} {dye or 'undyed'}", tex if dye is None else recolor(tex, key, palettes[dye])))
        rows.append(row)
    # Sheet 1: close-up of each closed box (front + top), 8x.
    cell = 16 * scale
    sheet = Image.new("RGBA", (len(shown) * (cell + 8) + 8, (len(rows)) * (2 * cell + 16) + 8), (48, 48, 48, 255))
    for r, row in enumerate(rows):
        for c, (_, img) in enumerate(row):
            x = 8 + c * (cell + 8)
            y = 8 + r * (2 * cell + 16)
            top = img.crop((16, 0, 32, 16))
            sheet.alpha_composite(big(top), (x, y))
            sheet.alpha_composite(big(front(img)), (x, y + cell + 4))
    sheet.save(os.path.join(directory, "tiered_shulker_boxes_faces.png"))
    # Sheet 2: the three full textures at 6x.
    full = Image.new("RGBA", (3 * (64 * 6 + 12) + 12, 64 * 6 + 24), (48, 48, 48, 255))
    for i, tex in enumerate(textures.values()):
        full.alpha_composite(big(tex, 6), (12 + i * (64 * 6 + 12), 12))
    full.save(os.path.join(directory, "tiered_shulker_boxes_textures.png"))
    # Sheet 3: in-game-ish size (4x) of all 17 colors per tier, front faces.
    small = Image.new("RGBA", (17 * 72 + 8, 3 * 72 + 8), (48, 48, 48, 255))
    for r, (tier, tex) in enumerate(textures.items()):
        for c, dye in enumerate([None] + list(DYES)):
            img = tex if dye is None else recolor(tex, key, palettes[dye])
            small.alpha_composite(big(front(img), 4), (8 + c * 72, 8 + r * 72))
    small.save(os.path.join(directory, "tiered_shulker_boxes_colors.png"))


if __name__ == "__main__":
    main()
