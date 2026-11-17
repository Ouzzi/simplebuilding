# PLAN N77 Teil 1 – Blaupausen-Scan mit Item-Frames und Entities (16.11.2026)

Auftrag: `/root/b-auftrag-20261109.md` Abschnitt N77 Punkt 1. Branch `b-tz77s` (von `b-bundle203-gen`).
Besitzer-Scan der Testzentrale (`/root/sb-tools-a/tz-owner-1611/layout-scan.txt`, 30x3x119) enthält keine Item-Frames.

## Ist-Zustand (Retrieval)
- 26.3 (Hauptlinie): die Blaupause lebt im Modul **simplebuildingtools** (`modules/simplebuildingtools/shared/java/com/simplebuildingtools/blueprint/`),
  nur Fabric 26.3. `common/src/shared/.../blueprint/` ist die alte 26.2-Kopie (`McVersion.BTOOLS_IN_SB` = true nur auf 26.2) – Port-Run, hier nicht angefasst.
- `BlueprintScanner.Job` liest nur Blockzustaende (`level.getBlockState`), `BlueprintModel` speichert nur `key -> BlockState`,
  `BlueprintCode` kennt nur Block-, Alias-, Variablen- und `pixels`-Zeilen, `BlueprintBuilder` setzt nur Bloecke.
  Item-Frames sind Entities -> gehen beim Scan komplett verloren.
- Tests: `modules/simplebuildingtools/shared/java/com/simplebuildingtools/test/BlueprintTests.java`, Katalog
  `modules/simplebuildingtools/fabric/src/main/java/com/simplebuilding/modules/simplebuildingtools/ModuleGameTest.java`, Ziel `module-simplebuildingtools-fabric-263`.
- Spezifikation: `docs/BLUEPRINT.md` (Abschnitt 1 Bausprache, 4 Bauen, 5 Scannen).

## Format (abwaertskompatibel, optionaler Entity-Abschnitt)
Eine Entity je Zeile, Zeile beginnt mit `@` (in alten Codes ein Fehler `bad_block_id` – alte, gueltige Codes enthalten nie `@`-Zeilen,
parsen also unveraendert). Ohne Entities schreibt der Serialisierer byte-genau dasselbe wie bisher.
```
# 5x3x4, 12 blocks, 3 entities
stone 0..4,0,0..1
@item_frame 1,1,2 north diamond_sword[enchantments={sharpness:5},custom_name="Klinge"] rot=2 invisible fixed
@glow_item_frame 2,1,0 up torch
@armor_stand 3.5,1,3.5 yaw=90 arms noplate head=iron_helmet hand=stone_sword head_pose=-10,0,0
@zombie 0.5,1,3.5 yaw=180 noai silent invulnerable name="Bob"
```
- `@<entity-id>` (ohne `minecraft:` wie bei Bloecken), dann Position `x,y,z` im selben Raster wie die Bloecke.
  Item-Frames: Ganzzahl = Blockstelle des Frames + Pflicht-Richtung (`north|south|east|west|up|down`, Richtung, in die der Frame zeigt).
  Andere: Kommazahlen (max. 3 Nachkommastellen).
- Item: Vanilla-Befehlssyntax wie `/give` (`id[komponente=SNBT,!komponente]`, optional `*Anzahl`); Komponenten werden registry-aware
  ueber den Komponenten-Codec mit `RegistryOps<Tag>` geschrieben und beim Bauen mit `ItemParser` (Welt-Registries) gelesen.
- Schalter: `invisible fixed invulnerable silent nogravity noai small arms noplate marker showname`;
  Werte: `rot=0..7`, `yaw=`, `name=<SNBT-Text>`, Ausruestung `head= chest= legs= feet= hand= offhand=`,
  Posen `head_pose= body_pose= left_arm_pose= right_arm_pose= left_leg_pose= right_leg_pose=` (je `x,y,z` Grad).
- Zeilen sind anfuehrungszeichen- und klammerbewusst getrennt (Leerzeichen/`#` in Namen sind erlaubt).
- Grenzen: hoechstens 4096 Entities je Code, Item-Text hoechstens 8192 Zeichen; Fehler werden als Problem gemeldet (kein Absturz):
  `entity_unknown`, `entity_position`, `entity_facing`, `entity_bad_token`, `entity_item_unknown`,
  `entity_item_syntax`, `entity_component_unknown`, `entity_too_many`, `entity_too_long`.
  Parse-Zeit (ohne Welt) prueft Ids, Komponenten-Ids und SNBT-Syntax; die Komponenten-Werte (z. B. Verzauberungen = dynamische Registry)
  prueft erst der Bau mit den Welt-Registries – schlaegt das fehl, steht der Frame leer da (geloggt, als Fehlstelle gezaehlt).

