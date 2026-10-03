# Beschaffungszeit – belastbare Teilmenge und Konzept

## Entscheidung

Keine Stundenangaben und kein Diagramm mit einer vorgeblich belegten logarithmischen
Zeitachse. Loot-Chancen und Handelsauswahl geben Wahrscheinlichkeiten pro Ereignis,
keine erreichbaren Ereignisse pro Stunde. Spawnversuche liefern ebenfalls keine
Reise-, Such-, Ausbildungs- oder Kampfzeit des Spielers. `tools/devserver/sbdev/params.py`
kennzeichnet die nötigen Raten als Annahmen bzw. Schätzungen. Auch die dortigen
Zeitalter-Stunden sind keine aus Minecraft ableitbaren Grenzen.

## Umgesetzte Teilmenge

Item-Seiten und Loot-Übersicht zeigen für eindeutig belegte Bernoulli-Truhenquellen
die erwarteten Öffnungen bis zum ersten und sechsten Stück. Voraussetzungen:
genau ein möglicher Wurf, genau ein Stück bei Erfolg, keine weitere Quelle desselben
Items in derselben Tabelle und keine zusätzliche Opferbedingung. Der Parser verwendet
die ungerundete Wahrscheinlichkeit aus `ModLootTableModifications.apply`/`rareCore`.
Andere Quellen bleiben ohne diese Kennzahl; keine Umrechnung von gerundeten Prozenten.

Bei unabhängigen, gleichartigen Kisten mit Trefferchance p gilt für das erste Stück
die geometrische Verteilung und für k Stück die negative Binomialverteilung:
`E[N_1] = 1/p`, `E[N_k] = k/p`. Das ist ein Mittelwert, keine Garantie. Die Zahl sechs
ist lediglich der gezeigte Zielbestand, keine Spielkonstante. Standardkonfiguration;
Loot-Multiplikatoren oder deaktivierte Quellen ändern die Werte. Kein p=0 wird geteilt.

Belegbare andere Teilgrößen dieser Welle: `Ø Stück je Kiste = E[R] * w/W * E[C]`,
Handelsverfügbarkeit pro neu erzeugtem Händler/Stufen-Angebot und sichtbare
Pool-Zugehörigkeit. Tresor-Untertabellen bleiben getrennt: Ohne deren Aufrufhäufigkeit
ist ihr Erwartungswert nicht gleich dem Ertrag je vollständiger Tresoröffnung.

## Späteres Zeitmodell (Annahmen ausdrücklich nötig)

1. Quellen pro Item mit präziser Ereignisdefinition und Mengenverteilung verbinden.
   Pools derselben Kiste gemeinsam falten, Untertabellen nach Vanilla-Gewichten mischen.
2. Zwei benannte Szenarien „gezielt“ und „normales Spiel“ benötigen vom Benutzer
   vorgegebene oder gemessene Ereignisraten. Keine voreingestellten erfundenen Zahlen.
   Reise-/Vorbereitungszeit getrennt; vorhandene Ressourcen und Freischaltungen erfassen.
3. `tools/devserver/sbdev/model.py` kann nach dieser Eingabe die Verteilung für k
   berechnen. Das dortige Poisson-Modell setzt unabhängige konstante Raten voraus;
   Händlerauffüllungen, Preisressourcen und endliche Strukturen verletzen dies häufig.
   Bei strikt gleichmäßigen Bernoulli-Ereignissen mit Rate r gilt für den Mittelwert
   `E[T_k] = k/(p*r)` nur unter ausdrücklich festgelegter Start-/Ereigniskonvention.
4. Zeitalter als belegte Voraussetzungen darstellen (Rezeptzutaten, Dimension,
   Villager-Level), nicht als universelle Stundenlinie. „Vor Braustand & Tränke“ ist
   eine Spielroute, keine durch alle Seeds/Spielweisen erzwungene Reihenfolge.
5. Erst bei vorhandenen Raten Diagramm „Zeit bis k Stück“: k wählbar, positive Zeiten
   auf logarithmischer Achse, Mittel/Median/90-%-Quantil benennen; Null separat und
   unerreichbar/fehlende Daten als Lücke statt Zahl. Zeitalter-Linien nur als explizite
   Szenario-Eingabe. Gleiche Daten auf Item-Seite und Beschaffungsübersicht nutzen.

## Verifikation und offene Daten

Unittests prüfen Bernoulli-Grenzen, Original-JSON/Java-Belege und Handelsnachziehen.
Browsertests prüfen Anzeige, Links und beide Sprachen. Offen bleiben gemessene
Suchraten, Routen, Restock-/Ressourcenzeiten, Szenarien und deren Zeitalter-Annahmen;
deshalb bleiben die beiden vollständigen Zeit-/Diagramm-Queue-Punkte offen.
