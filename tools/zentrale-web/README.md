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

## Testzentrale speichern (N50 TZEDIT50)

Dev-Seite im Wiki `/dev/index.html#/tzeditor` (Seitenleiste Dev → Testzentrale-Editor, `wiki/assets/tzeditor.js`; nur bei `isDevSite()`,
noindex; `/dev/tz-editor.html` leitet dorthin). Daten: `wiki/data/testcentre/` aus den eingecheckten Configs
(`python3 tools/tzedit/build_editor.py`, Erstexport aus dem Spiel: `tools/tzedit/export_layout.py`).
"Speichern" schickt einen Layout-Bereich an `POST /api/testcentre` (gleicher Login wie der übrige Dev-Bereich);
`tools/tzedit/tzedit_save.py check_save` prüft (Namen, Ids, Platzhalter `#grey/#light/#red/#frame`, gleich viele Zellen wie
Spalten, max. 512 KB) und devqueue legt die Speicherung als JSON unter `<data>/testcentre/` ab - kein git auf dem Web-Host.
`GET /api/testcentre` = Verlauf (ohne Zellen), `GET /api/testcentre/<sid>` = eine Speicherung.

- Einrichtung (einmal, als root auf dem Web-Host, nachdem der Stand in `/srv/wiki-dev` ist):
  `bash /srv/wiki-dev/tools/zentrale-web/setup_testcentre_save.sh /srv/wiki-dev devqueue`
- Abholen (Agent-Host, nur lesend auf dem Web-Host): neue Speicherungen werden je Bereich als Branch
  `tz-layout-<variant>-<area>-<sid>` im Hub committet (Datei `src/main/resources/data/simplebuilding/testcentre/layout_<variant>_<area>.json`,
  immer neu serialisiert), nie auf claude-wave1; ältere Speicherungen desselben Bereichs werden übersprungen:
  `python3 tools/tzedit/tzedit_save.py pull --git-dir /root/sb.git --remote "ssh -o BatchMode=yes sb-web" --report /root/b-fertig.md`
- Automatisch (N99, Cron auf sb-agent, von A eingerichtet): `tools/tzedit/tz_pull.sh` = `pull --seen /root/devq/tz_pull.seen`,
  danach je neuem `tz-layout-*`-Branch `build_editor.py` (wiki/data/testcentre) als zweiter Commit und die Express-Spur
  (`/root/sb-tools-b/express.sh <branch>` → claude-wave1 = Dev; volles Gate mit dem nächsten Bündel):
  `*/5 * * * * /root/wt/claude-wave1/tools/tzedit/tz_pull.sh >> /root/devq/tz_pull.log 2>&1` (Probe: `tz_pull.sh --dry-run`).
- Anzeige im Editor (N99): beim Laden zeigt der Editor je Bereich die neueste Speicherung (`GET /api/testcentre` +
  `GET /api/testcentre/<sid>`), wenn sie vom Repo-Stand abweicht und jünger ist als die Repo-Datei (`Last-Modified`), markiert
  „gespeichert – noch nicht online“ (Leiste + Bereichskopf, orange Ecke an jeder abweichenden Zelle); Schalter
  „Online-Stand zeigen“. Ohne Server-Liste gilt die lokale Kopie der eigenen letzten Speicherung (localStorage). Sobald die
  Speicherung auf claude-wave1 ist, gleicht sie dem Repo-Stand und die Markierung entfällt.
- Tests: `python3 -m unittest tools/tzedit/tests/test_tzedit_save.py`

### Bilder im Editor (N72)

Die Zellen zeigen Vanilla-Bilder nur im Dev-Bereich. Dafür muss `wiki/vanilla_textures.py` im Dev-Checkout laufen.
Das macht `sb-web-sync`, sobald sich der Dev-Stand ändert. Der Lauf schreibt drei Dinge, alle git-ignoriert:

- `wiki/assets/vanilla/**`: die Bilder aus dem Client-Jar.
- `wiki/data/vanilla-icons.json`: Id → Bildpfad, nur Namen. Aufgelöst wie im Spiel über `items/<id>.json` → Modell → Eltern →
  Texturen (`tools/devserver/sbdev/icons.py`). So bekommen auch Stufen, Treppen, Mauern, gewachste Blöcke, Laternen und Achtel
  ihr Bild; Mob-Köpfe ohne flaches Bild das Spawn-Ei des Mobs.
- `wiki/assets/devtex/**`: Modul-Texturen, die das Wiki sonst nicht hat.

Ohne Jar zeigt der Editor den Namen. Einrichtung, einmal:

1. sb-agent, Forced Command `/usr/local/bin/sb-web-gate`, neuer Fall vor `*)`:
   `  sb-jar) exec cat /root/.gradle/caches/fabric-loom/26.3/minecraft-client.jar ;;`
   Alternativ das Jar einmal kopieren:
   `scp ~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar sb-web:/var/lib/sb-web-sync/minecraft-client-26.3.jar`
