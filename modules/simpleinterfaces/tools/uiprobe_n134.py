"""N134 release polish, mods 47-49 (offen-vor-release.md section 3, "Szenen" of Simple Wanderers, Simple Challenges, Simple Stats):

Simple Wanderers  no "Szenen" line (UI "-"); the mod's only screen is the Vanilla trading screen of its trader, so one short extra scene:
  n134-wanderer-trade   a NoAI Village Wanderer (summoned 2.5 blocks ahead, no village: offers drawn for "plains"); crosshair read back
                        on simplewanderers:village_wanderer, REAL right click -> MerchantScreen (class read back), the visible offer
                        buttons are counted (>= 1; the count is a NOTE, the manual says up to ten per period), shots of the model and the
                        screen; Escape closes it.
Simple Stats      "Zähler real auslösen (Schild, Raketen, SB-Hammer) (W4C-45)."
  n134-stats-counters   survival, difficulty normal (the probe world starts in Peaceful).
                        Shield: shield in the selected hotbar slot (main hand read back), a dispenser with arrows 3 blocks ahead facing
                        the player, crosshair read back on it; the REAL right button is held (block delay 5 ticks) while a redstone
                        block behind the dispenser fires it three times -> '/simplestats show' on the shield: attacks_blocked >= 1
                        (damage_blocked as NOTE).
                        Rockets: elytra on the chest, rockets in the main hand, resistance against the landing; 30 blocks up, REAL Space
                        in the air starts gliding, two REAL right clicks use rockets; after landing the elytra is copied into the main
                        hand -> '/simplestats show': flights >= 1, rockets_used >= 1 (2 expected; distance as NOTE).
                        SB hammer: iron sledgehammer, haste; a 3x3 stone wall 2.5 blocks ahead, crosshair read back on its centre; the
                        REAL left button held until the centre breaks -> all 9 wall blocks are air and '/simplestats show' on the hammer
                        says blocks_mined = 9 (the area break counts every block).
Simple Challenges "Auswahlfenster + Startbereich-Editor (nur aus 'Welt erstellen' erreichbar)."
  n134-challenges-select  LEAVES the shared probe world, so it must be the LAST scenario of the run. REAL path: Escape -> 'Save and Quit
                        to Title' -> 'Singleplayer' -> 'Create New World' -> tab 'World' -> the world type button clicked until it shows
                        'Simple Challenges' -> the challenge row appears ('No challenge chosen', three recent buttons, 'Select
                        Challenge'). SelectChallengeScreen: first entry Vanilla Unlock (name box read back), a shipped preset cannot
                        be edited (Start area inactive); a click on row 2 selects Block by Block. Back on Vanilla Unlock, 'Duplicate'
                        -> own copy 'Vanilla Unlock 2', file in config/simplechallenges/presets/, Start area + Delete active.
                        StartAreaEditorScreen: buttons Square + / Square - / Only the spawn / Done / Cancel; 'Square +' twice and a
                        REAL left drag on the map, 'Done' -> the preset file changed; editor again, 'Only the spawn' + 'Cancel' ->
                        file unchanged. 'Delete' -> file gone, list back on Vanilla Unlock. 'Play Challenge' -> back on the World
                        tab with "Challenge: Vanilla Unlock"; 'Create New World' -> the world opens (an experimental question is
                        recorded and answered) -> '/simplechallenges status' in the chat names "Challenge Vanilla Unlock".

Sounds: none of the three mods ships own sounds - no listening page.

Patterns (uiprobe_n122-n133 lessons): difficulty normal for anything hostile; 'tp ... facing' with y - 1.62, targets <= 4 blocks,
crosshair read back before real clicks; main hand = selected hotbar slot, cleared before giving; chat commands ~1-1.5 s each.
Comparisons/assumptions are NOTE prints. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe134 --scale 2 \\
        --only n134-wanderer-trade,n134-stats-counters,n134-challenges-select
"""
import json
import re
import time
from pathlib import Path

TAG = 'N134'
REPO = Path(__file__).resolve().parents[3]
RUN = REPO / 'integration/run-fabric-263'
PRESETS = RUN / 'config/simplechallenges/presets'
FLOOR = 'minecraft:smooth_stone'


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


def _mark(s, mark):
    s.chat(f'say {TAG} MARK {mark}', 0.4)


