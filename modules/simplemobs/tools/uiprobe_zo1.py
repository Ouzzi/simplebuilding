"""ZO1 danger zones (N88, 17.11.): zone effects on by default - in a real client, far from the world spawn (zone level ~5) at
night, natural monster spawns appear and some carry zone effects. Shots: /resonance debug (zone + spawn level), sidebar counts
(monsters near / with effects), the effects of the nearest monster with effects, and the view.

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobezo1 --scale 2 --only simplemobs/zo1
"""
import time

NEAR = 'distance=..96'
MONSTERS = f'@e[type=!player,type=!item,type=!experience_orb,{NEAR}]'
WITH_EFFECTS = f'@e[type=!player,type=!item,{NEAR},nbt={{active_effects:[{{}}]}}]'


def count(s):
    s.chat(f'execute store result score Monster zo1 if entity {MONSTERS}', 0.4)
    s.chat(f'execute store result score MitEffekt zo1 if entity {WITH_EFFECTS}', 0.4)


def zones(s):
    s.close()
    s.chat('gamemode creative', 0.3)
    s.chat('difficulty hard', 0.3)
    # 26.x game rule ids are snake_case; the old camelCase names are kept as a harmless fallback
    for rule in ('spawn_mobs true', 'advance_time false', 'doMobSpawning true', 'doDaylightCycle false'):
        s.chat('gamerule ' + rule, 0.3)
    s.chat('time set midnight', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    s.chat('scoreboard objectives add zo1 dummy "ZO1 Zonen"', 0.3)
    s.chat('scoreboard objectives setdisplay sidebar zo1', 0.3)
    # zone curve: z0 0.5 at the spawn, 5.0 from 6000 blocks; stand high so the ground around is lit only by night
    s.chat('tp @s 6200 120 6200', 4.0)
    s.chat('spreadplayers 6200 6200 0 1 false @s', 4.0)
    s.chat('resonance debug', 1.0)
    s.shot('0-resonance-debug')
    for k in range(4):
        time.sleep(15)
        count(s)
        s.shot(f'{k + 1}-nach-{(k + 1) * 15}s')
    s.chat(f'data get entity {WITH_EFFECTS[:-1]},sort=nearest,limit=1] active_effects', 1.0)
    s.shot('5-effekte-naechstes-monster')
    s.chat(f'tp @s {WITH_EFFECTS[:-1]},sort=nearest,limit=1]', 1.5)
    s.chat('tp @s ~ ~3 ~ facing entity @e[type=!player,type=!item,sort=nearest,limit=1]', 1.5)
    s.shot('6-ansicht')
    s.check('zone shots taken (counts in the sidebar, effects in chat)', True, 'see shots/')


SCENARIOS = {'simplemobs/zo1': zones}
