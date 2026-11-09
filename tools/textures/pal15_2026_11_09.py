"""PAL15 (b-pal15, owner 09.11. ~20:30, N13a): the chiseled blocks show a carved MOTIF like Vanilla, one per family, not only a frame.

Vanilla analysis (26.3 jar, docs/ai/PLAN-B-PAL15-2026-11-09.md section 2): every Vanilla chiseled texture is a dark 1-px edge, a frame ring
lit top/left and shaded bottom/right, a slightly sunk field and one central motif (creeper, wither, spiral, piglin face, X) built from the
block's own 6-8 tones; the motif stands out by a lit upper/left edge, a darker lower/right edge and a cast shadow on the field beside it.
Hard transitions 0-9 % (resin 21 % is the outlier). This file draws the motifs as masks ('#' = raised) and derives the relief roles from
the mask, so all families share one carving rule:

    F outer edge   R frame ring top/left   S frame ring bottom/right   . sunk field
    H motif edge lit (top/left)   M motif face   E motif edge shaded (bottom/right)   C cast shadow on the field (below/right of the motif)

Three kinds of output, one rule:
  simpleblocks (Vanilla-derived, built from the jar, never committed): brightness factors per role over the Vanilla sprite
               (chiseled_2026_11_08.derived and pal14_2026_11_09.module_set call carve_vanilla()).
  simplebuilding End bricks (own art): roles -> ranks of the texture's own palette, written with .layers.json.
  simpleores metals (own art): roles -> tones of metals14 (frame of METALS14 kept, the rhombus replaced by the metal's motif).
Rounds (A8c) are parameter sets in ROUNDS, ROUND is built.

    python3.12 tools/textures/pal15_2026_11_09.py --own                write the 9 own-art textures + .layers.json
    python3.12 tools/textures/pal15_2026_11_09.py --preview OUT.html   owner page (old/new, 1x/8x/cube, Vanilla model, rounds; PAL15)
"""
from __future__ import annotations

import argparse
import base64
import io
import json
import os
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import layers  # noqa: E402

ROOT = HERE.parents[1]
SB_TEX = ROOT / 'src/main/resources/assets/simplebuilding/textures/block'
ORE_TEX = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures/block'

# ---------------------------------------------------------------- motifs ('#' raised, '.' field), centred in the 16 x 16 face

MOTIFS = {
    # simpleblocks (a-pal09 / b-pal14)
    'pickaxe': ['..######..', '.##.##.##.', '##..##..##', '#...##...#', '....##....', '....##....', '....##....', '....##....',
                '....##....', '....##....'],                                                     # andesite: the quarry stone
    'ammonite': ['..######..', '.#......#.', '#..####..#', '#.#....#.#', '#.#.##.#.#', '#.#..#.#.#', '#..##..#.#', '.#.....#.#',
                 '..#####..#', '.........#'],                                                    # diorite: fossil spiral
    'mountain': ['..........', '....#.....', '...###....', '...###..#.', '..#####.##', '..########', '.#########', '##########',
                 '##########', '..........'],                                                    # granite: two peaks
    'guardian_eye': ['#........#', '.#.####.#.', '.##....##.', '#...##...#', '#..####..#', '#..####..#', '#...##...#', '.##....##.',
                     '.#.####.#.', '#........#'],                                                # prismarine: guardian eye with spikes
    'fish': ['..........', '..........', '...####...', '.#######.#', '##.######.', '#########.', '.#######.#', '...####...',
             '..........', '..........'],                                                        # dark prismarine: fish (eye carved)
    'flame': ['....#.....', '....##....', '...###....', '...####.#.', '..#####.#.', '.###.####.', '.##...###.', '.##...##..',
              '..##..##..', '...####...'],                                                       # red nether bricks: nether fire
    'snowflake': ['#...#...#', '.#..#..#.', '..#.#.#..', '...###...', '#########', '...###...', '..#.#.#..', '.#..#..#.', '#...#...#'],
    # SimpleBuilding End palettes
    'star': ['....#....', '....#....', '...###...', '..#####..', '#########', '..#####..', '...###...', '....#....', '....#....'],
    'chorus': ['.##....##.', '.##....##.', '..#.##.#..', '..#.##.#..', '..######..', '....##....', '.##.##.##.', '..####.#..',
               '....###...', '...####...'],
    'ender_eye': ['...####...', '.##....##.', '.#..##..#.', '#...##...#', '#...##...#', '#...##...#', '#...##...#', '.#..##..#.',
                  '.##....##.', '...####...'],
    # Simple Ores metals (8 x 8 .. 10 x 10 inside the 3-px METALS14 frame)
    'moon': ['...###..', '.####...', '.###....', '###.....', '###.....', '.###....', '.####...', '...###..'],
    'bell': ['...##...', '..####..', '.######.', '.######.', '.######.', '########', '........', '...##...'],
    'bolt': ['....###.', '...###..', '..###...', '.######.', '...###..', '..###...', '.###....', '.#......'],
    'coin': ['..####..', '.######.', '##....##', '##.##.##', '##.##.##', '##....##', '.######.', '..####..'],
    'goblet': ['########', '.######.', '..####..', '...##...', '...##...', '...##...', '..####..', '.######.'],
    'gear': ['...##...', '.######.', '.##..##.', '###..###', '###..###', '.##..##.', '.######.', '...##...'],
}