def _log_since(s, mark):
    """client.log after the last line ending with '<TAG> MARK <mark>'."""
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {mark}')), default=-1)
    return '\n'.join(lines[idx + 1:]) if idx >= 0 else ''


def _show(s, mark):
    """'/simplestats show' on the held item: {key: value} from the chat lines "key = value" (values as printed)."""
    _mark(s, mark)
    s.chat('simplestats show', 0.4)
    time.sleep(1.5)
    text = _log_since(s, mark)
    out = {}
    for m in re.finditer(r'\b([a-z_]+) = ([\d.,]+)', text):
        out.setdefault(m.group(1), m.group(2))
    if not out:
        print(f'NOTE simplestats show ({mark}) printed: {text[-400:]!r}', flush=True)
    return out


def _num(v):
    try:
        return float(str(v).replace(',', ''))
    except (TypeError, ValueError):
        return None


def _center_pointer(s):
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    time.sleep(0.3)


def _prep(s, mode='creative'):
    s.close()
    s.io.button(1, False)
    s.io.button(3, False)
    for k in ('Shift_L', 'w', 'space'):
        s.io.key_up(k)
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect clear @s', 0.3)
    s.chat('clear @s', 0.3)
    _center_pointer(s)


def _arena(s, dx, x0=-4, z0=-4, x1=4, z1=8, height=5):
    """Moves dx blocks east, lays a smooth stone floor under fresh air and returns the feet position."""
    x, y, z = _pos(s)
    x += dx
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
    s.chat(f'fill {x + x0} {y} {z + z0} {x + x1} {y + height} {z + z1} minecraft:air', 0.6)
    s.chat(f'fill {x + x0} {y - 1} {z + z0} {x + x1} {y - 1} {z + z1} {FLOOR}', 0.6)
    s.chat('kill @e[type=minecraft:item,distance=..40]', 0.3)
    time.sleep(1.0)
    return x, y, z


def _aim(s, at, target, want, tag, tries=4, key='hitEntity'):
    """Feet at `at`, eyes onto the world point `target` (tp facing from the feet: y - 1.62); waits until the crosshair status `key`
    contains `want`. True when it does."""
    hit = None
    for _ in range(tries):
        s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', 0.8)
        for _ in range(6):
            hit = s.st().get(key)
            if hit and want in str(hit):
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, str(hit))
    return False


