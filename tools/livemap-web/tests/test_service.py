"""Unit tests of the Simple Mods account + hosted Simple Livemap service (plan docs/ai/PLAN-A-LIVEMAP34-2026-11-12.md).
Run: python3 -m unittest discover -s tools/livemap-web/tests -t tools/livemap-web"""
from __future__ import annotations

import importlib.util
import json
import shutil
import threading
import unittest
import urllib.request
import zipfile

from helpers import (ROOT, WORLD, WORLD2, Client, account, bundle, code_for, connect, link_from_mail, make, png, temp)
from smweb import billing, security
from smweb.app import serve
from smweb.config import VIEWER

TILE = "minecraft_overworld/4/0/0"


class Base(unittest.TestCase):
    over: dict = {}

    def setUp(self):
        self.dir = temp()
        self.svc, self.app, self.clock = make(self.dir, **self.over)
        self.c = Client(self.app)

    def tearDown(self):
        self.svc.db.close()
        shutil.rmtree(self.dir, ignore_errors=True)

    def dev(self, email="dev@example.com"):
        c = Client(self.app, ip="203.0.113.9")
        u = account(c, self.svc, email)
        with self.svc.db.tx() as tx:
            tx.execute("UPDATE users SET role = 'developer' WHERE id = ?", (u["id"],))
        return c

    def upload(self, key, tiles: dict, ctype="application/zip"):
        return self.c.api("POST", "tiles", key=key, raw=bundle({p + ".png": d for p, d in tiles.items()}), ctype=ctype)

    def finish(self, key, tiles: dict):
        import hashlib
        status, data, _ = self.c.api("POST", "manifest", {"tiles": {p: hashlib.sha256(d).hexdigest() for p, d in tiles.items()}, "done": True}, key=key)
        self.assertEqual(status, 200, data)
        if data["need"]:
            status, data, _ = self.upload(key, {p: tiles[p] for p in data["need"]})
            self.assertEqual(status, 200, data)
        return data


class SecurityTests(Base):
    def test_hash_format_is_the_wiki_login_format(self):
        spec = importlib.util.spec_from_file_location("zw_auth", ROOT.parent / "zentrale-web" / "auth.py")
        zw = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(zw)
        h = security.hash_password("a long passphrase")
        self.assertTrue(h.startswith("pbkdf2_sha256$1000$"))
        self.assertTrue(zw.verify_password("a long passphrase", h), "wiki login reads our hashes")
        self.assertTrue(security.verify_password("x" * 12, zw.hash_password("x" * 12, 1000)), "we read wiki hashes")
        self.assertFalse(security.verify_password("wrong", h))
        self.assertFalse(security.verify_password("a long passphrase", "pbkdf2_sha256$1$00$00"), "absurd round counts refused")

    def test_password_policy(self):
        self.assertEqual(security.password_problem("short"), "pw_short")
        self.assertEqual(security.password_problem("minecraft123"), "pw_common")
        self.assertEqual(security.password_problem("aaaaaaaaaaaaab"), "pw_common")
        self.assertEqual(security.password_problem("alex@example.com", "alex@example.com"), "pw_email")
        self.assertIsNone(security.password_problem("correct horse battery"))

    def test_codes_and_emails(self):
        self.assertEqual(security.normalize_code("abcd-efgh"), "ABCDEFGH")
        self.assertIsNone(security.normalize_code("ABCD-EFG0"), "0 is not in the alphabet")
        self.assertEqual(security.canonical_email("a.b+x@googlemail.com"), "ab@gmail.com")
        self.assertIsNone(security.normalize_email("not an address"))
        self.assertIsNone(security.normalize_email("a@b"))

    def test_limiter_window(self):
        lim = security.Limiter(self.clock)
        self.assertTrue(all(lim.allow("k", 3, 60) for _ in range(3)))
        self.assertFalse(lim.allow("k", 3, 60))
        self.clock.advance(61)
        self.assertTrue(lim.allow("k", 3, 60))


