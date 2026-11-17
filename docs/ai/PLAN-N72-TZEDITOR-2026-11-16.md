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

## N72b – Erweiterung (Besitzer: „implementiere es dann einfach“, gleicher Branch)

Ist-Zustand: Rechtsklick zeigt das Wiki-Menü („#light / Crafting tree“), weil die Zellen `data-id` tragen. Das Spiel liest
Configs nur für `LayoutConfig.AREAS` = metals, wood, aging, rest, stone. Kompakt und Items baut es nach dem Algorithmus
(`OrderAreas.compact`, `items`). Die Wiki-Kopien unter `wiki/data/testcentre/` waren seit b-fix197 (große Türen) veraltet.

Plan:
1. **Zellmenü** (tzeditor.js): Rechtsklick, Umschalt+F10 oder Menütaste, Pfeile, Enter, Esc. Einträge: Block ändern …
   (Suche + Vorschau, jede Id), fehlend (#red), gibt es nicht (#light), leeren, Rahmen an/aus (#frame, nur ohne Item), Reihe
   oder Spalte einfügen, Rückgängig. index.html: Wiki-Menü übergeht Ereignisse mit `defaultPrevented`. Rückgängig über alles:
   `History` merkt sich je Schritt alle berührten Bereiche.
2. **Spielplan**: Integrationstest `sbintegration:cross_mod_game_test_test_centre_plan_with_all_modules` mit `SB_TC_DUMP=1`
   (Server-Gametest, kein Client). Daraus `build_editor.py --plan` → `wiki/data/testcentre/plan.json` (nur Ids, Schlüssel,
   Gründe): Familie, Seite und x je Linien-Reihe, Reihen ohne Config, Band „Neu“, Kompakt-Blöcke je Mod, Ausnahmen.
   Ausnahmen sind (a) Musterfamilien ab 24 Varianten, die in der Config fehlen, und (b) registrierte Blöcke ohne Feld im
   Spielplan, mit den Gründen aus `TestCentreLayout.EXCLUDED`/`MODULE_EXCLUDED`/`technical`. Live-Prüfung im Seitenpanel.
3. **Alle Bereiche**: Eintrag im Bereich-Dropdown; alle Bereiche nebeneinander, jede Tabelle bearbeitbar, in der Zoom-Komponente.
4. **Wie gebaut**: `builtPlan` = Regel aus `OrderAreas.families`. Seite A: metals, wood, aging, rest, Stein-Rest. Seite B:
   Stein. Der Schnitt liegt an einer Familiengrenze, beide Seiten so gleich wie möglich, 114 Reihen je Linien-Paar. Familien
   kommen aus plan.json. Die Zeichnung zeigt je Reihe eine Spalte, Feld 0 am Weg, Schildfeld und graue Linie. Prüfung: das
   Ergebnis muss gleich der Dump-Reihenfolge sein (Test).
5. Speicherformat bleibt: eingefügte Reihen und Spalten sind normale Schlüssel und Spalten (Test in test_tzedit_save).

## Phase 2 – Umsetzung / Abweichungen

- Wie geplant: zoomarea.js, vanilla_textures.py `--icons` (+ `assets/devtex/` für Modul-Texturen ohne Wiki-Kopie), tzeditor.js,
  index.html, sb-web-sync (`sb-jar`), README.
- Abweichung: die Rezepte-Seite (`tab=recipes`) leitet auf „Alle Rezepte“ um. Statt ihr hat jetzt jede Liste mit mehr als
  40 Einträgen die Leiste (`listPage`). Die alte Seite `pageRecipes` hat die Leiste trotzdem.
- Abweichung: Mob-Köpfe haben kein flaches Bild. Sie zeigen das Spawn-Ei des Mobs (Alias für Riese/Kuh-Riese …). Herobrine
  und Riese zeigen das Gesicht des Skins, im Browser ausgeschnitten.
- Abweichung: „Rahmen an/aus“ schaltet den Platzhalter `#frame`. Bei einer Zelle mit Item ist der Eintrag gesperrt: das
  Format kennt keinen Rahmen je Item, und das Spiel entscheidet das selbst (`Exhibits.falls`).
- Zusatz: die veralteten Wiki-Kopien der Configs neu erzeugt (`build_editor.py`).
- Kompakt-Ergebnis: im Spiel hat jeder Block einen Platz (Algorithmus). In der Config-Datei fehlen 14 Blöcke (12 große
  Kupfertüren angelaufen, verwittert, oxidiert; Redstone- und Auto-Formschneide). Keine Familie erreicht die Ausnahme-Grenze.

## Phase 3 – Verifikation

Siehe Abschlussbericht der Session (Tests auf sb-test, Harness-Klicktest, Dev-Klicktest offen bis zum Bündel).
