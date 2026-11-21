"""N130 release polish, mods 35-37 (offen-vor-release.md section 3, "Szenen" of Simple Pads, Simple Beacons, Simple Sandwiches), word for word:

Simple Pads       "Flug, Start, Trank, Spawn (W4C-39)." - simplepads-3: the pads after the hardening (endless-flight fix) in the real game.
  n130-pads-flight    Flypad I, survival: standing on it grants flight (tag simplebuilding.flypad_flight + mayfly), a REAL double tap
                      on Space starts flying, REAL W flies out of the 4-block field -> flight gone + safety net (Slow Falling); back on
                      the pad: a redstone block next to it takes the flight, a broken pad takes it too (the hardening). Shots.
  n130-pads-elytra    Elytra Pad I, survival, empty chest: standing on it puts a Spawn Elytra into the chest slot; 30 blocks up (out of
                      the field) a REAL Space press while falling starts gliding (FallFlying); after landing outside the field (grace
                      3 s) the elytra is gone again. Shots while gliding.
  n130-pads-launch    Launchpad I: REAL right click with wind charges loads one charge (Charges 1), REAL Shift held >= 0.8 s + right
                      click (aimed from the sneaking eye 1.27, crosshair read back) loads the rest (capacity 4); survival, standing on
                      it 3 s throws the player >= 4 blocks up and uses the charges. Shots loaded / mid-flight.
  n130-pads-potion    Potion Pad I: a REAL throw of a Splash Potion of Swiftness (standing on the pad, looking down, crosshair read back)
                      stores it (Potion) and the pad shows ready (active=true); off and back on: Speed after ~1 s, after 3 s the pad
                      goes into its cooldown (cooling=true). Shots ready / standing / cooling.
  n130-pads-spawn     Spawn Teleporter (Enderite = 5 s, else tier II = 20 s), survival: bed + /spawnpoint, standing still -> active=true
                      while charging, then the jump lands at the bed; with a redstone block beside it the jump goes to the world spawn
                      (far from pad and bed). Shots charging / arrival.
Simple Beacons    "Partikel/Ton der Conduits (W4C-08)."
  n130-beacons-conduits  Four glass pools with a 5x5x5 prismarine frame each: Strong, Mighty, Evil, Mighty Evil Conduit. Subtitles switched
                      on through the real Options/Accessibility screen (the hum, the short ambient and the sculk clicking of the evil
                      kinds show as subtitle lines in the shots); per pool the player swims next to the conduit: effects read back
                      (Strong: Conduit Power + Dolphin's Grace, Mighty: + Night Vision, Evil: Mining Fatigue + Slowness, Mighty Evil: +
                      Weakness, Darkness; evilBeaconGraceMinutes 0 for the scene, restored to 5), underwater shots of the particles
                      (glow / purple / red / dark red dust, nautilus or sculk-soul stream), one overview shot at night. The sound itself
                      is the owner's ear (W4C-08); the conduits play Vanilla conduit sounds with their own volume/pitch.
Simple Sandwiches "Brett, Käse/Butter schneiden, Kessel-Stufen."
  n130-sandwiches-board  Cutting board, survival: bread, knife opens it, butter from the off hand, beef + apple + cheese slice, empty hand
                      closes, empty hand takes the sandwich - crosshair read back before EVERY click, board data (Stage, Buttered) after
                      each; shots per stage + sandwich tooltip under the real pointer.
  n130-sandwiches-cut    Cheese block (top face) and butter block (north face) cut with the knife: crosshair read back before each click (the
                      top sinks with each slice), exactly 4 slices each and slices=12,cut=<face>. Shots.
  n130-sandwiches-cauldron  butterTicks/cheeseTicks 800 by /simplesandwiches config for the scene (restored to 6000): an empty hand takes
                      a butter block out of a butter cauldron; REAL milk bucket into an empty cauldron, then the real ripening is
                      watched: milk 0-3 -> butter 0-3 -> cheese (times logged), an empty hand takes the cheese (cheese block in hand).
                      Shots of the stages. The 6 cut sounds are on the listening page n130-klang.html (synthetic, W4C-42).

Patterns (uiprobe_n122-n128 lessons): position via int(float()) of status 'pos'; 'tp ... facing' with y - 1.62 (sneaking: y - 1.27); real
clicks only after the crosshair target was read back (status hitBlock); main hand = selected hotbar slot; Shift held >= 0.8 s; chat
commands ~1-1.5 s each; counts from "Test passed. Count: N". No game code is changed.

    timeout 2400 xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe130 --scale 2 \\
        --only n130-pads-flight,n130-pads-elytra,n130-pads-launch,n130-pads-potion,n130-pads-spawn,n130-beacons-conduits,n130-sandwiches-board,n130-sandwiches-cut,n130-sandwiches-cauldron
"""
import re
import time

