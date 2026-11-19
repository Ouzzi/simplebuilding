"""Maps of the hosted Simple Livemap (spec sections 3 and 4): tiers, places, connect codes, preparation queue, archive,
sharing, views. Every limit is checked here on the server; the pages only show the numbers.

States: ``preparing`` (connected, first upload and minimum time running), ``ready`` (shown), ``archived`` (disconnected,
kept until ``archive_until``)."""
from __future__ import annotations

import math
import re
import secrets
import time

from . import security as sec
from .errors import Problem

CODE_SECONDS = 15 * 60
MAX_OPEN_CODES = 3
VISIBILITIES = ("private", "link", "public")
REPORT_REASONS = ("illegal", "privacy", "copyright", "other")
SLUG_RE = re.compile(r"^[a-z0-9](?:[a-z0-9-]{1,30}[a-z0-9])$")
RESERVED = {"app", "admin", "api", "static", "login", "register", "logout", "pricing", "explore", "self-host", "m", "new",
            "account", "settings", "simplemods", "livemap", "www", "help", "support", "status", "report"}
DEV_TIER = {"name": "Developer", "price_month": 0, "maps": 999, "public": 999, "archive": 999, "archive_days": 3650,
            "views_day": 10**9, "live_seconds": 1, "tile_seconds": 60, "prep_minutes": 0, "priority": 9, "slot_lock_days": 0,
            "shares": -1, "map_mb": 16384, "upload_tiles_min": 50000, "batch_kb": 8192, "custom_name": True}


def day(ts: int) -> str:
    return time.strftime("%Y-%m-%d", time.gmtime(ts))


