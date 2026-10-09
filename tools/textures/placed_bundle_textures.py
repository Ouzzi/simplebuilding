"""Placed bundle (PlacedBundleBlock): 32x32 texture sheets per tier (plain, dyed leather layer, dyed overlay) and the two
template block models. Texel density 1 texel per model pixel: a model UV unit is two texels.

Roles in the region maps:
  0..4  leather, darkest to lightest
  s S t tie/string (shadow, mid, light)
  a b c d strap (dark .. light)
  x X W crystal (enderite)

Run from the repo root:
  python tools/textures/placed_bundle_textures.py src/main/resources/assets/simplebuilding mc1_21_11/fabric/src/main/resources/assets/simplebuilding
(the 1.21.11 tree gets "render_type": "minecraft:cutout" in the dyed template for NeoForge).
"""
import json, os, sys
from PIL import Image

OUT_DIRS = sys.argv[1:] or ['.']

# ---------------------------------------------------------------------------------------------
# Regions: (name, x, y, rows) - rows are strings of role characters, top row first.
# ---------------------------------------------------------------------------------------------
BELLY_FRONT = ["2333333332",
               "1234443321",
               "1233333321",
               "1223333221",
               "1122222211",
               "0111111110"]
BELLY_BACK = ["2333333332",
              "1233333321",
              "1233333321",
              "1232323221",
              "1122222211",
              "0111111110"]
BELLY_SIDE = ["2333333332",
              "1233443321",
              "1233333321",
              "1223333221",
              "1122222211",
              "0111111110"]
# Rounded footprints (owner 2026-10-09: "von oben nicht quadratisch, leicht abgerundet"): every corner is stepped
# in by STEPS pixels, the belly by two (rows 6, 8, 10, ..., 10, 8, 6 wide), base, shoulder and tuft by one.
STEPS = {'base': 1, 'belly': 2, 'shoulder': 1, 'tuft': 1}


def round_mask(size, steps):
    """True where the rounded footprint covers a size x size square (row = z, column = x)."""
    return [[min(z, size - 1 - z) + min(x, size - 1 - x) >= steps or min(z, size - 1 - z) >= steps
             or min(x, size - 1 - x) >= steps for x in range(size)] for z in range(size)]


