#!/usr/bin/env python3
"""
Serves the SimpleBuilding test interface and lets it start test runs.

A page opened from disk can show the recorded history just fine, but it cannot
start a Gradle task - so the buttons need something behind them. This is that
something: a small local server that hands out ``testing/`` and exposes a
handful of endpoints for starting a run and following it while it happens.

It binds to 127.0.0.1 only. Starting arbitrary Gradle tasks is exactly the kind
of thing that must not be reachable from the network, and the loopback bind is
the guard - there is no authentication here and there should not need to be.

All the logic lives in run.py; this module imports it rather than keeping a
second copy, so the command line and the interface can never disagree about
what a run is.

Usage
    python tools/testrunner/serve.py                # http://127.0.0.1:8765
    python tools/testrunner/serve.py --port 9000
    python tools/testrunner/serve.py --no-open      # do not open a browser

Endpoints
    GET  /                  testing/index.html
    GET  /api/status        what is running right now, plus the tail of its output
    GET  /api/runs          the recorded runs, newest first
    GET  /api/catalogue     every registered test, per Minecraft line
    GET  /api/targets       the four targets
    POST /api/run           {"targets": [...] | null, "filter": null | "..."}
                            409 while another run is in flight
"""

from __future__ import annotations

import argparse
import json
import sys
import threading
import webbrowser
from collections import deque
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import run as runner  # noqa: E402  (needs the sys.path line above)

#: Enough to see what is happening without turning the page into a log viewer.
TAIL_LINES = 40

#: A request body larger than this is not a run request; refuse it rather than read it.
MAX_BODY_BYTES = 64 * 1024


class RunState:
    """The one run that may be in flight, and what it has said so far."""

    def __init__(self) -> None:
        self._lock = threading.Lock()
        self.running = False
        self.run_id: str | None = None
        self.started_at: str | None = None
        self.current_target: str | None = None
        self.filter: str | None = None
        self.tail: deque[str] = deque(maxlen=TAIL_LINES)
        self.last_result: dict | None = None
        self.error: str | None = None

    def start(self, targets: list[runner.Target], test_filter: str | None) -> bool:
        """Claims the slot and starts the worker. False when one is already running."""
        with self._lock:
            if self.running:
                return False
            self.running = True
            self.run_id = None
            self.started_at = runner.iso(runner.now_utc())
            self.current_target = None
            self.filter = test_filter
            self.tail.clear()
            self.error = None

        thread = threading.Thread(
            target=self._work, args=(targets, test_filter), name="testrun", daemon=True
        )
        thread.start()
        return True

    def _work(self, targets: list[runner.Target], test_filter: str | None) -> None:
        try:
            record = runner.execute(
                targets,
                test_filter,
                trigger="ui",
                timeout=runner.DEFAULT_TIMEOUT_SECONDS,
                on_line=lambda line: self.tail.append(line),
                on_target=self._on_target,
            )
            with self._lock:
                self.last_result = record
                self.run_id = record["runId"]
        except Exception as error:  # noqa: BLE001 - the thread must not die silently
            with self._lock:
                self.error = f"{type(error).__name__}: {error}"
            self.tail.append(f"ABBRUCH: {self.error}")
        finally:
            with self._lock:
                self.running = False
                self.current_target = None

    def _on_target(self, target: runner.Target) -> None:
        with self._lock:
            self.current_target = target.id
        self.tail.append(f"--- {target.label}")

    def snapshot(self) -> dict:
        with self._lock:
            return {
                "running": self.running,
                "runId": self.run_id,
                "startedAt": self.started_at,
                "target": self.current_target,
                "filter": self.filter,
                "tail": list(self.tail),
                "error": self.error,
                "lastRunId": (self.last_result or {}).get("runId"),
                "lastOk": (self.last_result or {}).get("ok"),
            }


STATE = RunState()