class AccountTests(Base):
    def test_register_verify_login_logout(self):
        u = account(self.c, self.svc)
        self.assertTrue(u["verified_at"])
        cookie = [v for v in self.c.jar if v.startswith("__Secure-")]
        self.assertEqual(cookie, ["__Secure-sm_session"])
        r = self.c.post("https://account.test/login", {"email": "alex@example.com", "password": "correct horse battery"},
                        page="https://livemap.test/app")
        sc = r.cookies()[0]
        for attr in ("HttpOnly", "Secure", "SameSite=Lax", "Domain=test", "Max-Age=2592000"):
            self.assertIn(attr, sc)
        self.assertEqual(self.c.get("https://livemap.test/app").status, 200, "the account cookie works on the livemap host")
        self.assertIsNone(self.svc.db.one("SELECT 1 FROM sessions WHERE token_hash = ?", self.c.jar["__Secure-sm_session"][0]), "only hashes stored")
        r = self.c.post("https://account.test/logout", {}, page="https://livemap.test/app")
        self.assertEqual(r.status, 303)
        self.assertEqual(self.c.get("https://livemap.test/app").status, 303, "logged out")

    def test_register_does_not_reveal_existing_accounts(self):
        account(self.c, self.svc)
        other = Client(self.app, ip="192.0.2.50")
        r = other.post("https://account.test/register", {"email": "Alex@Example.com", "password": "another good pass", "password2": "another good pass", "terms": "1"})
        self.assertEqual(r.status, 200)
        self.assertIn("Check your mail", r.body.decode())
        self.assertIn("already has an account", self.svc.mailer.sent[-1]["text"])
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM users"), 1)

    def test_forms_need_csrf_and_same_origin(self):
        r = self.c.request("POST", "https://account.test/login", form={"email": "x@example.com", "password": "y"})
        self.assertEqual(r.status, 403, "no token")
        token = self.c.csrf("https://account.test/login")
        r = self.c.request("POST", "https://account.test/login", form={"email": "x@example.com", "password": "y", "csrf": token}, origin="https://evil.example")
        self.assertEqual(r.status, 403, "foreign origin")
        r = self.c.request("POST", "https://account.test/login", form={"email": "x@example.com", "password": "y", "csrf": token})
        self.assertEqual(r.status, 401, "token + origin ok -> real answer")
        fresh = Client(self.app)
        r = fresh.request("POST", "https://account.test/login", form={"email": "x@example.com", "password": "y", "csrf": token})
        self.assertEqual(r.status, 403, "a token belongs to its cookie")

    def test_login_lockout(self):
        account(self.c, self.svc)
        attacker = Client(self.app, ip="192.0.2.66")
        for _ in range(5):
            r = attacker.post("https://account.test/login", {"email": "alex@example.com", "password": "wrong password"})
            self.assertEqual(r.status, 401)
        r = attacker.post("https://account.test/login", {"email": "alex@example.com", "password": "correct horse battery"})
        self.assertEqual(r.status, 429, "locked even with the right password")
        other = Client(self.app, ip="192.0.2.67")
        self.assertEqual(other.post("https://account.test/login", {"email": "alex@example.com", "password": "correct horse battery"}).status, 429,
                         "the e-mail is locked from every address")
        self.clock.advance(15 * 60 + 1)
        self.assertEqual(other.post("https://account.test/login", {"email": "alex@example.com", "password": "correct horse battery"}).status, 303)

    def test_forgot_and_reset(self):
        account(self.c, self.svc)
        other = Client(self.app, ip="192.0.2.80")
        self.assertEqual(other.post("https://account.test/forgot", {"email": "alex@example.com"}).status, 200)
        link = link_from_mail(self.svc, "reset")
        r = other.post(link, {"password": "a brand new secret", "password2": "a brand new secret", "t": link.split("t=")[1]}, page=link)
        self.assertEqual(r.status, 303)
        self.assertEqual(self.c.get("https://livemap.test/app").status, 303, "old sessions ended")
        self.assertEqual(other.get(link).status, 400, "link used once")
        self.assertEqual(other.post("https://account.test/login", {"email": "alex@example.com", "password": "a brand new secret"}).status, 303)
        other.post("https://account.test/forgot", {"email": "alex@example.com"})
        link = link_from_mail(self.svc, "reset")
        self.clock.advance(3601)
        self.assertEqual(other.get(link).status, 400, "expires after an hour")
        self.assertEqual(other.post("https://account.test/forgot", {"email": "nobody@example.com"}).status, 200, "same answer for unknown")

    def test_export_and_delete(self):
        u = account(self.c, self.svc)
        data = connect(self.c)
        self.upload(data["key"], {TILE: png(1)})
        r = self.c.get("https://livemap.test/app/account/export")
        text = r.body.decode()
        self.assertEqual(r.status, 200)
        self.assertIn("alex@example.com", text)
        self.assertNotIn("pbkdf2", text)
        self.assertNotIn(data["key"], text)
        blob = next((self.svc.cfg.data / "blobs").rglob("*.png"))
        r = self.c.post("https://livemap.test/app/account", {"action": "delete", "password": "correct horse battery", "confirm": "DELETE"})
        self.assertEqual(r.status, 303)
        self.assertIsNone(self.svc.user(u["id"]))
        self.assertFalse(blob.exists(), "tiles deleted with the account")
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM maps"), 0)


