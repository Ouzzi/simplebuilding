# Plan: Texturvorschläge + Weisheitserz, Erzdetektor-Nadel, Money-Schmelzzeit (2026-10-02, Branch claude-texprop)

Quelle: `.claude/QUEUE.md`, Queue-Ende 2026-10-02 abends. Regeln: wave2-rules (nur committen, kein Push).

## Ist-Zustand
- Enderit-Nugget `src/main/resources/.../item/enderite_nugget.png`: tropfenförmig, passt nicht zu den Vanilla-Nuggets.
  Barren `enderite_ingot.png` hat die Netherit-Barren-Form mit Lavendel-Rampe und rosa Glanzpunkten.
- Besatzvorlagen `glowing/pulsating/emitting_trim_template.png` (src/main). Zutaten: Leuchttintenbeutel, Echoscherbe,
  Glowstone-Staub (Kette Glowing → Emitting).
- Netherit-/Enderit-Apfel und -Karotte in src/main (Vanilla-Form, umgefärbt).
- Simple Money `special_fiber.png` (Kupfer/Gold/Diamant/Amethyst), `resin_fiber.png` (Harz, Honigwabe, Eisen, Knochenmehl).
- Weisheitserz und Tiefenschiefer-Weisheitserz sind **statisch** (16×16, kein mcmeta). Animiert ist nur die
  Weisheitskugel `sage_orb.png` (12 Frames, frametime 1, interpoliert; round3_settled).
- Erzdetektor (26.3-Overlay `item/detector_dial.png`, `detector_needle_00..31.png`, `detector.png`) aus
  `proposals_v5.detector_needle` über `round5_settled`. Befund: Ziffernblatt ist symmetrisch um x = 7,5
  (Rand A/B links, H/F rechts, Verschluss CC auf x = 7/8), Drehpunkt/Nabe aber auf x = 7. Nadel links bis x = 3
  (1 px vor dem Rand), rechts bis x = 11 (2 px vor dem Rand), Seitenmarken auf x = 2 (Rand) und 12 (1 px innen)
  → alles um ½ px nach links versetzt. Vertikal (Zeilen 4..10, Mitte 7) stimmt es.
- Money: einziges Ofenrezept `money_bill_from_blasting` (roher Schein → Geldschein), 10000 Ticks, 20 XP; Test in
  `MoneyTests` pinnt 10000; Doku `docs/modules/simplemoney.md`, Wiki-Prosa `modules/simplemoney/wiki/manual.json`.
  SimpleBuilding-Schmelzöfen (`FurnaceTierPerks`): verstärkt ×2, Netherit ×4, Enderit ×8 Tempo, Zusatzticks ohne
  Brennstoff, Netherit/Enderit doppelte XP.

## Vorschläge (nur Generator + Vorschau, nicht einbauen)
Gemeinsamer Helfer `tools/textures/proposal_sheet_2026_10_02.py` (Rampen aus Vanilla/Mod-Texturen, Umfärben per
Helligkeitsrang, beschriftete Vorschau A–J mit Referenzzeile, 10× hochskaliert).
1. `enderite_nugget_proposals_2026_10_02.py` → `previews/enderit-nugget-vorschau.png` (10 Nuggets).
2. `trim_template_proposals_2026_10_02.py` → `previews/besatzvorlagen-vorschau.png` (10 Sätze × 3 Vorlagen,
   Grundform je Satz aus einer Vanilla-Besatzvorlage, Farbe/Akzent je Zutat).
3. `food_proposals_2026_10_02.py` → `previews/netherit-enderit-essen-vorschau.png` (10 Sätze × 4).
4. `money_fiber_proposals_2026_10_02.py` → `previews/money-fasern-vorschau.png` (10 Spezialfaser, 10 Harzfaser).

