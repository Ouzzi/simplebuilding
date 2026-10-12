#!/usr/bin/env python3
"""Local preview harness: serves the static page, a mocked status.json and downloads/index.json, and forwards
/api/ to an in-process devqueue server with a fixed login header (no access proxy needed).

    python3 tools/zentrale-web/tests/harness.py [--port 8765] [--no-downloads]
"""
import argparse
import http.client
import json
import sys
import tempfile
import threading
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

WEB = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WEB))
import modselect  # noqa: E402
from devqueue import DevQueueServer  # noqa: E402

STATUS = {"generatedAt": "2026-10-12T08:00:00+00:00", "gates": [], "queue": [], "jobs": [], "tests": [],
          "roadmap": "", "queueOpen": [], "logs": []}


def mock_downloads():
    files = []
    for n in modselect.catalog():
        if n["planned"]:
            continue
        for loader in n["loaders"] or ["fabric"]:
            files.append({"module": n["id"], "name": n["name"], "kind": n["kind"], "parent": n["parent"],
                          "loader": loader, "minecraft": "26.3", "version": "1.3.1" if n["kind"] == "super" else "0.4.0",
                          "file": f"{n['id']}-26.3-{loader}.jar", "size": 3_400_000 if n["kind"] == "super" else 260_000,
                          "sha256": "0" * 64, "url": f"files/abc/{loader}/26.3/{n['id']}.jar"})
    for loader, mc in (("fabric", "26.2"), ("neoforge", "26.2"), ("fabric", "1.21.11")):
        files.append({"module": "simplebuilding", "name": "SimpleBuilding", "kind": "super", "parent": None,
                      "loader": loader, "minecraft": mc, "version": "1.3.1", "file": f"simplebuilding-{mc}.jar",
                      "size": 2_900_000, "sha256": "1" * 64, "url": f"files/abc/{loader}/{mc}/simplebuilding.jar"})
    build = {"commit": "f9339cc26" + "0" * 31, "short": "f9339cc260", "ref": "claude-wave1",
             "date": "2026-10-12T07:40:00+00:00", "verdict": "VERDICT: GREEN", "standalone": "alles gruen", "files": files}
    older = dict(build, commit="a" * 40, short="aaaaaaaaaa", date="2026-10-11T19:10:00+00:00")
    return {"schemaVersion": 1, "updatedAt": build["date"], "builds": [build, older]}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8765)
    ap.add_argument("--no-downloads", action="store_true")
    ap.add_argument("--api-fail", action="store_true", help="answer /api/mods with 500")
    a = ap.parse_args()
    api = DevQueueServer(("127.0.0.1", 0), Path(tempfile.mkdtemp()))
    threading.Thread(target=api.serve_forever, daemon=True).start()
    downloads = None if a.no_downloads else json.dumps(mock_downloads()).encode()

    class H(SimpleHTTPRequestHandler):
        def __init__(self, *args, **kw):
            super().__init__(*args, directory=str(WEB), **kw)

        def log_message(self, *args):
            pass

        def body(self, code, data, ctype="application/json"):
            self.send_response(code)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def proxy(self, method):
            if a.api_fail and self.path.startswith("/api/mods"):
                return self.body(500, b'{"error":"mod registry unavailable"}')
            length = int(self.headers.get("Content-Length", "0") or 0)
            data = self.rfile.read(length) if length else None
            c = http.client.HTTPConnection("127.0.0.1", api.server_address[1])
            c.request(method, self.path, body=data, headers={"Content-Type": "application/json",
                                                              "Cf-Access-Authenticated-User-Email": "owner@example.org"})
            r = c.getresponse()
            self.body(r.status, r.read())

        def do_GET(self):
            path = self.path.split("?")[0]
            if path.startswith("/api/"):
                return self.proxy("GET")
            if path == "/status.json":
                return self.body(200, json.dumps(STATUS).encode())
            if path == "/downloads/index.json":
                return self.body(200, downloads) if downloads else self.body(404, b"{}")
            return super().do_GET()

        def do_PUT(self):
            self.proxy("PUT")

        def do_POST(self):
            self.proxy("POST")

    ThreadingHTTPServer(("127.0.0.1", a.port), H).serve_forever()


if __name__ == "__main__":
    main()
