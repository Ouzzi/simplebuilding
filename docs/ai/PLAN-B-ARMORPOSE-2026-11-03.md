# Plan b-armor01 (BA03-5): Transposer stellt Rüstungsständer in Posen

Besitzer 03.11.: Rüstungsständer wie in Bedrock in Posen bringen; abgebaut behält er die Pose (wie Kupfergolem-Status).

## Ist-Stand
- Transposer = `simplegadgets:rotator` (`RotatorItem`, Modul simplegadgets). Kein Armor-Stand-Code dort; Claims (simpletweaks) nur Schutzprüfung.
- 26.3 hat `ArmorStand.ArmorStandPose` (+ `setArmorStandPose`). `ArmorStandItem` wendet beim Platzieren `ENTITY_DATA` des Items an
  (`EntityType.createDefaultStackConfig`). Abbauen (`brokenByPlayer`) droppt aber ein nacktes Item (nur CUSTOM_NAME).
- `ArmorStand.interact` würde das Werkzeug in einen Slot legen -> Eingriff muss vor `Entity.interact` kommen (Fabric `UseEntityCallback`).

## Entscheidungen
- Ort: Modul simplegadgets (dort lebt der Transposer), keine neue Abhängigkeit, Modul bleibt allein spielbar. `ArmorPoses` (shared) + Fabric-Event + ein Mixin.
- 13 Posen in Bedrock-Reihenfolge (Default, None, Solemn, Athena, Brandish, Honor, Entertain, Salute, Riposte, Zombie, Cancan A/B, Hero); Default = Vanilla-Haltung.
  Werte sind eigene, Vanilla-Pose-Winkel (Bedrock-Rohdaten nicht verfügbar). Rechtsklick = nächste, Schleichen = vorige; Arme werden sichtbar.
- Kosten 1 Ladung (`rotatorChargePerTurn`), leer = trockenes Klicken, Schutz via `GadgetProtection.mayAffectEntity`.
- Pose im Item: Vanillas `ENTITY_DATA` (ShowArms + Pose) -> Platzieren stellt sie ohne weiteren Code wieder her; Lore-Zeile mit Posenname (Tooltip <= 48 Zeichen). Hook: `@ModifyArg` auf `popResource` in `brokenByPlayer`.
  Kein eigener Komponenten-Typ nötig (kein Datafix, keine Netz-Sync).
- A12 Vanilla-Nutzen: Vanilla hat nur die feste Haltung / Befehle -> kein Überschnitt; leichter Eingriff (nur Transposer in der Hand, Drop trägt Daten).
- Kein Konflikt: Simple Stands/Claims/Dinos/Trims nutzen den Ständer nur als Träger; kein anderer Code hängt an `brokenByPlayer`.
- Keine neue Optik.

## Dateien
ArmorPoses.java, ArmorStandDropMixin.java (+ mixins.json), GadgetsFabric (UseEntityCallback), Lang EN/DE, manual.json EN/DE (+ Guide per module_guides.py), RotatorTests + GadgetsTests + ModuleGameTest.

## Verifikation
Lokale Prüfskripte, dann ein sbt-Lauf: Compile, Modul-Gametests (inkl. 2 neue), standalone, check_client_mixins, wiki --check.
