"""simpletrims-5: inverted trim (3 stages) and pulsating trim on armor stands in the real game picture (a-w4c, scenario for
modules/simpleinterfaces/tools/ui_probe.py).

Five armor stands in a row (iron armor, gold 'sentry' trim; pattern of ModuleClientSmoke): plain trim as reference, inverted trim stage 1, 2, 3
('simpletrims:inverted_trim'), and pulsating + glowing ('simpletrims:pulsating', 'simpletrims:glow_level'). Each stand is summoned in one
command with its four armor pieces; a server data test per stand checks that the component survived loading on the chest piece
(equipment.chest.components), then pictures: day (all five, GUI hidden), close-up of the inverted trims, night (the pulsating one glows;
two pictures 1.3 s apart because it pulses).
The JEI pages (smithing recipes, in-world tab) are not part of this probe.
# NUR BESITZER: whether the inverted stages 1-3 read as a negative of the pattern and differ enough; how the pulse feels over time; JEI pages.
"""
import math
import time

STANDS = (('plain', -3.6, ''),
          ('inverted1', -1.8, ',"simpletrims:inverted_trim":1'),
          ('inverted2', 0.0, ',"simpletrims:inverted_trim":2'),
          ('inverted3', 1.8, ',"simpletrims:inverted_trim":3'),
          ('pulsating', 3.6, ',"simpletrims:glow_level":1,"simpletrims:pulsating":true'))
PIECES = (('head', 'iron_helmet'), ('chest', 'iron_chestplate'), ('legs', 'iron_leggings'), ('feet', 'iron_boots'))


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _aim(s, f, t, wait=0.8):
    """Stand at feet point f and look at point t (eye height 1.62, like a crosshair)."""
    dx, dy, dz = t[0] - f[0], t[1] - (f[1] + 1.62), t[2] - f[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    s.chat(f'tp @s {f[0]:.2f} {f[1]} {f[2]:.2f} {yaw:.1f} {pitch:.1f}', wait)


def _sense(s, cond, slot=26):
    """A server-side test made visible: a stone appears in inventory.<slot> when the condition holds."""
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.3)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def _stand_command(x, y, z, name, dx, extra):
    trim = '"minecraft:trim":{material:"minecraft:gold",pattern:"minecraft:sentry"}' + extra
    equipment = ','.join(f'{slot}:{{id:"minecraft:{item}",count:1,components:{{{trim}}}}}' for slot, item in PIECES)
    return (f'summon minecraft:armor_stand {x + 0.5 + dx} {y} {z + 4.5} '
            f'{{Rotation:[180f,0f],ShowArms:1b,NoBasePlate:1b,Tags:["uiptrim","uipt_{name}"],equipment:{{{equipment}}}}}')


def trims(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    me = (x + 0.5, y, z + 0.5)
    s.chat('kill @e[type=minecraft:armor_stand,tag=uiptrim]', 0.3)
    s.key('F1', 0.5)
    try:
        for name, dx, extra in STANDS:
            s.chat(_stand_command(x, y, z, name, dx, extra), 0.4)
        for name, _dx, extra in STANDS[1:]:
            comp = '"simpletrims:pulsating"' if name == 'pulsating' else '"simpletrims:inverted_trim"'
            ok = _sense(s, f'if data entity @e[type=minecraft:armor_stand,tag=uipt_{name},limit=1] equipment.chest.components.{comp}')
            s.check(f'armor stand "{name}" keeps its component on the chest piece', ok, comp)
        _aim(s, me, (x + 0.5, y + 1.0, z + 4.5), 1.0)
        s.shot('trims-day')
        _aim(s, (x + 0.5, y, z + 1.8), (x + 0.5, y + 1.1, z + 4.5), 0.9)
        s.shot('trims-inverted-closeup')
        _aim(s, me, (x + 0.5, y + 1.0, z + 4.5), 0.9)
        s.chat('time set midnight', 1.2)
        time.sleep(1.0)
        s.shot('trims-night')
        time.sleep(1.3)
        s.shot('trims-night-later')
    finally:
        s.key('F1', 0.5)
        s.chat('time set day', 0.3)
        s.chat('kill @e[type=minecraft:armor_stand,tag=uiptrim]', 0.3)


SCENARIOS = {'simpletrims': trims}
