"""N129 release polish, mods 32-34 (offen-vor-release.md section 3, "Szenen" of Simple Foundry, Simple Arrows, Simple Riding), word for word:

Simple Foundry  "Pult-Fenster (Stück als Zähler „+1“)."
  n129-foundry-desk     A valid tank (bricks, 3 melting chambers, glass front, fire grate, desk on the west wall, melts merged in). Crosshair
                        read back on the desk, REAL empty-hand right click -> FoundryScreen, title "Foundry Desk", 13 desk slots (12 places
                        + fuel), tier 0 in the synced data. Raw iron lifted by a REAL slot click, REAL right click on place 3: the cursor
                        loses one and the window's waiting counter for iron (synced data 18/19 = FoundryScreen's " +N", N = units / 9) reads
                        9 = "+1" (the W4C run expected the piece as an item in the place - it is fed into the tank at once); a second piece
                        -> 18 = "+2"; the first locked place (4) refuses. Arrow button "▼" -> selected level 2. Then the desk is lifted
                        like a player (iron block in the hand, REAL right click, NBT Tier 1) and the window shows 6 open places (place 6
                        takes a piece, place 7 refuses). Shots of the window with the counter.
Simple Arrows   "Flug-Gefühl, Audit-Fragen F4/F5 (W4C-07)."
  n129-arrows-flight    Fletching table: REAL right click -> FletchingScreen, title "Fletching", 5 table slots (tip, shaft, fletching,
                        result, potion). Flight with a REAL bow (right button held 1.4 s, crafted arrows in the offhand, level shot south,
                        pitch 0): where the arrow lands (data get Pos) for stick + feather, end rod + feather (half gravity) and stick +
                        phantom membrane (30 % less gravity) - the end rod and the membrane arrows must land farther; a night shot of the
                        end rod's glowing trail. F4 (decided A, AUDSB31B: "the target burns 3 seconds longer per hit, at most 10 seconds
                        in all"): one REAL bow hit with a blaze rod arrow on a husk (crosshair read back on it) sets 1..60 fire ticks;
                        eight blaze rod arrows in a row (summoned with motion, ~1 s apart) leave at most 200 ticks (without the cap
                        ~340). F5 (decided A, "wind burst on impact, without damage; unlike a wind charge it also pushes armor stands and
                        dropped items"): a breeze rod arrow hits the floor between an armor stand and a dropped item - both are pushed
                        away, the armor stand keeps its health.
Simple Riding   "Hufpanel, Reiten mit Lag (Option B, Rückholungen)."
  n129-riding-panel     Tamed, saddled horse (execute summon + data merge, NoAI). Crosshair read back on the horse, REAL Shift held >= 0.8 s
                        + empty-hand right click -> the mount inventory (title "Horse"), the four hoof slots come last; REAL click puts a
                        copper shoe on, REAL shift-clicks the next three tiers, cobblestone is refused, a netherite shoe swaps with the
                        copper one, shift-click on a hoof slot gives the shoe back. N129 removed the dead horseshoe_panel.png - the panel
                        is drawn in code (HorseshoeScreenMixin, UiBoxes); the shots show it is still drawn, the log has no missing-texture
                        line for it.
  n129-riding-lag       Option B (safety.lenientMovementBudget, built in 91151a7cf7) with REAL riding: a tamed, saddled horse (speed
                        0.3375) mounted by a REAL right click, Ctrl + W held 7 s on a 120-block track while the server is stalled by
                        32 256-block fills above the track (each stall bunches the client's vehicle packets into one server tick, like a laggy
                        connection). A background thread samples the client position every 20 ms; a correction ("Rückholung") shows as
                        a step backwards. Run 1 lenient (default): the horse gallops >= 40 blocks with no step back. Run 2 strict
                        (config set safety.lenientMovementBudget false, reset afterwards): reported for comparison - steps back here
                        show the stalls really bunched packets (if strict shows none either, the stalls were too short and run 1 says
                        little: probe assumption, not a game fault). `tick query` after each run gives the server tick times. The real
                        network delay (100 ms) feel stays with the owner (W4C-41); the numbers are gametest lag_jitter.

Sounds: none of the three mods ships its own sounds (no sounds.json) - nothing synthetic, no listening page.

Patterns (uiprobe_n122-n127 lessons): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62; real clicks only after the
crosshair target was read back (status hitBlock / hitEntity); main hand = selected hotbar slot; Shift held >= 0.8 s; chat commands
~1-1.5 s each; counts from "Test passed. Count: N"; summon + data merge where the spawn logic matters. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe129 --scale 2 \\
        --only n129-foundry-desk,n129-arrows-flight,n129-riding-panel,n129-riding-lag
"""
import importlib.util
import re
import threading
import time
from pathlib import Path

