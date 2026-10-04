"""Usage: python tools/textures/speaker_settled_2026_10_04.py <vanilla textures dir> [preview png]

Owner 2026-10-04 settled the speaker textures (renamed the same day: Astralit -> Jukebox Amplifier, Nihilit -> Note
Amplifier; the material stays Astralit resp. Nihilith): variant D of
speaker_radial_proposals_2026_10_04.py (the vanilla note block, its hole dots radially from a wide dark centre to the
light material at the edge, continuous) - a little more subtle: the material range narrowed by a third (quantile
0.25 -> 0.65 instead of 0.2 -> 0.8, so the outermost dots are less bright) and every hole dot mixed 20 % back towards
the note block's own hole colour, so the wood dominates even more. One texture on all six faces: it is written as
both <block>_side and <block>_top (jukebox_amplifier, note_amplifier; the block model, a cube column, stays as it is).
music_disc_textures.py takes the speaker textures from here."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, '..', '..', 'build', 'lautsprecher-D-eingebaut.png')
sys.argv = [sys.argv[0], V]
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import speaker_proposals_2026_10_04 as sp  # noqa: E402
import speaker_radial_proposals_2026_10_04 as rad  # noqa: E402

sp.V = rad.V = V
BLOCK = os.path.join(ps.ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'block')
D = dict(lo=0.2, hi=0.8, steps=0, gamma=1.8)
SUBTLE = dict(lo=0.25, hi=0.65, steps=0, gamma=1.8)
WOOD_BACK = 0.2
ITEMS = {'astralit': 'astralit_dust', 'nihilith': 'nihilith_shard'}
#: Block id per material (owner 2026-10-04: Jukebox Amplifier = Astralit, Note Amplifier = Nihilit).
BLOCKS = {'astralit': 'jukebox_amplifier', 'nihilith': 'note_amplifier'}


def settled(note, item, subtle=True):
    mat = sp.material_colours(item)
    tex = rad.radial(note, mat, **(SUBTLE if subtle else D))
    if subtle:
        t = sp.tones(note)
        for y in range(16):
            for x in range(16):
                if t[note.getpixel((x, y))[:3]] == 1:
                    tex.putpixel((x, y), ps.mix(tex.getpixel((x, y))[:3], note.getpixel((x, y))[:3], WOOD_BACK) + (255,))
    return tex


def textures(note):
    out = {}
    for material, item in ITEMS.items():
        tex = settled(note, item)
        out[f'{BLOCKS[material]}_side'] = tex
        out[f'{BLOCKS[material]}_top'] = tex
    return out


def main():
    note = sp.vblock('note_block')
    for name, tex in textures(note).items():
        tex.save(os.path.join(BLOCK, name + '.png'))
    s, a = 10, 5
    cell = 16 * s + 12
    cw = 2 * 16 * a + 12
    row_h = 16 * s + 30
    im = Image.new('RGBA', (170 + 2 * (cell + cw), 40 + 3 * (row_h + 8)), (139, 139, 139, 255))
    d = ImageDraw.Draw(im)
    d.text((10, 8), 'Lautsprecher eingebaut: Variante D | D dezenter (eingebaut), eine Textur fuer alle 6 Seiten', fill=(0, 0, 0, 255))
    y = 30
    x = 170
    for label, tex in (('Vanilla-Notenblock', note), ('Plattenspieler', sp.vblock('jukebox_side'))):
        d.text((x, y), label, fill=(0, 0, 0, 255))
        im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
        im.alpha_composite(rad.cube(tex, a), (x + cell, y + 14))
        x += cell + cw
    for material, item in ITEMS.items():
        y += row_h + 8
        d.text((10, y + 40), material.capitalize(), fill=(0, 0, 0, 255))
        x = 170
        for label, tex in (('D', settled(note, item, subtle=False)), ('D dezenter (eingebaut)', settled(note, item))):
            d.text((x, y), label, fill=(0, 0, 0, 255))
            im.alpha_composite(tex.resize((16 * s, 16 * s), Image.NEAREST), (x, y + 14))
            im.alpha_composite(rad.cube(tex, a), (x + cell, y + 14))
            x += cell + cw
    os.makedirs(os.path.dirname(os.path.abspath(PREVIEW)), exist_ok=True)
    im.save(PREVIEW)
    print('ok')


if __name__ == '__main__':
    main()
