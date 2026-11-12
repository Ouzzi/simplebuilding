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


def png_rgba(pixels: list[tuple[int, int, int, int]], w: int = 128, h: int = 128, up: bool = False) -> bytes:
    """RGBA PNG; up=True writes every row with the "Up" filter like the mod's height tiles (TileStore.pngUp)."""
    rows = [b"".join(struct.pack("BBBB", *pixels[y * w + x]) for x in range(w)) for y in range(h)]
    if up:
        prev = [bytes(len(r)) for r in rows[:1]] + rows[:-1]
        raw = b"".join(b"\x02" + bytes((a - b) & 0xFF for a, b in zip(r, q)) for r, q in zip(rows, prev))
    else:
        raw = b"".join(b"\x00" + r for r in rows)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw, 6)) + chunk(b"IEND", b"")


def height(x: float, z: float) -> float:
    return (math.sin(x / 61) + math.cos(z / 47) + 0.6 * math.sin((x + z) / 23) + 0.3 * math.cos((x - 2 * z) / 13)
            + 0.22 * math.sin(x / 7.3 + 2 * math.cos(z / 11.1)) * math.cos(z / 8.9 + math.sin(x / 13.7))) / 3.1 + 0.5


MOD = (180, 220, 255, 135)   # Vanilla map brightness LOW, NORMAL, HIGH, LOWEST (of 255)


def rnd(x: int, z: int) -> int:
    """Stable pseudo-random 32-bit number of a block (no pattern)."""
    h = (x * 374761393 + z * 668265263) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return h ^ (h >> 16)


def column(x: int, z: int):
    """A made-up landscape (own drawing, no game pixels): base colour, surface y and water depth; None outside the explored ring."""
    if math.hypot(x - 40, z + 20) > 300 + 40 * math.sin(x / 50):
        return None
    h = height(x, z)
    y = round(62 + (h - 0.36) * 150)          # whole blocks: terraces like real terrain
    if h < 0.36:
        return (64, 64, 255), 62, max(1, min(63, 62 - y))
    if h < 0.4:
        c = (247, 233, 163)
    elif h < 0.62:
        c = (0, 124, 0) if rnd(x, z) % 9 == 0 else (127, 178, 56)
    elif h < 0.72:
        c = (0, 124, 0)
    else:
        c = (112, 112, 112) if h < 0.8 else (255, 255, 255)
    if c == (0, 124, 0):                      # trees: leaves stand 3-6 blocks above the ground
        y += 3 + rnd(z, x) % 4
    if (x // 16 + z // 16) % 37 == 0 and 0.4 < h < 0.62:
        c = (151, 109, 77)  # some paths/houses
    return c, y, 0


def pixels(x: int, z: int, prev):
    """(colour pixel, height pixel, y) like the mod: Vanilla map relief against the northern neighbour, height layer N34c."""
    col = column(x, z)
    if col is None:
        return (0, 0, 0, 0), (0, 0, 0, 0), None
    c, y, depth = col
    dither = (x + z) & 1
    if depth:
        d = depth * 0.1 + dither * 0.2
        b = 2 if d < 0.5 else 0 if d > 0.9 else 1
    else:
        d = (y - (y if prev is None else prev)) * 4.0 / 5.0 + (dither - 0.5) * 0.4
        b = 2 if d > 0.6 else 0 if d < -0.6 else 1
    v = max(0, min(0xFFFF, y + 32768))
    return (tuple(ch * MOD[b] // 255 for ch in c) + (255,)), (v >> 8, v & 0xFF, (b << 6) | depth, 255), y


def tile_pair(tx: int, tz: int, step: int = 1, cx: int | None = None, cz: int | None = None):
    """Colour and height pixels of one tile (step blocks per pixel; cx/cz = first block instead of the tile grid)."""
    colours, heights = [], []
    for j in range(128):
        for i in range(128):
            x = (tx * 128 + i) * step if cx is None else cx + i * step
            z = (tz * 128 + j) * step if cz is None else cz + j * step
            n = column(x, z - step)
            c, hgt, _ = pixels(x, z, None if n is None else n[1])
            colours.append(c)
            heights.append(hgt)
    return colours, heights


def tiles() -> dict[str, bytes]:
    out = {}
    for tz in range(-3, 3):
        for tx in range(-3, 3):
            px, hx = tile_pair(tx, tz)
            if any(p[3] for p in px):
                out[f"minecraft_overworld/4/{tx}/{tz}"] = png_rgba(px)
                out[f"minecraft_overworld.h/4/{tx}/{tz}"] = png_rgba(hx, up=True)
    # one coarse overview tile (zoom 0 = 16 blocks per pixel) for the dashboard preview
    px, hx = tile_pair(0, 0, 16, -64 * 16 + 8, -64 * 16 + 8)
    px = [p if abs(i % 128 - 64) < 30 and abs(i // 128 - 64) < 30 else (0, 0, 0, 0) for i, p in enumerate(px)]
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
