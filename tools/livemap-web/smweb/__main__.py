"""Command line of the service (run from tools/livemap-web):

  python3 -m smweb serve [--config FILE] [--bind 127.0.0.1] [--port 8095]
  python3 -m smweb set-role  --config FILE <email> user|developer       (owner: the developer account, spec 5)
  python3 -m smweb set-tier  --config FILE <email> free|pro|max|ultra   (test accounts, spec 6)
  python3 -m smweb add-user  --config FILE <email> [--verified]         (asks for the password twice, no echo)
  python3 -m smweb maintenance --config FILE                            (expired archives, codes, sessions; also hourly in serve)
"""
from __future__ import annotations

import argparse
import getpass
import sys
import threading
import time

from . import config as cfgmod
from . import security as sec
from .app import App, serve
from .service import Service


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(prog="smweb")
    ap.add_argument("command", choices=["serve", "set-role", "set-tier", "add-user", "maintenance"])
    ap.add_argument("args", nargs="*")
    ap.add_argument("--config", default=None)
    ap.add_argument("--bind", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=8095)
    ap.add_argument("--verified", action="store_true")
    a = ap.parse_args(argv)
    svc = Service(cfgmod.load(a.config))
    if a.command == "serve":
        app = App(svc)
        srv = serve(app, a.bind, a.port)

        def housekeeping():
            while True:
                time.sleep(3600)
                try:
                    svc.maintenance()
                except Exception as ex:  # keep serving; the next hour tries again
                    print("maintenance failed:", ex, file=sys.stderr)
        threading.Thread(target=housekeeping, daemon=True).start()
        print(f"smweb on http://{a.bind}:{a.port}/ (hosts {svc.cfg.hosts['account']}, {svc.cfg.hosts['livemap']})", flush=True)
        srv.serve_forever()
        return 0
    if a.command == "maintenance":
        svc.maintenance()
        print("ok")
        return 0
    if not a.args:
        ap.error("e-mail address missing")
    email = sec.normalize_email(a.args[0])
    if not email:
        ap.error("not an e-mail address")
    if a.command == "add-user":
        pw = getpass.getpass("Password: ")
        if pw != getpass.getpass("Again: "):
            print("passwords differ", file=sys.stderr)
            return 1
        problem = sec.password_problem(pw, email)
        if problem:
            print("password refused:", problem, file=sys.stderr)
            return 1
        with svc.db.tx() as c:
            c.execute("INSERT INTO users (email, email_canon, name, pw_hash, created_at, verified_at) VALUES (?,?,?,?,?,?)",
                      (email, sec.canonical_email(email), email.split("@")[0][:40], sec.hash_password(pw), svc.now(), svc.now() if a.verified else None))
        print("added", email)
        return 0
    user = svc.db.one("SELECT * FROM users WHERE email = ?", email)
    if not user:
        print("no such account", file=sys.stderr)
        return 1
    value = a.args[1] if len(a.args) > 1 else ""
    if a.command == "set-role":
        if value not in ("user", "developer"):
            ap.error("role: user or developer")
        with svc.db.tx() as c:
            c.execute("UPDATE users SET role = ? WHERE id = ?", (value, user["id"]))
            svc.audit(c, user["id"], "role", value)
    else:
        svc.set_tier(user, value)
    print("ok")
    return 0


if __name__ == "__main__":
    sys.exit(main())
