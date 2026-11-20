"""N122 release polish, mods 11-13 (offen-vor-release.md section 3, "Szenen" of Simple Hammers, Simple Mobs, Simple Animals), word for word:

Simple Hammers  "Ladeanzeige, Armhaltung, Rosé-Optik (W4C-22)."
  n122-hammers-charge   "Ladeanzeige": an upgrade has no bar or text, its charge shows as cracks in the block (stage 2/4/6/8 after strikes
                        1-4, SledgehammerProgress; gametest upgrade_charge_cracks_and_pose). Copper Chest + Netherite Sledgehammer + Cracked
                        Diamond in the off hand, crosshair read back on the chest, REAL right click held: shots after strikes 1..2, let go
                        (chest still a Copper Chest, shot: cracks stay), hold again until it turns into a Reinforced Chest (read back with
                        '/execute if block'). Then the reshape charge (sneak + hold, the bow-like draw): shots at 0.5 s / 1 s, block changed.
  n122-hammers-arm      "Armhaltung": the same upgrade on a second Copper Chest seen from the front (F5 twice): shots while drawing back
                        (arm over the head) and at the strike, then first person between two strikes (hammer tipped back). Holding the hammer
                        without an upgrade: normal pose (front shot). Judgement of the look = owner (W4C-22).
  n122-hammers-rose     "Rosé-Optik": Rose Copper and Rose Gold Sledgehammer (only with Simple Ores; texture = the gold hammer recoloured at
                        build time) in the hotbar (ids checked), each selected: first-person shot, front shot; both in item frames next to
                        the Gold Sledgehammer for comparison (frame contents read back). Judgement = owner (W4C-22).
Simple Mobs  "Howler, Trompete, Fear/Wither/Disguise (W4C-30)."   Hostile mobs never attack the camera player: creative mode + NoAI.
  n122-mobs-howler      Hollow Howler (NoAI, Silent off) in front and from the side; crosshair on it reads simplemobs:hollow_howler.
  n122-mobs-trumpet     Trumpet in the hand, REAL right click held looking 15 deg down (lowest note) and 15 deg up (highest note), front view
                        shots (note particles over the head; the tone itself is the owner's listening, W4C-29 / n122-klang.html).
  n122-mobs-fear        Dread and Hollow Watcher (NoAI) side by side, crosshair read back on each; shots front + close.
  n122-mobs-wither      Blighted Wither, Abyssal Wither and a Vanilla Wither (NoAI, mobGriefing off) in a row; all three present; shots.
  n122-mobs-disguise    Cloak of Deception on the head, empty hand, crosshair read back on a cow (NoAI), REAL right click: a disguise
                        marker appears (the player is a cow), front shot; a REAL left click into the air ends it (marker gone), shot.
Simple Animals  "alle Tiere/Varianten, Biom-Varianten (W4C-06)."
  n122-animals-all      all six kinds, every coat variant (stored Variant index), both crab models (Model 0/1 in both shells) and a baby
                        of each breedable kind (NoAI); every one read back by its NBT; overview + close-up shots with F1.
  n122-animals-biomes   four biome patches made with /fillbiome (desert, snowy taiga, swamp, beach); 12 animals per kind summoned
                        WITHOUT NBT (the spawn picks the coat from the biome: home coat weight 8, any other 1), then NoAI; the home coat
                        must be the most common one per patch (desert: sandy boar, desert hedgehog, fallow deer; snowy taiga: frost boar,
                        white-breasted hedgehog, red deer; swamp: rust boar, four-toed hedgehog; beach: coral crab). Counts from the
                        '/execute if entity' feedback in client.log. Shot per patch.

Patterns (uiprobe_n119/n121): position via int(float()) of status 'pos'; gamerules advance_time / spawn_mobs; block and entity states read
with commands ('say N122 <tag>' in client.log); real clicks only after the crosshair reads the target back (the tp-facing turn has
settled). No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe122 --scale 2 \
        --only n122-hammers-charge,n122-hammers-arm,n122-hammers-rose,n122-mobs-howler,n122-mobs-trumpet,n122-mobs-fear,n122-mobs-wither,n122-mobs-disguise,n122-animals-all,n122-animals-biomes
"""
import re
import time


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say_if(s, cond, tag):
    """cond: the part after 'execute' (e.g. 'if block 1 2 3 minecraft:stone')."""
    s.chat(f'execute {cond} run say N122 {tag}', 0.35)


