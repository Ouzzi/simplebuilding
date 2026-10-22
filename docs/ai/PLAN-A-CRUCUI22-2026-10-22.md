# Plan a-crucui22 (SP22-6, Tiegel-Fenster: Feuerzone mit Herd-Trapez) – 22.10.2026

## Wunsch (Queue SP22-6)
Tiegel-Fenster, besonders der End-Tiegel: unten etwa 1 Block mehr Platz; im Hintergrund dieser Zone ein dunkleres
Trapez für räumliche Tiefe im Stil des Hintergrunds; das Feuer liegt darüber und ist mit mehr Abstand zu den untersten
Items gut sichtbar. Alle Tiegel-Varianten (Eisen, Verstärkt, Netherit, Enderit; mit/ohne Fass) gleich.

## Ist-Zustand
- Das Fenster ist **nicht texturiert**: `CrucibleScreen` zeichnet alles aus Flächen (`UiBoxes.box/slot`, `UiFlames`),
  keine GUI-PNGs. Es gibt also keine Textur-Dateien zu erzeugen; der Generator-Teil des Auftrags entfällt im Repo
  (Abweichung, s. u.), die Vorschau entsteht per Python-Skript.
- Layout: `CrucibleMenu.sectionHeight = GRID_TOP(18) + rows*18 + FIRE_ROOM(12)`; Slots, Fass-Box, Inventar und
  Bildhöhe leiten sich daraus ab (Menu und Screen nutzen beide `CrucibleMenu.layout`).
- Flammen: `CrucibleFlames.targetPixels(heat, glowing, section)` wächst mit der Boxhöhe (Enderit extrem ≈ 50 px)
  und steht hinter den unteren 1–3 Slot-Reihen, verdeckt also von Items.
- Vorschau-Werkzeug `tools/textures/crucible_fire_2026_10_12.py` spiegelt Layout + Flammen in Python.
- Test `barrelBesideInventoryCentred` verlangt Bildhöhe ≤ 240 (Enderit heute 222).

## Änderung
1. `CrucibleMenu.FIRE_ROOM` 12 → 30 (+18 px = genau eine Slot-Teilung). Enderit-Bild 240 px (= Grenze, passt).
   Slots/Inventar/Fass rücken automatisch mit (alles aus `layout`). Neu `FIRE_GAP = 3` (Abstand Flammenspitze zur
   untersten Slot-Reihe) und `fireZone()` am Layout (Höhe der freien Zone unter dem Raster).
2. `CrucibleFlames`:
   - `hearth(...)`: dunkles Trapez (Herdboden) in der Zone unter dem Raster, unten volle Innenbreite (sitzt auf der
     hellen Innenlinie des Rahmens), nach hinten je 2 Zeilen 1 px schmaler (Pixel-Treppe wie die Box-Ecken);
     Farben aus der Box-Palette wie der Rahmen: Rückkante 0,40 × Füllung, Schrägkanten 0,50, Fläche 0,52→0,66
     (hinten dunkler, vorne heller = Tiefe). Folgt damit Helligkeitsstufen (UiBrightness) automatisch.
   - `targetPixels` rechnet aus der Zonenhöhe statt der Boxhöhe: Raum = Zone − FIRE_GAP; extrem 0,70, hoch 0,65,
     mittel 0,35, Nachglühen ≥ 3 px. Höchste Zunge (×1,4) bleibt so unter der untersten Slot-Reihe – für alle Stufen gleich.
3. `CrucibleScreen`: Trapez zeichnen, dann Flammen darüber, dann Slots. Hitze-Tooltip-Bereich = ganze Feuerzone.
4. Fass-Box: bleibt gleich hoch (bündig), ohne Trapez (kein Feuer dort).
5. Test (`CrucibleTests.barrelBesideInventoryCentred`): Zone unter dem Raster ≥ 16 px je Stufe.
6. Vorschau: `tools/textures/crucible_hearth_2026_10_22.py` (reines Python/PIL, importiert die Bausteine aus
   `crucible_fire_2026_10_12.py`, dessen `main()` dafür hinter `__main__` kommt) erzeugt vorher/nachher je Stufe
   und Hitze, HTML unter `/root/sb-tools-a/previews/crucui22-preview.html` (nicht im Repo).

## Abweichung vom Auftrag
Keine GUI-Texturen/Ebenen-Dateien: der Tiegel-Screen ist bewusst texturlos (N12-Stil, `UiBoxes`), das Trapez wird
wie der Rest aus Flächen gezeichnet. `wiki/data/textures.js` ändert sich daher voraussichtlich nicht.

## Risiken
- Enderit-Fenster genau 240 px hoch: passt bei 854×480 und GUI-Skala 2 (Testgrenze).
- Kleinere Flammen als vorher (stehen jetzt frei statt hinter den Items) – Besitzer-Abnahme im Spiel.

## Verifikation
`/root/sbt a-crucui22`: Compile Fabric/NeoForge/Forge + Modul; Modul-Tests `module-simplecrucibles-fabric-263`,
`-neoforge-263`, `-standalone-fabric-263`; SB-Tests mit Filter `simplebuilding:*crucible*`; Generatoren
(module_guides, wiki --all, discover) + `./gradlew check -q`. Keine Client-Läufe.
