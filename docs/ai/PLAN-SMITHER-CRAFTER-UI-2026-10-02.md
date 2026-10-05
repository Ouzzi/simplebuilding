# Auto Smither im Crafter-Stil

## Bestand und Umsetzung
Der Auto Smither nutzt aktuell den Schmiedetisch-Hintergrund, drei Eingaben bei
(8/26/44,48), Ausgabe (98,48) und Inventar (8,84). Ausgabe- und Trichterregeln
bleiben erhalten. Den Vanilla-Crafter-Hintergrund aus dem installierten 26.3-Jar
verwenden, dessen 3x3-Feld durch drei horizontale Eingaben ersetzen. Den großen
Ergebnisrahmen und mittleren Pfeil übernehmen, Titel dynamisch zentrieren.
Slotkoordinaten im Menu exakt an Textur und Geisterbilder ausrichten.
Fehlerpfeil weiterhin nur bei vollständigem ungültigem Rezept.

## Dateien und Verifikation
AutoSmitherScreen.java, AutoSmitherMenu.java, auto_smither_gui.py und GUI-PNG;
bestehenden Menütest um Koordinaten ergänzen. Generator prüft alle Slotfelder.
Vorher/Nachher-Vorschau unter previews/auto-smither-crafter-ui-vorschau.png.
Bestehende Auto-Smither-Tests auf allen drei 26.3-Loadern, volles Server-Gate,
Gradle check -q, 26.2-Compile, Wiki --all und --all --check. Keine Clienttests.
Risiko: Crafter-Ausgabe ist optisch größer als der 16x16-Itembereich; dieser muss
zentriert bleiben. Eigener Commit auf aktuellem Branch, niemals Push.

## Abgleich und Vorprüfungen
- CrafterScreen fehlt als Java-Datei in mcsrc263; `javap -c -p` aus dem lokalen
  26.3-Client-Jar bestätigt dynamische Titelzentrierung. CrafterMenu.java bestätigt
  mittlere Eingabezeile (26/44/62,35), Ausgabe (134,35), Inventar (8,84).
- Hintergrund aus `gui/container/crafter.png`; obere/untere Eingabereihe entfernt,
  mittlere Reihe und großer Ergebnisrahmen erhalten. Das unversorgte Crafter-Symbol
  steht wie in der Besitzerreferenz in der Mitte; Fehlerpfeil bei (91,33), 28x21.
- Bestehende Geisterbilder bleiben rezeptabhängig: Vorlagenicon immer, Basis und
  Material gemäß gewählter Schmiedevorlage. Ausgabe-/Trichterregeln unverändert.
- Generator liest die Slotkoordinaten aus dem Menu und prüft die Texturfelder:
  `Auto Smither GUI: Crafter pixels and 40 menu slot fields OK`.
- 16x-Vorschau visuell geprüft, beschriftete Layoutmontage mit Beispiel-Icons und
  Ersatzschrift, kein Client-Screenshot:
  `C:/Users/o_o/code/minecraft-mods/previews/auto-smither-crafter-ui-vorschau.png`.
- Fabric-/NeoForge-26.2-Compile: `COMPILE262_EXIT=0`.
- Änderungen am Gameplay sind nicht erforderlich; AUTO_SMITHER-Gating und
  Forge-SidedInvWrapper bleiben erhalten. Sichtabnahme im Spiel bleibt offen.
- Gemeinsame finale Wiki-Prüfung: `WIKI_GENERATE_EXIT=0`, `WIKI_CHECK_EXIT=0`,
  `wiki: up to date, everything documented.` Die bestehenden Auto-Smither-Texte
  über Ausgabe und Fehleranzeige gelten unverändert.

## Vollständige Serverprüfung
Lauf `2026-10-05T06-49-02Z-cb38`: Fabric 960/960, NeoForge 960/960,
Forge 961/961; `alles gruen: 2881/2881 bestanden, 0 rot`.
Auf jedem Loader bestanden: `auto_smither_smiths_once_per_pulse`,
`auto_smither_output_capacity_and_recipe_error`, `auto_smither_output_rejects_insertion`
und `auto_smither_sorts_hopper_input`. Damit bleiben alle Klickwege, nur entnehmbare
Ausgabe, echte Trichtertransfers einschließlich Forge-SidedInvWrapper, Kapazität,
Rezeptfehler und die neuen Slotkoordinaten abgesichert. Testzentralen-Neubau und
vollständige Item-/Blockabdeckung bestanden auf allen drei Loadern.
Besitzerwelt unberührt, keine Clienttests und kein Push.

## Abschluss-Gate
`gradlew.bat check -q`: `GATE_EXIT=0`, einschließlich 55 Wiki-Unit-Tests (`OK`),
Balance-, Atlas-, Jade- und Modulprüfungen. Vollständiges Log: `.ai-runs/checkui-gate.log`.
Keine Quelländerung unter mc1_21_11 oder mc26_4. Die beiden Aufgaben wurden gemeinsam
im finalen Arbeitsstand geprüft und werden in zwei fachlich getrennten Commits abgelegt.
