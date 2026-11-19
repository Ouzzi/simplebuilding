"""HTTP layer: one handler for both hosts (account.simplemods.de, livemap.simplemods.de; both may be the same host for local
tests). Fixed routes only, request bodies limited before they are read, CSRF token + Origin check on every form, security
headers on every answer, strict CSP (no inline script) everywhere except the map viewer, which is the mod's own page."""
from __future__ import annotations

import json
import mimetypes
import re
import time
import traceback
import urllib.parse
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from . import pages
from . import security as sec
from .config import REPO, ROOT
from .errors import Problem
from .i18n import t as tr
from .upload import TILE_RE

STATIC = ROOT / "static"
ICON = REPO / "modules/simplelivemap/shared/resources/assets/simplelivemap/icon_128.png"   # the mod's own icon
MAX_FORM = 64 * 1024
MAX_TILES_BODY = 8 * 1024 * 1024 + 1024
MAX_WEBHOOK = 256 * 1024
MAX_MANIFEST_BODY = 1024 * 1024
ACCOUNT_PATHS = {"/login", "/register", "/forgot", "/reset", "/verify", "/logout", "/resend"}
CSP = ("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-src 'self'; "
       "frame-ancestors 'none'; form-action 'self' {forms}; base-uri 'none'; object-src 'none'")
VIEWER_CSP = ("default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src 'self' blob:; connect-src 'self'; "
              "base-uri 'none'; form-action 'none'; frame-ancestors 'self'")
M_RE = re.compile(r"^/m/([a-z0-9-]{3,32})(/.*)?$")
APP_M_RE = re.compile(r"^/app/m/(\d{1,12})(/preview\.png)?$")


@dataclass
class Request:
    method: str
    host: str
    path: str
    query: dict = field(default_factory=dict)
    headers: dict = field(default_factory=dict)      # lower-case names
    body: bytes = b""
    ip: str = "?"
    form: dict = field(default_factory=dict)

    @property
    def cookies(self) -> dict:
        out = {}
        for part in self.headers.get("cookie", "").split(";"):
            k, _, v = part.strip().partition("=")
            if k and k not in out:
                out[k] = v
        return out


@dataclass
class Response:
    status: int = 200
    body: bytes = b""
    headers: list = field(default_factory=list)

    def header(self, name: str):
        for k, v in self.headers:
            if k.lower() == name.lower():
                return v
        return None

    def cookies(self) -> list[str]:
        return [v for k, v in self.headers if k.lower() == "set-cookie"]


class Ctx:
    """What a page needs: language, user, CSRF token, which site, the service."""

    def __init__(self, app, req: Request, site: str):
        self.app, self.req, self.site, self.svc = app, req, site, app.svc
        self.cfg = app.svc.cfg
        self.sid_token = req.cookies.get(self.cfg["cookie"]["name"])
        self.user = self.svc.session_user(self.sid_token)
        self.lang = self.pick_lang()
        self.set_cookies: list[str] = []
        self.anon_csrf = req.cookies.get("sm_csrf") or ""
        self.flash = ""
        self.error = ""

    def pick_lang(self) -> str:
        q = self.req.query.get("lang")
        if q in ("en", "de"):
            return q
        c = self.req.cookies.get("sm_lang")
        if c in ("en", "de"):
            return c
        if self.user is not None and self.user["lang"] in ("en", "de"):
            return self.user["lang"]
        return "de" if self.req.headers.get("accept-language", "").lower().startswith("de") else "en"

    def t(self, en: str, de: str) -> str:
        return de if self.lang == "de" else en

    def problem_text(self, p: Problem) -> str:
        return tr(self.lang, p.key, **p.params)

    def csrf(self) -> str:
        if self.user is not None:
            return sec.hmac_hex(self.cfg.secret, "csrf", self.user["sid"])
        if not self.anon_csrf:
            self.anon_csrf = sec.token(18)
            self.set_cookies.append(self.app.cookie("sm_csrf", self.anon_csrf, 7200, host_only=True))
        return sec.hmac_hex(self.cfg.secret, "csrf-anon", self.anon_csrf)

    def url(self, which: str, path: str = "/") -> str:
        return self.cfg.url(which, path)


