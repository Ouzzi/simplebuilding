# Zentrale (Status-Dashboard)

Statische Seite, die `status.json` aus demselben Verzeichnis lädt (alle 30 s). Ausgeliefert vom Webserver
des Projekts direkt aus diesem Ordner des Arbeitsstands; `status.json` und `logs/<ref>.log` werden dort
getrennt erzeugt und nicht eingecheckt. Reiter: Status, Laptop-Start (mit „Neuen PC einrichten“), Mods, Downloads, Tests, Roadmap, Links, Wiki (dev), KI-Queue.

Aufbau: Kopfzeile mit Live-Punkt, „Jetzt aktualisieren“ und Farbschema (automatisch/hell/dunkel, im Browser gemerkt);
Reiter mit Zuständen (Status: „läuft“/„rot“, Tests: Anzahl roter Ziele, Roadmap: offene Punkte, KI-Queue: offene Einträge).
Der Reiter Status beginnt mit dem Überblick (master, Arbeitsstand, laufendes Gate mit Fortschritt, Warteschlange),
darunter „Handlungsbedarf“ (rotes Gate, rote Testziele, Job-Fehler) und die Laptop-Start-Knöpfe. Gate-Karten sind
einklappbar (laufend vor rot vor grün), der Rohtext bleibt aufklappbar. Ohne Build, ohne externe Ressourcen.

Die Knöpfe unter „Laptop-Start“ nutzen das Protokoll `simplemods://` des Laptop-Starters.

## Dateien
- `index.html` – Status-Dashboard mit KI-Queue
- `devqueue.py` – HTTP-Server für die KI-Queue (mit Login aus `auth.py`)
- `auth.py` / `set_password.py` – Login (a-zw06) / Passwort setzen
- `nginx/`, `deploy_zw06.sh`, `smoke_zw06.py` – ein Host mit Login (a-zw06): Vorlagen, Ausrollen, Rauchtest
- `dev-overlay.js` – Overlay für Wiki (dev), lädt `window.sbDevHook` und sendet Anfragen an die API; ✎/„Bearbeiten“ an Texturen öffnet den Pixeleditor der Wiki (`wiki/assets/pixeleditor*.js`, siehe `wiki/README.md`)
- `modselect.py` – Mod-Baum (Super-Mod ▸ Sub-Mods aus `modules/modules.json` + geplante Sub-Mods), Regeln, Start-Auswahl
- `collect_downloads.py` / `publish_downloads.sh` – Jars eines gate-grünen Stands für den Reiter Downloads einsammeln
- `starter/` – Windows-Starter (`starter.ps1`, Protokoll `simplemods://`) und Installer (`install.ps1`), siehe „Neuer PC“
- `make_wave_bundle.sh` – git bundle des Arbeitsstands für den Starter
- `setup_texture_save.sh` – einmalige Einrichtung von „Speichern ins Repo“ auf dem Webserver (siehe unten)
- `tests/` – Unittests (`python3 -m unittest discover -s tests`), `tests/harness.py` = lokale Vorschau mit Mock-Backend

## API
Hinter dem Zugangsschutz der Zentrale:

- `GET /api/queue` – Liste offener/gestarter Einträge
- `POST /api/queue` – Neuer Eintrag; `kind: suggestion` („Änderung vorschlagen“, a-wikiux06) zusätzlich mit
  `object {id,label}` und `attachments [{name,type,data(base64)}]`: ≤ 5 Bilder, je ≤ 2 MB, zusammen ≤ 8 MB, nur
  PNG/JPEG/WebP (Magic-Bytes müssen zum Typ passen). Der Server vergibt die Dateinamen (`1.png`, …), legt sie unter
  `<data>/attachments/<id>/` ab und kopiert die aktuelle Textur jedes Ziels (per ID aus `wiki/data/textures.js`, Vanilla
  aus `wiki/assets/vanilla/`) als `ref-<n>.png` dazu. Body bis 12 MB → nginx `client_max_body_size 12m` für `/api/`
  (`setup_texture_save.sh` hebt ein altes `2m` an).
