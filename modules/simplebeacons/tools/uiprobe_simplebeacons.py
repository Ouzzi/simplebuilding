"""simplebeacons-5: the Strong / Mighty Beacon screen (StrongBeaconScreen) with the REAL mouse.

Scenario for modules/simpleinterfaces/tools/ui_probe.py. Opening: real right click on a simplebeacons:strong_beacon block
(StrongBeaconBlock.useWithoutItem -> openMenu). The screen is Vanilla's BeaconScreen with REAL widgets, so everything is clicked by
label/class from the status: StrongButton (the effect buttons of every pyramid row, label = effect name, "Speed II" for placeholders;
`active` = the row's pyramid level is reached), Vanilla's BeaconPowerButton (secondary power, Regeneration), BeaconConfirmButton /
BeaconCancelButton, plus the payment slot (menu slot 0, own=False). The pyramid is built with /fill WHILE the screen is open: the
level arrives through the menu data sync, so the locked/unlocked state of the rows is read live (scan happens every 80 ticks).
Stages: (1) Strong beacon, no pyramid: 10 effect buttons, all locked; (2) 2 levels: rows 1-2 open, rows 3-4 locked; (3) 5 levels (bottom
layer diamond): every button open + secondary Regeneration, real choice Speed + iron ingot payment + Done -> beacon NBT has the power
and the ingot is used; (4) second visit: Haste + Regeneration; (5) Mighty beacon with 6 levels: 15 buttons (5 rows).
The effect on the beacon is read with `execute if data block ... run item replace ... stick` (marker item in hotbar slot 8), because
chat output is not part of the status. NOT decidable by the script (needs a human): the text "Pyramid n/m", "Material x1.21", range and
duration lines, the marker icons, the locked tooltip look - they are only in the screenshots.
"""
import time

BEACON = 'simplebeacons:strong_beacon'
MIGHTY = 'simplebeacons:mighty_beacon'
ROWS = (('Speed', 'Haste', 'Saturation'), ('Resistance', 'Jump Boost', 'Slow Falling'), ('Strength', 'Invisibility', "Dolphin's Grace"), ('Luck',))


def _look(s, pos):
    """Aims the crosshair at the centre of block pos from where the player stands (`tp ... facing` looks from the FEET, so the
    target is lowered by the eye height 1.62; the driver's Session.face does not do that and misses blocks at the player's level)."""
    st = s.st()
    x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {pos[0] + 0.5} {pos[1] + 0.5 - 1.62:.2f} {pos[2] + 0.5}', 0.8)
    time.sleep(0.6)
    return s.st()


def _aim(s, block, dx, dy, dz):
    """setblock dx/dy/dz from the player's feet, then look at it."""
    st = s.st()
    x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
    pos = (x + dx, y + dy, z + dz)
    s.chat(f'setblock {pos[0]} {pos[1]} {pos[2]} {block}', 0.6)
    return pos, _look(s, pos)


def _pay(s):
    """One iron ingot from hotbar slot 0 into the payment slot (stack limit 1), the rest of the cursor back. Returns the payment stack."""
    st = s.click_slot(cslot=0, own=True)
    st = s.click_slot(cslot=0, own=False)
    pay = s.slot_xy(st, 0, False) if st else None
    if st and st.get('carried'):
        st = s.click_slot(cslot=0, own=True)
    return pay['stack'] if pay else None


def _nbt(s, pos, snbt):
    """Whether the block entity at pos matches the compound `snbt` (marker stick in hotbar slot 8)."""
    s.chat('item replace entity @s hotbar.8 with minecraft:air', 0.4)
    s.chat(f'execute if data block {pos[0]} {pos[1]} {pos[2]} {snbt} run item replace entity @s hotbar.8 with minecraft:stick', 0.9)
    item = s.st()['inventory'][8]
    return item.startswith('minecraft:stick'), item


def _buttons(s, st=None):
    return [w for w in s.widgets(st) if w['class'] == 'StrongButton']


def _wait(s, cond, seconds):
    end = time.time() + seconds
    while time.time() < end:
        st = s.st()
        if cond(st):
            return st
        time.sleep(0.5)
    return s.st()


