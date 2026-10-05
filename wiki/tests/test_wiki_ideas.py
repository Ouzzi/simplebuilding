"""Executable evidence for availability, config metadata and acquisition units."""
import itertools
import json
import math
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import generate as g
import config_metadata
import obtain_sources
import trade_sources


class TradeAvailabilityTests(unittest.TestCase):
    def test_formula_matches_all_orders_and_predicate_outcomes(self):
        probabilities = [0.1, 0.25, 0.5, 1]
        # Independent oracle: enumerate Vanilla's candidate removal and retry loop.
        for draws in (0, 1, 2, 4, 6):
            observed = [0.0] * len(probabilities)
            for outcomes in itertools.product((0, 1), repeat=len(probabilities)):
                mass = math.prod(p if yes else 1 - p for p, yes in zip(probabilities, outcomes))
                for order in itertools.permutations(range(len(probabilities))):
                    accepted = [i for i in order if outcomes[i]][:draws]
                    for i in accepted:
                        observed[i] += mass / math.factorial(len(probabilities))
            for index in range(len(probabilities)):
                self.assertAlmostEqual(observed[index], trade_sources.availability(probabilities, index, draws))

    def test_unknown_predicates_are_not_assumed_certain(self):
        self.assertIsNone(trade_sources.offer_probability({'type': 'minecraft:entity_properties'}))
        self.assertIsNone(trade_sources.offer_probability('minecraft:some_predicate'))
        self.assertIsNone(trade_sources.availability([1, None], 0, 2))
        self.assertEqual(0, trade_sources.availability([0, 1], 0, 2))
        self.assertAlmostEqual(.05, trade_sources.availability([.1, 1, 1, 1], 0, 2))
        self.assertGreater(trade_sources.availability([.1, .25, .5, 1], 0, 2), .05)

    def test_discard_bounds_contain_every_possible_context(self):
        bounds = trade_sources.availability_bounds([.1, None, .5, 1], 0, 2)
        for p in (0, .1, .5, 1):
            chance = trade_sources.availability([.1, p, .5, 1], 0, 2)
            self.assertLessEqual(bounds[0], chance)
            self.assertGreaterEqual(bounds[1], chance)
        self.assertTrue(trade_sources.can_discard({'given_item_modifier': [
            {'type': 'minecraft:enchant_randomly'},
            {'type': 'minecraft:filtered', 'on_fail': {'type': 'minecraft:discard'}}]}))

    def test_shipped_trade_json_and_vanilla_sets(self):
        trades = g.collect_trades(g.LINES['26.3'])
        self.assertTrue(trades)
        for trade in trades:
            data = json.loads((g.REPO / trade['source']).read_text(encoding='utf-8'))
            info = trade['availability']
            self.assertEqual(1, len(info['pools']), trade['id'])
            self.assertGreater(info['bounds'][0], 0, trade['id'])
            self.assertLessEqual(info['bounds'][1], trade_sources.offer_probability(data.get('merchant_predicate')))
        diamond = next(t for t in trades if t['id'].endswith('emerald_diamond_core'))
        self.assertIsNone(diamond['availability']['chance'])
        self.assertAlmostEqual(.009203839638622247, diamond['availability']['bounds'][0])
        self.assertEqual(23, diamond['availability']['pools'][0]['poolSize'])
        cache = json.loads((g.WIKI / 'data/vanilla-trades-26.3.json').read_text(encoding='utf-8'))
        for group, draws in [('buying', 2), ('common', 5), ('uncommon', 2)]:
            self.assertEqual(draws, cache['sets']['minecraft:wandering_trader/' + group]['amount'])

    def test_tag_merge_replace_nested_optional_and_unknown_pool(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cache = root / 'cache.json'
            cache.write_text(json.dumps({'sets': {'minecraft:pool': {'trades': '#minecraft:pool', 'amount': 1}},
                                        'tags': {'minecraft:pool': {'values': ['minecraft:a']}},
                                        'predicates': {'minecraft:a': None}}))
            tags = root / 'data/minecraft/tags/villager_trade'
            tags.mkdir(parents=True)
            (tags / 'pool.json').write_text(json.dumps({'replace': True, 'values': ['#minecraft:child']}))
            (tags / 'child.json').write_text(json.dumps({'values': ['minecraft:a', 'minecraft:a', {'id': 'mod:missing', 'required': False}]}))
            trades = [{'id': 'minecraft:a'}]
            trade_sources.annotate(trades, [root / 'data'], cache)
            self.assertEqual(1, trades[0]['availability']['chance'])
            self.assertEqual(1, trades[0]['availability']['pools'][0]['poolSize'])
            (tags / 'child.json').write_text(json.dumps({'values': ['mod:missing']}))
            with self.assertRaisesRegex(ValueError, 'Missing required trade'):
                trade_sources.annotate(trades, [root / 'data'], cache)


class ConfigMetadataTests(unittest.TestCase):
    def test_real_validation_and_executable_scope_sets(self):
        configs = {c['name']: c for c in g.collect_config(g.LINES['26.3'], {})}
        legacy = {c['name']: c for c in g.collect_config(g.LINES['26.2'], {})}
        for name, bounds in [('hudScale', [50, 200]), ('airJumpCooldownTicks', [20, 6000]),
                             ('server.tools.ironChiselCooldownTicks', [2, 200]),
                             ('server.loot.globalLootMultiplier', [0, 3]),
                             ('server.features.scarecrowRadius', [0, 16]),
                             ('server.trimStrengths.walkingSpeed', [0, 2])]:
            self.assertEqual(bounds, configs[name]['range'], name)
        self.assertEqual('client', configs['hudScale']['side'])
        self.assertEqual('server', configs['worldGen.enableLootTableChanges']['side'])
        self.assertEqual('yes', configs['worldGen.enableLootTableChanges']['reload'])
        self.assertEqual('recipes', configs['server.features.backpack']['reload'])
        self.assertEqual('restart', configs['server.charges.lensMaxCharge']['reload'])
        for name, maximum in [('tools.wandHungerMultiplier', 10), ('tools.magnetRangeMultiplier', 4),
                              ('tools.rotatorChargePerTurn', 4096), ('worldGen.buildingCoreLootChanceMultiplier', 1000),
                              ('tweaks.laserPointer.chargePerSecond', 2560), ('tweaks.laserPointer.effectCost', 2560)]:
            self.assertEqual([0, maximum], configs[name]['range'], name)
            self.assertEqual([0, None], legacy[name]['range'], name)
            self.assertEqual('server', configs[name]['side'], name)

    def test_comments_and_unresolved_bounds_are_not_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'Example.java'
            path.write_text('''class Example {
                // speed = clamp(speed, 1, 99);
                void validate() {
                    speed = clamp(speed, 1, UNKNOWN);
                    other = Math.max(2, Math.min(9, other));
                }
            }''')
            self.assertEqual({(path, 'Example', 'other'): [2, 9], (path, 'Example', 'speed'): [1, None]},
                             config_metadata.read_metadata([path]))


class LootMetricsTests(unittest.TestCase):
    def test_pool_expectation_shared_tables_and_bernoulli_subset(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'Loot.java'
            path.write_text('''class Loot {
                public static void apply(Object key) {
                    if (BuiltInLootTables.BASTION_TREASURE.equals(key) || BuiltInLootTables.BASTION_OTHER.equals(key)) {
                        editor.addPool(LootPool.lootPool().setRolls(LootNumbers.between(1, 3))
                            .add(counted(ModItems.TOKEN, 1, 2, 4)).add(EmptyLootItem.emptyItem().setWeight(3)));
                    }
                    if (BuiltInLootTables.SIMPLE_DUNGEON.equals(key)) {
                        rareCore(editor, ModItems.TOKEN, 0.01f);
                    }
                }
            }''')
            sources, problems = obtain_sources.parse_mod_loot(path, {'test:token'}, set(), 'test')
            self.assertEqual([], problems)
            shared = sources[:2]
            self.assertEqual(2, len(shared))
            for source in shared:
                self.assertEqual(1.5, source['perChest'])  # E[R]=2, share=1/4, E[C]=3
                self.assertEqual(2, len(source['sharedTables']))
                self.assertNotIn('expectedAttempts', source)
            self.assertAlmostEqual(100, sources[2]['expectedAttempts']['first'])
            self.assertAlmostEqual(600, sources[2]['expectedAttempts']['sixth'])

    def test_chance_and_mean_are_different(self):
        rolls = {'type': 'binomial', 'n': 4, 'p': .5}
        self.assertAlmostEqual(1 - .75**4, obtain_sources.chance_at_least_one(rolls, .5))
        self.assertEqual(2, obtain_sources.expected_rolls(rolls))
