"""Usage: python tools/textures/end_signal_wire_2026_10_02.py <vanilla textures dir>

Astral/Nihil Redstone laid out like vanilla redstone dust (owner 2026-10-02): instead of one 16x16 plane with the
B7b cross on it, the powder is a multipart like minecraft:redstone_wire (dot, side, side_alt, up) with its own,
fixed-coloured dot/line textures (no tint). Colours are the owner's pick B7b variant B (vanilla grey dust onto the
Astralit/Nihilith ramp, off 1..3, on 2..5). The item gets vanilla's redstone pile recoloured. Also rewrites the
switch (thin plate, gem 4x4 in the middle) and lamp (full cube like the redstone lamp) models, which were a
floating plane and a cut 14 px box. Writes textures, models, blockstates and item definitions into the 26.3
overlay and a preview to previews/astral-nihil-redstone-draht-vorschau.png (next to the repo checkout)."""
import json
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
V = sys.argv[1] if len(sys.argv) > 1 else 'build/vanilla-textures/'
sys.argv = [sys.argv[0], V, 'build/unused/']
import proposals_b7_2026_10_02 as b7  # noqa: E402

b7.V = V
A = os.path.join(HERE, '..', '..', 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
PREVIEW = os.environ.get('SB_PREVIEW_DIR', r'C:\Users\o_o\code\minecraft-mods\previews')
POWDERS = (('astral', 'astral_redstone', 'astralit'), ('nihil', 'nihil_redstone', 'nihilith'))
ON = '|'.join(str(i) for i in range(1, 16))


def write_json(rel, data):
    path = os.path.join(A, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def save(im, rel):
    path = os.path.join(A, 'textures', rel + '.png')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path)


def sprite(name):
    return {'sprite': f'simplebuilding:block/{name}', 'force_translucent': True}


def flat(z_from, z_to, uv_up, uv_down):
    return {'from': [0, 0.25, z_from], 'to': [16, 0.25, z_to], 'shade_direction_override': 'up',
            'faces': {'up': {'uv': uv_up, 'texture': '#line'}, 'down': {'uv': uv_down, 'texture': '#line'}}}


# Untinted templates, geometry identical to vanilla's redstone_dust_* models (without the overlay layer).
TEMPLATES = {
    'end_redstone_dust_dot': [flat(0, 16, [0, 0, 16, 16], [0, 16, 16, 0])],
    'end_redstone_dust_side': [flat(0, 8, [0, 0, 16, 8], [0, 8, 16, 0])],
    'end_redstone_dust_side_alt': [flat(8, 16, [0, 8, 16, 16], [0, 16, 16, 8])],
    'end_redstone_dust_up': [{'from': [0, 0, 0.25], 'to': [16, 16, 0.25], 'shade_direction_override': 'up',
                              'faces': {'south': {'uv': [0, 0, 16, 16], 'texture': '#line'},
                                        'north': {'uv': [16, 0, 0, 16], 'texture': '#line'}}}],
}
# model suffix -> (template, line texture suffix)
PARTS = {'dot': ('end_redstone_dust_dot', 'dot'), 'side0': ('end_redstone_dust_side', 'line0'),
         'side1': ('end_redstone_dust_side', 'line1'), 'side_alt0': ('end_redstone_dust_side_alt', 'line0'),
         'side_alt1': ('end_redstone_dust_side_alt', 'line1'), 'up': ('end_redstone_dust_up', 'line1')}


def powder_textures(kind, active):
    ramp = b7.RAMPS[kind]
    lo, hi = (2, 5) if active else (1, 3)
    return {s: b7.tint(b7.load(f'block/redstone_dust_{s}'), ramp, lo, hi) for s in ('dot', 'line0', 'line1')}


def powder_blockstate(prefix):
    sides = ('north', 'east', 'south', 'west')
    parts = []
    for power, suffix in (('0', ''), (ON, '_on')):
        m = lambda part: f'simplebuilding:block/{prefix}_{part}{suffix}'  # noqa: E731
        dot_when = [dict({s: 'none' for s in sides}, power=power)]
        for a, b in (('east', 'north'), ('east', 'south'), ('south', 'west'), ('north', 'west')):
            dot_when.append({a: 'side|up', b: 'side|up', 'power': power})
        parts.append({'apply': {'model': m('dot')}, 'when': {'OR': dot_when}})
        parts.append({'apply': {'model': m('side0')}, 'when': {'north': 'side|up', 'power': power}})
        parts.append({'apply': {'model': m('side_alt0')}, 'when': {'south': 'side|up', 'power': power}})
        parts.append({'apply': {'model': m('side_alt1'), 'y': 270}, 'when': {'east': 'side|up', 'power': power}})
        parts.append({'apply': {'model': m('side1'), 'y': 270}, 'when': {'west': 'side|up', 'power': power}})
        for side, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
            apply = {'model': m('up')}
            if y:
                apply['y'] = y
            parts.append({'apply': apply, 'when': {side: 'up', 'power': power}})
    return {'multipart': parts}


def variants(block, on_model, off_model):
    return {'variants': {f'power={i}': {'model': on_model if i else off_model} for i in range(16)}}


def switch_model(texture):
    side = {'uv': [2, 14, 14, 16], 'texture': '#texture'}
    return {'parent': 'minecraft:block/thin_block',
            'textures': {'texture': f'simplebuilding:block/{texture}', 'particle': f'simplebuilding:block/{texture}'},
            'elements': [{'from': [2, 0, 2], 'to': [14, 2, 14],
                          'faces': {'down': {'uv': [2, 2, 14, 14], 'texture': '#texture', 'cullface': 'down'},
                                    'up': {'uv': [2, 2, 14, 14], 'texture': '#texture'},
                                    'north': side, 'south': side, 'west': side, 'east': side}}]}


def main():
    for name, elements in TEMPLATES.items():
        write_json(f'models/block/{name}.json', {'ambientocclusion': False, 'elements': elements})
    sheet_rows = []
    for kind, prefix, block in POWDERS:
        row = []
        for active, suffix in ((False, ''), (True, '_on')):
            tex = powder_textures(kind, active)
            for s, im in tex.items():
                save(im, f'block/{prefix}_{s}{suffix}')
            for part, (template, line) in PARTS.items():
                write_json(f'models/block/{prefix}_{part}{suffix}.json', {
                    'parent': f'simplebuilding:block/{template}',
                    'textures': {'particle': f'simplebuilding:block/{prefix}_dot{suffix}',
                                 'line': sprite(f'{prefix}_{line}{suffix}')}})
            row.append(tex)
        write_json(f'blockstates/{prefix}.json', powder_blockstate(prefix))
        item = b7.remap(b7.load('item/redstone'), b7.RAMPS[kind], 0, 5)
        save(item, f'item/{prefix}')
        write_json(f'models/item/{prefix}.json', {'parent': 'minecraft:item/generated',
                                                   'textures': {'layer0': f'simplebuilding:item/{prefix}'}})
        # Switch and lamp: real geometry, the item shows the block like vanilla plates and lamps.
        for suffix in ('', '_active'):
            write_json(f'models/block/{block}_switch{suffix}.json', switch_model(f'{block}_switch{suffix}'))
            write_json(f'models/block/{block}_lamp{suffix}.json', {
                'parent': 'minecraft:block/cube_all', 'textures': {'all': f'simplebuilding:block/{block}_lamp{suffix}'}})
        for thing in ('switch', 'lamp'):
            write_json(f'blockstates/{block}_{thing}.json', variants(block, f'simplebuilding:block/{block}_{thing}_active',
                                                                     f'simplebuilding:block/{block}_{thing}'))
            write_json(f'items/{block}_{thing}.json', {'model': {'type': 'minecraft:model',
                                                                 'model': f'simplebuilding:block/{block}_{thing}'}})
            stale = os.path.join(A, 'models', 'item', f'{block}_{thing}.json')
            if os.path.isfile(stale):
                os.remove(stale)
        sheet_rows.append((prefix, item, row))
    preview(sheet_rows)
    print('ok')


def layout(tex, sides):
    """Top view of one dust cell: line halves for the connected sides, dot unless a straight line."""
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    line = tex['line0']
    if 'n' in sides:
        im.alpha_composite(line.crop((0, 0, 16, 8)), (0, 0))
    if 's' in sides:
        im.alpha_composite(line.crop((0, 8, 16, 16)), (0, 8))
    rot = line.rotate(90)
    if 'w' in sides:
        im.alpha_composite(rot.crop((0, 0, 8, 16)), (0, 0))
    if 'e' in sides:
        im.alpha_composite(rot.crop((8, 0, 16, 16)), (8, 0))
    if sides not in ('ns', 'ew'):
        im.alpha_composite(tex['dot'])
    return im


def preview(rows):
    labels = ['A Punkt', 'B Linie N-S', 'C Linie O-W', 'D Ecke', 'E T', 'F Kreuz', 'G Item']
    combos = ['', 'ns', 'ew', 'ne', 'nes', 'nesw']
    scale, pad = 6, 10
    cell = 16 * scale
    sheet = Image.new('RGBA', (pad + len(labels) * (cell + pad) + 140, pad + 4 * (cell + 24)), (139, 139, 139, 255))
    d = ImageDraw.Draw(sheet)
    r = 0
    for prefix, item, (off, on) in rows:
        for tex, state in ((off, 'aus'), (on, 'an')):
            y = pad + r * (cell + 24)
            d.text((pad, y + cell // 2), f'{prefix} {state}', fill=(255, 255, 255))
            for i, combo in enumerate(combos):
                x = 140 + i * (cell + pad)
                bg = Image.new('RGBA', (16, 16), (60, 56, 66, 255))
                bg.alpha_composite(layout(tex, combo))
                sheet.alpha_composite(bg.resize((cell, cell), Image.NEAREST), (x, y))
                if r == 0:
                    d.text((x, 0), labels[i], fill=(255, 255, 255))
            x = 140 + len(combos) * (cell + pad)
            sheet.alpha_composite(item.resize((cell, cell), Image.NEAREST), (x, y))
            if r == 0:
                d.text((x, 0), labels[-1], fill=(255, 255, 255))
            r += 1
    os.makedirs(PREVIEW, exist_ok=True)
    sheet.save(os.path.join(PREVIEW, 'astral-nihil-redstone-draht-vorschau.png'))


if __name__ == '__main__':
    main()
