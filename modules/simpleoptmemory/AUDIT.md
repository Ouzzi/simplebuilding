# Audit Simple Opt Memory (Client + Server) - `simpleoptmemory` (b-rel32-opt, 31.10.2026)

Basis b-rel32 d9cda1e21, Branch `b-rel32-opt`. Gelesen ohne Gradle: jede Datei vollstaendig, dazu `framework/.../OptAreas`, `OptCompat`, `OptSettings`, `ConfigFiles`, `SubMods`; MC-Klassen per `javap` gegen das 26.3-Jar (Unterschriften, Null-Pruefungen).

## Durchgesehene Dateien (Zeilen)
- `fabric/build.gradle` (7)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptmemory/ModuleGameTest.java` (10)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptmemory/fabric/MemoryOptFabric.java` (19)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptmemory/fabric/MemoryOptMixinPlugin.java` (27)
- `fabric/src/main/java/com/simplebuilding/modules/simpleoptmemory/fabric/OptTargetScan.java` (48)
- `fabric/src/main/resources/fabric.mod.json` (36)
- `shared/java/com/simplebuilding/modules/simpleoptmemory/MemoryOpt.java` (66)
- `shared/java/com/simplebuilding/modules/simpleoptmemory/MemoryOptTests.java` (105)
- `shared/java/com/simplebuilding/modules/simpleoptmemory/StateTables.java` (100)
- `shared/java/com/simplebuilding/modules/simpleoptmemory/mixin/state_tables/BlockStateCacheMixin.java` (23)
- `shared/java/com/simplebuilding/modules/simpleoptmemory/mixin/state_tables/StateHolderMixin.java` (27)
- `shared/resources/assets/simpleoptmemory/icon.layers.json` (118)
- `shared/resources/assets/simpleoptmemory/icon.png` (180 B)
- `shared/resources/assets/simpleoptmemory/icon_128.png` (497 B)
- `shared/resources/assets/simpleoptmemory/lang/de_de.json` (4)
- `shared/resources/assets/simpleoptmemory/lang/en_us.json` (4)
- `shared/resources/data/simpleoptmemory/priorities.json` (6)
- `shared/resources/data/simpleoptmemory/structure/empty.nbt` (1398 B)
- `shared/resources/simpleoptmemory.mixins.json` (16)
- `tools/check_data.py` (21)
- `wiki/manual.json` (58)

## Ergebnis C1-C9

| Punkt | Ergebnis |
|---|---|
| C1 Code | ok. `StateTables`: `ConcurrentHashMap`, Schluessel nach Array-Klasse + Element-Identitaet (nie `equals` fremder Werte), Boolean-Tabellen nach Inhalt; Arrays werden nur gelesen. `release()` bei `SERVER_STARTING` leert die Suchtabellen (nur kanonische Arrays, wenige MB). Unter 26.3 Mojang-Aenderung bereits: Nachbar-Arrays teilt Vanilla selbst, uebrig bleiben Werte-, Schluessel- und Face-Tabellen. Thread-sicher (Registrierung ist ohnehin einthreadig). |
| C2 Mixins | `StateHolderMixin` (`<init>` TAIL, ersetzt `propertyKeys/propertyValues` per `@Shadow @Mutable`) und `BlockStateCacheMixin` (`Cache.<init>` TAIL, `faceSturdy`). Konfliktrisiko: FerriteCore (`ferritecore` in der Rivalenliste, ours wird dann nicht angewendet); ModernFix hakt andere Stellen; Lithium hakt `BlockStateBase` teilweise - Zielpruefung warnt. Beide Mixins pruefen `MemoryOpt.live`. **Fix:** `required:false`/`defaultRequire:0` (vorher true/1: ein Feld-Rename in einem 26.3.x-Patch haette das Spiel beim Start beendet, Konzept 4.4 verlangt Abschalten statt Absturz). Gametest `sharedStates` weist das Teilen live nach. |
| C3 Config | ok: `config/simpleoptmemory.json` (Bereich `state_tables`, `mode`, `conflictScan`), Befehle `/simpleoptmemory config` (Server) und `/simpleclient simpleoptmemory`. Lang EN/DE. Aenderung braucht Neustart. |
| C4 Daten | ok: keine Rezepte/Tags/Loot, Lang EN/DE gleich, Icons. |
| C5 Unabhaengigkeit | ok: simplelib + Fabric API; **Fix:** `fabric-api` jetzt direkt in `depends` (nutzt `ServerLifecycleEvents`, war nur ueber simplelib transitiv). Rivalenerkennung nur per `isModLoaded`. |
| C6 A12 | Eingriff: tief (Block-State-Konstruktoren), aber Verhalten unveraendert (nur gleiche, nie geschriebene Arrays werden geteilt); Bereich abschaltbar (`mode off`), weicht FerriteCore. |
| C7 Netzwerk | n/a. Laeuft identisch auf Client und Dedicated Server (Seite BOTH). |
| C8 Lizenz/Assets | ok - CC0 (LICENSE, `license` in fabric.mod.json), keine Mojang-Pixel (Icon 16x16/128x128 aus `icon.layers.json` erzeugt), keine Secrets/IPs/Pfade im Code oder in Ressourcen (nur `127.0.0.1`/`[::1]` in Tests). |
| C9 Offen | kein TODO/FIXME. O5 (Modell-Backen) laut Konzept Stufe 5 nicht gebaut, `modernfix` wird nur beobachtet (Log). |

## Befunde

- simpleoptmemory-P2-1: Mixin-Config `required:true`/`defaultRequire:1` -> Absturz statt Abschalten bei fehlgeschlagener Injection. Fix: Commit 64e80c0cc (`required:false`, `defaultRequire:0`). Hinweis: Memory/Server haben (anders als Render) keinen `MixinGuard`; ein stilles Nicht-Anwenden zeigt sich im Log nur durch `O4 state tables: 0 of 0 arrays shared` und durch den Gametest `sharedStates`.
- simpleoptmemory-P3-1: `OptSettings.load` nicht atomar. Fix: Commit 2e29b17fb.
- simpleoptmemory-P3-2: `fabric-api` fehlte in `depends`. Fix: Commit 64e80c0cc.
- simpleoptmemory-P3-3 (Hinweis): Der behauptete Speichergewinn (`savedBytes`) ist eine Schaetzung (Array-Kopf + Referenzgroesse), keine Heap-Messung. Gemessen ist nur die Zaehlung im Integrationslauf (Konzept 10/11: 193 643 von 262 327 Arrays geteilt, ca. 7,1 MiB geschaetzt). Kein Heap-Vergleich vorhanden -> in Beschreibung nur "teilt Tabellen", keine MB-Zusage.
Keine P1.

## Urteil
**shipbar**