def _btn(s, st, label):
    return next((w for w in _buttons(s, st) if w['label'] == label), None)


def _pyramid(s, cx, by, cz, levels, bottom=None):
    """Level L (1-based) is a (2L+1)^2 plate at y = by - L around (cx, cz); `bottom` = block of the lowest one."""
    for level in levels:
        block = bottom if bottom and level == max(levels) else 'minecraft:iron_block'
        s.chat(f'fill {cx - level} {by - level} {cz - level} {cx + level} {by - level} {cz + level} {block}', 0.8)


def beacons(s):
    s.chat('gamemode creative @s', 0.5)
    s.chat('tp @s ~ ~ ~80', 1.0)  # own area
    time.sleep(1.5)
    s.clear_and_give('minecraft:iron_ingot 8')
    # ---- (1) Strong beacon without pyramid
    pos, st = _aim(s, BEACON, 3, 1, 0)
    cx, by, cz = pos
    s.check('crosshair on the strong beacon', st.get('hitBlock') == BEACON, st.get('hitBlock'))
    s.io.right_click()
    st = s.wait_screen('StrongBeaconScreen', 12)
    s.check('real right click opens the Strong Beacon screen', 'StrongBeaconScreen' in st.get('screen', ''), st.get('screen'))
    if 'StrongBeaconScreen' not in st.get('screen', ''):
        return
    time.sleep(1.0)  # the kind arrives with the first data sync, the screen rebuilds itself
    st = s.st()
    labels = [w['label'] for w in _buttons(s, st)]
    s.check('Strong beacon shows 10 effect buttons (3+3+3+1)', len(labels) == 10, labels)
    s.check('the buttons carry the 10 expected effect names', all(any(b == name for b in labels) for row in ROWS for name in row), labels)
    s.check('without pyramid every effect button is locked', all(not w['active'] for w in _buttons(s, st)), [(w['label'], w['active']) for w in _buttons(s, st)])
    done = s.find_widget(cls='BeaconConfirmButton', st=st)
    s.check('Done button present and inactive (nothing chosen, no payment)', done is not None and not done['active'], done)
    s.shot('no-pyramid')
    locked = _btn(s, st, 'Resistance')
    if locked:
        s.io.move(*s.abs(st, locked['x'], locked['y']))
        time.sleep(1.2)
        s.shot('locked-tooltip')
    # ---- (2) two levels while the screen is open
    _pyramid(s, cx, by, cz, [1, 2])
    st = _wait(s, lambda t: (_btn(s, t, 'Resistance') or {}).get('active'), 15)
    row12 = [(n, (_btn(s, st, n) or {}).get('active')) for n in ROWS[0] + ROWS[1]]
    row34 = [(n, (_btn(s, st, n) or {}).get('active')) for n in ROWS[2] + ROWS[3]]
    s.check('2 levels: rows 1-2 (6 buttons) are unlocked', all(a for _, a in row12), row12)
    s.check('2 levels: rows 3-4 stay locked', not any(a for _, a in row34), row34)
    s.shot('two-levels')
    # ---- (3) five levels, bottom layer diamond (material factor), then a real choice
    _pyramid(s, cx, by, cz, [3, 4, 5], bottom='minecraft:diamond_block')
    st = _wait(s, lambda t: (_btn(s, t, 'Luck') or {}).get('active'), 15)
    s.check('5 levels: all 10 effect buttons unlocked', all(w['active'] for w in _buttons(s, st)) and len(_buttons(s, st)) == 10,
            [(w['label'], w['active']) for w in _buttons(s, st)])
    regen = s.find_widget('Regeneration', 'BeaconPowerButton', st)
    s.check('secondary power Regeneration is unlocked at level 5', regen is not None and regen['active'], regen)
    time.sleep(1.0)
    s.shot('five-levels')  # "Pyramid 5/6", "Material x1.21" and the range/duration lines: look for a human
    if s.click_widget('Speed', 'StrongButton', 'Speed button'):
        st = s.st()
        done = s.find_widget(cls='BeaconConfirmButton', st=st)
        s.check('Done is still inactive without payment', done is not None and not done['active'], done)
        # payment: ingot from hotbar slot 0 into the payment slot (menu slot 0, own=False; it takes ONE, the rest goes back)
        pay = _pay(s)
        s.check('real clicks put one iron ingot into the payment slot', pay == 'minecraft:iron_ingot x1', pay)
        st = _wait(s, lambda t: (s.find_widget(cls='BeaconConfirmButton', st=t) or {}).get('active'), 6)
        done = s.find_widget(cls='BeaconConfirmButton', st=st)
        s.check('Done is active with a power and a payment', done is not None and done['active'], done)
        s.shot('speed-chosen')
        if done:
            s.click_xy(done['x'], done['y'], st)
            st = s.wait_screen('none', 8)
            s.check('real click on Done closes the screen', st.get('screen') == 'none', st.get('screen'))
            ok, item = _nbt(s, pos, '{primary_effect:"minecraft:speed"}')
            s.check('beacon block entity now has primary_effect speed', ok, item)
            ok, item = _nbt(s, pos, '{Levels:5}')
            s.check('beacon block entity counted 5 levels', ok, item)
    # ---- (4) second visit: Haste + Regeneration
    _look(s, pos)
    s.io.right_click()
    st = s.wait_screen('StrongBeaconScreen', 12)
    if 'StrongBeaconScreen' in st.get('screen', ''):
        time.sleep(1.0)
        if s.click_widget('Haste', 'StrongButton', 'Haste button') and s.click_widget('Regeneration', 'BeaconPowerButton', 'Regeneration button'):
            pay = _pay(s)
            st = _wait(s, lambda t: (s.find_widget(cls='BeaconConfirmButton', st=t) or {}).get('active'), 6)
            s.shot('haste-regeneration')
            done = s.find_widget(cls='BeaconConfirmButton', st=st)
            if done and done['active']:
                s.click_xy(done['x'], done['y'], st)
                s.wait_screen('none', 8)
                ok, item = _nbt(s, pos, '{primary_effect:"minecraft:haste",secondary_effect:"minecraft:regeneration"}')
                s.check('second choice stored: haste + regeneration', ok, item)
            else:
                s.check('Done active for Haste + Regeneration', False, (done, pay))
    else:
        s.check('second right click opens the screen again', False, st.get('screen'))
    s.close()
    # ---- (5) Mighty beacon: 6 levels, five rows. The beacon must stand 2 above the ground (6 plates fit above bedrock level);
    # the player stands on a stone block, so the crosshair ray passes OVER the first plate.
    s.chat('tp @s ~ ~ ~20', 1.0)
    time.sleep(1.5)
    st = s.st()
    x, y, z = [int(v) for v in st['pos'].replace(',', ' ').split()]
    _pyramid(s, x + 4, y + 2, z, [1, 2, 3, 4, 5, 6])
    s.chat(f'setblock {x} {y} {z} minecraft:stone', 0.6)
    s.chat(f'tp @s {x + 0.5} {y + 1} {z + 0.5}', 1.0)
    time.sleep(1.0)
    pos, st = _aim(s, MIGHTY, 4, 1, 0)
    s.io.right_click()
    st = s.wait_screen('StrongBeaconScreen', 12)
    s.check('Mighty beacon opens the same screen class', 'StrongBeaconScreen' in st.get('screen', ''), st.get('screen'))
    if 'StrongBeaconScreen' in st.get('screen', ''):
        st = _wait(s, lambda t: len(_buttons(s, t)) == 15, 6)
        s.check('Mighty beacon shows 15 effect buttons (5 rows of 3)', len(_buttons(s, st)) == 15, [w['label'] for w in _buttons(s, st)])
        st = _wait(s, lambda t: all(w['active'] for w in _buttons(s, t)) and len(_buttons(s, t)) == 15, 15)
        s.check('6 levels unlock all 15 buttons', all(w['active'] for w in _buttons(s, st)), [(w['label'], w['active']) for w in _buttons(s, st) if not w['active']])
        s.check('placeholder II buttons exist (Speed II, Jump Boost II)', _btn(s, st, 'Speed II') is not None and _btn(s, st, 'Jump Boost II') is not None,
                [w['label'] for w in _buttons(s, st)])
        s.shot('mighty-six-levels')
    s.close()


SCENARIOS = {'simplebeacons': beacons}
