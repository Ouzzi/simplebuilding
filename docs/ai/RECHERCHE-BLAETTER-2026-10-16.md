# Recherche: gut wirkende Blatt-Darstellungen (N68/N4, 2026-10-16)

Nur Merkmale notiert, nichts kopiert. Quellen: Minecraft Wiki (Leaves, Azalea/Birch leaves), Better Leaves (Modrinth/PandaDEV), Motschen's Better Leaves, Jerm's Better Leaves Add-on, Leaves Plus (Modrinth), Vergleichsartikel "best realistic leaves packs".

## Vanilla 26.x (aus den Texturen/Wiki-Beschreibungen)
| Blatt | Merkmale |
|---|---|
| Eiche/Birke/Dschungel (getönt) | Graustufen-Textur, vom Biom getönt; unregelmäßige Blattflecken in 3 Helligkeiten, dunkle Fugen zwischen Flecken, 1-2 px große Löcher in Gruppen (nie ein Raster), Ränder zackig. Alle Pixel über Tönung eingefärbt. |
| Kirsche | Rosa, nicht getönt; kleine helle Blütenflecken (2-3 px) ungleichmäßig verteilt, dunklere Zweig-Pixel dazwischen, viele Löcher (luftig). |
| Azalee / Blühende Azalee | Dichte kleine Blätter, kräftiges Grün; blühend = einzelne rosa/violette Blüten-Pixelgruppen (2-4 px) auf ca. 10-15 % der Fläche, ungleich verteilt, nie in Reihen. |
| Mangrove | Dichter, dunkler als Eiche, wenige große Löcher, deutliche Blatt-Linien (diagonale Adern), getönt. |
| Blasse Eiche | Matt, entsättigt grau-grün, sehr wenige Farbstufen, ruhige Fläche, kleine hellere Spitzen am Rand; wirkt "still", kein Rauschen. |

## Ressourcenpakete / Mods mit gelobten Blättern
| Ansatz | Merkmale |
|---|---|
| Better Leaves (PandaDEV) | Dezent: Vanilla-Textur bleibt, zusätzliche Ebenen füllen die Krone; Umriss bleibt kubisch, nur Kanten lockerer. Lob: "sichtbar, aber nie störend". |
| Motschen's Better Leaves | Mehrschichtige Modelle (Zusatzebenen), 16x-Look; Dichte kommt aus Überlagerung, nicht aus mehr Textur-Detail. |
| Jerm's Better Leaves | Sehr leichtes Add-on, mehr Dichte, Vanilla-Farben. |
| Leaves Plus | Dichter, natürlicher, Vanilla+-Stil; Randblätter ragen etwas aus dem Würfel. |
| Faithful-Stil | Gleiche Struktur wie Vanilla (Flecken + Fugen), nur feinere Schattierung; Löcher bleiben an denselben Stellen. |

## Gemeinsame Merkmale gut gemachter Blätter
1. Blattflecken als zusammenhängende Formen (Linsen 2-4 px, diagonal), keine einzelnen Punkte.
2. Löcher in kleinen Gruppen, unregelmäßiger Abstand; Kachel nahtlos (Wrap), aber ohne erkennbares Raster.
3. 3 Helligkeiten: helle Oberkante je Fleck, Mittelton, dunkle Fuge; Adern nur sparsam.
4. Akzent (Blüten/Früchte) selten, in Gruppen, ungleich verteilt; nie pro Block gleich.
5. Innen dichter und dunkler (Schatten), außen luftiger mit zackigem Rand; ein Modell reicht, die Textur trägt den Unterschied.
6. Farbe über Biom-Tönung, Textur bleibt grau.

## Folgerung für Ash-/Duskwood
Besitzer-Kritik "Raster lila Punkte": Ursache war der lila Kristallwürfel in der Mitte JEDES Laubblocks (Raster) plus gekreuzte Tendril-Ebenen. Umsetzung: Vanilla-naher Würfel mit Blattflecken-Textur (außen luftig/zackig, innen dicht), Kristall nur noch an Blättern, die Knospe/Frucht tragen (drop=1/2), Akzent-Spritzer (Pollen) selten und unregelmäßig.
