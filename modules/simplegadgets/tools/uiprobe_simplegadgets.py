"""simplegadgets-4: HUD, beam, detector, attractor, echo sounder, transposer in the real game picture (a-w4c, scenario for
modules/simpleinterfaces/tools/ui_probe.py).

The GUI stays ON (the gadget HUD is part of it); items go into hotbar slots 1-6 and are selected with the number keys, uses are real mouse
clicks / held buttons (XTest). Parts (creative unless noted):
  1 Gauge (Messuhr): HUD box standing, while sprinting with the real keys (Ctrl+W), and in the air (slow falling, 21 blocks up: altimeter line).
  2 Resonance Rod with Constructor's Touch: hold the right button on an ice block 6 blocks away, picture of beam + measuring HUD, ice must melt.
  3 Attractor (magnet): sneaking disables it - three item entities (pickup delay max) stay while Shift is held, then fly into the inventory.
  4 Transposer (rotator): a piston is turned by real clicks on its top face; a furnace only with Shift + click (known defect, see _rotator).
  5 Echo sounder: linked by a real click on a lodestone, moved 20 blocks away, charged with the held right button (3.6 s) -> jump to the lodestone.
  6 Detector (survival): iron ore 8 blocks ahead is pinged every second, the durability bar drops (data check) - hand and inventory picture.
# NUR BESITZER: sounds of the detector ping / echo charge / rotator ratchet, how the HUD reads while moving fast, particle beam brightness.
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


def _sense(s, cond, slot=26):
    """A server-side test made visible: a stone appears in inventory.<slot> when the condition holds."""
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.3)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def _click(s, after=0.6, hold=0.12, button=3):
    s.io.button(button, True)
    time.sleep(hold)
    s.io.button(button, False)
    time.sleep(after)


def _hand(s, slot, stack):
    s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.3)
    s.io.key(str(slot + 1), 0.4)


def _gauge(s, x, y, z):
    me = (x + 0.5, y, z + 0.5)
    _hand(s, 0, 'simplegadgets:velocity_gauge')
    _aim(s, me, (x + 0.5, y + 1.0, z + 8.5), 1.0)
    time.sleep(1.0)
    s.shot('gauge-hud-standing')
    start = _pos(s)
    s.io.key_down('Control_L')
    s.io.key_down('w')
    try:
        time.sleep(1.8)
        s.shot('gauge-hud-sprinting')
        time.sleep(0.8)
    finally:
        s.io.key_up('w')
        s.io.key_up('Control_L')
    end = _pos(s)
    s.check('gauge: real Ctrl+W moves the player', abs(end[2] - start[2]) >= 8, f'{start} -> {end}')
    s.chat('effect give @s minecraft:slow_falling 30 0 true', 0.3)
    s.chat(f'tp @s {end[0] + 0.5} {end[1] + 21} {end[2] + 0.5} 0 35', 0.4)
    time.sleep(0.8)
    s.shot('gauge-hud-altimeter')
    s.chat('effect clear @s', 0.3)


def _rod(s, x, y, z):
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'tp @s {me[0]} {y} {me[2]} 0 0', 1.2)
    s.chat(f'setblock {x} {y} {z + 6} minecraft:ice', 0.4)
    _hand(s, 1, 'simplegadgets:amethyst_lens[enchantments={"simplebuilding:constructors_touch":1}]')
    _aim(s, me, (x + 0.5, y + 0.5, z + 6.5), 0.9)
    s.io.button(3, True)
    try:
        time.sleep(1.5)
        s.shot('rod-beam')
        time.sleep(2.5)
    finally:
        s.io.button(3, False)
    time.sleep(0.4)
    melted = _sense(s, f'unless block {x} {y} {z + 6} minecraft:ice')
    s.check('rod: the held beam melts the ice block 6 blocks away', melted, 'ice gone')
    s.shot('rod-after')


def _magnet(s, x, y, z):
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'tp @s {me[0]} {y} {me[2]} 0 20', 1.2)
    s.chat('kill @e[type=minecraft:item,tag=uipitem]', 0.2)
    _hand(s, 2, 'simplegadgets:magnet')
    s.io.key_down('Shift_L')  # sneaking switches the magnet off
    try:
        for dx, dz in ((0.5, 3.4), (-0.6, 3.0), (1.6, 3.2)):
            # PickupDelay 32767 is Vanilla's "never pick up" mark: the attractor leaves such items alone by design (AttractorFilter
            # NEVER_PICK_UP, display items of other mods); a long but finite delay keeps them lying while sneaking (the magnet resets it)
            s.chat(f'summon minecraft:item {x + 0.5 + dx} {y + 0.2} {z + 0.5 + dz} {{Item:{{id:"minecraft:cobblestone",count:4}},PickupDelay:600s,Tags:["uipitem"]}}', 0.3)
        time.sleep(1.0)
        still = _sense(s, 'if entity @e[type=minecraft:item,tag=uipitem,distance=..6]')
        s.check('magnet: sneaking switches it off (items stay)', still, '3 cobblestone stacks lying')
        s.shot('magnet-items-lying')
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.3)
    s.shot('magnet-pulling')
    time.sleep(1.2)
    inv = s.st().get('inventory', [])
    got = sum(int(v.rsplit(' x', 1)[1]) for v in inv if v.startswith('minecraft:cobblestone'))
    s.check('magnet in the hand pulls the loose items into the inventory', got >= 12, f'{got} cobblestone in the inventory')


def _rotator(s, x, y, z):
    """A piston (no use action of its own) turns with a plain click on its top face. A furnace answers a right click itself (opens its
    menu), so the rotator only reaches it while sneaking - documented as a known defect in RotatorTests ("Known defect"): the probe
    turns the furnace with Shift + click and only reports what a plain click does."""
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'setblock {x} {y} {z + 3} minecraft:piston[facing=north]', 0.4)
    s.chat(f'setblock {x + 2} {y} {z + 3} minecraft:furnace[facing=north]', 0.4)
    _hand(s, 3, 'simplegadgets:rotator')
    _aim(s, me, (x + 0.5, y + 0.99, z + 3.5), 0.9)
    _click(s, after=0.7)
    turned = _sense(s, f'unless block {x} {y} {z + 3} minecraft:piston[facing=north]')
    s.check('transposer: a real click on the top face turns the piston', turned, 'facing changed from north')
    s.shot('rotator-turned')
    _click(s, after=0.7)
    _click(s, after=0.7)
    s.shot('rotator-turned-3x')
    # the furnace: plain click (reported only), then Shift + click
    _aim(s, me, (x + 2.5, y + 0.99, z + 3.5), 0.9)
    _click(s, after=0.7)
    plain = s.st().get('screen', 'none')
    s.close()
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.3)
        _click(s, after=0.7)
    finally:
        s.io.key_up('Shift_L')
    turned = _sense(s, f'unless block {x + 2} {y} {z + 3} minecraft:furnace[facing=north]')
    s.check('transposer: Shift + click turns the furnace (a plain click opens its menu, known defect)', turned,
            f'facing changed from north; plain click opened {plain.rsplit(".", 1)[-1]}')
    s.shot('rotator-furnace-sneak')


def _echo(s, x, y, z):
    lx, lz = x + 10, z + 10
    s.chat(f'setblock {lx} {y} {lz} minecraft:lodestone', 0.4)
    _hand(s, 4, 'simplegadgets:echo_sounder')
    _aim(s, (lx - 1.5, y, lz + 0.5), (lx + 0.5, y + 0.99, lz + 0.5), 1.0)
    _click(s, after=0.8)
    linked = _sense(s, 'if data entity @s Inventory[{Slot:4b}].components."minecraft:lodestone_tracker"')
    s.check('echo sounder: a real click on the lodestone links it', linked, 'lodestone_tracker component')
    time.sleep(5.5)  # the attempt lock after linking
    s.chat(f'tp @s {lx - 20.5} {y} {lz + 0.5} 0 0', 1.6)
    s.io.button(3, True)
    try:
        time.sleep(2.0)
        s.shot('echo-charging')
        time.sleep(1.8)
    finally:
        s.io.button(3, False)
    time.sleep(1.2)
    end = _pos(s)
    s.check('echo sounder: the charged jump lands at the linked lodestone', abs(end[0] - lx) <= 2 and abs(end[2] - lz) <= 2, f'now {end}, lodestone ({lx}, {y}, {lz})')
    s.shot('echo-arrived')


def _detector(s, x, y, z):
    s.chat('gamemode survival @s', 0.6)
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'tp @s {me[0]} {y} {me[2]} 0 0', 1.2)
    s.chat(f'setblock {x} {y} {z + 8} minecraft:iron_ore', 0.3)
    s.chat(f'setblock {x + 3} {y} {z + 8} minecraft:diamond_ore', 0.3)
    _hand(s, 5, 'simplegadgets:detector')
    _aim(s, me, (x + 0.5, y + 0.5, z + 8.5), 0.9)
    time.sleep(3.0)  # a ping every second finds the iron ore (default mode) and costs one durability point each
    s.shot('detector-hand')
    worn = _sense(s, 'if data entity @s Inventory[{Slot:5b}].components."minecraft:damage"')
    s.check('detector: pings cost durability (damage component after a few seconds)', worn, 'minecraft:damage on the detector')
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    slot = s.slot_xy(st, cslot=5, own=True)
    if slot and 'detector' in slot.get('stack', ''):
        s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
        time.sleep(0.4)
        s.io.move(*s.abs(st, slot['x'], slot['y']))
        time.sleep(0.9)
        s.shot('detector-inventory')
    else:
        s.check('inventory shows the detector in hotbar slot 6', False, slot)
    s.close()
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)


def gadgets(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    s.io.move(*s.abs(s.st(), 640, 360))
    try:
        for part in (_gauge, _rod, _magnet, _rotator, _echo, _detector):
            try:
                part(s, x, y, z)
            except Exception as e:  # one failing part must not stop the others
                s.check(f'{part.__name__.strip("_")} part ran without exception', False, repr(e)[:300])
            s.close()
            s.io.button(3, False)
            s.io.key_up('Shift_L')
            s.chat('effect clear @s', 0.3)
    finally:
        s.chat('kill @e[type=minecraft:item,tag=uipitem]', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


GADGETS = ['velocity_gauge', 'amethyst_lens', 'magnet', 'rotator', 'echo_sounder', 'detector']


def items(s):
    """Module smoke in the picture (simplegadgets has no client smoke): the six gadgets in the hotbar, the survival inventory with every
    tooltip under the real pointer, each gadget in the hand (first person and third person front). Check: all six are real items in the
    hotbar slots the inventory screen shows."""
    s.chat('time set day', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('gamemode survival @s', 0.6)
    for i, g in enumerate(GADGETS):
        s.chat(f'item replace entity @s hotbar.{i} with simplegadgets:{g}', 0.25)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    shown = [s.slot_xy(st, cslot=i, own=True) for i in range(len(GADGETS))]
    names = [(sl or {}).get('stack', '') for sl in shown]
    s.check('inventory screen shows the six gadgets in hotbar slots 1-6', all(n.startswith(f'simplegadgets:{g}') for n, g in zip(names, GADGETS)), names)
    s.shot('inventory')
    for g, sl in zip(GADGETS, shown):
        if sl and sl.get('stack'):
            s.io.move(*s.abs(st, sl['x'] + 3, sl['y'] + 3))
            time.sleep(0.3)
            s.io.move(*s.abs(st, sl['x'], sl['y']))
            time.sleep(0.8)
            s.shot(f'tooltip-{g}')
    s.close()
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 10', 1.0)
    for i, g in enumerate(GADGETS):
        s.io.key(str(i + 1), 0.6)
        s.shot(f'hand-{g}')
    s.key('F5', 0.5)
    s.key('F5', 0.5)
    for i, g in enumerate(GADGETS):
        s.io.key(str(i + 1), 0.6)
        s.shot(f'front-{g}')
    s.key('F5', 0.5)
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplegadgets': gadgets, 'simplegadgets/items': items}