def _hand(s, slot, stack, tag):
    """Clears and fills hotbar slot `slot`, selects it with the REAL number key and reads the main hand back."""
    s.chat(f'item replace entity @s hotbar.{slot} with minecraft:air', 0.3)
    s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.4)
    s.io.key(str(slot + 1), 0.5)
    item = stack.split('[', 1)[0].split(' ', 1)[0]
    _say(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-mainhand')
    return _check_said(s, f'{tag}: {item} is in the main hand (selected hotbar slot {slot + 1})', [f'{tag}-mainhand'])


def _texts(s, st=None):
    """The raw labels of the visible widgets (Session.labels prefixes the class)."""
    return [w['label'] for w in s.widgets(st or s.st())]


def _wait_for(s, fragment, seconds):
    end, st = time.time() + seconds, s.st()
    while time.time() < end:
        st = s.st()
        if fragment in st.get('screen', ''):
            return st
        time.sleep(0.3)
    return st


# ---------------------------------------------------------------- Simple Wanderers
def wanderer_trade(s):
    _prep(s)
    x, y, z = _arena(s, 140)
    try:
        s.chat(f'summon simplewanderers:village_wanderer {x + 0.5} {y} {z + 3.0} {{NoAI:1b,Tags:["n134"],Rotation:[180f,0f]}}', 1.0)
        if not _aim(s, (x + 0.5, y, z + 0.5), (x + 0.5, y + 1.4, z + 3.0), 'simplewanderers:village_wanderer', 'wanderer'):
            return
        s.shot('village-wanderer-model')
        s.io.right_click()
        st = s.wait_screen('MerchantScreen', 8)
        ok = 'MerchantScreen' in st.get('screen', '')
        s.check('REAL right click on the Village Wanderer opens the trading screen (MerchantScreen)', ok, st.get('screen'))
        if not ok:
            return
        time.sleep(0.8)
        offers = [w for w in s.widgets(st) if 'TradeOffer' in w['class']]
        print(f'NOTE visible offer buttons: {len(offers)} (manual: up to ten offers per period, the screen shows 7 at a time)', flush=True)
        s.check('the wanderer has offers (>= 1 offer button)', len(offers) >= 1, [w['class'] for w in s.widgets(st)][:20])
        s.shot('merchant-screen')
        s.close()
    finally:
        s.chat('kill @e[tag=n134]', 0.3)


# ---------------------------------------------------------------- Simple Stats
def _shield(s, x, y, z):
    s.chat(f'setblock {x} {y + 1} {z + 3} minecraft:dispenser[facing=north]{{Items:[{{Slot:0b,id:"minecraft:arrow",count:16}}]}}', 0.5)
    if not _hand(s, 0, 'minecraft:shield', 'shield'):
        return
    # look ABOVE the dispenser into the open air: a right click on the dispenser opens its window instead of raising the shield
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 3.2 - 1.62:.2f} {z + 2.95}', 0.8)
    for _ in range(8):
        if not s.st().get('hitBlock'):
            break
        time.sleep(0.3)
    if s.last.get('hitBlock'):
        s.check('shield: the crosshair is off the dispenser (open air)', False, s.last.get('hitBlock'))
        return
    time.sleep(0.6)
    s.io.button(3, True)
    try:
        time.sleep(0.8)  # shield block delay: 5 ticks
        for _ in range(3):
            s.chat(f'setblock {x} {y + 1} {z + 4} minecraft:redstone_block', 0.5)
            s.chat(f'setblock {x} {y + 1} {z + 4} minecraft:air', 0.5)
        time.sleep(0.6)
        s.shot('shield-raised-arrows')
    finally:
        s.io.button(3, False)
    time.sleep(0.5)
    vals = _show(s, 'shield')
    blocked = _num(vals.get('attacks_blocked'))
    print(f'NOTE shield statistics: {vals}', flush=True)
    s.check('REAL shield block against dispenser arrows: attacks_blocked >= 1 on the shield', blocked is not None and blocked >= 1, vals)
    s.chat(f'setblock {x} {y + 1} {z + 3} minecraft:air', 0.3)
    s.chat('kill @e[type=minecraft:arrow,distance=..20]', 0.3)


def _rockets(s, x, y, z):
    s.chat('item replace entity @s armor.chest with minecraft:elytra', 0.3)
    s.chat('effect give @s minecraft:resistance 60 4 true', 0.3)
    if not _hand(s, 1, 'minecraft:firework_rocket 3', 'rocket'):
        return
    # chat commands take ~1.5 s: start high, press jump at once a few times, prove the glide before the rockets (n130 lesson)
    s.chat(f'tp @s {x + 0.5} {y + 80} {z + 0.5} 0 0', 0.2)
    time.sleep(0.4)
    gliding = False
    for _ in range(3):
        for _ in range(2):
            s.io.key('space', 0.12)
            time.sleep(0.15)
        _say(s, 'if entity @s[nbt={FallFlying:1b}]', 'gliding')
        gliding = _said(s, ['gliding'], 0.6)['gliding']
        if gliding:
            break
    print(f'NOTE glide started: {gliding}', flush=True)
    s.io.right_click()
    s.shot('gliding-first-rocket')
    time.sleep(0.5)
    s.io.right_click()
    # wait for the landing (the y coordinate stops falling)
    last, still, t0 = None, 0, time.time()
    while time.time() - t0 < 20 and still < 4:
        p = _pos(s)
        still = still + 1 if last and p[1] == last[1] else 0
        last = p
        time.sleep(0.5)
    print(f'NOTE landed at {last} after {time.time() - t0:.1f} s', flush=True)
    s.chat('item replace entity @s weapon.mainhand from entity @s armor.chest', 0.4)
    vals = _show(s, 'elytra')
    print(f'NOTE elytra statistics: {vals}', flush=True)
    flights, rockets = _num(vals.get('flights')), _num(vals.get('rockets_used'))
    s.check('REAL jump in the air starts a flight: flights >= 1 on the elytra', flights is not None and flights >= 1, vals)
    s.check('REAL right clicks with rockets while gliding: rockets_used >= 1 on the elytra (2 expected)', rockets is not None and rockets >= 1, vals)
    s.chat('item replace entity @s armor.chest with minecraft:air', 0.3)
    s.chat('effect clear @s', 0.3)


