# Release-Audit Familie Simple Optimization (`b-rel32-opt`, 31.10.2026)

Mods: `simpleoptimization` (Super-Mod), `simpleoptrender`, `simpleoptmemory`, `simpleoptserver`. Alle Fabric 26.3, Version 0.1.0.
Basis `b-rel32` d9cda1e21. Je Mod ein Bericht `modules/<mod>/AUDIT.md` (Dateiliste mit Zeilenzahlen, C1-C9, Befunde, Urteil).
Nichts wurde veroeffentlicht oder hochgeladen.

## 1. Plan (Kopf)
- Dateiliste je Mod: siehe `AUDIT.md` (alle `.java`, Mixin-JSON, `fabric.mod.json`, Lang, Ressourcen, `wiki/manual.json`, Tools). Zusammen 3489 Zeilen (Java, JSON, Gradle, Python).
- Reihenfolge: Super-Mod (159 Z.) -> Memory (425) -> Server (638) -> Render (1103, Client-Mixins zuletzt, mit `javap` gegen das 26.3-Client-Jar). Genutzt: `framework` `OptAreas`, `OptCompat`, `OptSettings`, `ConfigFiles`, `SubMods`.
- Pruefliste C1-C9 aus `docs/ai/RELEASE-QOL-OPT-AES.md` Abschnitt 2; danach ein Test-Lauf (Abschnitt 6).

## 2. Ergebnis in Kuerze
| Mod | Urteil | P1 | P2 | P3 |
|---|---|---|---|---|
| simpleoptimization | shipbar | 0 | 0 | 1 (Hinweis) |
| simpleoptrender | shipbar, Vorbehalt Sodium/Iris-Lauf | 0 | 0 | 5 (2 gefixt, 3 Hinweise/Rueckfrage) |
| simpleoptmemory | shipbar | 0 | 1 (gefixt) | 3 (2 gefixt, 1 Hinweis) |
| simpleoptserver | shipbar | 0 | 1 (gefixt) | 3 (1 gefixt, 2 Hinweise) |

Fix-Commits: `2e29b17fb` (OptSettings atomar schreiben, framework), `64e80c0cc` (Memory/Server-Mixin-Config nicht `required`, Render-Beschreibung, Memory `depends fabric-api`), `AUDIT.md`-Commit danach.
Kernbefund (P2): `simpleoptmemory` und `simpleoptserver` hatten `required:true` + `defaultRequire:1`; ein verschobenes Ziel in einem 26.3.x-Patch haette das Spiel beim Start beendet. Konzept 4.4 verlangt "Bereich aus, nie Absturz" (Render macht das mit `MixinGuard`). Jetzt `required:false`/`defaultRequire:0`; die Gametests (`sharedStates`, `s5b*`) melden ein stilles Nichtanwenden.

## 3. Abnahmeplan fuer den Besitzer (In-Game)
Voraussetzung: Minecraft 26.3 mit Fabric, Fabric API, die vier Opt-Jars (oder nur das Super-Mod-Jar `simpleoptimization`, das die drei anderen enthaelt). Beim Start schreibt jeder Sub-Mod eine Tabelle ins `latest.log` ("simpleoptrender areas:" usw.); die Schritte beziehen sich darauf. Config-Aenderungen gelten nach Neustart.

### simpleoptimization (Super-Mod)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-OPT-1 | Nur das Super-Mod-Jar `simpleoptimization` (+ Fabric API) in `mods`, Spiel starten | Start ohne Fehler; falls Mod Menu installiert ist, zeigt es Simple Optimization mit den drei Sub-Mods als Kinder; `config/simpleoptimization.json` mit drei Schaltern (alle `true`) |
| QOLA-OPT-2 | `/simpleoptimization config` im Spiel | Befehl zeigt die drei "Enable Simple ..."-Schalter (Default: true) und weist auf den noetigen Neustart hin |
| QOLA-OPT-3 | In der Datei `enableSimpleOptRender` auf `false`, Neustart, `latest.log` lesen | Tabelle von Simple Opt Render zeigt alle Bereiche `off (switch_off)`; Memory und Server unveraendert `on` |
| QOLA-OPT-4 | Datei mit kaputtem Text ueberschreiben, Start | Spiel startet, Log meldet Quarantaene, Datei wird neu mit Standardwerten geschrieben, Kopie der kaputten Datei liegt daneben |

