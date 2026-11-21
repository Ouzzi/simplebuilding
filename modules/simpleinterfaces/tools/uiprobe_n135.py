"""N135 release polish, mods 50-52 (offen-vor-release.md section 3: Simple Questionary, Simple Dimensions, Simple Earth). None of the
three has a "Szenen" line; the scenes cover what N135 changed or what only the running game shows (kept short, world generation is heavy):

Simple Questionary
  n135-questionary-table  REAL right click on a Questionary Table (crosshair read back) opens QuestionaryScreen; 1 item slot + 36
                       inventory slots; the item slot sits in the horizontal middle of the window (N135 text fix: handbook/guide/table
                       text said "slot on the left, book on the right"); a REAL click puts the iron pickaxe from the hotbar into it
                       (stack read back); the REAL D key turns the page (screen field 'page' 0 -> 1 -> 2, A back to 1); closing gives the
                       pickaxe back (inventory count). Shots welcome / item in / each page.
Simple Questionary, Simple Dimensions, Simple Earth
  n135-config-screens  SimpleLib's generic config screen of each mod: every entry name is an English lang text (Questionary had NO
                       names before N135: "Recipe Facts" now), every option has a tooltip without raw keys; Earth's "Smallest edge"
                       tooltip names 64 (N135: was 8192). Shot per screen + tooltip under the real pointer.
Simple Dimensions
  n135-dimensions-skyblock  The 4-wide glowstone arch (AGGA/G..G/G..G/G..G) with a sea lantern 2 blocks behind it; REAL right click
                       with flint and steel on the floor under the interior (crosshair read back) -> sky_portal cells; stepping in
                       -> simpledimensions:skyblock (polled), feet in the generated return portal on bedrock, the Simple Modpack guide
                       handed over on the first journey (welcome advancement revoked first); leave the exit, wait out the cooldown,
                       step back in -> the overworld near the arch. Then the void rescue: 'execute in simpledimensions:skyblock' close
                       above the void floor -> back in the overworld (voidFall home). Shots.
  n135-dimensions-worlds  'execute in simpledimensions:mining': 5 sample columns read back - bedrock -64, dirt 189/190, grass 191
                       (>= 4 of 5 each, trees/ores may replace single blocks); 'execute in simpledimensions:travel': bedrock at y 0,
                       air above. Shots of both, then home.
  n135-dimensions-arch  A round obsidian arch (Vanilla would refuse it), REAL click with flint and steel on the bottom row (crosshair
                       read back) -> every interior cell is simpledimensions:shaped_nether_portal. Shot.
Simple Earth
  n135-earth-menu      UI cell "? EarthScreen": the REAL menu key (# = SDL BACKSLASH, keysym 'backslash' on the Xvfb US layout; if the
                       key does not open it, NOTE + the probe 'screen' command); the player view holds exactly the six sliders and the
                       four version buttons V25/V26/V27/V5, no drop-down; panel stays at the right edge (x > window middle); shot.
                       Then the orbit: tp to y 1700 above 0 ~ 0 looking down -> shots after 4 s and 12 s (the earth sharpens; FPS on
                       real hardware stays with the owner, W4C-05). Back to the ground.

Sounds: none of the three ships synthetic sounds that lack an owner listening (Questionary/Dimensions/Earth: no own sound events) - no
listening page.

Patterns (uiprobe_n122-n133 lessons): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62, targets <= 4 blocks,
crosshair read back before every real click; main hand = selected hotbar slot, cleared before giving; chat commands ~1-1.5 s each;
dimension travel polled ('execute if dimension'), never slept blind. Comparisons/assumptions are NOTE prints. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe135 --scale 2 \\
        --only n135-questionary-table,n135-config-screens,n135-dimensions-skyblock,n135-dimensions-worlds,n135-dimensions-arch,n135-earth-menu
"""
import importlib.util
import json
import re
import time
from pathlib import Path

TAG = 'N135'
FLOOR = 'minecraft:smooth_stone'
REPO = Path(__file__).resolve().parents[3]

_spec = importlib.util.spec_from_file_location('sort_probe', Path(__file__).resolve().parent / 'sort_probe.py')
sp = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(sp)


# ---------------------------------------------------------------- helpers
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say(s, cond, tag):
    s.chat(f'execute {cond} run say {TAG} {tag}', 0.35)


