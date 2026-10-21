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
 ├─ Simple Crucibles (Pflicht, gebündelt)      │
 ├─ Simple Octets (Pflicht, gebündelt)         │
 ├─ Simple Hammers (Pflicht, gebündelt)        │
 ├─ Simple Storage (Pflicht, gebündelt)        │
 ├─ Simple Mobheads (Pflicht, gebündelt)      │
 ├─ Simple Trims (gebündelt)                   ├─ simplemobmanners, simplefarming, simpletools,
 └─ Simple Maps (in Arbeit)                    └─ simplemovement (alle Sub-Mods, gebündelt)
                                                 (docs/ai/PLAN-QOL-AUFTEILUNG-2026-10-10.md)
Eigenständig: Money, Riding, Models, Fun, Visuals, Sounds, Dimensions, Sandwiches, Tweaks (braucht SB)
```

## Abhängigkeiten (heute)

| Mod (ID) | Pflicht | Optional (Integration) | Bündelt |
|---|---|---|---|
| SimpleBuilding (`simplebuilding`) | Cloth Config, Simple Ovens, Simple Crucibles, Simple Octets, Simple Blocks, Simple Hammers, Simple Storage (26.3) | JEI, REI, ModMenu, Jade, AppleSkin, Mouse Tweaks, Trinkets, Curios | simplelib, simpleovens, simplecrucibles, simpleoctets, simpleblocks, simplehammers, simplestorage (+ optionale Sub-Mods) |
| Simple Ovens (`simpleovens`) | simplelib | SimpleBuilding (Vorschlaghammer von Simple Hammers statt Axt) | simplelib |
| Simple Crucibles (`simplecrucibles`) | simplelib | SimpleBuilding (Vorschlaghammer statt Axt, Advancements), JEI, Jade | simplelib |
| Simple Blocks (`simpleblocks`) | simplelib | SimpleBuilding (Schwerkraftblöcke mit SB-Material, Suchtab-Platzierung, gemeinsamer Tab), Simple End (Formsäge-Holz), JEI | simplelib |
| Simple Octets (`simpleoctets`) | simplelib | SimpleBuilding (Paletten-Achtel, Vorschlaghammer schnitzt, Schach-Achtel mischen), Simple End (Holz-/Stamm-Achtel), Simple Blocks (2 Achtel), JEI | simplelib |
| Simple Hammers (`simplehammers`) | simplelib | SimpleBuilding (Besatz, Splitter, Platten, Oktant-Auswahl, Doppeltruhen, Advancements, Hand-Animation/Umriss), Simple Octets (Achtel schnitzen), Simple Crucibles (Bau/Andocken/gestufte Aufwertung), JEI | simplelib |
| Simple Storage (`simplestorage`) | simplelib | SimpleBuilding (Tab-Zeilen, Teststation, Lager-Handbuch, Aufwerten abgelegter Schalen), Simple End (Aschholz-/Dämmerholzkiste, Aschholz-/Dämmerholz-Truhenboote), Simple Hammers (Hammer statt Axt beim Aufwerten) | simplelib |
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
| Simple Tweaks (`simpletweaks`) | simplelib | SimpleBuilding, Simple Dimensions | simplelib, framework |
| Simple Stats (`simplestats`) | – (Fabric API) | SimpleBuilding (Hammer-Flächenabbau zählt jeden Block), FTB Quests (Handbuch-Kapitel) | framework |
| Simple Optimization (`simpleoptimization`, Super-Mod, nur Fabric 26.3) | Simple Opt Render, Simple Opt Memory, Simple Opt Server | – | simpleoptrender, simpleoptmemory, simpleoptserver, framework |
| Simple Opt Render (`simpleoptrender`, Client) | – (Fabric Loader) | weicht Cull Leaves, More Culling, Sodium, BadOptimizations, Sodium Extra, Fast IP Ping, No Telemetry | framework |
| Simple Opt Memory (`simpleoptmemory`) | – (Fabric Loader) | weicht FerriteCore; meldet ModernFix (O5 später) | framework |
| Simple Opt Server (`simpleoptserver`, Server) | Fabric API | weicht ServerCore | framework |
| Wiring Example (`wiringexample`) | – | – | – (nur Testmodul) |

## Features je Mod

### SimpleBuilding
- **Bauwerkzeuge:** Vorschlaghammer (3×3, Formen in Treppe/Stufe/Achtel), Meißel (Blöcke in der Welt umformen),
  Baustab (Flächen), Oktant (Messen/Planen), Blaupause (Bau-Code, Editor mit 3D-Vorschau, Scannen/Bauen; 26.3: Bild-Bauplan aus PNG, Umwandlung in SimpleLib `api.image`),
  Rotator (dreht Blöcke; zweite Funktion: Block in der Nebenhand tauscht den angeklickten Block), Farbpinsel (Goldpinsel) mit Farbkasten in 4 Stufen.
- **Kerne** (Kupfer bis Enderit) als Werkzeugstufen, mit Hand-Animationen; In-World-Umwandlungen per Schlag.
- **Vorschlaghammer-Zerlegungen** (schrittweise, ein Teil je Schlag): Diamantblock und Rissiger Diamantblock (9 Schläge, ab Eisenhammer) → 81 Diamantkiesel;
  Obsidian, Weinender Obsidian und Lohen-Obsidian (je 9 Schläge) → Obsidian-/Weinender-Obsidian-/Lohen-Obsidian-Splitter (nur so erhältlich; 9 Splitter → Block zurück); Eis, Packeis, Feuerkugel.
- **Helfer/Gadgets:** Attraktor (mehrere ziehen zum gemeinsamen Mittelpunkt), Detektor (Erz-Sonar),
  Resonanzstab (Laser, Scannen), Echolot, Geschwindigkeitsmesser.
- **Lager:** Rucksäcke (4 Stufen), Truhen- und Fallen-Truhen-Stufen (Kupfer→Enderit), Shulkerkisten-Stufen,
  Bündel-Stufen und Köcher, abgestellte Bündel, Astral-/Nihil-Gewölbe, Mod-Trichter mit Filter-Prinzip.
- **Fahrzeuge (26.3):** Trichterloren in den Stufen Verstärkt/Netherit/Enderit – Tempo wie der Trichter der Stufe,
  Filter-Prinzip (Antriebsloren: Simple Ovens; Güterloren und Truhenboote: Simple Storage, Stufe 8b-2).
- **Maschinen:** schnellere Öfen (3 Stufen je Ofenart; auf 26.3 aus der Pflicht-Sub-Mod Simple Ovens), Auto-Schmied, Autonomer Crafter, Befiederungs- und
  Schmiedetisch mit Rezeptbuch, stärkere Kolben, Astral-/Nihil-Kolben.
- **Schmelztiegel-Teil** (Tiegel, Seelen-Lava und Eimer selbst in Simple Crucibles, Stufe 5/5b): Vorschlaghammer-Wege
  (Tiegel bauen, Fass anbringen, Aufwerten), Quarz zerschlagen, Advancements „Heißer Topf“/„Seelenschöpfer“/„Bodenloser Eimer“.
- **End-Inhalte:** Astralit/Nihilit/Enderquarz-Paletten und Erze, Astral-/Nihil-Redstone als getrennte
  Redstone-Linien (Geräte nehmen jede Linie an), Brücken-Redstone, Redstone-Blöcke der Linien, Lampen, End-Kolben,
  Astral-/Nihil-Schienen, Enderit-Material (leeresicher); seltene Shulker seit Stufe 8b-3 in Simple Storage.
- **Kampf/Ausrüstung:** Pfeile aus Teilen (Spitze/Schaft/Feder), Rüstungsbesatz-Boni (Resonanz, Herz-Knopf mit
  8 Stufen im Inventar), Extra-Kopf-Slot im Inventar (Mob-Kopf/Kürbis; Effekte wie im Helm-Slot inkl. Attribute und Frostschutz, Anzieh-Sound, Modell vor einem Helm-Kopf, Größe wie am Mob, Helm darüber), Trank „Crafty Shulker“, Trainingspuppe (Skin per Namensschild), Rüstungsständer mit Armen/Tausch.
- **Deko/Platzieren:** abgelegte Vorlagen, Kleinteile-Häufchen (Barren/Ziegel/Diamant/Smaragd in 3D, Vorschlaghammer schaltet 3D/flach), Schachbrett-Blöcke und Schachfiguren (Schach-Achtel; Material-/Holz-/Melonen-Achtel seit Stufe 6 in Simple Octets),
  senkrechte Stäbe, Hängematte (jeder Winkel), Schallplatten je Dimension + Musik-Verstärker (spielen einen
  Plattenspieler bis 63 Blöcke mit, der Plattenspieler bleibt Vanilla; Rückport „Plattenspieler mit Truhe darüber“ = Playlist, N62, aus wo Vanilla es hat) / Noten-Verstärker (spielen Notenblöcke ebenso per Funk mit), Metall-Stangen (Eisen bis
  Enderit, Blitzableiter mit eigener Reichweite; früher „Rod“).
- **Lichtscheiben (26.3, N57):** flache Lampenscheiben (1 px, an jeder Blockseite, ohne Stütze fallen sie ab, Wasserlogging): normal, transparent (Schleier über dem Block dahinter), 16 Farben, Redstone-Variante (Farbe nach Signalstärke, Rechtsklick wie Notenblock stellt die Farbe für Stärke 1 ein); Rezepte aus Glasscheibe, Glowstone-Staub, Farbstoff, Redstone (nur Vanilla-Zutaten).
- **Verzauberungen:** Abbau-, Bau-, Lager-/Spieler-Verzauberungen, Luftsprung.
- **Welt/Wirtschaft:** Loot-Balance, bessere Strukturtruhen, Dorfbewohner-/Händlerangebote.
- **Pads und Tweaks** (aus Simple Tweaks übernommen): Spawn-/Elytra-/Trank-Pads in Stufen, Server-Optionen.
- **Integration:** JEI/REI-Kategorien, Jade, Modpack-/Claim-Schnittstellen, Guide-Bücher, Kreativtabs mit Schalter
  „in Vanilla-Tabs einsortieren“.
  Kreativtabs (N65): jedes Modul ab 3 Items hat einen eigenen Tab (neu: Simple Shops, Beacons, Trims, Maps, Mobs, Tweaks/Claims)
  und zusätzlich passende Plätze in den Vanilla-Tabs; Module mit 1–2 Items (Dimensions, Dragon Fight, Mob Manners, Models, QoL,
  Questionary, Respawn, Stats, Tools, Wanderers) stehen nur in Vanilla-Tabs. Test `CrossModGameTest#everyModuleItemIsInSomeCreativeTab`
  (alle Module geladen): jedes Item in mind. einem Tab, keine Dubletten, Modul-Tab enthält alle Items seines Moduls.

