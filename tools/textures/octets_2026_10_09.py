"""Usage: python tools/textures/octets_2026_10_09.py [--check] [--preview DIR] [--vanilla DIR]

Material-Achtel (Queue Nachtrag 24, docs/ai/PLAN-Q-HAMMER-OCTETS-2026-10-09.md). Writes into the 26.3 overlay:

  blockstates/<wood>_octet.json, blockstates/melon_octet.json   multipart: bit -> corner model (x/y rotation, uvlock)
  models/block/octet/<wood>.json         the chess corner template with the vanilla planks texture (no copy)
  models/block/octet/melon_corner.json   one melon octet: outer faces rind (melon_side / melon_top), cut faces flesh
  models/item/octet/<wood>_octet.json + items/<wood>_octet.json   the octet item (the chess octet item shape)
  textures/block/melon_flesh.png         own 16x16 pixel art: the cut melon (flesh with seeds)

N19/N15 (claude-q-octets2): one octet cell + item for every block that has stairs and slabs (vanilla + this mod,
without planks (the 13 woods above) and the chess checkers). The list lives in tools/textures/octet_materials.json
(made by --scan from the 26.3 client jar, the extracted German lang file and src/main/generated) and produces:

  models/block/octet/<base>.json, blockstates/<base>_octet.json, models/item/octet/<base>_octet.json, items/<base>_octet.json
  common/.../blocks/OctetMaterials.java      the list the game registers the cells from
  lang en_us/de_de                            names of all cells and items (generated block after melon_octet)

--check compares instead of writing. --preview DIR renders octet examples (needs the vanilla textures in --vanilla DIR,
an extracted assets/ tree; nothing of it is checked in). Needs Pillow.
"""
import io
import json
import os
import sys
import zipfile

from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
ASSETS = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'assets', 'simplebuilding')
MATERIALS_JSON = os.path.join(REPO, 'tools', 'textures', 'octet_materials.json')
JAVA_LIST = os.path.join(REPO, 'common', 'src', 'shared', 'java', 'com', 'simplebuilding', 'blocks', 'OctetMaterials.java')
LANG_DIR = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'lang')
MOD_GENERATED = os.path.join(REPO, 'src', 'main', 'generated', 'assets', 'simplebuilding')
CLIENT_JAR = '/root/vanilla263/client.jar'
DE_LANG = '/root/.gradle/caches/neoformruntime/assets/objects/76/766975253e4de94ffc2e7e6d3b10b209da2f52c4'
# Order = ModBlocks.OCTET_WOODS
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'poplar',
         'bamboo', 'crimson', 'warped']

# melon flesh: the reds of the vanilla melon slice, seeds in its darkest red-brown
FLESH = {'r': (191, 49, 35), 'l': (193, 60, 45), 'h': (202, 113, 93), 'd': (175, 22, 11), 's': (89, 24, 15),
         'k': (122, 24, 14)}
# N31: 1 px very dark line between rind and flesh. With uvlock the texture border is exactly where a cut face meets
# the block's outer (rind) faces, so the line goes round the texture's edge.
FLESH_EDGE = (28, 38, 14)
FLESH_MAP = [
    'rrlrrdrrlrrrdrlr',
    'rlrrrrrsrrlrrrrr',
    'rrrdrlrkrrrrlrdr',
    'lrsrrrrrrdrrrrrr',
    'rrkrrlrrrrrsrlrr',
    'rrrrrrrdlrrkrrrr',
    'rdrlrrrrrrrrrrdl',
    'rrrrrsrrrlrrdrrr',
    'rlrrrkrrrrrrrrrr',
    'rrrdrrrlrrsrrrlr',
    'hrrrrrrrrrkrdrrr',
    'rrlrsrdrrrrrrrrr',
    'rrrrkrrrlrrrrsrl',
    'rdrrrrrrrrdrrkrr',
    'rrrlrrhrrrrlrrrr',
    'rlrrrdrrrdrrrrdr',
]


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def js(data):
    return (json.dumps(data, indent=2) + '\n').encode('utf-8')


