"""Usage: python tools/textures/half_slabs_2026_11_17.py [--scan] [--check] [--out DIR]

N86 half slabs (docs/ai/PLAN-N86-HALFSLAB-2026-11-17.md) - DRAFT ("N86 Vorbereitung, nicht buendeln"): the owner has not
answered N86-1..11 yet. Every place that depends on an answer is a constant below, marked N86-<n>.

One half slab per block that has a slab (vanilla + the sub-mods), in module simpleoctets (N86-4), id
simpleoctets:<slab path without _slab>_half_slab. The list lives in tools/textures/half_slab_materials.json (made by --scan
from the 26.3 client jar, the module assets and the German lang cache) and produces:

  blockstates/<id>.json                               half x layers -> model (waterlogged not part of the key)
  models/block/half_slab/_<kind>_<L>[_top].json       templates (kind = slab | checker), L = layer count
  models/block/half_slab/<id>_<L>[_top].json          per material: template + the slab's own texture references
  items/<id>.json + models/item/half_slab/<id>.json   (another mod's material: in that mod's jar, self-contained)
  shared/java/com/simpleoctets/registry/HalfSlabMaterials.java   GENERATED list + the N86 constants for the Java side
  lang en_us/de_de                                    names (generated block after the octet keys)
  data: loot_table/blocks/<id>.json, recipe/half_slab/*.json (+ advancements), tags simpleoctets:half_slabs,
        simpleoctets:wooden_half_slabs, simpleoctets:half_slab/mineable/<tool>, .../occludes_vibration_signals|dampens_vibrations
        (referenced from the Vanilla tag files the octet generator writes, optional entries).

Recipes and advancements sit in a sub-folder (recipe/half_slab/): the octet generator removes every recipe file directly in
recipe/ it did not write (STALE), and modules/simpleoctets/tools/check_data.py counts them.

No Mojang pixels: models reference vanilla textures by resource location only; nothing is copied. --check compares instead
of writing; --out DIR writes (or checks) the same tree below DIR instead of the repository (draft/scratch runs).
"""
import json
import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import octets_2026_10_09 as octets  # noqa: E402  shared helpers: js, group_of, GROUPS, DYES, COPPER_ORDER, SAW_*, DE_LANG, CLIENT_JAR

REPO = octets.REPO
NS = octets.NS
MODULE = octets.MODULE
ASSETS = octets.ASSETS
DATA = octets.DATA
MATERIALS_JSON = os.path.join(REPO, 'tools', 'textures', 'half_slab_materials.json')
JAVA_LIST = os.path.join(MODULE, 'shared', 'java', 'com', NS, 'registry', 'HalfSlabMaterials.java')
LANG_DIR = os.path.join(ASSETS, 'lang')

# ---------------------------------------------------------------- owner answers (N86) - change only here
# N86-1: height of one half slab in pixels (4 = half a slab). Must divide 8.
HEIGHT_PX = 4
# N86-2: stacking. 'A+' = 4-px grid with the real slabs (1 -> +1 = slab -> +1 = 3 layers -> +1 = double slab);
# 'A' = only 1+1 -> slab; 'B' = own layer block up to 16 px (no slab merge); 'C' = not stackable.
STACK_MODE = 'A+'
# N86-3: names. EN: "<slab name without ' Slab'>" + NAME_EN; DE: "...stufe" -> "..." + NAME_DE.
NAME_EN = ' Half Slab'
NAME_DE = 'halbstufe'
# N86-5: scope. Natural slabs (fall, grass colour, dry out) need own classes - out in the first stage like the octets (A4).
NATURE = {'simpleblocks:dirt_slab', 'simpleblocks:grass_slab', 'simpleblocks:sand_slab', 'simpleblocks:red_sand_slab',
          'simpleblocks:gravel_slab'}