def _said(s, tags, settle=1.5):
    time.sleep(settle)
    log = _log(s)
    return {t: bool(re.search(r'N122 ' + re.escape(t) + r'(?![\w-])', log)) for t in tags}


def _check_said(s, name, tags):
    got = _said(s, tags)
    miss = [t for t, ok in got.items() if not ok]
    s.check(name, not miss, f'{len(tags) - len(miss)}/{len(tags)}' + (f' missing {miss[:12]}' if miss else ''))
    return not miss


def _counts(s, queries):
    """{tag: selector}: 'say N122 COUNT <tag>' then 'execute if entity <selector>'; the count is the next feedback line in client.log."""
    for tag, sel in queries.items():
        s.chat(f'say N122 COUNT {tag}', 0.3)
        s.chat(f'execute if entity {sel}', 0.35)
    time.sleep(1.5)
    lines = _log(s).splitlines()
    out = {}
    for tag in queries:
        idx = max((i for i, ln in enumerate(lines) if re.search(r'N122 COUNT ' + re.escape(tag) + r'$', ln.strip())), default=-1)
        n = None
        for ln in lines[idx + 1: idx + 6] if idx >= 0 else []:
            m = re.search(r'Test passed, count: (\d+)', ln)
            if m:
                n = int(m.group(1))
                break
            if 'Test failed' in ln:
                n = 0
                break
        out[tag] = n
    return out


