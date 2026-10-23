# Plan: Simple Maps Webkarte Stufe 2 – HTTP + Live (A, `a-webmap2`, 23.10.2026)

Grundlage: `docs/ai/KONZEPT-WEBKARTE-2026-10-19.md` (Stufe 2 „optionaler HTTP-Server (Port, Token) und Live-Spieler“,
Auslieferungsweg B), Stufe 1 `docs/ai/PLAN-B-WEBMAP-2026-10-19.md` (gemergt). Nur MC 26.3 Fabric (Regel 22.10.).
Stufen 3 (Wegpunkte, Dirty-Chunks, Claim-Verpixeln) und 4 (3D) bleiben offen.

## Ist-Zustand (erhoben)
- `web/WebMap` (Schalter, autoOffWith, Export-Job, Opt-in-SavedData `web_players`), `web/WebExport` (Kacheln, `markers.json`,
  `index.html` aus dem Jar), `web/WebCommands` (`/simplemaps web export|status|show|hide`). Export ist rein statisch,
  die Mod öffnet keinen Port; Server-Tick nur über `WebMap.tick` (Fabric `END_SERVER_TICK`).
- Ansicht `assets/simplemaps/web/index.html`: lädt `markers.json` alle 60 s neu und leert dabei **jedes Mal** den
  Kachel-Cache (alle sichtbaren Kacheln werden minütlich neu geladen) – wird mit Stufe 2 mitbehoben.
- Spieler in `markers.json` nur nach Opt-in, keine Zuschauer, keine UUIDs. Tests: `test/WebTests` (8 Gametests),
  Fabric-Adapter `ModuleGameTest`.

## Entscheidungen (selbst getroffen, begründet)
1. **Eigener Mini-Server auf `java.net.ServerSocket`** statt `com.sun.net.httpserver`: das Modul `jdk.httpserver` fehlt in
   verkleinerten Java-Laufzeiten (jlink, Launcher-Runtime) → Absturzrisiko; ein eigener GET-only-Server sind ~250 Zeilen
   und jede Grenze (Zeilenlänge, Header, Zeit, Rate) ist in unserer Hand. Kein Fremdcode.
2. **Live-Verfahren = Polling** einer kleinen Datei `live.json` (Spieler + Export-Stempel) statt WebSocket/SSE:
   kein Protokoll-Upgrade, keine offenen Verbindungen, funktioniert hinter jedem Reverse-Proxy/Cloudflare.
   `live.json` baut der **Server-Thread** alle `webLiveSeconds` (Standard 2) als fertigen String; HTTP-Threads lesen nur
   diesen `volatile`-String → kein Zugriff auf Spielzustand aus fremden Threads.
3. **Live-Kacheln:** Kacheln entstehen weiter nur durch den Export (Befehl oder `webIntervalMinutes`); `live.json` trägt
   `exportedAt`. Ändert er sich, lädt die Ansicht `markers.json` neu und holt die Kacheln mit neuem `?v=<Stand>`.
   Ohne Änderung wird der Cache nicht mehr geleert (Fix Ist-Zustand). Dirty-Chunks bleiben Stufe 3.
4. **Opt-in bleibt:** HTTP nur, wenn `webEnabled` **und** `webHttpEnabled` (beide Standard aus) und kein autoOffWith-Mod;
   `live.json` enthält dieselben Spielerdaten wie `markers.json` (nur `/simplemaps web show`, keine Zuschauer, keine UUIDs);
   Shops/`webHideClaims` gelten unverändert, weil nur der Exportordner ausgeliefert wird.
5. **Config** (flach wie Stufe 1, `simplemaps-server.json`): `webHttpEnabled` false, `webHttpBind` `127.0.0.1`,
   `webHttpPort` 8124 (1–65535), `webHttpToken` "" (leer = ohne), `webHttpRequestsPerMinute` 1200 (60–20000, je IP),
   `webLiveSeconds` 2 (1–60). Nicht-Loopback ohne Token → WARN-Logzeile.
6. **Lebenszyklus ohne Loader-Hook:** `WebMap.tick` gleicht jede Sekunde Soll/Ist ab (starten, stoppen, bei geänderter
   Config neu binden; Bind-Fehler → WARN, neuer Versuch erst nach 60 s). Der Annahme-Thread prüft jede Sekunde
   `MinecraftServer.isRunning()` und schließt sich selbst → Port wird beim Weltende frei, auch ohne Stop-Event
   (keine NeoForge/Forge-Anpassung nötig). Alle Threads sind Daemon-Threads.
7. `/simplemaps web status` nennt zusätzlich die HTTP-Adresse (bzw. „aus“). Kein neuer Befehl.

