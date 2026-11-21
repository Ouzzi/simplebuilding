"""N128 release polish, mods 29-31 (offen-vor-release.md section 3, "Szenen" of Simple Mobheads, Simple Trims, Simple Enchanting), word
for word:

Simple Mobheads    "Kopf-Modelle + Trage-Fähigkeiten (W4C-28)."
  n128-mobheads-models    All 97 heads of modules/simplemobheads/data/mobs.json as floor heads (rotation 0 = face south, toward the
                          camera) in rows of 10 and as wall heads (facing south) on a stone wall; every block read back on the server
                          (only misses print a say line, plus a positive control). Shots: floor field, wall, close-ups of the reworked
                          models (cod, salmon, pufferfish, tropical fish, rabbit, phantom, armadillo - IG31-22/ABN29-6/BS25-18), eight
                          heads worn by armor stands, and nine heads in the hotbar in the real inventory screen (GuiHeadFit,
                          armadillo pose IG31-10). The look of the models stays the owner's (W4C-28).
  n128-mobheads-abilities Wearing a head (armor.head) in survival, each with a control run without the head:
                          silverfish: scale attribute 0.5 (third-person shot); giant: scale 1.5 and REAL W walks up a whole block
                          (without the head the player stays below); spider: REAL W through a 6-block cobweb lane gets through (without:
                          stuck in the first blocks); cave spider: REAL left button held 2.5 s on a cobweb (crosshair read back) breaks
                          it with the bare hand (without: it stays); blaze: 3 s on a magma block cost no health (without: hurt).
Simple Trims       "Rüstungsständer normal/invers/pulsierend Tag/Nacht, JEI-Seite (W4C-48)."
  n128-trims-stands       Six armor stands (iron armor, gold sentry trim): plain, inverted stage 1/2/3, pulsating + glowing, emitting
                          (custom_data SimpleBuildingEmissionLevel 1 on all four pieces = light 12). Components read back on the chest
                          piece; the emitting stand sets an invisible light block at head height (TrimLight); shots by day, close-up of
                          the inverted stages, two night shots 1.3 s apart (pulse). Look of the stages = owner.
  n128-trims-jei          Real inventory (E), pointer on the Glowing Armor Trim template, REAL R -> JEI recipe screen (in-world upgrade
                          tab), REAL U -> uses (smithing extension); screen class read back, shots.
Simple Enchanting  "Verzauberer-Fenster (Margin)."
  n128-enchanting-margin  Dark Enchanter on Blazing Obsidian in a ring of blazewood shelves; empty-hand REAL right click (crosshair read
                          back) opens the EnchanterScreen; title = lang container.simpleenchanting.dark_enchanter, 3 table slots + 36
                          own. Margin (N62/N64, UiMargin): from the real slot positions and the screenshot, the run of box fill between
                          the outermost slots and the box's light line - inventory box left/right/top/bottom and container box left
                          (item row) - must be 3 gui px; numbers in shots/n128-enchanting-margin.json.
  Sounds: none of the three mods ships its own sound (no .ogg, no sounds.json) - no listening page.

Patterns (N122-N127): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62, crosshair read back before every real
click; main hand = selected hotbar slot, cleared before giving; chat commands ~1-1.5 s each; counts from "Test passed. Count: N".
No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe128 --scale 2 \\
        --only n128-mobheads-models,n128-mobheads-abilities,n128-trims-stands,n128-trims-jei,n128-enchanting-margin
"""
import importlib.util
import json
import re
import time
from pathlib import Path

TAG = 'N128'
REPO = Path(__file__).resolve().parents[3]
FLOOR = 'minecraft:smooth_stone'


def _load(name, rel):
    spec = importlib.util.spec_from_file_location(name, REPO / rel)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


# ---------------------------------------------------------------- helpers (uiprobe_n127 pattern)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _fpos(s):
    return [float(v) for v in s.st()['pos'].replace(',', ' ').split()]


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


