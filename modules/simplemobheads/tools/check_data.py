"""Simple Mobheads data checks: the generator is current, every head has names (EN/DE, also its wall block and its
variants), questionary texts, a loot table and models, the wiki documents every head, Simple Fun no longer ships heads."""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplemobheads'


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def main():
    errors = []
    run = subprocess.run([sys.executable, str(MODULE / 'tools/generate_heads.py'), '--check'], capture_output=True, text=True)
    if run.returncode:
        errors.append(run.stdout.strip() or run.stderr.strip())
    heads = read(MODULE / 'data/mobs.json')['heads']
    res, gen = MODULE / 'shared/resources', MODULE / 'generated/resources'
    langs = {loc: read(res / f'assets/{NS}/lang/{loc}.json') for loc in ('en_us', 'de_de')}
    if langs['en_us'].keys() != langs['de_de'].keys():
        errors.append('lang keys differ: ' + str(sorted(langs['en_us'].keys() ^ langs['de_de'].keys())[:10]))
    quest = {loc: read(res / f'assets/{NS}/questionary/{loc}.json') for loc in ('en_us', 'de_de')}
    for head in heads:
        item = head.get('item', head['id'] + '_head')
        wall = item[:-len('_skull')] + '_wall_skull' if item.endswith('_skull') else item[:-len('_head')] + '_wall_head'
        for path in (f'assets/{NS}/items/{item}.json', f'assets/{NS}/blockstates/{item}.json', f'assets/{NS}/blockstates/{wall}.json',
                     f'data/{NS}/loot_table/blocks/{item}.json', f'data/{NS}/advancement/heads/{head["id"]}.json'):
            if not (gen / path).is_file():
                errors.append(f'{head["id"]}: missing {path}')
        for loc, table in langs.items():
            for key in (f'block.{NS}.{item}', f'block.{NS}.{wall}'):
                if key not in table:
                    errors.append(f'{loc}: missing {key}')
        for loc, table in quest.items():
            if item not in table or wall not in table:
                errors.append(f'{loc}: no questionary text for {item}')
    notes = read(MODULE / 'wiki/manual.json')['notes']
    for pattern in ('*_head', '*_skull', '*_wall_head', '*_wall_skull'):
        if pattern not in notes:
            errors.append(f'wiki note {pattern} missing')
    fun = ROOT / 'modules/simplefun/generated/resources/assets/simplefun/items'
    if any(p.name.endswith('_head.json') for p in fun.glob('*.json')):
        errors.append('Simple Fun still ships head item models')
    if errors:
        print('\n'.join(errors))
        return 1
    print(f'Simple Mobheads: {len(heads)} heads, bilingual names, questionary texts, models, loot and wiki notes valid')
    return 0


if __name__ == '__main__':
    sys.exit(main())
