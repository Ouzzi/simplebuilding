"""simplefun-3: the toys of Simple Fun and the brick throw with REAL mouse clicks (a-w4c, scenario for modules/simpleinterfaces/tools/ui_probe.py).

One flat arena (survival, so used-up toys really disappear from the hand), the toy in hotbar slot 1 (key 1), the crosshair aimed with
'/tp ... yaw pitch', then the real right click (XTest button 3) and a picture right after it. Confirmations where the server leaves a trace:
the stack count of the used-up toy, the spawned entity (skipping stone / water balloon / paper plane / floating roll text), the block state
(duck placed, pirate duck on soul sand, campfire doused, glass broken), the data (wolf owner, wand wear).
  1 Party popper (confetti in the dye colour)    2 Dice and 3 coin (result floats over the head, third person)
  4 High five: needs a second player -> NUR BESITZER           5 Petting an own tamed wolf (sneak + empty hand)
  6 Skipping stone over a pond                    7 Water balloon on a lit campfire        8 Bubble wand (hold use)
  9 Paper plane                                   10 Rubber duck (place, pirate duck on soul sand)   11 Snow angel (sneak 3 s on snow, jump)
  12 Brick throw breaks glass (switch /simplefun tweaks bricks breakGlass true during the scene)
# NUR BESITZER: all sounds (popper, dice, coin, duck squeak, balloon, bubbles, high five clap), the high five between two players,
#   the eight "small delights" while eating (flower sniff, cookie crumbs ...), the JEI info pages.
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


def _sense(s, cond, slot=26, reset=True):
    """A server-side test made visible: a stone appears in inventory.<slot> when the condition holds (reset=False: one command only,
    for short-lived things; the slot must then be unused, the final 'clear' cleans up)."""
    if reset:
        s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.3)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def _use(s, after=0.15, hold=0.12, button=3):
    """One real click of the mouse button (3 = use) at the pointer position (window centre)."""
    s.io.button(button, True)
    time.sleep(hold)
    s.io.button(button, False)
    time.sleep(after)


def _hand(s, stack):
    s.chat(f'item replace entity @s hotbar.0 with {stack}', 0.3)
    s.io.key('1', 0.3)


def _count(s, slot=0):
    inv = s.st().get('inventory', [])
    stack = inv[slot] if len(inv) > slot else ''
    return int(stack.rsplit(' x', 1)[1]) if ' x' in stack else 0


def _third_person(s, front=False):
    s.key('F5', 0.5)
    if front:
        s.key('F5', 0.5)

    def back():
        s.key('F5', 0.5)
        if not front:
            s.key('F5', 0.5)
    return back


def toys(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    me = (x + 0.5, y, z + 0.5)
    s.io.move(*s.abs(s.st(), 640, 360))
    s.chat('kill @e[tag=uipfun]', 0.2)
    s.key('F1', 0.5)
    try:
        # 1 party popper in red
        _hand(s, 'simplefun:party_popper[dyed_color=11546150] 3')
        _aim(s, me, (x + 0.5, y + 1.6, z + 5.5), 0.8)
        _use(s, after=0.35)
        s.shot('party-popper')
        s.check('party popper used up (3 -> 2)', _count(s) == 2, s.st().get('inventory', [''])[0])

        # 2 dice, 3 coin: the result floats above the head, so third person
        for slot, (name, item) in ((25, ('dice', 'simplefun:dice 2')), (24, ('coin', 'simplefun:lucky_coin 2'))):
            _hand(s, item)
            _aim(s, me, (x + 0.5, y + 1.6, z + 5.5), 0.8)
            back = _third_person(s)
            _use(s, after=0.1)
            shown = _sense(s, 'if entity @e[type=minecraft:text_display,distance=..4]', slot, reset=False)  # the text lives 2 s
            s.shot(f'{name}-roll')
            s.check(f'{name}: the roll text floats above the head', shown, 'text_display within 4 blocks')
            back()
            time.sleep(1.5)

        # 5 petting: an own tamed wolf, sneak + empty hand
        s.chat(f'summon minecraft:wolf {x + 0.5} {y} {z + 2.5} {{NoAI:1b,Tags:["uipfun"]}}', 0.5)
        s.chat('data modify entity @e[type=minecraft:wolf,tag=uipfun,limit=1] Owner set from entity @s UUID', 0.6)
        owned = _sense(s, 'if data entity @e[type=minecraft:wolf,tag=uipfun,limit=1] Owner', 23, reset=False)
        s.check('petting: the wolf has its owner set', owned, 'Owner present')
        _hand(s, 'minecraft:air')
        s.io.key('2', 0.3)  # an empty hotbar slot
        _aim(s, me, (x + 0.5, y + 0.6, z + 2.5), 0.8)
        s.io.key_down('Shift_L')
        try:
            time.sleep(0.4)
            _use(s, after=0.3)
            s.shot('petting-hearts')
        finally:
            s.io.key_up('Shift_L')
        s.chat('kill @e[tag=uipfun]', 0.3)
        s.io.key('1', 0.3)

        # 6 skipping stone over a pond (9 x 10, two deep)
        s.chat(f'fill {x - 4} {y - 2} {z + 3} {x + 4} {y - 1} {z + 12} minecraft:water', 0.6)
        _hand(s, 'simplefun:skipping_stone 3')
        _aim(s, me, (x + 0.5, y - 0.9, z + 9.5), 0.9)
        _use(s, after=0.15)
        s.shot('skipping-stone')
        s.check('skipping stone thrown (count 3 -> 2)', _count(s) == 2, f'count {_count(s)}')

        # 7 water balloon douses a lit campfire
        s.chat(f'setblock {x - 3} {y} {z + 2} minecraft:campfire[lit=true]', 0.5)
        _hand(s, 'simplefun:water_balloon 3')
        _aim(s, me, (x - 2.5, y + 0.3, z + 2.5), 0.9)
        s.shot('balloon-before')
        _use(s, after=0.9)
        s.shot('balloon-after')
        doused = _sense(s, f'if block {x - 3} {y} {z + 2} minecraft:campfire[lit=false]', 22, reset=False)
        s.check('water balloon douses the campfire and is used up (3 -> 2)', doused and _count(s) == 2, f'doused {doused}, count {_count(s)}')

        # 8 bubble wand: hold the right button
        _hand(s, 'simplefun:bubble_wand')
        _aim(s, me, (x + 0.5, y + 3.0, z + 8.5), 0.8)
        s.io.button(3, True)
        try:
            time.sleep(3.6)
            s.shot('bubble-wand')
        finally:
            s.io.button(3, False)
        worn = _sense(s, 'if data entity @s Inventory[{Slot:0b}].components."minecraft:damage"', 21, reset=False)
        s.check('bubble wand wears while blowing', worn, 'damage component on the wand')
        time.sleep(0.5)

        # 9 paper plane
        _hand(s, 'simplefun:paper_plane 3')
        _aim(s, me, (x + 0.5, y + 4.0, z + 12.5), 0.8)
        _use(s, after=0.2)
        s.shot('paper-plane')
        glide = _sense(s, 'if entity @e[type=simplefun:paper_plane,distance=..80]', 20, reset=False)
        s.check('paper plane glides (entity in the air, count 3 -> 2)', glide and _count(s) == 2, f'entity {glide}, count {_count(s)}')

        # 10 rubber duck: place with the real click on the ground, again on soul sand (pirate duck)
        s.chat(f'setblock {x + 2} {y - 1} {z + 2} minecraft:soul_sand', 0.4)
        _hand(s, 'simplefun:rubber_duck 3')
        _aim(s, me, (x + 0.5, y - 0.01, z + 2.5), 0.8)
        _use(s, after=0.6)
        placed = _sense(s, f'if block {x} {y} {z + 2} simplefun:rubber_duck', 19, reset=False)
        s.check('rubber duck placed with the real click', placed, 'block at +2 south')
        _aim(s, me, (x + 2.5, y - 0.01, z + 2.5), 0.8)
        _use(s, after=0.6)
        pirate = _sense(s, f'if block {x + 2} {y} {z + 2} simplefun:rubber_duck[pirate=true]', 18, reset=False)
        s.check('rubber duck on soul sand becomes the pirate duck', pirate, 'pirate=true')
        _aim(s, me, (x + 1.5, y + 0.2, z + 3.2), 0.8)
        s.shot('rubber-ducks')
        s.io.key('2', 0.3)  # empty hand: click the duck, it squeaks and hops
        _aim(s, me, (x + 0.5, y + 0.2, z + 2.5), 0.8)
        _use(s, after=0.1)
        s.shot('duck-squeak')
        s.io.key('1', 0.3)

        # 11 snow angel: sneak still on snow for 3 s, then jump
        s.chat(f'fill {x - 3} {y - 1} {z + 8} {x + 3} {y - 1} {z + 13} minecraft:snow_block', 0.5)
        s.chat(f'tp @s {x + 0.5} {y} {z + 10.5} 0 0', 1.2)
        _hand(s, 'minecraft:air')
        s.io.key_down('Shift_L')
        try:
            time.sleep(3.8)
            s.io.key('space', 0.2)
        finally:
            s.io.key_up('Shift_L')
        time.sleep(0.8)
        s.chat('effect give @s minecraft:slow_falling 20 0 true', 0.3)
        s.chat(f'tp @s {x + 0.5} {y + 5} {z + 10.5} 0 90', 1.0)
        s.shot('snow-angel')
        s.chat('effect clear @s', 0.3)

        # 12 brick throw breaks glass (switch on for this scene only)
        s.chat('simplefun tweaks bricks breakGlass true', 0.8)
        s.chat(f'setblock {x} {y + 1} {z - 3} minecraft:glass', 0.4)
        # N60P: z+3.5 is inside the water pool of scene 6 (z+3..z+12, two deep): the player sank and threw the brick into the pool's
        # wall (run 1: both screenshots show the dirt wall under water, count 3 -> 2, glass stays). Stand on the grass at z+1.5.
        s.chat(f'tp @s {x + 0.5} {y} {z + 1.5} 0 0', 1.0)
        _hand(s, 'minecraft:brick 3')
        _aim(s, (x + 0.5, y, z + 1.5), (x + 0.5, y + 1.5, z - 2.5), 0.9)
        s.shot('brick-glass-before')
        _use(s, after=1.0)
        s.shot('brick-glass-after')
        broken = _sense(s, f'if block {x} {y + 1} {z - 3} minecraft:air', 17, reset=False)
        s.check('thrown brick breaks the glass (breakGlass on)', broken, f'count {_count(s)}')
    finally:
        s.io.button(3, False)
        s.io.key_up('Shift_L')
        s.key('F1', 0.5)
        s.chat('simplefun tweaks bricks breakGlass false', 0.5)
        s.chat('effect clear @s', 0.3)
        s.chat('kill @e[tag=uipfun]', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplefun': toys}
