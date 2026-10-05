# Hängematten im Testcenter (2026-10-05)

## Ist-Zustand und Umsetzung
Die Maschinenstation zeigt 16 Farben, aber fast nur kurze Z-Achsen-Matten an Zäunen,
zwei schräge Matten und eine Stock-Matte. TcCanvas.hammock und TcOp.Hammock bleiben
der gemeinsame Bauweg. Die automatische Abschnittsplanung berücksichtigt ihre Zellen.

Die bisherige Reihe wird rechts von Maschinen/Kolben/Auto-Schmied durch ein Raster
mit sechs Spalten und neun Blöcken Abstand ersetzt. 22 beschriftete Beispiele:
gerade X/Z und diagonal 45 Grad jeweils 2/3/4 freie Zellen; 3:1, 4:1, 5:2,
3:2, 4:3, -3:2; Holzpfosten; fünf Arten senkrechter Doppelstäbe; Netheritstab.
Die Farben laufen über alle 16 Varianten. Schilder nennen Versatz, freie Länge
entlang der Hauptachse, Anker und Farbe; EN/DE in beiden Ressourcenlinien.

Annahme: Netherit statt Kupfer-Blitzableiter erfüllt den gewünschten Metallanker.
Kupfer zieht Blitze von der bestehenden Eisenstab-Station ab, deren Test das
absichtlich ausschließt. Keine Änderung am Gameplay, keine Ports, kein Client.

## Dateien und Risiken
- TestCentreSections.java: Raster und kleiner gemeinsamer Beispiel-Bauschritt.
- TestCentreTests.java: vollständige Varianten, Schilder, Anker, Farben,
  kollisionsfreie Zellen und Abschnittsgrenzen prüfen; beim echten Neubau
  die BE-Verknüpfung jeder Matte mit HammockLayout.intact prüfen.
- Vorhandene Aufbau-/Szenario-Tests erweitern, keine neuen Test-IDs nötig.
- Vier Sprachdateien, Queue und dieser Plan.
- Negative X-Richtung braucht einen verschobenen Startanker; Stäbe brauchen
  zwei gestapelte Blöcke. Abstände müssen auch Seil- und Tuchzellen schützen.

## Verifikation
Fabric-/NeoForge-26.3-Testzentrale zunächst gefiltert, dann volle Serversuiten:
`python tools/testrunner/run.py --targets "fabric-263,neoforge-263"`.
`gradlew.bat check -q -PskipWiki`, 26.2 Fabric/NeoForge und Forge-26.3-Compile.
Wiki generieren und prüfen. Keine Clienttests, kein Push; Commit auf aktuellem Branch.

## Ergebnis
- 22 getrennte Beispiele umgesetzt, alle 16 Farben. Netheritstäbe stehen auf Holz;
  so bleibt jeder Metallstab nach oben frei (bestehender Blitzstationsvertrag).
- Bestehende Szenario- und Neubau-GameTests erweitert. Auf beiden Loadern sind alle
  acht Testcenter-Tests einschließlich Item-/Block-Abdeckung bestanden.
- Vollständiger Lauf `2026-10-05T12-50-08Z-dd9b`: Fabric 966/966, NeoForge 966/966,
  `alles gruen: 1932/1932 bestanden, 0 rot`.
- Erster Filterversuch `*test_centre*` fand wegen fehlendem Namespace keine Tests
  (0/0, nicht grün). Der anschließende vollständige Lauf ersetzt diesen Fehlversuch.
- Wiki `--all` und `--all --check`: Exit 0, `wiki: up to date, everything documented.`;
  keine inhaltliche Änderung der generierten Daten.
- Keine Texturen geändert, daher keine Pixelkunst-Vorschau. Keine Clientprüfung;
  Sichtabnahme und Neubau in der Besitzerwelt bleiben beim Besitzer.
- `gradlew.bat check -q -PskipWiki :compileJava :neoforge:compileJava -Pforge263=true
  :mc26_3:forge:compileJava`: `GRADLE_EXIT=0`. 26.2-Kompilierung und Forge-26.3-
  Kompilierung grün; keine Forge-GameTests angefordert oder gestartet.
