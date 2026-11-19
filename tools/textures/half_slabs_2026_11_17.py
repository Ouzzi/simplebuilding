"""Usage: python tools/textures/half_slabs_2026_11_17.py [--scan] [--check] [--out DIR]

N86 half slabs - BUILT (owner 18.11., docs/ai/PLAN-N86-BAU-2026-11-19.md): sub-mod Simple Slabs (N86-4), 2-px layers (N86-1),
any of the eight layers of a cell freely combined (N86-2 B) plus the side half slab / side slab, all 170 blocks with a slab incl.
dirt, grass, sand, red sand and gravel (N86-5), saw and craft-back recipes (N86-6, volume-true for 2 px).

Per material: simpleslabs:<slab path without _slab>_half_slab (item; block state l0..l7 + waterlogged) and
simpleslabs:<...>_side_half_slab (no item; facing + thickness 1..8 + waterlogged). The list lives in
tools/textures/half_slab_materials.json (made by --scan from the 26.3 client jar, the module assets and the German lang cache;
German names already in the list are kept when the cache is missing) and produces:

  blockstates/<id>.json, <side>.json             multipart: layer i when l<i>; side form = the same layer models turned
  models/block/half_slab/_<kind>_<i>.json         templates (kind = slab | checker | grass | dirt), i = layer 0..7
  models/block/half_slab/<id>_<i>[_dirt].json     per material: template + the slab's own texture references
  items/<id>.json + models/item/half_slab/<id>.json   (another mod's material: in that mod's jar, self-contained)
  shared/java/com/simpleslabs/registry/HalfSlabMaterials.java   GENERATED list for the Java side
  lang en_us/de_de                                names (generated block after the module's own keys)
  data: loot_table/blocks/<id>.json (one pool per layer), <side>.json (count = thickness), recipe/half_slab/*.json (+ advancements),
        tags simpleslabs:half_slabs, simpleslabs:wooden_half_slabs, minecraft:mineable/<tool>, occludes_vibration_signals,
        dampens_vibrations.

The shared helpers (js, groups, saw recipe type, partner jars, client jar, German lang cache) come from the octet generator
(tools/textures/octets_2026_10_09.py) - one library for both shape mods. No Mojang pixels: models reference textures by resource
location only; nothing is copied. --check compares instead of writing; --out DIR writes (or checks) below DIR.
"""
import json
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import octets_2026_10_09 as octets  # noqa: E402  shared helpers: js, group_of, GROUPS, SAW_*, DE_LANG, CLIENT_JAR, PARTNER_RES

REPO = octets.REPO
NS = 'simpleslabs'
MODULE = os.path.join(REPO, 'modules', NS)
ASSETS = os.path.join(MODULE, 'shared', 'resources', 'assets', NS)
DATA = os.path.join(MODULE, 'shared', 'resources', 'data')
MATERIALS_JSON = os.path.join(REPO, 'tools', 'textures', 'half_slab_materials.json')
JAVA_LIST = os.path.join(MODULE, 'shared', 'java', 'com', NS, 'registry', 'HalfSlabMaterials.java')
LANG_DIR = os.path.join(ASSETS, 'lang')

# ---------------------------------------------------------------- owner answers (N86, 18.11.)
# N86-1: one half slab = one layer of 2 px; a cell holds LAYERS layers.
LAYER_PX = 2
LAYERS = 16 // LAYER_PX
# N86-3: names. EN: "<slab name without ' Slab'>" + NAME_EN; DE: "...stufe" -> "..." + NAME_DE.
NAME_EN = ' Half Slab'
NAME_DE = 'halbstufe'
# N86-5: all slabs incl. the natural ones; sand, red sand and gravel fall, grass has its own model (tint, grass edge on top).
FALLING = {'simpleblocks:sand_slab', 'simpleblocks:red_sand_slab', 'simpleblocks:gravel_slab'}
SHOVEL = FALLING | {'simpleblocks:dirt_slab', 'simpleblocks:grass_slab'}
# Always out: petrified_oak_slab has no full block of its own (same exception as the octets, OctetTests.NO_OCTET).
EXCLUDE = {'minecraft:petrified_oak_slab'}
# N86-6 (volume-true for 2 px): saw 1 slab -> SLAB_CUT, 1 full block -> BLOCK_CUT; crafting HALVES_PER_SLAB -> 1 slab (shapeless).
# No "3 slabs -> 6 half slabs" (collides with the 16 vanilla cushion recipes, ###).
SLAB_CUT = 8 // LAYER_PX
BLOCK_CUT = 16 // LAYER_PX
HALVES_PER_SLAB = 8 // LAYER_PX
STONECUTTER_FALLBACK = True   # A4b: only without Simple Blocks (load condition), like the octets
# N86-11: never in #minecraft:slabs (foreign code assumes SlabBlock.TYPE there).

