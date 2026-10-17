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
 ├─ Simple Mobheads (Pflicht, gebündelt)      │
 ├─ Simple Trims (gebündelt)                   ├─ simplemobmanners, simplefarming, simpletools,
 └─ Simple Maps (in Arbeit)                    └─ simplemovement (alle Sub-Mods, gebündelt)
                                                 (docs/ai/PLAN-QOL-AUFTEILUNG-2026-10-10.md)
Eigenständig: Money, Riding, Models, Fun, Visuals, Sounds, Dimensions, Sandwiches, Tweaks (braucht SB)
```

## Abhängigkeiten (heute)

| Mod (ID) | Pflicht | Optional (Integration) | Bündelt |
|---|---|---|---|
| SimpleBuilding (`simplebuilding`) | Cloth Config, Simple Ovens, Simple Crucibles (26.3) | JEI, REI, ModMenu, Jade, AppleSkin, Mouse Tweaks, Trinkets, Curios | simplelib, simpleovens, simplecrucibles (+ optionale Sub-Mods) |
| Simple Ovens (`simpleovens`) | simplelib | SimpleBuilding (Vorschlaghammer statt Axt) | simplelib |
| Simple Crucibles (`simplecrucibles`) | simplelib | SimpleBuilding (Vorschlaghammer statt Axt, Advancements), JEI, Jade | simplelib |
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
| Wiring Example (`wiringexample`) | – | – | – (nur Testmodul) |

## Features je Mod

### SimpleBuilding
- **Bauwerkzeuge:** Vorschlaghammer (3×3, Formen in Treppe/Stufe/Achtel), Meißel (Blöcke in der Welt umformen),
  Baustab (Flächen), Oktant (Messen/Planen), Blaupause (Bau-Code, Editor mit 3D-Vorschau, Scannen/Bauen),
  Rotator (dreht Blöcke; zweite Funktion: Block in der Nebenhand tauscht den angeklickten Block), Farbpinsel (Goldpinsel) mit Farbkasten in 4 Stufen.
- **Kerne** (Kupfer bis Enderit) als Werkzeugstufen, mit Hand-Animationen; In-World-Umwandlungen per Schlag.
- **Vorschlaghammer-Zerlegungen** (schrittweise, ein Teil je Schlag): Diamantblock und Rissiger Diamantblock (9 Schläge, ab Eisenhammer) → 81 Diamantkiesel;
  Obsidian, Weinender Obsidian und Lohen-Obsidian (je 9 Schläge) → Obsidian-/Weinender-Obsidian-/Lohen-Obsidian-Splitter (nur so erhältlich; 9 Splitter → Block zurück); Eis, Packeis, Feuerkugel.
- **Helfer/Gadgets:** Attraktor (mehrere ziehen zum gemeinsamen Mittelpunkt), Detektor (Erz-Sonar),
  Resonanzstab (Laser, Scannen), Echolot, Geschwindigkeitsmesser.
- **Lager:** Rucksäcke (4 Stufen), Truhen- und Fallen-Truhen-Stufen (Kupfer→Enderit), Shulkerkisten-Stufen,
  Bündel-Stufen und Köcher, abgestellte Bündel, Astral-/Nihil-Gewölbe, Mod-Trichter mit Filter-Prinzip.
- **Fahrzeuge (26.3):** Güter-, Antriebs- und Trichterloren sowie Truhenboote (jede Holzart) in den Stufen
  Verstärkt/Netherit/Enderit – Plätze wie die Truhe, Brenndauer/Tempo wie Ofen/Trichter der Stufe, Filter-Prinzip.
- **Maschinen:** schnellere Öfen (3 Stufen je Ofenart; auf 26.3 aus der Pflicht-Sub-Mod Simple Ovens), Auto-Schmied, Autonomer Crafter, Befiederungs- und
  Schmiedetisch mit Rezeptbuch, stärkere Kolben, Astral-/Nihil-Kolben.
- **Schmelztiegel-Teil** (Tiegel, Seelen-Lava und Eimer selbst in Simple Crucibles, Stufe 5/5b): Vorschlaghammer-Wege
  (Tiegel bauen, Fass anbringen, Aufwerten), Quarz zerschlagen, Advancements „Heißer Topf“/„Seelenschöpfer“/„Bodenloser Eimer“.
- **End-Inhalte:** Astralit/Nihilit/Enderquarz-Paletten und Erze, Astral-/Nihil-Redstone als getrennte
  Redstone-Linien (Geräte nehmen jede Linie an), Brücken-Redstone, Redstone-Blöcke der Linien, Lampen, End-Kolben,
  Astral-/Nihil-Schienen, Enderit-Material (leeresicher), seltene verstärkte Shulker.
- **Kampf/Ausrüstung:** Pfeile aus Teilen (Spitze/Schaft/Feder), Rüstungsbesatz-Boni (Resonanz, Herz-Knopf mit
  8 Stufen im Inventar), Extra-Kopf-Slot im Inventar (Mob-Kopf/Kürbis; Effekte wie im Helm-Slot inkl. Attribute und Frostschutz, Anzieh-Sound, Modell vor einem Helm-Kopf, Größe wie am Mob, Helm darüber), Trank „Crafty Shulker“, Trainingspuppe (Skin per Namensschild), Rüstungsständer mit Armen/Tausch.
- **Deko/Platzieren:** abgelegte Vorlagen, Kleinteile-Häufchen (Barren/Ziegel/Diamant/Smaragd in 3D, Vorschlaghammer schaltet 3D/flach), Schachbrett-Blöcke und Schachfiguren (Achtel),
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
`#simplelib:hammers`/`#simplelib:upgrade_tools`, `OctetCells` (Achtelzellen je Material). Gemeinsame Materialien
(Stufe 3d, `simplelib:*`, früher SB): Diamantkiesel, Rissiger Diamant (+Block, Amboss-Zerschlagen), Netheritklumpen,
Enderit-Grundmaterial (Fragment, Platten, Barren, Klumpen, Block), Basis-/Enderit-Aufwertungsvorlage, Polierter Endstein (Modulfamilie Stufe 3, gemeinsamer End-Baublock), `c:`-Tags;
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
- **Simple Mob Manners** (bis 13.10. Simple Creatures, `simplecreatures`; Config, Befehl und Schalter werden übernommen): Kreaturen stummschalten (Typ/Namenssuffix), dauerhafte Jungtiere, Piglins ignorieren Goldbesatz/-werkzeug. Seit 2026-10-15 (N62): **Eimer mit Frosch** (`simplemobmanners:frog_bucket`, einziges Item des Mods; Wassereimer auf Frosch, Variante als `frog_variant`-Komponente, Name und Alter bleiben; Reiter Werkzeuge nach dem Kaulquappen-Eimer; Textur `tools/textures/frog_bucket_2026_10_15.py`).
- **Simple Farming:** Federfall schützt Acker, Schärfe schneidet Pflanzen, Ernten mit Hacke.
- **Simple Tools:** Haltbarkeitsbonus, Sparsamkeit-Verzauberung (ID bleibt `simplequalityoflife:thrift`), Reparatur hält
  Ambosskosten, Ofen mit Lava füllen.
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
Phase 1). Claim-Urkunde (Phase 2): Rezept Papier + Tintenbeutel + Honigwabe, Rechtsklick beansprucht und bindet sie an den Besitzer (`simpletweaks:deed`, nur Anzeige), gebunden öffnet sie den Rechte-Editor (Code-Feld, Fehlerzeilen vom Server, Online-Namen, Hilfe; simplelib-Kasten, gebündelt), Kopie wie ein Buch (Kopie nicht kopierbar). Claim-Tisch, Erweiterung und Maps-Ebene folgen (Phasen 3–5).
Admin/Schutz (17.10., Plan `docs/ai/PLAN-TWEAKS-ADMIN-2026-10-17.md`, Paket `admin`, Config `config/simpletweaks-admin.json`, je Feature eigener Schalter + OP-Stufe): Block-/Truhen-Protokoll mit `/inspect` und `/tweaks rollback` (an), Inventar-Sicherung beim Tod mit Wiederherstellen (an), `/invsee`/`/endersee` als eigene UI, auch offline (an), Vanish (an), Freeze (an), Neustart-Ankündigung mit Countdown (an), Auto-Backups + `/backup now` (an), Wartungsmodus (Schalter an, Modus aus bis `/maintenance on`), Lag-Finder `/lag` (an, misst nur während des Befehls).
Spieler/Welt/Events (Teil 2, 17.10., `docs/ai/PLAN-TWEAKS-PLAY-2026-10-17.md`, Config `config/simpletweaks-play.json`): an – /coords, privater Todes-Hinweis, @Erwähnungen, /playtime + /seen, PvP je Spieler (/pvp, beide an, Claims haben Vorrang), Abstimmungen (/vote; Tag/Nacht aus), AFK (Tab-Liste grau, zählt nicht fürs Schlafen; Kick aus), Weltgrenze je Dimension (Config-Ebene über die Vanilla-Grenze je Dimension), Teil-Keep-Inventory (hotbar_armor/xp/custom), Drachenei je Kampf, /tweaks event (Doppel-XP, Mehr Beute, Blutmond; Zeitplan aus), /leaderboard (Seitenleiste aus), /phantoms, /pregen; aus – Mob-Griefing je Mob, Willkommen/Tipps, Login-Belohnung (Geldscheine mit Simple Money), Nether-Dach, Mob-Grenze je Chunk, Bodengegenstände aufräumen. Optional: Simple Money (Scheine), Simple Weather (Wetter-Abstimmung aus, wenn Wetter abgeschaltet).

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
| Simple Maps (`simplemaps`, Sub-Mod SB) | in Arbeit (`claude-q-maps`) | Wegfinder-Karte (unendlich, Spieler mittig), GUI mit Lesezeichen, Wegpunkte, Locator-Bar, Karte je Dimension, Fundorte | `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md` |
| Simple Mobs (`simplemobs`, Sub-Mod SB, optional) | Stufe 1 (`claude-q-deceiver`) | Mob Deceiver (Endgame, Wellen aus Fake-/Echt-Mobs, Mirage-Puls über SB-Effekte, falls SB geladen), Täuscherstoff, Spawn-Ei; Furcht-Mobs Schreckensritter (Eisengolem-T mit Amboss in der Mitte, Skelettschädel, Eisenhelm aufsetzen) und Hohler Wächter (End-Außeninseln, Blickmechanik) mit Zitter-Aura (SB-Effekt `shivering`, ohne SB eigener), Ruhe-Amulett, Trank des Fokus (`claude-fear-mobs`, Plan `docs/ai/PLAN-FURCHT-MOBS-2026-10-12.md`); Verdorrter Wither (Wither-Figur auf Netheritblock, 600 Leben, 3 Phasen mit angekündigten Angriffen) mit Witherherz und Biom-Pinsel (Material in der Nebenhand malt Biomzellen, Claim-sicher; `claude-wither`, Konzept `docs/ai/KONZEPT-WITHER-BIOMPINSEL-2026-10-10.md`); Simple-Mobs-Handbuch; spielbar auch ohne SB | `docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` |
| Simple End (`simpleend`, Sub-Mod SB, optional; Super-Mod von End Dragon Fight) | `claude-simple-end` | End-Holz Aschholz/Dämmerholz (volles Vanilla-Holzset inkl. Boote, Schilder, Regal, Laub; mit SB zusätzlich Holzwerk, Holz-/Stamm-Achtel, gestufte Kistenboote, mit Sandwiches Schneidebretter – `claude-endwood-full`, `docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md`), Knospen (Licht ≥ 9 großer Baum, 2×2 Riesenbaum, dunkel + Knochenmehl Stöcke-Gestrüpp), End-Stöcke-Block, Schwebfrucht (4 s Sanfter Fall + Sprung), End-Gras/-Busch (N42), Biome Aschental/Dämmerlande auf äußeren End-Inseln, Items leere-sicher + doppelte Liegezeit, Knabbermilbe (passiv, knabbert Endstein → durchwühlter Endstein, erholt sich); spielbar ohne SB | `docs/ai/PLAN-SIMPLE-END-2026-10-12.md` |
| Simple End Dragon Fight (`simpleenddragonfight`, Sub-Mod von Simple End, optional) | Stufe 1 (`claude-dragon`) | Drache: +1 volle Lebensleiste je Spieler, +15 % je weiterer Beschwörung (300 bzw. 200 je Spieler, Schonfrist 60 s), +20 % Tempo, Eintritts-Animation je Spieler (Warteschlange), ab halbem Leben Dragonmites alle 30 s (Drop Drachen-Chitin); spielbar auch ohne SB | `docs/ai/KONZEPT-END-DRAGON-FIGHT-2026-10-11.md` |
| Simple Respawn (`simplerespawn`, Sub-Mod SB, optional) | Stufe 1 (`claude-respawn`) | Niedergeschlagen statt tot (nur mit Mitspielern online, Config: immer), Ausbluten 60 s, eigener Bildschirm (Zuschauen mit freier Kamera, Inventar nur ansehen, Aufgeben), Wiederbeleben durch 5 s Rechtsklick-Halten (danach 3 Herzen, 0 Hunger), Monster ignorieren Niedergeschlagene, Logout/Absturz = Vanilla-Tod (kein Dupe), Handbuch, Fortschritt Erste Hilfe; spielbar auch ohne SB | `docs/ai/PLAN-SIMPLE-RESPAWN-2026-10-12.md` |
| Simple Beacons (`simplebeacons`, Sub-Mod SB, optional; braucht `simplelib`) | Stufe 1 (`claude-beacons`) | Starke Aufwertungsvorlage (Netherstern, Duplikation), Starkes Leuchtfeuer (eigener Block, 6 Ebenen, Reichweite 60/70, 6 Zusatzeffekte E5/E6, Leuchtfeuer-Bildschirm im Vanilla-Stil mit 2 Zusatzzeilen (Hintergrund nur zur Build-Zeit aus Vanilla abgeleitet), Basis-Tag `strong_base_blocks`), Starker Aquisator (7×7×7-Rahmen, bis 144 Blöcke nur unter Wasser, Delfingunst, Angriff 12), Aufwertung am Schmiedetisch oder in der Welt (Axt/Hammer, 8 Schläge, Daten bleiben), Server-Konfig mit Deckeln; spielbar ohne SB | `docs/ai/PLAN-SIMPLE-BEACONS-2026-10-13.md` |
| Simple Blocks (`simpleblocks`, Sub-Mod SB, optional; nur `framework`) | N69/N71 + Formsäge (`claude-formsaege`, Merge offen) | Gedämpfte Tiefenschieferfliesen, Sculk-Tiefenschiefer, fehlende Vanilla-Treppen/-Stufen/-Mauern, rissige/bemooste Ziegel, Blaue Netherziegel; **Formsäge** (`simpleblocks:shaping_saw`, Prinzip A4b; Vanilla-Steinsäge mit Kupfer, Rezept CIC/###, Aufwertung in der Welt: Steinsäge + Axt bzw. mit SB Hammer + 4 Eisennuggets): schneidet alles der Vanilla-Steinsäge plus Holz (13 Holzarten inkl. Pappel/Bambus: Treppe/Stufe/Zaun/Stöcke, Stamm→Bretter/entrindet/Holz), Wolle, Netherziegelzaun; Rezepttyp `simpleblocks:sawing` (201 Rezepte, Simple End/SimpleBuilding nur mit Ladebedingung), Steinsägen-Rezepte anderer Module (z. B. Octets) erscheinen von selbst; Bildschirm 6×4 mit Scrollen und Suche, JEI-Kategorie, Entdeckungs-Tipp; allein spielbar | `docs/ai/PLAN-FORMSAEGE-2026-10-17.md` |
| Simple Ovens (`simpleovens`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt; braucht `simplelib`) | Modulaufteilung Stufe 4 (`claude-split-s4-ovens`) | Verstärkte/Netherit-/Enderit-Öfen, -Räucheröfen, -Schmelzöfen (2×/4×/8×, doppelte Erfahrung ab Netherit, Tempo aus eigener Server-Konfig, übernimmt SBs Werte beim ersten Start), Antriebsloren der drei Stufen, Menü-Mixin für lange Garzeiten, Aufwertung in der Welt über SimpleLibs `in_world_upgrades` (allein Axt, mit SB Vorschlaghammer), eigener Tab, Handbuch, Fragentisch, Preise; alte `simplebuilding:`-IDs (Blöcke, Items, Block-Entities, Loren, Rezepte, Rezept-Advancements) migrieren über SimpleLib-IdMoves; auf 26.3 nicht mehr in SB (26.2 unverändert); allein spielbar (Enderit-Beute über SimpleLib) | `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md` |
| Simple Crucibles (`simplecrucibles`, Sub-Mod SB, **Pflicht**: SB `requires`+bündelt, Sandwiches bündelt es ebenfalls; braucht `simplelib`) | Modulaufteilung Stufe 5 (`claude-split-s5-crucibles`) | Eisen-/Verstärkter/Netherit-/Enderit-Schmelztiegel (6/12/18/30 Plätze, beheizt vom Block darunter, alle Plätze zugleich, Tempo 1×/2×/4×/8×), Kupfer-/Verstärkte/Netherit-/Enderit-Fässer zum Andocken, Schmelztiegel-Rohling beim Bau (Axt: 16 Eisenbarren auf einen Kessel), verstärkter Kessel, Dorf-Feldküchen, eigene Server-Konfig `simplecrucibles-server.json` (übernimmt die Tiegel-Optionen aus `simplelib-server.json` beim ersten Start), Aufwertung in der Welt (allein Axt, mit SB Vorschlaghammer), Handbuch, Testzentrale-Station, Preise; alte `simplelib:`-/`simplebuilding:`-IDs (Blöcke, Items, Block-Entities, Rezept) wandern über IdMoves; **Stufe 5b** (`claude-s5b-crucibles`): Seelen-Lava (Fluid, Block, Seelenbrand, Weltgenerierung, Nebel, Gruppe `soulLava` der eigenen Config), alle 26 Eimer (Kupfer mit Oxidation/Wachs, Keramik mit 4 Stufen, Enderit halb/voll inkl. Milch und Seelen-Lava = 2 Füllungen, Seelen-Lava-Eimer), eigenes JEI-Plugin (Tiegel-Kategorie, Infoseiten) und Jade-Plugin (Tiegel, verstärkter Kessel); IDs inkl. Fluid/Effekt/Komponenten/Rezepte wandern über IdMoves | `docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md`, `docs/ai/PLAN-S5B-2026-10-16.md` |
| Simple Shops (`simpleshops`, Sub-Mod SB, optional; braucht `simplelib`; Claims/Simple Money nur optional) | Stufen 1–3 (`oc-shops`) | Spieler-Shops: Verkaufsblock (ein Preis aus bis zu 5 exakten Preis-Items nach Filter-Prinzip, 9 Einnahme-Plätze) unter der Vitrine (5 Warenplätze, Glow-Variante hell + Licht 6), Käufer-GUI mit Tausch (1 Stück je Tausch, atomar, Überlauf wird gedroppt, ein Spieler zugleich, Leerlauf-Timeout), Schutz (nur Besitzer/Op ab Stufe 2 bauen ab, Explosions-/Wither-/Drachen-/Kolben-/Trichterschutz, Limit 16 je Spieler, Claims: Setzen braucht B, Abbau durch Besitzer immer, Kauf ohne Recht), Renderer, Handbuch, Wiki; **Server-Shop** (`/servershop create|remove|list|info|tp|edit|price|goods|cash`, nur Op Stufe 2, IDs s001…, unbegrenzter Vorrat per Kopie, Einnahmen = Senke oder Admin-Kasse per Config, kein Abbau durch Spieler, mehrere Käufer zugleich); spielbar ohne SB | `docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md` |
| Simple Wanderers (`simplewanderers`, Sub-Mod SB, optional; keine harte Abhängigkeit, allein mit Vanilla spielbar; SB/Simple Money/Simple Mobheads nur optional) | Stufen 1–3 (`oc-wanderers`) | Dorf-Wanderer: ein umtexturierter Fahrender Händler je Dorf (Spawn beim ersten Besuch an der Dorfglocke, genau einer: Eintrag je Dorf + Zählung + Doppelprüfung), nie unsichtbar, nie despawnend, tötbar (Respawn nach 7 Tagen, nur bei Spieler an der Glocke), Pferd an der Leine (Schleichen + Rechtsklick leer: Pferd einfangen), alle 3 Tage neues Angebot (Seed aus Dorf, Periode, Weltsaat), alle 1,5 Tage Nachschub, Aufteilung der Waren zwischen Dorf-Wanderer und Fahrendem Händler (Vanilla-common 47/31, buying geteilt, Köpfe nur beim Wanderer, Kerne und Seltenes nur beim Fahrenden Händler), Biom-Sortiment je Dorftyp (7 Pools), Spawn-Ei, Handbuch, Config `simplewanderers-server.json`, SB-Schalter „Simple Wanderers aktivieren“ | `modules/simplewanderers/wiki/manual.json`, `docs/ai/KONZEPT-DORF-WANDERER-2026-10-15.md` |
| Simple Trims (`simpletrims`, Sub-Mod SB, optional) | Stufen 2–4 (`claude-trims2`) | Vorlagen Leuchtend/Strahlend/Pulsierend (`simpletrims:*`, alte `simplebuilding:`-IDs migrieren über SimpleLib-IdMoves), Aufwertungen am Schmiedetisch (eigener Rezepttyp, auch Auto-Smither), Strahlkraft-Licht, Leucht-/Puls-Darstellung, Besatzvorlagen ablegen und in der Welt schmieden, Werkzeugregel wie Tiegel (Axt allein, Hammer sobald ein Partner die Axt-Wege abschaltet), Fortschritte, Handbuch, Preise; bündelt SimpleLib; auf 26.3 nicht mehr in SB (26.2 unverändert); Besatz-Boni bleiben in SB | `docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md` |
| QoL-Aufteilung | Besitzer entscheidet | Sub-Mods für Container (fertig), Bewegung, Landwirtschaft, Werkzeuge/Haltbarkeit, Kreaturen, Wetter, Tresor (→ Simple Loot); Schärfe/Gras → Simple Combat; Kreaturen-Logik nach simplelib | QUEUE N14 |
| Mobs/Effekte | Konzepte | End-Mob, Shellker (Schwerkraft), Deceiver, Furcht-Mob, Seelenfeuer-Lohe; Effekte Zittern/Trugbild/Verblasst | `docs/ai/KONZEPT-MOBS-2026-10-07.md`, `KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md` |

Offene Punkte und Reihenfolge: `docs/ai/ROADMAP-2026-10-09.md`.
