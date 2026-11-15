"""simpleanimals-1: every animal and every coat variant in the real game picture (b-client60p, scenario for modules/simpleinterfaces/tools/ui_probe.py).

Scene (creative, GUI hidden with F1 for the pictures): one row per kind on the flat world, the variants of a kind side by side, all facing the
camera (NoAI, Silent, Invulnerable, PersistenceRequired, summoned with their stored `Variant` index - the catalogue order of AnimalVariants:
deer red/fallow, boar dark/sandy/frost/rust, hedgehog brown/desert/whitebreast/fourtoed, crab red/coral; zebra and zorse have no variant).
A summon with NBT skips finalizeSpawn, so the stored index stays (no biome pick).
Checks: per kind every variant is there with its index (server data), and a real right click with the deer spawn egg on the grass spawns a deer.
Pictures: overview of all rows, close-up of the small kinds (hedgehog, crab), close-up of deer/boar, zebra + zorse, the egg-spawned deer.
# NUR BESITZER: whether each coat reads as its biome (look), the models/animations (NoAI stands still), the sizes against the player.
"""
import math
import time

KINDS = [('deer', 2), ('boar', 4), ('hedgehog', 4), ('crab', 2), ('zebra', 0), ('zorse', 0)]


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _aim(s, f, t, wait=0.8):
    """Stand at feet point f and look at point t (eye height 1.62, like a crosshair)."""
    dx, dy, dz = t[0] - f[0], t[1] - (f[1] + 1.62), t[2] - f[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    s.chat(f'tp @s {f[0]:.2f} {f[1]} {f[2]:.2f} {yaw:.1f} {pitch:.1f}', wait)


def _sense(s, cond, slot=26):
    """A server-side test made visible: a stone appears in inventory.<slot> when `execute <cond>` holds."""
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.4)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def animals(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('kill @e[tag=uipan]', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    me = (x + 0.5, y, z + 0.5)
    rows = {}
    for row, (kind, variants) in enumerate(KINDS):
        rz = z + 6 + 4 * row
        rows[kind] = rz
        for v in range(max(1, variants)):
            nbt = f'NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:["uipan"]'
            if variants:
                nbt += f',Variant:{v}'
            s.chat(f'summon simpleanimals:{kind} {x - 3.5 + 2.5 * v} {y} {rz + 0.5} {{{nbt}}}', 0.3)
    time.sleep(2.0)
    for kind, variants in KINDS:
        if variants:
            cond = ' '.join(f'if entity @e[type=simpleanimals:{kind},tag=uipan,nbt={{Variant:{v}}}]' for v in range(variants))
        else:
            cond = f'if entity @e[type=simpleanimals:{kind},tag=uipan]'
        s.check(f'{kind}: {"all " + str(variants) + " variants" if variants else "it"} stand in the scene (stored Variant index kept)',
                _sense(s, cond), f'{variants or 1} summoned')
    s.key('F1', 0.5)
    try:
        _aim(s, (x + 0.5, y + 7, z - 3.5), (x + 0.5, y, z + 14.5), 1.5)
        s.shot('overview')
        _aim(s, (x + 0.5, y + 1.5, rows['hedgehog'] - 2.5), (x + 0.5, y, rows['hedgehog'] + 2.5), 1.5)
        s.shot('hedgehogs-and-crabs')
        _aim(s, (x + 0.5, y + 1, z + 1.5), (x + 0.5, y + 0.8, rows['boar'] + 0.5), 1.5)
        s.shot('deer-and-boars')
        _aim(s, (x - 3.5, y + 1, rows['zebra'] - 3.5), (x - 3, y + 0.8, rows['zorse'] + 0.5), 1.5)
        s.shot('zebra-and-zorse')
    finally:
        s.key('F1', 0.5)
    # a real right click with the deer spawn egg on the grass in front (the biome picks the coat: plains -> fallow)
    s.chat('kill @e[type=simpleanimals:deer,tag=!uipan,distance=..40]', 0.3)
    s.chat('item replace entity @s hotbar.0 with simpleanimals:deer_spawn_egg', 0.3)
    s.key('1', 0.3)
    _aim(s, (x + 0.5, y, z - 6.5), (x + 0.5, y - 0.1, z - 3.5), 1.0)
    s.io.right_click()
    time.sleep(1.0)
    spawned = _sense(s, 'if entity @e[type=simpleanimals:deer,tag=!uipan,distance=..8]')
    s.check('real right click with the deer spawn egg spawns a deer', spawned, 'deer without the scene tag near the player')
    s.key('F1', 0.5)
    s.shot('egg-deer')
    s.key('F1', 0.5)
    s.chat('kill @e[type=simpleanimals:deer,tag=!uipan,distance=..40]', 0.3)
    s.chat('kill @e[tag=uipan]', 0.3)
    s.chat('clear @s', 0.3)


SCENARIOS = {'simpleanimals': animals}
