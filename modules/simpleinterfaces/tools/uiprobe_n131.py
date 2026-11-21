"""N131 release polish, mods 38-40 (offen-vor-release.md section 3, "Szenen" of Simple Fun, Simple Movement, Simple Farming), word for word:

Simple Fun       "Spielzeuge, Abklatschen (2 Spieler), Ess-Effekte, Hörprobe (W4C fun-3)."
  n131-fun-toys       Survival, subtitles switched on through the real Options -> Accessibility screen (the shots show the sound lines).
                      Every toy from hotbar slot 1 (main hand read back with 'execute if items ... weapon.mainhand'), a REAL right click,
                      and a server trace read back for each: party popper (dyed red) - stat minecraft.used:simplefun.party_popper 1 and
                      the stack 3 -> 2; dice and lucky coin - their used stat and the floating roll text (text_display within 4 blocks,
                      read right after the click, it lives 2 s); petting - an own tamed wolf (execute summon + data merge, Owner set),
                      crosshair read back on the wolf, REAL Shift held >= 0.8 s + empty-hand click -> stat simplefun.pets_given 1;
                      skipping stone over a pond - used stat 1; water balloon - crosshair on a lit campfire, after the throw lit=false;
                      bubble wand - right button held 3.6 s, the wand has taken wear (mainhand damage component); paper plane - the
                      plane entity flies; rubber duck - placed on the floor (crosshair read back), on soul sand (pirate=true) and on
                      the pond's water surface; then an empty-hand click on the duck: the shot right after it shows the squeak
                      subtitle "Rubber duck squeaks" (the ElevenLabs squeak, Hörprobe also on previews/n131-klang.html); snow angel -
                      Shift held 3.8 s standing still on snow blocks, then a REAL jump -> stat simplefun.snow_angels 1, shot from above.
                      Abklatschen (high five) needs a SECOND player: one probe client cannot do it - covered by gametest toyHighFive
                      (raise, clap, stat for both) and the clap sound by the listening page (owner, W4C-20).
  n131-fun-delights   Creative (eating needs no hunger there), subtitles on, front view (F5 twice). Cookie, apple, carrot, melon slice,
                      honey bottle, bread, sweet berries: each eaten with the REAL right button held (crosshair on the sky, so berries
                      and carrots are not planted), stat minecraft.used:minecraft.<food> 1 read back, a shot at the moment the food is
                      finished (the small delight particles at eye height: crumbs, sparkle, crunch, splash, honey drips, hearts);
                      5.5 s between foods (shared 100-tick cosmetic cooldown). Cooked porkchop -> the piggy effect is active
                      (simplefun:piggy_effect) and the pig head shows in the front view. Flower sniff: a poppy in the hand, REAL Shift
                      held, shot with the composter particles and the "Fox sniffs" subtitle.
Simple Movement  "Autowalk, Stufenhöhe mit Auto-Jump aus (W4C-33)."
  n131-movement-autowalk  Survival on a 44-block strip, '/simplemovement autowalk true'. The Auto Walk key's default is arrow up (N78;
                      the old W4C probe rebound it to G and the guide said "unbound" - fixed in N131). From a known start: REAL arrow
                      up -> after 3 s >= 5 blocks walked; arrow up again -> the position stays; arrow up, then the inventory opened
                      (REAL E) -> walking stops; server switch off ('autowalk false') -> arrow up does nothing.
  n131-movement-step  Auto-Jump switched OFF through the real Options -> Controls screen (button label read back "Auto-Jump: OFF"; the W4C
                      run was red because Auto-Jump stayed on). Survival, REAL W held 1.6 s from 2.5 blocks before a 9-block-deep
                      strip, so the walk ends ON the strip: no boots -> blocked by a 1-block stone step; Jump Boost (effect) -> up the
                      stone step (jumpBoostStepUp); Step Height I boots -> up the stone step, blocked by a 1.5-block fence strip; Step
                      Height II -> up the fence strip. y read from the block position. Third-person shots on the step. Auto-Jump is
                      put back to what it was.
Simple Farming   "Erledigt (W4C farming-1). Keine Szenen nötig." -> no scenario (gametests hoeHarvest, farmland, grassThrough ...).

Sounds: Simple Fun's own squeak/clap are ElevenLabs (KI) -> listening page previews/n131-klang.html (answer code N131). Movement/Farming
ship no sounds.

Patterns (uiprobe_n122-n130 lessons): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62, targets <= 4 blocks;
real clicks only after the crosshair target was read back (status hitBlock / hitEntity); main hand = selected hotbar slot, read back;
Shift held >= 0.8 s and status 'sneaking' checked; chat commands ~1-1.5 s each; a held click repeats (counts accept multiples);
movement measured from a known start, strips long enough that the walk ends on them. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe131 --scale 2 \\
        --only n131-fun-toys,n131-fun-delights,n131-movement-autowalk,n131-movement-step
"""
import re
import time
from pathlib import Path

