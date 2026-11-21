"""N133 release polish, mods 44-46 (offen-vor-release.md section 3, "Szenen" of Simple Resonance, Simple End Dragon Fight, Simple Respawn):

Simple Resonance  "Inventar mit Resonanz-Anzeige (Mixin, Margin nicht geprüft)."
  n133-resonance-inventory  Survival with some experience; REAL E opens the survival inventory (screen class read back). The 19x19 button
                      (ResonanceLayout.INVENTORY_X/Y = 76/34 from leftPos/topPos; leftPos/topPos from the crafting result slot, menu
                      index 0, centre 162/36) is measured against every slot of the screen (18x18 slot frame around each slot centre):
                      no overlap, the smallest gap is printed, and it lies inside the 176x166 inventory. The margin rule
                      (ui-container-margin) is for container borders - the button is not a container, so the gap check replaces it.
                      Shots: plain, pointer on the button (blue frame + "Resonance: x (max y)"), REAL Shift held (every factor line);
                      pixels of the button plain != hovered. Then '/simpleresonance': "Your resonance value" and the danger level of
                      the land in the chat (client.log). Heart tier before/after much experience: NOTE only (the tier follows the XP
                      gathered since the last death, an assumption about how 'xp add' counts).
Simple End Dragon Fight  "Mehrspieler-Kampf 2-4 Spieler (Besitzer, W4C-17)."
  n133-dragon-solo    A probe client is ONE player: the 2-4 player fight (one health bar per player, faster dragon, bigger waves) stays
                      with the owner (W4C-17) and the gametests scaleKeepsShare / speedFactor / halfAndWaves / entryQueue. What one player
                      can show, kept short: difficulty normal (the probe world starts in Peaceful), the End via 'execute in
                      minecraft:the_end', a small obsidian/barrier arena 17 blocks from the portal; the main dragon appears (count read
                      back); creative does not count (countCreative false) - switching to survival joins the fight: shots of the entry
                      animation (portal ring, breath arc, growl ring); max health read back = 200 (solo = Vanilla); the dragon put
                      below half health -> a Dragonmite wave on the ring around the portal (count >= 1); a NoAI display mite (summoned,
                      unbound) 2 blocks in front with the crosshair read back on it: model shot; '/kill' of the dragon -> every bound
                      mite dissolves (count 0, no Dragon Chitin dropped), the unbound display mite stays.
Simple Respawn    "Erledigt (W4C respawn-3)." -> the Downed screen buttons are in the older scenario 'simplerespawn'. Here the parts
                      only the real game shows (task note: death screens and respawn are central):
  n133-respawn-bleedout  Survival, difficulty normal; onlyWithOtherPlayers off and bleedOutSeconds 15 for the scene (reset afterwards).
                      Sword in hotbar slot 4 (selected with the REAL key, main hand read back), helmet, diamonds in the main inventory.
                      '/damage @s 100 minecraft:generic' (lethal, not bypassing invulnerability) -> DownedScreen, tag
                      simplerespawn_downed, health 1, chat "is down and needs help!"; REAL click Spectate -> HUD "Downed - N s", shot
                      in third person (crawling pose); bleed-out -> Vanilla death screen after ~15 s (time measured); REAL click
                      Respawn -> Simple Tweaks T27: sword and helmet kept, diamonds dropped (scattered drops gathered and counted on
                      the death spot); the downed tag is gone; the selected slot after the respawn is printed (NOTE).
                      Then onlyWithOtherPlayers back to its default (on): alone, the same lethal damage is a plain Vanilla death - the
                      DownedScreen never shows. Reviving needs a SECOND player: gametests reviveGivesThreeHeartsNoFood / reviveBreaksOff.

Sounds: none of the three mods ships own sounds (Dragonmite/animation/downed use Vanilla sounds) - no listening page.

Patterns (uiprobe_n122-n131 lessons): difficulty normal for anything hostile; counts from "Test passed. Count: N"; 'tp ... facing' with
y - 1.62, targets <= 4 blocks, crosshair read back before real clicks; main hand = selected hotbar slot; Shift held >= 0.8 s; chat
commands ~1-1.5 s each; Simple Tweaks T27 keeps hotbar/armor/offhand, death drops scatter (gathered with execute as ... run tp).
Comparisons/assumptions are NOTE prints. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe133 --scale 2 \\
        --only n133-resonance-inventory,n133-dragon-solo,n133-respawn-bleedout
"""
import re
import time
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # pixel checks are then skipped with a note
    Image = None