## Direkt umsetzen
5. Weisheitserz: Annahme (Erz ist statisch): „kleiner" = Erzmuster der beiden Erze kleiner (äußere/dunkelste
   Speck-Pixel ausdünnen, ca. ¼ weniger Fläche), „langsamer animieren" = die einzige Sage-Animation, die
   Weisheitskugel, frametime 1 → 2 (Puls 24 statt 12 Ticks; Abweichung vom ersten Entwurf „3“: „etwas“ langsamer). Generator `sage_ore_smaller_2026_10_02.py`
   (liest aktuellen Stand, schreibt Overlay + `previews/weisheitserz-vorher-nachher.png`).
   Wiki-Texturkopien (`wiki/assets/...`) über `wiki/generate.py --all`.
6. Erzdetektor: Nabe zweispaltig (x = 7 und 8), Nadel um (7,5 | 7) gezeichnet (Spaltenwahl floor(8 + dx·L)),
   Länge so, dass Spitzen links x = 3 / rechts x = 12 / oben y = 4 / unten y = 10 liegen, Marken (2,7),(13,7),(7..8,4),(7..8,10).
   Änderung in `proposals_v5` (detector_dial/detector_needle, damit round5 dieselben Pixel erzeugt) + Nachfolger
   `ore_detector_centred_2026_10_02.py`, der nur Detektor-Dateien schreibt + `previews/erzdetektor-vorher-nachher.png`.
   Spiegelprüfung: Frame f und 32−f müssen horizontal um 7,5 gespiegelt sein (außer senkrechten Frames 0/16).
7. Money: cookingtime 10000 → 24000 (Begründung: Vanilla-Schmelzofen 1 Schein je Spieltag; SB-Öfen 2/4/8 je Tag,
   Enderit-Schmelzofen damit 3000 Ticks = 2,5 min statt 62,5 s; 8 Scheine/Tag/Enderit-Ofen ~ ein günstiges
   Angebot, Linkbudget 8 Käufe/Tag). XP bleibt 20. Dateien: Rezept-JSON, MoneyTests, docs/modules/simplemoney.md,
   manual.json, Wiki neu generieren.

## Risiken
- Wiki-Generator-Check und Datenprüfer (`modules/simplemoney/tools/check_data.py`) müssen grün bleiben.
- Detektor: Item-Modelle referenzieren unveränderte Dateinamen → nur Pixel ändern sich.

## Verifikation
Spiegelprüfung Detektor (Skript), Vorschauen ansehen, `check_data.py`, Wiki `--all` + `--check`, Testrunner
`fabric-263`, `module-simplemoney-fabric-263`, `neoforge-263`, Compiles 26.2 + Forge 26.3.

## Nachtrag (Besitzer, während der Umsetzung): Erzdetektor-Nadel breiter + Auswahl-Schimmer auf der Nadel
- Ist: Der Auswahl-Schimmer (`client/render/OreDetectorGlint`, aus `ItemDecorationsMixin`) läuft bei kalibriertem
  Detektor als 1-px-Funke mit 2 Schweifpixeln um den Rand des 16×16-Felds (Farbe = `OreDetectorItem#targetColor`).
  Clienttest `HudAndTooltipClientTest#oreDetectorGlintMarksTheCalibratedSlot` pinnt die Randpixel.
- Nadel breiter wie beim Bergungskompass: `proposals_v5.needle_path` zeichnet die Nadel als 4-verbundenen Strich
  (Füllpixel bei Diagonalschritten auf der Seite näher an der Ideallinie), Schweif 2 px. Weiter gespiegelt.
- Schimmer auf der Nadel: Generator schreibt die Kopfpixel je Bild (Nabe → Spitze) nach
  `common/src/shared/java/com/simplebuilding/client/render/OreDetectorNeedlePath.java`. `OreDetectorGlint` ermittelt
  das sichtbare Bild wie das Modell (ohne `lodestone_tracker` Ruhebild 16, sonst `CompassAngle(false, LODESTONE)`,
  gerundet auf 32) und lässt den Funken mit 150 ms/Schritt + 4 Schritte Pause darauf laufen. Randanimation entfällt.