class MapsMixin:
    # ---------------------------------------------------------------- tiers
    def tier(self, user) -> dict:
        if user["role"] == "developer":
            return DEV_TIER
        tiers = self.cfg.tiers["tiers"]
        return tiers.get(user["tier"]) or tiers["free"]

    def tier_rank(self, user) -> int:
        return self.tier(user)["priority"]

    def maps_of(self, uid: int, archived: bool = False):
        if archived:
            return self.db.all("SELECT * FROM maps WHERE user_id = ? AND state = 'archived' ORDER BY archived_at DESC", uid)
        return self.db.all("SELECT * FROM maps WHERE user_id = ? AND state != 'archived' ORDER BY id", uid)

    def usage(self, user) -> dict:
        t = self.tier(user)
        active = self.maps_of(user["id"])
        today = day(self.now())
        views_today = self.db.value("SELECT SUM(count) FROM views WHERE user_id = ? AND day = ?", user["id"], today, default=0)
        since = day(self.now() - 30 * 86400)
        views_30 = self.db.value("SELECT SUM(count) FROM views WHERE user_id = ? AND day > ?", user["id"], since, default=0)
        return {"maps": len(active), "maps_max": t["maps"], "public": sum(1 for m in active if m["visibility"] == "public"),
                "public_max": t["public"], "archived": len(self.maps_of(user["id"], True)), "archive_max": t["archive"],
                "views_today": views_today, "views_max": t["views_day"], "views_30": views_30,
                "bytes": sum(m["bytes"] for m in active), "over_views": views_today >= t["views_day"]}

    # ---------------------------------------------------------------- places (slots)
    def free_slot(self, user) -> tuple[int | None, int]:
        """(index of a place that may be used now, or None; when the next one frees up). Free tier: a place can be taken
        again at most once per ``slot_lock_days`` (spec 3); the first use of a place is always allowed."""
        t = self.tier(user)
        used = {m["slot"] for m in self.maps_of(user["id"]) if m["slot"] is not None}
        last = {r["idx"]: r["last_assigned_at"] for r in self.db.all("SELECT * FROM slots WHERE user_id = ?", user["id"])}
        lock = t["slot_lock_days"] * 86400
        now = self.now()
        soonest = 0
        for idx in range(t["maps"]):
            if idx in used:
                continue
            at = last.get(idx)
            if at is None or not lock or now - at >= lock:
                return idx, 0
            soonest = at + lock if not soonest else min(soonest, at + lock)
        return None, soonest

    # ---------------------------------------------------------------- connect codes
    def create_code(self, user, name: str, visibility: str, slug: str = "", map_id: int | None = None) -> tuple[str, int]:
        """A one-time code (15 min) for ``/simplelivemap connect``; the place and the public share are checked now and again
        when the code is used."""
        from .accounts import clean_name
        if not user["verified_at"]:
            raise Problem(403, "verify_first")
        if visibility not in VISIBILITIES:
            raise Problem(400, "bad_request")
        n = clean_name(name)
        if not n:
            raise Problem(400, "name_invalid")
        if map_id is not None:
            m = self.db.one("SELECT * FROM maps WHERE id = ? AND user_id = ? AND state = 'archived'", map_id, user["id"])
            if not m:
                raise Problem(404, "not_found")
        self.check_place(user, visibility)
        slug = (slug or "").strip().lower()
        if slug:
            self.check_slug(user, slug, map_id)
        now = self.now()
        open_codes = self.db.value("SELECT COUNT(*) FROM codes WHERE user_id = ? AND used_at IS NULL AND expires_at > ?", user["id"], now, default=0)
        if open_codes >= MAX_OPEN_CODES:
            raise Problem(429, "too_many_codes")
        code = sec.new_code()
        with self.db.tx() as c:
            c.execute("INSERT INTO codes VALUES (?,?,?,?,?,?,?,?,NULL)",
                      (sec.sha256(code), user["id"], n, visibility, slug or None, map_id, now, now + CODE_SECONDS))
            self.audit(c, user["id"], "code", n)
        return code, now + CODE_SECONDS

    def check_place(self, user, visibility: str):
        t = self.tier(user)
        idx, until = self.free_slot(user)
        if idx is None:
            if until:
                raise Problem(409, "slot_locked", date=time.strftime("%Y-%m-%d %H:%M UTC", time.gmtime(until)))
            raise Problem(409, "no_slot", max=t["maps"])
        if visibility == "public":
            public = sum(1 for m in self.maps_of(user["id"]) if m["visibility"] == "public")
            if public >= t["public"]:
                raise Problem(409, "no_public", max=t["public"])
        return idx

    def check_slug(self, user, slug: str, map_id: int | None = None):
        if not self.tier(user)["custom_name"]:
            raise Problem(403, "custom_name_tier")
        if not SLUG_RE.match(slug) or slug in RESERVED:
            raise Problem(400, "slug_invalid")
        other = self.db.one("SELECT id FROM maps WHERE slug = ?", slug)
        if other and other["id"] != map_id:
            raise Problem(409, "slug_taken")

    def random_slug(self) -> str:
        while True:
            s = "".join(secrets.choice("abcdefghijkmnpqrstuvwxyz23456789") for _ in range(8))
            if not self.db.one("SELECT 1 FROM maps WHERE slug = ?", s):
                return s

    def redeem(self, code: str, world: str, mod_version: str, ip: str) -> tuple[dict, str]:
        """``/simplelivemap connect <code>``: binds the world to a place, returns (map row, upload key shown only now).
        A world that waits in the archive of the same account comes back with its tiles (only differences upload)."""
        if not self.limiter.allow("code-ip:" + ip, 10, 60) or self.limiter.blocked("code-fail:" + ip, 20, 3600):
            raise Problem(429, "too_many", retry_after=60)
        c_norm = sec.normalize_code(code)
        if not c_norm:
            raise Problem(400, "code_invalid")
        if not isinstance(world, str) or not re.fullmatch(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", world):
            raise Problem(400, "world_invalid")
        now = self.now()
        row = self.db.one("SELECT * FROM codes WHERE code_hash = ?", sec.sha256(c_norm))
        if not row or row["used_at"] or row["expires_at"] < now:
            self.limiter.add("code-fail:" + ip)
            raise Problem(404, "code_unknown")
        user = self.user(row["user_id"])
        if not user or user["disabled"] or not user["verified_at"]:
            raise Problem(404, "code_unknown")
        t = self.tier(user)
        key = "slm_" + sec.token(32)
        mod_version = (mod_version or "")[:40]
        with self.db.tx() as c:
            # Checked again inside the transaction: a second code for the same place loses.
            idx = self.check_place(user, row["visibility"])
            if c.execute("UPDATE codes SET used_at = ? WHERE code_hash = ? AND used_at IS NULL", (now, row["code_hash"])).rowcount != 1:
                raise Problem(404, "code_unknown")
            first = user["connections_total"] < self.cfg.tiers.get("first_connections_without_wait", 2)
            wait = 0 if first or user["role"] == "developer" else t["prep_minutes"] * 60
            old = None
            if row["map_id"] is not None:
                old = c.execute("SELECT * FROM maps WHERE id = ? AND user_id = ? AND state = 'archived'", (row["map_id"], user["id"])).fetchone()
            if old is None:
                old = c.execute("SELECT * FROM maps WHERE user_id = ? AND world = ? AND state = 'archived' ORDER BY archived_at DESC LIMIT 1",
                                (user["id"], world)).fetchone()
            if old is not None and old["world"] not in (None, world):
                old = None  # another world: the archived map stays as it is, the new one starts empty
            if c.execute("SELECT 1 FROM maps WHERE user_id = ? AND world = ? AND state != 'archived'", (user["id"], world)).fetchone():
                raise Problem(409, "world_connected")
            fields = dict(name=row["name"], visibility=row["visibility"], world=world, key_hash=sec.sha256(key), state="preparing",
                          slot=idx, connected_at=now, prep_min_seconds=wait, manifest_done=0, need_count=0, done_count=0,
                          ready_at=None, archived_at=None, archive_until=None, mod_version=mod_version)
            if old is not None:
                if row["slug"]:
                    fields["slug"] = row["slug"]
                c.execute("UPDATE maps SET " + ", ".join(f"{k} = ?" for k in fields) + " WHERE id = ?", (*fields.values(), old["id"]))
                c.execute("DELETE FROM pending WHERE map_id = ?", (old["id"],))
                c.execute("DELETE FROM manifest_seen WHERE map_id = ?", (old["id"],))
                map_id = old["id"]
            else:
                fields.update(user_id=user["id"], slug=row["slug"] or self.random_slug(), view_key=secrets.token_hex(16), created_at=now)
                cur = c.execute("INSERT INTO maps (" + ", ".join(fields) + ") VALUES (" + ",".join("?" * len(fields)) + ")", tuple(fields.values()))
                map_id = cur.lastrowid
            c.execute("UPDATE codes SET map_id = ? WHERE code_hash = ?", (map_id, row["code_hash"]))
            c.execute("UPDATE users SET connections_total = connections_total + 1 WHERE id = ?", (user["id"],))
            c.execute("INSERT INTO slots VALUES (?,?,?) ON CONFLICT(user_id, idx) DO UPDATE SET last_assigned_at = excluded.last_assigned_at",
                      (user["id"], idx, now))
            self.audit(c, user["id"], "connect", f"map {map_id} slot {idx} wait {wait}s{' restored' if old is not None else ''}")
        return self.db.one("SELECT * FROM maps WHERE id = ?", map_id), key

    # ---------------------------------------------------------------- preparation
    def progress(self, m) -> int:
        """0-100: the smaller of upload share and time share (spec 3: progress shows the queue honestly)."""
        if m["state"] == "ready":
            return 100
        if m["state"] != "preparing":
            return 0
        up = 0.0 if not m["manifest_done"] else (1.0 if m["need_count"] == 0 else m["done_count"] / m["need_count"])
        need = m["prep_min_seconds"]
        tm = 1.0 if need <= 0 else min(1.0, (self.now() - m["connected_at"]) / need)
        return int(math.floor(min(up, tm) * 100))

    def refresh_state(self, m):
        """preparing -> ready when every needed tile arrived and the minimum time passed."""
        if m is None or m["state"] != "preparing":
            return m
        pending = self.db.value("SELECT COUNT(*) FROM pending WHERE map_id = ?", m["id"], default=0)
        timed = self.now() - m["connected_at"] >= m["prep_min_seconds"]
        if m["manifest_done"] and pending == 0 and timed:
            with self.db.tx() as c:
                c.execute("UPDATE maps SET state = 'ready', ready_at = ? WHERE id = ? AND state = 'preparing'", (self.now(), m["id"]))
            return self.db.one("SELECT * FROM maps WHERE id = ?", m["id"])
        return m

    def queue(self):
        """Maps still uploading their first picture, in queue order (tier priority first, then who came first)."""
        rows = self.db.all("SELECT m.*, u.role, u.tier AS user_tier FROM maps m JOIN users u ON u.id = m.user_id "
                           "WHERE m.state = 'preparing' AND (m.manifest_done = 0 OR EXISTS (SELECT 1 FROM pending p WHERE p.map_id = m.id))")
        tiers = self.cfg.tiers["tiers"]

        def prio(r):
            return 9 if r["role"] == "developer" else (tiers.get(r["user_tier"]) or tiers["free"])["priority"]
        return sorted(rows, key=lambda r: (-prio(r), r["connected_at"], r["id"]))

    def admitted(self, m) -> tuple[bool, int]:
        """(may upload now, place in the queue - 0 when admitted)."""
        if m["state"] != "preparing":
            return True, 0
        q = [r["id"] for r in self.queue()]
        if m["id"] not in q:
            return True, 0
        pos = q.index(m["id"])
        slots = max(1, int(self.cfg["prep_slots"]))
        return pos < slots, 0 if pos < slots else pos - slots + 1

    def instant_ready(self, map_id: int, actor):
        """Admin: 'sofort fertig' (developer account only, spec 3): ends the minimum time, the queue and missing tiles."""
        with self.db.tx() as c:
            c.execute("DELETE FROM pending WHERE map_id = ?", (map_id,))
            c.execute("UPDATE maps SET state = 'ready', ready_at = ?, manifest_done = 1, prep_min_seconds = 0 WHERE id = ? AND state = 'preparing'",
                      (self.now(), map_id))
            self.audit(c, actor["id"], "instant_ready", f"map {map_id}")

    # ---------------------------------------------------------------- disconnect / archive
    def disconnect(self, m, actor_id: int | None = None):
        user = self.user(m["user_id"])
        t = self.tier(user)
        now = self.now()
        with self.db.tx() as c:
            c.execute("UPDATE maps SET state = 'archived', slot = NULL, archived_at = ?, archive_until = ?, live = NULL WHERE id = ?",
                      (now, now + t["archive_days"] * 86400, m["id"]))
            c.execute("DELETE FROM pending WHERE map_id = ?", (m["id"],))
            self.audit(c, actor_id or m["user_id"], "disconnect", f"map {m['id']}")
        self.live_cache.pop(m["id"], None)
        self.trim_archive(user)

    def trim_archive(self, user):
        """At most ``archive`` archived maps per tier: the oldest go first."""
        keep = self.tier(user)["archive"]
        for m in self.maps_of(user["id"], True)[keep:]:
            self.delete_map(m["id"])

    def delete_map(self, map_id: int):
        with self.db.tx() as c:
            shas = [r[0] for r in c.execute("SELECT DISTINCT sha FROM tiles WHERE map_id = ?", (map_id,))]
            c.execute("DELETE FROM tiles WHERE map_id = ?", (map_id,))
            c.execute("DELETE FROM maps WHERE id = ?", (map_id,))
            for sha in shas:
                self.drop_blob_if_unused(c, sha)
        self.live_cache.pop(map_id, None)

    # ---------------------------------------------------------------- settings
    def set_visibility(self, m, visibility: str):
        if visibility not in VISIBILITIES:
            raise Problem(400, "bad_request")
        if visibility == "public" and m["visibility"] != "public":
            user = self.user(m["user_id"])
            public = sum(1 for x in self.maps_of(user["id"]) if x["visibility"] == "public")
            if public >= self.tier(user)["public"]:
                raise Problem(409, "no_public", max=self.tier(user)["public"])
        with self.db.tx() as c:
            c.execute("UPDATE maps SET visibility = ? WHERE id = ?", (visibility, m["id"]))

    def update_map(self, m, name: str, show_players: bool, slug: str = "", dpa: bool = False):
        from .accounts import clean_name
        n = clean_name(name)
        if not n:
            raise Problem(400, "name_invalid")
        owner = self.user(m["user_id"])
        if show_players and not m["show_players"] and not owner["dpa_at"]:
            # Legal draft U3: player positions are personal data of the owner's players; the processing agreement must be
            # accepted once (time + version kept) before the switch can go on.
            if not dpa:
                raise Problem(400, "dpa_needed")
            with self.db.tx() as c:
                c.execute("UPDATE users SET dpa_version = ?, dpa_at = ? WHERE id = ?",
                          (self.cfg["legal"].get("dpa_version", "draft"), self.now(), owner["id"]))
                self.audit(c, owner["id"], "dpa", self.cfg["legal"].get("dpa_version", "draft"))
        if not show_players:
            self.live_cache.pop(m["id"], None)
        slug = (slug or "").strip().lower()
        if slug and slug != m["slug"]:
            self.check_slug(self.user(m["user_id"]), slug, m["id"])
        with self.db.tx() as c:
            c.execute("UPDATE maps SET name = ?, show_players = ?, slug = ? WHERE id = ?", (n, 1 if show_players else 0, slug or m["slug"], m["id"]))

    def new_view_key(self, m):
        with self.db.tx() as c:
            c.execute("UPDATE maps SET view_key = ? WHERE id = ?", (secrets.token_hex(16), m["id"]))

    def share(self, m, email: str):
        user = self.user(m["user_id"])
        other = self.user_by_email(email)
        if not other or other["id"] == user["id"]:
            raise Problem(404, "share_unknown")
        limit = self.tier(user)["shares"]
        count = self.db.value("SELECT COUNT(*) FROM shares WHERE map_id = ?", m["id"], default=0)
        if limit >= 0 and count >= limit:
            raise Problem(409, "share_limit", max=limit)
        with self.db.tx() as c:
            c.execute("INSERT OR IGNORE INTO shares VALUES (?,?)", (m["id"], other["id"]))

    def unshare(self, m, uid: int):
        with self.db.tx() as c:
            c.execute("DELETE FROM shares WHERE map_id = ? AND user_id = ?", (m["id"], uid))

    def shares_of(self, map_id: int):
        return self.db.all("SELECT u.id, u.email, u.name FROM shares s JOIN users u ON u.id = s.user_id WHERE s.map_id = ? ORDER BY u.email", map_id)

    def can_view(self, m, user, bearer: str | None) -> bool:
        """public: everyone; link: the view key (``#t=`` in the link) or a member; private: owner, shared accounts, developers."""
        if m["visibility"] == "public":
            return True
        if m["visibility"] == "link" and bearer and sec.same(bearer, m["view_key"]):
            return True
        if user is None:
            return False
        if user["id"] == m["user_id"] or user["role"] == "developer":
            return True
        return self.db.one("SELECT 1 FROM shares WHERE map_id = ? AND user_id = ?", m["id"], user["id"]) is not None

    # ---------------------------------------------------------------- views (spec 4: nothing is blocked above the limit)
    def count_view(self, m) -> bool:
        """Counts one page view; True when the owner's account is over its daily views (cached tiles, slower live)."""
        with self.db.tx() as c:
            c.execute("INSERT INTO views VALUES (?,?,?,1) ON CONFLICT(user_id, map_id, day) DO UPDATE SET count = count + 1",
                      (m["user_id"], m["id"], day(self.now())))
        return self.over_views(m["user_id"])

    def over_views(self, uid: int) -> bool:
        user = self.user(uid)
        today = self.db.value("SELECT SUM(count) FROM views WHERE user_id = ? AND day = ?", uid, day(self.now()), default=0)
        return today > self.tier(user)["views_day"]

    # ---------------------------------------------------------------- tier changes (admin now, billing later)
    def set_tier(self, user, tier: str, actor_id: int | None = None):
        if tier not in self.cfg.tiers["tiers"]:
            raise Problem(400, "bad_request")
        with self.db.tx() as c:
            c.execute("UPDATE users SET tier = ? WHERE id = ?", (tier, user["id"]))
            self.audit(c, actor_id or user["id"], "tier", f"user {user['id']} -> {tier}")
        user = self.user(user["id"])
        t = self.tier(user)
        active = self.maps_of(user["id"])
        days = self.cfg.tiers.get("downgrade_archive_days", 30)
        now = self.now()
        # Spec 4: on a downgrade the extra maps (newest first) wait 30 days in the archive; extra public maps become link maps.
        extra = sorted(active, key=lambda m: (m["connected_at"] or 0, m["id"]), reverse=True)[: max(0, len(active) - t["maps"])]
        extra_ids = {m["id"] for m in extra}
        with self.db.tx() as c:
            for m in extra:
                c.execute("UPDATE maps SET state = 'archived', slot = NULL, archived_at = ?, archive_until = ?, live = NULL WHERE id = ?",
                          (now, now + days * 86400, m["id"]))
                c.execute("DELETE FROM pending WHERE map_id = ?", (m["id"],))
            public = [m for m in active if m["visibility"] == "public" and m["id"] not in extra_ids]
            for m in sorted(public, key=lambda m: m["id"], reverse=True)[: max(0, len(public) - t["public"])]:
                c.execute("UPDATE maps SET visibility = 'link' WHERE id = ?", (m["id"],))
            # Places above the new count are gone; the remaining maps keep or get places 0..n-1.
            kept = [m for m in active if m["id"] not in extra_ids]
            used = sorted(m["slot"] for m in kept if m["slot"] is not None and m["slot"] < t["maps"])
            free = [i for i in range(t["maps"]) if i not in used]
            for m in kept:
                if m["slot"] is None or m["slot"] >= t["maps"]:
                    c.execute("UPDATE maps SET slot = ? WHERE id = ?", (free.pop(0), m["id"]))
        for m in extra:
            self.live_cache.pop(m["id"], None)

    # ---------------------------------------------------------------- housekeeping
    def maintenance(self):
        now = self.now()
        for m in self.db.all("SELECT id FROM maps WHERE state = 'archived' AND archive_until < ?", now):
            self.delete_map(m["id"])
        with self.db.tx() as c:
            c.execute("DELETE FROM codes WHERE expires_at < ?", (now - 86400,))
            c.execute("DELETE FROM email_tokens WHERE expires_at < ?", (now,))
            c.execute("DELETE FROM sessions WHERE expires_at < ?", (now,))
            c.execute("UPDATE users SET signup_tag = NULL WHERE signup_tag IS NOT NULL AND created_at < ?", (now - 30 * 86400,))
            c.execute("DELETE FROM views WHERE day < ?", (day(now - 400 * 86400),))
            c.execute("DELETE FROM audit WHERE at < ?", (now - 400 * 86400,))
        for uid in [r[0] for r in self.db.all("SELECT id FROM users")]:
            self.trim_archive(self.user(uid))

    # ---------------------------------------------------------------- notices (legal draft U5, DSA Art. 16/17)
    def report(self, slug: str, reason: str, text: str, contact: str, confirmed: bool, reporter, ip: str, lang: str) -> int:
        """Stores a notice about a hosted map (anyone may send one, with or without account); receipt mail when a contact
        address is given. Rate limited per address (5 per hour) and in total (300 per day)."""
        m = self.db.one("SELECT * FROM maps WHERE slug = ?", slug or "")
        if m is None:
            raise Problem(404, "not_found")
        if reason not in REPORT_REASONS:
            raise Problem(400, "bad_request")
        text = (text or "").strip()
        if not 10 <= len(text) <= 2000:
            raise Problem(400, "report_text")
        if not confirmed:
            raise Problem(400, "report_confirm")
        contact = (contact or "").strip()
        mail = sec.normalize_email(contact) if contact else None
        if contact and mail is None:
            raise Problem(400, "email_invalid")
        if reporter is not None and mail is None:
            mail = reporter["email"]
        if not self.limiter.allow(f"report:{ip}", 5, 3600) or not self.limiter.allow("report:all", 300, 86400):
            raise Problem(429, "too_many")
        with self.db.tx() as c:
            rid = c.execute("INSERT INTO reports (at, map_id, slug, reason, text, contact, lang, reporter_id) VALUES (?,?,?,?,?,?,?,?)",
                            (self.now(), m["id"], m["slug"], reason, text, mail or "", "de" if lang == "de" else "en",
                             reporter["id"] if reporter else None)).lastrowid
        if mail:
            self.mail_text(mail, lang, "mail_report_subject", "mail_report_body", map=m["name"], id=rid)
        return rid

    def reports(self, state: str = "open", limit: int = 100):
        return self.db.all("SELECT r.*, m.name AS map_name, m.state AS map_state, m.user_id AS owner_id FROM reports r "
                           "LEFT JOIN maps m ON m.id = r.map_id WHERE r.state = ? ORDER BY r.at DESC LIMIT ?", state, limit)

    def handle_report(self, rid: int, action: str, actor, note: str = ""):
        """Developer decision: ``dismiss`` (nothing to do) or ``disconnect`` (map goes to the owner's archive, owner gets
        the reason - statement of reasons, DSA Art. 17). The notifier hears the decision when a contact is known."""
        r = self.db.one("SELECT * FROM reports WHERE id = ? AND state = 'open'", rid)
        if r is None or action not in ("dismiss", "disconnect"):
            raise Problem(400, "bad_request")
        note = (note or "").strip()[:1000]
        m = self.db.one("SELECT * FROM maps WHERE id = ?", r["map_id"]) if r["map_id"] else None
        if action == "disconnect" and m is not None and m["state"] != "archived":
            self.disconnect(m, actor["id"])
            owner = self.user(m["user_id"])
            if owner:
                self.mail_text(owner["email"], owner["lang"] or "en", "mail_report_owner_subject", "mail_report_owner_body",
                               map=m["name"], reason=r["reason"], note=note)
        with self.db.tx() as c:
            c.execute("UPDATE reports SET state = ?, handled_at = ?, handled_by = ?, note = ? WHERE id = ?",
                      ("actioned" if action == "disconnect" else "dismissed", self.now(), actor["id"], note, rid))
            self.audit(c, actor["id"], "report_" + action, f"report {rid} map {r['map_id']}")
        if r["contact"]:
            lang = r["lang"]
            decision = {"dismiss": ("no action", "keine Maßnahme"), "disconnect": ("map disconnected", "Karte getrennt")}[action]
            from .i18n import t
            self.mailer.send(r["contact"], t(lang, "mail_report_done_subject"),
                             t(lang, "mail_report_done_body", id=r["id"], map=m["name"] if m is not None else r["slug"],
                               decision=decision[1] if lang == "de" else decision[0], note=note))

    def flags(self) -> list[dict]:
        """Admin list 'auffällige Muster' (spec 3): several accounts of one mailbox or one sign-up address, many place changes."""
        out = []
        for r in self.db.all("SELECT email_canon, COUNT(*) n, GROUP_CONCAT(email, ', ') emails FROM users GROUP BY email_canon HAVING n > 1"):
            out.append({"kind": "mailbox", "detail": r["emails"], "count": r["n"]})
        for r in self.db.all("SELECT signup_tag, COUNT(*) n, GROUP_CONCAT(email, ', ') emails FROM users WHERE signup_tag IS NOT NULL GROUP BY signup_tag HAVING n > 2"):
            out.append({"kind": "address", "detail": r["emails"], "count": r["n"]})
        week = self.now() - 7 * 86400
        for r in self.db.all("SELECT a.user_id, u.email, COUNT(*) n FROM audit a JOIN users u ON u.id = a.user_id "
                             "WHERE a.action IN ('connect', 'disconnect') AND a.at > ? GROUP BY a.user_id HAVING n >= 6", week):
            out.append({"kind": "churn", "detail": r["email"], "count": r["n"]})
        return out
