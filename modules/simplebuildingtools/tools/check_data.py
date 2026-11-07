"""Simple Building Tools data invariants (b-btools07): every tool has EN/DE names, a model and a wiki note; the config keys have texts."""
import fnmatch
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
RES = MODULE / 'shared/resources'
COLORS = 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
ITEMS = [t + '_building_wand' for t in ('copper', 'iron', 'gold', 'diamond', 'netherite', 'enderite', 'creative')]
ITEMS += ['octant'] + ['octant_' + c for c in COLORS] + ['blueprint', 'creative_blueprint']
KEYS = ['blueprint.enabled', 'blueprint.maxBlocksPerTick', 'blueprint.pictureImport', 'blueprint.picturePermission',
        'blueprint.pictureMaxSide', 'wands.hungerCost', 'wands.hungerMultiplier']
problems = []
for locale in ('en_us', 'de_de'):
    lang = json.loads((RES / f'assets/simplebuildingtools/lang/{locale}.json').read_text(encoding='utf-8'))
    for item in ITEMS:
        if f'item.simplebuildingtools.{item}' not in lang: problems.append(f'{locale}: name of {item}')
    for e in ('cover', 'bridge', 'linear'):
        if f'enchantment.simplebuildingtools.{e}' not in lang: problems.append(f'{locale}: enchantment {e}')
    for key in KEYS:
        tip = lang.get(f'text.autoconfig.simplebuildingtools.option.{key}.@Tooltip', '')
        if f'text.autoconfig.simplebuildingtools.option.{key}' not in lang: problems.append(f'{locale}: option {key}')
        if ('Default:' if locale == 'en_us' else 'Standard:') not in tip: problems.append(f'{locale}: option {key} tooltip without default')
for item in ITEMS:
    if not (RES / f'assets/simplebuildingtools/items/{item}.json').exists(): problems.append(f'item definition {item}')
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for item in ITEMS:
    notes = manual['notes']
    if not any(fnmatch.fnmatch(f'simplebuildingtools:{item}', k) for k in notes):
        problems.append(f'wiki note {item}')
assert not problems, problems
print(f'simplebuildingtools: {len(ITEMS)} tools with names, models and wiki notes; {len(KEYS)} config keys with texts')
