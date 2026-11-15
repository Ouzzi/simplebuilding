"""simplefarming-1: swinging through grass with Sharpness I / V, with and without Sweeping Edge, with the REAL left mouse button
(a-w4c, scenario for modules/simpleinterfaces/tools/ui_probe.py).

Scene (survival, so a left click on grass really breaks it and a click on the cow really attacks): the player stands at the south
edge of a 7x5 short-grass field, one "blocker" grass block sits in front of a silent, invulnerable, AI-less cow, the crosshair is put
on the cow's legs (so the ray crosses the blocker's outline). The cut area is the 3x3 around the cow (radius 1, +1 per Sweeping Edge level):
    A = in the 3x3 of the cow         -> gone after the cow was hit (every variant)
    B = only in the 5x5 (Sweeping I)  -> gone only WITH Sweeping Edge
    C = outside both (control)        -> stays in every variant
Per variant: a real left click (it either swings through the grass - chance 10 % at Sharpness I, 90 % at V: the outline vanishes and the cow is
hit - or just breaks the blocker; the next click, up to 3 more, then hits the cow), picture after (and before for the first variant).
The chance of the first swing is only reported in the detail (information), the checks are the deterministic cut areas A/B/C.
# NUR BESITZER: whether the plant outline really disappears for a passing swing (visible only live as a flickering outline, 0.5 s per roll).
"""
import math
import time


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _aim(s, f, t, wait=0.8):
    """Stand at feet point f and look at point t (eye height 1.62, like a crosshair)."""
    dx, dy, dz = t[0] - f[0], t[1] - (f[1] + 1.62), t[2] - f[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    s.chat(f'tp @s {f[0]:.2f} {f[1]} {f[2]:.2f} {yaw:.1f} {pitch:.1f}', wait)


def _lclick(s, hold=0.1, wait=0.8):
    """One real left click at the current pointer position (the pointer stays over the window centre)."""
    s.io.button(1, True)
    time.sleep(hold)
    s.io.button(1, False)
    time.sleep(wait)


def _sense(s, conds):
    """Server-side block tests made visible in the inventory: every true condition puts a stone into inventory.26, .25, ..."""
    s.chat('clear @s minecraft:stone', 0.2)
    for i, cond in enumerate(conds):
        s.chat(f'execute {cond} run item replace entity @s inventory.{26 - i} with minecraft:stone 1', 0.2)
    inv = s.st().get('inventory', [])
    return [len(inv) > 35 - i and inv[35 - i].startswith('minecraft:stone') for i in range(len(conds))]


def _variant(s, name, sword, base, expect_b_gone, before=False):
    x, y, z = base
    grass = 'minecraft:short_grass'
    s.chat(f'fill {x - 3} {y} {z + 2} {x + 3} {y} {z + 6} {grass}', 0.3)
    s.chat(f'item replace entity @s hotbar.0 with minecraft:diamond_sword[enchantments={sword}]', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.9} ~ ~', 0.8)
    if before:
        s.shot(f'{name}-before')
    _lclick(s)
    # information only: after the very first click A is gone exactly when the swing passed through the grass (chance 10 % / 90 %)
    first = _sense(s, [f'unless block {x + 1} {y} {z + 4} {grass}'])[0]
    clicks = 1
    while not first and clicks < 4:  # the first click broke the blocker; the next swing hits the cow
        _lclick(s)
        clicks += 1
    time.sleep(0.5)
    s.shot(f'{name}-after')
    a, c = _sense(s, [f'unless block {x + 1} {y} {z + 4} {grass}', f'unless block {x + 3} {y} {z + 6} {grass}'])
    # ring B = every field cell exactly two blocks (Chebyshev) from the cow's block (x, z + 3): rows z + 2 .. z + 5 of the field, 11 cells
    ring = [(dx, dz) for dx in range(-2, 3) for dz in range(-1, 3) if max(abs(dx), abs(dz)) == 2]
    gone = _sense(s, [f'unless block {x + dx} {y} {z + 3 + dz} {grass}' for dx, dz in ring])
    cut = [cell for cell, g in zip(ring, gone) if g]
    # where the cow stands (block z offset 2 / 3 / 4): the cut is centred on target.blockPosition()
    cow = _sense(s, [f'if entity @e[tag=uipfarm,x={x},y={y},z={z + dz},dx=0,dy=0,dz=0]' for dz in (2, 3, 4)])
    cow_at = [dz for dz, here in zip((2, 3, 4), cow) if here]
    s.check(f'{name}: 3x3 around the cow cut (A gone)', a, f'clicks {clicks}, first swing passed through the grass: {first}')
    # N60P: the cut only changes blocks within InteractionGuard.MAX_REACH = 4.5 of the player's EYE (cell centre; anti-cheat, every
    # farming action). From the feet point (x+0.5, z+0.9) the far row z+5 (5 ring cells) lies 4.6+ away: run 1 cut exactly the 6 ring
    # cells in reach in both Sweeping variants. Ring B = the cells in reach are cut, the ones beyond reach stay.
    def in_reach(dx, dz):
        return math.dist((x + 0.5, y + 1.62, z + 0.9), (x + dx + 0.5, y + 0.5, z + 3 + dz + 0.5)) <= 4.5
    want = sorted(cell for cell in ring if in_reach(*cell)) if expect_b_gone else []
    s.check(f'{name}: 5x5 ring B {"cut where in reach (Sweeping Edge, 4.5-block guard)" if expect_b_gone else "stays (no Sweeping Edge)"}',
            sorted(cut) == want,
            f'{len(cut)}/{len(ring)} ring cells cut (dx, dz from the cow block z+3): {cut}, expected {want}; cow box touches block z+{cow_at}')
    s.check(f'{name}: control C outside the area stays', not c, f'C gone = {c}')


def sharpness_through_grass(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('gamemode survival @s', 0.6)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    base = _pos(s)
    x, y, z = base
    s.chat('kill @e[type=minecraft:cow,tag=uipfarm]', 0.2)
    s.chat(f'summon minecraft:cow {x + 0.5} {y} {z + 3.5} {{NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Tags:["uipfarm"],Rotation:[180f,0f]}}', 0.5)
    s.chat(f'fill {x - 3} {y} {z + 2} {x + 3} {y} {z + 6} minecraft:short_grass', 0.3)
    s.chat('item replace entity @s hotbar.0 with minecraft:diamond_sword', 0.3)
    s.io.key('1', 0.3)
    # crosshair on the cow's legs: the ray from the eye crosses the outline of the grass block in front of it
    _aim(s, (x + 0.5, y, z + 0.9), (x + 0.5, y + 0.3, z + 3.5), 1.0)
    s.io.move(*s.abs(s.st(), 640, 360))
    s.key('F1', 0.6)
    try:
        # the synced config is the default one here: min 10 %, max 90 %, sweeping radius +1 per level
        _variant(s, 'sharpness1', '{"minecraft:sharpness":1}', base, False, before=True)
        _variant(s, 'sharpness5', '{"minecraft:sharpness":5}', base, False)
        _variant(s, 'sharpness1-sweeping1', '{"minecraft:sharpness":1,"minecraft:sweeping_edge":1}', base, True)
        _variant(s, 'sharpness5-sweeping1', '{"minecraft:sharpness":5,"minecraft:sweeping_edge":1}', base, True)
    finally:
        s.key('F1', 0.6)
        s.chat('kill @e[type=minecraft:cow,tag=uipfarm]', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplefarming': sharpness_through_grass}