def _prep(s, mode='creative'):
    s.close()
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('difficulty normal', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'mob_griefing false', 'send_command_feedback true'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('kill @e[tag=n122]', 0.3)


def _aim(s, bx, by, bz, want, tag, tries=4):
    """Turns onto the block and waits until the crosshair reads it back (the turn has settled). True when it does."""
    hit = None
    for _ in range(tries):
        st = s.face(bx, by, bz)
        for _ in range(6):
            hit = st.get('hitBlock')
            if hit == want:
                return True
            time.sleep(0.3)
            st = s.st()
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _aim_entity(s, ex, ey, ez, want, tag, tries=4):
    """Looks at a point of an entity and waits until the crosshair reads the entity type back."""
    hit = None
    for _ in range(tries):
        x, y, z = _pos(s)
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {ex} {ey} {ez}', 0.8)
        time.sleep(0.5)
        for _ in range(6):
            hit = s.st().get('hitEntity')
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _view(s, f, t, wait=1.2):
    """Stand at f (feet) and look at point t."""
    s.chat(f'tp @s {f[0]} {f[1]} {f[2]} facing {t[0]} {t[1]} {t[2]}', wait)


def _front(s):
    """First person -> third person front (F5 twice)."""
    s.key('F5', 0.5)
    s.key('F5', 0.8)


def _first(s):
    """Third person front -> first person (F5 once)."""
    s.key('F5', 0.6)


def _clear_area(s, x, y, z, r=6, h=5):
    s.chat(f'fill {x - r} {y} {z - r} {x + r} {y + h} {z + r} minecraft:air', 0.4)
    s.chat(f'fill {x - r} {y - 1} {z - r} {x + r} {y - 1} {z + r} minecraft:smooth_stone', 0.4)


# ---------------------------------------------------------------- Simple Hammers
def _upgrade_kit(s):
    s.chat('item replace entity @s weapon.mainhand with simplehammers:netherite_sledgehammer', 0.3)
    s.chat('item replace entity @s weapon.offhand with simplelib:cracked_diamond 8', 0.3)


def hammers_charge(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z)
    c = (x, y, z + 2)
    s.chat(f'setblock {c[0]} {c[1]} {c[2]} minecraft:copper_chest[facing=north]', 0.5)
    _upgrade_kit(s)
    st = s.st()
    s.check('Netherite Sledgehammer in the main hand', (st.get('inventory') or [''])[0].startswith('simplehammers:netherite_sledgehammer'),
            (st.get('inventory') or [''])[:1])
    if not _aim(s, *c, 'minecraft:copper_chest', 'charge'):
        return
    s.shot('charge-0-aimed')
    # strikes fall every 20 ticks (Netherite); cracks after strike 1 (stage 2), 2 (stage 4) ...
    s.io.button(3, True)
    try:
        time.sleep(1.3)
        s.shot('charge-1-after-strike-1')   # the shot takes about a second: the hold ends after 2-3 of 5 strikes
    finally:
        s.io.button(3, False)
    time.sleep(0.8)
    _say_if(s, f'if block {c[0]} {c[1]} {c[2]} minecraft:copper_chest', 'chest-kept')
    _check_said(s, 'let go before the fifth strike: still a Copper Chest (no upgrade yet)', ['chest-kept'])
    s.shot('charge-3-released-cracks-stay')
    # hold again: the upgrade continues from the stored strikes and ends with a Reinforced Chest
    done = False
    if _aim(s, *c, 'minecraft:copper_chest', 'charge again'):
        s.io.button(3, True)
        try:
            end = time.time() + 9
            while time.time() < end:
                time.sleep(0.5)
                if s.st().get('hitBlock') == 'simplestorage:reinforced_chest':
                    done = True
                    break
        finally:
            s.io.button(3, False)
    time.sleep(0.8)
    _say_if(s, f'if block {c[0]} {c[1]} {c[2]} simplestorage:reinforced_chest', 'chest-upgraded')
    _check_said(s, 'holding again finishes the upgrade: Copper Chest -> Reinforced Chest', ['chest-upgraded'])
    s.check('crosshair saw the Reinforced Chest within 9 s of holding again', done, s.last.get('hitBlock'))
    s.shot('charge-4-reinforced')
    # reshape charge: sneak + hold right click on stone, the hammer draws back like a bow
    s.chat('item replace entity @s weapon.offhand with minecraft:air', 0.3)
    r = (x + 2, y, z)
    s.chat(f'setblock {r[0]} {r[1]} {r[2]} minecraft:stone', 0.5)
    if _aim(s, *r, 'minecraft:stone', 'reshape'):
        s.io.key_down('Shift_L')
        s.io.button(3, True)
        try:
            time.sleep(0.5)
            s.shot('reshape-0.5s')
            time.sleep(0.2)
            s.shot('reshape-1s')
            end = time.time() + 8
            while time.time() < end and s.st().get('hitBlock') == 'minecraft:stone':
                time.sleep(0.4)
        finally:
            s.io.button(3, False)
            s.io.key_up('Shift_L')
        got = s.st().get('hitBlock')
        s.check('sneak + holding right click reshaped the stone (crosshair block changed)', got != 'minecraft:stone', got)
        s.shot('reshape-done')
    s.chat(f'fill {x - 6} {y} {z - 6} {x + 6} {y + 5} {z + 6} minecraft:air', 0.4)


def hammers_arm(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z)
    s.chat('item replace entity @s weapon.mainhand with simplehammers:netherite_sledgehammer', 0.3)
    _front(s)
    s.shot('arm-idle-front')
    _first(s)
    c = (x, y, z + 2)
    s.chat(f'setblock {c[0]} {c[1]} {c[2]} minecraft:copper_chest[facing=north]', 0.5)
    _upgrade_kit(s)
    if not _aim(s, *c, 'minecraft:copper_chest', 'arm'):
        return
    _front(s)
    s.io.button(3, True)
    try:
        time.sleep(0.55)
        s.shot('arm-front-drawing-back')
        time.sleep(0.1)
        s.shot('arm-front-later')
    finally:
        s.io.button(3, False)
    _first(s)
    time.sleep(0.8)
    _say_if(s, f'if block {c[0]} {c[1]} {c[2]} minecraft:copper_chest', 'arm-chest-kept')
    _check_said(s, 'a short hold (front view) leaves the Copper Chest as it is', ['arm-chest-kept'])
    # first person on a second chest (the first one keeps its strikes, a further hold could finish it)
    c2 = (x + 2, y, z + 2)
    s.chat(f'setblock {c2[0]} {c2[1]} {c2[2]} minecraft:copper_chest[facing=north]', 0.5)
    if _aim(s, *c2, 'minecraft:copper_chest', 'arm first person'):
        s.io.button(3, True)
        try:
            time.sleep(0.6)
            s.shot('arm-first-person-drawn-back')
            time.sleep(0.1)
            s.shot('arm-first-person-later')
        finally:
            s.io.button(3, False)
    time.sleep(0.6)
    s.chat(f'fill {x - 6} {y} {z - 6} {x + 6} {y + 5} {z + 6} minecraft:air', 0.4)


ROSE = ['simplehammers:rose_bronze_sledgehammer', 'simplehammers:rose_gold_sledgehammer']


def hammers_rose(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z)
    for i, item in enumerate(ROSE + ['simplehammers:gold_sledgehammer']):
        s.chat(f'item replace entity @s hotbar.{i} with {item}', 0.3)
    inv = s.st().get('inventory', [])
    miss = [it for i, it in enumerate(ROSE) if not (len(inv) > i and inv[i].startswith(it))]
    s.check('Rose Copper and Rose Gold Sledgehammer in the hotbar (registered with Simple Ores)', not miss, f'missing {miss}' if miss else inv[:3])
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} facing {x + 0.5} {y + 1.2} {z + 5.5}', 0.8)
    for i, name in enumerate(['rose-copper', 'rose-gold', 'gold']):
        s.key(str(i + 1), 0.6)
        s.shot(f'hand-{name}')
    for i, name in enumerate(['rose-copper', 'rose-gold']):
        s.key(str(i + 1), 0.4)
        _front(s)
        s.shot(f'front-{name}')
        _first(s)
    # item frames on a wall: rose copper, rose gold, gold (comparison)
    s.chat(f'fill {x - 2} {y} {z + 3} {x + 2} {y + 2} {z + 3} minecraft:white_concrete', 0.4)
    for dx, item in zip((-1, 0, 1), ROSE + ['simplehammers:gold_sledgehammer']):
        s.chat(f'summon item_frame {x + dx} {y + 1} {z + 2} {{Facing:2b,Fixed:1b,Tags:["n122"],Item:{{id:"{item}",count:1}}}}', 0.4)
    for item in ROSE:
        _say_if(s, f'if entity @e[type=item_frame,tag=n122,nbt={{Item:{{id:"{item}"}}}}]', 'frame-' + item.split(':')[1])
    _check_said(s, 'both rose hammers sit in item frames', ['frame-' + it.split(':')[1] for it in ROSE])
    s.key('F1', 0.4)
    try:
        _view(s, (x + 0.5, y, z - 0.5), (x + 0.5, y + 1.5, z + 2.5))
        s.shot('frames-rose-copper-rose-gold-gold')
    finally:
        s.key('F1', 0.4)
    s.chat('kill @e[tag=n122]', 0.3)
    s.chat(f'fill {x - 6} {y} {z - 6} {x + 6} {y + 5} {z + 6} minecraft:air', 0.4)


