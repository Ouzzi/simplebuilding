"""simplestats-2: item statistics tooltip (Shift) and the statistics window (key I) with the REAL mouse and keyboard.

Scenario for modules/simpleinterfaces/tools/ui_probe.py. A diamond pickaxe with ready statistics is given by command (component
simplestats:item_stats, codec = StatsComponents.CODEC: id/since/totals/detail{e,o}/records). Survival, key E -> InventoryScreen, the
real pointer over hotbar slot 0:
  * tooltip without Shift ("Hold Shift for more") vs. with Shift held (extra value lines + "Press I ...") - the text is not in the
    status, so the check is a PIXEL DIFFERENCE of the two screenshots (Pillow); the wording is for a human;
  * key I over the item opens ItemStatsScreen (Vanilla Button "Done" is a real widget), mouse wheel / End / Home / click on the scroll
    bar scroll the list - `scroll` is not in the status either, so the list area is compared in the screenshots (changed pixels);
  * "Done" returns to the inventory (the parent), a second visit with no screen open: key I opens the HELD item's statistics, Done closes.
List geometry of StatsLayout.of(guiW, guiH) with ASSUMPTION gui = 1280 x 720 / scale: panel = min(360, gui - 12) x min(260, gui - 12)
centred, list from panel.y + 30, x + 6, width panel - 20, height up to the Done button (20 px, 6 px gaps), scroll bar 6 px wide right of it.
"""
import time

try:
    from PIL import Image, ImageChops
except ImportError:  # the pixel checks then fail with a clear message instead of an exception
    Image = ImageChops = None

STATS = ('{id:424242L,since:20741L,totals:{blocks_mined:640L,kills:41L,damage_dealt:120000L,hits:60L},'
         'detail:{blocks_mined:{e:{"minecraft:stone":300L,"minecraft:dirt":120L,"minecraft:granite":60L,"minecraft:diorite":40L,'
         '"minecraft:andesite":30L,"minecraft:gravel":25L,"minecraft:sand":20L,"minecraft:oak_log":15L,"minecraft:coal_ore":12L,'
         '"minecraft:iron_ore":10L,"minecraft:copper_ore":5L,"minecraft:deepslate":3L}},kills:{e:{"minecraft:zombie":41L}}},'
         'records:{biggest_hit:640L}}')
PICKAXE = f'minecraft:diamond_pickaxe[simplestats:item_stats={STATS}]'


def _open_image(s, name):
    """Screenshot, then the image (None when Pillow is missing or the file cannot be read)."""
    s.shot(name)
    if Image is None:
        return None
    try:
        return Image.open(s.out / 'shots' / f'{s.scenario}-{name}.png').convert('RGB')
    except Exception:  # noqa: BLE001 - a read problem is a failed check, not an exception
        return None


def _changed(a, b, box=None):
    """Number of pixels that differ clearly (brightness delta > 24) between two images, inside box = (x0, y0, x1, y1)."""
    if a is None or b is None or a.size != b.size:
        return -1
    if box:
        a, b = a.crop(box), b.crop(box)
    return sum(ImageChops.difference(a, b).convert('L').histogram()[25:])


def _layout(st):
    sc = st['scale']
    gw, gh = 1280 // sc, 720 // sc
    w, h = max(120, min(360, gw - 12)), max(90, min(260, gh - 12))
    px, py = (gw - w) // 2, (gh - h) // 2
    list_top = py + 24 + 6
    footer = py + h - 6 - 20
    list_h = max(18, footer - 6 - list_top)
    lx, lw = px + 6, w - 12 - 6 - 2
    return dict(sc=sc, list=(lx, list_top, lw, list_h), bar=(lx + lw + 2, list_top, 6, list_h))


