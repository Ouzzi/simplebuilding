#!/usr/bin/env python3
"""Set (or change) a login password for the dev area. Run on the web server as root:

    python3 set_password.py --user <name> [--email <identity>] [--file /srv/devqueue/auth.json]
    python3 set_password.py --remove <name>
    python3 set_password.py --cloudflare on|off [--team-domain <team>.cloudflareaccess.com] [--aud <AUD tag>]

The password is read twice without echo (getpass) or, for scripts, from stdin with --stdin; it is never printed,
logged or passed as an argument. A new password ends all old sessions of that user. --email keeps the identity the
rest of the Zentrale uses (mod selection per user); without it an existing one stays.
"""
import argparse
import getpass
import secrets
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import auth  # noqa: E402

DEFAULT = "/srv/devqueue/auth.json"


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--file", default=DEFAULT)
    ap.add_argument("--user")
    ap.add_argument("--email")
    ap.add_argument("--remove")
    ap.add_argument("--stdin", action="store_true", help="read the password from the first stdin line")
    ap.add_argument("--cloudflare", choices=["on", "off"])
    ap.add_argument("--team-domain")
    ap.add_argument("--aud")
    ap.add_argument("--iterations", type=int, default=auth.ITERATIONS, help=argparse.SUPPRESS)
    a = ap.parse_args(argv)
    path = Path(a.file)
    conf = auth.Auth(path).conf() if path.exists() else {}
    conf = dict(conf)
    conf.setdefault("secret", secrets.token_hex(32))
    conf.setdefault("session_days", auth.DEFAULT_DAYS)
    users = dict(conf.get("users") or {})
    cf = dict(conf.get("cloudflare") or {"enabled": False, "team_domain": "", "aud": ""})

    if a.user:
        if not auth.safe_name(a.user):
            print("Name: 1-64 Zeichen a-z A-Z 0-9 . _ - @", file=sys.stderr)
            return 2
        if a.stdin:
            pw = sys.stdin.readline().rstrip("\n")
            pw2 = pw
        else:
            pw = getpass.getpass("Neues Passwort: ")
            pw2 = getpass.getpass("Wiederholen: ")
        if pw != pw2:
            print("Passwörter stimmen nicht überein.", file=sys.stderr)
            return 2
        if len(pw) < 12:
            print("Mindestens 12 Zeichen.", file=sys.stderr)
            return 2
        old = users.get(a.user) or {}
        users[a.user] = {"hash": auth.hash_password(pw, a.iterations), "email": a.email or old.get("email") or ""}
        print(f"Passwort für {a.user} gesetzt (alte Sitzungen ungültig).")
    if a.remove:
        print(f"{a.remove} entfernt." if users.pop(a.remove, None) else f"{a.remove} gab es nicht.")
    if a.cloudflare:
        cf["enabled"] = a.cloudflare == "on"
        cf["team_domain"] = a.team_domain or cf.get("team_domain", "")
        cf["aud"] = a.aud or cf.get("aud", "")
        if cf["enabled"] and not (cf["team_domain"] and cf["aud"]):
            print("Für --cloudflare on fehlen --team-domain und --aud.", file=sys.stderr)
            return 2
        print(f"Cloudflare-Access-Prüfung {'an' if cf['enabled'] else 'aus'}.")
    if not (a.user or a.remove or a.cloudflare):
        ap.print_help()
        return 2
    conf["users"], conf["cloudflare"] = users, cf
    path.parent.mkdir(parents=True, exist_ok=True)
    auth.write_conf(path, conf)
    return 0


if __name__ == "__main__":
    sys.exit(main())
