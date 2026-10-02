# Konfiguration (Stand 2026-09-29)

Kurzüberblick über die Config der Mod: wo sie liegt, wie der Bildschirm aufgebaut ist, wie der
Befehl funktioniert und was man beim Hinzufügen einer Option beachten muss. Die vollständige
Liste mit Tooltips steht im Wiki (`?tab=config`, erzeugt von `wiki/generate.py`) und im Spiel
unter `/simplebuilding config list`.

## Datei und Loader

- Datei: `config/simplebuilding.json` (Cloth Config / AutoConfig, Gson).
- Klassen: `common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java` mit den
  Gruppen `tools`, `worldGen`, `tweaks` (`tweaks/TweaksConfig.java`) und `server`
  (`config/ServerTuningConfig.java`, Zugriff und Grenzen in `config/ServerTuning.java`).
  Andere Linien bleiben bis zum eigenen Port-Run unverändert; keine Identität der Kopien annehmen.
- Fabric: ModMenu öffnet den Bildschirm (`ModMenuIntegration`). NeoForge: Config-Knopf in der
  Mod-Liste (`SimplebuildingNeoForgeClient#buildConfigScreen`). 26.3 wie 26.2.
- Forge: kein Cloth Config für 26.x. Der Shim unter `forge/src/main/java/me/shedaniel/autoconfig/`
  liefert nur die Standardwerte, keine Datei, keinen Bildschirm. Seine Annotationen
  (`Category`, `TransitiveObject`, `BoundedDiscrete`, `ColorPicker`, `Tooltip`, ...) müssen jede
  Annotation abdecken, die die Config-Klassen benutzen, sonst baut Forge nicht.
- 26.4-Snapshot: kein Bildschirm (Cloth 26.3 stürzt dort beim Zeichnen ab), Datei und Befehl gehen.

## Bildschirm

Acht Reiter, Reihenfolge = erstes Feld jeder Kategorie in `SimplebuildingConfig`:

| Reiter (`category`) | Inhalt |
|---|---|
| Werkzeuge & Bauen (`building`) | `tools.*`: Baustab-Hunger + Faktor, Attraktor-Reichweite, Rotator-Kosten, Bündel-/Oktant-Bedienung, Vorschau-Deckkraft, Animationen |
| Verzauberungen & Rüstung (`equipment`) | Luftsprung + Abklingzeit, Besatz-Vorteile, Resonanz-Multiplikator |
| Kolben (`pistons`) | Verschleißbudgets, Endportalrahmen, Unzerstörbares anderer Mods |
| Pads & Tweaks (`tweaks`) | `tweaks.*` in Gruppen: Pads an/aus, Pad-Zeiten & -Stärke, Resonanzstab, Abstimmung, Spawn, Dimensionen, Befehle, Leistung |
| Beute, Handel & Welt (`world`) | `worldGen.*` (Loot, Kern-Chancen, Handel) und den alten Handbuch-Schalter (auf 26.3 wirkungslos) |
| Darstellung (Client) (`visuals`) | Buch-Texturen, Besatz-Icons |
| Kompatibilität & Erweitert (`advanced`) | Kolben-Abbau-Ereignisse (Schutz-Mods), Dev-Kreativ-Tab |
| Server & Modpack Tuning (`server`) | `server.*`: alle Gameplay-Stellschrauben für Server-/Modpack-Ersteller, serverseitig verbindlich (siehe unten) |

Die Gruppen stehen mit `@ConfigEntry.Gui.TransitiveObject` flach im Reiter; der JSON-Aufbau
(Verschachtelung, Schlüssel) ist dadurch **unverändert**, alte Dateien laden ohne Migration.
Namen/Tooltips: `text.autoconfig.simplebuilding.option.<pfad>[.@Tooltip]`, Reiter:
`...category.<name>`, Titel: `...title`. Jeder Tooltip nennt am Ende den Standard
(`Default: …` / `Standard: …`) und ob die Option client- oder serverseitig wirkt.

## Befehl `/simplebuilding config` (Operatoren)

- `list [filter]` – alle Optionen mit Wert (gold = vom Standard abweichend); Filter = Pfad-Präfix
  (`tools.`, `tweaks.pads`) oder `root`.
- `get <option>` – Wert, Standard, Typ.
- `set <option> <wert>` – Booleans auch `on/off`, Farben auch `#RRGGBB`. Begrenzt wie beim Laden
  (`validatePostLoad`), speichert, schickt den Clients die Server-Werte.
- `reset <option>` – zurück auf den Standard.
- `setTrimMultiplier`/`getTrimMultiplier` und `/simplebuilding tweaks …` bleiben.

