# PLAN A – Simple Livemap als Dienst, Phase 1 (N34, a-livemap34, 12.11.2026)

Auftrag: `/root/b-auftrag-20261109.md` „## N34“ + Besitzer-Zusätze 1/3/6/7; Spezifikation (alle 8 Abschnitte ok)
`/root/sb-tools-a/previews/a-livemap34-preview.html`; Konzept-Vorlauf `docs/ai/KONZEPT-B-LIVEMAP-HOSTED-2026-11-11.md`.
Branch `a-livemap34` (Basis hub/claude-wave1 388f19991). **Nichts deployen** (kein sb-web/CT 109, kein DNS/Cloudflare/Stripe).

## 1. Ist-Stand (erhoben)

- **Mod** `modules/simplelivemap`: Kacheln 128×128 PNG, Zoom 0–4 unter `<welt>/simplelivemap/tiles/<dim>/<z>/<x>/<y>.png`
  (`TileStore`, ein Hintergrund-Thread, atomares Schreiben), `visited.bin`, eingebauter Nur-Lese-Webserver `LivemapWeb`
  (eigener ServerSocket, 3 Routen, Bearer-Token, Ratenlimit, CSP), Viewer `assets/simplelivemap/web/index.html`
  (relativ: `tiles/…`, `live.json`, Schlüssel im `#t=`), Befehle `status|link|hint|visibility|hide|show`, Config
  `config/simplelivemap.json` (general/record/render/web), N29: Port-Ausweichen 8125–8134. Gametests `LivemapTests`.
- **Login heute** `tools/zentrale-web/auth.py`: PBKDF2-SHA256 600 000 Runden (`pbkdf2_sha256$it$salt$hash`), signiertes
  Cookie, Sperre 5 Fehlversuche/15 min, nginx `limit_req`, Origin-Prüfung; Dienst `devqueue.py` = Python-Standardbibliothek
  (`ThreadingHTTPServer`). Deploy-Muster `deploy_zw06.sh`/`deploy_dom07.sh` (check/…/rollback, `nginx -t`, Sicherung).