def flesh():
    img = Image.new('RGB', (16, 16))
    for y, row in enumerate(FLESH_MAP):
        for x, c in enumerate(row):
            img.putpixel((x, y), FLESH_EDGE if x in (0, 15) or y in (0, 15) else FLESH[c])
    return img


def rotate(pos, xr, yr):
    x, y, z = pos
    if xr == 180:
        y, z = 1 - y, 1 - z
    for _ in range(yr // 90):
        x, z = 1 - z, x
    return x, y, z


def corner_rotation(x, y, z):
    for xr in (0, 180):
        for yr in (0, 90, 180, 270):
            if rotate((0, 0, 0), xr, yr) == (x, y, z):
                return xr, yr
    raise ValueError((x, y, z))


def multipart(model):
    parts = []
    for i in range(8):
        x, y, z = i & 1, (i >> 1) & 1, (i >> 2) & 1
        xr, yr = corner_rotation(x, y, z)
        apply = {'model': model, 'uvlock': True}
        if xr:
            apply['x'] = xr
        if yr:
            apply['y'] = yr
        parts.append({'when': {f'o{x}{y}{z}': 'true'}, 'apply': apply})
    return {'multipart': parts}


# --- materials (N19/N15) ---
GROUPS = [  # (group, label) in creative-tab order
    ('stone', 'Stone'), ('sandstone', 'Sandstone'), ('bricks', 'Bricks'), ('deepslate', 'Deepslate and blackstone'),
    ('tuff', 'Tuff'), ('prismarine', 'Prismarine'), ('quartz', 'Quartz and purpur'), ('copper', 'Copper'),
    ('wool', 'Wool'), ('concrete', 'Concrete'), ('mod', 'SimpleBuilding'), ('other', 'Other')]
DYES = ['white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'cyan', 'light_blue',
        'blue', 'purple', 'magenta', 'pink']
# ids of the chess octets (ChessColor); a material with the same name gets '_block' in its id and name
CHESS_IDS = {'quartz', 'purpur', 'lapis', 'blackstone', 'resin', 'nether_brick', 'red_nether_brick', 'nihilith', 'astralit',
             'ender_quartz', 'polished_astralit', 'polished_nihilith', 'polished_ender_quartz'}
COPPER_ORDER = ['cut_copper', 'exposed_cut_copper', 'weathered_cut_copper', 'oxidized_cut_copper']


def group_of(ns, base):
    if ns != 'minecraft':
        return 'mod'
    if 'copper' in base:
        return 'copper'
    if base.endswith('_wool'):
        return 'wool'
    if base.endswith('_concrete'):
        return 'concrete'
    if 'sandstone' in base:
        return 'sandstone'
    if 'prismarine' in base:
        return 'prismarine'
    if 'tuff' in base:
        return 'tuff'
    if 'deepslate' in base or 'blackstone' in base:
        return 'deepslate'
    if 'quartz' in base or 'purpur' in base:
        return 'quartz'
    if 'brick' in base or base == 'bamboo_mosaic':
        return 'bricks'
    if base in ('stone', 'cobblestone', 'mossy_cobblestone') or any(k in base for k in ('granite', 'diorite', 'andesite')):
        return 'stone'
    return 'other'


def sort_key(m):
    b = m['base']
    gi = [g for g, _ in GROUPS].index(m['group'])
    if m['group'] == 'copper':
        plain = b.replace('waxed_', '')
        return (gi, b.startswith('waxed_'), COPPER_ORDER.index(plain) if plain in COPPER_ORDER else 9, b)
    if m['group'] in ('wool', 'concrete'):
        return (gi, False, DYES.index(b.rsplit('_', 1)[0]), b)
    return (gi, False, 0, b)


def forward_stairs(base, has):
    """Mirror of SledgehammerItem.reshapeTarget(block, false, true): the stairs of a full block, or None."""
    cands = [base + '_stairs']
    if base.endswith('_planks'):
        cands.append(base[:-7] + '_stairs')
    if base.endswith('_block'):
        cands.append(base[:-6] + '_stairs')
    if base.endswith('s'):
        cands.append(base[:-1] + '_stairs')
    for c in cands:
        if has(c):
            return c
    return None


def scan():
    z = zipfile.ZipFile(CLIENT_JAR)
    names = z.namelist()
    van = {n[len('assets/minecraft/blockstates/'):-5] for n in names
           if n.startswith('assets/minecraft/blockstates/') and n.endswith('.json')}
    mod_dir = os.path.join(MOD_GENERATED, 'blockstates')
    mod = {n[:-5] for n in os.listdir(mod_dir) if n.endswith('.json')}
    en_van = json.loads(z.read('assets/minecraft/lang/en_us.json'))
    de_van = json.load(open(DE_LANG, encoding='utf-8'))
    en_mod = json.load(open(os.path.join(LANG_DIR, 'en_us.json'), encoding='utf-8'))
    de_mod = json.load(open(os.path.join(LANG_DIR, 'de_de.json'), encoding='utf-8'))
    out = []
    for ns, have in (('minecraft', van), ('simplebuilding', mod)):
        for base in sorted(have):
            if base.endswith('_planks') or base in ('bamboo', 'bamboo_block') or 'checker' in base or base.endswith(('_stairs', '_slab', '_wall')):
                continue
            stairs = forward_stairs(base, lambda n: n in have)
            if not stairs or stairs.replace('_stairs', '') + '_slab' not in have:
                continue
            if ns == 'minecraft':
                state = json.loads(z.read(f'assets/minecraft/blockstates/{stairs}.json'))
                ref = next(iter(state['variants'].values()))
                ref = (ref[0] if isinstance(ref, list) else ref)['model'].split(':')[-1]
                model = json.loads(z.read(f'assets/minecraft/models/{ref}.json'))
                en = en_van.get(f'block.minecraft.{base}')
                de = de_van.get(f'block.minecraft.{base}')
            else:
                model = json.load(open(os.path.join(MOD_GENERATED, 'models', 'block', stairs + '.json')))
                en = en_mod.get(f'block.simplebuilding.{base}')
                de = de_mod.get(f'block.simplebuilding.{base}')
            t = model.get('textures', {})
            if not (en and de and {'bottom', 'side', 'top'} <= set(t)):
                print('SKIP (no textures/name): ' + base, file=sys.stderr)
                continue
            if t['bottom'] == t['side'] == t['top']:
                tex = {'all': t['side']}
            else:
                tex = {'up': t['top'], 'down': t['bottom'], 'side': t['side']}
            key = base + '_block' if base in CHESS_IDS else base
            out.append({'ns': ns, 'base': base, 'key': key, 'group': group_of(ns, base), 'tex': tex, 'en': en, 'de': de})
    out.sort(key=sort_key)
    with open(MATERIALS_JSON, 'w', encoding='utf-8') as f:
        f.write(json.dumps(out, indent=1, ensure_ascii=False) + '\n')
    print(f'scanned {len(out)} materials')


def materials():
    with open(MATERIALS_JSON, encoding='utf-8') as f:
        return json.load(f)


def sided_corner():
    face = lambda t, **kw: dict({'texture': t}, **kw)
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#side'},
            'elements': [{'from': [0, 0, 0], 'to': [8, 8, 8], 'faces': {
                'north': face('#side', cullface='north'), 'south': face('#side'), 'east': face('#side'),
                'west': face('#side', cullface='west'), 'up': face('#up'), 'down': face('#down', cullface='down')}}]}