### SimpleLib (gemeinsame Bibliothek)
Warmes Essen (Komponente `simplelib:warm`, Tag `warmable_food`; die Tiegel selbst liegen seit Stufe 5 in `simplecrucibles`), Stapelgrößen-Regel
(`StackLimits`), UI-Bausteine des Simple-Stils (Kästen, Slots, Symbole, Flammen, Filter-Knopf). Dienste der
Modulaufteilung: `IdMoves` (ID-Umzüge), Datentyp `in_world_upgrades` mit Übergabe-Handlern und Tags
`#simplelib:hammers`/`#simplelib:upgrade_tools`, `OctetCells` (Achtelzellen je Material), Bildumwandlung `api.image` (Lab-Palette, Floyd-Steinberg/Bayer, Lauflängen-Zeilen; Bild-Bauplan), `ToolPlacements` (Werkzeug-Bauten an Zuhörer wie das Block-Protokoll). Gemeinsame Materialien
(Stufe 3d, `simplelib:*`, früher SB): Diamantkiesel, Rissiger Diamant (+Block, Amboss-Zerschlagen), Netheritklumpen,
Enderit-Grundmaterial (Fragment, Platten, Barren, Klumpen, Block), Basis-/Enderit-Aufwertungsvorlage, Polierter Endstein (Modulfamilie Stufe 3, gemeinsamer End-Baublock), `c:`-Tags;
Rezepte/Zerschlagen nur, wenn eine Mod sie anfordert (`requestMaterial`, Condition `simplelib:material_requested`; SB fordert alle an).

### Simple Interfaces (früher Simple Containers, Sub-Mod von QoL, clientseitige Optik)
Simple-Stil für Truhen, Fässer, Shulker, Trichter, Werfer/Spender, Crafter, Reittier-Inventare, Arbeitsblöcke
(Werkbank, Öfen, Braustand, Leuchtfeuer, Zaubertisch), Amboss/Schleifstein/Steinsäge/Webstuhl/Kartentisch/
Schmiedetisch, Handel, Spieler-Inventar; Mod-Bildschirme im gleichen Stil; Vergleichsschalter Simple/Vanilla;
Inventar-Helligkeit Hell/Dämmerung/Dunkel (Einstellung, wirkt über SimpleLib auf alle Mod-Kästen).
Tooltips (19.10.): Shulkerkisten-Inhalt als 9x3-Raster mit Umschalt (Färbung nach Kiste), Endertruhen-Item zeigt
die eigene Endertruhe (Snapshot Server→Client bei Login und Schließen, Server-Teil optional); Schalter lokal + Server.
Bündel (19.10.): Tooltip zeigt alle Einträge statt 12 (bis 6 Reihen, darüber Seiten mit "+n", folgen der Mausrad-Auswahl),
Mausrad wählt jeden Eintrag; nur Client, liest nur `bundle_contents` (gilt auch für SBs verstärkte Bündel); Schalter lokal.

