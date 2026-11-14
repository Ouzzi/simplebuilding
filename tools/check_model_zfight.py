#!/usr/bin/env python3
"""N43: finds z-fighting in the Java entity/head models (ModelPart geometry), like tools/check_item_zfight.py does for JSON.

Scans every Java file under common/src and modules/*/shared/java that builds a mesh with ``addOrReplaceChild`` (the generated
``*Layers.java`` of Simple Animals / Simple Dinos and the hand-made models of the mob heads, mobs, dummies ...). Each static method
returning a ``LayerDefinition`` is one model: its parts are posed (offset, rotation Z*Y*X, scale; children on top), every cube
(with its ``CubeDeformation``) becomes six faces, and two faces of DIFFERENT cubes that lie in the same plane, face the same way and
overlap are reported - the GPU flickers between both textures. Entity layers in 26.x draw without back-face culling, so faces that
face each other in the same plane are reported too when one of the models is drawn translucent (see ``TRANSLUCENT``).

Heads (``HEAD_GROUPS``): the passes drawn together form one model, and a translucent layer also may not touch the floor (y = 0) or
the wall (z = +4 for an 8 px head) the skull stands on / hangs at.

Only the rest pose is checked (animations turn parts at run time); expressions it cannot evaluate (method parameters, fields of
other classes) count as 0 and are listed as warnings. JSON block/item models: tools/check_item_zfight.py.

    python3 tools/check_model_zfight.py              # all models, findings + summary, exit 1 on findings
    python3 tools/check_model_zfight.py --only dinos # path filter (substring)
    python3 tools/check_model_zfight.py --json out.json   # faces + findings for previews
"""
from __future__ import annotations

import argparse
import json
import math
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EPS_PLANE = 1e-3   # px: closer than this = the same plane
EPS_AREA = 1e-3    # px^2: smaller overlaps are edges/corners, not fighting
# Models drawn translucent: there faces that face each other in one plane fight too (you see both through the layer).
TRANSLUCENT = {'CustomHeadModels.slimeOuterLayer', 'ModSkullModels.slimeOuterLayer'}
# Mob heads drawn as several passes on one skull: checked as one model, plus floor/wall contact of translucent layers.
HEAD_GROUPS = {'CustomHeadModels.slime': ['slimeInnerLayer', 'slimeOuterLayer'], 'ModSkullModels.slime': ['slimeInnerLayer', 'slimeOuterLayer']}
# Known, accepted findings ("file-stem.method: part/cube~part/cube" prefixes) - keep empty unless the owner accepted a look.
ACCEPTED: set[str] = set()


# ---------------------------------------------------------------- tiny Java expression evaluator
def split_args(s):
    out, depth, cur, q = [], 0, '', False
    for ch in s:
        if ch == '"':
            q = not q
        if not q and ch in '([{':
            depth += 1
        elif not q and ch in ')]}':
            depth -= 1
        if ch == ',' and depth == 0 and not q:
            out.append(cur.strip())
            cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out


def close_paren(s, i):
    """Index of the ')' matching the '(' at s[i]."""
    depth, q = 0, False
    for j in range(i, len(s)):
        ch = s[j]
        if ch == '"':
            q = not q
        if q:
            continue
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
            if depth == 0:
                return j
    raise ValueError('unbalanced')


class Ctx:
    def __init__(self, consts, warn):
        self.consts = consts
        self.warn = warn

    def num(self, expr):
        e = expr.strip()
        e = re.sub(r'\((?:float|double|int)\)', '', e)
        e = e.replace('Math.PI', repr(math.pi)).replace('Mth.PI', repr(math.pi))
        e = re.sub(r'Math\.(sin|cos|sqrt|toRadians)', lambda m: 'math.' + ('radians' if m.group(1) == 'toRadians' else m.group(1)), e)
        e = re.sub(r'(?<![\w.])(\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?[FfDd]\b', lambda m: m.group(0)[:-1], e)

        def ident(m):
            name = m.group(0)
            if name in ('math',) or name.startswith('math.'):
                return name
            if name in self.consts:
                return repr(self.consts[name])
            self.warn(f'unknown value {name!r} counted as 0')
            return '0'
        e = re.sub(r'\b(?!math\b)[A-Za-z_][\w.]*', ident, e)
        try:
            return float(eval(e, {'math': math, '__builtins__': {}}))
        except Exception:
            self.warn(f'cannot evaluate {expr!r}, counted as 0')
            return 0.0

    def deformation(self, expr):
        e = expr.strip()
        m = re.match(r'new\s+CubeDeformation\s*\((.*)\)\s*$', e, re.S)
        if m:
            a = [self.num(x) for x in split_args(m.group(1))]
            return (a[0], a[0], a[0]) if len(a) == 1 else tuple(a[:3])
        if e in self.consts and isinstance(self.consts[e], tuple):
            return self.consts[e]
        m = re.match(r'(\w+)\.extend\s*\((.*)\)\s*$', e, re.S)
        if m:
            base = self.deformation(m.group(1))
            a = [self.num(x) for x in split_args(m.group(2))]
            a = a * 3 if len(a) == 1 else a
            return tuple(base[i] + a[i] for i in range(3))
        if e.endswith('NONE') or e == 'CubeDeformation.NONE':
            return (0.0, 0.0, 0.0)
        self.warn(f'unknown deformation {e!r} counted as 0')
        return (0.0, 0.0, 0.0)


