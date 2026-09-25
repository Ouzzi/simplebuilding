# Uebernahme von Simple Tweaks in SimpleBuilding

Stand: 2026-09-25. Quelle: Repo `simpletweaks` (Branch `1.21.11`, HEAD `bb8f976` plus die
uncommitteten Aenderungen im Arbeitsbaum, MC 1.21.11, nur Fabric, Yarn-Mappings, 56 Java-Dateien,
`readme.md`, `readme-modrinth.md`, `todo.md`, Datagen, Sprachdateien, Texturen, Config).

Ziel des Besitzers: fast alles aus Simple Tweaks wandert nach SimpleBuilding, damit Simple Tweaks
ueberfluessig wird - **ausser dem Claim-System**. Die Claim-Logik ist unten vollstaendig
festgehalten, damit sie spaeter aufgegriffen werden kann; sie wird jetzt **nicht** portiert.

Alles Portierte liegt in SimpleBuilding im Paket `com.simplebuilding.tweaks` (gemeinsamer Code
beider Linien), die Loader-Anbindung in `com.simplebuilding.tweaks.fabric` / `.neoforge` / `.forge`.
Namensraum aller Bloecke und Items ist `simplebuilding:` mit denselben Pfaden wie in Simple Tweaks
(`simpletweaks:elytra_pad` -> `simplebuilding:elytra_pad`).

## 1. Feature-Liste

Status: **port** = uebernommen, **neu** = in SimpleBuilding neu hinzugekommen (Besitzer-Wunsch),
**skip** = bewusst nicht uebernommen, **vorhanden** = gab es in SimpleBuilding schon.

### Druckplatten und Pads

| Feature (Simple Tweaks) | Status | Anmerkung |
|---|---|---|
| Diamant-Druckplatte (nur Spieler, wasserloggbar) | port | Rezept 2 Diamanten waagerecht |
| Netherit-Druckplatte (Fass darunter = Item-Whitelist, Besitzer baut schnell ab) | port | Smithing aus Diamant-Platte |
| Enderit-Druckplatte | neu | Stufe nach Netherit, siehe Stufentabelle |
| Kupfer-Druckplatten (4 Oxidationsstufen, loest erst nach 1-4 s Stehen aus, oxidiert, wachsbar) | port | Oxidation/Wachsen je Loader angebunden |
| Chunk-Loader (Kupferplatte + Netherit, haelt den eigenen Chunk geladen) | port | abschaltbar |
| Enderit-Chunk-Loader | neu | haelt 3x3 Chunks |
| Launchpad (Windkugeln laden, 3 s stehen, Start) | port | abschaltbar |
| Enderit-Launchpad | neu | 32 Ladungen, kein Fallschaden nach dem Start |
| Elytra-Pads I-IV (Radius 5/15/31/63, Hoehe 15/31/63/127) | port | jetzt I-V, abschaltbar |
| Flypads I-IV (Kreativflug im Radius) | port | jetzt I-V, abschaltbar |
| Spawn-Teleporter Stufe 1-4 (= Modi: Ziel Spawn 1-4, Fallback Weltspawn) | port | abschaltbar |
| Enderit-Spawn-Teleporter | neu | Ziel = eigener Wiedereinstiegspunkt (Bett/Anker) |
| Besitzer-Abbau (Besitzer schnell, Fremde sehr langsam, kein Kolben) | port | fuer alle Pads/Platten |
| Partikel fuer den Besitzer auf dem Spawn-Teleporter (Client-Mixin auf die eigene BE) | port | ohne Mixin, direkter Client-Tick der BE |

### Spawn und Spieler

| Feature | Status | Anmerkung |
|---|---|---|
| Spawn-Elytra im Spawnradius (Autoausruesten, Timer, Boosts mit Leertaste, Leuchten, Regeneration) | port | Config `giveElytraOnSpawn` |
| Boost-Paket (Client -> Server) | port | eigener Payload `simplebuilding:elytra_boost` |
| HUD: blaue Boost-Leiste statt XP-Leiste, Timer | port | als HUD-Ebene ueber der XP-Leiste |
| Schadensschutz der Spawn-Elytra (Fall + Kinetik, `IS_SAFE_ELYTRA`) | port | Mixin auf LivingEntity |
| Kein Fallschaden im Spawnbereich (`disableFallDamageInSpawn`) | port | |
| Erstbeitritt: Spawn-Teleporter + Elytra-Pad geschenkt | port | `spawnTeleporterCount` |
| Exakter Spawn (kein Zufallsradius, Bett-Mitte) | port | Mixin auf ServerPlayer |
| Eigener Weltspawn aus der Config beim Laden der Oberwelt | port | |
| Nether/End sperren | port | Mixin auf den Dimensionswechsel |

