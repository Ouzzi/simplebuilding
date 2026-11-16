"""python -m unittest discover -s tools/testrunner/tests"""

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import delta_gate  # noqa: E402
import gate_due  # noqa: E402

MANIFEST = {"modules": [
    {"id": "simplebuilding", "requires": ["simplestorage"]},
    {"id": "simplestorage", "requires": []},
    {"id": "simpleshops", "requires": ["simplemoney"]},
    {"id": "simplemoney", "requires": []},
    {"id": "simplevisuals", "requires": []},
    {"id": "simplebank", "requires": ["simpleshops"]},
]}
TARGETS = ["fabric-263", "neoforge-263", "forge-263", "integration-263",
           "module-simplevisuals-fabric-263", "module-simplevisuals-neoforge-263", "module-simplevisuals-forge-263",
           "module-simplevisuals-standalone-fabric-263", "module-simplevisuals-standalone-neoforge-263",
           "module-simplevisuals-with-simplemoney-fabric-263", "module-simplevisuals-client-263",
           "module-simplemoney-fabric-263", "module-simpleshops-fabric-263", "module-simplestorage-fabric-263",
           "module-simplebank-fabric-263"]
CHECK_ONLY = {"mode": "delta", "modules": [], "changed_modules": [], "targets": [], "filters": [], "compile": []}
DI = ("fabric-263", delta_gate.DATA_INTEGRITY)
TC = ("fabric-263", delta_gate.TEST_CENTRE)


class PlanTest(unittest.TestCase):
    def plan(self, *paths, loaders=("fabric",), registry=False):
        return delta_gate.plan(list(paths), MANIFEST, TARGETS, list(loaders), registry)

    def test_docs_only_needs_check_only(self):
        self.assertEqual(self.plan("docs/ai/X.md", ".claude/QUEUE.md", "wiki/data/a.js", "AGENTS.md"), CHECK_ONLY)

    def test_texture_generators_and_web_tools_need_check_only(self):
        self.assertEqual(self.plan("tools/textures/herobrine_head_2026_10_20.py", "tools/zentrale-web/index.html",
                                   "tools/landing/paket.html", "tools/check_lang_keys.py"), CHECK_ONLY)

    def test_one_module_runs_its_fabric_targets_without_client(self):
        p = self.plan("modules/simplevisuals/shared/java/A.java")
        self.assertEqual(p["targets"], ["module-simplevisuals-fabric-263", "module-simplevisuals-standalone-fabric-263",
                                        "module-simplevisuals-with-simplemoney-fabric-263", "integration-263"])
        self.assertEqual(p["changed_modules"], ["simplevisuals"])
        self.assertEqual(p["filters"], [DI])  # data_integrity always, test_centre only with registry changes
        self.assertEqual(p["compile"], [":mc26_3:fabric:compileJava", ":modules:simplevisuals:fabric:compileJava"])

    def test_all_loaders_when_widened(self):
        p = self.plan("modules/simplevisuals/x", loaders=("fabric", "neoforge", "forge"))
        self.assertIn("module-simplevisuals-neoforge-263", p["targets"])
        self.assertIn("module-simplevisuals-standalone-neoforge-263", p["targets"])
        self.assertNotIn("module-simplevisuals-client-263", p["targets"])

    def test_registry_change_adds_test_centre(self):
        self.assertEqual(self.plan("modules/simplevisuals/x", registry=True)["filters"], [DI, TC])

    def test_dependants_come_along_transitively(self):
        p = self.plan("modules/simplemoney/x")
        self.assertEqual(p["modules"], ["simplebank", "simplemoney", "simpleshops"])
        self.assertEqual(p["changed_modules"], ["simplemoney"])
        self.assertEqual(p["targets"], ["module-simplebank-fabric-263", "module-simplemoney-fabric-263",
                                        "module-simpleshops-fabric-263", "integration-263"])

    def test_module_simplebuilding_requires_brings_core_fabric_only(self):
        p = self.plan("modules/simplestorage/x", registry=True)
        self.assertEqual(p["targets"][0], "fabric-263")
        self.assertNotIn("neoforge-263", p["targets"])
        self.assertEqual(p["filters"], [])  # fabric-263 runs whole, data_integrity/test_centre included

    def test_sb_resources_are_the_simplebuilding_module(self):
        for path in ("mc26_3/overlay/resources/assets/simplebuilding/textures/block/a.png",
                     "src/main/resources/assets/simplebuilding/lang/de_de.json",
                     "mc26_3/generated/data/simplebuilding/recipe/a.json"):
            p = self.plan(path)
            self.assertEqual((p["mode"], p["changed_modules"], p["targets"]),
                             ("delta", ["simplebuilding"], ["fabric-263", "integration-263"]), path)

    def test_integration_only_runs_integration(self):
        p = self.plan("integration/src/gametest/java/A.java")
        self.assertEqual((p["targets"], p["filters"]), (["integration-263"], [DI]))

    def test_shared_code_means_full(self):
        for path in ("common/src/shared/java/a.java", "src/main/java/a.java", "mc26_3/framework/java/a.java",
                     "mc26_3/fabric/build.gradle", "modules/simplelib/a.java", "modules/wiringexample/shared/java/a/B.java",
                     "modules/simplelib/build.gradle", "modules/simplelib/shared/resources/simplelib.mixins.json",
                     "modules/wiringexample/fabric/src/main/resources/fabric.mod.json", "modules/simplelib/forge/src/main/resources/META-INF/mods.toml", "modules/simplelib/shared/java/x/Y.json",
                     "framework/x", "build.gradle", "settings.gradle", "gradle.properties", "gradle/wrapper/x",
                     "tools/testrunner/run.py", "tools/modtree/modtree.py"):
            self.assertEqual(self.plan(path)["mode"], "full", path)

    def test_shared_module_data_runs_only_that_module(self):  # N81
        p = self.plan("modules/simplelib/shared/resources/assets/simplelib/lang/de_de.json")
        self.assertEqual((p["mode"], p["modules"], p["changed_modules"]), ("delta", ["simplelib"], ["simplelib"]))
        self.assertEqual(p["targets"], ["integration-263"])
        self.assertEqual(p["filters"], [DI])

    def test_module_tools_wiki_docs_are_check_only(self):  # N81
        self.assertEqual(self.plan("modules/simplelib/tools/release_probe.py", "modules/simplestats/tools/p.py",
                                   "modules/simplechess/wiki/manual.json", "modules/simplevisuals/docs/a.md"), CHECK_ONLY)

    def test_manifest_runs_changed_entries(self):  # N81: was full
        p = delta_gate.plan(["modules/modules.json"], MANIFEST, TARGETS, ["fabric"], manifest_changed={"simplemoney", "gone"})
        self.assertEqual(p["mode"], "delta")
        self.assertEqual(p["changed_modules"], ["simplemoney"])
        self.assertIn("module-simplebank-fabric-263", p["targets"])
        self.assertIn("integration-263", p["targets"])
        p = delta_gate.plan(["modules/modules.json"], MANIFEST, TARGETS, ["fabric"], manifest_changed=set())
        self.assertEqual((p["mode"], p["targets"], p["filters"]), ("delta", ["integration-263"], [DI]))

    def test_manifest_changes(self):
        old = {"modules": [{"id": "a", "requires": []}, {"id": "b"}, {"id": "c"}]}
        new = {"modules": [{"id": "a", "requires": ["b"]}, {"id": "b"}, {"id": "d"}]}
        self.assertEqual(delta_gate.manifest_changes(old, new), {"a", "d"})

    def test_sb_game_tests_are_the_simplebuilding_module(self):  # N81: was full
        p = self.plan("common/src/shared/clientgametest/java/com/simplebuilding/clientgametest/X.java")
        self.assertEqual(p["mode"], "delta")
        self.assertIn("fabric-263", p["targets"])

    def test_one_shared_file_among_module_files_is_full(self):
        self.assertEqual(self.plan("modules/simplevisuals/x", "build.gradle")["mode"], "full")


