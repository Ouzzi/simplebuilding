"""Usage: python -I tools/textures/check_half_slabs.py [OUT_DIR]

N86 half slabs: pure-Python checks of the generator output (half_slabs_2026_11_17.py). OUT_DIR = the tree written with
--out (scratch run); without it the repository itself. Checks: JSON valid; ids unique and new; blockstate -> model -> parent
-> texture references resolve (vanilla by name in the client jar, nothing extracted); item definitions/models; loot, recipe,
tag and lang references; ids of other mods only behind load conditions or as optional tag entries; no PNG written; and the
suffix-risk sites (every "_slab" suffix table must try "_side_half_slab"/"_half_slab" first, so a half slab keeps its family).
N111: the shipped model since N86C/N86S - the half slab is a multipart of 4-px layers l0..l3 (every one of the 15 layer sets
shows exactly its layers), the side half slab its own block and item (facing x thickness 1..4, the same layer models turned).
"""
import json
import os
import re
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import half_slabs_2026_11_17 as gen  # noqa: E402

REPO = gen.REPO
NS = gen.NS
OUT = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else REPO
errors = []


def err(msg):
    if msg not in errors:
        errors.append(msg)


def out_path(p):
    return os.path.join(OUT, os.path.relpath(p, REPO))


mats = gen.materials()
ids = [m['id'] for m in mats]
by_id = {m['id']: m for m in mats}

# ---- ids
if len(set(ids)) != len(ids):
    err('duplicate half slab ids')
for m in mats:
    if not m['id'].endswith('_half_slab') or m['id'] != m['slab'].split(':')[1][:-len('_slab')] + '_half_slab':
        err(f'id rule: {m["id"]} <- {m["slab"]}')
z = zipfile.ZipFile(gen.octets.CLIENT_JAR)
jar = set(z.namelist())
van_blocks = {n[len('assets/minecraft/blockstates/'):-5] for n in jar if n.startswith('assets/minecraft/blockstates/')}
roots = gen.asset_roots()
existing = {}
for r in roots:
    for ns in os.listdir(r):
        bs = os.path.join(r, ns, 'blockstates')
        if os.path.isdir(bs):
            existing.setdefault(ns, set()).update(n[:-5] for n in os.listdir(bs) if n.endswith('.json'))
if any(p.endswith('_half_slab') for p in van_blocks):
    err('vanilla already has a *_half_slab block')
clash = set(ids) & {p for p in existing.get(NS, set()) if not p.endswith('_half_slab')}
if clash:
    err(f'ids taken in {NS}: {sorted(clash)}')
other_half = sorted(f'{ns}:{p}' for ns, ps in existing.items() if ns != NS for p in ps if p.endswith('_half_slab'))
if other_half:
    err(f'another mod has *_half_slab ids: {other_half}')
if len(mats) != 170:
    err(f'scope is every slab incl. the natural ones: 170 (N86-5, SlabTests), list has {len(mats)}')

# ---- all written files: JSON valid, no PNG
files = gen.outputs()
written = {}
for p, data in files.items():
    op = out_path(p)
    if not os.path.isfile(op):
        err(f'not written: {op}')
        continue
    if op.endswith('.png'):
        err(f'PNG written (no Mojang pixels): {op}')
    if op.endswith('.json'):
        try:
            written[op] = json.load(open(op, encoding='utf-8'))
        except ValueError as e:
            err(f'invalid JSON {op}: {e}')
for dirpath, _, names in os.walk(OUT if OUT != REPO else os.path.join(gen.MODULE)):
    for n in names:
        if n.endswith('.png') and 'half_slab' in n:
            err(f'half slab PNG: {os.path.join(dirpath, n)}')

# ---- model / texture resolution
ASSETS_OUT = out_path(gen.ASSETS)


