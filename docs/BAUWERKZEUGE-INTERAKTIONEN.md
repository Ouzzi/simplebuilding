# Bauwerkzeuge: Zusammenspiel, Lücken, Vorschläge

Stand 2026-09-25 (HEAD `a6b7917`). Ursprünglich reine Analyse. **Nachtrag 2026-09-25 (Baustab-Paket):**
V2, V3, V4, V6, V7 und V8 sind umgesetzt, die betroffenen „heute"-Abschnitte (1.2, 1.3, 1.4, Teil 2)
beschreiben den neuen Stand; Zeilennummern der geänderten Stellen sind durch Methodennamen ersetzt.
Neu: `util/WandPlacement.java`, `util/WandUndo.java`, `blueprint/ShapeFill.java`,
`client/render/QuiverLayer.java`, Tests in `gametest/WandModeTests.java`.

**Belege.** Pfade ohne Präfix liegen unter `common/src/shared/java/com/simplebuilding/`. Die
Zeilen gelten für die 26.2-Linie. Laut `docs/FRAGEBOGEN-BAUWERKZEUGE.md` hat der 1.21.11-Spiegel
dieselbe Logik mit umbenannten APIs. Für dieses Dokument wurde nur 26.2 gelesen. Tags und Rezepte
stammen aus `src/main/generated/data/simplebuilding/…` (Datagen-Ausgabe), Handel aus
`src/main/resources/data/simplebuilding/villager_trade/…`.

Teil 1 und 2 beschreiben, was der Code **heute** tut. Teil 3 und 4 sind Vorschläge.

---

## Teil 1 – Interaktionsmatrix

### 1.1 Wer kann welche Verzauberung tragen

| Verzauberung | Träger (Item-Tag) | Beleg |
|---|---|---|
| Berührung des Konstrukteurs | Bündel (3), Köcher (4), Shulkerkiste, Rucksäcke, Meißel, Vorschlaghämmer, Baustäbe, Tempomesser, Erzdetektor, Magnet, Oktanten, Stock | `tags/item/constructors_touch_enchantable.json`, `enchantment/ModEnchantments.java:72-75` |
| Baumeister | Baustäbe, Bündel, Köcher (`extra_inventory_items`) + Rucksäcke | `tags/item/master_builder_enchantable.json`, `tags/item/extra_inventory_items.json` |
| Farbpalette | Baustäbe, Bündel, Köcher | `ModEnchantments.java:130` + `extra_inventory_items.json` |
| Trichter, Tiefe Taschen | Bündel, Köcher, Rucksäcke | `tags/item/funnel_enchantable.json`, `deep_pockets_enchantable.json` |
| Schublade | Bündel, Köcher | `ModEnchantments.java:154-156`, `bundle_enchantable.json` |
| Radius | Vorschlaghämmer, Erzdetektor | `tags/item/radius_enchantable.json` |
| Durchbruch, Übersteuerung | Vorschlaghämmer | `ModEnchantments.java:167-169,192-194`, `ModTags.java:34` |
| Reichweite | Meißel, alle Abbauwerkzeuge, Vorschlaghämmer, Oktanten | `tags/item/chisel_and_mining_tools.json`, `ModEnchantments.java:85-103` |
| Abdeckung, Brücke, Linear | Baustäbe | `ModEnchantments.java:216-250` |

Exklusiv (`tags/enchantment/exclusive_set/`): Baumeister, Farbpalette und Schublade schließen sich
gegenseitig aus (`builder_group.json`). Abdeckung schließt Brücke und Linear aus
(`cover_group.json`, `wand_modifier_group.json`).

### 1.2 Materialquellen des Baustabs

Suchreihenfolge beim Flächenbau (`items/custom/BuildingWandItem.java:247-269`) und bei der
Blaupause (`:393-406`), jeweils die erste Quelle mit passendem Material:

| # | Quelle | Bedingung | Beleg |
|---|---|---|---|
| 1 | Nebenhand: Blockstapel | immer | `:251`, `:289-295` |
| 1b | Nebenhand: Verstärktes/Netherit-/Enderit-Bündel | Baumeister auf **Stab oder Bündel** | `:298-303` |
| 2 | Hotbar 0–8 (Blöcke und Bündel wie oben) | immer | `:255-258` |
| 3 | Hauptinventar 9–35 (Blöcke und Bündel) | nur mit Baumeister auf dem **Stab** | `:261-266` |
| 4 | Getragener Rucksack (Brust-Slot) | nur mit Baumeister auf dem **Rucksack**, Baumeister am Stab reicht nicht | `:71-78`, `:268`, `:272-283` |

Folgen, die man nicht sofort sieht:

- **Ein Bündel mit Baumeister bei einem Stab ohne Baumeister** wird nur in Nebenhand oder Hotbar
  gelesen. Die Schleife über das Hauptinventar läuft nur mit Baumeister am Stab (`:261`).
- **Rucksack im Inventar** statt getragen: Er wird nie gelesen (`BackpackItem.java:126-129`).
  Der Stab verbaut ihn auch nie selbst als Block (`BuildingWandItem.java:62-69`).
- **Köcher** sind technisch Bündel (`QuiverItem.java:29`, `extends ReinforcedBundleItem`). Der
  Stab durchsucht sie also mit. Weil sie nur Pfeile annehmen (`QuiverItem.java:52-57,68-71`),
  liefern sie nie Material.
- **Vanilla-Bündel und Shulkerkisten** werden nicht gelesen. Geprüft wird nur
  `instanceof ReinforcedBundleItem` (`:298`). Eine Shulkerkiste in der Hand gilt als BlockItem
  und wird samt Inhalt verbaut (`:67-69`, Defekt D1 im Fragebogen).
- **Welcher Block verbaut wird**, legt der erste Fund beim Klick fest (`useOn`, `BuildBlockRawId`).
  Danach sucht jede Position genau diesen Block (`findSpecificMaterial`). Mit **Farbpalette** nimmt
  jede Position den Eintrag, den `paletteIndex(pos, size)` ihr über die noch vorhandenen Stapel
  zuweist - derselbe, den die Vorschau zeigt. Der angeklickte Block bestimmt das Material nicht.
- **Gesetzt wird wie von einem Spieler** (`util/WandPlacement#stateFor`): je Stelle ein
  `BlockPlaceContext` mit der relativen Trefferposition des echten Klicks, also Treppen nach
  Blickrichtung und Klickhöhe, Stämme nach Klickseite, Stufen oben/unten, Zäune verbunden; ist der
  angeklickte Block dieselbe Sorte, wird seine Ausrichtung übernommen (`copyOrientation`). Danach
  `afterPlace`: Block-Entity-Daten, Komponenten, `setPlacedBy` (Türen, Betten). Stellen ohne Halt
  oder mit Wesen im Weg bleiben frei. `BlockItem#place` selbst wird nicht gerufen, deshalb greift
  das Rucksack-Nachfüllen durch Berührung des Konstrukteurs beim Stab weiterhin nicht
  (`mixin/BlockItemMixin.java:31-52`). Eine Shulkerkiste als Material wird jetzt samt Inhalt
  gesetzt (D1 behoben).

