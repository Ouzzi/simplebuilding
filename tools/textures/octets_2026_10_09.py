"""Usage: python tools/textures/octets_2026_10_09.py [--check] [--preview DIR] [--vanilla DIR] [--scan]

Simple Octets (split stage 6, docs/ai/PLAN-S6-OCTETS-2026-10-17.md; before: Queue Nachtrag 24 / N19 / N15 in SimpleBuilding).
Writes the module modules/simpleoctets (namespace simpleoctets):

  blockstates/<wood>_octet.json, blockstates/melon_octet.json   multipart: bit -> corner model (x/y rotation, uvlock)
  models/block/octet_corner.json, models/item/octet.json         the corner/item templates (were SimpleBuilding's chess ones)
  models/block/octet/<wood>.json         the corner template with the vanilla planks texture (no copy)
  models/block/octet/melon_corner.json   one melon octet: outer faces rind (melon_side / melon_top), cut faces flesh
  models/item/octet/<wood>_octet.json + items/<wood>_octet.json   the octet item
  textures/block/melon_flesh.png         own 16x16 pixel art: the cut melon (flesh with seeds; needs Pillow)

One octet cell + item for every block that has stairs and slabs (vanilla + SimpleBuilding + Simple End, without planks
(the woods above) and the chess checkers). The list lives in tools/textures/octet_materials.json (made by --scan from the
26.3 client jar, the extracted German lang file and SimpleBuilding's generated assets) and produces:

  models/block/octet/<base>.json, blockstates/<base>_octet.json, models/item/octet/<base>_octet.json, items/<base>_octet.json
  shared/java/com/simpleoctets/registry/OctetMaterials.java   the list the game registers the cells from
  lang en_us/de_de                                           names of all cells and items (generated block after melon_octet)
  data: recipe/<cell>_from_<base>_sawing (simpleblocks:sawing, only with Simple Blocks) + <cell>_from_<base>_stonecutting
        (fallback, only without Simple Blocks) + <base>_from_octets (+ recipe advancements under recipes/building_blocks),
        tags minecraft:mineable/axe|pickaxe; blocks of another mod (simplebuilding, simpleend) only while it is loaded
        (load conditions, optional tag entries)

--check compares instead of writing. --preview DIR renders octet examples (needs the vanilla textures in --vanilla DIR,
an extracted assets/ tree; nothing of it is checked in). Pillow is only needed for the melon texture and the preview.
"""
import io
import json
import os
import sys
import zipfile

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
NS = 'simpleoctets'
MODULE = os.path.join(REPO, 'modules', NS)
ASSETS = os.path.join(MODULE, 'shared', 'resources', 'assets', NS)
DATA = os.path.join(MODULE, 'shared', 'resources', 'data')
MATERIALS_JSON = os.path.join(REPO, 'tools', 'textures', 'octet_materials.json')
JAVA_LIST = os.path.join(MODULE, 'shared', 'java', 'com', NS, 'registry', 'OctetMaterials.java')
# SimpleBuilding's lang (scan: names of its palettes); the module's own lang gets the generated names
SB_LANG_DIR = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'simplebuilding', 'lang')
LANG_DIRS = [os.path.join(ASSETS, 'lang')]
MOD_GENERATED = os.path.join(REPO, 'src', 'main', 'generated', 'assets', 'simplebuilding')
CLIENT_JAR = os.environ.get('SB_VANILLA_DIR', os.path.expanduser('~/vanilla263')) + '/client.jar'
DE_LANG = os.path.expanduser('~/.gradle/caches') + '/neoformruntime/assets/objects/76/766975253e4de94ffc2e7e6d3b10b209da2f52c4'
# Order = OctetRegistry.WOODS
WOODS = ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry', 'pale_oak', 'poplar',
         'bamboo', 'crimson', 'warped', 'ashwood', 'duskwood']
