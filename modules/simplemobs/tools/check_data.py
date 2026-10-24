"""Module-owned data hook: bilingual names, loot tables, entity registration wiring."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
DERIVED = {'shellker_shell', 'soul_fire_charge', 'soulfire_powder'}
_vd = (MODULE.parents[1] / 'tools/textures/vanilla_derived.py').read_text(encoding='utf-8')
assert "'item/shellker_shell.png'" in _vd, 'Shellker Shell texture is built by vanilla_derived.MODULE_OUTPUTS'
ITEMS = ('deceiver_cloth', 'deceiver_spawn_egg', 'echo_core', 'hollow_howler_spawn_egg', 'deceiver_cloak', 'trick_mirror', 'illusion_shard', 'wither_wings', 'nether_wings', 'end_wings')
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
for tex in ('hollow_howler', 'hollow_howler_glow', 'hollow_howler_heart'):
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
assert loot['pools'][0]['condition'] == {'type': 'minecraft:random_chance', 'chance': 0.5}, 'cloth: 50 %, never doubled'
assert loot['pools'][1]['entries'][0]['name'] == 'simplemobs:illusion_shard'
bonus = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/deceiver_armor_bonus.json').read_text(encoding='utf-8'))
assert bonus['pools'] == [loot['pools'][1]], 'armored Deceiver: the shard pool once more (twice the shards)'
mirage = json.loads((MODULE / 'shared/resources/data/simplemobs/recipe/brewing/potion_warped_illusion_shard.json').read_text(encoding='utf-8'))
assert mirage['fabric:load_conditions'] and mirage['neoforge:conditions'] and mirage['forge:conditions'], 'Mirage potion only with Simple Building'
assert len(list((MODULE / 'shared/resources/data/simplemobs/recipe/brewing').glob('*.json'))) >= 10
# N71: brewed from the warped potion with SimpleLib, from the awkward potion without it (not-conditions in all three loader forms)
for reagent in ('echo_core', 'shellker_shell', 'watcher_eye'):
    for container in ('potion', 'splash_potion', 'lingering_potion'):
        for base, negated in (('warped', False), ('awkward', True)):
            r = json.loads((MODULE / f'shared/resources/data/simplemobs/recipe/brewing/{container}_{base}_{reagent}.json').read_text(encoding='utf-8'))
            assert r['input']['potion_contents']['potions'] == ('simplelib:warped' if base == 'warped' else 'minecraft:awkward'), (container, base, reagent)
            assert (r['fabric:load_conditions'][0]['condition'] == 'fabric:not') == negated, (container, base, reagent)
            assert (r['neoforge:conditions'][0]['type'] == 'neoforge:not') == negated and (r['forge:conditions'][0]['type'] == 'forge:not') == negated
# Shellker + Soulfire Blaze (claude-mobs2)
for item in ('shellker_shell', 'shellker_helmet', 'shellker_spawn_egg', 'soulfire_rod', 'soulfire_blaze_spawn_egg', 'soulfire_powder', 'soul_fire_charge'):
    for locale in ('en_us', 'de_de'):
        assert f'item.simplemobs.{item}' in json.loads((MODULE / f'shared/resources/assets/simplemobs/lang/{locale}.json').read_text(encoding='utf-8')), item
    assert manual['notes'][f'simplemobs:{item}']['en']['summary'] and manual['notes'][f'simplemobs:{item}']['de']['summary'], item
    assert (MODULE / f'shared/resources/assets/simplemobs/items/{item}.json').is_file(), item
    if item in DERIVED:   # Vanilla-derived (AGENTS.md rule 7): built from the client jar, never committed
        assert not (MODULE / f'shared/resources/assets/simplemobs/textures/item/{item}.png').is_file(), item
    else:
        assert (MODULE / f'shared/resources/assets/simplemobs/textures/item/{item}.png').is_file(), item
for tex in ('entity/shellker/shellker', 'entity/shellker/shellker_eyes', 'entity/shellker/spark', 'entity/soulfire_blaze/soulfire_blaze',
            'entity/equipment/humanoid/shellker', 'mob_effect/high_gravity'):
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/{tex}.png').is_file(), tex
shell = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/shellker.json').read_text(encoding='utf-8'))
assert shell['pools'][0]['entries'][0]['name'] == 'simplemobs:shellker_shell'
assert 'simplemobs:shellker_helmet' in json.loads((MODULE / 'shared/resources/data/minecraft/tags/item/head_armor.json').read_text(encoding='utf-8'))['values'], 'helmet must be head armor (trims)'
for name in ('soulfire_powder', 'soul_fire_charge'):
    assert (MODULE / f'shared/resources/data/simplemobs/recipe/{name}.json').is_file(), name
assert 'simplemobs:shellker_helmet' in json.loads((MODULE / 'shared/resources/data/minecraft/tags/item/trimmable_armor.json').read_text(encoding='utf-8'))['values']
for name in ('potion_warped_shellker_shell', 'potion_awkward_shellker_shell', 'potion_high_gravity_redstone', 'potion_awkward_soulfire_rod'):
    assert (MODULE / f'shared/resources/data/simplemobs/recipe/brewing/{name}.json').is_file(), name
# Fear mobs (claude-fear-mobs): Dread, Hollow Watcher, Calm Amulet, Focus
for item in ('dread_link', 'calm_amulet', 'watcher_eye', 'dread_spawn_egg', 'hollow_watcher_spawn_egg'):
    for locale in ('en_us', 'de_de'):
        assert f'item.simplemobs.{item}' in json.loads((MODULE / f'shared/resources/assets/simplemobs/lang/{locale}.json').read_text(encoding='utf-8')), item
        assert item in json.loads((MODULE / f'shared/resources/assets/simplemobs/questionary/{locale}.json').read_text(encoding='utf-8')), item
    assert manual['notes'][f'simplemobs:{item}']['en']['summary'] and manual['notes'][f'simplemobs:{item}']['de']['summary'], item
    assert (MODULE / f'shared/resources/assets/simplemobs/items/{item}.json').is_file(), item
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/item/{item}.png').is_file(), item
for locale in ('en_us', 'de_de'):
    lang = json.loads((MODULE / f'shared/resources/assets/simplemobs/lang/{locale}.json').read_text(encoding='utf-8'))
    for key in ('entity.simplemobs.dread', 'entity.simplemobs.hollow_watcher', 'effect.simplemobs.focus', 'effect.simplemobs.shivering', 'item.minecraft.potion.effect.focus'):
        assert key in lang, key
for tex in ('entity/dread/dread', 'entity/dread/dread_glow', 'entity/hollow_watcher/hollow_watcher', 'entity/hollow_watcher/hollow_watcher_eyes',
            'mob_effect/focus', 'mob_effect/shivering'):
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/{tex}.png').is_file(), tex
dread = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/dread.json').read_text(encoding='utf-8'))
assert dread['pools'][0]['entries'][0]['name'] == 'simplemobs:dread_link' and dread['pools'][0]['condition']['type'] == 'minecraft:killed_by_player'
watcher = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/hollow_watcher.json').read_text(encoding='utf-8'))
assert watcher['pools'][0]['entries'][0]['name'] == 'simplemobs:watcher_eye'
for name in ('potion_awkward_watcher_eye', 'potion_focus_redstone', 'potion_focus_gunpowder', 'splash_potion_focus_dragon_breath'):
    assert (MODULE / f'shared/resources/data/simplemobs/recipe/brewing/{name}.json').is_file(), name
assert (MODULE / 'shared/resources/data/simplemobs/recipe/calm_amulet.json').is_file()
print('simplemobs: bilingual names, loot and assets valid')
# Hollow Howler voice: every MobsSounds event is in sounds.json with a subtitle in both languages and 3-5 takes
# (howler5, Vanilla-style variants: plain names or {name, volume, pitch, weight}); every take file exists exactly once
# and no stray file; A (make_howler_sounds.EVENTS), B (vanilla_derived_sounds.recipes) and howler_sounds.json name
# every event (concept KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md section 10).
import re
import sys
_snd = json.loads((MODULE / 'shared/resources/assets/simplemobs/sounds.json').read_text(encoding='utf-8'))
_events = re.findall(r'make\("(entity\.hollow_howler\.[a-z_]+)"\)', (MODULE / 'shared/java/com/simplebuilding/modules/simplemobs/MobsSounds.java').read_text(encoding='utf-8'))
assert _events and set(_events) == set(_snd), 'MobsSounds and sounds.json differ'
_dir = MODULE / 'shared/resources/assets/simplemobs/sounds/entity/hollow_howler'
_seen = []
for _name, _ev in _snd.items():
    assert 3 <= len(_ev['sounds']) <= 5, f'{_name}: 3-5 takes like Vanilla, not {len(_ev["sounds"])}'
    for _s in _ev['sounds']:
        if isinstance(_s, dict):
            assert set(_s) <= {'name', 'volume', 'pitch', 'weight'}, _s
            assert 0.5 <= _s.get('volume', 1) <= 1 and 0.8 <= _s.get('pitch', 1) <= 1.2 and 1 <= _s.get('weight', 1) <= 4, _s
            _s = _s['name']
        assert _s.startswith('simplemobs:entity/hollow_howler/' + _name.removeprefix('entity.hollow_howler.')), _s
        assert (MODULE / f"shared/resources/assets/simplemobs/sounds/{_s.split(':', 1)[1]}.ogg").is_file(), _s
        _seen.append(_s.rsplit('/', 1)[1])
    for _locale in ('en_us', 'de_de'):
        assert _ev['subtitle'] in json.loads((MODULE / f'shared/resources/assets/simplemobs/lang/{_locale}.json').read_text(encoding='utf-8')), _ev['subtitle']
assert len(_seen) == len(set(_seen)), 'a take is listed twice'
_stray = sorted({p.stem for p in _dir.glob('*.ogg')} - set(_seen))
assert not _stray, f'unreferenced Howler sound files: {_stray}'
for _ogg in _dir.glob('*.ogg'):
    _b = _ogg.read_bytes()
    assert _b[:4] == b'OggS' and len(_b) < 64 * 1024, f'{_ogg.name}: Ogg, small'
    assert _b[0x27] == 1, f'{_ogg.name}: mono (Minecraft only places mono sounds in space)'
_tools = MODULE.parents[1] / 'tools/sounds'
_short = {k.removeprefix('entity.hollow_howler.') for k in _snd}
_a = set(re.findall(r'^    "([a-z_]+)": \(\d+, ', (_tools / 'make_howler_sounds.py').read_text(encoding='utf-8'), re.M))
assert _a == _short, f'variant A EVENTS differ from sounds.json: {sorted(_a ^ _short)}'
_vds = (_tools / 'vanilla_derived_sounds.py').read_text(encoding='utf-8')
_b_events = set(re.findall(r"'([a-z_]+)': [a-z_]+[,}]", _vds[_vds.index('    return {\'heartbeat\''):]))
assert _b_events == _short, f'variant B recipes differ from sounds.json: {sorted(_b_events ^ _short)}'
_map = json.loads((_tools / 'howler_sounds.json').read_text(encoding='utf-8'))
assert _map['mode'] in ('synth', 'vanilla', 'mix', 'eleven'), _map['mode']
assert set(_map['mix']) == _short and set(_map['mix'].values()) <= {'synth', 'vanilla'}, 'howler_sounds.json mix: every event, synth or vanilla'
# W24-3 mode eleven: the ElevenLabs takes (shared/howler-eleven, eleven_howler_takes.py) are exactly the takes of the
# events in "eleven" - every take present, nothing else, mono and small like the others
_eleven = _map['eleven']
assert set(_eleven) <= _short and all(re.fullmatch(r'h_[a-z]+_\d', v) for v in _eleven.values()), _eleven
_want = {(_s if isinstance(_s, str) else _s['name']).rsplit('/', 1)[1] for _e in _eleven for _s in _snd['entity.hollow_howler.' + _e]['sounds']}
_have = {p.stem for p in (MODULE / 'shared/howler-eleven').glob('*.ogg')}
assert _have == _want, f'howler-eleven takes differ from sounds.json: {sorted(_have ^ _want)}'
for _ogg in (MODULE / 'shared/howler-eleven').glob('*.ogg'):
    _b = _ogg.read_bytes()
    assert _b[:4] == b'OggS' and len(_b) < 64 * 1024 and _b[0x27] == 1, f'howler-eleven/{_ogg.name}: mono Ogg, small'
sys.path.insert(0, str(_tools))
import vanilla_derived_sounds as _vd  # stdlib only at import time
assert _vd.selected('vanilla') == list(_short_ord := [k.removeprefix('entity.hollow_howler.') for k in _snd])
assert _vd.selected('mix') == [e for e in _short_ord if _map['mix'][e] == 'vanilla'] and _vd.selected('synth') == []
print(f'simplemobs: Hollow Howler voice {len(_snd)} events, {len(_seen)} takes, mode {_map["mode"]}')
# EL24-1 Wither/Nether Wings: Strong Upgrade with Simple Beacons, Netherite Upgrade without (not-conditions in all loader forms)
for stage, base, add in (('wither_wings', 'minecraft:elytra', 'simplemobs:nether_heart'), ('nether_wings', 'simplemobs:wither_wings', 'simplemobs:nether_gem')):
    for suffix, template, negated in (('_smithing', 'simplebeacons:strong_upgrade_template', False),
                                      ('_smithing_without_simplebeacons', 'minecraft:netherite_upgrade_smithing_template', True)):
        r = json.loads((MODULE / f'shared/resources/data/simplemobs/recipe/{stage}{suffix}.json').read_text(encoding='utf-8'))
        assert (r['type'], r['template'], r['base'], r['addition']) == ('minecraft:smithing_transform', template, base, add), (stage, suffix)
        assert (r['fabric:load_conditions'][0]['condition'] == 'fabric:not') == negated and (r['neoforge:conditions'][0]['type'] == 'neoforge:not') == negated
    assert (MODULE / f'shared/resources/assets/simplemobs/equipment/{stage}.json').is_file()
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/entity/equipment/wings/{stage}.png').is_file()
# WA24-2 End Wings (stage 3): item, look and speed exist, but no recipe and no loot yet (material of a later End boss)
assert (MODULE / 'shared/resources/assets/simplemobs/equipment/end_wings.json').is_file()
assert (MODULE / 'shared/resources/assets/simplemobs/textures/entity/equipment/wings/end_wings.png').is_file()
assert not list((MODULE / 'shared/resources/data/simplemobs').rglob('*end_wings*')), 'End Wings must not be craftable or dropped yet'
