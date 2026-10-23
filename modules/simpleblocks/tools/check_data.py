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
assert len(g.BLOCKS) == 38, len(g.BLOCKS)

assert subprocess.run([sys.executable, str(MODULE / 'tools/generate_data.py'), '--check']).returncode == 0

# Moved from SimpleBuilding (b-blocksmove): NatureBlocks lists exactly the generator's MOVED blocks; every one has its
# blockstate, item definition, loot table and recipe (the nature variants' files came from SimpleBuilding's datagen); none
# of the module's data names a simplebuilding: id outside a simplebuilding load condition.
nature_java = (MODULE / 'shared/java/com/simpleblocks/nature/NatureBlocks.java').read_text(encoding='utf-8')
assert re.findall(r'(?:add|froglight)\("([a-z_]+)"', nature_java) == g.MOVED, (re.findall(r'(?:add|froglight)\("([a-z_]+)"', nature_java), g.MOVED)
assert len(g.MOVED) == 15
for name in g.MOVED:
    for rel in (f'assets/{NS}/blockstates/{name}.json', f'assets/{NS}/items/{name}.json', f'data/{NS}/loot_table/blocks/{name}.json'):
        assert (RES / rel).exists(), rel
recipe_dir = RES / f'data/{NS}/recipe'
for name in g.MOVED:
    assert any(p.stem == name or p.stem.startswith(name + '_from_') for p in recipe_dir.glob('*.json')), f'no recipe makes {name}'
for path in list((RES / f'data/{NS}').rglob('*.json')) + list((RES / f'assets/{NS}').rglob('*.json')):
    text = path.read_text(encoding='utf-8')
    if 'simplebuilding:' in text:
        assert '"simplebuilding"' in text, f'{path.relative_to(MODULE)} names a SimpleBuilding id without a simplebuilding condition'
for name in ('aqua_froglight_side', 'aqua_froglight_top', 'azure_froglight_side', 'azure_froglight_top', 'scarlet_froglight_side',
             'scarlet_froglight_top', 'nautilus_shell_block_side', 'nautilus_shell_block_top'):
    assert (RES / f'assets/{NS}/textures/block/{name}.png').exists(), name

langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for name in list(g.BLOCKS) + g.MOVED:
        assert data.get(f'block.{NS}.{name}') and data.get(f'item.{NS}.{name}'), (locale, name)
    for k in (f'itemgroup.{NS}.blocks', f'{NS}.testcentre.station', f'item.{NS}.guide_book', f'block.{NS}.{g.SAW}', f'container.{NS}.{g.SAW}'):
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
related = {r for f in manual['features'] for r in f.get('related', [])}
assert related >= {f'{NS}:{n}' for n in list(g.BLOCKS) + g.MOVED} | {f'{NS}:{g.SAW}'}, 'wiki: every block is documented'
for feature in manual['features']:
    for src in feature['sources']:
        assert (MODULE.parents[1] / src).exists(), src

# Textures: the own ones are committed, the cracked/mossy bricks are Vanilla-derived (built, never committed); since W23-3
# also sculk deepslate, hushed tiles and the chiseled ice (Vanilla base + own overlay). Sculk tiles: own, animated.
tex = RES / f'assets/{NS}/textures/block'
for name in ('blue_nether_bricks', 'sculk_tiles'):
    assert (tex / f'{name}.png').exists(), name
assert json.loads((tex / 'sculk_tiles.png.mcmeta').read_text(encoding='utf-8'))['animation'], 'sculk tiles are animated'
sys.path.insert(0, str(MODULE.parents[1] / 'tools/textures'))
import vanilla_derived  # noqa: E402
for name in ('sculk_deepslate', 'hushed_deepslate_tiles', 'chiseled_packed_ice', 'chiseled_blue_ice'):
    assert f'block/{name}.png' in vanilla_derived.MODULE_OUTPUTS[NS] and not (tex / f'{name}.layers.json').exists(), f'{name} is Vanilla-derived'
assert {f'block/shapecutter_{n}.png' for n in ('top', 'side', 'bottom', 'saw')} <= set(vanilla_derived.MODULE_OUTPUTS[NS]), 'shapecutter textures are Vanilla-derived'
# The blade is Vanilla's strip regreyed at build time (variant S1/S2); its animation stays Vanilla's (own .mcmeta, same values).
assert json.loads((tex / 'shapecutter_saw.png.mcmeta').read_text(encoding='utf-8')) == {'animation': {'interpolate': False, 'frametime': 1}}, 'blade animation'

# Shapecutter (A4b): Java ids match the generator; every wood family and every wool colour has its cuts; optional mods conditioned.
saw_java = (MODULE / 'shared/java/com/simpleblocks/saw/Sawing.java').read_text(encoding='utf-8')
assert f'NAME = "{g.SAW}"' in saw_java and 'id("sawing")' in saw_java, 'Sawing.java: saw id or recipe type id changed'
cuts = g.sawing_list()
assert len({c[0] for c in cuts}) == len(cuts), 'duplicate sawing recipe ids'
results = {c[2] for c in cuts}
for ns, w, *_ in g.WOODS:
    assert {f'{ns}:{w}_stairs', f'{ns}:{w}_slab', f'{ns}:{w}_fence', f'{ns}:{w}_planks'} <= results, w
for color in g.WOOLS:
    assert {f'minecraft:{color}_wool_stairs', f'minecraft:{color}_wool_slab'} <= results, color
for path, ingredient, out, count, mod in cuts:
    foreign = {ingredient.lstrip('#').split(':')[0], out.split(':')[0]} - {'minecraft', NS}
    assert foreign <= ({mod} if mod else set()), (path, 'needs a mod-loaded condition for', foreign)
print(f'simpleblocks: 38 blocks + 15 moved from SimpleBuilding + Shapecutter ({len(cuts)} sawing recipes), generated data, bilingual lang and wiki valid')
