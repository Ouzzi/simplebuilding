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
- `devqueue.py` – HTTP-Server für die KI-Queue
- `dev-overlay.js` – Overlay für Wiki (dev), lädt `window.sbDevHook` und sendet Anfragen an die API
- `modselect.py` – Mod-Baum (Super-Mod ▸ Sub-Mods aus `modules/modules.json` + geplante Sub-Mods), Regeln, Start-Auswahl
- `collect_downloads.py` / `publish_downloads.sh` – Jars eines gate-grünen Stands für den Reiter Downloads einsammeln
- `starter/` – Windows-Starter (`starter.ps1`, Protokoll `simplemods://`) und Installer (`install.ps1`), siehe „Neuer PC“
- `make_wave_bundle.sh` – git bundle des Arbeitsstands für den Starter
- `tests/` – Unittests (`python3 -m unittest discover -s tests`), `tests/harness.py` = lokale Vorschau mit Mock-Backend

## API
Hinter dem Zugangsschutz der Zentrale:

- `GET /api/queue` – Liste offener/gestarter Einträge
- `POST /api/queue` – Neuer Eintrag
- `PATCH /api/queue/<id>` – Kommentar/Status/Ergebnis aktualisieren
- `DELETE /api/queue/<id>` – Eintrag löschen
- `POST /api/run` – Lauf starten (textures/code/all)
- `GET /api/runs` – Läufe auflisten
- `GET /api/mods` – Mod-Baum, eigene Auswahl (`off`), aufgelöster Zustand (Sperren/Gründe) und Start-Daten
- `PUT /api/mods/selection` – `{"off": [ids]}` speichern (je Nutzer, `<data>/selections/<hash>.json`)

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
