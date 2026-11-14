"""simpletools-6: Biome Brush colouring and the ceramic / enderite buckets in the real game picture (a-w4c, scenario for
modules/simpleinterfaces/tools/ui_probe.py).

Brush: creative, brush in the main hand, red sand in the off hand (material of badlands), the crosshair on the grass 4 blocks ahead, ONE
real right click: the biome around the block becomes badlands (server test 'execute if biome'), picture before and after (GUI hidden) shows
the grass colour change of the foreground.
Buckets: a chest with all 22 bucket items of the module (4 ceramic wear stages empty / with water / with lava, raw bucket, enderite bucket
empty / 1/2 / 2/2 water, lava, milk, soul lava) is opened with the real right click; picture of the chest screen, tooltips of two buckets
under the real pointer. The check counts the stacks that loaded: an id that does not exist in the integration pack (e.g. enderite without Simple
Ores) is missing and named in the detail.
# NUR BESITZER: whether the four ceramic stages look different enough at inventory size, the paint colour feel, the brush sound.
"""
import math
import time

CERAMIC = ('ceramic', 'chipped_ceramic', 'cracked_ceramic', 'brittle_ceramic')
BUCKETS = ([f'{c}_water_bucket' for c in CERAMIC] + [f'{c}_lava_bucket' for c in CERAMIC] + ['raw_ceramic_bucket']
           + [f'{c}_bucket' for c in CERAMIC]
           + ['enderite_bucket', 'enderite_water_bucket', 'enderite_water_bucket_full', 'enderite_lava_bucket', 'enderite_lava_bucket_full',
              'enderite_milk_bucket', 'enderite_milk_bucket_full', 'enderite_soul_lava_bucket', 'enderite_soul_lava_bucket_full'])


def _pos(s):
    return tuple(int(v) for v in s.st()['pos'].replace(',', ' ').split())


def _aim(s, f, t, wait=0.8):
    """Stand at feet point f and look at point t (eye height 1.62, like a crosshair)."""
    dx, dy, dz = t[0] - f[0], t[1] - (f[1] + 1.62), t[2] - f[2]
    yaw = math.degrees(math.atan2(-dx, dz))
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    s.chat(f'tp @s {f[0]:.2f} {f[1]} {f[2]:.2f} {yaw:.1f} {pitch:.1f}', wait)


def _sense(s, cond, slot=26):
    """A server-side test made visible: a stone appears in inventory.<slot> when the condition holds."""
    s.chat(f'item replace entity @s inventory.{slot} with minecraft:air', 0.2)
    s.chat(f'execute {cond} run item replace entity @s inventory.{slot} with minecraft:stone 1', 0.3)
    inv = s.st().get('inventory', [])
    return len(inv) > 9 + slot and inv[9 + slot].startswith('minecraft:stone')


def _click(s, after=0.8, hold=0.12):
    s.io.button(3, True)
    time.sleep(hold)
    s.io.button(3, False)
    time.sleep(after)


def _brush(s, x, y, z):
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'tp @s {me[0]} {y} {me[2]} 0 0', 1.2)
    s.chat('item replace entity @s hotbar.0 with simpletools:biome_brush', 0.3)
    s.chat('item replace entity @s weapon.offhand with minecraft:red_sand 16', 0.3)
    s.io.key('1', 0.4)
    before = _sense(s, f'if biome {x} {y - 1} {z + 4} minecraft:badlands')
    s.check('brush: the grass patch is not badlands before the stroke', not before, 'biome before')
    _aim(s, me, (x + 0.5, y - 0.01, z + 4.5), 1.0)
    s.key('F1', 0.5)
    try:
        s.shot('brush-before')
        _click(s, after=2.5)  # the biome change is sent to the client, the chunk is redrawn
        s.shot('brush-after')
    finally:
        s.key('F1', 0.5)
    after = _sense(s, f'if biome {x} {y - 1} {z + 4} minecraft:badlands')
    s.check('brush: one real right click paints the biome around the block (badlands)', after, 'execute if biome ... badlands')


def _buckets(s, x, y, z):
    me = (x + 0.5, y, z + 0.5)
    s.chat(f'tp @s {me[0]} {y} {me[2]} 0 0', 1.2)
    items = ','.join(f'{{Slot:{i}b,id:"simpletools:{name}",count:1}}' for i, name in enumerate(BUCKETS))
    s.chat(f'setblock {x} {y} {z + 2} minecraft:chest[facing=north]{{Items:[{items}]}}', 0.6)
    _aim(s, me, (x + 0.5, y + 0.5, z + 2.5), 0.9)
    s.io.key('9', 0.3)
    _click(s, after=1.2)
    st = s.wait_screen('ContainerScreen', 8)
    s.check('real right click opens the chest with the buckets', 'ContainerScreen' in st.get('screen', ''), st.get('screen'))
    chest = [sl for sl in st.get('slots', []) if not sl['own']]
    shown = [sl['stack'].split(' x')[0].replace('simpletools:', '') for sl in chest if sl['stack']]
    missing = [b for b in BUCKETS if b not in shown]
    s.check(f'chest shows all {len(BUCKETS)} bucket items', not missing, f'{len(shown)} shown, missing: {missing}')
    s.shot('buckets-chest')
    for name in ('brittle_ceramic_water_bucket', 'enderite_water_bucket'):
        idx = BUCKETS.index(name)
        slot = s.slot_xy(st, cslot=idx, own=False)
        if not slot:
            s.check(f'chest slot of {name} present', False, f'{len(chest)} chest slots')
            continue
        s.io.move(*s.abs(st, slot['x'] + 3, slot['y'] + 3))
        time.sleep(0.4)
        s.io.move(*s.abs(st, slot['x'], slot['y']))
        time.sleep(0.9)
        s.shot(f'tooltip-{name}')
    s.close()
    st = s.st()
    s.io.move(st['windowX'] + 640, st['windowY'] + 360)


def tools(s):
    s.chat('time set day', 0.3)
    s.chat('weather clear', 0.3)
    s.chat('clear @s', 0.3)
    x, y, z = _pos(s)
    s.chat(f'tp @s {x + 0.5} {y} {z + 20.5} 0 0', 1.8)
    x, y, z = _pos(s)
    s.io.move(*s.abs(s.st(), 640, 360))
    try:
        for part in (_brush, _buckets):
            try:
                part(s, x, y, z)
            except Exception as e:  # one failing part must not stop the other
                s.check(f'{part.__name__.strip("_")} part ran without exception', False, repr(e)[:300])
            s.close()
            s.io.button(3, False)
    finally:
        s.chat('clear @s', 0.3)
        s.chat(f'setblock {x} {y} {z + 2} minecraft:air', 0.3)


SCENARIOS = {'simpletools': tools}
