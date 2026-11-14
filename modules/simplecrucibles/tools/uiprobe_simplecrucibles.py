"""simplecrucibles-3: the Crucible screen with an attached barrel, real mouse (scenario for modules/simpleinterfaces/tools/ui_probe.py).

Flow (iron crucible = 6 slots, one grid of 3 x 2):
  1. setblock a magma block (medium heat) with an iron crucible on it and a copper barrel (Simple Storage) beside it.
  2. empty-hand real right click on the crucible -> CrucibleScreen in the compact layout (no barrel): real clicks put potatoes into slot 0
     (the reserved result shows in the hidden ghost slot), shift-click moves beef into the next free slot.
  3. close, sledgehammer (simplehammers:iron_sledgehammer, the integration client has Simple Hammers so the axe way is off) in hand,
     SIX real right clicks on the barrel attach it (docking, WH26-6; one strike per second, cooldown 20 ticks).
  4. empty-hand right click on the BARREL opens the crucible menu again (proof of the dock) in the wide layout with the barrel box:
     real clicks put cobblestone into a barrel field, the cooked potatoes appear in the barrel/crucible fields, shift-click takes it out.
  5. empty Reinforced / Netherite / Enderite crucibles: screen opens, slot counts of the three sizes, screenshots.

How the status maps to the menu (CrucibleMenu keeps BOTH layouts, only the current one is active, the status lists all):
  n = crucible slots of the tier; indices 0..n-1 crucible (compact), n..2n-1 hidden ghosts of the results, 2n..3n-1 barrel fields,
  3n..4n-1 barrel ghosts, 4n..5n-1 crucible (with barrel), 5n..5n+35 own inventory (compact), 5n+36..5n+71 own inventory (with barrel);
  an inventory block is the 27 main slots (cslot 9..35) then the hotbar (cslot 0..8).  Total 5n + 72 slots (iron 102, enderite 222).
  A click must go to an ACTIVE slot (an inactive one is "outside" and would drop the cursor), so the indices come from the layout in force.
  The layout in force is decided by whether the barrel really docked: an empty-hand click on the barrel opens the crucible's CrucibleScreen
  when it did, a plain container screen when it did not.

Only a human can judge: the flames and hearth, the progress fill/colours of the slots, the barrel box beside the crucible box (frames,
colours), the sound and cracks while striking, the title scaling of long names, text contrast.  Not covered: JEI/Jade, soul lava,
field kitchen (world features, not mouse screens).
"""
import re
import time

DZ = 20  # own area of this module (south of the shops area)


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


def aim(s, px, py, pz, wait=0.8):
    """Puts the crosshair on the world point (the EYES look at it). 'tp @s ... facing' turns from the feet, so the eye height is taken off."""
    x, y, z = _xyz(s.st())
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {px:.3f} {py - 1.62:.3f} {pz:.3f}', wait)
    time.sleep(0.4)
    return s.st()


def count_of(st, item):
    n = 0
    for text in st.get('inventory', []):
        m = re.fullmatch(re.escape(item) + r' x(\d+)', text or '')
        if m:
            n += int(m.group(1))
    return n


def stack_at(st, index):
    for sl in st.get('slots', []):
        if sl['index'] == index:
            return sl['stack']
    return None


def wait_stack(s, index, want, seconds=5, prefix=False):
    """Waits until the slot shows 'want' ('' = empty; prefix: startswith). Returns (status, stack)."""
    end = time.time() + seconds
    st = s.st()
    while True:
        got = stack_at(st, index) or ''
        if got is not None and ((got.startswith(want) and bool(want)) if prefix else got == want):
            return st, got
        if time.time() > end:
            return st, got
        time.sleep(0.4)
        st = s.st()


def wait_until(s, pred, seconds=5):
    end = time.time() + seconds
    st = s.st()
    while not pred(st) and time.time() < end:
        time.sleep(0.4)
        st = s.st()
    return st


def open_with_empty_hand(s, point, fragment, seconds=10):
    s.key('9', 0.4)
    aim(s, *point)
    s.io.right_click()
    return s.wait_screen(fragment, seconds)


# ---------------------------------------------------------------- layout maths (see the docstring)
def inv_index(n, attached, cslot):
    base = 5 * n + (36 if attached else 0)
    return base + (cslot - 9 if cslot >= 9 else 27 + cslot)


def crucible_index(n, attached, i):
    return (4 * n if attached else 0) + i


def barrel_index(n, i):
    return 2 * n + i


def ghost_index(n, i):
    return n + i


def click_inv(s, n, attached, cslot, want, **kw):
    """Real click on the own slot (cslot) of the layout in force, after checking that it shows 'want'."""
    st = s.st()
    idx = inv_index(n, attached, cslot)
    got = stack_at(st, idx)
    sl = next((x for x in st.get('slots', []) if x['index'] == idx), None)
    if sl is None or not sl['own'] or sl['cslot'] != cslot or got != want:
        s.check(f'own slot cslot {cslot} (menu {idx}) shows {want}', False, (sl, got))
        return None
    return s.click_slot(index=idx, **kw)