# ---------------------------------------------------------------- Simple Mobs
def _summon(s, kind, x, y, z, extra=''):
    s.chat(f'summon {kind} {x} {y} {z} {{NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],Tags:["n122"]{extra}}}', 0.5)


def mobs_howler(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z, r=8)
    _summon(s, 'simplemobs:hollow_howler', x + 0.5, y, z + 5.5)
    time.sleep(1.0)
    _say_if(s, 'if entity @e[type=simplemobs:hollow_howler,tag=n122]', 'howler-there')
    _check_said(s, 'Hollow Howler stands in the scene (NoAI, creative: no attack)', ['howler-there'])
    if _aim_entity(s, x + 0.5, y + 1.2, z + 5.5, 'simplemobs:hollow_howler', 'howler'):
        s.check('crosshair reads the Hollow Howler', True, 'simplemobs:hollow_howler')
    s.key('F1', 0.4)
    try:
        _view(s, (x + 0.5, y + 0.5, z - 1.5), (x + 0.5, y + 1.4, z + 5.5))
        s.shot('howler-front')
        _view(s, (x + 6.5, y + 0.5, z + 5.5), (x + 0.5, y + 1.4, z + 5.5))
        s.shot('howler-side')
    finally:
        s.key('F1', 0.4)
    s.chat('kill @e[tag=n122]', 0.3)


