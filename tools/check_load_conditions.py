#!/usr/bin/env python3
"""Every conditional data JSON in the repo uses only load-condition ids its loader knows (run by checkModuleData).

Fabric reads "fabric:load_conditions" (key "condition"), NeoForge "neoforge:conditions" (key "type"), Forge 26.3 exactly
one object under "forge:condition" (net.minecraftforge.common.crafting.conditions.ICondition.DEFAULT_FIELD; a list under
"forge:conditions" is ignored by Forge itself). Module jars get "forge:condition" from gradle/module-forge.gradle when it
is missing: from "forge:conditions", else from "neoforge:conditions" with NeoForge built-ins renamed to their Forge twins.
This script applies that same rule. Gate 6d8630f4c: the rewrite turned an explicit forge:not into neoforge:not and Forge
refused the whole registry ("Unknown registry key ... forge:condition_codecs: neoforge:not").
"""
import glob
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKIP_DIRS = {'build', '.gradle', 'run', 'run-gametest', 'run-standalone', '.git', 'node_modules', 'graphify-out', 'bin', 'out'}
# Built-ins (Forge 26.3-66.0.8 ForgeMod CONDITION_SERIALIZERS; NeoForge NeoForgeMod; Fabric API ResourceConditions v1).
FORGE = {'forge:' + n for n in ('and', 'or', 'not', 'mod_loaded', 'item_exists', 'tag_empty', 'true', 'false')}
NEOFORGE = {'neoforge:' + n for n in ('and', 'or', 'not', 'mod_loaded', 'item_exists', 'tag_empty', 'true', 'false')}
FABRIC = {'fabric:' + n for n in ('and', 'or', 'not', 'true', 'all_mods_loaded', 'any_mods_loaded', 'tags_populated',
                                  'features_enabled', 'registry_contains')}
# Own conditions, registered on all three loaders (Forge CONDITION_SERIALIZERS, NeoForge CONDITION_CODECS, Fabric
# ResourceConditionType) by the named mod; a new one must be registered everywhere before it is added here.
CUSTOM = {'simplebuilding:config', 'simplefun:trades_enabled', 'simplelib:material_requested', 'simplemoney:config',
          'simpleriding:trades_enabled', 'simplesandwiches:enabled'}
RENAME = {n: 'forge:' + n.split(':', 1)[1] for n in NEOFORGE}


def to_forge(c):
    if isinstance(c, list):
        return [to_forge(x) for x in c]
    if not isinstance(c, dict):
        return c
    out = {k: to_forge(v) if k in ('value', 'values') else v for k, v in c.items()}
    if c.get('type') in RENAME:
        out['type'] = RENAME[c['type']]
    return out


def ids(c, key, found):
    if isinstance(c, list):
        for x in c:
            ids(x, key, found)
    elif isinstance(c, dict):
        found.append(c.get(key))
        for k in ('value', 'values'):  # nested conditions (not/and/or); mod id lists stay strings
            v = c.get(k)
            if isinstance(v, dict) or (isinstance(v, list) and v and all(isinstance(x, dict) for x in v)):
                ids(v, key, found)
    else:
        found.append(repr(c))
    return found


def problems_of(data):
    out = []
    checks = []
    if 'fabric:load_conditions' in data:
        checks.append(('fabric', data['fabric:load_conditions'], 'condition', FABRIC))
    if 'neoforge:conditions' in data:
        checks.append(('neoforge', data['neoforge:conditions'], 'type', NEOFORGE))
    forge = data.get('forge:condition')
    if forge is not None and not isinstance(forge, dict):
        out.append('"forge:condition" must be one object (join several with forge:and)')
    if forge is None and (data.get('forge:conditions') or data.get('neoforge:conditions')):
        conds = data.get('forge:conditions') or to_forge(data['neoforge:conditions'])
        forge = conds[0] if len(conds) == 1 else {'type': 'forge:and', 'values': conds}
    if forge is not None:
        checks.append(('forge', forge, 'type', FORGE))
    for loader, conds, key, builtin in checks:
        if loader != 'forge' and not isinstance(conds, list):
            out.append(f'{loader}: conditions must be a list')
            continue
        for cid in ids(conds, key, []):
            if cid not in builtin and cid not in CUSTOM:
                out.append(f'{loader}: unknown condition id {cid!r}')
    return out


def conditional_files(top, skip=SKIP_DIRS):
    for root, dirs, files in os.walk(top):
        dirs[:] = sorted(d for d in dirs if d not in skip)
        if os.sep + 'data' + os.sep not in root + os.sep:
            continue
        for name in sorted(files):
            if not name.endswith('.json'):
                continue
            path = os.path.join(root, name)
            try:
                data = json.load(open(path, encoding='utf-8'))
            except (ValueError, UnicodeDecodeError):
                continue
            if isinstance(data, dict) and any(k in data for k in (
                    'fabric:load_conditions', 'neoforge:conditions', 'forge:conditions', 'forge:condition')):
                yield path, data


def main():
    bad, count, built = [], 0, 0
    for path, data in conditional_files(REPO):
        count += 1
        bad.extend(f'{os.path.relpath(path, REPO)}: {p}' for p in problems_of(data))
    # Processed Forge resources, when a Forge build left them: exactly what Forge reads ("forge:condition" only).
    for top in sorted(glob.glob(os.path.join(REPO, '**', 'forge', 'build', 'resources', 'main'), recursive=True)):
        for path, data in conditional_files(top, SKIP_DIRS - {'build'}):
            built += 1
            forge = data.get('forge:condition')
            if forge is not None:
                bad.extend(f'{os.path.relpath(path, REPO)}: built {p}' for p in problems_of({'forge:condition': forge}))
    if bad:
        print('\n'.join(bad[:50]))
        sys.exit(f'load conditions: {len(bad)} problem(s) in {count} conditional JSON files (+{built} built Forge)')
    print(f'load conditions: {count} conditional JSON files (+{built} built Forge), ids valid on Fabric, NeoForge and Forge')


if __name__ == '__main__':
    main()
