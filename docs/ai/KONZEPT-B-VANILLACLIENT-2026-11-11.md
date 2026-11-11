# Konzept B-VANILLACLIENT (11.11.2026) – Vanilla-Clients auf Servern mit Simple-Mods (NUR Konzept, kein Code)

Auftrag: /root/b-auftrag-20261109.md „N27 d)“. Anlass: b-server26 (Plan `docs/ai/PLAN-B-SERVER26-2026-11-10.md`, Lauf
`2026-11-11T04-22-09Z-af38`, Basis 47ff8b97d): **alle 60 Mods weisen einen Vanilla-Client ab**, jedes Mal mit
„This server requires Fabric Loader and Fabric API installed on your client!“ – und `simplelib` steht in jeder Namensraum-Liste.
Basis dieses Konzepts: hub/b-bundle179-gen 089fdc551. Nur 26.3 Fabric (Besitzer 22.10.).

## 0. Kurzfassung
- Ursache ist **einzig die Fabric-API-Registry-Synchronisierung** (fabric-registry-sync-v0 8.0.1 in fabric-api 0.161.0+26.3):
  Sobald *irgendein* Mod einen Eintrag in einer synchronisierten statischen Registry (Block, Item, Datenkomponente, Sound,
  Trank, Effekt, Menü, Blockentität, Entität, Partikel …) hat und der Client den Fabric-Kanal nicht spricht, trennt der Server.
- **SimpleLib registriert immer** Blöcke/Items (Warzenwarze, Modpack-Guide, Materialien, Kerne, Endstein-Set),
  2 Datenkomponenten (`warm`, `rarity`), 1 Trank, 1 Sound → jeder Mod mit SimpleLib im Paket ist dadurch vanilla-untauglich.
- **14 Mods** hatten im Lauf *nur* `simplelib` in der Liste (reine Logik). Mit einer registry-freien SimpleLib (Variante B) ließen
  sie Vanilla-Clients rein; mit kleinen Zusatzschritten (Guide-Bücher ohne eigenes Item, B+) **18**.
- **Empfehlung: B** (SimpleLib-Inhalte nur auf Anforderung registrieren, deklarativ, mit Config-Schalter für Altwelten),
  danach `VANILLA_JOIN_REQUIRED` in `server_solo.py` mit der Liste füllen. A (nur Doku) als Sofortmaßnahme in die Release-Notizen.

## 1. Ist-Analyse je Mod

### 1.1 Messung (echter Server, b-server26)
Abweisungsgrund aus den Server-Logs auf sb-test (`testing/runs/2026-11-11T04-22-09Z-af38-server-solo-<mod>-fabric-263.log`,
Zeile „The following registry entry namespaces may be related“):

| Namensräume in der Abweisung | Mods |
|---|---|
| **nur `simplelib`** | simplechallenges, simpleearth, simplefarming, simpleinterfaces, simplelib, simplelivemap, simpleloot, simplemovement, simpleoptimization, simpleoptmemory, simpleoptrender, simpleoptserver, simplesounds, simplevisuals, simpleweather |
| simplelib + eigener (nur Guide-Buch) | simplemodels, simpleresonance, simplerespawn |
| simplelib + simplemodels | simpleaesthetics (Super-Mod: visuals, sounds, models) |
| simplelib + eigener (Guide + 1 Logik-Eintrag) | simplestats (Komponente `item_stats`), simplemobmanners (Item Froscheimer) |
| simplelib + eigener + harte Abhängigkeiten | simplequalityoflife (+simpletools, simplemobmanners), simplemobs (+simpleresonance), simplepads (+simpleores, simplemobheads), simpleovens (+simplecrucibles, simplefoundry), simplesandwiches (+simplecrucibles), simpleend (+simpleenddragonfight), simplebuilding (41 Namensräume) |
| simplelib + eigener (Inhalts-Mod) | alle übrigen (Blöcke/Items/Entitäten sind der Zweck des Mods) |

