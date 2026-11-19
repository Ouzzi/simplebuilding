"""Simple Dinosaurs: item sprites of DINO-REST step 1 (a-dinorest1, concept KONZEPT-A-DINO36 6): the Triceratops armor in three tiers
and the eggshells of the four dinosaurs that leave one (Triceratops, Stegosaurus, Utahraptor, Parasaurolophus); step 2 the Stegosaurus
armor and the Dino Plow, step 3 the Parasaurolophus armor, step 5 (b-utah48) the Utahraptor armor and the raptor feather.

Own pixel art drawn from code (ASCII sprites, tools/textures/optik32_engine.py shading after A8b: lit top/left, shaded bottom/right,
rim in the darkest tones of the same ramp, no black outline). The armor follows the T. rex armor (owner choice IG31-23, variant C:
optik32_items.armor) in tiers and ramps, in the Triceratops' shape: a frill shield with bony rim knobs, two brow-horn caps and a
nose cap on a strap. The eggshell is a broken half shell with its pale inside and a splinter, coloured from the egg's own ramp and
motif tone (tools/eggs.py, variant C), so shell and egg belong together. gen_blocks.py writes them; the rounds are in
docs/ai/PLAN-A-DINOREST1-2026-11-12.md.
"""
from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[2] / 'tools/textures'))
import eggs as EG  # noqa: E402
from optik32_engine import paint, ramp  # noqa: E402
from PIL import Image  # noqa: E402

N = 7
SHELL_KINDS = ('triceratops', 'stegosaurus', 'utahraptor', 'parasaurolophus')
ARMOR_TIERS = ('leather', 'iron', 'diamond')
# main tone and trim tone per tier, the same as the T. rex armor (optik32_items.TIERS)
TIERS = {'leather': ('9a6234', 'c9a23a'), 'iron': ('a8adb4', 'd8d4cc'), 'diamond': ('3fd0c4', '9af0e4')}
# N89A (owner N77): the precious metals for every dinosaur (the T. rex also copper, Netherite, Enderite); same tones as optik32_items.TIERS
METAL_TIERS = ('gold', 'rose_gold', 'rose_bronze')
TIERS.update({'copper': ('b0562c', 'de8a54'), 'gold': ('e0b020', 'fbe46a'), 'rose_gold': ('d2877f', 'efc0b4'),
              'rose_bronze': ('d37d5d', 'f2b88f'), 'netherite': ('4a4044', '7a6e70'), 'enderite': ('6d45b8', 'c77dff')})

# frill shield: c brow-horn caps, r rim with knobs, h shield, s strap, n nose cap
ARMOR = ['................',
         '.cc..........cc.',
         '.ccc........ccc.',
         '..ccc.k.kk.kcc..',
         '...cckrhhrkcc...',
         '...krhhvvhhrk...',
         '..krhhhvvhhhrk..',
         '.krhhhhvvhhhhrk.',
         '.rhhhhhvvhhhhhr.',
         '.rhhhhhvvhhhhhr.',
         '..rhhhhvvhhhhr..',
         '...rhhhvvhhhr...',
         '....ssshhsss....',
         '......hnnh......',
         '.......nn.......',
         '................']
# tier details on top of the shading (as on the T. rex armor): leather stitches, iron rivets, diamond facets
DETAILS = {'leather': [(4, 7), (4, 9), (11, 7), (11, 9), (6, 11), (9, 11)],
           'iron': [(4, 8), (11, 8), (5, 10), (10, 10)],
           'diamond': [(3, 7), (4, 6), (12, 7), (11, 6), (5, 10), (10, 10)]}
DETAILS.update({m: DETAILS['iron'] for m in METAL_TIERS})   # N89A: the metals carry the iron rivets


def _hex(c):
    return '%02x%02x%02x' % c