# block -> (motif, kind). kind: 'vanilla' (sprite name) / 'end' / 'metal'
SIMPLEBLOCKS = {
    'chiseled_polished_andesite': ('pickaxe', 'polished_andesite'), 'chiseled_polished_diorite': ('ammonite', 'polished_diorite'),
    'chiseled_polished_granite': ('mountain', 'polished_granite'), 'chiseled_prismarine': ('guardian_eye', 'prismarine'),
    'chiseled_dark_prismarine': ('fish', 'dark_prismarine'), 'chiseled_red_nether_bricks': ('flame', 'red_nether_bricks'),
    'chiseled_ice': ('snowflake', 'ice'),
}
END = {'chiseled_astralit_bricks': 'star', 'chiseled_nihilith_bricks': 'chorus', 'chiseled_ender_quartz_bricks': 'ender_eye'}
METAL = {'silver': 'moon', 'lead': 'bell', 'zinc': 'bolt', 'nickel': 'coin', 'tin': 'goblet', 'brass': 'gear'}
# Vanilla model per family (preview only)
VANILLA_MODEL = {'pickaxe': 'chiseled_stone_bricks', 'ammonite': 'chiseled_tuff', 'mountain': 'chiseled_red_sandstone',
                 'guardian_eye': 'chiseled_sandstone', 'fish': 'chiseled_deepslate', 'flame': 'chiseled_nether_bricks',
                 'snowflake': 'chiseled_quartz_block', 'star': 'chiseled_quartz_block', 'chorus': 'chiseled_tuff_bricks',
                 'ender_eye': 'chiseled_polished_blackstone', 'moon': 'chiseled_copper', 'bell': 'chiseled_copper', 'bolt': 'chiseled_copper',
                 'coin': 'chiseled_copper', 'goblet': 'chiseled_copper', 'gear': 'chiseled_copper'}

