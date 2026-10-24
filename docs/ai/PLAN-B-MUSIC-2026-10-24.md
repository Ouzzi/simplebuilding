# Plan b-music24 (24.10.): NA24-8 Simple Music, Beispiel-Notenblaetter + Player-Fehler

Ist: NoteCode-Parser (pure Java) und Server-Player SheetPlayback. Parser per javac-Harness geprueft (Akkorde, x-Bloecke
geschachtelt, Wiederholung, Padding, Alias, Notenbereiche je Instrument): kein Fehler gefunden.
Fehler (Player): SheetPlayback uebergab `range * volume`; Vanilla begrenzt die Lautstaerke auf 1 und nutzt Werte > 1 nur
fuer die Reichweite -> jedes `@vol` ab 0,34 klang wie volle Lautstaerke. Fix: leisere Toene mit eigener Lautstaerke
(Reichweite 16), volle mit der konfigurierten Reichweite (`SheetPlayback.volume`).
Beispiele: `data/simplemusic/examples/*.txt` (9 Stuecke, Quelle in der ersten Kommentarzeile), `SheetExamples`, Kreativ-Tab
"Simple Music" gibt signierte Blaetter aus.
- Eigen (3): First Steps (einfach), Redstone Walk (mittel: Melodie, Bass, Schlagzeug, Bloecke), Nether Dawn (komplex).
- Gemeinfrei (6): Alle meine Entchen (Volkslied), Ode an die Freude (Beethoven 1824), Greensleeves (trad., 16. Jh.),
  Fuer Elise (Beethoven 1810), Praeludium C-Dur BWV 846 (Bach 1722), Kanon in D (Pachelbel um 1680).
Tests: gametests `examples_parse`, `tone_volume` (+ Fabric-Adapter in ModuleGameTest); Target module-simplemusic-fabric-263.
