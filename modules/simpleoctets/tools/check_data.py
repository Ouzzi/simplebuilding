"""Module-owned data hook of Simple Octets: bilingual texts, every cell's resources and recipes, ids of other mods only
behind load conditions, mixins, structure and wiki sources (split stage 6, docs/ai/PLAN-S6-OCTETS-2026-10-17.md)."""
import json
import re
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simpleoctets'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpleoctets'
ASSETS = RES / f'assets/{NS}'
DATA = RES / f'data/{NS}'

materials = json.loads((ROOT / 'tools/textures/octet_materials.json').read_text(encoding='utf-8'))
woods = re.findall(r'"([a-z_]+)"', re.search(r'WOODS = List\.of\(([^;]*)\);', (JAVA / 'registry/OctetRegistry.java').read_text(encoding='utf-8')).group(1))
assert len(woods) == 15 and woods[0] == 'oak', woods
java_list = (JAVA / 'registry/OctetMaterials.java').read_text(encoding='utf-8')
assert java_list.count('new Material(') == len(materials), 'OctetMaterials.java out of date (run tools/textures/octets_2026_10_09.py)'
cells = [w + '_octet' for w in woods] + [m['key'] + '_octet' for m in materials]
assert len(set(cells)) == len(cells), 'duplicate cell ids'

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'{p}.{NS}.{c}' for c in cells for p in ('block', 'item')]
    need += [f'block.{NS}.melon_octet', f'block.{NS}.mixed_octet', f'itemgroup.{NS}.octets', f'{NS}.testcentre.station',
             f'item.{NS}.guide_book', f'jei.{NS}.info.octets', f'jei.{NS}.info.melon_octet']
    for k in need:
        assert data.get(k), (locale, k)
    for k in (f'tooltip.{NS}.material_octet', f'tooltip.{NS}.melon_slice.place'):
        assert len(data[k]) <= 45, ('tooltip line too long', locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

# Octets of another mod's block keep item definition + self-contained item model in that mod's jar (generator PARTNER_RES:
# the 26.3 client bakes every items/*.json, a missing partner would leave missing textures in the client log).
PARTNER_RES = {'simpleend': ROOT / 'modules/simpleend/shared/resources', 'simpleblocks': ROOT / 'modules/simpleblocks/shared/resources',
               'simplebuilding': ROOT / 'mc26_3/overlay/resources'}
cell_ns = {w + '_octet': 'simpleend' if w in ('ashwood', 'duskwood') else 'minecraft' for w in woods}
cell_ns.update({m['key'] + '_octet': m['ns'] for m in materials})
for c in cells:
    assert (ASSETS / f'blockstates/{c}.json').is_file(), ('blockstate', c)
    item_assets = PARTNER_RES[cell_ns[c]] / f'assets/{NS}' if cell_ns[c] in PARTNER_RES else ASSETS
    assert (item_assets / f'items/{c}.json').is_file(), ('item definition', c)
    assert (item_assets / f'models/item/octet/{c}.json').is_file(), ('item model', c)
    if item_assets != ASSETS:
        assert not (ASSETS / f'items/{c}.json').exists(), ('partner item definition left in the module', c)
        assert 'parent' not in json.loads((item_assets / f'models/item/octet/{c}.json').read_text(encoding='utf-8')) or \
            not json.loads((item_assets / f'models/item/octet/{c}.json').read_text(encoding='utf-8'))['parent'].startswith(NS), c
    assert (ASSETS / f'models/block/octet/{c[:-6]}.json').is_file(), ('corner model', c)
for b in ('melon_octet', 'mixed_octet'):
    assert (ASSETS / f'blockstates/{b}.json').is_file(), b
    assert not (ASSETS / f'items/{b}.json').exists(), f'{b} has no item'
assert (ASSETS / 'textures/block/melon_flesh.png').is_file()

recipes = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (DATA / 'recipe').glob('*.json')}
assert 'recipes/building_blocks/' in (JAVA / 'SimpleOctets.java').read_text(encoding='utf-8')
foreign = {m['key'] + '_octet': m['ns'] for m in materials if m['ns'] != 'minecraft'}
foreign.update({w + '_octet': 'simpleend' for w in woods if w in ('ashwood', 'duskwood')})
pairs = [(w + '_octet', 'simpleend' if w in ('ashwood', 'duskwood') else 'minecraft', w + '_planks') for w in woods]
pairs += [(m['key'] + '_octet', m['ns'], m['base']) for m in materials]
def mods_in(conds, key, field):
    """Mod ids a condition list requires (plain mod_loaded) and forbids (inside a not)."""
    ids = lambda c: set(c['values']) if 'values' in c else {c['modid']}
    need, never = set(), set()
    for c in conds:
        if c[key].endswith(':not'):
            never |= ids(c['value'])
        else:
            need |= ids(c)
    return need, never


