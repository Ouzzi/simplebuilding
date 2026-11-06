# Plan b-strong07 (07.11.2026) – WF25-10 Strong Upgrades nach SimpleLib

Wortlaut (wd25.md Z.70): „Strong Upgrades / Strong-Upgrade-Charts nach SimpleLib (für andere Mods nutzbar); ohne Simple
Mobs droppt der Wither eine Chart (wie früher gewünscht); Strong Upgrades upgraden die Schwingen mit den Materialien.“
Besitzer hat den Umzug mit dem Auftrag B07 freigegeben.

## Ist-Stand (geprüft auf hub/claude-wave1 4f2722c8a)

- `simplebeacons:strong_upgrade_template` (Schmiedevorlage, MYTHIC) und `simplebeacons:strong_upgrade_shard`
  (UNCOMMON, explosionsfest) in `BeaconsRegistry`; Rezepte `strong_upgrade_template` (Netherherz, nur mit Simple Mobs),
  `_duplication` (Netherherz, nur mit Simple Mobs), `_from_shards` (9 Splitter); `WitherShardMixin` → Vanilla-Wither
  droppt **immer** einen Splitter (b-wither21, Besitzer 21.10.), JEI-Mob-Drop über `BeaconsViewer`.
- Schwingen (Simple Mobs): `wither/nether/end/void_wings_smithing` nutzen die Strong-Vorlage **nur mit Simple Beacons**,
  sonst `*_without_simplebeacons` mit Netherit-Vorlage. Materialien: Netherherz, Nether-Juwel, Enderherz, Ender-Juwel.
- Vorbild für den Umzug: `basic_upgrade_template` (SimpleBuilding → SimpleLib, `LibMaterials`-Gruppe
  `upgrade_templates`, Rezepte mit Bedingung `simplelib:material_requested`, IdMoves durch den alten Besitzer).
- Teilweise erledigt: nichts vom Umzug; Schwingen-Upgrade existiert, aber nur bedingt.

## Entscheidungen

1. **Neue SimpleLib-Materialgruppe `strong_upgrade`** = `simplelib:strong_upgrade_template` + `simplelib:strong_upgrade_shard`
   (immer registriert wie alle Materialien; Rezepte/Drop nur, wenn ein Mod die Gruppe anfordert). Eigenschaften
   unverändert (MYTHIC bzw. UNCOMMON + explosionsfest). „Chart“ = die Vorlage (Schmiedevorlage).
2. **Evil-Vorlage bleibt in Simple Beacons** (nur für Evil-Leuchtfeuer/-Aquisator gebraucht, kein Fremdnutzen).
   Ebenso bleibt die In-World-Aufwertung (Hammer/Axt auf Leuchtfeuer) in Simple Beacons – sie kennt nur Beacon-Blöcke.
   `BeaconsRegistry.STRONG_UPGRADE_TEMPLATE/_SHARD` bleiben als Felder, zeigen aber auf die SimpleLib-Items (Muster
   `SharedMaterials`), damit der übrige Beacons-Code unverändert bleibt.
3. **IDs ändern sich** → `IdMoves` (registriert vom alten Besitzer Simple Beacons in `SimpleBeacons.init`):
   Items `simplebeacons:strong_upgrade_template/_shard → simplelib:…`; Rezept + Fortschritt `strong_upgrade_template_from_shards →
   simplelib:`; Netherherz-Rezepte `strong_upgrade_template`, `_duplication → simplemobs:` (Besitzer des Netherherzens;
   SimpleLib nennt keine Nutzer). Migrationstest `strong_upgrade_moved` in Simple Beacons.
4. **Wither-Drop zieht nach SimpleLib** (`WitherStrongUpgradeMixin`, nur `minecraft:wither`, nur wenn die Gruppe
   angefordert ist): Bietet ein geladener, eingeschalteter Mod einen eigenen Weg zur ersten Vorlage an
   (`SimpleLibApi.strongTemplateWay(BooleanSupplier)`, Simple Mobs meldet sich mit `MobsRegistry.on()` an), droppt der
   Wither wie bisher **einen Splitter**; sonst (Simple Mobs fehlt oder ist aus) droppt er **die ganze Vorlage** statt
   des Splitters – nie beides (keine Doppel-Drops). JEI-Mob-Drop wandert mit (Owner `simplelib`), zeigt das jeweils
   Gedroppte.
