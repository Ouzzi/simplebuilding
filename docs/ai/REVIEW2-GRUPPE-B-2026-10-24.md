# Review 2 – Gruppe B (24.10.2026, Branch b-rev2b24)

Auftrag Besitzer 24.10.: „Sub-Mods fertig machen: wirklich jede Datei einzeln scannen, ob sie vollständig ist oder ob etwas
fehlt oder Probleme entstehen könnten“ + Verbesserungsvorschläge. Nur 26.3 Fabric. Plan: `docs/ai/PLAN-B-REV2B-2026-10-24.md`.
Basis: REV24 (`docs/ai/REVIEW-CODE-2026-10-24.md`, dort behobene Punkte nicht erneut gemeldet) und AUDM24
(`docs/ai/AUDIT-MODULE-2026-10-24.md` auf b-audmods24; JEI → b-jei24, Config-Tooltips/Befehle → b-cmdcfg24).

Schwere: **P1** Absturz/Datenverlust/Exploit, **P2** Datenverlust im Einzelfall/Fehlfunktion mit Folgen, **P3** begrenzter Fehler
(Leck, Robustheit, falsches Verhalten am Rand), **P4** Vorschlag. Status: **behoben** (dieser Branch, mit Test wo prüfbar) oder
**Queue** (`.claude/QUEUE.md`, Abschnitt „Review 2 24.10. (REV2)“, Präfix REV2-B-).

## Vorgehen

- Je Modul alle Java-Dateien des gemeinsamen Codes und des Fabric-Teils gelesen (Logik, Mixins, Netz, Persistenz, Konfiguration,
  Tests auf „Defekt festhalten“-Erwartungen); NeoForge/Forge nur auf Bezüge geprüft (Auftrag: nur 26.3 Fabric).
- Skripte über alle 14 Module (Scratchpad, nicht eingecheckt): jede JSON-Datei parst; Fabric-Entrypoints und Mixin-Konfigs zeigen
  auf vorhandene Klassen, jede `@Mixin`-Klasse ist eingetragen; Lang en/de gleiche Schlüssel; IDs in Rezepten, Loot, Tags,
  Handelsdaten gegen Item-/Block-Definitionen aller Module. Ergebnis: keine fehlenden Klassen/Mixins, keine Lang-Lücken, keine
  falschen IDs (Treffer waren Rezept-/Trank-/Entity-IDs oder bewusste Alt-IDs, z. B. `guide_book_vanilla_*` als Legacy-Alias).
- Muster-Suche: statische Maps/Sets/ThreadLocals, `Files.writeString`, strenge `listOf()`-Codecs beim Laden, Payload-Handler,
  Server-Lifecycle-Hooks, Blockänderungen von Mobs (mobGriefing), Client-Caches mit Entity-/Level-Verweisen.

## Funde und Status

### simpleoctets
| Schwere | Fund | Status |
|---|---|---|
| P2 | `MixedOctetBlockEntity.loadAdditional` las die Palette mit strengem `BlockState.CODEC.listOf()`: ein Material eines entfernten Mods (z. B. SB-Schachachtel) ließ die ganze Liste scheitern → alle Materialien weg, unsichtbare Zelle mit Kollision und ohne Drops (Muster REV24 H-4). | **behoben**: Einträge einzeln (`Codec.PASSTHROUGH` + `BlockState.CODEC.parse`), Test `mixed_cell_keeps_readable_materials` |
| P3 | `OctetCellBlock` `BREAKING`/`AREA` (ThreadLocal): der Client ruft `getDrops` nie, der letzte gezielte Abbau (hält `ClientLevel`) bzw. `AreaBreak` (hält `LocalPlayer`) blieb am Render-Thread → alte Welt nach dem Verlassen im Speicher. | **behoben**: Client räumt in `restoreRest`, `AreaBreak` speichert die UUID |
| P4 | Gemischte Zelle mit Bits, aber ohne lesbares Material bleibt Geisterzelle (könnte beim Laden zu Luft/Wasser werden). | Queue REV2-B-5 |

