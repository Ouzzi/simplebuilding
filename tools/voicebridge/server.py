"""Run: python tools/voicebridge/server.py [--port 8772] [--tailscale]."""
from __future__ import annotations

import argparse
from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import mimetypes
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import threading
import time
from urllib.parse import urlsplit

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parents[2]))
from tools.voicebridge.core import (Audit, Confirmation, RateLimit, apply_edits, authenticated,
                                    command, host_allowed, load_projects, prepare_edits,
                                    spoken_answer, tailscale_ip, validate_host)
from tools.voicebridge.providers import Runner
from tools.voicebridge.adapters import StubSTT, StubTTS, WhisperSTT, PiperTTS

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]
MAX_BODY = 1_500_000
SHELL = {"/": "index.html", "/app.js": "app.js", "/style.css": "style.css",
         "/manifest.json": "manifest.json", "/sw.js": "sw.js", "/icon.svg": "icon.svg",
         "/icon-192.png": "icon-192.png", "/icon-512.png": "icon-512.png"}


def detect_tailscale():
    try:
        result = subprocess.run(["tailscale", "ip", "-4"], capture_output=True, text=True, timeout=5, shell=False)
        ip = result.stdout.strip().splitlines()[0]
        if result.returncode == 0 and tailscale_ip(ip):
            return ip
    except (OSError, subprocess.SubprocessError, IndexError):
        pass
    return None


def detect_name():
    try:
        result = subprocess.run(["tailscale", "status", "--json"], capture_output=True, text=True, timeout=5, shell=False)
        name = json.loads(result.stdout)["Self"]["DNSName"].rstrip(".").lower()
        if re.fullmatch(r"[a-z0-9-]+\.[a-z0-9.-]+\.ts\.net", name):
            return name
    except (OSError, ValueError, KeyError, subprocess.SubprocessError):
        pass
    return None


def read_records(root):
    folder = Path(root) / "testing/runs"
    records = []
    for path in sorted(folder.glob("*.json"), reverse=True)[:20]:
        try:
            if path.stat().st_size <= 2_000_000:
                record = json.loads(path.read_text(encoding="utf-8"))
                if isinstance(record, dict):
                    records.append(record)
        except (OSError, ValueError):
            pass
    return records


def fixed_command(argv, root):
    result = subprocess.run(argv, cwd=root, capture_output=True, text=True, encoding="utf-8",
                            errors="replace", timeout=15, shell=False,
                            env=dict(os.environ, GIT_OPTIONAL_LOCKS="0"))
    if result.returncode:
        return "Nicht verfügbar"
    return result.stdout[:16000].strip()