# Simple End woods (registered only with Simple End, docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md): planks in its namespace.
END_WOODS = ('ashwood', 'duskwood')
# Octets of another mod's block: their item definition + a self-contained item model live in that mod's resources (still
# namespace simpleoctets). The 26.3 client loads every items/*.json and bakes its model even when the item is not
# registered, so without the partner its textures would be missing (client log faults); with it in the partner's jar they
# only exist together with the textures. Blockstates/corner models stay here (only loaded for registered blocks).
PARTNER_RES = {'simpleend': os.path.join(REPO, 'modules', 'simpleend', 'shared', 'resources'),
               'simpleblocks': os.path.join(REPO, 'modules', 'simpleblocks', 'shared', 'resources'),
               'simplebuilding': os.path.join(REPO, 'mc26_3', 'overlay', 'resources')}


def item_assets_root(ns):
    """assets/simpleoctets of the jar that carries the octet item of a base block in namespace ns."""
    return os.path.join(PARTNER_RES[ns], 'assets', NS) if ns in PARTNER_RES else ASSETS


def planks_tex(wood):
    return f"{'simpleend' if wood in END_WOODS else 'minecraft'}:block/{wood}_planks"

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
    if img is None:
        return None
    buf = io.BytesIO()
    img.save(buf, 'PNG')
    return buf.getvalue()


def js(data):
    return (json.dumps(data, indent=2) + '\n').encode('utf-8')


def flesh():
    try:
        from PIL import Image
    except ImportError:
        return None
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
    ('metal', 'Metal and mineral blocks'), ('log', 'Logs and stems'), ('wool', 'Wool'), ('concrete', 'Concrete'),
    ('terracotta', 'Terracotta'), ('mod', 'Blocks of SimpleBuilding and its sub-mods'), ('other', 'Other')]
DYES = ['white', 'light_gray', 'gray', 'black', 'brown', 'red', 'orange', 'yellow', 'lime', 'green', 'cyan', 'light_blue',
        'blue', 'purple', 'magenta', 'pink']
# ids of the chess octets (ChessColor); a material with the same name gets '_block' in its id and name
CHESS_IDS = {'quartz', 'purpur', 'lapis', 'blackstone', 'resin', 'nether_brick', 'red_nether_brick', 'nihilith', 'astralit',
             'ender_quartz', 'polished_astralit', 'polished_nihilith', 'polished_ender_quartz'}
COPPER_ORDER = ['copper_block', 'exposed_copper', 'weathered_copper', 'oxidized_copper', 'cut_copper', 'exposed_cut_copper', 'weathered_cut_copper', 'oxidized_cut_copper']

# Queue "Mehr 0,125er-Bloecke" (docs/ai/PLAN-OCTETS-MORE-2026-10-12.md): building colours without stairs. Vanilla only, full
# opaque cubes, no block entities or functions. Logs/stems are found by name in scan().
EXTRA_BASES = (['terracotta'] + [d + '_terracotta' for d in DYES]
               + ['copper_block', 'exposed_copper', 'weathered_copper', 'oxidized_copper', 'iron_block', 'gold_block',
                  'diamond_block', 'emerald_block', 'lapis_block', 'coal_block', 'netherite_block',
                  'calcite', 'smooth_basalt', 'end_stone', 'netherrack', 'obsidian', 'bone_block'])


def group_of(ns, base):
    if ns != 'minecraft':
        return 'mod'
    if 'copper' in base:
        return 'copper'
    if base.endswith('_log') or base.endswith(('crimson_stem', 'warped_stem')):
        return 'log'
    if base.endswith('terracotta'):
        return 'terracotta'
    if base in ('iron_block', 'gold_block', 'diamond_block', 'emerald_block', 'lapis_block', 'coal_block', 'netherite_block'):
        return 'metal'
    if base in ('calcite', 'smooth_basalt', 'end_stone', 'netherrack', 'obsidian'):
        return 'stone'
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
    if base in ('stone', 'smooth_stone', 'cobblestone', 'mossy_cobblestone') or any(k in base for k in ('granite', 'diorite', 'andesite')):
        return 'stone'
    return 'other'


