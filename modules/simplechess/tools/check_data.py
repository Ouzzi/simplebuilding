"""Simple Chess data hook (b-chess47): bilingual names for every moved id, wiki features, valid recipes with their load
conditions (octets only with Simple Octets, palette colours only with SimpleBuilding), the chess assets (tools/check_chess_assets.py)."""
import json
import re
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
RES = MODULE / 'shared/resources'
PALETTE = ('nihilith', 'astralit', 'ender_quartz', 'polished_astralit', 'polished_nihilith', 'polished_ender_quartz')
langs = {loc: json.loads((RES / f'assets/simplechess/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
assert langs['en_us'].keys() == langs['de_de'].keys(), 'lang key mismatch: ' + str(langs['en_us'].keys() ^ langs['de_de'].keys())
items = sorted(p.stem for p in (RES / 'assets/simplechess/items').glob('*.json'))
assert len(items) >= 207 - 2 + 1, f'{len(items)} item definitions'  # 205 moved items + guide book
for item in items:
    for loc, lang in langs.items():
        assert f'item.simplechess.{item}' in lang or f'block.simplechess.{item}' in lang, f'{item}: no name in {loc}'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert {f['id'] for f in manual['features']} >= {'chess', 'checkers', 'octets', 'migration'}
for f in manual['features']:
    for src in f.get('sources', []):
        assert (ROOT / src).is_file(), f'{f["id"]}: missing source {src}'


def palette(path):
    return any(path == p or path.startswith(p + '_') for p in PALETTE)


def needs(data):
    mods = set()
    for c in data.get('fabric:load_conditions', []):
        if c.get('condition') == 'fabric:all_mods_loaded':
            mods |= set(c['values'])
    return mods


checked = 0
for path in sorted((RES / 'data/simplechess').rglob('*.json')):
    data = json.loads(path.read_text(encoding='utf-8'))
    if 'recipe' not in path.parts and 'advancement' not in path.parts:
        continue
    text = json.dumps({k: v for k, v in data.items() if k != 'fabric:load_conditions'})
    ids = set(re.findall(r'"simplechess:([a-z0-9_/]+)"', text))
    have = needs(data)
    if any(i.endswith('_octet') for i in ids):
        assert 'simpleoctets' in have, f'{path.name}: names an octet without the simpleoctets condition'
    if any(palette(i) for i in ids) or 'simplebuilding:' in text:
        assert 'simplebuilding' in have, f'{path.name}: names a palette colour without the simplebuilding condition'
    checked += 1
assert checked > 600, checked
result = subprocess.run([sys.executable, str(ROOT / 'tools/check_chess_assets.py')], capture_output=True, text=True)
assert result.returncode == 0, result.stdout + result.stderr
print(f'simplechess: {len(items)} items named EN/DE, {checked} recipes/advancements with their conditions, assets OK')
