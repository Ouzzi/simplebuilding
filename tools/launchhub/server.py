"""
Launch- und Testzentrale (Launch & Test Hub) - local dev server for SimpleBuilding.

    python tools/launchhub/server.py                 http://127.0.0.1:8771, opens the browser
    python tools/launchhub/server.py --no-browser --port 8771
    SB_HUB_DRY_RUN=1 python tools/launchhub/server.py   nothing is started, commands are only echoed
    SB_HUB_DRY_RUN=sim ...                             like 1, but jobs run a harmless simulated process

Starts clients/servers, runs tests in a gate worktree, shows the run history and starts AI fix jobs.
Python standard library only. Binds to 127.0.0.1 and rejects non-loopback Host headers. GET never
changes anything; every POST needs the header X-Hub-Client: 1 and is validated against allow-lists.
Details: docs/LAUNCHHUB.md
"""

from __future__ import annotations

import argparse
import json
import mimetypes
import sys
import threading
import traceback
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, unquote, urlparse

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from hub import paths  # noqa: E402
from hub.api import Hub, HubError  # noqa: E402

MAX_BODY = 512 * 1024
LOOPBACK = ("127.0.0.1", "localhost", "[::1]", "::1")


def host_ok(header: str | None) -> bool:
    """Only loopback names pass. Guards against DNS rebinding as well as against a non-local bind."""
    host = (header or "").strip()
    if host.startswith("["):
        host = host.split("]")[0] + "]"
    else:
        host = host.split(":")[0]
    return host in LOOPBACK


def make_handler(hub: Hub):
    class Handler(BaseHTTPRequestHandler):
        server_version = "Launchhub/1"

        def log_message(self, fmt, *args):
            if len(args) >= 2 and str(args[1])[:1] in ("4", "5"):
                sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

        def _send(self, status, body: bytes, ctype: str, extra=None):
            self.send_response(status)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.send_header("Referrer-Policy", "no-referrer")
            for key, value in (extra or {}).items():
                self.send_header(key, value)
            self.end_headers()
            self.wfile.write(body)

        def _json(self, data, status=200):
            self._send(status, json.dumps(data, ensure_ascii=False, allow_nan=False).encode("utf-8"),
                       "application/json; charset=utf-8")

        def _error(self, status, message, extra=None):
            self._json(dict({"error": message}, **(extra or {})), status)

        def _file(self, path: Path, root: Path):
            resolved = path.resolve()
            if not paths.is_within(resolved, root):
                return self._error(403, "Path outside the allowed folder.")
            if not resolved.is_file():
                return self._error(404, "File not found.")
            ctype = mimetypes.guess_type(resolved.name)[0] or "application/octet-stream"
            if ctype.startswith("text/") or ctype in ("application/javascript", "application/json"):
                ctype += "; charset=utf-8"
            self._send(200, resolved.read_bytes(), ctype)

        def do_GET(self):
            if not host_ok(self.headers.get("Host")):
                return self._error(403, "Local access only.")
            parsed = urlparse(self.path)
            path, query = unquote(parsed.path), parse_qs(parsed.query)
            try:
                if path in ("/", "/index.html"):
                    return self._file(paths.STATIC_DIR / "index.html", paths.STATIC_DIR)
                if path.startswith("/static/"):
                    return self._file(paths.STATIC_DIR / path[len("/static/"):], paths.STATIC_DIR)
                if path.startswith("/wiki/assets/"):
                    return self._file(paths.REPO / "wiki" / path[len("/wiki/"):], paths.REPO / "wiki" / "assets")
                kind, payload = hub.route_get(path, query)
                if kind == "text":
                    extra = {}
                    if query.get("download", ["0"])[0] == "1":
                        name = path.rsplit("/", 1)[-1] if path.endswith(".md") else "log.txt"
                        extra["Content-Disposition"] = f'attachment; filename="{name}"'
                    ctype = "text/markdown; charset=utf-8" if path.endswith(".md") else "text/plain; charset=utf-8"
                    return self._send(200, payload.encode("utf-8"), ctype, extra)
                return self._json(payload)
            except HubError as error:
                return self._error(error.status, error.message, error.extra)
            except Exception as error:  # pragma: no cover
                traceback.print_exc()
                return self._error(500, f"Server error: {error}")

        def do_POST(self):
            if not host_ok(self.headers.get("Host")):
                return self._error(403, "Local access only.")
            if self.headers.get("X-Hub-Client") != "1":
                return self._error(403, "Only the hub page may do this (header X-Hub-Client missing).")
            if "application/json" not in (self.headers.get("Content-Type") or ""):
                return self._error(415, "Send JSON (Content-Type: application/json).")
            try:
                length = int(self.headers.get("Content-Length") or 0)
            except ValueError:
                return self._error(400, "Invalid Content-Length.")
            if length > MAX_BODY:
                return self._error(413, "Request too large.")
            try:
                body = json.loads(self.rfile.read(length).decode("utf-8") or "{}")
            except (UnicodeDecodeError, json.JSONDecodeError) as error:
                return self._error(400, f"Not valid JSON: {error}")
            if not isinstance(body, dict):
                return self._error(400, "Expected a JSON object.")
            try:
                return self._json(hub.route_post(urlparse(self.path).path, body))
            except HubError as error:
                return self._error(error.status, error.message, error.extra)
            except (targets_error_types()) as error:
                return self._error(400, str(error))
            except Exception as error:  # pragma: no cover
                traceback.print_exc()
                return self._error(500, f"Server error: {error}")

    return Handler


def targets_error_types():
    from hub import ai, targets
    return (targets.TargetError, ai.ProviderError, TypeError, KeyError, ValueError)


def start(hub: Hub, host: str, port: int, tries: int = 10) -> ThreadingHTTPServer:
    last = None
    for candidate in range(port, port + tries):
        try:
            server = ThreadingHTTPServer((host, candidate), make_handler(hub))
            server.daemon_threads = True
            return server
        except OSError as error:
            last = error
    raise SystemExit(f"No free port from {port}: {last}")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=8771)
    parser.add_argument("--host", default="127.0.0.1", help="loopback only")
    parser.add_argument("--no-browser", action="store_true")
    args = parser.parse_args(argv)
    if args.host not in ("127.0.0.1", "localhost", "::1"):
        raise SystemExit("The hub has no login and binds to loopback only.")
    hub = Hub()
    server = start(hub, args.host, args.port)
    url = f"http://127.0.0.1:{server.server_address[1]}/"
    print(f"Launch- und Testzentrale on {url}   (stop: Ctrl+C; clients started from here keep running)")
    print(f"  repo {hub.repo}\n  runs {hub.runs_dir()}")
    from hub import settings
    if settings.dry_run():
        print("  DRY RUN: nothing will be started")
    if not args.no_browser:
        threading.Timer(0.4, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
