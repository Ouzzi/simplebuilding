"""Reference values for the time-to-k-th-piece model; wiki/tests/time_ui.cjs checks the JavaScript against them."""
import math
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import obtain_sources
import obtain_time as t

PAD = [0.0] * (t.K_MAX - 1)


def pool(share, count=(1, 1), rolls=None, pool_id=0, item='t:x', table='minecraft:chests/a'):
    return {'type': 'chest', 'item': item, 'table': table, 'label': {'en': 'A', 'de': 'A'}, 'count': list(count),
            'weight': share, 'totalWeight': 1, 'rolls': rolls or {'type': 'exactly', 'n': 1}, 'pool': pool_id}


class ObtainTime(unittest.TestCase):
    def test_bernoulli_matches_closed_forms(self):
        lam, h = t.combine([(2.0, [0.96, 0.04] + PAD)])
        self.assertAlmostEqual(0.08, lam)
        one = t.time_stats(lam, h, 1)
        self.assertAlmostEqual(12.5, one['mean'])
        self.assertAlmostEqual(math.log(2) / 0.08, one['median'], places=6)
        self.assertAlmostEqual(math.log(10) / 0.08, one['q90'], places=6)
        six = t.time_stats(lam, h, 6)  # Erlang(6, 0.08): mean 75, median 5.67016 / lam
        self.assertAlmostEqual(75.0, six['mean'])
        self.assertAlmostEqual(5.670164 / 0.08, six['median'], places=3)
        self.assertAlmostEqual(9.274 / 0.08, six['q90'], delta=0.01)

    def test_streams_from_pools(self):
        s = t.build_streams([pool(.04)], obtain_sources.roll_distribution)[0]
        self.assertAlmostEqual(.04, s['pmf'][1])
        self.assertAlmostEqual(.96, s['pmf'][0])
        # two rolls of 1/4, count 1..3: P(nothing) = (3/4)^2
        s = t.build_streams([pool(.25, (1, 3), {'type': 'exactly', 'n': 2})], obtain_sources.roll_distribution)[0]
        self.assertAlmostEqual(.5625, s['pmf'][0])
        self.assertAlmostEqual(1.0, sum(s['pmf']))
        self.assertAlmostEqual(2 * .25 * 2, sum(i * q for i, q in enumerate(s['pmf'])))  # E[Y] = rolls * share * mean count

    def test_gaps(self):
        self.assertIsNone(t.combine([(0.0, [0.96, 0.04] + PAD)]))
        self.assertIsNone(t.combine([(5.0, [1.0] + [0.0] * t.K_MAX)]))
        book = pool(.5)
        book['enchantment'] = 'x:y'
        self.assertEqual([], t.build_streams([book], obtain_sources.roll_distribution))

    def test_pieces_per_opening_shorten_the_time(self):
        lam, h = t.combine([(1.0, [0.0, 0.0, 1.0] + PAD)])  # always exactly two pieces
        self.assertAlmostEqual(3.0, t.time_stats(lam, h, 5)['mean'])  # 3 openings for 5 pieces

    def test_superposition(self):
        lam, h = t.combine([(1.0, [0.9, 0.1] + PAD), (3.0, [0.9, 0.1] + PAD)])
        self.assertAlmostEqual(0.4, lam)
        self.assertAlmostEqual(2.5, t.time_stats(lam, h, 1)['mean'])


if __name__ == '__main__':
    unittest.main()
