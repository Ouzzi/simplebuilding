# Simple Dimensions — Entwurf und Quellvergleich (26.3)

Status: Entwurf vor Implementierung. Die folgenden Sicherheitsregeln sind Zielverhalten,
kein Nachweis einer bereits fertigen Mod. Modul-ID: simpledimensions; persistenter
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
Geplanter Skyblock-Standard: sechs Besitzerbögen und separates Licht; neue Quellrezepte
bleiben über bestehende JSON-Konfigurationen verwendbar. Besitzerentscheidung zur
abweichenden Standardform ist angefragt. Keine bestehende Konfiguration überschreiben.

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
