"""Usage: python tools/textures/small_parts_textures.py <out-dir>

Stone Pebble and Flint Chip (owner 2026-10-02): small hand-set sprites in vanilla's item language - the
pebble in cobblestone greys (a round stone with a lit top left and a darker rim), the chip a small
conchoidal flint flake in vanilla flint's tones. Output: textures/item/stone_pebble.png, flint_chip.png."""
from PIL import Image
import os
import sys

OUT = sys.argv[1] if len(sys.argv) > 1 else 'build/small-parts/'

STONE = {'o': (60, 60, 60), 'd': (90, 90, 90), 'm': (120, 120, 120), 'l': (150, 150, 150), 'h': (186, 186, 186)}
FLINT = {'o': (28, 26, 26), 'd': (52, 50, 50), 'm': (78, 76, 76), 'l': (110, 108, 108), 'h': (160, 158, 158)}

PEBBLE = [
    '................',
    '................',
    '................',
    '................',
    '................',
    '......oooo......',
    '....oohhlloo....',
    '...ohhllmmmdo...',
    '...ohlmmmmddo...',
    '..olmmmdmmmddo..',
    '..ommmmmmdmddo..',
    '...oddmmmddddo..',
    '....ooddddooo...',
    '......ooooo.....',
    '................',
    '................',
]

CHIP = [
    '................',
    '................',
    '................',
    '................',
    '.........oo.....',
    '........ohlo....',
    '.......ohlmo....',
    '......ohlmmdo...',
    '.....ohlmmmdo...',
    '....ohlmmdmddo..',
    '....olmmmmdddo..',
    '...oomdmmdddoo..',
    '....ooddddoo....',
    '......oooo......',
    '................',
    '................',
]


def draw(rows, pal):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in pal:
                im.putpixel((x, y), pal[ch] + (255,))
    return im


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    draw(PEBBLE, STONE).save(os.path.join(OUT, 'stone_pebble.png'))
    draw(CHIP, FLINT).save(os.path.join(OUT, 'flint_chip.png'))