def sort_key(m):
    b = m['base']
    gi = [g for g, _ in GROUPS].index(m['group'])
    if m['group'] == 'copper':
        plain = b.replace('waxed_', '')
        return (gi, b.startswith('waxed_'), COPPER_ORDER.index(plain) if plain in COPPER_ORDER else 9, b)
    if m['group'] in ('wool', 'concrete', 'terracotta'):
        return (gi, False, DYES.index(b.rsplit('_', 1)[0]) if b != 'terracotta' else -1, b)
    if m['group'] == 'log':
        return (gi, b.startswith('stripped_'), 0, b)
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
    en_mod = json.load(open(os.path.join(SB_LANG_DIR, 'en_us.json'), encoding='utf-8'))
    de_mod = json.load(open(os.path.join(SB_LANG_DIR, 'de_de.json'), encoding='utf-8'))
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
    logs = sorted(n for n in van if n.endswith('_log') or n.replace('stripped_', '') in ('crimson_stem', 'warped_stem'))
    for base in EXTRA_BASES + logs:
        if base not in van or any(m['base'] == base for m in out):
            print('SKIP (extra missing/duplicate): ' + base, file=sys.stderr)
            continue
        state = json.loads(z.read(f'assets/minecraft/blockstates/{base}.json'))
        ref = state['variants'].get('axis=y') or next(iter(state['variants'].values()))
        ref = (ref[0] if isinstance(ref, list) else ref)['model'].split(':')[-1]
        t = json.loads(z.read(f'assets/minecraft/models/{ref}.json')).get('textures', {})
        en, de = en_van.get(f'block.minecraft.{base}'), de_van.get(f'block.minecraft.{base}')
        if 'all' in t:
            tex = {'all': t['all']}
        elif {'end', 'side'} <= set(t):
            tex = {'up': t['end'], 'down': t['end'], 'side': t['side']}
        else:
            print('SKIP (extra textures): ' + base, file=sys.stderr)
            continue
        if not (en and de):
            print('SKIP (extra name): ' + base, file=sys.stderr)
            continue
        out.append({'ns': 'minecraft', 'base': base, 'key': base, 'group': group_of('minecraft', base), 'tex': tex, 'en': en, 'de': de})
    out.extend(LIB_EXTRAS)
    out.sort(key=sort_key)
    last_log = max(i for i, m in enumerate(out) if m['group'] == 'log')
    out[last_log + 1:last_log + 1] = END_LOGS
    last_mod = max(i for i, m in enumerate(out) if m['group'] == 'mod')
    out[last_mod + 1:last_mod + 1] = BLOCKS_EXTRAS
    with open(MATERIALS_JSON, 'w', encoding='utf-8') as f:
        f.write(json.dumps(out, indent=1, ensure_ascii=False) + '\n')
    print(f'scanned {len(out)} materials')


# Simple End logs (docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md): not in the client jar, kept after the Vanilla logs; the game
# registers their cells only while Simple End is loaded (ModBlocks#materialOctets).
END_LOGS = [{'ns': 'simpleend', 'base': f'{s}{w}_log', 'key': f'{s}{w}_log', 'group': 'log',
             'tex': {'up': f'simpleend:block/{s}{w}_log_top', 'down': f'simpleend:block/{s}{w}_log_top', 'side': f'simpleend:block/{s}{w}_log'},
             'en': ('Stripped ' if s else '') + en + ' Log', 'de': ('Entrindeter ' if s else '') + de + 'stamm'}
            for s in ('', 'stripped_') for w, en, de in (('ashwood', 'Ashwood', 'Asch'), ('duskwood', 'Duskwood', 'Dämmer'))]


# Simple Blocks (N69, split stage 6): its full blocks with stairs and slabs (not in the client jar); the game registers
# their cells only while Simple Blocks is loaded (OctetRegistry#materialCell).
BLOCKS_EXTRAS = [{'ns': 'simpleblocks', 'base': b, 'key': b, 'group': 'mod', 'tex': {'all': f'simpleblocks:block/{b}'}, 'en': en, 'de': de}
                 for b, en, de in (('blue_nether_bricks', 'Blue Nether Bricks', 'Blaue Netherziegel'),
                                   ('hushed_deepslate_tiles', 'Hushed Deepslate Tiles', 'Gedämpfte Tiefenschieferfliesen'))]


