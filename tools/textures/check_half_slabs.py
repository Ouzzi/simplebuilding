"""Usage: python -I tools/textures/check_half_slabs.py [OUT_DIR]

N86 half slabs (DRAFT): pure-Python checks of the generator output (half_slabs_2026_11_17.py). OUT_DIR = the tree written with
--out (scratch run); without it the repository itself. Checks: JSON valid; ids unique and new; blockstate -> model -> parent
-> texture references resolve (vanilla by name in the client jar, nothing extracted); item definitions/models; loot, recipe,
tag and lang references; ids of other mods only behind load conditions or as optional tag entries; no PNG written; and the
suffix-risk sites (every "_slab" suffix table must try "_half_slab" first, so a half slab keeps its family).
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
if len(mats) != 165:
    err(f'first-stage scope is 165 (N86-5), list has {len(mats)}')

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


checked_models = set()
for m in mats:
    bs = written.get(os.path.join(ASSETS_OUT, 'blockstates', m['id'] + '.json'))
    if not bs:
        err(f'blockstate missing: {m["id"]}')
        continue
    keys = set(bs['variants'])
    want = {f'half={h},layers={n}' for h in ('bottom', 'top') for n in range(1, gen.MAX_LAYERS + 1)}
    if keys != want:
        err(f'{m["id"]}: blockstate keys {sorted(keys ^ want)}')
    for v in bs['variants'].values():
        if v['model'] not in checked_models:
            checked_models.add(v['model'])
            check_model(v['model'])
    root = out_path(gen.item_root(m['ns']))
    item = os.path.join(root, 'items', m['id'] + '.json')
    if not os.path.isfile(item):
        err(f'item definition missing: {item}')
        continue
    ref = json.load(open(item, encoding='utf-8'))['model']['model']
    check_model(ref, item_root=root)
    im = model_json(ref, root)
    if root != ASSETS_OUT and im and str(im.get('parent', '')).startswith(NS + ':'):
        err(f'partner item model of {m["id"]} has a parent in {NS} (partner may run without it)')

# ---- data references
def known_item(ref):
    ns, path = ref.split(':')
    if ns == 'minecraft':
        return path in van_blocks or f'assets/minecraft/items/{path}.json' in jar
    if ns == NS:
        return path in by_id
    return path in existing.get(ns, set())


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
                                                           'item', 'set_count', 'explosion_decay', 'block_state_property',
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
    loot = written.get(os.path.join(DATA_OUT, 'loot_table', 'blocks', m['id'] + '.json'))
    if not loot:
        err(f'loot table missing: {m["id"]}')
    if not gen.CRAFT_BACK:
        continue
    back = written.get(os.path.join(recipe_dir, 'half_slab', m['slab'].split(':')[1] + '_from_half_slabs.json'))
    if not back or back['result']['id'] != m['slab']:
        err(f'craft-back recipe of {m["id"]}')
tag = written.get(os.path.join(DATA_OUT, 'tags', 'block', 'half_slabs.json'))
if tag is None or len(tag['values']) != len(mats):
    err('tag simpleoctets:half_slabs incomplete')

# ---- lang
lang_out = out_path(gen.LANG_DIR)
en = json.load(open(os.path.join(lang_out, 'en_us.json'), encoding='utf-8'))
de = json.load(open(os.path.join(lang_out, 'de_de.json'), encoding='utf-8'))
if set(en) != set(de):
    err('EN/DE lang keys differ')
for m in mats:
    for k in (f'block.{NS}.{m["id"]}', f'item.{NS}.{m["id"]}'):
        if not en.get(k) or not de.get(k):
            err(f'lang {k}')
    if not de[f'block.{NS}.{m["id"]}'].lower().endswith(gen.NAME_DE):
        err(f'DE name {m["id"]}')

# ---- Java list
java = open(out_path(gen.JAVA_LIST), encoding='utf-8').read()
if java.count('new Material(') != len(mats):
    err('HalfSlabMaterials.java out of date')
for c in ('HEIGHT_PX', 'MAX_LAYERS', 'MERGE_SLAB', 'ONTO_SLAB'):
    if f' {c} = ' not in java:
        err(f'HalfSlabMaterials.{c} missing')

# ---- suffix-risk sites (source text of the branch): "_half_slab" before "_slab"
def src(rel):
    return open(os.path.join(REPO, rel), encoding='utf-8').read()


def suffix_list(text, name):
    m = re.search(name + r'\s*=\s*(?:List\.of)?\(([^;]*?)\)', text, re.S)
    return re.findall(r'"(_[a-z_]+)"', m.group(1)) if m else None


def strip(path, suffixes):
    for s in suffixes:
        if path.endswith(s) and len(path) > len(s):
            return path[:-len(s)]
    return path


SITES = [('common/src/shared/java/com/simplebuilding/dev/testcentre/ModBlockGallery.java', 'FORM_SUFFIXES'),
         ('common/src/shared/java/com/simplebuilding/dev/testcentre/OrderAreas.java', 'ROW_SUFFIXES'),
         ('modules/simplelib/shared/java/com/simplelib/api/TabSort.java', 'SHAPES'),
         ('tools/tzedit/build_editor.py', 'SHAPES')]
lists = {}
for rel, name in SITES:
    sfx = suffix_list(src(rel), name)
    if not sfx:
        err(f'{rel}: {name} not found')
        continue
    lists[rel] = sfx
    if '_half_slab' not in sfx or sfx.index('_half_slab') > sfx.index('_slab'):
        err(f'{rel}: {name} must list "_half_slab" before "_slab"')
        continue
    for m in mats:
        if strip(m['id'], sfx) != strip(m['slab'].split(':')[1], sfx):
            err(f'{rel}: {m["id"]} lands in family {strip(m["id"], sfx)!r}, its slab in {strip(m["slab"].split(":")[1], sfx)!r}')
            break
oa = src('common/src/shared/java/com/simplebuilding/dev/testcentre/OrderAreas.java')
i_half, i_slab = oa.find('bySuffix.put("_half_slab"'), oa.find('bySuffix.put("_slab"')
if i_half < 0 or i_half > i_slab:
    err('OrderAreas.slot: bySuffix must map "_half_slab" before "_slab"')
ot = src('modules/simpleoctets/shared/java/com/simpleoctets/test/OctetTests.java')
if 'path.endsWith("_half_slab")' not in ot:
    err('OctetTests.stairsAndSlabsHaveOctets must skip "_half_slab" (else it asks for an octet of "<x>_half")')
og = src('tools/textures/octets_2026_10_09.py')
if 'HALF_SLAB_TAG_REFS' not in og:
    err('octet generator does not reference the half slab tool/vibration tags (it owns the Vanilla tag files)')

print(f'half slabs: {len(mats)} entries, {len(files)} files below {OUT}, {len(checked_models)} block models walked')
if errors:
    print(f'{len(errors)} PROBLEMS:')
    for e in errors[:60]:
        print('  ' + e)
    sys.exit(1)
print('OK')
