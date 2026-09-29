"""
Zeit -> Stellwerte (sbdev/solver.py, POST /api/solve-time): eine eingetippte Zeit ergibt Werte, die neu
gerechnet genau diese Zeit liefern; mehrere Quellen proportional oder einzeln; Kappen an 0/100 %; Händler-
und Truhen-Quellen; und nichts wird geschrieben, solange der Besitzer nicht bestätigt.
"""

import math
import unittest

import helpers
from sbdev import model, solver
from sbdev.service import Service

ENDERITE = "const:ModLootTableModifications.ENDERITE_CORE_CHANCE"
IRON = "const:ModLootTableModifications.IRON_CORE_CHANCE"
IRON_MINE = "const:ModLootTableModifications.IRON_CORE_MINESHAFT_CHANCE"
IRON_TRADE = "trade:simplebuilding:wandering_trader/emerald_iron_cores:offerChance"
LOOT = "common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java"
LOOT_1211 = "mc1_21_11/shared/java/com/simplebuilding/loot/ModLootTableModifications.java"


class SolverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.service = helpers.fresh_store(helpers.shared_service())

    def solve(self, **kw):
        return self.service.solve_time(kw)

    def recompute(self, res):
        """Die Zeit mit den gelieferten Werten neu gerechnet - unabhängig vom Löser."""
        ctx = self.service.ctx(res["values"])
        return model.metric(ctx, res["item"], res["row"], res["mode"], res["stat"], res["k"])

    def test_round_trip_single_chest_source(self):
        res = self.solve(item="simplebuilding:enderite_core", row="structure:end_city", stat="mean", k=1, hours=20)
        self.assertTrue(res["feasible"], res["message"])
        self.assertEqual([l["id"] for l in res["lines"]], [ENDERITE])
        self.assertAlmostEqual(res["current"], 38.1, delta=0.1)
        # neu gerechnet = Ziel (Chance auf 4 gültige Stellen gerundet)
        self.assertAlmostEqual(self.recompute(res), 20.0, delta=20.0 * 0.001)
        self.assertAlmostEqual(res["achieved"], self.recompute(res), places=9)
        line = res["lines"][0]
        self.assertEqual(line["old"], 0.00175)
        self.assertAlmostEqual(line["new"], 1 / (15 * 20), delta=2e-6)
        self.assertEqual(line["apply"], "mod")
        self.assertTrue(line["applyNow"])
        self.assertTrue(any(s["file"].endswith("ModLootTableModifications.java") and s["line"] for s in line["sites"]))
        self.assertIn("1.21.11", " ".join(s["mc"] for s in line["sites"]))
        # alle anderen Zeiten folgen: das 2. Stück dauert jetzt doppelt so lange wie das 1. (Poisson, 1 je Treffer)
        row = next(r for r in res["after"]["rows"] if r["key"] == "structure:end_city")
        self.assertAlmostEqual(row["mean"][0], res["achieved"], places=6)
        self.assertAlmostEqual(row["mean"][1], 2 * res["achieved"], places=6)
        before = next(r for r in res["before"]["rows"] if r["key"] == "structure:end_city")
        self.assertAlmostEqual(before["mean"][0], res["current"], places=6)

    def test_round_trip_every_stat_and_k(self):
        for stat in ("mean", "median", "p90"):
            for k in (1, 3, 6):
                cur = model.metric(self.service.ctx(), "simplebuilding:enderite_core", "structure:end_city", "targeted", stat, k)
                target = cur * 0.6
                res = self.solve(item="simplebuilding:enderite_core", row="structure:end_city", stat=stat, k=k, hours=target)
                self.assertTrue(res["feasible"], (stat, k, res["message"]))
                self.assertAlmostEqual(self.recompute(res), target, delta=target * 0.001, msg=(stat, k))

    def test_multi_source_proportional_is_the_default(self):
        res = self.solve(item="simplebuilding:iron_core", row="__together__", stat="mean", k=1, hours=3)
        self.assertTrue(res["feasible"], res["message"])
        self.assertEqual(res["strategy"], "proportional")
        ids = {l["id"] for l in res["lines"]}
        self.assertEqual(ids, {IRON, IRON_MINE, IRON_TRADE})
        # ein gemeinsamer Faktor: jedes Verhältnis neu/alt gleich (bis auf die Rundung auf 4 Stellen)
        ratios = [l["newNumber"] / l["oldNumber"] for l in res["lines"]]
        for r in ratios:
            self.assertAlmostEqual(r, res["factor"], delta=res["factor"] * 1e-3)
        self.assertAlmostEqual(self.recompute(res), 3.0, delta=0.003)

    def test_multi_source_only_one_chosen_source(self):
        res = self.solve(item="simplebuilding:iron_core", row="__normal__", stat="mean", k=1, hours=10,
                         strategy="source:structure:woodland_mansion")
        self.assertTrue(res["feasible"], res["message"])
        self.assertEqual([l["id"] for l in res["lines"]], [IRON])
        self.assertEqual(res["mode"], "normal")
        self.assertAlmostEqual(self.recompute(res), 10.0, delta=0.01)
        # die anderen Quellen bleiben, wie sie sind
        self.assertNotIn(IRON_MINE, res["values"])
        self.assertNotIn(IRON_TRADE, res["values"])

    def test_single_value_strategy(self):
        vid = "loot:ancient_city:p0:octant:weight"
        res = self.solve(item="simplebuilding:octant", row="__together__", stat="p90", k=3, hours=4, strategy="value:" + vid)
        self.assertEqual([l["id"] for l in res["lines"]], [vid])
        self.assertIsInstance(res["lines"][0]["new"], int)
        # ganze Zahl: der Nachbar, der näher liegt
        new = res["lines"][0]["new"]
        got = self.recompute(res)
        for other in (new - 1, new + 1):
            t = model.metric(self.service.ctx({vid: other}), "simplebuilding:octant", "__together__", "targeted", "p90", 3)
            self.assertGreaterEqual(abs(t - 4), abs(got - 4) - 1e-9)

    def test_clamping_at_100_percent(self):
        # der Händler hat das Angebot schon zu 50 %; 5 h sind nur mit mehr als 100 % möglich
        res = self.solve(item="simplebuilding:iron_core", row="trade:simplebuilding:wandering_trader/emerald_iron_cores",
                         stat="mean", k=1, hours=5)
        self.assertFalse(res["feasible"])
        self.assertIn("Nicht ganz erreichbar", res["message"])
        line = next(l for l in res["lines"] if l["id"] == IRON_TRADE)
        self.assertEqual(line["new"], 1.0)
        self.assertEqual(line["clamped"], "max")
        self.assertAlmostEqual(res["achieved"], self.recompute(res), places=9)
        # mehrere Quellen: eine steht an 100 %, die anderen übernehmen den Rest - Ziel trotzdem genau
        res = self.solve(item="simplebuilding:iron_core", row="__normal__", stat="median", k=2, hours=10)
        self.assertTrue(res["feasible"], res["message"])
        trade = next(l for l in res["lines"] if l["id"] == IRON_TRADE)
        self.assertEqual(trade["new"], 1.0)
        self.assertEqual(trade["clamped"], "max")
        self.assertIn("Grenze", res["message"])
        self.assertAlmostEqual(self.recompute(res), 10.0, delta=0.01)

    def test_clamping_at_zero_percent_and_slower_targets(self):
        # langsamer als je: die Chance sinkt Richtung 0, bleibt aber > 0 (0 hieße "Quelle weg")
        res = self.solve(item="simplebuilding:enderite_core", row="structure:end_city", stat="mean", k=1, hours=5000)
        self.assertTrue(res["feasible"], res["message"])
        line = res["lines"][0]
        self.assertGreater(line["new"], 0.0)
        self.assertLess(line["new"], line["old"])
        self.assertAlmostEqual(self.recompute(res), 5000, delta=5)
        # ein int-Gewicht fällt nie auf 0
        res = self.solve(item="simplebuilding:diamond_pebble", row="structure:ancient_city", stat="mean", k=1, hours=1e5)
        self.assertGreaterEqual(res["lines"][0]["new"], 1)

    def test_trader_only_book_uses_its_pool_weight(self):
        item = "book:simplebuilding:range:2"
        row = "trade:simplebuilding:librarian/5/emerald_master_book"
        res = self.solve(item=item, row=row, stat="mean", k=1, hours=10)
        self.assertTrue(res["feasible"], res["message"])
        ids = [l["id"] for l in res["lines"]]
        self.assertEqual(ids, ["trade:simplebuilding:librarian/5/emerald_master_book:m0.range.2.weight"])
        # das Gewicht teilt sich den Pool mit anderen Büchern - die stehen als "wirkt auch auf" daneben
        self.assertIn("book:simplebuilding:range:3", res["lines"][0]["alsoAffects"])
        self.assertAlmostEqual(self.recompute(res), res["achieved"], places=9)
        self.assertLess(abs(res["achieved"] - 10) / 10, 0.02)

    def test_chest_only_item(self):
        item = "simplebuilding:quiver"
        kinds = {s["kind"] for s in self.service.snapshot["sources"][item]}
        self.assertEqual(kinds, {"structure"})
        cur = model.metric(self.service.ctx(), item, "__normal__", "normal", "mean", 1)
        res = self.solve(item=item, row="__normal__", stat="mean", k=1, hours=cur * 2)
        self.assertTrue(res["feasible"], res["message"])
        self.assertTrue(res["lines"])
        self.assertTrue(all(l["id"].startswith("loot:") or l["id"].startswith("const:") for l in res["lines"]))
        # ganze Gewichte: neu gerechnet genau das gemeldete Ergebnis, nah am Ziel
        self.assertAlmostEqual(self.recompute(res), res["achieved"], places=9)
        self.assertLess(abs(res["achieved"] - cur * 2) / (cur * 2), 0.02)

    def test_mob_source_changes_only_the_calculator_assumption(self):
        res = self.solve(item="simplebuilding:blaze_head", row="__normal__", stat="mean", k=1, hours=2)
        self.assertTrue(res["feasible"])
        self.assertEqual([l["id"] for l in res["lines"]], ["param:mob.charged_creeper.blaze"])
        self.assertEqual(res["lines"][0]["apply"], "tool")
        self.assertFalse(res["lines"][0]["applyNow"])

    def test_planned_source_chance(self):
        item = "simplebuilding:enderite_core"
        vid = f"source:{item}:qsolve"
        spec = {"kind": "mob", "label": "Shulker", "chance": 0.01, "countMin": 1, "countMax": 1, "rate": 20.0}
        res = self.service.solve_time({"item": item, "row": vid, "stat": "mean", "k": 1, "hours": 2, "overrides": {vid: spec}})
        self.assertTrue(res["feasible"], res["message"])
        new = res["values"][vid]
        self.assertEqual(new["label"], "Shulker")
        self.assertAlmostEqual(new["chance"], 1 / 40, delta=1e-4)

    def test_recipe_rows_are_not_editable(self):
        res = self.solve(item="simplebuilding:octant", row="recipe:simplebuilding:octant", stat="mean", k=1, hours=3)
        self.assertFalse(res["feasible"])
        self.assertEqual(res["lines"], [])
        self.assertIn("Zutat", res["message"])

    def test_invalid_requests(self):
        from sbdev.store import StoreError
        for bad in ({"item": "simplebuilding:iron_core", "hours": -1}, {"item": "simplebuilding:iron_core", "hours": "x"},
                    {"item": "simplebuilding:iron_core", "hours": 3, "stat": "max"},
                    {"item": "simplebuilding:iron_core", "hours": 3, "strategy": "alles"}, {"hours": 3}):
            with self.assertRaises(StoreError, msg=bad):
                self.service.solve_time(bad)

    def test_drivers_of_a_pool_with_one_entry_are_ignored(self):
        ctx = self.service.ctx()
        own, shared = solver.row_drivers(ctx, "simplebuilding:enderite_core", "structure:end_city")
        self.assertIn(ENDERITE, [d["id"] for d in own])
        self.assertTrue(all(d["type"] in ("prob", "int", "float") for d in own + shared))


