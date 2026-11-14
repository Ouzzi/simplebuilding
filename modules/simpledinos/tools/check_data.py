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
assert config_keys == ['enabled', 'brushSeconds', 'incubateSeconds', 'hatchSeconds', 'hatchWaitsForOwner', 'growMinutes', 'trexBiteDamage',
                       'triceratopsShake', 'shakeShare', 'shakeAppleChance', 'shakeTreeCooldownSeconds', 'ramCooldownSeconds',
                       'stegosaurusPlowWidth', 'stegosaurusSow', 'callRadius', 'callSeconds', 'callMaxAnimals', 'callCooldownSeconds',
                       'callIgnoreNamed', 'raptorClimbSeconds', 'raptorFeatherMinutes'], config_keys
KINDS = ('trex', 'triceratops', 'stegosaurus', 'utahraptor', 'parasaurolophus')
EGGS = tuple(f'{k}_egg' for k in KINDS)
ARMORS = ('trex_armor_leather', 'trex_armor_iron', 'trex_armor_diamond')
TRI_ARMORS = ('triceratops_armor_leather', 'triceratops_armor_iron', 'triceratops_armor_diamond')  # a-dinorest1
STEGO_ARMORS = ('stegosaurus_armor_leather', 'stegosaurus_armor_iron', 'stegosaurus_armor_diamond')  # a-dinorest2
PARA_ARMORS = ('parasaurolophus_armor_leather', 'parasaurolophus_armor_iron', 'parasaurolophus_armor_diamond')  # a-dinorest3
RAPTOR_ARMORS = ('utahraptor_armor_leather', 'utahraptor_armor_iron', 'utahraptor_armor_diamond')  # b-utah48
SHELLS = tuple(f'{k}_eggshell' for k in KINDS if k != 'trex')
ENTITIES = ('trex', 'triceratops', 'stegosaurus', 'parasaurolophus', 'utahraptor')
BLOCKS = ('fossil_block',) + EGGS
ITEMS = BLOCKS + ('large_saddle',) + ARMORS + TRI_ARMORS + STEGO_ARMORS + ('dino_plow',) + PARA_ARMORS + RAPTOR_ARMORS + ('raptor_feather',) + SHELLS + ('guide_book',)
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in ([f'entity.{NS}.{e}' for e in ENTITIES] + [f'block.{NS}.{b}' for b in BLOCKS] + [f'item.{NS}.{i}' for i in ITEMS if i not in BLOCKS]
              + [f'{NS}.testcentre.station', f'{NS}.egg.brooded', f'{NS}.egg.occupied', f'tooltip.{NS}.dino_plow']):
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

# Every block has a loot table; the fossil drops itself with Silk Touch and bones otherwise, an egg always drops itself.
for b in BLOCKS:
    table = json.loads((RES / f'data/{NS}/loot_table/blocks/{b}.json').read_text(encoding='utf-8'))
    assert table['type'] == 'minecraft:block', b
fossil = json.dumps(json.loads((RES / f'data/{NS}/loot_table/blocks/fossil_block.json').read_text(encoding='utf-8')))
assert 'minecraft:tool/can_silk_touch' in fossil and 'minecraft:bone' in fossil
# Recipes with their unlocking advancement.
for r in ('large_saddle',) + ARMORS + TRI_ARMORS + STEGO_ARMORS + ('dino_plow',) + PARA_ARMORS + RAPTOR_ARMORS:
    recipe = json.loads((RES / f'data/{NS}/recipe/{r}.json').read_text(encoding='utf-8'))
    assert recipe['result']['id'] == f'{NS}:{r}', r
    assert (RES / f'data/{NS}/advancement/recipes/equipment/{r}.json').is_file(), r
assert json.loads((RES / f'data/{NS}/recipe/trex_armor_iron.json').read_text(encoding='utf-8'))['key']['B'] == 'minecraft:bone_block'
# each dinosaur its own armor: the Triceratops armor has its eggshell in the middle (concept DINO36 6)
for r in TRI_ARMORS:
    assert json.loads((RES / f'data/{NS}/recipe/{r}.json').read_text(encoding='utf-8'))['key']['S'] == f'{NS}:triceratops_eggshell', r
for r in STEGO_ARMORS:
    assert json.loads((RES / f'data/{NS}/recipe/{r}.json').read_text(encoding='utf-8'))['key']['S'] == f'{NS}:stegosaurus_eggshell', r
for r in PARA_ARMORS:
    assert json.loads((RES / f'data/{NS}/recipe/{r}.json').read_text(encoding='utf-8'))['key']['S'] == f'{NS}:parasaurolophus_eggshell', r
for r in RAPTOR_ARMORS:
    assert json.loads((RES / f'data/{NS}/recipe/{r}.json').read_text(encoding='utf-8'))['key']['S'] == f'{NS}:utahraptor_eggshell', r
