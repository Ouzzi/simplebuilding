"""Module-owned data hook: bilingual names, loot tables, entity registration wiring."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
DERIVED = {'shellker_shell'}
_vd = (MODULE.parents[1] / 'tools/textures/vanilla_derived.py').read_text(encoding='utf-8')
assert "'item/shellker_shell.png'" in _vd, 'Shellker Shell texture is built by vanilla_derived.MODULE_OUTPUTS'
ITEMS = ('deceiver_cloth', 'deceiver_spawn_egg', 'echo_core', 'hollow_howler_spawn_egg', 'deceiver_cloak', 'trick_mirror', 'illusion_shard')
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
assert (MODULE / 'shared/resources/assets/simplemobs/textures/entity/equipment/humanoid/deceiver_cloak.png').is_file()
sees = json.loads((MODULE / 'shared/resources/data/simplemobs/tags/entity_type/sees_through_disguise.json').read_text(encoding='utf-8'))['values']
assert 'minecraft:warden' in sees and 'simplemobs:hollow_howler' in sees
for v in sees:
    assert not isinstance(v, dict) or v.get('required') is False, v
assert loot['pools'][0]['conditions'] == [{'condition': 'minecraft:random_chance', 'chance': 0.5}], 'cloth: 50 %, never doubled'
assert loot['pools'][1]['entries'][0]['name'] == 'simplemobs:illusion_shard'
bonus = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/deceiver_armor_bonus.json').read_text(encoding='utf-8'))
assert bonus['pools'] == [loot['pools'][1]], 'armored Deceiver: the shard pool once more (twice the shards)'
mirage = json.loads((MODULE / 'shared/resources/data/simplemobs/recipe/brewing/potion_awkward_illusion_shard.json').read_text(encoding='utf-8'))
assert mirage['fabric:load_conditions'] and mirage['neoforge:conditions'] and mirage['forge:conditions'], 'Mirage potion only with Simple Building'
assert len(list((MODULE / 'shared/resources/data/simplemobs/recipe/brewing').glob('*.json'))) >= 10
# Shellker + Soulfire Blaze (claude-mobs2)
for item in ('shellker_shell', 'shellker_helmet', 'shellker_spawn_egg', 'soulfire_rod', 'soulfire_blaze_spawn_egg'):
    for locale in ('en_us', 'de_de'):
        assert f'item.simplemobs.{item}' in json.loads((MODULE / f'shared/resources/assets/simplemobs/lang/{locale}.json').read_text(encoding='utf-8')), item
    assert manual['notes'][f'simplemobs:{item}']['en']['summary'] and manual['notes'][f'simplemobs:{item}']['de']['summary'], item
    assert (MODULE / f'shared/resources/assets/simplemobs/items/{item}.json').is_file(), item
    if item in DERIVED:   # Vanilla-derived (AGENTS.md rule 7): built from the client jar, never committed
        assert not (MODULE / f'shared/resources/assets/simplemobs/textures/item/{item}.png').is_file(), item
    else:
        assert (MODULE / f'shared/resources/assets/simplemobs/textures/item/{item}.png').is_file(), item
for tex in ('entity/shellker/shellker', 'entity/shellker/shellker_eyes', 'entity/shellker/spark', 'entity/soulfire_blaze/soulfire_blaze',
            'entity/equipment/humanoid/shellker', 'item/soul_fire_charge', 'mob_effect/high_gravity'):
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/{tex}.png').is_file(), tex
shell = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/shellker.json').read_text(encoding='utf-8'))
assert shell['pools'][0]['entries'][0]['name'] == 'simplemobs:shellker_shell'
assert 'simplemobs:shellker_helmet' in json.loads((MODULE / 'shared/resources/data/minecraft/tags/item/head_armor.json').read_text(encoding='utf-8'))['values'], 'helmet must be head armor (trims)'
for name in ('potion_awkward_shellker_shell', 'potion_high_gravity_redstone', 'potion_awkward_soulfire_rod'):
    assert (MODULE / f'shared/resources/data/simplemobs/recipe/brewing/{name}.json').is_file(), name
print('simplemobs: bilingual names, loot and assets valid')
