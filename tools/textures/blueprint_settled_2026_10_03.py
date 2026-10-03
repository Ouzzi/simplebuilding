"""Usage: python tools/textures/blueprint_settled_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03 settled the blueprint in the map-sheet direction of blueprint_proposals_2026_10_03.py (the vanilla
map sheet in cyanotype blue). The item model picks the texture by state (items/blueprint.json: no component ->
fresh, simplebuilding:blueprint_state -> edited / signed):
- item/blueprint         (fresh, nothing recorded)  = proposal B: the sheet with its faint grid, no content;
- item/blueprint_edited  (recorded / edited)        = proposal C: grid + the white house;
- item/blueprint_signed  (signed)                   = proposal C on a darker blue sheet, with the red wax seal and
                                                      ribbon of the previous signed texture (same pixels and place).
generate_textures.py reads these three files from the main tree from now on (1.21.11 copies wait for the port run).
Saves a labelled before/after preview."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'blaupausen-eingebaut.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import blueprint_proposals_2026_10_03 as bp  # noqa: E402

bp.V = V
DARK_BLUE = [(10, 24, 60), (18, 40, 92), (28, 60, 124), (40, 80, 150), (56, 100, 172)]
NAMES = ('blueprint', 'blueprint_edited', 'blueprint_signed')


def seal_pixels(old_signed):
    """The red wax seal and its ribbon of the previous signed texture."""
    return {p: old_signed.getpixel(p)[:3] for p in ps.opaque(old_signed)
            if old_signed.getpixel(p)[0] > old_signed.getpixel(p)[1] + 60}


def build(old_signed):
    props = dict(bp.proposals())
    fresh = props['Kartenblatt + Raster']
    edited = props['Raster + Haus']
    saved = bp.BLUE
    try:
        bp.BLUE = DARK_BLUE
        dark = dict(bp.proposals())['Raster + Haus']
    finally:
        bp.BLUE = saved
    signed = dark.copy()
    for p, c in seal_pixels(old_signed).items():
        signed.putpixel(p, c + (255,))
    return {'blueprint': fresh, 'blueprint_edited': edited, 'blueprint_signed': signed}


def main():
    before = {n: ps.load(os.path.join(ps.SB_ITEM, n + '.png')) for n in NAMES}
    after = build(before['blueprint_signed'])
    for n, im in after.items():
        im.save(os.path.join(ps.SB_ITEM, n + '.png'))
    s, cell = 12, 16 * 12 + 14
    sheet = Image.new('RGBA', (140 + 3 * cell, 2 * (cell + 22) + 34), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    d.text((10, 8), 'Blaupause eingebaut: frisch = B (leer), bearbeitet = C (mit Inhalt), signiert = C dunkler + Siegel',
           fill=(0, 0, 0, 255))
    for row, (label, imgs) in enumerate((('A vorher', before), ('B nachher', after))):
        y = 30 + row * (cell + 22)
        d.text((10, y + cell // 2), label, fill=(0, 0, 0, 255))
        for k, n in enumerate(NAMES):
            x = 140 + k * cell
            d.text((x, y), n, fill=(0, 0, 0, 255))
            sheet.alpha_composite(imgs[n].resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    sheet.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
