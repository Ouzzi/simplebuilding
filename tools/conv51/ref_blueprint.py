#!/usr/bin/env python3
"""Independent reference (Python port of BlueprintCode.serialize) for the N51 tests.
Usage: ref_blueprint.py <file>   (.nbt structure, Sponge .schem v1-v3, .litematic, MCEdit .schematic; gzip or plain)
Prints the blueprint code the Java serializer would write. The readers here are written separately from conv-formats.js;
only the tables (defaults, renames, MCEdit ids) come from the same generated wiki/assets/conv/conv-data.js."""
import gzip, json, os, re, struct, sys

_DATA_JS = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'wiki', 'assets', 'conv', 'conv-data.js')
_t = open(_DATA_JS, encoding='utf-8').read()
DATA = json.loads(_t[_t.index('/*DATA*/') + 8:_t.index('/*END*/')])
KNOWN = {ns + ':' + b for ns, v in DATA['blocks'].items() for b in v.split(' ')}

def read_nbt(data):
    if data[:2] == b'\x1f\x8b': data = gzip.decompress(data)
    p = [0]
    def take(n):
        v = data[p[0]:p[0] + n]; p[0] += n; return v
    def s(): return take(struct.unpack('>H', take(2))[0]).decode('utf-8')
    def val(t):
        if t == 1: return struct.unpack('>b', take(1))[0]
        if t == 2: return struct.unpack('>h', take(2))[0]
        if t == 3: return struct.unpack('>i', take(4))[0]
        if t == 4: return struct.unpack('>q', take(8))[0]
        if t == 5: return struct.unpack('>f', take(4))[0]
        if t == 6: return struct.unpack('>d', take(8))[0]
        if t == 7: n = struct.unpack('>i', take(4))[0]; return list(take(n))
        if t == 8: return s()
        if t == 9:
            et = take(1)[0]; n = struct.unpack('>i', take(4))[0]; return [val(et) for _ in range(n)]
        if t == 10:
            o = {}
            while True:
                tt = take(1)[0]
                if tt == 0: return o
                k = s(); o[k] = val(tt)
        if t == 11: n = struct.unpack('>i', take(4))[0]; return [struct.unpack('>i', take(4))[0] for _ in range(n)]
        if t == 12: n = struct.unpack('>i', take(4))[0]; return [struct.unpack('>q', take(8))[0] for _ in range(n)]
        raise ValueError(t)
    assert take(1)[0] == 10; s(); return val(10)

RULES = [(re.compile(p), d) for p, d in DATA['defaults']]
EXACT = {(b if ':' in b else 'minecraft:' + b): dict(kv.split('=', 1) for kv in d.split(','))
         for d, ids in DATA.get('stateDefaults', {}).items() for b in ids.split(' ')}


def upgrade(name, props, dv):
    if ':' not in name: name = 'minecraft:' + name
    if not name.startswith('minecraft:'): return name, props
    path = name[10:]
    if name not in KNOWN and path in DATA['renames']: path = DATA['renames'][path]
    for below, old, new in DATA['renamesBelow']:
        if dv is not None and dv < below and path == old: path = new
    for pat, keys, vmap in DATA['valueMaps']:
        if re.search(pat, path):
            props = {k: vmap.get(v, v) if k in keys else v for k, v in props.items()}
    if path == 'cauldron' and 'level' in props:
        props = dict(props); lv = props.pop('level')
        if lv != '0': path = 'water_cauldron'; props['level'] = lv
    return 'minecraft:' + path, props


def spec(name, props):
    ns, _, path = name.partition(':')
    out = path if ns == 'minecraft' else name
    if name in EXACT: defs = EXACT[name]
    elif ns == 'minecraft': defs = {} if EXACT else next((r for pat, r in RULES if pat.search(path)), None)
    else: defs = None
    parts = [f'{k}={v}' for k, v in sorted(props.items()) if not (defs and defs.get(k) == v)]
    return f'{out}[{",".join(parts)}]' if parts else out


def boxes(cells):  # cells: set of (x,y,z); greedy as BlueprintCode.boxes
    rem = set(cells); out = []
    for (x, y, z) in sorted(cells, key=lambda c: (c[1], c[2], c[0])):
        if (x, y, z) not in rem: continue
        x2 = x
        while (x2 + 1, y, z) in rem: x2 += 1
        z2 = z
        while all((i, y, z2 + 1) in rem for i in range(x, x2 + 1)): z2 += 1
        y2 = y
        while all((i, y2 + 1, k) in rem for i in range(x, x2 + 1) for k in range(z, z2 + 1)): y2 += 1
        for yy in range(y, y2 + 1):
            for zz in range(z, z2 + 1):
                for xx in range(x, x2 + 1): rem.discard((xx, yy, zz))
        out.append((x, y, z, x2, y2, z2))
    return out

