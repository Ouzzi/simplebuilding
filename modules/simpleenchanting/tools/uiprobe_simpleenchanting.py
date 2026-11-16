"""simpleenchanting-4 (offen-vor-release B5): the enchanter window (Dark and Astral), the Enchanted Golden Apple and a level V
enchantment with the REAL mouse, for the owner card. Scenario for modules/simpleinterfaces/tools/ui_probe.py (survival + 200 levels,
so prices, lapis and blaze powder are really taken):

  simpleenchanting/release
    1. Dark Enchanter on a 5x5 Blazing Obsidian floor inside a ring of 16 crimson blazewood bookshelves (SimpleLib's, worth 2; the best
       15 count = 30 points): strength 50 = TIER_MAX (EnchantingRules.tier). Empty-hand right click opens the EnchanterScreen; lapis
       lazuli and blaze powder are put into their slots by real clicks (window with stock, shot).
    2. Golden apple in the item slot: the button shows the Enchanted Golden Apple; a click turns it into one for 40 levels, 3 lapis and
       3 blaze powder (BS25-22). Tooltip of the result under the real pointer.
    3. Diamond boots: offers + slider row 0 pulled to its right end + Enchant; the result goes to the hotbar and is enchanted
       (status mainhandComponents).
    4. Astral Enchanter on a 5x5 Nihil Obsidian floor inside warped blazewood shelves: strength 60 = TIER_END, the endgame levels
       (Protection V / Feather Falling V, EnchantingRules.ENDGAME) are on. Boots / chestplate / leggings / helmet / book are tried until a
       row offers level V (menu data maxLevel = 5; a plain book enchant re-rolls the seed if none does), that slider goes to V, Enchant,
       and the tooltip shows the level V enchantment.
    5. Config screen through Mod Menu ("simple enchanting"), option names from the lang file.

Menu data (SortProbe "menuData" = EnchanterMenu's data slots in add order): 0 tier, 1 setup, 2 stage, 3-5 enchant id per row (-1 none),
6-8 max level, 9-11 chosen level, 12-14 value, 15-17 Vanilla max. Screen geometry (EnchanterScreen): item slot at (17, 20), slider track
x 54..162, row r track centre y = 42 + 28 r, button centre (27, 83), all relative to leftPos/topPos, which come from the item slot's
centre in the status.
Only a human can judge: the table tops, glyphs, slider colours, the button with book / apple, the text of prices and hints.
"""
import importlib.util
import re
import time
from pathlib import Path

_spec = importlib.util.spec_from_file_location('release_probe', Path(__file__).resolve().parents[2] / 'simplelib/tools/release_probe.py')
rp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rp)

SCREEN = 'EnchanterScreen'


def _md(st, i, default=None):
    d = st.get('menuData') or []
    return d[i] if i < len(d) else default


def _geo(st):
    """(leftPos, topPos, scale) of the enchanter screen from the item slot (menu index 0, centre = left + 17 + 8, top + 20 + 8)."""
    slot = next((x for x in st.get('slots', []) if x['index'] == 0), None)
    if not slot:
        return None
    sc = st['scale']
    return slot['x'] / sc - 25, slot['y'] / sc - 28, sc


def _stack(st, index):
    return next((x['stack'] for x in st.get('slots', []) if x['index'] == index), None)


def _build(s, table, floor, shelf):
    """Table 3 blocks east of the player on a 5x5 floor, a ring of shelves 2 blocks around it. Returns the table position."""
    x, y, z = rp.xyz(s.st())
    tx = x + 3
    s.chat(f'fill {tx - 2} {y - 1} {z - 2} {tx + 2} {y - 1} {z + 2} {floor}', 0.5)
    s.chat(f'fill {tx - 2} {y} {z - 2} {tx + 2} {y} {z + 2} {shelf}', 0.5)
    s.chat(f'fill {tx - 1} {y} {z - 1} {tx + 1} {y} {z + 1} minecraft:air', 0.5)
    s.chat(f'setblock {tx} {y} {z} {table}', 0.6)
    return tx, y, z


def _open(s, pos, tag):
    s.key('9', 0.4)
    rp.aim(s, pos[0] + 0.5, pos[1] + 0.7, pos[2] + 0.5)
    s.io.right_click()
    st = s.wait_screen(SCREEN, 10)
    ok = SCREEN in st.get('screen', '')
    s.check(f'{tag}: empty-hand right click opens the enchanter window', ok, f"{st.get('screen')} hit={st.get('hitBlock')}")
    return st if ok else None


