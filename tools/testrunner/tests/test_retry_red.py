"""python -m unittest discover -s tools/testrunner/tests"""

import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import retry_red  # noqa: E402

GATE = """gate on abc
check: OK
tests [fabric-263,neoforge-263,forge-263]: NOT GREEN
  ROT fabric-263 simplebuilding:a_test
  ROT fabric-263 simplebuilding:a_test
VERDICT: RED
"""
STANDALONE = """  ROT module-simplemobs-standalone-fabric-263 simplemobs:fear
      message
  NICHT gruen: 9/10 bestanden, 1 rot
"""


class RetryRedTest(unittest.TestCase):
    def logs(self, *texts):
        d = Path(tempfile.mkdtemp())
        paths = []
        for i, t in enumerate(texts):
            p = d / f"{i}.log"
            p.write_text(t, encoding="utf-8")
            paths.append(str(p))
        return d, paths

    def test_parses_red_tests_once(self):
        self.assertEqual(retry_red.red_tests(GATE), [("fabric-263", "simplebuilding:a_test")])

    def test_blockers(self):
        self.assertEqual(retry_red.blockers(GATE), [])
        self.assertIn("check failed", retry_red.blockers("check: FAILED (1)\n"))
        self.assertTrue(retry_red.blockers("  FEHLER Fabric: timeout\n"))
        self.assertIn("red without ROT lines", retry_red.blockers("tests [x]: NOT GREEN\n"))

    def test_green_on_second_run_is_logged_as_flaky(self):
        d, paths = self.logs(GATE, STANDALONE)
        flaky = d / "WACKEL.md"
        with mock.patch.object(retry_red, "retry", return_value=True) as r:
            self.assertEqual(retry_red.main(paths + ["--flaky-log", str(flaky), "--ref", "abc"]), 0)
        self.assertEqual(r.call_count, 2)
        text = flaky.read_text(encoding="utf-8")
        self.assertIn("| abc | fabric-263 | simplebuilding:a_test |", text)
        self.assertIn("simplemobs:fear", text)

    def test_red_again_stays_red(self):
        _, paths = self.logs(GATE)
        with mock.patch.object(retry_red, "retry", return_value=False):
            self.assertEqual(retry_red.main(paths), 1)

    def test_too_many_or_blocked_runs_nothing(self):
        many = "".join(f"  ROT fabric-263 ns:t{i}\n" for i in range(7))
        _, paths = self.logs(many)
        with mock.patch.object(retry_red, "retry") as r:
            self.assertEqual(retry_red.main(paths), 1)
            _, paths = self.logs("check: FAILED (1)\n" + GATE)
            self.assertEqual(retry_red.main(paths), 1)
        r.assert_not_called()


if __name__ == "__main__":
    unittest.main()
