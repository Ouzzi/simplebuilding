"""Simple Slabs data invariants (N86): the generated half slab files are up to date, 170 materials, both lang files name
every half slab and its side form, the loot tables use the 26.x format, the wiki manual has EN/DE features."""
import json
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
REPO = MODULE.parents[1]
gen = subprocess.run([sys.executable, str(REPO / 'tools/textures/half_slabs_2026_11_17.py'), '--check'], capture_output=True, text=True)
assert gen.returncode == 0, 'half slab generator: ' + (gen.stdout + gen.stderr).strip()
mats = json.loads((REPO / 'tools/textures/half_slab_materials.json').read_text(encoding='utf-8'))
assert len(mats) == 170, f'{len(mats)} half slab materials, owner N86-5: 170'
assert len({m['id'] for m in mats}) == 170, 'duplicate half slab ids'
for locale in ('en_us', 'de_de'):
    lang = json.loads((MODULE / f'shared/resources/assets/simpleslabs/lang/{locale}.json').read_text(encoding='utf-8'))
    for m in mats:
        side = m['id'][:-len('half_slab')] + 'side_half_slab'
        for key in (f'block.simpleslabs.{m["id"]}', f'item.simpleslabs.{m["id"]}', f'block.simpleslabs.{side}'):
            assert key in lang, f'{locale}: {key} missing'
    assert 'itemgroup.simpleslabs.half_slabs' in lang
loot = MODULE / 'shared/resources/data/simpleslabs/loot_table/blocks'
for f in loot.glob('*.json'):
    text = f.read_text(encoding='utf-8')
    assert '"conditions"' not in text and '"functions"' not in text, f'{f.name}: pre-26 loot format'
assert len(list(loot.glob('*.json'))) == 340, 'one loot table per half slab and side form'
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
assert manual['features'] and all(x['en'].get('summary') and x['de'].get('summary') for x in manual['features'])
print(f'simpleslabs: {len(mats)} half slabs (+ side forms), generated files, lang EN/DE, loot and wiki valid')
