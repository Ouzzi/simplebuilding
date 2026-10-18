#!/usr/bin/env python3
"""Item-model definitions that name a custom property must have it registered on every loader (MC 26.3).

An item definition (assets/<ns>/items/*.json) with {"property": "simplebuilding:foo"} (select / range_dispatch /
condition) is read by the client through the vanilla *ItemModelProperties.ID_MAPPER. If the property is not put there
on a loader the whole item model fails to load and the item shows the missing-model cube. This check finds, for every
non-minecraft property id used by a shipped item definition, the Java class or registration that declares it and
requires it on Fabric (src/main/java), NeoForge (neoforge/src) and Forge (forge/src).

Usage: python3 tools/check_item_properties.py   (exit 1 on any ERROR; no Gradle, no Minecraft)
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_missing_textures import ROOT, layer_roots  # noqa: E402

LOADERS = {'Fabric': 'src/main/java', 'NeoForge': 'neoforge/src', 'Forge': 'forge/src'}
# Item-model properties of the loader itself, registered by Forge/NeoForge/Fabric API under their own namespace.
FOREIGN_NS = {'minecraft', 'c', 'neoforge', 'forge', 'fabric'}


def used_properties() -> dict[str, set[str]]:
    """property id -> item definition files that use it."""
    out: dict[str, set[str]] = {}

    def walk(node, file):
        if isinstance(node, dict):
            for key in ('property', 'type'):
                v = node.get(key)
                if isinstance(v, str) and ':' in v and v.split(':')[0] not in FOREIGN_NS and (key == 'property' or 'model' in v):
                    out.setdefault(v, set()).add(file)
            for v in node.values():
                walk(v, file)
        elif isinstance(node, list):
            for v in node:
                walk(v, file)

    for _, base in layer_roots():
        for f in (base / 'assets').glob('*/items/*.json'):
            try:
                walk(json.loads(f.read_text(encoding='utf-8')), str(f.relative_to(ROOT)))
            except ValueError:
                pass
    return out


def java_files(sub: str) -> dict[Path, str]:
    return {p: p.read_text(encoding='utf-8', errors='replace') for p in (ROOT / sub).rglob('*.java')
            if '/build/' not in p.as_posix()}


def main() -> int:
    props = used_properties()
    common = java_files('common/src/shared/java')
    loaders = {name: java_files(sub) for name, sub in LOADERS.items()}
    errors = 0
    for pid in sorted(props):
        ns, path = pid.split(':', 1)
        if '/' in path:
            continue
        # The declaring class: names the id literal next to a Identifier/ResourceLocation factory, or is the property record.
        literal = re.compile(rf'"{re.escape(path)}"')
        decl = [p for p, t in common.items() if '/client/property/' in p.as_posix() and literal.search(t)]
        names = [p.stem for p in decl]
        for loader, files in loaders.items():
            ok = any(literal.search(t) or any(n in t for n in names) for t in files.values()
                     if 'ID_MAPPER' in t or 'register' in t.lower())
            if not ok:
                errors += 1
                print(f'ERROR {pid} (used by {sorted(props[pid])[0]}) is not registered on {loader}'
                      f' (declared by {names or "no class in client/property"})')
    print(f'check_item_properties: {len(props)} custom item-model ids, {errors} errors')
    print('alles gruen' if not errors else 'FEHLER')
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(main())