class Bridge:
    def __init__(self, directory=HERE, repo=REPO, projects_file=None):
        self.directory, self.repo = Path(directory), Path(repo)
        config = Path(projects_file) if projects_file else self.directory / "projects.json"
        if not config.exists():
            config = HERE / "projects.example.json"
        self.projects = load_projects(config, self.repo)
        self.selected = next(iter(self.projects))
        self.audit = Audit(self.directory / "audit.log")
        self.runner = Runner()
        self.confirm = Confirmation()
        self.turn_lock = threading.Lock()
        self.state_lock = threading.RLock()
        self.stopped = False
        self.generation = 0
        self.sessions = {}
        self.histories = {name: deque(maxlen=100) for name in self.projects}
        self.conversations = {name: 0 for name in self.projects}
        self.conversation_ids = {name: secrets.token_hex(12) for name in self.projects}
        state = self.directory / "sessions.json"
        if state.exists():
            try:
                saved = json.loads(state.read_text(encoding="utf-8"))
                for name, row in saved.items():
                    if name not in self.projects or not isinstance(row, dict) or row.get("identity") != self.identity(name):
                        continue
                    session = row.get("session")
                    if isinstance(session, str) and session != "dry-session" and re.fullmatch(r"[a-zA-Z0-9_-]{1,160}", session):
                        self.sessions[name] = session
                    conversation = row.get("conversation")
                    if isinstance(conversation, str) and re.fullmatch(r"[a-f0-9]{24}", conversation):
                        self.conversation_ids[name] = conversation
            except (OSError, ValueError, AttributeError):
                pass
        history = self.directory / "history.jsonl"
        if history.exists() and history.stat().st_size < 20_000_000:
            for line in history.read_text(encoding="utf-8").splitlines():
                try:
                    record = json.loads(line)
                    if record["project"] in self.histories:
                        self.histories[record["project"]].append(record)
                except (ValueError, KeyError, TypeError):
                    pass
        self.stt, self.tts = StubSTT(), StubTTS()

    def identity(self, name):
        project = self.projects[name]
        return [project["root"], project["provider"], project["model"]]

    def save_sessions(self):
        target = self.directory / "sessions.json"
        temp = target.with_suffix(".tmp")
        temp.write_text(json.dumps({name: dict(identity=self.identity(name), session=self.sessions.get(name),
                                               conversation=self.conversation_ids[name]) for name in self.projects}), encoding="utf-8")
        os.replace(temp, target)

    def emergency(self):
        with self.state_lock:
            self.stopped = True
            self.generation += 1
            self.confirm.pending = None
        self.runner.stop()
        self.audit.write("emergency_stop")
        return {"spoken": "Notaus. Die Bridge bleibt angehalten, bis du sie bewusst wieder aktivierst.", "stopped": True}

    def record(self, transcript, raw, spoken, details):
        record = dict(id=secrets.token_hex(12), time=time.time(), project=self.selected,
                      conversation=self.conversation_ids[self.selected],
                      dry_run=os.environ.get("SB_VOICE_DRY_RUN") == "1",
                      transcript=transcript, full=raw, spoken=spoken, details=details)
        out = self.directory / "out"
        out.mkdir(exist_ok=True)
        (out / (record["id"] + ".txt")).write_text(raw, encoding="utf-8")
        with (self.directory / "history.jsonl").open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(record, ensure_ascii=False) + "\n")
        with self.state_lock:
            self.histories[self.selected].append(record)
        self.audit.write("answer", project=self.selected, report=record["id"])
        return dict(spoken=spoken, details=details, full=raw, project=self.selected)

    def turn(self, text):
        if not isinstance(text, str) or not text.strip() or len(text) > 6000:
            raise ValueError("Transcript must contain 1–6000 characters")
        kind, argument = command(text)
        if kind == "emergency":
            return self.emergency()
        if kind == "quiet":
            with self.state_lock:
                self.generation += 1
                self.confirm.pending = None
            self.runner.stop()
            self.audit.write("quiet")
            return {"spoken": "", "quiet": True}
        if not self.turn_lock.acquire(blocking=False):
            raise ValueError("Eine Antwort läuft noch. Stopp oder Notaus bleibt verfügbar.")
        try:
            with self.state_lock:
                if self.stopped:
                    raise ValueError("Notaus aktiv. Erst bewusst wieder aktivieren.")
                self.runner.cancel.clear()
                generation = self.generation
                project = self.projects[self.selected]
                pending = self.confirm.pending is not None
            self.audit.write("owner_turn", project=self.selected, command=kind, characters=len(text))
            if pending:
                with self.state_lock:
                    edits = self.confirm.consume(self.selected, text)
                    if os.environ.get("SB_VOICE_DRY_RUN") == "1":
                        raise ValueError("Trockenlauf: keine Änderungen erlaubt.")
                    count = apply_edits(project["root"], edits, self.directory / "backups")
                self.audit.write("action_applied", project=self.selected, files=count)
                return self.record(text, "Bestätigte Dateivorschläge angewendet.",
                                   f"Fertig. {count} Dateien wurden gesichert und geändert. Die Freigabe ist verbraucht.", "")
            history = list(self.histories[self.selected])
            if kind in ("repeat", "report"):
                if not history:
                    return {"spoken": "Es gibt noch keinen Bericht."}
                last = history[-1]
                return {"spoken": last["spoken"], "full": last["full"], "details": last["details"]}
            if kind == "new":
                self.sessions.pop(self.selected, None)
                self.conversation_ids[self.selected] = secrets.token_hex(12)
                self.conversations[self.selected] = len(history)
                self.save_sessions()
                return {"spoken": "Eine neue Unterhaltung beginnt. Alte Berichte bleiben im Verlauf."}
            if kind == "switch":
                if argument not in self.projects:
                    raise ValueError("Unbekanntes Projekt. Verfügbare Namen: " + ", ".join(self.projects))
                self.selected = argument
                return {"spoken": "Projekt gewechselt zu " + argument + ". Lesemodus ist aktiv.", "project": argument}
            if kind in ("status", "failed"):
                records = read_records(project["root"])
                last = records[0] if records else {}
                if kind == "failed":
                    failures = []
                    for run in records:
                        for target in run.get("targets", []):
                            for test in target.get("tests", []):
                                if test.get("status") in ("failed", "fail"):
                                    failures.append(test)
                        if failures:
                            break
                    raw = json.dumps(failures, ensure_ascii=False)
                    spoken = f"Im letzten auffindbaren roten Lauf sind {len(failures)} fehlgeschlagene Tests." if failures else "Keine fehlgeschlagenen Tests in den letzten zwanzig gespeicherten Läufen gefunden. Das ist keine neue Prüfung."
                else:
                    branch = fixed_command(["git", "branch", "--show-current"], project["root"])
                    dirty = fixed_command(["git", "status", "--porcelain", "--untracked-files=no"], project["root"])
                    helper = self.repo / "tools/ai/aitool.py"
                    runs = fixed_command([sys.executable, str(helper), "status"], self.repo) if helper.is_file() else "Keine Laufzentrale"
                    verdict = ("grün" if last["ok"] else "rot") if "ok" in last else "kein gespeicherter Gate-Stand"
                    spoken = f"Branch {branch}. " + ("Es gibt Änderungen. " if dirty else "Keine verfolgten Änderungen. ") + f"Letzter gespeicherter Teststand: {verdict}."
                    raw = json.dumps(dict(branch=branch, dirty=dirty, runs=runs, last_gate=last), ensure_ascii=False)
                    # Speak a bounded summary of runs too, without reading paths/log instructions.
                    run_count = len(re.findall(r"\bRUNNING\b|\brunning\b", runs))
                    spoken += f" Die Laufzentrale markiert {run_count} Einträge als möglicherweise laufend; Details sind gespeichert."
                spoken, _, _ = spoken_answer(spoken)
                return self.record(text, raw, spoken, raw)
            active_history = [row for row in history if row.get("conversation") == self.conversation_ids[self.selected] and not row.get("dry_run")]
            prompt = "ACTION REQUEST: propose file edits only. " + argument if kind == "action" else text
            raw, session = self.runner.answer(project, prompt, self.sessions.get(self.selected), active_history)
            spoken, details, obj = spoken_answer(raw)
            with self.state_lock:
                if generation != self.generation or self.stopped:
                    raise ValueError("Antwort nach Stopp verworfen.")
                if session and os.environ.get("SB_VOICE_DRY_RUN") != "1" and re.fullmatch(r"[a-zA-Z0-9_-]{1,160}", session):
                    self.sessions[self.selected] = session
                    self.save_sessions()
                if kind == "action":
                    edits = prepare_edits(project["root"], obj)
                    spoken = self.confirm.offer(self.selected, spoken, edits)
                    self.audit.write("action_offered", project=self.selected, files=len(edits))
                # An unsolicited edits field in ordinary Q&A NEVER grants permission.
                return self.record(text, raw, spoken, details)
        finally:
            self.turn_lock.release()


