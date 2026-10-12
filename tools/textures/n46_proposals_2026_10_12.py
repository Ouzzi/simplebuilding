"""Nachtrag 46 texture proposals (owner 2026-10-12, screenshot docs/ai/refs/n44/n46_inventar.png).

Proposals only, nothing goes into the game textures (the paint box tiers are built in by paint_box_2026_10_09.py):
  Q1-Q3  questionary table, more Vanilla (lectern / cartography / fletching / enchanting table feel: plain wood and a
         book, few tones), drawn for the existing questionary_table model (same UV layout).
  A1-A3  astral vault, N1-N3 nihil vault: a Vanilla vault like cube (stone frame bands, metal cage with see-through
         windows, light strip, front states inactive / active / ejecting), only material and colours astral / nihil.

Own pixel art from code; Vanilla textures are only looked at as references (never copied or checked in).
Run: python3.12 tools/textures/n46_proposals_2026_10_12.py [--html out.html]
Writes the 16x16 PNGs to docs/ai/previews/n46/ and optionally one review page with data URIs.
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import subprocess
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402  (render, helpers)
import paint_box_2026_10_09 as pbox  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/ai/previews/n46'
QMOD = ROOT / 'modules/simplequestionary/shared/resources/assets/simplequestionary'
BASE = 'c80f6f054'  # claude-wave1 before N46 (for the 'before' column)
SHOT = ROOT / 'docs/ai/refs/n44/n46_inventar.png'
H = n41.H


def new():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def from_rows(rows, pal):
    im = new()
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch in pal:
                im.putpixel((x, y), H(pal[ch]))
    return im


# ======================================================================= questionary table
# UV use of the model: side rows 4-5 top slab edge, rows 6-12 cabinet (cols 1-14), rows 13-15 plinth; top 16x16;
# bottom for plinth top/bottom and the slab underside; gem [0,0,2,3] post sides + [2,0,4,2] post top;
# book: cover [0,8,10,16], cover edge [10,0,16,1], pages [0,0,5,7] + [5,0,10,7], page edges [0,7,7,8].

def planks_h(im, ramp, y0, y1, seams=(3, 11), shift=0):
    """Horizontal planks: rows with a light top line, a dark bottom seam, staggered vertical joints."""
    for y in range(y0, y1 + 1):
        k = (y - y0) % 4
        for x in range(16):
            c = ramp[3] if k == 0 else (ramp[1] if k == 3 else ramp[2])
            if k in (1, 2) and x == (seams[((y - y0) // 4 + shift) % 2]):
                c = ramp[1]
            if k in (1, 2) and n41.hsh(x, y, 7) > 0.86:
                c = ramp[3] if k == 1 else ramp[1]
            im.putpixel((x, y), H(c))


def planks_v(im, ramp, y0, y1):
    for y in range(y0, y1 + 1):
        for x in range(16):
            k = x % 4
            c = ramp[3] if k == 0 else (ramp[1] if k == 3 else ramp[2])
            if k in (1, 2) and y == y0 + 4 + (x // 4) * 2 % (y1 - y0 + 1):
                c = ramp[1]
            im.putpixel((x, y), H(c))


def book_tex(cover, pages=('f2e8cc', 'ddd0ae', 'b8a87e'), ink='5a4a3a'):
    """Shared plain book: leather cover board, cream pages with a few ink lines (lectern book feel)."""
    im = new()
    c0, c1, c2 = cover
    for y in range(8, 16):
        for x in range(10):
            edge = x in (0, 9) or y in (8, 15)
            im.putpixel((x, y), H(c0 if edge else (c2 if (x + y) % 5 == 0 else c1)))
    for x in range(10, 16):
        im.putpixel((x, 0), H(c0))
    for x in range(10):
        for y in range(7):
            right = x >= 5
            lx = x - 5 if right else x
            c = pages[0]
            if (not right and lx == 4) or (right and lx == 0):
                c = pages[1]                              # gutter shade
            elif y in (1, 3, 5) and 1 <= lx <= 3:
                c = ink if n41.hsh(x, y, 2) > 0.3 else pages[1]
            im.putpixel((x, y), H(c))
    for x in range(7):
        im.putpixel((x, 7), H(pages[2] if x % 2 else pages[1]))
    return im


def post_tex(side, dark, top):
    im = new()
    for y in range(3):
        im.putpixel((0, y), H(side))
        im.putpixel((1, y), H(dark))
    for y in range(2):
        for x in (2, 3):
            im.putpixel((x, y), H(top))
    return im


QUESTION = ['.##.', '#..#', '...#', '..#.', '..#.', '....', '..#.']  # 4x7 question mark


def stamp(im, x0, y0, pattern, col):
    for dy, row in enumerate(pattern):
        for dx, ch in enumerate(row):
            if ch == '#':
                im.putpixel((x0 + dx, y0 + dy), H(col))


def qtable(v):
    """Returns dict side/top/bottom/gem/book for proposal Q(v+1)."""
    side, top, bot = new(), new(), new()
    if v == 0:      # Q1 oak desk: lectern / bookshelf, a shelf of muted books in the cabinet
        W = ('4c3219', '6a4a26', '8a6436', 'a47c45', 'bc9458')
        planks_h(side, W, 0, 15)
        for x in range(16):
            side.putpixel((x, 4), H(W[4]))
            side.putpixel((x, 5), H(W[1]))
            side.putpixel((x, 13), H(W[3]))
            side.putpixel((x, 15), H(W[0]))
        for y in range(6, 13):
            for x in range(1, 15):
                edge = x in (1, 14) or y in (6, 12)
                side.putpixel((x, y), H(W[1] if edge else '2c1c0e'))
        books = ['8e3a2e', '8e3a2e', '4e6a34', '3a4e78', '3a4e78', 'a47c45', '6e2a4a', '4e6a34', '8e3a2e', '3a4e78',
                 '6a4a26', '4e6a34']
        for i, col in enumerate(books):
            x = 2 + i
            h = 4 + int(n41.hsh(i, 1, 9) * 2)
            for y in range(12 - h, 12):
                c = n41.mix(H(col), H('ffffff'), 0.25) if y == 12 - h else (n41.mix(H(col), H('000000'), 0.3)
                                                                         if i % 3 == 2 else H(col))
                side.putpixel((x, y), c)
        planks_h(top, W, 0, 15, seams=(5, 12))
        for i in range(16):
            for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
                top.putpixel((x, y), H(W[1]))
            for (x, y) in ((i, 1), (1, i)):
                if 1 <= i <= 14:
                    top.putpixel((x, y), H(W[4]))
        planks_h(bot, ('3a2614', '4c3219', '6a4a26', '7e5a30'), 0, 15)
        gem = post_tex(W[3], W[1], W[4])
        book = book_tex(('3a2214', '5a3a22', '6e4a2c'))
    elif v == 1:    # Q2 dark oak map table: cartography table feel, a parchment sheet with an inked question mark
        D = ('22150a', '33210f', '46301a', '5a3f24', '6e5232')
        planks_v(side, D, 0, 15)
        for x in range(16):
            side.putpixel((x, 4), H(D[4]))
            side.putpixel((x, 5), H(D[2]))
            side.putpixel((x, 13), H(D[3]))
            side.putpixel((x, 15), H(D[0]))
        for y in range(6, 13):
            side.putpixel((1, y), H(D[3]))
            side.putpixel((14, y), H(D[1]))
        for x in range(4, 12):      # drawer with a brass knob
            side.putpixel((x, 8), H(D[1]))
            side.putpixel((x, 11), H(D[1]))
        for y in range(8, 12):
            side.putpixel((4, y), H(D[1]))
            side.putpixel((11, y), H(D[1]))
        side.putpixel((7, 9), H('c8a040'))
        side.putpixel((8, 9), H('8a6a24'))
        planks_v(top, D, 0, 15)
        P = ('ece0c4', 'd8c8a2', 'b8a47a')
        for y in range(3, 13):
            for x in range(3, 13):
                c = P[0] if n41.hsh(x, y, 5) > 0.2 else P[1]
                if x == 12 or y == 12:
                    c = P[2]
                top.putpixel((x, y), H(c))
        stamp(top, 6, 4, QUESTION, '3a2e24')
        top.putpixel((11, 11), H('9a2e2a'))         # wax seal
        top.putpixel((10, 11), H('6e1e1c'))
        for y in range(2, 6):                       # quill to the right of the sheet
            top.putpixel((13, y), H('f4f0e8') if y < 5 else H('3a2e24'))
        top.putpixel((14, 2), H('d8d4cc'))
        planks_v(bot, ('180e06', '22150a', '33210f', '46301a', '5a3f24'), 0, 15)
        gem = post_tex(D[3], D[1], D[4])
        book = book_tex(('2e1410', '6e2a24', '86362e'))
    else:           # Q3 birch quiz desk: fletching table wood, enchanting table cloth (no gems), carved question mark
        B = ('6e5a36', '948056', 'b8a474', 'd0c08e', 'e4d8a8')
        planks_h(side, B, 0, 15, seams=(6, 12))
        for x in range(16):
            side.putpixel((x, 4), H(B[4]))
            side.putpixel((x, 5), H(B[1]))
            side.putpixel((x, 13), H(B[3]))
            side.putpixel((x, 15), H(B[0]))
        for y in range(6, 13):
            for x in range(1, 15):
                side.putpixel((x, y), H(B[2] if 2 <= x <= 13 and 7 <= y <= 11 else B[1]))
        stamp(side, 7, 7, ['##.', '..#', '.#.', '...', '.#.'], B[0])   # small carved question mark
        top_c = ('4e1416', '6e1e20', '8e2a2a', 'a83a36')
        planks_h(top, B, 0, 15)
        for y in range(2, 14):
            for x in range(2, 14):
                ring = x in (2, 13) or y in (2, 13)
                c = top_c[0] if ring else (top_c[2] if n41.hsh(x, y, 4) > 0.25 else top_c[1])
                if not ring and (x == 3 or y == 3):
                    c = top_c[3]
                top.putpixel((x, y), H(c))
        planks_h(bot, ('5a4a2c', '6e5a36', '948056', 'a89466'), 0, 15)
        gem = post_tex(B[3], B[1], B[4])
        book = book_tex(('2a2a4a', '3e4e7a', '52649a'))
    return {'side': side, 'top': top, 'bottom': bot, 'gem': gem, 'book': book}


def qtable_render(tex, size=128):
    model = json.loads((QMOD / 'models/block/questionary_table.json').read_text())
    els = []
    for e in model['elements']:
        faces = {}
        for name, f in e['faces'].items():
            faces[name] = (tex[f['texture'].lstrip('#')], tuple(f.get('uv', (0, 0, 16, 16))))
        els.append((tuple(e['from']), tuple(e['to']), faces))
    return n41.render(els, size=size, scale=0.62)


def qtable_current():
    t = QMOD / 'textures/block'
    return {k: n41.load(t / f'questionary_table_{k}.png') for k in ('side', 'top', 'bottom', 'gem', 'book')}


# ======================================================================= vaults
# chars: a-d frame stone (light..darkest), m-p cage metal (dark..light), '.' window (see-through), x interior,
# L/l/k light strip (state), E/e motif light (state), M mouth / glass (state)
MAT = {
    'astral': {'a': '8c7cae', 'b': '6e5e92', 'c': '4e406e', 'd': '2a2040', 'm': '120c1e', 'n': '24183a', 'o': '3c2c60',
               'p': '7a64b8', 'x': '0a0612',
               'on': {'L': 'f4e6ff', 'l': 'cfa8f4', 'k': '8a5ec8', 'E': 'ecd2ff', 'e': 'a878dc'},
               'off': {'L': '74648e', 'l': '5a4c74', 'k': '42355c', 'E': '7e6c9c', 'e': '4e4070'}},
    'nihil': {'a': '74928e', 'b': '567470', 'c': '3a5654', 'd': '1c3030', 'm': '081414', 'n': '102a2a', 'o': '1c4646',
              'p': '3e8a86', 'x': '040c0c',
              'on': {'L': 'eafcf8', 'l': 'b3dcdd', 'k': '5ec8c4', 'E': 'dafff8', 'e': '6ad0c8'},
              'off': {'L': '527270', 'l': '405e5c', 'k': '2e4a48', 'E': '5e827e', 'e': '3a5856'}},
}
MOTIF = {   # 6x6, '-' = keep the plate
    'astral': ['--EE--', '--ee--', 'EeMMeE', 'EeMMeE', '--ee--', '--EE--'],     # astral star
    'nihil': ['-EEEE-', 'Ee--eE', 'E-MM-E', 'E-MM-E', 'Ee--eE', '-EEEE-'],      # nihil ring (lamp motif)
}


def frame_rows():
    """Own stone frame bands (top rows 0-2, bottom rows 13-15) with a light strip in the bottom band."""
    top = ['aabaaabbaaabaaba', 'bbcbbbbcbbbbcbbb', 'ccdcccccccdccccc']
    bottom = ['abbckLLLLLLkcbba', 'bccckllllllkcccb', 'dddddcddddcddddd']
    return top, bottom


def put_motif(rows, mat, x0, y0):
    rows = [list(r) for r in rows]
    for dy, r in enumerate(MOTIF[mat]):
        for dx, ch in enumerate(r):
            if ch != '-':
                rows[y0 + dy][x0 + dx] = ch
    return [''.join(r) for r in rows]


def vault_faces(v, mat):
    """Returns dict face -> rows (16 strings). Faces: front, side, top, top_open, bottom."""
    ft, fb = frame_rows()
    if v == 0:      # A: closest to Vanilla - windowed cage around a centre plate with the motif as the 'face'
        cage = []
        for y in range(3, 13):
            bar = y in (3, 6, 9, 12)
            r = ''
            for x in range(16):
                if 4 <= x <= 11:
                    edge = x in (4, 11) or y in (3, 12)
                    r += ('p' if (x == 4 or y == 3) else 'n') if edge else 'o'
                elif x in (0, 15):
                    r += 'o'
                else:
                    r += 'n' if bar else '.'
            cage.append(r)
        front = put_motif(ft + cage + fb, mat, 5, 5)
        sc = []
        for y in range(3, 13):
            bar = y in (3, 6, 9, 12)
            sc.append(''.join('o' if x in (0, 15) else ('n' if bar or x in (5, 10) else '.') for x in range(16)))
        side = ft + sc + fb
        top = []
        for y in range(16):
            r = ''
            for x in range(16):
                ring = min(x, y, 15 - x, 15 - y)
                if ring == 0:
                    r += 'a' if (x + y) % 3 else 'b'
                elif ring == 1:
                    r += 'c'
                elif x in (7, 8) or y in (7, 8) or x == y or x == 15 - y:
                    r += 'o' if (x + y) % 2 else 'n'
                else:
                    r += '.'
            top.append(r)
        top = put_motif(top, mat, 5, 5)
    elif v == 1:    # B: porthole safe - closed metal front with a round glass window, slotted sides
        cage = []
        for y in range(3, 13):
            r = ''
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d < 3.2:
                    r += 'M'
                elif d < 4.4:
                    r += 'p' if x + y < 15 else 'n'
                elif (x, y) in ((1, 4), (14, 4), (1, 11), (14, 11)):
                    r += 'p'
                else:
                    r += 'o' if x not in (0, 15) else 'n'
            cage.append(r)
        front = ft + cage + fb
        front = [r[:7] + 'EE' + r[9:] if i in (7, 8) else r for i, r in enumerate(front)]   # motif spark in the glass
        sc = []
        for y in range(3, 13):
            r = ''
            for x in range(16):
                slot = x in (2, 3, 6, 7, 8, 9, 12, 13) and 5 <= y <= 10
                r += '.' if slot else ('n' if y in (3, 12) or x in (0, 15) else 'o')
            sc.append(r)
        side = ft + sc + fb
        top = []
        for y in range(16):
            r = ''
            for x in range(16):
                ring = min(x, y, 15 - x, 15 - y)
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if ring == 0:
                    r += 'a' if (x + y) % 3 else 'b'
                elif ring == 1:
                    r += 'c'
                elif d < 3.2:
                    r += 'M'
                elif d < 4.4:
                    r += 'p' if x + y < 15 else 'm'
                else:
                    r += 'o' if (x // 2 + y // 2) % 2 else 'n'
            top.append(r)
    else:           # C: barred cage - vertical bars all round (like iron bars), a lock plate with the motif
        cage = []
        for y in range(3, 13):
            r = ''
            for x in range(16):
                if 4 <= x <= 11 and 4 <= y <= 11:
                    r += 'p' if (x == 4 or y == 4) else ('n' if x == 11 or y == 11 else 'o')
                elif y in (3, 12):
                    r += 'n'
                else:
                    r += ('p' if y == 4 else 'o') if x % 3 == 0 else '.'
            cage.append(r)
        front = put_motif(ft + cage + fb, mat, 5, 5)
        sc = []
        for y in range(3, 13):
            sc.append(''.join('n' if y in (3, 12) else (('p' if y == 4 else 'o') if x % 3 == 0 else '.')
                              for x in range(16)))
        side = ft + sc + fb
        top = []
        for y in range(16):
            r = ''
            for x in range(16):
                ring = min(x, y, 15 - x, 15 - y)
                if ring == 0:
                    r += 'a' if (x + y) % 3 else 'b'
                elif ring == 1:
                    r += 'c'
                elif x % 3 == 0 or y % 3 == 0:
                    r += 'p' if (x % 3 == 0 and y % 3 == 0) else 'o'
                else:
                    r += '.'
            top.append(r)
        top = put_motif(top, mat, 5, 5)
    # opened top (ejecting): the centre turns into a dark shaft with a lit rim
    top_open = []
    for y, r in enumerate(top):
        top_open.append(''.join(('x' if 4 <= x <= 11 and 4 <= y <= 11 and not (x in (4, 11) or y in (4, 11))
                                 else ('l' if 4 <= x <= 11 and 4 <= y <= 11 else ch)) for x, ch in enumerate(r)))
    bottom = []
    for y in range(16):
        ring = [min(x, y, 15 - x, 15 - y) for x in range(16)]
        bottom.append(''.join('d' if k in (0, 3) else ('c' if k in (1, 4) else 'b') if k < 6 else 'c'
                              for k in ring))
    return {'front': front, 'side': side, 'top': top, 'top_open': top_open, 'bottom': bottom}


def paint(rows, mat, state, see_through=True):
    p = MAT[mat]
    lights = p['off'] if state == 'inactive' else p['on']
    pal = {k: p[k] for k in 'abcdmnopx'}
    pal.update(lights)
    pal['M'] = {'inactive': p['o'], 'active': p['x'], 'ejecting': lights['l']}[state]
    if not see_through:
        pal['.'] = p['x']
    return from_rows(rows, pal)


STATES = ('inactive', 'active', 'ejecting')


def vault_set(v, mat):
    """All textures of one proposal: name -> image (front per state, side off/on, top, top_open, bottom)."""
    f = vault_faces(v, mat)
    out = {}
    for s in STATES:
        out[f'front_{s}'] = paint(f['front'], mat, s)
    out['side_inactive'] = paint(f['side'], mat, 'inactive')
    out['side_active'] = paint(f['side'], mat, 'active')
    out['top'] = paint(f['top'], mat, 'active')
    out['top_ejecting'] = paint(f['top_open'], mat, 'ejecting')
    out['bottom'] = paint(f['bottom'], mat, 'inactive')
    return out, f


def vault_cube(f, mat, state, size=128):
    """Cube render; windows show the dark interior (Vanilla renders inner faces there)."""
    front = paint(f['front'], mat, state, False)
    side = paint(f['side'], mat, 'inactive' if state == 'inactive' else 'active', False)
    top = paint(f['top_open'] if state == 'ejecting' else f['top'], mat, state, False)
    full = (0, 0, 16, 16)
    return n41.render([((0, 0, 0), (16, 16, 16), {'up': (top, full), 'north': (front, full), 'west': (side, full)})],
                      size=size, scale=0.62)


# ======================================================================= page
def uri(im, scale=1):
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    b = io.BytesIO()
    im.save(b, 'PNG', optimize=True)
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def tex_cell(label, im):
    return (f'<figure class="t"><div class="sizes"><img class="px" src="{uri(im, 8)}" width="{im.width * 8}" '
            f'alt=""><span><img class="px" src="{uri(im)}" alt=""><img class="px" src="{uri(im, 2)}" alt=""></span>'
            f'</div><figcaption>{label}</figcaption></figure>')


def render_cell(label, im):
    return f'<figure class="r"><img class="px" src="{uri(im)}" alt=""><figcaption>{label}</figcaption></figure>'


REFS = [
    ('Minecraft Wiki: Vault (Zustände inactive/active/unlocking/ejecting, ominous)', 'https://minecraft.wiki/w/Vault'),
    ('Minecraft Wiki: Lectern', 'https://minecraft.wiki/w/Lectern'),
    ('Minecraft Wiki: Cartography Table', 'https://minecraft.wiki/w/Cartography_Table'),
    ('Minecraft Wiki: Fletching Table', 'https://minecraft.wiki/w/Fletching_Table'),
    ('Minecraft Wiki: Enchanting Table', 'https://minecraft.wiki/w/Enchanting_Table'),
    ('minecraft.net Snapshot 24w05a (Vault-Texturen eingeführt)',
     'https://www.minecraft.net/en-us/article/minecraft-snapshot-24w05a'),
    ('Faithful-Richtlinien (Formen/Farben wie Vanilla halten)',
     'https://docs.faithfulpack.net/pages/textures/cf32-texturing-guidelines'),
    ('Visible Vault (Vault ohne Gitter, zeigt die Fenster-Idee)', 'https://modrinth.com/mod/visible-vault'),
]

CSS = """
:root{--bg:#f6f5f2;--fg:#1d1c1a;--mut:#66625a;--card:#ffffff;--line:#dedad2;--chk:#8b8b8b;--acc:#6a4aa8}
@media (prefers-color-scheme:dark){:root:not([data-theme="light"]){--bg:#17161a;--fg:#ecebe8;--mut:#a7a39b;
--card:#222127;--line:#34323a;--chk:#5a5a5a;--acc:#b9a0f0}}
:root[data-theme="dark"]{--bg:#17161a;--fg:#ecebe8;--mut:#a7a39b;--card:#222127;--line:#34323a;--chk:#5a5a5a;
--acc:#b9a0f0}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif}
main{max-width:1180px;margin:0 auto;padding:16px}h1{font-size:1.5rem;margin:.4em 0}h2{margin:1.6em 0 .3em;
font-size:1.2rem;border-bottom:1px solid var(--line);padding-bottom:4px}h3{margin:1em 0 .3em;font-size:1rem;
color:var(--acc)}p,li{color:var(--mut)}a{color:var(--acc)}
.row{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end}
.card{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:10px;margin:10px 0}
figure{margin:0;text-align:center}figcaption{font-size:12px;color:var(--mut);margin-top:3px}
.px{image-rendering:pixelated;display:block;background:var(--chk)}
.sizes{display:flex;gap:6px;align-items:flex-end}.sizes span{display:flex;flex-direction:column;gap:4px}
.r img{width:128px;height:128px;background:transparent}
.shot{max-width:100%;image-rendering:pixelated}
@media (max-width:600px){.sizes>img.px{width:64px;height:auto}}
"""


def page(path):
    h = ['<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" '
         'content="width=device-width,initial-scale=1"><title>N46 Texturen</title>'
         f'<style>{CSS}</style></head><body><main>',
         '<h1>Nachtrag 46: Farbkasten, Fragentisch, Gewölbe</h1>',
         '<p>Größen je Textur: 8×, darunter 1× und 2×. Würfel/Modell als Inventar-Render. Eigene Pixel-Art aus '
         'Code (tools/textures/n46_proposals_2026_10_12.py), Vanilla nur als Referenz angesehen.</p>',
         '<h2>Web-Referenzen</h2><ul>' + ''.join(f'<li><a href="{u}">{t}</a></li>' for t, u in REFS) + '</ul>']
    if SHOT.exists():
        shot = n41.load(SHOT)
        h.append(f'<div class="card"><h3>Vorher: Screenshot Inventar</h3><img class="shot" src="{uri(shot, 2)}" '
                 f'alt="Screenshot"></div>')

    # 1 paint box
    h.append('<h2>1 · Farbkasten – Stufen im B4-Stil (eingebaut)</h2><p>Gleiche Form wie die Basis (B4: '
             'Lederkasten, Regenbogen-Fenster). Je Stufe Material-Rahmen, Ecken und Schloss: verstärkt = Eisen + '
             'Diamant-Schloss, Netherit = Netherit-Leder und -Beschläge, Enderit = Enderit-Leder, violette '
             'Beschläge und Sprenkel (wie die bisherigen Stufen und der Enderit-Rucksack).</p>')
    old = {}
    for n in pbox.B4_TIERS:
        try:
            data = subprocess.run(['git', '-C', str(ROOT), 'show',
                                   f'{BASE}:src/main/resources/assets/simplebuilding/textures/item/{n}.png'],
                                  capture_output=True, check=True).stdout
            old[n] = Image.open(io.BytesIO(data)).convert('RGBA')
        except subprocess.CalledProcessError:
            pass
    new_imgs = pbox.build()
    h.append('<div class="card"><h3>Neu</h3><div class="row">'
             + ''.join(tex_cell(n, im) for n, im in new_imgs.items()) + '</div></div>')
    h.append('<div class="card"><h3>Vorher</h3><div class="row">'
             + ''.join(tex_cell(n, im) for n, im in old.items()) + '</div></div>')

    # 2 questionary
    h.append('<h2>2 · Fragentisch – 3 vanillanähere Vorschläge</h2><p>Gleiches Modell (Sockel, Schrank, Platte, '
             'vier Pfosten, offenes Buch), nur Texturen: Holz und Buch, wenige Töne, kein Gold/Lapis/Edelstein. '
             'Q1 Eichenpult (Lectern/Bücherregal), Q2 Kartentisch (Cartography: Pergament mit Fragezeichen, '
             'Schublade), Q3 Birken-Fragepult (Fletching-Holz, rotes Tuch wie der Zaubertisch ohne Diamanten, '
             'geschnitztes Fragezeichen). Die Pfosten (bisher Edelsteine) werden Holzpfosten.</p>')
    cur = qtable_current()
    h.append('<div class="card"><h3>Vorher</h3><div class="row">' + render_cell('Render', qtable_render(cur))
             + ''.join(tex_cell(k, im) for k, im in cur.items()) + '</div></div>')
    names = ['Q1 · Eichenpult', 'Q2 · Kartentisch', 'Q3 · Birken-Fragepult']
    for v in range(3):
        t = qtable(v)
        for k, im in t.items():
            im.save(OUT / f'Q{v + 1}_questionary_table_{k}.png')
        h.append(f'<div class="card"><h3>{names[v]}</h3><div class="row">' + render_cell('Render', qtable_render(t))
                 + ''.join(tex_cell(k + (' (oben)' if k == 'top' else ' (Seiten)' if k == 'side' else ''), im)
                           for k, im in t.items()) + '</div></div>')

    # 3 vaults
    vnames = ['1 · Fenster-Käfig (am nächsten an Vanilla)', '2 · Bullaugen-Tresor', '3 · Gitterstäbe']
    for mat, letter, title in (('astral', 'A', 'Astral-Gewölbe'), ('nihil', 'N', 'Nihil-Gewölbe')):
        h.append(f'<h2>3 · {title} – 3 Vorschläge nach dem Vanilla-Vault</h2><p>Vanilla-Aufbau: Steinrahmen oben/'
                 'unten, Metallkäfig mit durchsichtigen Fenstern, Lichtleiste unten, Zustände inaktiv / aktiv '
                 '(Spieler nah oder Truhe offen) / auswerfen (Deckel-Schacht offen). Nur Material und Farben '
                 f'{"astral (violetter Stein, Lavendel-Licht, Astral-Raute)" if mat == "astral" else "nihil (petrol Stein, Türkis-Licht, Nihil-Ring)"}.'
                 '</p>')
        cur = n41.load(ROOT / f'mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/{mat}_vault.png')
        h.append(f'<div class="card"><h3>Vorher (Truhen-Textur 64×64)</h3><img class="px" src="{uri(cur, 3)}" '
                 'alt=""></div>')
        for v in range(3):
            imgs, f = vault_set(v, mat)
            for k, im in imgs.items():
                im.save(OUT / f'{letter}{v + 1}_{mat}_vault_{k}.png')
            h.append(f'<div class="card"><h3>{letter}{vnames[v]}</h3><div class="row">'
                     + ''.join(render_cell(s, vault_cube(f, mat, s)) for s in STATES) + '</div><div class="row">'
                     + ''.join(tex_cell(k, im) for k, im in imgs.items()) + '</div></div>')
    h.append('<h2>Hinweis Einbau</h2><p>Die Gewölbe sind heute Endertruhen-Varianten (Truhenmodell mit Deckel). '
             'Ein Vault-Look braucht ein Blockmodell (Würfel + innere Flächen wie Vanilla template_vault) und einen '
             'Zustand am Block (inaktiv/aktiv/auswerfen) statt der Deckel-Animation – das ist ein Code-Umbau nach '
             'der Wahl.</p></main></body></html>')
    path.write_text(''.join(h), encoding='utf-8')
    print(path, path.stat().st_size)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--html', type=Path)
    a = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    if a.html:
        a.html.parent.mkdir(parents=True, exist_ok=True)
        page(a.html)


if __name__ == '__main__':
    main()
