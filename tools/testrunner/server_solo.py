#!/usr/bin/env python3
"""Release check REL-CHK §4 (prio26 #5): a real Fabric dedicated server with ONE mod, then a vanilla client knocks.

Per mod (target ``server-solo-<module>-fabric-263``, group ``--targets server-solo``):

1. mods/ gets the release jar of the mod plus every hard requirement from its fabric.mod.json ``depends``
   (our modules recursively, Fabric API, external libraries such as Cloth Config only when a mod declares them
   hard). Nothing else - no client mods, no dev classpath, no :common/:framework classes.
2. The Fabric server launcher (meta.fabricmc.net, cached) starts the vanilla 26.3 server jar (Loom's cache when
   present) with a small heap, flat world, offline mode, a free port.
3. Proof: "Done (" in the log, a few seconds of ticking, then a vanilla status ping (Server List Ping) and a
   vanilla login without any mod channel (minimal protocol client below, packet ids read from the 26.3 jar).
4. ``stop`` on stdin, clean exit, no ERROR/Exception/crash line in the log and no crash report.

The vanilla join is recorded, not demanded: a mod that adds blocks/items is expected to turn away a client
without the mod (Fabric API registry sync). Only mods in VANILLA_JOIN_REQUIRED fail when the client is turned
away; a crash or exception while the client knocks always fails.

  python3 tools/testrunner/server_solo.py --list            closure (mods/ content) per module, runs nothing
  python3 tools/testrunner/server_solo.py simpletweaks ...   run these modules (normally via run.py)
"""
from __future__ import annotations

import io
import json
import os
import re
import shutil
import socket
import struct
import subprocess
import sys
import time
import urllib.request
import uuid
import zipfile
import zlib
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
WORK = REPO / "testing" / "server-solo"
SERVER_DIR = WORK / "server"
CACHE = WORK / "cache"
MC = "26.3"
#: Released modules only; the wiring example is a template, not a mod (prio26 #1 takes it out of the release).
SKIP_MODULES = {"wiringexample"}
#: Mods that must let a vanilla client in (server-side only by design). Empty until the owner names them
#: (Folgepunkt in docs/ai/PLAN-B-SERVER26-2026-11-10.md); a rejection elsewhere is recorded as "known".
VANILLA_JOIN_REQUIRED: set[str] = set()
#: depends keys the loader itself provides.
PROVIDED = {"minecraft", "java", "fabricloader", "fabric-loader"}
XMX = os.environ.get("SIMPLEBUILDING_SERVER_SOLO_XMX", "1G")
BOOT_TIMEOUT = int(os.environ.get("SIMPLEBUILDING_SERVER_SOLO_BOOT_S", "300"))
TICK_SECONDS = 5
#: "crash" not followed by "-": Fabric API's module fabric-crash-report-info-v1 is in every mod list line.
LOG_FAULT = re.compile(r"/ERROR\]|Exception\b|\bcrash(ed|ing)?\b(?!-)", re.IGNORECASE)
#: Lines that match LOG_FAULT but are no fault (keep short, explain each; empty until the first real run shows one).
LOG_FAULT_IGNORED: tuple[re.Pattern, ...] = ()
PLAYER = "SoloProbe"


# ----------------------------------------------------------------------------
# modules.json / fabric.mod.json: who needs what
# ----------------------------------------------------------------------------

def registry(root: Path = REPO) -> list[dict]:
    return json.loads((root / "modules/modules.json").read_text(encoding="utf-8"))["modules"]


def project_dir(entry: dict, root: Path = REPO) -> Path:
    return root.joinpath(*entry["projects"]["fabric"].strip(":").split(":"))


def solo_modules(root: Path = REPO) -> list[dict]:
    return [m for m in registry(root) if m.get("projects", {}).get("fabric") and m["id"] not in SKIP_MODULES]


def gradle_props(root: Path = REPO) -> dict[str, str]:
    out = {}
    for line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            k, _, v = line.partition("=")
            out[k.strip()] = v.strip()
    return out


