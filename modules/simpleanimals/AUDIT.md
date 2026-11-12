# Audit simpleanimals (a-audsb31e2, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplestorage/AUDIT.md` und `modules/simplemobheads/AUDIT.md`.
Umfang: 32 Java-Dateien / 2,8k Zeilen ohne Tests (davon Client 17 Dateien / 0,66k, Guide + Discover 0,46k); Katalog `test/AnimalsTests.java` 26 Gametests / 0,7k Zeilen, Fabric-Adapter und `clienttest/` zusaetzlich.
Der Mod hat **keine** Netzwerk-Payloads, **keine** Menues, **keine** Block-Entities und keine Befehle ausser SimpleLibs `ConfigCommands`: C1 und der Menue-Teil von C3 entfallen. Risikogeleitet gelesen: Entities
(Aggro, Brut, Zaehmen), Spawn-Regeln, Trueffel-Graben (Weltaenderung?), Drops, Leaks.

## Vollstaendig gelesen
- Entities: `entity/Boar.java` (298), `Hedgehog.java` (349, mit `CurledGoal` und `HuntSilverfishGoal`), `Deer.java`, `Crab.java`, `Zebra.java`, `Zorse.java`; Vanilla per javap gegen 26.3:
  `Horse#canUseSlot` (liefert wie `Zebra` immer `true`), `AbstractHorse#canUseSlot`, `EntityReference#of` (null-sicher).
- Spawns/Varianten/Config: `AnimalsSpawn.java`, `AnimalVariants.java`, `config/AnimalsConfig.java`, `SimpleAnimals.java`, `AnimalsRegistry.java`, `fabric/.../AnimalsFabric.java` (Biom-Modifikationen,
  Kreativ-Tabs), `fabric.mod.json`, `modules/modules.json`-Eintrag.
