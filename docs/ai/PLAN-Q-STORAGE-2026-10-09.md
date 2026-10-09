# Plan `claude-q-storage` (2026-10-09, Roadmap Gruppen D + E)

Queue: N15 Stapelgrößen-Rest, N18 Achtel-Stapel, N16 Trapped Copper Chest, N16 Nihil-Doppeltruhe, N17 Shulker-Zustand.

## Ist-Zustand
- simplelib `StackLimits` (0a84dab08) trägt Tiegel + angedocktes Fass + Client-Spiegel. Das **lose Fass** öffnet ein
  Vanilla-`ChestMenu` (Vanilla-`Slot` deckelt auf `stack.getMaxStackSize()` = 64, Client-Spiegel kennt keinen Faktor).
- Trichter: SB `HopperBlockEntityMixin` hebt die Grenze nur für SB-Truhen/-Shulker (`TieredChests.oversizedStorage`);
  Tiegel/Fass (simplelib) bleiben bei 64.
- TieredChests: x2/x4 schon mit eigener Slot-Klasse, Trichter-Mixin und Tests (`slotCountsAndStackLimitsFollowTheTier`,
  `vanillaHoppersFillAndEmptyOversizedSlots`). SB-`shared` kompiliert auch für 26.2 (ohne simplelib) und darf
  simplelib nicht importieren → Regel bleibt dort gleichlautend (`BackpackItem.maxStackSizeIn` = `StackLimits.max`).
- Achtel (Schach-Achtel `CheckerOctetItem`): Stapel 64. **Engine-Grenze 99**: `ItemStack.CODEC` und
  `max_stack_size` erlauben nur 1..99 – ein Spielerinventar mit 128er-Stapeln ließe sich nicht speichern.
- Nihil-Gewölbe: 27 Plätze, `ChestMenu.threeRows`; Astral-Gewölbe: 54 (`sixRows`).
- Trapped Copper Chest: gibt es nicht. Vanilla `CopperChestBlock` hängt fest an `BlockEntityTypes.CHEST`.
- Shulker: Vanilla-Deckel folgt nur dem Öffnerzähler.

## Umsetzung
1. **simplelib** `StackLimits`: `limit(container, stack, vanilla)` (größere Grenze des Containers, Doppeltruhe →
   erste Hälfte) + `StackLimits.LimitedSlot` (Grenze vom Container, nichts verlässt den Platz größer als ein
   normaler Stapel). Generischer Trichter-Mixin `HopperStackLimitsMixin` (dieselben drei Stellen wie SB, nur
   „anheben“, nie senken). Loses Fass: eigenes `BarrelMenu extends ChestMenu` mit `LimitedSlot`s und eigenem
   MenuType je Fass-Stufe (Client-Spiegel mit Faktor), Bildschirm = Vanilla `ContainerScreen`.
   Tiegel-/Fass-Slots im Tiegelmenü erben `LimitedSlot`.
2. **Achtel**: Schach-Achtel stapeln bis 99 (Abweichung von 128, Engine-Grenze); Material-Achtel aus
   `claude-q-hammer` sollen nach dem Merge denselben Wert nehmen.
3. **Nihil-Gewölbe**: 54 Plätze, sechs Reihen (alte 27er-Speicher laden unverändert in die ersten 27).
4. **Trapped Copper Chest** (nur 26.3, Schalter `McVersion.TRAPPED_TIERED_CHESTS`): 8 Blöcke (4 Oxidationsstufen
   × gewachst), eigener BE-Typ `trapped_copper_chest` (`ChestBlockEntity` + Signal wie Vanilla), Oxidation/Wachs/
   Axt wie die Kupfer-Druckplatte, Doppeltruhe nur mit Fallen-Kupfertruhen (älteste Stufe gleicht an wie Vanilla).
   Renderer: Vanilla-Kupfer-Sprite + eigenes Fallen-Overlay (nur die Vanilla-Fallen-Pixel, dezent rotverschoben,
   je Stufe; keine Vanilla-Textur im Repo). Item: `composite` aus Kupfertruhe + Overlay. Rezept: Kupfertruhe +
   Haken (wie Vanilla-Fallentruhe), Wachs-Rezept wie Vanilla.
5. **Shulker-Zustand** (Vanilla-Kisten, alle Farben): Komponente `simplebuilding:shulker_open`; offene Kiste
   (platziert) zeigt den Deckel offen; Rechtsklick schließt sie (Klang, kein Menü), danach normal. Öffnen:
   Rechtsklick mit Shulkerschale auf eine geschlossene Kiste. Abbauen behält den Zustand; Item-Modell offen/zu.

## Prüfung
Compile 3 Loader; neue GameTests je Punkt (simplelib `module-simplelib-*`, SB `fabric-263,neoforge-263,forge-263`);
`generate_textures --check`, Wiki `--all --check`; Vorschau `/root/previews/storage/`.