- `GET /api/queue/<id>/files/<name>` – Anhang/Referenz eines Vorschlags (nur Server-Namen, sonst 404)
- `PATCH /api/queue/<id>` – Kommentar/Status/Ergebnis aktualisieren
- `DELETE /api/queue/<id>` – Eintrag löschen
- `POST /api/run` – Lauf starten (textures/code/all)
- `GET /api/runs` – Läufe auflisten
- `GET /api/mods` – Mod-Baum, eigene Auswahl (`off`), aufgelöster Zustand (Sperren/Gründe) und Start-Daten
- `PUT /api/mods/selection` – `{"off": [ids]}` speichern (je Nutzer, `<data>/selections/<hash>.json`)
- `POST /api/textures` – Pixeleditor „Speichern ins Repo“ `{id, source, layers, png, comment}` (Regeln unten)
- `GET /api/textures` / `GET /api/textures/<sid>` – Speicherungen (Liste ohne Ebenen) / eine mit Ebenen; b-texed16: Feld
  `adopted {commit, subject, bundle}`, wenn die Save-ID in einem Commit dieses Checkouts steht (nur Lesen, je HEAD gecacht)
- `PATCH /api/textures/<sid>` – Abholer meldet `{state: picked|ready|failed, note, branch}`

## Textur speichern (TX22-5)
Kette: Dev-Wiki ▸ ✎ an der Textur ▸ Pixeleditor ▸ **„Speichern ins Repo“** ▸ `POST /api/textures` ▸ Abholer ▸ Branch
`tex-<name>-<sid>` ▸ Übernahme mit dem **nächsten Bündel** (Hauptsitzung: `git branch --list 'tex-*'` im Hub, mergen,
Generatoren `wiki/generate.py --all` (auch `wiki/data/textures.js`), Gate). Kein Claude-Lauf, kein Sofort-Gate.

Regeln (`tools/textures/editor_save.py`, gleich für Endpunkt und Abholer): nur `id` + `source` genau wie ein Eintrag in
`wiki/data/textures.js`, Pfad relativ unter `…/assets/<ns>/textures/…png`, ohne `..`/`\`, nie `build/` (Vanilla-abgeleitet,
A8), auf dem Basis-Branch eingecheckt; Ebenen ≤ 1 MB, gültig (`layers.validate`), Größe wie die Textur (≤ 512×512);
ein mitgeschicktes PNG (≤ 256 KB) muss der Komposition gleichen. Eingecheckt wird die **neu kodierte Komposition**, nie
die hochgeladenen Bytes; Kommentar ≤ 500 Zeichen ohne Steuerzeichen, nur in der Commit-Nachricht (über stdin).
Gespeichert wird unter `<data>/textures/<sid>.json`, getrennt von `runs/` (die gehen an Claude).

Abholer (Hub-Rechner, cron jede Minute, ohne Worktree – git-Plumbing direkt im Hub-Repo):
```
python3 tools/textures/editor_save.py pickup --git-dir <hub.git> --api "ssh -o BatchMode=yes <webserver>" [--base claude-wave1]
```
Er setzt `picked`, prüft erneut gegen `textures.js` des Basis-Branches, macht den Ebenen-Check
(`layers.check_file`: PNG = Komposition), legt den Branch an (überschreibt nie) und meldet `ready` + Branch bzw.
`failed` + Grund. Der Editor zeigt den Stand unter dem Knopf.

Nur-Lese-Abholer (b-texed16, **nicht eingeschaltet**, Plan `docs/ai/PLAN-B-TEXED16-2026-11-09.md` mit Cron-Zeile):
```
python3 tools/textures/editor_save.py pull --git-dir /root/sb.git --remote "ssh -o BatchMode=yes <webserver>" \
    [--dir /srv/devqueue/textures] [--base claude-wave1] [--report /root/b-fertig.md] [--dry-run]
