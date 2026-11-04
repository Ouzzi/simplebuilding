"""Create a 26.3 module that builds and passes the gates right away (B2, b-newmod01).

    python3 tools/newmod.py simplex --name "Simple X"                              # stand-alone Fabric module
    python3 tools/newmod.py simplex --name "Simple X" --parent simplebuilding --bundle required --config-switch true
    options: --bundle required|optional   --config-switch true|host|false   --loaders fabric[,neoforge,forge]
             --enderite  (items belong to the Enderite rule)   --description TEXT   --dry-run

Writes the skeleton from tools/templates/module (token item, Fabric entry, smoke GameTest, client smoke, lang EN/DE, wiki
manual EN/DE, check_data.py, framework + SimpleLib nesting), the modules.json entry (checks, tests, standalone tests), the
price table of Simple Money, enabled-mods.json, the placeholder icon (tools/textures/mod_icons.py) and TcContext.FAMILY.
For a SimpleBuilding sub-mod it also fills parent/bundle/configSwitch, SB `requires`/`optional`, the switch lang texts
and runs tools/submods/sync.py. Then it runs tools/modtree/modtree.py and tools/textures/mod_icons.py (generated files).
What stays by hand is printed as a checklist at the end.
"""
import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from multimod import ROOT, ID, registries, selection, write

ICON_ROWS = ("................", "......oo........", ".....oFFo.......", "....oFfFFo......", "...oFfFFFFo.....", "..oFfFFFFFFo....",
             ".oFFFFFFFFFFo...", ".oFFFFFFFFFFo...", "..oFFFFFFFFo....", "...oFFFFFFo.....", "....oFFFFo......", ".....oFFo.......",
             "......oo........", "................", "................", "................")
TASK = lambda mid: mid.capitalize()


def cls_of(mid):
    return ''.join(w.capitalize() for w in mid.removeprefix('simple').split('_')) or mid.capitalize()


def manifest_entry(mid, name, loaders, description, parent, bundle, switch):
    fab = 'fabric' in loaders
    suites = {}
    standalone = {}
    if 'fabric' in loaders:
        suites['fabric'] = dict(task=f':integration:run{TASK(mid)}GameTest', report=f'integration/build/{mid}-junit.xml')
        standalone['fabric'] = dict(task=f':integration:run{TASK(mid)}StandaloneGameTest', report=f'integration/build/standalone/{mid}-junit.xml')
    if 'neoforge' in loaders:
        suites['neoforge'] = dict(task=f':modules:{mid}:neoforge:runModuleIntegrationGameTest', report=f'modules/{mid}/neoforge/build/module-junit.xml')
        standalone['neoforge'] = dict(task=f':modules:{mid}:neoforge:runModuleStandaloneGameTest', report=f'modules/{mid}/neoforge/build/standalone-junit.xml')
    tests = dict(namespace=mid, catalogues=[f'modules/{mid}/fabric/src/main/java/com/simplebuilding/modules/{mid}/ModuleGameTest.java'],
                 requires=[mid], devMods=[], loaders=suites,
                 # Principle 8: the same suite with only this module (+ hard requirements) loaded.
                 standalone=dict(requires=[], devMods=[], loaders=standalone))
    if fab:
        tests['client'] = dict(entrypoints=[f'com.simplebuilding.modules.{mid}.ModuleClientSmoke'], sources=f'modules/{mid}/clienttest/java',
                               screenshots='integration/run-fabric-263/screenshots', task=':integration:runClientGameTest')
    entry = dict(id=mid, name=name, version='0.1.0', minecraft='26.3', defaultEnabled=True, displayName=name,
                 description=description, loaders=list(loaders), requires=['simplelib'], optional=[parent] if parent else [],
                 checks=[f'modules/{mid}/tools/check_data.py'], tests=tests,
                 paths=dict(root=f'modules/{mid}', shared=f'modules/{mid}/shared',
                            **{l: (f'modules/{mid}/{l}' if l in loaders else None) for l in ('fabric', 'neoforge', 'forge')},
                            generated=f'modules/{mid}/generated/resources',
                            lang=f'modules/{mid}/shared/resources/assets/{mid}/lang',
                            wikiManual=f'modules/{mid}/wiki/manual.json', balanceDir=f'balance/{mid}'),
                 projects={l: f':modules:{mid}:{l}' for l in loaders})
    if parent:
        entry.update(parent=parent, bundle=bundle, configSwitch=switch)
    return entry


