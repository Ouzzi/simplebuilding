"""N74 flat mod addresses: path form <-> route for every wiki mod x section and the old ?mod=/#/ links.

Runs wiki/tests/modpath_core.cjs, which takes the real address functions out of wiki/index.html (needs node).
"""
import json
import shutil
import subprocess
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent


@unittest.skipUnless(shutil.which('node'), 'node fehlt')
class ModPathTest(unittest.TestCase):
    def test_round_trip_and_old_links(self):
        r = subprocess.run(['node', str(HERE / 'modpath_core.cjs')], capture_output=True, text=True, timeout=300)
        out = json.loads((r.stdout.strip().splitlines() or ['{}'])[-1])
        self.assertEqual(out.get('failures'), [], r.stderr[-2000:])
        self.assertEqual(r.returncode, 0, r.stderr[-2000:])
        self.assertGreater(out['checks'], 1500)


if __name__ == '__main__':
    unittest.main()
