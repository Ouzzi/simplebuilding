# Plan: Keramik-Eimer nach vier Ausgießen (2026-10-09)

## Ursache / Ist-Zustand

`ModBucketItem` zählt derzeit in `CERAMIC_USES` sowohl Schöpfen als auch
Ausgießen. Bei `CERAMIC_USES = 32` und vier Stufen verschiebt sich der
Eimer erst nach acht Aktionen; dadurch hält ein Eimer 16 vollständige
Füllungen. `fill` ruft aktuell ebenfalls `wear` auf. Tests, Tooltip,
Guide-/JEI-Texte und das Wiki beschreiben diese alte Regel.

## Änderung

- Keramik zählt nur noch beim erfolgreichen Ausgießen.
- `CERAMIC_USES = 4`, `CERAMIC_USES_PER_STAGE = 1`; die vier Items der
  Stufen 0..3 bleiben erhalten, jede erfolgreiche Ausgießaktion wechselt
  genau eine Stufe, das vierte Ausgießen leert den Stack.
- `fill` übernimmt den Inhalt und die vorhandene Stufe/Nutzungszahl ohne
  Abnutzung. Das bestehende Break-Sound-Verhalten des echten Item-Uses
  bleibt erhalten.
- EN/DE-Tooltip- und Guide-Texte sowie das generierte Wiki nennen vier
  Ausgießen und kein Abnutzen beim Schöpfen.
- Crucible-GameTests, Katalog und Fabric-Wrapper prüfen die neue Regel;
  der bisherige 32-Uses-Test wird entsprechend umbenannt.

## Dateien

- `common/src/shared/java/com/simplebuilding/fluid/ModBucketItem.java`
- `common/src/shared/java/com/simplebuilding/fluid/ModFluids.java`
- `common/src/shared/java/com/simplebuilding/component/ModDataComponentTypes.java`
- `common/src/shared/java/com/simplebuilding/gametest/CrucibleTests.java`
- `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java`
- `src/main/java/com/simplebuilding/gametest/CrucibleGameTest.java`
- `common/src/shared/java/com/simplebuilding/guide/GuideContent.java`
- `src/main/resources/assets/simplebuilding/lang/{en_us,de_de}.json`
- `mc26_3/overlay/resources/assets/simplebuilding/lang/{en_us,de_de}.json`
- `wiki/manual.json` und generierte `wiki/data/*`

## Abweichungen vom Plan

- Die bestehende Testmethode wurde von
  `ceramicBucketHoldsWaterAndWearsOutAfterThirtyTwoUses` zu
  `ceramicBucketHoldsWaterAndBreaksAfterFourPours` umbenannt; Katalog und
  Fabric-Wrapper wurden synchron angepasst.
- `GuideContent.java` enthält keine Keramikprosa und musste nicht geändert
  werden. Die Keramiktexte liegen in den EN/DE-Langdateien und werden von der
  Guide-/JEI-Datenpipeline verwendet.

## Tests

Vor der Änderung geprüft:

- `python3.12 tools/testrunner/run.py --list`
- Relevantes Ziel: `fabric-263` (29 CrucibleTests), außerdem angefordert:
  `neoforge-263`, `forge-263`.

Nach der Änderung auszuführen:

```text
python3.12 wiki/generate.py --all
python3.12 wiki/generate.py --all --check
python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:crucible_game_test_*'
python3.12 tools/testrunner/run.py --targets fabric-263,neoforge-263,forge-263 --filter 'simplebuilding:crucible_game_test_*'
```

Ergebnisse:

- `python3.12 wiki/generate.py --all`: erfolgreich, Wiki-Daten geschrieben.
- `python3.12 wiki/generate.py --all --check`: erfolgreich, Ausgabe
  `wiki: up to date, everything documented.`
- `python3.12 tools/guide_book_pages.py`: beendet mit zwei bereits
  bestehenden deutschen Zeilenlängen-Problemen in `topics 1` und `topics 3`;
  kein Problem betrifft Keramiktext.
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:crucible_game_test_*'`:
  `alles gruen: 29/29 bestanden, 0 rot`.
- `python3.12 tools/testrunner/run.py --targets fabric-263,neoforge-263,forge-263 --filter 'simplebuilding:crucible_game_test_*'`:
  `alles gruen: 87/87 bestanden, 0 rot` (29/29 je Loader).

Nicht getestet wurden Client-Sicht/visuelle Tooltip-Darstellung sowie
separate Client-GameTests; NeoForge und Forge sind im abschließenden
Server-GameTest-Lauf enthalten.