# ---------------------------------------------------------------- rounds (A8c; verdicts in the plan, section 6)
ROUNDS = {
    # R1: first carving; factors guessed from the Vanilla ratios (frame 0.80/1.10, motif +-10 %), one depth for all sprites
    'R1': dict(f={'F': 0.80, 'R': 1.10, 'S': 0.88, '.': 0.94, 'H': 1.18, 'M': 1.08, 'E': 0.86, 'C': 0.82},
               end={'F': 0, 'R': 5, 'S': 1, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 1},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 7, 'C': 7}, deep={'ice': 1.6}, calm={}, frame=1),
    # R2: busy sprites (prismarine noise, the brick grids) calmed inside the frame, motif deeper on the polished stones;
    #     End ranks one step softer (ender quartz 41 % hard), coin with one ring, pickaxe with a curved head
    'R2': dict(f={'F': 0.82, 'R': 1.08, 'S': 0.90, '.': 0.95, 'H': 1.14, 'M': 1.06, 'E': 0.88, 'C': 0.87},
               end={'F': 1, 'R': 4, 'S': 2, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 2},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 6, 'C': 6},
               deep={'ice': 1.4, 'polished_andesite': 1.6, 'polished_diorite': 1.3, 'polished_granite': 1.6, 'prismarine': 1.8,
                     'dark_prismarine': 2.0, 'red_nether_bricks': 2.2},
               calm={'prismarine': 0.35, 'dark_prismarine': 0.30, 'red_nether_bricks': 0.25}, frame=1, thin=True),
    # R3 (built): after R2 (see the plan): the dark sprites (dark prismarine, red nether bricks) need a far deeper factor (same
    #     factor = few lum steps on a dark base), diorite calmed (its own speckle is 12.5 % hard), ice flatter (14-17 % hard),
    #     Ender quartz uses its 6 brighter tones (19 % -> lower), cast shadow one step lighter, pickaxe/snowflake a bit deeper
    'R3': dict(f={'F': 0.82, 'R': 1.08, 'S': 0.90, '.': 0.95, 'H': 1.14, 'M': 1.06, 'E': 0.88, 'C': 0.89},
               end={'F': 1, 'R': 4, 'S': 2, '.': 3, 'H': 5, 'M': 4, 'E': 2, 'C': 2},
               metal={'.': 5, 'H': 2, 'M': 3, 'E': 6, 'C': 6},
               deep={'ice': 1.25, 'polished_andesite': 1.8, 'polished_diorite': 1.1, 'polished_granite': 1.6, 'prismarine': 1.8,
                     'dark_prismarine': 2.8, 'red_nether_bricks': 3.6},
               calm={'prismarine': 0.35, 'dark_prismarine': 0.30, 'red_nether_bricks': 0.25, 'polished_diorite': 0.3}, frame=1, thin=True, end6=True),
}
# Motif shapes of R1 that were redrawn later (R2+ use MOTIFS)
MOTIFS_R1 = {'coin': ['..####..', '.#....#.', '#..##..#', '#.#..#.#', '#.#..#.#', '#..##..#', '.#....#.', '..####..'],
             'pickaxe': ['.########.', '#...##...#', '#...##...#', '....##....', '....##....', '....##....', '....##....', '....##....',
                         '....##....', '...####...']}
ROUND = 'R3'


def roles(motif: str, frame: int = 1, rnd: str = ROUND) -> list[str]:
    """16 role rows: frame rings (0 = F, 1..frame = R/S) + the motif mask centred with its relief roles."""
    m = MOTIFS_R1[motif] if rnd == 'R1' and motif in MOTIFS_R1 else MOTIFS[motif]
    h, w = len(m), len(m[0])
    y0, x0 = (16 - h) // 2, (16 - w) // 2
    raised = [[False] * 16 for _ in range(16)]
    for y in range(h):
        for x in range(w):
            raised[y0 + y][x0 + x] = m[y][x] == '#'
    out = []
    for y in range(16):
        row = ''
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                row += 'F'
            elif ring <= frame:
                row += 'R' if (x == ring or y == ring) and x + y < 15 else 'S'
            elif raised[y][x]:
                lit = not (raised[y - 1][x] and raised[y][x - 1])      # open to the light (top/left)
                dark = not (raised[y + 1][x] and raised[y][x + 1])     # open to the shade (bottom/right)
                if ROUNDS[rnd].get('thin'):
                    row += 'H' if lit and not dark else 'E' if dark and not lit else 'M'   # 1-px lines stay face, the cast shadow carries them
                else:
                    row += 'H' if lit else 'E' if dark else 'M'   # R1: every edge pixel lit first (1-px lines turned all-highlight)
            elif raised[y - 1][x] or raised[y][x - 1]:
                row += 'C'
            else:
                row += '.'
        out.append(row)
    return out


def hard_share(a: np.ndarray, thr: int = 40) -> float:
    lum = 0.299 * a[..., 0] + 0.587 * a[..., 1] + 0.114 * a[..., 2]
    h = (np.abs(np.diff(lum, axis=0)) > thr).sum() + (np.abs(np.diff(lum, axis=1)) > thr).sum()
    return float(h) / (16 * 15 * 2)


