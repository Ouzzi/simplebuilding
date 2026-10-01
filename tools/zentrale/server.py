"""
Zentrale der Zentralen - startet die lokalen Zentralen von SimpleBuilding einzeln oder gemeinsam.

    python tools/zentrale/server.py                  http://127.0.0.1:8760, oeffnet den Browser
    python tools/zentrale/server.py --no-browser --port 8760
    python tools/zentrale/server.py --start-all       startet alle fehlenden Zentralen und beendet sich

Register: tools/zentrale/centrals.json (neue Zentrale = ein Eintrag). Jede Zentrale laeuft hier im
Produktivmodus: alle Umgebungsvariablen mit DRY_RUN im Namen werden entfernt (Besitzer 2026-10-01).
Gestoppt werden eigene Prozesse und - nur auf ausdruecklichen Wunsch (external: true) - extern
gestartete Instanzen, aber nur, wenn der Prozess am Port wirklich diese Zentrale ist (seine
Kommandozeile nennt das registrierte Skript). Beendet wird nur dieser Prozess, nie sein Baum: vom Hub
gestartete Minecraft-Clients laufen weiter. Nur Standardbibliothek, nur 127.0.0.1. GET aendert nie
etwas; jeder POST braucht den Header X-Zentrale-Client: 1.
"""

from __future__ import annotations

import argparse
import json
import os
import socket
import subprocess
import sys
import threading
import time
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
REGISTRY = HERE / "centrals.json"
LOG_DIR = ROOT / ".ai-runs" / "zentrale"
LOOPBACK = ("127.0.0.1", "localhost", "[::1]", "::1")
MAX_BODY = 64 * 1024


class ZentraleError(Exception):
    pass


def load_registry(path: Path = REGISTRY) -> list[dict]:
    data = json.loads(path.read_text(encoding="utf-8"))
    centrals = data.get("centrals")
    if not isinstance(centrals, list) or not centrals:
        raise ZentraleError("Register ohne Zentralen")
    seen_ids, seen_ports = set(), set()
    for c in centrals:
        cid, port, args = c.get("id"), c.get("port"), c.get("args")
        if not isinstance(cid, str) or not cid.isidentifier():
            raise ZentraleError(f"ungueltige id {cid!r}")
        if not isinstance(port, int) or not 1024 <= port <= 65535:
            raise ZentraleError(f"{cid}: ungueltiger Port {port!r}")
        if not isinstance(args, list) or not args or not all(isinstance(a, str) for a in args):
            raise ZentraleError(f"{cid}: args muss eine Liste von Texten sein")
        if any(Path(a).is_absolute() or ".." in Path(a).parts for a in args):
            raise ZentraleError(f"{cid}: args duerfen nur Repo-relative Pfade nennen")
        env = c.get("env", {})
        if not isinstance(env, dict) or not all(isinstance(k, str) and isinstance(v, str) for k, v in env.items()):
            raise ZentraleError(f"{cid}: env muss Text auf Text abbilden")
        if any("DRY_RUN" in k.upper() for k in env):
            raise ZentraleError(f"{cid}: Zentralen laufen hier nie im Dry-Run")
        if cid in seen_ids or port in seen_ports:
            raise ZentraleError(f"{cid}: doppelte id oder doppelter Port")
        seen_ids.add(cid)
        seen_ports.add(port)
        c.setdefault("name", cid)
        c.setdefault("path", "/")
    return centrals


def java_major(home: str | None) -> int:
    """Hauptversion des JDK unter {@code home} laut seiner Datei {@code release}; 0, wenn unbekannt."""
    try:
        for line in (Path(home) / "release").read_text(encoding="utf-8", errors="replace").splitlines():
            if line.startswith("JAVA_VERSION="):
                v = line.split("=", 1)[1].strip().strip('"')
                return int(v.split(".")[0]) if not v.startswith("1.") else int(v.split(".")[1])
    except (OSError, ValueError, TypeError):
        pass
    return 0


