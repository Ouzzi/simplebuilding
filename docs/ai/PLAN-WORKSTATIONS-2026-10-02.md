# Plan – Rezeptbuch in Befiederungs-/Schmiedetisch + Auto-Schmied (2026-10-02)

Quelle: `.claude/QUEUE.md` („Rezeptbuch in Befiederungs- und Schmiedetisch“, „Auto-Schmied analog zum Autocrafter“).
Branch `claude-workstations`, nur Hauptlinie 26.3 (Flags), Fabric zuerst, NeoForge/Forge mitverdrahtet.

## Ist-Zustand (erhoben)
- `FletchingMenu`/`FletchingScreen` (shared): eigenes Material-Panel links mit Knopf (`clickMenuButton`), kein Vanilla-Rezeptbuch.
- `SmithingScreenMixin` (client): Knopf links neben dem Schmiedetisch öffnet `TrimReferenceScreen` (Besatz-Resonanz-Vorschau).
- Vanilla 26.3 (dekompiliert nach `%TEMP%\mcsrc263x`): `AbstractRecipeBookScreen` + `RecipeBookComponent<T extends RecipeBookMenu>`
  zeigen die Rezepte aus `ClientRecipeBook` nach `RecipeBookCategory`; Klick → `ServerboundPlaceRecipePacket` →
  `ServerGamePacketListenerImpl.handlePlaceRecipe` → nur wenn `containerMenu instanceof RecipeBookMenu` und das Rezept bekannt ist
  → `handlePlacement` (Vanilla: `ServerPlaceRecipe` mit Gitter). Fehlt etwas → Geisterrezept-Paket.
  `RecipeBookComponent`-API ist auf 26.2 identisch (javap geprüft) → Code darf in `shared` bleiben.
- Schmiede-Rezepte (`SmithingRecipe`) haben schon Kategorie `SMITHING` und `SmithingRecipeDisplay`; sie stehen nach
  Freischaltung (Vanilla-Advancements) im Rezeptbuch des Spielers, nur zeigt Vanilla kein Buch am Schmiedetisch.
- `Inventory.fillStackedContents` zählt nur „einfache“ Stapel (nicht beschädigt/verzaubert/benannt) → für Schmieden
  (verzauberte Rüstung) eigene Platzierung nötig.
- Es gibt keinen Mod-Autocrafter; „Autocrafter“ = Vanilla-Crafter (`CrafterBlock`: Redstone-Flanke → 4 Ticks → ein Ergebnis
  nach vorn, Komparator, `TRIGGERED`/`CRAFTING`).

## Umsetzung
### 1 Befiederungstisch mit echtem Vanilla-Rezeptbuch
- Neuer Rezepttyp `simplebuilding:fletching` (`FletchingRecipe(tip, shaft, fletching)` aus `ArrowParts`-Enums), Serializer,
  eigene `RecipeBookCategory` `simplebuilding:fletching`; Anzeige über Vanilla `SmithingRecipeDisplay`
  (Spitze/Schaft/Befiederung → Ergebnis, Station Befiederungstisch) – kein eigener Display-Typ nötig.
  Registrierung: Fabric `ModRecipes`, NeoForge/Forge `*ModRegistries` (DeferredRegister inkl. `RECIPE_BOOK_CATEGORY`);
  Halter `FletchingRecipes` (shared) mit statischen Feldern.
- Datagen: ein Rezept je Kombination (`allCombinations()`, 9×7×2 = 126) unter `recipe/fletching/<tip>_<shaft>_<fletching>.json`,
  nur mit `McVersion.FLETCHING` (landet in `mc26_3/generated`). Kein Advancement: Öffnen des Tisches schaltet alle
  Befiederungsrezepte frei (`awardRecipes`, ohne Toast) – entspricht dem bisherigen Panel, das immer alle Materialien zeigte.
- `FletchingMenu extends RecipeBookMenu` + `ServerPlaceRecipe.CraftingMenuAccess`: `handlePlacement` = Vanilla
  `ServerPlaceRecipe` mit 3×1-Gitter (Spitze, Schaft, Befiederung); Shift = maximale Menge; fehlt etwas → Geisterrezept.
  `RecipeBookType.CRAFTING` (Offen/Filter-Zustand wie die Werkbank, geteilt). Alte Knopf-Route `clickMenuButton` entfällt.
- `FletchingScreen extends AbstractRecipeBookScreen`: Hintergrund = Vanilla `crafting_table.png`, die 6 unbenutzten Gitterfelder
  übermalt; Teile diagonal auf Gitterplätzen (66,17)/(48,35)/(30,53), Ergebnis (124,35) wie Werkbank; Buchknopf
  `leftPos+5, height/2-49` (exakt Werkbank); Titel x=29, Text „Fletching“/„Befiederung“.
- `FletchingRecipeBookComponent`: ein Tab (Pfeil-Symbol), Filter-Sprites wie Werkbank, Geisterslots per Invoker-Mixin
  (`GhostSlots#setInput/setResult` sind package-protected).