def _said(s, tags, settle=1.5):
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(TAG + ' ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


_seq = [0]


def _test(s, cond, tag):
    """One server condition, read back at once through a unique say tag (True/False)."""
    _seq[0] += 1
    t = f'{tag}-{_seq[0]}'
    _say(s, cond, t)
    return _said(s, [t], 1.2)[t]


def _poll(s, cond, tag, seconds):
    """True as soon as the condition holds (each try ~1.5 s), False after `seconds`."""
    end = time.time() + seconds
    while time.time() < end:
        if _test(s, cond, tag):
            return True
    return False


def _prep(s, mode='creative'):
    s.close()
    for k in ('Shift_L', 'space', 'w'):
        s.io.key_up(k)
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect clear @s', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _done(s):
    s.close()
    _center_pointer(s)
    s.chat('effect clear @s', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.3)


def _arena(s, dx, x0=-4, z0=-4, x1=8, z1=8, height=7, y_abs=None):
    """Moves dx blocks east (to height y_abs if given), lays a smooth stone floor under fresh air and returns the feet position."""
    x, y, z = _pos(s)
    x += dx
    if y_abs is not None:
        y = y_abs
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
    s.chat(f'fill {x + x0} {y} {z + z0} {x + x1} {y + height} {z + z1} minecraft:air', 0.6)
    s.chat(f'fill {x + x0} {y - 1} {z + z0} {x + x1} {y - 1} {z + z1} {FLOOR}', 0.6)
    s.chat('kill @e[type=minecraft:item,distance=..30]', 0.3)
    time.sleep(1.0)
    return x, y, z


def _aim(s, at, target, want, tag, tries=4, key='hitBlock'):
    """Feet at `at`, eyes onto `target` (tp facing turns from the feet: y - 1.62); waits until status `key` reads `want`."""
    hit = None
    for _ in range(tries):
        s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', 0.8)
        for _ in range(6):
            hit = s.st().get(key)
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _hit_is(s, want, tag, key='hitBlock', wait=2.0):
    """Reads the crosshair back right before a real click (the camera keeps turning a moment after a tp)."""
    end, hit = time.time() + wait, None
    while time.time() < end:
        hit = s.st().get(key)
        if hit == want:
            return True
        time.sleep(0.25)
    s.check(f'{tag}: crosshair on {want} before the click', False, hit)
    return False


def _hand(s, slot, stack, tag):
    """Empties hotbar slot `slot`, puts the stack in, selects it with the REAL number key and reads the main hand back."""
    s.chat(f'item replace entity @s hotbar.{slot} with minecraft:air', 0.3)
    s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.4)
    s.io.key(str(slot + 1), 0.5)
    item = stack.split('[', 1)[0].split(' ', 1)[0]
    ok = _test(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-mainhand')
    s.check(f'{tag}: {item} is in the main hand (selected hotbar slot {slot + 1})', ok)
    return ok


def _click(s, after=0.7, button=3):
    s.io.button(button, True)
    time.sleep(0.12)
    s.io.button(button, False)
    time.sleep(after)


def _center_pointer(s):
    st = s.st()
    if 'windowX' in st:
        s.io.move(st['windowX'] + 640, st['windowY'] + 360)
        time.sleep(0.3)


def _total(s, item):
    n = 0
    for v in s.st().get('inventory', []):
        if v and v.split(' x')[0] == item:
            n += int(v.rsplit(' x', 1)[1]) if ' x' in v else 1
    return n


def _plain(text):
    return re.sub('§.', '', text or '').strip()


def _lang(mod, loc='en_us'):
    return json.loads((REPO / f'modules/{mod}/shared/resources/assets/{mod}/lang/{loc}.json').read_text(encoding='utf-8'))


def _home(s, home):
    """Back to the overworld spot the scene started from."""
    s.chat(f'execute in minecraft:overworld run tp @s {home[0] + 0.5} {home[1]} {home[2] + 0.5} 0 0', 1.5)
    s.check('back in the overworld', _poll(s, 'if dimension minecraft:overworld', 'home', 15))


# ---------------------------------------------------------------- Simple Questionary
TABLE = 'simplequestionary:questionary_table'


def questionary_table(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    try:
        s.chat(f'setblock {x} {y} {z + 3} {TABLE}', 0.6)
        if not _hand(s, 0, 'minecraft:iron_pickaxe', 'questionary'):
            return
        me, centre = (x + 0.5, y, z + 0.5), (x + 0.5, y + 0.5, z + 3.5)
        if not (_aim(s, me, centre, TABLE, 'table') and _hit_is(s, TABLE, 'table')):
            return
        _click(s, 0.5)
        st = s.wait_screen('QuestionaryScreen', 10)
        s.check('REAL right click opens the Questionary screen', 'QuestionaryScreen' in st.get('screen', ''), st.get('screen'))
        if 'QuestionaryScreen' not in st.get('screen', ''):
            return
        own = [v for v in st['slots'] if v['own']]
        desk = [v for v in st['slots'] if not v['own']]
        s.check('1 item slot + 36 inventory slots', len(desk) == 1 and len(own) == 36, f'{len(desk)} + {len(own)}')
        if desk:
            # window 1280 px wide: the middle is x = 640 (slot x is the slot centre in window pixels; a slot is 16 gui px = 32 px)
            s.check('the item slot sits in the horizontal middle (N135 text: "slot in the middle")', abs(desk[0]['x'] - 640) <= 20,
                    f"slot x {desk[0]['x']}, window middle 640")
            print(f"NOTE n135 questionary: item slot at {desk[0]['x']},{desk[0]['y']} (window pixels)", flush=True)
        s.shot('1-welcome')
        st = s.click_slot(cslot=0, own=True)
        s.check('REAL click lifts the pickaxe from hotbar slot 1', st and st.get('carried', '').startswith('minecraft:iron_pickaxe'),
                st and st.get('carried'))
        st = s.click_slot(cslot=0, own=False)
        item = st and s.slot_xy(st, 0, False)
        s.check('REAL click puts the pickaxe into the item slot', item and item['stack'] == 'minecraft:iron_pickaxe x1', item and item['stack'])
        _center_pointer(s)
        time.sleep(1.0)
        page0 = s.st().get('fields', {}).get('page')
        s.shot('2-item-in')
        pages = [page0]
        for name, key in (('3-page-d', 'd'), ('4-page-d', 'd'), ('5-page-a', 'a')):
            s.io.key(key, 0.8)
            pages.append(s.st().get('fields', {}).get('page'))
            s.shot(name)
        s.check('REAL D/D/A turn the pages (page field)', pages[0] is not None and pages[1:] == [pages[0] + 1, pages[0] + 2, pages[0] + 1],
                pages)
        s.close()
        time.sleep(0.8)
        s.check('closing gives the pickaxe back', _total(s, 'minecraft:iron_pickaxe') == 1, s.st().get('inventory'))
    finally:
        _done(s)


# ---------------------------------------------------------------- config screens (all three)
SCREENS = (
    # mod, at least n options, must show, tooltip that must contain
    ('simplequestionary', 1, 'Recipe Facts', None),
    ('simpledimensions', 9, 'Give Guide on First Trip', None),
    ('simpleearth', 10, 'Smallest edge', ('Smallest edge', 'Default: 64.')),
)


def config_screens(s):
    _prep(s)
    for mod, least, must, tip in SCREENS:
        lang = _lang(mod)
        names = {_plain(v) for k, v in lang.items() if k.startswith(f'text.autoconfig.{mod}.option.') and not k.endswith('@Tooltip')}
        cats = {_plain(v) for k, v in lang.items() if '.category.' in k or k == f'text.autoconfig.{mod}.title'}
        s.close()
        st = s.open_config(mod, 2.5)
        screen = st.get('screen', 'none')
        if screen == 'none':
            s.check(f'{mod}: config screen opens', False, screen)
            continue
        time.sleep(0.8)
        st = s.st()
        # Cloth Config keeps invisible internal entries labelled with a UUID: not shown, not options
        entries = [e for e in st.get('entries', []) if _plain(e.get('label')).strip()
                   and not re.fullmatch(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}', _plain(e.get('label')).strip())]
        shown = [_plain(e['label']) for e in entries]
        options = [n for n in shown if n not in cats]
        bad = [n for n in options if n not in names]
        no_tip = [n for e, n in zip(entries, shown) if n in names and not [x for x in e.get('tooltip', []) if _plain(x)]]
        raw_tip = [f'{n}: {x}' for e, n in zip(entries, shown) for x in e.get('tooltip', [])
                   if 'autoconfig' in x or '@Tooltip' in x or re.fullmatch(r'[a-z0-9_.]+\.[a-z0-9_.]+', _plain(x))]
        s.check(f'{mod}: config screen lists at least {least} options', len(options) >= least, f'{screen}: {shown}')
        s.check(f'{mod}: every entry name is an English lang text (no raw path)', options and not bad, bad)
        s.check(f'{mod}: every option has a tooltip, none is a raw key', options and not no_tip and not raw_tip, (no_tip, raw_tip[:4]))
        title = _plain(st.get('title', ''))
        s.check(f'{mod}: screen title is the lang title, not a raw key', title and 'autoconfig' not in title, title)
        if must:
            s.check(f'{mod}: shows "{must}"', must in shown, shown)
        if tip:
            e = next((e for e, n in zip(entries, shown) if n == tip[0]), None)
            text = ' '.join(_plain(x) for x in (e or {}).get('tooltip', []))
            s.check(f'{mod}: tooltip of "{tip[0]}" names "{tip[1]}"', tip[1] in text, text)
        s.shot(f'{mod}')
        s.io.move(*s.abs(st, 380, 120))
        time.sleep(1.2)
        s.shot(f'{mod}-tooltip')
        _center_pointer(s)
    s.close()


# ---------------------------------------------------------------- Simple Dimensions
SKY = 'simpledimensions:sky_portal'
IN_SKY = 'if dimension simpledimensions:skyblock'
IN_OVER = 'if dimension minecraft:overworld'


def dimensions_skyblock(s):
    _prep(s)
    # a normal surface height: on the flat probe world (y -60) the 1:1 arrival lies at Skyblock's floor (min y -64)
    x, y, z = _arena(s, 80, x0=-6, z0=-6, x1=10, z1=8, height=8, y_abs=64)
    home = (x, y, z)
    s.chat('simpledimensions config set accessEnabled true', 0.6)
    s.chat('simpledimensions config set skyblockEnabled true', 0.6)
    s.chat('simpledimensions config set giveGuideOnFirstTravel true', 0.6)
    s.chat('advancement revoke @s only simpledimensions:guide/welcome', 0.5)
    try:
        # arch recipe 1 along X: rows top -> bottom AGGA / G..G / G..G / G..G; the A cells stay air (checked)
        ax, az = x + 1, z + 3
        rows = ('AGGA', 'G..G', 'G..G', 'G..G')
        for r, row in enumerate(rows):
            for c, ch in enumerate(row):
                if ch == 'G':
                    s.chat(f'setblock {ax + c} {y + 3 - r} {az} minecraft:glowstone', 0.3)
        s.chat(f'setblock {ax + 1} {y} {az - 2} minecraft:sea_lantern', 0.4)  # separate light behind the arch, within 3 blocks
        cell = (ax + 1, y, az)
        if not _hand(s, 0, 'minecraft:flint_and_steel', 'sky'):
            return
        me, floor = (ax + 1.5, y, az + 2.5), (ax + 1.5, y - 0.02, az + 0.5)
        if not (_aim(s, me, floor, FLOOR, 'sky ignite') and _hit_is(s, FLOOR, 'sky ignite')):
            return
        _click(s, 1.0)
        lit = _poll(s, f'if block {cell[0]} {cell[1]} {cell[2]} {SKY}', 'sky-lit', 8)
        s.check('REAL click with flint and steel lights the glowstone arch (sky_portal cells)', lit)
        all_cells = all(_test(s, f'if block {ax + c} {y + r} {az} {SKY}', 'sky-cell') for c in (1, 2) for r in (0, 1, 2))
        s.check('all 6 interior cells are sky_portal', all_cells)
        s.shot('1-arch-lit')
        if not lit:
            return
        s.chat('clear @s', 0.4)
        s.chat(f'tp @s {cell[0] + 0.5} {y} {az + 0.5} 0 0', 0.5)
        arrived = _poll(s, IN_SKY, 'in-sky', 25)
        s.check('stepping in travels to simpledimensions:skyblock', arrived)
        if not arrived:
            return
        time.sleep(2.0)
        s.check('arrival: feet in the generated return portal', _test(s, f'if block ~ ~ ~ {SKY}', 'exit-feet'))
        s.check('arrival: bedrock island under the feet', _test(s, 'if block ~ ~-1 ~ minecraft:bedrock', 'island'))
        s.check('first journey hands over the Simple Modpack guide', _poll(s, 'if items entity @s container.* simplelib:modpack_guide', 'guide', 6))
        print(f'NOTE n135 skyblock arrival at {_pos(s)}', flush=True)
        s.chat('tp @s ~ ~ ~2.5 180 10', 1.0)
        s.shot('2-skyblock-island')
        s.check('still in skyblock after leaving the exit (no bounce)', _test(s, IN_SKY, 'stay'))
        time.sleep(4.0)  # travel cooldown 60 ticks
        s.chat('tp @s ~ ~ ~-2.5', 0.5)
        back = _poll(s, IN_OVER, 'back', 25)
        s.check('stepping into the exit returns to the overworld', back)
        if back:
            p = _pos(s)
            near = abs(p[0] - cell[0]) <= 8 and abs(p[2] - cell[2]) <= 8
            s.check('the return lands next to the arch', near, f'{p} vs arch {cell}')
            time.sleep(1.0)
            s.shot('3-returned')
        # void rescue: voidFall home (default) - close above the void floor of skyblock (minY -64, rescue below -61)
        s.chat('execute in simpledimensions:skyblock run tp @s 0.5 -56 0.5', 0.5)
        rescued = _poll(s, IN_OVER, 'rescued', 25)
        s.check('falling into the skyblock void brings you back to the overworld (voidFall home)', rescued)
        print(f'NOTE n135 void rescue landed at {_pos(s)}', flush=True)
    finally:
        _home(s, home)
        _done(s)


def _columns(s, dim, y, block, tag):
    hits = sum(_test(s, f'in {dim} if block {cx} {y} {cz} {block}', f'{tag}-{y}') for cx, cz in ((0, 0), (37, 5), (-41, 18), (12, -29), (-6, 44)))
    return hits


def dimensions_worlds(s):
    _prep(s)
    home = tuple(_pos(s))
    try:
        s.chat('execute in simpledimensions:mining run tp @s 0.5 230 0.5 0 60', 1.0)
        s.check('in simpledimensions:mining', _poll(s, 'if dimension simpledimensions:mining', 'mining', 25))
        for cx, cz in ((37, 5), (-41, 18), (12, -29), (-6, 44)):
            s.chat(f'execute in simpledimensions:mining run forceload add {cx} {cz}', 0.4)
        time.sleep(4.0)
        ok = all(_poll(s, f'in simpledimensions:mining if block {cx} -64 {cz} minecraft:bedrock', 'mining-loaded', 30)
                 for cx, cz in ((0, 0), (37, 5), (-41, 18), (12, -29), (-6, 44)))
        s.check('mining: the 5 sample chunks generated (bedrock at -64 polled)', ok)
        for y, block in ((-64, 'minecraft:bedrock'), (189, 'minecraft:dirt'), (190, 'minecraft:dirt'), (191, 'minecraft:grass_block')):
            n = _columns(s, 'simpledimensions:mining', y, block, 'mining')
            s.check(f'mining: {block} at y {y} in >= 4 of 5 columns', n >= 4, f'{n}/5')
        n = _columns(s, 'simpledimensions:mining', 100, 'minecraft:stone', 'mining-stone')
        print(f'NOTE n135 mining: stone at y 100 in {n}/5 columns (ores may replace single blocks)', flush=True)
        s.shot('1-mining')
        for cx, cz in ((37, 5), (-41, 18), (12, -29), (-6, 44)):
            s.chat(f'execute in simpledimensions:mining run forceload remove {cx} {cz}', 0.3)
        s.chat('execute in simpledimensions:travel run tp @s 0.5 3 0.5 0 30', 1.0)
        s.check('in simpledimensions:travel', _poll(s, 'if dimension simpledimensions:travel', 'travel', 25))
        s.check('travel: bedrock at y 0', _poll(s, 'in simpledimensions:travel if block 0 0 0 minecraft:bedrock', 'travel-bedrock', 20))
        s.check('travel: air right above the bedrock plane', _test(s, 'in simpledimensions:travel if block 3 1 3 minecraft:air', 'travel-air'))
        time.sleep(1.5)
        s.shot('2-travel')
    finally:
        _home(s, home)
        _done(s)


ARCH = ('  OOO  ', ' O...O ', 'O.....O', 'O.....O', 'O.....O', 'OOOOOOO')


def dimensions_arch(s):
    _prep(s)
    x, y, z = _arena(s, 120, x0=-4, z0=-4, x1=10, z1=8, height=8)
    s.chat('simpledimensions config set shapedNetherPortals true', 0.6)
    try:
        ax, az = x - 1, z + 3
        top = y + len(ARCH) - 1
        cells = []
        for r, row in enumerate(ARCH):
            for c, ch in enumerate(row):
                if ch == 'O':
                    s.chat(f'setblock {ax + c} {top - r} {az} minecraft:obsidian', 0.3)
                elif ch == '.':
                    cells.append((ax + c, top - r, az))
        if not _hand(s, 0, 'minecraft:flint_and_steel', 'arch'):
            return
        bx = ax + 3
        me, target = (bx + 0.5, y, az + 3.0), (bx + 0.5, y + 0.98, az + 0.5)
        if not (_aim(s, me, target, 'minecraft:obsidian', 'arch ignite') and _hit_is(s, 'minecraft:obsidian', 'arch ignite')):
            return
        _click(s, 1.0)
        c0 = cells[-1]
        lit = _poll(s, f'if block {c0[0]} {c0[1]} {c0[2]} simpledimensions:shaped_nether_portal', 'arch-lit', 8)
        s.check('REAL click lights the round obsidian arch as a Shaped Nether Portal', lit)
        if lit:
            n = sum(_test(s, f'if block {cx} {cy} {cz} simpledimensions:shaped_nether_portal', 'arch-cell') for cx, cy, cz in cells)
            s.check(f'every interior cell is shaped_nether_portal ({len(cells)})', n == len(cells), f'{n}/{len(cells)}')
        s.chat(f'tp @s {bx + 0.5} {y} {az + 5.5} facing {bx + 0.5} {y + 2.5 - 1.62:.2f} {az + 0.5}', 1.5)
        s.shot('1-arch')
    finally:
        _done(s)


# ---------------------------------------------------------------- Simple Earth
EARTH = 'com.simplebuilding.modules.simpleearth.client.EarthScreen'
SIMPLE = ('Render height', 'Detail level', 'Render distance', 'Atmosphere', 'Moon size', 'Show other players')


def earth_menu(s):
    _prep(s)
    home = tuple(_pos(s))
    s.chat('simpleclient simpleearth set developerMode false', 1.0)
    try:
        s.close()
        _center_pointer(s)
        s.io.key('backslash', 1.0)
        st = s.wait_screen('EarthScreen', 4)
        real = 'EarthScreen' in st.get('screen', '')
        print(f'NOTE n135 earth: the REAL # key (keysym backslash) {"opened" if real else "did NOT open"} the menu', flush=True)
        if not real:
            sp.command('screen', value=EARTH)
            st = s.wait_screen('EarthScreen', 8)
        s.check('the Simple Earth menu is open', 'EarthScreen' in st.get('screen', ''), st.get('screen'))
        if 'EarthScreen' not in st.get('screen', ''):
            return
        rows = [w['label'] for w in s.widgets(st) if ':' in w['label']]
        s.check('player view: exactly the six sliders', len(rows) == 6 and all(any(r.startswith(n) for r in rows) for n in SIMPLE), rows)
        s.check('player view: four version buttons V25/V26/V27/V5', all(s.find_widget(f'V{v}', st=st, contains=False) for v in (25, 26, 27, 5)),
                s.labels(st))
        s.check('player view: no drop-down of other versions', not s.find_widget('More versions', st=st), s.labels(st))
        xs = [w['x'] for w in s.widgets(st)]
        s.check('the panel sits at the right edge (all widgets right of the middle)', xs and min(xs) > 640, f'min x {min(xs) if xs else None}')
        s.shot('1-menu')
        s.close()
        # orbit: above the render height (1536 + blend band 128)
        s.chat('effect give @s slow_falling 120 0 true', 0.3)
        s.chat('tp @s 0.5 1700 0.5 0 90', 1.0)
        time.sleep(4.0)
        s.shot('2-orbit-4s')
        time.sleep(8.0)
        s.shot('3-orbit-12s')
        print(f'NOTE n135 earth orbit shots at {_pos(s)} (look and sharpening for a human; FPS stays W4C-05)', flush=True)
    finally:
        s.chat(f'tp @s {home[0] + 0.5} {home[1]} {home[2] + 0.5} 0 0', 1.5)
        _done(s)


SCENARIOS = {
    'n135-questionary-table': questionary_table,
    'n135-config-screens': config_screens,
    'n135-dimensions-skyblock': dimensions_skyblock,
    'n135-dimensions-worlds': dimensions_worlds,
    'n135-dimensions-arch': dimensions_arch,
    'n135-earth-menu': earth_menu,
}