INCLUDE_NATURE = False
VANILLA_ONLY = False
# Always out: petrified_oak_slab has no full block of its own (same exception as the octets, OctetTests.NO_OCTET).
EXCLUDE = {'minecraft:petrified_oak_slab'}
# N86-6: recipes. Saw: 1 slab -> SLAB_CUT, 1 base block -> BLOCK_CUT (both follow from the height); crafting HALVES_PER_SLAB
# half slabs -> 1 slab. No "3 slabs -> 6 half slabs" (collides with the 16 vanilla cushion recipes, ###).
SLAB_CUT = 8 // HEIGHT_PX
BLOCK_CUT = 16 // HEIGHT_PX
HALVES_PER_SLAB = 8 // HEIGHT_PX
CRAFT_BACK = True
STONECUTTER_FALLBACK = True   # A4b: only without Simple Blocks (load condition), like the octets
# N86-11: never in #minecraft:slabs (foreign code assumes SlabBlock.TYPE there).


def layer_plan():
    """(max own layer count, merge into the real slab at 8 px / double at 16 px, half slab may be put onto a slab)."""
    per_slab, per_block = 8 // HEIGHT_PX, 16 // HEIGHT_PX
    if STACK_MODE == 'A+':
        return per_block - 1, True, True
    if STACK_MODE == 'A':
        return per_slab - 1, True, False
    if STACK_MODE == 'B':
        return per_block, False, False
    if STACK_MODE == 'C':
        return 1, False, False
    raise ValueError(STACK_MODE)


MAX_LAYERS, MERGE_SLAB, ONTO_SLAB = layer_plan()
assert 8 % HEIGHT_PX == 0 and MAX_LAYERS >= 1


def own_layers():
    """Layer counts a half slab block really holds (A+: 8 px is the real slab, 16 px its double - never set, see Java)."""
    if not MERGE_SLAB:
        return list(range(1, MAX_LAYERS + 1))
    return [n for n in range(1, MAX_LAYERS + 1) if n * HEIGHT_PX not in (8, 16)]


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


# Properties template (vanilla block) of another mod's slab - mirrors OctetRegistry (its block may register after us).
BLOCK_TEMPLATES = dict({k: 'minecraft:' + v for k, v in {
    'blue_nether_brick': 'red_nether_bricks', 'hushed_deepslate_tile': 'deepslate_tiles', 'sculk_tile': 'deepslate_tiles',
    'mossy_deepslate_brick': 'deepslate_bricks', 'mossy_deepslate_tile': 'deepslate_tiles', 'mossy_tuff_brick': 'tuff_bricks',
    'mossy_mud_brick': 'mud_bricks', 'mossy_end_stone_brick': 'end_stone_bricks', 'mossy_cinnabar_brick': 'cinnabar_bricks',
    'mossy_sulfur_brick': 'sulfur_bricks', 'ice_brick': 'ice', 'packed_ice_brick': 'packed_ice', 'blue_ice_brick': 'blue_ice',
    'quartz_brick': 'quartz_bricks', 'cut_gold': 'gold_block', 'cut_iron': 'iron_block', 'polished_end_stone': 'end_stone',
    'ashwood': 'oak_planks', 'duskwood': 'oak_planks', 'crimson_blazewood': 'crimson_planks', 'warped_blazewood': 'warped_planks',
}.items()})


# Slabs whose stem names a non-full block (bamboo = the plant).
BASE_OVERRIDE = {'minecraft:bamboo_slab': ['bamboo_planks']}


def template_of(ns, stem, van):
    if ns == 'minecraft':
        return f'minecraft:{stem}_slab'
    if stem in BLOCK_TEMPLATES:
        return BLOCK_TEMPLATES[stem]
    if ns == 'simpleores':
        return 'minecraft:gold_block' if 'silver' in stem else 'minecraft:iron_block'
    if ns == 'simplechess':
        return 'minecraft:quartz_block'
    if ns == 'simplebuilding':
        return 'minecraft:purpur_block' if stem.endswith('_purpur') else 'minecraft:end_stone'
    if stem in van:
        return f'minecraft:{stem}'
    raise SystemExit(f'no property template for {ns}:{stem}_slab - add it to BLOCK_TEMPLATES')


