"""Login for the dev area (a-zw06): password sessions now, Cloudflare Access JWT as a prepared switch.

auth.json (outside the repo, /srv/devqueue/auth.json, mode 600, written only by set_password.py):
    {"secret": "<hex>", "session_days": 30,
     "users": {"<name>": {"hash": "pbkdf2_sha256$<iter>$<salt hex>$<hash hex>", "email": "<identity>"}},
     "cloudflare": {"enabled": false, "team_domain": "<team>.cloudflareaccess.com", "aud": "<AUD tag>"}}

The identity (email) is what the rest of devqueue keys on (mod selections, "by" of saves), so it stays the address
Cloudflare Access used before. Nothing here ever logs or returns a password or a hash.
"""
import base64
import hashlib
import hmac
import json
import os
import secrets
import threading
import time
import urllib.request
from pathlib import Path

COOKIE = "sb_session"
ITERATIONS = 600_000
MAX_FAILS = 5            # per client address and per user name ...
FAIL_WINDOW = 15 * 60    # ... within this window, then locked for the rest of it
DEFAULT_DAYS = 30
CERTS_TTL = 3600


def hash_password(password: str, iterations: int = ITERATIONS) -> str:
    salt = secrets.token_bytes(16)
    dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, iterations)
    return f"pbkdf2_sha256${iterations}${salt.hex()}${dk.hex()}"


def verify_password(password: str, stored: str) -> bool:
    try:
        algo, it, salt, want = stored.split("$")
        if algo != "pbkdf2_sha256":
            return False
        dk = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), bytes.fromhex(salt), int(it))
    except (ValueError, AttributeError):
        return False
    return hmac.compare_digest(dk.hex(), want)


