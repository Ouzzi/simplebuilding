"""Manifest paths and isolated module services. Legacy SimpleBuilding storage stays in place."""
import json
import re
import threading
from pathlib import Path


def resources(module):
    paths = module['paths']
    return [paths['root'] + '/shared/resources'] + [paths[k] + '/src/main/resources'
            for k in ('fabric', 'neoforge', 'forge') if paths.get(k)]


def load(repo):
    path = Path(repo) / 'modules/modules.json'
    if not path.exists():
        return []  # Older test fixtures and standalone checkouts.
    entries = json.loads(path.read_text(encoding='utf-8'))['modules']
    seen = set()
    for entry in entries:
        mid = entry['id']
        if not re.fullmatch(r'[a-z][a-z0-9_-]*', mid) or mid in seen:
            raise ValueError('Invalid or duplicate module id')
        seen.add(mid)
        for rel in entry['paths'].values():
            if rel and not (Path(repo) / rel).resolve().is_relative_to(Path(repo).resolve()):
                raise ValueError(f'Module path outside repository: {mid}')
    return entries


def storage(repo, module, override=None):
    # No copy/migration: all existing versions, applied logs and rollback literals remain readable.
    if override is not None:
        return Path(override) if module['id'] == 'simplebuilding' else Path(override) / module['id']
    return Path(repo) / module['paths']['balanceDir']


class Registry:
    def __init__(self, repo, store=None, **options):
        self.repo = Path(repo)
        self.modules = load(repo)
        self.store = store
        self.options = options
        self.services = {}
        self.lock = threading.RLock()

    def select(self, mid='simplebuilding'):
        from .service import Service
        with self.lock:
            module = next((m for m in self.modules if m['id'] == mid), None)
            if module is None:
                raise ValueError(f'Unknown module: {mid}')
            if mid not in self.services:
                self.services[mid] = Service(self.repo, storage(self.repo, module, self.store), module=module, **self.options)
            return self.services[mid]

    def overview(self):
        rows = []
        for module in self.modules:
            service = self.select(module['id'])
            state = service.store.state()
            statuses = service.statuses(state['entries'])
            rows.append(dict(module, balanceVersion=state['version'], changes=len(state['entries']),
                             pending=sum(s['status'] != 'applied' for s in statuses.values()),
                             values=len(service.snapshot['values'])))
        return {'modules': rows, 'totalChanges': sum(r['changes'] for r in rows),
                'totalPending': sum(r['pending'] for r in rows)}
