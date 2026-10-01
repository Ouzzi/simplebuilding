# Launch- und Testzentrale (Launch & Test Hub)

`python tools/launchhub/server.py` (Port 8771, `.claude/launch.json` Eintrag `launchhub`). Nur Python-Standardbibliothek,
bindet an 127.0.0.1, lehnt fremde Host-Header ab. Neben Wiki (8765) und Balancing-Zentrale (8770) ist sie der dritte
Eintrag in `launch.json`; die einzelnen Client-/Server-Einträge sind dort entfernt, ihre Gradle-Befehle stehen als Daten in
`tools/launchhub/launch_targets.json`.

## Bereiche

Auf der Startseite enthält „Mods für den Start“ die einklappbare Auswahl der
Projektmods. Standardmäßig sind alle registrierten Projektmods ausgewählt.
„Auswahl speichern“ übernimmt die Auswahl für den nächsten normalen 26.3-Start
auf Fabric, NeoForge oder Forge; SimpleBuilding selbst bleibt dabei geladen.
Die Auswahl gilt für Client, Server und frische Testwelt. Entwickler-Mods und
Presets bleiben im Bereich „Mods“. Ältere Minecraft-Linien sind unverändert.
- **Starten**: Karte je MC-Linie (26.3 hervorgehoben), Zeile je Loader mit *Client + frische Testwelt* (Standard), *Client*,
  *Server*, *Tests*, *Client-Tests*. Rechtsklick (oder Umschalt+F10 / Knopf ⋯) öffnet das Kontextmenü mit Stoppen,
  Erzwingen, Log, Laufordner. Jede Instanz ist eine Prozesskarte (Status, Live-Log, Exit-Code).
- **Tests**: „Alle Tests ausführen“ (Vorauswahl 26.3 Fabric + NeoForge; Voreinstellungen für alle Server-Linien und Client-Suiten),
  Filterlauf, nur Fehlgeschlagene, Einzeltest nochmal, Auswahl mehrerer Tests. Kacheln je Ziel mit Sparkline, Trend,
  „Vor dem Push“-Liste, Tabelle mit Suche, Gruppierung nach Testklasse, „nur rote“, „nur wackelige“.
- **Fehlschlaege**: aktuell rote Tests (neueste Aufzeichnung je Test), seit welchem Lauf rot, Fehlertext, Markdown kopieren.
- **Verlauf**: Läufe, Detail, Markdown-Export, zwei Läufe vergleichen (neu rot / neu grün).
- **KI-Fixes**: Anbieter-Status, Jobs mit Log, Branch, Commit, geänderten Dateien, Merge-Vorschau, Diff.
- **Worktrees**: Aufräumhilfe für `.claude/worktrees` (Größe, gemergt/ungemergt, ungesichert), Gate-Worktree.
- **Einstellungen**: `tools/launchhub/settings.json` (nicht im Git).
Tastatur: `r` Fehlgeschlagene wiederholen, `a` alle (26.3), `/` Suche, `1`–`7` Bereich, `?` Hilfe.

## Regeln, die der Hub durchsetzt
- **Urteil nur aus dem Datensatz.** `run.py` endet auch bei roten Tests mit Exit 0. Der Hub liest `testing/runs/*.json`
  (`runs.verdict`); null Tests oder ein Zielfehler sind nie grün. Mutationsläufe (`trigger` beginnt mit `mutation-`) sind
  absichtlich rot und zählen nicht in Fehlerlisten/Trends (Einstellung „Mutationsläufe einbeziehen“).
- **Ein Filtermuster pro Aufruf.** Kommas werden abgelehnt. Mehrere Tests → der Hub bildet gültige Einzelmuster
  (Klassen-Wildcard nur, wenn sie genau die Auswahl trifft; sonst exakte IDs; gleiche Muster für beide Loader in einem Aufruf)
  und reiht sie nacheinander in **einem** Job ein.
- **Nicht im Haupt-Repo bauen, solange Besitzer-Clients laufen.** Tests und `check` laufen standardmäßig im Gate-Worktree
  (`%TEMP%/sbgate`, Überschreiben mit `SB_HUB_GATE_DIR`); der Hub setzt ihn vorher auf HEAD (verweigert bei lokalen
  Änderungen). Im Repo starten geht nur nach Bestätigung, wenn dort ein vom Hub gestarteter Client/Server läuft.
  Die Datensätze aus dem Gate werden nach `testing/runs` des Repos kopiert (`runs.sync_runs`).
- **Platz**: unter `minFreeGb` (8) startet nichts. Gradle-Lock-Warnung bei parallelen Builds im selben Ordner.
  Zwei Server gleichzeitig gehen nicht (Port 25565); gleiches Ziel+Modus zweimal nicht (gleiches Laufverzeichnis).
- **Vor dem Push**: das grüne Abzeichen „Bereit zum Push“ gibt es erst, wenn (1) der Arbeitsbaum sauber ist,
  (2) Gradle `check` für genau den HEAD-Commit grün war (Exit-Code von Gradle, gespeichert in `tools/launchhub/data/checks.json`)
  und (3) ein ungefilterter Lauf `fabric-263` + `neoforge-263` für genau diesen Commit „alles gruen“ ist, ohne fehlende Tests.
  Trockenläufe zählen nie.

