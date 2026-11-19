"""Upload API v1 for the mod (plan section 3): the server of a connected world sends a manifest of its tiles, then the
missing or changed ones as ZIP bundles, and ``live.json`` in the tier's rhythm. Only 128 x 128 PNG tiles under the tile
path pattern are accepted - the service is no general file store. Tiles are stored by checksum (deduplicated)."""
from __future__ import annotations

import hashlib
import io
import json
import math
import os
import re
import shutil
import struct
import zipfile

from .errors import Problem

TILE_RE = re.compile(r"^[a-z0-9_.-]{1,96}/[0-4]/-?\d{1,7}/-?\d{1,7}$")
SHA_RE = re.compile(r"^[0-9a-f]{64}$")
PNG_SIG = b"\x89PNG\r\n\x1a\n"
MAX_TILE = 256 * 1024
MAX_ENTRIES = 512
MAX_MANIFEST = 5000
MAX_LIVE = 64 * 1024
MAX_PLAYERS = 200
LIVE_FRESH = 300                     # player positions older than this are not shown (server gone)


def check_png(data: bytes) -> bool:
    """PNG signature, IHDR first, 128 x 128, sane bit depth/colour type (no decoding: no image library on the server)."""
    if len(data) < 33 or data[:8] != PNG_SIG or data[12:16] != b"IHDR":
        return False
    w, h, depth, ctype = struct.unpack(">IIBB", data[16:26])
    return w == 128 and h == 128 and depth in (1, 2, 4, 8, 16) and ctype in (0, 2, 3, 4, 6)


