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

## Runde 2 (Besitzer-Feedback 09.10., Branch claude-brush2)
Plan:
1. Mechanik: kein Laden, keine Ladung, keine Pipette, kein Auswaschen. Jeder Strich nimmt die naechste
   "Tinte" wie der Bogen seine Pfeile: `ProjectileWeaponItem.getHeldProjectile` (Nebenhand, dann Haupthand),
   danach das Inventar in Vanilla-Reihenfolge (Slot 0..n). Tinte = Farbstoff oder nicht leere Farbpalette.
   Ein Strich kostet 1 Farbstoff + 1 Haltbarkeit (256); Kreativ verbraucht nichts. Ohne Tinte, auf
   nicht faerbbaren Bloecken oder bei gleicher Farbe passiert nichts (nichts verbraucht).
2. Farbpalette (Annahme, dem Besitzer melden): neues Item `color_palette`, buendelartig (BundleItem,
   `bundle_contents`), nimmt nur Farbstoffe auf (64 gesamt = Buendel-Gewicht 1). Als Tinte waehlt sie je Strich
   zufaellig (level.random) eine ihrer Farben, die sich von der Blockfarbe unterscheidet, und verbraucht genau
   diesen Farbstoff. Rezept: Rot/Gelb/Blau-Farbstoff ueber drei Holzbrettern (Tag planks).
3. Anzeige: Item-Modell `select` auf `simplebuilding:brush_ink` (clientseitig aus dem Inventar des Halters:
   `none`, Farbname, `palette`); je Farbe Ebene 1 (Borstenspitze, Graustufen) mit `constant`-Tint der
   Vanilla-Farbstofffarbe; `none` = neutrale Borsten; `palette` = bunte Spitze (eigene Ebene).
   Tooltip nennt die naechste Tinte. Registrierung: Fabric ID_MAPPER, NeoForge Event, Forge Mixin.
4. Textur: Variation des Vanilla-Pinsels (gleiche Form, Elternmodell `minecraft:item/brush` fuer die Haltung),
   Eisen-Zwinge statt Kupfer, lackierter dunkler Griff, Spitze als getoente Ebene; Palette eigene Pixelart.
   Generator `tools/textures/color_brush_2026_10_09.py` (liest Vanilla-Pinsel aus dem 26.3-Client-Jar im
   Gradle-Cache), Vorschau /root/previews/brush/brush-v2.png.
5. Tests (shared + Fabric-Wrapper + Katalog): Bogen-Reihenfolge, Nebenhand-Vorrang, Kreativ gratis,
   Palette zufaellig mit festem Seed, leerer Pinsel tut nichts, Zustaende/Inhalte bleiben.
6. Lang EN/DE, Wiki, Rezept, Kreativreihe, Testzentrale (Reihe color_brush deckt Palette mit ab).
Risiko: Datagen-Ausgaben von Hand gepflegt -> per runDatagen auf sb-test abgleichen.

## Runde 3 (Besitzer-Feedback 09.10., Branch claude-brush3)
Plan:
1. Farbpinsel = verstaerkter Vanilla-Pinsel: `ColorBrushItem extends BrushItem` (Haltbarkeit 256 bleibt).
   Textur = Vanilla-`brush.png` aus dem 26.3-Client-Jar, nur die Kupfer-Zwinge auf Gold umgefaerbt (Gold-Nugget-Palette),
   Spitzen-Ebene (`brush_ink`) bleibt. Rezept shapeless: Vanilla-Pinsel + Goldnugget + Feder.
   Konfliktregel (Entscheidung): auf abbuerstbaren Bloecken (`BrushableBlock`: verdaechtiger Sand/Kies) wird immer
   abgebuerstet, auch mit Farbstoff im Inventar; sonst faerbt ein Strich, wenn Tinte da ist und der Block faerbbar ist
   und eine andere Farbe bekommt; in allen anderen Faellen verhaelt er sich wie der Vanilla-Pinsel (Buersten-Animation,
   Staub, abbuersten via `BrushItem#onUseTick`).
2. Malerpalette -> Farbkasten (`paint_box`, ID-Wechsel, paint_palette entfaellt, war nie auf master). Eigene Komponente
   `simplebuilding:paint_box` (16 Zaehler in DyeColor-Reihenfolge + gewaehlte Farbe), nur Farbstoffe, je Farbe hoechstens
   64 x Stufenfaktor: Basis 1 (64), Verstaerkt 2 (128), Netherit 4 (256), Enderit 8 (512) - wie `StackLimits.max`
   (Faktor x Stapelgroesse). Klicks wie Buendel (Linksklick legt ein, Rechtsklick nimmt bis zu einen Stapel der
   gewaehlten Farbe heraus; ohne Wahl die erste vorhandene). Mausrad ueber dem Kasten waehlt die naechste vorhandene
   Farbe (reine Funktion `PaintBoxItem.nextSelection`), uebertragen ueber das vorhandene Buendel-Auswahlpaket
   (`ReinforcedBundleSelectionPayload`, Handler um den Kasten erweitert) - kein neues Paket; Wahl bleibt nach dem Hovern.
   Tooltip-Bild: alle 16 Farbstoffe in 8x2 Feldern, fehlende ausgegraut, Menge je Farbe, gewaehlte Farbe umrahmt.
   Pinsel nimmt aus dem Kasten weiter zufaellig (Farbe != Blockfarbe).
   Stufen-Weg wie beim Buendel: Verstaerkt = Werkbank-Aufwertung (`ReinforcedBundleRecipe`, behaelt Inhalt; erkennt
   jetzt auch den Kasten) mit vier Diamantkieseln, Netherit = Schmiedetisch (Netherit-Vorlage + Barren), Enderit =
   Schmiedetisch (Enderit-Vorlage + Enderitbarren). Texturen: eigene Pixelart je Stufe (Holz, Holz mit Eisen-Beschlag,
   Netherit, Enderit; Farbtoepfe oben), Generator `tools/textures/paint_box_2026_10_09.py`.
3. Resonanz-Herz: 10x9, Bruchstein-Struktur, dunkler Umriss (`TrimStatsLayout.ICON_WIDTH`/`ICON_HEIGHT`).
4. Tests (shared + Fabric-Wrapper + Katalog): Rezept, Abbuersten echter Weg (verdaechtiger Sand mit Farbstoff im
   Inventar -> Sand, Beute, Farbstoff bleibt), Faerben, Kasten-Limit je Stufe + nur Farbstoffe, Scroll-Auswahl,
   Pinsel zufaellig mit festem Seed, Upgrade behaelt Inhalt. Client: Tooltip-Screenshot des Kastens.
5. Lang EN/DE, Wiki, Kreativ-/Suchtab, Datagen-Ausgaben (von Hand, dann runDatagen auf sb-test abgleichen).
