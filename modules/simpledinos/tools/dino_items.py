"""Simple Dinosaurs: item sprites of DINO-REST step 1 (a-dinorest1, concept KONZEPT-A-DINO36 6): the Triceratops armor in three tiers
and the eggshells of the four dinosaurs that leave one (Triceratops, Stegosaurus, Utahraptor, Parasaurolophus).

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

N = 7
SHELL_KINDS = ('triceratops', 'stegosaurus', 'utahraptor', 'parasaurolophus')
ARMOR_TIERS = ('leather', 'iron', 'diamond')
# main tone and trim tone per tier, the same as the T. rex armor (optik32_items.TIERS)
TIERS = {'leather': ('9a6234', 'c9a23a'), 'iron': ('a8adb4', 'd8d4cc'), 'diamond': ('3fd0c4', '9af0e4')}

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


def all_sprites():
    """{name: image} of every sprite this file draws."""
    out = {f'triceratops_armor_{t}': armor(t) for t in ARMOR_TIERS}
    out.update({f'{k}_eggshell': eggshell(k) for k in SHELL_KINDS})
    out.update({f'stegosaurus_armor_{t}': stego_armor(t) for t in ARMOR_TIERS})
    out['dino_plow'] = plow()
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