### simplemusic
| Schwere | Fund | Status |
|---|---|---|
| P2 | `PocketJukeboxBlockEntity.loadAdditional`: `BundleContents.CODEC` scheitert als Ganzes → eine Platte eines entfernten Mods leerte die platzierte Taschen-Jukebox. | **behoben**: schneller Weg wie bisher, sonst Platte für Platte; Test `pocket_keeps_readable_discs` |
| P3 | `TrackStore.save` warf `IllegalStateException` (Platte voll); aus `MusicServer.tick` (Upload-Abschluss) nicht gefangen → Server-Absturz. | **behoben**: `save()` meldet `false`, `add()` rollt Eintrag + Datei zurück und wirft `IOException` („store_failed“) |
| P3 | `TrackStore.load`: ein beschädigter Eintrag (Handbearbeitung) verwarf den ganzen Index (alle Freigaben/Sperren). | **behoben**: eintragsweise; JUnit `damagedIndexEntrySkipsOnlyItself` |
| P3 | `TrackCache.INCOMING` (Client): abgebrochene Downloads blieben bis Spielende im Speicher, beliebig viele Hashes. | **behoben**: beim Trennen geleert (`TrackPlayer.clear`), höchstens 4 gleichzeitig, neue Downloads nur ab Stück 0 |
| P3 | `MusicClientConfig` schrieb nicht atomar. | **behoben** (`ConfigFiles.writeAtomic`) |
| P4 | `/music ban` nur für Spieler online; Shift+Mausrad an der Taschen-Jukebox prüft nur `mayBuild`/Spawnschutz, keine Claims (nur kosmetisch). | Queue REV2-B-6 |
| ok | Upload (Größe, Rate, Reihenfolge, eigener SHA-256, Worker-Thread), Downloads nur für hörbare Tracks, Dateinamen nur aus Hash, Notenblatt-Edit (Längen, signiert = schreibgeschützt), Presse-Menü (Slots, Quick-Move), Orchestrion-Umzug. | – |

### simplemobs
| Schwere | Fund | Status |
|---|---|---|
| P3 | Client-Leck: `DisguiseLooks.STAND_INS/SPECS`, `DeceiverLooks.STAND_INS` (WeakHashMap; der Wert ist ein Stand-in mit Verweis auf das `ClientLevel`, das den Schlüssel hält → nie frei) und `DisguiseMarker.CLIENT` (`onClientRemoval` läuft beim Trennen nicht) hielten die alte Client-Welt. | **behoben**: beim `DISCONNECT` geleert (`FabricClientExample`) |
| P4 | Tarnung, Howler-Ritual und Trickspiegel melden über die Aktionsleiste (Regel 4 „kein Text für Gadgets“) – bestehendes Design. | Queue REV2-B-10 (Besitzer) |
| ok | Howler-Fallen räumen bei Tod/Entfernen/Laden, mobGriefing überall beachtet, Beschwörungen nur am Muster (Helm-Anzeige droppt beim Abbau), FOV-Paket geklemmt + geräumt (REV24 N-1), Spawns prüfen geladene Chunks, Tarnung rein serverseitig. | – |

### simplemobheads
| Schwere | Fund | Status |
|---|---|---|
| P3 | `HeadAbilities` `SHORN`/`STEW`/`SNIFF_READY` (Spielzeit je UUID) und die WeakHashMaps wurden nie geleert: im Einzelspieler in der nächsten Welt (kleinere Spielzeit) lange gesperrt. | **behoben**: `forgetAll()` über `PlayerState.onStop` (nicht beim Verlassen: ein Relog darf die Scher-Abklingzeit nicht zurücksetzen); Test in `animal_abilities` |
| P3 | `SimpleMobHeads.saveConfig` schrieb nicht atomar (in REV24 N-4 übersehen). | **behoben** |
| P4 | Herobrine-Kopf: `flickerTorches` prüft 17³ Blöcke je Sekunde und Träger. | Queue REV2-B-7 |
| ok | Ritual nur an der einen Stelle und mit Spieler, Abklingzeiten in SavedData begrenzt, Heimsuchung nur an den einen Spieler, Technoblade (/kill + Leere wirken, Krone droppt nicht), Drops über Loot-Bedingung, Kopf-Varianten im Update-Tag. | – |

