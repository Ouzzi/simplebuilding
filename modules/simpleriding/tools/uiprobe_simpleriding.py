"""simpleriding-3: the hoof panel (four horseshoe slots left of the mount inventory) with the REAL mouse, plus pictures of the horseshoes on the hooves
(scenarios for modules/simpleinterfaces/tools/ui_probe.py).

SCENARIOS
  simpleriding         tamed Horse: sneak + right click opens its inventory; hoof slots by real click, shift-click (to the next free hoof
                       slot, and back out), refusal of a non-horseshoe, swap of a worn shoe, empty-slot icon; screenshots of panel and of the
                       hooves from the side/front.
  simpleriding/mounts  Donkey, Mule, Skeleton Horse, Zombie Horse (the other entities of #simpleriding:can_wear_horseshoes): open, shift-click a set of
                       four tiers, screenshot of the panel and of the shod hooves.

How the screen opens: a TAMED horse-like mount ('summon ... {Tame:1b,NoAI:1b}') and SNEAK (Shift held) + right click with an EMPTY hand on it
(Vanilla AbstractHorse.mobInteract; the player does not ride, so the inventory key E is not the way).  The status does not report the entity under the
crosshair, so a miss shows up as "no screen"; the crosshair is aimed at the mount's body centre.
Slots: HorseshoeMenuMixin appends the four hoof slots AFTER the player inventory, so they are the LAST four menu slots (own = false, cslot 0..3, order
front left, front right, hind left, hind right); saddle/armour/chest slots precede the inventory.  The panel itself (HorseshoePanel) is only drawn;
clicks on it are "inside" thanks to HorseshoeClickMixin, the slots are normal slots, so the generic slot click is the real mouse path.

Only a human can judge: the panel's look (box, frames, saddle-leather tint, slot icon of the empty hoof), the shoe models on the four hooves in the
pictures (size, tier colours, which hoof is which), the speed/jump/terrain effect of a set (needs riding and a long run), the latency ride test
(needs a network delay) - none of these is a mouse screen.
"""
import re
import time

DZ = 30  # own area of this module (south of the crucible area)
SET = [('simpleriding:copper_horseshoe', 'hotbar.0'), ('simpleriding:iron_horseshoe', 'hotbar.1'),
       ('simpleriding:golden_horseshoe', 'hotbar.2'), ('simpleriding:diamond_horseshoe', 'hotbar.3')]


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _xyz(st):
    return [int(v) for v in st['pos'].replace(',', ' ').split()]


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


def wait_stack(s, index, want, seconds=5):
    end = time.time() + seconds
    st = s.st()
    while True:
        got = stack_at(st, index) or ''
        if got == want or time.time() > end:
            return st, got
        time.sleep(0.4)
        st = s.st()


def own_index(st, text):
    for sl in st.get('slots', []):
        if sl['own'] and sl['stack'] == text:
            return sl['index']
    return None


def click_own(s, text, **kw):
    st = s.st()
    idx = own_index(st, text)
    if idx is None:
        s.check(f'own slot with {text} present', False, [sl['stack'] for sl in st.get('slots', []) if sl['own'] and sl['stack']][:12])
        return None
    return s.click_slot(index=idx, **kw)


def look(s, at, target, wait=0.9):
    """The player stands at 'at' (x, y, z) and the EYES look at 'target' (tp ... facing turns from the feet: eye height 1.62 taken off)."""
    s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', wait)
    time.sleep(0.5)
    return s.st()


def hoof_indices(st):
    """The four hoof slots: the last four menu slots, none of them the player's."""
    slots = st.get('slots', [])
    n = len(slots)
    last = slots[n - 4:] if n >= 4 else []
    if len(last) == 4 and all(not sl['own'] and sl['cslot'] == i for i, sl in enumerate(last)):
        return [sl['index'] for sl in last]
    return []