def java_list(mats):
    rows = ',\n'.join(f'            new Material("{m["ns"]}", "{m["base"]}", "{m["key"]}", "{m["group"]}")' for m in mats)
    groups = ', '.join(f'"{g}"' for g, _ in GROUPS)
    return f"""package com.simplebuilding.blocks;

import java.util.List;

/**
 * GENERATED by tools/textures/octets_2026_10_09.py from tools/textures/octet_materials.json - do not edit by hand.
 * Every block with stairs and slabs (vanilla and this mod, without planks and chess checkers) gets an octet cell
 * {{@code <key>_octet}} (Queue N19/N15); {{@link ModBlocks#MATERIAL_OCTETS}} registers them, in creative-tab order.
 */
public final class OctetMaterials {{
    public record Material(String namespace, String base, String key, String group) {{
    }}

    /** Creative-tab groups in display order. */
    public static final List<String> GROUPS = List.of({groups});

    public static final List<Material> ALL = List.of(
{rows});

    private OctetMaterials() {{
    }}
}}
"""


def merged_lang(lang, mats):
    path = os.path.join(LANG_DIR, lang + '.json')
    with open(path, encoding='utf-8') as f:
        data = json.load(f)
    own = set()
    for m in mats:
        own.update((f'item.simplebuilding.{m["key"]}_octet', f'block.simplebuilding.{m["key"]}_octet'))
    out = {}
    for k, v in data.items():
        if k in own:
            continue
        out[k] = v
        if k == 'block.simplebuilding.melon_octet':
            for m in mats:
                name = m['en' if lang == 'en_us' else 'de']
                if m['key'] != m['base']:
                    name += ' Block' if lang == 'en_us' else '-Block'
                suffix = ('{} Octet', '{} Octet Cell') if lang == 'en_us' else ('{}-Achtelblock', '{}-Achtelzelle')
                out[f'item.simplebuilding.{m["key"]}_octet'] = suffix[0].format(name)
                out[f'block.simplebuilding.{m["key"]}_octet'] = suffix[1].format(name)
    return path, (json.dumps(out, indent=2, ensure_ascii=False) + '\n').encode('utf-8')


