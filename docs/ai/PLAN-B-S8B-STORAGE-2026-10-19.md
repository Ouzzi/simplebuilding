# Plan b-s8b-storage (19.10.2026): Familienstufe 8, Teil 8b – Truhen und Shulkerkisten nach `simplestorage`

Branch `b-s8b-storage` (von `claude-wave1` 9cd18fe21), Sitzung B, Queue-Zeile 93. Grundlage: `PLAN-B-S8-STORAGE-2026-10-19.md`
(8a fertig: Modul-Gerüst, Kisten, Lager-Werkbank), `PLAN-MODULAUFTEILUNG-2026-10-11.md` (§1.2, §2.2, Stufe 8, F3),
Vorbild für 26.2 Stufe 4 Ovens (SB-Klassen bleiben, SB registriert auf 26.3 nichts, Zugriff per Id), `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## 1. Ist-Zustand (git grep, Stand 9cd18fe21)

- Kern (alles `common/src/shared`, 26.2 + 26.3): `ChestTier`, `TieredChestBlock`/`-BlockEntity`, `TieredShulkerBoxBlock`/`-BlockEntity`,
  `TrappedCopperChestBlockEntity`, Overlay- und 26.2-Zwilling `TieredTrappedChestBlock`/`TrappedCopperChestBlock` (26.2: nie registriert,
  `TRAPPED_TIERED_CHESTS=false`), `TieredChestMenu`/`-OpenData`/`-Slot`, `TieredChestScreen`, 4 Renderer, `TieredShulkerBoxItem`,
  `util/TieredChests`, `util/TieredShulkerBoxes`, `component/ContainerCounts`, Lade-Shims `platform/TieredChestMenus` (3 Loader,
  Extended-Menüs), Fahrzeuge `TieredChestBoat`/`-Item`/`TieredChestMinecart` (+ `OversizedStacks`, `VehicleTiers`, `VehicleRenderers`).
  ~5300 Zeilen, 125 Dateien nennen sie (davon ~45 Tests).
- Kopplung an SB-Inneres: `BackpackItem.maxStackSizeIn` (= SimpleLib `StackLimits.max`), `SledgehammerUpgrades.shouldSkipBlockUse`
  (= SimpleLib `SimpleLibApi.toolWants`, Muster `simpleovens/OvenUse`), `ModDataComponentTypes.CONTAINER_COUNTS`, `ModBlockEntities`,
  `ModScreenHandlers`, Hammer-Tabelle (`SledgehammerUpgrades.builtInTable`, Datagen `in_world_upgrades`), `LibServices`
  (Upgrade-Handler + Doppeltruhen-Teile), Trichter (`TieredChests.maxStackSize`; SimpleLib `HopperStackLimitsMixin` deckt jeden
  Container mit erhöhtem `getMaxStackSize(stack)` schon ab), Jade/`BlockInfo`, NeoForge-Capabilities, Tooltips (`client/ItemMixin`).
- Lager-Verzauberungen (Drawer, Deep Pockets, Funnel) gelten nur für `#simplebuilding:bundle_enchantable` (Bündel, Rucksäcke, Köcher).
- Seltene Funde (`BetterChests`, `RareShulkers`, `ShulkerShells` + Hüllen-Items, Listiger Shulker, Eskorte, End-Stadt-Mixins) setzen
  Stufen-Truhen/-Shulker in die Welt; Shulker-Deckel (`ShulkerLids`, `shulker_open`) gelten nur für Vanilla-Shulkerkisten.

## 2. Entscheidungen (selbst getroffen)

- **E1 Umfang 8b:** Stufen-Truhen (3), Stufen-Fallentruhen (3), Kupfer-Fallentruhen (8), Stufen-Shulkerkisten (3) mit BE, Menü,
  Bildschirm, Renderern, Komponente `container_counts`, Hammer-/Axt-Aufwertung (Kupfertruhe/Vanilla-Shulker → Verstärkt → Netherit →
  Enderit, Inhalt bleibt), Waschen im Kessel, Färben, Trichter/Komparator, Tooltips. **8b-2** (gleicher Branch, wenn 8b-1 grün):
  Kisten-Boote/-Loren (6). **Nicht in 8b:** Lager-Verzauberungen → 8c (sie hängen nur an Bündeln/Rucksäcken/Köchern; ohne diese
  hätte das Modul Verzauberungen ohne Ziel). Seltene Funde + Shulker-Hüllen + Shulker-Deckel bleiben vorerst SB (Weltfunde/Vanilla-
  Verhalten; SB nennt die Modul-Blöcke per Id) – als Folgepunkt notiert. Begründung: kleinster sauberer Schnitt, Modul allein voll
  spielbar (alle Rezepte nur aus Vanilla + SimpleLib-Material).
- **E2 26.2 (Muster Ovens):** Die SB-Klassen bleiben unverändert in `common/src/shared` (26.2 registriert und nutzt sie wie bisher).
  Neu `McVersion.STORAGE_IN_SB` (26.2 `true`, 26.3 `false`): auf 26.3 registriert SB keine dieser Blöcke/Items/BE/Menü-Typen/
  Komponente (Felder `null`), ihre Mixins (`ChestBlockMixin`, Hammer-Teil des `ShulkerBoxBlockMixin` bleibt, `client/ItemMixin`
  Truhen-Tooltip) wirken nur wo `STORAGE_IN_SB`. Alle verbleibenden SB-Verweise (Tabs, Suchtab, Testzentrale, Jade, Datagen,
  Hammer-Tabelle, Fahrzeuge, seltene Funde, Tests) gehen über `util/StorageRefs` (Id; 26.2 `simplebuilding:`, 26.3 `simplestorage:`)
  oder Vanilla-Schnittstellen (`ChestBlock`, `Container`, `getMaxStackSize(stack)`).
