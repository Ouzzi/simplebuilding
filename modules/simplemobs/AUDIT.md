# Audit simplemobs (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Branch a-audsb31), nur MC 26.3 Fabric; Forge/NeoForge sind nicht Teil des Releases.
Umfang: 158 Java-Dateien / 15,8k Zeilen ohne Tests (shared + fabric), dazu ~8,2k Zeilen Tests. 2 Netzwerk-Payloads (1 C2S, 1 S2C,
5 Dateien), 43 Mixins (33 gemeinsam, 10 nur Client), keine Menues, 1 Block mit (Vanilla-)BE (Behelfs-Spawner), 15 eigene Entity-Typen.
Geprueft **risikogeleitet** (P1-Klassen zuerst), Aufbau wie `modules/simplestorage/AUDIT.md`.

## Vollstaendig gelesen
- Netzwerk: `FearNet` (C2S Sichtfeld), `TrumpetNet` (S2C Ton), Registrierung `MobsFabric`, `FabricClientExample`, `client/TrumpetLoop`.
- Mixins: alle 33 gemeinsamen (`mixin/*` laut `simplemobs.mixins.json`); bei den 10 Client-Mixins nur Ziel/Liste (alle in `client`).
- Entities: `DeceiverEntity`, `HowlerEntity`, `HowlerRitualEntity`, `BroodRitualEntity`, `DisguiseMarker`, `ShellkerEntity`,
  `SoulFireball`, `BlightedWither` (ab Faehigkeiten), `VoidRunnerEntity` (ab Regeln); Stichproben `HollowWatcherEntity`
  (Ziel/Teleport/Griff), `DreadEntity`, `BrimguardEntity` (Suchen, Bossleiste).
- Spawns/Server-Ticks: `MobsServer`, `FearServer`, `EndNetherServer`, `ZoneSpawns`, `FearAura`, `ChunkHolds`.
- Rituale/Beschwoerung: `HowlerRitual`, `HowlerPattern`, `DreadPattern`, `BlightedPattern`, `ArmoredWither`, `BossRituals`, `BroodRitual`, `MobEssenceItem`.
- Drops/Bloecke: `WitherLoot`, `WitherLootBlock`, `SpawnerParts`, `MakeshiftSpawnerBlock`, `StrongTntBlock`, `StrongExplosives`, `ShellkerShellItem`, `SoulFireChargeItem`.
- Spieler-Ausruestung: `Disguise`, `CloakLogic`, `DisguiseRegistry`, `TrickMirrorItem`, `TrumpetItem`, `WitherWings`, `CombatEffects`.
- Trials: `EvilOmen`, `TrialsExtras` (Deckel, Treffer, Aura), `EvilBrewing`, `EvilTrialsData`, `TrialsHooks`, `TrialVaultBlock`, `Trials` (Omen/Schluessel).
- Config: `MobsConfig` (Lesen, Grenzen), `BossBars`.
- Nicht gelesen: Client-Renderer/Modelle/Looks (`client/*` ausser TrumpetLoop), `trim/TrimAbilities` (nur Ein-/Ausschalter, Grenzen,
  Tick-Stellen), `HowlerLogic`/`FearLogic`/`BossLogic`/`WitherLogic`/`EndNetherLogic`/`DeceiverLogic` (reine Regeln, nur Stichproben),
  `ShellkerBullet`, `SoulfireBlaze` (nur Griefing ueber `SoulFireball`), `NetherHeartBlock`, `MobsViewer`/`TrialsViewer`, `guide/`, `discover/`, Forge/NeoForge.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | 1 C2S: `FovPayload` (ein VarInt) - Server-Thread (`context.server().execute`), Wert per `FearLogic.clampFov` begrenzt, Map je UUID, beim Trennen/Stoppen geleert; ohne Drosselung, aber nur ein Map-Eintrag (P3 nicht noetig). S2C `TonePayload` 2 VarInts, nur bei Tonwechsel (min. 2 Ticks) bzw. alle 40 Ticks an Spieler im Umkreis. |
