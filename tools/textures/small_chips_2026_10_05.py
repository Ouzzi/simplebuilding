"""Usage: python tools/textures/small_chips_2026_10_05.py [--check] [preview.png]

New small parts (owner 2026-10-05: "Fire Chip" from a fire charge, "Ice Chip" from ice, plus an own idea in the same
logic: "Obsidian Chip"). Same item language as the Stone Pebble / Flint Chip (round10_settled_2026_10_02.py): a small
hand-set sprite, dark outline in the material's own darkest tone, lit top left, 5-7 px tall, each with a silhouette of
its own so the three chips do not read as recolours of the flint flake:
- fire_chip      a chunk of fire charge: charcoal crust (fire_charge greys/browns), glowing orange-yellow crack.
- ice_chip       a slim pointed ice splinter on the diagonal, ice / packed ice blues with a white edge light.
- obsidian_chip  a broad conchoidal glass flake, obsidian black-violet with one violet glint.
Writes mc26_3/overlay/.../textures/item/<name>.png; --check only compares."""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
T = os.path.join(ROOT, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures', 'item')


def hexrgb(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


CHIPS = {
    'fire_chip': ({'o': '#1f1615', 'd': '#372a28', 'm': '#514931', 'k': '#6c6056',
                   'r': '#993500', 'O': '#c16d0b', 'Y': '#eeac18'}, [
        '................',
        '................',
        '................',
        '................',
        '................',
        '.......ooo......',
        '......okmdo.....',
        '.....okrOmdo....',
        '.....orOYrdo....',
        '....okmrOddo....',
        '....odmdrdo.....',
        '.....oddddo.....',
        '......oooo......',
        '................',
        '................',
        '................',
    ]),
    'ice_chip': ({'o': '#3d5f9c', 'd': '#6d93df', 'm': '#86aefd', 'l': '#a1c3ff', 'h': '#dce9ff'}, [
        '................',
        '................',
        '................',
        '................',
        '................',
        '..........oo....',
        '.........ohho...',
        '........ohlmo...',
        '.......ohlmdo...',
        '......ohmmdo....',
        '......olmdo.....',
        '.....olddo......',
        '.....oooo.......',
        '................',
        '................',
        '................',
    ]),
    'obsidian_chip': ({'o': '#08060d', 'd': '#1b1428', 'm': '#2c1f40', 'l': '#463360', 'h': '#8f6fc0'}, [
        '................',
        '................',
        '................',
        '................',
        '................',
        '................',
        '......ooooo.....',
        '.....ohllmdo....',
        '....ohlmmmddo...',
        '....olmmmdddo...',
        '.....omdddoo....',
        '......oddo......',
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
    refs = [('stone_pebble (Bestand)', Image.open(os.path.join(T, 'stone_pebble.png')).convert('RGBA')),
            ('flint_chip (Bestand)', Image.open(os.path.join(T, 'flint_chip.png')).convert('RGBA'))]
    items = refs + [(f'{chr(65 + i)} {n}', draw(*CHIPS[n])) for i, n in enumerate(CHIPS)]
    z, cw = 14, 16 * 14 + 30
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
    print('small chips:', 'up to date' if check else 'written')


if __name__ == '__main__':
    main()
