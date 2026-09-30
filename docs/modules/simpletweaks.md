# Simple Tweaks: Vollabgleich und 26.3-Kompatibilitaetsmodul

Historischer Audit vom 2026-09-30, Arbeitsbranch `codex-port-tweaks`. Die folgenden
Auditabschnitte beschreiben den damaligen Kompatibilitaetsstand. Der neue,
standardmaessig ausgeschaltete Claim-Port steht im Abschnitt **Claims-Fortsetzung** am Ende.

## Ergebnis und Abgrenzung

Alle aktiven Nicht-Claim-Funktionen der Quelle existieren bereits in SimpleBuilding. Das Modul
registriert deshalb genau **ein** eigenes Item: die wirkungslose alte `simpletweaks:claim_deed`.
Es stellt die fehlende Namensraum-Kompatibilitaet fuer vorhandene Items, Blockzustaende,
Blockentitaeten und vier Elytra-Komponenten her. Aufloesen liefert das **selbe Registry-Objekt**
wie SimpleBuilding; keine zweiten Pads, Raketenregeln, XP-Mixins, Befehle, Keybinds oder Rezepte.

Die ausdruecklich zurueckgestellten Claims werden nicht aktiviert. Alte Claim-Dateien werden
weder geladen noch ueberschrieben. Eine Urkunde behauptet im Namen/Tooltip keinen aktiven Schutz,
veraendert bei Benutzung keine Welt und behaelt `minecraft:custom_data` unveraendert.

Dies ist eine Kompatibilitaetsimplementierung, keine Aussage, dass die bereits bestehenden
SimpleBuilding-Funktionen jede neue Sicherheitsanforderung erfuellen. Offene Bestandsrisiken
stehen unten; deren Behebung braucht einen eigenen SimpleBuilding-Run statt duplizierter Logik
im Modul. Im bestehenden SimpleBuilding-Code wurde nichts geaendert.

## Gelesene Quelle

Read-only: `C:/Users/oussa/Downloads/Minecraft/Mine/custom created mods/simpletweaks`,
Fabric 1.21.11, Version 1.2.12, HEAD `bb8f976bcaf387e72e184354de3f9f16423c618d`.
Der Arbeitsbaum war bereits dirty (15 Dateien); diese Aenderungen sind Teil dieses Abgleichs.
Die Quelle wurde nicht gebaut, ausgecheckt oder beschrieben. Der lokale Branch
`remove-ported-features`, `9882906050294967d229b3c7874c98e73b29c228`, wurde ausschliesslich
mit `git show`/`git ls-tree` gelesen. Er enthaelt nur Claim-Implementierung, Item, Befehle,
Datagen und Dokumentation; keinen uebersehenen zweiten Restmod.

`readme.md`, `readme-modrinth.md`, `todo.md`, Init, Config, Registrierungen, Datagen,
Mixins, Netzwerk und Implementierungen wurden gegen den Zielcode abgeglichen.
56 Java-Dateien; keine Quell-Unit-/GameTests im Git-Inventar. Vollstaendige Datei-Hashes,
Methodeninventar, Ziel-Dateien, alle 19 Quell-Rezept-JSONs, Konfigurationsdeklarationen und
Mixinliste liegen in `modules/simpletweaks/audit/source-inventory.json`.
Der Lizenztext ist CC0-1.0; das README-Schild Apache 2.0 ist dazu widerspruechlich.
Die urspruengliche Urkundentextur wurde bytegleich uebernommen, Vorschau 16x daneben.

## Jede Funktion: Quellverhalten, Ziel, Entscheidung

Ziel-Dateien unter `common/src/shared/java/com/simplebuilding/tweaks/`, sofern nicht anders angegeben.
Die genannte Klasse und Methoden sind Codebelege; Kommentare/README allein sind kein Beweis.
Alle vorhandenen Server-Regressionstests werden im ungefilterten 26.3-Bestandslauf ausgefuehrt.

