"""simplepads-3: launchpad start and flypad flight in the real game picture, with REAL clicks and keys (b-client60p, scenario for
modules/simpleinterfaces/tools/ui_probe.py).

Scenes:
  1 A row of the pads (launchpads I-III, flypads I-III, elytra pad, potion pad, spawn teleporter, chunk loader) for the picture.
  2 Launchpad I: loaded with wind charges by a real right click (one charge) and Shift + right click (the rest of the stack, capacity 4),
    read back from the block entity (Charges). Survival, standing on it for 3 s (LAUNCH_TICKS 60) starts the player upwards; the highest
    position while flying is sampled (Resistance 255 against the fall). Pictures: loaded pad, mid-flight looking down.
  3 Flypad I: survival, standing in its area grants flight (tag simplebuilding.flypad_flight, abilities.mayfly), a real double tap on Space
    starts flying, leaving the area takes the flight away again.
# NUR BESITZER: the start feel (strength per charge), the flight field edges (sparks), sounds; the potion pad and spawn teleporter only as picture.
"""
import math
import time

ROW = ['launchpad', 'netherite_launchpad', 'enderite_launchpad', 'flypad', 'reinforced_flypad', 'stellar_flypad', 'elytra_pad',
       'potion_pad', 'spawn_teleporter', 'chunk_loader']


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _fpos(s):
    return tuple(float(v) for v in s.st()['pos'].replace(',', ' ').split())


def _aim(s, f, t, wait=0.8):
    dx, dy, dz = t[0] - f[0], t[1] - (f[1] + 1.62), t[2] - f[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    s.chat(f'tp @s {f[0]:.2f} {f[1]} {f[2]:.2f} {yaw:.1f} {pitch:.1f}', wait)


def _sense(s, cond, slot=26):
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.4)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def _double_tap(s, name):
    """Two quick real presses (XTest): the driver's key()/key_down() pause 0.2-0.3 s per edge, too slow for Vanilla's double tap."""
    code = s.io._code(name)
    for _ in range(2):
        for down, pause in ((1, 0.06), (0, 0.08)):
            s.io.xtst.XTestFakeKeyEvent(s.io.d, code, down, 0)
            s.io.x11.XFlush(s.io.d)
            time.sleep(pause)


def _row(s, x, y, z):
    for i, b in enumerate(ROW):
        s.chat(f'setblock {x - 9 + 2 * i} {y} {z + 30} simplepads:{b}', 0.3)
    time.sleep(1.0)
    s.key('F1', 0.5)
    _aim(s, (x + 0.5, y + 4, z + 22.5), (x + 0.5, y, z + 30.5), 1.5)
    s.shot('pad-row')
    s.key('F1', 0.5)


def _launch(s, x, y, z):
    px, pz = x, z + 3
    s.chat(f'setblock {px} {y} {pz} simplepads:launchpad', 0.4)
    s.chat('item replace entity @s hotbar.0 with minecraft:wind_charge 4', 0.3)
    s.key('1', 0.3)
    me = (x + 0.5, y, z + 0.5)
    _aim(s, me, (px + 0.5, y + 0.03, pz + 0.5), 1.0)
    s.io.right_click()
    time.sleep(0.6)
    s.check('launchpad: a real right click with wind charges loads one charge', _sense(s, f'if data block {px} {y} {pz} {{Charges:1}}'), 'Charges 1')
    s.io.key_down('Shift_L')
    try:
        time.sleep(0.3)
        s.io.right_click()
    finally:
        s.io.key_up('Shift_L')
    time.sleep(0.6)
    s.check('launchpad: Shift + right click loads the rest up to the capacity 4', _sense(s, f'if data block {px} {y} {pz} {{Charges:4}}'), 'Charges 4')
    s.key('F1', 0.5)
    s.shot('launchpad-loaded')
    s.key('F1', 0.5)
    s.chat('gamemode survival @s', 0.6)
    s.chat('effect give @s minecraft:resistance 60 255 true', 0.3)
    s.chat(f'tp @s {px + 0.5} {y + 0.1} {pz + 0.5} 0 80', 0.4)
    start = y
    top, shot = start, False
    end = time.time() + 9
    while time.time() < end:
        cur = _fpos(s)[1]
        top = max(top, cur)
        if cur > start + 4 and not shot:
            s.key('F1', 0.2)
            s.shot('launch-flight-looking-down')
            s.key('F1', 0.2)
            shot = True
        if top > start + 4 and cur <= start + 0.5:
            break
        time.sleep(0.15)
    s.check('launchpad: standing on it 3 s starts the player upwards', top >= start + 4, f'highest y {top:.1f} (start {start})')
    s.check('launchpad: the start used up the charges', _sense(s, f'if data block {px} {y} {pz} {{Charges:0}}'), 'Charges 0')
    s.chat('gamemode creative @s', 0.5)


def _fly(s, x, y, z):
    fx, fz = x + 6, z + 3
    s.chat(f'setblock {fx} {y} {fz} simplepads:flypad', 0.4)
    s.chat('gamemode survival @s', 0.6)
    s.chat('effect give @s minecraft:resistance 60 255 true', 0.3)
    s.chat(f'tp @s {fx + 0.5} {y + 0.1} {fz + 0.5} 0 0', 1.5)
    time.sleep(1.0)
    granted = _sense(s, 'if entity @s[tag=simplebuilding.flypad_flight,nbt={abilities:{mayfly:1b}}]')
    s.check('flypad: standing on it grants flight (tag + mayfly)', granted, 'simplebuilding.flypad_flight, abilities.mayfly')
    _double_tap(s, 'space')  # Vanilla toggles flight when the second press comes within 7 ticks of the first
    time.sleep(0.4)
    s.io.key_down('space')
    time.sleep(0.6)
    s.io.key_up('space')
    flying = _sense(s, 'if entity @s[nbt={abilities:{flying:1b}}]')
    s.check('flypad: a real double tap on Space starts flying', flying, f'abilities.flying, y now {_fpos(s)[1]:.1f}')
    s.key('F1', 0.5)
    s.chat(f'tp @s {fx + 0.5} {y + 2.5} {fz - 2.5} 0 30', 1.0)
    s.shot('flypad-flying')
    s.key('F1', 0.5)
    s.chat(f'tp @s {fx + 60.5} {y} {fz + 0.5} 0 0', 1.5)
    time.sleep(1.0)
    gone = _sense(s, 'if entity @s[tag=!simplebuilding.flypad_flight,nbt={abilities:{mayfly:0b}}]')
    s.check('flypad: leaving the area takes the flight away', gone, 'no tag, mayfly 0')
    s.chat('gamemode creative @s', 0.5)


def pads(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    s.io.move(*s.abs(s.st(), 640, 360))
    try:
        for part in (_row, _launch, _fly):
            try:
                part(s, x, y, z)
            except Exception as e:  # one failing part must not stop the others
                s.check(f'{part.__name__.strip("_")} part ran without exception', False, repr(e)[:300])
            s.io.key_up('Shift_L')
            s.io.key_up('space')
            s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.0)
    finally:
        s.chat('effect clear @s', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simplepads': pads}
