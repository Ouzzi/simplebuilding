# Wiki UX – Runden (N73, 2026-11-17)

Auftrag N73: das Wiki (öffentlich https://wiki.simplemods.de/ und Dev https://wiki.simplemods.de/dev/) in mindestens
fünf Runden durchgehen – je Runde „was stört / was fehlt / was ist überladen“, umsetzen, Screens vorher/nachher.
Leitbild: Einfachheit, nichts überfüllt, Ruhe wie im Nerion Wiki. Branch `b-n73` (nur `wiki/`, `docs/`), ein Commit je Runde.
Guidebook/Questionary bleiben unberührt (N80, Branch `b-n80`).

**Referenz Nerion Wiki:** `nerionwiki.azarfane.com` liegt hinter einer Anmeldung; sichtbar war nur die Anmeldeseite.
Daraus übernommen: fast weißer, neutral-kühler Hintergrund, weiße Flächen mit dünner grauer Kante, keine Schatten,
keine farbigen Flächen, eine dunkle Akzentfarbe nur für die Hauptaktion, viel Luft, wenige Elemente je Fläche.

Screens: `/root/sb-tools-b/previews/n73/` (vorher = Dev-Stand 17.11., nachher = Branch über die Dev-URLs geladen,
Playwright `ctx.route` liefert `index.html`/`assets/*` aus dem Branch, Daten/Overlay kommen vom Dev-Server).

## Runde 1 – Navigation (N74, schon live auf Dev)

Umgesetzt in N74 (`docs/ai/PLAN-N74-WIKINAV-2026-11-16.md`): Mod-Dropdown und „Whole pack“-Schalter weg; Startseite mit
Mod-Kacheln (4 Super-Mods groß, 3 eigenständige klein, „In allen Mods nachschlagen“); Mod-Bereich mit eigener
Seitenleiste („← Alle Mods“, Mod-Kopf, Gruppen mit Familien-Zählern, Sub-Mod-Liste mit Filter); flache Adressen
`/<mod>/<abschnitt>/<id>`; Breadcrumb „Alle Mods › … › Seite“; Suche im Mod mit „In allen Mods suchen (N)“;
Modpack-Leiste nur noch auf „Mod-Struktur“.

## Runde 2 – Ruhe: Farben und Flächen

Durchgang (Dev + Prod, 1280/390 px, hell/dunkel): Startseite, Mod-Übersicht, Blöcke & Items, Suche, Konfig, Beute, Handel.
- **Stört:** beiger, warmer Grund wirkt schwer; grün getönte Flächen (aktiver Seitenleisten-Eintrag, Versions-Chip,
  „Benötigt“-Chips, Auswahl in der Schnellsuche, Kontextmenü); gewählte Filter-Pillen voll dunkelgrün gefüllt (die Zahl
  darin kaum lesbar); grüner 3-px-Balken oben auf jeder Kopf-Karte; Schatten unter allen Karten; Karten „hüpfen“ beim Hover.
- **Fehlt:** eine neutrale Auswahlfarbe, die in hell und dunkel funktioniert.
- **Überladen:** jede Fläche hatte Rahmen + Schatten + Farbe.

Umgesetzt (nur CSS in `wiki/index.html`):
- Palette hell neutral-kühl (`--bg #f7f8fa`, Kanten `#e1e4e9`), Akzentgrün unverändert (Kontrast ≥ 4,5:1 bleibt).
- `--shadow: none` – Karten, Kacheln, Kopf-Karte flach mit dünner Kante; Hover nur noch Kantenfarbe, kein Anheben.
- Neue Variable `--sel` (hell `#eef0f3`, dunkel `#2a2e34`) für Auswahl: aktiver Seitenleisten-Eintrag, Schnellsuche,
  Kontextmenü, markierte Tabellenzeile – Akzent nur als 3-px-Kante links.
- Gewählte Pillen: weiß mit Akzent-Rahmen und Akzent-Schrift statt Vollfläche. Versions-/„Benötigt“-/End-Chips ohne Tönung.
- Grüner Balken oben auf der Kopf-Karte entfernt.
Nicht angefasst: Guidebook-Seiten (`.gb-book` behält sein Papier), Questionary.

## Runde 3 – Kopfzeile, Mod-Übersicht, Startseite entrümpeln

- **Stört:** Kopfzeile trägt Version · MC-Linien · Datenstand neben Suche/Anmelden/Thema/Sprache; Übersicht doppelt:
  zweites Suchfeld in der Kopf-Karte (die Kopfzeile hat schon eins), Weg-Kachel „Alle Mods“ (steht in Seitenleiste und
  Breadcrumb), Zusatzzeile „Every item … generated from its own data“ unter dem Einleitungstext; Startseite: „…oder such
  einfach oben“, obwohl das Feld darunter steht; Kacheltext „Super-mod: bundles …“ direkt unter „Super-mod · 3 sub-mods“;
  „1 blocks & items · 0 recipes“.
- **Überladen:** SimpleBuilding-Übersicht listet alle 649 Anleitungen als Karten – Seite 29 000 px hoch.
- **Fehlt:** ein klarer Weg „alle Anleitungen“ statt der Endlosliste.

Umgesetzt:
- Kopfzeile nur noch Marke, Suche, Anmelden, Hell/Dunkel, Sprache; Version/MC/Datenstand stehen im Seitenfuß.
- Übersicht: Kopf-Karte = Symbol, Name, Chips, ein Satz, zwei Wege (Anleitungen, Rezepte); ohne zweites Suchfeld,
  ohne „Alle Mods“-Weg, ohne Zusatzzeile. Anleitungen: die ersten 9 Karten + „Alle 649 Anleitungen →“ (Erste Schritte).
  SimpleBuilding-Übersicht: 29 037 px → 2 426 px.
- Startseite: Text „Wähle einen Mod oder such direkt.“; Kacheltext ohne doppeltes „Super-mod:“; Zahlen ohne Nullen
  und mit Einzahl („1 Block oder Item“).
- Tests: `home_ui` – Wege jetzt 1–2 (statt 2–3); „+N more“-Prüfung klickt die Ausklappe der optionalen Zeile (war
  vorbestehend rot, weil inzwischen auch „Benötigt“ ausklappt). `ui_lists` startet mit `WIKI_CHROME` (wie die anderen).

## Runde 4 – Listen, Suche, Rezepte

- **Stört:** Suchseite zeigt alle Treffer auf einmal („copper“: 798 Zeilen) und hängt an jede Zeile das Abzeichen des
  Abschnitts, in dessen Gruppe sie schon steht („Guides“ × 82); unter jeder langen Liste steht dauerhaft „Ctrl + mouse
  wheel zooms this area“; „Alle Rezepte“ beginnt mit sechs gleichen Karten „#simplestorage:dyeable_storage“ (Name ist eine
  Tag-Id, `#` sortiert vor alle Buchstaben); Filter der Rezeptseite in einem eigenen Kasten über der Kartenliste.
