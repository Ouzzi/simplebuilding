# Testzentrale

Stand 2026-09-25. Die Testzentrale ist eine Welt, in der alles aus SimpleBuilding nebeneinander steht:
Rüstungsständer, Rahmenwände, Meißel-Türme, laufende Maschinen, das Erzdetektor-Feld und eine
Steuerwand mit Befehlsblöcken. Sie wird **aus Code** gebaut (`/sbtestcentre`), ist also auf jeder
Linie (1.21.11, 26.2, 26.3) und jedem Loader (Fabric, NeoForge, Forge) gleich und wächst mit neuen
Features mit.

**Regel:** Am Ende eines Feature-Runs (einer Gruppe von Wellen) die Zentrale neu bauen und
durchgehen - nicht nach jedem kleinen Schritt. Der Abdeckungstest meldet vorher schon, wenn ein
neues Item keinen Platz hat.

## Welt anlegen und öffnen

1. Im Entwicklungs-Client (`runClient`, beliebiger Loader) **Einzelspieler → Neue Welt**.
2. Weltname genau **`SB-Testzentrale`**, Spielmodus Kreativ, **Cheats an**, unter *Welt* den Typ
   **Flachland** (Superflat) wählen. Flachland ist wichtig: der Bau leert nur den Bereich über dem
   Boden, Berge ragten sonst hinein.
3. Welt betreten. In einer Entwicklungsumgebung baut sich die Zentrale beim ersten Betreten selbst
   (einige Sekunden) und setzt dich an den Eingang. Erkennbar ist "schon gebaut" an der Datei
   `simplebuilding_testcentre.txt` im Weltordner (sie hält den Ursprung `x y z`).
4. In jeder anderen Welt: `/sbtestcentre build` (über 0/0) oder `/sbtestcentre build here`.

## Befehle

Alle unter `/sbtestcentre`, Befehlsrechte Stufe 2 (Operator/Cheats; Befehlsblöcke dürfen ihn auch).

| Befehl | Wirkung |
|---|---|
| `build` | ganze Zentrale am gespeicherten Ursprung neu bauen (sonst über 0/0, Oberfläche) |
| `build here` | an den eigenen Füßen bauen, Ursprung merken |
| `build <x y z>` | an festem Punkt bauen (so rufen die Befehlsblöcke) |
| `section <id> [x y z]` | nur einen Abschnitt leeren und neu bauen (z. B. `section machines`) |
| `kit [Spieler]` | höchste Stufe von Meißel, Baustab, Vorschlaghammer, Spitzhacke (max. verzaubert), die Geräte, eine Blaupause, Steinziegel |
| `tp` | zum Eingang |
| `coverage` | nennt Mod-Items, die noch keinem Abschnitt zugeordnet sind |

Der Bau leert zuerst den ganzen Bereich (Entities außer Spielern, Behälterinhalte, Blöcke), legt
den Boden (Glatter Stein, Seelaternen im 6er-Raster, Tiefenschieferfliesen unter den Abschnitten)
und setzt dann die Abschnitte - Blöcke "leise" (ohne Nachbar-Updates, ohne Drops), danach Schilder,
Behälter, Befehlsblöcke, zuletzt Rahmen und Rüstungsständer. Ein voller Bau dauert rund eine Sekunde.

## Übersicht der Abschnitte

Die Abschnitte liegen in Reihen (höchstens 140 Blöcke breit) in +x; vor jeder Reihe ein Gang von
7 Blöcken in -z. Der Ursprung ist die Standhöhe am Anfang des ersten Gangs. Stand heute (26.2)
etwa 143 × 116 Blöcke, 16 Abschnitte, rund 660 Rahmen und 29 Rüstungsständer. Die genaue Lage
steht im Log des Bau-Tests (`test centre ... sections:`), weil sie aus den Inhalten folgt.

