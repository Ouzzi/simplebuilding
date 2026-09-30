"""Create a 26.3 module: python tools/newmod.py mymod "My Mod"."""
import argparse
import json
import re
from pathlib import Path
from multimod import ROOT, ID, registries, selection, write

def create(mid, name, root=ROOT):
    modules, dev = registries(root)
    if not re.fullmatch(r'[a-z][a-z0-9_]{1,63}', mid) or mid in {e['id'] for e in modules + dev}:
        raise ValueError('Use a unique lowercase mod id (letters, digits, underscores).')
    if not name.strip() or len(name) > 80 or any(ord(c) < 32 for c in name) or any(c in name for c in '\\"'):
        raise ValueError('Display name must be 1–80 printable characters without quotes or backslashes.')
    dest = root / 'modules' / mid
    if dest.exists():
        raise ValueError(f'Already exists: {dest}')
    enabled = selection(root)
    template = root / 'tools/templates/module'
    for template_file in template.rglob('*'):
        if template_file.is_file():
            path = dest / template_file.relative_to(template).as_posix().replace('wiringexample', mid)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(template_file.read_text(encoding='utf-8').replace('__MODID__', mid)
                            .replace('__DISPLAY_NAME__', name), encoding='utf-8')
    module_root = f'modules/{mid}'
    modules.append(dict(id=mid, name=name, displayName=name, description='', version='0.1.0', minecraft='26.3', defaultEnabled=True,
                        loaders=['fabric', 'neoforge'], requires=[], optional=[],
                        paths=dict(root=module_root, shared=module_root+'/shared/java',
                                   fabric=module_root+'/fabric', neoforge=module_root+'/neoforge', forge=module_root+'/forge',
                                   generated=module_root+'/generated', lang=module_root+f'/shared/resources/assets/{mid}/lang',
                                   wikiManual=module_root+'/wiki/manual.json', balanceDir='balance/'+mid),
                        projects={loader: f':modules:{mid}:{loader}' for loader in ('fabric','neoforge')}))
    write(root / 'modules/modules.json', {'schemaVersion':1, 'modules':modules})
    enabled['modules'].append(mid)
    write(root / 'integration/enabled-mods.json', enabled)
    print(f'Created modules/{mid}; Gradle discovers both loader projects automatically.')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('modid')
    parser.add_argument('display_name')
    args = parser.parse_args()
    try:
        create(args.modid, args.display_name)
    except ValueError as error:
        parser.error(str(error))