Die Optionsliste kommt aus `ConfigOptions` (Reflexion über die Felder), der Befehl kann also nicht
hinter der Datei zurückbleiben. `ConfigOptions.CLIENT_SIDE` markiert reine Client-Optionen (der
Befehl weist auf einem dedizierten Server darauf hin), `APPLY_ON_RELOAD` die Datenpaket-Optionen.

## Server → Client

Was Client und Server gleich sehen müssen, schickt der Server beim Einloggen und nach jedem
Config-Befehl: `TweaksConfigPayload` (Raketen-Stapel, Boosts, Linse an/aus + Reichweite, die
Luftsprung-Abklingzeit und seit 2026-09-29 der **ganze Reiter `server` als JSON**),
`PistonConfigPayload` (die beiden Durchbruch-Optionen), `TrimDataPayload` (Resonanz-Multiplikator).
Code liest den Reiter nur über `ServerTuning.get()`: auf dem Server (auch dem integrierten) die eigene
Datei, auf dem Client-Thread den vom Server gemeldeten Stand – die eigene Datei eines Clients hat keine
Stimme. Ohne Meldung (Hauptmenü, Server ohne Mod) gilt die eigene Datei. Abweichende Höchstladungen
(siehe unten) meldet der Client im Log.

## Neu im Umbau 2026-09-28 (Standard = bisheriges Verhalten)

| Option | Standard | Wirkung |
|---|---|---|
| `tools.wandHungerMultiplier` | 1.0 | Faktor auf die Baustab-Erschöpfung je bezahltem Block (0 = kostenlos) |
| `tools.magnetRangeMultiplier` | 1.0 | Faktor auf die Attraktor-Reichweite |
| `tools.rotatorChargePerTurn` | 1 | Ladung je Rotator-Drehung (0 = kostenlos) |
| `worldGen.buildingCoreLootChanceMultiplier` | 1.0 | Faktor auf die Baukern-Chancen in Truhen (0 = keine; bei `/reload`) |
| `tweaks.pads.enablePotionPads` | true | Trank-Pads an/aus |
| `tweaks.padTuning.teleporterTier1WarmupTicks` | 1000 | Wartezeit Spawn-Teleporter I (50 s; seit 2026-09-28 drei Stufen, ersetzt `teleporterWarmupTicks`) |
| `tweaks.padTuning.teleporterTier2WarmupTicks` | 400 | Wartezeit Spawn-Teleporter II (20 s) |
| `tweaks.padTuning.teleporterTier3WarmupTicks` | 100 | Wartezeit Enderit-Spawn-Teleporter III (5 s; ersetzt `enderiteTeleporterWarmupTicks`) |
| `tweaks.padTuning.launchpadStrengthMultiplier` | 1.0 | Faktor auf den Startrampen-Schub |
| `tweaks.padTuning.potionPadChargeStepTicks` | 20 | Länge eines Trank-Pad-Ladeschritts |
| `tweaks.padTuning.potionPadCooldownFactor` | 2.0 | Trank-Pad-Abklingzeit × Wirkdauer (0 = keine) |
| `tweaks.laserPointer.chargePerSecond` | 4 | Stab-Ladung je Sekunde Strahlen; alter beamCostPerSecond-Schlüssel ohne Wirkung |
| `tweaks.laserPointer.effectCost` | 5 | Linsen-Ladung je Wirkung |
| `tweaks.balancing.echoSounderJumpCooldownTicks` | 480 | Echolot-Abklingzeit nach dem Sprung (24 s; ersetzt `echoSounderCooldownTicks` = 120, neue Namen, damit gespeicherte Altwerte nicht weiter gelten) |
| `tweaks.commands.killCommandRadius` | 100 | Reichweite von `/killboats`, `/killcarts` |
| `tweaks.optimization.xpClumpRadius` | 2.0 | Einsammel-Radius der XP-Kugeln |

Außerdem: Luftsprung-Abklingzeit wird vom Server synchronisiert; die zwölf Teleporter-Ziel-Felder
(`tweaks.spawn.spawn1X` …) haben endlich Namen/Tooltips; Tooltips von Bündel- und Oktant-Option
beschreiben jetzt, was der Code tut; `maxMultiplierLimit` und `laserPointer.showLine` sind aus dem
Bildschirm verschwunden (keine Optionen).

## Reiter „Server & Modpack Tuning“ (`server.*`, Besitzer 2026-09-28)

