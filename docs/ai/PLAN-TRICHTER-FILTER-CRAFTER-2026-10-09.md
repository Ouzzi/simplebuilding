# Plan: Trichter-GUI, Filter-Prinzip, Truhen, Autonomer Crafter (2026-10-09, Branch claude-q-hopper)

Aufträge: Queue Nachtrag 23 „Trichter“ + „Truhen“, Nachtrag 26 „Autonomer Crafter“ + „Filter-Prinzip“,
Prinzip `docs/ai/PRINZIPIEN-FILTER.md`.

## Ist-Zustand (erhoben)

- Rezept verstärkter Trichter (Trichter + gesprungener Diamant + Namensschild) existiert schon
  (`reinforced_hopper_from_crafting`, Test `hopperRecipesCraftFromTheirDocumentedPatterns`). Nichts zu tun.
- Fallen-Truhen-Titel ohne „Trapped“ und kein „Stacks xN“ im 26.2-Bild: schon erledigt (1b374aa2d,
  `docs/ai/PLAN-TRUHEN-GUI-2026-10-08.md`). Offen dort: der 26.3-Kasten-Stil
  (`ModScreenStyle.tieredChestLabels`) zeichnet noch Stapel-Symbol + „x2/x4“ – auch bei Shulker-Kisten (gleiches Menü).
- Fallen-Truhen-Texturen: `tools/textures/tier_chest_textures.py --trapped` färbt Vanillas Masken-Pixel kräftig rot.
- Mod-Trichter: Filter über **Geister-Items** (`ghostItems`, Payloads `SetHopperGhostItemPayload`/`SyncHopperGhostItemPayload`,
  `HopperSync`, Screen-Klick-Abfang). Widerspricht dem neuen Filter-Prinzip.
- 26.3-Trichter-UI: Text „Filter“ ist schon weg, Taste mit eingraviertem Trichter + Modus-Abzeichen rechts neben den Slots
  (4 px Abstand), Slots wie Vanilla bei x=44.
- Vanilla-Crafter: `CrafterBlock`/`CrafterBlockEntity`/`CrafterMenu`/`CrafterScreen`; Vorlage für Mod-Automaten: Auto-Schmied
  (`McVersion.AUTO_SMITHER`, Registrierung je Loader, Forge-ItemHandler-Mixin).

## Entscheidungen

1. **Filter-Prinzip (Trichter):** Im Filtermodus ist das echte Item im Slot der Filter. Automatik (Trichter/Rohre) legt nur
   in belegte, passende Slots (leer = nimmt nichts); der Spieler darf in leere Slots legen (= Filter setzen). Weitergegeben
   bzw. herausgezogen wird nur, solange mehr als 1 im Slot liegt (`WorldlyContainer#canTakeItemThroughFace`, `canTakeItem`,
   eigener Schub). Modi bleiben: aus / exakt (gleiche Komponenten) / gleiche Art (gleiches Item).
   Geister-Items fallen ersatzlos weg (Felder, Payloads, HopperSync, Screen-Abfang, Jade-Zeile). Alte Welten: gespeicherte
   `GhostItems` werden ignoriert (kein Item kann erfunden werden) – leere Slots im Filtermodus nehmen dann nichts an, bis
   der Spieler ein Item hineinlegt.
2. **Gemeinsamer Code:** Abgleich + „eins bleibt liegen“ als `com.simplelib.api.filter.ItemFilters` wäre Wunsch, aber der
   Trichter liegt in `common/src/shared` und kompiliert auch für 26.2 ohne simplelib. Daher: Logik in SimpleBuilding
   (`util/ItemFilter`, beide Linien, von Trichter und Crafter genutzt), **Knopf-Zeichnung in simplelib**
   (`UiFilterButton.draw`, Modus-Symbole), weil UI ohnehin nur 26.3-Kasten-Stil ist. Begründung im Bericht.
3. **Trichter-UI (26.3):** 5 Slots, Lücke 22 px (4 + 1 Slot) mit Trichter-Symbol + „:“, dann die Taste (nur Modus-Symbol);
   Block mittig (Slots ab x=24). Slots werden im Menü nur auf der Stil-Linie verschoben (`McVersion.CRUCIBLE` = simplelib da).
4. **Truhen:** Stapel-Symbol aus `tieredChestLabels` raus; Fallen-Texturen: nur Vanillas Rotstich (dR−dG) × 0,6 auf die
   Stufen-Pixel, keine Flächenfärbung. Vorschau `/root/previews/hopper/trapped-{before,after}.png`.
5. **Lore-Trichter** (Hopper-Minecarts der Mod-Stufen, im selben Queue-Punkt): nicht in diesem Branch (eigene Entitäten,
   Renderer, 3 Loader) – bleibt offen in der Queue.
6. **Autonomer Crafter** (`simplebuilding:autonomous_crafter`, nur 26.3 über `McVersion.AUTONOMOUS_CRAFTER`):
   - Block wie Crafter (Eigenschaften `Blocks.CRAFTER`), ohne Ausrichtung; `TRIGGERED` (Redstone) und `CRAFTING` (Gesicht).
   - Takt: alle 4 Ticks ein Versuch, nur wenn **unter ihm ein Trichter** (BlockEntity `Hopper`, Vanilla oder Mod) steht und
     **kein Redstone-Signal** anliegt. Ergebnis + Reste gehen in den Trichter darunter; passt das Ergebnis nicht ganz, wird
     nicht gecraftet.
   - 3x3 mit abschaltbaren Slots wie der Crafter, Filter-Taste (aus / exakt / gleiche Art). Filter an: jeder belegte Slot
     behält 1 Item (Rezept-Vorlage), gecraftet wird erst, wenn jeder belegte Slot ≥ 2 hat; Trichter oben legen nur
     passende Items in belegte Slots. Filter aus: Verhalten wie Vanilla-Crafter (letztes Item wird verbraucht, Einlegen
     wie Vanilla `canPlaceItem`).
   - Unten nichts herausziehbar (Trichter darunter saugt keine Zutaten ab), andere Seiten nehmen nur auf.
   - Menü/Screen eigen (Kasten-Stil); Slot-Abschalten und Filter über `clickMenuButton` (keine neuen Payloads).
   - Rezept: Crafter + Trichter + Redstone-Vergleicher (formlos). Textur: eigener Generator `tools/textures/autonomous_crafter.py`.
   - Lang EN/DE, Wiki (`wiki/manual.json` additiv), Loot (dropSelf), Spitzhacke-Tag, Kreativ/Suche neben Auto-Schmied.
   - Tests: craftet nur mit Trichter darunter; stoppt bei Redstone; Filter-Item bleibt liegen; Wrapper + Katalog.

## Tests / Verifikation

- Compile Fabric/NeoForge/Forge 26.3 (+ gametest).
- Trichter: Filter-Tests in `HopperTests` neu (eins bleibt liegen, Automatik nur in passende belegte Slots, Herausziehen
  lässt 1), Geister-Tests entfernt/umgeschrieben (HopperTests, HopperAndTrimTests, BlockInfoTests, WorldUpgradeTests,
  SledgehammerUpgradeTests, Client-Tests).
- Crafter-Tests (Fabric/NeoForge/Forge), Datagen, `generate_textures --check`, `wiki/generate.py --all --check`.
- Client-Screenshots `mod-ui-style` vorher/nachher nach `/root/previews/hopper/`.