# ---------------------------------------------------------------- Vanilla-derived (simpleblocks)

def vanilla(jar, sprite: str) -> np.ndarray:
    with zipfile.ZipFile(jar) as z:
        im = Image.open(io.BytesIO(z.read(f'assets/minecraft/textures/block/{sprite}.png'))).convert('RGBA')
    return np.array(im.crop((0, 0, 16, 16)))


def carve_vanilla(a: np.ndarray, motif: str, sprite: str = '', rnd: str = ROUND) -> np.ndarray:
    """Brightness factor per role over the Vanilla pixels (relief, not paint). Calm sprites (ice) are carved deeper; busy sprites
    (prismarine noise, brick grids) get their contrast inside the frame pulled toward the median first (calm), like the quiet centre
    of the Vanilla chiseled nether bricks / quartz, so the motif is not lost in the pattern. No blur: one value per pixel."""
    p = ROUNDS[rnd]
    deep = next((v for k, v in p['deep'].items() if k == sprite or (k == 'ice' and 'ice' in sprite)), 1.0)
    grid = roles(motif, p['frame'], rnd)
    f = np.array([[1 + (p['f'][ch] - 1) * deep for ch in row] for row in grid])
    src = a.astype(float)
    if sprite in p['calm']:
        k = p['calm'][sprite]
        med = np.median(src[..., :3].reshape(-1, 3), axis=0)
        inner = np.array([[ch != 'F' for ch in row] for row in grid])
        src[inner, :3] = med + (src[inner, :3] - med) * k
    out = src.copy()
    out[..., :3] = np.clip(src[..., :3] * f[..., None], 0, 255)
    out = np.rint(out).astype(np.uint8)
    if 'ice' in sprite:  # the motif is denser ice: its alpha rises like the frame of the chiseled packed ice
        alpha = out[..., 3].astype(int)
        solid = np.array([[ch in 'FRSHE' for ch in row] for row in grid])
        alpha[solid] = np.minimum(255, alpha[solid] + 70)
        out[..., 3] = alpha.astype(np.uint8)
    return out


def simpleblocks(jar, rnd: str = ROUND) -> dict:
    return {f'block/{name}.png': carve_vanilla(vanilla(jar, sprite), motif, sprite, rnd) for name, (motif, sprite) in SIMPLEBLOCKS.items()}


# ---------------------------------------------------------------- own art: End bricks

# The palettes of the End bricks before PAL15 (the PNG/.layers.json are rewritten by --own, so they are kept here).
END_PALETTE = {'chiseled_astralit_bricks': ['#f5cdea', '#bb62a2', '#edb8df', '#cc77b4', '#d98cc4', '#e4a2d2'],
               'chiseled_nihilith_bricks': ['#c3d5f2', '#5b78b8', '#adc3e9', '#6e8bc8', '#829ed5', '#97b1e0'],
               'chiseled_ender_quartz_bricks': ['#bfa2d8', '#5f3f80', '#ab8aca', '#714f96', '#8461aa', '#9774bb', '#4e3269']}
ORIG_END = {n: sorted([tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,) for c in cols],
                      key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) for n, cols in END_PALETTE.items()}


def end_texture(name: str, rnd: str = ROUND) -> Image.Image:
    """Roles -> rank in the texture's own palette (darkest 0); ranks are given for 6 colours and scaled to the palette length."""
    pal = ORIG_END[name]
    p = ROUNDS[rnd]
    grid = roles(END[name], p['frame'], rnd)
    if p.get('end6'):
        pal = pal[-6:]   # Ender quartz has a 7th, very dark tone (65 lum): its steps are 20-23 lum wide, the 6 brighter ones match the others
    ranks = p['end']
    def col(ch):
        r = ranks[ch] * (len(pal) - 1) / 5
        return tuple(pal[int(round(r))])
    im = Image.new('RGBA', (16, 16))
    for name_l, keep in (('Grundfläche', '.'), ('Rahmen', '.FRS'), ('Motiv', '.FRSM'), ('Relief', '.FRSMHEC')):
        for y in range(16):
            for x in range(16):
                ch = grid[y][x]
                im.putpixel((x, y), col(ch if ch in keep else '.'))
        layers.snap(im, name_l)
    return im


