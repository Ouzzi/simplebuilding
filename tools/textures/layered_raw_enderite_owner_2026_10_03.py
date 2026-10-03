"""Usage: python tools/textures/layered_raw_enderite_owner_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03: "the enderite texture is not good, I made my own - only adjust the colours to stay consistent with
the other netherite stuff". His Resprite canvas (reconstructed into tools/textures/hand/owner/
layered_raw_enderite_owner.png) is a layered chunk like vanilla's netherite scrap, so it is the Raw Enderite Scrap
(item layered_raw_enderite, the netherite-scrap counterpart; the Enderite Scrap item is the smaller lump).

Form and shading structure stay 1:1: his ten colours, ranked dark -> light, map one to one onto the ten-step enderite
scrap ramp of generate_textures.py (LAYERED_RAW_ENDERITE_PAL, the same steps as the enderite scrap: darkest = the
outline colour of the enderite ingot/scrap family). generate_textures.py reads the result from the main tree."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'enderit-besitzer-vorschau.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402

RAMP_HEX = ["#1f0c3d", "#2c1356", "#442871", "#4a2784", "#553190", "#6841a9", "#8d65cd", "#9d7ad5", "#a67aef", "#b58ef6"]


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


def ramp():
    return sorted((hexrgb(h) for h in RAMP_HEX), key=ps.lum)


def recolour(owner):
    cols = sorted({owner.getpixel(p)[:3] for p in ps.opaque(owner)}, key=ps.lum)
    target = ramp()
    mapping = {c: target[round(i * (len(target) - 1) / max(1, len(cols) - 1))] for i, c in enumerate(cols)}
    out = owner.copy()
    for p in ps.opaque(owner):
        out.putpixel(p, mapping[owner.getpixel(p)[:3]] + (255,))
    return out, len(cols)


def main():
    owner = ps.load(os.path.join(HERE, 'hand', 'owner', 'layered_raw_enderite_owner.png'))
    before = ps.load(os.path.join(ps.SB_ITEM, 'layered_raw_enderite.png'))
    new, n = recolour(owner)
    print(f'{n} owner colours -> {len(RAMP_HEX)} ramp steps')
    new.save(os.path.join(ps.SB_ITEM, 'layered_raw_enderite.png'))
    vitem = lambda name: ps.load(os.path.join(V, 'item', name + '.png'))
    cells = [('bisher', before), ('Besitzer-Original', owner), ('farbangepasst (eingebaut)', new),
             ('Netheritschrott', vitem('netherite_scrap')), ('Enderit-Barren', ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))),
             ('Enderit-Nugget', ps.load(os.path.join(ps.SB_ITEM, 'enderite_nugget.png'))),
             ('Enderitschrott', ps.load(os.path.join(ps.SB_ITEM, 'enderite_scrap.png')))]
    s, cell = 12, 16 * 12 + 14
    im = Image.new('RGBA', (20 + len(cells) * cell, 16 * s + 60), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 6), 'Raw Enderite Scrap: Besitzer-Textur, Form 1:1, Farben auf die Enderit-Schrott-Rampe (10 Stufen)',
           fill=(0, 0, 0, 255))
    for k, (label, sprite) in enumerate(cells):
        x = 10 + k * cell
        d.text((x, 24), label, fill=(0, 0, 0, 255))
        im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (x, 40))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