### simpleoptrender (Client)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-OPT-R1 | Singleplayer-Start, Log lesen | "simpleoptrender areas:" mit sechs Zeilen (leaf_culling O1, lightmap_cache O7, fast_ip_ping M1, no_telemetry M2, more_culling O3, details O9), alle `on (no_rival)` ohne Fremd-Mods |
| QOLA-OPT-R2 | Grafik "Schoen", in einen dichten Eichenwald-Wipfel fliegen/stellen, von aussen und innen ansehen | Von aussen kein Loch, Kronen sehen wie Vanilla aus; innen keine ueberfluessigen Wand-Flaechen (es sieht "voll" aus). Bei `mode: off` (Neustart) identisches Aussehen von aussen |
| QOLA-OPT-R3 | `/simpleclient simpleoptrender`, `depth` 2, Neustart, gleiche Krone | Mehr Flaechen bleiben (dichter), keine Loecher |
| QOLA-OPT-R4 | Tag -> Nacht warten, Helligkeit-Regler (Gamma) bewegen, Nachtsicht-Trank trinken | Beleuchtung folgt sofort, kein haengendes oder flackerndes Licht (Lichtkarten-Cache) |
| QOLA-OPT-R5 | Mehrspieler -> Server hinzufuegen mit reiner IP (z. B. `127.0.0.1` oder die eigene LAN-IP) | Eintrag erscheint ohne Verzoegerung, Ping/MOTD werden geladen; Eintrag mit Hostnamen funktioniert wie zuvor |
| QOLA-OPT-R6 | Log nach Spielstart nach "telemetry" durchsuchen | Keine gestartete Telemetrie-Sitzung (`mode: off` zum Gegenpruefen) |
| QOLA-OPT-R7 | Schild mit Text auf beide Seiten, Wandschild und Haengeschild; von vorn, von hinten, von der Seite ansehen | Beide Texte immer von der jeweils richtigen Seite lesbar, nie fehlend; nahe der Bretter-Ebene (seitlich) sind beide sichtbar |
| QOLA-OPT-R8 | Item-Rahmen (auch leuchtender) und Karte an der Wand; um die Wand herum gehen | Von vorn wie Vanilla; von der Rueckseite der Wand ist nichts zu sehen, kein Flackern beim Herumgehen |
| QOLA-OPT-R9 | Leuchtfeuer mit Strahl aufbauen, Blick abwenden und zurueck | Strahl verschwindet ausserhalb des Sichtfelds und ist sofort wieder da, kein Aufblitzen am Bildrand |
| QOLA-OPT-R10 | Zusaetzlich Sodium installieren (und mit Iris + Shaderpack) | Start ohne Fehler; Log: `leaf_culling off (rival_wins: sodium)`; Wald, Schilder, Rahmen, Leuchtfeuer sehen richtig aus; Warnzeilen "possible conflict" sind erlaubt, kein Absturz (Pflicht-Lauf vor Veroeffentlichung, bisher nie gefahren) |
| QOLA-OPT-R11 | Cull Leaves oder More Culling zusaetzlich einwerfen | Der jeweilige Bereich steht auf `off (rival_wins: ...)`, der Rest bleibt an |
| QOLA-OPT-R12 | `details`: `particles false` -> Block abbauen; `textureAnimations false` -> Wasser/Lava ansehen; `fog false` -> Fernsicht, dann unter Wasser, in Lava, mit Blindheit-Trank | Keine Partikel; Wasser/Lava stehen still; Luft-Nebel am Horizont weg, Wasser-/Lava-/Blindheits-Nebel bleiben |

