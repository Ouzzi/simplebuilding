# Konzept: MineTracer/Ledger-Vorteile in Simple Tweaks T10 – B, 19.10.2026

Status: Konzept, Umsetzung erst nach Besitzer-Freigabe. Nur Dokument.

## Ziel
Funktionsvergleich Ledger / MineTracer gegen T10 (Block-/Truhen-Protokoll) und Übernahme der sinnvollen Vorteile in `modules/simpletweaks/.../admin/blocklog/`.

## Ist-Zustand T10 (Plan `PLAN-TWEAKS-ADMIN-2026-10-17.md`, Code `BlockLog`, `BlockRecorder`, `Inspect`, `Rollback`, `BlockLogCommands`, ca. 540 Zeilen)
Erfasst Abbau (mit BE-Daten bis 32 KB), Setzen, Container nehmen/legen. TSV-Tagesdateien je Region `<world>/simpletweaks/blocklog/<dim>/<rx>_<rz>/<tag>.tsv`, gepufferter Schreib-Thread (Flush 5 s), Aufbewahrung 14 Tage, `/inspect` (Seiten), `/tweaks rollback <player> <time> [radius]` mit Obergrenze 20000. `autoOffWith`: `ledger`, `minetracer` schalten T10 ab. Config: `AdminConfig.BlockLog`.

## Vergleich (Quellen: öffentliche Mod-Seiten auf Modrinth/CurseForge/Moddex; Angaben dort teils uneinheitlich)
| Feature | Ledger | MineTracer | T10 | Übernehmen? |
|---|---|---|---|---|
| Block abbauen/setzen | ja (hunderte Ereignisse) | ja, mit vollem NBT | ja (BE bis 32 KB) | – |
| Container (Truhe, Ofen, Trichter, Shulker) | ja | ja, mit Anzahlen | Slots mit BlockEntity | ja: Trichter/Loren-Lücken prüfen |
| Schilder (alt/neu Text) | – (nicht bestätigt) | ja | nein | ja (klein, nützlich) |
| Tötungen/Mob-Kills | ja (Entity-Ereignisse) | ja (kill) | nein | später |
| Weitere Ereignisse (Explosion, Feuer, Flüssigkeit, Item-Wurf, Kommandos) | ja, viele | teils | nein | Explosion/Feuer ja, Rest später |
| Filter (Spieler, Zeit, Radius, Aktion, Objekt/Item) | ja | ja (user, time, range, action, item) | Spieler+Zeit+Radius | ja: Aktion- und Item-Filter |
| Lookup-Befehl mit Seiten | ja | `lookup`, `page` | nur `/inspect` | ja: `/tweaks lookup` |
| Inspector (Klick) | ja | `inspector` | `/inspect` | – |
| Rollback | ja | ja mit Filtern | ja (Spieler+Zeit+Radius) | ja: Filter gleich wie Lookup |
| Restore (Rollback rückgängig) | ja (üblich) | nicht bestätigt | nein | ja: Undo-Puffer der letzten Aktion |
| Vorschau (Dry-Run, Anzahl) | teils | nicht bestätigt | nein | ja: `--preview` mit Zählung |
| Rechte je Befehl | Berechtigungsknoten | ein Knoten je Befehl | zwei Stufen (permission, rollbackPermission) | ja: Stufen je Befehl feiner |
| Speicher | SQLite (MySQL/Postgres/H2 per Erweiterung) | nicht bestätigt (vermutlich Datei/DB) | TSV-Tagesdateien | siehe Abschnitt Speicher |
| Lizenz | LGPL-3.0 | nicht bestätigt | eigen | nur Funktionsliste, kein Code |
| Mod-Verträglichkeit (Fake-Player, Maschinen) | Quellen-Kennung | nicht bestätigt | Spieler-UUID | später: Quelle `#mod` als Akteur |

Hinweis: MineTracer ist nach eigener Seite noch in Entwicklung; Angaben vor Übernahme gegen die Projektseite prüfen.

