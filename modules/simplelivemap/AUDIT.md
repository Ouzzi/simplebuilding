# Audit Simple Livemap (Server, Browser-Karte) - `simplelivemap` (b-w3b, 14.11.2026, Paket simplelivemap-3)

Basis `hub/b-bundle192` e487fcbf4, Branch `b-w3b`. Nachgeholte Release-Prüfung C1-C9 (Muster der Audits 31.10./12.11.).
Gelesen ohne Gradle: `LivemapConfig` (Kopf, Web-/Hosted-Gruppe), `LivemapWeb` (Kopf, Routen, Token-Prüfung, Grenzen), `HostedUploader` (Kopf), `LivemapCommands` (Berechtigungen), `fabric.mod.json`, `ModuleGameTest`-Katalog (19 Tests). **Nicht** jede der 3 374 Zeilen gelesen (Teile von `TileStore`, `ChunkSampler`, `VisitedStore`, `HostedClient`, `LivemapRuntime`, `SelfHostLock` nur nach Kopf/Tests); die Tabelle sagt je Punkt, was belegt ist.

## Ergebnis C1-C9

| Punkt | Ergebnis |
|---|---|
| C1 Code | ok nach Stichprobe. Alles Schwere läuft außerhalb des Server-Threads oder budgetiert: Kacheln `chunksPerTick` je Tick, Web-Server mit 4 Daemon-Threads und Warteschlange 32, Upload auf EINEM Hintergrund-Thread (Backoff bis 30 Minuten). Der Web-Server baut `live.json` aus einem vom Server-Thread vorbereiteten String. Eigene `ServerSocket`-Schleife statt `jdk.httpserver` (fehlt in abgespeckten Server-Runtimes). |
| C2 Mixins | keine (Mod hat keine Mixin-Konfiguration; Erfassung über Ticks/Events). Kein Absturzrisiko durch Mojang-Patches an Einhängestellen. |
| C3 Config | ok: `config/simplelivemap.json`, fünf Gruppen (general, record, render, web, hosted), jede Zahl geklemmt (Server-Autorität), kaputte/fehlende Schlüssel = Standard und Datei wird vollständig neu geschrieben. Web-Server an, aber nur `127.0.0.1` und privat (Token, 32 Hex-Zeichen, nie geloggt, im Befehl `***`); bestehende Datei mit `enabled:false` bleibt aus. `/simplelivemap config` + Mod-Menü-Bildschirm (SimpleLib), Token geheim. |
| C4 Daten | ok: Lang EN/DE, Viewer-Seite unter `assets/simplelivemap/web`, keine Rezepte/Loot. Kacheln in `world/…/simplelivemap` (nur Dateien dieser Mod). |
| C5 Unabhängigkeit | ok: `depends` simplelib >=0.4.0 (Konfig-Baustein), Fabric API; keine Klassen anderer Mods (Härtung wie Simple Maps' `WebHttp` bewusst nachgebaut). Läuft allein (Test `module-simplelivemap-standalone-fabric-263`). |
| C6 A12 | Eingriff: gering bis mittel. Ändert kein Spielverhalten; erfasst besuchte Chunks und zeigt sie im Browser. Standard ist Vanilla-nah und sicher: nur lokal (127.0.0.1), privat, Spielerpositionen im Web nur laut Schalter `showPlayers`. Der gehostete Dienst (`/simplelivemap connect`) ist nur auf ausdrücklichen Befehl aktiv. |
| C7 Netzwerk | Web: nur `GET`/`HEAD`, drei feste Routen (Viewer `/`, `/live.json`, `/tiles/<dim>/<0-4>/<x>/<y>.png`), alles andere 404; keine Prozent-Dekodierung, kein `..`, keine Links aus dem Ordner (Test `httpTraversal`); private Karte verlangt `Authorization: Bearer <token>` (konstanter Vergleich `MessageDigest.isEqual`, leerer Token öffnet nie; Test `httpVisibility`); Token steht im URL-Fragment (geht nie an Server/Proxy); Content-Security-Policy `default-src 'none'`, `frame-ancestors 'none'`; je Adresse Minutenbudget vor dem Lesen, Anfrage in 5 s, Zeilen 2 048 Zeichen, 64 Header, Datei max. 1 MiB (Test `httpRateLimit`). Befehle: Spielleiter (Stufe 2) oder Besitzer der Einzelspielerwelt (`masterOrOwner`, Test `connectPermission`). Hosted: nur https (http nur zu dieser Maschine), Schlüssel liegt im Weltordner und nie in der Config, 401/410 beenden die Verbindung. Eigene Spielerdaten (Position) gehen nur an den, der den Token kennt oder an alle, wenn der Besitzer `public` wählt. |
| C8 Lizenz/Assets | Lizenz `LicenseRef-SimpleMods-ARR`, Icon 16 px + 128-px-Kopie aus `icon.layers.json` (`check_mod_icons.py` grün). Keine Secrets im gelesenen Code; Standard-Endpunkt ist die öffentliche Adresse livemap.simplemods.de. Viewer-Seite ohne fremde Skripte (`script-src 'unsafe-inline'`, keine externen Hosts). |
| C9 Offen | kein TODO/FIXME im Mod-Code (grep). Offen: Browser-Prüfung im echten Browser aus dem Spiel (Paket simplelivemap-2: Chat-Link, ziehen/zoomen, Dimensionen, 3 Stile) und der echte Client-Start im Bündel. |

## Befunde

- simplelivemap-P3-1: `script-src 'unsafe-inline'` in der CSP, weil die Viewer-Seite ein eingebettetes Skript ist. Die Seite enthält keine fremden Daten; Hinweis (kein Fix nötig, `connect-src 'self'` begrenzt Abflüsse).
- simplelivemap-P3-2: Web-Server bindet bei `bind` ungleich 127.0.0.1 auf alle gewählten Adressen - dann ist die Karte aus dem Netz erreichbar; der Standard bleibt lokal + privat, `public` ist eine bewusste Besitzer-Entscheidung (Handbuch erklärt es). Hinweis.
- simplelivemap-P3-3: Der Standard-Endpunkt des gehosteten Dienstes ist fest im Code (`HostedClient.DEFAULT_ENDPOINT`), per Config änderbar, nur https. Hinweis für die Mod-Seite (Datenschutz-Satz: "Nichts wird hochgeladen, bis `/simplelivemap connect` ausgeführt wird").

Keine P1/P2.

## Urteil

**shipbar** (0 P1, 0 P2, 3 P3 Hinweis). Vor der Veröffentlichung: Browser-Prüfung (simplelivemap-2) und echter Client-Start im Bündel.

## Release-Checkliste Simple Livemap 1.0.0

- [x] Version 1.0.0 (`modules.json`, `moduleVersion`, Changelog-Kopf)
- [x] Icon 16 px + 128 px, Lizenz, Kontakt/Quellen, `depends`
- [x] Config mit Standard/Default-Texten, Mod-Menü-Bildschirm (SimpleLib), Token geheim
- [x] Web-Härtung getestet (Traversal, Sichtbarkeit, Rate-Limit, Port-Fallback), Gametests laufen im Modul-Lauf
- [x] Seitentext und Changelog (Entwurf, `docs/release/pages/simplelivemap.md`, `docs/release/CHANGELOG-simplelivemap.md`)
- [x] Handbuch-Kapitel geprüft (siehe Abschlussbericht b-w3b: Kapitel im Handbuch vorhanden)
- [ ] Im echten Browser aus dem Spiel: Chat-Link, Karte ziehen/zoomen, Dimension, 3 Stile (Paket simplelivemap-2)
- [ ] Echter Client-Start im Bündel, Gate grün
- [ ] Besitzer-Abnahme, Freigabe "ja" vor dem Hochladen
