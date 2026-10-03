"""Usage: python tools/textures/trim_template_animation_2026_10_03.py <vanilla textures dir> [preview dir]

Owner 2026-10-03: the three trim templates get a calm texture animation on top of his own motifs (placed on
background A by trim_templates_owner_2026_10_03.py). Motif pixel positions never change - only their brightness:
- Glowing:   the gold patches glow softly (two frames, dim <-> bright, interpolated, 2 x 20 ticks).
- Pulsating: the glints pulse in turns - three groups, each lit in its own frame (interpolated, 3 x 10 ticks).
- Emitting:  a bright ring runs from the centre outwards over the rays (6 frames, interpolated, 6 x 4 ticks).
The motif mask is the same as for the static texture: every pixel of the owner's canvas that is not one of its four
background colours. Writes the strips + .mcmeta into the main tree and the 1.21.11 copy, GIFs and a strip preview."""
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import trim_templates_owner_2026_10_03 as owner  # noqa: E402
import trim_template_bg_motif_2026_10_02 as bgm  # noqa: E402
import trim_template_proposals_2026_10_02 as r1  # noqa: E402

bgm.V = r1.V = V
CX, CY = 7.5, 7.5


def scale(c, k):
    """Brighter (k > 1, towards white) or dimmer (k < 1) - the hue stays."""
    if k >= 1:
        return ps.mix(c, (255, 255, 255), min(1.0, k - 1))
    return tuple(round(v * k) for v in c)


def static(name):
    th = r1.themes()
    plate = bgm.backgrounds(th[owner.THEMES[name]]['body'])[0][1]
    own, built, _ = owner.build(name, plate)
    return built, owner.motif_pixels(own)


def frames(name):
    base, motif = static(name)
    out = []
    if name == 'glowing':
        for k in (0.82, 1.18):
            im = base.copy()
            for p, c in motif.items():
                im.putpixel(p, scale(c, k) + (255,))
            out.append(im)
        return out, 20
    if name == 'pulsating':
        for f in range(3):
            im = base.copy()
            for (x, y), c in motif.items():
                im.putpixel((x, y), scale(c, 1.35 if (x * 2 + y) % 3 == f else 0.85) + (255,))
            out.append(im)
        return out, 10
    n = 6
    rmax = max(math.hypot(x + 0.5 - CX, y + 0.5 - CY) for x, y in motif)
    for f in range(n):
        ring = f / n * (rmax + 1.5)
        im = base.copy()
        for (x, y), c in motif.items():
            d = math.hypot(x + 0.5 - CX, y + 0.5 - CY)
            im.putpixel((x, y), scale(c, 1.3 if abs(d - ring) < 1.3 else 0.9) + (255,))
        out.append(im)
    return out, 4


def main():
    rows = []
    for name in ('glowing', 'pulsating', 'emitting'):
        fr, ticks = frames(name)
        strip = Image.new('RGBA', (16, 16 * len(fr)), (0, 0, 0, 0))
        for k, im in enumerate(fr):
            strip.alpha_composite(im, (0, 16 * k))
        for tree in owner.TREES:
            strip.save(os.path.join(tree, f'{name}_trim_template.png'))
            with open(os.path.join(tree, f'{name}_trim_template.png.mcmeta'), 'w', encoding='utf-8', newline='\n') as f:
                json.dump({'animation': {'frametime': ticks, 'interpolate': True}}, f, indent=2)
                f.write('\n')
        # GIF: interpolated like the game, 4 steps between two frames, 50 ms per tick
        steps = []
        for k in range(len(fr)):
            a, b = fr[k], fr[(k + 1) % len(fr)]
            for i in range(4):
                steps.append(Image.blend(a, b, i / 4))
        gif = []
        for im in steps:
            bg = Image.new('RGBA', (128, 128), (139, 139, 139, 255))
            bg.alpha_composite(im.resize((128, 128), Image.NEAREST))
            gif.append(bg.convert('P', palette=Image.ADAPTIVE))
        gif[0].save(os.path.join(OUT, f'besatz-animation-{name}.gif'), save_all=True, append_images=gif[1:],
                    duration=round(ticks * 50 / 4), loop=0)
        rows.append((f'{name} ({len(fr)} x {ticks} Ticks)', fr))
    ps.sheet('Besatzvorlagen - Animation der Besitzer-Motive (nur Helligkeit, Motivpixel unveraendert; Spalten = Frames)',
             [], rows, os.path.join(OUT, 'besatz-animationen-vorschau.png'), scale=10,
             notes=['Glowing: Goldflecken leuchten sanft auf und ab.  Pulsating: drei Glimmergruppen pulsieren abwechselnd.',
                    'Emitting: heller Ring laeuft vom Zentrum nach aussen ueber die Strahlen.  Alle Frames interpoliert.'])
    print('ok')


if __name__ == '__main__':
    main()
