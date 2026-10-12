"""Nachtrag 45 textures (owner 2026-10-12).

Run: python3.12 tools/textures/n45_2026_10_12.py [--html out.html] [--vanilla]
- astral_table_side(): side of the Astral Enchanting Table repainted for the 12 px high model (uv 0,4..16,16 like the
  Vanilla enchanting table): rows 0-3 stay transparent, the A3 motif (cloth drape, gold hem, enderite crystals in the
  top corners, faint constellation) sits in rows 4-15. Used by astral_enchanting_textures.py (owner: apply directly).
- blazing_obsidian(v): three proposals B1-B3 that read as a variant of Vanilla crying obsidian: the same five-tone
  obsidian ground and the same "tears" layout idea, but blaze/ember orange instead of violet. B3 is animated
  (3 frames, interpolated like Vanilla magma). Proposals only, the game texture is unchanged.
Own pixel art drawn from code; Vanilla textures are only read (with --vanilla) for the side-by-side comparison.
"""
from __future__ import annotations

import base64
import io
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import n41_proposals_2026_10_12 as n41  # noqa: E402
from n41_proposals_2026_10_12 import GOLD, OBS, VIOLET, get, mix, new, pick, put, fbm, H  # noqa: E402

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/ai/previews/n45'
OV = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures'

# Obsidian ground: five tones dark -> light with roughly the share Vanilla obsidian uses (25/29/26/13/7 %).
OBS5 = [H('000001'), H('06030b'), H('100c1c'), H('271e3d'), H('3b2754')]
OBS5_SHARE = [0.25, 0.54, 0.80, 0.93, 1.0]
EMB3 = [H('5c1603'), H('b04606'), H('ff8a1c')]   # dark rim, glow, hot core (crying obsidian uses three violets)
EMB_HOT = H('ffe07a')


# ------------------------------------------------------------------ astral enchanting table, side for 12 px
def astral_table_side():
    side = new()
    base = new()
    n41.obsidian_base(base, 33, OBS, flow=1.0)   # same ground as the N41 A3 side
    for y in range(4, 16):
        for x in range(16):
            side.putpixel((x, y), base.getpixel((x, y)))
    # cloth drape over the top edge between the corner crystals: deepest in the middle (like Vanilla's red drape)
    for x in range(3, 13):
        depth = 4 + (3 if 5 <= x <= 10 else 2 if x in (4, 11) else 1)   # rows 4..hem-1 are cloth
        if 7 <= x <= 8:
            depth += 1
        for y in range(4, depth):
            fold = 0.5 + 0.5 * n41.math.sin(n41.TAU * x / 8 + y * 0.3)
            side.putpixel((x, y), pick(VIOLET[:5], 0.2 + fold * 0.45 + (0.2 if y == 4 else 0)))
        side.putpixel((x, depth), GOLD[3] if x % 2 else GOLD[2])
        side.putpixel((x, depth + 1), mix(get(side, x, depth + 1), GOLD[0], 0.45))
    # enderite crystals in the top corners (match the top's corner crystals, like Vanilla's diamonds)
    for cx in (1, 14):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            put(side, cx + dx, 5 + dy, VIOLET[5] if (dx, dy) == (0, 0) else VIOLET[3])
        put(side, cx, 4, VIOLET[6])
    # faint constellation in the stone
    for (x, y) in ((3, 11), (6, 13), (9, 10), (12, 12)):
        put(side, x, y, VIOLET[5])
    for x in range(4, 6):
        put(side, x, 12, VIOLET[2])
    for x in range(7, 9):
        put(side, x, 12 - (x - 7), VIOLET[2])
    for x in range(10, 12):
        put(side, x, 11, VIOLET[2])
    # lighter pillar columns at the edges so the block reads like a table (below the crystals)
    for y in range(7, 16):
        for x in (0, 15):
            side.putpixel((x, y), mix(side.getpixel((x, y)), OBS[-1], 0.35))
    return side


# ------------------------------------------------------------------ blazing obsidian proposals
def obsidian_ground(seed):
    vals = sorted((fbm(x, y, seed) + 0.35 * n41.hsh(x, y, seed + 3), x, y) for y in range(16) for x in range(16))
    im = new()
    for i, (_, x, y) in enumerate(vals):
        t = i / 256
        k = next(j for j, s in enumerate(OBS5_SHARE) if t < s)
        im.putpixel((x, y), OBS5[k])
    return im


def tears(seed, clusters, steps, drip=False):
    """Tileable glowing clusters like crying obsidian's tears: list of (x, y, level 0..2)."""
    rnd = random.Random(seed)
    cells = {}
    for _ in range(clusters):
        x, y = rnd.randrange(16), rnd.randrange(16)
        for s in range(rnd.randint(*steps)):
            lvl = 2 if s == 1 else 1 if s in (0, 2) else 0
            cells[(x % 16, y % 16)] = max(cells.get((x % 16, y % 16), 0), lvl)
            if drip:
                dx, dy = rnd.choice(((0, 1), (0, 1), (0, 1), (1, 1), (-1, 1)))
            else:
                dx, dy = rnd.choice(((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, 1), (1, -1)))
            x, y = x + dx, y + dy
        # dark ember rim on one or two neighbours, like the dark violet edge of the tears
        for dx, dy in rnd.sample(((1, 0), (-1, 0), (0, 1), (0, -1)), 2):
            cells.setdefault(((x + dx) % 16, (y + dy) % 16), 0)
    return cells