- **Überladen:** Suchgruppen, Filter-Kasten + Zoom-Leiste + Hinweistext übereinander.

Umgesetzt:
- Suche: je Gruppe die ersten 8 Treffer, darunter „Alle N zeigen“ (aufklappen, kein Neuladen, alle Zeilen bleiben im DOM);
  kein Abschnitts-Abzeichen mehr in Gruppen (in den „Meintest du“-Vorschlägen der Fehlseite bleibt es, dort ist gemischt).
- Zoom-Leiste (`wiki/assets/zoomarea.js`): Hinweis als Tooltip der Leiste statt Dauertext.
- Rezeptfilter ohne Kasten – nur eine Trennlinie zur Kartenliste.
- „Alle Rezepte“: Karten ohne lesbaren Namen (`#mod:tag`) ans Ende – die Seite beginnt mit „Birch Cauldron, Bamboo Crate …“.
- **Offen (nicht geändert):** drei verschiedene Rezept-Zahlen (Seitenleiste 4 550 = Mod-Rezepte + In-der-Welt, Startseiten-
  Kachel 3 685 = nur Mod-Rezepte, Rezeptseite 6 674 = inkl. Vanilla). Braucht eine Entscheidung, welche Zahl gilt.

## Runde 5 – Seitenleiste und Telefon

- **Stört:** Wurzel-Seitenleiste wiederholt „ALL MODS:“ vor jeder Gruppe („All mods: Browse / Reference / Progress &
  settings“); Fußnote der Seitenleiste nennt zusätzlich `wiki/generate.py` (steht im Seitenfuß); Kopf-Karte zeigt je
  Minecraft-Linie und je Loader eine eigene Chip (6 Chips, auf dem Telefon 3 Zeilen); Telefon: 10 große Zahlen-Kacheln
  zweispaltig (~470 px), Kategorie-Karten mit 4–5 Zeilen Text, Tastenkürzel-Zeile im Fuß (ohne Tastatur sinnlos),
  Highlight-Namen brechen mitten im Wort („Sledgehamm|er“), Titel „SimpleBuilding Wiki“ in 2,1 rem.
- **Fehlt:** `<html lang>` blieb immer „en“ (auch auf Deutsch) – Vorlesen/Silbentrennung falsch.

Umgesetzt:
- Seitenleiste Wurzel: eine Überschrift „In allen Mods nachschlagen“, darunter die Gruppen ohne Präfix. Fußnote nur noch
  „Zähler: …“.
- Kopf-Karte: eine Chip „Minecraft 1.21.11 · 26.2 · 26.3“, eine Chip „Fabric · NeoForge“.
- Telefon (≤ 640 px): Zahlen-Kacheln dreispaltig und kompakt, Kategorie-Texte auf 2 Zeilen, keine Tastenkürzel im Fuß,
  kleinerer Titel/Innenabstand der Kopf-Karte. Highlight-Namen mit Silbentrennung statt Bruch an beliebiger Stelle.
- `document.documentElement.lang` folgt der Sprache (Start und Umschalten).
- **Offen (außerhalb `wiki/`):** die Dev-Leiste „KI-Queue“ (`tools/zentrale-web/dev-overlay.js`) belegt auf dem Telefon
  unten ~150 px und liegt über dem Inhalt – nur Dev, nicht Teil dieses wiki-only-Zweigs.

## Tests (Stand Runde 5, lokal mit Chromium 1248)

`home_ui` 12 Ansichten ok · `nav74_ui` 270 ok · `wiki24_ui` 444 ok · `zoomarea_ui` 130 ok · `time_ui` 126 ok ·
`enchanting_ui` 184 ok · Python-Unittests 124 OK. Vorbestehend rot (auch auf der Basis `claude-wave1`): `ui_lists`
(Spaltenköpfe der Konfig-Tabelle), `tzeditor_ui` („search hits with preview“). `generate.py --check` nicht nötig
(keine Daten geändert).