### 1.3 Paarweise Matrix – Baustab, Blaupause, Oktant

| Kombination | Was heute passiert | Beleg |
|---|---|---|
| Stab (Haupthand), Klick auf Block | Baut eine quadratische Fläche vor der Klickseite, ausgerichtet wie von Hand gesetzt (1.2). Ring für Ring alle 4 Ticks, je Block 1 Haltbarkeit. Durchmesser je Stufe: 3/5/7/9/11/13. Belegte Stellen werden übersprungen, Bau stoppt, wenn Material fehlt. | `BuildingWandItem#useOn`, `#inventoryTick`, `Plan` |
| Stab, Schleichen + Rechtsklick in die Luft | **Rückgängig**: nimmt die letzte Bau-Aktion zurück (Fläche, Linie, Brücke, Abdeckung, Oktant-Füllung, Dach, Blaupause). Nur Blöcke, die noch dieselben sind, keine Behälter; Items ins Inventar, Überlauf fällt. Haltbarkeit und Hunger nicht erstattet, Kreativ gibt nichts zurück. Nur dieselbe Sitzung (am Spieler-Objekt), höchstens 65 536 Blöcke. Laufende Bauten werden vorher angehalten. | `util/WandUndo.java`, `BuildingWandItem#use` |
| Stab (Nebenhand) | Ein Klick tut nichts (nur die Haupthand startet). Ein laufender Bau läuft auch in der Nebenhand weiter. Wandert der Stab aus beiden Händen, bricht der Bau ab. | `:469`, `:516` |
| Stab im Kreativmodus ohne Blöcke | Nichts: Mangels Material wird der Zielblock Luft, und der Tick bricht ab. | `:486-488`, `:539-543` |
| Stab + **Berührung des Konstrukteurs** | Öffnet mit der Einstellungstaste das Stab-Menü (Radius, Achse). Ohne die Verzauberung öffnet es nicht, und der Stab baut immer in voller Stufengröße. | `src/main/java/com/simplebuilding/SimplebuildingClient.java:123-133`, `neoforge/src/main/java/com/simplebuilding/neoforge/SimplebuildingNeoForgeClient.java:201-208`, `BuildingWandItem.java:142-146` |
| Stab + **Baumeister** | Erweitert die Suche auf das Hauptinventar und schaltet **alle** Verstärkten Bündel als Quelle frei. Den Rucksack schaltet er **nicht** frei. | `:261-266`, `:301`, `:71-78` |
| Stab + **Farbpalette** | Vorschau und Bau mischen gleich: jede Stelle nimmt `paletteIndex(pos, size)` (Hash der ganzen Position, auch Böden gemischt) über die mitgeführten Stapel. Geht ein Stapel aus, schrumpft die Palette. | `BuildingWandItem#paletteIndex`, `#previewOf`, `#inventoryTick` |
| Stab + **Linear** | Mit Schleichen: Linie von der Klickseite weg, doppelter Flächen-Durchmesser lang (6…26), endet vor dem ersten belegten Block. Ohne Schleichen: Fläche, Ringverzögerung 2 statt 4 Ticks. | `Plan.forClick`, `lineLength` |
| Stab + **Abdeckung** | Fläche nur vor Blöcken der angeklickten Sorte, zusammenhängend mit der Mitte (4er-Nachbarschaft, Flutfüllung im Radius). Achsen-Einstellung gilt nicht. | `Plan.coverRegion` |
| Stab + **Brücke** | Rechtsklick in die Luft (ohne Schleichen): vom Block unter den Füßen geradeaus in Blickrichtung, gleiche Länge wie Linear, endet vor dem ersten belegten Block. Vorschau beim Blick in die Luft. | `BuildingWandItem#use`, `Plan.forBridge` |
| Stab (Haupthand) + **Blaupause** (Nebenhand) | Blaupausen-Baumodus: Das Bauwerk entsteht vor dem Spieler, Strg+Mausrad dreht es. Material kommt aus denselben Quellen, nur nach Item statt nach Block. 1 Haltbarkeit je Block. Die Blaupause muss signiert sein, ihre längste Kante darf 16/32/48/64/128/256 je Stabstufe nicht überschreiten. Fehlt Material, warnt der erste Klick nur, ein zweiter innerhalb von 3 s baut. Große Bauten laufen als Auftrag und überstehen Logout. Schutzprüfungen laufen an **jeder** Position. | `BuildingWandItem.java:471-473`, `blueprint/BlueprintBuilder.java:40-62`, `:340-345`, `:417-431`, `:563`, `:584-590`, `:531`, `:639`, `blueprint/BlueprintTiers.java:12-36` |
| Blaupause allein (Rechtsklick) | Öffnet den Editor. In der Nebenhand bei einem Stab in der Haupthand passiert nichts (PASS). | `items/custom/BlueprintItem.java:49-58` |
| Vorschau Stab + Blaupause | Geisterblöcke des Bauwerks. Stellen ohne Material rot, nach einem Warnklick pulsierend. | `client/render/BuildingWandPreviewRenderer.java:69-82` |
| **Oktant** allein | Klick = Pos1, Schleichen + Klick = Pos2, je 1 von 128 Haltbarkeit. Schleichen + Luftklick setzt zurück. Gesperrt heißt: keine Änderung. | `items/custom/OctantItem.java:30`, `:66-98`, `:101-114` |
| Oktant + **Berührung des Konstrukteurs** | Die Figur wird gefüllt dargestellt statt nur mit Eckwürfeln. Die Config `invertOctantSneak` kehrt das um. | `client/render/BlockHighlightRenderer.java:168-171` |
| Oktant in der Nebenhand | Hervorhebung und Klicks gehen. Manager (Einstellungstaste) und Mausrad nur in der Haupthand. | `BlockHighlightRenderer.java:87-92`, `mixin/client/MouseMixin.java:34`, `SimplebuildingClient.java:126` |
| **Oktant → Blaupause** | Am Kartentisch: Oktant oben, Blaupause unten. Gescannt wird genau die Figur (Quader, Zylinder, Kugel, Pyramide, Prisma …). Der Tisch muss höchstens 32 Blöcke von der Box entfernt stehen. Der Oktant bleibt liegen, und seine Auswahl wird im Tisch voll angezeigt. | `blueprint/BlueprintCartography.java:15-33`, `blueprint/BlueprintScanner.java:18-32`, `:83-99`, `BlockHighlightRenderer.java:80-85` |
| **Stab (Haupthand) + Oktant mit Auswahl (Nebenhand)** | **Figur füllen:** Vorschau der gefüllten Figur (fehlend rot) unabhängig vom Fadenkreuz, Klick auf einen Block baut über den Blaupausen-Planer (Zwei-Klick-Regel, Staffelung, Haltbarkeit je Block, Abbruch beim Weglegen, nicht gespeichert). Material ab Hotbar, Farbpalette gemischt. Hohl = Hülle, Reihenfolge = Schichten Grundfläche→Spitze / aufwärts / abwärts, Ebenenmodus = je Klick die nächste unfertige Schicht. Kante je Stufe 16…256, höchstens 4 194 304 Stellen. **Dach:** Treppe als Material + Prisma/Pyramide mit Spitze oben → Treppen zum First, First aus unteren Stufen (`*_stairs`→`*_slab`), innen leer. | `blueprint/ShapeFill.java`, `BlueprintBuilder#buildLayout`, `ListLayout` |
| Oktant in der Haupthand + Stab | Kein Zusammenspiel (nur die Nebenhand zählt). | `BuildingWandItem#useOn` |

