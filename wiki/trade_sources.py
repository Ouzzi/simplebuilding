"""Trade availability for the documented Vanilla + one-mod default profile."""
import json
import zipfile
from pathlib import Path


def offer_probability(predicate):
    if predicate is None:
        return 1.0
    if (isinstance(predicate, dict)
            and predicate.get('type', predicate.get('condition')) == 'minecraft:random_chance'
            and isinstance(predicate.get('chance'), (int, float))
            and 0 <= predicate['chance'] <= 1):
        return predicate['chance']
    return None


def availability(probabilities, target, draws):
    """Uniform removal, retry failures, stop after draws successful offers (26.3)."""
    if any(p is None for p in probabilities) or draws < 0:
        return None
    distribution = [1.0]
    for index, p in enumerate(probabilities):
        if index == target:
            continue
        following = [0.0] * (len(distribution) + 1)
        for k, mass in enumerate(distribution):
            following[k] += mass * (1 - p)
            following[k + 1] += mass * p
        distribution = following
    return probabilities[target] * sum(mass * min(1, draws / (k + 1))
                                       for k, mass in enumerate(distribution))


def availability_bounds(probabilities, target, draws):
    """Unknown competing offers can only lower availability when they succeed."""
    low = [1 if p is None else p for p in probabilities]
    high = [0 if p is None else p for p in probabilities]
    low[target] = 0 if probabilities[target] is None else probabilities[target]
    high[target] = 1 if probabilities[target] is None else probabilities[target]
    return [availability(low, target, draws), availability(high, target, draws)]


def can_discard(data):
    # Vanilla getOffer also returns null if an item modifier empties the stack.
    # Preserve that uncertainty instead of treating every absent predicate as success.
    modifier = data.get('given_item_modifier', data.get('given_item_modifiers'))
    def discards(value):
        if isinstance(value, list):
            return any(discards(v) for v in value)
        if isinstance(value, dict):
            kind = value.get('type', value.get('function'))
            safe = {'minecraft:enchant_randomly', 'minecraft:enchant_with_levels',
                    'minecraft:set_random_dyes', 'minecraft:set_potion',
                    'minecraft:set_random_potion', 'minecraft:set_stew_effect',
                    'simplebuilding:weighted_enchant'}
            return kind not in safe
        return value is not None
    return discards(modifier)


def vanilla_data(jar):
    """Keep original JSON evidence; exclude experimental built-in datapacks."""
    result = {'version': '26.3', 'sets': {}, 'tags': {}, 'predicates': {}, 'mayDiscard': []}
    with zipfile.ZipFile(jar) as archive:
        for name in sorted(archive.namelist()):
            if not name.startswith('data/minecraft/') or not name.endswith('.json'):
                continue
            for folder, key in (('trade_set', 'sets'), ('tags/villager_trade', 'tags'),
                                ('villager_trade', 'predicates')):
                prefix = 'data/minecraft/' + folder + '/'
                if not name.startswith(prefix):
                    continue
                data = json.loads(archive.read(name))
                identifier = 'minecraft:' + name[len(prefix):-5]
                result[key][identifier] = data.get('merchant_predicate') if key == 'predicates' else data
                if key == 'predicates' and can_discard(data):
                    result['mayDiscard'].append(identifier)
    return result


def sync_cache(jar, cache, check=False):
    if not jar.exists():
        return [] if cache.exists() else ['Missing Vanilla trade cache: ' + str(cache)]
    data = vanilla_data(jar)
    if check:
        return [] if cache.exists() and json.loads(cache.read_text(encoding='utf-8')) == data else ['Vanilla trade cache is OUT OF DATE']
    cache.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return []


def annotate(trades, data_roots, cache):
    """Merge tags exactly once per resource root; optional missing trades are omitted."""
    vanilla = json.loads(Path(cache).read_text(encoding='utf-8'))
    tags = dict(vanilla['tags'])
    sets = dict(vanilla['sets'])
    predicates = dict(vanilla['predicates'])
    may_discard = set(vanilla.get('mayDiscard', []))
    for root in data_roots:
        for namespace in sorted(Path(root).glob('*')):
            if not namespace.is_dir():
                continue
            for folder, into in (('villager_trade', predicates), ('trade_set', sets),
                                 ('tags/villager_trade', tags)):
                for path in sorted((namespace / folder).rglob('*.json')):
                    identifier = namespace.name + ':' + path.relative_to(namespace / folder).with_suffix('').as_posix()
                    value = json.loads(path.read_text(encoding='utf-8'))
                    if folder == 'villager_trade':
                        if can_discard(value):
                            may_discard.add(identifier)
                        else:
                            may_discard.discard(identifier)
                        value = value.get('merchant_predicate')
                    elif folder.startswith('tags/') and not value.get('replace'):
                        value = {'values': tags.get(identifier, {}).get('values', []) + value.get('values', [])}
                    into[identifier] = value

    def members(tag, visiting=()):
        if tag in visiting or tag not in tags:
            raise ValueError('Unresolved/cyclic trade tag: ' + tag)
        out = []
        for entry in tags[tag].get('values', []):
            identifier = entry if isinstance(entry, str) else entry['id']
            optional = isinstance(entry, dict) and not entry.get('required', True)
            if identifier.startswith('#'):
                if optional and identifier[1:] not in tags:
                    continue
                out += members(identifier[1:], visiting + (tag,))
            elif identifier in predicates:
                out.append(identifier)
            elif not optional:
                raise ValueError('Missing required trade: ' + identifier)
        return list(dict.fromkeys(out))

    by_trade = {}
    for identifier, trade_set in sets.items():
        ref = trade_set.get('trades')
        if not isinstance(ref, str) or not ref.startswith('#'):
            continue
        pool = members(ref[1:])
        draws = trade_set.get('amount')
        known = isinstance(draws, int) and not trade_set.get('allow_duplicates', False)
        probabilities = [None if t in may_discard else offer_probability(predicates[t]) for t in pool]
        for index, trade in enumerate(pool):
            by_trade.setdefault(trade, []).append({
                'id': identifier, 'poolSize': len(pool), 'draws': draws,
                'chance': availability(probabilities, index, draws) if known else None,
                'bounds': availability_bounds(probabilities, index, draws) if known else None,
            })
    for trade in trades:
        pools = by_trade.get(trade['id'], [])
        trade['availability'] = {'profile': 'vanilla-plus-selected-mod', 'pools': pools,
                                 'chance': pools[0]['chance'] if len(pools) == 1 else None,
                                 'bounds': pools[0]['bounds'] if len(pools) == 1 else None}
