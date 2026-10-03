"""Usage: python tools/textures/raw_enderite_scrap_proposals_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03: ten proposals for the Raw Enderite Scrap (item layered_raw_enderite). Proposals only.
References: vanilla netherite_scrap (stacked slabs), ancient_debris (layered rings), raw_gold / raw_iron (lumpy raw
chunks). Colours: the mod's raw enderite (Raw Enderite Fragment) and the enderite ingot, histogram-matched so dark
outline, body tones and highlight keep their vanilla proportions; End accents where a variant calls for them (end
stone seams, chorus/pearl teal glints)."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402
import enderite_nugget_proposals_v2_2026_10_02 as nug  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'raw-enderite-scrap-vorschau.png')
nug.V = V
END_STONE = [(196, 194, 140), (219, 222, 158), (235, 240, 180)]
PEARL = [(40, 120, 110), (90, 190, 170)]


def vitem(name):
    return ps.load(os.path.join(V, 'item', name + '.png'))


def vblock(name):
    return ps.load(os.path.join(V, 'block', name + '.png'))


def raw_colours():
    """Raw enderite fragment + ingot colours (every opaque pixel, dark -> light) - the darker raw side dominates."""
    raw = ps.load(os.path.join(ps.SB_ITEM, 'raw_enderite.png'))
    ingot = ps.load(os.path.join(ps.SB_ITEM, 'enderite_ingot.png'))
    cols = [raw.getpixel(p)[:3] for p in ps.opaque(raw)] * 2 + [ingot.getpixel(p)[:3] for p in ps.opaque(ingot)
                                                                if ingot.getpixel(p)[:3] not in (nug.PINK, nug.PINK_LIGHT)]
    return sorted(cols, key=ps.lum)


def seams(im, colours, every=None):
    """Lay end-stone colour on the brightest pixel of each row-run's top edge (the seam between slabs)."""
    out = im.copy()
    for (x, y) in ps.opaque(im):
        above = (x, y - 1)
        if (y == 0 or im.getpixel(above)[3] == 0 or ps.lum(im.getpixel(above)) < ps.lum(im.getpixel((x, y))) - 40) \
                and (every is None or (x + y) % every == 0):
            out.putpixel((x, y), colours[(x + y) % len(colours)] + (255,))
    return out


def glints(im, cols, positions):
    out = im.copy()
    for k, p in enumerate(positions):
        if im.getpixel(p)[3]:
            out.putpixel(p, cols[k % len(cols)] + (255,))
    return out


def debris_chunk():
    """The raw-gold chunk silhouette filled with ancient debris's ring pattern, both in enderite colours."""
    shape = vitem('raw_gold')
    debris = vblock('ancient_debris_side')
    im = ps.blank()
    for p in ps.opaque(shape):
        im.putpixel(p, debris.getpixel(p))
    return ps.outline(nug.matched(im, raw_colours()[6:]), nug.OUTLINE)


SLABS = [
    '................',
    '................',
    '.....ooooo......',
    '...oo87765oo....',
    '..o8766554433o..',
    '..o5443322221o..',
    '...oooo543oooo..',
    '...o8776655o....',
    '..o766554433oo..',
    '..o44332211111o.',
    '...oo543321ooo..',
    '....o8776554o...',
    '....o5443221o...',
    '.....oooooooo...',
    '................',
    '................',
]
LUMP = [
    '................',
    '................',
    '......oooo......',
    '....oo8776oo....',
    '...o877665543o..',
    '..o87766554432o.',
    '..o66554433221o.',
    '..ooo4433ooo11o.',
    '..o8776655443o..',
    '..o6655443321o..',
    '...o44332211o...',
    '....oo2211oo....',
    '......oooo......',
    '................',
    '................',
    '................',
]
GEODE = [
    '................',
    '................',
    '.......ooo......',
    '.....oo876o.....',
    '....o8776655o...',
    '...o877666544o..',
    '...o76AB65433o..',
    '..o766ABA54331o.',
    '..o65544433221o.',
    '..o5443322211o..',
    '...o43322111o...',
    '....oo32211o....',
    '......oooo......',
    '................',
    '................',
    '................',
]


def own(rows):
    """Own drawing from a tone map: 1..8 dark -> light on the raw enderite ramp, o = the ingot's outline,
    A/B = pearl teal (the broken face of a geode-like chunk)."""
    cols = raw_colours()
    pal = {str(k): cols[min(len(cols) - 1, int((k - 0.5) / 8 * len(cols)))] for k in range(1, 9)}
    pal.update({'o': nug.OUTLINE, 'A': PEARL[0], 'B': PEARL[1]})
    return ps.grid(rows, pal)


def proposals():
    scrap, gold, iron = vitem('netherite_scrap'), vitem('raw_gold'), vitem('raw_iron')
    cols = raw_colours()
    a = nug.matched(scrap, cols)
    return [
        ('Netheritschrott-Form', a),
        ('Bruchstueck mit Perl-Kern (eigen)', own(GEODE)),
        ('Rohgold-Klumpen', nug.matched(gold, cols)),
        ('Roheisen-Klumpen', nug.matched(iron, cols)),
        ('Klumpen mit Schuttringen', debris_chunk()),
        ('Schrott + Perlen-Glanz', glints(a, PEARL, [(6, 5), (10, 9), (5, 11)])),
        ('gebrochene Schichten (eigen)', own(SLABS)),
        ('Rohklumpen mit Bruchkante (eigen)', own(LUMP)),
        ('Schrott, mehr Kontrast', nug.matched(scrap, cols, contrast=1.35)),
        ('Rohgold-Klumpen + Endstein-Staub', glints(nug.matched(gold, cols), END_STONE, [(4, 6), (9, 4), (12, 10), (7, 12)])),
    ]


def main():
    props = proposals()
    item = lambda n: ps.load(os.path.join(ps.SB_ITEM, n + '.png'))
    refs = [('Rohes Enderit-Stueck jetzt', item('layered_raw_enderite')), ('Enderit-Fragment', item('raw_enderite')),
            ('Enderitschrott', item('enderite_scrap')), ('Netheritschrott', vitem('netherite_scrap')),
            ('Antiker Schutt', vblock('ancient_debris_side')), ('Rohgold', vitem('raw_gold')), ('Roheisen', vitem('raw_iron'))]
    notes = [f'{ps.LETTERS[k]}: {n}' for k, (n, _) in enumerate(props)]
    ps.sheet('Raw Enderite Scrap (layered_raw_enderite) - 10 Vorschlaege', refs, [('Vorschlag', [im for _, im in props])],
             PREVIEW, scale=10, notes=['  '.join(notes[:5]), '  '.join(notes[5:])])
    print('ok')


if __name__ == '__main__':
    main()