class ConnectTests(Base):
    def test_unverified_cannot_connect(self):
        account(self.c, self.svc, verify=False)
        r = self.c.post("https://livemap.test/app/connect", {"name": "x", "visibility": "private"})
        self.assertEqual(r.status, 403)

    def test_code_once_and_15_minutes(self):
        account(self.c, self.svc)
        code = code_for(self.c)
        status, data, _ = self.c.api("POST", "connect", {"code": code.lower(), "world": WORLD, "mod": "0.2.0"})
        self.assertEqual(status, 200)
        self.assertTrue(data["key"].startswith("slm_"))
        self.assertEqual(data["limits"]["liveSeconds"], 10)
        self.assertEqual(self.c.api("POST", "connect", {"code": code, "world": WORLD2})[0], 404, "used once")
        code = code_for(self.c, name="Two")
        self.clock.advance(15 * 60 + 1)
        self.assertEqual(self.c.api("POST", "connect", {"code": code, "world": WORLD2})[0], 404, "expired")
        self.assertEqual(self.c.api("POST", "connect", {"code": "nope", "world": WORLD2})[0], 400)
        self.assertEqual(self.c.api("POST", "connect", {"code": "ABCD-EFGH", "world": "not-a-uuid"})[0], 400)

    def test_code_guessing_is_rate_limited(self):
        for i in range(10):
            self.c.api("POST", "connect", {"code": "ABCD-EFG" + "ABCDEFGHJK"[i], "world": WORLD})
        self.assertEqual(self.c.api("POST", "connect", {"code": "ABCD-EFGM", "world": WORLD})[0], 429)

    def test_places_and_public_share(self):
        account(self.c, self.svc)
        connect(self.c, visibility="public")
        r = self.c.post("https://livemap.test/app/connect", {"name": "B", "visibility": "public"})
        self.assertEqual(r.status, 409, "Free: 1 public")
        connect(self.c, world=WORLD2, name="B", visibility="link")
        r = self.c.post("https://livemap.test/app/connect", {"name": "C", "visibility": "private"})
        self.assertEqual(r.status, 409, "Free: 2 places")
        self.assertIn("2 places", r.body.decode())

    def test_free_place_lock_and_first_two_without_wait(self):
        u = account(self.c, self.svc)
        a = connect(self.c)
        b = connect(self.c, world=WORLD2, name="B")
        rows = self.svc.db.all("SELECT prep_min_seconds FROM maps ORDER BY id")
        self.assertEqual([r[0] for r in rows], [0, 0], "first two connections: no minimum time")
        self.assertEqual(self.c.api("POST", "disconnect", key=a["key"])[0], 200)
        self.assertEqual(self.c.api("GET", "status", key=a["key"])[0], 410, "key of a disconnected map: 410")
        r = self.c.post("https://livemap.test/app/connect", {"name": "C", "visibility": "private"})
        self.assertEqual(r.status, 409, "the freed place is locked for 7 days")
        self.assertIn("7 days", r.body.decode())
        self.clock.advance(7 * 86400 + 1)
        third = "22222222-3333-4444-8555-666666666666"
        c = connect(self.c, world=third, name="C")
        m = self.svc.db.one("SELECT * FROM maps WHERE slug = ?", c["map"])
        self.assertEqual(m["prep_min_seconds"], 6 * 3600, "third connection: Free minimum 6 h")
        self.assertEqual(self.svc.user(u["id"])["connections_total"], 3)
        self.assertEqual(b["state"], "preparing")

    def test_pro_tier_wait_and_custom_name(self):
        u = account(self.c, self.svc)
        self.svc.set_tier(u, "pro")
        with self.svc.db.tx() as tx:
            tx.execute("UPDATE users SET connections_total = 5 WHERE id = ?", (u["id"],))
        data = connect(self.c, slug="my-server")
        self.assertEqual(data["map"], "my-server")
        self.assertEqual(self.svc.db.value("SELECT prep_min_seconds FROM maps"), 3600)
        r = self.c.post("https://livemap.test/app/connect", {"name": "x", "visibility": "private", "slug": "admin"})
        self.assertEqual(r.status, 400, "reserved name")

    def test_connect_state_endpoint(self):
        account(self.c, self.svc)
        r = self.c.post("https://livemap.test/app/connect", {"name": "S", "visibility": "private"})
        import re
        ref = re.search(r'data-ref="([0-9a-f]{16})"', r.body.decode()).group(1)
        code = re.search(r'aria-label="code">([A-Z0-9-]{9})<', r.body.decode()).group(1)
        st = json.loads(self.c.get(f"https://livemap.test/app/connect/state?c={ref}").body)
        self.assertEqual(st["state"], "waiting")
        self.c.api("POST", "connect", {"code": code, "world": WORLD})
        st = json.loads(self.c.get(f"https://livemap.test/app/connect/state?c={ref}").body)
        self.assertEqual(st["state"], "preparing")
        self.assertIn("/app/m/", st["url"])


