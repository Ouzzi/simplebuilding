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
SimpleBuilding (Super-Mod, geplant)          Simple Quality of Life (Super-Mod)
 ├─ simplelib (gebündelt)                      ├─ simpleweather, simpleinterfaces, simpleloot,
 ├─ Simple Ovens (Pflicht, gebündelt)          │
 ├─ Simple Mobheads (Pflicht, gebündelt)      │
 ├─ Simple Trims (gebündelt)                   ├─ simplemobmanners, simplefarming, simpletools,
 └─ Simple Maps (in Arbeit)                    └─ simplemovement (alle Sub-Mods, gebündelt)
                                                 (docs/ai/PLAN-QOL-AUFTEILUNG-2026-10-10.md)
Eigenständig: Money, Riding, Models, Fun, Visuals, Sounds, Dimensions, Sandwiches, Tweaks (braucht SB)
```

## Abhängigkeiten (heute)

| Mod (ID) | Pflicht | Optional (Integration) | Bündelt |
|---|---|---|---|
| SimpleBuilding (`simplebuilding`) | Cloth Config, Simple Ovens (26.3) | JEI, REI, ModMenu, Jade, AppleSkin, Mouse Tweaks, Trinkets, Curios | simplelib, simpleovens (+ optionale Sub-Mods) |
| Simple Ovens (`simpleovens`) | simplelib | SimpleBuilding (Vorschlaghammer statt Axt) | simplelib |
| SimpleLib (`simplelib`) | – | – | – |
| Simple Interfaces (`simpleinterfaces`) | Cloth Config, simplelib | SimpleBuilding, Simple QoL (Schalter), ModMenu | simplelib |
| Simple Quality of Life (`simplequalityoflife`) | Cloth Config, alle sieben Sub-Mods | SimpleBuilding, ModMenu | simplelib, simpleweather, simpleinterfaces, simpleloot, simplemobmanners, simplefarming, simpletools, simplemovement |
| Simple Weather (`simpleweather`) | Cloth Config | Simple QoL (Schalter), ModMenu | – |
| Simple Loot (`simpleloot`) | Cloth Config | Simple QoL (Schalter), ModMenu | – |
| Simple Mob Manners (`simplemobmanners`) | Cloth Config | Simple QoL (Schalter), ModMenu | – |
| Simple Farming (`simplefarming`) | Cloth Config | SimpleBuilding, Simple QoL (Schalter), ModMenu | – |
| Simple Tools (`simpletools`) | Cloth Config | SimpleBuilding, Simple QoL (Schalter), ModMenu, Simple Money (Geldscheine als Opfer, optionaler Tag) | – |
| Simple Movement (`simplemovement`) | Cloth Config | Simple QoL (Schalter), ModMenu | – |
| Simple Riding (`simpleriding`) | Cloth Config | SimpleBuilding (Enderit-Hufeisen), ModMenu | simplelib |
| Simple Sandwiches (`simplesandwiches`) | – | SimpleBuilding | simplelib |
| Simple Money (`simplemoney`) | Cloth Config | alle Simple-Mods (Preislisten), JEI, ModMenu | – |
| Simple Models (`simplemodels`) | – | SimpleBuilding, ModMenu | – |
| Simple Fun (`simplefun`) | Cloth Config | ModMenu, JEI, Jade | – |
| Simple Mobheads (`simplemobheads`, Pflicht-Sub-Mod SB) | Cloth Config, simplelib | SimpleBuilding (Extra-Kopf-Slot), Simple Mobs/End/End Dragon Fight (Köpfe ihrer Mobs), JEI, ModMenu | simplelib, framework |
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
- **Maschinen:** schnellere Öfen (3 Stufen je Ofenart; auf 26.3 aus der Pflicht-Sub-Mod Simple Ovens), Auto-Schmied, Autonomer Crafter, Befiederungs- und
  Schmiedetisch mit Rezeptbuch, stärkere Kolben, Astral-/Nihil-Kolben.
- **Schmelztiegel-Teil** (auf simplelib): Enderit-Tiegel/-Fass, Seelen-Lava (Fluid, Weltgenerierung, Seelenbrand),
  Kupfer-, Keramik- und Enderit-Eimer (Enderit fasst 2 Eimer).
- **End-Inhalte:** Astralit/Nihilit/Enderquarz-Paletten und Erze, Astral-/Nihil-Redstone als getrennte
  Redstone-Linien (Geräte nehmen jede Linie an), Brücken-Redstone, Redstone-Blöcke der Linien, Lampen, End-Kolben,
  Astral-/Nihil-Schienen, Enderit-Material (leeresicher), seltene verstärkte Shulker.
- **Kampf/Ausrüstung:** Pfeile aus Teilen (Spitze/Schaft/Feder), Rüstungsbesatz-Boni (Resonanz, Herz-Knopf mit
  8 Stufen im Inventar), Extra-Kopf-Slot im Inventar (Mob-Kopf/Kürbis, Effekte, am Spieler unter dem Helm), Trank „Crafty Shulker“, Trainingspuppe (Skin per Namensschild), Rüstungsständer mit Armen/Tausch.
- **Deko/Platzieren:** abgelegte Vorlagen, Kleinteile-Häufchen, Schachbrett-Blöcke und Schachfiguren (Achtel),
  senkrechte Stäbe, Hängematte (jeder Winkel), Schallplatten je Dimension + Musik-Verstärker (spielen einen
  Plattenspieler bis 63 Blöcke mit, der Plattenspieler bleibt Vanilla) / Noten-Verstärker (spielen Notenblöcke ebenso per Funk mit), Metall-Stangen (Eisen bis
  Enderit, Blitzableiter mit eigener Reichweite; früher „Rod“).
- **Verzauberungen:** Abbau-, Bau-, Lager-/Spieler-Verzauberungen, Luftsprung.
- **Welt/Wirtschaft:** Loot-Balance, bessere Strukturtruhen, Dorfbewohner-/Händlerangebote.
- **Pads und Tweaks** (aus Simple Tweaks übernommen): Spawn-/Elytra-/Trank-Pads in Stufen, Server-Optionen.
- **Integration:** JEI/REI-Kategorien, Jade, Modpack-/Claim-Schnittstellen, Guide-Bücher, Kreativtabs mit Schalter
  „in Vanilla-Tabs einsortieren“.

### SimpleLib (gemeinsame Bibliothek)
Schmelztiegel (Eisen/Verstärkt/Netherit, beheizt vom Block darunter, Axt-Weg ohne SB; In-World-Bau aus Kessel + 16 Eisenbarren
mit Stufe je Schlag, Tiegel-Upgrades 12 Schläge mit sichtbarer Material-Einheit alle 3 Schläge, N51), Kupfer-/Netherit-Fässer am
Tiegel, warmes Essen, verstärkter Kessel (erbt Vanilla-Kessel inkl. Milch), Dorf-Feldküchen, Stapelgrößen-Regel
(`StackLimits`), UI-Bausteine des Simple-Stils (Kästen, Slots, Symbole, Flammen, Filter-Knopf). Dienste der
Modulaufteilung: `IdMoves` (ID-Umzüge), Datentyp `in_world_upgrades` mit Übergabe-Handlern und Tags
`#simplelib:hammers`/`#simplelib:upgrade_tools`, `OctetCells` (Achtelzellen je Material). Gemeinsame Materialien
(Stufe 3d, `simplelib:*`, früher SB): Diamantkiesel, Rissiger Diamant (+Block, Amboss-Zerschlagen), Netheritklumpen,
Enderit-Grundmaterial (Fragment, Platten, Barren, Klumpen, Block), Basis-/Enderit-Aufwertungsvorlage, `c:`-Tags;
Rezepte/Zerschlagen nur, wenn eine Mod sie anfordert (`requestMaterial`, Condition `simplelib:material_requested`; SB fordert alle an).

