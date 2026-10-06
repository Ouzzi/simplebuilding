# Plan Crucible N12 (Besitzer-Feedback nach UI v3, 2026-10-06)

Branch `claude-crucible5` (Basis cddb27ae2). Referenzen `previews/refs-n12/` (Bild 1 Flammenstreifen, Bild 2 Lagerfeuer,
Bild 3 Container-Kaesten, Bild 4 Ofen-Slot mit Flammenfuellung).

## Ist-Zustand
- `CrucibleMenu` (simplelib, gemeinsam): Layout v3, ein Panel, Flammen-Raum `FIRE_ROOM` unter dem Raster.
- `CrucibleScreen`: Vanilla-artiges Panel, 18er-Slotrahmen, Kochfortschritt als Fuellung des ganzen Slots.
- `CrucibleFlames`: 2x2-Zellen-Saeulen (Wellen), rot/orange/gelb, Funken.
- Vorschau-Tool `tools/textures/crucible_ui_v3_preview_2026_10_06.py` spiegelt Layout + Flammen.
- Angedocktes Fass: `modules/simplelib/tools/gen_resources.py` (`DOCKED_BODY` x/z 1..15, Hoehe 12, Flansch z -1..1).
- Eimer: `ModBucketItem.Kind` COPPER/ENDERITE/IRON/CERAMIC; `filled(kind, fluid)` steuert Schoepfen, Spender,
  Kessel (Mixin) zentral. Keramik: nur Wasser (`CERAMIC -> null` fuer Lava, Kommentar "fired clay cracks in lava").

## Umsetzung
1. **Slot-Fortschritt** (Konstante `CrucibleScreen.PROGRESS_STYLE`): A = untere 2 Pixelreihen des Slots, ueber dem
   Item gezeichnet (eingebaut); B = gleiche Reihen hinter dem Item; C = in der 2-px-Fuge unter dem Slot. Zustaende
   (kalt/blockiert/fertig) ebenfalls nur als Randbalken; Eck-Markierungen bleiben (Farbenblinde), "kein Rezept" grau.
2. **Flammen** neu (Bild 1/2): 1-px-Zellen, einzelne spitze Zungen (Profil mit Spitze), Rand rot, darin rot-orange,
   orange, gelber Kern unten, unterste Reihe Glut (flackernde Kohle), Funken steigen, manche als kleines Plus.
   Extrem = gleiche Form in Blau. Python-Vorschau spiegelt den Algorithmus.
3. **GUI Bild 3/4**: zwei Kaesten – oben Tiegel-Kasten in Stufenfarbe (Eisen grau, Verstaerkt blaugruen, Netherit
   dunkelbraun, Enderit lila), unten heller Inventar-Kasten, 2 px Fuge dazwischen (= "Trennstrich"), abgerundete
   Kaesten mit dunklem Rand + dezenter Licht-/Schattenkante; Slots 16x16 abgerundet, eingelassen (dunkle Oberkante),
   2 px Fugen; Fass-Felder in einem eigenen Kupfer-Kasten. Menue-Positionen bleiben im 18er-Raster.
4. **Vorschlaege**: Heizanzeige-Konstante `CrucibleMenu.HEAT_STYLE`: BAND (Flammenstreifen hinter den Slots, eingebaut)
   oder SLOT (Bild 4: eingelassener Hitze-Slot links vom Raster, fuellt sich mit Flammen, blau bei extrem).
   Vorschau-PNGs `previews/crucible-n12-*.png` + `crucible-n12-flammen.gif`.
5. **Fass**: Koerper 13 x 11 x 13 (x 1.5..14.5, y 0..11, z 0..13 – 1 px naeher am Tiegel), Flansch z -1..0,
   Rinne passend auf neue Hoehe. Vorschau `crucible-n12-fass.png` (alt/neu).
6. **Keramik-Lavaeimer** `simplebuilding:ceramic_lava_bucket`: Entscheidung – gebrannter Ton haelt Lava (echte
   Schmelztiegel sind Keramik), gleiche Abnutzung wie Wasser (32 Benutzungen), Brennstoff wie Lava, aber ohne
   Rueckgabe (der Eimer verbrennt; kein Reparatur-Trick). Seelenlava/Milch weiter nein. Ueber `filled()` greifen
   Schoepfen, Spender und Kessel automatisch. Textur im Stil der Keramik-Eimer (Generator texture_round7),
   Modelle/Item-JSON 26.3-Overlay, Lang EN+DE beide Orte, Kreativtab, JEI-Info, Guide, Datenintegritaetstest, GameTest.

## Risiken
- Fortschritt ueber dem Item verdeckt die unterste Reihe der Stueckzahl leicht (wie Haltbarkeitsbalken) – bewusst.
- Python-Vorschau und Java muessen gleich bleiben (Kommentar in beiden).
- Layoutaenderung verschiebt Slots: keine Tests haengen an Koordinaten (geprueft).

## Verifikation
Testrunner fabric/neoforge-263 + simplelib/simplesandwiches-Module, `gradlew check -q`, Compiles 26.2/Forge 26.3,
Datagen 26.3, Wiki --all/--check, neuer GameTest Keramik-Lavaeimer. Kein Client – Sicht nur ueber Vorschau.

## Ergebnis / Abweichungen
- Flammen nach Sichtpruefung nachjustiert: Zungenabstand 10 px, Halbbreite 4.5-6, schiefe Zungen (eine Seite steiler),
  Spitzen 2 Reihen rot, gelber Kern nur tief innen; Python-Vorschau identisch.
- Fortschrittsbalken 14 px breit (innerhalb der abgerundeten Slot-Ecken), Zeilen y+14/y+15 des 16er-Slots.
- Keramik-Lavaeimer: Verbrennen ohne Rueckgabe (kein `use_remainder`), Pour nutzt nur ab (kein Bruch wie Kupfer).
- Vorschauen: crucible-n12-ui/-fortschritt/-flammen(.png/.gif)/-fass/-keramik-lavaeimer.png.
- Gates gruen: Testrunner fabric/neoforge-263 + simplelib/simplesandwiches je fabric/neoforge (2070/2070), check,
  Compile 26.2 + Forge 26.3, Datagen 26.3, Wiki --all/--check. Nicht getestet: Client-Sicht (kein Client gestartet).
