"""N65 (KONZEPT-N65-LINKED-2026-11-15 §8/§9): game screenshots of linked opening - the HUD tiles after marking, a marked chest
with a furnace (A: box below, hotbar strip, inventory tab), with the inventory (E) and two double chests (C side by side or
B tabs), each with the probe status (slot centres in window pixels, GUI scale) saved next to the shot for the
pixel count (/root/sb-tools-b/ig57u/mock/measure_shots.py). Scenario: simpleinterfaces/n65 - run once with --scale 3 and once
with --scale 2 (window 1280x720); the shot names carry the scale. Slot positions in the status are slot centres in window px. Survival, because creative never shows the mark in E."""
import json
import time


def _save(s, name):
    dest = s.out / 'shots'
    dest.mkdir(parents=True, exist_ok=True)
    st = s.st()
    (dest / f"{s.scenario.replace('/', '-')}-{name}.json").write_text(json.dumps(st, indent=1))
    return st


def _pos(s):
    return [int(v) for v in s.st()['pos'].replace(',', ' ').split()]


def _sneak_use(s):
    """Sneak + right-click with an empty main hand on the block under the crosshair (marks / unmarks it)."""
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.4)
        s.io.right_click()
        time.sleep(0.6)
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.5)
    return s.st()


def _linked(st, own_slots):
    """Window slots of the appended (marked) containers: menu index behind the menu's own slots."""
    return [x for x in st.get('slots', []) if x['index'] >= own_slots]


def _visible(st, slot):
    return slot is not None and 0 <= slot['y'] <= 720 and 0 <= slot['x'] <= 1280


def _player(st, cslot):
    """The player's inventory slot cslot (0-8 hotbar, 9-35 main) of the open menu."""
    for x in st.get('slots', []):
        if x.get('own') and x['cslot'] == cslot:
            return x
    return None


def run(s):
    s.chat('gamemode survival @s', 0.5)
    s.chat('clear @s', 0.3)
    s.close()
    scale = s.st().get('scale', '?')
    tag = f's{scale}'
    x, y, z = _pos(s)
    # --- 1: mark a chest, HUD tile top left (variant A), no screen opens
    chest = s.stage('minecraft:chest', 2, 0, 0)
    st = _sneak_use(s)
    s.check(f'{tag}: sneak + right-click marks without opening the chest', st.get('screen') == 'none', st.get('screen'))
    s.close()
    time.sleep(1.0)
    s.shot(f'{tag}-hud-tiles')
    _save(s, f'{tag}-hud-tiles')

    # --- 2: chest + furnace (A: furnace on top, marked box below, hotbar strip; inventory behind the tab)
    s.stage('minecraft:furnace', -2, 0, 0)
    s.io.right_click()
    st = s.wait_screen('FurnaceScreen', 10)
    ok = 'FurnaceScreen' in st.get('screen', '')
    s.check(f'{tag}: furnace opens', ok, st.get('screen'))
    if ok:
        time.sleep(0.8)
        st = _save(s, f'{tag}-chest-furnace')
        s.shot(f'{tag}-chest-furnace')
        linked = _linked(st, 3 + 36)
        s.check(f'{tag}: furnace shows the 27 marked slots', len(linked) == 27, f'{len(linked)} linked slots')
        hotbar = _player(st, 0)
        s.check(f'{tag}: hotbar stays visible below the box (A)', _visible(st, hotbar), hotbar)
        main = _player(st, 9)
        s.check(f'{tag}: main inventory hidden behind its tab', main is not None and not _visible(st, main), main)
        if _visible(st, hotbar):
            # the inventory tab, second tile left of the box (LinkedLayout A, one mark): from the hotbar strip's first slot centre
            # (image x + 16, cut + 99) to the tab centre (image x - 14, cut + 38)
            g = st.get('scale', 2)
            st = s.click_xy(hotbar['x'] - 30 * g, hotbar['y'] - 61 * g, st)
            time.sleep(0.6)
            st = _save(s, f'{tag}-chest-furnace-inventory')
            s.shot(f'{tag}-chest-furnace-inventory')
            main = _player(st, 9)
            s.check(f'{tag}: inventory tab shows the main inventory', _visible(st, main), main)
    s.close()

    # --- 3: chest + E (inventory image on top, box below; at 720p scale 3 beside)
    s.key('e', 1.0)
    st = s.wait_screen('InventoryScreen', 8)
    ok = 'InventoryScreen' in st.get('screen', '')
    s.check(f'{tag}: E opens the inventory', ok, st.get('screen'))
    if ok:
        time.sleep(0.8)
        st = _save(s, f'{tag}-chest-e')
        s.shot(f'{tag}-chest-e')
        linked = _linked(st, 46)
        s.check(f'{tag}: E shows the marked chest', len(linked) == 27 and all(_visible(st, l) for l in linked[:9]), f'{len(linked)} linked slots')
    s.close()

    # --- 4: double chest marked + double chest opened (C side by side when the GUI is >= 360 wide, else B tabs)
    s.io.key('Escape', 0.8)  # Escape in the world ends every mark (no pause menu while marked)
    s.close()
    s.chat(f'setblock {chest[0]} {chest[1]} {chest[2]} minecraft:air', 0.3)
    s.chat(f'setblock {x + 2} {y} {z} minecraft:chest[type=left,facing=north]', 0.3)
    s.chat(f'setblock {x + 3} {y} {z} minecraft:chest[type=right,facing=north]', 0.3)
    s.chat(f'setblock {x - 2} {y} {z} minecraft:air', 0.3)
    s.chat(f'setblock {x - 3} {y} {z + 2} minecraft:chest[type=left,facing=north]', 0.3)
    s.chat(f'setblock {x - 2} {y} {z + 2} minecraft:chest[type=right,facing=north]', 0.3)
    s.face(x + 2, y, z)
    st = _sneak_use(s)
    s.check(f'{tag}: double chest marked without opening', st.get('screen') == 'none', st.get('screen'))
    s.close()
    s.face(x - 2, y, z + 2)
    s.io.right_click()
    st = s.wait_screen('ContainerScreen', 10)
    ok = 'ContainerScreen' in st.get('screen', '')
    s.check(f'{tag}: second double chest opens', ok, st.get('screen'))
    if ok:
        time.sleep(0.8)
        st = _save(s, f'{tag}-doublechests')
        s.shot(f'{tag}-doublechests')
        linked = _linked(st, 54 + 36)
        first = next((x for x in st.get('slots', []) if x['index'] == 0), None)
        beside = bool(linked) and first is not None and all(l['x'] < first['x'] for l in linked if _visible(st, l))
        s.check(f'{tag}: both double chests whole (C: marked box left of the screen, or B: tabs)', len(linked) == 54, f'{len(linked)} linked, beside={beside}')
    s.close()
    s.io.key('Escape', 0.8)
    s.close()
    s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simpleinterfaces/n65': run}
