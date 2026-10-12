# Zentrale (Status-Dashboard)

Statische Seite, die `status.json` aus demselben Verzeichnis lädt (alle 30 s). Ausgeliefert vom Webserver
des Projekts direkt aus diesem Ordner des Arbeitsstands; `status.json` und `logs/<ref>.log` werden dort
getrennt erzeugt und nicht eingecheckt. Reiter: Status, Laptop-Start, Mods, Downloads, Tests, Roadmap, Links, Wiki (dev), KI-Queue.

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
Super-Mod, die per Jar-in-Jar ohnehin mitkommen). Ohne `off` = alles an. Fertiger Baustein für den Windows-Starter:
`starter-modselect.ps1` (neben `starter.ps1` legen, zwei Zeilen einfügen – steht im Kopf der Datei).

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
