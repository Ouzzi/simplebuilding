# Kreativ-Tabs – Plan und Testbericht (2026-10-08)

## Ursache

- `CreativeTabLayout.emit` erzeugt aus `GAP` und aus dem Zeilenumbruch
  `CREATIVE_SPACER`-Stapel. Dadurch enthalten die Mod-Tabs künstliche Lücken,
  obwohl die Spacer nur als Layout-Hilfe gedacht waren.
- `SearchTabPlacement.placements()` wird auf Fabric, NeoForge und Forge
  unbedingten Vanilla-Tab-Ereignissen hinzugefügt. Es gibt noch keinen
  mod-eigenen Schalter, der Vanilla-Tabs unverändert lässt.
- Der Entwickler-Tab wird auf Fabric nach den Mod-Tabs registriert; seine
  Position ist nicht als eigene, getestete Schlussposition abgesichert.

## Änderung

1. Die Spacer-/GAP-Logik bleibt in `CreativeTabLayout` erhalten, ist aber über
   eine Konstante standardmäßig deaktiviert. Im Normalbetrieb werden keine
   Spacer ausgegeben und Kategorien laufen ohne künstliche Lücken weiter.
2. Der Entwickler-Tab bleibt registriert, wird aber loaderübergreifend
   ausdrücklich hinter dem letzten normalen Mod-Tab platziert.
3. `SimplebuildingConfig` erhält die serverseitig wirksame Option
   `addItemsToVanillaTabs` mit Standard `false`. Die Fabric-/NeoForge-/Forge-
   Einfügungen prüfen diese Option; die eigenen Mod-Tabs bleiben unabhängig.
4. EN/DE-Texte und `DataIntegrityTests` werden für Spacer-Freiheit,
   Entwickler-Tab-Ende und den ausgeschalteten Vanilla-Schalter angepasst.

## Betroffene Dateien

- `common/src/shared/java/com/simplebuilding/items/CreativeTabLayout.java`
- `common/src/shared/java/com/simplebuilding/items/ModItemGroupsContent.java`
- `common/src/shared/java/com/simplebuilding/items/SearchTabPlacement.java`
- `common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java`
- `src/main/java/com/simplebuilding/items/ModItemGroups.java`
- `neoforge/src/main/java/com/simplebuilding/neoforge/NeoForgeSearchTabPlacement.java`
- `forge/src/main/java/com/simplebuilding/forge/Forge262Events.java`
- `common/src/shared/java/com/simplebuilding/gametest/DataIntegrityTests.java`
- `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java`
- die EN/DE-Sprachdateien der 26.3-Hauptlinie

## Tests

Nach der Umsetzung:

- `python3.12 tools/testrunner/run.py --list`
- gezielte Fabric-26.3-GameTests mit `--filter`
- `python3.12 modules/simplelib/tools/check_data.py`

## Abweichungen

- Fabric 26.3 besitzt beim verwendeten `FabricCreativeModeTab.Builder` weder
  `withTabsAfter` noch `withTabsBefore`. Der Entwickler-Tab bleibt deshalb als
  letzter registrierter Fabric-Tab hinter den normalen Mod-Tabs; NeoForge und
  Forge setzen den letzten normalen Tab mit `withTabsBefore` vor den
  Entwickler-Tab.
- Der vorhandene Search-Tab-Integritätstest aktiviert die neue Option während
  des Tests und stellt den ursprünglichen Wert danach wieder her, damit er
  weiterhin den ausdrücklich aktivierten Einfügepfad prüft. Der neue Test für
  den deaktivierten Pfad prüft separat, dass keine Platzierungen geliefert
  werden.

## Testbericht

Erfolgreich:

- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:data_integrity_game_test_every_mod_item_is_in_exactly_one_creative_tab'`
  → `alles gruen: 1/1 bestanden, 0 rot`
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:data_integrity_game_test_creative_spacer_cannot_be_taken_or_kept'`
  → `alles gruen: 1/1 bestanden, 0 rot`
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:data_integrity_game_test_vanilla_tabs_stay_unchanged_when_mod_items_are_disabled'`
  → `alles gruen: 1/1 bestanden, 0 rot`
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:data_integrity_game_test_every_mod_item_has_its_place_in_the_search_tab'`
  → `alles gruen: 1/1 bestanden, 0 rot`
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:data_integrity_game_test_*tab*'`
  → `alles gruen: 13/13 bestanden, 0 rot`
- `python3.12 modules/simplelib/tools/check_data.py`
  → `simplelib: resources, bilingual names and wiki notes valid`
- `git diff --check` und JSON-Parsing der vier geänderten Sprachdateien
  → `lang JSON valid`

Nicht getestet: Client-Sichtprüfung sowie NeoForge- und Forge-GameTests. Ein
`--list`-Aufruf wurde begonnen, blieb ohne Ausgabe und wurde wegen des
laufenden Test-/Gate-Betriebs beendet. Ein vorheriger unvollständiger
Fabric-Lauf scheiterte vor Teststart an der zunächst nicht unterstützten
`withTabsAfter`-API; danach wurde die Implementierung korrigiert und die oben
aufgeführten Läufe wurden erfolgreich wiederholt.

## Review (Claude, Übernahme nach claude-wave1)

- Übernommen: Spacer aus (`CreativeTabLayout.SPACERS_ENABLED = false`, Logik bleibt), Schalter
  `addItemsToVanillaTabs` für SimpleBuilding mit Hooks auf Fabric, NeoForge, Forge 26.2 und – nachgezogen –
  Forge 26.3 (`mc26_3/forge/.../Forge263Events.java` war ungeschützt), Tests.
- Geändert: Standard **an** (bisheriges Verhalten bleibt ohne Besitzer-Entscheidung); Tooltip ohne
  „Serverseitig“ (Kreativ-Tabs baut der Client beim Betreten einer Welt). Der Test „Dev-Tab zuletzt“ liest die
  Registry-Reihenfolge statt `keySet()` (Hash-Menge ohne Ordnung).
- Verworfen: Umbau in `NeoForgeModRegistries`/`ForgeModRegistries` – `withTabsBefore(dev)` am letzten Inhalts-Tab
  hätte den Entwickler-Tab **vor** diesen Tab gestellt und die Kette zum Vorgänger gelöst; der Entwickler-Tab stand
  dort schon hinter dem letzten Inhalts-Tab.
- Offen: Schalter je Modul (simplefun, simplemoney, simpleriding, simpletweaks, simplemodels, simpledimensions,
  simplequalityoflife … sortieren eigene Items in Vanilla-Tabs ein) – noch nicht umgesetzt. Entwickler-Tab hinter
  den Tabs *anderer* Mods: nicht umgesetzt (Fabric ordnet nach Registrierreihenfolge der Mods).
