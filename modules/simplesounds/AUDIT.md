# Audit simplesounds (b-rel32-aes, 31.10.2026)

Basis `b-rel32` 1be1d2873, nur MC 26.3 Fabric. Gelesen: alle Fabric- und Shared-Java-Dateien des Mods vollstaendig, dazu fabric.mod.json, Mixin-JSON, Gradle, Daten, Tools-Skript und genutzte Framework-Klassen (`SubMods`, `ConfigFiles`, `CosmeticIntensity`). Gametest-Klassen (SoundTests) nur ueberflogen, Forge/NeoForge-Zweige nicht Teil dieses Releases (nur Dateiliste). Nichts veroeffentlicht.

Durchgesehene Dateien (40 Dateien, 1777 Zeilen; PNG/Binaerdateien nur auf Herkunft geprueft). Forge/NeoForge-Teile sind nur gelesen, nicht Teil dieses Releases (nur 26.3 Fabric):

- `clienttest/java/com/simplebuilding/modules/simplesounds/ModuleClientSmoke.java` (16)
- `docs/next-small-plan.md` (100)
- `fabric/build.gradle` (8)
- `fabric/src/main/java/com/simplebuilding/modules/simplesounds/ModuleGameTest.java` (29)
- `fabric/src/main/java/com/simplebuilding/modules/simplesounds/SoundsFabric.java` (11)
- `fabric/src/main/java/com/simplebuilding/modules/simplesounds/SoundsFabricClient.java` (9)
- `fabric/src/main/java/com/simplebuilding/modules/simplesounds/SoundsModMenu.java` (2)
- `fabric/src/main/resources/fabric.mod.json` (48)
- `forge/build.gradle` (8)
- `forge/src/main/java/com/simplebuilding/modules/simplesounds/SoundsForge.java` (13)
- `forge/src/main/java/com/simplebuilding/modules/simplesounds/forge/ModuleForgeTests.java` (26)
- `forge/src/main/java/com/simplebuilding/modules/simplesounds/forge/SoundsForgeClient.java` (6)
- `forge/src/main/java/com/simplebuilding/modules/simplesounds/forge/mixin/RegistryLoadTaskMixin.java` (54)
- `forge/src/main/resources/META-INF/mods.toml` (45)
- `forge/src/main/resources/pack.mcmeta` (10)
- `forge/src/main/resources/simplesounds.forge.mixins.json` (12)
- `neoforge/build.gradle` (18)
- `neoforge/src/main/java/com/simplebuilding/modules/simplesounds/ModuleNeoTests.java` (38)
- `neoforge/src/main/java/com/simplebuilding/modules/simplesounds/SoundsNeoClient.java` (6)
- `neoforge/src/main/java/com/simplebuilding/modules/simplesounds/SoundsNeoForge.java` (11)
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml` (43)
- `shared/java/com/simplebuilding/modules/simplesounds/ExampleItems.java` (2)
- `shared/java/com/simplebuilding/modules/simplesounds/SoundBudget.java` (44)
- `shared/java/com/simplebuilding/modules/simplesounds/SoundClient.java` (57)
- `shared/java/com/simplebuilding/modules/simplesounds/SoundClientAssertions.java` (21)
- `shared/java/com/simplebuilding/modules/simplesounds/SoundConfig.java` (68)
- `shared/java/com/simplebuilding/modules/simplesounds/SoundScreen.java` (24)
- `shared/java/com/simplebuilding/modules/simplesounds/SoundTests.java` (65)
- `shared/java/com/simplebuilding/modules/simplesounds/SoundsRegistry.java` (22)
- `shared/java/com/simplebuilding/modules/simplesounds/client/SoundsGuideCommand.java` (26)
- `shared/java/com/simplebuilding/modules/simplesounds/guide/SoundsGuide.java` (59)
- `shared/java/com/simplebuilding/modules/simplesounds/mixin/SoundTickMixin.java` (7)
- `shared/resources/assets/simplesounds/effects.json` (98)
- `shared/resources/assets/simplesounds/icon.layers.json` (100)
- `shared/resources/assets/simplesounds/lang/de_de.json` (89)
- `shared/resources/assets/simplesounds/lang/en_us.json` (89)
- `shared/resources/data/simplesounds/structure/empty.nbt` (19)
- `shared/resources/simplesounds.mixins.json` (11)
- `tools/check_data.py` (34)
- `wiki/manual.json` (429)

## Herkunft der Audiodaten (C8, Pflichtpunkt)
Das Jar enthaelt **keine einzige Audiodatei** (`find modules/simplesounds -name '*.ogg|*.wav|*.mp3'` leer, kein `sounds.json`). Die zwoelf Effekte in `effects.json` verweisen ausschliesslich auf Vanilla-Sound-Events (`minecraft:...`), die zur Laufzeit lokal ueber `playLocalSound` abgespielt werden; `SoundsRegistry.load()` lehnt jede Zuordnung ausserhalb `minecraft:` ab. Damit gibt es weder ElevenLabs- noch fremde Dateien, und keine Mojang-Assets im Jar (die Sounds liegen im Spiel des Nutzers). Keine Lizenzfrage offen. Die `previews/*.png` (Screenshots) liegen ausserhalb von `src`/`resources` und gelangen nicht ins Jar.

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Code | `SoundClient.tick` (Client-Tick, Mixin) mit hartem Budget (`SoundBudget`: 4/Tick, 2/Spieler, 8 Spieler, Cooldown >= 20 Ticks, Warteschlange verworfen). Config wird beim Lesen geklemmt. Befund P3-1 (Tick-Allokation) gefixt. Kein Server-Pfad: Mixin steht nur in `client`. |
| C2 Mixins | `SoundTickMixin` -> `Minecraft.tick` TAIL, nur Client, `require 1`. Konfliktrisiko gering (viele Mods injizieren dort, TAIL-Inject ist kooperativ); Sodium/Iris beruehren `Minecraft.tick` nicht. |
| C3 Config | Client-Config `config/simplesounds.json`, Cloth-Bildschirm ueber Mod Menu, `/simpleclient simplesounds`; Grenzen geklemmt (Cap 0,25, Cooldown 20-1200); Lang EN/DE vollstaendig mit Defaults (`check_data.py` gruen lokal). Kaputte Datei wird jetzt wie bei den anderen Mods unter `.broken-<zeit>` abgelegt statt still ueberschrieben (P3-2, gefixt). |
| C4 Daten | `effects.json` 12 Eintraege, Sound-Events muessen in der Registry existieren (Test `effect*`); Lang EN/DE gleiche Keys, kein "?"-Schaden. |
| C5 Unabhaengigkeit | allein spielbar: folgt der Visuals-Stufe nur, wenn Visuals eine Stufe veroeffentlicht (`CosmeticIntensity`), sonst eigene Stufe. `suggests` simplevisuals/modmenu. |
| C6 A12 | rein lokal-kosmetisch, Standard `SUBTLE`, Lautstaerkendeckel 0,25; kein Eingriff in Gameplay. |
| C7 Netzwerk | keine Pakete (nur lokaler Spielerzustand). Dedicated Server: Mod-Main registriert nur den Befehls-Spec; `depends cloth-config` gilt aber auch auf dem Server (P2-1, Rueckfrage). |
| C8 Lizenz/Assets | siehe oben; CC0-1.0; kein LICENSE-Datei im Modulordner (P3-3). |
| C9 Offen | `docs/next-small-plan.md` ist ein veraltetes, themenfremdes Plan-Dokument (QoL-Haltbarkeit, Branch codex-next-small) - P3-4, nicht im Jar. |

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| simplesounds-P2-1 | P2 | `fabric.mod.json` `depends cloth-config` (und fabric-api) ohne Umgebungsangabe: ein Dedicated Server mit Simple Sounds muss Cloth Config installieren, obwohl der Mod rein clientseitig wirkt. Gleiches gilt fuer simplevisuals. | **Rueckfrage**: `depends` -> `recommends`/`suggests` fuer cloth-config + Mod-Menu-Eintrag per `isModLoaded` absichern, oder `environment: client` (letzteres entfernt den Mod aus Server-Gametests) |
| simplesounds-P3-1 | P3 | `SoundClient.tick` berechnete `CONFIG.volume()` (normalisiert + allokiert ein HashSet) fuer alle 12 Effekte in jedem Tick, auch inaktive | **gefixt** (nur fuer aktive Effekte) |
| simplesounds-P3-2 | P3 | `SoundClient.load` schluckte Lesefehler, kaputte Datei wurde beim naechsten Speichern ueberschrieben | **gefixt** (`ConfigFiles.readOrQuarantine`) |
| simplesounds-P3-3 | P3 | Kein `LICENSE` im Modulordner | Hinweis |
| simplesounds-P3-4 | P3 | veraltetes `docs/next-small-plan.md` | Hinweis (loeschen, nicht noetig fuers Jar) |
| simplesounds-P3-5 | P3 | "fireflies"/"pollen" nutzen die Overworld-Uhr auch im Nether/End (nur kosmetisch) | Hinweis |

## Urteil
**shipbar** (0 P1, 1 P2 als Rueckfrage, 5 P3; 2 gefixt). Kein Lizenzrisiko bei den Sounds.

## Testergebnis
@@TEST@@
