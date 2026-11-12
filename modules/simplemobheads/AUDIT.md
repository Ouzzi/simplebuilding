# Audit simplemobheads (a-audsb31, 12.11.2026)

Basis `hub/claude-wave1` 388f19991 (Buendel 186), nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplestorage/AUDIT.md`.
Umfang: 73 Java-Dateien / 5,8k Zeilen ohne Tests (Katalog `test/` 3 Dateien / 1,8k Zeilen, `clienttest/` und Fabric-Adapter zusaetzlich); 97 Koepfe
(`data/HeadTable.java` ist generiert aus `data/mobs.json`), 23 Mixins im Hauptblock + 11 Client-Mixins. Der Mod hat **keine** Netzwerk-Payloads, **keine**
Menues und keine eigenen Block-Entities (Vanillas `SkullBlockEntity`): C1 und der Menue-Teil von C3 entfallen. Risikogeleitet gelesen: Drops, getragene
Faehigkeiten, Mixins, Helm-Aufsatz, Rituale/Easter Eggs.

## Vollstaendig gelesen
- Drops: `drop/HeadDrops.java`, `drop/HeadDropCondition.java`, `drop/CopyVariantFunction.java`, `drop/Variants.java`, `drop/HeadsViewer.java`,
  `mixin/CreeperMixin.java` (Wrap um `dropFromLootTable` in `Creeper#killedEntity`, gegen Vanilla per javap geprueft), Block-Loot (`generated/.../loot_table/blocks/*.json`,
  per Skript alle 97: gleiche Form, kopiert nur Name, Sound, Variante), `data/HeadDef.java`, `config/MobHeadsConfig.java`, `SimpleMobHeads.java`, `MobHeads.java`, `MobHeadItem.java`, `MobHeadType.java`.
- Getragene Faehigkeiten: `ability/HeadAbilities.java` (532 Zeilen, alle Faehigkeiten, Zeitgeber, statische Maps), `easter/Giants.java` (Groesse des Traegers),
  `mixin/ServerPlayerTickMixin`, `HeadAbilityLivingEntity/Entity/Player/Farmland/ShulkerBox/ConsumeEffectMixin`, `BeehiveBlockMixin`, `TurtleEggBlockMixin`,
  `MobTargetMixin`, `RamTargetMixin`.
- Helm auf platzierten Koepfen: `helmet/HeadHelmets.java`, `mixin/HeadHelmetUseMixin`, `HeadHelmetDropMixin`, `SkullBlockEntityMixin`, `SkullBlockEntityTypeMixin`.
- Rituale/Easter Eggs: `ritual/HerobrineRitual`, `HerobrineHaunt`, `RitualCooldowns`, `mixin/IgniteItemMixin`, `easter/Technoblade`, `EntityTimes`, `KillerBunny`,
  `Giants` (Gewitter, Heu), `EasterDeathMixin`, `EasterThunderMixin`, `PigThunderMixin`, `AnimalAiMixin`, `CowFeedMixin`, `RabbitSpawnMixin`.
- Einstieg/Pflege: `fabric/MobHeadsFabric`, `command/MobHeadsCommands`, `HeadMoves`, `HeadSlotFit`, `TestCentre`, `guide/MobHeadsGuide` (Registrierung, `use`),
  `discover/MobHeadsDiscover` (Laufzeitteil), Lang EN/DE (Schluesselgleichheit per Skript), `simplemobheads.mixins.json`, `fabric.mod.json`, `modules.json`-Eintrag.
- Client-Trennung: alle `net.minecraft.client`-Importe per grep; `mixin/client/CustomHeadLayerMixin`, `WornModHeadMixin` gelesen.
- Nicht gelesen: `client/*` (Modelle, Rendern, Config-Bildschirm, `GuiHeadFit`), uebrige `mixin/client/*` (nur Eintrag in `"client"` und Importe geprueft), `compat/MobHeadsJeiPlugin`,
  `HeadTable` im Einzelnen (nur Eindeutigkeit der `mob`-Ids und Ausnahmen per Skript), Tests und `clienttest/`.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine Payloads (weder C2S noch S2C); der Helm-Aufsatz laeuft ueber Vanillas Block-Klick (Spawnschutz/`mayInteract` gelten davor), Fernwirkung gibt es nicht. |