def _move(s, own_cslot, index, want, tag):
    """Real clicks: pick the stack up from the own slot, put it into menu slot index; checks the slot shows want (prefix)."""
    s.click_slot(cslot=own_cslot, own=True)
    time.sleep(0.3)
    s.click_slot(index=index)
    st = rp.wait(s, lambda t: (_stack(t, index) or '').startswith(want), 4)
    got = _stack(st, index)
    s.check(f'{tag}: real clicks put {want} into slot {index}', (got or '').startswith(want), got)
    return st


def _take(s, index, own_cslot):
    """Real clicks: menu slot index -> own slot (empty)."""
    s.click_slot(index=index)
    time.sleep(0.3)
    s.click_slot(cslot=own_cslot, own=True)
    return rp.wait(s, lambda t: not _stack(t, index), 3)


def _click_gui(s, st, gx, gy):
    sc = st['scale']
    return s.click_xy(gx * sc, gy * sc, st)


def _slider_end(s, st, row):
    left, top, _ = _geo(st)
    _click_gui(s, st, left + 161, top + 42 + 28 * row)
    time.sleep(0.6)
    return s.st()


def _enchant(s, st):
    left, top, _ = _geo(st)
    _click_gui(s, st, left + 27, top + 83)
    time.sleep(1.0)
    return s.st()


def _offers(s):
    return rp.wait(s, lambda t: _md(t, 3, -1) >= 0, 4)