class UploadMixin:
    # ---------------------------------------------------------------- auth
    def map_by_key(self, authorization: str | None):
        if not authorization or not authorization.startswith("Bearer ") or len(authorization) > 200:
            raise Problem(401, "key_unknown")
        from .security import sha256
        m = self.db.one("SELECT * FROM maps WHERE key_hash = ?", sha256(authorization[7:].strip()))
        if m is None:
            raise Problem(401, "key_unknown")
        if m["state"] == "archived":
            raise Problem(410, "map_disconnected")
        return self.refresh_state(m)

    def limits(self, m) -> dict:
        t = self.tier(self.user(m["user_id"]))
        return {"liveSeconds": t["live_seconds"], "tileSeconds": t["tile_seconds"], "batchBytes": t["batch_kb"] * 1024,
                "batchTiles": min(MAX_ENTRIES, t["upload_tiles_min"])}

    def wait_for(self, m, stored: int = 0) -> int:
        """Seconds the mod waits before the next tile bundle: queue place, upload rate of the tier, tile rhythm when done."""
        ok, pos = self.admitted(m)
        if not ok:
            return 60
        t = self.tier(self.user(m["user_id"]))
        pace = math.ceil(60 * stored / max(1, t["upload_tiles_min"]))
        pending = self.db.value("SELECT COUNT(*) FROM pending WHERE map_id = ?", m["id"], default=0)
        if m["state"] == "preparing" and (pending or not m["manifest_done"]):
            return max(1, pace)
        return max(pace, t["tile_seconds"])

    def answer(self, m, **extra) -> dict:
        m = self.refresh_state(self.db.one("SELECT * FROM maps WHERE id = ?", m["id"]))
        ok, pos = self.admitted(m)
        out = {"map": m["slug"], "name": m["name"], "state": m["state"], "progress": self.progress(m),
               "viewer": self.cfg.url("livemap", f"/m/{m['slug']}/"), "queued": pos, "limits": self.limits(m)}
        out.update(extra)
        out.setdefault("wait", self.wait_for(m))
        return out

    # ---------------------------------------------------------------- endpoints
    def api_connect(self, body: dict, ip: str) -> dict:
        if not isinstance(body, dict):
            raise Problem(400, "bad_request")
        m, key = self.redeem(body.get("code"), body.get("world"), str(body.get("mod") or ""), ip)
        return self.answer(m, key=key)

    def api_status(self, m) -> dict:
        return self.answer(m)

    def api_manifest(self, m, body: dict) -> dict:
        tiles = body.get("tiles") if isinstance(body, dict) else None
        if not isinstance(tiles, dict) or len(tiles) > MAX_MANIFEST:
            raise Problem(400, "bad_request")
        for path, sha in tiles.items():
            if not isinstance(path, str) or not TILE_RE.match(path) or not isinstance(sha, str) or not SHA_RE.match(sha):
                raise Problem(400, "bad_request")
        have = {}
        if tiles:
            have = {r["path"]: r["sha"] for r in self.db.all(
                "SELECT path, sha FROM tiles WHERE map_id = ? AND path IN (" + ",".join("?" * len(tiles)) + ")", m["id"], *tiles)}
        need = [p for p, sha in tiles.items() if have.get(p) != sha]
        done = bool(body.get("done"))
        with self.db.tx() as c:
            added = 0
            for p in need:
                added += c.execute("INSERT OR IGNORE INTO pending VALUES (?,?)", (m["id"], p)).rowcount
            c.executemany("INSERT OR IGNORE INTO manifest_seen VALUES (?,?)", [(m["id"], p) for p in tiles])
            c.execute("UPDATE maps SET need_count = need_count + ?, manifest_done = manifest_done OR ? WHERE id = ?",
                      (added, 1 if done else 0, m["id"]))
            if done:
                self.prune_unreported(c, m["id"])
        return self.answer(m, need=need)

    def prune_unreported(self, c, map_id: int) -> int:
        """End of a manifest round: tiles the mod no longer has (it sends its whole tile list on every start, in chunks up
        to ``done``) leave the map. Interrupted rounds only add up (fewer deletions, never more); a round that reported no
        tile at all deletes nothing (a moved or empty world folder is no reason to wipe a map)."""
        seen = c.execute("SELECT COUNT(*) FROM manifest_seen WHERE map_id = ?", (map_id,)).fetchone()[0]
        gone = []
        if seen:
            gone = c.execute("SELECT path, sha, size FROM tiles WHERE map_id = ? AND path NOT IN "
                             "(SELECT path FROM manifest_seen WHERE map_id = ?)", (map_id, map_id)).fetchall()
        for row in gone:
            c.execute("DELETE FROM tiles WHERE map_id = ? AND path = ?", (map_id, row["path"]))
            self.drop_blob_if_unused(c, row["sha"])
        if gone:
            c.execute("UPDATE maps SET bytes = MAX(0, bytes - ?) WHERE id = ?", (sum(r["size"] for r in gone), map_id))
        if seen:  # asked-for tiles the mod no longer has would keep the map "preparing" forever
            stale = c.execute("DELETE FROM pending WHERE map_id = ? AND path NOT IN (SELECT path FROM manifest_seen WHERE map_id = ?)",
                              (map_id, map_id)).rowcount
            if stale:
                c.execute("UPDATE maps SET need_count = MAX(done_count, need_count - ?) WHERE id = ?", (stale, map_id))
        c.execute("DELETE FROM manifest_seen WHERE map_id = ?", (map_id,))
        return len(gone)

    def api_tiles(self, m, data: bytes) -> dict:
        ok, pos = self.admitted(m)
        if not ok:
            raise Problem(429, "queued", retry_after=60, position=pos)
        user = self.user(m["user_id"])
        t = self.tier(user)
        if len(data) > t["batch_kb"] * 1024:
            raise Problem(413, "too_large")
        entries = self.read_bundle(data, min(MAX_ENTRIES, t["upload_tiles_min"]))
        # Upload rate of the tier (tiles per minute), counted per map.
        for _ in entries:
            if not self.limiter.allow(f"tiles:{m['id']}", t["upload_tiles_min"], 60):
                raise Problem(429, "too_many", retry_after=60)
        new_bytes = sum(len(b) for _, b in entries)
        if m["bytes"] + new_bytes > t["map_mb"] * 1024 * 1024:
            raise Problem(507, "map_full")
        if self.storage_used() + new_bytes > self.cfg["storage_quota_gb"] * 1024 ** 3 or self.disk_percent() >= self.cfg["disk_stop_percent"]:
            raise Problem(507, "storage_full")
        stored = 0
        with self.db.tx() as c:
            for path, png in entries:
                sha = hashlib.sha256(png).hexdigest()
                old = c.execute("SELECT sha, size FROM tiles WHERE map_id = ? AND path = ?", (m["id"], path)).fetchone()
                if old and old["sha"] == sha:
                    done = c.execute("DELETE FROM pending WHERE map_id = ? AND path = ?", (m["id"], path)).rowcount
                    c.execute("UPDATE maps SET done_count = done_count + ? WHERE id = ?", (done, m["id"]))
                    continue
                self.put_blob(c, sha, png)
                c.execute("INSERT INTO tiles VALUES (?,?,?,?) ON CONFLICT(map_id, path) DO UPDATE SET sha = excluded.sha, size = excluded.size",
                          (m["id"], path, sha, len(png)))
                if old:
                    self.drop_blob_if_unused(c, old["sha"])
                done = c.execute("DELETE FROM pending WHERE map_id = ? AND path = ?", (m["id"], path)).rowcount
                c.execute("UPDATE maps SET bytes = bytes + ? - ?, done_count = done_count + ?, tiles_at = ? WHERE id = ?",
                          (len(png), old["size"] if old else 0, done, self.now(), m["id"]))
                stored += 1
        return self.answer(m, stored=stored, wait=self.wait_for(self.refresh_state(self.db.one("SELECT * FROM maps WHERE id = ?", m["id"])), len(entries)))

    def read_bundle(self, data: bytes, max_entries: int) -> list[tuple[str, bytes]]:
        """Validated (path, png) pairs of a ZIP bundle; any bad entry rejects the whole bundle (400)."""
        try:
            z = zipfile.ZipFile(io.BytesIO(data))
            infos = z.infolist()
        except (zipfile.BadZipFile, ValueError, OSError, EOFError):
            raise Problem(400, "bundle_invalid")
        if not infos or len(infos) > max_entries:
            raise Problem(400, "bundle_invalid")
        total = 0
        seen = set()
        out = []
        for info in infos:
            name = info.filename
            if not name.endswith(".png") or not TILE_RE.match(name[:-4]) or name in seen or info.is_dir():
                raise Problem(400, "bundle_invalid")
            if info.compress_type not in (zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED) or info.file_size > MAX_TILE or info.flag_bits & 0x1:
                raise Problem(400, "bundle_invalid")
            total += info.file_size
            if total > 4 * len(data) + 64 * 1024:
                raise Problem(400, "bundle_invalid")  # compression ratio of a zip bomb, not of PNG tiles
            seen.add(name)
            try:
                with z.open(info) as f:
                    png = f.read(MAX_TILE + 1)
            except (zipfile.BadZipFile, OSError, RuntimeError, EOFError):
                raise Problem(400, "bundle_invalid")
            if len(png) > MAX_TILE or len(png) != info.file_size or not check_png(png):
                raise Problem(400, "bundle_invalid")
            out.append((name[:-4], png))
        return out

    def api_live(self, m, body: dict) -> dict:
        """Keeps only dimensions [{id, folder}] and players [{name, dimension, x, z}]; positions stay in memory only (the
        last state, never a history) and only when the map shows players."""
        if not isinstance(body, dict):
            raise Problem(400, "bad_request")
        dims = []
        for d in (body.get("dimensions") or [])[:64]:
            if isinstance(d, dict) and isinstance(d.get("id"), str) and isinstance(d.get("folder"), str) \
                    and len(d["id"]) <= 128 and re.fullmatch(r"[a-z0-9_.-]{1,96}", d["folder"]) and re.fullmatch(r"[a-z0-9_.:/-]{1,128}", d["id"]):
                dims.append({"id": d["id"], "folder": d["folder"]})
        players = []
        if m["show_players"]:
            for p in (body.get("players") or [])[:MAX_PLAYERS]:
                if isinstance(p, dict) and isinstance(p.get("name"), str) and re.fullmatch(r"[A-Za-z0-9_]{1,16}", p["name"]) \
                        and isinstance(p.get("dimension"), str) and len(p["dimension"]) <= 128 \
                        and all(isinstance(p.get(k), int) and abs(p[k]) <= 30_000_000 for k in ("x", "z")):
                    players.append({"name": p["name"], "dimension": p["dimension"], "x": p["x"], "z": p["z"]})
        self.live_cache[m["id"]] = (players, self.now())
        dims_json = json.dumps(dims, separators=(",", ":"))
        if dims_json != (m["live"] or ""):
            with self.db.tx() as c:
                c.execute("UPDATE maps SET live = ?, live_at = ? WHERE id = ?", (dims_json, self.now(), m["id"]))
        t = self.tier(self.user(m["user_id"]))
        return {"wait": t["live_seconds"], "players": len(players)}

    def api_disconnect(self, m) -> dict:
        self.disconnect(m)
        return {"state": "archived"}

    # ---------------------------------------------------------------- viewer data
    def live_json(self, m, over: bool) -> dict:
        try:
            dims = json.loads(m["live"] or "[]")
        except ValueError:
            dims = []
        players, at = self.live_cache.get(m["id"], ([], 0))
        if not m["show_players"] or self.now() - at > LIVE_FRESH:
            players = []
        t = self.tier(self.user(m["user_id"]))
        return {"dimensions": dims, "players": players, "poll": t["live_seconds"] * (4 if over else 1)}

    def tile(self, m, path: str):
        row = self.db.one("SELECT sha FROM tiles WHERE map_id = ? AND path = ?", m["id"], path)
        if not row:
            return None, None
        try:
            return row["sha"], self.blob_path(row["sha"]).read_bytes()
        except OSError:
            return None, None

    # ---------------------------------------------------------------- blob store
    def blob_path(self, sha: str):
        return self.cfg.data / "blobs" / sha[:2] / (sha + ".png")

    def put_blob(self, c, sha: str, png: bytes):
        p = self.blob_path(sha)
        if not p.exists():
            p.parent.mkdir(parents=True, exist_ok=True)
            tmp = p.with_suffix(".tmp")
            with open(tmp, "wb") as f:
                f.write(png)
            os.replace(tmp, p)
        if c.execute("INSERT OR IGNORE INTO blobs VALUES (?,?)", (sha, len(png))).rowcount:
            self._storage = None

    def drop_blob_if_unused(self, c, sha: str):
        if c.execute("SELECT 1 FROM tiles WHERE sha = ? LIMIT 1", (sha,)).fetchone():
            return
        c.execute("DELETE FROM blobs WHERE sha = ?", (sha,))
        self._storage = None
        try:
            self.blob_path(sha).unlink()
        except OSError:
            pass

    def storage_used(self) -> int:
        if getattr(self, "_storage", None) is None:
            self._storage = self.db.value("SELECT SUM(size) FROM blobs", default=0)
        return self._storage

    def disk_percent(self) -> int:
        try:
            u = shutil.disk_usage(self.cfg.data)
            return int(100 * u.used / max(1, u.total))
        except OSError:
            return 0