### simplemovement
| Schwere | Fund | Status |
|---|---|---|
| P3 | `MovementFabric`: der `onChange`-Hook aus `SERVER_STARTED` hielt den `MinecraftServer` nach dem Stopp (Einzelspieler: ganze Welt im Speicher). Gleiches Muster in simplefarming/simpleinterfaces/simpletools (Gruppe A). | **behoben** (Stopp setzt No-op); Gruppe A: Queue REV2-B-1 |
| P3 | `ClimbPacketMixin`: Sprung mit Sprungkraft am Leiterfuß (0,52), Schweben, Riptide, Elytra an der Leiter überschritten das Kletterbudget (0,45) → Gummiband. | **behoben**: Sprungkraft +0,1/Stufe, Schweben/Riptide/Elytra ausgenommen; Test `climb_boosts` |
| ok | Kriechen (Rate, nur Server entscheidet), Config-Sync begrenzt, Tastenmigration atomar (REV24 N-3). | – |

### simplemoney
| Schwere | Fund | Status |
|---|---|---|
| P3 | `SimpleMoney.enabled()` (Resource-Condition) warf `IllegalStateException`, wenn `simplemoney.json` im Betrieb gelöscht/unlesbar war → `/reload` bzw. Weltstart brach ab; `loadConfig` ebenso, Schreiben nicht atomar. | **behoben**: geladene Werte bleiben, Warnung einmal, atomar; Test `config_file_gone` |
| P3 | `MoneyConfigScreen` speicherte nicht atomar und warf bei IO-Fehler (Client-Absturz beim Speichern). | **behoben** |
| P4 | `LinkBudget` entfernt keine Einträge; bei 100 000 Spielern bekäme kein neuer mehr Links. | Queue REV2-B-4 |
| ok | Preise validiert, Budget je Spieler, Rabatte/Nachfrage gesperrt, Quick-Move geprüft. JEI-Entrypoint fehlt (AUDM24-1 → b-jei24). | – |

### simplequalityoflife
| Schwere | Fund | Status |
|---|---|---|
| P3 | `adoptRenamedSwitches` schrieb nicht atomar; toter Code `if(!true)return;` in `QualityOfLifeFabric`. | **behoben** |
| ok | Schalter je Sub-Mod über framework SubMods, Befehl nur Owner, Lang vollständig. | – |

### simplemobmanners
| Schwere | Fund | Status |
|---|---|---|
| P4 | `EntityMuteMixin.isSilent` (heißer Pfad, jeder Klang) baut je Aufruf `EntityType.getKey().toString()` und sucht in einer Liste. | Queue REV2-B-2 |
| ok | Froscheimer (Leine gelöst, Variante/Name/Alter), Migrationen, Listen begrenzt, Piglin-Gold. | – |

### simplemodels
| Schwere | Fund | Status |
|---|---|---|
| P4 | Knopf „Ordner öffnen“ nutzt `gameDir/config/simplemodels/catalogue`, der Server liest `FabricLoader.getConfigDir()/simplemodels` (abweichend bei eigenem Config-Pfad). | Queue REV2-B-9 |
| ok | Pfad-, Tiefen- und Größengrenzen, Amboss prüft beim Herausnehmen neu, Rechte, Katalog-Paket begrenzt. | – |

### simpleovens
| Schwere | Fund | Status |
|---|---|---|
| P4 | Ofen-Tooltip zeigt `tier.speed()` aus der lokalen Client-Config (nicht synchronisiert) → auf fremden Servern evtl. falsche Zahl. | Queue REV2-B-3 |
| ok | Gemeinsamer BE-Typ je Familie (Upgrade behält Inhalt), LongCook-Teiler, Erfahrungs-Ausschluss-Tag, IdMoves, Config geklemmt + atomar. | – |

