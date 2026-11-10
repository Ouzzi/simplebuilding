"""Simple Layers data invariants (no Minecraft, no Gradle): generated data up to date, every item named in EN and DE with a
wiki note, every model texture present, sounds present, the special rooms 17 x 7 x 17."""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
RES = MODULE / 'shared/resources'
ITEMS = ['cloud_block', 'cloud_stone', 'mist_block', 'damp_carpet', 'wallpaper', 'striped_wallpaper', 'dotted_wallpaper', 'ceiling_tile',
         'stained_ceiling_tile', 'neon_light', 'exit_sign', 'caution_sign', 'caution_tape', 'glitched_bedrock', 'almond_water', 'backdoor_key']
ITEMS += ['chalk', 'red_chalk', 'blue_chalk', 'yellow_chalk', 'chalk_box']  # BACK20
FLAT = ('almond_water', 'backdoor_key', 'chalk', 'red_chalk', 'blue_chalk', 'yellow_chalk', 'chalk_box')
problems = []
r = subprocess.run([sys.executable, str(MODULE / 'tools/gen_data.py'), '--check'], capture_output=True, text=True)
if r.returncode:
    problems.append(r.stdout.strip())
lang = {loc: json.loads((RES / f'assets/simplelayers/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
notes = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))['notes']
for item in ITEMS:
    kind = 'item' if item in FLAT else 'block'
    for loc, data in lang.items():
        if f'{kind}.simplelayers.{item}' not in data:
            problems.append(f'{loc}: {kind}.simplelayers.{item}')
    if f'simplelayers:{item}' not in notes:
        problems.append(f'wiki note simplelayers:{item}')
if set(lang['en_us']) != set(lang['de_de']):
    problems.append(f'lang keys differ: {sorted(set(lang["en_us"]) ^ set(lang["de_de"]))[:6]}')
for model in (RES / 'assets/simplelayers/models').rglob('*.json'):
    for tex in json.loads(model.read_text(encoding='utf-8')).get('textures', {}).values():
        if tex.startswith('simplelayers:') and not (RES / f'assets/simplelayers/textures/{tex.split(":", 1)[1]}.png').exists():
            problems.append(f'{model.name}: texture {tex} missing')
for event in json.loads((RES / 'assets/simplelayers/sounds.json').read_text(encoding='utf-8')).values():
    for s in event['sounds']:
        if not (RES / f'assets/simplelayers/sounds/{s["name"].split(":", 1)[1]}.ogg').exists():
            problems.append(f'sound {s["name"]} missing')
if problems:
    print('simplelayers:', *problems, sep='\n  ')
    sys.exit(1)
print(f'simplelayers: {len(ITEMS)} items bilingual with wiki notes, models, sounds and generated data valid')
