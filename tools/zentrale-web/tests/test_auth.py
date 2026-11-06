"""Login of the dev area (a-zw06): sessions, cookie flags, rate limit, auth check, Cloudflare JWT switch."""
import base64
import hashlib
import http.client
import json
import os
import random
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from pathlib import Path

WEB = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(WEB))

import auth  # noqa: E402
from devqueue import DevQueueServer  # noqa: E402

PW = "correct horse battery"


def set_pw(path, user, pw=PW, email="owner@example.org", extra=()):
    return subprocess.run([sys.executable, str(WEB / "set_password.py"), "--file", str(path), "--user", user,
                           "--email", email, "--stdin", "--iterations", "1000", *extra],
                          input=pw + "\n", text=True, capture_output=True)


class Base(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.data = Path(self.tmp.name)
        self.auth_path = self.data / "auth.json"
        r = set_pw(self.auth_path, "owner")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.server = DevQueueServer(("127.0.0.1", 0), self.data)
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.port = self.server.server_address[1]

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.tmp.cleanup()

    def req(self, method, path, headers=None, body=None):
        c = http.client.HTTPConnection("127.0.0.1", self.port, timeout=10)
        c.request(method, path, body=body, headers=dict({"Host": "wiki.test"}, **(headers or {})))
        r = c.getresponse()
        data = r.read()
        c.close()
        return r.status, {k.lower(): v for k, v in r.getheaders()}, data

    def login(self, user="owner", pw=PW, nxt="/dev/", ip="10.0.0.1", headers=None):
        form = f"user={user}&password={pw.replace(' ', '+')}&next={nxt}"
        h = {"Content-Type": "application/x-www-form-urlencoded", "X-Client-IP": ip}
        h.update(headers or {})
        return self.req("POST", "/api/auth/login", h, form.encode())


class LoginTest(Base):
    def test_password_file(self):
        self.assertEqual(os.stat(self.auth_path).st_mode & 0o777, 0o600)
        conf = json.loads(self.auth_path.read_text())
        self.assertNotIn(PW, self.auth_path.read_text())
        self.assertTrue(conf["users"]["owner"]["hash"].startswith("pbkdf2_sha256$"))
        self.assertFalse(conf["cloudflare"]["enabled"])
        self.assertGreaterEqual(len(conf["secret"]), 64)

    def test_set_password_rules(self):
        self.assertNotEqual(set_pw(self.auth_path, "owner", "short").returncode, 0)
        self.assertNotEqual(set_pw(self.auth_path, "bad name").returncode, 0)
        r = set_pw(self.auth_path, "owner")
        self.assertNotIn(PW, r.stdout + r.stderr)

    def test_login_cookie_and_check(self):
        st, h, _ = self.req("GET", "/api/auth/check")
        self.assertEqual(st, 401)
        st, h, _ = self.login()
        self.assertEqual(st, 303)
        self.assertEqual(h["location"], "/dev/")
        sc = h["set-cookie"]
        for flag in ("HttpOnly", "Secure", "SameSite=Lax", "Path=/", f"Max-Age={30 * 86400}"):
            self.assertIn(flag, sc)
        cookie = sc.split(";")[0]
        st, h, body = self.req("GET", "/api/auth/check", {"Cookie": cookie})
        self.assertEqual(st, 200)
        self.assertEqual(h["x-auth-user"], "owner@example.org")
        self.assertEqual(json.loads(body)["user"], "owner")
        self.assertEqual(h["cache-control"], "no-store")
        # the cookie alone opens the API too (identity = the stored email)
        st, _, _ = self.req("GET", "/api/queue", {"Cookie": cookie})
        self.assertEqual(st, 200)

    def test_wrong_password_and_unknown_user(self):
        st, h, body = self.login(pw="wrong password!!")
        self.assertEqual(st, 401)
        self.assertNotIn("set-cookie", h)
        st, _, _ = self.login(user="nobody")
        self.assertEqual(st, 401)

    def test_tampered_expired_and_new_password(self):
        cookie = self.login()[1]["set-cookie"].split(";")[0]
        name, value = cookie.split("=", 1)
        body, sig = value.split(".")
        forged = auth.b64url(json.dumps({"u": "owner", "exp": int(time.time()) + 10 ** 8, "n": "x"}).encode())
        self.assertEqual(self.req("GET", "/api/auth/check", {"Cookie": f"{name}={forged}.{sig}"})[0], 401)
        self.assertEqual(self.req("GET", "/api/auth/check", {"Cookie": f"{name}={body}.{sig[:-2]}AA"})[0], 401)
        a = self.server.auth
        old = time.time
        try:
            auth.time.time = lambda: old() + 31 * 86400
            self.assertIsNone(a.session(value))
        finally:
            auth.time.time = old
        self.assertEqual(self.req("GET", "/api/auth/check", {"Cookie": cookie})[0], 200)
        time.sleep(0.01)
        set_pw(self.auth_path, "owner", "another long password")
        self.assertEqual(self.req("GET", "/api/auth/check", {"Cookie": cookie})[0], 401)

    def test_rate_limit(self):
        for _ in range(auth.MAX_FAILS):
            self.assertEqual(self.login(pw="wrong password!!")[0], 401)
        st, h, _ = self.login()  # even the right password is locked now
        self.assertEqual(st, 429)
        self.assertGreater(int(h["retry-after"]), 0)
        # same user from another address: the per-user lock still holds
        self.assertEqual(self.login(ip="10.0.0.2")[0], 429)

    def test_rate_limit_per_address(self):
        for i in range(auth.MAX_FAILS):
            self.login(user=f"guess{i}", pw="wrong password!!")
        self.assertEqual(self.login()[0], 429)
        self.assertEqual(self.login(ip="10.0.0.9")[0], 303)

    def test_logout(self):
        cookie = self.login()[1]["set-cookie"].split(";")[0]
        st, h, _ = self.req("POST", "/api/auth/logout", {"Cookie": cookie, "Origin": "https://wiki.test"})
        self.assertEqual(st, 303)
        self.assertIn("Max-Age=0", h["set-cookie"])

    def test_cross_origin_and_open_redirect(self):
        self.assertEqual(self.login(headers={"Origin": "https://evil.example"})[0], 403)
        for bad in ("//evil.example/", "https://evil.example/", "/\\evil.example"):
            st, h, _ = self.login(nxt=bad)
            self.assertEqual(h["location"], "/", bad)
        st, h, body = self.req("GET", "/api/auth/login?next=%22%3E%3Cscript%3E")
        self.assertEqual(st, 200)
        self.assertNotIn(b"<script>", body)
        self.assertIn(b"frame-ancestors 'none'", h["content-security-policy"].encode())

    def test_identity_header_not_used_by_check(self):
        st, _, _ = self.req("GET", "/api/auth/check", {"Cf-Access-Authenticated-User-Email": "x@y"})
        self.assertEqual(st, 401)

    def test_no_auth_file(self):
        self.auth_path.unlink()
        st, _, body = self.req("GET", "/api/auth/login")
        self.assertIn("Noch kein Konto".encode(), body)
        self.assertEqual(self.login()[0], 401)


# ---- Cloudflare Access JWT (switch prepared, off by default) ----
def _prime(bits, rng):
    while True:
        n = rng.getrandbits(bits) | (1 << (bits - 1)) | 1
        if all(pow(a, n - 1, n) == 1 for a in (2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37)):
            return n


def rsa_key():
    rng = random.Random(7)
    e = 65537
    while True:
        p, q = _prime(512, rng), _prime(512, rng)
        phi = (p - 1) * (q - 1)
        if p != q and phi % e:
            return p * q, e, pow(e, -1, phi)


def sign(n, d, header, claims):
    h = auth.b64url(json.dumps(header).encode())
    p = auth.b64url(json.dumps(claims).encode())
    k = (n.bit_length() + 7) // 8
    t = auth._SHA256_PREFIX + hashlib.sha256(f"{h}.{p}".encode()).digest()
    em = b"\x00\x01" + b"\xff" * (k - len(t) - 3) + b"\x00" + t
    s = pow(int.from_bytes(em, "big"), d, n).to_bytes(k, "big")
    return f"{h}.{p}.{auth.b64url(s)}"


class CloudflareTest(Base):
    KEY = None

    def setUp(self):
        super().setUp()
        if CloudflareTest.KEY is None:
            CloudflareTest.KEY = rsa_key()
        n, e, _ = self.KEY
        jwk = {"keys": [{"kty": "RSA", "kid": "k1", "n": auth.b64url(n.to_bytes((n.bit_length() + 7) // 8, "big")),
                         "e": auth.b64url(e.to_bytes(3, "big"))}]}
        self.server.auth.fetch_certs = lambda team: jwk

    def token(self, **over):
        n, _, d = self.KEY
        claims = {"aud": ["AUD1"], "iss": "https://team.cloudflareaccess.com", "email": "owner@example.org",
                  "exp": int(time.time()) + 600}
        claims.update(over)
        return sign(n, d, {"alg": "RS256", "kid": "k1"}, claims)

    def check(self, tok):
        return self.req("GET", "/api/auth/check", {"Cf-Access-Jwt-Assertion": tok})[0]

    def test_off_by_default(self):
        self.assertEqual(self.check(self.token()), 401)

    def test_on(self):
        r = subprocess.run([sys.executable, str(WEB / "set_password.py"), "--file", str(self.auth_path),
                            "--cloudflare", "on", "--team-domain", "team.cloudflareaccess.com", "--aud", "AUD1"],
                           capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual(self.check(self.token()), 200)
        self.assertEqual(self.check(self.token(aud=["OTHER"])), 401)
        self.assertEqual(self.check(self.token(exp=int(time.time()) - 5)), 401)
        self.assertEqual(self.check(self.token(iss="https://evil.cloudflareaccess.com")), 401)
        tok = self.token()
        h, p, s = tok.split(".")
        p2 = auth.b64url(json.dumps({"aud": ["AUD1"], "iss": "https://team.cloudflareaccess.com",
                                     "email": "evil@example.org", "exp": int(time.time()) + 600}).encode())
        self.assertEqual(self.check(f"{h}.{p2}.{s}"), 401)
        # password sessions keep working next to it
        self.assertEqual(self.login()[0], 303)


if __name__ == "__main__":
    unittest.main()
