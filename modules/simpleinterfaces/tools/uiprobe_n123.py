"""N123 release polish, mods 14-16 (offen-vor-release.md section 3, "Szenen" of Simple Dinosaurs, Simple End, Simple Tools), word for word:

Simple Dinosaurs  "Brüten/Schlüpfen/Felle/Hitbox (W4C-15)."
  n123-dinos-brood    Brooding and hatching with REAL input: incubateSeconds/hatchSeconds set to 10 by /simpledinos config (reset at the
                      end). T. rex egg and Triceratops egg set on the floor; empty hand, crosshair on the egg (read back), REAL right click
                      -> the player rides the invisible seat (tag simpledinos_seat); after 3 s the egg is past phase 0, after the brooding
                      time in phase 12..14 (brooded, cracks); sneak (real Shift) stands up; after the hatching time the egg is gone, a
                      hatchling stands next to it (T. rex: no eggshell; Triceratops: eggshell items). Shots: sitting, brooded, hatchling.
  n123-dinos-coats    "Felle" = the five coats (olive, sand, rust, moss, ash; DinoVariants, NBT CoatVariant 0..4): every kind summoned
                      grown up in all five coats (NoAI, so nobody walks into the camera; dinosaurs never attack players anyway), each
                      coat read back with '/execute if entity ...{CoatVariant:n}', one shot per kind.
  n123-dinos-hitbox   Each kind alone, side on: crosshair on the body centre reads back hitEntity = the kind (the core hitbox exists in the
                      client); F3+B hitbox shot; then a stone wall right in front of the head: shot after 2 s (head pulls back /
                      lifts - judged in the picture, the server logic is gametest DinosTests part hitboxes).
                      Raptor sounds: listening sample RAPTOR82B (owner), not touched.
Simple End        "Boote/Schilder/Gemälde, End-Garten, Riesenbäume (W4C-16)."
  n123-end-woodset    Boats: Ashwood/Duskwood boat + chest boat summoned (each read back), crosshair on the Ashwood boat, REAL right click
                      -> the player sits in it; Shift leaves. Signs: Ashwood Sign in hand, REAL right click on the floor -> sign editor
                      (SignEditScreen), "ashwood" typed, Done -> the sign holds the text (data get). Duskwood Hanging Sign, REAL right
                      click on a wall face -> HangingSignEditScreen, "duskwood", Done. End Painting, REAL right click on a 5 x 4 wall ->
                      a painting with a simpleend: variant; two shots 2 s apart (animation).
  n123-end-garden     End garden built by command on End stone: Ash/Dusk buds, End grass, End bushes, Ashwood/Duskwood leaves, burrowed
                      End stone and six Nibblemites (both colourings, NoAI); every plant still stands after 2 s (survives on End stone),
                      both Nibblemite variants exist. Bone meal (REAL right click) on a lone End grass spreads tufts (count > 1). Shots.
  n123-end-trees      Gigantic End trees with REAL bone-meal clicks (light 15, outside the End): one Ash Bud -> big tree (Ashwood log at
                      +14), 2x2 Dusk Buds -> huge tree (log at +22 in one trunk column), 4x4 Ash Buds -> colossal tree (log at +37);
                      shot of each and an overview. N89D (owner): bud textures (Chorus chalice, b-n89c in Production) are judged by the
                      owner in these shots, not by a check.
Simple Tools      "Biom-Pinsel, 22 Eimer, Todesvorrat (W4C-47)."
  n123-tools-brush    Biome Brush in the main hand, Sand in the off hand, crosshair on a grass block, REAL right click -> the clicked
                      cell is desert, a cell 16 blocks away stays plains; sneak + REAL click with Snow Block -> only the clicked cell
                      snowy plains (its neighbour cell 4 blocks away still desert). Shots before/after (grass colour).
  n123-tools-buckets  all 22 buckets (13 ceramic incl. raw/chipped/cracked/brittle with water/lava, 9 Enderite incl. the full ones) in
                      the inventory (each read back), survival inventory open, REAL pointer on a full Enderite bucket and on a brittle
                      ceramic bucket (tooltip shots).
  n123-tools-vault    Death with items (survival, keepInventory off), Respawn by REAL click, Death Vault set and opened with a REAL right
                      click: DeathVaultScreen, title "Death Vault", 46 own slots (45 fields + offering), the dropped items shown; shot.

Patterns (uiprobe_n121): position via int(float()) of status 'pos'; gamerules advance_time / spawn_mobs; blocks set and read with commands;
real clicks only after the crosshair target was read back (the tp-facing turn has settled) and inside screens. No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe123 --scale 2 \
        --only n123-dinos-brood,n123-dinos-coats,n123-dinos-hitbox,n123-end-woodset,n123-end-garden,n123-end-trees,n123-tools-brush,n123-tools-buckets,n123-tools-vault
"""
import re
import time