def source_depends(entry: dict, root: Path = REPO) -> dict[str, str]:
    """depends of the mod as built. SimpleBuilding's fabric.mod.json is a 26.2 template that mc26_3/fabric/build.gradle
    rewrites (cloth-config + every required sub-mod), so for it the rewrite is reproduced from modules.json."""
    if entry["id"] == "simplebuilding":
        by_id = {m["id"]: m for m in registry(root)}
        deps = {"fabric-api": "*", "cloth-config": ">=26.3"}
        for req in entry.get("requires", []):
            if by_id.get(req, {}).get("projects", {}).get("fabric"):
                deps[by_id[req].get("modId", req)] = "*"
        return deps
    path = project_dir(entry, root) / "src/main/resources/fabric.mod.json"
    return json.loads(path.read_text(encoding="utf-8")).get("depends", {})


def fabric_mod_ids(root: Path = REPO) -> dict[str, dict]:
    """fabric mod id -> module entry."""
    return {m.get("modId", m["id"]): m for m in solo_modules(root) + [m for m in registry(root) if m["id"] in SKIP_MODULES]}


def external_libs(root: Path = REPO) -> dict[str, str]:
    """fabric mod id -> maven coordinate of the hard libraries tools/devmods.json knows (e.g. cloth-config)."""
    props = gradle_props(root)
    out = {}
    for mod in json.loads((root / "tools/devmods.json").read_text(encoding="utf-8"))["mods"]:
        maven = mod.get("sources", {}).get("fabric", {}).get("maven")
        fid = mod.get("loaderIds", {}).get("fabric")
        if maven and fid:
            out[fid] = re.sub(r"\$\{([^}]+)\}", lambda m: props.get(m.group(1), m.group(0)), maven)
    return out


def closure(module_id: str, root: Path = REPO) -> dict:
    """{'modules': [module ids, the mod first], 'external': {fabric id: maven}, 'fabricApi': bool, 'unknown': [ids]}"""
    by_fid = fabric_mod_ids(root)
    by_id = {m["id"]: m for m in registry(root)}
    libs = external_libs(root)
    mods, external, unknown, api = [], {}, [], False
    todo = [module_id]
    while todo:
        mid = todo.pop(0)
        if mid in mods:
            continue
        mods.append(mid)
        for dep in source_depends(by_id[mid], root):
            if dep in PROVIDED:
                continue
            if dep in by_fid:
                todo.append(by_fid[dep]["id"])
            elif dep in libs:
                external[dep] = libs[dep]
            elif dep == "fabric-api" or dep.startswith("fabric-"):  # the whole API or one of its modules
                api = True
            else:
                unknown.append(dep)
    if external:
        api = True  # every library on the list is itself a Fabric API user
    return {"modules": mods, "external": external, "fabricApi": api, "unknown": sorted(set(unknown))}


# ----------------------------------------------------------------------------
# Jars: ours from Gradle, Fabric API/libraries from the Gradle cache, launcher + server jar cached
# ----------------------------------------------------------------------------

def gradlew() -> list[str]:
    return [str(REPO / ("gradlew.bat" if os.name == "nt" else "gradlew"))]


def build_jars(module_ids: list[str], timeout: int = 3600) -> tuple[int, str]:
    """ONE Gradle call for every release jar the selected runs need."""
    by_id = {m["id"]: m for m in registry()}
    tasks = sorted({f"{by_id[m]['projects']['fabric']}:jar" for m in module_ids})
    if not tasks:
        return 0, ""
    try:
        done = subprocess.run(gradlew() + tasks + ["-q"], cwd=REPO, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              timeout=timeout, check=False)
    except subprocess.TimeoutExpired:
        return -1, "Gradle-Jar-Bau: Zeitgrenze"
    return done.returncode, done.stdout.decode("utf-8", "replace")


def module_jar(module_id: str) -> Path:
    entry = {m["id"]: m for m in registry()}[module_id]
    libs = project_dir(entry) / "build" / "libs"
    jars = [j for j in libs.glob("*.jar") if not j.name.endswith(("-sources.jar", "-dev.jar", "-javadoc.jar"))]
    if not jars:
        raise FileNotFoundError(f"kein Release-Jar in {libs.relative_to(REPO)}")
    return max(jars, key=lambda j: j.stat().st_mtime)


