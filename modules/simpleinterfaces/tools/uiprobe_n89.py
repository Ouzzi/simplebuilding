"""N89A worn armour layers (b-red89a, texture review N89A open): in the flat world, with NoAI, in rows facing the camera -
a T. rex in each of the six new tiers (copper, gold, rose gold, rose copper, netherite, Enderite); Triceratops, Stegosaurus,
Parasaurolophus and Utahraptor each in gold, rose gold and rose copper; a horse in rose gold / rose copper horse armour and a
Vanilla nautilus in both rose nautilus armours. Body armour sits in the body equipment slot (Dino extends AbstractHorse, the
renderer picks the tier texture from EquipmentSlot.BODY), summoned like the test centre's horse
(FeatureStations.horseCommand: equipment:{body:{...}}). Close shots per block: Tier-Ruestung, Dino-Ruestungen, Pferd/Nautilus.

    xvfb-run -a python3.12 modules/simpleinterfaces/tools/ui_probe.py --out /var/tmp/sb/uiprobe89 --scale 2 --only simpleinterfaces/n89
"""
import time

TAG = 'uip89'
BASE = f'NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Tame:1b,Tags:["{TAG}"]'
TREX_TIERS = ['copper', 'gold', 'rose_gold', 'rose_bronze', 'netherite', 'enderite']
SMALL_TIERS = ['gold', 'rose_gold', 'rose_bronze']
# kind -> spacing between the three of a kind (hitbox width 2.6 / 2.6 / 2.2 / 1.3 plus air)
SMALL = [('triceratops', 4), ('stegosaurus', 4), ('parasaurolophus', 4), ('utahraptor', 3)]
ROSE = ['rose_gold', 'rose_bronze']


def _pos(s):
    return [int(v) for v in s.st()['pos'].replace(',', ' ').split()]


def _sense(s, cond, slot=26):
    """True when the server sees cond (execute ... run puts a stone into an inventory slot the probe can read)."""
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.4)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def _summon(s, entity, x, y, z, body, extra='', yaw=180):
    """yaw 180 = facing -z (towards the player start), 0 = facing +z."""
    s.chat(f'summon {entity} {x} {y} {z} {{{BASE},Rotation:[{yaw}f,0f]{extra},'
           f'equipment:{{body:{{id:"{body}",count:1}}}}}}', 0.4)


def _wears(s, entity, body):
    return _sense(s, f'if entity @e[type={entity},tag={TAG},nbt={{equipment:{{body:{{id:"{body}"}}}}}}]')


def _cam(s, fx, fy, fz, tx, ty, tz, name, wait=1.5):
    s.chat(f'tp @s {fx} {fy} {fz} facing {tx} {ty} {tz}', wait)
    s.shot(name)