### simpleoptmemory (Client + Server)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-OPT-M1 | Spiel starten, Log lesen | "simpleoptmemory areas:" mit `state_tables O4 on`; spaeter Zeile "O4 state tables: N of M arrays shared, about X KiB saved" (nur Zaehlung, kein Messwert) |
| QOLA-OPT-M2 | Welt laden; Treppen, Tueren, Redstone-Leitung, Trichter, Zaun setzen und umschalten | Alles verhaelt sich wie Vanilla (Ausrichtungen, Redstone-Zustaende, Waessern) |
| QOLA-OPT-M3 | FerriteCore dazu | Log: `state_tables off (rival_wins: ferritecore)`, Spiel laeuft |
| QOLA-OPT-M4 | `mode: off` in `config/simpleoptmemory.json`, Neustart | Keine "state tables"-Zeile; Spiel laeuft |

### simpleoptserver (Server)
| ID | Aktion | Erwartet |
|---|---|---|
| QOLA-OPT-S1 | Dedizierten Server mit den Jars starten | Log: "simpleoptserver areas:" (`dynamic_distance on`, `activation_range off (config_off)`), "S5a dynamic distance: view 6..N, simulation M (fixed), target 45 MSPT" |
| QOLA-OPT-S2 | Als Operator `/simpleopt status`; als Nicht-Operator | Operator sieht je Bereich "on/off (Grund)" und die aktuelle Weite; Nicht-Operator bekommt keine Berechtigung |
| QOLA-OPT-S3 | `targetMspt` auf 1 setzen, Neustart, Spieler beitreten | Alle 15 s sinkt die Sichtweite um 1 bis `minView` (Log "S5a: MSPT ... -> view ..."), nie unter 3; mit `targetMspt` 45 und ruhigem Server steigt sie wieder bis `server.properties` |
| QOLA-OPT-S4 | `dynamic_distance.mode` auf `off`, Neustart, Spieler beitreten | Sichtweite bleibt auf dem Wert aus `server.properties`, Status zeigt `off (config_off)` |
| QOLA-OPT-S5 | S5b: `activation_range.mode` auf `auto`, Neustart; Kuehe/Huehner/Dorfbewohner in 60+ Bloecken Abstand, Spieler weit weg lassen; danach hingehen | `/simpleopt status` zeigt `activation_range on`; Mobs in der Naehe bewegen sich normal; Tiere paaren sich, Huehner legen Eier, Dorfbewohner arbeiten auch fern ganz normal (ausgenommen); ferne untaetige Mobs stehen, ohne zu springen oder zu verschwinden, und laufen bei Annaeherung sofort normal |
| QOLA-OPT-S6 | Vanilla-Client (ohne Mod) auf diesen Server | Beitritt moeglich, Status-Texte im Klartext |

## 4. Stand der Optimierungs-Konzeptstufen
Quellen: `docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md` (Abschnitte 7, 9-11), `.claude/QUEUE.md` (Zeilen e2/e3/e4/e5, END-1/END-2, OPT-e4), Memory `uebergabe-2026-10-08.md`, `bench-results/`, Code.

| Stufe | Queue | Inhalt laut Konzept | Stand im Code |
|---|---|---|---|
| 0 | e | Konzept | fertig; Besitzer-Antworten 17.10. in Abschnitt 9 (alle Empfehlungen, Abweichung: Benchmarks auf sb-test) |
| 1 | e1 | Geruest Super-Mod + 3 Sub-Mods, Bereichs-Tabelle, Mixin-Plugin, Config, Status-Befehl, Bench-Harness `tools/bench` | gebaut und in master (`OptCompat`, `OptAreas`, `OptSettings`, Plugin je Sub-Mod, `/simpleopt status`, `run_bench.py`, `fetch_mods.py`, `modsets.json`) |
| 2 | e2 [x] | O1 Blaetter-Culling, O7 Lichtkarte, S5a Sichtweite nach Last, M1 IP-Ping, M2 Telemetrie, Kompat-Ziel | gebaut, Gametests gruen; Ziel `compat-opt-263` (Cull Leaves, More Culling, BadOptimizations, ServerCore) existiert in `tools/testrunner/run.py`, **wurde nie gefahren** |
| 3 | e3 [ ] (END-1) | Benchmark am Besitzer-PC, `priorities.json` fuellen | **offen, braucht den Besitzer**; `priorities.json` (je Sub-Mod): alle Bereiche `theirs`, `measured ""`, `runs 0` |
| 4 | e4 / OPT-e4 [~] | O3, O4, O9, S5b (Standard aus), Zielpruefung | gebaut und in dieser Basis. Begruendet **nicht gebaut**: O3 Regen hinter Decken, O4 Modell-Deduplizierung (Vanilla 26.3 macht es), S1a (Messung: kein Familien-BE teurer als der Vanilla-Trichter). Die Queue-Zeile OPT-e4 steht noch auf "[~] laeuft (a-opt45b)", der Inhalt ist laut Konzept 11 und Code erledigt -> kann auf [x] (Hauptsitzung entscheidet). |
| 5 | -- | O2, O6, O5, S1b, S3, S6, O10 nur bei Messbedarf | **nicht gebaut**, Bedarf nur durch e3 belegbar; fuer das Release nicht noetig |
| K | e5 [ ] (END-2) | Kompat-Test Modpack K1-K23 | **offen**. Fuer die Opt-Mods relevant: K15 Iris, K16 Sodium/More Culling/Cull Leaves/Entity Culling, K17 ImmediatelyFast, K18 Lithium/C2ME/ServerCore/ScalableLux (Mixin-Konflikte). Es gibt weder einen `claude-opt-compat`-Branch noch ein Ergebnis; `compat-opt-263` deckt nur 4 der Fremd-Mods ab und weder Sodium noch Iris |