def light_of(ns, stem):
    """Glow of the astral blocks (OctetRegistry: Astralit and astral End stone/purpur 10, chess astralit checkers 5)."""
    if ns == 'simplebuilding' and ('astralit' in stem or stem.startswith('astral_')):
        return 10
    if ns == 'simplechess' and 'astralit' in stem:
        return 5
    return 0


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
    for ns, path, state in slabs:
        sid = f'{ns}:{path}'
        if sid in EXCLUDE or (sid in NATURE and not INCLUDE_NATURE) or (VANILLA_ONLY and ns != 'minecraft'):
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
        else:
            if ns not in langs:
                langs[ns] = (mod_lang(roots, ns, 'en_us'), mod_lang(roots, ns, 'de_de'))
            en, de = langs[ns][0].get(f'block.{ns}.{path}'), langs[ns][1].get(f'block.{ns}.{path}')
        if not (en and de):
            raise SystemExit(f'{sid}: no EN/DE name')
        bpath = base.split(':')[1] if base else stem
        wood = bpath.endswith('_planks') or bpath == 'bamboo_mosaic'
        group = 'wood' if wood else ('mod' if ns != 'minecraft' else octets.group_of('minecraft', bpath))
        tool = 'axe' if wood else (None if bpath.endswith('_wool') else 'pickaxe')
        mods = [] if ns in ('minecraft', 'simplelib') else [ns]
        if ns == 'simplechess' and any(w in stem for w in ('astralit', 'nihilith', 'ender_quartz')):
            mods.append('simplebuilding')   # ChessColor palette colours exist only with SimpleBuilding (PALETTE_SOURCE)
        out.append({'id': f'{stem}_half_slab', 'slab': sid, 'base': base, 'ns': ns, 'mods': mods, 'group': group,
                    'kind': kind, 'tex': textures, **({'cells': cells} if cells else {}), 'tool': tool, 'template': template_of(ns, stem, van),
                    'light': light_of(ns, stem), 'en': english(en, sid), 'de': german(de, sid)})
    groups = ['wood'] + [g for g, _ in octets.GROUPS]
    out.sort(key=lambda m: (groups.index(m['group']), m['id']))
    with open(MATERIALS_JSON, 'w', encoding='utf-8') as f:
        f.write(json.dumps(out, indent=1, ensure_ascii=False) + '\n')
    print(f'scanned {len(out)} half slabs, skipped {len(skipped)}: {", ".join(skipped)}')


def materials():
    with open(MATERIALS_JSON, encoding='utf-8') as f:
        return json.load(f)


# ---------------------------------------------------------------- models
def model_name(mid, layers, top):
    return f'{NS}:block/half_slab/{mid}_{layers}' + ('_top' if top else '')


def slab_template(layers, top):
    """Like minecraft:block/slab, height layers*HEIGHT_PX at the bottom or the top of the cell (auto UV = the matching strip)."""
    h = layers * HEIGHT_PX
    y0, y1 = (16 - h, 16) if top else (0, h)
    faces = {d: {'texture': '#side', 'cullface': d} for d in ('north', 'south', 'east', 'west')}
    faces['down'] = {'texture': '#bottom', **({'cullface': 'down'} if y0 == 0 else {})}
    faces['up'] = {'texture': '#top', **({'cullface': 'up'} if y1 == 16 else {})}
    return {'parent': 'minecraft:block/block', 'textures': {'particle': '#side'},
            'elements': [{'from': [0, y0, 0], 'to': [16, y1, 16], 'faces': faces}]}


def checker_template(cells_model, layers, top):
    """The chess checker slab's cells (simplechess:block/checker_cells/<n>, 4 cells 8x8x8) cut to the half slab's height."""
    h = layers * HEIGHT_PX
    out = json.loads(json.dumps(cells_model))
    for e in out['elements']:
        if top:
            e['from'][1] = 16 - h
        else:
            e['to'][1] = h
        for d, f in e['faces'].items():
            if d in ('north', 'south', 'east', 'west') and 'uv' in f:
                u0, v0, u1, v1 = f['uv']
                f['uv'] = [u0, v0, u1, v0 + h] if top else [u0, v1 - h, u1, v1]
    return out


def checker_cells(roots, ref):
    ns, path = ref.split(':')
    m = mod_json(roots, ns, f'models/{path}.json')
    if m is None:
        raise SystemExit('checker cells model missing: ' + ref)
    return m


