# Launch Hub offline (2026-10-04)

## Befund und Plan
- Fabric-26.3-Start konfiguriert unnoetig Forge 26.2; Mavenizer laedt dabei das Launcher-Manifest.
- Gemeinsame Gradle-Optionen in `tools/launchhub/hub/targets.py`: Forge 26.2 nur fuer seine eigenen Targets konfigurieren; Offline-Erkennung mit begrenztem DNS-Check und kurzem Cache, Umgebungsvariable hat Vorrang.
- Launch-, Integrations- und Test-/Check-Aufrufe pruefen; Forge 26.2 offline mit klarer Hub-Meldung ablehnen.
- Regressionstests unter `tools/launchhub/tests`, kurze Dokumentation in `docs/LAUNCHHUB.md`.
- Loom-Version und Offline-Assetvorbereitung ohne Client-Start pruefen. Annahme: bereits gefuellte Gradle-/Minecraft-Caches; fehlende Dateien bleiben ein nachvollziehbarer Offline-Fehler.

## Risiken und Verifikation
- DNS-Erfolg beweist keine Internetverbindung; erzwungenes `SIMPLEBUILDING_GRADLE_OFFLINE=1` bleibt verfuegbar. DNS darf Hub nicht unbegrenzt blockieren.
- Vollstaendiges `check` darf Forge-26.2-Abdeckung online nicht still verlieren.
- uv-Python: `python -m unittest discover -s tools/launchhub/tests`.
- Worktree: `gradlew.bat --offline -PskipForge262=true :mc26_3:fabric:classes`; zusaetzlich Loom-Clientvorbereitung ohne Ausfuehrung von runClient.
- Weitere Projekt-Gates nach Moeglichkeit; keine Minecraft-Clients starten. Ergebnisse und Grenzen hier nachtragen.
- Nur Branch `gpt-hubclient` committen, kein Push; vorgeschriebener Co-Author.

## Umsetzung und Ergebnisse
- Gemeinsame Optionen fuer Starts und Integration; Testprozesse erhalten Offline-/Skip-Optionen ueber ihre eigene Umgebung. Kleine Erweiterung in `tools/testrunner/run.py` fuer das Auslassen von Forge 26.2 auch online.
- Volles Hub-`check` bleibt einschliesslich Forge 26.2 und wird offline klar abgelehnt; keine still reduzierte Gate-Abdeckung.
- uv-Python: `Ran 78 tests in 114.363s` / `OK` (vollstaendige Hub-Suite, einschliesslich DNS-Fehler, Cache, Timeout, Mod-Auswahl und Ablehnung vor Job-Start).
- Geforderter Worktree-Compile `gradlew.bat --offline -PskipForge262=true :mc26_3:fabric:classes`: `BUILD SUCCESSFUL in 39m 42s`, `5 actionable tasks: 5 executed`.
- Loom 1.17.20 aus dem installierten JAR geprueft: Gradles `StartParameter.isOffline()` schaltet den DownloadBuilder offline. Kein zusaetzlicher Loom-Schalter.
- Die zusaetzliche Clientvorbereitung verwendet eine temporaere Gradle-Task, die nur von `runClient.taskDependencies` abhaengt; `runClient` selbst wird niemals ausgefuehrt. Erster Versuch scheiterte an `checkMultimod`/Windows-Python-Stub (9009); Wiederholung mit uv-Python im PATH.
- Wiederholung: `--offline -PskipForge262=true -Phub_mod_selection=true -I hub-prepare-client.init.gradle :mc26_3:fabric:hubPrepareClientOffline` erfolgreich: `BUILD SUCCESSFUL in 7m`, `53 actionable tasks: 30 executed, 23 up-to-date`. Enthalten: `downloadAssets`, `configureClientLaunch`, `stageHubLocalMods`, `syncDevMods`; kein Client-Prozess.
- Umfang: Infrastrukturfix, keine Gameplay-Aenderung/Ports. Keine Minecraft-Clients, Gameplay-Suiten, Testzentralen-Neubauten oder vollstaendigen Multiloader-Gates ausgefuehrt; gezielte Verifikation gemaess Bugauftrag. Kein Push.
