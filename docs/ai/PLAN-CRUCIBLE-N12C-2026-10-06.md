# Plan Crucible N12c (Besitzer-Feedback zu N12b, 2026-10-06)

Branch `claude-crucible5`, zuerst `claude-wave1` (70f92c614) hineingemergt (Fast-Forward).

1. **Fass-Kasten**: bisher ein duenner Innenkasten (2-px-Rand, helle/dunkle Kante) im Tiegel-Kasten - rechts/unten
   wirkte er durch Schattenkante + Slot-Lichtkanten dicker. Jetzt ein eigener Kasten neben dem Tiegel-Kasten
   (BOX_GAP 2), mit genau demselben Rahmen wie alle Kaesten (`CrucibleScreen.box`: 5 px, unten 7 px mit 2 px
   Schatten, Ecken 2 px), Felder mit 8 px Rand wie das Tiegel-Raster. Der Tiegel-Kasten bleibt mindestens 176 breit
   (Platz fuer den Titel), der Inventar-Kasten spannt unter beiden.
2. **Fass-Felder = Tiegel-Plaetze** (6/9/18/27): Doku (Plan 2026-10-04, Antwort 59 "immer 9") wird durch die neue
   Besitzer-Vorgabe ersetzt. `CrucibleBarrelBlockEntity.getContainerSize()` angebracht = Plaetze des Tiegels nebenan
   (`attachedSlots()`, Fallback 6). Die Felder liegen im Menue in derselben Form wie das Tiegel-Raster.
   **Entscheidung Rest-Inhalt**: nicht mehr herauswerfen, sondern verwahren - Slots jenseits der Tiegel-Zahl sind
   angebracht unsichtbar (Trichter, Tiegel, Menue sehen nur die ersten), kommen beim Abnehmen zurueck, fallen beim
   Abbau. Begruendung: nichts geht verloren (auch kein Despawn von Boden-Items), Stufen-Upgrade des Tiegels gibt die
   Felder automatisch frei. Reservierungen jenseits der Grenze werden verworfen.
   JEI-Text/Wiki: "%s" = "6/9/18/27", Rest bleibt verwahrt. GameTest `LibTests.barrelAttach` angepasst
   (6 Felder, Menue 6 Fass-Felder, Rest bleibt, nach Abnehmen wieder da).
3. **Feuer breit**: `CrucibleFlames.SHAPE` = POINTED (eingebaut) / BROAD (Abstand 14, breite runde Zungen, niedriger).

Vorschauen: `previews/crucible-n12c-ui.png`, `crucible-n12c-feuer.gif` (Tool `tools/textures/crucible_n12c_2026_10_06.py`).
