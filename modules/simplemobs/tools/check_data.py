"""Module-owned data hook: bilingual names, loot tables, entity registration wiring."""
import json
from pathlib import Path
MODULE = Path(__file__).resolve().parents[1]
ITEMS = ('deceiver_cloth', 'deceiver_spawn_egg')
for locale in ('en_us', 'de_de'):
    data = json.loads((MODULE / f'shared/resources/assets/simplemobs/lang/{locale}.json').read_text(encoding='utf-8'))
    assert 'entity.simplemobs.deceiver' in data
    for item in ITEMS:
        assert f'item.simplemobs.{item}' in data, item
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
for item in ITEMS:
    assert manual['notes'][f'simplemobs:{item}']['en']['summary'] and manual['notes'][f'simplemobs:{item}']['de']['summary']
    assert (MODULE / f'shared/resources/assets/simplemobs/items/{item}.json').is_file()
    assert (MODULE / f'shared/resources/assets/simplemobs/models/item/{item}.json').is_file()
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/item/{item}.png').is_file()
for tex in ('deceiver', 'deceiver_eyes'):
    assert (MODULE / f'shared/resources/assets/simplemobs/textures/entity/deceiver/{tex}.png').is_file()
loot = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/deceiver.json').read_text(encoding='utf-8'))
assert loot['pools'][0]['entries'][0]['name'] == 'simplemobs:deceiver_cloth'
summoned = json.loads((MODULE / 'shared/resources/data/simplemobs/loot_table/entities/summoned.json').read_text(encoding='utf-8'))
assert summoned['pools'] == [], 'summoned mobs must not drop anything'
print('simplemobs: bilingual names, loot and assets valid')