def outputs():
    files = {}
    a = lambda rel, data: files.__setitem__(os.path.join(ASSETS, *rel.split('/')), data)
    for wood in WOODS:
        a(f'models/block/octet/{wood}.json', js({
            'parent': 'simplebuilding:block/chess/octet_corner', 'textures': {'all': f'minecraft:block/{wood}_planks'}}))
        a(f'blockstates/{wood}_octet.json', js(multipart(f'simplebuilding:block/octet/{wood}')))
        a(f'models/item/octet/{wood}_octet.json', js({
            'parent': 'simplebuilding:item/chess/octet', 'textures': {'all': f'minecraft:block/{wood}_planks'}}))
        a(f'items/{wood}_octet.json', js({'model': {'type': 'minecraft:model', 'model': f'simplebuilding:item/octet/{wood}_octet'}}))
    # Melon: the corner's outer faces (north, west, down) carry the rind, the three cut faces the flesh.
    outer = {'texture': '#side'}
    a('models/block/octet/melon_corner.json', js({
        'parent': 'minecraft:block/block',
        'textures': {'particle': 'minecraft:block/melon_side', 'side': 'minecraft:block/melon_side',
                     'end': 'minecraft:block/melon_top', 'cut': 'simplebuilding:block/melon_flesh'},
        'elements': [{'from': [0, 0, 0], 'to': [8, 8, 8], 'faces': {
            'north': dict(outer, cullface='north'), 'west': dict(outer, cullface='west'),
            'down': {'texture': '#end', 'cullface': 'down'},
            'south': {'texture': '#cut'}, 'east': {'texture': '#cut'}, 'up': {'texture': '#cut'}}}]}))
    a('blockstates/melon_octet.json', js(multipart('simplebuilding:block/octet/melon_corner')))
    a('textures/block/melon_flesh.png', png_bytes(flesh()))
    mats = materials()
    a('models/block/octet/_corner_sided.json', js(sided_corner()))
    a('models/item/octet/_sided.json', js({
        'parent': 'simplebuilding:item/chess/octet', 'textures': {'particle': '#side'},
        'elements': [{'from': [4, 4, 4], 'to': [12, 12, 12], 'faces': {
            'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'},
            'west': {'texture': '#side'}, 'up': {'texture': '#up'}, 'down': {'texture': '#down'}}}]}))
    for m in mats:
        b = m['key']
        if 'all' in m['tex']:
            blk = {'parent': 'simplebuilding:block/chess/octet_corner', 'textures': {'all': m['tex']['all']}}
            itm = {'parent': 'simplebuilding:item/chess/octet', 'textures': {'all': m['tex']['all']}}
        else:
            blk = {'parent': 'simplebuilding:block/octet/_corner_sided', 'textures': m['tex']}
            itm = {'parent': 'simplebuilding:item/octet/_sided', 'textures': m['tex']}
        a(f'models/block/octet/{b}.json', js(blk))
        a(f'blockstates/{b}_octet.json', js(multipart(f'simplebuilding:block/octet/{b}')))
        a(f'models/item/octet/{b}_octet.json', js(itm))
        a(f'items/{b}_octet.json', js({'model': {'type': 'minecraft:model', 'model': f'simplebuilding:item/octet/{b}_octet'}}))
    files[JAVA_LIST] = java_list(mats).encode('utf-8')
    for lang in ('en_us', 'de_de'):
        path, data = merged_lang(lang, mats)
        files[path] = data
    return files


