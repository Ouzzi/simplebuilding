# Plan N51 – Eisen-Tiegel und Tiegel-Upgrades in der Welt bauen (2026-10-13, Branch `claude-crucible-build`)

Besitzer (Queue Nachtrag 51): Eisen-Tiegel aus 16 Eisenbarren Schlag für Schlag, jeder Schritt sichtbar; jedes weitere
Upgrade 12 Schläge, alle 3 Schläge 1 Material (4 rissige Diamanten / 4 Netherit-Nuggets / 4 Enderit-Nuggets).

## Ist-Zustand
- Bau (SimpleLib `CrucibleBlankBlock`, SB-Hammer ruft `SimpleLibApi.buildStrike`): Eisenblock + 4 schwere Druckplatten
  (Tag `simplelib:crucible_walls`) + 2 Griffe (Tag `simplelib:crucible_handles`: Eisenbarren, SB: Eisenstange),
  Rohling `stage` 1..5, Loot = Eisenblock + verbaute Teile.
- Upgrades: SB-Hammer (`SledgehammerUpgrades`, Daten `in_world_upgrades`): Tiegel 2 Material am Ende, 10 Schläge
  (5 × Faktor 2), wachsende Vorschau + Risse. SimpleLib-Axt-Weg (ohne SB): 10 Schläge, 2 Diamanten / 1 Netheritbarren.

## Entscheidungen (Autonomie, im Bericht genannt)
- „Basis-Tiegel“ = wie bisher der Eisenblock: der 1. Schlag macht ihn zum Boden (Fuß + Boden des Kessels) samt erster
  halber Platte. 16 Schläge, je 1 Barren: Platten N/O/S/W je halb → ganz (1–8), Griffe O/W je halb → ganz (9–12),
  Rand (Hals) N/O/S/W (13–16, der 16. setzt den Eisen-Tiegel). Rohling `stage` 1..15 (alte Welten: 1..5 bleiben gültig,
  bedeuten jetzt den neuen Stand).
- Material: Platten/Rand Tag `simplelib:crucible_walls` (jetzt `minecraft:iron_ingot`), Griffe `simplelib:crucible_handles`
  (Eisenbarren; SB ergänzt die Eisenstange wie bisher). Abbau gibt Eisenblock + je Stufe einen Eisenbarren zurück.
- Der Rohling zeigt seine Stufe selbst (Modell je Stufe), deshalb dort keine zusätzliche Block-Vorschau (sie würde die
  Teilmodelle überdecken); Risse/Partikel/Klang wie bisher.
- Upgrades nur der Tiegel (Fässer/Kessel unverändert): `CrucibleBlock.upgrade` 0..3 = schon verbaute Material-Einheiten;
  Modell je Einheit ein Band aus dem Zielmaterial auf einer Seite (N, O, S, W; Texturen Rissiger-Diamant-Block,
  Netheritblock, Enderitblock). Die Tiegel-Block-Entity merkt sich das verbaute Material und gibt es beim Abbau zurück;
  beim Umbau wird es nicht fallen gelassen. Wachsende Vorschau des Ziels bleibt.
- SB-Hammer: Tiegel-Stufen fest 12 Schläge, Einheit bei Schlag 3/6/9/12, `material_count` 4 (Daten), zum Ansetzen genügt
  1 Material in der Nebenhand. Fortsetzung nach Abbruch über `SledgehammerProgress` (mindestens Einheiten × 3).
- SimpleLib-Axt-Weg (nur ohne SB): ebenfalls 12 Schläge / 4 Einheiten; Material-Tags `simplelib:crucible_upgrade_reinforced`
  (rissiger Diamant, Diamant) und `simplelib:crucible_upgrade_netherite` (Netherit-Nugget, Netheritschrott), weil die
  SimpleLib-Materialien allein keinen Bezugsweg haben (MaterialRequestTests: nur auf Anfrage).

## Dateien
SimpleLib: `crucible/CrucibleBlankBlock`, `CrucibleBlock`, `CrucibleBlockEntity`, `CrucibleUpgrades`, `AxeWays`,
`registry/LibTags`, `api/SimpleLibApi`, `tools/gen_resources.py` (+ generierte Modelle/Blockstates/Loot/Tags),
`test/LibTests` + Modul-Gametest-Wrapper, Wiki/Fragentisch (`wiki/manual.json`, `questionary/*.json`).
SB: `util/SledgehammerUpgrades`, `crucible/CrucibleCompat` (26.3 + 26.2-Zwilling), `compat/CauldronWorldCatalog` + Lang,
`gametest/CrucibleTests` + Katalog + Fabric-Wrapper, Daten `in_world_upgrades` (Datagen), Wiki.

## Prüfung
Compile 3 Loader; SB-Filter crucible*, inworld*, sledgehammer*, data_integrity*; simplelib-Modultests; Datagen-Diff;
`gen_resources.py --check`, `wiki/generate.py --all --check`, `tools/discover/discover.py --check`, Fragentisch-Check;
Client-Screenshots aller Stufen nach `~/previews/crucible-build/`.

## Korrektur Besitzer (13.10., Branch `claude-crucible-build2`)
- Grundlage ist ein **leerer Vanilla-Kessel**, nicht der Eisenblock. Ein Kessel mit Wasser/Lava/Pulverschnee wird nicht
  angenommen (erst leeren, nichts geht verloren). Rohling-Eigenschaft `cauldron` (true bei neuen Bauten); alte Rohlinge
  (ohne Eigenschaft = false) bleiben weiter baubar und geben ihren Eisenblock zurück. Abbau-Rückgabe in
  `CrucibleBlankBlock#getDrops` (Kessel bzw. Eisenblock + je Schlag ein Barren), Loot-Tabelle leer.