TAG = 'N130'
FLOOR = 'minecraft:smooth_stone'


# ---------------------------------------------------------------- helpers
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


_seq = [0]


def _test(s, cond, tag):
    """One server condition, read back at once through a unique say tag (True/False)."""
    _seq[0] += 1
    t = f'{tag}-{_seq[0]}'
    _say(s, cond, t)
    return _said(s, [t], 1.2)[t]


def _prep(s, mode='creative', night_vision=True):
    s.close()
    s.io.button(3, False)
    s.io.key_up('Shift_L')
    s.io.key_up('space')
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
    """Moves dx blocks east, lays a smooth stone floor under fresh air and returns the feet position."""
    x, y, z = _pos(s)
    x += dx
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.2)
    s.chat(f'fill {x + x0} {y} {z + z0} {x + x1} {y + height} {z + z1} minecraft:air', 0.6)
    s.chat(f'fill {x + x0} {y - 1} {z + z0} {x + x1} {y - 1} {z + z1} {FLOOR}', 0.6)
    s.chat('kill @e[type=minecraft:item,distance=..40]', 0.3)
    time.sleep(1.0)
    return x, y, z


def _aim(s, at, target, want, tag, tries=4, eye=1.62):
    """Feet at `at`, eyes (eye height `eye`) onto the world point `target`; waits until the crosshair reads back block `want`."""
    hit = None
    for _ in range(tries):
        s.chat(f'tp @s {at[0]:.2f} {at[1]} {at[2]:.2f} facing {target[0]:.3f} {target[1] - eye:.3f} {target[2]:.3f}', 0.8)
        for _ in range(6):
            hit = s.st().get('hitBlock')
            if hit == want:
                return True
            time.sleep(0.3)
    s.check(f'{tag}: crosshair on {want}', False, hit)
    return False


def _hit_is(s, want, tag, wait=2.0):
    """Reads the crosshair back (the camera keeps turning a moment after a tp) - True when it is on `want`."""
    end, hit = time.time() + wait, None
    while time.time() < end:
        hit = s.st().get('hitBlock')
        if hit == want:
            return True
        time.sleep(0.25)
    s.check(f'{tag}: crosshair on {want} before the click', False, hit)
    return False


def _hand(s, slot, stack, tag):
    """Puts the stack into hotbar slot `slot` (emptied first), selects it with the REAL number key and reads the main hand back."""
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


def _tap(s, name, hold=0.15):
    s.io.key_down(name)
    time.sleep(hold)
    s.io.key_up(name)


def _double_tap(s, name, hold=0.06, gap=0.08):
    """Two quick real presses (XTest): the driver's key()/key_down() pause 0.2-0.3 s per edge, too slow for Vanilla's double tap."""
    code = s.io._code(name)
    for _ in range(2):
        for down, pause in ((1, hold), (0, gap)):
            s.io.xtst.XTestFakeKeyEvent(s.io.d, code, down, 0)
            s.io.x11.XFlush(s.io.d)
            time.sleep(pause)


def _center_pointer(s):
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)
    time.sleep(0.3)


def _near(p, q, r):
    return abs(p[0] - q[0]) <= r and abs(p[2] - q[2]) <= r and abs(p[1] - q[1]) <= r + 1


