#!/usr/bin/env python3
import argparse
import json
import os
import threading
import time
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from pathlib import Path
from urllib.parse import urlparse

HEADER_NAME = "Cf-Access-Authenticated-User-Email"
MAX_BODY = 256 * 1024
MAX_COMMENT = 4000
MAX_TARGETS = 500
CATEGORIES = {"textures", "code"}
KINDS = {"texture", "recipe", "note"}


def atomic_write_json(path: Path, data: dict | list) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_suffix(path.suffix + ".tmp")
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
    os.replace(tmp_path, path)


class Handler(BaseHTTPRequestHandler):
    server: "DevQueueServer"

    def log_message(self, fmt, *args):
        return

    def send_json(self, status: int, data: dict | list | None = None):
        body = b"{}" if data is None else json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def read_json(self) -> dict:
        length = int(self.headers.get("Content-Length", "0"))
        if length > MAX_BODY:
            raise ValueError("body too large")
        data = self.rfile.read(length) if length > 0 else b"{}"
        try:
            return json.loads(data.decode("utf-8")) if data else {}
        except Exception as e:
            raise ValueError("invalid json") from e

    def check_auth(self) -> str | None:
        email = self.headers.get(HEADER_NAME)
        if not email:
            self.send_json(403, {"error": "forbidden"})
            return None
        return email

    def do_GET(self):
        if self.check_auth() is None:
            return
        parsed = urlparse(self.path)
        path = parsed.path
        if path == "/api/queue":
            with self.server.lock:
                self.send_json(200, self.server.queue)
            return
        if path == "/api/runs":
            with self.server.lock:
                self.send_json(200, self.server.runs_list())
            return
        self.send_json(404, {"error": "not found"})

    def do_POST(self):
        email = self.check_auth()
        if email is None:
            return
        parsed = urlparse(self.path)
        path = parsed.path
        try:
            payload = self.read_json()
        except ValueError as e:
            self.send_json(400, {"error": str(e)})
            return
        if path == "/api/queue":
            self.handle_create_queue(payload, email)
            return
        if path == "/api/run":
            self.handle_create_run(payload)
            return
        self.send_json(404, {"error": "not found"})

    def do_PATCH(self):
        email = self.check_auth()
        if email is None:
            return
        parsed = urlparse(self.path)
        path = parsed.path
        if path.startswith("/api/queue/"):
            qid = path[len("/api/queue/") :]
            try:
                payload = self.read_json()
            except ValueError as e:
                self.send_json(400, {"error": str(e)})
                return
            self.handle_patch_queue(qid, payload)
            return
        self.send_json(404, {"error": "not found"})

    def do_DELETE(self):
        email = self.check_auth()
        if email is None:
            return
        parsed = urlparse(self.path)
        path = parsed.path
        if path.startswith("/api/queue/"):
            qid = path[len("/api/queue/") :]
            self.handle_delete_queue(qid)
            return
        self.send_json(404, {"error": "not found"})

    def handle_create_queue(self, payload: dict, email: str):
        category = payload.get("category")
        kind = payload.get("kind")
        targets = payload.get("targets")
        comment = payload.get("comment", "")
        if category not in CATEGORIES:
            self.send_json(400, {"error": "invalid category"})
            return
        if kind not in KINDS:
            self.send_json(400, {"error": "invalid kind"})
            return
        if not isinstance(targets, list) or len(targets) == 0 or len(targets) > MAX_TARGETS:
            self.send_json(400, {"error": "invalid targets"})
            return
        if not isinstance(comment, str) or len(comment) > MAX_COMMENT:
            self.send_json(400, {"error": "invalid comment"})
            return
        if kind == "texture" and category != "textures":
            self.send_json(400, {"error": "invalid kind for category"})
            return
        if (kind == "recipe" or kind == "note") and category != "code":
            self.send_json(400, {"error": "invalid kind for category"})
            return
        for t in targets:
            if not isinstance(t, dict) or not all(k in t and isinstance(t[k], str) for k in ("id", "label", "path")):
                self.send_json(400, {"error": "invalid target"})
                return
        now = int(time.time() * 1000)
        entry = {
            "id": str(int(time.time() * 1000000 + threading.get_ident())),
            "category": category,
            "kind": kind,
            "targets": list(targets),
            "comment": comment,
            "by": email,
            "createdAt": now,
            "updatedAt": now,
            "status": "open",
            "result": "",
        }
        with self.server.lock:
            self.server.queue.append(entry)
            self.server.save_queue()
        self.send_json(201, entry)

    def handle_patch_queue(self, qid: str, payload: dict):
        with self.server.lock:
            for item in self.server.queue:
                if str(item["id"]) == str(qid):
                    if "comment" in payload:
                        c = payload["comment"]
                        if not isinstance(c, str) or len(c) > MAX_COMMENT:
                            self.send_json(400, {"error": "invalid comment"})
                            return
                        item["comment"] = c
                    if "status" in payload:
                        s = payload["status"]
                        if not isinstance(s, str):
                            self.send_json(400, {"error": "invalid status"})
                            return
                        item["status"] = s
                    if "result" in payload:
                        r = payload["result"]
                        if not isinstance(r, str) or len(r) > MAX_COMMENT:
                            self.send_json(400, {"error": "invalid result"})
                            return
                        item["result"] = r
                    item["updatedAt"] = int(time.time() * 1000)
                    self.server.save_queue()
                    self.send_json(200, item)
                    return
            self.send_json(404, {"error": "not found"})

    def handle_delete_queue(self, qid: str):
        with self.server.lock:
            for i, item in enumerate(self.server.queue):
                if str(item["id"]) == str(qid):
                    self.server.queue.pop(i)
                    self.server.save_queue()
                    self.send_json(204, None)
                    return
            self.send_json(404, {"error": "not found"})

    def handle_create_run(self, payload: dict):
        category = payload.get("category")
        if category not in CATEGORIES and category != "all":
            self.send_json(400, {"error": "invalid category"})
            return
        now = time.localtime()
        run_id = time.strftime("%Y%m%d-%H%M", now)
        if category == "textures":
            run_id = f"{run_id}-textures"
        elif category == "code":
            run_id = f"{run_id}-code"
        else:
            run_id = f"{run_id}-all"
        entries = []
        with self.server.lock:
            for item in self.server.queue:
                if item.get("status") != "open":
                    continue
                if category == "all":
                    entries.append(item)
                elif item.get("category") == category:
                    entries.append(item)
            for item in entries:
                item["status"] = "started"
                item["run"] = run_id
            run_data = {
                "id": run_id,
                "category": category,
                "createdAt": int(time.time() * 1000),
                "entries": [dict(e) for e in entries],
                "state": "new",
            }
            run_path = self.server.runs_dir / f"{run_id}.json"
            atomic_write_json(run_path, run_data)
            self.server.save_queue()
            self.server.refresh_runs()
        self.send_json(201, {"id": run_id, "entries": len(entries)})


