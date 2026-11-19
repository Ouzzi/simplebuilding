"""N100B in-game shots (owner 19.11.): octet pillar inner faces show straight LINES along the grain (one middle row of the end
grain stretched lengthwise), not the rings; the 1 px bark line only on the outer edge. Scenes from uiprobe_n100.py (b-fix100):
a thin column, a notched 2x2 pillar (7 of 8 octets, 3 blocks high) and a half pillar - pale oak (owner's picture), oak and a
crimson stem (stems use the same template), plus stripped oak (no bark: lines over the whole face).

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe100b --scale 2 --only simpleinterfaces/n100b
"""
import time


def _pos(s):
    # status 'pos' is a "x, y, z" string (pattern uiprobe_n83/uiprobe_n100)
    return [int(float(v)) for v in s.st()['pos'].replace(',', ' ').split()]


NOTCH = 'o000=true,o100=true,o001=true,o101=true,o010=true,o110=true,o011=true,o111=false'
WOODS = (('pale_oak_log', 'blasseiche'), ('oak_log', 'eiche'), ('crimson_stem', 'karmesin'), ('stripped_oak_log', 'eiche-entrindet'))


def scenes(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('time set noon', 0.3)
    for rule in ('advance_time false', 'spawn_mobs false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    for n, (wood, label) in enumerate(WOODS):
        b = f'simpleoctets:{wood}_octet'
        ox = x + n * 12
        # thin 1-octet column, notched 2x2 pillar, half pillar (south half missing): 3 blocks high, 4 blocks ahead
        for dy in range(3):
            s.chat(f'setblock {ox - 2} {y + dy} {z + 4} {b}[o000=true,o010=true]', 0.2)
            s.chat(f'setblock {ox + 1} {y + dy} {z + 4} {b}[{NOTCH}]', 0.2)
            s.chat(f'setblock {ox + 4} {y + dy} {z + 4} {b}[o000=true,o100=true,o010=true,o110=true]', 0.2)
        s.chat(f'tp @s {ox + 1.5} {y + 1} {z + 1.5} facing {ox + 1.5} {y + 1.5} {z + 4.5}', 1.0)
        time.sleep(1.0)
        s.shot(f'{n + 1}a-{label}-saeulen-vorn')
        s.chat(f'tp @s {ox + 2.8} {y + 4.2} {z + 5.8} facing {ox + 1.5} {y + 2.5} {z + 4.5}', 1.0)
        time.sleep(0.8)
        s.shot(f'{n + 1}b-{label}-kerbe-oben')
        # close-up of the notch: both inner faces fill the view (lines + bark line on the outer edge)
        s.chat(f'tp @s {ox + 2.9} {y + 1.6} {z + 5.9} facing {ox + 1.75} {y + 1.25} {z + 4.75}', 1.0)
        time.sleep(0.8)
        s.shot(f'{n + 1}c-{label}-kerbe-nah')
    s.check('N100B shots taken (octet pillars: pale oak, oak, crimson stem, stripped oak)', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n100b': scenes}