- Slot-Silhouetten (`Slot#getNoItemIcon`, wie Rüstungsslots): `simplebuilding:container/slot/arrow_tip|arrow_shaft|arrow_fletching`,
  Generator `tools/textures/fletching_slot_icons.py` (Vanilla-Grau 85,85,85, Formen aus Vanilla-`arrow.png`/`stick.png`/`feather.png`),
  Vorschau `previews/fletching-slots-vorschau.png`.

### 2 Schmiedetisch: Rezeptbuch statt Resonanz-Knopf
- `SmithingScreenMixin`: Knopf + `TrimReferenceScreen`-Öffnung entfernt (Screen-Klassen bleiben, falls woanders genutzt; sonst löschen).
- `RecipeBookSmithingScreen extends SmithingScreen implements RecipeUpdateListener` (Logik von `AbstractRecipeBookScreen` übernommen);
  Austausch per `Gui#setScreen`-Mixin nur bei exakt `SmithingScreen.class` (andere Mods/Unterklassen unberührt), Flag
  `McVersion.SMITHING_RECIPE_BOOK`. Buchknopf über dem dritten Slot: `leftPos+42, topPos+27` (zwischen Titel und Slot).
- Client-Adapter `SmithingBookMenu extends RecipeBookMenu` (nie registriert) liefert der Komponente Typ und Bestand
  (inkl. verzauberter/beschädigter Inventar-Items, damit „herstellbar“ stimmt).
- Server: Mixin auf `handlePlaceRecipe` (nach `ensureRunningOnSameThread`): ist das offene Menü ein `SmithingMenu`, gleiche Prüfungen
  wie Vanilla (Container-ID, `stillValid`, Rezept bekannt, Typ SMITHING), dann `SmithingPlacement`: Felder ins Inventar räumen,
  passende Items suchen (auch verzaubert), Vorlage/Material 1 (CountBased: deren Anzahl), Shift = ganzer Stapel; fehlt etwas →
  Geisterrezept-Paket.

### 3 Auto-Schmied (`simplebuilding:auto_smither`, Flag `McVersion.AUTO_SMITHER`)
- `AutoSmitherBlock` (FACING 6 Richtungen, TRIGGERED, CRAFTING) wie `CrafterBlock`: steigende Redstone-Flanke → 4 Ticks → ein
  Schmiedevorgang, Ergebnis in den Container davor (Hopper-Logik) oder als Item nach vorn; kein Rezept → Klick-Sound 1050.
  Komparator = Anzahl belegter Eingänge × 5 (0/5/10/15).
- `AutoSmitherBlockEntity` (WorldlyContainer, 3 Slots): Hopper/Seiten legen nach Vanilla-Property-Sets ein (Vorlage → 0, Basis → 1,
  Material → 2), Herausziehen per Hopper gesperrt (wie Ausgabe nur nach vorn). Ergebnis: `TrimUpgrades.result` (Mod-Regeln) sonst
  `RecipeType.SMITHING`; Verbrauch je 1, CountBased-Material entsprechend.
- Menü/Screen: 3 Eingänge + Vorschau-Ergebnis (nicht nehmbar), Vanilla-`smithing.png`-Layout, Redstone-Anzeige wie Crafter.
- Rezept (Datagen): `III / ISI / RDR` (Eisen, Schmiedetisch, Redstone, Spender) analog Crafter. Loot (droppt sich + Inhalt),
  Spitzhacken-Tag, Kreativ-Tab neben Crafter-artigem Zeug, Lang EN/DE (beide Orte), Blockstate/Modelle per Datagen.
- Textur: Generator `tools/textures/auto_smither_textures.py` aus Vanilla-Schmiedetisch + Crafter-Front (Redstone-Auge),
  Vorschau `previews/auto-smither-vorschau.png`.

## Tests (GameTests, `SimpleBuildingGameTests` + Fabric-Adapter)
- Fletching: Platzierung über `handlePlacement` (legt Teile ein / Geisterrezept bei Fehlen / Shift max), Rezepte geladen (126) und beim
  Öffnen freigeschaltet. Alter Knopf-Test ersetzt.
- Smithing: `SmithingPlacement` legt Netherit-Upgrade mit verzauberter Diamantrüstung ein; fehlende Teile → kein Verschieben.
- Auto-Schmied: Redstone-Puls schmiedet genau einmal und wirft nach vorn; in Truhe davor; Hopper-Einsortierung; kein Doppelauslösen
  bei Dauersignal; Komparator.
- Gates: `run.py --targets fabric-263` und `neoforge-263`, `:compileJava :neoforge:compileJava` (26.2),
  `-Pforge263=true :mc26_3:forge:compileJava`, Texturen-/Wiki-Checks soweit betroffen.

## Risiken / Annahmen
- Geteilter `RecipeBookType.CRAFTING`: Buch-offen/Filter gilt für Werkbank, Befiederungs- und Schmiedetisch gemeinsam (Annahme: gewollt
  „wie die Werkbank“; Vanilla-Enum ist nicht erweiterbar ohne Loader-Hacks).
- Befiederungsrezepte ohne Gruppen (Vanilla-Overlay kann `SmithingRecipeDisplay` nicht zeichnen) → 126 Einzelknöpfe, ~7 Seiten, Suche + Filter.
- Client-Anzeige nicht per Client-Test abgenommen, falls keine Client-Tests laufen dürfen (Besitzer-Clients) – im Bericht vermerken.
