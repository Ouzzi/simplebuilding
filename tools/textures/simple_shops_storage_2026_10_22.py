"""Sales Storage and display case glass textures (Queue SP22-7 + TX22-1 glass, branch b-shops22).

Own pixel art in layers (PRINZIPIEN A8), same wood/iron/gold palette as simple_shops_2026_10_15.py, light from the top left.
The storage is a directed full block that faces its sales block (model like Vanilla's template_piston):

* sales_storage_side: row 0 is the edge at the sales block. An iron collar clamps onto the sales block, two iron funnel walls
  run together towards the back, and a gold stream inside is brightest at the intake (the earnings come out of the sales
  block, like the light rising in a beacon).
* sales_storage_intake: the face on the sales block (seen in the inventory): iron collar ring, dark mouth, gold core.
* sales_storage_back: the far face, a banded crate lid with the brass lock (the old crate front without its arrow).
* display_case_glass: clearer glass (tint alpha 24 instead of 70) with two Vanilla-like light streaks; the model now also uses
  it for the lid, so the goods are visible from above.

Pure Python (tools/textures/layers.py, no Pillow): every PNG is the composition of its <name>.layers.json.
Run: python3 tools/textures/simple_shops_storage_2026_10_22.py [--check]
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layers as L  # noqa: E402

ASSETS = os.path.join(HERE, '..', '..', 'modules', 'simpleshops', 'shared', 'resources', 'assets', 'simpleshops', 'textures', 'block')

WOOD = [(36, 24, 14), (56, 38, 21), (78, 54, 30), (100, 72, 40), (124, 90, 52)]
IRON = [(58, 60, 66), (96, 100, 108), (136, 140, 148), (184, 188, 196), (224, 228, 234)]
GOLD = [(140, 92, 20), (206, 154, 38), (248, 208, 84), (255, 242, 168)]


class Layer:
    def __init__(self, name):
        self.name = name
        self.px = bytearray(16 * 16 * 4)

    def put(self, x, y, c, a=255):
        if 0 <= x < 16 and 0 <= y < 16:
            o = (y * 16 + x) * 4
            self.px[o:o + 4] = bytes((c[0], c[1], c[2], c[3] if len(c) == 4 else a))


def iron_rim(lay, top=True):
    """The sales block's rim: lit left (and top) edge, dark right and bottom edge."""
    for i in range(16):
        if top:
            lay.put(i, 0, IRON[4] if i % 8 == 0 else IRON[3])
        lay.put(0, i, IRON[4] if i % 8 == 0 else IRON[3])
        lay.put(i, 15, IRON[1])
        lay.put(15, i, IRON[1])
    lay.put(15, 15, IRON[0])


def side():
    wood, iron, gold = Layer('Holz'), Layer('Eisen'), Layer('Gold')
    # Vertical boards along the flow (seams every 4 columns), grain spots, a lit row under the collar.
    for y in range(4, 16):
        for x in range(16):
            c = WOOD[3]
            if x % 4 == 3:
                c = WOOD[1]
            elif (x * 5 + y * 3) % 7 == 0:
                c = WOOD[2]
            elif y == 4:
                c = WOOD[4] if x % 4 != 2 else WOOD[3]
            wood.put(x, y, c)
    # Collar at the sales block: lit top edge, rivets, shadow line.
    for x in range(16):
        iron.put(x, 0, IRON[4] if x % 8 == 0 else IRON[3])
        iron.put(x, 1, IRON[2] if 0 < x < 15 else IRON[3] if x == 0 else IRON[1])
        iron.put(x, 2, IRON[2] if 0 < x < 15 else IRON[3] if x == 0 else IRON[1])
        iron.put(x, 3, IRON[1] if x else IRON[2])
    for x in (2, 13):
        iron.put(x, 1, IRON[4])
        iron.put(x, 2, GOLD[1])
        iron.put(x + 1, 2, IRON[1])
    # Frame around the rest of the face (the far edge is row 15).
    for y in range(4, 16):
        iron.put(0, y, IRON[3])
        iron.put(15, y, IRON[1])
    for x in range(16):
        iron.put(x, 15, IRON[1])
    iron.put(0, 15, IRON[2])
    iron.put(15, 15, IRON[0])
    # Funnel walls: two iron strips running together from the collar to a narrow spout at the back.
    for y in range(4, 14):
        lx = 1 + (y - 4) // 2
        rx = 14 - (y - 4) // 2
        iron.put(lx, y, IRON[3])
        iron.put(lx + 1, y, IRON[2])
        iron.put(rx, y, IRON[1])
        iron.put(rx - 1, y, IRON[2])
        for x in range(lx + 2, rx - 1):
            iron.put(x, y, WOOD[0] if y == 4 or x == rx - 2 else WOOD[1])
    for x in range(5, 11):
        iron.put(x, 14, IRON[2] if x in (5, 10) else IRON[1])
    # The gold stream: brightest at the intake, fading towards the back.
    ramp = {4: GOLD[3], 5: GOLD[3], 6: GOLD[2], 7: GOLD[2], 8: GOLD[2], 9: GOLD[1], 10: GOLD[1], 11: GOLD[1], 12: GOLD[0], 13: GOLD[0]}
    for y, c in ramp.items():
        gold.put(7, y, c)
        gold.put(8, y, GOLD[ramp_index(c) - 1] if ramp_index(c) > 0 else c)
    # Coins on their way in, near the mouth.
    for x, y, c in ((4, 5, GOLD[1]), (11, 5, GOLD[2]), (5, 7, GOLD[0]), (10, 8, GOLD[1])):
        gold.put(x, y, c)
    return [wood, iron, gold]


