# Konfiguration (Stand 2026-09-28)

Kurzüberblick über die Config der Mod: wo sie liegt, wie der Bildschirm aufgebaut ist, wie der
Befehl funktioniert und was man beim Hinzufügen einer Option beachten muss. Die vollständige
Liste mit Tooltips steht im Wiki (`?tab=config`, erzeugt von `wiki/generate.py`) und im Spiel
unter `/simplebuilding config list`.

## Datei und Loader

- Datei: `config/simplebuilding.json` (Cloth Config / AutoConfig, Gson).
- Klassen: `common/src/shared/java/com/simplebuilding/config/SimplebuildingConfig.java` mit den
  Gruppen `tools`, `worldGen` und `tweaks` (`tweaks/TweaksConfig.java`). Die 1.21.11-Linie hat eine
  identische Kopie unter `mc1_21_11/shared/...` – beide Dateien immer gleich halten.
- Fabric: ModMenu öffnet den Bildschirm (`ModMenuIntegration`). NeoForge: Config-Knopf in der
  Mod-Liste (`SimplebuildingNeoForgeClient#buildConfigScreen`). 26.3 wie 26.2.
- Forge: kein Cloth Config für 26.x. Der Shim unter `forge/src/main/java/me/shedaniel/autoconfig/`
  liefert nur die Standardwerte, keine Datei, keinen Bildschirm. Seine Annotationen
  (`Category`, `TransitiveObject`, `BoundedDiscrete`, `ColorPicker`, `Tooltip`, ...) müssen jede
  Annotation abdecken, die die Config-Klassen benutzen, sonst baut Forge nicht.
- 26.4-Snapshot: kein Bildschirm (Cloth 26.3 stürzt dort beim Zeichnen ab), Datei und Befehl gehen.

## Bildschirm

Sieben Reiter, Reihenfolge = erstes Feld jeder Kategorie in `SimplebuildingConfig`:

| Reiter (`category`) | Inhalt |
|---|---|
| Werkzeuge & Bauen (`building`) | `tools.*`: Baustab-Hunger + Faktor, Magnet-Reichweite, Rotator-Kosten, Bündel-/Oktant-Bedienung, Vorschau-Deckkraft, Animationen |
| Verzauberungen & Rüstung (`equipment`) | Luftsprung + Abklingzeit, Besatz-Vorteile, Resonanz-Multiplikator |
| Kolben (`pistons`) | Verschleißbudgets, Endportalrahmen, Unzerstörbares anderer Mods |
| Pads & Tweaks (`tweaks`) | `tweaks.*` in Gruppen: Pads an/aus, Pad-Zeiten & -Stärke, Amethystlinse, Abstimmung, Spawn, Dimensionen, Befehle, Leistung |
| Beute, Handel & Welt (`world`) | `worldGen.*` (Loot, Kern-Chancen, Handel) und das Einsteiger-Handbuch |
| Darstellung (Client) (`visuals`) | Buch-Texturen, Besatz-Icons |
| Kompatibilität & Erweitert (`advanced`) | Kolben-Abbau-Ereignisse (Schutz-Mods), Dev-Kreativ-Tab |

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
Config-Befehl: `TweaksConfigPayload` (Raketen-Stapel, Boosts, Linse an/aus + Reichweite und seit
2026-09-28 die Luftsprung-Abklingzeit), `PistonConfigPayload` (die beiden Durchbruch-Optionen),
`TrimDataPayload` (Resonanz-Multiplikator). Alle neuen Balance-Optionen des Umbaus liest nur der
Server.

## Neu im Umbau 2026-09-28 (Standard = bisheriges Verhalten)

| Option | Standard | Wirkung |
|---|---|---|
| `tools.wandHungerMultiplier` | 1.0 | Faktor auf die Baustab-Erschöpfung je bezahltem Block (0 = kostenlos) |
| `tools.magnetRangeMultiplier` | 1.0 | Faktor auf die Magnet-Reichweite |
| `tools.rotatorChargePerTurn` | 1 | Ladung je Rotator-Drehung (0 = kostenlos) |
| `worldGen.buildingCoreLootChanceMultiplier` | 1.0 | Faktor auf die Baukern-Chancen in Truhen (0 = keine; bei `/reload`) |
| `tweaks.pads.enablePotionPads` | true | Trank-Pads an/aus |
| `tweaks.padTuning.teleporterWarmupTicks` | 100 | Wartezeit Spawn-Teleporter I–IV |
| `tweaks.padTuning.enderiteTeleporterWarmupTicks` | 60 | Wartezeit Enderit-Teleporter (V) |
| `tweaks.padTuning.launchpadStrengthMultiplier` | 1.0 | Faktor auf den Startrampen-Schub |
| `tweaks.padTuning.potionPadChargeStepTicks` | 20 | Länge eines Trank-Pad-Ladeschritts |
| `tweaks.padTuning.potionPadCooldownFactor` | 2.0 | Trank-Pad-Abklingzeit × Wirkdauer (0 = keine) |
| `tweaks.laserPointer.beamCostPerSecond` | 1 | Linsen-Ladung je Sekunde Strahlen |
| `tweaks.laserPointer.effectCost` | 5 | Linsen-Ladung je Wirkung |
| `tweaks.balancing.echoSounderCooldownTicks` | 120 | Echolot-Abklingzeit nach dem Sprung |
| `tweaks.commands.killCommandRadius` | 100 | Reichweite von `/killboats`, `/killcarts` |
| `tweaks.optimization.xpClumpRadius` | 2.0 | Einsammel-Radius der XP-Kugeln |

Außerdem: Luftsprung-Abklingzeit wird vom Server synchronisiert; die zwölf Teleporter-Ziel-Felder
(`tweaks.spawn.spawn1X` …) haben endlich Namen/Tooltips; Tooltips von Bündel- und Oktant-Option
beschreiben jetzt, was der Code tut; `maxMultiplierLimit` und `laserPointer.showLine` sind aus dem
Bildschirm verschwunden (keine Optionen).

## Neue Option hinzufügen – Checkliste

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