def model_json(ref, item_root=None):
    ns, path = ref.split(':') if ':' in ref else ('minecraft', ref)
    if ns == 'minecraft':
        name = f'assets/minecraft/models/{path}.json'
        return json.loads(z.read(name)) if name in jar else None
    if ns == NS:
        for base in ([item_root] if item_root else []) + [ASSETS_OUT]:
            p = os.path.join(base, 'models', path + '.json')
            if os.path.isfile(p):
                return json.load(open(p, encoding='utf-8'))
        return None
    return gen.mod_json(roots, ns, f'models/{path}.json')


# Vanilla-derived textures are built from the client jar, never committed (tools/textures/vanilla_derived.py): they count as present.
import vanilla_derived  # noqa: E402
DERIVED = {('simplebuilding', k[:-4]) for k in vanilla_derived.OUTPUTS} | \
          {(ns, k[:-4]) for ns, outs in vanilla_derived.MODULE_OUTPUTS.items() for k in outs}


def texture_exists(ref):
    ns, path = ref.split(':') if ':' in ref else ('minecraft', ref)
    if ns == 'minecraft':
        return f'assets/minecraft/textures/{path}.png' in jar
    if (ns, path) in DERIVED:
        return True
    return any(os.path.isfile(os.path.join(r, ns, 'textures', path + '.png')) for r in roots)


def check_model(ref, item_root=None, seen=None):
    """Walks the parent chain; every face texture variable must end at an existing texture."""
    chain, textures, elements = [], {}, None
    cur = ref
    while cur and not cur.startswith('builtin/'):
        m = model_json(cur, item_root)
        if m is None:
            err(f'model missing: {cur} (from {ref})')
            return
        chain.append(cur)
        for k, v in m.get('textures', {}).items():
            textures.setdefault(k, v)
        if elements is None and 'elements' in m:
            elements = m['elements']
        cur = m.get('parent')
        if len(chain) > 10:
            err(f'parent loop: {ref}')
            return
    used = {f['texture'] for e in (elements or []) for f in e.get('faces', {}).values()} | {'#particle'}
    for var in used:
        v, hops = var, 0
        while v.startswith('#') and hops < 10:
            v, hops = textures.get(v[1:], '?' + v), hops + 1
        if v.startswith('?') or v.startswith('#'):
            err(f'{ref}: texture variable {var} unresolved')
        elif not texture_exists(v):
            err(f'{ref}: texture {v} does not exist')


LAYER_NAMES = [f'l{i}' for i in range(gen.LAYERS)]


def matches(when, state):
    """Multipart condition: every property names the state's value (alternatives with |); OR lists are not used here."""
    return all(state.get(k) in str(v).split('|') for k, v in when.items())


def applied(bs, state):
    return [p['apply'] for p in bs['multipart'] if matches(p.get('when', {}), state)]


def layer_of(model, mid):
    """Layer index of a per-material layer model (<id>_<i> or <id>_<i>_dirt), else None."""
    m = re.fullmatch(re.escape(f'{NS}:block/half_slab/{mid}_') + r'(\d+)(_dirt)?', model)
    return int(m.group(1)) if m else None


checked_models = set()


def walk(model):
    if model not in checked_models:
        checked_models.add(model)
        check_model(model)


def check_item(item_id, m):
    root = out_path(gen.item_root(m['ns']))
    item = os.path.join(root, 'items', item_id + '.json')
    if not os.path.isfile(item):
        err(f'item definition missing: {item}')
        return
    ref = json.load(open(item, encoding='utf-8'))['model']['model']
    check_model(ref, item_root=root)
    im = model_json(ref, root)
    if root != ASSETS_OUT and im and str(im.get('parent', '')).startswith(NS + ':'):
        err(f'partner item model of {item_id} has a parent in {NS} (partner may run without it)')


