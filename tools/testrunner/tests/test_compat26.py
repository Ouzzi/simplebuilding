"""python -m unittest discover -s tools/testrunner/tests  (b-compat26: the log verdict of target compat26-client-263)"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import run  # noqa: E402

OK = "\n".join(f"[compat26] INFO mod {m} loaded=true" for m in ("simplebuilding", "lithium", "jei", "modmenu")) + \
    "\n[compat26] done: shots 12, faults 0\n"


class Compat26Test(unittest.TestCase):
    def test_target_listed_and_opt_in(self):
        t = next(x for x in run.INTEGRATION_TARGETS if x.id == "compat26-client-263")
        self.assertTrue(t.compat26 and t.audit and t.kind == "client")
        self.assertIn("-PmoduleClientTest=compat26", t.gradle_args)
        self.assertNotIn(t, run.TARGETS)

    def test_clean_log_passes(self):
        self.assertIsNone(run.compat26_check(OK))

    def test_faults_and_conflicts_fail(self):
        self.assertIn("Beweis", run.compat26_check("nothing"))
        self.assertIn("FAULT x", run.compat26_check("[compat26] FAULT x\n"))
        self.assertIn("Mixin-Konflikt", run.compat26_check(OK + "Critical injection failure: Redirector foo\n"))
        self.assertIn("lithium", run.compat26_check(OK.replace("lithium loaded=true", "lithium loaded=false")))


if __name__ == "__main__":
    unittest.main()
