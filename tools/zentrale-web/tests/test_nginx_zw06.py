"""nginx templates of the one-host site (a-zw06).

Static checks always; with an nginx binary (SB_NGINX=<path>, or nginx on PATH / /usr/sbin/nginx) the rendered
"parallel" and "switch" sites run for real against an in-process devqueue and smoke_zw06.run() checks them.
"""
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path

WEB = Path(__file__).resolve().parents[1]
NGX = WEB / "nginx"
sys.path.insert(0, str(WEB))

import smoke_zw06  # noqa: E402
from devqueue import DevQueueServer  # noqa: E402


def blocks(text):
    """Top-level server { ... } blocks."""
    out, depth, start = [], 0, None
    for i, ch in enumerate(text):
        if ch == "{":
            if depth == 0 and text[max(0, i - 8):i].strip().endswith("server"):
                start = i
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0 and start is not None:
                out.append(text[start:i + 1])
                start = None
    return out


def location(text, head):
    """Body of the location whose header line is `head` (brace-balanced)."""
    i = text.index(head)
    j, depth = text.index("{", i), 0
    for k in range(j, len(text)):
        depth += {"{": 1, "}": -1}.get(text[k], 0)
        if depth == 0:
            return text[j:k + 1]
    raise AssertionError(head)


class StaticTest(unittest.TestCase):
    def test_every_server_has_allowlist(self):
        for f in ("prod-old.conf", "wiki-one.conf", "legacy-current.conf", "legacy-redirect.conf"):
            bs = blocks((NGX / f).read_text())
            self.assertTrue(bs, f)
            for b in bs:
                self.assertIn("include /etc/nginx/snippets/simplemods-allow.conf;", b, f)

    def test_protected_paths(self):
        one = (NGX / "wiki-one.conf").read_text()
        for head in ("location ^~ /dev/ ", "location = /dev/_overlay.js ", "location ^~ /zentrale/ ",
                     "location ^~ /api/ "):
            self.assertIn("auth_request /_auth;", location(one, head), head)
        api = location(one, "location ^~ /api/ ")
        self.assertIn("proxy_set_header Cf-Access-Authenticated-User-Email $sb_user;", api)
        for head in ("location ^~ /api/auth/ ", "location = /api/auth/check ", "location = /_auth "):
            body = location(one, head)
            self.assertNotIn("auth_request /_auth", body, head)
            self.assertIn('proxy_set_header Cf-Access-Authenticated-User-Email "";', body, head)
        self.assertIn("internal;", location(one, "location = /_auth "))
        self.assertIn("limit_req zone=sb_login", location(one, "location ^~ /api/auth/ "))
        self.assertIn("absolute_redirect off;", one)

    def test_legacy_keeps_setup_and_downloads(self):
        red = (NGX / "legacy-redirect.conf").read_text()
        zen = blocks(red)[1]
        for head in ("location /setup/ ", "location /downloads/ "):
            self.assertNotIn("return 30", location(zen, head))
        self.assertIn("simplemods-wiki.azarfane.com/zentrale$request_uri", zen)
        self.assertIn("simplemods-wiki.azarfane.com/dev$request_uri", blocks(red)[0])

    def test_no_private_addresses_in_repo(self):
        for f in NGX.glob("*.conf"):
            self.assertIsNone(re.search(r"\b(192\.168|10\.\d+\.\d+|172\.(1[6-9]|2\d|3[01]))\.", f.read_text()), f)


def nginx_bin():
    for c in (os.environ.get("SB_NGINX"), shutil.which("nginx"), "/usr/sbin/nginx"):
        if c and os.path.isfile(c) and os.access(c, os.X_OK):
            return c
    return None


def free_port():
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    p = s.getsockname()[1]
    s.close()
    return p