def ramp_index(c):
    return GOLD.index(c)


def intake():
    iron, mouth, gold = Layer('Eisen'), Layer('Öffnung'), Layer('Goldkern')
    iron_rim(iron)
    for i in range(1, 15):
        for x, y in ((i, 1), (1, i), (i, 14), (14, i)):
            iron.put(x, y, IRON[2])
        for x, y in ((i, 2), (2, i), (i, 13), (13, i)):
            if 2 <= i <= 13:
                iron.put(x, y, IRON[3] if x == 2 or y == 2 else IRON[1])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        iron.put(x, y, GOLD[1])
    for y in range(3, 13):
        for x in range(3, 13):
            mouth.put(x, y, WOOD[1] if x == 3 or y == 3 else WOOD[0])
    # A plus-shaped core like the strong beacon's, gold instead of amethyst.
    for i in range(5, 11):
        gold.put(i, 7, GOLD[1])
        gold.put(i, 8, GOLD[1])
        gold.put(7, i, GOLD[1])
        gold.put(8, i, GOLD[1])
    for y in range(6, 10):
        for x in range(6, 10):
            if (x in (6, 9)) and (y in (6, 9)):
                continue
            gold.put(x, y, GOLD[2])
    for x, y in ((7, 7), (8, 7), (7, 8)):
        gold.put(x, y, GOLD[3])
    for x, y in ((5, 5), (10, 5), (5, 10), (10, 10)):
        gold.put(x, y, GOLD[0])
    return [iron, mouth, gold]


def back():
    wood, iron, brass = Layer('Holz'), Layer('Eisen'), Layer('Schloss')
    for k, (y0, y1) in enumerate(((0, 5), (5, 10), (10, 16))):
        for y in range(y0, y1):
            for x in range(16):
                if y == y0:
                    c = WOOD[1]
                elif y == y0 + 1:
                    c = WOOD[4]
                elif y == y1 - 1:
                    c = WOOD[2]
                else:
                    c = WOOD[3] if (x * 5 + y * 3 + k * 7 + 3) % 7 else WOOD[2]
                wood.put(x, y, c)
        end = (5 + k * 6 + 3) % 16
        for y in range(y0, y1):
            wood.put(end, y, WOOD[1])
    for y in (3, 4, 11, 12):
        for x in range(16):
            iron.put(x, y, IRON[3] if y in (3, 11) else IRON[1])
    for y in (3, 11):
        for x in (2, 13):
            iron.put(x, y, IRON[4])
    iron_rim(iron)
    for y in range(5, 11):
        for x in range(6, 10):
            edge = x in (6, 9) or y in (5, 10)
            brass.put(x, y, GOLD[2] if edge else GOLD[1])
    brass.put(6, 5, GOLD[3])
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        brass.put(x, y, WOOD[0])
    brass.put(7, 9, WOOD[0])
    return [wood, iron, brass]


def glass():
    tint, shine = Layer('Glas'), Layer('Glanz')
    for y in range(16):
        for x in range(16):
            tint.put(x, y, (200, 232, 244), 24)
    for x, y in ((6, 3), (5, 4), (4, 5), (3, 6), (8, 3), (7, 4)):
        shine.put(x, y, (255, 255, 255), 170)
    for x, y in ((12, 9), (11, 10), (10, 11), (12, 11)):
        shine.put(x, y, (255, 255, 255), 150)
    # The streak pixels replace the tint (one layer per pixel keeps the composition exact).
    for i in range(256):
        if shine.px[i * 4 + 3]:
            tint.px[i * 4:i * 4 + 4] = bytes(4)
    return [tint, shine]


def outputs():
    # W23-4 (owner 23.10.): the Sales Storage faces are drawn by misctex23b_2026_10_23.py (softer B2); side()/intake()/
    # back() above are the SP22-7 design, kept as history. Only the display case glass is still written here.
    return {'display_case_glass': glass()}


def main():
    check = '--check' in sys.argv
    stale = []
    for name, lays in outputs().items():
        doc = L.from_layers(16, 16, [(lay.name, bytes(lay.px)) for lay in lays])
        png = L.png_write(16, 16, bytes(L.compose(doc)))
        path = os.path.join(ASSETS, name + '.png')
        if check:
            have = L.png_read(open(path, 'rb').read())[2] if os.path.exists(path) else b''
            if bytes(have) != bytes(L.compose(doc)):
                stale.append(path)
            continue
        with open(path, 'wb') as f:
            f.write(png)
        L.write_beside(path, doc)
    if check and stale:
        raise SystemExit('stale: ' + ', '.join(stale))
    print('simple shops storage/glass textures: ' + ('up to date' if check else 'written'))


if __name__ == '__main__':
    main()