def blazing_obsidian(v, frame=0):
    im = obsidian_ground(450 + (v if v < 2 else 0))
    if v == 1:   # B2: fewer, longer drips running down (glowing tears), hot cores
        cells = tears(4512, 7, (5, 8), drip=True)
    else:        # B1/B3: crying-obsidian layout, scattered short branches
        cells = tears(4511, 11, (3, 6))
    pulse = (0.0, 0.5, 1.0)[frame]
    for (x, y), lvl in cells.items():
        c = EMB3[lvl]
        if v == 1 and lvl == 2 and n41.hsh(x, y, 9) > 0.55:
            c = EMB_HOT
        if v == 2:   # B3 animated: the glow breathes (rim -> glow, glow -> hot) over three frames
            c = mix(c, EMB3[min(2, lvl + 1)] if lvl < 2 else EMB_HOT, pulse * 0.6)
        im.putpixel((x, y), c)
    return im


def blazing_frames(v):
    return [blazing_obsidian(v, f) for f in range(3)] if v == 2 else [blazing_obsidian(v)]


# ------------------------------------------------------------------ page
def vanilla(name):
    return __import__('n41_round2_2026_10_12').vanilla(name)


def table_render(top, side, bottom, size=128):
    up, sd = (0, 0, 16, 16), (0, 4, 16, 16)
    return n41.render([((0, 0, 0), (16, 12, 16), {'up': (top, up), 'north': (side, sd), 'west': (side, sd)})],
                      size=size, scale=0.62)