## Was gescannt wird (Entscheidung)
1. **Item-Frames und Leucht-Rahmen** (jede `ItemFrame`-Klasse): Stelle, Richtung, Item inkl. Komponenten, Drehung 0..7, unsichtbar, fest, unverwundbar.
2. **Ruestungsstaender** (jede `ArmorStand`-Klasse, auch Modul-Staender/Trainingspuppe per Entity-Id): Position, Blickrichtung, 6 Ausruestungsplaetze,
   Posen (nur abweichend vom Standard), klein/Arme/ohne Platte/Marker/unsichtbar/ohne Schwerkraft/unverwundbar, Name.
3. **Mobs ohne KI** (`Mob.isNoAi()`): nur Typ, Position, Blickrichtung, Name und Schalter (noai/silent/invulnerable/nogravity/invisible).
   Begruendung: billig (dieselbe Zeilen-Mechanik), die Schau-Mobs der Testzentrale sind KI-los; Varianten-/Ausruestungsdaten der Mobs
   werden bewusst NICHT uebernommen (grosses NBT). Mobs mit KI, Spieler, Item-Entities, Loren, Gemaelde usw. werden nicht gescannt.
Erfasst wird, wessen Blockstelle in der Oktant-Figur liegt. Die Bounding Box des Modells umfasst Bloecke und Entity-Stellen.

## Bauen
- Entities werden gesetzt, wenn der Block-Auftrag fertig ist (Frames brauchen ihre Wand), auch bei `completeJob` und fortgesetzten Auftraegen.
- Drehung: Stellen wie Bloecke, Frame-Richtung mitgedreht, Bodenrahmen/Deckenrahmen: Item-Drehung +-2 je Viertel, `yaw` + 90 je Viertel.
- Schon vorhanden (gleicher Typ an derselben Stelle, Frames zusaetzlich gleiche Richtung) -> uebersprungen (kein Doppeln beim Nachbauen).
- Rechte: wie Bloecke (`mayVisit` + `BuildPermissions.mayChange`); Frames nur, wenn `survives()` (sonst fallen sie ab).
- Kreativ (freeBuild): alles gratis. Ueberleben: Frame/Staender kosten ihr Item (Baustab-Vorrat); Inhalt (Frame-Item, Ausruestung)
  nur, wenn genau dieses Item (gleiche Komponenten) im Inventar liegt – sonst bleibt der Platz leer; Mobs nur kreativ.
  Keine Gratis-Items in Ueberleben (kein Dupe).
- Nicht im Umfang: Geister-Vorschau der Entities, Rueckgaengig (WandUndo) fuer Entities, Befehls-Export (`COMMANDS`) fuer Entities,
  Materialliste mit Entities, 3D-Ansicht. -> offene Punkte.

## Dateien
- `blueprint/BlueprintEntity.java` (neu): Datensatz, Zeilen-Parser/-Schreiber, Scan einer Entity, Bauen einer Entity.
- `BlueprintModel`: Entity-Liste, Bounds, `normalized`, `equals`.
- `BlueprintCode`: `@`-Zeilen im Parser (Einfaerbung + Probleme), Serialisierer haengt Entity-Zeilen an (Kopf nennt Anzahl nur bei > 0).
- `BlueprintScanner`: Entities der Auswahl am Ende sammeln.
- `BlueprintBuilder`: Entities nach dem Block-Auftrag setzen.
- Lang en_us/de_de: neue Fehler-Schluessel. `docs/BLUEPRINT.md`: Abschnitt 1.10.
- Tests: `test/BlueprintEntityTests.java` (neu) + Katalog; Fixture `testing/fixtures/blueprint/tz-owner-1611-layout-scan.txt`.

## Verifikation
Gametests (module-simplebuildingtools-fabric-263): Rundreise Scan -> Code -> Bauen fuer Frame + Leucht-Rahmen, Drehung 0..7,
unsichtbar+fest, verzaubertes+benanntes Item, jede Richtung inkl. Boden/Decke, Ruestungsstaender (Ausruestung, Pose, Schalter),
KI-loser Mob, Drehung des Baus (Viertel), alter Code parst unveraendert, Besitzer-Scan parst fehlerfrei und serialisiert byte-gleich,
Fehlerfaelle (unbekannte Entity/Item/Komponente, kaputtes SNBT, zu viele) melden Probleme. Ueberleben: kein Gratis-Inhalt.
Dann: Kompilieren (Modul Fabric 26.3), alle Blueprint-Tests des Moduls, precheck.
