#!/usr/bin/env python3
"""Smoke test of the one-host site (a-zw06), standard library only (sb-web has no curl).

    python3 smoke_zw06.py --port 8084 [--legacy] [--auth-file /srv/devqueue/auth.json]

Without --auth-file only the anonymous side is checked. With it a throwaway user (random password, never shown) is
added, logged in, used and removed again. --legacy checks the old hosts after "switch" (8082/8083 redirect).
Exit code 0 = all fine; every failed check is printed.
"""
import argparse
import http.client
import json
import secrets
import subprocess
import sys
import time
from pathlib import Path

HOST = "simplemods-wiki.azarfane.com"
HERE = Path(__file__).resolve().parent


def request(port, method, path, headers=None, body=None, host=None):
    c = http.client.HTTPConnection("127.0.0.1", port, timeout=10)
    h = {"Host": host or HOST}
    h.update(headers or {})
    c.request(method, path, body=body, headers=h)
    r = c.getresponse()
    data = r.read()
    c.close()
    return r.status, {k.lower(): v for k, v in r.getheaders()}, data


def run(port, auth_file=None, legacy=False, legacy_ports=(8082, 8083), set_password=None, wiki_marker=None):
    fails = []

    def expect(label, cond, info=""):
        if not cond:
            fails.append(f"{label} {info}".strip())

    st, h, body = request(port, "GET", "/")
    expect("GET / (public wiki) 200", st == 200, st)
    if wiki_marker:
        expect("GET / contains wiki marker", wiki_marker.encode() in body)
    st, h, _ = request(port, "GET", "/assets/vanilla/block/stone.png")
    expect("public /assets/vanilla/ is 404", st == 404, st)
    for p in ("/dev/", "/zentrale/", "/zentrale/status.json", "/dev/_overlay.js", "/zentrale/setup/install.ps1"):
        st, h, _ = request(port, "GET", p)
        expect(f"GET {p} without login -> 302 login", st == 302 and h.get("location", "").endswith(
            "/api/auth/login?next=" + p), f"{st} {h.get('location')}")
    st, h, _ = request(port, "GET", "/api/queue")
    expect("GET /api/queue without login 401", st == 401, st)
    st, h, _ = request(port, "GET", "/api/queue", {"Cf-Access-Authenticated-User-Email": "forged@example.org"})
    expect("forged identity header is not trusted (401)", st == 401, st)
    st, h, body = request(port, "GET", "/api/auth/check")
    expect("GET /api/auth/check 401 JSON", st == 401 and h.get("content-type", "").startswith("application/json"), st)
    st, h, body = request(port, "GET", "/api/auth/login?next=/dev/")
    expect("GET /api/auth/login page 200", st == 200 and b'name="password"' in body, st)
    st, h, _ = request(port, "GET", "/dev")
    expect("GET /dev -> /dev/", st == 301 and h.get("location", "").endswith("/dev/"), st)

    if auth_file:
        name = "zw06-smoke-" + secrets.token_hex(3)
        pw = secrets.token_urlsafe(24)
        tool = set_password or str(HERE / "set_password.py")
        subprocess.run([sys.executable, tool, "--file", str(auth_file), "--user", name, "--email",
                        "smoke@localhost", "--stdin", "--iterations", "1000"], input=pw + "\n", text=True,
                       check=True, capture_output=True)
        try:
            form = f"user={name}&password={pw}&next=/dev/".encode()
            st, h, _ = request(port, "POST", "/api/auth/login", {"Content-Type": "application/x-www-form-urlencoded",
                                                                 "Origin": f"https://{HOST}"}, form)
            cookie = (h.get("set-cookie") or "").split(";")[0]
            expect("login -> 303 /dev/ + cookie", st == 303 and h.get("location") == "/dev/" and cookie, st)
            sc = (h.get("set-cookie") or "").lower()
            expect("cookie HttpOnly/Secure/SameSite=Lax/30 days",
                   all(x in sc for x in ("httponly", "secure", "samesite=lax", "max-age=2592000")), sc)
            ck = {"Cookie": cookie}
            st, h, body = request(port, "GET", "/api/auth/check", ck)
            expect("check with cookie 200", st == 200 and json.loads(body or b"{}").get("user") == name, st)
            st, h, body = request(port, "GET", "/dev/", ck)
            expect("GET /dev/ logged in 200 + overlay", st == 200 and b"/dev/_overlay.js" in body, st)
            st, h, _ = request(port, "GET", "/dev/_overlay.js", ck)
            expect("GET /dev/_overlay.js 200", st == 200, st)
            st, h, body = request(port, "GET", "/zentrale/", ck)
            expect("GET /zentrale/ 200 html", st == 200 and b"Zentrale" in body, st)
            st, h, _ = request(port, "GET", "/zentrale/status.json", ck)
            expect("GET /zentrale/status.json 200", st == 200, st)
            st, h, body = request(port, "GET", "/api/queue", ck)
            expect("GET /api/queue logged in 200", st == 200, st)
            st, h, body = request(port, "GET", "/api/mods", dict(ck, **{"Cf-Access-Authenticated-User-Email": "x@y"}))
            expect("GET /api/mods answers (identity from login)", st in (200, 500), st)
            st, h, _ = request(port, "POST", "/api/auth/logout", dict(ck, Origin=f"https://{HOST}"))
            expect("logout clears cookie", st == 303 and "max-age=0" in (h.get("set-cookie") or "").lower(), st)
        finally:
            subprocess.run([sys.executable, tool, "--file", str(auth_file), "--remove", name],
                           check=False, capture_output=True)

    if legacy:
        dev, zen = legacy_ports
        st, h, _ = request(dev, "GET", "/item/x?y=1")
        expect("old wiki-dev -> /dev/...", st == 302 and h.get("location") == f"https://{HOST}/dev/item/x?y=1",
               f"{st} {h.get('location')}")
        cf = {"Cf-Access-Authenticated-User-Email": "smoke@localhost"}
        st, h, _ = request(zen, "GET", "/", cf)
        expect("old zentrale / -> /zentrale/", st == 302 and h.get("location") == f"https://{HOST}/zentrale/",
               f"{st} {h.get('location')}")
        st, h, _ = request(zen, "GET", "/wiki/x", cf)
        expect("old zentrale /wiki/x -> /dev/x", st == 302 and h.get("location") == f"https://{HOST}/dev/x",
               f"{st} {h.get('location')}")
        st, h, _ = request(zen, "GET", "/setup/install.ps1", cf)
        expect("old zentrale /setup/ stays direct (200/404, no redirect)", st in (200, 404), st)
        st, h, _ = request(zen, "GET", "/downloads/index.json", cf)
        expect("old zentrale /downloads/ stays direct (200/404, no redirect)", st in (200, 404), st)
        st, h, _ = request(zen, "GET", "/setup/install.ps1")
        expect("old zentrale without Access header 403", st == 403, st)
    return fails