# --- preview: isometric voxels, one texture per voxel face kind ---
def render(cells, tex, size=6):
    """cells: {(x, y, z): key} on a 0.5 grid (octets); tex[key] = (top, side) 16x16 images. View from south-east."""
    w = size * 8
    pts = []
    for (x, y, z) in cells:
        pts.append((x, y, z))
    img = Image.new('RGBA', (w * 6, w * 6), (0, 0, 0, 0))
    ox, oy = w * 3, w * 3

    def proj(x, y, z):
        return ox + (x - z) * w * 0.87, oy + (x + z) * w * 0.5 - y * w

    order = sorted(cells, key=lambda p: (p[0] + p[2], p[1]))
    d = ImageDraw.Draw(img)
    for (x, y, z) in order:
        top, side = tex[cells[(x, y, z)]]
        ct = top.resize((1, 1), Image.BOX).getpixel((0, 0))[:3]
        cs = side.resize((1, 1), Image.BOX).getpixel((0, 0))[:3]
        s = 0.5
        faces = [
            ([(x, y + s, z), (x + s, y + s, z), (x + s, y + s, z + s), (x, y + s, z + s)], ct, 1.0),
            ([(x, y, z + s), (x + s, y, z + s), (x + s, y + s, z + s), (x, y + s, z + s)], cs, 0.8),
            ([(x + s, y, z), (x + s, y, z + s), (x + s, y + s, z + s), (x + s, y + s, z)], cs, 0.6),
        ]
        for quad, col, shade in faces:
            d.polygon([proj(*p) for p in quad], fill=tuple(int(c * shade) for c in col) + (255,), outline=(40, 40, 40, 255))
    bbox = img.getbbox()
    return img.crop(bbox) if bbox else img


def textured_face(tex, size=64):
    return tex.resize((size, size), Image.NEAREST)


