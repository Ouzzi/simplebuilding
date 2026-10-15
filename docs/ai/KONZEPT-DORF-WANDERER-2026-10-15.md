# Konzept: Dorf-Wanderer (Queue Nachtrag 59, 2026-10-15)

Nur Konzept, kein Code. Quelle: Queue Nachtrag 59 („weiterer, umtexturierter Wandering Trader mit einem Pferd an der Leine; genau einer pro Dorf;
bietet seltene, aber nicht so seltene Items; Angebot alle 3 Tage neu, Restock ca. alle 1,5 Tage; Trades passend aufteilen“).
Belege: Minecraft-26.3-Quellen (`forge-26.3-66.0.8-sources.jar`) und Vanilla-Daten (`client-extra-26.3`), Repo per grep. Nichts davon im Spiel geprüft.

## 1. Ist-Zustand

**Vanilla Wandering Trader (26.3), aus dem Code:**
- `WanderingTraderSpawner` (CustomSpawner): alle 1200 Ticks Zähler, alle 24000 Ticks ein Wurf mit Chance 25 → +25 je Versuch, max. 75 % (`nextInt(100) <= chance`);
  danach noch „1 aus 10“; Chance fällt nur nach erfolgreichem Spawn auf 25 zurück. Grob ein Händler je ~13 Spieltage, wenn die Chance bei 75 steht.
- Position: zufälliger Spieler, bevorzugt Bell-POI (`PoiTypes.MEETING`) im Umkreis 48, sonst Spielerposition; Spawn ≤48 Blöcke davon. Gamerule `spawn_wandering_traders`,
  Biom-Tag `without_wandering_trader_spawns`. Despawn nach 48000 Ticks (2 Tage), außer er handelt. `removeWhenFarAway=false`, `setHomeTo(ref, 16)`.
- Er bringt 2 Trader-Lamas an der Leine (`TraderLlama`, Despawn folgt dem Händler, Spieler kann sie nicht reiten, solange sie am Händler hängen, `doPlayerRide`).
- Nachts trinkt er Unsichtbarkeit (`UseItemGoal`), morgens Milch. Flieht vor Zombie, Illager, Vex, Zoglin.
- Angebote: `updateTrades` zieht aus 3 Trade-Sets: `wandering_trader/buying` (2 aus 6), `uncommon` (2 aus 16), `common` (5 aus 78) = 9 Angebote, **einmal beim ersten
  Öffnen gewürfelt, nie neu, nie Restock** (kein Restock-Code in `AbstractVillager`/`WanderingTrader`). Daten: `data/minecraft/trade_set/wandering_trader/*`,
  Pools als Tags `data/minecraft/tags/villager_trade/wandering_trader/{buying,common,uncommon}`; es gibt **kein** `rare`-Set.
- Vanilla-Inhalt: buying = Wasserflasche, Wassereimer, Milcheimer, gärendes Spinnenauge, Ofenkartoffel, Heuballen (je max. 2). common (78) = 16 Farben, Fisch-/Kugelfischeimer, Blumen,
  Setzlinge, Saatgut, Korallen, Moose, Sand, Dripstone, Namensschild usw. uncommon (16) = Packeis, Blaueis, Schwarzpulver, Podsol, 10 Stämme, Eisenspitzhacke, lange Unsichtbarkeit.

**Im Projekt (grep):**
- Keine eigenen Händler-Mobs, kein eigener Spawner, kein Dorf-/POI-Code außer `MobsServer` (Deceiver meidet Dörfer, `getStructureWithPieceAt(pos, StructureTags.VILLAGE)`).
  Muster für Spawn-Würfe pro Spieler: `modules/simplemobs/.../MobsServer.java` (alle 600 Ticks).
- SimpleBuilding liefert 10 Wandering-Trader-Trades (`src/main/resources/data/simplebuilding/villager_trade/wandering_trader/`): kaufen `octant_emerald`, `reinforced_bundle_emerald`
  (Tag `buying`); common `emerald_diamond_pebbles`; uncommon `emerald_octant`, `emerald_reinforced_bundle`, `emerald_wand_book`, Kupfer-/Eisen-/Gold-/Diamantkern
  (Preise 24/32/48/64 Smaragde, Chancen 100/50/25/10 %, siehe `docs/KERNE-SELTENHEIT.md`; Entscheidung 28.09.: Kerne nur beim fahrenden Händler, selten).
  Test `TradeAndMigrationTests#modTradesAreMergedIntoTheVanillaTradePools` und `wanderingTraderCanRollAModTrade` prüfen diese Pools per ID.