### 1.4 Paarweise Matrix – Bündel, Rucksack, Köcher

| Kombination | Was heute passiert | Beleg |
|---|---|---|
| Bündel-Stufen | Kapazität: Verstärkt 1,5×, Netherit 3×, Enderit 4,5× eines Vanilla-Bündels (96/192/288 Items bei 64er-Stapeln). | `items/custom/ReinforcedBundleItem.java:472-488`, `:496` |
| Bündel + **Baumeister**, Rechtsklick auf Block | Setzt den ausgewählten (praktisch den ersten) Blockeintrag über `BlockItem#useOn`, also wie ein Spieler, mit Ausrichtung, und nimmt 1 heraus. | `:157-204` |
| Bündel + Baumeister + **Farbpalette** | Geht nicht, die beiden schließen sich aus (`builder_group.json`). Der Code würde sonst zufällig über **alle** Einträge würfeln (`:171-174`, Defekt D12). | — |
| Bündel, Rechtsklick in die Luft | Wirft den ausgewählten oder ersten Stapel ab, sofern er kein Block ist. Bei einem Block: FAIL. | `:207-230` |
| Bündel + **Tiefe Taschen** I/II | Kapazität ×2/×4. | `:532-538` |
| Bündel + **Schublade** | Kapazität × (16+Stufe)/8, höchstens 5 Sorten. | `:39`, `:505-529`, `:308` |
| Bündel + **Trichter** I/II | Saugt Items beim Berühren ein: I nur Sorten, die schon drin sind, II alles. Bündel in der Hand ohne Aufhebeverzögerung, im Inventar erst danach. Nie beim Schleichen. | `:579-605`, `mixin/ItemEntityMixin.java:60-82` |
| Bündel + **Berührung des Konstrukteurs** | **Keine Wirkung.** Es gibt nur ein Modell-Prädikat. | `client/property/EnchantmentModelProperty.java:49` (einzige Fundstelle außer Tag) |
| Bündel + Stab | Siehe 1.2: Quelle mit Baumeister auf einem der beiden. | `BuildingWandItem.java:298-303` |
| Bündel mit Baumeister setzt + getragener Rucksack mit Berührung des Konstrukteurs | Kein Nachfüllen. Das Bündel platziert eine Kopie, der Handstapel ist ein anderer. | `mixin/BlockItemMixin.java:20-23`, `:47-50` |
| **Rucksack**-Stufen | 9/18/33/50 Slots (Reihen + Zusatzspalten), Rüstung 1–4. Nur getragen oder als abgestellter Block zu öffnen. Schleichen + Klick stellt ihn ab, ohne Schleichen wird er angezogen. | `items/custom/BackpackTier.java:3-24`, `items/custom/BackpackItem.java:26-39`, `:59-68` |
| Rucksack + **Baumeister** (getragen) | Letzte Materialquelle für Stab und Blaupause. | `BuildingWandItem.java:76-78`, `:268`, `:404` |
| Rucksack + **Berührung des Konstrukteurs** (getragen) | Wird der Blockstapel in der Hand beim normalen Platzieren leer, kommt ein gleicher Stapel aus dem Rucksack nach (höchstens ein normaler Stapel, nicht im Kreativmodus). Wirkt nur beim Platzieren von Hand, nicht beim Stab. | `BackpackItem.java:277-294`, `BlockItemMixin.java:36-52` |
| Rucksack + **Trichter** (getragen) | Saugt nach allen Bündeln und erst nach der Aufhebeverzögerung ein. I nur vorhandene Sorten, II alles. | `BackpackItem.java:246-268`, `ItemEntityMixin.java:84-88` |
| Rucksack + **Tiefe Taschen** | Stapelgröße je Slot ×2/×4. | `BackpackItem.java:145-172` |
| Rucksack in Bündel/Shulker/Rucksack | Nicht möglich. | `BackpackItem.java:70-73`, `:131-134` |
| **Köcher**-Stufen | 64/96/128/192 Pfeile, nur Pfeile. | `items/custom/QuiverItem.java:52-98` |
| Köcher, Pfeilsuche des Bogens | Reihenfolge: Nebenhand → Brust-Slot → Hotbar → mit **Berührung des Konstrukteurs** auch Köcher im Hauptinventar. | `QuiverItem.java:162-188`, `:221-224` |
| Köcher + Baumeister / Farbpalette | Lässt sich auftragen, bewirkt aber nichts: `useOn`/`use` geben PASS zurück. | `QuiverItem.java:38-46` |
| **Köcher + Rucksack** | Schließen sich aus. Beide belegen den Brust-Slot. | `BackpackItem.java:126-129`, `QuiverItem.java:167-171`, `items/ModItems.java:445-455` |
| Köcher getragen | Wird auf dem Rücken gezeichnet: das Item-Modell, etwas tiefer gestreckt, schräg von der rechten Schulter zur linken Hüfte (Spieler, Mannequin; alle Loader, beide Linien). Rüstungsständer zeigen ihn nicht. | `client/render/QuiverLayer.java` |

### 1.5 Paarweise Matrix – Vorschlaghammer, Meißel

| Kombination | Was heute passiert | Beleg |
|---|---|---|
| Hammer, Abbau | 3×3 senkrecht zur Trefferseite (Grundradius 1). Schleichen = genau ein Block. | `items/custom/SledgehammerItem.java:370-435`, `:388-391`, `util/SledgehammerUsageEvent.java:31` |
| Hammer + **Radius** / **Durchbruch** | Radius + Stufe (Radius I = 5×5). Tiefe + Stufe (bis 2). | `:398-402` |
| Hammer + **Übersteuerung** ≥ II | Richtiges Werkzeug auch für Axt-, Schaufel- und Hacken-Blöcke. | `:103-120` |
| Hammer, Rechtsklick halten | Umformen: Block → Treppe → Stufe, 1 Haltbarkeit. Diamantblock → 81 Diamantklumpen. | `:142-179`, `:196-247`, `:324-363`, `:77`, `:437-464` |
| Hammer + Schleichen + **Berührung des Konstrukteurs** | Rückwärts: Stufe → Treppe → Block, 2 Haltbarkeit. Ohne die Verzauberung tut Schleichen + Rechtsklick nichts. | `:239-241`, `:289-302` |
| Hammer + **Nugget in der Nebenhand** | Maschinen-Aufwertung Verstärkt → Netherit → Enderit (5 s, 5 Schläge), nur wenn der Block passt. Sonst gilt das normale Verhalten. | `:153-158`, `util/SledgehammerUpgrades.java` (Klassen-Javadoc) |
| Hammer + Oktant / Stab / Blaupause | **Kein Zusammenspiel.** Die Hammer-Hervorhebung gibt es nur in der Haupthand. | `BlockHighlightRenderer.java:95-102` |
| Meißel / Spachtel | Formt den Block um (Tabelle je Stufe). Spachtel standardmäßig rückwärts, Meißel + Schleichen rückwärts (teurer). Abklingzeit, **Schnelles Meißeln** kürzt sie um 30 % je Stufe. | `items/custom/ChiselItem.java:294-328`, `:423-453`, `:471-485` |
| Meißel + **Berührung des Konstrukteurs** | Nutzt die erweiterten Tabellen (`touchForward`/`touchBackward`, u. a. Beton ↔ Betonpulver bei Netherit). | `:429-453`, `:645-656` |
| Meißel/Hammer nach dem Stab | Nur nacheinander: Der Stab setzt Standardzustände (1.2), Meißel und Hammer formen danach Block für Block um und richten dabei am Spieler aus. | `SledgehammerItem.java:301,307`, `ChiselItem.java:466-467` |
| **Reichweite** | +2 Blockreichweite je Stufe (Attribut, Haupthand). Gilt für Meißel, Hammer, Abbauwerkzeuge und Oktant, **nicht** für den Stab. | `ModEnchantments.java:85-103`, `tags/item/chisel_and_mining_tools.json` |

