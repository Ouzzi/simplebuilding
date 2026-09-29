"""
Gespeicherte Werte wirken in der Mod: Java-Literale schreiben (alle Linien), zurücksetzen bis Byte für
Byte, checkBalance, Datagen-Auftrag. Alles auf Kopien des Repos - das echte Repo wird nie geschrieben.
"""

import json
import os
import shutil
import stat
import subprocess
import sys
import time
import unittest
from pathlib import Path

import helpers
from sbdev import apply as applier
from sbdev import check as balance_checker
from sbdev import javaedit, jobs
from sbdev.service import Service
from sbdev.store import StoreError

ENDERITE = "const:ModLootTableModifications.ENDERITE_CORE_CHANCE"
LOOT = "common/src/shared/java/com/simplebuilding/loot/ModLootTableModifications.java"
LOOT_1211 = "mc1_21_11/shared/java/com/simplebuilding/loot/ModLootTableModifications.java"
CONFIG = "common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java"
CONFIG_1211 = "mc1_21_11/shared/java/com/simplebuilding/config/SimplebuildingConfig.java"
ENCH = "common/src/shared/java/com/simplebuilding/enchantment/ModEnchantments.java"
ITEMS = "common/src/shared/java/com/simplebuilding/items/ModItems.java"
INJECT_END_CITY = "src/main/generated/data/simplebuilding/loot_table/inject/chests/end_city_treasure.json"


def snapshot_files(repo: Path, rels) -> dict:
    return {rel: (repo / rel).read_bytes() for rel in rels}


class JavaEditTests(unittest.TestCase):
    def test_literal_keeps_the_authors_style(self):
        cases = [
            (0.002, "0.00175f", "float", "0.002f"),
            (0.2, "0.10f", "float", "0.20f"),       # Nullen am Ende bleiben
            (3.0, "2.0F", "float", "3.0F"),
            (7, "5", "int", "7"),
            (-2.8, "-3.0f", "float", "-2.8f"),
            (22.5, "20", "float", "22.5f"),          # int-Literal in einem float-Ausdruck
            (24.0, "20", "float", "24"),
            (1.5, "1", "double", "1.5"),
            (0x00FF00, "0xFFAA00", "int", "0x00FF00"),
            (2000, "1_000", "int", "2_000"),
            (True, "false", "boolean", "true"),
            ('minecraft:the_end, "x"', '""', "String", '"minecraft:the_end, \\"x\\""'),
        ]
        for new, old, jtype, want in cases:
            with self.subTest(old=old, new=new):
                self.assertEqual(javaedit.format_literal(new, old, jtype), want)
                self.assertEqual(javaedit.token_value(want), new)

    def test_transform_sites(self):
        site = {"transform": {"op": "*", "k": 4, "operand": "left"}, "jtype": "int"}
        self.assertEqual(javaedit.forward(site, 64), 256)
        self.assertEqual(javaedit.inverse(site, 260), 65)
        with self.assertRaises(javaedit.JavaEditError):
            javaedit.inverse(site, 257)
        minus = {"transform": {"op": "-", "k": 0.4, "operand": "left"}, "jtype": "float"}
        self.assertEqual(javaedit.forward(minus, -3.0), -3.4)
        self.assertEqual(javaedit.inverse(minus, -3.2), -2.8)
        ratio = {"transform": {"op": "/", "k": 1.0, "operand": "right"}, "jtype": "float"}
        self.assertEqual(javaedit.inverse(ratio, 0.005), 200.0)

    def test_crlf_file_keeps_its_line_endings_and_bytes(self):
        root = helpers.temp_dir()
        path = root / "A.java"
        path.write_bytes(b"class A {\r\n    // Kommentar 0.5f\r\n    static final float X = 0.5f;\r\n    static final int Y = 3;\r\n}\r\n")
        raw, text = javaedit.read(path)
        a = text.index("0.5f;")
        b = text.index("3;")
        site_x = {"file": "A.java", "span": [a, a + 4], "token": "0.5f", "jtype": "float", "line": 3}
        site_y = {"file": "A.java", "span": [b, b + 1], "token": "3", "jtype": "int", "line": 4}
        javaedit.apply_file(root, "A.java", [(site_x, 0.5, 0.75), (site_y, 3, 12)])
        self.assertEqual(path.read_bytes(),
                         b"class A {\r\n    // Kommentar 0.5f\r\n    static final float X = 0.75f;\r\n    static final int Y = 12;\r\n}\r\n")
        # die Stelle hat sich geändert: eine zweite Speicherung mit dem alten Stand wird abgelehnt
        with self.assertRaises(javaedit.JavaEditError):
            javaedit.apply_file(root, "A.java", [(site_x, 0.5, 0.6)])

    def test_prefer_restores_an_unusual_literal(self):
        site = {"jtype": "float"}
        self.assertEqual(javaedit.literal_for(site, "0.002f", 0.0015, prefer="1.5e-3f"), "1.5e-3f")
        self.assertEqual(javaedit.literal_for(site, "0.002f", 0.0025, prefer="1.5e-3f"), "0.0025f")


