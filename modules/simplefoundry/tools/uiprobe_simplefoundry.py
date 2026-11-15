"""simplefoundry-4: the Foundry Desk window (FoundryScreen) through all four tiers with the REAL mouse.

Scenario for modules/simpleinterfaces/tools/ui_probe.py. Opening: real right click (empty hand) on simplefoundry:foundry_desk
(DeskBlock.useWithoutItem -> openMenu). The desk is the west wall block of a small tank built like the module's client smoke
(bricks, 3 melting chambers, glass front, fire grate), so the window has a valid structure, a section with 3 levels and melts
(data merge block {Melt_*}). The four tiers (3 / 6 / 9 / 12 places) are bought exactly like a player: right click on the desk with
the upgrade item (iron block, diamond block, netherite ingot) - the tier is read from the block entity NBT (`Tier`, marker stick in
hotbar slot 8, because chat output is not in the status). Per tier the window is opened and checked with real clicks:
  * the last active place takes a piece (right click with raw iron on the cursor), the first locked place refuses it;
  * (tier 1 only) the Vanilla arrow Button "▼" cycles the selected level (NBT `Selected`), a click on the section's lowest band
    selects level 0 - that band is drawn and hit-tested by the screen itself (FoundryScreen.levelAt: section x 70..106, y 22..84 of
    the 176 x 208 window, window origin = ((guiW - 176) / 2, (guiH - 208) / 2), ASSUMPTION gui = 1280 x 720 / scale).
Only a human can judge: the look of the window (locked places veiled with a cross, tank bands, fuel gauge, status mark) - see the shots.
"""
import time

DESK = 'simplefoundry:foundry_desk'
UPGRADES = (None, 'minecraft:iron_block', 'minecraft:diamond_block', 'minecraft:netherite_ingot')
SLOTS = (3, 6, 9, 12)


def _look(s, pos, at=None):
    """Aims at the centre of block pos (optionally after standing at `at` = (x, z) block); `tp ... facing` looks from the FEET, so the
    target is lowered by the eye height 1.62 (the driver's Session.face does not do that and misses blocks at the player's level)."""
    st = s.st()
    x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
    if at:
        x, z = at
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {pos[0] + 0.5} {pos[1] + 0.5 - 1.62:.2f} {pos[2] + 0.5}', 0.8)
    time.sleep(0.8)
    return s.st()


def _nbt(s, pos, snbt, present=True):
    """Whether the block entity at pos matches (present) / does not match (not present) the compound `snbt`; the answer is a
    marker stick in hotbar slot 8, because chat output is not in the status."""
    s.chat('item replace entity @s hotbar.8 with minecraft:air', 0.4)
    s.chat(f'execute {"if" if present else "unless"} data block {pos[0]} {pos[1]} {pos[2]} {snbt} run item replace entity @s hotbar.8 with minecraft:stick', 0.9)
    item = s.st()['inventory'][8]
    return item.startswith('minecraft:stick'), item


