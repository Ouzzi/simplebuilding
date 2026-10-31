"""Simple Foundry data generator (committed outputs): lang EN/DE, block states, models, item definitions, loot tables, recipes, tags and
the own placeholder textures (Variant A of the preview /root/sb-tools-b/previews/b-foundry32-preview.html). Pure Python + Pillow, no
Vanilla pixels. Run: python3.12 modules/simplefoundry/tools/gen_data.py"""
import json
import random
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
NS = 'simplefoundry'
RES = MODULE / 'shared/resources'
A = RES / f'assets/{NS}'
D = RES / f'data/{NS}'

BLOCKS = ('foundry_brick', 'melting_chamber', 'foundry_desk', 'fire_grate', 'foundry_spout', 'casting_tray')
METALS = ('gold', 'copper', 'iron')

EN = {
    'block.simplefoundry.foundry_brick': 'Foundry Brick',
    'block.simplefoundry.melting_chamber': 'Melting Chamber',
    'block.simplefoundry.foundry_desk': 'Foundry Desk',
    'block.simplefoundry.fire_grate': 'Fire Grate',
    'block.simplefoundry.foundry_spout': 'Foundry Spout',
    'block.simplefoundry.casting_tray': 'Casting Tray',
    'itemgroup.simplefoundry.foundry': 'Simple Foundry',
    'modmenu.nameTranslation.simplefoundry': 'Simple Foundry',
    'simplefoundry.testcentre.station': 'Simple Foundry',
    'simplefoundry.metal.gold': 'Gold',
    'simplefoundry.metal.copper': 'Copper',
    'simplefoundry.metal.iron': 'Iron',
    'simplefoundry.status.ok': 'Foundry ready',
    'simplefoundry.status.no_chamber': 'No melting chamber next to the desk',
    'simplefoundry.status.open_hull': 'The hull is not closed (brick, desk, grate, spout or glass on every side)',
    'simplefoundry.status.too_big': 'Too many melting chambers or too wide',
    'simplefoundry.status.unloaded': 'Part of the foundry is in an unloaded chunk',
    'simplefoundry.status.no_grate': 'No fire grate on the hull',
    'simplefoundry.status.second_desk': 'A second desk touches the tank',
    'simplefoundry.desk.status': 'Foundry: %s',
    'simplefoundry.desk.metal': '%s: %s units melted, %s waiting',
    'simplefoundry.desk.capacity': 'Filled %s of %s units, fire %s s, fuel: %s',
    'simplefoundry.desk.no_fuel': 'none',
    'simplefoundry.desk.refused': 'Nothing taken (%s, or the tank is full)',
    'simplefoundry.desk.fuel_full': 'The fuel slot is full or holds another fuel (%s)',
    'simplefoundry.tray.hot': 'The ingots are still setting',
}
DE = {
    'block.simplefoundry.foundry_brick': 'Hüttenziegel',
    'block.simplefoundry.melting_chamber': 'Schmelzkammer',
    'block.simplefoundry.foundry_desk': 'Hüttenpult',
    'block.simplefoundry.fire_grate': 'Feuerrost',
    'block.simplefoundry.foundry_spout': 'Gusstülle',
    'block.simplefoundry.casting_tray': 'Gussschale',
    'itemgroup.simplefoundry.foundry': 'Simple Foundry',
    'modmenu.nameTranslation.simplefoundry': 'Simple Foundry',
    'simplefoundry.testcentre.station': 'Simple Foundry',
    'simplefoundry.metal.gold': 'Gold',
    'simplefoundry.metal.copper': 'Kupfer',
    'simplefoundry.metal.iron': 'Eisen',
    'simplefoundry.status.ok': 'Gießerei bereit',
    'simplefoundry.status.no_chamber': 'Keine Schmelzkammer am Pult',
    'simplefoundry.status.open_hull': 'Die Hülle ist nicht geschlossen (Ziegel, Pult, Rost, Tülle oder Glas auf jeder Seite)',
    'simplefoundry.status.too_big': 'Zu viele Schmelzkammern oder zu breit',
    'simplefoundry.status.unloaded': 'Ein Teil der Gießerei liegt in einem nicht geladenen Chunk',
    'simplefoundry.status.no_grate': 'Kein Feuerrost in der Hülle',
    'simplefoundry.status.second_desk': 'Ein zweites Pult berührt den Tank',
    'simplefoundry.desk.status': 'Gießerei: %s',
    'simplefoundry.desk.metal': '%s: %s Einheiten geschmolzen, %s warten',
    'simplefoundry.desk.capacity': 'Gefüllt %s von %s Einheiten, Feuer %s s, Brennstoff: %s',
    'simplefoundry.desk.no_fuel': 'keiner',
    'simplefoundry.desk.refused': 'Nichts genommen (%s, oder der Tank ist voll)',
    'simplefoundry.desk.fuel_full': 'Der Brennstoffplatz ist voll oder hält einen anderen Brennstoff (%s)',
    'simplefoundry.tray.hot': 'Die Barren erstarren noch',
}