# Side form: facing (the wall it leans on) -> y rotation of the layer models turned with x=90 (layer 0 then lies at the south).
SIDE_Y = {'south': 0, 'west': 90, 'north': 180, 'east': 270}


def side_id(mid):
    return mid[:-len('half_slab')] + 'side_half_slab'


# ---------------------------------------------------------------- scan
# Asset roots of the 26.3 line per namespace (models, lang): modules first, then SimpleBuilding's generated/overlay trees.
def asset_roots():
    roots = []
    for top in ('modules',):
        for mod in sorted(os.listdir(os.path.join(REPO, top))):
            for sub in ('shared/resources', 'generated/resources'):
                p = os.path.join(REPO, top, mod, sub, 'assets')
                if os.path.isdir(p):
                    roots.append(p)
    for rel in ('mc26_3/generated/assets', 'mc26_3/overlay/resources/assets', 'src/main/generated/assets', 'src/main/resources/assets'):
        p = os.path.join(REPO, rel)
        if os.path.isdir(p):
            roots.append(p)
    return roots


def mod_json(roots, ns, rel):
    for r in roots:
        p = os.path.join(r, ns, rel)
        if os.path.isfile(p):
            with open(p, encoding='utf-8') as f:
                return json.load(f)
    return None


def mod_lang(roots, ns, locale):
    out = {}
    for r in reversed(roots):  # first root wins
        p = os.path.join(r, ns, 'lang', locale + '.json')
        if os.path.isfile(p):
            with open(p, encoding='utf-8') as f:
                out.update(json.load(f))
    return out


# Slabs whose stem names a non-full block (bamboo = the plant).
BASE_OVERRIDE = {'minecraft:bamboo_slab': ['bamboo_planks']}


def german(slab_de, sid):
    if slab_de.endswith('stufe'):
        return slab_de[:-len('stufe')] + NAME_DE
    if slab_de.endswith('Stufe'):
        return slab_de[:-len('Stufe')] + NAME_DE[:1].upper() + NAME_DE[1:]
    raise SystemExit(f'German slab name of {sid} does not end in "stufe": {slab_de!r}')


def english(slab_en, sid):
    if slab_en.endswith(' Slab'):
        return slab_en[:-len(' Slab')] + NAME_EN
    raise SystemExit(f'English slab name of {sid} does not end in " Slab": {slab_en!r}')


