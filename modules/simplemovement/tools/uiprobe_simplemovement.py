"""simplemovement-4: Auto Walk key and the two enchantments Step Height (boots) and Sprint (leggings) with REAL keys
(a-w4c, scenario for modules/simpleinterfaces/tools/ui_probe.py).

Auto Walk: the key is UNBOUND by default (MovementClient: InputConstants.UNKNOWN). Because the driver starts the client right after loading the
scenario files, this file binds it to G (and sets autoJump:false) in <run dir>/options.txt at import time (only when that file already holds a
'version:' line, so the data fixer is not confused; the game writes the lines back itself on exit). '/simplemovement autowalk true' switches the server value on.
Checks: pressing G makes the position change on its own, pressing G again stops it.
Step Height: a 1-block stone step and a 1.5-block fence field (both 9 blocks deep, so the player still stands on them when y is read); real W key held. No boots: blocked; Step Height I: up the stone, not the fence;
Step Height II: up the fence (checked by the block position's y). Sprint: real Ctrl+W for 3.5 s on the flat world with plain leggings and
with Sprint III leggings: the distance must be clearly longer (+30 % expected).
Pictures: Auto Walk running, standing on the wall in third person, enchanted boots and leggings from the front (glint).
# NUR BESITZER: the feel of the stepping and of the +30 % sprint, the click sound of the Auto Walk key.
"""
import math
import time
from pathlib import Path

GAME = Path(__file__).resolve().parents[3] / 'integration/run-fabric-263'
OPTIONS = {'key_key.simplemovement.autowalk': 'key.keyboard.g', 'autoJump': 'false'}


def _set_options():
    """The client reads options.txt while it starts, i.e. after this module was loaded by the driver. Binds Auto Walk to G (it is unbound by
    default) and switches Vanilla's Auto-Jump off (it would jump up the 1-block wall without boots). Only when the file already exists with a
    'version:' line (a fresh file would go through the data fixer); the game writes the values back itself on exit."""
    opt = GAME / 'options.txt'
    try:
        if not opt.is_file():
            return
        lines = opt.read_text().splitlines()
        if not any(l.startswith('version:') for l in lines):
            return
        out = [l for l in lines if l.split(':', 1)[0] not in OPTIONS]
        out += [f'{k}:{v}' for k, v in OPTIONS.items()]
        opt.write_text('\n'.join(out) + '\n')
    except OSError:
        pass


_set_options()


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _run(s, keys, seconds):
    """Holds real keys down for a time (list of X key names)."""
    for k in keys:
        s.io.key_down(k)
    try:
        time.sleep(seconds)
    finally:
        for k in reversed(keys):
            s.io.key_up(k)


def _third_person(s, front=False):
    """F5 once = behind the player, twice = in front; the returned function goes back to first person."""
    s.key('F5', 0.5)
    if front:
        s.key('F5', 0.5)

    def back():
        s.key('F5', 0.5)
        if not front:
            s.key('F5', 0.5)
    return back


def _autowalk(s):
    s.chat('simplemovement autowalk true', 1.5)
    base = _pos(s)
    s.chat(f'tp @s {base[0] + 0.5} {base[1]} {base[2] + 0.5} 0 0', 1.0)
    start = _pos(s)
    s.key('g', 0.2)
    time.sleep(3.0)
    mid = _pos(s)
    moved = abs(mid[2] - start[2]) + abs(mid[0] - start[0])
    s.check('Auto Walk: after pressing G the player walks on its own', moved >= 5, f'{start} -> {mid} ({moved} blocks in ~3 s; key G unbound in options.txt if 0)')
    s.key('F1', 0.4)
    s.shot('autowalk-running')
    s.key('F1', 0.4)
    s.key('g', 0.2)
    time.sleep(1.0)
    stop1 = _pos(s)
    time.sleep(1.2)
    stop2 = _pos(s)
    s.check('Auto Walk: pressing G again stops', stop1 == stop2, f'{stop1} then {stop2}')
    s.chat('simplemovement autowalk false', 0.6)