Auffällig: simplevisuals, simplemodels, simplerespawn, simplestats nennen SimpleLib in `fabric.mod.json` nicht als `depends`,
binden es aber per `include` (JiJ) ein (z. B. `modules/simplevisuals/fabric/build.gradle` Z. 6) → SimpleLib ist im Release-Jar
immer dabei. simplesounds/simpleweather u. a. führen SimpleLib in `fabric.mod.json` als `depends`, in `modules.json` aber nicht
(nur `cloth_config`) – Inkonsistenz, nicht Teil dieses Auftrags, aber für B relevant (Abhängigkeit bleibt ohnehin).

### 1.2 Registrierungen im Code (statische Suche, Shared + Fabric, ohne Tests)
Methode: Zeilen mit `register` und `BuiltInRegistries.X`/`Registries.X`, `PayloadTypeRegistry`, SimpleLib-Inhalts-APIs
(`requestMaterial`, `materialItem`, `AgingChain`, `Buckets`, `ModTabs.register`) und Datapack-Ordner unter `data/<ns>/`.
„S“ = synchronisierte Registry (führt zur Abweisung), „–“ = keine. Zahlen sind Fundstellen, nicht Einträge.

**Reine Logik (keine eigenen S-Einträge):**

| Mod | S-Registries | Sonstiges | Payloads | Datapack-Registries | SimpleLib-Nutzung |
|---|---|---|---|---|---|
| simplevisuals | – | – | 0 | – | api.client (UI-Stil) |
| simplesounds | – | – | 0 | – | ConfigCommands |
| simpleweather | – | – | 0 | – | ConfigCommands |
| simplemovement | – | – | 2 | enchantment (2: step_height, sprint – nur Vanilla-Effekttypen) | ConfigCommands, CommandAliases |
| simplefarming | – | – | 1 | – | ConfigCommands |
| simpleloot | – | – | 0 | – | ConfigCommands |
| simplelivemap | – | – | 0 | – | ConfigCommands |
| simpleoptrender / -optmemory / -optserver / simpleoptimization | – | – | 0 | – | ConfigCommands, CommandAliases |
| simpleinterfaces | – | – | 6 | – | api.client (UI), FreeLook, InteractionGuard, TabSort, DevVariants |
| simpleearth | – | – | 5 | – | api, HeightHandover |
| simplechallenges | – | – | 3 | – | api |

**Fast Logik (wenige, ersetzbare S-Einträge):**

| Mod | S-Einträge | ersetzbar durch |
|---|---|---|
| simplemodels, simpleresonance, simplerespawn | altes Guide-Buch (`WrittenBookItem`, `*Guide.register()`) | Vanilla `minecraft:written_book` mit `custom_data` (Kapitel-Tag gibt es schon: `simplelib_guide_chapter`) |
| simpleaesthetics | nur über simplemodels | folgt simplemodels |
| simplestats | Guide + `simplestats:item_stats` (Komponente auf Vanilla-Werkzeugen) | `minecraft:custom_data` + Lore-Zeilen serverseitig; Migration bestehender Stacks nötig |
| simplemobmanners | Froscheimer (Item = Feature) | nur per „Server-only“-Schalter abschaltbar (Variante C) |

**Inhalts-Mods (brauchen Registrierungen zwingend):** simplebuilding, simplefun, simpletweaks, simpledimensions, simplesandwiches,
simplemaps, simpletrims, simplemobs, simpletools, simplequestionary, simpleenddragonfight, simpleend, simplebeacons, simpleovens,
simplemobheads, simpleshops, simplewanderers, simpleblocks, simplecrucibles, simpleoctets, simplehammers, simplestorage,
simplemusic, simplelights, simplegadgets, simpleanimals, simpledinos, simpleores, simplemachines, simplearrows, simplefoundry,
simpleenchanting, simplepads, simplebuildingtools, simplechisels, simplelayers, simplevault, simplemoney, simpleriding;
simplequalityoflife nur über seine harten Sub-Mods simpletools/simplemobmanners (eigener Eintrag: altes QoL-Buch).
Typisch: BLOCK/ITEM/BLOCK_ENTITY_TYPE/ENTITY_TYPE/MENU/DATA_COMPONENT_TYPE/MOB_EFFECT/POTION/SOUND_EVENT/PARTICLE_TYPE/FLUID/
RECIPE_BOOK_CATEGORY (alle S). Nicht synchronisiert und daher unkritisch: CREATIVE_MODE_TAB, RECIPE_SERIALIZER, RECIPE_TYPE,
LOOT_FUNCTION/CONDITION_TYPE, FEATURE_TYPE, CHUNK_GENERATOR, BIOME_SOURCE, TICKET_TYPE, TRIGGER_TYPES.