- Damit Funke und Nadel exakt übereinstimmen: Itemmodell `CompassAngle` ohne Nachschwingen (wobble=false),
  Datagen 26.2 + 26.3 neu.
- Tests: neuer GameTest `ore_detector_game_test_selection_glimmer_path_follows_the_centred_needle` (Pfad beginnt an
  der Nabe, lückenlos, auf dem Ziffernblatt, gespiegelt, Spitzen der Hauptrichtungen); Clienttest auf Nadelpixel
  umgestellt (Clienttests laufen nicht im Standard-Gate – nur kompiliert).
- Risiko: Kompassnadel schwingt im Inventar/der Hand nicht mehr nach (bewusst).

## Nachtrag 2 (Besitzer): Ruhepuls der Nadel
- Solange nichts gefunden/gewählt ist, zeigt das Modell das Ruhebild `item/detector.png`. Dieses wird ein
  2-Frame-Streifen (Nadel gedimmt 0,6 ↔ 0,85) mit `detector.png.mcmeta` (frametime 16, interpolate) → sanftes
  Pulsieren alle 1,6 s, vanilla-nah wie animierte Items. Vorschau: previews/erzdetektor-ruhepuls.png/.gif.
- Annahme: Ein kalibrierter Detektor ohne Fund zeigt ebenfalls das Ruhebild; dort liegt der Auswahl-Schimmer über
  dem Puls (eine Modellbedingung „kalibriert“ gibt es ohne neue Item-Eigenschaft nicht; bewusst nicht gebaut).
- 26.2-Linie (src/main-Texturen, älteres Nadeldesign) bleibt bis zum Port-Run unverändert; der Schimmerpfad gilt für
  die 26.3-Nadeln.

## Nachtrag 3 (Besitzer, 2026-10-02 spät)
- Erzdetektor senkrecht mittig: Vanilla-Bergungskompass baut sein Ziffernblatt um EIN Drehpixel (Kreuz-Nabe bei
  x = 8) und führt die senkrechte Nadel in dieser Spalte, mit dunklerem Seitenpixel am Fuß. Unser Ziffernblatt ist
  symmetrisch um x = 7,5, daher übernehmen wir das Prinzip „Nadel liegt auf der Drehmitte" so: senkrecht (Frames 0
  und 16) ist die Nadel 2 px breit (Spalten 7+8, rechte Spalte eine Stufe dunkler), Schweif ebenso. Schimmerpfad
  `OreDetectorNeedlePath` jetzt schrittweise (x, y, Schritt; senkrecht 2 Pixel je Schritt); GameTest prüft zusätzlich
  die Mittigkeit der Frames 0/16. Vorschau previews/erzdetektor-v2-vorher-nachher.png.
- Früchte EINGEBAUT: Netherit-Apfel/-Karotte = Satz A; Enderit = Umfärbung davon, Glimmer über das bestehende
  Enderit-Glimmer-System von `generate_textures.py` (Vorlagen in tools/textures/hand/, neue Punkte). Glimmer ist
  statisch (keine mcmeta). Verzauberte Äpfel nutzen dieselben Texturen. 1.21.11-Kopien bleiben (Port-Run).
  Generator `foods_settled_2026_10_02.py`, Vorschau previews/netherit-enderit-essen-eingebaut.png.
- Weitere Vorschläge (nicht eingebaut): Enderit-Nugget v2, Besatz-Hintergründe + -Motive, Money-Fasern v2.

