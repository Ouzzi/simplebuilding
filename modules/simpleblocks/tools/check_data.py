"""Module-owned data hook: block list matches BlocksRegistry, generated files are current, bilingual lang, wiki and textures."""
import json
import re
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simpleblocks'
RES = MODULE / 'shared/resources'
sys.path.insert(0, str(MODULE / 'tools'))
import generate_data as g  # noqa: E402

java = (MODULE / 'shared/java/com/simpleblocks/BlocksRegistry.java').read_text(encoding='utf-8')
# Names in BlocksRegistry: the explicit adds, the SHAPES list and the AGED pairs must equal the generator's block list.
registry = set(re.findall(r'add\("([a-z_]+)"', java))
registry |= {m for m in re.findall(r'"([a-z_]+_(?:stairs|slab|wall))"', java.split('SHAPES = List.of(')[1].split(');')[0])}
registry |= {m for m in re.findall(r'new String\[\]\{"([a-z_]+)", "[a-z_]+"\}', java)}
assert registry == set(g.BLOCKS), (sorted(registry ^ set(g.BLOCKS)))
assert len(g.BLOCKS) == 33, len(g.BLOCKS)

assert subprocess.run([sys.executable, str(MODULE / 'tools/generate_data.py'), '--check']).returncode == 0

langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for name in g.BLOCKS:
        assert data.get(f'block.{NS}.{name}') and data.get(f'item.{NS}.{name}'), (locale, name)
    for k in (f'itemGroup.{NS}', f'{NS}.testcentre.station', f'item.{NS}.guide_book'):
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
related = {r for f in manual['features'] for r in f.get('related', [])}
assert related >= {f'{NS}:{n}' for n in g.BLOCKS}, 'wiki: every block is documented'
for feature in manual['features']:
    for src in feature['sources']:
        assert (MODULE.parents[1] / src).exists(), src

# Textures: the own ones are committed, the cracked/mossy bricks are Vanilla-derived (built, never committed).
tex = RES / f'assets/{NS}/textures/block'
for name in ('hushed_deepslate_tiles', 'sculk_deepslate'):
    assert (tex / f'{name}.png').exists(), name
print('simpleblocks: 33 blocks, generated data, bilingual lang and wiki valid')