5. **Schwingen immer über die Strong-Vorlage**: Simple Mobs fordert `strong_upgrade` an; die vier Schmiede-Rezepte
   verlieren die `simplebeacons`-Bedingung und nennen `simplelib:strong_upgrade_template`; die vier
   `*_without_simplebeacons`-Netherit-Rezepte entfallen (Vorlage ist jetzt immer da: Netherherz-Rezept bzw. Splitter).
6. Lang: Vorlagen-/Splitter-Texte und JEI-Notiz nach `simplelib` (EN/DE); „Anwendbar auf“ neutral: Leuchtfeuer,
   Aquisator, Elytren/Schwingen. Texturen/Modelle per `git mv` nach `assets/simplelib` (keine neue Optik, nichts Sichtbares
   neu → keine Vorschau nötig).
7. Folge-Verweise: simplemoney-Preise (Schlüssel `simplelib:`), Datagen-Tag `placeable_small` (generierte Datei von Hand
   mitziehen), `PlacedDisplay3dTests`, Discover-Listen, `check_data.py` (beacons, mobs), Wiki-Manual, Questionary,
   Testzentrale-Liste, `MaterialRequestTests`. Generierte Wiki-Daten (`wiki/data/*.js|json`) macht das Bündel.

## Dateien (Kern)

simplelib: `LibMaterials`, `SimpleLibApi`, neu `material/StrongUpgradeDrops` + `mixin/WitherStrongUpgradeMixin`,
`simplelib.mixins.json`, Lang, Assets, Rezept `strong_upgrade_template_from_shards` (+Fortschritt), Tests
`MaterialRequestTests`, neu `StrongUpgradeTests` + Fabric-Adapter. simplebeacons: `BeaconsRegistry`, `SimpleBeacons`,
`BeaconsViewer` (entfällt), `WitherShardMixin` (entfällt), Rezepte/Fortschritte, Lang, Tests. simplemobs: Rezepte,
`WitherLoot.idMoves` (Anforderung + Weg), `WingsTests`, `check_data.py`.

## Tests

Ein sbt-Lauf: Compile `:mc26_3:fabric`, `:modules:simplelib/simplebeacons/simplemobs:fabric`; Targets
`module-simplelib-fabric-263`, `module-simplebeacons-fabric-263`, `module-simplebeacons-standalone-fabric-263`,
`module-simplemobs-fabric-263`, `module-simplemobs-standalone-fabric-263`; `fabric-263 --filter 'simplebuilding:data_integrity*'`.

## Risiken

- IdMoves nur, wenn die alte ID fehlt → alte Registrierung im selben Commit entfernt.
- Welten ohne Simple Beacons, aber mit alter Vorlage: konnte es vorher nicht geben (Item gehörte Beacons).
- NeoForge/Forge: geteilter Code/Mixin-JSON gilt mit, wird aber nicht kompiliert/getestet (Regel „nur 26.3 Fabric“).

## Abweichungen / Ergebnis (07.11.)

- Item-Modelle, Rezept und Fortschritt der Splitter-Vorlage erzeugt `modules/simplelib/tools/gen_resources.py` (Gruppe
  `strong_upgrade`), nicht von Hand in `shared/`.
- Kreativ-Tab: im SB-Paket ohne Simple Beacons stand die Vorlage in keinem Tab (`data_integrity_…_creative_tab`). SimpleLib
  bietet Vorlage + Splitter jetzt (nur angefordert) dem Vanilla-Zutaten-Tab an; die Tab-Regeln ziehen sie in den Simple-Lib-Tab.
  Nur Fabric (NeoForge/Forge-Hook offen, Regel „nur 26.3 Fabric“).
- Zusätzlich: Questionary-Einträge nach `assets/simplelib/questionary`, Mod-Icon-Motiv Beacons liest die Vorlage aus SimpleLib
  (`MOTIF_FROM`), Textur-Generatoren (`wither21`, `beacons_a29`) zeigen auf den neuen Pfad, Simple-Mobs-Wiki/Questionary ohne
  Netherit-Hinweis.
- Tests: Lauf 1 Compile grün, Module 527/527 (simplelib 55, beacons 38+38, mobs 198+198), data_integrity 47/48 (Tab) → Fix;
  Lauf 2 data_integrity 48/48, simplelib + beacons (+standalone) 131/131.