| Funktion | Quelle / Abweichung im bestehenden SimpleBuilding | Entscheidung / Regression |
|---|---|---|
| Diamantplatte | Quelle: nur Spieler, Wasserlog; 2 Diamanten waagerecht. Ziel `DiamondPressurePlateBlock`: vorhanden. | Duplikat auslassen; `TweaksTests.diamondPressurePlatesReactToPlayersOnly`, Wasser-Setztests. |
| Netheritplatte | Quelle prueft das **ganze Inventar** gegen Fassitems, nicht bloss Hand wie README. Besitzer 1,5 s, Fremde 10 s; kein Fass = alle Spieler. Ziel `FilterPressurePlateBlock`: vorhanden. | Quellcode massgeblich; keine zweite Filterplatte. `netheritePlatesAdmitOnlyHoldersOfBarrelItems`. |
| Kupferplatten, alle vier Oxidationsstufen | Quelle wartet 20/40/60/80 Ticks beim Betreten, schaltet beim Verlassen sofort aus. Ziel wartet symmetrisch auch beim Verlassen, hat Wachsvarianten und sichtbar gedrueckte Modelle; Axt erst Wachs, dann Oxidation. | Besitzer-Aenderung beibehalten; `PressurePlateTests` und `copperPlatesWaitLongerTheMoreTheyOxidized`, `copperPlatesOxidizeInOrderAndTheAxeScrapesThemBack`. |
| Elytra-Pads I-IV | Quelle `new Box(pos).expand(radius, -2, radius).stretch(0,height-2,0)`; die README-Breiten sind keine exakten geometrischen Belege. Ziel fuenf Stufen: 1/5/16/32/128 Breite, 15/31/63/95/127 Hoehe. | Besitzer-Geometrie beibehalten; `TweaksTierTests.elytraPadAndFlypadAreasMatchTheirTiers`, echte Bereichstests. |
| Pad-Elytra, Aufladen | Quelle gibt nur bei leerem Brustslot, Flugzeit reset im Bereich, Boosts nur in niedriger Saeule, `IS_SAFE_ELYTRA=false`. Ziel erhaelt Spawn-Sicherheit, ab Enderit ganze Flaeche laedt Boosts. | Bewusste Enderit-Erweiterung; `elytraPadsEquipAnUnsafeSpawnElytraInTheirArea`, `elytraPadsRechargeBoostsInTheColumnAndFromEnderiteOnInTheWholeArea`. |
| Flypads I-IV | Quelle 4 Ranges, Flight-Tracking je BE; Abbau nimmt Flug nicht sicher zurueck, Ueberlappung kollidiert. Ziel drei Enderit-Stufen 4x4x6, 8x8x12, 16x16x24, Sicherheitsnetz, nur selbst erteilter Flug wird entzogen. Alte Netherit-/Enderit-IDs bleiben als Legacy-Tiers. | Besitzer-Stufen und Fehlerkorrektur beibehalten; `flypadsGrantFlightInsideAndTakeItBackOutside`, `overlappingFlypadsKeepThePlayerFlyingAndTakeOnlyTheirOwnFlight`, Tier-Migration. |
| Spawn-Teleporter I-IV | Quelle vier Ziele, immer 100 Ticks, Bewegung >0,0001 Distanzquadrat bricht ab. Ziel drei Zeiten 1000/400/100 Ticks, ein konfiguriertes Ziel; Enderit bevorzugt Bett/Anker, Redstone erzwingt allgemeines Ziel, jedes Signalwechselereignis setzt zurueck. | Besitzer-Aenderung beibehalten; `spawnTeleportersSendStillPlayersToTheirSpawnPoint`, Fallback-/Signal-/Tier-Tests. Alte source `_tier_3/4` loesen bestehende SB-Legacy-Tiers auf. |
| Teleporter-Effekte / Besitzerpartikel | Quelle Countdown/Abbruch/Willkommen als Bildschirmtext, Riser-Sounds, Ziel +2Y, Nausea/Darkness/Slow Falling; Client-Owner-Mixin. Ziel Sounds/Partikel/Blockzustand; Partikel durch eigenen Client-BE-Tick. | Keine alte Chat-/HUD-Logik portieren; subjektive Klaenge bleiben Besitzerabnahme. |
| Launchpad | Quelle 16 Ladungen, 60 Ticks, Schub 1,5+0,4*n, Einzel-Laden, Abbau verliert Ladung. Ziel 4/8/16 und 1,5+0,8*n, Schleich-Laden ganze Hand, Ueberschuss/Abbau wirft Windkugeln, Enderit-Fallschutz. Ein heutiges normales Pad ist absichtlich schwaecher als das volle Quellpad; hoechste Zielstufe mit 16 ist staerker als Quellpad mit 16. | Besitzer-Balance beibehalten; `TweaksTierTests` (Fassung, echter Start, Schleich-Laden, Upgrade-Ueberschuss), `flightTimeAndBoostsAreCappedAndBrokenLaunchpadsDropTheirCharges`. Kein falsches Versprechen voller 16-zu-16-Gleichwertigkeit. |
| Chunk-Loader | Quelle ein Chunk dauerhaft, entlaedt blind auch fremd erzwungene Chunks. Ziel Stufen 1/Kreuz5/9, eigene Forced-Menge, Uebergabe, Online-Besitzer-/Dimensions-/Config-Gates, sichtbarer Aktivzustand. | Besitzer-Online-Regel und Fehlerkorrektur; `chunkLoadersForceTheirChunksAndReleaseOnlyTheirOwn`, `chunkLoadersReleaseOnSetblockAndHandOverSharedChunks`, Tier-Tests. |
| Besitzer-Abbau / Kolben / Wasser | Quelle einzelne Klassen schreiben `Owner`; fremd langsam ist **kein** Landclaim und keine generelle Unzerstoerbarkeit; Creative Vanilla. Ziel gemeinsame `OwnedBlockEntity`/`PadOwnership`, Wasserlog und tierbezogene Regeln. | Kein zweiter Griefschutz; `ownersBreakTheirPadsFastAndStrangersSlowly`, `placingPadsMakesThePlacerTheOwner`, `padsPlacedInWaterAreWaterloggedAndLeaveTheWaterBehind`. |
| Spawn-Elytra und Timer | Quelle verteilt Elytra/Regeneration/Glowing in Quadrat in jeder Dimension; Ticktimer, Cleanup nur einmal/s und nur wenn Spawn-Funktion an. Ziel nur Weltspawn-Dimension, Cleanup auch bei abgeschalteter Spawn-Funktion, keine Lagerung/Drop/Bundle, Flugzeit 1..86400 s, Boostanzahl 1..100. | Fehlerkorrektur/Caps beibehalten; Ablauf-, Slot-, Spawngebiet-/Fallschutztests. |
| Spawn-Elytra Boost-Paket | Quelle prueft nur Elytra/Restladung, addiert unbeschraenkte konfigurierte Staerke; kein serverseitiger Gleitcheck. Ziel `TweaksNetwork.handleBoost` fordert Gleitflug und serverseitige Ladung, aber weiterhin kein Geschwindigkeits-/Staerkecap/Ratecap. | Gleitcheck korrekt; **offener Sicherheitsfix** unten. `theSpawnElytraBoostSpendsOneChargeAndOnlyWhileGliding` beweist nicht Speed-/Rate-Sicherheit. |
| Boost-HUD und zweite Timeranzeige | Quelle zeichnet blaue182px-Leiste statt XP und mm:ss/Active mit unter30s Blinkfarbe; zusaetzlich separater Timer ueber Hunger. Ziel behaelt XP-Leiste/Timer, respektiert SB-HUD-Schalter und getrennte NF-Level-Ebene; zweite Hunger-Anzeige entfaellt. | Besitzerentscheidung gegen Doppelanzeige, kein weiterer Modul-HUD; visuelle Bestandsabnahme bleibt offen. |
| Elytra-Schadensschutz | Quelle safe-Elytra schuetzt Fall/Kinetik, Spawn-Fallschutz-Kreis passt nicht zum Elytra-Quadrat. Ziel gleicher Spawn-Quadratbereich und Dimension, Enderit-Launchschutz. | Fehlerkorrektur behalten; `safeSpawnElytrasAndEnderiteLaunchesPreventFallDamage`, `theSpawnAreaLiesOnlyInTheSpawnDimensionAndFallProtectionCoversAllOfIt`. |
| Erstbeitritt | Quelle Tag `simpletweaks.first_join`, ein Configwert (Default1) fuer beide Geschenke; Quell-README sagt teils nur ein Pad. Ziel respektiert Alttag, getrennte Mengen Default0, Schalter je Familie, legacy1 wird Default0, sonst clamp0..64 fuer beide. | Besitzer-Aenderung, nicht erneut verschenken; Erstbeitritts-/Migrations-/Persistenztests. |
| Exakter Respawn / Weltspawn | Quelle forceExact=true und eigener Weltspawn beim Laden. Ziel forceExact=false, eigener Weltspawn nur mit `useCustomWorldSpawn=false` als Default und explicit an; Bettmitte aus RespawnConfig, kein Nebeneffekt bei anderen Configbefehlen. | Bestehende Welten respektieren; Bett-/Weltspawn-/Befehlsnebeneffekttests. |
| Nether-/End-Sperren | Quelle Mixin auf Dimensionswechsel. Ziel sperrt nicht Command-Teleports, kein Sperrmeldungsspam; sonst vorhandene Funktionen. | Absichtliche Admin-Ausnahme behalten; `theNetherAndTheEndCanBeLockedByConfig`, `commandTeleportsPassTheDimensionLockAndTheLockMessageWaits`. |
| Laserpointer | Quelle kosmetischer Punkt, UUID/Koordinaten ungeprueft an ganze Dimension, Renderer/Entfernung; maxDamage500 ohne laufenden Verbrauch. Ziel Resonanzstab `amethyst_lens`, neue Rezept-/Ladungs-/Wirkungs-/Anvil-Mechanik, echte serverseitige Blickstrahlen; cosmetic relay max12/s, 128B Empfaenger, UUID ersetzt, finite/range geprueft. | Besitzer-Neuentwurf behalten; alle `theLens*`, `theRod*`, Relay-/PvP-/Adventure-/TNT-/Portal-/Aufladetests. Modul alias nur alte Item-ID; alte Pakete werden nicht akzeptiert. |
| Laser showLine | Quelle deklariert, nirgends ausgewertet; Ziel ebenfalls inaktive/excluded Altoption. | Nicht als Funktion ausliefern oder im Modul anzeigen. |
| Echo Compass aus libs | Quelle bindet `libs/echo-compass-v1.1.0.jar` ein, kein eigener Java-Gegenstand; Library-Metadata bestaetigt AGPL-3.0-or-later-Datapack. Ziel eigener neu geschriebener `echo_sounder`, 3s/6s Hold, Ladung1500, Mending/Unbreaking, keine Perle. | Keine Fremd-JAR/AGPL-Implementierung kopieren. Alte externe Library-Daten werden **nicht** durch dieses Modul umgeschrieben; ihr vanilla:compass bleibt ladbar, ohne Library-Teleport. Eine automatische Echo-Sounder-Migration ist eine separate Entscheidung. Echo-Sounder-Bestandsregressionen laufen mit. |
| XP-Clumping / Sofortpickup | Quelle addiert Einzelwerte ohne Vanilla `pickingCount`, alle20 Ticks/R2, kein Short-Cap; Ziel count einbezogen, Short.MAX_VALUE, resetCount1, Restkugeln nicht verlieren. Radius konfigurierbar aber ohne harte obere Schranke. | XP-Erhaltungskorrektur behalten; `xpOrbsClumpWithoutLosingExperience`. Radius-Haertung offen. |
| XP-Skalierung | Quelle und Ziel exakt min(3, 1+Wert/500); Client-RenderState/IOrbValue wird zu OrbValueHolder und 26.3 submit. | Formel beibehalten, nicht nochmals mixen; subjektive Darstellung nicht durch Servertests bewiesen. |
| Raketen-Stapel | Quelle config rocketStackSize64 + ItemStack-Mixin; Ziel serverSync, nur Verringerung Vanilla-Grenze, ungueltige Grenze erhoeht Vanilla nicht. | Nicht duplizieren; `theRocketStackSizeFollowsTheConfig`, Sync-/Configtests. |
| /killboats, /killcarts | Quelle standard/empty/all, configurable enable true/false, OP. Ziel gleichnamige Befehle, OP-Schalter und Containerinhalt korrekt gedroppt, Config-Radius100. | Kein zweiter Befehlsbaum. Modus-/Contents-/Operator-/Schaltertests; Radius-Cap offen. |
| /simpletweaks config & ModMenu/Cloth | Quelle eigene Datei und Unterbaeume. Ziel `/simplebuilding tweaks`, ausser Ziele2..4 entfallen; Config-Namen/Defaults/Tooltips/Tabs zentral. | Nicht erneut anbieten oder zweite Quelle von Gameplaywerten schaffen; `ConfigOptionTests` und Tweaks-Config-/Synctests. |
| Claims | Quelle `ClaimState`, `ClaimProtectionHandler`, `ClaimDeedItem`, /claim-Unterbaum; nicht in SB vorhanden. Details/Sicherheitsluecken unten. | Bisherige ausdrueckliche Zurueckstellung beibehalten; Modul liefert **nur inaktive Datenerhaltung**. |
| ModEntities, Lang-/Texturreste, TODO | Leere ModEntities; money_items, vaultCooldownDays, autowalk, brick_snowball, alte Texturen ohne aktive Referenz. TODO Kompost/Wachs/Werfer/Chatrechner/Questbuch/Extra-Inventar ohne Umsetzung. | Keine neue Funktion aus toten Texten/Ideen erfinden. Andere Module koennen separat daran arbeiten. |