## Frische Testwelt
`/sbtestcentre build` läuft in Entwicklungsumgebungen beim ersten Betreten einer Welt namens `SB-Testzentrale` von selbst
(`TestCentreCommand.onPlayerJoin`; erkennt „alt“ am Fingerabdruck in `simplebuilding_testcentre.txt`, siehe `docs/TESTZENTRALE.md`).
Der Hub löscht deshalb vor dem Start nur diese Datei (`world: rebuild`, Standard) – das Testzentrum baut sich neu. Optional
`world: recreate` verschiebt die Welt nach `<Laufordner>/hub-old-worlds/`. Eine neue Welt selbst kann der Hub nicht anlegen
(kein Headless-Weg; einmal Flachland, Kreativ, Cheats an). Existiert die Welt, hängt der Hub `--args=--quickPlaySingleplayer
SB-Testzentrale` an (abschaltbar). Client-Tests einzeln: Feld „Client-Testnamen“ setzt `SIMPLEBUILDING_CLIENT_ONLY`.

## KI-Fix
„Mit KI beheben“ nimmt die ausgewählten roten Tests (Standard alle), baut den Prompt aus `tools/launchhub/prompt_template.md`
(Platzhalter `{{failures}}`, `{{file_hints}}`, `{{rerun_commands}}`, `{{branch}}` …; unbekannte Platzhalter meldet die Vorschau),
zeigt ihn in der Vorschau (kopierbar für jeden Assistenten) und startet den Assistenten erst nach Bestätigung in einem **neuen**
Worktree `.claude/worktrees/hubfix-<zeit>` auf Branch `hub-fix/<zeit>`. Nie automatisches Mergen oder Pushen; die Karte zeigt
Commit, geänderte Dateien, Merge-Vorschau (`git merge-tree`), Diff und „Merge-Befehl kopieren“.
Anbieter in den Einstellungen; Befehlsvorlage editierbar, jedes Wort wird ein Argument (keine Shell), Platzhalter `{prompt_file}`,
`{prompt}`, `{worktree}`, `{branch}`, Prompt per stdin (Standard). Voreinstellungen `claude -p --permission-mode acceptEdits`
und `codex exec --full-auto -C {worktree} -`. **Nicht geprüft**: beide CLIs waren beim Bau nicht installiert; vor dem ersten Einsatz
`claude --help` / `codex exec --help` lesen und die Vorlage anpassen. Nicht installiert → Karte „nicht installiert“ mit Installationshinweis.

## Sicherheit
GET ändert nichts. Jedes POST braucht `X-Hub-Client: 1`, JSON und wird gegen Erlaubnislisten geprüft (Startziel-IDs, Testziel-IDs
aus `run.py`, nur aktuell bekannte Tests, Worktrees nur unter `.claude/worktrees`). Es gibt keine Shell und keine Nutzerstrings
in Befehlen außer als einzelne Argumente. Der Hub beendet beim Schließen keine gestarteten Clients.

## Trockenlauf
`SB_HUB_DRY_RUN=1` (oder Einstellung): nichts wird gestartet, Worktrees/Welten/Dateien werden nicht angefasst, die Befehle stehen
im Log; die Kopfleiste zeigt „Trockenlauf“. `SB_HUB_DRY_RUN=sim`: wie 1, aber jeder Job führt einen harmlosen simulierten Prozess
aus (Live-Log, Stoppen testen). `SB_HUB_RUNS_DIR` zeigt auf einen anderen `testing/runs`-Ordner (ein Worktree ohne eigene Läufe
nimmt automatisch den des Haupt-Checkouts).

## Aufbau
`server.py` (HTTP), `hub/api.py` (alle Endpunkte), `hub/runs.py` (Datensätze, Flaky, Trend, Vergleich, Vor-dem-Push),
`hub/targets.py` (Ziele, Befehle, Filtermuster; nutzt `tools/testrunner/run.py` als Modul), `hub/processes.py`
(Jobs, Log, Prozessbaum beenden), `hub/ai.py`, `hub/worktrees.py`, `hub/settings.py`, `static/`.
Logs unter `tools/launchhub/logs/` (die letzten 300 Jobs), alles Lokale ist gitignored.
Tests: `python -m unittest discover tools/launchhub/tests` (nicht an `gradlew check` gehängt, wie die der Balancing-Zentrale).
Die ältere Oberfläche `tools/testrunner/serve.py` + `testing/index.html` bleibt funktionsfähig, ist aber abgelöst.

## Grenzen
Windows-Prozessbaum: `taskkill /T`; die Spiel-JVM gehört dem Gradle-Daemon, der bei Trennung den Build abbricht – bleibt ein Spiel
stehen, im Task-Manager beenden. Diagnosetexte des Servers sind Englisch, die Oberfläche Deutsch.