### Simple Weather (Sub-Mod von QoL)
Wetter abschalten (Server), Regendichte (Client); eigene Config `config/simpleweather.json`, alte QoL-Werte werden
einmalig übernommen; in QoL mit „Simple Weather aktivieren“ abschaltbar.

### Simple Loot / Creatures / Farming / Tools / Movement (Sub-Mods von QoL, 2026-10-11)
Je eigene Config `config/<id>.json` (alte `qOL.*`-Werte werden beim ersten Start einmalig übernommen), eigener Befehl
`/<id>`, in QoL mit „Simple XY aktivieren“ abschaltbar, allein spielbar.
- **Simple Loot:** Tresor-Abklingzeit in Tagen.
- **Simple Mob Manners** (bis 13.10. Simple Creatures, `simplecreatures`; Config, Befehl und Schalter werden übernommen): Kreaturen stummschalten (Typ/Namenssuffix), dauerhafte Jungtiere, Piglins ignorieren Goldbesatz/-werkzeug. Seit 2026-10-15 (N62): **Eimer mit Frosch** (`simplemobmanners:frog_bucket`, einziges Item des Mods; Wassereimer auf Frosch, Variante als `frog_variant`-Komponente, Name und Alter bleiben; Reiter Werkzeuge nach dem Kaulquappen-Eimer; Textur `tools/textures/frog_bucket_2026_10_15.py`).
- **Simple Farming:** Federfall schützt Acker, Schärfe schneidet Pflanzen, Ernten mit Hacke.
- **Simple Tools:** Haltbarkeitsbonus, Sparsamkeit-Verzauberung (ID bleibt `simplequalityoflife:thrift`), Reparatur hält
  Ambosskosten, Ofen mit Lava füllen.
  Seit 2026-10-19: **Bessere Rahmen** (Rahmen und Leuchtrahmen, nur schleichend und mit Item im Rahmen; nur Vanilla-Felder `Invisible`/`Fixed`): Schere versteckt/zeigt den Rahmen (1 Haltbarkeit, geleerter versteckter Rahmen wird wieder sichtbar), Honigwabe sperrt (nicht drehen/entnehmen/abbauen), Axt entsperrt, sonst dreht Schleichen-Rechtsklick rückwärts; Schalter `enableItemFrameInvisibility`/`enableItemFrameLock`/`enableItemFrameReverseRotate`. Durchklicken durch unsichtbare Rahmen: später. Plan: `docs/ai/PLAN-B-ITEMFRAMES-2026-10-19.md`.
  Seit 2026-10-12: **Seelengebunden** (`simpletools:soulbound`, nur Buch: fahrender Händler 5 % für 24–32 Diamanten, End-City-Schatz 2 %; Amboss immer 35 Level, Schleifstein entfernt; bleibt beim Tod im selben Platz, Haltbarkeit danach min(aktuell, 50)) und **Todesgewölbe** (`simpletools:death_vault`, Block mit GUI; seit 2026-10-14 (N55) Kernfunktion: farbcodierte Anzeige, was aus den Drops des **letzten Todes** wurde – liegt noch (mit Despawn-Fortschrittsbalken, Pausen-Zeichen bei nicht geladenem Chunk) / von dir bzw. anderem Spieler (Name) aufgehoben / verbrannt / despawnt / ins Void gefallen / anders verschwunden, Legende in der GUI. Der Rückkauf gegen Opfer (4 Diamanten je Rückgabe, gewichtet nach Aktualität und Wert, mehrere Tode) ist **zukünftig**: Code bleibt, Option `deathVaultRedeem` Standard aus). Plan: `docs/ai/PLAN-SOULBOUND-DEATHVAULT-2026-10-12.md`.
- **Simple Movement:** Kriechen (`/crawl`), Autowalk, schnelles Leiterklettern/-rutschen, Frostläufer auf Pulverschnee;
  alte QoL-Tastenbelegungen werden übernommen. Seit 2026-10-15 (N59): Schatz-Verzauberungen **Stufenhöhe** (`simplemovement:step_height`, Stiefel, I–II: step_height +0,4/+0,9) und **Sprint** (`simplemovement:sprint`, Hosen, I–III: +10/20/30 % Grundtempo nur beim Sprinten); rein datengetrieben, Tags `treasure`/`tradeable`/`on_random_loot`, nicht am Verzauberungstisch. Seit 2026-10-15 (N62): **Sprungkraft steigt Stufen** – solange Sprungkraft aktiv ist, hebt ein transienter step_height-Modifier (`simplemovement:jump_boost_step`) die Schritthöhe auf mindestens 1,0 (Maximum mit Stufenhöhe-Stiefeln, nie Summe); Schalter `jumpBoostStepUp`, Standard an.
- **Simple Interfaces** übernimmt verknüpfte Behälter (Vormerken; HUD-Banner rechts, Ende mit Escape oder nach einmal Öffnen/Schließen; im Kreativinventar nie aktiv) und Shulkerkisten/Endertruhen aus dem Inventar
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
Geld in Truhen, verknüpfte Preislisten für andere Simple-Mods. Fahrender Händler: Preislisten-Links (Tag `wandering_trader/rare`)
zieht ein eigenes Trade-Set `simplemoney:wandering_trader/links` (1 Zusatzangebot nach den Vanilla-Sets, Mixin `WanderingTraderLinksMixin`).

### Simple Models
Server-freigegebene eigene Item-Modelle, Zuweisung am Amboss, durchsuchbarer Modell-Browser, Admin-Reload.

### Simple Fun
Schleichwurf (Yeet), Wurfziegel und Ziegelschneeball, Schweinverwandlung, „Schadlos“-Verzauberung, Rückstoß V,
kleine kosmetische Freuden. Seit 1.4.0 ohne Köpfe (Tier- und Spielerköpfe → Simple Mobheads).

