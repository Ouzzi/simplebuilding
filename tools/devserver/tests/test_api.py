"""Der HTTP-Server: echte Anfragen gegen einen Server auf einem freien Port."""

import json
import threading
import unittest
import urllib.error
import urllib.request

import helpers
import serve


class ApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.service = helpers.fresh_store(helpers.shared_service())
        cls.server = serve.start(cls.service, helpers.REPO, "127.0.0.1", 0, tries=1)
        cls.port = cls.server.server_address[1]
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def request(self, path, body=None, headers=None, method=None):
        url = f"http://127.0.0.1:{self.port}{path}"
        data = None
        hdrs = {}
        if body is not None:
            data = body if isinstance(body, bytes) else json.dumps(body).encode()
            hdrs = {"Content-Type": "application/json", "X-Balance-Client": "1"}
        hdrs.update(headers or {})
        req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
        try:
            with urllib.request.urlopen(req, timeout=60) as res:
                return res.status, res.headers.get("Content-Type"), res.read()
        except urllib.error.HTTPError as err:
            return err.code, err.headers.get("Content-Type"), err.read()

    def json(self, path, body=None, **kw):
        status, _, raw = self.request(path, body, **kw)
        return status, json.loads(raw)

    def test_page_and_static_files(self):
        status, ctype, raw = self.request("/")
        self.assertEqual(status, 200)
        self.assertIn("text/html", ctype)
        self.assertIn(b"Balancing-Zentrale", raw)
        status, ctype, _ = self.request("/static/app.js")
        self.assertEqual(status, 200)
        self.assertIn(self.request("/static/../serve.py")[0], (403, 404))
        self.assertIn(self.request("/static/%2e%2e/serve.py")[0], (403, 404))
        self.assertIn(self.request("/wiki/assets/../../readme.md")[0], (403, 404))

    def test_state(self):
        status, data = self.json("/api/state")
        self.assertEqual(status, 200)
        self.assertIn("const:ModLootTableModifications.ENDERITE_CORE_CHANCE", data["snapshot"]["values"])
        self.assertEqual(data["store"]["version"], self.service.store.state()["version"])
        self.assertTrue(data["docs"])

    def test_writes_need_the_client_header_and_json(self):
        body = json.dumps({"baseVersion": 0, "changes": []}).encode()
        status, _, raw = self.request("/api/save", body, headers={"X-Balance-Client": ""})
        self.assertEqual(status, 403)
        status, _, raw = self.request("/api/save", body, headers={"Content-Type": "text/plain"})
        self.assertEqual(status, 415)
        status, _, raw = self.request("/api/save", b"{kaputt")
        self.assertEqual(status, 400)
        self.assertIn("JSON", json.loads(raw)["error"])
        status, _, raw = self.request("/api/state", headers={"Host": "evil.example"})
        self.assertEqual(status, 403)

    def test_preview_save_version_rollback_over_http(self):
        vid = "config:airJumpCooldownTicks"
        base = self.service.store.state()["version"]
        status, pv = self.json("/api/preview", {"baseVersion": base, "changes": [{"id": vid, "value": "140", "expected": 100}]})
        self.assertEqual(status, 200)
        self.assertEqual(pv["summary"][0]["new"], 140)
        self.assertEqual(pv["summary"][0]["apply"], "mod")
        self.assertEqual([x["mc"] for x in pv["summary"][0]["sites"]], ["26.2/26.3/26.4", "1.21.11"])
        self.assertTrue(any("ConfigOptionTests" in w for w in pv["summary"][0]["warnings"]))  # Spieltest hält den Standard
        status, bad = self.json("/api/save", {"baseVersion": base, "changes": [{"id": vid, "value": "x"}]})
        self.assertEqual(status, 400)
        self.assertIn("keine Zahl", bad["details"][0]["message"])
        status, res = self.json("/api/save", {"baseVersion": base, "changes": [{"id": vid, "value": "140", "expected": 100}], "message": "api"})
        self.assertEqual(status, 200)
        self.assertEqual(res["version"], base + 1)
        status, conflict = self.json("/api/save", {"baseVersion": base, "changes": [{"id": vid, "value": 150}]})
        self.assertEqual(status, 409)
        status, v = self.json(f"/api/version/{base + 1}")
        self.assertEqual(v["entries"][vid]["value"], 140)
        status, rb = self.json("/api/rollback", {"target": base, "baseVersion": base + 1})
        self.assertEqual(status, 200)
        self.assertNotIn(vid, self.service.store.state()["entries"])
        self.assertEqual(self.json("/api/version/999")[0], 404)

    def test_calculators(self):
        status, rep = self.json("/api/calc", {"item": "simplebuilding:enderite_core", "overrides": {}})
        self.assertEqual(status, 200)
        self.assertAlmostEqual(rep["best"]["mean"][0] if rep["best"]["key"] == "structure:end_city" else rep["rows"][0]["mean"][0], 38.1, delta=0.1)
        status, rev = self.json("/api/reverse", {"item": "simplebuilding:enderite_core", "row": "structure:end_city",
                                                  "tunable": {"id": "const:ModLootTableModifications.ENDERITE_CORE_CHANCE"},
                                                  "k": 1, "stat": "mean", "hours": 45, "overrides": {}})
        self.assertEqual(status, 200)
        self.assertTrue(rev["feasible"])
        status, err = self.json("/api/reverse", {"item": "simplebuilding:enderite_core", "tunable": {"id": "nope"}, "hours": 5})
        self.assertEqual(status, 400)
        status, ov = self.json("/api/overview", {"overrides": {"const:ModLootTableModifications.ENDERITE_CORE_CHANCE": 0.0035}})
        self.assertEqual(status, 200)
        row = next(r for r in ov["rows"] if r["item"] == "simplebuilding:enderite_core")
        self.assertLess(row["bestMean"], 38.0)
        self.assertIn("simplebuilding:wandering_trader/emerald_iron_cores", ov["offers"])

    def test_check_and_datagen_routes(self):
        status, res = self.json("/api/check")
        self.assertEqual(status, 200)
        self.assertTrue(res["ok"], res["errors"][:3])
        self.assertGreater(res["stats"]["generatedChecked"], 300)
        status, job = self.json("/api/datagen")
        self.assertEqual((status, job["status"]), (200, "idle"))
        status, err = self.json("/api/datagen", {})
        self.assertEqual(status, 403)  # der Test-Service ist schreibgeschützt
        self.assertEqual(self.json("/api/datagen/cancel", {})[0], 400)
        self.assertEqual(self.json("/api/handover")[0], 200)

    def test_docs_and_phase2(self):
        status, doc = self.json("/api/docs/LOOT-BALANCE.md")
        self.assertEqual(status, 200)
        self.assertIn("<table>", doc["html"])
        self.assertEqual(self.json("/api/docs/..%2Freadme.md")[0], 404)
        status, p2 = self.json("/api/phase2")
        self.assertEqual(status, 200)
        self.assertIn("groups", p2)


if __name__ == "__main__":
    unittest.main()
