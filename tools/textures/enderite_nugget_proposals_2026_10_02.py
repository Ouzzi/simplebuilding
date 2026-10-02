"""Usage: python tools/textures/enderite_nugget_proposals_2026_10_02.py <vanilla textures dir> [preview png] [png dir]

Owner 2026-10-02: ten Enderite Nugget proposals that match the Enderite Ingot and the vanilla nuggets. Proposals only
(nothing is written into the mod). Colours come from enderite_ingot.png (lavender ramp, dark violet outline, pink
sparkles); shapes from the vanilla gold/iron/copper nuggets and the mod's netherite nugget."""
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'enderit-nugget-vorschau.png')
PNG_DIR = sys.argv[3] if len(sys.argv) > 3 else None

OUTLINE = (28, 10, 51)
BODY = [(62, 33, 115), (71, 36, 128), (85, 48, 154), (109, 69, 184), (123, 81, 201), (142, 99, 220), (165, 125, 233),
        (207, 178, 251), (241, 232, 255)]
FULL = [OUTLINE] + BODY
SPARK, SPARK_LIGHT = (199, 125, 255), (244, 210, 255)


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def brightest(im):
    return max(ps.opaque(im), key=lambda p: (ps.lum(im.getpixel(p)), -p[1], -p[0]))


def sparkle(im, at, light=False):
    out = im.copy()
    ps.px(out, at[0], at[1], SPARK_LIGHT if light else SPARK)
    return out


def mini_ingot():
    """The ingot, nearest-scaled to half size, outlined - a broken-off corner of the bar."""
    ingot = ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))
    box = ingot.getbbox()
    piece = ingot.crop(box).resize(((box[2] - box[0]) // 2, (box[3] - box[1]) // 2), Image.NEAREST)
    im = ps.blank()
    im.alpha_composite(piece, ((16 - piece.width) // 2, (16 - piece.height) // 2 + 1))
    return ps.outline(ps.recolor(im, BODY[1:]), OUTLINE)


PEBBLE = [
    '................',
    '................',
    '................',
    '................',
    '.......oo.......',
    '......o76o......',
    '.....o7865o.....',
    '....o578643o....',
    '....o4665433o...',
    '.....o44332o....',
    '......o221o.....',
    '.......ooo......',
    '................',
    '................',
    '................',
    '................',
]
CHIPS = [
    '................',
    '................',
    '................',
    '.........oo.....',
    '........o86o....',
    '........o54o....',
    '.....oo..oo.....',
    '....o876o.......',
    '...o87644o......',
    '...o654332o.....',
    '....o4321o......',
    '.....oooo.......',
    '................',
    '................',
    '................',
    '................',
]


def from_grid(rows):
    pal = {'o': OUTLINE}
    pal.update({str(i): BODY[i] for i in range(len(BODY))})
    return ps.grid(rows, pal)


def proposals():
    gold, iron, copper = vitem('gold_nugget'), vitem('iron_nugget'), vitem('copper_nugget')
    neth = ps.load(os.path.join(ps.SB_ITEM, 'netherite_nugget.png'))
    a = ps.recolor(gold, FULL)
    b = ps.recolor(iron, FULL)
    c = ps.recolor(copper, FULL)
    d = ps.recolor(neth, FULL)
    e = sparkle(a, brightest(a))
    f = ps.recolor(iron, FULL)
    bx, by = brightest(f)
    f = sparkle(sparkle(f, (bx, by), light=True), (bx + 1, by + 1))
    g = mini_ingot()
    h = from_grid(CHIPS)
    i = ps.outline(ps.recolor(gold, BODY[1:], by_rank=False), OUTLINE)
    i = sparkle(i, brightest(i), light=True)
    j = from_grid(PEBBLE)
    j = sparkle(j, (7, 6), light=True)
    return [
        ('Goldnugget-Form', a), ('Eisennugget-Form', b), ('Kupfernugget-Form', c), ('Netheritnugget-Form', d),
        ('Gold-Form + Funkel', e), ('Eisen-Form + 2 Funkel', f), ('Mini-Barren', g), ('zwei Splitter', h),
        ('Gold-Form, Barrenrand', i), ('runder Kiesel', j),
    ]


def main():
    props = proposals()
    refs = [('Enderit-Barren', ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))),
            ('Enderit-Nugget jetzt', ps.load(os.path.join(ps.SB_ITEM, 'enderite_nugget.png'))),
            ('Netherit-Nugget', ps.load(os.path.join(ps.SB_ITEM, 'netherite_nugget.png'))),
            ('Goldnugget', vitem('gold_nugget')), ('Eisennugget', vitem('iron_nugget')), ('Kupfernugget', vitem('copper_nugget'))]
    notes = [f'{ps.LETTERS[k]}: {name}' for k, (name, _) in enumerate(props)]
    ps.sheet('Enderit-Nugget - 10 Vorschlaege (Farben aus dem Enderit-Barren, Formen der Vanilla-Nuggets)',
             refs, [('Vorschlag', [im for _, im in props])], PREVIEW, notes=['  '.join(notes[:5]), '  '.join(notes[5:])])
    if PNG_DIR:
        ps.save_pngs(PNG_DIR, {f'enderite_nugget_{ps.LETTERS[k].lower()}': im for k, (_, im) in enumerate(props)})
    print('ok')


if __name__ == '__main__':
    main()