### 1.3 SimpleLib selbst (`modules/simplelib/fabric/.../SimpleLibFabric.java`, `shared/java/com/simplelib/registry/*`)
Unbedingt bei jedem Start:
- `LibComponents.register()` → S `DATA_COMPONENT_TYPE` `simplelib:warm`, `simplelib:rarity` (`warm` landet auf Essen –
  auch Vanilla-Essen; `rarity` auf Items via `SimpleRarity.apply`).
- `LibBlocks.register()` / `LibItems.register()` → S `simplelib:warped_wart` (Block+Item), `simplelib:modpack_guide` (Item).
- `LibMaterials.ensureBlocks()/ensureItems()` → S alle Material-Gruppen (cracked_diamond, netherite_nugget, upgrade_templates,
  strong_upgrade, cores ×6, polished_end_stone-Set). Javadoc: „SimpleLib always registers them, so a world stays loadable
  whichever mod is missing“ – nur die *Wege in die Welt* (Rezepte, Amboss) hängen an `request`.
- `LibPotions.register()` → S `simplelib:warped`; `LibSounds.register()` → S `simplelib:legendary`.
- `ModTabs.register(...)` (Tab „Simple Lib“, nicht S), `PortalPreload` (TICKET_TYPE, nicht S), Payloads (5, alle
  `send` hinter `canSend` – vorbildlich), Loot-Änderung Warzenwarze in Bastionen.
- Auf Anforderung (über Partner): `AgingChain` (Blöcke/Items), `Buckets` (Items) – registrieren nur, wenn ein Mod sie nutzt.

## 2. Was Fabric Registry Sync genau auslöst (Quellen)
Quelle: Fabric-API-Code `fabric-registry-sync-v0` 8.0.1+fcdff87f5d (in fabric-api 0.161.0+26.3, `~/.gradle/caches/fabric-loom/
fabric-api/fabric-api-0.161.0+26.3.pom`), Klassen per `javap` gelesen; Quelltext der Vorgängerversion 6.2.6 (Sources-Jar im
Gradle-Cache) für die Logik, die in 8.0.1 unverändert ist. Öffentliche Doku: github.com/FabricMC/fabric-api (Branch 26.3,
`_autodocs`), Context7 `/fabricmc/fabric-api`.

