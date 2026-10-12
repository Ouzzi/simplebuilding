"""Nachtrag 44 round 2 (owner 2026-10-12): refinements of the picks from round 1. Proposals only.

Lamps: A1 (Astral) and A4 (Nihil) as base, three refinements each - even meander, calmer contrast, a clean
copper-bulb-like frame (light top/left rim, dark bottom/right rim, 2 px centre cross, four recessed cells).
Wood: B1 Aschholz and B3 Daemmerholz (a little browner), closer to the Vanilla construction (four 4 px boards with
seam row and staggered joints, vertical bark strips with cracks, square rings on the log top, framed doors and
trapdoors with bevelled panels), two variants each, full set as in round 1 incl. tree sketches.

Run: python3.12 tools/textures/n44_round2_2026_10_12.py [--html out.html]
Writes PNGs to docs/ai/previews/n44/r2/. Own pixel art drawn from code; Vanilla textures were only looked at.
"""
from __future__ import annotations

import html
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_page  # noqa: E402
import n44_proposals_2026_10_12 as r1  # noqa: E402
from n41_proposals_2026_10_12 import H, cube, hsh, mix, new, put, ramp, rect  # noqa: E402

ROOT = r1.ROOT
OUT = ROOT / 'docs/ai/previews/n44/r2'
CLEAR = (0, 0, 0, 0)

# ------------------------------------------------------------------ A. lamps

RIM_HI, RIM_LO, FR, FR_LO, CELL, LN, CORE = range(7)


def rot(g):
    n = len(g)
    return [[g[n - 1 - x][y] for x in range(n)] for y in range(n)]


def rotn(g, k):
    for _ in range(k):
        g = rot(g)
    return g


def frame_map(cross=True):
    """Copper-bulb frame: light rim top/left, dark rim bottom/right, optional 2 px centre cross; rest = cell."""
    m = [[CELL] * 16 for _ in range(16)]
    for i in range(16):
        m[0][i] = RIM_HI
        m[i][0] = RIM_HI
        m[15][i] = RIM_LO
        m[i][15] = RIM_LO
    if cross:
        for i in range(1, 15):
            m[7][i] = FR
            m[8][i] = FR_LO
            m[i][7] = FR
            m[i][8] = FR_LO
    return m


def put_grid(m, g, ox, oy, val=LN):
    for y, row in enumerate(g):
        for x, v in enumerate(row):
            if v:
                m[oy + y][ox + x] = val


def lamp_a(top):
    """Refinement 1: copper-bulb grid, four identical 6x6 meander spirals turned by 90 deg (4-fold symmetry)."""
    m = frame_map()
    if top:
        m = frame_map(False)
        put_grid(m, r1.spiral(12), 2, 2)
        m[7][7] = m[7][8] = m[8][7] = m[8][8] = CORE
        return m
    g = r1.spiral(6)
    for k, (ox, oy) in enumerate(((1, 1), (9, 1), (9, 9), (1, 9))):
        put_grid(m, rotn(g, k), ox, oy)
    return m


def lamp_b(top):
    """Refinement 2: 1 px inset rim inside each cell, 4x4 spiral hooks - more air, quieter."""
    m = frame_map()
    g = r1.spiral(4)
    for k, (ox, oy) in enumerate(((1, 1), (9, 1), (9, 9), (1, 9))):
        for i in range(6):          # inset rim
            m[oy][ox + i] = FR if m[oy][ox + i] == CELL else m[oy][ox + i]
            m[oy + i][ox] = FR if m[oy + i][ox] == CELL else m[oy + i][ox]
        put_grid(m, rotn(g, k), ox + 1, oy + 1)
    if top:
        for k, (ox, oy) in enumerate(((1, 1), (9, 1), (9, 9), (1, 9))):
            put_grid(m, rotn(g, (k + 1) % 4), ox + 1, oy + 1, CELL)
            put_grid(m, rotn(g, k), ox + 1, oy + 1)
        m[7][7] = m[7][8] = m[8][7] = m[8][8] = CORE
    return m