TAG = 'N123'


# ---------------------------------------------------------------- helpers (every uiprobe file carries its own copy)
def _pos(s):
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _plain(label):
    return re.sub('§.', '', label or '')


def _log(s):
    p = s.out / 'client.log'
    return p.read_text(errors='replace') if p.exists() else ''


def _say(s, cond, tag):
    """cond = the part between 'execute' and 'run', e.g. 'if block 1 2 3 minecraft:stone'."""
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


def _count(s, tag, box, block):
    s.chat(f'say {TAG} COUNT {tag}', 0.35)
    s.chat(f'fill {box} minecraft:air replace {block}', 0.5)


def _counts(s, tags):
    time.sleep(1.5)
    lines = _log(s).splitlines()
    out = {}
    for t in tags:
        idx = max((i for i, ln in enumerate(lines) if re.search(TAG + r' COUNT ' + re.escape(t) + r'$', ln.strip())), default=-1)
        n = None
        for ln in lines[idx + 1: idx + 8] if idx >= 0 else []:
            m = re.search(r'(?:Successfully filled|filled) (\d+) block', ln)
            if m:
                n = int(m.group(1))
                break
            if 'No blocks were filled' in ln:
                n = 0
                break
        out[t] = n
    return out


def _after(s, marker, pattern):
    """First regex match of pattern in the log lines after the last 'N123 MARK <marker>' line."""
    time.sleep(1.5)
    lines = _log(s).splitlines()
    idx = max((i for i, ln in enumerate(lines) if ln.strip().endswith(f'{TAG} MARK {marker}')), default=-1)
    for ln in lines[idx + 1: idx + 12] if idx >= 0 else []:
        m = re.search(pattern, ln)
        if m:
            return m
    return None


def _mark(s, marker):
    s.chat(f'say {TAG} MARK {marker}', 0.35)


def _prep(s, mode='creative'):
    s.close()
    s.chat('ride @s dismount', 0.3)
    s.chat(f'gamemode {mode} @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false', 'send_command_feedback true', 'keep_inventory false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('weather clear', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)


def _move(s, dx):
    """Moves dx blocks east onto fresh flat ground (the scenarios do not share space) and returns the new feet position."""
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + dx + 0.5} {y} {z + 0.5} 0 0', 1.0)
    time.sleep(1.5)
    return _pos(s)


def _wait_screen(s, fragment, seconds=6.0):
    st = s.wait_screen(fragment, seconds)
    return st if fragment in st.get('screen', '') else None


def _park(s, st):
    s.io.move(*s.abs(st, 4, 4))
    time.sleep(0.3)


def _aim(s, bx, by, bz, want, tag, tries=4):
    """Turns onto the block and waits until the crosshair reads it back (the turn has settled)."""
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


def _aim_entity(s, bx, by, bz, want, tag, tries=4):
    hit = None
    for _ in range(tries):
        st = s.face(bx, by, bz)
        for _ in range(6):
            hit = st.get('hitEntity')
            if hit == want:
                return True
            time.sleep(0.3)
            st = s.st()
    s.check(f'{tag}: crosshair on entity {want}', False, hit)
    return False


def _sneak(s, seconds=0.6):
    s.io.key_down('Shift_L')
    try:
        time.sleep(seconds)
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.6)


def _sneak_click(s):
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.4)
        s.io.right_click()
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.6)


def _floor(s, x0, z0, x1, z1, y, block='minecraft:smooth_stone', clear=4):
    s.chat(f'fill {x0} {y} {z0} {x1} {y + clear} {z1} minecraft:air', 0.4)
    s.chat(f'fill {x0} {y - 1} {z0} {x1} {y - 1} {z1} {block}', 0.4)


# ---------------------------------------------------------------- Simple Dinosaurs
KINDS = ['trex', 'triceratops', 'stegosaurus', 'parasaurolophus', 'utahraptor']
COATS = ['olive', 'sand', 'rust', 'moss', 'ash']


def _kill_dinos(s):
    for k in KINDS:
        s.chat(f'kill @e[type=simpledinos:{k}]', 0.25)


