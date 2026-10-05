"""Die Rechner: geschlossene Formeln gegen bekannte Werte und gegen docs/KERNE-SELTENHEIT.md."""

import math
import random
import unittest

import helpers
from sbdev import model


class FormulaTests(unittest.TestCase):
    def test_single_items_follow_the_gamma_distribution(self):
        lam = 0.25
        dist = [0.0, 1.0] + [0.0] * 5  # jedes Ereignis genau 1 Stueck
        t = model.compound_times([(lam, dist)], [1, 2, 3])
        self.assertAlmostEqual(t["mean"][0], 4.0)
        self.assertAlmostEqual(t["mean"][2], 12.0)
        self.assertAlmostEqual(t["median"][0], math.log(2) / lam, places=6)
        self.assertAlmostEqual(t["p90"][0], math.log(10) / lam, places=6)
        # Median der Gamma(2)-Verteilung: 1,678 / lam
        self.assertAlmostEqual(t["median"][1] * lam, 1.678346990, places=6)

    def test_zero_events_are_thinned_out(self):
        # 10 Ereignisse/h, je 5 % ein Stueck == 0,5 Treffer/h
        dist = [0.95, 0.05] + [0.0] * 5
        t = model.compound_times([(10.0, dist)], [1])
        self.assertAlmostEqual(t["mean"][0], 2.0)

    def test_multi_item_events_match_a_seeded_simulation(self):
        # ein Ereignis gibt 2-5 Stueck mit 30 %: Zeit bis 6 Stueck
        count = model.uniform_count(2, 5, 6)
        dist = [0.7 + 0.3 * count[0]] + [0.3 * c for c in count[1:]]
        exact = model.compound_times([(1.0, dist)], [6])["mean"][0]
        rng = random.Random(7)
        total, runs = 0.0, 40000
        for _ in range(runs):
            t, have = 0.0, 0
            while have < 6:
                t += rng.expovariate(1.0)
                if rng.random() < 0.3:
                    have += rng.randint(2, 5)
            total += t
        self.assertAlmostEqual(exact, total / runs, delta=0.05)

    def test_nothing_means_never(self):
        t = model.compound_times([(0.0, [1.0, 0, 0])], [1, 2])
        self.assertEqual(t["mean"], [math.inf, math.inf])

    def test_offer_probability_matches_brute_force(self):
        others = [1.0] * 5 + [0.5, 0.25]
        exact = model.offer_probability(0.1, others, 2)
        rng = random.Random(3)
        hits, runs = 0, 200000
        chances = others + [0.1]
        for _ in range(runs):
            order = list(range(len(chances)))
            rng.shuffle(order)
            offered = 0
            for idx in order:
                if rng.random() < chances[idx]:
                    if idx == len(chances) - 1:
                        hits += 1
                        break
                    offered += 1
                    if offered == 2:
                        break
        self.assertAlmostEqual(exact, hits / runs, delta=0.003)


