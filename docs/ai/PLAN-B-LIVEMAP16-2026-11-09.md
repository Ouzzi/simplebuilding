# PLAN B – Simple Livemap auf dem Web-Host (N14b, b-livemap16, 09.11.)

Auftrag N14b (Besitzer „ja“): die Simple Livemap soll auf demselben Host wie das Wiki ausgeliefert werden.
Nur Repo-Skripte und Anleitung; eingespielt wird von A (mit Besitzer-OK). Das Repo ist öffentlich: keine IPs,
Host-Nummern oder Schlüssel hier – Adressen stehen nur in der Memory und werden beim Einspielen als Parameter übergeben.

## 1. Ist-Stand (erhoben 09.11., Web-Host nur lesend)

**Mod (`modules/simplelivemap`, Stufe 1 aus b-live32, in Production seit Gate 84):**
- Eigener HTTP-Server im Spielserver-Prozess (`web/LivemapWeb.java`, eigener `ServerSocket`, kein jdk.httpserver).
  Config `config/simplelivemap.json`, Gruppe `web`: `enabled` (Standard **aus**), `bind` (Standard `127.0.0.1`),
  `port` (Standard `8125`), `visibility` `private|public` (Standard private), `token`, `showPlayers` (aus),
  `requestsPerMinute` (1200, je Client-Adresse), `liveSeconds` (2).
- Nur `GET`/`HEAD`, drei Routen: `/` (Viewer-Seite aus dem Jar, ohne Daten), `/live.json` (Dimensionsliste +
  optional Spieler, im Speicher vorbereitet, **nicht** auf Platte), `/tiles/<dim>/<z>/<x>/<y>.png` (Dateien aus
  `<Welt>/simplelivemap/tiles/`, 128 px, z 0–4). 204 für unerforschte Kacheln. Kein Websocket/SSE: der Viewer
  pollt `live.json` alle 2 s und lädt Kacheln per `fetch`.
- **Auth-Mechanismus:** private Karte → Datenrouten nur mit `Authorization: Bearer <token>` (konstantzeitiger
  Vergleich, leerer Token öffnet nie). Token = 128-bit-Zufall, beim ersten Start erzeugt, steht nur in der
  Config-Datei, im Befehl als `***`. Der Viewer liest ihn aus dem URL-Fragment `#t=…` (geht nie an Server/Proxy-Logs)
  und schickt ihn nur als Header. `/` selbst ist ohne Token abrufbar (enthält keine Daten).
- Härtung: 5-s-Zeitlimit je Anfrage, eine Anfrage je Verbindung (`Connection: close`), 4 Worker + Warteschlange 32,
  Ratenbudget je Client-IP pro Minute, CSP `default-src 'none'` mit `connect-src 'self'`, `img-src 'self' blob:`.
- **Pfade im Viewer sind relativ** (`tiles/…`, `live.json`) → läuft unverändert unter einem Präfix wie `/map/`,
  solange die Seite mit Schrägstrich am Ende aufgerufen wird. **Keine Mod-Änderung nötig** (siehe §4).
- Simple Maps hat zusätzlich einen eigenen statischen Export (`WebExport`, Wegfinder-Karten) – anderes Modul,
  nicht Teil dieses Auftrags.

**Web-Host (Wiki, nginx 1.26, 1 GB RAM, 7,8 GB Platte, ~57 % belegt; Platte lief schon dreimal voll):**
- Eine Site-Datei `sites-available/simplemods`, gerendert von `tools/zentrale-web/deploy_dom07.sh domains` aus
  `tools/zentrale-web/nginx/*.conf` (wiki-one.conf + wiki-extra.conf an der Marke `@EXTRA@`). Handänderungen an der
  gerenderten Datei gehen beim nächsten Rendern verloren.
- Server `wiki.simplemods.de` lauscht auf den Tunnel-Ports; Allowlist-Snippet lässt nur die Tunnel-Connectoren und
  Loopback zu. Login über devqueue (`auth_request /_auth` → `/api/auth/check`), Muster `/dev/`, `/zentrale/`.
  Client-IP für Ratenlimits: `CF-Connecting-IP` (map `$sb_client_ip`, schon vorhanden).
- Kein `curl`, kein `rsync`, kein `jq` auf dem Host; `python3`, `wget`, `nft` vorhanden. `/var/cache/nginx` fehlt.
- Zugriff Internet → Host nur über den Cloudflare-Tunnel (drei Connectoren).

**Spielserver:** eigene VM auf demselben Proxmox-Host (Adresse siehe Memory `ct-umgebung.md`), von den Agenten
nicht erreichbar. Ob die Livemap dort installiert und `web.enabled` an ist, ist **nicht** geprüft → Schritt 1 der
Anleitung (A/Besitzer).

