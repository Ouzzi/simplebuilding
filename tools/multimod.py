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

def validate_relative_path(value, root, label):
    if (not isinstance(value, str) or not value or "\\" in value or ":" in value
            or value.startswith('/') or '..' in value.split('/')
            or not (root / value).resolve().is_relative_to(root.resolve())):
        raise ValueError(f'Invalid {label}: {value}')


def validate_tests(entry, tests, root):
    mid = entry['id']
    if not isinstance(tests, dict) or not ID.fullmatch(tests.get('namespace', mid)):
        raise ValueError(f'Invalid test namespace: {mid}')
    catalogues = tests.get('catalogues')
    if not isinstance(catalogues, list) or not catalogues or len(set(catalogues)) != len(catalogues):
        raise ValueError(f'Invalid test catalogues: {mid}')
    for path in catalogues:
        validate_relative_path(path, root, f'{mid}: catalogue')
    suites = tests.get('loaders', {})
    if not isinstance(suites, dict) or not suites or not set(suites) <= set(entry['loaders']):
        raise ValueError(f'Invalid test loaders: {mid}')
    for loader, spec in suites.items():
        if not isinstance(spec, dict) or not re.fullmatch(r'(?::[A-Za-z][A-Za-z0-9_]*)+', spec.get('task', '')):
            raise ValueError(f'Invalid test task: {mid}/{loader}')
        expected_prefix = ':integration:run' if loader == 'fabric' else entry['projects'][loader] + ':run'
        if not spec['task'].startswith(expected_prefix) or len(spec['task']) <= len(expected_prefix):
            raise ValueError(f'Invalid test task owner: {mid}/{loader}')
        validate_relative_path(spec.get('report'), root, f'{mid}: report')
        if not isinstance(spec.get('gradleArgs', []), list) or any(not isinstance(arg, str) for arg in spec.get('gradleArgs', [])):
            raise ValueError(f'Invalid test Gradle arguments: {mid}')
    for field in ('requires', 'devMods'):
        values = tests.get(field, [])
        if not isinstance(values, list) or any(not isinstance(value, str) or not ID.fullmatch(value) for value in values):
            raise ValueError(f'Invalid test {field}: {mid}')
    standalone = tests.get('standalone')
    if standalone is not None:
        validate_standalone(entry, standalone, root)
    client = tests.get('client')
    if client is not None:
        if not isinstance(client, dict) or not client.get('entrypoints') or any(not re.fullmatch(r'[A-Za-z_][\w]*(?:\.[A-Za-z_][\w]*)+', value) for value in client['entrypoints']):
            raise ValueError(f'Invalid client entrypoints: {mid}')
        for field in ('sources', 'screenshots'):
            validate_relative_path(client.get(field), root, f'{mid}: client {field}')
        if client.get('task') != ':integration:runClientGameTest':
            raise ValueError(f'Invalid client smoke task: {mid}')


def validate_standalone(entry, standalone, root):
    """Principle 8: a standalone suite loads only the module and its hard requirements."""
    mid = entry['id']
    if not isinstance(standalone, dict):
        raise ValueError(f'Invalid standalone tests: {mid}')
    if 'exempt' in standalone:
        if set(standalone) != {'exempt'} or not isinstance(standalone['exempt'], str) or not standalone['exempt'].strip():
            raise ValueError(f'Standalone exemption needs only a reason: {mid}')
        return
    for field in ('requires', 'devMods'):
        values = standalone.get(field, [])
        if not isinstance(values, list) or any(not isinstance(v, str) or not ID.fullmatch(v) for v in values) or mid in values:
            raise ValueError(f'Invalid standalone {field}: {mid}')
    extra = set(standalone.get('requires', [])) - set(entry.get('requires', []))
    if extra:
        raise ValueError(f'Standalone may only add hard requirements: {mid}: {sorted(extra)}')
    if 'reason' in standalone and (not isinstance(standalone['reason'], str) or not standalone['reason'].strip()):
        raise ValueError(f'Invalid standalone reason: {mid}')
    suites = standalone.get('loaders')
    if not isinstance(suites, dict) or not suites or not set(suites) <= set(entry['loaders']) - {'forge'}:
        raise ValueError(f'Invalid standalone loaders: {mid}')
    for loader, spec in suites.items():
        expected = (f':integration:run{mid.capitalize()}StandaloneGameTest' if loader == 'fabric'
                    else entry['projects'][loader] + ':runModuleStandaloneGameTest')
        if not isinstance(spec, dict) or spec.get('task') != expected:
            raise ValueError(f'Invalid standalone task: {mid}/{loader} (expected {expected})')
        validate_relative_path(spec.get('report'), root, f'{mid}: standalone report')


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
                if loader not in ('fabric', 'neoforge', 'forge') or not isinstance(value, str) or not re.fullmatch(r'(?::[a-z][a-z0-9_]*)+', value):
                    raise ValueError(f'invalid project: {mid}')
                path = root.joinpath(*value.strip(':').split(':')).resolve()
                if not path.is_relative_to(root.resolve()) or not (path / 'build.gradle').is_file():
                    raise ValueError(f'missing project: {mid}: {path}')
            if entry in modules and not entry.get('projects'):
                raise ValueError(f'missing projects: {mid}')
            if entry in modules:
                for field in ('displayName', 'description'):
                    if not isinstance(entry.get(field), str) or not entry[field].strip():
                        raise ValueError(f'missing {field}: {mid}')
                loaders = entry.get('loaders')
                if (not isinstance(loaders, list) or not loaders or len(set(loaders)) != len(loaders)
                        or not set(loaders) <= {'fabric', 'neoforge', 'forge'}
                        or set(loaders) != set(entry['projects'])):
                    raise ValueError(f'invalid loaders: {mid}')
                paths = entry.get('paths', {})
                for field in ('root', 'shared', 'fabric', 'neoforge', 'forge', 'generated', 'lang', 'wikiManual', 'balanceDir'):
                    value = paths.get(field)
                    if field in ('fabric', 'neoforge', 'forge') and field not in loaders and value is None:
                        continue
                    if (not isinstance(value, str) or not value or '\\' in value or ':' in value
                            or value.startswith('/') or '..' in value.split('/')
                            or not (root / value).resolve().is_relative_to(root.resolve())):
                        raise ValueError(f'invalid {field} path: {mid}')
                for field in ('requires', 'optional'):
                    value = entry.get(field)
                    if (not isinstance(value, list) or any(not isinstance(i, str) or not ID.fullmatch(i) for i in value)
                            or len(value) != len(set(value)) or mid in value):
                        raise ValueError(f'invalid {field}: {mid}')

                if not isinstance(entry.get('checks', []), list):
                    raise ValueError(f'Invalid check scripts: {mid}')
                for script in entry.get('checks', []):
                    validate_relative_path(script, root, f'{mid}: check script')
                tests = entry.get('tests')
                if tests is not None:
                    validate_tests(entry, tests, root)

            if entry in dev:
                if not entry['purpose'] or not entry['sources']:
                    raise ValueError(f'missing source/purpose: {mid}')
                for loader, source in entry['sources'].items():
                    if loader not in ('fabric', 'neoforge', 'forge') or set(source) not in ({'maven'}, {'local'}):
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
