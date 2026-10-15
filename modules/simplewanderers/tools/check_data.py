"""Module-owned data hook: bilingual texts, config keys in the wiki, trade pools (split of the Wandering Trader's goods), texture and wiki sources."""
import json
import re
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
NS = 'simplewanderers'
RES = MODULE / 'shared/resources'
JAVA = MODULE / 'shared/java/com/simplewanderers'
keys = re.search(r'KEYS = List\.of\(([^;]*)\);', (JAVA / 'config/WandererConfig.java').read_text(encoding='utf-8'), re.S).group(1)
config_keys = re.findall(r'"([A-Za-z]+)"', keys)
assert config_keys == ['enabled', 'respawnDays', 'rerollDays', 'restockDays', 'villageRadius', 'homeRadius', 'horse',
                       'raidBlocksSpawn', 'biomeAssortment'], config_keys
langs = {}
for locale in ('en_us', 'de_de'):
    data = json.loads((RES / f'assets/{NS}/lang/{locale}.json').read_text(encoding='utf-8'))
    langs[locale] = data
    for k in (f'entity.{NS}.village_wanderer', f'entity.{NS}.wanderer_horse', f'item.{NS}.village_wanderer_spawn_egg',
              f'item.{NS}.guide_book', f'{NS}.testcentre.station'):
        assert data.get(k), (locale, k)
assert set(langs['en_us']) == set(langs['de_de']), 'EN/DE lang keys differ'

# Trade pools: the Vanilla common pool (78) is split 47 wanderer / 31 traveling trader, nothing twice, nothing lost.
tags = RES / f'data/{NS}/tags/villager_trade'


def values(path):
    out = []
    for v in json.loads(path.read_text(encoding='utf-8'))['values']:
        i = v if isinstance(v, str) else v['id']
        out.append(i)
        if not isinstance(v, str) and not i.startswith('minecraft:'):
            assert v.get('required') is False, ('foreign trade must be optional', path.name, i)
    return out


dw = [i for i in values(tags / 'wanderer/common.json') if i.startswith('minecraft:')]
wt = [i for i in values(tags / 'wandering_trader/common.json') if i.startswith('minecraft:')]
assert len(dw) == 47 and len(wt) == 31 and not set(dw) & set(wt), (len(dw), len(wt))
for pool in ('buying', 'common', 'uncommon', 'rare'):
    for i in values(tags / f'wanderer/{pool}.json'):
        assert 'core' not in i and 'wand_book' not in i, ('a core or wand book at the wanderer', i)
for biome in ('desert', 'jungle', 'plains', 'savanna', 'snow', 'swamp', 'taiga'):
    assert len(values(tags / f'wanderer/biome/{biome}.json')) >= 4, biome
    assert (RES / f'data/{NS}/trade_set/wanderer/biome/{biome}.json').is_file(), biome
for name in ('buying', 'common'):
    override = json.loads((RES / f'data/minecraft/trade_set/wandering_trader/{name}.json').read_text(encoding='utf-8'))
    assert override['trades'] == f'#{NS}:wandering_trader/{name}', name
for tex in ('entity/village_wanderer', 'item/village_wanderer_spawn_egg', 'item/guide_book'):
    assert (RES / f'assets/{NS}/textures/{tex}.png').is_file(), tex
assert (RES / f'data/{NS}/structure/arena.nbt').is_file()
manual = json.loads((MODULE / 'wiki/manual.json').read_text(encoding='utf-8'))
config = next(f for f in manual['features'] if f['id'] == 'config_server')
for locale in ('en', 'de'):
    text = ' '.join(config[locale]['details'])
    for k in config_keys:
        assert k in text, ('wiki config lacks', locale, k)
for f in manual['features']:
    for src in f['sources']:
        assert (MODULE.parents[1] / src).is_file(), src
print(f'{NS}: {len(config_keys)} config keys, trade split 47/31, biome pools, bilingual texts, texture and wiki sources valid')