### Simple Mobheads (Pflicht-Sub-Mod von SimpleBuilding, allein spielbar)
94 Köpfe (93 Mob-Köpfe + geheimer Herobrine-Kopf) aus `modules/simplemobheads/data/mobs.json` (Generator `tools/generate_heads.py`): jeder 26.3-Mob ohne
Vanilla-Kopf, die Mobs der Simple-Mods, die 4 Tierköpfe aus Simple Fun und die 12 Köpfe aus SimpleBuilding
(`simplefun:*`/`simplebuilding:*` → `simplemobheads:*` über IdMoves). Drop nur durch geladenen Creeper (Variante des
Mobs als Komponente, Babys normaler Kopf, Wither aus/Warden an, Spieler-Kill-Chance optional aus), PvP-Spielerkopf,
Tarnung gegen den eigenen Mob, Notenblock-Stimme, Gruß beim Schleichen, 29 Fähigkeiten mit Schaltern, eigener Tab +
Suchtab hinter dem Drachenkopf, `/simplemobheads` (Alias `/simplefun pvp headDrops`). Plan
`docs/ai/PLAN-SIMPLE-MOBHEADS-2026-10-13.md`. Easter Egg (KONZ20): Herobrine-Ritual (Goldschrein + Moos + 4 Redstone-Fackeln +
Netherrack, nachts anzünden) → Herobrine-Kopf (eigene Pixelkunst, leuchtende Augen; Fackeln flackern, Mobs zögern im Dunkeln),
5 min Spuk nur für den Auslöser, Hinweisbuch in Minenschacht-Truhen; Plan `docs/ai/PLAN-B-HEROBRINE-2026-10-20.md`.

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
Phase 1). Claim-Urkunde (Phase 2): Rezept Papier + Tintenbeutel + Honigwabe, Rechtsklick beansprucht und bindet sie an den Besitzer (`simpletweaks:deed`, nur Anzeige), gebunden öffnet sie den Rechte-Editor (Code-Feld, Fehlerzeilen vom Server, Online-Namen, Hilfe; simplelib-Kasten, gebündelt), Kopie wie ein Buch (Kopie nicht kopierbar). Claim-Tisch, Erweiterung und Maps-Ebene folgen (Phasen 3–5).
Alle Befehle gibt es auch als `/simpletweaks <befehl>` (z. B. `/simpletweaks invsee <Spieler>`, `/simpletweaks pvp on`, `/simpletweaks claim create`; `/simpletweaks help` listet die erlaubten); `/tweaks` und die Kurzbefehle bleiben als Alias.

Admin/Schutz (17.10., Plan `docs/ai/PLAN-TWEAKS-ADMIN-2026-10-17.md`, Paket `admin`, Config `config/simpletweaks-admin.json`, je Feature eigener Schalter + OP-Stufe): Block-/Truhen-Protokoll mit `/inspect`, `/tweaks lookup` (Filter Spieler/Aktion/Item/Block/Dimension/Zeit/Radius), `/tweaks rollback` (`--preview`), `/tweaks restore`, `/tweaks purge <Spieler>`; erfasst auch Schilder, Explosionen, Feuer, Trichter optional; Rechte je Befehl (an, 19.10.), Inventar-Sicherung beim Tod mit Wiederherstellen (an), `/invsee`/`/endersee` als eigene UI, auch offline (an), Vanish (an), Freeze (an), Neustart-Ankündigung mit Countdown (an), Auto-Backups + `/backup now` (an), Wartungsmodus (Schalter an, Modus aus bis `/maintenance on`), Lag-Finder `/lag` (an, misst nur während des Befehls).
Spieler/Welt/Events (Teil 2, 17.10., `docs/ai/PLAN-TWEAKS-PLAY-2026-10-17.md`, Config `config/simpletweaks-play.json`): an – /coords, privater Todes-Hinweis, @Erwähnungen, /playtime + /seen, PvP je Spieler (/pvp, beide an, Claims haben Vorrang), Abstimmungen (/vote; Tag/Nacht aus), AFK (Tab-Liste grau, zählt nicht fürs Schlafen; Kick aus), Weltgrenze je Dimension (Config-Ebene über die Vanilla-Grenze je Dimension), Teil-Keep-Inventory (hotbar_armor/xp/custom), Drachenei je Kampf, /tweaks event (Doppel-XP, Mehr Beute, Blutmond; Zeitplan aus), /leaderboard (Seitenleiste aus), /phantoms, /pregen; aus – Mob-Griefing je Mob, Willkommen/Tipps, Login-Belohnung (Geldscheine mit Simple Money), Nether-Dach, Mob-Grenze je Chunk, Bodengegenstände aufräumen. Optional: Simple Money (Scheine), Simple Weather (Wetter-Abstimmung aus, wenn Wetter abgeschaltet).
Schutz (17.10., `docs/ai/PLAN-TWEAKS-PROTECT-2026-10-17.md`, Paket `protect`, Config `config/simpletweaks-protect.json`): an – Anti-X-Ray (verdeckte Erze im Chunk-Paket als Wirtsgestein, Aufdecken bei Nachbaränderung, Listen/Höhe je Dimension, Kreativ/Zuschauer ausgenommen, Modus 2 Fake-Erze aus), `/simpletweaks protect status|reload|bench|checks` (auch `/tweaks protect`); aus – Freecam-Regel (Kollision erzwingen über Client-Mixin bei geladenem Freecam, optional Kick ohne Simple Tweaks), Seed-Hash zufällig, Gesundheit anderer Spieler verbergen, Reichweiten-/Schnellabbau-Protokoll (Grenzen aus echten Server-Attributen, Hammer/Baustab/Blaupause nie gemeldet). Optional: Freecam (Client).

### Simple Optimization (Super-Mod + 3 Sub-Mods, `claude-opt1`, `b-opt45`, nur Fabric 26.3)
Eigene Umsetzungen veröffentlichter Optimierungen, jeder Bereich einzeln schaltbar (`auto|ours|theirs|off` in
`config/simpleopt*.json`); ist ein bekannter Mod für denselben Bereich da, gewinnt er (Mod-Erkennung im Mixin-Plugin,
framework `OptCompat`, Prioritätstabelle `data/<submod>/priorities.json`). Render: O1 Blätter-Culling (Tiefe 0–4),
O7 Lichtkarten-Cache, M1 schneller IP-Ping, M2 Telemetrie aus, O3 weiteres Culling (Schild-Rückseite, Rahmen hinter Wand,
Leuchtfeuer-Strahl außer Sicht), O9 Detail-Schalter (Partikel, Textur-Animationen, Luft-Nebel). Server: S5a Sichtweite nach
Serverlast (Simulationsweite nur per Config), S5b Aktivierungsbereich ferner Mobs (Standard aus), `/simpleopt status`.
Memory: O4 geteilte Blockzustands-Tabellen. Zielprüfung (§4.5): fremder Mod auf derselben Zielklasse → Bereich aus
(framework `OptAreas`). Benchmark `tools/bench/run_bench.py` (sb-test).
Konzept `docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md`, Pläne `docs/ai/PLAN-OPT1-2026-10-18.md`, `docs/ai/PLAN-B-OPT45-2026-10-19.md`.