1. **Wann:** in der Konfigurationsphase (`RegistrySyncManager.configureClient`). Der Server baut eine Karte aller Registries mit
   Attribut `SYNCED` **und** `MODDED` (MODDED setzt Fabric, sobald ein Eintrag außerhalb von `minecraft:` hinzukommt).
   Ist die Karte leer → nichts passiert, Vanilla-Client darf rein. Sonst: kann der Client den Kanal `RegistrySyncPayload` nicht
   empfangen (`ServerConfigurationNetworking.canSend` = false, also Vanilla) und sind **nicht alle** betroffenen Registries
   `OPTIONAL` → `disconnect` mit genau dem gemessenen Text („This server requires Fabric Loader and Fabric API installed on your
   client! … The following registry entry namespaces may be related: … Contact the server's administrator …“).
2. **Welche Registries sind SYNCED** (FabricRegistryInit 8.0.1): SOUND_EVENT, FLUID, MOB_EFFECT, BLOCK, ENTITY_TYPE, ITEM,
   POTION, PARTICLE_TYPE, BLOCK_ENTITY_TYPE, CUSTOM_STAT, COMMAND_ARGUMENT_TYPE, MENU, ATTRIBUTE, STAT_TYPE, VILLAGER_TYPE,
   VILLAGER_PROFESSION, GAME_EVENT, NUMBER_FORMAT_TYPE, POSITION_SOURCE_TYPE, DATA_COMPONENT_TYPE, DATA_COMPONENT_PREDICATE_TYPE,
   MAP_DECORATION_TYPE, CONSUME_EFFECT_TYPE, RECIPE_DISPLAY, SLOT_DISPLAY, RECIPE_BOOK_CATEGORY, POINT_OF_INTEREST_TYPE,
   DEBUG_SUBSCRIPTION. **Nicht** SYNCED u. a.: CREATIVE_MODE_TAB, RECIPE_TYPE, RECIPE_SERIALIZER, LOOT_*_TYPE, FEATURE_TYPE,
   CHUNK_GENERATOR, BIOME_SOURCE, STRUCTURE_*, TICKET_TYPE, Trigger.
3. **`RegistryAttribute.OPTIONAL`** existiert, gilt aber für die *ganze Registry* und nur für Registries, die ein Mod selbst
   anlegt (`FabricRegistryBuilder`). Für Vanilla-Registries wie ITEM/BLOCK ist es keine Lösung: Ein Vanilla-Client kennt die
   Einträge nicht und würde an den Rohdaten (Chunk-Paletten, Item-IDs, Komponenten) scheitern. Es gibt **keine** Option, einen
   einzelnen Block-/Item-Eintrag als „optional“ zu markieren, und keine Config, die Sync abschaltet.
4. **Neu in 8.x: `SAVE_DATA_VALIDATED`** + Paket `validate` (Bildschirm „fehlende Registry-Einträge“ mit Sicherung):
   Lädt eine Welt, in der früher modded Einträge gespeichert waren, die jetzt fehlen, warnt Fabric und bietet eine Sicherung an.
   Wichtig für Abschnitt 3 (Altwelten).
5. **Datapack-(dynamische) Registries** (Verzauberungen, Biome, Gemälde, Jukebox-Lieder, Trim, Schadenstypen, Handel …) sind
   **nicht** Teil von Fabric Registry Sync; Vanilla überträgt sie selbst. Ein Vanilla-Client nimmt eigene Einträge an, solange
   sie nur Vanilla-Typen referenzieren (geprüft: simplemovement `sprint`/`step_height` nutzen nur `minecraft:attributes`,
   `minecraft:location_changed` …). Referenziert ein Eintrag einen modded Effekttyp oder modded Sound/Item → Dekodierfehler.
   `DynamicRegistries.registerSynced(..., SKIP_WHEN_EMPTY)` gilt nur für mod-eigene dynamische Registries.
6. **Netzwerk-Payloads** sind kein Grund zur Abweisung: Fabric meldet Kanäle; `ServerPlayNetworking.canSend(player, TYPE)` ist
   beim Vanilla-Client false. Muster: jede `send`-Stelle hinter `canSend` (SimpleLib macht das; simplemovement 4×`send` bei
   3×`canSend` → beim Umsetzen prüfen). Unbekannte Payloads verwirft der Vanilla-Client ohnehin (DiscardedPayload).
7. **Server-only-Muster** in der Fabric-Welt: (a) keine Einträge in SYNCED-Registries, nur Mixins/Events/Datapacks/Befehle;
   Texte per `Component.translatableWithFallback` oder serverseitig übersetzt (Vanilla-Client hat die Lang-Dateien nicht);
   Item-Daten in `minecraft:custom_data` statt eigener Komponenten; (b) Polymer (Patbox): eigene Blöcke/Items werden pro
   Spieler auf Vanilla-Optik abgebildet – Fremdbibliothek, großer Eingriff, siehe Variante D.

## 3. Was SimpleLib bräuchte

### 3.1 Aufteilung Kern / Inhalte
- **SimpleLib-Kern (registry-frei):** Config/ConfigCommands/CommandAliases, api.client (UI-Bausteine, FreeLook, ServerCommand),
  InteractionGuard, TabSort/CreativeTabPolicy (Tab-Regeln, keine S-Registry), IdMoves, HangWatchdog, OwnedLights-Logik,
  InWorldUpgrades/-Strikes (Daten + Payloads), RecipeViewer-Brücke, ModpackGuide-*Daten*, Payloads mit `canSend`.
- **SimpleLib-Inhalte (S-Einträge):** LibComponents (`warm`, `rarity`), LibBlocks/LibItems (Warzenwarze, Modpack-Guide-Item),
  LibMaterials (Gruppen + Kerne + Endstein-Set), LibPotions, LibSounds, AgingChain, Buckets.

Zwei Bauformen, beide ohne neues Modul:
1. **Bedingte Registrierung in SimpleLib (bevorzugt):** Ein Paket, aber jede Inhalts-Gruppe registriert erst, wenn ein geladener
   Mod sie anfordert. Anforderung **deklarativ** in `fabric.mod.json` → `"custom": { "simplelib:content": ["materials:cores",
   "warm", "rarity", "warped_wart", "guide_item", ...] }`, beim Start von SimpleLib über `FabricLoader.getAllMods()` gelesen.
   Vorteil: keine Abhängigkeit von der Reihenfolge der Initialisierer, Server und modded Client kommen mit denselben Mods zum
   selben Ergebnis, Fabric Registry Sync gleicht Unterschiede ohnehin ab. Die heutigen Laufzeit-Aufrufe (`requestMaterial`,
   `materialItem`, `SimpleRarity.apply`, `Warm.*`) bleiben gültig und registrieren zusätzlich „lazy“, solange die Registries
   offen sind (auf Fabric bis nach allen `onInitialize`); danach Fehlermeldung mit Hinweis auf den fehlenden `custom`-Eintrag.
2. **Zweites Modul `simplelib-content`:** sauberer, aber +1 Jar, alle 46 Inhalts-Mods brauchen eine neue Abhängigkeit,
   IdMoves für `simplelib:*` → `simplelibcontent:*` (oder Namensraum beibehalten). Mehr Aufwand ohne Mehrwert gegenüber 1.

### 3.2 Config-Schalter
`simplelib-common.json` → `content.register`: `auto` (Standard: nur Angefordertes) | `always` (heutiges Verhalten, für Server
mit Altwelten) | `never` (Diagnose). Tooltip/Lang EN/DE mit „Default:/Standard:“. Bei `auto` und einer Welt, deren
Speicherstand `simplelib:*` enthält, das nicht registriert ist → Fabric zeigt den neuen Validate-Bildschirm (Abschnitt 2.4);
zusätzlich SimpleLib-Logzeile „setze content.register=always, um diese Einträge zu behalten“.

### 3.3 Weitere nötige Anpassungen (nur für die vanilla-tauglichen Mods)
- **Guide-Bücher:** Logik-Mods haben heute kein eigenes Buch, bekommen den Modpack-Guide über SimpleLib (`modpack_guide` = Item).
  Für Vanilla: Guide als `minecraft:written_book` mit `custom_data` `simplelib_guide_chapter` (Mechanik existiert bereits für
  `turned(guide)`); das Item `simplelib:modpack_guide` nur registrieren, wenn ein Inhalts-Mod es anfordert.
- **Texte:** Befehls-/Aktionsleisten-/Config-Rückmeldungen nutzen `Component.translatable` (SimpleLib 70, simplechallenges 73,
  simpleinterfaces 33, simplevisuals 28 Fundstellen; `translatableWithFallback` fast nirgends). Vanilla-Client sieht sonst
  Schlüssel. Muster: ConfigCommands-Ausgaben mit englischem Fallback; Verzauberungs-JSON `description` mit `"fallback"`.
- **Komponenten auf Vanilla-Items:** `simplelib:warm` (Essen) und `simplelib:rarity` dürfen nur bei angeforderter Gruppe
  gesetzt werden; ohne Registrierung wird die Funktion stumm (kein Absturz, `LibComponents.WARM == null`-Prüfung).
- **Payload-Stellen** ohne `canSend` absichern (simplemovement prüfen).
- **Erwartung klarstellen:** „Lässt rein“ ≠ „wirkt beim Vanilla-Spieler“. Serverseitig wirken weather, farming, loot,
  movement (Servers-Teil), earth (Weltgen), challenges, optserver. Clientseitige Mods (visuals, sounds, optrender, optmemory,
  Teile von interfaces/livemap) haben beim Vanilla-Spieler schlicht keine Wirkung – sie stören aber auch nicht mehr.

### 3.4 Auswirkungen
- **Modul-Unabhängigkeit** (docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md): wird gestärkt – jeder Mod nennt, was er von SimpleLib
  braucht; SimpleLib nennt weiterhin keine Nutzer. Standalone-Ziele bleiben gültig, je Inhalts-Mod muss die `custom`-Liste
  vollständig sein (sonst fehlt ein Item im Standalone-Lauf → neue Prüfung in `precheck`/DataIntegrity: „jede genutzte
  SimpleLib-Gruppe ist deklariert“).
- **Bestehende Welten / IdMoves:** Welten, die nur mit Logik-Mods + SimpleLib gespielt wurden, können `simplelib:warped_wart`
  (Bastion-Loot!), `simplelib:modpack_guide`, Material-Items enthalten. Bei `auto` würden diese Stacks/Blöcke verschwinden.
  Abhilfe: (1) Server-Config `content.register=always` für alte Welten (Hinweis in den Release-Notizen und in der Logzeile aus
  3.2). Eine automatische Erkennung „Welt kannte früher Gruppe X“ geht auf Fabric nicht, weil die Registries vor dem Laden der
  Welt einfrieren; (2) IdMoves-Ziele auf Vanilla (`simplelib:warped_wart` → `minecraft:nether_wart`, Guide →
  `minecraft:written_book`) für den Fall „Inhalt nicht registriert“. Empfehlung: (2) für Warzenwarze/Guide, sonst (1) per Config.
- **Bastion-Loot Warzenwarze:** nur einhängen, wenn die Gruppe registriert ist (heute unbedingt).
- **NeoForge/Forge:** NeoForge hat eigene Registry-Sync-Regeln (Vanilla-Clients werden bei modded Einträgen ebenfalls
  abgewiesen); die Aufteilung hilft dort gleich. Laut Besitzer erst beim Port (Memory „NUR 26.3 Fabric“).

### 3.5 Aufwand / Risiko
- SimpleLib bedingt (3.1-1 + 3.2): ~1 Agent-Tag. 46 Inhalts-Mods `custom`-Liste: mechanisch (Skript aus Nutzungsstellen), ~½ Tag.
  Guide-Buch-Umstellung (3 Mods + Modpack-Guide-Weg): ~½ Tag. Texte mit Fallback: ~½–1 Tag (viele Stellen, mechanisch).
- Tests: `server-solo`-Ziele (b-server26) mit `VANILLA_JOIN_REQUIRED` = Liste unten; Standalone-Ziele aller Inhalts-Mods
  (fehlende Deklaration fällt dort sofort auf); DataIntegrity.
- Risiko **mittel**: vergessene Deklaration → fehlendes Item in einem Mod-Paket (fällt in Standalone-Tests auf); Altwelten
  (abgefedert durch Config/IdMoves); Reihenfolge-Fehler bei Lazy-Registrierung (abgefedert durch deklarativ).

## 4. Varianten

| | A: nichts ändern, Doku | B: SimpleLib-Inhalte nur auf Anforderung | B+: B + Guide-Bücher ohne eigenes Item | C: „Server-only“-Schalter je Mod | D: Polymer |
|---|---|---|---|---|---|
| Was | Release-Notiz „Client braucht Fabric API + gleiche Mods“ | 3.1-1, 3.2, 3.3 (Texte, Komponenten, Payloads) | zusätzlich simplemodels/-resonance/-respawn Guide als Vanilla-Buch | Config `serverOnly` schaltet eigene S-Einträge eines Mods ab (z. B. Froscheimer, Stats-Komponente) | Inhalts-Mods auf Polymer umbauen (Vanilla-Optik je Spieler) |
| Vanilla-taugliche Mods | 0 | 14 (+ SimpleLib) | 18 | B+ und wenige mehr (simplemobmanners ohne Froscheimer, simplestats nach custom_data) | theoretisch alle |
| A12 (Vanilla-Nutzen/Eingriff) | kein Eingriff | kein Spielwirkungs-Eingriff; Server-Admins können Vanilla-Spieler zulassen (Nutzen: Logik-Mods als „Server-Plugin“) | wie B; Guide sieht für alle wie ein Vanilla-Buch aus (minimal) | Feature fehlt im Modus → Verhalten je Server unterschiedlich (mittel) | stark (Optik/Verhalten weicht ab, Fremdabhängigkeit) |
| Aufwand | Minuten | ~2–3 Agent-Tage | +½ Tag | +½ Tag je Mod | Wochen |
| Risiko | keins | mittel (Deklaration, Altwelten) | gering zusätzlich | mittel (zwei Verhaltensweisen testen) | hoch |

**Empfehlung:** A sofort (Release-Notiz, auch im Wiki „Server“), danach **B+** als ein Paket (SimpleLib + 3 Guide-Mods +
`server_solo.VANILLA_JOIN_REQUIRED`). C nur auf ausdrücklichen Wunsch für einzelne Mods; D nicht.

**Mit B vanilla-tauglich (14 + SimpleLib):** simplechallenges, simpleearth, simplefarming, simpleinterfaces, simplelivemap,
simpleloot, simplemovement, simpleoptimization, simpleoptmemory, simpleoptrender, simpleoptserver, simplesounds, simplevisuals,
simpleweather (+ simplelib allein).
**Zusätzlich mit B+:** simplemodels, simpleresonance, simplerespawn, simpleaesthetics.
**Mit C möglich:** simplestats (Komponente → custom_data), simplemobmanners (ohne Froscheimer), danach simplequalityoflife nur,
wenn auch simpletools seine Blöcke (Todes-Tresor, Entrindungsstation) abschaltet – nicht sinnvoll.

Verifikation nach Umsetzung: `python3.12 tools/testrunner/run.py --targets server-solo` (60 Ziele, ~50–70 min, nur einzeln auf
sb-test) – `vanilla_join` muss für die Liste grün statt `known` sein; Standalone-Ziele der Inhalts-Mods grün.

## 5. Offene Fragen an den Besitzer (Antwortcode VANILLA27)
1. **Überhaupt gewünscht?** Sollen Logik-Mods Vanilla-Clients reinlassen (B/B+), oder reicht die Doku (A)?
   Vorschlag: B+.
2. **Altwelten:** Standard `content.register=auto` (neue Welten sauber, Altwelten brauchen Config `always`) oder Standard
   `always` und Vanilla-Tauglichkeit nur per Opt-in? Vorschlag: `auto` + IdMoves Warzenwarze/Guide → Vanilla.
3. **Guide für Vanilla-Spieler:** Modpack-Guide als Vanilla-Buch (Text ohne eigene Oberfläche) in Ordnung?
4. **simplestats / simplemobmanners:** Variante C (Stats in `custom_data`, Froscheimer abschaltbar) gewollt oder nicht nötig?
5. **Texte:** Englischer Fallback für Server-Meldungen genügt (Vanilla-Spieler sehen Englisch), oder serverseitige Übersetzung
   nach Client-Sprache (mehr Aufwand)?