### Was fehlt fuer das Release
1. Pflicht (ohne Besitzer, auf sb-test machbar): `compat-opt-263` einmal fahren; `mixinaudit-client-263` (wird vom Bundle-Gate ohnehin fahren). Ein echter Client-Lauf mit Sodium und Iris (QOLA-OPT-R10) fehlt, ist aber das groesste Restrisiko der Render-Mixins.
2. Besitzer: e3 (Benchmark am PC, FPS/1%-Low entscheiden nur dort) und e5 (Modpack-Test K15-K18 mit seinem echten Pack). Beides ist **kein Blocker fuer Version 1.0**, solange die Texte keine Leistungsgewinne zusagen (s. u.); ohne e3 bleibt jeder Bereich "Fremd-Mod gewinnt", unser Teil laeuft nur, wenn kein Rivale installiert ist.
3. Vorhandene Messungen (`bench-results/2026-10-18-sbtest*`, sb-test, Software-GL llvmpipe, 3 Laeufe je Szene) belegen **keinen** Gewinn: O1 FPS vanilla 20,1 +- 0,7, Cull Leaves 19,6, More Culling 19,6, unsere 18,1 +- 1,1 (zweiter Lauf: 19,98 / 19,33 / 19,80 / 19,45); der Quad-Zaehler stimmt nur mit Fremd-Mods (Vanilla-Szene 0, nicht belastbar), unsere Quad-Zahl (2277) entspricht der von Cull Leaves (2277). S5a MSPT vanilla 6,3 +- 1,5, ServerCore 4,0 +- 0,2, unsere 3,6 +- 0,1 - ohne gemessene Last und ohne Weiten-Zaehler nicht dem Bereich zuzuschreiben. Konzept 9: Software-GL, "GPU-gebunden, nur Tendenz".
4. Vorhandene, als Beleg brauchbare Zahlen (headless, Gametest-Logs "SB-BENCH", Konzept 11): S5b spart bei 36 untaetigen, fernen Kuehen 72/73 % der Mob-Tick-Zeit (3 von 4 Ticks uebersprungen); das ist die Zeit der Mob-Ticks, nicht der ganze MSPT. O4: 193 643 von 262 327 Arrays geteilt, geschaetzt 7,1 MiB (Schaetzung, kein Heap-Vergleich). S1a: Tabelle der Leerlaufkosten, kein Gewinn.

### Behauptungen in Texten (Regel: nur mit Beleg)
Erlaubt: "teilt identische Blockzustands-Tabellen", "ueberspringt Lichtkarten-Uploads ohne Bildaenderung", "aeussere Blaetter bleiben sichtbar, Innenflaechen entfallen", "S5b: ferne untaetige Mobs ticken nur jeden 4. Tick (Standard aus)", "weicht bekannten Mods". Nicht erlaubt: FPS-, MSPT- oder MB-Zusagen ohne e3.

