#!/usr/bin/env python3
"""N46 (b-circ46, 14.11.2026): own pixel art of the Simple Ores circuit blocks (no Mojang pixels, committed):
nihil_core(_on) - 32x32 connected-texture sheet (tiles A full border, B none, C top+bottom, D left+right; see
modules/simpleores/tools/circuit_models.py), astral_relay(_on) - 16x16. Round 2 (no seams inside a group, lit: speckles and rim glow) placeholders, owner review CIRC46B.

    python3 tools/textures/circuit46_2026_11_14.py [--out DIR]
"""
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'modules/simpleores/shared/resources/assets/simpleores/textures/block'

def hexc(s):
    s = s.lstrip('#'); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

# Nihilit casing: dark teal frame, cool plate; lit: the plate's seams glow cyan.
EDGE, RIM, PLATE, PLATE_D, PLATE_L = map(hexc, ('#16323a', '#3f7f8b', '#2b5763', '#234852', '#35697a'))
GLOW, GLOW_L = hexc('#57d6e6'), hexc('#b4f4fa')


def plate(x, y, lit):
    """Seamless plate (pattern repeats every 16 px, so a group of cores reads as one surface; round 2: no per-block seams)."""
    x %= 16; y %= 16
    if (x + 2 * y) % 7 == 0:
        return GLOW_L if lit and (x * 3 + y) % 4 == 0 else (GLOW if lit else PLATE_L)
    if (x + 2 * y) % 7 == 3 and (x + y) % 2 == 0:
        return PLATE_D
    return PLATE


def tile(top_bottom, left_right, lit):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = plate(x, y, lit)
            edge_v = top_bottom and (y in (0, 15)); edge_h = left_right and (x in (0, 15))
            rim_v = top_bottom and (y in (1, 14)); rim_h = left_right and (x in (1, 14))
            if edge_v or edge_h:
                c = EDGE
            elif rim_v or rim_h:
                c = GLOW if lit else RIM
            im.putpixel((x, y), c)
    return im


def core(lit):
    sheet = Image.new('RGBA', (32, 32))
    sheet.paste(tile(True, True, lit), (0, 0))      # A
    sheet.paste(tile(False, False, lit), (16, 0))   # B
    sheet.paste(tile(True, False, lit), (0, 16))    # C
    sheet.paste(tile(False, True, lit), (16, 16))   # D
    return sheet


# Astral relay: copper-bulb shape in Astralit rose; the lens lights up.
FRAME_D, FRAME, FRAME_L = map(hexc, ('#5a2e52', '#9a5688', '#c27fb1'))
LENS_OFF, LENS_OFF_L = hexc('#3a2238'), hexc('#57344f')
LENS_ON, LENS_ON_L, LENS_ON_W = hexc('#e49dd6'), hexc('#f6c9ec'), hexc('#fff0fb')


def relay(lit):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = FRAME_D
            elif x in (1, 14) or y in (1, 14):
                c = FRAME_L if (x + y) % 2 == 0 else FRAME
            elif 4 <= x <= 11 and 4 <= y <= 11:
                if lit:
                    c = LENS_ON_W if 6 <= x <= 9 and 6 <= y <= 9 and (x + y) % 3 else (LENS_ON_L if 5 <= x <= 10 and 5 <= y <= 10 else LENS_ON)
                else:
                    c = LENS_OFF_L if (x == 5 and 5 <= y <= 7) or (y == 5 and 5 <= x <= 7) else LENS_OFF
            elif x in (3, 12) or y in (3, 12):
                c = FRAME_D
            else:
                c = FRAME
            im.putpixel((x, y), c)
    return im


def main():
    out = Path(sys.argv[sys.argv.index('--out') + 1]) if '--out' in sys.argv else OUT
    out.mkdir(parents=True, exist_ok=True)
    for lit in (False, True):
        sfx = '_on' if lit else ''
        core(lit).save(out / f'nihil_core{sfx}.png')
        relay(lit).save(out / f'astral_relay{sfx}.png')
    print('circuit46 textures written to', out)


if __name__ == '__main__':
    main()
