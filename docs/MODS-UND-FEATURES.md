# Mods und Features – Übersicht

Stand 2026-10-09 (`claude-wave1`). Quelle der Wahrheit für IDs und Abhängigkeiten: `modules/modules.json`;
Details je Feature: `wiki/manual.json` bzw. `modules/<id>/wiki/manual.json`. Dieses Dokument bei jeder
Feature-, Modul- oder Abhängigkeitsänderung mitziehen (AGENTS.md §8 Punkt 7).

Alle Mods: Minecraft 26.3, Fabric + NeoForge, Forge 26.3 experimentell (opt-in). Jede Sub-Mod muss allein
spielbar sein (`docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, Standalone-Testziel je Modul).

## Struktur

**Super-/Sub-Mod-Regel** (`docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md`): Sub-Mods sind eigenständige Jars;
eine Super-Mod *requires* und bündelt ihre Sub-Mods und enthält nur Übergreifendes (Config-Oberfläche, Guide,
Befehle). In ihrer Config steht je Sub-Mod oben „Simple XY aktivieren“. Gemeinsames gehört in **simplelib**.

```
SimpleBuilding (Super-Mod, geplant)          Simple Quality of Life (Super-Mod, geplant)
 ├─ simplelib (gebündelt)                      ├─ simpleinterfaces (Sub-Mod, vorhanden)
 ├─ Simple Trims (geplant)                     ├─ Simple Loot (geplant: Tresor-Abklingzeit)
 └─ Simple Maps (in Arbeit)                    └─ weitere Teile nach Besitzer-Entscheidung
                                              Simple Combat (geplant: Schärfe schneidet Gras)
Eigenständig: Money, Riding, Models, Fun, Visuals, Sounds, Dimensions, Sandwiches, Tweaks (braucht SB)
```

## Abhängigkeiten (heute)

| Mod (ID) | Pflicht | Optional (Integration) | Bündelt |
|---|---|---|---|
| SimpleBuilding (`simplebuilding`) | Cloth Config | JEI, REI, ModMenu, Jade, AppleSkin, Mouse Tweaks, Trinkets, Curios | simplelib |
| SimpleLib (`simplelib`) | – | – | – |
| Simple Interfaces (`simpleinterfaces`) | Cloth Config, simplelib | SimpleBuilding, ModMenu | simplelib |
| Simple Quality of Life (`simplequalityoflife`) | Cloth Config | SimpleBuilding, ModMenu | simplelib |
| Simple Riding (`simpleriding`) | Cloth Config | SimpleBuilding (Enderit-Hufeisen), ModMenu | simplelib |
| Simple Sandwiches (`simplesandwiches`) | – | SimpleBuilding | simplelib |
| Simple Money (`simplemoney`) | Cloth Config | alle Simple-Mods (Preislisten), JEI, ModMenu | – |
| Simple Models (`simplemodels`) | – | SimpleBuilding, ModMenu | – |
| Simple Fun (`simplefun`) | Cloth Config | ModMenu, JEI, Jade | – |
| Simple Visuals (`simplevisuals`) | Cloth Config | SimpleBuilding, Simple Models, ModMenu | – |
| Simple Sounds (`simplesounds`) | Cloth Config | Simple Visuals (Stufe koppeln), SimpleBuilding, ModMenu | – |
| Simple Dimensions (`simpledimensions`) | Cloth Config | SimpleBuilding, ModMenu, Claim-Mods (Flan, FTB Chunks, OPAC, GriefDefender, ClaimChunk) | – |
| Simple Tweaks (`simpletweaks`) | SimpleBuilding | Simple Dimensions | – |
| Wiring Example (`wiringexample`) | – | – | – (nur Testmodul) |

## Features je Mod

### SimpleBuilding
- **Bauwerkzeuge:** Vorschlaghammer (3×3, Formen in Treppe/Stufe/Achtel), Meißel (Blöcke in der Welt umformen),
  Baustab (Flächen), Oktant (Messen/Planen), Blaupause (Bau-Code, Editor mit 3D-Vorschau, Scannen/Bauen),
  Rotator, Farbpinsel (Goldpinsel) mit Farbkasten in 4 Stufen.
- **Kerne** (Kupfer bis Enderit) als Werkzeugstufen, mit Hand-Animationen; In-World-Umwandlungen per Schlag.
- **Helfer/Gadgets:** Attraktor (mehrere ziehen zum gemeinsamen Mittelpunkt), Detektor (Erz-Sonar),
  Resonanzstab (Laser, Scannen), Echolot, Geschwindigkeitsmesser.
- **Lager:** Rucksäcke (4 Stufen), Truhen- und Fallen-Truhen-Stufen (Kupfer→Enderit), Shulkerkisten-Stufen,
  Bündel-Stufen und Köcher, abgestellte Bündel, Astral-/Nihil-Gewölbe, Mod-Trichter mit Filter-Prinzip.
- **Fahrzeuge (26.3):** Güter-, Antriebs- und Trichterloren sowie Truhenboote (jede Holzart) in den Stufen
  Verstärkt/Netherit/Enderit – Plätze wie die Truhe, Brenndauer/Tempo wie Ofen/Trichter der Stufe, Filter-Prinzip.
- **Maschinen:** schnellere Öfen (3 Stufen je Ofenart), Auto-Schmied, Autonomer Crafter, Befiederungs- und
  Schmiedetisch mit Rezeptbuch, stärkere Kolben, Astral-/Nihil-Kolben.
- **Schmelztiegel-Teil** (auf simplelib): Enderit-Tiegel/-Fass, Seelen-Lava (Fluid, Weltgenerierung, Seelenbrand),
  Kupfer-, Keramik- und Enderit-Eimer (Enderit fasst 2 Eimer).
- **End-Inhalte:** Astralit/Nihilit/Enderquarz-Paletten und Erze, Astral-/Nihil-Redstone als getrennte
  Signalkanäle, Schalter/Lampen, Astral-/Nihil-Schienen, Enderit-Material (leeresicher), seltene verstärkte Shulker.
- **Kampf/Ausrüstung:** Pfeile aus Teilen (Spitze/Schaft/Feder), Rüstungsbesatz-Boni (Resonanz, Statusfeld im
  Inventar), Trank „Crafty Shulker“, Trainingspuppe (Skin per Namensschild), Rüstungsständer mit Armen/Tausch.
- **Deko/Platzieren:** abgelegte Vorlagen, Kleinteile-Häufchen, Schachbrett-Blöcke und Schachfiguren (Achtel),
  senkrechte Stäbe, Hängematte (jeder Winkel), Schallplatten je Dimension + Musik-/Noten-Verstärker.
- **Verzauberungen:** Abbau-, Bau-, Lager-/Spieler-Verzauberungen, Luftsprung.
- **Welt/Wirtschaft:** Loot-Balance, bessere Strukturtruhen, Dorfbewohner-/Händlerangebote.
- **Pads und Tweaks** (aus Simple Tweaks übernommen): Spawn-/Elytra-/Trank-Pads in Stufen, Server-Optionen.
- **Integration:** JEI/REI-Kategorien, Jade, Modpack-/Claim-Schnittstellen, Guide-Bücher, Kreativtabs mit Schalter
  „in Vanilla-Tabs einsortieren“.

### SimpleLib (gemeinsame Bibliothek)
Schmelztiegel (Eisen/Verstärkt/Netherit, beheizt vom Block darunter, Axt-Weg ohne SB), Kupfer-/Netherit-Fässer am
Tiegel, warmes Essen, verstärkter Kessel (erbt Vanilla-Kessel inkl. Milch), Dorf-Feldküchen, Stapelgrößen-Regel
(`StackLimits`), UI-Bausteine des Simple-Stils (Kästen, Slots, Symbole, Flammen, Filter-Knopf).

### Simple Interfaces (früher Simple Containers, Sub-Mod von QoL, clientseitige Optik)
Simple-Stil für Truhen, Fässer, Shulker, Trichter, Werfer/Spender, Crafter, Reittier-Inventare, Arbeitsblöcke
(Werkbank, Öfen, Braustand, Leuchtfeuer, Zaubertisch), Amboss/Schleifstein/Steinsäge/Webstuhl/Kartentisch/
Schmiedetisch, Handel, Spieler-Inventar; Mod-Bildschirme im gleichen Stil; Vergleichsschalter Simple/Vanilla.

### Simple Quality of Life
Kriechen, Autowalk, schnelles Leiterklettern/-rutschen, Eisläufer auf Pulverschnee, Federfall schützt Acker, Ernten
mit Hacke, Ofen mit Lava füllen, Schärfe schneidet Pflanzen, Haltbarkeitsbonus, Kreaturen stummschalten, dauerhafte
Jungtiere, Piglins ignorieren Gold, Wettersteuerung, Tresor-Abklingzeit, Sparsamkeit-Verzauberung, Reparatur hält
Ambosskosten, verknüpfte Behälter (Vormerken), Shulkerkisten/Endertruhen aus dem Inventar öffnen.

### Simple Riding
Rückenwind und Sprungkraft (Sättel, Geschirre, Nautilus), Rüstungsverzauberungen für Reittiere, Hufeisen
(Kupfer bis Enderit, 4 Hufplätze, Satzboni), Beute/Handel, serverseitige Reitgrenzen.

### Simple Sandwiches
Sandwiches aus Brot + bis zu 5 Zutaten (Effekte kombiniert), Schneidebrett, Messer, Käse und Butter aus dem Kessel,
Essen direkt aus Bündeln, warme Sandwiches über simplelib.

### Simple Money
Geldschein-Herstellung (Spezialpapier, Fasern, Rohlinge, Schmelzen), zusätzliche Dorfbewohner-/Händlerangebote,
Geld in Truhen, verknüpfte Preislisten für andere Simple-Mods.

### Simple Models
Server-freigegebene eigene Item-Modelle, Zuweisung am Amboss, durchsuchbarer Modell-Browser, Admin-Reload.

### Simple Fun
Schleichwurf (Yeet), Wurfziegel und Ziegelschneeball, Schweinverwandlung, „Schadlos“-Verzauberung, Rückstoß V,
Spieler- und Tierköpfe (mit Fähigkeiten: melken, scheren, Eier, Trüffelnase), kleine kosmetische Freuden.

### Simple Visuals (clientseitig)
Geschwindigkeitslinien, Aufnahmehinweise, Elytra-Neigungshilfe, Effekt-Zeitleisten, Spielerköpfe in Locator und Chat,
lokale Todeskoordinaten, Karten-Tooltip, Item-Info, Schadensanzeigen, Biomhinweise, Amboss-Formatierung,
CIT-Kompatibilität, 12 Immersions-Partikeleffekte in 5 Stufen.

### Simple Sounds (clientseitig)
Vanilla-Klang-Gegenstücke zu den 12 Visuals-Effekten, Intensität gekoppelt an Simple Visuals, harte Grenzen.

### Simple Dimensions
Skyblock-, Abbau- und Reisedimension, Portale aus Glowstone-Bögen, sichere Reise und Rückkehr, Datenpakete,
Claim-Prüfung.

### Simple Tweaks
Kompatibilität alter Welten (ID-Zuordnung), Claim-System in Stufen (standardmäßig aus).

## Geplant / in Arbeit
| Mod | Stand | Inhalt | Quelle |
|---|---|---|---|
| Simple Maps (`simplemaps`, Sub-Mod SB) | in Arbeit (`claude-q-maps`) | Wegfinder-Karte (unendlich, Spieler mittig), GUI mit Lesezeichen, Wegpunkte, Locator-Bar, Karte je Dimension, Fundorte | `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md` |
| Simple Trims (Sub-Mod SB) | geplant, Frage F1 offen | alle Schmiedevorlagen inkl. Platzieren; Axt ohne SB, Hammer mit SB | PLAN-N18 |
| Simple Respawn (`simplerespawn`) | wartet auf Besitzer-Konzept | Niedergeschlagen statt Tod im Mehrspieler, Wiederbeleben, danach 3 Herzen/0 Hunger | `docs/ai/KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md` |
| QoL-Aufteilung | Besitzer entscheidet | Sub-Mods für Container (fertig), Bewegung, Landwirtschaft, Werkzeuge/Haltbarkeit, Kreaturen, Wetter, Tresor (→ Simple Loot); Schärfe/Gras → Simple Combat; Kreaturen-Logik nach simplelib | QUEUE N14 |
| Mobs/Effekte | Konzepte | End-Mob, Shellker (Schwerkraft), Deceiver, Furcht-Mob, Seelenfeuer-Lohe; Effekte Zittern/Trugbild/Verblasst | `docs/ai/KONZEPT-MOBS-2026-10-07.md`, `KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` |

Offene Punkte und Reihenfolge: `docs/ai/ROADMAP-2026-10-09.md`.