class WriteJavaTests(unittest.TestCase):
    """Beute, Config, Konstanten, Verzauberungen: Speichern schreibt 26.2 und 1.21.11; Rückgängig stellt alles her."""

    @classmethod
    def setUpClass(cls):
        cls.repo = helpers.shared_full_repo()

    def setUp(self):
        self.s = Service(self.repo, helpers.temp_dir("bz-store-"))
        self.watched = [LOOT, LOOT_1211, CONFIG, CONFIG_1211, ENCH, ITEMS, INJECT_END_CITY,
                        "mc1_21_11/shared/java/com/simplebuilding/items/ModItems.java",
                        "mc1_21_11/shared/java/com/simplebuilding/enchantment/ModEnchantments.java",
                        "mc1_21_11/fabric/src/main/generated/data/simplebuilding/loot_table/inject/chests/end_city_treasure.json",
                        "mc26_3/generated/data/simplebuilding/loot_table/inject/chests/end_city_treasure.json",
                        "common/src/shared/java/com/simplebuilding/config/ServerTuningConfig.java",
                        "mc1_21_11/shared/java/com/simplebuilding/config/ServerTuningConfig.java"]
        self.before = snapshot_files(self.repo, self.watched)

    def tearDown(self):
        for rel, data in self.before.items():
            (self.repo / rel).write_bytes(data)

    def save(self, changes, **kw):
        base = self.s.store.state()["version"]
        return self.s.save(dict({"baseVersion": base, "changes": changes, "message": "t"}, **kw))

    def test_core_chance_round_trip_both_lines_check_and_rollback(self):
        res = self.save([{"id": ENDERITE, "value": 0.01, "expected": 0.00175}])
        self.assertEqual(res["applied"], 1)
        self.assertTrue(res["datagen"])
        for rel in (LOOT, LOOT_1211):
            text = (self.repo / rel).read_text(encoding="utf-8")
            self.assertIn("ENDERITE_CORE_CHANCE = 0.01f;", text, rel)
        # genau ein Token je Datei
        for rel in (LOOT, LOOT_1211):
            self.assertEqual((self.repo / rel).read_bytes(), self.before[rel].replace(b"0.00175f;", b"0.01f;", 1))
        self.assertEqual(self.s.snapshot["values"][ENDERITE]["value"], 0.01)
        entry = self.s.store.state()["entries"][ENDERITE]
        self.assertTrue(entry["applied"])
        self.assertEqual(entry["originText"], ["0.00175f", "0.00175f"])
        # checkBalance: Ablage = Code, aber die Inject-Tabelle ist noch alt -> Datagen fehlt
        result = self.s.balance_check()
        self.assertFalse(result["ok"])
        self.assertTrue(any(e["kind"] == "datagen" and "end_city_treasure" in e["message"] for e in result["errors"]))
        # "Datagen": die erzeugte Tabelle bekommt den neuen Wert -> ok
        inject = self.repo / INJECT_END_CITY
        inject.write_bytes(self.before[INJECT_END_CITY].replace(b'"chance": 0.00175', b'"chance": 0.01'))
        for other in ("mc1_21_11/fabric/src/main/generated/", "mc26_3/generated/"):
            path = self.repo / (other + INJECT_END_CITY.split("src/main/generated/", 1)[1])
            if path.exists():
                raw = path.read_bytes()
                path.write_bytes(raw.replace(b"0.00175", b"0.01"))
        self.assertTrue(self.s.balance_check()["ok"], self.s.balance_check()["errors"][:3])
        # jemand ändert die Zahl im Code an der Zentrale vorbei -> checkBalance meldet die Abweichung
        text = (self.repo / LOOT).read_text(encoding="utf-8")
        (self.repo / LOOT).write_text(text.replace("= 0.01f;", "= 0.02f;"), encoding="utf-8")
        self.s.reload()
        self.assertTrue(any(e["kind"] == "drift" for e in self.s.balance_check()["errors"]))
        (self.repo / LOOT).write_text(text, encoding="utf-8")
        self.s.reload()
        # Rückgängig auf v0: Original-Literale zurück, Eintrag weg
        self.s.rollback({"target": 0, "baseVersion": 1})
        for rel in (LOOT, LOOT_1211):
            self.assertEqual((self.repo / rel).read_bytes(), self.before[rel], rel)
        self.assertEqual(self.s.store.state()["entries"], {})

    def test_config_defaults_int_bool_and_string(self):
        self.save([{"id": "config:airJumpCooldownTicks", "value": 140},
                   {"id": "config:enableDoubleJump", "value": False},
                   {"id": "config:server.dimensionLocks.flypadBlockedDimensions", "value": "minecraft:the_end"}])
        for rel in (CONFIG, CONFIG_1211):
            text = (self.repo / rel).read_text(encoding="utf-8")
            self.assertIn("public int airJumpCooldownTicks = 140;", text)
            self.assertIn("public boolean enableDoubleJump = false;", text)
        server = self.repo / "common/src/shared/java/com/simplebuilding/config/ServerTuningConfig.java"
        self.assertIn('flypadBlockedDimensions = "minecraft:the_end";', server.read_text(encoding="utf-8"))
        self.s.rollback({"target": 0, "baseVersion": 1})
        for rel in (CONFIG, CONFIG_1211):
            self.assertEqual((self.repo / rel).read_bytes(), self.before[rel])
        self.assertNotIn('"minecraft:the_end"', server.read_text(encoding="utf-8"))

    def test_expression_constant_and_item_alias(self):
        # DURABILITY_IRON = 64*4: 260 schreibt 65*4 (in beiden Linien)
        self.save([{"id": "const:ModItems.DURABILITY_IRON", "value": 260}])
        text = (self.repo / ITEMS).read_text(encoding="utf-8")
        self.assertIn("DURABILITY_IRON = 65*4;", text)
        self.assertIn("DURABILITY_IRON = 65*4;", (self.repo / "mc1_21_11/shared/java/com/simplebuilding/items/ModItems.java")
                      .read_text(encoding="utf-8"))
        # der Eisen-Meißel zeigt auf die Konstante; sein Export ist bis zum Datagen alt
        chisel = self.s.snapshot["values"]["item:simplebuilding:iron_chisel:durability"]
        self.assertEqual(chisel["alias"], {"id": "const:ModItems.DURABILITY_IRON", "factor": 1})
        self.assertTrue(chisel.get("generatedStale"))
        self.s.rollback({"target": 0, "baseVersion": 1})
        self.assertEqual((self.repo / ITEMS).read_bytes(), self.before[ITEMS])

    def test_enchantment_weight_in_java_and_generated_check(self):
        vid = "enchant:simplebuilding:range:weight"
        self.assertEqual(self.s.snapshot["values"][vid]["value"], 1)
        self.save([{"id": vid, "value": 2}])
        self.assertEqual(self.s.snapshot["values"][vid]["value"], 2)
        errors = self.s.balance_check()["errors"]
        self.assertTrue(any(e["id"] == vid and e["kind"] == "datagen" for e in errors), errors[:3])
        self.s.rollback({"target": 0, "baseVersion": 1})
        self.assertEqual((self.repo / ENCH).read_bytes(), self.before[ENCH])
        self.assertTrue(self.s.balance_check()["ok"])

    def test_a_changed_twin_blocks_the_whole_save(self):
        path = self.repo / LOOT_1211
        text = path.read_text(encoding="utf-8")
        path.write_text(text.replace("ENDERITE_CORE_CHANCE = 0.00175f", "ENDERITE_CORE_CHANCE = 0.003f"), encoding="utf-8")
        try:
            with self.assertRaises(StoreError) as ctx:
                self.save([{"id": ENDERITE, "value": 0.01}])
            self.assertEqual(ctx.exception.status, 409)
            self.assertIn("1.21.11", ctx.exception.details[0]["message"])
            self.assertEqual((self.repo / LOOT).read_bytes(), self.before[LOOT])  # 26.2 unangetastet
            self.assertEqual(self.s.store.state()["version"], 0)
        finally:
            path.write_bytes(self.before[LOOT_1211])

    def test_failed_write_restores_every_file(self):
        target = self.repo / LOOT_1211
        os.chmod(target, stat.S_IREAD)
        try:
            record = self.s.record_for(ENDERITE)
            with self.assertRaises(Exception):
                applier.write_all(self.repo, [(record, 0.01)])
        finally:
            os.chmod(target, stat.S_IWRITE | stat.S_IREAD)
        self.assertEqual((self.repo / LOOT).read_bytes(), self.before[LOOT])
        self.assertEqual(target.read_bytes(), self.before[LOOT_1211])

    def test_plan_only_then_apply_later(self):
        self.save([{"id": ENDERITE, "value": 0.004}], applyToMod=False)
        self.assertEqual((self.repo / LOOT).read_bytes(), self.before[LOOT])
        self.assertEqual([p["id"] for p in self.s.pending_apply()], [ENDERITE])
        res = self.s.apply_planned({})
        self.assertEqual(res["applied"], 1)
        self.assertIn(b"0.004f;", (self.repo / LOOT).read_bytes())
        self.assertIn(b"0.004f;", (self.repo / LOOT_1211).read_bytes())
        self.assertEqual(self.s.pending_apply(), [])
        self.save([{"id": ENDERITE, "reset": True}])
        self.assertEqual((self.repo / LOOT).read_bytes(), self.before[LOOT])


