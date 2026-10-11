"""Simple Questionary data checks: bilingual names, wiki notes, block/item models, textures up to date, recipe, loot,
axe tag, and the questionary texts of all mods (structure; missing texts only reported - tools/questionary/check_data.py)."""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROOT = MODULE.parents[1]
NS = 'simplequestionary'
BLOCK = 'questionary_table'


def main():
    errors = []
    res = MODULE / 'shared/resources'
    langs = {loc: json.loads((res / f'assets/{NS}/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
    for a, b in (('en_us', 'de_de'), ('de_de', 'en_us')):
        errors += [f'{a}: missing {k}' for k in sorted(langs[b].keys() - langs[a].keys())]
    if f'block.{NS}.{BLOCK}' not in langs['en_us']:
        errors.append(f'no name for {BLOCK}')
    notes = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))['notes']
    if f'{NS}:{BLOCK}' not in notes:
        errors.append(f'no wiki note for {BLOCK}')
    for path in (f'assets/{NS}/blockstates/{BLOCK}.json', f'assets/{NS}/items/{BLOCK}.json', f'assets/{NS}/models/block/{BLOCK}.json',
                 f'data/{NS}/recipe/{BLOCK}.json', f'data/{NS}/loot_table/blocks/{BLOCK}.json',
                 f'data/{NS}/advancement/recipes/decorations/{BLOCK}.json', f'assets/{NS}/questionary/en_us.json',
                 f'assets/{NS}/questionary/de_de.json', f'data/{NS}/structure/empty.nbt'):
        if not (res / path).is_file():
            errors.append(f'missing {path}')
    model = json.loads((res / f'assets/{NS}/models/block/{BLOCK}.json').read_text(encoding='utf-8'))
    for ref in model['textures'].values():
        if not (res / f'assets/{NS}/textures/{ref.split(":", 1)[1]}.png').is_file():
            errors.append(f'missing texture {ref}')
    axe = json.loads((res / 'data/minecraft/tags/block/mineable/axe.json').read_text(encoding='utf-8'))
    if f'{NS}:{BLOCK}' not in axe['values']:
        errors.append('not mineable with an axe')
    for script in (MODULE / 'tools/gen_textures.py', ROOT / 'tools/questionary/check_data.py'):
        args = [sys.executable, str(script)] + (['--check'] if script.name == 'gen_textures.py' else [])
        run = subprocess.run(args, capture_output=True, text=True)
        if run.returncode:
            errors.append(f'{script.name}: ' + (run.stdout.strip() or run.stderr.strip() or 'failed'))
    for e in errors:
        print(e)
    if errors:
        return 1
    print('simplequestionary: names, wiki notes, models, textures, recipe, loot, tags and questionary texts valid')
    return 0


if __name__ == '__main__':
    sys.exit(main())