### Items, Optimierung, Befehle

| Feature | Status | Anmerkung |
|---|---|---|
| Laserpointer (Punkt fuer alle sichtbar, Entfernungsanzeige) | port | Renderer auf 26.x-Submit-Pipeline umgebaut |
| Echo-Kompass (Fremd-Datenpaket `echo-compass-v1.1.0.jar`, AGPL, per `libs/` eingebunden) | port (neu geschrieben) | eigenes Item statt Datenpaket, Rezept neu, Unbreaking-Bug behoben |
| XP-Kugeln verklumpen + sofort aufheben | port | `enableXpClumps` |
| XP-Kugeln nach Wert skalieren | port | `scaleXpOrbs` (Client) |
| Raketen-Stapelgroesse | port | `rocketStackSize` |
| `/killboats`, `/killcarts` (standard/empty/all) | port | eigene Config-Schalter |
| `/simpletweaks ...` Config-Befehle | port | als `/simplebuilding tweaks ...` (gleicher Unterbaum) |
| ModMenu-/Cloth-Config-Seite | port | als Abschnitt "Simple Tweaks" in der SimpleBuilding-Config |
| Claim-System (`/claim`, Urkunde, Schutz) | **skip** | siehe Abschnitt 3 |
| `brick_snowball`-Textur, `itemgroup.simpletweaks.money_items`, `vaultCooldownDays`, `key.simpletweaks.autowalk` | skip | nur Reste in Lang/Texturen, kein Code dahinter |
| `spawn_teleporter_old.png`, `copper_block.png` usw. (unbenutzte Texturen) | skip | kein Modell verweist darauf |
| `ModEntities` (leer) | skip | keine Entities |
| Laser-`showLine`-Option | port (Config) | war schon in Simple Tweaks ohne Wirkung; bleibt ohne Wirkung, dokumentiert |

### todo.md in Simple Tweaks

- BUGS: "unbreaking doesn't work on echo compass" -> **behoben**: der Echo-Kompass ist jetzt ein
  eigenes Item mit Haltbarkeit 64, das Schaden ueber `hurtAndBreak` nimmt; Unbreaking und Mending
  wirken damit wie bei jedem Werkzeug (das Datenpaket zog Haltbarkeit per `set_damage` direkt ab).
- BUGS: "claim deed - not protecting land, just showing who owns it" -> Claims, siehe Abschnitt 3.
- TODO-Punkte (Kompost, Wachs-Varianten, Werfer, Chat-Rechner, Questbuch, Challenges, Extra-Inventar,
  "wooden pressureplate can be turned into ?") sind unfertige Ideen ohne Code -> nicht portiert,
  bleiben hier als Ideenliste stehen.
- DONE-Punkte sind alle im Code enthalten und damit portiert.

## 2. Stufen (Besitzer-Aenderung Phase 3a)

Nach der Netherit-Stufe kommt eine Enderit-Stufe; die Netherstern-Stufe rueckt eins nach oben.
Enderit-Stufen entstehen einheitlich im Schmiedetisch: **Enderit-Aufwertungsvorlage + Netherit-Stufe
+ Enderitbarren**. Die Zusatzfunktion einer Enderit-Stufe behalten alle hoeheren Stufen derselben
Familie.