def safe_name(name: str) -> bool:
    return isinstance(name, str) and 0 < len(name) <= 64 and all(c.isascii() and (c.isalnum() or c in "._-@") for c in name)


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def unb64url(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def safe_next(value: str | None) -> str:
    """Only local paths ("/x"), never "//host" or "/\\host" (open redirect)."""
    if not value or not value.startswith("/") or value.startswith("//") or "\\" in value:
        return "/"
    if any(ord(c) < 32 or ord(c) == 127 for c in value):
        return "/"
    return value[:500]


# ---- RS256 (Cloudflare Access) with the standard library only ----
_SHA256_PREFIX = bytes.fromhex("3031300d060960864801650304020105000420")


def rs256_verify(signing_input: bytes, signature: bytes, n: int, e: int) -> bool:
    k = (n.bit_length() + 7) // 8
    if len(signature) != k:
        return False
    em = pow(int.from_bytes(signature, "big"), e, n).to_bytes(k, "big")
    t = _SHA256_PREFIX + hashlib.sha256(signing_input).digest()
    expected = b"\x00\x01" + b"\xff" * (k - len(t) - 3) + b"\x00" + t
    return hmac.compare_digest(em, expected)


class Auth:
    def __init__(self, path: Path):
        self.path = Path(path)
        self.lock = threading.Lock()
        self._mtime = None
        self._conf: dict = {}
        self._fails: dict[str, list[float]] = {}
        self._certs: tuple[float, dict] = (0.0, {})
        self.fetch_certs = self._fetch_certs  # replaceable in tests

    # ---- config ----
    def conf(self) -> dict:
        """auth.json, re-read when it changes (set_password.py works without a restart)."""
        try:
            mtime = self.path.stat().st_mtime_ns
        except OSError:
            self._mtime, self._conf = None, {}
            return {}
        if mtime != self._mtime:
            try:
                with open(self.path, "r", encoding="utf-8") as f:
                    conf = json.load(f)
                self._conf = conf if isinstance(conf, dict) else {}
            except (OSError, ValueError):
                self._conf = {}
            self._mtime = mtime
        return self._conf

    def days(self) -> int:
        try:
            return max(1, min(90, int(self.conf().get("session_days", DEFAULT_DAYS))))
        except (TypeError, ValueError):
            return DEFAULT_DAYS

    def user(self, name: str) -> dict | None:
        users = self.conf().get("users")
        u = users.get(name) if isinstance(users, dict) and isinstance(name, str) else None
        return u if isinstance(u, dict) and isinstance(u.get("hash"), str) else None

    def _key(self) -> bytes | None:
        s = self.conf().get("secret")
        return bytes.fromhex(s) if isinstance(s, str) and len(s) >= 32 else None

    # ---- rate limit ----
    def locked(self, *keys: str) -> int:
        """Seconds until the first of these keys is free again (0 = not locked)."""
        now = time.time()
        wait = 0
        with self.lock:
            for k in keys:
                hits = [t for t in self._fails.get(k, []) if now - t < FAIL_WINDOW]
                self._fails[k] = hits
                if len(hits) >= MAX_FAILS:
                    wait = max(wait, int(FAIL_WINDOW - (now - hits[0])) + 1)
        return wait

    def fail(self, *keys: str):
        with self.lock:
            for k in keys:
                self._fails.setdefault(k, []).append(time.time())
            if len(self._fails) > 10000:  # bound memory on a 1-GB container
                self._fails = {k: v for k, v in self._fails.items() if v and time.time() - v[-1] < FAIL_WINDOW}

    def clear(self, *keys: str):
        with self.lock:
            for k in keys:
                self._fails.pop(k, None)

    # ---- sessions ----
    def _sig(self, key: bytes, body: str, user: dict) -> str:
        # the stored hash is part of the signature: a new password ends every old session
        return b64url(hmac.new(key, (body + "|" + user["hash"]).encode("utf-8"), hashlib.sha256).digest())

    def login(self, name: str, password: str) -> str | None:
        """Cookie value on success, else None (a missing user costs the same hashing time)."""
        u = self.user(name)
        ok = verify_password(password, u["hash"]) if u else (verify_password(password, _dummy()) and False)
        key = self._key()
        if not ok or key is None:
            return None
        body = b64url(json.dumps({"u": name, "exp": int(time.time()) + self.days() * 86400,
                                  "n": secrets.token_hex(8)}, separators=(",", ":")).encode("utf-8"))
        return body + "." + self._sig(key, body, u)

    def session(self, cookie_value: str | None) -> dict | None:
        """{"name", "email"} for a valid, unexpired, correctly signed session."""
        key = self._key()
        if not cookie_value or key is None or cookie_value.count(".") != 1:
            return None
        body, sig = cookie_value.split(".")
        try:
            data = json.loads(unb64url(body))
            name, exp = data["u"], int(data["exp"])
        except (ValueError, KeyError, TypeError):
            return None
        u = self.user(name)
        if not u or exp < time.time() or not hmac.compare_digest(sig, self._sig(key, body, u)):
            return None
        return {"name": name, "email": u.get("email") or f"{name}@local", "via": "password"}

    # ---- Cloudflare Access (prepared, off until auth.json says enabled) ----
    def cf_conf(self) -> dict | None:
        cf = self.conf().get("cloudflare")
        if isinstance(cf, dict) and cf.get("enabled") and cf.get("team_domain") and cf.get("aud"):
            return cf
        return None

    def _fetch_certs(self, team_domain: str) -> dict:
        url = f"https://{team_domain}/cdn-cgi/access/certs"
        with urllib.request.urlopen(url, timeout=5) as r:
            return json.load(r)

    def _keys(self, team_domain: str) -> dict:
        at, keys = self._certs
        if time.time() - at > CERTS_TTL or not keys:
            try:
                data = self.fetch_certs(team_domain)
                keys = {k["kid"]: (int.from_bytes(unb64url(k["n"]), "big"), int.from_bytes(unb64url(k["e"]), "big"))
                        for k in data.get("keys", []) if k.get("kty") == "RSA" and "kid" in k}
                self._certs = (time.time(), keys)
            except Exception:
                keys = self._certs[1]
        return keys

    def cf_session(self, token: str | None) -> dict | None:
        cf = self.cf_conf()
        if not cf or not token or token.count(".") != 2:
            return None
        h, p, s = token.split(".")
        try:
            header, claims, sig = json.loads(unb64url(h)), json.loads(unb64url(p)), unb64url(s)
        except ValueError:
            return None
        if header.get("alg") != "RS256":
            return None
        key = self._keys(cf["team_domain"]).get(header.get("kid"))
        if not key or not rs256_verify(f"{h}.{p}".encode("ascii"), sig, *key):
            return None
        aud = claims.get("aud")
        auds = aud if isinstance(aud, list) else [aud]
        now = time.time()
        if cf["aud"] not in auds or claims.get("iss") != f"https://{cf['team_domain']}":
            return None
        if not isinstance(claims.get("exp"), (int, float)) or claims["exp"] < now or claims.get("nbf", 0) > now + 60:
            return None
        email = claims.get("email")
        if not isinstance(email, str) or not email:
            return None
        return {"name": email, "email": email, "via": "cloudflare"}


_DUMMY: list[str] = []


def _dummy() -> str:
    if not _DUMMY:
        _DUMMY.append(hash_password(secrets.token_hex(8)))
    return _DUMMY[0]


def write_conf(path: Path, conf: dict):
    """Atomic write with mode 600; keeps the owner of an existing file (the service user)."""
    path = Path(path)
    tmp = path.with_name(path.name + ".tmp")
    fd = os.open(tmp, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as f:
        json.dump(conf, f, indent=1)
    try:
        st = (path if path.exists() else path.parent).stat()
        os.chown(tmp, st.st_uid, st.st_gid)
    except (OSError, AttributeError):
        pass
    os.chmod(tmp, 0o600)
    os.replace(tmp, path)
