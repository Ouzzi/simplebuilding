# Simple Maps: Webkarte (Stufe 1 statischer Export, Stufe 2 eingebauter Server + Live, Stufe 3 Wegpunkte/Dirty/Claims)

Konzept: `docs/ai/KONZEPT-WEBKARTE-2026-10-19.md`, Pläne: `docs/ai/PLAN-B-WEBMAP-2026-10-19.md` (Stufe 1),
`docs/ai/PLAN-A-WEBMAP2-2026-10-23.md` (Stufe 2, mit Sicherheitsbegründung), `docs/ai/PLAN-A-WEBMAP3-2026-10-23.md` (Stufe 3).
Code: `modules/simplemaps/shared/java/com/simplemaps/web/`, Ansicht `modules/simplemaps/shared/resources/assets/simplemaps/web/index.html`.

## Was die Mod tut
- Schreibt auf Befehl (`/simplemaps web export`, Rechtestufe 2) oder periodisch einen Ordner:
  `index.html` (eigene Canvas-Ansicht, kein Fremdcode, kein CDN), `markers.json`, `tiles/<ns>_<dim>/<z>/<x>/<y>.png`.
- Zoom 4 = 1 Block je Pixel, Kachel 128 × 128 Blöcke; Zoom 0 = 16 Blöcke je Pixel. `y` ist die Block-Z-Kachel.
- Nur Erkundetes (Vereinigung aller Wegfinder-Karten der Dimension), Vanilla-Kartenfarben. Geladene Chunks werden für die
  erkundeten Pixel frisch gelesen, nie wird ein Chunk geladen. Gedrosselt: `webChunksPerTick` je Server-Tick.
- Öffnet standardmäßig **keinen Port**. Ausliefern macht ein normaler Webserver – oder, nur wenn eingeschaltet, der
  eingebaute Server (Stufe 2, unten).

## Config (`config/simplemaps-server.json`)
| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `webEnabled` | `false` | Export erlaubt (Befehl und periodisch) |
| `webExportDir` | `""` | leer = `<world>/simplemaps-web`; relativ = relativ zum Weltordner; absolut erlaubt |
| `webChunksPerTick` | `32` (1–512) | Chunks je Tick; eine Pyramiden-Kachel zählt 16 |
| `webIntervalMinutes` | `0` (0–1440) | automatischer Export alle n Minuten, 0 = aus |
| `webHideClaims` | `false` | Chunks leer lassen, in denen ein Schutz (framework `Protection`, z. B. Simple-Tweaks-Claims) Fremden das Bauen verbietet |
| `webClaimStyle` | `hide` | mit `webHideClaims`: `hide` = geschützter Chunk leer, `pixelate` = jedes erkundete Pixel des Chunks in seiner häufigsten Farbe (grobes Gelände, keine Bauten); Unbekanntes → `hide` |
| `webWaypoints` | `false` | freigegebene Wegpunkte in `markers.json` (Stufe 3, unten) |
| `webShops` | `false` | Shops (Ebene `shops` von Simple Shops) in `markers.json`: Dimension, x/z, Angebot/Preis/Besitzername, `icon` `money_bill` (Simple Money geladen, Bild `icons/money_bill.png`) sonst `diamond`; nur erkundete Kacheln, mit `webHideClaims` nie in geschützten Chunks |
| `webAutoOffWith` | `bluemap, dynmap, squaremap, pl3xmap, journeymap` | einer geladen → Export aus (Logzeile); leere Liste hält ihn an |
| `webHttpEnabled` | `false` | eingebauter Server (Stufe 2); läuft nur zusammen mit `webEnabled` und ohne autoOffWith-Mod |
| `webHttpBind` | `127.0.0.1` | Bind-Adresse; nur dieser Rechner. `0.0.0.0` = alle Netze (dann Token oder Proxy davor!) |
| `webHttpPort` | `8124` (1–65535) | Port |
| `webHttpToken` | `""` | leer = ohne; sonst nötig als `?token=…` (Ansicht reicht es weiter) oder `Authorization: Bearer …`; nur Buchstaben/Ziffern verwenden |
| `webHttpRequestsPerMinute` | `1200` (60–20000) | Anfragen je IP und Minute, darüber 429 |
| `webLiveSeconds` | `2` (1–60) | Takt von `live.json` (Spieler live) |