# SimpleLib's polished end stone (it has stairs, slab and wall there); SimpleLib is a required dependency of the module,
# the block is always registered.
LIB_EXTRAS = [{'ns': 'simplelib', 'base': 'polished_end_stone', 'key': 'polished_end_stone', 'group': 'stone',
               'tex': {'all': 'simplelib:block/polished_end_stone'}, 'en': 'Polished End Stone', 'de': 'Polierter Endstein'}]


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
    return f"""package com.simpleoctets.registry;

import java.util.List;

/**
 * GENERATED by tools/textures/octets_2026_10_09.py from tools/textures/octet_materials.json - do not edit by hand.
 * Every block with stairs and slabs (vanilla, SimpleBuilding and Simple End, without planks and chess checkers) gets an
 * octet cell {{@code <key>_octet}}; {{@link OctetRegistry#registerBlocks}} registers them in creative-tab order, the
 * ones of another mod's block only while that mod is loaded.
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


def generated_names(mats, lang):
    out = {}
    for m in mats:
        name = m['en' if lang == 'en_us' else 'de']
        if m['key'] != m['base']:
            name += ' Block' if lang == 'en_us' else '-Block'
        suffix = ('{} Octet', '{} Octet Cell') if lang == 'en_us' else ('{}-Achtelblock', '{}-Achtelzelle')
        out[f'item.{NS}.{m["key"]}_octet'] = suffix[0].format(name)
        out[f'block.{NS}.{m["key"]}_octet'] = suffix[1].format(name)
    return out


def merged_lang(lang_dir, lang, mats):
    """The module's lang file: hand-written keys keep their place, the generated names follow block.<ns>.melon_octet."""
    path = os.path.join(lang_dir, lang + '.json')
    with open(path, encoding='utf-8') as f:
        data = json.load(f)
    gen = generated_names(mats, lang)
    out = {}
    for k, v in data.items():
        if k in gen:
            continue
        out[k] = v
        if k == f'block.{NS}.melon_octet':
            out.update(gen)
    if not all(k in out for k in gen):
        out.update(gen)
    return path, (json.dumps(out, indent=2, ensure_ascii=False) + '\n').encode('utf-8')


def corner_template():
    face = lambda **kw: dict({'texture': '#all'}, **kw)
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#all'},
            'elements': [{'from': [0, 0, 0], 'to': [8, 8, 8], 'faces': {
                'north': face(cullface='north'), 'south': face(), 'east': face(), 'west': face(cullface='west'),
                'up': face(), 'down': face(cullface='down')}}]}


def item_template():
    face = {'texture': '#all'}
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#all'},
            'elements': [{'from': [4, 4, 4], 'to': [12, 12, 12], 'faces': {
                'north': face, 'south': face, 'east': face, 'west': face, 'up': face, 'down': face}}],
            'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]}}}


def cells(mats):
    """(cell path, base namespace, base path, tool) of every octet cell with a saw/stonecutter recipe, in tab order."""
    out = []
    for wood in WOODS:
        ns = 'simpleend' if wood in END_WOODS else 'minecraft'
        out.append((f'{wood}_octet', ns, f'{wood}_planks', 'axe'))
    for m in mats:
        b = m['base']
        if m['ns'] == 'simpleend' or b == 'bamboo_mosaic' or b.endswith(('_log', '_stem')):
            tool = 'axe'
        elif b.endswith('_wool'):
            tool = None
        else:
            tool = 'pickaxe'
        out.append((f'{m["key"]}_octet', m['ns'], b, tool))
    return out


# The Shaping Saw of Simple Blocks (docs/ai/PRINZIPIEN.md A4b): octets are cut there only while Simple Blocks is loaded;
# without it a minecraft:stonecutting fallback keeps Simple Octets playable alone, and the Vanilla stonecutter stays Vanilla.
SAW_MOD = 'simpleblocks'
SAW_TYPE = 'simpleblocks:sawing'


