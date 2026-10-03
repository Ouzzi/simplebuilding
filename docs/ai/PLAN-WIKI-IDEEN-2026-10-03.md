# Wiki-Ideen – Plan, 2026-10-03

## Umfang und Ablauf

Plan → Umsetzung → Test. Nur dieser Worktree und Branch `claude-gpt-wikiideas`, kein
Push und kein Master. Keine Gameplay-Änderungen, kein Datagen und kein Gradle nötig.
Vorhandenes `.serena/` bleibt unberührt.

1. Handelsdaten aus Vanilla-26.3-Trade-Sets, Tags und Trade-JSONs sowie den jeweiligen
   Mod-Tags zusammenführen. Vanilla-Basis als reproduzierbaren Wiki-Cache ablegen.
   Standardkonfiguration, nur ausgewählte Mod plus Vanilla, kein Trade Rebalance;
   diese Einschränkung sichtbar nennen. Unbekannte Bedingungen ausdrücklich offenlassen.
2. Vorhandenes `perChest` anzeigen. Gemeinsame Java-Pools mit allen betroffenen
   Tabellen kennzeichnen. Bei Tresoren Werte pro Aufruf der Untertabelle ausweisen,
   nicht ungeprüft als Wahrscheinlichkeit pro vollständiger Tresoröffnung.
3. Tabellenzeilen über `?f=<id>` fokussieren, gespeicherte Filter für das Ziel
   überwinden, Eltern öffnen, scrollen und kurz hervorheben. Unbekannte Seiten
   nennen den vollständigen angefragten Pfad und bieten Rückwege.
4. Config: Annotationen und ausführbare Validierung für Grenzen auswerten;
   `ConfigOptions.CLIENT_SIDE`, `APPLY_ON_RELOAD`, `RECIPES_ON_RELOAD` und
   `RESTART_REQUIRED` lesen. Nicht belegte Metadaten als unbekannt anzeigen.
5. Beschaffungszeit als separates Konzept mit kleiner belegbarer Teilmenge:
   Ereignisanzahl statt erfundener Stunden. Das bestehende `sbdev/params.py`
   bezeichnet Reise-, Such- und Spielraten ausdrücklich als Annahmen/Schätzungen.

## Handelsformel und Beleg

Am 2026-10-03 den lokalen 26.3-Client-Jar per `javap -c -p` geprüft:
`AbstractVillager.addOffersFromItemListingsWithoutDuplicates` entfernt einen
gleichverteilt gewählten Kandidaten, ruft `VillagerTrade.getOffer` auf und erhöht
den Angebotszähler nur bei einem nicht-null Angebot. Abgelehnte Kandidaten werden
ersetzt, bis `TradeSet.amount` erfolgreiche Angebote oder keine Kandidaten bleiben.
`TradeSet.CODEC` setzt `allow_duplicates` standardmäßig auf false.

Für einen Zieltrade mit unabhängiger Erfolgswahrscheinlichkeit p und K erfolgreichen
anderen Kandidaten ist die Aufnahmechance `p * E[min(1, d/(K+1))]`, wobei d die
gewünschte Angebotszahl ist. K folgt der Poisson-Binomialverteilung der anderen
Kandidaten. Begründung: Bedingt auf die erfolgreichen Kandidaten ist ihre Reihenfolge
gleichverteilt; die ersten d werden aufgenommen. Wenn alle anderen N−1 Kandidaten
sicher erfolgreich sind, ergibt sich `p * min(1, d/N)`. Bei allgemeinen zufälligen
Ablehnungen ist dieses einfache Produkt dagegen falsch. Die Verteilung lässt sich
mit einer kleinen dynamischen Faltung exakt bestimmen. Unbekannte Prädikate oder
Duplikat-Auswahl werden nicht mit dieser Formel ausgewertet.

Vanilla-JSON bestätigt: Bibliothekar level_3 amount=2; fahrender Händler buying=2,
common=5, uncommon=2. Poolgrößen werden aus gemergten Tags ermittelt, nicht getippt.
Weitere Mods, Datapacks, abgeschaltete Trades und experimentelle Pools verändern sie.

## Dateien, Risiken und Verifikation

`wiki/generate.py`, vorhandene Parser/kleine Hilfsdatei bei Bedarf,
`wiki/obtain_sources.py`, `wiki/index.html` (enthält JS/CSS), `wiki/tests`, generierte
Wiki-Daten und `.claude/QUEUE.md`. Beschaffungszeit-Konzept unter
`docs/ai/PLAN-WIKI-BESCHAFFUNGSZEIT-2026-10-03.md`.

Risiken: bedingte Trades, überlagerte Ressourcen, Java-Parsergrenzen, gerundete
Loot-Chancen, Untertabellen von Tresoren, persistente Filter und URL-Encoding.
Tests müssen diese Grenzen abdecken; fehlende Evidenz bleibt sichtbar unbekannt.