```
liest per `ls`/`cat` (kein PATCH), je Textur nur die neueste Speicherung, überspringt vorhandene `tex-*-<sid>`-Branches
und Save-IDs, die schon in einem Commit eines Hub-Branches stehen; Fehler/überholte IDs merkt er sich in `--state`.
Konvention für Übernahmen von Hand: Save-ID(s) in die Commit-Nachricht schreiben (daran erkennt die Wiki-Dev „übernommen“).

Einrichtung Webserver (einmal, als root, nach dem Merge in den ausgelieferten Stand):
`tools/zentrale-web/setup_texture_save.sh <checkout> <nginx-site> [dienst]` – prüft Platte und Code, setzt
`client_max_body_size 2m` für `/api/` (Sicherung `.bak-texsave`, `nginx -t`), startet den Dienst neu.

## Ein Host mit Login (a-zw06, Besitzer 06.11.)
`simplemods-wiki.azarfane.com` trägt alles (ab IG31-55 siehe Abschnitt „Domain-Umzug“: `wiki.simplemods.de`); Cloudflare-Pfadschutz kommt später, bis dahin ein eigener Login.

| Pfad | Inhalt | Zugriff |
|---|---|---|
| `/` | Wiki Production (`/srv/wiki-prod/wiki`) | öffentlich (`/assets/vanilla/` immer 404) |
| `/dev/` | Wiki-Arbeitsstand (`/srv/wiki-dev/wiki`) + Overlay `/dev/_overlay.js` | Login |
| `/zentrale/` (`/z`) | diese Seite; `status.json`, `logs/`, `downloads/`, `setup/` aus `/srv/zentrale` | Login |
| `/api/` | devqueue | Login (außer `/api/auth/*`) |
| alter Host wiki-dev | 302 auf `/dev/…` | |
| alter Host zentrale | 302 auf `/zentrale/…` (`/wiki/x` → `/dev/x`); `/setup/`, `/downloads/`, `/status.json` bleiben direkt (Starter/Installer mit cloudflared-Token, Access-App des alten Hosts bleibt) | Cloudflare Access |

Login (`auth.py`, in `devqueue.py`): `GET/POST /api/auth/login` (Formular, `next` nur lokale Pfade),
`POST /api/auth/logout`, `GET /api/auth/check` (200 + `X-Auth-User` bzw. 401, für nginx `auth_request`).
Passwort-Hash PBKDF2-SHA256 (600 000 Runden) in `/srv/devqueue/auth.json` (600, Dienstnutzer, nie im Repo);
Cookie `sb_session` signiert (HMAC, enthält den Hash: neues Passwort beendet alle Sitzungen), HttpOnly, Secure,
SameSite=Lax, 30 Tage. Sperre nach 5 Fehlversuchen je Adresse (`CF-Connecting-IP`) und je Name für 15 min, dazu
nginx `limit_req` 10/min auf `/api/auth/`. nginx reicht als Identität nur die Antwort von `/api/auth/check` weiter,
ein vom Browser gesetzter `Cf-Access-Authenticated-User-Email` kommt nie durch. Wiki-Kopf (`wiki/assets/sitebar.js`):
„Anmelden“, nach Login Umschalter Production | Dev, „Zentrale“, „Abmelden“; ohne Login-Endpunkt (GitHub Pages,
alte Hosts) unsichtbar.

Passwort setzen (Besitzer, auf dem Webserver als root; fragt zweimal ohne Echo):
```
python3 /srv/wiki-dev/tools/zentrale-web/set_password.py --user <name> --email <Cloudflare-Mail wie bisher>
```
Die Mail hält die Mod-Auswahl je Nutzer. `--remove <name>` löscht ein Konto.

Später Cloudflare Access (Schalter, aus): Access-App für `simplemods-wiki` nur mit den Pfaden `/dev/*`,
`/zentrale/*`, `/api/*` anlegen, dann `set_password.py --cloudflare on --team-domain <team>.cloudflareaccess.com
--aud <AUD-Tag>`: `/api/auth/check` prüft dann zusätzlich das Access-JWT (`Cf-Access-Jwt-Assertion`, RS256 gegen
`/cdn-cgi/access/certs`, `aud`, `iss`, `exp`). Passwort-Login bleibt daneben gültig, bis die Konten entfernt sind.

Netz: nginx nimmt nur Verbindungen vom cloudflared-Connector (eigener Container, daher kein Binden an Loopback) und
von Loopback an (`/etc/nginx/snippets/simplemods-allow.conf`, vom Deploy-Skript aus `--tunnel-ip` geschrieben; die IP
steht in der Memory, nicht im öffentlichen Repo). Der Connector braucht dafür eine feste Adresse (DHCP-Reservierung).

### Ausrollen (`deploy_zw06.sh`, als root auf dem Webserver, nach Merge in claude-wave1)
Vorlagen `nginx/*.conf`, das Skript setzt daraus die ganze Site-Datei zusammen; Sicherung beim ersten Lauf
(`*.bak-zw06`), `nginx -t` vor jedem Reload, bei Fehler alte Datei zurück; jeder Schritt mehrfach ausführbar.
1. `deploy_zw06.sh check` – Vorprüfung (Code im ausgelieferten Stand, nginx-Module, `auth.json` 600, Platte).
2. Besitzer setzt das Passwort (`set_password.py`, oben).
3. `deploy_zw06.sh parallel --tunnel-ip <connector-ip>` – neue Seite auf :8084 neben den alten (:8081–8083 wie
   bisher, nur noch vom Tunnel), devqueue-Neustart, Rauchtest (`smoke_zw06.py`: anonym + Wegwerf-Konto, das danach
   wieder gelöscht wird). Aus dem LAN prüfen: `http://<webserver>:8084/` muss 403 geben.
4. `deploy_zw06.sh switch` – :8081 liefert jetzt die neue Seite (dieselbe Tunnel-Route, **in Cloudflare nichts
   umstellen**), alte Hosts leiten um; Rauchtest beider Ports und der Umleitungen.
5. Rückfall jederzeit: `deploy_zw06.sh rollback` (Sicherung zurück, Reload, Sekunden).

Cloudflare: Für den Host `simplemods-wiki` besteht die Route schon (→ :8081), es ist nichts umzustellen. Nur falls
man ohne `switch` testen will: Route vorübergehend auf :8084 legen. Die Access-Apps der alten Hosts bleiben, bis die
Laptops auf die neue Adresse umgestellt sind (Starter lädt `/setup/` mit cloudflared-Token).

Tests: `python3 -m unittest discover -s tests` (`test_auth.py`; `test_nginx_zw06.py` startet mit `SB_NGINX=<nginx>`
die gerenderten Stufen echt gegen devqueue, sonst nur die statischen Prüfungen).

## Start
```bash
python3 devqueue.py --data <ordner> [--port 8090] [--bind 127.0.0.1]
```

Das Overlay wird nur in der Zentrale eingebunden und ruft die API relativ auf (`/api/...`).

## Mods (Spielstart-Auswahl)
Gespeichert wird je Nutzer die Menge der **abgeschalteten** Mods (neue Mods sind damit automatisch an).
Eltern-Schalter schalten ihre Sub-Mods mit; `bundle: required` ist gesperrt, solange die Super-Mod an ist;
`requires` (z. B. SimpleLib) schaltet sich selbst zu. Die Start-Knöpfe hängen `&off=<ids>` an `simplemods://start` an.
Der Laptop-Starter wendet das vor `:integration:runIntegrationClient` an:

```
python tools/zentrale-web/modselect.py apply --off <off-Liste aus der URL>
```

Das schreibt `integration/enabled-mods.json` (abgeschaltete Mods bleiben aus dem Mod-Ordner) und setzt in
`integration/run-fabric-263/config/<super-mod>.json` die Schalter `enableSimpleX` (abgeschaltete Sub-Mods einer laufenden
Super-Mod, die per Jar-in-Jar ohnehin mitkommen). Ohne `off` = alles an. Der Windows-Starter (`starter/starter.ps1`) macht das
selbst (`Invoke-ModSelection`).

## Downloads
Der Reiter liest `downloads/index.json` (neben der Seite, nicht eingecheckt). Nach einem grünen Gate (Gate + Standalone)
im Gate-Checkout `publish_downloads.sh <ref> "<VERDICT-Zeile>" "<Standalone-Zeile>" <Gate-Start>` aufrufen; es sammelt die
Release-Jars aller Loader/MC-Versionen (`*/build/libs`, ohne `-sources`/`-dev`, nur jünger als der Gate-Start) nach
`files/<commit>/<loader>/<mc>/` und behält die letzten drei Stände. Der Gate-Lauf ruft das nach GRUEN automatisch auf (baut vorher alle Loader-Jars) und schickt den Ordner per tar ueber ssh an den
Hub-Rechner; der Webserver holt ihn dort bei geaendertem `index.json` ab und tauscht den Ordner atomar (Symlink). Er liefert ihn
unter `/downloads/` aus (hinter demselben Zugangsschutz). Die Hosts stehen nur in den Skripten ausserhalb des Repos.

## Vorschau
`python3 tests/harness.py --port 8765` (Mock-Status, Mock-Downloads, echte API mit festem Login) und im Browser öffnen;
`--no-downloads --api-fail` zeigt Leer- und Fehlerzustände.

## Neuer PC (Windows-Starter)
Reiter Laptop-Start ▸ „Neuen PC einrichten“: `install.ps1` herunterladen (`/setup/install.ps1`, gespeichert als
`simplemods-install.txt` – keine Browser-Warnung, keine Ausführungsrichtlinie) und den angezeigten Befehl in PowerShell
einfügen (führt die Datei als Scriptblock aus und übergibt die Adresse der Zentrale). Der Installer holt per winget, was fehlt (Git, Eclipse Temurin
JDK 25, Python 3.12 + Pillow/numpy – der Textur-Build `generateVanillaDerived` und `modselect.py` brauchen es; ohne nimmt der
Build Platzhalter –, cloudflared), schreibt `~\.simplemods\config.json`, legt `starter.ps1` dorthin, registriert
`simplemods://` unter `HKCU\Software\Classes` (ohne Admin) und holt den Code einmal. Erneut ausführen = reparieren.

