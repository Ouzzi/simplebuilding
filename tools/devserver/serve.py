"""
Balancing-Zentrale - lokaler Entwicklungs-Server für alle Balance-Werte von SimpleBuilding.

    python tools/devserver/serve.py            startet auf http://127.0.0.1:8770 und öffnet den Browser
    python tools/devserver/serve.py --no-browser --port 8771
    python tools/devserver/serve.py --refresh-vanilla   Vanilla-Handelspools/Namen neu aus dem Client-Jar lesen
    python tools/devserver/serve.py --check    checkBalance: Ablage, Code und erzeugte Dateien passen zusammen?
                                               (Exit-Code 1 bei Fehlern; gradlew checkBalance ruft das auf)

Liest die Werte bei jedem Start (und auf Knopfdruck) aus dem Repo, hält geplante Werte versioniert in
balance/, schreibt bestätigte Werte in die Mod (JSON und Java, alle Linien), startet Datagen und rechnet
Beschaffungszeiten. Nur Standardbibliothek. Hoert nur auf 127.0.0.1. Aufbau: docs/BALANCING-ZENTRALE.md.
"""

from __future__ import annotations

import argparse
import json
import mimetypes
import sys
import threading
import traceback
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse, parse_qs

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))

from sbdev import extract  # noqa: E402
from sbdev.service import Service  # noqa: E402
from sbdev.store import StoreError  # noqa: E402

REPO = HERE.parent.parent
STATIC = HERE / "static"
MAX_BODY = 5 * 1024 * 1024


