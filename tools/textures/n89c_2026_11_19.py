"""N89C (b-n89c, 19.11.2026): owner picks K1 "Chorus-Kelch" (End tree buds) and T1 "Fallgitter" (nether brick fence gate).

Owner 19.11. (über A): "N89C: K1 Chorus-Kelch (End-Knospen, ersetzt ash_bud/dusk_bud) + T1 Fallgitter (Netherziegel-Zauntor)
-> bauen (Texturen = abgenommene Konzepte)". Ported from the accepted concept generators /root/sb-tools-b/n89c/buds.py (K1 grids
and palettes verbatim) and gate.py (t1() voxels and the own nether-brick ramp verbatim); plan /root/sb-tools-b/n89c/PLAN-BAU.md.
Own pixel art only, nothing is read from a Mojang file.

- simpleend block/ash_bud.png, dusk_bud.png: the K1 16x16 grids. The bud models stay block/cross, flower_pot_cross and
  item/generated (datagen) - the concept used exactly these. 26.3's flower_pot_cross maps the whole texture onto y4-16, the pot
  rim hides only rows 14-15 (stem foot), so crystal and calyx show in the pot.
- simpleblocks block/nether_brick_fence_gate.png: the concept's front view of the closed gate (posts in the concept brick
  courses, bars, straps, rivets). models/block/nether_brick_fence_gate{,_open,_wall,_wall_open}.json: elements built from the
  concept voxels - two posts (brick UVs) plus the door voxels greedily merged into one-coloured boxes whose faces all point
  at a 1x1 texel of that colour (so the open state needs no UV turning); open = the concept swing of the halves behind the
  posts, in a wall 3 px lower (like Vanilla). Concept x is mirrored so the rivet side seen from the north matches the concept
  picture. The blockstate loses uvlock (it would move the 1x1 texels on turned variants).

    python3 tools/textures/n89c_2026_11_19.py          write PNGs (+ layer sources), models, blockstate
    python3 tools/textures/n89c_2026_11_19.py --check  fail when a file is stale or the models no longer match the concept
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
sys.path.insert(0, str(HERE))
import layers as L  # noqa: E402

END_TEX = ROOT / 'modules/simpleend/shared/resources/assets/simpleend/textures/block'
SB = ROOT / 'modules/simpleblocks/shared/resources/assets/simpleblocks'
GATE = 'nether_brick_fence_gate'


# ------------------------------------------------------------------ K1 Chorus-Kelch (buds.py, verbatim)

def hx(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def pal(bark, wood, acc, cry):
    b, w, a, c = [list(map(hx, r)) for r in (bark, wood, acc, cry)]
    return {"o": b[1], "d": b[2], "m": b[3], "l": b[4], "h": b[5], "w": w[4],
            "A": a[0], "B": a[1], "C": a[2], "D": a[3],
            "1": c[0], "2": c[1], "3": c[2], "4": c[3], "5": c[4], "*": (255, 252, 255, 255)}


# bark/wood/accent = the woods' ramps of modules/simpleend/tools/gen_textures.py, crystal = the leaves' crystal ramp
ASH = pal("2b2724 423c37 5a534c 736b63 8e857b aaa096".split(), "7a7167 92897e a89f93 bcb3a7 cfc7bb e0d9cf".split(),
          "5e4a66 86709a b9a3cc e6d8f0".split(), "4e3f5c 7a6890 a998c4 d9cdea f6f0ff".split())
DUSK = pal("28221f 3b332d 51463d 695b4f 827263 9b8a79".split(), "6c6055 837567 988a7a ab9d8c beb09f cfc2b2".split(),
           "5a3f55 7f5a78 a9849f d6b9cd".split(), "5a3a4c 86607a b38ca4 dcbfd0 fbeef5".split())

# Segmented stem like a chorus plant (side stubs with an accent knob), on top a closed faceted crystal - the crystal heart of
# the leaves - in a calyx of accent leaves. Ash slim and tall, Dusk squatter with a drooping calyx.
K1_ASH = ["................", "................", "................", ".......5........", "......545.......", "......434.......",
          "......434.......", ".....C323C......", ".....BC2CB......", "......BmB.......", "....Bddm........", ".......d........",
          ".......mddB.....", ".......d........", "......dmd.......", "......omo......."]
K1_DUSK = ["................", "................", "................", "................", ".......5........", "......545.......",
           ".....34543......", "....C23432C.....", "...B.C232C.B....", "......BmB.......", "...Bdddm........", ".......d........",
           ".......mdddB....", ".......d........", "......dmd.......", "......omo......."]


def bud(rows, p):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    im = Image.new('RGBA', (16, 16))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != '.':
                im.putpixel((x, y), p[ch])
    return L.snap_grid(im, rows, p, [('Kelch', 'ABCD'), ('Kristall', '12345*')], base='Stiel')


# ------------------------------------------------------------------ T1 Fallgitter (gate.py, verbatim)

NB = [(22, 10, 13), (40, 19, 24), (56, 27, 33), (72, 36, 43), (92, 48, 56)]       # own nether-brick ramp (dark -> light)


def h(x, y, z, s=0):
    """Hash noise of /root/sb-tools-b/n77k2/render.py (the concept renderer)."""
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (s * 2654435761)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535


def brick(x, y, z):
    """Brick pattern in MC coords (y up): 4-high courses (mortar every 4th row), 8-wide bricks offset by 4 per course."""
    yy = (y + 1) % 4
    off = 4 if ((y + 1) // 4) % 2 else 0
    if yy == 3 or (x + z + off) % 8 == 7:
        return NB[0]
    n = h(x, y, z, 7)
    return NB[3] if yy == 2 and n > 0.3 else (NB[2] if n > 0.25 else NB[1])


class Gate:
    """Voxels in concept coords (x right, y up, z front->back). part: 'post' stays, 'L'/'R' are door halves."""
    def __init__(s):
        s.v = {}

    def box(s, x0, y0, z0, x1, y1, z1, c, part=None):
        for x in range(x0, x1):
            for y in range(y0, y1):
                for z in range(z0, z1):
                    p = part or ("post" if x < 2 or x >= 14 else ("L" if x < 8 else "R"))
                    s.v[(x, y, z)] = (c(x, y, z) if callable(c) else c, p)

    def px(s, x, y, z, c, part=None):
        s.box(x, y, z, x + 1, y + 1, z + 1, c, part)

    def state(s, open_=False):
        """{(x, y, z): (colour, part)} of the closed or open gate (concept swing of gate.py Gate.model)."""
        out = {}
        for (x, y, z), (c, p) in s.v.items():
            if open_ and p == "L":       # swings about the left post: lies along z behind it (x0-2, z9-15)
                x, z = z - 7, 9 + (x - 2)
            elif open_ and p == "R":
                x, z = 14 + (z - 7), 9 + (13 - x)
            out[(x, y, z)] = (c, p)
        return out


def posts(g, top=16):
    g.box(0, 5, 7, 2, top, 9, brick)
    g.box(14, 5, 7, 16, top, 9, brick)


def t1():
    """Fallgitter: 1px bars in a 2-px grid, 2 horizontal straps, spiked bottom points below the rail line."""
    g = Gate(); posts(g)
    for x in range(2, 14):
        if x % 2 == 1 or x in (7, 8):
            g.box(x, 4, 7, x + 1, 15, 9, lambda X, Y, Z: NB[1] if Y > 4 else NB[3])
            g.px(x, 3, 7, NB[3]) if x % 4 == 1 else None     # spike tips reach to y3 (still inside the block)
    for y in (6, 11):
        g.box(2, y, 7, 14, y + 1, 9, lambda X, Y, Z: NB[2] if X % 2 == 0 else NB[3])
    g.box(2, 14, 7, 14, 15, 9, NB[2])
    g.box(7, 4, 7, 9, 15, 9, lambda X, Y, Z: NB[3] if X == 7 else NB[2])   # meeting stiles
    for x in (3, 5, 10, 12):                                                 # rivets on strap crossings
        for y in (6, 11):
            g.px(x, y, 6, NB[4])
    return g


def front_view(vox) -> Image.Image:
    """Front-most voxel colour per (x, y), concept x to the right = the concept's front picture (gate.py views()['front'])."""
    im = Image.new('RGBA', (16, 16))
    best = {}
    for (x, y, z), (c, _) in vox.items():
        if 0 <= x < 16 and 0 <= y < 16 and ((x, y) not in best or z < best[(x, y)][0]):
            best[(x, y)] = (z, c)
    for (x, y), (_, c) in best.items():
        im.putpixel((x, 15 - y), tuple(c) + (255,))
    return im


