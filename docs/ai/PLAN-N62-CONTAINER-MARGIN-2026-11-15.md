# N62 – feste Container-Margin (Besitzer 15.11. ~12:40 UTC)

Regel: siehe `docs/ai/PRINZIPIEN.md` A7 „Container-Margin“. Vorschau `/root/sb-tools-b/previews/n62.html`
(Generator `/root/sb-tools-b/ig57u/mock/render3.py` + `html_n62.py`, Maße pro Bild nachgezählt in `previews/n62/measures.json`).
Basis: b-ig57u2 (Disc-Press-Fix N57-10 bleibt), ersetzt dessen AIR/NO_SHADOW-Regel.

## Maße (M = 3)
- Slot = 16×16 (dunkle Kante oben/links innen) + 1 px Lichtkante rechts/unten → Fläche zwischen heller Innenlinie (Rahmen 5, unten 7 mit Schatten) und Slot = M.
- Breite: links itemX − 8, rechts itemMaxX + 17 + 3 + 5 → Vanilla-Kiste 0..177 (1 px über die 176er Bildbreite hinaus, keine Klemmung auf imageWidth mehr).
- Container-Kasten: oben Bildrand (mit Titel) bzw. erster Slot − 8; unten letzter Slot + 17 + 3 + 7 (Schatten bleibt immer: nur noch Variante TWO_BOXES).
- Inventar-Kasten: oben erste Inventarreihe − 8, unten Hotbar + 17 + 3 + 7.
- Teiler ≥ 2 px; fehlt Platz → Verschiebung s = max(0, Containerunterkante + 2 − Inventaroberkante); Kiste/Fass/Doppelkiste/Mod-Fässer/Werfer/Trichter s = 6, Shulker s = 7; Bildhöhe +s+3.

## Umsetzung (nach Besitzer-OK zu N62-1..3)
1. `UiBoxes.MARGIN = 3` (simplelib) + Javadoc.
2. `BoxLayout.compute`: neue Rechnung, `Layout` bekommt `int shift` + `int height`; SEAM/NO_SHADOW entfallen (Enum-Werte bleiben, bis alle Aufrufer umgestellt sind).
3. Vanilla-Container (StorageBackgroundMixin/StyledScreens): in `init` HEAD Layout aus den Menü-Slots rechnen, Spieler-Slots über `SlotPositionAccessor` um s nach unten (einmal pro Menü – Original-y merken, Resize/Re-init darf nicht doppelt schieben), `imageHeight += s + 3` vor der Zentrierung (Muster `CartographyHeightMixin`); Inventar-Label bleibt versteckt.
4. Stationen (StationScreens/WorkScreens): gleiche Regel; Seiten bei mittigem Inhalt = Vanilla-Anordnung, oben/unten M.
5. Eigene Screens (simplestorage TieredChest/Controller/Pipe/StorageCrafting, simplemachines TierHopper/AutonomousCrafter/AutoSmither, simplemusic DiscPress): Menü-INVENTORY_Y/Slot-y direkt anpassen (Server+Client), Kasten über die gemeinsame Rechnung statt `box(..., false)`; StorageCrafting = verbundene Variante B (Fuge, 3 px zu Fuge/Rand).
   - Umgesetzt (Abweichung): Menüs/Server unverändert; `UiMargin` (simplelib) rechnet aus den Menü-Slots + Elementen (großer Ergebnis-Slot, Pfeil, Filter-Taste, Rohr-Tasten, Controller-Scrollbalken), Spieler-Slots rücken nur im Client-Menü (nur bei Stil an; Stil aus = Vanilla-Bild wie bisher). Geometrie je Modul in `StorageMargins`/`MachineMargins`/`PressMargins`, Gametests `screen_margins` (simplestorage, simplemachines) + `press_layout` (simplemusic). Shift/Höhe: Mod-Truhen 6/+8, Crafting Table 6/175, Controller 2/227, Rohr 5/150, Mod-Trichter 0/142, Autonomous Crafter 6/175, Auto Smither 0/169, Musikpresse 0/169. Inventar-Kasten immer eigene Breite (3 px), Container-Kasten über beide.
   - OFFEN: StorageCrafting Variante B (27 Kistenslots unter der Fuge) braucht Lager-Slots, die Menü/BlockEntity nicht haben (BE nur 9er-Raster, bewusst kein Container; Besitzerfrage IG57U „zieht das Raster aus den 27 Feldern?“ offen) – vorerst ein Eichenkasten nur mit Raster+Ergebnis nach der Regel.
6. Tests: ContainerTests neu (Kiste/Shulker/Trichter/Werfer: alle Ränder = M, shift-Werte), Pixel-Check im Client-Test (Fläche zählen wie render3.py) für Kiste/Shulker/Netherite-Fass.

## Prüfung (sb-test, einzeln)
- `/root/sbt b-n62 "… module-simpleinterfaces-fabric-263"`, danach simplestorage, simplemachines, simplemusic; Client-Screens Kiste/Shulker/Fass/Netherite-Fass mit Pixelzählung.