- Daten: `loot_table/entities/boar.json` und `gameplay/boar_truffle.json` (die uebrigen 5 Loot-Tabellen, 4 Rezepte, 9 NeoForge-Biom-Modifier per `tools/check_data.py`).
- Nicht gelesen: `client/*` (17 Dateien: Renderer, Modelle, Layer), `guide/AnimalsGuide.java`, `discover/AnimalsDiscover.java`, Tests, Texturen.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Entfaellt: keine Payloads. |
| C2 Server-Autoritaet / Reichweite | Alle Entscheidungen im Server-Zweig (`aiStep` prueft `isClientSide`, `hurtServer`, Goals). Spielerzustand fuer die Aggro kommt aus Vanillas Spielerliste (`getNearestPlayer`, Sneak/Kreativ/Zuschauer-Filter), nicht aus Client-Daten. Client-Klassen nur unter `client/`. |
| C3 Dupes / Verlust | Keine Container, keine Slots. Brut: Elternvariante wird in `getBreedOffspring` kopiert, `finalizeSpawn` laeuft bei Zucht nicht (Variante bleibt). Zebra x Pferd -> Zorse (`canMate` des Zebras, Zorse unfruchtbar: `canMate` false, `getBreedOffspring` null). Drops: Vanilla-Loot-Tabellen je Tier, Truffel nur ueber die eigene Tabelle; Pferde-Ausruestung wie Vanilla-`Horse`. |
| C4 NBT/Komponenten | `Variant`, `Model`, `Antlers`, `Curled` werden mit Standardwert gelesen und auf den Katalog begrenzt (`AnimalVariants.clamp`, `setModel` 0/1); keine Listen, keine SavedData. |
| C5 Tick-Kosten | Eber `aiStep`: eine `getNearestPlayer`-Abfrage je Tick (Radius 3, Spielerliste); Trueffel-Wurf alle 200 Ticks. Igel: `threatened()` alle 5 Ticks (Box 8x4x8), Silberfisch-Suche alle 20 Ticks (Radius 2-16). Ferkel-Alarm nur bei verletztem Ferkel. Spawns: Gewichte 1-4, `spawnScale` verduennt weiter (P3-4). |
| C6 Leaks | Keine statischen Maps/Sets/Weltreferenzen (`AnimalVariants`: unveraenderliche Kataloge). Ziel-/Besitzerverweise nur ueber Vanilla-Goals. |
| C7 Sicherheit/Rechte | **Trueffel-Graben aendert keine Bloecke** (`digTruffles` Z.218-232: Item, Partikel, Klang; `canDig` liest nur den Boden), daher weder `mobGriefing` noch Claim-Schutz noetig. Igel toetet Silberfische (nur Tier, keine Bloecke). Kein Mixin, kein Befehl, keine Dateizugriffe ausser der Config (`ConfigFiles.writeAtomic`). Eber-Aggro respektiert Kreativ/Zuschauer/Frieden ueber Vanillas `TargetingConditions`. |
| C8 Unabhaengigkeit | Allein spielbar: `fabric.mod.json` `depends` nur `simplelib`, `suggests` simplebuilding/simplemoney/simplemobheads (= `modules.json optional`; das Modul selbst nennt keinen der drei, die Gegenrichtung steht in `simplemobheads/data/mobs.json` und Simple Moneys Preisliste). Standalone-Ziel `fabric/build.gradle` vorhanden. |
| C9 Doku/Pflege | Keine TODO/FIXME. Lang EN=DE: 49 Schluessel, identisch. Keine Kern-Dubletten. `check_data.py` gruen (Spawn-Listen Fabric = NeoForge). Amerikanisches Englisch geprueft (Stichwortsuche). |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simpleanimals-P3-1 | P3 (Spielgefuehl) | Die Eber-Aggro ist Mob-weit, nicht zielgenau: `provokedBy(target)` (`Boar.java` Z.161-165) liest den Schubs-/Naehe-Zaehler des ganzen Ebers, `NearestAttackableTargetGoal` nimmt dann den naechsten Spieler im Folgebereich (16), der das besteht. Schleicht der Ausloeser weg, solange der Zaehler hoch ist (`boarCloseSeconds` 2-30 s) oder der Schubs laeuft (5 s), kann ein unbeteiligter Zweiter in 4-16 Bloecken das Ziel werden. Kein Weltschaden, Schaden 4/6/8 je Schwierigkeit. | Hinweis (Besitzer, Spielgefuehl); kein Fix ohne Mechanik-Entscheid |
| simpleanimals-P3-2 | P3 (Config-Grenze) | `boarTruffleChance` ist bis 1,0 einstellbar (`AnimalsConfig.TRUFFLE_MAX`): dann wirft jeder erwachsene Eber auf Erde alle 10 s 1-2 Trueffel (Itemfluss ohne Obergrenze, Eberfarm). Standard 0,005 (etwa einer je 33 Min je Eber), nur durch Admins aenderbar. | Rueckfrage an den Besitzer (Balance): A Grenze lassen (Admin-Entscheid); B `TRUFFLE_MAX` auf 0,05 senken; C Tageslimit je Eber. Empfehlung A. |
| simpleanimals-P3-3 | P3 | Keine `SpawnPlacements`-Registrierung: die Boden-/Licht-Regeln stehen nur in `checkSpawnRules` der Instanzen (greift bei natuerlichem Spawn und bei der Chunk-Erzeugung ueber `Mob#checkSpawnRules`). Fremde Mods/Spawner, die nur `SpawnPlacements` fragen, sehen keine Einschraenkung; Spawn-Eier/Spawner/Zucht sind bewusst frei. | Hinweis (funktioniert wie beabsichtigt) |
| simpleanimals-P3-4 | P3 (Tick-Kosten) | Igel: `threatened()` (Z.138-143) ruft alle 5 Ticks je erwachsenem Igel `getEntitiesOfClass(LivingEntity, ...)` mit Praedikat, `HuntSilverfishGoal.canUse` (Z.305-316) alle 20 Ticks eine Suche bis Radius 16; skaliert linear mit der Zahl der Igel (Zucht mit Suessbeeren, nur Mobcap begrenzt). Unkritisch. | Hinweis |
| simpleanimals-P3-5 | P3 (BEW39: Feature offen, nicht Teil des Audits) | Mob-Koepfe fehlen fuer alle sechs Tiere (`simplemobheads/data/mobs.json`: "head comes later"), Hirsch mit Lama-Klaengen, Client-Abnahme/Besitzer-Wahl der Varianten und Modelle (Krebs-Beine, Zebra B) offen, Config-Optionstexte (`text.autoconfig.simpleanimals.option.*`) fehlen, 7 Texturen orange. Nicht gebaut. | notiert |

## Urteil
**shipbar** (0 P1, 0 P2, 5 P3). Keine Weltaenderung durch Tiere (Trueffel-Graben, Igel-Jagd und Eber-Rammen aendern keine Bloecke), Pferde-Verhalten von Zebra/Zorse entspricht Vanillas `Horse`, Spawn-Regeln
sind mehrfach begrenzt (Gewicht, Licht, Boden, Schalter, `spawnScale`), keine Leaks. Offen sind Spielgefuehl und Inhalt (Besitzer), kein Fehler im Code.

## Testergebnis
Lokal: keine Code-Aenderung in diesem Mod, daher kein javac-Lauf noetig; `modules/simpleanimals/tools/check_data.py` gruen, `tools/check_dup_impl.py` 0 Probleme, Lang EN=DE per Skript gleich.
Vanilla-Verhalten per javap gegen 26.3 belegt (`Horse#canUseSlot`, `EntityReference#of`).
sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: Client (Modelle, Tiere im echten Spiel), Mehrspieler-Aggro (P3-1), echtes Spawnverhalten ueber viele Chunks, Forge/NeoForge.