| Id | Inhalt | Quelle der Inhalte |
|---|---|---|
| `controls` | Befehlsblöcke mit Knopf: alles/je Abschnitt neu bauen, Kit, Tag/Nacht, Wetter, Kreativ/Überleben, Zombie/Skelett/Creeper im Dunkelraum, Mobs und Drops entfernen. Zwischen zwei Befehlsblöcken steht Glas: ein Knopf versorgt seinen Träger stark, und ein stark versorgter Befehlsblock hätte den Nachbarn mit ausgelöst (bis 2026-09-25 feuerte jeder Knopf zwei Befehle); der Bautest drückt jeden Knopf und prüft, dass genau sein Block Strom bekommt | Abschnittsliste, Anker `mob_spawn` |
| `armour` | Ständer: jede Rüstungsstufe (mit Schwert), Enderit mit leuchtendem / strahlendem / beidem Besatz, je Besatzmuster ein Enderit-Satz; Wand: alle Schmiedevorlagen, alle Besatzmaterialien, je Muster eine Spalte mit vier Teilen | Tab-Zeilen helmets…boots/swords, Register `trim_pattern`/`trim_material`, alle `SmithingTemplateItem`, alle Items mit `provides_trim_material` |
| `books` | jede Verzauberung (Mod, dann Vanilla, auch Flüche) als Buch auf Höchststufe mit Schild | Register `enchantment` |
| `tools` | je Familie eine Zeile unverzaubert und eine max. verzaubert (Meißel, Baustäbe, Vorschlaghämmer, Spitzhacken, Schaufeln, Hacken, Äxte, Schwerter, Speere, Geräte), alte Spachtel; daneben alle Varianten des Entwickler-Tabs (jede exklusive Auswahl) | `ModItemGroupsContent.toolsRows`, `DevEnchantedTab` |
| `storage` | Bündel, Köcher, Rucksäcke: unverzaubert, alle Verzauberungs-Varianten, gefärbt (rot/limette/blau/gelb); Rucksäcke zusätzlich als Block | Tab-Zeilen bundles/quivers/backpacks |
| `food` | alle Mod-Lebensmittel plus Apfel, Goldapfel, verzauberter Goldapfel, Karotte, goldene Karotte zum Vergleich | Items mit `food` |
| `materials` | der ganze Tab SimpleMaterials mit Namensschildern | Tab MATERIALS |
| `chisel` | je Meißel-Kette ein Turm (Block für Block) mit dem Startblock davor zum Meißeln; Rahmen mit dem nötigen Meißel (mit Constructor's Touch, wenn nur die Touch-Tabelle die Kette kennt), Schild mit Stufe, Länge, "Kreislauf" | Tabellen des höchsten Meißels (`getForwardMap`/`getTouchForwardMap`) |
| `inworld` | je In-World-Umwandlung eine Station: Umformen mit dem Vorschlaghammer (Block → Treppe → Stufe, Mod-Blöcke + Stein/Eichenbretter), Diamantblock zerschlagen, Maschinen-Aufwertung (Maschine, Nugget, schwächster passender Hammer), Schere an Wolle, Besatzvorlage im Rahmen, Oktant im Kessel waschen | `InWorldTransformations`, `SledgehammerUpgrades`, `SledgehammerEntityInteraction`, `OctantCauldronWash` |
| `blocks` | Musterwand des Tabs SimpleBuilding (je Block eine Säule mit Namensschild), Schachbretter zusätzlich als Bodenflächen, Schwebesand/-kies frei schwebend, Levitationssand/-kies unter Glas | Tab BUILDING_BLOCKS |
| `lightroom` | geschlossener Dunkelraum mit Baulichtern und Tür; Monster dürfen trotz Licht spawnen (Knöpfe auf der Steuerwand) | - |
| `machines` | je Ofen-Familie und Stufe eine laufende Kette: Truhe → Trichter (gleiche Stufe) → Ofen → Trichter → Truhe, Kohle von der Seite; Kolben aller Stufen mit Hebel (Reihe rechts neben den Maschinen an der Gangkante; bis 2026-09-25 stand sie hinter der Rückwand und war vom Gang aus unsichtbar): 13 Steine (Vanilla schafft es nicht), Netherit-Kolben zerbricht, Enderit-Kolben vor verstärktem Tiefenschiefer mit Redstoneblöcken in der Truhe (Durchbruch) | Tab-Zeilen hoppers, furnaces, smokers, blast_furnaces, pistons |
| `ores` | Erzdetektor-Feld: je Wirtsgestein (Stein, Tiefenschiefer, Netherrack, Endstein) ein Block, darin jedes Erz einzeln in 3/7/11/15 Blöcken Abstand; Schild mit Klasse, Abstand, Reichweite; Detektor im Rahmen | alle Blöcke `*_ore` und Antiker Schutt, `OreDetectorItem.classify` |
| `planning` | alle Oktanten, Zeile Bauplanung; je Baustab-Modus (Linie, Brücke, Bedecken, Farbpalette, Oktant füllen, Dach) eine Fläche mit passendem Stab; Brücke mit Graben (auf flachem Boden gibt es nichts zu überbrücken), Dach mit Prisma-Oktant im Rahmen und Truhe voll Eichentreppen/-stufen; Beispielhaus mit Kartentisch, Oktant mit gesetzter Auswahl und daraus beim Bau gescannter Blaupause | Tab-Zeilen, `BlueprintScanner` |
| `mining` | gemischte Wand (Vielseitigkeit), Eisen- und Kohleader (Aderabbau), lange Steinwand (Tunnelabbau), je mit passend verzaubertem Werkzeug | Verzauberungen, Tab-Zeilen |
| `tweaks` | die aus Simple Tweaks uebernommenen Pads und Platten: alle Tweaks-Tab-Zeilen als Rahmen, davor Spawn-Teleporter I und Enderit, Elytra-Pad, Flypad, Launchpad mit Truhe Windkugeln, Diamant-/oxidierte Kupferplatte an Lampen, Netherit-/Enderit-Platte mit Fass (Diamant) darunter, Leitstein mit Truhe (Echo-Kompass, Enderperlen); Chunk-Loader nur im Rahmen (gesetzt wuerde er Chunks erzwingen) | `TweaksStation`, `TweaksItems.functionalRows` |
| `devices` | **Grundversorgung für neue Tab-Zeilen**: jede Zeile aus SimpleTools oder Maschinen & Lager, die kein Abschnitt eigens liest, erscheint hier als Rahmenzeile, Blöcke daraus zusätzlich auf dem Boden | Tab-Zeilen minus `KNOWN_TOOL_ROWS`/`KNOWN_FUNCTIONAL_ROWS` |
| `unsorted` | Mod-Items, die nirgends stehen - soll leer sein | Rest |

Schilder und Namensschilder sind übersetzbar (`simplebuilding.testcentre.*` in `en_us`/`de_de`,
Rückfall Englisch im Code): ein deutscher Client liest Deutsch, ein englischer Englisch.

## Abdeckungstest

`TestCentreTests` (Katalog `test_centre_game_test_*`, alle Linien, alle Server-Ziele):

- **`every_mod_item_and_block_has_its_place_in_the_test_centre`** plant die Zentrale ohne Welt und
  prüft: jedes registrierte Mod-Item steht in einem Abschnitt (Block, Rahmen, Ständer oder
  Behälterinhalt), jeder Mod-Block ist gesetzt oder über sein Item gezeigt, jede Ausnahme in
  `TestCentreLayout.EXCLUDED` gibt es noch, `/sbtestcentre` ist registriert. Rot bedeutet: ein
  neues Item landet in "unsorted" - der Test nennt es.
- **`the_whole_centre_builds_and_matches_its_plan`** baut die ganze Zentrale fern der anderen Tests
  (x = z = 20000) und vergleicht mit der Planung: jeder geplante Block steht, jedes Schild hat
  Text, Rahmen und Ständer sind genau so viele wie geplant, die Blaupause ist gescannt, der Oktant
  hat seine Auswahl, kein Drop liegt herum; danach baut er `armour` neu und prüft, dass sich keine
  Rahmen verdoppeln. Zum Schluss wird der Bereich wieder geleert.

Gegenprobe (2026-09-25, fabric-262): Enderit-Kolben aus der Kolben-Zeile gefiltert → Test 1 rot
("item simplebuilding:enderite_piston has no section …; block … is neither placed nor framed");
Oktant-Rahmen im Builder übersprungen → Test 2 rot ("item frames: planned 664, placed 662").

Bewusste Ausnahmen (`TestCentreLayout.EXCLUDED`): `creative_spacer` (Platzhalter der Tabs),
`netherite_piston_head` (technischer Block ohne Item).

## Erweitern

Code: `com.simplebuilding.dev.testcentre` (26.x unter `common/src/shared/java`, 1.21.11-Spiegel
unter `mc1_21_11/shared/java` - dort ohne die `McVersion`-Shims; 26.3 braucht für Schildtext,
Unverwundbarkeit und Inventar die Zwillinge in `common/src/mc26_2/.../McVersion` und
`mc26_3/overlay/.../McVersion`).

- **Neues Item in einer bekannten Tab-Zeile** (z. B. ein neuer Meißel in `chisels`): nichts zu tun.
- **Neue Tab-Zeile**: erscheint von selbst unter `devices`. Braucht das Feature mehr als Anschauen
  (eine Bahn, eine Maschine, ein Raum), bekommt es eine eigene Station: Methode in
  `TestCentreSections`, Zeilenname in `KNOWN_TOOL_ROWS`/`KNOWN_FUNCTIONAL_ROWS`.
- **Item ohne Tab-Zeile** (etwa nur im Tab SimpleMaterials): steht unter `materials`; sonst landet
  es in `unsorted` und der Abdeckungstest wird rot.
- **Neuer Abschnitt**: Methode `static TcCanvas name(TcContext ctx)` in `TestCentreSections`,
  Id in `TestCentreLayout.SECTION_IDS` (Reihenfolge = Reihenfolge in der Welt) und Eintrag in
  `TestCentreLayout.plan`. Die Steuerwand bekommt den Neu-bauen-Knopf automatisch.
- **Zeichenregeln**: lokale Koordinaten, y = 0 ist die Standhöhe, Blick der Besucher nach +z,
  Rückwände bei großem z (`TcCanvas.backWall`, `wallFrame`, `wallSign`, `frameGrid`, `rowsPanel`).
  Weltkoordinaten (Oktant-Ecken, Befehle) nie selbst ausrechnen: `octantFrame`/`blueprintFrame`
  verschieben ihre Ecken mit, Befehle bekommen den Ursprung von `TestCentreLayout.controls`.
- Beschriftungen immer über `TcText.t(key, englisch)` und den Schlüssel in beide Sprachdateien
  beider Linien.
- Nach Änderungen: `run.py --targets fabric-262,fabric-12111,fabric-263 --filter "simplebuilding:test_centre_*"`.

## Grenzen

- Entities in nicht geladenen Chunks sieht das Leeren nicht; wer weit weg steht, bekommt beim
  Neubau doppelte Rahmen. Beim Bauen in der Zentrale stehen (Sichtweite ≥ 10 Chunks reicht).
- Der Bau schaut nicht nach Schutzgebieten - er ist ein Entwicklerwerkzeug für Testwelten.
- Trichter und Öfen laufen nach dem Bau sofort los; der Knopf "Neu bauen: Maschinen" setzt sie zurück.
