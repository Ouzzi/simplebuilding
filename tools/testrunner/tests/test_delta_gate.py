"""python -m unittest discover -s tools/testrunner/tests"""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import delta_gate  # noqa: E402

MANIFEST = {"modules": [
    {"id": "simplebuilding", "requires": ["simplestorage"]},
    {"id": "simplestorage", "requires": []},
    {"id": "simpleshops", "requires": ["simplemoney"]},
    {"id": "simplemoney", "requires": []},
    {"id": "simplevisuals", "requires": []},
]}
TARGETS = ["fabric-263", "neoforge-263", "forge-263", "integration-263",
           "module-simplevisuals-fabric-263", "module-simplevisuals-neoforge-263", "module-simplevisuals-forge-263",
           "module-simplevisuals-standalone-fabric-263", "module-simplevisuals-client-263",
           "module-simplemoney-fabric-263", "module-simpleshops-fabric-263", "module-simplestorage-fabric-263"]


class PlanTest(unittest.TestCase):
    def plan(self, *paths):
        return delta_gate.plan(list(paths), MANIFEST, TARGETS)

    def test_docs_only_needs_check_only(self):
        self.assertEqual(self.plan("docs/ai/X.md", ".claude/QUEUE.md", "wiki/data/a.js"),
                         {"mode": "delta", "modules": [], "targets": []})

    def test_one_module_runs_its_targets_without_client(self):
        p = self.plan("modules/simplevisuals/shared/java/A.java")
        self.assertEqual(p["targets"], ["module-simplevisuals-fabric-263", "module-simplevisuals-neoforge-263",
                                        "module-simplevisuals-forge-263", "module-simplevisuals-standalone-fabric-263",
                                        "integration-263"])

    def test_dependants_come_along(self):
        p = self.plan("modules/simplemoney/x")
        self.assertEqual(p["modules"], ["simplemoney", "simpleshops"])
        p = self.plan("modules/simplestorage/x")
        self.assertEqual(p["targets"][:3], delta_gate.CORE)

    def test_shared_code_means_full(self):
        for path in ("common/src/a.java", "modules/simplelib/a.java", "framework/x", "build.gradle", "tools/testrunner/run.py"):
            self.assertEqual(self.plan(path)["mode"], "full", path)


if __name__ == "__main__":
    unittest.main()
