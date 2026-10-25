"""Module-owned data hook: bilingual texts, ids, recipes, unlocks, models, textures, structure and wiki sources of Simple Gadgets."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplegadgets'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplegadgets'
GADGETS = ['velocity_gauge', 'rotator']

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'item.{NS}.{g}' for g in GADGETS]
    need += [f'itemgroup.{NS}.gadgets', f'{NS}.testcentre.station', f'item.{NS}.guide_book']
    for k in need:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for gadget in GADGETS:
    assert (RES / f'assets/{NS}/items/{gadget}.json').is_file(), gadget
    assert (RES / f'assets/{NS}/textures/item/{gadget}.png').is_file(), gadget
    for variant in ('', '_without_simplebuilding'):
        assert (RES / f'data/{NS}/recipe/{gadget}{variant}.json').is_file(), (gadget, variant)
        assert (RES / f'data/{NS}/advancement/recipes/tools/{gadget}{variant}.json').is_file(), (gadget, variant)
for frame in range(17):  # the Gauge's needle frames
    assert (RES / f'assets/{NS}/models/item/velocity_gauge_{frame:02d}.json').is_file(), frame
    assert (RES / f'assets/{NS}/textures/item/velocity_gauge_needle_{frame:02d}.png').is_file(), frame
for path in list((RES / f'data/{NS}').rglob('*.json')) + list((RES / f'assets/{NS}').rglob('*.json')):
    if path.name.endswith('.layers.json') or path.parent.name in ('lang', 'questionary'):
        continue  # prose (guide pages, wiki) may name the old ids
    text = path.read_text(encoding='utf-8').replace('simplebuilding:copper_core', '').replace('simplebuilding:iron_core', '').replace('simplebuilding:iron_pole', '')  # with SimpleBuilding: its cores and rods (conditional)
    assert 'simplebuilding:' not in text, path
for part in ('rotator', 'rotator_empty', 'rotator_active'):  # the Transposer's models and textures
    assert (RES / f'assets/{NS}/models/item/{part}.json').is_file(), part
    assert (RES / f'assets/{NS}/textures/item/{part}.png').is_file(), part
for tag in ('minecraft/tags/item/enchantable/durability', 'minecraft/tags/item/enchantable/mining', 'simplelib/tags/item/xp_repair_incompatible'):
    assert 'simplegadgets:rotator' in (RES / f'data/{tag}.json').read_text(encoding='utf-8'), tag
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for f in list(manual['features']) + list(manual['notes'].values()):
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
print(f'{NS}: {len(GADGETS)} gadgets, {len(used)} UI texts, recipes, unlocks, models, textures, structure and wiki valid')
