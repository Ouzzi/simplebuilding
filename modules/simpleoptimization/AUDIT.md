# Audit Simple Optimization (Super-Mod) - `simpleoptimization` (b-rel32-opt, 31.10.2026)

Basis b-rel32 d9cda1e21, Branch `b-rel32-opt`. Gelesen ohne Gradle: jede Datei vollstaendig, dazu `framework/.../OptAreas`, `OptCompat`, `OptSettings`, `ConfigFiles`, `SubMods`; MC-Klassen per `javap` gegen das 26.3-Jar (Unterschriften, Null-Pruefungen).

## Durchgesehene Dateien (Zeilen)
- `fabric/build.gradle` (13)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptimization/ModuleGameTest.java` (8)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptimization/OptimizationFabric.java` (12)
- `fabric/src/main/resources/fabric.mod.json` (35)
- `shared/java/com/simplebuilding/modules/simpleoptimization/OptimizationTests.java` (63)
- `shared/java/com/simplebuilding/modules/simpleoptimization/SimpleOptimization.java` (76)
- `shared/resources/assets/simpleoptimization/icon.layers.json` (82)
- `shared/resources/assets/simpleoptimization/icon.png` (200 B)
- `shared/resources/assets/simpleoptimization/icon_128.png` (530 B)
- `shared/resources/assets/simpleoptimization/lang/de_de.json` (9)
- `shared/resources/assets/simpleoptimization/lang/en_us.json` (9)
- `shared/resources/data/simpleoptimization/structure/empty.nbt` (1398 B)
- `tools/check_data.py` (21)
- `wiki/manual.json` (40)

## Ergebnis C1-C9

| Punkt | Ergebnis |
|---|---|
| C1 Code | ok. `SimpleOptimization` (Config-Datei lesen/atomar schreiben, kaputte Datei in Quarantaene, Schalter an `SubMods`), `OptimizationFabric` (Befehl `/simpleoptimization config`, `.restart(...)` fuer die drei Schalter). Kein Mixin, keine Registry, kein Netzwerk. Statische `config` ist nicht volatile, wird aber nur beim Start (Haupt-Thread) und im Befehl (Server-Thread) geschrieben. |
| C2 Mixins | keine (0). |
| C3 Config | ok: `enableSimpleOptRender/Memory/Server`, Default true; Lang EN/DE mit Tooltip und "Default: true." (Paritaet EN/DE geprueft). Aenderung braucht Neustart (Mixin-Bereiche entscheiden vor `onInitialize`; Befehl markiert `restart`). |
| C4 Daten | ok: keine Rezepte/Tags/Loot; Lang vollstaendig; Icon vorhanden; `structure/empty.nbt` nur fuer Gametests. |
| C5 Unabhaengigkeit | ok: haengt hart nur an den drei Sub-Mods (`depends`, Absicht laut Konzept 5.2) und simplelib; Sub-Mods genestet per `include`; die Sub-Mods laufen allein (eigene Targets `module-<mod>-standalone-fabric-263`). Ohne Super-Mod sind alle Schalter an (`SubMods`/`OptSettings.switchOn`). |
| C6 A12 | ok: kein Eingriff in Vanilla, nur Schalter. |
| C7 Netzwerk | n/a (kein Paket). |
| C8 Lizenz/Assets | ok - CC0 (LICENSE, `license` in fabric.mod.json), keine Mojang-Pixel (Icon 16x16/128x128 aus `icon.layers.json` erzeugt), keine Secrets/IPs/Pfade im Code oder in Ressourcen (nur `127.0.0.1`/`[::1]` in Tests). |
| C9 Offen | kein TODO/FIXME im Code. Offene Konzeptstufen e3/e5 siehe `docs/ai/release-qoa/opt.md` Abschnitt 4. |

## Befunde

keine P1/P2. P3-1 (Hinweis, kein Fix): Version 0.1.0 (Schema uneinheitlich, ST-14) - Release-Entscheidung, siehe opt.md. Kein Mod-Menu-Bildschirm; Einstellung nur ueber JSON/`/simpleoptimization config` (Rueckfrage an den Besitzer, ob vor Release ein Screen noetig ist).

## Urteil
**shipbar**