def item_model(m, block_models):
    """Item model = the 1-layer bottom block model (like a vanilla slab item). Partner materials inline it (no parent from
    this module: the partner's jar may run without Simple Octets)."""
    if m['ns'] in octets.PARTNER_RES or m['ns'] in EXTRA_PARTNER_RES:
        tpl = json.loads(json.dumps(block_models[m['kind']][(1, False)]))
        tpl['textures'] = dict(tpl['textures'], **m['tex'])
        return tpl
    return {'parent': model_name(m['id'], 1, False)}


# Simple Chess carries its half slab items like the other partners (octets have no chess partner entry: chess octets are its own).
EXTRA_PARTNER_RES = {'simplechess': os.path.join(REPO, 'modules', 'simplechess', 'shared', 'resources')}


def item_root(ns):
    res = dict(octets.PARTNER_RES, **EXTRA_PARTNER_RES)
    return os.path.join(res[ns], 'assets', NS) if ns in res else ASSETS


def blockstate(m):
    variants = {}
    for half in ('bottom', 'top'):
        for n in range(1, MAX_LAYERS + 1):
            variants[f'half={half},layers={n}'] = {'model': model_name(m['id'], n, half == 'top')}
    return {'variants': variants}


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
    hid = f'{NS}:{m["id"]}'
    counts = [{'function': 'minecraft:set_count', 'count': n, 'add': False,
               'conditions': [{'condition': 'minecraft:block_state_property', 'block': hid, 'properties': {'layers': str(n)}}]}
              for n in range(2, MAX_LAYERS + 1)]
    return dict(conditions(m['mods']), type='minecraft:block', pools=[{
        'rolls': 1, 'bonus_rolls': 0,
        'entries': [{'type': 'minecraft:item', 'name': hid, 'functions': counts + [{'function': 'minecraft:explosion_decay'}]}]}],
        random_sequence=f'{NS}:blocks/{m["id"]}')


def opt(mid, m):
    return f'{NS}:{mid}' if not m['mods'] else {'id': f'{NS}:{mid}', 'required': False}


VIBRATION_DAMPENING = {'simpleblocks:hushed_deepslate_tile_slab'}
TAG_REFS = ('mineable/axe', 'mineable/pickaxe', 'occludes_vibration_signals', 'dampens_vibrations')


def data_files(mats):
    files = {}
    d = lambda rel, data: files.__setitem__(os.path.join(DATA, *rel.split('/')), octets.js(data))
    tags = {'half_slabs': [], 'wooden_half_slabs': [], 'half_slab/mineable/axe': [], 'half_slab/mineable/pickaxe': [],
            'half_slab/occludes_vibration_signals': [], 'half_slab/dampens_vibrations': []}
    for m in mats:
        hid, slab = f'{NS}:{m["id"]}', m['slab']
        d(f'{NS}/loot_table/blocks/{m["id"]}.json', loot(m))
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
        if CRAFT_BACK:
            back = f'half_slab/{slab.split(":")[1]}_from_half_slabs'
            if HALVES_PER_SLAB == 2:
                recipe = dict(type='minecraft:crafting_shaped', category='building', pattern=['#', '#'], key={'#': hid},
                              result={'id': slab})
            else:
                recipe = dict(type='minecraft:crafting_shapeless', category='building', ingredients=[hid] * HALVES_PER_SLAB,
                              result={'id': slab})
            d(f'{NS}/recipe/{back}.json', dict(conditions(m['mods']), **recipe))
            d(f'{NS}/advancement/recipes/building_blocks/{back}.json', dict(conditions(m['mods']), **unlock(back, hid)))
        tags['half_slabs'].append(opt(m['id'], m))
        if m['group'] == 'wood':
            tags['wooden_half_slabs'].append(opt(m['id'], m))
        if m['tool']:
            tags[f'half_slab/mineable/{m["tool"]}'].append(opt(m['id'], m))
        if m['group'] == 'wool' or m['slab'] in VIBRATION_DAMPENING:
            for t in ('occludes_vibration_signals', 'dampens_vibrations'):
                tags[f'half_slab/{t}'].append(opt(m['id'], m))
    for name, values in tags.items():
        d(f'{NS}/tags/block/{name}.json', {'values': values})
    d(f'{NS}/tags/item/half_slabs.json', {'values': tags['half_slabs']})
    d(f'{NS}/tags/item/wooden_half_slabs.json', {'values': tags['wooden_half_slabs']})
    return files