class UploadTests(Base):
    def setUp(self):
        super().setUp()
        self.user = account(self.c, self.svc)
        self.data = connect(self.c)
        self.key = self.data["key"]

    def test_big_manifest_chunk(self):
        tiles = {f"minecraft_overworld/4/{i % 70}/{i // 70}": "ab" * 32 for i in range(2000)}
        st, d, _ = self.c.api("POST", "manifest", {"tiles": tiles, "done": False}, key=self.key)
        self.assertEqual((st, len(d["need"])), (200, 2000), "a full chunk of the mod (2000)")
        self.assertEqual(self.c.api("POST", "manifest", {"tiles": {f"a/4/{i}/0": "ab" * 32 for i in range(5001)}}, key=self.key)[0], 400)

    def test_preparation_to_ready(self):
        tiles = {TILE: png(1), "minecraft_overworld/0/0/0": png(2)}
        st, d, _ = self.c.api("POST", "manifest", {"tiles": {p: "0" * 64 for p in tiles}, "done": False}, key=self.key)
        self.assertEqual(sorted(d["need"]), sorted(tiles))
        self.assertEqual(d["state"], "preparing")
        self.assertEqual(d["progress"], 0, "manifest not finished")
        self.c.api("POST", "manifest", {"tiles": {}, "done": True}, key=self.key)
        st, d, _ = self.upload(self.key, {TILE: tiles[TILE]})
        self.assertEqual((st, d["stored"], d["progress"]), (200, 1, 50))
        st, d, _ = self.upload(self.key, {"minecraft_overworld/0/0/0": tiles["minecraft_overworld/0/0/0"]})
        self.assertEqual(d["state"], "ready", "first map: ready as soon as the upload is done")
        self.assertGreaterEqual(d["wait"], 900, "then tile rhythm of Free (15 min)")

    def test_minimum_time_holds_ready_back(self):
        with self.svc.db.tx() as tx:
            tx.execute("UPDATE maps SET prep_min_seconds = 3600")
        d = self.finish(self.key, {TILE: png(3)})
        self.assertEqual(d["state"], "preparing")
        self.assertEqual(d["progress"], 0)
        self.clock.advance(1800)
        self.assertEqual(self.c.api("GET", "status", key=self.key)[1]["progress"], 50, "progress = time share")
        self.clock.advance(1801)
        self.assertEqual(self.c.api("GET", "status", key=self.key)[1]["state"], "ready")

    def test_bundle_checks(self):
        bad = [
            {"../../etc/passwd.png": png()},
            {TILE + ".txt": png()},
            {TILE + ".png": b"not a png"},
            {TILE + ".png": png(0, 64, 64)},
        ]
        for entries in bad:
            st, d, _ = self.c.api("POST", "tiles", key=self.key, raw=bundle(entries), ctype="application/zip")
            self.assertEqual(st, 400, entries.keys())
        self.assertEqual(self.c.api("POST", "tiles", key=self.key, raw=b"PK\x03\x04garbage", ctype="application/zip")[0], 400)
        self.assertEqual(self.c.api("POST", "tiles", key=self.key, raw=bundle({TILE + ".png": png()}), ctype="text/plain")[0], 415)
        bomb = bundle({TILE + ".png": b"\x89PNG\r\n\x1a\n" + b"\x00" * 250_000}, zipfile.ZIP_DEFLATED)
        self.assertEqual(self.c.api("POST", "tiles", key=self.key, raw=bomb, ctype="application/zip")[0], 400, "zip bomb ratio")
        many = bundle({f"minecraft_overworld/4/{i}/0.png": png(i) for i in range(301)})
        self.assertIn(self.c.api("POST", "tiles", key=self.key, raw=many, ctype="application/zip")[0], (400, 413), "more than a Free bundle")
        self.assertEqual(self.c.api("POST", "tiles", key="slm_wrong", raw=bundle({TILE + ".png": png()}), ctype="application/zip")[0], 401)

    def test_quota(self):
        self.svc.cfg.tiers["tiers"]["free"]["map_mb"] = 0
        self.assertEqual(self.upload(self.key, {TILE: png()})[0], 507)
        self.svc.cfg.tiers["tiers"]["free"]["map_mb"] = 64
        self.svc.cfg.v["storage_quota_gb"] = 0
        self.assertEqual(self.upload(self.key, {TILE: png()})[1]["error"], "storage_full")

    def test_live_is_filtered_and_not_stored(self):
        live = {"dimensions": [{"id": "minecraft:overworld", "folder": "minecraft_overworld"}, {"id": "<script>", "folder": "x"}],
                "players": [{"name": "Steve", "dimension": "minecraft:overworld", "x": 1, "z": 2, "uuid": "secret"},
                            {"name": "<b>bad</b>", "dimension": "minecraft:overworld", "x": 1, "z": 2}]}
        st, d, _ = self.c.api("PUT", "live", live, key=self.key)
        self.assertEqual((st, d["players"]), (200, 0), "players off by default")
        with self.svc.db.tx() as tx:
            tx.execute("UPDATE maps SET show_players = 1")
        st, d, _ = self.c.api("PUT", "live", live, key=self.key)
        self.assertEqual(d["players"], 1)
        m = self.svc.db.one("SELECT * FROM maps")
        self.assertNotIn("Steve", m["live"] or "", "positions never in the database")
        out = self.svc.live_json(m, False)
        self.assertEqual(out["players"], [{"name": "Steve", "dimension": "minecraft:overworld", "x": 1, "z": 2}])
        self.assertEqual(out["dimensions"], [{"id": "minecraft:overworld", "folder": "minecraft_overworld"}])
        self.clock.advance(301)
        self.assertEqual(self.svc.live_json(m, False)["players"], [], "old positions vanish")

    def test_archive_restore_uploads_only_differences(self):
        import hashlib
        tiles = {TILE: png(5), "minecraft_overworld/4/1/0": png(6)}
        self.finish(self.key, tiles)
        self.assertEqual(self.c.api("POST", "disconnect", key=self.key)[0], 200)
        self.clock.advance(8 * 86400)
        again = connect(self.c, name="Back")
        self.assertEqual(again["map"], self.data["map"], "same world -> same map, tiles kept")
        changed = dict(tiles, **{TILE: png(7)})
        st, d, _ = self.c.api("POST", "manifest", {"tiles": {p: hashlib.sha256(b).hexdigest() for p, b in changed.items()}, "done": True}, key=again["key"])
        self.assertEqual(d["need"], [TILE], "only the changed tile")

    def test_queue_priority(self):
        self.svc.cfg.v["prep_slots"] = 1
        pro = Client(self.app, ip="192.0.2.90")
        pu = account(pro, self.svc, "pro@example.com")
        self.svc.set_tier(pu, "pro")
        p = connect(pro, world=WORLD2)
        q = [r["id"] for r in self.svc.queue()]
        self.assertEqual(self.svc.db.value("SELECT user_id FROM maps WHERE id = ?", q[0]), pu["id"], "Pro first")
        st, d, _ = self.upload(self.key, {TILE: png()})
        self.assertEqual((st, d["error"]), (429, "queued"))
        self.assertEqual(pro.api("POST", "tiles", key=p["key"], raw=bundle({TILE + ".png": png()}), ctype="application/zip")[0], 200)

    def test_dedup_blobs(self):
        other = Client(self.app, ip="192.0.2.91")
        account(other, self.svc, "b@example.com")
        k2 = connect(other, world=WORLD2)["key"]
        same = png(42)
        self.upload(self.key, {TILE: same})
        other.api("POST", "tiles", key=k2, raw=bundle({TILE + ".png": same}), ctype="application/zip")
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM blobs"), 1)
        self.svc.delete_map(self.svc.db.value("SELECT id FROM maps WHERE user_id = ?", self.user["id"]))
        self.assertEqual(self.svc.db.value("SELECT COUNT(*) FROM blobs"), 1, "still used by the other map")