TAG = 'N133'
REPO = Path(__file__).resolve().parents[3]
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


def _log_since(s, mark):
    """client.log after the last line ending with '<TAG> MARK <mark>'."""
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {mark}')), default=-1)
    return '\n'.join(lines[idx + 1:]) if idx >= 0 else ''


def _mark(s, mark):
    s.chat(f'say {TAG} MARK {mark}', 0.4)


def _center_pointer(s):
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    time.sleep(0.3)


def _prep(s, mode='creative'):
    s.close()
    s.io.button(1, False)
    s.io.button(3, False)
    for k in ('Shift_L', 'w'):
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
        s.chat(f'{at[3] if len(at) > 3 else ""}tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - 1.62:.3f} {target[2]:.3f}', 0.8)
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


def _third_person(s):
    s.key('F5', 0.5)

    def back():
        s.key('F5', 0.5)
        s.key('F5', 0.5)
    return back


def _crop_mean(s, name, box):
    if Image is None:
        return None
    try:
        img = Image.open(s.out / 'shots' / f"{s.scenario.replace('/', '-')}-{name}.png").convert('RGB')
    except OSError:
        return None
    px = list(img.crop(box).getdata())
    return tuple(sum(p[i] for p in px) / max(1, len(px)) for i in range(3))


def _score(s, objective='n133'):
    s.chat(f'scoreboard objectives remove {objective}', 0.3)
    s.chat(f'scoreboard objectives add {objective} dummy', 0.4)


# ---------------------------------------------------------------- Simple Resonance
BUTTON_X, BUTTON_Y, BUTTON = 76, 34, 19


def resonance_inventory(s):
    _prep(s, 'survival')
    s.chat('difficulty normal', 0.3)
    s.chat('xp add @s 300 points', 0.5)
    time.sleep(1.5)  # the server sends simpleresonance:stats once a second
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    ok = st.get('screen', '').endswith('.InventoryScreen')
    s.check('REAL E opens the survival inventory (InventoryScreen)', ok, st.get('screen'))
    if not ok:
        return
    sc = st['scale']
    slots = st.get('slots', [])
    result = next((x for x in slots if x['index'] == 0), None)
    s.check('crafting result slot found (menu index 0)', result is not None, len(slots))
    if not result:
        return
    left, top = result['x'] / sc - 162, result['y'] / sc - 36
    bx0, by0 = left + BUTTON_X, top + BUTTON_Y
    bx1, by1 = bx0 + BUTTON, by0 + BUTTON
    overlaps, gaps = [], []
    for x in slots:
        cx, cy = x['x'] / sc, x['y'] / sc
        fx0, fy0, fx1, fy1 = cx - 9, cy - 9, cx + 9, cy + 9  # 18x18 slot frame
        if fx0 < bx1 and bx0 < fx1 and fy0 < by1 and by0 < fy1:
            overlaps.append(x['index'])
        dx = max(fx0 - bx1, bx0 - fx1, 0)
        dy = max(fy0 - by1, by0 - fy1, 0)
        gaps.append((max(dx, dy), x['index']))
    gaps.sort()
    s.check('the resonance button overlaps no slot frame of the inventory', not overlaps, f'overlapping slot indices {overlaps}')
    s.check('the button lies inside the 176x166 inventory', 0 <= BUTTON_X and BUTTON_X + BUTTON <= 176 and 0 <= BUTTON_Y and BUTTON_Y + BUTTON <= 166,
            (BUTTON_X, BUTTON_Y))
    print(f'NOTE resonance button {BUTTON}x{BUTTON} at {BUTTON_X}/{BUTTON_Y}: nearest slot frames (gap GUI px, menu index) {gaps[:4]}', flush=True)
    cx, cy = (bx0 + BUTTON / 2) * sc, (by0 + BUTTON / 2) * sc
    s.io.move(*s.abs(st, cx - 150, cy + 150))
    time.sleep(0.8)
    s.shot('inventory-plain')
    s.io.move(*s.abs(st, cx + 3, cy + 3))
    time.sleep(0.3)
    s.io.move(*s.abs(st, cx, cy))
    time.sleep(0.9)
    s.shot('button-hover-value')
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.9)
        s.shot('button-hover-shift-all-factors')
    finally:
        s.io.key_up('Shift_L')
    half = 9 * sc
    box = (int(cx - half + 2), int(cy - half + 2), int(cx + half - 2), int(cy + half - 2))
    plain, hovered = _crop_mean(s, 'inventory-plain', box), _crop_mean(s, 'button-hover-value', box)
    if plain is None or hovered is None:
        print('NOTE button pixel check skipped (PIL or shots missing)', plain, hovered, flush=True)
    else:
        s.check('the button is drawn and lights up under the pointer (pixels differ)', max(abs(a - c) for a, c in zip(plain, hovered)) > 4,
                (plain, hovered))
    heart = (int(cx - 4 * sc), int(cy - 4 * sc), int(cx + 4 * sc), int(cy + 4 * sc))
    before = _crop_mean(s, 'inventory-plain', heart)
    s.close()
    # NOTE only: much more experience - does the heart tier change?
    s.chat('xp add @s 20000 points', 0.5)
    time.sleep(2.0)
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    s.io.move(*s.abs(st, cx - 150, cy + 150))
    time.sleep(0.8)
    s.shot('inventory-after-much-xp')
    after = _crop_mean(s, 'inventory-after-much-xp', heart)
    print(f'NOTE heart pixels (mean RGB) at 300 XP {before} / after +20000 XP {after}', flush=True)
    s.close()
    _mark(s, 'resonance-cmd')
    s.chat('simpleresonance', 1.5)
    time.sleep(1.0)
    log = _log_since(s, 'resonance-cmd')
    s.check('/simpleresonance shows the player value in the chat', 'Your resonance value' in log or 'Resonanzwert' in log, log[-300:])
    s.check('/simpleresonance shows the danger level of the land', 'Danger level' in log or 'Gefahrenstufe' in log, log[-300:])
    s.key('t', 0.6)
    s.shot('chat-player-value')
    s.close()
    s.chat('difficulty peaceful', 0.3)


