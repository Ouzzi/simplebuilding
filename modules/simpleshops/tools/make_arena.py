"""Writes data/simpleshops/structure/arena.nbt: an all-air 15 x 12 x 15 test structure (shop arena).

Same layout as the module template's empty.nbt (palette [air], DataVersion 4903): python modules/simpleshops/tools/make_arena.py
"""
import gzip
import struct
from pathlib import Path

SIZE = (15, 12, 15)
OUT = Path(__file__).resolve().parents[1] / 'shared/resources/data/simpleshops/structure/arena.nbt'


def name(text):
    raw = text.encode('utf-8')
    return struct.pack('>H', len(raw)) + raw


def int_list(key, values):
    return b'\x09' + name(key) + b'\x03' + struct.pack('>i', len(values)) + b''.join(struct.pack('>i', v) for v in values)


def build():
    blocks = []
    for y in range(SIZE[1]):
        for x in range(SIZE[0]):
            for z in range(SIZE[2]):
                blocks.append(int_list('pos', [x, y, z]) + b'\x03' + name('state') + struct.pack('>i', 0) + b'\x00')
    body = int_list('size', list(SIZE))
    body += b'\x09' + name('entities') + b'\x00' + struct.pack('>i', 0)
    body += b'\x09' + name('blocks') + b'\x0a' + struct.pack('>i', len(blocks)) + b''.join(blocks)
    body += b'\x09' + name('palette') + b'\x0a' + struct.pack('>i', 1) + b'\x08' + name('Name') + name('minecraft:air') + b'\x00'
    body += b'\x03' + name('DataVersion') + struct.pack('>i', 4903)
    return b'\x0a' + name('') + body + b'\x00'


if __name__ == '__main__':
    OUT.write_bytes(gzip.compress(build(), mtime=0))
    print(OUT, OUT.stat().st_size)