def conditions(ns, saw=None):
    """Load conditions of a recipe/advancement: another mod's block needs that mod (none for Vanilla and the required
    simplelib); saw=True adds "simpleblocks loaded", saw=False "simpleblocks not loaded" (Fabric, NeoForge, Forge)."""
    fabric, neo, forge = [], [], []
    mods = [] if ns in ('minecraft', 'simplelib') else [ns]
    if saw and SAW_MOD not in mods:
        mods.append(SAW_MOD)
    for mod in mods:
        fabric.append({'condition': 'fabric:all_mods_loaded', 'values': [mod]})
        neo.append({'type': 'neoforge:mod_loaded', 'modid': mod})
        forge.append({'type': 'forge:mod_loaded', 'modid': mod})
    if saw is False:
        fabric.append({'condition': 'fabric:not', 'value': {'condition': 'fabric:all_mods_loaded', 'values': [SAW_MOD]}})
        neo.append({'type': 'neoforge:not', 'value': {'type': 'neoforge:mod_loaded', 'modid': SAW_MOD}})
        forge.append({'type': 'forge:not', 'value': {'type': 'forge:mod_loaded', 'modid': SAW_MOD}})
    if not fabric:
        return {}
    # Forge reads one condition object ("forge:condition"); several are joined with forge:and.
    return {'fabric:load_conditions': fabric, 'neoforge:conditions': neo,
            'forge:condition': forge[0] if len(forge) == 1 else {'type': 'forge:and', 'values': forge}}


def unlock(recipe, item):
    crit = 'has_' + item.split(':')[1]
    return {'parent': 'minecraft:recipes/root',
            'criteria': {crit: {'conditions': {'items': [{'items': item}]}, 'trigger': 'minecraft:inventory_changed'},
                         'has_the_recipe': {'conditions': {'recipes': f'{NS}:{recipe}'}, 'trigger': 'minecraft:recipe_unlocked'}},
            'requirements': [['has_the_recipe', crit]], 'rewards': {'recipes': [f'{NS}:{recipe}']}}


def data_files(mats):
    files = {}
    d = lambda rel, data: files.__setitem__(os.path.join(DATA, *rel.split('/')), js(data))
    tags = {'axe': [], 'pickaxe': []}
    for cell, ns, base, tool in cells(mats):
        cond = conditions(ns)
        full, octet = f'{ns}:{base}', f'{NS}:{cell}'
        saw = f'{cell}_from_{base}_sawing'
        cut = f'{cell}_from_{base}_stonecutting'
        back = f'{base}_from_octets'
        # The saw has no recipe book: the sawing recipe needs no unlock advancement.
        d(f'{NS}/recipe/{saw}.json', dict(conditions(ns, True), type=SAW_TYPE, ingredient=full, result={'count': 8, 'id': octet}))
        if ns != SAW_MOD:
            fallback = conditions(ns, False)
            d(f'{NS}/recipe/{cut}.json', dict(fallback, type='minecraft:stonecutting', ingredient=full, result={'count': 8, 'id': octet}))
            d(f'{NS}/advancement/recipes/building_blocks/{cut}.json', dict(fallback, **unlock(cut, full)))
        d(f'{NS}/recipe/{back}.json', dict(cond, type='minecraft:crafting_shapeless', category='building',
                                             ingredients=[octet] * 8, result={'id': full}))
        d(f'{NS}/advancement/recipes/building_blocks/{back}.json', dict(cond, **unlock(back, octet)))
        if tool:
            tags[tool].append(octet if ns == 'minecraft' else {'id': octet, 'required': False})
    tags['axe'].append(f'{NS}:melon_octet')
    # Recipes (and their advancements) the generator no longer writes, e.g. a stonecutter fallback that cannot apply.
    for sub in ('recipe', os.path.join('advancement', 'recipes', 'building_blocks')):
        folder = os.path.join(DATA, NS, sub)
        if os.path.isdir(folder):
            STALE.extend(os.path.join(folder, n) for n in sorted(os.listdir(folder))
                         if n.endswith('.json') and n != 'guide_book.json' and os.path.join(folder, n) not in files)
    for tool, values in tags.items():
        d(f'minecraft/tags/block/mineable/{tool}.json', {'values': values})
    return files


