# Plan B: Bündel-Ansicht ohne 12er-Grenze (b-bundles, 19.10.2026)

Umsetzung von `docs/ai/KONZEPT-BUNDLES-BEYOND-2026-10-19.md`, Stufe 1, mit den Empfehlungen: Ansicht in
**Simple Interfaces**, keine Farb-/Größenvarianten (später), keine Tastenbedienung, kein Essen aus Rucksäcken,
Scrollrichtung wie Vanilla (Mausrad, Vanilla-Logik unverändert). Basis: Branch b-tooltips (Container-Tooltips).
Vorbild-Mod Bundles Beyond (CC-BY-NC-SA): nur die Idee, kein Code, eigenes Verfahren (Seiten statt Liste).

## Ist-Zustand (geprüft, 26.3 Fabric/NeoForge/Forge-Quellen identisch)
- `ClientBundleTooltip`: `slotCount() = min(12, size)`, Überlauf `size > 12` -> 11 Items + Feld "+n",
  `getShownItems` = immer die ersten k Einträge. `BundleContents.getNumberOfItemsToShow()` (max. 12/11) begrenzt
  auch die Mausrad-Auswahl: Vanilla `BundleMouseActions` und SBs `ReinforcedBundleTooltipSubmenuHandler` (verstärktes
  Bündel, eigene Klasse, nicht im Tag `minecraft:bundles`) lesen beide diesen Wert. Vanilla 26.3 hat die Grenze noch
  -> Rückport-Regel greift nicht.
- Server: Vanilla `setSelectedBundleItemIndex` und SBs `handleReinforcedBundleSelection` enden beide in
  `BundleItem.toggleSelectedItem` -> `Mutable.indexIsOutsideAllowedBounds` (Index < 0 oder >= Anzahl Einträge -> keine
  Auswahl). Index 13+ ist also schon gültig, zu große/negative Indizes werden verworfen; der Farbkasten klemmt seinen
  Index im Record `PaintBoxContents`. **Kein Bounds-Fix in SB nötig**, nur ein Test, der das festhält.
- SB mischt selbst in `ClientBundleTooltip` (Tönung `extractSlot`-blitSprite ordinal 1, Füllbalken) – andere
  Stellen als hier, verstärkte Bündel nutzen dieselbe Klasse und bekommen die Ansicht dadurch mit.
- Sandwiches `bundleEating` isst den obersten Eintrag (`top`), unabhängig von der Auswahl -> unberührt.

## Umsetzung
1. `tooltip/BundleView` (shared, ohne Client-Klassen): Record `(start, shown, overflow, cells)` und
   `of(size, selected, config)`: null = Vanilla (Schalter aus oder <= 12 Einträge). Bis `maxRows*4` Einträge alle
   zeigen, sonst Seiten zu `maxRows*4-1` Einträgen + Feld "+n" (alles außerhalb der Seite); die Seite folgt der
   Auswahl. `selectable(size, config)` (für die Mausrad-Grenze), `hidden(contents, view)`.
2. Client-Mixins (nur `client`-Liste): `BundleTooltipViewMixin` auf `ClientBundleTooltip` (slotCount, Überlauf-
   Konstante 12, getShownItems, getAmountOfHiddenItems, Auswahl-Hervorhebung um `start` versetzt) und
   `BundleSelectionMixin` auf `BundleContents.getNumberOfItemsToShow` (Auswahl über alle Einträge, gilt für Vanilla-
   und SB-Bündel, da beide diesen Wert lesen). Kein neuer Paket-Typ, keine SB-Importe.
3. Config lokal (`ContainersConfig`): `bundleShowAll` (an), `bundleMaxRows` (6, Bereich 3..16). Cloth-Einträge
   (Forge über native-config), Lang EN/DE, Wiki-Features `bundle_view`, `config_bundleShowAll`, `config_bundleMaxRows`.
4. Keine neuen Farben (Vanilla-Sprites und Vanilla-"+n"); Kontrast-Tool trotzdem laufen lassen.

## Nicht jetzt
Farb-/Größenvarianten (Stufe 3), Tasten (Pfeile/Leertaste), Scrollachsen-Schalter, Rucksack-Essen, Shift-Füllen.

## Verifikation
- Gametests `BundleTests` (simpleinterfaces): Layout für n = 0/12/13/24/25/40/100 und Auswahl (Seitenwechsel,
  Hidden-Zahl), maxRows-Klemme, Config aus -> Vanilla (null, Vanilla-Grenze), Server: Vanilla-Bündel mit 20 Einträgen,
  `setSelectedBundleItemIndex` 15 gilt und `removeOne` nimmt Eintrag 15, 20/-2 verworfen.
- SB-Gametest `ReinforcedBundleTests.selectionPayloadReachesPastTheTwelveShownEntries` (Payload-Handler, 20 Einträge:
  15 gilt, 20 und -3 verworfen) + Fabric-Adapter + Katalog.
- Läufe: module-simpleinterfaces-fabric-263/-neoforge-263 + standalone, fabric-263 Filter `simplebuilding:*bundle*`,
  Compile Fabric/NeoForge/Forge 26.3 inkl. `:modules:simpleinterfaces:forge:compileJava` und 26.2 `:compileJava`
  (SB-Testdatei in common/src/shared), `tools/check_ui_contrast.py`, `modules/simpleinterfaces/tools/check_data.py`,
  Abschluss `./gradlew check -q`.
- Offen (kein Client-Lauf erlaubt): Client-Sicht der Ansicht und des Mausrads.
