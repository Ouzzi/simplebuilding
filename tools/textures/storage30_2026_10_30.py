"""Simple Storage A30 (ST27-1 pipe, BS29b-3 chest network): textures, models, block states, item definitions, loot tables,
recipes and upgrade entries of the pipe (4 steps), the copper cable, the chest interface and the four controller chests.

Own pixel art (recommended variant A of /root/sb-tools-a/previews/a-storage30-preview.html; the other variants are only in
that preview until the owner picks). Writes into modules/simplestorage/shared/resources:
  assets/simplestorage/textures/block/{pipe,reinforced_pipe,netherite_pipe,enderite_pipe,copper_cable,chest_interface}.png (+ .layers.json)
  assets/simplestorage/textures/entity/chest/{wooden,reinforced,netherite,enderite}_controller.png (+ .layers.json): the tier chest
    texture with an amber lock, a network emblem on the lid and a cyan indicator strip on the front (wooden: the steel texture
    mapped to oak browns - own pixels, nothing from Vanilla)
  assets/simplestorage/{models/block,models/item,blockstates,items}/...
  data/simplestorage/{loot_table/blocks,recipe,in_world_upgrades}/...  (recipe unlock advancements: tools/discover/discover.py)

Texture cells of a pipe sheet (16 x 16): A core plate 6x6 at (0,0), C end ring 6x6 at (6,0), AU arrow up 6x5 at (0,6),
AR arrow right 5x6 at (6,6). Model faces cut what they need (input arms use slices of A, the output arm the arrows).

Run from the repo root: python3 tools/textures/storage30_2026_10_30.py
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
from layers import save, snap  # noqa: E402

RES = ROOT / 'modules/simplestorage/shared/resources'
ASSETS = RES / 'assets/simplestorage'
DATA = RES / 'data/simplestorage'
NS = 'simplestorage'


def hexc(value: str):
    return tuple(int(value[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def dump(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, 'w', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


# ------------------------------------------------------------------------------------------------- pipe textures

# ramp dark -> light, arrow main and shade (recommendation A: copper pipe with a gold chevron, then the tier colours)
PIPE_STEPS = {
    'pipe': (['#4a2314', '#7a3f24', '#a8592f', '#cf7c42', '#efa66a'], '#f4d36e', '#b78a2c'),
    'reinforced_pipe': (['#2f4346', '#4c6a6f', '#678b91', '#92b6bc', '#c4dde0'], '#eaf6f7', '#8fb2b8'),
    'netherite_pipe': (['#241d1a', '#3e3430', '#5a4d47', '#80726a', '#a39588'], '#d48a52', '#92572f'),
    'enderite_pipe': (['#33244a', '#503a6d', '#6c4f8c', '#8d6bb0', '#b89ad8'], '#e6d2ff', '#9a7fc4'),
}

CORE = ['333332', '342221', '322221', '322221', '322241', '211111']
ARROW_UP = ['12aa21', '1aaaa1', 'aaaaaa', '12aa21', '12bb21']


def pipe_sheet(name: str, ramp, arrow_main, arrow_shade) -> Image.Image:
    r = [hexc(c) for c in ramp]
    a, b = hexc(arrow_main), hexc(arrow_shade)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(CORE):  # A: plate with two rivets
        for x, ch in enumerate(row):
            px[x, y] = r[int(ch)]
    snap(img, 'Kernplatte')
    for y in range(6):  # C: end ring around a dark opening
        for x in range(6):
            edge = min(x, y, 5 - x, 5 - y)
            tone = (3 if x + y < 5 else 2) if edge == 0 else 1 if edge == 1 else 0
            px[6 + x, y] = r[tone]
    snap(img, 'Endring')
    arrow = Image.new('RGBA', (6, 5))
    ap = arrow.load()
    for y, row in enumerate(ARROW_UP):  # AU: chevron, tip up
        for x, ch in enumerate(row):
            ap[x, y] = a if ch == 'a' else b if ch == 'b' else r[int(ch)]
    img.paste(arrow, (0, 6))
    right = arrow.rotate(-90, expand=True)  # AR: the same arrow, tip right (5 wide, 6 high)
    img.paste(right, (6, 6))
    snap(img, 'Pfeil')
    return img


def write_pipes() -> None:
    for name, (ramp, arrow_main, arrow_shade) in PIPE_STEPS.items():
        save(pipe_sheet(name, ramp, arrow_main, arrow_shade), ASSETS / f'textures/block/{name}.png')


# ------------------------------------------------------------------------------------------------- cable and interface

COPPER = ['#6a3520', '#8d492b', '#b4663c', '#da8a52']
WOOL = ['#b8b2a6', '#d9d4c8', '#efebe0']


def cable_sheet() -> Image.Image:
    c = [hexc(v) for v in COPPER]
    w = [hexc(v) for v in WOOL]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(4):  # core 4x4 at (0,0): wool wrap around copper
        for x in range(4):
            px[x, y] = w[1] if (x in (0, 3) or y in (0, 3)) else c[2]
    px[0, 0], px[3, 3] = w[2], w[0]
    snap(img, 'Kern')
    for y in range(6):  # arm side 4 wide x 6 high at (4,0): bands run across
        for x in range(4):
            band = (y % 3) == 1
            px[4 + x, y] = (c[3] if x == 0 else c[1] if x == 3 else c[2]) if band else (w[2] if x == 0 else w[0] if x == 3 else w[1])
    for y in range(4):  # arm side 6 wide x 4 high at (0,6): the same, turned
        for x in range(6):
            band = (x % 3) == 1
            px[x, 6 + y] = (c[3] if y == 0 else c[1] if y == 3 else c[2]) if band else (w[2] if y == 0 else w[0] if y == 3 else w[1])
    snap(img, 'Wickel')
    for y in range(4):  # arm end 4x4 at (8,0): wool ring, cut copper
        for x in range(4):
            inner = 1 <= x <= 2 and 1 <= y <= 2
            px[8 + x, y] = (c[3] if (x, y) == (1, 1) else c[2]) if inner else w[1]
    snap(img, 'Schnittende')
    return img


GOLD = ['#7a5412', '#a8791c', '#d6a436', '#f2cf63']
QUARTZ = ['#cfc8bc', '#ece6dc', '#faf6ee']
GLASS = ['#6aa7b6', '#a9dbe6', '#d9f4f8']


def interface_sheet() -> Image.Image:
    g = [hexc(v) for v in GOLD]
    q = [hexc(v) for v in QUARTZ]
    gl = [hexc(v) for v in GLASS]
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(14):  # front 14x14 at (0,0)
        for x in range(14):
            edge = min(x, y, 13 - x, 13 - y)
            if edge < 2:  # gold frame, lit top left
                px[x, y] = g[3] if edge == 0 and x + y < 13 else g[2] if edge == 0 else g[2] if x + y < 13 else g[1]
                if edge == 1 and x + y >= 13:
                    px[x, y] = g[0]
            elif edge == 2:  # quartz border
                px[x, y] = q[2] if x + y < 13 else q[0]
            else:  # quartz field with the glass pane in the middle
                px[x, y] = q[1]
    snap(img, 'Rahmen')
    for y in range(4, 10):  # glass pane 6x6
        for x in range(4, 10):
            edge = min(x - 4, y - 4, 9 - x, 9 - y)
            px[x, y] = gl[0] if edge == 0 else gl[1]
    px[5, 5], px[6, 5], px[5, 6] = gl[2], gl[2], gl[2]
    snap(img, 'Glas')
    for (x, y) in ((3, 3), (10, 3), (3, 10), (10, 10)):  # gold studs in the field corners
        px[x, y] = g[2]
    for x in (5, 6, 7, 8):  # a quartz/gold port line under the glass
        px[x, 11] = g[1]
    snap(img, 'Details')
    return img


# ------------------------------------------------------------------------------------------------- controller chests

EMBLEM = ['c.....c', '.l...l.', '..lll..', '..lHl..', '..lll..', '.l...l.', 'c.....c']
CYAN = {'c': '#58e4ea', 'l': '#26a3ab', 'H': '#c9fbfd', 'd': '#10484e'}
AMBER = ['#6b3e0b', '#a8661a', '#e09a2c', '#ffd06a']


def tier_chest(path: Path) -> Image.Image:
    return Image.open(path).convert('RGBA')


def is_teal(p) -> bool:
    return p[3] > 0 and p[2] > p[0] + 25 and p[1] > p[0] + 15


def wooden_from_steel(src: Image.Image) -> Image.Image:
    """The steel chest sheet mapped to an oak-brown ramp by brightness (the lock stays amber below)."""
    ramp = [hexc(v) for v in ('#2e1d0e', '#4f3318', '#6e4a24', '#8d6433', '#aa7f45', '#c79a5a')]
    out = src.copy()
    sp, op = src.load(), out.load()
    lows = [sum(sp[x, y][:3]) / 3 for x in range(src.width) for y in range(src.height) if sp[x, y][3] > 0 and not is_teal(sp[x, y])]
    lo, hi = min(lows), max(lows)
    for y in range(src.height):
        for x in range(src.width):
            p = sp[x, y]
            if p[3] == 0 or is_teal(p):
                continue
            t = (sum(p[:3]) / 3 - lo) / max(1.0, hi - lo)
            op[x, y] = ramp[min(len(ramp) - 1, int(t * len(ramp)))]
    return out


def controller_sheet(step: str) -> Image.Image:
    base = tier_chest(ASSETS / f'textures/entity/chest/{"reinforced" if step == "wooden" else step}.png')
    img = wooden_from_steel(base) if step == 'wooden' else base.copy()
    snap(img, 'Truhe der Stufe')
    px = img.load()
    amber = [hexc(v) for v in AMBER]
    for y in range(0, 6):  # the lock (top left of the sheet) turns amber: the controller's mark
        for x in range(0, 7):
            p = px[x, y]
            if p[3] > 0 and is_teal(p):
                lum = sum(p[:3]) / 3
                px[x, y] = amber[3] if lum > 170 else amber[2] if lum > 110 else amber[1] if lum > 60 else amber[0]
    snap(img, 'Schloss')
    for y, row in enumerate(EMBLEM):  # network emblem on the lid top (14,0)-(28,14), centred
        for x, ch in enumerate(row):
            if ch != '.':
                px[14 + 3 + x, 3 + y] = hexc(CYAN[ch])
    snap(img, 'Netzwerk-Zeichen')
    # indicator strip on the base front (14,33)-(28,43): a dark slot with three lit cyan cells, and two dots on the lid front
    for x in range(17, 26):
        for y in (38, 39):
            px[x, y] = hexc(CYAN['d'])
    for x in (18, 21, 24):
        px[x, 38] = hexc(CYAN['c'])
        px[x, 39] = hexc(CYAN['l'])
    for x in (18, 23):
        px[x, 17] = hexc(CYAN['c'])
    snap(img, 'Anzeige')
    return img


def write_controllers() -> None:
    for step in ('wooden', 'reinforced', 'netherite', 'enderite'):
        save(controller_sheet(step), ASSETS / f'textures/entity/chest/{step}_controller.png')


# ------------------------------------------------------------------------------------------------- models and states

DIRS = ('north', 'east', 'south', 'west', 'up', 'down')
ROT = {'north': {}, 'east': {'y': 90}, 'south': {'y': 180}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}


def face(uv, tex='#pipe', **extra):
    out = {'uv': list(uv), 'texture': tex}
    out.update(extra)
    return out


def pipe_templates() -> None:
    # the middle piece 6x6x6: every face the core plate
    cube = {d: face((0, 0, 6, 6)) for d in ('down', 'up', 'north', 'south', 'west', 'east')}
    dump(ASSETS / 'models/block/template_pipe_core.json', {
        'textures': {'particle': '#pipe'},
        'elements': [{'from': [5, 5, 5], 'to': [11, 11, 11], 'faces': cube}]})
    # input arm (toward north): z 0..5 around x/y 5..11, slices of the plate, the open end is the ring
    dump(ASSETS / 'models/block/template_pipe_arm.json', {
        'textures': {'particle': '#pipe'},
        'elements': [{'from': [5, 5, 0], 'to': [11, 11, 5], 'faces': {
            'north': face((6, 0, 12, 6)),
            'up': face((0, 0, 6, 5)), 'down': face((0, 0, 6, 5)),
            'east': face((0, 0, 5, 6)), 'west': face((0, 0, 5, 6))}}]})
    # output arm (toward north): the chevrons point out of the pipe (up face: tip up = north; down face flipped; east: tip right = north;
    # west: tip left = north)
    dump(ASSETS / 'models/block/template_pipe_out.json', {
        'textures': {'particle': '#pipe'},
        'elements': [{'from': [5, 5, 0], 'to': [11, 11, 5], 'faces': {
            'north': face((6, 0, 12, 6)),
            'up': face((0, 6, 6, 11)), 'down': face((0, 11, 6, 6)),
            'east': face((6, 6, 11, 12)), 'west': face((11, 6, 6, 12))}}]})
    # the item model: a straight pipe along z (arrow arm north, input arm south)
    dump(ASSETS / 'models/block/template_pipe_item.json', {
        'parent': 'minecraft:block/block',
        'textures': {'particle': '#pipe'},
        'elements': [
            {'from': [5, 5, 5], 'to': [11, 11, 11], 'faces': cube},
            {'from': [5, 5, 0], 'to': [11, 11, 5], 'faces': {
                'north': face((6, 0, 12, 6)), 'up': face((0, 6, 6, 11)), 'down': face((0, 11, 6, 6)),
                'east': face((6, 6, 11, 12)), 'west': face((11, 6, 6, 12))}},
            {'from': [5, 5, 11], 'to': [11, 11, 16], 'faces': {
                'south': face((6, 0, 12, 6)), 'up': face((0, 0, 6, 5)), 'down': face((0, 0, 6, 5)),
                'east': face((0, 0, 5, 6)), 'west': face((0, 0, 5, 6))}}]})
    for name in PIPE_STEPS:
        for part in ('core', 'arm', 'out', 'item'):
            dump(ASSETS / f'models/block/{name}_{part}.json', {
                'parent': f'{NS}:block/template_pipe_{part}',
                'textures': {'pipe': f'{NS}:block/{name}', 'particle': f'{NS}:block/{name}'}})
        # blockstate: core, inputs (arm on, not the output side), the output arm
        parts = [{'apply': {'model': f'{NS}:block/{name}_core'}}]
        for d in DIRS:
            others = '|'.join(x for x in DIRS if x != d)
            parts.append({'when': {'AND': [{d: 'true'}, {'facing': others}]}, 'apply': {'model': f'{NS}:block/{name}_arm', **ROT[d]}})
        for d in DIRS:
            parts.append({'when': {'facing': d}, 'apply': {'model': f'{NS}:block/{name}_out', **ROT[d]}})
        dump(ASSETS / f'blockstates/{name}.json', {'multipart': parts})
        dump(ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/{name}_item'}})


def cable_models() -> None:
    cube = {d: face((0, 0, 4, 4), '#cable') for d in ('down', 'up', 'north', 'south', 'west', 'east')}
    dump(ASSETS / 'models/block/copper_cable_core.json', {
        'textures': {'cable': f'{NS}:block/copper_cable', 'particle': f'{NS}:block/copper_cable'},
        'elements': [{'from': [6, 6, 6], 'to': [10, 10, 10], 'faces': cube}]})
    # arm toward north: z 0..6 around x/y 6..10; up/down 4x6 bands, east/west 6x4 bands, the cut end
    dump(ASSETS / 'models/block/copper_cable_arm.json', {
        'textures': {'cable': f'{NS}:block/copper_cable', 'particle': f'{NS}:block/copper_cable'},
        'elements': [{'from': [6, 6, 0], 'to': [10, 10, 6], 'faces': {
            'north': face((8, 0, 12, 4), '#cable'),
            'up': face((4, 0, 8, 6), '#cable'), 'down': face((4, 0, 8, 6), '#cable'),
            'east': face((0, 6, 6, 10), '#cable'), 'west': face((0, 6, 6, 10), '#cable')}}]})
    dump(ASSETS / 'models/block/copper_cable_item.json', {
        'parent': 'minecraft:block/block',
        'textures': {'cable': f'{NS}:block/copper_cable', 'particle': f'{NS}:block/copper_cable'},
        'elements': [
            {'from': [6, 6, 6], 'to': [10, 10, 10], 'faces': cube},
            {'from': [6, 6, 0], 'to': [10, 10, 6], 'faces': {
                'north': face((8, 0, 12, 4), '#cable'), 'up': face((4, 0, 8, 6), '#cable'), 'down': face((4, 0, 8, 6), '#cable'),
                'east': face((0, 6, 6, 10), '#cable'), 'west': face((0, 6, 6, 10), '#cable')}},
            {'from': [6, 6, 10], 'to': [10, 10, 16], 'faces': {
                'south': face((8, 0, 12, 4), '#cable'), 'up': face((4, 0, 8, 6), '#cable'), 'down': face((4, 0, 8, 6), '#cable'),
                'east': face((0, 6, 6, 10), '#cable'), 'west': face((0, 6, 6, 10), '#cable')}}]})
    parts = [{'apply': {'model': f'{NS}:block/copper_cable_core'}}]
    for d in DIRS:
        parts.append({'when': {d: 'true'}, 'apply': {'model': f'{NS}:block/copper_cable_arm', **ROT[d]}})
    dump(ASSETS / 'blockstates/copper_cable.json', {'multipart': parts})
    dump(ASSETS / 'items/copper_cable.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/copper_cable_item'}})


def interface_models() -> None:
    # plate toward north (the chest is to the north): 14x14x3 at z 0..3 and the 4x4 post to the core; the plate's front is the sheet
    front = (0, 0, 14, 14)
    frame = (0, 0, 14, 3)
    dump(ASSETS / 'models/block/chest_interface_plate.json', {
        'textures': {'plate': f'{NS}:block/chest_interface', 'particle': f'{NS}:block/chest_interface'},
        'elements': [
            {'from': [1, 1, 0], 'to': [15, 15, 3], 'faces': {
                'south': face(front, '#plate'), 'north': face(front, '#plate'),
                'up': face(frame, '#plate'), 'down': face(frame, '#plate'), 'east': face((0, 0, 3, 14), '#plate'), 'west': face((0, 0, 3, 14), '#plate')}},
            {'from': [6, 6, 3], 'to': [10, 10, 6], 'faces': {
                'up': face((0, 0, 4, 3), '#plate'), 'down': face((0, 0, 4, 3), '#plate'),
                'east': face((0, 0, 3, 4), '#plate'), 'west': face((0, 0, 3, 4), '#plate')}}]})
    dump(ASSETS / 'models/block/chest_interface_item.json', {
        'parent': 'minecraft:block/block',
        'textures': {'plate': f'{NS}:block/chest_interface', 'cable': f'{NS}:block/copper_cable', 'particle': f'{NS}:block/chest_interface'},
        'elements': [
            {'from': [1, 1, 0], 'to': [15, 15, 3], 'faces': {
                'south': face(front, '#plate'), 'north': face(front, '#plate'),
                'up': face(frame, '#plate'), 'down': face(frame, '#plate'), 'east': face((0, 0, 3, 14), '#plate'), 'west': face((0, 0, 3, 14), '#plate')}},
            {'from': [6, 6, 3], 'to': [10, 10, 10], 'faces': {
                'up': face((4, 0, 8, 6), '#cable'), 'down': face((4, 0, 8, 6), '#cable'),
                'east': face((0, 6, 6, 10), '#cable'), 'west': face((0, 6, 6, 10), '#cable'), 'south': face((8, 0, 12, 4), '#cable')}}],
        'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.8, 0.8, 0.8]}}})
    parts = [{'when': {'facing': d}, 'apply': {'model': f'{NS}:block/chest_interface_plate', **ROT[d]}} for d in DIRS]
    parts.append({'apply': {'model': f'{NS}:block/copper_cable_core'}})
    for d in DIRS:
        parts.append({'when': {d: 'true'}, 'apply': {'model': f'{NS}:block/copper_cable_arm', **ROT[d]}})
    dump(ASSETS / 'blockstates/chest_interface.json', {'multipart': parts})
    dump(ASSETS / 'items/chest_interface.json', {'model': {'type': 'minecraft:model', 'model': f'{NS}:block/chest_interface_item'}})


def controller_models() -> None:
    for step in ('wooden', 'reinforced', 'netherite', 'enderite'):
        name = f'{step}_controller_chest'
        particle = {'wooden': 'minecraft:block/oak_planks', 'reinforced': 'simplelib:block/cracked_diamond_block',
                    'netherite': 'minecraft:block/netherite_block', 'enderite': 'simplelib:block/enderite_block'}[step]
        dump(ASSETS / f'models/block/{name}.json', {'textures': {'particle': particle}})
        dump(ASSETS / f'blockstates/{name}.json', {'variants': {'': {'model': f'{NS}:block/{name}'}}})
        dump(ASSETS / f'items/{name}.json', {'model': {'type': 'minecraft:special', 'base': 'minecraft:item/chest',
                                                       'model': {'type': 'minecraft:chest', 'texture': f'{NS}:{step}_controller'}}})


# ------------------------------------------------------------------------------------------------- data

BLOCK_IDS = ['pipe', 'reinforced_pipe', 'netherite_pipe', 'enderite_pipe', 'copper_cable', 'chest_interface',
             'wooden_controller_chest', 'reinforced_controller_chest', 'netherite_controller_chest', 'enderite_controller_chest']


def loot() -> None:
    for name in BLOCK_IDS:
        entry = {'type': 'minecraft:item', 'name': f'{NS}:{name}'}
        if name.endswith('controller_chest'):
            entry['modifier'] = {'type': 'minecraft:copy_components', 'include': ['minecraft:custom_name'], 'source': 'block_entity'}
        # the pipes' and controllers' contents drop through the block entity (BlockEntity#preRemoveSideEffects)
        dump(DATA / f'loot_table/blocks/{name}.json', {
            'type': 'minecraft:block',
            'pools': [{'condition': {'type': 'minecraft:survives_explosion'}, 'entries': [entry], 'rolls': 1}]})
    # the loot_table dump above writes "condition"; the module's tables use the same key (see reinforced_chest.json)


def shaped(pattern, key, result, count=1, group=None):
    out = {'type': 'minecraft:crafting_shaped', 'key': key, 'pattern': pattern, 'result': {'id': result} if count == 1 else {'count': count, 'id': result}}
    if group:
        out['group'] = group
    return out


def shapeless(ingredients, result, count=1):
    return {'type': 'minecraft:crafting_shapeless', 'ingredients': ingredients, 'result': {'id': result} if count == 1 else {'count': count, 'id': result}}


def recipes() -> None:
    n = lambda x: f'{NS}:{x}'
    dump(DATA / 'recipe/pipe.json', shaped(['III', 'ICI', 'III'], {'I': 'minecraft:iron_ingot', 'C': 'minecraft:chest'}, n('pipe'), 4))  # A31: iron around a chest
    dump(DATA / 'recipe/reinforced_pipe.json', shapeless([n('pipe'), 'simplelib:cracked_diamond', 'simplelib:cracked_diamond'], n('reinforced_pipe')))
    dump(DATA / 'recipe/netherite_pipe.json', shapeless([n('reinforced_pipe'), 'simplelib:netherite_nugget', 'simplelib:netherite_nugget'], n('netherite_pipe')))
    dump(DATA / 'recipe/enderite_pipe.json', shapeless([n('netherite_pipe'), 'simplelib:enderite_nugget', 'simplelib:enderite_nugget'], n('enderite_pipe')))
    dump(DATA / 'recipe/copper_cable.json', shaped([' W ', 'CCC', ' W '], {'W': '#minecraft:wool', 'C': 'minecraft:copper_ingot'}, n('copper_cable'), 3))
    dump(DATA / 'recipe/chest_interface.json', shaped(['GRQ', 'QPG', 'GCQ'], {
        'G': 'minecraft:gold_ingot', 'Q': 'minecraft:quartz', 'R': 'minecraft:repeater', 'P': 'minecraft:glass_pane', 'C': 'minecraft:comparator'},
        n('chest_interface')))
    chests = {'wooden': 'minecraft:chest', 'reinforced': n('reinforced_chest'), 'netherite': n('netherite_chest'), 'enderite': n('enderite_chest')}
    for step, chest in chests.items():
        dump(DATA / f'recipe/{step}_controller_chest.json', shapeless([n('chest_interface'), chest, 'minecraft:writable_book', 'minecraft:ender_pearl'],
                                                                      n(f'{step}_controller_chest')))


def upgrades() -> None:
    def entry(src, dst, material, tool, damage, tier):
        return {'damage_per_hit': damage, 'from': n(src), 'material': material, 'min_tool': tool, 'tier': tier, 'to': n(dst)}
    n = lambda x: f'{NS}:{x}'
    dump(DATA / 'in_world_upgrades/pipes.json', {'upgrades': [
        entry('pipe', 'reinforced_pipe', 'simplelib:cracked_diamond', 'any', 2, 'reinforced'),
        entry('reinforced_pipe', 'netherite_pipe', 'simplelib:netherite_nugget', 'diamond', 4, 'netherite'),
        entry('netherite_pipe', 'enderite_pipe', 'simplelib:enderite_nugget', 'netherite', 10, 'enderite')]})


def tags() -> None:
    path = RES / 'data/minecraft/tags/block/mineable/pickaxe.json'
    data = json.loads(path.read_text(encoding='utf-8'))
    for name in BLOCK_IDS:
        if f'{NS}:{name}' not in data['values']:
            data['values'].append(f'{NS}:{name}')
    dump(path, data)


def main() -> None:
    write_pipes()
    cable = cable_sheet()
    save(cable, ASSETS / 'textures/block/copper_cable.png')
    save(interface_sheet(), ASSETS / 'textures/block/chest_interface.png')
    write_controllers()
    pipe_templates()
    cable_models()
    interface_models()
    controller_models()
    loot()
    recipes()
    upgrades()
    tags()
    print('done')


if __name__ == '__main__':
    main()