class PotionPadTests(unittest.TestCase):
    """Die Regeln der Trank-Pads (Lauf AA): sobald PotionPadRules.java da ist, zeigt und schreibt die Zentrale sie."""

    def test_rules_table_is_read_and_written(self):
        repo = helpers.shared_full_repo()
        rel = "common/src/shared/java/com/simplebuilding/tweaks/PotionPadRules.java"
        shutil.copy(helpers.HERE / "fixtures" / "PotionPadRules.java.txt", repo / rel)
        self.addCleanup(lambda: (repo / rel).unlink())
        s = Service(repo, helpers.temp_dir("bz-store-"))
        table = s.snapshot["potionPads"]
        self.assertEqual(table["file"], rel)
        effects = [r["effect"] for r in table["rows"]]
        self.assertIn("default", effects)
        self.assertIn("minecraft:regeneration", effects)
        regen = s.snapshot["values"]["potionpad:minecraft:regeneration:cooldownMultiplier"]
        self.assertEqual((regen["value"], regen["apply"], regen["category"]), (1.5, "mod", "potionpad"))
        s.save({"baseVersion": 0, "changes": [{"id": regen["id"], "value": 2.0}], "message": "t"})
        self.assertIn('Map.entry("minecraft:regeneration", new Rule(1, 0, 1.0, 2.0, 1200))',
                      (repo / rel).read_text(encoding="utf-8"))

    def test_without_the_file_there_is_no_table(self):
        snap = helpers.shared_service().snapshot
        if (helpers.REPO / "common/src/shared/java/com/simplebuilding/tweaks/PotionPadRules.java").exists():
            self.skipTest("Lauf AA ist gemergt")
        self.assertEqual(snap["potionPads"]["rows"], [])