### simpleoptimization, simpleoptmemory, simpleoptrender, simpleoptserver
| Schwere | Fund | Status |
|---|---|---|
| P4 | optmemory: `StateTables`-Maps bleiben nach dem Bauen der Blockzustände gefüllt (Map-Overhead, könnte nach Registry-Freeze geleert werden). optserver: wird der Schalter im Betrieb aus, bleibt eine abgesenkte Sichtweite (Schalter praktisch nur beim Start). | Queue REV2-B-8 |
| ok | Rivalen-Erkennung, Zielprüfung, MixinGuard (Render-Bereich schaltet sich ab statt abzustürzen), riskante Bereiche Standard aus, Aktivierungsbereich (REV24 M-2). Tooltips der Super-Mod-Schalter → b-cmdcfg24. | – |

## Verbesserungsvorschläge (gesamt)

1. **Ein Lenient-Helfer in simplelib** (`SafeCodecs.listLenient(codec)` o. ä.) für alle Block-Entity-/SavedData-Listen: dieselbe
   Lücke (eine fremde ID löscht alles) trat jetzt in Octets und Music auf, vorher in Tiegel/Fass/Schneidebrett/Kiste (REV24 H-4).
   Ein gemeinsamer Helfer plus Prüfskript (`input.read(..., X.listOf())` verbieten) verhindert neue Fälle.
2. **Client-Lebenszyklus wie PlayerState**: ein `ClientState.onDisconnect` in simplelib, damit Client-Caches (Stand-ins, Marker,
   Download-Puffer) nicht je Modul vergessen werden.
3. **Server-Hooks ohne Server-Referenz**: `onChange`-Hooks der QoL-Sub-Mods über `PlayerState.onStop` zurücksetzen (Gruppe A).
4. Für Config-Dateien nur noch `ConfigFiles.writeAtomic`; ein Check in `checkModuleData`, der `Files.writeString` auf `*.json`
   in `config`-Pfaden meldet.

## Tests

Neue/erweiterte GameTests: simpleoctets `mixed_cell_keeps_readable_materials`, simplemusic `pocket_keeps_readable_discs`,
simplemovement `climb_boosts`, simplemoney `config_file_gone`, simplemobheads `animal_abilities` (forgetAll); JUnit
simplemusic `UploadStoreTest.damagedIndexEntrySkipsOnlyItself`.

Ergebnis (sb-test, 24.10.): module-{simpleoctets,simplemusic,simplemovement,simplemoney,simplemobheads}-fabric-263 und
-standalone-fabric-263 **296/296 grün** (alle neuen Tests im Bericht), JUnit simplemusic grün, Compile simplemobs/simplequalityoflife
grün, Generatoren (Guides, Wiki, Discover) ohne Änderung, `./gradlew check` grün (Drift `forgetAll/0` der reduzierten
Kern-`HeadAbilities` in `tools/dup_pairs.txt` erlaubt). Nicht laufen gelassen: simplemobs/-qualityoflife/-mobmanners/-models/-ovens/
-opt*-Module (nur Compile bzw. keine Änderung), Client-Tests (Lecks der Client-Caches nur per Code-Lesen belegt).

## Nicht oder nur teilweise geprüft

- Client-Renderer/Modelle (simplemobs `client/`, simplemobheads `client/`, simplemusic Screens) nur auf Lecks/Absturzmuster,
  nicht auf Darstellung; keine Client-Läufe (Auftrag).
- Generierte Daten (simplemoney `generated/` Handelsdateien, simplemobheads `HeadTable`, Octet-Rezepte) per Skript auf IDs geprüft,
  nicht jede Datei einzeln gelesen; Discover-/Guide-Klassen (generiert) nur überflogen.
- NeoForge/Forge-Quellen nicht geprüft (nur 26.3 Fabric).
