# N72 – Testzentrale-Editor: Texturen + gemeinsame Vollbild/Zoom-Komponente (2026-11-16)

Branch `b-tz72` (Basis `b-bundle202` = Production 590f2ee3f3), Hub `/root/sb.git`.
Besitzer-Auftrag: Screenshot `9-tzeditor-fragezeichen.png` (viele Zellen „?“), plus eine Komponente für alle großen
Scroll-Flächen (Vollbild/Zurück+Esc, Plus/Minus, Strg+Mausrad nur über der Fläche, Zoom bleibt beim Wechsel).

## Phase 1 – Ist-Zustand (geprüft 16.11.)

Vorher-Probe mit Playwright gegen `https://wiki.simplemods.de/dev/` (angemeldet, Test-Nutzer): 656 Id-Zellen im
Bereich „lines / Metalle“, 344 HTTP-404, alle Vanilla-Zellen zeigen „?“.

Ursachen (alle vier zusammen ergeben das Bild):

1. **Auf dem Dev-Host gibt es gar keine Vanilla-Bilder.** `/dev/assets/vanilla/block/stone.png` → 404,
   `data/vanilla-textures.js` → 404. Der Vanilla-Schritt (`wiki/vanilla_textures.py`) steht nur in der Repo-Fassung
   `tools/zentrale-web/sb-web-sync`; die live `/usr/local/bin/sb-web-sync` auf sb-web ist älter (Ausrollen 05.11. war vor
   dem Vanilla-Block vom 06.11.). Selbst ausgerollt würde er scheitern: das Jar holt er mit
   `ssh sb-agent "cat ~/.gradle/.../minecraft-client.jar"`, und der Forced Command `sb-web-gate` auf sb-agent lehnt alles
   außer git-upload-pack/sb-status/sb-log/sb-dist ab („denied“).
2. **Falscher Name je Id.** `img()` baut `assets/vanilla/block/<id-name>.png` – für Stufen, Treppen, Mauern, Gitter,
   gewachste Varianten, Laternen, Ketten, Erze mit anderem Dateinamen, Roh-Blöcke usw. gibt es keine Datei dieses Namens
   (Textur steht im Modell: items/<id>.json → Modell → Eltern → textures). Für Vanilla-Ids nimmt `img()` zudem zuerst
   `ctx.icon()` = `textureOf()` = `assets/textures/minecraft/<name>.png` (liegt nur dort, wo generate.py lokal mit Jar
   lief) – damit steht genau ein Kandidat in der Liste, die `assets/vanilla/…`-Versuche kommen nie dran.
3. **„?“ statt Name:** der globale Bildfehler-Handler in `index.html` (`$main` capture, `slot-text` mit `alt||'?'`)
   ersetzt das `<img alt="">` des Editors durch „?“, bevor der Editor seinen nächsten Kandidaten bzw. den Namen zeigen kann.
   „? ?“ = Grundblock und Achtel darauf, beide so ersetzt.
4. **Mod-Ids ohne eigenes Bild** (Stufen/Mauern/Treppen von simpleblocks, Achtel von simpleoctets …) raten über
   `index.vanilla` Grundnamen (`cut_copper_octet`, `blue_ices` …) statt das Modell aufzulösen.

Vorhanden und wiederverwendet: `tools/devserver/sbdev/icons.py` `Resolver` (Item-Definition → Modell → Eltern →
Texturen, Mod-Assets aller Module + Client-Jar; nur Standardbibliothek), `wiki/vanilla_textures.py` (Dev-Job, schreibt nur
git-ignorierte Dateien, läuft im sb-web-sync), Wiki-Tokens/`.btn`/`.chip`, i18n-Muster EN+DE.

## Phase 1 – Plan

A. **Zuordnung Id → Bild (nur Namen/Pfade, keine Pixel)**: `wiki/vanilla_textures.py --icons` schreibt
   `wiki/data/vanilla-icons.json` (git-ignoriert): alle Vanilla-Items des Jars + alle Ids der Testzentrale-Layouts ohne
   eigenes Modulbild, über `sbdev.icons.Resolver` aufgelöst, Pfade auf Wiki-Pfade umgeschrieben (`assets/vanilla/…`,
   `assets/textures/…`). Läuft im Standardlauf mit (also automatisch im sb-web-sync, sobald der Vanilla-Schritt dort läuft).
B. **tzeditor.js**: lädt `data/vanilla-icons.json` (darf fehlen); Kandidatenkette je Id: Modulbild (index) → Zuordnung →
   `ctx.icon` → alte Namensversuche; doppelte raus. Scheitert alles: Name in der Zelle (nie „?“).
C. **index.html**: Bildfehler-Handler fasst Bilder in `.tz-page` / `[data-own-fallback]` nicht an.
D. **Komponente `wiki/assets/zoomarea.js`** (`window.SBZoom`): Leiste je Fläche mit −, Prozent, +, „Vollbild“ /
   „Zurück (Esc)“; Strg+Mausrad über der Fläche zoomt (preventDefault nur dort); Vollbild = Fläche füllt das Fenster
   (fixed, Wiki-Tokens, Hell/Dunkel), Esc/Zurück zurück; Zoom-Stand je Fläche bleibt (sessionStorage) – auch über
   Vollbild/normal und Neuzeichnen. Generisch per CSS `zoom` auf dem Inhalt, Editor mit eigener Anwendung (Zellgröße `--z`,
   Schieberegler bleibt synchron). EN+DE-Texte in der Komponente.
E. **Anwenden**: Editor-Raster (zuerst), Texturen-Liste (`#tx-results`), Rezeptliste (Alle Rezepte `#ar-results`,
   Rezepte-Seite), große innere Tabellen-Rollbereiche (`.table-wrap` ohne `tw-full`, nur wenn sie überlaufen).
F. **sb-web**: Repo-`sb-web-sync` holt das Jar über neuen Forced Command `sb-jar`; README: Operator-Schritte.
   Nichts live selbst ändern.
G. **Tests**: `test_vanilla_textures` (Zuordnung Stufe/Treppe/Mauer/Mod-Achtel aus Fake-Jar), `test_tzeditor`
   (Kette, Handler-Ausnahme), `zoomarea_core.cjs` (Stufen, Klemmen), Playwright `zoomarea_ui.cjs` (Harness).

Risiken: CSS `zoom` (Chrome/Edge/Safari, Firefox ≥126) – sonst bleibt 100 %. Vollbild über Lightbox: z-index 150
(< Lightbox 190/200), Esc mit `defaultPrevented` beachtet. Ohne Jar auf sb-web bleibt der Dev-Host bei Namen statt „?“.

Verifikation: Unit-Tests auf sb-test (sbt), Wiki-Generate-Check, Playwright-Harness gegen statischen Server des Branches
auf sb-test (mit `vanilla_textures.py`-Lauf = was der Dev-Host nach dem Operator-Schritt tut): Zellen ohne Bild zählen,
Vollbild/Esc/±/Strg+Rad prüfen, Screens. Endgültiger Dev-Klick-Test erst nach Bündeln + Operator-Schritten.

## Phase 2 – Umsetzung / Abweichungen

(siehe unten, wird beim Bauen ergänzt)

## Phase 3 – Verifikation

(wird ergänzt)
