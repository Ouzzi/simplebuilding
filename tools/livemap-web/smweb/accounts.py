"""Accounts of the central Simple Mods login (spec section 5): register with e-mail confirmation, login, logout, password
reset, sessions for all *.simplemods.de, profile, data export, deleting the account. Answers never tell whether an
e-mail address has an account (same text, same hashing time)."""
from __future__ import annotations

from . import security as sec
from .errors import Problem

VERIFY_SECONDS = 24 * 3600
RESET_SECONDS = 3600
LOGIN_FAILS, LOGIN_WINDOW = 5, 15 * 60
REGISTER_PER_HOUR = 5
FORGOT_PER_MAIL, FORGOT_PER_IP = 3, 10
SIGNUP_TAG_DAYS = 30


class AccountsMixin:
    # ---------------------------------------------------------------- register / confirm
    def register(self, email: str, password: str, name: str, lang: str, ip: str) -> None:
        if not self.cfg["registration"]:
            raise Problem(403, "register_closed")
        if not self.limiter.allow("reg-ip:" + ip, REGISTER_PER_HOUR, 3600):
            raise Problem(429, "too_many")
        e = sec.normalize_email(email)
        if not e:
            raise Problem(400, "email_invalid")
        problem = sec.password_problem(password, e)
        if problem:
            raise Problem(400, problem)
        name = clean_name(name) or e.split("@")[0][:40]
        now = self.now()
        existing = self.db.one("SELECT id, verified_at FROM users WHERE email = ?", e)
        if existing:
            # Same answer as for a new address; the owner of the mailbox gets a hint instead of a second account.
            sec.hash_password(password)
            self.mail_text(e, lang, "mail_exists_subject", "mail_exists_body", link=self.cfg.url("account", "/forgot"))
            return
        pw = sec.hash_password(password)
        tag = sec.hmac_hex(self.cfg.secret, "signup", ip)[:16]
        with self.db.tx() as c:
            # Legal draft U2: which terms version was accepted and when (registration requires the box).
            cur = c.execute("INSERT INTO users (email, email_canon, name, pw_hash, lang, created_at, signup_tag, terms_version, terms_at) "
                            "VALUES (?,?,?,?,?,?,?,?,?)", (e, sec.canonical_email(e), name, pw, lang if lang in ("en", "de") else "", now, tag,
                                                          self.cfg["legal"].get("terms_version", "draft"), now))
            uid = cur.lastrowid
            self.audit(c, uid, "register", "")
        self.send_verify(uid)

    def send_verify(self, uid: int) -> None:
        u = self.user(uid)
        if not u or u["verified_at"]:
            return
        if not self.limiter.allow(f"verify-mail:{uid}", 3, 3600):
            raise Problem(429, "too_many")
        t = sec.token()
        with self.db.tx() as c:
            c.execute("DELETE FROM email_tokens WHERE user_id = ? AND kind = 'verify'", (uid,))
            c.execute("INSERT INTO email_tokens VALUES (?,?,?,?)", (sec.sha256(t), uid, "verify", self.now() + VERIFY_SECONDS))
        self.mail_text(u["email"], u["lang"], "mail_verify_subject", "mail_verify_body", link=self.cfg.url("account", "/verify?t=" + t))

    def verify(self, token: str) -> int:
        row = self.take_token(token, "verify")
        with self.db.tx() as c:
            c.execute("UPDATE users SET verified_at = ? WHERE id = ? AND verified_at IS NULL", (self.now(), row["user_id"]))
            self.audit(c, row["user_id"], "verify", "")
        return row["user_id"]

    def take_token(self, token: str, kind: str):
        if not isinstance(token, str) or not 20 <= len(token) <= 100:
            raise Problem(400, "link_invalid")
        h = sec.sha256(token)
        row = self.db.one("SELECT * FROM email_tokens WHERE token_hash = ? AND kind = ?", h, kind)
        if not row or row["expires_at"] < self.now():
            raise Problem(400, "link_invalid")
        with self.db.tx() as c:
            c.execute("DELETE FROM email_tokens WHERE token_hash = ?", (h,))
        return row

    # ---------------------------------------------------------------- login / sessions
    def login(self, email: str, password: str, ip: str) -> str:
        e = sec.normalize_email(email) or ""
        wait = max(self.limiter.blocked("login-ip:" + ip, LOGIN_FAILS, LOGIN_WINDOW),
                   self.limiter.blocked("login-mail:" + e, LOGIN_FAILS, LOGIN_WINDOW))
        if wait:
            raise Problem(429, "login_locked", minutes=max(1, wait // 60))
        u = self.db.one("SELECT * FROM users WHERE email = ?", e) if e else None
        ok = sec.verify_password(password, u["pw_hash"]) if u else sec.burn_time(password)
        if not ok or u["disabled"]:
            self.limiter.add("login-ip:" + ip)
            self.limiter.add("login-mail:" + e)
            raise Problem(401, "login_failed")
        self.limiter.clear("login-mail:" + e)
        return self.new_session(u["id"])

    def new_session(self, uid: int) -> str:
        t = sec.token()
        now = self.now()
        days = max(1, min(90, int(self.cfg["cookie"].get("days", 30))))
        with self.db.tx() as c:
            c.execute("INSERT INTO sessions VALUES (?,?,?,?)", (sec.sha256(t), uid, now, now + days * 86400))
            c.execute("DELETE FROM sessions WHERE user_id = ? AND expires_at < ?", (uid, now))
        return t

    def session_user(self, token: str | None):
        if not token or len(token) > 100:
            return None
        row = self.db.one("SELECT u.*, s.token_hash AS sid FROM sessions s JOIN users u ON u.id = s.user_id "
                          "WHERE s.token_hash = ? AND s.expires_at > ?", sec.sha256(token), self.now())
        return row if row and not row["disabled"] else None

    def logout(self, token: str | None):
        if token:
            with self.db.tx() as c:
                c.execute("DELETE FROM sessions WHERE token_hash = ?", (sec.sha256(token),))

    def logout_others(self, user, keep_sid: str):
        with self.db.tx() as c:
            c.execute("DELETE FROM sessions WHERE user_id = ? AND token_hash != ?", (user["id"], keep_sid))
            self.audit(c, user["id"], "logout_others", "")

    def session_count(self, uid: int) -> int:
        return self.db.value("SELECT COUNT(*) FROM sessions WHERE user_id = ? AND expires_at > ?", uid, self.now(), default=0)

    # ---------------------------------------------------------------- password
    def forgot(self, email: str, lang: str, ip: str) -> None:
        e = sec.normalize_email(email)
        if not self.limiter.allow("forgot-ip:" + ip, FORGOT_PER_IP, 3600):
            raise Problem(429, "too_many")
        if not e or not self.limiter.allow("forgot-mail:" + e, FORGOT_PER_MAIL, 3600):
            return  # same answer: no hint whether the address exists or was asked too often
        u = self.db.one("SELECT * FROM users WHERE email = ?", e)
        if not u or u["disabled"]:
            return
        t = sec.token()
        with self.db.tx() as c:
            c.execute("DELETE FROM email_tokens WHERE user_id = ? AND kind = 'reset'", (u["id"],))
            c.execute("INSERT INTO email_tokens VALUES (?,?,?,?)", (sec.sha256(t), u["id"], "reset", self.now() + RESET_SECONDS))
        self.mail_text(e, u["lang"] or lang, "mail_reset_subject", "mail_reset_body", link=self.cfg.url("account", "/reset?t=" + t))

    def check_reset(self, token: str):
        if not isinstance(token, str) or not 20 <= len(token) <= 100:
            raise Problem(400, "link_invalid")
        row = self.db.one("SELECT * FROM email_tokens WHERE token_hash = ? AND kind = 'reset'", sec.sha256(token))
        if not row or row["expires_at"] < self.now():
            raise Problem(400, "link_invalid")
        return row

    def reset(self, token: str, password: str) -> int:
        row = self.check_reset(token)
        u = self.user(row["user_id"])
        problem = sec.password_problem(password, u["email"])
        if problem:
            raise Problem(400, problem)
        self.take_token(token, "reset")
        with self.db.tx() as c:
            # The mailbox owner proved access: the address counts as confirmed; every old session ends.
            c.execute("UPDATE users SET pw_hash = ?, verified_at = COALESCE(verified_at, ?) WHERE id = ?",
                      (sec.hash_password(password), self.now(), u["id"]))
            c.execute("DELETE FROM sessions WHERE user_id = ?", (u["id"],))
            self.audit(c, u["id"], "reset", "")
        return u["id"]

    def change_password(self, user, old: str, new: str):
        if not sec.verify_password(old, user["pw_hash"]):
            raise Problem(400, "pw_wrong")
        problem = sec.password_problem(new, user["email"])
        if problem:
            raise Problem(400, problem)
        with self.db.tx() as c:
            c.execute("UPDATE users SET pw_hash = ? WHERE id = ?", (sec.hash_password(new), user["id"]))
            c.execute("DELETE FROM sessions WHERE user_id = ? AND token_hash != ?", (user["id"], user["sid"]))
            self.audit(c, user["id"], "password", "")

    def set_profile(self, user, name: str, lang: str):
        n = clean_name(name)
        if not n:
            raise Problem(400, "name_invalid")
        with self.db.tx() as c:
            c.execute("UPDATE users SET name = ?, lang = ? WHERE id = ?", (n, lang if lang in ("en", "de") else "", user["id"]))

    # ---------------------------------------------------------------- data protection
    def export(self, user) -> dict:
        """Everything stored about the account (spec 5): no hashes, no keys, no tiles (those are pictures of the world)."""
        maps = [{k: m[k] for k in ("slug", "name", "visibility", "state", "created_at", "connected_at", "ready_at", "archived_at",
                                     "archive_until", "show_players", "bytes", "mod_version")}
                | {"tiles": self.db.value("SELECT COUNT(*) FROM tiles WHERE map_id = ?", m["id"], default=0),
                   "shared_with": [r[0] for r in self.db.all("SELECT u.email FROM shares s JOIN users u ON u.id = s.user_id WHERE s.map_id = ?", m["id"])]}
                for m in self.db.all("SELECT * FROM maps WHERE user_id = ? ORDER BY id", user["id"])]
        return {
            "account": {k: user[k] for k in ("email", "name", "role", "tier", "lang", "created_at", "verified_at", "connections_total",
                                             "terms_version", "terms_at", "dpa_version", "dpa_at")},
            "sessions": self.session_count(user["id"]),
            "maps": maps,
            "shared_with_me": [r[0] for r in self.db.all("SELECT m.slug FROM shares s JOIN maps m ON m.id = s.map_id WHERE s.user_id = ?", user["id"])],
            "views": [dict(r) for r in self.db.all("SELECT day, SUM(count) AS views FROM views WHERE user_id = ? GROUP BY day ORDER BY day", user["id"])],
            "audit": [dict(r) for r in self.db.all("SELECT at, action FROM audit WHERE user_id = ? ORDER BY id", user["id"])],
        }

    def delete_account(self, user, password: str):
        if not sec.verify_password(password, user["pw_hash"]):
            raise Problem(400, "pw_wrong")
        for m in self.db.all("SELECT id FROM maps WHERE user_id = ?", user["id"]):
            self.delete_map(m["id"])
        with self.db.tx() as c:
            c.execute("DELETE FROM users WHERE id = ?", (user["id"],))
            c.execute("DELETE FROM audit WHERE user_id = ?", (user["id"],))
            c.execute("INSERT INTO audit (at, user_id, action) VALUES (?, NULL, 'account_deleted')", (self.now(),))

    # ---------------------------------------------------------------- helpers
    def user(self, uid: int):
        return self.db.one("SELECT * FROM users WHERE id = ?", uid)

    def user_by_email(self, email: str):
        e = sec.normalize_email(email)
        return self.db.one("SELECT * FROM users WHERE email = ?", e) if e else None

    def audit(self, c, uid, action: str, detail: str):
        c.execute("INSERT INTO audit (at, user_id, action, detail) VALUES (?,?,?,?)", (self.now(), uid, action, detail[:200]))

    def mail_text(self, to: str, lang: str, subject_key: str, body_key: str, **params):
        from .i18n import t
        self.mailer.send(to, t(lang, subject_key), t(lang, body_key, **params))


def clean_name(value) -> str:
    if not isinstance(value, str):
        return ""
    v = " ".join(value.split())
    v = "".join(ch for ch in v if ch.isprintable() and ch not in "<>\"'`")
    return v[:40]
