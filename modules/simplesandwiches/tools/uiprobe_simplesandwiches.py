"""simplesandwiches-4: the kitchen in the real game picture with REAL right clicks (a-w4c, scenario for modules/simpleinterfaces/tools/ui_probe.py).

Survival, GUI hidden (F1) for the world pictures. Scenes:
  1 Cutting board: bread laid on (click 1), the knife opens the loaf, butter from the off hand, beef + apple + cheese slice, empty hand closes,
    empty hand takes the sandwich - after every click the board's server data (Stage, Buttered) is tested; pictures per stage, the sandwich in the
    hand and its tooltip (layers, hunger, effects) in the inventory under the real pointer.
  2 A cheese block and a butter block cut with the knife (4 real clicks each: top cut / side cut -> 12 slices), pictures.
  3 Slice layers like carpets (cheese 2/8/14, butter 4/10) and a mixed stack made by a real click (cheese slice on a butter carpet), picture.
  4 Cauldrons: milk stages 0-3, butter and cheese in a row; plus a real milk bucket into an empty cauldron and an empty-hand pick-up of the cheese.
  5 Eating from a bundle: sneak + hold use eats one cooked beef out of a bundle (contents 3 -> 2), picture while eating.
# NUR BESITZER: the 6 cut / cauldron sounds, the Slippery effect on a butter block (felt, not seen), JEI special routes, config screen.
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


def _click(s, key=None, after=0.6, hold=0.12):
    """Selects hotbar slot <key> (1-9) and gives one real right click at the pointer position."""
    if key:
        s.io.key(key, 0.25)
    s.io.button(3, True)
    time.sleep(hold)
    s.io.button(3, False)
    time.sleep(after)


def _count(s, slot):
    stack = s.st().get('inventory', [''] * 9)[slot]
    return int(stack.rsplit(' x', 1)[1]) if ' x' in stack else 0


def _total(s, item):
    """How many of item the whole inventory holds (status strings 'id xN')."""
    n = 0
    for v in s.st().get('inventory', []):
        if v and v.split(' x')[0] == item:
            n += int(v.rsplit(' x', 1)[1]) if ' x' in v else 1
    return n


def _empty_hand(s):
    """Hotbar 9 really empty: the board scene leaves the sandwich there (the empty hand took it), so later "empty hand" clicks would
    hold the sandwich (a-w4c: the cheese cauldron click placed the sandwich in front of the cauldron instead of taking the cheese)."""
    s.chat('item replace entity @s hotbar.8 with minecraft:air', 0.3)


def _board(s, me, x, y, z):
    bx, by, bz = x, y, z + 2
    s.chat(f'setblock {bx} {by} {bz} simplesandwiches:oak_cutting_board[facing=north]', 0.5)
    _aim(s, me, (bx + 0.5, by + 0.19, bz + 0.5), 0.9)
    s.chat('item replace entity @s weapon.offhand with simplesandwiches:butter_slice 2', 0.3)

    def stage(name, buttered=None):
        nbt = f'{{Stage:"{name}"' + (',Buttered:1b' if buttered else '') + '}'
        return _sense(s, f'if data block {bx} {by} {bz} {nbt}')

    _click(s, '1')
    s.check('board: bread laid on (stage loaf, bread 3 -> 2)', stage('loaf') and _count(s, 0) == 2, f'bread {_count(s, 0)}')
    s.shot('board-bread')
    _click(s, '2')
    s.check('board: the knife opens the loaf', stage('open'), 'stage open')
    s.shot('board-open')
    _click(s, '2')
    s.check('board: butter from the off hand (Buttered)', stage('open', True), 'Buttered')
    s.shot('board-buttered')
    for key in ('3', '4', '5'):
        _click(s, key)
    s.check('board: beef, apple and cheese slice taken (3 ingredients)', _count(s, 2) == 0 and _count(s, 3) == 0 and _count(s, 4) == 0, 'hotbar 3-5 empty')
    s.shot('board-filled')
    _click(s, '9')
    s.check('board: the empty hand closes the sandwich', stage('closed', True), 'stage closed')
    s.shot('board-closed')
    _click(s, '9')
    inv = s.st().get('inventory', [])
    got = len(inv) > 8 and inv[8].startswith('simplesandwiches:sandwich')
    s.check('board: the empty hand takes the sandwich', got and stage('empty'), inv[8] if len(inv) > 8 else inv)
    _aim(s, me, (x + 0.5, y - 1.0, z + 2.5), 0.8)
    s.shot('sandwich-in-hand')
    # the tooltip in the inventory under the real pointer
    s.key('F1', 0.5)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    slot = s.slot_xy(st, cslot=8, own=True)
    if slot and 'sandwich' in slot.get('stack', ''):
        s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
        time.sleep(0.4)
        s.io.move(*s.abs(st, slot['x'], slot['y']))
        time.sleep(0.9)
        s.shot('sandwich-tooltip')
    else:
        s.check('inventory shows the sandwich in hotbar slot 9', False, slot)
    s.close()
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    s.key('F1', 0.5)


def _blocks(s, me, x, y, z):
    s.chat(f'setblock {x + 3} {y} {z + 2} simplesandwiches:cheese_block', 0.4)
    s.chat(f'setblock {x - 3} {y} {z + 2} simplesandwiches:butter_block', 0.4)
    s.io.key('2', 0.3)
    # the off hand still holds butter slices from the board: cut slices would go there (placeItemBackInInventory) and not be counted
    s.chat('item replace entity @s weapon.offhand with minecraft:air', 0.3)
    for block, bx, aim_at, face in (('cheese', x + 3, (x + 3.5, y + 0.99, z + 2.5), 'up'),       # top face of the cheese block
                                    ('butter', x - 3, (x - 2.5, y + 0.5, z + 2.01), 'north')):  # north face of the butter block
        _aim(s, me, aim_at, 0.8)
        hit = s.st().get('hitBlock')
        before = _total(s, f'simplesandwiches:{block}_slice')
        # the first right click(s) after the inventory screen closed can get lost (b60: cheese 2 cuts from 4 clicks): click until four slices
        # arrived, at most 8 clicks; every cut must give exactly one slice and the block must show 16 - 4
        clicks, got = 0, 0
        while got < 4 and clicks < 8:
            _click(s, None, after=0.5)
            clicks += 1
            got = _total(s, f'simplesandwiches:{block}_slice') - before
        want = f'slices=12,cut={face}'
        if _sense(s, f'if block {bx} {y} {z + 2} simplesandwiches:{block}_block[{want}]'):
            state = want
        else:  # what it is instead (for the report)
            state = next((f'slices={n},cut={c}' for n in range(16, 7, -1) for c in ('up', 'north', 'south', 'east', 'west')
                          if _sense(s, f'if block {bx} {y} {z + 2} simplesandwiches:{block}_block[slices={n},cut={c}]')), 'other')
        s.check(f'{block} block: 4 real knife clicks on the {face} face cut 4 slices (slices=12,cut={face})',
                state == want and got == 4, f'block {state}, {got} slices into the inventory after {clicks} clicks, crosshair on {hit}')
        s.shot(f'{block}-cut')


def _layers(s, me, x, y, z):
    row = [('cheese_carpet', 2), ('cheese_carpet', 8), ('cheese_carpet', 14), ('butter_carpet', 4), ('butter_carpet', 10)]
    for i, (block, layers) in enumerate(row):
        s.chat(f'setblock {x - 2 + i} {y} {z + 3} simplesandwiches:{block}[layers={layers}]', 0.3)
    # a real click: a cheese slice on the butter carpet (4 layers) makes a mixed stack
    s.io.key('5', 0.3)
    s.chat('item replace entity @s hotbar.4 with simplesandwiches:cheese_slice 4', 0.3)
    _aim(s, me, (x + 1.5, y + 0.1, z + 3.5), 0.9)
    _click(s, None, after=0.6)
    mixed = _sense(s, f'if block {x + 1} {y} {z + 3} simplesandwiches:slice_stack')
    s.check('cheese slice on a butter carpet makes a mixed stack', mixed, 'slice_stack')
    _aim(s, (x + 0.5, y, z + 7.5), (x + 0.5, y + 0.1, z + 3.5), 0.9)
    s.shot('slice-layers')


def _cauldrons(s, x, y, z):
    row = [('milk', 0), ('milk', 1), ('milk', 2), ('milk', 3), ('butter', 0), ('cheese', 0)]
    for i, (content, stage) in enumerate(row):
        s.chat(f'setblock {x - 3 + i} {y} {z + 10} simplesandwiches:milk_cauldron[content={content},stage={stage}]', 0.3)
    _aim(s, (x + 0.5, y, z + 13.5), (x, y + 0.5, z + 10.5), 0.9)
    s.shot('cauldron-stages')
    # a real milk bucket into an empty cauldron, an empty hand takes the cheese
    s.chat(f'setblock {x + 4} {y} {z + 10} minecraft:cauldron', 0.4)
    s.chat('item replace entity @s hotbar.5 with minecraft:milk_bucket', 0.3)
    _aim(s, (x + 3.5, y, z + 13.0), (x + 4.5, y + 0.6, z + 10.5), 0.9)
    _click(s, '6', after=0.6)
    filled = _sense(s, f'if block {x + 4} {y} {z + 10} simplesandwiches:milk_cauldron[content=milk]')
    s.check('real milk bucket click fills the cauldron with milk', filled, 'milk_cauldron content=milk')
    _empty_hand(s)
    _aim(s, (x + 2.5, y, z + 13.0), (x + 2.5, y + 0.6, z + 10.5), 0.9)
    _click(s, '9', after=0.6)  # empty hand on the cheese cauldron (6th = x + 2)
    taken = _sense(s, f'if block {x + 2} {y} {z + 10} minecraft:cauldron')
    hand = s.st().get('inventory', [''] * 9)[8]
    s.check('empty hand takes the cheese out of the cauldron (cauldron empty, cheese block in the hand)', taken and hand.startswith('simplesandwiches:cheese_block'),
            f'cauldron emptied {taken}, hand {hand!r}')


def _bundle(s, me):
    s.chat('item replace entity @s hotbar.0 with minecraft:bundle[bundle_contents=[{id:"minecraft:cooked_beef",count:3}]]', 0.4)
    s.io.key('1', 0.3)
    # crosshair on the sky: a block under the crosshair would take the click first (a-w4c still looked at the cauldron row)
    _aim(s, me, (me[0], me[1] + 30, me[2] + 20), 0.8)
    # clearly hungry (cooked beef needs food < 20): Hunger 128 for 3 s burns ~9 food points incl. the 5 saturation. The probe world is
    # Peaceful (SortProbe), which refills food at once - a-w4c's player was never hungry, so the meal could not start: Easy for this part
    s.chat('difficulty easy', 0.4)
    s.chat('effect give @s minecraft:hunger 3 127 true', 0.3)
    time.sleep(3.5)
    s.chat('effect clear @s', 0.3)
    hungry = _sense(s, 'if entity @s[nbt={foodLevel:20}]') is False
    s.io.key_down('Shift_L')
    s.io.button(3, True)
    try:
        time.sleep(1.0)
        s.shot('bundle-eating')
        time.sleep(1.4)
    finally:
        s.io.button(3, False)
        s.io.key_up('Shift_L')
    time.sleep(0.5)
    two = _sense(s, 'if data entity @s Inventory[{Slot:0b}].components."minecraft:bundle_contents"[{id:"minecraft:cooked_beef",count:2}]')
    s.chat('difficulty peaceful', 0.4)
    s.check('sneak + hold use eats one beef out of the bundle (3 -> 2)', two, f'bundle_contents cooked_beef x2 (player was hungry: {hungry})')


def kitchen(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    me = (x + 0.5, y, z + 0.5)
    s.io.move(*s.abs(s.st(), 640, 360))
    for slot, stack in enumerate(['minecraft:bread 3', 'simplesandwiches:knife', 'minecraft:cooked_beef', 'minecraft:apple', 'simplesandwiches:cheese_slice']):
        s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.25)
    s.key('F1', 0.5)
    try:
        _board(s, me, x, y, z)
        _blocks(s, me, x, y, z)
        _layers(s, me, x, y, z)
        _cauldrons(s, x, y, z)
        _bundle(s, me)
    finally:
        s.io.button(3, False)
        s.io.key_up('Shift_L')
        s.key('F1', 0.5)
        s.chat('effect clear @s', 0.3)
        s.chat('difficulty peaceful', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplesandwiches': kitchen}