def gate_texture(g) -> Image.Image:
    im = front_view(g.state(False))
    return L.snap(im, 'Fallgitter')


# ------------------------------------------------------------------ models

def mc(vox, dy=0):
    """Concept voxels -> model voxels: x mirrored (rivet side seen from the north = concept front picture), y shifted."""
    return {(15 - x, y + dy, z): cp for (x, y, z), cp in vox.items()}


def greedy(vox):
    """Door voxels -> non-overlapping one-coloured boxes (extend z, then x, then y) as (x0, y0, z0, x1, y1, z1, colour)."""
    left = dict(vox)
    out = []
    for key in sorted(vox, key=lambda k: (k[1], k[2], k[0])):
        if key not in left:
            continue
        c = left[key]
        x0, y0, z0 = key

        def same(x, y, z):
            return left.get((x, y, z)) == c

        z1 = z0 + 1
        while same(x0, y0, z1):
            z1 += 1
        x1 = x0 + 1
        while all(same(x1, y0, z) for z in range(z0, z1)):
            x1 += 1
        y1 = y0 + 1
        while all(same(x, y1, z) for x in range(x0, x1) for z in range(z0, z1)):
            y1 += 1
        for x in range(x0, x1):
            for y in range(y0, y1):
                for z in range(z0, z1):
                    del left[(x, y, z)]
        out.append((x0, y0, z0, x1, y1, z1, c))
    return out


