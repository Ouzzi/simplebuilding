# NeoForge module runs (2026-10-05)

## Befund und Plan

Branch: `gpt-neorun`. Nur hier committen, kein Push, kein Client. Gate-Worktree
`%TEMP%/sbgate` ausschliesslich lesen.

1. Gate-Logs und Run-Konfigurationen pruefen. Der neueste Sammellog
   `2026-10-05T07-53-19Z-a45d-module-simplemoney-neoforge-263.log` zeigt fuer Money
   nur SimpleLib im Mod-Container und `Test selection matcher (simplemoney:*) found no tests`.
   `loadedMods.add` ersetzt die Gradle-Konvention statt sie zu erweitern.
   Die erfolgreichen 21 Tests gehoeren zu `:modules:simplelib:neoforge:runModuleIntegrationGameTest`.
   Die Claims-Fehler stammen ebenfalls aus anderen Tasks desselben
   Sammellogs; `ClaimAccessTests.capsAndFailures` erzwingt die atomaren Schreibfehler,
   `ClaimTests.malformed` prueft absichtlich unlesbare Altdaten. Beide Stacktraces
   stehen im Gate-Log; keine Claims-Abschaltung oder Testabschwaechung erforderlich.
2. `gradle/module-neoforge.gradle`: bestehende Mod-Liste beim Hinzufuegen von
   SimpleLib erhalten; gemeinsame Standard-Spielordner nach Modul trennen.
   Die bisherigen elf Ueberschreibungen in `modules/*/neoforge/build.gradle` und
   im Scaffold entfernen. Standalone-Verzeichnisse und erlaubte Mods erhalten.
3. Regression auf der ausgewerteten Gradle-Konfiguration pruefen: eindeutige
   Ordner, eigener Mod bleibt geladen, SB-Integration und SimpleLib bleiben erhalten,
   Standalone laedt keine optionalen Partner. Bestehende GameTests unveraendert ausfuehren.
4. Money/Dimensions zweimal hintereinander, danach alle NeoForge-Modulziele
   inklusive Standalone; jeweils `alles gruen` lesen. `check -q -PskipWiki`,
   Kern-NeoForge und Pflicht-Compiles aus den Wellenregeln. Ergebnisse hier nachtragen.

## Risiken und Annahmen

Alle elf Integrationslaeufe teilen bisher `integration/run-neoforge-263`, obwohl
der Runner mehrere Gradle-Projekte mit `--parallel` startet. Allerdings serialisiert
`integration/build.gradle` die NeoForge-Integrationslaeufe bereits per `mustRunAfter`;
eine konkrete Dateikollision ist deshalb nicht belegt. Diese Reihenfolge bleibt
zur Begrenzung der Serverlast erhalten. Isolierte Verzeichnisse
verhindern Kollisionen von Welten, Configs und Logs. `mods/` und `defaultconfigs/`
im untersuchten Gate-Ordner sind leer. Modul-Abhaengigkeiten kommen aus Gradle;
Tools referenzieren keinen gemeinsamen NeoForge-Modulordner. Keine alten Welten
kopieren, loeschen oder reparieren. Claims-Schutz unveraendert lassen.

## Verifikation

- Gradle-Konfigurationspruefung: `NeoForge run configuration: 21 runs checked,
  unique directories and required mods preserved` (Exit 0).
- Money/Dimensions, erster Lauf: `alles gruen: 61/61 bestanden, 0 rot`,
  `testing/runs/2026-10-05T08-34-24Z-fbfd.json`. Money 19, Dimensions 42 Tests;
  beide Java-Prozesse und Gradle Exit 0, sauberes Speichern/Herunterfahren.
- Money/Dimensions, direkt anschliessende Wiederholung: `alles gruen: 61/61 bestanden,
  0 rot`, `testing/runs/2026-10-05T08-39-26Z-3410.json`, beide Ziele Exit 0.
- Alle elf Modul-NeoForge-Integrationsziele und alle zehn deklarierten Standalone-Ziele:
  `alles gruen: 617/617 bestanden, 0 rot`, alle 21 Ziele Exit 0,
  `testing/runs/2026-10-05T08-41-32Z-c5a1.json`. Auswahl direkt aus
  `tools.testrunner.run.MODULE_TARGETS` mit Suffix `-neoforge-263`, kein Filter.
  SimpleLib hat im bestehenden Manifest keinen separaten Standalone-Eintrag.
- `gradlew.bat check -q -PskipWiki :compileJava :neoforge:compileJava
  -Pforge263=true :mc26_3:forge:compileJava`: `GRADLE_EXIT=0`.
  Log: `testing/runs/neorun-check.log`. Wiki-Gate wie beauftragt ausgelassen;
  keine Wiki-Inhalte oder Gameplay-Quellen geaendert.
- Kern-NeoForge: `alles gruen: 962/962 bestanden, 0 rot`, Exit 0,
  `testing/runs/2026-10-05T08-54-16Z-3a1b.json`. Enthalten und gruen:
  `test_centre_game_test_the_whole_centre_builds_and_matches_its_plan` und
  `test_centre_game_test_every_mod_item_and_block_has_its_place_in_the_test_centre`.
- Kein Client, kein Push, kein Port. Vorhandenes unversioniertes `.serena/`
  bleibt unberuehrt. Ein rein durch Generatoren geaendertes Zeilenende in
  `modules/simpletweaks/generated/resources/wiki/items.json` wurde nach inhaltlichem
  Vergleich mit Git wiederhergestellt; kein generierter Inhaltsdiff.
