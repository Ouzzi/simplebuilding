"""Usage: python tools/textures/trim_templates_owner_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03 painted the three trim-template motifs himself (Resprite on the iPad) and wants "these textures with
the correct background for the trims". His canvases were reconstructed cell by cell from the screenshots
(owner_canvas_2026_10_03.py) into tools/textures/hand/owner/*_trim_template_owner.png:
- emitting  <- screenshot 9ae4a290: a gold cross in the middle with rays running out, over a grey lattice - the same
  layout as his original Emitting (1c6980c4: sun, rays to the edges, faint lattice);
- pulsating <- screenshot 41f00a5e: cyan glints all over (the "more glimmer" asked for Pulsating), the only cyan one;
- glowing   <- screenshot bb3fd8bf: gold glow patches without rays.
All three canvases share one background: the dark outline and three navy tones. Every pixel that is not one of those
four colours is his motif. This script puts the motif pixels 1:1 (same position, same colour) on background A
(trim_template_bg_motif_2026_10_02, the plain Sentry plate in the template's colours) and writes the result as the
mod's glowing/pulsating/emitting_trim_template.png (26.2/26.3 main tree and the 1.21.11 copy). Motif pixels that
would fall outside the background-A silhouette are reported and left out. The preview shows owner canvas | motif on
background A | the original texture."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'besatz-besitzer-motive-vorschau.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import trim_template_bg_motif_2026_10_02 as bgm  # noqa: E402
import trim_template_proposals_2026_10_02 as r1  # noqa: E402

bgm.V = r1.V = V
OWNER = os.path.join(HERE, 'hand', 'owner')
TREES = [ps.SB_ITEM, os.path.join(ps.ROOT, 'mc1_21_11', 'fabric', 'src', 'main', 'resources', 'assets', 'simplebuilding',
                                  'textures', 'item')]
CANVAS_BACKGROUND = [(8, 8, 11), (74, 78, 102), (49, 52, 74), (26, 28, 41)]
THEMES = {'glowing': 'Glowing', 'pulsating': 'Pulsating', 'emitting': 'Emitting'}


def is_background(c):
    return any(all(abs(c[i] - b[i]) <= 3 for i in range(3)) for b in CANVAS_BACKGROUND)


def motif_pixels(owner):
    return {p: owner.getpixel(p)[:3] for p in ps.opaque(owner) if not is_background(owner.getpixel(p))}


def build(name, plate):
    owner = ps.load(os.path.join(OWNER, f'{name}_trim_template_owner.png'))
    out = plate.copy()
    outside = []
    for p, c in motif_pixels(owner).items():
        if plate.getpixel(p)[3]:
            out.putpixel(p, c + (255,))
        else:
            outside.append(p)
    return owner, out, outside


def main():
    th = r1.themes()
    originals = {n: ps.load(os.path.join(ps.SB_ITEM, f'{n}_trim_template.png')) for n in THEMES}
    rows = []
    for name, theme in THEMES.items():
        plate = bgm.backgrounds(th[theme]['body'])[0][1]
        owner, built, outside = build(name, plate)
        print(f'{name}: {len(motif_pixels(owner))} motif pixels, outside background A: {outside or "none"}')
        for tree in TREES:
            built.save(os.path.join(tree, f'{name}_trim_template.png'))
        rows.append((theme, owner, built, originals[name]))
    s, cell, left = 14, 16 * 14 + 14, 120
    refs = [(n, bgm.vtex(f'item/{n}_armor_trim_smithing_template')) for n in ('sentry', 'eye', 'ward', 'silence', 'wayfinder')]
    im = Image.new('RGBA', (left + 5 * cell, 30 + (len(rows) + 1) * (cell + 24) + 40), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 8), 'Besatzvorlagen: Motive des Besitzers (Resprite) 1:1 auf Hintergrund A - eingebaut', fill=(0, 0, 0, 255))
    y = 30
    d.text((10, y + cell // 2), 'Vanilla', fill=(0, 0, 0, 255))
    for k, (n, ref) in enumerate(refs):
        d.text((left + k * cell, y), n, fill=(0, 0, 0, 255))
        im.alpha_composite(ref.resize((16 * s, 16 * s), Image.NEAREST), (left + k * cell, y + 14))
    for theme, owner, built, orig in rows:
        y += cell + 24
        d.text((10, y + cell // 2), theme, fill=(0, 0, 0, 255))
        for k, (label, sprite) in enumerate((('Besitzer (Rekonstruktion)', owner), ('Motiv auf Hintergrund A', built),
                                             ('Original bisher', orig))):
            d.text((left + k * cell, y), label, fill=(0, 0, 0, 255))
            im.alpha_composite(sprite.resize((16 * s, 16 * s), Image.NEAREST), (left + k * cell, y + 14))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
