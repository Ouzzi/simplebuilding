"""Simple Pads data invariants (b-pads01): lang EN/DE in step, every block has blockstate, item definition, loot and is mineable,
the data names no simplebuilding: id (the module runs without SimpleBuilding), the Enderite tiers use Simple Ores' template,
config keys are documented, wiki sources exist."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplepads'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplepads'

en = json.loads((RES / f'assets/{NS}/lang/en_us.json').read_text(encoding='utf-8'))
de = json.loads((RES / f'assets/{NS}/lang/de_de.json').read_text(encoding='utf-8'))
assert set(en) == set(de), sorted(set(en) ^ set(de))
blocks = re.findall(r'register\("([a-z0-9_]+)"', (JAVA / 'block/PadsBlocks.java').read_text(encoding='utf-8'))
assert len(blocks) == 35, len(blocks)
pickaxe = json.loads((RES / 'data/minecraft/tags/block/mineable/pickaxe.json').read_text(encoding='utf-8'))['values']
for b in blocks:
    assert (RES / f'assets/{NS}/blockstates/{b}.json').is_file(), b
    assert (RES / f'assets/{NS}/items/{b}.json').is_file(), b
    assert (RES / f'data/{NS}/loot_table/blocks/{b}.json').is_file(), b
    assert f'{NS}:{b}' in pickaxe, b
    assert f'block.{NS}.{b}' in en, b
for item in ('spawn_elytra', 'funny_stick'):
    assert (RES / f'assets/{NS}/items/{item}.json').is_file(), item
    assert f'item.{NS}.{item}' in en, item
# every model/texture an asset names exists in the module
for path in (RES / f'assets/{NS}').rglob('*.json'):
    if path.name.endswith('.layers.json') or path.parent.name == 'lang':
        continue
    text = path.read_text(encoding='utf-8')
    for kind, ref in re.findall(rf'"{NS}:(block|item)/([a-z0-9_/]+)"', text):
        assert (RES / f'assets/{NS}/models/{kind}/{ref}.json').is_file() or (RES / f'assets/{NS}/textures/{kind}/{ref}.png').is_file(), (path.name, ref)
# alone playable: no SimpleBuilding id in the data (ingredients: Vanilla, SimpleLib, Simple Ores, Simple Mobheads)
for path in list((RES / f'data/{NS}').rglob('*.json')) + list((RES / f'assets/{NS}').rglob('*.json')):
    if path.name.endswith('.layers.json') or path.parent.name == 'lang':
        continue
    assert 'simplebuilding:' not in path.read_text(encoding='utf-8'), path
for path in (RES / f'data/{NS}/recipe').rglob('*.json'):
    for ing in re.findall(r'"#?([a-z0-9_]+):[a-z0-9_/]+"', path.read_text(encoding='utf-8')):
        assert ing in ('minecraft', 'simplelib', 'simpleores', 'simplemobheads', NS), (path.name, ing)
enderite = [p for p in (RES / f'data/{NS}/recipe').glob('enderite_*.json')]
assert enderite and all('simpleores:enderite_upgrade_template' in p.read_text(encoding='utf-8') for p in enderite), 'Enderite tiers need Simple Ores'
# config keys: every key has its EN/DE texts and is named on the wiki page
cfg = (JAVA / 'PadsConfig.java').read_text(encoding='utf-8')
keys = re.findall(r'"([A-Za-z0-9.]+)"', re.search(r'KEYS = List\.of\(([^)]*)\)', cfg).group(1))
for key in keys:
    assert f'text.autoconfig.{NS}.option.{key}' in en and f'text.autoconfig.{NS}.option.{key}.@Tooltip' in en, key
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for f in list(manual['features']) + list(manual['notes'].values()):
    for src in f.get('sources', []):
        assert (ROOT / src).is_file(), src
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
print(f'{NS}: {len(blocks)} blocks, lang, models, loot, recipes, config texts and wiki valid')