def _brood_one(s, kind, x, y, z, shell):
    egg = f'simpledinos:{kind}_egg'
    ex, ey, ez = x, y, z + 2
    s.chat(f'setblock {ex} {ey} {ez} minecraft:air', 0.3)
    s.chat(f'setblock {ex} {ey} {ez} {egg}', 0.5)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 0.6)
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    if not _aim(s, ex, ey, ez, egg, f'{kind} egg'):
        return
    s.io.right_click()
    time.sleep(1.0)
    _say(s, 'on vehicle if entity @s[type=minecraft:armor_stand,tag=simpledinos_seat]', f'{kind}-seated')
    time.sleep(3.0)
    _say(s, f'unless block {ex} {ey} {ez} {egg}[phase=0]', f'{kind}-phase-up')
    _check_said(s, f'{kind}: REAL right click with an empty hand sits on the egg (seat) and brooding runs (phase > 0)',
                [f'{kind}-seated', f'{kind}-phase-up'])
    s.shot(f'{kind}-sitting')  # no tp while seated: a tp would dismount
    time.sleep(9.0)
    for ph in (12, 13, 14):
        _say(s, f'if block {ex} {ey} {ez} {egg}[phase={ph}]', f'{kind}-brooded')
    _check_said(s, f'{kind}: after incubateSeconds (10) the egg is brooded (phase 12..14)', [f'{kind}-brooded'])
    _sneak(s)
    _say(s, 'on vehicle if entity @s', f'{kind}-still-seated')
    stood = not _said(s, [f'{kind}-still-seated'], 0.8)[f'{kind}-still-seated']
    s.check(f'{kind}: sneak (real Shift) stands up', stood, '' if stood else 'still riding the seat')
    s.chat(f'tp @s {x + 0.5} {y} {z - 3.5} facing {ex + 0.5} {ey} {ez + 0.5}', 0.6)
    s.shot(f'{kind}-brooded')
    time.sleep(13.0)
    _say(s, f'if block {ex} {ey} {ez} minecraft:air', f'{kind}-egg-gone')
    _say(s, f'positioned {ex} {ey} {ez} if entity @e[type=simpledinos:{kind},distance=..4]', f'{kind}-hatchling')
    tags = [f'{kind}-egg-gone', f'{kind}-hatchling']
    if shell:
        _say(s, f'positioned {ex} {ey} {ez} if entity @e[type=minecraft:item,distance=..4,nbt={{Item:{{id:"simpledinos:{kind}_eggshell"}}}}]',
             f'{kind}-shells')
        tags.append(f'{kind}-shells')
    else:
        _say(s, f'positioned {ex} {ey} {ez} unless entity @e[type=minecraft:item,distance=..4]', f'{kind}-no-shell')
        tags.append(f'{kind}-no-shell')
    _check_said(s, f'{kind}: after hatchSeconds (10) the egg is gone and a hatchling stands there ('
                + ('eggshells dropped' if shell else 'no eggshell') + ')', tags)
    s.shot(f'{kind}-hatchling')


def dinos_brood(s):
    _prep(s)
    _kill_dinos(s)
    x, y, z = _move(s, 40)
    _floor(s, x - 4, z - 6, x + 4, z + 6, y)
    s.chat('simpledinos config set incubateSeconds 10', 0.5)
    s.chat('simpledinos config set hatchSeconds 10', 0.5)
    try:
        _brood_one(s, 'trex', x, y, z, shell=False)
        s.chat('kill @e[type=minecraft:item]', 0.3)
        _brood_one(s, 'triceratops', x + 3, y, z, shell=True)
    finally:
        s.chat('ride @s dismount', 0.3)
        s.chat('simpledinos config reset incubateSeconds', 0.5)
        s.chat('simpledinos config reset hatchSeconds', 0.5)
        s.chat('kill @e[type=minecraft:item]', 0.3)


def dinos_coats(s):
    _prep(s)
    _kill_dinos(s)
    x, y, z = _move(s, 40)
    _floor(s, x - 16, z - 22, x + 16, z + 20, y, clear=8)
    for kind in KINDS:
        gap = 3 if kind == 'utahraptor' else 6
        for i in range(len(COATS)):
            sx = x + (i - 2) * gap
            s.chat(f'summon simpledinos:{kind} {sx + 0.5} {y} {z + 8.5} '
                   f'{{NoAI:1b,PersistenceRequired:1b,CoatVariant:{i},Rotation:[180f,0f],Tags:["n123coat"]}}', 0.4)
        time.sleep(1.0)
        tags = []
        for i, coat in enumerate(COATS):
            _say(s, f'if entity @e[type=simpledinos:{kind},tag=n123coat,nbt={{CoatVariant:{i}}}]', f'{kind}-coat-{coat}')
            tags.append(f'{kind}-coat-{coat}')
        _check_said(s, f'{kind}: five coats summoned and read back (olive, sand, rust, moss, ash)', tags)
        back = 10 if kind == 'utahraptor' else 16
        s.chat(f'tp @s {x + 0.5} {y + 3} {z + 8.5 - back} facing {x + 0.5} {y + 1.2} {z + 8.5}', 1.0)
        time.sleep(1.5)
        s.shot(f'coats-{kind}')
        s.chat(f'kill @e[type=simpledinos:{kind},tag=n123coat]', 0.5)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 0.5)


