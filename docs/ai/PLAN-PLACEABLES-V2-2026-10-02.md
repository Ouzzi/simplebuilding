# Plan: Placeables v2 (Queue Nachtrag 4, 2026-10-03)

Branch `claude-placeables2` (Basis 434a3d36). Nur committen, nicht pushen.

## Ist-Zustand
- `placed_small_parts` (`PlacedSmallPartsBlock`, BE `PlacedSmallPartsBlockEntity`, Renderer `PlacedSmallPartsRenderer`,
  Logik `util/PlacedSmallParts`): bis 4 Teile, Tag `simplebuilding:placeable_small` + Eier (`PlacedEggs`).
  Haken: `ItemUseOnMixin` (Basis-`Item#useOn`) -> `PlacedSmallParts.tryPlace`. Altbloecke `placed_egg` und liegende
  Einzelteile (`placed_smithing_template`, FLOOR) werden beim Dazulegen zum Haeufchen.
- Liegende Teile = Platte der Item-Textur, Eier = 3D-Modell ueber Ersatz-Stapel (`minecraft:item_model`
  -> `simplebuilding:placed_egg_<farbe>`), Trefferform aus derselben Lage (`place`).
- Kein Licht, keine Partikel, keine Interaktion ausser Shulkerschalen-Aufwertung.
- Mischbarkeit eigener Teile: alle Tag-Teile + Eier mischen schon frei; Luecken = Kerzen/Seegurken (Vanilla-Bloecke)
  und eigene Kleinmaterialien, die (noch) nicht im Tag stehen.
- CosmeticIntensity (Framework) ist ein Register, in das *Module* ihre Stufe melden; die Hauptmod meldet nichts und das
  Framework haengt nur an der 26.3-Linie -> passt nicht. Stattdessen eigene Client-Option (wie die Werkzeug-Animationen).

## Entscheidungen (selbst getroffen)
1. **Neue Kleinteile** (Platten, kein eigenes `useOn`, vanilla-nah, keine Bloecke):
   Vanilla: Knochen, Feder, Pfeil, Spektralpfeil, Lohenrute, Boe-Rute, Glowstonestaub, Leuchttintenbeutel,
   Prismarinkristalle, Netherstern, Hasenpfote, Schildkroetenhornschild, Guerteltier-Hornschild, Plattenbruchstueck,
   Ghast-Traene. Mod: Nihilithsplitter, Astralitstaub, Enderquarz, Rohenderit, Enderitschrott, Rissiger Diamant,
   Weisheitskugel.
   Nicht: Toepferscherben (23 Varianten, gehoeren auf Krueger; "Scherben" = Amethyst-/Prismarin-/Echoscherbe sind schon
   drin), Enderperle/Schneeball (Wurf-Konflikt), Redstone/Faden/Leuchtbeeren (BlockItems mit eigenem Setzen),
   getippte/gefertigte Pfeile (Komponenten-Modelle).
2. **Kerzen + Seegurken**: eigene Teil-Art (stehend wie Eier), Modell = Vanilla-Blockmodell
   (`minecraft:block/<farbe>_candle_one_candle[_lit]`, `minecraft:block/[dead_]sea_pickle`) ueber Item-Definitionen
   `simplebuilding:placed_<kerze>[_lit]`, `placed_sea_pickle`, `placed_dead_sea_pickle`.
   - Nur gemischt wird es der Mischblock: Erstes Ablegen einer Kerze/Seegurke bleibt Vanilla. Schleichen + Rechtsklick
     mit einem *anderen* Teil auf einen Vanilla-Kerzen-/Seegurkenblock (oder den Boden darunter) macht daraus ein
     Haeufchen (n Kerzen/Gurken + Teil; LIT und Wasser werden uebernommen). Gleichfarbige Kerze / Seegurke auf den
     eigenen Block = Vanilla (nichts Neues). Kerze/Seegurke auf ein Haeufchen = dazulegen.
   - Blockzustand bekommt `lit`, `candles` (0..4), `pickles` (0..4); Licht rein aus dem Zustand:
     nass: Seegurken > 0 -> 3 + 3*Gurken (Vanilla), trocken: lit -> 3*Kerzen (Vanilla). Zaehler gleicht die BE nach
     jeder Teil-Aenderung ab (Server).
   - Anzuenden: Feuerzeug/Feuerkugel (ohne Schleichen) auf ein Haeufchen mit Kerzen, trocken, aus -> an (Haltbarkeit/
     Verbrauch, Klang). Brennender Pfeil zuendet an. Leere Hand loescht (Vanilla-Klang, Rauch). Wasserfuellen loescht.
   - Drops: jedes Teil als es selbst (Kerze -> Kerze, Gurke -> Gurke). Optionen `placeVanillaItems`/`placeDisabledItems`
     gelten auch fuer Kerzen/Seegurken.
   - Save-Kompatibilitaet: alte Haeufchen ohne die neuen Eigenschaften laden mit den Standardwerten (0/aus) = richtig.
   - Haken fuer `BlockItem#useOn` (Kerzen/Seegurke ueberschreiben `useOn`): neuer Inject in `BlockItemMixin`.