# ---------------------------------------------------------------- own art: metals (frame of METALS14)

def metal_texture(metal: str, rnd: str = ROUND) -> Image.Image:
    import metals14_2026_11_09 as m14
    import ores_2026_10_30 as ores
    ramp = ores.RAMPS[m14.METALS[metal]]
    lo, hi = m14.SPANS[m14.ROUND][metal]
    def colour(t):
        return ores.ramp_at(ramp, (lo + (hi - lo) * t / 9.0) / 9.0) + (255,)
    base, edge, _ = m14.chiseled(4)
    p = ROUNDS[rnd]
    grid = roles(METAL[metal], 3, rnd)   # METALS14 frame is 3 px (edge, ring, sunk step); the motif sits in the 10 x 10 field inside
    motif = [[p['metal'][grid[y][x]] if grid[y][x] in 'HMEC' else None for x in range(16)] for y in range(16)]
    im = Image.new('RGBA', (16, 16))
    for name, raster in (('Grundfläche', base), ('Kanten', edge), ('Motiv', motif)):
        for y in range(16):
            for x in range(16):
                if raster[y][x] is not None:
                    im.putpixel((x, y), colour(raster[y][x]))
        layers.snap(im, name)
    return im


def own(rnd: str = ROUND) -> dict:
    out = {SB_TEX / f'{n}.png': end_texture(n, rnd) for n in END}
    out.update({ORE_TEX / f'chiseled_{m}.png': metal_texture(m, rnd) for m in METAL})
    return out


# ---------------------------------------------------------------- preview

def find_jar(explicit=None):
    for c in (explicit, os.environ.get('SB_CLIENT_JAR'), Path.home() / '.gradle/caches/fabric-loom/26.3/minecraft-client.jar'):
        if c and Path(c).exists():
            return Path(c)
    sys.exit('no Minecraft client jar found (--jar)')


def _uri(im: Image.Image, k: int) -> str:
    buf = io.BytesIO()
    im.convert('RGBA').resize((16 * k, 16 * k), Image.NEAREST).save(buf, 'PNG')
    return 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()


def cube(im: Image.Image, k: int = 6) -> Image.Image:
    """Isometric block (top, left 80 %, right 62 % brightness like the inventory render), nearest-neighbour."""
    t = np.array(im.convert('RGBA').resize((16 * k, 16 * k), Image.NEAREST)).astype(float)
    s = 16 * k
    W, H = 2 * s, 2 * s
    out = np.zeros((H, W, 4), np.uint8)
    for y in range(H):
        for x in range(W):
            # top face (diamond): x = s + (u - v), y = (u + v) / 2  ->  u, v in [0, s)
            u = (y + (x - s) / 2); v = (y - (x - s) / 2)
            if 0 <= u < s and 0 <= v < s and y < s:
                out[y, x] = np.clip(t[int(v), int(u)], 0, 255)
                continue
            # left face
            if x < s:
                ty = y - (s / 2) - x / 2
                if 0 <= ty < s:
                    px = t[int(ty), int(x)] * [0.8, 0.8, 0.8, 1]
                    out[y, x] = np.clip(px, 0, 255)
                    continue
            else:
                xx = x - s
                ty = y - s + xx / 2
                if 0 <= ty < s and xx < s:
                    px = t[int(ty), int(xx)] * [0.62, 0.62, 0.62, 1]
                    out[y, x] = np.clip(px, 0, 255)
    return Image.fromarray(out, 'RGBA')