@unittest.skipUnless(nginx_bin(), "kein nginx (SB_NGINX=<pfad> setzen)")
class NginxIntegrationTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        t = self.root = Path(self.tmp.name)
        os.chmod(t, 0o755)
        (t / "prod/wiki").mkdir(parents=True)
        (t / "prod/wiki/index.html").write_text("<html><body>PROD-WIKI</body></html>")
        (t / "dev/wiki").mkdir(parents=True)
        (t / "dev/wiki/index.html").write_text("<html><body>DEV-WIKI</body></html>")
        (t / "dev/wiki/assets/vanilla/block").mkdir(parents=True)
        (t / "dev/tools").mkdir(parents=True)
        shutil.copytree(WEB, t / "dev/tools/zentrale-web", ignore=shutil.ignore_patterns("tests", "__pycache__"))
        for d in ("z/setup", "z/logs", "z/downloads"):
            (t / d).mkdir(parents=True)
        (t / "z/status.json").write_text("{}")
        (t / "z/setup/install.ps1").write_text("# installer")
        (t / "z/downloads/index.json").write_text("{}")
        (t / "allow.conf").write_text("allow 127.0.0.1; deny all;\n")
        self.data = t / "devqueue"
        self.data.mkdir()
        self.api = DevQueueServer(("127.0.0.1", 0), self.data)
        threading.Thread(target=self.api.serve_forever, daemon=True).start()
        self.ports = {p: free_port() for p in (8081, 8082, 8083, 8084)}
        self.proc = None

    def tearDown(self):
        if self.proc:
            self.proc.terminate()
            self.proc.wait(10)
        self.api.shutdown()
        self.api.server_close()
        self.tmp.cleanup()

    def start(self, stage):
        t = self.root
        parts = [(NGX / "common.conf").read_text()]
        if stage == "parallel":
            parts += [(NGX / f).read_text() for f in ("prod-old.conf", "wiki-one.conf", "legacy-current.conf")]
        else:
            parts += [(NGX / "wiki-one.conf").read_text().replace("  # @LISTEN_MORE@", "  listen 8081;"),
                      (NGX / "legacy-redirect.conf").read_text()]
        site = "\n".join(parts)
        site = (site.replace("/srv/wiki-prod", str(t / "prod")).replace("/srv/wiki-dev", str(t / "dev"))
                .replace("/srv/zentrale", str(t / "z")).replace("/etc/nginx/snippets/simplemods-allow.conf", str(t / "allow.conf"))
                .replace("127.0.0.1:8090", f"127.0.0.1:{self.api.server_address[1]}"))
        for old, new in self.ports.items():
            site = site.replace(f"listen {old};", f"listen 127.0.0.1:{new};")
        user = "user root;\n" if os.geteuid() == 0 else ""
        conf = (f"{user}worker_processes 1; daemon off; pid {t}/nginx.pid; error_log {t}/error.log;\n"
                f"events {{ worker_connections 64; }}\n"
                f"http {{ access_log off; default_type application/octet-stream;\n"
                f"types {{ text/html html; application/javascript js; application/json json; image/png png; }}\n"
                + "".join(f"{k}_temp_path {t}/{k};\n" for k in ("client_body", "proxy", "fastcgi", "uwsgi", "scgi"))
                + site + "\n}\n")
        (t / "nginx.conf").write_text(conf)
        r = subprocess.run([nginx_bin(), "-t", "-p", str(t), "-c", str(t / "nginx.conf")], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.proc = subprocess.Popen([nginx_bin(), "-p", str(t), "-c", str(t / "nginx.conf")],
                                     stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        for _ in range(50):
            try:
                socket.create_connection(("127.0.0.1", self.ports[8084]), 0.2).close()
                return
            except OSError:
                time.sleep(0.1)
        self.fail("nginx startet nicht: " + (t / "error.log").read_text())

    def smoke(self, port, legacy=False):
        fails = smoke_zw06.run(self.ports[port], self.data / "auth.json", legacy=legacy,
                               legacy_ports=(self.ports[8082], self.ports[8083]), wiki_marker="PROD-WIKI")
        self.assertEqual(fails, [])

    def test_parallel(self):
        self.start("parallel")
        self.smoke(8084)
        st, _, body = smoke_zw06.request(self.ports[8081], "GET", "/")
        self.assertEqual((st, b"PROD-WIKI" in body), (200, True))
        # old zentrale unchanged: header-protected, /wiki/ still there
        st, _, _ = smoke_zw06.request(self.ports[8083], "GET", "/wiki/")
        self.assertEqual(st, 403)
        st, _, body = smoke_zw06.request(self.ports[8083], "GET", "/wiki/", {"Cf-Access-Authenticated-User-Email": "a@b"})
        self.assertEqual(st, 200)
        self.assertIn(b"/dev-overlay.js", body)

    def test_switch(self):
        self.start("switch")
        self.smoke(8081)
        self.smoke(8084, legacy=True)


if __name__ == "__main__":
    unittest.main()
