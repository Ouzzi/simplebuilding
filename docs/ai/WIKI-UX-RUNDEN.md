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