def preview(jar: Path, path: Path) -> None:
    import chiseled_2026_11_08 as c8
    import metals14_2026_11_09 as m14
    import pal14_2026_11_09 as p14
    rows = []
    old_sb = {f'block/{n}.png': Image.fromarray(c8.look_a(c8.vanilla(jar, s)), 'RGBA') for n, s in c8.SPRITES.items()}
    old_sb['block/chiseled_ice.png'] = Image.fromarray(p14.chiseled(p14.vanilla(jar, 'ice'), p14.ROUNDS[p14.ROUND]), 'RGBA')
    entries = []
    for name, (motif, sprite) in SIMPLEBLOCKS.items():
        new = {r: Image.fromarray(carve_vanilla(vanilla(jar, sprite), motif, sprite, r), 'RGBA') for r in ROUNDS}
        entries.append((name, motif, 'Simple Blocks (Vanilla-abgeleitet)', old_sb[f'block/{name}.png'], new))
    for name, motif in END.items():
        old = Image.open(SB_TEX / f'{name}.png').convert('RGBA')
        if name in OLD_PNG:
            old = OLD_PNG[name]
        entries.append((name, motif, 'SimpleBuilding End (eigene Kunst)', old, {r: end_texture(name, r) for r in ROUNDS}))
    for metal, motif in METAL.items():
        old = m14.paint(metal, 'chiseled')
        entries.append((f'chiseled_{metal}', motif, 'Simple Ores Metalle (eigene Kunst)', old, {r: metal_texture(metal, r) for r in ROUNDS}))
    for name, motif, group, old, new in entries:
        van = Image.fromarray(vanilla(jar, VANILLA_MODEL[motif]), 'RGBA')
        built = new[ROUND]
        rounds = ''.join(f'<figure><img class=s src="{_uri(im, 6)}" alt=""><figcaption>{r} · hart {hard_share(np.array(im.convert("RGBA"))):.0%}</figcaption></figure>'
                         for r, im in new.items())
        tiled = Image.new('RGBA', (48, 48))
        for ty in range(3):
            for tx in range(3):
                tiled.paste(built, (tx * 16, ty * 16))
        buf = io.BytesIO(); tiled.resize((144, 144), Image.NEAREST).save(buf, 'PNG')
        tile_uri = 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()
        cbuf = io.BytesIO(); cube(built, 4).save(cbuf, 'PNG')
        cube_uri = 'data:image/png;base64,' + base64.b64encode(cbuf.getvalue()).decode()
        rows.append(f'''<section data-id="{name}"><h3>{name.replace('_', ' ')} <small>– Motiv <b>{MOTIF_DE[motif]}</b> · {group}</small></h3>
<p class=why>{WHY[motif]}</p>
<div class=row>
<figure><img src="{_uri(old, 8)}" alt=""><figcaption>alt (8x)</figcaption></figure>
<figure><img src="{_uri(built, 8)}" alt=""><figcaption>neu {ROUND} (8x) · hart {hard_share(np.array(built.convert("RGBA"))):.0%}</figcaption></figure>
<figure><img class=one src="{_uri(built, 1)}" alt=""><img class=one src="{_uri(old, 1)}" alt=""><figcaption>1x neu / alt</figcaption></figure>
<figure><img src="{tile_uri}" alt=""><figcaption>3×3 gekachelt</figcaption></figure>
<figure><img src="{cube_uri}" alt="" style="width:128px;height:128px"><figcaption>Würfel</figcaption></figure>
<figure><img src="{_uri(van, 8)}" alt=""><figcaption>Vanilla-Vorbild<br>{VANILLA_MODEL[motif]}</figcaption></figure>
</div>
<details><summary>Runden R1–R3</summary><div class=row>{rounds}</div></details>
<div class=pick><button onclick="pick(this,'ok')">ok</button><button onclick="pick(this,'einwand')">Einwand</button>
<input placeholder="Einwand / Wunsch-Motiv" oninput="upd()"></div></section>''')
    html = f"""<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>b-pal15: Gemeißelte Blöcke mit Motiv</title>
<style>:root{{--bg:#f2efe6;--fg:#1c1e21;--card:#fff;--ac:#2f6b3a;--mut:#555}}@media(prefers-color-scheme:dark){{:root{{--bg:#1d1f22;--fg:#e8e6df;--card:#2a2d31;--ac:#7fc08a;--mut:#b8b5ad}}}}
body{{font:16px/1.5 system-ui,sans-serif;margin:0;padding:1rem;background:var(--bg);color:var(--fg);max-width:1100px}}h1{{font-size:1.4rem}}
section{{background:var(--card);padding:.8rem 1rem;border-radius:8px;margin-bottom:1rem}}h3{{margin:.2rem 0;font-size:1.05rem}}small{{color:var(--mut);font-weight:normal}}
.row{{display:flex;flex-wrap:wrap;gap:.6rem;align-items:flex-end}}figure{{margin:0;text-align:center}}img{{width:128px;height:128px;image-rendering:pixelated;display:block}}
img.s{{width:96px;height:96px}}img.one{{width:16px;height:16px;display:inline-block;margin:0 4px}}figcaption{{font-size:.75rem;max-width:150px;color:var(--mut)}}
.why{{margin:.2rem 0 .5rem;font-size:.9rem}}button{{margin:.4rem .4rem 0 0;padding:.5rem .9rem;font-size:1rem;border:2px solid var(--ac);border-radius:6px;background:transparent;color:var(--fg);cursor:pointer}}
button.on{{background:var(--ac);color:var(--bg)}}input{{padding:.45rem;font-size:.95rem;min-width:240px;border-radius:6px;border:1px solid var(--mut);background:var(--card);color:var(--fg)}}
#code{{position:sticky;bottom:0;background:var(--card);padding:.7rem 1rem;border-radius:8px;border:2px solid var(--ac);word-break:break-all}}</style>
<h1>b-pal15: gemeißelte Blöcke mit Motiv (PAL15)</h1>
<p>Besitzer 09.11.: „gemeißelte Varianten mit MOTIV wie Vanilla – je Familie ein passendes eigenes Motiv, nicht nur Rahmen“. 16 Blöcke:
7 Simple Blocks (Pixel aus Vanilla, beim Bauen erzeugt), 3 End-Ziegel (SimpleBuilding), 6 Metalle (Simple Ores). Je Block: alt, neu (eingebaut {ROUND}),
1x, gekachelt, Würfel, Vanilla-Vorbild; Runden aufklappbar. Gemeißeltes Pack-/Blaueis behält sein abgenommenes Motiv (B23-T3 B: Kristall/Raute).
Aufbau wie Vanilla: dunkle Kante, Rahmenring oben/links hell und unten/rechts dunkel, leicht vertieftes Feld, erhabenes Motiv mit Lichtkante oben/links,
Schattenkante unten/rechts und Schlagschatten aufs Feld. „hart“ = Anteil harter Übergänge (A8b ~7 %, Relief bis ~12 % bewusst).</p>
<h2>Rundenverlauf</h2><ul>{''.join(f'<li><b>{r}</b>: {t}</li>' for r, t in ROUND_NOTES.items())}</ul>
{''.join(rows)}
<p id=code>Antwortcode: <code id=c>PAL15</code></p>
<script>function pick(b,v){{var s=b.closest('section');s.querySelectorAll('button').forEach(x=>x.classList.remove('on'));b.classList.add('on');s.dataset.v=v;upd()}}
function upd(){{var p=[];document.querySelectorAll('section').forEach(s=>{{if(!s.dataset.v)return;var t=s.querySelector('input').value.trim();p.push(s.dataset.id.replace('chiseled_','')+'='+s.dataset.v+(t?'('+t+')':''))}});
var c='PAL15'+(p.length?' '+p.join(' '):'');document.getElementById('c').textContent=c;try{{navigator.clipboard.writeText(c)}}catch(e){{}}}}</script></html>"""
    path.write_text(html, encoding='utf-8')
    print(f'wrote {path}')