- Simple Money liefert `wandering_trader/40..46` (40, 41 in `common`: Chorusfrucht, Äpfel für Scheine; 42–46 in `uncommon`: Netherit-Schrott, Shulkerschale, Diamanterz,
  Tiefenschiefer-Diamanterz, Knospender Amethyst) und **238 Preislisten-Trades** (`links/<mod>/<item>`, `merchant: wandering_trader`, Preis in Geldscheinen,
  `LinkBudget` je Spieler/Tag, `max_uses`=Bestand). Verteilung: SB 139 (Tier 4: 84, Tier 5: 51, 4 Kerne), Simple End 5, Drachenkampf 1, Ovens 6, **Mobheads 87** (Tier 3, Chance 0,1 ×14, 0,05 ×73).
- **Fund (Vorbedingung):** die 238 Links stehen im Tag `wandering_trader/rare`, den kein Vanilla-Trade-Set (26.2 und 26.3) liest; im Repo gibt es weder ein `trade_set` noch einen Mixin dafür
  (`modules/simplemoney/tools/generate_links.py:37`). Per grep also **nie angeboten** – im Spiel zu bestätigen (Test: 64 Händler würfeln, Link-Item erscheint). Eigener Nachtrag/Fix, siehe 9.
- Soulbound-Buch beim fahrenden Händler (5 %): `WanderingTraderSoulboundMixin` (Simple Tools, Mixin auf `WanderingTrader#updateTrades` TAIL).
- Simple Mobheads hat `wandering_trader_head`/`trader_llama_head` (Entity-ID-gebunden, kein Bezug zum neuen Mob).

## 2. Modul und Namen
**Empfehlung: neues Modul `simplewanderers` („Simple Wanderers“), additiv unter `modules/simplewanderers/`** (`python tools/newmod.py`), Hauptlinie 26.3, alle drei Loader.
Gründe: Entity + Spawner + Pferd sind eigener Code, weder Geld noch Bau; Simple Mobs ist Endgame/Feindmob-Modul; SB-Kern soll nicht weiter wachsen.
Allein spielbar (Prinzip): nur Vanilla-Trades, Mod-Items über bedingte Tags (`required:false`) und Load-Conditions, keine harte Abhängigkeit.
Erweiterung durch Simple Money (Preislisten) und SimpleBuilding (Octant/Bündel) nur über **eigene Tags im Namensraum des Moduls** (Abschnitt 6).
IDs: `simplewanderers:village_wanderer` (EN „Village Wanderer“, DE „Dorf-Wanderer“), `simplewanderers:wanderer_horse`; Spawn-Ei; Config `simplewanderers.json` (Server).
Alternative: in Simple Mobs (weniger Aufwand, falsche Heimat; Frage 1).

## 3. Entity, Spawn „genau einer pro Dorf“
- `VillageWanderer extends WanderingTrader` (Klasse nicht final): erbt Handel, Sounds, Flucht-Goals, Modell; eigene Goals ohne Unsichtbarkeit (er bleibt sichtbar und nachts am Glockenplatz).
  `removeWhenFarAway=false`, **kein** Despawn-Timer, `setHomeTo(Glocke, 24)`, `WanderToPositionGoal` zurück zur Glocke. Eigene Entity-Registrierung auf allen drei Loadern.
- **Dorf-Erkennung:** Bell-POI (`PoiTypes.MEETING`, wie Vanilla, billig über den POI-Index) im Umkreis 48 eines Spielers **und** `structureManager().getStructureWithPieceAt(bellPos, StructureTags.VILLAGE)`
  gültig. Letzteres filtert vom Spieler gebaute Glocken (sonst Farm) und schließt Mod-Dörfer im Tag ein. **Dorf-ID = `StructureStart#getChunkPos()`** (nicht die Glocke: mehrere Glocken je Dorf möglich).
- **Persistenz:** `SavedData simplewanderers:villages` je `ServerLevel` (Codec-Liste, Obergrenze 100000 Einträge): `startChunk`, Zustand `ALIVE(uuid)`/`DEAD(sinceGameTime)`, `periodAnchor`. Eintrag wird beim Spawn angelegt.
- **Würfe:** alle 200 Ticks je Nicht-Spectator-Spieler (Muster `MobsServer`): Glocke finden → Dorf-ID → Zustand prüfen → Spawn am Glockenplatz (Platzprüfung wie `findSpawnPositionNear`, Radius 8).
  Spawn sofort beim ersten Besuch (keine Zufallschance). Zusätzliche Sperren: aktiver Raid am Dorf, Gamerule/Config aus, Welt ohne Spieler im Umkreis.
