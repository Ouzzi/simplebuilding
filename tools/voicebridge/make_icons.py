"""Generate the PWA raster icons from simple microphone geometry, using stdlib only."""
from pathlib import Path
import math
import struct
import zlib


def icon(size):
    data = bytearray()
    for row in range(size):
        data.append(0)
        for column in range(size):
            x, y = (column+.5)*192/size, (row+.5)*192/size
            capsule = 77 <= x <= 115 and 57 <= y <= 97
            capsule |= (x-96)**2 + (y-57)**2 <= 19**2
            capsule |= (x-96)**2 + (y-97)**2 <= 19**2
            arc = 32 <= math.hypot(x-96,y-100) <= 42 and y >= 100
            sides = (54 <= x <= 64 or 128 <= x <= 138) and 88 <= y <= 100
            stand = 91 <= x <= 101 and 137 <= y <= 157 or 73 <= x <= 119 and 152 <= y <= 162
            data.extend((124,200,90) if capsule or arc or sides or stand else (20,22,25))
    def chunk(kind, payload):
        return struct.pack('>I',len(payload)) + kind + payload + struct.pack('>I',zlib.crc32(kind+payload))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR',struct.pack('>IIBBBBB',size,size,8,2,0,0,0)) + chunk(b'IDAT',zlib.compress(data)) + chunk(b'IEND',b'')


if __name__ == '__main__':
    for size in (192,512):
        path = Path(__file__).parent/'static'/f'icon-{size}.png'
        # Show the inspected target before replacing a generated artifact.
        print(f'{path}: {"existing generated icon" if path.exists() else "new icon"}')
        path.write_bytes(icon(size))