Spieler erscheinen nur nach eigenem Opt-in: `/simplemaps web show` bzw. `hide` (gespeichert in der Welt,
`simplemaps:web_players`). `markers.json` enthält Name, Dimension, x/z dieser Spieler – keine UUIDs (Wegpunkte nur freigegebene, Stufe 3).
Status: `/simplemaps web status`.

## Stufe 2: eingebauter Server + Live (Standard aus)
Einschalten: `webEnabled` **und** `webHttpEnabled` auf `true`, Server neu starten oder warten (Abgleich jede Sekunde; geänderte
Werte binden neu). Dann `http://127.0.0.1:8124/` im Browser des Server-Rechners. `/simplemaps web status` nennt die Adresse.

- **Endpunkte (nur diese, sonst 404):** `/` bzw. `/index.html` (Ansicht; aus dem Jar, solange noch nicht exportiert),
  `/markers.json`, `/tiles/<dim>/<0-4>/<x>/<y>.png`, `/icons/<name>.png` aus dem Exportordner und `/live.json`
  (`{format, generatedAt, refreshSeconds, exportedAt, players[]}`).
- **Live:** `live.json` baut der Server-Thread alle `webLiveSeconds` (nur Spieler mit `/simplemaps web show`, keine
  Zuschauer, keine UUIDs). Die Ansicht fragt ihn in diesem Takt ab; ändert sich `exportedAt` (neuer Export, z. B. per
  `webIntervalMinutes`), lädt sie `markers.json` und die Kacheln neu (`?v=<Stand>`). Neue Kacheln entstehen weiter nur
  durch den Export; Dirty-Chunks sind Stufe 3. Statisch ausgeliefert fehlt `live.json` – die Ansicht bleibt dann beim
  Minuten-Abruf von `markers.json` (leert den Kachel-Cache nur noch bei neuem Export).
- **Sicherheit:** nur `GET`/`HEAD` (sonst 405), kein Endpunkt ändert etwas, keine Befehle über HTTP. Feste Weißliste der
  Pfade, `%`/`\`/`..`/`//` abgelehnt (keine Dekodierung), Ziel muss – auch nach Auflösen von Links – im Exportordner liegen,
  Dateien ≤ 4 MiB. Ratenbegrenzung je IP, 4 Arbeits-Threads + kurze Warteschlange, 5 s Lese-Timeout, kurze Anfragezeilen,
  eine Anfrage je Verbindung. Header `nosniff`, `no-referrer`, strenge CSP. Bind-Fehler → WARN, neuer Versuch nach 60 s.
  Nicht-Loopback ohne Token → WARN im Log. Endet der Spielserver, schließt sich der Port binnen einer Sekunde.
- **Öffentlich machen:** besser Reverse-Proxy (nginx/Caddy, TLS, Cloudflare Access) auf `127.0.0.1:8124` als den Port direkt
  freizugeben. Achtung: ein Link mit `?token=` gibt das Token mit weiter.

```nginx
location / { proxy_pass http://127.0.0.1:8124; }   # Server-Rechner, webHttpBind bleibt 127.0.0.1
```