def gradle_cache_jar(coordinate: str) -> Path | None:
    group, artifact, version = coordinate.split(":")[:3]
    base = Path.home() / ".gradle/caches/modules-2/files-2.1" / group / artifact / version
    hits = sorted(base.glob(f"*/{artifact}-{version}.jar")) if base.is_dir() else []
    return hits[0] if hits else None


def download(url: str, dest: Path) -> Path:
    if not dest.is_file():
        dest.parent.mkdir(parents=True, exist_ok=True)
        req = urllib.request.Request(url, headers={"User-Agent": "simplebuilding-server-solo/1.0"})
        with urllib.request.urlopen(req, timeout=300) as r:
            tmp = dest.with_suffix(".part")
            tmp.write_bytes(r.read())
            tmp.replace(dest)
    return dest


def maven_jar(coordinate: str, repo: str = "https://maven.fabricmc.net") -> Path:
    hit = gradle_cache_jar(coordinate)
    if hit:
        return hit
    group, artifact, version = coordinate.split(":")[:3]
    url = f"{repo}/{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.jar"
    return download(url, CACHE / f"{artifact}-{version}.jar")


def fabric_api_jar() -> Path:
    return maven_jar(f"net.fabricmc.fabric-api:fabric-api:{gradle_props()['mc263_fabric_version']}")


def prepare_server_dir() -> None:
    """Launcher + vanilla server jar once; libraries and the unpacked bundler stay in SERVER_DIR between mods."""
    props = gradle_props()
    loader = props["mc263_loader_version"]
    SERVER_DIR.mkdir(parents=True, exist_ok=True)
    launcher = SERVER_DIR / "fabric-server-launch.jar"
    if not launcher.is_file():
        installers = json.loads(urllib.request.urlopen("https://meta.fabricmc.net/v2/versions/installer", timeout=60).read())
        installer = next(i["version"] for i in installers if i.get("stable"))
        download(f"https://meta.fabricmc.net/v2/versions/loader/{MC}/{loader}/{installer}/server/jar", launcher)
    server_jar = SERVER_DIR / "server.jar"
    if not server_jar.is_file():
        loom = Path.home() / ".gradle/caches/fabric-loom" / MC / "minecraft-server.jar"
        if loom.is_file():
            shutil.copy2(loom, server_jar)  # else the launcher downloads it itself
    (SERVER_DIR / "fabric-server-launcher.properties").write_text("serverJar=server.jar\n", encoding="utf-8")
    (SERVER_DIR / "eula.txt").write_text("eula=true\n", encoding="utf-8")


def fill_mods(module_id: str) -> list[Path]:
    """Fresh mods/, config/, world, logs for one mod; returns the jars placed."""
    c = closure(module_id)
    for sub in ("mods", "config", "world", "logs", "crash-reports"):
        shutil.rmtree(SERVER_DIR / sub, ignore_errors=True)
    (SERVER_DIR / "mods").mkdir()
    jars = [module_jar(m) for m in c["modules"]]
    if c["fabricApi"]:
        jars.append(fabric_api_jar())
    jars += [maven_jar(coord, "https://maven.shedaniel.me") for coord in c["external"].values()]
    for jar in jars:
        shutil.copy2(jar, SERVER_DIR / "mods")
    return jars


def free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def write_properties(port: int) -> None:
    (SERVER_DIR / "server.properties").write_text("\n".join([
        f"server-port={port}", "server-ip=127.0.0.1", "online-mode=false", "enforce-secure-profile=false",
        "level-type=minecraft\\:flat", "generate-structures=false", "spawn-monsters=false",
        "view-distance=3", "simulation-distance=3", "max-players=2", "sync-chunk-writes=false",
        "motd=simplebuilding server-solo", ""]), encoding="utf-8")


# ----------------------------------------------------------------------------
# Minimal vanilla protocol client (no mod channels): status ping + offline login up to "joined the game"
# Packet ids from the 26.3 jar (ConfigurationProtocols/LoginProtocols addPacket order, javap 10.11.).
# ----------------------------------------------------------------------------