def scaffold(mid, name, root, loaders, cls, parent):
    template = root / 'tools/templates/module'
    dest = root / 'modules' / mid
    for src in template.rglob('*'):
        rel = src.relative_to(template)
        if not src.is_file() or (rel.parts[0] in ('neoforge', 'forge') and rel.parts[0] not in loaders):
            continue
        path = dest / rel.as_posix().replace('wiringexample', mid).replace('__CLASS__', cls)
        path.parent.mkdir(parents=True, exist_ok=True)
        if src.suffix == '.nbt':
            path.write_bytes(src.read_bytes())
        else:
            path.write_text(src.read_text(encoding='utf-8').replace('__MODID__', mid).replace('__CLASS__', cls)
                            .replace('__DISPLAY_NAME__', name), encoding='utf-8')
    fmj = dest / 'fabric/src/main/resources/fabric.mod.json'
    if fmj.is_file():
        data = json.loads(fmj.read_text(encoding='utf-8'))
        data['icon'] = f'assets/{mid}/icon.png'
        data['depends'].update({'fabric-api': '*', 'simplelib': '>=0.3.0'})
        if parent:
            data['suggests'] = {parent: '*'}
        fmj.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def add_after(path, anchor, line, notes, label):
    """Insert `line` after the first line starting with `anchor`; skipped (and listed) when the anchor is missing."""
    text = path.read_text(encoding='utf-8') if path.is_file() else ''
    if line.strip() in text:
        return
    pos = text.find(anchor)
    if pos < 0:
        notes.append(f'BY HAND: {label} ({path.name}: anchor {anchor!r} not found)')
        return
    end = text.index('\n', pos) + 1
    path.write_text(text[:end] + line + '\n' + text[end:], encoding='utf-8')


def wire(mid, name, root, switch, parent, bundle, enderite, notes):
    # Simple Money: every module needs a price table (empty prices with a reason are allowed).
    prices = root / f'modules/simplemoney/shared/resources/data/simplemoney/money/{mid}/prices.json'
    prices.parent.mkdir(parents=True, exist_ok=True)
    write(prices, dict(module=mid, derivation='Placeholder module with a plain token item; nothing is sold yet.',
                       excluded={f'{mid}:token': 'Integration token, not a trade good.'}, prices=[]))
    # Test centre: the namespace belongs to the SimpleBuilding family.
    tc = root / 'common/src/shared/java/com/simplebuilding/dev/testcentre/TcContext.java'
    if tc.is_file():
        text = tc.read_text(encoding='utf-8')
        m = re.search(r'(FAMILY\s*=\s*[^;]*?"[a-z0-9_]+")\s*\);', text, re.S)
        if m and f'"{mid}"' not in text[m.start():m.end()]:
            tc.write_text(text[:m.end(1)] + f', "{mid}"' + text[m.end(1):], encoding='utf-8')
        elif not m:
            notes.append('BY HAND: TcContext.FAMILY (list not recognised)')
    # Placeholder icon and background colour (A8 c: every mod has a 16x16 icon); tools/textures/mod_icons.py draws the files.
    icons = root / 'tools/textures/mod_icons.py'
    if icons.is_file():
        pal = '{"o": "#2a2a30", "F": "#e8e8f0", "f": "#ffffff"}'
        rows = (',\n' + ' ' * 25).join(f'"{r}"' for r in ICON_ROWS)
        add_after(icons, 'ICONS: dict[str, dict] = {', f'    "{mid}": icon("Platzhalter-Raute", {pal},\n'
                  f'                         [("Kontur", "o"), ("Fuellung", "Ff")],\n                         {rows}),', notes, 'icon entry')
        add_after(icons, 'BG = {', f'    "{mid}": "#8a9098",', notes, 'icon background colour')
        notes.append(f'Placeholder icon for {mid}: redraw before release (tools/textures/mod_icons.py, ICONS/BG entry; A8b).')
    if enderite:
        tags = root / 'common/src/shared/java/com/simplebuilding/util/ModTags.java'
        add_after(tags, 'public static final Set<String> ENDERITE_NAMESPACES', f'                "{mid}",', notes, 'ENDERITE_NAMESPACES')
    if parent == 'simplebuilding':
        if switch is True:
            field = 'enableSimple' + mid[6:].capitalize()
            for loc, tip in (('en_us', f'Turns the bundled {name} module on or off. Off hides the module\'s creative entries; existing items keep working. Takes effect after a restart. Default: on.'),
                             ('de_de', f'Schaltet das gebündelte Modul {name} an oder aus. Aus blendet die Kreativ-Einträge des Moduls aus; vorhandene Gegenstände funktionieren weiter. Wirkt nach einem Neustart. Standard: an.')):
                path = root / f'mc26_3/overlay/resources/assets/simplebuilding/lang/{loc}.json'
                if not path.is_file():
                    notes.append(f'BY HAND: switch lang texts ({path})')
                    continue
                lang = json.loads(path.read_text(encoding='utf-8'))
                k = f'text.autoconfig.simplebuilding.option.{field}'
                lang[k] = f'Enable {name}' if loc == 'en_us' else f'{name} aktivieren'
                lang[k + '.@Tooltip'] = tip
                path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


