# Launch-Hub: frische Testwelt (2026-10-05)

Branch: `gpt-hubworld`. Nur dieser Worktree, kein Push, kein Minecraft-Client.

## Befund und Plan

1. `client_fresh` verwendet standardmaessig `rebuild` und entfernt nur die
   Ursprungsdatei. Welt, Spielerstand und Seed bleiben erhalten. Standard auf
   `recreate` aendern, bestehende Welt mit eindeutigem Namen archivieren und
   QuickPlay auf den freigewordenen Weltordner richten. Explizites `rebuild`
   erhaelt die Ursprungskoordinaten und entwertet nur den Fingerabdruck.
2. Neuere Besitzerlogs (`launch-20261004-210025-961a.log`,
   `launch-20261005-052258-0555.log`) zeigen Bau bei `0 -64 0`.
   `savedOrDefaultOrigin` fragt die Hoehe ohne geladenen Chunk ab; der Boden
   bei y=-65 kann nicht gebaut werden. Ursprungschunk vorher laden und
   ungueltige gespeicherte Hoehen reparieren. Weltspawn am gebauten Eingang
   setzen, Spieler nach dem Bau sicher teleportieren, Fallbewegung loeschen.
3. QuickPlay benutzt bereits `WorldDataConfiguration.DEFAULT`, keine extra
   experimentellen Feature-Packs. Die Vanilla-Lifecycle-Warnung bei modifizierten
   Registries nur fuer explizite Hub-Dev-Starts auf 26.3 automatisch bestaetigen.
   Vanilla-Methoden anhand des lokalen 26.3-Jars pruefen; Property nur an den
   Clientprozess der Hub-Starts weiterreichen. Normale Installationen unveraendert.

Dateien: `tools/launchhub/hub/{api,targets}.py`, Hub-Tests/UI,
`TestCentreCommand`, GameTests, 26.3-Client-Mixin/Registrierung,
Gradle-Hub-Konfiguration, `docs/LAUNCHHUB.md`, Queue/Handoff.

## Risiken und Verifikation

- Weltarchiv statt Loeschung: Besitzerbauten bleiben erhalten. Pfade weiterhin
  mit `safe_join`; gleiche Zeitstempel duerfen Archive nicht ueberschreiben.
- Explizites Rebuild muss bei vorhandenem Ursprung lagefest bleiben.
- Dev-Warnungsumgehung darf weder Versions-/Datapackfehler noch normale
  Spieler-Installationen beeinflussen; kein Clientstart zur Pruefung erlaubt.
- Python: uv-Python 3.12, `-m unittest discover -s tools/launchhub/tests`.
- Spawn-/Teleport-GameTest, gefiltert; danach volle Fabric-/NeoForge-26.3-Suiten
  mit gelesener Zeile `alles gruen`, Neubau und vollstaendiger Itemabdeckung.
- `check -q -PskipWiki`, Compile 26.2 Fabric/NeoForge und Forge 26.3.
- Wiki generieren/pruefen; keine neue Wiki-Behauptung ueber ungepruefte UI.
- Besitzer prueft zwei aufeinanderfolgende frische Starts, sicheren Eingang,
  Wiederbetreten/Respawn und ausbleibenden Experimental-Dialog im Hub.

## Ergebnis

Alle drei Fixes umgesetzt. Die neue Java-Funktionalitaet ist auf 26.3 begrenzt
(`McVersion.HUB_TEST_WORLD`; 26.2 hat einen leeren Client-Mixin-Zwilling).
Die Warnungsumgehung aendert nur die lokale Lifecycle-Entscheidung, keine
gespeicherten Weltdaten. Die Methodensignaturen und die Lifecycle-Abfrage wurden
am lokalen Vanilla-26.3-Client-Jar mit `javap` geprueft.

### Testbelege (2026-10-05)

- Neue Hub-Tests: `python -m unittest discover -s tools/launchhub/tests -p test_worlds.py`:
  `Ran 5 tests ... OK`. Archivierung inklusive Spielerstand, gleiche Zeitstempel,
  lagefestes Rebuild, fehlende Welt, RunDir/QuickPlay und Client-/Versions-Opt-in.
- Gesamte Hub-Suite: `Ran 87 tests ... FAILED (failures=1)`, also **86/87**.
  Ein unveraenderter Bestandsfehler:
  `test_standalone_targets_cover_every_module_suite_and_only_hard_requirements`:
  `simplelib needs tests.standalone (principle 8)`. Sowohl der betroffene Test
  als auch `modules/modules.json` blieben unveraendert. Kein Test wurde abgeschwaecht.
- Fabric-Filter `simplebuilding:test_centre_game_test_*`:
  **`alles gruen: 8/8 bestanden, 0 rot`**,
  `testing/runs/2026-10-05T05-52-49Z-a47f.json`.
- Volle Server-Suiten Fabric/NeoForge 26.3, je **961/961**:
  **`alles gruen: 1922/1922 bestanden, 0 rot`**,
  `testing/runs/2026-10-05T06-09-34Z-7614.json`.
  Enthalten: neuer Spawn-/Teleporttest, Testzentralen-Neubau, echte Befehlsbloecke
  und vollstaendige Item-/Block-Abdeckung. Serverziele liefen parallel, kein Client.
- Gate samt Pflicht-Compiles in einem Gradle-Aufruf:
  `check -q -PskipWiki :compileJava :neoforge:compileJava :mc26_3:forge:compileJava`
  mit `-Pforge263=true`: **`GRADLE_EXIT=0`**.
  Zusaetzlich wurde mit `-Phub_client=true -Pforge_runs=true` und einem lokalen
  Init-Skript die Run-Konfiguration ausgelesen, ohne runClient auszufuehren:
  **`HUB_CLIENT_PROPERTIES=OK (Fabric, NeoForge, Forge; servers unchanged; no client launched)`**.
- `wiki/generate.py --all` und `--all --check`: beide **Exit 0**,
  `wiki: up to date, everything documented.` Kein Wiki-Inhaltsdiff.
- `git diff --check`: ohne Befund.

Die Serverlaeufe nutzten den warmen Cache (`SIMPLEBUILDING_GRADLE_OFFLINE=1`),
das komplette Gate enthielt auch Forge 26.2. Fuer uv-Python, Gradle-Cache und
den gemeinsamen Git-Index waren erweiterte Rechte erforderlich. Keine Arbeit
im Haupt-Checkout; dort wurden nur die genannten Besitzerlogs gelesen.
Lokale Belege: `.ai-runs/hub-validation/`; Property-Pruefung:
`.ai-runs/hub-verify.gradle`.

### Offen fuer die Besitzerabnahme

Nach Uebernahme des Commits und Hub-Neustart zweimal nacheinander eine frische
Fabric-26.3-Testwelt starten (Client dazwischen schliessen). Eigene Markierung
aus Lauf 1 darf in Lauf 2 nicht vorhanden sein; alte Welt liegt im Archiv.
Eingang, Wiederbetreten und Respawn pruefen. Experimental-Dialog darf bei Hub-
Starts entfallen; normale Installationen behalten Vanilla. Keine echte
Client-/Dialogpruefung durch diesen Run, kein Forge-Serverlauf, kein Port/Push.
