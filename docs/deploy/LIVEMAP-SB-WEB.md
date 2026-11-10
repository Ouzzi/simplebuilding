# Simple Livemap unter wiki.simplemods.de/map/ (N14b)

Anleitung für A (Einspielen nur mit Besitzer-OK). Hintergrund, Varianten und Begründung:
`docs/ai/PLAN-B-LIVEMAP16-2026-11-09.md`. Skripte: `tools/deploy/livemap/`.

**Kurz:** Der Spielserver behält seinen eingebauten Livemap-Webserver (nur im LAN, nur für den Web-Host erreichbar,
privat mit Token). nginx auf dem Web-Host (sb-web) leitet `/map/` dorthin weiter, hinter dem vorhandenen Wiki-Login,
setzt den Token selbst und cacht Kacheln kurz (Spielserver schläft → letzter Stand bleibt sichtbar).

Platzhalter (echte Werte nur in der Memory, nie ins Repo): `GAMEHOST` = LAN-Adresse der Spielserver-VM,
`WEBHOST` = LAN-Adresse des Web-Hosts, `SERVERDIR` = Ordner des Minecraft-Servers.

## Voraussetzungen

- Branch mit `tools/deploy/livemap/` ist in `claude-wave1` (= `/srv/wiki-dev` auf dem Web-Host, Sync jede Minute)
  **und** `tools/zentrale-web/nginx/wiki-extra.conf` enthält die Livemap-Include-Zeile (sonst entfernt das nächste
  `deploy_dom07.sh domains` die `/map/`-Route wieder; `check` warnt).
- Simple Livemap ist auf dem Spielserver installiert (`SERVERDIR/mods/simplelivemap-*.jar`).
- Platte des Web-Hosts unter 90 % (`df -h /`), Cache ist auf 256 MB gedeckelt.

## Einspielen

1. **Spielserver: Mod-Config** (`SERVERDIR/config/simplelivemap.json`, Gruppe `web`; Besitzer bzw. A mit Zugang):
   `"enabled": true`, `"bind": "GAMEHOST"` (LAN-Adresse der VM, **nicht** `0.0.0.0`), `"port": 8125`,
   `"visibility": "private"` (bleibt privat, auch für die öffentliche Variante), `"requestsPerMinute": 6000`
   (hinter dem Proxy kommen alle Anfragen von einer Adresse; das Limit je Besucher macht nginx),
   `"showPlayers"` nach Besitzer-Wunsch (Standard `false`). Die Config wird beim Start gelesen → Server neu starten.
   Im Log muss stehen: `Simple Livemap: web map on http://GAMEHOST:8125/ (private)`.
2. **Spielserver: Firewall** – Port 8125 nur für den Web-Host (Beispiel nftables, auf der Spielserver-VM als root):
   ```
   nft add table inet livemap
   nft add chain inet livemap input '{ type filter hook input priority 0; policy accept; }'
   nft add rule inet livemap input tcp dport 8125 ip saddr WEBHOST accept
   nft add rule inet livemap input tcp dport 8125 drop
   ```
   Dauerhaft: dieselben Regeln in `/etc/nftables.conf` (bzw. die vorhandene Firewall der VM nutzen).
3. **Web-Host: Vorprüfung** (als root, ändert nichts):
   ```
   cd /srv/wiki-dev/tools/deploy/livemap
   ./deploy_livemap.sh check --upstream GAMEHOST:8125 < /dev/null
   ```
   Erwartet: `Spielserver-Livemap erreichbar, privat (401 ohne Token) – gut`. Bei `nicht erreichbar`: Schritt 1/2 prüfen.
4. **Trockenlauf** (zeigt die Dateien, die entstehen würden, ändert nichts):
   `./deploy_livemap.sh install --upstream GAMEHOST:8125 --dry-run < /dev/null`
5. **Installieren** – Token nur über stdin (nie als Argument, nie ausgeben). Der Token steht in der Mod-Config des
   Spielservers unter `web.token`; z. B. vom Spielserver aus direkt durchreichen:
   ```
   ssh root@GAMEHOST "python3 -c 'import json;print(json.load(open(\"SERVERDIR/config/simplelivemap.json\"))[\"web\"][\"token\"])'" \
     | ssh root@WEBHOST '/srv/wiki-dev/tools/deploy/livemap/deploy_livemap.sh install --upstream GAMEHOST:8125'
   ```
   (oder auf dem Web-Host `read -rs T; printf '%s\n' "$T" | ./deploy_livemap.sh install --upstream GAMEHOST:8125; unset T`
   und den Token einfügen). Das Skript prüft den Token gegen den Spielserver **bevor** es ihn ablegt
   (`/etc/nginx/simplemods-livemap.secret`, 600), schreibt `/etc/nginx/conf.d/simplemods-livemap.conf` und
   `/etc/nginx/snippets/simplemods-livemap.conf`, fügt die Include-Zeile in den Server `wiki.simplemods.de` ein
   (Sicherung `sites-available/simplemods.bak-livemap`), `nginx -t` (bei Fehler wird alles zurückgesetzt), reload,
   dann Smoke. Ende: `Smoke grün`.