def find_jdk(major: int, current: str | None) -> str | None:
    """Ein JDK der gewuenschten Hauptversion: das aktuelle JAVA_HOME, sonst die ueblichen Installationsorte."""
    if current and java_major(current) == major:
        return current
    roots = [Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Eclipse Adoptium", Path.home() / ".jdks",
             Path(os.environ.get("ProgramFiles", "C:/Program Files")) / "Java", Path("/usr/lib/jvm")]
    for root in roots:
        if root.is_dir():
            for jdk in sorted(root.iterdir(), reverse=True):
                if jdk.is_dir() and java_major(str(jdk)) == major:
                    return str(jdk)
    return None


def toolchain_env(env: dict) -> dict:
    """Was Hub-Starts (Gradle, Testlaeufe) brauchen: Java 25 als JAVA_HOME, das Python dieser Zentrale im PATH."""
    jdk = find_jdk(25, env.get("JAVA_HOME"))
    path = [str(Path(sys.executable).parent), str(Path(sys.executable).parent / "Scripts")]
    if jdk:
        env["JAVA_HOME"] = jdk
        path.insert(0, str(Path(jdk) / "bin"))
    env["PATH"] = os.pathsep.join(path + [env.get("PATH", "")])
    env.setdefault("PYTHONUTF8", "1")
    return env


def production_env(extra: dict | None = None, base: dict | None = None) -> dict:
    """Die Umgebung des Kindprozesses: ohne jede DRY_RUN-Variable, mit Werkzeugkette, plus die Werte des Eintrags."""
    env = {k: v for k, v in (os.environ if base is None else base).items() if "DRY_RUN" not in k.upper()}
    toolchain_env(env)
    env.update(extra or {})
    env["PYTHONUNBUFFERED"] = "1"
    return env


def port_open(port: int, timeout: float = 0.3) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(timeout)
        return s.connect_ex(("127.0.0.1", port)) == 0


def signature(central: dict) -> str:
    """Was die Kommandozeile eines echten Prozesses dieser Zentrale enthalten muss."""
    args = central["args"]
    return " ".join(args[:2]) if args[0] == "-m" else args[0]


def pid_on_port(port: int) -> int | None:
    """PID, die auf 127.0.0.1:port lauscht (Windows netstat, sonst lsof)."""
    try:
        if os.name == "nt":
            out = subprocess.run(["netstat", "-ano", "-p", "TCP"], capture_output=True, text=True, timeout=10).stdout
            for line in out.splitlines():
                parts = line.split()
                # Sprachunabhaengig (deutsch "ABHOEREN"): ein lauschender Socket hat keine Gegenadresse.
                if len(parts) >= 5 and parts[1].endswith(f":{port}") and parts[2] in ("0.0.0.0:0", "[::]:0"):
                    return int(parts[4])
        else:
            out = subprocess.run(["lsof", "-nP", f"-iTCP:{port}", "-sTCP:LISTEN", "-t"], capture_output=True, text=True, timeout=10).stdout
            return int(out.split()[0]) if out.split() else None
    except (OSError, ValueError, subprocess.SubprocessError):
        return None
    return None


def command_line(pid: int) -> str:
    try:
        if os.name == "nt":
            r = subprocess.run(["powershell", "-NoProfile", "-Command",
                                f"(Get-CimInstance Win32_Process -Filter 'ProcessId={int(pid)}').CommandLine"],
                               capture_output=True, text=True, timeout=15)
            return r.stdout.strip()
        return Path(f"/proc/{int(pid)}/cmdline").read_bytes().replace(b"\0", b" ").decode(errors="replace")
    except (OSError, subprocess.SubprocessError):
        return ""


def matches(central: dict, cmdline: str) -> bool:
    norm = cmdline.replace("\\", "/").lower()
    return "python" in norm and signature(central).replace("\\", "/").lower() in norm


