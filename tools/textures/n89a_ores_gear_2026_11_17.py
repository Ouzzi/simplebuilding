"""N89A (owner N77, 17.11.2026): spear, horse armor and nautilus armor of the two rose gold alloys (Simple Ores).

Own pixel art only, nothing read from Vanilla:
- horse/nautilus armor (icons 16x16, worn layers horse_body 64x64 / nautilus_body 128x128): the Enderite mount armor of
  mount_armor_textures.py (own maps and painting) with the alloy ramp instead of the Enderite ramp; the saddle is brown leather instead
  of Netherite, the Ender veins/glimmer become the ramp's two lightest tones (metal highlights), the pearls metal studs. The module's
  globals are swapped only while painting and restored afterwards, so the Enderite output stays byte-identical.
- spear icon: the Enderite gear map of the spear (generate_textures.ENDERITE_GEAR_MAPS/ENDERITE_HANDLE_MAPS) painted on the alloy ramp,
  wooden handle with alloy wraps; the held spear (32x32, model minecraft:item/spear_in_hand) is that icon at twice the size.
Ramps: ores_2026_10_30.RAMPS ('soft' = Rose Gold, 'hard' = Rose Copper), the ramps of the alloys' other textures.

    python3 tools/textures/n89a_ores_gear_2026_11_17.py          write (PNG + layer source)
    python3 tools/textures/n89a_ores_gear_2026_11_17.py --check  fail when a file is stale
"""
from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import generate_textures as G  # noqa: E402
import layers as L  # noqa: E402
import mount_armor_textures as M  # noqa: E402
import ores_2026_10_30 as O  # noqa: E402

ROOT = HERE.parents[1]
TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures'
ALLOYS = {'rose_gold': 'soft', 'rose_bronze': 'hard'}
# brown leather of the saddle/strap and the wooden spear handle, dark -> light (the brown handle ramp of the Enderite gear)
LEATHER = ['#2b1a10', '#3d2616', '#56351f', '#6b4527', '#83582f', '#9a6b3a']


def ramp8(alloy):
    """Eight tones dark -> light from the 10-tone alloy ramp (brightest first), without its two extremes."""
    r = O.RAMPS[ALLOYS[alloy]]
    return [r[i] for i in range(8, 0, -1)]


def _hex(h):
    return M._hex(h)


# ---------------------------------------------------------------- horse and nautilus armor
def mount(alloy):
    """{relative texture path: image} of the alloy's horse and nautilus armor (icons + worn layers)."""
    tones = ramp8(alloy)
    light, lighter = O.RAMPS[ALLOYS[alloy]][1], O.RAMPS[ALLOYS[alloy]][0]
    saved = (M.ENDER, M.VEIN, M.GLOW, M.NETHER)
    M.ENDER, M.VEIN, M.GLOW, M.NETHER = tones, lighter, light, LEATHER
    try:
        pal = {'o': tones[1], 'O': tones[0], 'v': lighter, 'g': light,
               'n': LEATHER[1], 'a': LEATHER[2], 'b': LEATHER[3], 'c': LEATHER[4], 'd': LEATHER[5]}
        pal.update({str(i): tones[i] for i in range(1, 8)})
        out = {f'item/{alloy}_horse_armor.png': M.render_icon(M.HORSE_ICON, pal),
               f'item/{alloy}_nautilus_armor.png': M.render_icon(M.NAUTILUS_ICON, pal),
               f'entity/equipment/horse_body/{alloy}.png': M.horse_body(),
               f'entity/equipment/nautilus_body/{alloy}.png': M.nautilus_body()}
    finally:
        M.ENDER, M.VEIN, M.GLOW, M.NETHER = saved
    for name, img in out.items():
        L.snap(img, 'Grundform')
    return out


# ---------------------------------------------------------------- spear
def spear(alloy):
    tones = ramp8(alloy)
    lighter = O.RAMPS[ALLOYS[alloy]][0]
    rows = G.ENDERITE_GEAR_MAPS['spear']
    head_t, handle_t = '01234567', 'abcdef'
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))

    def at(x, y):
        return rows[y][x] if 0 <= x < 16 and 0 <= y < 16 else '.'

    for y in range(16):
        for x in range(16):
            c = rows[y][x]
            if c == '.':
                continue
            if c in handle_t:
                ti = handle_t.index(c)
                col = LEATHER[min(len(LEATHER) - 1, ti + 1)] if (x + y) % 4 == 0 else LEATHER[ti]
            else:
                tone = head_t.index(c)
                col = tones[tone]
                n4 = sum(at(x + dx, y + dy) in head_t for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if n4 == 4 and tone >= 3 and (x + 2 * y) % 11 == 0:
                    col = lighter   # a polished glint instead of the Ender vein
            img.putpixel((x, y), _hex(col))
    L.snap(img, 'Spitze')
    # the handle of the Enderite spear map: wood, the wraps in the alloy's tones
    colours = {'a': LEATHER[1], 'b': LEATHER[4], 'c': LEATHER[0], 'd': LEATHER[3], 'W': tones[4], 'G': tones[6], 'H': tones[5]}
    for y, row in enumerate(G.ENDERITE_HANDLE_MAPS['spear']):
        for x, c in enumerate(row):
            if c != '.':
                img.putpixel((x, y), _hex(colours[c]))
    L.snap(img, 'Griff')
    held = img.resize((32, 32), Image.Resampling.NEAREST)
    L.snap(held, 'Grundform')
    return {f'item/{alloy}_spear.png': img, f'item/{alloy}_spear_in_hand.png': held}


def textures():
    out = {}
    for alloy in ALLOYS:
        out.update(mount(alloy))
        out.update(spear(alloy))
    return out


def main(argv):
    check = '--check' in argv
    stale = []
    for rel, img in textures().items():
        path = TEX / rel
        same = path.exists() and Image.open(path).convert('RGBA').tobytes() == img.convert('RGBA').tobytes()
        if not same:
            stale.append(path)
            if not check:
                path.parent.mkdir(parents=True, exist_ok=True)
                L.save(img, path)
    if check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print('n89a ores gear: %d textures %s' % (len(textures()), 'current' if not stale else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
