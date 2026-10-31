"""Module-owned data hook: bilingual texts, config keys in the wiki, loot tables, recipes, worldgen numbers, derived textures."""
import json
import re
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simpleores'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpleores'
sys.path.insert(0, str(ROOT / 'tools/textures'))
import vanilla_derived  # noqa: E402

keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/OresConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert config_keys == ['enabled', 'soundproofing', 'soundproofFactor', 'soundproofThreshold', 'soundproofRecords', 'silverArrowUndeadBonus'], config_keys
BLOCKS = ('silver_ore', 'deepslate_silver_ore', 'end_silver_ore', 'raw_silver_block', 'silver_block', 'lead_ore', 'deepslate_lead_ore', 'raw_lead_block', 'lead_block',
          'silver_mirror', 'rose_bronze_block', 'blossom_gold_block')
ALLOYS = ('rose_bronze', 'blossom_gold')
GEAR = ('sword', 'pickaxe', 'axe', 'shovel', 'hoe', 'helmet', 'chestplate', 'leggings', 'boots')
ITEMS = ('raw_silver', 'silver_ingot', 'silver_nugget', 'raw_lead', 'lead_ingot', 'lead_nugget', 'silver_arrow') + tuple(f'{a}_{p}' for a in ALLOYS for p in ('ingot', 'pressling') + GEAR)
assert not any(i.startswith('silver_') and i.rsplit('_', 1)[-1] in GEAR for i in ITEMS), 'silver is no tool material (ABN30-10)'
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in [f'block.{NS}.{b}' for b in BLOCKS] + [f'item.{NS}.{i}' for i in ITEMS] + [f'item.{NS}.guide_book', f'{NS}.testcentre.station']:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

# Models and definitions for everything; loot table for every block; the derived textures are listed (never committed).
derived = vanilla_derived.MODULE_OUTPUTS[NS]
for b in BLOCKS:
    for rel in (f'assets/{NS}/blockstates/{b}.json', f'assets/{NS}/models/block/{b}.json', f'assets/{NS}/items/{b}.json', f'data/{NS}/loot_table/blocks/{b}.json'):
        assert (RES / rel).is_file(), rel
    assert f'block/{b}.png' in derived, b
    assert not (RES / f'assets/{NS}/textures/block/{b}.png').exists(), f'{b} texture is Vanilla-derived, not committed'
for i in ITEMS:
    for rel in (f'assets/{NS}/models/item/{i}.json', f'assets/{NS}/items/{i}.json'):
        assert (RES / rel).is_file(), rel
    assert f'item/{i}.png' in derived, i
for a in ALLOYS:
    assert (RES / f'assets/{NS}/equipment/{a}.json').is_file()
    for layer in ('humanoid', 'humanoid_leggings'):
        assert f'entity/equipment/{layer}/{a}.png' in derived, (a, layer)
assert len(derived) == len(BLOCKS) + len(ITEMS) + 4 + 1, len(derived)   # + the silver arrow entity texture
assert 'entity/projectiles/silver_arrow.png' in derived

# Pressing recipes: shapeless, 1 pressing from 7 to 9 ingots; the loss is in the recipe. Only the blast furnace melts a pressing,
# 600 ticks in the blast furnace (the recipe stores the furnace time, which the blast furnace halves), exactly 1 ingot.
for a in ALLOYS:
    blast = json.loads((RES / f'data/{NS}/recipe/{a}_ingot_from_blasting_{a}_pressling.json').read_text(encoding='utf-8'))
    assert blast['type'] == 'minecraft:blasting' and blast['cookingtime'] == 1200 and blast['result'] == {'id': f'{NS}:{a}_ingot'}, a
    assert blast['ingredient'] == f'{NS}:{a}_pressling'
    for other in ('smelting', 'smoking', 'campfire_cooking'):
        for p in (RES / f'data/{NS}/recipe').glob('*.json'):
            r = json.loads(p.read_text(encoding='utf-8'))
            assert not (r.get('type') == f'minecraft:{other}' and r.get('ingredient') == f'{NS}:{a}_pressling'), p.name
pressings = [p for p in (RES / f'data/{NS}/recipe').glob('*_pressling_*.json') if 'blasting' not in p.name]
assert len(pressings) == 5, pressings
for p in pressings:
    r = json.loads(p.read_text(encoding='utf-8'))
    assert r['type'] == 'minecraft:crafting_shapeless' and r['result'].get('count', 1) == 1 and len(r['ingredients']) in (7, 8, 9), p.name
    assert 'minecraft:copper_ingot' in r['ingredients'] and r['ingredients'].count('minecraft:gold_ingot') >= 4, p.name
    assert any(i.startswith(f'{NS}:silver_') for i in r['ingredients']), p.name
for p in (RES / f'data/{NS}/recipe').glob('*.json'):
    assert any((RES / f'data/{NS}/advancement/recipes').glob(f'*/{p.name}')), f'no recipe unlock for {p.name}'