### 1.6 Berührung des Konstrukteurs im Überblick

| Träger | Wirkung | Beleg |
|---|---|---|
| Baustab | Schaltet das Stab-Menü frei (Radius/Achse) | Client-Dateien, siehe 1.3 |
| Oktant | Zeigt die Figur gefüllt | `BlockHighlightRenderer.java:168-171` |
| Meißel | Erweiterte Umformtabellen | `ChiselItem.java:429-453` |
| Vorschlaghammer | Rückwärts umformen beim Schleichen | `SledgehammerItem.java:289-302` |
| Rucksack (getragen) | Füllt die Hand beim Platzieren nach | `BackpackItem.java:277-294` |
| Köcher | Pfeile auch aus dem Hauptinventar | `QuiverItem.java:221-224` |
| Magnet / Erzdetektor | Größere Reichweite / weniger Signalverlust | `MagnetItem.java:87`, `OreDetectorItem.java:233` |
| Stock | Schaltet die erste Blockeigenschaft weiter | `util/ConstructorsTouchInteraction.java:14-44` |
| Bündel, Shulkerkiste | **nichts** (nur Modell) | `EnchantmentModelProperty.java:49` |

---

## Teil 2 – Wiki-Kapitel-Entwurf: „Wie die Bauwerkzeuge zusammenspielen"

> Entwurf. Beschreibt nur heutiges Verhalten. Bei Änderungen aus Teil 3 mitziehen.

### Drei Aufgaben, drei Werkzeuge

SimpleBuilding teilt das Bauen in drei Schritte: **planen**, **setzen**, **nacharbeiten**.

- **Planen: der Oktant.** Mit dem Oktanten markierst du zwei Ecken (Klick und Schleichen + Klick).
  Im Manager wählst du eine Figur: Quader, Zylinder, Kugel, Pyramide, Prisma, Rechteck oder
  Ellipse. Das HUD zeigt Maße, Fläche oder Volumen. Mit *Berührung des Konstrukteurs* siehst du
  die Figur als gefüllten Körper. Der Oktant selbst setzt keine Blöcke.
- **Setzen: Baustab, Blaupause und Bündel.**
  - Der **Baustab** füllt mit einem Klick eine Fläche vor der angeklickten Seite. Die Fläche
    wächst Ring für Ring, von 3×3 (Kupfer) bis 13×13 (Enderit).
  - Die **Blaupause** hält ein ganzes Bauwerk fest. Mit dem Baustab in der Haupthand und der
    Blaupause in der Nebenhand baust du es beliebig oft wieder auf.
  - Das **Verstärkte Bündel mit Baumeister** ist eine Palette zum Setzen per Hand.
- **Nacharbeiten: Meißel und Vorschlaghammer.** Der Meißel wandelt Blöcke in ihre Varianten um,
  z. B. Stein zu Steinziegeln zu gemeißelten Steinziegeln, die Spachtel wieder zurück. Der
  Vorschlaghammer baut 3×3 ab, formt Blöcke zu Treppen und Stufen um, zerschlägt Diamantblöcke und
  wertet Maschinen auf.

### Vom Oktanten zur Blaupause

1. Markiere das fertige Bauwerk mit dem Oktanten. Die Figur zählt, eine Kugel scannt also nur die
   Kugel.
2. Lege Oktant (oben) und eine leere Blaupause (unten) in einen Kartentisch. Er darf höchstens 32
   Blöcke vom Bauwerk entfernt stehen. Rechts kommt die gefüllte Blaupause heraus, der Oktant bleibt
   liegen.
3. Signiere die Blaupause im Editor.
4. Nimm den Baustab in die Haupthand und die Blaupause in die Nebenhand. Die Vorschau zeigt das
   Bauwerk, Strg+Mausrad dreht es. Ein Klick baut.

Die Stufe des Stabs begrenzt die Größe: Die längste Kante darf 16 (Kupfer), 32, 48, 64, 128 oder
256 Blöcke (Enderit) messen. Fehlt Material, warnt der erste Klick, und die fehlenden Stellen
leuchten rot. Ein zweiter Klick innerhalb von drei Sekunden baut alles, was da ist. Die Lücken
lassen sich später mit demselben Klick schließen. Große Bauwerke entstehen über mehrere Ticks und
laufen nach Logout oder Neustart weiter, sobald du Stab und Blaupause wieder hältst.

### Woher der Baustab sein Material nimmt

Der Stab sucht der Reihe nach:

1. **Nebenhand.** Ein Blockstapel dort legt fest, was gebaut wird.
2. **Hotbar.**
3. **Restliches Inventar**, nur wenn der Stab *Baumeister* trägt.
4. **Getragener Rucksack**, nur wenn der **Rucksack** *Baumeister* trägt.

**Verstärkte Bündel** zählen als Quelle, wenn der Stab **oder** das Bündel *Baumeister* hat. Ohne
Baumeister am Stab müssen sie dafür in Nebenhand oder Hotbar liegen. Köcher, Vanilla-Bündel und
Shulkerkisten liefern kein Material.

Der erste Fund entscheidet, welcher Block gebaut wird, nicht der angeklickte Block. Der Stab setzt
Blöcke so, wie du sie selbst setzen würdest: Treppen nach Blickrichtung und Klickhöhe, Stämme nach
der Klickseite, Stufen oben oder unten. Klickst du auf einen Block derselben Sorte, übernimmt er
dessen Ausrichtung. Schleichen + Rechtsklick in die Luft macht die letzte Aktion rückgängig.

### Taschen, die beim Bauen helfen

