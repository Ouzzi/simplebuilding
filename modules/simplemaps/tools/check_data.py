"""Simple Maps data checks: bilingual names, wiki coverage, textures up to date, recipes and tags present."""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simplemaps'
ITEMS = ('wayfinder_map', 'nether_wayfinder_map', 'end_wayfinder_map')


def main():
    errors = []
    res = MODULE / 'shared/resources'
    langs = {loc: json.loads((res / f'assets/{NS}/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
    for a, b in (('en_us', 'de_de'), ('de_de', 'en_us')):
        errors += [f'{a}: missing {k}' for k in sorted(langs[b].keys() - langs[a].keys())]
    notes = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))['notes']
    for item in ITEMS:
        if f'item.{NS}.{item}' not in langs['en_us']:
            errors.append(f'no name for {item}')
        if f'{NS}:{item}' not in notes:
            errors.append(f'no wiki note for {item}')
        for path in (f'assets/{NS}/items/{item}.json', f'assets/{NS}/models/item/{item}.json',
                     f'assets/{NS}/textures/item/{item}.png', f'data/{NS}/recipe/{item}.json'):
            if not (res / path).is_file():
                errors.append(f'missing {path}')
    for tag in ('tags/dimension_type/nether_wayfinder', 'tags/dimension_type/end_wayfinder', 'tags/item/waypoint_heads', 'tags/item/wayfinder_maps'):
        if not (res / f'data/{NS}/{tag}.json').is_file():
            errors.append(f'missing tag {tag}')
    textures = subprocess.run([sys.executable, str(MODULE / 'tools/gen_textures.py'), '--check'], capture_output=True, text=True)
    if textures.returncode:
        errors.append(textures.stdout.strip() or 'texture check failed')
    for e in errors:
        print(e)
    if errors:
        return 1
    print('simplemaps: names, wiki notes, models, textures, recipes and tags valid')
    return 0


if __name__ == '__main__':
    sys.exit(main())