- **Genau einer, abgesichert dreifach:** (1) Zustand `ALIVE` verhindert Neuspawn; (2) vor jedem Spawn Zählung lebender `VillageWanderer` mit gleicher Dorf-ID im Dorf-Bounding-Box+32;
  (3) beim Betreten der Welt (`EntityJoinLevel`/`onAddedToLevel`) verwirft ein Duplikat mit gleicher Dorf-ID und anderer UUID sich selbst. Spawn-Ei/`/summon` ohne Dorf-ID zählt nicht (Test/Kreativ).
- **Tod/Verlust:** `remove(reason)` mit `reason.shouldDestroy()` (KILLED/DISCARDED) → `DEAD(now)`; Entladen/Dimensionswechsel zählt nicht. **Respawn nach `respawnDays` (Standard 7, 0 = nie, max. 60)**,
  ausgewertet nur, wenn wieder ein Spieler an der Glocke ist (keine Chunk-Last, keine Tick-Kosten). Gespeichert als absolute Spielzeit, überlebt Neustart.
- Vanilla-Spawner bleibt unberührt; der Vanilla-Händler zählt nicht mit (anderer Typ).

## 4. Pferd an der Leine
- `WandererHorse extends Horse` (eigener Typ, Vanilla-Renderer und -Fellvarianten, keine neue Textur nötig). Beim Spawn des Wanderers: 1 Pferd 2–4 Blöcke daneben, `setLeashedTo(wanderer, true)`,
  persistent, `setHomeTo` wie der Wanderer. Optional Körper-Rüstung: gefärbte Lederpferderüstung in der Wandererfarbe (`dropChance 0`) – Wiedererkennung ohne Texturarbeit.
- **Diebstahlschutz (Empfehlung):** `doPlayerRide` gesperrt, solange es am Wanderer hängt (wie `TraderLlama`); Leine nicht abnehmbar (Test: Rechtsklick mit Leine/leerer Hand);
  Tod des Wanderers löst das Pferd **nicht**: es bekommt `DespawnDelay` (Pferd verschwindet mit dem Händler, wie Lamas), sonst entstünde ein Pferd je Dorf als Nebenprodukt. Spawn-Ei-Pferd ist normal.
- **Kill-Schutz (Empfehlung): keiner für den Wanderer** (Vanilla-konform, Respawn-Timer ist die Strafe); Pferd nimmt normalen Schaden. Stirbt nur das Pferd, kommt ein neues am nächsten 3-Tage-Zeitpunkt (Abschnitt 5).
- Verhalten: Pferd folgt über die Leine (Vanilla-Elastizität), grast nicht weg, Panik wie Lama (`PanicGoal`). Reiten/Einfangen wäre ein Feature für später (Frage 4).

## 5. Trade-Rotation (alle 3 Tage neu, Restock ~1,5 Tage)
Tag = 24000 Ticks, Basis **Spielzeit (`gameTime`)**, nicht Tageszeit (Schlafen verschiebt nichts). Periode 72000 Ticks, Halbperiode 36000.
- Vanilla kennt weder Reroll noch Restock beim Wanderer → **eigene Logik im `VillageWanderer`**, nicht in `AbstractVillager`.
- `periodIndex = floor(gameTime / 72000)`: ändert er sich, werden die Angebote **verworfen und neu gezogen**; Zufallsquelle `Seed = hash(dorfId, periodIndex, Weltseed)`
  – jeder Spieler sieht dasselbe, Neuladen/Relog würfelt nichts neu. `halfIndex = floor(gameTime / 36000)`: ändert er sich ohne neuen `periodIndex` → alle Angebote `resetUses()` (Restock).
  Reroll ist zugleich Restock. Zeitfolge: 0 d Angebot A · 1,5 d Restock · 3 d Angebot B · 4,5 d Restock …
