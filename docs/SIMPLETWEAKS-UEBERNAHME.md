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
| Kupfer-Druckplatten (4 Oxidationsstufen, loest erst nach 1-4 s Stehen aus, oxidiert, Axt kratzt eine Stufe ab) | port | seit 2026-09-27 **wachsbar** wie Vanilla-Kupfer (Abschnitt 2.4), Loslassen so verzoegert wie Ausloesen, gedrueckt sichtbar eingesunken |
| Chunk-Loader (Kupferplatte + Netherit, haelt den eigenen Chunk geladen) | port | abschaltbar; seit 2026-09-27 Stufe I von drei |
| Netherit-Chunk-Loader II | neu | Kreuz aus 5 Chunks (eigener + 4 Nachbarn mit gemeinsamer Kante) |
| Enderit-Chunk-Loader III | neu | haelt 3x3 Chunks |
| Launchpad (Windkugeln laden, 3 s stehen, Start) | port | abschaltbar; seit 2026-09-27 Stufe I (4 Ladungen) von drei, Schleichen + Rechtsklick laedt alle Windkugeln der Hand |
| Netherit-Launchpad II | neu | 8 Ladungen |
| Enderit-Launchpad III | neu | 16 Ladungen (Schub wie frueher 32), kein Fallschaden nach dem Start |
| Elytra-Pads I-IV (Radius 5/15/31/63, Hoehe 15/31/63/127) | port | jetzt I-V (1x1 bis 128x128, Abschnitt 2.3), abschaltbar |
| Flypads I-IV (Kreativflug im Radius) | port | jetzt drei Stufen aus Enderit (Abschnitt 2.3), abschaltbar |
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
| Erstbeitritt: Spawn-Teleporter + Elytra-Pad geschenkt | port, geaendert | `firstJoinTeleporterCount`, `firstJoinElytraPadCount` (beide Standard 0; alter `spawnTeleporterCount` != 1 wird fuer beide uebernommen; abgeschaltete Familie = kein Geschenk) |
| Exakter Spawn (kein Zufallsradius, Bett-Mitte) | port | Mixin auf ServerPlayer; `forceExactSpawn` jetzt Standard **aus** |
| Eigener Weltspawn aus der Config beim Laden der Oberwelt | port | `useCustomWorldSpawn`, Standard **aus** |
| Nether/End sperren | port | Mixin auf den Dimensionswechsel |

### Items, Optimierung, Befehle