## Speicher/Format
TSV hat den Vorteil: kein JDBC, keine Abhängigkeit, löschen ganzer Tage, Mod bleibt allein spielbar. Nachteil: Abfragen über große Zeiträume/Regionen lesen Dateien. Empfehlung: bei TSV bleiben, ergänzen um (a) pro Tagesdatei eine kleine Kopfzeile mit Min/Max-Position zum Überspringen, (b) Akteur-Typ-Spalte (Spieler/Mod/Welt), (c) neue Aktionskennungen (`S` Schild, `K` Kill, `X` Explosion). Format ist abwärtskompatibel (Parser ignoriert unbekannte Aktionen). SQLite nur falls Abfragen messbar zu langsam werden (Messung in Stufe 1).

## Befehle (Vorschlag)
`/tweaks lookup <filter>` (Seiten), `/tweaks rollback <filter> [--preview]`, `/tweaks restore` (letzter Rollback), `/inspect` bleibt. Filter: `player:`, `time:`, `radius:`, `action:`, `item:`, `block:`, `dim:`.

## Zielmod und Stufen
Alles in **Simple Tweaks** (T10 existiert dort; Admin-Teil bleibt allein spielbar, keine Abhängigkeit von Ledger/MineTracer). Gemeinsames (Zeit-/Filter-Parser) bleibt in `admin/`, nur wenn ein zweites Modul es braucht nach simplelib.
1. Lookup + Filter-Parser (Aktion, Item, Block, Dimension), Rollback nimmt dieselben Filter, `--preview`.
2. Restore (Undo-Puffer, Größe begrenzt, nur im RAM oder letzter Lauf in Datei).
3. Neue Ereignisse: Schilder, Explosion, Feuer; Trichter/Loren-Container.
4. Kill-Protokoll, Akteur-Typ Mod/Maschine.
5. Optional: SQLite-Backend hinter Schnittstelle, falls Messung es verlangt.

## Config
Je Ereignisgruppe ein Schalter (`blockLog.signs`, `explosions`, `fire`, `kills`; neue Standard aus, damit die Platte nicht überläuft), `rollbackPreviewDefault`, `restoreKeep`, Stufen je Befehl (`lookupPermission`, `restorePermission`). `autoOffWith` bleibt (Ledger/MineTracer geladen → T10 aus).

## Server/Client, Sicherheit, Performance
Nur Server. Nur Aufzeichnen in Queue (Server-Thread), IO-Thread schreibt; Ereignisse mit hohem Aufkommen (Feuer, Flüssigkeit) nur mit Drossel und Limit pro Tick. Rollback bleibt auf Obergrenze und prüft „noch derselbe Block“. Spielernamen sind Personendaten: Aufbewahrung 14 Tage Standard, Löschbefehl für einen Spieler (`/tweaks purge <player>`) für Datenschutz auf öffentlichen Servern.

## Risiken
Plattenverbrauch bei neuen Ereignissen, Rollback gegen Mod-Blöcke mit eigenen BlockEntities, Kollision mit Claims (Rollback in fremden Claims nur Admin), Kill-Protokoll mit Maschinen-Spam.

## Tests
Parser (Filter), Lookup findet Schild/Explosion, Preview zählt ohne zu ändern, Restore stellt wieder her, Alt-Dateien (nur B/P/T/D) werden gelesen, Schalter aus = keine Datei, Retention, Performance-Test mit 1 Mio. Zeilen (Lookup unter Zeitgrenze).

## Offene Fragen an den Besitzer
1. Speicher? Empfehlung: bei TSV bleiben, SQLite nur nach Messung. | Alternative: gleich SQLite (schnellere Abfragen, neue Abhängigkeit).
2. Welche neuen Ereignisse zuerst? Empfehlung: Schilder, Explosion, Feuer. | Alternative: zusätzlich Kills und Flüssigkeit von Anfang an.
3. Restore (Rollback rückgängig)? Empfehlung: ja, letzter Lauf. | Alternative: nein, Rollback bleibt endgültig mit Preview.
4. Aufbewahrung/Datenschutz? Empfehlung: 14 Tage Standard plus `purge <player>`. | Alternative: unbegrenzt, nur manuell aufräumen.
5. Rechtestufen? Empfehlung: je Befehl eigene Stufe (lookup 2, rollback/restore 3). | Alternative: wie jetzt zwei Stufen.
