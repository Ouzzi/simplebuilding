"""Bearbeiten -> Vorschau -> Speichern -> Rueckgaengig, Pruefung ungueltiger Werte und das Schreiben in Handelsdateien."""

import json
import unittest

import helpers
from sbdev.service import Service
from sbdev.store import StoreError

ENDERITE = "const:ModLootTableModifications.ENDERITE_CORE_CHANCE"
AIRJUMP = "config:airJumpCooldownTicks"
IRON_PRICE = "trade:simplebuilding:wandering_trader/emerald_iron_cores:price"
OCTANT_CHANCE = "trade:simplebuilding:wandering_trader/emerald_octant:offerChance"


class RoundTripTests(unittest.TestCase):
    """Auf dem echten Repo mit schreibgeschütztem Service (read_only): nur die Ablage, keine Mod-Datei."""

    def setUp(self):
        self.s = helpers.fresh_store(helpers.shared_service())

    def save(self, changes, base=None, **kw):
        base = self.s.store.state()["version"] if base is None else base
        return self.s.save(dict({"baseVersion": base, "changes": changes, "message": "t"}, **kw))

    def test_edit_save_reload_rollback_round_trip(self):
        self.assertEqual(self.s.store.state()["version"], 0)
        r1 = self.save([{"id": ENDERITE, "value": 0.0025, "expected": 0.00175}, {"id": AIRJUMP, "value": "120"}])
        self.assertEqual(r1["version"], 1)
        r2 = self.save([{"id": ENDERITE, "value": "0,003"}])
        self.assertEqual(r2["version"], 2)
        # neu laden (neuer Service-Store auf demselben Ordner) - Werte sind da
        from sbdev.store import Store
        reloaded = Store(self.s.store.root).state()
        self.assertEqual(reloaded["entries"][ENDERITE]["value"], 0.003)
        self.assertEqual(reloaded["entries"][ENDERITE]["origin"], 0.00175)
        self.assertEqual(reloaded["entries"][AIRJUMP]["value"], 120)
        # Rollback auf v1: neue Version v3 mit dem Inhalt von v1
        pv = self.s.rollback_preview({"target": 1})
        self.assertEqual(pv["nextVersion"], 3)
        self.assertEqual([(x["id"], x["old"], x["new"]) for x in pv["summary"]], [(ENDERITE, 0.003, 0.0025)])
        r3 = self.s.rollback({"target": 1, "baseVersion": 2})
        self.assertEqual(r3["version"], 3)
        self.assertEqual(self.s.store.state()["entries"], self.s.store.version(1)["entries"])
        # Rollback auf v0: alle Plaene weg, v1..v3 bleiben
        self.s.rollback({"target": 0, "baseVersion": 3})
        state = self.s.store.state()
        self.assertEqual(state["version"], 4)
        self.assertEqual(state["entries"], {})
        self.assertEqual([h["version"] for h in self.s.store.history()], [0, 1, 2, 3, 4])
        self.assertEqual(self.s.store.version(2)["entries"][ENDERITE]["value"], 0.003)
        with self.assertRaises(StoreError):
            self.s.rollback({"target": 0, "baseVersion": 4})  # nichts zu tun

    def test_invalid_values_are_rejected_with_clear_messages_and_nothing_is_written(self):
        cases = [
            ({"id": AIRJUMP, "value": "abc"}, "keine Zahl"),
            ({"id": AIRJUMP, "value": 1.5}, "ganze Zahl"),
            ({"id": AIRJUMP, "value": -1}, "kleiner als das Minimum"),
            ({"id": ENDERITE, "value": 1.5}, "größer als das Maximum"),
            ({"id": ENDERITE, "value": "NaN"}, "keine Zahl"),
            ({"id": ENDERITE, "value": float("inf")}, "keine endliche Zahl"),
            ({"id": "config:tools.buildingWandHungerCost", "value": "vielleicht"}, "an/aus"),
            ({"id": "gibt:es:nicht", "value": 1}, "Unbekannter Wert"),
            ({"id": "item:simplebuilding:iron_chisel:durability", "value": 300}, "bitte dort ändern"),
            ({"id": "const:EchoCompassItem.CRACKED_CHARGE_TICKS", "value": 100}, "bitte dort ändern"),
            ({"id": "const:ModItems.DURABILITY_IRON", "value": 257}, "ganze Zahl"),
            ({"id": "param:eras", "value": [{"name": "X", "hours": -3}]}, "Stunden"),
            ({"id": "param:rate.end_city.chest", "value": "viele"}, "keine Zahl"),
            ({"id": "source:simplebuilding:iron_core:x1", "value": {"kind": "mob", "chance": 2, "countMin": 1, "countMax": 1, "rate": 1}}, "Chance"),
        ]
        for change, text in cases:
            with self.subTest(change=change):
                pv = self.s.preview({"baseVersion": 0, "changes": [change]})
                self.assertEqual(len(pv["errors"]), 1, pv)
                self.assertIn(text, pv["errors"][0]["message"])
                with self.assertRaises(StoreError) as ctx:
                    self.save([change])
                self.assertEqual(ctx.exception.status, 400)
                self.assertEqual(self.s.store.state()["version"], 0)
        with self.assertRaises(StoreError):
            self.s.save({"baseVersion": 0, "changes": "kein Array"})

    def test_stale_expected_value_is_a_conflict(self):
        self.save([{"id": ENDERITE, "value": 0.002}])
        with self.assertRaises(StoreError) as ctx:
            self.save([{"id": ENDERITE, "value": 0.004, "expected": 0.00175}])  # UI kannte noch den Mod-Wert
        self.assertEqual(ctx.exception.status, 409)
        self.assertEqual(self.s.store.state()["entries"][ENDERITE]["value"], 0.002)

    def test_stale_base_version_is_a_conflict(self):
        self.save([{"id": ENDERITE, "value": 0.002}])
        with self.assertRaises(StoreError) as ctx:
            self.save([{"id": AIRJUMP, "value": 50}], base=0)
        self.assertEqual(ctx.exception.status, 409)

    def test_reset_removes_the_plan_and_unchanged_values_are_skipped(self):
        self.save([{"id": ENDERITE, "value": 0.002}])
        pv = self.s.preview({"baseVersion": 1, "changes": [{"id": AIRJUMP, "value": 100}]})
        self.assertEqual(pv["skipped"], [AIRJUMP])  # 100 ist schon der Wert
        self.save([{"id": ENDERITE, "reset": True}])
        self.assertEqual(self.s.store.state()["entries"], {})

    def test_orphaned_values_are_kept_until_removed_on_purpose(self):
        self.s.store.commit(0, [{"id": "loot:umbenannt:p0:weg:weight", "new": 7, "mod": 5}], "alt")
        state = self.s.state_payload()
        self.assertEqual(state["store"]["status"]["loot:umbenannt:p0:weg:weight"]["status"], "orphan")
        # ein anderer Speichervorgang laesst ihn in Ruhe
        self.save([{"id": ENDERITE, "value": 0.002}])
        self.assertIn("loot:umbenannt:p0:weg:weight", self.s.store.state()["entries"])
        # bewusst entfernen
        self.save([{"id": "loot:umbenannt:p0:weg:weight", "reset": True}])
        self.assertNotIn("loot:umbenannt:p0:weg:weight", self.s.store.state()["entries"])

    def test_planned_sources_and_params_round_trip(self):
        spec = {"kind": "structure", "structure": "ancient_city", "chance": 0.01, "countMin": 1, "countMax": 2, "label": "Test"}
        self.save([{"id": "source:simplebuilding:enderite_core:qa", "value": spec},
                   {"id": "sourceoff:simplebuilding:enderite_core:structure:end_city", "value": True},
                   {"id": "param:rate.end_city.chest", "value": 20},
                   {"id": "param:eras", "value": [{"id": "x", "name": "Test", "hours": 10}]}])
        entries = self.s.store.state()["entries"]
        self.assertEqual(entries["source:simplebuilding:enderite_core:qa"]["value"]["container"], "chest")
        report = self.s.calc({"item": "simplebuilding:enderite_core"})
        self.assertTrue(any(r["kind"] == "custom" for r in report["rows"]))
        self.assertTrue(next(r for r in report["rows"] if r["key"] == "structure:end_city")["disabled"])
        phase2 = self.s.phase2()
        self.assertIn("source", phase2["groups"])
        self.assertNotIn("param", phase2["groups"])  # Annahmen wirken nie in der Mod
        # Quelle entfernen
        self.save([{"id": "source:simplebuilding:enderite_core:qa", "value": None}])
        self.assertNotIn("source:simplebuilding:enderite_core:qa", self.s.store.state()["entries"])