# ---------------------------------------------------------------- parsing
def parse_cubes(text, ctx):
    """CubeListBuilder chain -> [(name, (x, y, z), (w, h, d), (gx, gy, gz))]."""
    cubes, i, mirror = [], 0, False
    for m in re.finditer(r'\.(addBox|mirror)\s*\(', text):
        if m.group(1) == 'mirror':
            continue
        start = m.end() - 1
        args = split_args(text[start + 1:close_paren(text, start)])
        name = ''
        if args and args[0].startswith('"'):
            name, args = args[0].strip('"'), args[1:]
        if len(args) < 6:
            ctx.warn(f'addBox with {len(args)} args skipped')
            continue
        pos = tuple(ctx.num(a) for a in args[0:3])
        size = tuple(ctx.num(a) for a in args[3:6])
        grow = (0.0, 0.0, 0.0)
        if len(args) >= 7 and args[6] not in ('true', 'false'):
            g = args[6]
            if 'Deformation' in g or '.extend(' in g or isinstance(ctx.consts.get(g), tuple):
                grow = ctx.deformation(g)
            else:
                v = ctx.num(g)
                grow = (v, v, v)
        cubes.append((name or f'#{len(cubes)}', pos, size, grow))
    return cubes


def parse_pose(text, ctx):
    t = text.strip()
    pose = dict(offset=(0.0, 0.0, 0.0), rot=(0.0, 0.0, 0.0), scale=(1.0, 1.0, 1.0))
    m = re.search(r'\.withScale\s*\(', t)
    if m:
        a = [ctx.num(x) for x in split_args(t[m.end():close_paren(t, m.end() - 1)])]
        pose['scale'] = (a[0],) * 3 if len(a) == 1 else tuple(a[:3])
    for kind in ('offsetAndRotation', 'offset', 'rotation'):
        m = re.search(r'PartPose\.' + kind + r'\s*\(', t)
        if m:
            a = [ctx.num(x) for x in split_args(t[m.end():close_paren(t, m.end() - 1)])]
            if kind == 'offsetAndRotation':
                pose['offset'], pose['rot'] = tuple(a[0:3]), tuple(a[3:6])
            elif kind == 'offset':
                pose['offset'] = tuple(a[0:3])
            else:
                pose['rot'] = tuple(a[0:3])
            break
    return pose


def class_consts(src):
    consts = {}
    for m in re.finditer(r'(?:static\s+)?final\s+(?:float|double|int)\s+(\w+)\s*=\s*([^;]+);', src):
        try:
            consts[m.group(1)] = float(eval(re.sub(r'(\d)[FfDd]\b', r'\1', m.group(2)).replace('Math.PI', repr(math.pi)),
                                            {'__builtins__': {}}))
        except Exception:
            pass
    for m in re.finditer(r'(?:static\s+)?final\s+int\s+WIDTH\s*=\s*(\d+)\s*,\s*HEIGHT\s*=\s*(\d+)', src):
        consts['WIDTH'], consts['HEIGHT'] = float(m.group(1)), float(m.group(2))
    return consts