Der Starter (Log `~\.simplemods\starter.log`, Fehler als Popup) klont master öffentlich von GitHub in den Dev-Ordner
(Standard `~\code\simplemods-dev`, Branch `simplemods-run`), holt für `ref=wave` den Arbeitsstand als git bundle aus der
Zentrale, wendet die Mod-Auswahl an und startet `:integration:runIntegrationClient`. Lokale Änderungen im Dev-Ordner brechen
ab statt sie zu überschreiben. Nach jedem Holen übernimmt er seine eigene neue Fassung aus dem Stand.

Zugang ohne Heimnetz und ohne Geheimnisse im Repo: alles unter `/setup/` liegt hinter demselben Zugangsschutz wie die Seite.
Der Starter meldet sich mit `cloudflared access login <zentrale>` einmal im Browser an und schickt das von cloudflared
verwaltete Token als Header `cf-access-token` mit; läuft es ab, öffnet sich die Anmeldung erneut.

Server: `make_wave_bundle.sh <checkout> claude-wave1 <github-url> <ordner>` (vom Sync des Webservers jede Minute, tut nichts,
solange sich der Branch nicht bewegt) schreibt `wave.bundle` (nur die Commits über dem öffentlichen master, wenige MB) und
`wave.json`; ausgeliefert als `/setup/wave.bundle|wave.json`, `install.ps1`/`starter.ps1` direkt aus diesem Ordner.
Tests: `tests/test_setup.py` (Bundle; mit `pwsh` zusätzlich Syntax, PSScriptAnalyzer und der Repo-Abgleich des Starters).