## Alte Registry-IDs und Daten

Quelle hat 20 Bloecke mit Blockitems, zwei weitere aktive Nicht-Claim-Items und Claim-Urkunde,
sieben BE-Typen, vier Komponenten. Keine eigenen Entities, Enchantments oder eigenen KeyMappings;
Boost benutzt Vanillas Sprungtaste. Kein separates neues Kreativtab (Tools-Tab in Quelle).
Das Modul fuegt keine eigene Kategorie hinzu: die Urkunde ist eine nicht craftbare Altlast.

Alle Namen unten unter `simpletweaks:`. Die Bruecke loest nur fehlende Namen auf, nur in exakt
vier Registries. Nicht passende Registries/Namensraeume/unbekannte Namen bleiben unangetastet.
Die Gegenprobe fuer jeden Namen prueft Identifier-, ResourceKey-, Holder-, Value- und
containsKey-Lookups und die kanonische Rueckspeicherung. Existierende Eintraege gewinnen.

| Quell-Block/Blockitem | Ziel |
|---|---|
| `spawn_teleporter` | `simplebuilding:spawn_teleporter` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `spawn_teleporter_tier_2` | `simplebuilding:spawn_teleporter_tier_2` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `spawn_teleporter_tier_3` | `simplebuilding:spawn_teleporter_tier_3` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `spawn_teleporter_tier_4` | `simplebuilding:spawn_teleporter_tier_4` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `launchpad` | `simplebuilding:launchpad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `diamond_pressure_plate` | `simplebuilding:diamond_pressure_plate` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `netherite_pressure_plate` | `simplebuilding:netherite_pressure_plate` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `elytra_pad` | `simplebuilding:elytra_pad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `reinforced_elytra_pad` | `simplebuilding:reinforced_elytra_pad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `netherite_elytra_pad` | `simplebuilding:netherite_elytra_pad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `fine_elytra_pad` | `simplebuilding:fine_elytra_pad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `flypad` | `simplebuilding:flypad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `reinforced_flypad` | `simplebuilding:reinforced_flypad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `netherite_flypad` | `simplebuilding:netherite_flypad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `stellar_flypad` | `simplebuilding:stellar_flypad` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `chunk_loader` | `simplebuilding:chunk_loader` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `copper_pressure_plate` | `simplebuilding:copper_pressure_plate` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `exposed_copper_pressure_plate` | `simplebuilding:exposed_copper_pressure_plate` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `weathered_copper_pressure_plate` | `simplebuilding:weathered_copper_pressure_plate` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |
| `oxidized_copper_pressure_plate` | `simplebuilding:oxidized_copper_pressure_plate` (gleicher Eintrag, ggf. bestehende SB-Tiermigration) |

- `spawn_elytra` -> `simplebuilding:spawn_elytra`; `laser_pointer` -> `simplebuilding:amethyst_lens`.
- `claim_deed` bleibt `simpletweaks:claim_deed`, eigenes inaktives Item, max16.
- BE: `spawn_teleporter_be`, `launchpad_be`, `elytra_pad_be`, `flypad_be`, `chunk_loader_be`,
  `copper_pressure_plate_be`, `netherite_pressure_plate_be` -> gleicher Pfad unter SimpleBuilding.