6. **Prüfen von außen** (beliebiger Rechner, siehe „Prüfen“), dann im Browser mit Wiki-Login
   `https://wiki.simplemods.de/map/` öffnen: Karte, Dimensionsauswahl, Konsole ohne Fehler.
7. Memory/Hosting-Notiz aktualisieren (Upstream-Adresse, Modus, Datum).

**Modus wechseln:** `--mode login` (Standard, hinter Wiki-Login), `--mode public` (ohne Login, nur nach
Besitzer-Entscheidung; nginx setzt weiter den Token, die Mod bleibt privat), `--mode token` (ohne Wiki-Login,
Besucher brauchen den Link `https://wiki.simplemods.de/map/#t=<token>`; ohne Cache). Einfach `install` mit dem
anderen `--mode` erneut aufrufen (ohne stdin bleibt der Token, er wird erneut geprüft). Hinweis: `/simplelivemap link`
im Spiel zeigt die LAN-Adresse; für den Modus `token` den Teil vor `#t=` durch `https://wiki.simplemods.de/map/` ersetzen.

**Token gewechselt** (neuer Token in der Mod-Config + Neustart): Schritt 5 erneut mit dem neuen Token. Bis dahin
liefert `/map/` gecachte Kacheln, `live.json` 401 → Karte zeigt „privat“.

## Prüfen

Auf dem Web-Host: `./deploy_livemap.sh smoke` (lokal gegen nginx :8081, plus Spielserver direkt mit/ohne Token).

Von außen (`curl` gibt es auf dem Web-Host nicht):
```
curl -sI https://wiki.simplemods.de/map           # 301, Location: /map/
curl -sI https://wiki.simplemods.de/map/          # login: 302 nach /api/auth/login?next=/map/
curl -sI https://wiki.simplemods.de/map/live.json # login: 302 (public: 200 application/json, Cache-Control: no-store)
curl -sI https://wiki.simplemods.de/map/x.txt     # login: 302, sonst 404 – erreicht den Spielserver nie
curl -s -o /dev/null -w '%{http_code}\n' -X POST https://wiki.simplemods.de/map/   # 403/302, nie 200
```
Mit Login (Browser, Entwicklerwerkzeuge → Netzwerk): `live.json` 200 alle 2 s, Kacheln 200/204 mit
`X-Cache: MISS`, beim zweiten Laden `HIT`; Antwort-Header ohne `Set-Cookie` vom Spielserver.
Cache-Größe: `du -sh /var/cache/nginx/simplelivemap` (max. 256 MB).
Spielserver schläft: Seite und bekannte Kacheln kommen weiter (`X-Cache: STALE`), `live.json` 502.

## Rückbau

`/srv/wiki-dev/tools/deploy/livemap/deploy_livemap.sh rollback` – entfernt Snippet, http-Teil, Token-Datei, Cache
und die eingefügte Include-Zeile, `nginx -t`, reload (Sekunden). Die Glob-Zeile in `wiki-extra.conf` bleibt und ist
ohne Datei wirkungslos. Spielserver: `web.enabled` auf `false` + Neustart, Firewall-Tabelle `nft delete table inet livemap`.
Notfall ohne Skript: `cp /etc/nginx/sites-available/simplemods.bak-livemap /etc/nginx/sites-available/simplemods`,
`rm /etc/nginx/conf.d/simplemods-livemap.conf /etc/nginx/snippets/simplemods-livemap.conf`, `nginx -t && systemctl reload nginx`.

## Sicherheit

