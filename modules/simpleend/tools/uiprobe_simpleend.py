"""simpleend-6: the End woods' boats, signs and the End paintings in the real game picture, with REAL clicks (b-client60p, scenario for
modules/simpleinterfaces/tools/ui_probe.py). The client smoke shows the garden, trees and shelves; these three were missing.

Scenes (on the flat overworld, daylight - the look of the items, not the End light):
  1 Boats: a water pool with the ashwood/duskwood boat and chest boat (summoned); a real right click on the ashwood boat boards it
    (player NBT RootVehicle), picture from the boat in third person, Shift leaves it.
  2 Signs: an ashwood sign placed by a real right click opens the sign editor, "probe" is typed with the real keyboard and Done clicked;
    the block's front text holds it. Wall signs and hanging signs of both woods placed by command for the picture.
  3 Paintings: survival, three End Paintings hung by real right clicks on an end stone wall (the item picks a variant from the
    simpleend:end_paintings tag that fits); the hand is empty afterwards and painting entities hang on the wall. Picture of the wall.
# NUR BESITZER: boat/sign textures, the animated paintings (EndPaintingAnim, 4-frame loops), the hanging sign chains.
"""
import math
import time


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


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


def _boats(s, x, y, z):
    s.chat(f'fill {x - 4} {y - 1} {z + 3} {x + 4} {y - 1} {z + 9} minecraft:water', 0.5)
    boats = ['ashwood_boat', 'ashwood_chest_boat', 'duskwood_boat', 'duskwood_chest_boat']
    for i, b in enumerate(boats):
        s.chat(f'summon simpleend:{b} {x - 3 + 2 * i + 0.5} {y - 0.4} {z + 6.5} {{Tags:["uipend"],Rotation:[90f,0f]}}', 0.4)
    time.sleep(1.5)
    cond = ' '.join(f'if entity @e[type=simpleend:{b},tag=uipend]' for b in boats)
    s.check('the four End boats float in the pool', _sense(s, cond), ', '.join(boats))
    s.key('F1', 0.5)
    _aim(s, (x + 0.5, y + 3, z + 0.5), (x + 0.5, y - 0.5, z + 6.5), 1.5)
    s.shot('boats')
    s.key('F1', 0.5)
    # board the ashwood boat with a real right click (empty hand)
    s.chat('item replace entity @s hotbar.8 with minecraft:air', 0.3)
    s.key('9', 0.3)
    # N60P: (x-2.5, z+3.5) is a water cell of the pool - the creative player sank a block before the click and the ray (aimed from the
    # bank height) went under the boat (run 1: crosshair on None). A stepping stone in the pool keeps the feet where the aim assumes,
    # 2 blocks from the boat (inside the 3-block entity reach)
    s.chat(f'setblock {x - 3} {y - 1} {z + 4} minecraft:stone', 0.4)
    _aim(s, (x - 2.5, y, z + 4.5), (x - 2.5, y - 0.2, z + 6.5), 1.0)
    time.sleep(0.5)
    aimed = s.st().get('hitEntity')
    s.io.right_click()
    time.sleep(1.0)
    riding = _sense(s, 'if entity @s[nbt={RootVehicle:{}}]')
    s.check('a real right click boards the ashwood boat', riding, f'player NBT RootVehicle (crosshair was on {aimed})')
    if riding:
        s.key('F5', 0.5)
        s.key('F1', 0.5)
        time.sleep(0.8)
        s.shot('in-the-boat')
        s.key('F1', 0.5)
        s.key('F5', 0.5)
        s.key('F5', 0.5)
        s.io.key_down('Shift_L')
        time.sleep(0.6)
        s.io.key_up('Shift_L')
        time.sleep(0.5)
    s.chat(f'tp @s {x + 0.5} {y} {z + 0.5} 0 0', 1.0)