def armor(tier: str):
    main, trim = TIERS[tier]
    ramps = {'h': ramp(main, N), 'r': ramp(main, N, spread=0.2, sat_shift=0.05), 'c': ramp(trim, N), 'n': ramp(trim, N),
             's': ramp('5a3a22', N), 'k': ramp(trim, N, spread=0.1), 'v': ramp(main, N, spread=0.1)}
    img = paint(ARMOR, ramps, seed=23, base={'k': N // 2 + 1, 'v': N // 2 + 1})
    light = ramp(main, N)[N - 2]
    for x, y in DETAILS[tier]:
        img.putpixel((x, y), light + (255,))
    return img


# half shell: s shell, i inside (pale membrane), e broken edge, f splinter, p motif groups of the egg
SHELL = ['................',
         '................',
         '................',
         '......e...e.....',
         '....e.ee.eee.e..',
         '...eeiiiiiiiee..',
         '..seiiiiiiiiies.',
         '..ssiiiiiiiiipp.',
         '..sssiiiiiiissp.',
         '..ppsssssssssss.',
         '..spsssssssssss.',
         '...sssssssssss..',
         '....sssssppss...',
         '......ssssp.....',
         '.ff.............',
         '.fpf............']


def eggshell(kind: str):
    shell = EG.ramp(kind, 'C')              # five tones dark -> light (A8b steps of about 18 luminance)
    tones = EG.pat(kind, 'C')               # motif shadow, tone, light
    base = shell[2]
    # the inside is the pale membrane tinted toward the shell's light tone, so the rim is not a hard white cut (A8b)
    inside = tuple((a + b) // 2 for a, b in zip((0xe6, 0xdc, 0xc4), shell[4]))
    ramps = {'s': ramp(_hex(base), N, spread=0.12), 'e': ramp(_hex(shell[3]), N, spread=0.1), 'f': ramp(_hex(base), N, spread=0.12),
             'i': ramp(_hex(inside), N, spread=0.1), 'p': ramp(_hex(tones[1]), N, spread=0.1)}
    return paint(SHELL, ramps, seed=29)


# ---------------------------------------------------------------- a-dinorest2: Stegosaurus armor and the Dino Plow
# Stegosaurus armor (side view as worn): the flank plate h with its rim r, open on top where the back plates a stay free (drawn in the
# warm plate tone, so the shape reads as Stegosaurus armor), the neck guard n in the trim tone; v tier details
STEGO_ARMOR = ['................',
               '................',
               '.......a...a....',
               '......aa..aa..a.',
               '....rraarraarraa',
               '...rhhrrhhrrhhrr',
               '..rhhhvhhhhhvhhr',
               '.rhhhhhhhhhhhhhr',
               'nnrhhhhhhhhhhhhr',
               'nnnrhhhhhhhhhhr.',
               '.nnrhhhvhhhhvhr.',
               '..nrrhhhhhhhhr..',
               '.....rrrrrrrr...',
               '................',
               '................',
               '................']
STEGO_DETAILS = {'leather': [(5, 8), (8, 8), (11, 8), (14, 8)], 'iron': [(5, 6), (14, 6), (5, 10), (13, 10)], 'diamond': [(6, 7), (10, 9), (13, 6)]}
STEGO_DETAILS.update({m: STEGO_DETAILS['iron'] for m in METAL_TIERS})   # N89A


def stego_armor(tier: str, rows=None):
    main, trim = TIERS[tier]
    ramps = {'h': ramp(main, N), 'r': ramp(main, N, spread=0.2, sat_shift=0.05), 'n': ramp(trim, N), 'a': ramp('c07444', N, spread=0.12),
             'v': ramp(main, N, spread=0.1)}
    img = paint(rows or STEGO_ARMOR, ramps, seed=31, base={'v': N // 2 + 1})
    light = ramp(main, N)[N - 2]
    for x, y in STEGO_DETAILS[tier]:
        img.putpixel((x, y), light + (255,))
    return img


# Dino Plow (front view, the recipe in a picture): the pull ring and chain c on top, the wooden cross beam w, three posts p and three
# iron shares s one block apart
PLOW = ['................',
        '......cccc......',
        '......c..c......',
        '.......kk.......',
        '.......cc.......',
        '..wwggwwwwwggw..',
        '.wwwwwwggwwwwww.',
        '..ddddddddddddd.',
        '...p....p....p..',
        '..sp...sp...sp..',
        '..sss..sss..sss.',
        '..sss..sss..sss.',
        '...ss...ss...ss.',
        '....s....s....s.',
        '................',
        '................']


def plow(rows=None):
    ramps = {'w': ramp('9a7448', N), 'g': ramp('8a6640', N, spread=0.1), 'd': ramp('7a5a36', N), 'p': ramp('6e5030', N), 'c': ramp('8a8e96', N), 'k': ramp('6a6e76', N),
             's': ramp('b8bcc2', N, spread=0.12)}
    return paint(rows or PLOW, ramps, seed=37)


# ---------------------------------------------------------------- a-dinorest3: Parasaurolophus armor
# Parasaurolophus armor (side view, facing left; rounds in docs/ai/PLAN-A-DINOREST3-2026-11-12.md): the crest hood c over the skull and the
# long crest tube sweeping back and up, its rim k in the trim tone, the eye hole b, the duck bill a in the dinosaur's own skin tone (so the
# shape reads as a Parasaurolophus), the chin strap n; below it the hip blanket h with its rim r, a trim band n, tier details v and fringe f
PARA_ARMOR = ['................',
              '............kcc.',
              '..........kcccc.',
              '........kcccc...',
              '......kcccc.....',
              '.....kccc.......',
              '...kccccc.......',
              '..kcbccccc......',
              '.aacccccck......',
              'aaaannnnk.......',
              '................',
              '......rrrrrrrr..',
              '.....rhhhhhhhhr.',
              '.....rnnnnnnnnr.',
              '.....rhhvhhvhhr.',
              '......f.f.f.f...']


def _toward(a: str, b: str, t: float) -> str:
    """Hex colour a moved the share t toward b."""
    pa, pb = [int(a[i:i + 2], 16) for i in (0, 2, 4)], [int(b[i:i + 2], 16) for i in (0, 2, 4)]
    return '%02x%02x%02x' % tuple(round(pa[i] + (pb[i] - pa[i]) * t) for i in range(3))


def para_armor(tier: str, rows=None, trim_mix=0.35):
    """Round 4: the trim tone moved 35 % toward the main tone (hard transitions 25 -> 7-10 %, A8b), the shapes of round 3."""
    main, trim = TIERS[tier]
    trim = _toward(trim, main, trim_mix)
    ramps = {'c': ramp(main, N), 'k': ramp(trim, N), 'h': ramp(main, N), 'r': ramp(main, N, spread=0.2, sat_shift=0.05), 'n': ramp(trim, N),
             'v': ramp(main, N, spread=0.1), 'f': ramp(trim, N), 'a': ramp('6e7a48', N, spread=0.12), 'b': ramp('3a4024', N, spread=0.1)}
    return paint(rows or PARA_ARMOR, ramps, seed=41, base={'v': N // 2 + 1})


# ---------------------------------------------------------------- b-utah48: Utahraptor armor and the raptor feather
# Utahraptor armor (side view, facing left; rounds in docs/ai/PLAN-B-UTAH48-2026-11-14.md): the raptor's head a (its own skin tone, eye e,
# so the shape reads as a Utahraptor) on the S-neck covered by the neck guard g with the band rims k in the trim tone, the chest plate h with
# its rim r, a trim band n and tier details v, a shin guard s with straps t bottom left; the claws stay free. The claws stay free, so nothing covers the feet.
RAPTOR_ARMOR = ['................',
                '.aaaa...........',
                'aaaaae..........',
                '..aaakgk........',
                '.....gggk.......',
                '.....kgggk......',
                '......kgggk.....',
                '.......kgggrr...',
                '.......rhhhhhr..',
                '......rhvhhvhhr.',
                '......rnnnnnnnr.',
                '......rhhhvhhhr.',
                '.ss....rhhhhhr..',
                'ssss....rrrrr...',
                'tsst............',
                'ssss............']


def raptor_armor(tier: str, rows=None, trim_mix=0.35):
    main, trim = TIERS[tier]
    trim = _toward(trim, main, trim_mix)
    ramps = {'g': ramp(main, N), 'k': ramp(trim, N), 'h': ramp(main, N), 'r': ramp(main, N, spread=0.2, sat_shift=0.05), 'n': ramp(trim, N),
             'v': ramp(main, N, spread=0.1), 's': ramp(main, N, spread=0.15), 't': ramp('5a3a22', N), 'a': ramp('6e7a48', N, spread=0.12),
             'e': ramp('3a4024', N, spread=0.1)}
    return paint(rows or RAPTOR_ARMOR, ramps, seed=43, base={'v': N // 2 + 1})


# Raptor feather (diagonal, quill bottom left): the vane f in the plumage tone with darker bands b (the banded arm and tail feathers of the
# model), a light tip l, the quill q; reads as a feather but browner and banded, unlike the white Vanilla feather.
RAPTOR_FEATHER = ['................',
                  '..........lll...',
                  '.........lllll..',
                  '........fflllf..',
                  '.......ffffqff..',
                  '......bbbfqfbb..',
                  '.....ffffqffff..',
                  '.....fffqffff...',
                  '....bbbqfbbb....',
                  '....ffqffff.....',
                  '....fqffff......',
                  '...fqfff........',
                  '...qff..........',
                  '..q.............',
                  '.q..............',
                  'q...............']


def raptor_feather(rows=None):
    ramps = {'f': ramp('8a6a44', N), 'b': ramp('6a5034', N, spread=0.1), 'l': ramp('c8b48a', N, spread=0.1), 'q': ramp('d8ccb0', N, spread=0.08)}
    return paint(rows or RAPTOR_FEATHER, ramps, seed=47)


# ---------------------------------------------------------------- N96A (owner 18.11.): the armor icons redrawn closer to Vanilla
# Style model is the Vanilla horse armor icon (principle only, no Mojang pixels): a side view of the animal wearing the armor, head to
# the right; a few clean tone bands per material instead of noise - light edge where the outline faces top/left, the darkest tone where
# it faces bottom/right, a shadow band above the bottom edge, a gloss pixel on top-left corners; a trim piece in the tier's trim tone
# like the Vanilla saddle cloth; uncovered skin, teeth and claws in the dinosaur's own tones so every armor reads as its dinosaur.
# Regions: a armor, t trim, s strap leather, k skin, w bone (teeth, claws, horns), e eye, p Stegosaurus back plate, x armor with the
# tier detail (leather stitch, metal rivet, diamond facet).
SKIN = {'trex': '5f6b3c', 'triceratops': '7d6a4c', 'stegosaurus': '6f7a44', 'parasaurolophus': '6e7a48', 'utahraptor': '7a6448'}


def _mix(c, d, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c, d))


def vramp(hexcol: str) -> list:
    """Six tones dark -> light: outline, shadow, shade, base, light, gloss. Shadows lean cool, lights warm (Vanilla metal ramps)."""
    c = tuple(int(hexcol[i:i + 2], 16) for i in (0, 2, 4))
    cool, warm = (22, 16, 38), (255, 250, 236)
    return [_mix(c, cool, 0.62), _mix(c, cool, 0.42), _mix(c, cool, 0.2), c, _mix(c, warm, 0.3), _mix(c, warm, 0.58)]


def vpaint(rows, ramps, flat=''):
    h, w = len(rows), len(rows[0])
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))

    def at(x, y):
        c = rows[y][x] if 0 <= x < w and 0 <= y < h else '.'
        return 'a' if c == 'x' else c

    for y in range(h):
        for x in range(w):
            c = at(x, y)
            if c == '.':
                continue
            R = ramps[c]
            if c in flat:
                im.putpixel((x, y), R[3] + (255,))
                continue
            up, left, down, right = at(x, y - 1), at(x - 1, y), at(x, y + 1), at(x + 1, y)
            if down == '.' or right == '.':
                i = 1 if (up == '.' or left == '.') and down != '.' else 0      # outline: darkest bottom/right
            elif up == '.' and left == '.':
                i = 5                                                           # gloss corner
            elif up == '.' or left == '.':
                i = 4                                                           # light edge top/left
            elif down != c or right != c:
                i = 2                                                           # seam towards another region
            elif at(x, y + 2) == '.':
                i = 2                                                           # shadow band above the bottom edge
            elif up != c or left != c:
                i = 4
            else:
                i = 3
            im.putpixel((x, y), R[i] + (255,))
    return im


TREX_V = ['................',
          '.........aaaaa..',
          '........aaaaaaa.',
          '........aaxeaaaa',
          '.......taaaaaaaa',
          '.......tw.w.w.w.',
          '......atkkkkkkk.',
          '....aaatt.......',
          '..aaaaaatttaa...',
          '.aaaxaaatttaaak.',
          'aaaaaaaatttaaa..',
          '..aaaaaaaaaaa...',
          '....aaa..aaa....',
          '....aa....aa....',
          '...waw...waw....',
          '................']
TRI_V = ['.......t.t.t....',
         '......tatatat...',
         '......taaaaat...',
         '......taaaaaat.w',
         '......taaaaaaww.',
         '......taaaeaaa..',
         '..aaaaataaaaaakw',
         '.aaaxaataaaaakk.',
         'aaaattttaaaaa...',
         'aaaattttaaxaa...',
         '.aaattttaaaaa...',
         '..aaaaaaaaaaa...',
         '..aaa....aaa....',
         '..aa......aa....',
         '..ww......ww....',
         '................']
STEGO_V = ['................',
           '.......p...p....',
           '....p..pp..pp...',
           '...pp.ppp.ppp.p.',
           '..ppaaaaaaaaapp.',
           '...aaaaaaaaaaaa.',
           '..aaaattttaaaaa.',
           'w.aaxatttttaaaak',
           '.waaaatttttaaakk',
           '..aaaaaaaaaaaake',
           '...aaaaaaaxaa.kk',
           '....aaaaaaaaa...',
           '....aaa...aaa...',
           '....aa.....aa...',
           '....ww.....ww...',
           '................']
PARA_V = ['................',
          '.....ttt........',
          '......tttt......',
          '.........tta....',
          '..........aaa...',
          '..........axaak.',
          '..........aeakkk',
          '......aaaaaaa...',
          '...aaaaaaaaaa...',
          '.aaaattttaaaa...',
          'aaaaatttxaaaak..',
          '.aaaattttaaa.k..',
          '..aaaaaaaaaa....',
          '....aaa..aaa....',
          '....aa....aa....',
          '...waw...waw....']
RAPTOR_V = ['................',
            '..........aaaa..',
            '.........aaaeaak',
            '.........aakkkww',
            '........taa.....',
            '........taa.....',
            '.......taa......',
            'aa....aaaaaaa...',
            '.aaaaaaxatttaa..',
            '..aaaaaaatttaaw.',
            '....aaaaaatttaa.',
            '......aaaaaaa...',
            '.......aaa.aa...',
            '.......aa..aa...',
            '......waw..aaw..',
            '.....w.....w....']
V_SHAPES = {'trex': TREX_V, 'triceratops': TRI_V, 'stegosaurus': STEGO_V, 'parasaurolophus': PARA_V, 'utahraptor': RAPTOR_V}


def vanilla_armor(dino: str, tier: str, rows=None):
    """N96A armor icon of the dinosaur in the tier (see the style notes above)."""
    rows = rows or V_SHAPES[dino]
    main, trim = TIERS[tier]
    ramps = {'a': vramp(main), 't': vramp(_toward(trim, main, 0.2)), 's': vramp('6a4428'), 'k': vramp(SKIN[dino]), 'w': vramp('e4dcc4'),
             'e': [(30, 24, 28)] * 6, 'p': vramp('c07444')}
    img = vpaint(rows, ramps, flat='e')
    a = ramps['a']
    detail = {'leather': a[1], 'diamond': a[5]}.get(tier, a[5])
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == 'x':
                img.putpixel((x, y), detail + (255,))
    return img


def trex_armor(tier: str):
    return vanilla_armor('trex', tier)


def all_sprites():
    """{name: image} of every sprite this file draws."""
    out = {f'triceratops_armor_{t}': vanilla_armor('triceratops', t) for t in ARMOR_TIERS + METAL_TIERS}  # N96A
    out.update({f'{k}_eggshell': eggshell(k) for k in SHELL_KINDS})
    out.update({f'stegosaurus_armor_{t}': vanilla_armor('stegosaurus', t) for t in ARMOR_TIERS + METAL_TIERS})  # N96A
    out['dino_plow'] = plow()
    out.update({f'parasaurolophus_armor_{t}': vanilla_armor('parasaurolophus', t) for t in ARMOR_TIERS + METAL_TIERS})  # N96A
    out.update({f'utahraptor_armor_{t}': vanilla_armor('utahraptor', t) for t in ARMOR_TIERS + METAL_TIERS})  # N96A
    out['raptor_feather'] = raptor_feather()
    return out


if __name__ == '__main__':
    import subprocess
    out = Path(sys.argv[1] if len(sys.argv) > 1 else '.')
    out.mkdir(parents=True, exist_ok=True)
    paths = []
    for name, img in all_sprites().items():
        p = out / f'{name}.png'
        img.save(p)
        paths.append(str(p))
    subprocess.run([sys.executable, str(HERE.parents[2] / 'tools/textures/texrate.py'), *paths])