### Simple Interfaces (früher Simple Containers, Sub-Mod von QoL, clientseitige Optik)
Simple-Stil für Truhen, Fässer, Shulker, Trichter, Werfer/Spender, Crafter, Reittier-Inventare, Arbeitsblöcke
(Werkbank, Öfen, Braustand, Leuchtfeuer, Zaubertisch), Amboss/Schleifstein/Steinsäge/Webstuhl/Kartentisch/
Schmiedetisch, Handel, Spieler-Inventar; Mod-Bildschirme im gleichen Stil; Vergleichsschalter Simple/Vanilla;
Inventar-Helligkeit Hell/Dämmerung/Dunkel (Einstellung, wirkt über SimpleLib auf alle Mod-Kästen).

### Simple Weather (Sub-Mod von QoL)
Wetter abschalten (Server), Regendichte (Client); eigene Config `config/simpleweather.json`, alte QoL-Werte werden
einmalig übernommen; in QoL mit „Simple Weather aktivieren“ abschaltbar.

### Simple Loot / Creatures / Farming / Tools / Movement (Sub-Mods von QoL, 2026-10-11)
Je eigene Config `config/<id>.json` (alte `qOL.*`-Werte werden beim ersten Start einmalig übernommen), eigener Befehl
`/<id>`, in QoL mit „Simple XY aktivieren“ abschaltbar, allein spielbar.
- **Simple Loot:** Tresor-Abklingzeit in Tagen.
- **Simple Mob Manners** (bis 13.10. Simple Creatures, `simplecreatures`; Config, Befehl und Schalter werden übernommen): Kreaturen stummschalten (Typ/Namenssuffix), dauerhafte Jungtiere, Piglins ignorieren Goldbesatz/-werkzeug.
- **Simple Farming:** Federfall schützt Acker, Schärfe schneidet Pflanzen, Ernten mit Hacke.
- **Simple Tools:** Haltbarkeitsbonus, Sparsamkeit-Verzauberung (ID bleibt `simplequalityoflife:thrift`), Reparatur hält
  Ambosskosten, Ofen mit Lava füllen.
  Seit 2026-10-12: **Seelengebunden** (`simpletools:soulbound`, nur Buch: fahrender Händler 5 % für 24–32 Diamanten, End-City-Schatz 2 %; Amboss immer 35 Level, Schleifstein entfernt; bleibt beim Tod im selben Platz, Haltbarkeit danach min(aktuell, 50)) und **Todesgewölbe** (`simpletools:death_vault`, Block mit GUI: Todes-Buchführung aller Tode, nur despawnte/verbrannte/explodierte/ins Leere gefallene Todes-Drops rückkaufbar, Opfer 4 Diamanten je Rückgabe, gewichtet nach Aktualität und Wert). Plan: `docs/ai/PLAN-SOULBOUND-DEATHVAULT-2026-10-12.md`.