## Stufe 3: nur Geändertes, Claims verpixeln, Wegpunkte
- **Dirty-Kacheln:** Der erste Export einer Serversitzung rendert alles. Danach merkt sich die Mod je 1:1-Kachel eine
  Signatur (welche Kartenkacheln + deren Pixelstand + Claim-Maske der 64 Chunks) und welche Dateien es gibt. Der nächste
  Export (Befehl oder `webIntervalMinutes`) prüft jede erkundete Kachel (eine Budget-Einheit, kein Chunk wird gelesen) und
  rendert nur neue Kacheln, Kacheln mit geänderten Kartenpixeln, mit Blockänderung (Mixin auf
  `ServerLevel.sendBlockUpdated`, nur bei `webEnabled`) oder geänderter Claim-Lage – dazu ihre Pyramiden-Eltern.
  Voll wird exportiert nach Neustart, mit anderem Ordner/Claim-Modus, nach Abbruch, ab 262144 markierten Kacheln oder mit
  `/simplemaps web export full`. Blockänderungen ohne Client-Update (seltene Flags, Weltgenerierung) erscheinen erst beim
  nächsten Vollexport.
- **Claims verpixeln:** `webHideClaims` + `webClaimStyle: "pixelate"`. „Fremd“ heißt wie bisher: der Schutz verbietet
  Unbeteiligten das Bauen. Shops und Wegpunkte in geschützten Chunks fehlen in beiden Modi.
- **Wegpunkte (Standard aus):** Server-Schalter `webWaypoints`; zusätzlich muss jeder Spieler selbst freigeben:
  `/simplemaps web waypoints share` mit einer Wegfinder-Karte in der Hand speichert einen **Schnappschuss** ihrer Wegpunkte
  in der Welt (`simplemaps:web_waypoints`); nach Änderungen erneut `share`. `/simplemaps web waypoints unshare` nimmt die
  gehaltene Karte zurück, ohne Karte alle eigenen Freigaben. Inventare werden nie automatisch ausgelesen.
  `markers.json` → `waypoints[]`: `dimension`, `x`, `z`, `name`, `color` (`#rrggbb`), `icon` (Item-Id) – nur auf erkundeten
  Kacheln, nie in verborgenen Claims, ohne Spielernamen/UUID, höchstens 4096. Die Ansicht zeigt sie als Quadrat in
  Wegpunktfarbe mit Namen (Schalter „Wegpunkte“).

## Auslieferung statisch (nur beschrieben, nicht eingerichtet)
Der Ordner ist rein statisch. Beispiel nginx (z. B. auf CT sb-web, Ordner per rsync/Freigabe vom Spielserver holen):

```nginx
server {
    listen 80;
    server_name map.example.org;
    root /srv/simplemaps-web;          # Kopie von <world>/simplemaps-web
    location / { try_files $uri =404; }
    location = /markers.json { add_header Cache-Control "no-cache"; }
    location /tiles/ { add_header Cache-Control "public, max-age=300"; }
}
```

- Synchronisieren, z. B. per Cron alle 5 min: `rsync -a --delete <server>/world/simplemaps-web/ sb-web:/srv/simplemaps-web/`.
- Zugriffsschutz: Cloudflare Access (wie beim Wiki) oder `auth_basic` in nginx; die Karte zeigt Erkundetes der Welt.
- `file://` geht in den meisten Browsern nicht (Fetch von `markers.json`); immer über HTTP öffnen.
- Alte Kacheln gelöschter Karten bleiben liegen: Ordner leeren und neu exportieren.

## Lizenz
Ansicht ist eigener Code (CC0 wie die Mod). Leaflet (BSD-2-Clause) wäre eine Alternative für spätere Stufen; dann
Lizenztext mitliefern.

## Folgestufen
2. erledigt (`a-webmap2`, 23.10.): eingebauter Server (Port/Token, Standard 127.0.0.1, aus), Live-Spieler über `live.json`.
3. erledigt (`a-webmap3`, 23.10.): Wegpunkte (nur freigegebene), Claim-Verpixeln, Dirty-Kacheln statt Vollexport.
   Offen: Dimensionen-Auswahl (`dimensions`-Option des Konzepts).
4. 3D (BlueMap-Anbindung oder Eigenbau) – Besitzer-Entscheidung.
