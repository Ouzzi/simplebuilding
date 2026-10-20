"""Module-owned data hook: bilingual texts, config keys in the wiki, recipes, tags, mixins, structure, guide and wiki sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simpleshops'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simpleshops'
BLOCKS = ('sales_block', 'sales_storage', 'display_case', 'glow_display_case')
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/ShopsConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert config_keys == ['enabled', 'maxShopsPerPlayer', 'idleTimeoutSeconds', 'collectServerShopIncome', 'remoteStock', 'remoteDelaySeconds',
                       'remoteCrossDimension'], config_keys
used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in sorted(used) + [f'item.{NS}.guide_book', f'{NS}.testcentre.station'] + [f'block.{NS}.{b}' for b in BLOCKS + ('trade_terminal',)]:
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
recipes = {p.stem: json.loads(p.read_text(encoding='utf-8')) for p in (RES / f'data/{NS}/recipe').glob('*.json')}
assert recipes['sales_block']['pattern'] == ['PPP', 'IEI', 'PCP'], 'sales block recipe (ender pearl, KONZ20)'
assert recipes['sales_storage']['pattern'] == ['PPP', 'IEI', 'PHP'], 'sales storage recipe (ender pearl, KONZ20)'
assert recipes['display_case']['pattern'] == ['NNN', 'GGG', 'PPP'], 'display case recipe'
assert recipes['trade_terminal']['pattern'] == ['GEG', 'BLB', 'PPP'], 'trade terminal recipe (KONZ20 §3)'
assert recipes['glow_display_case']['ingredients'] == [f'{NS}:display_case', 'minecraft:glow_ink_sac'], 'glow case recipe'
for name in BLOCKS:
    loot = json.loads((RES / f'data/{NS}/loot_table/blocks/{name}.json').read_text(encoding='utf-8'))
    assert loot['pools'][0]['entries'][0]['name'] == f'{NS}:{name}', name
    for kind in ('wither_immune', 'dragon_immune'):
        tag = json.loads((RES / f'data/minecraft/tags/block/{kind}.json').read_text(encoding='utf-8'))
        assert f'{NS}:{name}' in tag['values'] and tag['replace'] is False, (kind, name)
    for sub in ('blockstates', 'items', 'models/block'):
        assert (RES / f'assets/{NS}/{sub}/{name}.json').is_file(), (sub, name)
# The trade terminal holds nothing: an ordinary block (axe, own loot with the explosion rule, never in the immune tags).
loot = json.loads((RES / f'data/{NS}/loot_table/blocks/trade_terminal.json').read_text(encoding='utf-8'))
assert loot['pools'][0]['entries'][0]['name'] == f'{NS}:trade_terminal'
for kind in ('wither_immune', 'dragon_immune'):
    assert f'{NS}:trade_terminal' not in json.loads((RES / f'data/minecraft/tags/block/{kind}.json').read_text(encoding='utf-8'))['values']
for sub in ('blockstates', 'items', 'models/block'):
    assert (RES / f'assets/{NS}/{sub}/trade_terminal.json').is_file(), sub
for tex in ('trade_terminal_front', 'trade_terminal_side', 'trade_terminal_top', 'sales_block_front', 'sales_block_side', 'sales_block_top', 'sales_storage_front', 'sales_storage_side', 'sales_storage_top', 'display_case_base', 'display_case_frame',
            'display_case_glass', 'glow_display_case_base', 'glow_display_case_frame'):
    assert (RES / f'assets/{NS}/textures/block/{tex}.png').is_file(), tex
assert (RES / f'data/{NS}/structure/arena.nbt').is_file()
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
for locale in ('en_us', 'de_de'):
    q = json.loads((RES / f'assets/{NS}/questionary/{locale}.json').read_text(encoding='utf-8'))
    assert set(q) == set(BLOCKS) | {'trade_terminal'}, (locale, set(q))
print(f'{NS}: {len(config_keys)} config keys, {len(used)} UI texts, {len(recipes)} recipes, tags, mixins, structure, questionary and wiki valid')