| Familie | Stufe | ID | Radius (Breite x Breite x Hoehe) / Wirkung | Rezept |
|---|---|---|---|---|
| Elytra-Pad | I | `elytra_pad` | 5x5x15 | Schmiede: beliebige Vorlage + Diamant-Druckplatte + Diamant |
| | II | `reinforced_elytra_pad` | 15x15x31 | Schmiede: beliebige Vorlage + Pad I + Diamantblock |
| | III | `netherite_elytra_pad` | 31x31x63 | Schmiede: Netherit-Vorlage + Pad II + Netheritbarren |
| | **IV (neu)** | `enderite_elytra_pad` | 47x47x95, **Boosts laden im ganzen Bereich** (sonst nur 3x3-Saeule) | Schmiede: Enderit-Vorlage + Pad III + Enderitbarren |
| | V | `fine_elytra_pad` | 63x63x127 + Zusatz von IV | Schmiede: Netherit-Vorlage + **Pad IV** + Netherstern |
| Flypad | I | `flypad` | 5x5x15 | Schmiede: Netherit-Vorlage + Elytra-Pad V + Netheritbarren |
| | II | `reinforced_flypad` | 15x15x31 | Schmiede: Netherit-Vorlage + Flypad I + Netheritblock |
| | III | `netherite_flypad` | 31x31x63 | Werkbank `DBD/ESE/KFK` (Diamantblock, Netheritblock, Verz. Goldapfel, Netherstern, Unheilvoller Schluessel, Flypad II) |
| | **IV (neu)** | `enderite_flypad` | 47x47x95, **Sicherheitsnetz**: wer den Bereich fliegend verlaesst, bekommt 10 s Sanfter Fall statt abzustuerzen | Schmiede: Enderit-Vorlage + Flypad III + Enderitbarren |
| | V | `stellar_flypad` | 63x63x127 + Zusatz von IV | Werkbank `KKK/ESE/FFF` mit F = **Flypad IV** |
| Druckplatte | - | `diamond_pressure_plate` | nur Spieler | Werkbank `DD` |
| | - | `netherite_pressure_plate` | Fass darunter = Item-Whitelist | Schmiede: Netherit-Vorlage + Diamant-Platte + Netheritbarren |
| | **neu** | `enderite_pressure_plate` | wie Netherit, plus **Spielerschloss**: ohne Fass nur der Besitzer; mit Fass zusaetzlich jeder, dessen Name auf einem umbenannten Namensschild im Fass steht | Schmiede: Enderit-Vorlage + Netherit-Platte + Enderitbarren |
| Spawn-Teleporter | I-IV | `spawn_teleporter`, `_tier_2..4` | Ziel Spawn 1-4 (per `/simplebuilding tweaks worldspawn setspawnN`), sonst Weltspawn; 5 s stillstehen | I: beliebige Vorlage + leichte Waegeplatte + Diamantblock **oder** Netherit-Vorlage + leichte Waegeplatte + Netheritbarren; II-IV: Netherit-Vorlage + vorige Stufe + Netheritbarren |
| | **V (neu)** | `enderite_spawn_teleporter` | **Ziel = eigener Wiedereinstiegspunkt** (Bett/Anker, auch in anderer Dimension), Fallback Spawn 1/Weltspawn; nur 3 s stillstehen | Schmiede: Enderit-Vorlage + Teleporter IV + Enderitbarren |
| Chunk-Loader | - | `chunk_loader` | eigener Chunk | Schmiede: Netherit-Vorlage + Kupfer-Druckplatte + Netheritbarren |
| | **neu** | `enderite_chunk_loader` | **3x3 Chunks** um den eigenen | Schmiede: Enderit-Vorlage + Chunk-Loader + Enderitbarren |
| Launchpad | - | `launchpad` | bis 16 Windkugeln | Schmiede: beliebige Vorlage + schwere Waegeplatte + Diamantblock **oder** Netherit-Vorlage + schwere Waegeplatte + Netheritbarren |
| | **neu** | `enderite_launchpad` | bis **32** Windkugeln, **kein Fallschaden** bis zur naechsten Landung | Schmiede: Enderit-Vorlage + Launchpad + Enderitbarren |

Kupfer-Druckplatten sind Oxidationsstufen, keine Materialstufen, und bekommen deshalb keine
Enderit-Variante.

Namen (en): "Elytra Pad I", "Reinforced Elytra Pad II", "Netherite Elytra Pad III",
"Enderite Elytra Pad IV", "Fine Elytra Pad V"; "Flypad I" ... "Enderite Flypad IV",
"Stellar Flypad V"; "Spawn Teleporter I" ... "IV", "Enderite Spawn Teleporter V".

## 3. Echo-Kompass (Phase 3b)