def serialize(blocks):
    if not blocks: return ''
    mn = [min(p[i] for p in blocks) for i in range(3)]; mx = [max(p[i] for p in blocks) for i in range(3)]
    by = {}
    for p, s in blocks.items(): by.setdefault(s, set()).add(tuple(p[i] - mn[i] for i in range(3)))
    groups = sorted(by.items(), key=lambda kv: (-len(kv[1]), kv[0]))
    ax = lambda a, b: str(a) if a == b else f'{a}..{b}'
    def wrap(sp, regs):
        lines, line, on = [], sp, 0
        for r in regs:
            if on and len(line) + 1 + len(r) > 100: lines.append(line); line, on = sp, 0
            line += ' ' + r; on += 1
        return lines + [line]
    def alias(i):
        s = ''
        while True:
            s = chr(97 + i % 26) + s; i = i // 26 - 1
            if i < 0: return s
    head = f'# {mx[0]-mn[0]+1}x{mx[1]-mn[1]+1}x{mx[2]-mn[2]+1}, {len(blocks)} blocks\n'; al = body = ''; ai = 0
    for sp, cs in groups:
        regs = [f'{ax(b[0],b[3])},{ax(b[1],b[4])},{ax(b[2],b[5])}' for b in boxes(cs)]
        lines = wrap(sp, regs)
        if len(lines) > 1 and len(sp) > 12:
            a = '$' + alias(ai); ai += 1; al += f'{a} = {sp}\n'; lines = wrap(a, regs)
        body += ''.join(l + '\n' for l in lines)
    return (head + al + body).rstrip()

SKIP = re.compile(r'^minecraft:(air|cave_air|void_air|structure_void|moving_piston)$')


def state(name, props, dv):
    name, props = upgrade(name, {k: str(v) for k, v in props.items()}, dv)
    return None if SKIP.match(name) else spec(name, props)


def parse_state(text):
    m = re.match(r'^([^\[]+)(?:\[(.*)\])?$', text.strip())
    props = dict(kv.split('=', 1) for kv in m.group(2).split(',') if '=' in kv) if m.group(2) else {}
    return m.group(1), {k.strip(): v.strip() for k, v in props.items()}


def read_structure(n):
    dv = n.get('DataVersion'); pal = n.get('palette') or n['palettes'][0]
    sp = [state(e['Name'], e.get('Properties', {}), dv) for e in pal]
    return {tuple(b['pos']): sp[b['state']] for b in n['blocks'] if sp[b['state']]}


def read_sponge(n):
    s = n['Schematic'] if isinstance(n.get('Schematic'), dict) else n
    v3 = s.get('Version') == 3
    blk = s['Blocks'] if v3 else s
    pal, data = blk['Palette'], bytes(b & 255 for b in (blk['Data'] if v3 else s['BlockData']))
    w, h, l = (s[k] & 0xffff for k in ('Width', 'Height', 'Length'))
    dv = s.get('DataVersion')
    sp = {}
    for text, i in pal.items():
        name, props = parse_state(text); sp[i] = state(name, props, dv)
    out, i, p = {}, 0, 0
    while p < len(data) and i < w * h * l:
        v = shift = 0
        while True:
            b = data[p]; p += 1; v |= (b & 127) << shift; shift += 7
            if not b & 128: break
        y, rest = divmod(i, w * l); z, x = divmod(rest, w)
        if sp.get(v): out[(x, y, z)] = sp[v]
        i += 1
    return out


def read_litematic(n, only=None):
    dv = n.get('MinecraftDataVersion'); regs = []
    for name, r in n['Regions'].items():
        pos = [r['Position'][a] for a in 'xyz']; size = [r['Size'][a] for a in 'xyz']
        regs.append((name, r, [pos[i] + (size[i] + 1 if size[i] < 0 else 0) for i in range(3)], [abs(v) for v in size]))
    base = [min(g[2][i] for g in regs) for i in range(3)]
    out = {}
    for name, r, mn, (sx, sy, sz) in regs:
        if only is not None and name != only: continue
        pal = r['BlockStatePalette']; sp = [state(e['Name'], e.get('Properties', {}), dv) for e in pal]
        bits = max(2, (len(pal) - 1).bit_length()); longs = [v & 0xffffffffffffffff for v in r['BlockStates']]
        big = sum(v << (64 * i) for i, v in enumerate(longs))  # whole array as one integer, bit k = bit k of the stream
        for i in range(sx * sy * sz):
            v = (big >> (i * bits)) & ((1 << bits) - 1)
            y, rest = divmod(i, sx * sz); z, x = divmod(rest, sx)
            if sp[v]: out[(mn[0] - base[0] + x, mn[1] - base[1] + y, mn[2] - base[2] + z)] = sp[v]
    return out


RAIL = ['north_south', 'east_west', 'ascending_east', 'ascending_west', 'ascending_north', 'ascending_south', 'south_east', 'south_west', 'north_west', 'north_east']


