# Plan Simple Sandwiches v2 – Besitzer-Feedback nach Client-Test (2026-10-05)

Branch `claude-sandwiches2` (Basis `cb466f86`). Vorgänger-Plan: `PLAN-SIMPLE-SANDWICHES-2026-10-04.md`.

## Ist-Zustand
- Brett: `CuttingBoardBlock.useItemOn` → `interact()`; Client und Server entscheiden beide per `plan()` anhand des
  Block-Entity-Zustands. Vanilla-Reihenfolge (`Minecraft.startUseItem`, 26.3 geprüft) ist useItemOn → useWithoutItem →
  `useItem`. Liefert der Client `PASS` (z. B. weil sein Block-Entity-Stand vom Server abweicht oder das Item in diesem
  Zustand nichts tut), ruft er sofort `useItem` → Brot/Sandwich/Zutat wird gegessen statt aufs Brett gelegt.
  Die bisherigen GameTests rufen `BlockState.useItemOn` direkt und decken den Client-Pfad nicht ab.
- Renderer legt Brot/Zutaten als flache Item-Modelle aufs Brett; „aufgeschnitten“ sieht aus wie der Laib.
- Texturen: `tools/textures/sandwiches.py` (Sandwich Seitenansicht, Messer, Brett aus Planken, Kuchenstück).
- `SliceBlock` (Käse/Butter): Loot droppt angeschnitten die Restscheiben; kein Bounce; Schnitt-Sound = Schafschere.

## Umsetzung
1. **Brett-Vorrang (alle Loader, Code in shared):** `CuttingBoardBlock.claims(stack)` = Brot, Sandwich, Messer,
   Butterscheibe, Zutat-Tag. Auf dem **Client** gibt `interact` für solche Items immer `SUCCESS` zurück (der Server
   entscheidet; der Client startet nie das Essen, solange er aufs Brett zielt). Server unverändert. Schleichen umgeht
   das Brett wie Vanilla. GameTest `board_priority_over_eating`: hungriger Spieler über `ServerPlayerGameMode.useItemOn`
   (Server-Pfad) mit Brot, Sandwich, Zutat; danach kein `isUsingItem`; dazu `claims()`-Prüfung als Ersatz für den
   Client-Pfad (Client nicht testbar ohne Client-Start).
2. **Aufgeschnittenes Brot:** Stufe OPEN zeigt zwei Brothälften nebeneinander (Schnittfläche oben); Butter und Zutaten
   liegen auf der linken Hälfte. Umsetzung ohne neues Registry-Item: `ItemStack(BREAD)` mit `DataComponents.ITEM_MODEL`
   = `simplesandwiches:bread_half` (Item-Modell-Datei + Textur `item/bread_half`).
3. **Texturen (je Gruppe 3 Varianten A/B/C, beste eingebaut, Generator-Schalter):** Sandwich/Brot (Schichten bleiben),
   Brothälfte, Messer, Schneidebrett (Maserung, Rand, Griffloch, je Holz aus Planken), Kuchenstück. Vorschau
   `previews/sandwiches-v2-vorschau.png` (aktuell | A | B | C, 16× + 1×).
4. **Brett-Modell:** gestufte Kante (Grundplatte 1 px + eingerückte Deckplatte 1 px), Hitbox unverändert.
5. **Käse/Butter-Zustand:** Loot angeschnitten = Block-Item mit `minecraft:copy_state` (slices, cut); Vanilla-`BlockItem`
   stellt den Zustand beim Platzieren wieder her (`BLOCK_STATE`-Komponente). Item-Modell zeigt die Restmenge
   (`minecraft:select` auf `block_state` slices). Scheiben weiterhin nur per Messer. Gilt für Käse und Butter.
6. **Käse federt:** wie `BedBlock` – Fallschaden halbiert, Abprall `-vy*0.66` (Schleichen unterdrückt).
7. **Sounds:** `simplesandwiches:block.butter_block.cut`, `block.cheese_block.cut` (Registry je Loader, `sounds.json`,
   je 3 OGG-Varianten selbst synthetisiert mit `tools/sounds/make_sandwich_sounds.py`, numpy+soundfile, keine Samples),
   Untertitel EN/DE.
8. Doku: Wiki-Prosa `modules/simplesandwiches/wiki/manual.json`, Guide-Generator, Memory `sandwiches-offen.md`.

## Annahmen (selbst entschieden)
- „nur wenn nicht hungrig“: Ursache liegt im Client-Pfad (useItem nach PASS) → Fix wie (1); Wirkung auch bei Sandwich/
  Zutaten/Butter. Nebenwirkung: Wer aufs Brett zielt, isst nicht (gewollt: Brett hat Vorrang); Schleichen = essen/platzieren.
- Vollständiger Block droppt ohne Zustandskomponente (stapelt mit neuen Blöcken); angeschnittene Blöcke stapeln nur
  mit gleichem Zustand.
- Kuchenstück bleibt 2D-Item (vanilla-Kuchen-Farben), kein 3D-Modell.

## Risiken
- Client-Verhalten (Essen, Renderer, Modelle) nur per Code-Review, kein Client-Start erlaubt.
- `ITEM_MODEL`-Komponente auf einem Render-Stack: nur clientseitig, keine Server-Daten.

## Verifikation
Module-Tests simplesandwiches fabric/neoforge (+ standalone), simplelib, fabric-263 Kern, `gradlew check -q`,
Modul-Generator `--check`, `tools/guides/module_guides.py`, Wiki `--all` + `--all --check`, Textur-Generator `--check`.