| C2 Server-Autoritaet | Drops, Faehigkeiten, Rituale und Easter Eggs entscheidet der Server. Client-Klassen nur in `client/` und `mixin/client/` (alle elf Client-Mixins stehen unter `"client"` in `simplemobheads.mixins.json`). Ausnahme siehe P3-1 (Client liest lokale Config fuer vorhergesagte Bewegung). |
| C3 Dupes/Verlust | Charged-Creeper: ein Kopf je Explosion (Vanillas `droppedSkulls`, `CreeperMixin` ruft den Verbraucher), jede `mob`-Id kommt in genau einem Kopf vor (97 Koepfe, 0 Doppelte, keine Kollision mit Vanillas Zombie/Skelett/Creeper/Piglin/Drache), Babys droppen den normalen Kopf, ein Riese genau seinen Riesenkopf statt des normalen Wurfs. Helm: `put` verbraucht 1, `take` tauscht um, Abbau wirft ihn ueber `preRemoveSideEffects` einmal ab, der Block-Loot kopiert nur Name, Sound und Variante (nicht den Helm). Technoblade: Krone faellt je Schwein und je Spieler hoechstens einmal je `technobladeCooldownTicks`; Spieler-Tags bleiben beim Respawn (`PlayerList#respawn` kopiert sie). Looting nur beim optionalen Spieler-Kill (Standard aus). |
| C4 NBT/Komponenten | `VARIANT` (String, unbekannte Werte -> Basis-Look), `HELMET` (`ItemStack.CODEC`, nur echte Helme mit `assetId`, Koepfe selbst sind keine Helme -> keine Verschachtelung), Ritual-Cooldowns mit Listengrenze 100 000. |
| C5 Tick-Kosten | `HeadAbilities.tick` je Spieler und Tick rund 8 `active()`-Abfragen (Map-Lookups, kein Weltzugriff); Allay/Herobrine nur alle 10/20 Ticks, Fackelsuche palettengefiltert (hoechstens ~5k Bloecke je Sekunde und Traeger), Krone nur jede Sekunde je Schwein. Mixins in heissen Pfaden (`useItemOn`, `isInvulnerableTo`, `travelInAir`) brechen vor jedem Weltzugriff ab. |
| C6 Leaks | Entity-Maps sind `WeakHashMap` (Spieler/Mobs), Haunt-Map wird bei Tick-Ende/Verlassen/Stopp geleert; nur drei UUID-Maps wachsen bis zum Serverstop (P3-2). Transiente Attribut-Modifikatoren (Riese, Silberfisch/Schleim als Item-Attribut) fallen beim Absetzen, Tod, Logout und Abschalten weg. |
| C7 Sicherheit/Rechte | Befehle `/simplemobheads`, `/simplefun pvp` nur Stufe 2 (Werte geklemmt). Ritual prueft `mayBuild` + `mayInteract`, Cooldown je Schrein und Spieler (Weltspeicher). Alle Mixins brechen bei abgeschaltetem Mod/Schalter ab (`SimpleMobHeads.on()` + `abilities.<id>`); absichtlich immer aktiv: `SkullBlockEntityType/-Entity`, `HeadHelmetDrop` (Daten bleiben heil). Siehe P2-1, P3-3. |
| C8 Unabhaengigkeit | Nur Vanilla + SimpleLib + Framework-API (19 Zugriffe auf `com.simplebuilding.framework.api`); Simple Mobs/SB/Dragon Fight nur ueber Namen/Tags. Standalone-Target vorhanden (`modules.json`, laeuft ohne Cloth Config). |
| C9 Doku/Pflege | Kein TODO/FIXME; Lang EN=DE (533 = 533 Schluessel, 29 gleichlautende sind Namen). 9 Kern-Dubletten (P3-6). Test- und Diagnosecode liegt im Mod-Jar (Projektkonvention). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplemobheads-P2-1 | P2 (Logik, Spielerschaden) | Riesen- und Riesenkuh-Kopf: der Traeger waechst auf 1,5x (`Giants.wearerTick` 248-254, `wear` 256-266), **ohne zu pruefen, ob dort Platz ist**. Augenhoehe 1,62 x 1,5 = 2,43: in einem zwei Bloecke hohen Gang steckt das Auge in der Decke -> Erstickungsschaden (`IN_WALL`, etwa 1 Herz je Sekunde) bis der Kopf abgesetzt wird; Vanilla schiebt nur seitlich aus Bloecken, nicht nach oben. Riesen-Mobs pruefen den Platz sehr wohl (`hasRoom`, Giants.java:198-201). | **Rueckfrage**: A) Groesse nur, solange die vergroesserte Box frei ist (`noCollision`, wie bei den Mobs; in engen Gaengen faellt der Modifikator weg, im Freien kommt er zurueck) - **Empfehlung**; B) bleibt, Warnung im Handbuch; C) Groesse 1,2x statt 1,5x (Auge 1,94 < 2). Aenderung der vom Besitzer gewaehlten Groesse/Regel -> nicht selbst gefixt |
| simplemobheads-P3-1 | P3 (Mehrspieler) | Client-vorhergesagte Faehigkeiten (Frosch-Sprung `HeadAbilityLivingEntityMixin`, Eisbaer-Reibung, Spinne/Hoehlenspinne `HeadAbilityPlayerMixin`, Drowned-Blasen) fragen `HeadAbilities.active()` auf **beiden** Seiten, und `SimpleMobHeads.config()` ist auf dem Client die **lokale** Datei: schaltet ein Server eine Faehigkeit aus (oder weicht die Client-Config ab), sagt der Client sie trotzdem voraus -> Rueckschlaege beim Springen/Laufen. | Hinweis (Fix: Schalter einmal beim Beitritt per S2C-Payload an den Client; eigenes Vorhaben) |
| simplemobheads-P3-2 | P3 (Leak) | `SHORN`, `STEW`, `SNIFF_READY` (HeadAbilities.java:99-103) sind UUID-Maps ohne Bereinigung beim Verlassen, nur `forgetAll` beim Serverstop (gewollt: Abklingzeit ueberlebt Relog). Wachstum ~50 Byte je Spieler. | Hinweis |
| simplemobheads-P3-3 | P3 (Schutz) | `HeadHelmets.put/take` (94-123) pruefen kein `player.mayBuild()`: Abenteuer-Spieler koennen Helme auf platzierte Koepfe setzen und abnehmen (Spawnschutz/`mayInteract` gelten, Vanilla prueft sie vor dem Klick). Entspricht Vanillas Blumentopf/Lesepult. | Hinweis (bewusst; fuer Abenteuerkarten ggf. `mayBuild` ergaenzen) |
| simplemobheads-P3-4 | P3 | `HeadAbilities.tick` (306) laeuft auch fuer tote Spieler am Todesbildschirm (nur `isSpectator` wird ausgenommen; ein toter Spieler tickt laut Kommentar in `MobHeadTests#wornHeadDeath` bis zum Respawn weiter): mit behaltenem Kopf (Simple Tweaks T27 `hotbar_armor`) kann dort noch ein Ei fallen/gegraben werden - sehr kurz. | Hinweis (`!isAlive()` ergaenzen) |
| simplemobheads-P3-5 | P3 | Spieler-Kill-Chance: `playerKillChance + Looting x Bonus` (HeadDrops.java:131-133) ist nur je Summand geklemmt (<= 10 % + 5 % je Stufe), Looting > 3 (Befehl/Kreativ) kann 100 % ergeben. Standard aus. | Hinweis |
| simplemobheads-P3-6 | P3 (Pflege) | 9 Kern-Dubletten (`tools/dup_pairs.txt`: HeadAbilities + 7 Mixins + WornModHeadMixin): Fixes muessen im 26.2-Kern nachgezogen werden. `modules.json` fuehrt `cloth_config` unter `requires`, obwohl `fabric.mod.json` und das Standalone-Ziel ohne laufen (Q-B1). | Liste |
| simplemobheads-P3-7 | P3 (BS25-19) | **Seelenfeuer-Lohen-Kopf-Absturz (BS25-19): im Code nicht erkennbar.** Der Kopf hat keine Faehigkeit (`ability` leer), nur Tarnung, Noten-Klang und Gruss; der Tod-Pfad des Mods (`HeadDrops.onDeath`, `EasterDeathMixin` nur fuer Schweine, `HeadAbilities.tick`, `Giants.wearerTick`) enthaelt keine Schleife, kein Warten und keinen Weltzugriff ausser begrenzten Suchen; der Gametest `worn_head_death` (echter Tod + Respawn) war laut `docs/ai/PLAN-B-BS25CRASH-2026-10-25.md` gruen (hier nicht neu gelaufen). Verdacht bleibt beim Client (Renderer des toten Spielers, Todesbildschirm) oder bei Tweaks (T27); gelesen wurden die Client-Dateien nicht. | Hinweis (Besitzer-Log fehlt; Client-Lauf steht als b-bs25crash31 aus) |
| simplemobheads-P3-8 | P3 | Fremde Spieler koennen einen Traeger des Kuh-/Schaf-/Pilzkuh-Kopfes ohne Zustimmung melken/scheren/Suppe abfuellen (`HeadAbilities.interact` 256-296; Abklingzeit 2 Min/1 s, kein Schaden, kein Itemverlust). | Hinweis (bewusst) |

