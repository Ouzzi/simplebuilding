# Plan B-TRIALS13 (08.11.2026, Branch b-trials13) - TR31-R Rest

Basis hub/claude-wave1 655c0df8e, Modul `simplemobs`, nur MC 26.3 Fabric. Rest aus b-trials32 (Plan `PLAN-B-TRIALS32-2026-10-31.md`):
Kategorienregel "min. 2 Kategorien" beim Tresor-Ziehen; Stufe der Vanilla-Spawner und Raid-Markierung über Neustart speichern.

## 1 Kategorienregel (Konzept KONZEPT-A-WP20 9.4, R7)
- `TrialsExtras.category(ItemStack)`: RESOURCE / HEALING / MAGIC / COSMETIC. Heilung = FOOD- oder DEATH_PROTECTION-Komponente (Goldäpfel, Totem);
  Magie = verzaubert/gespeicherte Verzauberung, Ominöse Flasche, Buch, Erfahrungsfläschchen, Windkugel; Kosmetik = Schallplatte, Bannermuster,
  Schmiedevorlage (außer Netherit-Aufwertung), Töpferscherbe; Rest Ressource.
- `drawDistinct`: Neuwurf (max. 12) bis kein Gegenstand doppelt UND (bei >= 2 Gegenständen) >= 2 Kategorien. Sonst bester Wurf (erst Anzahl, dann Kategorien).
- Entscheidung: kein eigener Schalter, die Regel hängt an `trialLootNoRepeat` ("Regeln gegen Wiederholung" im Konzept sind eine Einheit). Lang/Handbuch-Text ergänzt.

## 2 Persistenz
- Neu `trials/EvilTrialsData` (SavedData je Dimension, `simplemobs:evil_trials`, Muster DragonKills/RitualCooldowns): `vanilla_spawners` [{pos, kind, stage}],
  `raids` [{id (Vanilla-Raid-ID aus Raids), kind, stage}]. Fehlt die Datei (alte Welt) → leer; ungültige Einträge (Weathered/Stufe <= 0) fallen beim Laden weg. Keine Migration nötig:
  vorher lag alles nur im Speicher.
- `EvilOmen.VANILLA` → Daten; `kindAt/stageAt` lesen nur noch an echtem Vanilla-Trial-Spawner (verwaiste Einträge wirken nicht).
- `EVIL_RAIDS` bleibt als Cache (Weak, auch für Raids ohne ID im Test); dazu Eintrag mit Raid-ID in den Daten. Nach Neustart holt `hydrate(level)` die Einträge über
  `Raids.get(id)` zurück; verschwundene Raids werden entfernt, nach Truhe ebenso.
- Abweichung: keine Attachments (simplemobs nutzt keine; Raid ist kein Attachment-Träger) - SavedData genügt.

## Test
`trials_loot_categories` (Kategorien + Neuwurf mit gesteuertem Lieferanten + 60 Ziehungen je Evil-Stufe >= 2 Kategorien), `trials_vanilla_spawner` um Speichern/Laden/Neustart erweitert,
neu `trials_raid_persist` (echter Raid über `Raids.createOrExtendRaid`, Daten kodieren/parsen, `set`, Cache leeren, Stufe kommt zurück). Ein sbt-Lauf: Compile 26.3 Fabric + simplemobs,
`module-simplemobs-fabric-263,module-simplemobs-standalone-fabric-263`, `fabric-263 --filter 'simplebuilding:data_integrity*'`.

## Abweichung nach Lauf 1
Lauf 1 (c5f7391e3): Compile 0, data_integrity 48/48, simplemobs 199/200 je Ziel - rot `trials_loot_categories`: Leere-Tresor Stufe 4 lieferte nach 13 Würfen
eine 4-Gegenstände-Ziehung nur aus Ressourcen, weil der Bestwurf erst nach Anzahl, dann nach Kategorien gewählt wurde (Tresor-Tabellen haben eine feste
random_sequence, daher reproduzierbar). Fix: Bewertung Kategorienregel zuerst, dann ohne Wiederholung, dann Anzahl; Abbruch beim ersten Wurf, der beides erfüllt.