DIRS = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1), 'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}


def covered(occ, box, face):
    """True when every voxel next to this face of the box is filled (face hidden inside the model)."""
    x0, y0, z0, x1, y1, z1 = box
    dx, dy, dz = DIRS[face]
    xs = [x1] if dx > 0 else [x0 - 1] if dx < 0 else range(x0, x1)
    ys = [y1] if dy > 0 else [y0 - 1] if dy < 0 else range(y0, y1)
    zs = [z1] if dz > 0 else [z0 - 1] if dz < 0 else range(z0, z1)
    return all((x, y, z) in occ for x in xs for y in ys for z in zs)


def swatch(tex: Image.Image, c) -> list:
    """UV of the middle of a texel with colour c (1x1 colour field, direction does not matter)."""
    want = tuple(c) + (255,)
    for v in range(16):
        for u in range(16):
            if tex.getpixel((u, v)) == want:
                return [u + 0.25, v + 0.25, u + 0.75, v + 0.75]
    raise ValueError(f'colour {c} not in the gate texture')


def post_element(x0, y0, dy, comment):
    """A post (model x0..x0+2, y5..16 shifted by dy, z7-9); faces show its own brick column of the texture (u = 15 - x)."""
    u0 = 14 - x0                   # texture column of model x (u = 15 - x) for the 2 px wide post
    top, bot = 0, 11               # rows y15..y5
    side = [u0, top, u0 + 2, bot]
    faces = {
        'down': {'uv': [u0, bot - 2, u0 + 2, bot], 'texture': '#texture'},
        'up': {'uv': [u0, top, u0 + 2, top + 2], 'texture': '#texture'},
        'north': {'uv': side, 'texture': '#texture'},
        'south': {'uv': side, 'texture': '#texture'},
        'west': {'uv': side, 'texture': '#texture'},
        'east': {'uv': side, 'texture': '#texture'},
    }
    faces['west' if x0 == 0 else 'east']['cullface'] = 'west' if x0 == 0 else 'east'
    return {'__comment': comment, 'from': [x0, y0 + dy, 7], 'to': [x0 + 2, 16 + dy, 9], 'faces': faces}


