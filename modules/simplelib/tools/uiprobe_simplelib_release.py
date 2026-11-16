"""simplelib-7 (offen-vor-release B5): SimpleLib's player-facing parts in pictures for the owner card, with the REAL mouse/keys.
Scenario for modules/simpleinterfaces/tools/ui_probe.py (its own file: uiprobe_simplelib.py holds the world-reload scenario that runs
only by name):

  simplelib/release
    1. Modpack handbook (GUIDE01): simplelib:modpack_guide used in the air opens the guide screen; start page and two pages on.
    2. Warm food: cooked beef / bread with the component simplelib:warm (until far in the future = full warmth) next to cold beef -
       glow in the inventory slots, tooltip of the warm stack, steam in the hand (first person, two shots: the particles are random),
       third person, and a dropped warm item on the ground (glow of the item entity). Check: the held stack carries simplelib:warm.
    3. Rarity: Biome Brush (simpletools, CURSED) and Ender Gem (simplemobs, MYTHIC) - tooltips under the real pointer (RarityRules).
    4. Creative tab rows (CL-2): every displayed creative tab whose namespace starts with "simple" is opened by a real click (SortProbe
       "tabs" = id, centre, Fabric page; "selectedTab"), one shot at the top and one after three wheel steps down.
    5. Free Look: the client log must show "SimpleLib feature free_look: on, asked for by [...]" naming Simple Quality of Life or
       Simple Earth (both are in the integration client, LibFeaturesFabric). Real left Alt held while the pointer moves: the body's
       yaw (SortProbe "yaw") stays, the camera turns (shot); without Alt the same move turns the body (baseline). If the baseline does not
       turn either, the X pointer does not reach the game's grabbed mouse here and the yaw check is reported as information only.
    6. Config screen through Mod Menu ("simplelib", SimpleLib's own Cloth screen), option names from the lang file.

Not covered here (also on the a-w1c list): SlotAlert / orbit camera, the mob-drops page of the Abyssal Wither.
Only a human can judge: the guide's look, glow colour and steam amount, the Cursed flames / Mythic opal animation, the row layout of
each tab, the free-look camera feel.
"""
import importlib.util
import re
import time
from pathlib import Path

_spec = importlib.util.spec_from_file_location('release_probe', Path(__file__).resolve().parent / 'release_probe.py')
rp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(rp)

WARM = '[simplelib:warm={until:2000000000L}]'


def _guide(s):
    s.chat('item replace entity @s weapon.mainhand with simplelib:modpack_guide', 0.5)
    x, y, z = rp.xyz(s.st())
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 -50', 0.8)
    s.io.right_click()
    st = s.wait_screen('Guide', 8)
    ok = 'Guide' in st.get('screen', '')
    s.check('modpack guide: use in the air opens the guide screen', ok, st.get('screen'))
    if not ok:
        return
    time.sleep(0.8)
    s.shot('guide-start')
    for n in (2, 3):
        s.key('Right', 0.9)
        s.shot(f'guide-page-{n}')
    s.close()


def _warm(s):
    s.chat('gamemode survival @s', 0.6)
    s.chat('clear @s', 0.3)
    for slot, item in (('hotbar.0', f'minecraft:cooked_beef{WARM} 4'), ('hotbar.1', 'minecraft:cooked_beef 4'), ('hotbar.2', f'minecraft:bread{WARM} 4'),
                       ('hotbar.3', 'simpletools:biome_brush'), ('hotbar.4', 'simplemobs:ender_gem')):
        s.chat(f'item replace entity @s {slot} with {item}', 0.3)
    s.key('1', 0.5)
    st = s.st()
    comps = st.get('mainhandComponents', '')
    s.check('warm beef in hand carries simplelib:warm', 'simplelib:warm' in comps, comps[:300])
    inv = st.get('inventory', [])
    s.check('rarity items exist: simpletools:biome_brush and simplemobs:ender_gem', len(inv) > 4 and inv[3].startswith('simpletools:biome_brush')
            and inv[4].startswith('simplemobs:ender_gem'), inv[:5])
    rp.park_pointer(s)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    if 'InventoryScreen' not in st.get('screen', ''):
        s.check('E opens the inventory', False, st.get('screen'))
        return
    s.shot('warm-inventory-glow')
    rp.hover_slot(s, st, s.slot_xy(st, cslot=0, own=True), 'tooltip-warm-beef')
    rp.hover_slot(s, st, s.slot_xy(st, cslot=1, own=True), 'tooltip-cold-beef')
    rp.hover_slot(s, st, s.slot_xy(st, cslot=3, own=True), 'tooltip-cursed-biome-brush')
    time.sleep(1.0)
    s.shot('tooltip-cursed-biome-brush-later')  # the flames move: a second frame
    rp.hover_slot(s, st, s.slot_xy(st, cslot=4, own=True), 'tooltip-mythic-ender-gem')
    time.sleep(1.0)
    s.shot('tooltip-mythic-ender-gem-later')
    s.close()
    rp.park_pointer(s)
    # steam in the hand
    x, y, z = rp.xyz(s.st())
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 20', 0.8)
    s.key('1', 0.4)
    for n in (1, 2):
        time.sleep(0.9)
        s.shot(f'warm-steam-hand-{n}')
    s.key('F5', 0.8)
    time.sleep(0.6)
    s.shot('warm-steam-third-person')
    s.key('F5', 0.4)
    s.key('F5', 0.6)
    # a dropped warm item on the ground
    s.key('q', 0.5)
    time.sleep(1.5)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 55', 0.8)
    time.sleep(0.5)
    s.shot('warm-dropped-item-glow')