Philosophie des Besitzers: jede Gameplay-Stellschraube serverseitig verbindlich, an einem Ort für
Server- und Modpack-Ersteller; jede Geschwindigkeit/Reichweite mit Obergrenze, die Vanilla nicht
gefährdet. Standard = bisheriges Verhalten – einzige gewollte Änderung: Chunk-Loader laufen nur, solange
ihr Besitzer online ist. Grenzen stehen in `ServerTuning` (Konstanten) und werden in
`ServerTuningConfig#validate` (Laden, Befehl) und in den Zugriffen noch einmal angewandt.

| Gruppe | Optionen (Standard, Grenzen) | Wirkung |
|---|---|---|
| `features` | `airJump`, `dynamicLight`, `anvilRepairKeepsCost`, `placeVanillaItems`, `placeDisabledItems` (leer), `backpack`, `attractor`, `echoSounder`, `blueprint`, `oreDetector`, `levitatingBlocks` (alle an) | Aus = Funktion sofort aus (Meldung „auf diesem Server abgeschaltet“); Rezepte fallen beim nächsten `/reload` weg. Luftsprung: Server-Wächter + Client-Vorhersage; dyn. Licht aus räumt gesetzte Lichtblöcke beim nächsten Takt des Trägers; Rucksack: getragen öffnet nicht, abgestellt schon (kein Inhaltsverlust); schwebende Blöcke: nur Rezepte |
| `chunkLoaders` | `requireOwnerOnline` (an) | Loader hält Chunks nur mit Besitzer online; `ChunkLoaderRegistry` (SavedData der Oberwelt) weckt ihn beim Wiederkommen (Abgleich alle 100 Ticks im Server-Tick), Prüfung auch beim Setzen; Loader ohne Besitzer laufen immer |
| `dimensionLocks` | `chunkLoaderBlockedDimensions`, `flypadBlockedDimensions`, `echoSounderBlockedDimensions` (leer) | Dimension-IDs (Komma/Leerzeichen); Loader lädt nichts, Flypad gibt keinen Flug, Echolot springt weder hinein noch heraus. Per Befehl leert `""` die Liste |
| `laser` | `igniteFlammables`, `igniteTnt`, `igniteEntities` (an) | Einzelschalter des Linsenstrahls (Schmelzen/Trocknen/Kerzen bleiben) |
| `oreGeneration` | `endOres`, `astralitOre`, `nihilitOre` (an) | Bedingung `simplebuilding:config` mit Flag `astralitOre`/`nihilitOre` an den NeoForge-/Forge-Biom-Modifikatoren, Fabric über die Biomauswahl; nächster Weltstart, nur neue Chunks |
| `pads` | `strangerPadBreakSeconds` 60, `strangerPlateBreakSeconds` 10 (1..3600) | Abbauzeit Fremder; der Client rechnet den Fortschritt mit dem Server-Wert |
| `charges` | `lensMaxCharge` 640 (64..2560), `rotatorMaxCharge` 1024 (64..4096), `echoSounderMaxCharge` 1500 (150..6000) | **Neustart nötig**: Haltbarkeit wird beim Registrieren gelesen (`MAX_CHARGE`/`MAX_DAMAGE`); Client und Server brauchen dieselbe Datei (Modpack), sonst falsche Ladebalken |
| `tools` | `sledgehammerUpgradeSeconds` 5 (1..30), `reinforced/netherite/enderiteUpgradeDamagePerHit` 2/4/10 (0..64), `<stufe>ChiselCooldownTicks` 30/25/25/20/10/5/5 (2..200) | Hammer-Aufwertung = Schläge je Sekunde (Animation liest den Server-Wert); Meißel und Spachtel teilen den Wert ihrer Stufe |
| `machines` | `reinforced/netherite/enderiteHopperSpeed` 2/4/8, `...FurnaceSpeed` 2/4/8 (1..8) | Vielfaches von Vanilla; 8 = ein Trichter-Transfer je Tick (bisheriges Maximum); Ofen, Räucherofen, Schmelzofen teilen den Wert |
| `oreDetector` | `rangeMultiplier` 1,0 (0,25..1,5), `scanIntervalTicks` 20 (10..200) | Reichweite/Suchkugel aller Klassen (Tooltip zeigt den Server-Wert); Nebenhand halb so oft |
| `loot` | `globalLootMultiplier` 1,0 (0..3), 15 Struktur-Schalter (an), `tradePriceMultiplier` 1,0 (0,25..4) | Faktor = ganze Kopien je Pool plus eine mit Restwahrscheinlichkeit (`TunedLootEditor`; fertig gebaute Pools gerundet); Schalter nach Tabellen-Präfix (`ServerTuning#lootEnabledFor`), Köpfe ausgenommen. Preis: erster Preis-Slot der Mod-Angebote (26.x `VillagerTradePriceMixin`, 1.21.11 `TradeDefinition`), gerundet, 1..Stapel |
| `blueprint` | `maxBlocksPerTick` 32768 (1..32768) | Obergrenze je Tick; der Standard liegt über allem, was ein Bau heute nutzt |
| `trimStrengths` | 27 Faktoren, je Wirkung (1,0; 0..2) | Faktor auf die Rate einer Wirkung aus allen Mustern/Materialien; Deckel bleiben; Tooltips zeigen den Server-Wert |

