"""Usage: python tools/textures/round10_settled_2026_10_02.py <vanilla textures dir>

Owner picks of 2026-10-02 (late): Stone Pebble (round-9 A without its bottom-right stray pixel), Flint Chip (between
round-9 A and B), Astral/Nihil Redstone powder and lamp (B7b variant B), switches (B7b variant A with a larger
indicator in the middle). Written into the 26.3 overlay."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
sys.argv = [sys.argv[0], V, 'build/unused/']
import proposals_b7_2026_10_02 as b7  # noqa: E402
import proposals_v9_2026_10_02 as v9  # noqa: E402

b7.V = V
T = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding', 'textures')

PEBBLE = ['................'] * 5 + [
    '......oooo......',
    '....oollmmoo....',
    '...olhllmmmdo...',
    '...ollmmmmddo...',
    '...odmmmmddo....',
    '....oddddoo.....',
    '.....oooo.......'] + ['................'] * 4
FLINT = ['................'] * 5 + [
    '........oo......',
    '.......olho.....',
    '......olmmdo....',
    '.....olmmddo....',
    '....olmmddo.....',
    '....oddddo......',
    '.....ooo........'] + ['................'] * 4


def switch(kind, active):
    """Variant A, the gem in the middle 4x4 instead of 2x2 so on/off reads at a glance."""
    ramp = b7.RAMPS[kind]
    im = b7.switch(kind, active, 'A')  # plate + bevel; A's small gem (inside 6..9) is redrawn bigger below
    gem, glow, dark, edge = (ramp[4], ramp[5], ramp[2], ramp[1]) if active else (ramp[2], ramp[3], ramp[1], ramp[0])
    cells = {}
    for x in range(6, 10):
        for y in range(6, 10):
            corner = (x in (6, 9)) and (y in (6, 9))
            if corner:
                continue
            lit = x + y <= 13
            cells[(x, y)] = glow if (x, y) in ((7, 6), (6, 7)) else gem if lit else dark
    for (x, y) in ((6, 6), (9, 6), (6, 9), (9, 9)):
        cells[(x, y)] = edge
    for (x, y), c in cells.items():
        im.putpixel((x, y), c + (255,))
    return im


def save(im, path):
    target = os.path.join(T, path + '.png')
    os.makedirs(os.path.dirname(target), exist_ok=True)
    im.save(target)


def main():
    save(v9.draw(PEBBLE, {'o': v9.STONE[0], 'd': v9.STONE[1], 'm': v9.STONE[2], 'l': v9.STONE[3], 'h': v9.STONE[4]}), 'item/stone_pebble')
    save(v9.draw(FLINT, {'o': v9.FLINT[0], 'd': v9.FLINT[1], 'm': v9.FLINT[2], 'l': v9.FLINT[3], 'h': v9.FLINT[4]}), 'item/flint_chip')
    for kind, prefix, block in (('astral', 'astral_redstone', 'astralit'), ('nihil', 'nihil_redstone', 'nihilith')):
        save(b7.powder(kind, False, 'B'), f'block/{prefix}')
        save(b7.powder(kind, True, 'B'), f'block/{prefix}_active')
        save(b7.lamp(kind, False, 'B'), f'block/{block}_lamp')
        save(b7.lamp(kind, True, 'B'), f'block/{block}_lamp_active')
        save(switch(kind, False), f'block/{block}_switch')
        save(switch(kind, True), f'block/{block}_switch_active')
    print('ok')


if __name__ == '__main__':
    main()
