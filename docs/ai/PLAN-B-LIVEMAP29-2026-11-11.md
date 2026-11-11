# PLAN B – Simple Livemap: funktioniert immer, selbst hosten, später gehostet (N29, b-livemap29, 11.11.)

Auftrag N29 (Besitzer 11.11. ~12:25, `/root/b-auftrag-20261109.md`): „Ich möchte, dass Simple Livemap immer
funktioniert, wenn man die Mod installiert hat; wenn man will, kann man auch eine eigene Seite hosten, oder geringe
Bandbreite über meine Seite haben, und wer mehr haben will, zahlt – das aber später.“
Basis `hub/claude-wave1` (7def212d6, enthält b-livemap16 über Bündel 169 – geprüft mit `git log --oneline | grep livemap`;
den Branch `b-livemap16` selbst gibt es auf dem Hub nicht mehr, nur lokal).

## 1. Ist-Stand

- `LivemapConfig`: `web.enabled` Standard **false**, `bind` 127.0.0.1, `port` 8125, `visibility` private, Token
  128 bit beim ersten Start. Die Datei wird beim Laden **immer vollständig** neu geschrieben (`load` → `save`).
- `LivemapRuntime.syncWeb`: startet den Server im Takt (alle 20 Ticks), belegter Port → Warnung, neuer Versuch nach 60 s.
  Kein Ausweichen auf einen anderen Port, kein Hinweis an Spieler.
- `/simplelivemap status|link|visibility` nur für Spielleiter (Level 2) – im Einzelspieler ohne Cheats also nicht nutzbar.
- Selbst-Hosten: nur die sb-web-spezifischen Werkzeuge aus N14b (`tools/deploy/livemap/`, `docs/deploy/LIVEMAP-SB-WEB.md`,
  Bash/systemd/nginx für unseren Wiki-Host).

## 2. Entscheidungen

| Frage | Entscheidung | Grund |
|---|---|---|
| Bestehende Configs mit `"enabled": false` | bleiben aus, keine Migration | Die Datei steht immer vollständig auf der Platte; ein altes `false` ist von einer bewussten Abschaltung nicht zu unterscheiden. Einen Netzdienst still einzuschalten wäre eine Sicherheits-Überraschung für Server-Betreiber. Neue Installationen (keine Datei) bekommen `true`. Handbuch/Doku nennen den Schalter. |
| Port-Ausweichen | nur wenn der eingestellte Port im Standardbereich 8125–8134 liegt: eingestellter Port, dann die folgenden bis 8134, dann ab 8125 (Ringsuche, höchstens 10 Versuche); Log-Zeile „Port X belegt, nutze Y“ | Ein selbst gewählter Port (Proxy/Firewall zeigen darauf) darf nicht still wandern. Die Config wird nicht geändert (Ausweichen gilt nur für diese Sitzung). |
| Wer bekommt den Chat-Hinweis | nur der Einzelspieler-Besitzer (`isSingleplayerOwner`) auf einem integrierten Server (Einzelspieler oder „Im LAN öffnen“), per `sendSystemMessage` nur an ihn | LAN-Gäste dürfen den Schlüssel nicht bekommen (er ist das Geheimnis des Gastgebers), und `127.0.0.1` wäre auf ihrem Rechner ohnehin falsch. Dedicated Server: nur Log. |
| Wie oft | einmal je Sitzung (Welt öffnen) und Spieler; abschaltbar per `web.chatHint` (Standard an) oder Klick „Nicht mehr zeigen“ (`/simplelivemap hint off`) | „einmal je Welt/Sitzung“: der Link gilt nur, solange die Welt offen ist, und der Port kann je Sitzung anders sein (Ausweichen) → je Sitzung einmal ist der nützliche Zeitpunkt. |
| Befehle im Einzelspieler ohne Cheats | `status`, `link`, `hint` auch für den Einzelspieler-Besitzer | Sonst zeigt der Hinweis auf einen Befehl, den der Spieler nicht ausführen darf. Server-Rechte unverändert (Level 2). |
| Link-Adresse bei `bind` 0.0.0.0/:: | Link zeigt `127.0.0.1` | `http://0.0.0.0/` öffnet in vielen Browsern nicht. Sonst wie bisher. |
| Dedicated-Hinweis | beim Start auf einem Dedicated Server eine Log-Zeile: nur dieser Rechner (Loopback) bzw. „im Netz erreichbar“ (sonst), mit Verweis auf `web.bind`/`web.visibility` und `/simplelivemap link` | Kein Zugriff von außen ohne bewusste Einstellung bleibt gewahrt (`bind` bleibt 127.0.0.1). |
| Wiki-Seite Selbst-Hosten | neuer Abschnitt `self_host` in `modules/simplelivemap/wiki/manual.json` (summary + details EN/DE; das Handbuch zeigt nur die Zusammenfassung) + `docs/deploy/LIVEMAP-SELF-HOST.md` | Wiki-Seiten der Module entstehen aus manual.json; Handbuch bleibt kurz (Seitenlimit 100). |
| Sync-Skript | `tools/deploy/livemap/selfhost/livemap_static_sync.py` (nur Python-Standardbibliothek → Linux/macOS/Windows), kopiert nur das Kachelmuster, schreibt `live.json` + `index.html` (aus dem Mod-Jar), optional Schleife | eine Datei statt Bash+PowerShell-Zwilling; Hochladen (rsync/scp/SFTP) bleibt beim Nutzer. |
| Proxy-Beispiele | `selfhost/nginx-livemap.conf.example`, `selfhost/Caddyfile.example` (Platzhalter `map.example.org`, keine echten Hosts) | aus `nginx-map.conf.in` verallgemeinert (Routenfilter, nur GET/HEAD, Ratenlimit, Token-Modi). |

