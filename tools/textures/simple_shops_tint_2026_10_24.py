"""Simple Shops tinted display case, ONE example tone (red; NA24-3, branch b-shops24). Own pixel art on top of the checked-in textures.

Three variants for the red case, the rest of the 16 tones only after the owner picks one (UI/look rule 22.10.):

* A "Tinted glass" (recommended, checked in as display_case_glass_red): the glass pane gets a red tint (alpha 78 instead of 24), frame stays iron.
* B "Red frame": clear glass, iron frame in a red ramp (display_case_frame_red).
* C "Both soft": glass alpha 48 with a warmer red and the red frame.

Writes variant A (the chosen one) beside the other block textures; with --variants DIR all three variants as 16x16 PNGs into DIR
(used by the preview). Pure Python (layers.py, no Pillow). Run: python3 tools/textures/simple_shops_tint_2026_10_24.py [--check] [--variants DIR]
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import layers as L  # noqa: E402

ASSETS = os.path.join(HERE, '..', '..', 'modules', 'simpleshops', 'shared', 'resources', 'assets', 'simpleshops', 'textures', 'block')
CLEAR = (200, 232, 244, 24)
IRON_TO_RED = {(224, 228, 234): (240, 150, 140), (184, 188, 196): (214, 96, 88), (136, 140, 148): (170, 52, 48), (96, 100, 108): (116, 32, 32)}
CHOSEN = 'a'


def read(name):
    w, h, px = L.png_read(open(os.path.join(ASSETS, name + '.png'), 'rb').read())
    return [tuple(px[i:i + 4]) for i in range(0, len(px), 4)]


def pack(pixels):
    return b''.join(bytes(p) for p in pixels)


def glass(rgba):
    return [rgba if p == CLEAR else p for p in read('display_case_glass')]


def frame():
    return [IRON_TO_RED.get(p[:3], p[:3]) + (p[3],) for p in read('display_case_frame')]


def variants():
    return {'a': {'glass': glass((214, 48, 40, 78))}, 'b': {'frame': frame()}, 'c': {'glass': glass((226, 70, 60, 48)), 'frame': frame()}}


def out(name, pixels):
    return pack(pixels), L.from_layers(16, 16, [('Grundform', pack(pixels))])


def main():
    check = '--check' in sys.argv
    if '--variants' in sys.argv:
        d = sys.argv[sys.argv.index('--variants') + 1]
        os.makedirs(d, exist_ok=True)
        for v, parts in variants().items():
            for part, pixels in parts.items():
                open(os.path.join(d, f'{v}_{part}.png'), 'wb').write(L.png_write(16, 16, pack(pixels)))
        return
    raw, doc = out('display_case_glass_red', variants()[CHOSEN]['glass'])
    path = os.path.join(ASSETS, 'display_case_glass_red.png')
    if check:
        have = bytes(L.png_read(open(path, 'rb').read())[2]) if os.path.exists(path) else b''
        if have != raw:
            raise SystemExit('stale: ' + path)
        print('simple shops tint textures: up to date')
        return
    open(path, 'wb').write(L.png_write(16, 16, raw))
    L.write_beside(path, doc)
    print('simple shops tint textures: written')


if __name__ == '__main__':
    main()
