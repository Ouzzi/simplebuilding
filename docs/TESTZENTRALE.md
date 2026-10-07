# Testzentrale

Stand 2026-10-02. Die Testzentrale ist eine Welt, in der alles aus SimpleBuilding nebeneinander steht:
Rüstungsständer, Rahmenwände, Meißel-Türme, laufende Maschinen, das Detektor-Feld und eine
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
   `simplebuilding_testcentre.txt` im Weltordner (sie hält den Ursprung und den Fingerabdruck der
   Planung: `x y z fingerabdruck`).
4. **Alte Welt, neuer Code:** weicht der gespeicherte Fingerabdruck beim Betreten von der heutigen
   Planung ab (oder fehlt er, wie in allen Welten von vor 2026-09-28), kommst du an den Eingang und
   die Zentrale baut sich nach 3 s neu ("älter als der Code"). Die 3 s braucht der Server, bis die
   Rahmen der alten Zentrale geladen sind - sonst sähe das Leeren sie nicht, und sie hingen doppelt.
   Vorher blieb eine einmal gebaute Zentrale für immer auf ihrem alten Stand: die dicht gepackten
   Befehlsblöcke, die am 2026-09-25 behoben wurden, feuerten in alten Welten weiter doppelt.
   Ändert sich nur der Bau-Code (`TestCentreBuilder`), nicht die Planung, `TestCentreLayout.BUILDER_VERSION`
   erhöhen.
5. In jeder anderen Welt: `/sbtestcentre build` (über 0/0) oder `/sbtestcentre build here`.

## Befehle

Alle unter `/sbtestcentre`, Befehlsrechte Stufe 2 (Operator/Cheats; Befehlsblöcke dürfen ihn auch).

| Befehl | Wirkung |
|---|---|
| `build` | ganze Zentrale am gespeicherten Ursprung neu bauen (sonst über 0/0, Oberfläche) |
| `build here` | an den eigenen Füßen bauen, Ursprung merken |
| `build <x y z>` | an festem Punkt bauen (so rufen die Befehlsblöcke) |
| `section <id> [x y z]` | nur einen Abschnitt leeren und neu bauen (z. B. `section machines`) |
| `kit [Spieler]` | höchste Stufe von Meißel, Baustab, Vorschlaghammer, Spitzhacke (max. verzaubert), die Geräte, eine Blaupause, Steinziegel |
| `give <station> [Spieler]` | das Kit einer Station (siehe unten); **ersetzt das Inventar**. So ruft der Ausgabe-Knopf vorn an jeder Station |
| `tp` | zum Eingang |
| `coverage` | nennt Mod-Items, die noch keinem Abschnitt zugeordnet sind |

Der Bau leert zuerst den ganzen Bereich (Entities außer Spielern, Behälterinhalte, Blöcke), legt
den Boden (Glatter Stein, Seelaternen im 6er-Raster, Tiefenschieferfliesen unter den Abschnitten)
und setzt dann die Abschnitte - Blöcke "leise" (ohne Nachbar-Updates, ohne Drops), danach Schilder,
Behälter, Befehlsblöcke, zuletzt Rahmen und Rüstungsständer. Ein voller Bau dauert rund eine Sekunde.

## Ausgabe-Knöpfe (Kits)

Jede Station außer der Steuerwand hat **vorn links einen Knopf zum Gang** (Befehlsblock auf Glas,
Schild "Items holen" darüber; die Station rückt dafür zwei Spalten nach rechts). Er führt
`/sbtestcentre give <station> @p` aus und ersetzt das Inventar durch das Kit der Station
(`TestCentreKits`):

- **Haupt- und Nebenhand** für die Interaktionstests der Station, z. B. `inworld`: höchster
  Vorschlaghammer + Aufwertungs-Nugget, `templates`: höchster Vorschlaghammer + Glowstonestaub
  (Leuchttinte im Inventar), `chisel`: höchster Meißel + derselbe mit Constructor's Touch,
  `planning`: höchster Baustab + Oktant (der Dach-Modus braucht ihn in der Nebenhand), `states`: leerer Resonanzstab + Amethystsplitter (am Amboss laden), `placeables`: Steinkiesel, `sinkdamper`: Leitern, `tools`:
  höchste Spitzhacke + höchster Meißel (max. verzaubert), `ores`: Detektor, `mining`: Werkzeug mit
  Vielseitigkeit, `blocks`: höchster Baustab, `lightroom`: Baulichter, `tweaks`: Windkugeln,
  `armour`: höchstes Schwert. Ohne Festlegung nimmt die Haupthand das erste gezeigte Item.
