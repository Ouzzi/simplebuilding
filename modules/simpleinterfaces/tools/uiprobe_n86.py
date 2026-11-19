"""N86 half slabs (b-half86, Simple Slabs, preview N86B): in the flat world, built with setblock and block states (no clicks
needed for the look) - written by the build session, run by the main session (not run here).

Scenes (shot names = German preview blocks):
  1 Materialproben  12 materials, each as 1 layer (2 px), 4 layers (= slab height) and 8 layers (full cell)
  2 Kombinationen   the owner's examples 0-4 + 12-16 px (layers 0,1,6,7) and 0-4 + 8-12 px (0,1,4,5), a single top layer,
                    a 1..8 layer staircase, slab + half slab (layers 0-4)
  3 Seitenformen    side half slab (1 layer) and side slab (4 layers) against a stone wall in all four directions
  4 Kupfer          cut copper half slabs in the four ageing stages + waxed, and the copper octets next to them
  5 Testzentrale    order block, stairs, slab, half slab (stone bricks, oak) and the light grey / red placeholders
  6 Natur           grass half slab (biome tint, grass edge on the top layer only), dirt; a sand half slab dropped from
                    above lands bottom-aligned

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe86 --scale 2 --only simpleinterfaces/n86
"""
import time

LAYERS = 8
MATERIALS = ['oak', 'spruce', 'stone', 'stone_brick', 'deepslate_tile', 'quartz', 'cut_copper', 'white_wool', 'red_concrete',
             'prismarine', 'blackstone', 'mud_brick']


def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _mask(layers):
    return 'waterlogged=false,' + ','.join(f'l{i}={"true" if i in layers else "false"}' for i in range(LAYERS))


def _half(s, x, y, z, material, layers, ns='simpleslabs'):
    s.chat(f'setblock {x} {y} {z} {ns}:{material}_half_slab[{_mask(layers)}]', 0.15)


def _side(s, x, y, z, material, facing, thickness):
    s.chat(f'setblock {x} {y} {z} simpleslabs:{material}_side_half_slab[facing={facing},thickness={thickness},waterlogged=false]', 0.15)


def _cam(s, fx, fy, fz, tx, ty, tz, name, wait=1.5):
    s.chat(f'tp @s {fx} {fy} {fz} facing {tx} {ty} {tz}', wait)
    s.shot(name)


