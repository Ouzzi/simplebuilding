"""Usage: python tools/textures/raw_enderite_scrap_settled_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03 chose variant D of raw_enderite_scrap_v3_2026_10_03.py for the Raw Enderite Scrap
(layered_raw_enderite): his own form and shading 1:1, the colours of the Raw Enderite Fragment laid onto his ten
tones - and a little darker: every step at 90 % brightness (about one step down), the darkest step (the rim) the
family's darkest tone, the enderite ingot's outline. Writes the main tree and the 1.21.11 copy (generate_textures.py
reads the main-tree file) and a labelled preview (large + slot size)."""
import colorsys
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'raw-enderite-scrap-D-eingebaut.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import raw_enderite_scrap_v3_2026_10_03 as v3  # noqa: E402

DARKER = 0.90
RIM = (28, 10, 51)      # enderite ingot outline - the darkest tone of the family
TREES = [ps.SB_ITEM, os.path.join(ps.ROOT, 'mc1_21_11', 'fabric', 'src', 'main', 'resources', 'assets', 'simplebuilding',
                                  'textures', 'item')]


def palette_d():
    fragment = ps.load(os.path.join(ps.SB_ITEM, 'raw_enderite.png'))
    return v3.resample(sorted(set(v3.weighted(fragment)), key=ps.lum), 10)


def darker(pal):
    out = []
    for c in pal:
        h, s, v = colorsys.rgb_to_hsv(*[x / 255 for x in c])
        out.append(tuple(round(x * 255) for x in colorsys.hsv_to_rgb(h, s, v * DARKER)))
    out[0] = RIM
    return out


def main():
    d = v3.lay(palette_d())
    dd = v3.lay(darker(palette_d()))
    for tree in TREES:
        dd.save(os.path.join(tree, 'layered_raw_enderite.png'))
    item = lambda n: ps.load(os.path.join(ps.SB_ITEM, n + '.png'))
    cells = [('Variante D', d), ('D dunkler (eingebaut)', dd), ('Enderit-Barren', item('enderite_ingot')),
             ('Enderit-Nugget', item('enderite_nugget')), ('Enderit-Fragment', item('raw_enderite'))]
    s, cell = 12, 16 * 12 + 16
    im = Image.new('RGBA', (20 + len(cells) * cell, 16 * s + 120), (139, 139, 139, 255))
    dr = ImageDraw.Draw(im)
    dr.text((10, 6), 'Raw Enderite Scrap eingebaut: Variante D (Fragment-Farben) 10 % dunkler, Rand = dunkelster Familienton',
            fill=(0, 0, 0, 255))
    for k, (label, sprite) in enumerate(cells):
        x = 10 + k * cell
        dr.text((x, 24), label, fill=(0, 0, 0, 255))
        im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (x, 40))
    y = 40 + 16 * s + 14
    dr.text((10, y), 'Slotgroesse 1x / 2x:', fill=(0, 0, 0, 255))
    for k, (_, sprite) in enumerate(cells):
        x = 10 + k * cell
        im.alpha_composite(sprite, (x, y + 18))
        im.alpha_composite(sprite.resize((32, 32), Image.NEAREST), (x + 24, y + 18))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