## sb-web-sync (Kopie des Sync-Skripts auf sb-web)

`sb-web-sync` ist die Fassung von `/usr/local/bin/sb-web-sync` auf CT 109 (Timer jede Minute).
Seit 31.10. räumt sie vorab auf, weil die Platte zweimal durch `tmp_pack_*`-Reste abgebrochener
Fetches voll lief und der Starter danach alten Stand lieferte:
- `tmp_*` in `objects/pack` älter als 1 h löschen,
- täglich (bei ≥ 85 % sofort) `git gc --prune=now` für wiki-dev/wiki-prod und Journal auf 200 MB,
- alte `dl-*` löschen, außer dem Symlink-Ziel und einem Rückfall-Ordner.
Danach schreibt sie `web` (`diskPct`, `freeMB`, `devFetchAgeS`) in `status.json`; die Zentrale
zeigt unter „Handlungsbedarf“ eine Warnung ab 85 % Platte oder wenn der Arbeitsstand > 30 min alt ist.

Einspielen (auf pve, Besitzer): `pct exec 109 -- cp /srv/wiki-dev/tools/zentrale-web/sb-web-sync /usr/local/bin/sb-web-sync`
(vorher Backup; das Skript liegt nach dem Merge in claude-wave1 automatisch in `/srv/wiki-dev`).