for m in mats:
    mid, sid = m['id'], gen.side_id(m['id'])
    # half slab: multipart of the 4-px layers l0..l3 (any combination, N86-2 B / N86C)
    bs = written.get(os.path.join(ASSETS_OUT, 'blockstates', mid + '.json'))
    if not bs or 'multipart' not in bs:
        err(f'blockstate missing or not multipart: {mid}')
    else:
        props = {k for p in bs['multipart'] for k in p.get('when', {})}
        if props - set(LAYER_NAMES):
            err(f'{mid}: blockstate names unknown properties {sorted(props - set(LAYER_NAMES))}')
        for mask in range(1, 1 << gen.LAYERS):
            state = {f'l{i}': 'true' if mask >> i & 1 else 'false' for i in range(gen.LAYERS)}
            got = sorted(layer_of(a['model'], mid) if layer_of(a['model'], mid) is not None else -1 for a in applied(bs, state))
            want = [i for i in range(gen.LAYERS) if mask >> i & 1]
            if got != want:
                err(f'{mid}: layers {want} show models of layers {got}')
                break
            if any(a.get('x') or a.get('y') for a in applied(bs, state)):
                err(f'{mid}: a flat half slab layer is rotated')
                break
        for p in bs['multipart']:
            walk(p['apply']['model'])
    # side half slab (N86S): its own block, facing x thickness, layer k while thickness > k, turned x=90 to the wall
    side = written.get(os.path.join(ASSETS_OUT, 'blockstates', sid + '.json'))
    if not side or 'multipart' not in side:
        err(f'blockstate missing or not multipart: {sid}')
    else:
        props = {k for p in side['multipart'] for k in p.get('when', {})}
        if props != {'facing', 'thickness'}:
            err(f'{sid}: blockstate properties {sorted(props)}, want facing and thickness')
        for facing, y in gen.SIDE_Y.items():
            for t in range(1, gen.LAYERS + 1):
                parts = applied(side, {'facing': facing, 'thickness': str(t)})
                got = sorted(layer_of(a['model'], mid) if layer_of(a['model'], mid) is not None else -1 for a in parts)
                if got != list(range(t)):
                    err(f'{sid} {facing} thickness {t}: models of layers {got}, want {list(range(t))}')
                if any(a.get('x') != 90 or a.get('y', 0) != y for a in parts):
                    err(f'{sid} {facing}: layers not turned x=90 y={y}')
        for p in side['multipart']:
            walk(p['apply']['model'])
    check_item(mid, m)
    check_item(sid, m)

# ---- data references
def known_item(ref):
    ns, path = ref.split(':')
    if ns == 'minecraft':
        return path in van_blocks or f'assets/minecraft/items/{path}.json' in jar
    if ns == NS:
        return path in by_id or path in side_ids
    return path in existing.get(ns, set())


side_ids = {gen.side_id(m['id']) for m in mats}
DATA_OUT = out_path(os.path.join(gen.DATA, NS))
for p, d in written.items():
    if not p.startswith(DATA_OUT):
        continue
    text = json.dumps(d)
    for ref in set(re.findall(r'"((?:minecraft|simple[a-z]+):[a-z0-9_/]+)"', text)):
        ns, path = ref.split(':')
        if '/' in path and ns == NS:   # recipe ids (half_slab/...), random sequences (blocks/...), tag paths
            continue
        if ns in ('minecraft',) and ('/' in path or path in ('crafting_shaped', 'crafting_shapeless', 'stonecutting', 'block',
                                                           'item', 'set_count', 'explosion_decay', 'block_state_property', 'match_block',
                                                           'inventory_changed', 'recipe_unlocked')):
            continue
        if ns == 'simpleblocks' and path == 'sawing':
            continue
        if ns in ('fabric', 'neoforge', 'forge'):
            continue
        if not known_item(ref):
            err(f'{os.path.relpath(p, OUT)}: unknown id {ref}')
    for ns in ('simplebuilding', 'simpleend', 'simpleblocks', 'simpleores', 'simpleenchanting', 'simplechess'):
        if f'"{ns}:' in text and not (ns == 'simpleblocks' and '"simpleblocks:sawing"' in text and text.count('"simpleblocks:') == 1):
            ok = 'fabric:load_conditions' in d or all(isinstance(v, dict) and v.get('required') is False
                                                      for v in d.get('values', []) if f'{ns}:' in json.dumps(v))
            if not ok:
                err(f'unconditional foreign id {ns} in {os.path.relpath(p, OUT)}')