def mobs_trumpet(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z)
    s.chat('item replace entity @s weapon.mainhand with simplemobs:trumpet', 0.3)
    inv = s.st().get('inventory', [])
    s.check('Trumpet in the main hand', bool(inv) and inv[0].startswith('simplemobs:trumpet'), inv[:1])
    for pitch, name in ((15, 'low-looking-down'), (-15, 'high-looking-up')):
        s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 {pitch}', 1.0)
        got = s.st().get('pitch')
        s.check(f'look angle {pitch} deg ({name})', got is not None and abs(float(got) - pitch) < 1.5, got)
        _front(s)
        s.io.button(3, True)
        try:
            time.sleep(1.2)
            s.shot(f'trumpet-{name}')
        finally:
            s.io.button(3, False)
        _first(s)
        time.sleep(0.5)


def mobs_fear(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z, r=8)
    _summon(s, 'simplemobs:dread', x - 1.5, y, z + 5.5)
    _summon(s, 'simplemobs:hollow_watcher', x + 2.5, y, z + 5.5)
    time.sleep(1.0)
    _say_if(s, 'if entity @e[type=simplemobs:dread,tag=n122]', 'dread-there')
    _say_if(s, 'if entity @e[type=simplemobs:hollow_watcher,tag=n122]', 'watcher-there')
    _check_said(s, 'Dread and Hollow Watcher stand in the scene (NoAI)', ['dread-there', 'watcher-there'])
    if _aim_entity(s, x - 1.5, y + 1.2, z + 5.5, 'simplemobs:dread', 'dread'):
        s.check('crosshair reads the Dread', True, 'simplemobs:dread')
    if _aim_entity(s, x + 2.5, y + 1.2, z + 5.5, 'simplemobs:hollow_watcher', 'watcher'):
        s.check('crosshair reads the Hollow Watcher', True, 'simplemobs:hollow_watcher')
    s.key('F1', 0.4)
    try:
        _view(s, (x + 0.5, y + 0.5, z - 2.5), (x + 0.5, y + 1.4, z + 5.5))
        s.shot('fear-both')
        _view(s, (x - 1.5, y + 0.5, z + 1.5), (x - 1.5, y + 1.6, z + 5.5))
        s.shot('fear-dread-close')
        _view(s, (x + 2.5, y + 0.5, z + 1.5), (x + 2.5, y + 1.6, z + 5.5))
        s.shot('fear-watcher-close')
    finally:
        s.key('F1', 0.4)
    s.chat('kill @e[tag=n122]', 0.3)


def mobs_wither(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z, r=12, h=8)
    kinds = [('simplemobs:blighted_wither', -5), ('simplemobs:abyssal_wither', 0), ('minecraft:wither', 5)]
    for kind, dx in kinds:
        _summon(s, kind, x + dx + 0.5, y, z + 9.5)
    time.sleep(1.5)
    for kind, _ in kinds:
        _say_if(s, f'if entity @e[type={kind},tag=n122]', 'there-' + kind.split(':')[1])
    _check_said(s, 'Blighted, Abyssal and Vanilla Wither stand in a row (NoAI, mobGriefing off)', ['there-' + k.split(':')[1] for k, _ in kinds])
    s.key('F1', 0.4)
    try:
        _view(s, (x + 0.5, y + 1.5, z - 3.5), (x + 0.5, y + 2.5, z + 9.5))
        s.shot('withers-row')
        _view(s, (x - 4.5, y + 1.5, z + 4.5), (x - 4.5, y + 2.5, z + 9.5))
        s.shot('blighted-close')
        _view(s, (x + 0.5, y + 1.5, z + 4.5), (x + 0.5, y + 2.5, z + 9.5))
        s.shot('abyssal-close')
    finally:
        s.key('F1', 0.4)
    s.chat('kill @e[tag=n122]', 0.3)