def _prep(s, mode='creative', night_vision=True):
    s.close()
    s.io.button(1, False)
    s.io.button(3, False)
    s.io.key_up('Shift_L')
    s.io.key_up('w')
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect clear @s', 0.3)
    if night_vision:
        s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _arena(s, dx, x0=-3, z0=-3, x1=14, z1=24, height=8):
    """Moves dx blocks east, lays a smooth stone floor under fresh air (x0..x1, z0..z1 around the feet) and returns the feet position."""
    x, y, z = _pos(s)
    x += dx
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
    s.chat(f'fill {x + x0} {y} {z + z0} {x + x1} {y + height} {z + z1} minecraft:air', 0.6)
    s.chat(f'fill {x + x0} {y - 1} {z + z0} {x + x1} {y - 1} {z + z1} {FLOOR}', 0.6)
    s.chat('kill @e[type=!minecraft:player,distance=..60]', 0.3)
    time.sleep(1.0)
    return x, y, z


def _aim(s, at, target, want, tag, tries=4):
    """Feet at `at`, eyes onto the world point `target` (tp facing from the feet: y - 1.62); waits until the crosshair reads back block
    `want`. True when it does."""
    hit = None
    for _ in range(tries):
        s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', 0.8)
        for _ in range(6):
            hit = s.st().get('hitBlock')
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _look(s, at, target, wait=1.2):
    s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', wait)


def _score(s, objective, tag, cmd):
    """`execute store result score @s <objective> run <cmd>`; returns the stored value read back with say lines (None = not found)."""
    s.chat(f'execute store result score @s {objective} run {cmd}', 0.4)
    s.chat(f'say {TAG} SCORE {tag}', 0.3)
    s.chat(f'scoreboard players get @s {objective}', 0.4)
    time.sleep(1.4)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} SCORE {tag}')), default=-1)
    for ln in lines[idx + 1: idx + 8] if idx >= 0 else []:
        m = re.search(r' has (-?\d+) \[', ln)
        if m:
            return int(m.group(1))
    return None


def _count(s, tag, selector):
    """'execute if entity <selector>': the number from the feedback line "Test passed. Count: N" after a marker line (None = no line)."""
    s.chat(f'say {TAG} COUNT {tag}', 0.3)
    s.chat(f'execute if entity {selector}', 0.35)
    time.sleep(1.5)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} COUNT {tag}')), default=-1)
    for ln in lines[idx + 1: idx + 8] if idx >= 0 else []:
        m = re.search(r'Test passed[.,] [Cc]ount: (\d+)', ln)
        if m:
            return int(m.group(1))
        if 'Test failed' in ln:
            return 0
    return None


def _walk(s, seconds):
    """REAL W held for `seconds`; returns (start, end) feet positions (floats)."""
    start = _fpos(s)
    s.io.key_down('w')
    try:
        time.sleep(seconds)
    finally:
        s.io.key_up('w')
    time.sleep(0.6)
    return start, _fpos(s)


def _heads():
    data = json.loads((REPO / 'modules/simplemobheads/data/mobs.json').read_text(encoding='utf-8'))['heads']
    out = []
    for h in data:
        item = h.get('item', h['id'] + '_head')
        wall = item[:-len('_skull')] + '_wall_skull' if item.endswith('_skull') else item[:-len('_head')] + '_wall_head'
        out.append((h['id'], 'simplemobheads:' + item, 'simplemobheads:' + wall))
    return out


REWORKED = ('cod', 'salmon', 'pufferfish', 'tropical_fish', 'rabbit', 'phantom', 'armadillo')


