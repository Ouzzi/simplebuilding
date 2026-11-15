"""simpledinos-5: the five dinosaurs, the five coats, a baby, the eggs and the fossil block in the real game picture (b-client60p, scenario for
modules/simpleinterfaces/tools/ui_probe.py).

Scene (creative, GUI hidden for the pictures, NoAI/Silent/Invulnerable/PersistenceRequired so nothing walks off):
  row 1  T-Rex, Triceratops, Stegosaurus, Parasaurolophus, Utahraptor (coat 0 = olive), 10 blocks apart, facing the camera;
  row 2  five Utahraptors in the five coats (DinoVariants.COATS: olive, sand, rust, moss, ash; summoned with CoatVariant, so finalizeSpawn's
         biome pick does not run) and a baby T-Rex (Age -24000) beside an adult one;
  row 3  the five eggs and the fossil block.
Checks: every kind is there, every coat index is kept, the baby is a baby (server data), the egg blocks stand.
# NUR BESITZER: models, coat textures, hitboxes against the model, baby proportions, egg/fossil look (and the hatching in survival).
"""
import math
import time

KINDS = ['trex', 'triceratops', 'stegosaurus', 'parasaurolophus', 'utahraptor']
COATS = ['olive', 'sand', 'rust', 'moss', 'ash']


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


def dinos(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('kill @e[tag=uipdino]', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    base = 'NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[180f,0f]'
    for i, kind in enumerate(KINDS):  # row 1
        s.chat(f'summon simpledinos:{kind} {x - 19.5 + 10 * i} {y} {z + 34.5} {{{base},CoatVariant:0,Tags:["uipdino","uipdino_kind"]}}', 0.4)
    for c in range(len(COATS)):  # row 2
        s.chat(f'summon simpledinos:utahraptor {x - 7.5 + 3 * c} {y} {z + 18.5} {{{base},CoatVariant:{c},Tags:["uipdino","uipdino_coat"]}}', 0.4)
    s.chat(f'summon simpledinos:trex {x + 12.5} {y} {z + 20.5} {{{base},CoatVariant:0,Tags:["uipdino"]}}', 0.4)
    s.chat(f'summon simpledinos:trex {x + 8.5} {y} {z + 16.5} {{{base},CoatVariant:0,Age:-24000,Tags:["uipdino","uipdino_baby"]}}', 0.4)
    blocks = [f'{k}_egg' for k in KINDS] + ['fossil_block']
    for i, b in enumerate(blocks):  # row 3
        s.chat(f'setblock {x - 5 + 2 * i} {y} {z + 8} simpledinos:{b}', 0.3)
    time.sleep(2.5)
    cond = ' '.join(f'if entity @e[type=simpledinos:{k},tag=uipdino_kind]' for k in KINDS)
    s.check('all five dinosaur kinds stand in the scene', _sense(s, cond), ', '.join(KINDS))
    cond = ' '.join(f'if entity @e[type=simpledinos:utahraptor,tag=uipdino_coat,nbt={{CoatVariant:{c}}}]' for c in range(len(COATS)))
    s.check('five Utahraptors keep their coat index 0-4 (olive, sand, rust, moss, ash)', _sense(s, cond), 'CoatVariant 0..4')
    # Age counts up even without AI; still clearly negative a few seconds later
    s.check('the baby T-Rex is a baby (Age < 0)', _sense(s, 'unless entity @e[type=simpledinos:trex,tag=uipdino_baby,nbt={Age:0}] if entity @e[type=simpledinos:trex,tag=uipdino_baby]'),
            'Age not 0')
    cond = ' '.join(f'if block {x - 5 + 2 * i} {y} {z + 8} simpledinos:{b}' for i, b in enumerate(blocks))
    s.check('the five eggs and the fossil block stand', _sense(s, cond), ', '.join(blocks))
    s.key('F1', 0.5)
    try:
        _aim(s, (x + 0.5, y + 14, z - 12.5), (x + 0.5, y + 2, z + 30.5), 2.0)
        s.shot('overview')
        _aim(s, (x + 0.5, y + 3, z + 10.5), (x + 0.5, y + 1.5, z + 34.5), 1.5)
        s.shot('five-kinds')
        _aim(s, (x - 1.5, y + 1.5, z + 11.5), (x - 1.5, y + 0.8, z + 18.5), 1.5)
        s.shot('raptor-coats')
        _aim(s, (x + 10.5, y + 1.5, z + 9.5), (x + 10.5, y + 1.5, z + 18.5), 1.5)
        s.shot('trex-baby-and-adult')
        _aim(s, (x + 0.5, y + 1.5, z + 4.5), (x + 0.5, y, z + 8.5), 1.5)
        s.shot('eggs-and-fossil')
    finally:
        s.key('F1', 0.5)
    s.chat('kill @e[tag=uipdino]', 0.3)


SCENARIOS = {'simpledinos': dinos}