- **E3 Modul-Code:** Kopien unter `com.simplestorage.chest.*` (`ChestTier`, Blöcke, BE, Menü, Slot) und `com.simplestorage.client.*`,
  ohne SB-Import: Stapelgrenzen über SimpleLib `StackLimits`, Aufwertungsklick über `InWorldUpgrades.axeWants`/`SimpleLibApi.toolWants`,
  Inhalt bei Aufwertung über `InWorldUpgrades.registerHandler`, Doppeltruhen-Teile über `HammerActions.registerParts`. **Menüs ohne
  Loader-Shims:** neun einfache `MenuType`s (Stufe × einzeln/doppelt/Shulker, wie Vanillas `GENERIC_9x*`), daher keine
  Extended-Menüs je Loader. Kupfer-Fallentruhen altern/wachsen wie bisher (26.3-Overlay-Code). NeoForge: Item-Capabilities im Modul.
- **E4 Ids (SimpleLib `IdMoves`):** BLOCK + ITEM der 17 Blöcke; BLOCK_ENTITY_TYPE `tiered_chest`, `trapped_copper_chest`,
  `tiered_shulker_box`; DATA_COMPONENT_TYPE `container_counts`; RECIPE + Rezept-Advancement je Rezept. Menütypen nur neu (nie gespeichert).
  Tags `simplebuilding:*` der Truhen bleiben SB-Tags (Inhalt per Id), das Modul liefert eigene Block-/Item-Tags (Spitzhacke, `c:chests`…).
- **E5 Ressourcen:** Modelle/Blockstates/Items/Loot/Rezepte/Aufwertungsdateien aus `src/main/generated` + `mc26_3/generated` ins Modul
  **kopiert** (Namensraum umgeschrieben, 26.2 behält SBs Dateien), Datagen 26.3 lässt sie weg (`removed-on-26.3.txt` neu). Texturen
  kopiert (26.2 nutzt SBs). Lang EN/DE ins Modul; im 26.3-Overlay-Lang fallen die Schlüssel weg, `src/main` behält sie.
- **E6 Tests:** Modul-Tests (`StorageTests`) für Registrierung, Migration, Rezepte, Menü-Größen/Stapel, Doppeltruhe, Trichter x2/x4,
  Komparator, Aufwertung (Axt allein / Hammer mit SB, Inhalt + Farbe bleiben), Shulker-Inhalt am Item, Kupfer-Fallentruhe Redstone.
  SB-Testklassen `TieredChestTests`/`TieredShulkerBoxTests` laufen auf 26.3 per Id gegen das Modul, soweit sie nur Vanilla-API nutzen;
  Tests, die SB-Klassen casten, prüfen auf 26.3 das Modul (Fallback übersprungen mit Hinweis, wie Ovens `VehicleTests`).

## 3. Umsetzung (Commits)

1. Plan (dieses Dokument).
2. Modul 8b-1: Klassen, Registrierung (3 Loader), Renderer/Screens, Mixin (Kupfertruhe → Hammer/Axt), IdMoves, Ressourcen, Lang, Tests.
3. SB 8b-1: `STORAGE_IN_SB`, Registrierungen gaten, `StorageRefs` erweitern, Aufrufer umstellen, `LibServices`/Hammer-Tabelle,
   Mixin-Plugin, Tabs/Suchtab/Testzentrale/Jade/NeoForge-Capabilities, Datagen 26.3, Overlay-Lang, Tests, Fremdmodule (Money, Interfaces).
4. 8b-2 Kisten-Boote/-Loren (gleiches Muster), wenn 8b-1 grün.
5. Erzeugen auf sb-test: Datagen 26.3, `module_guides.py`, `discover.py`, `wiki/generate.py --all`, Money-Links.

## 4. Risiken

- Doppelte Wirkung SB-/Modul-Mixin → SB-Teile hinter `STORAGE_IN_SB` (Mixin-Plugin wie Ovens).
- Gefüllte Truhen/Shulker in alten Welten: BE-Id + Block-Id + Komponente müssen alle ziehen (Migrationswelt-Test).
- Übergroße Stapel (>99) im BE-Codec: Modul übernimmt `EXTRA_COUNTS`-Schlüssel unverändert.
- Atlas: Truhen-/Shulker-Texturen brauchen Atlas-Quellen im Modul (`checkAtlases`).

## 5. Prüfung (sb-test)

Compile 26.3 fabric/neoforge/forge + 26.2 `:compileJava`; `module-simplestorage-{fabric,neoforge,standalone-fabric}-263`;
fabric-263 und neoforge-263 voll (nacheinander); forge-263 gezielt (`*chest*`, `*shulker*`, `*data_integrity*`); `./gradlew check`.
Nicht geprüft (Auftrag): Client-Läufe (Renderer, Bildschirm).

## 6. Stand und Abweichungen

(wird am Ende ausgefüllt)
