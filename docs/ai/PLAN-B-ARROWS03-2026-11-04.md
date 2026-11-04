# Plan b-arrows03 (Sitzung B, 04.11.2026) – Simple Arrows Stufe 2: Befiederungstisch und alle SB-Pfeile ins Modul

Auftrag: /root/sb-tools-b/b02-auftrag.md, Besitzer DW04C-4 (/root/b-antworten-20261104c.md): „simple arrows existiert 2 mal der
creative tab, die simple building arrows sollen in simple arrows ausgelagert werden auch der fletching table.“ = Queue
ARROWS-STUFE2 (zurückgestellt in docs/ai/PLAN-B-ARROWS02-2026-11-03.md §2, Konzept docs/ai/KONZEPT-B-ARROWS01-2026-11-01.md
Stufe 2). Basis 3a7907e06 (Bündel 106). NUR 26.3 Fabric.

## 1. Ist-Stand
- SB-Kern `common/src/shared/java/com/simplebuilding/fletching/`: `ArrowParts` (9 Spitzen, 5 Schäfte, 2 Befiederungen als Enum
  mit Item-Suppliern auf SB-Felder `DIAMOND_PEBBLE`, `NETHERITE_NUGGET`, `ENDERITE_NUGGET`, `DIAMOND_ROD`), `CraftedArrow`,
  `CraftedArrowItem`, `FletchingMenu` (3 Teile + Trank-Slot 40), `FletchingRecipe`/`FletchingRecipes` (Typ + 3 Buch-Kategorien),
  `ArrowRecovery` (gilt für jeden Pfeil, auch 26.2), Renderer; `client/gui/FletchingScreen` (+ `ThreeSlotRecipeBookComponent`, das
  auch der Schmiedetisch nutzt), Stil `ModScreenStyle.fletching*` (26.3, Gravur VIS24-9), `ModScreenLayout.FLETCHING_BOOK_*`;
  Mixins `FletchingTableMixin` (BlockBehaviour.useWithoutItem), `AbstractArrowAccessor` (setPierceLevel/doPostHurtEffects/onHitEntity).
- Registrierung nur mit `McVersion.FLETCHING` (26.2 `false` – 26.2 hatte den Tisch nie): Fabric `ModRecipes`/`ModScreenHandlers`/
  `SimplebuildingClient`, Item `ModItems.CRAFTED_ARROW`, Entity `ModEntities.CRAFTED_ARROW`, Komponente
  `ModDataComponentTypes.ARROW_PARTS` (immer), NeoForge/Forge-Zwillinge. Datagen `ModRecipeProvider` → 16 Rezepte
  `mc26_3/generated/data/simplebuilding/recipe/fletching/**`, Fortschritt `recipes/combat/fletching_table_parts` (overlay),
  Item-Modell `items/crafted_arrow.json` + 15 Teil-Modelle/-Texturen `item/arrow/*`, 3 Leer-Slot-Sprites `container/slot/arrow_*`.
- SB-Tab `simplebuilding:arrows` („SimpleArrows“, 90 Pfeile) **neben** dem Modul-Tab `simplearrows:arrows` („Simple Arrows“, Licht-
  und Silberpfeil) = der doppelte Tab des Besitzers.
- Weitere Nutzer: JEI (`LibServices.fletchingEntries`, `RecipelessJeiInfo`), Testzentrale (Abschnitt `arrows`, Bogenstand-Truhen),
  Discover-Id, Questionary, Wiki-Manual, Tests `FletchingTests` (8), `CombatTests` (Rückgabe), `TrainingDummyTests` (Puppenkopf zählt),
  `ModScreenStyleTests`, `DataIntegrityTests`, Client `ModUiStyleClientTest`, Probe `modules/simplearrows/tools/fletching_probe.py`.
- Auffällig: `simplebuilding:crafted_arrow` steht in keinem `#minecraft:arrows` (Bogen/Armbrust/Köcher nehmen ihn nicht, nur Werfer).

## 2. Entscheidungen (selbst getroffen, begründet)
1. **Ganzer Tisch ins Modul** (`com.simplearrows.fletching`, Client `com.simplearrows.client`), Muster ench02: SB löscht seine
   Klassen (26.2 hatte sie nie → kein 26.2-Zwilling nötig; `McVersion.FLETCHING` bleibt als „diese Linie hat den Tisch – 26.3 in
   Simple Arrows“). Kein SB-Feld zeigt mehr auf den Pfeil: SB nennt ihn nur per Id (`util/ArrowRefs`, wie `EnchantingRefs`), braucht
   also kein `initializeFirst`. `ThreeSlotRecipeBookComponent` bleibt in SB (Schmiedetisch), das Modul bekommt eine Kopie (dup_pairs).
2. **ArrowRecovery bleibt in SB** (gilt für alle Pfeile, `server.arrows`). Die Amethyst-Ausnahme ohne Klassenbezug: der Modul-Pfeil mit
   Amethyst-Spitze setzt vor dem Treffer `pickup = DISALLOWED` (er zerspringt ohnehin), SBs Regel „nur ALLOWED“ greift.