def armour(s):
    s.close()
    s.chat('gamemode creative @s', 0.3)
    s.chat('clear @s', 0.3)
    s.chat('time set noon', 0.3)
    s.chat('weather clear', 0.3)
    # 26.x game rule ids are snake_case (the camelCase names are rejected, N83 run 2)
    s.chat('gamerule advance_time false', 0.3)
    s.chat('gamerule spawn_mobs false', 0.3)
    s.chat('kill @e[type=minecraft:slime]', 0.3)
    s.chat(f'kill @e[tag={TAG}]', 0.3)
    s.chat('effect give @s night_vision 99999 0 true', 0.3)
    x, y, z = _pos(s)

    # Block 1 Tier-Ruestung: six T. rexes 6 blocks apart at z + 12, facing the camera (-z).
    rz = z + 12.5
    trex_x = [x - 14.5 + 6 * i for i in range(len(TREX_TIERS))]
    for tx, tier in zip(trex_x, TREX_TIERS):
        _summon(s, 'simpledinos:trex', tx, y, rz, f'simpledinos:trex_armor_{tier}', ',CoatVariant:0')

    # Block 2 Dino-Ruestungen: one group of three per kind on z + 34, 16 blocks between the groups.
    dz = z + 34.5
    groups = []
    for k, (kind, gap) in enumerate(SMALL):
        gx = x - 26.5 + 16 * k
        groups.append((kind, gx, gap))
        for j, tier in enumerate(SMALL_TIERS):
            _summon(s, f'simpledinos:{kind}', gx + gap * j, y, dz, f'simpledinos:{kind}_armor_{tier}', ',CoatVariant:0')

    # Block 3 Pferd/Nautilus: behind the player start (-z side), facing +z (towards the start).
    hz = z - 12.5
    for j, tier in enumerate(ROSE):
        _summon(s, 'minecraft:horse', x - 4.5 + 3 * j, y, hz, f'simpleores:{tier}_horse_armor', ',Variant:0', yaw=0)
    for j, tier in enumerate(ROSE):
        _summon(s, 'minecraft:nautilus', x + 2.5 + 3 * j, y, hz, f'simpleores:{tier}_nautilus_armor', yaw=0)
    time.sleep(2.0)

    # Every summon arrived with its body armour (wrong NBT would leave the slot empty or the entity missing).
    for tier in TREX_TIERS:
        s.check(f'T. rex wears trex_armor_{tier}', _wears(s, 'simpledinos:trex', f'simpledinos:trex_armor_{tier}'))
    for kind, _, _ in groups:
        for tier in SMALL_TIERS:
            s.check(f'{kind} wears {kind}_armor_{tier}', _wears(s, f'simpledinos:{kind}', f'simpledinos:{kind}_armor_{tier}'))
    for tier in ROSE:
        s.check(f'horse wears {tier}_horse_armor', _wears(s, 'minecraft:horse', f'simpleores:{tier}_horse_armor'))
        s.check(f'nautilus wears {tier}_nautilus_armor', _wears(s, 'minecraft:nautilus', f'simpleores:{tier}_nautilus_armor'))

    s.key('F1', 0.5)
    try:
        # Tier-Ruestung: overview, then pairs close up (copper/gold, rose gold/rose copper, netherite/Enderite).
        _cam(s, x + 0.5, y + 6, z - 6.5, x + 0.5, y + 1.5, rz, '1-tier-ruestung-uebersicht')
        for p in range(3):
            cx = (trex_x[2 * p] + trex_x[2 * p + 1]) / 2
            _cam(s, cx, y + 2.5, rz - 9, cx, y + 1.8, rz, f'1-tier-ruestung-{TREX_TIERS[2 * p]}-{TREX_TIERS[2 * p + 1]}')
        # side view of the rose pair (plates on the flank)
        cx = (trex_x[2] + trex_x[3]) / 2
        _cam(s, cx - 7, y + 3, rz - 5, cx, y + 1.8, rz, '1-tier-ruestung-rose-schraeg')

        # Dino-Ruestungen: one shot per kind; the camera looks +z, so gold (lowest x) stands on the right, rose copper left.
        for kind, gx, gap in groups:
            cx = gx + gap
            dist = 6 if kind == 'utahraptor' else 8
            _cam(s, cx, y + 2.2, dz - dist, cx, y + 1.2, dz, f'2-dino-ruestungen-{kind}')

        # Pferd/Nautilus: they face +z (the player start); the camera stands between start and them, looking -z.
        _cam(s, x - 3, y + 2, hz + 6, x - 3, y + 1, hz, '3-pferd-rose')
        _cam(s, x + 4, y + 1.8, hz + 5, x + 4, y + 0.6, hz, '3-nautilus-rose')
        _cam(s, x + 6, y + 2.5, hz + 4, x, y + 0.8, hz, '3-pferd-nautilus-schraeg')
    finally:
        s.key('F1', 0.5)
    s.chat(f'kill @e[tag={TAG}]', 0.5)
    s.check('N89 armour shots taken (Tier-Ruestung, Dino-Ruestungen, Pferd/Nautilus)', True, 'see shots/')


SCENARIOS = {'simpleinterfaces/n89': armour}
