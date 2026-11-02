# Plan B-FOUNDRY03 (Sitzung B, 02.11.2026) - Simple Foundry: Schichten sichtbar, Brennstofftank, Pult-Fenster

Basis: `hub/b-bundle91-gen` (8d395b8e7), Branch `b-foundry03`. Auftrag B02, Besitzerwunsch 02.11. (b-antworten-20261102b, Abschnitt Simple Foundry), Wahlen FOUNDRY32-UI = A, FOUNDRY32-NAME = A (Namen stehen schon: Hüttenziegel, Schmelzkammer, Hüttenpult).

## Ist-Stand (vor diesem Durchgang)
Stufe 1-4 (PLAN-B-FOUNDRY32/-02): Hülle, Schmelzkammer (Würfel mit Fliesen), Pult-BE mit EINEM Brennstoff-Stapel (`fuel`), Bedienung nur Rechtsklick und Chat; die Plätze 3/6/9/12 waren parallele "Bahnen" ohne Fenster; Schichten nur als Zahl im Chat.

## Was jetzt gebaut ist (Zusammenfassung des "endgültigen Konzepts" für den Besitzer)
1. **Schichten sichtbar (Variante A)**: Jede Schmelzkammer hat ein Fenster (12 x 12 Pixel, dunkel). Vier Teilstücke übereinander (je 3 Pixel) zeigen das Metall, das auf dieser Höhe im Tank liegt: Gold unten, dann Kupfer, Eisen oben. Wer Glas in die Hülle setzt, sieht die Schichten wie Honig im Glas. Reine Anzeige: Blockzustand `s0..s3` (none/gold/copper/iron) der Kammer, vom Pult gesetzt, nur geänderte Blöcke werden geschrieben, höchstens alle 10 Ticks.
2. **Brennstofftank mit Füllstand**: Das Pult speichert Brennstoff als Brenndauer (Kapazität 40 000 Ticks = zwei Lava-Eimer), getrennt nach Hitze (mittel: Kohle/Holz, hoch: Lava, Lohenrute, Kohleblock). Füllstand 0-4 steht als Blockzustand `fuel` auf der Pultfront (Anzeige-Stufen in der Textur, links oben) und als Balken im Pult-Fenster.
3. **Nachfüllen**: Hauptweg Lava-Eimer (Rechtsklick aufs Pult oder in den Brennstoffplatz des Fensters: 20 000 Ticks, hoch, der leere Eimer kommt zurück). Nebenweg Kohle/Holzkohle (je 1 600 Ticks, mittel), auch Holz, Lohenrute, Kohleblock. Passt etwas nicht mehr hinein (Kapazität), wird es nicht genommen. Alte Welten: der frühere Brennstoff-Stapel wird beim Laden in den Tank gegossen. Kühl brennendes Feuer, das auf Eisen warten würde, gibt den Rest seiner Stufe zurück, sobald heißer Brennstoff da ist (vorher hing das Pult fest).
4. **Pult-Fenster (FOUNDRY32-UI = A)**: Rechtsklick mit leerer Hand öffnet es. Links die Plätze (3/6/9/12 je Stufe; gesperrte mit Kreuz und Hinweis), in der Mitte der **Schnitt** des Tanks (ein Band je Ebene, die Schmelzen übereinander, gewählte Ebene umrandet, Klick wählt, der Pfeil-Knopf darunter schaltet eine Schicht nach unten), rechts der **Stapel** (Eisen, Kupfer, Gold mit Barrenzahl und wartenden Barren), darunter der Brennstoff-Balken und der Brennstoffplatz. Statuspunkt oben rechts (grün Haken / rot Kreuz, Tooltip nennt den Grund). Die Plätze sind echte Slots: je aktivem Platz geht pro Schmelzintervall ein Stück in die Schmelze.
Schleichen + leere Hand schaltet weiter die Schicht (SA4), Rechtsklick mit Erz/Brennstoff wirft weiter direkt ein.

## Geänderte/neue Dateien (modules/simplefoundry)
- Java: `block/ChamberBlock` (neu, Zustände s0..s3), `block/FoundryMenu` (neu), `client/FoundryScreen` (neu, simplelib `UiBoxes`/`UiPalette`, Vanilla-Button für den Pfeil), `block/DeskBlockEntity` (Tank, Slots, Kammeranzeige, ContainerData), `block/DeskBlock` (Zustand `fuel`, Lava-Eimer, Fenster), `Layers` (`atUnit`, `windowPart`), `FoundryScan.Result.minY`, `FoundryRegistry` (Kammer-Klasse, MenuType), Fabric: `FoundryFabricClient` (neu), `fabric.mod.json` (client entrypoint).
- Daten/Texturen: `tools/gen_foundry03.py` (Fenster-Kammer, 3 Schmelz-Overlays, 5 Pult-Fronten, Modelle, Blockstates, Lang); Textur nach Endholz-Prozess (A8b, weiche Töne, ~7 % harte Stufen), Platzhalter-Charakter, Vorschau liegt bei.
- Wiki: `wiki/manual.json` EN+DE (Pult, Brennstofftank, Fenster, Stufen, Pfeil), Sheets neu erzeugt. `modules/modules.json`: Client-Smoke für simplefoundry.
- Tests: `FoundryTests` + Fabric-Adapter: `fuel_tank`, `fuel_burn`, `layer_view`, `window_slots`; Client-Smoke `clienttest/.../ModuleClientSmoke` (Schichten durch Glas, Anzeigestufen, Fenster-Screenshots).

## Entscheidungen (selbst getroffen, begründet)
- Brennstoff getrennt nach Hitze statt einer Zahl: sonst könnte Kohle Eisen schmelzen. Kleine Brennstufe 100 Ticks, damit ein Wechsel auf Lava schnell greift.
- Anzeige über Blockzustände statt Block-Entity-Renderer: datengetrieben, kein Client-Code, auf dem Server testbar. Kosten: 256 Zustände je Kammer (Blockstate-Multipart), unkritisch.
- Pfeil als echter Vanilla-Button (Erfahrung Simple Earth: selbstgezeichnete Treffer versagten mit echter Maus); der Klick auf eine Ebene ist nur Abkürzung.
- Menü-Daten als 16-Bit-Hälften (ContainerData ist short), keine eigenen Pakete.

## Risiken / nicht getestet
- Client-Optik und echte OS-Maus (XTest) nur soweit der Client-Smoke reicht (siehe Bericht). Forge/NeoForge nicht (nur 26.3 Fabric).
- 26.3-API nur per Compile/Gametest geprüft.

## Verifikation
Ein sbt-Lauf: Compile Fabric + simplefoundry, Modul-Gametests (+standalone), check_client_mixins, wiki --check, Client-Smoke simplefoundry (Screenshots).
