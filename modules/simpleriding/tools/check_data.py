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
        if 'build' not in path.parts:
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
    assert entry['version'] == '1.0.5' and entry['loaders'] == ['fabric', 'neoforge']
    lang_dir = MODULE / 'shared/resources/assets/simpleriding/lang'
    en, de = [read(lang_dir / f'{language}.json') for language in ('en_us', 'de_de')]
    assert en.keys() == de.keys(), 'language completeness'
    assert all(isinstance(v, str) and v for language in (en, de) for v in language.values())
    options = read(ROOT / 'balance/simpleriding/options.json')['options']
    assert len(options) == 6 and len({o['path'] for o in options}) == 6
    for option in options:
        for language in (en, de):
            assert option['nameKey'] in language and option['tooltipKey'] in language
            assert str(option['default']).lower() in language[option['tooltipKey']]
            assert 'text.autoconfig.simpleriding.option.' + option['tab'] in language
    manual = read(MODULE / 'wiki/manual.json')['features']
    assert len({f['id'] for f in manual}) == len(manual)
    assert len(manual) == 12, 'all gameplay/registry chapters and six config options'
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
    assert {'id': 'simplebuilding:enderite_horse_armor', 'required': False} in armor['values']
    assert not list((MODULE / 'shared/resources/assets/simpleriding').glob('items/*.json')), 'no invented item models'
    print('Simple Riding: manifest, bilingual wiki/config, legacy IDs, and 26.3 trade data valid')


if __name__ == '__main__':
    check()