## Nachtrag 4 (Besitzer, Runde 3)
- Enderit-Nugget EINGEBAUT: Runde-1-Vorschlag G (halber Enderitbarren als Klumpen) mit geschlossener rechter Spitze
  (überstehendes Pixel entfernt, dunkler Rand x = 11 wie links, helle Pixel davor eine Stufe dunkler). Quelle ist
  `generate_textures.py` (ENDERITE_NUGGET, nur Hauptbaum); Prüfung + Vorschau `enderite_nugget_settled_2026_10_02.py`.
  Offen: 1.21.11-Kopie (Port-Run); Pfeilspitzen-Texturen aus `arrow_part_textures.py` leiten sich vom Nugget ab und
  wurden nicht neu erzeugt.
- Besatzvorlagen Runde 3 (Vorschlag): Hintergrund A für alle drei; Motive aus den Originaltexturen (Glowing/Emitting
  vom Besitzer, Januar; Pulsating aktuelle Textur) pixelweise klassifiziert und übertragen; Pulsating dezenter mit
  mehr Glimmer.
- Fasern Runde 3 (Vorschlag): aus dem Vanilla-Faden (Strang 1 px, zwei helle Töne, Schatten darunter) abgeleitet.

## Nachtrag 5 (Besitzer, 2026-10-03): eigene Motive eingebaut
- Drei Resprite-Screenshots: Leinwand am Rahmen (rgb 34,34,34) gefunden, 16x16 Zellen, Median der Zellmitte,
  Schachbrett (192/128 grau) = transparent (`owner_canvas_2026_10_03.py`); Ergebnis in tools/textures/hand/owner/.
- Zuordnung: Bild 1 (goldenes Kreuz mit Strahlen über grauem Gitter) = Emitting wie Original 1c6980c4; Bild 2
  (cyan Glimmer) = Pulsating; Bild 3 (goldene Flecken ohne Strahlen) = Glowing.
- Motiv = alle Pixel außer den vier Hintergrundfarben der Leinwand (Umriss + drei Navy-Töne); 1:1 auf Hintergrund A
  gesetzt, kein Pixel außerhalb der A-Silhouette. Eingebaut in src/main und die 1.21.11-Kopie; Pulsating nicht mehr
  aus der Pixelkarte von generate_textures.py (dort jetzt hand_drawn).

## Nachtrag 6 (Besitzer 2026-10-03 abends)
- Besatzvorlagen: zweite Besitzer-Leinwände 1:1 (eigener Hintergrund) eingebaut, Animation nur auf seinen Motivpixeln.
- Raw Enderite Scrap: Besitzer-Textur, Form 1:1, Farben Rang für Rang auf die 10-stufige Enderit-Schrott-Rampe.
- Blaupause eingebaut (Kartenblatt): frisch = B, bearbeitet = C, signiert = C dunkler + bisheriges Siegel.
- Astralit/Nihilith-Material: 10 Vorschläge (nicht eingebaut).
- Alternativblöcke (eingebaut): je 3 (veined/crystalline/layered Astralit, veined/crystalline/frosted Nihilith),
  Eigenschaften vom Grundblock. Gewinnung: Enderit-Meißel verlängert die Palettenkette linear (… gemeißelte
  Ziegel → Grundblock → A → B → C; Spachtel zurück). ABWEICHUNG: kein Meißel-Ring C → Grundblock, weil der
  Rückweg Grundblock → gemeißelte Ziegel sonst überschrieben würde (Reversibilitäts-Test). Quadrat-Rezepte
  A → 4 B → 4 C → 4 Grundblock (Grundblock → A per Steinmetz 1:1, sein Quadrat bleibt → 4 poliert).
  Tags (Spitzhacke), Beute (sich selbst), Modelle, Lang EN/DE beide Orte, Kreativ-Zeile unter dem Grundblock,
  Suchreiter, Wiki (end_building_blocks), Tests: ChiselTests (Kette + festgenagelte Tabelle),
  DataIntegrity (Reiter-Zeilen, Steinmetz, neuer Test end_alternates_follow_their_base_block). Kein Flag nötig
  (normale Blöcke, 26.2 und 26.3).