def dinos_hitbox(s):
    _prep(s)
    _kill_dinos(s)
    x, y, z = _move(s, 50)
    _floor(s, x - 10, z - 10, x + 14, z + 14, y, clear=7)
    s.io.key_down('F3')
    s.io.key('b', 0.3)
    s.io.key_up('F3')
    try:
        for kind in KINDS:
            small = kind == 'utahraptor'
            cz = z + (3 if small else 4)
            s.chat(f'fill {x - 6} {y} {cz - 4} {x + 12} {y + 6} {cz + 4} minecraft:air', 0.4)
            # side on: facing east (yaw -90), so the body box faces the camera and the head points away from it
            s.chat(f'summon simpledinos:{kind} {x + 0.5} {y} {cz + 0.5} {{NoAI:1b,PersistenceRequired:1b,Rotation:[-90f,0f],Tags:["n123hit"]}}', 0.8)
            s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 0.6)
            ok = _aim_entity(s, x, y + (0 if small else 1), cz, f'simpledinos:{kind}', f'{kind} body')
            s.check(f'{kind}: crosshair on the body centre hits the dinosaur (core hitbox in the client)', ok, s.last.get('hitEntity'))
            s.chat(f'tp @s {x + 2.5} {y + 2} {cz - 9.5} facing {x + 2.5} {y + 1.5} {cz + 0.5}', 1.0)
            time.sleep(1.2)
            s.shot(f'{kind}-free')
            # wall right in front of the head (reach: T. rex about 3.5 blocks, smaller kinds less)
            wx = x + (3 if small else 4)
            s.chat(f'fill {wx} {y} {cz - 3} {wx} {y + 5} {cz + 3} minecraft:stone', 0.5)
            time.sleep(2.5)
            s.shot(f'{kind}-wall')
            s.chat(f'kill @e[type=simpledinos:{kind},tag=n123hit]', 0.4)
            s.chat(f'fill {wx} {y} {cz - 3} {wx} {y + 5} {cz + 3} minecraft:air', 0.4)
    finally:
        s.io.key_down('F3')
        s.io.key('b', 0.3)
        s.io.key_up('F3')


# ---------------------------------------------------------------- Simple End
def _sign_text(s, bx, by, bz, word, tag):
    _mark(s, tag)
    s.chat(f'data get block {bx} {by} {bz} front_text.messages[0]', 0.6)
    m = _after(s, tag, r'has the following block data: (.*)$')
    s.check(f'{tag}: the sign holds "{word}"', bool(m and word in m.group(1)), m.group(1) if m else 'no data line')


def _edit_sign(s, screen, word, tag):
    st = _wait_screen(s, screen, 5)
    s.check(f'{tag}: REAL right click opens {screen}', st is not None, s.last.get('screen'))
    if not st:
        return False
    s.type_text(word)
    time.sleep(0.4)
    _park(s, s.st())
    s.shot(f'{tag}-editor')
    if s.find_widget(label='Done', st=s.st()) is not None:
        s.click_widget(label='Done')
    else:
        s.close()
    time.sleep(0.8)
    return True


