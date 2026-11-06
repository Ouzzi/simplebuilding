# Plan a-wikiux06 – Texturen im Wiki (Ziel des Besitzers 06.11., Abschnitt „Texturen (jetzt)“)

Basis: hub `claude-wave1` c529ff1e2 (enthält a-wikitex05: eine Karte je Block/Item).
Quelle: /root/sb-tools-a/WIKI-ZIEL-2026-11-06.md, Auftrag a05 + Zusatz a05b.

## Ist-Zustand (erhoben)
- `wiki/textures.py` indiziert jede Textur aller 58 Module (2763 Einträge). Abgleich aller PNGs unter
  `src/main/resources`, `mc26_3/overlay`, `modules/**`, `common/**` gegen `wiki/data/textures.js`: **0 fehlen per ID**
  (122 Pfade aus `src/main/resources` sind vom 26.3-Overlay überdeckt – richtig so; 690 Pfade aus `mc1_21_11` gehören
  nicht zur 26.3-Linie).
- Was der Besitzer als „fehlt“ sieht: **329 Einträge** (+14 Render-Icons) zeigen auf Dateien, die git ignoriert
  (Vanilla-abgeleitet, A8, `build/vanilla-derived`). Dev- und Prod-Wiki sind reine git-Checkouts auf sb-web →
  die Bilder gibt es dort nicht, die Kacheln bleiben leer bzw. zeigen ein kaputtes Bild.
- Pixeleditor (`wiki/assets/pixeleditor*.js`) bearbeitet das ganze PNG; `editor_save.check_save` verlangt gleiche
  Größe, schreibt nur PNG + `.layers.json` → Sheet-Format und `.mcmeta` bleiben schon heute erhalten, ungeprüft.
- KI-Queue `POST /api/queue` (devqueue.py) kennt nur `texture|recipe|note`, ohne Anhänge; Schreibrecht = Header
  `Cf-Access-Authenticated-User-Email` (Cloudflare-Access-Allowlist des Besitzers).
- Rezepte: zwei Reiter „Alle Rezepte“ (`allrecipes`, Stations-Filter) und „Rezepte“ (`recipes`, ein Abschnitt je Typ).

## Umsetzung
1. **Fehlende Texturen**: (a) `wiki/vanilla_textures.py --derived` legt die Vanilla-abgeleiteten Dateien
   (`tools/textures/vanilla_derived.write`, sonst Platzhalter) an genau die Wiki-Pfade aus `textures.js` – nur auf
   dem Dev-Server (sb-web-sync), nie ins Repo. (b) Prod/fehlende Datei: Kachel zeigt statt eines kaputten Bildes
   einen schraffierten Platzhalter „nicht veröffentlicht (aus Vanilla abgeleitet)“. Zählung vorher/nachher im Bericht.
2. **Vanilla**: `wiki/vanilla_textures.py` liest das 26.3-Client-Jar, schreibt `wiki/assets/vanilla/**` +
   `wiki/data/vanilla-textures.js` (git-ignoriert), gleiche Felder/Gruppierung wie `textures.py` (`model_owners` aus den
   Jar-Modellen). `generate.py --with-vanilla` ruft es auf (nur Dev). Die Wiki-Seite lädt die Datei **nie selbst**; nur
   `dev-overlay.js` (Zentrale, hinter Login) lädt sie → Kategorie „Vanilla“, ✎ als Vorlage (ohne „Speichern ins Repo“).
   sb-web-sync: Jar per ssh von sb-agent, Lauf nur bei neuem Dev-Stand. Operator: nginx muss `/assets/vanilla/` und
   `/data/vanilla-textures.js` auf dem öffentlichen Prod-Host nicht haben (gibt es dort nicht, da Prod nie den Lauf macht).
3. **Karten-Detail**: Klick auf den Kartenkopf → Dialog mit allen Texturen groß, Rolle als Überschrift, Animationen
   laufen nach mcmeta (frametime/frames), Sheet daneben. Dev: „Bearbeiten“ fragt zuerst die Textur/Seite → Pixeleditor.
4. **Animationen im Editor**: `SBPixelEditor.open({anim})` zeigt Frame-Linien, Wahl „Ganzes Sheet | Frame n“; mit Frame
   gewählt bleiben Pixel außerhalb unberührt. `editor_save.check_save` prüft bei animierten Texturen Sheet-Raster
   (Höhe = n × Frame) und lehnt `mcmeta` im Payload ab; Test: Commit ändert `.mcmeta` nicht.
5. **„Änderung vorschlagen“** (Dev-Overlay): Knopf an Karte, Detail-Textur, Großansicht, Item-Seite. Textfeld unter den
   Texturen, Bilder per Upload/Paste (≤ 5, je ≤ 2 MB, PNG/JPEG/WebP). `POST /api/queue` `kind: suggestion` mit `object`,
   `targets` (Textur-Pfade), `attachments` (base64). Server prüft Magic-Bytes/Größe, vergibt die Dateinamen selbst,
   legt die Bilder unter `<data>/attachments/<id>/` ab und kopiert die aktuelle Textur (aus `textures.js` per ID) als
   Referenz dazu. `GET /api/queue/<id>/files/<name>` liefert sie; die Zentrale zeigt Vorschaubilder. Löschen räumt auf.
   Schreibrecht unverändert (Access-Header). nginx `client_max_body_size` für `/api/` auf 12m (Operator, setup-Skript).
6. **Rezepte**: Reiter „Rezepte“ entfällt in der Seitenleiste; `#/recipes` bzw. `?tab=recipes` zeigt die eine Ansicht
   „Alle Rezepte“ (Filter nach Station/Typ, Suche). Rezept-Detailseiten bleiben.

## Risiken
- `index.html` ist groß; nur angehängte Funktionen, keine Umbauten an bestehenden Pfaden.
- Body-Größe Vorschlag > nginx-Grenze bis zur Operator-Änderung → Fehlertext „413“ im Overlay.
- Vanilla-Index ~ 1–2 MB: nur Dev, nachgeladen.

## Verifikation
- `python3 -m unittest discover -s wiki/tests`, `python3 -m unittest discover -s tools/zentrale-web/tests`,
  `python3 wiki/generate.py --all --check`; Playwright-Screenshots (sb-test, venv wikivis) von Karten, Detail,
  Vorschlag, Vanilla unter /root/sb-tools-b/previews/a-wikiux06/; `./gradlew checkWiki -q` per sbt.
- Kein Server-Deploy (A nach Freigabe).

## Rückfragen
- keine; Frame-Editor mit Zeitleiste bleibt „später“.