def mobs_disguise(s):
    _prep(s)
    x, y, z = _pos(s)
    _clear_area(s, x, y, z)
    s.chat('item replace entity @s armor.head with simplemobs:deceiver_cloak', 0.3)
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    _summon(s, 'minecraft:cow', x + 0.5, y, z + 3.5)
    _say_if(s, 'if items entity @s armor.head simplemobs:deceiver_cloak', 'cloak-worn')
    _check_said(s, 'Cloak of Deception sits on the head slot', ['cloak-worn'])
    _front(s)
    s.shot('cloak-worn-front')
    _first(s)
    if not _aim_entity(s, x + 0.5, y + 0.8, z + 3.5, 'minecraft:cow', 'cow'):
        return
    s.io.right_click()
    time.sleep(1.0)
    _say_if(s, 'if entity @e[type=simplemobs:disguise_marker,distance=..3]', 'disguised')
    if _check_said(s, 'real right click with an empty hand on the cow: disguised (marker follows the player)', ['disguised']):
        _front(s)
        s.shot('disguised-as-cow-front')
        _first(s)
        # any swing ends the disguise: a real left click into the sky
        _view(s, (x + 0.5, y, z + 0.5), (x + 0.5, y + 40, z + 3.5), 1.0)
        s.io.button(1, True)
        time.sleep(0.15)
        s.io.button(1, False)
        time.sleep(1.0)
        n = _counts(s, {'marker-after-swing': '@e[type=simplemobs:disguise_marker,distance=..3]'})['marker-after-swing']
        s.check('a real left click into the air ends the disguise (no marker left)', n == 0, n)
        _front(s)
        s.shot('disguise-ended-front')
        _first(s)
    s.chat('kill @e[tag=n122]', 0.3)
    s.chat('clear @s', 0.3)


# ---------------------------------------------------------------- Simple Animals
KINDS = [('deer', 2), ('boar', 4), ('hedgehog', 4), ('crab', 2), ('zebra', 0), ('zorse', 0)]
BABIES = ['deer', 'boar', 'hedgehog', 'zebra']


def animals_all(s):
    _prep(s)
    x, y, z = _pos(s)
    s.chat(f'fill {x - 8} {y} {z - 4} {x + 8} {y + 4} {z + 34} minecraft:air', 0.5)
    s.chat(f'fill {x - 8} {y - 1} {z - 4} {x + 8} {y - 1} {z + 34} minecraft:grass_block', 0.5)
    expect = {}
    for row, (kind, variants) in enumerate(KINDS):
        rz = z + 6 + 4 * row
        for v in range(max(1, variants)):
            extra = f',Silent:1b,Variant:{v}' if variants else ',Silent:1b'
            _summon(s, f'simpleanimals:{kind}', x - 3.5 + 2.5 * v, y, rz + 0.5, extra)
            tag = f'{kind}-v{v}'
            expect[tag] = f'@e[type=simpleanimals:{kind},tag=n122' + (f',nbt={{Variant:{v}}}' if variants else '') + ']'
    # fiddler crab: model 1 in both shells, one row behind the crabs
    crab_z = z + 6 + 4 * 3 + 2
    for v in range(2):
        _summon(s, 'simpleanimals:crab', x + 3.5 + 2 * v, y, crab_z + 0.5, f',Silent:1b,Variant:{v},Model:1')
        expect[f'fiddler-v{v}'] = f'@e[type=simpleanimals:crab,tag=n122,nbt={{Variant:{v},Model:1}}]'
    # babies, one row in front
    for i, kind in enumerate(BABIES):
        s.chat(f'summon simpleanimals:{kind} {x - 3.5 + 2.5 * i} {y} {z + 3.5} '
               f'{{NoAI:1b,PersistenceRequired:1b,Silent:1b,Rotation:[180f,0f],Age:-24000,Tags:["n122","n122baby"]}}', 0.5)
    time.sleep(1.5)
    got = _counts(s, expect)
    miss = [t for t, n in got.items() if not n]
    s.check(f'all kinds, coats and both crab models in both shells stand in the scene ({len(expect)} groups)', not miss,
            f'missing {miss}' if miss else f'{len(expect)}/{len(expect)}')
    for kind in BABIES:
        _say_if(s, f'as @e[type=simpleanimals:{kind},tag=n122baby,limit=1] if predicate '
                   '{condition:"minecraft:entity_properties",entity:"this",predicate:{flags:{is_baby:true}}}', 'baby-' + kind)
    _check_said(s, 'a baby of every breedable kind (deer, boar, hedgehog, zebra)', ['baby-' + k for k in BABIES])
    s.key('F1', 0.4)
    try:
        _view(s, (x + 0.5, y + 8, z - 4.5), (x + 0.5, y, z + 16.5), 1.5)
        s.shot('overview')
        _view(s, (x - 0.5, y + 1.6, z), (x - 0.5, y, z + 4), 1.5)
        s.shot('babies')
        _view(s, (x + 0.5, y + 1.5, z + 12.5), (x + 0.5, y, z + 19.5), 1.5)
        s.shot('hedgehogs-crabs-fiddlers')
        _view(s, (x + 0.5, y + 1.2, z + 1.5), (x + 0.5, y + 0.8, z + 10.5), 1.5)
        s.shot('deer-and-boars')
        _view(s, (x - 2.5, y + 1.2, z + 22.5), (x - 3, y + 0.8, z + 31), 1.5)
        s.shot('zebra-and-zorse')
    finally:
        s.key('F1', 0.4)
    s.chat('kill @e[tag=n122]', 0.3)