def end_woodset(s):
    _prep(s)
    x, y, z = _move(s, 50)
    _floor(s, x - 8, z - 4, x + 8, z + 10, y, clear=6)
    # boats
    boats = ['ashwood_boat', 'duskwood_boat', 'ashwood_chest_boat', 'duskwood_chest_boat']
    for i, b in enumerate(boats):
        s.chat(f'summon simpleend:{b} {x - 3 + 2 * i + 0.5} {y} {z + 3.5} {{Rotation:[90f,0f]}}', 0.4)
    time.sleep(1.0)
    for b in boats:
        _say(s, f'if entity @e[type=simpleend:{b},distance=..12]', f'boat-{b}')
    _check_said(s, 'four End boats summoned (Ashwood/Duskwood, with and without chest)', [f'boat-{b}' for b in boats])
    s.chat(f'tp @s {x + 0.5} {y + 1} {z - 2.5} facing {x + 0.5} {y} {z + 3.5}', 1.0)
    time.sleep(1.0)
    s.shot('boats')
    s.chat(f'tp @s {x - 2.5} {y} {z + 0.5}', 0.6)
    if _aim_entity(s, x - 3, y - 1, z + 3, 'simpleend:ashwood_boat', 'ashwood boat'):
        s.io.right_click()
        time.sleep(1.0)
        _say(s, 'on vehicle if entity @s[type=simpleend:ashwood_boat]', 'boat-ride')
        _check_said(s, 'REAL right click on the Ashwood Boat: the player sits in it', ['boat-ride'])
        s.shot('in-boat')
        _sneak(s)
    s.chat('ride @s dismount', 0.3)
    for b in boats:
        s.chat(f'kill @e[type=simpleend:{b}]', 0.2)
    s.chat('kill @e[type=minecraft:item]', 0.3)
    # standing sign
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 0.6)
    s.chat('item replace entity @s weapon.mainhand with simpleend:ashwood_sign', 0.4)
    if _aim(s, x, y - 1, z + 2, 'minecraft:smooth_stone', 'ashwood sign floor'):
        s.io.right_click()
        if _edit_sign(s, 'SignEditScreen', 'ashwood', 'ashwood-sign'):
            _say(s, f'if block {x} {y} {z + 2} simpleend:ashwood_sign', 'sign-placed')
            _check_said(s, 'Ashwood Sign placed on the floor', ['sign-placed'])
            _sign_text(s, x, y, z + 2, 'ashwood', 'ashwood-sign')
    # wall hanging sign on the face of a planks block at eye height
    s.chat(f'setblock {x + 3} {y + 1} {z + 3} simpleend:duskwood_planks', 0.4)
    s.chat(f'tp @s {x + 3.5} {y} {z + 0.5}', 0.6)
    s.chat('item replace entity @s weapon.mainhand with simpleend:duskwood_hanging_sign', 0.4)
    if _aim(s, x + 3, y + 1, z + 3, 'simpleend:duskwood_planks', 'duskwood hanging sign wall'):
        s.io.right_click()
        if _edit_sign(s, 'HangingSignEditScreen', 'duskwood', 'duskwood-hanging-sign'):
            _say(s, f'if block {x + 3} {y + 1} {z + 2} simpleend:duskwood_wall_hanging_sign', 'hanging-placed')
            _check_said(s, 'Duskwood Hanging Sign hangs on the wall face', ['hanging-placed'])
            _sign_text(s, x + 3, y + 1, z + 2, 'duskwood', 'duskwood-hanging-sign')
    s.chat(f'tp @s {x + 1.5} {y + 1} {z - 2.5} facing {x + 1.5} {y + 0.5} {z + 2.5}', 1.0)
    time.sleep(0.8)
    s.shot('signs')
    # End painting on a 5 x 4 wall
    s.chat(f'fill {x - 3} {y} {z + 7} {x + 1} {y + 3} {z + 7} simpleend:ashwood_planks', 0.4)
    s.chat(f'fill {x - 3} {y} {z + 6} {x + 1} {y + 3} {z + 6} minecraft:air', 0.3)
    s.chat('kill @e[type=minecraft:painting]', 0.3)
    s.chat(f'tp @s {x - 0.5} {y} {z + 4.5}', 0.6)
    s.chat('item replace entity @s weapon.mainhand with simpleend:end_painting', 0.4)
    if _aim(s, x - 1, y + 1, z + 7, 'simpleend:ashwood_planks', 'end painting wall'):
        s.io.right_click()
        time.sleep(1.0)
        _say(s, f'positioned {x - 1} {y + 1} {z + 6} if entity @e[type=minecraft:painting,distance=..4]', 'painting')
        _check_said(s, 'REAL right click with the End Painting hangs a painting', ['painting'])
        _mark(s, 'painting-variant')
        s.chat(f'execute positioned {x - 1} {y + 1} {z + 6} as @e[type=minecraft:painting,distance=..4,limit=1] run data get entity @s variant', 0.6)
        m = _after(s, 'painting-variant', r'has the following entity data: (.*)$')
        s.check('the painting shows an End picture (variant simpleend:...)', bool(m and 'simpleend:' in m.group(1)),
                m.group(1) if m else 'no data line')
    s.chat(f'tp @s {x - 0.5} {y + 1} {z - 0.5} facing {x - 0.5} {y + 1.5} {z + 7}', 1.0)
    time.sleep(1.0)
    s.shot('painting-1')
    time.sleep(2.0)
    s.shot('painting-2')


GARDEN_PLANTS = ['simpleend:ash_bud', 'simpleend:dusk_bud', 'simpleend:end_grass', 'simpleend:end_bush']