TAG = 'N131'
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


def _center_pointer(s):
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    time.sleep(0.3)


def _prep(s, mode='creative'):
    s.close()
    s.io.button(3, False)
    for k in ('Shift_L', 'w', 'Up'):
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


def _arena(s, dx, x0=-4, z0=-3, x1=4, z1=24, height=6):
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
    s.check(f'{tag}: crosshair on {want}', False, str(hit))
    return False


def _hand(s, slot, stack, tag):
    """Clears and fills hotbar slot `slot`, selects it with the REAL number key and reads the main hand back."""
    s.chat(f'item replace entity @s hotbar.{slot} with minecraft:air', 0.3)
    s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.4)
    s.io.key(str(slot + 1), 0.5)
    item = stack.split('[', 1)[0].split(' ', 1)[0]
    if item == 'minecraft:air':
        _say(s, 'unless items entity @s weapon.mainhand *', f'{tag}-mainhand')
    else:
        _say(s, f'if items entity @s weapon.mainhand {item}', f'{tag}-mainhand')
    return _check_said(s, f'{tag}: {item} is in the main hand (selected hotbar slot {slot + 1})', [f'{tag}-mainhand'])


def _click(s, after=0.7, button=3, hold=0.12):
    s.io.button(button, True)
    time.sleep(hold)
    s.io.button(button, False)
    time.sleep(after)


def _sneak_click(s, tag, button=3):
    """REAL Shift held >= 0.8 s (sneaking read back), then the click."""
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.9)
        s.check(f'{tag}: the client sneaks before the click (real Shift held)', bool(s.st().get('sneaking')), str(s.last.get('sneaking')))
        _click(s, 0.6, button)
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.4)


def _count(s, slot=0):
    inv = s.st().get('inventory', [])
    stack = inv[slot] if len(inv) > slot else ''
    return int(stack.rsplit(' x', 1)[1]) if ' x' in stack else 0


def _third_person(s, front=False):
    """F5 once = behind the player, twice = in front; the returned function goes back to first person."""
    s.key('F5', 0.5)
    if front:
        s.key('F5', 0.5)

    def back():
        s.key('F5', 0.5)
        if not front:
            s.key('F5', 0.5)
    return back


def _stat(s, name, criterion):
    """A fresh scoreboard objective for a statistic criterion (starts at 0 for the player)."""
    s.chat(f'scoreboard objectives remove {name}', 0.3)
    s.chat(f'scoreboard objectives add {name} {criterion}', 0.4)


def _menu_toggle(s, path, label, want_on):
    """Pause -> Options -> <path buttons> -> the toggle button whose label starts with `label`; clicks it until it reads ON/OFF as
    wanted. Returns (ok, was_on) - was_on = the state before (None when the button was not found)."""
    s.close()
    s.io.key('Escape', 1.0)
    if 'Pause' not in s.wait_screen('Pause', 5).get('screen', ''):
        s.check(f'{label}: pause screen opened (Escape)', False, s.last.get('screen', '?'))
        return False, None
    ok, was = False, None
    try:
        if not s.click_widget('Options', name='Options button'):
            return False, None
        s.wait_screen('Options', 5)
        for step in path:
            time.sleep(0.6)
            if not s.click_widget(step, name=f'{step} button'):
                return False, None
        time.sleep(1.0)
        for _ in range(3):
            w = s.find_widget(label, st=s.st())
            if not w:
                s.check(f'{label} button present', False, '; '.join(s.labels(s.last))[:400])
                break
            on = w['label'].rstrip().endswith('ON')
            if was is None:
                was = on
            if on == want_on:
                ok = True
                break
            s.click_xy(w['x'], w['y'])
            time.sleep(0.6)
    finally:
        s.close()
        _center_pointer(s)
    return ok, was


def _subtitles(s, want_on):
    return _menu_toggle(s, ['Accessibility'], 'Closed Captions', want_on)  # 26.3 names the subtitles button "Closed Captions"


