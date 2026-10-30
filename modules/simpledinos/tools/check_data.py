"""Module-owned data hook: bilingual texts, config keys in the wiki, loot tables, recipes, worldgen, tags, texture and wiki sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simpledinos'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpledinos'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/DinosConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert config_keys == ['enabled', 'brushSeconds', 'incubateSeconds', 'hatchSeconds', 'growMinutes', 'trexBiteDamage'], config_keys
KINDS = ('trex', 'triceratops', 'stegosaurus', 'utahraptor', 'parasaurolophus')
EGGS = tuple(f'{k}_egg' for k in KINDS)
ARMORS = ('trex_armor_leather', 'trex_armor_iron', 'trex_armor_diamond')
BLOCKS = ('fossil_block',) + EGGS
ITEMS = BLOCKS + ('large_saddle',) + ARMORS + ('guide_book',)
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in ([f'entity.{NS}.trex'] + [f'block.{NS}.{b}' for b in BLOCKS] + [f'item.{NS}.{i}' for i in ITEMS if i not in BLOCKS]
              + [f'{NS}.testcentre.station', f'{NS}.egg.brooded', f'{NS}.egg.occupied']):
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

# Every block has a loot table; the fossil drops itself with Silk Touch and bones otherwise, an egg always drops itself.
for b in BLOCKS:
    table = json.loads((RES / f'data/{NS}/loot_table/blocks/{b}.json').read_text(encoding='utf-8'))
    assert table['type'] == 'minecraft:block', b
fossil = json.dumps(json.loads((RES / f'data/{NS}/loot_table/blocks/fossil_block.json').read_text(encoding='utf-8')))
assert 'minecraft:tool/can_silk_touch' in fossil and 'minecraft:bone' in fossil
# Recipes with their unlocking advancement.
for r in ('large_saddle',) + ARMORS:
    recipe = json.loads((RES / f'data/{NS}/recipe/{r}.json').read_text(encoding='utf-8'))
    assert recipe['result']['id'] == f'{NS}:{r}', r
    assert (RES / f'data/{NS}/advancement/recipes/equipment/{r}.json').is_file(), r
assert json.loads((RES / f'data/{NS}/recipe/trex_armor_iron.json').read_text(encoding='utf-8'))['key']['B'] == 'minecraft:bone_block'
# World generation: configured + placed feature, biome tag, NeoForge modifier; Fabric reads the tag.
for p in ('worldgen/configured_feature', 'worldgen/placed_feature'):
    assert (RES / f'data/{NS}/{p}/fossil_block.json').is_file(), p
assert (RES / f'data/{NS}/tags/worldgen/biome/has_fossil_block.json').is_file()
mod = json.loads((RES / f'data/{NS}/neoforge/biome_modifier/fossil_block.json').read_text(encoding='utf-8'))
assert mod['features'] == f'{NS}:fossil_block' and mod['step'] == 'underground_ores' and mod['biomes'] == f'#{NS}:has_fossil_block'
# Entity type tags: who may wear the large saddle and the armor.
for t in ('can_equip_large_saddle', 'can_wear_dino_armor'):
    assert json.loads((RES / f'data/{NS}/tags/entity_type/{t}.json').read_text(encoding='utf-8'))['values'] == [f'{NS}:trex'], t
# The pickaxe mines the fossil block.
assert f'{NS}:fossil_block' in json.loads((RES / 'data/minecraft/tags/block/mineable/pickaxe.json').read_text(encoding='utf-8'))['values']
# Textures: fossil stages, egg bases and overlays, entity textures per armor tier, items; and the matching models/blockstates.
tex = RES / f'assets/{NS}/textures'
for s in range(10):
    assert (tex / f'block/fossil_{s}.png').is_file() and (RES / f'assets/{NS}/models/block/fossil_{s}.json').is_file(), s
for k in KINDS:
    assert (tex / f'block/egg_{k}.png').is_file(), k
    assert (RES / f'assets/{NS}/blockstates/{k}_egg.json').is_file() and (RES / f'assets/{NS}/items/{k}_egg.json').is_file(), k
    for p in range(15):
        assert (RES / f'assets/{NS}/models/block/{k}_egg_{p}.json').is_file(), (k, p)
for p in range(15):
    assert (tex / f'block/egg_overlay_{p}.png').is_file(), p
for t in ('plain', 'leather', 'iron', 'diamond'):
    assert (tex / f'entity/trex/{t}.png').is_file(), t
for i in ('large_saddle',) + ARMORS + ('guide_book',):
    assert (tex / f'item/{i}.png').is_file(), i
assert (RES / f'data/{NS}/structure/arena.nbt').is_file()
# Generated files are current (models, blocks).
import subprocess, sys
for script in ('gen_models.py', 'gen_blocks.py'):
    r = subprocess.run([sys.executable, str(MODULE / 'tools' / script), '--check'], capture_output=True, text=True)
    assert r.returncode == 0, (script, r.stdout.strip())
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
for i in ITEMS:
    assert f'{NS}:{i}' in manual['notes'], ('wiki note missing', i)
print('simpledinos: bilingual texts, loot, recipes, worldgen, tags, textures and wiki data valid')