# ---------------------------------------------------------------- Simple Pads
def pads_flight(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    s.chat('effect give @s minecraft:resistance 120 255 true', 0.3)
    fx, fz = x + 4, z + 6
    s.chat(f'setblock {fx} {y} {fz} simplepads:flypad', 0.5)
    on_pad = f'tp @s {fx + 0.5} {y + 0.1} {fz + 0.5} 0 0'
    s.chat(on_pad, 1.5)
    time.sleep(0.8)
    granted = _test(s, 'if entity @s[tag=simplebuilding.flypad_flight,nbt={abilities:{mayfly:1b}}]', 'fly-granted')
    s.check('flypad: standing on it grants flight (tag + mayfly)', granted, 'simplebuilding.flypad_flight, abilities.mayfly')
    s.check('flypad: the pad shows it is working (active=true)', _test(s, f'if block {fx} {y} {fz} simplepads:flypad[active=true]', 'fly-active'))
    # Vanilla toggles flight on a second press within 7 ticks; at a low frame rate a too short press is lost - slower taps until one works
    flying = False
    for hold, gap in ((0.06, 0.08), (0.12, 0.1), (0.16, 0.12), (0.2, 0.1)):
        _double_tap(s, 'space', hold, gap)
        time.sleep(0.3)
        s.io.key_down('space')
        time.sleep(0.5)
        s.io.key_up('space')
        flying = _test(s, 'if entity @s[nbt={abilities:{flying:1b}}]', 'flying')
        if flying:
            break
        s.chat(on_pad, 1.0)
    s.check('flypad: a REAL double tap on Space starts flying', flying, f'abilities.flying, y now {_fpos(s)[1]:.1f}')
    s.shot('1-flying')
    if flying:
        # REAL W while flying: out of the 4-block field (half width 1.5) -> flight revoked + safety net
        start = _fpos(s)
        s.io.key_down('w')
        try:
            time.sleep(1.4)
        finally:
            s.io.key_up('w')
        time.sleep(1.2)
        end = _fpos(s)
        s.check('flypad: REAL W flies out of the field (>= 3 blocks)', abs(end[2] - start[2]) >= 3, f'{start} -> {end}')
        _say(s, 'if entity @s[tag=!simplebuilding.flypad_flight,nbt={abilities:{mayfly:0b,flying:0b}}]', 'fly-revoked')
        _say(s, 'if entity @s[nbt={active_effects:[{id:"minecraft:slow_falling"}]}]', 'fly-net')
        _check_said(s, 'flypad: flying out takes the flight away and catches the player with Slow Falling (safety net)', ['fly-revoked', 'fly-net'])
        s.shot('2-out-safety-net')
        time.sleep(2.0)
    # the hardening: switched off by redstone, then broken - players in the field lose the flight
    s.chat('effect clear @s minecraft:slow_falling', 0.3)
    s.chat(on_pad, 1.5)
    time.sleep(0.8)
    if _test(s, 'if entity @s[nbt={abilities:{mayfly:1b}}]', 'fly-again'):
        s.chat(f'setblock {fx + 1} {y} {fz} minecraft:redstone_block', 0.5)
        time.sleep(1.0)
        s.check('flypad: a redstone signal switches the pad off and takes the flight', _test(s, 'if entity @s[nbt={abilities:{mayfly:0b}}]', 'fly-redstone'))
        s.chat(f'setblock {fx + 1} {y} {fz} minecraft:air', 0.5)
        time.sleep(1.0)
        s.check('flypad: signal gone - the flight is granted again', _test(s, 'if entity @s[nbt={abilities:{mayfly:1b}}]', 'fly-back'))
        s.chat(f'setblock {fx} {y} {fz} minecraft:air', 0.5)
        time.sleep(1.0)
        s.check('flypad: breaking the pad takes the flight (no endless flight, tag gone)',
                _test(s, 'if entity @s[tag=!simplebuilding.flypad_flight,nbt={abilities:{mayfly:0b}}]', 'fly-broken'))
        s.shot('3-pad-broken')
    else:
        s.check('flypad: flight granted again on the pad', False, 'mayfly 0')
    s.chat('gamemode creative @s', 0.3)


def pads_elytra(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40, x0=-20, z0=-3, x1=20, z1=60)
    s.chat('effect give @s minecraft:resistance 120 255 true', 0.3)
    ex, ez = x + 2, z + 4
    s.chat(f'setblock {ex} {y} {ez} simplepads:elytra_pad', 0.5)
    s.chat(f'tp @s {ex + 0.5} {y + 0.15} {ez + 0.5} 0 20', 1.5)
    time.sleep(1.0)
    _say(s, 'if items entity @s armor.chest simplepads:spawn_elytra', 'elytra-chest')
    _say(s, f'if block {ex} {y} {ez} simplepads:elytra_pad[active=true]', 'elytra-active')
    if not _check_said(s, 'elytra pad: standing on it puts a Spawn Elytra into the empty chest slot, the pad shows active', ['elytra-chest', 'elytra-active']):
        return
    s.shot('1-on-pad')
    # out of the 5-high field: 30 up, falling, a REAL Space press starts the glide
    s.chat(f'tp @s {ex + 0.5} {y + 30} {ez + 0.5} 0 35', 0.6)
    time.sleep(0.5)
    gliding = False
    for _ in range(3):
        _tap(s, 'space', 0.15)
        gliding = _test(s, 'if entity @s[nbt={FallFlying:1b}]', 'gliding')
        if gliding:
            break
    s.check('elytra: a REAL Space press while falling starts gliding (FallFlying)', gliding, f'y {_fpos(s)[1]:.1f}')
    s.key('F5', 0.6)
    s.shot('2-gliding')
    s.key('F5', 0.3)
    s.key('F5', 0.3)
    # land outside the field: the elytra stays for the 3-s grace, then goes once the player is on the ground
    gone = False
    end = time.time() + 25
    while time.time() < end and not gone:
        gone = _test(s, 'unless items entity @s armor.chest simplepads:spawn_elytra', 'elytra-gone')
    land = _pos(s)
    s.check('elytra: landed outside the field, the Spawn Elytra is taken off again', gone and not _near(land, (ex, y, ez), 3), f'landed at {land}')
    s.chat('gamemode creative @s', 0.3)


def pads_launch(s):
    _prep(s)
    x, y, z = _arena(s, 40)
    px, pz = x, z + 3
    s.chat(f'setblock {px} {y} {pz} simplepads:launchpad', 0.4)
    me = (x + 0.5, y, z + 0.5)
    if not _hand(s, 0, 'minecraft:wind_charge 4', 'launch'):
        return
    if _aim(s, me, (px + 0.5, y + 0.05, pz + 0.5), 'simplepads:launchpad', 'launchpad'):
        _click(s, 0.6)
        s.check('launchpad: a REAL right click with wind charges loads one charge', _test(s, f'if data block {px} {y} {pz} {{Charges:1}}', 'charges1'), 'Charges 1')
    # REAL Shift held >= 0.8 s, aimed from the sneaking eye (1.27), crosshair read back, then the click loads the rest
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.9)
        s.check('launchpad: the client sneaks before the click (real Shift held)', bool(s.st().get('sneaking')), s.last.get('sneaking'))
        if _aim(s, me, (px + 0.5, y + 0.05, pz + 0.5), 'simplepads:launchpad', 'launchpad sneaking', eye=1.27):
            _click(s, 0.6)
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.5)
    got = next((n for n in (4, 3, 2, 1, 0) if _test(s, f'if data block {px} {y} {pz} {{Charges:{n}}}', f'charges{n}')), None)
    s.check('launchpad: Shift (held) + right click loads the rest up to the capacity 4', got == 4, f'Charges {got}')
    s.shot('1-loaded')
    s.chat('gamemode survival @s', 0.6)
    s.chat('effect give @s minecraft:resistance 60 255 true', 0.3)
    s.chat(f'tp @s {px + 0.5} {y + 0.1} {pz + 0.5} 0 80', 0.4)
    top, shot = float(y), False
    end = time.time() + 10
    while time.time() < end:
        cur = _fpos(s)[1]
        top = max(top, cur)
        if cur > y + 4 and not shot:
            s.shot('2-mid-flight-looking-down')
            shot = True
        if top > y + 4 and cur <= y + 0.5:
            break
        time.sleep(0.15)
    s.check('launchpad: standing on it 3 s throws the player >= 4 blocks up', top >= y + 4, f'highest y {top:.1f} (start {y})')
    s.check('launchpad: the start used the charges', _test(s, f'if data block {px} {y} {pz} {{Charges:0}}', 'charges-used'), 'Charges 0')
    s.chat('gamemode creative @s', 0.3)


