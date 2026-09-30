"""Conservative adapter for ordinary item/empty JSON loot pools.

Unsupported conditions/functions are reported instead of producing optimistic acquisition times.
"""
from .values import value, problem


def tables(files, records, name):
    index = {(r['source'].get('file'), tuple(r['source'].get('path', []))): r['id'] for r in records}
    result, sources, assumptions, problems = [], {}, [], []
    for rel, ident, data in files:
        pools = []
        try:
            if data.get('functions'):
                raise ValueError('table functions')
            for pi, pool in enumerate(data.get('pools', [])):
                if pool.get('conditions') or pool.get('functions') or pool.get('bonus_rolls', 0) != 0:
                    raise ValueError('conditions, functions or bonus rolls')
                raw = pool.get('rolls', 1)
                root = ('pools', pi)
                if isinstance(raw, (int, float)) and raw == int(raw):
                    rolls = {'type': 'exactly', 'n': raw, 'ids': {'n': index.get((rel, root + ('rolls',)))}}
                elif isinstance(raw, dict) and raw.get('type') == 'minecraft:uniform':
                    rolls = {'type': 'uniform', 'min': raw['min'], 'max': raw['max'], 'ids':
                             {k: index.get((rel, root + ('rolls', k))) for k in ('min', 'max')}}
                else:
                    raise ValueError('roll provider')
                entries = []
                for ei, entry in enumerate(pool.get('entries', [])):
                    kind = entry.get('type')
                    if kind not in ('minecraft:item', 'minecraft:empty') or entry.get('conditions'):
                        raise ValueError('entry type or conditions')
                    count, ids = [1, 1], {'weight': index.get((rel, root + ('entries', ei, 'weight')))}
                    for fi, fn in enumerate(entry.get('functions', [])):
                        if fn.get('function') != 'minecraft:set_count' or fn.get('conditions') or fn.get('add'):
                            raise ValueError('entry function')
                        c = fn['count']
                        key = root + ('entries', ei, 'functions', fi, 'count')
                        if isinstance(c, (int, float)):
                            count = [c, c]
                            ids.update(min=index.get((rel, key)), max=index.get((rel, key)))
                        elif isinstance(c, dict) and c.get('type') == 'minecraft:uniform':
                            count = [c['min'], c['max']]
                            ids.update({k: index.get((rel, key + (k,))) for k in ('min', 'max')})
                        else:
                            raise ValueError('count provider')
                    item = entry.get('name') if kind == 'minecraft:item' else None
                    entries.append({'item': item, 'key': str(ei), 'weight': entry.get('weight', 1),
                                    'count': count if item else [0, 0], 'ids': ids,
                                    'name': name(item) if item else {'de': 'leer', 'en': 'empty'}})
                pools.append({'rolls': rolls, 'entries': entries, 'line': 1})
        except (ValueError, KeyError, TypeError) as err:
            problems.append(problem('loot', 'Nicht modellierte JSON-Beute: ' + str(err), file=rel,
                                    why='Zahlen bleiben sichtbar; keine Beschaffungszeit ohne passenden Adapter.'))
            continue
        result.append({'id': ident, 'label': ident, 'kind': 'json', 'pools': pools, 'file': rel})
        param = 'loot.' + ident
        assumptions.append(value('param:' + param, 'param', ident + ' Ereignisse/h', 'float', 1.0,
                                 group='Mod-Beute', apply='tool', min=0, max=1000,
                                 note='Annahme: Öffnungen, Abbau oder Tötungen pro Stunde; vom Besitzer einstellen.'))
        for item in {e['item'] for p in pools for e in p['entries'] if e['item']}:
            sources.setdefault(item, []).append({'key': 'loot:' + ident, 'kind': 'loot', 'table': ident,
                                                 'param': param, 'label': ident})
    return result, sources, assumptions, problems