class ViewerTests(Base):
    def setUp(self):
        super().setUp()
        self.user = account(self.c, self.svc)

    def ready_map(self, visibility="public", world=WORLD):
        d = connect(self.c, world=world, visibility=visibility)
        self.c.api("POST", "manifest", {"tiles": {}, "done": True}, key=d["key"])
        self.upload_one(d["key"])
        return d

    def upload_one(self, key):
        return self.c.api("POST", "tiles", key=key, raw=bundle({TILE + ".png": png(9)}), ctype="application/zip")

    def test_viewer_is_the_mods_page(self):
        d = self.ready_map()
        anon = Client(self.app, ip="192.0.2.200")
        r = anon.get(f"https://livemap.test/m/{d['map']}/")
        self.assertEqual(r.status, 200)
        self.assertEqual(r.body, VIEWER.read_bytes(), "byte for byte the viewer of the mod")
        self.assertIn("frame-ancestors 'self'", r.header("Content-Security-Policy"))
        t = anon.get(f"https://livemap.test/m/{d['map']}/tiles/{TILE}.png")
        self.assertEqual((t.status, t.body), (200, png(9)))
        etag = t.header("ETag")
        self.assertEqual(anon.get(f"https://livemap.test/m/{d['map']}/tiles/{TILE}.png", headers={"If-None-Match": etag}).status, 304)
        self.assertEqual(anon.get(f"https://livemap.test/m/{d['map']}/tiles/minecraft_overworld/4/9/9.png").status, 204)
        self.assertEqual(anon.get(f"https://livemap.test/m/{d['map']}/tiles/../../x.png").status, 404)

    def test_link_and_private_access(self):
        d = self.ready_map("link")
        key = self.svc.db.value("SELECT view_key FROM maps")
        anon = Client(self.app, ip="192.0.2.201")
        base = f"https://livemap.test/m/{d['map']}"
        self.assertEqual(anon.get(base + "/").status, 200, "page has no data")
        self.assertEqual(anon.get(base + "/live.json").status, 401)
        self.assertEqual(anon.get(base + "/live.json", headers={"Authorization": "Bearer " + key}).status, 200)
        with self.svc.db.tx() as tx:
            tx.execute("UPDATE maps SET visibility = 'private'")
        self.assertEqual(anon.get(base + "/").status, 303, "private: login first")
        self.assertEqual(anon.get(base + "/live.json", headers={"Authorization": "Bearer " + key}).status, 401, "view key no longer opens it")
        friend = Client(self.app, ip="192.0.2.202")
        account(friend, self.svc, "friend@example.com")
        self.assertEqual(friend.get(base + "/").status, 403)
        m = self.svc.db.one("SELECT * FROM maps")
        self.svc.share(m, "friend@example.com")
        self.assertEqual(friend.get(base + "/").status, 200, "shared account sees it")
        self.assertEqual(friend.get(base + "/live.json").status, 200)

    def test_preparing_page_and_views_limit(self):
        d = connect(self.c, visibility="public")
        anon = Client(self.app, ip="192.0.2.203")
        r = anon.get(f"https://livemap.test/m/{d['map']}/")
        self.assertIn("being prepared", r.body.decode())
        self.svc.instant_ready(self.svc.db.value("SELECT id FROM maps"), self.user)
        self.svc.cfg.tiers["tiers"]["free"]["views_day"] = 2
        self.c.api("POST", "tiles", key=d["key"], raw=bundle({TILE + ".png": png(9)}), ctype="application/zip")
        for _ in range(3):
            anon.get(f"https://livemap.test/m/{d['map']}/")
        t = anon.get(f"https://livemap.test/m/{d['map']}/tiles/{TILE}.png")
        self.assertEqual(t.status, 200, "nothing blocked above the limit")
        self.assertIn("max-age=3600", t.header("Cache-Control"))
        self.assertEqual(json.loads(anon.get(f"https://livemap.test/m/{d['map']}/live.json").body)["poll"], 40)
        self.assertIn("Lots of visitors", self.c.get("https://livemap.test/app").body.decode())

    def test_downgrade_archives_extra_maps(self):
        self.svc.set_tier(self.user, "pro")
        worlds = [WORLD, WORLD2, "33333333-3333-4333-8333-333333333333", "44444444-4444-4444-8444-444444444444"]
        for i, w in enumerate(worlds):
            connect(self.c, world=w, name=f"M{i}", visibility="public" if i < 3 else "private")
        self.svc.set_tier(self.svc.user(self.user["id"]), "free")
        active = self.svc.maps_of(self.user["id"])
        self.assertEqual(len(active), 2)
        self.assertEqual(sum(1 for m in active if m["visibility"] == "public"), 1)
        archived = self.svc.maps_of(self.user["id"], True)
        self.assertEqual(len(archived), 2)
        self.assertEqual(archived[0]["archive_until"] - archived[0]["archived_at"], 30 * 86400)
        self.assertEqual(sorted(m["slot"] for m in active), [0, 1])

    def test_dashboard_and_map_pages_render(self):
        d = self.ready_map("link")
        r = self.c.get("https://livemap.test/app")
        self.assertIn("1 of 2 places", r.body.decode())
        mid = self.svc.db.value("SELECT id FROM maps")
        page = self.c.get(f"https://livemap.test/app/m/{mid}").body.decode()
        self.assertIn("#t=", page)
        r = self.c.post(f"https://livemap.test/app/m/{mid}", {"action": "save", "name": "Ren & <Stimpy>", "visibility": "public", "players": "1"})
        self.assertIn("processing agreement", r.body.decode(), "players need the agreement first (legal U3)")
        r = self.c.post(f"https://livemap.test/app/m/{mid}", {"action": "save", "name": "Ren & <Stimpy>", "visibility": "public", "players": "1", "dpa": "1"})
        self.assertIn("Ren &amp; Stimpy", r.body.decode(), "cleaned and escaped")
        self.assertEqual(self.svc.db.value("SELECT show_players FROM maps"), 1)
        self.assertTrue(self.svc.user(self.user["id"])["dpa_at"])
        self.assertEqual(self.c.get(f"https://livemap.test/app/m/{mid}/preview.png").status, 204, "no coarse tile yet")
        key = self.svc.db.value("SELECT key_hash FROM maps")
        self.assertTrue(key)
        with self.svc.db.tx() as tx:
            tx.execute("UPDATE maps SET key_hash = ? WHERE id = ?", (security.sha256("slm_k"), mid))
        self.c.api("POST", "tiles", key="slm_k", raw=bundle({"minecraft_overworld/2/0/0.png": png(77)}), ctype="application/zip")
        self.assertEqual(self.c.get(f"https://livemap.test/app/m/{mid}/preview.png").body, png(77), "zoom 2 overview as preview")
        for path in ("/app/archive", "/app/plan", "/app/account", "/pricing", "/", "/self-host", "/explore"):
            self.assertEqual(self.c.get("https://livemap.test" + path).status, 200, path)
        self.assertEqual(self.c.get("https://livemap.test/app/m/9999").status, 404)