WIKI = "wiki.simplemods.de"
REDIRECT_HOSTS = ("www.simplemods.de", "simplewiki.de", "www.simplewiki.de", "simplemods-wiki.azarfane.com")


def run_domains(port):
    """IG31-55: every host by Host header (stage "domains" of deploy_dom07.sh)."""
    fails = []

    def expect(label, cond, info=""):
        if not cond:
            fails.append(f"{label} {info}".strip())

    st, h, body = request(port, "GET", "/", host=WIKI)
    expect(f"{WIKI} / public 200", st == 200, st)
    st, h, _ = request(port, "GET", "/assets/vanilla/block/stone.png", host=WIKI)
    expect(f"{WIKI} /assets/vanilla/ 404", st == 404, st)
    for p in ("/dev/", "/zentrale/", "/setup/install.ps1", "/downloads/index.json"):
        st, h, _ = request(port, "GET", p, host=WIKI)
        expect(f"{WIKI}{p} without login -> 302 login", st == 302 and "/api/auth/login?next=" in h.get("location", ""),
               f"{st} {h.get('location')}")
    st, h, _ = request(port, "GET", "/api/queue", host=WIKI)
    expect(f"{WIKI} /api/queue 401", st == 401, st)
    st, h, _ = request(port, "GET", "/api/auth/login?next=/dev/", host=WIKI)
    expect(f"{WIKI} login page 200", st == 200, st)

    st, h, body = request(port, "GET", "/", host="simplemods.de")
    expect("simplemods.de / landing 200 html", st == 200 and b"<html" in body.lower(), st)
    expect("simplemods.de / is not the wiki redirect", "location" not in h)
    st, h, _ = request(port, "GET", "/setup/x?a=1", host="simplemods.de")
    expect("simplemods.de /setup/ -> wiki", st == 301 and h.get("location") == f"https://{WIKI}/setup/x?a=1",
           f"{st} {h.get('location')}")

    for host in REDIRECT_HOSTS:
        for p in ("/", "/dev/item/x?y=1", "/zentrale/"):
            st, h, _ = request(port, "GET", p, host=host)
            expect(f"{host}{p} -> 301 wiki", st == 301 and h.get("location") == f"https://{WIKI}{p}",
                   f"{st} {h.get('location')}")
    old = "simplemods-wiki.azarfane.com"
    cf = {"Cf-Access-Authenticated-User-Email": "smoke@localhost"}
    for p in ("/setup/install.ps1", "/downloads/index.json"):
        st, h, _ = request(port, "GET", p, cf, host=old)
        expect(f"{old}{p} stays direct (200/404, no redirect)", st in (200, 404), st)
        st, h, _ = request(port, "GET", p, host=old)
        expect(f"{old}{p} without Access header 403", st == 403, st)
    return fails


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--port", type=int, default=8084)
    ap.add_argument("--auth-file")
    ap.add_argument("--legacy", action="store_true")
    ap.add_argument("--domains", action="store_true", help="IG31-55: all domains by Host header")
    a = ap.parse_args()
    if a.domains:
        # nginx "reload" returns before the new workers answer: retry so a request that still hits the old
        # config (a-dom08: 17 false errors right after the reload, all fine seconds later) does not count.
        for _ in range(6):
            fails = run_domains(a.port)
            if not fails:
                break
            time.sleep(2)
    else:
        fails = run(a.port, a.auth_file, a.legacy)
    for f in fails:
        print("FEHLER:", f)
    print("smoke:", "OK" if not fails else f"{len(fails)} Fehler")
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