- Wartet, solange ein Spieler handelt (`isTrading()`), dann beim Schließen. Beim Laden nach langer Abwesenheit: **höchstens ein** Reroll, nicht nachholen (kein Burst).
- Persistiert: `lastPeriod`, `lastHalf` im Entity-NBT; gezogene Angebote bleiben im Vanilla-`Offers`-NBT.
- Eigene Ziehung statt `updateTrades` von Vanilla: `addOffersFromTradeSet` ist `protected` (aufrufbar aus der Unterklasse) mit eigenen Trade-Sets (Abschnitt 6); das Soulbound-Mixin am `WanderingTrader` greift dann **nicht**, solange
  `updateTrades` überschrieben wird – gewollt (Frage 7: Soulbound auch hier?).
- Simple-Money-Preislisten sind zusätzlich durch `LinkBudget` (Tageslimit, Abklingzeit je Spieler) begrenzt: Restock umgeht das nicht.

## 6. Trade-Aufteilung Wandering Trader (WT) vs. Dorf-Wanderer (DW)
**Leitregel:** WT = **exotisch/selten** (nur über Reisen, Biome, Tier ≥ 4, Kerne, seltene Köpfe). DW = **weniger selten** (Dorf-/Garten-/Wald-Alltag, Tier ≤ 3, einziger Händler mit verlässlichem Zugang).
**Kein Mod-Item mit Tier ≥ 4 beim DW.** Die Kern-Kalibrierung (`KERNE-SELTENHEIT.md`) bleibt unberührt: alle bestehenden SB-/Simple-Money-Trades am WT bleiben, nur die **Vanilla-`common`-Gruppe wird aufgeteilt**
(WT zieht 5 aus 31 statt 5 aus 78: exotischer, nicht seltener); `uncommon` bleibt beim WT unverändert (sonst verdichtet sich die Kernchance, 2 aus 23 → 2 aus 15), der DW bekommt Kopien.
Neue DW-Pools als eigene Tags (`data/simplewanderers/tags/villager_trade/wanderer/{buying,common,uncommon,rare}`), Trade-Sets `simplewanderers:wanderer/*`; Fremdmods tragen **nur** per Tag mit `required:false` ein.

| Pool | WT (nach Aufteilung) | DW (neu) |
|---|---|---|
| Draws je Besuch/Periode | buying 2, uncommon 2, common 5 (+ `rare` 1, siehe Fund) | buying 2, common 4, uncommon 2, rare 1 (nur wenn Pool nicht leer) |

**Vanilla `buying` (6):** WT: Wasserflasche, gärendes Spinnenauge (+ SB `octant_emerald`, `reinforced_bundle_emerald`, unverändert). DW: Wassereimer, Milcheimer, Ofenkartoffel, Heuballen (+ Kopien Octant/Bündel).
**Vanilla `common` (78 = 31 WT + 47 DW, vollständig, geprüft):**
- DW (47): 16 Farben (white … black), Saatgut ×4 (Weizen, Rote Bete, Kürbis, Melone), Farn, Zuckerrohr, Kürbis, Löwenzahn, Mohn, Allium, Porzellansternchen, Tulpe rot/orange/weiß/rosa, Margerite,
  Kornblume, Maiglöckchen, Wildblumen, Pilz braun/rot, Setzling Eiche/Birke/Fichte/Akazie/Schwarzeiche, Namensschild, Sand, Roter Sand, Kaktus, Trockenes Gras.
- WT (31): Tropenfisch-/Kugelfischeimer, Seegurke, Schleimball, Glowstone, Nautilusschale, Seetang, Blaue Orchidee, Offene Augenblüte, Setzling Dschungel/Kirsche/Blasseiche, Mangroven-Schössling, Pappel-Setzling,
  5 Korallenblöcke, Ranken, Blasses Hängemoos, Seerosenblatt, Kleines Tropfblatt, Spitzer Tropfstein, Schwefelspitze, Wurzelerde, Moosblock, Blasser Moosblock, Glühwürmchen-Busch, Goldener Löwenzahn, Regalpilz.
**Vanilla `uncommon` (16):** WT: alle 16 (unverändert). DW (Kopien, 8): Stämme Eiche/Birke/Fichte/Akazie/Schwarzeiche/Dschungel, Podsol, Schwarzpulver.
  Nur WT: Packeis, Blaueis, Eisenspitzhacke, lange Unsichtbarkeit, Kirsch-/Mangroven-/Blasseichen-/Pappelstamm.
**SimpleBuilding-Trades (10, bleiben am WT, Kopien am DW):** DW zusätzlich `emerald_octant` (10 Sm.), `emerald_reinforced_bundle` (16), `emerald_diamond_pebbles` (5→3) in `uncommon`/`common`;
  nur WT: 4 Kerne, `emerald_wand_book`, Kauftrades. DW verkauft **keine Kerne** (Besitzerentscheidung 28.09.).