# Lead: 6 veins of 9 per chunk between Y -64 and 48 (more than silver, fewer ore blocks than Vanilla copper), explosion-proof block that occludes
# vibrations (like wool) but does not dampen them, tag of the muffling blocks, the silver arrow is an arrow, the mirror needs no tool to drop.
lead_placed = json.loads((RES / f'data/{NS}/worldgen/placed_feature/lead_ore.json').read_text(encoding='utf-8'))
assert next(m for m in lead_placed['placement'] if m['type'] == 'minecraft:count')['count'] == 6
lead_height = next(m for m in lead_placed['placement'] if m['type'] == 'minecraft:height_range')['height']
assert lead_height['min_inclusive']['absolute'] == -64 and lead_height['max_inclusive']['absolute'] == 48
assert json.loads((RES / f'data/{NS}/worldgen/feature/lead_ore.json').read_text(encoding='utf-8'))['size'] == 9
mc = RES / 'data/minecraft/tags'
assert f'{NS}:lead_block' in json.loads((mc / 'block/occludes_vibration_signals.json').read_text(encoding='utf-8'))['values']
assert not (mc / 'block/dampens_vibrations.json').exists(), 'lead must NOT dampen vibrations'
assert json.loads((RES / f'data/{NS}/tags/block/soundproofing.json').read_text(encoding='utf-8'))['values'] == [f'{NS}:lead_block']
assert f'{NS}:silver_arrow' in json.loads((mc / 'item/arrows.json').read_text(encoding='utf-8'))['values']
assert f'{NS}:silver_arrow' in json.loads((mc / 'entity_type/arrows.json').read_text(encoding='utf-8'))['values']
arrow = json.loads((RES / f'data/{NS}/recipe/silver_arrow.json').read_text(encoding='utf-8'))
assert arrow['pattern'] == ['N', 'S', 'F'] and arrow['result'] == {'id': f'{NS}:silver_arrow', 'count': 4}
for lang in langs.values():
    for key in config_keys:
        assert lang.get(f'text.autoconfig.{NS}.option.{key}') and lang.get(f'text.autoconfig.{NS}.option.{key}.@Tooltip'), key
    for key in config_keys:
        assert ('Default:' in lang[f'text.autoconfig.{NS}.option.{key}.@Tooltip']) or ('Standard:' in lang[f'text.autoconfig.{NS}.option.{key}.@Tooltip']), key
mixins = json.loads((RES / 'simpleores.mixins.json').read_text(encoding='utf-8'))
assert mixins['mixins'] == ['SoundproofMixin'] and (JAVA / 'mixin/SoundproofMixin.java').is_file()
mod = json.loads((MODULE / 'fabric/src/main/resources/fabric.mod.json').read_text(encoding='utf-8'))
assert mod['mixins'] == ['simpleores.mixins.json'] and 'client' in mod['entrypoints']

# World generation: silver 1 to 2 veins of 5 per chunk (Overworld, Y -48..16), rarer than gold (4 veins of 9); End silver on the outer islands.
placed = json.loads((RES / f'data/{NS}/worldgen/placed_feature/silver_ore.json').read_text(encoding='utf-8'))
count = next(m for m in placed['placement'] if m['type'] == 'minecraft:count')['count']
assert (count['min_inclusive'], count['max_inclusive']) == (1, 2), count
feature = json.loads((RES / f'data/{NS}/worldgen/feature/silver_ore.json').read_text(encoding='utf-8'))
assert feature['size'] == 5 and len(feature['targets']) == 2
height = next(m for m in placed['placement'] if m['type'] == 'minecraft:height_range')['height']
assert height['min_inclusive']['absolute'] == -48 and height['max_inclusive']['absolute'] == 16
end = json.loads((RES / f'data/{NS}/worldgen/placed_feature/end_silver_ore.json').read_text(encoding='utf-8'))
assert end['feature'] == f'{NS}:end_silver_ore'
for name, biomes in (('silver_ore', '#minecraft:is_overworld'), ('end_silver_ore', f'#{NS}:has_end_silver_ore')):
    mod = json.loads((RES / f'data/{NS}/neoforge/biome_modifier/{name}.json').read_text(encoding='utf-8'))
    assert mod['biomes'] == biomes and mod['features'] == f'{NS}:{name}' and mod['step'] == 'underground_ores', name
fabric = (MODULE / 'fabric/src/main/java/com/simplebuilding/modules/simpleores/OresFabric.java').read_text(encoding='utf-8')
assert 'foundInOverworld' in fabric and 'has_end_silver_ore' in fabric
tags = json.loads((RES / f'data/{NS}/tags/worldgen/biome/has_end_silver_ore.json').read_text(encoding='utf-8'))['values']
assert 'minecraft:the_end' not in tags and 'minecraft:end_highlands' in tags, 'End silver lies on the outer islands only'

# Wiki: config keys, sources exist.
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config = next(f for f in manual['features'] if f['id'] == 'config_server')
for locale in ('en', 'de'):
    text = ' '.join(config[locale]['details'])
    for k in config_keys:
        assert k in text, ('wiki config lacks', locale, k)
for f in manual['features']:
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
for note, entry in manual['notes'].items():
    for src in entry['sources']:
        assert (ROOT / src).is_file(), (note, src)
assert {f'{NS}:{i}' for i in ITEMS} | {f'{NS}:{b}' for b in BLOCKS} | {f'{NS}:guide_book'} <= set(manual['notes'])
assert {'lead', 'soundproofing', 'silver_arrow', 'silver_mirror'} <= {f['id'] for f in manual['features']}
assert (RES / f'data/{NS}/structure/arena.nbt').is_file()
import subprocess
subprocess.run([sys.executable, str(MODULE / 'tools/gen_data.py'), '--check'], check=True)
print('simpleores: bilingual texts, loot, recipes, worldgen numbers, derived textures and wiki data valid')