class ApplyToModTests(unittest.TestCase):
    """Handelswerte: auf einer Kopie des Repos pruefen, dass genau ein Token wechselt."""

    @classmethod
    def setUpClass(cls):
        cls.repo = helpers.temp_repo()

    def setUp(self):
        self.s = Service(self.repo, helpers.temp_dir("bz-store-"))
        self.file = self.repo / "src/main/resources/data/simplebuilding/villager_trade/wandering_trader/emerald_iron_cores.json"

    def test_trade_price_is_written_surgically_and_rolled_back(self):
        before = self.file.read_bytes()
        res = self.s.save({"baseVersion": 0, "changes": [{"id": IRON_PRICE, "value": 40, "expected": 32}], "message": "teurer"})
        self.assertEqual(res["applied"], 1)
        after = self.file.read_bytes()
        self.assertEqual(after, before.replace(b'"count": 32', b'"count": 40'))
        self.assertEqual(self.s.snapshot["values"][IRON_PRICE]["value"], 40)  # neu eingelesen
        self.assertEqual(self.s.statuses(self.s.store.state()["entries"])[IRON_PRICE]["status"], "applied")
        self.assertEqual(len(self.s.store.applied_for(1)), 1)
        # Rueckgaengig schreibt den Ursprungswert zurueck
        self.s.rollback({"target": 0, "baseVersion": 1})
        self.assertEqual(self.file.read_bytes(), before)
        self.assertEqual(self.s.snapshot["values"][IRON_PRICE]["value"], 32)

    def test_offer_chance_is_inserted_when_missing(self):
        path = self.repo / "src/main/resources/data/simplebuilding/villager_trade/wandering_trader/emerald_octant.json"
        self.s.save({"baseVersion": 0, "changes": [{"id": OCTANT_CHANCE, "value": 0.5}]})
        data = json.loads(path.read_text(encoding="utf-8"))
        self.assertEqual(data["merchant_predicate"], {"condition": "minecraft:random_chance", "chance": 0.5})
        self.assertEqual(self.s.snapshot["values"][OCTANT_CHANCE]["value"], 0.5)

    def test_file_changed_since_reading_blocks_the_save(self):
        text = self.file.read_text(encoding="utf-8")
        self.file.write_text(text.replace('"xp": 10', '"xp": 11'), encoding="utf-8")  # fremde Aenderung, anderer Wert
        other = "trade:simplebuilding:wandering_trader/emerald_iron_cores:xp"
        with self.assertRaises(StoreError) as ctx:
            self.s.save({"baseVersion": 0, "changes": [{"id": other, "value": 20}]})
        self.assertEqual(ctx.exception.status, 409)
        self.assertIn("seit dem Einlesen", ctx.exception.details[0]["message"])
        self.assertEqual(self.s.store.state()["version"], 0)  # nichts gespeichert
        self.assertIn('"xp": 11', self.file.read_text(encoding="utf-8"))  # fremde Aenderung unangetastet
        self.file.write_text(text, encoding="utf-8")

    def test_plan_without_applying_and_apply_later(self):
        self.s.save({"baseVersion": 0, "changes": [{"id": IRON_PRICE, "value": 36}], "applyToMod": False})
        self.assertIn('"count": 32', self.file.read_text(encoding="utf-8"))
        self.assertEqual(self.s.statuses(self.s.store.state()["entries"])[IRON_PRICE]["status"], "planned")
        self.assertEqual([p["id"] for p in self.s.pending_apply()], [IRON_PRICE])
        self.s.apply_planned({})
        self.assertIn('"count": 36', self.file.read_text(encoding="utf-8"))
        self.assertEqual(self.s.pending_apply(), [])
        # aufraeumen fuer die anderen Tests
        self.s.save({"baseVersion": 1, "changes": [{"id": IRON_PRICE, "reset": True}]})
        self.assertIn('"count": 32', self.file.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