- **Landing** `tools/landing/` (Minecraft-Pixel-Stil, Farben #1d1a16/#55a630/#ffd75e, Karten `.c`, Knöpfe `.btn`).
- **Selbst-Hosten** `docs/deploy/LIVEMAP-SELF-HOST.md`, sb-web-Variante `tools/deploy/livemap/` (N14b).
- sb-web: 1 GB RAM, kleine Platte, kein pip → Dienst nur mit Standardbibliothek (Python ≥ 3.10, sqlite3).

## 2. Entscheidungen (selbst getroffen, begründet)

| # | Entscheidung | Grund |
|---|---|---|
| D1 | Ein Python-Dienst `tools/livemap-web/smweb` (nur Standardbibliothek: `http.server`, `sqlite3` WAL, `hashlib`, `hmac`), bedient **beide** Hosts per `Host`-Kopf: `account.simplemods.de` (Konto) und `livemap.simplemods.de` (Karten). Ein Port hinter nginx. | wie devqueue, kein pip auf sb-web, eine DB „für alle Simple-Mods-Seiten“ (Spez. 5) |
| D2 | Sitzungen serverseitig (Tabelle `sessions`, Token 256 bit, in der DB nur SHA-256), Cookie `__Secure-sm_session`, `Domain=simplemods.de` (Spez. 5: „Sitzung für alle *.simplemods.de“), HttpOnly, Secure, SameSite=Lax, 30 Tage. Passwortwechsel/„überall abmelden“ löscht Sitzungen. | widerrufbar, kein Geheimnis im Cookie |
| D3 | Passwort PBKDF2-SHA256 600 000 Runden, **gleiches Format wie `auth.py`** (Wiki-Login zieht später um, Hashes übernehmbar). Mindestlänge 10, max. 256, nicht gleich E-Mail, kleine Liste häufiger Passwörter. argon2 bräuchte pip → nein. | OWASP-Wert, Migration |
| D4 | CSRF: alle POST-Formulare mit Token (angemeldet: HMAC(Geheimnis, Sitzungs-ID); vor Login: Double-Submit-Cookie `sm_csrf`) **und** Origin/Referer-Prüfung gegen die konfigurierten Hosts. Mod-API nur mit Bearer-Schlüssel (keine Cookies). | doppelt abgesichert |
| D5 | Ratenlimits im Speicher (gleitende Fenster): Login 5 Fehler/15 min je Adresse **und** je E-Mail; Registrieren 5/h je Adresse; Passwort vergessen 3/h je E-Mail, 10/h je Adresse; Code einlösen 10/min + 20 Fehler/h je Adresse; Upload je Kartenschlüssel nach Stufe. Adresse = `X-Client-IP` von nginx (CF-Connecting-IP). Dazu nginx `limit_req`. | Brute-Force, Missbrauch |
| D6 | E-Mail-Versand: SMTP aus der Server-Config (außerhalb Repo); ohne SMTP schreibt der Dienst Mails in `<data>/outbox/` (Dev), Admin sieht sie. Bestätigungslink 24 h, Reset-Link 1 h, Tokens nur gehasht. | SMTP-Konto fehlt noch (TODO Besitzer) |
| D7 | Stufen aus `tools/livemap-web/tiers.json` (Zahlen der Spezifikation 4), Server-Config kann überschreiben. Rolle `developer`: alles unbegrenzt, „sofort fertig“, Admin. | Spez. 4/5 |
| D8 | **Erste 2 Verbindungen eines Kontos (Lebenszeit) ohne Mindestwartezeit** (Zähler `connections_total` am Konto). | Besitzer-Zusatz 3 |
| D9 | „Karte wird vorbereitet“: fertig, wenn Erst-Upload vollständig (Manifest gemeldet, nichts mehr fehlt) **und** Mindestzeit der Stufe vorbei. Fortschritt = min(Upload-Anteil, Zeit-Anteil). Warteschlange: höchstens `prep_slots` (Standard 4) Karten laden gleichzeitig hoch, Vorrang nach Stufe, dann Reihenfolge; Upload-Rate je Stufe (Kacheln/min); jede Antwort sagt der Mod `wait` (Sekunden). | Spez. 3 |
| D10 | Kacheln inhaltsadressiert gespeichert (`blobs/ab/<sha256>.png`, dedupliziert) + Index in SQLite; ausgeliefert unter `/m/<name>/tiles/…png` mit `ETag` = Prüfsumme und `max-age` = Kachel-Takt der Stufe (60–900 s). **Abweichung:** keine Prüfsummen-URLs im Viewer (Viewer bleibt identisch mit der Mod); Cloudflare cacht trotzdem. Prüfsummen-URLs mit dem R2-Umzug (Bezahlstart). | „derselbe Viewer“, Spez. 7 |
| D11 | Viewer = **dieselbe Datei** aus der Mod (`modules/simplelivemap/.../web/index.html`), vom Dienst direkt aus dem Checkout geliefert. Einmal für beide erweitert: Spieler an/aus, Spieler folgen, Koordinaten-Sprung, Vollbild, Einstellungsfenster (Ansicht, im Browser gemerkt), Handy. Server-Einstellungen bleiben beim Self-Host im Spiel (`/simplelivemap config`) – der Mod-Webserver bleibt **nur lesend** (kein Schreib-Endpunkt). | Spez. 1/2, Sicherheit |
| D12 | Sichtbarkeit `private` (Besitzer + geteilte Konten), `link` (geheimer Ansichtsschlüssel im `#t=`), `public` (alle). Öffentlich zählt gegen „davon öffentlich“. `/explore` = Platzhalter „kommt später“ (Spez. 8). | Spez. 2/4/8 |
| D13 | Free-Platzsperre: jeder Platz merkt seine letzte Belegung; neu belegen erst nach 7 Tagen (erste Belegung frei). | Spez. 3 |
| D14 | Trennen → Puffer (Daten bleiben, Ablauf nach Stufen-Tagen, Anzahl begrenzt: älteste fällt raus). Verbinden mit Welt-Fingerabdruck eines Puffer-Eintrags desselben Kontos → Karte wird wiederhergestellt, Manifest-Abgleich lädt nur Abweichungen. Wieder verbinden = immer „wird vorbereitet“. | Spez. 3 |
| D15 | Herabstufen: überzählige Karten (neueste zuerst) 30 Tage in den Puffer, überzählige öffentliche → `link`. | Spez. 4 |
| D16 | Aufrufe = Viewer-Seitenaufrufe je Konto und Tag. Über dem Limit nichts gesperrt: längere Cache-Zeiten (Kacheln 1 h), Live-Takt ×4, Hinweis im Dashboard. | Spez. 4 |
| D17 | Speicher: Gesamt-Kontingent in der Config (`storage_quota_gb`, Standard 20), je Karte nach Stufe (Free 64 MB, Pro 256 MB, Max 1 GB, Ultra 4 GB – Spez. nennt nur „begrenzt“), Upload-Stopp ab 90 % Platte. | Besitzer-Zusatz 7 |
| D18 | Stripe vorbereitet, **aus**: `billing.json` (Produkte/Preise je Stufe × Monat/Jahr, IDs als Platzhalter), Checkout/Portal antworten „coming soon“, Webhook prüft `Stripe-Signature` (HMAC-SHA256, Toleranz 300 s, mehrere v1) – bei `enabled=false` 503. Geheimnisse nur in `/etc/simplemods/livemap-web.json` (600). | Spez. 6 |
| D19 | **Self-Host strikt eine Karte** (Besitzer-Zusatz 1): Sperrdatei `config/simplelivemap-web.lock` je Installation (OS-Dateisperre, Halter-Info Welt-ID/Port/PID), Port-Erkennung (Antwortkopf `X-Simple-Livemap`; belegt den Port schon eine Livemap → **kein** Ausweichen, Web aus + Logzeile), Welt-ID `<welt>/simplelivemap/world.json` (Zufalls-UUID, kein Seed). Multi-Welt-Proxys nicht unterstützt (Doku). Das N29-Ausweichen bleibt nur für fremde Programme. | Zusatz 1 |
| D20 | Mod: `/simplelivemap connect <code>` (Spielleiter oder Einzelspieler-Besitzer wie `link`), `/simplelivemap disconnect`; Zugangsdaten **je Welt** in `<welt>/simplelivemap/hosted.json` (nie in der Config, nie im Log), Hochladen in einem Hintergrund-Thread (Manifest → fehlende Kacheln als ZIP-Bündel → danach nur Änderungen; `live.json` im Stufen-Takt). Endpunkt `hosted.endpoint` (Standard `https://livemap.simplemods.de`, http nur für Loopback). | Spez. 3/7 |
| D21 | Rechtstext-Entwürfe nur in `docs/legal/` (nicht unter `tools/livemap-web/static`, nicht ausgeliefert). | Zusatz 6 |
| D22 | Deploy als fertiges Skript `tools/livemap-web/deploy/deploy_livemap_web.sh` (check/install/smoke/rollback) + systemd-Unit + nginx-Vorlage; **nicht ausgeführt**, TODO in der Übergabe. | Auftrag |

## 3. Mod-API v1 (Vertrag Dienst ↔ Mod)

Basis `<endpoint>/api/v1`, JSON UTF-8, Antwort immer mit `wait` (Sekunden bis zum nächsten Kachel-Bündel).
- `POST /connect` `{code, world, mod, mc}` → `{map, name, key, viewer, state, limits:{liveSeconds, tileSeconds, batchBytes, batchTiles}}`;
  Fehler 400 (Format), 404 (unbekannt/abgelaufen/benutzt – gleiche Meldung), 409 (kein Platz mehr), 429.
- `GET /status` (Bearer) → `{state, progress, wait, limits, viewer}`.
- `POST /manifest` (Bearer) `{tiles:{"<dim>/<z>/<x>/<y>": "<sha256>"} ≤ 5000, done:bool}` → `{need:[…], wait}`; `done` beendet die Erhebung
  (Index-Einträge, die nicht gemeldet wurden, fallen weg).
- `POST /tiles` (Bearer, `application/zip`, Einträge `<dim>/<z>/<x>/<y>.png`) → `{stored, wait, state, progress}`; geprüft: Name,
  ≤ 256 KiB je Eintrag, PNG-Signatur + IHDR 128×128, Anzahl/Bytes je Stufe, keine Duplikate, entpackt ≤ 4× Körper.
- `PUT /live` (Bearer, ≤ 16 KiB) → nur `dimensions[{id,folder}]`, `players[{name,dimension,x,z}]` übernommen; Spieler nur, wenn die
  Karte „Spieler zeigen“ an hat (Standard aus).
- `POST /disconnect` (Bearer) → Karte in den Puffer, Schlüssel ungültig.
Fehler: 401 (Schlüssel unbekannt), 410 (Karte getrennt → Mod vergisst Zugang), 413, 429 + `Retry-After`, 507 (Speicher).

## 4. Umfang / Dateien

1. `tools/livemap-web/` – `smweb/{config,db,security,accounts,maps,upload,billing,mail,pages,i18n,app,__main__}.py`,
   `static/{site.css,app.js}`, `tiers.json`, `billing.example.json`, `config.example.json`, `README.md`,
   `deploy/{deploy_livemap_web.sh,smweb.service,nginx-livemap.conf.in}`, `tests/` (Unit-Tests), `tests/harness.py` (lokaler Start
   mit Beispieldaten für Screenshots).
   Seiten: livemap `/`, `/pricing`, `/self-host`, `/explore`, `/m/<name>/` (Viewer + `live.json` + Kacheln), `/app`, `/app/connect`,
   `/app/m/<id>`, `/app/archive`, `/app/plan`, `/app/account`, `/admin`; account `/login`, `/register`, `/forgot`, `/reset`, `/verify`,
   `/logout`; livemap `/login|/register|/forgot` → 302 zum Konto-Host mit `next`.
2. Mod: `web/SelfHostLock.java`, `WorldId.java`, `hosted/{HostedConfig,HostedClient,HostedUploader}.java`, Änderungen
   `LivemapWeb` (Kopf + Probe + kein Ausweichen bei fremder Livemap), `LivemapRuntime` (Sperre, Welt-ID, Uploader),
   `LivemapCommands` (`connect`, `disconnect`, Status), `LivemapConfig` (Gruppe `hosted`: `endpoint`), `TileStore` (Änderungs-Hörer),
   Viewer-Erweiterung, Lang EN/DE, `wiki/manual.json`, Guides neu, Gametests (`selfHostLock`, `selfHostPortProbe`, `hostedConnect`,
   `hostedBundle`, `connectPermission`).
3. `docs/legal/` – Entwürfe Impressum, Datenschutzerklärung, AGB (Abo, Kündigung, Widerruf digitale Dienste), Liste
   Auftragsverarbeiter, `README.md` (≥ 2 Prüfrunden, Platzhalter `⟦…⟧`, „Besitzer muss eintragen/prüfen“, keine Rechtsberatung).
4. `docs/deploy/LIVEMAP-SELF-HOST.md`: Abschnitt „Eine Karte je Installation“. `docs/MODS-UND-FEATURES.md` Zeile Livemap.
5. `build.gradle`: `testLivemapWeb` (Unit-Tests des Dienstes im Gate, wie `testModtree`).

## 5. Risiken

- Kein Gradle hier, sb-test nicht erreichbar → Java nur per `javac` gegen das 26.3-Jar typgeprüft; Gametests als TODO (`/root/sbt`).
- Sicherheit eines selbst geschriebenen Web-Dienstes: deshalb kleine Angriffsfläche (nur Standardbibliothek, feste Routen, Größen-
  grenzen vor dem Lesen, parametrisierte SQL, Escaping aller Ausgaben, strenge CSP ohne Inline-Skripte außer dem Viewer).
- Domain-Cookie gilt für alle *.simplemods.de: nur eigene Hosts, keine Nutzer-HTML-Inhalte (Kacheln nur PNG mit `nosniff`).
- E-Mail ohne SMTP-Konto: Dev-Ausgang; Produktion braucht SMTP (Besitzer).

## 6. Prüfung

- `python3 -m unittest discover -s tools/livemap-web/tests -t tools/livemap-web` (Hashing, Sitzung, CSRF, Ratenlimit, Registrieren →
  Bestätigen → Login → Logout, Reset, Code 15 min/einmalig, Plätze/öffentlich, 7-Tage-Sperre, erste 2 ohne Wartezeit, Vorbereitung/
  Warteschlange, Upload-Prüfungen (Namen, PNG, Größe, ZIP-Bombe, Pfad), Manifest-Abgleich, Puffer/Wiederherstellen, Herabstufen,
  Aufrufe/Limit, Admin-Rechte, „sofort fertig“, Stripe-Signatur, Export/Löschen, Speicher-Kontingent, Viewer = Mod-Datei).
- Lokale Repo-Prüfungen laut Regeln (precheck --fast, check_data aller Module, dup_impl, tooltip, wiki --all --check,
  missing_textures, layers check), `javac` der geänderten Mod-Klassen.
- Screenshots per Playwright gegen `tests/harness.py` (nur lokal, nur Bilder) → Vorschau LIVEMAP34b
  `/root/sb-tools-a/previews/a-livemap34-preview2.html`.
- Gametests auf sb-test, sobald erreichbar: `/root/sbt a-livemap34 "python3.12 tools/testrunner/run.py --targets module-simplelivemap-fabric-263"`.

## 7. Abweichungen (während der Umsetzung nachgetragen)

- **A1 Kachel-URLs:** keine Prüfsummen-URLs im Viewer (D10): Kacheln liegen inhaltsadressiert, ausgeliefert unter dem Pfad
  mit `ETag` + `max-age` = Kachel-Takt; Prüfsummen-URLs mit dem R2-Umzug.
- **A2 Spieler gehostet:** zwei Schalter: `web.showPlayers` am Server (was den Server verlässt) und „Spieler zeigen“ der Karte;
  zusätzlich einmalige Annahme der AV-Vereinbarung (Rechtsentwurf U3, Zeitpunkt + Version am Konto). Positionen nur im RAM.
- **A3 Rechtsentwurf-Hinweise umgesetzt:** U2 (AGB-Version/Zeitpunkt bei Registrierung), U3 (s. A2), U7 (kein „nur … sonst
  nichts“ in Texten), U8 (Test: Preise aus `tiers.json` stehen im AGB-Entwurf). Offen: U1 Fußzeilen-Links (Config `legal.*`
  vorbereitet), U5 Melde-Link je Karte, U12 Bestell-/Kündigungsbutton (Bezahlstart).
- **A4 Manifest:** Index-Einträge, die die Mod nicht mehr meldet, werden (noch) nicht gelöscht – die Mod löscht Kacheln nur,
  wenn sie ganz leer werden; harmlos, beim R2-Umzug mitmachen.
- **A5 Dev-Stufe:** eigene Hosts `account-dev`/`livemap-dev.simplemods.de` mit nginx-Basic-Auth (Mod-API ausgenommen); bekannte
  Einschränkung: „per Link“-Karten senden `Authorization: Bearer` und kollidieren dort mit Basic-Auth.
- **A6 E-Mail:** ohne SMTP-Konto landen Mails im Dev-Ausgang (Admin-Seite); Produktion braucht SMTP (Besitzer, LIVEMAP34b-3).
- **A7 Self-Host:** Sperre je Installation (Config-Ordner) + Port-Erkennung; zwei Installationen mit absichtlich verschiedenen
  Ports auf einem Rechner sind nicht verhinderbar (dokumentiert, „nicht unterstützt“).

## 8. Prüfung (Ergebnis 12.11.)

- Plan-Abgleich: Abschnitt 4 Punkte 1–5 umgesetzt; Admin/„sofort fertig“, Stufen, erste 2 ohne Wartezeit, Self-Host-Sperre,
  connect-Befehl, Rechtsentwürfe (≥ 2 Runden, Protokoll in `docs/legal/README.md`), Deploy-Skript (nicht ausgeführt).
- `python3 -m unittest discover -s tools/livemap-web/tests -t tools/livemap-web/tests`: **40 grün** (Python 3.13 und 3.12;
  f-Strings zusätzlich auf 3.11-Verträglichkeit geprüft).
- `javac` der Modul-Klassen gegen das 26.3-Jar: nur der bekannte SimpleLib-AW-Fehler (`LibMaterials` StairBlock), sonst sauber.
- Lokale Checks: precheck --fast grün, alle 60 `check_data.py` grün, `check_dup_impl` 0, `check_tooltip_text` OK,
  `check_config_commands` 0, `check_gametest_adapters` OK, `check_missing_textures` grün, `layers.py check` 0;
  `wiki/generate.py --all --check`: nur die vorbestehende simpledimensions/modules.js-Drift + simplebuilding.json-Quellpfade,
  die nur durch den nach /tmp verlegten `build/`-Ordner entstehen (sb-agent hatte 0 freie Inodes).
- Playwright-Bilder gegen `tests/harness.py` (nur lokal, danach beendet): 20 Seiten, 0 JS-Fehler, 0 px Querüberlauf (1280 und
  390 px) → Vorschau `/root/sb-tools-a/previews/a-livemap34-preview2.html` (LIVEMAP34b).
- Kontrast (AGENTS 15, gerechnet): Text/Grund 14,0 · gedämpft/Karte 7,1 · Knopf grün 4,9 · grau 6,9 · rot 7,4 · Gold/Karte 8,9 ·
  Zustand verbunden 7,4 / vorbereitet 6,5 · Eingabefeld 15,1 – alle ≥ 4,5:1.
- **Nicht getestet:** Gametests (`selfHostLock`, `selfHostPortProbe`, `hostedParts`, `hostedConnect`, `connectPermission` und die
  bestehenden) – sb-test „No route to host“; nginx -t der Vorlagen (kein nginx auf sb-agent, nur Struktur-Test); echter
  Mod↔Dienst-Lauf (nach Dev-Deploy); SMTP-Versand; Stripe gegen die echte API.

## 9. a-livemap34c – Anzeigearten, Streichpreis, Q1 = A (Besitzer 12.11. ~18:35, LIVEMAP34b 1–7 = A)

Auftrag: `/root/sb-tools-a/agenten-1530.md` § 12c; Wortlaut „Livemap-Themes/Anzeigearten: 1 Blockform wie Chunkbase (Standard),
2 Shader, 3 etwas Besonderes; Preise: alten Jahresplan durchgestrichen notieren“. Basis hub/b-bundle188 90fb9d8b6, Branch `a-livemap34c`.

### 9.1 Ist-Stand (erhoben)

- Viewer (`index.html`, eine Datei für Mod und Dienst) zeichnet nur die Farbkacheln: Vanilla-Kartenfarben **mit eingebranntem**
  Vanilla-Relief (`render.shading`, 3 Helligkeitsstufen + 1-Pixel-Dither, nur Vergleich mit dem Nordnachbarn). Die Kachel enthält
  **keine Höhe** – eine echte Höhenschattierung oder Höhenlinien lassen sich aus dem Bild nicht berechnen.
- `ChunkSampler.column` kennt Höhe und Wassertiefe je Säule schon, wirft sie nach dem Färben weg.
- Kachelpfade: Mod-Webserver, Dienst-Upload, Manifest und Auslieferung prüfen `<ordner>/<z>/<x>/<y>` mit Ordner `[a-z0-9_.-]{1,96}`.
  Dashboard-Vorschau nimmt die größte Kachel einer groben Zoomstufe; `livemap_static_sync.py` baut `live.json` aus den Ordnern.
- Preisseite: Monat/Jahr-Umschalter (`data-month`/`data-year`), Jahrespreis = 10 × Monatspreis, kein Vergleichspreis. Karte „Kündigen“
  sagt „Jährlich … verlängert sich nur, wenn nicht gekündigt“ – passt nicht zu Q1 = A. AGB 11.3 hat noch drei Varianten (A/B/C),
  README B1 „Entscheidung nötig“, Anlage 3 nennt „automatische Verlängerung“.

### 9.2 Entscheidungen (selbst getroffen, begründet)

| # | Entscheidung | Grund |
|---|---|---|
| E1 | **Höhenebene** als zweite Kachelreihe im Ordner `<dimension>.h` (gleiche Pyramide, gleiche Routen, gleicher Upload): Pixel RGBA, R/G = y + 32768 (16 bit), B = Vanilla-Helligkeitsstufe (2 bit) · 64 + Flüssigkeitstiefe (0–63), A = 255; durchsichtig = unbekannt (z. B. nur von Karten-Items gefüllt). `TileStore.folder` gibt für echte Dimensionen nie `….h` aus (dann Hash-Form), höchstens 94 Zeichen. | Farbkacheln bleiben Bilder (Dashboard-Vorschau, statisches Hosten, alte Daten, Anzeigeart 1 braucht nichts Neues); keine neue Route, kein neues Prüfmuster (der Punkt ist schon erlaubt). |
| E2 | Anzeigeart **1 „Blöcke“ (Standard)**: Farbkacheln wie bisher (Vanilla-Kartenfarben + -relief); ab 6 px je Block Blockkanten (oben/links hell, unten/rechts dunkel) und je Block eine kleine Helligkeitsstreuung → liest sich wie die Blockkarte von Chunkbase. Braucht keine Höhenebene. | Wortlaut „Blockform wie Chunkbase“; keine Mojang-Texturen (eigene, berechnete Pixel). |
| E3 | Anzeigeart **2 „Schattiert“**: Farben ohne eingebranntes Relief (Helligkeitsstufe aus B herausgerechnet) × Hangschattierung (Licht Nordwest 45°, zentrale Differenzen über Kachelgrenzen mit Nachbarkacheln), leichte Höhentönung; Flüssigkeiten nach Tiefe (flach hell → tief dunkel). | „Shader: Höhenschattierung/Relief, Wasser-Tiefe“. |
| E4 | Anzeigeart **3 „Kartograf“** (alte Papierkarte): Pergament, Farben als Tusche-Lasur, Höhenlinien (alle 8 Blöcke fein, alle 32 kräftig, bei grobem Zoom weiter), Wasser blaugrau mit Uferlinie, Papierkorn, Windrose; Unerkundetes = leeres Papier wie bei Vanilla-Karten. | Passt zu Simple Maps / Kartentisch, braucht nur vorhandene Daten. „Nacht/Leuchten“ verworfen: bräuchte Blocklicht-Daten, die die Kacheln nicht haben. |
| E5 | Ohne Höhenebene (alte Kacheln, Karten-Items, Höhenkachel fehlt): 2 und 3 nutzen die Farben ohne Relief-Neuberechnung – nichts bricht. | Rückwärtsverträglich. |
| E6 | Auswahl „Kartenstil“ als drei Knöpfe im Ansichts-Fenster, gemerkt in `slm-opts.style` (nur dieser Browser, try/catch), Standard `blocks`. | Auftrag; wie die übrigen Ansichts-Einstellungen. |
| E7 | Zeichnen: je Kachel und Stil ein 128×128-Zwischenbild, höchstens 12 neue je Bild (Rest im nächsten Bild, bis dahin Farbkachel), neu, wenn Kachel oder Nachbar-Höhe nachlädt; Höhenkacheln nur in Stil 2/3 geladen. | Flüssig auch auf dem Handy, Stil 1 lädt nichts zusätzlich. |
| E8 | Preisseite + Stufen-Seite: im Jahresmodus neben dem Jahrespreis **durchgestrichen 12 × Monatspreis** (`<s>`, Screenreader-Text „statt“, Unterzeile „12 × Monatspreis“). Beschriftung als Monatsvergleich, nicht als früherer Preis. | Wortlaut; Irreführungsschutz (§ 5 UWG/§ 11 PAngV) → Prüfpunkt in `docs/legal/README.md`. |
| E9 | **Q1 = A** überall gleich: AGB 11.3 nur noch Variante A (12 Monate, danach unbefristet monatlich zum Monatspreis, jederzeit zum Monatsende kündbar, Hinweis-Mail vor Ablauf), README B1 entschieden, Anlage 3 angepasst, Preisseite „Kündigen“ + Hinweis unter dem Umschalter. Test prüft Preisseite/AGB. | Besitzer-Antwort. |
| E10 | Dienst: Dashboard-Vorschau ohne `.h`-Ordner; `livemap_static_sync.py` listet `.h` nicht als Dimension (kopiert die Kacheln aber mit). | Höhenkacheln sind keine Bilder für Menschen. |

### 9.3 Dateien

Viewer `modules/simplelivemap/shared/resources/assets/simplelivemap/web/index.html`; Mod `ChunkSampler`, `TileStore`, `LivemapRuntime`,
`LivemapTests` (tileColor + Höhenebene); `modules/simplelivemap/wiki/manual.json` (Kacheln, Ansicht) + Wiki neu;
Dienst `smweb/pages.py`, `smweb/app.py`, `static/app.js`, `static/site.css`, `tests/test_service.py`, `tests/harness.py` (Höhenkacheln
für die Bilder); `tools/deploy/livemap/selfhost/livemap_static_sync.py`; `docs/legal/agb.md`, `docs/legal/README.md`.

### 9.4 Risiken

- Speicher/Upload: Höhenkacheln ≈ +50–100 % Dateien und Bytes (zählen zu `map_mb` und Upload-Takt). Bewusst ohne neuen Schalter;
  Rückfrage in der Vorschau.
- Java nur per `javac` geprüft; Gametest-Lauf erst, wenn sb-test erreichbar ist (`/root/sb-tools-a/a-livemap34c-sbt.cmd`).
- Canvas-Rücklesen: Höhenkacheln mit `colorSpaceConversion: "none"`, `premultiplyAlpha: "none"` dekodiert (A = 255 → verlustfrei).

### 9.5 Prüfung (geplant)

Unit-Tests des Dienstes (+ Streichpreis, Q1-Text, Vorschau ohne `.h`, statische Synchronisation), `javac` der Mod-Klassen, lokale
Repo-Checks laut Regeln, Playwright-Bilder der drei Stile (Desktop + Handy, 0 JS-Fehler, kein Querüberlauf) und der Preisseite
(Jahr) gegen `tests/harness.py`, Kontrast ≥ 4,5:1 gerechnet; Gametests als sbt-Datei + TODO.

### 9.6 Abweichungen (während der Umsetzung)

- **A8 Eigener PNG-Schreiber für Höhenkacheln** (`TileStore.pngUp`, Filter „Up“ je Zeile, Deflate 9): ImageIO schrieb die Höhenkacheln
  ~3× so groß wie die Farbkacheln, `pngUp` ~2× (gemessen an Beispieldaten mit Wald; Rundlauf über ImageIO/Pillow verlustfrei).
  Farbkacheln bleiben bei ImageIO (dort kleiner).
- **A9 Höhenlinien nicht auf Baumkronen** (Kartograf): Pixel mit der Vanilla-Kartenfarbe „plant“ (Laub) bekommen keine Linie – sonst
  zeichnet der Wald Rauschen statt Gelände.
- **A10 Gametest:** kein neuer Test, `tileColor` prüft zusätzlich Höhenpixel (y, trocken), Farben unverändert, Höhenkachel = Höhenpixel,
  negative Höhen und die `.h`-Ordnerregel.
- **A11 Harness:** zeichnet jetzt Vanilla-artig (Grundfarbe × Helligkeitsstufe, Relief gegen den Nordnachbarn, ganze Blockhöhen, Bäume)
  und lädt Höhenkacheln mit hoch; nur Bild-Erzeugung.
- **A12 Preistabelle:** `.tw` ist `position: relative`, sonst ragte der Screenreader-Text „statt“ auf dem Handy aus dem Scroll-Kasten.
- **A13 Speicher:** Höhenebene ≈ +200 % zu den Farbkacheln (Beispieldaten) → Besitzer-Frage LIVEMAP34c F2 (A immer / B Schalter /
  C nur bis Zoom 3). Handy: das Ansichts-Fenster liegt wie vorher über der 2./3. Leiste (unverändert).

### 9.7 Prüfung (Ergebnis 12.11. abends)

- Plan-Abgleich 9.2 E1–E10 umgesetzt (E9: AGB 11.3, Tabelle 3.1, Anlage 3, README B1/24, Preisseite, `billing.py`-Hinweis).
- `python3 -m unittest discover -s tools/livemap-web/tests -t tools/livemap-web/tests`: **43 grün** (Python 3.12 und 3.13).
- `javac` aller Klassen von simplelivemap gegen 26.3: nur der bekannte SimpleLib-AW-Fehler (`LibMaterials` StairBlock).
- precheck --fast grün, 60 × `check_data.py` grün, `check_dup_impl` 0, `check_tooltip_text` OK, `check_missing_textures` 0 Fehler,
  `layers.py check` 0; `wiki/generate.py --module simplelivemap` neu; `--all --check` nur mit den Abweichungen aus Bündel 188
  (simpleblocks/simpleoctets/modules.js/simplebuilding.json/textures.js – nicht von diesem Branch).
- Playwright gegen `tests/harness.py` (lokal, danach beendet): 3 Stile × 2 Zoomstufen + Blockkanten + Fenster (Desktop/Handy) +
  Preisseite DE/EN/Handy: 0 JS-Fehler, 0 px Querüberlauf; Stil übersteht Neuladen; Streichpreis nur im Jahresmodus sichtbar.
- Kontrast: Fenstertext 11,5 · gedämpft 6,9 · Spielername auf Pergament 11,8 · Windrose „N“ 6,4 (Rand: Lichthof 12,6) · alter Preis
  7,1 · Jahrespreis 12,4 · Hinweis 9,9.
- **Nicht getestet:** Gametests (sb-test nicht erreichbar → `/root/sb-tools-a/a-livemap34c-sbt.cmd`), echter Mod-Lauf mit echten
  Welten (Speicherbedarf der Höhenebene in echten Welten), Darstellung in Firefox/Safari (nur Chromium).
