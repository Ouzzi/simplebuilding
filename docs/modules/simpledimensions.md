# Simple Dimensions — Entwurf und Quellvergleich (26.3)

Status: Entwurf abgenommen; Implementierung und Pr?fkatalog f?r Fabric/NeoForge 26.3 vorhanden.
Aktuelle Pr?fergebnisse und Grenzen stehen am Ende dieses Dokuments. Modul-ID: simpledimensions; persistenter
Registry-/Loader-Namensraum: simpledimension. Fabric und NeoForge zuerst.

## Quellen und Entscheidung

Primär: simpledimentions_05-2026, Minecraft 26.1.2, Pakete dev.simpledimension.
Sekundär: simpledimensions, Pakete de.simpledimension, einschließlich der vollständig
gelesenen Datei instructions+ (eine Datei, kein Ordner). Beide Quellen bleiben read-only.
Der neuere Stand hat echte Loader-Registrierung, konfigurierbare Portalrezepte,
Blockentitäten und drei Dimensionen; der ältere Stand enthält überwiegend reflektive
Brücken und Paper. Diese Brücken werden nicht übernommen. Die Besitzeranweisung verlangt
sechs Glowstone-Bögen, Zusatzlicht und Plattformbreite Portalbreite + 4. Das ältere
Java implementiert dagegen nur den 5x4-Bogen und prüft indirektes Blocklicht; damit zählt
Glowstone selbst. Diese Umsetzung beweist die spätere Besitzeranweisung nicht.
Der neuere README nennt einfache Rahmen, während defaultSkyblock/miningDimensionPreset/
travelDimensionPreset im Java bereits gemischte Rezeptformen definieren. Java gewinnt.
Skyblock-Standard: sechs Besitzerbögen und separates Licht. Bestehende Definitionen werden nicht überschrieben. Weitere frei konfigurierbare Formen und Aktivierungen werden erst nach dem gemeinsamen Durchgehen mit dem Besitzer erweitert.

## Dimensionskatalog

| ID | Thema / Stimmung | Zugang | Regeln und Belohnung | Weltlogik |
|---|---|---|---|---|
| simpledimension:skyblock | Stille, hellblaue Leere; eigener Bauplatz | Sechs Glowstone-Bögen, Feuerzeug/Feuerkugel und echte Zusatzlichtquelle | Leere Vanilla-Void-Biome; 1:1; sichere Insel mit Rückweg; keine geschenkten Erze/Items | Licht hält einen Weg in die Leere offen; Materialien bringt der Spieler mit |
| simpledimension:mining | Tiefer Stein, Waldoberfläche; Erkundung | DXXD / D..D / D..D / D..D / DEED; D Tiefenschieferziegel, X Diamanterz, E Smaragderz | Quellstandard 0.5 Quellblöcke je Zielblock; Oberfläche Y191, Erze/Bäume über Vanilla-Biomefeatures | Aufwand für den Zugang zu einem getrennten Rohstoffgebiet; keine Chunk-Resets |
| simpledimension:travel | Karger violetter Durchgang | KRRK / O..O / O..O / O..O / SSSS; K Witherskelettschädel, R Harzziegel, O dunkler Eichenstamm, S Seelensand | Quelle 10:1; flache Bedrock-Ebene; keine zusätzliche Bewegungsgeschwindigkeit | Seltene Endgame-Zutaten erschließen einen komprimierten Reiseweg |
| Datapack-/Config-Dimensionen | Vom Pack definierte, zweisprachig dokumentierte Themen | Eindeutiges Rezept oder Rechteck; keine Vanilla-Portalübernahme | Begrenzte Definitionen; unbekannte Ziele deaktivieren Zugang; immer Rückweg | Weltgeneration beim Weltstart, nicht durch /reload nachträglich versprechen |

Mining-Schichten ab Y-64: 1 Bedrock, 64 Tiefenschiefer, 188 Stein, 2 Erde, 1 Gras.
Keine 60/30/10-Erde/Sand/Kies-Mischung und keine mehreren Biome im Flat-Preset: Quelle
implementiert dies nicht. Noise verwendet festes Biome oder Checkerboard, nicht natürliche
Multi-Noise-Übergänge. generateOres wählt bei Noise overworld/caves; orePreset ist in
der Quelle ohne Wirkung. Keine separate Mob-/Beute-/Handels- oder Fortschrittsregistrierung.

