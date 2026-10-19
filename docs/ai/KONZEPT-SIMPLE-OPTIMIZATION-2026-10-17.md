# Konzept: Simple Optimization (`simpleoptimization`), 17.10.2026

Nur Konzept, kein Code. Besitzer-Wunsch 17.10.: eigener, allein lauffähiger Super-Mod mit eigenen Fassungen gängiger
Optimierungen, jede einzeln schaltbar; voll verträglich mit bekannten Optimierungs-Mods (ist einer da, gewinnt standardmäßig
er und unser Teil schaltet sich ab); Vorrang je Bereich nach Messung; objektiver Benchmark. Grundlagen:
`docs/ai/PRINZIPIEN.md` (A1, A2, A9, A10), `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, Super-Mod-Muster wie
`simpleaesthetics`/`simplequalityoflife` (framework `SubMods`, ein Schalter je Sub-Mod), Modpack-Liste
`docs/ai/MODPACK-BESITZER-2026-10-17.md`.

## 0. Kurz für den Besitzer
- Simple Optimization bringt **eigene** Optimierungen (kein fremder Code). Jede lässt sich einzeln an/aus schalten.
- Ist ein bekannter Mod für denselben Bereich installiert (z. B. Cull Leaves), **gewinnt der fremde Mod**, unser Teil geht
  von selbst aus. Nichts geht kaputt. Erst wenn unsere Messung besser ist, schlagen wir unseren Teil vor; du kannst es je
  Bereich in der Config festlegen.
- Gemessen wird mit einem festen Ablauf (gleiche Welt, gleicher Kameraweg, mehrere Läufe). Der Testrechner hat keine
  Grafikkarte, deshalb zählen FPS-Zahlen nur von **deinem PC** – dafür gibt es ein Skript (§5.5).
- Start mit drei kleinen, sicheren Teilen: Blätter-Culling, Lichtkarten-Cache, Sichtweite nach Serverlast (§6).
- Große Dinge (Sodium-Renderer, Licht-Engine, Parallel-Chunks, LOD, Shader) bauen wir **nicht** nach – nur Verträglichkeit.

## 1. Lizenzregel (verbindlich)
- Repo ist CC0 (`LICENSE`). **Kein fremder Code**, auch nicht von MIT-Mods (MIT verlangt den Lizenzhinweis, passt nicht zu CC0;
  einheitliche Regel ist einfacher). GPL/LGPL/MPL/ARR/Polyform/tr7zw-Lizenz sowieso nicht.
- **Eigene Umsetzung veröffentlichter Techniken** (Projektbeschreibung, Wiki, README, Issues, Vorträge, allgemeine
  Grafik-/Spiele-Literatur). Agenten öffnen **nicht** den Quellcode der genannten Mods (Clean-Room); sie lesen nur
  Beschreibungen und den Vanilla-Code. Jede Teil-Datei nennt im Kopf die Quelle der Technik (Beschreibung, nicht Code).
- Keine Mixin-Ziele/Methodenauswahl aus fremden Mixin-Dateien abschreiben; eigene Analyse des Vanilla-Codes.

## 2. Recherche: Mods des Besitzers einzeln
Lizenz, Seite und 26.x-Stand per Modrinth-API am 17.10.2026 abgefragt (vor Umsetzung je Stufe erneut prüfen).
„26.3“ = Version für unsere Hauptlinie vorhanden. Seite: C = Client, S = Server.

| Mod (Version Besitzer) | Seite | Was genau / Technik (veröffentlicht) | Lizenz | 26.3 | Bereich |
|---|---|---|---|---|---|
| BadOptimizations 2.4.1 | C | Kleinoptimierungen außerhalb des Renderers: Lichtkarte nur neu rechnen, wenn sich Eingaben ändern; Himmels-/Nebelfarbe cachen; überflüssige Zustands- und Debug-Arbeit je Frame sparen | MIT | ja | O7 |
| Bobby 5.2.11 | C | Client-Chunk-Cache auf Platte: geladene Chunks werden gespeichert und über die Server-Sichtweite hinaus angezeigt | LGPL-3.0 | ja | O10 |
| C2ME 0.3.7 | S (C opt.) | Chunk-Generierung, -Laden, -Speichern parallel über mehrere Kerne; schnellere Serialisierung/IO | MIT | ja | S2 |
| Cull Leaves 4.1.1.1 | C | Innenflächen zwischen benachbarten Blättern werden nicht gezeichnet (Blätter verhalten sich zu Blättern wie Vollblöcke), optional mit Tiefe | MIT | ja | O1 |
| Entity Culling 1.9.5 | C | Asynchrones Raycasting durch Blockdaten: nicht sichtbare Entities und Block-Entities werden nicht gerendert/getickt | tr7zw Protective (eigen) | ja | O2 |
| FerriteCore 8.2.0 | C+S | Speicher: Blockzustands-Eigenschaften als kompakte Index-Tabellen statt je Zustand eigener Maps, Deduplizierung von Modell-Teilen/Multipart-Prädikaten, Nachbartabellen teilen, Strings/IDs internieren | MIT | ja | O4 |
| ImmediatelyFast 1.14.1 | C | Immediate-Mode-Zeichnen bündeln (HUD, Text, Items in GUIs, Karten, Schilder): weniger Draw-Calls/Pufferwechsel, Karten-Atlas | LGPL-3.0 | ja | O6 |
| Lithium 0.21.2 | C+S | Spiel-Logik: Trichter/Container-Zugriffe, Kollisionen, POI-Suche, Block-Entity-Schlaf, Chunk-Zugriffs-Caches, KI-Ziele, Zufall/Mathe; verhaltensneutral | LGPL-3.0 | ja | S1 |
| ModernFix-mVUS 5.25.2 | C+S | Startzeit/Speicher: Modelle bei Bedarf statt alle beim Start backen, Ressourcen-Neuladen beschleunigen, Duplikate sparen, diverse Fehlerbehebungen; Fork von ModernFix für mehr Versionen | LGPL-3.0 | ja | O5 |
| More Culling 1.6.1 | C | Mehr Culling: Blätter, Rahmen/Item-Frames, Schilder-Text, Leuchtfeuer-Strahl, Pulverschnee, Regen, Flächen gegen „Voll“-Modelle | GPL-3.0 | ja | O1, O3 |
| Motschen's Better Leaves 9.4 | C | **Optik**, kein Culling: buschigere Blätter über Modelle/Ressourcenpaket (kostet eher Leistung) | MIT | nein (26.2) | – (Kompat O1) |
| NoisiumForked 2.8.3 | S | Weltgen: schnellere Noise-Abtastung und Füllschleifen, Biom-Caches; Ergebnis seed-gleich | LGPL-3.0 | nein (26.2) | S3 |
| ScalableLux 0.1.6 | C+S | Licht-Engine auf Starlight-Basis: schnellere, parallele Lichtupdates | LGPL-3.0 | ja | S4 |
| ServerCore 1.5.15 | S | Server: dynamische Sicht-/Simulationsweite nach MSPT, Aktivierungsbereiche für Entities, Mob-Caps, Item-/XP-Zusammenfassen, Chunk-Tick-Grenzen, Befehle zur Laststeuerung | MIT | ja | S5 |
| Sodium 0.8.2 | C | Neuer Chunk-Renderer (eigene Meshes, Occlusion, Vertex-Format, Mehrkern-Mesh-Bau) | Polyform Shield | ja | O8 (nur Kompat) |
| Sodium Extra 0.8.3 | C | Zusatzschalter zu Sodium: Animationen, Partikel, Nebel, Wolken, FPS-Anzeige, Details aus | LGPL-3.0 | ja | O9 |
| Reese's Sodium Options 2.0.2 | C | Nur anderes Optionsmenü für Sodium | MIT | ja | – (UI) |
| Very Many Players 0.2.0 | S | Viele Spieler: Entity-Tracker und Spieler-Chunk-Verwaltung effizienter | MIT | ja | S6 |
| voxy 0.2.9 | C | LOD-Rendering (ferne Landschaft vereinfacht) | ARR | nein (26.2) | O11 (nur Kompat) |
| Iris 1.10.5 | C | Shader-Lader (OptiFine-Shaderpacks) auf Sodium | LGPL-3.0 | ja | O12 (Kompat-Ziel) |
| Fast IP Ping 1.0.8 | C | Server-Liste: keine Rückwärts-DNS-Abfrage bei reinen IP-Adressen | LGPL-3.0 | ja | M1 |
| No Telemetry 1.10.0 | C | Telemetrie abschalten | MPL-2.0 | ja | M2 |
| Low On Fire 1.5 | C | **Optik**: Feuer-Overlay kleiner | ARR | ja | – (Simple Visuals-Kandidat) |
| Smooth Gui 1.1.0 | C | **UI**: Menüs öffnen mit Animation | MIT | ja | – (Simple Visuals-Kandidat) |
| Speed FOV Limiter 1.1.0 | C | **UI**: Grenzen, wie stark Tempo/Langsamkeit das Sichtfeld ändern | MIT | ja | – (Simple Visuals-Kandidat) |
| Chunky 1.4.55 | S (C opt.) | Chunk-Vorgenerierung per Befehl | GPL-3.0 | ja | S7 = Simple Tweaks T33 |

Ausgeschlossen: Konfig/Bibliothek – Cloth Config, YACL, MidnightLib, Forge Config API Port, Config Manager, Mod Menu,
Controlling, Balm, Architectury, Collective, Puzzles Lib, MaLiLib, ukulib, TCDCommons, Text Placeholder API, Fabric API,
Fabric Language Kotlin.

## 3. Bereiche (was wir bauen)
Aufwand S/M/L. Gewinn = Erwartung aus den Projektbeschreibungen, **unbestätigt bis zur eigenen Messung**. Priorität
„unsere“: 1 = zuerst bauen, 2 = später, 3 = nur wenn Messung es rechtfertigt, – = nie (nur Kompat).

| ID | Bereich | Fremd-Mod beim Besitzer | Technik (eigene Umsetzung) | Gewinn | Risiko | Aufwand | Prio |
|---|---|---|---|---|---|---|---|
| O1 | Blätter-Culling | Cull Leaves, More Culling | Fläche Blatt↔Blatt nicht zeichnen (eigene Occlusion-Regel für `#minecraft:leaves`, auch unsere End-Blätter); Option „Tiefe 1“ (äußerste Schicht bleibt fein) | hoch in Wäldern (viele Flächen weniger) | gering (nur Optik innen) | S | 1 |
| O7 | Lichtkarten-/Himmels-Cache | BadOptimizations | Lichtkarte nur neu berechnen, wenn Tageszeit/Gamma/Effekt/Dimension sich ändern; Himmels-/Nebelfarbe je Frame cachen | mittel, CPU je Frame | gering | S | 1 |
| S5a | Sichtweite nach Serverlast | ServerCore | Sicht- und Simulationsweite schrittweise senken, wenn MSPT > Grenze, wieder anheben, wenn frei; Grenzen aus `server.properties`, serverautoritär | hoch auf vollen Servern | gering (nur Weite) | S–M | 1 |
| M1 | Schneller IP-Ping | Fast IP Ping | bei Literal-IP keine Rückwärts-DNS-Auflösung | klein (Serverliste) | sehr gering | S | 1 (Kleinkram) |
| M2 | Telemetrie aus | No Telemetry | Telemetrie-Sender nicht starten | Datenschutz, minimal Leistung | sehr gering | S | 1 (Kleinkram) |
| O3 | Weiteres Culling | More Culling | Item-Frames/Schilder-Text nur in Nähe/Sicht, Leuchtfeuer-Strahl außerhalb Sicht, Regen hinter Decken | mittel (Basen, Dörfer) | gering–mittel | M | 2 |
| O4 | Speicher Blockzustände | FerriteCore | Eigenschaften-Tabellen kompakt teilen, Modell-Teile deduplizieren | hoch (Heap, große Packs) | mittel (Kern-Mixins) | M | 2 |
| S1a | Familien-BE-Schlaf | (Lithium für Vanilla) | **unsere** Block-Entities (Trichter-Stufen, Öfen, Tiegel, Lager) ticken nur bei Änderung; kein Vanilla-Eingriff | mittel (SB-Basen) | gering (eigener Code) | M | 2 |
| S5b | Aktivierungsbereich Entities | ServerCore | ferne Mobs seltener ticken; **verändert Spiel** → Standard aus | hoch (Farmen) | mittel (Verhalten) | M | 2 |
| O9 | Detail-Schalter | Sodium Extra | Schalter für Animationen/Partikel/Nebel ohne Sodium-Abhängigkeit | klein–mittel | gering | S | 2 |
| O2 | Entity-/BE-Occlusion | Entity Culling | eigener asynchroner Sichtstrahl über Sektions-Belegung, unsichtbare (B)Es nicht rendern | hoch (viele BEs, unsere BE-Renderer) | mittel (Flackern) | L | 3 |
| O6 | Zeichnen bündeln | ImmediatelyFast | HUD/Text/GUI-Items in gemeinsamen Puffern zeichnen | mittel | mittel (Reihenfolge) | L | 3 |
| O5 | Startzeit/Modelle | ModernFix | Modelle bei Bedarf backen, Ressourcen-Reload | hoch (Startzeit) | hoch (viele Mods) | L | 3 |
| S1b | Vanilla-Spiellogik | Lithium | einzelne verhaltensneutrale Pfade (Trichter, Kollision, POI) | mittel–hoch | hoch (Vanilla-Gleichheit) | L | 3 |
| S3 | Weltgen-Noise | NoisiumForked | schnellere Noise-Schleifen, **seed-gleich** (Pflichttest: Chunk-Hash gleich Vanilla) | mittel (Pregen) | mittel | M–L | 3 |
| S6 | Viele Spieler | Very Many Players | Entity-Tracker effizienter | nur große Server | mittel | L | 3 |
| O10 | Client-Chunk-Cache | Bobby | Chunks lokal speichern, über Server-Sichtweite zeigen | Optik/Komfort | mittel (Platte) | L | 3 |
| O8 | Chunk-Renderer | Sodium | – | – | – | – | – (nur Kompat) |
| S2 | Parallele Chunks | C2ME | – | – | – | – | – (nur Kompat) |
| S4 | Licht-Engine | ScalableLux | – | – | – | – | – (nur Kompat) |
| O11 | LOD | voxy | – | – | – | – | – (nur Kompat) |
| O12 | Shader | Iris | – | – | – | – | – (Kompat-Ziel) |
| S7 | Vorgenerierung | Chunky | liegt in Simple Tweaks T33 `/pregen` | – | – | – | – (Kompat K5) |