# biome, centre offset, {kind: (home index, number of coats, coat name)}
BIOMES = [
    ('minecraft:desert', -60, {'boar': (1, 4, 'sandy'), 'hedgehog': (1, 4, 'desert'), 'deer': (1, 2, 'fallow')}),
    ('minecraft:snowy_taiga', -20, {'boar': (2, 4, 'frost'), 'hedgehog': (2, 4, 'whitebreast'), 'deer': (0, 2, 'red')}),
    ('minecraft:swamp', 20, {'boar': (3, 4, 'rust'), 'hedgehog': (3, 4, 'fourtoed')}),
    ('minecraft:beach', 60, {'crab': (1, 2, 'coral')}),
]
PER_KIND = 12


def animals_biomes(s):
    _prep(s)
    x0, y, z0 = _pos(s)
    z = z0 + 40   # away from the other scenes
    for biome, dx, kinds in BIOMES:
        bx = x0 + dx
        tag = 'b122' + biome.split(':')[1]
        s.chat(f'fill {bx - 9} {y} {z - 9} {bx + 9} {y + 4} {z + 9} minecraft:air', 0.4)
        s.chat(f'fill {bx - 9} {y - 1} {z - 9} {bx + 9} {y - 1} {z + 9} minecraft:grass_block', 0.4)
        s.chat(f'fillbiome {bx - 12} {y - 4} {z - 12} {bx + 12} {y + 8} {z + 12} {biome}', 1.0)
        for row, kind in enumerate(kinds):
            for i in range(PER_KIND):
                # no NBT: the spawn runs finalizeSpawn and picks the coat from the biome
                s.chat(f'summon simpleanimals:{kind} {bx - 6.5 + i} {y} {z - 5.5 + 4 * row}', 0.25)
            s.chat(f'tag @e[type=simpleanimals:{kind},x={bx - 9},y={y - 1},z={z - 9},dx=18,dy=6,dz=18,tag=!n122] add {tag}', 0.3)
            s.chat(f'tag @e[tag={tag}] add n122', 0.3)
            s.chat(f'execute as @e[tag={tag}] run data merge entity @s {{NoAI:1b,Silent:1b}}', 0.4)
        time.sleep(1.0)
        for kind, (home, coats, name) in kinds.items():
            q = {f'{tag}-{kind}-v{v}': f'@e[type=simpleanimals:{kind},tag={tag},nbt={{Variant:{v}}}]' for v in range(coats)}
            n = _counts(s, q)
            vals = [n[f'{tag}-{kind}-v{v}'] or 0 for v in range(coats)]
            ok = sum(vals) >= PER_KIND - 1 and all(vals[home] > vals[v] for v in range(coats) if v != home)
            s.check(f'{biome}: the home coat {name} is the most common of {PER_KIND} {kind}s', ok, f'counts by coat index {vals}')
        s.key('F1', 0.4)
        try:
            _view(s, (bx + 0.5, y + 5, z - 11.5), (bx + 0.5, y, z + 0.5), 1.5)
            s.shot(biome.split(':')[1])
        finally:
            s.key('F1', 0.4)
        s.chat(f'kill @e[tag={tag}]', 0.4)
    s.chat(f'tp @s {x0 + 0.5} {y} {z0 + 0.5}', 1.5)


SCENARIOS = {
    'n122-hammers-charge': hammers_charge,
    'n122-hammers-arm': hammers_arm,
    'n122-hammers-rose': hammers_rose,
    'n122-mobs-howler': mobs_howler,
    'n122-mobs-trumpet': mobs_trumpet,
    'n122-mobs-fear': mobs_fear,
    'n122-mobs-wither': mobs_wither,
    'n122-mobs-disguise': mobs_disguise,
    'n122-animals-all': animals_all,
    'n122-animals-biomes': animals_biomes,
}