class AdminBillingTests(Base):
    def test_admin_only_for_developers(self):
        account(self.c, self.svc)
        self.assertEqual(self.c.get("https://livemap.test/admin").status, 403)
        dev = self.dev()
        self.assertEqual(dev.get("https://livemap.test/admin").status, 200)
        d = connect(self.c)
        mid = self.svc.db.value("SELECT id FROM maps")
        with self.svc.db.tx() as tx:
            tx.execute("UPDATE maps SET prep_min_seconds = 99999")
        r = dev.post("https://livemap.test/admin", {"action": "ready", "map": str(mid)})
        self.assertEqual(r.status, 200)
        self.assertEqual(self.svc.db.value("SELECT state FROM maps"), "ready", "sofort fertig")
        uid = self.svc.db.value("SELECT id FROM users WHERE email = 'alex@example.com'")
        dev.post("https://livemap.test/admin", {"action": "tier", "user": str(uid), "tier": "max"})
        self.assertEqual(self.svc.user(uid)["tier"], "max")
        self.assertEqual(d["state"], "preparing")

    def test_stripe_signature(self):
        body = b'{"type":"x"}'
        secret = "whsec_test"
        h = billing.sign(body, secret, 1000)
        self.assertTrue(billing.verify_signature(body, h, secret, now=1100))
        self.assertFalse(billing.verify_signature(body + b" ", h, secret, now=1100), "body changed")
        self.assertFalse(billing.verify_signature(body, h, "other", now=1100), "other secret")
        self.assertFalse(billing.verify_signature(body, h, secret, now=1000 + 301), "too old")
        two = h.replace("v1=", "v1=deadbeef,v1=")
        self.assertTrue(billing.verify_signature(body, two, secret, now=1100), "any v1 may match")
        self.assertFalse(billing.verify_signature(body, None, secret))

    def test_billing_off_says_coming_soon(self):
        account(self.c, self.svc)
        r = self.c.post("https://livemap.test/app/plan", {"tier": "pro", "interval": "month"})
        self.assertIn("coming soon", r.body.decode().lower())
        r = self.app.handle(__import__("smweb.app", fromlist=["Request"]).Request("POST", "livemap.test", "/api/stripe/webhook", {}, {}, b"{}"))
        self.assertEqual(r.status, 503)

    def test_webhook_sets_tier_when_enabled(self):
        u = account(self.c, self.svc)
        self.svc.cfg.billing = {"enabled": True, "secret_key": "sk_test", "webhook_secret": "whsec_x", "prices": {}}
        from smweb.app import Request
        ts = int(self.clock())

        def send(event):
            body = json.dumps(event).encode()
            return self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, {"stripe-signature": billing.sign(body, "whsec_x", ts)}, body))
        self.assertEqual(send({"type": "checkout.session.completed", "data": {"object": {"client_reference_id": str(u["id"]), "customer": "cus_1"}}}).status, 200)
        send({"type": "customer.subscription.updated", "data": {"object": {"customer": "cus_1", "status": "active", "metadata": {"tier": "max"}}}})
        self.assertEqual(self.svc.user(u["id"])["tier"], "max")
        send({"type": "customer.subscription.deleted", "data": {"object": {"customer": "cus_1", "status": "canceled", "metadata": {"tier": "max"}}}})
        self.assertEqual(self.svc.user(u["id"])["tier"], "free")
        bad = self.app.handle(Request("POST", "livemap.test", "/api/stripe/webhook", {}, {"stripe-signature": "t=1,v1=00"}, b"{}"))
        self.assertEqual(bad.status, 400)


