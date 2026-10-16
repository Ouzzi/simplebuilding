# Plan: Warped Wart / Wirrwarze (Queue Nachtrag 71, Branch claude-warpedwart)

Besitzer-Text: Warped Wart wie Netherwarze (Seelensand, Wachstumsstufen, Ernte), in Bastionstruhen, für die Mod-Tränke
genutzt; 9 Warped Wart <-> Warped Wart Block; Blaue Netherziegel (türkis) mit Treppe/Stufe/Mauer in Simple Blocks.

## Ist-Zustand (geprüft)
- Vanilla 26.3: kein Item/Block "Warped Wart" (nur `minecraft:warped_wart_block`, Block ohne Pflanze). Bastionstruhen
  enthalten keine Netherwarze. Kein Rückport-Fall (PRINZIPIEN-VANILLA-RUECKPORTS): nichts aus Vanilla zu schalten, kein Flag.
- Brauen ist in 26.3 datengetrieben (`minecraft:brewing`-Rezepte). SB-Mod-Tränke: `mc26_3/fabric/.../ModBrewingProvider.java`
  (Datagen -> `mc26_3/generated/.../recipe/brewing`). Simple Mobs: JSON in `modules/simplemobs/.../recipe/brewing`.
- Kompost ist in 26.3 eine Item-Komponente (`Item.Properties.compostable`), nicht mehr eine Tabelle.

## Modul-Zuordnung
| Teil | Modul | Grund |
|---|---|---|
| Warped Wart (Item + Pflanze), Basis-Trank `simplelib:warped` ("Wirrer Trank"), Rezept Wart <-> Vanilla-Block, Bastion-Loot | **simplelib** | Mehrere Module (SB, Simple Mobs) brauchen die Basis; simplelib ist ohnehin in jedem Paket gebündelt (Regel 6a). Loot läuft über den vorhandenen Loot-Haken je Loader (`MaterialLoot.apply`: Fabric-Event, NeoForge-Event, Forge-Mixin). |
| Blaue Netherziegel + Treppe/Stufe/Mauer | **simpleblocks** | AGENTS.md Regel 16 (Baublöcke gehören in die Familie, hier Simple Blocks). |

## Mod-Tränke (Bestandsaufnahme per grep) und Umstellung
Basis wird `simplelib:warped` statt `minecraft:awkward` (Wasserflasche + Warped Wart; Wurf-/Verweilflasche wie Vanilla):
- **SimpleBuilding** (immer mit simplelib, weil es Simple Ovens/Mob Heads bündelt): Listiger Shulker (Shulkerkopf), Dunkelheit
  (Wärterranke), Übelkeit (Rotpilz), Zittern (Schneeball), Trugbild (Amethystsplitter), Verblassen (Tintenbeutel) -> nur noch Warped.
  Folgestufen (Redstone, Glowstone, Spinnenauge, Schwarzpulver, Drachenatem) bleiben, sie hängen am Mod-Trank.
- **Simple Mobs**: Stille (Echokern), Schwerkraft (Shellker-Schale), Fokus (Wächterauge), Trugbild (Trugsplitter, nur mit SB).
  Simple Mobs braucht allein kein simplelib -> **Fallback**: ohne simplelib bleibt der Seltsame Trank (Netherwarze) gültig
  (Rezept mit `not simplelib`-Bedingung, je Loader Fabric/NeoForge/Forge), mit simplelib gilt nur der Wirre Trank (Bedingung
  `simplelib geladen`). So bleibt jedes Modul allein spielbar und es gibt nie zwei Wege gleichzeitig.
- Bleiben bei der Netherwarze (Vanilla-Ergebnis oder Vanilla-Basis): Seelenfeuer-Rute -> lange Feuerresistenz, Verhüllte
  Unsichtbarkeit (Basis Unsichtbarkeit), alle Vanilla-Tränke.
- Keine weiteren Module mit Tränken (grep BrewingTests/ModEffects/Potions/recipe/brewing).

## Umsetzung
1. simplelib: `WarpedWartBlock extends NetherWartBlock` (Seelensand, AGE 0-3, Zufallsticks, kein Knochenmehl = wie Vanilla),
   Item (Kompost 65 % = wie Netherwarze), `LibPotions.WARPED`, Loot (Bastion: Hort 60 % 3-7, sonst 35 % 2-5), Rezepte, Lang,
   Wiki-Notiz; Generator `tools/gen_resources.py`. Hacke-Ernte (Simple Farming) pflanzt über `block.asItem()` nach.
2. simpleblocks: 4 Blöcke, Generator `tools/generate_data.py`, Rezept wie rote Netherziegel (2 Wart + 2 Netherziegel); ohne
   simplelib Ersatz mit Vanillas Warped-Wart-Block (1 Block + 8 Netherziegel -> 4).
3. Texturen: `tools/textures/warped_wart_2026_10_16.py` (eigene Pixelkunst, nichts aus Vanilla), Vorschau 8x
   `docs/ai/previews/claude-warpedwart/`.
4. Tests: simplelib (Wachstum, Ernte, Kompost/Pflanzbarkeit, Bastion-Loot der echten Tabellen, Brauen, Blockrezepte),
   Simple Blocks (Ziegel-Rezepte beider Wege, Stonecutter), SB `BrewingTests` (Mod-Tränke aus Warped; Seltsamer Trank braut sie
   nicht mehr), Simple Mobs (Rezeptschlüssel je nach simplelib).

## Risiken
- `not`-Bedingungen in Rezepten (Forge/NeoForge/Fabric) nur an Quellen geprüft; Verifikation über Module-Targets mit/ohne simplelib.
- Welten mit alten Mod-Tränken: Tränke bleiben gültig (nur Rezepte ändern sich).