def make_handler(service: Service, repo: Path):
    wiki_assets = (repo / "wiki" / "assets").resolve()

    class Handler(BaseHTTPRequestHandler):
        server_version = "Balancing-Zentrale/1"

        def log_message(self, fmt, *args):  # ruhiger Log: nur Fehler
            if len(args) >= 2 and str(args[1])[:1] in ("4", "5"):
                sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

        # ---- Antworten -------------------------------------------------------------------
        def _send(self, status, body: bytes, ctype: str, extra=None, cache: str = "no-store"):
            self.send_response(status)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", cache)
            self.send_header("X-Content-Type-Options", "nosniff")
            for k, v in (extra or {}).items():
                self.send_header(k, v)
            self.end_headers()
            self.wfile.write(body)

        def _json(self, data, status=200, extra=None):
            body = json.dumps(data, ensure_ascii=False, allow_nan=False).encode("utf-8")
            self._send(status, body, "application/json; charset=utf-8", extra)

        def _error(self, status, message, details=None):
            self._json({"error": message, "details": details or []}, status)

        def _host_ok(self) -> bool:
            host = (self.headers.get("Host") or "").split(":")[0]
            return host in ("127.0.0.1", "localhost", "[::1]", "::1")

        # ---- GET -------------------------------------------------------------------------
        def do_GET(self):
            if not self._host_ok():
                return self._error(403, "Nur lokal erreichbar.")
            path = urlparse(self.path).path
            try:
                selected = service.select(parse_qs(urlparse(self.path).query).get("mod", ["simplebuilding"])[0]) if hasattr(service, "select") else service
                if path == "/api/modules":
                    return self._json(service.overview() if hasattr(service, "select") else {"modules": []})
                if path in ("/", "/index.html"):
                    return self._file(STATIC / "index.html")
                if path.startswith("/static/"):
                    return self._file(STATIC / unquote(path[len("/static/"):]), root=STATIC)
                if path.startswith("/wiki/assets/"):
                    return self._file(repo / "wiki" / unquote(path[len("/wiki/"):]), root=wiki_assets)
                if path.startswith(("/modtex/", "/vanilla/")):
                    # Bilder aus den Mod-Assets bzw. dem Client-Jar (sbdev/icons.py) - nur PNGs unter textures/
                    data = extract.icon_resolver(repo).read(unquote(path))
                    if data is None:
                        return self._error(404, "Bild nicht gefunden.")
                    return self._send(200, data, "image/png", cache="max-age=300")
                if path == "/api/state":
                    return self._json(dict(selected.state_payload(), module=selected.module))
                if path.startswith("/api/version/"):
                    number = int(path.rsplit("/", 1)[1])
                    data = selected.store.version(number)
                    data = dict(data, applied=selected.store.applied_for(number))
                    return self._json(data)
                if path.startswith("/api/docs/"):
                    return self._json(selected.doc(unquote(path.rsplit("/", 1)[1])))
                if path in ("/api/phase2", "/api/handover"):
                    data = selected.phase2()
                    return self._json(data, extra={"Content-Disposition": 'attachment; filename="balance-phase2.json"'})
                if path == "/api/pending-apply":
                    return self._json({"pending": selected.pending_apply()})
                if path == "/api/check":
                    return self._json(selected.balance_check())
                if path == "/api/datagen":
                    return self._json(selected.datagen_status())
                return self._error(404, f"Nicht gefunden: {path}")
            except FileNotFoundError:
                return self._error(404, "Nicht gefunden.")
            except StoreError as err:
                return self._error(err.status, err.message, err.details)
            except ValueError as err:
                return self._error(400, str(err))
            except Exception as err:  # pragma: no cover - Fehler sichtbar machen statt Verbindung abbrechen
                traceback.print_exc()
                return self._error(500, f"Serverfehler: {err}")

        def _file(self, path: Path, root: Path | None = None):
            path = path.resolve()
            if root is not None and root.resolve() not in path.parents and path != root.resolve():
                return self._error(403, "Pfad ausserhalb des erlaubten Ordners.")
            if not path.is_file():
                return self._error(404, "Datei nicht gefunden.")
            ctype = mimetypes.guess_type(path.name)[0] or "application/octet-stream"
            if ctype.startswith("text/") or ctype in ("application/javascript",):
                ctype += "; charset=utf-8"
            self._send(200, path.read_bytes(), ctype)

        # ---- POST ------------------------------------------------------------------------
        def do_POST(self):
            if not self._host_ok():
                return self._error(403, "Nur lokal erreichbar.")
            # Schutz gegen fremde Webseiten (CSRF): ein eigener Header erzwingt einen Preflight,
            # den dieser Server nie erlaubt.
            if self.headers.get("X-Balance-Client") != "1":
                return self._error(403, "Nur die Balancing-Zentrale darf schreiben (Header X-Balance-Client fehlt).")
            if "application/json" not in (self.headers.get("Content-Type") or ""):
                return self._error(415, "Bitte JSON senden (Content-Type: application/json).")
            try:
                length = int(self.headers.get("Content-Length") or 0)
            except ValueError:
                return self._error(400, "Content-Length ungültig.")
            if length > MAX_BODY:
                return self._error(413, "Anfrage zu groß.")
            try:
                payload = json.loads(self.rfile.read(length).decode("utf-8") or "{}")
            except (UnicodeDecodeError, json.JSONDecodeError) as err:
                return self._error(400, f"Kein gültiges JSON: {err}")
            if not isinstance(payload, dict):
                return self._error(400, "Erwartet ein JSON-Objekt.")
            path = urlparse(self.path).path
            try:
                selected = service.select(parse_qs(urlparse(self.path).query).get("mod", ["simplebuilding"])[0]) if hasattr(service, "select") else service
            except ValueError as err:
                return self._error(400, str(err))
            routes = {
                "/api/preview": selected.preview,
                "/api/save": selected.save,
                "/api/rollback/preview": selected.rollback_preview,
                "/api/rollback": selected.rollback,
                "/api/calc": selected.calc,
                "/api/reverse": selected.reverse,
                "/api/solve-time": selected.solve_time,
                "/api/overview": selected.overview,
                "/api/apply-planned": selected.apply_planned,
                "/api/reload": lambda _p: selected.reload(),
                "/api/datagen": selected.start_datagen,
                "/api/datagen/cancel": selected.cancel_datagen,
            }
            handler = routes.get(path)
            if handler is None:
                return self._error(404, f"Nicht gefunden: {path}")
            try:
                return self._json(handler(payload))
            except StoreError as err:
                return self._error(err.status, err.message, err.details)
            except (TypeError, ValueError, KeyError) as err:
                return self._error(400, f"Ungültige Anfrage: {err}")
            except Exception as err:  # pragma: no cover
                traceback.print_exc()
                return self._error(500, f"Serverfehler: {err}")

    return Handler