class DeployTemplateTests(unittest.TestCase):
    def test_templates_render_completely(self):
        import re
        d = ROOT / "deploy"
        values = {"@STAGE@": "dev", "@PORT@": "8096", "@ACCOUNT_HOST@": "account-dev.simplemods.de", "@LIVEMAP_HOST@": "livemap-dev.simplemods.de",
                  "@DEV_AUTH@": 'auth_basic "x"; auth_basic_user_file /etc/nginx/smweb-dev.htpasswd;', "@SRC@": "/srv/x", "@CONFIG@": "/etc/c.json", "@DATA@": "/srv/d"}
        for name in ("nginx-smweb.conf.in", "nginx-smweb-proxy.conf.in", "smweb.service.in"):
            text = (d / name).read_text(encoding="utf-8")
            for k, v in values.items():
                text = text.replace(k, v)
            self.assertNotRegex(text, r"@[A-Z_]+@", name)
            body = "\n".join(l.split("#", 1)[0] for l in text.splitlines())
            self.assertEqual(body.count("{"), body.count("}"), name + ": braces balanced")
        conf = (d / "nginx-smweb.conf.in").read_text(encoding="utf-8")
        self.assertIn("client_max_body_size 9m", conf, "tiles bodies fit (service limit 8 MiB)")
        self.assertIn("location = /api/v1/manifest { client_max_body_size 1m", conf, "manifest chunks of the mod fit")
        from smweb.app import body_limit
        self.assertGreaterEqual(body_limit("/api/v1/manifest"), 5000 * 110, "5000 manifest entries fit")
        self.assertIn("X-Client-IP $sb_client_ip", (d / "nginx-smweb-proxy.conf.in").read_text(encoding="utf-8"))
        self.assertIn("simplemods-allow.conf", conf, "only the tunnel connector and loopback")
        sh = (d / "deploy_livemap_web.sh").read_text(encoding="utf-8")
        self.assertNotRegex(sh, r"(?i)(sk_live|whsec_[a-z0-9]{8}|password\s*=\s*\S)", "no secrets in the deploy script")