- **Ins Inventar alles, was die Station zeigt**, als volle Stapel: Rahmen, Ständer, Behälterinhalte,
  dann die gesetzten Blöcke (Mod vor Vanilla) - an `blocks` also die ganze Blockpalette. Gerüst
  (Wände, Boden, Glas, Schilder, Truhen, Befehlsblöcke) gehört nicht dazu. Was nicht in die 36 Plätze
  passt, fällt weg; der Befehl meldet die Zahl.

Das Kit folgt aus der Planung der Station, nicht aus einer Liste: zeigt eine Station etwas Neues, ist
es auch im Kit. Stationen, die nichts zeigen (heute `devices`, `gallery`, `unsorted`: jede Tab-Zeile hat eine eigene Station, jeder Block steht irgendwo), bekommen keinen Knopf; der Test verlangt einen Knopf genau dann, wenn das Kit nicht leer wäre. Eine neue Station braucht dafür nichts zu tun.

## Übersicht der Abschnitte

Die Abschnitte liegen in Reihen (höchstens 140 Blöcke breit) in +x; vor jeder Reihe ein Gang von
7 Blöcken in -z. Der Ursprung ist die Standhöhe am Anfang des ersten Gangs. Stand 2026-09-28 (26.2)
147 × 129 Blöcke, 19 Abschnitte, 704 Rahmen, 29 Rüstungsständer, 46 Befehlsblöcke (Steuerwand und
Ausgabe-Knöpfe); die Steuerwand allein ist 91 Blöcke breit. Stand 2026-10-02 (26.3): 145 × 194 Blöcke, 31 Abschnitte, rund 1600 Rahmen, Steuerwand 130 Blöcke breit. Die genaue Lage
steht im Log des Bau-Tests (`test centre ... sections:`), weil sie aus den Inhalten folgt.