OLD_PNG: dict = {}
ROUND_NOTES = {
    'R1': 'Erstentwurf, eine Tiefe für alle: auf Prismarin/Dunklem Prismarin/Roten Netherziegeln geht das Motiv im Muster unter, '
          'Spitzhacke kaum lesbar, 1-px-Linien werden ganz zur Lichtkante (Enderauge/Chorus grell, Enderquarz 41 % hart).',
    'R2': 'Unruhige Sprites innen beruhigt (Kontrast zum Median auf 25–35 %), Motiv je Stein tiefer, 1-px-Linien als Fläche mit '
          'Schlagschatten, End-Stufen weicher, Münze mit einem Ring, Spitzhacke mit gebogenem Kopf. Rest: dunkle Sprites (Netherziegel, '
          'Dunkler Prismarin) zu flach, Diorit 29 % hart, Eis 17 %.',
    'R3': 'Eingebaut. Dunkle Sprites deutlich tiefer (Faktor 2,8/3,6), Diorit beruhigt, Eis flacher (9 %), Enderquarz auf seinen 6 hellen '
          'Tönen (8 %). Alle Motive in 1x erkennbar; Härte 0–12 %, Ausnahmen Diorit 18 % und Prismarin 26 % (deren Vanilla-Grundsprite hat '
          'schon 12,5 % bzw. 28 %).',
}
MOTIF_DE = {'pickaxe': 'Spitzhacke', 'ammonite': 'Ammonit-Spirale', 'mountain': 'Berggipfel', 'guardian_eye': 'Wächter-Auge', 'fish': 'Fisch',
            'flame': 'Netherflamme', 'snowflake': 'Schneeflocke', 'star': 'Stern', 'chorus': 'Chorus-Pflanze', 'ender_eye': 'Enderauge',
            'moon': 'Mondsichel', 'bell': 'Glocke', 'bolt': 'Blitz', 'coin': 'Münze', 'goblet': 'Zinnkelch', 'gear': 'Zahnrad'}