def dump(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def item_def(name: str, model: str | None = None) -> None:
    dump(A / f'items/{name}.json', {'model': {'type': 'minecraft:model', 'model': model or f'{NS}:block/{name}'}})


def cube_all(name: str) -> None:
    dump(A / f'models/block/{name}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/{name}'}})
    dump(A / f'blockstates/{name}.json', {'variants': {'': {'model': f'{NS}:block/{name}'}}})
    item_def(name)


def facing_variants(model: str) -> dict:
    return {f'facing={d}': {'model': model, 'y': y} if y else {'model': model}
            for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}


def recipe(name: str, pattern, key, result, count=1) -> None:
    dump(D / f'recipe/{name}.json', {'type': 'minecraft:crafting_shaped', 'category': 'building', 'key': key, 'pattern': pattern,
                                     'result': {'id': f'{NS}:{name}', **({'count': count} if count > 1 else {})}})


def data() -> None:
    dump(A / 'lang/en_us.json', EN)
    dump(A / 'lang/de_de.json', DE)
    cube_all('foundry_brick')
    cube_all('melting_chamber')
    dump(A / 'models/block/foundry_desk.json', {'parent': 'minecraft:block/orientable', 'textures': {
        'top': f'{NS}:block/foundry_desk_top', 'front': f'{NS}:block/foundry_desk_front', 'side': f'{NS}:block/foundry_brick'}})
    dump(A / 'blockstates/foundry_desk.json', {'variants': facing_variants(f'{NS}:block/foundry_desk')})
    item_def('foundry_desk')
    dump(A / 'models/block/fire_grate.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/fire_grate'}})
    dump(A / 'models/block/fire_grate_lit.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/fire_grate_lit'}})
    dump(A / 'blockstates/fire_grate.json', {'variants': {'lit=false': {'model': f'{NS}:block/fire_grate'}, 'lit=true': {'model': f'{NS}:block/fire_grate_lit'}}})
    item_def('fire_grate')
    dump(A / 'models/block/foundry_spout.json', {'parent': 'minecraft:block/orientable', 'textures': {
        'top': f'{NS}:block/foundry_brick', 'front': f'{NS}:block/foundry_spout_front', 'side': f'{NS}:block/foundry_brick'}})
    dump(A / 'blockstates/foundry_spout.json', {'variants': facing_variants(f'{NS}:block/foundry_spout')})
    item_def('foundry_spout')
    # tray: a low box; the top texture shows the metal
    dump(A / 'models/block/casting_tray_base.json', {'textures': {'particle': f'{NS}:block/casting_tray_side'}, 'elements': [{
        'from': [1, 0, 1], 'to': [15, 5, 15], 'faces': {
            'down': {'texture': '#side', 'uv': [1, 1, 15, 15], 'cullface': 'down'},
            'up': {'texture': '#top', 'uv': [1, 1, 15, 15]},
            'north': {'texture': '#side', 'uv': [1, 11, 15, 16]}, 'south': {'texture': '#side', 'uv': [1, 11, 15, 16]},
            'west': {'texture': '#side', 'uv': [1, 11, 15, 16]}, 'east': {'texture': '#side', 'uv': [1, 11, 15, 16]}}}]})
    for i, content in enumerate(('empty',) + METALS):
        dump(A / f'models/block/casting_tray_{content}.json', {'parent': f'{NS}:block/casting_tray_base', 'textures': {
            'top': f'{NS}:block/casting_tray_{content}', 'side': f'{NS}:block/casting_tray_side'}})
    dump(A / 'blockstates/casting_tray.json', {'variants': {f'content={i}': {'model': f'{NS}:block/casting_tray_{c}'}
                                                          for i, c in enumerate(('empty',) + METALS)}})
    item_def('casting_tray', f'{NS}:block/casting_tray_empty')
    for b in BLOCKS:
        dump(D / f'loot_table/blocks/{b}.json', {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'condition': {'type': 'minecraft:survives_explosion'}, 'entries': [
            {'type': 'minecraft:item', 'name': f'{NS}:{b}'}]}], 'random_sequence': f'{NS}:blocks/{b}'})
    dump(RES / 'data/minecraft/tags/block/mineable/pickaxe.json', {'values': [f'{NS}:{b}' for b in BLOCKS]})
    dump(D / 'tags/block/hull.json', {'values': [f'{NS}:{b}' for b in ('foundry_brick', 'foundry_desk', 'fire_grate', 'foundry_spout')] + ['#minecraft:impermeable']})
    n = f'{NS}:foundry_brick'
    recipe('foundry_brick', ['BSB', 'SCS', 'BSB'], {'B': 'minecraft:brick', 'S': 'minecraft:smooth_stone', 'C': '#minecraft:coals'}, None, 4)
    recipe('melting_chamber', ['BPB', 'PUP', 'BPB'], {'B': n, 'P': 'minecraft:glass_pane', 'U': 'minecraft:bucket'}, None)
    recipe('foundry_desk', ['BBB', 'BFB', 'BBB'], {'B': n, 'F': 'minecraft:furnace'}, None)
    recipe('fire_grate', ['BBB', ' F ', 'BUB'], {'B': n, 'F': 'minecraft:blast_furnace', 'U': 'minecraft:bucket'}, None)
    recipe('foundry_spout', ['BBB', ' U ', ' H '], {'B': n, 'U': 'minecraft:bucket', 'H': 'minecraft:hopper'}, None, 2)
    recipe('casting_tray', ['B B', ' C ', 'B B'], {'B': n, 'C': 'minecraft:cauldron'}, None)


