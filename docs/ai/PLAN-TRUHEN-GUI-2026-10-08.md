# Plan: Truhen-GUI-Titel und Stapelanzeige (2026-10-08)

## Ursache

Fallen-Truhen verwenden im Standardnamen die eigene Block-ID (`*_trapped_chest`).
Dadurch zeigt ein einzelnes GUI den Fallen-/Redstone-Namen, während der
Doppeltruhen-Fallback ebenfalls `block.getName()` der Fallen-Truhe nutzt.
`TieredChestScreen` zeichnet außerdem für Netherit und Enderit ein sichtbares
`stack_bonus`-Label rechts neben dem Titel.

## Änderung

- Den Container-/Menünamen einer gestuften Truhe aus der normalen Stufen-ID
  ableiten, wenn es sich um eine Fallen-Truhe handelt. Itemnamen und
  Blocknamen bleiben unverändert.
- Das sichtbare Stapelfaktor-Label aus `TieredChestScreen` entfernen.
- Den dadurch unbenutzten `container.simplebuilding.tiered_chest.stack_bonus`
  Schlüssel in den aktiven 26.3-Sprachdateien entfernen. Item-Tooltips und
  Jade-Informationen bleiben erhalten.
- Einen Fabric-GameTest ergänzen, der die Menü-Titel normaler und gefangener
  Truhen je Stufe vergleicht.

## Dateien

- `common/src/shared/java/com/simplebuilding/blocks/custom/TieredChestBlock.java`
- `common/src/shared/java/com/simplebuilding/blocks/entity/custom/TieredChestBlockEntity.java`
- `common/src/shared/java/com/simplebuilding/util/TieredChests.java`
- `common/src/shared/java/com/simplebuilding/client/gui/TieredChestScreen.java`
- `common/src/shared/java/com/simplebuilding/gametest/TieredChestTests.java`
- `src/main/java/com/simplebuilding/gametest/TieredChestGameTest.java`
- `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java`
- `src/main/resources/assets/simplebuilding/lang/{en_us,de_de}.json`
- `mc26_3/overlay/resources/assets/simplebuilding/lang/{en_us,de_de}.json`

## Tests

Ausgeführt:

```text
python3.12 tools/testrunner/run.py --list | grep -E 'fabric-263|client' | head -40
python3.12 tools/testrunner/run.py --targets fabric-263 --filter tiered_chest_game_test_trapped_titles_match_normal_tiers
python3.12 tools/testrunner/run.py --targets fabric-263 --filter simplebuilding:tiered_chest_game_test_trapped_titles_match_normal_tiers
```

Der erste Filterlauf ohne Namespace startete keine Tests (`NICHT gruen: 0/0`),
weil Fabric den vollständigen Testnamen erwartet. Der maßgebliche korrigierte
Lauf endete mit:

```text
Fabric - MC 26.3                  0      1      1    0    31.1s
alles gruen: 1/1 bestanden, 0 rot
```

Zusätzlich bestanden `git diff --check` und die JSON-Prüfung der vier aktiven
EN/DE-Sprachdateien. Nicht getestet wurden Client-Sichtprüfung sowie
NeoForge/Forge; Texturen wurden nicht geändert.

## Review (Claude, Übernahme nach claude-wave1)

- Übernommen: Menütitel der Fallentruhen = normale Truhe der Stufe (einzeln und „Große …“), GameTest,
  Entfernen des Lang-Schlüssels `container.simplebuilding.tiered_chest.stack_bonus`.
- Angepasst: In `TieredChestScreen` bleibt `extractLabels` (nur noch `super`), Klassen-Javadoc unverändert –
  so trifft der spätere Merge von claude-sc-merge (fügt am Methodenanfang `ModScreenStyle.tieredChestLabels` ein)
  keinen Konflikt. Wiki-Sätze zum Stapelfaktor in der Titelzeile ersetzt.
- Offen nach dem sc-merge-Merge: `ModScreenStyle.tieredChestLabels` (26.3, Kasten-Stil) zeichnet ein
  Stapel-Symbol – laut Auftrag ebenfalls entfernen.