class App:
    def __init__(self, svc):
        self.svc = svc
        self.cfg = svc.cfg
        self.viewer_cache: tuple[float, bytes] | None = None

    # ---------------------------------------------------------------- plumbing
    def site_of(self, host: str) -> str | None:
        h = (host or "").lower()
        a, l = self.cfg.hosts["account"].lower(), self.cfg.hosts["livemap"].lower()
        if a == l and h == a:
            return "single"
        if h == a:
            return "account"
        if h == l:
            return "livemap"
        return None

    def cookie(self, name: str, value: str, max_age: int, host_only: bool = False) -> str:
        c = self.cfg["cookie"]
        parts = [f"{name}={value}", "Path=/", f"Max-Age={max_age}", "HttpOnly", "SameSite=Lax"]
        if c.get("secure", True):
            parts.append("Secure")
        if c.get("domain") and not host_only:
            parts.append("Domain=" + c["domain"])
        return "; ".join(parts)

    def headers(self, ctype: str, csp: str | None, extra: list | None = None, cache: str = "no-store") -> list:
        forms = " ".join(self.cfg.url(k, "") for k in ("account", "livemap"))
        h = [("Content-Type", ctype), ("X-Content-Type-Options", "nosniff"), ("Referrer-Policy", "strict-origin-when-cross-origin"),
             ("Cross-Origin-Opener-Policy", "same-origin"), ("Permissions-Policy", "camera=(), microphone=(), geolocation=()"),
             ("Cache-Control", cache)]
        if csp is not None:
            h.append(("Content-Security-Policy", csp.format(forms=forms) if "{forms}" in csp else csp))
        if self.cfg["scheme"] == "https":
            h.append(("Strict-Transport-Security", "max-age=31536000"))
        return h + (extra or [])

    def html(self, ctx: Ctx, status: int, body: str, cache: str = "no-store") -> Response:
        r = Response(status, body.encode("utf-8"), self.headers("text/html; charset=utf-8", CSP, cache=cache))
        r.headers += [("Set-Cookie", c) for c in ctx.set_cookies]
        if ctx.req.query.get("lang") in ("en", "de"):
            r.headers.append(("Set-Cookie", self.cookie("sm_lang", ctx.req.query["lang"], 365 * 86400)))
        return r

    def json(self, status: int, data, extra: list | None = None, cache: str = "no-store") -> Response:
        return Response(status, json.dumps(data, separators=(",", ":")).encode("utf-8"),
                        self.headers("application/json; charset=utf-8", "default-src 'none'; frame-ancestors 'none'", extra, cache))

    def redirect(self, location: str, ctx: Ctx | None = None, status: int = 303, cookies: list | None = None) -> Response:
        r = Response(status, b"", self.headers("text/plain; charset=utf-8", None) + [("Location", location)])
        r.headers += [("Set-Cookie", c) for c in (cookies or []) + (ctx.set_cookies if ctx else [])]
        return r

    def plain(self, status: int, text: str, extra: list | None = None) -> Response:
        return Response(status, text.encode("utf-8"), self.headers("text/plain; charset=utf-8", "default-src 'none'", extra))

    def safe_next(self, value: str | None) -> str:
        default = self.cfg.url("livemap", "/app")
        if not value or len(value) > 500 or any(ord(ch) < 32 for ch in value):
            return default
        for which in ("livemap", "account"):
            base = self.cfg.url(which, "/")
            if value.startswith(base):
                return value
        return default

    def origin_ok(self, req: Request) -> bool:
        allowed = {self.cfg.url(k, "") for k in ("account", "livemap")}
        origin = req.headers.get("origin")
        if origin:
            return origin in allowed
        ref = req.headers.get("referer", "")
        return any(ref.startswith(a + "/") for a in allowed)

    def check_form(self, ctx: Ctx):
        if not self.origin_ok(ctx.req):
            raise Problem(403, "csrf")
        if not sec.same(ctx.req.form.get("csrf"), ctx.csrf()):
            raise Problem(403, "csrf")

    # ---------------------------------------------------------------- entry
    def handle(self, req: Request) -> Response:
        site = self.site_of(req.host)
        if site is None:
            return self.plain(404, "unknown host")
        if req.path == "/healthz":
            return self.plain(200, "ok")
        if req.path.startswith("/static/"):
            return self.static(req.path[8:])
        if req.path.startswith("/api/"):
            return self.api(req, site)
        ctx = Ctx(self, req, site)
        try:
            if req.method == "POST":
                ctype = req.headers.get("content-type", "")
                if not ctype.startswith("application/x-www-form-urlencoded"):
                    raise Problem(415, "bad_request")
                req.form = {k: v[0] for k, v in urllib.parse.parse_qs(req.body.decode("utf-8", "replace"), max_num_fields=50).items()}
                self.check_form(ctx)
            elif req.method not in ("GET", "HEAD"):
                return self.plain(405, "method not allowed", [("Allow", "GET, HEAD, POST")])
            if site == "account" or (site == "single" and req.path in ACCOUNT_PATHS):
                return self.account_site(ctx)
            return self.livemap_site(ctx)
        except Problem as p:
            return self.html(ctx, p.status, pages.message(ctx, ctx.t("Something went wrong", "Etwas ging schief"), ctx.problem_text(p)))

    # ---------------------------------------------------------------- account host
    def account_site(self, ctx: Ctx) -> Response:
        req, svc, path = ctx.req, self.svc, ctx.req.path
        if path == "/" and req.method != "POST":
            return self.redirect(ctx.url("livemap", "/app/account" if ctx.user else "/"), ctx)
        if path == "/login":
            nxt = self.safe_next(req.query.get("next") or req.form.get("next"))
            if req.method == "POST":
                try:
                    token = svc.login(req.form.get("email", ""), req.form.get("password", ""), req.ip)
                except Problem as p:
                    ctx.error = ctx.problem_text(p)
                    return self.html(ctx, p.status, pages.login(ctx, nxt, req.form.get("email", "")))
                cookie = self.cookie(self.cfg["cookie"]["name"], token, int(self.cfg["cookie"].get("days", 30)) * 86400)
                return self.redirect(nxt, None, cookies=[cookie])
            if ctx.user:
                return self.redirect(nxt, ctx)
            return self.html(ctx, 200, pages.login(ctx, nxt, ""))
        if path == "/register":
            if req.method == "POST":
                f = req.form
                try:
                    if f.get("password", "") != f.get("password2", ""):
                        raise Problem(400, "pw_mismatch")
                    if f.get("terms") != "1":
                        raise Problem(400, "bad_request")
                    svc.register(f.get("email", ""), f.get("password", ""), f.get("name", ""), ctx.lang, req.ip)
                except Problem as p:
                    ctx.error = ctx.problem_text(p)
                    return self.html(ctx, p.status, pages.register(ctx, f))
                return self.html(ctx, 200, pages.message(ctx, ctx.t("Check your mail", "Schau in dein Postfach"), ctx.t(
                    "If the address can be used, a confirmation link is on its way. It works for 24 hours.",
                    "Wenn die Adresse verwendet werden kann, ist ein Bestätigungslink unterwegs. Er gilt 24 Stunden."), mail=True))
            return self.html(ctx, 200, pages.register(ctx, {}))
        if path == "/verify":
            try:
                svc.verify(req.query.get("t", ""))
            except Problem as p:
                return self.html(ctx, p.status, pages.message(ctx, ctx.t("Link invalid", "Link ungültig"), ctx.problem_text(p)))
            return self.html(ctx, 200, pages.message(ctx, ctx.t("E-mail confirmed", "E-Mail bestätigt"),
                                                     ctx.t("Your account is ready.", "Dein Konto ist bereit."),
                                                     link=(ctx.url("livemap", "/app"), ctx.t("To my maps", "Zu meinen Karten"))))
        if path == "/forgot":
            if req.method == "POST":
                try:
                    svc.forgot(req.form.get("email", ""), ctx.lang, req.ip)
                except Problem as p:
                    ctx.error = ctx.problem_text(p)
                    return self.html(ctx, p.status, pages.forgot(ctx))
                return self.html(ctx, 200, pages.message(ctx, ctx.t("Check your mail", "Schau in dein Postfach"), ctx.t(
                    "If an account uses this address, a link to set a new password is on its way (valid one hour).",
                    "Wenn ein Konto diese Adresse nutzt, ist ein Link für ein neues Passwort unterwegs (gilt eine Stunde)."), mail=True))
            return self.html(ctx, 200, pages.forgot(ctx))
        if path == "/reset":
            token = req.query.get("t") or req.form.get("t", "")
            try:
                svc.check_reset(token)
                if req.method == "POST":
                    if req.form.get("password", "") != req.form.get("password2", ""):
                        raise Problem(400, "pw_mismatch")
                    svc.reset(token, req.form.get("password", ""))
                    return self.redirect(ctx.url("account", "/login?done=reset"), ctx,
                                         cookies=[self.cookie(self.cfg["cookie"]["name"], "", 0)])
            except Problem as p:
                if p.key == "link_invalid":
                    return self.html(ctx, p.status, pages.message(ctx, ctx.t("Link invalid", "Link ungültig"), ctx.problem_text(p)))
                ctx.error = ctx.problem_text(p)
                return self.html(ctx, p.status, pages.reset(ctx, token))
            return self.html(ctx, 200, pages.reset(ctx, token))
        if path == "/logout" and req.method == "POST":
            svc.logout(ctx.sid_token)
            return self.redirect(ctx.url("livemap", "/"), None, cookies=[self.cookie(self.cfg["cookie"]["name"], "", 0)])
        if path == "/resend" and req.method == "POST" and ctx.user:
            svc.send_verify(ctx.user["id"])
            return self.redirect(ctx.url("livemap", "/app?sent=1"), ctx)
        raise Problem(404, "not_found")

    # ---------------------------------------------------------------- livemap host
    def need_user(self, ctx: Ctx):
        if ctx.user is None:
            raise Redirect(ctx.url("account", "/login?next=" + urllib.parse.quote(ctx.url("livemap", ctx.req.path), safe="")))
        return ctx.user

    def livemap_site(self, ctx: Ctx) -> Response:
        try:
            return self._livemap(ctx)
        except Redirect as r:
            return self.redirect(r.location, ctx)

    def _livemap(self, ctx: Ctx) -> Response:
        req, svc, path = ctx.req, self.svc, ctx.req.path
        if path in ("/login", "/register", "/forgot"):
            return self.redirect(ctx.url("account", path + "?next=" + urllib.parse.quote(ctx.url("livemap", "/app"), safe="")), ctx)
        if path == "/":
            return self.html(ctx, 200, pages.landing(ctx), cache="no-cache")
        if path == "/pricing":
            return self.html(ctx, 200, pages.pricing(ctx), cache="no-cache")
        if path == "/self-host":
            return self.html(ctx, 200, pages.self_host(ctx), cache="no-cache")
        if path == "/explore":
            return self.html(ctx, 200, pages.explore(ctx), cache="no-cache")
        if path == "/report":
            return self.report_route(ctx)
        m = M_RE.match(path)
        if m:
            return self.map_route(ctx, m.group(1), m.group(2))
        if path == "/app":
            user = self.need_user(ctx)
            maps = [svc.refresh_state(x) for x in svc.maps_of(user["id"])]
            shared = svc.db.all("SELECT m.* FROM shares s JOIN maps m ON m.id = s.map_id WHERE s.user_id = ? AND m.state != 'archived'", user["id"])
            return self.html(ctx, 200, pages.dashboard(ctx, maps, shared, svc.usage(user)))
        if path == "/app/connect":
            return self.connect_route(ctx)
        if path == "/app/connect/state":
            user = self.need_user(ctx)
            return self.json(200, self.connect_state(user, req.query.get("c", "")))
        am = APP_M_RE.match(path)
        if am:
            return self.app_map(ctx, int(am.group(1)), bool(am.group(2)))
        if path == "/app/archive":
            user = self.need_user(ctx)
            if req.method == "POST":
                m = svc.db.one("SELECT * FROM maps WHERE id = ? AND user_id = ? AND state = 'archived'", int_or(req.form.get("map")), user["id"])
                if not m:
                    raise Problem(404, "not_found")
                if req.form.get("action") == "delete":
                    svc.delete_map(m["id"])
                return self.redirect(ctx.url("livemap", "/app/archive"), ctx)
            return self.html(ctx, 200, pages.archive(ctx, svc.maps_of(user["id"], True), svc.usage(user)))
        if path == "/app/plan":
            user = self.need_user(ctx)
            if req.method == "POST":
                try:
                    url = svc.portal(user) if req.form.get("action") == "portal" else svc.checkout(user, req.form.get("tier", ""), req.form.get("interval", ""))
                    return self.redirect(url, ctx)
                except Problem as p:
                    ctx.flash = ctx.problem_text(p)
            return self.html(ctx, 200, pages.plan(ctx, svc.usage(user)))
        if path == "/app/account":
            return self.account_settings(ctx)
        if path == "/app/account/export":
            user = self.need_user(ctx)
            data = json.dumps(svc.export(user), indent=1, ensure_ascii=False).encode("utf-8")
            return Response(200, data, self.headers("application/json; charset=utf-8", "default-src 'none'",
                                                    [("Content-Disposition", 'attachment; filename="simplemods-account.json"')]))
        if path == "/admin":
            return self.admin(ctx)
        raise Problem(404, "not_found")

    def report_route(self, ctx: Ctx) -> Response:
        """Notice form of every hosted map (legal draft U5): reachable without login from the viewer's "Report" link."""
        req, svc = ctx.req, self.svc
        f = req.form if req.method == "POST" else {}
        slug = (f.get("map") if req.method == "POST" else req.query.get("map")) or ""
        m = svc.db.one("SELECT * FROM maps WHERE slug = ?", slug) if M_RE.match(f"/m/{slug}") else None
        if m is None:
            raise Problem(404, "not_found")
        if req.method == "POST":
            try:
                rid = svc.report(slug, f.get("reason", ""), f.get("text", ""), f.get("contact", ""), f.get("confirm") == "1",
                                 ctx.user, req.ip, ctx.lang)
                return self.html(ctx, 200, pages.message(ctx, ctx.t("Thank you", "Danke"), ctx.t(
                    f"Your notice has number {rid}. We check it and decide; with a contact address you hear the result.",
                    f"Deine Meldung hat die Nummer {rid}. Wir prüfen sie und entscheiden; mit Kontaktadresse erfährst du das Ergebnis.")))
            except Problem as p:
                ctx.error = ctx.problem_text(p)
                return self.html(ctx, p.status, pages.report_form(ctx, m, f))
        return self.html(ctx, 200, pages.report_form(ctx, m, f))

    # ---------------------------------------------------------------- viewer and map data
    def viewer(self) -> bytes | None:
        p = Path(self.cfg["viewer_path"])
        try:
            mtime = p.stat().st_mtime
            if self.viewer_cache is None or self.viewer_cache[0] != mtime:
                self.viewer_cache = (mtime, p.read_bytes())
            return self.viewer_cache[1]
        except OSError:
            return None

    def map_route(self, ctx: Ctx, slug: str, rest: str | None) -> Response:
        svc = self.svc
        m = svc.db.one("SELECT * FROM maps WHERE slug = ?", slug)
        if rest is None:
            return self.redirect(ctx.url("livemap", f"/m/{slug}/"), ctx, status=301)
        if m is None or m["state"] == "archived":
            return self.html(ctx, 404, pages.message(ctx, ctx.t("Map not found", "Karte nicht gefunden"), ctx.t(
                "This map does not exist or is not connected at the moment.", "Diese Karte gibt es nicht oder sie ist gerade nicht verbunden.")))
        m = svc.refresh_state(m)
        # The viewer sends a link map's key as X-Livemap-Token (keeps a password prompt of the dev stage working);
        # Authorization: Bearer stays accepted (older viewer pages, scripts).
        bearer = ctx.req.headers.get("x-livemap-token", "").strip()[:200] or None
        if bearer is None:
            auth = ctx.req.headers.get("authorization", "")
            bearer = auth[7:].strip() if auth.startswith("Bearer ") else None
        if rest == "/":
            if m["visibility"] == "private" and not svc.can_view(m, ctx.user, None):
                if ctx.user is None:
                    raise_redirect = ctx.url("account", "/login?next=" + urllib.parse.quote(ctx.url("livemap", f"/m/{slug}/"), safe=""))
                    return self.redirect(raise_redirect, ctx)
                raise Problem(403, "forbidden")
            if m["state"] != "ready":
                return self.html(ctx, 200, pages.preparing(ctx, m, svc.progress(m)))
            page = self.viewer()
            if page is None:
                raise Problem(404, "not_found")
            svc.count_view(m)
            return Response(200, page, self.headers("text/html; charset=utf-8", VIEWER_CSP, [("X-Frame-Options", "SAMEORIGIN")], cache="no-cache"))
        if not svc.can_view(m, ctx.user, bearer):
            return self.plain(401, "token required", [("Cache-Control", "no-store")])
        if m["state"] != "ready":
            return self.plain(503, "map is being prepared", [("Retry-After", "60")])
        over = svc.over_views(m["user_id"])
        public = m["visibility"] == "public"
        if rest == "/live.json":
            data = svc.live_json(m, over)
            cache = f"public, max-age={data['poll']}" if public else "private, no-store"
            return self.json(200, data, cache=cache)
        if rest.startswith("/tiles/") and rest.endswith(".png") and TILE_RE.match(rest[7:-4]):
            sha, png = svc.tile(m, rest[7:-4])
            if png is None:
                return Response(204, b"", self.headers("image/png", None, cache="public, max-age=60" if public else "private, max-age=60"))
            t = svc.tier(svc.user(m["user_id"]))
            age = 3600 if over else max(60, min(900, t["tile_seconds"]))
            cache = (f"public, max-age={age}" if public else f"private, max-age={age}")
            etag = f'"{sha}"'
            if ctx.req.headers.get("if-none-match") == etag:
                return Response(304, b"", self.headers("image/png", None, [("ETag", etag)], cache=cache))
            return Response(200, png, self.headers("image/png", None, [("ETag", etag)], cache=cache))
        return self.plain(404, "not found")

    # ---------------------------------------------------------------- connect wizard
    def connect_route(self, ctx: Ctx) -> Response:
        user = self.need_user(ctx)
        svc, req = self.svc, ctx.req
        archive_id = int_or(req.query.get("archive") or req.form.get("archive"))
        archived = svc.db.one("SELECT * FROM maps WHERE id = ? AND user_id = ? AND state = 'archived'", archive_id, user["id"]) if archive_id else None
        if req.method == "POST":
            f = req.form
            try:
                code, until = svc.create_code(user, f.get("name", ""), f.get("visibility", "private"), f.get("slug", ""),
                                              archived["id"] if archived else None)
            except Problem as p:
                ctx.error = ctx.problem_text(p)
                return self.html(ctx, p.status, pages.connect_form(ctx, svc.usage(user), svc.tier(user), f, archived))
            return self.html(ctx, 200, pages.connect_code(ctx, sec.pretty_code(code), until, self.code_ref(sec.sha256(code))))
        idx, until = svc.free_slot(user)
        return self.html(ctx, 200, pages.connect_form(ctx, svc.usage(user), svc.tier(user),
                                                      {"name": archived["name"] if archived else "", "visibility": archived["visibility"] if archived else "private"},
                                                      archived, slot_until=until if idx is None else 0))

    def code_ref(self, code_hash: str) -> str:
        """Handle of a code for the status poll: keyed with the server secret, so the page tells nothing about the code."""
        return sec.hmac_hex(self.cfg.secret, "code-ref", code_hash)[:16]

    def connect_state(self, user, ref: str) -> dict:
        if not re.fullmatch(r"[0-9a-f]{16}", ref or ""):
            return {"state": "unknown"}
        row = next((r for r in self.svc.db.all("SELECT * FROM codes WHERE user_id = ? ORDER BY created_at DESC LIMIT 20", user["id"])
                    if sec.same(self.code_ref(r["code_hash"]), ref)), None)
        if not row:
            return {"state": "unknown"}
        if row["used_at"] is None:
            return {"state": "expired" if row["expires_at"] < self.svc.now() else "waiting", "left": max(0, row["expires_at"] - self.svc.now())}
        m = self.svc.db.one("SELECT * FROM maps WHERE id = ? AND user_id = ? AND state != 'archived'", row["map_id"], user["id"])
        if not m:
            return {"state": "connected"}
        m = self.svc.refresh_state(m)
        return {"state": m["state"], "progress": self.svc.progress(m), "url": self.cfg.url("livemap", f"/app/m/{m['id']}")}

    # ---------------------------------------------------------------- one map (owner)
    def app_map(self, ctx: Ctx, map_id: int, preview: bool) -> Response:
        user = self.need_user(ctx)
        svc, req = self.svc, ctx.req
        m = svc.db.one("SELECT * FROM maps WHERE id = ?", map_id)
        if m is None or (m["user_id"] != user["id"] and user["role"] != "developer"):
            raise Problem(404, "not_found")
        if preview:
            # Overview picture: the fullest tile of a coarse zoom (2 = 512 blocks per tile, then 1, 0, 3); never a height tile
            # (folder "<dimension>.h", data for the viewer's shaded styles, not a picture).
            row = svc.db.one("SELECT sha FROM tiles WHERE map_id = ? AND substr(path, instr(path, '/') + 1, 2) IN ('2/', '1/', '0/', '3/') "
                             "AND substr(path, 1, instr(path, '/') - 1) NOT LIKE '%.h' "
                             "ORDER BY CASE substr(path, instr(path, '/') + 1, 1) WHEN '2' THEN 0 WHEN '1' THEN 1 WHEN '0' THEN 2 ELSE 3 END, size DESC LIMIT 1", m["id"])
            if not row:
                return Response(204, b"", self.headers("image/png", None))
            try:
                return Response(200, svc.blob_path(row["sha"]).read_bytes(), self.headers("image/png", None, cache="private, max-age=300"))
            except OSError:
                return Response(204, b"", self.headers("image/png", None))
        if req.method == "POST":
            f, action = req.form, req.form.get("action")
            try:
                if action == "save":
                    svc.update_map(m, f.get("name", ""), f.get("players") == "1", f.get("slug", ""), f.get("dpa") == "1")
                    svc.set_visibility(svc.db.one("SELECT * FROM maps WHERE id = ?", m["id"]), f.get("visibility", m["visibility"]))
                    ctx.flash = ctx.t("Saved.", "Gespeichert.")
                elif action == "share":
                    svc.share(m, f.get("email", ""))
                    ctx.flash = ctx.t("Shared.", "Geteilt.")
                elif action == "unshare":
                    svc.unshare(m, int_or(f.get("user")) or 0)
                elif action == "newkey":
                    svc.new_view_key(m)
                    ctx.flash = ctx.t("New link made; the old one no longer works.", "Neuer Link erzeugt; der alte geht nicht mehr.")
                elif action == "disconnect" and m["state"] != "archived":
                    svc.disconnect(m, user["id"])
                    return self.redirect(ctx.url("livemap", "/app/archive"), ctx)
                else:
                    raise Problem(400, "bad_request")
            except Problem as p:
                ctx.error = ctx.problem_text(p)
            m = svc.db.one("SELECT * FROM maps WHERE id = ?", map_id)
        m = svc.refresh_state(m)
        owner = svc.user(m["user_id"])
        stats = {"tiles": svc.db.value("SELECT COUNT(*) FROM tiles WHERE map_id = ?", m["id"], default=0),
                 "views_today": svc.db.value("SELECT SUM(count) FROM views WHERE map_id = ? AND day = ?", m["id"], time.strftime("%Y-%m-%d", time.gmtime(svc.now())), default=0),
                 "views_30": svc.db.value("SELECT SUM(count) FROM views WHERE map_id = ? AND day > ?", m["id"], time.strftime("%Y-%m-%d", time.gmtime(svc.now() - 30 * 86400)), default=0)}
        ok, pos = svc.admitted(m)
        return self.html(ctx, 200, pages.map_page(ctx, m, svc.progress(m), pos, svc.shares_of(m["id"]), svc.tier(owner), stats))

    # ---------------------------------------------------------------- account settings (spec: /app/account)
    def account_settings(self, ctx: Ctx) -> Response:
        user = self.need_user(ctx)
        svc, req = self.svc, ctx.req
        if req.method == "POST":
            f, action = req.form, req.form.get("action")
            try:
                if action == "profile":
                    svc.set_profile(user, f.get("name", ""), f.get("lang", ""))
                    ctx.flash = ctx.t("Saved.", "Gespeichert.")
                elif action == "password":
                    if f.get("new", "") != f.get("new2", ""):
                        raise Problem(400, "pw_mismatch")
                    svc.change_password(user, f.get("old", ""), f.get("new", ""))
                    ctx.flash = ctx.t("Password changed; other sessions ended.", "Passwort geändert; andere Sitzungen beendet.")
                elif action == "others":
                    svc.logout_others(user, user["sid"])
                    ctx.flash = ctx.t("All other sessions ended.", "Alle anderen Sitzungen beendet.")
                elif action == "delete":
                    if f.get("confirm") != "DELETE":
                        raise Problem(400, "bad_request")
                    svc.delete_account(user, f.get("password", ""))
                    return self.redirect(ctx.url("livemap", "/"), None, cookies=[self.cookie(self.cfg["cookie"]["name"], "", 0)])
                else:
                    raise Problem(400, "bad_request")
            except Problem as p:
                ctx.error = ctx.problem_text(p)
            ctx.user = svc.session_user(ctx.sid_token)
        return self.html(ctx, 200, pages.account(ctx, svc.session_count(ctx.user["id"]), svc.usage(ctx.user)))

    # ---------------------------------------------------------------- admin (developer role only)
    def admin(self, ctx: Ctx) -> Response:
        user = self.need_user(ctx)
        svc, req = self.svc, ctx.req
        if user["role"] != "developer":
            raise Problem(403, "forbidden")
        if req.method == "POST":
            f, action = req.form, req.form.get("action")
            try:
                if action == "tier":
                    target = svc.user(int_or(f.get("user")) or 0)
                    if not target:
                        raise Problem(404, "not_found")
                    svc.set_tier(target, f.get("tier", ""), user["id"])
                elif action in ("disable", "enable"):
                    target = svc.user(int_or(f.get("user")) or 0)
                    if not target or target["id"] == user["id"]:
                        raise Problem(400, "bad_request")
                    with svc.db.tx() as c:
                        c.execute("UPDATE users SET disabled = ? WHERE id = ?", (1 if action == "disable" else 0, target["id"]))
                        if action == "disable":
                            c.execute("DELETE FROM sessions WHERE user_id = ?", (target["id"],))
                        svc.audit(c, user["id"], action, f"user {target['id']}")
                elif action == "ready":
                    svc.instant_ready(int_or(f.get("map")) or 0, user)
                elif action == "disconnect":
                    m = svc.db.one("SELECT * FROM maps WHERE id = ?", int_or(f.get("map")) or 0)
                    if m and m["state"] != "archived":
                        svc.disconnect(m, user["id"])
                elif action == "maintenance":
                    svc.maintenance()
                elif action in ("report_dismiss", "report_disconnect"):
                    svc.handle_report(int_or(f.get("report")) or 0, action[7:], user, f.get("note", ""))
                else:
                    raise Problem(400, "bad_request")
                ctx.flash = ctx.t("Done.", "Erledigt.")
            except Problem as p:
                ctx.error = ctx.problem_text(p)
        users = svc.db.all("SELECT u.*, (SELECT COUNT(*) FROM maps m WHERE m.user_id = u.id AND m.state != 'archived') AS active "
                           "FROM users u ORDER BY u.id DESC LIMIT 200")
        maps = [svc.refresh_state(m) for m in svc.db.all("SELECT * FROM maps ORDER BY id DESC LIMIT 200")]
        owners = {u["id"]: u["email"] for u in svc.db.all("SELECT id, email FROM users")}
        storage = {"used": svc.storage_used(), "quota": svc.cfg["storage_quota_gb"] * 1024 ** 3, "disk": svc.disk_percent()}
        return self.html(ctx, 200, pages.admin(ctx, users, maps, owners, svc.queue(), storage, svc.flags(),
                                               svc.mailer.recent() if svc.mailer.uses_outbox else [], svc.reports()))

    # ---------------------------------------------------------------- API (mod, Stripe)
    def api(self, req: Request, site: str) -> Response:
        svc = self.svc
        if site == "account":
            return self.plain(404, "not found")
        lang = "en"
        try:
            path = req.path
            if path == "/api/stripe/webhook" and req.method == "POST":
                return self.json(200, svc.webhook(req.body, req.headers.get("stripe-signature")))
            if not path.startswith("/api/v1/"):
                raise Problem(404, "not_found")
            name = path[8:]
            if name == "connect" and req.method == "POST":
                return self.json(200, svc.api_connect(parse_json(req.body), req.ip))
            m = svc.map_by_key(req.headers.get("authorization"))
            if name == "status" and req.method == "GET":
                return self.json(200, svc.api_status(m))
            if name == "manifest" and req.method == "POST":
                return self.json(200, svc.api_manifest(m, parse_json(req.body)))
            if name == "tiles" and req.method == "POST":
                if not req.headers.get("content-type", "").startswith("application/zip"):
                    raise Problem(415, "bad_request")
                return self.json(200, svc.api_tiles(m, req.body))
            if name == "live" and req.method == "PUT":
                if len(req.body) > MAX_FORM:
                    raise Problem(413, "too_large")
                return self.json(200, svc.api_live(m, parse_json(req.body)))
            if name == "disconnect" and req.method == "POST":
                return self.json(200, svc.api_disconnect(m))
            raise Problem(404, "not_found")
        except Problem as p:
            extra = [("Retry-After", str(p.retry_after))] if p.retry_after else []
            return self.json(p.status, {"error": p.key, "message": tr(lang, p.key, **p.params)}, extra)

    # ---------------------------------------------------------------- static files
    def static(self, name: str) -> Response:
        if not re.fullmatch(r"[a-z0-9_-]{1,40}\.(css|js|png|svg|ico)", name):
            return self.plain(404, "not found")
        p = ICON if name == "livemap-icon.png" else STATIC / name
        try:
            data = p.read_bytes()
        except OSError:
            return self.plain(404, "not found")
        ctype = mimetypes.guess_type(name)[0] or "application/octet-stream"
        if ctype.startswith("text/") or ctype.endswith("javascript"):
            ctype += "; charset=utf-8"
        return Response(200, data, self.headers(ctype, None, cache="public, max-age=3600"))