class DevQueueServer(ThreadingHTTPServer):
    def __init__(self, server_address, data_dir: Path):
        super().__init__(server_address, Handler)
        self.data_dir = data_dir
        self.queue_path = data_dir / "queue.json"
        self.runs_dir = data_dir / "runs"
        self.runs_dir.mkdir(parents=True, exist_ok=True)
        self.lock = threading.Lock()
        self.queue = []
        self._runs_cache = []
        self.load_queue()
        self.refresh_runs()

    def load_queue(self):
        if self.queue_path.exists():
            try:
                with open(self.queue_path, "r", encoding="utf-8") as f:
                    self.queue = json.load(f)
            except Exception:
                self.queue = []
        else:
            self.queue = []

    def save_queue(self):
        atomic_write_json(self.queue_path, self.queue)

    def runs_list(self) -> list:
        return list(self._runs_cache)

    def refresh_runs(self):
        items = []
        for p in sorted(self.runs_dir.glob("*.json"), reverse=True):
            try:
                with open(p, "r", encoding="utf-8") as f:
                    items.append(json.load(f))
            except Exception:
                pass
        self._runs_cache = items


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8090)
    parser.add_argument("--bind", default="127.0.0.1")
    parser.add_argument("--data", default=".")
    args = parser.parse_args()

    data_dir = Path(args.data).resolve()
    server = DevQueueServer((args.bind, args.port), data_dir)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
