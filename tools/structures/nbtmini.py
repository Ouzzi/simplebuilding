"""Minimal gzip NBT reader/writer for structure templates (no dependency).
Values: ('b'|'s'|'i'|'l'|'f'|'d', x), str, list ('list', tag, [..]), dict (compound), ('ia', [...])."""
import gzip, io, struct

_ID = {'b': 1, 's': 2, 'i': 3, 'l': 4, 'f': 5, 'd': 6}
_FMT = {1: '>b', 2: '>h', 3: '>i', 4: '>q', 5: '>f', 6: '>d'}


def _tag(v):
    if isinstance(v, tuple):
        return {'list': 9, 'ia': 11}.get(v[0]) or _ID[v[0]]
    return 8 if isinstance(v, str) else 10


def _w(o, t, v):
    if t in _FMT:
        o.write(struct.pack(_FMT[t], v[1]))
    elif t == 8:
        b = v.encode(); o.write(struct.pack('>H', len(b)) + b)
    elif t == 10:
        for k, x in v.items():
            xt = _tag(x); o.write(bytes([xt])); _w(o, 8, k); _w(o, xt, x)
        o.write(b'\0')
    elif t == 9:
        o.write(bytes([v[1]]) + struct.pack('>i', len(v[2])))
        for x in v[2]: _w(o, v[1], x)
    elif t == 11:
        o.write(struct.pack('>i', len(v[1])))
        for x in v[1]: o.write(struct.pack('>i', x))


def dumps(root):
    o = io.BytesIO(); o.write(b'\x0a\x00\x00'); _w(o, 10, root)
    return gzip.compress(o.getvalue(), mtime=0)


def _r(i, t):
    if t in _FMT:
        n = struct.calcsize(_FMT[t]); return struct.unpack(_FMT[t], i.read(n))[0]
    if t == 8:
        return i.read(struct.unpack('>H', i.read(2))[0]).decode()
    if t == 10:
        d = {}
        while True:
            c = i.read(1)[0]
            if c == 0: return d
            k = _r(i, 8); d[k] = _r(i, c)
    if t == 9:
        c = i.read(1)[0]; n = struct.unpack('>i', i.read(4))[0]
        return [_r(i, c) for _ in range(n)]
    if t == 11:
        n = struct.unpack('>i', i.read(4))[0]; return list(struct.unpack('>%di' % n, i.read(4 * n)))
    if t == 7:
        n = struct.unpack('>i', i.read(4))[0]; return i.read(n)
    if t == 12:
        n = struct.unpack('>i', i.read(4))[0]; return list(struct.unpack('>%dq' % n, i.read(8 * n)))
    raise ValueError(t)


def loads(data):
    i = io.BytesIO(gzip.decompress(data)); i.read(1); _r(i, 8); return _r(i, 10)
