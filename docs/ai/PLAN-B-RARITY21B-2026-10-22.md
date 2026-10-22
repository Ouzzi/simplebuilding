# Plan b-rarity21b (Sitzung B, 22.10.)

Queue: zwei Zeilen „(B, b-rarity21b, 22.10.)“ (Besitzer 21.10., Folge b-rarity21):
(1) „verbotene … namen wie feuer animiert …, als wären die buchstaben aus feuer mit orange was rein animiert ist und das andere
etwas edler … edlere farben, die cool sind und zueinander passen“; (2) „das herz in 3d bitte etwas herzförmiger machen im 3d modell“.
Vorschau (nicht veröffentlicht): `scratchpad/rarity21b-preview.html` der Sitzung.

## Ist-Zustand (b-rarity21, in master)
- `com.simplelib.api.SimpleRarity` (CURSED `#FF3030` + Schatten schwarz, MYTHIC Türkis `#3CF0D2` → Hellgrün `#9CFF6A`),
  Komponente `simplelib:rarity`, Vanilla Epic als Rückfall. `SimpleRarityNameMixin` auf `ItemStack.getHoverName` (Stufen-Stil als
  Kind) und `getStyledHoverName` (nur Client-Render-Thread: Mythic animiert je Buchstabe). Cursed ist überall einfarbig.
- 26.3-Text: Komponenten färben nur **ganze Buchstaben**. Tooltip-Zeilen zeichnet `ClientTextTooltip.extractText(GuiGraphicsExtractor,
  Font, int, int)` (Vanilla-Klasse, Fabric/NeoForge/Forge gleich, per javap/Quelle geprüft) mit `graphics.text(font, text, x, y, -1, true)`.
  `GuiGraphicsExtractor.enableScissor` gilt je Text-Zustand; Glyphen werden in Einreichungs-Reihenfolge gezeichnet (`glyphStates`
  ungesortiert) → zusätzliche, auf **eine Pixelzeile** beschnittene Durchgänge liegen über dem Vanilla-Text.
- Hotbar-Name (`Hud`) vergleicht `getHoverName()` per `equals` → muss zeitunabhängig bleiben.
- Netherherz-Block: ein 8×8×8-Würfel (`models/block/nether_heart.json`, Fläche uv 4..12), kein `facing`, Form `box(4,0,4,12,8,12)`;
  Texturen aus `tools/textures/wither21_textures.py` (Ebenen-Quelle, Animation Gold → Blau, 16×80-Streifen + mcmeta).

## Entscheidungen
### Cursed: Buchstaben aus Feuer (empfohlen A „Glut“, Varianten B/C nur in der Vorschau)
- **Pixelzeilen-Feuer im Tooltip:** neuer Client-Mixin `RarityTooltipTextMixin` (TAIL von `ClientTextTooltip.extractText`) ruft
  `com.simplelib.client.RarityText.paint`: Buchstaben mit Marker (Style-`insertion` `simplelib:rarity/<stufe>`) werden nach dem
  Vanilla-Durchgang (der den schwarzen Schatten zeichnet) Zeile für Zeile (−3..8, Umlaute/Unterlängen) neu gezeichnet, jede Zeile per
  Scissor auf 1 GUI-Pixel beschnitten, ohne Schatten, Farbe = `SimpleRarity.pixel(buchstabe, anzahl, zeile, millis)`.
- **Feuerfunktion:** Flammenhöhe je Buchstabe = Welle, die von links nach rechts durch den Namen läuft (`sin`, ~1,7 s) + Flackern
  (glattes Wertrauschen, 110 ms); Hitze = Flammenhöhe − Höhe der Zeile → unten Gelb/Orange, oben strahlendes Rot. Rampe aus der
  abgenommenen Flammen-Palette (`UiFlames.FIRE`), nur die hellen Stufen, damit jede Farbe ≥ 4,5:1 auf dem Tooltip liest:
  `#FF3030` (Grundrot) → `#FF5A1E` → `#F7921C` → `#FFC832` → `#FFE98A`. Schatten bleibt schwarz (Besitzerwunsch 21.10. „schwarze Outline“).
- Rückfall: ohne Overlay (fremder Tooltip-Renderer) je Buchstabe die Farbe der Zeile 4 (animiert); Hotbar/Chat/Server unverändert
  einfarbig Rot + schwarzer Schatten (stabil für `equals`, „Fallback = statische Farbe“).
