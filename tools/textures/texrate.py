#!/usr/bin/env python3
"""Texture rating after PRINZIPIEN A8d (b-texrate15, 09.11.): measures a texture like the RIFT14 Vanilla analysis did
and gives a traffic light (rot/orange/gruen) with reasons.

Metrics (per texture, first animation frame, only opaque pixels count):
  big    largest 4-connected area of exactly one colour, scaled to a 16x16 texture (px)
  ge6    share of opaque pixels that lie in such areas >= 6 px (%)
  top    share of the most frequent colour (%)
  hard   A8b: share of neighbour pairs with a luminance jump > 40 (%)
  cols   number of distinct colours
The thresholds are guide values (Richtwerte) taken from the Vanilla 26.3 textures; `--vanilla` measures the Vanilla
block/item textures from the client jar AT RUNTIME and prints how they would be rated (nothing from the jar is written
into the repo). Usage:
  python3 tools/textures/texrate.py a.png b.png          # rate files
  python3 tools/textures/texrate.py --json out.json PNG..  # machine readable
  python3 tools/textures/texrate.py --vanilla [--jar J]  # Vanilla reference statistics (percentiles + light counts)
  python3 tools/textures/texrate.py --selftest
"""
from __future__ import annotations

import argparse
import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image

N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))
JAR = Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'

# Richtwerte (orange from, red from), set by the RIFT14 owner decision (R6 with a 31-px area and 48 % in areas >= 6 px
# was "zu flach" -> orange; R9 with 6 px / 5 % -> green) and checked on the Vanilla 26.3 textures (`--vanilla`):
# rough Vanilla terrain is green (planks, ores, end stone, deepslate, bricks), deliberately smooth Vanilla (stone,
# iron/purpur block, stone bricks) is orange, i.e. orange = "is the smoothness intended?", decided by a look. Opaque block faces vs. cutout textures (items, plants, doors, ...).
# hard follows A8b (~7 % wanted, over ~12-15 % too hard); for cutouts the outline makes hard edges normal, so it is
# not rated there. cols: too few colours (flat) is red at <= 3, too many (> 32, smooth gradient/blur?) orange.
RULES = {
    'block': dict(big=(20, 50), ge6=(40, 65), top=(40, 55), hard=(15, 30)),
    'cutout': dict(big=(34, 60), ge6=(45, 70), top=(40, 55)),
}
COLS_RED, COLS_MANY = 3, 32
# Typical Vanilla building materials/items, the yardstick shown by `--vanilla` (names only; pixels come from the jar).
REF = ('stone cobblestone deepslate tuff andesite polished_andesite granite smooth_stone stone_bricks mossy_stone_bricks '
       'bricks mud_bricks tuff_bricks deepslate_bricks deepslate_tiles end_stone end_stone_bricks purpur_block '
       'purpur_pillar_top obsidian crying_obsidian blackstone polished_blackstone_bricks netherrack nether_bricks basalt_side '
       'amethyst_block quartz_block_side quartz_bricks sandstone cut_sandstone prismarine prismarine_bricks '
       'dark_prismarine oak_planks spruce_planks birch_planks dark_oak_planks crimson_planks warped_planks cherry_planks '
       'bamboo_planks oak_log oak_log_top iron_block gold_block diamond_block emerald_block netherite_block '
       'copper_block cut_copper raw_iron_block coal_ore iron_ore gold_ore deepslate_iron_ore nether_gold_ore '
       'ancient_debris_side terracotta white_concrete white_wool').split()
REF_ITEMS = ('iron_ingot gold_ingot copper_ingot netherite_ingot raw_iron raw_gold diamond emerald amethyst_shard '
             'ender_pearl apple bread stick iron_pickaxe iron_sword oak_door iron_nugget coal quartz brick').split()