def _hammer(s, x, y, z):
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 0.8)
    s.chat(f'fill {x - 1} {y} {z + 3} {x + 1} {y + 2} {z + 3} minecraft:stone', 0.5)
    s.chat('effect give @s minecraft:haste 60 2 true', 0.3)
    if not _hand(s, 2, 'simplehammers:iron_sledgehammer', 'hammer'):
        return
    if not _aim(s, (x + 0.5, y, z + 0.5), (x + 0.5, y + 1.5, z + 2.95), 'minecraft:stone', 'hammer', key='hitBlock'):
        return
    time.sleep(0.6)
    s.io.button(1, True)
    t0, hit = time.time(), 'minecraft:stone'
    try:
        while time.time() - t0 < 10:
            hit = s.st().get('hitBlock')
            if hit != 'minecraft:stone':
                break
            time.sleep(0.2)
    finally:
        s.io.button(1, False)
    print(f'NOTE hammer: centre broke after {time.time() - t0:.1f} s (crosshair now {hit})', flush=True)
    time.sleep(1.0)
    s.shot('hammer-wall-broken')
    tags = []
    for dx in (-1, 0, 1):
        for dy in (0, 1, 2):
            t = f'air-{dx}-{dy}'
            tags.append(t)
            _say(s, f'if block {x + dx} {y + dy} {z + 3} minecraft:air', t)
    _check_said(s, 'REAL hammer break: the whole 3x3 wall is gone', tags)
    vals = _show(s, 'hammer')
    mined = _num(vals.get('blocks_mined'))
    print(f'NOTE hammer statistics: {vals}', flush=True)
    s.check('the SB hammer counts every block of the area break: blocks_mined = 9', mined == 9, vals)
    s.chat('kill @e[type=minecraft:item,distance=..20]', 0.3)


