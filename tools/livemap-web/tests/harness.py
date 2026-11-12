"""Local preview of the service with sample data - ONLY for pictures/screenshots, never an acceptance (that happens on the
dev stage). One host for both sites: http://127.0.0.1:<port>/ ; accounts (password "correct horse battery"):
owner@example.com (developer, Pro, maps), free@example.com (Free, one map preparing).

    python3 tests/harness.py --port 8790 [--data <folder>]
"""
from __future__ import annotations

import argparse
import hashlib
import math
import struct
import sys
import tempfile
import time
import zlib
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from smweb import config, security  # noqa: E402
from smweb.app import App, serve  # noqa: E402
from smweb.service import Service  # noqa: E402

PW = "correct horse battery"


def png_rgba(pixels: list[tuple[int, int, int, int]], w: int = 128, h: int = 128) -> bytes:
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *pixels[y * w + x]) for x in range(w)) for y in range(h))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 6)) + chunk(b"IEND", b"")


def height(x: float, z: float) -> float:
    return (math.sin(x / 61) + math.cos(z / 47) + 0.6 * math.sin((x + z) / 23) + 0.3 * math.cos((x - 2 * z) / 13)) / 2.9 + 0.5


def colour(x: int, z: int) -> tuple[int, int, int, int]:
    """A made-up landscape in map-like colours (own drawing, no game pixels); outside a ring = unexplored (transparent)."""
    if math.hypot(x - 40, z + 20) > 300 + 40 * math.sin(x / 50):
        return (0, 0, 0, 0)
    h = height(x, z)
    shade = 1.12 if h > height(x, z - 1) + 0.002 else (0.86 if h < height(x, z - 1) - 0.002 else 1.0)
    if h < 0.36:
        c = (44, 72, 170) if h < 0.3 else (60, 92, 196)
        shade = 1.0
    elif h < 0.4:
        c = (214, 198, 140)
    elif h < 0.62:
        c = (74, 128, 44) if (x * 7 + z * 13) % 11 == 0 else (102, 160, 58)
    elif h < 0.72:
        c = (58, 104, 40)
    else:
        c = (128, 128, 128) if h < 0.8 else (236, 240, 244)
    if (x // 16 + z // 16) % 37 == 0 and 0.4 < h < 0.62:
        c = (150, 110, 70)  # some paths/houses
    return (min(255, int(c[0] * shade)), min(255, int(c[1] * shade)), min(255, int(c[2] * shade)), 255)


def tiles() -> dict[str, bytes]:
    out = {}
    for tz in range(-3, 3):
        for tx in range(-3, 3):
            px = [colour(tx * 128 + i % 128, tz * 128 + i // 128) for i in range(128 * 128)]
            if any(p[3] for p in px):
                out[f"minecraft_overworld/4/{tx}/{tz}"] = png_rgba(px)
    # one coarse overview tile (zoom 0 = 16 blocks per pixel) for the dashboard preview
    px = [colour((i % 128 - 64) * 16 + 8, (i // 128 - 64) * 16 + 8) if abs(i % 128 - 64) < 30 and abs(i // 128 - 64) < 30 else (0, 0, 0, 0) for i in range(128 * 128)]
    out["minecraft_overworld/0/-1/-1"] = png_rgba(px)
    return out


def seed(svc: Service):
    security.ITERATIONS = 1000  # sample data only
    now = svc.now()

    def user(email, name, role="user", tier="free"):
        with svc.db.tx() as c:
            c.execute("INSERT INTO users (email, email_canon, name, pw_hash, role, tier, created_at, verified_at, terms_version, terms_at) "
                      "VALUES (?,?,?,?,?,?,?,?,?,?)", (email, security.canonical_email(email), name, security.hash_password(PW), role, tier,
                                                      now - 40 * 86400, now - 40 * 86400, "draft", now - 40 * 86400))
        return svc.db.one("SELECT * FROM users WHERE email = ?", email)

    owner = user("owner@example.com", "Ouzzi", "developer", "pro")
    free = user("free@example.com", "Mia")
    user("mia+second@example.com", "Mia 2")
    worlds = ["0f8a7c1e-2b3d-4c5e-8f90-a1b2c3d4e5f6", "11111111-2222-4333-8444-555555555555",
              "22222222-3333-4444-8555-666666666666", "33333333-4444-4555-8666-777777777777"]

    def connect(u, name, vis, world, slug=""):
        code, _ = svc.create_code(u, name, vis, slug)
        m, key = svc.redeem(code, world, "0.2.0", "127.0.0.1")
        return m, key

    m1, k1 = connect(owner, "Survival Server", "public", worlds[0], "survival")
    t = tiles()
    svc.api_manifest(m1, {"tiles": {p: hashlib.sha256(b).hexdigest() for p, b in t.items()}, "done": True})
    items = list(t.items())
    for i in range(0, len(items), 50):
        import io
        import zipfile
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            for p, b in items[i:i + 50]:
                z.writestr(p + ".png", b)
        svc.api_tiles(svc.db.one("SELECT * FROM maps WHERE id = ?", m1["id"]), buf.getvalue())
    with svc.db.tx() as c:
        c.execute("UPDATE maps SET show_players = 1 WHERE id = ?", (m1["id"],))
        c.execute("UPDATE users SET dpa_version = 'draft', dpa_at = ? WHERE id = ?", (now, owner["id"]))
    m1 = svc.db.one("SELECT * FROM maps WHERE id = ?", m1["id"])
    svc.api_live(m1, {"dimensions": [{"id": "minecraft:overworld", "folder": "minecraft_overworld"}, {"id": "minecraft:the_nether", "folder": "minecraft_the_nether"}],
                      "players": [{"name": "Ouzzi", "dimension": "minecraft:overworld", "x": 40, "z": -30},
                                  {"name": "Steve", "dimension": "minecraft:overworld", "x": -120, "z": 60}]})
    m2, k2 = connect(owner, "Creative Builds", "link", worlds[1], "creative")
    with svc.db.tx() as c:  # a map that is still being prepared (minimum time of Pro), half uploaded
        c.execute("UPDATE maps SET prep_min_seconds = 3600, connected_at = ?, manifest_done = 1, need_count = 120, done_count = 70 WHERE id = ?", (now - 1500, m2["id"]))
        c.executemany("INSERT INTO pending VALUES (?,?)", [(m2["id"], f"minecraft_overworld/4/{i}/9") for i in range(50)])
    m3, _ = connect(owner, "Old Hardcore World", "private", worlds[2])
    svc.disconnect(svc.db.one("SELECT * FROM maps WHERE id = ?", m3["id"]))
    m4, _ = connect(free, "Mia's SMP", "private", worlds[3])
    for _ in range(37):
        svc.count_view(svc.db.one("SELECT * FROM maps WHERE id = ?", m1["id"]))
    svc.share(svc.db.one("SELECT * FROM maps WHERE id = ?", m1["id"]), "free@example.com")
    svc.mailer.send("free@example.com", "Bestätige dein Simple-Mods-Konto", "Hallo,\n\nbitte bestätige …\n\nhttp://127.0.0.1/verify?t=…\n\nSimple Mods")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=8790)
    ap.add_argument("--data", default=None)
    a = ap.parse_args()
    data = a.data or tempfile.mkdtemp(prefix="smweb-harness-")
    host = f"127.0.0.1:{a.port}"
    cfg = config.load(None, data_dir=data, hosts={"account": host, "livemap": host}, scheme="http", dev_outbox=True,
                      cookie={"name": "sm_session", "domain": "", "secure": False, "days": 30})
    svc = Service(cfg)
    seed(svc)
    srv = serve(App(svc), "127.0.0.1", a.port)
    print(f"harness on http://{host}/ (data {data}); accounts owner@example.com / free@example.com, password: {PW}", flush=True)
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
