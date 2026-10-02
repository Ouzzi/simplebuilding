# Plan: Offline-Testlauf in der Launch- und Testzentrale (2026-10-02)

Branch `claude-offlinehub` (ab 9b00b80a). Nur Python/PowerShell-Tooling, kein Minecraft-Code.

## Ist-Zustand
- Lokales, unversioniertes `.ai-runs/offline-gate.ps1` + `Start-Offline-Gate.cmd` (Haupt-Checkout): testet SHAs
  unbeaufsichtigt im Worktree `%TEMP%\sbgate-offline`, schreibt `offline-results.md`, `offline-status.txt`,
  `offline-logs/`, Lock `offline-gate.lock` (PID). Feste Pfade (Repo, JDK, Python).
- `tools/testrunner/run.py` kennt `SIMPLEBUILDING_GRADLE_OFFLINE=1` (`--offline --configure-on-demand`).
- Hub `tools/launchhub`: `hub/api.py` (Service, Mixins wie `hub/mods.py`), `server.py` (Routing über `route_get/route_post`),
  `static/app.js` (Bereiche als Funktionen, deutsche UI mit ae/oe/ue), Tests `tests/test_hub.py` (unittest), Dry-Run
  `SB_HUB_DRY_RUN`. Identitätsprüfung nach Vorbild `.ai-runs/Start-Alle-Zentralen.ps1` (Win32_Process Name + CommandLine,
  CreationDate vor dem Beenden erneut vergleichen).

## Umsetzung
1. `tools/testrunner/offline_gate.ps1` (pwsh 7): Verhalten wie bisher. Änderungen:
   - Repo = `$PSScriptRoot\..\..`; Ausgabeordner = `.ai-runs` des **Haupt-Checkouts** (über `git --git-common-dir`),
     damit Hub aus Worktree und Haupt-Checkout dieselben Dateien sehen; Override `SB_OFFLINE_RUNS_DIR`.
   - Worktree `SB_OFFLINE_WORKTREE` (Standard `%TEMP%\sbgate-offline`), JDK `SB_OFFLINE_JAVA_HOME`, Java 8
     `SB_OFFLINE_JAVA8_HOME`, Python `SB_OFFLINE_PYTHON` (Standard = bisherige Pfade).
   - Lock: Prozess aus Lock gilt nur als „läuft“, wenn er wirklich ein offline_gate-Skript ist (PID-Wiederverwendung).
   - Refs vorab mit `git rev-parse --verify` prüfen (unbekannte → Fehlerzeile, kein Worktree-Anlegen mit kaputter SHA);
     neuer Worktree wird an der ersten gültigen SHA angelegt.
   - Rot-Zeilen case-sensitiv (`^\s+(ROT|FEHLER) `), damit die Tabellenkopfzeile „rot“ nicht mehr in der .md landet.
   - Zusätzlich `offline-results.json` (schemaVersion 1, letzte 30 Läufe): je Lauf id/start/end/mode/profile/refs/pid/status,
     je SHA: ref, sha, error, notes, groups[name, kind(check|tests), targets, ok, passed, total, failed, red[], log], verdict
     (`green`/`green-no-forge`/`red`), verdictText. Atomar geschrieben (tmp + Move).
   - Starter `tools/testrunner/start_offline_gate.cmd`.
2. Hub: `hub/offline.py` (`OfflineMixin`): `offline_state` (Lock + Identität, Status, JSON-Läufe bzw. Fallback MD-Parser,
   Warteschlange, master-HEAD), `offline_start` (Validierung `git rev-parse`, Doppelstart-Sperre, DETACHED-Start
   `CREATE_NEW_PROCESS_GROUP|CREATE_NO_WINDOW` (+ Breakaway, falls erlaubt); Dry-Run gibt nur argv zurück),
   `offline_stop` (nur bei passender Identität, CreationDate erneut prüfen, `taskkill /T /F`, Lock entfernen),
   `offline_queue_save`, `offline_log` (nur Dateien in `offline-logs`). Routen `/api/offline`, `/api/offline/log`,
   POST `/api/offline/start|stop|queue`; `open_path` kennt `offline-logs`.
   UI: Bereich „Offline-Testlauf“ (Status live, Start-Formular, Stoppen, Warteschlange, Ergebnistabelle, Forge-Hinweis).
3. Tests `tools/launchhub/tests/test_offline.py`: JSON/MD-Parsing, abgebrochene Läufe, Lock/Identität, Stop-Verweigerung,
   Startkommando im Dry-Run (Popen darf nicht laufen), Validierung, Doppelstart, Warteschlange, Parser-Check des .ps1.
4. Doku: `docs/LAUNCHHUB.md` Abschnitt „Offline-Testlauf“, Verweis in `testing/README.md`.

## Annahmen / Entscheidungen
- Dry-Run (`1` und `sim`) startet nichts, liefert nur das Kommando.
- Stop beendet den Prozessbaum hart (`/F`); ein laufender Gradle-Daemon bricht seinen Build beim Client-Abbruch selbst ab.
- Warteschlange: Zeilen müssen auflösbare Refs sein (sonst 400), Kommentare `#` erlaubt.
- Hub zeigt einen Lauf ohne Ende und ohne lebenden Prozess als „abgebrochen“ (Datei wird dafür nicht umgeschrieben).

## Risiken
- `Get-CimInstance` über PowerShell aus Python kostet ~1 s → 4 s Cache.
- Das laufende lokale Skript wird nicht angefasst; altes Lock-Format (nur PID) bleibt kompatibel.

## Verifikation
- `python -m unittest discover tools/launchhub/tests`.
- Skript echt mit unbekannter SHA in Scratch-Ordner (`SB_OFFLINE_RUNS_DIR`) → .md/.json/Status/Lock korrekt.
- Hub aus dem Worktree auf Port 8793 mit `SB_HUB_DRY_RUN=1`, Klick-Test im Browser; Stop-Identität mit Dummy-Prozess.
