"""N100 in-game shots: octet pillar inner faces (pale oak log octets), pressed netherite pressure plate, axe on a waxed pumpkin
(normal click and sneaking: is the hand swing played?).

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe100 --scale 2 --only simpleinterfaces/n100
"""
import time


def _pos(s):
    # status 'pos' is a "x, y, z" string (pattern uiprobe_n83); run 1 read it as a list and built in the air
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


def scenes(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    b = 'simpleoctets:pale_oak_log_octet'
    # 1) octet pillars: a thin 1-cell column (o000+o010) and a notched column (7 of 8 cells), 3 blocks high, 4 blocks ahead
    for dy in range(3):
        s.chat(f'setblock {x - 2} {y + dy} {z + 4} {b}[o000=true,o010=true]', 0.2)
        s.chat(f'setblock {x + 1} {y + dy} {z + 4} {b}[o000=true,o100=true,o001=true,o101=true,o010=true,o110=true,o011=true,o111=false]', 0.2)
        s.chat(f'setblock {x + 4} {y + dy} {z + 4} {b}[o000=true,o100=true,o010=true,o110=true]', 0.2)
    s.chat(f'tp @s {x + 1.5} {y + 1} {z + 1.5} facing {x + 1.5} {y + 1.5} {z + 4.5}', 1.0)
    time.sleep(1.0)
    s.shot('1-octet-saeulen-vorn')
    s.chat(f'tp @s {x + 2.8} {y + 4.2} {z + 5.8} facing {x + 1.5} {y + 2.5} {z + 4.5}', 1.0)
    time.sleep(0.8)
    s.shot('2-octet-saeulen-kerbe-oben')
    # 2) netherite pressure plate: released next to pressed (an armor stand stands on the right one)
    px, pz = x + 10, z
    s.chat(f'setblock {px} {y} {pz} simplebuilding:netherite_pressure_plate', 0.2)
    s.chat(f'setblock {px + 2} {y} {pz} simplebuilding:netherite_pressure_plate', 0.2)
    s.chat(f'setblock {px + 4} {y} {pz} simplebuilding:diamond_pressure_plate', 0.2)
    s.chat(f'summon minecraft:armor_stand {px + 2.5} {y} {pz + 0.5} {{Invisible:1b,NoGravity:0b}}', 0.2)
    s.chat(f'summon minecraft:armor_stand {px + 4.5} {y} {pz + 0.5} {{Invisible:1b,NoGravity:0b}}', 0.2)
    s.chat(f'tp @s {px + 2.5} {y + 2.2} {pz - 2.0} facing {px + 2.5} {y} {pz + 0.5}', 1.5)
    time.sleep(1.0)
    s.shot('3-netherit-platte-frei-gedrueckt-diamant')
    s.chat('kill @e[type=minecraft:armor_stand]', 0.3)
    # 3) axe on a waxed pumpkin: real right click, shots right after the click (swing ~6 ticks)
    wx, wz = x - 8, z
    s.chat(f'setblock {wx} {y} {wz + 2} simpleblocks:waxed_pumpkin', 0.3)
    s.chat('item replace entity @s weapon.mainhand with minecraft:iron_axe', 0.3)
    s.chat(f'tp @s {wx + 0.5} {y} {wz + 0.5} facing {wx + 0.5} {y + 0.5} {wz + 2.5}', 1.0)
    time.sleep(0.6)
    s.shot('4-axt-vorher')
    for label, sneak in (('normal', False), ('schleichen', True)):
        if sneak:
            s.io.key_down('Shift_L')
            time.sleep(0.3)
        s.io.right_click()
        time.sleep(0.06)
        s.shot(f'5-axt-{label}-kurz-nach-klick')
        time.sleep(0.12)
        s.shot(f'6-axt-{label}-spaeter')
        if sneak:
            s.io.key_up('Shift_L')
        time.sleep(1.0)
        s.chat(f'setblock {wx} {y} {wz + 2} simpleblocks:waxed_pumpkin', 0.3)
        s.chat(f'tp @s {wx + 0.5} {y} {wz + 0.5} facing {wx + 0.5} {y + 0.5} {wz + 2.5}', 0.8)
    s.check('N100 shots taken (octet pillars, netherite plate, axe on waxed pumpkin)', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n100': scenes}