def rim(size, steps, edge, inner, corner):
    """Top/bottom face map: ``edge`` along the rounded outline, ``corner`` where it turns, ``inner`` inside."""
    m = round_mask(size, steps)
    def inside(x, z):
        return 0 <= x < size and 0 <= z < size and m[z][x]
    rows = []
    for z in range(size):
        row = ''
        for x in range(size):
            out = sum(not inside(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            row += inner if not m[z][x] or out == 0 else (corner if out >= 2 else edge)
        rows.append(row)
    return rows


BELLY_TOP = rim(10, STEPS['belly'], '4', '3', '3')
BELLY_BOTTOM = rim(10, STEPS['belly'], '1', '0', '1')
BASE_SIDE = ["01111110"]
BASE_BOTTOM = ["0" * 8] * 8
SHOULDER_FRONT = ["23444432"]
SHOULDER_SIDE = ["23444432"]
SHOULDER_TOP = rim(8, STEPS['shoulder'], '4', '3', '3')
TIE_SIDE = ["sStS"]
TUFT_FRONT = ["434343",
              "212121"]
TUFT_SIDE = ["343434",
             "121212"]
TUFT_TOP = ["434434",
            "342243",
            "423324",
            "423324",
            "342243",
            "434434"]
TUFT_BOTTOM = ["1" * 6] * 6
KNOT_FRONT = ["tS",
              "Ss"]
KNOT_SIDE = ["St",
             "sS"]
KNOT_TOP = ["tt",
            "tS"]

# (x, y) of each region on the 32x32 sheet
LAYOUT = {
    'belly_front': (0, 0, BELLY_FRONT),
    'belly_back': (10, 0, BELLY_BACK),
    'belly_side': (20, 0, BELLY_SIDE),
    'belly_top': (0, 6, BELLY_TOP),
    'belly_bottom': (10, 6, BELLY_BOTTOM),
    'base_side': (20, 6, BASE_SIDE),
    'base_bottom': (20, 7, BASE_BOTTOM),
    'shoulder_front': (0, 16, SHOULDER_FRONT),
    'shoulder_side': (8, 16, SHOULDER_SIDE),
    'shoulder_top': (0, 17, SHOULDER_TOP),
    'tie_side': (8, 17, TIE_SIDE),
    'tuft_front': (16, 16, TUFT_FRONT),
    'tuft_side': (16, 18, TUFT_SIDE),
    'tuft_top': (16, 20, TUFT_TOP),
    'tuft_bottom': (22, 16, TUFT_BOTTOM),
    'knot_front': (0, 25, KNOT_FRONT),
    'knot_side': (2, 25, KNOT_SIDE),
    'knot_top': (4, 25, KNOT_TOP),
}

# Tier details painted over the belly: (region, x, y, char)
def details(tier):
    d = []
    if tier == 'bundle':
        # string ends hanging from the knot
        d += [('shoulder_front', 3, 0, 't'), ('shoulder_front', 4, 0, 'S'),
              ('belly_front', 4, 0, 't'), ('belly_front', 5, 0, 'S'),
              ('belly_front', 4, 1, 'S'), ('belly_front', 5, 1, 's'),
              ('belly_front', 4, 2, 'S'), ('belly_front', 5, 3, 's')]
    elif tier == 'reinforced':
        # copper strap with a rivet, rivets on the sides
        for y, (l, r) in enumerate(["dc", "cb", "dc", "cb", "cb", "ba"]):
            d += [('belly_front', 4, y, l), ('belly_front', 5, y, r)]
        d += [('shoulder_front', 3, 0, 'c'), ('shoulder_front', 4, 0, 'b')]
        d += [('belly_back', 4, 1, 'b'), ('belly_back', 5, 1, 'a'), ('belly_back', 4, 2, 'c'), ('belly_back', 5, 2, 'b')]
        d += [('belly_side', 2, 1, 'd'), ('belly_side', 7, 1, 'd'), ('belly_side', 2, 2, 'b'), ('belly_side', 7, 2, 'b')]
    else:
        # pink strap cross like the item
        for y, (l, r) in enumerate(["cb", "cb", "dc", "cb", "cb", "ba"]):
            d += [('belly_front', 4, y, l), ('belly_front', 5, y, r)]
        d += [('shoulder_front', 3, 0, 'c'), ('shoulder_front', 4, 0, 'b')]
        d += [('belly_front', 3, 2, 'b'), ('belly_front', 6, 2, 'a')]
        d += [('belly_back', 4, 1, 'b'), ('belly_back', 5, 1, 'a'), ('belly_back', 4, 2, 'b'), ('belly_back', 5, 2, 'a')]
        if tier == 'enderite':
            d += [('belly_front', 1, 1, 'W'), ('belly_front', 1, 2, 'x'),
                  ('belly_front', 8, 4, 'X'), ('belly_front', 7, 4, 'x'),
                  ('belly_side', 3, 2, 'X'), ('belly_side', 3, 3, 'x'), ('belly_side', 7, 1, 'x'),
                  ('belly_back', 7, 3, 'X'), ('belly_back', 2, 1, 'x'),
                  ('tuft_side', 2, 0, 'x'), ('tuft_front', 4, 0, 'X')]
    return d


def hexc(h):
    h = h.lstrip('#')
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


PALETTES = {
    'bundle': {
        'leather': ['#4f2b10', '#7d4034', '#a6572c', '#bb6936', '#cd7b46'],
        'dyed': ['#5c5c5c', '#8a8a8a', '#b1b1b1', '#d0d0d0', '#ececec'],
        'tie': ['#815634', '#b79963', '#dfc38d'],
    },
    'reinforced': {
        'leather': ['#341c14', '#3f2316', '#642d16', '#8e4b2f', '#a75e3f'],
        'dyed': ['#755a4a', '#826452', '#9f7964', '#bb8f76', '#d7a488'],
        'tie': ['#9d3e19', '#ce7451', '#f79b81'],
        'strap': ['#733d27', '#9d3e19', '#ce7451', '#f2bdac'],
    },
    'netherite': {
        'leather': ['#190c11', '#2d1a24', '#3e2236', '#4e363e', '#59454b'],
        'dyed': ['#58535a', '#6b656d', '#767079', '#87808a', '#9d94a0'],
        'tie': ['#6e3a4f', '#9a6899', '#c194b9'],
        'strap': ['#6e3a4f', '#96577b', '#ba7aa3', '#c194b9'],
    },
    'enderite': {
        'leather': ['#190c11', '#382236', '#4c2c50', '#5f3b5f', '#6e4a70'],
        'dyed': ['#5f5176', '#6f5f8b', '#7f6c9e', '#927cb6', '#ae94da'],
        'tie': ['#6e3a4f', '#9a6899', '#c194b9'],
        'strap': ['#6e3a4f', '#96577b', '#ba7aa3', '#c194b9'],
        'crystal': ['#9660b2', '#c77dff', '#f4d2ff'],
    },
}


def colour(pal, role, layer):
    """layer: 'plain' | 'dyed' | 'overlay'. None = transparent."""
    if role in '01234':
        i = int(role)
        if layer == 'plain':
            return hexc(pal['leather'][i])
        if layer == 'dyed':
            return hexc(pal['dyed'][i])
        return None
    if role in 'sSt':
        c = hexc(pal['tie']['sSt'.index(role)])
    elif role in 'abcd':
        c = hexc(pal['strap']['abcd'.index(role)])
    elif role in 'xXW':
        c = hexc(pal['crystal']['xXW'.index(role)])
    else:
        raise ValueError(role)
    if layer == 'dyed':
        return hexc(pal['dyed'][2])  # hidden under the overlay; keep the leather layer opaque
    return c


def sheet(tier, layer):
    pal = PALETTES[tier]
    grid = [['2'] * 32 for _ in range(32)]  # unused texels: mid leather (particles stay leather coloured)
    for name, (x0, y0, rows) in LAYOUT.items():
        for dy, row in enumerate(rows):
            for dx, ch in enumerate(row):
                grid[y0 + dy][x0 + dx] = ch
    for name, dx, dy, ch in details(tier):
        x0, y0, _ = LAYOUT[name]
        grid[y0 + dy][x0 + dx] = ch
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            c = colour(pal, grid[y][x], layer)
            if c is not None:
                im.putpixel((x, y), c)
    return im


# ---------------------------------------------------------------------------------------------
# Model
# ---------------------------------------------------------------------------------------------
def uv(region, w, h, dx=0, dy=0):
    x0, y0, _ = LAYOUT[region]
    return [(x0 + dx) / 2, (y0 + dy) / 2, (x0 + dx + w) / 2, (y0 + dy + h) / 2]


def face(region, w, h, tex, tint, cull=None, dx=0, dy=0):
    f = {"uv": uv(region, w, h, dx, dy), "texture": tex}
    if tint:
        f["tintindex"] = 0
    if cull:
        f["cullface"] = cull
    return f


def boxes(x0, x1, z0, z1, steps):
    """Non-overlapping boxes of a rounded footprint (no coplanar overlaps, so no z-fighting): the full-width
    middle band, then per step k a k-inset row in front and behind. Returns (bx0, bx1, bz0, bz1, inner face)."""
    out = [(x0, x1, z0 + steps, z1 - steps, None)]
    for k in range(1, steps + 1):
        out.append((x0 + k, x1 - k, z0 + steps - k, z0 + steps - k + 1, 'south'))
        out.append((x0 + k, x1 - k, z1 - steps + k - 1, z1 - steps + k, 'north'))
    return out


def rounded(x0, x1, y0, y1, z0, z1, steps, regions, tex, tint, skip=(), cull=None):
    """One part with a rounded footprint; ``regions`` maps north/south/east/west/up/down to a sheet region of the
    whole part (width x1-x0 resp. z1-z0), each box takes its slice so the faces read as one surface."""
    h = y1 - y0
    E = []
    for bx0, bx1, bz0, bz1, inner in boxes(x0, x1, z0, z1, steps):
        w, d = bx1 - bx0, bz1 - bz0
        offs = {'north': (x1 - bx1, 0, w, h), 'south': (bx0 - x0, 0, w, h), 'west': (bz0 - z0, 0, d, h),
                'east': (z1 - bz1, 0, d, h), 'up': (bx0 - x0, bz0 - z0, w, d), 'down': (bx0 - x0, z1 - bz1, w, d)}
        faces = {}
        for side, (dx, dy, fw, fh) in offs.items():
            if side == inner or side in skip or side not in regions:
                continue
            faces[side] = face(regions[side], fw, fh, tex, tint, cull if side == 'down' else None, dx, dy)
        E.append({"from": [bx0, y0, bz0], "to": [bx1, y1, bz1], "faces": faces})
    return E


def elements(tex, tint):
    E = []
    # base 8x1x8, rounded
    E += rounded(4, 12, 0, 1, 4, 12, STEPS['base'], {s: 'base_side' for s in ('north', 'south', 'east', 'west')}
                 | {'down': 'base_bottom'}, tex, tint, cull='down')
    # belly 10x6x10, rounded by two steps
    E += rounded(3, 13, 1, 7, 3, 13, STEPS['belly'], {'north': 'belly_front', 'south': 'belly_back', 'east': 'belly_side',
                 'west': 'belly_side', 'up': 'belly_top', 'down': 'belly_bottom'}, tex, tint)
    # shoulder 8x1x8, rounded
    E += rounded(4, 12, 7, 8, 4, 12, STEPS['shoulder'], {'north': 'shoulder_front', 'south': 'shoulder_side',
                 'east': 'shoulder_side', 'west': 'shoulder_side', 'up': 'shoulder_top'}, tex, tint)
    # neck 4x1x4: the string around it
    E.append({"from": [6, 8, 6], "to": [10, 9, 10], "faces": {
        "north": face('tie_side', 4, 1, tex, tint), "south": face('tie_side', 4, 1, tex, tint),
        "east": face('tie_side', 4, 1, tex, tint), "west": face('tie_side', 4, 1, tex, tint)}})
    # tuft 6x2x6: the gathered top, closed, rounded
    E += rounded(5, 11, 9, 11, 5, 11, STEPS['tuft'], {'north': 'tuft_front', 'south': 'tuft_side', 'east': 'tuft_side',
                 'west': 'tuft_side', 'up': 'tuft_top', 'down': 'tuft_bottom'}, tex, tint)
    # knot 2x2x2 in front of the neck
    E.append({"from": [7, 8, 4], "to": [9, 10, 6], "faces": {
        "north": face('knot_front', 2, 2, tex, tint), "east": face('knot_side', 2, 2, tex, tint),
        "west": face('knot_side', 2, 2, tex, tint), "up": face('knot_top', 2, 2, tex, tint)}})
    return E


COMMENT = ("Abgestelltes Buendel (PlacedBundleBlock): Boden, Bauch 10x6x10, Schulter, Hals mit Schnur, "
           "gebundener Zipfel und Knoten; von oben abgerundet (Ecken gestuft, Bauch zwei Stufen), "
           "Vorderseite nach Norden. Textur 32x32, ein Texel je Modellpixel (UV = Texel/2); erzeugt von "
           "tools/textures/placed_bundle_textures.py. Die Trefferform in PlacedBundleBlock#NORTH_SHAPE muss zu den Quadern passen.")


def model(dyed, cutout):
    m = {"__comment": COMMENT if not dyed else COMMENT + " Gefaerbt: dieselben Quader zweimal - Leder-Ebene "
         "(#bundle, tintindex 0, Farbe aus der Block-Entity), darueber die ungefaerbte Ebene (#overlay: Schnur, "
         "Riemen, Kristalle).",
         "parent": "block/block", "ambientocclusion": False}
    if dyed and cutout:
        m["render_type"] = "minecraft:cutout"
    m["textures"] = {"particle": "#particle"}
    m["elements"] = elements("#bundle", dyed) + (elements("#overlay", False) if dyed else [])
    m["display"] = {}
    del m["display"]
    return m


def main():
    for d in OUT_DIRS:
        tex_dir = os.path.join(d, 'textures', 'block')
        os.makedirs(tex_dir, exist_ok=True)
        for tier in PALETTES:
            base = 'placed_bundle' if tier == 'bundle' else 'placed_%s_bundle' % tier
            sheet(tier, 'plain').save(os.path.join(tex_dir, base + '.png'))
            sheet(tier, 'dyed').save(os.path.join(tex_dir, base + '_dyed.png'))
            sheet(tier, 'overlay').save(os.path.join(tex_dir, base + '_dyed_overlay.png'))
        model_dir = os.path.join(d, 'models', 'block')
        os.makedirs(model_dir, exist_ok=True)
        cutout = 'mc1_21_11' in d.replace('\\', '/')
        with open(os.path.join(model_dir, 'template_placed_bundle.json'), 'w', newline='\n') as f:
            json.dump(model(False, cutout), f, indent=2)
            f.write('\n')
        with open(os.path.join(model_dir, 'template_placed_bundle_dyed.json'), 'w', newline='\n') as f:
            json.dump(model(True, cutout), f, indent=2)
            f.write('\n')


if __name__ == '__main__':
    main()