def kill_pid(pid: int) -> None:
    if os.name == "nt":
        subprocess.run(["taskkill", "/PID", str(int(pid)), "/F"], capture_output=True, timeout=15)
    else:
        os.kill(int(pid), 15)


def host_ok(header: str | None) -> bool:
    host = (header or "").strip()
    host = host.split("]")[0] + "]" if host.startswith("[") else host.split(":")[0]
    return host in LOOPBACK


class Zentrale:
    def __init__(self, centrals: list[dict], root: Path = ROOT, log_dir: Path = LOG_DIR):
        self.centrals = {c["id"]: c for c in centrals}
        self.root = root
        self.log_dir = log_dir
        self.procs: dict[str, subprocess.Popen] = {}
        self.lock = threading.Lock()

    def _own(self, cid: str) -> subprocess.Popen | None:
        proc = self.procs.get(cid)
        if proc is not None and proc.poll() is not None:
            self.procs.pop(cid, None)
            return None
        return proc

    def status(self) -> list[dict]:
        out = []
        with self.lock:
            for cid, c in self.centrals.items():
                own = self._own(cid)
                running = port_open(c["port"])
                state = "own" if own and running else "starting" if own else "external" if running else "stopped"
                out.append({"id": cid, "name": c["name"], "port": c["port"], "path": c["path"],
                            "url": f"http://127.0.0.1:{c['port']}{c['path']}", "state": state,
                            "pid": own.pid if own else None})
        return out

    def _ids(self, ids) -> list[str]:
        if ids in (None, "all"):
            return list(self.centrals)
        if not isinstance(ids, list) or not ids or not all(isinstance(i, str) for i in ids):
            raise ZentraleError("ids muss eine Liste sein")
        unknown = [i for i in ids if i not in self.centrals]
        if unknown:
            raise ZentraleError("unbekannte Zentrale: " + ", ".join(unknown))
        return list(dict.fromkeys(ids))

    def start(self, ids=None) -> list[str]:
        started = []
        with self.lock:
            for cid in self._ids(ids):
                c = self.centrals[cid]
                if self._own(cid) or port_open(c["port"]):
                    continue
                self.log_dir.mkdir(parents=True, exist_ok=True)
                log = open(self.log_dir / f"{cid}.log", "ab")
                flags = getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0) | getattr(subprocess, "CREATE_NO_WINDOW", 0)
                self.procs[cid] = subprocess.Popen([sys.executable, *c["args"]], cwd=str(self.root),
                                                   env=production_env(c.get("env")), stdout=log,
                                                   stderr=subprocess.STDOUT, stdin=subprocess.DEVNULL,
                                                   creationflags=flags)
                started.append(cid)
        return started

    def stop(self, ids=None, external: bool = False) -> list[str]:
        stopped = []
        with self.lock:
            for cid in self._ids(ids):
                proc = self._own(cid)
                if proc is None:
                    if external and self._stop_external(cid):
                        stopped.append(cid)
                    continue
                proc.terminate()
                try:
                    proc.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    proc.kill()
                    proc.wait(timeout=5)
                self.procs.pop(cid, None)
                stopped.append(cid)
        return stopped

    def _stop_external(self, cid: str) -> bool:
        """Beendet die extern gestartete Instanz, wenn der Prozess am Port wirklich diese Zentrale ist."""
        c = self.centrals[cid]
        if not port_open(c["port"]):
            return False
        pid = pid_on_port(c["port"])
        if pid is None or pid == os.getpid():
            raise ZentraleError(f"{c['name']}: Prozess an Port {c['port']} nicht gefunden")
        if not matches(c, command_line(pid)):
            raise ZentraleError(f"{c['name']}: an Port {c['port']} laeuft ein fremder Prozess, nicht gestoppt")
        kill_pid(pid)
        deadline = time.time() + 10
        while port_open(c["port"]) and time.time() < deadline:
            time.sleep(0.2)
        return not port_open(c["port"])

    def wait_ready(self, ids, timeout: float = 20.0) -> list[str]:
        """Die Zentralen, deren Port innerhalb von timeout antwortet."""
        deadline, ready = time.time() + timeout, []
        pending = list(ids)
        while pending and time.time() < deadline:
            for cid in list(pending):
                if port_open(self.centrals[cid]["port"]):
                    ready.append(cid)
                    pending.remove(cid)
            time.sleep(0.2)
        return ready


