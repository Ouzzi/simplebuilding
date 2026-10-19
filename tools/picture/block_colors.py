#!/usr/bin/env python3
"""Mean colours of the picture-blueprint palette (wool, concrete, terracotta) from the Minecraft client jar.

The palette (PALETTE in common/src/shared/java/com/simplebuilding/blueprint/BlueprintPicture.java) is the
plain sRGB mean of the opaque pixels of each block's texture in the default resource pack (what a player sees on a wall). MapColor is not used: wool and
concrete share one MapColor per dye, so half the palette would collide.

Usage: python3 tools/picture/block_colors.py <minecraft-client.jar>   (prints Java lines)
No Pillow needed: 8-bit RGB/RGBA and 1-8-bit palette/grey PNGs are decoded with zlib.
"""
import struct
import sys
import zipfile
import zlib

DYES = ["white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
        "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"]


def blocks():
    for kind in ("wool", "concrete", "terracotta"):
        for dye in DYES:
            yield f"{dye}_{kind}"
    yield "terracotta"


def png_pixels(data):
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    pos, idat, plte, trns = 8, b"", None, None
    while pos < len(data):
        length, kind = struct.unpack(">I4s", data[pos:pos + 8])
        body = data[pos + 8:pos + 8 + length]
        pos += 12 + length
        if kind == b"IHDR":
            w, h, depth, colour, _, _, interlace = struct.unpack(">IIBBBBB", body)
            assert interlace == 0 and (depth == 8 or colour in (0, 3)), "only 8-bit or low-bit palette/grey"
        elif kind == b"PLTE":
            plte = body
        elif kind == b"tRNS":
            trns = body
        elif kind == b"IDAT":
            idat += body
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[colour]
    raw = zlib.decompress(idat)
    stride = (w * channels * depth + 7) // 8
    bpp = max(1, channels * depth // 8)
    prev = bytearray(stride)
    out = []
    i = 0
    for _ in range(h):
        f = raw[i]
        line = bytearray(raw[i + 1:i + 1 + stride])
        i += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        prev = line
        if depth < 8:
            per = 8 // depth
            line = bytearray((line[x // per] >> (8 - depth * (x % per + 1))) & ((1 << depth) - 1) for x in range(w))
            if colour == 0:
                line = bytearray(v * 255 // ((1 << depth) - 1) for v in line)
        for x in range(w):
            px = line[x * channels:(x + 1) * channels]
            if colour == 6:
                out.append(tuple(px))
            elif colour == 2:
                out.append((px[0], px[1], px[2], 255))
            elif colour == 3:
                k = px[0]
                alpha = trns[k] if trns and k < len(trns) else 255
                out.append((plte[3 * k], plte[3 * k + 1], plte[3 * k + 2], alpha))
            elif colour == 0:
                out.append((px[0], px[0], px[0], 255))
            else:
                out.append((px[0], px[0], px[0], px[1]))
    return out


def mean(pixels):
    opaque = [p for p in pixels if p[3] >= 128]
    n = len(opaque)
    return tuple(round(sum(p[c] for p in opaque) / n) for c in range(3))


def main():
    with zipfile.ZipFile(sys.argv[1]) as jar:
        for name in blocks():
            r, g, b = mean(png_pixels(jar.read(f"assets/minecraft/textures/block/{name}.png")))
            print(f'            new Entry("{name}", 0x{r:02X}{g:02X}{b:02X}),')


if __name__ == "__main__":
    main()
