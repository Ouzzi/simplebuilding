# Plan A: Fässer nach Simple Storage (WH26-6, 06.11.2026)

Besitzer 06.11.: "Fässer, also Barrels, gehören ja in Simple Storage." Eimer (WH26-7) bleiben unberührt (unbeantwortet).

## Ist-Zustand
- Vier Fässer (Kupfer, Verstärkt, Netherit, Enderit) lagen in Simple Crucibles (`simplecrucibles:*_barrel`, davor `simplelib:`, Enderit `simplebuilding:`).
- Das Fass dockt an einen Tiegel an (Block-Eigenschaft `attached`/`facing`, Block-Entity liefert die Plätze, der Tiegel öffnet sein Menü). Tiegel kennt das Fass (Plätze, Ergebnisse zuerst ins Fass).
- Aufwertung des Fasses: doppelt vorhanden, Datei `in_world_upgrades/crucibles.json` (SimpleLib-Axt-/Hammerweg) und ein eigener Axt-Zweig in `CrucibleUpgrades` (10 Schläge, Material-Tags).

## Entscheidung (Modul-Unabhängigkeit, Regel 3: keine Klassenimporte über Modulgrenzen)
Storage und Crucibles bleiben je allein spielbar. Das Andocken läuft über einen Vertrag in SimpleLib (`com.simplelib.api.BarrelDock`, erlaubt nach Regel 6a):
- `Dockable` (Fass-Block, trägt `FACING`/`ATTACHED`), `Barrel` (Fass-Block-Entity: `attached`, Schläge, `onAttached/onDetached`, Stapel-Multiplikator), `Host` (Tiegel-Block-Entity: `dockSlots`, `dockStackMultiplier`).
- Block-Tag `#simplelib:barrel_hosts` (liefert Simple Crucibles mit seinen vier Tiegeln); `hostSide` sucht den Tiegel neben einem Fass.
- Storage besitzt: Fass-Block, -Block-Entity, -Menü, Stufen, Registrierung, Rezept, Tags, Lang, Texturen, Modelle (Generator `tools/gen_barrel_resources.py`), Aufwertung (`in_world_upgrades/barrels.json` + Handler `BarrelUpgrades`), Preis (Enderit-Fass), Wiki, Questionary, Testzentrum-Items, Gametests.
- Crucibles behält: Tiegel, `BarrelAttach` (Anbau-Schläge mit Axt/Hammer, Risse, Vorschau), Host-Tag, Tiegel-Menü mit Fass-Feldern (über `BarrelDock.Barrel` als `Container`). Ohne Storage gibt es kein Fass, nichts läuft.
- Der eigene Axt-Zweig in `CrucibleUpgrades` für Fässer entfällt (der SimpleLib-Datenweg macht dasselbe: 5 Schläge x Dauerfaktor 2 = 10; Material = Datei, 2 Stück); der `addUpgradeStrike`-Zähler entfällt.

## Umzug alter Welten (IdMoves, alles in `SimpleStorage.registerIdMoves`)
- BLOCK + ITEM: `simplecrucibles:`/`simplelib:` (Kupfer, Verstärkt, Netherit) und `simplebuilding:` (Enderit) -> `simplestorage:`.
- BLOCK_ENTITY_TYPE `barrel` aus `simplecrucibles:` und `simplelib:`; RECIPE + ADVANCEMENT `copper_barrel` (`recipes/misc/copper_barrel`).
- Die alten Moves lib->crucibles für Fässer sind in Crucibles entfernt (IdMoves verweigert zwei Ziele je Alt-Id).
- Loot-Tabellen haben keinen Weltzustand; Menütypen werden nicht gespeichert.

## Betroffene Dateien (Auswahl)
SimpleLib `api/BarrelDock.java`; Storage `barrel/*`, `registry/Storage{Blocks,Items,BlockEntities,Menus,Registry}`, `SimpleStorage`, Loader-Clients, `generated/resources`, Lang/Questionary, Wiki-Manual, Preise (Money), `check_data.py`; Crucibles `crucible/BarrelAttach`, `CrucibleBlockEntity`, `CrucibleMenu`, `CrucibleUpgrades`, `SimpleCruciblesApi`, `AxeBuildMixin`, Viewer/Info/Discover, Registry, Generator, `check_data.py`; Texturen-Werkzeuge (`tools/textures`), `.gitignore`, `modules.json` (optional + Combo `crucibles+storage`), SB-Lesestellen (Enderit-Fass-Id).

## Tests
- Storage (Gametests `barrels_registered`, `barrels_migrate_from_crucibles_lib_and_simplebuilding`, `barrel_recipe`, `barrel_placement_tiers`, `loose_barrel_*`, `barrel_upgrade_keeps_contents`, `barrel_dock_contract`).
- Crucibles: Anbau + Ergebnisse zuerst + spätes Anbauen laufen im neuen Combo-Ziel `module-simplecrucibles-with-simplestorage-fabric-263`; allein (Standalone) überspringen sie ohne Storage und prüfen nur den Tag `barrel_hosts`.
- Läufe: `module-simplestorage` und `module-simplecrucibles`, je Fabric + Standalone, plus das Combo.

## Nicht angefasst / offen
- Neo/Forge nicht kompiliert (Vorgabe: nur 26.3 Fabric); Test-Kataloge von Neo/Forge nicht nachgezogen.
- Eimer (WH26-7).
- Client-Sicht der Fässer (Modelle sind byte-gleich mit den alten, nur Namensraum).
