# Plan b-revlow24: niedrige Review-Funde (24.10.2026)

Quelle: /root/sb-tools-b/review24 (teil1-kern F-22..F-30, teil2-module N-1..N-8). Nur 26.3 Fabric. Nicht angefasst: Themen von b-revfix24 (Blueprint, Licht, Rucksack, Backups-Stop-Logik, MapsNetwork-Karten), b-revmid24, b-revcore24.

| Fund | Befund im Code | Entscheidung |
|---|---|---|
| F-22 | bestaetigt: drei tick-TAIL-Hooks, `modifyVoidDamage` ohne Praefix | Ein Hook `simplebuilding$tickEffects` (gleiche Reihenfolge/Modulo), Rename `simplebuilding$modifyVoidDamage`. Wither-Gegenheilung bleibt (Verhalten unveraendert; Accessor-Umbau = Vorschlag) |
| F-23 | bestaetigt | Kopie nur serverseitig mit Spieler (Client verwirft sie ohnehin) |
| F-24 | veraltet: `inRelayRange` nutzt schon `maybeHas` pro Abschnitt, `new BlockPos` nur nach Palettentreffer | verworfen. Rest (Welt-Cache statt globaler `GENERATION`) = Vorschlag |
| F-26 | halb: `List.of()` bei leeren Providern gibt es schon; stilles `catch` bestaetigt | je Provider einmal warnen (System.Logger, framework bleibt reines Java) |
| F-27 | bestaetigt | Namen hochzaehlen (`-2`, `-3`) statt Kollision |
| F-28 | Vorschlag | Registrierung nur in Dev/Gametest; Aufwand: Loader-Einhaengung, Nutzen klein, daher nicht umgesetzt |
| F-29 | nicht belegt, aber ungefaehrlich | `Transaction.getCurrentUnsafe()` -> nested statt outer |
| F-30 | bestaetigt (Client/Server-Thread im Einzelspieler) | `ThreadLocal<int[]>` |
| N-1 | teils: `Pvp`/`WandererSpawner` raeumen schon bei Stop. Offen: `FearNet.forget` nie gerufen, `HerobrineHaunt` ueberlebt Disconnect | Fabric-DISCONNECT in MobsFabric/MobHeadsFabric. NetherRoof/TrimLight und der gemeinsame simplelib-Hook `PlayerState.onLeave` = Vorschlag |
| N-2 | bestaetigt | `SimpleLibApi.isWarm/warm/warmFasterUse` (+ vorhandene `warmable/warmingTicks`); Crucibles nutzt nur noch die API |
| N-3 | bestaetigt | `ConfigFiles.migrateOptions` + `renameOptionKey` (zeilenanfangs-genau, atomar) im framework; beide Migrationen nutzen sie (Duplikat-Logik weg, framework ist in beiden Modulen gebuendelt) |
| N-4 | bestaetigt | `ConfigFiles.writeAtomic`; alle `Files.writeString(file/path, GSON...)`-Config-Schreiber in 28 Modulen umgestellt (nicht: Tests, Client-Screens, Erststart-Sonderfaelle mit CREATE_NEW) |
| N-5 | Vorschlag | InteractionGuard/Testhelfer nach simplelib: eigene Aufgabe (Modulgrenzen, 3 Module) |
| N-6 | bestaetigt | `stop()`: `awaitTermination(5 s)`; bewusst nur diese Zeile (b-revfix24 aendert `stop()` darueber) |
| N-7 | halb: `LayerService.handle` prueft schon | `enabled`-Check in `handleWaypoint`/`handleDisplay` |
| N-8 | bestaetigt | Schluessel `Cell(dimension, pos)` |

Verifikation: framework-JUnit `ConfigFilesTest`, Compile fabric 26.3 + betroffene Module, Modul-Gametests simplecrucibles/simplemaps/simplehammers/simplestorage/simplemovement (fabric-263), Generatoren, `./gradlew check`.
