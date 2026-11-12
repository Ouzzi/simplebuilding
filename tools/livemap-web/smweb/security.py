"""Security building blocks of the Simple Mods account service (N34, plan D2-D5).

Passwords: PBKDF2-SHA256, 600 000 rounds, stored as ``pbkdf2_sha256$<iter>$<salt hex>$<hash hex>`` - the same format as
the wiki login (tools/zentrale-web/auth.py), so those accounts can move into this database later without a reset.
Random tokens come from ``secrets``; the database only ever stores their SHA-256. Nothing here logs a secret.
"""
from __future__ import annotations

import hashlib
import hmac
import re
import secrets
import threading
import time
from collections import deque

ITERATIONS = 600_000
PW_MIN, PW_MAX = 10, 256
# A short list of passwords that are always refused (the full lists would need a download; length 10 removes most).
COMMON = {
    "1234567890", "12345678910", "0123456789", "qwertzuiop", "qwertyuiop", "password12", "password123", "passwort12",
    "passwort123", "minecraft1", "minecraft12", "minecraft123", "iloveyou12", "abcdefghij", "1q2w3e4r5t", "1111111111",
    "aaaaaaaaaa", "letmein123", "welcome123", "football12", "simplemods", "simplemods1", "livemap123", "qwerty1234",
}
CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  # no 0/O, 1/I: easy to type in chat
CODE_LEN = 8
EMAIL_RE = re.compile(r"^[^@\s<>\"'`;,()]{1,64}@[A-Za-z0-9](?:[A-Za-z0-9-]{0,62}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,62}[A-Za-z0-9])?)+$")


def hash_password(password: str, iterations: int | None = None) -> str:
    iterations = iterations or ITERATIONS  # read at call time (tests lower it)
    salt = secrets.token_bytes(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, iterations)
    return f"pbkdf2_sha256${iterations}${salt.hex()}${dk.hex()}"


def verify_password(password: str, stored: str | None) -> bool:
    try:
        algo, it, salt, want = (stored or "").split("$")
        if algo != "pbkdf2_sha256" or not 1000 <= int(it) <= 10_000_000:
            return False
        dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), bytes.fromhex(salt), int(it))
    except (ValueError, AttributeError):
        return False
    return hmac.compare_digest(dk.hex(), want)


_DUMMY: list[str] = []


def burn_time(password: str) -> bool:
    """Same hashing cost for an unknown e-mail as for a known one (no account enumeration by timing); always False."""
    if not _DUMMY:
        _DUMMY.append(hash_password(secrets.token_hex(8)))
    verify_password(password, _DUMMY[0])
    return False


def password_problem(password: str, email: str = "") -> str | None:
    """i18n key of the first problem, or None when the password is acceptable."""
    if not isinstance(password, str) or len(password) < PW_MIN:
        return "pw_short"
    if len(password) > PW_MAX:
        return "pw_long"
    low = password.lower()
    if low in COMMON or len(set(low)) <= 2:
        return "pw_common"
    if email and low in (email.lower(), email.split("@")[0].lower()):
        return "pw_email"
    return None


def normalize_email(value: str) -> str | None:
    """Trimmed, lower-cased address, or None when it does not look like one (no IDN, no quoted local parts)."""
    if not isinstance(value, str):
        return None
    e = value.strip().lower()
    if len(e) > 254 or not EMAIL_RE.match(e) or ".." in e:
        return None
    return e


def canonical_email(email: str) -> str:
    """The address as one mailbox: '+tag' removed everywhere, dots removed for Gmail (admin flag for many free accounts)."""
    local, _, domain = email.partition("@")
    local = local.split("+", 1)[0]
    if domain in ("gmail.com", "googlemail.com"):
        local, domain = local.replace(".", ""), "gmail.com"
    return f"{local}@{domain}"


def token(nbytes: int = 32) -> str:
    return secrets.token_urlsafe(nbytes)


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def new_code() -> str:
    return "".join(secrets.choice(CODE_ALPHABET) for _ in range(CODE_LEN))


def normalize_code(value: str) -> str | None:
    """'abcd-efgh' / 'ABCD EFGH' -> 'ABCDEFGH'; None for anything that cannot be a code."""
    if not isinstance(value, str):
        return None
    c = re.sub(r"[\s-]", "", value).upper()
    if len(c) != CODE_LEN or any(ch not in CODE_ALPHABET for ch in c):
        return None
    return c


def pretty_code(code: str) -> str:
    return code[:4] + "-" + code[4:]


def hmac_hex(secret: bytes, *parts: str) -> str:
    return hmac.new(secret, "|".join(parts).encode("utf-8"), hashlib.sha256).hexdigest()


def same(a: str | None, b: str | None) -> bool:
    return isinstance(a, str) and isinstance(b, str) and hmac.compare_digest(a.encode("utf-8"), b.encode("utf-8"))


class Limiter:
    """Sliding-window counters in memory (per process; addresses are kept at most one window, never written to disk)."""

    MAX_KEYS = 50_000

    def __init__(self, clock=time.time):
        self.clock = clock
        self.lock = threading.Lock()
        self.hits: dict[str, deque] = {}

    def _window(self, key: str, window: float) -> deque:
        now = self.clock()
        q = self.hits.get(key)
        if q is None:
            if len(self.hits) >= self.MAX_KEYS:
                self._sweep(now)
            q = self.hits[key] = deque()
        while q and now - q[0] >= window:
            q.popleft()
        return q

    def _sweep(self, now: float):
        for k in [k for k, q in self.hits.items() if not q or now - q[-1] > 3600]:
            del self.hits[k]
        if len(self.hits) >= self.MAX_KEYS:  # still full: drop the oldest half (fail open on memory, not on security keys)
            for k in sorted(self.hits, key=lambda k: self.hits[k][-1] if self.hits[k] else 0)[: self.MAX_KEYS // 2]:
                del self.hits[k]

    def allow(self, key: str, limit: int, window: float) -> bool:
        """Counts this hit; False when it is over ``limit`` within ``window`` seconds."""
        with self.lock:
            q = self._window(key, window)
            if len(q) >= limit:
                return False
            q.append(self.clock())
            return True

    def blocked(self, key: str, limit: int, window: float) -> int:
        """Seconds until ``key`` is below ``limit`` again (0 = free), without counting a hit."""
        with self.lock:
            q = self._window(key, window)
            if len(q) < limit:
                return 0
            return max(1, int(window - (self.clock() - q[0])) + 1)

    def add(self, key: str):
        with self.lock:
            self._window(key, 3600).append(self.clock())

    def clear(self, key: str):
        with self.lock:
            self.hits.pop(key, None)
