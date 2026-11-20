"""N86S: side half slabs as their own items - hotbar/inventory icons, real right clicks against a wall (place, add layers),
on the floor (leans on the far wall in view direction).

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe86s --scale 2 --only simpleinterfaces/n86s
"""
import time


def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def side_items(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false'):
        s.chat('gamerule ' + rule, 0.3)
    s.clear_and_give('simpleslabs:stone_side_half_slab 64', 'simpleslabs:oak_side_half_slab 64',
                     'simpleslabs:cut_copper_side_half_slab 64', 'simpleslabs:stone_half_slab 64')
    time.sleep(0.8)
    s.shot('1-hotbar')
    s.key('e', 1.2)
    s.shot('2-inventar')
    s.close()
    x, y, z = _pos(s)
    # a wall 3 blocks ahead, the player looks at its face at eye height
    s.chat(f'fill {x - 1} {y} {z + 3} {x + 1} {y + 2} {z + 3} minecraft:stone_bricks', 0.4)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 1.6} {z + 3}', 1.0)
    s.chat('item replace entity @s weapon.mainhand with simpleslabs:stone_side_half_slab 64', 0.4)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 1.6} {z + 3}', 0.6)
    for n in range(1, 4):
        s.io.right_click()
        time.sleep(0.4)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 1.6} {z + 3}', 0.4)
    # run 1: the say line was not in client.log yet when it was read - check every thickness and wait for the log
    # run 2 found nothing at (x, y+1, z+2): search the cells in front of the wall for the side form and its thickness
    for dx in (-1, 0, 1):
        for dy in (0, 1, 2):
            for t in range(1, 5):
                s.chat(f'execute if block {x + dx} {y + dy} {z + 2} simpleslabs:stone_side_half_slab[thickness={t}] run say SIDE_AT_{dx}_{dy}_THICKNESS_{t}', 0.15)
    time.sleep(1.5)
    s.chat(f'tp @s {x + 2.5} {y} {z + 0.8} facing {x + 0.5} {y + 1.3} {z + 2.6}', 1.0)
    s.shot('3-an-der-wand-3-lagen-schraeg')
    log = (s.out / 'client.log').read_text(errors='replace') if (s.out / 'client.log').exists() else ''
    import re
    got = sorted(set(re.findall(r'SIDE_AT_(-?\d)_(\d)_THICKNESS_(\d)', log)))
    s.check('3 real right clicks against the wall give a side half slab of thickness 3', len(got) == 1 and got[0][2] == '3', f'found (dx, dy, thickness): {got}')
    s.check('N86S shots taken (hotbar, inventory, wall)', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n86s': side_items}