- Eigenes Item `simplebuilding:echo_compass` (vorher: Vanilla-Kompass mit `custom_data` aus einem
  Fremd-Datenpaket). Neu geschrieben, kein Code aus dem AGPL-Datenpaket uebernommen.
- Rechtsklick auf einen Leitstein verknuepft (Vanilla-Komponente `lodestone_tracker`, der Kompass
  zeigt wie ein Leitsteinkompass dorthin). Leitstein weg -> Verknuepfung erlischt wie in Vanilla.
- Benutzen teleportiert auf den Block ueber dem Leitstein, verbraucht **eine Enderperle** (nicht im
  Kreativmodus), der Kompass selbst bleibt, nimmt 1 Haltbarkeit (64 gesamt, Unbreaking/Mending
  wirken), 6 s Abklingzeit. Effekte wie im Datenpaket (Blindheit 1 s, Leuchten 3 s, Sanfter Fall
  1 s, Langsamkeit 1 s, Uebelkeit 6 s).
- Dimensionen: jede Dimension, die der Server kennt (auch Mod-Dimensionen - war trivial, weil die
  Vanilla-Komponente die Dimension mitfuehrt).
- Rezept (Werkbank, geformt):

  ```
   E
  PRP
  ```
  E = Enderit-Kern, P = Netherit-Druckplatte, R = Bergungskompass.

## 4. Claim-System (NICHT portiert - vollstaendige Notiz)

Quelle: `world/ClaimState.java`, `event/ClaimProtectionHandler.java`, `item/custom/ClaimDeedItem.java`,
`command/ModCommands.java` (Zweig `claim`), `todo.md`, Readmes.

### 4.1 Datenmodell `ClaimState` (PersistentState pro ServerWorld)

- Gespeichert unter dem Namen `simpletweaks_claims` (DataFixTypes.LEVEL), **pro Dimension** (die
  Welt, in der der Spieler steht, `ClaimState.get(serverWorld)`).
- Inhalt: `Map<Long, ClaimData>`; Schluessel = `ChunkPos.toLong()`. Beim Speichern als String-Schluessel
  (Codec `Codec.STRING.xmap(Long::parseLong, String::valueOf)`), weil NBT-Compound-Schluessel Strings
  sein muessen (mit `Codec.LONG` scheiterte das Speichern ab dem ersten Claim mit "Not a string" -
  Fix im Commit `bad0e23`).
- `ClaimData`: `owner` (UUID, als String kodiert, Feld `Owner`) und `whitelist` (Set<UUID>, Feld
  `Whitelist`, optional, Standard leer).
- Methoden: `isClaimed(pos)`, `claim(pos, owner)` (nur wenn frei), `unclaim(pos, requestor, isAdmin)`
  (Besitzer oder Admin), `canInteract(pos, player)` (frei -> ja; Besitzer -> ja; auf Whitelist -> ja;
  OP-Stufe >= GAMEMASTERS (2) -> ja), `addFriend/removeFriend(pos, owner, friend)` (nur Besitzer),
  `getOwner`, `getWhitelist`, `getClaimsByPlayer(uuid)`, `getAllClaims()`.

### 4.2 Schutz `ClaimProtectionHandler` (Fabric-Events)

- `PlayerBlockBreakEvents.BEFORE`: Abbau nur mit Erlaubnis, sonst Actionbar "This chunk is claimed!".
- `UseBlockCallback`: jede Block-Interaktion (Kisten, Knoepfe, Bauen) ohne Erlaubnis -> FAIL, ohne Meldung.
- `UseEntityCallback`: Entity-Interaktion (Handel, Rahmen) ohne Erlaubnis -> FAIL.
- `AttackEntityCallback`: Angriffe auf friedliche Entities ohne Erlaubnis -> FAIL + Meldung;
  feindliche Mobs (`!spawnGroup.isPeaceful()`) darf jeder schlagen.
- Geprueft wird der Chunk des Zielblocks bzw. der Entity.
- Registrierung: im Arbeitsbaum aus `Simpletweaks.onInitialize` (der doppelte Aufruf aus
  `ModItems.registerModItems` wurde im Arbeitsbaum entfernt).
- Bekannte Luecken: keine Explosions-, Kolben-, Fluessigkeits-, Feuer- oder Projektil-Pruefung; kein
  Schutz gegen Mobs/Redstone; nur ein Chunk pro Claim; keine Grenzen-Anzeige; Admin-Stufe im
  Schutz 2, in den Admin-Befehlen 4.