recipe_dir = os.path.join(DATA_OUT, 'recipe')
if os.path.isdir(recipe_dir) and [n for n in os.listdir(recipe_dir) if 'half_slab' in n and n.endswith('.json')]:
    err('half slab recipes directly in recipe/ (the octet generator deletes them as STALE) - keep them in recipe/half_slab/')
for m in mats:
    sid = gen.side_id(m['id'])
    loot = written.get(os.path.join(DATA_OUT, 'loot_table', 'blocks', m['id'] + '.json'))
    if not loot or len(loot.get('pools', [])) != gen.LAYERS:
        err(f'loot table missing or not one pool per layer: {m["id"]}')
    side_loot = written.get(os.path.join(DATA_OUT, 'loot_table', 'blocks', sid + '.json'))
    if not side_loot or side_loot['pools'][0]['entries'][0].get('name') != f'{NS}:{sid}':
        err(f'side loot table missing or not dropping the side item: {sid}')
    back = written.get(os.path.join(recipe_dir, 'half_slab', m['slab'].split(':')[1] + '_from_half_slabs.json'))
    if not back or back['result']['id'] != m['slab'] or len(back['ingredients']) != gen.HALVES_PER_SLAB:
        err(f'craft-back recipe of {m["id"]}')
    for src, dst in ((m['id'], sid), (sid, m['id'])):
        conv = written.get(os.path.join(recipe_dir, 'half_slab', f'{dst}_from_{src}.json'))
        if not conv or conv['ingredients'] != [f'{NS}:{src}'] or conv['result']['id'] != f'{NS}:{dst}':
            err(f'1:1 recipe {src} -> {dst}')
for kind in ('block', 'item'):
    tag = written.get(os.path.join(DATA_OUT, 'tags', kind, 'half_slabs.json'))
    if tag is None or len(tag['values']) != 2 * len(mats):
        err(f'{kind} tag {NS}:half_slabs incomplete (half and side half slab of every material)')

# ---- lang
lang_out = out_path(gen.LANG_DIR)
en = json.load(open(os.path.join(lang_out, 'en_us.json'), encoding='utf-8'))
de = json.load(open(os.path.join(lang_out, 'de_de.json'), encoding='utf-8'))
if set(en) != set(de):
    err('EN/DE lang keys differ')
for m in mats:
    sid = gen.side_id(m['id'])
    for k in (f'block.{NS}.{m["id"]}', f'item.{NS}.{m["id"]}', f'block.{NS}.{sid}', f'item.{NS}.{sid}'):
        if not en.get(k) or not de.get(k):
            err(f'lang {k}')
    if not de.get(f'block.{NS}.{m["id"]}', '').lower().endswith(gen.NAME_DE):
        err(f'DE name {m["id"]}')
    if not de.get(f'block.{NS}.{sid}', '').startswith(gen.SIDE_DE) or not en.get(f'block.{NS}.{sid}', '').endswith(gen.SIDE_EN):
        err(f'side name {sid}')

# ---- Java list
java = open(out_path(gen.JAVA_LIST), encoding='utf-8').read()
if java.count('new Material(') != len(mats):
    err('HalfSlabMaterials.java out of date')
layered = open(os.path.join(gen.MODULE, 'shared', 'java', 'com', NS, 'block', 'LayeredSlabBlock.java'), encoding='utf-8').read()
m_layers = re.search(r'static final int LAYERS = (\d+);', layered)
if not m_layers or int(m_layers.group(1)) != gen.LAYERS or 'LAYER_PX = 16 / LAYERS;' not in layered:
    err(f'LayeredSlabBlock.LAYERS/LAYER_PX do not match the generator ({gen.LAYERS} layers of {gen.LAYER_PX} px)')

# ---- suffix-risk sites (source text of the branch): "_half_slab" before "_slab"
def src(rel):
    return open(os.path.join(REPO, rel), encoding='utf-8').read()


