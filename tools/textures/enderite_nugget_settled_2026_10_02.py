"""Usage: python tools/textures/enderite_nugget_settled_2026_10_02.py <vanilla textures dir> [preview png] [old nugget png]

Owner 2026-10-02 settled the Enderite Nugget: round-1 proposal G (enderite_nugget_proposals_2026_10_02.mini_ingot,
half an enderite ingot as a nugget form of its own) with the right tip closed cleanly. The texture itself is drawn by
generate_textures.py (ENDERITE_NUGGET, main tree only); this script checks that the built texture is G with exactly
the planned fixes and saves the labelled before/after preview next to the ingot and the vanilla nuggets."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'enderit-nugget-G-eingebaut.png')
OLD = sys.argv[3] if len(sys.argv) > 3 else None
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import enderite_nugget_proposals_2026_10_02 as r1  # noqa: E402

OUTLINE = (28, 10, 51)
# (x, y) -> colour after the fix; None = transparent. Everything else must equal proposal G.
FIXES = {(12, 8): None, (11, 8): OUTLINE, (10, 8): (85, 48, 154), (10, 7): (109, 69, 184)}


def main():
    g = r1.mini_ingot()
    built = ps.load(os.path.join(ps.SB_ITEM, 'enderite_nugget.png'))
    for y in range(16):
        for x in range(16):
            want = FIXES.get((x, y), 'keep')
            have = built.getpixel((x, y))
            if want == 'keep':
                assert have == g.getpixel((x, y)), f'({x},{y}) differs from proposal G'
            elif want is None:
                assert have[3] == 0, f'({x},{y}) should be transparent'
            else:
                assert have[:3] == want and have[3] == 255, f'({x},{y}) should be {want}'
    vitem = lambda n: ps.load(os.path.join(V, 'item', n + '.png'))
    cells = [('Nugget bisher', ps.load(OLD) if OLD else None), ('Vorschlag G (Runde 1)', g), ('eingebaut', built),
             ('Enderit-Barren', ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))),
             ('Goldnugget', vitem('gold_nugget')), ('Eisennugget', vitem('iron_nugget'))]
    cells = [c for c in cells if c[1] is not None]
    s = 14
    cell = 16 * s + 14
    im = Image.new('RGBA', (20 + len(cells) * cell, 16 * s + 60), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 6), 'Enderit-Nugget: Vorschlag G mit sauber geschlossener rechter Spitze (rot markiert: geaenderte Pixel)',
           fill=(0, 0, 0, 255))
    for k, (label, sprite) in enumerate(cells):
        x = 10 + k * cell
        d.text((x, 24), label, fill=(0, 0, 0, 255))
        im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (x, 40))
        if label in ('Vorschlag G (Runde 1)', 'eingebaut'):
            for (px, py) in FIXES:
                d.rectangle([x + px * s, 40 + py * s, x + (px + 1) * s - 1, 40 + (py + 1) * s - 1], outline=(255, 0, 0, 255))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