def lamp_c(top):
    """Refinement 3: the round-1 look (big 7x7 hooks, no cross) but the hooks turned 4-fold and a calm frame."""
    m = frame_map(False)
    hook = r1.key7()
    for k, (ox, oy) in enumerate(((1, 1), (8, 1), (8, 8), (1, 8))):
        put_grid(m, rotn(hook, (k + 2) % 4), ox, oy)
    if top:
        m = frame_map(False)
        put_grid(m, r1.spiral(14), 1, 1)
        m[7][7] = m[7][8] = m[8][7] = m[8][8] = CORE
    return m


def colour_lamp(m, line, lit):
    r, f = r1.LINE[line], r1.FRAME[line]
    glow = H('fff4fa') if line == 'astral' else H('effffd')
    im = new()
    for y in range(16):
        for x in range(16):
            k = m[y][x]
            if k == RIM_HI:
                c = f[4]
            elif k == RIM_LO:
                c = f[1]
            elif k == FR:
                c = f[3]
            elif k == FR_LO:
                c = f[2]
            elif k == CELL:
                c = mix(r[3], r[4], 0.35) if lit else r[1]
                if not lit and (x + y) % 7 == 0:
                    c = mix(r[1], r[2], 0.5)
            elif k == LN:
                c = glow if lit else mix(f[3], r[2], 0.4)
            else:
                c = H('ffffff') if lit else r[2]
            if lit and k in (RIM_HI, FR, FR_LO):
                c = mix(c, r[4], 0.15)
            put(im, x, y, tuple(c[:3]) + (255,))
    return im


LAMPS = [('Kupferlampen-Raster, 4 gleiche Spiralen', lamp_a), ('Raster mit Innenrand, kleine Haken', lamp_b),
         ('A1 bereinigt: große Haken, 4-fach gedreht', lamp_c)]

# ------------------------------------------------------------------ B. wood

SETS = [
    dict(key='b1', base='B1', name='Aschholz', seed=11,
         bark=ramp('2b2724', '3f3934', '554e47', '6d655d', '867d74', 'a0968c'),
         wood=ramp('6e665d', '857c72', '9b9286', 'aea599', 'c1b9ad', 'd4cdc2'),
         accent=ramp('5e4a66', '86709a', 'b9a3cc', 'e6d8f0'), tint=H('a49a8a')),
    dict(key='b3', base='B3', name='Dämmerholz (etwas brauner)', seed=37,
         bark=ramp('2b211b', '3f3128', '574536', '6f5a47', '8a735d', 'a38b73'),
         wood=ramp('6f5f50', '877461', '9d8873', 'b09b85', 'c2ad97', 'd3c0aa'),
         accent=ramp('5a3f55', '7f5a78', 'a9849f', 'd6b9cd'), tint=H('9a8670')),
]


