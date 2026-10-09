# Zentrale (Status-Dashboard)

Statische Seite, die `status.json` aus demselben Verzeichnis lädt (alle 30 s). Ausgeliefert vom Webserver
des Projekts direkt aus diesem Ordner des Arbeitsstands; `status.json` und `logs/<ref>.log` werden dort
getrennt erzeugt und nicht eingecheckt. Reiter: Status, Tests, Roadmap, Laptop-Start, Links, Wiki (dev), KI-Queue.

Die Knöpfe unter „Laptop-Start“ nutzen das Protokoll `simplemods://` des Laptop-Starters.

## Dateien
- `index.html` – Status-Dashboard mit KI-Queue
- `devqueue.py` – HTTP-Server für die KI-Queue
- `dev-overlay.js` – Overlay für Wiki (dev), lädt `window.sbDevHook` und sendet Anfragen an die API
- `tests/` – Unittests für `devqueue.py`

## API
Hinter dem Zugangsschutz der Zentrale:

- `GET /api/queue` – Liste offener/gestarter Einträge
- `POST /api/queue` – Neuer Eintrag
- `PATCH /api/queue/<id>` – Kommentar/Status/Ergebnis aktualisieren
- `DELETE /api/queue/<id>` – Eintrag löschen
- `POST /api/run` – Lauf starten (textures/code/all)
- `GET /api/runs` – Läufe auflisten

## Start
```bash
python3 devqueue.py --data <ordner> [--port 8090] [--bind 127.0.0.1]
```

Das Overlay wird nur in der Zentrale eingebunden und ruft die API relativ auf (`/api/...`).