# SimpleBuilding's chess octets (checker_octet cells, docs/ai/PLAN-SCHACH-2026-10-06.md): 8 from a checker (quartz: the
# quartz block) - in the Shaping Saw with Simple Blocks, the stonecutter only without it. SimpleBuilding's datagen has no
# load conditions, so these recipes live in its 26.3 overlay; the pieces cut from an octet stay stonecutter recipes.
SB_OVERLAY_DATA = os.path.join(REPO, 'mc26_3', 'overlay', 'resources', 'data', 'simplebuilding')
CHESS_COLORS = os.path.join(REPO, 'common', 'src', 'shared', 'java', 'com', 'simplebuilding', 'chess', 'ChessColor.java')


def chess_octet_files():
    import re
    files = {}
    text = open(CHESS_COLORS, encoding='utf-8').read()
    colors = re.findall(r'^\s+[A-Z_]+\("([a-z_]+)", \(\) -> (?:ModBlocks\.([A-Z_]+)|null), \d+\)', text, re.M)
    assert len(colors) == 13, colors
    for color, checker in colors:
        src = 'simplebuilding:' + checker.lower() if checker else 'minecraft:quartz_block'
        octet = f'simplebuilding:{color}_octet'
        stem = f'{color}_octet_from_{src.split(":")[1]}'
        result = {'count': 8, 'id': octet}
        files[os.path.join(SB_OVERLAY_DATA, 'recipe', stem + '_sawing.json')] = js(
            dict(conditions('minecraft', True), type=SAW_TYPE, ingredient=src, result=result))
        cut = stem + '_stonecutting'
        files[os.path.join(SB_OVERLAY_DATA, 'recipe', cut + '.json')] = js(
            dict(conditions('minecraft', False), type='minecraft:stonecutting', ingredient=src, result=result))
        adv = unlock(cut, src)
        adv['criteria']['has_the_recipe']['conditions']['recipes'] = f'simplebuilding:{cut}'
        adv['rewards']['recipes'] = [f'simplebuilding:{cut}']
        files[os.path.join(SB_OVERLAY_DATA, 'advancement', 'recipes', 'building_blocks', cut + '.json')] = js(
            dict(conditions('minecraft', False), **adv))
    return files


def sided_item_template():
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#side'},
            'display': {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.9, 0.9, 0.9]}},
            'elements': [{'from': [4, 4, 4], 'to': [12, 12, 12], 'faces': {
                'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'},
                'west': {'texture': '#side'}, 'up': {'texture': '#up'}, 'down': {'texture': '#down'}}}]}


