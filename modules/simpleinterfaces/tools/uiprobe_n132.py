"""N132 release polish, mods 41-43 (offen-vor-release.md section 3: Simple Weather, Simple Loot, Simple Mob Manners), word for word:

Simple Weather
  n132-weather-off     disableWeather in the real game: '/weather rain' rains (weather_check predicate), then
                       '/simpleweather config set disableWeather true' -> the rain stops and the game rule advance_weather reads false;
                       '/weather rain' AFTER that still rains and keeps raining for 10 s (N132 fix: the switch cleared the weather every
                       tick and swallowed /weather); switching the option off leaves the rule off (handbook), restored by /gamerule.
                       Shots raining / cleared / raining with the switch on.
Simple Weather, Simple Loot, Simple Mob Manners
  n132-config-screens  The config screen of each mod (SimpleLib's generic screen, opened through the probe): every entry name is the
                       English lang text of its option (no raw path), every entry has a tooltip without raw keys; Simple Weather shows
                       "Rain Density (Client)" (N132 rename of "Rain Disturbance"). Shots per screen + tooltip under the real pointer.
Simple Loot
  n132-loot-vault      Survival, vaultCooldownDays 1 for the scene (restored to 100): a REAL right click with a trial key on a Vanilla
                       vault (crosshair read back) uses one key and ejects loot; a second click keeps the key (once per player, like
                       Vanilla); the vault stores the loot time (SimpleBuildingLootTimes, read with /data); the stamp is moved one
                       Minecraft day (24000 ticks + 1) into the past -> the third click uses the key and ejects loot again. Shots.
Simple Mob Manners
  n132-mobmanners-mute Subtitles switched on through the real Options/Accessibility screen; NoAI cow "Daisy" hurt -> a subtitle appears
                       (bottom-right box differs from the idle shot); cow "Daisy_mute" hurt -> no subtitle (box unchanged); pig hurt
                       with "minecraft:pig" in mutedEntities -> none; after removing it -> subtitle again.
  n132-mobmanners-piglins  Survival, a piglin with AI (summoned with spawn logic, immune to zombification, adult) 5 blocks away, the
                       player takes damage (scoreboard damage_taken) only in the control phase: golden sword in the hand -> no damage
                       for 12 s; iron chestplate with a gold trim, empty hand -> none for 12 s; neither -> damage within 20 s.
  n132-mobmanners-frog Survival: REAL right click with a water bucket on the cold frog "Kermit" (crosshair on the frog read back) ->
                       Bucket of Frog in the main hand with frog/variant cold, the frog gone; REAL right click on the floor -> the frog
                       is back with name and variant, the hand holds an empty bucket. Shots + tooltip of the bucket.

Patterns (uiprobe_n122-n130 lessons): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62; real clicks only after
the crosshair target was read back (status hitBlock / hitEntity); main hand = selected hotbar slot, cleared before giving; chat commands
~1-1.5 s each; mobs summoned with 'execute positioned ... summon ... run data merge' keep their spawn logic. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe132 --scale 2 \\
        --only n132-weather-off,n132-config-screens,n132-loot-vault,n132-mobmanners-mute,n132-mobmanners-piglins,n132-mobmanners-frog
"""
import json
import re
import time
from pathlib import Path

TAG = 'N132'
FLOOR = 'minecraft:smooth_stone'
REPO = Path(__file__).resolve().parents[3]


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


def _mark(s, marker):
    s.chat(f'say {TAG} MARK {marker}', 0.35)


def _after(s, marker, pattern, lines_after=20):
    """Last regex match of pattern in the log lines after the last 'N132 MARK <marker>' line."""
    time.sleep(1.2)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {marker}')), default=-1)
    found = None
    for ln in lines[idx + 1: idx + 1 + lines_after] if idx >= 0 else []:
        m = re.search(pattern, ln)
        if m:
            found = m
    return found


def _prep(s, mode='creative'):
    s.close()
    for k in ('Shift_L', 'space', 'w'):
        s.io.key_up(k)
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
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


