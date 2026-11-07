"""Simple Chisels data invariants (b-chisels07): bilingual lang, seven chisels with model, item definition, texture, layers and
recipe, Fast Chiseling, the tag, the default tables only with Vanilla blocks, and a wiki entry per item."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
RES = MODULE / 'shared/resources'
TIERS = ['stone', 'copper', 'iron', 'gold', 'diamond', 'netherite', 'enderite']
def read(p): return json.loads(p.read_text(encoding='utf-8'))
lang = {l: read(RES / f'assets/simplechisels/lang/{l}.json') for l in ('en_us', 'de_de')}
assert lang['en_us'].keys() == lang['de_de'].keys(), 'EN/DE keys differ'
for t in TIERS:
    n = f'{t}_chisel'
    assert f'item.simplechisels.{n}' in lang['en_us'], n
    for rel in (f'assets/simplechisels/models/item/{n}.json', f'assets/simplechisels/items/{n}.json',
                f'assets/simplechisels/textures/item/{n}.png', f'assets/simplechisels/textures/item/{n}.layers.json'):
        assert (RES / rel).is_file(), rel
assert read(RES / 'data/simplechisels/tags/item/chisels.json')['values'] == [f'simplechisels:{t}_chisel' for t in TIERS]
ench = read(RES / 'data/simplechisels/enchantment/fast_chiseling.json')
assert ench['supported_items'] == '#simplechisels:chisels' and 'enchantment.simplechisels.fast_chiseling' in lang['en_us']
recipes = {p.stem for p in (RES / 'data/simplechisels/recipe').glob('*.json')}
assert len(recipes) == 10, recipes
for p in (RES / 'data/simplechisels/recipe').glob('*.json'):
    adv = list((RES / 'data/simplechisels/advancement/recipes').glob(f'*/{p.name}'))
    assert len(adv) == 1, f'recipe unlock missing for {p.stem}'
for p in (RES / 'data/simplechisels/chisel_transformations').glob('*.json'):
    data = read(p)
    assert data['tier'] in ('stone', 'iron', 'diamond', 'netherite', 'enderite') and data['table'] in ('chisel', 'touch')
    for chain in data['chains']:
        assert all(b.startswith('minecraft:') for b in chain['blocks']), f'{p.name}: only Vanilla chains here'
manual = read(MODULE / 'wiki/manual.json')
assert manual['notes']['simplechisels:*_chisel'] and manual['notes']['simplechisels:fast_chiseling']
for f in manual['features']:
    for loc in ('en', 'de'):
        assert f[loc]['title'] and f[loc]['summary']
    for s in f['sources']:
        assert (MODULE.parents[1] / s).is_file(), s
print('simplechisels: 7 chisels, 10 recipes, Fast Chiseling, Vanilla tables, bilingual lang and wiki valid')
