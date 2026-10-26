# Simple Earth – schneller umrunden ohne zu kleine Kante (E29-8, 26.10.2026)

Ausgangslage (Besitzer): Eine kleine Kantenlänge (z. B. 2048) macht das Umrunden schnell, passt aber nicht zur normalen
Renderdistanz – unten sieht man 384 Blöcke weit echtes Terrain, oben ist die ganze Fläche nur 2048 breit, und ein Gebiet,
das man unten in Minuten durchläuft, ist oben ein Daumenbreit. Eine große Kante (65 536) passt zum Gefühl unten, aber man
fliegt Stunden bis zur Kante. Seit WG26-1 bewegt sich der Spieler echt (kein Parken), also bestimmt seine Fluggeschwindigkeit
das Umrunden.

## Vorschläge

| # | Idee | Wie | Vorteile | Nachteile |
|---|---|---|---|---|
| 1 | **Tempo mit der Höhe** | Über der Umschalthöhe wird die horizontale Bewegung des Spielers (Kreativflug, Elytra) mit einem Faktor ~ Höhe über Grund / Umschalthöhe multipliziert (Server erlaubt es, Chunks werden über der Umschalthöhe nicht geladen bzw. nur leer generiert). | Bildschirmtempo bleibt in jeder Höhe gleich; große Kante bleibt möglich; fühlt sich an wie Zoomen auf einer Karte. | Echte Position springt schnell durch die Welt → beim Absinken landet man weit weg (gewollt?); Server muss Chunk-Laden über der Höhe unterdrücken, sonst Last; Anti-Cheat „moved too quickly“ anpassen. |
| 2 | **Winkelgeschwindigkeit statt Blocktempo** | Im Orbit steuert man Grad pro Sekunde um die Erde (z. B. 3 °/s = 2 min pro Umrundung), die echte Position folgt (Kante/90° je Fläche). | Umrundung dauert unabhängig von der Kante gleich lang; physikalisch anschaulich (Orbit). | Wie 1: große Sprünge in der echten Welt; bei großer Kante sehr schnelle echte Bewegung. |
| 3 | **Orbit-Position getrennt von der Welt (Karten-Modus)** | Oben bewegt man nur einen Orbit-Cursor (wie früher das Parken, aber freiwillig per Taste/Modus), die Figur bleibt; beim Absinken Wahl: „hier landen“ oder „zurück“. | Kein Chunk-Laden, beliebig schnell, große Kante. | Genau das hat der Besitzer mit WG26-1 abgelehnt (Spieler soll sich frei bewegen) – nur als zuschaltbarer Modus sinnvoll. |
| 4 | **Zeitraffer-Flug** | Taste halten: Spielerbewegung ×N (z. B. 8–32), dazu Bewegungsunschärfe/Sternschlieren als Rückmeldung. | Bewusste Entscheidung des Spielers, sonst normales Tempo. | Wie 1 Server-seitig; Taste muss frei sein (Freelook liegt auf Alt). |
| 5 | **Kante abhängig von der Höhe (Zoom-Welt)** | Je höher, desto größer die effektive Kante der Darstellung (Detailstufen wie ein Kartenzoom), unten passend zur Renderdistanz. | Unten konsistent zur Welt, oben große Erde. | Die Erde „wächst“ beim Steigen – widerspricht der Würfel-Logik (Kante = fester Weltabstand); Kacheln je Höhe neu. |
| 6 | **Elytra-Schub im Orbit** | Über der Umschalthöhe gibt die Elytra einen dauerhaften Schub (keine Raketen nötig), Endtempo z. B. 300 b/s. | Vanilla-nah, keine neue Taste, Spieler bewegt sich echt. | Kreativflug bleibt langsam; Chunk-Generierung unter dem Spieler bei 300 b/s teuer (Ticket-Unterdrückung nötig). |

## Empfehlung
1 + 6 kombinieren: über der Umschalthöhe wächst das Tempo mit der Höhe (Faktor begrenzt, Server-Option), Elytra gleitet
dort ohne Raketen; dazu serverseitig keine Chunk-Tickets über der Umschalthöhe (die Welt darunter ist ohnehin verdeckt).
So bleibt die Kante groß (16 384–65 536) und eine Umrundung dauert wenige Minuten. Entscheidung beim Besitzer (Frage E29-8).