Zusatz **F0 Familien-Zuschlag** (Queue „ZULETZT … alle Mods optimieren“): derselbe Benchmark misst „Vanilla“ gegen
„Vanilla + alle Simple-Mods“; Ausreißer unserer Mods werden dort gefunden und im jeweiligen Mod behoben (nicht in Optimization).

## 4. Verträglichkeit (Mechanismus)
1. **Bereichs-Tabelle im Code** (`Area`): ID, Seite (C/S), Mixin-Paket, Liste der Fremd-Mod-IDs, die denselben Bereich
   abdecken (aus §2, z. B. O1: `cullleaves`, `moreculling`; O7: `badoptimizations`; S5a: `servercore`; O4: `ferritecore`;
   M1: `fastipping`; M2: `no-telemetry`). Mod-IDs vor Umsetzung aus den Jars bestätigen.
2. **Früh entscheiden, bevor Klassen laden:** je Bereich eine eigene Mixin-Config mit `IMixinConfigPlugin`; `shouldApplyMixin`
   liefert `false`, wenn der Bereich aus ist. Mod-Erkennung im Plugin über den Loader (Fabric `FabricLoader.isModLoaded`,
   NeoForge `LoadingModList`, Forge `FMLLoader.getLoadingModList`), gekapselt in einem kleinen Loader-Adapter
   (Prinzip A1: kein `Class.forName` auf fremde Klassen).
