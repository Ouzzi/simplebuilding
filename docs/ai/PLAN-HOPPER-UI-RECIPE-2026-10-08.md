# Trichter-GUI, Lore und Rezept (2026-10-08)

## Ist-Zustand und Ursache

- `NetheriteHopperScreen` verwendet die Vanilla-Hopper-Slotpositionen x=44, 62, 80, 98, 116
  und setzt den Filter-Button unmittelbar rechts davon bei x=138. Dadurch gibt es keine
  slotbreite Lücke und der Block ist nach rechts verschoben.
- `ModHopperScreenHandler` übernimmt diese Positionen direkt aus `HopperMenu`; Screen und Menü
  haben deshalb dieselbe, aber für die gewünschte Anordnung falsche Geometrie.
- Die Info-Tooltip-Logik enthält Geschwindigkeit und Filterhinweis für alle drei Mod-Trichter,
  die vorhandenen Tests belegen aber nur Verstärkt und einen Teil der Stufen.
- Das Rezept `reinforced_hopper_from_crafting` verlangt aktuell fünf Vanilla-Trichter, drei
  gesprungene Diamanten und ein Namensschild und erzeugt fünf Ausgaben statt genau eines
  verstärkten Trichters.

## Änderung

1. Die fünf Menüslots um 18 Pixel nach links verschieben und den Filter-Button rechts mit
   ungefähr einer Slotbreite Abstand platzieren; im Zwischenraum das vorhandene Vanilla-
   Trichter-Item als 16x16-Platzhalter sowie einen Doppelpunkt rendern. Keine neue Textur,
   Farbe, Rahmen- oder Hintergrundänderung.
2. Tooltip-Lore für alle Mod-Trichter zweisprachig vollständig belegen und durch GameTests
   absichern.
3. Das verstärkte Trichter-Rezept als shapelesses Rezept aus je einem Trichter, gesprungenem
   Diamanten und Namensschild mit genau einem Ergebnis datengenerieren. Den CraftingInput-
   GameTest auf dieses Rezept umstellen.
4. Einen GameTest für die mittige Slotanordnung ergänzen.

## Dateien

- `common/src/shared/java/com/simplebuilding/screen/ModHopperScreenHandler.java`
- `common/src/shared/java/com/simplebuilding/client/gui/NetheriteHopperScreen.java`
- `common/src/shared/java/com/simplebuilding/items/tooltip/InfoTooltips.java`
- `src/main/java/com/simplebuilding/datagen/ModRecipeProvider.java`
- `common/src/shared/java/com/simplebuilding/gametest/HopperTests.java`
- `src/main/java/com/simplebuilding/gametest/HopperGameTest.java`
- `common/src/shared/java/com/simplebuilding/gametest/ImmersionTests.java`
- `src/main/resources/assets/simplebuilding/lang/{en_us,de_de}.json`
- `mc26_3/overlay/resources/assets/simplebuilding/lang/{en_us,de_de}.json`
- generierte 26.3-/gemeinsame Rezept- und Advancement-Dateien

## Tests und Bericht

Nach der Umsetzung:

```text
python3.12 tools/testrunner/run.py --targets fabric-263 --list
python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:hopper_game_test_*'
python3.12 tools/testrunner/run.py --targets fabric-263 --filter 'simplebuilding:immersion_game_test_pad_and_machine_tooltips_name_their_numbers'
```

Ergebnisse:

- `:mc26_3:fabric:runDatagen :mc26_3:fabric:syncGenerated263`: Exit 0.
- `./gradlew runDatagen`: Exit 0; der dabei entstandene unbeteiligte Generated-Drift wurde
  nicht übernommen, nur das geänderte 26.2-Rezept blieb erhalten.
- `2026-10-08T18-07-29Z-4239`: `alles gruen: 12/12 bestanden, 0 rot`.
- `2026-10-08T18-08-18Z-d636`: `alles gruen: 1/1 bestanden, 0 rot`.

Nicht getestet wurden die Client-Sichtabnahme sowie NeoForge und Forge. Eine zunächst falsche
Auswahl `hopper` bzw. `hopper_game_test_` lief mit 0/0, weil Fabric den vollständigen
Namespace-Wildcard benötigt; sie zählt nicht als Testergebnis.

## Review (Claude, Übernahme nach claude-wave1)

- Übernommen: formloses Rezept 1 Trichter + 1 Rissiger Diamant + 1 Namensschild → 1 Verstärkter Trichter
  (Datagen + 26.2-Generat, Rezept-GameTest), Tooltip-Zeile `tooltip.simplebuilding.hopper.filter.2` für alle
  drei Mod-Trichter samt Test. Wiki (`wiki/manual.json`, Beschaffung + Rezeptzeile EN/DE) nachgezogen.
- Entfernt: `mc26_3/generated/.../reinforced_hopper_from_crafting.json` war identisch mit dem 26.2-Generat
  (syncGenerated263 behält nur Abweichungen).
- Zurückgestellt: GUI-Teil (Slots 18 px nach links, Filterknopf bei x=132, Trichter-Platzhalter + Doppelpunkt
  statt „Filter“). Der 26.2-/Vanilla-Pfad zeichnet weiter `hopper.png` mit Slots bei x=44 – verschobene Slots
  lägen neben ihren Rahmen. Der Kasten-Stil (Branch claude-sc-merge, `ModScreenStyle.hopper`) zeichnet die Slots
  aus dem Menü und ersetzt „Filter“ bereits durch eine Symboltaste; das mittige Layout gehört nach dessen Merge
  dorthin (Sperrrechteck des Filterknopfs `left + 138` mitziehen).
