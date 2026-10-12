"""Questionary texts: structure check and coverage report (concept docs/ai/KONZEPT-QUESTIONARY-TABLE-2026-10-11.md).

Reads every assets/<ns>/questionary/<lang>.json of SimpleBuilding and the modules and
- fails on structure errors: bad JSON, EN/DE key sets differ, a page that is not a list of texts, TODO marks,
  unknown placeholders, pages over 350 characters, a fallback that is not 8 variants per class and page;
- reports coverage: per namespace (and vanilla from vanilla_items.txt, the item ids of the 26.3 client jar) how many items (from the lang keys item.<ns>.* / block.<ns>.*) have an own entry or
  a regex family in English and German. Tag families are resolved only in game (coverage game test).
Missing texts are a warning while the texts are being written; --strict makes them an error.
python3 tools/questionary/check_data.py [--strict] [--list NAMESPACE]
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LANGS = ('en_us', 'de_de')
PAGES = ('lore', 'use', 'secret')
KINDS = ('block', 'tool', 'armor', 'food', 'misc')
VARIANTS = 8
MAX_PAGE_CHARS = 350
PLACEHOLDER = re.compile(r'\{(name|mod|item:[a-z0-9_.-]+:[a-z0-9_./-]+)}')


def module_roots():
    """(namespace, resource dirs, lang dir) of SimpleBuilding and every module."""
    modules = json.loads((ROOT / 'modules/modules.json').read_text(encoding='utf-8'))['modules']
    for m in modules:
        p = m['paths']
        ns = m.get('namespace', m['id'])
        if m['id'] == 'simplebuilding':
            dirs = [ROOT / 'src/main/resources', ROOT / 'mc26_3/overlay/resources', ROOT / 'mc26_3/generated']
            langs = [ROOT / p['lang'], ROOT / p['langOverlay']]
        else:
            dirs = [ROOT / p['shared'] / 'resources', ROOT / p['generated']]
            dirs += [ROOT / p[l] / 'src/main/resources' for l in m['loaders'] if p.get(l)]
            langs = [ROOT / p['lang']]
        yield ns, dirs, langs
    yield 'minecraft', [ROOT / 'modules/simplequestionary/shared/resources'], []


def items_of(ns, langs):
    if ns == 'minecraft':  # ids of the 26.3 item registry (assets/minecraft/items), see vanilla_items.txt
        return {l.strip() for l in (Path(__file__).parent / 'vanilla_items.txt').read_text(encoding='utf-8').splitlines() if l.strip()}
    found = set()
    for d in langs:
        f = d / 'en_us.json'
        if not f.is_file():
            continue
        for key in json.loads(f.read_text(encoding='utf-8')):
            for prefix in ('item.', 'block.'):
                if key.startswith(f'{prefix}{ns}.') and '.' not in key[len(prefix) + len(ns) + 1:]:
                    found.add(key[len(prefix) + len(ns) + 1:])
    return found


def page_problems(value):
    if isinstance(value, str):
        value = [value]
    if not isinstance(value, list) or not all(isinstance(p, str) for p in value):
        return ['not a list of texts']
    out = []
    text = '\n'.join(value)
    filled = PLACEHOLDER.sub('Placeholder', text)
    if '{' in filled or '}' in filled:
        out.append('unknown placeholder')
    if 'TODO' in text:
        out.append('TODO left')
    if len(filled) > MAX_PAGE_CHARS:
        out.append(f'longer than {MAX_PAGE_CHARS} characters ({len(filled)})')
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--strict', action='store_true', help='missing texts are errors')
    parser.add_argument('--list', metavar='NAMESPACE', help='print the items of NAMESPACE that still lack a text')
    args = parser.parse_args()
    errors, report = [], []
    for ns, dirs, langs in module_roots():
        files = {lang: [d / 'assets' / ns / 'questionary' / f'{lang}.json' for d in dirs] for lang in LANGS}
        data = {}
        for lang in LANGS:
            merged = {}
            for f in files[lang]:
                if not f.is_file():
                    continue
                try:
                    merged.update(json.loads(f.read_text(encoding='utf-8')))
                except json.JSONDecodeError as e:
                    errors.append(f'{f.relative_to(ROOT)}: {e}')
            data[lang] = merged
        present = {lang for lang in LANGS if any(f.is_file() for f in files[lang])}
        if present and present != set(LANGS):
            errors.append(f'{ns}: questionary only in {sorted(present)}')
        en_keys = {k for k in data['en_us'] if not k.startswith('_')}
        de_keys = {k for k in data['de_de'] if not k.startswith('_')}
        if present and en_keys != de_keys:
            errors.append(f'{ns}: EN/DE keys differ: only EN {sorted(en_keys - de_keys)}, only DE {sorted(de_keys - en_keys)}')
        regexes = {lang: [] for lang in LANGS}
        for lang in LANGS:
            for key, value in data[lang].items():
                if key.startswith('_'):
                    continue
                where = f'{ns} {lang} {key}'
                if not isinstance(value, dict):
                    errors.append(f'{where}: not an object')
                    continue
                if key == 'fallback':
                    for kind in KINDS:
                        for page in PAGES:
                            variants = value.get(kind, {}).get(page, [])
                            if len(variants) != VARIANTS:
                                errors.append(f'{where}.{kind}.{page}: {len(variants)} variants, expected {VARIANTS}')
                            for v in variants:
                                errors += [f'{where}.{kind}.{page}: {p}: {v[:40]}' for p in page_problems([v])]
                    continue
                if key.startswith('family:'):
                    for item in value.get('items', []):
                        if isinstance(item, str) and item.startswith('regex:'):
                            try:
                                regexes[lang].append(re.compile(item[len('regex:'):]))
                            except re.error as e:
                                errors.append(f'{where}: bad regex {item}: {e}')
                for page in PAGES:
                    if page in value:
                        errors += [f'{where} {page}: {p}' for p in page_problems(value[page])]
        items = items_of(ns, langs)
        if not items:
            continue
        def described(item, lang):
            entry = data[lang].get(item)
            if isinstance(entry, dict) and all(entry.get(p) for p in PAGES):
                return True
            return any(r.fullmatch(item) for r in regexes[lang])
        missing = sorted(i for i in items if not all(described(i, lang) for lang in LANGS))
        report.append((ns, len(items), len(missing)))
        if args.list == ns:
            print('\n'.join(f'{ns}:{i}' for i in missing))
    for ns, total, miss in report:
        print(f'{ns}: {total - miss}/{total} items with questionary text (EN+DE), {miss} missing')
    for e in errors:
        print('ERROR', e)
    missing_total = sum(m for _, _, m in report)
    if errors or (args.strict and missing_total):
        return 1
    print(f'questionary data valid ({missing_total} items still without text - warning only)' if not args.strict else 'questionary complete')
    return 0


if __name__ == '__main__':
    sys.exit(main())