# ---------------------------------------------------------------- Simple Mobheads
def mobheads_models(s):
    _prep(s)
    heads = _heads()
    s.check('mobs.json lists the 97 heads', len(heads) == 97, len(heads))
    x, y, z = _arena(s, 60, x0=-4, z0=-4, x1=56, z1=40, height=10)  # 61 x 11 x 45 < fill limit 32768
    s.chat('gamerule random_tick_speed 0', 0.3)
    try:
        # ---- floor heads: rows of 10, 3 blocks apart (giant heads are 1.5 blocks), face south toward the camera
        fz0 = z + 6
        cells = {}
        for i, (hid, item, _wall) in enumerate(heads):
            hx, hz = x + 3 * (i % 10), fz0 + 3 * (i // 10)
            cells[hid] = (hx, hz)
            s.chat(f'setblock {hx} {y} {hz} {item}[rotation=0]', 0.3)
        time.sleep(1.0)
        s.chat(f'say {TAG} floor-check-start', 0.3)
        for hid, item, _wall in heads:
            hx, hz = cells[hid]
            _say(s, f'unless block {hx} {y} {hz} {item}', f'floor-missing-{hid}')
        first = heads[0]
        _say(s, f'if block {cells[first[0]][0]} {y} {cells[first[0]][1]} {first[1]}', 'floor-control')
        got = _said(s, ['floor-control'] + [f'floor-missing-{h[0]}' for h in heads])
        miss = [t[len('floor-missing-'):] for t, ok in got.items() if ok and t.startswith('floor-missing-')]
        s.check('all 97 floor heads stand where they were set (server read back)', got['floor-control'] and not miss,
                f'control {got["floor-control"]}, missing {miss[:12]}')
        # overview from the south side (heads face south = +z): camera behind the last row, looking north over the field
        _look(s, (x + 13.5, y + 9, fz0 + 3 * 10 + 6), (x + 13.5, y, fz0 + 12))
        s.shot('1-floor-overview')
        for hid in REWORKED:
            if hid not in cells:
                s.check(f'reworked head {hid} is in mobs.json', False, 'missing')
                continue
            hx, hz = cells[hid]
            _look(s, (hx + 0.5, y, hz + 2.3), (hx + 0.5, y + 0.25, hz + 0.5), 1.0)
            s.shot(f'2-closeup-{hid}')
        # ---- wall heads: stone wall north of the field, 4 rows x 25 columns, 2 blocks apart, facing south
        wz = z - 3
        s.chat(f'fill {x - 1} {y} {wz} {x + 50} {y + 8} {wz} minecraft:stone', 0.6)
        wcells = {}
        for i, (hid, _item, wall) in enumerate(heads):
            hx, hy = x + 2 * (i % 25), y + 1 + 2 * (i // 25)
            wcells[hid] = (hx, hy)
            s.chat(f'setblock {hx} {hy} {wz + 1} {wall}[facing=south]', 0.3)
        time.sleep(1.0)
        for hid, _item, wall in heads:
            hx, hy = wcells[hid]
            _say(s, f'unless block {hx} {hy} {wz + 1} {wall}', f'wall-missing-{hid}')
        _say(s, f'if block {wcells[first[0]][0]} {wcells[first[0]][1]} {wz + 1} {first[2]}', 'wall-control')
        got = _said(s, ['wall-control'] + [f'wall-missing-{h[0]}' for h in heads])
        miss = [t[len('wall-missing-'):] for t, ok in got.items() if ok and t.startswith('wall-missing-')]
        s.check('all 97 wall heads hang on the wall (server read back)', got['wall-control'] and not miss,
                f'control {got["wall-control"]}, missing {miss[:12]}')
        s.chat(f'fill {x - 3} {y} {fz0 - 2} {x + 50} {y + 3} {fz0 + 40} minecraft:air', 0.6)  # field away for the wall view
        _look(s, (x + 12.5, y + 2, wz + 15), (x + 12.5, y + 4, wz + 1))
        s.shot('3-wall-left')
        _look(s, (x + 36.5, y + 2, wz + 15), (x + 36.5, y + 4, wz + 1))
        s.shot('4-wall-right')
        # ---- worn by armor stands (facing south, toward the camera)
        worn = list(REWORKED) + ['giant']
        for i, hid in enumerate(worn):
            item = next((h[1] for h in heads if h[0] == hid), None)
            if not item:
                continue
            s.chat(f'summon minecraft:armor_stand {x + 0.5 + 2 * i} {y} {z + 8.5} {{Rotation:[0f,0f],NoBasePlate:1b,ShowArms:1b,'
                   f'Tags:["n128head"],equipment:{{head:{{id:"{item}",count:1}}}}}}', 0.4)
        time.sleep(1.0)
        n = _count(s, 'stands', '@e[type=minecraft:armor_stand,tag=n128head,nbt={equipment:{head:{}}}]')
        s.check('eight armor stands wear the reworked heads and the giant head', n == 8, n)
        _look(s, (x + 7.5, y, z + 15.5), (x + 7.5, y + 1.6, z + 8.5))
        s.shot('5-worn-by-stands')
        # ---- heads in the real inventory screen (survival: hotbar visible in the InventoryScreen)
        s.chat('kill @e[type=minecraft:armor_stand,tag=n128head]', 0.3)
        s.chat('gamemode survival @s', 0.4)
        s.chat('clear @s', 0.3)
        for i, hid in enumerate(list(REWORKED) + ['giant', 'blaze']):
            item = next((h[1] for h in heads if h[0] == hid), None)
            if item:
                s.chat(f'item replace entity @s hotbar.{i} with {item}', 0.3)
        st = s.key('e', 1.2)
        st = s.wait_screen('InventoryScreen', 6)
        ok = 'InventoryScreen' in st.get('screen', '')
        s.check('REAL E opens the inventory screen', ok, st.get('screen'))
        if ok:
            stacks = [x_['stack'] for x_ in st.get('slots', []) if x_['own'] and x_['cslot'] < 9]
            heads_in = sum(1 for t in stacks if (t or '').startswith('simplemobheads:'))
            s.check('nine heads sit in the hotbar row of the inventory screen', heads_in == 9, stacks)
            st0 = s.st()
            s.io.move(st0['windowX'] + 640, st0['windowY'] + 40)
            time.sleep(0.6)
            s.shot('6-inventory')
            arm = s.slot_xy(st, cslot=REWORKED.index('armadillo'), own=True)
            if arm:
                s.io.move(*s.abs(st, arm['x'] + 3, arm['y'] + 3))
                time.sleep(0.3)
                s.io.move(*s.abs(st, arm['x'], arm['y']))
                time.sleep(0.9)
                s.shot('7-inventory-armadillo-tooltip')
        s.close()
    finally:
        s.chat('gamerule random_tick_speed 3', 0.3)
        s.chat('kill @e[type=minecraft:armor_stand,tag=n128head]', 0.3)
        s.chat('gamemode creative @s', 0.3)
        s.chat('clear @s', 0.3)


def _wear(s, item):
    s.chat('item replace entity @s armor.head with ' + (item or 'minecraft:air'), 0.6)


def _heal(s):
    s.chat('effect give @s minecraft:instant_health 1 10 true', 0.6)


def mobheads_abilities(s):
    _prep(s, 'survival')
    s.chat('scoreboard objectives add n128 dummy', 0.4)
    s.chat('scoreboard objectives add n128dmg minecraft.custom:minecraft.damage_taken', 0.4)
    x, y, z = _arena(s, 60, x0=-6, z0=-3, x1=14, z1=24)
    try:
        # ---- silverfish: half size
        _wear(s, 'simplemobheads:silverfish_head')
        time.sleep(0.8)
        v = _score(s, 'n128', 'silverfish', 'attribute @s minecraft:scale get 100')
        s.check('silverfish head: the wearer shrinks to half size (scale attribute 0.5)', v == 50, v)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 0.8)
        s.key('F5', 0.5)
        s.key('F5', 1.0)  # third person, front
        s.shot('1-silverfish-third-person')
        s.key('F5', 0.6)
        _wear(s, None)
        time.sleep(0.6)
        v = _score(s, 'n128', 'plain', 'attribute @s minecraft:scale get 100')
        s.check('head off: scale back to 1.0', v == 100, v)

        # ---- giant: 1.5x and steps up whole blocks (REAL W against a 1-block step, control without the head first)
        sx = x + 4
        s.chat(f'fill {sx} {y} {z + 3} {sx} {y} {z + 8} minecraft:stone', 0.4)
        for label, item, want_up in (('without head', None, False), ('giant head', 'simplemobheads:giant_head', True)):
            _wear(s, item)
            s.chat(f'tp @s {sx + 0.5} {y} {z + 1.5} 0 0', 1.2)
            if item:
                v = _score(s, 'n128', 'giant', 'attribute @s minecraft:scale get 100')
                s.check('giant head: the wearer grows to 1.5x (scale attribute)', v == 150, v)
            start, end = _walk(s, 1.6)
            up = end[1] >= y + 0.9
            s.check(f'{label}: REAL W {"walks up" if want_up else "does not walk up"} the 1-block step',
                    up == want_up, f'{start} -> {end}')
            if item:
                s.key('F5', 0.5)
                s.key('F5', 1.0)
                s.shot('2-giant-third-person')
                s.key('F5', 0.6)
        _wear(s, None)

        # ---- spider: cobwebs do not slow (REAL W through a 6-block lane; control first)
        cx = x - 3
        for label, item, through in (('without head', None, False), ('spider head', 'simplemobheads:spider_head', True)):
            s.chat(f'fill {cx} {y} {z + 2} {cx} {y + 1} {z + 7} minecraft:cobweb', 0.4)
            _wear(s, item)
            s.chat(f'tp @s {cx + 0.5} {y} {z + 0.5} 0 0', 1.2)
            start, end = _walk(s, 2.4)
            past = end[2] >= z + 8
            s.check(f'{label}: REAL W {"gets through" if through else "is held in"} the 6-block cobweb lane',
                    past == through, f'{start} -> {end}')
            if item:
                s.shot('3-spider-after-lane')
            s.chat(f'fill {cx} {y} {z + 1} {cx} {y + 1} {z + 12} minecraft:air', 0.4)
        _wear(s, None)

        # ---- cave spider: bare hands cut cobwebs like a sword (REAL left button held 2.5 s, crosshair read back; control first)
        wx, wz = x + 8, z + 3
        s.chat('clear @s', 0.3)
        s.key('1', 0.3)
        for label, item, broken in (('without head', None, False), ('cave spider head', 'simplemobheads:cave_spider_head', True)):
            s.chat(f'setblock {wx} {y} {wz} minecraft:cobweb', 0.4)
            _wear(s, item)
            if not _aim(s, (wx + 0.5, y, wz - 1.5), (wx + 0.5, y + 0.5, wz + 0.5), 'minecraft:cobweb', f'cobweb {label}'):
                continue
            s.io.button(1, True)
            try:
                time.sleep(2.5)
            finally:
                s.io.button(1, False)
            time.sleep(0.5)
            tag = 'web-gone-head' if item else 'web-there-plain'
            _say(s, f'{"unless" if broken else "if"} block {wx} {y} {wz} minecraft:cobweb', tag)
            _check_said(s, f'{label}: REAL left button 2.5 s with the bare hand {"breaks" if broken else "does not break"} the cobweb', [tag])
            s.chat(f'setblock {wx} {y} {wz} minecraft:air', 0.3)
        _wear(s, None)

        # ---- blaze: magma blocks do not burn (3 s standing, not sneaking; control first). Damage read from the statistic
        # minecraft.custom:damage_taken (natural regeneration would hide it in the health value)
        mx, mz = x + 10, z + 8
        s.chat(f'setblock {mx} {y - 1} {mz} minecraft:magma_block', 0.4)
        for label, item, unhurt in (('without head', None, False), ('blaze head', 'simplemobheads:blaze_head', True)):
            _wear(s, item)
            s.chat(f'tp @s {mx + 0.5} {y} {mz - 2.5} 0 0', 0.4)
            _heal(s)
            time.sleep(0.8)
            before = _score(s, 'n128', f'dmg-before-{label}', 'scoreboard players get @s n128dmg')
            s.chat(f'tp @s {mx + 0.5} {y} {mz + 0.5} 0 0', 0.3)
            time.sleep(3.0)
            s.chat(f'tp @s {mx + 0.5} {y} {mz - 2.5} 0 0', 0.3)
            after = _score(s, 'n128', f'dmg-after-{label}', 'scoreboard players get @s n128dmg')
            hurt = before is not None and after is not None and after > before
            s.check(f'{label}: 3 s on a magma block {"cost no health" if unhurt else "hurt"} (damage_taken statistic)',
                    before is not None and after is not None and hurt != unhurt, f'{before} -> {after}')
        s.chat(f'setblock {mx} {y - 1} {mz} {FLOOR}', 0.3)
    finally:
        s.io.key_up('w')
        s.io.button(1, False)
        _wear(s, None)
        s.chat('scoreboard objectives remove n128', 0.3)
        s.chat('scoreboard objectives remove n128dmg', 0.3)
        s.chat('gamemode creative @s', 0.3)
        s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- Simple Trims
def trims_stands(s):
    t = _load('uiprobe_simpletrims_n128', 'modules/simpletrims/tools/uiprobe_simpletrims.py')
    _prep(s, night_vision=False)
    x, y, z = _arena(s, 60, x0=-8, z0=-3, x1=10, z1=10)
    stands = list(t.STANDS) + [('emitting', 5.4, ',"minecraft:custom_data":{SimpleBuildingEmissionLevel:1}')]
    s.chat('kill @e[type=minecraft:armor_stand,tag=uiptrim]', 0.3)
    s.key('F1', 0.5)
    try:
        for name, dx, extra in stands:
            s.chat(t._stand_command(x, y, z, name, dx, extra), 0.4)
        time.sleep(1.0)
        for name, _dx, _extra in stands[1:]:
            comp = {'pulsating': '"simpletrims:pulsating"', 'emitting': '"minecraft:custom_data".SimpleBuildingEmissionLevel'}.get(
                name, '"simpletrims:inverted_trim"')
            _say(s, f'if data entity @e[type=minecraft:armor_stand,tag=uipt_{name},limit=1] equipment.chest.components.{comp}', f'comp-{name}')
        for stage in (1, 2, 3):
            _say(s, f'if data entity @e[type=minecraft:armor_stand,tag=uipt_inverted{stage},limit=1] '
                    f'equipment.chest.components{{"simpletrims:inverted_trim":{stage}}}', f'stage-{stage}')
        _check_said(s, 'every stand keeps its component on the chest piece (inverted stage 1/2/3, pulsating, emitting)',
                    [f'comp-{n}' for n, _d, _e in stands[1:]] + [f'stage-{i}' for i in (1, 2, 3)])
        time.sleep(1.5)  # resting carriers place their light every 10 ticks
        ex = x + 0.5 + 5.4
        for dy in (1, 2):
            _say(s, f'positioned {ex:.1f} {y + dy} {z + 4.5} if block ~ ~ ~ minecraft:light', f'light-{dy}')
        got = _said(s, ['light-1', 'light-2'])
        s.check('the emitting stand sets an invisible light block at head height (TrimLight)', any(got.values()), got)
        me = (x + 0.9, y, z + 0.5)
        _look(s, me, (x + 0.9, y + 1.0, z + 4.5))
        s.shot('1-day')
        _look(s, (x + 0.5, y, z + 1.8), (x + 0.5, y + 1.1, z + 4.5), 0.9)
        s.shot('2-inverted-closeup')
        _look(s, me, (x + 0.9, y + 1.0, z + 4.5), 0.9)
        s.chat('time set midnight', 1.5)
        time.sleep(1.0)
        s.shot('3-night')
        time.sleep(1.3)
        s.shot('4-night-later')
    finally:
        s.key('F1', 0.5)
        s.chat('time set noon', 0.3)
        s.chat('kill @e[type=minecraft:armor_stand,tag=uiptrim]', 0.3)


def trims_jei(s):
    _prep(s, 'survival')
    s.chat('item replace entity @s hotbar.0 with simpletrims:glowing_trim_template', 0.4)
    s.chat('item replace entity @s hotbar.1 with minecraft:iron_chestplate', 0.4)
    for key, what in (('r', 'recipes'), ('u', 'uses')):
        s.close()
        s.key('e', 1.2)
        st = s.wait_screen('InventoryScreen', 6)
        if 'InventoryScreen' not in st.get('screen', ''):
            s.check(f'{what}: REAL E opens the inventory', False, st.get('screen'))
            continue
        slot = s.slot_xy(st, cslot=0, own=True)
        if not slot or not (slot.get('stack') or '').startswith('simpletrims:glowing_trim_template'):
            s.check(f'{what}: the template is in hotbar slot 1', False, slot)
            continue
        s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
        time.sleep(0.3)
        s.io.move(*s.abs(st, slot['x'], slot['y']))
        time.sleep(0.8)
        s.key(key, 1.5)
        st = s.wait_screen('Recipes', 6)
        ok = 'Recipes' in st.get('screen', '')
        s.check(f'REAL {key.upper()} on the Glowing Armor Trim template opens the JEI {what} screen', ok, st.get('screen'))
        if ok:
            s.io.move(st['windowX'] + 1270, st['windowY'] + 710)
            time.sleep(0.8)
            s.shot(f'jei-{what}')
    s.close()
    s.chat('clear @s', 0.3)
    s.chat('gamemode creative @s', 0.3)


# ---------------------------------------------------------------- Simple Enchanting
def _lang(module, locale):
    p = REPO / f'modules/{module}/shared/resources/assets/{module}/lang/{locale}.json'
    return json.loads(p.read_text(encoding='utf-8')) if p.exists() else {}


def _runs(img, ratio, sc, gx, gy, dx, dy, limit=12):
    """Pixels of one colour from gui pixel (gx, gy) stepping (dx, dy), and whether a different colour ends the run."""
    def px(ax, ay):
        return img.getpixel((int((ax + 0.5) * sc * ratio), int((ay + 0.5) * sc * ratio)))[:3]
    first = px(gx, gy)
    n = 0
    while n < limit and px(gx + dx * n, gy + dy * n) == first:
        n += 1
    return n


def enchanting_margin(s):
    _prep(s)
    x, y, z = _arena(s, 60, x0=-4, z0=-4, x1=10, z1=10)
    tx, tz = x + 3, z + 4
    s.chat(f'fill {tx - 2} {y - 1} {tz - 2} {tx + 2} {y - 1} {tz + 2} simpleenchanting:blazing_obsidian', 0.5)
    s.chat(f'fill {tx - 2} {y} {tz - 2} {tx + 2} {y} {tz + 2} simplelib:crimson_blazewood_bookshelf', 0.5)
    s.chat(f'fill {tx - 1} {y} {tz - 1} {tx + 1} {y} {tz + 1} minecraft:air', 0.5)
    s.chat(f'setblock {tx} {y} {tz} simpleenchanting:dark_enchanter', 0.6)
    s.chat(f'setblock {tx} {y} {tz - 2} minecraft:air', 0.4)  # a gap in the ring to stand in
    s.key('9', 0.4)
    if not _aim(s, (tx + 0.5, y, tz - 1.5), (tx + 0.5, y + 0.7, tz + 0.5), 'simpleenchanting:dark_enchanter', 'enchanter'):
        return
    s.io.button(3, True)
    time.sleep(0.12)
    s.io.button(3, False)
    st = s.wait_screen('EnchanterScreen', 8)
    ok = 'EnchanterScreen' in st.get('screen', '')
    s.check('empty-hand REAL right click opens the EnchanterScreen', ok, st.get('screen'))
    if not ok:
        return
    titles = {_lang('simpleenchanting', loc).get('container.simpleenchanting.dark_enchanter') for loc in ('en_us', 'de_de')}
    s.check('window title = lang container.simpleenchanting.dark_enchanter', st.get('title') in titles, (st.get('title'), titles))
    table = [x_ for x_ in st.get('slots', []) if not x_['own']]
    own = [x_ for x_ in st.get('slots', []) if x_['own']]
    s.check('3 table slots (item, lapis, blaze powder) and 36 own slots', len(table) == 3 and len(own) == 36, f'{len(table)} + {len(own)}')
    st0 = s.st()
    s.io.move(st0['windowX'] + 1270, st0['windowY'] + 710)
    time.sleep(0.8)
    s.shot('margin')
    shot = s.out / 'shots' / f"{s.scenario.replace('/', '-')}-margin.png"
    measured = {}
    try:
        from PIL import Image
        img = Image.open(shot).convert('RGB')
    except Exception as e:  # noqa: BLE001
        s.check('margin screenshot readable (PIL)', False, repr(e))
        return
    sc = st.get('scale') or 2
    ratio = img.width / 1280.0
    items = [(round(x_['x'] / sc) - 8, round(x_['y'] / sc) - 8) for x_ in own]
    px0, px1 = min(a for a, _ in items), max(a for a, _ in items)
    py0, py1 = min(b for _, b in items), max(b for _, b in items)
    row = py1 + 8  # hotbar row
    col = px0 + 8
    measured['inventory_left'] = _runs(img, ratio, sc, px0 - 1, row, -1, 0)
    measured['inventory_right'] = _runs(img, ratio, sc, px1 + 17, row, 1, 0)
    measured['inventory_top'] = _runs(img, ratio, sc, col, py0 - 1, 0, -1)
    measured['inventory_bottom'] = _runs(img, ratio, sc, col, py1 + 17, 0, 1)
    item_slot = next((x_ for x_ in table if x_['index'] == 0), None)
    if item_slot:
        measured['container_left_item_row'] = _runs(img, ratio, sc, px0 - 1, round(item_slot['y'] / sc), -1, 0)
    measured.update({'scale': sc, 'image': [img.width, img.height], 'own_items': [px0, py0, px1, py1]})
    dest = s.out / 'shots'
    dest.mkdir(parents=True, exist_ok=True)
    (dest / 'n128-enchanting-margin.json').write_text(json.dumps({'measured': measured, 'status': st}, indent=1))
    sides = [k for k in measured if k.startswith(('inventory_', 'container_'))]
    bad = {k: measured[k] for k in sides if measured[k] != 3}
    s.check('3 px box fill between the outermost slots and the light line (inventory box 4 sides, container box left)', not bad,
            ', '.join(f'{k} {measured[k]}' for k in sides))
    s.close()


SCENARIOS = {
    'n128-mobheads-models': mobheads_models,
    'n128-mobheads-abilities': mobheads_abilities,
    'n128-trims-stands': trims_stands,
    'n128-trims-jei': trims_jei,
    'n128-enchanting-margin': enchanting_margin,
}