3. **Entscheidungsregel je Bereich** (`mode` in der Config): `auto` (Standard) → Prioritätstabelle; `ours` → unser Teil an,
   auch wenn fremder da ist (nur erlaubt, wenn sich beide nicht im selben Ziel treffen oder der fremde Teil per eigener Config
   aus ist – sonst Warnung und Rückfall auf `theirs`); `theirs` → unserer aus, sobald fremder da; `off` → immer aus.
   „Unsere besser“ heißt in `auto`: Hinweis im Status „fremden Teil X abschalten, unserer ist schneller“ – wir schalten
   **nie** fremde Mods ab.
4. **Konfliktarm schreiben:** nur `@Inject`/`@WrapOperation`/`@ModifyExpressionValue` (MixinExtras, in Fabric enthalten),
   kein `@Overwrite`, kein `@Redirect` auf heißen Pfaden; jede Injektion mit `require = 1` **im Dev**, im Spiel fängt der
   Bereich einen Fehlschlag ab und schaltet sich mit Log-Zeile ab (nie Absturz).
5. **Zielprüfung (Stufe 2):** beim Start alle geladenen Mixin-Configs auf gleiche Zielklassen prüfen (Mixin-API
   `Mixins.getConfigs()`/`IMixinConfig.getTargets()`, Verfügbarkeit prüfen); Treffer eines unbekannten Mods → Bereich auf
   `theirs` + Log „möglicher Konflikt mit X“. So decken wir auch Mods ab, die nicht in der Liste stehen.
