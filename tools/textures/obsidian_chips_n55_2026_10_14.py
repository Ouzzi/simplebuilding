"""Usage: python tools/textures/obsidian_chips_n55_2026_10_14.py [--check] [preview.png]

N55 (owner 2026-10-14): the Crying Obsidian Chip and the Blazing Obsidian Chip, small parts like the Obsidian Chip
(small_chips_2026_10_05.py). Both keep that item language - small hand-set sprite, dark outline in the material's own
darkest tone, lit from the top left, 5-8 px tall - and start from the obsidian flake's silhouette, but each gets its
own colours and its own detail instead of being a recolour:
- crying_obsidian_chip   the flake in the blue-violet of crying obsidian, with a tear: a light streak runs down the
                         face and a drop hangs off the lower right edge.
- blazing_obsidian_chip  a slightly wider, jagged flake of warm black glass with a glowing ember crack running
                         from the upper right to the lower left (dark red rim, orange glow, hot yellow core).
Own pixel art; no Vanilla pixels. Writes mc26_3/overlay/.../textures/item/<name>.png; --check only compares."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
T = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


CHIPS = {
    'crying_obsidian_chip': ({'o': '#0a0620', 'd': '#1c0f4d', 'm': '#31207f', 'l': '#4e34b0', 'h': '#9a7cf2',
                              't': '#7a66f2', 'T': '#b9a8ff'}, [
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '......ooooo.....',
        '.....ohllmdo....',
        '....ohlmmmddo...',
        '....olmmtdddo...',
        '.....omdtdoo....',
        '......oddto.o...',
        '.......oo.oTo...',
        '...........o....',
        '................',
        '................',
    ]),
    'blazing_obsidian_chip': ({'o': '#0a0608', 'd': '#1c1119', 'm': '#2c1a26', 'l': '#3f2636', 'h': '#7a5468',
                               'r': '#7e2f08', 'O': '#d4660e', 'Y': '#ffc050'}, [
        '................',
        '................',
        '................',
        '................',
        '................',
        '.........o......',
        '......ooooo.....',
        '.....ohlmmdOo...',
        '....ohlmmrOddo..',
        '....olmmrYrddo..',
        '.....omrOrdoo...',
        '......orddo.....',
        '.......oo.......',
        '................',
        '................',
        '................',
    ]),
}


def draw(pal, rows):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    assert len(rows) == 16
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != '.':
                im.putpixel((x, y), hexrgb(pal[ch]) + (255,))
    return im


def preview(path):
    refs = [('obsidian_chip (Bestand)', Image.open(os.path.join(T, 'obsidian_chip.png')).convert('RGBA')),
            ('flint_chip (Bestand)', Image.open(os.path.join(T, 'flint_chip.png')).convert('RGBA'))]
    items = refs + [(n, draw(*CHIPS[n])) for n in CHIPS]
    z, cw = 8, 16 * 8 + 30
    img = Image.new('RGBA', (20 + len(items) * cw, 16 * z + 90), (198, 198, 198, 255))
    d = ImageDraw.Draw(img)
    for i, (name, im) in enumerate(items):
        x = 20 + i * cw
        img.alpha_composite(im.resize((16 * z, 16 * z), Image.NEAREST), (x, 10))
        slot = Image.new('RGBA', (18, 18), (139, 139, 139, 255))
        slot.alpha_composite(im, (1, 1))
        img.alpha_composite(slot, (x, 16 * z + 20))
        img.alpha_composite(slot.resize((36, 36), Image.NEAREST), (x + 26, 16 * z + 20))
        d.text((x, 16 * z + 64), name, fill=(0, 0, 0, 255))
    img.save(path)


def main():
    args = sys.argv[1:]
    check = '--check' in args
    args = [a for a in args if a != '--check']
    stale = []
    for name, (pal, rows) in CHIPS.items():
        im, path = draw(pal, rows), os.path.join(T, name + '.png')
        if check:
            try:
                same = Image.open(path).convert('RGBA').tobytes() == im.tobytes()
            except OSError:
                same = False
            if not same:
                stale.append(name)
        else:
            im.save(path)
    if stale:
        raise SystemExit('stale chip textures: ' + ', '.join(stale))
    if args:
        preview(args[0])
    print('obsidian chips N55:', 'up to date' if check else 'written')


if __name__ == '__main__':
    main()
