# Audit Simple Opt Server (Server) - `simpleoptserver` (b-rel32-opt, 31.10.2026)

Basis b-rel32 d9cda1e21, Branch `b-rel32-opt`. Gelesen ohne Gradle: jede Datei vollstaendig, dazu `framework/.../OptAreas`, `OptCompat`, `OptSettings`, `ConfigFiles`, `SubMods`; MC-Klassen per `javap` gegen das 26.3-Jar (Unterschriften, Null-Pruefungen).

## Durchgesehene Dateien (Zeilen)
- `fabric/build.gradle` (7)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptserver/ModuleGameTest.java` (13)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptserver/fabric/OptTargetScan.java` (48)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptserver/fabric/ServerOptFabric.java` (23)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptserver/fabric/ServerOptMixinPlugin.java` (27)
- `fabric/src/main/resources/fabric.mod.json` (36)
- `shared/java/com/simplebuilding/modules/simpleoptserver/ActivationRange.java` (50)
- `shared/java/com/simplebuilding/modules/simpleoptserver/DynamicDistance.java` (67)
- `shared/java/com/simplebuilding/modules/simpleoptserver/ServerOpt.java` (105)
- `shared/java/com/simplebuilding/modules/simpleoptserver/ServerOptRuntime.java` (73)
- `shared/java/com/simplebuilding/modules/simpleoptserver/ServerOptTests.java` (212)
- `shared/java/com/simplebuilding/modules/simpleoptserver/mixin/activation_range/ServerLevelMixin.java` (20)
- `shared/resources/assets/simpleoptserver/icon.layers.json` (118)
- `shared/resources/assets/simpleoptserver/icon.png` (160 B)
- `shared/resources/assets/simpleoptserver/icon_128.png` (506 B)
- `shared/resources/assets/simpleoptserver/lang/de_de.json` (7)
- `shared/resources/assets/simpleoptserver/lang/en_us.json` (7)
- `shared/resources/data/simpleoptserver/priorities.json` (7)
- `shared/resources/data/simpleoptserver/structure/empty.nbt` (1398 B)
- `shared/resources/simpleoptserver.mixins.json` (15)
- `tools/check_data.py` (21)
- `wiki/manual.json` (59)

## Ergebnis C1-C9

| Punkt | Ergebnis |
|---|---|
| C1 Code | ok. `DynamicDistance` (reine Regel, Grenzen: nie ueber `server.properties`, nie unter 3, Simulation nur mit `adjustSimulation`), `ServerOptRuntime` (Controller nur auf Dedicated Server, nur bei Spielern, gibt Weiten beim Abschalten zur Laufzeit zurueck), `ActivationRange` (Mob-Ausnahmen: Fahrzeug, angeleint, Ziel, verletzt, in der Luft, Wasser/Lava, brennend, Baby, Effekte, Liebe/Brutzeit, Huehner, gezaehmt, Dorfbewohner/Haendler, Wither/Drache). Alles auf dem Server-Thread. Kein Client-Klassenzugriff. |
| C2 Mixins | ein Mixin: `ServerLevelMixin` (`@WrapOperation` auf `Entity.tick()` in `tickNonPassenger`), nur angewendet wenn `activation_range` aktiv (Standard `mode off`, also im Auslieferungszustand nicht geladen). Konfliktrisiko: ServerCore (Rivale), Lithium/andere `ServerLevel`-Hooker -> Zielpruefung stellt den Bereich ab (klassenweise, daher konservativ). **Fix:** `required:false`/`defaultRequire:0` (vorher true/1). Der Wrap umgeht `tick()` nur, Vanilla-Gemeinsames (`tickCount++`, Positionsalt) laeuft weiter. |
| C3 Config | ok: `config/simpleoptserver.json`: `dynamic_distance` (targetMspt 1..1000, minView 3..32, minSimulation, stepSeconds 1..3600, adjustSimulation) und `activation_range` (mode off, range 16..128, interval 2..20), alles geklemmt. `/simpleoptserver config` (alle Optionen `restart`). Lang EN/DE. |
| C4 Daten | ok: keine Rezepte/Tags/Loot, Lang EN/DE gleich, Statuszeilen mit `translatableWithFallback` (Vanilla-Client sieht Klartext). |
| C5 Unabhaengigkeit | ok: simplelib + Fabric API (`depends` vorhanden); reiner Server-Mod, Vanilla-Clients koennen beitreten (kein Paket, kein Registry-Eintrag). |
| C6 A12 | S5a: nur Sichtweite (Simulation standardmaessig unangetastet). S5b veraendert Spielverhalten (Wachstums-/Umwandlungs-/Despawn-Timer ferner Mobs laufen langsamer) -> Standard aus, bewusst. |
| C7 Netzwerk | `/simpleopt status` nur fuer Gamemaster (Level 2). Kein Paket, der Client kann nichts steuern. |
| C8 Lizenz/Assets | ok - CC0 (LICENSE, `license` in fabric.mod.json), keine Mojang-Pixel (Icon 16x16/128x128 aus `icon.layers.json` erzeugt), keine Secrets/IPs/Pfade im Code oder in Ressourcen (nur `127.0.0.1`/`[::1]` in Tests). |
| C9 Offen | kein TODO/FIXME. S1a/S3/S6 laut Konzept nicht gebaut (S1a begruendet kein Bereich). |

## Befunde

- simpleoptserver-P2-1: Mixin-Config `required:true`/`defaultRequire:1`. Fix: Commit 64e80c0cc.
- simpleoptserver-P3-1: `OptSettings.load` nicht atomar. Fix: Commit 2e29b17fb.
- simpleoptserver-P3-2 (Hinweis, S5b Standard aus): bei aktivem S5b laufen Timer ferner Mobs (z. B. Piglin-/Dorfbewohner-Umwandlung, Despawn-Wuerfeln, Sonnenbrand) im Verhaeltnis 1:interval langsamer; Mob-Cap-Auslastung wirkt leicht traeger. Als Warnung in der Beschreibung (Modrinth-Text) aufnehmen.
- simpleoptserver-P3-3 (Hinweis): `adjustSimulation`/S5a gelten nur dediziert, nie auf dem integrierten Server (Absicht, steht im Manual).
Keine P1.

## Urteil
**shipbar**