3. **Partikel** (`animateTick`, clientseitig): brennende Kerzen = Vanilla-Flamme + Rauch an jedem Docht (immer, wie
   Vanilla). Dezente Glanz-Partikel (abschaltbar ueber neue Client-Option `tools.placedPartParticles`, Standard an):
   Glowstonestaub/Leuchttintenbeutel/Prismarinkristalle -> `glow`, Netherstern/Astralitstaub -> `end_rod`,
   Lohenrute -> `small_flame`, Echoscherbe -> `sculk_charge_pop`, Weisheitskugel -> `enchant`,
   Enderit-Teile/Enderquarz -> `portal`. Seltener Zufall je Tick (dezent).
4. Testzentrale: neue Teile bekommen automatisch ihre Spalte; dazu eine Reihe Misch-Beispiele (Kerze + 2 Kiesel + Ei,
   brennende Kerzen-Mischung, Seegurken + Kiesel unter Wasser, Kerze + Seegurke trocken).

## Dateien
- `util/PlacedSmallParts.java` (Arten, Konvertierung, Licht, Lage/Trefferform), `blocks/custom/PlacedSmallPartsBlock.java`
  (Zustand, Licht, Anzuenden/Loeschen, Wasser, animateTick), `PlacedSmallPartsBlockEntity.java` (Zustand abgleichen),
  `client/render/PlacedSmallPartsRenderer.java`, neu `util/PlacedPartParticles.java`, `mixin/BlockItemMixin.java`,
  `blocks/ModBlocks.java` (lightLevel), `datagen/ModItemTagProvider.java` + `mc26_3/generated` (Tag),
  Item-Definitionen `mc26_3/overlay/resources/assets/simplebuilding/items/placed_*candle*.json`, `placed_*sea_pickle.json`
  (Generator `tools/textures/placeables_v2_2026_10_03.py`, auch Vorschau), Config + `ConfigOptions` + `ConfigOptionTests`,
  Lang EN/DE (beide Orte), `wiki/manual.json`, Testzentrale (`FeatureStations.placeables`), Tests in `PlacedTemplateTests`.

## Risiken
- Zustandswechsel des Blocks durch die BE (setBlock gleicher Block behaelt BE) - getestet.
- Kerzen-Modell als Item-Modell (ItemDisplayContext.NONE) -> gleiche Lage wie Ei (Modellmitte im Ursprung).
- Testzentrale-Breite waechst um ~22 Spalten; `TestCentreTests` pruefen.
- 26.2: SMALL_PLACEABLES aus -> alles inaktiv, Code muss aber kompilieren (nur gemeinsame API).

## Verifikation
GameTests: neue Teile ablegbar; Kerze+Kiesel+Ei mischen (Konvertierung Vanilla-Kerze, LIT/Wasser uebernommen);
reine Kerze/Seegurke bleibt Vanilla; Licht (Kerzen an/aus, Gurken nass/trocken); Anzuenden mit Feuerzeug, Loeschen mit
leerer Hand, Loeschen durch Wasser; Drops; altes Haeufchen ohne neue Eigenschaften (Standardzustand) bleibt gueltig;
Item-Definitionen zeigen auf Vanilla-Modelle. Gates: fabric-263, neoforge-263, Compile 26.2 + Forge 26.3, Wiki-Check.
Nicht testbar ohne Client: Aussehen/Partikel (nur Code-Review + Vorschau).

## Umsetzung - Abweichungen
- Testzentrale: die nassen Seegurken stehen nicht in einer Bodenmulde (Canvas erlaubt nur y >= -1), sondern zwischen
  zwei Haeufchen mit einer Glasscheibe davor (fliessendes Wasser fuellt keine Haeufchen).
- Partikel: Enderit-Teile bekommen keine Partikel (dezent bleiben); Glowstonestaub nutzt `wax_on` statt `glow`.
- Save-Test liest ueber `NbtUtils.readBlockState` (wie die Welt), nicht ueber `BlockState.CODEC`.
- Ergebnis: fabric-263 und neoforge-263 je 891/891 gruen, Compile 26.2 und Forge 26.3 gruen, Wiki-Check gruen.
