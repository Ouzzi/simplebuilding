# Plan b-dimrest31 (B, 11.11.) – Rest aus b-dimid27 (RELSB31 #4 / L8)

Basis hub/b-bundle182 (9b5764995). b-dimid27 (25136775f) ist enthalten (`git merge-base --is-ancestor` geprüft) → kein Merge nötig.

## Ist-Stand
- Mod-ID `simpledimension` → `simpledimensions` (b-dimid27, `LegacyIds`): Registry-IDs, Welt-Schlüssel, Ordner, eigene Configs migriert.
- Simple Tweaks Claims: `config/simpletweaks-claims.json` (`ClaimConfig.dimensions`, Allowlist) und das Hauptbuch
  `simpletweaks-claims.json` (`ClaimStore`, Schlüssel `dimension`+`chunk`, auch Abschnitte der Länder) speichern Dimensions-IDs als Text.
  Ein alter Eintrag `simpledimension:<x>` passt nach dem Umbau zu keiner Dimension mehr: Allowlist sperrt die Dimension,
  Grundstücke dort sind verwaist (Chunk wirkt frei).
- `legacy_dimensions`-Gametest (Fix 25136775f) noch nie gelaufen.

## Änderung
1. `ClaimConfig.migrateDimension(String)` (+ `LEGACY_NS`): `simpledimension:x` → `simpledimensions:x`, sonst unverändert.
   Kein Import aus simpledimensions (Modul-Unabhängigkeit): nur Text.
2. `ClaimConfig.load`: Allowlist-Einträge immer (unabhängig von `configVersion`) umschreiben; geändert → Datei einmal
   atomar neu schreiben (wie die Versions-Migration), Log-Zeile.
3. `ClaimStore.Key`: kompakter Konstruktor migriert `dimension` → Hauptbuch, Abschnitte, alte NBT-Dateien, Claim-Codes.
   Beim Laden des Hauptbuchs: Grundstück/Abschnitt unter alter ID, dessen neue ID schon belegt ist (Spieler hat nach dem
   ID-Wechsel neu beansprucht) → alter Eintrag fällt weg (Log), statt dass das ganze Hauptbuch als „doppelt“ abgewiesen wird.
   Gespeichert wird mit der nächsten Änderung (vorhandenes Verhalten).
4. Gametest `simpletweaks:claims_legacy_dimension` (CreditTests.legacyDimension + TESTS + Fabric-Adapter).

## Test
Ein sbt: Compile Kern + simpletweaks + simpledimensions; `module-simpletweaks-fabric-263,module-simpletweaks-standalone-fabric-263`;
`module-simpledimensions-fabric-263,module-simpledimensions-standalone-fabric-263` (bestätigt legacy_dimensions-Fix).

## Risiken
Duplikat-Regel nur für migrierte Einträge gelockert; echte Duplikate bleiben Fehler.