def end_garden(s):
    _prep(s)
    x, y, z = _move(s, 50)
    _floor(s, x - 10, z, x + 10, z + 14, y, 'minecraft:end_stone', clear=6)
    placed = []
    for i in range(20):
        px, pz = x - 9 + (i * 7) % 19, z + 1 + (i * 5) % 12
        plant = GARDEN_PLANTS[i % 4]
        s.chat(f'setblock {px} {y} {pz} {plant}', 0.25)
        placed.append((px, pz, plant))
    s.chat(f'fill {x + 6} {y} {z + 9} {x + 8} {y + 1} {z + 11} simpleend:ashwood_leaves', 0.3)
    s.chat(f'fill {x - 8} {y} {z + 9} {x - 6} {y + 1} {z + 11} simpleend:duskwood_leaves', 0.3)
    s.chat(f'fill {x - 2} {y - 1} {z + 2} {x + 2} {y - 1} {z + 3} simpleend:burrowed_end_stone', 0.3)
    for i in range(6):
        s.chat(f'summon simpleend:nibblemite {x - 2.5 + i} {y} {z + 2.5 + (i % 2)} '
               f'{{NoAI:1b,PersistenceRequired:1b,Variant:{i % 2},Rotation:[180f,0f],Tags:["n123mite"]}}', 0.3)
    time.sleep(2.5)
    tags = []
    for i, (px, pz, plant) in enumerate(placed):
        _say(s, f'if block {px} {y} {pz} {plant}', f'plant-{i}')
        tags.append(f'plant-{i}')
    _check_said(s, f'{len(placed)} End plants (buds, grass, bushes) still stand on End stone after 2 s', tags)
    _say(s, 'if entity @e[type=simpleend:nibblemite,tag=n123mite,nbt={Variant:0}]', 'mite-ash')
    _say(s, 'if entity @e[type=simpleend:nibblemite,tag=n123mite,nbt={Variant:1}]', 'mite-dusk')
    _check_said(s, 'Nibblemites of both colourings in the garden', ['mite-ash', 'mite-dusk'])
    s.chat(f'tp @s {x + 0.5} {y + 6} {z - 8.5} facing {x + 0.5} {y} {z + 7}', 1.0)
    time.sleep(1.5)
    s.shot('garden')
    s.chat(f'tp @s {x + 0.5} {y + 1.2} {z - 0.5} facing {x + 0.5} {y} {z + 3}', 1.0)
    time.sleep(1.0)
    s.shot('nibblemites')
    # bone meal on a lone End grass spreads tufts onto the End stone around it
    gx, gz = x, z + 18
    _floor(s, gx - 3, gz - 3, gx + 3, gz + 3, y, 'minecraft:end_stone', clear=3)
    s.chat(f'setblock {gx} {y} {gz} simpleend:end_grass', 0.4)
    s.chat(f'tp @s {gx + 0.5} {y} {gz - 2.5}', 0.6)
    s.chat('item replace entity @s weapon.mainhand with minecraft:bone_meal 64', 0.4)
    if _aim(s, gx, y, gz, 'simpleend:end_grass', 'end grass bone meal'):
        for _ in range(3):
            s.io.right_click()
            time.sleep(0.5)
        time.sleep(1.0)
        s.shot('grass-spread')
        _count(s, 'grass', f'{gx - 3} {y} {gz - 3} {gx + 3} {y} {gz + 3}', 'simpleend:end_grass')
        n = _counts(s, ['grass'])['grass']
        s.check('bone meal (REAL right clicks) on End grass spreads tufts (more than the one placed)', n is not None and n > 1, n)
    s.chat('kill @e[type=simpleend:nibblemite,tag=n123mite]', 0.3)


def _bonemeal_until(s, bx, by, bz, bud, tag, tries=10):
    if not _aim(s, bx, by, bz, bud, tag):
        return False
    for _ in range(tries):
        s.io.right_click()
        time.sleep(0.6)
        if s.st().get('hitBlock') != bud:
            return True
    return False