6. **Status sichtbar:** `/simpleoptimization status` (Server: Server-Bereiche; Client: Client-Befehl) und Config-Screen
   (Mod Menu/Cloth Config) zeigen je Bereich „aktiv“ / „aus: Cull Leaves aktiv“ / „aus: Config“. Mixin-Bereiche: Änderung
   braucht Neustart (Hinweis im Screen).
7. **Tests:** Gametest je Bereich (an/aus, Ergebnis gleich Vanilla bei verhaltensneutralen Teilen), Kompat-Ziel
   `compat-opt-263` lädt die Fremd-Mods aus §2 (nur 26.3-Fassungen) zusammen mit unserem Mod: Start ohne Fehler, Status
   zeigt erwartete Abschaltungen.

## 5. Config
`config/simpleoptimization.json` (Super-Mod: Schalter je Sub-Mod wie `simpleaesthetics`), je Sub-Mod eigene Datei:
```json
{ "areas": {
    "leaf_culling":     { "mode": "auto", "depth": 0 },
    "lightmap_cache":   { "mode": "auto" },
    "dynamic_distance": { "mode": "auto", "targetMspt": 45, "minView": 6, "minSimulation": 5, "stepSeconds": 15 },
    "fast_ip_ping":     { "mode": "auto" },
    "no_telemetry":     { "mode": "auto" } },
  "conflictScan": true }
```
Server-Werte serverautoritär mit Obergrenzen (A9): Weiten nie über `server.properties`, nie unter 3; verhaltensändernde
Bereiche (S5b) Standard `off`.