TAG = 'N129'
REPO = Path(__file__).resolve().parents[3]
FLOOR = 'minecraft:smooth_stone'


def _load(name, rel):
    spec = importlib.util.spec_from_file_location(name, REPO / rel)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


_SP = _load('sort_probe_n129', 'modules/simpleinterfaces/tools/sort_probe.py')  # status() without waiting for fresh ticks


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


def _check_said(s, name, tags):
    got = _said(s, tags)
    miss = [t for t, ok in got.items() if not ok]
    s.check(name, not miss, f'{len(tags) - len(miss)}/{len(tags)}' + (f' missing {miss[:12]}' if miss else ''))
    return not miss


def _mark(s, marker):
    s.chat(f'say {TAG} MARK {marker}', 0.35)


def _after(s, marker, pattern, lines_after=12):
    """Last regex match of pattern in the log lines after the last 'N129 MARK <marker>' line."""
    time.sleep(1.2)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {marker}')), default=-1)
    found = None
    for ln in lines[idx + 1: idx + 1 + lines_after] if idx >= 0 else []:
        m = re.search(pattern, ln)
        if m:
            found = m
    return found


def _data_num(s, marker, command):
    """Runs a '/data get ...' that reports one number; None when the log shows none."""
    _mark(s, marker)
    s.chat(command, 0.5)
    m = _after(s, marker, r'has the following entity data: (-?[\d.]+)')
    return float(m.group(1)) if m else None


def _data_pos(s, marker, selector):
    """'data get entity <selector> Pos' -> (x, y, z) floats or None."""
    _mark(s, marker)
    s.chat(f'data get entity {selector} Pos', 0.5)
    m = _after(s, marker, r'has the following entity data: \[(-?[\d.]+)d, (-?[\d.]+)d, (-?[\d.]+)d\]')
    return tuple(float(v) for v in m.groups()) if m else None


def _prep(s, mode='creative', night_vision=True):
    s.close()
    s.io.button(3, False)
    s.io.key_up('Shift_L')
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    s.chat('difficulty normal', 0.3)  # the probe world starts in Peaceful: no husk could be summoned
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect clear @s', 0.3)
    if night_vision:
        s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _arena(s, dx, x0=-3, z0=-3, x1=14, z1=24, height=6):
    """Moves dx blocks east, lays a smooth stone floor under fresh air (x0..x1, z0..z1 around the feet) and returns the feet position."""
    x, y, z = _pos(s)
    x += dx
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
    s.chat(f'fill {x + x0} {y} {z + z0} {x + x1} {y + height} {z + z1} minecraft:air', 0.6)
    s.chat(f'fill {x + x0} {y - 1} {z + z0} {x + x1} {y - 1} {z + z1} {FLOOR}', 0.6)
    s.chat('kill @e[type=minecraft:item,distance=..60]', 0.3)
    time.sleep(1.0)
    return x, y, z


def _aim(s, at, target, want, tag, tries=4, key='hitBlock'):
    """Feet at `at`, eyes onto the world point `target` (tp facing from the feet: y - 1.62); waits until the crosshair reads back
    `want` in status `key` (hitBlock / hitEntity; None = nothing). True when it does."""
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


def _hand(s, slot, stack, tag):
    """Puts the stack into hotbar slot `slot`, selects it with the REAL number key and reads the main hand back."""
    s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.4)
    s.io.key(str(slot + 1), 0.5)
    item = stack.split('[', 1)[0].split(' ', 1)[0]
    _say(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-mainhand')
    return _check_said(s, f'{tag}: {item} is in the main hand (selected hotbar slot {slot + 1})', [f'{tag}-mainhand'])


def _click(s, after=0.7, button=3):
    s.io.button(button, True)
    time.sleep(0.12)
    s.io.button(button, False)
    time.sleep(after)


def _sneak_click(s, tag, button=3):
    """REAL Shift held >= 0.8 s (sneaking read back), then the click."""
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.9)
        s.check(f'{tag}: the client sneaks before the click (real Shift held)', bool(s.st().get('sneaking')), s.last.get('sneaking'))
        _click(s, 0.6, button)
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.4)


def _stack_at(st, index):
    for sl in st.get('slots', []):
        if sl['index'] == index:
            return sl['stack']
    return None


def _wait_stack(s, index, want, seconds=5):
    end = time.time() + seconds
    st = s.st()
    while True:
        got = _stack_at(st, index) or ''
        if got == want or time.time() > end:
            return st, got
        time.sleep(0.4)
        st = s.st()


def _own_index(st, text):
    for sl in st.get('slots', []):
        if sl['own'] and sl['stack'] == text:
            return sl['index']
    return None