# Utahraptor (b-utah48): one raptor feather gives three Vanilla feathers; meat as food; ice and glass give no hold
feather = json.loads((RES / f'data/{NS}/recipe/feather_from_raptor_feather.json').read_text(encoding='utf-8'))
assert feather['ingredients'] == [f'{NS}:raptor_feather'] and feather['result'] == {'id': 'minecraft:feather', 'count': 3}, feather
assert (RES / f'data/{NS}/advancement/recipes/misc/feather_from_raptor_feather.json').is_file()
assert json.loads((RES / f'data/{NS}/tags/item/utahraptor_food.json').read_text(encoding='utf-8'))['values'] == ['#minecraft:meat']
unclimbable = json.loads((RES / f'data/{NS}/tags/block/raptor_unclimbable.json').read_text(encoding='utf-8'))['values']
assert {'minecraft:ice', 'minecraft:packed_ice', 'minecraft:blue_ice', 'minecraft:glass', 'minecraft:glass_pane'} <= {v for v in unclimbable if isinstance(v, str)}, unclimbable
assert all(v['required'] is False for v in unclimbable if isinstance(v, dict)), unclimbable
# Dino Plow (concept DINO36 6): I K I / H H H, enchantable with Unbreaking and Mending
plow = json.loads((RES / f'data/{NS}/recipe/dino_plow.json').read_text(encoding='utf-8'))
assert plow['pattern'] == ['IKI', 'HHH'] and plow['key'] == {'I': 'minecraft:iron_ingot', 'K': 'minecraft:iron_chain', 'H': 'minecraft:iron_hoe'}, plow
assert f'{NS}:dino_plow' in json.loads((RES / 'data/minecraft/tags/item/enchantable/durability.json').read_text(encoding='utf-8'))['values']
for s in SHELLS:
    recipe = json.loads((RES / f'data/{NS}/recipe/bone_meal_from_{s}.json').read_text(encoding='utf-8'))
    assert recipe['ingredients'] == [f'{NS}:{s}'] and recipe['result'] == {'id': 'minecraft:bone_meal', 'count': 3}, s
    assert (RES / f'data/{NS}/advancement/recipes/misc/bone_meal_from_{s}.json').is_file(), s
# Stegosaurus: food (fern first, the favourite) and the seeds the plow sows
assert json.loads((RES / f'data/{NS}/tags/item/stegosaurus_food.json').read_text(encoding='utf-8'))['values'][0] == 'minecraft:fern'
assert 'minecraft:wheat_seeds' in json.loads((RES / f'data/{NS}/tags/item/plow_seeds.json').read_text(encoding='utf-8'))['values']
# Parasaurolophus: food (sweet berries first, the favourite), the herd tag (farm animals only, Simple Animals optional), floating while ridden
assert json.loads((RES / f'data/{NS}/tags/item/parasaurolophus_food.json').read_text(encoding='utf-8'))['values'][0] == 'minecraft:sweet_berries'
herd = json.loads((RES / f'data/{NS}/tags/entity_type/herd_followers.json').read_text(encoding='utf-8'))['values']
assert 'minecraft:cow' in herd and 'minecraft:wolf' not in herd and all(v['required'] is False for v in herd if isinstance(v, dict)), herd
assert json.loads((RES / 'data/minecraft/tags/entity_type/can_float_while_ridden.json').read_text(encoding='utf-8'))['values'] == [f'{NS}:parasaurolophus']
assert not (RES / 'data/minecraft/tags/entity_type/dismounts_underwater.json').exists(), 'the Parasaurolophus never throws its rider off'
# Triceratops: food tag, windfall leaves and loot table
assert json.loads((RES / f'data/{NS}/tags/item/triceratops_food.json').read_text(encoding='utf-8'))['values'][0] == 'minecraft:apple'
assert set(json.loads((RES / f'data/{NS}/tags/block/windfall_leaves.json').read_text(encoding='utf-8'))['values']) == {'minecraft:oak_leaves', 'minecraft:dark_oak_leaves'}
assert json.loads((RES / f'data/{NS}/loot_table/shake/windfall.json').read_text(encoding='utf-8'))['type'] == 'minecraft:chest'
# World generation: configured + placed feature, biome tag, NeoForge modifier; Fabric reads the tag.
for p in ('worldgen/feature', 'worldgen/placed_feature'):
    assert (RES / f'data/{NS}/{p}/fossil_block.json').is_file(), p
assert (RES / f'data/{NS}/tags/worldgen/biome/has_fossil_block.json').is_file()
mod = json.loads((RES / f'data/{NS}/neoforge/biome_modifier/fossil_block.json').read_text(encoding='utf-8'))
assert mod['features'] == f'{NS}:fossil_block' and mod['step'] == 'underground_ores' and mod['biomes'] == f'#{NS}:has_fossil_block'
# Entity type tags: who may wear the large saddle and the armor.
def tag_values(t):
    return json.loads((RES / f'data/{NS}/tags/entity_type/{t}.json').read_text(encoding='utf-8'))['values']
assert tag_values('can_equip_large_saddle') == [f'{NS}:{e}' for e in ENTITIES]
assert tag_values('can_wear_dino_armor') == [f'{NS}:trex'] and tag_values('can_wear_triceratops_armor') == [f'{NS}:triceratops']
assert tag_values('can_wear_stegosaurus_armor') == [f'{NS}:stegosaurus']
assert tag_values('can_wear_parasaurolophus_armor') == [f'{NS}:parasaurolophus']
assert tag_values('can_wear_utahraptor_armor') == [f'{NS}:utahraptor']
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
for e in ENTITIES:
    for c in ('olive', 'sand', 'rust', 'moss', 'ash'):
        for t in ('plain', 'leather', 'iron', 'diamond'):
            assert (tex / f'entity/{e}/{c}/{t}.png').is_file(), (e, c, t)
for i in ('large_saddle',) + ARMORS + TRI_ARMORS + STEGO_ARMORS + ('dino_plow',) + PARA_ARMORS + RAPTOR_ARMORS + ('raptor_feather',) + SHELLS + ('guide_book',):
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