2. Web-Host: die Repo-Fassung des Sync-Skripts einspielen, mit Sicherung vorher:
   `cp /usr/local/bin/sb-web-sync /usr/local/bin/sb-web-sync.bak-n72 && cp /srv/wiki-dev/tools/zentrale-web/sb-web-sync /usr/local/bin/sb-web-sync`
3. Optional, Web-Host: `apt-get install -y python3-pil python3-numpy`. Damit sind die Vanilla-abgeleiteten Texturen echt;
   ohne diese Pakete sind es beschriftete Platzhalter.

Log: `/var/log/sb-wiki-vanilla.log`. Prüfen: `/dev/data/vanilla-icons.json` liefert 200.

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
**Tunnel-Connectoren (Stand 09.11.2026):** Alle `cloudflared`-Connectoren desselben Tunnels müssen in der Allowlist stehen, sonst bekommt ein Teil der Besuche 403:
`192.168.178.91` (CT 110, pve4), `192.168.178.106` (CT 122, munir), `192.168.178.107` (CT 114, pve5, zweiter Connector seit 09.11.).
Wer `deploy_zw06.sh` mit `--tunnel-ip` aufruft, muss ALLE drei angeben (`--tunnel-ip 192.168.178.91 --tunnel-ip 192.168.178.106 --tunnel-ip 192.168.178.107`); ohne `--tunnel-ip` bleibt die vorhandene Datei unverändert.

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

### Flache Mod-Adressen im Wiki (N74, vorbereitet, NICHT eingespielt – Besitzer-Freigabe nötig)
`nginx/wiki-one.conf` liefert `/<mod>/…` (`^/simple[a-z0-9_]+(/.*)?$`) als `index.html` aus, Production und
verschachtelt unter `/dev/` (dort mit Login, Overlay und `sub_filter` wie jede Dev-Seite). Zusätzlich setzt
`sub_filter` in jede Wiki-HTML-Antwort `<base href="/" data-modpath>` (Dev `/dev/`): Daran erkennt das Wiki, dass der
Server die Pfad-Form kann; ohne Regel bleibt es bei `?mod=…` (nichts bricht, alte Links gehen weiter). Kein allgemeiner
Rückfall auf `index.html`: fehlende `data/*.js`/Bilder bleiben 404. Reihenfolge egal: Regel vor oder nach dem
Production-Push des Wikis (das alte Wiki ignoriert das Kennzeichen, verlinkt keine Pfade).
Einspielen (als root auf sb-web, wenn der Stand mit N74 in `/srv/wiki-dev` liegt):
1. `cp -a /etc/nginx/sites-available/simplemods /root/simplemods.pre-n74` (eigene Sicherung; `deploy_dom07.sh
   rollback` ginge bis vor den Domain-Umzug zurück).
2. `/srv/wiki-dev/tools/zentrale-web/deploy_dom07.sh check`, dann `… deploy_dom07.sh domains` (rendert alle Hosts aus
   den Vorlagen neu, `nginx -t`, Reload, Rauchtest je Host).
3. Prüfen: `curl -sI -H 'Host: wiki.simplemods.de' http://127.0.0.1:8081/simpleend/items/ashwood_boat` → 200
   `text/html`; `curl -s … /simpleend/ | grep -c 'data-modpath'` → 1; `curl -sI … /data/fehlt.js` → 404;
   `curl -sI … /dev/simpleend/` ohne Cookie → 302 auf `/api/auth/login?next=/dev/simpleend/`.
4. Im Browser `https://wiki.simplemods.de/dev/simplebuilding/` (Klick-Test N74) und `https://wiki.simplemods.de/?mod=simpleend`
   → landet auf `/simpleend/`.
5. Rückfall: `cp -a /root/simplemods.pre-n74 /etc/nginx/sites-available/simplemods && nginx -t && systemctl reload nginx`
   (das Wiki fällt von selbst auf die Query-Form zurück; schon geteilte `/<mod>/…`-Links gäben dann 404).
Test: `test_nginx_zw06.py` (`test_flat_mod_paths_n74`, statisch und mit `SB_NGINX` echt: Pfade 200 mit `<base`, Daten-404,
`/dev/simpleend/` ohne Login 302, mit Login 200 + Overlay).

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
`simplewiki.de`, `www.simplewiki.de`, `simplemods-wiki.azarfane.com` = 301 auf `wiki.simplemods.de` mit
gleichem Pfad; `www.simplemods.de` = 301 auf `simplemods.de` (Landing, Besitzer 12.11.). Ausnahme: `/setup/` und `/downloads/` auf der alten azarfane-Adresse bleiben direkt (nur mit Access-Header,
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