def run(root, script, *args, notes):
    path = root / script
    if not path.is_file():
        notes.append(f'BY HAND: run {script} {" ".join(args)} (missing)')
        return
    r = subprocess.run([sys.executable, str(path), *args], cwd=root, capture_output=True, text=True)
    if r.returncode:
        notes.append(f'FAILED {script}: {(r.stdout + r.stderr).strip()[-400:]}')


def create(mid, name, root=ROOT, loaders=('fabric', 'neoforge', 'forge'), parent=None, bundle='optional', switch=True,
           enderite=False, description=None, do_wire=False):
    """Scaffold + manifest entry (+ repo wiring with do_wire). Returns the list of notes (checklist / failures)."""
    modules, dev = registries(root)
    if not re.fullmatch(r'[a-z][a-z0-9_]{1,63}', mid) or mid in {e['id'] for e in modules + dev}:
        raise ValueError('Use a unique lowercase mod id (letters, digits, underscores).')
    if not name.strip() or len(name) > 80 or any(ord(c) < 32 for c in name) or any(c in name for c in '\\"'):
        raise ValueError('Display name must be 1–80 printable characters without quotes or backslashes.')
    loaders = tuple(loaders)
    if 'fabric' not in loaders or not set(loaders) <= {'fabric', 'neoforge', 'forge'}:
        raise ValueError('Loaders: fabric plus optionally neoforge, forge.')
    if parent not in (None, 'simplebuilding') or bundle not in ('required', 'optional') or switch not in (True, False, 'host'):
        raise ValueError('--parent simplebuilding|none, --bundle required|optional, --config-switch true|host|false.')
    if parent == 'simplebuilding' and not re.fullmatch(r'simple[a-z]+', mid) and switch is True:
        raise ValueError('A sub-mod switch needs the id simple<word> (letters only), or --config-switch false.')
    dest = root / 'modules' / mid
    if dest.exists():
        raise ValueError(f'Already exists: {dest}')
    notes = []
    cls = cls_of(mid)  # entry classes: <Cls>Fabric/<Cls>NeoForge/<Cls>Forge
    scaffold(mid, name, root, loaders, cls, parent)
    modules.append(manifest_entry(mid, name, loaders, description or f'{name} module.', parent, bundle, switch))
    if parent:  # SimpleBuilding lists its sub-mods (bundled jar-in-jar vs. suggested)
        next(m for m in modules if m['id'] == parent)['requires' if bundle == 'required' else 'optional'].append(mid)
    write(root / 'modules/modules.json', {'schemaVersion': 1, 'modules': modules})
    enabled = selection(root)
    enabled['modules'].append(mid)
    write(root / 'integration/enabled-mods.json', enabled)
    if do_wire:
        wire(mid, name, root, switch, parent, bundle, enderite, notes)
        if parent == 'simplebuilding':
            run(root, 'tools/submods/sync.py', notes=notes)
        # only this module's icon files (a full run would also refresh other modules' derived item icons)
        code = ('import sys; sys.path.insert(0, "tools/textures"); import mod_icons as m\n'
                f'for e in m.modules():\n if e["id"] == {mid!r}:\n  for p, d in m.outputs(e).items(): p.parent.mkdir(parents=True, exist_ok=True); p.write_bytes(d)\n')
        r = subprocess.run([sys.executable, '-c', code], cwd=root, capture_output=True, text=True)
        if r.returncode:
            notes.append(f'FAILED icon: {r.stderr.strip()[-300:]}')
        run(root, 'tools/modtree/modtree.py', notes=notes)
    return notes