class Redirect(Exception):
    def __init__(self, location: str):
        super().__init__(location)
        self.location = location


def int_or(value):
    try:
        return int(value)
    except (TypeError, ValueError):
        return None


def parse_json(body: bytes):
    try:
        return json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, ValueError):
        raise Problem(400, "bad_request")


# -------------------------------------------------------------------- real HTTP server
def body_limit(path: str) -> int:
    if path == "/api/v1/tiles":
        return MAX_TILES_BODY
    if path == "/api/v1/manifest":
        return MAX_MANIFEST_BODY   # up to 5000 entries of path + SHA-256 (the mod sends 2000 per call)
    if path == "/api/stripe/webhook":
        return MAX_WEBHOOK
    return MAX_FORM


class Handler(BaseHTTPRequestHandler):
    app: App = None
    server_version = "smweb"
    sys_version = ""
    timeout = 30

    def log_message(self, fmt, *args):  # no request log with addresses or query strings (privacy); nginx logs access
        pass

    def do_GET(self):
        self.dispatch("GET")

    def do_HEAD(self):
        self.dispatch("HEAD")

    def do_POST(self):
        self.dispatch("POST")

    def do_PUT(self):
        self.dispatch("PUT")

    def dispatch(self, method: str):
        try:
            u = urllib.parse.urlsplit(self.path)
            path = u.path
            if len(self.path) > 4096 or "\\" in path or ".." in path:
                return self.send(Response(400, b"bad request", [("Content-Type", "text/plain")]))
            body = b""
            if method in ("POST", "PUT"):
                if self.headers.get("Transfer-Encoding"):
                    return self.send(Response(411, b"length required", [("Content-Type", "text/plain")]))
                try:
                    n = int(self.headers.get("Content-Length", "-1"))
                except ValueError:
                    n = -1
                if n < 0:
                    return self.send(Response(411, b"length required", [("Content-Type", "text/plain")]))
                if n > body_limit(path):
                    self.close_connection = True
                    return self.send(Response(413, b"too large", [("Content-Type", "text/plain")]))
                body = self.rfile.read(n)
            headers = {k.lower(): v for k, v in self.headers.items()}
            ip_header = (self.app.cfg["client_ip_header"] or "").lower()
            ip = headers.get(ip_header) if ip_header and self.client_address[0] in ("127.0.0.1", "::1") else None
            req = Request(method="GET" if method == "HEAD" else method, host=headers.get("host", ""), path=path,
                          query={k: v[0] for k, v in urllib.parse.parse_qs(u.query, max_num_fields=20).items()},
                          headers=headers, body=body, ip=(ip or self.client_address[0])[:64])
            resp = self.app.handle(req)
            self.send(resp, head=method == "HEAD")
        except Exception:  # never leak a trace to the client
            traceback.print_exc()
            try:
                self.send(Response(500, b"internal error", [("Content-Type", "text/plain")]))
            except OSError:
                pass

    def send(self, resp: Response, head: bool = False):
        self.send_response(resp.status)
        for k, v in resp.headers:
            self.send_header(k, v)
        self.send_header("Content-Length", str(len(resp.body)))
        self.end_headers()
        if not head and resp.body:
            self.wfile.write(resp.body)


def serve(app: App, bind: str, port: int) -> ThreadingHTTPServer:
    handler = type("BoundHandler", (Handler,), {"app": app})
    srv = ThreadingHTTPServer((bind, port), handler)
    srv.daemon_threads = True
    return srv