def pads_potion(s):
    _prep(s, 'survival', night_vision=False)
    x, y, z = _arena(s, 40)
    px, pz = x + 2, z + 4
    s.chat(f'setblock {px} {y} {pz} simplepads:potion_pad', 0.5)
    if not _hand(s, 0, 'minecraft:splash_potion[potion_contents={potion:"minecraft:swiftness"}] 3', 'potion'):
        return
    stored = False
    for attempt, (ox, oz) in enumerate(((0.5, 0.75), (0.5, 0.6), (0.4, 0.7))):
        # standing on the pad, looking straight down: the potion (thrown 20 degrees above the view) bursts on the pad itself
        s.chat(f'tp @s {px + ox} {y + 0.13} {pz + oz} 180 90', 0.9)
        if not _hit_is(s, 'simplepads:potion_pad', f'potion throw {attempt + 1}'):
            continue
        _click(s, 0.4)
        s.chat(f'tp @s {px + 0.5} {y} {pz + 3.5} 180 20', 0.8)  # off the pad at once (else the ramp starts)
        stored = _test(s, f'if data block {px} {y} {pz} Potion', 'potion-stored')
        if stored:
            break
    s.check('potion pad: a REAL thrown Splash Potion of Swiftness bursting on the pad is stored (Potion)', stored)
    if not stored:
        return
    s.chat('effect clear @s', 0.3)
    time.sleep(1.0)
    s.check('potion pad: ready (active=true, no cooldown)', _test(s, f'if block {px} {y} {pz} simplepads:potion_pad[active=true,cooling=false]', 'potion-ready'))
    s.shot('1-ready')
    s.chat(f'tp @s {px + 0.5} {y + 0.13} {pz + 0.5} 180 30', 0.3)
    time.sleep(0.9)
    s.check('potion pad: standing on it gives Speed after about 1 s (25 % step)',
            _test(s, 'if entity @s[nbt={active_effects:[{id:"minecraft:speed"}]}]', 'potion-speed'))
    s.shot('2-standing')
    time.sleep(2.0)
    _say(s, 'if entity @s[nbt={active_effects:[{id:"minecraft:speed"}]}]', 'potion-speed-full')
    _say(s, f'if block {px} {y} {pz} simplepads:potion_pad[cooling=true]', 'potion-cooling')
    _check_said(s, 'potion pad: after 3 s the full effect stays and the pad goes into its cooldown (cooling=true)', ['potion-speed-full', 'potion-cooling'])
    s.chat(f'tp @s {px + 0.5} {y} {pz + 3.5} 180 25', 1.0)
    s.shot('3-cooling')
    s.chat('gamemode creative @s', 0.3)