## 3. Umsetzung

a) Mod (`modules/simplelivemap/shared/java/...`):
- `LivemapConfig`: `web.enabled` Standard true, neuer Schlüssel `web.chatHint` (Standard true), `PORT_RANGE = 10`,
  `candidates(port)` (Ringsuche im Standardbereich, sonst nur der Port).
- `LivemapWeb.start(settings, ports, live, alive)`: bindet den ersten freien Port der Liste; alle belegt → letzte Ausnahme.
- `LivemapRuntime`: Ausweichen + Log, Dedicated-Log-Hinweis, `hintDue(...)` (reine Entscheidung, testbar) + Senden im
  Tick (alle 20 Ticks nach `syncWeb`), `hinted`-Menge je Sitzung.
- `LivemapCommands`: `hint on|off`, Besitzer-Recht für `status|link|hint`, `hintMessage(url)` (Text + „[Livemap öffnen]“
  mit `ClickEvent.OpenUrl` + „[Nicht mehr zeigen]“ mit `RunCommand`), Wildcard-bind → 127.0.0.1.
- Lang EN/DE (Tooltips mit Default:/Standard:, neue Befehlstexte), manual.json (Webkarte/Privat/neu Hinweis, Selbst hosten),
  Handbuchseiten per `module_guides.py`, Beschreibungen (modules.json, fabric.mod.json, Release-Seiten).
- Gametests: `configDefaults` (Standard an, chatHint an), `httpOffDefault` → `httpDefaultOn` (Standard an: Loopback,
  privat, 401 ohne Schlüssel; aus → zu), neu `httpPortFallback` (Ringsuche, belegter Port → nächster, alle belegt →
  Fehler, eigener Port wandert nicht), neu `chatHintOnce` (einmal je Spieler, nie auf Dedicated/für Gäste/ohne Webserver/
  abgeschaltet; Text enthält OpenUrl mit `#t=` bzw. ohne Token öffentlich).

b) Doku: `docs/deploy/LIVEMAP-SELF-HOST.md` + `tools/deploy/livemap/selfhost/` (nginx, Caddy, Sync-Skript),
   Verweis aus `docs/deploy/LIVEMAP-SB-WEB.md`.

c) Konzept: `docs/ai/KONZEPT-B-LIVEMAP-HOSTED-2026-11-11.md` (kein Bau).

## 4. Prüfung

Lokal: javac der geänderten Modulklassen gegen den 26.3-Jar, `check_data.py`, `check_lang_keys`, `check_tooltip_text`,
`module_guides.py --check`, `precheck --fast`, `nginx -t` des Beispiels (nginx 1.26 aus /var/tmp/ngx-b), Sync-Skript
gegen eine Test-Kachelstruktur (inkl. Link/Fremddatei/zu groß). Ein sbt: Compile + module-simplelivemap-fabric-263 +
standalone + `fabric-263 --filter 'simplebuilding:data_integrity*'`.
Nicht prüfbar hier: Klick im echten Client (keine Client-Läufe erlaubt, Regel 16) → Mockup in der Vorschau, Client-Sicht
im Bündel. Caddy-Beispiel ohne Caddy-Binary nur gelesen.

## 5. Abweichungen

(nach der Umsetzung ergänzt)