class LegalDraftTests(Base):
    def test_terms_table_prices_match_tiers(self):
        agb = (ROOT.parents[1] / "docs/legal/agb.md").read_text(encoding="utf-8")
        for name, t in self.svc.cfg.tiers["tiers"].items():
            if t["price_month"]:
                price = f"{t['price_month'] // 100},{t['price_month'] % 100:02d}"
                self.assertIn(price, agb, f"{name}: price of tiers.json in the terms draft (legal U8)")


class HttpTests(Base):
    def test_headers_and_redirects(self):
        r = self.c.get("https://livemap.test/")
        csp = r.header("Content-Security-Policy")
        self.assertIn("script-src 'self'", csp)
        self.assertNotIn("unsafe-inline", csp)
        self.assertEqual(r.header("X-Content-Type-Options"), "nosniff")
        self.assertIn("max-age=", r.header("Strict-Transport-Security"))
        self.assertEqual(self.c.get("https://unknown.test/").status, 404)
        self.assertEqual(self.app.safe_next("https://evil.example/"), "https://livemap.test/app")
        self.assertEqual(self.app.safe_next("//evil.example"), "https://livemap.test/app")
        self.assertEqual(self.app.safe_next("https://livemap.test/m/x/"), "https://livemap.test/m/x/")
        r = self.c.get("https://livemap.test/login")
        self.assertTrue(r.header("Location").startswith("https://account.test/login?next="))
        self.assertEqual(self.c.get("https://livemap.test/static/../smweb/app.py").status, 404)

    def test_real_server_limits(self):
        srv = serve(self.app, "127.0.0.1", 0)
        port = srv.server_address[1]
        t = threading.Thread(target=srv.serve_forever, daemon=True)
        t.start()
        try:
            req = urllib.request.Request(f"http://127.0.0.1:{port}/healthz", headers={"Host": "livemap.test"})
            self.assertEqual(urllib.request.urlopen(req, timeout=5).read(), b"ok")
            import http.client
            c = http.client.HTTPConnection("127.0.0.1", port, timeout=5)
            c.putrequest("POST", "/api/v1/live", skip_host=True)
            c.putheader("Host", "livemap.test")
            c.putheader("Content-Length", str(10 ** 8))
            c.endheaders()
            self.assertEqual(c.getresponse().status, 413, "refused before reading")
            c = http.client.HTTPConnection("127.0.0.1", port, timeout=5)
            c.putrequest("POST", "/api/v1/live", skip_host=True)
            c.putheader("Host", "livemap.test")
            c.endheaders()
            self.assertEqual(c.getresponse().status, 411)
        finally:
            srv.shutdown()
            srv.server_close()


class SingleHostTests(Base):
    over = {"hosts": {"account": "127.0.0.1:8790", "livemap": "127.0.0.1:8790"}, "scheme": "http",
            "cookie": {"name": "sm_session", "domain": "", "secure": False, "days": 30}}

    def test_one_host_for_local_tests(self):
        c = Client(self.app)
        r = c.post("http://127.0.0.1:8790/register", {"email": "a@example.com", "password": "correct horse battery", "password2": "correct horse battery", "terms": "1"})
        self.assertEqual(r.status, 200)
        r = c.post("http://127.0.0.1:8790/login", {"email": "a@example.com", "password": "correct horse battery"})
        self.assertEqual(r.status, 303)
        self.assertNotIn("Secure", r.cookies()[0])
        self.assertEqual(c.get("http://127.0.0.1:8790/app").status, 200)
        self.assertEqual(c.get("http://127.0.0.1:8790/").status, 200, "landing, not the account redirect")


if __name__ == "__main__":
    unittest.main()
