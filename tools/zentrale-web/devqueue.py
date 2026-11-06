#!/usr/bin/env python3
import argparse
import base64
import binascii
import hashlib
import json
import os
import re
import shutil
import threading
import time
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
import sys
from pathlib import Path
from urllib.parse import urlparse

import modselect

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "textures"))
import editor_save  # noqa: E402  (TX22-5: one set of rules for the endpoint and the pickup)

HEADER_NAME = "Cf-Access-Authenticated-User-Email"
MAX_BODY = 256 * 1024
MAX_COMMENT = 4000
MAX_TARGETS = 500
CATEGORIES = {"textures", "code"}
KINDS = {"texture", "recipe", "note", "suggestion"}
# "Änderung vorschlagen" (a-wikiux06): images go along as attachments, stored beside the queue, never in the repo
MAX_QUEUE_BODY = 12 * 1024 * 1024
MAX_ATTACHMENTS = 5
MAX_ATTACHMENT = 2 * 1024 * 1024
MAX_ATTACHMENTS_TOTAL = 8 * 1024 * 1024
MAX_REF = 2 * 1024 * 1024
IMAGE_TYPES = {"image/png": "png", "image/jpeg": "jpg", "image/webp": "webp"}
CONTENT_TYPES = {"png": "image/png", "jpg": "image/jpeg", "webp": "image/webp"}
QUEUE_ID = re.compile(r"^[0-9]{6,40}$")
FILE_NAME = re.compile(r"^(?:ref-)?[0-9]{1,2}\.(?:png|jpg|webp)$")
TEXTURE_ID = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def image_kind(data: bytes) -> str | None:
    """File type from the magic bytes (the declared type must match)."""
    if data[:8] == b"\x89PNG\r\n\x1a\n":
        return "png"
    if data[:3] == b"\xff\xd8\xff":
        return "jpg"
    if data[:4] == b"RIFF" and data[8:12] == b"WEBP":
        return "webp"
    return None


def clean_label(value: object, limit: int = 200) -> str:
    if not isinstance(value, str):
        return ""
    return "".join(c for c in value if ord(c) >= 32 and ord(c) != 127).strip()[:limit]


def check_attachments(raw: object) -> list[tuple[str, bytes, str]]:
    """[(extension, bytes, cleaned original name)] or ValueError; never uses a client path."""
    if raw is None:
        return []
    if not isinstance(raw, list) or len(raw) > MAX_ATTACHMENTS:
        raise ValueError(f"at most {MAX_ATTACHMENTS} images")
    out, total = [], 0
    for item in raw:
        if not isinstance(item, dict) or item.get("type") not in IMAGE_TYPES or not isinstance(item.get("data"), str):
            raise ValueError("image must be png, jpeg or webp")
        if len(item["data"]) > MAX_ATTACHMENT * 4 // 3 + 8:
            raise ValueError("image larger than 2 MB")
        try:
            data = base64.b64decode(item["data"], validate=True)
        except (binascii.Error, ValueError) as e:
            raise ValueError("image unreadable") from e
        ext = IMAGE_TYPES[item["type"]]
        if not data or len(data) > MAX_ATTACHMENT:
            raise ValueError("image larger than 2 MB")
        if image_kind(data) != ext:
            raise ValueError("image content does not match its type")
        total += len(data)
        if total > MAX_ATTACHMENTS_TOTAL:
            raise ValueError("images larger than 8 MB together")
        name = re.sub(r"[\\/]+", "_", clean_label(item.get("name"), 120))
        out.append((ext, data, name))
    return out
