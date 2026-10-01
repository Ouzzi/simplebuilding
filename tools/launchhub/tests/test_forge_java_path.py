"""Machine-specific Java 8 overrides must also reach manifest-driven targets."""
import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[3]
spec = importlib.util.spec_from_file_location("forge_path_runner", ROOT / "tools/testrunner/run.py")
runner = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = runner
spec.loader.exec_module(runner)


class ForgeJavaPathTests(unittest.TestCase):
    def test_environment_overrides_legacy_path_without_losing_flags(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "modules").mkdir()
            manifest = {"modules": [{"id": "probe", "displayName": "Probe", "tests": {
                "catalogues": ["modules/probe/Tests.java"], "loaders": {"forge": {
                    "task": ":modules:probe:forge:runGameTestServer", "report": "probe.xml",
                    "gradleArgs": ["-Pforge263=true", "-Pforge_runs=true", "-Porg.gradle.java.installations.paths=C:/old"]
                }}}}]}
            (root / "modules/modules.json").write_text(json.dumps(manifest))
            with patch.dict("os.environ", {"SIMPLEBUILDING_JAVA8_HOME": "C:/portable/java8"}):
                args = runner.module_targets(root)[0].gradle_args
                self.assertEqual(args, ("-Pforge263=true", "-Pforge_runs=true", "-Porg.gradle.java.installations.paths=C:/portable/java8"))
            with patch.dict("os.environ", {}, clear=True):
                self.assertIn("-Porg.gradle.java.installations.paths=C:/old", runner.module_targets(root)[0].gradle_args)
                manifest["modules"][0]["tests"]["loaders"]["forge"]["gradleArgs"].pop()
                (root / "modules/modules.json").write_text(json.dumps(manifest))
                self.assertIn("-Porg.gradle.java.installations.paths=" + runner.JAVA8_HOME, runner.module_targets(root)[0].gradle_args)


if __name__ == "__main__":
    unittest.main()
