"""B23-T5..T9 (owner 23.10. afternoon, docs/ai/PLAN-A-SBTEX23-2026-10-23.md): exemplary candidates, nothing built in.

    python3.12 tools/textures/sbtex23_2026_10_23.py --out /root/sb-tools-a/previews/a-sbtex23-preview.html \
        --cand /root/sb-tools-a/previews/a-sbtex23 [--jar client.jar]

  T5  Astral / Nihil / Bridge Redstone Block   A Vanilla build (rim, noisy dust, dark core) - B gem bevel rings - C pressed dust grains
  T6  Astralit / Nihilith ore                  End stone base + ore overlay: A clusters - B clusters + glow (Astral) /
                                               shadow halo (Nihil, "anti-glow") - C streaks + glow / shadow
  T7  Astral / Nihil piston (off + active)     A push plate with chevrons - B star burst / vortex window - C tiles with wave rings
  T8  Construction Light (12-frame pulse)      A polished lantern - B four panes - C crystal core
  T9  Astral Vault front motif                 6x6 Enderite motif centred under the lock: A lump - B glowing core - C core + halo

Every candidate is drawn in layers (Basis / Overlay / Details, layers.snap) from own pixel maps and own ramps of 8-12
tones; the ore base and the vault sheet come from the 26.3 client jar (they would ship through vanilla_derived.py,
like today's vault and pistons) and the Vanilla comparisons live only in the HTML. Candidates (PNG + .layers.json)
go to --cand outside the repository; nothing in the repository is written. Run from the repo root.
"""
from __future__ import annotations

import argparse
import base64
import io
import math
import random
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))

from PIL import Image  # noqa: E402

import layers  # noqa: E402
import vanilla_derived  # noqa: E402
import n46_round3_2026_10_12 as n46  # noqa: E402

BLOCK = 'textures/block'
OVERLAY = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding' / BLOCK
MAIN = ROOT / 'src/main/resources/assets/simplebuilding' / BLOCK


# ---------------------------------------------------------------- colour + noise helpers

def hx(s: str) -> tuple:
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def ramp(keys: list[str], n: int) -> list[tuple]:
    """n tones evenly spread over the key colours (dark -> light)."""
    ks = [hx(k) for k in keys]
    out = []
    for i in range(n):
        t = i / (n - 1) * (len(ks) - 1)
        j = min(int(t), len(ks) - 2)
        f = t - j
        out.append(tuple(round(ks[j][c] + (ks[j + 1][c] - ks[j][c]) * f) for c in range(3)))
    return out


def mix(a, b, t):
    return tuple(round(a[c] + (b[c] - a[c]) * t) for c in range(3))


def vnoise(seed: int, cell: int, size: int = 16) -> list[list[float]]:
    """Tileable value noise (period = size), smoothstep between lattice points every `cell` pixels."""
    r = random.Random(seed)
    n = size // cell
    lat = [[r.random() for _ in range(n)] for _ in range(n)]
    out = []
    for y in range(size):
        row = []
        for x in range(size):
            gx, gy = x / cell, y / cell
            x0, y0 = int(gx) % n, int(gy) % n
            x1, y1 = (x0 + 1) % n, (y0 + 1) % n
            fx, fy = gx - int(gx), gy - int(gy)
            sx, sy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
            a = lat[y0][x0] + (lat[y0][x1] - lat[y0][x0]) * sx
            b = lat[y1][x0] + (lat[y1][x1] - lat[y1][x0]) * sx
            row.append(a + (b - a) * sy)
        out.append(row)
    return out


def fbm(seed: int) -> list[list[float]]:
    """Vanilla-like stone/dust noise: coarse blotches + finer grain + per-pixel jitter (0..1)."""
    a, b, c = vnoise(seed, 8), vnoise(seed + 1, 4), vnoise(seed + 2, 2)
    r = random.Random(seed + 3)
    return [[0.45 * a[y][x] + 0.3 * b[y][x] + 0.15 * c[y][x] + 0.1 * r.random() for x in range(16)] for y in range(16)]


def edge(x, y, w=16, h=16):
    return min(x, y, w - 1 - x, h - 1 - y)