WHY = {
    'pickaxe': 'Andesit ist der Steinbruch-Stein der Höhlen: Spitzhacke als Werkzeug des Steinmetzes (Gegenstück zur Säule/Spirale von Stein/Tuff).',
    'ammonite': 'Diorit: heller, gesprenkelter Stein – eingemeißeltes Fossil (Ammonit-Spirale), runder als die eckige Tuff-Spirale.',
    'mountain': 'Granit = Gebirgsgestein: zwei Gipfel, erhaben gemeißelt.',
    'guardian_eye': 'Prismarin = Ozeanmonument: das Auge des Wächters mit Stacheln (Besitzer-Beispiel).',
    'fish': 'Dunkler Prismarin: Fisch (Besitzer-Beispiel), Auge eingetieft – unterscheidet sich klar vom Wächter-Auge des hellen Prismarins.',
    'flame': 'Rote Netherziegel: Netherfeuer, Hohlform innen – passt zum Nether ohne das Gesicht der Vanilla-Netherziegel zu kopieren.',
    'snowflake': 'Eis: Schneeflocke, durch die Meißelung dichteres (weniger durchsichtiges) Eis; Pack-/Blaueis behalten Kristall/Raute.',
    'star': 'Astralit (astral, leuchtend): vierzackiger Stern.',
    'chorus': 'Nihilith (die blaue End-Art): verzweigte Chorus-Pflanze des Endes.',
    'ender_eye': 'Enderquarz: Enderauge mit senkrechter Pupille (Ender = Auge).',
    'moon': 'Silber: in der Alchemie das Metall des Mondes – Mondsichel.',
    'bell': 'Blei dämpft Schall (Schallschutz in Simple Ores): Glocke.',
    'bolt': 'Zink: Batterie-/Galvanik-Metall – Blitz.',
    'coin': 'Nickel: Münzmetall – Münze mit Rand und Mitte.',
    'goblet': 'Zinn: Zinngeschirr – Kelch.',
    'gear': 'Messing: Uhrwerk-Messing – Zahnrad.',
}


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument('--own', action='store_true')
    ap.add_argument('--preview')
    ap.add_argument('--old', help='directory with the old own-art PNGs (preview: alt column)')
    ap.add_argument('--jar')
    a = ap.parse_args()
    if a.old:
        for n in END:
            p = Path(a.old) / f'{n}.png'
            if p.exists():
                OLD_PNG[n] = Image.open(p).convert('RGBA')
    if a.own:
        for p, im in own().items():
            layers.save(im, p)
        print(f'pal15: {len(END) + len(METAL)} own textures + layers written')
    if a.preview:
        preview(find_jar(a.jar), Path(a.preview))


if __name__ == '__main__':
    main()