def _step_runs(s):
    s.chat('gamemode survival @s', 0.6)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    # deep steps: 1.6 s of walking covers ~7 blocks, so a 1-block-thick wall is climbed AND left again before y is read (a-w4c saw y
    # back at the floor and blamed Auto-Jump); the player must still stand on the step when it is measured
    s.chat(f'fill {x - 3} {y} {z + 3} {x + 3} {y} {z + 11} minecraft:stone', 0.4)
    s.chat(f'fill {x - 3} {y} {z + 15} {x + 3} {y} {z + 23} minecraft:oak_fence', 0.4)
    s.chat('clear @s', 0.3)

    def attempt(label, boots, wall_z, expect_up):
        s.chat(f'item replace entity @s armor.feet with {boots}' if boots else 'item replace entity @s armor.feet with minecraft:air', 0.3)
        s.chat(f'tp @s {x + 0.5} {y} {z + wall_z - 2.5} 0 0', 1.0)
        _run(s, ['w'], 1.6)
        time.sleep(0.5)
        end = _pos(s)
        up = end[1]
        s.check(f'Step Height: {label}', (up >= y + 1) == expect_up,
                f'y {y} -> {up} at z {end[2] - z} (step starts at z {wall_z}), expected {"up" if expect_up else "blocked"}')
        return up

    attempt('no boots: blocked by the 1-block stone wall', None, 3, False)
    back = None
    up = attempt('Step Height I: steps up the 1-block wall', 'minecraft:diamond_boots[enchantments={"simplemovement:step_height":1}]', 3, True)
    if up >= y + 1:
        back = _third_person(s)
        s.key('F1', 0.4)
        s.shot('step-height-1-on-wall')
        s.key('F1', 0.4)
        back()
    attempt('Step Height I: blocked by the 1.5-block fence', 'minecraft:diamond_boots[enchantments={"simplemovement:step_height":1}]', 15, False)
    up = attempt('Step Height II: steps up the 1.5-block fence', 'minecraft:diamond_boots[enchantments={"simplemovement:step_height":2}]', 15, True)
    if up >= y + 1:
        back = _third_person(s)
        s.key('F1', 0.4)
        s.shot('step-height-2-on-fence')
        s.key('F1', 0.4)
        back()
    s.chat('item replace entity @s armor.feet with minecraft:air', 0.3)


def _sprint_runs(s):
    s.chat('gamemode survival @s', 0.6)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    dist = {}
    for label, legs in (('plain', None), ('Sprint III', 'minecraft:diamond_leggings[enchantments={"simplemovement:sprint":3}]')):
        s.chat(f'item replace entity @s armor.legs with {legs}' if legs else 'item replace entity @s armor.legs with minecraft:air', 0.3)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.0)
        _run(s, ['Control_L', 'w'], 3.5)
        end = _pos(s)
        dist[label] = abs(end[2] - z)
        time.sleep(0.8)
    ratio = dist['Sprint III'] / dist['plain'] if dist['plain'] else 0
    s.check('Sprint III: sprinting is clearly faster than with plain leggings', ratio >= 1.12, f'plain {dist["plain"]} blocks, Sprint III {dist["Sprint III"]} blocks, ratio {ratio:.2f} (+30 % expected)')
    # the enchanted gear from the front (glint) with Step Height boots too
    s.chat('item replace entity @s armor.feet with minecraft:diamond_boots[enchantments={"simplemovement:step_height":2}]', 0.3)
    s.chat('item replace entity @s armor.chest with minecraft:diamond_chestplate', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.0)
    back = _third_person(s, front=True)
    s.key('F1', 0.4)
    s.shot('enchanted-boots-and-leggings-front')
    s.key('F1', 0.4)
    back()
    s.chat('clear @s', 0.3)


def movement(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('gamemode survival @s', 0.6)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    try:
        _autowalk(s)
        _step_runs(s)
        _sprint_runs(s)
    finally:
        for k in ('w', 'Control_L'):
            s.io.key_up(k)
        s.chat('simplemovement autowalk false', 0.4)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplemovement': movement}
