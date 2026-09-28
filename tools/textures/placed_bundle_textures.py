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
BELLY_TOP = ["3444444443"] + ["4" + "3" * 8 + "4"] * 8 + ["3444444443"]
BELLY_BOTTOM = ["1111111111"] + ["1" + "0" * 8 + "1"] * 8 + ["1111111111"]
BASE_SIDE = ["01111110"]
BASE_BOTTOM = ["0" * 8] * 8
SHOULDER_FRONT = ["23444432"]
SHOULDER_SIDE = ["23444432"]
SHOULDER_TOP = ["34444443"] + ["4" + "3" * 6 + "4"] * 6 + ["34444443"]
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


def face(region, w, h, tex, tint, cull=None):
    f = {"uv": uv(region, w, h), "texture": tex}
    if tint:
        f["tintindex"] = 0
    if cull:
        f["cullface"] = cull
    return f


def elements(tex, tint):
    E = []
    # base 8x1x8
    E.append({"from": [4, 0, 4], "to": [12, 1, 12], "faces": {
        "north": face('base_side', 8, 1, tex, tint), "south": face('base_side', 8, 1, tex, tint),
        "east": face('base_side', 8, 1, tex, tint), "west": face('base_side', 8, 1, tex, tint),
        "down": face('base_bottom', 8, 8, tex, tint, "down")}})
    # belly 10x6x10
    E.append({"from": [3, 1, 3], "to": [13, 7, 13], "faces": {
        "north": face('belly_front', 10, 6, tex, tint), "south": face('belly_back', 10, 6, tex, tint),
        "east": face('belly_side', 10, 6, tex, tint), "west": face('belly_side', 10, 6, tex, tint),
        "up": face('belly_top', 10, 10, tex, tint), "down": face('belly_bottom', 10, 10, tex, tint)}})
    # shoulder 8x1x8
    E.append({"from": [4, 7, 4], "to": [12, 8, 12], "faces": {
        "north": face('shoulder_front', 8, 1, tex, tint), "south": face('shoulder_side', 8, 1, tex, tint),
        "east": face('shoulder_side', 8, 1, tex, tint), "west": face('shoulder_side', 8, 1, tex, tint),
        "up": face('shoulder_top', 8, 8, tex, tint)}})
    # neck 4x1x4: the string around it
    E.append({"from": [6, 8, 6], "to": [10, 9, 10], "faces": {
        "north": face('tie_side', 4, 1, tex, tint), "south": face('tie_side', 4, 1, tex, tint),
        "east": face('tie_side', 4, 1, tex, tint), "west": face('tie_side', 4, 1, tex, tint)}})
    # tuft 6x2x6: the gathered top, closed
    E.append({"from": [5, 9, 5], "to": [11, 11, 11], "faces": {
        "north": face('tuft_front', 6, 2, tex, tint), "south": face('tuft_side', 6, 2, tex, tint),
        "east": face('tuft_side', 6, 2, tex, tint), "west": face('tuft_side', 6, 2, tex, tint),
        "up": face('tuft_top', 6, 6, tex, tint), "down": face('tuft_bottom', 6, 6, tex, tint)}})
    # knot 2x2x2 in front of the neck
    E.append({"from": [7, 8, 4], "to": [9, 10, 6], "faces": {
        "north": face('knot_front', 2, 2, tex, tint), "east": face('knot_side', 2, 2, tex, tint),
        "west": face('knot_side', 2, 2, tex, tint), "up": face('knot_top', 2, 2, tex, tint)}})
    return E


COMMENT = ("Abgestelltes Buendel (PlacedBundleBlock): Boden, Bauch 10x6x10, Schulter, Hals mit Schnur, "
           "gebundener Zipfel und Knoten, Vorderseite nach Norden. Textur 32x32, ein Texel je Modellpixel (UV = Texel/2); erzeugt von "
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