class Canvas:
    """A 16x16 (or w x h) picture painted as tone indices into one or more ramps; snap() records a layer."""

    def __init__(self, w=16, h=16):
        self.img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
        self.w, self.h = w, h

    def put(self, x, y, rgb, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img.putpixel((x, y), tuple(rgb) + (a,))

    def get(self, x, y):
        return self.img.getpixel((x % self.w, y % self.h))[:3]

    def snap(self, name):
        layers.snap(self.img, name)
        return self


def draw_map(cv: Canvas, rows: list[str], colors: dict, ox=0, oy=0):
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                c = colors[ch]
                cv.put(ox + x, oy + y, c(cv.get(ox + x, oy + y)) if callable(c) else c)


# ---------------------------------------------------------------- ramps (own, 9-12 tones)

LINE = {  # the three redstone lines (B7 / N51 hues), widened to 10 tones each
    'astral': ramp(['1c0a1a', '3e1838', '67285c', '8e3e80', 'b45aa2', 'd47cbe', 'eba6d8', 'fbd6ee', 'fff4fb'], 10),
    'nihil': ramp(['06141a', '0c2630', '15404a', '1f5a64', '2e7880', '46969c', '68b6b8', '9ad6d6', 'd8f6f4'], 10),
    'red': ramp(['3a0400', '5c0a00', '7c1000', '9a1602', 'b81c04', 'd42208', 'ec300c', 'ff5a2c', 'ff9a74'], 10),
}
ENDERITE = {k: hx(v) for k, v in {'K': '1c0a33', 'D': '2d1656', 'd': '3e2173', 'm': '55309a', 'M': '6d45b8',
                                    'l': '8e63dc', 'L': 'a57de9', 'H': 'cfb2fb', 'W': 'f4d2ff'}.items()}


# ---------------------------------------------------------------- T5 redstone blocks

SPARKLE = [(3, 4), (11, 3), (12, 11), (5, 12)]
PITS = [(4, 4), (11, 5), (10, 11), (4, 11)]


def rs_vanilla(kind: str) -> Image.Image:
    """A: the Vanilla redstone block build - light rim, noisy compacted dust, an irregular dark core - in the line's
    own ramp; Astral gets four tiny star glints, Nihil dark pits and a deeper void, Bridge red with Astral and Nihil
    veins (marble-like, following the dust noise)."""
    R = LINE['red' if kind == 'bridge' else kind]
    n, n2 = fbm(11), fbm(23)
    cv = Canvas()
    idx = [[0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 7.5
            v = 0.62 + 0.42 * (n[y][x] - 0.5) - 0.42 * max(0.0, 1 - d) ** 1.1 * (0.6 + 0.8 * n2[y][x])
            idx[y][x] = max(1, min(6, round(v * 8)))
            cv.put(x, y, R[idx[y][x]])
    cv.snap('Basis Staub')
    for y in range(16):
        for x in range(16):
            e = edge(x, y)
            if e == 0:
                cv.put(x, y, R[8 if (x + y) % 5 else 7])
            elif e == 1:
                cv.put(x, y, R[max(idx[y][x], 5 + (n[y][x] > 0.55))])
    cv.snap('Rand')
    if kind == 'astral':
        for sx, sy in SPARKLE:
            cv.put(sx, sy, R[9])
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                cv.put(sx + dx, sy + dy, R[7])
    elif kind == 'nihil':
        for y in range(5, 11):
            for x in range(5, 11):
                if math.hypot(x - 7.5, y - 7.5) < 2.4 + n2[y][x]:
                    cv.put(x, y, R[0 if math.hypot(x - 7.5, y - 7.5) < 1.6 else 1])
        for px, py in PITS:
            cv.put(px, py, R[0])
            cv.put(px, py + 1, R[6])
    else:
        # two thin veins running down to the left (like the strands of the bridge dust), light crest + dark underside
        for R2, off, ph in ((LINE['astral'], -3, 0.0), (LINE['nihil'], 4, 2.0)):
            for x in range(2, 14):
                y = 15 - x + off + round(1.3 * math.sin(x * 0.75 + ph))
                if 2 <= y <= 13:
                    cv.put(x, y, R2[min(8, idx[y][x] + 3)])
                    if y + 1 <= 13:
                        cv.put(x, y + 1, R2[max(2, idx[y][x] + 1)])
    return cv.snap('Details').img


def rs_bevel(kind: str) -> Image.Image:
    """B: the mineral-block bevel of Vanilla's emerald / diamond block - stepped light and dark rings towards the
    middle, a little dust noise; Astral a soft four-pointed star in the middle, Nihil a dark void ring, Bridge the
    rings in the three lines (outer redstone, then Astral, inner Nihil)."""
    RING = [8, 6, 7, 5, 6, 4, 5, 3]
    n = fbm(37)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            e = edge(x, y)
            R = LINE['red' if kind == 'bridge' and e < 3 else 'astral' if kind == 'bridge' and e < 5 else
                     'nihil' if kind == 'bridge' else kind]
            t = RING[e] + (1 if n[y][x] > 0.68 else -1 if n[y][x] < 0.3 else 0)
            # bevel: bottom/right of each ring one tone darker (Vanilla emerald block light from top left)
            if e < 7 and (x == 15 - e or y == 15 - e):
                t -= 1
            cv.put(x, y, R[max(1, min(9, t))])
    cv.snap('Basis Facetten')
    R = LINE['red' if kind == 'bridge' else kind]
    if kind == 'astral':
        star = ['....x...', '....x...', '...xyx..', 'xxyzzyxx', '...xyx..', '....x...', '....x...', '........']
        draw_map(cv, star, {'x': R[7], 'y': R[8], 'z': R[9]}, 4, 4)
    elif kind == 'nihil':
        for y in range(4, 12):
            for x in range(4, 12):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 1.6:
                    cv.put(x, y, R[0])
                elif d < 2.6:
                    cv.put(x, y, R[1])
                elif d < 3.4:
                    cv.put(x, y, R[7])
    else:
        N = LINE['nihil']
        for y in range(6, 10):
            for x in range(6, 10):
                cv.put(x, y, N[2 if (x in (6, 9)) != (y in (6, 9)) else 1 if x in (6, 9) else 0])
    return cv.snap('Motiv').img


def _grains():
    r, pts = random.Random(5), []
    for _ in range(4000):                     # spaced grains (torus distance > 4), at most 10
        if len(pts) == 10:
            break
        p = (r.randrange(16), r.randrange(16), r.random() < 0.6)
        if all(min(abs(p[0] - q[0]), 16 - abs(p[0] - q[0])) + min(abs(p[1] - q[1]), 16 - abs(p[1] - q[1])) > 4 for q in pts):
            pts.append(p)
    return pts


GRAINS = _grains()


def rs_dust(kind: str) -> Image.Image:
    """C: pressed dust like Vanilla's glowstone / amethyst block - dark packed ground, rounded light grains with a
    light top-left and a dark lower edge, a calm darker rim; Bridge grains alternate between the three lines."""
    lines = ['red', 'astral', 'nihil'] if kind == 'bridge' else [kind]
    R0 = LINE[lines[0]]
    n = fbm(53)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            cv.put(x, y, R0[1 + round(n[y][x] * 2.4) + (1 if edge(x, y) == 0 else 0)])
    cv.snap('Basis Grund')
    for i, (gx, gy, big) in enumerate(GRAINS):
        R = LINE[lines[i % len(lines)]]
        cells = [(0, 0), (1, 0), (0, 1), (1, 1)] + ([(2, 0), (2, 1), (0, 2), (1, 2)] if big else [])
        for dx, dy in cells:
            t = 6 - (dx + dy) + (1 if big else 0)
            cv.put((gx + dx) % 16, (gy + dy) % 16, R[max(3, min(8, t))])
        cv.put(gx % 16, gy % 16, R[9 if big else 8])
        for dx, dy in ((0, 3), (1, 3), (2, 2), (3, 1), (3, 0)) if big else ((0, 2), (1, 2), (2, 1), (2, 0)):
            cv.put((gx + dx) % 16, (gy + dy) % 16, R[1])
    return cv.snap('Körner').img


# ---------------------------------------------------------------- T6 ores (End stone base + overlay)

ORE_COL = {
    'astral': {'D': hx('6e2462'), 'm': hx('c04ea6'), 'L': hx('f286d8'), 'H': hx('ffe2f6')},
    'nihil': {'D': hx('040a0e'), 'm': hx('0e2a34'), 'L': hx('1e5864'), 'H': hx('7ad8de')},
}
# Clusters like Vanilla's diamond / emerald ore (light top-left, dark rim bottom-right); '.' keeps the end stone.
ORE_CLUSTERS = {
    'astral': ['................',
               '.........mL.....',
               '..mL....mHLm....',
               '.mHLm...DmLD....',
               '.DmLLm...DD.....',
               '..DmD...........',
               '..........mL....',
               '....mL...mHLm...',
               '...mHLD..DmLLm..',
               '...DmD....DmD...',
               '................',
               '.mL.........mL..',
               'mHLm.......mHLD.',
               'DmLD..mL...DmD..',
               '.DD..mHLD.......',
               '......DD........'],
    'nihil': ['................',
              '...m........Dm..',
              '..mLD......DmH..',
              '.DLHm......Dm...',
              '..Dm............',
              '.......Dm.......',
              '......DmLm......',
              '.....DmHLD......',
              '......DmD.......',
              '..........Dm....',
              '..Dm.....DmLm...',
              '.DmLm....DLHD...',
              '..DHD.....DD....',
              '...D...Dm.......',
              '.......DD.......',
              '................'],
}
# Streaks like Vanilla's nether quartz ore: thin diagonal crystals with a dark underside.
ORE_STREAKS = {
    'astral': ['................',
               '...........mH...',
               '..mL......mLD...',
               '...LHm...mLD....',
               '....DLm..DD.....',
               '.....DD.........',
               '........mL......',
               '.......mHLD.....',
               '..m....DLD......',
               '..Lm....D....mL.',
               '..DHm.......mHD.',
               '...DLm.....mLD..',
               '....DD.....DD...',
               '.......mL.......',
               '.......DD.......',
               '................'],
    'nihil': ['................',
              '..........Dm....',
              '..Dm.......Lm...',
              '...mL.......HD..',
              '...DHm......D...',
              '....DD..........',
              '.........m......',
              '........mLD.....',
              '.......DHD......',
              '.Dm....DD.......',
              '..mL.........Dm.',
              '..DLm.......mL..',
              '...DHD.....mHD..',
              '....D......DD...',
              '................',
              '................'],
}


def ore(kind: str, variant: str, base: Image.Image, glow_scale: float = 1.0, glint: int | None = None) -> Image.Image:
    """glow_scale / glint (W24-4 animation): the glow (shadow) strength and which glint pixel stays at full brightness
    (the others a third towards the light tone); defaults = the plain picture."""
    rows = ORE_STREAKS[kind] if variant == 'C' else ORE_CLUSTERS[kind]
    cv = Canvas()
    cv.img = base.crop((0, 0, 16, 16)).convert('RGBA')
    cv.snap('Basis Endstein')
    if variant in ('B', 'C'):
        glow = hx('ff6ad2') if kind == 'astral' else hx('061016')
        amount = (0.42 if kind == 'astral' else 0.5) * glow_scale
        ore_px = {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch != '.'}
        for y in range(16):
            for x in range(16):
                if (x, y) in ore_px:
                    continue
                d = min((math.hypot(x - ox, y - oy) for ox, oy in ore_px), default=9)
                if d <= 2.3:
                    t = amount * (1.0 if d <= 1.0 else 0.55 if d <= 1.5 else 0.25)
                    cv.put(x, y, mix(cv.get(x, y), glow, t))
        cv.snap('Leuchten' if kind == 'astral' else 'Schatten (Anti-Leuchten)')
    draw_map(cv, rows, {k: v for k, v in ORE_COL[kind].items() if k in 'Dm'})
    cv.snap('Erz')
    draw_map(cv, rows, {k: v for k, v in ORE_COL[kind].items() if k in 'LH'})
    if glint is not None:
        hs = [(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch == 'H']
        for i, (x, y) in enumerate(hs):
            if i != glint % len(hs):
                cv.put(x, y, mix(ORE_COL[kind]['H'], ORE_COL[kind]['L'], 0.35))
    return cv.snap('Glanz').img


# ---------------------------------------------------------------- W23-1 T6 = C, hidden (owner 23.10. evening)
# The owner picked C (streaks + glow / shadow) but wants the ore harder to spot: less contrast and glow, smaller visible ore.
# C1 "muted": the C streaks, half the glow, ore tones a quarter towards the End stone, the brightest glint toned down.
# C2 "hidden" (built in): only the core of each streak (no dark rim, the tips far from the light part gone), ore tones
# blended far towards the End stone under them, a soft glint, no glow; the dark rim becomes a faint End stone shadow.
HIDE = {'C1': {'glow': 0.5, 'blend': 0.25, 'H': 'L'}, 'C2': {'glow': 0.0, 'blend': 0.45, 'H': 'L'}}
ES_SHADOW = hx('8e865c')                      # a darker End stone tone (shadow of a crack), not an ore colour


def streak_core(rows: list[str]) -> list[str]:
    """C2: drops the dark rim and every 'm' tip with no light pixel next to it -> 2-3 visible pixels per streak."""
    grid = [list(r.replace('D', 's')) for r in rows]   # 's' = soft End stone shadow where the rim was
    for y, r in enumerate(grid):
        for x, ch in enumerate(r):
            if ch != 'm':
                continue
            near = [grid[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1))
                    if 0 <= x + dx < 16 and 0 <= y + dy < 16]
            if 'H' not in near and 'L' not in near:
                grid[y][x] = 's'
    return [''.join(r) for r in grid]


def ore_hidden(kind: str, grade: str, base: Image.Image) -> Image.Image:
    """T6 = C made harder to spot (grade C1 / C2, see HIDE)."""
    h = HIDE[grade]
    rows = streak_core(ORE_STREAKS[kind]) if grade == 'C2' else ORE_STREAKS[kind]
    cv = Canvas()
    cv.img = base.crop((0, 0, 16, 16)).convert('RGBA')
    cv.snap('Basis Endstein')
    ore_px = {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch in 'DmLH'}
    if h['glow']:
        glow = hx('ff6ad2') if kind == 'astral' else hx('061016')
        amount = (0.42 if kind == 'astral' else 0.5) * h['glow']
        for y in range(16):
            for x in range(16):
                if (x, y) in ore_px:
                    continue
                d = min((math.hypot(x - ox, y - oy) for ox, oy in ore_px), default=9)
                if d <= 1.5:
                    cv.put(x, y, mix(cv.get(x, y), glow, amount * (1.0 if d <= 1.0 else 0.5)))
        cv.snap('Leuchten' if kind == 'astral' else 'Schatten (Anti-Leuchten)')
    col = ORE_COL[kind]
    tone = {'s': lambda c: mix(c, ES_SHADOW, 0.35)}
    for k in 'Dm':
        tone[k] = (lambda kk: lambda c: mix(col[kk], c, h['blend']))(k)
    draw_map(cv, rows, tone)
    cv.snap('Erz')
    light = {'L': lambda c: mix(col['L'], c, h['blend']), 'H': lambda c: mix(col[h['H']], c, h['blend'] * 0.6)}
    draw_map(cv, rows, light)
    return cv.snap('Glanz').img


# ---------------------------------------------------------------- W23-2 dull ores ("unrefined", fake)
# Same streak layout as the built real ore (C2), so a vein of both reads as one vein:
# F1 "dull" (built in): the crystals milky and greyed, no glint at all.
# F2 "impure": the same streaks broken into fragments, ochre-brown inclusions between them.
DULL_COL = {
    'astral': {'D': hx('5e4458'), 'm': hx('9a7c92'), 'L': hx('c2aabb'), 'H': hx('c2aabb')},
    'nihil': {'D': hx('1c2426'), 'm': hx('3c4a4c'), 'L': hx('6a7a7a'), 'H': hx('6a7a7a')},
}
OCHRE = hx('8a6a32')


def dull_ore(kind: str, variant: str, base: Image.Image) -> Image.Image:
    rows = streak_core(ORE_STREAKS[kind])
    if variant == 'F2':                       # break the streaks: every third pixel along them stays End stone
        rows = [''.join(ch if ch in '.s' or (x + y) % 3 else 's' for x, ch in enumerate(r)) for y, r in enumerate(rows)]
    cv = Canvas()
    cv.img = base.crop((0, 0, 16, 16)).convert('RGBA')
    cv.snap('Basis Endstein')
    col = DULL_COL[kind]
    tone = {'s': lambda c: mix(c, ES_SHADOW, 0.35)}
    for k in 'DmLH':
        tone[k] = (lambda kk: lambda c: mix(col[kk], c, 0.35))(k)
    draw_map(cv, rows, tone)
    cv.snap('Trübes Erz')
    if variant == 'F2':
        for x, y in ((6, 2), (12, 6), (3, 11), (10, 13), (14, 10)):
            cv.put(x, y, mix(OCHRE, cv.get(x, y), 0.3))
        cv.snap('Einschlüsse')
    return cv.img


# Dull drops (W23-2 drop proposal A, built): the own Astralit dust / Nihilit shard icons, clouded - the colours pulled
# most of the way to a grey of the same brightness, then a little towards milky white; same shape, so it reads as
# "the same material, unrefined".
ITEM = ROOT / 'src/main/resources/assets/simplebuilding/textures/item'
OVERLAY_ITEM = ROOT / 'mc26_3/overlay/resources/assets/simplebuilding/textures/item'
DULL_ITEMS = {'dull_astralit_dust': 'astralit_dust', 'dull_nihilith_shard': 'nihilith_shard'}
MILKY = hx('d8d2d6')


def dull_item(src_name: str) -> Image.Image:
    cv = Canvas()
    cv.img = Image.open(ITEM / f'{src_name}.png').convert('RGBA')
    cv.snap('Basis (echtes Material)')
    for y in range(16):
        for x in range(16):
            r, g, b, a = cv.img.getpixel((x, y))
            if a:
                lum = round(0.299 * r + 0.587 * g + 0.114 * b)
                cv.put(x, y, mix(mix((r, g, b), (lum, lum, lum), 0.6), MILKY, 0.18))
    return cv.snap('Trübung').img


def install(check: bool) -> int:
    """W23-2: writes the two dull drop icons (+ layers) into the 26.3 overlay; --check only compares."""
    bad = []
    for name, src in DULL_ITEMS.items():
        im, path = dull_item(src), OVERLAY_ITEM / f'{name}.png'
        if check:
            if not path.exists() or Image.open(path).convert('RGBA').tobytes() != im.tobytes():
                bad.append(path)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            layers.save(im, path)
    for b in bad:
        print('out of date:', b.relative_to(ROOT))
    return 1 if bad else 0


# ---------------------------------------------------------------- W24-4 brushing the hidden ores free (owner 23.10.)
# Stage 0 is the hidden C2, the freed ore is C. The two stages between reveal C pixel by pixel diagonally from the top
# left (like a brush stroke), no blending: stage 1 up to x+y < 11, stage 2 up to x+y < 21. The freed ore glimmers:
# EXPOSED_FRAMES pictures where the glow (Astral) / anti-glow shadow (Nihil) breathes 70-130 % and the full glint
# moves from crystal to crystal; frametime 6 with interpolation (the .mcmeta lives in the 26.3 overlay).
STAGE_EDGE = {1: 11, 2: 21}
EXPOSED_FRAMES = 6
ORE_NAMES = {'astral': 'astralit_ore', 'nihil': 'nihilith_ore'}


def ore_stage(kind: str, stage: int, base: Image.Image) -> Image.Image:
    hidden, free = ore_hidden(kind, ORE_PICK['real'], base), ore(kind, 'C', base)
    cv = Canvas()
    cv.img = hidden.copy()
    cv.snap('Versteckt (C2)')
    for y in range(16):
        for x in range(16):
            if x + y < STAGE_EDGE[stage]:
                cv.put(x, y, free.getpixel((x, y))[:3])
    return cv.snap(f'Freigelegt bis x+y<{STAGE_EDGE[stage]}').img


def ore_exposed(kind: str, base: Image.Image) -> Image.Image:
    strip = Image.new('RGBA', (16, 16 * EXPOSED_FRAMES))
    for k in range(EXPOSED_FRAMES):
        scale = 1.0 + 0.3 * math.cos(2 * math.pi * k / EXPOSED_FRAMES)
        strip.paste(ore(kind, 'C', base, glow_scale=scale, glint=k), (0, 16 * k))
    return strip


# Built picks (W23-1 T6, W23-2); vanilla_derived.py draws them from the client jar (End stone base).
ORE_PICK = {'real': 'C2', 'dull': 'F1'}


def derived(jar) -> dict:
    """Vanilla-derived output for tools/textures/vanilla_derived.py: the End ores on Vanilla's End stone."""
    import vn12_preview_2026_10_23 as vp
    end_stone = vp.Jar(Path(jar)).tex('block/end_stone')
    return {'block/astralit_ore.png': ore_hidden('astral', ORE_PICK['real'], end_stone),
            'block/nihilith_ore.png': ore_hidden('nihil', ORE_PICK['real'], end_stone),
            'block/dull_astralit_ore.png': dull_ore('astral', ORE_PICK['dull'], end_stone),
            'block/dull_nihilith_ore.png': dull_ore('nihil', ORE_PICK['dull'], end_stone),
            **{f'block/{ORE_NAMES[k]}_brushed_{st}.png': ore_stage(k, st, end_stone) for k in ORE_NAMES for st in STAGE_EDGE},
            **{f'block/{ORE_NAMES[k]}_exposed.png': ore_exposed(k, end_stone) for k in ORE_NAMES}}


# ---------------------------------------------------------------- T7 pistons (all six faces alike, off + active)

STONE = {  # frame stone from the polished Astralit / Nihilith family, own 10-tone ramps
    'astral': ramp(['3c1a36', '6a3060', '9a5088', 'c278b0', 'dc9ccc', 'f0c2e2', 'fce4f2'], 10),
    'nihil': ramp(['141e36', '263a64', '3e5a90', '5c7cb8', '7e9ed6', 'a6c0ea', 'd8e6fa'], 10),
}
GLOW = {'astral': LINE['astral'], 'nihil': LINE['nihil']}


def piston_frame(kind, cv, n):
    S = STONE[kind]
    for y in range(16):
        for x in range(16):
            e = edge(x, y)
            if e == 0:
                t = 3 if (x in (0, 15) and y in (0, 15)) else 5 + (n[y][x] > 0.55)
                if x == 15 or y == 15:
                    t -= 2
            elif e == 1:
                t = 7 if (x == 1 or y == 1) and x < 15 and y < 15 else 3
            else:
                t = 4 + round((n[y][x] - 0.5) * 2)
            cv.put(x, y, S[max(0, min(9, t))])


def piston_plate(kind: str, active: bool) -> Image.Image:
    """A "push plate": stone frame, a raised plate (Vanilla piston head build: plank-like courses, studs in the
    corners) and in the gutter four chevrons - Astral pointing out (pushes), Nihil pointing in (pulls); active:
    plate lighter, chevrons glow."""
    S, G = STONE[kind], GLOW[kind]
    n = fbm(71 if kind == 'astral' else 73)
    cv = Canvas()
    piston_frame(kind, cv, n)
    cv.snap('Basis Rahmen')
    lift = 1 if active else 0
    for y in range(4, 12):
        for x in range(4, 12):
            course = (y - 4) // 2
            seam = (y - 4) % 2 == 1 or x == (6 if course % 2 else 9)
            t = (6 if not seam else 4) + lift + (1 if y == 4 or x == 4 else 0) - (1 if y == 11 or x == 11 else 0)
            cv.put(x, y, S[max(0, min(9, t))])
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        cv.put(x, y, S[9 if active else 8])
    cv.snap('Platte')
    for y in range(2, 14):                    # the gutter between frame and plate, in shadow
        for x in range(2, 14):
            if edge(x, y) in (2, 3):
                cv.put(x, y, S[1 if edge(x, y) == 3 else 2])
    cv.snap('Fuge')
    # one chevron per side in the gutter: Astral tip towards the edge (pushes), Nihil tip towards the plate (pulls)
    out = kind == 'astral'
    c_tip, c_arm = (G[9], G[8]) if active else (G[6], G[5])
    for a, k in ((6, 1), (7, 0), (8, 0), (9, 1)):
        d = 2 + (k if out else 1 - k)         # gutter row 2 (outer) / 3 (inner)
        for x, y in ((a, d), (a, 15 - d), (d, a), (15 - d, a)):
            cv.put(x, y, c_tip if k == 0 else c_arm)
    return cv.snap('Pfeile').img


STAR8 = ['...xx...',
         '...yy...',
         '.z.yy.z.',
         'xyyWWyyx',
         'xyyWWyyx',
         '.z.yy.z.',
         '...yy...',
         '...xx...']


def piston_burst(kind: str, active: bool) -> Image.Image:
    """B: a deep window in the stone frame; Astral a four-pointed star whose rays run out to the window edge
    (pushes outward), Nihil a spiral of rings into a black core (pulls inward); active: brighter, wider glow."""
    S, G = STONE[kind], GLOW[kind]
    n = fbm(81)
    cv = Canvas()
    piston_frame(kind, cv, n)
    for y in range(3, 13):
        for x in range(3, 13):
            e = edge(x, y)
            cv.put(x, y, S[2] if e == 3 else G[1])
    cv.snap('Basis Rahmen + Fenster')
    boost = 1 if active else 0
    for y in range(4, 12):
        for x in range(4, 12):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if kind == 'astral':
                ch = STAR8[y - 4][x - 4]
                t = {'x': 6, 'y': 7, 'W': 9, 'z': 5, 'g': 3}.get(ch, 2 if d > 3.2 else 3) + (boost if ch != '.' else 0)
            else:
                ang = math.atan2(dy, dx)
                spiral = (d * 1.6 + ang * 1.2 / math.pi) % 2
                if d < 1.2:
                    t = 0
                elif spiral < 0.8:
                    t = min(9, 3 + int(d) + boost * 2)
                else:
                    t = 1 + (1 if d > 2.5 else 0)
            cv.put(x, y, G[max(0, min(9, t))])
    return cv.snap('Motiv').img


def piston_waves(kind: str, active: bool) -> Image.Image:
    """C: polished tiles (2x2 like polished Astralit / Nihilith) with a gem in the middle and a ring wave on the tile
    seams; the wave sits outside (Astral, pushed away) or close to the gem (Nihil, pulled in), active = lit."""
    S, G = STONE[kind], GLOW[kind]
    n = fbm(91)
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            # W24-1 (owner: "the motif in the middle is not centred"): the old seams (light at 0/8, dark at 7/15) put
            # the dark seam cross at 7,7 - half a pixel off the gem/ring centre 7.5,7.5. Now a 2 px groove (7|8 and,
            # between blocks, 15|0) with the bevel inside each tile, so the cross and the motif share the exact centre.
            lx, ly = x % 8, y % 8
            t = 6 + round((n[y][x] - 0.5) * 2)
            if lx == 1 or ly == 1:
                t = 8
            if lx == 6 or ly == 6:
                t = 5
            if lx in (0, 7) or ly in (0, 7):
                t = 3
            cv.put(x, y, S[max(0, min(9, t))])
    cv.snap('Basis Fliesen')
    r = (5.6, 6.4) if kind == 'astral' else (2.8, 3.6)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) + 0.35 * min(abs(x - 7.5), abs(y - 7.5)) / 7.5
            if r[0] - 0.5 <= d <= r[1] - 0.5:
                cv.put(x, y, G[8 if active else 4] if (x + y) % 2 == 0 or active else G[3])
    cv.snap('Welle')
    gem = ['.dd.', 'dLMd', 'dMmd', '.dd.']
    draw_map(cv, gem, {'d': G[1], 'L': G[9] if active else G[7], 'M': G[7] if active else G[5],
                       'm': G[5] if active else G[3]}, 6, 6)
    return cv.snap('Edelstein').img


# ---------------------------------------------------------------- T8 construction light

LIGHT = ramp(['3f6f86', '5b93ab', '75afc5', '89bccf', 'a5cede', 'bddce7', 'd6ecf4', 'eef8fb', 'ffffff'], 11)
FRAMES = 12
BAYER = [[0, 2], [3, 1]]


def soft(cv, x, y, v, lo=0, hi=10):
    """v (float tone) -> two neighbouring tones by a 2x2 ordered dither: soft gradients from few colours."""
    v = max(lo, min(hi, v))
    i = int(v)
    f = v - i
    cv.put(x, y, LIGHT[min(hi, i + (1 if f * 4 > BAYER[y % 2][x % 2] + 0.5 else 0))])


def light_frame_metal(cv):
    for y in range(16):
        for x in range(16):
            e = edge(x, y)
            if e == 0:
                cv.put(x, y, LIGHT[0] if (x in (0, 15) and y in (0, 15)) else LIGHT[2 if (x + y) % 4 else 1])
            elif e == 1:
                cv.put(x, y, LIGHT[5])
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        cv.put(x, y, LIGHT[8])


def construction_light(variant: str, phase: float) -> Image.Image:
    """phase -1..1 = the pulse (12 frames, frametime 4 as today). A: polished lantern - soft superellipse glow, glass
    sheen streak, rivets; B: four panes under a mullion cross, each pane glowing, light bleeding over the cross;
    C: a faceted crystal core (diamond) in a softly lit glass box."""
    cv = Canvas()
    light_frame_metal(cv)
    cv.snap('Basis Rahmen')
    g = 0.6 * phase
    for y in range(2, 14):
        for x in range(2, 14):
            dx, dy = x - 7.5, y - 7.5
            if variant == 'A':
                n = ((abs(dx) / 5.5) ** 4 + (abs(dy) / 5.5) ** 4) ** 0.25
                v = 10.4 - 7.2 * n + g
            elif variant == 'B':
                px, py = abs(dx) - 3.0, abs(dy) - 3.0
                n = ((abs(px) / 3.0) ** 3 + (abs(py) / 3.0) ** 3) ** (1 / 3)
                v = 10.2 - 4.6 * n + g - 0.8 * (math.hypot(dx, dy) / 7)
            else:
                n = (abs(dx) + abs(dy)) / 7.0
                v = 9.2 - 4.4 * n + g
            soft(cv, x, y, v, 4)
    cv.snap('Leuchten')
    if variant == 'A':
        for x, y in ((3, 5), (4, 4), (5, 3)):    # glass sheen, top left
            cv.put(x, y, LIGHT[8])
    elif variant == 'B':
        for k in (7, 8):
            for a in range(2, 14):
                c = LIGHT[6 if 5 <= a <= 10 else 5]
                cv.put(k, a, c)
                cv.put(a, k, c)
        for k in (7, 8):
            cv.put(k, k, LIGHT[8])
            cv.put(k, 15 - k, LIGHT[8])
    else:
        gem = ['...ab...', '..abbc..', '.abWWbc.', 'abWWWWbc', 'cbWWWWbd', '.cbWWbd.', '..cbbd..', '...cd...']
        hi = 10 if phase > -0.3 else 9
        draw_map(cv, gem, {'a': LIGHT[9], 'b': LIGHT[hi - 1], 'W': LIGHT[hi], 'c': LIGHT[7], 'd': LIGHT[6]}, 4, 4)
    return cv.snap('Details').img


def light_strip(variant):
    frames = [construction_light(variant, math.sin(2 * math.pi * f / FRAMES)) for f in range(FRAMES)]
    return frames


# ---------------------------------------------------------------- T9 astral vault front motif

VAULT_MOTIF = {
    # 6x6 at x 46..51, y 36..41: centred on the 14 px front (lock sits on x 48/49) and on the 6 rows the lock leaves free
    'A': ['.dMm..', 'dlLHm.', 'mLHWLm', 'MlLHlD', '.mMlmD', '..dDD.'],      # an Enderite lump like raw Enderite
    'B': ['..mm..', '.mLHm.', 'mLHWHm', 'mlHWLd', '.mlld.', '..dd..'],      # a cut gem with the Enderite block's light
    'C': ['.mllm.', 'mLHHLm', 'lHWWHl', 'lHWWHl', 'mLHHLm', '.mllm.'],      # core + a halo into the panel
}
VX, VY = 46, 36


def vault_sheet(variant: str, van: Image.Image) -> Image.Image:
    sheet = n46.game_chests(van)['astral']
    cv = Canvas(64, 64)
    cv.img = sheet.copy()
    for y in range(VY - 2, VY + 7):          # the old 5x5 motif (x 46..50, y 36..40) back to the empty socket
        for x in range(VX - 2, VX + 8):
            p = van.getpixel((x, y))
            if p[3] and n46.is_eye(p):
                cv.put(x, y, hx(n46.RAMP['astral'][0]))
    cv.snap('Basis Truhe (Vanilla-Enderkiste umgefärbt)')
    if variant == 'C':
        for y in range(VY - 2, VY + 8):
            for x in range(VX - 2, VX + 8):
                if 43 <= x <= 54 and 34 <= y <= 41:
                    d = math.hypot(x - (VX + 2.5), y - (VY + 2.5))
                    if 3.0 < d < 5.2:
                        cv.put(x, y, mix(cv.get(x, y), ENDERITE['m'], 0.45 if d < 4.2 else 0.22))
        cv.snap('Schein')
    draw_map(cv, VAULT_MOTIF[variant], {k: ENDERITE[k] for k in 'KDdmM'}, VX, VY)
    cv.snap('Motiv')
    draw_map(cv, VAULT_MOTIF[variant], {k: ENDERITE[k] for k in 'lLHW'}, VX, VY)
    return cv.snap('Leuchten').img


def vault_front(sheet: Image.Image) -> Image.Image:
    """Front view 14x14 as in game: lid front (x 42..55, y 14..18), base front below the lid (y 34..42), the lock
    front (x 4..5, y 1..4) over lid rows 3-4 and base rows 34-35 at x 6..7."""
    out = Image.new('RGBA', (14, 14))
    out.paste(sheet.crop((42, 14, 56, 19)), (0, 0))
    out.paste(sheet.crop((42, 34, 56, 43)), (0, 5))
    out.paste(sheet.crop((4, 1, 6, 5)), (6, 3))
    return out


# ---------------------------------------------------------------- page helpers

def uri(im: Image.Image, fmt='PNG') -> str:
    buf = io.BytesIO()
    im.save(buf, fmt)
    return f'data:image/{fmt.lower()};base64,' + base64.b64encode(buf.getvalue()).decode()


def gif(frames, scale, ticks=4):
    ims = [f.resize((f.width * scale, f.height * scale), Image.NEAREST).convert('RGB').convert('P', palette=Image.ADAPTIVE)
           for f in frames]
    buf = io.BytesIO()
    ims[0].save(buf, 'GIF', save_all=True, append_images=ims[1:], duration=ticks * 50, loop=0)
    return 'data:image/gif;base64,' + base64.b64encode(buf.getvalue()).decode()


def fig(im, scale, cap, cls='px'):
    return (f'<figure><img class="{cls}" src="{uri(im)}" width="{im.width * scale}" height="{im.height * scale}" '
            f'alt="{cap}"><figcaption>{cap}</figcaption></figure>')


def gfig(src, w, h, cap):
    return f'<figure><img class="px" src="{src}" width="{w}" height="{h}" alt="{cap}"><figcaption>{cap}</figcaption></figure>'


def tile(im, n=3):
    out = Image.new('RGBA', (16 * n, 16 * n))
    for y in range(n):
        for x in range(n):
            out.paste(im.crop((0, 0, 16, 16)), (16 * x, 16 * y))
    return out


def wall(center, around, n=5):
    """n x n wall of `around` with `center` in the middle (ore in an End stone wall, block in context)."""
    out = tile(around, n)
    out.paste(center.crop((0, 0, 16, 16)), (16 * (n // 2), 16 * (n // 2)))
    return out


def committed(rel: str):
    try:
        data = subprocess.run(['git', '-C', str(ROOT), 'show', f'claude-wave1:{rel}'], capture_output=True, check=True).stdout
        return Image.open(io.BytesIO(data)).convert('RGBA')
    except (subprocess.CalledProcessError, OSError):
        p = ROOT / rel
        return Image.open(p).convert('RGBA') if p.exists() else None


def candidate(img: Image.Image, path: Path):
    """PNG + .layers.json outside the repo; returns [(layer name, picture of that layer alone)]."""
    steps = layers.steps_of(img)
    final = img.convert('RGBA').tobytes()
    if not steps or steps[-1][1] != final:
        steps.append(('Nachbearbeitung', final))
    doc = layers.from_steps(img.width, img.height, steps)
    if bytes(layers.compose(doc)) != final:
        raise SystemExit(f'layers of {path.name} do not compose to the PNG')
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    layers.write_beside(path, doc)
    parts = []
    for lay in doc['layers']:
        im = Image.new('RGBA', img.size)
        for i, px in enumerate(layers.layer_pixels(doc, lay)):
            if px is not None:
                im.putpixel((i % img.width, i // img.width), tuple(px))
        parts.append((lay['name'], im))
    return parts


def layer_row(parts, scale=4):
    return '<details><summary>Ebenen</summary><div class="row">' + ''.join(fig(im, scale, n) for n, im in parts) + '</div></details>'


def radios(name, opts, rec):
    return '<div class="pick">' + ''.join(
        f'<label><input type="radio" name="{name}" value="{o}"{" checked" if o == rec else ""}> {o}</label>' for o in opts) + \
        f'<label><input type="radio" name="{name}" value="keins"> keins/neu</label></div>'


# ---------------------------------------------------------------- page

REC = {'T5': 'A', 'T6': 'B', 'T7': 'B', 'T8': 'A', 'T9': 'B'}


def drop_sketch(kind: str) -> Image.Image:
    """Sketches of the not-built drop proposals (W23-2): B slag (one grey-brown lump), C raw fragment (dull shard, dark rim)."""
    base = dull_item('astralit_dust' if kind == 'B' else 'nihilith_shard')
    cv = Canvas()
    cv.img = base.copy()
    for y in range(16):
        for x in range(16):
            r, g, b, a = cv.img.getpixel((x, y))
            if not a:
                continue
            if kind == 'B':
                lum = (r + g + b) // 3
                cv.put(x, y, mix((lum, lum, lum), hx('6a5638'), 0.45))
            elif any(not cv.img.getpixel((x + dx, y + dy))[3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                     if 0 <= x + dx < 16 and 0 <= y + dy < 16):
                cv.put(x, y, mix((r, g, b), hx('2a2024'), 0.5))
    return cv.img


def w23_page(out: str, jarp):
    """W23-1 / W23-2 (owner 23.10. evening): ONE page - what is built, and what is still to choose."""
    import vn12_preview_2026_10_23 as vp
    import end_pistons_2026_10_03 as ep
    jar = vp.Jar(jarp)
    es = jar.tex('block/end_stone')
    ender = n46.vanilla(jarp)

    def cube(im):
        return fig(vp.cube_iso(im, im, 5), 1, 'Block', 'px plain')

    def old(rel):
        im = committed(rel)
        return fig(im.crop((0, 0, 16, 16)), 6, 'alt') if im is not None else ''

    built = []
    rows = ''.join(f'<div class="row">{fig(im, 8, k)}{fig(tile(im), 2, "Wand 3×3")}{cube(im)}'
                   f'{old("mc26_3/overlay/resources/assets/simplebuilding/textures/block/" + k + "_redstone_block.png")}</div>'
                   for k in ('astral', 'nihil', 'bridge') for im in [rs_vanilla(k)])
    built.append(f'<div class="card"><h3>T5 = A Redstone-Blöcke (Vanilla-Aufbau)</h3>{rows}</div>')
    rows = ''.join(f'<div class="row">{fig(ore_hidden(k, "C2", es), 8, n)}{fig(wall(ore_hidden(k, "C2", es), es), 2, "in der Endstein-Wand")}'
                   f'{cube(ore_hidden(k, "C2", es))}{old("src/main/resources/assets/simplebuilding/textures/block/" + n + ".png")}</div>'
                   for k, n in (('astral', 'astralit_ore'), ('nihil', 'nihilith_ore')))
    built.append(f'<div class="card"><h3>T6 = C, versteckt (Stufe C2 eingebaut)</h3>{rows}<p class="note">Endstein-Basis = Vanilla → '
                 f'baut <code>vanilla_derived.py</code>, nicht im Repo. Astraliterz hat weiter Lichtstufe 5 (Mechanik, unverändert).</p></div>')
    rows = ''.join(f'<div class="row">{fig(piston_waves(k, False), 8, k + " aus")}{fig(piston_waves(k, True), 8, "an")}'
                   f'{fig(tile(piston_waves(k, False)), 2, "3×3")}{cube(piston_waves(k, False))}'
                   f'{fig(ep.variant_c(k, False), 6, "alt (Vanilla-Platte umgefärbt)")}</div>' for k in ('astral', 'nihil'))
    built.append(f'<div class="card"><h3>T7 = C Kolben (Fliesen + Wellenring), eigene Pixel</h3>{rows}</div>')
    frames = light_strip('B')
    olds = committed('src/main/resources/assets/simplebuilding/textures/block/construction_light.png')
    oldf = [olds.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(olds.height // 16)] if olds is not None else []
    built.append(f'<div class="card"><h3>T8 = B Baulicht (vier Scheiben, Puls wie bisher)</h3><div class="row">'
                 f'{gfig(gif(frames, 10), 160, 160, "pulsierend")}{gfig(gif([tile(f) for f in frames], 2), 96, 96, "Wand 3×3")}'
                 f'{cube(frames[0])}{gfig(gif(oldf, 6), 96, 96, "alt") if oldf else ""}</div></div>')
    new_sheet = n46.game_chests(ender)['astral']
    old_sheet = n46.sheet(ender, 'astral', 2, n46.PICK['astral'])
    built.append(f'<div class="card"><h3>T9 = B Astral-Gewölbe (6×6-Enderit unter dem Schloss)</h3><div class="row">'
                 f'{fig(vault_front(new_sheet), 12, "Vorderseite")}{fig(vault_front(old_sheet), 8, "alt")}</div></div>')
    # W23-2 built: dull ores F1 + dull drops A, a vein in the wall
    vein = tile(es, 5)
    for (x, y), im in {(1, 2): ore_hidden('astral', 'C2', es), (2, 2): dull_ore('astral', 'F1', es), (2, 1): ore_hidden('astral', 'C2', es),
                       (3, 3): ore_hidden('astral', 'C2', es)}.items():
        vein.paste(im, (16 * x, 16 * y))
    veinn = tile(es, 5)
    for (x, y), im in {(1, 1): ore_hidden('nihil', 'C2', es), (2, 1): ore_hidden('nihil', 'C2', es), (2, 2): dull_ore('nihil', 'F1', es),
                       (3, 2): ore_hidden('nihil', 'C2', es)}.items():
        veinn.paste(im, (16 * x, 16 * y))
    items = ''.join(fig(dull_item(src), 8, name) + fig(Image.open(ITEM / f'{src}.png').convert('RGBA'), 4, 'echt')
                    for name, src in DULL_ITEMS.items())
    built.append(f'<div class="card"><h3>W23-2 Trübe Erze (F1 eingebaut) + Drop A</h3><div class="row">'
                 f'{fig(dull_ore("astral", "F1", es), 8, "trübes Astraliterz")}{fig(dull_ore("nihil", "F1", es), 8, "trübes Nihiliterz")}'
                 f'{fig(vein, 3, "Astralit-Ader: 3 echt + 1 trüb")}{fig(veinn, 3, "Nihilit-Ader: 3 echt + 1 trüb")}</div>'
                 f'<div class="row">{items}</div><p class="note">Ader-Rechnung (Nachbildung OreFeature 26.3, 10⁶ Adern im Platzierungs-'
                 f'Umfeld): Astralit Größe 4→5, trüb 0,162 / echt 0,773 → echt 1,89 Blöcke je Ader wie vorher, trüb ¼ davon = 20 %. '
                 f'Nihilit Größe 5→6, trüb 0,194 / echt 0,966 → echt 1,58 wie vorher. Gametest misst je 600 Adern. Drop A: trüber Staub/'
                 f'Splitter (Glück wie echt, Behutsamkeit = Block), 4 → 1 echt in der Werkbank. Etwas weicher (15/20 statt 20/25), 1–3 EP, '
                 f'trübes Astralit leuchtet 3 statt 5; gleiche Werkzeugstufe (Diamant). Der Erzdetektor findet nur echtes Erz.</p></div>')

    pick = []
    pick.append(f'<div class="card"><h3>T6 Abstufung: wie versteckt?</h3>{radios("T6", ["C1", "C2"], "C2")}<div class="row">'
                + ''.join(fig(ore(k, 'C', es), 6, 'C (gewählt) ' + k) + fig(ore_hidden(k, 'C1', es), 6, 'C1 gedämpft')
                          + fig(ore_hidden(k, 'C2', es), 6, 'C2 versteckt ★') for k in ('astral', 'nihil'))
                + f'</div><div class="row">{fig(wall(ore_hidden("astral", "C1", es), es), 2, "C1 in der Wand")}'
                  f'{fig(wall(ore_hidden("astral", "C2", es), es), 2, "C2 in der Wand")}'
                  f'{fig(wall(ore_hidden("nihil", "C1", es), es), 2, "C1 Nihilit")}{fig(wall(ore_hidden("nihil", "C2", es), es), 2, "C2 Nihilit")}'
                  f'</div><p class="note">C1: Adern wie C, halber Schein, Töne ¼ zum Endstein. C2: nur der Kern jeder Ader (kein dunkler Rand, '
                  f'Spitzen weg), Töne fast halb zum Endstein, kein Schein.</p></div>')
    pick.append(f'<div class="card"><h3>Trübes Erz – Textur</h3>{radios("F", ["F1", "F2"], "F1")}<div class="row">'
                + ''.join(fig(dull_ore(k, v, es), 6, f'{v} {k}') for v in ('F1', 'F2') for k in ('astral', 'nihil'))
                + f'{fig(ore_hidden("astral", "C2", es), 6, "echt (C2)")}{fig(ore_hidden("nihil", "C2", es), 6, "echt (C2)")}</div>'
                  f'<p class="note">F1 trüb ★: gleiche Adern, milchig-grau, kein Glanz. F2 verunreinigt: Adern in Bruchstücke, ockerbraune '
                  f'Einschlüsse.</p></div>')
    pick.append(f'<div class="card"><h3>Trübes Erz – Drop</h3>{radios("D", ["A", "B", "C"], "A")}<div class="row">'
                f'{fig(dull_item("astralit_dust"), 6, "A trüber Staub")}{fig(dull_item("nihilith_shard"), 6, "A trüber Splitter")}'
                f'{fig(drop_sketch("B"), 6, "B Endschlacke (Skizze)")}{fig(drop_sketch("C"), 6, "C rohes Bruchstück (Skizze)")}</div>'
                f'<ul class="note"><li><b>A ★ trüber Staub / Splitter</b> (eingebaut): Drop wie echtes Erz, 4 → 1 echtes (Werkbank). Wert ¼, '
                f'ohne Fremdmodul.</li><li><b>B Endschlacke</b>: ein gemeinsames Item, kein Glück-Bonus, nur Brennstoff/Dünger – reine '
                f'Täuschung, kein Wert.</li><li><b>C rohes Bruchstück</b>: im Tiegel (Simple Crucibles, nur wenn geladen) 2 → 1 echtes '
                f'veredelt; ohne Tiegel wertlos.</li></ul><p class="note">Austausch = nur Loot-Tabelle + Rezept '
                f'(<code>mc26_3/overlay/resources/data/simplebuilding/loot_table/blocks/dull_*.json</code>).</p></div>')

    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SimpleBuilding Texturen W23</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mut:#5c5c62;--card:#fff;--line:#d9d7d2;--in:#1f7a3a;--cand:#9a5b00}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}}}}
:root[data-theme="dark"]{{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}}
body{{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}}
main{{max-width:1280px;margin:auto}} h1{{font-size:1.4rem}} h2{{font-size:1.15rem;margin:0 0 6px}} h3{{font-size:1rem;margin:0 0 6px}}
section{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;margin:14px 0;min-width:0}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(340px,1fr));gap:12px}} .card{{border:1px solid var(--line);border-radius:8px;padding:10px;min-width:0}}
.row{{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;margin:6px 0}}
figure{{margin:0;text-align:center;max-width:100%}} figcaption{{font-size:12px;color:var(--mut)}}
img.px{{image-rendering:pixelated;max-width:100%;height:auto;background:#8b8b8b}} img.plain{{background:transparent}}
.note{{color:var(--mut);margin:4px 0 8px}} .tag{{font-size:12px;padding:2px 6px;border-radius:6px;border:1px solid}} .in{{color:var(--in)}} .cand{{color:var(--cand)}}
.pick{{display:flex;gap:14px;flex-wrap:wrap;margin:6px 0 10px}}
#code{{position:sticky;top:0;z-index:2;background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;display:flex;gap:8px;flex-wrap:wrap;align-items:center}}
button{{font:inherit;padding:4px 10px}} code{{font-size:14px;overflow-wrap:anywhere}}
</style></head><body><main>
<h1>SimpleBuilding W23 – eingebaut und offen</h1>
<p class="note">Branch a-sbtex23b. Oben: was nach der Wahl vom 23.10. abends eingebaut ist (alter Stand daneben). Unten: was noch zu wählen
ist (★ = vorläufig eingebaut). Vanilla-Endstein nur auf dieser Seite und im Build (vanilla_derived), nie im Repo.</p>
<div id="code"><span>Antwort:</span><code id="c"></code><button id="cp">Kopieren</button></div>
<section><h2>Eingebaut <span class="tag in">im Spiel</span></h2><div class="grid">{"".join(built)}</div></section>
<section><h2>Wahl offen <span class="tag cand">vorläufig ★ eingebaut</span></h2><div class="grid">{"".join(pick)}</div></section>
</main><script>
function upd(){{const r=k=>{{const e=document.querySelector('input[name="'+k+'"]:checked');return e?e.value:'?'}};
document.getElementById('c').textContent='W23 T6='+r('T6')+' Fake='+r('F')+' Drop='+r('D');}}
document.querySelectorAll('input').forEach(e=>e.addEventListener('change',upd));upd();
document.getElementById('cp').onclick=()=>{{try{{navigator.clipboard.writeText(document.getElementById('c').textContent)}}catch(e){{}}}};
</script></body></html>'''
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    Path(out).write_text(html, encoding='utf-8')
    print('preview:', out, f'({len(html) // 1024} KB)')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out')
    ap.add_argument('--cand')
    ap.add_argument('--jar')
    ap.add_argument('--install', action='store_true', help='write the built dull drop icons (W23-2) into the overlay')
    ap.add_argument('--check', action='store_true', help='with --install: only compare')
    ap.add_argument('--w23', help='W23 page (built + open choices), e.g. /root/sb-tools-a/previews/a-sbtex23b-preview.html')
    a = ap.parse_args()
    if a.install:
        raise SystemExit(install(a.check))
    if a.w23:
        jarp = vanilla_derived.find_jar(a.jar)
        if jarp is None:
            raise SystemExit('sbtex23: needs the 26.3 client jar')
        return w23_page(a.w23, jarp)
    if not (a.out and a.cand):
        ap.error('--out and --cand are needed for the candidate page')
    jarp = vanilla_derived.find_jar(a.jar)
    if jarp is None:
        raise SystemExit('sbtex23: needs the 26.3 client jar (End stone base, vault sheet, Vanilla comparisons)')
    import vn12_preview_2026_10_23 as vp
    import end_pistons_2026_10_03 as ep
    jar = vp.Jar(jarp)
    cand = Path(a.cand)
    end_stone = jar.tex('block/end_stone')
    ender = n46.vanilla(jarp)

    def van(names, kind='block'):
        return ''.join(fig(im, 4, 'Vanilla ' + n) for n in names if (im := jar.tex(f'{kind}/{n}')) is not None)

    def cube(im, top=None):
        return fig(vp.cube_iso(top or im, im, 5), 1, 'Block', 'px plain')

    sec = []

    # T5
    cards = []
    for v, (f, title) in {'A': (rs_vanilla, 'Vanilla-Aufbau'), 'B': (rs_bevel, 'Edelstein-Facetten'),
                          'C': (rs_dust, 'Gepresster Staub')}.items():
        rows = []
        for kind in ('astral', 'nihil', 'bridge'):
            im = f(kind)
            parts = candidate(im, cand / f'T5{v}_{kind}_redstone_block.png')
            rows.append(f'<div class="row">{fig(im, 8, kind)}{fig(tile(im), 2, "Wand 3×3")}{cube(im)}</div>'
                        + layer_row(parts))
        cards.append(f'<div class="card"><h3>{v} – {title}{" ★ Empfehlung" if REC["T5"] == v else ""}</h3>{"".join(rows)}</div>')
    old = ''.join(fig(Image.open(OVERLAY / f'{k}_redstone_block.png').convert('RGBA'), 6, 'alt ' + k) for k in ('astral', 'nihil', 'bridge'))
    sec.append(f'''<section><h2>B23-T5 Astral-/Nihil-/Brücken-Redstone-Block <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Besitzer: „passen gar nicht in den Minecraft-Stil“. A folgt dem Vanilla-Redstone-Block (heller Rand, verdichteter Staub mit
Rauschen, unregelmäßig dunkler Kern), Astral mit vier Sternfunkeln, Nihil mit Leere und Gruben, Brücke rot mit Astral- und Nihil-Ader.
B nimmt die Facettenringe von Smaragd-/Diamantblock, C gepresste Staubkörner wie Glowstone/Amethyst. Je 10 Töne pro Linie, Ebenen.</p>
{radios("T5", "ABC", REC["T5"])}<div class="grid">{"".join(cards)}</div>
<div class="row">{old}</div><div class="row van">{van(["redstone_block", "emerald_block", "glowstone", "amethyst_block"])}</div></section>''')

    # T6
    cards = []
    for v, title in {'A': 'Kristall-Nester', 'B': 'Nester + Leuchten / Schatten', 'C': 'Adern + Leuchten / Schatten'}.items():
        rows = []
        for kind in ('astral', 'nihil'):
            im = ore(kind, v, end_stone)
            parts = candidate(im, cand / f'T6{v}_{"astralit" if kind == "astral" else "nihilith"}_ore.png')
            rows.append(f'<div class="row">{fig(im, 8, kind)}{fig(wall(im, end_stone), 2, "in der Endstein-Wand")}{cube(im)}</div>'
                        + layer_row(parts))
        cards.append(f'<div class="card"><h3>{v} – {title}{" ★ Empfehlung" if REC["T6"] == v else ""}</h3>{"".join(rows)}</div>')
    old = ''.join(fig(Image.open(MAIN / f'{k}_ore.png').convert('RGBA'), 6, 'alt ' + k) for k in ('astralit', 'nihilith'))
    sec.append(f'''<section><h2>B23-T6 Astralit-/Nihilit-Erz <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Basis ist der Vanilla-Endstein (Ebene „Basis Endstein“, käme wie Truhe und Kolben über <code>vanilla_derived.py</code> ins Spiel,
nie ins Repo), darüber das eigene Erz-Overlay wie bei Diamant/Smaragd (Nester) oder Netherquarz (Adern). Astralit leuchtet: kräftiges Pink mit
fast weißen Lichtpunkten, in B/C ein rosa Schein auf dem Endstein ringsum. Nihilit „Anti-Leuchten“: fast schwarze Splitter mit einem
türkisen Glanzpunkt, in B/C ein dunkler Schattenhof, der den Endstein verschluckt. (Echtes Leuchten im Dunkeln wäre Mechanik: Lichtstufe
oder emissive Ebene – nicht Teil dieses Punkts.)</p>
{radios("T6", "ABC", REC["T6"])}<div class="grid">{"".join(cards)}</div>
<div class="row">{old}</div><div class="row van">{van(["end_stone", "diamond_ore", "emerald_ore", "nether_quartz_ore"])}</div></section>''')

    # T7
    cards = []
    for v, (f, title) in {'A': (piston_plate, 'Stoßplatte mit Pfeilen'), 'B': (piston_burst, 'Sternstoß / Sog-Spirale'),
                          'C': (piston_waves, 'Fliesen mit Wellenring')}.items():
        rows = []
        for kind in ('astral', 'nihil'):
            off, on = f(kind, False), f(kind, True)
            parts = candidate(off, cand / f'T7{v}_{kind}_piston.png')
            candidate(on, cand / f'T7{v}_{kind}_piston_active.png')
            rows.append(f'<div class="row">{fig(off, 8, kind + " aus")}{fig(on, 8, "an")}{fig(tile(off), 2, "3×3")}{cube(off)}{cube(on)}</div>'
                        + layer_row(parts))
        cards.append(f'<div class="card"><h3>{v} – {title}{" ★ Empfehlung" if REC["T7"] == v else ""}</h3>{"".join(rows)}</div>')
    old_imgs = ep.derived(jarp)
    old = ''.join(fig(old_imgs[f'block/{k}.png'], 6, 'alt ' + k) for k in ('astral_piston', 'astral_piston_active', 'nihil_piston', 'nihil_piston_active') if f'block/{k}.png' in old_imgs)
    sec.append(f'''<section><h2>B23-T7 Astral-/Nihil-Kolben <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Ganz neu statt umgefärbter Vanilla-Kolbenplatte (heute C, Vanilla-abgeleitet). Der Kolben hat keine Blickrichtung und wirkt nach
allen 6 Seiten – darum zeigt jede Seite dasselbe: Astral drückt (Motiv nach außen), Nihil zieht (Motiv nach innen); „an“ = leuchtet.
Alles eigene Pixel aus der polierten Astralit-/Nihilith-Familie, also wieder normal im Repo statt über vanilla_derived.</p>
{radios("T7", "ABC", REC["T7"])}<div class="grid">{"".join(cards)}</div>
<div class="row">{old}</div><div class="row van">{van(["piston_top", "piston_side", "piston_inner"])}</div></section>''')

    # T8
    cards = []
    for v, title in {'A': 'Laterne poliert', 'B': 'Vier Scheiben', 'C': 'Kristallkern'}.items():
        frames = light_strip(v)
        parts = candidate(frames[0], cand / f'T8{v}_construction_light_frame0.png')
        strip = Image.new('RGBA', (16, 16 * FRAMES))
        for i, fr in enumerate(frames):
            strip.paste(fr, (0, 16 * i))
        strip.save(cand / f'T8{v}_construction_light.png')
        cards.append(f'<div class="card"><h3>{v} – {title}{" ★ Empfehlung" if REC["T8"] == v else ""}</h3><div class="row">'
                     f'{gfig(gif(frames, 10), 160, 160, "pulsierend 10×")}{fig(frames[0], 8, "Bild 1")}'
                     f'{gfig(gif([tile(fr) for fr in frames], 2), 96, 96, "Wand 3×3")}{cube(frames[0])}</div>{layer_row(parts)}</div>')
    olds = Image.open(MAIN / 'construction_light.png').convert('RGBA')
    oldf = [olds.crop((0, 16 * i, 16, 16 * i + 16)) for i in range(olds.height // 16)]
    sec.append(f'''<section><h2>B23-T8 Baulicht <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Gefällt schon – nur schöner nach dem Endholz-Prozess: 11 Töne statt 8, weiche Übergänge (geordnetes 2×2-Dithering statt
harter Ringe), gleicher Rahmen ringsum mit Nieten in den Ecken, gleicher Puls (12 Bilder, frametime 4 wie heute).</p>
{radios("T8", "ABC", REC["T8"])}<div class="grid">{"".join(cards)}</div>
<div class="row">{gfig(gif(oldf, 8), 128, 128, "alt (eingebaut)")}</div><div class="row van">{van(["sea_lantern", "glowstone", "redstone_lamp_on"])}</div></section>''')

    # T9
    cards = []
    for v, title in {'A': 'Enderit-Klumpen', 'B': 'geschliffen, Block-Leuchten', 'C': 'Kern mit Schein'}.items():
        sh = vault_sheet(v, ender)
        parts = candidate(sh, cand / f'T9{v}_astral_vault.png')
        cards.append(f'<div class="card"><h3>{v} – {title}{" ★ Empfehlung" if REC["T9"] == v else ""}</h3><div class="row">'
                     f'{fig(vault_front(sh), 12, "Vorderseite mit Schloss")}{fig(sh, 3, "ganzes Truhenblatt")}</div>{layer_row(parts, 2)}</div>')
    old = n46.game_chests(ender)['astral']
    lib = ROOT / 'modules/simplelib/shared/resources/assets/simplelib/textures'
    ref = ''.join(fig(Image.open(lib / p).convert('RGBA'), 4, n) for p, n in (('block/enderite_block.png', 'Enderit-Block'),
                                                                             ('item/raw_enderite.png', 'Enderit-Klumpen'),
                                                                             ('item/enderite_nugget.png', 'Enderit-Nugget')))
    sec.append(f'''<section><h2>B23-T9 Astral-Gewölbe Vorderseite <span class="tag cand">Vorschau, Wahl offen</span></h2>
<p class="note">Das blaue 5×5-Motiv (halb verschoben, Mitte auf x 48) wird ein 6×6-Enderit-Motiv bei x 46–51 / y 36–41: waagerecht genau
mittig unter dem Schloss (Schloss sitzt auf x 48/49) und senkrecht in den 6 Zeilen, die das Schloss frei lässt. Farben aus Enderit-Block und
-Klumpen (gefallen sehr). Nur das Motiv ändert sich; das Truhenblatt bleibt die umgefärbte Vanilla-Enderkiste (vanilla_derived).</p>
{radios("T9", "ABC", REC["T9"])}<div class="grid">{"".join(cards)}</div>
<div class="row">{fig(vault_front(old), 12, "alt (eingebaut)")}{ref}{fig(vault_front(ender), 8, "Vanilla Enderkiste")}</div></section>''')

    html = f'''<!doctype html><html lang="de"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>SimpleBuilding Texturen B23</title><style>
:root{{--bg:#f6f5f2;--fg:#1d1d1f;--mut:#5c5c62;--card:#fff;--line:#d9d7d2;--in:#1f7a3a;--cand:#9a5b00}}
@media (prefers-color-scheme:dark){{:root:not([data-theme="light"]){{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}}}}
:root[data-theme="dark"]{{--bg:#18181b;--fg:#ececee;--mut:#a3a3ab;--card:#232327;--line:#3a3a40;--in:#5fd38a;--cand:#f0b45a}}
body{{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif}}
main{{max-width:1280px;margin:auto}} h1{{font-size:1.4rem}} h2{{font-size:1.15rem;margin:0 0 6px}} h3{{font-size:1rem;margin:0 0 6px}}
section{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px;margin:14px 0;min-width:0}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(340px,1fr));gap:12px}} .card{{border:1px solid var(--line);border-radius:8px;padding:10px;min-width:0}}
.row{{display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;margin:6px 0}} .van figcaption{{font-style:italic}}
figure{{margin:0;text-align:center;max-width:100%}} figcaption{{font-size:12px;color:var(--mut)}}
img.px{{image-rendering:pixelated;max-width:100%;height:auto;background:#8b8b8b}} img.plain{{background:transparent}}
.note{{color:var(--mut);margin:4px 0 8px}} .tag{{font-size:12px;padding:2px 6px;border-radius:6px;border:1px solid}} .cand{{color:var(--cand)}}
.pick{{display:flex;gap:14px;flex-wrap:wrap;margin:6px 0 10px}} summary{{cursor:pointer;color:var(--mut);font-size:13px}}
#code{{position:sticky;top:0;z-index:2;background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px;display:flex;gap:8px;flex-wrap:wrap;align-items:center}}
button{{font:inherit;padding:4px 10px}} code{{font-size:14px;overflow-wrap:anywhere}}
</style></head><body><main>
<h1>SimpleBuilding-Texturen B23 (T5–T9) – exemplarisch</h1>
<p class="note">Je Punkt drei Varianten A/B/C, ★ = Empfehlung. Nichts ist eingebaut; nach der Wahl wird die Variante in den Generator übernommen
(Ebenen-Quelle .layers.json mit). Eigene Pixel in Ebenen (Basis / Overlay / Details, „Ebenen“ aufklappen); Vanilla-Bilder (kursiv) nur auf dieser Seite.</p>
<div id="code"><span>Antwort:</span><code id="c"></code><button id="cp">Kopieren</button></div>
{''.join(sec)}
</main><script>
function upd(){{const r=k=>{{const e=document.querySelector('input[name="'+k+'"]:checked');return e?e.value:'?'}};
document.getElementById('c').textContent='B23 '+['T5','T6','T7','T8','T9'].map(k=>k+'='+r(k)).join(' ');}}
document.querySelectorAll('input').forEach(e=>e.addEventListener('change',upd));upd();
document.getElementById('cp').onclick=()=>{{try{{navigator.clipboard.writeText(document.getElementById('c').textContent)}}catch(e){{}}}};
</script></body></html>'''
    Path(a.out).parent.mkdir(parents=True, exist_ok=True)
    Path(a.out).write_text(html, encoding='utf-8')
    print('preview:', a.out, f'({len(html) // 1024} KB)')


if __name__ == '__main__':
    main()
