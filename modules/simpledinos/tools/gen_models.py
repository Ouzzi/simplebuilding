"""Simple Dinosaurs: the T. rex model, its textures and the Java layer class from ONE spec (concept docs/ai/KONZEPT-A-WP18-2026-10-29.md).

Own pixel art drawn from code, nothing copied from Vanilla. Every custom model (deer, boar, hedgehog, crab) is a list
of parts/boxes below; this script packs the boxes into the texture (shelf packing), paints them (soft shading, about
7 % hard transitions, A8b), writes the Java `LayerDefinition` classes (client/*Layers.java) and the PNGs. The zebra
and the zorse use the Vanilla horse model, so only their texture is painted, on the Vanilla horse UV layout.

    python3 modules/simpledinos/tools/gen_models.py            write textures + Java layers
    python3 modules/simpledinos/tools/gen_models.py --check    fail when a file is stale
    python3 modules/simpledinos/tools/gen_models.py --preview <dir>   also write the preview data (json + pngs)
"""
from __future__ import annotations

import argparse
import json
import math
import sys
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
sys.path.insert(0, str(ROOT / 'tools/textures'))
import layers as L  # noqa: E402

TEX = MODULE / 'shared/resources/assets/simpledinos/textures/entity'
JAVA = MODULE / 'shared/java/com/simpledinos/client'


