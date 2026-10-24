"""Module-owned data hook: bilingual texts, ids, recipes, unlocks, loot, tags, structure and wiki sources of Simple Lights."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplelights'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplelights'
DYES = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
        'brown', 'green', 'red', 'black']
PANES = (['light_pane', 'transparent_light_pane'] + [f'{d}_light_pane' for d in DYES] + ['redstone_light_pane', 'copper_light_pane']
         + [f'{d}_copper_light_pane' for d in DYES] + ['copper_redstone_light_pane'])
assert len(PANES) == 37

used = set()
for src in JAVA.rglob('*.java'):
    used |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', src.read_text(encoding='utf-8')))
used = {k for k in used if NS in k and not k.endswith('.') and not k.endswith('_')}
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    need = sorted(used) + [f'{p}.{NS}.{b}' for b in PANES for p in ('block', 'item')]
    need += [f'itemgroup.{NS}.lights', f'{NS}.testcentre.station', f'item.{NS}.guide_book', f'tooltip.{NS}.redstone_light_pane']
    for k in need:
        assert data.get(k), (locale, k)
    assert len(data[f'tooltip.{NS}.redstone_light_pane']) <= 40, ('tooltip line too long', locale)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'
for pane in PANES:
    for kind in ('blockstates', 'items'):
        assert (RES / f'assets/{NS}/{kind}/{pane}.json').is_file(), (kind, pane)
    assert (RES / f'data/{NS}/loot_table/blocks/{pane}.json').is_file(), pane
    assert (RES / f'data/{NS}/recipe/{pane}.json').is_file(), pane
    folder = 'redstone' if pane == 'redstone_light_pane' or 'copper' in pane else 'decorations'
    assert (RES / f'data/{NS}/advancement/recipes/{folder}/{pane}.json').is_file(), (folder, pane)
for tag, members in (('light_panes', 17), ('copper_light_panes', 17)):
    values = json.loads((RES / f'data/{NS}/tags/item/{tag}.json').read_text(encoding='utf-8'))['values']
    assert len(values) == members and all(v.startswith(f'{NS}:') for v in values), (tag, values)
for path in RES.rglob('*.json'):
    if path.name.endswith('.layers.json'):
        continue
    assert 'simplebuilding:' not in path.read_text(encoding='utf-8'), path
assert (RES / f'data/{NS}/structure/empty.nbt').is_file()
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for f in list(manual['features']) + list(manual['notes'].values()):
    for src in f['sources']:
        assert (ROOT / src).is_file(), src
print(f'{NS}: {len(PANES)} panes, {len(used)} UI texts, recipes, unlocks, loot, tags, structure and wiki valid')