def uri(im):
    b = io.BytesIO()
    im.save(b, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(b.getvalue()).decode()


def gif_uri(frames, scale):
    big = []
    seq = []
    for i in range(3):   # interpolate like the mcmeta (frametime 8 = 0.4 s per frame)
        a, b = frames[i], frames[(i + 1) % 3]
        for k in range(4):
            seq.append(Image.blend(a, b, k / 4))
    for f in seq:
        big.append(f.convert('RGB').resize((16 * scale, 16 * scale), Image.NEAREST))
    b = io.BytesIO()
    big[0].save(b, 'GIF', save_all=True, append_images=big[1:], duration=100, loop=0)
    return 'data:image/gif;base64,' + base64.b64encode(b.getvalue()).decode()


def scaled(im, s):
    return uri(im.resize((im.width * s, im.height * s), Image.NEAREST))


def tile_card(label, im, note='', cube=None, anim=None):
    imgs = ''.join(f'<img src="{scaled(im, s)}" width="{16 * s}" height="{16 * s}" alt="{label} {s}x">' for s in (1, 2, 8))
    extra = f'<img src="{anim}" width="128" height="128" alt="{label} animiert">' if anim else ''
    cb = f'<img src="{uri(cube)}" width="128" height="128" alt="{label} Wuerfel">' if cube else ''
    return (f'<figure><figcaption><b>{label}</b><span>{note}</span></figcaption>'
            f'<div class="row">{imgs}{extra}{cb}</div></figure>')


def page(path, with_vanilla):
    top = Image.open(OV / 'block/astral_enchanting_table_top.png').convert('RGBA')
    bottom = Image.open(OV / 'block/astral_enchanting_table_bottom.png').convert('RGBA')
    old_side = n41.astral_table(2, round_top=True)[1]
    new_side = astral_table_side()
    cards_a = [tile_card('vorher: Seite (N41 A3)', old_side, 'Motiv in Zeilen 0-5, das Modell zeigt nur 4-15',
                         table_render(top, old_side, bottom)),
               tile_card('nachher: Seite fuer 12 px', new_side, 'Zeilen 0-3 leer wie Vanilla, Tuch/Saum/Kristalle in 4-15',
                         table_render(top, new_side, bottom)),
               tile_card('Oberseite (unveraendert)', top, 'volle 16x16, passt bereits')]
    if with_vanilla:
        vt, vs = vanilla('block/enchanting_table_top'), vanilla('block/enchanting_table_side')
        cards_a.insert(0, tile_card('Vanilla enchanting_table (Referenz)', vs, 'Seite fuer 12 px gemalt', table_render(vt, vs, vt)))
    cards_b = []
    if with_vanilla:
        vc = vanilla('block/crying_obsidian')
        cards_b.append(tile_card('Vanilla crying_obsidian (Referenz)', vc, 'statisch, 5 Obsidian- + 3 Violett-Toene', n41.cube(vc, vc)))
    cur = Image.open(OV / 'block/blazing_obsidian.png').convert('RGBA')
    cards_b.append(tile_card('jetzt: N41 A18', cur, 'Glut-Linien, eigener Grund', n41.cube(cur, cur)))
    notes = {0: 'B1 Glut-Traenen: Obsidian-Grund + verstreute kurze Aeste wie crying, 3 Orange-Toene, statisch',
             1: 'B2 Glut-Rinnsale: weniger, laengere Tropfspuren nach unten, heisse gelbe Kerne, statisch',
             2: 'B3 = B1 animiert: Glut pulsiert (3 Frames, interpolate, frametime 8 wie Vanilla magma)'}
    for v in range(3):
        fr = blazing_frames(v)
        cards_b.append(tile_card(f'B{v + 1}', fr[0], notes[v], n41.cube(fr[0], fr[0]),
                                 gif_uri(fr, 8) if v == 2 else None))
    refs = [('Vanilla 26.3 Client-Jar: block/crying_obsidian, obsidian, magma (+.mcmeta), enchanting_table_side/top, '
             'models/block/enchanting_table.json (uv 0,4,16,16) - nur gelesen', None),
            ('Tynker: Crying Obsidian', 'https://www.tynker.com/minecraft/blocks/view/crying_obsidian/'),
            ('BisectHosting: An Inventory\'s Tale - Crying Obsidian (Traenen nicht animiert)',
             'https://www.bisecthosting.com/de-de/blog/an-inventorys-tale-crying-obsidian'),
            ('Block-Render crying_obsidian', 'https://blocks-0w3d.onrender.com/block/crying_obsidian'),
            ('Block-Render enchanting_table', 'https://blocks-0w3d.onrender.com/block/enchanting_table'),
            ('Faithful f32 Texturing Guidelines (Formen/Farben wie Vanilla halten)',
             'https://docs.faithfulpack.net/pages/textures/f32-texturing-guidelines')]
    ref_html = ''.join(f'<li><a href="{u}">{t}</a></li>' if u else f'<li>{t}</li>' for t, u in refs)
    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>N45 Texturen</title><style>
:root{{--bg:#f4f4f2;--fg:#1d1f23;--card:#fff;--mut:#5d636c;--line:#d9dadd}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#16181c;--fg:#e8e9ec;--card:#20232a;--mut:#9aa1ab;--line:#30343c}}}}
:root[data-theme="dark"]{{--bg:#16181c;--fg:#e8e9ec;--card:#20232a;--mut:#9aa1ab;--line:#30343c}}
body{{background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;margin:0;padding:16px}}
main{{max-width:1100px;margin:auto}} h2{{margin-top:28px}}
figure{{background:var(--card);border:1px solid var(--line);border-radius:8px;margin:0 0 12px;padding:12px}}
figcaption{{display:flex;flex-wrap:wrap;gap:4px 12px;margin-bottom:8px}} figcaption span{{color:var(--mut)}}
.row{{display:flex;flex-wrap:wrap;align-items:flex-end;gap:12px}}
img{{image-rendering:pixelated;max-width:100%;height:auto}} a{{color:inherit}} li{{color:var(--mut)}}
</style></head><body><main>
<h1>Nachtrag 45: Texturen</h1>
<p>Je Kachel 1x / 2x / 8x, dann Block-Render. Eigene Pixel-Art aus Code (tools/textures/n45_2026_10_12.py), Vanilla nur als Vergleich.</p>
<h2>2. Astral-Verzauberungstisch: Seite fuer das 12 px hohe Modell (umgesetzt)</h2>
<p>Das Modell ist wie der Vanilla-Zaubertisch 16x12x16 und zeigt an den Seiten uv 0,4..16,16. Die alte A3-Seite hatte Tuch und Saum in den Zeilen 0-5, davon blieb nur der Saum sichtbar. Neu: Zeilen 0-3 leer, das Motiv in 4-15.</p>
{''.join(cards_a)}
<h2>1. Lohen-Obsidian: 3 Vorschlaege als Variante von Crying Obsidian</h2>
<p>Gleicher fuenftoniger Obsidian-Grund und gleiches Traenen-Prinzip wie Crying Obsidian, aber Glut-Orange statt Violett. Crying Obsidian ist in Vanilla nicht animiert; B3 zeigt die animierte Variante (GIF-Vorschau neben 8x).</p>
{''.join(cards_b)}
<h2>Referenzen</h2><ul>{ref_html}</ul>
</main></body></html>'''
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(html)
    return html


def main():
    args = sys.argv[1:]
    OUT.mkdir(parents=True, exist_ok=True)
    astral_table_side().save(OUT / 'astral_enchanting_table_side_12px.png')
    for v in range(3):
        fr = blazing_frames(v)
        if len(fr) == 1:
            fr[0].save(OUT / f'B{v + 1}_blazing_obsidian.png')
        else:   # animated strip, as it would ship with a .mcmeta
            strip = Image.new('RGBA', (16, 48))
            for i, f in enumerate(fr):
                strip.paste(f, (0, 16 * i))
            strip.save(OUT / f'B{v + 1}_blazing_obsidian.png')
    if '--html' in args:
        page(args[args.index('--html') + 1], '--vanilla' in args)
    print('n45 textures written to', OUT.relative_to(ROOT))


if __name__ == '__main__':
    main()