def checklist(mid, parent, loaders):
    return [
        f'python3.12 /root/sb-tools-b/precheck.py --fix   (wiki, handbooks, discover, modtree; gate check)',
        f'sbt: python3.12 tools/testrunner/run.py --targets module-{mid}-fabric-263,module-{mid}-standalone-fabric-263',
        f'modules/{mid}/wiki/manual.json: every new item/block needs a note EN+DE (the token has one); description in modules.json',
        f'modules/simplemoney/.../money/{mid}/prices.json: real prices or a reason per item (excluded) once items are real',
        f'Own config: ConfigCommands.server("{mid}")... (tools/check_config_commands.py); tooltips max 48 chars/line',
        'Upgrade data (in-world upgrades): com.simplelib.api.InWorldUpgrades.registerDefaults(MOD_ID) in the init',
        'Guide book: add a dict to tools/guides/module_guides.py MODULES, then run it (needs a guide registration in the init)',
        'Creative tab / SubMods.enabled(MOD_ID) in the entry class (see ArrowsFabric) when the module has items for players',
        'Textures: Vanilla-near, A8b; redraw the placeholder icon' + (' (Forge/NeoForge: embed on all loaders via multimod)' if len(loaders) > 1 else ''),
    ] + ([f'Open the sub-mod to the others: SB lang option texts reviewed; docs/MODSTRUKTUR via modtree (done)'] if parent else [])


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('modid')
    ap.add_argument('display_name', nargs='?', help='display name (alternative to --name)')
    ap.add_argument('--name')
    ap.add_argument('--parent', default='none', choices=['none', 'simplebuilding'])
    ap.add_argument('--bundle', default='optional', choices=['required', 'optional'])
    ap.add_argument('--config-switch', default=None, choices=['true', 'host', 'false'])
    ap.add_argument('--loaders', default='fabric')
    ap.add_argument('--enderite', action='store_true')
    ap.add_argument('--description')
    args = ap.parse_args(argv)
    name = args.name or args.display_name
    if not name:
        ap.error('display name missing: --name "Simple X"')
    parent = None if args.parent == 'none' else args.parent
    switch = {'true': True, 'false': False, 'host': 'host', None: True if parent else False}[args.config_switch]
    loaders = tuple(l for l in args.loaders.split(',') if l)
    try:
        notes = create(args.modid, name, ROOT, loaders, parent, args.bundle, switch, args.enderite, args.description, do_wire=True)
    except ValueError as error:
        ap.error(str(error))
    print(f'Created modules/{args.modid} ({"/".join(loaders)}, 26.3).')
    for n in notes:
        print('  ' + n)
    print('Next (checklist):')
    for line in checklist(args.modid, parent, loaders):
        print('  - ' + line)


if __name__ == '__main__':
    main()