## 2. Varianten

| | (A) Reverse-Proxy | (B) Sync + statisch | (C) Mischform |
|---|---|---|---|
| Live-Daten | ja (Spieler, frische Kacheln) | nein (Kacheln nur so frisch wie der Timer, keine Spieler) | ja |
| Mod-Seite | `web.enabled`, `bind` auf LAN-IP, Firewall nur Web-Host | Web-Server bleibt aus | wie A |
| Web-Host Platte | nginx-Cache, gedeckelt (256 MB) | volle Kachelkopie, wächst mit der Welt | Cache gedeckelt |
| Web-Host Pakete | keine | `rsync` auf beiden Seiten, SSH-Schlüssel + `rrsync -ro` | keine |
| Spielserver aus | Seite/Kacheln aus dem Cache (stale), live.json 502 | läuft weiter mit letztem Stand | wie A |
| Last Spielserver | nur Cache-Misses (≤ 1 Anfrage je Kachel je 30 s), live.json max. 1/s | rsync-Scan je Lauf | wie A |
| Risiken | Port im LAN offen (Firewall!) | Platte voll (bekanntes Problem), Kopie veraltet | – |

(C) ist mit nginx-Mitteln ohne eigenes Sync-Programm erreichbar: Proxy + `proxy_cache` mit
`proxy_cache_use_stale` → frische Daten, solange der Spielserver läuft, letzter Stand, wenn er schläft.

**Pfad:** `wiki.simplemods.de/map/` statt `livemap.simplemods.de`: gleiches Login-Cookie und dieselbe Site,
kein neuer Tunnel-Hostname, kein neues DNS; Viewer kann relative Pfade. Eigene Subdomain bleibt später möglich.

**Login (drei Modi im Skript, `--mode`):**
- `login` (**Standard**): `/map/` hinter dem vorhandenen Wiki-Login; nginx setzt den Bearer-Token selbst
  (`proxy_set_header Authorization`), der Token verlässt Web-Host/Spielserver nie, kein Link nötig. Cache an.
- `token`: kein Wiki-Login; Spieler bekommen den Link `…/map/#t=<token>` (`/simplelivemap link`), der Header des
  Browsers wird durchgereicht. **Cache aus** (sonst bekäme ein Besucher ohne Token gecachte Kacheln).
- `public`: Mod `visibility public`, kein Login, Cache an. Nur nach ausdrücklicher Besitzer-Entscheidung.

**Empfehlung: (A) mit Cache = (C)-Verhalten, Pfad `/map/`, Modus `login`.** Begründung: einzige Variante mit
Live-Daten; keine neuen Pakete/Schlüssel; Platte durch `max_size` gedeckelt (B würde unbegrenzt wachsen, Platte
lief schon mehrfach voll); Login-Infrastruktur und Ratenlimit-Map gibt es schon; Rückbau = drei Dateien + eine
Include-Zeile. (B) wird als Rückfall mitgeliefert (`livemap-sync.sh`), falls der Besitzer den Port im LAN nicht
öffnen möchte.

## 3. Entscheidungen (selbst getroffen)

- E1 **Ratenlimit:** Hinter dem Proxy sieht die Mod nur die Adresse des Web-Hosts → ihr Budget wird zum
  Gesamtbudget. Lösung ohne Mod-Änderung: nginx-Limit je Besucher (`$sb_client_ip`, 10 r/s, burst 80 – ein
  Viewer lädt ~40 Kacheln auf einmal) + Mod-Budget hoch (`requestsPerMinute` 6000). X-Forwarded-For in der Mod
  auszuwerten wäre zusätzlicher Angriffsweg (Header fälschbar, Vertrauensliste nötig) → bewusst nicht.
- E2 **Cache:** Mod schickt `Cache-Control: private` → nginx ignoriert das (`proxy_ignore_headers`) und cacht
  selbst: Kacheln 200/204 30 s, Seite 10 min, live.json 1 s (bündelt viele Viewer zu ≤1 Anfrage/s), stale nur für
  Kacheln + Seite. An den Browser geht `private` (hinter Login). Cache-Schlüssel ohne Token.
- E3 **Kein Websocket-Upgrade** nötig (Mod pollt). Hinweis im Snippet für künftiges SSE (`proxy_buffering off`).
- E4 **Token-Ablage** (Modus login): `/etc/nginx/simplemods-livemap.secret` (root, 600), nur per stdin ins
  Skript (nie als Argument → nicht in `ps`/History). `nginx -T` würde ihn zeigen → in der Anleitung verboten bzw.
  nur gefiltert.