| Id | Inhalt | Quelle der Inhalte |
|---|---|---|
| `controls` | Befehlsblöcke mit Knopf: alles/je Abschnitt neu bauen, Kit, Tag/Nacht, Wetter, Gewitter (Eisenstab), Kreativ/Überleben, Zombie/Skelett/Creeper im Dunkelraum, Mobs und Drops entfernen. Jeder Befehlsblock steht auf Glas, links Luft, rechts eine Glassäule (Abstand 3), kein Stein verbindet zwei Blöcke: ein Knopf versorgt seinen Träger stark, und ein stark versorgter Leiter löst jeden Befehlsblock daneben mit aus (bis 2026-09-25 feuerte jeder Knopf zwei Befehle). Geprüft statisch und im Lauf, siehe unten | Abschnittsliste, Anker `mob_spawn` |
| `armour` | Ständer: jede Rüstungsstufe (mit Schwert), Enderit mit leuchtendem / strahlendem / beidem Besatz, je Besatzmuster ein Enderit-Satz; Wand: alle Schmiedevorlagen, alle Besatzmaterialien, je Muster eine Spalte mit vier Teilen | Tab-Zeilen helmets…boots/swords, Register `trim_pattern`/`trim_material`, alle `SmithingTemplateItem`, alle Items mit `provides_trim_material` |
| `books` | jede Verzauberung (Mod, dann Vanilla, auch Flüche) als Buch auf Höchststufe mit Schild | Register `enchantment` |
| `tools` | je Familie eine Zeile unverzaubert und eine max. verzaubert (Meißel, Baustäbe, Vorschlaghämmer, Spitzhacken, Schaufeln, Hacken, Äxte, Schwerter, Speere, Geräte), alte Spachtel; daneben alle Varianten des Entwickler-Tabs (jede exklusive Auswahl) | `ModItemGroupsContent.toolsRows`, `DevEnchantedTab` |
| `states` | **Kaputt neben heil** (B12): vorn Amboss mit Truhe voller Lade- und Reparaturmaterial (Amethystsplitter, Enderperlen, Echosplitter, Erfahrungsfläschchen, Barren, Weisheitskugeln), rissiger neben heilem Diamantblock, Hochofen mit rissigen Diamanten (→ Diamant), abgenutzter neben neuem Enderit-Ständer, drehbare Blöcke für den Rotator; Wand: Resonanzstab und Rotator leer/halb/voll mit Lade-Material, Echolot in allen drei Riss-Stufen und repariert, Mending-Zeile (rissiges Echolot, abgenutzte Spitzhacke, EP), rissiger Diamant → Kiesel → Diamant; Haltbarkeits-Tafeln: jedes abnutzbare Werkzeug, Gerät und Rüstungsteil fast verbraucht direkt neben dem neuen | `FeatureStations.states`, Tab-Zeilen `DURABILITY_ROWS` |
| `storage` | Bündel, Köcher, Rucksäcke: unverzaubert, alle Verzauberungs-Varianten, gefärbt (rot/limette/blau/gelb); Rucksäcke zusätzlich als Block; daneben abgestellte Bündel (jede Stufe, ein gefärbtes, ein blaues Vanilla-Bündel, je mit vier Items: schleichend hinsehen zeigt das oberste, Rechtsklick nimmt es) | Tab-Zeilen bundles_and_quivers/backpacks (bis 2026-10-02 las die Station die alten Zeilennamen bundles/quivers und zeigte Bündel und Köcher gar nicht), `PlacedBundles` |
| `food` | alle Mod-Lebensmittel plus Apfel, Goldapfel, verzauberter Goldapfel, Karotte, goldene Karotte zum Vergleich | Items mit `food` |
| `materials` | der ganze Tab SimpleMaterials mit Namensschildern | Tab MATERIALS |
| `placeables` | **Ablegen** (26.3): je Kleinteil aus dem Tag `placeable_small` (Steinkiesel, Feuersteinsplitter, Diamantkiesel, Nuggets, Barren, Vanilla-Teile) Rahmen, Schild und das abgelegte Teil auf dem Boden; die drei Eier aufgestellt; Eisenstab frei unter dem Himmel (nichts darüber; Knopf "Gewitter" auf der Steuerwand) | `FeatureStations.smallParts`, `PlacedEggBlock.Egg` |
| `arrows` | Befiederungstisch (B14, nur 26.3): ein Tisch zum Ausprobieren, dahinter alle 72 Pfeile, eine Zeile je Spitze | Tab ARROWS |
| `chisel` | je Meißel-Kette ein Turm (Block für Block) mit dem Startblock davor zum Meißeln; Rahmen mit dem nötigen Meißel (mit Constructor's Touch, wenn nur die Touch-Tabelle die Kette kennt), Schild mit Stufe, Länge, "Kreislauf" | Tabellen des höchsten Meißels (`getForwardMap`/`getTouchForwardMap`) |
| `inworld` | je In-World-Umwandlung eine Station: Umformen mit dem Vorschlaghammer (Block → Treppe → Stufe, Mod-Blöcke + Stein/Eichenbretter), Diamantblock zerschlagen, Maschinen-Aufwertung (Maschine, Nugget, schwächster passender Hammer), Schere an Wolle, abgelegte Besatzvorlage (26.3), Oktant im Kessel waschen | `InWorldTransformations`, `SledgehammerUpgrades`, `SledgehammerEntityInteraction`, `OctantCauldronWash` |
| `templates` | abgelegte Schmiedevorlagen: vorn vier aufwertbare Besatzvorlagen auf dem Boden zum Draufhauen (Hammer + Glowstone/Leuchttinte, fünf Schläge, Hinweis-Funken in der Nähe), dahinter Netherit-, schlichte und Enderit-Aufwertungsvorlage sowie die leuchtende und strahlende Vorlage je auf dem Boden und an der Wand, zuletzt eine Blaupause auf dem Boden, an der Wand und unter der Decke; die Stapel kommen per `TcOp.Fill` in die Block-Entity | `PlacedTemplates`, `PlacedTemplateBlock`, `PlacedPlate`, `SledgehammerEntityInteraction` |
| `blocks` | Musterwand des Tabs SimpleBuilding (je Block eine Säule mit Namensschild), Schachbretter zusätzlich als Bodenflächen, Schwebesand/-kies frei schwebend, Levitationssand/-kies unter Glas | Tab BUILDING_BLOCKS |
| `lightroom` | geschlossener Dunkelraum mit Baulichtern und Tür; Monster dürfen trotz Licht spawnen (Knöpfe auf der Steuerwand) | - |
| `machines` | je Ofen-Familie und Stufe eine laufende Kette: Truhe → Trichter (gleiche Stufe) → Ofen → Trichter → Truhe, Kohle von der Seite; Kolben aller Stufen mit Hebel (Reihe rechts neben den Maschinen an der Gangkante; bis 2026-09-25 stand sie hinter der Rückwand und war vom Gang aus unsichtbar): 13 Steine (Vanilla schafft es nicht), Netherit-Kolben zerbricht, Enderit-Kolben vor verstärktem Tiefenschiefer mit Redstoneblöcken in der Truhe (Durchbruch) | Tab-Zeilen hoppers, furnaces, smokers, blast_furnaces, pistons |
| `ores` | Detektor-Feld: je Wirtsgestein (Stein, Tiefenschiefer, Netherrack, Endstein) ein Block, darin jedes Erz einzeln in 3/7/11/15 Blöcken Abstand; Schild mit Klasse, Abstand, Reichweite; Detektor im Rahmen | alle Blöcke `*_ore` und Antiker Schutt, `OreDetectorItem.classify` |
| `planning` | alle Oktanten, Zeile Bauplanung; je Baustab-Modus (Linie, Brücke, Bedecken, Farbpalette, Oktant füllen, Dach) eine Fläche mit passendem Stab; Brücke mit Graben (auf flachem Boden gibt es nichts zu überbrücken), Dach mit Prisma-Oktant im Rahmen und Truhe voll Eichentreppen/-stufen; Beispielhaus mit Kartentisch, Oktant mit gesetzter Auswahl und daraus beim Bau gescannter Blaupause | Tab-Zeilen, `BlueprintScanner` |
| `mining` | gemischte Wand (Vielseitigkeit), Eisen- und Kohleader (Aderabbau), lange Steinwand (Tunnelabbau), je mit passend verzaubertem Werkzeug | Verzauberungen, Tab-Zeilen |
| `enchants` | je Mod-Verzauberung, die keine andere Station vorführt (Break Through, Deep Pockets, Air Jump, Drawer, Fast Chiseling, Funnel, Kinetic Protection, Master Builder, Override, Radius, Range), ein Pfosten mit dem höchsten passenden Gegenstand, damit verzaubert, und Hinweis; davor eine Prüfwand (Stein/Erde/Bretter, zwei Lagen) oder für Bündel/Köcher eine Truhe mit Nachschub. Neue Mod-Verzauberungen erscheinen von selbst | Register `enchantment` minus `FeatureStations.SHOWN_ELSEWHERE` |
| `sinkdamper` | Fallturm für den Sinkdämpfer der Enderit-Rüstung (P8): Säule mit Leiter, Plattform in 12 Blöcken Höhe, Ständer mit vollem Enderit-Satz (das Kit legt ihn ins Inventar) - anziehen, hochklettern, im Fall schleichen; Sprinten bremst | `FeatureStations.sinkDamper` |
| `tweaks` | die aus Simple Tweaks uebernommenen Pads und Platten: alle Tweaks-Tab-Zeilen als Rahmen, davor Spawn-Teleporter I (50 s) und III (Enderit, 5 s), Elytra-Pad, Flypad I mit eigenem Flugfeld (4 × 4, 8 hoch, auf dem Boden mit Purpur markiert; kein anderes Pad liegt darin - bis 2026-09-28 stand es dicht neben dem Launchpad, wer das testen wollte, flog), Launchpad mit Truhe Windkugeln, Diamant-/oxidierte Kupferplatte an Lampen, Netherit-/Enderit-Platte mit Fass (Diamant) darunter, Leitstein mit Truhe (Echolot; Enderperlen braucht es seit 2026-09-27 nicht mehr), Trank-Pad I mit Truhe Wurf-/Verweiltraenke und Wasserflasche zum Leerwischen (und Lohen- und Endermankopf); Chunk-Loader nur im Rahmen (gesetzt wuerde er Chunks erzwingen) | `TweaksStation`, `TweaksItems.functionalRows` |
| `devices` | **Grundversorgung für neue Tab-Zeilen**: jede Zeile aus SimpleTools oder Maschinen & Lager, die kein Abschnitt eigens liest, erscheint hier als Rahmenzeile, Blöcke daraus zusätzlich auf dem Boden | Tab-Zeilen minus `KNOWN_TOOL_ROWS`/`KNOWN_FUNCTIONAL_ROWS` |
| `tab_tools`, `tab_building_blocks`, `tab_materials`, `tab_functional`, `tab_pads`, `tab_arrows` | **Item-orientierter Rundgang** (B12): je Kreativ-Tab eine Wand mit dem ganzen Tab in Anzeigereihenfolge (spaltenweise, sechs Reihen), mit Ausgabe-Knopf. Zählt **nicht** für die Abdeckung: jedes Item braucht trotzdem eine Station zum Ausprobieren (testorientierter Rundgang = alle anderen Abschnitte) | `ModItemGroupsContent.populate` je Tab, `TabBrowser` |
| `gallery` | jeder Mod-Block, den kein anderer Abschnitt setzt (etwa nur im Rahmen zeigt), einmal auf dem Boden mit Schild - neue Blöcke von selbst. Ausgenommen `TestCentreLayout.frameOnly`: Pads mit Wirkung auf die Umgebung (Elytra-Pad III gäbe auf 128 × 128 Blöcken Elytren, Teleporter versetzen, Chunk-Loader erzwingen Chunks, Kupferplatten altern) und die alten Flypad-, Spawn-Teleporter- und Elytra-Pad-Stufen | alle Mod-Blöcke minus gesetzte |
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
- **`command_blocks_are_isolated_and_every_station_has_its_give_button`** (ohne Welt): kein
  Befehlsblock berührt einen anderen oder dessen Knopf, kein Leiterblock berührt zwei Befehlsblöcke,
  Knopf- und Schildplatz sind frei; jede Station außer der Steuerwand hat ihren Ausgabe-Knopf in der
  vordersten Reihe nach Norden, ihr Kit füllt die Haupthand und passt ins Inventar (`tools`, `chisel`,
  `inworld`, `templates`, `planning` auch die Nebenhand); `/sbtestcentre give` ist registriert und ersetzt an einem
  Testspieler das Inventar (Erde vorher weg, alle Kit-Stapel da); der Fingerabdruck ist stabil und
  hängt am Ursprung; im Flypad-Flugfeld steht kein anderes Pad. Außerdem prüft der Abdeckungstest
  jetzt, dass jeder Mod-Block mit Item auch **gesetzt** ist (nicht nur gerahmt), außer `frameOnly`.
- **`broken_and_repaired_states_stand_side_by_side`** (ohne Welt): Resonanzstab und Rotator leer, halb und voll; Echolot in allen drei Riss-Stufen des Item-Modells und repariert; jedes abnutzbare Stück der Haltbarkeits-Zeilen fast verbraucht mit dem neuen direkt östlich daneben; rissiger und heiler Diamantblock, Amboss, Hochofen mit rissigen Diamanten, abgenutzter und neuer Ständer.
- **`every_creative_tab_has_its_item_browser_wall`** (ohne Welt): je Tab ein Abschnitt `tab_<id>` in `SECTION_IDS`, dessen Rahmen genau die Stapel des Tabs in Tab-Reihenfolge zeigen, mit Ausgabe-Knopf.
- **`every_feature_station_sets_up_its_scenario`** (ohne Welt): jede Mod-Verzauberung steht auf einem Gegenstand (kein Buch) in `mining`, `planning`, `chisel` oder `enchants`; jedes Kleinteil und jedes Ei liegt in `placeables` (26.3); jeder Eisenstab hat nichts über sich und kein Kupfer-Blitzableiter steht in der Zentrale; die Steuerwand hat den Gewitter-Knopf; der Fallturm ist mindestens 10 hoch, die Leiter reicht bis oben, ein Ständer trägt den vollen Enderit-Satz.
- **`each_button_runs_exactly_its_own_command_block`** baut die Zentrale bei x = z = 26000 (Chunks
  erzwungen), ersetzt jeden Befehl durch einen Zähler (`summon marker` an einer eigenen Stelle über
  dem Block) und drückt jeden Knopf der Steuerwand und jeden Ausgabe-Knopf wie von Hand: drei Ticks
  später muss genau sein eigener Zähler um eins gestiegen sein und kein anderer. Das fängt auch Wege,
  die der statische Test nicht kennt. Der frühere Knopftest las nur `isPowered()` direkt nach dem
  Druck und sah nicht, was wirklich lief.

Gegenprobe (2026-09-25, fabric-262): Enderit-Kolben aus der Kolben-Zeile gefiltert → Test 1 rot
("item simplebuilding:enderite_piston has no section …; block … is neither placed nor framed");
Oktant-Rahmen im Builder übersprungen → Test 2 rot ("item frames: planned 664, placed 662").

Bewusste Ausnahmen (`TestCentreLayout.EXCLUDED`): `creative_spacer` (Platzhalter der Tabs),
die drei Kolbenköpfe `reinforced_piston_head`, `netherite_piston_head`, `enderite_piston_head`
(technische Blöcke ohne Item; sie erscheinen in der Kolbenreihe, sobald ein Kolben ausfährt).

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
- **Neues Feature zum Ausprobieren** (Szenario statt nur Anschauen): Station in `FeatureStations` (oder `TestCentreSections`), dazu eine Prüfung in `every_feature_station_sets_up_its_scenario`. Gegenstände mit Zustand (Ladung, Risse, Haltbarkeit) gehören zusätzlich in `states`.
- **Neuer Abschnitt**: Methode `static TcCanvas name(TcContext ctx)` in `TestCentreSections` oder `FeatureStations`,
  Id in `TestCentreLayout.SECTION_IDS` (Reihenfolge = Reihenfolge in der Welt) und Eintrag in
  `TestCentreLayout.plan`. Die Steuerwand bekommt den Neu-bauen-Knopf automatisch.
- **Zeichenregeln**: lokale Koordinaten, y = 0 ist die Standhöhe, Blick der Besucher nach +z,
  Rückwände bei großem z (`TcCanvas.backWall`, `wallFrame`, `wallSign`, `frameGrid`, `rowsPanel`).
  Weltkoordinaten (Oktant-Ecken, Befehle) nie selbst ausrechnen: `octantFrame`/`blueprintFrame`
  verschieben ihre Ecken mit, Befehle bekommen den Ursprung von `TestCentreLayout.controls`.
- Beschriftungen immer über `TcText.t(key, englisch)` und den Schlüssel in beide Sprachdateien
  beider Linien.
- **Neuer Befehlsblock** (irgendwo): nur über `TcCanvas.command` setzen - das legt Glas darunter und
  den Kantenstein für das Schild darüber in die Planung, und die beiden Knopftests sehen ihn sofort.
- **Hände eines Kits** festlegen: `TestCentreKits.hands`; der Rest des Kits kommt von selbst.
- Nach Änderungen: `run.py --targets fabric-262,fabric-12111,fabric-263 --filter "simplebuilding:test_centre_*"`.

## Grenzen

- Entities in nicht geladenen Chunks sieht das Leeren nicht; wer weit weg steht, bekommt beim
  Neubau doppelte Rahmen. Beim Bauen in der Zentrale stehen (Sichtweite ≥ 10 Chunks reicht). Der
  automatische Neubau einer veralteten Zentrale setzt dich deshalb erst an den Eingang und wartet 3 s.
- Ein Kit ersetzt das Inventar ohne Rückfrage - die Zentrale ist eine Testwelt.
- Der Bau schaut nicht nach Schutzgebieten - er ist ein Entwicklerwerkzeug für Testwelten.
- Trichter und Öfen laufen nach dem Bau sofort los; der Knopf "Neu bauen: Maschinen" setzt sie zurück.
