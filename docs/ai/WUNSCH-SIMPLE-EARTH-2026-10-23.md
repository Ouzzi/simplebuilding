# Wunsch Simple Earth (Besitzer 23.10.2026, Warteliste – später umsetzen)

Sinngemäß vollständig festgehalten aus der Sprachnachricht. Inspiration: die Mod „Cosmic Horizons“ (macht es anders, gilt aber als sehr gut).

## Zwei Höhen
- **Aktivierungshöhe:** Ab hier ist der Spieler „überirdisch“/im Weltraum; seine Position wird ab jetzt anders getrackt. Der Punkt (x, z), an dem er die Aktivierungshöhe erreicht, wird zur **Mitte** des Erd-Würfels.
- **Render-Umschalthöhe:** Ab hier wird das Terrain unter dem Spieler nicht mehr normal gerendert, sondern als **Minecraft-Erde = Würfel**. Fliegt man senkrecht hoch, macht es keinen Unterschied (gleiche x/z, nur y ändert sich).

## Bewegung über dem Würfel
- Fliegt man z. B. nach Norden, rotiert der Würfel unter einem leicht, bis man an der **Kante** der unteren Fläche ist. In echten Koordinaten entspricht das dem Weg von der Mitte bis zum Rand dieser Terrain-Fläche (Flug dauert entsprechend).
- Fliegt man nur in eine Richtung, zeigt am Ende die **Kante** zum Spieler; die neu aufgedeckte nächste Würfelfläche ist das nächste Terrain.
- Fliegt man diagonal (z. B. Nordost), dreht sich die **Ecke** Richtung Mitte; bei 45° bis zum Rand liegt die Würfelecke („Ecke der Welt“) genau unter dem Spieler.
- Ecken/Kanten leicht abgerundet, im Stil der Inventar-UI-Abrundung – nicht zu stark.

## Terrain-Darstellung
- Aus grob geschätzten Chunk-Abfragen (ähnlich Chunkbase beim Weitzoomen) bzw. wie die Karten: pro Pixel 10/20/30/64 Chunks zusammengefasst (je nach Höhe), trotzdem so detailliert, dass man erkennt, wo welches Biom ist.

## „Tesserakt“-Effekte
- Die Welt ist unendlich: Fliegt man einmal 360° „um“ den Würfel, kommt man **nicht** an derselben Stelle heraus – jede neue Fläche ist neues Terrain. Rechenbeispiel: Kante nach 100 000 Blöcken, danach je 200 000 pro Fläche → nach vier Flächen ist man bei ~1 000 000 statt wieder bei 0,0. Zurück nur auf demselben Weg.
- Diagonal (schräg links) decken sich zwei neue Seiten auf; eine ganze Fläche würde „übersprungen“ → visuelle Lösung nötig (Besitzer offen für bessere Idee), einzigartig, tesserakt-artig.

## Himmel
- Der Mond ist näher und sichtbar, leicht würfelig. Die Sonne ist sehr weit weg und bleibt flach gezeichnet.

## Anspruch
- Stilistisch einfach und simpel, aber riesiger, cooler Effekt; poliert.
- Auftrag: recherchieren (Cosmic Horizons u. a.) und ein einzigartiges Konzept vorlegen.
