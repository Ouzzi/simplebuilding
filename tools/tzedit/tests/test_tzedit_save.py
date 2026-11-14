"""N50 TZEDIT50: rules of a testcentre editor save (tools/tzedit/tzedit_save.py), the devqueue endpoint and the hub commit."""
import http.client
import json
import subprocess
import sys
import tempfile
import threading
import unittest
from pathlib import Path

HERE = Path(__file__).resolve()
sys.path.insert(0, str(HERE.parents[1]))
sys.path.insert(0, str(HERE.parents[2] / "zentrale-web"))
import tzedit_save as tz  # noqa: E402

GOOD = {"variant": "lines", "area": "metals-excerpt", "columns": ["button", "plate"],
        "rows": [{"key": "copper", "label": "copper", "cells": ["copper_button", None]},
                 {"key": "diamond", "label": "diamond", "cells": ["#red", "simplelib:diamond_pressure_plate"]}],
        "comment": "test"}


def bad(**change):
    d = json.loads(json.dumps(GOOD))
    d.update(change)
    return d


class CheckSave(unittest.TestCase):
    def test_good_is_cleaned(self):
        s = tz.check_save(dict(GOOD, extra="dropped"))
        self.assertNotIn("extra", s)
        self.assertEqual(tz.layout_path(s), tz.LAYOUT_DIR + "/layout_lines_metals-excerpt.json")
        self.assertEqual(json.loads(tz.layout_text(s))["rows"][1]["cells"][0], "#red")

    def test_rejects(self):
        for payload in (bad(variant="../x"), bad(area="A B"), bad(columns=[]), bad(columns=["ok", "Bad"]),
                        bad(rows=[{"key": "a", "cells": ["x"]}]),                      # wrong length
                        bad(rows=[{"key": "a", "cells": ["x", "y"]}, {"key": "a", "cells": [None, None]}]),  # duplicate key
                        bad(rows=[{"key": "a", "cells": ["<script>", None]}]),
                        bad(rows=[{"key": "a", "cells": ["#blue", None]}]),
                        bad(excluded=["ok", 3]), bad(comment="x" * 600), [], "x"):
            with self.assertRaises(tz.SaveError, msg=str(payload)[:80]):
                tz.check_save(payload)


class Endpoint(unittest.TestCase):
    def setUp(self):
        from devqueue import DevQueueServer
        self.tmp = tempfile.TemporaryDirectory()
        self.server = DevQueueServer(("127.0.0.1", 0), Path(self.tmp.name))
        threading.Thread(target=self.server.serve_forever, daemon=True).start()

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.tmp.cleanup()

    def call(self, method, path, body=None, user="u@test"):
        c = http.client.HTTPConnection("127.0.0.1", self.server.server_address[1])
        headers = {"Content-Type": "application/json"}
        if user:
            headers["Cf-Access-Authenticated-User-Email"] = user
        c.request(method, path, body=json.dumps(body) if body is not None else None, headers=headers)
        r = c.getresponse()
        return r.status, json.loads(r.read() or b"null")

    def test_save_list_get(self):
        self.assertEqual(self.call("POST", "/api/testcentre", GOOD, user=None)[0], 403)
        self.assertEqual(self.call("POST", "/api/testcentre", bad(area="A B"))[0], 400)
        status, rec = self.call("POST", "/api/testcentre", GOOD)
        self.assertEqual(status, 201)
        self.assertEqual((rec["by"], rec["state"]), ("u@test", "new"))
        status, items = self.call("GET", "/api/testcentre")
        self.assertEqual((status, items[0]["sid"], items[0]["rowCount"]), (200, rec["sid"], 2))
        status, full = self.call("GET", "/api/testcentre/" + rec["sid"])
        self.assertEqual(full["rows"][0]["cells"], ["copper_button", None])
        self.assertEqual(self.call("GET", "/api/testcentre/../x")[0], 404)


class HubCommit(unittest.TestCase):
    def test_apply_creates_branch_and_never_overwrites(self):
        with tempfile.TemporaryDirectory() as tmp:
            hub, work = Path(tmp) / "hub.git", Path(tmp) / "w"
            run = lambda *a, cwd=None: subprocess.run(a, cwd=cwd, check=True, capture_output=True)
            run("git", "init", "-q", "--bare", str(hub))
            run("git", "init", "-q", "-b", "claude-wave1", str(work))
            (work / "README").write_text("x")
            run("git", "-C", str(work), "add", "README")
            run("git", "-C", str(work), "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-qm", "base")
            run("git", "-C", str(work), "push", "-q", str(hub), "claude-wave1")
            branch = tz.apply_save(GOOD, "1234567", hub, "claude-wave1", "u@test")
            self.assertEqual(branch, "tz-layout-lines-metals-excerpt-1234567")
            text = run("git", "--git-dir", str(hub), "show", f"{branch}:{tz.LAYOUT_DIR}/layout_lines_metals-excerpt.json").stdout
            self.assertEqual(json.loads(text)["rows"][0]["key"], "copper")
            with self.assertRaises(tz.SaveError):
                tz.apply_save(GOOD, "1234567", hub, "claude-wave1")


if __name__ == "__main__":
    unittest.main()