### 5.1 Prioritätstabelle „unsere vs. fremde“
Datei im Jar `data/simpleoptimization/priorities.json`, je Bereich: `winner` (`theirs`|`ours`), `vs` (Fremd-Mod + Version),
`measured` (Datum, Rechner-Klasse), `delta` (Median-Unterschied je Kennzahl), `runs`. Startwert überall `theirs`
(etablierter Mod bevorzugt). Umschalten auf `ours` nur, wenn auf dem **Besitzer-PC** (Client-Bereiche) bzw. sb-test
(Server-Bereiche: MSPT/Heap/Start) unser Teil **≥ 3 % besser im Median** und **nicht schlechter** bei 1%-Low und Heap ist,
in **2 getrennten Messreihen**. Die Datei wird nur per `--apply-priorities` aus Ergebnis-JSON erzeugt und committet.
Config `mode` schlägt die Tabelle immer.

### 5.2 Struktur
Super-Mod `simpleoptimization` ohne eigenen Inhalt (Muster `simpleaesthetics`), bündelt drei Sub-Mods, jedes allein lauffähig:
`simpleoptrender` (Client: O1, O3, O7, O9, später O2/O6, M1, M2), `simpleoptmemory` (C+S: O4, später O5),
`simpleoptserver` (Server: S5a, S5b, S1a, später S3/S6). Vorteil: Server installieren nur `simpleoptserver`.
Hartes Laden nur von Fabric API (+ Cloth Config für den Screen, optional). Kein Inhalt, keine Registry-Einträge
(Welten bleiben ohne den Mod ladbar). Fabric 26.3 zuerst; NeoForge/Forge erst auf Anforderung (Mixin-Plugin dort anders).

