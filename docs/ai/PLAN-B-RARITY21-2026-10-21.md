# Plan b-rarity21 (Sitzung B, 21.10.)

Queue: zwei Zeilen „(B, b-rarity21, 21.10.)“ – (1) Seltenheitsstufen über Vanilla EPIC hinaus in simplelib,
(2) Netherherz/Nether-Juwel animiert (Gold-Adern → Blau und zurück), Nether Gem → „Red Nether Gem“.

## Ist-Zustand
- 26.3 `Rarity` ist ein Enum (COMMON/UNCOMMON/RARE/EPIC). NeoForge macht es erweiterbar (`IExtensibleEnum`), Fabric und
  Forge nicht – eigene Enum-Werte gehen also nicht für alle drei Loader.
- Namensfarbe: `ItemStack.getStyledHoverName()` (Tooltip) und `Hud.extractSelectedItemName` (Hotbar-Name) und
  `ItemStack.getDisplayName()` (Chat-[Item]) bauen alle `Component.empty().append(getHoverName()).withStyle(<Rarity-Farbe>)`.
  Gemeinsamer Punkt: `getHoverName()`. Die Hud-Fassung unterscheidet sich zwischen Vanilla und NeoForge, `getHoverName` nicht.
- `Style` kennt seit 1.21.4 `shadowColor`; ein gesetzter Schattenwert zeichnet immer (auch ohne Schatten-Flag), Alpha
  folgt dem Text (Hotbar-Ausblenden bleibt korrekt).
- Netherherz/-juwel: `tools/textures/wither21_textures.py` (Ebenen-Quelle, reines Python, Variante A gold, Variante B blau
  beim Herz). Item- und Block-Textur je PNG + `.layers.json`. Animierte Item-Texturen mit `.png.mcmeta` gibt es schon
  (simplecrucibles, simpletrims).

## Entscheidung Seltenheit (sauberste Lösung)
- **Eigene Item-Komponente `simplelib:rarity`** (`SimpleRarity` = CURSED | MYTHIC, Codec + StreamCodec, persistent + synchron)
  **plus Vanilla `rarity = EPIC`** als Rückfall (Sortierung, fremde Mods, Clients ohne Mixin sehen Epic-Lila).
  Komponente statt NeoForge-Enum-Erweiterung, weil sie auf allen drei Loadern gleich ist und je Stack gesetzt werden kann
  (Loot, Befehle, `/give ...[simplelib:rarity=cursed]`).
- **Ein gemeinsamer Mixin auf `ItemStack.getHoverName()`** (alle Loader, Client und Server): umhüllt den Namen mit dem
  Stufen-Stil. Die äußere Rarity-Farbe der Aufrufer wirkt nur auf den Elternteil, der Kind-Stil gewinnt → Tooltip,
  Hotbar-Name, Chat-[Item], Todesmeldungen, Item-Rahmen-Name usw. alle in der neuen Stufe, ohne loaderspezifische Hud-Mixins.
- CURSED: strahlendes Rot `#FF3030`, Schatten schwarz `#000000` (schwarze Kontur/Hinterlegung unten rechts, wie
  Vanilla-Schatten, aber schwarz statt Dunkelrot). Echter 8-fach-Umriss (`Font.drawInBatch8xOutline`, Leuchtschild) wäre ein
  Render-Eingriff an jeder Textstelle – verworfen.
- MYTHIC: Türkis `#3CF0D2` → Hellgrün `#9CFF6A` als Farbverlauf je Zeichen, Schatten dunkeltürkis `#0A3A32`, Verzauberungs-
  Glint am Item (`enchantment_glint_override`). Nur auf dem Client-Render-Thread (per Mixin auf `Minecraft.<init>` gemerkt)
  wird der Name zeichenweise eingefärbt: in `getHoverName` statisch (Hud vergleicht Namen per `equals` – muss stabil bleiben),
  in `getStyledHoverName` (Tooltip) zusätzlich animiert (Verlauf wandert, alle 3 s läuft ein heller Schimmer durch). Auf dem
  Server bleibt es ein übersetzbarer Text mit einfarbigem Türkis-Stil (keine eingebrannte Serversprache).
- API `com.simplelib.api.SimpleRarity`: `CURSED.apply(props)` / `MYTHIC.apply(props)`, `SimpleRarity.of(stack)`,
  `style()`, `color()`, `shadow()`. Module brauchen nur simplelib (Pflichtabhängigkeit) → allein spielbar.

## Zuordnung
- **Cursed:** `simpletools:biome_brush`, `simplemobs:nether_gem` (Red Nether Gem), `simplemobs:nether_heart`
  (Beute des Verdorrten Withers, gleiche Familie wie das Juwel).
- **Mythic (absolut selten):** `simplebeacons:strong_upgrade_template`, `simplebeacons:strong_beacon`,
  `simplebeacons:strong_conduit` (Endstufe, nur über die Vorlage erreichbar).
- Nicht angefasst: Herobrine-/Shops-Items (b-shopsherob21), SimpleBuilding-Kern (26.2-/1.21.11-Kopien, späterer Port-Run).

## Netherherz / Red Nether Gem
- ID bleibt `simplemobs:nether_gem` (nur Namen ändern; keine IdMoves-Migration nötig, kein Risiko für Welten).
- Lang: EN „Red Nether Gem“, DE „Rotes Nether-Juwel“; Wiki-Manual, Guide-Titel, modules.json, fabric.mod.json,
  MODS-UND-FEATURES nachziehen.
- Animation (Herz Item + Block, Juwel Item + Block): Streifen 16×80 (5 Bilder), `.png.mcmeta` mit `frames`:
  Bild 0 gold 200 Ticks (10 s Ruhe), 1–3 je 2 Ticks (Adern werden von oben nach unten blau), 4 blau 40 Ticks (2 s),
  3–1 zurück je 2 Ticks. Herz: Ader-Pixel E/C gold → blau; Juwel: Glanz F/C gold → blau.
  Erzeugt von `tools/textures/wither21_textures.py` (Ebenen-Quelle 16×80 mit Grundform + Licht), keine Mojang-Pixel.

## Dateien
- simplelib: `api/SimpleRarity.java` (neu), `registry/LibComponents.java`, `mixin/SimpleRarityNameMixin.java` (neu),
  `mixin/client/RarityClientThreadMixin.java` (neu), `simplelib.mixins.json`, `test/LibTests.java` + Fabric-Adapter.
- simpletools `ToolsRegistry`, simplemobs `WitherMobs`, simplebeacons `BeaconsRegistry` + je ein Gametest + Adapter.
- `tools/textures/wither21_textures.py`, Texturen + mcmeta, Lang, Wiki/Guide-Quellen.

## Verifikation
- Gametests: `simplelib:*rarity*` (Komponente, Stil, Codec), `simplemobs:*rarity*`, `simpletools:*rarity*`,
  `simplebeacons:strong_items_rarity` auf fabric-263 + neoforge-263, Standalone-Targets der drei Module, `simplebuilding:data_integrity`.
- Compile Fabric/NeoForge/Forge 26.3, `wither21_textures.py --check`, `layers.py check`, gen (Guides/Wiki/Discover), `check`.
- Keine Client-Läufe (Optik nur per Vorschau-HTML; Client-Sicht bleibt Besitzer-Abnahme).
