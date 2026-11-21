"""Static module contract, language, wiki, and 26.3 data integrity gate."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
MODULE = ROOT / 'modules/simpleriding'


def unique(pairs):
    result = {}
    for key, value in pairs:
        assert key not in result, f'duplicate JSON key: {key}'
        result[key] = value
    return result


def read(path):
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique)


def check():
    for path in MODULE.rglob('*.json'):
        if not any(part == 'build' or part == 'run' or part.startswith('run-') for part in path.relative_to(MODULE).parts):
            read(path)
    manifest = read(ROOT / 'modules/modules.json')
    fields = {'id', 'name', 'displayName', 'description', 'version', 'loaders', 'minecraft', 'paths', 'requires', 'optional'}
    paths = {'root', 'shared', 'fabric', 'neoforge', 'forge', 'generated', 'lang', 'wikiManual', 'balanceDir'}
    for entry in manifest['modules']:
        assert fields <= entry.keys(), f'incomplete manifest: {entry["id"]}'
        assert paths <= entry['paths'].keys(), f'incomplete paths: {entry["id"]}'
        assert set(entry['loaders']) <= {'fabric', 'neoforge', 'forge'}
        assert entry['minecraft'] == '26.3'
    entry = next(e for e in manifest['modules'] if e['id'] == 'simpleriding')
    assert entry['version'] in ('0.9.0', '0.9.0-beta.1') and entry['loaders'] == ['fabric', 'neoforge', 'forge']
    lang_dir = MODULE / 'shared/resources/assets/simpleriding/lang'
    en, de = [read(lang_dir / f'{language}.json') for language in ('en_us', 'de_de')]
    assert en.keys() == de.keys(), 'language completeness'
    assert all(isinstance(v, str) and v for language in (en, de) for v in language.values())
    options = read(ROOT / 'balance/simpleriding/options.json')['options']
    assert len(options) == 26 and len({o["path"] for o in options}) == 26
    catalogue = (MODULE / 'shared/java/com/simpleriding/RidingOptions.java').read_text()
    screen = (MODULE / 'shared/java/com/simpleriding/client/RidingConfigScreen.java').read_text()
    assert 'for(var option:RidingOptions.ALL)' in screen
    for option in options:
        assert f'new Option("{option["path"]}"' in catalogue, 'GUI catalogue covers producer metadata'
        if option['maximum'] is not None:
            assert option.get('minimum', 0) <= option['default'] <= option['maximum']
        for language in (en, de):
            assert option['nameKey'] in language and option['tooltipKey'] in language
            assert str(option['default']).lower() in language[option['tooltipKey']]
            assert 'text.autoconfig.simpleriding.option.' + option['tab'] in language
    manual = read(MODULE / 'wiki/manual.json')['features']
    assert len({f['id'] for f in manual}) == len(manual)
    assert len(manual) == 11 + len(options) and any(f['id'] == 'forge_263' for f in manual) and any(f['id'] == 'commands' for f in manual), 'all gameplay/registry/security/horseshoe and creative-tab chapters, config options, the commands (CMD24) and the Forge support chapter'
    for feature in manual:
        for language in ('en', 'de'):
            assert feature[language]['title'] and feature[language]['summary'] and feature[language]['details']
        assert feature['sources'] and all((ROOT / p).is_file() for p in feature['sources'])
    resources = MODULE / 'generated/resources/data'
    for level in (2, 3, 4):
        trade_id = f'simpleriding:librarian/{level}/riding_book'
        trade = read(resources / f'simpleriding/villager_trade/librarian/{level}/riding_book.json')
        assert 'given_item_modifiers' not in trade and trade['given_item_modifier']
        function = trade['given_item_modifier'][0]
        assert function['type'] == 'simpleriding:weighted_enchant' and 'function' not in function
        assert trade['wants']['count']['type'] == 'minecraft:uniform'
        assert {e['enchantment'] for e in function['pool']} == {'simpleriding:tailwind', 'simpleriding:leaping'}
        assert all(e['level'] == level - 1 for e in function['pool'])
        tag = read(resources / f'minecraft/tags/villager_trade/librarian/level_{level}.json')
        assert not tag.get('replace') and {'id': trade_id, 'required': False} in tag['values']
    armor = read(resources / 'simpleriding/tags/item/horse_armor_enchantable.json')
    assert {'id': 'simpleores:enderite_horse_armor', 'required': False} in armor['values']
    nautilus = read(resources / 'simpleriding/tags/item/nautilus_armor_enchantable.json')
    assert {'id': 'simpleores:enderite_nautilus_armor', 'required': False} in nautilus['values']
    assert len(nautilus['values']) == 8  # N89A: + rose_bronze/rose_gold nautilus armor (optional)
    for a in ('rose_bronze', 'rose_gold'):
        assert {'id': f'simpleores:{a}_horse_armor', 'required': False} in armor['values']
        assert {'id': f'simpleores:{a}_nautilus_armor', 'required': False} in nautilus['values']
    assert read(resources / 'simpleriding/enchantment/leaping.json')['supported_items'] == '#simpleriding:mount_armor_enchantable'
    assets = MODULE / 'shared/resources/assets/simpleriding'
    tiers = ['copper', 'iron', 'golden', 'diamond', 'netherite', 'enderite', 'rose_bronze', 'rose_gold']  # N89A: the two rose alloys
    items = [t + '_horseshoe' for t in tiers] + ['horseshoe_smithing_template']
    books = ['enchanted_book_leaping', 'enchanted_book_tailwind']
    # guide_book: the generated module guide (tools/guides/module_guides.py).
    assert sorted(p.stem for p in assets.glob('items/*.json')) == sorted(items + books + ['guide_book']), 'horseshoes, Riding book models and the guide'
    for book in books:
        assert read(assets / f'items/{book}.json')['model'] == {'type': 'minecraft:model', 'model': f'simpleriding:item/{book}'}
        assert read(assets / f'models/item/{book}.json') == {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'simpleriding:item/{book}'}}
        png = (assets / f'textures/item/{book}.png').read_bytes()
        assert png[:8] == b'\x89PNG\r\n\x1a\n' and int.from_bytes(png[16:20], 'big') == int.from_bytes(png[20:24], 'big') == 16
    assert not (assets.parent / 'minecraft/items/enchanted_book.json').exists(), 'do not replace other mods book selection'
    assert 'client.RidingBookModelMixin' in read(MODULE / 'shared/resources/simpleriding.mixins.json')['client']
    for item in items:
        assert read(assets / f'items/{item}.json')['model']['model'] == f'simpleriding:item/{item}'
        assert (assets / f'textures/item/{item}.png').is_file()
        for language in (en, de):
            assert language.get(f'item.simpleriding.{item}'), f'name for {item}'
    for tier in tiers:
        assert (assets / f'textures/entity/horseshoe/{tier}.png').is_file(), 'worn hoof texture'
    assert (assets / 'textures/gui/sprites/container/slot/horseshoe.png').is_file()
    # N129: the hoof panel is drawn in code (HorseshoeScreenMixin -> UiBoxes); a PNG here would be a dead asset again
    assert not (assets / 'textures/gui/container/horseshoe_panel.png').exists(), 'dead horseshoe_panel.png is back'
    recipes = resources / 'simpleriding/recipe'
    template = 'simpleriding:horseshoe_smithing_template'
    for tier, base, addition in [('copper', 'minecraft:copper_ingot', 'minecraft:iron_nugget'), ('iron', 'minecraft:iron_ingot', 'minecraft:iron_nugget'),
                                 ('golden', 'minecraft:gold_ingot', 'minecraft:iron_nugget'), ('diamond', 'minecraft:diamond', 'minecraft:iron_nugget'),
                                 ('netherite', 'simpleriding:diamond_horseshoe', 'minecraft:netherite_ingot'),
                                 ('enderite', 'simpleriding:netherite_horseshoe', '#c:ingots/enderite')]:
        recipe = read(recipes / f'{tier}_horseshoe_smithing.json')
        assert recipe['type'] == 'minecraft:smithing_transform' and recipe['template'] == template
        assert recipe['base'] == base and recipe['addition'] == addition and recipe['result']['id'] == f'simpleriding:{tier}_horseshoe'
    # AUDSB31E1 F17: the Enderite horseshoe is always registered; its recipe takes the item tag and loads only while the tag has content (no mod name).
    enderite = read(recipes / 'enderite_horseshoe_smithing.json')
    assert enderite['addition'] == '#c:ingots/enderite'
    assert enderite['fabric:load_conditions'] == [{'condition': 'fabric:tags_populated', 'registry': 'minecraft:item', 'values': ['c:ingots/enderite']}]
    assert enderite['neoforge:conditions'] == [{'type': 'neoforge:not', 'value': {'type': 'neoforge:tag_empty', 'tag': 'c:ingots/enderite'}}]
    assert enderite['forge:conditions'] == [{'type': 'forge:not', 'value': {'type': 'forge:tag_empty', 'tag': 'c:ingots/enderite'}}]
    assert 'simplebuilding' not in json.dumps(enderite), 'the Enderite recipe must not depend on a mod name'
    advancement = read(MODULE / 'shared/resources/data/simpleriding/advancement/recipes/misc/enderite_horseshoe_smithing.json')
    for key in ('fabric:load_conditions', 'neoforge:conditions', 'forge:conditions'):
        assert advancement[key] == enderite[key], f'advancement and recipe share the {key}'
    # N89A: the rose horseshoes, like Enderite: always registered, recipe on c:ingots/<alloy>, loaded only while the tag has content
    for alloy in ('rose_bronze', 'rose_gold'):
        rose = read(recipes / f'{alloy}_horseshoe_smithing.json')
        tag = f'c:ingots/{alloy}'
        assert rose['type'] == 'minecraft:smithing_transform' and rose['template'] == template and rose['base'] == '#' + tag
        assert rose['addition'] == 'minecraft:iron_nugget' and rose['result']['id'] == f'simpleriding:{alloy}_horseshoe'
        assert rose['fabric:load_conditions'] == [{'condition': 'fabric:tags_populated', 'registry': 'minecraft:item', 'values': [tag]}]
        assert rose['neoforge:conditions'] == [{'type': 'neoforge:not', 'value': {'type': 'neoforge:tag_empty', 'tag': tag}}]
        assert rose['forge:conditions'] == [{'type': 'forge:not', 'value': {'type': 'forge:tag_empty', 'tag': tag}}]
        adv = read(MODULE / f'shared/resources/data/simpleriding/advancement/recipes/misc/{alloy}_horseshoe_smithing.json')
        assert all(adv[k] == rose[k] for k in ('fabric:load_conditions', 'neoforge:conditions', 'forge:conditions'))
        assert f'simpleriding:{alloy}_horseshoe' in read(resources / 'simpleriding/tags/item/horseshoes.json')['values']
    assert 'simpleriding:enderite_horseshoe' in read(resources / 'simpleriding/tags/item/horseshoes.json')['values'], 'always-registered Enderite horseshoe is a plain tag entry'
    registry_java = (MODULE / 'shared/java/com/simpleriding/Horseshoes.java').read_text(encoding='utf-8')
    assert 'ENDERITE&&!' not in registry_java.replace(' ', ''), 'no tier is skipped at registration'
    assert read(resources / 'simpleriding/tags/item/repairs_enderite_horseshoe.json')['values'] == [
        {'id': '#c:ingots/enderite', 'required': False}]
    # The ingot is Simple Ores' on 26.3 (IG31-13; before SimpleLib's, split stage 3d); it publishes the common tag.
    assert 'simpleores:enderite_ingot' in read(ROOT / 'modules/simpleores/shared/resources/data/c/tags/item/ingots/enderite.json')['values']
    duplicate = read(recipes / 'horseshoe_smithing_template.json')
    assert duplicate['pattern'] == ['#S#', '#C#', '###'] and duplicate['key'] == {'#': 'minecraft:copper_ingot', 'C': 'minecraft:iron_ingot', 'S': template}
    assert duplicate['result'] == {'count': 2, 'id': template}
    assert read(resources / 'minecraft/tags/item/enchantable/durability.json')['values'] == ['#simpleriding:horseshoes']
    wearers = read(resources / 'simpleriding/tags/entity_type/can_wear_horseshoes.json')['values']
    assert set(wearers) == {'minecraft:horse', 'minecraft:donkey', 'minecraft:mule', 'minecraft:skeleton_horse', 'minecraft:zombie_horse'}
    print('Simple Riding: manifest, bilingual wiki/config, legacy IDs, horseshoes, and 26.3 trade data valid')


if __name__ == '__main__':
    check()