def methods(src):
    """(name, body) of every method that builds a LayerDefinition/MeshDefinition and adds parts."""
    for m in re.finditer(r'(?:static\s+)?(?:LayerDefinition|MeshDefinition|void)\s+(\w+)\s*\(([^)]*)\)\s*\{', src):
        start = m.end() - 1
        depth = 0
        for j in range(start, len(src)):
            if src[j] == '{':
                depth += 1
            elif src[j] == '}':
                depth -= 1
                if depth == 0:
                    break
        body = src[start + 1:j]
        if 'addOrReplaceChild' in body:
            yield m.group(1), m.group(2), body


def parse_model(body, consts, warn, params=''):
    """Parts of one method: {name: dict(parent, pose, cubes)} in definition order. Method parameters count as 0 / no
    deformation (the base call; passes with an inflate are separate models)."""
    local = dict(consts)
    for m in re.finditer(r'(\w+)\s+(\w+)\s*(?:,|$)', params.strip()):
        local[m.group(2)] = (0.0, 0.0, 0.0) if m.group(1) == 'CubeDeformation' else 0.0
    ctx = Ctx(local, warn)
    for m in re.finditer(r'(?:float|double)\s+(\w+)\s*=\s*([^;]+);', body):
        local[m.group(1)] = ctx.num(m.group(2))
    for m in re.finditer(r'CubeDeformation\s+(\w+)\s*=\s*(new\s+CubeDeformation\s*\([^;]*\))\s*;', body):
        local[m.group(1)] = ctx.deformation(m.group(2))
    parts = {'': dict(parent=None, pose=dict(offset=(0, 0, 0), rot=(0, 0, 0), scale=(1, 1, 1)), cubes=[])}
    var = {}
    for m in re.finditer(r'(\w+)\s*=\s*(\w+)\.getRoot\(\)', body):
        var[m.group(1)] = ''
    for stmt in body.split(';'):
        k = stmt.find('.addOrReplaceChild(')
        if k < 0:
            continue
        head = stmt[:k]
        assign = re.search(r'(\w+)\s*=\s*([^=]*)$', head)
        target = assign.group(1) if assign else None
        pm = re.search(r'(\w+(?:\.\w+\((?:"\w*")?\))*)\s*$', head)
        parent_expr = pm.group(1) if pm else ''
        if parent_expr.endswith('getRoot()'):
            parent = ''
        else:
            chain = re.findall(r'getChild\("(\w+)"\)', parent_expr)
            base = re.match(r'(\w+)', parent_expr)
            parent = chain[-1] if chain else var.get(base.group(1) if base else '', base.group(1) if base else '')
        p0 = k + len('.addOrReplaceChild')
        args = split_args(stmt[p0 + 1:close_paren(stmt, p0)])
        if len(args) < 3:
            continue
        name = args[0].strip('"')
        parts[name] = dict(parent=parent if parent in parts else '', pose=parse_pose(args[2], ctx), cubes=parse_cubes(args[1], ctx))
        if target:
            var[target] = name
    return parts


# ---------------------------------------------------------------- geometry
def mat_zyx(x, y, z):
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    return [[cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx],
            [sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx],
            [-sy, cy * sx, cy * cx]]