def board_planks(s, var):
    """Vanilla construction: four 4 px boards, darkest seam row at the bottom of each board, one staggered joint per
    board (dark line + light edge), grain as short horizontal dashes."""
    w, sd = s['wood'], s['seed'] + var
    joints = (12, 4, 9, 1) if var == 0 else (6, 13, 2, 10)
    im = new()
    for b in range(4):
        for yy in range(4):
            y = b * 4 + yy
            for x in range(16):
                c = w[3]
                if yy == 0:
                    c = w[4] if hsh(x, y, sd) < 0.45 else w[3]
                elif yy == 3:
                    c = w[1]
                else:
                    seg = int(hsh(x // 3 + b * 7, y, sd) * 10)
                    c = w[2] if seg < 3 else w[4] if seg > 7 else w[3]
                if x == joints[b] and yy < 3:
                    c = w[1]
                if x == (joints[b] + 1) % 16 and yy < 3:
                    c = w[4]
                put(im, x, y, c)
    if var == 1:   # End detail: small pale pegs at the joints (like end-stone pores, but placed)
        for b in range(4):
            put(im, (joints[b] + 2) % 16, b * 4 + 1, s['accent'][2])
    return im


def bark_side(s, var):
    """Vertical bark strips 1-2 px wide in four tones, long runs, dark cracks; variant 2 adds chorus knobs/rings."""
    b, sd = s['bark'], s['seed'] + var * 5
    im = new()
    widths, x = [], 0
    while x < 16:
        wdt = 1 + int(hsh(x, 0, sd) * 2)
        widths.append((x, wdt))
        x += wdt
    for i, (x0, wdt) in enumerate(widths):
        tone = 2 + int(hsh(i, 3, sd) * 3)
        crack = hsh(i, 5, sd) < 0.28
        for x in range(x0, min(16, x0 + wdt)):
            for y in range(16):
                run = int(hsh(i, (y + int(hsh(i, 9, sd) * 16)) // 5, sd + 1) * 3) - 1
                t = max(2, min(5, tone + run))
                c = b[t]
                if crack and (y + i * 3) % 16 < 11:
                    c = b[1]
                put(im, x, y, c)
    if var == 1:
        if s['key'] == 'b1':     # two chorus knobs
            for kx, ky in ((4, 4), (11, 11)):
                put(im, kx, ky, b[4])
                put(im, kx + 1, ky, b[5])
                put(im, kx, ky + 1, b[3])
                put(im, kx + 1, ky + 1, b[1])
        else:                    # faint twist: diagonal light flecks
            for i in range(4):
                put(im, (i * 4 + 2) % 16, (i * 4 + 1) % 16, b[5])
                put(im, (i * 4 + 3) % 16, (i * 4 + 1) % 16, b[4])
    return im


def log_top(s, var, stripped=False):
    """Square rings like Vanilla: bark rim, light inner rim, rings alternating every 2 px, rounded corners."""
    w, b, a = s['wood'], s['bark'], s['accent']
    im = new()
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            corner = (x in (1, 14) and y in (1, 14))
            if d == 0:
                c = (w[2] if stripped else (b[2] if hsh(x, y, 4) < 0.6 else b[1]))
            elif d == 1:
                c = w[1] if corner and not stripped else w[4]
            elif d in (2, 3, 6):
                c = w[3]
            elif d in (4, 5):
                c = w[2]
            else:
                c = w[2] if not var else a[1] if (x + y) % 2 else a[2]
            put(im, x, y, c)
    return im


def stripped_side(s, var):
    w, sd = s['wood'], s['seed'] + var + 40
    im = new()
    for x in range(16):
        tone = 2 + int(hsh(x, 1, sd) * 3)
        for y in range(16):
            run = int(hsh(x, (y + x * 3) // 6, sd) * 3) - 1
            put(im, x, y, w[max(2, min(5, tone + run))])
    if var == 1:
        for x in range(16):
            if x % 5 == 2:
                for y in range(16):
                    if (y + x) % 8 < 5:
                        put(im, x, y, w[2])
    return im


def bevel(im, x0, y0, x1, y1, w, hole=False):
    if hole:
        rect(im, x0, y0, x1, y1, CLEAR)
        return
    rect(im, x0, y0, x1, y1, w[3])
    rect(im, x0, y0, x1, y0, w[1])
    rect(im, x0, y0, x0, y1, w[1])
    rect(im, x0, y1, x1, y1, w[4])
    rect(im, x1, y0, x1, y1, w[4])


def frame16(s, top_edge=True, bottom_edge=True):
    w = s['wood']
    im = board_planks_vertical(s)
    for i in range(16):
        put(im, 0, i, w[1])
        put(im, 15, i, w[1])
        put(im, 1, i, w[4])
        put(im, 14, i, w[2])
        if top_edge:
            put(im, i, 0, w[1])
            put(im, i, 1, w[4]) if 0 < i < 15 else None
        if bottom_edge:
            put(im, i, 15, w[1])
            put(im, i, 14, w[2]) if 0 < i < 15 else None
    return im


def board_planks_vertical(s):
    w, sd = s['wood'], s['seed'] + 3
    im = new()
    for x in range(16):
        for y in range(16):
            seg = int(hsh(x, y // 3, sd) * 10)
            put(im, x, y, w[2] if seg < 3 else w[4] if seg > 7 else w[3])
    return im


def door(s, var, half):
    w, a = s['wood'], s['accent']
    if half == 'top':
        im = frame16(s, True, False)
        if var == 0:    # 2x2 panes like Vanilla, mullion cross
            for x0, y0 in ((3, 3), (9, 3), (3, 9), (9, 9)):
                bevel(im, x0, y0, x0 + 3, y0 + 3, w, hole=True)
        else:           # one chorus-flower window: rounded square with a 4-petal mullion
            rect(im, 3, 3, 12, 12, CLEAR)
            for p in ((3, 3), (12, 3), (3, 12), (12, 12)):
                put(im, p[0], p[1], w[3])
            rect(im, 7, 3, 8, 12, w[3])
            rect(im, 3, 7, 12, 8, w[3])
            rect(im, 6, 6, 9, 9, w[4])
            rect(im, 7, 7, 8, 8, a[2])
        put(im, 0, 4, H('6b6b70'))   # hinges
        put(im, 0, 11, H('6b6b70'))
    else:
        im = frame16(s, False, True)
        if var == 0:
            bevel(im, 3, 2, 7, 12, w)
            bevel(im, 9, 2, 12, 12, w)
        else:
            for x0, y0 in ((3, 2), (9, 2), (3, 8), (9, 8)):
                bevel(im, x0, y0, x0 + 3, y0 + 4, w)
        rect(im, 12, 0, 13, 0, H('5a5a60'))   # handle
        put(im, 13, 1, H('8a8a90'))
        put(im, 0, 4, H('6b6b70'))
        put(im, 0, 11, H('6b6b70'))
    return im


def trapdoor(s, var):
    w = s['wood']
    im = frame16(s)
    if var == 0:
        for x0, y0 in ((3, 3), (9, 3), (3, 9), (9, 9)):
            rect(im, x0, y0, x0 + 3, y0 + 3, CLEAR)
    else:
        rect(im, 3, 3, 12, 5, CLEAR)
        rect(im, 3, 10, 12, 12, CLEAR)
        rect(im, 3, 7, 12, 8, w[4])
        put(im, 7, 7, s['accent'][2])
        put(im, 8, 8, s['accent'][1])
    return im


def leaves(s, var):
    """Vanilla-like: grey clumps of 3-4 px with a light top-left and darker bottom-right, ~10 % holes, tinted."""
    tint, sd = s['tint'], s['seed'] + 60 + var
    im = new()
    for y in range(16):
        for x in range(16):
            ox, oy = (y // 4) % 2 * 2, 0
            cx, cy = (x + ox) // 4, (y + oy) // 4
            lx, ly = (x + ox) % 4, (y + oy) % 4
            if hsh(x, y, sd + 1) < 0.1:
                continue
            g = 0.72 + 0.16 * hsh(cx, cy, sd)
            if lx + ly <= 1:
                g += 0.18
            elif lx + ly >= 5:
                g -= 0.2
            g = max(0.45, min(1.0, g + 0.06 * (hsh(x, y, sd + 3) - 0.5)))
            put(im, x, y, tuple(int(c * g) for c in tint[:3]) + (255,))
    if var == 1:
        for x, y in ((4, 4), (11, 9), (6, 13)):
            put(im, x, y, s['accent'][3])
    return im


def sapling(s, var):
    """Bud on a short knobby stem with two small tinted leaves (Vanilla sapling silhouette)."""
    b, a, tint = s['bark'], s['accent'], s['tint']
    im = new()
    for y in range(8, 16):
        put(im, 7, y, b[3] if y % 3 else b[4])
    put(im, 8, 12, b[2])
    leaf = [tuple(int(c * g) for c in tint[:3]) + (255,) for g in (0.6, 0.8, 1.0)]
    for x, y, t in ((4, 11, 1), (5, 11, 2), (5, 12, 0), (6, 11, 1), (9, 13, 1), (10, 13, 2), (10, 14, 0), (8, 13, 1)):
        put(im, x, y, leaf[t])
    pet = [(7, 3), (6, 4), (7, 4), (8, 4), (5, 5), (6, 5), (7, 5), (8, 5), (9, 5), (6, 6), (7, 6), (8, 6), (7, 7)]
    if var == 1:
        pet += [(4, 5), (10, 5), (7, 2)]
    for x, y in pet:
        c = a[3] if (x + y) % 2 == 0 else a[2]
        if y >= 6:
            c = a[1]
        put(im, x, y, c)
    put(im, 7, 5, H('ffffff') if var else a[3])
    return im


def door_item(top, bot):
    return r1.door_item(top, bot)


def card(label, figs):
    return f'<div class="card"><div class="lab">{label}</div><div class="faces">{"".join(figs)}</div></div>'


def save(im, name):
    OUT.mkdir(parents=True, exist_ok=True)
    im.save(OUT / f'{name}.png')


def iso_fig(im, cap):
    return (f'<figure><div class="slot iso"><img src="{n41_page.uri(im)}" alt="Render"></div>'
            f'<figcaption>{html.escape(cap)}</figcaption></figure>')


REFS_A = [
    ('Vanilla copper_bulb', 'Rahmen: heller Rand oben/links, dunkler Rand unten/rechts, 2-px-Mittelkreuz, vier vertiefte '
     'Felder mit Haken. Übernommen als Raster in A1.1/A1.2 (A4.1/A4.2).'),
    ('Besitzer-Wahl Runde 1', 'A1 (Astral) und A4 (Nihil) als Basis; Ziel: Mäander gleichmäßig (4-fach gedreht statt '
     'gespiegelt), ruhigerer Kontrast (kein Pixelrauschen im Rahmen, nur 2 Rahmentöne), sauberer Rahmen.'),
]
REFS_B = [
    ('Vanilla oak/spruce/dark_oak_planks', 'Aufbau übernommen: vier 4-px-Bretter, dunkelste Fugenzeile unten je Brett, '
     'je Brett ein versetzter Stoß (dunkle Linie + helle Kante), Maserung als kurze waagerechte Striche.'),
    ('Vanilla oak_log / log_top / stripped', 'Rinde als senkrechte 1–2-px-Streifen in vier Tönen mit Rissen; Stirnseite '
     'mit Rindenrand, hellem Innenrand und eckigen Ringen alle 2 px; entrindet = ruhige Längsstreifen.'),
    ('Vanilla oak_door / trapdoor', 'Türrahmen 2 px, Fenster 2×2 bzw. Kassetten mit Fase (Licht oben links), '
     'Scharniere links, Griff rechts; Falltür mit vier Öffnungen.'),
    ('Faithful 32x', 'Polierte Vanilla-Nähe: dieselben Bauteile, weniger Rauschen, klare Fugen.'),
    ('Vanilla Tweaks / Prettier Planks / Nullified Logs', 'Bereinigte Brettfugen ohne harte Gitterlinien, rundere '
     'Stämme; übernommen: Fugen nur in einer Zeile, Rinde in langen Bahnen.'),
    ('Biomes O\' Plenty / Regions Unexplored', 'Viele eigene Holzsets mit Vanilla-Bauplan und nur einem Kennzeichen je Holz '
     '(Farbe, Fensterform). Übernommen: Grundaufbau Vanilla, End-Merkmal nur in Variante 2 (Pflöcke, Chorus-Knubbel, '
     'violettes Kernholz, Chorus-Fenster).'),
    ('Quark (Vertical Planks, Hölzer)', 'Zusatzhölzer im Vanilla-Raster; Variante 2 der Bretter bleibt daher im Raster.'),
]
SOURCES = r1.SOURCES + [
    ('Faithful 32x (CurseForge)', 'https://www.curseforge.com/minecraft/texture-packs/faithful-32x'),
    ('Vanilla Tweaks', 'https://vanillatweaks.net/picker/resource-packs/'),
    ('Prettier Planks', 'https://metamods.net/en/resourcepacks/prettier-planks'),
    ('Nullified Logs', 'https://www.curseforge.com/minecraft/texture-packs/nullified-logs'),
    ('Better Botanicals: Lumber & Woodstuffs', 'https://www.curseforge.com/minecraft/texture-packs/better-botanicals-lumber-woodstuffs'),
    ('Biomes O\' Plenty (Hölzer)', 'https://pyrotech.readthedocs.io/en/latest/scripts/wood/'),
    ('Regions Unexplored', 'https://www.curseforge.com/minecraft/mc-mods/regions-unexplored/files/7736799'),
    ('Regions Unexplored (Guide)', 'https://www.minecraft-guides.com/mod/regions-unexplored/'),
    ('Vertical Planks (Quark-Idee)', 'https://www.modpackindex.com/mod/104321/vertical-planks'),
]


def main():
    n41_page.SHOW_1X[0] = True
    out = ['<!doctype html><html lang="de"><head><meta charset="utf-8">',
           '<meta name="viewport" content="width=device-width,initial-scale=1">',
           '<title>N44 Runde 2</title>', f'<style>{n41_page.CSS}</style></head><body><main>',
           '<h1>N44 Runde 2</h1>',
           '<p class="lead">Nur Vorschläge, nichts ist im Spiel. 16×16 in 1×, 2× und 8× auf Inventar-Slot. '
           'Gestrichelt = Wahl aus Runde 1. Zum Auswählen die Nummern nennen (z. B. „A1.2, A4.2, B1.1, B3.2“).</p>']
    out.append('<h2>A · Lampen (Basis A1 / A4)</h2><div class="refs"><b>Referenzen &amp; was übernommen wurde</b><ul>')
    out += [f'<li><b>{html.escape(a)}:</b> {html.escape(b)}</li>' for a, b in REFS_A]
    out.append('</ul></div>')
    for line, base, block, title in (('astral', 'A1', 'astralit', 'Astral-Lampe'), ('nihil', 'A4', 'nihilith', 'Nihilith-Lampe')):
        out.append(f'<h3>{title}</h3><div class="row">')
        old = [n41_page.slot(r1.colour_lamp(r1.lamp_v1(t), line, lit, (9 if t else 5)), f'{"oben" if t else "Seite"} {"an" if lit else "aus"}')
               for lit in (False, True) for t in (False, True)]
        out.append(f'<div class="card before"><div class="lab">{base} (Runde 1)</div><div class="faces">{"".join(old)}</div></div>')
        for i, (lname, fn) in enumerate(LAMPS, 1):
            lab = f'{base}.{i}'
            figs, tex = [], {}
            for lit in (False, True):
                st = 'an' if lit else 'aus'
                side, top = colour_lamp(fn(False), line, lit), colour_lamp(fn(True), line, lit)
                tex[lit] = (side, top)
                save(side, f'{lab}_{block}_lamp_side_{"on" if lit else "off"}')
                save(top, f'{lab}_{block}_lamp_top_{"on" if lit else "off"}')
                figs += [n41_page.slot(side, f'Seite {st}'), n41_page.slot(top, f'oben {st}')]
            for lit in (False, True):
                rr = cube(tex[lit][1], tex[lit][0])
                save(rr, f'{lab}_{block}_lamp_render_{"on" if lit else "off"}')
                figs.append(iso_fig(rr, f'Würfel {"an" if lit else "aus"}'))
            out.append(card(f'{lab} · {html.escape(lname)}', figs))
        out.append('</div>')
    out.append('<h2>B · End-Holz (Basis B1 / B3)</h2><div class="refs"><b>Referenzen &amp; was übernommen wurde</b><ul>')
    out += [f'<li><b>{html.escape(a)}:</b> {html.escape(b)}</li>' for a, b in REFS_B]
    out.append('</ul></div>')
    for s in SETS:
        out.append(f'<h3>{s["base"]} · {html.escape(s["name"])}</h3><div class="row">')
        for var in (0, 1):
            lab = f'{s["base"]}.{var + 1}'
            t = {'Stamm Seite': bark_side(s, var), 'Stamm oben': log_top(s, var),
                 'entrindet Seite': stripped_side(s, var), 'entrindet oben': log_top(s, var, True),
                 'Bretter': board_planks(s, var), 'Tür oben': door(s, var, 'top'), 'Tür unten': door(s, var, 'bottom')}
            t['Tür Item'] = door_item(t['Tür oben'], t['Tür unten'])
            t['Falltür'] = trapdoor(s, var)
            t['Blätter'] = leaves(s, var)
            t['Knospe / Setzling'] = sapling(s, var)
            t['Gestrüpp-Stöcke Seite'] = r1.sticks_side(s)
            t['Gestrüpp-Stöcke oben'] = r1.sticks_top(s)
            t['Stock Item'] = r1.stick_item(s)
            figs = []
            for cap, im in t.items():
                save(im, f'{lab}_{cap.replace(" / ", "_").replace(" ", "_").lower()}')
                figs.append(n41_page.slot(im, cap if cap != 'Blätter' else f'Blätter (Tint #{"%02x%02x%02x" % s["tint"][:3]})'))
            for cap, a, b in (('Stamm', 'Stamm oben', 'Stamm Seite'), ('entrindet', 'entrindet oben', 'entrindet Seite'),
                              ('Bretter', 'Bretter', 'Bretter'), ('Blätter', 'Blätter', 'Blätter'),
                              ('Stöcke', 'Gestrüpp-Stöcke oben', 'Gestrüpp-Stöcke Seite')):
                rr = cube(t[a], t[b])
                save(rr, f'{lab}_render_{cap.lower()}')
                figs.append(iso_fig(rr, f'Würfel {cap}'))
            out.append(card(lab, figs))
        b, a = s['bark'], s['accent']
        cols = {'log': b[3], 'leaf': tuple(int(c * 0.85) for c in s['tint'][:3]) + (255,), 'bud': a[3], 'stick': b[4],
                'knot': mix(b[5], a[2], 0.35)}
        trees = []
        for cap, fn in (('hell: großer End-Baum', r1.big_tree), ('dunkel: schmales Gestrüpp aus Stöcken', r1.scrub),
                        ('End-natürlich: chorusartig, schmal', r1.chorus)):
            im = r1.iso(fn(s['key']), cols)
            save(im, f'{s["base"]}_baum_{fn.__name__}')
            trees.append(f'<figure>{r1.big(im, 300)}<figcaption>{html.escape(cap)}</figcaption></figure>')
        out.append(card(f'{s["base"]} · Baumformen (Skizze)', trees))
        out.append('</div>')
    out.append('<h2>Quellen</h2><ul>')
    out += [f'<li><a href="{u}">{html.escape(tt)}</a></li>' for tt, u in SOURCES]
    out.append('</ul><p class="muted">Generator: tools/textures/n44_round2_2026_10_12.py · PNGs: docs/ai/previews/n44/r2/ '
               '(Branch claude-n44-tex). Vanilla-Texturen nur angesehen, nicht eingecheckt.</p></main></body></html>')
    if '--html' in sys.argv:
        Path(sys.argv[sys.argv.index('--html') + 1]).write_text('\n'.join(out), encoding='utf-8')


if __name__ == '__main__':
    main()