## 6. Benchmark (Methode + Harness)
### 6.1 Szenarien je Bereich
(a) Vanilla (+ Fabric API + Mess-Mod), (b) fremder Mod allein, (c) unser Mod mit **nur** dem vergleichbaren Bereich an,
(d) beide (`auto` → zeigt Abschaltung; zusätzlich `ours` erzwungen, wo erlaubt). Gesamtvergleich: „Besitzer-Stack“ =
Sodium + Lithium + FerriteCore + ModernFix + ImmediatelyFast + Entity Culling + More Culling/Cull Leaves + C2ME +
ScalableLux + BadOptimizations + Noisium + ServerCore, gegen „unser Mod alle Bereiche“, gegen „beide“, gegen Vanilla.
Auf 26.3 fehlen Noisium (letzte 26.2) – dann ohne, im Bericht vermerkt. Fremd-Mods per Modrinth-API mit fester Versions-ID
und SHA-512 (`tools/bench/modsets.json`), nie eingecheckt.

### 6.2 Feste Bedingungen
Fester Seed und gespeicherte Bench-Welt (`testing/worlds/bench-263`, einmal erzeugt + Radius vorgeneriert), Uhrzeit Mittag,
`doDaylightCycle`/`doWeatherCycle`/`doMobSpawning` aus, feste Szene mit 200 Entities und 300 Block-Entities (Truhen,
Item-Frames, Schilder, Öfen, unsere BE-Renderer); feste `options.txt` (1920x1080, Sichtweite 16, Simulation 12,
VSync aus, FPS unbegrenzt, Grafik „Schön“, GUI 2), gleiche JVM-Argumente (`-Xmx4G`, G1). Kameraweg aus Keyframes
(Position/Blick, 10 Blöcke/s): K1 Wald (Blätter), K2 Dorf/Farm (Entities), K3 Lagerraum (BEs, Frames, Text), K4 gerader
Flug 600 Blöcke (Chunk-Laden), K5 Inventar/HUD offen (Text-Bündeln). Server-Szene: S-Pregen Radius 25 Chunks (Weltgen),
S-Farm 60 s Ticks.

### 6.3 Ablauf und Kennzahlen
Jeder Lauf frische JVM; Warmup 30 s (verworfen), Messung 60 s je Szene; N = 5 (Besitzer-PC) bzw. 3 (sb-test).
Ein Dev-Mess-Mod (nur im Harness, nicht ausgeliefert) misst:
- **Frame-Zeit** je Frame (ns) → Mittel-FPS, Median, **1%-Low** (Mittel der langsamsten 1 % Frames), p99;
- **MSPT** des integrierten/dedizierten Servers (Mittel, p95);
- **Heap nach GC** an festen Punkten (nach Welt-Laden und nach K3: `System.gc()` ×2, 2 s warten, `used`);
- **Startzeit**: JVM-Start (`RuntimeMXBean.getStartTime`) bis Titelbild und bis Welt spielbar;
- **Chunk-Ladezeit**: K4 bis alle Sektionen im Radius kompiliert (Vanilla-Abfrage; mit Sodium dessen Zähler, sonst nur
  Server-Seite: Zeit bis Chunks `FULL`).
Auswertung: Median je Kennzahl über N, Streuung (Std.-Abw.), Unterschied gilt nur, wenn > 2× Streuung **und** > 3 %.

### 6.4 Ausgabe
`bench-results/<datum>-<rechner>/results.json` (Schema: `machine` {os, cpu, gpu, ram, java}, `mc`, `scenario`, `area`,
`mods` [id, version, sha], `runs` [{fps_avg, fps_1low, frame_p99_ms, mspt_avg, mspt_p95, heap_mb, startup_s, chunkload_s}],
`summary`) + `report.html` (eine Seite, Tabelle je Bereich, Gewinner markiert, Balken je Kennzahl, hell/dunkel).

