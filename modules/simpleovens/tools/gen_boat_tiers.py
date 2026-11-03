"""Simple Ovens (b-small04, owner 03.11.): the furnace boats of the upper furnace tiers. Writes the plain data files (item definition,
item model, recipe, recipe advancement) and the EN/DE lang keys for {reinforced, netherite, enderite} x every wood. Idempotent.
Recipe: shapeless, the Vanilla boat (raft) of the wood + the furnace of the tier; the Enderite one only with Simple Ores.
    python3 modules/simpleovens/tools/gen_boat_tiers.py
"""
import json
from pathlib import Path

RES = Path(__file__).resolve().parents[1] / 'shared/resources'
WOODS = {'oak': ('Oak', 'Eichen'), 'spruce': ('Spruce', 'Fichten'), 'birch': ('Birch', 'Birken'), 'jungle': ('Jungle', 'Jungle'), 'acacia': ('Acacia', 'Akazien'),
         'dark_oak': ('Dark Oak', 'Schwarzeichen'), 'mangrove': ('Mangrove', 'Mangroven'), 'cherry': ('Cherry', 'Kirschholz'), 'pale_oak': ('Pale Oak', 'Blasseichen'),
         'bamboo': ('Bamboo', 'Bambus')}
WOODS['jungle'] = ('Jungle', 'Dschungelholz')
TIERS = {'reinforced': ('Reinforced', 'Verstärktes'), 'netherite': ('Netherite', 'Netherit-'), 'enderite': ('Enderite', 'Enderit-')}
ORES = {'fabric:load_conditions': [{'condition': 'fabric:all_mods_loaded', 'values': ['simpleores']}],
        'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'simpleores'}]}


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')


def main():
    lang = {loc: json.loads((RES / f'assets/simpleovens/lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('en_us', 'de_de')}
    for tier, (t_en, t_de) in TIERS.items():
        for wood, (w_en, w_de) in WOODS.items():
            raft = wood == 'bamboo'
            name = f'{tier}_{wood}_furnace_{"raft" if raft else "boat"}'
            vanilla = f'minecraft:{wood}_{"raft" if raft else "boat"}'
            furnace = f'simpleovens:{tier}_furnace'
            extra = ORES if tier == 'enderite' else {}
            dump(RES / f'assets/simpleovens/items/{name}.json', {'model': {'type': 'minecraft:model', 'model': f'simpleovens:item/{name}'}})
            dump(RES / f'assets/simpleovens/models/item/{name}.json', {'parent': 'minecraft:item/generated', 'textures': {
                'layer0': f'minecraft:item/{wood}_{"raft" if raft else "boat"}', 'layer1': f'simpleovens:item/{tier}_furnace_{"raft" if raft else "boat"}_overlay'}})
            dump(RES / f'data/simpleovens/recipe/{name}.json', {**extra, 'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'group': f'{tier}_furnace_boat',
                                                                  'ingredients': [vanilla, furnace], 'result': {'id': f'simpleovens:{name}'}})
            dump(RES / f'data/simpleovens/advancement/recipes/misc/{name}.json', {**extra, 'parent': 'minecraft:recipes/root', 'criteria': {
                f'has_{wood}_{"raft" if raft else "boat"}': {'conditions': {'items': [{'items': vanilla}]}, 'trigger': 'minecraft:inventory_changed'},
                f'has_{tier}_furnace': {'conditions': {'items': [{'items': furnace}]}, 'trigger': 'minecraft:inventory_changed'},
                'has_the_recipe': {'conditions': {'recipes': f'simpleovens:{name}'}, 'trigger': 'minecraft:recipe_unlocked'}},
                'requirements': [['has_the_recipe', f'has_{wood}_{"raft" if raft else "boat"}', f'has_{tier}_furnace']], 'rewards': {'recipes': [f'simpleovens:{name}']}})
            kind_en, kind_de = ('Furnace Raft', 'Ofenfloß') if raft else ('Furnace Boat', 'Ofenboot')
            en = f'{t_en} {w_en} {kind_en}'
            de = f'{t_de}{" " if tier == "reinforced" else ""}{w_de}-{kind_de}'
            for p in ('item', 'entity'):
                lang['en_us'][f'{p}.simpleovens.{name}'] = en
                lang['de_de'][f'{p}.simpleovens.{name}'] = de
    for loc, data in lang.items():
        old = list(json.loads((RES / f'assets/simpleovens/lang/{loc}.json').read_text(encoding='utf-8')))
        ordered = list(old)
        for key in sorted(k for k in data if k not in old):   # the file is not strictly sorted: new keys go before the first greater old key
            ordered.insert(next((i for i, k in enumerate(ordered) if k > key), len(ordered)), key)
        (RES / f'assets/simpleovens/lang/{loc}.json').write_text(json.dumps({k: data[k] for k in ordered}, indent=2, ensure_ascii=False) + '\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
