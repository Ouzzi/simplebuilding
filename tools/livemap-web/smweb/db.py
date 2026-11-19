"""SQLite database of the Simple Mods accounts and the Simple Livemap service (one file, WAL, one connection per thread).

Secrets are stored only as SHA-256 (session tokens, e-mail links, connect codes, map keys). Schema changes go through
``MIGRATIONS`` (append only; ``user_version`` tells how far a database is).
"""
from __future__ import annotations

import sqlite3
import threading
from contextlib import contextmanager
from pathlib import Path

MIGRATIONS = [
    """
    CREATE TABLE users (
        id INTEGER PRIMARY KEY,
        email TEXT NOT NULL UNIQUE,
        email_canon TEXT NOT NULL,
        name TEXT NOT NULL DEFAULT '',
        pw_hash TEXT NOT NULL,
        role TEXT NOT NULL DEFAULT 'user',
        tier TEXT NOT NULL DEFAULT 'free',
        lang TEXT NOT NULL DEFAULT '',
        verified_at INTEGER,
        created_at INTEGER NOT NULL,
        connections_total INTEGER NOT NULL DEFAULT 0,
        signup_tag TEXT,
        disabled INTEGER NOT NULL DEFAULT 0,
        billing_customer TEXT,
        billing_until INTEGER,
        terms_version TEXT,
        terms_at INTEGER,
        dpa_version TEXT,
        dpa_at INTEGER
    );
    CREATE INDEX users_canon ON users(email_canon);
    CREATE INDEX users_tag ON users(signup_tag);
    CREATE TABLE sessions (
        token_hash TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        created_at INTEGER NOT NULL,
        expires_at INTEGER NOT NULL
    );
    CREATE INDEX sessions_user ON sessions(user_id);
    CREATE TABLE email_tokens (
        token_hash TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        kind TEXT NOT NULL,
        expires_at INTEGER NOT NULL
    );
    CREATE TABLE slots (
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        idx INTEGER NOT NULL,
        last_assigned_at INTEGER NOT NULL,
        PRIMARY KEY (user_id, idx)
    );
    CREATE TABLE maps (
        id INTEGER PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        slug TEXT NOT NULL UNIQUE,
        name TEXT NOT NULL,
        visibility TEXT NOT NULL DEFAULT 'private',
        view_key TEXT NOT NULL,
        world TEXT,
        key_hash TEXT UNIQUE,
        state TEXT NOT NULL,
        slot INTEGER,
        created_at INTEGER NOT NULL,
        connected_at INTEGER,
        prep_min_seconds INTEGER NOT NULL DEFAULT 0,
        manifest_done INTEGER NOT NULL DEFAULT 0,
        need_count INTEGER NOT NULL DEFAULT 0,
        done_count INTEGER NOT NULL DEFAULT 0,
        ready_at INTEGER,
        archived_at INTEGER,
        archive_until INTEGER,
        show_players INTEGER NOT NULL DEFAULT 0,
        bytes INTEGER NOT NULL DEFAULT 0,
        live TEXT,
        live_at INTEGER,
        tiles_at INTEGER,
        mod_version TEXT NOT NULL DEFAULT ''
    );
    CREATE INDEX maps_user ON maps(user_id, state);
    CREATE INDEX maps_world ON maps(user_id, world);
    CREATE TABLE tiles (
        map_id INTEGER NOT NULL REFERENCES maps(id) ON DELETE CASCADE,
        path TEXT NOT NULL,
        sha TEXT NOT NULL,
        size INTEGER NOT NULL,
        PRIMARY KEY (map_id, path)
    );
    CREATE INDEX tiles_sha ON tiles(sha);
    CREATE TABLE pending (
        map_id INTEGER NOT NULL REFERENCES maps(id) ON DELETE CASCADE,
        path TEXT NOT NULL,
        PRIMARY KEY (map_id, path)
    );
    CREATE TABLE blobs (sha TEXT PRIMARY KEY, size INTEGER NOT NULL);
    CREATE TABLE codes (
        code_hash TEXT PRIMARY KEY,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        name TEXT NOT NULL,
        visibility TEXT NOT NULL,
        slug TEXT,
        map_id INTEGER,
        created_at INTEGER NOT NULL,
        expires_at INTEGER NOT NULL,
        used_at INTEGER
    );
    CREATE INDEX codes_user ON codes(user_id);
    CREATE TABLE shares (
        map_id INTEGER NOT NULL REFERENCES maps(id) ON DELETE CASCADE,
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        PRIMARY KEY (map_id, user_id)
    );
    CREATE TABLE views (
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        map_id INTEGER NOT NULL,
        day TEXT NOT NULL,
        count INTEGER NOT NULL DEFAULT 0,
        PRIMARY KEY (user_id, map_id, day)
    );
    CREATE TABLE audit (
        id INTEGER PRIMARY KEY,
        at INTEGER NOT NULL,
        user_id INTEGER,
        action TEXT NOT NULL,
        detail TEXT NOT NULL DEFAULT ''
    );
    CREATE INDEX audit_user ON audit(user_id, action, at);
    """,
    # 2: Simple Mods Pro (N108): orders with the consents (kept after an account deletion: proof, user_id becomes NULL), mirror of
    # the Stripe subscriptions, entitlements (account -> plan -> perks), processed webhook events (idempotency), cancellations
    # via the cancel button (section 312k BGB), sent one-off notices.
    """
    CREATE TABLE orders (
        id INTEGER PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
        plan TEXT NOT NULL,
        interval TEXT NOT NULL,
        price_cents INTEGER NOT NULL,
        terms_version TEXT NOT NULL,
        terms_at INTEGER NOT NULL,
        waiver INTEGER NOT NULL DEFAULT 0,
        waiver_text TEXT NOT NULL DEFAULT '',
        created_at INTEGER NOT NULL,
        session_id TEXT UNIQUE,
        subscription_id TEXT,
        completed_at INTEGER
    );
    CREATE INDEX orders_user ON orders(user_id, created_at);
    CREATE TABLE subscriptions (
        id TEXT PRIMARY KEY,
        user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
        customer TEXT NOT NULL,
        plan TEXT NOT NULL,
        interval TEXT NOT NULL DEFAULT 'month',
        status TEXT NOT NULL,
        period_end INTEGER,
        cancel_at_period_end INTEGER NOT NULL DEFAULT 0,
        schedule_id TEXT,
        order_id INTEGER,
        event_at INTEGER NOT NULL DEFAULT 0,
        updated_at INTEGER NOT NULL
    );
    CREATE INDEX subscriptions_user ON subscriptions(user_id);
    CREATE INDEX subscriptions_customer ON subscriptions(customer);
    CREATE TABLE entitlements (
        user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
        plan TEXT NOT NULL,
        perks TEXT NOT NULL,
        livemap_tier TEXT NOT NULL,
        source TEXT NOT NULL,
        subscription_id TEXT,
        starts_at INTEGER NOT NULL,
        valid_until INTEGER,
        updated_at INTEGER NOT NULL
    );
    CREATE TABLE stripe_events (
        id TEXT PRIMARY KEY,
        type TEXT NOT NULL,
        created INTEGER NOT NULL DEFAULT 0,
        received_at INTEGER NOT NULL
    );
    CREATE TABLE cancellations (
        id INTEGER PRIMARY KEY,
        at INTEGER NOT NULL,
        email TEXT NOT NULL,
        user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
        subscription_id TEXT,
        kind TEXT NOT NULL,
        reason TEXT NOT NULL DEFAULT '',
        effective_at INTEGER,
        state TEXT NOT NULL
    );
    CREATE TABLE notices (
        user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        kind TEXT NOT NULL,
        ref TEXT NOT NULL,
        at INTEGER NOT NULL,
        PRIMARY KEY (user_id, kind, ref)
    );
    """,
    # 3: owner 19.11.2026 (N109): "Pro" -> "Max" (plans), livemap tier "pro" -> "basic"; fellow-player seats (P039). The code also
    # accepts the old ids on read (config.LIVEMAP_ALIAS / PLAN_ALIAS), so a half-migrated copy still works.
    """
    UPDATE users SET tier = 'basic' WHERE tier = 'pro';
    UPDATE entitlements SET livemap_tier = 'basic' WHERE livemap_tier = 'pro';
    UPDATE entitlements SET plan = 'max' WHERE plan = 'pro';
    UPDATE subscriptions SET plan = 'max' WHERE plan = 'pro';
    UPDATE orders SET plan = 'max' WHERE plan = 'pro';
    CREATE TABLE lite_seats (
        id INTEGER PRIMARY KEY,
        owner_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        member_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
        created_at INTEGER NOT NULL,
        removed_at INTEGER
    );
    CREATE UNIQUE INDEX lite_member_active ON lite_seats(member_id) WHERE removed_at IS NULL;
    CREATE INDEX lite_owner ON lite_seats(owner_id, removed_at);
    """,
    # 4: owner answer part 2 (19.11.2026): plan change at the period end (P070), coupons (P074), gift subscriptions bound to a
    # Minecraft name (P068), contact form counter-free (mails only).
    """
    ALTER TABLE subscriptions ADD COLUMN pending_plan TEXT;
    ALTER TABLE orders ADD COLUMN kind TEXT NOT NULL DEFAULT 'subscription';
    ALTER TABLE orders ADD COLUMN months INTEGER;
    ALTER TABLE orders ADD COLUMN coupon TEXT;
    CREATE TABLE gifts (
        id INTEGER PRIMARY KEY,
        buyer_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
        order_id INTEGER NOT NULL UNIQUE,
        plan TEXT NOT NULL,
        months INTEGER NOT NULL,
        state TEXT NOT NULL,
        starts_at INTEGER,
        ends_at INTEGER,
        created_at INTEGER NOT NULL
    );
    CREATE TABLE gift_names (
        id INTEGER PRIMARY KEY,
        gift_id INTEGER NOT NULL REFERENCES gifts(id) ON DELETE CASCADE,
        mc_name TEXT NOT NULL,
        mc_lower TEXT NOT NULL,
        set_at INTEGER NOT NULL,
        removed_at INTEGER
    );
    CREATE INDEX gift_names_lower ON gift_names(mc_lower, removed_at);
    """,
]