### Mythic: edler (empfohlen A „Opal“, Varianten B „Türkis-Gold“, C „Saphir-Silber“ nur in der Vorschau)
- Zyklischer, wandernder Verlauf Aquamarin `#5CF2DE` → Azur `#6AB6FF` → Lavendel `#C6A4FF` (benachbarte, kühle Edelstein-Töne, bleibt nah am
  ursprünglichen Türkis-Wunsch, klar getrennt von Epic-Magenta und vom Feuer), oberste Pixelzeilen perlweiß aufgehellt (Schliff-Glanz),
  alle 3,4 s ein schräger Perl-Glanz (`#FFFFFF`) quer durch die Buchstaben, Schatten Mitternachtsblau `#0F1838`, Glint bleibt.
- Server/statisch: Aquamarin `#5CF2DE`; Hotbar: statischer Verlauf (millis 0, ohne Glanz).
### Netherherz-3D-Modell (empfohlen A „prall“, B „flach“, C „mit Adern-Stummeln“ nur in der Vorschau)
- Herzsilhouette 12 breit × 11 hoch, Spitze unten auf dem Boden, zwei Lappen mit Kerbe; A 8 tief (Ränder/Lappen-Kuppen 6, Spitze 4 → rundlich),
  B 6 tief, C wie B plus zwei Adern-Stummel wie beim Item-Sprite. **Abweichung vom ersten Planstand:** C (Adern) war zuerst empfohlen, wirkt im
  Render aber wie zwei Schornsteine, und die mittige Goldader las sich wie ein Riss → A ohne Adern-Stummel, Ader seitlich versetzt.
  Elemente, Vorderbild und Seitenhaut kommen aus **einer** Definition in `wither21_textures.py` (Modell-JSON wird mitgeschrieben und mit
  `--check` geprüft).
- Texturen: `block/nether_heart.png` = Vorderbild passend zur Silhouette (Licht oben links, dunkle Kante unten rechts, Glanz am linken
  Lappen, Goldadern, Animation Gold → Blau bleibt: 5-Bild-Streifen + mcmeta); neu `block/nether_heart_side.png` (Fleisch ohne Adern) für
  Seiten/Oben/Unten. Je PNG + `.layers.json` (A8 b), eigene Pixel, Palette des Items (Vanilla-nah: Licht oben links, keine Mojang-Pixel).
- **Blickrichtung:** neue Klasse `NetherHeartBlock extends WitherLootBlock` mit `facing` (horizontal): beim Setzen schaut die
  Vorderseite zum Spieler; nach dem Wither-Tod zum nächsten Spieler (48 Blöcke, sonst Norden). Blockstate mit 4 Drehungen,
  Umriss `Shapes.rotateHorizontal(box(2,0,4,14,11,12))`. Alte Welten: fehlendes `facing` → Standard Norden (keine Migration).
  `WitherLootBlock` bekommt `lootState(level, pos)` (Standard `defaultBlockState()`), `WitherLoot.place` nutzt es; Juwel unverändert.

## Dateien
- simplelib: `api/SimpleRarity.java` (Farbfunktionen `pixel`, `letters`, Marker), `mixin/SimpleRarityNameMixin.java` (beide Stufen im
  Tooltip), neu `mixin/client/RarityTooltipTextMixin.java`, neu `client/RarityText.java`, `simplelib.mixins.json`, `test/LibTests.java`,
  `wiki/manual.json`.
- simplemobs: neu `NetherHeartBlock.java`, `WitherMobs.java`, `WitherLootBlock.java`, `WitherLoot.java`, `WitherTests.java` + Fabric-Adapter,
  `blockstates/nether_heart.json`, `models/block/nether_heart.json`, Texturen, `wiki/manual.json`.
- `tools/textures/wither21_textures.py`, `docs/MODS-UND-FEATURES.md`, Queue-Zeilen.

## Verifikation
- Gametests (fabric-263 + neoforge-263): `simplelib:*rarity*` (Stil, Marker, Feuer unten heißer als oben, Animation ändert Farben,
  Kontrast jeder Feuer-/Opal-Farbe über Buchstaben × Zeilen × Zeiten ≥ 4,5:1, Codec), `simplemobs:*nether*`/`*wither_loot*` (facing,
  Platzierung zum Spieler, Umriss je Richtung, Drehung), `simpletools`/`simplebeacons`-Rarity-Tests; Standalone simplelib/simplemobs.
- Compile Fabric/NeoForge/Forge 26.3, `wither21_textures.py --check`, `layers.py check`, `check_missing_textures.py`, Gen (Guides/Wiki/
  Discover) auf sb-test, `./gradlew check`.
- **Nicht testbar hier (keine Client-Läufe):** sichtbares Feuer/Opal im Tooltip und das Modell im Spiel → Vorschau-HTML (gleiche
  Farbfunktionen in JS, Modell aus dem echten JSON gezeichnet) + Besitzer-Client-Abnahme. Mixin-Ziel per javap gegen die 26.3-Jars
  (Fabric merged, NeoForge patched) geprüft.