| Tasche | Verzauberung | Hilft beim Bauen, weil … |
|---|---|---|
| Verstärktes Bündel | Baumeister | du direkt aus dem Bündel setzt (Rechtsklick auf Block) und der Baustab es anzapft |
| Verstärktes Bündel | Trichter | abgebaute Blöcke von selbst hineinwandern |
| Verstärktes Bündel | Tiefe Taschen / Schublade | mehr Material in einen Slot passt |
| Rucksack (getragen) | Baumeister | Baustab und Blaupause daraus bauen |
| Rucksack (getragen) | Berührung des Konstrukteurs | dein Blockstapel beim Setzen von Hand nachgefüllt wird |
| Rucksack (getragen) | Trichter / Tiefe Taschen | er einsammelt bzw. mehr pro Slot hält |

Rucksack und Köcher belegen beide den Brust-Slot, du trägst also immer nur eins von beiden.

### Berührung des Konstrukteurs, kurz

Sie macht aus jedem Bauwerkzeug die „Profi-Version": Stab-Menü (Radius, Achse), gefüllte
Oktant-Figur, erweiterte Meißeltabellen, Rückwärts-Umformen mit dem Hammer beim Schleichen,
Nachfüllen aus dem Rucksack. Auf Bündeln und Shulkerkisten bewirkt sie derzeit nichts.

### Stab und Oktant