class Database:
    def __init__(self, path: Path):
        self.path = Path(path)
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.local = threading.local()
        self.write_lock = threading.RLock()
        self.migrate()

    def conn(self) -> sqlite3.Connection:
        c = getattr(self.local, "conn", None)
        if c is None:
            c = sqlite3.connect(self.path, timeout=10, isolation_level=None, check_same_thread=False)
            c.row_factory = sqlite3.Row
            c.execute("PRAGMA foreign_keys = ON")
            c.execute("PRAGMA journal_mode = WAL")
            c.execute("PRAGMA busy_timeout = 10000")
            self.local.conn = c
        return c

    def close(self):
        c = getattr(self.local, "conn", None)
        if c is not None:
            c.close()
            self.local.conn = None

    def migrate(self):
        c = self.conn()
        version = c.execute("PRAGMA user_version").fetchone()[0]
        for i, sql in enumerate(MIGRATIONS[version:], start=version + 1):
            c.executescript("BEGIN;" + sql + f"; PRAGMA user_version = {i}; COMMIT;")

    @contextmanager
    def tx(self):
        """One write transaction (BEGIN IMMEDIATE), serialized in this process."""
        c = self.conn()
        with self.write_lock:
            c.execute("BEGIN IMMEDIATE")
            try:
                yield c
            except BaseException:
                c.execute("ROLLBACK")
                raise
            c.execute("COMMIT")

    def one(self, sql: str, *args):
        return self.conn().execute(sql, args).fetchone()

    def all(self, sql: str, *args):
        return self.conn().execute(sql, args).fetchall()

    def value(self, sql: str, *args, default=None):
        row = self.one(sql, *args)
        return default if row is None or row[0] is None else row[0]
