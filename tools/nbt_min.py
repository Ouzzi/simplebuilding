"""Minimal NBT (gzip, big-endian) reader/writer for structure templates. Tags are kept as (type, value)
pairs so a file round-trips byte for byte; helpers build new compounds."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


def _read(buf, pos, t):
    if t == BYTE:
        return struct.unpack_from('>b', buf, pos)[0], pos + 1
    if t == SHORT:
        return struct.unpack_from('>h', buf, pos)[0], pos + 2
    if t == INT:
        return struct.unpack_from('>i', buf, pos)[0], pos + 4
    if t == LONG:
        return struct.unpack_from('>q', buf, pos)[0], pos + 8
    if t == FLOAT:
        return struct.unpack_from('>f', buf, pos)[0], pos + 4
    if t == DOUBLE:
        return struct.unpack_from('>d', buf, pos)[0], pos + 8
    if t == BYTE_ARRAY:
        n = struct.unpack_from('>i', buf, pos)[0]
        return list(buf[pos + 4:pos + 4 + n]), pos + 4 + n
    if t == STRING:
        n = struct.unpack_from('>H', buf, pos)[0]
        return buf[pos + 2:pos + 2 + n].decode('utf-8'), pos + 2 + n
    if t == LIST:
        et = buf[pos]
        n = struct.unpack_from('>i', buf, pos + 1)[0]
        pos += 5
        items = []
        for _ in range(n):
            v, pos = _read(buf, pos, et)
            items.append(v)
        return (et, items), pos
    if t == COMPOUND:
        out = {}
        while True:
            tt = buf[pos]
            pos += 1
            if tt == END:
                return out, pos
            n = struct.unpack_from('>H', buf, pos)[0]
            name = buf[pos + 2:pos + 2 + n].decode('utf-8')
            pos += 2 + n
            v, pos = _read(buf, pos, tt)
            out[name] = (tt, v)
    if t == INT_ARRAY:
        n = struct.unpack_from('>i', buf, pos)[0]
        return list(struct.unpack_from('>%di' % n, buf, pos + 4)), pos + 4 + 4 * n
    if t == LONG_ARRAY:
        n = struct.unpack_from('>i', buf, pos)[0]
        return list(struct.unpack_from('>%dq' % n, buf, pos + 4)), pos + 4 + 8 * n
    raise ValueError(t)


def load(path):
    buf = gzip.open(path).read()
    assert buf[0] == COMPOUND
    n = struct.unpack_from('>H', buf, 1)[0]
    v, _ = _read(buf, 3 + n, COMPOUND)
    return v


def _write(t, v):
    if t == BYTE:
        return struct.pack('>b', v)
    if t == SHORT:
        return struct.pack('>h', v)
    if t == INT:
        return struct.pack('>i', v)
    if t == LONG:
        return struct.pack('>q', v)
    if t == FLOAT:
        return struct.pack('>f', v)
    if t == DOUBLE:
        return struct.pack('>d', v)
    if t == BYTE_ARRAY:
        return struct.pack('>i', len(v)) + bytes(x & 0xFF for x in v)
    if t == STRING:
        b = v.encode('utf-8')
        return struct.pack('>H', len(b)) + b
    if t == LIST:
        et, items = v
        if not items:
            et = END if et is None else et
        return bytes([et]) + struct.pack('>i', len(items)) + b''.join(_write(et, x) for x in items)
    if t == COMPOUND:
        out = b''
        for name, (tt, vv) in v.items():
            nb = name.encode('utf-8')
            out += bytes([tt]) + struct.pack('>H', len(nb)) + nb + _write(tt, vv)
        return out + bytes([END])
    if t == INT_ARRAY:
        return struct.pack('>i', len(v)) + struct.pack('>%di' % len(v), *v)
    if t == LONG_ARRAY:
        return struct.pack('>i', len(v)) + struct.pack('>%dq' % len(v), *v)
    raise ValueError(t)


def dump(root, path):
    data = bytes([COMPOUND]) + struct.pack('>H', 0) + _write(COMPOUND, root)
    with gzip.GzipFile(path, 'wb', mtime=0) as f:
        f.write(data)


def plain(v, t=COMPOUND):
    """Readable view for inspection."""
    if t == COMPOUND:
        return {k: plain(vv, tt) for k, (tt, vv) in v.items()}
    if t == LIST:
        et, items = v
        return [plain(x, et) for x in items]
    return v