def end_trees(s):
    _prep(s)
    x, y, z = _move(s, 60)
    s.chat('item replace entity @s weapon.mainhand with minecraft:bone_meal 64', 0.4)
    trees = [  # (offset x, bud, width, log, check height)
        (0, 'simpleend:ash_bud', 1, 'simpleend:ashwood_log', 14, 'big'),
        (40, 'simpleend:dusk_bud', 2, 'simpleend:duskwood_log', 22, 'huge'),
        (95, 'simpleend:ash_bud', 4, 'simpleend:ashwood_log', 37, 'colossal'),
    ]
    for ox, bud, n, log, h, name in trees:
        bx, bz = x + ox, z + 6
        s.chat(f'tp @s {bx + 0.5} {y} {z + 0.5}', 1.0)
        time.sleep(1.0)
        _floor(s, bx - 4, bz - 4, bx + n + 3, bz + n + 3, y, 'minecraft:end_stone', clear=1)
        s.chat(f'fill {bx} {y} {bz} {bx + n - 1} {y} {bz + n - 1} {bud}', 0.5)
        s.chat(f'tp @s {bx + 0.5} {y} {bz - 2.5}', 0.6)
        grown = _bonemeal_until(s, bx, y, bz, bud, f'{name} tree')
        s.check(f'{name}: REAL bone-meal clicks on the bud grow a tree (bud replaced)', grown, s.last.get('hitBlock'))
        time.sleep(1.5)
        tag = f'tree-{name}'
        for dx in range(n):
            for dz in range(n):
                _say(s, f'if block {bx + dx} {y + h} {bz + dz} {log}', tag)
        _check_said(s, f'{name}: {log} trunk reaches +{h} ({"1 bud" if n == 1 else f"{n}x{n} buds"})', [tag])
        back = {1: 14, 2: 24, 4: 44}[n]
        s.chat(f'tp @s {bx + 0.5} {y + h * 0.5} {bz - back} facing {bx + 0.5} {y + h * 0.55} {bz + 0.5}', 1.0)
        time.sleep(3.0)
        s.shot(f'tree-{name}')
    s.chat(f'tp @s {x + 50.5} {y + 30} {z - 70} facing {x + 50.5} {y + 20} {z + 6}', 1.0)
    time.sleep(4.0)
    s.shot('trees-overview')
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 0.5)


# ---------------------------------------------------------------- Simple Tools
def tools_brush(s):
    _prep(s)
    x, y, z = _move(s, 140)
    gx, gy, gz = x, y - 1, z + 3
    s.chat(f'fill {x - 20} {y} {z - 8} {x + 20} {y + 4} {z + 24} minecraft:air', 0.4)
    s.chat(f'fill {x - 20} {y - 1} {z - 8} {x + 20} {y - 1} {z + 24} minecraft:grass_block', 0.4)
    s.chat('item replace entity @s weapon.mainhand with simpletools:biome_brush', 0.3)
    s.chat('item replace entity @s weapon.offhand with minecraft:sand 16', 0.3)
    s.chat(f'tp @s {x + 0.5} {y + 4} {z - 6.5} facing {x + 0.5} {y} {z + 6}', 1.0)
    time.sleep(1.0)
    s.shot('before')
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 0.6)
    if _aim(s, gx, gy, gz, 'minecraft:grass_block', 'brush grass'):
        s.io.right_click()
        time.sleep(2.0)
        _say(s, f'if biome {gx} {gy} {gz} minecraft:desert', 'desert-here')
        _say(s, f'if biome {gx} {gy} {gz + 16} minecraft:plains', 'plains-far')
        _check_said(s, 'REAL click with Biome Brush + Sand: the clicked cell is desert, 16 blocks away still plains',
                    ['desert-here', 'plains-far'])
        s.chat(f'tp @s {x + 0.5} {y + 4} {z - 6.5} facing {x + 0.5} {y} {z + 6}', 1.0)
        time.sleep(2.0)
        s.shot('after-desert')
    # sneak: only the clicked cell
    s.chat('item replace entity @s weapon.offhand with minecraft:snow_block 16', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 0.6)
    if _aim(s, gx, gy, gz, 'minecraft:grass_block', 'brush sneak'):
        _sneak_click(s)
        time.sleep(1.5)
        _say(s, f'if biome {gx} {gy} {gz} minecraft:snowy_plains', 'snow-cell')
        _say(s, f'if biome {gx} {gy} {gz + 4} minecraft:desert', 'desert-next')
        _check_said(s, 'sneak + REAL click with Snow Block: only the clicked cell becomes snowy plains (next cell still desert)',
                    ['snow-cell', 'desert-next'])
        s.chat(f'tp @s {x + 0.5} {y + 4} {z - 6.5} facing {x + 0.5} {y} {z + 6}', 1.0)
        time.sleep(2.0)
        s.shot('after-sneak-snow')


BUCKETS = (['raw_ceramic_bucket']
           + [f'{w}ceramic{f}_bucket' for w in ('', 'chipped_', 'cracked_', 'brittle_') for f in ('', '_water', '_lava')]
           + ['enderite_bucket'] + [f'enderite_{f}_bucket{full}' for f in ('water', 'lava', 'milk', 'soul_lava') for full in ('', '_full')])