class DatagenJobTests(unittest.TestCase):
    def setUp(self):
        self.repo = helpers.temp_repo()
        self.s = Service(self.repo, helpers.temp_dir("bz-store-"))

    def test_job_runs_steps_then_rereads_and_checks(self):
        self.s.datagen_steps = [
            {"key": "a", "label": "Schritt A", "cmd": [sys.executable, "-c", "print('datagen a')"]},
            {"key": "b", "label": "Schritt B", "cmd": [sys.executable, "-c", "print('datagen b')"]},
        ]
        first = self.s.start_datagen({})
        self.assertEqual(first["status"], "running")
        with self.assertRaises(StoreError):
            if self.s.job.status == "running":
                self.s.start_datagen({})
            else:
                raise StoreError(409, "schon fertig")
        self.s.job.thread.join(60)
        state = self.s.datagen_status()
        self.assertEqual(state["status"], "ok")
        self.assertEqual([st["status"] for st in state["steps"]], ["ok", "ok"])
        self.assertTrue(any("datagen b" in line for line in state["log"]))
        self.assertIn("check", state["result"])
        self.assertIn("diff", state["result"])

    def test_a_failing_step_stops_the_job(self):
        self.s.datagen_steps = [
            {"key": "a", "label": "kaputt", "cmd": [sys.executable, "-c", "import sys; sys.exit(3)"]},
            {"key": "b", "label": "danach", "cmd": [sys.executable, "-c", "print(1)"]},
        ]
        self.s.start_datagen({})
        self.s.job.thread.join(60)
        state = self.s.datagen_status()
        self.assertEqual(state["status"], "failed")
        self.assertEqual([st["status"] for st in state["steps"]], ["failed", "skipped"])

    def test_running_dev_game_needs_force(self):
        original = jobs.running_dev_games
        jobs.running_dev_games = lambda repo=None: ["java ... net.fabricmc.devlaunchinjector.Main runClient"]
        try:
            with self.assertRaises(StoreError) as ctx:
                self.s.start_datagen({})
            self.assertEqual(ctx.exception.status, 409)
            self.assertIn("NoClassDefFoundError", ctx.exception.message)
        finally:
            jobs.running_dev_games = original

    def test_default_steps_cover_every_line(self):
        keys = [s["key"] for s in jobs.default_steps(helpers.REPO)]
        self.assertEqual(keys, ["26.2", "26.3", "1.21.11", "wiki"])
        self.assertIn(":mc26_3:fabric:runDatagen", jobs.default_steps(helpers.REPO)[1]["cmd"])
        self.assertIn("26.4", [s["key"] for s in jobs.default_steps(helpers.REPO, include_264=True)])