# ---------------------------------------------------------------- lang, java
def generated_names(mats, lang):
    out = {}
    for m in mats:
        name = m['en' if lang == 'en_us' else 'de']
        out[f'block.{NS}.{m["id"]}'] = name
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
        f'            new Material("{m["id"]}", "{m["slab"]}", {java_str(m["base"])}, "{m["template"]}", '
        f'List.of({", ".join(chr(34) + x + chr(34) for x in m["mods"])}), "{m["group"]}", {m["light"]})' for m in mats)
    groups = ', '.join(f'"{g}"' for g in ['wood'] + [g for g, _ in octets.GROUPS])
    return f"""package com.simpleoctets.registry;

import java.util.List;

/**
 * GENERATED by tools/textures/half_slabs_2026_11_17.py from tools/textures/half_slab_materials.json - do not edit by hand.
 * N86 half slabs (DRAFT): one {{@code <slab path without _slab>_half_slab}} per slab; {{@link HalfSlabRegistry}} registers them
 * in creative-tab order, the ones of another mod's slab only while every mod in {{@code mods}} is loaded.
 */
public final class HalfSlabMaterials {{
    /** id, the real slab ({{@code ns:path}}), its full block (null = unknown), vanilla block whose properties are copied, mods, tab group, light. */
    public record Material(String id, String slab, String base, String template, List<String> mods, String group, int light) {{
    }}

    /** N86-1: height of one layer in pixels. */
    public static final int HEIGHT_PX = {HEIGHT_PX};
    /** N86-2 ({STACK_MODE}): highest layer count of the block state {{@code layers}}. */
    public static final int MAX_LAYERS = {MAX_LAYERS};
    /** N86-2: 8 px becomes the real slab, 16 px its double slab. */
    public static final boolean MERGE_SLAB = {str(MERGE_SLAB).lower()};
    /** N86-2: a half slab put onto the matching real slab adds a layer (A+: 8 + 4 = 12 px). */
    public static final boolean ONTO_SLAB = {str(ONTO_SLAB).lower()};

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
    block_models = {'slab': {}, 'checker': {}}
    for n in range(1, MAX_LAYERS + 1):
        for top in (False, True):
            block_models['slab'][(n, top)] = slab_template(n, top)
            a(f'models/block/half_slab/_slab_{n}{"_top" if top else ""}.json', slab_template(n, top))
    checker_refs = next((m['cells'] for m in mats if m['kind'] == 'checker'), None)
    if checker_refs:
        bottom_cells, top_cells = (checker_cells(roots, r) for r in checker_refs)
        for n in range(1, MAX_LAYERS + 1):
            for top in (False, True):
                tpl = checker_template(top_cells if top else bottom_cells, n, top)
                block_models['checker'][(n, top)] = tpl
                a(f'models/block/half_slab/_checker_{n}{"_top" if top else ""}.json', tpl)
    for m in mats:
        tex = m['tex']
        for n in range(1, MAX_LAYERS + 1):
            for top in (False, True):
                a(f'models/block/half_slab/{m["id"]}_{n}{"_top" if top else ""}.json',
                  {'parent': f'{NS}:block/half_slab/_{m["kind"]}_{n}{"_top" if top else ""}', 'textures': tex})
        a(f'blockstates/{m["id"]}.json', blockstate(m))
        root = item_root(m['ns'])
        files[os.path.join(root, 'models', 'item', 'half_slab', m['id'] + '.json')] = octets.js(item_model(m, block_models))
        files[os.path.join(root, 'items', m['id'] + '.json')] = octets.js(
            {'model': {'type': 'minecraft:model', 'model': f'{NS}:item/half_slab/{m["id"]}'}})
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