LOGIN_CB = {"disconnect": 0x00, "hello": 0x01, "finished": 0x02, "compression": 0x03, "custom_query": 0x04, "cookie": 0x05}
LOGIN_SB = {"hello": 0x00, "custom_query_answer": 0x02, "acknowledged": 0x03, "cookie": 0x04}
CONFIG_CB = {"cookie": 0x00, "custom_payload": 0x01, "disconnect": 0x02, "finish": 0x03, "keep_alive": 0x04, "ping": 0x05,
             "select_known_packs": 0x0F, "code_of_conduct": 0x14}
CONFIG_SB = {"cookie": 0x01, "finish": 0x03, "keep_alive": 0x04, "pong": 0x05, "select_known_packs": 0x07,
             "accept_code_of_conduct": 0x09}


def varint(value: int) -> bytes:
    value &= 0xFFFFFFFF
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def read_varint(stream) -> int:
    result = 0
    for shift in range(0, 35, 7):
        raw = stream.read(1)
        if not raw:
            raise EOFError("Verbindung zu")
        result |= (raw[0] & 0x7F) << shift
        if not raw[0] & 0x80:
            return result - (1 << 32) if result & (1 << 31) else result
    raise ValueError("VarInt zu lang")


def mc_string(text: str) -> bytes:
    data = text.encode("utf-8")
    return varint(len(data)) + data


def read_string(stream) -> str:
    return stream.read(read_varint(stream)).decode("utf-8", "replace")


class Conn:
    def __init__(self, port: int, timeout: float = 15.0):
        self.sock = socket.create_connection(("127.0.0.1", port), timeout=timeout)
        self.file = self.sock.makefile("rb")
        self.threshold = -1

    def send(self, packet_id: int, payload: bytes = b"") -> None:
        body = varint(packet_id) + payload
        if self.threshold >= 0:
            body = varint(len(body)) + zlib.compress(body) if len(body) >= self.threshold else varint(0) + body
        self.sock.sendall(varint(len(body)) + body)

    def recv(self) -> tuple[int, io.BytesIO]:
        length = read_varint(self.file)
        data = self.file.read(length)
        if len(data) < length:
            raise EOFError("Paket abgeschnitten")
        stream = io.BytesIO(data)
        if self.threshold >= 0:
            inner = read_varint(stream)
            if inner:
                stream = io.BytesIO(zlib.decompress(stream.read()))
        return read_varint(stream), stream

    def close(self) -> None:
        try:
            self.sock.close()
        except OSError:
            pass


def handshake(conn: Conn, protocol: int, port: int, intent: int) -> None:
    conn.send(0x00, varint(protocol) + mc_string("127.0.0.1") + struct.pack(">H", port) + varint(intent))


def status_ping(port: int) -> dict:
    """Server List Ping as any vanilla client does it. Returns the status JSON (version.protocol is used for login)."""
    conn = Conn(port)
    try:
        handshake(conn, -1, port, 1)
        conn.send(0x00)
        pid, stream = conn.recv()
        if pid != 0x00:
            raise ValueError(f"Status-Antwort mit Paket 0x{pid:02x}")
        status = json.loads(read_string(stream))
        conn.send(0x01, struct.pack(">q", 4711))
        pid, stream = conn.recv()
        if pid != 0x01 or struct.unpack(">q", stream.read(8))[0] != 4711:
            raise ValueError("Pong fehlt oder falsch")
        return status
    finally:
        conn.close()


