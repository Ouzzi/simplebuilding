# Zentrale der Zentralen

Startet die lokalen Zentralen einzeln oder gemeinsam. Über die Zentrale laufen sie immer im
Produktivmodus, nie als Dry-Run (Besitzer 2026-10-01).

```powershell
python tools/zentrale/server.py              # http://127.0.0.1:8760, öffnet den Browser
python tools/zentrale/server.py --start-all  # alle fehlenden starten, Status ausgeben, beenden
```

| Zentrale | Port | Adresse |
| --- | --- | --- |
| Wiki | 8765 | http://127.0.0.1:8765/ |
| Balancing-Zentrale | 8770 | http://127.0.0.1:8770/ |
| Launch- und Testzentrale | 8773 | http://127.0.0.1:8773/#/launch |

## Verhalten

- **Register:** `tools/zentrale/centrals.json`. Eine neue Zentrale ist ein Eintrag mit `id`, `name`,
  `port`, `path` und `args` (Repo-relative Argumente für das Python der Zentrale), optional `env`.
- **Produktivmodus:** Jede Umgebungsvariable mit `DRY_RUN` im Namen wird entfernt; ein Register-Eintrag
  darf keine setzen. Der Hub bekommt `SB_HUB_PRODUCTION=1` und ignoriert dann auch ein gespeichertes
  `dryRun`.
- **Status:** `läuft (von hier gestartet)`, `läuft (extern gestartet)`, `startet` oder `gestoppt`, je nach
  Port und eigenem Prozess.
- **Oberfläche:** gleiche Bausteine wie die Launch- und Testzentrale (die Zentrale liefert deren
  `app.css` mit), Hell/Dunkel wie im Wiki, Statusleiste, je Zentrale Starten, Neu starten, Stoppen,
  Öffnen; dazu Alle starten, Alle neu starten, Alle stoppen, Alle laufenden öffnen.
- **Stoppen:** eigene Prozesse sofort. Extern gestartete Instanzen nach einer Bestätigung, und nur, wenn
  der Prozess am Port wirklich diese Zentrale ist (Python mit dem registrierten Skript in der
  Kommandozeile); ein fremder Prozess am Port wird nie beendet. Beendet wird nur der Server, nicht sein
  Prozessbaum: ein vom Hub gestarteter Minecraft-Client läuft weiter. Gestartete Zentralen laufen weiter,
  wenn die Zentrale selbst endet. API: `POST /api/stop {"ids": "all", "external": true}`.
- **Protokolle:** `.ai-runs/zentrale/<id>.log`.
- **Sicherheit:** nur `127.0.0.1`, fremde `Host`-Header werden abgewiesen, jeder POST braucht
  `X-Zentrale-Client: 1`.
- **Tests:** `python -m unittest discover -s tools/zentrale/tests`.
