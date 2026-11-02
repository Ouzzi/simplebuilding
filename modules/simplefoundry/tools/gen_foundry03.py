"""Simple Foundry 02.11. (b-foundry03): the layered melt in the chamber windows (Variant A), the fuel gauge on the desk front, the models and blockstates
for both, and the lang keys of the desk window. Pure Python + Pillow, own pixel art (soft tones, about 7 % hard steps, A8b).
Run: python3.12 modules/simplefoundry/tools/gen_foundry03.py   (then python3 tools/textures/layers.py sync for the layer sources)"""
import json
import random
from pathlib import Path

from PIL import Image

MODULE = Path(__file__).resolve().parents[1]
NS = 'simplefoundry'
A = MODULE / 'shared/resources/assets' / NS
TEX = A / 'textures/block'
METALS = {'gold': (240, 196, 64), 'copper': (206, 120, 78), 'iron': (206, 206, 210)}
PARTS = 4


def dump(path: Path, data) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def shade(rgb, k):
    return tuple(max(0, min(255, round(c * k))) for c in rgb)


def chamber_window() -> None:
    """Melting chamber: the fire clay tiles keep their joints, a 12 x 12 window (x, y 2..13) shows the dark inside."""
    img = Image.open(TEX / 'melting_chamber.png').convert('RGB')
    rng = random.Random(311002)
    dark = [(30, 25, 24), (36, 30, 28), (42, 35, 32), (48, 40, 36)]
    for y in range(2, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            tone = dark[0] if edge else dark[min(3, max(0, 1 + (1 if rng.random() < 0.3 else 0) + (1 if rng.random() < 0.07 else 0) - (1 if y > 10 else 0)))]
            img.putpixel((x, y), tone)
    # light lip at the bottom edge of the window, shade on top: the glass sits in a recess
    for x in range(2, 14):
        img.putpixel((x, 13), (92, 78, 66))
        img.putpixel((x, 2), (22, 18, 17))
    img.save(TEX / 'melting_chamber.png')


def melt_overlay(name: str, rgb) -> None:
    """Window overlay of one metal: every part of 3 rows has a dark bottom row, a body and a lit top row; transparent outside the window."""
    rng = random.Random(sum(map(ord, name)) * 31)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(3, 13):
        band = (13 - y) % 3  # 0 = bottom row of a part
        for x in range(3, 13):
            k = (0.78, 0.95, 1.12)[band] + rng.choice((-0.04, 0, 0, 0.04))
            if rng.random() < 0.07:
                k += rng.choice((-0.15, 0.12))
            img.putpixel((x, y), (*shade(rgb, k), 255))
    # the part edges at the window border (rows 2 and 13, columns 2 and 13) keep a darker rim so the metal sits inside the frame
    for x in range(2, 14):
        for y in (2, 13):
            img.putpixel((x, y), (*shade(rgb, 0.62), 255))
    for y in range(2, 14):
        for x in (2, 13):
            img.putpixel((x, y), (*shade(rgb, 0.62), 255))
    img.save(TEX / f'chamber_melt_{name}.png')


def desk_fronts() -> None:
    """Desk front with the fuel gauge (4 steps) where the orange dial was: x 4..7, y 4..9; level n lights the lowest n of four rows."""
    base = Image.open(TEX / 'foundry_desk_front.png').convert('RGB')
    for y in range(4, 9):
        for x in range(4, 8):
            base.putpixel((x, y), base.getpixel((8, y)))
    lit = [(150, 70, 30), (200, 95, 35), (232, 120, 40), (255, 170, 60)]
    out = (28, 26, 26)
    for level in range(5):
        img = base.copy()
        for y in range(4, 10):
            for x in range(4, 8):
                if y in (4, 9) or x in (4, 7):
                    img.putpixel((x, y), out)
        for row in range(4):
            y = 8 - row
            col = lit[row] if row < level else (58, 40, 34)
            for x in (5, 6):
                img.putpixel((x, y), col)
        img.save(TEX / ('foundry_desk_front.png' if level == 0 else f'foundry_desk_front_f{level}.png'))


def models() -> None:
    dump(A / 'models/block/melting_chamber.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': f'{NS}:block/melting_chamber'}})
    multipart = [{'apply': {'model': f'{NS}:block/melting_chamber'}}]
    for metal in METALS:
        for k in range(PARTS):
            y0, y1 = 2 + 3 * k, 5 + 3 * k
            face = {'texture': '#melt', 'uv': [0, 16 - y1, 16, 16 - y0]}
            dump(A / f'models/block/chamber_melt_{metal}_{k}.json', {'textures': {'melt': f'{NS}:block/chamber_melt_{metal}', 'particle': f'{NS}:block/melting_chamber'}, 'elements': [{
                'from': [-0.01, y0, -0.01], 'to': [16.01, y1, 16.01],
                'faces': {d: {**face, 'cullface': d} for d in ('north', 'south', 'east', 'west')}}]})
            multipart.append({'when': {f's{k}': metal}, 'apply': {'model': f'{NS}:block/chamber_melt_{metal}_{k}'}})
    dump(A / 'blockstates/melting_chamber.json', {'multipart': multipart})
    for level in range(1, 5):
        dump(A / f'models/block/foundry_desk_f{level}.json', {'parent': 'minecraft:block/orientable', 'textures': {
            'top': f'{NS}:block/foundry_desk_top', 'front': f'{NS}:block/foundry_desk_front_f{level}', 'side': f'{NS}:block/foundry_brick'}})
    variants = {}
    for level in range(5):
        model = f'{NS}:block/foundry_desk' + ('' if level == 0 else f'_f{level}')
        for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            variants[f'facing={d},fuel={level}'] = {'model': model, 'y': y} if y else {'model': model}
    dump(A / 'blockstates/foundry_desk.json', {'variants': variants})


