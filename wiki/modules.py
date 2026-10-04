"""Manifest discovery and the data-only extraction contract for additional mods.

Complex/dynamic registries export generated/wiki/items.json (same format as
SimpleBuilding). Literal Java Identifier registrations, item definitions and
language keys also provide inventory evidence for small modules.
"""
import fnmatch
import hashlib
import json
import re
from pathlib import Path


def discover(repo):
    entries = json.loads((repo / 'modules/modules.json').read_text(encoding='utf-8'))['modules']
    seen = set()
    for entry in entries:
        mid = entry['id']
        if not re.fullmatch(r'[a-z][a-z0-9_-]*', mid) or mid in seen:
            raise ValueError(f'Invalid or duplicate module id: {mid}')
        seen.add(mid)
        if not re.fullmatch(r'[a-z][a-z0-9_-]*', entry.get('namespace', mid)):
            raise ValueError(f'Invalid registry namespace: {mid}')
        for key in ('name', 'displayName', 'description', 'version', 'loaders', 'minecraft', 'paths', 'requires', 'optional'):
            if key not in entry:
                raise ValueError(f'{mid}: missing manifest field {key}')
        for key in ('root', 'shared', 'fabric', 'neoforge', 'forge', 'generated', 'lang', 'wikiManual', 'balanceDir'):
            if key not in entry['paths']:
                raise ValueError(f'{mid}: missing manifest path {key}')
            value = entry['paths'][key]
            if value is not None and (Path(value).is_absolute() or '..' in Path(value).parts or ':' in value):
                raise ValueError(f'{mid}: unsafe path {key}')
        if entry['minecraft'] != '26.3' or not set(entry['loaders']) <= {'fabric', 'neoforge', 'forge'}:
            raise ValueError(f'{mid}: invalid compatibility')
    return entries


def trade_values(repo, resources, ns):
    """
    What an item is worth in emeralds, read from the module's own villager trades: every trade that
    takes exactly this item and gives emeralds (a set_count modifier on the emeralds widens the range).
    {id: {"emeralds": {"min", "max"}, "trades": n, "sources": [...]}} - only for items such a trade
    proves (owner 2026-10-02: the bill's value only when the code backs it).
    """
    found = {}
    for resource in resources:
        root = resource / 'data' / ns / 'villager_trade'
        if not root.exists():
            continue
        for path in sorted(root.rglob('*.json')):
            try:
                trade = json.loads(path.read_text(encoding='utf-8'))
            except (OSError, json.JSONDecodeError):
                continue
            wants, gives = trade.get('wants') or {}, trade.get('gives') or {}
            if trade.get('additional_wants') or gives.get('id') != 'minecraft:emerald' or not str(wants.get('id', '')).startswith(ns + ':'):
                continue
            paid, count = wants.get('count', 1), gives.get('count', 1)
            if not isinstance(paid, int) or paid < 1 or not isinstance(count, int):
                continue
            low = high = count
            for modifier in trade.get('given_item_modifier') or []:
                amount = modifier.get('count') if isinstance(modifier, dict) else None
                if modifier.get('type') == 'minecraft:set_count' and isinstance(amount, dict) and amount.get('type') == 'minecraft:uniform':
                    low, high = amount.get('min', low), amount.get('max', high)
                elif modifier.get('type') == 'minecraft:set_count' and isinstance(amount, (int, float)):
                    low = high = amount
            entry = found.setdefault(wants['id'], {'emeralds': {'min': None, 'max': None}, 'trades': 0, 'sources': []})
            per_low, per_high = low / paid, high / paid
            entry['emeralds']['min'] = per_low if entry['emeralds']['min'] is None else min(entry['emeralds']['min'], per_low)
            entry['emeralds']['max'] = per_high if entry['emeralds']['max'] is None else max(entry['emeralds']['max'], per_high)
            entry['trades'] += 1
            entry['sources'].append(path.relative_to(repo).as_posix())
    for entry in found.values():
        for key in ('min', 'max'):
            value = entry['emeralds'][key]
            entry['emeralds'][key] = int(value) if float(value).is_integer() else round(value, 2)
    return found