def release(s):
    s.chat('time set day', 0.3)
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.3)
    s.chat('xp add @s 200 levels', 0.3)
    for slot, item in (('hotbar.1', 'minecraft:lapis_lazuli 32'), ('hotbar.2', 'minecraft:blaze_powder 32'), ('hotbar.3', 'minecraft:diamond_boots'),
                       ('hotbar.4', 'minecraft:golden_apple')):
        s.chat(f'item replace entity @s {slot} with {item}', 0.3)

    # ---- 1: Dark Enchanter at strength 50
    dark = _build(s, 'simpleenchanting:dark_enchanter', 'simpleenchanting:blazing_obsidian', 'simplelib:crimson_blazewood_bookshelf')
    rp.aim(s, dark[0] + 0.5, dark[1] + 0.7, dark[2] + 0.5)
    s.shot('dark-table-world')
    st = _open(s, dark, 'Dark')
    if not st:
        return
    s.check('Dark Enchanter: strength 50 (15 blazewood shelves + Blazing floor)', _md(st, 0) == 50, st.get('menuData'))
    s.shot('dark-empty')
    _move(s, 1, 1, 'minecraft:lapis_lazuli', 'Dark')
    _move(s, 2, 2, 'minecraft:blaze_powder', 'Dark')

    # ---- 2: golden apple -> Enchanted Golden Apple
    _move(s, 4, 0, 'minecraft:golden_apple', 'Dark apple')
    time.sleep(0.6)
    s.shot('dark-apple-offer')
    st = _enchant(s, s.st())
    st = rp.wait(s, lambda t: (_stack(t, 0) or '').startswith('minecraft:enchanted_golden_apple'), 4)
    s.check('Enchant turns the golden apple into an Enchanted Golden Apple', (_stack(st, 0) or '').startswith('minecraft:enchanted_golden_apple'), _stack(st, 0))
    s.check('the apple took 3 lapis lazuli and 3 blaze powder', _stack(st, 1) == 'minecraft:lapis_lazuli x29' and _stack(st, 2) == 'minecraft:blaze_powder x29',
            (_stack(st, 1), _stack(st, 2)))
    slot0 = next((x for x in st.get('slots', []) if x['index'] == 0), None)
    rp.hover_slot(s, st, slot0, 'tooltip-enchanted-golden-apple')
    _take(s, 0, 4)

    # ---- 3: boots at the Dark Enchanter
    _move(s, 3, 0, 'minecraft:diamond_boots', 'Dark boots')
    st = _offers(s)
    s.check('Dark: offers for the boots (three rows, enchantment ids)', _md(st, 3, -1) >= 0, st.get('menuData'))
    rp.park_pointer(s)
    s.shot('dark-offers')
    st = _slider_end(s, s.st(), 0)
    s.check('Dark: real click on the slider end chooses a level for row 0', _md(st, 9, 0) > 0, st.get('menuData'))
    s.shot('dark-slider')
    st = _enchant(s, st)
    st = rp.wait(s, lambda t: _md(t, 3, 0) == -1, 4)
    s.check('Dark: Enchant enchants the boots (no more offers for the enchanted item)', _md(st, 3, 0) == -1, st.get('menuData'))
    _take(s, 0, 8)
    st = s.st()
    s.check('Dark: the enchanted boots in hand carry enchantments', 'enchantments' in st.get('mainhandComponents', ''), st.get('mainhandComponents', '')[:300])
    s.close()

    # ---- 4: Astral Enchanter at strength 60, a level V enchantment
    s.chat('clear @s', 0.3)
    for slot, item in (('hotbar.1', 'minecraft:lapis_lazuli 32'), ('hotbar.2', 'minecraft:blaze_powder 32'), ('hotbar.3', 'minecraft:diamond_boots'),
                       ('hotbar.4', 'minecraft:diamond_chestplate'), ('hotbar.5', 'minecraft:diamond_leggings'), ('hotbar.6', 'minecraft:diamond_helmet'),
                       ('inventory.0', 'minecraft:book'), ('inventory.1', 'minecraft:book'), ('inventory.2', 'minecraft:book')):
        s.chat(f'item replace entity @s {slot} with {item}', 0.3)
    s.chat('tp @s ~ ~ ~12', 1.0)
    astral = _build(s, 'simpleenchanting:astral_enchanter', 'simpleenchanting:nihil_obsidian', 'simplelib:warped_blazewood_bookshelf')
    rp.aim(s, astral[0] + 0.5, astral[1] + 0.7, astral[2] + 0.5)
    s.shot('astral-table-world')
    st = _open(s, astral, 'Astral')
    if not st:
        return
    s.check('Astral Enchanter: strength 60 (15 blazewood shelves + Nihil floor)', _md(st, 0) == 60, st.get('menuData'))
    _move(s, 1, 1, 'minecraft:lapis_lazuli', 'Astral')
    _move(s, 2, 2, 'minecraft:blaze_powder', 'Astral')
    rp.park_pointer(s)
    s.shot('astral-empty')
    found, books = None, [9, 10, 11]
    for rnd in range(3):
        for cslot, item in ((3, 'minecraft:diamond_boots'), (4, 'minecraft:diamond_chestplate'), (5, 'minecraft:diamond_leggings'),
                            (6, 'minecraft:diamond_helmet')):
            _move(s, cslot, 0, item, f'Astral try {rnd}')
            st = _offers(s)
            rows = [r for r in range(3) if _md(st, 6 + r, 0) >= 5]
            if rows:
                found = (cslot, item, rows[0])
                break
            _take(s, 0, cslot)
        if found or not books:
            break
        # no level V offered: enchant a book at level 1 of row 0 (a purchase re-rolls the enchantment seed) and try again
        b = books.pop(0)
        _move(s, b, 0, 'minecraft:book', f'Astral re-roll {rnd}')
        st = _offers(s)
        left, top, _ = _geo(st)
        mx = _md(st, 6, 1) or 1
        _click_gui(s, st, left + 54 + round(108 / mx), top + 42)
        time.sleep(0.5)
        _enchant(s, s.st())
        time.sleep(0.5)
        _take(s, 0, b)
    s.check('Astral: an item offers a level V enchantment (menu data max level 5)', found is not None, s.st().get('menuData'))
    if not found:
        return
    cslot, item, row = found
    st = s.st()
    s.shot('astral-offers-level-v')
    st = _slider_end(s, st, row)
    s.check(f'Astral: the slider of row {row} goes up to V', _md(st, 9 + row, 0) == 5, st.get('menuData'))
    rp.park_pointer(s)
    s.shot('astral-slider-level-v')
    st = _enchant(s, st)
    st = rp.wait(s, lambda t: _md(t, 3, 0) == -1, 4)
    s.check(f'Astral: Enchant enchants the {item}', _md(st, 3, 0) == -1, st.get('menuData'))
    s.key('9', 0.3)
    _take(s, 0, 8)
    st = s.st()
    comps = st.get('mainhandComponents', '')
    s.check('Astral: the item in hand carries Protection or Feather Falling (level V chosen above)',
            bool(re.search(r'protection|feather_falling', comps)), comps[:400])
    rp.hover_slot(s, st, s.slot_xy(st, cslot=8, own=True), 'tooltip-level-v')
    s.close()
    rp.park_pointer(s)

    # ---- 5: config screen
    s.chat('gamemode creative @s', 0.5)
    rp.config_screen(s, 'simpleenchanting', 'simple enchanting', 'Simple Enchanting')


SCENARIOS = {'simpleenchanting/release': release}