def vanilla_login(port: int, protocol: int, wait_play: float = 20.0) -> tuple[str, str]:
    """('play', '') when the server let the client through configuration, ('rejected', reason) when it said no,
    ('error', text) on protocol trouble. The server log is the second witness (see run_solo)."""
    conn = Conn(port)
    try:
        handshake(conn, protocol, port, 2)
        conn.send(LOGIN_SB["hello"], mc_string(PLAYER) + uuid.uuid3(uuid.NAMESPACE_OID, "OfflinePlayer:" + PLAYER).bytes)
        state = "login"
        deadline = time.time() + wait_play
        while time.time() < deadline:
            pid, stream = conn.recv()
            if state == "login":
                if pid == LOGIN_CB["disconnect"]:
                    return "rejected", read_string(stream)
                if pid == LOGIN_CB["hello"]:
                    return "error", "Server verlangt Verschluesselung (online-mode?)"
                if pid == LOGIN_CB["compression"]:
                    conn.threshold = read_varint(stream)
                elif pid == LOGIN_CB["custom_query"]:
                    conn.send(LOGIN_SB["custom_query_answer"], varint(read_varint(stream)) + b"\x00")  # "not understood"
                elif pid == LOGIN_CB["cookie"]:
                    conn.send(LOGIN_SB["cookie"], mc_string(read_string(stream)) + b"\x00")
                elif pid == LOGIN_CB["finished"]:
                    conn.send(LOGIN_SB["acknowledged"])
                    state = "configuration"
            elif state == "configuration":
                if pid == CONFIG_CB["disconnect"]:
                    return "rejected", "Trennung in der Konfigurationsphase (Grund im Server-Log)"
                if pid == CONFIG_CB["keep_alive"]:
                    conn.send(CONFIG_SB["keep_alive"], stream.read(8))
                elif pid == CONFIG_CB["ping"]:
                    conn.send(CONFIG_SB["pong"], stream.read(4))
                elif pid == CONFIG_CB["cookie"]:
                    conn.send(CONFIG_SB["cookie"], mc_string(read_string(stream)) + b"\x00")
                elif pid == CONFIG_CB["select_known_packs"]:
                    packs = [(read_string(stream), read_string(stream), read_string(stream)) for _ in range(read_varint(stream))]
                    known = [p for p in packs if p[0] == "minecraft"]  # a vanilla client knows only the vanilla packs
                    conn.send(CONFIG_SB["select_known_packs"],
                              varint(len(known)) + b"".join(mc_string(a) + mc_string(b) + mc_string(c) for a, b, c in known))
                elif pid == CONFIG_CB["code_of_conduct"]:
                    conn.send(CONFIG_SB["accept_code_of_conduct"])
                elif pid == CONFIG_CB["finish"]:
                    conn.send(CONFIG_SB["finish"])
                    state = "play"
            else:
                return "play", ""  # first play packet (the Login packet): the client is in the world
        return "error", f"keine Antwort im Zustand {state} nach {wait_play:.0f}s"
    except EOFError:
        return "rejected", "Server schloss die Verbindung (Grund im Server-Log)"
    except (OSError, ValueError, zlib.error) as error:
        return "error", f"{type(error).__name__}: {error}"
    finally:
        conn.close()


# ----------------------------------------------------------------------------
# One mod
# ----------------------------------------------------------------------------

def java_cmd() -> list[str]:
    home = os.environ.get("SIMPLEBUILDING_SERVER_JAVA_HOME") or os.environ.get("JAVA_HOME")
    java = str(Path(home) / "bin" / "java") if home else "java"
    return [java, f"-Xmx{XMX}", "-Xms256M", "-XX:+UseSerialGC", "-jar", "fabric-server-launch.jar", "nogui"]


def log_faults(text: str) -> list[str]:
    hits = []
    for line in text.splitlines():
        if LOG_FAULT.search(line) and not any(p.search(line) for p in LOG_FAULT_IGNORED):
            hits.append(line.strip()[:300])
    return hits


def wait_for(proc, out_path: Path, needle: str, timeout: float) -> bool:
    end = time.time() + timeout
    while time.time() < end:
        if needle in out_path.read_text(encoding="utf-8", errors="replace"):
            return True
        if proc.poll() is not None:
            return False
        time.sleep(1)
    return False


