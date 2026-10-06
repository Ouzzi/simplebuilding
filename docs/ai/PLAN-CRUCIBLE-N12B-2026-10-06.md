# Plan Crucible N12b (Besitzer-Feedback zu N12, 2026-10-06)

Branch `claude-crucible5` (auf 041b4bec6, bereits in claude-wave1 gemergt). Referenzen `previews/refs-n12/`.

## Messung der Referenzen (Pixel)
- **Bild 4** (1 GUI-Pixel = ca. 10,9 Bildpixel; Slot 197 px = 18 GUI-px): Rahmen oben/seitlich 5 px = Umriss 1
  (fast schwarz) + Fase 1 (~0,82 x Flaeche) + dunkles Band 2 (~0,64 x) + helle Innenlinie 1 (~1,23 x) + Flaeche (132).
  Unten 7 px: Innenlinie 1, Band 2, Fase 1, **Schatten 2 (~0,36-0,40 x)**, Umriss. Ecken: Aussenkontur 2-px-Treppe,
  Innenlinie um 1 px eingekerbt. Slot: dunkle Oberkante 1 (48), dunkle Linke 1 (78), Flaeche (108), helle Kante
  unten/rechts 1 (162), eckig.
- **Bild 3** (ca. 2,08 px je GUI-px, Slot-Raster 37,5 px): Kasten mit dunklem Umriss 1, heller Innenlinie 1, unten
  2 px dunkleres Schattenband, Ecken ca. 2 px rund. Slots 16 px, 2 px Fuge, Ecken 1 px rund, dunkle Oberkante 1 px.

## Umsetzung
1. `CrucibleScreen.box`: Rahmen nach Bild 4 (5/7 px, Ecken 2 px: Zeilenkuerzung 3,1,1 aussen), Farben aus der
   Stufenfarbe abgeleitet (Faktoren oben); Slots nach Bild 3 (16 px rund, 2 px Fuge) mit Bild-4-Einlassung (dunkle
   Ober- und Linkskante, helle Kante unten/rechts). Layout: `FIRE_ROOM` 12, Inventar-Kasten 100 px (Label +6, Slots +17).
   Fass-Felder in einem duennen Innenkasten. Heiz-Slot-Variante (HEAT_STYLE) entfernt: Besitzer behaelt A.
2. `CrucibleFlames`: drei Ruhestufen - hoch/extrem lebhaft wie N12; mittel niedrig (1/6,5), langsam, ohne hellgelbes
   Band, wenige Funken; Nachgluehen (Quelle weg, `afterglow > 0`) = "niedrig": nur Glut + 3-5 px rote Glimmzungen.
3. Fortschritt: `CrucibleScreen.PROGRESS_STYLE` FILL (V1, eingebaut: Slot fuellt sich wie Ofen-Slot in Bild 4 hinter
   dem Item, blau/still bei zu kalt, rot voll bei blockiert, gruener Rahmen bei fertig) / GAP_BAR (V2: senkrechter
   2-px-Balken in der Fuge rechts). Randbalken aus N12 entfernt.
4. Keramik-Eimer: Steinzeug-Toene (creme/warmgrau) statt Terrakotta; 4 Abnutzungsstufen = 4 Kupfer-Oxidationsstufen
   (0..3), je Stufe eigenes Item je Fuellung (leer/Wasser/Lava): intakt, angeschlagen (chipped), rissig (cracked),
   bruechig (brittle). 32 Einsaetze, 8 je Stufe, Zaehler im versteckten Component `simplebuilding:ceramic_uses` (kein
   Haltbarkeitsbalken), letzter Einsatz zerbricht. Fuellung bleibt beim Stufenwechsel. Kreativtab nur intakt (roh,
   leer, Wasser, Lava); abgenutzte Stufen nur durch Benutzung, in JEI mit eigener Info, in Eimerliste (Spender, Suche).
   Tooltip: verbleibende Einsaetze.

## Entscheidungen
- "Separate Items je Stufe" wie verlangt (Kupfer selbst nutzt Component + Modellwahl; Stufenzahl von dort uebernommen).
- Altes Haltbarkeits-Damage alter Keramik-Eimer wird ignoriert (Eimer zaehlen ab 0) - unkritisch.
- V1-Fuellung liegt hinter dem Item (wie Ofen); bei grossen Items ist sie nur am Rand sichtbar, Eckzeichen bleiben.

## Verifikation
Testrunner fabric/neoforge/forge-263 + simplelib/simplesandwiches, `check`, Compiles, Datagen, Wiki; GameTest
`crucible_game_test_ceramic_bucket_wears_through_stages_and_keeps_its_filling`. Vorschauen crucible-n12b-*.