LABEL = dict(big='groesste Flaeche', ge6='in Flaechen>=6', top='Hauptton', hard='hart', cols='Farben')


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def load(src) -> Image.Image:
    """RGBA, first square frame of an animation strip."""
    im = src if isinstance(src, Image.Image) else Image.open(src)
    im = im.convert('RGBA')
    w, h = im.size
    return im.crop((0, 0, w, w)) if h > w else im


def regions(im):
    """4-connected areas of exactly the same colour (opaque pixels only) -> [(colour, [pixels])]."""
    a = im.load(); w, h = im.size; seen = set(); out = []
    for y in range(h):
        for x in range(w):
            if (x, y) in seen or a[x, y][3] == 0:
                continue
            c, st, r = a[x, y], [(x, y)], []
            while st:
                q = st.pop()
                if q in seen or not (0 <= q[0] < w and 0 <= q[1] < h) or a[q] != c:
                    continue
                seen.add(q); r.append(q)
                st += [(q[0] + dx, q[1] + dy) for dx, dy in N4]
            out.append((c, r))
    return out


def hard_share(im):
    """A8b: share of opaque neighbour pairs (right/down) with a luminance jump > 40."""
    a = im.load(); w, h = im.size; n = hd = 0
    for y in range(h):
        for x in range(w):
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < w and y + dy < h and a[x, y][3] and a[x + dx, y + dy][3]:
                    n += 1
                    hd += abs(lum(a[x, y]) - lum(a[x + dx, y + dy])) > 40
    return hd / max(1, n)


def metrics(src) -> dict:
    im = load(src); w, h = im.size
    regs = regions(im)
    opaque = sum(len(r) for _, r in regs)
    if not opaque:
        return dict(size=f'{w}x{h}', opaque=0, kind='cutout', big=0, ge6=0, top=0, hard=0, cols=0)
    scale = 256 / (w * h)  # 32x32 textures: areas count like on a 16x16 face
    cols = {}
    for c, r in regs:
        cols[c] = cols.get(c, 0) + len(r)
    return dict(size=f'{w}x{h}', opaque=round(opaque / (w * h) * 100, 1),
                kind='block' if opaque >= 0.95 * w * h else 'cutout',
                big=round(max(len(r) for _, r in regs) * scale, 1),
                ge6=round(sum(len(r) for _, r in regs if len(r) * scale >= 6) / opaque * 100, 1),
                top=round(max(cols.values()) / opaque * 100, 1),
                hard=round(hard_share(im) * 100, 1), cols=len(cols))


def rate(m: dict) -> tuple[str, list[str]]:
    """Traffic light: rot if one metric reaches its red value or three reach orange; orange if one or two reach orange
    (two orange flat-metrics big+ge6 count as one finding: they describe the same flat area); else gruen."""
    if not m.get('opaque'):
        return 'gruen', ['leer']
    rule = RULES[m['kind']]; red, orange = [], []
    for k, (o, r) in rule.items():
        v = m[k]
        if v >= r: red.append(f'{LABEL[k]} {v:g}')
        elif v >= o: orange.append(f'{LABEL[k]} {v:g}')
    if m['cols'] <= COLS_RED: red.append(f"{LABEL['cols']} {m['cols']}")
    elif m['cols'] > COLS_MANY: orange.append(f"{LABEL['cols']} {m['cols']} (Verlauf?)")
    flat_pair = sum(1 for s in orange if s.startswith((LABEL['big'], LABEL['ge6']))) == 2
    findings = len(orange) - (1 if flat_pair else 0)
    if red or findings >= 3:
        return 'rot', red + orange
    if orange:
        return 'orange', orange
    return 'gruen', []


def vanilla(jar: Path = JAR, dirs=('block', 'item')):
    """Vanilla textures straight from the client jar (memory only)."""
    out = {}
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            for d in dirs:
                p = f'assets/minecraft/textures/{d}/'
                if n.startswith(p) and n.endswith('.png') and '/' not in n[len(p):]:
                    out[f'{d}/{n[len(p):-4]}'] = Image.open(io.BytesIO(z.read(n)))
    return out