# ---- textures (own pixel art, soft tones with about 7 % hard steps) ----
def shade(rgb, k):
    return tuple(max(0, min(255, round(c * k))) for c in rgb)


def ramp(base, n=9):
    return [shade(base, 0.55 + 0.9 * i / (n - 1)) for i in range(n)]


def noise_tile(rng, tones, size=16, hard=0.07):
    mid = len(tones) // 2
    idx = [[mid] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            left = idx[y][x - 1] if x else mid
            up = idx[y - 1][x] if y else mid
            base = (left + up) // 2
            step = rng.choice((-1, 0, 0, 1))
            if rng.random() < hard:
                step = rng.choice((-3, -2, 2, 3))
            idx[y][x] = max(0, min(len(tones) - 1, base + step + (mid - base) // 4))
    return idx


def brick_pattern(rng, base, mortar, rows=4, bw=8):
    tones = ramp(base)
    img = Image.new('RGB', (16, 16))
    rh = 16 // rows
    for r in range(rows):
        shift = 0 if r % 2 == 0 else bw // 2
        for b in range(16 // bw + 1):
            level = rng.randint(3, 5)
            for y in range(r * rh, r * rh + rh - 1):
                for dx in range(bw - 1):
                    x = (b * bw + dx - shift) % 16
                    j = rng.choice((-1, 0, 0, 1)) + (rng.choice((-2, 2)) if rng.random() < 0.07 else 0)
                    shade_i = level + j + (1 if y == r * rh else 0) - (1 if y == r * rh + rh - 2 else 0)
                    img.putpixel((x, y), tones[max(0, min(len(tones) - 1, shade_i))])
        for x in range(16):
            img.putpixel((x, r * rh + rh - 1), mortar)
        for b in range(2):
            mx = (b * bw - shift - 1) % 16
            for y in range(r * rh, r * rh + rh):
                img.putpixel((mx, y), mortar)
    return img


def save(img, name):
    path = A / f'textures/block/{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def textures() -> None:
    rng = random.Random(3110)
    soot = (96, 72, 64)
    mortar = (44, 36, 34)
    brick = brick_pattern(rng, soot, mortar)
    save(brick, 'foundry_brick')
    # melting chamber: fire clay tiles 8x8 with a dark joint
    clay = ramp((176, 150, 112))
    cham = Image.new('RGB', (16, 16))
    idx = noise_tile(rng, clay)
    for y in range(16):
        for x in range(16):
            cham.putpixel((x, y), (40, 33, 30) if x in (0, 8) or y in (0, 8) else clay[idx[y][x]])
    for y in range(16):
        for x in range(16):
            if (x in (1, 9) or y in (1, 9)) and cham.getpixel((x, y)) != (40, 33, 30):
                cham.putpixel((x, y), shade(cham.getpixel((x, y)), 1.15))
    save(cham, 'melting_chamber')
    # desk: front = brick with an iron plate, two gauges and a slot; top = iron plate
    front = brick.copy()
    plate = ramp((120, 124, 130))
    pi = noise_tile(rng, plate)
    for y in range(3, 13):
        for x in range(2, 14):
            front.putpixel((x, y), plate[pi[y][x]])
    for x in range(2, 14):
        front.putpixel((x, 3), plate[7])
        front.putpixel((x, 12), plate[1])
    for cx, col in ((5, (220, 120, 60)), (10, (90, 190, 120))):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1), (2, 0), (0, 2), (2, 1), (1, 2), (-1, 1), (1, -1)):
            if abs(dx - 0.5) + abs(dy - 0.5) <= 2:
                front.putpixel((cx + dx - 0, 5 + dy), shade(col, 0.8))
        front.putpixel((cx, 5), col)
        front.putpixel((cx + 1, 6), shade(col, 1.2))
    for x in range(4, 12):
        front.putpixel((x, 9), (28, 26, 26))
        front.putpixel((x, 10), (28, 26, 26))
    save(front, 'foundry_desk_front')
    top = Image.new('RGB', (16, 16))
    ti = noise_tile(rng, plate)
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            top.putpixel((x, y), shade(plate[ti[y][x]], 0.7) if edge else plate[ti[y][x]])
    for y in range(5, 11):
        for x in range(5, 11):
            top.putpixel((x, y), (30, 28, 28))
    save(top, 'foundry_desk_top')
    # fire grate: iron bars over ash (unlit) or glowing coals (lit)
    for name, bg in (('fire_grate', (58, 50, 48)), ('fire_grate_lit', (228, 112, 40))):
        img = Image.new('RGB', (16, 16))
        tones = ramp(bg)
        gi = noise_tile(rng, tones, hard=0.07)
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), tones[gi[y][x]])
        bars = ramp((70, 72, 78))
        for x in (1, 5, 9, 13):
            for y in range(16):
                img.putpixel((x, y), bars[6 if y % 5 else 3])
                img.putpixel((x + 1, y), bars[4])
        for y in (0, 15):
            for x in range(16):
                img.putpixel((x, y), bars[2])
        save(img, name)
    # spout: brick with a dark mouth and a metal lip
    spout = brick.copy()
    for y in range(7, 13):
        for x in range(5, 11):
            if (x - 7.5) ** 2 + (y - 9.5) ** 2 <= 9:
                spout.putpixel((x, y), (24, 20, 20))
    for x in range(4, 12):
        spout.putpixel((x, 13), plate[6])
        spout.putpixel((x, 14), plate[2])
    save(spout, 'foundry_spout_front')
    # tray: iron rim, top shows the metal as a 3x3 ingot grid
    rim = ramp((84, 86, 92))
    side = Image.new('RGB', (16, 16))
    si = noise_tile(rng, rim)
    for y in range(16):
        for x in range(16):
            side.putpixel((x, y), rim[si[y][x]])
    save(side, 'casting_tray_side')
    contents = {'empty': (52, 44, 40), 'gold': (240, 196, 64), 'copper': (206, 120, 78), 'iron': (206, 206, 210)}
    for name, col in contents.items():
        img = Image.new('RGB', (16, 16))
        for y in range(16):
            for x in range(16):
                img.putpixel((x, y), rim[7] if min(x, y, 15 - x, 15 - y) < 1 else rim[3] if min(x, y, 15 - x, 15 - y) < 2 else shade(contents['empty'], 1.0))
        if name != 'empty':
            tones = ramp(col)
            for gx in range(3):
                for gy in range(3):
                    x0, y0 = 2 + gx * 4, 2 + gy * 4
                    for dy in range(3):
                        for dx in range(3):
                            img.putpixel((x0 + dx, y0 + dy), tones[7 if dy == 0 or dx == 0 else 4 if dy == 2 or dx == 2 else 6])
        save(img, f'casting_tray_{name}')


if __name__ == '__main__':
    data()
    textures()
    print('simplefoundry: data and textures written')