def mm(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def mv(a, v):
    return [sum(a[i][k] * v[k] for k in range(3)) for i in range(3)]


def world(parts):
    """name -> (3x3 linear, translation) in model space (ModelPart: translate, rotate ZYX, scale; parent first)."""
    out = {'': ([[1, 0, 0], [0, 1, 0], [0, 0, 1]], [0.0, 0.0, 0.0])}
    for name, p in parts.items():
        if name == '':
            continue
        lin_p, t_p = out.get(p['parent'], out[''])
        ox, oy, oz = p['pose']['offset']
        rx, ry, rz = p['pose']['rot']
        sx, sy, sz = p['pose']['scale']
        local = mm(mat_zyx(rx, ry, rz), [[sx, 0, 0], [0, sy, 0], [0, 0, sz]])
        t = [t_p[i] + mv(lin_p, [ox, oy, oz])[i] for i in range(3)]
        out[name] = (mm(lin_p, local), t)
    return out


FACE_IDX = {  # face name -> (axis, side) ; corners in local cube coords
    'west': (0, 0), 'east': (0, 1), 'up': (1, 0), 'down': (1, 1), 'north': (2, 0), 'south': (2, 1)}


def faces(parts, tag=''):
    """[dict(part, cube, face, pts(4x3), n, d, layer)] for all cubes."""
    W = world(parts)
    out = []
    for name, p in parts.items():
        lin, t = W.get(name, W[''])
        for ci, (cname, pos, size, grow) in enumerate(p['cubes']):
            lo = [pos[i] - grow[i] for i in range(3)]
            hi = [pos[i] + size[i] + grow[i] for i in range(3)]
            for fname, (ax, side) in FACE_IDX.items():
                o1, o2 = [a for a in range(3) if a != ax]
                pts = []
                for a, b in ((0, 0), (1, 0), (1, 1), (0, 1)):
                    c = [0.0] * 3
                    c[ax] = hi[ax] if side else lo[ax]
                    c[o1] = hi[o1] if a else lo[o1]
                    c[o2] = hi[o2] if b else lo[o2]
                    w = mv(lin, c)
                    pts.append([w[i] + t[i] for i in range(3)])
                nl = [0.0] * 3
                nl[ax] = 1.0 if side else -1.0
                n = mv(lin, nl)
                ln = math.sqrt(sum(x * x for x in n)) or 1.0
                n = [x / ln for x in n]
                out.append(dict(part=name, cube=ci, cname=cname, face=fname, pts=pts, n=n, d=sum(n[i] * pts[0][i] for i in range(3)),
                                layer=tag, flat=size[ax] == 0 and grow[ax] == 0))
    return out


def signed_area(poly):
    return 0.5 * sum(poly[i - 1][0] * poly[i][1] - poly[i][0] * poly[i - 1][1] for i in range(len(poly)))


def poly_area(poly):
    return abs(signed_area(poly))


def clip(subject, clipper):
    def inside(p, a, b):
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -1e-9

    def inter(p, q, a, b):
        x1, y1, x2, y2, x3, y3, x4, y4 = *p, *q, *a, *b
        den = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4)
        if abs(den) < 1e-12:
            return p
        t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / den
        return (x1 + t * (x2 - x1), y1 + t * (y2 - y1))
    out = subject
    if signed_area(clipper) < 0:
        clipper = clipper[::-1]
    for i in range(len(clipper)):
        a, b = clipper[i - 1], clipper[i]
        inp, out = out, []
        if not inp:
            break
        for j in range(len(inp)):
            p, q = inp[j - 1], inp[j]
            if inside(q, a, b):
                if not inside(p, a, b):
                    out.append(inter(p, q, a, b))
                out.append(q)
            elif inside(p, a, b):
                out.append(inter(p, q, a, b))
    return out


def overlap(f1, f2):
    n = f1['n']
    u = [1, 0, 0] if abs(n[0]) < 0.9 else [0, 1, 0]
    e1 = [n[1] * u[2] - n[2] * u[1], n[2] * u[0] - n[0] * u[2], n[0] * u[1] - n[1] * u[0]]
    l = math.sqrt(sum(x * x for x in e1))
    e1 = [x / l for x in e1]
    e2 = [n[1] * e1[2] - n[2] * e1[1], n[2] * e1[0] - n[0] * e1[2], n[0] * e1[1] - n[1] * e1[0]]
    pr = lambda f: [(sum(p[i] * e1[i] for i in range(3)), sum(p[i] * e2[i] for i in range(3))) for p in f['pts']]
    a, b = pr(f1), pr(f2)
    if poly_area(a) < 1e-9 or poly_area(b) < 1e-9:
        return 0.0
    c = clip(a, b)
    return poly_area(c) if len(c) >= 3 else 0.0


def findings(fs, translucent_layers=(), contact=None):
    """Pairs of faces of different cubes in one plane that overlap: 'same' (same facing) always, 'opposed' when one layer is
    translucent. contact = extra planes [(name, n, d)] (floor/wall) checked against translucent layers' faces."""
    res = []
    buckets = {}
    for i, f in enumerate(fs):
        key = tuple(round(abs(x), 3) for x in f['n'])
        buckets.setdefault(key, []).append(i)
    for idx in buckets.values():
        for ii in range(len(idx)):
            f1 = fs[idx[ii]]
            for jj in range(ii + 1, len(idx)):
                f2 = fs[idx[jj]]
                if f1['part'] == f2['part'] and f1['cube'] == f2['cube'] and f1['layer'] == f2['layer']:
                    continue
                dot = sum(f1['n'][k] * f2['n'][k] for k in range(3))
                if abs(abs(dot) - 1) > 1e-6:
                    continue
                same = dot > 0
                if abs(f1['d'] - (f2['d'] if same else -f2['d'])) > EPS_PLANE:
                    continue
                if not same and not (f1['layer'] in translucent_layers or f2['layer'] in translucent_layers):
                    continue
                area = overlap(f1, f2)
                if area > EPS_AREA:
                    res.append(dict(kind='same' if same else 'opposed', a=f1, b=f2, area=area))
    for cname, n, d in contact or ():
        for f in fs:
            if f['layer'] not in translucent_layers:
                continue
            dot = sum(f['n'][k] * n[k] for k in range(3))
            if dot > 1 - 1e-6 and abs(f['d'] - d) < EPS_PLANE:
                res.append(dict(kind='contact', a=f, b=dict(part=cname, cname='', face='', layer='world'), area=poly_area(
                    [(p[0], p[2]) if abs(n[1]) > 0.5 else (p[0], p[1]) for p in f['pts']])))
    return res


