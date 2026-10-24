# Plan b-revmid24 (24.10.2026): mittlere Funde M-2..M-12 aus review24/teil2-module.md

Basis claude-wave1. Nicht angefasst (b-revfix24): F-01..F-06, H-1..H-4, Codec-Helfer. M-1 (Redirects) und der Codec-Teil von M-9 bleiben offen.
Nur 26.3 Fabric. Verifikation: Compile Fabric + Modul-Fabric, gezielte Gametests (Targets module-<mod>-fabric-263), Generatoren, `./gradlew check`.

| Fund | Befund im Code | Aenderung | Test |
|---|---|---|---|
| M-2 | bestaetigt: `ActivationRange.exempt` kennt Effekte/Brut/Huehner nicht | exempt: aktive Effekte, `getAge() != 0` (Brut-Abkuehlzeit), Huehner | `s5bRule` erweitert |
| M-3 | bestaetigt: `AntiXray.obfuscate` rechnet je Spieler neu; Ergebnis ist spielerunabhaengig | Cache je (Level, Chunk) 20 Ticks, Invalidierung (Chunk + 8 Nachbarn) in `sendBlockUpdated`, Config-Wechsel | `bench` erweitert |
| M-4 | bestaetigt: Recht nur bei Markieren/Anhaengen | `LinkedContainers.tick`: jede Sekunde `canOpen` + `permission`, sonst Markierung beenden | `linked_mark` erweitert |
| M-5 | (b)/(c) bestaetigt; (a) Absturz-Reste: kein Marker, bleibt offen | Lit(Dimension, Pos); ungeladener Chunk wird nicht geladen, Licht wartet in STALE und wird beim Laden entfernt; Entfernen in der eigenen Dimension | nicht automatisierbar (Chunk-Entladung) |
| M-6 | bestaetigt | Gesamt-Deadline 5 s je Verbindung (`readLine(in, deadline)`), Rate-Limit direkt nach accept | `web_http_rate_limit` erweitert |
| M-7 | bestaetigt; Verarbeitung auf Worker-Thread zurueckgestellt (Inventar-Zugriff, Server-Thread) | Puffer waechst statt Vorab-Allokation, Ablauf im Tick, `on()`-Pruefung im Chunk-Handler | neu `upload_abandoned_expires` |
| M-8 | bestaetigt | `collect()` vor Abschluss-Drain, Queue-Grenze 200 000 (neue verwerfen + eine Warnung) | `worldEvents` erweitert |
| M-9 | nur statischer Zwischenzustand | `@WrapMethod` mit try/finally statt HEAD/RETURN | vorhandene soulbound-/vault-Tests |
| M-10 | bestaetigt | Ort vor Teleport pruefen (`standable`), bei Disconnect/Stop vergessen | `roof` erweitert |
| M-11 | bestaetigt | Probe zaehlt `InteractionGuard.probing`, Veto fuer Flaechenhaemmer (`HammerActions.registerAreaVeto`); gemeinsame Probe in simplelib bleibt offen | Fabric-Test `permissionProbeVetoesAreas` |
| M-12 | Teil bestaetigt (Chunk-Zugriffe); Drosselung gibt es schon (INTERVAL = 200 Ticks) -> verworfen | `hasChunkAt` vor Bell-Struktursuche und in `findSpot` | neu `find_spot_skips_unloaded` |