- **Simple Movement:** Kriechen (`/crawl`), Autowalk, schnelles Leiterklettern/-rutschen, Frostläufer auf Pulverschnee;
  alte QoL-Tastenbelegungen werden übernommen.
- **Simple Interfaces** übernimmt verknüpfte Behälter (Vormerken) und Shulkerkisten/Endertruhen aus dem Inventar
  (Serverwerte in `config/simpleinterfaces-server.json`).

### Simple Quality of Life (Super-Mod)
Bündelt die sieben Sub-Mods, je ein Schalter „Simple XY aktivieren“ (auch `/simplequalityoflife submod <name> <bool>`),
Handbuch über alle Sub-Mods und FTB-Startquest.

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
kleine kosmetische Freuden. Seit 1.4.0 ohne Köpfe (Tier- und Spielerköpfe → Simple Mobheads).

### Simple Mobheads (Pflicht-Sub-Mod von SimpleBuilding, allein spielbar)
93 Mob-Köpfe aus `modules/simplemobheads/data/mobs.json` (Generator `tools/generate_heads.py`): jeder 26.3-Mob ohne
Vanilla-Kopf, die Mobs der Simple-Mods, die 4 Tierköpfe aus Simple Fun und die 12 Köpfe aus SimpleBuilding
(`simplefun:*`/`simplebuilding:*` → `simplemobheads:*` über IdMoves). Drop nur durch geladenen Creeper (Variante des
Mobs als Komponente, Babys normaler Kopf, Wither aus/Warden an, Spieler-Kill-Chance optional aus), PvP-Spielerkopf,
Tarnung gegen den eigenen Mob, Notenblock-Stimme, Gruß beim Schleichen, 28 Fähigkeiten mit Schaltern, eigener Tab +
Suchtab hinter dem Drachenkopf, `/simplemobheads` (Alias `/simplefun pvp headDrops`). Plan
`docs/ai/PLAN-SIMPLE-MOBHEADS-2026-10-13.md`.

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
Kompatibilität alter Welten (ID-Zuordnung), Claim-System (standardmäßig an). Rechte je Person (B D U C R A M, Rollen
`@guest/@helper/@member/@manager`), Rechte-Code mit Server-Parser, Ledger v2 mit Migration (Plan `docs/ai/PLAN-CLAIM-URKUNDE-GUI-2026-10-13.md`,
Phase 1; Editor, Claim-Tisch und Maps-Ebene folgen).