## Sicherheit (Begründung)
- **Nur lesend:** nur `GET`/`HEAD`; alles andere 405. Kein Request-Body wird gelesen. Kein Endpunkt ändert Zustand,
  keine Befehle/Konsole über HTTP – die einzigen Antworten sind Dateien des Exportordners und der fertige `live.json`-String.
- **Kein Pfad-Traversal:** feste Weißliste per Regex (`index.html`, `markers.json`, `live.json`,
  `tiles/<ordner>/<0-4>/<x>/<y>.png`, `icons/<name>.png`); `%`, `\`, `..`, `//`, NUL werden vor dem Regex abgewiesen
  (keine Prozent-Dekodierung → keine Doppel-Kodierungstricks). Danach `normalize().startsWith(dir)` und
  `toRealPath().startsWith(dir.toRealPath())` (Symlinks aus dem Ordner heraus werden abgelehnt). Nur reguläre Dateien
  ≤ 4 MiB. Unbekanntes → 404 (keine Auskunft, ob eine Datei existiert).
- **Ratenbegrenzung:** fester Minutenzähler je IP (`webHttpRequestsPerMinute`), Überschreitung → 429 + `Retry-After`;
  höchstens 4096 IPs je Fenster (danach neue IPs abgewiesen). Arbeits-Pool 4 Threads + Warteschlange 32, voll →
  Verbindung wird geschlossen. Je Verbindung eine Anfrage (`Connection: close`), Lese-Timeout 5 s,
  Anfragezeile ≤ 2048 Byte, ≤ 64 Header à ≤ 2048 Byte.
- **Zugang:** Standard-Bind `127.0.0.1` (nur der eigene Rechner); optionales Token (`?token=` oder
  `Authorization: Bearer`), konstante Vergleichszeit. Für öffentliche Karten weiter Reverse-Proxy + Cloudflare Access
  empfohlen (Doku).
- Antwort-Header: `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, strenge CSP
  (`default-src 'none'`, nur Inline-Skript/-Stil der eigenen Seite, Bilder/Fetch nur `'self'`, `frame-ancestors 'none'`).

## Dateien
- neu `modules/simplemaps/shared/java/com/simplemaps/web/WebHttp.java` (Server, `route`, `RateLimit`)
- `web/WebMap.java` (HTTP-Abgleich im Tick, `live.json`-Schnappschuss), `web/WebExport.java` (`players(server)` statisch
  für Export und Live), `web/WebCommands.java` (Status-Zeile), `MapsConfig.java` (6 Schlüssel)
- `assets/simplemaps/web/index.html` (Token durchreichen, Live-Polling, Cache-Fix, `?v=`)
- `test/WebTests.java` (+6 Gametests), Fabric-Adapter `ModuleGameTest`, Lang EN/DE (1 Schlüssel)
- Doku `docs/SIMPLEMAPS-WEBKARTE.md`, `modules/simplemaps/wiki/manual.json` (EN/DE), `docs/MODS-UND-FEATURES.md`, Queue.

## Tests (Gametests, `MapsTests.ALL`, Fabric-Adapter)
`web_http_config_defaults` (Standardwerte aus, 127.0.0.1, Grenzen geklemmt), `web_http_on_off` (Default kein Server;
an → Port offen, aus → Port zu; webEnabled aus/autoOff → kein Server), `web_http_traversal` (`route` lehnt
`..`/`%2e`/`\`/absolute/fremde Endungen ab; echte Anfragen auf `/../`, `%2e%2e`, Datei außerhalb, Symlink → 404),
`web_http_endpoints` (`/` HTML, `markers.json`, Kachel-PNG, `live.json`, 404 fehlend, 405 POST, HEAD ohne Body,
Token 401/200), `web_http_rate_limit` (Grenze → 429, neues Fenster wieder frei), `web_live_opt_in` (`live.json` nur mit
Opt-in, ohne UUID). Server in Tests auf Port 0 (frei gewählt), eigene Instanzen, Aufräumen im `finally`.

## Verifikation (sb-test über `/root/sbt a-webmap2`)
Compile `:mc26_3:fabric:compileJava` + `:modules:simplemaps:fabric:compileJava`; Targets `module-simplemaps-fabric-263`,
`module-simplemaps-standalone-fabric-263` (Filter `simplemaps:*`); Guides/Wiki/Discover erzeugen; `./gradlew check -q`.
Nicht getestet: Ansicht im Browser (keine Client-/Browser-Läufe) → Besitzer-Abnahme; NeoForge/Forge (Regel 22.10.).

## Risiken
Port-Freigabe/Hoster-Regeln (Standard aus + localhost); Token in der URL kann beim Teilen des Links mitwandern (Doku);
viele Zuschauer → Rate-Grenze/Pool begrenzen die Last; Exportordner auf langsamer Platte → 5-s-Timeout.