def _click_own(s, text, **kw):
    st = s.st()
    idx = _own_index(st, text)
    if idx is None:
        s.check(f'own slot with {text} present', False, [sl['stack'] for sl in st.get('slots', []) if sl['own'] and sl['stack']][:12])
        return None
    return s.click_slot(index=idx, **kw)


def _put_back(s):
    """Cursor stack into the first empty own slot."""
    st = s.st()
    if st.get('carried'):
        back = next((sl['index'] for sl in st['slots'] if sl['own'] and not sl['stack']), None)
        if back is not None:
            s.click_slot(index=back)
            time.sleep(0.4)


def _cursor_count(st):
    c = st.get('carried', '') or ''
    return int(c.rsplit(' x', 1)[1]) if ' x' in c else 0


# ---------------------------------------------------------------- Simple Foundry
DESK = 'simplefoundry:foundry_desk'
IRON = 2  # Metal.IRON ordinal; waiting units of a metal = menuData[14 + 2 * ordinal] | menuData[15 + 2 * ordinal] << 15


def _waiting_iron(st):
    d = st.get('menuData') or []
    return d[14 + 2 * IRON] | d[15 + 2 * IRON] << 15 if len(d) >= 20 else None


def _desk_window(s, pos, stand, tag):
    if not _aim(s, stand, (pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5), DESK, tag):
        return None
    _click(s, after=0.3)
    st = s.wait_screen('FoundryScreen', 10)
    ok = 'FoundryScreen' in st.get('screen', '')
    s.check(f'{tag}: REAL empty-hand right click opens the desk window (FoundryScreen)', ok, st.get('screen'))
    if not ok:
        s.close()
        return None
    time.sleep(0.8)
    return s.st()


def _feed_piece(s, place, tag, want_waiting):
    """Raw iron (hotbar slot 4) lifted by a REAL click, REAL right click on desk place `place` (0-based), cursor back; returns the
    window's waiting iron units afterwards."""
    lifted = s.click_slot(cslot=4, own=True)
    before = _cursor_count(lifted or {})
    s.check(f'{tag}: raw iron lifted onto the cursor', (lifted or {}).get('carried', '').startswith('minecraft:raw_iron'), (lifted or {}).get('carried'))
    s.click_slot(index=place, right=True)
    time.sleep(1.2)
    st = s.st()
    after = _cursor_count(st)
    waiting = _waiting_iron(st)
    s.check(f'{tag}: place {place + 1} takes ONE piece (cursor {before} -> {before - 1})', after == before - 1, f'{before} -> {after}')
    s.check(f'{tag}: the window counts it as waiting iron {want_waiting} units = "+{want_waiting // 9}" (synced data 18/19)',
            waiting == want_waiting, f'waiting {waiting}, place shows {_stack_at(st, place)!r}')
    s.click_slot(cslot=4, own=True)
    time.sleep(0.4)
    return waiting