# ---------------------------------------------------------------- Simple End Dragon Fight
def dragon_solo(s):
    _prep(s, 'creative')
    home = _pos(s)
    s.chat('difficulty normal', 0.3)
    _score(s)
    end = 'execute in minecraft:the_end run '
    try:
        s.chat(end + 'tp @s 12.5 90 12.5', 3.0)
        dragons = None
        for _ in range(12):
            dragons = _count(s, f'dragon-{_}', '@e[type=minecraft:ender_dragon]')
            if dragons:
                break
            time.sleep(2.0)
        s.check('the End: the main dragon appears', bool(dragons), f'count {dragons}')
        if not dragons:
            return
        # small arena 17 blocks from the portal: obsidian floor (dragon-immune), invisible barrier walls
        s.chat(end + 'fill 9 79 9 15 84 15 minecraft:barrier hollow', 0.6)
        s.chat(end + 'fill 9 79 9 15 79 15 minecraft:obsidian', 0.6)
        s.chat(end + 'tp @s 12.5 80 12.5 facing 0 66.4 0', 1.2)
        s.chat('effect give @s minecraft:resistance infinite 4 true', 0.3)
        s.chat('effect give @s minecraft:regeneration infinite 2 true', 0.3)
        s.chat('kill @e[type=simpleenddragonfight:dragonmite]', 0.3)
        # creative does not count (countCreative false): survival joins the fight -> entry animation
        s.chat('gamemode survival @s', 0.3)
        time.sleep(1.2)
        s.shot('entry-animation-1')
        time.sleep(1.0)
        s.shot('entry-animation-2')
        time.sleep(1.2)
        s.shot('entry-animation-3')
        s.chat('execute store result score #max n133 run attribute @e[type=minecraft:ender_dragon,limit=1] minecraft:max_health get', 0.4)
        _say(s, 'if score #max n133 matches 200', 'max-200')
        _check_said(s, 'one player: the dragon keeps the Vanilla 200 health (solo = Vanilla)', ['max-200'])
        # below half health -> a Dragonmite wave on the ring around the portal
        s.chat('data modify entity @e[type=minecraft:ender_dragon,limit=1] Health set value 80.0f', 0.4)
        time.sleep(2.5)
        mites = _count(s, 'wave', '@e[type=simpleenddragonfight:dragonmite,tag=!n133]')
        s.check('below half health: a Dragonmite wave spawns (miteBase 2 with one player)', bool(mites), f'count {mites}')
        s.chat(end + 'tp @s 12.5 80 12.5 facing 0 66.4 0', 1.0)
        s.shot('wave-around-portal')
        # display mite (summoned, unbound, NoAI) for the model shot
        s.chat(end + 'summon simpleenddragonfight:dragonmite 12.5 80 14.5 {NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Tags:["n133"]}', 0.6)
        if _aim(s, (12.5, 80, 12.5, end), (12.5, 80.2, 14.5), 'dragonmite', 'display mite'):
            s.shot('dragonmite-model')
            back = _third_person(s)
            s.shot('dragonmite-third-person')
            back()
        # the dragon gone -> every bound mite dissolves without loot, the unbound one stays
        s.chat('kill @e[type=minecraft:ender_dragon]', 0.4)
        time.sleep(2.5)
        bound = _count(s, 'bound-after', '@e[type=simpleenddragonfight:dragonmite,tag=!n133]')
        unbound = _count(s, 'unbound-after', '@e[type=simpleenddragonfight:dragonmite,tag=n133]')
        chitin = _count(s, 'chitin', '@e[type=minecraft:item,nbt={Item:{id:"simpleenddragonfight:dragon_chitin"}}]')
        s.check('the dragon gone: every bound Dragonmite dissolves', bound == 0, f'{bound} left')
        s.check('the dissolved mites drop no Dragon Chitin', chitin == 0, f'{chitin} chitin items')
        s.check('the unbound (summoned) mite stays', unbound == 1, f'{unbound}')
    finally:
        s.chat('kill @e[type=simpleenddragonfight:dragonmite]', 0.3)
        s.chat(end + 'fill 9 79 9 15 84 15 minecraft:air', 0.5)
        s.chat('effect clear @s', 0.3)
        s.chat('gamemode creative @s', 0.3)
        s.chat(f'execute in minecraft:overworld run tp @s {home[0] + 0.5} {home[1]} {home[2] + 0.5}', 2.0)
        s.chat('difficulty peaceful', 0.3)


