"""Simple Trims data checks: bilingual lang files, wiki manual shape, empty test structure present."""
import json
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simpletrims'


def main():
    errors = []
    res = MODULE / 'shared/resources'
    langs = {loc: json.loads((res / f'assets/{NS}/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
    for a, b in (('en_us', 'de_de'), ('de_de', 'en_us')):
        errors += [f'{a}: missing {k}' for k in sorted(langs[b].keys() - langs[a].keys())]
    manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
    for feature in manual['features']:
        for loc in ('en', 'de'):
            if not feature.get(loc, {}).get('summary', '').strip():
                errors.append(f"wiki {loc}: no summary for {feature['id']}")
        for src in feature['sources']:
            if not (MODULE.parents[1] / src).is_file():
                errors.append(f'wiki source missing: {src}')
    if not (res / f'data/{NS}/structure/empty.nbt').is_file():
        errors.append('missing data/simpletrims/structure/empty.nbt')
    for e in errors:
        print(e)
    if errors:
        return 1
    print('simpletrims: lang, wiki manual and test structure valid')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
