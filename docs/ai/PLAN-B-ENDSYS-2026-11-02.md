# Plan MACH32-ENDSYS (Sitzung B, b-endsys01, 02.11.) - Enderit nur mit Simple Ores, Simple Ores als recommends

Besitzer-Wortlaut: "nur wenn enderite existiert (also simple ores, dann verfuegbar) ueberall wo enderite optional ist soll es als
recommended gelistet sein". Die Rueckfrage "End-Systeme nach Simple End?" ist NICHT beantwortet; es wird nichts umgezogen.

## 1. Audit (Stand Basis b-bundle91)

Mechanik seit IG31-13 (unveraendert): Ids bleiben registriert; ohne Ores sind Rezepte per `fabric:load_conditions`/`neoforge:conditions`
(`simpleores` geladen) weg, `CreativeTabPolicy.isEnderiteVariant` (`*enderite*`) blendet Tabs/Suche aus, `SimpleLibJeiPlugin` blendet
Zutaten aus, In-World-Aufwertungen ueberspringen unbekannte Materialien (`InWorldUpgrades`, Zeile 351).

| Modul | Enderit-Varianten | vorher bedingt | Luecke / jetzt |
|---|---|---|---|
| simplebuilding (26.3) | Werkzeuge, Ruestung, Kolben, Trichter, Pads, Kerne ... | Ores ist Pflicht-Sub-Mod (R1), immer da | keine; nichts zu tun |
| simplestorage | Truhe, Trichter-/Controller-Truhe, Fallentruhe, Shulker (+16 Farben), Rucksack, Buendel, Koecher, Rohr, Truhenboot (13 Hoelzer), Truhenlore | Schmiede-Rezepte, Rohr | LUECKE: Truhenboote (13), Truhenlore, Controller-Truhe, Fallentruhe, Shulker-Farben (16), Shulker aus Schalen + je Advancement = 64 Dateien (inkl. Ovens) -> jetzt bedingt |
| simpleovens | Ofen, Raeucherofen, Hochofen, Ofenlore | Lore-Schmiede | LUECKE: `enderite_furnace_minecart` (+Advancement) -> bedingt |
| simplecrucibles | Tiegel, Fass, Eimer (+Milch) | Eimer-Rezept, In-World-Upgrade | keine |
| simplehammers | Enderit-Vorschlaghammer | Rezept bedingt | keine |
| simpleriding | Enderit-Hufeisen | bedingt | keine |
| simplemaps | End-/Mega-Wegfinderkarte | Rezept bedingt, `*_without_enderite` Gegenstueck | keine |
| simplebeacons | Maechtige Beacons/Conduits (Enderit-Kern) | bedingt | keine |
| simplemoney | Haendler-Angebote fuer Enderit-Varianten (SB, Storage, Ovens, Crucibles, Hammers) | nur Kern + simpleores-Material | LUECKE: ~45 Angebote ohne Bedingung -> Generator setzt jetzt automatisch `fabric:all_mods_loaded simpleores` (+NeoForge/Forge) fuer jede Zeile mit "enderite" im Pfad (Namespace nicht simpleores) |
| simplesandwiches, simplemobs, simpleend, simplequestionary, simpleinterfaces, simplevisuals | nur Verweise auf SB-Enderit (SB hat Ores), Texturen/Modelle, Texte | - | keine Aenderung |

## 2. recommends (Punkt 2)

`simpleores` als `"recommends"` in `fabric.mod.json` und `optional`-Dependency in NeoForge (`type="optional"`) / Forge (`mandatory=false`)
fuer: simplestorage, simpleovens, simplecrucibles, simplehammers (bisher `suggests`, jetzt `recommends`), simpleriding, simplemaps,
simplebeacons, simplemoney. `modules/modules.json` `optional` ergaenzt, `tools/multimod.py` liest `suggests` UND `recommends`,
MODSTRUKTUR neu erzeugt. simpleoctets behaelt `suggests` (Blei, kein Enderit).

## 3. Tests (Punkt 3)

- NEU `simplelib:enderite_only_with_ores` (LibTests + ModuleGameTest): laeuft in jedem Modul-/Standalone-Target. Ohne Ores: keine
  Rezept-Id mit "enderite" (ausser minecraft/simpleores) und kein Enderit-Eintrag in Kreativ-/Suchtabs; mit Ores: bekannte Rezepte der
  geladenen Module (Storage-Karren/Rohr, Ovens-Karren) sind da.
- Angepasst auf "Enderit-Rezept existiert genau mit Ores": OvensTests `recipes`, StorageTests `recipes`, RareFindsTests (Schalen-Rezepte),
  NetworkTests (Controller-Truhe), ChestTests (Fallentruhe-Rezepte), LinkTests (Angebote je Zeile `requires`).
- Lokal: check_load_conditions, multimod, modtree --check, check_data (money/storage/ovens/hammers), loot, dup, tooltip, lang.
- sbt: Compile Fabric + Module, Targets module-{simplelib,simplestorage,simpleovens,simplemoney}-fabric-263 + -standalone-fabric-263.

## 4. Nicht gemacht / offen

- End-Systeme (Astral/Nihil) haben keine Enderit-Variante und wandern nicht (Rueckfrage offen).
- NeoForge/Forge-Laufzeit ungetestet (Phase 26.3 Fabric); Daten-Bedingungen werden von `check_load_conditions` fuer alle Loader geprueft.
- Block-Beutetabellen der Enderit-Varianten bleiben unbedingt (harmlos, Bloecke nicht beschaffbar).