class RegistryTest(unittest.TestCase):
    def test_new_blockstate_file(self):
        self.assertTrue(delta_gate.registry_changed("A\tmodules/x/assets/x/blockstates/a.json\n", ""))
        self.assertFalse(delta_gate.registry_changed("M\tmodules/x/assets/x/blockstates/a.json\n", ""))

    def test_register_line(self):
        self.assertTrue(delta_gate.registry_changed("", "+    BLOCKS.register(\"a\", A::new);\n"))
        self.assertTrue(delta_gate.registry_changed("", "-Registry.register(x)\n"))
        self.assertFalse(delta_gate.registry_changed("", "+++ b/A.java\n+int a = 1;\n"))


class DueTest(unittest.TestCase):
    def test_counter_cycle(self):
        with tempfile.TemporaryDirectory() as d:
            f = Path(d) / "full-due.json"
            s = gate_due.load(f)  # missing file = fresh start
            self.assertIsNone(gate_due.due(s))
            self.assertEqual(gate_due.status(s), "Voll fällig in 7 Bündeln / 15 Mods")
            for i in range(gate_due.DELTAS):
                self.assertIsNone(gate_due.due(gate_due.load(f), ["simplevisuals"]), i)
                gate_due.save(f, gate_due.record_delta(gate_due.load(f), ["simplevisuals", "simplemoney"]))
            s = gate_due.load(f)
            self.assertEqual((s["deltas"], s["mods"]), (7, 2))
            self.assertIn("7 delta gates", gate_due.due(s))
            gate_due.save(f, gate_due.record_full("abc123"))
            s = gate_due.load(f)
            self.assertEqual((s["deltas"], s["mods"], s["last_full_ref"]), (0, 0, "abc123"))

    def test_mods_count_cumulatively_including_the_new_change(self):
        s = {"deltas": 2, "mods": 14, "mod_ids": [f"m{i}" for i in range(14)], "last_full_ref": None}
        self.assertIsNone(gate_due.due(s, ["m1"]))  # already counted
        self.assertIn("15 mods", gate_due.due(s, ["new"]))
        self.assertEqual(gate_due.status(s), "Voll fällig in 5 Bündeln / 1 Mods")

    def test_cli(self):
        with tempfile.TemporaryDirectory() as d:
            f = str(Path(d) / "c.json")
            self.assertEqual(gate_due.main(["--file", f, "check"]), 1)
            self.assertEqual(gate_due.main(["--file", f, "record-delta", "r1", "--mods", "a,b"]), 0)
            self.assertEqual(gate_due.load(Path(f))["mod_ids"], ["a", "b"])
            self.assertEqual(gate_due.main(["--file", f, "record-full", "r2"]), 0)
            self.assertEqual(gate_due.load(Path(f))["deltas"], 0)


if __name__ == "__main__":
    unittest.main()