# ---------------------------------------------------------------- colour helpers
def H(s):
    s = s.lstrip('#')
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def hsh(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


FACE_LIGHT = {'top': 1.10, 'front': 1.0, 'right': 0.94, 'left': 0.94, 'back': 0.88, 'bottom': 0.78}


def lit(c, face, noise=0.0, k=0.0):
    f = FACE_LIGHT[face] * (1.0 + (noise - 0.5) * 2 * k)
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


def solid(c, k=0.05, seed=3):
    return lambda face, x, y, w, h: lit(c, face, hsh(x, y, seed + len(face)), k)


def with_marks(inner, marks):
    """marks: {(face, x, y): rgb} applied after the inner painter (eyes, nose, ...)."""
    def f(face, x, y, w, h):
        return marks.get((face, x, y), inner(face, x, y, w, h))
    return f


# ---------------------------------------------------------------- painters
def skin(base, belly, dark, seed, k=0.05, belly_from=0.6):
    """Dinosaur hide: darker back, lighter belly, 2 px blotches and soft bands on top and sides."""
    def f(face, x, y, w, h):
        t = y / max(1, h - 1) if face not in ('top', 'bottom') else 0.0
        c = base
        blotch = hsh(x // 2, y // 2, seed)
        if blotch > 0.7:
            c = mix(c, dark, 0.28)
        elif blotch < 0.12:
            c = mix(c, belly, 0.14)
        band = (x // 3 + (y // 4) * 2) % 5 == 0
        if face in ('top', 'right', 'left', 'back') and band and hsh(x, y, seed + 1) > 0.35:
            c = mix(c, dark, 0.32)
        if face == 'top':
            c = mix(c, dark, 0.3)
        if face != 'top' and t > belly_from:
            c = mix(c, belly, min(1.0, (t - belly_from) / (1 - belly_from) * 1.5))
        if face == 'bottom':
            c = belly
        return lit(c, face, hsh(x, y, seed + len(face)), k)
    return f


def jawed(inner, teeth, eye=None, lower=False):
    """Skull or jaw: a row of teeth along the bottom (upper jaw) or top (lower jaw) of the side faces, eyes on the skull."""
    def f(face, x, y, w, h):
        if face in ('right', 'left', 'front') and ((y >= h - 2) if not lower else (y <= 1)):
            if x % 2 == 0:
                return lit(teeth, face, 0.5, 0.0)
        if eye is not None and not lower and face in ('right', 'left') and (x, y) == (w - 6, 3):
            return eye
        return inner(face, x, y, w, h)
    return f


def clawed(inner, claw):
    def f(face, x, y, w, h):
        if face in ('front', 'right', 'left') and y >= h - 2:
            return lit(claw, face, 0.5, 0.0)
        if face == 'bottom':
            return lit(claw, face, 0.5, 0.0)
        return inner(face, x, y, w, h)
    return f


TIER_PALETTES = {1: ('#7a5030', '#9a6a40', '#4e321c', 'leather'), 2: ('#9aa0a8', '#c4c9d0', '#5c6168', 'iron'),
                 3: ('#3fd2c6', '#8ff0e6', '#1f8a82', 'diamond')}


def plate(tier, seed):
    """Armor plate: leather stitches, iron rivets, diamond facets; always a darker rim."""
    base, light, dark, kind = TIER_PALETTES[tier]
    base, light, dark = H(base), H(light), H(dark)

    def f(face, x, y, w, h):
        c = base
        edge = x in (0, w - 1) or y in (0, h - 1)
        if edge:
            c = dark
        elif kind == 'leather' and (x + y) % 4 == 0:
            c = light
        elif kind == 'iron' and x % 5 == 2 and y % 5 == 2:
            c = light
        elif kind == 'diamond' and (x + y) % 3 == 0:
            c = light
        return lit(c, face, hsh(x, y, seed), 0.04)
    return f


# ---------------------------------------------------------------- the spec
def B(pos, size, paint, grow=0.0, uv=None, mirror=False, share=None):
    return dict(pos=pos, size=size, paint=paint, grow=grow, uv=uv, mirror=mirror, share=share)


def P(name, boxes=(), pose=(0, 0, 0), rot=(0, 0, 0), parent=None):
    return dict(name=name, boxes=list(boxes), pose=pose, rot=rot, parent=parent)


def Y(h):
    """Model y (down) of a point h pixels above the ground (the ground is at y = 24)."""
    return 24.0 - h


def trex_spec(coat, belly, dark, tier=0, name='trex'):
    """Tyrannosaurus rex, 1 px = 1/16 block: about 10.8 m from snout to tail tip, 4.7 m to the top of the head and 3.7 m at the
    hips (concept 3.4). Saddle and armor parts are always in the layout (same UVs for every variant); the model hides them
    when they are not worn. `tier` paints the armor plates (0 = hidden placeholder)."""
    body_p = skin(coat, belly, dark, 11)
    tail_p = skin(coat, belly, dark, 12, belly_from=0.75)
    neck_p = skin(coat, belly, dark, 13, belly_from=0.7)
    leg_p = skin(coat, belly, dark, 14, belly_from=0.9)
    foot_p = clawed(skin(coat, belly, dark, 15, belly_from=1.1), H('e8dcc0'))
    arm_p = clawed(skin(coat, belly, dark, 16, belly_from=1.1), H('e8dcc0'))
    skull_p = jawed(skin(coat, belly, dark, 17, belly_from=0.7), H('f0e8d0'), eye=H('e8b020'))
    jaw_p = jawed(skin(coat, belly, belly, 18, belly_from=0.2), H('f0e8d0'), lower=True)
    seat_p = solid(H('6a4024'), 0.06, 20)
    pommel_p = solid(H('8a8f96'), 0.05, 21)
    plate_p = plate(max(1, tier), 22)
    parts = [
        P('body', [B((-14, -15, -24), (28, 30, 48), body_p, share='body')], pose=(0, Y(48), 0)),
        P('neck', [B((-8, -24, -9), (16, 24, 18), neck_p, share='neck')], pose=(0, -11, -20), rot=(0.5, 0, 0), parent='body'),
        P('head', [B((-8, -8, -28), (16, 16, 30), skull_p, share='head')], pose=(0, -23, -2), rot=(-0.5, 0, 0), parent='neck'),
        P('jaw', [B((-6.5, 0, -26), (13, 5, 26), jaw_p, share='jaw')], pose=(0, 4, -2), rot=(0.12, 0, 0), parent='head'),
        P('tail1', [B((-10, -10, 0), (20, 20, 28), tail_p, share='tail1')], pose=(0, -3, 22), rot=(-0.03, 0, 0), parent='body'),
        P('tail2', [B((-7, -8, 0), (14, 16, 28), tail_p, share='tail2')], pose=(0, 0, 28), rot=(0.04, 0, 0), parent='tail1'),
        P('tail3', [B((-4, -5, 0), (8, 10, 28), tail_p, share='tail3')], pose=(0, 0, 28), rot=(0.05, 0, 0), parent='tail2'),
        P('arm_right', [B((-2, 0, -2), (4, 12, 4), arm_p, share='arm')], pose=(-12, 4, -21), rot=(0.8, 0, 0), parent='body'),
        P('arm_left', [B((-2, 0, -2), (4, 12, 4), arm_p, share='arm')], pose=(12, 4, -21), rot=(0.8, 0, 0), parent='body'),
        P('right_thigh', [B((-6, 0, -7), (12, 22, 14), leg_p, share='thigh')], pose=(-13, Y(42), 7)),
        P('right_shin', [B((-4, 0, -4), (8, 18, 8), leg_p, share='shin')], pose=(0, 21, 2), rot=(-0.3, 0, 0), parent='right_thigh'),
        P('right_foot', [B((-5, 0, -12), (10, 4, 15), foot_p, share='foot')], pose=(0, 17, -1), rot=(0.3, 0, 0), parent='right_shin'),
        P('left_thigh', [B((-6, 0, -7), (12, 22, 14), leg_p, share='thigh')], pose=(13, Y(42), 7)),
        P('left_shin', [B((-4, 0, -4), (8, 18, 8), leg_p, share='shin')], pose=(0, 21, 2), rot=(-0.3, 0, 0), parent='left_thigh'),
        P('left_foot', [B((-5, 0, -12), (10, 4, 15), foot_p, share='foot')], pose=(0, 17, -1), rot=(0.3, 0, 0), parent='left_shin'),
        P('saddle', [B((-11, -18, -6), (22, 3, 14), seat_p, share='saddle_seat'), B((-3, -21, -8), (6, 3, 2), pommel_p, share='saddle_pommel')], parent='body'),
        P('armor_head', [B((-8.6, -8.6, -16), (17.2, 5.2, 18), plate_p, share='armor_head')], parent='head'),
        P('armor_back', [B((-15, -17, -18), (30, 3, 36), plate_p, share='armor_back')], parent='body'),
    ]
    return dict(name=name, w=256, parts=parts)


# ---------------------------------------------------------------- the textures
# Coat variants by rearing biome (DinoVariants.COATS, same names and order): earthy tones, dark back, light belly.
COATS = {
    'olive': (H('66703f'), H('cfc6a0'), H('353a1f')),   # temperate land (standard)
    'sand': (H('a58e57'), H('e6d9ab'), H('5f4d2a')),    # desert, savanna
    'rust': (H('8d583a'), H('d9bd98'), H('4a2c1b')),    # badlands
    'moss': (H('4b5a35'), H('b9b68c'), H('222d17')),    # jungle, swamp
    'ash': (H('7b7165'), H('d0c9ba'), H('3d372f')),     # cold, mountains
}
TIER_NAMES = {0: 'plain', 1: 'leather', 2: 'iron', 3: 'diamond'}
PALETTES = {}
for _coat, (_c, _b, _d) in COATS.items():
    for _tier in range(4):
        PALETTES[f'trex_{_coat}_{_tier}'] = trex_spec(_c, _b, _d, _tier)
MODELS = {'trex': 'trex_olive_0'}
TONES = 16


def finish(img, tones):
    """A8b: at most `tones` colours (median cut over the opaque pixels, no dithering), alpha kept."""
    rgb = Image.new('RGB', img.size)
    rgb.paste(img.convert('RGB'))
    q = rgb.quantize(colors=tones, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).convert('RGBA')
    out = img.copy()
    src, dst = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            if src[x, y][3]:
                dst[x, y] = q.getpixel((x, y))[:3] + (src[x, y][3],)
    return out


TEXTURES = {k: (lambda spec=v, tier=int(k.split('_')[2]): finish(render_model(spec)[0], TONES + (6 if tier else 0))) for k, v in PALETTES.items()}


def out_name(key):
    """textures/entity/trex/<coat>/<tier>.png for every coat and armor tier."""
    _, coat, tier = key.split('_')
    return f'trex/{coat}/{TIER_NAMES[int(tier)]}'


# ---------------------------------------------------------------- packing + painting
def box_footprint(size):
    w, h, d = (math.ceil(s) for s in size)
    return 2 * (d + w), d + h


def pack(spec):
    """Shelf-pack the unique boxes; returns {share: (u, v)} and the texture height."""
    uniq = {}
    for part in spec['parts']:
        for b in part['boxes']:
            uniq.setdefault(b['share'], b)
    items = sorted(uniq.items(), key=lambda kv: (-box_footprint(kv[1]['size'])[1], -box_footprint(kv[1]['size'])[0]))
    width = spec['w']
    x = y = shelf = 0
    place = {}
    for key, b in items:
        fw, fh = box_footprint(b['size'])
        if x + fw > width:
            x, y, shelf = 0, y + shelf, 0
        place[key] = (x, y)
        x += fw
        shelf = max(shelf, fh)
    height = y + shelf
    h = 16
    while h < height:
        h *= 2
    return place, max(h, width // 2)


def faces(u, v, size):
    w, h, d = (math.ceil(s) for s in size)
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


def paint_boxes(img, boxes_uv):
    steps = []
    px = img.load()
    for (u, v, size, paint) in boxes_uv:
        for face, (fx0, fy0, fw, fh) in faces(u, v, size).items():
            for y in range(fh):
                for x in range(fw):
                    px[fx0 + x, fy0 + y] = paint(face, x, y, fw, fh) + (255,)
    return steps


def hard_share(img):
    """Share of neighbouring opaque pixel pairs with a luminance jump above 28 (of 255)."""
    px, w, h = img.load(), img.width, img.height
    n = hard = 0
    for y in range(h):
        for x in range(w):
            if px[x, y][3] == 0:
                continue
            for dx, dy in ((1, 0), (0, 1)):
                if x + dx < w and y + dy < h and px[x + dx, y + dy][3]:
                    n += 1
                    hard += abs(lum(px[x, y]) - lum(px[x + dx, y + dy])) > 28
    return hard / max(1, n)


def render_model(spec):
    place, height = pack(spec)
    img = Image.new('RGBA', (spec['w'], height), (0, 0, 0, 0))
    boxes_uv, seen = [], set()
    for part in spec['parts']:
        for b in part['boxes']:
            if b['share'] in seen:
                continue
            seen.add(b['share'])
            u, v = place[b['share']]
            boxes_uv.append((u, v, b['size'], b['paint']))
    paint_boxes(img, boxes_uv)
    return img, place


# ---------------------------------------------------------------- Java
def jname(s):
    return s


def java_layers(model, spec):
    place, height = pack(spec)
    lines = ['package com.simpledinos.client;', '',
             'import net.minecraft.client.model.geom.PartPose;',
             'import net.minecraft.client.model.geom.builders.CubeDeformation;',
             'import net.minecraft.client.model.geom.builders.CubeListBuilder;',
             'import net.minecraft.client.model.geom.builders.LayerDefinition;',
             'import net.minecraft.client.model.geom.builders.MeshDefinition;',
             'import net.minecraft.client.model.geom.builders.PartDefinition;', '',
             '/** GENERATED by modules/simpledinos/tools/gen_models.py from the model spec, do not edit. */',
             f'final class {model.capitalize()}Layers {{',
             f'    static final int WIDTH = {spec["w"]}, HEIGHT = {height};', '',
             '    static LayerDefinition create() {',
             '        MeshDefinition mesh = new MeshDefinition();',
             '        PartDefinition root = mesh.getRoot();']
    var = {None: 'root'}
    for part in spec['parts']:
        cubes = 'CubeListBuilder.create()'
        for b in part['boxes']:
            u, v = place[b['share']]
            x, y, z = b['pos']
            w, h, d = b['size']
            grow = f', new CubeDeformation({b["grow"]}F)' if b['grow'] else ''
            cubes += f'.texOffs({u}, {v}).addBox({x}F, {y}F, {z}F, {w}F, {h}F, {d}F{grow})'
        px, py, pz = part['pose']
        rx, ry, rz = part['rot']
        pose = (f'PartPose.offsetAndRotation({px}F, {py}F, {pz}F, {rx}F, {ry}F, {rz}F)' if (rx or ry or rz)
                else f'PartPose.offset({px}F, {py}F, {pz}F)')
        name = part['name']
        lines.append(f'        PartDefinition {name} = {var[part["parent"]]}.addOrReplaceChild("{name}", {cubes}, {pose});')
        var[name] = name
    lines += ['        return LayerDefinition.create(mesh, WIDTH, HEIGHT);', '    }', '', '    private %sLayers() {}' % model.capitalize(), '}', '']
    return '\n'.join(lines)


def preview_data(spec):
    place, height = pack(spec)
    out = []
    for part in spec['parts']:
        for b in part['boxes']:
            u, v = place[b['share']]
            out.append(dict(part=part['name'], parent=part['parent'], pose=part['pose'], rot=part['rot'], pos=b['pos'], size=b['size'],
                            uv=[u, v]))
    return dict(w=spec['w'], h=height, boxes=out,
                parts={p['name']: dict(parent=p['parent'], pose=p['pose'], rot=p['rot']) for p in spec['parts']})


# ---------------------------------------------------------------- main
def outputs():
    """{path: bytes|str} of everything this script owns."""
    files = {}
    for key, fn in TEXTURES.items():
        name = out_name(key)
        if name is None:
            continue
        img = fn()
        L.snap(img, 'Grundform')
        files[TEX / (name + '.png')] = img
    for model, key in MODELS.items():
        files[JAVA / f'{model.capitalize()}Layers.java'] = java_layers(model, PALETTES[key])
    return files


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--preview')
    args = ap.parse_args(argv)
    files = outputs()
    stale = []
    for path, content in files.items():
        if isinstance(content, str):
            if not path.exists() or path.read_text(encoding='utf-8') != content:
                stale.append(path)
                if not args.check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_text(content, encoding='utf-8')
        else:
            same = path.exists() and Image.open(path).convert('RGBA').tobytes() == content.convert('RGBA').tobytes()
            if not same:
                stale.append(path)
                if not args.check:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    L.save(content, path)
    for key in PALETTES:
        if key.endswith('_0') or key.endswith('_1'):
            img = TEXTURES[key]()
            print(f'{key}: {img.width}x{img.height}, hard transitions {hard_share(img) * 100:.1f} %')
    if args.preview:
        out = Path(args.preview)
        out.mkdir(parents=True, exist_ok=True)
        (out / 'models.json').write_text(json.dumps({m: preview_data(PALETTES[k]) for m, k in MODELS.items()}), encoding='utf-8')
        for key, fn in TEXTURES.items():
            fn().save(out / (key + '.png'))
    if args.check and stale:
        print('stale: ' + ', '.join(str(p.relative_to(ROOT)) for p in stale))
        return 1
    print('simpledinos models: %d files %s' % (len(files), 'current' if not stale else 'written'))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