def _signs(s, x, y, z):
    sx, sz = x, z + 14
    s.chat('item replace entity @s hotbar.0 with simpleend:ashwood_sign', 0.3)
    s.key('1', 0.3)
    _aim(s, (sx + 0.5, y, sz - 2.5), (sx + 0.5, y - 0.1, sz + 0.5), 1.0)
    s.io.right_click()
    st = s.wait_screen('Sign', 6)
    opened = 'Sign' in st.get('screen', '')
    s.check('placing the ashwood sign with a real right click opens the sign editor', opened, st.get('screen'))
    if opened:
        s.type_text('probe')
        time.sleep(0.4)
        s.shot('sign-editor')
        if not s.click_widget('Done', name='Done button'):
            s.close()
        time.sleep(0.8)
    written = _sense(s, f'if block {sx} {y} {sz} simpleend:ashwood_sign if data block {sx} {y} {sz} front_text{{messages:["probe"]}}')
    s.check('the sign stands and its front text holds the typed "probe"', written, 'front_text.messages contains "probe"')
    # the other sign kinds for the picture: a plank wall with wall signs, hanging signs under a beam
    s.chat(f'fill {sx + 2} {y} {sz + 1} {sx + 5} {y + 3} {sz + 1} simpleend:duskwood_planks', 0.4)
    s.chat(f'setblock {sx + 2} {y + 1} {sz} simpleend:ashwood_wall_sign[facing=north]', 0.3)
    s.chat(f'setblock {sx + 3} {y + 1} {sz} simpleend:duskwood_wall_sign[facing=north]', 0.3)
    s.chat(f'setblock {sx + 4} {y + 2} {sz} simpleend:ashwood_wall_hanging_sign[facing=east]', 0.3)
    s.chat(f'setblock {sx + 5} {y + 2} {sz} simpleend:duskwood_wall_hanging_sign[facing=east]', 0.3)
    s.chat(f'setblock {sx - 2} {y + 3} {sz} simpleend:ashwood_planks', 0.3)
    s.chat(f'setblock {sx - 2} {y + 2} {sz} simpleend:duskwood_hanging_sign', 0.3)
    s.chat(f'setblock {sx - 3} {y} {sz} simpleend:duskwood_sign[rotation=0]', 0.3)
    s.key('F1', 0.5)
    _aim(s, (sx + 1.5, y + 1.5, sz - 5.5), (sx + 1.5, y + 1, sz + 0.5), 1.5)
    s.shot('signs')
    s.key('F1', 0.5)


def _paintings(s, x, y, z):
    wz = z + 26
    s.chat(f'fill {x - 12} {y} {wz} {x + 12} {y + 8} {wz} minecraft:end_stone', 0.6)
    s.chat('kill @e[type=minecraft:painting,distance=..40]', 0.3)
    s.chat('gamemode survival @s', 0.6)
    s.chat('item replace entity @s hotbar.0 with simpleend:end_painting 3', 0.3)
    s.key('1', 0.3)
    for px in (x - 7, x, x + 7):
        _aim(s, (px + 0.5, y, wz - 3.5), (px + 0.5, y + 2.5, wz + 0.01), 1.0)
        s.io.right_click()
        time.sleep(0.6)
    hand = s.st().get('inventory', [''])[0] or ''
    hung = _sense(s, f'if entity @e[type=minecraft:painting,x={x - 12},y={y},z={wz - 2},dx=24,dy=9,dz=3]')
    s.check('three End Paintings hung by real right clicks (hand empty, paintings on the wall)', hung and not hand.startswith('simpleend:end_painting'),
            f'hand {hand!r}, painting entity on the wall: {hung}')
    s.chat('gamemode creative @s', 0.5)
    s.key('F1', 0.5)
    _aim(s, (x + 0.5, y + 3, wz - 14.5), (x + 0.5, y + 3, wz), 1.5)
    time.sleep(1.0)
    s.shot('paintings')
    s.key('F1', 0.5)


def end(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    s.io.move(*s.abs(s.st(), 640, 360))
    try:
        for part in (_boats, _signs, _paintings):
            try:
                part(s, x, y, z)
            except Exception as e:  # one failing part must not stop the others
                s.check(f'{part.__name__.strip("_")} part ran without exception', False, repr(e)[:300])
            s.close()
            s.io.key_up('Shift_L')
    finally:
        s.chat('kill @e[tag=uipend]', 0.3)
        s.chat('clear @s', 0.3)
        s.chat('gamemode creative @s', 0.5)


SCENARIOS = {'simpleend': end}