# ---------------------------------------------------------------- driver
def sources():
    for base in [ROOT / 'common/src'] + sorted((ROOT / 'modules').glob('*/shared/java')):
        for p in sorted(base.rglob('*.java')):
            txt = p.read_text(encoding='utf-8', errors='replace')
            txt = re.sub(r'/\*.*?\*/', '', txt, flags=re.S)
            txt = re.sub(r'(?<![:"])//[^\n]*', '', txt)  # comments (not "http://")
            if 'addOrReplaceChild' in txt and 'addBox' in txt:
                yield p, txt


def label(f):
    lay = f"{f['layer']}:" if f['layer'] else ''
    return f"{lay}{f['part']}[{f['cname']}].{f['face']}"


def scan(only=None):
    models = []
    for path, src in sources():
        rel = path.relative_to(ROOT).as_posix()
        if only and only not in rel:
            continue
        consts = class_consts(src)
        bodies = {}
        for name, params, body in methods(src):
            warns = []
            parts = parse_model(body, consts, warns.append, params)
            bodies[name] = parts
            key = f'{path.stem}.{name}'
            if len(parts) <= 1:
                continue
            fs = faces(parts, '')
            tl = {''} if key in TRANSLUCENT else set()
            # cubes built in loops/from arrays cannot be read statically: listed, not judged
            models.append(dict(file=rel, model=key, parts=parts, faces=fs, warnings=sorted(set(warns)),
                               findings=[] if warns else findings(fs, tl), skipped=bool(warns)))
        for gkey, layers in HEAD_GROUPS.items():
            stem, _ = gkey.split('.', 1)
            if stem != path.stem:
                continue
            fs, tl = [], set()
            for layer in layers:
                if layer in bodies:
                    fs += faces(bodies[layer], layer)
                    if f'{stem}.{layer}' in TRANSLUCENT:
                        tl.add(layer)
            contact = [('floor', [0.0, 1.0, 0.0], 0.0), ('wall', [0.0, 0.0, 1.0], 4.0)]
            models.append(dict(file=rel, model=gkey + ' (on the skull)', parts=None, faces=fs, warnings=[],
                               findings=findings(fs, tl, contact)))
    return models


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument('--only')
    ap.add_argument('--json')
    ap.add_argument('-q', '--quiet', action='store_true', help='only models with findings')
    args = ap.parse_args(argv)
    models = scan(args.only)
    total = 0
    for m in models:
        found = [f for f in m['findings'] if not any(f"{m['model']}: {label(f['a'])}".startswith(a) for a in ACCEPTED)]
        total += len(found)
        if args.quiet and not found and not m.get('skipped'):
            continue
        print(f"{'FUND' if found else 'skip' if m.get('skipped') else 'ok  '} {m['model']}  ({m['file']}, {len(m['faces']) // 6} cubes)")
        for f in found:
            print(f"     {f['kind']:8s} {label(f['a'])} ~ {label(f['b'])}  ({f['area']:.2f} px2)")
        for w in m['warnings'] if found or not args.quiet else []:
            print(f'     warn  {w}')
    print(f'{len(models)} models, {total} findings')
    if args.json:
        def slim(f):
            return {k: f[k] for k in ('part', 'cname', 'face', 'layer', 'pts', 'n') if k in f}
        Path(args.json).write_text(json.dumps([dict(model=m['model'], file=m['file'], faces=[slim(f) for f in m['faces']],
                                                    findings=[dict(kind=f['kind'], area=f['area'], a=slim(f['a']), b=slim(f['b']))
                                                              for f in m['findings']]) for m in models]), encoding='utf-8')
    return 1 if total else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
