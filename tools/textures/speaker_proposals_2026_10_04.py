"""Usage: python tools/textures/speaker_proposals_2026_10_04.py <vanilla textures dir> [preview png]

Owner 2026-10-04: the Astralit and Nihilith Speaker textures stand out too much. New start: vanilla's note block
(block/note_block.png, one texture on all six sides) with its dark holes - the grille dots - filled from the material's
own item texture (the owner-painted astralit_dust / nihilith_shard): wood stays dominant, the material shows only in
the holes. Five proposals each, one texture for all six faces. Proposals only.

The note block's tones (dark -> light): 0 outline, 1 the hole dots, 2 the dark wood between them, 3-5 lighter wood.
- A: hole dots -> the material's middle tone;
- B: hole dots -> the material's dark tones (the most subtle);
- C: hole dots -> the material's own pixels in order (its structure, darkest to lightest across the grille);
- D: hole dots -> dark material, the dark wood between them tinted 30 % towards the material;
- E: the four centre dots in the material's light tone (a small accent), the others dark material."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'lautsprecher-texturen-vorschau.png')
OVERLAY = os.path.join(ps.ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'block')


def vblock(name):
    return ps.load(os.path.join(V, 'block', name + '.png'))


def tones(note):
    cols = sorted({note.getpixel((x, y))[:3] for y in range(16) for x in range(16)}, key=ps.lum)
    return {c: i for i, c in enumerate(cols)}


def material_colours(item):
    """The item's colours without its outline, dark -> light, every pixel (proportions kept)."""
    im = ps.load(os.path.join(ps.SB_ITEM, item + '.png'))
    cols = sorted((im.getpixel(p)[:3] for p in ps.opaque(im)), key=ps.lum)
    return cols[len(cols) // 10:]          # the darkest tenth is the outline


def variant(note, mat, kind):
    t = tones(note)
    holes = [(x, y) for y in range(16) for x in range(16) if t[note.getpixel((x, y))[:3]] == 1]
    between = [(x, y) for y in range(16) for x in range(16) if t[note.getpixel((x, y))[:3]] == 2]
    out = note.copy()
    q = lambda f: mat[min(len(mat) - 1, int(f * len(mat)))]
    if kind == 'A':
        for p in holes:
            out.putpixel(p, q(0.5) + (255,))
    elif kind == 'B':
        for k, p in enumerate(holes):
            out.putpixel(p, q(0.12 + 0.12 * (k % 2)) + (255,))
    elif kind == 'C':
        order = sorted(holes, key=lambda p: (p[1], p[0]))
        for k, p in enumerate(order):
            out.putpixel(p, q(0.1 + 0.8 * k / max(1, len(order) - 1)) + (255,))
    elif kind == 'D':
        for p in holes:
            out.putpixel(p, q(0.15) + (255,))
        for p in between:
            out.putpixel(p, ps.mix(note.getpixel(p)[:3], q(0.4), 0.3) + (255,))
    else:
        centre = sorted(holes, key=lambda p: (p[0] - 7.5) ** 2 + (p[1] - 7.5) ** 2)[:4]
        for p in holes:
            out.putpixel(p, (q(0.85) if p in centre else q(0.15)) + (255,))
    return out


def cube(tex, scale=6):
    """A small isometric block view: top, left and right face the same texture, shaded like the inventory."""
    s = 16
    size = 2 * s * scale
    out = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    faces = (('top', 1.0), ('left', 0.8), ('right', 0.6))
    for face, shade in faces:
        for v in range(s):
            for u in range(s):
                c = tex.getpixel((u, v))[:3]
                c = tuple(round(x * shade) for x in c) + (255,)
                if face == 'top':
                    x, y = (u - v) * scale + s * scale, (u + v) * scale // 2
                elif face == 'left':
                    x, y = u * scale, s * scale // 2 + u * scale // 2 + v * scale
                else:
                    x, y = s * scale + u * scale, s * scale + v * scale - u * scale // 2
                ImageDraw.Draw(out).rectangle([x, y, x + scale, y + scale], fill=c)
    return out


def main():
    note = vblock('note_block')
    refs = [('Vanilla-Notenblock', note), ('Plattenspieler Seite', vblock('jukebox_side')),
            ('Plattenspieler oben', vblock('jukebox_top'))]
    mats = [('Astralit', 'astralit_dust', 'jukebox_amplifier'), ('Nihilith', 'nihilith_shard', 'note_amplifier')]
    s, cell = 8, 16 * 8 + 16
    cube_w = 2 * 16 * 3
    rows_h = 16 * s + 30
    im = Image.new('RGBA', (180 + 5 * (cell + cube_w + 10), 40 + (len(mats) + 1) * (rows_h + 10) + 30), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 8), 'Lautsprecher: Vanilla-Notenblock, Loecher mit Pixeln des Rohmaterials (Besitzer-Items), je 5 Vorschlaege, '
                    'eine Textur fuer alle 6 Seiten', fill=(0, 0, 0, 255))
    y = 30
    x = 180
    for label, tex in refs:
        d.text((x, y), label, fill=(0, 0, 0, 255))
        im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
        im.alpha_composite(cube(tex, 3), (x + cell, y + 14))
        x += cell + cube_w + 10
    for label, item, speaker in mats:
        y += rows_h + 10
        d.text((10, y + 30), label, fill=(0, 0, 0, 255))
        try:
            side = ps.load(os.path.join(OVERLAY, speaker + '_side.png'))
            d.text((10, y + 50), 'jetzt:', fill=(0, 0, 0, 255))
            im.alpha_composite(side.resize((64, 64), Image.NEAREST), (10, y + 64))
        except FileNotFoundError:
            pass
        mat = material_colours(item)
        im.alpha_composite(ps.load(os.path.join(ps.SB_ITEM, item + '.png')).resize((48, 48), Image.NEAREST), (90, y + 64))
        x = 180
        for kind in 'ABCDE':
            tex = variant(note, mat, kind)
            d.text((x, y), kind, fill=(0, 0, 0, 255))
            im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            im.alpha_composite(cube(tex, 3), (x + cell, y + 14))
            x += cell + cube_w + 10
    d.text((10, y + rows_h + 4), 'A Mittelton  B dunkle Toene (am dezentesten)  C Material-Struktur ueber das Gitter  '
                                 'D dunkel + Holz dazwischen getoent  E vier Mittelloecher hell', fill=(0, 0, 0, 255))
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