def pads_spawn(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40, x0=-3, z0=-3, x1=20, z1=24)
    s.chat('effect give @s minecraft:resistance 120 255 true', 0.3)
    bx, bz = x + 14, z + 18
    s.chat(f'setblock {bx} {y} {bz} minecraft:red_bed[facing=south,part=foot]', 0.4)
    s.chat(f'setblock {bx} {y} {bz + 1} minecraft:red_bed[facing=south,part=head]', 0.4)
    s.chat(f'spawnpoint @s {bx} {y} {bz}', 0.5)
    tx, tz = x + 2, z + 3
    block, wait = 'simplepads:enderite_spawn_teleporter', 5
    s.chat(f'setblock {tx} {y} {tz} {block}', 0.5)
    if not _test(s, f'if block {tx} {y} {tz} {block}', 'tele-placed'):
        block, wait = 'simplepads:spawn_teleporter_tier_2', 20  # Enderite tier needs Simple Ores
        s.chat(f'setblock {tx} {y} {tz} {block}', 0.5)
    s.check(f'spawn teleporter placed ({block}, {wait} s)', _test(s, f'if block {tx} {y} {tz} {block}', 'tele-placed2'))
    on = f'tp @s {tx + 0.5} {y + 0.1} {tz + 0.5} 0 30'
    s.chat(on, 0.5)
    time.sleep(1.5)
    s.check('spawn teleporter: standing still charges it (active=true)', _test(s, f'if block {tx} {y} {tz} {block}[active=true]', 'tele-active'))
    s.shot('1-charging')
    time.sleep(max(1.0, wait - 3.5) + 2.5)
    land = _pos(s)
    s.check('spawn teleporter: the jump lands at the own spawn (the bed)', _near(land, (bx, y, bz), 3), f'now {land}, bed ({bx}, {y}, {bz})')
    s.shot('2-at-bed')
    # with a redstone signal: the world spawn
    s.chat(f'setblock {tx + 1} {y} {tz} minecraft:redstone_block', 0.5)
    s.chat(on, 0.5)
    time.sleep(wait + 3.5)
    land2 = _pos(s)
    s.check('spawn teleporter + redstone: the jump goes to the world spawn (away from pad and bed)',
            not _near(land2, (tx, y, tz), 8) and not _near(land2, (bx, y, bz), 4), f'now {land2}')
    s.shot('3-world-spawn')
    s.chat('gamemode creative @s', 0.3)


# ---------------------------------------------------------------- Simple Beacons
CONDUITS = [('strong_conduit', ['conduit_power', 'dolphins_grace']),
            ('mighty_conduit', ['conduit_power', 'dolphins_grace', 'night_vision']),
            ('evil_conduit', ['mining_fatigue', 'slowness']),
            ('mighty_evil_conduit', ['mining_fatigue', 'slowness', 'weakness', 'darkness'])]


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