def scan():
    z = zipfile.ZipFile(octets.CLIENT_JAR)
    names = set(z.namelist())
    van = {n[len('assets/minecraft/blockstates/'):-5] for n in names
           if n.startswith('assets/minecraft/blockstates/') and n.endswith('.json')}
    en_van = json.loads(z.read('assets/minecraft/lang/en_us.json'))
    de_van = {}
    if os.path.isfile(octets.DE_LANG):
        with open(octets.DE_LANG, encoding='utf-8') as f:
            de_van = json.load(f)
    roots = asset_roots()
    slabs = []  # (ns, path, blockstate)
    for p in sorted(van):
        if p.endswith('_slab'):
            slabs.append(('minecraft', p, json.loads(z.read(f'assets/minecraft/blockstates/{p}.json'))))
    seen = set()
    for r in roots:
        for ns in sorted(os.listdir(r)):
            bs = os.path.join(r, ns, 'blockstates')
            if ns == 'minecraft' or not os.path.isdir(bs):
                continue
            for n in sorted(os.listdir(bs)):
                if n.endswith('_slab.json') and not n.endswith('_half_slab.json') and (ns, n) not in seen:
                    seen.add((ns, n))
                    with open(os.path.join(bs, n), encoding='utf-8') as f:
                        slabs.append((ns, n[:-5], json.load(f)))
    mod_blocks = {}
    for r in roots:
        for ns in os.listdir(r):
            bs = os.path.join(r, ns, 'blockstates')
            if os.path.isdir(bs):
                mod_blocks.setdefault(ns, set()).update(n[:-5] for n in os.listdir(bs) if n.endswith('.json'))
    langs = {}
    out, skipped = [], []
    known_de = {m['slab']: m['de'] for m in materials()} if os.path.isfile(MATERIALS_JSON) else {}
    for ns, path, state in slabs:
        sid = f'{ns}:{path}'
        if sid in EXCLUDE:
            skipped.append(sid)
            continue
        stem = path[:-len('_slab')]
        variants = state['variants']
        bottom = (variants['type=bottom'][0] if isinstance(variants['type=bottom'], list) else variants['type=bottom'])['model']
        top = (variants['type=top'][0] if isinstance(variants['type=top'], list) else variants['type=top'])['model']
        mns, mpath = bottom.split(':') if ':' in bottom else ('minecraft', bottom)
        if mns == 'minecraft':
            model = json.loads(z.read(f'assets/minecraft/models/{mpath}.json'))
        else:
            model = mod_json(roots, mns, f'models/{mpath}.json')
        if model is None:
            raise SystemExit(f'{sid}: model {bottom} not found')
        parent, tex = model.get('parent', ''), model.get('textures', {})
        if parent in ('minecraft:block/slab', 'block/slab') and {'bottom', 'top', 'side'} <= set(tex):
            kind, textures, cells = 'slab', {k: tex[k] for k in ('bottom', 'top', 'side')}, None
        elif sid == 'simpleblocks:grass_slab' and {'bottom', 'top', 'side', 'overlay'} <= set(tex):
            kind, textures, cells = 'grass', {k: tex[k] for k in ('bottom', 'top', 'side', 'overlay')}, None
        elif parent.startswith('simplechess:block/checker_cells/') and {'side', 'side_mirror'} <= set(tex):
            kind, textures = 'checker', {'side': tex['side'], 'side_mirror': tex['side_mirror']}
            tmns, tmpath = top.split(':')
            top_parent = mod_json(roots, tmns, f'models/{tmpath}.json')['parent']
            cells = [parent, top_parent]   # bottom/top cell models the checker template is cut from
        else:
            raise SystemExit(f'{sid}: unusual slab model {bottom} (parent {parent!r}) - needs its own template')
        # base block: the double model's block, else the octet naming rules (stem, stems, _block, _planks)
        have = van if ns == 'minecraft' else mod_blocks.get(ns, set())
        double = variants.get('type=double')
        double = (double[0] if isinstance(double, list) else double)['model'] if double else ''
        dpath = double.split(':')[-1].removeprefix('block/')
        base = None
        # the stem first (waxed copper: the double model is the unwaxed block), the double model's block second
        for cand in BASE_OVERRIDE.get(sid, [stem, stem + 's', stem + '_block', stem + '_planks', dpath, stem + '_bricks']):
            if cand in have:
                base = f'{ns}:{cand}'
                break
            if ns == 'simpleblocks' and cand in van:   # Simple Blocks cuts slabs of vanilla blocks (calcite ...)
                base = f'minecraft:{cand}'
                break
        if ns == 'minecraft':
            en, de = en_van.get(f'block.minecraft.{path}'), de_van.get(f'block.minecraft.{path}')
            if not de and sid in known_de:   # no German cache here (sb-agent): keep the name the list already has
                out_de = known_de[sid]
                de = out_de[:-len(NAME_DE)] + 'stufe' if out_de.endswith(NAME_DE) else None
        else:
            if ns not in langs:
                langs[ns] = (mod_lang(roots, ns, 'en_us'), mod_lang(roots, ns, 'de_de'))
            en, de = langs[ns][0].get(f'block.{ns}.{path}'), langs[ns][1].get(f'block.{ns}.{path}')
        if not (en and de):
            raise SystemExit(f'{sid}: no EN/DE name')
        bpath = base.split(':')[1] if base else stem
        wood = bpath.endswith('_planks') or bpath == 'bamboo_mosaic'
        group = 'wood' if wood else ('mod' if ns != 'minecraft' else octets.group_of('minecraft', bpath))
        tool = 'axe' if wood else 'shovel' if sid in SHOVEL else (None if bpath.endswith('_wool') else 'pickaxe')
        mods = [] if ns in ('minecraft', 'simplelib') else [ns]
        if ns == 'simplechess' and any(w in stem for w in ('astralit', 'nihilith', 'ender_quartz')):
            mods.append('simplebuilding')   # ChessColor palette colours exist only with SimpleBuilding (PALETTE_SOURCE)
        out.append({'id': f'{stem}_half_slab', 'slab': sid, 'base': base, 'ns': ns, 'mods': mods, 'group': group,
                    'kind': kind, 'tex': textures, **({'cells': cells} if cells else {}), 'tool': tool, 'falls': sid in FALLING,
                    'en': english(en, sid), 'de': german(de, sid)})
    groups = ['wood'] + [g for g, _ in octets.GROUPS]
    out.sort(key=lambda m: (groups.index(m['group']), m['id']))
    with open(MATERIALS_JSON, 'w', encoding='utf-8') as f:
        f.write(json.dumps(out, indent=1, ensure_ascii=False) + '\n')
    print(f'scanned {len(out)} half slabs, skipped {len(skipped)}: {", ".join(skipped)}')