Stab in die Haupthand, Oktant mit beiden Ecken in die Nebenhand: Die Vorschau zeigt die gefüllte
Figur, ein Klick baut sie (Hohl, Ebenenmodus und Reihenfolge des Oktanten gelten). Mit Treppen als
Material wird aus Prisma oder Pyramide ein Dach: Figur mit Spitze nach oben (Ausrichtung +Y), und
der **erste Baublock** in Suchreihenfolge (Nebenhand, Hotbar, mit Meisterbauer Inventar) ist eine
Treppe. Weil die Nebenhand den Oktanten hält, heißt das praktisch: Treppen in den vordersten
Hotbar-Platz mit Blöcken. Die Farbpalette schaltet das Dach nicht ab (seit 2026-09-25; vorher füllte
der Enderit-Stab aus dem Testzentrale-Kit die Figur bunt statt ein Dach zu decken). Die
Aktionsleiste sagt beim Klick, ob ein Dach entsteht („Dach: Eichentreppe an den Hängen, Eichenstufe
am First") oder warum nicht (erster Baublock ist keine Treppe).

### Stab und Brücke

Rechtsklick in die Luft mit der Verzauberung Brücke baut auf Höhe des Blocks unter den Füßen
geradeaus in Blickrichtung. Steht vor den Füßen noch Boden, beginnt die Brücke an dessen Kante
(bis zur Stablänge weit gesucht), und sie endet vor dem nächsten festen Block. Auf durchgehend
flachem Boden gibt es nichts zu überbrücken; die Aktionsleiste sagt dann „keine Lücke voraus"
(seit 2026-09-25; vorher blieb der Klick stumm, und die Brücke wirkte kaputt).

### Was (noch) nicht zusammenspielt

- Vorschlaghammer und Oktant: Der Hammer baut keine Oktant-Auswahl ab.

---

## Teil 3 – Vorschläge: den Baustab nützlicher machen

**Leitlinie des Besitzers:** Der Stab soll lästige Bauaufgaben übernehmen, wie es die Blaupause für
Wiederholungen tut, und das normale Bauen nicht ersetzen. Meißel und Hammer bleiben in ihrer
Kernfunktion unverändert.

**Beobachtung aus Teil 1:** Die nötige Infrastruktur liegt schon da. Die Blaupause hat einen
Auftrags-Planer mit Budget, Schutzprüfung an jeder Position, item-genauer Materialsuche,
Fehlstellen-Vorschau und Logout-Festigkeit (`BlueprintBuilder.java:40-62,340-345,417-431`).
`OctantShape` liefert Figuren als Prädikat (`BlueprintScanner.java:99`). Der Flächenbau des Stabs
nutzt davon **nichts** (rohes `setBlock`, `BuildingWandItem.java:563`). Die meisten Vorschläge
unten bestehen darin, den Stab auf diese Planer-Schiene zu setzen.

Aufwand: **S** = Stunden, **M** = 1–2 Tage, **L** = mehr. Risiko = was schiefgehen oder Spieler
stören kann.

> **Umsetzungsstand 2026-09-25:** V2 (Oktant-Figur), V3 (Dach), V4 (Abdeckung), V6 (Linie/Brücke),
> V7 (Setzen wie ein Spieler, per `BlockPlaceContext` je Stelle statt `BlockItem.place`) und V8
> (Rückgängig, nur die letzte Aktion, nur dieselbe Sitzung) sind umgesetzt. **V1 nicht:** Oktant-
> Füllung und Dach laufen über den Planer (`BlueprintBuilder.ListLayout`, `buildLayout`), der
> normale Flächenbau bleibt bei seinem Ring-Tick mit NBT-Zustand - rund 40 Spieltests samt
> Mutationsankern pinnen genau diesen Ablauf (Ring je Tick, `Active`, Zeitgeber), der Umbau hätte
> das Paket verdoppelt. D2 (Schutzprüfung je Stelle) gilt damit für Flächen, Linien und Brücken
> weiterhin nicht.

**V1 · Flächenbau über den Blaupausen-Planer.** Der normale Stabklick erzeugt ein Layout
(die Ringe) und läuft durch `BlueprintBuilder.Planner`.
- Nutzen: behebt D2 (Schutz an jeder Position), D4 (Bruch stoppt, `Planner.run` prüft
  `wandBroken`) und D11/D13 teilweise. Alle folgenden Modi bekommen Vorschau, Warnklick und
  Budget geschenkt. Ist die Grundlage für V2–V6.
- Aufwand: M. Risiko: Die Ring-Animation muss erhalten bleiben (Scheibe je Ring). Tests zum
  Stab-Timing müssen angepasst werden.

**V2 · Oktant-Figur füllen (Stab in der Haupthand, Oktant in der Nebenhand).** Füllt die
Oktant-Figur mit Material und nutzt die gespeicherten Optionen hohl/Schicht/Reihenfolge
(`OctantItem.java:48-56`, bisher ungenutzt). Größenlimit wie bei der Blaupause je Stabstufe.
- Nutzen: der größte Hebel. Fundamente, Böden, Wände, Kuppeln, Türme, Säulen. Genau die lästigen
  Aufgaben, bei denen niemand Block für Block setzen will. Nutzt vorhandene Teile (OctantShape,
  Planer, BlueprintTiers).
- Aufwand: M (mit V1), sonst L. Risiko: Kollision mit dem Nebenhand-Material (Quelle 1). Das
  Material muss dann aus der Hotbar kommen, das Wiki muss es sagen. Große Auswahlen: Figur-Deckel
  (Fragebogen 21) vorher lösen.

**V3 · Dach- und Treppenmodus.** Als Erweiterung von V2: Prisma und Pyramide werden mit Treppen
gefüllt, die außen nach unten zeigen, der First mit Stufen oder Vollblöcken.
- Nutzen: Dächer sind die meistgehasste Handarbeit im Survival. Kein vergleichbarer Mod macht es so
  einfach.
- Aufwand: M. Risiko: Ecken (Treppen-`shape`) und schräge Figuren. Mit „nur 45°" anfangen.

**V4 · Anbauen (Abdeckung wirksam machen).** Die Fläche folgt der angeklickten Oberfläche: Gesetzt
wird nur vor Blöcken **derselben Sorte** und zusammenhängend, wie bei Construction Wands (Vorschlag
13a im Fragebogen).
- Nutzen: Mauern um eine Schicht verdicken, Böden erweitern, Fassaden ausgleichen, ohne quadratische
  Überstände. Gibt *Abdeckung* endlich eine Wirkung.
- Aufwand: S–M (Flutfüllung auf der Ebene, begrenzt auf den Stab-Durchmesser). Risiko: gering.
  Der heutige Quadratmodus bleibt der Standard.

**V5 · Ersetzen-Modus.** Blöcke der angeklickten Sorte im Stab-Radius (oder in der Oktant-Figur)
werden gegen das Material getauscht. Die alten Blöcke wandern ins Inventar, Bündel oder in den
Rucksack (mit Trichter-Regeln).
- Nutzen: Umgestalten statt Abreißen und Neubauen. Die zweitlästigste Aufgabe beim Bauen.
- Aufwand: M. Risiko: Drops und Block-Entities (Truhen) ausschließen. Schutzprüfung nötig (V1).
  Darf den Hammer nicht ersetzen: nur „gleiche Sorte → Material", kein Freiabbau.

**V6 · Linie / Säule / Brücke (Linear und Brücke wirksam machen).** Linie entlang der
Flächennormalen, beim Schleichen waagerecht nach vorn (Brücke). Länge = Stab-Durchmesser × 2.
- Nutzen: Stützpfeiler, Zäune, Brücken über Schluchten. Beide Verzauberungen hätten einen Sinn
  (Fragebogen 14/15).
- Aufwand: S. Risiko: gering.

**V7 · Setzen wie ein Spieler (`BlockItem.place`).** Ausrichtung am Spieler, Komponenten bleiben,
Türen und Betten vollständig (Fragebogen 1).
- Nutzen: Treppen, Stämme und Säulen kommen richtig herum an, der Stab taugt damit für echte
  Details statt nur für Würfel. Behebt D1 größtenteils, und Rucksack-Nachfüllen würde gratis
  mitgehen.
- Aufwand: M. Risiko: `place` spielt eigene Geräusche und Ereignisse ab, das ist bei 169 Blöcken
  laut. Performance bei großen Füllungen: nur für den Flächenmodus nutzen, V2 bleibt beim
  Zustand-Setzen mit Rotation.

**V8 · Rückgängig (letzte 1–5 Stab-Aktionen).** Items zurück, wenn der Block noch unverändert
dasteht.
- Nutzen: nimmt die Angst vor großen Aktionen. Ohne Undo nutzt kaum jemand V2/V5 im Survival.
- Aufwand: L (Aufzeichnung je Spieler, Speicherung wie `BlueprintJobs`). Risiko: Missbrauch als
  „Abbau ohne Werkzeug". Nur eigene, unveränderte Blöcke zulassen, mit Zeitlimit.

**V9 · Kopieren/Einfügen ohne Kartentisch.** Stab + Oktant: Schleichen + Klick scannt die
Oktant-Figur in eine leere Blaupause im Inventar. Der Kartentisch bleibt der Weg ohne Stab.
- Nutzen: Wiederholungen spontan, z. B. ein Fenster, ein Pfeiler, ein Baumhaus-Modul.
- Aufwand: S–M (`BlueprintScanner.start` existiert). Risiko: macht den Kartentisch-Schritt
  überflüssig. Eventuell an den Enderitkern binden (Teil 4).

**V10 · Materialwahl sichtbar und klick-basiert.** Pick-Block-Logik: Material = angeklickter Block,
wenn vorhanden, sonst die heutige Reihenfolge. Actionbar-Zeile „7×7 · Stein ×243". Radius per
Schleichen + Mausrad für alle, Berührung des Konstrukteurs für Achse und Modi (Fragebogen 7, 10,
17).
- Nutzen: weniger Überraschungen („warum baut er Erde?"), schneller Radiuswechsel.
- Aufwand: S. Risiko: Mausrad-Konflikt mit der Blaupausen-Drehung (Strg) und dem Oktanten. Nur ohne
  Nebenhand-Blaupause auslösen.

**V11 · Symmetrie-Hilfe.** Eine gesetzte Spiegelebene. Jeder **von Hand** gesetzte Block im Umkreis
wird gespiegelt mitgesetzt (Material aus denselben Quellen).
- Nutzen: Symmetrische Häuser, Burgen und Schiffe in der halben Zeit. Ersetzt normales Bauen
  nicht, sondern verdoppelt es.
- Aufwand: M–L (Hook am Platzieren, gibt es schon: `BlockItemMixin`). Risiko: überraschende
  Blöcke, wenn man die Ebene vergisst. Braucht eine sichtbare Ebene und eine Zeitbegrenzung.
  Kandidat für den Diamantkern (Teil 4).

**Empfohlene Reihenfolge:** V1 → V6 (schneller Gewinn) → V2 → V4 → V7 → V3 → V5 → V8. V9–V11
optional, an Kerne gebunden (Teil 4).

---

## Teil 4 – Die Baukerne

### 4.1 Befund

| Kern | Rezept | Weitere Quellen | Verwendung heute |
|---|---|---|---|
| Kupfer | 4 Kupferbarren + Netherstern | Maurer Stufe 2 (25 Smaragde), Wanderhändler | Kupfer-Baustab |
| Eisen | 4 Eisenbarren + Netherstern | Waldanwesen-Beute, Wanderhändler | Eisen-Baustab / Upgrade Kupfer-Stab → Eisen-Stab |
| Gold | 4 Goldbarren + Netherstern | Bastion, Netherfestung, Wanderhändler | Gold-Baustab / Upgrade Eisen-Stab → Gold-Stab, **Erzdetektor** |
| Diamant | 4 Diamanten + Netherstern | Maurer (3 Netheritbarren!), Trial Chambers | Diamant-Baustab / Upgrade Gold-Stab → Diamant-Stab |
| Netherit | Schmiede: Diamantkern + Netheritbarren | Bastion-Schatz (2×) | **keine** |
| Enderit | Schmiede: Netheritkern + Enderitbarren | – | **keine** |

Belege: `src/main/generated/data/simplebuilding/recipe/{copper,iron,gold,diamond}_core_plus.json`,
`netherite_core_smithing.json`, `enderite_core_smithing.json`, `ore_detector.json`,
`upgrade_*_building_wand*.json`. Netherit- und Enderit-Stab entstehen aus dem **Stab** plus Barren,
nicht aus dem Kern (`netherite_building_wand_smithing.json`, `enderite_building_wand_smithing.json`).
Beute: `loot/ModLootTableModifications.java:118-133,141-148,169-178,254-259`. Handel:
`villager_trade/mason/2/emerald_copper_core.json`, `netherite_diamond_core.json`. Items:
`items/ModItems.java:311-321`.

Drei Befunde nebenbei:

1. **Netherit- und Enderitkern sind Sackgassen.** Man kann sie herstellen, aber nirgends
   verwenden.
2. **Jeder Kern kostet einen Netherstern.** Das ist ein Wither je Kern und für Kupfer völlig
   unverhältnismäßig. Mit echten Funktionen würde man mehrere Kerne brauchen, dann muss
   Fragebogen-Frage 20 (Stern nur für Diamant und höher) mitentschieden werden.
3. Der Gold-Stab hat nur 256 Haltbarkeit (32 × 4 × 2, `SledgehammerItem.java:67`,
   `ModItems.java:355`). Der Diamant-Stab hat 12 488. Eine Stab-Stufe, die man sofort überspringt.

### 4.2 Das Prinzip: Kerne als Module statt als Stufenleiter

Heute ist ein Kern nur ein Zwischenprodukt auf dem Weg zum nächsten Stab. Wer den Diamantkern hat,
braucht die anderen nie. Damit jeder Kern auch im Late-Game zählt, sollte jeder Kern **eine eigene
Fähigkeit** tragen, die kein anderer Kern hat. Die Stab-Stufe bestimmt dann nur noch Größe und
Haltbarkeit.

Drei Wege, einen Kern an den Stab zu bringen:

| Mechanik | Pro | Contra |
|---|---|---|
| **M1 · Kern in der Nebenhand = Modus** | Kein GUI, sofort verständlich. Technisch konfliktfrei: Ein Kern ist kein BlockItem, die Materialsuche überspringt ihn (`BuildingWandItem.java:285-305`) und nimmt die Hotbar. | Die Nebenhand ist auch Materialquelle 1, Blaupause oder Oktant. Immer nur ein Modus. |
| **M2 · Kern einsetzen (Schmiedetisch, Basic Upgrade Template)** | Dauerhaft, Nebenhand bleibt frei. Mehrere Kerne je Stab möglich. Kerne werden **verbraucht**, die Nachfrage bleibt. | Ein Menü zum Umschalten ist nötig (Stab-Menü existiert, `client/gui/BuildingWandScreen.java`). |
| **M3 · Mischform** | M2 für Dauer-Module, Umschalten per Schleichen + Mausrad. | Komplexer, mehr Tests. |

**Empfehlung: M2.** Slots je Stabstufe: Kupfer/Eisen 1, Gold/Diamant 2, Netherit 3, Enderit 4.
Der Kern bleibt beim Schmiede-Upgrade des Stabs erhalten (Komponente mitkopieren). Umschalten im
Stab-Menü (Berührung des Konstrukteurs) oder per Schleichen + Mausrad.

### 4.3 Je Kern: (a) Late-Game-Verwendung, (b) Funktion für sich allein

#### Kupferkern

| | Option | Pro | Contra |
|---|---|---|---|
| a1 | **Linien-Modul:** Stab baut Linie, Säule, Brücke (= V6) | billigster Kern für die einfachste Form, logisch als Einstieg | eigentlich Aufgabe von *Linear*/*Brücke*, doppelt belegt |
| a2 | **Anbau-Modul** (= V4, Oberfläche folgen) | sehr nützlich, früh verfügbar | macht *Abdeckung* überflüssig |
| a3 | Zutat für den **Leitungs-/Blitzableiter-Kolben** (Kolben, der auf Redstone-Blitz reagiert) | Kupfer = Elektrik | neues Gadget, lenkt vom Bauthema ab |
| b1 | **Patina-Stimmgabel:** Rechtsklick auf Kupferblöcke → nächste Oxidationsstufe im 3×3, Schleichen = zurück, Abklingzeit | Kupfer-Bauten ohne Warten oder Axt. Passt zu Meißel und Hammer (Umformen in der Welt) | nicht verbrauchend, braucht Abklingzeit statt Haltbarkeit (Kern stapelt bis 16) |
| b2 | **Blitzfänger:** in einem Blitzableiter eingesetzt, zieht er bei Gewitter Blitze sicher an (kein Feuer) | lustig, Mob-Umwandlung (Creeper geladen, Schwein → Piglin) | Nischenfunktion |

**Empfehlung:** a2 + b1. Anbauen ist die meistgenutzte Stabfunktion in vergleichbaren Mods und
gehört an den Anfang. *Abdeckung* kann dann das Anbauen am Stab ohne Kern freischalten oder
entfallen. b1 ist klein und passt zum Umform-Thema.

#### Eisenkern

| | Option | Pro | Contra |
|---|---|---|---|
| a1 | **Ersetzen-Modul** (= V5) | robust und praktisch. Eisen = Werkzeugstahl | braucht Schutzprüfung (V1) |
| a2 | **Gerüst-Modul:** Stab setzt eine Gerüstsäule bis zum Klickpunkt und baut sie nach dem Bau wieder ab | nimmt „Gerüst hoch, Gerüst runter" ab | Gerüst-Physik, Rückgabe der Blöcke |
| a3 | Zutat für eine **Magnet-Aufwertung** (größere Reichweite) | nutzt ein vorhandenes Item (`MagnetItem.java:87-88`) | nicht baubezogen |
| b1 | **Luftanker („Engelblock"):** Kern in der Nebenhand + Block in der Haupthand → Rechtsklick in die Luft setzt den Block 2 Blöcke vor dir in die Luft | klassischer Bau-Helfer, löst Brücke-Option b aus dem Fragebogen ohne Verzauberung | kann Brücke/Linie (Kupfer) verwässern. Nur 1 Block je Klick |
| b2 | **Lot:** zeigt Fallhöhe und Abstand zum Boden, markiert die Senkrechte | billig | überschneidet sich mit dem Oktanten/Entfernungsmesser |

**Empfehlung:** a1 + b1.

#### Goldkern

| | Option | Pro | Contra |
|---|---|---|---|
| a1 | **Muster-Modul:** Stab setzt Hotbar-Blöcke als Muster (Schachbrett, Streifen, Rand) statt zufälliger Farbpalette | Deko-Kern. Gold = Schmuck. Ergänzt das Enderquarz-Schachbrett. Löst die Farbpalette-Vorschau-Lüge (D16) sauber | Muster-Auswahl braucht UI |
| a2 | **Schnellbau:** ganze Fläche in einem Tick, halbe Haltbarkeit je Block | Gold = schnell (wie Goldwerkzeug) | schwach als Late-Game-Grund |
| a3 | **Erzdetektor-Aufwertung** (Gold-Kern im Schmiedetisch: +1 Radius-Stufe oder alle Erze statt Klasse) | baut auf der einzigen heutigen Verwendung auf | nicht baubezogen |
| b1 | **Piglin-Amulett:** Kern im Inventar zählt wie Goldrüstung (Piglins neutral) | sofort spürbar im Nether, spart einen Rüstungs-Slot | kein Bauthema |
| b2 | **Erfahrungsspeicher:** Rechtsklick speichert 1 Level im Kern (bis 30), Schleichen gibt zurück | nützlich für Verzauberer | Balance, Doppelung mit Mods |

**Empfehlung:** a1 + a3 (der Erzdetektor bleibt die freistehende Funktion und wird aufwertbar).
b1 optional als Spaß-Feature.

#### Diamantkern

| | Option | Pro | Contra |
|---|---|---|---|
| a1 | **Figuren-Modul:** Stab füllt die Oktant-Figur (= V2), inklusive Dach (V3) | das wertvollste Feature an den wertvollsten frühen Kern gebunden | V2 würde dann nicht für alle gelten. Alternativ: Quader für alle, runde Figuren und Dach mit Diamantkern |
| a2 | **Präzisions-Modul:** Setzen wie ein Spieler (V7) mit wählbarer fester Ausrichtung | Treppen/Stämme richtig | eher Grundfunktion als Modul |
| a3 | Zutat für eine **Oktant-Aufwertung** (Figur-Deckel 256 → 512, Fernauswahl) | Diamant = Präzision | Oktant-Deckel ist eigentlich Bugfix (D6) |
| b1 | **Symmetrie-Anker** (= V11): Kern auf einen Block → Spiegelebene. Von Hand gesetzte Blöcke werden gespiegelt | echte Arbeitsersparnis unabhängig vom Stab, sichtbar und cool | L-Aufwand, Überraschungsblöcke |
| b2 | **Schleifstein:** Werkzeug + Kern im Amboss → 50 % Reparatur ohne Erfahrung | einfach | gewöhnlich, wertet Mending ab |

**Empfehlung:** a1 (runde Figuren und Dach, Quader ohne Kern) + b1.

#### Netheritkern

| | Option | Pro | Contra |
|---|---|---|---|
| a1 | **Rückbau-Modul:** Gegenstück zum Bauen. Entfernt nur **Blöcke, die dieser Stab gesetzt hat** (V8 als Werkzeug), Items zurück in Bündel/Rucksack | macht Fehlbauten und Gerüste harmlos, nimmt dem Hammer nichts weg | Aufzeichnung wie V8 nötig |
| a2 | **Feuerfest + unzerbrechlich:** eingesetzt → Stab feuerfest, Haltbarkeit halbiert verbraucht | löst Fragebogen 11. Klar messbarer Nutzen | wenig aufregend |
| a3 | **Lava-Modul:** Stab baut auch in Lava und Wasser (ersetzt Flüssigkeit, heute nur `canBeReplaced`) | Nether-Bau | sehr nischig |
| b1 | **Lava-Läufer:** Kern in der Nebenhand → Lava unter dir wird für 10 s zu Magma (Frostläufer für Lava) | Nether-Brücken ohne Material, spektakulär | Feuerschaden-Balance, Magma schadet |
| b2 | **Ewiger Brennstoff** in (Mod-)Öfen: brennt wie 1 Lavaeimer je Stunde, ohne verbraucht zu werden | nutzt die Maschinenreihe | kann Enderit-Ofen-Boni entwerten |

**Empfehlung:** a1 + b1. Wenn a1 zu teuer ist: a2 als Sofortlösung.

#### Enderitkern

| | Option | Pro | Contra |
|---|---|---|---|
| a1 | **Fernlager-Modul:** Stab und Blaupause beziehen Material aus der **Endertruhe** und einer mit dem Kern verknüpften Truhe/Rucksack-Block in derselben Dimension | beseitigt die größte Survival-Hürde großer Bauten (Materialschlepperei). Enderit = Ender-Speicher | Chunk muss geladen sein. Missbrauch als Fernzugriff einschränken (nur Blöcke entnehmen) |
| a2 | **Fernbau:** Stab wirkt bis 64 Blöcke weit (Raycast) | Türme, Brücken aus der Distanz | Schutz/Claims, Griefing-Risiko |
| a3 | **Kopieren/Einfügen am Ort** (= V9): Oktant-Figur direkt in eine Blaupause, ohne Kartentisch | krönt die Blaupausen-Kette | macht den Kartentisch-Schritt entbehrlich |
| b1 | **Rückruf-Anker:** Rechtsklick markiert einen Punkt, Schleichen + Rechtsklick teleportiert zurück (gleiche Dimension, verbraucht 1 Enderperle, Abklingzeit) | cool, Ender-Thema, hilft beim Pendeln zwischen Lager und Baustelle | Konkurrenz zu Mod-Teleportern. Balance über Perlen |
| b2 | **Void-Retter:** Kern im Inventar rettet einmal vor dem Void (verbraucht) | knüpft an Enderit-Void-Schutz an (`tags/item/void_protected.json`) | kaum Bauthema, sehr seltener Nutzen |

**Empfehlung:** a1 + b1.

### 4.4 Empfohlene Gesamtkombination

| Kern | Stab-Modul (M2) | Funktion allein | Aufwand |
|---|---|---|---|
| Kupfer | Anbauen (V4) | Patina-Stimmgabel | S–M |
| Eisen | Ersetzen (V5) | Luftanker („Engelblock") | M |
| Gold | Muster | Erzdetektor (heute) + dessen Aufwertung | M |
| Diamant | Runde Figuren + Dach (V2/V3) | Symmetrie-Anker (V11) | L |
| Netherit | Rückbau eigener Blöcke | Lava-Läufer | M–L |
| Enderit | Fernlager (Endertruhe/verknüpfte Truhe) | Rückruf-Anker | M |

Dazu gehört:

- **Rezepte:** Netherstern nur für Diamant und höher (Fragebogen 20c). Kupfer/Eisen/Gold:
  4 Barren + passender Block + Redstone o. ä. Sonst sammelt niemand sechs Kerne.
- **Netherit- und Enderit-Stab** künftig aus Stab + **Kern** statt Stab + Barren. Damit sind die
  beiden Sackgassen-Kerne sofort sinnvoll, noch bevor ein Modul existiert (Aufwand S, reine Daten).
- **Gold-Stab-Haltbarkeit** anheben oder den Gold-Stab über das Muster-Modul rechtfertigen.

**Kleinster sinnvoller Einstieg:** (1) Stab-Schmiederezepte auf Kerne umstellen (S). (2) V1 + V6
als Grundlage (M). (3) M2-Steckplatz mit Kupfer (Anbauen) und Diamant (Figuren) als ersten zwei
Modulen. Alles Weitere lässt sich danach einzeln entscheiden.

---

## Teil 5 – Notizen für später (nicht umgesetzt)

### Enderit-Kolben: Tunnelbohren zu leicht

Besitzer-Befund 2026-09-25: Der Enderit-Kolben bricht bis zu drei Blöcke vor sich durch
(Durchbruch mit Redstoneblock als Brennstoff). Mit einer Hebelschaltung bohrt er damit Tunnel fast
ohne Aufwand - das nimmt Spitzhacke, Vorschlaghammer und Strip Miner die Aufgabe weg.

Balance-Idee, **noch nicht entschieden**: ein Verschleiß- bzw. Schadenszustand am Kolben (z. B.
Abnutzungsstufen im Blockzustand, je Durchbruch eine Stufe, bei der letzten wird er zum
gewöhnlichen Netherit-Kolben oder muss mit Enderit repariert werden), alternativ ein höherer
Brennstoffpreis je Durchbruch oder eine Abklingzeit. Vor der Umsetzung mit dem Besitzer klären.