Prüfungen: Python-Unittests `python -m unittest discover -s wiki/tests`, vorhandener
Playwright-Test erweitert um Deep-Links/Spalten/Fehlerseite; venv-Python
`wiki/generate.py --all`, anschließend uv-Python `wiki/generate.py --all --check`.
Vor Commit vollständigen Diff und Plan-Abgleich lesen. Queue nur entsprechend dem
tatsächlich erreichten Umfang abhaken. Commit endet mit der vorgegebenen Co-Author-Zeile.

## Befunde und präzisierte Umsetzung

- Zweiter Vanilla-Abgleich: `VillagerTrade.getOffer` verwirft auch leere Ergebnisse
  des `given_item_modifier`. Vanilla verwendet etwa `filtered` + `discard` nach
  Verzauberungen oder Kartensuche. Der Cache hält diese Unsicherheit fest. Unbekannte
  Prädikate/Modifikatoren liefern keine exakten Prozentzahlen. Für bekannte Ziehungen
  ohne Duplikate werden sichere Grenzen berechnet: unbekannte Konkurrenz für die
  Untergrenze immer erfolgreich, für die Obergrenze immer abgelehnt; ein unbekanntes
  Zielangebot entsprechend 0/1. Monotonie des Auswahlverfahrens begründet diese Grenzen,
  auch wenn unbekannte Kontextbedingungen voneinander abhängen.
- Diamantkern: Poolgröße 23, zwei erfolgreiche Angebote; eigenes Prädikat 10 %.
  Belegbare Verfügbarkeit 0,9203839639–0,9648268398 %. Die anfängliche Rechnung mit
  erfolgreichen Vanilla-Modifikatoren ist nur die Untergrenze, nicht die exakte Chance.
- `WeightedEnchantFunction.run` gibt auch bei leerer Auswahl den ursprünglichen Stack
  zurück, verwirft ihn also nicht. Unbekannte Modifikatoren bleiben konservativ offen.
- Vollständige Loot-Zeilen-IDs enthalten Tabelle, Pool, Item, Verzauberung/Stufe und
  Mob-Bedingung; Bücher desselben Pools sind dadurch einzeln adressierbar.
- Config-Parser liest auch einseitige Untergrenzen und reflektive Trim-Validierung;
  unbekannte Obergrenzen werden nicht als 0 oder unendlich ausgegeben. Modul-Exporte
  ohne entsprechende Evidenz zeigen „Nicht ermittelt“.
- Bestehender Testfehler: der Handbuch-Beleg für `polished_ender_quartz_checker`
  verwies auf eine nicht vorhandene Overlay-Rezeptdatei. Die identische, vorhandene
  Basisrezeptdatei belegt das 2+2→4-Rezept; nur diesen Pfad korrigiert.
- `--all` übernimmt außerdem schon im Ausgangsstand vorhandene Manual-Änderungen in
  generierte Daten. Bestehende Hinweise auf Sprachschlüssel ohne Registry-Export bleiben
  sichtbar; kein Datagen/Gameplay geändert. Vom Generator entfernte, bereits vorhandene
  Alternativblock-Vorschaubilder werden unverändert erhalten, nicht als Löschung committet.
- RTK/rg waren im Prozess-PATH nicht verfügbar, Serena/Context7 nicht als Tools
  verfügbar. Scoped native reads und lokale Vanilla-Daten verwendet; keine externen
  Bibliotheks-APIs nötig. Playwright nur unter ignoriertem `build/wiki-browser` installiert.

## Abschlussprüfung

- venv-Python `-m unittest discover -s wiki/tests`: `Ran 47 tests` / `OK`.
- venv-Python `wiki/generate.py --all`: Exit 0, `Everything in the game has prose.`
- Danach uv-Python `wiki/generate.py --all --check`: Exit 0,
  `wiki: up to date, everything documented.`
- `node wiki/tests/ui_lists.cjs` mit `NODE_PATH=build/wiki-browser/node_modules`:
  `PASS: list navigation/reload, module isolation, reset, recipe query, items+blocks views,
  grouped recipes, sticky headers, mobile, deep links, trade/loot/config metrics,
  missing paths, escaping and unavailable storage`.
- `git diff --check`: keine Fehler. Bestehende Katalog-/Registry-Hinweise des Generators
  sind dokumentiert, kein Datagen/Gradle/Minecraft-Client nötig oder ausgeführt.
- Erledigt: Handel, Loot, Deep-Links/Fehlerpfad, Config-Spalten. Beschaffungszeit hat
  die erlaubte belegbare Teilmenge und das Konzept; Stunden-/Zeitalterdiagramm bleibt
  ausdrücklich offen. Keine Reise-/Spielraten erfunden. Kein Push, kein Master-Merge.