def foundry(s):
    s.chat('gamemode creative @s', 0.5)
    s.chat('tp @s ~ ~ ~120', 1.0)  # own area
    time.sleep(1.5)
    st = s.st()
    x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
    # the tank like the module's client smoke: hull, 3 chambers, glass front, desk (west wall) and fire grate (east wall)
    for cmd in (f'fill {x - 1} {y} {z + 5} {x + 1} {y + 4} {z + 7} simplefoundry:foundry_brick',
                f'fill {x} {y + 1} {z + 6} {x} {y + 3} {z + 6} simplefoundry:melting_chamber',
                f'fill {x - 1} {y + 1} {z + 5} {x + 1} {y + 3} {z + 5} minecraft:glass',
                f'setblock {x - 1} {y + 1} {z + 6} {DESK}[facing=south]',
                f'setblock {x + 1} {y + 1} {z + 6} simplefoundry:fire_grate'):
        s.chat(cmd, 0.6)
    pos = (x - 1, y + 1, z + 6)
    s.chat(f'data merge block {pos[0]} {pos[1]} {pos[2]} {{Melt_gold:81,Melt_copper:162,Melt_iron:100}}', 0.8)
    time.sleep(1.0)
    # stand two blocks west of the desk (its west face is free), look at it
    stand = (x - 3, z + 6)
    st = _look(s, pos, at=stand)
    s.check('crosshair on the foundry desk', st.get('hitBlock') == DESK, st.get('hitBlock'))
    # raw iron in hotbar slot 4 (never the hand: with raw iron in the hand a right click would throw it into the desk instead of opening
    # the window); key 1 makes hotbar slot 0 the hand
    s.key('1', 0.4)
    s.chat('clear @s', 0.4)
    s.chat('item replace entity @s hotbar.4 with minecraft:raw_iron 40', 0.5)

    for tier in range(4):
        slots = SLOTS[tier]
        # ---- buy the tier like a player: upgrade item in the hand, real right click on the desk
        if tier > 0:
            s.chat(f'item replace entity @s weapon.mainhand with {UPGRADES[tier]} 1', 0.6)
            _look(s, pos, at=stand)
            s.io.right_click()
            time.sleep(0.8)
            ok, item = _nbt(s, pos, f'{{Tier:{tier}}}')
            s.check(f'tier {tier + 1}: right click with {UPGRADES[tier].split(":")[1]} lifts the desk (NBT Tier {tier})', ok, item)
        # ---- open the window with an empty hand
        s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.6)
        st = _look(s, pos, at=stand)
        s.io.right_click()
        st = s.wait_screen('FoundryScreen', 12)
        opened = 'FoundryScreen' in st.get('screen', '')
        s.check(f'tier {tier + 1}: real right click opens the desk window', opened, st.get('screen'))
        if not opened:
            s.close()
            continue
        time.sleep(0.6)
        st = s.st()
        places = [t for t in st['slots'] if not t['own']]
        s.check(f'tier {tier + 1}: window reports 13 desk slots (12 places + fuel)', len(places) == 13, len(places))
        s.shot(f'window-tier{tier + 1}-{slots}-places')
        # ---- real clicks: lift raw iron from the hotbar, right click = place ONE piece
        lifted = s.click_slot(cslot=4, own=True)
        s.check(f'tier {tier + 1}: raw iron lifted onto the cursor', lifted and lifted.get('carried', '').startswith('minecraft:raw_iron'), lifted and lifted.get('carried'))
        last = s.click_slot(index=slots - 1, right=True)
        time.sleep(1.2)  # the server answers the click with the slot content
        last = s.st()
        got = s.slot_xy(last, index=slots - 1)['stack'] if last and s.slot_xy(last, index=slots - 1) else None
        s.check(f'tier {tier + 1}: last active place ({slots}) takes a piece', got == 'minecraft:raw_iron x1', got)
        time.sleep(0.5)
        ok, item = _nbt(s, pos, f'{{Slot{slots - 1}:{{id:"minecraft:raw_iron"}}}}')
        s.check(f'tier {tier + 1}: the desk block entity holds it (Slot{slots - 1})', ok, item)
        if slots < 12:
            lock = s.click_slot(index=slots, right=True)
            time.sleep(1.2)
            lock = s.st()
            got = s.slot_xy(lock, index=slots)['stack'] if lock and s.slot_xy(lock, index=slots) else None
            s.check(f'tier {tier + 1}: first locked place ({slots + 1}) refuses it', not got, got)
            ok, item = _nbt(s, pos, f'{{Slot{slots}:{{id:"minecraft:raw_iron"}}}}', present=False)
            s.check(f'tier {tier + 1}: the desk block entity has nothing in Slot{slots}', ok, item)
        s.shot(f'window-tier{tier + 1}-filled')
        if tier == 0:
            # ---- the Vanilla arrow button and the section hit test (levels 3: selected level counts down, wraps)
            if s.click_widget('▼', name='arrow button'):
                time.sleep(0.8)
                ok, item = _nbt(s, pos, '{Selected:2}')
                s.check('arrow button: selected level wraps from 0 to the top (Selected 2)', ok, item)
                s.shot('arrow-clicked')
            st = s.st()
            sc = st['scale']
            gw, gh = 1280 // sc, 720 // sc
            left, top = (gw - 176) // 2, (gh - 208) // 2
            s.click_xy((left + 70 + 18) * sc, (top + 22 + 62 - 3) * sc, st)  # lowest band of the section
            time.sleep(0.8)
            ok, item = _nbt(s, pos, '{Selected:0}')
            s.check('click on the lowest band of the section selects level 0 (Selected 0)', ok, item)
            s.shot('section-clicked')
        # cursor back to the hotbar, close
        back = s.st()
        if back.get('carried'):
            s.click_slot(cslot=4, own=True)
        s.close()
        time.sleep(0.5)


SCENARIOS = {'simplefoundry': foundry}