def suffix_list(text, name):
    m = re.search(name + r'\s*=\s*(?:List\.of)?\(([^;]*?)\)', text, re.S)
    return re.findall(r'"(_[a-z_]+)"', m.group(1)) if m else None


def strip(path, suffixes, longest=False):
    """The family stem: the first listed ending that matches (or the longest one, like TabSort.shapeIndex)."""
    hits = [s for s in suffixes if path.endswith(s) and len(path) > len(s)]
    if longest:
        hits.sort(key=len, reverse=True)
    return path[:-len(hits[0])] if hits else path


# (file, list, longest ending wins): TabSort.shapeIndex takes the longest match, the others the first listed one.
SITES = [('common/src/shared/java/com/simplebuilding/dev/testcentre/ModBlockGallery.java', 'FORM_SUFFIXES', False),
         ('common/src/shared/java/com/simplebuilding/dev/testcentre/OrderAreas.java', 'ROW_SUFFIXES', False),
         ('modules/simplelib/shared/java/com/simplelib/api/TabSort.java', 'SHAPES', True),
         ('tools/tzedit/build_editor.py', 'SHAPES', False)]
lists = {}
for rel, name, longest in SITES:
    sfx = suffix_list(src(rel), name)
    if not sfx:
        err(f'{rel}: {name} not found')
        continue
    lists[rel] = sfx
    if '_half_slab' not in sfx or '_side_half_slab' not in sfx:
        err(f'{rel}: {name} must list "_side_half_slab" and "_half_slab"')
        continue
    if not longest and not sfx.index('_side_half_slab') < sfx.index('_half_slab') < sfx.index('_slab'):
        err(f'{rel}: {name} must list "_side_half_slab" before "_half_slab" before "_slab"')
        continue
    for m in mats:
        slab_stem = strip(m['slab'].split(':')[1], sfx, longest)
        for form in (m['id'], gen.side_id(m['id'])):
            if strip(form, sfx, longest) != slab_stem:
                err(f'{rel}: {form} lands in family {strip(form, sfx, longest)!r}, its slab in {slab_stem!r}')
                break
oa = src('common/src/shared/java/com/simplebuilding/dev/testcentre/OrderAreas.java')
i_side, i_half, i_slab = (oa.find(f'bySuffix.put("{k}"') for k in ('_side_half_slab', '_half_slab', '_slab'))
if min(i_side, i_half) < 0 or not i_side < i_half < i_slab:
    err('OrderAreas.slot: bySuffix must map "_side_half_slab", then "_half_slab", then "_slab"')
ot = src('modules/simpleoctets/shared/java/com/simpleoctets/test/OctetTests.java')
if 'path.endsWith("_half_slab")' not in ot:
    err('OctetTests.stairsAndSlabsHaveOctets must skip "_half_slab" (else it asks for an octet of "<x>_half")')
# Since the move to Simple Slabs (N86-4) the half slab generator writes the module's own Vanilla tool/vibration tags (the
# octet generator's HALF_SLAB_TAG_REFS of the draft are gone): every half and side half slab with a tool is in mineable/<tool>.
vanilla_tags = out_path(os.path.join(gen.DATA, 'minecraft', 'tags', 'block'))
for m in mats:
    if not m['tool']:
        continue
    tag = written.get(os.path.join(vanilla_tags, 'mineable', m['tool'] + '.json'))
    ids = {v if isinstance(v, str) else v.get('id') for v in (tag or {}).get('values', [])}
    for form in (m['id'], gen.side_id(m['id'])):
        if f'{NS}:{form}' not in ids:
            err(f'{NS}:{form} missing in minecraft:mineable/{m["tool"]}')

print(f'half slabs: {len(mats)} materials (+ side forms), {len(files)} files below {OUT}, {len(checked_models)} block models walked')
if errors:
    print(f'{len(errors)} PROBLEMS:')
    for e in errors[:60]:
        print('  ' + e)
    sys.exit(1)
print('OK')