class CheckCliTests(unittest.TestCase):
    def test_check_on_the_real_repo_passes(self):
        """Das Gate: python tools/devserver/serve.py --check (gradlew checkBalance)."""
        proc = subprocess.run([sys.executable, str(helpers.DEVSERVER / "serve.py"), "--check", "--store", str(helpers.temp_dir())],
                              capture_output=True, text=True, encoding="utf-8", timeout=300)
        self.assertEqual(proc.returncode, 0, proc.stdout + proc.stderr)
        self.assertIn("checkBalance: ok", proc.stdout)

    def test_check_fails_on_a_stale_generated_file(self):
        repo = helpers.shared_full_repo()
        path = repo / INJECT_END_CITY
        before = path.read_bytes()
        self.addCleanup(lambda: path.write_bytes(before))
        path.write_text(path.read_text(encoding="utf-8").replace('"weight": 40', '"weight": 41'), encoding="utf-8")
        proc = subprocess.run([sys.executable, str(helpers.DEVSERVER / "serve.py"), "--check", "--repo", str(repo),
                               "--store", str(helpers.temp_dir())], capture_output=True, text=True, encoding="utf-8", timeout=300)
        self.assertEqual(proc.returncode, 1, proc.stdout)
        self.assertIn("runDatagen fehlt", proc.stdout)


if __name__ == "__main__":
    unittest.main()
