# Plan WB24-8 / REV24-F-14: Gametest- und Dev-Code aus dem Release-Jar (26.3 Fabric)

Branch `b-wb24f14` (Basis claude-wave1 e11eed7ef). Nur MC 26.3 + Fabric.

## Ist-Zustand

- `:mc26_3:fabric` kompiliert `common/src/shared/java`, `src/main/java`, `mc26_3/overlay/java` (+ JEI/Jade) in **ein**
  `main`-Sourceset. Darin liegen `com.simplebuilding.gametest` (Server-Gametests: 116 Dateien shared, 108 Fabric-Adapter
  in `src/main/java`, 8 Helfer im Overlay) und `com.simplebuilding.dev.testcentre` (Testzentrale, 18 + 1 Dateien).
  Das `gametest`-Sourceset von Loom enthält nur die Client-Tests und ist schon getrennt.
- `src/main/resources/fabric.mod.json` (geteilt mit 26.2) listet ~107 `fabric-gametest`-Entrypoints.
- Läufe: `fabric-263` = `:mc26_3:fabric:runGametest` (Loom-Dev-Lauf, lädt die Klassen-Ordner, nicht das Jar).
  `integration-263`, `module-*-263`, `module-*-standalone-*` laden das **Jar** (`:mc26_3:fabric:jar`) in einen
  mods-Ordner, filtern aber auf `sbintegration:*` bzw. den Modul-Namensraum; SBs eigene Gametests laufen dort nicht.
- Gate-Auslieferung (`/root/dist-publish.sh` → `tools/zentrale-web/collect_downloads.py`) nimmt
  `mc26_3/fabric/build/libs/*.jar` ohne `-dev/-sources/...` = Ausgabe von `jar`.
- Messung Release-Jar e11eed7ef (dist): 46,2 MB; SB-eigene Klassen 1150 + **338 gametest/dev** (2,27 MB komprimiert,
  47 % der SB-Klassenbytes). Rest: Schallplatten 12,3 MB, eingebettete Sub-Mod-Jars 23,3 MB.
- Produktivcode, der `dev` anfasst (4 Stellen): `SimplebuildingCommand.register` (`/sbtestcentre`),
  `TweaksContent.onServerTick` (`TestCentreCommand.serverTick`), `Simplebuilding` (JOIN → `onPlayerJoin`),
  `QuickPlayTestWorldMixin` (nur die Konstante `WORLD_NAME`, von javac eingesetzt, und hinter `isDevelopmentEnvironment`).
  `gametest` wird von keinem Produktivcode benutzt (nur Unit-Test `PlacedDisplay3dTests`, der die Klassen-Ordner nutzt).
- Module: eingebettete Modul-Jars tragen 2–24 eigene Gametest-Klassen. Die werden von den Integrations-/Standalone-
  Zielen **aus dem Jar** ausgeführt → dort nicht billig trennbar (bräuchte eigene Test-Jars + Umbau von integration).

## Entscheidung: Release-Jar-Filter statt eigenem Sourceset

Eigenes Sourceset hieße: Quellen der geteilten Bäume aufteilen, die auch 26.2/NeoForge/Forge kompilieren (dort darf
nichts angepasst werden), eigenes Test-Mod-JSON, Loom-Classpath-Gruppen ändern. Der Filter dagegen ändert die Dev-/
Testläufe gar nicht (sie nutzen die Klassen-Ordner) und trifft genau das ausgelieferte Artefakt.

## Änderungen

1. `mc26_3/fabric/build.gradle`, Task `jar`:
   - `exclude 'com/simplebuilding/gametest/**', 'com/simplebuilding/dev/**'`
   - `fabric.mod.json` im Jar: Zeilen mit `"com.simplebuilding.gametest.` entfernen (Liste bleibt leer, JSON gültig).
   - `doLast`-Prüfung: fabric.mod.json im Jar parsen; jeder `com.simplebuilding.*`-Entrypoint muss als Klasse im
     Jar liegen, kein Eintrag unter gametest/dev → sonst Build-Fehler (schützt vor künftigen Abhängigkeiten).
2. `ModEnvironment.hasTestCentre()` (geteilt, ladefrei per `Class.forName(..., false, ...)`), damit die 3 Aufrufer
   die Testzentrale nur anfassen, wenn ihre Klassen da sind. Bewusst: `/sbtestcentre` gibt es im Release nicht mehr
   (Review-Vorschlag „nur Entwicklungsumgebung“); Hub/runClient/Gametests sind Dev-Läufe und behalten sie.
   NeoForge/Forge/26.2-Jars bleiben unverändert (Klassen vorhanden → true).
3. `PistonEventProbe`/`ProtectionProbe` bleiben (kleine Produktiv-Hooks in `platform`, keine Testklassen).
4. Module: nicht geändert (siehe oben), im Bericht vermerkt.

## Verifikation

- Compile `:mc26_3:fabric:compileJava`, Jar bauen, Größe/Klassenzahl vorher/nachher, Jar-Inhalt (`gametest/dev` = 0,
  fabric.mod.json ohne gametest-Einträge).
- `fabric-263` voll, `integration-263`, 2–3 Standalone-Module, `./gradlew check`, Generatoren.
- Gate-Skript auf sb-test muss nicht geändert werden (nimmt weiter `jar`-Ausgabe).