**Rezepte abgeschalteter Funktionen** (`recipe/RecipeFilter` + `mixin/RecipeMapFilterMixin`): der private
`RecipeMap`-Konstruktor ist auf 1.21.11/26.2/26.3/26.4 gleich; dort fallen Rezepte im Namensraum
`simplebuilding` weg, deren ID zur Funktion passt (`backpack`, `magnet`/`attractor`, `echo_sounder`/`echo_compass`,
`blueprint`, `detector`, `levitating_`/`suspended_`, und für die Pad-Schalter `chunk_loader`,
`elytra_pad`, `flypad`, `spawn_teleporter`, `launchpad`, `potion_pad`, `laser_pointer`). Wirkt beim Laden
der Datenpakete; der Befehl sagt es (`ConfigOptions.RECIPES_ON_RELOAD`).

**Admin-Befehl**: `/simplebuilding chunkloaders list` (Position, Dimension, Besitzer, online, Bereich,
lädt/ruht – ohne Chunks zu laden) und `/simplebuilding chunkloaders remove <dimension> <pos>` (baut ab,
Item fällt; oder streicht einen verwaisten Eintrag).

**JEI**: Infoseiten von Chunk-Loader, Pads, Linse und Echolot bekommen eine Zeile „Dieser Server: …“
(`config/ServerTuningInfo`).

**Befehl**: `ConfigOptions.RESTART_REQUIRED` (Höchstladungen, End-Erze) und `RECIPES_ON_RELOAD` geben
nach `set` einen Hinweis; die Loot-Optionen stehen in `APPLY_ON_RELOAD`.

Tests: `ServerTuningTests` (Server-Wert gewinnt, Grenzen, Luftsprung-Schalter, Chunk-Loader offline/
Dimension/Register, Rezeptfilter, Loot-Faktor/Struktur-Schalter, Laser-Schalter, Werte bei Werkzeugen und
Maschinen); `ConfigOptionTests` pinnt jedes Feld samt Reiter.

## Neue Option hinzufügen – Checkliste

0. Gameplay-Stellschraube für Server/Modpacks? Dann in `ServerTuningConfig` (Reiter `server`), lesen
   über `ServerTuning.get()`, Grenze in `validate()` und als Konstante in `ServerTuning`; der Client
   bekommt den Wert automatisch.
1. Feld in `SimplebuildingConfig`/`TweaksConfig` (beide Linien), `@ConfigEntry.Gui.Tooltip`; auf
   oberster Ebene zusätzlich `@ConfigEntry.Category`. Standard = bisheriges Verhalten; Grenzen in
   `validate()`/`validatePostLoad()` und im lesenden Code.
2. Lang-Schlüssel en_us + de_de (beide Linien), Tooltip endet mit `Default:` / `Standard:`.
3. Neue Annotation? Dann auch im Forge-Shim.
4. Braucht der Client den Wert? In `SimpleTweaks.ServerValues` + `TweaksConfigPayload` aufnehmen;
   reine Client-Option in `ConfigOptions.CLIENT_SIDE`.
5. `ConfigOptionTests.EXPECTED_OPTIONS` (und für `tweaks.*` `TweaksTests`) nachziehen, eine
   Verhaltens-Assertion schreiben; `everyOptionHasNameTooltipAndTab` prüft Lang-Schlüssel und Reiter.
6. `python wiki/generate.py` (Wiki-Config-Seite) und die Zeile in `wiki/manual.json` → `configuration`.

### Bekannte Rezeptfilter-Grenze (Faktenpass 26.3)

`RecipeFilter.removes` prüft beim Strahlschalter noch `laser_pointer`. Das aktuelle
Resonanzstab-Rezept `amethyst_lens` bleibt deshalb verfügbar, obwohl die Benutzung
abgeschaltet wird. Dieser Faktenpass dokumentiert die Abweichung und ändert kein Gameplay.