def extract(entry, g, check=False):
    """Return the UI schema and completeness problems; absent features are empty."""
    repo, mid, paths = g.REPO, entry['id'], entry['paths']
    ns = entry.get('namespace', mid)
    manual_path = repo / paths['wikiManual']
    manual = g.read_json(manual_path) if manual_path.exists() else {}
    lang = {locale: g.read_json(repo / paths['lang'] / (locale + '.json'))
            for locale in ('en_us', 'de_de') if (repo / paths['lang'] / (locale + '.json')).exists()}
    resources = [repo / paths['shared'] / 'resources']
    resources += [repo / paths[loader] / 'src/main/resources' for loader in entry['loaders'] if paths.get(loader)]
    generated = repo / paths['generated']
    roots = dict(generated_data=str(generated / 'data' / ns),
                 resource_data=str(resources[0] / 'data' / ns), namespace=ns)
    # Each resource layer overrides the earlier layer by id, matching loader resources.
    collections = {}
    for name, collector in [('recipes', g.collect_recipes), ('lootTables', g.collect_loot_tables), ('tags', g.collect_tags),
                            ('advancements', lambda roots: g.collect_advancements(roots, lang)),
                            ('enchantments', lambda roots: g.collect_enchantments(roots, lang)[0])]:
        values = {}
        for resource in [generated, *resources]:
            roots['generated_data'] = str(resource / 'data' / ns)
            roots['resource_data'] = str(repo / 'build/wiki-no-resource-layer')
            for value in collector(roots):
                if name == 'tags' and value['id'] in values and not value.get('replace'):
                    value['values'] = values[value['id']]['values'] + [v for v in value['values'] if v not in values[value['id']]['values']]
                values[value['id']] = value
        collections[name] = sorted(values.values(), key=lambda value: value['id'])
    inventory = {'items': set(), 'blocks': set()}
    model_only = manual.get('modelOnly', {})
    model_only_ids = set(model_only) if isinstance(model_only, dict) else set()
    item_models = set()
    for table in lang.values():
        for key in table:
            for kind, prefix in [('items', 'item'), ('blocks', 'block')]:
                if key.startswith(f'{prefix}.{ns}.') and '.' not in key.split('.', 2)[2]:
                    inventory[kind].add(ns + ':' + key.split('.', 2)[2])
    for resource in [*resources, generated]:
        for kind, directory in [('items', 'items'), ('blocks', 'blockstates')]:
            base = resource / 'assets' / ns / directory
            identifiers = {ns + ':' + p.relative_to(base).with_suffix('').as_posix() for p in base.rglob('*.json')}
            if kind == 'items':
                item_models.update(identifiers)
                identifiers -= model_only_ids
            inventory[kind].update(identifiers)
    props_path = generated / 'wiki/items.json'
    props = g.read_json(props_path) if props_path.exists() else {}
    if isinstance(props, dict) and 'items' in props:
        props = props['items']
    if isinstance(props, list):
        props = {e['id']: e for e in props}
    for identifier, value in props.items():
        if identifier.startswith(ns + ':'):
            inventory['blocks' if value.get('kind') == 'block' else 'items'].add(identifier)
    # The scaffold uses a literal identifier followed by a registry registration.
    for source in (repo / paths['shared']).rglob('*.java'):
        code = re.sub(r'/\*.*?\*/|//[^\n]*', '', source.read_text(encoding='utf-8'), flags=re.S)
        if 'Registry.register' not in code or 'BuiltInRegistries.' not in code:
            continue
        literal = r'Identifier\.fromNamespaceAndPath\(\s*"([\w]+)"\s*,\s*"([\w/]+)"\s*\)'
        for registration in re.finditer(
                r'Registry\.register\(\s*BuiltInRegistries\.(ITEM|BLOCK)\s*,\s*(' + literal + r'|\w+)\s*,', code):
            registry, argument = registration.group(1, 2)
            identifier = re.fullmatch(literal, argument)
            if identifier is None:
                # Resolve the nearest preceding literal assignment to this variable.
                assignments = list(re.finditer(r'\b' + re.escape(argument) + r'\s*=\s*(' + literal + r')\s*;', code[:registration.start()]))
                identifier = re.fullmatch(literal, assignments[-1].group(1)) if assignments else None
            if identifier and identifier.group(1) == ns:
                inventory['blocks' if registry == 'BLOCK' else 'items'].add(ns + ':' + identifier.group(2))
    # Block items inherit the block's translation key and are documented on the
    # block page; their item model/export must not demand a phantom item key.
    inventory['items'].difference_update(inventory['blocks'])
    notes, undocumented, incomplete, problems = manual.get('notes', {}), [], {}, []
    # Render aliases are not registry entries. Other evidence must never be hidden.
    if not isinstance(model_only, dict):
        problems.append(f'{paths["wikiManual"]}: modelOnly must map model ids to reasons')
    else:
        for identifier, reason in model_only.items():
            if not isinstance(reason, str) or not reason.strip():
                problems.append(f'{identifier}: modelOnly needs a nonempty reason')
            if identifier not in item_models:
                problems.append(f'{identifier}: modelOnly has no item model definition')
            if identifier in inventory['items'] or identifier in inventory['blocks']:
                problems.append(f'{identifier}: modelOnly conflicts with registry or language evidence')
    if not isinstance(notes, dict):
        problems.append(f'{paths["wikiManual"]}: notes must be an object keyed by item/block id or glob')
        notes = {}
    # An exact feature chapter can document its registry id without duplicating prose.
    feature_notes = {f["id"] if ':' in f["id"] else ns + ':' + f["id"]:
                     {locale: f[locale] for locale in ('en', 'de') if locale in f}
                     for f in manual.get('features', [])}
    notes = feature_notes | notes
    problems += g.item_note_problems(notes, inventory['items'] | inventory['blocks'])
    def record(identifier, note):
        languages = g.prose_languages(note)
        if not languages:
            undocumented.append(identifier)
        elif languages != {'en', 'de'}:
            incomplete[identifier] = sorted({'en', 'de'} - languages)
    for kind, ids in inventory.items():
        values = []
        for identifier in sorted(ids):
            name = identifier.split(':', 1)[1]
            note = next((v for k, v in notes.items() if fnmatch.fnmatchcase(identifier, k) or fnmatch.fnmatchcase(name, k)), None)
            note = g.item_note(note, identifier)
            record(identifier, note)
            prefix = 'item' if kind == 'items' else 'block'
            key = f'{prefix}.{ns}.{name}'
            if not all(key in table for table in lang.values()) or len(lang) != 2:
                problems.append(f'{identifier}: missing English/German name')
            value = dict(props.get(identifier, {}), id=identifier, name=g.display_name(lang, key, name), note=note)
            # Copy module-owned textures with namespace isolation. Vanilla-only
            # models remain text tiles, just like missing Vanilla images in CI.
            for resource in [*resources, generated]:
                texture = resource / 'assets' / ns / 'textures' / prefix / (name + '.png')
                if texture.exists():
                    import shutil
                    relative = f'assets/textures/{mid}/{prefix}/{name}.png'
                    target = g.WIKI / relative
                    if not check:
                        target.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copyfile(texture, target)
                    elif not target.exists() or target.read_bytes() != texture.read_bytes():
                        problems.append(f'{relative}: missing or stale texture')
                    value['texture'] = relative
            values.append(value)
        collections[kind] = values
    for feature in manual.get('features', []):
        record(feature['id'], feature)
    problems += g.duplicate_feature_ids(manual_path)
    # Recipes on the item pages (craftedBy/usedIn like the main mod) with their raw materials in total,
    # resolved through the module's own and the vanilla 26.3 recipes (owner 2026-10-02).
    g.annotate_base_materials('26.3', collections['recipes'], collections['tags'])
    made, used = {}, {}
    for recipe in collections['recipes']:
        result = (recipe.get('result') or {}).get('id')
        if result:
            made.setdefault(result, []).append(recipe['id'])
        for ingredient in recipe.get('ingredients', []):
            used.setdefault(ingredient, []).append(recipe['id'])
    values = trade_values(repo, [generated, *resources], ns)
    for kind in ('items', 'blocks'):
        for value in collections[kind]:
            value['craftedBy'] = sorted(made.get(value['id'], []))
            value['usedIn'] = sorted(used.get(value['id'], []))
            if value['id'] in values:
                value['value'] = values[value['id']]
    data = dict(schema=1, generatedFrom={'line': '26.3', 'generator': 'wiki/generate.py'},
                mod=dict(id=mid, name=entry['displayName'], version=entry['version'], minecraftLines=['26.3'], loaders=entry['loaders']),
                features=manual.get('features', []), **collections)
    for key in ('trades', 'config', 'quests', 'recipesOtherLines'):
        data[key] = []
    data.update(inWorld={'entries': [], 'kinds': []}, obtain={'sources': []},
                undocumented=sorted(set(undocumented)), incompleteProse=incomplete)
    # Optional loader-neutral datagen export for features that cannot be parsed
    # safely from arbitrary Java implementations (e.g. trades/config/registries).
    export_path = generated / 'wiki/data.json'
    if export_path.exists():
        export = g.read_json(export_path)
        for key in ('trades', 'enchantments', 'config', 'advancements', 'quests', 'inWorld', 'obtain'):
            if key in export:
                data[key] = export[key]
    for enchantment in data['enchantments']:
        identifier = enchantment['id']
        enchantment['note'] = notes.get(identifier, enchantment.get('note'))
        record(identifier, enchantment['note'])
    for kind in data['inWorld'].get('kinds', []):
        record('inWorld:' + kind['id'], kind.get('note'))
    data['undocumented'] = sorted(set(undocumented))
    data['counts'] = {key: len(value) for key, value in data.items() if isinstance(value, list)}
    data['counts'].update(inWorld=len(data['inWorld']['entries']), incompleteProse=len(incomplete))
    return data, problems


