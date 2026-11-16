# PLAN N74 – Wiki-Navigation ohne Mod-Dropdown (2026-11-16)

Auftrag: Besitzer-Abnahme 16.11. ~12:30 UTC – Variante B (Mod-Kacheln + Mod-Bereich mit eigener Seitenleiste,
flache Adressen) plus Breadcrumb aus A; N74-2/-3/-4 ok. Spezifikation: `previews/n74/KONZEPT-N74.md` (sb-tools-b),
Mockup `previews/n74.html`. Branch `b-nav74` von `b-tz72` (N72 berührt dieselben Dateien).

## Ist-Zustand (erhoben)
- `wiki/index.html` (7208 Z.): eine Mod je Seitenaufruf (`selectedMod` aus `?mod=`, sonst localStorage
  `simple-wiki-mod`, sonst Pack-Kopf), `D = MODULE_DATA[selectedMod]`, alle Indizes (`IX`, `SEARCH`, `IW`, Obtain)
  aus `D`. Kopf: `#mod-select` (Dropdown mit „★ All mods“) und Schalter `#all-mods` („Whole pack“-Suche).
  Router: Query-Form (`?mod=..&item=..`), Hash-Links `#/…` werden per MutationObserver umgeschrieben; Mod-Wechsel
  = Neuladen. Modpack-Leiste (`packBarHtml`) steht über JEDER Seite. Seite `#/allmods` = Tabelle aller Mods.
- `module-loader.js` lädt beim Start ALLE 63 `data/<id>.js` → Familien-Zusammenführung kostet keine Netzlast.
- Daten (gemessen): IDs von items/blocks/recipes/loot/trades/enchantments/tags/advancements sind über alle Mods
  eindeutig und tragen immer den Namespace = Mod-Id. Kollisionen nur bei `features` (143, z. B. `commands`) und
  `config` (29, z. B. `enabled`); `inWorld.kinds` doppelt (14, gleiche Definition), Einträge eindeutig.
  `obtain`, `quests`, `recipesOtherLines`, `vanillaRecipes` gibt es nur in simplebuilding.
- Baum: 7 Wurzeln ohne `wiringexample`: 4 Super-Mods (simplebuilding 42 Sub-Mods, simplequalityoflife 7,
  simpleaesthetics 3, simpleoptimization 3) + 3 eigenständige (simplelib, simplelivemap, simplestats).
- `generate.py` schreibt nur `data/*.js` → keine Generator-Änderung.
- Tests: `test_allmods.py`/`test_modpack.py` prüfen Dropdown/Pack-Leiste als Strings; Browser-Harnesses
  (`home_ui`, `ui_lists`, `wiki24_ui`, `time_ui`, `enchanting_ui`, `tzeditor_ui`, `zoomarea_ui`) laden `?mod=…`
  bzw. `?tab=…` ohne Mod (bisher = SimpleBuilding).
- `tools/zentrale-web/nginx/wiki-one.conf`: Prod `location / { try_files $uri $uri/ =404; }`, Dev `^~ /dev/` mit
  `auth_request` + `sub_filter` (Overlay). `docs/WIKI-HOSTING.md`: GitHub Pages unter `/simplebuilding/` (!).

## Entscheidungen (selbst getroffen)
1. **Umfang = Familie über ein zusammengeführtes `D`** (`scopeData`): Im Mod-Bereich ist `D` die Vereinigung der
   Daten von Mod + allen Nachfahren (eigener Mod zuerst → seine Einzelwerte wie `obtain`/`vanillaRecipes` gelten),
   auf der Wurzel die aller Mods (Modpack-Filter berücksichtigt, simplebuilding zuerst). Dadurch zählen und listen
   ALLE Seiten (Zähler, Blöcke & Items, Rezepte, Beute, Handel, Konfiguration, Tags, Fortschritte, Anleitungen,
   Suche) automatisch über die Familie – kein Teilschritt mit „+N aus Sub-Mods“ nötig. Kollidierende Feature-/
   Konfig-Schlüssel eines Sub-Mods werden als `<mod>:<id>` geführt (Konvention, die `hrefFor` schon kennt);
   jedes Element merkt sich seinen Mod (`ORIGIN`), Listen zeigen bei Fremd-Herkunft ein Mod-Abzeichen,
   die Konfig-Tabelle gruppiert nach Mod mit eigenem Befehl.
