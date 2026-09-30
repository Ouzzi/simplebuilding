"""Validated registries and launch selections; no shell commands or owner paths."""
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ID = re.compile(r"^[a-z][a-z0-9_-]{1,63}$")

def local_roots(root=ROOT):
    """Read local jars in this checkout, then the main checkout, without owner paths."""
    result = subprocess.run(['git', '-C', str(root), 'rev-parse', '--path-format=absolute', '--git-common-dir'],
                            capture_output=True, text=True, timeout=10)
    roots = [root]
    if result.returncode == 0:
        main = Path(result.stdout.strip()).parent
        if main.resolve() != root.resolve(): roots.append(main)
    return roots

def read(path):
    return json.loads(path.read_text(encoding="utf-8"))

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix('.tmp')
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    temporary.replace(path)

def registries(root=ROOT):
    manifest = read(root / 'modules/modules.json')
    dev_registry = read(root / 'tools/devmods.json')
    if manifest.get('schemaVersion') != 1 or dev_registry.get('schemaVersion') != 1:
        raise ValueError('Unsupported registry schema')
    modules = manifest['modules']
    dev = dev_registry['mods']
    if not isinstance(modules, list) or not isinstance(dev, list) or not modules or modules[0]['id'] != 'simplebuilding':
        raise ValueError('SimpleBuilding must be the first module')
    seen = set()
    for entries in (modules, dev):
        for entry in entries:
            mid = entry['id']
            if not ID.fullmatch(mid) or mid in seen:
                raise ValueError(f'invalid or duplicate mod id: {mid}')
            seen.add(mid)
            if not isinstance(entry['name'], str) or not entry['name']:
                raise ValueError(f'missing name: {mid}')
            if type(entry['defaultEnabled']) is not bool:
                raise ValueError(f'invalid default: {mid}')
            if not entry['version'] or entry['minecraft'] != '26.3':
                raise ValueError(f'invalid compatibility: {mid}')
            for loader, value in entry.get('projects', {}).items():
                if loader not in ('fabric', 'neoforge') or not isinstance(value, str) or not re.fullmatch(r'(?::[a-z][a-z0-9_]*)+', value):
                    raise ValueError(f'invalid project: {mid}')
                path = root.joinpath(*value.strip(':').split(':')).resolve()
                if not path.is_relative_to(root.resolve()) or not (path / 'build.gradle').is_file():
                    raise ValueError(f'missing project: {mid}: {path}')
            if entry in modules and not entry.get('projects'):
                raise ValueError(f'missing projects: {mid}')
            if entry in dev:
                if not entry['purpose'] or not entry['sources']:
                    raise ValueError(f'missing source/purpose: {mid}')
                for loader, source in entry['sources'].items():
                    if loader not in ('fabric', 'neoforge') or set(source) not in ({'maven'}, {'local'}):
                        raise ValueError(f'invalid source: {mid}')
                    if 'maven' in source and (not isinstance(source['maven'], str) or len(source['maven'].split(':')) != 3):
                        raise ValueError(f'invalid Maven source: {mid}')
                    if 'local' in source:
                        value = source['local']
                        if not isinstance(value, str) or not value.endswith('.jar') or '\\' in value or ':' in value or value.startswith('/') or '..' in value.split('/'):
                            raise ValueError(f'local JAR path must remain inside the repository: {mid}')
    return modules, dev

def validate_selection(value, root=ROOT):
    modules, dev = registries(root)
    if not isinstance(value, dict) or set(value) != {'schemaVersion', 'modules', 'devMods'} or value['schemaVersion'] != 1:
        raise ValueError('invalid selection schema')
    for key, entries in (('modules', modules), ('devMods', dev)):
        ids = value[key]
        if not isinstance(ids, list) or any(not isinstance(i, str) for i in ids) or len(ids) != len(set(ids)) or not set(ids) <= {e['id'] for e in entries}:
            raise ValueError(f'unknown or duplicate {key}')
    return value

def selection(root=ROOT):
    path = root / 'integration/enabled-mods.json'
    if path.exists():
        return validate_selection(read(path), root)
    modules, dev = registries(root)
    return {'schemaVersion': 1, 'modules': [e['id'] for e in modules if e['defaultEnabled']],
            'devMods': [e['id'] for e in dev if e['defaultEnabled']]}

if __name__ == '__main__':
    registries()
    selection()
    print('Multimod registries valid')