## 5. Release-Punkte
### Version / Changelog (Entwurf)
- Vorschlag Version 1.0.0 fuer alle vier Jars (Versionsschema 0.1.0, ST-14; Entscheidung beim Besitzer, **Rueckfrage**: alle 1.0.0 oder Opt bei 0.x lassen, weil e3/e5 offen sind).
- Changelog 1.0.0 (Entwurf): Neu: Simple Opt Render (Blaetter-Culling, Lichtkarten-Cache, weiteres Culling fuer Schilder/Rahmen/Leuchtfeuer, schneller IP-Ping, Telemetrie aus, Detail-Schalter). Simple Opt Memory (geteilte Blockzustands-Tabellen). Simple Opt Server (Sichtweite nach Serverlast, Aktivierungsbereich fuer ferne Mobs, Standard aus). Super-Mod bundelt alle drei. Jeder Bereich weicht bekannten Mods und laesst sich einzeln abschalten.

### Beschreibungen (Entwurf, Modrinth/CurseForge)
| Mod | Kurz (<= 100 Zeichen) | Lang |
|---|---|---|
| Simple Optimization | Own optimizations, each switchable; steps back for known mods. | Bundle of Simple Opt Render, Memory and Server. Each optimization is our own implementation, can be switched off on its own, and turns itself off when a known mod covers the same area (Sodium, Cull Leaves, More Culling, BadOptimizations, FerriteCore, ServerCore, Fast IP Ping, No Telemetry, Sodium Extra). We never switch another mod off. Install the sub-mods alone if you only need one side (e.g. only Simple Opt Server on a server). |
| Simple Opt Render | Client: leaf culling, light-map cache, more culling, fast IP ping, telemetry off. | Client-side optimizations: leaves hide faces they share with leaves (fancy look outside stays), the light map is only uploaded when it changes, hidden sign sides / item frames behind walls / beacon beams out of view are not drawn, literal IPs skip DNS lookups, telemetry is off. Optional detail switches (particles, texture animations, air fog; all default to Vanilla). Each area steps back for Sodium, Cull Leaves, More Culling, BadOptimizations, Sodium Extra, Fast IP Ping, No Telemetry. |
| Simple Opt Memory | Shares identical block-state tables to save memory; steps back for FerriteCore. | Block states with the same properties share their read-only property and face tables. Behaviour is unchanged. Steps back when FerriteCore is installed. |
| Simple Opt Server | Server: view distance follows server load; optional activation range. | Dedicated servers: the view distance drops when ticks get slow and recovers when the server is free, always within server.properties. Optional (off by default) activation range: far, idle mobs tick every 4th tick; timers of such mobs run slower, so only enable it knowingly. Vanilla clients can join. Steps back for ServerCore. |

### fabric.mod.json
| Mod | Befund |
|---|---|
| alle | `license` CC0-1.0, `authors` Ouzzi, `contact` (homepage, sources, issues) gesetzt, `icon` 16x16 (128x128 liegt daneben); `depends` Loader, MC `~26.3`, Java >=25, simplelib >=0.3.0 |
| simpleoptimization | `depends` auf die drei Sub-Mods `*` (Nesting per `include`), kein Mixin; Rueckfrage: Mod-Menu-Bildschirm fehlt (Einstellung nur JSON/Befehl) |
| simpleoptrender | `environment *` absichtlich (Super-Mod nestet es auch auf Servern); Beschreibung ergaenzt (O3/O9) |
| simpleoptmemory | `fabric-api` in `depends` ergaenzt |
| simpleoptserver | `fabric-api` vorhanden; Mixin-Config nicht mehr `required` |

## 6. Tests
Ein sbt-Lauf auf sb-test (Branch `b-rel32-opt`): Compile, Module-/Standalone-Gametests aller vier Mods (acht Targets), `check_client_mixins.py`, `check_lang_keys.py`, dazu der Client-Smoke `module-simpleoptrender-client-263`. Ergebnis siehe unten (wird nach dem Lauf eingetragen).

**Stand bei Abgabe: der sbt-Lauf (Log im Scratchpad der Sitzung, `sbt.log`) war noch nicht beendet; Ergebnisse C/T/M/L/K stehen aus und wurden NICHT geprueft. Vor dem Merge nachholen.**