- E5 **Persistenz über Neu-Rendern:** `tools/zentrale-web/nginx/wiki-extra.conf` bekommt
  `include /etc/nginx/snippets/simplemods-livemap[.]conf;` (Glob → ohne Datei kein Fehler, also No-op bis zum
  Einspielen). Bis zum nächsten Rendern fügt das Deploy-Skript die gleiche Zeile idempotent in die Live-Datei ein.
- E6 Spielserver-Seite: `bind` = LAN-IP der VM (nicht 0.0.0.0), Firewall-Regel nur Web-Host → Port; Beispiel
  nftables in der Anleitung (Ausführung durch Besitzer).

## 4. Umsetzung (Dateien)

- `tools/deploy/livemap/deploy_livemap.sh` – auf dem Web-Host als root: `check | install | smoke | rollback`,
  Parameter `--upstream HOST:PORT`, `--mode login|token|public`, `--source proxy|static`, `--dry-run`.
- `tools/deploy/livemap/nginx-http.conf.in` – http-Kontext (Cache-Pfad, Limit-Zone, Upstream).
- `tools/deploy/livemap/nginx-map.conf.in` – Server-Kontext `/map/` (Proxy), Platzhalter `@AUTH@`, `@TOKEN_INCLUDE@`,
  `@CACHE@`.
- `tools/deploy/livemap/nginx-map-static.conf.in` – Variante B.
- `tools/deploy/livemap/livemap-sync.sh` + `livemap-sync.service` / `livemap-sync.timer` – Variante B, idempotent,
  `--dry-run`, flock, Plattenwächter.
- `tools/deploy/livemap/test_render.sh` – lokaler Test: rendert alle Modi in ein Temp-Verzeichnis, prüft
  Platzhalter/Klammern und – falls nginx lokal vorhanden – `nginx -t` mit Stub-Konfiguration.
- `tools/zentrale-web/nginx/wiki-extra.conf` – Glob-Include (E5).
- `docs/deploy/LIVEMAP-SB-WEB.md` – Anleitung für A.
- Keine Mod-Änderung → kein sbt-Lauf (Auftrag: „falls im Mod nötig“).

## 5. Verifikation

- Lokal: `bash -n`, `shellcheck` (falls vorhanden), `test_render.sh`, `nginx -t` mit Stub, falls nginx installiert.
- Nicht getestet (kann nur A beim Einspielen): echter Proxy-Weg zum Spielserver, Login-Weiterleitung live,
  Cache-Verhalten unter Last. Prüfschritte dafür in der Anleitung (Abschnitt „Prüfen“).

## 6. Abweichungen / Ergebnis

- Umgesetzt wie §4; zusätzlich `livemap-sync.default.example` (Beispiel-Umgebungsdatei für Variante B).
- Abweichung: Modus `public` setzt ebenfalls den Token per nginx (Mod bleibt `private`) statt `visibility public` in
  der Mod – sicherer im LAN, gleiches Ergebnis für Besucher.
- Gefundene/behobene Fehler durch den lokalen Funktionstest: Regex-Location mit `{}` muss in nginx gequotet sein;
  falscher Token hätte die gültige Token-Datei überschrieben → jetzt erst gegen den Spielserver prüfen, dann ersetzen.
- Tests (lokal, nginx 1.26.3 aus dem Debian-Paket entpackt, gleiche Version wie der Web-Host):
  `test_render.sh` 50/50 ok (`TEST_RENDER_FAILS=0`: 5 Kombinationen gerendert, `nginx -t`, Funktionstest gegen
  nachgebauten Livemap-Server: Login-302, Token-Injektion, Cookie nicht weitergereicht, Cache MISS→HIT trotz `?bust`,
  Modus token ohne Cache 401, fremde Pfade nie beim Spielserver, Punktdateien 403); End-to-End `deploy_livemap.sh`
  mit Shims (install login/public/token/static, Idempotenz, falscher Token → alte Datei bleibt, kaputtes Template →
  alles zurück, rollback → Site-Datei = Sicherung); `tools/zentrale-web/tests/test_nginx_zw06` 7/7 mit echtem nginx;
  `bash -n` + shellcheck 0 Befunde.
- Nicht getestet: `livemap-sync.sh` echt (kein rsync auf sb-agent; nur `bash -n`/shellcheck), echter Weg zum
  Spielserver, Live-Login, Last. Kein sbt-Lauf (keine Mod-Änderung).
- Mögliche Folge (nicht nötig, nicht gebaut): Mod-Config `web.publicUrl`, damit `/simplelivemap link` gleich die
  Wiki-Adresse zeigt (nur relevant für Modus `token`).