- Komponenten `flight_time`, `boost_level`, `last_pad_tick`, `is_safe_elytra` -> gleiche Pfade unter SB.
  Die existierenden Value-Codecs bleiben zustaendig; die Bruecke erfindet keine Bonuswerte.
- Quelle BE-NBT `Owner` (UUID als Intarray) und Launchpad `Charges` werden erhalten; Ziellogik
  wendet nach dem Laden die abgenommenen Tiers an (Launchpad-Ueberschuss wird ausgeworfen).
- Urkunden-`custom_data`: `ClaimPos`, `OwnerName`, `FriendNames`, auch unbekannte Felder bleiben erhalten.
  Der Tooltip ist statisch/kurz; gefaelschte Besitzer-/Gastdaten sind niemals Autoritaet.
- Claims: per-Dimension SavedData `simpletweaks_claims`, Map mit Chunk-Longs als String-Schluessel,
  `Owner` UUID-String, `Whitelist` Liste von UUID-Strings. Quelle kann alte Dateien lesen;
  dieses Modul greift nicht auf sie zu. Alte Protektion ist folglich **nicht aktiv**.
- Noch keine vollstaendige echte 1.21.11-Welt geoeffnet. Tests beweisen gezielte Codec-/Registry-
  und BE-Ladewege auf 26.3; nicht jedes Vanilla-Upgrade einer beliebigen Quellwelt.
- Alte Rezept-/Advancement-IDs werden nicht als Zweitrezepte registriert. Entdeckte alte
  Rezeptbuchfortschritte duerfen dadurch verschwinden; aktuelle Rezepte ersetzt Vanilla beim Laden.
- Config `config/simpletweaks.json` wird nicht gelesen/geaendert. Kein automatisches Ueberschreiben
  vorhandener SimpleBuilding-Servereinstellungen. Werte werden im bestehenden SB-Configbereich gepflegt.

## Rezepte und Bezugsquellen: kompletter Quellkatalog

Keine der folgenden bereits uebernommenen Quellen wird im Modul ein zweites Mal hergestellt.
Ausnahme Claim-Urkunde: kein Quell-Rezept; source Tools-Creative-/give, hier bestehende Welt oder /give.
Laserpointer und Spawn-Elytra haben ebenfalls kein Quell-Datagen-Rezept; Spawn-Elytra kommt durch
Spawn/Pad, Pointer durch Creative/give. Kupferstufen entstehen auch durch Oxidation/Axt.
Alle Quell-Block-Loot-Tabellen sind einfache self drops; neue SB-Lootlogik erhaelt Zusatzdaten.
19 Quell-Rezepte sind vollstaendig als JSON im Audit enthalten; nachstehend Inhaltszusammenfassung:

| Quellrezept(e) | Quellzutaten | Heutige Entscheidung |
|---|---|---|
| diamond_pressure_plate | DD Diamant | unveraendert vorhanden |
| netherite_pressure_plate_smithing | Netheritvorlage + Diamantplatte + Netheritbarren | unveraendert vorhanden |
| copper_pressure_plate | CC Kupferbloecke | bestehendes SB-Rezept/alle Varianten; nicht doppeln |
| elytra_pad_smithing | beliebige Quellvorlage + Diamantplatte + Diamant | heutige Quelle ersetzt durch Platte + Elytra (Besitzer) |
| reinforced_elytra_pad_smithing | beliebige Vorlage + I + Diamantblock | Ziel-Aufwertung kostet Diamantplatte |
| netherite_elytra_pad_smithing | Netheritvorlage + II + Netheritbarren | Ziel kostet Netheritplatte |
| fine_elytra_pad_smithing | Netheritvorlage + III + Netherstern | heute V nach neuem IV; Besitzerkette |
| flypad_tier1_smithing | Netheritvorlage + Fine-Elytra-Pad + Netheritbarren | neue Enderit-Flypad-Kette |
| flypad_tier2_smithing | Netheritvorlage + I + Netheritblock | neue Enderit-Flypad-Kette |
| netherite_flypad_crafting | DBD/ESE/KFK: Diamantbloecke, Netheritblock, verzauberte Goldaepfel, Netherstern, Ominous Trial Keys, II | Legacy-Netherit -> heutiges II; kein zweites Rezept |
| stellar_flypad_crafting | KKK/ESE/FFF: drei Keys, zwei verzauberte Goldaepfel, Netherstern, drei III | heutiges III aus zweimalII; Besitzerkette |
| spawn_teleporter_smithing, _alternative | beliebige Vorlage + leichte Platte + Diamantblock; alternativ Netheritvorlage + leichte Platte + Barren | heute Einstieg mit Endermankopf |
| spawn_teleporter_tier2/3/4_smithing | jeweils Netheritvorlage + vorige Stufe + Barren | drei Zielstufen, Platten als Aufwertung, Legacy-Tiers |
| launchpad_smithing, _alternative | beliebige Vorlage + schwere Platte + Diamantblock; alternativ Netheritvorlage + schwere Platte + Barren | heutiger Einstieg Eisenkern und dreistufige Aufwertung |
| chunk_loader_smithing | Netheritvorlage + Kupferplatte + Netheritbarren | heutiger Einstieg Kupferkern und dreistufige Aufwertung |

Die Quelle schreibt ausser diesen Rezept-Freischaltungsadvancements keine eigene aktive
Spielprogression. Ziel hat eigene Guides/JEI/Advancement-Hinweise und neue geheime Kette;
sie bleiben bei SimpleBuilding. Die Quell-Tags `minecraft:trimmable_armor` (Platten/Pads) und
`minecraft:trim_materials` (Diamantblock/Netherstern) werden absichtlich nicht uebernommen.

### Echo-Library separat geprueft

`libs/echo-compass-v1.1.0.jar`: SHA256
`796f62849734432e76e16900ba73dd7d4bd610d2ef470eb0436f75b46a3a2923`, 40 ZIP-Eintraege,
Mod-ID `mr_echo_compass`, Lizenz `AGPL-3.0-or-later`, Datapack-Format80.
Inventar/Metadata/Rezept in `modules/simpletweaks/audit/echo-library-inventory.json`.
Rezept `echo_compass:echo_compass`: Enderauge + Bergungskompass + Echoscherbe, shapeless,
Ergebnis **minecraft:compass** mit custom_data.echo_compass=true, max_damage64, max_stack_size1,
Glint, Name/Lore. Link via item_used_on_block-Advancement setzt consumable und
custom_data.echo_compass.dimension (overworld/the_nether/the_end); Gebrauch benoetigt
Enderperle, sperrt bei Nausea, zieht Schaden direkt per Itemmodifier ab und teleportiert
ueber Funktionen/storage. Daher kein Unbreaking, keine beliebigen Moddimensionen,
und Perle kann beim spaeter fehlgeschlagenen Sprung bereits verbraucht sein.
Scoreboards x/y/z/damage/max_damage und function-load-Tag sind Library-Daten; kein eigenes
Block-/Item-/Entity-Registryobjekt. Ziel-Echolot-Tests verifizieren den absichtlichen Neubau,
keine zweite Library-Registrierung oder alte Funktionen. Das JAR wurde nicht ausgeliefert.

