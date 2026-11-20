#!/usr/bin/env python3
"""Every config option has a name in EN and DE (N111, release audit N110: simplemaps 26, simpleshops 10, simpleearth 2 options
showed raw paths on the config screen and in the wiki).

SimpleLib's config screen (ConfigScreenModel / GenericConfigScreen, the Cloth key schema) names an option by
``text.autoconfig.<modid>.option.<path>``, its tooltip by the same key + ``.@Tooltip``, the screen by ``.title`` and a group
(path with a dot) by ``.category.<group>``; without a key it shows the raw path. For every module that announces a config to
``ConfigCommands`` (shared and fabric code) this reads the options the same way the wiki does (``wiki/module_extras.py``:
toJson/apply of JSON configs, fields of Gson configs) and requires, in en_us AND de_de: the title, every option name, every
group's category name (not for a module with its own Mod Menu screen, which names its tabs itself); a tooltip in one language
needs one in the other. Not required: the title (the screen falls back to the mod's name, not to a path). Not seen here: options
whose keys only exist at run time (maps filled in a loop: their entries become options of their own) and Gson fields that are
null by default (Gson does not write them) - the per-module GameTest ``ConfigScreenModel.missingLabels`` covers the loaded EN.
SimpleBuilding's own Cloth config is checked by the wiki generator (``--check``, undocumented options).
Usage: check_config_lang.py [module ...]   Exit 1 on a missing key.
"""
from __future__ import annotations

import glob
import json
import os
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO / 'wiki'))
import module_extras  # noqa: E402

LOCALES = ('en_us', 'de_de')


def lang_tables():
    tables = {loc: {} for loc in LOCALES}
    for loc in LOCALES:
        for path in sorted(glob.glob(str(REPO / '**' / 'lang' / f'{loc}.json'), recursive=True)):
            norm = path.replace(os.sep, '/')
            if '/build/' in norm or '/node_modules/' in norm or '/.git/' in norm:
                continue
            with open(path, encoding='utf-8') as f:
                tables[loc].update(json.load(f))
    return tables


def options_of(entry):
    """{path: option} the generic screen shows by a fixed key: maps (run-time entries) and null Gson fields left out."""
    _, _, opts = module_extras.code_options(REPO, entry, ('shared', 'fabric'))
    return {p: o for p, o in opts.items()
            if (o.get('type') or '').split('<')[0] not in ('object', 'Map', 'HashMap', 'LinkedHashMap', 'JsonObject')
            and not o.get('nullDefault')}


def own_screen(entry):
    """The module brings its own Mod Menu screen (ModMenuApi in its Fabric code)."""
    fabric = entry['paths'].get('fabric')
    if not fabric or not (REPO / fabric).is_dir():
        return False
    return any('ModMenuApi' in p.read_text(encoding='utf-8', errors='ignore')
               for p in (REPO / fabric).rglob('*.java') if '/build/' not in p.as_posix())


def problems_of(entry, tables, opts=None):
    """Missing keys of one module: (locale, key, why)."""
    if opts is None:
        opts = options_of(entry)
    mod = entry['id']
    out = []
    if not opts:
        return out
    base = f'text.autoconfig.{mod}.'
    groups = [] if own_screen(entry) else sorted({p.split('.')[0] for p in opts if '.' in p})
    for loc in LOCALES:
        t = tables[loc]
        for g in groups:
            if base + 'category.' + g not in t:
                out.append((loc, base + 'category.' + g, 'group name'))
        for path in sorted(opts):
            if base + 'option.' + path not in t:
                out.append((loc, base + 'option.' + path, 'option name'))
    for path in sorted(opts):
        tip = base + 'option.' + path + '.@Tooltip'
        have = [loc for loc in LOCALES if tip in tables[loc]]
        if len(have) == 1:
            other = LOCALES[1 - LOCALES.index(have[0])]
            out.append((other, tip, f'tooltip only in {have[0]}'))
    return out


def main(argv):
    entries = json.loads((REPO / 'modules' / 'modules.json').read_text(encoding='utf-8'))
    entries = entries if isinstance(entries, list) else entries['modules']
    entries = [e for e in entries if e['id'] != 'simplebuilding' and (not argv or e['id'] in argv)]
    tables = lang_tables()
    bad, mods, options = [], 0, 0
    for entry in entries:
        opts = options_of(entry)
        if not opts:
            continue
        mods += 1
        options += len(opts)
        bad += [(entry['id'], *p) for p in problems_of(entry, tables, opts)]
    for mod, loc, key, why in bad:
        print(f'ERROR {mod}: {loc} has no "{key}" ({why})')
    print(f'check_config_lang: {options} options of {mods} modules, {len(bad)} missing keys')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