# ---------------------------------------------------------------- the scenario
def crucible_barrel(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    x, y, z = _xyz(s.st())
    s.chat('clear @s', 0.3)
    s.chat('item replace entity @s hotbar.0 with simplehammers:iron_sledgehammer', 0.3)
    s.chat('item replace entity @s hotbar.3 with minecraft:potato 4', 0.3)
    s.chat('item replace entity @s hotbar.4 with minecraft:beef 2', 0.3)
    s.chat('item replace entity @s hotbar.5 with minecraft:cobblestone 8', 0.3)
    cx = x + 3
    s.chat(f'setblock {cx} {y - 1} {z} minecraft:magma_block', 0.5)
    s.chat(f'setblock {cx} {y} {z} simplecrucibles:iron_crucible', 0.5)
    s.chat(f'setblock {cx} {y} {z + 1} simplestorage:copper_barrel', 0.5)
    crucible = (cx + 0.5, y + 0.5, z + 0.5)
    barrel = (cx + 0.2, y + 0.5, z + 1.5)  # a point inside the barrel, the ray from the west crosses its west face first
    n = 6

    # ---- compact layout
    st = open_with_empty_hand(s, crucible, 'CrucibleScreen')
    s.check('empty-hand right click on the crucible opens the Crucible screen', 'CrucibleScreen' in st.get('screen', ''), f"{st.get('screen')} hit={st.get('hitBlock')}")
    if 'CrucibleScreen' not in st.get('screen', ''):
        return
    s.check(f'iron crucible menu has 5n+72 = {5 * n + 72} slots', len(st.get('slots', [])) == 5 * n + 72, len(st.get('slots', [])))
    s.shot('compact-empty')
    click_inv(s, n, False, 3, 'minecraft:potato x4')
    s.click_slot(index=crucible_index(n, False, 0))
    st, got = wait_stack(s, crucible_index(n, False, 0), 'minecraft:potato x4')
    s.check('real clicks put the potatoes into crucible slot 0', got == 'minecraft:potato x4', got)
    st, got = wait_stack(s, ghost_index(n, 0), 'minecraft:baked_potato', 8, prefix=True)
    s.check('the reserved result (baked potato) shows in the hidden ghost slot', got.startswith('minecraft:baked_potato'), got)
    click_inv(s, n, False, 4, 'minecraft:beef x2', shift=True)
    st = wait_until(s, lambda t: any((stack_at(t, i) or '').startswith('minecraft:beef') for i in range(1, n)), 5)
    s.check('shift-click moves the beef into a free crucible slot', any((stack_at(st, i) or '').startswith('minecraft:beef') for i in range(1, n)),
            [stack_at(st, i) for i in range(n)])
    s.shot('compact-cooking')
    s.close()

    # ---- dock the barrel: six real strikes with the sledgehammer
    s.key('1', 0.4)
    st = aim(s, *barrel)
    s.check('crosshair on the barrel', st.get('hitBlock') == 'simplestorage:copper_barrel', st.get('hitBlock'))
    for i in range(6):
        s.io.right_click()
        time.sleep(0.4)  # the strike cooldown is 20 ticks
        if i == 2:
            s.shot('strikes-3-of-6')
    st = open_with_empty_hand(s, barrel, 'Screen', 10)
    attached = 'CrucibleScreen' in st.get('screen', '')
    if not attached:
        # one more round (a strike may have fallen into the cooldown)
        s.close()
        s.key('1', 0.4)
        aim(s, *barrel)
        for _ in range(3):
            s.io.right_click()
            time.sleep(1.0)
        st = open_with_empty_hand(s, barrel, 'Screen', 10)
        attached = 'CrucibleScreen' in st.get('screen', '')
    s.check('six strikes docked the barrel: a click on it now opens the crucible menu', attached, st.get('screen'))
    if not attached:
        s.shot('not-docked')
        return
    st = s.st()
    s.shot('docked-open')
    click_inv(s, n, True, 5, 'minecraft:cobblestone x8')
    s.click_slot(index=barrel_index(n, 0))
    st, got = wait_stack(s, barrel_index(n, 0), 'minecraft:cobblestone x8')
    s.check('a real click puts the cobblestone into barrel field 0', got == 'minecraft:cobblestone x8', got)
    # the potatoes keep cooking: the result goes to the barrel first (up to ~20 s at medium heat), else it stays in the crucible
    fields = [barrel_index(n, i) for i in range(n)] + [crucible_index(n, True, i) for i in range(n)]
    st = wait_until(s, lambda t: any((stack_at(t, i) or '').startswith('minecraft:baked_potato') for i in fields), 22)
    s.check('a baked potato came out (barrel field or crucible slot)', any((stack_at(st, i) or '').startswith('minecraft:baked_potato') for i in fields),
            [stack_at(st, i) for i in fields])
    s.shot('docked-cooked')
    click_inv_before = count_of(st, 'minecraft:cobblestone')
    s.click_slot(index=barrel_index(n, 0), shift=True)
    st = wait_until(s, lambda t: count_of(t, 'minecraft:cobblestone') == click_inv_before + 8, 5)
    s.check('shift-click on the barrel field takes the cobblestone back into the inventory', count_of(st, 'minecraft:cobblestone') == click_inv_before + 8,
            count_of(st, 'minecraft:cobblestone'))
    s.close()

    # ---- the other three sizes, empty, at three more places around the player (all within reach)
    for tier, slots, (dx, dz) in (('reinforced', 12, (-3, 0)), ('netherite', 18, (0, -3)), ('enderite', 30, (-3, -3))):
        bx, bz = x + dx, z + dz
        s.chat(f'setblock {bx} {y} {bz} simplecrucibles:{tier}_crucible', 0.5)
        st = open_with_empty_hand(s, (bx + 0.5, y + 0.5, bz + 0.5), 'CrucibleScreen')
        ok = 'CrucibleScreen' in st.get('screen', '')
        s.check(f'{tier} crucible opens its screen', ok, f"{st.get('screen')} hit={st.get('hitBlock')}")
        if ok:
            s.check(f'{tier} crucible menu has 5n+72 = {5 * slots + 72} slots', len(st.get('slots', [])) == 5 * slots + 72, len(st.get('slots', [])))
            s.shot(f'{tier}-empty')
        s.close()
    time.sleep(0.5)


SCENARIOS = {'simplecrucibles': crucible_barrel}