**Simple Money (WT 7 + 238 Links):** `wandering_trader/40` (Chorus) und `41` (Äpfel) am WT **und** DW (`common`); `42–46` nur WT.
  Preislisten: DW bekommt die **14 Köpfe mit Chance 0,1** (Pig, Cow, Chicken, Sheep, Blaze, Enderman, Husk, Spider, Cave Spider, Slime, Silverfish, Breeze, Shulker, Drowned) im Pool `rare`
  (`merchant: village_wanderer`, neuer Schalter `enableWandererTrades`, Standard an); WT behält 73 Köpfe (0,05), alle Tier-4/5-Items (SB 135, Ovens 6, Simple End 5, Dragon Chitin 1) und die 4 Kerne-Links.

**Mod-Items ohne Tag/Datei im Modul:** Simple Mobheads, SB, Simple Money tragen per Load-Condition ein; ohne Simple Money bleiben nur Vanilla und SB-Kopien. Ohne SB/Money ist der DW nur Vanilla (Prinzip „allein spielbar“).

## 7. Textur-Idee
- Nur Textur, Modell/Animationen von `VillagerModel`/`WanderingTraderRenderer` unverändert (umtexturiert, wie der Auftrag). 64×64, Vanilla-UV-Layout, **neue Pixelkunst von Hand** (eigener Generator
  `tools/textures/village_wanderer_textures.py`, `--check`, Vorschau 16-fach alt neben neu unter `docs/ai/previews/oc-wanderer-concept/`), nie am Rand malen, keine weichgezeichneten Skalierungen.
- Look: warmer Reisemantel in Ocker/Rostbraun statt Blau/Weiß des Händlers, breite Krempenkapuze, grüner Schal, Rucksack-Rolle auf dem Rücken (nur aufgemalt), kleines Messing-Glöckchen am Schal (Dorf-Bezug zur Glocke).
  Pferd: Vanilla-Fell + gefärbte Lederrüstung in Ocker (keine neue Pferdetextur).
- Regel Besitzer 12.10.: Pixel dürfen **nicht aus dem Mojang-Sprite abgeleitet** sein; eigener Entwurf, also kein Eintrag in `vanilla_derived.py` nötig. Vorlage nur als Layout-Referenz (UV-Inseln), nichts kopiert.
  Spawn-Ei-Item: eigener 16×16-Entwurf (Frage 5: Wunschfarben, 3 Varianten zur Auswahl).

## 8. Balancing, Konfiguration, Server-Autorität
Config `simplewanderers.json` (nur Server, Obergrenzen): `enabled` (an), `respawnDays` 7 (0–60), `rerollDays` 3 (1–14), `restockDays` 1,5 (0,5–`rerollDays`), `villageRadius` 48 (16–64), `homeRadius` 24 (8–48),
`horse` an, `raidBlocksSpawn` an. Zahlen auch in der Balancing-Zentrale `balance/simplewanderers/`. Obergrenzen so, dass Vanilla nicht gefährdet wird (z. B. `restockDays` nie unter 0,5).
- Angebotsmenge je Periode 8–9 (WT 9–10). Max-Verwendungen der Vanilla-Trades gelten (2…12, teils unbegrenzt bei Stämmen/Ranken: Vanilla-Verhalten, Preise unverändert).
- **Risiko Dauerquelle:** ein DW je Dorf mit Restock statt einmaliger Ware macht Bulk-Items (Farben, Stämme, Gunpowder 4 je Trade, Kernbauteile) planbar. Gegenmittel: Restock-Takt 1,5 d,
  gunpowder `max_uses 2` aus Vanilla, Simple-Money-Budgets bleiben, Preise nicht rabattieren (`reputation_discount` 0, kein Preisnachlass durch Ruf). Prüfen: Smaragd-Schleife (buying→selling) – Vanilla-Buying-Trades zahlen weniger, als die Verkaufstrades kosten.
- Dichte: in einer Welt mit 30 Dörfern 30 DW; Entities nur im geladenen Bereich, Tick-Kosten wie ein Dorfbewohner ohne Brutlogik.

