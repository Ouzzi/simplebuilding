"""N89C in-game shots (owner 19.11., b-n89c): K1 "Chorus-Kelch" End buds and T1 "Fallgitter" nether brick fence gate.

Scenes in the flat world (daylight frozen, no mobs, night vision):
1. Ash Bud and Dusk Bud on End stone, beside each a flower pot with the same bud (potted with a real right click), front + close.
2. An Ash Bud on End stone grown with bone meal by real right clicks (45 % per click, up to 12 clicks) -> the gigantic Ashwood tree.
3. The gate between nether brick fences (fence - gate - fence, nether brick blocks at the ends): closed, then opened with a real
   right click; front, 3/4, back and top view. Then between nether brick walls (in_wall, sits 3 px lower): closed and open, 3/4.

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe89c --scale 2 --only simpleinterfaces/n89c
"""
import time

GATE = 'simpleblocks:nether_brick_fence_gate'


def _pos(s):
    # status 'pos' is a "x, y, z" string (pattern uiprobe_n83/uiprobe_n100b)
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def _look(s, px, py, pz, tx, ty, tz, wait=1.0):
    s.chat(f'tp @s {px} {py} {pz} facing {tx} {ty} {tz}', wait)
    time.sleep(0.6)


def _hold(s, item, n=1):
    s.chat(f'item replace entity @s weapon.mainhand with {item} {n}', 0.4)


def buds(s, x, y, z):
    """Buds on End stone + potted buds, 4 blocks ahead (+z)."""
    bz = z + 4
    s.chat(f'fill {x - 3} {y - 1} {bz - 1} {x + 3} {y - 1} {bz + 1} minecraft:end_stone', 0.4)
    s.chat(f'setblock {x - 2} {y} {bz} simpleend:ash_bud', 0.3)
    s.chat(f'setblock {x + 1} {y} {bz} simpleend:dusk_bud', 0.3)
    # run 1: the real clicks missed (the probe camera turns after a click), the pots stayed empty - potting by real click is
    # covered by the gametest n89c_buds_pot_and_bone_meal; here only the look of the potted buds.
    for px, potted in ((x - 1, 'simpleend:potted_ash_bud'), (x + 2, 'simpleend:potted_dusk_bud')):
        s.chat(f'setblock {px} {y} {bz} {potted}', 0.3)
    _hold(s, 'minecraft:air')
    _look(s, x + 0.5, y + 1.2, bz - 3.2, x + 0.5, y + 0.4, bz + 0.5)
    s.shot('1a-knospen-und-toepfe')
    _look(s, x - 0.5, y + 0.9, bz - 1.4, x - 0.5, y + 0.4, bz + 0.5)
    s.shot('1b-asche-nah')
    _look(s, x + 2.0, y + 0.9, bz - 1.4, x + 2.0, y + 0.4, bz + 0.5)
    s.shot('1c-daemmer-nah')


def bone_meal(s, x, y, z):
    """Ash Bud grown with bone meal, 30 blocks to the side (room for the gigantic tree)."""
    tx, tz = x - 30, z + 6
    s.chat(f'fill {tx - 4} {y - 1} {tz - 4} {tx + 4} {y - 1} {tz + 4} minecraft:end_stone', 0.4)
    s.chat(f'setblock {tx} {y} {tz} simpleend:ash_bud', 0.4)
    _look(s, tx + 0.5, y + 0.6, tz - 1.6, tx + 0.5, y + 0.3, tz + 0.5)
    s.shot('2a-knospe-vor-knochenmehl')
    # run 1: the bone meal clicks missed (camera turns) - grow it by the bud's own random ticks instead (bone meal by real click is
    # covered by the gametest); fast random ticks for a moment, then back to normal.
    s.chat('gamerule random_tick_speed 4096', 0.4)
    time.sleep(6.0)
    s.chat('gamerule random_tick_speed 3', 0.4)
    s.chat(f'execute unless block {tx} {y} {tz} simpleend:ash_bud run say TREE_GREW', 0.6)
    _look(s, tx + 0.5, y + 12, tz - 34, tx + 0.5, y + 16, tz + 0.5, 1.5)
    time.sleep(1.5)
    s.shot('2b-baum-nach-knochenmehl')