STATUSES = {"open", "started", "done", "dropped"}
RUN_STATES = {"new", "picked", "done", "failed"}


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
        if status == 204:
            self.send_response(204)
            self.end_headers()
            return
        body = b"{}" if data is None else json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def read_json(self, limit: int = MAX_BODY) -> dict:
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError as e:
            raise ValueError("invalid length") from e
        if length < 0 or length > limit:
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
        if path == "/api/mods":
            self.handle_get_mods(self.headers.get(HEADER_NAME))
            return
        if path == "/api/textures":
            self.send_json(200, self.server.texture_saves())
            return
        if path.startswith("/api/textures/"):
            save = self.server.texture_save(path[len("/api/textures/"):])
            self.send_json(200 if save else 404, save or {"error": "not found"})
            return
        parts = path.split("/")
        if len(parts) == 6 and parts[:3] == ["", "api", "queue"] and parts[4] == "files":
            self.send_attachment(parts[3], parts[5])
            return
        self.send_json(404, {"error": "not found"})

    def send_attachment(self, qid: str, name: str):
        """An image of a suggestion (or its reference texture); names are the server's own (FILE_NAME)."""
        if not QUEUE_ID.match(qid) or not FILE_NAME.match(name):
            self.send_json(404, {"error": "not found"})
            return
        path = self.server.attachments_dir / qid / name
        try:
            data = path.read_bytes()
        except OSError:
            self.send_json(404, {"error": "not found"})
            return
        self.send_response(200)
        self.send_header("Content-Type", CONTENT_TYPES[name.rsplit(".", 1)[1]])
        self.send_header("Content-Length", str(len(data)))
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Cache-Control", "private, max-age=3600")
        self.end_headers()
        self.wfile.write(data)

    def do_POST(self):
        email = self.check_auth()
        if email is None:
            return
        parsed = urlparse(self.path)
        path = parsed.path
        try:
            payload = self.read_json(editor_save.MAX_BODY if path == "/api/textures" else MAX_QUEUE_BODY if path == "/api/queue" else MAX_BODY)
        except ValueError as e:
            self.send_json(400 if str(e) != "body too large" else 413, {"error": str(e)})
            return
        if path == "/api/textures":
            self.handle_texture_save(payload, email)
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
        if path.startswith("/api/runs/"):
            try:
                payload = self.read_json()
            except ValueError as e:
                self.send_json(400, {"error": str(e)})
                return
            self.handle_patch_run(path[len("/api/runs/") :], payload)
            return
        if path.startswith("/api/queue/"):
            qid = path[len("/api/queue/") :]
            try:
                payload = self.read_json()
            except ValueError as e:
                self.send_json(400, {"error": str(e)})
                return
            self.handle_patch_queue(qid, payload)
            return
        if path.startswith("/api/textures/"):
            try:
                payload = self.read_json()
            except ValueError as e:
                self.send_json(400, {"error": str(e)})
                return
            self.handle_patch_texture(path[len("/api/textures/"):], payload)
            return
        self.send_json(404, {"error": "not found"})

    def do_PUT(self):
        email = self.check_auth()
        if email is None:
            return
        if urlparse(self.path).path != "/api/mods/selection":
            self.send_json(404, {"error": "not found"})
            return
        try:
            payload = self.read_json()
        except ValueError as e:
            self.send_json(400, {"error": str(e)})
            return
        try:
            nodes = modselect.catalog(self.server.repo)
            off = modselect.normalize_off(payload.get("off"), nodes)
        except ValueError as e:
            self.send_json(400, {"error": str(e)})
            return
        except (OSError, KeyError):
            self.send_json(500, {"error": "mod registry unavailable"})
            return
        with self.server.lock:
            atomic_write_json(self.server.selection_path(email), {"by": email, "off": off,
                                                                  "updatedAt": int(time.time() * 1000)})
        self.handle_get_mods(email)

    def handle_get_mods(self, email: str):
        """Catalog tree + this user's stored selection + the resolved state (locks, launch data)."""
        try:
            nodes = modselect.catalog(self.server.repo)
            registry = modselect.load_registry(self.server.repo)
        except (OSError, KeyError, ValueError):
            self.send_json(500, {"error": "mod registry unavailable"})
            return
        stored = {}
        path = self.server.selection_path(email)
        if path.exists():
            try:
                with open(path, "r", encoding="utf-8") as f:
                    stored = json.load(f)
            except Exception:
                stored = {}
        known = {n["id"] for n in nodes}
        off = [i for i in stored.get("off", []) if i in known]  # mods removed since then are dropped
        try:
            off = modselect.normalize_off(off, nodes)
        except ValueError:
            off = []
        state = modselect.resolve(nodes, off)
        plan = modselect.launch_plan(nodes, off, registry)
        self.send_json(200, {"nodes": nodes, "off": off, "state": state, "launch": plan,
                             "updatedAt": stored.get("updatedAt")})

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
        if kind != "suggestion" and (payload.get("attachments") or payload.get("object")):
            self.send_json(400, {"error": "attachments only with a suggestion"})
            return
        try:
            images = check_attachments(payload.get("attachments"))
        except ValueError as e:
            self.send_json(400, {"error": str(e)})
            return
        obj = payload.get("object")
        if obj is not None and (not isinstance(obj, dict) or not isinstance(obj.get("id"), str) or len(obj["id"]) > 200):
            self.send_json(400, {"error": "invalid object"})
            return
        if kind == "suggestion" and not comment.strip() and not images:
            self.send_json(400, {"error": "text or image needed"})
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
        if kind == "suggestion":
            entry["object"] = {"id": clean_label(obj.get("id")), "label": clean_label(obj.get("label"))} if obj else None
            folder = self.server.attachments_dir / entry["id"]
            stored, refs = [], []
            for n, (ext, data, name) in enumerate(images, 1):
                folder.mkdir(parents=True, exist_ok=True)
                (folder / f"{n}.{ext}").write_bytes(data)
                stored.append({"file": f"{n}.{ext}", "type": CONTENT_TYPES[ext], "bytes": len(data), "name": name})
            for n, t in enumerate(targets[:20], 1):  # the current texture as reference, read by id from this checkout
                ref = self.server.reference_texture(t["id"])
                if ref is None:
                    continue
                folder.mkdir(parents=True, exist_ok=True)
                (folder / f"ref-{n}.png").write_bytes(ref)
                refs.append({"id": t["id"], "file": f"ref-{n}.png"})
            entry["attachments"] = stored
            entry["refs"] = refs
            entry["files"] = f"attachments/{entry['id']}/"  # under the data folder (runs keep the entry)
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
                        if s not in STATUSES:
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

    def handle_patch_run(self, run_id: str, payload: dict):
        """The pickup marks a run picked/done/failed and may attach a note (log, branch)."""
        state = payload.get("state")
        note = payload.get("note", "")
        if state not in RUN_STATES or not isinstance(note, str) or len(note) > MAX_COMMENT:
            self.send_json(400, {"error": "invalid state"})
            return
        run_path = self.server.runs_dir / f"{run_id}.json"
        if "/" in run_id or ".." in run_id or not run_path.is_file():
            self.send_json(404, {"error": "not found"})
            return
        with self.server.lock:
            with open(run_path, "r", encoding="utf-8") as f:
                run = json.load(f)
            run["state"] = state
            run["note"] = note
            run["updatedAt"] = int(time.time() * 1000)
            atomic_write_json(run_path, run)
            self.server.refresh_runs()
        self.send_json(200, run)

    def handle_texture_save(self, payload: dict, email: str):
        """TX22-5: the pixel editor's "Save to repo". Only listed own textures (wiki/data/textures.js of this checkout),
        checked by tools/textures/editor_save.py; the pickup on the agent host commits it on a tex-* branch."""
        try:
            save = editor_save.check_save(payload, editor_save.load_whitelist(self.server.repo))
        except (editor_save.SaveError, OSError) as e:
            self.send_json(400, {"error": str(e)})
            return
        now = int(time.time() * 1000)
        with self.server.lock:
            sid = str(max(now, self.server.last_save_id + 1))
            self.server.last_save_id = int(sid)
            record = dict(save, sid=sid, by=email, createdAt=now, updatedAt=now, state="new", note="", branch="")
            atomic_write_json(self.server.textures_dir / f"{sid}.json", record)
        self.send_json(201, {k: v for k, v in record.items() if k != "layers"})

    def handle_patch_texture(self, sid: str, payload: dict):
        """The pickup reports picked/ready/failed with a note and the branch."""
        state, note, branch = payload.get("state"), payload.get("note", ""), payload.get("branch", "")
        if state not in editor_save.STATES or not isinstance(note, str) or len(note) > MAX_COMMENT \
                or not isinstance(branch, str) or len(branch) > 200:
            self.send_json(400, {"error": "invalid state"})
            return
        with self.server.lock:
            record = self.server.texture_save(sid)
            if record is None:
                self.send_json(404, {"error": "not found"})
                return
            record.update(state=state, note=note, updatedAt=int(time.time() * 1000))
            if branch:
                record["branch"] = branch
            atomic_write_json(self.server.textures_dir / f"{sid}.json", record)
        self.send_json(200, {k: v for k, v in record.items() if k != "layers"})

    def handle_delete_queue(self, qid: str):
        with self.server.lock:
            for i, item in enumerate(self.server.queue):
                if str(item["id"]) == str(qid):
                    self.server.queue.pop(i)
                    self.server.save_queue()
                    if QUEUE_ID.match(str(qid)):
                        shutil.rmtree(self.server.attachments_dir / str(qid), ignore_errors=True)
                    self.send_json(204, None)
                    return
            self.send_json(404, {"error": "not found"})

    def handle_create_run(self, payload: dict):
        category = payload.get("category")
        if category not in CATEGORIES and category != "all":
            self.send_json(400, {"error": "invalid category"})
            return
        now = time.localtime()
        run_id = time.strftime("%Y%m%d-%H%M%S", now)
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
            if not entries:
                self.send_json(200, {"id": None, "entries": 0})
                return
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
        self.repo = modselect.REPO
        self.selections_dir = data_dir / "selections"
        self.textures_dir = data_dir / "textures"  # editor saves, apart from runs/ (those go to Claude)
        self.textures_dir.mkdir(parents=True, exist_ok=True)
        self.attachments_dir = data_dir / "attachments"  # images of suggestions (+ reference textures)
        self.last_save_id = 0
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

    def selection_path(self, email: str) -> Path:
        """One file per user; the name is a hash so addresses never become paths."""
        return self.selections_dir / (hashlib.sha256(email.strip().lower().encode("utf-8")).hexdigest()[:32] + ".json")

    def reference_texture(self, tid: str) -> bytes | None:
        """Current PNG of a texture by id: own ones from wiki/data/textures.js, Vanilla from the dev-only folder."""
        if not isinstance(tid, str) or not TEXTURE_ID.match(tid) or ".." in tid:
            return None
        wiki = (Path(self.repo) / "wiki").resolve()
        try:
            entry = editor_save.load_whitelist(Path(self.repo)).get(tid)
        except (OSError, ValueError, editor_save.SaveError):
            entry = None
        rel = entry.get("file") if entry else (f"assets/vanilla/{tid.split(':', 1)[1]}.png" if tid.startswith("minecraft:") else None)
        if not isinstance(rel, str):
            return None
        path = (wiki / rel).resolve()
        if not path.is_relative_to(wiki / "assets") or path.suffix != ".png":
            return None
        try:
            data = path.read_bytes()
        except OSError:
            return None
        return data if len(data) <= MAX_REF and image_kind(data) == "png" else None

    def texture_save(self, sid: str) -> dict | None:
        if not editor_save.SAVE_ID.match(sid or ""):
            return None
        path = self.textures_dir / f"{sid}.json"
        try:
            with open(path, "r", encoding="utf-8") as f:
                return json.load(f)
        except (OSError, ValueError):
            return None

    def texture_saves(self) -> list:
        """Newest first, without the layers (the editor and the pickup only need the state)."""
        out = []
        for p in sorted(self.textures_dir.glob("*.json"), reverse=True)[:200]:
            save = self.texture_save(p.stem)
            if save:
                save.pop("layers", None)
                out.append(save)
        return out

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
    parser.add_argument("--repo", default=str(modselect.REPO), help="checkout with modules/modules.json")
    args = parser.parse_args()

    data_dir = Path(args.data).resolve()
    server = DevQueueServer((args.bind, args.port), data_dir)
    server.repo = Path(args.repo).resolve()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
