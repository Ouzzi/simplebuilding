"""B1 (b-lib01): SimpleBuilding's sub-mod switches from modules/modules.json, the single source.

    python3 tools/submods/sync.py          write the generated blocks (also over merge conflict markers inside them)
    python3 tools/submods/sync.py --check  only compare, exit 1 when anything is stale (gradle check: checkSubMods)

modules.json: a module with "parent": "simplebuilding" names its switch in "configSwitch": true (field enableSimple<X>,
generated here and handed to framework SubMods under its mod id), "host" (field kept by hand, the sub-mod reads its key
itself: Maps, Questionary) or false (no switch). Generated blocks sit between `// <submods:begin NAME>` and
`// <submods:end NAME>`, in modules.json order. Also checked: the shared fabric.mod.json suggests every optional
sub-mod (written when missing) and both lang files name and explain every switch (texts stay hand-written).
The 26.3 Fabric build (depends, include, implementation) reads modules.json itself (mc26_3/fabric/build.gradle).
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONFIG = 'common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java'
OPTIONS = 'common/src/shared/java/com/simplebuilding/config/ConfigOptions.java'
TESTS = 'common/src/shared/java/com/simplebuilding/gametest/ConfigOptionTests.java'
FABRIC_MOD = 'src/main/resources/fabric.mod.json'
LANGS = ('mc26_3/overlay/resources/assets/simplebuilding/lang/en_us.json',
         'mc26_3/overlay/resources/assets/simplebuilding/lang/de_de.json')
BLOCK = re.compile(r'(?P<head>^[ \t]*// <submods:begin (?P<name>\w+)>\n)(?P<body>.*?)(?P<tail>^[ \t]*// <submods:end (?P=name)>$)',
                   re.M | re.S)


def registry(root=ROOT):
    modules = json.loads((root / 'modules/modules.json').read_text(encoding='utf-8'))['modules']
    return {m['id']: m for m in modules}, modules


def switches(modules):
    """(field, mod id) of every generated switch, modules.json order; errors for SB sub-mods without a decision."""
    result, errors = [], []
    for m in modules:
        if m.get('parent') != 'simplebuilding':
            continue
        value = m.get('configSwitch')
        if value not in (True, False, 'host'):
            errors.append(f'modules.json {m["id"]}: "configSwitch" must be true, false or "host"')
        elif value is True:
            if not m['id'].startswith('simple') or not re.fullmatch(r'[a-z]+', m['id'][6:]):
                errors.append(f'modules.json {m["id"]}: no switch name derivable (simple<word>)')
                continue
            result.append(('enableSimple' + m['id'][6:].capitalize(), m.get('modId', m['id'])))
    return result, errors


def blocks(sw):
    return {
        CONFIG: {
            'fields': ''.join(f'    @ConfigEntry.Category("modules")\n    @ConfigEntry.Gui.Tooltip\n    public boolean {f} = true;\n\n'
                              for f, _ in sw).rstrip('\n') + '\n',
            'apply': ''.join(f'        setSubMod("{mid}", c.{f});\n' for f, mid in sw),
        },
        OPTIONS: {'restart': ''.join(f'            "{f}",\n' for f, _ in sw)},
        TESTS: {
            'expected': ''.join(f'            "root.{f} boolean=true",\n' for f, _ in sw),
            'switches': ''.join(f'                {{"{f}", "{mid}"}},\n' for f, mid in sw),
        },
    }


def render(text, wanted, path):
    seen, errors = set(), []

    def repl(match):
        name = match['name']
        seen.add(name)
        if name not in wanted:
            errors.append(f'{path}: unknown block {name}')
            return match.group(0)
        return match['head'] + wanted[name] + match['tail']

    out = BLOCK.sub(repl, text)
    errors += [f'{path}: marker block {name} missing' for name in wanted if name not in seen]
    return out, errors


def suggests(text, by_id):
    """Shared fabric.mod.json: every optional sub-mod of SB suggested; missing ones appended to "suggests"."""
    sb = by_id['simplebuilding']
    want = [by_id[i].get('modId', i) for i in sb['optional'] if i in by_id]
    have = json.loads(text).get('suggests', {})
    missing = [i for i in want if i not in have]
    if not missing:
        return text, []
    match = re.search(r'("suggests": \{\n[^}]*?)\n([ \t]*)\}', text)
    body = match.group(1)
    indent = re.search(r'\n([ \t]+)"', body).group(1)
    body += ',\n' + ',\n'.join(f'{indent}"{i}": "*"' for i in missing) + '\n'
    return text[:match.start()] + body + match.group(2) + '}' + text[match.end():], [f'{FABRIC_MOD}: suggests misses {missing}']


def run(check, root=ROOT):
    by_id, modules = registry(root)
    sw, errors = switches(modules)
    stale = []
    for path, wanted in blocks(sw).items():
        file = root / path
        text = file.read_text(encoding='utf-8')
        out, errs = render(text, wanted, path)
        errors += errs
        if out != text:
            stale.append(path)
            if not check:
                file.write_text(out, encoding='utf-8')
    file = root / FABRIC_MOD
    text = file.read_text(encoding='utf-8')
    out, errs = suggests(text, by_id)
    if errs:
        stale.append(FABRIC_MOD)
        if not check:
            file.write_text(out, encoding='utf-8')
    for path in LANGS:
        lang = json.loads((root / path).read_text(encoding='utf-8'))
        for f, _ in sw:
            for key in (f'text.autoconfig.simplebuilding.option.{f}', f'text.autoconfig.simplebuilding.option.{f}.@Tooltip'):
                if not str(lang.get(key, '')).strip():
                    errors.append(f'{path}: missing {key} (name and tooltip of the switch, by hand)')
    return stale, errors


def main(argv):
    check = '--check' in argv
    stale, errors = run(check)
    for e in errors:
        print('ERROR ' + e)
    if check and stale:
        for path in stale:
            print(f'STALE {path} - fix: python3 tools/submods/sync.py')
    elif stale:
        for path in stale:
            print(f'wrote {path}')
    if errors or (check and stale):
        return 1
    print(f'Sub-mod switches in sync ({len(switches(registry()[1])[0])} switches)')
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