### 6.5 sb-test und Software-GL (llvmpipe)
sb-test hat keine Grafikkarte: Mesa **llvmpipe** rastert auf der CPU. Folgen: FPS sind absolut bedeutungslos (oft
einstellig), GPU-Ersparnisse (weniger Flächen/Draw-Calls) wirken übertrieben oder verschwinden, und die CPU wird mit dem
Rendering geteilt (Gradle-Läufe stören zusätzlich). **Gültig auf sb-test:** MSPT, Heap, Startzeit, Server-Chunk-Zeiten,
Absturz-/Konflikt-Freiheit, Plausibilität der Szenarien. **FPS/1%-Low entscheidet nur der Besitzer-PC.**
Befehl auf sb-test (über `/root/sbt <branch> "..."`, Job-Sperre, nie parallel zu Gates):
`LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s '-screen 0 1920x1080x24 +extension GLX +render -noreset' python3.12 tools/bench/run_bench.py --profile sbtest --runs 3 --scenarios server,startup,smoke`

### 6.6 Besitzer-PC (Windows)
Einmalig: Repo-Checkout + JDK 25 (wie für den Build). Dann im Repo-Ordner:
`py -3 tools\bench\run_bench.py --profile pc --runs 5 --scenarios owner-stack,ours-all,both,vanilla` (ganze Matrix je Bereich: `--scenarios areas`).
Das Skript lädt die Fremd-Mods (fest gepinnt), startet je Lauf einen frischen Client mit eigener Spielordner-Kopie
(berührt die normale Installation nicht), fährt den Kameraweg allein ab (Maus/Tastatur nicht anfassen, keine anderen
Programme), schreibt `results.json` + `report.html` und öffnet den Bericht. Dauer Zielwert ≈ 45 min (Stack-Vergleich) bzw.
≈ 2 h (alle Bereiche). Ergebnisordner an uns geben → `--apply-priorities`.
Hinweis: 26.x ist nicht mehr obfuskiert; fertige Mod-Jars sollten im Dev-Lauf ohne Remapping laden (Stufe 1 prüft das,
sonst Start über eine Launcher-Instanz mit gleicher Ordnerstruktur).

## 7. Stufenplan
| Stufe | Branch (Vorschlag) | Inhalt | Prüfung |
|---|---|---|---|
| 0 | `claude-optconcept` | dieses Konzept + Modpack-Liste | Besitzer-Review |
| 1 | `claude-opt-s1` | Gerüst: Super-Mod + 3 Sub-Mods (`modules.json`, Fabric 26.3), Bereichs-Tabelle, Mixin-Plugin, Loader-Erkennung, Config, Status-Befehl; Harness `tools/bench/` (Mess-Mod, Welt, Kameraweg, JSON/HTML), Modsets | Compile, Gametests an/aus, Standalone-Targets, Harness-Smoke auf sb-test |
| 2 | `claude-opt-s2` | **O1 Blätter-Culling, O7 Lichtkarten-Cache, S5a Sichtweite nach Last** + M1/M2 Kleinkram | Gametests, Kompat-Ziel mit Cull Leaves/More Culling/BadOptimizations/ServerCore, Bench sb-test (S5a), Bericht |
| 3 | – (Besitzer) | Benchmark auf Besitzer-PC (Stack + Bereiche O1/O7), `priorities.json` erstmals füllen | Besitzer-Bericht |
| 4 | `claude-opt-s4` | O3, O4, S1a, O9, S5b (Standard aus) + Zielprüfung §4.5 | wie 2 |
| 5 | je Bereich | O2, O6, O5, S1b, S3, S6, O10 nur, wenn Messung aus 3/4 Bedarf zeigt | wie 2 + Vanilla-Gleichheit (S3 Chunk-Hash) |
| K | `claude-opt-compat` | Kompatibilitätstest Modpack des Besitzers (`MODPACK-BESITZER-2026-10-17.md` §6, K1–K23) | Bericht je K-Punkt |

