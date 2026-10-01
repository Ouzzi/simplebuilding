"""Register, Produktiv-Umgebung, Start/Stopp und HTTP-Schutz der Zentrale."""
import json
import os
import socket
import sys
import tempfile
import threading
import unittest
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path
from unittest.mock import patch

HERE = Path(__file__).resolve().parents[1]
ROOT = HERE.parents[1]
sys.path.insert(0, str(ROOT / "tools/launchhub"))
import importlib.util  # noqa: E402

_spec = importlib.util.spec_from_file_location("zentrale_server", HERE / "server.py")
zentrale = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(zentrale)


def free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


class RegistryTests(unittest.TestCase):
    def write(self, centrals):
        d = tempfile.mkdtemp()
        p = Path(d) / "c.json"
        p.write_text(json.dumps({"centrals": centrals}), encoding="utf-8")
        return p

    def test_shipped_registry_is_valid_and_lists_the_three_centrals(self):
        ids = [c["id"] for c in zentrale.load_registry()]
        self.assertEqual(ids, ["wiki", "balancing", "launchhub"])

    def test_rejects_dry_run_env_absolute_paths_and_duplicates(self):
        base = {"id": "a", "port": 9001, "args": ["x.py"]}
        for bad in ([dict(base, env={"SB_HUB_DRY_RUN": "1"})],
                    [dict(base, args=["C:/x.py"])],
                    [dict(base, args=["../x.py"])],
                    [base, dict(base, id="b")],
                    [dict(base, port=80)],
                    [dict(base, id="no-dash")]):
            with self.assertRaises(zentrale.ZentraleError):
                zentrale.load_registry(self.write(bad))

    def test_production_env_strips_every_dry_run_variable(self):
        env = zentrale.production_env({"X": "1"}, base={"SB_HUB_DRY_RUN": "1", "SB_VOICE_DRY_RUN": "1",
                                                        "my_dry_run": "y", "PATH": "p"})
        self.assertEqual({k for k in env if "DRY_RUN" in k.upper()}, set())
        self.assertTrue(env["PATH"].endswith(os.pathsep + "p"), "the inherited PATH is kept at the end")
        self.assertEqual(env["X"], "1")


class ToolchainTests(unittest.TestCase):
    def test_children_get_this_python_on_the_path(self):
        env = zentrale.production_env(base={"PATH": "C:/nothing"})
        self.assertTrue(env["PATH"].split(os.pathsep)[0].lower().endswith("bin") or
                        str(Path(sys.executable).parent) in env["PATH"])
        self.assertIn(str(Path(sys.executable).parent), env["PATH"])

    def test_java_major_reads_the_release_file(self):
        d = Path(tempfile.mkdtemp())
        (d / "release").write_text('JAVA_VERSION="25.0.4"', encoding="utf-8")
        self.assertEqual(zentrale.java_major(str(d)), 25)
        (d / "release").write_text('JAVA_VERSION="1.8.0_504"', encoding="utf-8")
        self.assertEqual(zentrale.java_major(str(d)), 8)
        self.assertEqual(zentrale.java_major(None), 0)


class HubProductionTests(unittest.TestCase):
    def test_hub_is_never_dry_under_the_zentrale(self):
        from hub import settings
        with patch.dict(os.environ, {"SB_HUB_PRODUCTION": "1"}, clear=False):
            os.environ.pop("SB_HUB_DRY_RUN", None)
            self.assertEqual(settings.dry_run(), "")
            self.assertFalse(settings.load()["dryRun"])


class LifecycleTests(unittest.TestCase):
    def test_start_status_stop_only_own_process(self):
        port = free_port()
        z = zentrale.Zentrale([{"id": "dummy", "name": "Dummy", "port": port, "path": "/",
                                "args": ["-m", "http.server", str(port), "--bind", "127.0.0.1"]}],
                              root=Path(tempfile.mkdtemp()), log_dir=Path(tempfile.mkdtemp()))
        try:
            self.assertEqual(z.start(), ["dummy"])
            self.assertEqual(z.wait_ready(["dummy"]), ["dummy"])
            self.assertEqual(z.status()[0]["state"], "own")
            self.assertEqual(z.start(), [], "a running central is not started twice")
        finally:
            self.assertEqual(z.stop(["dummy"]), ["dummy"])
        self.assertEqual(z.status()[0]["state"], "stopped")

    def test_external_instance_is_shown_but_never_stopped(self):
        port = free_port()
        ext = ThreadingHTTPServer(("127.0.0.1", port), zentrale.BaseHTTPRequestHandler)
        threading.Thread(target=ext.serve_forever, daemon=True).start()
        try:
            z = zentrale.Zentrale([{"id": "ext", "name": "E", "port": port, "path": "/", "args": ["x.py"]}])
            self.assertEqual(z.status()[0]["state"], "external")
            self.assertEqual(z.start(), [])
            self.assertEqual(z.stop(), [])
        finally:
            ext.shutdown()
            ext.server_close()

    def test_unknown_ids_are_rejected(self):
        z = zentrale.Zentrale(zentrale.load_registry())
        with self.assertRaises(zentrale.ZentraleError):
            z.start(["nope"])


