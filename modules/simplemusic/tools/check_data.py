"""Module-owned data hook: bilingual texts, config keys in the wiki, recipes, mixins, structure and wiki sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simplemusic'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplemusic'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/MusicConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert len(config_keys) == 13, config_keys
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in sorted(used) + [f'item.{NS}.guide_book', f'item.{NS}.sheet_music', f'item.{NS}.blank_disc',
                             f'item.{NS}.custom_disc', f'block.{NS}.disc_press', f'{NS}.testcentre.station']:
        assert data.get(k), (locale, k)
    for k, v in data.items():
        if k.startswith('item.') and '.length' in k:
            assert len(v) <= 40, ('tooltip line too long', locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
recipes = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (RES / f'data/{NS}/recipe').glob('*.json')}
assert 'orchestrion' not in recipes, 'B23-M9: the Orchestrion is gone'
assert sorted(recipes['sheet_music']['ingredients']) == ['minecraft:feather', 'minecraft:ink_sac', 'minecraft:paper']
assert recipes['blank_disc']['key']['A'] == 'minecraft:amethyst_shard'
# owner 21.10.: disc press like Vanilla's stone/iron workstations, the piston presses
assert recipes['disc_press']['pattern'] == [' P ', 'I I', 'SSS'] and recipes['disc_press']['key'] == {
    'P': 'minecraft:piston', 'I': 'minecraft:iron_ingot', 'S': 'minecraft:smooth_stone'}
for tex in ('block/disc_press_side', 'block/disc_press_top', 'item/custom_disc_composed'):
    assert (RES / f'assets/{NS}/textures/{tex}.png').is_file(), tex
pockets = [k for k in recipes if k.endswith('pocket_jukebox')]
assert len(pockets) == 17 and len([k for k in recipes if k.endswith('pocket_jukebox_dyeing')]) == 16, 'pocket jukebox recipes'
for k in pockets:  # owner 21.10.: a diamond instead of the blank disc, the colour comes from the bundle
    r = recipes[k]
    assert r['pattern'] == ['NDN', 'NBN', 'NNN'] and r['key']['N'] == 'minecraft:copper_nugget' and r['key']['D'] == 'minecraft:diamond', k
    assert r['key']['B'] == 'minecraft:' + k.replace('pocket_jukebox', 'bundle').lstrip('_'), k
    for suffix in ('', '_open_back', '_open_front'):
        assert (RES / f'assets/{NS}/textures/item/{k}{suffix}.png').is_file(), (k, suffix)
    # b-pjuke21: placeable, 3D in the hand, breaking keeps the discs
    assert (RES / f'assets/{NS}/textures/block/{k}_case.png').is_file(), k
    state = json.loads((RES / f'assets/{NS}/blockstates/{k}.json').read_text(encoding='utf-8'))
    fallback = json.loads((RES / f'assets/{NS}/items/{k}.json').read_text(encoding='utf-8'))['model']['fallback']
    if k == 'pocket_jukebox':
        # a-pjuke23 (exemplary on the undyed one): turning record while playing, copper contacts to lines
        assert any(p['apply']['model'] == f'{NS}:block/pocket_jukebox_playing' for p in state['multipart']), k
        assert any(p['apply']['model'] == f'{NS}:block/pocket_jukebox_link' for p in state['multipart']), k
        assert fallback['on_false']['model'] == f'{NS}:block/{k}' and fallback['on_true']['model'] == f'{NS}:block/pocket_jukebox_playing', k
        for tex in ('block/pocket_jukebox_spin', 'item/pocket_jukebox_playing'):
            assert (RES / f'assets/{NS}/textures/{tex}.png.mcmeta').is_file(), tex
    else:
        assert state['variants'], k
        assert fallback['model'] == f'{NS}:block/{k}', k
    loot = json.loads((RES / f'data/{NS}/loot_table/blocks/{k}.json').read_text(encoding='utf-8'))
    entry = loot['pools'][0]['entries'][0]
    assert entry['name'] == f'{NS}:{k}' and entry['modifier'] == {'type': 'minecraft:copy_components', 'source': 'block_entity'}, k
    assert 'conditions' not in loot['pools'][0], ('always drops', k)
assert (RES / f'assets/{NS}/textures/block/pocket_jukebox_parts.png').is_file()
for r in recipes.values():
    for value in re.findall(r'"(#?[a-z0-9_.-]+:[a-z0-9_./-]+)"', json.dumps(r)):
        if not value.startswith(('minecraft:', '#minecraft:', f'{NS}:', f'#{NS}:')):
            raise AssertionError(('foreign id in a recipe without condition', value))
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
mixins = json.loads((RES / f'{NS}.mixins.json').read_text(encoding='utf-8'))
for name in mixins['mixins'] + mixins['client']:
    assert (JAVA / 'mixin' / (name.replace('.', '/') + '.java')).is_file(), name
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config = next(f for f in manual['features'] if f['id'] == 'config_server')
for locale in ('en', 'de'):
    text = ' '.join(config[locale]['details'])
    for k in config_keys:
        assert k in text, ('wiki config lacks', locale, k)
for f in manual['features']:
    for src in f['sources']:
        assert (MODULE.parents[1] / src).is_file(), src
# W24-3: the six music discs - song data, streamed mono Ogg (at most 1 MiB), sound event with subtitle, item model,
# texture, both languages, a wiki entry
discs = re.findall(r'new Disc\("([a-z_]+)", ([0-9.]+)F, (\d+), table\("([a-z_/]+)"\), (\d+)\)',
                   (JAVA / 'disc/MusicDiscs.java').read_text(encoding='utf-8'))
assert len(discs) == 6, discs
sounds_json = json.loads((RES / f'assets/{NS}/sounds.json').read_text(encoding='utf-8'))
for disc, seconds, comparator, table, weight in discs:
    song = json.loads((RES / f'data/{NS}/jukebox_song/{disc}.json').read_text(encoding='utf-8'))
    assert song['length_in_seconds'] == float(seconds) and song['comparator_output'] == int(comparator), disc
    assert song['sound_event'] == {'sound_id': f'{NS}:music_disc.{disc}'} and song['description'] == {'translate': f'jukebox_song.{NS}.{disc}'}, disc
    event = sounds_json[f'music_disc.{disc}']
    assert event['sounds'] == [{'name': f'{NS}:records/{disc}', 'stream': True}], disc
    ogg = (RES / f'assets/{NS}/sounds/records/{disc}.ogg').read_bytes()
    assert ogg[:4] == b'OggS' and ogg[0x27] == 1 and len(ogg) <= 1024 * 1024, (disc, 'mono Ogg, at most 1 MiB')
    assert (RES / f'assets/{NS}/textures/item/music_disc_{disc}.png').is_file(), disc
    assert json.loads((RES / f'assets/{NS}/items/music_disc_{disc}.json').read_text(encoding='utf-8'))['model']['model'] == f'{NS}:item/music_disc_{disc}'
    for locale in ('en_us', 'de_de'):
        for k in (f'item.{NS}.music_disc_{disc}', f'jukebox_song.{NS}.{disc}', event['subtitle']):
            assert langs[locale].get(k), (locale, k)
        assert langs[locale][f'jukebox_song.{NS}.{disc}'].startswith('Simple Music - '), (locale, disc)
assert any(f['id'] == 'music_discs' for f in manual['features']), 'wiki entry music_discs'
print(f'{NS}: {len(config_keys)} config keys, {len(used)} UI texts, {len(recipes)} recipes, mixins, structure and wiki valid')