# ---------------------------------------------------------------- Simple Fun: toys
def fun_toys(s):
    _prep(s, 'survival')
    ok, _ = _subtitles(s, True)
    s.check('subtitles switched on through the real Options/Accessibility screen', ok)
    x, y, z = _arena(s, 40, x0=-5, x1=5, z0=-4, z1=16)
    me = (x + 0.5, y, z + 0.5)
    stats = {'n131pp': 'minecraft.used:simplefun.party_popper', 'n131di': 'minecraft.used:simplefun.dice',
             'n131co': 'minecraft.used:simplefun.lucky_coin', 'n131pe': 'minecraft.custom:simplefun.pets_given',
             'n131ss': 'minecraft.used:simplefun.skipping_stone', 'n131sa': 'minecraft.custom:simplefun.snow_angels'}
    for name, crit in stats.items():
        _stat(s, name, crit)
    s.chat('kill @e[tag=n131fun]', 0.3)
    try:
        # party popper (red)
        if _hand(s, 0, 'simplefun:party_popper[dyed_color=11546150] 3', 'popper'):
            s.chat(f'tp @s {me[0]} {y} {me[2]} 0 -10', 1.0)
            _click(s, 0.35)
            s.shot('party-popper')
            _say(s, 'if score @s n131pp matches 1..', 'popper-used')
            _check_said(s, 'party popper: used once (stat simplefun.party_popper)', ['popper-used'])
            s.check('party popper: the stack is used up by one (3 -> 2)', _count(s) == 2, s.st().get('inventory', [''])[0])

        # dice and lucky coin: the roll floats over the head (third person)
        for obj, (name, item) in (('n131di', ('dice', 'simplefun:dice 2')), ('n131co', ('coin', 'simplefun:lucky_coin 2'))):
            if not _hand(s, 0, item, name):
                continue
            s.chat(f'tp @s {me[0]} {y} {me[2]} 0 -10', 1.0)
            back = _third_person(s)
            _click(s, 0.05)
            _say(s, 'if entity @e[type=minecraft:text_display,distance=..4]', f'{name}-text')
            s.shot(f'{name}-roll')
            back()
            _say(s, f'if score @s {obj} matches 1..', f'{name}-used')
            _check_said(s, f'{name}: rolled (used stat) and the result floats over the head (text display)', [f'{name}-text', f'{name}-used'])
            time.sleep(1.5)

        # petting an own tamed wolf (Shift + empty hand)
        s.chat(f'execute positioned {x + 0.5} {y} {z + 2.5} summon minecraft:wolf run data merge entity @s {{NoAI:1b,Tags:["n131fun"]}}', 0.6)
        s.chat('data modify entity @e[type=minecraft:wolf,tag=n131fun,limit=1] Owner set from entity @s UUID', 0.6)
        _say(s, 'if data entity @e[type=minecraft:wolf,tag=n131fun,limit=1] Owner', 'wolf-owner')
        _check_said(s, 'petting: the wolf is tamed by the player (Owner)', ['wolf-owner'])
        if _hand(s, 1, 'minecraft:air', 'petting') and _aim(s, me, (x + 0.5, y + 0.5, z + 2.5), 'minecraft:wolf', 'petting', key='hitEntity'):
            _sneak_click(s, 'petting')
            s.shot('petting-hearts')
            _say(s, 'if score @s n131pe matches 1..', 'pet-stat')
            _check_said(s, 'petting: Shift + empty-hand click pets the own wolf (stat simplefun.pets_given)', ['pet-stat'])
        s.chat('kill @e[tag=n131fun]', 0.3)

        # pond (three deep, z+6..z+14) for the skipping stone and the floating duck; deep enough that the crosshair does not reach the
        # pond floor (a block click on the floor would place the duck under water like any block)
        s.chat(f'fill {x - 4} {y - 4} {z + 6} {x + 4} {y - 4} {z + 14} {FLOOR}', 0.5)
        s.chat(f'fill {x - 4} {y - 3} {z + 6} {x + 4} {y - 1} {z + 14} minecraft:water', 0.6)
        if _hand(s, 0, 'simplefun:skipping_stone 3', 'stone'):
            s.chat(f'tp @s {me[0]} {y} {me[2]} facing {x + 0.5} {y - 1 - 1.62 + 0.9:.2f} {z + 12.5}', 1.0)
            _click(s, 0.4)
            s.shot('skipping-stone')
            _say(s, 'if score @s n131ss matches 1..', 'stone-used')
            _check_said(s, 'skipping stone: thrown over the pond (used stat)', ['stone-used'])

        # water balloon douses a lit campfire
        s.chat(f'setblock {x - 3} {y} {z + 2} minecraft:campfire[lit=true]', 0.5)
        if _hand(s, 0, 'simplefun:water_balloon 3', 'balloon') and _aim(s, me, (x - 2.5, y + 0.3, z + 2.5), 'minecraft:campfire', 'balloon'):
            s.shot('balloon-before')
            _click(s, 1.2)
            s.shot('balloon-after')
            _say(s, f'if block {x - 3} {y} {z + 2} minecraft:campfire[lit=false]', 'campfire-out')
            _check_said(s, 'water balloon: the lit campfire is doused (lit=false)', ['campfire-out'])
            s.check('water balloon: used up by one (3 -> 2)', _count(s) == 2, f'count {_count(s)}')

        # bubble wand: right button held
        if _hand(s, 0, 'simplefun:bubble_wand', 'wand'):
            s.chat(f'tp @s {me[0]} {y} {me[2]} 0 -35', 1.0)
            s.io.button(3, True)
            try:
                time.sleep(3.6)
                s.shot('bubble-wand')
            finally:
                s.io.button(3, False)
            time.sleep(0.5)
            comp = s.st().get('mainhandComponents', '')
            s.check('bubble wand: blowing wears the wand (damage component)', 'damage' in comp, comp[:200])

        # paper plane
        if _hand(s, 0, 'simplefun:paper_plane 3', 'plane'):
            s.chat(f'tp @s {me[0]} {y} {me[2]} 0 -20', 1.0)
            _click(s, 0.2)
            _say(s, 'if entity @e[type=simplefun:paper_plane,distance=..60]', 'plane-flies')
            s.shot('paper-plane')
            _check_said(s, 'paper plane: the plane flies (entity)', ['plane-flies'])
            s.check('paper plane: used up by one (3 -> 2)', _count(s) == 2, f'count {_count(s)}')

        # rubber duck: floor, soul sand (pirate), water surface; then the squeak
        s.chat(f'setblock {x + 2} {y - 1} {z + 2} minecraft:soul_sand', 0.4)
        if _hand(s, 0, 'simplefun:rubber_duck 4', 'duck'):
            if _aim(s, me, (x + 0.5, y - 0.02, z + 2.5), FLOOR, 'duck floor'):
                _click(s, 0.6)
                _say(s, f'if block {x} {y} {z + 2} simplefun:rubber_duck[pirate=false]', 'duck-floor')
            if _aim(s, me, (x + 2.5, y - 0.02, z + 2.5), 'minecraft:soul_sand', 'duck soul sand'):
                _click(s, 0.6)
                _say(s, f'if block {x + 2} {y} {z + 2} simplefun:rubber_duck[pirate=true]', 'duck-pirate')
            # the water surface of the pond: the crosshair passes the water (fluids are not targeted) and ends in reach before the
            # floor (None); the duck's own use() finds the water source (like a lily pad)
            stand = (x + 0.5, y, z + 3.5)
            if _aim(s, stand, (x + 0.5, y - 0.1, z + 6.5), None, 'duck water'):
                _click(s, 0.6)
                _say(s, f'if block {x} {y} {z + 6} simplefun:rubber_duck', 'duck-water')
            _check_said(s, 'rubber duck: placed on the floor, as pirate duck on soul sand and floating on the water', ['duck-floor', 'duck-pirate', 'duck-water'])
            # only the camera for the shot (no check: the view ends on the floor, which is fine)
            s.chat(f'tp @s {me[0]} {me[1]} {me[2]} facing {x + 1.5} {y + 0.2 - 1.62:.2f} {z + 3.0}', 0.8)
            s.shot('rubber-ducks')
        if _hand(s, 1, 'minecraft:air', 'squeak') and _aim(s, me, (x + 0.5, y + 0.25, z + 2.5), 'simplefun:rubber_duck', 'squeak'):
            _click(s, 0.1)
            s.shot('duck-squeak-subtitle')
            time.sleep(1.0)

        # snow angel: Shift held still on snow, then a real jump
        s.chat(f'fill {x - 3} {y - 1} {z - 3} {x + 3} {y - 1} {z + 1} minecraft:snow_block', 0.6)
        if _hand(s, 1, 'minecraft:air', 'angel'):
            s.chat(f'tp @s {x + 0.5} {y} {z - 0.5} 0 20', 1.2)
            s.io.key_down('Shift_L')
            try:
                time.sleep(3.8)
                s.check('snow angel: the client sneaks (real Shift held)', bool(s.st().get('sneaking')), str(s.last.get('sneaking')))
                s.io.key('space', 0.3)
            finally:
                s.io.key_up('Shift_L')
            time.sleep(0.8)
            _say(s, 'if score @s n131sa matches 1..', 'angel-stat')
            _check_said(s, 'snow angel: 3 s still on snow + jump makes the angel (stat simplefun.snow_angels)', ['angel-stat'])
            s.chat('effect give @s minecraft:slow_falling 20 0 true', 0.3)
            s.chat(f'tp @s {x + 0.5} {y + 5} {z - 0.5} 0 90', 1.0)
            s.shot('snow-angel')
            s.chat('effect clear @s', 0.3)
    finally:
        s.io.button(3, False)
        s.io.key_up('Shift_L')
        s.chat('kill @e[tag=n131fun]', 0.3)
        for name in stats:
            s.chat(f'scoreboard objectives remove {name}', 0.2)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.4)
        _subtitles(s, False)