class ExternalStopTests(unittest.TestCase):
    def test_signature_and_match(self):
        wiki = {"args": ["-m", "http.server", "8765"]}
        hub = {"args": ["tools/launchhub/server.py", "--port", "8773"]}
        self.assertEqual(zentrale.signature(wiki), "-m http.server")
        self.assertTrue(zentrale.matches(hub, r'"C:\py\python.exe" tools\launchhub\server.py --port 8773'))
        self.assertFalse(zentrale.matches(hub, "java.exe -jar minecraft.jar"))
        self.assertFalse(zentrale.matches(hub, "python tools/devserver/serve.py"))

    def start_external(self, port):
        import subprocess
        p = subprocess.Popen([sys.executable, "-m", "http.server", str(port), "--bind", "127.0.0.1"],
                             stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.addCleanup(lambda: p.poll() is None and p.kill())
        z = zentrale.Zentrale([{"id": "x", "name": "X", "port": port, "path": "/", "args": ["-m", "http.server"]}])
        self.assertEqual(z.wait_ready(["x"], 15), ["x"])
        return p

    def test_external_instance_stops_only_on_request(self):
        port = free_port()
        p = self.start_external(port)
        z = zentrale.Zentrale([{"id": "x", "name": "X", "port": port, "path": "/", "args": ["-m", "http.server"]}])
        self.assertEqual(z.stop(["x"]), [], "without external=True an external instance stays")
        self.assertEqual(z.stop(["x"], external=True), ["x"])
        self.assertEqual(z.status()[0]["state"], "stopped")
        p.wait(timeout=10)

    def test_a_foreign_process_on_the_port_is_never_stopped(self):
        port = free_port()
        p = self.start_external(port)
        z = zentrale.Zentrale([{"id": "x", "name": "X", "port": port, "path": "/", "args": ["tools/launchhub/server.py"]}])
        with self.assertRaises(zentrale.ZentraleError):
            z.stop(["x"], external=True)
        self.assertIsNone(p.poll(), "the foreign process was killed")


class HttpTests(unittest.TestCase):
    def setUp(self):
        self.z = zentrale.Zentrale(zentrale.load_registry())
        self.srv = ThreadingHTTPServer(("127.0.0.1", 0), zentrale.make_handler(self.z))
        self.port = self.srv.server_address[1]
        threading.Thread(target=self.srv.serve_forever, daemon=True).start()

    def tearDown(self):
        self.srv.shutdown()
        self.srv.server_close()

    def req(self, path, method="GET", headers=None, body=None):
        r = urllib.request.Request(f"http://127.0.0.1:{self.port}{path}", method=method,
                                   headers=headers or {}, data=body)
        try:
            with urllib.request.urlopen(r) as resp:
                return resp.status, json.loads(resp.read() or b"{}") if path.startswith("/api") else resp.read()
        except urllib.error.HTTPError as e:
            return e.code, None

    def test_page_and_status_are_served(self):
        self.assertEqual(self.req("/")[0], 200)
        status, data = self.req("/api/centrals")
        self.assertEqual(status, 200)
        self.assertEqual(len(data["centrals"]), 3)

    def test_post_needs_the_client_header(self):
        self.assertEqual(self.req("/api/stop", "POST", body=b"{}")[0], 403)
        status, data = self.req("/api/stop", "POST", {"X-Zentrale-Client": "1"}, b'{"ids": ["wiki"]}')
        self.assertEqual(status, 200)
        self.assertEqual(data["stopped"], [])

    def test_foreign_host_header_is_refused(self):
        self.assertEqual(self.req("/api/centrals", headers={"Host": "evil.example"})[0], 403)


if __name__ == "__main__":
    unittest.main()
