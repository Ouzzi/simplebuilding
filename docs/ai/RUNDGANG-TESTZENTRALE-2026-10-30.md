# Rundgang durch die Testzentrale (TZ22-9, Stand 30.10.2026, Agent a-jei30)

Zweck: nach einem Feature-Lauf die Zentrale neu bauen und in einer festen Reihenfolge durchgehen. Die Zentrale ist aus
Code gebaut (`/sbtestcentre`), Details zu jedem Abschnitt stehen in `docs/TESTZENTRALE.md`; diese Datei ist der
**Laufweg** und die **Prüfliste**. Nur 26.3 Fabric.

## Vorbereitung
1. Welt `SB-Testzentrale` (Flachland, Kreativ, Cheats) öffnen; weicht der Fingerabdruck ab, baut sie sich nach 3 s neu.
   Sonst `/sbtestcentre build` (alles). Für einen kurzen Lauf: `/sbtestcentre build only workshop,states`.
2. `/sbtestcentre coverage` muss „Every mod item has a place“ melden (Rot = Item in `unsorted`).
3. Jede Station: vorn links der **Ausgabe-Knopf** (Kit), die Steuerwand (`controls`) hat Tag/Nacht, Wetter, Mobs-Knöpfe.

## Neu: Haltestellen
Jeder gebaute Abschnitt ist eine Haltestelle: `/sbtestcentre tp section/<id>` stellt dich in den Gang vor den Abschnitt,
Blick nach Süden (Tab-Vervollständigung zeigt alle). Mobs weiter über `tp mob/<mod>/<mob>`. Absicherung: Gametest
`the_layout_is_compact_with_low_walls` verlangt für jeden Abschnitt eine Haltestelle vor ihm.

## Laufweg (Reihenfolge = Bezirke von `TestCentreLayout.DISTRICTS`)
| Nr. | Bezirk (`build only …`) | Haltestellen (`tp section/…`) | Was prüfen |
|---|---|---|---|
| 1 | Eingang & Werkzeuge (`workshop`) | `controls`, `tools`, `chisel`, `planning`, `mining`, `enchants` | Knöpfe der Steuerwand tun genau ihr Ding; Werkzeugzeilen unverzaubert/verzaubert; Meißel-Türme; Baustab-Modi; Vielseitigkeit/Tunnelabbau; Mod-Verzauberungen am Pfosten |
| 2 | Zustände & Umwandlungen (`states`) | `states`, `inworld`, `upgrades`, `templates`, `placeables`, `books` | kaputt neben heil; Hammer-Umformen/Zerschlagen; Aufwertungsketten nach Stufen (Schilder = Live-Daten); Vorlagen-Aufwertung; Kleinteile ablegen; Verzauberbücher |
| 3 | Baublöcke (`blocks`) | `blocks`, `woodwork`, `simpleend`, `chess`, `astral`, `lightroom`, `ores` | Block-Ausstellung; Holzwerk (Röhren, Platten, Kessel); Simple End (Holzsets, Knospen, End-Gras); Schach 3D/flach; Astral-Tisch Stufe 50; Dunkelraum mit Baulichtern; Erz-Detektor-Feld |
| 4 | Technik & Lager (`technics`) | `storage`, `ui`, `devices`, `machines`, `crucible`, `music`, `vehicles` | Rucksäcke/Bündel; UI-Blöcke + Befüll-Knopf + Tooltip-Truhe; Redstone-/Schienenstrecke (Astral/Nihil); laufende Ofenketten, Kolben; Tiegel/Fass/Eimer; Plattenspieler/Verstärker; Lore/Boot-Strecken |
| 5 | Kampf & Bewegung (`combat`) | `armour`, `archery`, `arrows`, `sinkdamper`, `riding`, `tweaks`, `deathvault`, `respawn`, `food`, `materials` | Rüstungsstufen/Besatz; Bögen, Puppen mit Köpfen; Befiederungstisch; Fallturm; Reiten; Pads/Platten; Todestresor; Respawn; Essen; Materialien |
| 6 | Module (`modules`) | `modules` | je Modul eine Bucht, „Weitere“ leer oder begründet |
| 7 | Mobs (`mobs`) | `mobs`, `tp mob/<mod>/<mob>` | je Mob Zelle: still/aktiv spawnen, entfernen, Drops |
| 8 | Strukturen (`structures`) | `structures` | jedes Strukturteil einmal, End-Wracks/-Brunnen/-Wege/Feldküchen |
| 9 | Crafting-Rezepte (`recipes`) | `recipes` | Crafter-Muster = Live-Rezept; Signal craftet |
| 10 | Katalog (`catalog`) | `tab_tools` … `tab_arrows` | je Kreativ-Tab eine Wand in Anzeigereihenfolge (zählt nicht für Abdeckung) |
| 11 | Rest (`rest`) | `gallery`, `unsorted` | `unsorted` leer |

## Was ohne Client prüfbar ist und was nicht
- Automatisch (Server, `fabric-263`/`integration-263`, Filter `simplebuilding:test_centre*`): Abdeckung, Gesamtbau gegen Planung,
  Knöpfe/Kits, Teilbau, Schienen, Wege-Schilder, UI-Befüllen, Rezepte, Aufwertungen, Layout + Haltestellen.
- **Nur im Client / Besitzer-Abnahme:** Optik der Stationen, Lesbarkeit der Schilder, Gefühl der Strecken. Bilder per
  Server-Render sind ohne Client nicht möglich; daher keine Screenshots in dieser Datei. Besitzer-Abnahme = Welt neu bauen
  und obige Tabelle einmal abgehen (Queue CL-6 / END-10 bleiben dafür offen, WP-34).
