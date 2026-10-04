"""Usage: python tools/textures/speaker_radial_proposals_2026_10_04.py <vanilla textures dir> [preview png]

Owner 2026-10-04 chose variant C of speaker_proposals_2026_10_04.py for both speakers, but radial: the note block's
hole dots dark in the middle, the light material at the edge, with a very soft transition. Five proposals each
(one texture for all six faces, so the block reads seamless; centre exactly at 7.5/7.5). Proposals only.
- A: continuous, linear over the radius (material quantile 0.15 -> 0.85);
- B: three ring steps;
- C: two steps, the light ring only on the outer dots;
- D: continuous, eased (dark centre wider), narrower range 0.2 -> 0.8 - the softest;
- E: four ring steps, a little more contrast (0.1 -> 0.9).
The cube view is a proper affine projection of the same texture on top, left and right face (no sampling pattern)."""
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import speaker_proposals_2026_10_04 as sp  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'lautsprecher-radial-vorschau.png')
sp.V = V
CX = CY = 7.5

VARIANTS = {
    'A linear': dict(lo=0.15, hi=0.85, steps=0, gamma=1.0),
    'B drei Ringe': dict(lo=0.15, hi=0.85, steps=3, gamma=1.0),
    'C zwei Stufen': dict(lo=0.2, hi=0.8, steps=2, gamma=1.6),
    'D weich, schmal': dict(lo=0.2, hi=0.8, steps=0, gamma=1.8),
    'E vier Ringe, kontrastreicher': dict(lo=0.1, hi=0.9, steps=4, gamma=1.0),
}


def radial(note, mat, lo, hi, steps, gamma):
    t = sp.tones(note)
    holes = [(x, y) for y in range(16) for x in range(16) if t[note.getpixel((x, y))[:3]] == 1]
    rmax = max(math.hypot(x + 0.5 - CX - 0.5, y + 0.5 - CY - 0.5) for x, y in holes)
    out = note.copy()
    for (x, y) in holes:
        f = math.hypot(x - CX, y - CY) / rmax
        f = min(1.0, f) ** gamma
        if steps:
            f = min(steps - 1, int(f * steps)) / (steps - 1)
        q = lo + (hi - lo) * f
        out.putpixel((x, y), mat[min(len(mat) - 1, int(q * len(mat)))] + (255,))
    return out


def cube(tex, a=6):
    """Isometric block: the texture affinely mapped onto top (100 %), left (80 %) and right (60 %) face. The texture
    is enlarged first (each texel a 16 x 16 block) so the projection never drops or doubles a texel - no moire."""
    up = 16
    big = tex.resize((16 * up, 16 * up), Image.NEAREST)
    s = 16 * up
    k = a / up                      # screen pixels per enlarged source pixel
    w = round(2 * s * k)
    out = Image.new('RGBA', (w, w), (0, 0, 0, 0))
    for face, shade in (('top', 1.0), ('left', 0.8), ('right', 0.6)):
        shaded = Image.eval(big.convert('RGB'), lambda v: round(v * shade)).convert('RGBA')
        if face == 'top':
            data = (1 / (2 * k), 1 / k, -w / (4 * k), -1 / (2 * k), 1 / k, w / (4 * k))
        elif face == 'left':
            data = (1 / k, 0, 0, -1 / (2 * k), 1 / k, -s / 2)
        else:
            data = (1 / k, 0, -s, 1 / (2 * k), 1 / k, -s - s / 2)
        layer = shaded.transform((w, w), Image.AFFINE, data, resample=Image.BILINEAR, fillcolor=(0, 0, 0, 0))
        out.alpha_composite(layer)
    return out


def main():
    note = sp.vblock('note_block')
    mats = [('Astralit', 'astralit_dust'), ('Nihilith', 'nihilith_shard')]
    s, a = 8, 4
    cell = 16 * s + 12
    cw = 2 * 16 * a + 12
    row_h = 16 * s + 30
    im = Image.new('RGBA', (170 + 5 * (cell + cw), 40 + 3 * (row_h + 8) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 8), 'Lautsprecher radial (Variante C): Loecher innen dunkel, aussen helles Material - je 5 Vorschlaege, '
                    'eine Textur fuer alle 6 Seiten', fill=(0, 0, 0, 255))
    y = 30
    x = 170
    for label, tex in (('Vanilla-Notenblock', note), ('Plattenspieler', sp.vblock('jukebox_side'))):
        d.text((x, y), label, fill=(0, 0, 0, 255))
        im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
        im.alpha_composite(cube(tex, a), (x + cell, y + 14))
        x += cell + cw
    for label, item in mats:
        y += row_h + 8
        d.text((10, y + 30), label, fill=(0, 0, 0, 255))
        im.alpha_composite(ps.load(os.path.join(ps.SB_ITEM, item + '.png')).resize((48, 48), Image.NEAREST), (10, y + 50))
        mat = sp.material_colours(item)
        x = 170
        for name, kw in VARIANTS.items():
            tex = radial(note, mat, **kw)
            d.text((x, y), name, fill=(0, 0, 0, 255))
            im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            im.alpha_composite(cube(tex, a), (x + cell, y + 14))
            x += cell + cw
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