# ---------------------------------------------------------------- Simple Respawn
def _screens_for(s, seconds, want):
    """Polls the screen class for `seconds`; returns (every class seen, seconds until `want` appeared or None)."""
    seen, t0, hit = [], time.time(), None
    while time.time() - t0 < seconds:
        scr = s.st().get('screen', '')
        if not seen or seen[-1] != scr:
            seen.append(scr)
        if want in scr and hit is None:
            hit = time.time() - t0
            break
        time.sleep(0.25)
    return seen, hit


def respawn_bleedout(s):
    _prep(s, 'survival')
    s.chat('difficulty normal', 0.3)
    s.chat('simplerespawn config set onlyWithOtherPlayers false', 0.8)
    s.chat('simplerespawn config set bleedOutSeconds 15', 0.8)
    _score(s)
    try:
        x, y, z = _arena(s, 60)
        s.chat('item replace entity @s armor.head with minecraft:iron_helmet', 0.3)
        s.chat('item replace entity @s inventory.5 with minecraft:diamond 3', 0.3)
        _hand(s, 3, 'minecraft:iron_sword', 'sword')
        _mark(s, 'downed')
        t0 = time.time()
        s.chat('damage @s 100 minecraft:generic', 0.3)
        st = s.wait_screen('DownedScreen', 8)
        ok = 'DownedScreen' in st.get('screen', '')
        s.check('lethal damage (alone, onlyWithOtherPlayers off) knocks down: DownedScreen', ok, st.get('screen'))
        if not ok:
            return
        labels = s.labels(st)
        s.check('Downed screen: Spectate / Inventory / Give up', all(any(t in l for l in labels) for t in ('Spectate', 'Inventory', 'Give up'))
                or all(any(t in l for l in labels) for t in ('Zuschauen', 'Inventar', 'Aufgeben')), labels)
        s.shot('downed-screen')
        _say(s, 'if entity @s[tag=simplerespawn_downed]', 'downed-tag')
        s.chat('execute store result score #hp n133 run data get entity @s Health', 0.4)
        _say(s, 'if score #hp n133 matches 1', 'health-1')
        _check_said(s, 'downed: tag simplerespawn_downed and health 1', ['downed-tag', 'health-1'])
        log = _log_since(s, 'downed')
        s.check('chat: "<name> is down and needs help!" (announce)', 'is down and needs help' in log or 'braucht Hilfe' in log, log[-300:])
        if s.click_widget('Spectate'):
            st = s.wait_screen('none', 6)
            s.check('REAL click Spectate closes the screen (camera free, HUD timer)', st.get('screen') == 'none', st.get('screen'))
            time.sleep(0.6)
            s.shot('downed-hud-timer')
            back = _third_person(s)
            time.sleep(0.6)
            s.shot('downed-crawling-third-person')
            back()
        seen, hit = _screens_for(s, 25, 'DeathScreen')
        took = time.time() - t0
        s.check('bleed-out: the Vanilla death screen follows', hit is not None, f'screens {seen}')
        if hit is None:
            return
        print(f'NOTE knock-down to death screen {took:.1f} s (bleedOutSeconds 15, plus command latency)', flush=True)
        s.check('the bleed-out takes about bleedOutSeconds (15 s; 12-22 s with latency)', 12 <= took <= 22, f'{took:.1f} s')
        time.sleep(1.2)
        s.shot('bled-out-death-screen')
        s.click_widget(label='Respawn')
        time.sleep(2.0)
        if 'DeathScreen' in s.screen():
            s.check('REAL click on Respawn', False, s.last.get('screen'))
            return
        _say(s, 'if items entity @s hotbar.3 minecraft:iron_sword', 'sword-kept')
        _say(s, 'if items entity @s armor.head minecraft:iron_helmet', 'helmet-kept')
        _say(s, 'unless items entity @s inventory.* minecraft:diamond', 'diamond-dropped')
        _say(s, 'unless entity @s[tag=simplerespawn_downed]', 'tag-gone')
        _check_said(s, 'after the respawn (T27): sword and helmet kept, diamonds dropped, downed tag gone',
                    ['sword-kept', 'helmet-kept', 'diamond-dropped', 'tag-gone'])
        _say(s, 'if items entity @s weapon.mainhand minecraft:iron_sword', 'selected-still-4')
        print('NOTE selected slot after the respawn still 4 (sword in hand):', _said(s, ['selected-still-4'])['selected-still-4'], flush=True)
        dx, dy, dz = x + 0.5, y, z + 0.5
        s.chat(f'execute as @e[type=minecraft:item,x={dx},y={dy},z={dz},distance=..10] run tp @s {dx} {dy} {dz}', 0.6)
        n = _count(s, 'death-drops', f'@e[type=minecraft:item,x={dx},y={dy},z={dz},distance=..3,nbt={{Item:{{id:"minecraft:diamond"}}}}]')
        s.check('the diamonds lie on the death spot', bool(n), f'{n} diamond stacks')
        s.chat('kill @e[type=minecraft:item,distance=..80]', 0.3)
        # alone with the default (onlyWithOtherPlayers on): a plain Vanilla death, no Downed screen
        s.chat('simplerespawn config reset onlyWithOtherPlayers', 0.8)
        s.chat(f'tp @s {dx} {dy} {dz} 0 0', 1.0)
        s.chat('damage @s 100 minecraft:generic', 0.2)
        seen, hit = _screens_for(s, 8, 'DeathScreen')
        s.check('alone (default on): lethal damage is a plain Vanilla death', hit is not None and not any('DownedScreen' in x for x in seen),
                f'screens {seen}')
        if hit is not None:
            time.sleep(1.0)
            s.shot('alone-vanilla-death')
            s.click_widget(label='Respawn')
            time.sleep(2.0)
    finally:
        s.chat('simplerespawn config reset onlyWithOtherPlayers', 0.6)
        s.chat('simplerespawn config reset bleedOutSeconds', 0.6)
        s.chat('kill @e[type=minecraft:item,distance=..80]', 0.3)
        s.chat('difficulty peaceful', 0.3)


SCENARIOS = {
    'n133-resonance-inventory': resonance_inventory,
    'n133-dragon-solo': dragon_solo,
    'n133-respawn-bleedout': respawn_bleedout,
}