Die sechs Glowstone-Matrizen aus instructions+ (G = Glowstone, A = leeres Prüf-Feld):
1. AGA / GAG / GAG
2. AGGA / GAAG / GAAG / GAAG
3. AGGGA / GAAAG / GAAAG / GAAAG
4. AAGGAA / AGAAGA / GAAAAG / GAAAAG / GAAAAG
5. AAGGGAA / AGAAAGA / GAAAAAG / GAAAAAG / GAAAAAG
6. AAGGGGAA / AGAAAAGA / GAAAAAAG / GAAAAAAG / GAAAAAAG
Außenecken bleiben geprüft leer und werden nicht selbst zur Portalfläche. Lichtsuche
Radius 3 um die Form: emittierende Blöcke, kein Glowstone, Feuer, Seelenfeuer oder Portal.
Geplante Insel: runde Unterseite, sichere ebene Oberseite; Breiten 7/8/9/10/11/12,
zwei Blöcke Überstand je Seite. Rückweg darf keine Lichtpflicht haben, die Spieler einsperrt.

## Vollständiges Quellinventar

Neu: Block und Blockentität simpledimension:sky_portal, Achse x/z; BE-Felder Color (RGB)
und Destination (String). Kein Blockitem, kein eigener Kreativtab, Entity, Enchantment,
Befehl, Keybind, Craftingrezept, Loot-Tabelle, Tag, Advancement, Mixin oder Config-GUI.
Portalbau ist ein Welt-Rezept und kein Craftingrezept. Oberfläche: weiße animierte
Netherwirbel-Textur mit BE-Tint und seltenen Portalpartikeln. Vanilla-Feuerzeug und
Feuerkugeln aktivieren, Spieler-Berührung reist. Quelle schreibt deutsche Chatmeldungen;
diese werden durch Sounds/Partikel ersetzt. NeoForge lädt generierte Packs über
FolderRepositorySource; Fabric verlangt manuelles Kopieren. Gleichwertige automatische
Pack-Anbindung beider Loader ist ein Portziel. README nennt vier Beispiele, vorhanden sind drei.
Alt: simpledimension:light_blue_portal einschließlich Blockitem und skyblock-Dimension;
5x5x3-Halbkugel, Spawnlocator, 1:1-Reise, Lichtabfrage, gemeinsame Orchestrator-Interfaces,
Fabric/Forge/NeoForge-Reflection und Paper-Plugin. Legacy-Block/Item-ID muss lesbar bleiben,
auch wenn es im Survival keinen Drop und kein Craftingrezept gibt.

Quelltests neu: DimensionPortalConfig, PortalFrameScanner, PortalRecipeMatcher,
PortalTravelRules, TeleportCooldowns (JUnit). Alt zusätzlich FrameValidator, Activation,
DestinationPlanner, VisualSpec, TravelService, RuntimeService, Orchestrator, Geometry,
SpawnLocator, BootstrapService, AssemblyRecipe, HemispherePlatformGenerator. Unit-Tests
sind kein Launch-/Worldgen-Nachweis; Pflichtkatalog muss tatsächliche Serverwelten prüfen.

## Konfigurationsvertrag