def _sense_block(s, x, y, z, block_state, slot=26):
    """True when the server sees the block state at x y z (execute if block -> a stone in an inventory slot the probe reads)."""
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute if block {x} {y} {z} {block_state} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.4)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def half_slabs(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('time set noon', 0.3)
    s.chat('weather clear', 0.3)
    # 26.x game rule ids are snake_case (N83 run 2)
    s.chat('gamerule advance_time false', 0.3)
    s.chat('gamerule spawn_mobs false', 0.3)
    s.chat('gamerule random_tick_speed 0', 0.3)   # copper stays in its stage for the shot
    s.chat('kill @e[type=minecraft:slime]', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    x, y, z = _pos(s)
    # a clean stage: stone floor 40 x 40 in front of the player
    s.chat(f'fill {x - 20} {y - 1} {z + 2} {x + 20} {y - 1} {z + 42} minecraft:smooth_stone', 0.8)
    s.chat(f'fill {x - 20} {y} {z + 2} {x + 20} {y + 6} {z + 42} minecraft:air', 0.8)

    # 1 Materialproben: x row, z = 1 / 4 / 8 layers
    z1 = z + 6
    for i, m in enumerate(MATERIALS):
        mx = x - 12 + 2 * i
        _half(s, mx, y, z1, m, [0])
        _half(s, mx, y, z1 + 2, m, [0, 1, 2, 3])
        _half(s, mx, y, z1 + 4, m, list(range(LAYERS)))
    s.check('stone half slab placed with 4 layers', _sense_block(s, x - 8, y, z1 + 2, 'simpleslabs:stone_half_slab[l3=true,l4=false]'))

    # 2 Kombinationen (owner examples) at z + 18
    z2 = z + 18
    _half(s, x - 6, y, z2, 'stone', [0, 1, 6, 7])          # 0-4 + 12-16 px
    _half(s, x - 4, y, z2, 'stone', [0, 1, 4, 5])          # 0-4 + 8-12 px
    _half(s, x - 2, y, z2, 'oak', [7])                     # a single top layer
    _half(s, x, y, z2, 'quartz', [0, 2, 4, 6])             # every second layer
    for n in range(1, LAYERS + 1):                         # staircase 1..8 layers
        _half(s, x + 2 + n, y, z2, 'stone_brick', list(range(n)))
    _half(s, x - 8, y, z2, 'stone', [0, 1, 2, 3, 4])       # slab + half slab (what a half slab on a stone slab becomes)
    s.chat(f'setblock {x - 10} {y} {z2} minecraft:stone_slab[type=bottom]', 0.15)   # the slab next to it for comparison
    s.check('owner example 0-4 + 12-16 px', _sense_block(s, x - 6, y, z2, 'simpleslabs:stone_half_slab[l0=true,l1=true,l2=false,l5=false,l6=true,l7=true]'))

    # 3 Seitenformen: a stone pillar per direction with a side half slab (1) and a side slab (4) leaning on it
    z3 = z + 26
    for i, (facing, dx, dz) in enumerate((('north', 0, 1), ('south', 0, -1), ('west', 1, 0), ('east', -1, 0))):
        px = x - 9 + 6 * i
        s.chat(f'fill {px} {y} {z3} {px} {y + 1} {z3} minecraft:stone', 0.15)
        # facing = the side of the cell the form leans on: the cell next to the pillar leans towards it
        _side(s, px + dx, y, z3 + dz, 'oak', facing, 1)
        _side(s, px + dx, y + 1, z3 + dz, 'oak', facing, 4)
    s.check('side slab placed', _sense_block(s, x - 9, y + 1, z3 + 1, 'simpleslabs:oak_side_half_slab[facing=north,thickness=4]'))

    # 4 Kupfer: stages + waxed (half slabs 2 layers), the copper octets on the right
    z4 = z + 32
    for i, p in enumerate(['', 'exposed_', 'weathered_', 'oxidized_', 'waxed_']):
        _half(s, x - 8 + 2 * i, y, z4, p + 'cut_copper', [0, 1])
        s.chat(f'setblock {x + 4 + 2 * i} {y} {z4} simpleoctets:{p}{"copper_block" if p in ("", "waxed_") else "copper"}_octet'
               f'[o000=true,o100=true,o001=true,o101=true]', 0.15)

    # 5 Testzentrale: block, stairs, slab, half slab from the block outwards + the placeholders
    z5 = z + 38
    for j, (block, stairs, slab, half) in enumerate((('stone_bricks', 'stone_brick_stairs', 'stone_brick_slab', 'stone_brick'),
                                                     ('oak_planks', 'oak_stairs', 'oak_slab', 'oak'))):
        rx = x - 8 + 8 * j
        s.chat(f'setblock {rx} {y} {z5} minecraft:{block}', 0.15)
        s.chat(f'setblock {rx + 1} {y} {z5} minecraft:{stairs}[facing=west]', 0.15)
        s.chat(f'setblock {rx + 2} {y} {z5} minecraft:{slab}[type=bottom]', 0.15)
        _half(s, rx + 3, y, z5, half, [0])
    _half(s, x + 9, y, z5, 'light_gray_concrete', [0])
    _half(s, x + 10, y, z5, 'red_concrete', [0])
    s.chat(f'setblock {x + 11} {y} {z5} minecraft:gray_concrete_slab[type=bottom]', 0.15)   # middle line stays a slab

    # 6 Natur: grass stack and a dropped sand half slab (needs Simple Blocks)
    z6 = z + 12
    s.chat(f'fill {x + 14} {y - 1} {z6 - 2} {x + 19} {y - 1} {z6 + 2} minecraft:grass_block', 0.3)
    _half(s, x + 15, y, z6, 'grass', [0])
    _half(s, x + 16, y, z6, 'grass', [0, 1, 2, 3])
    _half(s, x + 17, y, z6, 'grass', [0, 1, 6, 7])
    _half(s, x + 18, y, z6, 'dirt', [0, 1])
    _half(s, x + 16, y + 4, z6 + 2, 'sand', [5, 7])          # floats: falls as 2 layers onto the grass
    time.sleep(2.0)
    s.check('sand half slab fell as 2 bottom layers', _sense_block(s, x + 16, y, z6 + 2, 'simpleslabs:sand_half_slab[l0=true,l1=true,l2=false]'))

    s.key('F1', 0.5)
    try:
        _cam(s, x + 0.5, y + 4, z1 - 4, x + 0.5, y, z1 + 2, '1-materialproben')
        _cam(s, x - 11.5, y + 1.6, z1 + 6, x - 7, y + 0.2, z1 + 1, '1-materialproben-schraeg')
        _cam(s, x - 5, y + 1.2, z2 - 3.5, x - 5, y + 0.5, z2 + 0.5, '2-kombinationen-besitzer-beispiele')
        _cam(s, x + 6.5, y + 2.5, z2 - 5, x + 6.5, y + 0.3, z2 + 0.5, '2-kombinationen-treppe-und-stufe')
        _cam(s, x - 8, y + 1.0, z2 - 3, x - 9, y + 0.4, z2 + 0.5, '2-stufe-plus-halbstufe')
        _cam(s, x + 0.5, y + 4, z3 - 6, x + 0.5, y + 0.8, z3, '3-seitenformen')
        _cam(s, x - 6.5, y + 2, z3 + 4, x - 9, y + 0.8, z3, '3-seitenformen-nah')
        _cam(s, x + 1, y + 3, z4 - 5, x + 1, y, z4 + 0.5, '4-kupfer-stufen')
        _cam(s, x + 0.5, y + 3, z5 - 5, x + 0.5, y + 0.3, z5 + 0.5, '5-testzentrale-reihenfolge')
        _cam(s, x + 16.5, y + 2.2, z6 - 4, x + 16.5, y + 0.3, z6 + 1, '6-natur-gras-sand')
    finally:
        s.key('F1', 0.5)
    s.chat('gamerule random_tick_speed 3', 0.3)
    s.check('N86 half slab shots taken (Materialproben, Kombinationen, Seitenformen, Kupfer, Testzentrale, Natur)', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n86': half_slabs}
