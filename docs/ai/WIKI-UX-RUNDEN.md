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