class Handler(SimpleHTTPRequestHandler):
    """Static files out of testing/, plus the handful of endpoints above."""

    def __init__(self, *args, **kwargs) -> None:
        super().__init__(*args, directory=str(runner.TESTING), **kwargs)

    # -- Antworten ---------------------------------------------------------

    def _json(self, payload: object, status: HTTPStatus = HTTPStatus.OK) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        # The data is regenerated after every run, so a cached answer would show
        # the previous run as if it were the current one.
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    # -- Routen ------------------------------------------------------------

    def do_GET(self) -> None:  # noqa: N802 - name comes from the base class
        route = self.path.split("?", 1)[0]
        if route == "/api/status":
            self._json(STATE.snapshot())
            return
        if route == "/api/runs":
            self._json(runner.load_runs())
            return
        if route == "/api/catalogue":
            self._json(runner.read_catalogue())
            return
        if route == "/api/targets":
            self._json([
                {
                    "id": t.id,
                    "label": t.label,
                    "loader": t.loader,
                    "mcLine": t.mc_line,
                    "gradleTask": t.gradle_task,
                }
                for t in runner.TARGETS
            ])
            return
        if route.startswith("/api/"):
            self._json({"error": "unbekannter Endpunkt"}, HTTPStatus.NOT_FOUND)
            return
        super().do_GET()

    def do_POST(self) -> None:  # noqa: N802
        if self.path.split("?", 1)[0] != "/api/run":
            self._json({"error": "unbekannter Endpunkt"}, HTTPStatus.NOT_FOUND)
            return

        try:
            length = int(self.headers.get("Content-Length") or 0)
        except ValueError:
            length = 0
        if length > MAX_BODY_BYTES:
            self._json({"error": "Anfrage zu gross"}, HTTPStatus.REQUEST_ENTITY_TOO_LARGE)
            return

        try:
            payload = json.loads(self.rfile.read(length) or b"{}")
        except (json.JSONDecodeError, UnicodeDecodeError) as error:
            self._json({"error": f"kein gueltiges JSON: {error}"}, HTTPStatus.BAD_REQUEST)
            return
        if not isinstance(payload, dict):
            self._json({"error": "erwartet wird ein JSON-Objekt"}, HTTPStatus.BAD_REQUEST)
            return

        wanted = payload.get("targets")
        if wanted in (None, [], "all"):
            targets = list(runner.TARGETS)
        elif isinstance(wanted, list) and all(isinstance(x, str) for x in wanted):
            unknown = [x for x in wanted if x not in runner.BY_ID]
            if unknown:
                self._json({"error": "unbekannte Ziele: " + ", ".join(unknown)}, HTTPStatus.BAD_REQUEST)
                return
            targets = [runner.BY_ID[x] for x in wanted]
        else:
            self._json({"error": "targets muss eine Liste von Ids sein oder null"}, HTTPStatus.BAD_REQUEST)
            return

        test_filter = payload.get("filter") or None
        if test_filter is not None and not isinstance(test_filter, str):
            self._json({"error": "filter muss eine Zeichenkette sein oder null"}, HTTPStatus.BAD_REQUEST)
            return

        if not STATE.start(targets, test_filter):
            snapshot = STATE.snapshot()
            self._json(
                {"error": "es laeuft schon ein Testlauf", "running": snapshot},
                HTTPStatus.CONFLICT,
            )
            return

        self._json({"started": True, "targets": [t.id for t in targets], "filter": test_filter})

    # -- Ruhe im Log -------------------------------------------------------

    def log_message(self, fmt: str, *args) -> None:
        """Keeps the terminal readable: the test output is the interesting part."""
        if self.path.startswith("/api/status"):
            return
        sys.stderr.write("  %s\n" % (fmt % args))


def main(argv: list[str] | None = None) -> int:
    runner.force_utf8_stdout()
    parser = argparse.ArgumentParser(description="Serves the SimpleBuilding test interface.")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--no-open", action="store_true", help="do not open a browser")
    args = parser.parse_args(argv)

    if not (runner.TESTING / "index.html").exists():
        print(f"  testing/index.html fehlt unter {runner.TESTING}")
        return 1

    # So the page has something to show before the first run of this session.
    runner.refresh_ui_data()

    address = ("127.0.0.1", args.port)
    try:
        server = ThreadingHTTPServer(address, Handler)
    except OSError as error:
        print(f"  Port {args.port} laesst sich nicht belegen: {error}")
        return 1

    url = f"http://127.0.0.1:{args.port}/"
    print(f"  Test-Oberflaeche auf {url}")
    print("  Beenden mit Strg+C")
    if not args.no_open:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print()
        print("  beendet")
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
