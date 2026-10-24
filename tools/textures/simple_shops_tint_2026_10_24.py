"""Simple Shops tinted display cases, all 16 dye tones (NA24-3 -> WB24-5, owner picked variant C "both soft", 24.10.).

Variant C per tone: the glass pane gets a soft tint (alpha 48) AND the iron frame goes into a ramp of the tone. The red tone is
the hand-picked example (glass 226,70,60 / ramp 240,150,140 .. 116,32,32); the other 15 take its saturation/brightness steps
at their own hue (HSV), scaled by the dye colour's saturation/value, so white stays pale and black stays dark.
Writes display_case_glass_<c>, display_case_frame_<c>, glow_display_case_frame_<c> (the two glow accents stay) beside the other
block textures. Pure Python (layers.py, no Pillow). Run: python3 tools/textures/simple_shops_tint_2026_10_24.py [--check] [--preview DIR]
"""
import colorsys
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layers as L  # noqa: E402

ASSETS = os.path.join(HERE, '..', '..', 'modules', 'simpleshops', 'shared', 'resources', 'assets', 'simpleshops', 'textures', 'block')
CLEAR = (200, 232, 244, 24)
IRON = [(224, 228, 234), (184, 188, 196), (136, 140, 148), (96, 100, 108)]
RED_RAMP = [(240, 150, 140), (214, 96, 88), (170, 52, 48), (116, 32, 32)]
RED_GLASS = (226, 70, 60, 48)
GLASS_ALPHA = 48
# Vanilla dye colours (DyeColor.getTextureDiffuseColor), in DyeColor order.
DYES = {'white': 0xF9FFFE, 'orange': 0xF9801D, 'magenta': 0xC74EBD, 'light_blue': 0x3AB3DA, 'yellow': 0xFED83D, 'lime': 0x80C71F,
        'pink': 0xF38BAA, 'gray': 0x474F52, 'light_gray': 0x9D9D97, 'cyan': 0x169C9C, 'purple': 0x8932B8, 'blue': 0x3C44AA,
        'brown': 0x835432, 'green': 0x5E7C16, 'red': 0xB02E26, 'black': 0x1D1D21}


def hsv(rgb):
    return colorsys.rgb_to_hsv(*(c / 255 for c in rgb))


def from_hsv(h, s, v):
    return tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, min(1, s), min(1, v)))


def tone(name, ref):
    """The reference (red) colour at the hue of the dye, saturation and brightness scaled by the dye's own."""
    if name == 'red':
        return ref
    d = DYES[name]
    dh, ds, dv = hsv(((d >> 16) & 255, (d >> 8) & 255, d & 255))
    _, rs, rv = hsv(ref)
    return from_hsv(dh, rs * min(1, ds / 0.7), rv * (0.35 + 0.65 * dv))


def ramp(name):
    return [tone(name, c) for c in RED_RAMP]


def glass_color(name):
    return tone(name, RED_GLASS[:3]) + (GLASS_ALPHA,)


def read(name):
    w, h, px = L.png_read(open(os.path.join(ASSETS, name + '.png'), 'rb').read())
    return [tuple(px[i:i + 4]) for i in range(0, len(px), 4)]


def pack(pixels):
    return b''.join(bytes(p) for p in pixels)


def textures(name):
    m = dict(zip(IRON, ramp(name)))
    return {
        'display_case_glass_' + name: [glass_color(name) if p == CLEAR else p for p in read('display_case_glass')],
        'display_case_frame_' + name: [m.get(p[:3], p[:3]) + (p[3],) for p in read('display_case_frame')],
        'glow_display_case_frame_' + name: [m.get(p[:3], p[:3]) + (p[3],) for p in read('glow_display_case_frame')],
    }


def main():
    check = '--check' in sys.argv
    if '--preview' in sys.argv:
        d = sys.argv[sys.argv.index('--preview') + 1]
        os.makedirs(d, exist_ok=True)
        for c in DYES:
            for n, px in textures(c).items():
                open(os.path.join(d, n + '.png'), 'wb').write(L.png_write(16, 16, pack(px)))
        return
    for c in DYES:
        for n, px in textures(c).items():
            raw = pack(px)
            path = os.path.join(ASSETS, n + '.png')
            if check:
                have = bytes(L.png_read(open(path, 'rb').read())[2]) if os.path.exists(path) else b''
                if have != raw:
                    raise SystemExit('stale: ' + path)
                continue
            open(path, 'wb').write(L.png_write(16, 16, raw))
            L.write_beside(path, L.from_layers(16, 16, [('Grundform', raw)]))
    print('simple shops tint textures: ' + ('up to date' if check else 'written'))


if __name__ == '__main__':
    main()