def preview(files, vanilla, out_dir):
    vt = lambda n: Image.open(os.path.join(vanilla, 'assets', 'minecraft', 'textures', 'block', n + '.png')).convert('RGBA')
    fl = Image.open(io.BytesIO(files[os.path.join(ASSETS, 'textures', 'block', 'melon_flesh.png')])).convert('RGBA')
    tex = {w: (vt(w + '_planks'), vt(w + '_planks')) for w in WOODS}
    tex['melon'] = (vt('melon_top'), vt('melon_side'))
    tex['flesh'] = (fl, fl)
    full = {(x * 0.5, y * 0.5, z * 0.5) for x in (0, 1) for y in (0, 1) for z in (0, 1)}
    examples = [
        ('oak: 1 Achtel oben weg = Treppe', 'oak', full - {(0.5, 0.5, 0.5)}),
        ('oak: oben+unten je 1 weg = Achtel', 'oak', full - {(0.5, 0.5, 0.5), (0.0, 0.0, 0.5)}),
        ('spruce: Diagonale = Achtel', 'spruce', full - {(0.5, 0.5, 0.5), (0.0, 0.5, 0.0)}),
        ('cherry: Stufe - 1 = Achtel', 'cherry', {p for p in full if p[1] == 0} - {(0.5, 0.0, 0.5)}),
        ('crimson: Saeule 2/8', 'crimson', {(0.0, 0.0, 0.0), (0.0, 0.5, 0.0)}),
        ('melon: 7/8 (Schnitt = Fruchtfleisch)', 'melon', full - {(0.5, 0.5, 0.5)}),
    ]
    font = ImageFont.load_default()
    cw, ch = 230, 210
    img = Image.new('RGBA', (cw * 3, ch * 2 + 40 + 120), (198, 198, 198, 255))
    d = ImageDraw.Draw(img)
    d.text((8, 8), 'Material-Achtel (Queue N24): Hammer-Zerlegung und Melone - Farben je Flaeche gemittelt', fill=(0, 0, 0, 255), font=font)
    for i, (label, key, cells) in enumerate(examples):
        cx, cy = (i % 3) * cw, 40 + (i // 3) * ch
        cellmap = {}
        for p in cells:
            k = key
            if key == 'melon':
                # cut faces show flesh: approximate by tinting the voxel whose +x/+z/+y neighbour is missing
                k = 'flesh' if any((p[0] + dx, p[1] + dy, p[2] + dz) not in cells and 0 <= p[0] + dx <= 0.5
                                   and 0 <= p[1] + dy <= 0.5 and 0 <= p[2] + dz <= 0.5
                                   for dx, dy, dz in ((0.5, 0, 0), (0, 0.5, 0), (0, 0, 0.5))) else 'melon'
            cellmap[p] = k
        sprite = render(cellmap, tex, 12)
        img.alpha_composite(sprite, (cx + (cw - sprite.width) // 2, cy + 10))
        d.text((cx + 6, cy + ch - 24), label, fill=(0, 0, 0, 255), font=font)
    # textures row: the new flesh texture and the wood planks used by the octets
    y = 40 + 2 * ch
    d.text((8, y), 'neu: melon_flesh.png (x4) | Holz-Achtel nutzen die Vanilla-Bretter:', fill=(0, 0, 0, 255), font=font)
    img.alpha_composite(textured_face(fl), (8, y + 16))
    for i, w in enumerate(WOODS):
        img.alpha_composite(textured_face(tex[w][0], 32), (90 + i * 40, y + 16))
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, 'octets-n24.png')
    img.save(path)
    print('preview ' + path)


def main():
    if '--scan' in sys.argv:
        scan()
        return
    check = '--check' in sys.argv
    files = outputs()
    bad = []
    for path, data in files.items():
        if check:
            try:
                with open(path, 'rb') as f:
                    if f.read() != data:
                        bad.append(path)
            except OSError:
                bad.append(path)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, 'wb') as f:
                f.write(data)
    if check:
        if bad:
            print('OUTDATED: ' + str(len(bad)) + ' files, e.g. ' + bad[0])
            sys.exit(1)
        print(f'OK: {len(files)} octet files up to date')
        return
    print(f'wrote {len(files)} files')
    if '--preview' in sys.argv:
        out_dir = sys.argv[sys.argv.index('--preview') + 1]
        vanilla = sys.argv[sys.argv.index('--vanilla') + 1]
        preview(files, vanilla, out_dir)


if __name__ == '__main__':
    main()
