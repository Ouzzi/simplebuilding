#!/usr/bin/env python3
"""Independent reference (Python port of BlueprintCode.serialize) for the N51 tests. Usage: ref_blueprint.py <structure.nbt>
Reads a vanilla structure .nbt (gzip) and prints the blueprint code the Java serializer would write
(defaults table limited to the rules in conv-core.js; same omission rule)."""
import gzip, re, struct, sys

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

F4 = dict(north='false', east='false', south='false', west='false')
def d(**k): return dict(waterlogged='false', **k)
RULES = [
 (r'_stairs$', d(facing='north', half='bottom', shape='straight')), (r'_slab$', d(type='bottom')),
 (r'_fence_gate$', dict(facing='north', in_wall='false', open='false', powered='false')), (r'_fence$', d(**F4)),
 (r'_wall$', d(east='none', north='none', south='none', west='none', up='true')),
 (r'_trapdoor$', d(facing='north', half='bottom', open='false', powered='false')),
 (r'_door$', dict(facing='north', half='lower', hinge='left', open='false', powered='false')),
 (r'(_log|_wood|_stem|_hyphae|_pillar|hay_block|bone_block)$|^(basalt|deepslate|quartz_pillar|purpur_pillar)$', dict(axis='y')),
 (r'_leaves$', d(distance='7', persistent='false')), (r'^(glass_pane|iron_bars)$|_glass_pane$', d(**F4)),
 (r'^(lantern|soul_lantern)$', d(hanging='false')), (r'^(ladder|chest|trapped_chest|ender_chest)$', d(facing='north', type='single')),
 (r'^(furnace|blast_furnace|smoker)$', dict(facing='north', lit='false')), (r'^(wall_torch|soul_wall_torch|redstone_wall_torch)$', dict(facing='north')),
 (r'_wall_sign$', d(facing='north')), (r'_sign$', d(rotation='0')), (r'_button$', dict(face='wall', facing='north', powered='false')),
 (r'_bed$', dict(facing='north', occupied='false', part='foot'))]

def spec(name, props):
    ns, _, path = name.partition(':')
    out = path if ns == 'minecraft' else name
    defs = next((r for pat, r in RULES if re.search(pat, path)), None) if ns == 'minecraft' else None
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

if __name__ == '__main__':
    n = read_nbt(open(sys.argv[1], 'rb').read())
    skip = re.compile(r'^minecraft:(air|cave_air|void_air|structure_void|moving_piston)$')
    pal = n.get('palette') or n['palettes'][0]
    sp = [None if skip.match(e['Name']) else spec(e['Name'], {k: str(v) for k, v in e.get('Properties', {}).items()}) for e in pal]
    print(serialize({tuple(b['pos']): sp[b['state']] for b in n['blocks'] if sp[b['state']]}), end='')
