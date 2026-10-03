"""Usage: python tools/textures/astralit_nihilit_material_proposals_2026_10_03.py <vanilla textures dir> [preview png]

Owner 2026-10-03: the MATERIALS Astralit (item astralit_dust) and Nihilith (item nihilith_shard) - ten proposals
each, own forms in the manner of vanilla's crystal items (amethyst shard, echo shard, nether quartz, prismarine
shard): one-pixel dark outline, light from the top left, facets as flat tone fields, a white glint. Proposals only.
The same ten drawn forms serve both materials, each in its own ramp taken from the current item (astralit pink,
nihilith blue-teal), stretched for clear facets."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import proposal_sheet_2026_10_02 as ps  # noqa: E402

V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
PREVIEW = sys.argv[2] if len(sys.argv) > 2 else os.path.join(ps.ROOT, 'build', 'astralit-nihilit-material-vorschau.png')

# tone maps: 1..6 dark -> light, o outline, w glint
FORMS = {
    'langer Splitter': [
        '................', '...........oo...', '..........o6wo..', '.........o654o..', '........o6543o..',
        '.......o6543o...', '......o6543o....', '.....o6543o.....', '....o6543o......', '...o5432o.......',
        '..o4321o........', '..o321o.........', '..oooo..........', '................', '................',
        '................'],
    'drei Kristalle': [
        '................', '................', '......o.........', '.....o6o....o...', '.....o5o...o6o..',
        '..o..o5o...o54o.', '.o6o.o43o..o43o.', '.o54oo432o.o32o.', '.o432o321oo321o.', '..o321o21o.o21o.',
        '..oo21oo1ooo1o..', '...ooooooooooo..', '................', '................', '................',
        '................'],
    'geschliffener Stein': [
        '................', '................', '.....oooooo.....', '....o6w6554o....', '...o66655443o...',
        '..o6655544332o..', '..o5544433221o..', '...o44332211o...', '....o433211o....', '.....o3221o.....',
        '......o21o......', '.......oo.......', '................', '................', '................',
        '................'],
    'Staubhaufen': [
        '................', '................', '................', '................', '................',
        '.......w........', '......o6o.......', '....oo665oo.....', '...o6655443o.w..', '..o665544332o...',
        '.o66554433221o..', '.o54433221111o..', '..oooooooooooo..', '................', '................',
        '................'],
    'gezackter Bruch': [
        '................', '..........o.....', '.........o6o....', '........o65o....', '.......o654oo...',
        '......o6543o....', '.....o65432o....', '....o6543o21o...', '...o6543o.oo....', '..o5432o........',
        '..o4321o........', '...o21o.........', '....oo..........', '................', '................',
        '................'],
    'Sechskant-Prisma': [
        '................', '.......oo.......', '......o6wo......', '.....o6655o.....', '.....o6545o.....',
        '.....o6545o.....', '.....o5434o.....', '.....o5434o.....', '.....o4323o.....', '.....o4323o.....',
        '.....o3212o.....', '......o21o......', '.......oo.......', '................', '................',
        '................'],
    'gekreuzte Splitter': [
        '................', '..o.........o...', '.o6o.......o6o..', '.o65o.....o65o..', '..o54o...o54o...',
        '...o43o.o43o....', '....o32o32o.....', '.....o2w1o......', '....o32o21o.....', '...o43o.o21o....',
        '..o43o...o21o...', '..o3o.....o1o...', '...o.......o....', '................', '................',
        '................'],
    'Rohkristall': [
        '................', '................', '......ooo.......', '....oo656o......', '...o66w554o.....',
        '..o665544o3o....', '..o6544332o2o...', '..o5443322o1o...', '...o43322211o...', '...o3322111o....',
        '....oo2211o.....', '......oooo......', '................', '................', '................',
        '................'],
    'Stern': [
        '................', '.......o........', '......o6o.......', '......o6o.......', '.....o654o......',
        '..ooo6w543ooo...', '.o66655443322o..', '..ooo4433ooo....', '.....o432o......', '......o3o.......',
        '......o2o.......', '.......o........', '................', '................', '................',
        '................'],
    'Splitter auf Stein': [
        '................', '...........o....', '..........o6o...', '.........o65o...', '........o654o...',
        '.......o654o....', '.....oo6543o....', '....o65543o.....', '...oSs5432oo....', '..oSSss321sso...',
        '..osssSssssso...', '...oossssooo....', '.....oooo.......', '................', '................',
        '................'],
}


def ramp_of_item(name):
    im = ps.load(os.path.join(ps.SB_ITEM, name + '.png'))
    cols = ps.ramp_of(im)
    return [cols[round(i * (len(cols) - 1) / 5)] for i in range(6)]


def render(rows, ramp):
    pal = {str(k + 1): ramp[k] for k in range(6)}
    pal.update({'o': ps.mix(ramp[0], (0, 0, 0), 0.55), 'w': ps.mix(ramp[5], (255, 255, 255), 0.6),
                'S': (110, 108, 104), 's': (78, 76, 74)})
    return ps.grid(rows, pal)


def main():
    # the current items' ramps are too flat for crystal facets (vanilla shards step clearly from facet to facet), so the
    # hue of each item is kept and the ramp stretched: Astralit pink, Nihilith blue-teal
    mats = [('Astralit', [(96, 30, 74), (150, 56, 116), (200, 98, 160), (232, 150, 200), (248, 196, 228), (255, 232, 244)]),
            ('Nihilith', [(20, 38, 78), (36, 78, 128), (58, 122, 166), (96, 168, 196), (150, 210, 222), (210, 244, 246)])]
    rows = [(name, [render(g, ramp) for g in FORMS.values()]) for name, ramp in mats]
    vitem = lambda n: ps.load(os.path.join(V, 'item', n + '.png'))
    refs = [('Astralit-Staub jetzt', ps.load(os.path.join(ps.SB_ITEM, 'astralit_dust.png'))),
            ('Nihilith-Splitter jetzt', ps.load(os.path.join(ps.SB_ITEM, 'nihilith_shard.png'))),
            ('Amethystscherbe', vitem('amethyst_shard')), ('Echoscherbe', vitem('echo_shard')),
            ('Netherquarz', vitem('quartz')), ('Prismarinscherbe', vitem('prismarine_shard'))]
    names = list(FORMS)
    ps.sheet('Astralit / Nihilith Material - je 10 Vorschlaege (eigene Kristallformen im Vanilla-Stil)', refs, rows,
             PREVIEW, scale=10, notes=['  '.join(f'{ps.LETTERS[k]}: {n}' for k, n in enumerate(names[:5])),
                                       '  '.join(f'{ps.LETTERS[k + 5]}: {n}' for k, n in enumerate(names[5:]))])
    print('ok')


if __name__ == '__main__':
    main()