def foundry_desk(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    for cmd in (f'fill {x - 1} {y} {z + 5} {x + 1} {y + 4} {z + 7} simplefoundry:foundry_brick',
                f'fill {x} {y + 1} {z + 6} {x} {y + 3} {z + 6} simplefoundry:melting_chamber',
                f'fill {x - 1} {y + 1} {z + 5} {x + 1} {y + 3} {z + 5} minecraft:glass',
                f'setblock {x - 1} {y + 1} {z + 6} {DESK}[facing=south]',
                f'setblock {x + 1} {y + 1} {z + 6} simplefoundry:fire_grate'):
        s.chat(cmd, 0.6)
    pos = (x - 1, y + 1, z + 6)
    s.chat(f'data merge block {pos[0]} {pos[1]} {pos[2]} {{Melt_gold:81,Melt_copper:162,Melt_iron:99,Pending_iron:0}}', 0.8)
    stand = (x - 3 + 0.5, y, z + 6.5)  # two blocks west of the desk, its west face is free
    s.io.key('1', 0.4)  # hotbar slot 0 (empty) is the hand
    s.chat('item replace entity @s hotbar.4 with minecraft:raw_iron 40', 0.5)

    st = _desk_window(s, pos, stand, 'tier 1')
    if not st:
        return
    s.check('desk window title is the lang text "Foundry Desk"', st.get('title') == 'Foundry Desk', st.get('title'))
    places = [t for t in st['slots'] if not t['own']]
    s.check('desk window reports 13 desk slots (12 places + fuel)', len(places) == 13, len(places))
    d = st.get('menuData') or []
    s.check('synced data: 20 values, tier 0 (3 places), structure ok (status 0)', len(d) == 20 and d[0] == 0 and d[3] == 0, d[:8])
    s.check('window counter before: no waiting iron', _waiting_iron(st) == 0, _waiting_iron(st))
    s.shot('1-window-empty')
    if _feed_piece(s, 2, 'piece 1', 9) == 9:
        s.shot('2-window-plus1')
    if _feed_piece(s, 2, 'piece 2', 18) == 18:
        s.shot('3-window-plus2')
    _say(s, f'if data block {pos[0]} {pos[1]} {pos[2]} {{Pending_iron:18}}', 'pending-18')
    _check_said(s, 'the desk block entity holds the two pieces as Pending_iron 18 (fed into the tank, not kept in the place)', ['pending-18'])
    # the first locked place refuses
    s.click_slot(cslot=4, own=True)
    s.click_slot(index=3, right=True)
    time.sleep(1.2)
    st = s.st()
    s.check('tier 1: the first locked place (4) refuses the piece', not _stack_at(st, 3) and _waiting_iron(st) == 18,
            f'place {_stack_at(st, 3)!r}, waiting {_waiting_iron(st)}')
    _put_back(s)
    if s.click_widget('▼', name='arrow button'):
        time.sleep(0.8)
        st = s.st()
        s.check('arrow button "▼": the selected level wraps from 0 to the top (synced selected 2)', (st.get('menuData') or [0, -1])[1] == 2,
                (st.get('menuData') or [])[:3])
        s.shot('4-arrow-clicked')
    s.close()
    time.sleep(0.5)

    # lift the desk like a player: iron block in the hand, REAL right click on it
    s.chat('item replace entity @s hotbar.0 with minecraft:iron_block 1', 0.5)
    if _aim(s, stand, (pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5), DESK, 'upgrade'):
        _click(s, after=1.0)
        _say(s, f'if data block {pos[0]} {pos[1]} {pos[2]} {{Tier:1}}', 'tier-1')
        _check_said(s, 'REAL right click with an iron block lifts the desk (NBT Tier 1)', ['tier-1'])
    s.chat('item replace entity @s hotbar.0 with minecraft:air', 0.4)
    # the refused piece's put-back may land in another slot: refill the raw iron where _feed_piece takes it
    s.chat('item replace entity @s hotbar.4 with minecraft:raw_iron 38', 0.4)
    st = _desk_window(s, pos, stand, 'tier 2')
    if not st:
        return
    s.check('tier 2: synced tier 1 (6 places)', (st.get('menuData') or [-1])[0] == 1, (st.get('menuData') or [])[:3])
    _feed_piece(s, 5, 'tier 2 place 6', 27)
    s.click_slot(cslot=4, own=True)
    s.click_slot(index=6, right=True)
    time.sleep(1.2)
    st = s.st()
    s.check('tier 2: place 7 stays locked', not _stack_at(st, 6) and _waiting_iron(st) == 27, f'place {_stack_at(st, 6)!r}, waiting {_waiting_iron(st)}')
    _put_back(s)
    s.shot('5-window-tier2-plus3')
    s.close()
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- Simple Arrows
def _arrow(shaft='stick', fletching='feather', tip='flint', count=16):
    return f'simplearrows:crafted_arrow[simplearrows:arrow_parts={{tip:"{tip}",shaft:"{shaft}",fletching:"{fletching}"}}] {count}'


def _arrow_nbt(shaft):
    return ('{id:"simplearrows:crafted_arrow",count:1,components:{"simplearrows:arrow_parts":'
            f'{{tip:"flint",shaft:"{shaft}",fletching:"feather"}}}}}}')


def _draw(s, hold=1.4):
    s.io.button(3, True)
    try:
        time.sleep(hold)
    finally:
        s.io.button(3, False)


ARROW = 'simplearrows:crafted_arrow'


def arrows_flight(s):
    _prep(s)
    x, y, z = _arena(s, 40, x0=-6, z0=-4, x1=6, z1=80, height=6)
    me = (x + 0.5, y, z + 0.5)

    # ---- the fletching table window
    s.chat(f'setblock {x} {y} {z + 3} minecraft:fletching_table', 0.4)
    s.io.key('9', 0.4)  # empty slot
    if _aim(s, me, (x + 0.5, y + 0.5, z + 3.5), 'minecraft:fletching_table', 'table'):
        _click(s, after=0.3)
        st = s.wait_screen('FletchingScreen', 10)
        ok = 'FletchingScreen' in st.get('screen', '')
        s.check('REAL right click on the fletching table opens FletchingScreen', ok, st.get('screen'))
        if ok:
            time.sleep(0.6)
            st = s.st()
            s.check('fletching window title is the lang text "Fletching"', st.get('title') == 'Fletching', st.get('title'))
            table = [sl for sl in st['slots'] if not sl['own']]
            s.check('fletching window: 5 table slots (tip, shaft, fletching, result, potion)', len(table) == 5, len(table))
            s.shot('1-fletching-window')
        s.close()
    s.chat(f'setblock {x} {y} {z + 3} minecraft:air', 0.3)

    # ---- flight: REAL bow, level shot south, where the arrow lands
    if not _hand(s, 0, 'minecraft:bow', 'bow'):
        return
    landed = {}
    for name, shaft, fletching in (('stick', 'stick', 'feather'), ('end_rod', 'end_rod', 'feather'), ('membrane', 'stick', 'phantom_membrane')):
        s.chat(f'kill @e[type={ARROW}]', 0.3)
        s.chat(f'item replace entity @s weapon.offhand with {_arrow(shaft, fletching)}', 0.4)
        if name == 'end_rod':
            s.chat('time set midnight', 0.8)
        s.chat(f'tp @s {me[0]} {me[1]} {me[2]} 0 0', 1.0)
        time.sleep(0.6)
        _draw(s)
        if name == 'end_rod':
            time.sleep(0.15)
            s.shot('2-end-rod-trail-night')
        time.sleep(2.5)
        p = _data_pos(s, f'land-{name}', f'@e[type={ARROW},limit=1,sort=nearest]')
        landed[name] = None if p is None else p[2] - me[2]
        s.check(f'flight {name}: the REAL bow shot a {shaft}/{fletching} arrow that landed (data get Pos)', p is not None,
                f'{p} -> {landed[name] if landed[name] is None else round(landed[name], 1)} blocks')
        if name == 'end_rod':
            s.chat('time set noon', 0.5)
    if None not in landed.values():
        s.check('flight: the end rod arrow (half gravity) lands farther than the stick arrow', landed['end_rod'] >= landed['stick'] + 1.5,  # measured 2.8-ish farther from 27 blocks
                f'stick {landed["stick"]:.1f}, end rod {landed["end_rod"]:.1f}')
        # real bow shots spread (Vanilla inaccuracy): membrane measured 30.1 / 28.7 / 26.1 against stick 24.8 / 27.4 / 26.3 - its
        # 30 % lower gravity (0.035) is the gametest's exact check; here only reported
        print(f'NOTE flight: stick {landed["stick"]:.1f}, phantom membrane {landed["membrane"]:.1f} (gravity 0.035 vs 0.05: gametest)', flush=True)
    s.chat(f'tp @s {me[0]} {me[1]} {me[2]} 0 20', 0.8)
    s.shot('3-landed-arrows')
    s.chat(f'kill @e[type={ARROW}]', 0.3)

    # ---- F4: blaze rod shaft, fire at most 10 s in all
    hz = z + 4
    s.chat(f'execute positioned {x + 0.5} {y} {hz + 0.5} summon minecraft:husk run data merge entity @s '
           '{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[180f,0f],attributes:[{id:"minecraft:max_health",base:1000d}],Health:1000f}', 1.0)
    s.chat(f'item replace entity @s weapon.offhand with {_arrow("blaze_rod")}', 0.4)
    if _aim(s, me, (x + 0.5, y + 1.2, hz + 0.5), 'minecraft:husk', 'F4 husk', key='hitEntity'):
        _draw(s)
        time.sleep(0.4)
        fire1 = _data_num(s, 'fire-1', 'data get entity @e[type=minecraft:husk,limit=1,sort=nearest] Fire')
        s.check('F4: one REAL bow hit with a blaze rod arrow sets the husk burning (Fire 1..60 ticks)', fire1 is not None and 0 < fire1 <= 60, fire1)
        s.shot('4-husk-burning')
    readings = []
    for i in range(8):
        s.chat(f'summon {ARROW} {x + 0.5} {y + 1.2} {hz - 2.5} {{Motion:[0.0d,0.0d,1.6d],pickup:0b,damage:0.5d,item:{_arrow_nbt("blaze_rod")}}}', 0.6)
    fire8 = _data_num(s, 'fire-8', 'data get entity @e[type=minecraft:husk,limit=1,sort=nearest] Fire')
    readings.append(fire8)
    s.check('F4: eight blaze rod hits in a row stack the fire but leave at most 200 ticks (10 s; without the cap ~340)',
            fire8 is not None and 60 < fire8 <= 200, f'Fire {fire8}')
    s.chat('kill @e[type=minecraft:husk,distance=..20]', 0.3)
    s.chat(f'kill @e[type={ARROW}]', 0.3)

    # ---- F5: breeze rod shaft, wind burst pushes an armor stand and a dropped item, no damage
    bz = z + 8
    # 1.2 blocks either side of the impact point (burst radius 1.2, knockback reach about twice that); items move only with
    # mob_griefing on (Vanilla rule for wind bursts, AUDSB31B F5)
    s.chat('gamerule mob_griefing true', 0.3)
    # like the gametest: the arrow hits a target cow; the burst at the hit pushes the cow beside it (here the "stand") and an item
    s.chat(f'execute positioned {x + 0.5} {y} {bz + 0.5} summon minecraft:cow run data merge entity @s {{NoAI:1b,Silent:1b,Tags:["n129target"]}}', 0.5)
    # beside it an armor stand (a NoAI cow does not take knockback; the stand has no AI to switch off)
    s.chat(f'summon minecraft:armor_stand {x + 1.6} {y} {bz + 0.5} {{Tags:["n129stand"]}}', 0.5)
    s.chat(f'summon minecraft:item {x - 0.6} {y} {bz + 0.5} {{Item:{{id:"minecraft:cobblestone",count:1}},PickupDelay:32767s,Tags:["n129item"]}}', 0.5)
    time.sleep(1.0)
    stand0 = _data_pos(s, 'stand-0', '@e[tag=n129stand,limit=1]')
    item0 = _data_pos(s, 'item-0', '@e[tag=n129item,limit=1]')
    s.chat(f'tp @s {x + 0.5} {y} {bz - 6.5} 0 0', 0.8)
    s.chat(f'summon {ARROW} {x + 0.5} {y + 0.7} {bz - 2.0} {{Motion:[0.0d,0.0d,1.6d],pickup:0b,damage:0.5d,item:{_arrow_nbt("breeze_rod")}}}', 0.2)
    time.sleep(0.2)
    s.shot('5-breeze-burst')
    time.sleep(1.5)
    stand1 = _data_pos(s, 'stand-1', '@e[tag=n129stand,limit=1]')
    item1 = _data_pos(s, 'item-1', '@e[tag=n129item,limit=1]')
    health = _data_num(s, 'stand-health', 'data get entity @e[tag=n129stand,limit=1] Health')

    def moved(a, b):
        return None if a is None or b is None else ((a[0] - b[0]) ** 2 + (a[2] - b[2]) ** 2) ** 0.5

    ms, mi = moved(stand0, stand1), moved(item0, item1)
    s.check('F5: the breeze rod wind burst pushes the armor stand beside the hit cow away', ms is not None and ms >= 0.3, f'{stand0} -> {stand1} ({ms})')
    s.check('F5: the breeze rod wind burst pushes the dropped item away', mi is not None and mi >= 0.3, f'{item0} -> {item1} ({mi})')
    s.check('F5: the burst does no damage - the armor stand is still there', stand1 is not None, f'health {health}')
    s.shot('6-after-burst')
    s.chat('kill @e[tag=n129stand]', 0.3)
    s.chat('kill @e[tag=n129item]', 0.3)
    s.chat(f'kill @e[type={ARROW}]', 0.3)
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- Simple Riding
SHOES = ['simpleriding:copper_horseshoe', 'simpleriding:iron_horseshoe', 'simpleriding:golden_horseshoe', 'simpleriding:diamond_horseshoe']
HORSE_TAME = 'Tame:1b,Silent:1b,PersistenceRequired:1b,equipment:{saddle:{id:"minecraft:saddle",count:1}}'  # inside the compound


def _hoof_indices(st):
    slots = st.get('slots', [])
    last = slots[-4:] if len(slots) >= 4 else []
    if len(last) == 4 and all(not sl['own'] and sl['cslot'] == i for i, sl in enumerate(last)):
        return [sl['index'] for sl in last]
    return []


def riding_panel(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    s.chat('clear @s', 0.3)
    for i, shoe in enumerate(SHOES):
        s.chat(f'item replace entity @s hotbar.{i} with {shoe}', 0.3)
    s.chat('item replace entity @s inventory.0 with simpleriding:netherite_horseshoe', 0.3)
    s.chat('item replace entity @s hotbar.4 with minecraft:cobblestone 4', 0.3)
    hx, hz = x + 0.5, z + 3.5
    s.chat(f'execute positioned {hx} {y} {hz} summon minecraft:horse run data merge entity @s ' + '{' + HORSE_TAME + ',NoAI:1b,Rotation:[90f,0f]}', 1.0)
    _say(s, f'if entity @e[type=minecraft:horse,distance=..8,nbt={{Tame:1b}}]', 'horse-tame')
    _check_said(s, 'the horse is tamed and saddled (execute summon + data merge)', ['horse-tame'])
    s.io.key('9', 0.4)  # empty hand
    if not _aim(s, (x + 0.5, y, z + 0.5), (hx, y + 1.0, hz), 'minecraft:horse', 'panel horse', key='hitEntity'):
        return
    _sneak_click(s, 'panel open')
    st = s.wait_screen('InventoryScreen', 10)
    opened = 'InventoryScreen' in st.get('screen', '') and 'Creative' not in st.get('screen', '')
    s.check('REAL Shift (held) + right click on the tamed horse opens its inventory', opened, st.get('screen'))
    if not opened:
        return
    time.sleep(0.6)
    st = s.st()
    s.check('mount inventory title is the horse name "Horse"', st.get('title') == 'Horse', st.get('title'))
    hoof = _hoof_indices(st)
    s.check('four hoof slots come last in the menu (not the player\'s, cslot 0..3)', len(hoof) == 4, len(st.get('slots', [])))
    if len(hoof) != 4:
        s.close()
        return
    s.check('hoof slots start empty', all(not _stack_at(st, i) for i in hoof), [_stack_at(st, i) for i in hoof])
    s.shot('1-panel-empty')
    _click_own(s, SHOES[0] + ' x1')
    s.click_slot(index=hoof[0])
    st, got = _wait_stack(s, hoof[0], SHOES[0] + ' x1')
    s.check('REAL click: the copper horseshoe sits on the front-left hoof', got == SHOES[0] + ' x1', got)
    for k in (1, 2, 3):
        _click_own(s, SHOES[k] + ' x1', shift=True)
        st, got = _wait_stack(s, hoof[k], SHOES[k] + ' x1')
        s.check(f'REAL shift-click moves {SHOES[k].split(":")[1]} to hoof slot {k}', got == SHOES[k] + ' x1', got)
    s.shot('2-panel-full')
    _click_own(s, 'minecraft:cobblestone x4')
    s.click_slot(index=hoof[0])
    time.sleep(0.8)
    st = s.st()
    s.check('cobblestone on a hoof slot is refused (the shoe stays, cobblestone stays on the cursor)',
            _stack_at(st, hoof[0]) == SHOES[0] + ' x1' and st.get('carried') == 'minecraft:cobblestone x4', f'{_stack_at(st, hoof[0])} / {st.get("carried")}')
    _put_back(s)
    _click_own(s, 'simpleriding:netherite_horseshoe x1')
    s.click_slot(index=hoof[0])
    st, got = _wait_stack(s, hoof[0], 'simpleriding:netherite_horseshoe x1')
    s.check('swap: the netherite shoe replaces the copper one, the copper one is on the cursor',
            got == 'simpleriding:netherite_horseshoe x1' and st.get('carried') == SHOES[0] + ' x1', f'{got} / {st.get("carried")}')
    _put_back(s)
    s.click_slot(index=hoof[3], shift=True)
    st, got = _wait_stack(s, hoof[3], '')
    s.check('REAL shift-click on hoof 3 gives the diamond shoe back to the inventory', got == '' and _own_index(st, SHOES[3] + ' x1') is not None, got)
    s.shot('3-panel-after')
    s.close()
    s.check('no missing-texture line for the removed horseshoe_panel.png in the client log', 'horseshoe_panel' not in _log(s), '')
    s.chat(f'tp @s {hx - 2.4:.2f} {y} {hz - 1.6:.2f} facing {hx} {y + 0.35 - 1.62:.2f} {hz}', 1.2)
    s.shot('4-hooves')
    s.chat('kill @e[type=minecraft:horse,distance=..20]', 0.3)
    s.chat('clear @s', 0.3)


class _Sampler(threading.Thread):
    """Polls the probe status file every 20 ms (no fresh-tick wait) and keeps (client tick, block z) per new client tick."""

    def __init__(self):
        super().__init__(daemon=True)
        self.samples = []
        self.stop = False

    def run(self):
        last = None
        while not self.stop:
            st = _SP.status()
            if st and st.get('tick') != last and st.get('pos'):
                last = st.get('tick')
                try:
                    self.samples.append((last, int(float(st['pos'].replace(',', ' ').split()[2]))))
                except (ValueError, IndexError):
                    pass
            time.sleep(0.02)


def _ride(s, x, y, z, mode):
    """One run on the track: fresh horse, REAL mount, Ctrl + W 7 s with server stalls; returns (travelled, steps back, samples)."""
    s.chat('ride @s dismount', 0.3)
    s.chat('kill @e[type=minecraft:horse,distance=..200]', 0.4)
    s.chat(f'tp @s {x + 0.5} {y} {z - 1.5} 0 0', 1.0)
    s.chat(f'execute positioned {x + 0.5} {y} {z + 0.5} summon minecraft:horse run data merge entity @s '
           '{' + HORSE_TAME + ',NoAI:1b,Rotation:[0f,0f],attributes:[{id:"minecraft:movement_speed",base:0.3375d}]}', 1.0)
    s.io.key('9', 0.4)  # NoAI until mounted: the second run's horse had wandered off the crosshair
    if not _aim(s, (x + 0.5, y, z - 1.5), (x + 0.5, y + 1.0, z + 0.5), 'minecraft:horse', f'{mode} mount', key='hitEntity'):
        return None
    _click(s, after=1.0)
    _say(s, 'on vehicle if entity @s[type=minecraft:horse]', f'{mode}-riding')
    if not _check_said(s, f'{mode}: REAL right click on the tamed, saddled horse mounts it', [f'{mode}-riding']):
        return None
    s.chat('execute on vehicle run data merge entity @s {NoAI:0b}', 0.4)  # a NoAI horse cannot be steered
    s.chat('rotate @s 0 5', 0.6)
    sampler = _Sampler()
    sampler.start()
    s.io.key_down('Control_L')
    s.io.key_down('w')
    try:
        t_end = time.time() + 7.0
        n = 0
        while time.time() < t_end:
            block = 'minecraft:deepslate' if n % 2 else 'minecraft:stone'
            # 64 x 9 x 56 = 32 256 blocks above the track (the probe world floor is near the bottom of the world): one server tick of
            # work while the client keeps sending vehicle moves
            s.chat(f'fill {x - 32} {y + 7} {z} {x + 31} {y + 15} {z + 55} {block}', 0.05)
            n += 1
            if n == 4:
                s.shot(f'ride-{mode}')
    finally:
        s.io.key_up('w')
        s.io.key_up('Control_L')
        time.sleep(0.5)
        sampler.stop = True
        sampler.join(1.0)
    zs = [zz for _, zz in sampler.samples]
    back = sum(1 for a, b in zip(zs, zs[1:]) if b < a)
    travelled = (zs[-1] - zs[0]) if zs else 0
    _mark(s, f'tick-{mode}')
    s.chat('tick query', 0.5)
    time.sleep(1.2)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK tick-{mode}')), default=-1)
    ticks = ' | '.join(ln.split(']: ', 1)[-1][:120] for ln in (lines[idx + 1: idx + 10] if idx >= 0 else [])
                       if re.search(r'(?i)average|P50|P95|P99', ln)) or 'no tick query line'
    s.chat('ride @s dismount', 0.3)
    return travelled, back, len(zs), n, ticks


def riding_lag(s):
    _prep(s)
    x, y, z = _arena(s, 40, x0=-4, z0=-4, x1=4, z1=125, height=5)
    _mark(s, 'stall-fill')
    s.chat(f'fill {x - 32} {y + 7} {z} {x + 31} {y + 15} {z + 55} minecraft:stone', 1.0)
    m = _after(s, 'stall-fill', r'(?i)filled (\d+)')
    s.check('stall fill works (32 256 blocks, loaded, within the block limit)', m is not None and int(m.group(1)) >= 32000, m.group(0) if m else 'no fill line')
    s.chat('simpleriding config reset safety.lenientMovementBudget', 0.6)
    try:
        r = _ride(s, x, y, z, 'lenient')
        if r:
            travelled, back, samples, stalls, ticks = r
            s.check('lenient (Option B, default): the horse gallops >= 40 blocks on REAL Ctrl + W under server stalls', travelled >= 40,
                    f'{travelled} blocks, {samples} samples, {stalls} stalls')
            s.check('lenient (Option B): no correction - the client position never steps back', back == 0, f'{back} steps back; {ticks}')
        s.chat('simpleriding config set safety.lenientMovementBudget false', 0.6)
        r2 = _ride(s, x, y, z, 'strict')
        if r2:
            travelled, back, samples, stalls, ticks = r2
            # a comparison, not a game check: 0 here means the probe's stalls were too short to bunch packets (run 1 then says little)
            print(f'NOTE strict comparison: {back} steps back, {travelled} blocks, {samples} samples, {stalls} stalls'
                  + (' - stalls too short to reproduce the old pull-back' if back == 0 else ''), flush=True)
    finally:
        s.chat('simpleriding config reset safety.lenientMovementBudget', 0.6)
        s.chat('ride @s dismount', 0.3)
        s.chat('kill @e[type=minecraft:horse,distance=..200]', 0.3)
    s.chat(f'fill {x - 32} {y + 7} {z} {x + 31} {y + 15} {z + 55} minecraft:air', 0.8)


SCENARIOS = {
    'n129-foundry-desk': foundry_desk,
    'n129-arrows-flight': arrows_flight,
    'n129-riding-panel': riding_panel,
    'n129-riding-lag': riding_lag,
}
