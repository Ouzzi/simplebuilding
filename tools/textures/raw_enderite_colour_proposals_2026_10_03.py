"""Usage: python tools/textures/raw_enderite_colour_proposals_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03: the installed Raw Enderite Scrap (his own texture, layered_raw_enderite_owner_2026_10_03.py) a bit
further in colour - three proposals A-C. Form and shading stay 1:1: his ten colours, ranked dark -> light, map one
to one onto a ten-step ramp; only the ramp changes. Proposals only.
- A warmer / more violet, towards the enderite ingot's lavender;
- B cooler, with an End-teal accent on the two lightest steps (chorus / ender pearl);
- C more contrast like vanilla's netherite scrap: darker shadows, a brighter top step."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'raw-enderite-farben-vorschau.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import layered_raw_enderite_owner_2026_10_03 as owner  # noqa: E402

RAMPS = {
    'A waermer / violetter (zum Barren)': ps.ramp((32, 10, 58), (62, 24, 112), (104, 58, 172), (150, 104, 220),
                                                  (196, 160, 246), n=10),
    'B kuehler, End-Tuerkis-Akzent': ps.ramp((18, 14, 52), (40, 34, 104), (70, 64, 150), (110, 106, 196), n=8)
    + [(92, 176, 170), (150, 222, 210)],
    'C kontrastreicher (wie Netheritschrott)': ps.ramp((14, 4, 28), (44, 16, 86), (86, 50, 150), (150, 110, 214),
                                                       (226, 204, 255), n=10),
}


def recolour(src, ramp):
    cols = sorted({src.getpixel(p)[:3] for p in ps.opaque(src)}, key=ps.lum)
    mapping = {c: ramp[round(i * (len(ramp) - 1) / max(1, len(cols) - 1))] for i, c in enumerate(cols)}
    out = src.copy()
    for p in ps.opaque(src):
        out.putpixel(p, mapping[src.getpixel(p)[:3]] + (255,))
    return out


def main():
    own = ps.load(os.path.join(HERE, 'hand', 'owner', 'layered_raw_enderite_owner.png'))
    current = ps.load(os.path.join(ps.SB_ITEM, 'layered_raw_enderite.png'))
    vitem = lambda n: ps.load(os.path.join(V, 'item', n + '.png'))
    cells = [('jetzt eingebaut', current)] + [(name, recolour(own, ramp)) for name, ramp in RAMPS.items()] + [
        ('Enderit-Barren', ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))),
        ('Enderit-Nugget', ps.load(os.path.join(ps.SB_ITEM, 'enderite_nugget.png'))),
        ('Netheritschrott', vitem('netherite_scrap'))]
    s, cell = 12, 16 * 12 + 16
    im = Image.new('RGBA', (20 + len(cells) * cell, 16 * s + 60), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 6), 'Raw Enderite Scrap - 3 Farbvorschlaege (Form und Schattierung des Besitzers 1:1, nur die Farbrampe)',
           fill=(0, 0, 0, 255))
    for k, (label, sprite) in enumerate(cells):
        x = 10 + k * cell
        d.text((x, 24), label[:34], fill=(0, 0, 0, 255))
        im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (x, 40))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
