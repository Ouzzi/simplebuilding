"""Questionary skeleton: lists the entries that are still missing, grouped by family (docs/ai/KONZEPT-QUESTIONARY-TABLE-2026-10-11.md).

Resolves every item of a namespace the way the game does (own entry, then the first family with a non-empty page, regex
families only; tag families are resolved in game) and prints per language:
- the items without text on some page, grouped by a suggested family (name suffix such as _octet, _stairs, _pressure_plate;
  tier prefixes such as reinforced_/netherite_/enderite_ are folded away), each group with a ready-made stub for the data file;
- families whose regex matches no item (dead pattern);
- with --overlap: items matched by several families on the same page (the first one wins).
python3 tools/questionary/skeleton.py [NAMESPACE] [--overlap]
"""
import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_data as cd  # noqa: E402

TIERS = ('reinforced', 'netherite', 'enderite', 'copper', 'iron', 'gold', 'diamond', 'stone', 'waxed', 'exposed', 'weathered', 'oxidized')
COLORS = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')


def suggest(item):
    """Name of the family an item most likely belongs to: its last word group without tier, color or wood prefix."""
    parts = item.split('_')
    while len(parts) > 1 and (parts[0] in TIERS or parts[0] in COLORS or parts[0] in ('polished', 'stripped', 'chiseled')):
        parts = parts[1:]
    return '_'.join(parts[-2:]) if len(parts) > 2 else '_'.join(parts)


def load(ns):
    for n, dirs, langs in cd.module_roots():
        if n == ns:
            data = {}
            for lang in cd.LANGS:
                merged = {}
                for d in dirs:
                    f = d / 'assets' / ns / 'questionary' / f'{lang}.json'
                    if f.is_file():
                        merged.update(json.loads(f.read_text(encoding='utf-8')))
                data[lang] = merged
            return data, cd.items_of(ns, langs)
    sys.exit(f'unknown namespace {ns}')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('namespace', nargs='?', default='simplebuilding')
    parser.add_argument('--overlap', action='store_true', help='list items matched by several families on one page')
    args = parser.parse_args()
    data, items = load(args.namespace)
    problems = 0
    for lang in cd.LANGS:
        entries = {k: v for k, v in data[lang].items() if isinstance(v, dict) and not k.startswith('_') and k != 'fallback'}
        families = []
        for key, v in entries.items():
            if key.startswith('family:'):
                pats = [re.compile(p[len('regex:'):]) for p in v.get('items', []) if p.startswith('regex:')]
                families.append((key, pats, v))
        used = defaultdict(int)
        missing = defaultdict(list)
        for item in sorted(items):
            for page in cd.PAGES:
                own = entries.get(item, {}).get(page)
                hits = [(k, v) for k, pats, v in families if any(p.fullmatch(item) for p in pats) and v.get(page)]
                if own:
                    continue
                if hits:
                    used[hits[0][0]] += 1
                    if args.overlap and len(hits) > 1:
                        print(f'[{lang}] {item}/{page}: {hits[0][0]} wins over {[k for k, _ in hits[1:]]}')
                else:
                    missing[suggest(item)].append(f'{item}/{page}')
        if missing:
            problems += 1
            print(f'[{lang}] missing (suggested family: items):')
            for fam in sorted(missing):
                shown = sorted({m.split('/')[0] for m in missing[fam]})
                print(f'  family:{fam}  ({len(shown)})  stub {{"items": ["regex:.*_{fam}"], "lore": ["TODO"], "use": ["TODO"], "secret": ["TODO"]}}  e.g. {", ".join(shown[:3])}')
        dead = [k for k, pats, _ in families if k not in used and not any(p.fullmatch(i) for p in pats for i in items)]
        if dead:
            print(f'[{lang}] families matching no {args.namespace} item (tag families or dead regex): {dead}')
    print('skeleton: all items covered on every page' if not problems else 'skeleton: gaps listed above')
    return 1 if problems else 0


if __name__ == '__main__':
    sys.exit(main())