Pfad bleibt config/simpledimension/dimensions/*.json. Eine Datei je Definition.
Identität: id, sourceDimensionId, targetDimensionId, targetDisplayName.
Optik: portalColorHex. Form: portalRecipes (rows, legend, interior, ignore), frameBlock,
minPortalWidth, maxPortalWidth, minPortalHeight, maxPortalHeight.
Reise: portalDelayTicks, teleportCooldownTicks, travelCoordinateScale,
allowIgniteFromSource, allowIgniteFromTarget, openFromDimensions,
generateReturnPortalOnArrival, createDestinationPlatform.
Generation: worldGeneration.preset, generatorType, baseBiome, biomeSelection, minY,
height, logicalHeight, hasSkylight, hasCeiling, ultraWarm, natural, coordinateScale,
generateOres, orePreset, flatLayers (block, height). Bestehende Schlüssel bleiben lesbar;
wirkungsloses orePreset/preset nicht als funktionierende Stellschraube bewerben.

Ziel-GUI mit wenigen Serveroptionen in Reiter Zugang/Sicherheit; fortgeschrittene
Worldgen/Rezepte als JSON/Datapack. Jede Option EN/DE, Name/Tooltip/Reiter/Default.
Obergrenzen: höchstens 16 Definitionen, 8 Formen je Definition, höchstens 21x21 Innenraum,
23x23 Rezeptmatrix, 8 zusätzliche Ursprünge, Verzögerung 0..200 Ticks, Cooldown 20..1200,
Koordinatenverhältnis 0.5..10 (NaN/Infinity verweigern). Weltgeneration minY -64..0,
Höhe 16..384 in 16er-Schritten, logicalHeight <= height; Listen/Dateigrößen begrenzen.
Neue Config niemals Clientautorität; Serveroptionen nur lokaler Server/OP. Abschaltbar:
Zugang, automatische Zielanlage und Nichtspielerreise (Standard aus). Abschalten des
Zugangs sperrt keinen Rückweg. One-way-Schlüssel nur als Legacy-Input: ohne sicheren
Rückweg keine Hinreise. Begrenzungen und Defaults als benannte Konstanten exportieren.

## Rückweg und Sicherheitsmodell

Hinreise erst nach vollständiger Prüfung von Zielregistry, geladenen Chunks, Weltgrenze,
Bauhöhe, sicherer Boden-/Kopffreiheit und Rückweg. Keine position-only-Fallbacks. Keine
Reittiere/Passagiere/Items: keine Duplikation von Inventaren. Cooldowns mit Serverticks,
Zustand je Spieler und aktuellem Portal, Reset bei Logout/Serverstop/Portalwechsel;
kein System.currentTimeMillis und keine statischen undichten UUID-Listen.
Persistente exakte Rückverbindung, nicht nur skalierte Koordinaten; fremde nahe Portale
niemals ungeprüft übernehmen. Alter Destination-String wird validiert. Color ist rein optisch.
Portal zerstört/Config entfernt/Ziel fehlt: Server behält Spieler am sicheren Ursprung;
Spieler in entfernten Definitionen erhalten einen sicheren Rückweg zum ursprünglichen
Ursprung, sonst Oberweltspawn. Kein /reload-Versprechen für neue Dimensionsregistries.

Automatisches Kopieren wertvoller Rahmen/Erz-/Schädelblöcke aus der Quelle dupliziert
Ressourcen. Keine generierten abbaubaren wertvollen Rahmen; Rückweg und Plattform liefern
keine Rohstoffe. Quell-clearIfObstructing löscht fast jeden Nicht-BE-Block und kann Bauten
zerstören: nicht übernehmen. Ohne generische Claim-Freigabe keine Änderungen an bestehenden
Bauten. Plan zuerst, Mutation danach; kein teilweiser Zielbau bei verweigerter Reise.
Harte Obergrenze für Suchradius, Zielanlage/Spieler/Tick und Gesamtportale; keine Chunktickets
nach Reise. Keine Border-Clamps, die mehrere Verbindungen auf fremde Bauten zusammenlegen.
Ablehnung mit Sound/Partikeln, keine Chat-/Aktionsleistenmeldung.

## Kollisionen und Integration

SimpleBuilding bietet Spawn-Teleporter und Echolot, aber keine eigenen Dimensionen oder
simpledimension-IDs. Nicht duplizieren: Spawn-Konfiguration, generelle Nether-/End-Sperren,
Chunkloader und Spawn-Elytra. Module koppeln nur über öffentliche Registry-IDs/Vanilla.
SpawnRules.blocksDimensionChange kontrolliert Vanilla Nether/End; Modul darf keinen
Befehls-Bypass verwenden. Tests müssen echte Wechsel samt geladenem SimpleBuilding prüfen,
plus fremde Items im Rucksack/Trichter und funktionierende Vanilla-Netherportale.
Kein Quell-Keybind oder Mixin; daher kein gleicher Methoden-Injektor. Obsidianrechtecke
verweigern, Custom-Rezepte dürfen Vanilla-Portalformen nicht übernehmen. Kein neues
Gebietsschutzsystem; konservativ unbekannte Claim-Integrationen behandeln.

## Pflichtprüfungen und spätere Ports

Beide 26.3-Loader: Launch/Registry, drei geladene Dimensionen und echte Chunkgeneration,
alle Formen/Achsen/Mutationen/Lichtfälle, Zündkosten, Portalzerfall, Warmup/Cooldown,
exakter Rückweg mehrerer Ursprünge, kein Bounce, Entfernen/Abschalten/Restart, BE-Roundtrip
alter Color/Destination-Daten, Border/Höhe/Nichtspieler/Passagiere/Claims/Spam/NaN,
keine Rahmen-Duplikation oder überschriebenen Bauten; vollständige Config-/Lang-/Datenprüfung.
Quell-Unitfälle zusätzlich übernehmen. SimpleBuilding-Hauptsuiten und integration-263,
Wiki generate + check und abschließendes gradlew.bat check -q. Client-Smoke nur nach
Prozessprüfung: Titel -> Welt, getönte Portalansicht und Config, Screenshot ansehen.
Keine Pflichtprüfung als skipped oder Trockenlauf als Launch-Nachweis darstellen.

Forge 26.3: später eigener Registry-/Event-/Pack-/Client-/Testadapter. 26.2, 1.21.11 und
26.4 vollständig deferred bis Release-Port; keine Quellen dort ändern. Paper ist kein
Loader für denselben Clientmod-JAR und bleibt separates Projekt. Echte alte Spielwelt
muss zusätzlich abgenommen werden; IDs allein beweisen keinen erfolgreichen Weltupgrade.


## Implementierung (Fortsetzung 2026-09-30)

Modulger?st mit tools/newmod.py; Modul-/Launch-Hub-ID simpledimensions, tats?chliche
Loader-, Block-, Item-, BE- und Dimensions-IDs weiterhin simpledimension. Fabric/NeoForge
26.3 werden gebaut; Forge-Ger?st ist nicht im aktiven Manifest und gilt nicht als Port.
Kein SimpleBuilding-Feature wurde kopiert. Vanilla-TeleportTransition respektiert dessen
Nether-/End-Sperr-Mixin; keine Befehlsreise und keine internen SimpleBuilding-Imports.

Stufencommits: 4d8c3af9 Ger?st, f901cdb2 Registry/BE, 049597b3 JSON/Caps,
dfd10ef7 Formen/Z?ndung, b12d1399 Reisen/R?ckadressen, e864f622 Pack/Generation,
111baec0 Spiel-/Quelltests samt Client-/Configadaptern. Abschlie?ende Daten/Belege folgen.

Serverdatei config/simpledimension/server.json hat Zugang, automatische Zielanlage,
Mindestwartezeit und Mindestcooldown. NonplayerTravel ist ein reservierter Legacywert,
der immer false normalisiert wird: Mobs, Items, Reittiere und Passagiere sind verweigert.
Portaldefinitionen behalten die Quellschl?ssel in config/simpledimension/dimensions/*.json.
Die Einzeldefinition kann gr??ere Wartezeiten als den globalen Mindestwert verlangen.
Bestehende JSON-Dateien werden nicht ?berschrieben. Beispiele entstehen nur, wenn die
jeweilige Datei fehlt. Alle Optionsnamen, Reiter,
Tooltips und Defaults sind in config-options.json mit EN/DE-Schl?sseln beschrieben.

GeneratedPack l?dt denselben verpflichtenden Pack auf beiden Loadern, vor dem Laden der
Weltregistries. Gen.natural w?hlt Oberweltuhr/Timelines; ultraWarm wird zu Wasserverdunstung
und schneller Lava ?bersetzt. Beds/Anker setzen bewusst keinen Spawn in den Moddimensionen.
Legacy preset/orePreset bleiben Bezeichnungen. Noise verwendet Quell-Settings
(overworld bei generateOres=true, sonst caves); das ist kein Schalter zum garantierten
Entfernen aller Erze. Mining verwendet Vanilla-Flat-Biomfeatures statt eigener Erzalgorithmen.
Ziele sind konservativ auf simpledimension:<id> beschr?nkt; Vanilla-Zieldimensionen sind
nicht ?ber JSON freischaltbar. Datapacks k?nnen diese eigenen Dimensionen definieren.

Zielplan: runde Bedrockinsel mit ebener Oberfl?che, keine abbaubaren wertvollen Rahmen.
Existierende Bl?cke werden nie gel?scht/?berschrieben. Eine erste Reise braucht Insel und
R?ckportal; Legacy-One-way-/Plattform-Aus schaltet diese Reise ab. H?chstens neun Chunks
je Zielplan, ein Zielbau pro Servertick, 1.024 dauerhaft gez?hlte Portalanker und 4.096
Spieler-R?ckadressen. Die Ankerobergrenze z?hlt auch fr?here Portale (kein billiges
Abbauen/Neusetzen zum Umgehen). Es gibt keine dauerhaften Chunk-Tickets.

Jede erzeugte R?ckfl?che speichert das exakte sichere Ursprungsziel; alle Quellfl?chen
speichern die exakte Zielverbindung. config entfernt/Zugang aus sperrt erzeugte Ausg?nge
nicht. R?ckadressen ?berleben einen Serverneustart unter <world>/simpledimension-returns.json.
Ein zerst?rter/unsicherer Ursprung f?hrt zur gepr?ften Oberweltoberfl?che nahe Spawn.
Sollte auch dort kein sicheres, erlaubtes Feld existieren, verweigert die Mod die unsichere
Teleportation; sie ?berschreibt keine Spawn-/Claimbauten. Auf einer vollst?ndig absichtlich
unbewohnbaren Oberwelt kann ein Administrator daher weiterhin eingreifen m?ssen.

Claim-Sicherheit: Vanilla mayInteract plus eine ?ffentliche modulinterne Permission-Schnittstelle.
Erkannte flan/ftbchunks/openpartiesandclaims/griefdefender/claimchunk sperren Portal?nderungen
und Reisen ohne registrierten Adapter. Konkrete Drittanbieteradapter sind nicht implementiert;
die Mod behauptet keinen universellen Claim-Nachweis. Keine eigenen Clientpakete existieren.
Z?ndposition und Reichweite werden serverseitig gepr?ft (h?chstens f?nf Bl?cke); h?chstens
ein Z?ndversuch je Spieler pro zehn Serverticks. Framezerfall pr?ft h?chstens acht geladene
Anker je Tick; keine Chunkladung durch die Wartung.

Config-GUI: drei Cloth-Reiter Zugang/Sicherheit/Dimensionen. Lokaler integrierter Server ist editierbar;
Mehrspieleransicht ausdr?cklich nur lesend. Kein unsicheres OP-Edit-Paket wird eingef?hrt:
Dedizierte Server bearbeiten JSON und starten f?r Definitionen/Weltgeneration neu.
Fabric-Mod-Men? und NeoForge-Konfigurationsbutton ?ffnen die Seite. Quellanimation bleibt
erhalten; Farbwert synchronisiert per Blockentit?t und eigener Tintquelle auf beiden Loadern.
Keine neue Pixelkunst; keine Chat-/Aktionsleistenmeldungen. Keine eigenen Craftingrezepte,
Beutetabellen, Entities, Enchantments, Commands oder Keybinds; das Weltrezept ist die Quelle.

Pr?farchitektur: eigener Manifestkatalog und Modulziele f?r beide Loader. SimpleBuilding
ist in beiden Modulinstanzen geladen. Die 26.3-GameTestServer-Klasse verwirft normalerweise
Datapack-Dimensionen; DimensionTestWorldMixin l?sst nur diesen Testserver echte Datapack-
LevelStems ?bernehmen. Normale Welten verwenden unver?ndert Vanilla-WorldLoader.
Die fünf JUnit-Quellkataloge bleiben erhalten: aktuelle Standardformen, alte
unbegrenzte Ratios jetzt geklemmt, Cooldown-Uhren instanzgebunden mit Serverticks.

Derzeitiger gr?ner Modulbeleg: 60/60 Spieltests, beide Loader, Run
2026-09-30T16-50-58Z-7109. Dieser Beleg ersetzt das abschlie?ende Gate nach den letzten
?nderungen nicht. Client-/Bestands-/Gatebelege werden nach tats?chlichem Lauf erg?nzt.
Echte alte Besitzerwelt, Forge 26.3 und 26.2/1.21.11/26.4 sind separate sp?tere Abnahmen.

### Abschlussbelege (2026-09-30)

Bestand Fabric/NeoForge 26.3: 1.562/1.562, Integration 1/1 und damaliger
Modulkatalog 60/60 gemeinsam gruen (Run 2026-09-30T17-01-21Z-ad92).
Erweiterter Modulkatalog: 68/68 gruen (2026-09-30T17-30-24Z-c774),
23 uebernommene JUnit-Pruefungen ohne Fehler; Fabric-Client 3/3 gruen
(2026-09-30T17-24-34Z-f4ba), Portal- und Konfigurationsscreenshots angesehen.
Legacy-Dimensionstyp simpledimension:skyblock bleibt neben skyblock_type ladbar.
Beide Portal-BlockEntityTypes verweigern nicht autorisierte Creative-Item-NBT;
gefaelschte Rueckverbindungen wurden auf beiden Loadern abgewiesen (2/2,
2026-09-30T17-35-34Z-65d2). Endlauf/Gate siehe abschliessenden Commit und Run-Daten.

Generische Infrastrukturkorrekturen: Wiki liest optional namespace aus dem Manifest
(statt Modulordnernamen als Welt-ID); Integration verwendet optional modId fuer
Client-Abhaengigkeiten. Beide behalten id als Default und enthalten keine Mod-Sonderzweige.
Launch Hub entdeckt das Modul und seine Tests aus dem Manifest; keine eigene Registryzeile.
Nicht verifiziert: echte alte Besitzerwelt, NeoForge-Client, externe Claim-Adapter.
Forge 26.3 und 26.2/1.21.11/26.4 bleiben ausdruecklich zurueckgestellt.


Endlauf 2026-09-30T17-38-55Z-8a8a: alles gruen, 68/68, 0 rot (34 je Loader).
Abschliessendes ./gradlew.bat check -q --no-daemon: Exit 0; Wiki aktuell, 19 Wiki-Tests gruen, checkBalance 0 Fehler, Modul-Datenpruefung gruen.


### DIMFIX diagnosis (2026-09-30, codex-dimfix)

The arrival plan used MOTION_BLOCKING, then required every planned cell to be air.
Nonblocking plants do not raise that heightmap, so the lowest bedrock layer can
intersect short grass. This is a product defect, not a reason to relax the travel
assertion or discard integration worlds. WORLD_SURFACE includes every nonair
block and raises the complete bounded plan above vegetation without clearing it.
Existing air/permission/border/build-height checks, chunk limits, build budget,
coordinate mapping and exact return links are unchanged.

Evidence: the original code plus a deterministic short-grass fixture failed the
unchanged Real mining outbound assertion (run 2026-09-30T18-11-32Z-2ed8, 0/1).
Temporary refusal logging reported the platform occupancy check:
`center=(-11431996,204,2230880), blocked=(-11431996,202,2230880),
state=minecraft:short_grass, motion height=202, allowed=true,
tick=776, lastBuild=-1, portals=81`.
This excludes claims, the per-tick build budget, portal capacity and borders for
the reproduced refusal. The normal merged module set was loaded throughout.
The mining test now deliberately places supported grass above the highest
surface in the arrival footprint and checks plant preservation and safe ground,
in addition to real outbound travel, the 0.5 ratio and exact return.

Limits of the historical reproduction: the fresh worktree had no integration
save and initially passed. A read-only copy of sbgate's failed world also passed
at a new random Fabric test location. Attempts near the two reported historical
positions passed; the exact original terrain/refusal was not captured. Thus the
short-grass failure mechanism is directly demonstrated; attributing the old
unlogged failure to that same mechanism remains an inference. No owner/gate
world was edited or erased; only this worktree's disposable test-world copy was
used. Temporary logging and fixed-coordinate experiments were removed.


DIMFIX verification (all summary lines read):
- 2026-09-30T18-12-58Z-3fd9: alles gruen: 68/68 bestanden, 0 rot (34 per loader).
- 2026-09-30T18-15-03Z-fddb: alles gruen: 34/34 bestanden, 0 rot (second fixed Fabric suite).
- 2026-09-30T18-16-18Z-7405: alles gruen: 1600/1600 bestanden, 0 rot (800 per main-line loader).
- 2026-09-30T18-18-28Z-fa6b: alles gruen: 379/379 bestanden, 0 rot (integration plus all nine Fabric/NeoForge module catalogues; third fixed Fabric Dimensions suite, 34/34).
- Both main-line centre build, complete item/block coverage and command/station tests passed; owner world was untouched.
- Default/all-module wiki generation and checks passed. No client tests, owner-world travel, external claims, Forge or deferred MC runtime lines were verified.
- Final `./gradlew.bat check -q`: GRADLE_EXIT=0; 19 wiki tests, balance/module gates and existing shared/26.2 compilation passed. Source/test/wiki fix commit: 4fdfebe7.


## Dimensions settings (2026-09-30)

Skyblock, Mining and Travel have independent default-on server settings: `skyblockEnabled`,
`miningEnabled`, `travelEnabled`. The dedicated Dimensions tab shows localized names,
tooltips and defaults. Local integrated-server changes are queued on the server thread;
remote clients retain the existing read-only notice without a server-edit packet. A
dedicated administrator sets the server-owned `config/simpledimension/server.json` and
restarts the server. Existing server files without these keys default all three to on.

Ignition, first outbound travel and already-linked outbound travel use the same settings
check. Generated return portals bypass access switches. Disabling access does not remove
dimension registries, world data, portal links or existing JSON definitions. The old
`enabled` value is ignored for the three built-in definitions, even when false; custom
definitions retain that legacy switch. The global access switch remains a master control.
The six glowstone arches and their separate-light requirement are unchanged. No new
shape editor or activation mechanics are added.

Both loader builds consume the module's shared `en_us.json` and `de_de.json`; they have
no separate 26.3 language overlay. Runtime tests inspect both shipped locales on each
loader; the data check also verifies category, scope, defaults and GUI save bindings.

### Settings verification

- Branch `codex-next-dimensions`; plan `37293dc4`, implementation `f83a75c4`.
- Both complete module catalogues: `2026-09-30T20-51-56Z-7330`, **alles gruen: 76/76 bestanden, 0 rot**, 38 per loader. Four new cases per loader cover the three independent switches plus old-file defaults/persistence. Journeys invoke Vanilla `useItemOn` and wait for registered server tick hooks: disabled ignition and first/linked outbound access, reenablement, exact safe return while off after disk reload, and unchanged definition files. Existing 34 cases per loader remain intact.
- Isolated Fabric/NeoForge test centres: `2026-09-30T21-05-46Z-cc12`, **alles gruen: 10/10 bestanden, 0 rot**; rebuild and complete SimpleBuilding item/block coverage passed. Owner world untouched.
- Final `gradlew.bat check -q`: **GRADLE_EXIT=0**, log `scratchpad/dimensions-settings/final-check.log`, output read. All 23 JUnit cases passed (five catalogues, no skipped cases), 19 wiki tests passed; balance/module/atlas/Jade/wiki gates and existing shared/26.2 compilation passed. Default/module wiki generation and checks passed.
- Automatic generated-output exception to the module folder boundary: the module's `wiki/data/simpledimensions.json`/JS and manifest-driven wiki index were regenerated. No shared implementation, build, runner, Hub, other module language or deferred-line source changes.
- Not verified: clients/visual settings interaction, owner-world travel, a physical dedicated-server process restart, external claim adapters, full merged server/integration/all-module gate, Forge or other Minecraft runtime lines. Runtime reconstruction from disk proves settings persistence; it is not a process-restart acceptance test. The orchestrator runs the complete merged gate; owner client/multiplayer acceptance remains. No new owner decision, push or merge.