## Geplant / in Arbeit
| Mod | Stand | Inhalt | Quelle |
|---|---|---|---|
| Simple Maps (`simplemaps`, Sub-Mod SB) | in Arbeit (`claude-q-maps`) | Wegfinder-Karte (unendlich, Spieler mittig), GUI mit Lesezeichen, Wegpunkte, Locator-Bar, Karte je Dimension, Fundorte | `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md` |
| Simple Mobs (`simplemobs`, Sub-Mod SB, optional) | Stufe 1 (`claude-q-deceiver`) | Mob Deceiver (Endgame, Wellen aus Fake-/Echt-Mobs, Mirage-Puls über SB-Effekte, falls SB geladen), Täuscherstoff, Spawn-Ei; Furcht-Mobs Schreckensritter (Eisengolem-T mit Amboss in der Mitte, Skelettschädel, Eisenhelm aufsetzen) und Hohler Wächter (End-Außeninseln, Blickmechanik) mit Zitter-Aura (SB-Effekt `shivering`, ohne SB eigener), Ruhe-Amulett, Trank des Fokus (`claude-fear-mobs`, Plan `docs/ai/PLAN-FURCHT-MOBS-2026-10-12.md`); Verdorrter Wither (Wither-Figur auf Netheritblock, 600 Leben, 3 Phasen mit angekündigten Angriffen) mit Witherherz und Biom-Pinsel (Material in der Nebenhand malt Biomzellen, Claim-sicher; `claude-wither`, Konzept `docs/ai/KONZEPT-WITHER-BIOMPINSEL-2026-10-10.md`); Simple-Mobs-Handbuch; spielbar auch ohne SB | `docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` |
| Simple End (`simpleend`, Sub-Mod SB, optional; Super-Mod von End Dragon Fight) | `claude-simple-end` | End-Holz Aschholz/Dämmerholz (volles Vanilla-Holzset inkl. Boote, Schilder, Regal, Laub; mit SB zusätzlich Holzwerk, Holz-/Stamm-Achtel, gestufte Kistenboote, mit Sandwiches Schneidebretter – `claude-endwood-full`, `docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md`), Knospen (Licht ≥ 9 großer Baum, 2×2 Riesenbaum, dunkel + Knochenmehl Stöcke-Gestrüpp), End-Stöcke-Block, Schwebfrucht (4 s Sanfter Fall + Sprung), End-Gras/-Busch (N42), Biome Aschental/Dämmerlande auf äußeren End-Inseln, Items leere-sicher + doppelte Liegezeit, Knabbermilbe (passiv, knabbert Endstein → durchwühlter Endstein, erholt sich); spielbar ohne SB | `docs/ai/PLAN-SIMPLE-END-2026-10-12.md` |
| Simple End Dragon Fight (`simpleenddragonfight`, Sub-Mod von Simple End, optional) | Stufe 1 (`claude-dragon`) | Drache: +1 volle Lebensleiste je Spieler, +15 % je weiterer Beschwörung (300 bzw. 200 je Spieler, Schonfrist 60 s), +20 % Tempo, Eintritts-Animation je Spieler (Warteschlange), ab halbem Leben Dragonmites alle 30 s (Drop Drachen-Chitin); spielbar auch ohne SB | `docs/ai/KONZEPT-END-DRAGON-FIGHT-2026-10-11.md` |
| Simple Respawn (`simplerespawn`, Sub-Mod SB, optional) | Stufe 1 (`claude-respawn`) | Niedergeschlagen statt tot (nur mit Mitspielern online, Config: immer), Ausbluten 60 s, eigener Bildschirm (Zuschauen mit freier Kamera, Inventar nur ansehen, Aufgeben), Wiederbeleben durch 5 s Rechtsklick-Halten (danach 3 Herzen, 0 Hunger), Monster ignorieren Niedergeschlagene, Logout/Absturz = Vanilla-Tod (kein Dupe), Handbuch, Fortschritt Erste Hilfe; spielbar auch ohne SB | `docs/ai/PLAN-SIMPLE-RESPAWN-2026-10-12.md` |
| Simple Beacons (`simplebeacons`, Sub-Mod SB, optional; braucht `simplelib`) | Stufe 1 (`claude-beacons`) | Starke Aufwertungsvorlage (Netherstern, Duplikation), Starkes Leuchtfeuer (eigener Block, 6 Ebenen, Reichweite 60/70, 6 Zusatzeffekte E5/E6, zweispaltiger Bildschirm, Basis-Tag `strong_base_blocks`), Starker Aquisator (7×7×7-Rahmen, bis 144 Blöcke nur unter Wasser, Delfingunst, Angriff 12), Aufwertung am Schmiedetisch oder in der Welt (Axt/Hammer, 8 Schläge, Daten bleiben), Server-Konfig mit Deckeln; spielbar ohne SB | `docs/ai/PLAN-SIMPLE-BEACONS-2026-10-13.md` |
| Simple Ovens (`simpleovens`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt; braucht `simplelib`) | Modulaufteilung Stufe 4 (`claude-split-s4-ovens`) | Verstärkte/Netherit-/Enderit-Öfen, -Räucheröfen, -Schmelzöfen (2×/4×/8×, doppelte Erfahrung ab Netherit, Tempo aus eigener Server-Konfig, übernimmt SBs Werte beim ersten Start), Antriebsloren der drei Stufen, Menü-Mixin für lange Garzeiten, Aufwertung in der Welt über SimpleLibs `in_world_upgrades` (allein Axt, mit SB Vorschlaghammer), eigener Tab, Handbuch, Fragentisch, Preise; alte `simplebuilding:`-IDs (Blöcke, Items, Block-Entities, Loren, Rezepte, Rezept-Advancements) migrieren über SimpleLib-IdMoves; auf 26.3 nicht mehr in SB (26.2 unverändert); allein spielbar (Enderit-Beute über SimpleLib) | `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md` |
| Simple Trims (`simpletrims`, Sub-Mod SB, optional) | Stufen 2–4 (`claude-trims2`) | Vorlagen Leuchtend/Strahlend/Pulsierend (`simpletrims:*`, alte `simplebuilding:`-IDs migrieren über SimpleLib-IdMoves), Aufwertungen am Schmiedetisch (eigener Rezepttyp, auch Auto-Smither), Strahlkraft-Licht, Leucht-/Puls-Darstellung, Besatzvorlagen ablegen und in der Welt schmieden, Werkzeugregel wie Tiegel (Axt allein, Hammer sobald ein Partner die Axt-Wege abschaltet), Fortschritte, Handbuch, Preise; bündelt SimpleLib; auf 26.3 nicht mehr in SB (26.2 unverändert); Besatz-Boni bleiben in SB | `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md` |
| QoL-Aufteilung | Besitzer entscheidet | Sub-Mods für Container (fertig), Bewegung, Landwirtschaft, Werkzeuge/Haltbarkeit, Kreaturen, Wetter, Tresor (→ Simple Loot); Schärfe/Gras → Simple Combat; Kreaturen-Logik nach simplelib | QUEUE N14 |
| Mobs/Effekte | Konzepte | End-Mob, Shellker (Schwerkraft), Deceiver, Furcht-Mob, Seelenfeuer-Lohe; Effekte Zittern/Trugbild/Verblasst | `docs/ai/KONZEPT-MOBS-2026-10-07.md`, `KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` |

Offene Punkte und Reihenfolge: `docs/ai/ROADMAP-2026-10-09.md`.