class DocumentedNumbersTests(unittest.TestCase):
    """docs/KERNE-SELTENHEIT.md Abschnitt 5.3 - dieselben Annahmen muessen dieselben Zahlen geben."""

    @classmethod
    def setUpClass(cls):
        cls.service = helpers.fresh_store(helpers.shared_service())
        cls.ctx = cls.service.ctx({})

    def row(self, item, key):
        report = model.item_report(self.ctx, item)
        return next(r for r in report["rows"] if r["key"] == key), report

    def test_core_times_from_section_5_3(self):
        cases = {  # Item, Zeile, Mittel 1., Mediane 1..6
            "gold_core": ("structure:bastion", 12.5, [8.7, 21.1, 33.6, 46.1, 58.6, 71.2]),
            "diamond_core": ("structure:trial_chambers", 21.0, [14.6, 35.3, 56.2, 77.2, 98.2, 119.2]),
            "netherite_core": ("structure:bastion", 24.9, [17.2, 41.7, 66.5, 91.3, 116.2, 141.0]),
            # 26.3: 15 chests/hour at 0.5%; gamma medians for the first six successes.
            "enderite_core": ("structure:end_city", 13.3, [9.2, 22.4, 35.7, 49.0, 62.3, 75.6]),
        }
        for item, (key, mean, medians) in cases.items():
            row, _ = self.row(f"simplebuilding:{item}", key)
            self.assertAlmostEqual(row["mean"][0], mean, delta=0.06, msg=item)
            for got, want in zip(row["median"], medians):
                self.assertAlmostEqual(got, want, delta=0.06, msg=item)

    def test_iron_core_with_mansion_and_mineshaft_together(self):
        report = model.item_report(self.ctx, "simplebuilding:iron_core")
        # KERNE-SELTENHEIT 5.3: Anwesen 8/h x 1,5 % + Mine 6/h x 0,5 % -> 0,150/h; Trader kommt hier dazu
        structures = [r for r in report["rows"] if r["kind"] == "structure"]
        lam = sum(1 / r["mean"][0] for r in structures)
        self.assertAlmostEqual(lam, 0.150, places=3)

    def test_wandering_trader_offer_chances(self):
        # KERNE-SELTENHEIT 5.3: je Haendlerbesuch ~10,1 / 4,9 / 2,4 / 1,0 % (26.2)
        want = {"copper_core": 0.101, "iron_core": 0.049, "gold_core": 0.024, "diamond_core": 0.010}
        for item, p in want.items():
            report = model.item_report(self.ctx, f"simplebuilding:{item}")
            row = next(r for r in report["rows"] if r["kind"] == "wandering")
            self.assertAlmostEqual(row["offerChance"], p, delta=0.0015, msg=item)

    def test_reverse_round_trip(self):
        vid = "const:ModLootTableModifications.ENDERITE_CORE_CHANCE"
        entries = self.service.store.state()["entries"]

        def ctx_with(extra):
            return model.Ctx(self.service.snapshot, self.service.effective_fn(entries, extra))

        res = model.reverse(self.service.snapshot, ctx_with, "simplebuilding:enderite_core", "structure:end_city",
                            {"id": vid}, 1, "mean", "targeted", 45.0)
        self.assertTrue(res["feasible"])
        self.assertAlmostEqual(res["value"], 1 / (15 * 45), delta=2e-6)
        self.assertAlmostEqual(res["achieved"], 45.0, delta=0.1)
        impossible = model.reverse(self.service.snapshot, ctx_with, "simplebuilding:enderite_core", "structure:end_city",
                                   {"id": vid}, 1, "mean", "targeted", 0.01)
        self.assertFalse(impossible["feasible"])
        self.assertIn("nicht erreichbar", impossible["message"])

    def test_planned_source_and_switched_off_source(self):
        item = "simplebuilding:enderite_core"
        spec = {"kind": "mob", "label": "Shulker", "chance": 0.01, "countMin": 1, "countMax": 1, "rate": 20.0}
        ctx = self.service.ctx({f"source:{item}:qtest": spec})
        report = model.item_report(ctx, item)
        planned = next(r for r in report["rows"] if r["kind"] == "custom")
        self.assertAlmostEqual(planned["mean"][0], 5.0)
        self.assertEqual(report["best"]["key"], f"source:{item}:qtest")
        ctx_off = self.service.ctx({f"sourceoff:{item}:structure:end_city": True})
        report_off = model.item_report(ctx_off, item)
        self.assertTrue(next(r for r in report_off["rows"] if r["key"] == "structure:end_city")["disabled"])
        self.assertNotEqual((report_off["best"] or {}).get("key"), "structure:end_city")

    def test_draft_values_change_the_result(self):
        vid = "const:ModLootTableModifications.ENDERITE_CORE_CHANCE"
        before, _ = self.row("simplebuilding:enderite_core", "structure:end_city")
        ctx = self.service.ctx({vid: 0.01})
        after = next(r for r in model.item_report(ctx, "simplebuilding:enderite_core")["rows"] if r["key"] == "structure:end_city")
        self.assertAlmostEqual(after["mean"][0], before["mean"][0] / 2, delta=0.05)


if __name__ == "__main__":
    unittest.main()