## 9. Umsetzungsstufen und Tests
Stufen (je Stufe gemeinsam für Fabric + NeoForge + Forge 26.3):
0. **Vorab:** `wandering_trader/rare` wirkungslos beheben (Mixin `WanderingTrader#updateTrades` TAIL zieht `rare`, `amount 1`) – auch ohne DW nötig; Test, dass ein Link-Item erscheint.
1. Modul-Gerüst, `VillageWanderer`, `WandererHorse`, Registry, Spawn-Ei, SavedData, Spawner, Respawn.
2. Trade-Sets/Tags, Aufteilung des Vanilla-`common`, Kopien, Simple-Money-/SB-Bedingungen, Rotation (Reroll/Restock).
3. Textur (Generator + Vorschau), Lang EN+DE, Wiki (`modules/simplewanderers/wiki/manual.json`), Guide, `docs/MODS-UND-FEATURES.md`, Config-Seite, Zentrale.
4. Port-Run 26.2/1.21.11/26.4 erst nach Abnahme (AGENTS §3).
GameTests (Namensraum `simplewanderers:`, Fabric braucht `@GameTest`-Wrapper + Katalog; Module: Targets `module-simplewanderers-*-263`):
`spawn_one_per_village` (zwei Würfe → ein Wanderer), `spawn_not_at_player_bell` (Glocke außerhalb Dorfstruktur), `persists_across_reload`, `respawn_after_days_only_when_player_near`,
`no_respawn_when_zero`, `duplicate_removed_on_join`, `horse_leashed_and_not_rideable`, `horse_follows_wanderer_death`, `rerolls_every_72000_ticks_deterministic`, `restocks_every_36000_ticks`,
`no_reroll_while_trading`, `no_catchup_burst`, `trades_only_from_wanderer_pools`, `no_tier4_mod_item`, `vanilla_common_partition_complete` (31+47=78, keine Doppelten),
`wt_pools_unchanged_for_mod_trades` (Kerne, Octant, Bündel, Wandbuch), `works_standalone` (nur Vanilla), `config_caps`. Anpassen: `TradeAndMigrationTests` (Common-Pool-Prüfung),
Wiki-/Texturgates (`wiki/generate.py --all --check --strict`, `generate_textures.py --check`, `check_missing_textures.py`, `module_guides.py --check`, `check_tooltip_text.py`).
Klick-Test auf der Development-Instanz: Dorf anfliegen, Händler + Pferd sehen, handeln, 3 Tage vorspulen (`/time add 72000`), Angebot wechselt, Tod → 7 Tage.
Nicht belegbar ohne Spiel: Leinen-Verhalten bei Fremdinteraktion, Navigation Pferd↔Händler, Strukturerkennung bei Mod-Dörfern, Performance bei vielen Dörfern.

## 10. Offene Besitzer-Fragen (Empfehlung zuerst)
1. Modul: neues `simplewanderers` (**Empfehlung**) oder in Simple Mobs?
2. Aufteilung: nur Vanilla-`common` teilen, alles Mod-Bestehende am WT lassen, DW bekommt Kopien (**Empfehlung**, schont Kernchance) oder echte Aufteilung auch bei `uncommon`/SB-Trades?
3. Respawn nach Tod: 7 Tage, nur wenn Spieler da (**Empfehlung**), oder nie, oder sofort beim Wiederbesuch?
4. Pferd: nur Deko, nicht reit-/stehlbar (**Empfehlung**) oder kaufbar (Handel liefert Pferde-Spawn-Ei) / nach Leine-Duell einfangbar?
5. Kill-Schutz für den Wanderer (z. B. nur Mobs/Spieler im Kreativ töten)? Empfehlung: keiner, Vanilla-konform.
6. Spawn: sofort beim ersten Besuch (**Empfehlung**) oder erst nach 0–1 Tag Zufallsverzögerung?
7. Soulbound-Buch (Simple Tools) auch beim DW? Empfehlung: nein, nur WT.
8. Mod-Köpfe (0,1-Gruppe) und Kerne: Kerne am DW (Kupferkern, 24 Sm.)? Empfehlung: nein (Entscheidung 28.09.), Köpfe ja (14).
9. Vorab-Fix `wandering_trader/rare` (Stufe 0) jetzt als eigener Auftrag? Empfehlung: ja, unabhängig vom DW.
10. Biom-abhängiges Sortiment (Wüstendorf: Kaktus/Sand, Taiga: Fichte/Podsol …) als spätere Stufe? Empfehlung: später, erst Basisversion.
11. Eigene Tierkopf-/Kopf-Variante `village_wanderer_head` (Simple Mobheads) und Drop? Empfehlung: später, nicht Teil dieser Runde.