def tools_buckets(s):
    _prep(s, 'survival')
    s.check('22 bucket ids in the list', len(BUCKETS) == 22, len(BUCKETS))
    for i, b in enumerate(BUCKETS):
        slot = f'hotbar.{i}' if i < 9 else f'inventory.{i - 9}'
        s.chat(f'item replace entity @s {slot} with simpletools:{b}', 0.25)
    time.sleep(0.8)
    inv = s.st().get('inventory', [])
    miss = [b for i, b in enumerate(BUCKETS) if not (len(inv) > i and str(inv[i]).startswith(f'simpletools:{b}'))]
    s.check('all 22 buckets in the inventory (each read back)', not miss, f'missing {miss}' if miss else '22/22')
    s.key('e', 1.2)
    st = _wait_screen(s, 'InventoryScreen', 5)
    s.check('survival inventory open (InventoryScreen)', st is not None, s.last.get('screen'))
    if st:
        _park(s, st)
        s.shot('inventory-22-buckets')
        for b in ('enderite_water_bucket_full', 'brittle_ceramic_lava_bucket'):
            i = BUCKETS.index(b)
            sl = s.slot_xy(st, cslot=i, own=True)
            if not sl:
                s.check(f'slot of {b} found', False, f'{len(st.get("slots", []))} slots')
                continue
            s.io.move(*s.abs(st, sl['x'], sl['y']))
            time.sleep(0.8)
            s.shot(f'tooltip-{b}')
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('clear @s', 0.3)


def tools_vault(s):
    _prep(s, 'survival')
    x, y, z = _move(s, 60)
    _floor(s, x - 4, z - 4, x + 4, z + 6, y)
    s.chat('item replace entity @s hotbar.0 with minecraft:iron_ingot 5', 0.3)
    s.chat('item replace entity @s hotbar.1 with minecraft:gold_ingot 2', 0.3)
    s.chat('item replace entity @s hotbar.2 with minecraft:diamond_sword', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 4.5}', 0.8)  # die 2.5 blocks behind the vault: the drops stay lying (no pickup)
    s.chat('kill @s', 1.5)
    st = _wait_screen(s, 'DeathScreen', 6)
    s.check('death screen after /kill', st is not None, s.last.get('screen'))
    if st:
        time.sleep(1.5)  # the Respawn button is enabled after a short delay
        s.click_widget(label='Respawn')
        time.sleep(2.0)
    if 'DeathScreen' in s.screen():
        s.check('REAL click on Respawn', False, s.last.get('screen'))
        return
    s.chat('gamemode creative @s', 0.3)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5}', 1.0)
    time.sleep(1.0)
    s.chat(f'setblock {x} {y} {z + 2} simpletools:death_vault', 0.5)
    s.chat('item replace entity @s weapon.mainhand with minecraft:air', 0.3)
    for attempt in range(3):
        if not _aim(s, x, y, z + 2, 'simpletools:death_vault', 'death vault'):
            continue
        s.io.right_click()
        st = _wait_screen(s, 'DeathVaultScreen', 4)
        if st:
            break
        s.close()
    s.check('REAL right click opens the Death Vault (DeathVaultScreen)', bool(st and 'DeathVaultScreen' in st.get('screen', '')),
            s.last.get('screen'))
    if st and 'DeathVaultScreen' in st.get('screen', ''):
        s.check('title "Death Vault"', _plain(st.get('title', '')) == 'Death Vault', st.get('title'))
        own = [sl for sl in st.get('slots', []) if not sl.get('own')]
        s.check('46 vault slots (45 fields + offering)', len(own) == 46, len(own))
        shown = [sl.get('stack', '') for sl in own if sl.get('stack') and 'air' not in str(sl.get('stack'))]
        want = ['minecraft:iron_ingot', 'minecraft:gold_ingot', 'minecraft:diamond_sword']
        miss = [w for w in want if not any(str(v).startswith(w) for v in shown)]
        s.check('the dropped items are listed (iron, gold, sword)', not miss, f'missing {miss}; shown {shown[:6]}')
        _park(s, st)
        s.shot('death-vault')
        s.close()
    s.chat('kill @e[type=minecraft:item]', 0.3)


SCENARIOS = {
    'n123-dinos-brood': dinos_brood,
    'n123-dinos-coats': dinos_coats,
    'n123-dinos-hitbox': dinos_hitbox,
    'n123-end-woodset': end_woodset,
    'n123-end-garden': end_garden,
    'n123-end-trees': end_trees,
    'n123-tools-brush': tools_brush,
    'n123-tools-buckets': tools_buckets,
    'n123-tools-vault': tools_vault,
}
