"""Load test of the hosted Simple Livemap service (N106): one account with N maps of a tier (default 13 x Ultra = the most one
account may connect) against a LOCAL instance - for measuring only, the acceptance happens on the dev stage.

    python3 tests/loadtest.py [--maps 13] [--tier ultra] [--tiles 1500] [--seconds 60] [--viewers 3] [--real <tiles dir>]

Phase 1 = first upload of every map at once (manifest in chunks of 2000 like the mod, ZIP bundles at the pace the service
answers with ``wait``). Phase 2 = steady state for ``--seconds``: every map sends live.json in its tier rhythm and one bundle
of changed tiles per tile rhythm; ``--viewers`` browsers per map poll live.json and load tiles. The server runs in its own
process (CPU/RSS are read from /proc). Tiles: PNG files of a real tiles folder (``--real``, e.g. a world's simplelivemap/tiles)
or the harness landscape; every copy gets its own tEXt chunk, so nothing is deduplicated by checksum.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import os
import random
import statistics
import struct
import subprocess
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.request
import zipfile
import zlib
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))
sys.path.insert(0, str(HERE))


# ------------------------------------------------------------------ server process
def serve(port: int, data: str, maps: int, tier: str):
    from smweb import config, security
    from smweb.app import App, serve as http_serve
    from smweb.service import Service
    security.ITERATIONS = 1000
    host = f"127.0.0.1:{port}"
    cfg = config.load(None, data_dir=data, hosts={"account": host, "livemap": host}, scheme="http", dev_outbox=True,
                      storage_quota_gb=200, cookie={"name": "sm_session", "domain": "", "secure": False, "days": 30})
    svc = Service(cfg)
    now = svc.now()
    with svc.db.tx() as c:
        c.execute("INSERT INTO users (email, email_canon, name, pw_hash, role, tier, created_at, verified_at, terms_version, terms_at, "
                  "connections_total) VALUES (?,?,?,?,?,?,?,?,?,?,?)", ("load@example.com", "load@example.com", "Load",
                                                                       security.hash_password("x" * 12), "user", tier, now, now, "draft", now, 2))
    user = svc.db.one("SELECT * FROM users WHERE email = 'load@example.com'")
    keys = []
    for i in range(maps):
        vis = "public" if i < svc.tier(user)["public"] else "private"
        code, _ = svc.create_code(user, f"Load map {i + 1}", vis)
        m, key = svc.redeem(code, f"00000000-0000-4000-8000-{i:012d}", "1.0.0", "127.0.0.1")
        with svc.db.tx() as c:
            c.execute("UPDATE maps SET show_players = 1, prep_min_seconds = 0 WHERE id = ?", (m["id"],))
            c.execute("UPDATE users SET dpa_version = 'draft', dpa_at = ? WHERE id = ?", (now, user["id"]))
        keys.append({"key": key, "slug": m["slug"], "view": m["view_key"], "visibility": vis})
    Path(data, "keys.json").write_text(json.dumps(keys))
    srv = http_serve(App(svc), "127.0.0.1", port)
    print("ready", flush=True)
    srv.serve_forever()


# ------------------------------------------------------------------ tiles
def with_text(png: bytes, n: int) -> bytes:
    """Same picture, own bytes: a tEXt chunk before IEND (the service checks only signature + IHDR)."""
    data = b"slm\x00" + str(n).encode()
    chunk = struct.pack(">I", len(data)) + b"tEXt" + data + struct.pack(">I", zlib.crc32(b"tEXt" + data) & 0xFFFFFFFF)
    return png[:-12] + chunk + png[-12:]


def source_tiles(real: str | None) -> list[tuple[str, bytes]]:
    if real:
        out = []
        for p in sorted(Path(real).rglob("*.png")):
            rel = p.relative_to(real).with_suffix("").as_posix()
            out.append((rel, p.read_bytes()))
        if out:
            return out
    import harness
    return list(harness.tiles().items())


def tile_set(base: list[tuple[str, bytes]], count: int, salt: int) -> dict[str, bytes]:
    """``count`` tiles: the source pictures spread over a grid of zoom-4 paths (+ their height layer when the source has one)."""
    out = {}
    pics = [b for p, b in base]
    i = 0
    side = int((count ** 0.5)) + 1
    while len(out) < count:
        x, y = i % side - side // 2, i // side - side // 2
        out[f"minecraft_overworld/4/{x}/{y}"] = with_text(pics[i % len(pics)], salt * 1_000_000 + i)
        i += 1
    return out


def bundle(entries: dict[str, bytes]) -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_STORED) as z:
        for p, b in entries.items():
            z.writestr(p + ".png", b)
    return buf.getvalue()


# ------------------------------------------------------------------ client side
class Stats:
    def __init__(self):
        self.lock = threading.Lock()
        self.lat: dict[str, list[float]] = {}
        self.codes: dict[str, dict[int, int]] = {}
        self.api_times: list[float] = []

    def add(self, kind: str, ms: float, code: int, api: bool):
        with self.lock:
            self.lat.setdefault(kind, []).append(ms)
            c = self.codes.setdefault(kind, {})
            c[code] = c.get(code, 0) + 1
            if api:
                self.api_times.append(time.time())


def call(stats: Stats, kind: str, base: str, method: str, path: str, body: bytes | None = None, headers: dict | None = None,
         api: bool = False):
    req = urllib.request.Request(base + path, data=body, method=method, headers=headers or {})
    t0 = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            data, code = r.read(), r.status
    except urllib.error.HTTPError as e:
        data, code = e.read(), e.code
    except OSError:
        data, code = b"", -1
    stats.add(kind, (time.perf_counter() - t0) * 1000, code, api)
    return code, data


def first_upload(stats, base, key, tiles: dict[str, bytes], max_wait: float):
    auth = {"Authorization": "Bearer " + key}
    shas = {p: hashlib.sha256(b).hexdigest() for p, b in tiles.items()}
    paths = list(shas)
    need = []
    for i in range(0, len(paths), 2000):
        chunk = {p: shas[p] for p in paths[i:i + 2000]}
        code, data = call(stats, "manifest", base, "POST", "/api/v1/manifest", json.dumps({"tiles": chunk, "done": i + 2000 >= len(paths)}).encode(),
                          dict(auth, **{"Content-Type": "application/json"}), api=True)
        if code == 200:
            ans = json.loads(data)
            need += ans["need"]
            batch = ans["limits"]["batchTiles"]
            limit_bytes = ans["limits"]["batchBytes"]
    while need:
        part, size = {}, 0
        while need and len(part) < batch and size + len(tiles[need[0]]) < limit_bytes - 65536:
            p = need.pop(0)
            part[p] = tiles[p]
            size += len(tiles[p])
        code, data = call(stats, "tiles(first)", base, "POST", "/api/v1/tiles", bundle(part), dict(auth, **{"Content-Type": "application/zip"}), api=True)
        if code != 200:
            need = list(part) + need
            time.sleep(5)
            continue
        wait = json.loads(data).get("wait", 1)
        if need:
            time.sleep(min(max_wait, max(0.0, wait)))


def proc_sample(pid: int) -> tuple[float, int]:
    """(CPU seconds, RSS bytes) of the server process."""
    with open(f"/proc/{pid}/stat") as f:
        parts = f.read().rsplit(")", 1)[1].split()
    cpu = (int(parts[11]) + int(parts[12])) / os.sysconf("SC_CLK_TCK")
    rss = 0
    with open(f"/proc/{pid}/status") as f:
        for line in f:
            if line.startswith("VmRSS:"):
                rss = int(line.split()[1]) * 1024
    return cpu, rss


def pct(values: list[float], p: float) -> float:
    if not values:
        return 0.0
    s = sorted(values)
    return s[min(len(s) - 1, int(len(s) * p))]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--serve", action="store_true", help=argparse.SUPPRESS)
    ap.add_argument("--port", type=int, default=8797)
    ap.add_argument("--data", default=None)
    ap.add_argument("--maps", type=int, default=13)
    ap.add_argument("--tier", default="ultra")
    ap.add_argument("--tiles", type=int, default=1500, help="tiles per map in the first upload")
    ap.add_argument("--seconds", type=int, default=60)
    ap.add_argument("--viewers", type=int, default=3, help="browsers per map in phase 2")
    ap.add_argument("--changed", type=int, default=64, help="changed tiles per map and tile rhythm")
    ap.add_argument("--max-wait", type=float, default=30.0, help="cap of the service's wait between first-upload bundles")
    ap.add_argument("--real", default=None, help="folder with real tiles (<folder>/<z>/<x>/<y>.png)")
    ap.add_argument("--json", default=None, help="write the result here")
    a = ap.parse_args()
    if a.serve:
        serve(a.port, a.data, a.maps, a.tier)
        return
    data = a.data or tempfile.mkdtemp(prefix="smweb-load-")
    proc = subprocess.Popen([sys.executable, __file__, "--serve", "--port", str(a.port), "--data", data, "--maps", str(a.maps), "--tier", a.tier],
                            stdout=subprocess.PIPE, text=True)
    try:
        if proc.stdout.readline().strip() != "ready":
            raise SystemExit("server did not start")
        base = f"http://127.0.0.1:{a.port}"
        keys = json.loads(Path(data, "keys.json").read_text())
        src = source_tiles(a.real)
        sets = [tile_set(src, a.tiles, i) for i in range(a.maps)]
        tile_bytes = [len(b) for s in sets for b in s.values()]
        stats = Stats()
        cpu0, _ = proc_sample(proc.pid)
        rss_max = [0]
        stop = threading.Event()

        def watch():
            while not stop.is_set():
                rss_max[0] = max(rss_max[0], proc_sample(proc.pid)[1])
                time.sleep(0.5)
        threading.Thread(target=watch, daemon=True).start()

        # phase 1
        t0 = time.time()
        th = [threading.Thread(target=first_upload, args=(stats, base, k["key"], s, a.max_wait)) for k, s in zip(keys, sets)]
        [t.start() for t in th]
        [t.join() for t in th]
        p1 = time.time() - t0
        cpu1, _ = proc_sample(proc.pid)
        tier = json.loads(call(stats, "status", base, "GET", "/api/v1/status", None, {"Authorization": "Bearer " + keys[0]["key"]})[1])
        live_s, tile_s = tier["limits"]["liveSeconds"], tier["limits"]["tileSeconds"]
        states = [json.loads(call(stats, "status", base, "GET", "/api/v1/status", None, {"Authorization": "Bearer " + k["key"]})[1])["state"] for k in keys]

        # phase 2
        end = time.time() + a.seconds
        players = [{"name": f"Player{i}", "dimension": "minecraft:overworld", "x": i * 10, "z": -i * 7} for i in range(10)]
        live_body = json.dumps({"dimensions": [{"id": "minecraft:overworld", "folder": "minecraft_overworld"}], "players": players}).encode()

        def server_loop(k, s):
            auth = {"Authorization": "Bearer " + k["key"]}
            next_tiles = time.time() + random.uniform(0, min(tile_s, a.seconds))
            n = 0
            while time.time() < end:
                call(stats, "live(put)", base, "PUT", "/api/v1/live", live_body, dict(auth, **{"Content-Type": "application/json"}), api=True)
                if time.time() >= next_tiles:
                    n += 1
                    part = {p: with_text(b, 9_000_000 + n) for p, b in list(s.items())[:a.changed]}
                    call(stats, "tiles(changed)", base, "POST", "/api/v1/tiles", bundle(part), dict(auth, **{"Content-Type": "application/zip"}), api=True)
                    next_tiles += tile_s
                time.sleep(live_s)

        def viewer_loop(k, s):
            hdr = {} if k["visibility"] == "public" else {"X-Livemap-Token": k["view"]}
            paths = list(s)
            pre = f"/m/{k['slug']}"
            call(stats, "viewer page", base, "GET", pre + "/", None, hdr)
            last_tiles = 0.0
            first = True
            while time.time() < end:
                call(stats, "viewer live.json", base, "GET", pre + "/live.json", None, hdr)
                if first or time.time() - last_tiles > 5:
                    for p in random.sample(paths, 30 if first else 5):
                        call(stats, "viewer tile", base, "GET", f"{pre}/tiles/{p}.png", None, hdr)
                    last_tiles, first = time.time(), False
                time.sleep(live_s)
        # private maps cannot be viewed by key (owner/shares only): viewers only on public/link maps
        th = [threading.Thread(target=server_loop, args=(k, s)) for k, s in zip(keys, sets)]
        th += [threading.Thread(target=viewer_loop, args=(k, s)) for k, s in zip(keys, sets) if k["visibility"] == "public" for _ in range(a.viewers)]
        t2 = time.time()
        [t.start() for t in th]
        [t.join() for t in th]
        p2 = time.time() - t2
        cpu2, _ = proc_sample(proc.pid)
        stop.set()
        disk = sum(f.stat().st_size for f in Path(data).rglob("*") if f.is_file())
        # peak API requests per second from this one address (nginx limit_req per address)
        per_sec: dict[int, int] = {}
        for t in stats.api_times:
            per_sec[int(t)] = per_sec.get(int(t), 0) + 1
        steady = [v for s, v in per_sec.items() if t2 + 2 <= s <= t2 + p2 - 2]
        res = {
            "maps": a.maps, "tier": a.tier, "tiles_per_map": a.tiles, "tile_source": a.real or "harness", "viewers_per_public_map": a.viewers,
            "tile_bytes_mean": int(statistics.mean(tile_bytes)), "phase1_seconds": round(p1, 1),
            "phase1_tiles_per_second": round(a.maps * a.tiles / max(p1, 0.001), 1), "phase1_cpu_seconds": round(cpu1 - cpu0, 1),
            "states_after_phase1": {s: states.count(s) for s in set(states)},
            "phase2_seconds": round(p2, 1), "phase2_cpu_percent": round(100 * (cpu2 - cpu1) / max(p2, 0.001), 1),
            "rss_max_mb": round(rss_max[0] / 2 ** 20, 1), "data_dir_mb": round(disk / 2 ** 20, 1),
            "api_requests_per_second_steady_mean": round(statistics.mean(steady), 1) if steady else 0,
            "api_requests_per_second_peak": max(per_sec.values()) if per_sec else 0,
            "requests": {k: {"n": len(v), "p50_ms": round(pct(v, 0.5), 1), "p95_ms": round(pct(v, 0.95), 1), "max_ms": round(max(v), 1),
                             "codes": stats.codes[k]} for k, v in stats.lat.items()},
        }
        print(json.dumps(res, indent=1))
        if a.json:
            Path(a.json).write_text(json.dumps(res, indent=1))
    finally:
        proc.terminate()
        proc.wait(5)


if __name__ == "__main__":
    main()