| Feature | Status | Anmerkung |
|---|---|---|
| Laserpointer (Punkt fuer Spieler in 128 Bloecken sichtbar, Entfernungsanzeige) | port + Umbau zur "Amethystlinse" | Renderer auf 26.x-Submit-Pipeline umgebaut; Server prueft Item/Schalter/Rate (Audit 2026-09-26 #17); seit 2026-09-27 Strahlwirkungen, Ladung, Amboss-Aufladen (siehe unten) |
| Echo-Kompass (Fremd-Datenpaket `echo-compass-v1.1.0.jar`, AGPL, per `libs/` eingebunden) | port (neu geschrieben, 2026-09-27 umgebaut, jetzt "Echolot"/"Echo Sounder") | eigenes Item statt Datenpaket (Id weiter `echo_compass`), Rezept neu, Unbreaking-Bug behoben; Aufladen 3 s, Leeren/Aufladen/Zerspringen, eigene Textur, keine Enderperle mehr |
| XP-Kugeln verklumpen + sofort aufheben | port | `enableXpClumps` |
| XP-Kugeln nach Wert skalieren | port | `scaleXpOrbs` (Client) |
| Raketen-Stapelgroesse | port | `rocketStackSize` |
| `/killboats`, `/killcarts` (standard/empty/all) | port | eigene Config-Schalter |
| `/simpletweaks ...` Config-Befehle | port | als `/simplebuilding tweaks ...` (gleicher Unterbaum) |
| ModMenu-/Cloth-Config-Seite | port | als Abschnitt "Simple Tweaks" in der SimpleBuilding-Config |
| Claim-System (`/claim`, Urkunde, Schutz) | **skip** | siehe Abschnitt 4 |
| `brick_snowball`-Textur, `itemgroup.simpletweaks.money_items`, `vaultCooldownDays`, `key.simpletweaks.autowalk` | skip | nur Reste in Lang/Texturen, kein Code dahinter |
| `spawn_teleporter_old.png`, `copper_block.png` usw. (unbenutzte Texturen) | skip | kein Modell verweist darauf |
| `ModEntities` (leer) | skip | keine Entities |
| Laser-`showLine`-Option | port (Config) | war schon in Simple Tweaks ohne Wirkung; Schluessel bleibt lesbar, im Config-Bildschirm ausgeblendet |

### todo.md in Simple Tweaks

- BUGS: "unbreaking doesn't work on echo compass" -> **behoben**: der Echo-Kompass ist jetzt ein
  eigenes Item mit Haltbarkeit; Unbreaking und Mending wirken wie bei jedem Werkzeug (das Datenpaket
  zog Haltbarkeit per `set_damage` direkt ab). Seit 2026-09-27: 1500 Punkte, ein Sprung leert ihn
  (Unbreaking je Punkt), siehe Abschnitt 3.
- BUGS: "claim deed - not protecting land, just showing who owns it" -> Claims, siehe Abschnitt 4.
- TODO-Punkte (Kompost, Wachs-Varianten, Werfer, Chat-Rechner, Questbuch, Challenges, Extra-Inventar,
  "wooden pressureplate can be turned into ?") sind unfertige Ideen ohne Code -> nicht portiert,
  bleiben hier als Ideenliste stehen.
- DONE-Punkte sind alle im Code enthalten und damit portiert.

## 2. Stufen (Besitzer-Aenderung Phase 3a)

Nach der Netherit-Stufe kommt eine Enderit-Stufe; die Netherstern-Stufe rueckt eins nach oben.
Enderit-Stufen entstehen im Schmiedetisch aus **Enderit-Aufwertungsvorlage + Netherit-Stufe**; die
dritte Zutat war anfangs ein Enderitbarren und ist seit Abschnitt 2.1 die Enderit-Druckplatte (die
Enderit-Druckplatte selbst nimmt weiter den Barren; Rezepte je Stufe in der Tabelle). Die Zusatzfunktion einer Enderit-Stufe behalten alle hoeheren Stufen derselben
Familie.

| Familie | Stufe | ID | Radius (Breite x Breite x Hoehe) / Wirkung | Rezept |
|---|---|---|---|---|
| Elytra-Pad | I | `elytra_pad` | **1x1**x15 | Schmiede: beliebige Vorlage + **Elytra** (keine dritte Zutat) |
| | II | `reinforced_elytra_pad` | **5x5**x31 | Schmiede: beliebige Vorlage + Pad I + **Diamant-Druckplatte** |
| | III | `netherite_elytra_pad` | **16x16**x63 | Schmiede: Netherit-Vorlage + Pad II + **Netherit-Druckplatte** |
| | **IV (neu)** | `enderite_elytra_pad` | **32x32**x95, **Boosts laden im ganzen Bereich** (sonst nur 3x3-Saeule) | Schmiede: Enderit-Vorlage + Pad III + **Enderit-Druckplatte** |
| | V | `fine_elytra_pad` | **128x128**x127 + Zusatz von IV | Schmiede: Netherit-Vorlage + **Pad IV** + Netherstern (Muster der bisherigen Endstufe) |
| Flypad | I | `flypad` | **4x4x6**, Sicherheitsnetz (alle Stufen) | Schmiede: Enderit-Vorlage + **Enderit-Druckplatte** + **Enderit-Kern** |
| | II | `reinforced_flypad` | **8x8x12** | Schmiede: Enderit-Vorlage + Flypad I + **Enderit-Druckplatte** |
| | III | `stellar_flypad` | **16x16x24** | Schmiede: Enderit-Vorlage + Flypad II + **Flypad II** (zwei II zusammen) |
| | alt | `netherite_flypad`, `enderite_flypad` | wird zu II bzw. III (Abschnitt 2.3) | kein Rezept, nicht im Kreativ-Tab |
| Druckplatte | - | `diamond_pressure_plate` | nur Spieler | Werkbank `DD` |
| | - | `netherite_pressure_plate` | Fass darunter = Item-Whitelist | Schmiede: Netherit-Vorlage + Diamant-Platte + Netheritbarren |
| | **neu** | `enderite_pressure_plate` | wie Netherit, plus **Spielerschloss**: ohne Fass nur der Besitzer; mit Fass zusaetzlich jeder, dessen Name auf einem umbenannten Namensschild im Fass steht | Schmiede: Enderit-Vorlage + Netherit-Platte + Enderitbarren |
| Spawn-Teleporter | I-IV | `spawn_teleporter`, `_tier_2..4` | Ziel Spawn 1-4 (per `/simplebuilding tweaks worldspawn setspawnN`), sonst Weltspawn; 5 s stillstehen | I: beliebige Vorlage + leichte Waegeplatte + Diamantblock **oder** Netherit-Vorlage + leichte Waegeplatte + Netheritbarren; II-IV: Netherit-Vorlage + vorige Stufe + **Netherit-Druckplatte** |
| | **V (neu)** | `enderite_spawn_teleporter` | **Ziel = eigener Wiedereinstiegspunkt** (Bett/Anker, auch in anderer Dimension), Fallback Spawn 1/Weltspawn; nur 3 s stillstehen | Schmiede: Enderit-Vorlage + Teleporter IV + **Enderit-Druckplatte** |
| Chunk-Loader | I | `chunk_loader` | nur der eigene Chunk | Schmiede: beliebige Vorlage + Kupfer-Druckplatte + **Diamant-Druckplatte** |
| | **II (neu)** | `netherite_chunk_loader` | **5 Chunks**: der eigene und die vier mit gemeinsamer Kante (Kreuz) | Schmiede: Netherit-Vorlage + Chunk-Loader I + **Netherit-Druckplatte** |
| | III | `enderite_chunk_loader` | **3x3 Chunks** um den eigenen | Schmiede: Enderit-Vorlage + Chunk-Loader II + **Enderit-Druckplatte** |
| Launchpad | I | `launchpad` | bis **4** Windkugeln | Schmiede: beliebige Vorlage + schwere Waegeplatte + **Diamant-Druckplatte** |
| | **II (neu)** | `netherite_launchpad` | bis **8** Windkugeln | Schmiede: Netherit-Vorlage + Launchpad I + **Netherit-Druckplatte** |
| | III | `enderite_launchpad` | bis **16** Windkugeln, **kein Fallschaden** bis zur naechsten Landung | Schmiede: Enderit-Vorlage + Launchpad II + **Enderit-Druckplatte** |
| Trank-Pad (neu) | I | `potion_pad` | gespeicherter Wurftrank, **30 s** nach 3 s Stehen, danach **60 s** Abklingzeit | Werkbank (formlos): **Netherit-Druckplatte + Lohenkopf** |
| | II | `reinforced_potion_pad` | **60 s**, Abklingzeit **120 s** | Schmiede: Enderit-Vorlage + Trank-Pad I + **Enderit-Druckplatte** |
| | III | `infused_potion_pad` | **120 s**, Abklingzeit **240 s** | Schmiede: Enderit-Vorlage + Trank-Pad II + **Enderit-Kern** |

Kupfer-Druckplatten sind Oxidationsstufen, keine Materialstufen, und bekommen deshalb keine
Enderit-Variante.

**Stapelgroesse (Besitzer 2026-09-28):** jedes Pad-Item stapelt nicht (Stapelgroesse 1) - alle Stufen
aller Familien (Elytra-Pad, Flypad, Spawn-Teleporter, Launchpad, Chunk-Loader, Trank-Pad), ihre
Easter-Stufen und die alten Flypads. Grund: jedes Pad ist ein Einzelstueck (Easter-Stufe, gespeicherter
Trank, Restabklingzeit). Umsetzung `TweaksItems#isPad` (jeder `PadBlock` ausser den Kupfer-Druckplatten)
setzt `stacksTo(1)`; die Druckplatten stapeln weiter bis 64. Test
`potion_pad_game_test_every_pad_item_stacks_to_one`.

### 2.1 Aufwertungen zahlen mit Druckplatten (Besitzer-Aenderung 2026-09-27)

Jede Aufwertung einer Pad-Familie kostet die **Druckplatte des Zielmaterials** statt des Rohstoffs
(Barren, Block). Das gilt fuer alle Schmiede-Aufwertungen; unveraendert bleiben die Einstiegsstufen,
die Druckplatten selbst (sie sind die Quelle der Platten) und die Netherstern-Stufe des Elytra-Pads
(es gibt keine Netherstern-Platte). Die Flypads sind seit Abschnitt 2.3 eine reine Enderit-Familie.

| Ziel-Material | Zutat vorher | Zutat jetzt | Aufwertungen |
|---|---|---|---|
| Diamant | Diamantblock (Pads) bzw. - (neu) | Diamant-Druckplatte | Elytra-Pad II, Launchpad I, Chunk-Loader I |
| Netherit | Netheritbarren / Netheritblock | Netherit-Druckplatte | Elytra-Pad III, Spawn-Teleporter II-IV, Launchpad II, Chunk-Loader II |
| Enderit | Enderitbarren | Enderit-Druckplatte | Elytra-Pad IV, Flypad I (Basis) und II, Spawn-Teleporter V, Launchpad III, Chunk-Loader III |

- Die frueheren Rezepte `launchpad_smithing_alternative` (Netherit-Vorlage + schwere Waegeplatte +
  Netheritbarren) und der Chunk-Loader aus Netheritbarren entfallen; Launchpad I und Chunk-Loader I
  sind jetzt die Diamant-Stufe (beliebige Vorlage + Familienplatte + Diamant-Druckplatte).

### 2.2 Launchpad- und Chunk-Loader-Stufen (Besitzer-Aenderung 2026-09-27)

- **Launchpad:** I/II/III fassen 4/8/16 Windkugeln. Der Schub je Ladung ist verdoppelt
  (`1,5 + 0,8 x Ladungen` statt `1,5 + 0,4 x Ladungen`): jede volle Stufe startet wie frueher die
  doppelte Ladung, 16 Ladungen wie die alten 32. Die Enderit-Stufe behaelt den Fallschutz bis zur
  naechsten Landung. **Schleichen + Rechtsklick** mit Windkugeln laedt alle Windkugeln der Hand auf
  einmal (bis zum Fassungsvermoegen); normaler Rechtsklick weiter eine. Technik: beim Schleichen fragt
  Vanilla den Block nicht, darum legt `LaunchpadWindChargeMixin` `useOn` in `WindChargeItem` an.
- **Chunk-Loader:** I = eigener Chunk, II = Kreuz aus 5 Chunks, III = 3x3. Freigabe beim Abbau, in
  `setRemoved` (`/setblock`/`/fill`) und die Uebergabe an einen ueberlappenden Loader pruefen die Form
  der jeweiligen Stufe (ein Netherit-Loader uebernimmt keinen Diagonal-Chunk).
- **Bestehende Welten:** die IDs bleiben. `launchpad` ist Stufe I, `enderite_launchpad` Stufe III,
  `chunk_loader` Stufe I (weiter nur der eigene Chunk), `enderite_chunk_loader` Stufe III (weiter 3x3).
  Ein altes Launchpad mit mehr Ladungen, als seine Stufe jetzt fasst (Enderit bis 32, das normale bis
  16), behaelt beim ersten Tick 16 bzw. 4 und wirft den Rest als Windkugeln aus - keine geht verloren
  (`LaunchpadBlockEntity#clampToCapacity`). Weil die IDs bleiben, braucht es keinen DataFixer.

### 2.3 Elytra-Pad- und Flypad-Stufen (Besitzer-Aenderung 2026-09-27, zweite Runde)

- **Elytra-Pad** (der Besitzer schrieb "Flypad", meinte aber das Elytra-Pad, weil Stufe I eine Elytra
  kostet): fuenf Stufen 1x1, 5x5, 16x16, 32x32, 128x128 (Hoehen unveraendert 15/31/63/95/127). Stufe I
  entsteht im Schmiedetisch aus beliebiger Vorlage + Elytra ohne dritte Zutat (SmithingTransformRecipe
  mit leerer Zutat, `tweaksSmithingWithoutAddition`); II-IV mit Diamant-/Netherit-/Enderit-Druckplatte;
  V wie bisher mit Netherstern (das bestehende Muster der Endstufe). Bestehende Pads behalten ID und Rang
  (I bleibt I usw.); nur ihre Bereiche aendern sich.
- **Flypad**: nur noch drei Stufen, alle aus Enderit statt Netherit: 4x4x6, 8x8x12, 16x16x24 (Breite x
  Tiefe x Hoehe, ab der Unterkante des Pads). I = Enderit-Vorlage + Enderit-Druckplatte + Enderit-Kern,
  II = Enderit-Vorlage + I + Enderit-Druckplatte, III = zwei II im Schmiedetisch (Enderit-Vorlage, das
  zweite als Zutat). Weil jede Stufe aus Enderit ist, hat jede das Sicherheitsnetz (10 s Sanfter Fall
  beim fliegenden Verlassen), das vorher erst ab der Enderit-Stufe IV galt.
- **Bestehende Flypads** (nach Rang auf die naechste neue Stufe): alt I `flypad` -> I, alt II
  `reinforced_flypad` -> II, alt III `netherite_flypad` -> II, alt IV `enderite_flypad` -> III, alt V
  `stellar_flypad` -> III. Die zwei entfallenen IDs bleiben als `LegacyFlypadBlock` registriert: sie
  arbeiten wie ihre neue Stufe und werden beim ersten Tick zu ihr (Besitzer und verfolgte Flieger bleiben);
  ihr Item (`LegacyTierBlockItem`) tauscht sich im Spielerinventar gegen die neue Stufe. Nicht im
  Kreativ-Tab, kein Rezept, per `c:hidden_from_recipe_viewers` aus JEI ausgeblendet, in der Testzentrale
  unter "Alte Stufen".
- **Texturen**: die neuen Flypad-Stufen tragen `flypad_ender`, `reinforced_flypad_ender`,
  `stellar_flypad_ender` (Enderit-Rahmen und Motiv des alten Enderit-Flypads, Akzent je Stufe: Violett,
  Ender-Magenta, Goldweiss). Die alten `flypad.png`, `reinforced_flypad.png`, `stellar_flypad.png` bleiben
  liegen - seit 2026-09-28 in der Netherit-Palette die Vorlage der Trank-Pads (Abschnitt 2.4).
- **Endstufen**: `TweaksFamilies.tiers(Family)` / `lastTier(Family)` nennt je Familie die Stufen und die
  hoechste, als Ansatzpunkt fuer spaetere Erweiterungen ueber den Endstufen.

### 2.4 Trank-Pad und Lohenkopf (Besitzer 2026-09-28)

Neue Pad-Familie `TweaksFamilies.Family.POTION_PAD` (Endstufe `infused_potion_pad`, damit greift auch
die Kette ueber den Endstufen), Code `PotionPadBlock` / `PotionPadBlockEntity`.

- **Speichern**: ein Wurftrank, der auf dem Pad zerschellt (`Block#onProjectileHit`, also Treffer auf
  Ober- oder Seitenflaeche), wird gespeichert (`PotionContents`, BE-Schluessel `Potion`) und ersetzt den
  vorigen. **Verweiltraenke zaehlen wie Wurftraenke** (entschieden: beide sind Wurftraenke, und ein
  teurer gebrauter Trank soll nicht wirkungslos zerschellen). Ein Trank ohne Wirkung (Wasser, seltsamer
  Trank) **wischt das Pad leer**. Anzeige: alle halbe Sekunde Wirkungspartikel in der Trankfarbe
  (vom Server gesendet, kein Client-Code); Rechtsklick ohne Gegenstand zeigt Trank und Dauer. Kein
  Komparator-Ausgang. Beim Abbau behaelt das Item den Trank (`potion_contents`) und gibt ihn beim
  Setzen zurueck (seit 2026-09-28, zusammen mit der Easter-Kette; vorher ging er verloren).
- **Wirkung, Aufladen in drei Schritten** (Besitzer 2026-09-28, ersetzt "volle Dauer beim Betreten"):
  jeder **Spieler** (keine Mobs), der auf dem Pad steht, bekommt die gespeicherten Dauerwirkungen mit der
  **Verstaerkung des Tranks**: nach **1 s 25 %**, nach **2 s 50 %**, nach **3 s 100 %** von
  **30 s / 60 s / 120 s** (I/II/III; letzte Easter-Stufe 240 s). Jeder Schritt zeigt Wirkungspartikel in
  der Trankfarbe und einen leisen Klang (Amethyst-Klingen, hoeher je Schritt; bei 100 % das Braustand-
  Geraeusch). Wer vorher absteigt, **behaelt das bisher Erhaltene** und beginnt beim naechsten Betreten
  wieder bei 0 (`PotionPadBlockEntity#standing`, nicht gespeichert). Eine laengere Wirkung, die der Spieler
  schon hat, wird nicht gekuerzt (`MobEffectInstance#update`).
- **Sofortwirkungen** (Heilung, Schaden): **einmal, beim 3-s-Schritt**; die alte 2-s-Sperre je Spieler
  entfaellt, die Abklingzeit des Pads uebernimmt das. Schaden trifft ohne Verursacher (`magic`).
- **Abklingzeit** (Besitzer 2026-09-28): der 100-%-Schritt setzt das **ganze Pad** fuer die **doppelte
  Wirkdauer** in die Abklingzeit - **60 s / 120 s / 240 s**, letzte Easter-Stufe **480 s**
  (`PotionPadBlock#cooldownAt`). **Entschieden: erst 100 % startet sie**; ein abgebrochenes Aufladen
  nicht, sonst koennte ein kurzer Schritt das Pad fuer alle sperren. Waehrend der Abklingzeit gibt das Pad
  niemandem etwas (auch Mitstehende gehen leer aus). Sie laeuft **nur, solange das Pad gesetzt ist** (der
  Block-Entity-Ticker zaehlt, BE-Schluessel `Cooldown`); Blockzustand `cooling=true` zeigt die animierte
  Textur, Rechtsklick nennt die Restzeit. **Abbau in der Abklingzeit**: das Item traegt die Restticks als
  Komponente `simplebuilding:potion_pad_cooldown` (eigener Item-Zustand: animiertes Item-Modell ueber
  `minecraft:has_component`, Tooltip "Klingt ab: noch m:ss", Stapelgroesse 1), zusammen mit Trank und
  Easter-Stufe; gesetzt laeuft die Zeit dort weiter (`getStateForPlacement` setzt `cooling`,
  `applyImplicitComponents` die Restzeit). Ein bereites Pad faellt ohne diese Komponente.
- **Haltbarkeit**: unbegrenzt (keine Ladungen, Besitzer-Vorgabe).
- **Bereich**: die Blockspalte des Pads bis einen halben Block hoch (`PotionPadBlockEntity#area`).
- Alle Stufen brennen nicht (Netherit); I ist ungewoehnlich (Netherit-Druckplatte), II und III sind episch (docs/RARITAETEN.md); Besitzer-Abbau und kein Kolben wie alle Pads.
- **Rezepte**: I = Werkbank formlos Netherit-Druckplatte + Lohenkopf. II = Schmiede Enderit-Vorlage +
  I + **Enderit-Druckplatte** (Regel 2.1: Aufwertungen zahlen mit der Druckplatte des Zielmaterials).
  III = Schmiede Enderit-Vorlage + II + **Enderit-Kern** (wie Flypad I; es gibt keine hoehere Platte).
- **Texturen**: die alten `flypad.png`, `reinforced_flypad.png`, `stellar_flypad.png` (fuer genau
  diesen Zweck aufgehoben) in der Netherit-Palette: Stein auf die Netherit-Rampe, die blauen Adern auf
  eine Glut-Rampe je Stufe (I Lohen-Orange, II Enderit-Violett, III Gold), die Funkelsterne des
  stellaren Bildes warmweiss; in der Mitte eine kleine Trankflasche in der Stufenfarbe.
  Abklingzeit: je Stufe ein Streifen `<id>_cooling.png` mit 12 Bildern (je 8 Ticks, `.mcmeta` vom
  Generator): die Adern verlieren die Glut und pulsieren langsam, die Flasche steht leer und fuellt sich
  Bild fuer Bild wieder. `tools/textures/potion_pad_textures.py` (von `generate_textures.py`
  eingebunden). Abnahme offen.

**Lohenkopf** (`blaze_head`, Wandvariante `blaze_wall_head`): Mob-Kopf wie die Vanilla-Koepfe, als
Vanillas `SkullBlock`/`WallSkullBlock` mit eigenem Kopf-Typ `BlazeHeadType` (`simplebuilding:blaze`).
Setzbar (16 Drehungen, Wand), tragbar (`equippable` Kopf, `#minecraft:skulls` fuer die Flueche),
Notenblock spielt das Lohen-Geraeusch (Instrument `CUSTOM_HEAD` + Item-Komponente `note_block_sound`).
Keine Redstone-Animation (Vanillas Kopfwuerfel hat keine bewegten Teile).
- **Quelle**: nur eine Lohe, die von der Explosion eines **geladenen Creepers** stirbt, genau wie die
  Vanilla-Koepfe (ein Kopf je Explosion - `Creeper#killedEntity` rollt `charged_creeper/root` nur
  einmal). Umgesetzt als zusaetzlicher Pool in `minecraft:charged_creeper/root` mit Bedingung "Opfer ist
  eine Lohe" (`ModLootTableModifications#blazeHeadPool`), unabhaengig vom Schalter
  `enableLootTableChanges`, weil der Kopf die einzige Quelle fuer das Trank-Pad ist.
- **Technik**: `SkullBlockEntityTypeMixin` laesst den Lohenkopf als gueltigen Block des Vanilla-Typs
  `minecraft:skull` zu (sonst verweigert `BlockEntity#validateBlockState` das Setzen); das Modell
  (Vanillas Mob-Kopf-Wuerfel, `SkullModel.createMobHeadLayer`) und die Textur
  `textures/entity/blaze_head.png` haengt `SkullModelMixin` an `SkullBlockRenderer` - das nutzen
  Block, Item-Modell (`minecraft:head`, `kind: simplebuilding:blaze`) und der getragene Kopf.
- **Textur**: neue Pixelkunst (64x32, Kopf bei UV 0,0), Glutgesicht mit Brauenkante und Lohenaugen.

Namen (en/de): "Potion Pad I" / "Trank-Pad I", "Reinforced Potion Pad II" / "Verstaerktes Trank-Pad II",
"Infused Potion Pad III" / "Durchtraenktes Trank-Pad III", "Blaze Head" / "Lohenkopf". Kreativ-Tab:
Zeile `potion_pads` (Lohenkopf + drei Stufen) am Ende der Pad-Zeilen; JEI-Infoseiten `potion_pad` und
`blaze_head` (inklusive Herkunft des Kopfes); Testzentrale: Station mit Trank-Pad und einer Truhe
Wurftraenke.

Namen (en): "Elytra Pad I", "Reinforced Elytra Pad II", "Netherite Elytra Pad III",
"Enderite Elytra Pad IV", "Fine Elytra Pad V"; "Flypad I", "Reinforced Flypad II", "Stellar Flypad III"
(alt: "Netherite Flypad (Legacy)", "Enderite Flypad (Legacy)" - bis 2026-09-28 "Old ... Flypad"); "Spawn Teleporter I" ... "IV",
"Spawn Teleporter V" (Besitzer 2026-09-28, vorher "Enderite Spawn Teleporter V"); "Launchpad I", "Netherite Launchpad II", "Enderite Launchpad III";
"Chunk Loader I", "Netherite Chunk Loader II", "Enderite Chunk Loader III".

### 2.4 Druckplatten wie Vanilla (Besitzer-Aenderung 2026-09-27)

- **Gewachste Kupferplatten**: `waxed_copper_pressure_plate`, `waxed_exposed_…`, `waxed_weathered_…`,
  `waxed_oxidized_copper_pressure_plate` ("Waxed … Copper Pressure Plate" / "Gewachste … Kupfer-Druckplatte").
  Honigwabe wachst (Vanilla-Partikel und -Geraeusch, eine Wabe), gewachste Platten oxidieren nicht; die
  Axt kratzt zuerst das Wachs ab (Wachs-ab-Partikel und -Geraeusch), ungewachst wie bisher eine
  Oxidationsstufe. Wachsen geht auch an der Werkbank (Platte + Honigwabe,
  `<gewachste Platte>_from_honeycomb`). Texturen und Modelle sind die der ungewachsten Stufe (wie Vanilla).
  Die Folge steht in `CopperPressurePlateBlock` selbst - auf allen Loadern gleich, ohne
  `OxidizableBlocksRegistry`/Datenkarten; der Besitzer bleibt beim Wachsen und Abkratzen erhalten.
  Schleichen + Honigwabe/Axt geht (wie bisher beim Abkratzen) am Block vorbei an das Vanilla-Item und tut nichts.
- **Loslassen verzoegert**: nach dem Heruntergehen bleibt die Kupferplatte so lange gedrueckt, wie das
  Ausloesen dauerte (1-4 s); wer zurueckkommt, haelt sie gedrueckt.
- **Sichtbar gedrueckt**: alle Mod-Druckplatten (Diamant, Netherit, Enderit, Kupfer und gewachstes Kupfer)
  zeigen bei `powered=true` das Modell `pressure_plate_down` und sind gedrueckt nur einen halben Pixel hoch -
  vorher blieben alle Platten optisch oben.
- **Kreativ-Tab** (Maschinen & Lager): alle Vanilla-Druckplatten neben den Mod-Platten, nach Material mit
  aufsteigender Stufe: Holz (12, ab MC 26.3 mit Pappel 13; zwei Zeilen), Stein + polierter Schwarzstein, Kupfer (vier Stufen, dann
  gewachst), dann schwere (Eisen) und leichte Waegeplatte (Gold), Diamant, Netherit, Enderit. Launchpads und
  Chunk-Loader haben je eine eigene Zeile (vorher gemeinsam "travel_and_loading"); in der Elytra-Pad-Zeile stehen
  erst die Pads I-V, dann die Spawn-Elytra. SimpleTools: eine Geraete-Zeile (siehe Abschnitt 3).

## 3. Echolot / Echo Sounder, frueher Echo-Kompass (Phase 3b, Umbau 2026-09-27)

- **Name** (Besitzer 2026-09-27, zweite Runde): en "Echo Sounder", de "Echolot". Die Registry-Id bleibt
  `simplebuilding:echo_compass` (alte Welten, Rezept-/Modell-/Textur-Pfade, Lang-Schluessel
  `item.simplebuilding.echo_compass*`), nur die angezeigten Texte (Name, Meldungen, JEI, Testzentrale,
  Wiki) sagen Echolot. Unten steht aus historischen Gruenden oft noch "Kompass".
- Eigenes Item `simplebuilding:echo_compass` (vorher: Vanilla-Kompass mit `custom_data` aus einem
  Fremd-Datenpaket). Neu geschrieben, kein Code aus dem AGPL-Datenpaket uebernommen.
- Rechtsklick auf einen Leitstein verknuepft (Vanilla-Komponente `lodestone_tracker`, der Kompass
  zeigt wie ein Leitsteinkompass dorthin). Leitstein weg -> Verknuepfung erlischt wie in Vanilla.
- **Aufladen** (Besitzer 2026-09-27): Benutzen gedrueckt halten, 3 s (60 Ticks, Bogen-Animation). Wer
  vorher loslaesst, springt nicht und verliert nichts (keine Ladung, keine Abklingzeit).
  Server-Effekte je Ladetick (`EchoCompassItem#chargeEffects`): Sculk-Seelen kreisen in drei Armen von
  2,4 auf 0,9 Bloecke enger, weit gestreute Portalpartikel (Streuung 1,6) ziehen hinein ("weiter
  gestreut", 2026-09-27; vorher zwei Arme 1,2 -> 0,4 und Streuung 0,7), Amethyst-Resonanz steigt von tief nach hoch, Sculk-Klicken und
  Seelenanker-Aufladen an den Dritteln, Warden-Schallladen zum Schluss. Client: FOV-Sog bis 12 %
  enger mit leichtem Puls (`tweaks.client.EchoCompassFov`), skaliert mit Vanillas
  Barrierefreiheitsregler "FOV-Effekte" (0 = aus); Anbindung per `EchoCompassFovMixin`
  (`simplebuilding.tweaks.mixins.json`, auf allen Loadern einschliesslich Forge, das die Konfig per
  Manifest `MixinConfigs` laedt). Einen eigenen `ComputeFovModifierEvent`-Hoerer auf Forge gab es bis
  2026-09-28; er wirkte zusaetzlich zum Mixin (Faktor doppelt) und ist entfernt.
- Sprung am Ende der Ladung auf den Block ueber dem Leitstein, **ohne Enderperle** (bis 2026-09-27
  kostete er eine; jetzt bezahlt allein die Haltbarkeit), 6 s Abklingzeit, Schallknall +
  Seelenanker-Klang + Partikel bei der Ankunft (Wolke mit Streuung ~1,4 Bloecke plus ein Ring aus
  Sculk-Seelen, der am Boden nach aussen laeuft), Rueckwaerts-Portal-Wolke am Abflugort (Streuung 1,1). Effekte wie im Datenpaket (Blindheit 1 s, Leuchten 3 s,
  Sanfter Fall 1 s, Langsamkeit 1 s, Uebelkeit 6 s).
- **Haltbarkeit** (Besitzer 2026-09-27): 1500 Punkte. Ein Sprung leert den Kompass ganz (Schaden
  1500 = "zerbrochen"); Unbreaking wirkt je Punkt ueber `EnchantmentHelper#processDurabilityChange`
  (Unbreaking III: im Mittel nur ~375). Nicht voll repariert (Schaden > 0): Riss-Textur in drei Stufen
  nach Schadensanteil (`minecraft:damaged` + `minecraft:damage`), kein Glanz (`isFoil` nur bei
  Schaden 0, auch verzaubert), Tooltip mit Ladestand. Aufladen: Mending (2 Punkte je XP-Punkt,
  750 XP im leeren Zustand) oder Amboss mit Echoscherben (`repairable(ECHO_SHARD)`, Vanilla: je
  Scherbe ein Viertel, vier fuellen ihn). Erst bei Schaden 0 springt er normal.
- **Bruch provozieren**: Benutzen im nicht voll reparierten Zustand laedt doppelt so lange (6 s) mit
  Warnzeichen (Knacken dichter werdend, Funken, Rauch, Sculk-Kreischer zur Haelfte, alles lauter);
  der Sprung gelingt, danach zerspringt der Kompass (Item weg, Vanilla-Bruchereignis + Glasbruch).
  Unbreaking rettet ihn dabei nicht. Kreativmodus: weder leeren noch zerspringen.
  Alte Kompasse aus Welten vor dem Umbau (Schaden 1..63 bei Maximum 64) gelten jetzt als nicht voll
  repariert.
- Textur: eigene Pixelkunst (`tools/textures/echo_compass_textures.py`, von `generate_textures.py`
  eingebunden): Kompass-Gehaeuse mit Bergungskompass-Farben, 32 gerasterte Nadelstellungen
  (`echo_compass_00..31`, Zaehlung wie Vanilla) und `echo_compass_cracked_0..2` (0 = leer).
- Dimensionen: jede Dimension, die der Server kennt (auch Mod-Dimensionen - war trivial, weil die
  Vanilla-Komponente die Dimension mitfuehrt).
- Kreativ-Tab: SimpleTools, Zeile "gadgets" (seit 2026-09-27: Kompass, Bergungskompass, Echolot, Geschwindigkeitsmesser, Erzdetektor, Magnet, Rotator, Amethystlinse, Oktant; danach die gefaerbten Oktanten).
- Rezept (Werkbank, geformt, Besitzer 2026-09-27; zweite Runde: auch oben mittig ein Nugget, 7 Nuggets):

  ```
  NNN
  NRN
  NEN
  ```
  N = Enderit-Nugget, R = Bergungskompass, E = Enderit-Kern.

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

Branch `remove-ported-features` im Repo `simpletweaks` (abgezweigt von `1.21.11`; `master` und
`1.21.11` unberuehrt, nichts gepusht):

- `d56621b` - uebernimmt die bis dahin uncommitteten Aenderungen des Arbeitsbaums unveraendert,
  damit das Entfernen auf dem Stand aufsetzt, der auch portiert wurde.
- `9882906` - entfernt alles Portierte. Es bleiben: `Simpletweaks` (nur noch Items, Claim-Schutz,
  Befehle), `ClaimState`, `ClaimProtectionHandler`, `ClaimDeedItem`, `ModItems` (nur `claim_deed`),
  `ModCommands` (nur `/claim`), `ModModelProvider` (nur das Urkunden-Modell), Textur/Lang der Urkunde,
  Icon und Readme-Bilder. Cloth Config, Mod Menu, die Client-Einstiegsklasse und die Mixins
  entfallen. `gradlew build` und `runDatagen` laufen gruen.

## 7. Abweichungen von Simple Tweaks und behobene Fehler

- Exakter Spawn und eigener Weltspawn sind standardmaessig **aus** (in Simple Tweaks an); sonst
  haette das Einspielen von SimpleBuilding jeden bestehenden Server umgestellt.
- Kein Fallschaden im Spawnbereich gilt nur, wenn auch `giveElytraOnSpawn` an ist (sonst waere der
  Spawn ohne Elytra-Funktion fallschadenfrei).
- Boost-Paket: der Server prueft, ob der Spieler wirklich gleitet (vorher konnte ein manipulierter
  Client jederzeit boosten).
- Der Flugzeit-Timer einer Pad-Elytra laeuft auch, wenn die Spawn-Elytra abgeschaltet ist (vorher
  blieb sie dann ewig).
- Erstbeitritt schenkt Teleporter **und** Elytra-Pad (vorher ueberschrieb das zweite Geschenk das erste
  bei vollem Slot). Seit 2026-09-26 zwei getrennte Mengen `spawn.firstJoinTeleporterCount` und
  `spawn.firstJoinElytraPadCount`, **beide Standard 0** (Besitzer-Wunsch; Simple Tweaks: ein Wert
  `spawnTeleporterCount`, Standard 1). Ein alter `spawnTeleporterCount` in einer bestehenden
  Config-Datei wird beim Laden (`SimplebuildingConfig.validatePostLoad`) fuer beide uebernommen, ausser
  er steht auf dem alten Standard 1 - der wird zu 0; beim naechsten Speichern verschwindet der alte
  Schluessel. Befehle: `/simplebuilding tweaks spawn teleporterCount|elytraPadCount <0-64>`. Ist die
  Familie abgeschaltet (`pads.enableSpawnTeleporters` bzw. `pads.enableElytraPads`), faellt ihr Geschenk weg.
- Flypad: wird es abgebaut oder abgeschaltet, verlieren Spieler im Bereich den Flug (vorher behielten
  sie ihn) - seit dem Audit 2026-09-26 aber nicht, solange ein anderes Flypad sie noch abdeckt, und nur
  Flug, den ein Flypad gab (Spieler-Tag `simplebuilding.flypad_flight`).
- Chunk-Loader: nach Serverneustart wieder aktiv, und er gibt nur Chunks frei, die er selbst erzwungen
  hat (vorher konnte er fremd erzwungene Chunks freigeben). Seit dem Audit 2026-09-26 auch nach
  `/setblock`/`/fill` (Freigabe in `setRemoved`), und bei Ueberlappung uebernimmt ein anderer Loader die
  gemeinsamen Chunks.
- Spawn-Elytra (Audit 2026-09-26): nur in der Weltspawn-Dimension; Fallschutz im selben Quadrat wie die
  Elytra-Vergabe (Simple Tweaks: Kreis); sie verschwindet beim Fallenlassen, kein Container/Buendel
  nimmt sie, Aufraeumen von Inventar/Cursor jeden Tick. Flugzeit hoechstens 24 h, Boosts hoechstens 100.
- Config-Abgleich (Audit 2026-09-26 #16): `rocketStackSize`, `maxBoosts`, Laser-Schalter und -Reichweite
  schickt der Server beim Einloggen und nach jedem `/simplebuilding tweaks`-Befehl (`TweaksConfigPayload`).
- Laserpointer -> **Amethystlinse** (2026-09-27; Registry-Id bleibt `laser_pointer`, alte Welten laden):
  - Rezept `RAR`/`ICI`/`III` (Redstone, Amethystsplitter, Eisenbarren, Eisen-Baukern `iron_core`), kein Glas.
  - Punkt: feste Weltgroesse (Config `scale`, auf 0,05..1 begrenzt), waechst erst ab ~3 Pixel
    Bildschirmgroesse mit (0,004 Bloecke je Block) - frueher 0,12 je Block, also ein Riesenkreis in der
    Ferne. Entfernungszahl 13 statt 10 GUI-Pixel neben der Fadenkreuzmitte.
  - Strahlwirkungen (`LaserBeam`, Server, bis zur Linsen-Reichweite - seit 2026-09-27, vorher fest 24 Bloecke;
    auf dem Server hoechstens die Sichtweite (mind. 2 Chunks), damit der Strahl keine Chunks laedt; mehr als
    `range + 8` entfernt wirkt nichts - Verweildauer auf derselben Blockseite, Zeiten bis 5 Bloecke Abstand): Eis/
    Frosteis -> Wasser (2 s; verdampft, wo Wasser verdampft), Packeis -> Eis, Blaueis -> Packeis
    (eine Stufe statt Wasser aus Bloecken, die in Vanilla nie schmelzen); Schnee/Schneeblock/Pulverschnee
    -> weg; Lagerfeuer, Seelenlagerfeuer, Kerzen, Kerzenkuchen an (1 s); Seelensand/-erde oben
    Seelenfeuer (2 s); Brennbares (Zuendwert der Feuer-Tabelle > 0, per Invoker `FireBlockInvoker`)
    faengt nach 3 s Feuer auf der angestrahlten Seite, nur wo `fire_spread_radius_around_player`
    Ausbreitung erlaubt (Seelenfeuer/Lagerfeuer wie Feuerzeug ohne diese Regel); Zusatzwirkung: nasser
    Schwamm trocknet (5 s). **TNT** (seit 2026-09-27, Besitzer): nach 3 s Verweildauer (wie Brennbares)
    gezuendet - `PrimedTnt` mit dem Spieler als Zuender wie beim Feuerzeug, Block weg, 5 Ladung; beachtet
    die Spielregel `tnt_explodes` (aus: kein Verweilen, nichts passiert, keine Ladung weg) und den Schutz
    unten, nicht aber `fire_spread_radius_around_player` (es entsteht kein Feuer) und nicht `mob_griefing`
    (gilt nur fuer Mobs). Nie Netherportale (kein Feuer in einen leeren Portalrahmen - `BaseFireBlock#onPlace`
    wuerde ihn fuellen). Schutz: `mayInteract` (Spawnschutz, Weltgrenze) und
    `mayUseItemAt` (Abenteuermodus) am Block und am Feuerplatz.
  - **Abstand** (2026-09-27): Verweildauer = Basis x (1 + 0,096 x max(0, d - 5)^0,773), d = Auge bis Punkt
    (`LaserBeam#dwellTicks`). Bis 5 Bloecke Basis (Anzuenden 3 s), 10 Bloecke ~4 s, 200 Bloecke ~20 s; die
    Kurve ist so gewaehlt, dass sie genau die drei Vorgaben des Besitzers trifft, und waechst stetig weiter.
  - **Lebewesen** (2026-09-27): Spieler und Mobs brennen 4 s (`igniteForSeconds`), nach der doppelten
    Verweildauer eines brennbaren Blocks im selben Abstand, 5 Ladung. Nicht: feuerfest, unverwundbar, nass
    (Wasser-/Regen-Merker oder Wasserblock an der Position), Spieler im Kreativ-/Zuschauermodus (Faehigkeit
    `invulnerable`), andere Spieler nur mit `Player#canHarmPlayer` (Spielregel `pvp`, Server-PvP, Team-
    Freundfeuer). Entity-Suche entlang des Strahls bis 64 Bloecke (`ProjectileUtil#getEntityHitResult`).
  - **Klaenge am Trefferpunkt** (2026-09-27, Vanilla-Events, positionsgebunden): Summen (Leuchtfeuer-Summen,
    hoch, leise) alle 20 Ticks, solange etwas getroffen wird; waehrend der Verweildauer alle 8 Ticks Zischen
    (Feuer-Loeschen, hoch; Schmelzen/Trocknen) bzw. Knistern (Feuer + Lagerfeuer-Knistern; Anzuenden,
    Seelenfeuer, Lagerfeuer/Kerzen, TNT, Lebewesen); Abschlussklaenge wie bisher. Test-Haken
    `LaserBeam#setSoundHook`.
  - Ladung = Haltbarkeit 640: 1 je angefangene Sekunde Benutzen - auch blosses Zeigen ins Leere oder auf
    einen Block ohne Wirkung, und schon im ersten Tick (bis 2026-09-27 erst nach 20 Ticks, kurzes Antippen
    war dadurch gratis) -, 5 je Wirkung, kreativ gratis; zerbricht nie, leer
    kein Strahlen mehr (Modell `item/laser_pointer_empty` mit eigener Textur
    `textures/item/laser_pointer_empty.png`, auf beiden Linien vorhanden). Amboss + Redstone laedt auf,
    0 Stufen (`AnvilScreenHandlerMixin`, auch `mayPickup`), 10 je Staub, 64 = voll, nur Noetiges wird verbraucht.
  - Kreativ-Tab: Werkzeuge, Zeile "gadgets" (nicht mehr bei den Chunk-Loadern).
  - Entfernungsanzeige misst bis zum Laserpunkt (Audit #34).
- Echo-Kompass: ein blockierter Sprung kostet weder Haltbarkeit noch Abklingzeit (Perlen braucht er nicht mehr).
- Launchpad: beim Abbau fallen die geladenen Windkugeln heraus. Eigener Weltspawn per Befehl gilt sofort.
- Stufen (2026-09-27): Launchpad und Chunk-Loader haben je drei Stufen, die Aufwertungen kosten
  Druckplatten, Flypad I Enderit-Druckplatte + Enderit-Kern (Enderit-Vorlage; die Elytra kostet das
  Elytra-Pad I), Launchpad laedt schleichend alle Windkugeln (Abschnitte 2.1-2.3).
- XP-Verklumpen: beim Zusammenlegen ging Erfahrung verloren (Anzahl der Kugeln wurde ignoriert) -
  behoben, Obergrenze `Short.MAX_VALUE` je Kugel.
- Kupfer-Druckplatten behalten beim Oxidieren/Abkratzen ihren Besitzer und melden den Block darunter an.
- Echo-Kompass neu geschrieben (kein AGPL-Code), Unbreaking/Mending wirken, jede Dimension; 2026-09-27
  umgebaut (Aufladen, Leeren/Aufladen/Zerspringen, eigene Textur, Abschnitt 3).
- Enderit-Texturen (Pads, Teleporter, Druckplatte, Chunk-Loader, Launchpad) seit 2026-09-26 neu gezeichnet
  (vorher Umfaerbungen der Stufe III): Enderit-Rahmen mit Eckbeschlaegen und Glimmer wie die
  Enderit-Maschinen, Motiv je Familie; Karten in `tools/textures/generate_textures.py`
  (`ENDERITE_TWEAK_MAPS`). Abnahme durch den Besitzer offen.
- Nicht uebernommen: die Timer-Anzeige der Spawn-Elytra ueber der Hungerleiste (doppelt zur
  Boost-Leiste) und die Ergaenzung von `TRIMMABLE_ARMOR`/`TRIM_MATERIALS`-Tags.
- JEI: jede Familie (Pads, Platten, Teleporter, Chunk-Loader, Launchpad, Spawn-Elytra, Laser,
  Echolot) hat eine Infoseite (`jei.simplebuilding.info.*`), das Wiki einen Abschnitt
  "Simple Tweaks" mit Notizen je Block.

## 8. Tests

`TweaksTests` (Katalog `SimpleBuildingGameTests`, Fabric-Adapter `TweaksGameTest`, Methode = Test-ID
`simplebuilding:tweaks_*`), in beiden Codelinien: Besitzer und Abbau, Pad-Bereiche je Stufe,
Elytra-Pad/Flypad/Teleporter/Launchpad/Chunk-Loader inklusive Enderit-Zusatz, Filter- und
Kupferplatten, Echolot (Name/Id, Verknuepfen ohne Perle, Unbreaking, Aufladen/Loslassen, Leeren, Aufladen per
Mending, Zerspringen, Rezept mit 7 Nuggets), Amethystlinse (Wirkungen, TNT nach Verweildauer mit Regeln,
Portal nie, Ladung schon beim ersten Tick und beim Zeigen ins Leere, Verweildauer nah/fern/200 Bloecke,
Lebewesen nach doppelter Zeit, kein Spieler ohne PvP, Klaenge ueber den Test-Haken),
Geschwindigkeitsmesser-Rezept (`QAQ`/`NCN`/`NKN`: Quarz, Amethystsplitter, Kupfernugget, Kompass, Kupferkern), XP-Verklumpen, Stapelgroessen,
Spawn-Regeln, Befehle (seit 2026-09-28 auch `/killcarts` ueber den echten Befehlsbaum: Schalter,
Operator-Sperre, Modi, Inhalt der Kistenlore), Pads unter Wasser (jede wasserloggbare Platte ueber den
echten Setz-Weg), Config-Schalter je Familie. Jeder Test wurde gegengeprueft (Mutation des
geprueften Verhaltens macht ihn rot).

`TweaksTierTests` (Fabric-Adapter `TweaksTierGameTest`, Test-ID `simplebuilding:tweaks_tier_game_test_*`),
beide Codelinien: Launchpad-Fassungsvermoegen je Stufe, Laden aller Windkugeln beim Schleichen (ueber
den ganzen Vanilla-Weg `ServerPlayerGameMode#useItemOn`), Schub-Gleichwertigkeit (16 = alte 32) samt
echtem Start, Welt-Upgrade alter Launchpads (Ueberschuss faellt heraus), Chunk-Loader-Bereiche 1/5/9,
Uebergabe mit Kreuzform, Druckplatten-Rezepte samt Wegfall der Rohstoff-Wege, Elytra-Pad- und
Flypad-Bereiche je Stufe (Geometrie und echter Durchlauf), Umbau alter Flypads in Welt und Inventar,
Endstufe je Familie. Gegenprobe 2026-09-27: jede der 16 Mutationen (Fassungsvermoegen, Schleich-Laden,
Schub, Ueberschuss-Auswurf, Kreuzform, Flypad-Umbau, Item-Tausch, Bereiche, Sicherheitsnetz,
Familienreihenfolge, fuenf Rezept-JSONs) machte ihre Tests auf allen drei Linien rot.

`PotionPadTests` (Fabric-Adapter `PotionPadGameTest`, Test-ID `simplebuilding:potion_pad_game_test_*`),
beide Codelinien: Speichern (echter Wurf und Treffer), Ersetzen durch einen Verweiltrank, Leerwischen
mit Wasser, Aufladen 25/50/100 % von 30/60/120 s mit der Verstaerkung des Tranks (Abbruch behaelt das
Erhaltene, startet keine Abklingzeit, Neustart bei 0), Sofortwirkung einmal beim 3-s-Schritt,
Abklingzeit 2x Wirkdauer je Stufe ohne Wirkung und mit Tick-Ablauf, Abbau in der Abklingzeit
(Restzeit als Komponente, Trank und Easter-Stufe bleiben, nicht stapelbar, laeuft gesetzt weiter),
Stapelgroesse 1 aller Pad-Items (Gegenprobe Druckplatten 64), Rezepte aller Stufen, ein
Lohenkopf je Explosion eines geladenen Creepers und keiner bei anderem Tod. Gegenprobe Abklingzeit/
Aufladen 2026-09-28 (Fabric 26.2 und 1.21.11): Faktor 3 statt 2, Stufen 50/50/100, keine Sperre in
der Abklingzeit, Restzeit nicht auf das Item, Restzeit beim Setzen verworfen, Abklingzeit zaehlt nicht,
kein `stacksTo(1)` - jede machte ihre Tests rot. Gegenprobe 2026-09-28: fuenf
Mutationen (Speichern aus, Dauer 30/30/30, Sofortwirkung ohne Betreten-Pruefung, Pool an der
Lohen-Beutetabelle statt an charged_creeper, Rezept-JSON mit Enderitbarren) machten alle sechs Tests auf
allen drei Linien rot.

Testzentrale: eigene Station `tweaks` (`com.simplebuilding.dev.testcentre.TweaksStation`, siehe
`docs/TESTZENTRALE.md`); Gegenprobe: Zeile `travel_and_loading` (seit 2026-09-27 `launchpads` und
`chunk_loaders`) weggelassen -> Abdeckungstest rot (nennt Chunk-Loader, Enderit-Launchpad, Laserpointer).

`PressurePlateTests` (Fabric-Adapter `PressurePlateGameTest`, Test-ID `simplebuilding:pressure_plate_game_test_*`),
beide Codelinien: Wachsen jeder Stufe (Wabe verbraucht, Besitzer bleibt, kein Zufallstick, keine naechste
Stufe, zweite Wabe wirkungslos), Axt erst Wachs dann Oxidation, Werkbank-Rezepte, symmetrische
Loslass-Verzoegerung (echte Ticks), sichtbar gedrueckte Platten (Form und `_down`-Modell je Mod-Platte aus
der Registry) und Namen im Vanilla-Muster; dazu das Tab-Layout in `DataIntegrityTests`.

## 9. Offene Punkte

- Laser-Option `showLine` hat weiter keine Wirkung (wie in Simple Tweaks); im Config-Bildschirm ausgeblendet.
- Enderit-Texturen vom Besitzer pruefen lassen.
- Claim-System (Abschnitt 4) bei Bedarf spaeter portieren.

## 10. SPOILER: die versteckte Kette ueber den Endstufen (Besitzer 2026-09-27)

> **Spoiler.** Dieser Abschnitt verraet ein Easter Egg. Das oeffentliche Wiki erwaehnt es nur mit
> "manches ist versteckt"; Rezepte, Items und Advancements stehen dort bewusst nicht
> (`wiki/generate.py`: `SECRET_ITEMS`, `SECRET_RECIPE_PREFIX`).

**Idee (Besitzer):** Jede Pad-Familie laesst sich auf ihrer Endstufe im Schmiedetisch zurueck auf
Stufe I schmieden. Das Ergebnis heisst **"Don't do it"** - der Spieler wurde gewarnt. Es ist der
normale Stufe-I-Block mit derselben Funktion, nur ein Easter Egg. Wer weitermacht, steigt eine eigene
Kette hinauf, deren letzte Stufe doppelt so stark ist wie die echte Endstufe, und kann sie zuletzt zu
einem Stock schmieden.

**Familien:** Elytra-Pad (5 Stufen), Spawn-Teleporter (5), Flypad (3), Launchpad (3), Chunk-Loader (3),
Trank-Pad (3, seit 2026-09-28).
Die Druckplatten-Familie (`TweaksFamilies.Family.PRESSURE_PLATE`) hat keine Kette: Platten sind keine
Pads, haben keine Kraft zum Verdoppeln und sind selbst Zutat der Pad-Aufwertungen.

| Easter-Stufe | Block | Name (en) | Name (de) | Verhalten |
|---|---|---|---|---|
| 1 | Stufe I | Don't do it | Tu es nicht | wie Stufe I |
| 2 | Stufe II | Seriously? | Echt jetzt? | wie Stufe II |
| 3 | Stufe III | Stop. Please. | Hoer auf. Bitte. | wie Stufe III (nur 5-stufige Familien) |
| 4 | Stufe IV | Last Chance | Letzte Chance | wie Stufe IV (nur 5-stufige Familien) |
| letzte (5 bzw. 3) | Endstufe | Name der Endstufe, verschleiert | dito | **doppelt so stark** |

Die Namen der letzten Easter-Stufe stehen als Formatcodes in den Sprachdateien
(`item.simplebuilding.easter.final.<familie>`): Farbverlauf je Familie (Elytra-Pad Gold-Gelb-Weiss,
Flypad Violett-Magenta-Weiss, Spawn-Teleporter Blau-Aqua-Weiss, Launchpad Tuerkis-Aqua-Weiss,
Chunk-Loader Rot-Orange-Gelb, Trank-Pad Violett-Rosa-Aqua), fett, zwei verschleierte Zeichen (die Stufenziffer und ein Buchstabe
in der Mitte); sichtbar genau so viele Zeichen wie der normale Name. Deutsch uebersetzt verspielt, die
Endnamen folgen den deutschen Blocknamen.

**Doppelte Kraft der letzten Easter-Stufe:**

| Familie | Endstufe | letzte Easter-Stufe |
|---|---|---|
| Elytra-Pad V | 128x128, 127 hoch | 256x256, 254 hoch |
| Flypad III | 16x16x24 | 32x32x48 |
| Spawn-Teleporter V | 3 s stillstehen | 1,5 s |
| Launchpad III | 16 Windkugeln | 32 Windkugeln (Schub pro Ladung unveraendert) |
| Chunk-Loader III | 3x3 Chunks | 5x5 Chunks (Radius verdoppelt) |
| Trank-Pad III | Wirkdauer 120 s | 240 s (Verstaerkung des Tranks unveraendert) |

**Kosten (Schmiedetisch):** jeder Schritt kostet, was die normale Stufe kostet - dieselbe Vorlage,
dieselbe Zutat (`EasterEggs#steps`, gleiche Liste wie `ModRecipeProvider#buildTweaksRecipes`):

- Einstieg (Endstufe -> "Don't do it"): Vorlage und Hauptzutat der Stufe I, die Endstufe als Basis.
  Elytra-Pad: beliebige Vorlage + **Elytra**; Flypad: Enderit-Vorlage + Enderit-Kern; Spawn-Teleporter:
  beliebige Vorlage + Diamantblock; Launchpad und Chunk-Loader: beliebige Vorlage + Diamant-Druckplatte;
  Trank-Pad (Stufe I entsteht an der Werkbank, ohne Vorlage): wie beim Elytra-Pad beliebige Vorlage +
  Hauptzutat, also **Lohenkopf**. Danach Enderit-Vorlage + Enderit-Druckplatte, dann Enderit-Vorlage +
  Enderit-Kern.
- Easter-Stufe n -> n+1: Vorlage und Zutat der normalen Aufwertung von Stufe n auf n+1
  (Flypad II -> III also ein zweites Flypad II).
- **Funny Stick:** Netherit-Vorlage + letzte Easter-Stufe + **Netheritbarren**.

**Funny Stick** (`simplebuilding:funny_stick`): ein Stock (Vanilla-Stockbild, in der Hand wie ein
Werkzeug), episch, Verzauberungsglanz, nicht stapelbar, feuerfest. In Haupt- oder Nebenhand steigt alle
4 Ticks ein Endstab-Funke auf, ab und zu eine Note oder ein Dorfbewohner-Glitzern (serverseitig, alle
in der Naehe sehen es). Sonst tut er nichts.

**Advancements** (alle `hidden`, eigener Tab "What have you done?" mit End-Hintergrund, der erst mit dem
ersten erscheint; Toast und Chatmeldung):

| Id | Titel (en / de) | Ausloeser (inventory_changed) |
|---|---|---|
| `simplebuilding:easter/what_have_you_done` | What have you done? / Was hast du getan? | ein Item mit Easter-Stufe 1 |
| `simplebuilding:easter/seriously` | Seriously? / Echt jetzt? | ein Item mit Easter-Stufe 2 |
| `simplebuilding:easter/it_was_worth_it` (Challenge) | It Was Worth It / Es hat sich gelohnt | die letzte Easter-Stufe einer Familie (je Familie ein Kriterium, eines reicht) |
| `simplebuilding:easter/all_that_for_a_stick` (Challenge) | All That for a Stick? / Alles fuer einen Stock? | der Funny Stick |

**Technik:**

- Keine neuen Bloecke: die Item-Komponente `simplebuilding:easter_stage` (1..5) auf dem normalen
  Stufenitem, dazu ein `item_name` aus der Stufe (`EasterEggs#mark`). Nur Stufen, die zum Block passen
  (Stufe n = Block der Stufe n), zaehlen (`EasterEggs#fits`).
- Gesetzt: `OwnedBlockEntity` liest die Komponente in `applyImplicitComponents` und speichert
  `EasterStage`; `collectImplicitComponents` gibt Stufe und Name fuer Strg+Mittelklick zurueck.
  Abgebaut: `PadBlock#getDrops` schreibt die Stufe auf das Item aus der Loot-Tabelle (auch bei
  Explosionen). Ein Amboss setzt nur `custom_name`, nie die Komponente: ein umbenanntes normales Pad ist
  kein Easter-Pad.
- Rezepte: eigener Serializer `simplebuilding:easter_smithing` (`EasterSmithingRecipe`), prueft die
  Easter-Stufe der Basis; Spezialrezept ohne Anzeige (kein Rezeptbuch, keine Freischalt-Meldung, JEI
  zeigt fremde Schmiede-Klassen ohne Erweiterung nicht). Weil die Kette dieselben Zutaten nimmt wie die
  normalen Aufwertungen, lehnt `SmithingTransformEasterGuardMixin` (in `simplebuilding.tweaks.mixins.json`,
  laedt auf Fabric, NeoForge und Forge) jede Basis mit Easter-Stufe fuer normale Umwandlungsrezepte ab.
  Rezepte unter `recipe/easter/`, 28 Stueck.
- Advancements per Datagen (`EasterEggData`, aus `ModRecipeProvider`): sie reisen ueber
  `RecipeOutput#accept` mit den Elytra-Pad-Rezepten, damit dieselbe Datei auf 26.2 und 26.3 baut.
- Versteckt: kein Kreativ-Tab, `c:hidden_from_recipe_viewers` (Funny Stick; Easter-Pads sind nur
  Komponenten-Varianten und stehen nirgends), Testzentrale `TestCentreLayout.EXCLUDED`, Wiki-Filter.
- Doppelte Kraft: `EasterEggs#isBoosted(level, pos)`, abgefragt in `ElytraPadBlockEntity#areaOf`,
  `FlypadBlockEntity#areaOf` (auch fuer die Uebergabe zwischen Flypads), `LaunchpadBlock#capacityAt`,
  `ChunkLoaderBlockEntity` (Radius 2, `MAX_RADIUS` = 2), `SpawnTeleporterBlockEntity#requiredTicks(level, pos, tier)`
  und `PotionPadBlock#effectDurationAt(level, pos)` (Aufladen, Abklingzeit = doppelte Dauer, Rechtsklick-Anzeige).
- Trank-Pad: der gespeicherte Trank reist als `potion_contents` mit dem Item (`PotionPadBlock#getDrops`,
  `PotionPadBlockEntity#collectImplicitComponents`) und kommt beim Setzen zurueck
  (`applyImplicitComponents`), neben der Easter-Stufe; das gilt auch fuer normale Trank-Pads.

**Tests:** `TweaksEasterTests` (Fabric-Adapter `TweaksEasterGameTest`, Ids
`simplebuilding:tweaks_easter_game_test_*`), beide Codelinien: Einstieg je Familie mit echtem Setzen,
Abbauen und Wiedersetzen, Stufe-I-Verhalten, Amboss-Umbenennung; ganze Kette je Familie (Namen, Stufen,
gleiche Kosten wie die normale Stufe, kein normales Rezept nimmt ein Easter-Pad, Sprachdateien mit
Formatcodes); doppelte Kraft je Familie (Trank-Pad: 240 s am Spieler, 480 s Abklingzeit, Setzen/Abbauen behaelt Stufe und Trank);
Advancements (versteckt, Kette, Ausloeser); Funny Stick
(nur aus der letzten Stufe, frisches Item, Funken); Unsichtbarkeit (Tag, Kreativ-Tabs, Rezepte,
Testzentrale).