class NothingWrittenWithoutConfirmTests(unittest.TestCase):
    """Lösen schreibt weder Ablage noch Mod-Dateien; erst preview -> save mit den gelieferten Werten."""

    @classmethod
    def setUpClass(cls):
        cls.repo = helpers.shared_full_repo()

    def setUp(self):
        self.s = Service(self.repo, helpers.temp_dir("bz-store-"))
        self.before = {rel: (self.repo / rel).read_bytes() for rel in (LOOT, LOOT_1211)}

    def tearDown(self):
        for rel, data in self.before.items():
            (self.repo / rel).write_bytes(data)

    def test_solve_writes_nothing_then_save_writes_the_solved_value(self):
        res = self.s.solve_time({"item": "simplebuilding:enderite_core", "row": "structure:end_city", "stat": "mean",
                                 "k": 1, "hours": 20})
        self.assertTrue(res["feasible"])
        # nichts geschrieben: Ablage leer, keine Version, Dateien unverändert
        self.assertEqual(self.s.store.state()["version"], 0)
        self.assertEqual(self.s.store.state()["entries"], {})
        self.assertEqual(len(self.s.store.history()), 1)
        for rel, data in self.before.items():
            self.assertEqual((self.repo / rel).read_bytes(), data, rel)
        # die Oberfläche übernimmt die Werte als Entwürfe -> preview zeigt alt -> neu, schreibt aber nichts
        changes = [{"id": l["id"], "value": res["values"][l["id"]], "expected": l["old"]} for l in res["lines"]]
        pv = self.s.preview({"baseVersion": 0, "changes": changes})
        self.assertFalse(pv["errors"])
        self.assertEqual(pv["summary"][0]["old"], 0.00175)
        self.assertEqual(self.s.store.state()["version"], 0)
        for rel, data in self.before.items():
            self.assertEqual((self.repo / rel).read_bytes(), data, rel)
        # erst die Bestätigung (save) schreibt - in beide Linien
        out = self.s.save({"baseVersion": 0, "changes": changes, "message": "Zielzeit 20 h"})
        self.assertEqual(out["version"], 1)
        literal = f"{res['values'][ENDERITE]:g}"
        for rel in (LOOT, LOOT_1211):
            self.assertIn(f"ENDERITE_CORE_CHANCE = {literal}", (self.repo / rel).read_text(encoding="utf-8"), rel)
        # neu eingelesen ergibt der gespeicherte Stand die Zielzeit
        t = model.metric(self.s.ctx(), "simplebuilding:enderite_core", "structure:end_city", "targeted", "mean", 1)
        self.assertAlmostEqual(t, 20.0, delta=0.02)
        self.s.rollback({"target": 0, "baseVersion": 1})
        for rel, data in self.before.items():
            self.assertEqual((self.repo / rel).read_bytes(), data, rel)


if __name__ == "__main__":
    unittest.main()