def stats_counters(s):
    _prep(s, 'survival')
    s.chat('difficulty normal', 0.3)
    try:
        x, y, z = _arena(s, 160)
        _shield(s, x, y, z)
        _rockets(s, x, y, z)
        _hammer(s, x, y, z)
    finally:
        s.io.button(1, False)
        s.io.button(3, False)
        s.chat('effect clear @s', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('difficulty peaceful', 0.3)
        s.chat('gamemode creative @s', 0.3)


# ---------------------------------------------------------------- Simple Challenges
def _preset_files():
    try:
        return {p.name: p.read_text(errors='replace') for p in PRESETS.glob('*.json')}
    except OSError:
        return {}


def _name_box(s, st=None):
    w = s.find_widget(cls='EditBox', st=st or s.st())
    return w.get('value') if w else None


def _active(s, label):
    w = s.find_widget(label, st=s.st(), contains=False)
    return None if w is None else w.get('active')


def _click_row(s, index):
    """Real click on row `index` of the preset list (rows 22 GUI px; the first row's top a few px below the list top)."""
    st = s.st()
    lst = s.find_widget(cls='PresetList', st=st)
    if not lst:
        s.check('preset list widget present', False, [w['class'] for w in s.widgets(st)])
        return None
    sc = st['scale']
    top = lst['y'] - lst['h'] / 2
    s.click_xy(lst['x'], top + (4 + 22 * index + 11) * sc, st)
    time.sleep(0.6)
    return _name_box(s)


def challenges_select(s):
    s.close()
    s.key('Escape', 1.0)
    st = _wait_for(s, 'PauseScreen', 8)
    s.check('pause screen open', 'PauseScreen' in st.get('screen', ''), st.get('screen'))
    if not s.click_widget('Save and Quit'):
        return
    st = _wait_for(s, 'TitleScreen', 60)
    s.check('back on the title screen', 'TitleScreen' in st.get('screen', ''), st.get('screen'))
    if not s.click_widget('Singleplayer'):
        return
    _wait_for(s, 'SelectWorldScreen', 20)
    if not s.click_widget('Create New World'):
        return
    st = _wait_for(s, 'CreateWorldScreen', 30)
    ok = 'CreateWorldScreen' in st.get('screen', '')
    s.check('REAL path: Create New World screen open', ok, st.get('screen'))
    if not ok:
        return
    time.sleep(1.0)
    tab = s.find_widget('World', cls='MenuTabButton', st=s.st(), contains=False)  # 26.3 class name
    if not tab:
        s.check('tab button "World" present', False, s.labels(s.st()))
        return
    s.click_xy(tab['x'], tab['y'])
    time.sleep(0.8)
    seen = []
    for _ in range(14):
        wt = next((w for w in s.widgets(s.st()) if w['class'] == 'CycleButton' and 'World Type' in w['label']), None)
        if not wt:
            break
        seen.append(wt['label'])
        if 'Simple Challenges' in wt['label']:
            break
        s.click_xy(wt['x'], wt['y'])
        time.sleep(0.7)
    ok = bool(seen) and 'Simple Challenges' in seen[-1]
    s.check('the world type button offers "Simple Challenges" (REAL clicks)', ok, ' -> '.join(seen))
    if not ok:
        return
    time.sleep(0.8)
    st = s.st()
    labels = _texts(s, st)
    s.check('challenge row: "No challenge chosen" and "Select Challenge"', any('No challenge chosen' in l for l in labels)
            and any(l == 'Select Challenge' for l in labels), labels)
    recents = [w['label'] for w in s.widgets(st) if w['class'] == 'Button' and w['w'] == 100 * st['scale']]
    print(f'NOTE recent challenge buttons: {recents} (first start: Classic Skyblock, Sky One Block, Vanilla Unlock)', flush=True)
    s.shot('world-tab-challenge-row')
    if not s.click_widget('Select Challenge'):
        return
    st = _wait_for(s, 'SelectChallengeScreen', 8)
    ok = 'SelectChallengeScreen' in st.get('screen', '')
    s.check('REAL click "Select Challenge" opens SelectChallengeScreen', ok, st.get('screen'))
    if not ok:
        return
    time.sleep(0.8)
    s.check('first entry selected: Vanilla Unlock (name box)', _name_box(s) == 'Vanilla Unlock', _name_box(s))
    s.check('shipped preset: Start area inactive, Duplicate and Play Challenge active',
            _active(s, 'Start area') is False and _active(s, 'Duplicate') is True and _active(s, 'Play Challenge') is True,
            (_active(s, 'Start area'), _active(s, 'Duplicate'), _active(s, 'Play Challenge')))
    s.shot('select-vanilla-unlock')
    name = _click_row(s, 1)
    s.check('REAL click on row 2 selects Block by Block', name == 'Block by Block', name)
    s.shot('select-block-by-block')
    name = _click_row(s, 0)
    if name != 'Vanilla Unlock':
        s.check('REAL click on row 1 selects Vanilla Unlock again', False, name)
        return
    before = _preset_files()
    if not s.click_widget('Duplicate'):
        return
    time.sleep(1.0)
    after = _preset_files()
    new = sorted(set(after) - set(before))
    s.check('Duplicate: own copy "Vanilla Unlock 2" selected and written as a preset file', _name_box(s) == 'Vanilla Unlock 2' and len(new) == 1,
            (_name_box(s), new))
    if len(new) != 1:
        return
    fname = new[0]
    try:
        s.check('own preset: Start area and Delete active', _active(s, 'Start area') is True and _active(s, 'Delete') is True,
                (_active(s, 'Start area'), _active(s, 'Delete')))
        s.shot('select-own-copy')
        if not s.click_widget('Start area'):
            return
        st = _wait_for(s, 'StartAreaEditorScreen', 8)
        ok = 'StartAreaEditorScreen' in st.get('screen', '')
        s.check('REAL click "Start area" opens StartAreaEditorScreen', ok, st.get('screen'))
        if not ok:
            return
        labels = _texts(s, st)
        s.check('editor buttons: Square + / Square - / Only the spawn / Done / Cancel',
                all(t in labels for t in ('Square +', 'Square -', 'Only the spawn', 'Done', 'Cancel')), labels)
        s.shot('editor-open')
        text0 = (PRESETS / fname).read_text(errors='replace')
        s.click_widget('Square +')
        time.sleep(0.4)
        s.click_widget('Square +')
        time.sleep(0.6)
        # a REAL left drag on the map (left part of the panel): adds a rectangle
        sc = st['scale']
        gw, gh = 1280 // sc, 720 // sc
        pw, ph = min(gw - 12, 440), min(gh - 12, 290)
        px, py = (gw - pw) // 2, (gh - ph) // 2
        mx0, my0 = (px + 8 + 20) * sc, (py + 22 + 20) * sc
        s.io.move(*s.abs(st, mx0, my0))
        time.sleep(0.3)
        s.io.button(1, True)
        time.sleep(0.2)
        for k in range(1, 9):
            s.io.move(*s.abs(st, mx0 + 6 * k * sc, my0 + 4 * k * sc))
            time.sleep(0.15)
        time.sleep(0.4)
        s.io.button(1, False)
        time.sleep(0.6)
        s.shot('editor-square2-and-drag')
        if not s.click_widget('Done'):
            return
        st = _wait_for(s, 'SelectChallengeScreen', 8)
        time.sleep(0.8)
        text1 = (PRESETS / fname).read_text(errors='replace')
        s.check('Done: back in SelectChallengeScreen and the start area in the preset file changed',
                'SelectChallengeScreen' in st.get('screen', '') and text1 != text0, (st.get('screen'), len(text0), len(text1)))
        try:
            print('NOTE start area after the edit:', json.dumps(json.loads(text1)).replace(' ', '')[:400], flush=True)
        except ValueError:
            pass
        s.shot('select-own-copy-edited')
        if s.click_widget('Start area'):
            _wait_for(s, 'StartAreaEditorScreen', 8)
            s.click_widget('Only the spawn')
            time.sleep(0.4)
            s.shot('editor-only-spawn-before-cancel')
            s.click_widget('Cancel')
            st = _wait_for(s, 'SelectChallengeScreen', 8)
            time.sleep(0.6)
            s.check('Cancel: the preset file stays unchanged', (PRESETS / fname).read_text(errors='replace') == text1, st.get('screen'))
    finally:
        if 'SelectChallengeScreen' in s.st().get('screen', '') and _name_box(s) == 'Vanilla Unlock 2':
            s.click_widget('Delete')
            time.sleep(0.8)
    s.check('Delete: the own preset file is gone, the list is back on Vanilla Unlock',
            not (PRESETS / fname).exists() and _name_box(s) == 'Vanilla Unlock', ((PRESETS / fname).exists(), _name_box(s)))
    if _name_box(s) != 'Vanilla Unlock':
        return
    if not s.click_widget('Play Challenge'):
        return
    st = _wait_for(s, 'CreateWorldScreen', 8)
    time.sleep(0.8)
    labels = _texts(s, s.st())
    s.check('Play Challenge: back on the World tab, row says "Challenge: Vanilla Unlock"',
            'CreateWorldScreen' in st.get('screen', '') and any('Challenge: Vanilla Unlock' in l for l in labels), labels)
    s.shot('world-tab-vanilla-unlock-chosen')
    if not s.click_widget('Create New World'):
        return
    seen, end, st = [], time.time() + 300, s.st()
    while time.time() < end:
        st = s.st()
        screen = st.get('screen', 'none')
        if not seen or seen[-1] != screen:
            seen.append(screen)
        if 'Confirm' in screen or 'Experimental' in screen:
            s.shot('create-question')
            if not s.click_widget('Proceed'):
                s.click_widget("know what") or s.click_widget('Yes')
            time.sleep(1.0)
        if screen == 'none' and 'pos' in st:
            break
        time.sleep(0.3)
    print('NOTE screens while creating the world: ' + ' -> '.join(seen), flush=True)
    ok = st.get('screen') == 'none' and 'pos' in st
    s.check('the challenge world opens', ok, st.get('screen'))
    if not ok:
        return
    time.sleep(3.0)
    s.shot('challenge-world')
    # the new challenge world has no cheats: 'say' (the log mark) is refused there - read the log grown since the command
    n0 = len(_log(s))
    s.chat('simplechallenges status', 0.5)
    time.sleep(2.0)
    log = _log(s)[n0:]
    s.check('/simplechallenges status: "Challenge Vanilla Unlock: ... cells free"', 'Challenge Vanilla Unlock' in log, log[-300:])


SCENARIOS = {
    'n134-wanderer-trade': wanderer_trade,
    'n134-stats-counters': stats_counters,
    'n134-challenges-select': challenges_select,
}