# ---------------------------------------------------------------- Simple Fun: small delights while eating
FOODS = [('cookie', 1.6), ('apple', 1.6), ('carrot', 1.6), ('melon_slice', 1.6), ('honey_bottle', 2.0), ('bread', 1.6),
         ('sweet_berries', 1.6)]


def fun_delights(s):
    _prep(s, 'creative')
    ok, _ = _subtitles(s, True)
    s.check('subtitles switched on through the real Options/Accessibility screen', ok)
    x, y, z = _arena(s, 60)
    tags = []
    try:
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 -40', 1.0)
        back = _third_person(s, front=True)
        try:
            for i, (food, secs) in enumerate(FOODS):
                obj = f'n131f{i}'
                _stat(s, obj, f'minecraft.used:minecraft.{food}')
                if not _hand(s, 0, f'minecraft:{food} 4', food):
                    continue
                s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 -40', 0.8)
                hit = s.st().get('hitBlock')
                s.check(f'{food}: the crosshair is on the sky (nothing planted or placed)', hit is None, str(hit))
                s.io.button(3, True)
                try:
                    time.sleep(secs + 0.05)
                    s.shot(f'delight-{food}')
                finally:
                    s.io.button(3, False)
                _say(s, f'if score @s {obj} matches 1..', f'ate-{food}')
                tags.append(f'ate-{food}')
                s.chat(f'scoreboard objectives remove {obj}', 0.3)
                time.sleep(4.0)  # shared cosmetic cooldown 100 ticks
            _check_said(s, 'small delights: every food eaten with the real held button (used stats)', tags)

            # piggy: cooked porkchop -> the piggy effect, pig head in the front view
            if _hand(s, 0, 'minecraft:cooked_porkchop 2', 'porkchop'):
                s.io.button(3, True)
                try:
                    time.sleep(1.7)
                finally:
                    s.io.button(3, False)
                time.sleep(0.6)
                _say(s, 'if entity @s[nbt={active_effects:[{id:"simplefun:piggy_effect"}]}]', 'piggy')
                _check_said(s, 'piggy: a cooked porkchop gives the piggy effect', ['piggy'])
                s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.0)
                s.shot('piggy-head')
                s.chat('effect clear @s simplefun:piggy_effect', 0.3)

            # flower sniff: a poppy in the hand, real Shift held
            time.sleep(4.0)
            if _hand(s, 0, 'minecraft:poppy', 'flower'):
                s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.0)
                s.io.key_down('Shift_L')
                try:
                    time.sleep(1.0)
                    s.check('flower sniff: the client sneaks (real Shift held)', bool(s.st().get('sneaking')), str(s.last.get('sneaking')))
                    s.shot('flower-sniff')
                finally:
                    s.io.key_up('Shift_L')
        finally:
            back()
    finally:
        s.io.button(3, False)
        s.io.key_up('Shift_L')
        s.chat('effect clear @s', 0.3)
        s.chat('clear @s', 0.3)
        _subtitles(s, False)