def sneak_use(s, point):
    """Empty hand, crosshair on the point, Shift held for one real right click (opens a tamed mount's inventory)."""
    x, y, z = _xyz(s.st())
    s.key('9', 0.4)
    look(s, (x + 0.5, y, z + 0.5), point)
    s.io.key_down('Shift_L')
    try:
        s.io.right_click()
    finally:
        s.io.key_up('Shift_L')


def give_set(s):
    s.chat('clear @s', 0.3)
    for item, where in SET:
        s.chat(f'item replace entity @s {where} with {item}', 0.3)


# ---------------------------------------------------------------- scenario 1: horse, every panel operation
def horse(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    x, y, z = _xyz(s.st())
    give_set(s)
    s.chat('item replace entity @s inventory.0 with simpleriding:netherite_horseshoe', 0.3)
    s.chat('item replace entity @s hotbar.4 with minecraft:cobblestone 4', 0.3)
    hx, hz = x + 3.5, z + 0.5
    s.chat(f'summon minecraft:horse {hx} {y} {hz} {{Tame:1b,NoAI:1b,Silent:1b,Rotation:[90f,0f]}}', 1.0)
    sneak_use(s, (hx, y + 0.9, hz))
    st = s.wait_screen('InventoryScreen', 10)
    opened = 'InventoryScreen' in st.get('screen', '') and 'Creative' not in st.get('screen', '')
    s.check('sneak + right click on the tamed horse opens its inventory', opened, st.get('screen'))
    if not opened:
        return
    s.check('screen is the horse (mount) inventory', 'Horse' in st.get('screen', '') or 'Mount' in st.get('screen', ''), st.get('screen'))
    hoof = hoof_indices(st)
    s.check('four hoof slots come last in the menu (not the player\'s, cslot 0..3)', len(hoof) == 4, len(st.get('slots', [])))
    if len(hoof) != 4:
        return
    s.check('hoof slots start empty', all(not stack_at(st, i) for i in hoof), [stack_at(st, i) for i in hoof])
    s.shot('panel-empty')

    # (1) real click: copper horseshoe from the hotbar onto the front-left hoof
    click_own(s, 'simpleriding:copper_horseshoe x1')
    s.click_slot(index=hoof[0])
    st, got = wait_stack(s, hoof[0], 'simpleriding:copper_horseshoe x1')
    s.check('real click: the copper horseshoe sits on the front-left hoof', got == 'simpleriding:copper_horseshoe x1', got)
    # (2) shift-click: iron, golden, diamond go to the next free hoof slots in order
    for item, slot in (('simpleriding:iron_horseshoe x1', 1), ('simpleriding:golden_horseshoe x1', 2), ('simpleriding:diamond_horseshoe x1', 3)):
        click_own(s, item, shift=True)
        st, got = wait_stack(s, hoof[slot], item)
        s.check(f'shift-click moves {item.split(":")[1].split(" ")[0]} to hoof slot {slot}', got == item, got)
    s.shot('panel-full')
    # (3) a non-horseshoe is refused (the cursor keeps it, the shoe stays)
    click_own(s, 'minecraft:cobblestone x4')
    s.click_slot(index=hoof[0])
    st = s.st()
    s.check('cobblestone on an occupied hoof slot: the shoe stays', stack_at(st, hoof[0]) == 'simpleriding:copper_horseshoe x1', stack_at(st, hoof[0]))
    s.check('cobblestone stays on the cursor (refused)', st.get('carried') == 'minecraft:cobblestone x4', st.get('carried'))
    back = next((sl['index'] for sl in st['slots'] if sl['own'] and not sl['stack']), None)
    if back is not None:
        s.click_slot(index=back)
    # (4) swap: a netherite shoe onto the worn copper one
    click_own(s, 'simpleriding:netherite_horseshoe x1')
    s.click_slot(index=hoof[0])
    st, got = wait_stack(s, hoof[0], 'simpleriding:netherite_horseshoe x1')
    s.check('swap: the netherite shoe replaces the copper one on hoof 0', got == 'simpleriding:netherite_horseshoe x1', got)
    s.check('swap: the copper shoe is on the cursor', st.get('carried') == 'simpleriding:copper_horseshoe x1', st.get('carried'))
    back = next((sl['index'] for sl in st['slots'] if sl['own'] and not sl['stack']), None)
    if back is not None:
        s.click_slot(index=back)
    # (5) shift-click on a hoof slot returns the shoe to the inventory
    s.click_slot(index=hoof[3], shift=True)
    st, got = wait_stack(s, hoof[3], '')
    s.check('shift-click on hoof 3 empties it', got == '', got)
    st = s.st()
    s.check('the diamond shoe is back in the inventory', count_of(st, 'simpleriding:diamond_horseshoe') == 1, count_of(st, 'simpleriding:diamond_horseshoe'))
    s.shot('panel-after')
    # put it back so all four hooves are shod for the pictures
    click_own(s, 'simpleriding:diamond_horseshoe x1', shift=True)
    st, got = wait_stack(s, hoof[3], 'simpleriding:diamond_horseshoe x1')
    s.check('shift-click puts the diamond shoe on hoof 3 again', got == 'simpleriding:diamond_horseshoe x1', got)
    s.close()

    # pictures of the hooves: from the front-left and from the side (the horse stands still, NoAI)
    look(s, (hx - 2.4, y, hz + 1.6), (hx, y + 0.35, hz))
    s.shot('hooves-front-left')
    look(s, (hx, y, hz + 2.8), (hx, y + 0.45, hz))
    s.shot('hooves-side')
    look(s, (hx + 2.4, y, hz - 1.6), (hx, y + 0.35, hz))
    s.shot('hooves-rear')
    time.sleep(0.5)


# ---------------------------------------------------------------- scenario 2: the other mounts
def mounts(s):
    s.chat('tp @s ~ ~ ~' + str(DZ), 1.0)
    x, y, z = _xyz(s.st())
    for name, (dx, dz) in (('donkey', (3.5, 0.5)), ('mule', (-2.5, 0.5)), ('skeleton_horse', (0.5, 3.5)), ('zombie_horse', (0.5, -2.5))):
        mx, mz = x + dx, z + dz
        give_set(s)
        s.chat(f'summon minecraft:{name} {mx} {y} {mz} {{Tame:1b,NoAI:1b,Silent:1b}}', 1.0)
        sneak_use(s, (mx, y + 0.8, mz))
        st = s.wait_screen('InventoryScreen', 10)
        opened = 'InventoryScreen' in st.get('screen', '') and 'Creative' not in st.get('screen', '')
        s.check(f'{name}: sneak + right click opens the inventory', opened, st.get('screen'))
        if opened:
            hoof = hoof_indices(st)
            s.check(f'{name}: four hoof slots', len(hoof) == 4, len(st.get('slots', [])))
            if len(hoof) == 4:
                for i, (item, _) in enumerate(SET):
                    click_own(s, item + ' x1', shift=True)
                st = s.st()
                st, got = wait_stack(s, hoof[3], 'simpleriding:diamond_horseshoe x1')
                s.check(f'{name}: four shift-clicks shod all hooves', [stack_at(st, i) for i in hoof] == [it + ' x1' for it, _ in SET],
                        [stack_at(st, i) for i in hoof])
                s.shot(f'{name}-panel')
        s.close()
        # picture of this mount's hooves from two blocks away
        look(s, (mx - 2.2, y, mz + 1.3), (mx, y + 0.35, mz))
        s.shot(f'{name}-hooves')
        # go back to the spot before the next mount (aim() of the next round turns from here)
        look(s, (x + 0.5, y, z + 0.5), (x + 0.5, y + 1.0, z + 5.0))
    time.sleep(0.5)


SCENARIOS = {'simpleriding': horse, 'simpleriding/mounts': mounts}