def model(g, tex, open_, wall):
    dy = -3 if wall else 0
    vox = mc(g.state(open_), dy)
    door = {k: c for k, (c, p) in vox.items() if p != 'post'}
    occ = set(vox)
    els = [post_element(0, 5, dy, 'Left-hand post'), post_element(14, 5, dy, 'Right-hand post')]
    for x0, y0, z0, x1, y1, z1, c in greedy(door):
        uv = swatch(tex, c)
        faces = {f: {'uv': uv, 'texture': '#texture'} for f in DIRS if not covered(occ, (x0, y0, z0, x1, y1, z1), f)}
        if faces:
            els.append({'from': [x0, y0, z0], 'to': [x1, y1, z1], 'faces': faces})
    parent = 'minecraft:block/template_fence_gate' + ('_wall' if wall else '') + ('_open' if open_ else '')
    return {'parent': parent,
            'textures': {'particle': f'simpleblocks:block/{GATE}', 'texture': f'simpleblocks:block/{GATE}'},
            'elements': els}


def model_voxels(m, tex):
    """Self check: the model JSON back to voxels {(x, y, z): colour} (posts: colour of their texture column per row)."""
    out = {}
    for e in m['elements']:
        (x0, y0, z0), (x1, y1, z1) = e['from'], e['to']
        post = '__comment' in e
        uv = next(iter(e['faces'].values()))['uv']
        for x in range(x0, x1):
            for y in range(y0, y1):
                for z in range(z0, z1):
                    if post:
                        u, v = 15 - x, 15 - (y - y0 + 5)
                    else:
                        u, v = int(uv[0]), int(uv[1])
                    out[(x, y, z)] = tex.getpixel((u, v))[:3]
    return out


def verify(g, tex, models) -> list[str]:
    """Each model reproduces the concept voxels: same cells; door colours exact; posts show the concept front bricks."""
    errs = []
    front = {(x, y): tex.getpixel((x, 15 - y))[:3] for x in range(16) for y in range(16) if tex.getpixel((x, 15 - y))[3]}
    for (open_, wall), m in models.items():
        dy = -3 if wall else 0
        want = mc(g.state(open_), dy)
        got = model_voxels(m, tex)
        if set(want) != set(got):
            errs.append(f'open={open_} wall={wall}: cells differ ({len(set(want) ^ set(got))})')
            continue
        for k, (c, p) in want.items():
            if p != 'post' and tuple(c) != got[k]:
                errs.append(f'open={open_} wall={wall}: colour at {k}')
            elif p == 'post' and got[k] != front[(15 - k[0], k[1] - dy)]:
                errs.append(f'open={open_} wall={wall}: post colour at {k}')
    return errs


def blockstate(path: Path) -> str:
    d = json.loads(path.read_text(encoding='utf-8'))
    for v in d['variants'].values():
        v.pop('uvlock', None)
    return json.dumps(d, indent=2, ensure_ascii=False) + '\n'


# ------------------------------------------------------------------ main

def outputs():
    g = t1()
    tex = gate_texture(g)
    models = {(o, w): model(g, tex, o, w) for o in (False, True) for w in (False, True)}
    pngs = {END_TEX / 'ash_bud.png': bud(K1_ASH, ASH), END_TEX / 'dusk_bud.png': bud(K1_DUSK, DUSK), SB / f'textures/block/{GATE}.png': tex}
    files = {}
    for (o, w), m in models.items():
        name = GATE + ('_wall' if w else '') + ('_open' if o else '')
        files[SB / f'models/block/{name}.json'] = json.dumps(m, indent=2) + '\n'
    bs = SB / f'blockstates/{GATE}.json'
    files[bs] = blockstate(bs)
    return g, tex, models, pngs, files


def main(argv) -> int:
    check = '--check' in argv
    g, tex, models, pngs, files = outputs()
    errs = verify(g, tex, models)
    stale = []
    for path, img in pngs.items():
        if not (path.exists() and Image.open(path).convert('RGBA').tobytes() == img.convert('RGBA').tobytes()):
            stale.append(path)
            if not check:
                L.save(img, path)
    for path, text in files.items():
        if not (path.exists() and path.read_text(encoding='utf-8') == text):
            stale.append(path)
            if not check:
                path.write_text(text, encoding='utf-8')
    for e in errs:
        print('model does not match the concept: ' + e)
    if check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
    n = sum(len(m['elements']) for m in models.values())
    print(f'n89c: {len(pngs)} textures, {len(files)} json ({n} elements in 4 models), '
          + ('stale' if stale and check else 'written' if stale else 'current') + (', CONCEPT MISMATCH' if errs else ''))
    return 1 if errs or (check and stale) else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
