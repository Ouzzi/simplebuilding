# Plan: Farbpinsel (2026-10-09)

## Ursache / Ziel
SimpleBuilding besitzt bisher kein Werkzeug, das vorhandene farbige Baubloecke direkt
umfaerbt. Der Farbpinsel soll Farbe laden, Familienvarianten serverseitig ersetzen und
Blockzustand (z. B. Treppenrichtung und Wasser) soweit wie beim Zielblock moeglich erhalten.

## Entscheidungen
- Der Pinsel hat 256 Haltbarkeit und 8 Striche je Farbstoff.
- Kreativspieler verbrauchen weder Striche noch Haltbarkeit.
- Holzbeize ist ausdrücklich nicht enthalten; Holzblöcke bleiben unverändert.
- Farbspritzer und Pinselstrich-Sound werden serverseitig erzeugt.
- Die erste Implementierung nutzt Vanilla-/Registry-Familien und bleibt fuer Modfamilien
  ueber den vorgesehenen `simplebuilding:dyeable_families`-Tag erweiterbar.

## Aenderung
1. Loader-neutrales `ColorBrushItem` mit Farb-/Ladungs-Komponente, Laden, Waschen,
   Pipette, Faerben und Zustandsuebernahme.
2. Registrierung, Kreativtab, Modell-/Rezept-/Sprachdaten und Textur.
3. Shared GameTests und Fabric-Adapter fuer Laden, Faerben, Pipette, Waschen,
   Zustandserhalt, Verbrauch und Kreativmodus.
4. Wiki regenerieren und gezielten GameTest-Lauf ausfuehren.

## Dateien
`common/.../ColorBrushItem.java`, `ModItems.java`, `ModItemGroupsContent.java`,
`common/.../ColorBrushTests.java`, `src/.../ColorBrushGameTest.java`,
`src/main/java/com/simplebuilding/datagen/ModRecipeProvider.java`,
`ModModelProvider.java`, beide Sprachorte, Wiki und generierte Ressourcen.

## Testbericht
- `./gradlew :common:compileJava :mc26_3:neoforge:compileJava --no-daemon -q`: erfolgreich.
- `python3.12 tools/textures/generate_textures.py --check`: `OK: 512 Texturen und 10 .mcmeta in 2 Baeumen aktuell`.
- `python3.12 wiki/generate.py --all --check`: `wiki: up to date, everything documented.` (bestehende
  Hinweise zu nicht registrierten Guide-Items und der Enchantment-Katalogwarnung bleiben).
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:*brush*'`
  (Lauf `2026-10-09T03-21-44Z-c0df`): `alles gruen: 1/1 bestanden, 0 rot`.
- `python3.12 tools/testrunner/run.py --targets neoforge-263 --filter 'simplebuilding:*brush*'`
  (Lauf `2026-10-09T03-20-56Z-74d4`): `2/4 gruen, 2 rot`. Die roten Tests sind
  `paintsConcreteAndPreservesGlassPaneState` (Farbziel bleibt rot statt weiss) und
  `pipetteAndWash` (Pipette laedt nicht wie erwartet). Der Lauf endet daher mit
  `NICHT gruen`; die Ursache ist auf dem NeoForge-Interaktionspfad noch offen.
- Client-Sicht, Forge-26.3 und ein Voll-Gate wurden nicht ausgefuehrt.

## Review Claude (2026-10-09)
- Übernommen: Item, Laden/Waschen/Pipette, Familienliste, Kreativ ohne Verbrauch, Rezept, Kreativreihe, Wiki.
  Keine Holz-Beize (Holz bleibt bewusst draußen, Test prüft Eichenbretter).
- Nachgebessert: Testfehler relativ/absolut (`helper.setBlock` mit absoluter Position, Ursache der roten
  NeoForge-Tests); Fabric-Einstieg in `fabric.mod.json` fehlte; Tag nach `tags/block/` (26.x-Pfad);
  Pipette im Überlebensmodus kostet einen passenden Farbstoff statt gratis 256 Striche; Block-Entity-Daten
  (Shulker-Inhalt, Bannermuster) wandern mit, Bett wird mit beiden Hälften umgefärbt; normales Glas wird
  gefärbtes Glas; Wandbanner, Woll-/Beton-Treppen und -Stufen (26.3), Kerzenkuchen; Tooltip mit Ladung;
  Testzentrale-Werkzeugreihe `color_brush`; Wiki-Text als Item-Notiz; Datagen-Ausgaben.
- Ziehen: gehaltener Rechtsklick wiederholt den Strich (Vanilla-Wiederholung), kein eigener Tick-Pfad.
- Offen: Textur (Platzhalter, Besitzer-Abnahme), Client-Sicht, Mod-Farbfamilien im Tag.