# Octets exclusive to the Shapecutter (docs/ai/PRINZIPIEN.md A4b): simpleblocks:sawing only with Simple Blocks, a
# minecraft:stonecutting fallback only without it (none for Simple Blocks' own blocks), crafting back always.
sawing = 0
for c, base_ns, base in pairs:
    saw, cut, back = f'{c}_from_{base}_sawing', f'{c}_from_{base}_stonecutting', f'{base}_from_octets'
    foreign_mod = set() if base_ns in ('minecraft', 'simplelib') else {base_ns}
    assert saw in recipes and back in recipes, ('recipes', c)
    assert (cut in recipes) == (base_ns != 'simpleblocks'), ('stonecutter fallback', c)
    assert recipes[saw]['type'] == 'simpleblocks:sawing' and recipes[saw]['result'] == {'count': 8, 'id': f'{NS}:{c}'} \
        and recipes[saw]['ingredient'] == f'{base_ns}:{base}', saw
    assert not (DATA / f'advancement/recipes/building_blocks/{saw}.json').exists(), ('the saw has no recipe book', saw)
    sawing += 1
    expect = {saw: (foreign_mod | {'simpleblocks'}, set()), back: (foreign_mod, set())}
    if cut in recipes:
        assert recipes[cut]['type'] == 'minecraft:stonecutting' and recipes[cut]['result'] == {'count': 8, 'id': f'{NS}:{c}'} \
            and recipes[cut]['ingredient'] == f'{base_ns}:{base}', cut
        expect[cut] = (foreign_mod, {'simpleblocks'})
    assert recipes[back]['ingredients'] == [f'{NS}:{c}'] * 8 and recipes[back]['result'] == {'id': f'{base_ns}:{base}'}, back
    for name, (need, never) in expect.items():
        data = recipes[name]
        assert mods_in(data.get('fabric:load_conditions', []), 'condition', 'values') == (need, never), ('fabric conditions', name)
        assert mods_in(data.get('neoforge:conditions', []), 'type', 'modid') == (need, never), ('neoforge conditions', name)
        forge = data.get('forge:condition')
        forge = [] if forge is None else forge['values'] if forge['type'] == 'forge:and' else [forge]
        assert mods_in(forge, 'type', 'modid') == (need, never), ('forge condition', name)
        if name != saw:
            adv = DATA / f'advancement/recipes/building_blocks/{name}.json'
            assert adv.is_file(), ('recipe advancement', name)
            adv_data = json.loads(adv.read_text(encoding='utf-8'))
            assert adv_data.get('fabric:load_conditions') == data.get('fabric:load_conditions'), ('advancement condition', name)
assert not [n for n, r in recipes.items() if r['type'] == 'minecraft:stonecutting' and 'fabric:load_conditions' not in r], \
    'a stonecutter recipe without "simpleblocks not loaded"'
assert len(recipes) == 3 * len(cells) - sum(1 for _, ns, _ in pairs if ns == 'simpleblocks') + (1 if 'guide_book' in recipes else 0), \
    (len(recipes), len(cells))

# Other mods' ids only behind load conditions or as optional tag entries.
for path in (RES / 'data').rglob('*.json'):
    text = path.read_text(encoding='utf-8')
    for ns in ('simplebuilding', 'simpleend', 'simpleblocks'):
        if f'"{ns}:' in text:
            data = json.loads(text)
            ok = 'fabric:load_conditions' in data or all(isinstance(v, dict) and v.get('required') is False
                                                         for v in data.get('values', []) if f'{ns}:' in json.dumps(v))
            assert ok, ('unconditional foreign id', path, ns)
for tool in ('axe', 'pickaxe'):
    values = json.loads((RES / f'data/minecraft/tags/block/mineable/{tool}.json').read_text(encoding='utf-8'))['values']
    for v in values:
        vid = v if isinstance(v, str) else v['id']
        assert vid.split(':')[1] in cells + ['melon_octet'], ('tag entry', tool, vid)
        if vid.split(':')[1] in foreign:
            assert isinstance(v, dict) and v['required'] is False, ('optional tag entry', vid)

mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
for name in mixins['mixins'] + mixins.get('client', []):
    assert (JAVA / 'mixin' / f'{name}.java').is_file(), name
assert (DATA / 'structure/empty.nbt').is_file()

manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for entry in list(manual['features']) + list(manual['notes'].values()):
    for source in entry.get('sources', []):
        assert (ROOT / source).exists(), ('wiki source', source)
    for locale in ('en', 'de'):
        assert entry[locale].get('summary'), ('wiki text', locale)
for key in ('simpleoctets:*_octet', 'simpleoctets:melon_octet', 'simpleoctets:mixed_octet'):
    assert key in manual['notes'], key

gen = subprocess.run([sys.executable, str(ROOT / 'tools/textures/octets_2026_10_09.py'), '--check'], capture_output=True, text=True)
assert gen.returncode == 0, 'octet resources out of date: ' + gen.stdout + gen.stderr
print(f'Simple Octets data: {len(cells)} cells ({len(woods)} woods, {len(materials)} materials), {len(recipes)} recipes '
      f'({sawing} Shapecutter, stonecutter only without Simple Blocks), '
      f'{len(foreign)} conditional on other mods, bilingual texts, wiki complete')
