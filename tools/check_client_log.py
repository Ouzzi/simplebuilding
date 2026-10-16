#!/usr/bin/env python3
"""Scans Minecraft client logs for the warnings that mean "missing texture / missing model" (Nachtrag 66).

The client logs a line for almost every fault that ends in the purple/black checkerboard, but only as WARN,
so a "green" run hides them. Patterns are the 26.3 client's own messages (ModelManager, ModelDiscovery,
ModelBakery, MaterialBaker, TextureSlots, atlas sources, SpriteResourceLoader).

Usage: python3 tools/check_client_log.py <log> [<log> ...]   (exit 1 if any fault)
Also used by tools/testrunner/run.py (every client target warns, target audit-models-263 fails).
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

FAULT_PATTERNS = [re.compile(p) for p in (
    r'Missing textures in model ',
    r'Missing texture references in model ',
    r'Missing item model for location ',
    r'Missing model for variant',
    r'Missing block model: ',
    r'Unable to bake (item )?model',
    r'Unable to load model',
    r'Failed to load blockstate definition',
    r'Failed to open item model',
    r'Failed to load (item|model|texture)',
    r'Failed to parse atlas definition',
    r'Missing sprite: ',
    r'Unable to find texture ',
    r'Using missing texture, unable to load ',
    r"Couldn't load texture ",
    r'Unable to parse metadata from ',
    r'Multiple atlases used in model',
    r'Rejecting block model ',
    r'Unresolved texture references in ',
    r'Requested a model that was not discovered',
    r'Model .* ignored due to cyclic dependency',
    r'[Mm]issing particle (texture|sprite)',
    r'Unknown particle',
    r'\[model-audit\] FAULT',
)]
LINE = re.compile(r'^\[\d\d:\d\d:\d\d\] \[[^\]]*/(WARN|ERROR|INFO|DEBUG)\]')


def faults(text: str) -> list[str]:
    """Fault blocks (the first line plus its indented/stack continuation lines, trimmed), de-duplicated."""
    lines = text.splitlines()
    out: list[str] = []
    seen: set[str] = set()
    for i, line in enumerate(lines):
        if not LINE.match(line) or not any(p.search(line) for p in FAULT_PATTERNS):
            continue
        block = [re.sub(r'^\[[^\]]*\] \[[^\]]*\] ', '', line, count=1)]
        for follow in lines[i + 1:i + 12]:
            if LINE.match(follow):
                break
            if follow.strip() and not follow.startswith('[') and not follow.lstrip().startswith('at '):
                block.append(follow.rstrip())
        key = '\n'.join(block)
        if key not in seen:
            seen.add(key)
            out.append(key)
    return out


def main() -> int:
    bad = 0
    for name in sys.argv[1:]:
        found = faults(Path(name).read_text('utf-8', 'replace'))
        for f in found:
            print(f'FAULT {name}: {f}')
        bad += len(found)
    print(f'check_client_log: {bad} fault block(s)')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
