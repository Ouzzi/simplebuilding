# Plan: Resonanz-Statusfeld neben dem Rezeptbuch

## Ursache / Ist-Zustand

`TrimStatsPanel` zeichnet das Resonanzfeld derzeit zentral links neben dem Inventar.
Es verwendet eine Ward-Schmiedevorlage als Symbol, grüne Schrift und ein relativ breites
20-Pixel-Feld. Die normale Inventaransicht und `BackpackScreen` rufen dieselbe Klasse auf,
haben aber unterschiedliche horizontale Vanilla-Anker für den Rezeptbuch-Button.

## Änderung

- Eine reine, client-unabhängige Layoutfunktion berechnet aus Buch-Button-Position und
  Textbreite die Position und Breite des kompakten Felds direkt rechts neben dem Button.
- Das Feld wird schmaler und niedriger/kompakter; der Wert wird grau dargestellt.
- Die Ward-Vorlage wird durch den vorhandenen Vanilla-HUD-Herzsprite als Platzhalter ersetzt.
  Es wird keine neue Pixelkunst und keine neue Textur angelegt.
- Beide Bildschirme verwenden ihre tatsächliche Rezeptbuch-Button-Position.
- Ein registrierter GameTest prüft die Layoutfunktion mit mehreren Textbreiten und
  Randbedingungen.

## Betroffene Dateien

- `common/src/shared/java/com/simplebuilding/client/gui/TrimStatsLayout.java` (neu)
- `common/src/shared/java/com/simplebuilding/client/gui/TrimStatsPanel.java`
- `common/src/shared/java/com/simplebuilding/mixin/client/InventoryScreenMixin.java`
- `common/src/shared/java/com/simplebuilding/client/gui/BackpackScreen.java`
- `common/src/shared/java/com/simplebuilding/gametest/TrimWiringTests.java`
- `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java`
- `src/main/java/com/simplebuilding/gametest/TrimWiringGameTest.java`
- Sprachdateien müssen nicht geändert werden: Der vorhandene Wert und die Tooltiptexte bleiben
  unverändert; es kommt kein neuer sichtbarer Text hinzu.

## Abweichungen

Die Fabric-Registrierung über den loader-spezifischen Adapter musste zusätzlich geändert werden;
der gemeinsame Testkatalog allein wird von Fabric nicht als Testmethode entdeckt.

## Tests / Bericht

Gezielter Lauf:

`python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:trim_wiring_game_test_resonance_panel_layout_sits_beside_recipe_book'`

Ergebnis aus der Testzentrale:

`Fabric - MC 26.3                  0      1      1    0    31.0s`

`alles gruen: 1/1 bestanden, 0 rot`

Nicht getestet: die Client-Sicht (bewusst kein Client-Lauf), NeoForge 26.3 und Forge 26.3.
Der erste Versuch mit einem unqualifizierten Filter fand wegen Fabric-Matcher-Semantik keine
Tests; der namespaced Filter oben ist der maßgebliche erfolgreiche Lauf.

## Abschluss Claude (2026-10-09)
- Herz: eigenes 9x9-Sprite `simplebuilding:resonance_heart` in Steinfarben (Generator
  `tools/textures/resonance_heart_2026_10_09.py`) statt des Vanilla-Herzcontainers.
- Feld 18 px hoch wie der Buch-Knopf, Herz 3 px eingerückt und senkrecht mittig, Wert grau dahinter.
- Ungewollte Änderung des Kreativtab-Absatzes im Handbuch aus dem WIP verworfen.
- Client-Test `inventoryShowsTheResonanceFieldAndItsDetails` macht jetzt den Screenshot
  `inventory-resonance-field`.

## Runde 2 (Besitzer 09.10.): Vanilla-Stil
Befund Besitzer: dicker Rahmen (popup/background) + Farbe machen den Inhalt unleserlich, Herz kaum erkennbar.
Plan: Hintergrund exakt wie der Rezeptbuch-Knopf (`recipe_book/button`: 1-px-Schwarzrand mit runden Ecken,
weisse Kante oben/links, #555555 unten/rechts, Fuellung #C6C6C6) als eigenes Nine-Slice-Sprite
`resonance_field` (Rand 3), Hoehe 18 wie der Knopf; Breite waechst mit dem Text (20 px reichen fuer Herz + "0.22x"
nicht - Abweichung). Herz = Vanilla-Herzform (hud/heart container + full) in Steingrau mit schwarzem 1-px-Umriss.
Wert weiss mit Schatten wie Vanilla-Stapelzahlen. Hover: `recipe_book/button_highlighted`-Rand analog.
Pruefung: HudAndTooltip-Client-Test (Screenshot), 3x-Ausschnitt nach /root/previews/resonance/resonance-v2.png.