def materials():
    with open(MATERIALS_JSON, encoding='utf-8') as f:
        return json.load(f)


# ---------------------------------------------------------------- models
def model_name(mid, layer, dirt=False):
    return f'{NS}:block/half_slab/{mid}_{layer}' + ('_dirt' if dirt else '')


def slab_template(layer):
    """Like minecraft:block/slab, one 2-px layer (auto UV = the matching strip of the side texture, so a full stack looks like the block)."""
    y0, y1 = layer * LAYER_PX, (layer + 1) * LAYER_PX
    faces = {d: {'texture': '#side', 'cullface': d} for d in ('north', 'south', 'east', 'west')}
    faces['down'] = {'texture': '#bottom', **({'cullface': 'down'} if y0 == 0 else {})}
    faces['up'] = {'texture': '#top', **({'cullface': 'up'} if y1 == 16 else {})}
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#side'},
            'elements': [{'from': [0, y0, 0], 'to': [16, y1, 16], 'faces': faces}]}


def grass_template(layer):
    """The open top layer of a grass stack: grass top (biome tint), the top strip of the grass side + its tinted overlay, dirt below."""
    y0, y1 = layer * LAYER_PX, (layer + 1) * LAYER_PX
    strip = [0, 0, 16, LAYER_PX]
    base = {d: {'uv': strip, 'texture': '#side', 'cullface': d} for d in ('north', 'south', 'east', 'west')}
    base['down'] = {'texture': '#bottom', **({'cullface': 'down'} if y0 == 0 else {})}
    base['up'] = {'texture': '#top', 'tintindex': 0, **({'cullface': 'up'} if y1 == 16 else {})}
    over = {d: {'uv': strip, 'texture': '#overlay', 'tintindex': 0, 'cullface': d} for d in ('north', 'south', 'east', 'west')}
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#bottom'},
            'elements': [{'from': [0, y0, 0], 'to': [16, y1, 16], 'faces': base},
                         {'from': [0, y0, 0], 'to': [16, y1, 16], 'faces': over}]}


def dirt_template(layer):
    """A covered layer of a grass stack: dirt on every face."""
    t = slab_template(layer)
    t['textures'] = {'particle': '#bottom'}
    for f in t['elements'][0]['faces'].values():
        f['texture'] = '#bottom'
    return t