def _pool(s, cx, cy, cz, conduit):
    """Glass box 9x9x9 around (cx, cy, cz) full of water, 5x5x5 prismarine frame (three rings of 16), conduit in the middle."""
    s.chat(f'fill {cx - 4} {cy - 4} {cz - 4} {cx + 4} {cy + 4} {cz + 4} minecraft:glass hollow', 0.6)
    s.chat(f'fill {cx - 3} {cy - 3} {cz - 3} {cx + 3} {cy + 3} {cz + 3} minecraft:water', 0.6)
    for a, b in (((cx - 2, cy - 2, cz), (cx + 2, cy + 2, cz)), ((cx - 2, cy, cz - 2), (cx + 2, cy, cz + 2)), ((cx, cy - 2, cz - 2), (cx, cy + 2, cz + 2))):
        s.chat(f'fill {a[0]} {a[1]} {a[2]} {b[0]} {b[1]} {b[2]} minecraft:prismarine', 0.5)
    s.chat(f'fill {cx - 1} {cy - 1} {cz - 1} {cx + 1} {cy + 1} {cz + 1} minecraft:water', 0.5)
    s.chat(f'setblock {cx} {cy} {cz} simplebeacons:{conduit}', 0.5)


def beacons_conduits(s):
    _prep(s, night_vision=False)
    x, y, z = _pos(s)
    x += 40
    s.chat(f'tp @s {x + 0.5} {y + 12} {z + 0.5}', 1.2)
    s.chat(f'fill {x - 6} {y} {z - 6} {x + 48} {y + 12} {z + 8} minecraft:air', 0.8)
    s.chat(f'fill {x - 6} {y - 1} {z - 6} {x + 48} {y - 1} {z + 8} {FLOOR}', 0.6)
    s.chat('simplebeacons config set evilBeaconGraceMinutes 0', 0.8)
    s.check('subtitles switched on through the real Options/Accessibility screen', _subtitles(s, True))
    try:
        cy = y + 4
        centers = []
        for i, (block, _) in enumerate(CONDUITS):
            cx = x + 2 + 12 * i
            _pool(s, cx, cy, z + 1, block)
            centers.append(cx)
        time.sleep(2.5)  # the frame scan runs every 40 ticks
        for (block, effects), cx in zip(CONDUITS, centers):
            s.chat('effect clear @s', 0.3)
            # swim next to the conduit (inside the water, outside the frame rings), looking at the eye
            s.chat(f'tp @s {cx + 2.5} {cy - 1} {z + 2.5} facing {cx + 0.5} {cy + 0.5 - 1.62:.2f} {z + 1.5}', 0.5)
            got = {}
            end = time.time() + 9
            while time.time() < end:
                for e in effects:
                    if not got.get(e):
                        got[e] = _test(s, f'if entity @s[nbt={{active_effects:[{{id:"minecraft:{e}"}}]}}]', f'{block}-{e}')
                if all(got.values()):
                    break
            s.check(f'{block}: active under water - player gets {", ".join(effects)}', all(got.values()), got)
            s.shot(f'{block}-underwater')
            time.sleep(2.0)
            s.shot(f'{block}-underwater-later')
        s.chat('time set midnight', 0.8)
        s.chat(f'tp @s {x + 20.5} {y + 9} {z - 10.5} facing {x + 20.5} {cy - 1.62:.2f} {z + 1.5}', 1.5)
        time.sleep(1.5)
        s.shot('overview-night')
    finally:
        s.chat('simplebeacons config set evilBeaconGraceMinutes 5', 0.6)
        s.chat('time set noon', 0.3)
        s.chat('effect clear @s', 0.3)
        _subtitles(s, False)
        s.chat(f'tp @s {x + 0.5} {y} {z - 8.5}', 0.8)


# ---------------------------------------------------------------- Simple Sandwiches
def _total(s, item):
    n = 0
    for v in s.st().get('inventory', []):
        if v and v.split(' x')[0] == item:
            n += int(v.rsplit(' x', 1)[1]) if ' x' in v else 1
    return n


