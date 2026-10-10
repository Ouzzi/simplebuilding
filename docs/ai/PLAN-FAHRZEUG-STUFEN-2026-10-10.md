# Plan: Stufen für Kistenboote, Kisten-, Ofen- und Trichterloren (Queue N19 + N23, 2026-10-10)

Branch `claude-q-vehicles` (von `claude-wave1` 9cb965759). Nur 26.3 (`McVersion.TIERED_VEHICLES`), alle drei Loader.

## Ist-Zustand
- Truhen-Stufen: `ChestTier` (36/45/54 Plätze, Stapel x1/x2/x4), Menü `TieredChestMenu` (nimmt jeden `Container`),
  Öffnen je Loader `platform.TieredChestMenus`. Übergroße Stapel speichert `TieredChestBlockEntity` als Zusatzliste.
- Öfen: Tempo je Stufe aus `ServerTuning.furnaceExtraTicks` (2x/4x/8x). Mod-Trichter: `ServerTuning.hopperSpeed`
  (2x/4x/8x), Filter `ItemFilter`/`HopperFilterMode` (Filter-Prinzip), Menü `NetheriteHopperScreenHandler`.
- Vanilla-Loren: `MinecartChest` 27, `MinecartFurnace` (3600 Ticks je Kohle, max 32000, halbe Höchstgeschwindigkeit),
  `MinecartHopper` (5 Plätze, saugt 1 Item je Tick), `ChestBoat` 27 (je Holzart ein Entity-Typ).

## Entscheidungen (selbst getroffen, Volle Autonomie)
1. **Stufen = `ChestTier`** (Verstärkt/Netherit/Enderit), Stufen-Nummer 1/2/3 für `ServerTuning`.
2. **Kistenlore / Kistenboot**: Plätze und Stapelfaktor wie die Truhe der Stufe (36/45/54, x1/x2/x4), dasselbe
   Truhen-Menü. Übergroße Stapel wie bei der Truhe als Zusatzliste `simplebuilding:counts` gespeichert.
3. **Kistenboot: ein Entity-Typ je Stufe**, die Holzart steht als Komponente `simplebuilding:boat_wood` am Item und
   synchronisiert an der Entity (11 Holzarten inkl. Bambus-Floß). Sonst wären es 33 Items/Entity-Typen.
4. **Ofenlore**: Brenndauer je Brennstoff 3600 x Ofen-Tempo der Stufe (2/4/8, also wie der Ofen derselben Stufe),
   Höchstfüllung 32000 x Tempo; Höchstgeschwindigkeit 0,625 / 0,75 / 1,0 einer normalen Lore (Vanilla-Ofenlore 0,5).
5. **Trichterlore**: saugt je Tick bis zu Trichter-Tempo der Stufe Items (2/4/8; Vanilla-Trichterlore 1) – dasselbe
   Verhältnis wie Mod-Trichter zu Vanilla-Trichter. Filter-Knopf und Filter-Prinzip wie der Block (echtes Item im
   Slot bleibt, erst das zweite wird bewegt), dasselbe Menü/Bildschirm.
6. **Rezepte**: Werkbank formlos Lore + Stufen-Truhe/-Ofen/-Trichter bzw. Boot + Stufen-Truhe (Holzart im Ergebnis);
   Schmiedetisch Verstärkt -> Netherit (Netherit-Vorlage + Netheritbarren) -> Enderit (Enderit-Vorlage +
   Enderitbarren) wie Rucksack/Bündel (Komponenten inkl. Holzart bleiben).
7. **Darstellung**: Vanilla-Lore/-Boot-Modelle. Ofen/Trichter zeigen den Stufen-Block als Inhalt, die Kistenlore die
   Stufen-Truhe (Truhen-Atlas der Stufe), das Kistenboot das Vanilla-Boot der Holzart ohne Kiste plus Stufen-Truhe.
   Item-Sprites per Generator `tools/textures/vehicles_2026_10_10.py` (Lore + eigener Stufen-Aufsatz; Boot: Vanilla-
   Kistenboot der Holzart + Stufen-Kisten-Ebene).
8. Drops: die Stufen-Item (Boot mit Holzart, Name bleibt) plus Inhalt.

## Dateien
- Neu: `entity/vehicle/*` (4 Entities, `VehicleTiers`, `OversizedStacks`), `items/custom/Tiered*Item`,
  `client/render/VehicleRenderers` (26.3 Overlay + 26.2-Stub), `util/FilterHopper`, Tests `VehicleTests` + Fabric-Wrapper.
- Geändert: `ModEntities`, `ModItems`, `ModDataComponentTypes`, `ModItemGroupsContent`, `McVersion` (beide Linien),
  Trichter-Menü (`ModHopperScreenHandler`, `NetheriteHopperScreenHandler`, `ModMessageHandlers`, `ModScreenStyle`),
  `HopperMenus` (3 Loader), Client-Registrierung (3 Loader), `TieredChests.oversizedStorage`, Datagen (Rezepte,
  Modelle), Lang EN/DE, Wiki, Testzentrale (Station „vehicles“).

## Prüfung
Compile 26.3 Fabric/NeoForge/Forge + 26.2 Fabric/NeoForge; Tests `simplebuilding:vehicle*` auf fabric/neoforge/forge-263
(Inventargröße, Stapel, Speichern, Geschwindigkeit, Brenndauer, Filter, Saug-Tempo, Drop, Rezepte); Datenprüfungen;
Wiki-Check; Client-Screenshot nach `/root/previews/vehicles/`.