def stats(s):
    s.chat('gamemode survival @s', 0.8)
    s.chat('clear @s', 0.4)
    s.chat(f'give @s {PICKAXE}', 0.8)
    s.key('1', 0.4)  # hotbar slot 0 = the hand
    st = s.st()
    s.check('the pickaxe with statistics is in hotbar slot 0', st['inventory'][0] == 'minecraft:diamond_pickaxe x1', st['inventory'][:3])
    # ---- inventory, pointer over the item
    st = s.key('e', 1.0)
    st = s.wait_screen('InventoryScreen', 10)
    s.check('E opens the survival inventory', 'InventoryScreen' in st.get('screen', '') and 'Creative' not in st.get('screen', ''), st.get('screen'))
    slot = s.slot_xy(st, 0, True)
    if not slot or not slot['stack']:
        s.check('hotbar slot 0 reported with the item', False, slot)
        return
    s.io.move(*s.abs(st, slot['x'], slot['y']))
    time.sleep(1.5)
    plain = _open_image(s, 'tooltip-no-shift')
    s.io.key_down('Shift_L')
    time.sleep(1.2)
    shifted = _open_image(s, 'tooltip-shift')
    changed = _changed(plain, shifted)
    s.check('holding Shift changes the tooltip (more lines, > 1500 pixels differ)', changed > 1500, f'{changed} pixels differ' if changed >= 0 else 'PIL missing / shots unreadable')
    s.io.key_up('Shift_L')
    time.sleep(0.6)
    # ---- key I over the item opens the statistics window
    s.io.move(*s.abs(st, slot['x'], slot['y']))
    time.sleep(0.6)
    st = s.key('i', 1.0)
    st = s.wait_screen('ItemStatsScreen', 10)
    s.check('key I over the item opens ItemStatsScreen', 'ItemStatsScreen' in st.get('screen', ''), st.get('screen'))
    if 'ItemStatsScreen' not in st.get('screen', ''):
        return
    s.check('the window has the real Done button', s.find_widget('Done', st=st) is not None, s.labels(st))
    time.sleep(0.5)
    top = _open_image(s, 'stats-top')
    lay = _layout(st)
    sc = lay['sc']
    lx, ly, lw, lh = lay['list']
    box = (lx * sc, ly * sc, (lx + lw) * sc, (ly + lh) * sc)
    # ---- mouse wheel over the list
    s.wheel((lx + lw // 2) * sc, (ly + lh // 2) * sc, clicks=4, down=True, st=st)
    wheeled = _open_image(s, 'stats-wheel-down')
    n = _changed(top, wheeled, box)
    s.check('mouse wheel down scrolls the list (list area changes)', n > 2000, n)
    # ---- End / Home keys
    s.key('End', 0.8)
    end = _open_image(s, 'stats-end')
    n = _changed(top, end, box)
    s.check('End shows the bottom of the list (differs from the top)', n > 2000, n)
    s.key('Home', 0.8)
    home = _open_image(s, 'stats-home')
    n = _changed(top, home, box)
    s.check('Home returns to the top (same as at the start)', 0 <= n < 600, n)
    # ---- left click on the lower end of the scroll bar, then on the upper end (IG31-36 fixed the 26.3 button number)
    bx, by, bw, bh = lay['bar']
    s.click_xy((bx + bw // 2) * sc, (by + bh - 2) * sc, st)
    time.sleep(0.6)
    low = _open_image(s, 'stats-bar-bottom')
    n = _changed(top, low, box)
    s.check('click on the bottom of the scroll bar scrolls down', n > 2000, n)
    s.click_xy((bx + bw // 2) * sc, (by + 2) * sc, st)
    time.sleep(0.6)
    up = _open_image(s, 'stats-bar-top')
    n = _changed(top, up, box)
    s.check('click on the top of the scroll bar scrolls back up', 0 <= n < 600, n)
    # ---- Done returns to the inventory (the parent screen)
    if s.click_widget('Done'):
        st = s.wait_screen('InventoryScreen', 8)
        s.check('Done goes back to the inventory', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
    s.close()
    # ---- no screen: key I shows the HELD item's statistics, Done closes it
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    st = s.key('i', 1.0)
    st = s.wait_screen('ItemStatsScreen', 10)
    s.check('key I without a screen opens the held item\'s statistics', 'ItemStatsScreen' in st.get('screen', ''), st.get('screen'))
    s.shot('held-item')
    if s.click_widget('Done'):
        st = s.wait_screen('none', 8)
        s.check('Done closes the window (no parent)', st.get('screen') == 'none', st.get('screen'))
    s.close()


# N78 (owner 16.11.): screenshots for the preview - stats tooltip with the Top-1 icon (without/with Shift), the key page (new defaults
# U / arrows / unbound highlight) and grates seen from inside (Simple Ores brass + Vanilla copper with the template's inner sides).
SMOOTH = ('{id:424243L,since:20741L,totals:{blocks_mined:2L},detail:{blocks_mined:{e:{"minecraft:smooth_stone":2L}}}}')
ROD = ('{id:424244L,since:20741L,totals:{catches:9L},detail:{catches:{e:{"minecraft:cod":5L,"minecraft:salmon":3L,"minecraft:name_tag":1L}}}}')


def _hover(s, st, index, name):
    slot = s.slot_xy(st, index, True)
    if not slot or not slot['stack']:
        s.check(f'hotbar slot {index} reported with the item', False, slot)
        return None, None
    s.io.move(*s.abs(st, slot['x'], slot['y']))
    time.sleep(1.5)
    plain = _open_image(s, f'{name}-tooltip')
    s.io.key_down('Shift_L')
    time.sleep(1.2)
    shifted = _open_image(s, f'{name}-tooltip-shift')
    s.io.key_up('Shift_L')
    time.sleep(0.6)
    return plain, shifted


def n78(s):
    s.chat('gamemode survival @s', 0.8)
    s.chat('time set noon', 0.4)
    s.clear_and_give(f'minecraft:diamond_pickaxe[simplestats:item_stats={SMOOTH}]', PICKAXE,
                     f'minecraft:fishing_rod[simplestats:item_stats={ROD}]')
    s.key('1', 0.4)
    s.key('e', 1.0)
    st = s.wait_screen('InventoryScreen', 10)
    s.check('E opens the survival inventory', 'InventoryScreen' in st.get('screen', ''), st.get('screen'))
    if 'InventoryScreen' in st.get('screen', ''):
        for index, name in ((0, 'pickaxe-smooth'), (1, 'pickaxe-many'), (2, 'rod')):
            plain, shifted = _hover(s, st, index, name)
            n = _changed(plain, shifted)
            s.check(f'{name}: Shift changes the tooltip (top list)', n > 800, n)
    s.close()
    # ---- key page: U opens it (N78 default), screenshots of the first pages
    st = s.key('u', 1.2)
    st = s.wait_screen('KeysScreen', 8)
    s.check('U opens the Simple key page', 'KeysScreen' in st.get('screen', ''), st.get('screen'))
    if 'KeysScreen' in st.get('screen', ''):
        s.shot('keys-1')
        for page in (2, 3, 4):
            s.wheel(640, 360, clicks=6, down=True, st=st)
            time.sleep(0.6)
            s.shot(f'keys-{page}')
    s.close()
    # ---- grates from inside: a hollow 5 x 5 x 5 brass grate room, the west wall Vanilla copper grate, HUD hidden
    s.chat('gamemode creative @s', 0.6)
    s.chat('fill ~-2 ~-1 ~-2 ~2 ~3 ~2 simpleores:brass_grate hollow', 1.0)
    s.chat('fill ~-2 ~ ~-1 ~-2 ~2 ~1 minecraft:copper_grate', 0.8)
    s.chat('fill ~2 ~ ~-1 ~2 ~2 ~1 simpleores:stainless_steel_grate', 0.8)
    s.key('F1', 0.4)
    for name, yaw, pitch in (('grate-inside-north', 180, 10), ('grate-inside-copper-west', 90, 10), ('grate-inside-steel-east', -90, 10),
                             ('grate-inside-floor', 0, 80), ('grate-inside-corner', 135, -30)):
        s.chat(f'tp @s ~ ~ ~ {yaw} {pitch}', 1.0)
        time.sleep(1.0)
        s.shot(name)
    s.chat('tp @s ~6 ~1 ~6 135 25', 1.2)
    time.sleep(1.0)
    s.shot('grate-outside')
    s.key('F1', 0.4)
    s.check('grate screenshots taken', True, 'compare by eye: inner sides visible through the holes')


SCENARIOS = {'simplestats': stats, 'simplestats/n78': n78}
