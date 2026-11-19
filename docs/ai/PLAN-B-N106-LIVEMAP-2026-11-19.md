# PLAN B N106 – Simple Livemap: Offenes fertig + Release-Check (19.11.2026)

Auftrag: `/root/b-auftrag-20261109.md` „## N106–N108“ › N106 (Besitzer 19.11., Prio 1). Branch `b-livemap106`
(Basis `claude-wave1` ea9326b793 = Production). Parallel läuft N108 (`b-stripe108`) in `tools/livemap-web` (Billing/Recht).

## 1. Ist-Zustand (erhoben)

- Dienst `tools/livemap-web` (N34/N34c, A): gebaut, **nie deployt**. `livemap-dev.simplemods.de`/`account-dev…` lösen nicht
  auf (DNS/Tunnel fehlt) → eine DEV-Abnahme des Dienstes ist heute **nicht möglich**. `wiki.simplemods.de/dev/` = Wiki-Dev
  (Login), dort läuft kein Livemap-Dienst.
- Mod `modules/simplelivemap` 1.0.0: Audit b-w3b „shipbar“ (0 P1/P2), offen: echter Browser aus dem Spiel, Client-Start im Bündel.
- Owner-Antwort LIVEMAP34c = ok → F1 A Kartograf, F2 A Höhenebene immer, F3 A Streichpreis, F4 A Stil je Besucher,
  F5 A Viewer bleibt Englisch (alles schon gebaut).
- Keine Memory-Dateien `livemap34*`; Quellen: `uebergabe-A.md` (TODO a-livemap34, a-livemap34 Deploy, a-livemap34 offen im
  Dienst, a-livemap34c), `PLAN-A-LIVEMAP34` §7 A1–A7 / §9.7, `docs/legal/README.md` U1/U5/U12, `AUDIT.md` C9, QUEUE N76.

## 2. Liste offener Punkte (mit Quelle) und was N106 damit macht

| # | Punkt | Quelle | N106 |
|---|---|---|---|
| O1 | Gametests a-livemap34/34c nie gezielt gelaufen (selfHost*, hosted*, tileColor+Höhe) | uebergabe-A TODO a-livemap34/34c | **laufen lassen** (`module-simplelivemap-fabric-263` + standalone) |
| O2 | „Per Link“-Karte sendet `Authorization: Bearer` → kollidiert mit Basic-Auth der Dev-Stufe | PLAN-A A5, README | **bauen**: Viewer sendet gehostet `X-Livemap-Token`, Dienst nimmt beides |
| O3 | Manifest löscht keine Kacheln, die die Mod nicht mehr meldet | PLAN-A A4, uebergabe-A | **bauen**: Runde = Chunks bis `done`; Tabelle `manifest_seen`; Leermeldung löscht nie |
| O4 | U5 Melde-Link je Karte + Admin-Ansicht (DSA Art. 16) | legal README U5, A3 | **bauen**: `/report`, Tabelle `reports`, Ratenlimit, Admin-Liste (erledigen/Karte trennen) |
| O5 | U1 Fußzeilen-Links | legal README U1 | Code fertig (Config `legal.*_url`); **Besitzer** trägt URLs ein, wenn Texte freigegeben |
| O6 | U12 Bestell-/Kündigungsbutton, Jahresabo als Stripe-Zeitplan | legal README U12, uebergabe-A 34c | **N108** (`b-stripe108`), hier nicht anfassen (Konflikte) |
| O7 | SMTP-Anbieter | uebergabe-A, A6 | **Besitzer** |
| O8 | Deploy Dev-Stufe (CT, Tunnel-Hostnames, htpasswd, deploy-Skript) | uebergabe-A Deploy | **A/Besitzer**: Anleitung im Bericht, kein Deploy durch B |
| O9 | Prüfsummen-URLs + R2 | PLAN-A A1 | später (Bezahlstart), kein Release-Blocker |
| O10 | Nicht getestet: echte Welt (Höhenebene-Speicher, Upload-Takt), Firefox/Safari | PLAN-A §9.7 | **Release-Check** |
| O11 | Echter Browser aus dem Spiel, Client-Start | AUDIT C9 | Probe `uiprobe_n106.py` schreiben (Hauptsitzung schießt) |
| O12 | Self-Host auf sb-web `/map/` (N76) | QUEUE b-livemap16 | ruht (Spielserver aus) – Besitzer |

## 3. Umsetzung (Dateien)

- Viewer `modules/simplelivemap/shared/resources/assets/simplelivemap/web/index.html`: gehostet (`/m/<slug>/`) Header
  `X-Livemap-Token`, sonst wie bisher `Authorization: Bearer` (Mod-Webserver unverändert, kein Java-Eingriff); Link „Report“
  nur gehostet.
- Dienst `smweb/app.py` (Token-Header, `/report`, Admin-Aktionen), `smweb/maps.py` oder `upload.py` (Prune, Reports),
  `smweb/db.py` (Migration 3: `manifest_seen`, `reports`), `smweb/pages.py` (Meldeformular, Admin-Liste), `deploy/nginx-smweb.conf.in`
  (`/report` POST-Limit falls nötig), `tests/test_service.py` (neue Tests), `tests/loadtest.py` (neu, 13 Karten Ultra).
- Probe `modules/simpleinterfaces/tools/uiprobe_n106.py` (nicht ausführen).

## 4. Risiken

- N108 hängt eventuell ebenfalls eine Migration an `MIGRATIONS` → Merge-Konflikt (Reihenfolge beim Bündeln festlegen; noch
  nichts deployt, also unkritisch). Im Bericht vermerken.
- Prune darf nie zu viel löschen: unterbrochene Runden → Vereinigung (nur weniger gelöscht); Dienst-Neustart egal (DB);
  Runde mit 0 Kacheln löscht nichts.

## 5. Verifikation

- Unit-Tests Dienst (lokal Python, kein Gradle) + `/root/sbt b-livemap106` Gametests simplelivemap (fabric-263 + standalone)
  + `testLivemapWeb`, nacheinander.
- Release-Check: Playwright gegen `tests/harness.py` (lokal nur für Messung/Bilder): Kontrast, Container-Rand, Mobil 390 px,
  Fehlerseiten, DE/EN, Chromium (+ Firefox/WebKit falls installierbar); Lasttest 13 Karten Ultra (`tests/loadtest.py`);
  echte Welt = Kacheln aus generierten Gametest-Welten auf sb-test (Speicher Farb-/Höhenkacheln, Upload in Dienst).
- DEV-Klicktest: nicht möglich, solange die Dev-Stufe nicht deployt ist → ehrlich als offen melden + Deploy-Anleitung.
