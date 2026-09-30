"""Module constants and data; no global extractor settings or foreign write sites."""
import json
import time
from . import ex_constants, ex_data, ex_trades, jsonedit, params, vanilla, ex_jsonloot, icons
from .values import CATEGORIES, value, problem
from .modules import resources


def build(repo, module):
    started = time.time()
    mid, paths = module['id'], module['paths']
    roots = [paths['shared']] + [paths[k] + '/src/main/java' for k in ('fabric', 'neoforge', 'forge') if paths.get(k)]
    _, records, problems = ex_constants.scan(repo, roots)
    pools, vanilla_names, notes = vanilla.load(repo)
    lang = {}
    for locale in ('en_us', 'de_de'):
        path = repo / paths['lang'] / (locale + '.json')
        if path.is_file():
            lang[locale] = json.loads(path.read_text(encoding='utf-8'))

    def name(ident):
        ns, short = ident.split(':', 1)
        fallback = short.replace('_', ' ').title()
        return {locale: next((lang.get(code, {}).get(f'{kind}.{ns}.{short}') for kind in ('item', 'block')
                              if lang.get(code, {}).get(f'{kind}.{ns}.{short}')), fallback)
                for locale, code in (('en', 'en_us'), ('de', 'de_de'))}

    exported_items = []
    if (repo / paths['generated'] / 'wiki/items.json').is_file():
        exported_items, item_values, p = ex_data.extract_items(repo, ex_data.Names(lang, vanilla_names, mid), module)
        records += item_values
        problems += p

    recipes, recipe_values, p = ex_data.extract_recipes(repo, module)
    records += recipe_values
    problems += p
    trades = []
    for resource_root in resources(module):
        if not (repo / resource_root / 'data' / mid / 'villager_trade').is_dir():
            continue
        found, trade_values, p = ex_trades.extract(repo, pools, module, resource_root)
        trades += found
        records += trade_values
        problems += p
    # Only hand-written data is writable. Generated numbers remain planning values unless a
    # producer supplies a named Java constant; never rewrite generated output behind datagen.
    occupied = {(r['source'].get('file'), tuple(r['source'].get('path', []))) for r in records}
    loot_files = []
    data_roots = [(repo / root / 'data' / mid, False) for root in resources(module)]
    data_roots.append((repo / paths['generated'] / 'data' / mid, True))
    for base, generated in data_roots:
        for path in sorted(base.rglob('*.json')) if base.exists() else []:
            rel = path.relative_to(repo).as_posix()
            try:
                text = path.read_text(encoding='utf-8')
                data = json.loads(text)
                spans = jsonedit.scan(text)
            except (ValueError, jsonedit.JsonEditError) as err:
                problems.append(problem('data', str(err), file=rel))
                continue
            area = path.relative_to(base).parts[0]
            category = {'loot_table': 'loot', 'recipe': 'recipe', 'villager_trade': 'trade',
                        'worldgen': 'worldgen', 'enchantment': 'enchant'}.get(area, 'config')
            if area == 'loot_table':
                loot_files.append((rel, mid + ':' + path.relative_to(base / 'loot_table').with_suffix('').as_posix(), data))
            for key, span in spans.items():
                current = jsonedit.get(data, list(key))
                if type(current) not in (int, float, bool) or (rel, key) in occupied:
                    continue
                kind = 'bool' if type(current) is bool else 'int' if type(current) is int else 'float'
                probability = key and str(key[-1]) in ('chance', 'probability', 'reputation_discount')
                records.append(value(f'data:{rel}:{json.dumps(key, separators=(",", ":"))}', category,
                                     '/'.join(map(str, key)), 'prob' if probability else kind, current,
                                     group=path.stem, apply='plan' if generated else 'mod', min=0 if current >= 0 else None,
                                     max=1 if probability else None,
                                     source={'file': rel, 'path': list(key), 'generated': generated, 'line': jsonedit.line_of(text, span['start'])}))
    loot, sources, assumptions, p = ex_jsonloot.tables(loot_files, records, name)
    records += assumptions
    problems += p
    display = {ident: {'name': name(ident), 'icon': None} for ident in sources}
    for item in exported_items:
        display[item['id']] = {'name': item['name'], 'icon': None}
    for recipe in recipes:
        ident = recipe['result']['id']
        if ident:
            recipe['name'] = name(ident)
            display[ident] = {'name': name(ident), 'icon': None}
        for ingredient in recipe['ingredients']:
            for ident in ingredient['id'].split(' / '):
                if ident and not ident.startswith('#'):
                    display.setdefault(ident, {'name': name(ident), 'icon': None})
    for trade in trades:
        for field, label in (('gives', 'givesName'), ('wants', 'wantsName'), ('alsoWants', 'alsoWantsName')):
            ident = (trade.get(field) or {}).get('id')
            trade[label] = name(ident) if ident else {'de': 'leer', 'en': 'empty'}
            if ident:
                display.setdefault(ident, {'name': name(ident), 'icon': None})
        ident = (trade.get('gives') or {}).get('id')
        if ident:
            sources.setdefault(ident, []).append({'key': 'trade:' + trade['id'], 'kind':
                'wandering' if trade['profession'] == 'wandering_trader' else 'villager', 'trade': trade['id']})
            display[ident] = {'name': name(ident), 'icon': None}
    records += params.param_records([], [])
    values = {}
    for record in records:
        record['lines'] = ['26.3'] if record['apply'] == 'mod' else []
        record['source']['lines'] = ['26.3']
        record['source'].pop('twins', None)
        if record['category'] == 'recipe' and not record['source'].get('generated'):
            record['apply'] = 'mod'
        if record['id'] in values:
            problems.append(problem('constant', 'Duplicate value id: ' + record['id'], file=record['source'].get('file', '')))
        else:
            values[record['id']] = record
    resolver = icons.Resolver(repo)
    for ident, entry in display.items():
        entry['icon'] = (resolver.resolve(ident) or {}).get('icon')
    counts = {cat: sum(r['category'] == cat for r in values.values()) for cat in CATEGORIES}
    return {'schema': 1, 'line': '26.3', 'module': module, 'builtAt': time.strftime('%Y-%m-%d %H:%M:%S'),
            'buildSeconds': round(time.time() - started, 2), 'values': values, 'counts': counts,
            'items': [{'id': ident, 'name': entry['name'], 'icon': entry['icon'],
                       'stats': next((i['stats'] for i in exported_items if i['id'] == ident), {}), 'family': mid}
                      for ident, entry in display.items() if ident.startswith(mid + ':')],
            'display': display, 'books': [], 'loot': {'tables': loot, 'coreChances': {}}, 'trades': trades,
            'recipes': recipes, 'enchantments': [], 'worldgen': [], 'blockDrops': [], 'mobDrops': [],
            'config': [], 'potionPads': [], 'sources': sources,
            'structures': {k: {'label': s['label'], 'containers': {c: {'label': v['label']} for c, v in s['containers'].items()}}
                           for k, s in params.STRUCTURES.items()},
            'report': {'problems': problems, 'notes': notes, 'gaps': [
                {'area': 'Datagen', 'message': 'Generated data stays read-only planning.',
                 'why': 'Named Java constants and hand-written JSON are writable. Custom Java builders and loot conditions need an extractor adapter.'}]}}


def fingerprint(repo, module):
    roots = resources(module) + [module['paths'][key] for key in ('shared', 'fabric', 'neoforge', 'forge', 'generated', 'lang')
                                 if module['paths'].get(key)]
    return max((p.stat().st_mtime for root in roots for p in (repo / root).rglob('*') if p.is_file()), default=0)
