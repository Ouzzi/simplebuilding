# Simple Maps: Webkarte (Stufe 1, statischer Export)

Konzept: `docs/ai/KONZEPT-WEBKARTE-2026-10-19.md`, Plan: `docs/ai/PLAN-B-WEBMAP-2026-10-19.md`.
Code: `modules/simplemaps/shared/java/com/simplemaps/web/`, Ansicht `modules/simplemaps/shared/resources/assets/simplemaps/web/index.html`.

## Was die Mod tut
- Schreibt auf Befehl (`/simplemaps web export`, Rechtestufe 2) oder periodisch einen Ordner:
  `index.html` (eigene Canvas-Ansicht, kein Fremdcode, kein CDN), `markers.json`, `tiles/<ns>_<dim>/<z>/<x>/<y>.png`.
- Zoom 4 = 1 Block je Pixel, Kachel 128 × 128 Blöcke; Zoom 0 = 16 Blöcke je Pixel. `y` ist die Block-Z-Kachel.
- Nur Erkundetes (Vereinigung aller Wegfinder-Karten der Dimension), Vanilla-Kartenfarben. Geladene Chunks werden für die
  erkundeten Pixel frisch gelesen, nie wird ein Chunk geladen. Gedrosselt: `webChunksPerTick` je Server-Tick.
- Öffnet **keinen Port**. Ausliefern macht ein normaler Webserver.

## Config (`config/simplemaps-server.json`)
| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `webEnabled` | `false` | Export erlaubt (Befehl und periodisch) |
| `webExportDir` | `""` | leer = `<world>/simplemaps-web`; relativ = relativ zum Weltordner; absolut erlaubt |
| `webChunksPerTick` | `32` (1–512) | Chunks je Tick; eine Pyramiden-Kachel zählt 16 |
| `webIntervalMinutes` | `0` (0–1440) | automatischer Export alle n Minuten, 0 = aus |
| `webHideClaims` | `false` | Chunks leer lassen, in denen ein Schutz (framework `Protection`, z. B. Simple-Tweaks-Claims) Fremden das Bauen verbietet |
| `webShops` | `false` | Shops (Ebene `shops` von Simple Shops) in `markers.json`: Dimension, x/z, Angebot/Preis/Besitzername, `icon` `money_bill` (Simple Money geladen, Bild `icons/money_bill.png`) sonst `diamond`; nur erkundete Kacheln, mit `webHideClaims` nie in geschützten Chunks |
| `webAutoOffWith` | `bluemap, dynmap, squaremap, pl3xmap, journeymap` | einer geladen → Export aus (Logzeile); leere Liste hält ihn an |

Spieler erscheinen nur nach eigenem Opt-in: `/simplemaps web show` bzw. `hide` (gespeichert in der Welt,
`simplemaps:web_players`). `markers.json` enthält Name, Dimension, x/z dieser Spieler – keine UUIDs, keine Wegpunkte.
Status: `/simplemaps web status`.

## Auslieferung (nur beschrieben, nicht eingerichtet)
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

## Folgestufen (offen)
2. optionaler HTTP-Server (Port/Token, nur 127.0.0.1 Standard), Live-Spieler.
3. Wegpunkte (nur freigegebene), mehrere Dimensionen-Optionen, Claim-Verpixeln, Dirty-Chunks statt Vollexport.
4. 3D (BlueMap-Anbindung oder Eigenbau) – Besitzer-Entscheidung.