EN = {
    'gui.simplefoundry.fuel_hint': 'Fuel slot: coal or lava',
    'gui.simplefoundry.locked': 'Locked: needs a desk with %s places',
    'gui.simplefoundry.locked_item': 'Lift the desk with: %s',
    'gui.simplefoundry.level': 'Level %s: %s',
    'gui.simplefoundry.filled': 'Tank: %s of %s ingots',
    'gui.simplefoundry.arrow': 'Next layer down (back to the top)',
    'gui.simplefoundry.fuel': 'Fuel tank: %s of %s ticks',
    'gui.simplefoundry.fuel_ways': 'Lava bucket (empty one comes back) or coal',
    'gui.simplefoundry.metal': '%s: %s ingots melted, %s waiting',
    'simplefoundry.desk.fuel_full': 'The fuel tank is full or cannot take that (%s)',
}
DE = {
    'gui.simplefoundry.fuel_hint': 'Brennstoffplatz: Kohle oder Lava',
    'gui.simplefoundry.locked': 'Gesperrt: braucht ein Pult mit %s Plätzen',
    'gui.simplefoundry.locked_item': 'Pult aufwerten mit: %s',
    'gui.simplefoundry.level': 'Ebene %s: %s',
    'gui.simplefoundry.filled': 'Tank: %s von %s Barren',
    'gui.simplefoundry.arrow': 'Nächste Schicht nach unten (zurück nach oben)',
    'gui.simplefoundry.fuel': 'Brennstofftank: %s von %s Ticks',
    'gui.simplefoundry.fuel_ways': 'Lava-Eimer (leerer kommt zurück) oder Kohle',
    'gui.simplefoundry.metal': '%s: %s Barren geschmolzen, %s warten',
    'simplefoundry.desk.fuel_full': 'Der Brennstofftank ist voll oder nimmt das nicht (%s)',
}


def lang() -> None:
    for code, extra in (('en_us', EN), ('de_de', DE)):
        path = A / f'lang/{code}.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        data.update(extra)
        dump(path, data)


if __name__ == '__main__':
    chamber_window()
    for name, rgb in METALS.items():
        melt_overlay(name, rgb)
    desk_fronts()
    models()
    lang()
    print('simplefoundry: b-foundry03 data and textures written')