- **Token nie in URLs/Logs:** Im Modus `login`/`public` kennt der Browser den Token gar nicht; nginx setzt den Header
  aus einer Root-Datei (600). Im Modus `token` steht er nur im Fragment `#t=…`, das Browser nie senden; er geht nur als
  `Authorization`-Header, den nginx im Standard-Logformat nicht protokolliert. Das Skript liest den Token nur von stdin
  und gibt ihn nie aus. **Nicht `nginx -T` ungefiltert laufen lassen** (druckt alle Config-Dateien inkl. Token-Datei);
  stattdessen `nginx -t` bzw. `nginx -T 2>/dev/null | grep -v -i bearer`.
- **Nur lesend:** nginx lässt nur `GET`/`HEAD` durch (`limit_except`), nur die drei Routen der Mod (`/`, `live.json`,
  Kachel-Muster); alles andere beantwortet nginx selbst mit 404. Die Mod selbst kennt ebenfalls nur `GET`/`HEAD`.
  Wiki-Session-Cookie und Identitäts-Header werden nicht zum Spielserver weitergereicht; `Set-Cookie` vom
  Spielserver wird verworfen.
- **Ratenlimit:** nginx je Besucher (CF-Connecting-IP) 10 Anfragen/s, burst 80, sonst 429; die Mod begrenzt zusätzlich
  gesamt (`requestsPerMinute`) und hat 4 Worker + Warteschlange 32. Der Cache-Schlüssel enthält keine Query, Cache-Busting
  (`?x=…`) erreicht den Spielserver also nicht.
- **Netz:** Der Livemap-Port ist nur im LAN gebunden und per Firewall nur für den Web-Host offen; ins Internet kommt
  man nur über den Cloudflare-Tunnel und die Allowlist des Web-Hosts. Die Mod-Sichtbarkeit bleibt `private`
  (Verteidigung in der Tiefe: ohne Token liefert der Port im LAN keine Daten).
- **Spieler-Daten:** `showPlayers` ist Standard aus; an zeigt `live.json` nur Name, Dimension, x, z (keine UUID).
  Vor `--mode public` mit `showPlayers: true` Besitzer fragen (Datenschutz).
- **Ressourcen:** nginx-Cache max. 256 MB, 1 Tag inaktiv; Spielserver sieht höchstens eine Anfrage je Kachel je 30 s
  und `live.json` höchstens 1/s, egal wie viele Besucher.

## Variante B (Rückfall): statische Kopie

Nur, wenn der Livemap-Port im LAN nicht geöffnet werden soll. Keine Spieler, Kacheln so frisch wie der Timer (5 min),
volle Kachelkopie auf der knappen Platte des Web-Hosts (Wächter: Abbruch ab 85 %).
1. `apt install rsync` auf beiden Seiten. Web-Host: `ssh-keygen -t ed25519 -N '' -f /root/.ssh/livemap_sync`.
   Spielserver, `authorized_keys` des Server-Nutzers:
   `command="rrsync -ro SERVERDIR/world/simplelivemap/tiles",restrict ssh-ed25519 AAAA… livemap-sync`.
   Einmal von Hand verbinden (Host-Schlüssel bestätigen): `ssh -i /root/.ssh/livemap_sync USER@GAMEHOST`.
2. `cp tools/deploy/livemap/livemap-sync.sh /usr/local/bin/`, `livemap-sync.default.example` → `/etc/default/livemap-sync`
   (600, Werte eintragen), `livemap-sync.service`/`.timer` → `/etc/systemd/system/`, `mkdir -p /srv/livemap`
   (muss vor dem ersten Start existieren, `ReadWritePaths`).
3. `livemap-sync.sh --dry-run` (mit `set -a; . /etc/default/livemap-sync; set +a`), dann `livemap-sync.sh`, dann
   `systemctl daemon-reload && systemctl enable --now livemap-sync.timer`.
4. `./deploy_livemap.sh install --source static --mode login < /dev/null`, Prüfen wie oben (ohne Spielserver-Teil).
Rückbau: `systemctl disable --now livemap-sync.timer`, Units + `/usr/local/bin/livemap-sync.sh` + `/etc/default/livemap-sync`
löschen, `rm -rf /srv/livemap`, `deploy_livemap.sh rollback`, Schlüssel-Zeile auf dem Spielserver entfernen.

## Lokal testen (ohne Server)

`NGINX_BIN=<nginx 1.26> tools/deploy/livemap/test_render.sh` rendert alle Modi, `nginx -t` je Modus und ein
Funktionstest gegen einen nachgebauten Livemap-Server (Login, Token, Cache, Routenfilter). Ein nginx-Binary ohne
Installation: `apt-get download nginx && dpkg -x nginx_*.deb x && NGINX_BIN=x/usr/sbin/nginx …`.