## Jede Quell-Configoption

Quelle AutoConfig/Gson `simpletweaks`; Ziel `simplebuilding`/`tweaks`.
Die alten fachlichen Feldnamen wurden uebernommen, mit den bereits abgenommenen Ausnahmen unten.
Rein kosmetische XP-/Laseroptionen sind Clientwerte; alle Gameplayentscheidungen erfolgen im
SB-Servercode. Keine neuen Spieleinstellungen im Kompatibilitaetsmodul, deshalb auch kein leerer
Configscreen, keine Scheintabs oder duplizierten Defaults. `balance/simpletweaks/module.json`
beschreibt diese leere Gameplayoptionsmenge und den festen Stackwert16. Alle Producerpfade
und Abhaengigkeiten stehen im Manifest; keine Migration bestehender SB-Balance-Versionen.

| Alte Option | Quell-Default | Ziel / Entscheidung |
|---|---|---|
| `rocketStackSize` | `64` | `tweaks.balancing.rocketStackSize`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `allowNether` | `true` | `tweaks.dimensions.allowNether`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `allowEnd` | `true` | `tweaks.dimensions.allowEnd`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `forceExactSpawn` | `true` | `tweaks.spawn.forceExactSpawn`; Zieldefaultfalse, Besitzerentscheidung |
| `disableFallDamageInSpawn` | `true` | `tweaks.spawn.disableFallDamageInSpawn`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `xCoordSpawnPoint` | `0` | `tweaks.spawn.xCoordSpawnPoint`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `yCoordSpawnPoint` | `-1` | `tweaks.spawn.yCoordSpawnPoint`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `zCoordSpawnPoint` | `0` | `tweaks.spawn.zCoordSpawnPoint`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `spawnTeleporterCount` | `1` | `tweaks.spawn.firstJoinTeleporterCount / firstJoinElytraPadCount`; Default0/0, legacy1 ->0, sonst0..64 fuer beide |
| `giveElytraOnSpawn` | `false` | `tweaks.spawn.giveElytraOnSpawn`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `spawnElytraRadius` | `25` | `tweaks.spawn.spawnElytraRadius`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `useWorldSpawnAsCenter` | `false` | `tweaks.spawn.useWorldSpawnAsCenter`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `customSpawnElytraX` | `0` | `tweaks.spawn.customSpawnElytraX`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `customSpawnElytraZ` | `0` | `tweaks.spawn.customSpawnElytraZ`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `flightTimeSeconds` | `300` | `tweaks.spawn.flightTimeSeconds`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `maxBoosts` | `3` | `tweaks.spawn.maxBoosts`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `boostStrength` | `0.6f` | `tweaks.spawn.boostStrength`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `spawn1X` | `0` | `tweaks.spawn.spawn1X`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `spawn1Y` | `-1000` | `tweaks.spawn.spawn1Y`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `spawn1Z` | `0` | `tweaks.spawn.spawn1Z`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `spawn2X` | `0` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn2Y` | `-1000` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn2Z` | `0` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn3X` | `0` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn3Y` | `-1000` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn3Z` | `0` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn4X` | `0` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn4Y` | `-1000` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `spawn4Z` | `0` | `entfallen`; Besitzer: ein Teleporterziel; nicht doppelt anbieten |
| `enableKillBoatsCommand` | `true` | `tweaks.commands.enableKillBoatsCommand`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `enableKillCartsCommand` | `false` | `tweaks.commands.enableKillCartsCommand`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `enableXpClumps` | `true` | `tweaks.optimization.enableXpClumps`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `scaleXpOrbs` | `true` | `tweaks.optimization.scaleXpOrbs`; uebernommen; heutige Grenzen/Risiken siehe unten |
| `enable` | `true` | `tweaks.laserPointer.enable`; visuals.laserPointer -> laserPointer |
| `color` | `0xFF0000` | `tweaks.laserPointer.color`; visuals.laserPointer -> laserPointer |
| `scale` | `0.25f` | `tweaks.laserPointer.scale`; visuals.laserPointer -> laserPointer |
| `range` | `512` | `tweaks.laserPointer.range`; visuals.laserPointer -> laserPointer |
| `showLine` | `false` | `tweaks.laserPointer.showLine`; visuals.laserPointer umbenannt; showLine inaktiv/excluded |

Zusaetzliche Zieloptionen (nicht in Quelle): acht Familien-Schalter; `padTuning` mit drei
Teleporterzeiten, Launchmultiplikator, Potion-Aufladeschritt und Cooldownfaktor; Weltspawn-Schalter;
Killradius; XP-Clump-Radius; Laser-Ladungsverbrauch/Effektkosten; Echo-Sounder-Cooldowns;
`server.pads` fuer Loader/Abbau und weitere zentrale SB-Optionen.
`ConfigOptionTests.everyOptionHasNameTooltipAndTab`, `everyConfigOptionKeepsItsPersistedNameAndDefault`,
`theConfigCommandReachesEveryOption` und Verhaltenstests werden im Bestandslauf mitgeprueft.
Das ist kein Beweis fuer harte Caps, wo die Implementierung nur Untergrenzen besitzt.

## Befehle, Kollisionen und Mixins

Quellkommandos /killboats und /killcarts [standard|empty|all] sind bereits in SB registriert.
Quell-/simpletweaks Unterbaeume: spawn elytra toggle/radius/flightTime/maxBoosts/boostStrength/center
worldspawn|here|set; spawn teleporterCount; worldspawn forceExact/here/set/setspawn1..4;
dimension nether/end; balancing rocketStackSize; commands enableKillBoats/enableKillCarts.
Heute derselbe fachliche Baum unter `/simplebuilding tweaks`, getrenntes elytraPadCount,
worldspawn custom und nur setspawn1. Das Modul registriert keinen dieser alten Befehle.

Claim-Quelle: `/claim` zeigt aktuellen Chunk; `/claim trust <player>`, `/claim untrust <player>`
aendern die Owner-Whitelist; `/claim admin unclaim`, `listall`, `list <player>` (OP4), Listen
max50/all bzw10/player. Namen online und aus Cache; /claim selbst braucht kein OP.
In Quelle kann nur die Urkunde claimen; kein Owner-/claim unclaim-Befehl. OP2 umgeht Protektion.

Die Quellmixins DimensionBlock/ExactSpawn greifen ServerPlayer-Wechsel/Respawn an,
ExperienceOrb/Invoker XP, ItemStack Raketenstacks, SpawnElytraDamage LivingEntity-Schaden;
Client ExperienceOrbRenderer/RenderState, InGameHudLevel/Bar/Crosshair und Teleporterpartikel.
SB hat diese Wirkungen schon in den eigenen getrennten Mixins. Zweites Laden haette doppelte
XP-/Stack-/HUD-/Schadenwirkung bzw. Mixin-Kollisionen. Keine dieser Klassen wurde kopiert.
Das Modul hat genau einen Registry-RETURN-Mixin; gleicher Hook wie SBs Rename-Mixin,
aber disjunkter Quellnamensraum (`simpletweaks` gegen `simplebuilding`) und eigene Methodenprefixe.
Beide Loader starten mit beiden Mixins und Tests pruefen die echte Aufloesung. Keine `ModifyConstant`-
Kollision, kein neuer eigener Netzwerkkanal oder Keybinding. Der alte Modjar darf nicht parallel
zum Ersatzmod installiert werden (derselbe Mod-ID); die Source-Mod darf nicht mit allen Features
zusammen mit SimpleBuilding benutzt werden.

## Sicherheitsbefunde und Entscheidungen

**Neue Bruecke:** konstante Allowlist20 Bloecke/22 Items/7BE/4Komponenten; keine beliebigen
Zielnamen, keine Aliaszyklen, keine Registrierungen derselben Pads. Vanilla entscheidet Gameplay;
keine Clientdaten/API koennen Claims anlegen, Rechte vergeben, Entitaeten oder Chunks spawnen.
Falsche Registry/Namensraum/unbekannte IDs liefern keine Aliasantwort. Deedbenutzung ist PASS;
NBT bleibt nur Anzeige-/Archivdaten, keine Authority. Keine neue Arbitrage/Handels-/Loot-/Rezept-
Quelle, keine Bewegungs-/Reach-Boni. Kein stiller Eingriff in Besitzer-Config oder Claimdateien.

**Schon vorhandenes SimpleBuilding, kein duplizierter Fix im Modul:**

1. `TweaksNetwork.handleBoost` liest `spawn.boostStrength` direkt; `TweaksConfig.validate`
   begrenzt diesen Wert nicht. Der spezielle Configbefehl hat nur min0,1, keine Max-Grenze.
   Es fehlt ein absolutes Velocitycap und ein Boost-Ratefenster. Refill im Spawn/Pad plus
   Paketspam koennte die Beschleunigung wiederholt nutzen. Entscheidung: eigener SB-Fix
   mit finite Staerke, Obergrenze, Velocitycap, Rate-/Budgetlimit und echten Paket-Gegenproben.
2. `XpClumping.radius` und `padTuning.launchpadStrengthFactor` haben finite/Untergrenzen,
   aber keine harte Max-Grenze; extreme Serverconfigs koennen riesige Scans/Schub ausloesen.
   Entscheidung: eigene harte SB-Caps plus Grenz-/Runtime-Tests, keine neue XP-/Launch-Logik hier.
3. `killCommandRadius` sowie Spawnradius sind nicht praktisch nach oben begrenzt; Teleporter-/
   Potionzeit und Cooldownfaktoren koennen extrem sein. Entscheidung: im SB-Config-Haertungsrun
   Wertebereich mit Vanilla-gerechten Caps und Default-Tooltip synchronisieren.
4. Cosmetic Laser-Relay ist rate-/UUID-/finite-/distanzgesichert; Gameplayblickstrahl wird selbst
   serverseitig bestimmt. `range` fuer cosmetic Relay hat kein praktisches Maxcap; reale
   Wirkung ist durch Sichtweite/Entityrange begrenzt. Entscheidung: SB-Range-Haertung getrennt.
5. Bekanntes Bestandsproblem `RecipeFilter`: Laser-Schalter prueft alte laser_pointer-Rezept-ID
   statt amethyst_lens (HANDOFF). Entscheidung: bereits offener SB-Fix, hier keine
   fremden Rezept-/Configdateien aendern.

**Claim-Quelle, deshalb kein sicherheitsfertiger Port:** keine Claims-/Whitelist-/Globalcaps,
keine Costs/Cooldown, keine Weltgrenzen-/Spawnpruefung bei Claim; OP2-Bypass pauschal, mutable
Whitelist-Getter. UseBlock prueft nur geklickten Chunk, nicht den angrenzenden Bauzielchunk.
Keine vollstaendige Absicherung von Eimern/Fluessigkeiten, Feuer, Explosionen, Kolben,
Hoppertransfer, Projektilen/indirektem Schaden oder anderen Modwerkzeugen. Vier Fabriccallbacks
sind kein vollstaendiger Claim-Schutz. Trust/Untrust offline/cache, Admin-Schalter und Bypass-
Policies brauchen klares Design. `ClaimDeedItem` greift aus Shared auf Client-/GLFW-APIs zu,
schreibt englischen Bildschirmtext und kann freie unbegrenzte Claims erzeugen; Quelle-TODO
selbst nennt fehlenden Land-Schutz. Allein Codec-Fixes der claims-only Branch machen es nicht sicher.

Bei spaeterer Freigabe braucht der Claimport mindestens: serverseitige Enable-/MaxClaims/
MaxTrusted-/Globalcaps und Cooldown; explizite OP-/Dimension-/Spawn-/Weltgrenzenpolitik;
atomic Persistenz und unveraenderliche Whitelist-Sichten; Permissions an **jedem Ziel** von
Baustab/Hammer/Beam/Teleporter sowie Vanilla Use/Break/Place/Entity/Bucket/Automation;
Explosion/Fire/Piston/Fluid-/Remote-Angriffspruefungen, Quellenwelt-Fixtures und 2-Spieler-Tests.
Alle Optionen brauchen EN/DE-Name/Tooltip/Tab/Default und wirksame harte Caps. Bis dahin
bleiben Claim-NBT und Urkunden Archivdaten, nicht ein Sicherheitsversprechen.

## Dateiweise Vollstaendigkeit

Der Snapshot listet jede der 56 Dateien einschliesslich Hash/Methoden. Diese Tabelle nennt die
Implementierungs-/Infrastrukturzuordnung (nicht jede Klasse ist eine eigenstaendige Funktion).

| Quell-Java (com/simpletweaks/) | Ziel-Datei(en) oder Disposition |
|---|---|
| `block/custom/ChunkLoaderBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/ChunkLoaderBlock.java` |
| `block/custom/CopperPressurePlateBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/CopperPressurePlateBlock.java` |
| `block/custom/DiamondPressurePlateBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/DiamondPressurePlateBlock.java` |
| `block/custom/ElytraPadBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/ElytraPadBlock.java` |
| `block/custom/FlypadBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/FlypadBlock.java` |
| `block/custom/LaunchpadBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/LaunchpadBlock.java` |
| `block/custom/NetheritePressurePlateBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/FilterPressurePlateBlock.java` |
| `block/custom/SpawnTeleporterBlock.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/SpawnTeleporterBlock.java` |
| `block/entity/ChunkLoaderBlockEntity.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/ChunkLoaderBlockEntity.java` |
| `block/entity/CopperPressurePlateBlockEntity.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/CopperPressurePlateBlockEntity.java` |
| `block/entity/ElytraPadBlockEntity.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/ElytraPadBlockEntity.java` |
| `block/entity/FlypadBlockEntity.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/FlypadBlockEntity.java` |
| `block/entity/LaunchpadBlockEntity.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/LaunchpadBlockEntity.java` |
| `block/entity/ModBlockEntities.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/TweaksBlockEntities.java` |
| `block/entity/ModEntities.java` | empty |
| `block/entity/NetheritePressurePlateBlockEntity.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/FilterPlateBlockEntity.java` |
| `block/entity/SpawnTeleporterBlockEntity.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/SpawnTeleporterBlockEntity.java` |
| `block/ModBlocks.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/TweaksBlocks.java` |
| `client/gui/SpawnElytraTimerOverlay.java` | `common/src/shared/java/com/simplebuilding/tweaks/client/SpawnElytraHud.java` |
| `client/IOrbValue.java` | `common/src/shared/java/com/simplebuilding/tweaks/client/OrbValueHolder.java` |
| `client/render/LaserRenderer.java` | `common/src/shared/java/com/simplebuilding/tweaks/client/LaserRenderer.java` |
| `client/SpawnElytraClient.java` | `common/src/shared/java/com/simplebuilding/tweaks/client/TweaksClient.java` |
| `command/ModCommands.java` | `common/src/shared/java/com/simplebuilding/tweaks/command/TweaksCommands.java` |
| `compat/ModMenuIntegration.java` | `src/main/java/com/simplebuilding/compat/ModMenuIntegration.java` |
| `component/ModDataComponentTypes.java` | `common/src/shared/java/com/simplebuilding/tweaks/component/TweaksComponents.java` |
| `config/SimpletweaksConfig.java` | `common/src/shared/java/com/simplebuilding/tweaks/TweaksConfig.java` |
| `datagen/ModItemTagProvider.java` | `src/main/java/com/simplebuilding/datagen/ModItemTagProvider.java` |
| `datagen/ModLootTableProvider.java` | `src/main/java/com/simplebuilding/datagen/ModLootTableProvider.java` |
| `datagen/ModModelProvider.java` | `src/main/java/com/simplebuilding/datagen/ModModelProvider.java` |
| `datagen/ModRecipeProvider.java` | `src/main/java/com/simplebuilding/datagen/ModRecipeProvider.java` |
| `event/ClaimProtectionHandler.java` | deferred-claims |
| `event/FirstJoinHandler.java` | `common/src/shared/java/com/simplebuilding/tweaks/spawn/SpawnSetup.java` |
| `event/SpawnHandler.java` | `common/src/shared/java/com/simplebuilding/tweaks/spawn/SpawnElytra.java` |
| `event/WorldSpawnHandler.java` | `common/src/shared/java/com/simplebuilding/tweaks/spawn/SpawnSetup.java` |
| `item/custom/ClaimDeedItem.java` | deferred-claims |
| `item/custom/LaserPointerItem.java` | `common/src/shared/java/com/simplebuilding/tweaks/item/LaserPointerItem.java` |
| `item/custom/SpawnElytraItem.java` | `common/src/shared/java/com/simplebuilding/tweaks/item/SpawnElytraItem.java` |
| `item/ModItems.java` | `common/src/shared/java/com/simplebuilding/tweaks/item/TweaksItems.java` |
| `mixin/client/ExperienceOrbRendererMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/client/ExperienceOrbRendererMixin.java` |
| `mixin/client/ExperienceOrbRenderStateMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/client/ExperienceOrbRenderStateMixin.java` |
| `mixin/client/InGameHudLevelMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/client/HudExperienceLevelMixin.java` |
| `mixin/client/InGameHudMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/client/ExperienceBarMixin.java` |
| `mixin/client/InGameScreenHudMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/client/LaserRenderer.java` |
| `mixin/client/SpawnTeleporterClientMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/block/entity/SpawnTeleporterBlockEntity.java` |
| `mixin/DimensionBlockMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/TweaksServerPlayerMixin.java` |
| `mixin/ExactSpawnMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/TweaksServerPlayerMixin.java` |
| `mixin/ExperienceOrbInvoker.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/ExperienceOrbAccessor.java` |
| `mixin/ExperienceOrbMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/ExperienceOrbMixin.java` |
| `mixin/ItemStackMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/TweaksItemStackMixin.java` |
| `mixin/SpawnElytraDamageMixin.java` | `common/src/shared/java/com/simplebuilding/tweaks/mixin/SpawnElytraDamageMixin.java` |
| `network/LaserManager.java` | `common/src/shared/java/com/simplebuilding/tweaks/network/TweaksNetwork.java` |
| `network/SpawnElytraNetworking.java` | `common/src/shared/java/com/simplebuilding/tweaks/network/TweaksNetwork.java` |
| `Simpletweaks.java` | `common/src/shared/java/com/simplebuilding/tweaks/SimpleTweaks.java` |
| `SimpletweaksClient.java` | `common/src/shared/java/com/simplebuilding/tweaks/client/TweaksClient.java` |
| `SimpletweaksDataGenerator.java` | `src/main/java/com/simplebuilding/SimplebuildingDataGenerator.java` |
| `world/ClaimState.java` | deferred-claims |

## Verifikation und verbleibende Ports

- Modulkatalog: 12 GameTests je Loader mit SimpleBuilding mitgeladen: Boot/Duplikatfreiheit,
  alle Registry-Zugaenge, Item-/Block-/BE-Ladung, Owner, Charges, Stapel64, Komponenten,
  inaktive Urkunde/NBT, Hopperintegration, Allowlist, fehlende alte Befehle.
- Eigener Datenhook: 56-Dateien-Snapshot, EN/DE-gleiche eindeutige Keys, Modelle/Textur, Wiki,
  Runtime-Registryexport, kein interner SB-Import und genau eine Registrierung.
- Quelle hat keine Tests zu portieren. Wiederpruefung aller bereits portierten Features erfolgt
  durch den aktuellen **vollstaendigen** bestehenden 26.3-Serverlauf, nicht alte Handoff-Zahlen.
- Fabric-Modulclient: beide Mods Boot, Titel -> echte Welt, kanonische Alias-ID im Client,
  Legacy-Deed im Inventar gerendert, drei Screenshotpunkte. Kein SB-Clientsuite-Run.
- NeoForge-Client, echte hochgestufte Quellwelt, Vanilla-Upgrade aller NBT-Konstellationen,
  subjektive Klaenge, deutsche Screenshots, echte JEI/Jade-Bedienung und Besitzerwelt offen.
- Forge 26.3 hat jetzt einen experimentellen Opt-in-Adapter; Details unten.
- 26.2/1.21.11/26.4 erst nach Besitzer-Releaseentscheidung; keine Quelltexte dieser Linien geaendert.
- Launch Hub entdeckt Mods und Tests aus dem Manifest. Kein shared Wiringblock geaendert;
  enabled-mods ist die vom Scaffold aktualisierte Integrationsauswahl.
- Testzentrale wird durch den vollstaendigen SB-Bestandslauf in beiden isolierten Testwelten
  aufgebaut und auf Item-/Block-Abdeckung geprueft; die bewusst nicht kreative Legacy-Urkunde
  hat ihren eigenen Modul-Registry-/Daten-/Inventartest. Besitzerwelt bleibt unberuehrt.

Aktuelle Run-IDs/Gate-Ausgaenge werden nach Abschluss unten ergaenzt.

### Aktuelle Belege

- Finale Serversuite: **1587/1587, alles gruen**, 2026-09-30T16-46-21Z-da1c;
  781 Fabric +781 NeoForge Bestand, 1 Integration, 12+12 Modul. Keine roten/ausgelassenen Tests innerhalb dieser Ziele.
- Fabric-Client: **3/3, alles gruen**, 2026-09-30T16-34-44Z-6351; Bilder unter
  `modules/simpletweaks/previews/000{0,1,2}_simpletweaks-*.png`; Inventarbild und
  16x-Alt/Neu-Texturvorschau angesehen. Nachherige Java-Klassenumbenennung aendert keine Clientfunktion.
- Beide Wiki-Varianten generate/--check und --all/--all --check Exit0; Datenhook gueltig.
  Buecher: problems0, Texturen470 +9 mcmeta aktuell, Multimodregistries valid.
- Source-HEAD, urspruenglicher dirty-Status und alle56 Java-Hashes nach der Arbeit unveraendert.
- Anfangslauf hatte noch ungueltige Integrationsauswahl; nach fertiger Manifestregistrierung gruen.
  Erste Mod-Gegenprobe fand einen Testfehler (Container.removeItem leert Referenz); vor Vergleich
  Itemidentitaet sichern, finale echte Lagerungspruefungen auf beiden Loadern gruen.

- Abschliessendes Worktree-Gate `gradlew.bat check -q --no-daemon`: **GRADLE_EXIT=0**, Ausgabe gelesen, einschliesslich shared/26.2-Kompilierung, Modul-Daten, Wiki/Balancing/Atlas/Jade und Integrations-/Client-Harness-Kompilierung.

## Experimentelles Forge 26.3

Mit `-Pforge263=true`; SimpleBuilding ist verpflichtend und wird vorher geladen. Native ForgeRegistry-Aliase und ein eigener NamespacedWrapper-Mixin ergänzen die gemeinsamen MappedRegistry-Lookups. Alle 12 kanonischen Fälle prüfen echte alte Item-/Block-/BE-/Komponenten-Codecs, Owner/Charges/Stacks, Canonical-Saves und Verweigerung unbekannter IDs. Keine zusätzlichen Config-/Netzwerk-/Client-Hooks nötig: das Modul besitzt keine davon; die Urkunde nutzt Vanilla-Itemdarstellung. Claims bleiben inaktiv. Forge-Client, echte alte Welt und Besitzerwelt sind nicht abgenommen.

Forge follow-up verification: 12/12 canonical cases passed in the combined run `2026-09-30T17-52-13Z-77a1`; **2828/2828, alles gruen** across existing Fabric/NeoForge/Forge 26.3, integration and all manifest module server suites. Explicit Forge compile without `forge_runs`: exit 0. Client and owner-world limits above remain open.

## Claims-Fortsetzung (codex-next-claims, 2026-09-30)

Claims bleiben standardmaessig AUS, auch nach weiteren Ausbaustufen. Taskplan:
`modules/simpletweaks/CLAIMS-PLAN.md`. Stufe 1 ist die Daten-/Befehlsgrundlage,
noch kein vollstaendiger Landschutz. Die Config-Tooltiptexte nennen den aktuellen Umfang.

Die dokumentierten lokalen Quellcommits bb8f976 und 9882906 sowie der lokale
Claims-only-Branch sind auf GitHub nicht verfuegbar; gezielte Fetch-Versuche wurden
abgelehnt. Die read-only gelesene oeffentliche 1.21.11-Quelle ist 739537ef0f250303be618dff204ccac64fdfff66.
Dateihashes: `modules/simpletweaks/audit/claims-source.json`. Der fruehere lokale
Audit bleibt als zusaetzlicher Formatbeleg erhalten. Kein Quellcode wurde dort geaendert.

- Serverdatei `config/simpletweaks-claims.json`, Neustart erforderlich; keine C2S-Konfiguration.
  Defaults: enabled=false, maxClaimsPerPlayer=16 (1..256), maxTrustedPlayers=8 (0..64),
  globalCap=4096 (1..10000), cooldownTicks=100 (20..72000), opBypass=false,
  spawnBuffer=16 (0..256), dimensions=Oberwelt/Nether/Ende (maximal 32 IDs).
- Neue Claims muessen vollstaendig in der Weltgrenze und ausserhalb des Spawnquadrats
  liegen. Vanilla-Spawnschutz wird mindestens eingehalten, auch fuer OPs. Die
  Dimensionsliste beschraenkt neue Claims, nicht den Schutz schon vorhandener Daten.
- Falls eingeschaltet, gilt ein Schutz-Bypass nur fuer OP4; er hebt keine Claim-Caps,
  Spawn- oder Weltgrenzenregeln auf. Befehlsadministration ist separat OP4.
- Globaler Datensatz `<world>/simpletweaks-claims.json`: Datei erst nach aktiviertem
  Zugriff lesen; atomarer Dateitausch und unveraenderliche Map-/Whitelist-Sichten.
  Schreiben erfolgt vor Veroeffentlichung der neuen Rechte. Bei Lesefehlern oder
  Schreibfehlern sperrt die aktivierte Runtime den Zugriff; Originaldateien bleiben erhalten.
- Alte per-Dimension `data/simpletweaks_claims.dat` werden beim ersten aktivierten
  Lesen importiert, falls noch kein globaler Datensatz existiert. Es wird dabei noch
  nichts geschrieben. Originale werden nie ersetzt; unbekannte/defekte Daten werden
  nicht zu einer leeren, beschreibbaren Claim-Welt umgedeutet. Der Import umfasst
  auch ungeladene eigene Dimensionen unter `dimensions/` (begrenzter Scan;
  bei mehr als 100000 Eintraegen wird sicher abgebrochen). Nach dem ersten atomaren Schreiben
  ist der globale Datensatz massgeblich. Alte Dateikopien bleiben als Archiv erhalten.
- Urkunden behalten alle CustomData-Felder unveraendert. Diese Felder sind niemals
  Autoritaet. Aktivierte Verwendung beantragt serverseitig einen Claim; Feedback nur
  Sound/Partikel, keine Chat-/Aktionsleistentexte. Kein neues Rezept oder neue Pixelkunst.
- Das Modul besitzt einen gemeinsamen Sprachressourcenort fuer beide Loader, keine
  separate 26.2-/26.3-Sprachkopie. Beide EN/DE-Dateien enthalten Name, Tooltip, Tab und
  Default fuer jede eigene Option; fremde SimpleBuilding-Sprachdateien bleiben unveraendert.

Neue Serverchecks liegen im eigenen ClaimsGameTest-Katalog. Bestehende zwoelf
Kompatibilitaetsfaelle bleiben erhalten; der bisherige Test fuer fehlende Claim-Befehle
prueft jetzt, dass der ausgeschaltete Claim-Baum unbenutzbar bleibt. Forge-Claims,
Forge-Katalogerweiterung, Clients, echte Altwelten und Besitzerwelt bleiben separat.
