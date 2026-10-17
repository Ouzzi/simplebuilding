# Plan B-WIKITIME: Beschaffungszeit und Diagramm "Zeit bis k Stück" (17.10.2026)

Ausgangslage: `docs/ai/PLAN-WIKI-BESCHAFFUNGSZEIT-2026-10-03.md` (keine erfundenen Stunden).
Entscheidung: Raten sind Szenario-EINGABEN des Lesers, nicht Fakten.

## Umsetzung
- `wiki/obtain_time.py`: aus den Loot-Pools je (Tabelle, Item) die Verteilung der Stückzahl je Öffnung
  (`timeStreams`, alle Pools einer Tabelle gefaltet; Mob-Drops, Verzauberungsbücher, bedingte Pools
  ausgelassen), Szenario-Vorbelegung aus `tools/devserver/sbdev/params.py` (`scenario`, als Annahme
  gekennzeichnet), Referenzrechnung (zusammengesetzter Poisson-Strom: Mittel, Median, 90 %).
- `wiki/generate.py`: hängt `timeStreams` und `scenario` an `obtain`.
- `wiki/index.html`: `timeStats` (gleiche Formel in JS), Eingabemaske (Raten gezielt/normal, k-Werte,
  optionale Zeitalter-Stunden; localStorage in try/catch; Zurücksetzen/Leeren), Zeile je Quellenkarte,
  Abschnitt "Beschaffungszeit" mit Tabelle + SVG-Diagramm (log. Zeitachse, k-Auswahl, Mittel/Median/90 %,
  Zeitalter-Linien nur bei Nutzereingabe) auf Item-Seiten, Übersicht auf der Loot-Seite. EN/DE.
- Zeitalter: nur belegte Voraussetzungen (Dimension aus Tabelle, Dorfbewohner-Stufe aus Handeln,
  Braustand/Trank in direkten Rezeptzutaten), daraus frühester Routenschritt und "vor/ab Braustand & Tränke".
  Keine Stundenlinie.
- Lücken statt Zahlen: fehlende/0-Rate, Quelle ohne Treffer, nicht modellierte Quellen (Mob, Bücher, Handel).

## Verifikation
`python3 -m unittest wiki.tests.test_obtain_time` (Referenzwerte), Wiki auf sb-test neu erzeugen,
`wiki/tests/time_ui.cjs` (playwright-core): Item-Seite + Übersicht, DE/EN, Desktop + 390 px, gesperrter Storage,
JS gegen dieselben Referenzwerte.

## Offen / bewusst nicht enthalten
Handel (Auffüllungen, Preisressourcen), Mob-Drops, Verzauberungsbücher: kein Zeitmodell. Reise-/Vorbereitungszeit
nicht modelliert. Nur Standardkonfiguration (Loot-Multiplikatoren ändern die Werte).