2. **Abweichung vom Konzept: `<base href>` kommt vom Server, nicht aus einem Inline-Skript.** Grund: GitHub Pages
   liefert das Wiki unter `/simplebuilding/` aus – ein Inline-Skript, das `/simple…` als Mod-Segment deutet, würde
   dort alle relativen Pfade brechen. nginx setzt deshalb per `sub_filter '<head>' '<head><base href="/" data-modpath>'`
   (Dev: `/dev/`) die Basis. Das ist zugleich die **Laufzeit-Erkennung**: Nur wenn `<base data-modpath>` da ist (und
   http/https), erzeugt das Wiki Pfad-Adressen (`MODPATH`). Ohne Regel (heutiger Server, Pages, file://) bleibt
   alles in der Query-Form (`/?mod=simpleend&item=…`) – kein 404, kein Probe-Request.
3. URL-Schema (Pfad-Form): `<base><mod>/` Übersicht, `<base><mod>/<abschnitt>[/<id>]`, Item-Ids ohne eigenen
   Namespace (`/simpleend/items/ashwood_boat`), fremde Namespaces bleiben (`/simplebuilding/items/simpleend:x`),
   Suche `<base><mod>/?q=`, `tab`/`f`/`lang`/`pack` als Query. Wurzel: `/`, `/?tab=items`, `/?q=…`.
   Query-Form ohne Regel: `?mod=<mod>&…` wie bisher, Wurzel ohne `mod`.
4. Alte Links (beide Modi): `?mod=X…` → Mod-Bereich X (Pfad-Form per `replaceState`, wenn `MODPATH`);
   `#/allmods`/`?tab=allmods` → Startseite; `?item=ns:x`/`#/items/ns:x` ohne `mod` → Bereich des Namespace-Mods;
   übrige Alt-Hash-Links ohne Mod → SimpleBuilding (wie bisher); `?pack=…` → Filter + Hinweis auf der Startseite.
   Unbekannte Mod-Ids im Pfad → Startseite (kein 404).
5. Wurzel-Links auf Einzelseiten (Item, Block, Rezept, …) führen in den Bereich des Besitzer-Mods; Links aus
   einem Bereich auf Einträge außerhalb der Familie ebenso (wie bisher `crossModHref`).
6. Modpack-Leiste nur noch auf „Mod-Struktur“ (N74-4); `localStorage simple-wiki-mod` und `#all-mods` entfallen.

## Änderungen (Dateien)
- `wiki/index.html`
  - Kopf: `#mod-select`, `label.allmods` + CSS raus; Marke „Simple Mods Wiki“ → Wurzel; neue Mod-Leiste `#modstrip`
    (nur < 920 px): `[← Alle Mods] Icon Name · in Eltern-Mod`.
  - Mod-Wahl: `parseLocation()` (Pfad-Segment → `?mod=` → Alt-Link-Ableitung → Wurzel), `SCOPE`, `scopeData()`,
    `ORIGIN`; `buildModSelect`/Listener weg; `crossModHref` → `urlFor()`.
  - Router: `urlFor(mod, route)`, `pathToRoute()`, `routeUrl()` nutzt Pfad-Form bei `MODPATH`; `goHref` lädt neu,
    wenn das Mod-Segment wechselt; Startup schreibt Alt-Formen per `replaceState` um.
  - Seiten: `pageHome()` (7 Kacheln: 4 groß, 3 klein, Zahlen über alle, „In allen Mods nachschlagen“, Filter-Hinweis),
    `pageOverview` mit Breadcrumb, `crumbs()` beginnt mit „Alle Mods › Eltern-Kette“, `pageSubmods` im Bereich =
    Familienbaum, `pageCommands`/`pageConfig` nach Umfang, Modpack-Leiste nur in `pageStructure`.
  - Seitenleiste zwei Formen: Wurzel („Alle Mods“, Browser, Mod-Struktur, die 7 Mods, eingeklappt „In allen Mods: …“)
    und Bereich („← Alle Mods“, Mod-Kopf mit Icon/Name/„Sub-Mod von …“, Übersicht, Gruppen mit Zählern über die
    Familie, „Sub-Mods (n)“ eingerückt mit Filterfeld ab > 8, bei Blatt-Sub-Mods „Weitere in <Eltern>“, Fußnote
    „Zähler: X + N Sub-Mods“).
  - Suche: Pool = Umfang; Schnellsuche mit Kopfzeile „Gesucht in: …“ und Zeile „In allen Mods suchen (N Treffer)“
    (erweitert einmal, Enter → Wurzel `?q=`), Suchseite mit Link „In allen Mods suchen (N)“.
  - Texte EN/DE (`navAllModsBack`, `scopeIn`, `searchWide`, `searchNarrow`, `subOf`, `moreIn`, `countsScope`,
    `homeTitle`, … ); weg: `pbSearchAll`, `amOtherGroup`, alte `pageAllMods`-Tabelle.
- `wiki/assets/sitebar.js`: Production/Dev-Umschalter nimmt Pfad + Query + Hash mit (aktualisiert vor dem Klick).
- `tools/zentrale-web/dev-overlay.js`: `itemOf()` versteht `/<mod>/items/<id>`.
- `tools/zentrale-web/nginx/wiki-one.conf`: Prod-Regel `location ~ ^/simple[a-z0-9_]+(/.*)?$` → `/index.html`,
  `sub_filter` für `<base>` in `location /` und Dev; Dev verschachtelt `rewrite … /dev/index.html last`.
  Fehlende `data/*.js` bleiben 404 (kein allgemeiner Rückfall).
- `tools/zentrale-web/tests/test_nginx_zw06.py`: statische Prüfung + Integration (`/simpleend/items/x` 200 mit
  `<base`, `/data/fehlt.js` 404, `/dev/simpleend/` ohne Login 302, mit Login 200 + `<base href="/dev/"`).
- `tools/zentrale-web/README.md`: Operator-Schritte (nicht ausgeführt, Freigabe nötig). `wiki/README.md`: URL-Schema.
- Tests: `test_allmods.py`, `test_modpack.py` angepasst; neu `test_modpath.py` (Node: Pfad ↔ Route hin/zurück
  für alle Mods × Abschnitte, Alt-Links); neu `wiki/tests/nav74_ui.cjs` (Playwright: Startkacheln, Bereich,
  Zähler = Familiensumme, Sub-Mod-Filter, Breadcrumb, Suche im Mod + alle Mods, `?mod=`-Weiterleitung,
  Zurück-Taste, 1280/390 px, hell/dunkel, Kontrast, kein Querscrollen, keine Skriptfehler; Server einmal mit
  Regel+Basis (Pfad-Form) und einmal ohne (Query-Form)). Bestehende Harnesses wo nötig auf `?mod=simplebuilding`.

## Risiken
- Laufzeit der Indizes auf der Wurzel (alle Mods) – messen (Ziel < 1,5 s bis zur ersten Seite auf sb-test).
- Kollidierende Feature-Ids: Prosa-Links eines Sub-Mods auf `commands` landen im Familienbereich beim
  gleichnamigen Feature des Kopf-Mods (kleine Ungenauigkeit, vorher gar nicht verlinkt).
- `<base href>` ändert die Auflösung relativer `#anker`-Links: Klicks fängt der vorhandene Handler ab.
- nginx-Regel erst nach Besitzer-Freigabe aktiv → Pfad-Form bis dahin nur in Tests sichtbar.

## Verifikation
Plan-Abgleich; `python3 -m unittest discover -s wiki/tests`; `python3 wiki/generate.py --check`;
`tools/zentrale-web` Tests mit `SB_NGINX` (falls nginx auf sb-test); alle Browser-Harnesses + `nav74_ui.cjs`
auf sb-test via `/root/sbt b-nav74`; Precheck `python3.12 /root/gate-tools/precheck.py`. Screens nach
`/var/tmp/sb/pw/n74` (sb-test). Dev-Klick-Test erst nach Bündelung + nginx (nicht Teil dieses Auftrags).
