"""Simple Vault data invariants (UNDER21; no Minecraft, no Gradle): generated data up to date, every vault block and item named
in EN and DE with a wiki note, config keys with tooltips that name the default, the dimension/biome pair present."""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
RES = MODULE / 'shared/resources'
BLOCKS = ['vault_gate', 'vault_lamp', 'vault_lever', 'chime_crystal', 'path_tile', 'seal_pedestal', 'seal_socket', 'ascent_altar', 'ascent_seal', 'ascent_stream']
ITEMS = ['seal_fragment_light', 'seal_fragment_sound', 'seal_fragment_path']
CONFIG = ['enabled', 'gapRarity', 'closeSeconds', 'ambientVolume']
problems = []
r = subprocess.run([sys.executable, str(MODULE / 'tools/gen_data.py'), '--check'], capture_output=True, text=True)
if r.returncode:
    problems.append(r.stdout.strip())
lang = {loc: json.loads((RES / f'assets/simplevault/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
notes = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))['notes']
for kind, names in (('block', BLOCKS), ('item', ITEMS)):
    for n in names:
        for loc, data in lang.items():
            if f'{kind}.simplevault.{n}' not in data:
                problems.append(f'{loc}: {kind}.simplevault.{n}')
        if f'simplevault:{n}' not in notes:
            problems.append(f'wiki note simplevault:{n}')
for key in CONFIG:
    for loc, data in lang.items():
        tip = data.get(f'text.autoconfig.simplevault.option.vault.{key}.@Tooltip', '')
        if ('Default:' if loc == 'en_us' else 'Standard:') not in tip:
            problems.append(f'{loc}: tooltip vault.{key} without default')
for loc, data in lang.items():
    for k in ('effect.simplevault.vault_bound', 'biome.simplevault.bedrock_vault', 'text.autoconfig.simplevault.category.vault', 'itemgroup.simplevault.vault'):
        if k not in data:
            problems.append(f'{loc}: {k}')
for rel in ('data/simplevault/dimension/bedrock_vault.json', 'data/simplevault/dimension_type/bedrock_vault.json',
            'data/simplevault/worldgen/biome/bedrock_vault.json', 'assets/simplevault/textures/block/vault_gate.png',
            'assets/simplevault/textures/mob_effect/vault_bound.png'):
    if not (RES / rel).exists():
        problems.append(f'missing {rel}')
if problems:
    print('simplevault:', *problems, sep='\n  ')
    sys.exit(1)
print(f'simplevault: {len(BLOCKS)} blocks and {len(ITEMS)} items bilingual with wiki notes, config tooltips, generated data valid')