def make_handler(z: Zentrale):
    page = (HERE / "index.html").read_bytes()
    # Gleiches Aussehen wie die anderen Zentralen: die Stylesheet des Hubs und die Wiki-Texturen.
    static = {"/static/app.css": (ROOT / "tools/launchhub/static/app.css", "text/css; charset=utf-8"),
              "/static/zentrale.js": (HERE / "zentrale.js", "text/javascript; charset=utf-8")}
    icon = ROOT / "wiki/assets/textures/item/diamond_core.png"

    class Handler(BaseHTTPRequestHandler):
        server_version = "Zentrale/1"

        def log_message(self, fmt, *args):
            pass

        def _send(self, status: int, body: bytes, ctype: str):
            self.send_response(status)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.send_header("X-Content-Type-Options", "nosniff")
            self.end_headers()
            self.wfile.write(body)

        def _json(self, status: int, data):
            self._send(status, json.dumps(data, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")

        def do_GET(self):
            if not host_ok(self.headers.get("Host")):
                return self._json(403, {"error": "nur lokal"})
            if self.path in ("/", "/index.html"):
                return self._send(200, page, "text/html; charset=utf-8")
            if self.path == "/api/centrals":
                return self._json(200, {"centrals": z.status()})
            if self.path in static and static[self.path][0].is_file():
                file, ctype = static[self.path]
                return self._send(200, file.read_bytes(), ctype)
            if self.path == "/icon.png" and icon.is_file():
                return self._send(200, icon.read_bytes(), "image/png")
            self._json(404, {"error": "nicht gefunden"})

        def do_POST(self):
            if not host_ok(self.headers.get("Host")) or self.headers.get("X-Zentrale-Client") != "1":
                return self._json(403, {"error": "nur lokal mit X-Zentrale-Client"})
            length = int(self.headers.get("Content-Length") or 0)
            if length > MAX_BODY:
                return self._json(413, {"error": "zu gross"})
            try:
                body = json.loads(self.rfile.read(length) or b"{}")
                ids = body.get("ids") if isinstance(body, dict) else None
                external = isinstance(body, dict) and body.get("external") is True
                if self.path == "/api/start":
                    result = {"started": z.start(ids)}
                elif self.path == "/api/stop":
                    result = {"stopped": z.stop(ids, external)}
                elif self.path == "/api/restart":
                    stopped = z.stop(ids, external)
                    started = z.start(ids)
                    result = {"stopped": stopped, "started": started, "ready": z.wait_ready(started, 15)}
                else:
                    return self._json(404, {"error": "nicht gefunden"})
            except (ZentraleError, json.JSONDecodeError) as e:
                return self._json(400, {"error": str(e)})
            result["centrals"] = z.status()
            self._json(200, result)

    return Handler


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=8760)
    parser.add_argument("--no-browser", action="store_true")
    parser.add_argument("--start-all", action="store_true", help="alle fehlenden Zentralen starten und beenden")
    parser.add_argument("--registry", default=str(REGISTRY), help="anderes Register (Tests)")
    a = parser.parse_args(argv)
    z = Zentrale(load_registry(Path(a.registry)))
    if a.start_all:
        started = z.start()
        ready = z.wait_ready(started)
        for s in z.status():
            print(f"{s['name']:28} {s['url']:40} {s['state']}")
        return 0 if set(ready) == set(started) else 1
    server = ThreadingHTTPServer(("127.0.0.1", a.port), make_handler(z))
    url = f"http://127.0.0.1:{a.port}/"
    print("Zentrale auf", url)
    if not a.no_browser:
        threading.Timer(0.5, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