def _arena(s, dx, x0=-4, z0=-3, x1=8, z1=12, height=6):
    """Moves dx blocks east, lays a smooth stone floor under fresh air and returns the feet position."""
    x, y, z = _pos(s)
    x += dx
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
    _say(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-mainhand')
    return _check_said(s, f'{tag}: {item} is in the main hand (selected hotbar slot {slot + 1})', [f'{tag}-mainhand'])


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


def _shot_path(s, name):
    return s.out / 'shots' / f"{s.scenario.replace('/', '-')}-{name}.png"


def _differs(a, b, min_rms=2.0, box=None):
    """(changed, rms) of two screenshots (PIL), optionally only inside box (fractions); a missing file counts as unchanged."""
    try:
        from PIL import Image, ImageChops, ImageStat
        ia, ib = Image.open(a).convert('RGB'), Image.open(b).convert('RGB')
        if ia.size != ib.size:
            return True, 999.0
        if box:
            w, h = ia.size
            rect = (int(box[0] * w), int(box[1] * h), int(box[2] * w), int(box[3] * h))
            ia, ib = ia.crop(rect), ib.crop(rect)
        rms = sum(ImageStat.Stat(ImageChops.difference(ia, ib)).rms) / 3
        return rms >= min_rms, round(rms, 2)
    except Exception as e:  # noqa: BLE001
        return False, f'{type(e).__name__}: {e}'


def _plain(text):
    return re.sub('§.', '', text or '').strip()


def _lang(mod, loc='en_us'):
    return json.loads((REPO / f'modules/{mod}/shared/resources/assets/{mod}/lang/{loc}.json').read_text(encoding='utf-8'))


def _summon(s, entity, x, y, z, nbt):
    """Summon WITH the spawn logic (finalizeSpawn), then the NBT (plain 'summon ... {nbt}' skips finalizeSpawn)."""
    s.chat(f'execute positioned {x} {y} {z} summon {entity} run data merge entity @s {nbt}', 0.6)


# ---------------------------------------------------------------- Simple Weather
RAINING = 'if predicate {condition:"minecraft:weather_check",raining:true}'
DRY = 'unless predicate {condition:"minecraft:weather_check",raining:true}'


def _rule(s, marker):
    _mark(s, marker)
    s.chat('gamerule advance_weather', 0.5)
    m = _after(s, marker, r'advance_weather\D*?(true|false)')
    return m.group(1) if m else None


def weather_off(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    s.chat('simpleweather config set disableWeather false', 0.8)
    s.chat('gamerule advance_weather true', 0.5)
    try:
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 -20', 0.8)
        s.chat('weather rain 6000', 0.5)
        s.check('before: /weather rain makes it rain', _poll(s, RAINING, 'rain-before', 12))
        time.sleep(2.0)
        s.shot('1-raining')
        s.chat('simpleweather config set disableWeather true', 0.8)
        s.check('disableWeather true: the rain stops at once', _poll(s, DRY, 'cleared', 8))
        rule = _rule(s, 'rule-on')
        s.check('disableWeather true: the game rule advance_weather reads false', rule == 'false', rule)
        time.sleep(1.5)
        s.shot('2-cleared')
        s.chat('weather rain 6000', 0.5)
        started = _poll(s, RAINING, 'rain-switch-on', 12)
        stays = started and all(_test(s, RAINING, f'rain-stays-{i}') or False for i in range(4))
        s.check('/weather rain still works while disableWeather is on (rains and keeps raining ~8 s, N132 fix)', started and stays,
                f'started {started}, stays {stays}')
        time.sleep(1.0)
        s.shot('3-raining-switch-on')
        s.chat('weather clear', 0.5)
        s.chat('simpleweather config set disableWeather false', 0.8)
        rule = _rule(s, 'rule-off')
        s.check('disableWeather false again: the rule stays off (handbook: /gamerule turns it back on)', rule == 'false', rule)
    finally:
        s.chat('simpleweather config set disableWeather false', 0.6)
        s.chat('gamerule advance_weather true', 0.5)
        s.chat('weather clear', 0.5)
        _done(s)


# ---------------------------------------------------------------- config screens (all three)
SCREENS = (('simpleweather', 4, 'Rain Density (Client)'), ('simpleloot', 2, None), ('simplemobmanners', 4, None))


def config_screens(s):
    _prep(s)
    for mod, least, must in SCREENS:
        lang = _lang(mod)
        names = {_plain(v) for k, v in lang.items() if k.startswith(f'text.autoconfig.{mod}.option.') and not k.endswith('@Tooltip')}
        cats = {_plain(v) for k, v in lang.items() if '.category.' in k or k.endswith('.tab.general') or k == f'{mod}.title'}
        s.close()
        st = s.open_config(mod, 2.5)
        screen = st.get('screen', 'none')
        if screen == 'none':
            s.check(f'{mod}: config screen opens', False, screen)
            continue
        time.sleep(0.8)
        st = s.st()
        entries = [e for e in st.get('entries', []) if _plain(e.get('label')).strip()]
        shown = [_plain(e['label']) for e in entries]
        options = [n for n in shown if n not in cats]
        bad = [n for n in options if n not in names]
        no_tip = [n for e, n in zip(entries, shown) if n in names and not [x for x in e.get('tooltip', []) if _plain(x)]]
        raw_tip = [f'{n}: {x}' for e, n in zip(entries, shown) for x in e.get('tooltip', [])
                   if 'autoconfig' in x or '@Tooltip' in x or re.fullmatch(r'[a-z0-9_.]+\.[a-z0-9_.]+', _plain(x))]
        s.check(f'{mod}: config screen lists at least {least} options', len(options) >= least, f'{screen}: {shown}')
        s.check(f'{mod}: every entry name is an English lang text (no raw path)', options and not bad, bad)
        s.check(f'{mod}: every option has a tooltip, none is a raw key', options and not no_tip and not raw_tip, (no_tip, raw_tip[:4]))
        if must:
            s.check(f'{mod}: shows "{must}"', must in shown, shown)
        s.shot(f'{mod}')
        s.io.move(*s.abs(st, 380, 120))
        time.sleep(1.2)
        s.shot(f'{mod}-tooltip')
        _center_pointer(s)
    s.close()


# ---------------------------------------------------------------- Simple Loot
def loot_vault(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    s.chat('simpleloot vaults repeat true', 0.8)
    s.chat('simpleloot vaults cooldown 1', 0.8)
    vx, vy, vz = x, y, z + 3
    vault = f'{vx} {vy} {vz}'
    me = (x + 0.5, y, z + 0.5)
    centre = (vx + 0.5, vy + 0.5, vz + 0.5)
    try:
        s.chat(f'setblock {vault} minecraft:vault[facing=north]', 0.6)
        s.chat('effect give @s minecraft:resistance 300 4 true', 0.3)
        if not _hand(s, 0, 'minecraft:trial_key 3', 'vault'):
            return
        s.check('the vault turns active with the player near', _poll(s, f'if block {vault} minecraft:vault[vault_state=active]', 'active', 10))

        def use(n):
            if not _aim(s, me, centre, 'minecraft:vault', f'vault click {n}'):
                return False
            if not _hit_is(s, 'minecraft:vault', f'vault click {n}'):
                return False
            _click(s, 0.8)
            return True

        def ejected(tag):
            """Loot items lie around the vault within ~12 s (the vault unlocks, then ejects one by one)."""
            return _poll(s, f'positioned {vx} {vy} {vz} if entity @e[type=minecraft:item,distance=..5]', tag, 12)

        use(1)
        keys = _total(s, 'minecraft:trial_key')
        loot = ejected('loot-1')
        s.check('click 1: one trial key used and loot ejected', keys == 2 and loot, f'keys {keys}, loot {loot}')
        s.shot('1-reward')
        s.check('vault back to active after ejecting', _poll(s, f'if block {vault} minecraft:vault[vault_state=active]', 'active-2', 20))
        s.chat(f'kill @e[type=minecraft:item,distance=..12]', 0.4)
        use(2)
        time.sleep(1.5)
        keys = _total(s, 'minecraft:trial_key')
        none = not _test(s, f'positioned {vx} {vy} {vz} if entity @e[type=minecraft:item,distance=..5]', 'loot-2')
        s.check('click 2 right after: the key stays (once per player until the cooldown, like Vanilla)', keys == 2 and none, f'keys {keys}, no loot {none}')
        s.shot('2-refused')
        _mark(s, 'times')
        s.chat(f'data get block {vault} SimpleBuildingLootTimes', 0.6)
        m = _after(s, 'times', r'"([0-9a-f]{8}-[0-9a-f-]{27})"\s*:\s*(-?\d+)L')
        s.check('the vault stored the loot time of the player (SimpleBuildingLootTimes)', m is not None)
        _mark(s, 'gametime')
        s.chat('time query gametime', 0.6)
        g = _after(s, 'gametime', r'The time is (\d+)')
        s.check('game time read back', g is not None)
        if not (m and g):
            return
        past = int(g.group(1)) - 24001
        s.chat(f'data modify block {vault} SimpleBuildingLootTimes."{m.group(1)}" set value {past}L', 0.8)
        time.sleep(2.5)  # the vault's cooldown check runs every 40 ticks
        use(3)
        keys = _total(s, 'minecraft:trial_key')
        loot = ejected('loot-3')
        s.check('one Minecraft day later (stamp moved back 24001 ticks): click 3 uses a key and ejects loot again', keys == 1 and loot,
                f'keys {keys}, loot {loot}')
        s.shot('3-reward-again')
    finally:
        s.chat('simpleloot vaults cooldown 100', 0.6)
        s.chat(f'setblock {vault} minecraft:air', 0.3)
        s.chat('kill @e[type=minecraft:item,distance=..15]', 0.3)
        _done(s)


# ---------------------------------------------------------------- Simple Mob Manners
def _subtitles(s, want_on):
    """Show Subtitles through the real Options -> Accessibility screen; True when the button reads the wanted state."""
    s.close()
    s.io.key('Escape', 1.0)
    if 'Pause' not in s.wait_screen('Pause', 5).get('screen', ''):
        return False
    ok = False
    try:
        if not s.click_widget('Options', name='Options button'):
            return False
        s.wait_screen('Options', 5)
        if not s.click_widget('Accessibility', name='Accessibility button'):
            return False
        time.sleep(1.0)
        for _ in range(2):
            w = s.find_widget('Subtitles', st=s.st())
            if not w:
                break
            on = w['label'].rstrip().endswith('ON')
            if on == want_on:
                ok = True
                break
            s.click_xy(w['x'], w['y'])
            time.sleep(0.6)
    finally:
        s.close()
        _center_pointer(s)
    return ok


SUB_BOX = (0.62, 0.62, 1.0, 0.97)   # Vanilla draws subtitles bottom right


def mobmanners_mute(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    s.chat('simplemobmanners mutedEntities clear', 0.8)
    s.check('subtitles switched on through the real Options/Accessibility screen', _subtitles(s, True))
    try:
        # the animals stand behind the player (out of the picture); the camera looks at the plain floor
        _summon(s, 'minecraft:cow', x - 2.5, y, z - 2.0, '{CustomName:"Daisy",NoAI:1b,PersistenceRequired:1b,Tags:["n132mute","n132a"]}')
        _summon(s, 'minecraft:cow', x + 0.5, y, z - 2.0, '{CustomName:"Daisy_mute",NoAI:1b,PersistenceRequired:1b,Tags:["n132mute","n132b"]}')
        _summon(s, 'minecraft:pig', x + 3.5, y, z - 2.0, '{NoAI:1b,PersistenceRequired:1b,Tags:["n132mute","n132c"]}')
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 60', 1.0)
        time.sleep(5.0)           # every subtitle of the setup fades out
        s.shot('0-idle')
        idle = _shot_path(s, '0-idle')

        def hurt(tag, name):
            s.chat(f'damage @e[tag={tag},limit=1] 1', 0.2)
            time.sleep(0.4)
            s.shot(name)
            changed = _differs(idle, _shot_path(s, name), 3.0, SUB_BOX)
            time.sleep(4.5)       # let the subtitle fade before the next
            return changed

        got = hurt('n132a', '1-daisy')
        s.check('cow "Daisy" hurt: a subtitle appears (subtitle box differs from the idle shot)', got[0], got[1])
        got = hurt('n132b', '2-daisy_mute')
        s.check('cow "Daisy_mute" hurt: no subtitle (name suffix _mute silences it)', not got[0], got[1])
        s.chat('simplemobmanners mutedEntities add "minecraft:pig"', 0.8)
        got = hurt('n132c', '3-pig-muted')
        s.check('pig hurt with minecraft:pig in mutedEntities: no subtitle', not got[0], got[1])
        s.chat('simplemobmanners mutedEntities remove "minecraft:pig"', 0.8)
        got = hurt('n132c', '4-pig-unmuted')
        s.check('pig hurt after removing it from mutedEntities: subtitle again', got[0], got[1])
    finally:
        s.chat('simplemobmanners mutedEntities clear', 0.6)
        s.chat('kill @e[tag=n132mute]', 0.3)
        _subtitles(s, False)
        _done(s)


def mobmanners_piglins(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40, x0=-6, z0=-6, x1=6, z1=12)
    s.chat('simplemobmanners piglinsIgnoreGoldTrims true', 0.8)
    s.chat('simplemobmanners piglinsIgnoreGoldTools true', 0.8)
    s.chat('scoreboard objectives add n132dmg minecraft.custom:minecraft.damage_taken', 0.4)
    me = (x + 0.5, y, z + 0.5)
    trim = 'minecraft:iron_chestplate[minecraft:trim={material:"minecraft:gold",pattern:"minecraft:coast"}]'
    phases = (('golden sword in the hand, no armor', 'minecraft:golden_sword', None, False, 12),
              ('iron chestplate with a gold trim, empty hand', None, trim, False, 12),
              ('control: no gold at all', None, None, True, 20))
    try:
        for i, (what, held, chest, want_hurt, seconds) in enumerate(phases):
            s.chat('kill @e[tag=n132piglin]', 0.3)
            s.chat('clear @s', 0.3)
            s.chat('effect give @s minecraft:regeneration 120 2 true', 0.3)
            s.chat('effect give @s minecraft:resistance 120 1 true', 0.3)
            if held:
                _hand(s, 0, held, f'piglin {i + 1}')
            else:
                s.chat('item replace entity @s hotbar.0 with minecraft:air', 0.3)
                s.io.key('1', 0.4)
            if chest:
                s.chat(f'item replace entity @s armor.chest with {chest}', 0.4)
            s.chat(f'tp @s {me[0]} {y} {me[2]} 0 0', 0.6)
            s.chat('scoreboard players set @s n132dmg 0', 0.3)
            _summon(s, 'minecraft:piglin', x + 0.5, y, z + 5.5,
                    '{IsImmuneToZombification:1b,IsBaby:0b,PersistenceRequired:1b,Tags:["n132piglin"]}')
            hurt = _poll(s, 'if score @s n132dmg matches 1..', f'hurt-{i + 1}', seconds)
            s.shot(f'{i + 1}-' + ('attacked' if hurt else 'left-alone'))
            if want_hurt:
                s.check(f'piglin, {what}: the piglin attacks (damage taken)', hurt)
            else:
                s.check(f'piglin, {what}: no damage for {seconds} s', not hurt)
    finally:
        s.chat('kill @e[tag=n132piglin]', 0.3)
        s.chat('scoreboard objectives remove n132dmg', 0.3)
        _done(s)


def mobmanners_frog(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    me = (x + 0.5, y, z + 0.5)
    fx, fz = x + 0.5, z + 3.0
    try:
        _summon(s, 'minecraft:frog', fx, y, fz, '{variant:"minecraft:cold",CustomName:"Kermit",NoAI:1b,PersistenceRequired:1b,Tags:["n132frog"]}')
        if not _hand(s, 0, 'minecraft:water_bucket', 'frog'):
            return
        if not _aim(s, me, (fx, y + 0.25, fz), 'minecraft:frog', 'frog catch', key='hitEntity'):
            return
        if not _hit_is(s, 'minecraft:frog', 'frog catch', key='hitEntity'):
            return
        _click(s, 0.8)
        _say(s, 'if items entity @s weapon.mainhand simplemobmanners:frog_bucket[minecraft:frog/variant="minecraft:cold"]', 'frog-bucket')
        _say(s, 'unless entity @e[type=minecraft:frog,name=Kermit]', 'frog-gone')
        _check_said(s, 'REAL right click with a water bucket catches the cold frog (Bucket of Frog, variant cold, frog gone)', ['frog-bucket', 'frog-gone'])
        s.chat(f'tp @s {me[0]} {y} {me[2]} 0 30', 0.8)
        s.shot('1-bucket-in-hand')
        s.key('e', 1.2)
        st = s.wait_screen('InventoryScreen', 8)
        slot = s.slot_xy(st, cslot=0, own=True) if 'InventoryScreen' in st.get('screen', '') else None
        if slot and 'frog_bucket' in slot.get('stack', ''):
            s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
            time.sleep(0.4)
            s.io.move(*s.abs(st, slot['x'], slot['y']))
            time.sleep(0.9)
            s.shot('2-bucket-tooltip')
        else:
            s.check('inventory shows the frog bucket in hotbar slot 1', False, slot)
        s.close()
        _center_pointer(s)
        target = (x + 0.5, y - 0.02, z + 3.5)
        if not _aim(s, me, target, FLOOR, 'frog release'):
            return
        if not _hit_is(s, FLOOR, 'frog release'):
            return
        _click(s, 1.0)
        _say(s, 'if entity @e[type=minecraft:frog,name=Kermit,nbt={variant:"minecraft:cold"}]', 'frog-back')
        _say(s, 'if items entity @s weapon.mainhand minecraft:bucket', 'frog-empty')
        _check_said(s, 'REAL right click on the floor releases Kermit with name and variant; an empty bucket stays', ['frog-back', 'frog-empty'])
        s.shot('3-released')
    finally:
        s.chat('kill @e[type=minecraft:frog,distance=..20]', 0.3)
        s.chat(f'fill {x - 3} {y} {z} {x + 4} {y + 1} {z + 6} minecraft:air replace minecraft:water', 0.3)
        _done(s)


SCENARIOS = {
    'n132-weather-off': weather_off,
    'n132-config-screens': config_screens,
    'n132-loot-vault': loot_vault,
    'n132-mobmanners-mute': mobmanners_mute,
    'n132-mobmanners-piglins': mobmanners_piglins,
    'n132-mobmanners-frog': mobmanners_frog,
}