def _tabs(s):
    s.chat('gamemode creative @s', 0.6)
    s.close()
    s.key('e', 1.2)
    st = s.wait_screen('CreativeModeInventoryScreen', 8)
    if 'CreativeModeInventoryScreen' not in st.get('screen', ''):
        s.check('creative: E opens the creative inventory', False, st.get('screen'))
        return
    tabs = [t for t in st.get('tabs', []) if t['id'].split(':')[0].startswith('simple')]
    s.check('creative: SortProbe lists the Simple tabs', bool(tabs), st.get('tabsError') or f"{len(st.get('tabs', []))} tabs")
    opened, missed = 0, []
    for tab in tabs:
        st = s.st()
        # page of the tab (Fabric pages the tabs): turn with the real '<' / '>' buttons
        for _ in range(12):
            if 'page' not in tab or st.get('tabPage', tab['page']) == tab['page']:
                break
            arrow = '>' if st.get('tabPage', 0) < tab['page'] else '<'
            w = next((w for w in s.widgets(st) if rp.plain(w['label']) == arrow), None)
            if not w:
                break
            s.click_xy(w['x'], w['y'], st)
            time.sleep(0.6)
            st = s.st()
        cur = next((t for t in st.get('tabs', []) if t['id'] == tab['id']), tab)
        s.click_xy(cur['x'], cur['y'], st)
        time.sleep(0.8)
        st = s.st()
        if st.get('selectedTab') != tab['id']:
            missed.append(f"{tab['id']} (selected {st.get('selectedTab')}, page {st.get('tabPage')}/{tab.get('page')})")
            continue
        opened += 1
        name = re.sub('[^a-z0-9]+', '-', tab['id'].lower())
        rp.park_pointer(s)
        time.sleep(0.4)
        s.shot(f'tab-{name}')
        s.wheel(640, 330, 3, down=True)
        s.shot(f'tab-{name}-scrolled')
    s.check(f'creative: every Simple tab opens with a real click ({opened}/{len(tabs)})', tabs and not missed, missed[:12])
    s.close()


def _free_look(s):
    try:
        log = (s.out / 'client.log').read_text(errors='replace')
    except OSError:
        log = ''
    line = next((ln for ln in log.splitlines() if 'SimpleLib feature free_look:' in ln), '')
    s.check('client log: "SimpleLib feature free_look: on, asked for by [...]" with Simple QoL or Simple Earth',
            'free_look: on, asked for by [' in line and ('simplequalityoflife' in line or 'simpleearth' in line), line[-300:] or 'no line')
    s.chat('gamemode creative @s', 0.5)
    s.close()
    x, y, z = rp.xyz(s.st())
    s.chat(f'setblock {x + 4} {y} {z} minecraft:gold_block', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} -90 0', 1.0)
    st = s.st()
    cx, cy = st['windowX'] + 640, st['windowY'] + 360

    def turn():
        before = s.st().get('yaw')
        s.io.move(cx, cy)
        time.sleep(0.3)
        for step in range(1, 9):
            s.io.move(cx + 30 * step, cy)
            time.sleep(0.05)
        time.sleep(0.6)
        return before, s.st().get('yaw')

    b0, b1 = turn()
    baseline = b0 is not None and b1 is not None and abs(b1 - b0) > 2
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} -90 0', 1.0)
    s.shot('freelook-before')
    s.io.key_down('Alt_L')
    try:
        time.sleep(0.4)
        a0, a1 = turn()
        s.shot('freelook-alt-held-camera-turned')
    finally:
        s.io.key_up('Alt_L')
    time.sleep(0.6)
    s.shot('freelook-released')
    if baseline:
        s.check('Free Look: with left Alt held the pointer turns only the camera (body yaw stays)', a0 is not None and a1 is not None and abs(a1 - a0) < 1,
                f'baseline {b0}->{b1}, with Alt {a0}->{a1}')
    else:
        s.check('(info) Free Look yaw check skipped: the pointer move did not turn the body even without Alt here', True,
                f'baseline {b0}->{b1}, with Alt {a0}->{a1}')


def release(s):
    s.chat('time set day', 0.3)
    s.chat('clear @s', 0.3)
    _guide(s)
    _warm(s)
    _tabs(s)
    _free_look(s)
    rp.config_screen(s, 'simplelib', 'simplelib', 'SimpleLib')


SCENARIOS = {'simplelib/release': release}