def start(service: Service, repo: Path, host: str = "127.0.0.1", port: int = 8770, tries: int = 10):
    last = None
    for candidate in range(port, port + tries):
        try:
            server = ThreadingHTTPServer((host, candidate), make_handler(service, repo))
            server.daemon_threads = True
            return server
        except OSError as err:
            last = err
    raise SystemExit(f"Kein freier Port ab {port}: {last}")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--port", type=int, default=8770)
    parser.add_argument("--host", default="127.0.0.1", help="nur lokale Adressen sind sinnvoll")
    parser.add_argument("--no-browser", action="store_true", help="Browser nicht öffnen")
    parser.add_argument("--store", default=None, help="Ablage-Ordner (Standard: balance/ im Repo)")
    parser.add_argument("--repo", default=None, help="Repo-Wurzel (Standard: zwei Ebenen über diesem Skript)")
    parser.add_argument("--refresh-vanilla", action="store_true", help="Vanilla-Pools und -Namen neu aus dem Client-Jar lesen")
    parser.add_argument("--check", action="store_true",
                        help="checkBalance: prüfen statt starten (Exit-Code 1, wenn Ablage, Code und erzeugte Dateien abweichen)")
    parser.add_argument("--json", action="store_true", help="mit --check: Ergebnis als JSON")
    args = parser.parse_args(argv)
    if args.check:
        return run_check(args)
    if args.host not in ("127.0.0.1", "localhost", "::1"):
        print("Warnung: die Zentrale ist für lokalen Gebrauch gebaut und hat keine Anmeldung.")
    repo = Path(args.repo).resolve() if args.repo else REPO
    store = Path(args.store).resolve() if args.store else repo / "balance"
    print("Balancing-Zentrale: lese die Werte aus", repo)
    from sbdev.modules import Registry
    service = Registry(repo, Path(args.store).resolve() if args.store else None, refresh_vanilla=args.refresh_vanilla)
    snap = service.select().snapshot
    print(f"  {len(snap['values'])} Werte in {snap['buildSeconds']} s, "
          f"{len(snap['report']['problems'])} nicht auslesbare Stellen (Seite 'Auslese-Bericht')")
    server = start(service, repo, args.host, args.port)
    url = f"http://127.0.0.1:{server.server_address[1]}/"
    print(f"  läuft auf {url}   (Beenden: Strg+C)")
    if not args.no_browser:
        threading.Timer(0.4, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nBeendet.")
    finally:
        server.server_close()
    return 0


def run_check(args) -> int:
    from sbdev import check as balance_checker
    repo = Path(args.repo).resolve() if args.repo else REPO
    store = Path(args.store).resolve() if args.store else repo / "balance"
    from sbdev.modules import Registry, load
    if load(repo):
        registry = Registry(repo, Path(args.store).resolve() if args.store else None)
        results = {m["id"]: registry.select(m["id"]).balance_check() for m in registry.modules}
        result = {"ok": all(r["ok"] for r in results.values()), "modules": results,
                  "errors": [dict(e, module=mid) for mid, r in results.items() for e in r["errors"]],
                  "warnings": [dict(e, module=mid) for mid, r in results.items() for e in r["warnings"]],
                  "stats": {k: sum(r["stats"][k] for r in results.values()) for k in next(iter(results.values()))["stats"]}}
    else:
        result = Service(repo, store).balance_check()
    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=1))
    else:
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except (AttributeError, ValueError):
            pass
        print(balance_checker.format_report(result))
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
