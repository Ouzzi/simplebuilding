"""Module-owned data hook: bilingual names, loot tables, entity registration wiring."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
ITEMS = ('deceiver_cloth', 'deceiver_spawn_egg', 'echo_core', 'hollow_howler_spawn_egg', 'deceiver_cloak', 'trick_mirror')
for locale in ('en_us', 'de_de'):
    data = json.loads((MODULE / f'shared/resources/assets/simplemobs/lang/{locale}.json').read_text(encoding='utf-8'))
    assert 'entity.simplemobs.deceiver' in data and 'entity.simplemobs.hollow_howler' in data
    for key in ('effect.simplemobs.silence', 'item.minecraft.potion.effect.silence', 'message.simplemobs.howler.fades', 'message.simplemobs.howler.too_bright'):
        assert key in data, key
    for item in ITEMS:
        assert f'item.simplemobs.{item}' in data, item
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for item in ITEMS:
    assert manual['notes'][f'simplemobs:{item}']['en']['summary'] and manual['notes'][f'simplemobs:{item}']['de']['summary']
    assert (MODULE / f'shared/resources/assets/simplemobs/items/{item}.json').is_file()
    assert (MODULE / f'shared/resources/assets/simplemobs/models/item/{item}.json').is_file()
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/item/{item}.png').is_file()
for tex in ('deceiver', 'deceiver_eyes'):
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/entity/deceiver/{tex}.png').is_file()
loot = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/deceiver.json').read_text(encoding='utf-8'))
assert loot['pools'][0]['entries'][0]['name'] == 'simplemobs:deceiver_cloth'
summoned = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/summoned.json').read_text(encoding='utf-8'))
assert summoned['pools'] == [], 'summoned mobs must not drop anything'
for tex in ('hollow_howler', 'hollow_howler_glow'):
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/entity/hollow_howler/{tex}.png').is_file()
assert (MODULE / 'shared/resources/assets/simplemobs/textures/mob_effect/silence.png').is_file()
howler = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/hollow_howler.json').read_text(encoding='utf-8'))
names = [e.get('name') or e.get('items') for pool in howler['pools'] for e in pool['entries']]
assert names == ['minecraft:echo_shard', 'simplemobs:echo_core', '#simplemobs:howler_jaw_drops'], names
jaw = json.loads((MODULE / 'shared/resources/data/simplemobs/tags/block/howler_jaw.json').read_text(encoding='utf-8'))
assert 'minecraft:sculk_shrieker' in jaw['values'], 'the shrieker keeps the ritual playable without Simple Building'
for tag in ('block/howler_jaw', 'item/howler_jaw_drops'):
    for v in json.loads((MODULE / f'shared/resources/data/simplemobs/tags/{tag}.json').read_text(encoding='utf-8'))['values']:
        if isinstance(v, dict) or not v.startswith(('minecraft:', '#minecraft:', 'simplemobs:')):
            assert isinstance(v, dict) and v.get('required') is False, f'foreign id must be optional: {v}'
assert len(list((MODULE / 'shared/resources/data/simplemobs/recipe/brewing').glob('*.json'))) == 23
assert (MODULE / 'shared/resources/assets/simplemobs/textures/entity/equipment/humanoid/deceiver_cloak.png').is_file()
sees = json.loads((MODULE / 'shared/resources/data/simplemobs/tags/entity_type/sees_through_disguise.json').read_text(encoding='utf-8'))['values']
assert 'minecraft:warden' in sees and 'simplemobs:hollow_howler' in sees
for v in sees:
    assert not isinstance(v, dict) or v.get('required') is False, v
print('simplemobs: bilingual names, loot and assets valid')
