"""Test helpers: a service in a temp folder with a fake clock, an in-process client with a cookie jar (Domain cookies,
Max-Age=0 deletes), CSRF from the rendered forms, and tiny valid PNG tiles made with zlib (no image library)."""
from __future__ import annotations

import io
import json
import re
import struct
import sys
import tempfile
import urllib.parse
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from smweb import config, security  # noqa: E402
from smweb.app import App, Request  # noqa: E402
from smweb.service import Service  # noqa: E402

security.ITERATIONS = 1000   # fast hashing in tests; the format and checks stay the same
WORLD = "0f8a7c1e-2b3d-4c5e-8f90-a1b2c3d4e5f6"
WORLD2 = "11111111-2222-4333-8444-555555555555"


class Clock:
    def __init__(self, t: float = 1_800_000_000.0):
        self.t = t

    def __call__(self):
        return self.t

    def advance(self, seconds: float):
        self.t += seconds


def make(tmp: str, **over):
    over.setdefault("hosts", {"account": "account.test", "livemap": "livemap.test"})
    over.setdefault("cookie", {"name": "__Secure-sm_session", "domain": "test", "secure": True, "days": 30})
    cfg = config.load(None, data_dir=tmp, **over)
    clock = Clock()
    svc = Service(cfg, clock)
    return svc, App(svc), clock


def png(seed: int = 0, w: int = 128, h: int = 128) -> bytes:
    raw = b"".join(b"\x00" + bytes([(seed + x + y) % 256 for x in range(w)]) for y in range(h))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 0, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")


def png_gray(pixels: bytes, w: int = 128, h: int = 128) -> bytes:
    """8-bit grey PNG of the given pixel bytes (row by row)."""
    raw = b"".join(b"\x00" + pixels[y * w:(y + 1) * w] for y in range(h))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 0, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")


def bundle(entries: dict, compression=zipfile.ZIP_STORED) -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", compression) as z:
        for name, data in entries.items():
            z.writestr(name, data)
    return buf.getvalue()


class Client:
    def __init__(self, app: App, ip: str = "198.51.100.7"):
        self.app = app
        self.ip = ip
        self.jar: dict[str, tuple[str, str | None, str]] = {}   # name -> (value, domain, host)

    def cookies_for(self, host: str) -> str:
        out = []
        for name, (value, domain, origin_host) in self.jar.items():
            if (domain and (host == domain or host.endswith("." + domain))) or (not domain and host == origin_host):
                out.append(f"{name}={value}")
        return "; ".join(out)

    def store(self, host: str, resp):
        for c in resp.cookies():
            parts = [p.strip() for p in c.split(";")]
            name, _, value = parts[0].partition("=")
            attrs = {p.split("=", 1)[0].lower(): (p.split("=", 1)[1] if "=" in p else True) for p in parts[1:]}
            if attrs.get("max-age") == "0":
                self.jar.pop(name, None)
            else:
                self.jar[name] = (value, attrs.get("domain"), host)

    def request(self, method: str, url: str, form: dict | None = None, body: bytes = b"", headers: dict | None = None,
                origin: str | None = "auto"):
        u = urllib.parse.urlsplit(url)
        h = {k.lower(): v for k, v in (headers or {}).items()}
        cookie = self.cookies_for(u.netloc)
        if cookie:
            h["cookie"] = cookie
        if form is not None:
            body = urllib.parse.urlencode(form).encode()
            h.setdefault("content-type", "application/x-www-form-urlencoded")
            if origin == "auto":
                h["origin"] = f"{u.scheme}://{u.netloc}"
            elif origin:
                h["origin"] = origin
        req = Request(method, u.netloc, u.path, {k: v[0] for k, v in urllib.parse.parse_qs(u.query).items()}, h, body, self.ip)
        resp = self.app.handle(req)
        self.store(u.netloc, resp)
        return resp

    def get(self, url: str, **kw):
        return self.request("GET", url, **kw)

    def csrf(self, url: str) -> str:
        r = self.get(url)
        m = re.search(r'name="csrf" value="([0-9a-f]+)"', r.body.decode())
        assert m, f"no form on {url}: {r.status}"
        return m.group(1)

    def post(self, url: str, form: dict, page: str | None = None, **kw):
        form = dict(form)
        form.setdefault("csrf", self.csrf(page or url))
        return self.request("POST", url, form=form, **kw)

    def api(self, method: str, name: str, data=None, key: str | None = None, raw: bytes | None = None, ctype: str = "application/json"):
        h = {"content-type": ctype}
        if key:
            h["authorization"] = "Bearer " + key
        body = raw if raw is not None else (json.dumps(data).encode() if data is not None else b"")
        r = self.app.handle(Request(method, "livemap.test", "/api/v1/" + name, {}, h, body, self.ip))
        return r.status, (json.loads(r.body) if r.body else None), r


def link_from_mail(svc, kind: str) -> str:
    for mail in reversed(svc.mailer.sent):
        m = re.search(r"https://account\.test/" + kind + r"\?t=[A-Za-z0-9_\-]+", mail["text"])
        if m:
            return m.group(0)
    raise AssertionError("no mail with " + kind)


def account(c: Client, svc, email="alex@example.com", password="correct horse battery", verify=True):
    r = c.post("https://account.test/register", {"email": email, "password": password, "password2": password, "name": "Alex", "terms": "1"})
    assert r.status == 200, r.body[:300]
    if verify:
        c.get(link_from_mail(svc, "verify"))
    r = c.post("https://account.test/login", {"email": email, "password": password, "next": "https://livemap.test/app"})
    assert r.status == 303, r.body[:300]
    return svc.db.one("SELECT * FROM users WHERE email = ?", email)


def code_for(c: Client, name="Survival", visibility="private", **extra) -> str:
    r = c.post("https://livemap.test/app/connect", {"name": name, "visibility": visibility, **extra})
    m = re.search(r'aria-label="code">([A-Z0-9]{4}-[A-Z0-9]{4})<', r.body.decode())
    assert m, r.body.decode()[:2000]
    return m.group(1)


def connect(c: Client, world=WORLD, **kw) -> dict:
    code = code_for(c, **kw)
    status, data, _ = c.api("POST", "connect", {"code": code, "world": world, "mod": "0.2.0"})
    assert status == 200, data
    return data


def temp() -> str:
    return tempfile.mkdtemp(prefix="smweb-test-")