def legacy(i, m, below):
    e = DATA['legacy'].get(str(i))
    if e is None: return 'legacy:id_%d' % i, {}
    if isinstance(e, str): return e, {}
    names, mask, kind = e[0], e[1], (e[2] if len(e) > 2 else '')
    lst = isinstance(names, list)
    pick = lambda k: (names[k] if k < len(names) else names[0]) if lst else names
    name, p = pick(m & mask), {}
    if kind == 'level': p['level'] = str(m & 15)
    elif kind == 'log':
        ax = (m >> 2) & 3
        if ax == 3: name = re.sub(r'_log$', '_wood', name); p['axis'] = 'y'
        else: p['axis'] = 'yxz'[ax]
    elif kind == 'leaves': p['persistent'] = 'true' if m & 4 else 'false'
    elif kind == 'facing6': p['facing'] = ['down', 'up', 'north', 'south', 'west', 'east', 'north', 'north'][m & 7]
    elif kind == 'prail': p['shape'] = RAIL[m & 7] if (m & 7) < 6 else 'north_south'; p['powered'] = 'true' if m & 8 else 'false'
    elif kind == 'rail': p['shape'] = RAIL[m] if m < 10 else 'north_south'
    elif kind == 'dslab': name = pick(m & 7); p['type'] = 'double'
    elif kind == 'slab': p['type'] = 'top' if m & 8 else 'bottom'
    elif kind in ('torch', 'torch_off'):
        if 1 <= m <= 4: name = re.sub(r'torch$', 'wall_torch', name); p['facing'] = ['east', 'west', 'south', 'north'][m - 1]
        if kind == 'torch_off': p['lit'] = 'false'
    elif kind == 'stairs': p['facing'] = ['east', 'west', 'south', 'north'][m & 3]; p['half'] = 'top' if m & 4 else 'bottom'
    elif kind in ('facing4', 'litfacing4'):
        p['facing'] = {2: 'north', 3: 'south', 4: 'west', 5: 'east'}.get(m & 7, 'north')
        if kind == 'litfacing4': p['lit'] = 'true'
    elif kind == 'age': p['age'] = str(m & 7)
    elif kind == 'rot16': p['rotation'] = str(m & 15)
    elif kind == 'door':
        low = (below or 0) if m & 8 else m
        p['facing'] = ['east', 'south', 'west', 'north'][low & 3]; p['open'] = 'true' if low & 4 else 'false'
        p['half'] = 'upper' if m & 8 else 'lower'
        if m & 8: p['hinge'] = 'right' if m & 1 else 'left'
    elif kind == 'dplant':
        if m & 8: name = names[(2 if below is None else below) & 7] if ((2 if below is None else below) & 7) < len(names) else names[2]; p['half'] = 'upper'
        else: name = names[m & 7] if (m & 7) < len(names) else names[2]
    elif kind == 'pumpkin': p['facing'] = ['south', 'west', 'north', 'east'][m & 3]
    elif kind == 'snow': p['layers'] = str((m & 7) + 1)
    elif kind == 'lit': p['lit'] = 'true'
    elif kind == 'inverted': p['inverted'] = 'true'
    elif kind == 'quartz':
        if 2 <= m <= 4: p['axis'] = 'yxz'[m - 2]
    elif kind == 'axis': p['axis'] = ('yxz' + 'y')[(m >> 2) & 3]
    return name, p


def read_mcedit(n):
    w, h, l = (n[k] & 0xffff for k in ('Width', 'Height', 'Length'))
    ids, meta, add = n['Blocks'], n.get('Data') or [0] * len(n['Blocks']), n.get('AddBlocks')
    def bid(i):
        b = ids[i] & 255
        if add and (i >> 1) < len(add): b |= ((add[i >> 1] & 0x0f) << 8) if i % 2 == 0 else ((add[i >> 1] & 0xf0) << 4)
        return b
    out = {}
    for i in range(min(w * h * l, len(ids))):
        b = bid(i)
        if b == 0: continue
        m = meta[i] & 15; below = None
        e = DATA['legacy'].get(str(b))
        if isinstance(e, list) and len(e) > 2 and e[2] in ('door', 'dplant') and m & 8 and i >= w * l and bid(i - w * l) == b: below = meta[i - w * l] & 15
        name, props = legacy(b, m, below)
        s = state(name, props, 1343)
        y, rest = divmod(i, w * l); z, x = divmod(rest, w)
        if s: out[(x, y, z)] = s
    return out


def read_any(n):
    if isinstance(n.get('Regions'), dict): return read_litematic(n)
    s = n['Schematic'] if isinstance(n.get('Schematic'), dict) else n
    if ('Palette' in s and 'BlockData' in s) or isinstance(s.get('Blocks'), dict): return read_sponge(n)
    if isinstance(n.get('Blocks'), list): return read_mcedit(n)
    return read_structure(n)


if __name__ == '__main__':
    print(serialize(read_any(read_nbt(open(sys.argv[1], 'rb').read()))), end='')