# ---------------------------------------------------------------- Simple Movement
def _still(s, seconds=1.2):
    a = _pos(s)
    time.sleep(seconds)
    b = _pos(s)
    return a, b


def movement_autowalk(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 80, x0=-2, x1=2, z0=-2, z1=44, height=4)
    try:
        s.chat('simplemovement autowalk true', 1.2)

        def start():
            s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
            return _pos(s)

        a = start()
        s.check('autowalk: known start position', a == [x, y, z], f'{a} vs {[x, y, z]}')
        s.key('Up', 0.2)
        time.sleep(3.0)
        b = _pos(s)
        walked = b[2] - a[2]
        s.check('autowalk: REAL arrow up (default key) -> the player walks on its own (>= 5 blocks in 3 s)', walked >= 5,
                f'{a} -> {b} ({walked} blocks; 0 = key not on arrow up or server switch not synced)')
        s.shot('autowalk-running')
        s.key('Up', 0.2)
        time.sleep(0.6)
        p1, p2 = _still(s)
        s.check('autowalk: arrow up again stops it', p1 == p2, f'{p1} then {p2}')

        a = start()
        s.key('Up', 0.2)
        time.sleep(1.0)
        s.key('e', 1.0)
        scr = s.st().get('screen', 'none')
        s.close()
        p1, p2 = _still(s)
        s.check('autowalk: opening a screen (REAL E, inventory) stops it', p1 == p2 and p1[2] > a[2], f'screen {scr}; {a} -> {p1} then {p2}')

        s.chat('simplemovement autowalk false', 1.2)
        a = start()
        s.key('Up', 0.2)
        time.sleep(2.0)
        b = _pos(s)
        s.check('autowalk: with the server switch off the key does nothing', a == b, f'{a} -> {b}')
    finally:
        s.chat('simplemovement autowalk false', 0.5)
        s.chat('gamemode creative @s', 0.4)