def vanilla_stats(jar: Path = JAR):
    ms = {}
    for n, im in vanilla(jar).items():
        if min(im.size) >= 16:
            ms[n] = metrics(im)
    lines = []
    for kind in ('block', 'cutout'):
        sub = [m for m in ms.values() if m['kind'] == kind]
        lines.append(f'{kind}: {len(sub)} Vanilla-Texturen')
        for k in ('big', 'ge6', 'top', 'hard', 'cols'):
            v = sorted(m[k] for m in sub)
            q = lambda p: v[min(len(v) - 1, int(p * len(v)))]
            lines.append(f'  {LABEL[k]:16s} p25 {q(.25):6g} p50 {q(.5):6g} p75 {q(.75):6g} p90 {q(.9):6g}')
        lights = {}
        for m in sub:
            lights[rate(m)[0]] = lights.get(rate(m)[0], 0) + 1
        lines.append(f'  Ampel: {lights}')
    for title, names in (('Baustoffe (REF)', [f'block/{n}' for n in REF]), ('Items (REF_ITEMS)', [f'item/{n}' for n in REF_ITEMS])):
        rated = [(n, *rate(ms[n])) for n in names if n in ms]
        cnt = {}
        for _, l, _ in rated:
            cnt[l] = cnt.get(l, 0) + 1
        lines.append(f'{title}: {cnt}')
        lines += [f'  {l:6s} {n}: {", ".join(w)}' for n, l, w in rated if l != 'gruen']
    return ms, '\n'.join(lines)


def selftest():
    flat = Image.new('RGBA', (16, 16), (120, 120, 120, 255))
    m = metrics(flat); assert m['big'] == 256 and m['cols'] == 1 and rate(m)[0] == 'rot', m
    chk = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):  # 2x2 groups of 4 close tones: no flat area, no hard edge
            chk.putpixel((x, y), [(100, 100, 100, 255), (110, 110, 110, 255), (120, 120, 120, 255),
                                  (130, 130, 130, 255)][((x // 2) + (y // 2) * 3) % 4])
    m = metrics(chk); assert m['big'] == 4 and m['hard'] == 0 and rate(m)[0] == 'gruen', m
    hard = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            hard.putpixel((x, y), (0, 0, 0, 255) if (x + y) % 2 else (250, 250, 250, 255))
    m = metrics(hard); assert m['hard'] == 100 and rate(m)[0] == 'rot', m
    item = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); item.paste(chk.crop((0, 0, 6, 6)), (5, 5))
    m = metrics(item); assert m['kind'] == 'cutout' and m['opaque'] == 14.1 and m['big'] == 4, m
    strip = Image.new('RGBA', (16, 32), (0, 0, 0, 255)); strip.paste(chk, (0, 0))
    assert metrics(strip)['size'] == '16x16'
    big = chk.resize((32, 32), Image.NEAREST)
    assert metrics(big)['big'] == 4, metrics(big)  # 32x32: a 2x2 group of 16 px counts as 4 px
    print('texrate selftest ok')


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('png', nargs='*')
    ap.add_argument('--json')
    ap.add_argument('--vanilla', action='store_true')
    ap.add_argument('--jar', type=Path, default=JAR)
    ap.add_argument('--selftest', action='store_true')
    a = ap.parse_args(argv)
    if a.selftest:
        selftest(); return 0
    if a.vanilla:
        print(vanilla_stats(a.jar)[1]); return 0
    rows = []
    for p in a.png:
        m = metrics(p); light, why = rate(m)
        rows.append(dict(path=p, light=light, why=why, **m))
        print(f"{light:6s} {p}  big {m['big']:g} | ge6 {m['ge6']:g} % | top {m['top']:g} % | hart {m['hard']:g} % | "
              f"Farben {m['cols']} | {m['kind']}{'  -> ' + ', '.join(why) if why else ''}")
    if a.json:
        Path(a.json).write_text(json.dumps(rows, indent=1), encoding='utf-8')
    return 0


if __name__ == '__main__':
    sys.exit(main())