3. **Spitzen datengetrieben** (Konzept „Rezept-JSON je Teil + Tag“): jedes Teil hat einen Item-Tag `simplearrows:fletching/<art>/<teil>`
   (Vanilla fest, Fremd-Items `required:false`), Slots/Ergebnis lesen nur den Tag (auch Client, Tags werden synchronisiert). Werte und
   Wirkungen bleiben fest im Code (B14: keine Config, nichts ausnutzbar). Ein Teil mit leerem Tag fehlt in Tab/Testzentrale/Buch.
   Rezepte `simplearrows:fletching/<art>/<teil>`, fremde mit `fabric:load_conditions`: Enderit-Spitze → simpleores, Diamantstab →
   simplebuilding. Diamant-/Netherit-Spitze nutzen SimpleLib-Material (`simplelib:diamond_pebble`, `simplelib:netherite_nugget`), das
   Modul fordert `cracked_diamond` + `netherite_nugget` an (wie Simple Machines) → allein spielbar mit 8 Spitzen, 4 Schäften.
4. **Puppenkopf** (Trainingspuppe zählt als ihr Kopf-Mob): Modul-Regel per Id `simplebuilding:training_dummy` + Kopf-Block-Typ (reines
   Vanilla, Kopie der SB-Regel `DummyTargets`), kein Import.
5. **IdMoves** (`simplebuilding:` → `simplearrows:`): ITEM/ENTITY_TYPE `crafted_arrow`, DATA_COMPONENT_TYPE `arrow_parts`,
   RECIPE_SERIALIZER/RECIPE_TYPE/MENU `fletching`, RECIPE_BOOK_CATEGORY `fletching_tip|shaft|fletching`, RECIPE der 16 Teile,
   ADVANCEMENT `recipes/combat/fletching_table_parts`. Lang-Schlüssel ziehen mit (`item.simplearrows.crafted_arrow.*`,
   `container.simplearrows.fletching*`, `jei.simplearrows.note.fletching`).
6. **Ein Tab**: SB-Tab `ARROWS` entfällt (Enum, Lang, `arrowsRows`). Modul-Tab: Licht-, Silberpfeil, dann die Pfeile je Spitze, Handbuch.
   Vanilla-Kampf-Tab unverändert nur Licht/Silber (90 Pfeile dort wären Lärm).
7. **Fehlerbehebung (bewusste Abweichung von „kein neues Verhalten“)**: `simplearrows:crafted_arrow` in `#minecraft:arrows` (Item; Entity
   `#arrows`/`#impact_projectiles` wie die anderen Modul-Pfeile) – sonst schießt kein Bogen die Tisch-Pfeile.
8. **Bildschirm** im Modul mit SimpleLib-Bausteinen (UiBoxes/UiMotifs/UiSymbols/UiSlotIcons), gleiche Maße, Gravur A, gleiche Farben;
   Vanilla-Look ohne Stil wie bisher. Kein neues Optik-Asset (Sprites/Modelle/Texturen per git mv).
9. **Rand:** Discover (`ArrowsDiscover`, gruppierter Fortschritt), Handbuch (`ArrowsGuide`), Wiki-Manual (Einträge aus SB-Manual ins
   Modul), Questionary-Eintrag, JEI-Sondereintrag im Modul, Preise (nichts verkauft, Text ergänzt), Testzentrale (SB-Abschnitt liest den
   Modul-Tab per Id), modtree/MODS-UND-FEATURES. NeoForge/Forge: Registrierungen entfernt, nicht kompiliert (Regel „nur 26.3 Fabric“).

## 3. Tests
- Modul (`module_game_test_*`, Fabric + standalone): die 8 Tisch-Tests portiert (Spitzen-Bonus, Flug, Diamantstab nur mit SB, alte
  Schäfte, 4 Pfeile, Rezeptbuch, ein Eintrag je Teil, Rechtsklick öffnet, Trank-Slot), `old_fletching_ids_move` (Item, Entity,
  Komponente im gespeicherten Stapel, Rezepte, Kategorien, Fortschritt), `fletching_layout` (Slot-Reihe, Buch-Knopf),
  `amethyst_arrow_is_not_recovered`, `dummy_head_counts_for_tips` (nur mit SB), `tip_tags_are_data` (Standalone: Teile ohne Mod fehlen),
  `discover_*`, `guide_book`.
- SB: `CombatTests`/`TrainingDummyTests`/`DataIntegrityTests`/Testzentrale ohne Modul-Klassen; `FletchingTests` + Adapter entfallen.
- sbt: Compile `:mc26_3:fabric:compileJava :mc26_3:fabric:compileGametestJava :modules:simplearrows:fabric:compileJava`, Modul-Targets,
  `fabric-263` gefiltert (`in_world_export*`, `modpack*`, `test_centre*`, `data_integrity*`, `config_option*`, `combat*`,
  `training_dummy*`), check_client_mixins, wiki --check, Probe mit echter Maus (fletching_probe.py, xvfb).

## 4. Risiken
- Kein lokaler Compile (sb-agent): API gegen 26.3-Jar geprüft (javap), ein sbt-Lauf + ein Nachlauf.
- Tag-Lookup im statischen Kontext vor dem Datapack-Laden: `allCombinations` nur zur Laufzeit (Tab/Testzentrale), Tags dann gebunden.
- NeoForge/Forge-Quellen unkompiliert (bewusst).