def item(files, ns, cell, tex, template):
    """Item definition + item model of one octet (template '' = item/octet, '/_sided' = item/octet/_sided). A partner's
    octet gets the template inlined (no parent from this module: the partner may run without it)."""
    root = item_assets_root(ns)
    if root == ASSETS:
        model = {'parent': f'{NS}:item/octet{template}', 'textures': tex}
    else:
        model = sided_item_template() if template else item_template()
        model['textures'] = dict(model['textures'], **tex)
    files[os.path.join(root, 'models', 'item', 'octet', cell + '.json')] = js(model)
    files[os.path.join(root, 'items', cell + '.json')] = js(
        {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/octet/{cell}'}})
    if root != ASSETS:
        STALE.extend(os.path.join(ASSETS, *rel) for rel in (('models', 'item', 'octet', cell + '.json'), ('items', cell + '.json')))


STALE = []


def outputs():
    files = {}
    a = lambda rel, data: files.__setitem__(os.path.join(ASSETS, *rel.split('/')), data)
    a('models/block/octet_corner.json', js(corner_template()))
    a('models/item/octet.json', js(item_template()))
    for wood in WOODS:
        a(f'models/block/octet/{wood}.json', js({
            'parent': f'{NS}:block/octet_corner', 'textures': {'all': planks_tex(wood)}}))
        a(f'blockstates/{wood}_octet.json', js(multipart(f'{NS}:block/octet/{wood}')))
        item(files, 'simpleend' if wood in END_WOODS else 'minecraft', f'{wood}_octet', {'all': planks_tex(wood)}, '')
    # Melon: the corner's outer faces (north, west, down) carry the rind, the three cut faces the flesh.
    outer = {'texture': '#side'}
    a('models/block/octet/melon_corner.json', js({
        'parent': 'minecraft:block/block',
        'textures': {'particle': 'minecraft:block/melon_side', 'side': 'minecraft:block/melon_side',
                     'end': 'minecraft:block/melon_top', 'cut': f'{NS}:block/melon_flesh'},
        'elements': [{'from': [0, 0, 0], 'to': [8, 8, 8], 'faces': {
            'north': dict(outer, cullface='north'), 'west': dict(outer, cullface='west'),
            'down': {'texture': '#end', 'cullface': 'down'},
            'south': {'texture': '#cut'}, 'east': {'texture': '#cut'}, 'up': {'texture': '#cut'}}}]}))
    a('blockstates/melon_octet.json', js(multipart(f'{NS}:block/octet/melon_corner')))
    # The mixed cell is drawn by its block entity renderer (RenderShape.INVISIBLE): only a particle texture.
    a('blockstates/mixed_octet.json', js({'variants': {'': {'model': f'{NS}:block/mixed_octet'}}}))
    a('models/block/mixed_octet.json', js({'textures': {'particle': 'minecraft:block/stone'}}))
    texture = png_bytes(flesh())
    if texture is not None:
        a('textures/block/melon_flesh.png', texture)
    else:
        print('Pillow missing: textures/block/melon_flesh.png left as it is', file=sys.stderr)
    mats = materials()
    a('models/block/octet/_corner_sided.json', js(sided_corner()))
    a('models/item/octet/_sided.json', js(sided_item_template()))
    for m in mats:
        b = m['key']
        if 'all' in m['tex']:
            blk = {'parent': f'{NS}:block/octet_corner', 'textures': {'all': m['tex']['all']}}
        else:
            blk = {'parent': f'{NS}:block/octet/_corner_sided', 'textures': m['tex']}
        a(f'models/block/octet/{b}.json', js(blk))
        a(f'blockstates/{b}_octet.json', js(multipart(f'{NS}:block/octet/{b}')))
        item(files, m['ns'], f'{b}_octet', m['tex'], '' if 'all' in m['tex'] else '/_sided')
    files[JAVA_LIST] = java_list(mats).encode('utf-8')
    files.update(data_files(mats))
    files.update(chess_octet_files())
    for lang_dir in LANG_DIRS:
        for lang in ('en_us', 'de_de'):
            path, data = merged_lang(lang_dir, lang, mats)
            files[path] = data
    return files


# --- preview: isometric voxels, one texture per voxel face kind ---
def render(cells, tex, size=6):
    """cells: {(x, y, z): key} on a 0.5 grid (octets); tex[key] = (top, side) 16x16 images. View from south-east."""
    from PIL import Image, ImageDraw
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
    from PIL import Image
    return tex.resize((size, size), Image.NEAREST)


def preview(files, vanilla, out_dir):
    from PIL import Image, ImageDraw, ImageFont
    vt = lambda n: Image.open(os.path.join(vanilla, 'assets', 'minecraft', 'textures', 'block', n + '.png')).convert('RGBA')
    fl = Image.open(io.BytesIO(files[os.path.join(ASSETS, 'textures', 'block', 'melon_flesh.png')])).convert('RGBA')
    tex = {w: (vt(w + '_planks'), vt(w + '_planks')) for w in WOODS if w not in END_WOODS}
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
    for i, w in enumerate(w for w in WOODS if w not in END_WOODS):
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
    bad = [p for p in STALE if os.path.exists(p)] if check else []
    if not check:
        for p in STALE:
            if os.path.exists(p):
                os.remove(p)
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