def run_solo(module_id: str, log_path: Path) -> dict:
    """Steps -> status. Returns {'steps': [(name, status, message)], 'error': str|None, 'warning': str|None}."""
    steps: list[tuple[str, str, str | None]] = []
    c = closure(module_id)
    if c["unknown"]:
        return {"steps": [("mods_folder", "failed", f"harte Abhaengigkeit unbekannt: {c['unknown']}")],
                "error": f"harte Abhaengigkeit nicht aufloesbar: {', '.join(c['unknown'])}", "warning": None}
    prepare_server_dir()
    jars = fill_mods(module_id)
    steps.append(("mods_folder", "passed", ", ".join(j.name for j in jars)))
    port = free_port()
    write_properties(port)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    out = log_path.open("w", encoding="utf-8")
    proc = subprocess.Popen(java_cmd(), cwd=SERVER_DIR, stdin=subprocess.PIPE, stdout=out, stderr=subprocess.STDOUT,
                            text=True)
    error = warning = None
    try:
        if not wait_for(proc, log_path, "Done (", BOOT_TIMEOUT):
            steps.append(("boot", "failed", "kein 'Done (' im Log" + (f", Exit {proc.returncode}" if proc.poll() is not None else
                                                                     f" nach {BOOT_TIMEOUT}s")))
            error = steps[-1][2]
            return {"steps": steps, "error": error, "warning": None}
        steps.append(("boot", "passed", None))
        time.sleep(TICK_SECONDS)
        steps.append(("ticks", "passed" if proc.poll() is None else "failed", f"{TICK_SECONDS}s nach Done"))
        try:
            status = status_ping(port)
            protocol = int(status["version"]["protocol"])
            steps.append(("status_ping", "passed", f"{status['version'].get('name')} / Protokoll {protocol}"))
        except (OSError, ValueError, KeyError, EOFError) as e:
            steps.append(("status_ping", "failed", f"{type(e).__name__}: {e}"))
            protocol = None
        if protocol is not None:
            outcome, detail = vanilla_login(port, protocol)
            time.sleep(2)
            text = log_path.read_text(encoding="utf-8", errors="replace")
            joined = f"{PLAYER} joined the game" in text
            lost = re.findall(rf"{PLAYER} lost connection: (.*)", text) or re.findall(rf"Disconnecting {PLAYER}.*?: (.*)", text)
            if outcome == "play" or joined:
                steps.append(("vanilla_join", "passed", "Vanilla-Client in der Welt"))
            elif outcome == "rejected" or lost:
                reason = (lost[0] if lost else detail)[:200]
                required = module_id in VANILLA_JOIN_REQUIRED
                steps.append(("vanilla_join", "failed" if required else "known", f"abgewiesen: {reason}"))
                if not required:
                    warning = f"Vanilla-Client abgewiesen (erwartet bei Mods mit eigenen Bloecken/Items): {reason}"
            else:
                steps.append(("vanilla_join", "failed", detail))
        proc.stdin.write("stop\n")
        proc.stdin.flush()
        try:
            code = proc.wait(timeout=90)
            steps.append(("stop", "passed" if code == 0 else "failed", f"Exit {code}"))
        except subprocess.TimeoutExpired:
            steps.append(("stop", "failed", "Server hielt nach 'stop' nicht in 90s an"))
    finally:
        if proc.poll() is None:
            proc.kill()
            proc.wait(timeout=30)
        out.close()
    text = log_path.read_text(encoding="utf-8", errors="replace")
    crashes = list((SERVER_DIR / "crash-reports").glob("*.txt")) if (SERVER_DIR / "crash-reports").is_dir() else []
    faults = log_faults(text)
    if crashes:
        steps.append(("log_clean", "failed", f"Crash-Report {crashes[0].name}"))
    elif faults:
        steps.append(("log_clean", "failed", f"{len(faults)} Fehlerzeilen, erste: {faults[0]}"))
    else:
        steps.append(("log_clean", "passed", None))
    bad = [s for s in steps if s[1] == "failed"]
    if bad:
        error = f"{bad[0][0]}: {bad[0][2]}"
    return {"steps": steps, "error": error, "warning": warning, "faults": faults[:20]}


def main(argv: list[str]) -> int:
    if not argv or argv[0] == "--list":
        for m in solo_modules():
            c = closure(m["id"])
            extra = (["fabric-api"] if c["fabricApi"] else []) + list(c["external"]) + [f"UNBEKANNT:{u}" for u in c["unknown"]]
            print(f"  server-solo-{m['id']}-fabric-263: {' + '.join(c['modules'] + extra)}")
        return 0
    need = sorted({x for m in argv for x in closure(m)["modules"]})
    code, text = build_jars(need)
    if code != 0:
        print(text[-3000:])
        return 1
    ok = True
    for m in argv:
        result = run_solo(m, WORK / "logs" / f"{m}.log")
        ok &= result["error"] is None
        print(f"{m}: {'OK' if result['error'] is None else 'ROT ' + result['error']}" + (f" ({result['warning']})" if result["warning"] else ""))
        for name, status, msg in result["steps"]:
            print(f"    {name:<13}{status:<8}{msg or ''}"[:220])
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
