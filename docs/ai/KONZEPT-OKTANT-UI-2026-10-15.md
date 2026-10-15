# Konzept Oktant-Manager-UI und HUD-Overlay (N64, 2026-10-15)

Status: nur Konzept + Vorschauen, nichts am Spiel geändert. Umsetzung erst nach Besitzer-Freigabe.
Vorschauen: `docs/ai/previews/claude-octantui/` (Skript `tools/textures/octant_ui_preview.py`, baut `UiBoxes.box`,
`UiPalette.derived` und `HudPanel` pixelgenau nach; Hintergrund und Schrift synthetisch, Schrift weicht minimal von Minecraft ab).

## 1. Ist-Zustand

- Screen `common/src/shared/java/com/simplebuilding/client/gui/OctantScreen.java`: Kasten per `ModScreenStyle.panel(..., ModScreenLayout.OCTANT_FILL, ...)`
  (26.3, `mc26_3/overlay/.../ModScreenStyle.java`) -> `UiBoxes.box` aus SimpleLib (`modules/simplelib/.../ui/UiBoxes.java`). Keine Textur, nur `fill`-Rechtecke.
  Ohne Stil (26.2/Toggle aus) Vanilla-Sprite `popup/background`. Füllfarbe `ModScreenLayout.OCTANT_FILL = 0xFFA0642E` (Messing/Orange),
  Palette `UiPalette.derived`: light 1.27x, Band 0.64x, Bevel 0.82x, Schatten 0.40x, Label dunkel (Kontrast 4,2:1).
  Zusammenfassungsfeld: `ModScreenStyle.field` (INVENTORY-Grau #C6C6C6, Schrift #404040).
- Overlay: `RangefinderHudOverlay` -> `HudPanel.draw/background` -> Vanilla-Tooltip (`TooltipRenderUtil`, Hintergrund #F0100010, violetter Rahmen,
  3 px Rahmen + 6 px Padding). Werte orange: `guiDrawHelper.getColorTheme` ORANGE = #FFAD33 (Pos 1/Ergebnis), #FF7F00 (Pos 2); `HudPanel.COLOR_VALUE = #FF7F4C`.
  Gauge und Rod-Anzeige teilen sich dieselbe Hülle (`HudPanel`), jede Änderung dort wirkt auf alle drei.
- Helligkeit: `UiBrightness` (light/dawn/dark) wirkt zentral in `UiPalette`; neue Farben laufen automatisch durch.

## 2. Ursache des Eckpixels (genau)

`UiBoxes.box` zeichnet über `rounded()` mehrere Schichten mit festen Eck-Kürzungen: `CUT_OUTER = {3,1,1}`, `CUT_BEVEL = {2,1}`, `CUT_INNER = {1}`.
Zwei Fehler, in der Screenshot-Ecke (unten links) sichtbar, nachgebaut in `ecke-detail.png` (links = Besitzer-Bild reproduziert):
1. Die Außenlinie (Rim) `{3,1,1}` ergibt die Silhouette 3,1,1,0 statt 3,2,1,0 - der Rahmen springt 2 px quer, dann 2 Zeilen senkrecht: keine Treppe.
2. Unten wird der Bevel (Zeile y+h-5, Spalte x+2) mit denselben `{2,1}`-Kürzungen gezeichnet wie oben, sitzt aber 2 px höher als der Schatten
   (`s = 2`): ein einzelnes Bevel-Pixel steht frei neben der dunklen Schatten-Spalte. Das ist das "Pixel zu viel".
Betrifft alle Kästen, die `UiBoxes.box` nutzen (Container, Truhen, Tiegel, Rucksack...), nicht nur den Oktanten. Es ist Zeichencode, keine Textur.

## 3. Konzept

### (a) Sanftere Palette (Füllfarbe; alle Töne leitet `derived` ab)
| Variante | Fill | light (Innenlinie) | Label | Kontrast Label/Fill | Charakter |
|---|---|---|---|---|---|
| Vorher | #A0642E | #CB7D3A | #2E3034 | 4,2:1 | grelles Orange |
| A Sand | #B9A27E | #EACDA0 | #2E3034 | 5,4:1 | helles Messing-Beige, nah am Vanilla-Container-Grau, ruhigste Variante |
| B Bronze matt | #6F5F4B | #948879 | #F2EEE8 | 5,3:1 | dunkel, gedämpft, Overlay und Screen wirken wie aus einem Guss |
| C Salbei | #7F8F8A | #A1B5AF | #2E3034 | 3,9:1 | grau-grünlich, passt zu Reinforced/Iron-Paletten, verliert Messing-Bezug |
Empfehlung A (Kontrast, Konsistenz mit hellem Inventar-Stil); B, wenn der Besitzer "dark" mag. C nur, wenn Messing ausdrücklich egal ist.
Dazu: Pos-1/Pos-2-Farben im HUD nicht mehr Orange-Theme, sondern aus der Palette (siehe c); die Zahlen-Farbthemen der Schablone bleiben unberührt.

### (b) Eck-Fix (in `UiBoxes`, SimpleLib)
- `CUT_OUTER = {3,2,1}` (Rim wird 45-Grad-Treppe).
- `rounded()` bekommt einen Überlader mit eigener Unterkante; bei Schatten (`shadow = true`) Bevel unten `{3,2,1}`, Band unten `{2,1}`; oben unverändert.
- Ergebnis `ecke-detail.png` rechts: durchgehende Diagonale, kein loses Pixel, keine 2-px-Sprünge. Wirkt auf alle Kästen (gewollt, einheitlich); Kontrolle per
  Client-Screenshots der vorhandenen Container-Tests, Slot-Maße ändern sich nicht (nur Eckpixel).

### (c) Overlay schlank im neuen Stil
Gleiche Anordnung (Titel, Pos 1, Pos 2, Ergebnis, Maße in Klammern, gleicher Anker/Stapel). Änderungen:
- Hülle: kein Tooltip-Violett, kein 3-px-Außenrahmen; 1-px-Rahmen in Band-Farbe (0.64 x Fill), 4 px Padding statt 6+3, Titelabstand 3 px, Ecke 1 px geschnitten. Etwa 25 % kleiner.
- Hintergrund: Fill x 0.20 (nahezu schwarz mit Variantenton), Deckkraft #C8 (78 %) statt #F0 (94 %); Vorschlag Config-Regler 50-100 %.
- Schrift: Titel weiß, Pos-1/Ergebnis helle Palettenfarbe (light, 35 % zu Weiß), Pos 2 etwas dunkler, Nebenzeilen #B8B8B8; Schatten bleibt.
- Kontrast (WCAG) Weiß/Wert auf effektivem Hintergrund (Variante A): dunkle Welt Titel 16,8:1, Wert 12,7:1; über rein weißem Himmel (Worst Case) Titel 8,0:1, Wert 6,1:1 - alle Zeilen >= 4,0:1 (Nebenzeile), Hauptzeilen >= 5,5:1.
  Bronze: Wert 4,5:1 und Pos 2 3,2:1 im Worst Case (Werte müssten heller gewählt werden); Salbei 4,4:1. Vorher: Orange #FF7F00 auf #100010 = 8,1:1 (guter Kontrast, aber unruhig).
- Gilt per gemeinsamer `HudPanel.background` automatisch auch für Messuhr und Resonanzstab (Besitzer-Regel "alle Overlays ein Stil"); für den Oktanten allein wäre ein Parameter nötig.

### (d) Änderungen je Datei, Aufwand, Risiko
| Datei | Änderung | Aufwand |
|---|---|---|
| `modules/simplelib/shared/java/com/simplelib/api/client/ui/UiBoxes.java` | `CUT_OUTER`, `rounded`-Überlader, Bevel/Band-Unterkante | klein |
| `common/.../client/gui/ModScreenLayout.java` | `OCTANT_FILL` neue Farbe (1 Zeile) | trivial |
| `common/.../client/gui/HudPanel.java` | `background()` eigener flacher Kasten statt `TooltipRenderUtil`, Konstanten PADDING/TITLE_GAP/COLOR_VALUE | klein-mittel |
| `common/.../client/gui/RangefinderHudOverlay.java` | Farben aus Palette statt `getColorTheme` | klein |
| optional `ModHud`/Client-Config | Deckkraft-Regler | klein |
| `tools/ui/simpleinterfaces_preview.py` | Eck-Regel dort synchron halten (Kommentar in `UiPalette.derived` verlangt das) | klein |
| Tests | Client-Screenshots Container (Eck-Änderung), `HudAndTooltipClientTest`, JUnit Layout | mittel (Gate) |
Risiken: Eck-Fix verändert optisch alle Kästen (sichtbar, aber gewollt); 26.2/1.21.11 behalten alten Zeichencode, falls SimpleLib dort geteilt wird, Verhalten prüfen;
Messuhr/Stab ändern sich mit; Schriftfarben bei Variante B/C im Worst Case knapp (siehe Kontrast).

## 4. Vorschaubilder (`docs/ai/previews/claude-octantui/`)
`ecke-detail.png` (Eckfehler vorher/nachher, 14x), `screen-A-sand.png`, `screen-B-bronze.png`, `screen-C-salbei.png` (Screen vorher/nachher, 3x),
`overlay-A-sand.png`, `overlay-B-bronze.png`, `overlay-C-salbei.png` (HUD vorher/nachher, 4x).

## 5. Fragen an den Besitzer
1. Welche Palette: A Sand, B Bronze matt, C Salbei (oder Wunschfarbe)?
2. Eck-Fix für alle Kästen der Mod (Empfehlung) oder nur für den Oktanten?
3. Overlay-Stil auch für Messuhr und Resonanzstab (einheitlich, Empfehlung) oder nur Oktant?
4. Deckkraft fest 78 % oder einstellbar (Config-Regler)?
5. Pos-1/Pos-2-Unterscheidung im HUD nur über Helligkeit derselben Farbe okay, oder weiterhin zwei Farbtöne?

## 6. Antworten Besitzer (2026-10-15, verbindlich)
O1 Palette B Bronze matt (#6F5F4B). O2 Eck-Fix für ALLE Kästen der Mod (UiBoxes, betrifft alle UIs mit diesem Rahmen). O3 neuer Overlay-Stil einheitlich auch für Messuhr und Resonanzstab (HudPanel). O4 Deckkraft fest 78 %. O5 Pos 1/Pos 2 über Helligkeit derselben Farbe – dabei die Lesbarkeit sichern (Konzept nennt für B Pos 2 nur 3,2:1 im schlechtesten Fall → Helligkeitsstufen so wählen, dass beide Zeilen gut lesbar bleiben).