| C2 Server-Autoritaet | Keine Menues. Beschwoerungen, Verkleidung, Trompetenton, Fluegel-Faktor (Server stempelt `simplemobs:speed_factor`) entscheidet der Server. Keine Client-Klassen in gemeinsamem Code ausser `client/HowlerShake` (nur im Client-Zweig von `HowlerEntity#tick` aufgerufen). |
| C3 Dupes/Verlust | Helm-Kronen (Item-Display) und Seitenhelme: kein Dupe (Display haelt Kopie, Hand -1, Schaedel weg = Helm faellt). Brutmulde: Opfer atomar genommen, Abbruch erstattet genau einmal. Behelfs-Spawner: kein Fragment-Kreislauf (Test vorhanden), aber **P1-1 (EP-Kreislauf) gefunden und gefixt**. Beschworene ohne Beute gaben Mob-Essenz: **P2-3 gefixt**. |
| C4 NBT | Heuler-Fallen (Listen gepaart gelesen), Brut-Erstattung (`ItemStack.CODEC`, max. 99 je Stapel, Essenz 64 passt), `EvilTrialsData` (Codec, ungueltige Eintraege fallen weg), Taeuscher (`Look`-String, nur per Befehl manipulierbar). |
| C5 Tick-Kosten | Natuerliche Spawns je Spieler alle 10-30 s mit 128er-AABB (ok). Heuler-Puls/Fallen/Schock nur mit Abklingzeit; Bossleisten iterieren `level.players()` je Tick (ok). `CombatEffectsMixin` (WrapMethod auf jedem `hurtServer`) und `LivingEntityGravityMixin` (jeder `travel`) steigen frueh aus. Howler-Sensor-Suche 11^3 Bloecke je Hornruf (Horn-Abklingzeit). |
| C6 Leaks | `FearNet.FOV`, `TrumpetNet.SENT` bei Logout/Stop geleert; `FearServer.FOREST_TICKS` alle 60 s bereinigt; Spieler-Spotted-Map alle 100 Ticks; Client-Caches beim Trennen. **P2-2 gefunden und gefixt** (Trials-Zustand haelt ServerLevel ueber den Server-Stopp). **P2-5 offen** (verwaiste Taeuscher-Fakes). |
| C7 Sicherheit/Rechte | Keine eigenen Befehle (nur `/simplemobs config` ueber SimpleLib). Griefing: Heuler-Sculkspur/-Nachwirkung, Fallen, Wither-Welken, Seelenfeuerball (Mob-Besitzer) pruefen `mobGriefing`, Starkes TNT `tntExplodes`. Rituale nur per `useOn` (Vanilla prueft Spawnschutz). Mixins: kein `@Overwrite`/`@Redirect`; Wirkung bei Aus: **P2-1 gefixt**, Rest P3-1/P3-2. |
| C8 Unabhaengigkeit | Standalone-Ziel vorhanden; harte Abhaengigkeit `simpleresonance` (dokumentiertes Add-on, `PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, b-mob32), `simplelib`. SimpleBuilding nur bedingt (Shivering, Sculk-Kiefer, Mirage per Registry-Lookup). |
| C9 Doku/Pflege | Keine TODO/FIXME; Lang EN=DE (282/282); `check_data.py` gruen; keine Kern-Dubletten in `tools/dup_pairs.txt`. Doppelte Test-Registrierung P3-7. |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplemobs-P1-1 | **P1 (Exploit)** | Der Behelfs-Spawner erbt `SpawnerBlock#spawnAfterBreak` und gab bei jedem Abbau Vanillas 15-43 EP; da er sich selbst droppt (Loot-Tabelle ohne Behutsamkeit), ergab Setzen + Abbauen **endlos Erfahrung** (`MakeshiftSpawnerBlock.java:39`). | **gefixt**: Override reicht `dropExperience=false` an Vanilla weiter (Fragment-Hook unberuehrt); Gametest `spawner_makeshift_no_xp_loop` (Vanilla-Spawner gibt weiter EP, Behelfs-Spawner 3x nie) |
| simplemobs-P2-1 | P2 (Rechte/Logik) | Die eigenen natuerlichen Spawns (Taeuscher `MobsServer.java:23`, Hohlwaechter `FearServer.java:47`, Seelenfeuer-Lohe `EndNetherServer.java:46`) ignorierten die Spielregeln `spawnMobs`/`spawnMonsters`; der Taeuscher kam auch auf Friedlich, Lohe und Shellker-Torwachen (`EndNetherServer.java:20`) auch bei ausgeschaltetem Sub-Mod. | **gefixt**: `MobsServer.naturalSpawns(level)` (Sub-Mod an, nicht Friedlich, beide Regeln an) an allen drei Stellen, Torwachen fragen den Sub-Mod; Gametest `natural_spawn_rules` |
| simplemobs-P2-2 | P2 (Leak) | `EvilOmen.EVIL_RAIDS` (WeakHashMap, Wert `RaidInfo` haelt das `ServerLevel` und damit den Schluessel-Raid, `EvilOmen.java:52-54`), `RAIDS` und `TrialsExtras.ACTIVE` (`TrialsExtras.java:59`) wurden beim Server-Stopp nie geleert: im Einzelspieler blieb die alte Welt im Speicher, und alte Deckel-Marken (Zeitstempel in der Zukunft, `TrialsExtras.java:111`) zaehlten in der naechsten Welt als aktiv. | **gefixt**: `EvilOmen.forgetAll()` (`EvilOmen.java:315`), registriert per `PlayerState.onStop("simplemobs:trials")` (`MobsFabric.java:48`); Gametest `trials_forget_on_stop` (prueft die Aufraeum-Methode; der Stopp-Aufruf selbst ist per Gametest nicht pruefbar) |
| simplemobs-P2-3 | P2 (Endlos-Farm) | Beschworene des Taeuschers (Wellen ohne Ende, Fakes mit 1 Leben) und Wither-Diener haben bewusst keine Beute (`DeathLootTable` = summoned), gaben aber Mob-Essenz (`BroodRitual.java:51` kennt `MOB_SUMMONED` nicht) - risikoloser Essenz-/Spawn-Ei-Farm. | **gefixt**: Tag `simplemobs.no_essence` beim Beschwoeren (`DeceiverEntity.java:519`, `BlightedWither.java:265`); Gametests `brood_no_essence_from_summons` (Wither-Diener + Kontrolle) und Pruefung in `fight` (Taeuscher-Welle) |
| simplemobs-P2-4 | P2 (Balance/Farm) | Taeuscher-Fakes (Illusionen, 1 Leben, kein Schaden) geben echte Erfahrung (Zombie 5 EP); ein feindlicher Taeuscher beschwoert alle 8-12 s 5-16 davon, solange ein Spieler in 32 Bloecken steht - EP-Farm ohne Risiko (`DeceiverEntity.java:532` `makeFake`). | **Rueckfrage AUDSB31-M1** (unten) |
| simplemobs-P2-5 | P2 (Server-Last) | Die Liste der Beschworenen wird nicht gespeichert (`DeceiverEntity.java:93`, `:200`); Fakes sind dauerhaft (`setPersistenceRequired`, `:541`). Entlaedt der Taeuscher-Chunk mitten im Kampf oder startet der Server neu, bleiben alle Fakes der Welle fuer immer (stumm, 1 Leben). Haeuft sich bei abgebrochenen Kaempfen. | offen (kein kleiner Fix): Vorschlag Fakes mit Ablaufzeit im Tag (`FAKE_LIFETIME_TICKS`) und eine kleine Tick-Pruefung, die abgelaufene Fakes entfernt; zusaetzlich Liste speichern |
| simplemobs-P3-1 | P3 | Helm auf Skelettschaedel (`ItemHelmetDreadMixin.java:31`), Netherit-Helm auf Witherschaedel (`ItemHelmetWitherMixin.java:31`), Drachenatem/Netherstern auf Purpur/Obsidian (`ItemBossRitualMixin.java:34`): auch bei ausgeschalteter Beschwoerung oder Sub-Mod Rauch + verbrauchter Klick (Helm laesst sich dort nicht per Rechtsklick anziehen; Client nutzt Standard-Config). | Empfehlung: bei Aus frueh `return` (Vanilla-Verhalten) |
| simplemobs-P3-2 | P3 | Tarnumhang/Verkleidung (`Disguise.java:39`, Player-Mixins) wirken auch bei ausgeschaltetem Sub-Mod. | Hinweis |
| simplemobs-P3-3 | P3 | Verkleidung als anderer Spieler uebernimmt dessen Aussehen samt Name (`Disguise.java:131`) - auf Servern Verwechslung mit Admins moeglich (Besitzer-Entscheid 11.10.). | Hinweis (ggf. Server-Option) |
| simplemobs-P3-4 | P3 | `BossBars.track` (`BossBars.java:60`) entfernt nur Spieler derselben Welt; wer die Dimension wechselt oder respawnt, behaelt die Leiste, bis der Boss entlaedt/stirbt. | Hinweis |
| simplemobs-P3-5 | P3 | `MobsConfig.num` (`MobsConfig.java:393`): `NaN` aus der Datei uebersteht `Math.max/min`-Grenzen (nur Betreiber-Datei; `dbl()` prueft schon `isFinite`). | Empfehlung: `Float.isFinite` wie `dbl()` |
| simplemobs-P3-6 | P3 | Taeuscher-Naturspawn ohne Platzierungspruefung (`MobsServer.java:47-57`, Heightmap kann Wasseroberflaeche liefern). | Hinweis |
| simplemobs-P3-7 | P3 | Test `sub_mod_switch` doppelt registriert (`MobsTests.java:33`, `:49`, identisch). | Hinweis (Pflege) |
| simplemobs-P3-8 | P3 | Heuler setzt 20 s lang SimpleBuildings Sculk-Kiefer als Falle (`HowlerEntity.java:691`); droppt der Kiefer im SB-Kern sich selbst, ist das Abbauen einer Falle ein Gratis-Item. | im Kern pruefen (ausserhalb des Moduls) |
| simplemobs-P3-9 | P3 | Seelenfeuerpulver passt per Trichter in den Brennstoff-Slot auch bei ausgeschalteter Evil-Kette (`BrewingStandSoulMixin.java:79`); dann ungenutzt. | Hinweis |

## Rueckfragen (Besitzer)
- **AUDSB31-M1** (simplemobs-P2-4): Sollen Taeuscher-Fakes Erfahrung geben? A: nein, Fakes 0 EP, echte Beschworene wie Vanilla
  (Empfehlung - Illusionen, die nur den Kampf erschweren). B: Fakes 1 EP. C: bleibt (Vanilla-Beschwoerer wie der Magier geben auch EP).

## Urteil
**shipbar nach Fix** (1 P1 gefixt, 5 P2: 3 gefixt, 1 Rueckfrage, 1 offen, 9 P3). Netzwerk und Mixins sind sauber (keine Overwrites,
Griefing-Regeln beachtet); die Risiken lagen bei Drops/Farmen und Spawn-Schaltern.

## Testergebnis
Lokal: javac der 13 geaenderten Dateien gegen 26.3 + Fabric-API (`/root/sb-tools-a/audsb31/javac.sh`): nur bekannte
Access-Widener-Meldungen (`MobsRegistry`, `LibMaterials`, in `MobsFabric.java:56` die unveraenderte Zeile `CreativeModeTabs.TOOLS_AND_UTILITIES`),
keine in neuem Code. `modules/simplemobs/tools/check_data.py` gruen, `tools/check_dup_impl.py` 0 Probleme.
Neue Gametests: `spawner_makeshift_no_xp_loop`, `natural_spawn_rules`, `trials_forget_on_stop`, `brood_no_essence_from_summons` (+ Pruefung in `fight`).
sbt: siehe docs/ai/PLAN-A-AUDSB31-2026-11-12.md.
Nicht getestet: der echte Server-Stopp-Aufruf (P2-2), Naturspawns im laufenden Tick (nur die Regel), Client, Forge/NeoForge
(deren Einstiege registrieren `forgetAll` nicht - nicht Teil des Releases), Mehrspieler.