## Urteil
**shipbar** (0 P1, 1 P2 Rueckfrage, 8 P3). Die Drop-Kette ist sauber (ein Kopf je Explosion, keine Doppelbelegung, Config bei jedem Wurf gelesen), Mixins und Zeitgeber brechen
bei abgeschaltetem Mod sofort ab, Daten (Variante, Helm) bleiben auch dann erhalten. Offen ist nur die Platzpruefung beim Riesen-Kopf (Besitzer-Entscheid).

## Testergebnis
Lokal: keine Code-Aenderung in diesem Mod, daher kein javac-Lauf noetig; `modules/simplemobheads/tools/check_data.py` gruen (97 Koepfe, Texte, Modelle, Loot, Wiki). Vanilla-Verhalten per javap
gegen 26.3 belegt (`Creeper#killedEntity`, `LevelChunk#setBlockState` -> `preRemoveSideEffects`, `PlayerList#respawn` kopiert Tags, Vanilla-Schaedel-Loot ohne `survives_explosion`).
sbt: siehe `docs/ai/PLAN-A-AUDSB31-2026-11-12.md`.
Nicht getestet: Client (Rendern, Todesbildschirm, BS25-19 im echten Client), Mehrspieler-Rueckschlaege (P3-1), Forge/NeoForge; P2-1 ist nur gelesen, kein Gametest in zu niedrigem Gang.