### Simple Stats (neu, `oc-simplestats`, allein spielbar)
Jedes Werkzeug, jede Waffe, Rüstung, Schild, Elytra, Angel, Bürste und das Feuerzeug zählt eigene Statistik in der Komponente
`simplestats:item_stats` (Versionsfeld, Byte-Budget statt Top-N, Rest als „weitere“): Abbau je Block, Kills je Mob und Variante,
Schaden/Rekorde, absorbierter Schaden (Näherung nach Rüstungspunkten), Schild, Stiefel-Strecken, Elytra, Angel, Schere, Bürste, Tragezeit.
Zuordnung über Tags/Komponenten, nicht Klassen (Fremd-Gear mit Vanilla-Verhalten zählt mit). Anzeige: Tooltip (Hauptwert, Shift: wichtigste Werte)
und eigener Statistik-Bildschirm je Gegenstand (Taste I, über dem Gegenstand im Inventar oder in der Hand). Kreativ/Maschinen zählen nicht,
Zurücksetzen nur per `/simplestats reset`. Konzept `docs/ai/KONZEPT-ITEM-STATISTIKEN-2026-10-15.md` (Abschnitt 14 verbindlich).

## Geplant / in Arbeit
| Mod | Stand | Inhalt | Quelle |
|---|---|---|---|
| Simple Maps (`simplemaps`, Sub-Mod SB) | in Arbeit (`claude-q-maps`) | Wegfinder-Karte (unendlich, Spieler mittig), GUI mit Lesezeichen, Wegpunkte, Locator-Bar, Karte je Dimension, Fundorte; Webkarten-Export Stufe 1 (`b-webmap`: statische PNG-Kacheln + markers.json + Ansicht, Standard aus, Spieler-Opt-in, autoOffWith bluemap/dynmap/squaremap/pl3xmap/journeymap) | `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md`, `docs/SIMPLEMAPS-WEBKARTE.md` |
| Simple Mobs (`simplemobs`, Sub-Mod SB, optional) | Stufe 1 (`claude-q-deceiver`) | Mob Deceiver (Endgame, Wellen aus Fake-/Echt-Mobs, Mirage-Puls über SB-Effekte, falls SB geladen), Täuscherstoff, Spawn-Ei; Furcht-Mobs Schreckensritter (Eisengolem-T mit Amboss in der Mitte, Skelettschädel, Eisenhelm aufsetzen) und Hohler Wächter (End-Außeninseln, Blickmechanik) mit Zitter-Aura (SB-Effekt `shivering`, ohne SB eigener), Ruhe-Amulett, Trank des Fokus (`claude-fear-mobs`, Plan `docs/ai/PLAN-FURCHT-MOBS-2026-10-12.md`); Verdorrter Wither (Wither-Figur auf Netheritblock, 600 Leben, 3 Phasen mit angekündigten Angriffen) mit Witherherz und Biom-Pinsel (Material in der Nebenhand malt Biomzellen, Claim-sicher; `claude-wither`, Konzept `docs/ai/KONZEPT-WITHER-BIOMPINSEL-2026-10-10.md`); Simple-Mobs-Handbuch; spielbar auch ohne SB | `docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` |
| Simple End (`simpleend`, Sub-Mod SB, optional; Super-Mod von End Dragon Fight) | `claude-simple-end` | End-Holz Aschholz/Dämmerholz (volles Vanilla-Holzset inkl. Boote, Schilder, Regal, Laub; mit SB zusätzlich Holzwerk, Holz-/Stamm-Achtel, gestufte Kistenboote, mit Sandwiches Schneidebretter – `claude-endwood-full`, `docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md`), Knospen (Licht ≥ 9 großer Baum, 2×2 Riesenbaum, dunkel + Knochenmehl Stöcke-Gestrüpp), End-Stöcke-Block, Schwebfrucht (4 s Sanfter Fall + Sprung), End-Gras/-Busch (N42), Biome Aschental/Dämmerlande auf äußeren End-Inseln, Items leere-sicher + doppelte Liegezeit, Knabbermilbe (passiv, knabbert Endstein → durchwühlter Endstein, erholt sich); spielbar ohne SB | `docs/ai/PLAN-SIMPLE-END-2026-10-12.md` |
| Simple End Dragon Fight (`simpleenddragonfight`, Sub-Mod von Simple End, optional) | Stufe 1 (`claude-dragon`) | Drache: +1 volle Lebensleiste je Spieler, +15 % je weiterer Beschwörung (300 bzw. 200 je Spieler, Schonfrist 60 s), +20 % Tempo, Eintritts-Animation je Spieler (Warteschlange), ab halbem Leben Dragonmites alle 30 s (Drop Drachen-Chitin); spielbar auch ohne SB | `docs/ai/KONZEPT-END-DRAGON-FIGHT-2026-10-11.md` |
| Simple Respawn (`simplerespawn`, Sub-Mod SB, optional) | Stufe 1 (`claude-respawn`) | Niedergeschlagen statt tot (nur mit Mitspielern online, Config: immer), Ausbluten 60 s, eigener Bildschirm (Zuschauen mit freier Kamera, Inventar nur ansehen, Aufgeben), Wiederbeleben durch 5 s Rechtsklick-Halten (danach 3 Herzen, 0 Hunger), Monster ignorieren Niedergeschlagene, Logout/Absturz = Vanilla-Tod (kein Dupe), Handbuch, Fortschritt Erste Hilfe; spielbar auch ohne SB | `docs/ai/PLAN-SIMPLE-RESPAWN-2026-10-12.md` |
| Simple Beacons (`simplebeacons`, Sub-Mod SB, optional; braucht `simplelib`) | Stufe 1 (`claude-beacons`) | Starke Aufwertungsvorlage (Netherstern, Duplikation), Starkes Leuchtfeuer (eigener Block, 6 Ebenen, Reichweite 60/70, 6 Zusatzeffekte E5/E6, Leuchtfeuer-Bildschirm im Vanilla-Stil mit 2 Zusatzzeilen (Hintergrund nur zur Build-Zeit aus Vanilla abgeleitet), Basis-Tag `strong_base_blocks`), Starker Aquisator (7×7×7-Rahmen, bis 144 Blöcke nur unter Wasser, Delfingunst, Angriff 12), Aufwertung am Schmiedetisch oder in der Welt (Axt/Hammer, 8 Schläge, Daten bleiben), Server-Konfig mit Deckeln; spielbar ohne SB | `docs/ai/PLAN-SIMPLE-BEACONS-2026-10-13.md` |
| Simple Music (`simplemusic`, eigenes Modul ohne Eltern-Mod; braucht `simplelib`, SB/Mobheads optional) | Stufen A1–A3, B1–B3 (`b-music20`) | Notenblatt mit Text-Notencode (Editor mit SimpleLib-Codefeld `UiCodeArea`, Probehören, Import .txt/.nbs aus `config/simplemusic/sheets/`, Signieren), Orchestrion (Server-Sequenzer, Redstone R1, Schleife ohne Auswurf, Playlist-Behälter darüber, Komparator), alle Klänge als Instrument mit Sperrliste, Rohling + eigene Platte (Upload mono Ogg aus `config/simplemusic/upload/`, Server-Prüfung ohne Decoder, 4 MiB/5 min/5 je Spieler, OP-Freigabe, `/music`-Befehle, Download + Client-Cache, Wiedergabe im Orchestrion und in der Vanilla-Jukebox per Mixin hinter Schalter), Taschen-Jukebox (`b-pocketjuke`: 17 Farben, Rezept 8 Kupfernuggets + Bündel + Diamant, 20 Platten wie im Bündel, Abspielen/Pause nur für den Träger, Schleife); Rasterzeichnung (E2/E3) offen | `docs/ai/PLAN-B-MUSIC-2026-10-20.md`, `docs/ai/PLAN-B-POCKETJUKE-2026-10-21.md` |
| Simple Blocks (`simpleblocks`, Sub-Mod SB, **Pflicht** seit b-blocksmove: SB `requires`+bündelt, Schalter „Simple Blocks aktivieren“; braucht `simplelib`) | N69/N71 + Formsäge (`claude-formsaege`) + Baublock-Umzug (`b-blocksmove`) | **Aus SimpleBuilding umgezogen (15, IdMoves):** schwebender/aufsteigender Sand und Kies (+ Entity `levitating_block`; Rezept mit SB: Nihilitsplitter/Astralitstaub, ohne SB: Phantomhaut/Windrute), Erd-/Gras-/Sand-/Kiesstufe, rissiges Eis, gemeißeltes Pack-/Blaueis, Nautilusschalenblock, drei Froschlicht-Farben; eigener Tab mit SBs Baublockzeilen dahinter (T1/T2). Gedämpfte Tiefenschieferfliesen, Sculk-Tiefenschiefer, fehlende Vanilla-Treppen/-Stufen/-Mauern, rissige/bemooste Ziegel, Blaue Netherziegel; **Formsäge** (`simpleblocks:shaping_saw`, Prinzip A4b; Vanilla-Steinsäge mit Kupfer, Rezept CIC/###, Aufwertung in der Welt: Steinsäge + Axt bzw. mit Simple Hammers Hammer + 4 Eisennuggets): schneidet alles der Vanilla-Steinsäge plus Holz (13 Holzarten inkl. Pappel/Bambus: Treppe/Stufe/Zaun/Stöcke, Stamm→Bretter/entrindet/Holz), Wolle, Netherziegelzaun; Rezepttyp `simpleblocks:sawing` (201 Rezepte, Simple End/SimpleBuilding nur mit Ladebedingung), Steinsägen-Rezepte anderer Module (z. B. Octets) erscheinen von selbst; Bildschirm 6×4 mit Scrollen und Suche, JEI-Kategorie, Entdeckungs-Tipp; allein spielbar | `docs/ai/PLAN-FORMSAEGE-2026-10-17.md`, `docs/ai/PLAN-B-BLOCKSMOVE-2026-10-18.md` |
| Simple Ovens (`simpleovens`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt; braucht `simplelib`) | Modulaufteilung Stufe 4 (`claude-split-s4-ovens`) | Verstärkte/Netherit-/Enderit-Öfen, -Räucheröfen, -Schmelzöfen (2×/4×/8×, doppelte Erfahrung ab Netherit, Tempo aus eigener Server-Konfig, übernimmt SBs Werte beim ersten Start), Antriebsloren der drei Stufen, Menü-Mixin für lange Garzeiten, Aufwertung in der Welt über SimpleLibs `in_world_upgrades` (allein Axt, mit Simple Hammers Vorschlaghammer), eigener Tab, Handbuch, Fragentisch, Preise; alte `simplebuilding:`-IDs (Blöcke, Items, Block-Entities, Loren, Rezepte, Rezept-Advancements) migrieren über SimpleLib-IdMoves; auf 26.3 nicht mehr in SB (26.2 unverändert); allein spielbar (Enderit-Beute über SimpleLib) | `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md` |
| Simple Crucibles (`simplecrucibles`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt, Sandwiches bündelt es ebenfalls; braucht `simplelib`) | Modulaufteilung Stufe 5 (`claude-split-s5-crucibles`) | Eisen-/Verstärkter/Netherit-/Enderit-Schmelztiegel (6/12/18/30 Plätze, beheizt vom Block darunter, alle Plätze zugleich, Tempo 1×/2×/4×/8×), Kupfer-/Verstärkte/Netherit-/Enderit-Fässer zum Andocken, Schmelztiegel-Rohling beim Bau (Axt: 16 Eisenbarren auf einen Kessel), verstärkter Kessel, Dorf-Feldküchen, eigene Server-Konfig `simplecrucibles-server.json` (übernimmt die Tiegel-Optionen aus `simplelib-server.json` beim ersten Start), Aufwertung in der Welt (allein Axt, mit Simple Hammers Vorschlaghammer), Handbuch, Testzentrale-Station, Preise; alte `simplelib:`-/`simplebuilding:`-IDs (Blöcke, Items, Block-Entities, Rezept) wandern über IdMoves; **Stufe 5b** (`claude-s5b-crucibles`): Seelen-Lava (Fluid, Block, Seelenbrand, Weltgenerierung, Nebel, Gruppe `soulLava` der eigenen Config), alle 26 Eimer (Kupfer mit Oxidation/Wachs, Keramik mit 4 Stufen, Enderit halb/voll inkl. Milch und Seelen-Lava = 2 Füllungen, Seelen-Lava-Eimer), eigenes JEI-Plugin (Tiegel-Kategorie, Infoseiten) und Jade-Plugin (Tiegel, verstärkter Kessel); IDs inkl. Fluid/Effekt/Komponenten/Rezepte wandern über IdMoves | `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md`, `docs/ai/PLAN-S5B-2026-10-16.md` |
| Simple Octets (`simpleoctets`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt, Schalter „Simple Octets aktivieren“; braucht `simplelib`) | Modulaufteilung Stufe 6 (`claude-s6-octets`) | Achtel (0,5³) aus 15 Hölzern, 159 Materialien (jeder Block mit Treppe+Stufe, Baufarben, Stämme) und der Melone (Schleichen + Rechtsklick mit Melonenscheibe), gemischte Zelle (Block-Entity, Renderer), 8 aus Vollblock nur in der Formsäge (`simpleblocks:sawing`, mit Simple Blocks; ohne Simple Blocks Steinmetz-Ersatz, Prinzip A4b) + formlos zurück, Zellen-API (`OctetCellBlock`/`OctetCells`, SB-Schach-Achtel sind Zellen davon), SimpleLib `OctetCells` für Schnitzwerkzeuge, Paletten-Achtel von SB/Simple End/Simple Blocks nur mit dem Mod (Rezepte mit Lade-Bedingung), eigener Tab, JEI-Infoseiten, Handbuch, Testzentrale-Station; alte `simplebuilding:`-IDs (Blöcke, Items, BE, 340 Rezepte + Freischaltungen) wandern über IdMoves | `docs/ai/PLAN-S6-OCTETS-2026-10-17.md` |
| Simple Hammers (`simplehammers`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt, Schalter „Simple Hammers aktivieren“; braucht `simplelib`) | Modulaufteilung Stufe 7 (`claude-s7-hammers`) | 7 Vorschlaghämmer (Stein…Enderit; Enderit bleibt bis Simple Enderite herstellbar, F3-TODO), 3×3-Flächenabbau mit Stückzeit-Formel, Abtragen/Umformen (Achtel mit Simple Octets, Rückwärts mit SBs Berührung des Konstrukteurs), Diamant-/Quarzblock zerschlagen, In-World-Aufwertung (Aufträge, Schläge, Fortschritt) auf SimpleLibs `in_world_upgrades`, schaltet die Axt-Wege aller Simple-Mods ab, Hammer bleibt im Werkbankraster, Verzauberungen Durchbruch/Übersteuerung/Radius, SimpleLib-Vertrag `HammerActions` (andere Mods hängen ihre Hammer-Wege ein, ohne Import), eigene Server-Config `simplehammers-server.json` (übernimmt SBs `server.tools`-Werte), Tab, JEI-Infoseiten, Handbuch, Testzentrale-Station, Preise; alte `simplebuilding:`-IDs (7 Items, 3 Verzauberungen, 10 Rezepte + Freischaltungen) wandern über IdMoves; auf 26.3 nicht mehr in SB (26.2 unverändert) | `docs/ai/PLAN-S7-HAMMERS-2026-10-18.md` |
| Simple Storage (`simplestorage`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt, Schalter „Simple Storage aktivieren“; braucht `simplelib`) | Modulaufteilung Stufe 8a (`b-s8-storage`) | Kisten aller 12 Vanilla-Hölzer (+ Aschholz/Dämmerholz mit Simple End): offene Holzkästen für 9 Stapel Essen in höchstens 9 Sorten, Lagen sichtbar (Renderer), Trichter/Komparator, wie ein Fass drehbar, brennen/Brennstoff wie Bretter; Werkbank mit Lager (behält ihr 3x3-Raster, Raster liegt sichtbar auf der Platte, geteilt zwischen Spielern, Fenster im Container-Stil über SimpleLib-UI-Bausteine); eigener Tab, Handbuch, Fragentisch, Teststation; alte `simplebuilding:`-IDs (Blöcke, Items, Block-Entities, Rezepte, Freischaltungen) wandern über SimpleLib-IdMoves; beide gab es auf 26.2 nie. **8b** (`b-s8b-storage`): Verstärkte/Netherit-/Enderit-Truhe (36/45/54 Plätze, Stapel x1/x2/x4, Doppeltruhen nur gleicher Stufe, Menüs ohne Blättern), ihre Fallen-Zwillinge, Fallen-Kupfertruhen (4 Stufen, gewachst), Stufen-Shulkerkisten (färbbar, im Kessel waschbar, Inhalt samt übergroßer Stapel am Item, `simplestorage:container_counts`); Aufwertung in der Welt Kupfertruhe/Vanilla-Shulker → Verstärkt → Netherit → Enderit mit Axt (allein) oder Hammer (Simple Hammers), Inhalt bleibt; Trichter/Komparator/NeoForge-Pipes mit Stufen-Grenze. 26.2 behält SBs eigene Klassen (`McVersion.STORAGE_IN_SB`). **8b-2** (`b-s8c-storage`): Verstärkte/Netherit-/Enderit-Güterlore und -Truhenboot (jede Holzart, Bambus als Floß, mit Simple End auch Aschholz/Dämmerholz; Holzart als `simplestorage:boat_wood`), Plätze/Stapel der Stufen-Truhe, Schmieden Verstärkt → Netherit → Enderit (SimpleLib-Vorlage), alte `simplebuilding:`-Fahrzeuge/-Items wandern per IdMoves (nur 26.3). **8c-1** (`b-s8c1-storage`): Verstärktes/Netherit-/Enderit-Bündel und Köcher (4 Stufen, Brustplatz/Zubehörplatz, Bogen/Armbrust nehmen Pfeile daraus, Köcher im getragenen SB-Rucksack mit Meisterbauer über SimpleLib `CarriedStacks`), abgestelltes Bündel (auch Vanillas Bündel), Lager-Verzauberungen Schublade/Tiefe Taschen/Trichter (`simplestorage:*`, Bücher-Textur nur mit SB), Färben wie Vanilla (`simplestorage:dyeable_storage`), Rezepte allein mit Leder (mit SB Lederplatte), Option `invertBundleInteractions` in `config/simplestorage.json` (aus SB übernommen); Zubehörplätze über SimpleLib `AccessorySlots` (Curios/Trinkets-Haken bis 8c-2 von SB); alte `simplebuilding:`-Items/Block/BE/Verzauberungen/Rezepte wandern per IdMoves; 26.2 behält SBs Klassen. **8c-2** (`b-s8c2-storage`): die vier Rucksäcke (9/18/33/50 Plätze, Brustplatz/Zubehörplatz, abstellbar, Tiefe Taschen/Trichter, Rucksack-Taste B `key.simplestorage.open_backpack` – alte Belegung wird übernommen –, Menü wie das Inventar samt Rezeptbuch, Rücken-Ebene, Färben), Curios-/Trinkets-Haken jetzt im Modul; mit SB Meisterbauer/Konstrukteurs Hand am getragenen Rucksack über SimpleLib `CarriedStacks` (Eintrag nennt seinen Behälter), Zusatz-Kopfplatz + Resonanzknopf im Rucksack-Bildschirm über SimpleLib `InventoryExtras`; Rezepte allein mit Leder; alte `simplebuilding:`-Items/Blöcke/BE/Komponente/Rezepte wandern per IdMoves; 26.2 behält SBs Klassen. **8b-3** (`b-s8b3-storage`): seltene Strukturfunde – bessere Loot-Truhen (Festung Verstärkt, Bastion/Netherfestung Netherit, End-Stadt/-Schiff Enderit, doppelte Beute), seltene End-Stadt-Shulker (verstärkt/Netherit/Enderit mit 1,5-/2-/3-fachem Leben, Hülle der Stufen-Shulkerkiste, einmalige Endermiten-Begleitung, lebend mit Klumpen aufwertbar), Stufen-Shulkerschalen (0–2 je Tod, Rezept Kupfertruhe + Stufen-Schale + Shulkerschale → Stufen-Shulkerkiste), Chancen in `config/simplestorage.json` (aus SB `server.loot.*` übernommen); alte `simplebuilding:`-Schalen/-Rezepte per IdMoves, alte Shulker behalten Stufe (alte Modifikator-/Tag-Namen werden mitgelesen); nur 26.3. Bleibt SB: Aufwerten der abgelegten Schale auf SBs Häufchen (per Id), Shulker-Deckel der Vanilla-Kisten | `docs/ai/PLAN-B-S8-STORAGE-2026-10-19.md`, `docs/ai/PLAN-B-S8B-STORAGE-2026-10-19.md`, `docs/ai/PLAN-B-S8C-STORAGE-2026-10-20.md`, `docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md`, `docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md`, `docs/ai/PLAN-B-S8B3-STORAGE-2026-10-21.md` |
| Simple Shops (`simpleshops`, Sub-Mod SB, optional; braucht `simplelib`; Claims/Simple Money nur optional) | Stufen 1–3 (`oc-shops`) | Spieler-Shops: Verkaufsblock (ein Preis aus bis zu 5 exakten Preis-Items nach Filter-Prinzip, 9 Einnahme-Plätze) unter der Vitrine (5 Warenplätze, Glow-Variante hell + Licht 6), Käufer-GUI mit Tausch (1 Stück je Tausch, atomar, Überlauf wird gedroppt, ein Spieler zugleich, Leerlauf-Timeout), Schutz (nur Besitzer/Op ab Stufe 2 bauen ab, Explosions-/Wither-/Drachen-/Kolben-/Trichterschutz, Limit 16 je Spieler, Claims: Setzen braucht B, Abbau durch Besitzer immer, Kauf ohne Recht), Renderer, Handbuch, Wiki; **Server-Shop** (`/servershop create|remove|list|info|tp|edit|price|goods|cash`, nur Op Stufe 2, IDs s001…, unbegrenzter Vorrat per Kopie, Einnahmen = Senke oder Admin-Kasse per Config, kein Abbau durch Spieler, mehrere Käufer zugleich); **Ender-Shops** (KONZ20, b-shops20: Inhalt in Weltdaten `simpleshops:ender_shops`, Rezepte mit Enderperle, Migration alter Shops, `/servershop orphans [clear]`), **Handelskontor** `trade_terminal` (Tab Meine Shops: Einnahmen abholen, Preise, öffentlich/privat, Ware nur entnehmen mit Lieferzeit; Tab Shops suchen: Suche/Filter/Sortierung, Fernkauf nur gleiche Dimension; nie Chunk-Laden); spielbar ohne SB | `docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md`, `docs/ai/KONZEPT-SHOPS-UMBAU-2026-10-19.md` |
| Simple Wanderers (`simplewanderers`, Sub-Mod SB, optional; keine harte Abhängigkeit, allein mit Vanilla spielbar; SB/Simple Money/Simple Mobheads nur optional) | Stufen 1–3 (`oc-wanderers`) | Dorf-Wanderer: ein umtexturierter Fahrender Händler je Dorf (Spawn beim ersten Besuch an der Dorfglocke, genau einer: Eintrag je Dorf + Zählung + Doppelprüfung), nie unsichtbar, nie despawnend, tötbar (Respawn nach 7 Tagen, nur bei Spieler an der Glocke), Pferd an der Leine (Schleichen + Rechtsklick leer: Pferd einfangen), alle 3 Tage neues Angebot (Seed aus Dorf, Periode, Weltsaat), alle 1,5 Tage Nachschub, Aufteilung der Waren zwischen Dorf-Wanderer und Fahrendem Händler (Vanilla-common 47/31, buying geteilt, Köpfe nur beim Wanderer, Kerne und Seltenes nur beim Fahrenden Händler), Biom-Sortiment je Dorftyp (7 Pools), Spawn-Ei, Handbuch, Config `simplewanderers-server.json`, SB-Schalter „Simple Wanderers aktivieren“ | `modules/simplewanderers/wiki/manual.json`, `docs/ai/KONZEPT-DORF-WANDERER-2026-10-15.md` |
| Simple Trims (`simpletrims`, Sub-Mod SB, optional) | Stufen 2–4 (`claude-trims2`) | Vorlagen Leuchtend/Strahlend/Pulsierend (`simpletrims:*`, alte `simplebuilding:`-IDs migrieren über SimpleLib-IdMoves), Aufwertungen am Schmiedetisch (eigener Rezepttyp, auch Auto-Smither), Strahlkraft-Licht, Leucht-/Puls-Darstellung, Besatzvorlagen ablegen und in der Welt schmieden, Werkzeugregel wie Tiegel (Axt allein, Hammer sobald ein Partner die Axt-Wege abschaltet), Fortschritte, Handbuch, Preise; bündelt SimpleLib; auf 26.3 nicht mehr in SB (26.2 unverändert); Besatz-Boni bleiben in SB | `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md` |
| QoL-Aufteilung | Besitzer entscheidet | Sub-Mods für Container (fertig), Bewegung, Landwirtschaft, Werkzeuge/Haltbarkeit, Kreaturen, Wetter, Tresor (→ Simple Loot); Schärfe/Gras → Simple Combat; Kreaturen-Logik nach simplelib | QUEUE N14 |
| Mobs/Effekte | Konzepte | End-Mob, Shellker (Schwerkraft), Deceiver, Furcht-Mob, Seelenfeuer-Lohe; Effekte Zittern/Trugbild/Verblasst | `docs/ai/KONZEPT-MOBS-2026-10-07.md`, `KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` |

Offene Punkte und Reihenfolge: `docs/ai/ROADMAP-2026-10-09.md`.