def movement_step(s):
    _prep(s, 'survival')
    ok, was = _menu_toggle(s, ['Controls'], 'Auto-Jump', False)
    s.check('Auto-Jump switched OFF through the real Options/Controls screen (label read back)', ok, f'was {"ON" if was else "OFF" if was is not None else "?"}')
    x, y, z = _arena(s, 100, x0=-3, x1=3, z0=-2, z1=26, height=5)
    s.chat('gamemode survival @s', 0.4)
    # 1-block stone strip z+3..z+11, 1.5-block fence strip z+15..z+23 (9 deep: 1.6 s of walking ~7 blocks ends on the strip)
    s.chat(f'fill {x - 3} {y} {z + 3} {x + 3} {y} {z + 11} minecraft:stone', 0.5)
    s.chat(f'fill {x - 3} {y} {z + 15} {x + 3} {y} {z + 23} minecraft:oak_fence', 0.5)

    def attempt(label, boots, wall_z, expect_up, effect=False):
        s.chat('effect clear @s', 0.3)
        if effect:
            s.chat('effect give @s minecraft:jump_boost 60 1 true', 0.4)
        s.chat(f'item replace entity @s armor.feet with {boots}' if boots else 'item replace entity @s armor.feet with minecraft:air', 0.4)
        s.chat(f'tp @s {x + 0.5} {y} {z + wall_z - 2.5} 0 0', 1.2)
        start = _pos(s)
        s.io.key_down('w')
        try:
            time.sleep(1.6)
        finally:
            s.io.key_up('w')
        time.sleep(0.6)
        end = _pos(s)
        up = end[1] >= y + 1
        s.check(f'step: {label}', up == expect_up and start[1] == y,
                f'start {start}, end y {end[1]} (floor {y}) at z+{end[2] - z} (strip starts z+{wall_z}), expected {"up" if expect_up else "blocked"}')
        return up

    try:
        attempt('no boots, Auto-Jump off -> blocked by the 1-block stone step', None, 3, False)
        if attempt('Jump Boost (jumpBoostStepUp) -> walks up the 1-block step without jumping', None, 3, True, effect=True):
            back = _third_person(s)
            s.shot('jump-boost-on-step')
            back()
        b1 = 'minecraft:diamond_boots[enchantments={"simplemovement:step_height":1}]'
        b2 = 'minecraft:diamond_boots[enchantments={"simplemovement:step_height":2}]'
        if attempt('Step Height I -> walks up the 1-block step', b1, 3, True):
            back = _third_person(s)
            s.shot('step-height-1-on-step')
            back()
        attempt('Step Height I -> blocked by the 1.5-block fence strip', b1, 15, False)
        if attempt('Step Height II -> walks up the 1.5-block fence strip', b2, 15, True):
            back = _third_person(s)
            s.shot('step-height-2-on-fence')
            back()
    finally:
        s.io.key_up('w')
        s.chat('effect clear @s', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.4)
        if was:
            _menu_toggle(s, ['Controls'], 'Auto-Jump', True)


SCENARIOS = {
    'n131-fun-toys': fun_toys,
    'n131-fun-delights': fun_delights,
    'n131-movement-autowalk': movement_autowalk,
    'n131-movement-step': movement_step,
}