def sync(g, entries, selected, check=False, strict=False):
    problems = []
    outputs = {}
    for entry in entries:
        if entry['id'] == 'simplebuilding' or entry['id'] not in selected:
            continue
        data, errors = extract(entry, g, check)
        problems += [f"{entry['id']}: {error}" for error in errors]
        if check or strict:
            problems += [f"{entry['id']}: undocumented {identifier}" for identifier in data['undocumented']]
            problems += [f"{entry['id']}: incomplete prose {identifier}" for identifier in data['incompleteProse']]
        payload = json.dumps(data, indent=2, ensure_ascii=False) + '\n'
        outputs[entry['id'] + '.json'] = payload
        outputs[entry['id'] + '.js'] = 'window.WIKI_MODULE_DATA = window.WIKI_MODULE_DATA || {};\nwindow.WIKI_MODULE_DATA[' + json.dumps(entry['id']) + '] = ' + payload.rstrip() + ';\n'
    # Metadata remains separate to preserve SimpleBuilding's existing payload bytes.
    properties = dict(re.findall(r'^([\w]+)=(.*)$', (g.REPO / 'gradle.properties').read_text(encoding='utf-8'), re.M))
    catalog = [{k: e[k] for k in ('id', 'displayName', 'description', 'version', 'minecraft', 'loaders', 'requires', 'optional')} for e in entries]
    for e in catalog:
        e['version'] = re.sub(r'\$\{(\w+)\}', lambda m: properties.get(m[1], m[0]), e['version'])
        if e['id'] != 'simplebuilding':
            name = e['id'] + '.js'
            target = g.WIKI / 'data' / name
            script = outputs.get(name, target.read_text(encoding='utf-8') if target.exists() else '')
            e['dataHash'] = hashlib.sha256(script.encode('utf-8')).hexdigest()[:12]
    outputs['modules.js'] = 'window.WIKI_MODULES = ' + json.dumps(catalog, indent=2, ensure_ascii=False) + ';\n'
    for name, payload in outputs.items():
        target = g.WIKI / 'data' / name
        if check:
            if not target.exists() or target.read_text(encoding='utf-8') != payload:
                problems.append(f'wiki/data/{name} is OUT OF DATE')
        else:
            g.write_atomic(target, payload)
    for problem in problems:
        print('PROBLEM:', problem)
    return int(bool(problems))