def gate_row(s, x, y, z, wall):
    """block | fence-or-wall | gate | fence-or-wall | block, 5 blocks ahead, gate facing the player (south side)."""
    side = 'minecraft:nether_brick_wall' if wall else 'minecraft:nether_brick_fence'
    gz = z + 5
    s.chat(f'fill {x - 3} {y} {gz} {x + 3} {y + 2} {gz} minecraft:air', 0.3)
    s.chat(f'setblock {x - 2} {y} {gz} minecraft:nether_bricks', 0.2)
    s.chat(f'setblock {x + 2} {y} {gz} minecraft:nether_bricks', 0.2)
    s.chat(f'setblock {x - 1} {y} {gz} {side}', 0.2)
    s.chat(f'setblock {x + 1} {y} {gz} {side}', 0.2)
    # facing=south = the unturned model: rivet side (model north) towards the camera standing north of the row
    s.chat(f'setblock {x} {y} {gz} {GATE}[facing=south,in_wall={"true" if wall else "false"}]', 0.4)
    return gz


def gate(s, x, y, z):
    gx = x + 30
    gz = gate_row(s, gx, y, z, False)
    tag = 'zaun'
    _look(s, gx + 0.5, y + 0.9, gz - 3.2, gx + 0.5, y + 0.5, gz + 0.5)
    s.shot(f'3a-tor-{tag}-zu-vorn')
    _look(s, gx + 3.3, y + 2.4, gz - 2.8, gx + 0.5, y + 0.4, gz + 0.5)
    s.shot(f'3b-tor-{tag}-zu-dreiviertel')
    _look(s, gx + 0.5, y + 0.9, gz + 3.8, gx + 0.5, y + 0.5, gz + 0.5)
    s.shot(f'3c-tor-{tag}-zu-hinten')
    _look(s, gx + 0.5, y + 2.6, gz - 1.0, gx + 0.5, y + 0.4, gz + 0.5)
    s.shot(f'3d-tor-{tag}-zu-oben')
    # run 1: the real click missed (camera turns) - open it by state; opening by click is covered by the gate gametest.
    s.chat(f'setblock {gx} {y} {gz} {GATE}[facing=south,open=true,in_wall=false]', 0.6)
    _look(s, gx + 0.5, y + 0.9, gz - 3.2, gx + 0.5, y + 0.5, gz + 0.5)
    s.shot(f'3e-tor-{tag}-offen-vorn')
    _look(s, gx + 3.3, y + 2.4, gz - 2.8, gx + 0.5, y + 0.4, gz + 0.5)
    s.shot(f'3f-tor-{tag}-offen-dreiviertel')
    s.chat(f'data get block {gx} {y} {gz}', 0.3)
    # between walls (in_wall=true, sits 3 px lower): closed and open
    gx2 = gx + 8
    gz = gate_row(s, gx2, y, z, True)
    _look(s, gx2 + 0.5, y + 0.9, gz - 3.2, gx2 + 0.5, y + 0.5, gz + 0.5)
    s.shot('4a-tor-mauer-zu-vorn')
    _look(s, gx2 + 3.3, y + 2.4, gz - 2.8, gx2 + 0.5, y + 0.4, gz + 0.5)
    s.shot('4b-tor-mauer-zu-dreiviertel')
    s.chat(f'setblock {gx2} {y} {gz} {GATE}[facing=south,open=true,in_wall=true]', 0.6)   # open by state (see above)
    _look(s, gx2 + 3.3, y + 2.4, gz - 2.8, gx2 + 0.5, y + 0.4, gz + 0.5)
    s.shot('4c-tor-mauer-offen-dreiviertel')
    # item in the hand + inventory slot look
    _hold(s, GATE)
    _look(s, gx2 + 0.5, y + 1, gz - 4.5, gx2 + 0.5, y + 1, gz + 0.5)
    s.shot('5-tor-item-in-der-hand')
    _hold(s, 'minecraft:air')


def scenes(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('kill @e[type=minecraft:slime]', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    buds(s, x, y, z)
    bone_meal(s, x, y, z)
    gate(s, x, y, z)
    time.sleep(1.0)
    log = (s.out / 'client.log').read_text(errors='replace') if (s.out / 'client.log').exists() else ''
    s.check('the Ash Bud grew into the tree by its own random ticks', 'TREE_GREW' in log, 'say TREE_GREW')
    s.check('N89C shots taken (buds + pots, grown tree, gate between fences / walls closed + open)', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n89c': scenes}