## Domain-Umzug IG31-55 (vorbereitet, NICHT deployt; Besitzer 06.11.)
Ziel: `wiki.simplemods.de` = Wiki (`/` öffentlich; `/dev/`, `/zentrale/`, `/api/`, `/setup/`, `/downloads/` mit Login);
`simplemods.de` = Landing Page (`/srv/landing`, Quelle `tools/landing/index.html`, solange LAND09 offen ist: Entwurf B);
`www.simplemods.de`, `simplewiki.de`, `www.simplewiki.de`, `simplemods-wiki.azarfane.com` = 301 auf `wiki.simplemods.de` mit
gleichem Pfad. Ausnahme: `/setup/` und `/downloads/` auf der alten azarfane-Adresse bleiben direkt (nur mit Access-Header,
wie bisher). Alle Hosts landen in einem nginx (`:8081`, Auswahl per `server_name`); Vorlagen `nginx/domains.conf`,
`nginx/wiki-extra.conf`. Cookie `sb_session` hat kein Domain-Attribut (host-only): gilt automatisch für wiki.simplemods.de,
der Origin-Check des Logins vergleicht mit dem durchgereichten Host. Alte Sitzungen von azarfane enden (neu anmelden).

Schritte Besitzer (Freigabe nötig, vorher nichts auf sb-web ändern):
1. Freigabe geben; DNS/Tunnel in Cloudflare: Public Hostnames `wiki.simplemods.de`, `simplemods.de`, `www.simplemods.de`
   (und `simplewiki.de`, `www.simplewiki.de`, sobald die Nameserver umgestellt sind) auf den bisherigen Tunnel-Dienst
   (sb-web `:8081`) setzen; der Host-Header muss der öffentliche Name bleiben (kein httpHostHeader-Override).
2. Landing-Wahl LAND09 nennen (dann wird die Datei als `tools/landing/index.html` abgelegt).
3. Alte Cloudflare-Access-App für `/setup/`, `/downloads/` der azarfane-Adresse nicht löschen (Installer nutzen sie).

Schritte A (nach Freigabe, auf sb-web als root, nach `deploy_zw06.sh switch`):
```
/srv/wiki-dev/tools/zentrale-web/deploy_dom07.sh check
/srv/wiki-dev/tools/zentrale-web/deploy_dom07.sh domains   # Sicherung .bak-dom07, Landing kopieren, nginx -t, reload, Smoke je Host
/srv/wiki-dev/tools/zentrale-web/deploy_dom07.sh rollback  # Rückfall in Sekunden
```
Smoke-Test: `python3 smoke_zw06.py --domains --port 8081` prüft jeden Host per Host-Header (Wiki, Login-Pflicht, Landing,
301-Ziele, Ausnahme azarfane `/setup/`+`/downloads/`). Danach im Browser: Login auf wiki.simplemods.de, `/dev/`, `/zentrale/`.
Offen für A: Mod-Metadaten (`homepage`/`displayURL`) zeigen auf `https://azarfane.de/`, nicht auf die Wiki-Adresse, und
bleiben unverändert (eigene Entscheidung des Besitzers).

## Öffentliche Release-Downloads (W12, simplemods.de/downloads/)
`collect_downloads.py` ruft nach jeder Sammlung `public_downloads.py` auf: `<downloads>/public/index.json` +
`public/files/<loader>/<mc>/<jar>` (Hardlinks) aus dem neuesten **vollständigen** grünen Build (SimpleLib je Loader
vorhanden), nur `--public-loaders` (Standard `fabric`, Minecraft 26.3). Je Jar: Version, SHA-256, Größe, `requires`
(modules.json + Jar-Metadaten), `contains` (jar-in-jar), `external`/`externalOptional` (Modrinth-Links, nie mitverteilt).
nginx (`nginx/domains.conf`, Host simplemods.de): `/downloads/` → `/srv/zentrale/downloads/public/`, ohne Login, ohne Listing.
Alles andere unter `/downloads/` bleibt hinter dem Login auf wiki.simplemods.de. Von Hand: `python3 public_downloads.py --out <downloads>`.