## 8. Offene Fragen an den Besitzer (mit Empfehlung)
1. **Struktur:** Super-Mod + 3 Sub-Mods (Render/Speicher/Server) oder ein Mod mit Bereichen? *Empfehlung: Super-Mod + 3 Sub-Mods (Server braucht nur `simpleoptserver`).*
2. **Loader:** nur Fabric 26.3, bis du NeoForge/Forge anforderst? *Empfehlung: ja (Ports erst auf Anforderung).*
3. **Erste Teile:** O1 Blätter-Culling, O7 Lichtkarten-Cache, S5a Sichtweite nach Last (+ IP-Ping, Telemetrie)? *Empfehlung: ja.*
4. **Spiel verändernde Server-Teile** (Aktivierungsbereich, Mob-Caps): Standard aus? *Empfehlung: aus, nur per Config.*
5. **Schwelle „unsere besser“:** ≥ 3 % Median, nicht schlechter bei 1%-Low/Heap, 2 Messreihen? *Empfehlung: ja.*
6. **Besitzer-PC:** Windows + welche GPU/CPU? Darf das Skript Mods von Modrinth laden und ≈ 45 min laufen? *Empfehlung: ja, einmal je Stufe.*
7. **Version:** Messen auf 26.3 (unsere Linie), dein Pack ist 1.21.11 – einige Mods fehlen auf 26.3 (Noisium, voxy, Better Leaves). *Empfehlung: 26.3; 1.21.11 erst im Port-Run.*
8. **Kleinkram-Bereiche** (IP-Ping, Telemetrie) hier oder in Simple Tweaks? Optik-Wünsche (Low On Fire, Smooth Gui, FOV-Grenze) nach Simple Visuals? *Empfehlung: Kleinkram hier, Optik nach Simple Visuals (eigener Queue-Punkt, nur auf Wunsch).*


## 9. Besitzer-Antworten (17.10.2026)
Alle Empfehlungen aus §8 übernommen (Super-Mod + 3 Sub-Mods; erst Fabric 26.3; Start O1/O7/S5a + IP-Ping/Telemetrie; spielverändernde Teile Standard aus; Schwelle ≥3 % Median, 1%-Low/Heap nicht schlechter, 2 Messreihen; Messung auf 26.3; Kleinkram hier, Optik nach Simple Visuals nur auf Wunsch).
**Abweichung zu Frage 6:** Benchmarks laufen auf dem Server (sb-test), nicht am Besitzer-PC (Besitzer nicht immer verfügbar). Folge: CPU-/Server-Kennzahlen (MSPT, Heap, Startzeit, Chunk-Laden, Frame-Zeit unter Software-GL) sind aussagekräftig; reine GPU-Gewinne nur grob (llvmpipe) – im Bericht je Bereich als „GPU-gebunden, nur Tendenz“ markieren. Das PC-Skript bleibt optional.

## 10. Stand Stufe 4 (`b-opt45`, 19.10.2026)
Plan `docs/ai/PLAN-B-OPT45-2026-10-19.md`. Umgesetzt (Fabric 26.3, framework 0.1.13):
- **Zielprüfung §4.5**: framework `OptAreas` + Plugin-`acceptTargets` in allen drei Sub-Mods; Treffer → Bereich zur Laufzeit
  aus (Grund `conflict`), Familie/Fabric API ignoriert, `mode: ours` behält, `conflictScan: false` überspringt.
- **O3** `more_culling` (Schild-Rückseite, Rahmen hinter fester Wand, Leuchtfeuer-Strahl außer Sicht), **O9** `details`
  (Partikel, Textur-Animationen, Luft-Nebel; Rivale `sodium-extra`), **O4** `state_tables` (geteilte Werte-/Schlüssel-Arrays
  und Seiten-Tabellen; Mojang hat FerriteCores Kern in 26.3 schon übernommen), **S5b** `activation_range` (Standard `off`).
- **Offen:** S1a (Audit: Leerlauf der Familien-BEs schon billig; teuer nur Trichter-Saugen/Pads, Schlaf bräuchte
  Item-Entity-Signal = Vanilla-Eingriff), O3 Regen hinter Decken, O4 Modell-Deduplizierung (Client), Messung O3/O4/O9/S5b
  (Bench-Fall `o4` angelegt; Bench läuft als Client und wurde nicht gefahren).