### 4.3 Urkunde `claim_deed` (Item, stapelbar bis 16)

- Rechtsklick: freier Chunk -> wird fuer den Spieler geclaimt ("Chunk claimed successfully!"), die
  Urkunde bekommt Daten; eigener Chunk -> Urkunde wird aktualisiert; fremder Chunk -> Fehler.
- Daten in `minecraft:custom_data`: `ClaimPos` (long), `OwnerName` (String), `FriendNames` (Liste von
  Strings, Namen online aufgeloest, sonst die ersten 8 Zeichen der UUID).
- Tooltip: "Deed for Chunk: [x, z]", "Owner: ...", "Authorized Guests:" mit 2 Namen, alle bei
  gedrueckter Umschalttaste (clientseitig ueber GLFW abgefragt - muss auf 26.3 auf SDL/InputConstants
  umgestellt werden), sonst "Unsigned Deed (Right click to claim)".
- Die Urkunde wird beim Claimen **nicht** verbraucht.
- Textur `textures/item/claim_deed.png`, Modell `item/generated`, kein Rezept (nur Kreativ-Tab Werkzeuge).
- **todo.md (Besitzer): "claim deed - not protecting land, just showing who owns it"** - gemeint ist
  (so gelesen): die Urkunde soll kuenftig nur anzeigen, wem ein Chunk gehoert, statt selbst Land zu
  schuetzen; beim Wiederaufgreifen mit dem Besitzer klaeren, ob der Schutz dann ganz entfaellt oder
  getrennt (Befehl) bleibt.

### 4.4 Befehle `/claim`

- `/claim` - zeigt den Besitzer des aktuellen Chunks ("This chunk is wild." / "Chunk owned by: X").
- `/claim trust <spieler>` / `/claim untrust <spieler>` - nur fuer den Besitzer des aktuellen Chunks;
  Spieler online oder aus dem Namens-Cache (`nameToIdCache`).
- `/claim admin unclaim` - Chunk zwangsweise freigeben (Stufe 4).
- `/claim admin listall` - alle Claims der aktuellen Dimension, hoechstens 50 Zeilen.
- `/claim admin list <spieler>` - Claims eines Spielers, hoechstens 10 Zeilen.
- Namensaufloesung: online -> Name, sonst Namens-Cache, sonst UUID.

### 4.5 Was bei einer spaeteren Portierung zu beachten ist

- Loader-Events je Loader (NeoForge: `BlockEvent.BreakEvent`, `PlayerInteractEvent.RightClickBlock`,
  `EntityInteract`, `AttackEntityEvent`); Datenhaltung ueber `SavedData` (26.x `SavedDataType`).
- Nicht mit den Besitzer-Pads verwechseln: die Pads haben schon heute eigenen Besitzer-Abbauschutz.
- Die Readmes listen die `/claim`-Befehle; sie bleiben in Simple Tweaks.

## 5. Config

Neuer Abschnitt `tweaks` in der SimpleBuilding-Config (`TweaksConfig`), gleiche Feldnamen wie in
Simple Tweaks, dazu Schalter je Druckplatten-Familie:

- `pads.enableChunkLoaders`, `pads.enableElytraPads`, `pads.enableFlypads`,
  `pads.enableSpawnTeleporters`, `pads.enableLaunchpads`, `pads.enableTimedCopperPlates`,
  `pads.enableFilterPlates` - aus = der Block bleibt platzierbar, tut aber nichts (Chunk-Loader geben
  ihre Chunks frei, Flypads nehmen den Flug zurueck, Platten senden kein Signal).
- `balancing.rocketStackSize`, `dimensions.allowNether/allowEnd`, `spawn.*`, `commands.*`,
  `optimization.enableXpClumps/scaleXpOrbs`, `laserPointer.*`.

## 6. Was in Simple Tweaks bleibt

Nach der Uebernahme bleibt im Branch `remove-ported-features` von Simple Tweaks nur das Claim-System
(ClaimState, ClaimProtectionHandler, Claim-Urkunde samt Textur/Modell/Lang, `/claim`-Befehle) und
das Geruest (Hauptklasse, Config-Rest, ModMenu). Die Liste des Entfernten steht im Commit dort.