def checker_template(bottom_cells, top_cells, layer):
    """The chess checker slab's cells (simplechess:block/checker_cells/<n>, 4 cells 8x8x8) cut to one 2-px layer: layers 0-3 from
    the bottom slab's cells, 4-7 from the top slab's; side UVs follow the cut, up/down keep the cell's square."""
    y0, y1 = layer * LAYER_PX, (layer + 1) * LAYER_PX
    out = json.loads(json.dumps(bottom_cells if layer < LAYERS // 2 else top_cells))
    for e in out['elements']:
        cy0, cy1 = e['from'][1], e['to'][1]
        e['from'][1], e['to'][1] = y0, y1
        faces = {}
        for d, f in e['faces'].items():
            f = dict(f)
            if d in ('north', 'south', 'east', 'west') and 'uv' in f:
                u0, v0, u1, v1 = f['uv']
                k = (v1 - v0) / (cy1 - cy0)
                f['uv'] = [u0, v0 + (cy1 - y1) * k, u1, v0 + (cy1 - y0) * k]
            if d == 'up':
                f.pop('cullface', None)
                if y1 == 16:
                    f['cullface'] = 'up'
            if d == 'down':
                f.pop('cullface', None)
                if y0 == 0:
                    f['cullface'] = 'down'
            faces[d] = f
        # a layer needs both cut faces; a source cell without one (inside its block) takes the other's square
        for d, other in (('up', 'down'), ('down', 'up')):
            if d not in faces and other in faces:
                faces[d] = {k: v for k, v in faces[other].items() if k != 'cullface'}
                if (d == 'up' and y1 == 16) or (d == 'down' and y0 == 0):
                    faces[d]['cullface'] = d
        e['faces'] = faces
    return out


def checker_cells(roots, ref):
    ns, path = ref.split(':')
    m = mod_json(roots, ns, f'models/{path}.json')
    if m is None:
        raise SystemExit('checker cells model missing: ' + ref)
    return m


# Simple Chess carries its half slab items like the other partners (octets have no chess partner entry: chess octets are its own).
EXTRA_PARTNER_RES = {'simplechess': os.path.join(REPO, 'modules', 'simplechess', 'shared', 'resources')}


def partner(ns):
    return ns in octets.PARTNER_RES or ns in EXTRA_PARTNER_RES


def item_root(ns):
    res = dict(octets.PARTNER_RES, **EXTRA_PARTNER_RES)
    return os.path.join(res[ns], 'assets', NS) if ns in res else ASSETS


def item_model(m, templates):
    """Item model = the bottom layer (layer 0, the grass layer for grass). A partner's jar inlines it (no parent from this module:
    the partner may run without Simple Slabs)."""
    if partner(m['ns']):
        tpl = json.loads(json.dumps(templates[m['kind']][0]))
        tpl['textures'] = dict(tpl['textures'], **m['tex'])
        return tpl
    return {'parent': model_name(m['id'], 0)}


def item_definition(m):
    model = {'type': 'minecraft:model', 'model': f'{NS}:item/half_slab/{m["id"]}'}
    if m['kind'] == 'grass':   # like Simple Blocks' grass slab item
        model['tints'] = [{'type': 'minecraft:grass', 'downfall': 1.0, 'temperature': 0.5}]
    return {'model': model}


def blockstate(m):
    parts = []
    for i in range(LAYERS):
        if m['kind'] == 'grass':
            if i < LAYERS - 1:
                parts.append({'when': {f'l{i}': 'true', f'l{i + 1}': 'false'}, 'apply': {'model': model_name(m['id'], i)}})
                parts.append({'when': {f'l{i}': 'true', f'l{i + 1}': 'true'}, 'apply': {'model': model_name(m['id'], i, True)}})
            else:
                parts.append({'when': {f'l{i}': 'true'}, 'apply': {'model': model_name(m['id'], i)}})
        else:
            parts.append({'when': {f'l{i}': 'true'}, 'apply': {'model': model_name(m['id'], i)}})
    return {'multipart': parts}


def side_blockstate(m):
    """Side form: layer k exists while thickness > k; the layer models turned with x=90 (lying against the wall of `facing`)."""
    parts = []
    for facing, y in SIDE_Y.items():
        rot = {'x': 90, **({'y': y} if y else {})}
        for k in range(LAYERS):
            thick = '|'.join(str(n) for n in range(k + 1, LAYERS + 1))
            if m['kind'] == 'grass':
                parts.append({'when': {'facing': facing, 'thickness': str(k + 1)}, 'apply': dict({'model': model_name(m['id'], k)}, **rot)})
                if k + 2 <= LAYERS:
                    deeper = '|'.join(str(n) for n in range(k + 2, LAYERS + 1))
                    parts.append({'when': {'facing': facing, 'thickness': deeper}, 'apply': dict({'model': model_name(m['id'], k, True)}, **rot)})
            else:
                parts.append({'when': {'facing': facing, 'thickness': thick}, 'apply': dict({'model': model_name(m['id'], k)}, **rot)})
    return {'multipart': parts}


# ---------------------------------------------------------------- data
def conditions(mods, saw=None):
    """Load conditions (Fabric, NeoForge, Forge) for the required mods; saw=True/False adds Simple Blocks loaded/not loaded."""
    fabric, neo, forge = [], [], []
    need = list(mods)
    if saw and octets.SAW_MOD not in need:
        need.append(octets.SAW_MOD)
    for mod in need:
        fabric.append({'condition': 'fabric:all_mods_loaded', 'values': [mod]})
        neo.append({'type': 'neoforge:mod_loaded', 'modid': mod})
        forge.append({'type': 'forge:mod_loaded', 'modid': mod})
    if saw is False:
        fabric.append({'condition': 'fabric:not', 'value': {'condition': 'fabric:all_mods_loaded', 'values': [octets.SAW_MOD]}})
        neo.append({'type': 'neoforge:not', 'value': {'type': 'neoforge:mod_loaded', 'modid': octets.SAW_MOD}})
        forge.append({'type': 'forge:not', 'value': {'type': 'forge:mod_loaded', 'modid': octets.SAW_MOD}})
    if not fabric:
        return {}
    return {'fabric:load_conditions': fabric, 'neoforge:conditions': neo,
            'forge:condition': forge[0] if len(forge) == 1 else {'type': 'forge:and', 'values': forge}}


def unlock(recipe, item):
    crit = 'has_' + item.split(':')[1]
    return {'parent': 'minecraft:recipes/root',
            'criteria': {crit: {'conditions': {'items': [{'items': item}]}, 'trigger': 'minecraft:inventory_changed'},
                         'has_the_recipe': {'conditions': {'recipes': f'{NS}:{recipe}'}, 'trigger': 'minecraft:recipe_unlocked'}},
            'requirements': [['has_the_recipe', crit]], 'rewards': {'recipes': [f'{NS}:{recipe}']}}


def loot(m):
    """One pool per layer: every occupied layer drops one half slab (owner N86 'Abbau/Drops')."""
    hid = f'{NS}:{m["id"]}'
    pools = [{'rolls': 1, 'bonus_rolls': 0,
              'conditions': [{'condition': 'minecraft:block_state_property', 'block': hid, 'properties': {f'l{i}': 'true'}},
                             {'condition': 'minecraft:survives_explosion'}],
              'entries': [{'type': 'minecraft:item', 'name': hid}]} for i in range(LAYERS)]
    return dict(conditions(m['mods']), type='minecraft:block', pools=pools, random_sequence=f'{NS}:blocks/{m["id"]}')


def side_loot(m):
    """Side form: thickness half slabs."""
    hid, sid = f'{NS}:{m["id"]}', f'{NS}:{side_id(m["id"])}'
    counts = [{'function': 'minecraft:set_count', 'count': n, 'add': False,
               'conditions': [{'condition': 'minecraft:block_state_property', 'block': sid, 'properties': {'thickness': str(n)}}]}
              for n in range(2, LAYERS + 1)]
    return dict(conditions(m['mods']), type='minecraft:block', pools=[{
        'rolls': 1, 'bonus_rolls': 0,
        'entries': [{'type': 'minecraft:item', 'name': hid, 'functions': counts + [{'function': 'minecraft:explosion_decay'}]}]}],
        random_sequence=f'{NS}:blocks/{side_id(m["id"])}')


def opt(path, m):
    return f'{NS}:{path}' if not m['mods'] else {'id': f'{NS}:{path}', 'required': False}


VIBRATION_DAMPENING = {'simpleblocks:hushed_deepslate_tile_slab'}


def data_files(mats):
    files = {}
    d = lambda rel, data: files.__setitem__(os.path.join(DATA, *rel.split('/')), octets.js(data))
    tags = {f'{NS}/tags/block/half_slabs': [], f'{NS}/tags/block/wooden_half_slabs': [], f'{NS}/tags/item/half_slabs': [],
            f'{NS}/tags/item/wooden_half_slabs': [], 'minecraft/tags/block/mineable/axe': [], 'minecraft/tags/block/mineable/pickaxe': [],
            'minecraft/tags/block/mineable/shovel': [], 'minecraft/tags/block/occludes_vibration_signals': [],
            'minecraft/tags/block/dampens_vibrations': []}
    for m in mats:
        hid, slab, sid = f'{NS}:{m["id"]}', m['slab'], side_id(m['id'])
        d(f'{NS}/loot_table/blocks/{m["id"]}.json', loot(m))
        d(f'{NS}/loot_table/blocks/{sid}.json', side_loot(m))
        sources = [(slab, SLAB_CUT)] + ([(m['base'], BLOCK_CUT)] if m['base'] else [])
        for src, count in sources:
            sp = src.split(':')[1]
            saw = f'half_slab/{m["id"]}_from_{sp}_sawing'
            d(f'{NS}/recipe/{saw}.json', dict(conditions(m['mods'], True), type=octets.SAW_TYPE, ingredient=src,
                                              result={'count': count, 'id': hid}))
            if STONECUTTER_FALLBACK and m['ns'] != octets.SAW_MOD:
                cut = f'half_slab/{m["id"]}_from_{sp}_stonecutting'
                fb = conditions(m['mods'], False)
                d(f'{NS}/recipe/{cut}.json', dict(fb, type='minecraft:stonecutting', ingredient=src, result={'count': count, 'id': hid}))
                d(f'{NS}/advancement/recipes/building_blocks/{cut}.json', dict(fb, **unlock(cut, src)))
        back = f'half_slab/{slab.split(":")[1]}_from_half_slabs'
        recipe = dict(type='minecraft:crafting_shapeless', category='building', ingredients=[hid] * HALVES_PER_SLAB, result={'id': slab})
        d(f'{NS}/recipe/{back}.json', dict(conditions(m['mods']), **recipe))
        d(f'{NS}/advancement/recipes/building_blocks/{back}.json', dict(conditions(m['mods']), **unlock(back, hid)))
        for path in (m['id'], sid):
            tags[f'{NS}/tags/block/half_slabs'].append(opt(path, m))
            if m['group'] == 'wood':
                tags[f'{NS}/tags/block/wooden_half_slabs'].append(opt(path, m))
            if m['tool']:
                tags[f'minecraft/tags/block/mineable/{m["tool"]}'].append(opt(path, m))
            if m['group'] == 'wool' or m['slab'] in VIBRATION_DAMPENING:
                for t in ('occludes_vibration_signals', 'dampens_vibrations'):
                    tags[f'minecraft/tags/block/{t}'].append(opt(path, m))
        tags[f'{NS}/tags/item/half_slabs'].append(opt(m['id'], m))
        if m['group'] == 'wood':
            tags[f'{NS}/tags/item/wooden_half_slabs'].append(opt(m['id'], m))
    for name, values in tags.items():
        d(f'{name}.json', {'values': values})
    return files


# ---------------------------------------------------------------- lang, java
def generated_names(mats, lang):
    out = {}
    for m in mats:
        name = m['en' if lang == 'en_us' else 'de']
        out[f'block.{NS}.{m["id"]}'] = name
        out[f'block.{NS}.{side_id(m["id"])}'] = name   # the side form is the same half slab, placed against a wall
        out[f'item.{NS}.{m["id"]}'] = name
    return out


def merged_lang(lang, mats):
    """Module lang: every other key keeps its place, the half slab names follow at the end (replaced on each run)."""
    path = os.path.join(LANG_DIR, lang + '.json')
    with open(path, encoding='utf-8') as f:
        data = json.load(f)
    out = {k: v for k, v in data.items() if not k.endswith('_half_slab')}
    out.update(generated_names(mats, lang))
    return path, (json.dumps(out, indent=2, ensure_ascii=False) + '\n').encode('utf-8')


def java_str(v):
    return 'null' if v is None else '"' + v + '"'


def java_list(mats):
    rows = ',\n'.join(
        f'            new Material("{m["id"]}", "{m["slab"]}", {java_str(m["base"])}, '
        f'List.of({", ".join(chr(34) + x + chr(34) for x in m["mods"])}), "{m["group"]}", {str(m["falls"]).lower()}, '
        f'{str(m["kind"] == "grass").lower()})' for m in mats)
    groups = ', '.join(f'"{g}"' for g in ['wood'] + [g for g, _ in octets.GROUPS])
    return f"""package com.simpleslabs.registry;

import java.util.List;

/**
 * GENERATED by tools/textures/half_slabs_2026_11_17.py from tools/textures/half_slab_materials.json - do not edit by hand.
 * N86: one {{@code <slab path without _slab>_half_slab}} per slab (plus its side form); {{@link SlabRegistry}} registers them in
 * creative-tab order, the ones of another mod's slab only while every mod in {{@code mods}} is loaded.
 */
public final class HalfSlabMaterials {{
    /** id, the real slab ({{@code ns:path}}), its full block (null = unknown), mods, tab group, falls like sand, grass model. */
    public record Material(String id, String slab, String base, List<String> mods, String group, boolean falls, boolean grass) {{
    }}

    /** Creative-tab groups in display order. */
    public static final List<String> GROUPS = List.of({groups});

    public static final List<Material> ALL = List.of(
{rows});

    private HalfSlabMaterials() {{
    }}
}}
"""


# ---------------------------------------------------------------- outputs
def outputs():
    files = {}
    a = lambda rel, data: files.__setitem__(os.path.join(ASSETS, *rel.split('/')), octets.js(data))
    mats = materials()
    roots = asset_roots()
    templates = {'slab': {}, 'grass': {}, 'dirt': {}, 'checker': {}}
    checker_refs = next((m['cells'] for m in mats if m['kind'] == 'checker'), None)
    cells = [checker_cells(roots, r) for r in checker_refs] if checker_refs else None
    for i in range(LAYERS):
        templates['slab'][i] = slab_template(i)
        templates['grass'][i] = grass_template(i)
        templates['dirt'][i] = dirt_template(i)
        if cells:
            templates['checker'][i] = checker_template(cells[0], cells[1], i)
        for kind, tpl in templates.items():
            if i in tpl:
                a(f'models/block/half_slab/_{kind}_{i}.json', tpl[i])
    for m in mats:
        for i in range(LAYERS):
            a(f'models/block/half_slab/{m["id"]}_{i}.json', {'parent': f'{NS}:block/half_slab/_{m["kind"]}_{i}', 'textures': m['tex']})
            if m['kind'] == 'grass':
                a(f'models/block/half_slab/{m["id"]}_{i}_dirt.json', {'parent': f'{NS}:block/half_slab/_dirt_{i}', 'textures': m['tex']})
        a(f'blockstates/{m["id"]}.json', blockstate(m))
        a(f'blockstates/{side_id(m["id"])}.json', side_blockstate(m))
        root = item_root(m['ns'])
        files[os.path.join(root, 'models', 'item', 'half_slab', m['id'] + '.json')] = octets.js(item_model(m, templates))
        files[os.path.join(root, 'items', m['id'] + '.json')] = octets.js(item_definition(m))
    files[JAVA_LIST] = java_list(mats).encode('utf-8')
    files.update(data_files(mats))
    for lang in ('en_us', 'de_de'):
        path, data = merged_lang(lang, mats)
        files[path] = data
    return files


def main():
    if '--scan' in sys.argv:
        scan()
        return
    check = '--check' in sys.argv
    out = sys.argv[sys.argv.index('--out') + 1] if '--out' in sys.argv else None
    files = outputs()
    bad = []
    for path, data in files.items():
        target = os.path.join(out, os.path.relpath(path, REPO)) if out else path
        if check:
            try:
                with open(target, 'rb') as f:
                    if f.read() != data:
                        bad.append(target)
            except OSError:
                bad.append(target)
        else:
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, 'wb') as f:
                f.write(data)
    if check:
        if bad:
            print(f'OUTDATED: {len(bad)} files, e.g. {bad[0]}')
            sys.exit(1)
        print(f'OK: {len(files)} half slab files up to date')
        return
    print(f'wrote {len(files)} files' + (f' below {out}' if out else ''))


if __name__ == '__main__':
    main()
