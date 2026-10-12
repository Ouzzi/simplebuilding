"""Nachtrag 46 round 2 (owner answer 2026-10-12). Proposals only.

  R1-R3  questionary table: like the Vanilla lectern, but close to the CURRENT table - lapis on top, an obsidian layer
         at the bottom (it is more special than Vanilla). Same model / UV layout as before.
  A1-A3  astral vault, N1-N3 nihil vault: they stay chests (chest model, no code change). 64x64 chest sheets in the
         layout and shading logic of the Vanilla ENDER CHEST (obsidian body, dotted lid rim, eye lock on the front),
         only in astral / nihil material; the existing motifs stay (astral diamond, nihil ring).

Own pixel art from code; the Vanilla ender chest / lectern were only looked at (structure, value steps), never copied.
Run: python3.12 tools/textures/n46_round2_2026_10_12.py [--html out.html]
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402
import n46_proposals_2026_10_12 as r1  # noqa: E402

ROOT = r1.ROOT
OUT = ROOT / 'docs/ai/previews/n46/r2'
H, hsh, mix = n41.H, n41.hsh, n41.mix

# ======================================================================= questionary table, round 2
OBS = ('0e0a14', '1a1324', '261c35', '35284a', '4a3966')          # the table's own obsidian (current generator)
LAPIS = ('142a6b', '1e3f9a', '2e5cc8', '5c8af0', '9cc0ff')
VELVET = ('1d2c64', '26397e', '2e4596')
OAK = ('4c3219', '6a4a26', '8a6436', 'a47c45', 'bc9458')            # lectern-like oak
DARK = ('2a1a0c', '3c2814', '503620', '64482a', '7a5c36')


def obsidian(im, x0, y0, x1, y1, seed=1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = hsh(x, y, seed)
            im.putpixel((x, y), H(OBS[0] if t < 0.3 else OBS[1] if t < 0.75 else OBS[2] if t < 0.93 else OBS[3]))


def velvet(im, x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            im.putpixel((x, y), H(VELVET[2] if hsh(x, y, 4) > 0.55 else VELVET[1]))


def lapis_tiles(im, x0, y0, x1, y1):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            t = hsh(x, y, 6)
            c = LAPIS[2] if t < 0.55 else LAPIS[1] if t < 0.85 else LAPIS[3]
            if (x - x0) % 6 == 5 or (y - y0) % 6 == 5:
                c = LAPIS[0]
            im.putpixel((x, y), H(c))


def lectern_panel(im, W, y0=6, y1=12):
    """Lectern-like cabinet: a raised frame panel (light top-left edge, dark bottom-right) in the given wood."""
    for y in range(y0, y1 + 1):
        for x in range(1, 15):
            if x in (1, 14) or y in (y0, y1):
                c = W[1]
            elif x == 2 or y == y0 + 1:
                c = W[4]
            elif x == 13 or y == y1 - 1:
                c = W[2]
            else:
                c = W[3] if (y + x // 4) % 3 else W[2]
            im.putpixel((x, y), H(c))


def qtable2(v):
    side, top, bot = r1.new(), r1.new(), r1.new()
    W = OAK if v != 1 else DARK
    r1.planks_h(side, W, 0, 3)
    obsidian(side, 0, 13, 15, 15)                      # obsidian layer at the bottom (plinth)
    for x in range(16):
        side.putpixel((x, 13), H(OBS[3] if x % 5 else OBS[4]))
    obsidian(bot, 0, 0, 15, 15, 3)
    if v == 0:      # R1 oak lectern cabinet, oak slab edge, lapis velvet top in an oak frame, lapis posts
        lectern_panel(side, W)
        for x in range(16):
            side.putpixel((x, 4), H(W[4]))
            side.putpixel((x, 5), H(W[1]))
        r1.planks_h(top, W, 0, 15)
        velvet(top, 2, 2, 13, 13)
        for i in range(2, 14):
            top.putpixel((i, 2), H(LAPIS[3]))
            top.putpixel((2, i), H(LAPIS[3]))
            top.putpixel((i, 13), H(VELVET[0]))
            top.putpixel((13, i), H(VELVET[0]))
        gem = r1.post_tex(LAPIS[2], LAPIS[1], LAPIS[4])
        book = r1.book_tex(('4c1418', '8e2b2b', 'a83a36'))
    elif v == 1:    # R2 dark oak cabinet with a lapis inlay window, obsidian slab edge, lapis tile top, obsidian posts
        lectern_panel(side, W)
        for y in range(8, 11):
            for x in range(6, 10):
                side.putpixel((x, y), H(LAPIS[3] if (x, y) == (6, 8) else LAPIS[2] if y < 10 else LAPIS[1]))
        for x in range(16):
            side.putpixel((x, 4), H(OBS[3]))
            side.putpixel((x, 5), H(OBS[1]))
        obsidian(top, 0, 0, 15, 15, 5)
        lapis_tiles(top, 1, 1, 14, 14)
        for i in range(1, 15):
            top.putpixel((i, 1), H(LAPIS[4]))
        gem = r1.post_tex(OBS[2], OBS[1], LAPIS[3])
        book = r1.book_tex(('1e2440', '2e3e6e', '3e5290'))
    else:           # R3 oak bookshelf cabinet (lectern books), lapis strip on the slab edge, velvet top with obsidian rim
        r1.planks_h(side, W, 6, 12)
        for y in range(6, 13):
            for x in range(1, 15):
                edge = x in (1, 14) or y in (6, 12)
                side.putpixel((x, y), H(W[1] if edge else '2c1c0e'))
        for i, col in enumerate(['2e5cc8', '8e2b2b', '6a4a26', '1e3f9a', '8e2b2b', '4e6a34', '2e5cc8', 'a47c45',
                                 '1e3f9a', '8e2b2b', '6a4a26', '2e5cc8']):
            h = 4 + int(hsh(i, 1, 9) * 2)
            for y in range(12 - h, 12):
                side.putpixel((2 + i, y), mix(H(col), H('ffffff'), 0.25) if y == 12 - h else H(col))
        for x in range(16):
            side.putpixel((x, 4), H(LAPIS[3]))
            side.putpixel((x, 5), H(LAPIS[1]))
        obsidian(top, 0, 0, 15, 15, 8)
        velvet(top, 2, 2, 13, 13)
        for i in range(2, 14):
            top.putpixel((i, 2), H(LAPIS[2]))
            top.putpixel((2, i), H(LAPIS[2]))
        gem = r1.post_tex(W[3], W[1], W[4])
        book = r1.book_tex(('4c1418', '8e2b2b', 'a83a36'))
    return {'side': side, 'top': top, 'bottom': bot, 'gem': gem, 'book': book}


# ======================================================================= ender-chest-style vault sheets
MAT = {   # obsidian ramp (dark -> light), accent ramp (dark -> light), motif colours (dark, mid, light)
    'astral': {'obs': ('08040f', '120a20', '1c1130', '281a42', '362458', '4a3276'),
               'acc': ('3c2470', '5a3a9c', '8460c8', 'b496ec'), 'mot': ('2a8c8c', '5ad8d0', 'c8fff6')},
    'nihil': {'obs': ('030c0c', '081818', '0f2424', '173232', '22463f', '30605a'),
              'acc': ('1c4a48', '2e6e6a', '4a9a94', '86cfc8'), 'mot': ('3e8e8a', 'b3dcdd', 'eafcf8')},
}
MOT5 = {'astral': ['..o..', '.oMo.', 'oMLMo', '.oMo.', '..o..'],       # astral diamond (existing concept)
        'nihil': ['.ooo.', 'oM.Mo', 'o.L.o', 'oM.Mo', '.ooo.']}        # nihil ring (existing concept)


def chest_sheet(v, mat):
    """64x64 chest texture. v0 Vanilla structure (dotted rim), v1 solid material rim (old concept),
    v2 starfield / void dust with the motif also on the lid."""
    p = MAT[mat]
    O, A, M = [H(c) for c in p['obs']], [H(c) for c in p['acc']], [H(c) for c in p['mot']]
    im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))

    def speck(x0, y0, x1, y1, seed, base=1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                t = hsh(x, y, seed)
                im.putpixel((x, y), O[base] if t < 0.55 else O[base + 1] if t < 0.85 else O[base + 2])

    def zigzag(x0, y0, x1, y1, seed):
        """Base side: obsidian with diagonal lighter streaks (Vanilla ender chest base logic)."""
        speck(x0, y0, x1, y1, seed, 0)
        for y in range(y0, y1):
            for x in range(x0, x1):
                if (x + 2 * y) % 7 == 0 and hsh(x, y, seed + 1) > 0.35:
                    im.putpixel((x, y), O[3] if v != 2 else (A[2] if hsh(x, y, 9) > 0.8 else O[3]))
                elif v == 2 and hsh(x, y, seed + 5) > 0.95:
                    im.putpixel((x, y), A[3])                 # star / void dust

    def rim_square(x0, y0, size, dotted):
        for i in range(size):
            for (x, y) in ((x0 + i, y0), (x0 + i, y0 + size - 1), (x0, y0 + i), (x0 + size - 1, y0 + i)):
                im.putpixel((x, y), O[0])
        for i in range(1, size - 1):
            for k, (x, y) in enumerate(((x0 + i, y0 + 1), (x0 + i, y0 + size - 2), (x0 + 1, y0 + i),
                                        (x0 + size - 2, y0 + i))):
                if dotted:
                    c = A[2] if i % 3 == 1 else (A[1] if i % 3 == 2 else O[4])
                else:
                    c = A[2] if k in (0, 2) else A[0]
                im.putpixel((x, y), c)

    def motif(cx, cy):
        for dy, row in enumerate(MOT5[mat]):
            for dx, ch in enumerate(row):
                c = {'o': M[1], 'M': M[0], 'L': M[2]}.get(ch)
                if c:
                    im.putpixel((cx - 2 + dx, cy - 2 + dy), c)

    # lid top (14,0) and underside (28,0), 14x14
    speck(15, 1, 27, 13, 11)
    rim_square(14, 0, 14, v != 1)
    if v == 2:
        motif(21, 7)
    speck(28, 0, 42, 14, 12, 1)
    rim_square(28, 0, 14, True)
    # lid sides rows 14-18 over 56 px
    for x in range(56):
        im.putpixel((x, 14), O[0])
        im.putpixel((x, 18), O[1])
    speck(0, 15, 56, 18, 13, 1)
    for x in range(56):
        if x % 14 in (0, 13):
            for y in range(15, 18):
                im.putpixel((x, y), O[0])
        elif v == 1:
            im.putpixel((x, 15), A[2] if x % 14 > 0 else O[0])
            im.putpixel((x, 17), A[0])
        elif x % 3 == 1:
            im.putpixel((x, 16), A[2])
            im.putpixel((x, 15), O[4])
    # base top (inner floor, 14,19) and bottom (28,19)
    speck(14, 19, 28, 33, 14, 0)
    for y in range(19, 33):
        for x in range(14, 28):
            if (x - 14) % 4 == 1 or (y - 19) % 4 == 1:
                im.putpixel((x, y), O[2])
    speck(28, 19, 42, 33, 15, 0)
    # base sides rows 33-42
    for x in range(56):
        im.putpixel((x, 33), O[0])
        im.putpixel((x, 42), O[0])
    zigzag(0, 34, 56, 42, 16)
    for x in range(56):
        if x % 14 in (0, 13):
            for y in range(34, 42):
                im.putpixel((x, y), O[0])
        elif v == 1:
            im.putpixel((x, 34), A[2] if x % 14 not in (0, 13) else O[0])
            im.putpixel((x, 41), A[0])
    # front panel (u 42-56): eye socket with the motif as the eye (the Vanilla ender eye spot)
    for y in range(35, 41):
        for x in range(45, 53):
            d = ((x - 48.5) / 3.6) ** 2 + ((y - 37.5) / 2.9) ** 2
            if d <= 1:
                im.putpixel((x, y), O[0])
    motif(49, 38)
    # lock (0,0): material knob
    for y in range(0, 5):
        for x in range(0, 6):
            if y == 0 and x in (0, 5):
                continue
            im.putpixel((x, y), A[3] if y == 0 else (A[1] if x in (1, 2) else A[0]))
    im.putpixel((1, 2), M[1])
    return im


# chest geometry in pixels (lid 14x5x14 at y 9, base 14x10x14), front = the eye panel side
def box_faces(sheet, u, v, w, h, d):
    def crop(x0, y0, x1, y1):
        return sheet.crop((x0, y0, x1, y1)).resize((16, 16), Image.NEAREST)
    full = (0, 0, 16, 16)
    return {'up': (crop(u + d, v, u + d + w, v + d), full),
            'north': (crop(u + d + w + d, v + d, u + d + w + d + w, v + d + h), full),
            'west': (crop(u, v + d, u + d, v + d + h), full)}


def chest_render(sheet, size=128):
    lid = box_faces(sheet, 0, 0, 14, 5, 14)
    base = box_faces(sheet, 0, 19, 14, 10, 14)
    lid['up'] = (lid['up'][0].transpose(Image.FLIP_TOP_BOTTOM), lid['up'][1])
    lock = box_faces(sheet, 0, 0, 2, 4, 1)
    els = [((1, 0, 1), (15, 10, 15), base), ((1, 9, 1), (15, 14, 15), lid), ((7, 7, 0), (9, 11, 1), lock)]
    return n41.render(els, size=size, scale=0.62)


# ======================================================================= page
def page(path):
    OUT.mkdir(parents=True, exist_ok=True)
    u, tc, rc = r1.uri, r1.tex_cell, r1.render_cell
    h = ['<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" '
         'content="width=device-width,initial-scale=1"><title>N46 Runde 2</title>'
         f'<style>{r1.CSS}</style></head><body><main>',
         '<h1>Nachtrag 46 · Runde 2: Fragentisch und Gewölbe</h1>',
         '<p>Größen: 8×, darunter 1× und 2×. Render wie im Inventar. Eigene Pixel-Art aus Code '
         '(tools/textures/n46_round2_2026_10_12.py); Vanilla-Lesepult und -Endertruhe nur als Vorlage für Aufbau und '
         'Schattierung angesehen.</p><h2>Web-Referenzen</h2><ul>'
         + ''.join(f'<li><a href="{x}">{t}</a></li>' for t, x in [
             ('Minecraft Wiki: Lectern', 'https://minecraft.wiki/w/Lectern'),
             ('Minecraft Wiki: Ender Chest (Obsidian-Korpus, Ender-Auge)', 'https://minecraft.wiki/w/Ender_Chest'),
             ('Minecraft Wiki: Chest (Texturaufteilung 64×64)', 'https://minecraft.wiki/w/Chest'),
             ('Minecraft Wiki: Block of Lapis Lazuli', 'https://minecraft.wiki/w/Block_of_Lapis_Lazuli'),
             ('Minecraft Wiki: Obsidian', 'https://minecraft.wiki/w/Obsidian'),
             ('Faithful-Richtlinien (Formen/Farben wie Vanilla halten)',
              'https://docs.faithfulpack.net/pages/textures/cf32-texturing-guidelines')]) + '</ul>']
    h.append('<h2>Fragentisch – 3 Vorschläge (Lesepult-Art, oben Lapis, unten Obsidian)</h2><p>Bestehendes Modell. '
             'R1 Eichen-Lesepult mit Lapis-Samt und Lapis-Pfosten, R2 Dunkeleiche mit Lapis-Fenster, Obsidian-Kante, '
             'Lapis-Fliesen oben, R3 Eichen-Bücherschrank, Lapis-Kante, Samt mit Obsidian-Rand. Unten immer eine '
             'Obsidian-Schicht.</p>')
    cur = r1.qtable_current()
    h.append('<div class="card"><h3>Vorher</h3><div class="row">' + rc('Render', r1.qtable_render(cur))
             + ''.join(tc(k, im) for k, im in cur.items()) + '</div></div>')
    names = ['R1 · Eichen-Lesepult', 'R2 · Dunkeleiche mit Lapis-Fenster', 'R3 · Bücherschrank']
    for v in range(3):
        t = qtable2(v)
        for k, im in t.items():
            im.save(OUT / f'R{v + 1}_questionary_table_{k}.png')
        h.append(f'<div class="card"><h3>{names[v]}</h3><div class="row">' + rc('Render', r1.qtable_render(t))
                 + ''.join(tc(k, im) for k, im in t.items()) + '</div></div>')
    vn = ['1 · Vanilla-Aufbau (gepunkteter Deckelrand)', '2 · Material-Rahmen (bisheriges Konzept)',
          '3 · Sternenstaub/Leere, Motiv auch auf dem Deckel']
    for mat, L, title, note in (('astral', 'A', 'Astral-Gewölbe', 'violetter Obsidian, Astral-Raute als Auge'),
                                ('nihil', 'N', 'Nihil-Gewölbe', 'petrolfarbener Obsidian, Nihil-Ring als Auge')):
        h.append(f'<h2>{title} – 3 Vorschläge nach der Endertruhe</h2><p>Bleibt Truhe (Truhenmodell, kein '
                 f'Code-Umbau). {note}. 3D-Render, Item-Größe (32 px/16 px) und Truhenbild 64×64.</p>')
        cur = n41.load(ROOT / f'mc26_3/overlay/resources/assets/simplebuilding/textures/entity/chest/{mat}_vault.png')
        h.append('<div class="card"><h3>Vorher</h3><div class="row">' + rc('Render', chest_render(cur))
                 + f'<figure><img class="px" src="{u(cur, 4)}" alt=""><figcaption>Truhenbild 4×</figcaption>'
                 '</figure></div></div>')
        for v in range(3):
            s = chest_sheet(v, mat)
            s.save(OUT / f'{L}{v + 1}_{mat}_vault_chest.png')
            r = chest_render(s)
            items = (f'<figure><span class="sizes"><img class="px" src="{u(chest_render(s, 32))}" alt="">'
                     f'<img class="px" src="{u(chest_render(s, 16))}" alt=""></span><figcaption>Item 32/16</figcaption>'
                     '</figure>')
            h.append(f'<div class="card"><h3>{L}{vn[v]}</h3><div class="row">' + rc('3D-Truhe', r) + items
                     + f'<figure><img class="px" src="{u(s, 4)}" alt=""><figcaption>Truhenbild 4×</figcaption>'
                     '</figure></div></div>')
    h.append('</main></body></html>')
    path.write_text(''.join(h), encoding='utf-8')
    print(path, path.stat().st_size)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--html', type=Path)
    a = ap.parse_args()
    if a.html:
        page(a.html)