def make_handler(bridge, token, hosts, port):
    limiter = RateLimit()
    limit_lock = threading.Lock()

    class Handler(BaseHTTPRequestHandler):
        def setup(self):
            super().setup()
            self.connection.settimeout(10)

        def log_message(self, *_):
            pass

        def respond(self, status, body, content_type="application/json; charset=utf-8"):
            if not isinstance(body, bytes):
                body = json.dumps(body, ensure_ascii=False).encode()
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.send_header("Referrer-Policy", "no-referrer")
            self.send_header("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; media-src 'self' blob:; object-src 'none'; frame-ancestors 'none'; base-uri 'none'")
            self.end_headers()
            try:
                self.wfile.write(body)
            except (BrokenPipeError, ConnectionResetError):
                pass

        def guard(self, mutate=False):
            if len(self.headers.get_all("Host", [])) != 1 or not host_allowed(self.headers.get("Host"), hosts, port):
                bridge.audit.write("reject", reason="host")
                self.respond(403, {"error": "Host refused"})
                return False
            path = urlsplit(self.path).path
            if path.startswith("/api/"):
                if not authenticated(self.headers.get("Authorization"), token):
                    bridge.audit.write("reject", reason="auth")
                    self.respond(401, {"error": "Token fehlt oder ist ungültig. Start-URL erneut öffnen."})
                    return False
                csrf = self.headers.get("X-CSRF-Token", "")
                if mutate and (not csrf.isascii() or not secrets.compare_digest(csrf, token)):
                    self.respond(403, {"error": "CSRF token refused"})
                    return False
            origin = self.headers.get("Origin")
            if origin:
                parsed = urlsplit(origin)
                if parsed.scheme not in ("http", "https") or not host_allowed(parsed.netloc, hosts, port):
                    self.respond(403, {"error": "Origin refused"})
                    return False
            with limit_lock:
                allowed = limiter.allow(self.client_address[0]) if path != "/api/emergency" else True
            if not allowed:
                self.respond(429, {"error": "Zu viele Anfragen. Eine Minute warten."})
                return False
            return True

        def do_GET(self):
            if not self.guard():
                return
            path = urlsplit(self.path).path
            if path in SHELL:
                filename = HERE / "static" / SHELL[path]
                mime = {".js": "text/javascript", ".svg": "image/svg+xml", ".json": "application/json"}.get(filename.suffix, mimetypes.guess_type(filename)[0] or "application/octet-stream")
                self.respond(200, filename.read_bytes(), mime)
            elif path == "/api/state":
                with bridge.state_lock:
                    self.respond(200, dict(project=bridge.selected, projects=list(bridge.projects), mode="read-only",
                                           stopped=bridge.stopped, busy=bridge.turn_lock.locked(),
                                           dry_run=os.environ.get("SB_VOICE_DRY_RUN") == "1",
                                           stt=not isinstance(bridge.stt, StubSTT), tts=not isinstance(bridge.tts, StubTTS)))
            elif path == "/api/history":
                with bridge.state_lock:
                    self.respond(200, {"history": list(bridge.histories[bridge.selected])})
            else:
                self.respond(404, {"error": "Not found"})

        def do_POST(self):
            if not self.guard(True):
                return
            self.connection.settimeout(10)
            path = urlsplit(self.path).path
            if path == "/api/emergency":
                self.close_connection = True
                self.respond(200, bridge.emergency())
                return
            try:
                if self.headers.get("Transfer-Encoding"):
                    raise ValueError("Chunked bodies are refused")
                lengths = self.headers.get_all("Content-Length", [])
                size = int(lengths[0]) if len(lengths) == 1 else -1
                if not 0 <= size <= MAX_BODY:
                    self.close_connection = True
                    self.respond(413, {"error": "Body size refused"})
                    return
                body = self.rfile.read(size)
                if len(body) != size:
                    raise ValueError("Incomplete body")
                if path == "/api/stt":
                    if self.headers.get("Content-Type", "").split(";")[0] not in ("audio/webm", "audio/mp4", "audio/wav", "audio/ogg"):
                        raise ValueError("Unsupported audio format")
                    with bridge.turn_lock:
                        text = bridge.stt.transcribe(body, "de-DE")
                    self.respond(200, {"transcript": text})
                    return
                if self.headers.get("Content-Type", "").split(";")[0] != "application/json":
                    raise ValueError("JSON required")
                data = json.loads(body)
                if not isinstance(data, dict):
                    raise ValueError("JSON object required")
                if path == "/api/turn":
                    answer = bridge.turn(data.get("text"))
                elif path == "/api/reactivate":
                    with bridge.state_lock:
                        if bridge.turn_lock.locked():
                            raise ValueError("Gestoppte Anfrage läuft noch aus. Kurz warten.")
                        bridge.stopped = False
                        bridge.confirm.pending = None
                    bridge.audit.write("reactivate")
                    answer = {"spoken": "Bridge wieder aktiv. Lesemodus."}
                elif path == "/api/tts":
                    text = data.get("text")
                    if not isinstance(text, str) or not 1 <= len(text) <= 1000:
                        raise ValueError("TTS text too long")
                    self.respond(200, bridge.tts.synthesize(text), "audio/wav")
                    return
                else:
                    self.respond(404, {"error": "Not found"})
                    return
                self.respond(200, answer)
            except (ValueError, OSError, subprocess.SubprocessError, UnicodeError, KeyError) as exc:
                bridge.audit.write("error", kind=type(exc).__name__)
                self.respond(400, {"error": str(exc)[:300]})
    return Handler


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8772)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--tailscale", action="store_true")
    parser.add_argument("--projects", type=Path)
    parser.add_argument("--stt", choices=("off", "whisper"), default="off")
    parser.add_argument("--whisper-model", default="small")
    parser.add_argument("--piper-model", type=Path)
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        parser.error("Port outside 1–65535")
    ip = detect_tailscale()
    host = ip if args.tailscale else args.host
    if not host:
        parser.error("Tailscale IPv4 not detected")
    try:
        host = validate_host(host)
        bridge = Bridge(projects_file=args.projects)
        if args.stt == "whisper":
            bridge.stt = WhisperSTT(args.whisper_model)
        if args.piper_model:
            bridge.tts = PiperTTS(args.piper_model)
    except (ValueError, ImportError) as exc:
        parser.error(str(exc))
    token_path = HERE / "token.txt"
    if not token_path.exists():
        with token_path.open("x", encoding="ascii") as stream:
            stream.write(secrets.token_urlsafe(32))
        if os.name != "nt":
            token_path.chmod(0o600)
    token = token_path.read_text(encoding="ascii").strip()
    if not re.fullmatch(r"[A-Za-z0-9_-]{40,128}", token):
        parser.error("Invalid token.txt; inspect and rotate it manually")
    name = detect_name()
    hosts = {"127.0.0.1", "localhost", host}
    if ip:
        hosts.add(ip)
    if name:
        hosts.add(name)
    server = ThreadingHTTPServer((host, args.port), make_handler(bridge, token, hosts, args.port))
    server.daemon_threads = True
    bridge.audit.write("start", host=host, port=args.port)
    print(f"Voicebridge: http://{host}:{args.port}/#token={token}", flush=True)
    if name:
        print(f"Phone HTTPS (after tailscale serve): https://{name}/#token={token}", flush=True)
    if ip:
        print(f"Tailscale detected: {ip}; --tailscale binds directly. Phone speech/PWA requires HTTPS.", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        bridge.emergency()
        server.server_close()


if __name__ == "__main__":
    main()