def sandwiches_board(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    me = (x + 0.5, y, z + 0.5)
    bx, bz = x, z + 2
    s.chat(f'setblock {bx} {y} {bz} simplesandwiches:oak_cutting_board[facing=north]', 0.5)
    for slot, stack in enumerate(['minecraft:bread 3', 'simplesandwiches:knife', 'minecraft:cooked_beef', 'minecraft:apple', 'simplesandwiches:cheese_slice']):
        s.chat(f'item replace entity @s hotbar.{slot} with {stack}', 0.25)
    s.chat('item replace entity @s hotbar.8 with minecraft:air', 0.25)
    s.chat('item replace entity @s weapon.offhand with simplesandwiches:butter_slice 2', 0.3)
    board = 'simplesandwiches:oak_cutting_board'
    if not _aim(s, me, (bx + 0.5, y + 0.08, bz + 0.5), board, 'board'):
        return

    def stage(name, buttered=False):
        nbt = f'{{Stage:"{name}"' + (',Buttered:1b' if buttered else '') + '}'
        return _test(s, f'if data block {bx} {y} {bz} {nbt}', f'board-{name}')

    def click(key, tag):
        s.io.key(key, 0.4)
        if not _hit_is(s, board, tag):
            _aim(s, me, (bx + 0.5, y + 0.08, bz + 0.5), board, tag)
        _click(s, 0.6)

    click('1', 'board bread')
    s.check('board: bread laid on (stage loaf, bread 3 -> 2)', stage('loaf') and _total(s, 'minecraft:bread') == 2, f'bread {_total(s, "minecraft:bread")}')
    s.shot('1-bread')
    click('2', 'board knife')
    s.check('board: the knife opens the loaf (stage open)', stage('open'))
    s.shot('2-open')
    click('2', 'board butter')
    s.check('board: butter from the off hand (Buttered)', stage('open', True))
    s.shot('3-buttered')
    for key, item in (('3', 'minecraft:cooked_beef'), ('4', 'minecraft:apple'), ('5', 'simplesandwiches:cheese_slice')):
        click(key, f'board {item}')
    left = [_total(s, i) for i in ('minecraft:cooked_beef', 'minecraft:apple', 'simplesandwiches:cheese_slice')]
    s.check('board: beef, apple and cheese slice taken (3 ingredients)', left == [0, 0, 0], f'left {left}')
    s.shot('4-filled')
    click('9', 'board close')
    s.check('board: the empty hand closes the sandwich (stage closed)', stage('closed', True))
    s.shot('5-closed')
    click('9', 'board take')
    inv = s.st().get('inventory', [])
    got = len(inv) > 8 and (inv[8] or '').startswith('simplesandwiches:sandwich')
    s.check('board: the empty hand takes the sandwich (board empty)', got and stage('empty'), inv[8] if len(inv) > 8 else inv)
    s.chat(f'tp @s {me[0]} {y} {me[2]} 0 40', 1.0)
    s.shot('6-sandwich-in-hand')
    s.key('e', 1.2)
    st = s.wait_screen('InventoryScreen', 8)
    slot = s.slot_xy(st, cslot=8, own=True) if 'InventoryScreen' in st.get('screen', '') else None
    if slot and 'sandwich' in slot.get('stack', ''):
        s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
        time.sleep(0.4)
        s.io.move(*s.abs(st, slot['x'], slot['y']))
        time.sleep(0.9)
        s.shot('7-sandwich-tooltip')
    else:
        s.check('inventory shows the sandwich in hotbar slot 9', False, slot)
    s.close()
    _center_pointer(s)
    s.chat('gamemode creative @s', 0.3)


def sandwiches_cut(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'setblock {x + 2} {y} {z + 2} simplesandwiches:cheese_block', 0.4)
    s.chat(f'setblock {x - 2} {y} {z + 2} simplesandwiches:butter_block', 0.4)
    if not _hand(s, 1, 'simplesandwiches:knife', 'cut'):
        return
    s.chat('item replace entity @s weapon.offhand with minecraft:air', 0.3)
    for block, bx, face in (('cheese', x + 2, 'up'), ('butter', x - 2, 'north')):
        want_block = f'simplesandwiches:{block}_block'
        before = _total(s, f'simplesandwiches:{block}_slice')
        clicks, got = 0, 0
        while got < 4 and clicks < 8:
            # the top sinks with every slice from above (shape = slices/16 high): aim at the current surface
            target = (bx + 0.5, y + (16 - got) / 16 - 0.03, z + 2.5) if face == 'up' else (bx + 0.5, y + 0.4, z + 2.02)
            if not _aim(s, me, target, want_block, f'{block} cut {clicks + 1}'):
                break
            _click(s, 0.5)
            clicks += 1
            got = _total(s, f'simplesandwiches:{block}_slice') - before
        want = f'slices=12,cut={face}'
        ok_state = _test(s, f'if block {bx} {y} {z + 2} {want_block}[{want}]', f'{block}-state')
        s.check(f'{block} block: 4 real knife clicks on the {face} face, one slice each ({want})',
                ok_state and got == 4 and clicks == 4, f'{got} slices after {clicks} clicks, state ok {ok_state}')
        s.shot(f'{block}-cut')
    s.chat('gamemode creative @s', 0.3)


def sandwiches_cauldron(s):
    _prep(s, 'survival')
    x, y, z = _arena(s, 40)
    s.chat('simplesandwiches config set butterTicks 800', 0.8)
    s.chat('simplesandwiches config set cheeseTicks 800', 0.8)
    try:
        cx, cz = x, z + 3
        me = (x + 0.5, y, z + 0.5)
        # butter first (a butter cauldron ripens on to cheese by itself): an empty hand takes a butter block
        s.chat(f'setblock {cx + 3} {y} {cz} simplesandwiches:milk_cauldron[content=butter,stage=0]', 0.4)
        s.chat('item replace entity @s hotbar.7 with minecraft:air', 0.3)
        s.io.key('8', 0.4)
        if _aim(s, (x + 3.5, y, z + 0.5), (cx + 3.5, y + 0.6, cz + 0.5), 'simplesandwiches:milk_cauldron', 'cauldron butter'):
            _click(s, 0.6)
            _say(s, f'if block {cx + 3} {y} {cz} minecraft:cauldron', 'butter-taken')
            _say(s, 'if items entity @s weapon.mainhand simplesandwiches:butter_block', 'butter-hand')
            _check_said(s, 'an empty hand takes the butter (cauldron empty, butter block in the hand)', ['butter-taken', 'butter-hand'])
        s.chat(f'setblock {cx} {y} {cz} minecraft:cauldron', 0.4)
        if not _hand(s, 5, 'minecraft:milk_bucket', 'cauldron'):
            return
        if not _aim(s, me, (cx + 0.5, y + 0.6, cz + 0.5), 'minecraft:cauldron', 'cauldron milk'):
            return
        _click(s, 0.6)
        t0 = time.time()
        filled = _test(s, f'if block {cx} {y} {cz} simplesandwiches:milk_cauldron[content=milk,stage=0]', 'milk0')
        s.check('REAL milk bucket click fills the empty cauldron (content=milk, stage 0)', filled)
        if not filled:
            return
        s.shot('1-milk-stage0')
        # 4 stages each: milk 1-3 (200 ticks apart), butter 0-3 (200 ticks apart), then cheese
        steps = [('milk', 1), ('milk', 2), ('milk', 3), ('butter', 0), ('butter', 1), ('butter', 2), ('butter', 3), ('cheese', 0)]
        seen = [('milk0', 0.0)]
        k = 0
        end = t0 + 110
        while k < len(steps) and time.time() < end:
            content, stage = steps[k]
            if _test(s, f'if block {cx} {y} {cz} simplesandwiches:milk_cauldron[content={content},stage={stage}]', f'{content}{stage}'):
                seen.append((f'{content}{stage}', round(time.time() - t0, 1)))
                if stage in (0, 3) or content == 'milk':
                    s.shot(f'{len(seen)}-{content}-stage{stage}')
                k += 1
        s.check('cauldron ripens by itself through every stage: milk 0-3 -> butter 0-3 -> cheese (butterTicks/cheeseTicks 800)',
                k == len(steps), f'seen {seen}')
        s.chat('item replace entity @s hotbar.8 with minecraft:air', 0.3)
        s.io.key('9', 0.4)
        if k == len(steps) and _aim(s, me, (cx + 0.5, y + 0.6, cz + 0.5), 'simplesandwiches:milk_cauldron', 'cauldron cheese'):
            _click(s, 0.6)
            _say(s, f'if block {cx} {y} {cz} minecraft:cauldron', 'cheese-taken')
            _say(s, 'if items entity @s weapon.mainhand simplesandwiches:cheese_block', 'cheese-hand')
            _check_said(s, 'an empty hand takes the cheese (cauldron empty, cheese block in the hand)', ['cheese-taken', 'cheese-hand'])
        s.shot('9-taken')
    finally:
        s.chat('simplesandwiches config set butterTicks 6000', 0.6)
        s.chat('simplesandwiches config set cheeseTicks 6000', 0.6)
        s.chat('gamemode creative @s', 0.3)


SCENARIOS = {
    'n130-pads-flight': pads_flight,
    'n130-pads-elytra': pads_elytra,
    'n130-pads-launch': pads_launch,
    'n130-pads-potion': pads_potion,
    'n130-pads-spawn': pads_spawn,
    'n130-beacons-conduits': beacons_conduits,
    'n130-sandwiches-board': sandwiches_board,
    'n130-sandwiches-cut': sandwiches_cut,
    'n130-sandwiches-cauldron': sandwiches_cauldron,
}
