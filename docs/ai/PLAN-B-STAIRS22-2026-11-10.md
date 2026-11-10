# PLAN B-STAIRS22 (10.11.2026) – Live-Fix Backrooms-Treppen

Auftrag: N22 „Sofort“ (/root/b-auftrag-20261109.md), Besitzer-Kritik /root/sb-tools-a/wunsch-back22.md: Treppen in den Backrooms
„zerklüftet“ (lose Treppenblöcke) und Kopf stößt an die Deckenplatten. Klein fixen, kein Architektur-Umbau (BACK22-Konzept folgt).

## Ist-Stand (Basis b-bundle172-gen = Production-Stand b-back18)
- Treppen gibt es nur in Level 0 (Lobby), Treppenhaus-Zellen `LobbyGen.stairwell` → `LobbyGen.stairPiece`.
  Alt: 7 Birkentreppen (lx 1-7, y 2-8) frei schwebend (Luft darunter) + 1 Stufe nach Süden; Decke y 7 nur ab lx 4 offen.
- Nachgerechnet (Spieler 0,6 × 1,8, Stufenhöhe 0,6, echte Treppen-Boxen): Spieler bleibt bei x 3,75 / Füße 5,5 an der
  Deckenplatte (3, 7, 2) hängen – genau der gemeldete Bug. Ursache: über Stufe lx 3 nur 3 Blöcke bis zur Decke, beim Schritt
  auf die nächste halbe Stufe überlappt der Kopf noch die alte Spalte → je Stufe braucht es 3 freie Blöcke (2 Kopf + 1 Diagonale).
- Andere Ebenen: Level 1 (Lagerhalle) und Poolrooms einstöckig ohne Treppen; Level 2 Leiter-Schächte (Spalte y 2-9 Leiter,
  darüber Obergeschoss frei) und Eingangs-Schacht (3 × 3 offen) sind in Ordnung → nur per Test abgesichert.

## Umsetzung
- `LobbyGen.STAIR_STEPS`: ein durchgehender L-Lauf: Ost lx 2-7 (y 2-7, lx 1 = Podest in Flucht), dann 2 Stufen Süd (7,8,3),
  (7,9,4) bis Obergeschoss (Füße y 10). `STAIR_HEADROOM = 3`: über jeder Stufe 3 Blöcke frei (durch Decke, Bedrock-Platte,
  Teppich geschnitten). Ost-Lauf steht auf Sockel aus Birkenbrettern (`Piece.STAIR_BASE`) bis zum Boden; die Süd-Stufen
  liegen auf Deckenplatte/Bedrock. Alte Öffnung (y 7-9, lx ≥ 4) und Geländer y 10 bleiben.
- Abweichung vom Wortlaut „mindestens 2 Blöcke Luft“: 3 Blöcke, weil 2 beim Gehen nachweislich nicht reichen (s. o.).
- Entscheidung: keine Stufen-Formen (Ecke) nötig – Ost- und Süd-Lauf liegen auf verschiedenen Höhen.

## Test
- Gametest `simplelayers:stairwells_walkable` (Katalog `LayersTests.ALL` + Fabric-Adapter): 5 Seeds × 3 Treppenhäuser:
  je Stufe Sockel darunter, 3 passierbare Blöcke darüber; simulierter Spieler mit echten Kollisionsformen läuft vom Podest
  bis Obergeschoss (Füße y 10). Dazu Level 2: Leiterschacht frei bis Obergeschoss, Austritt 2 Blöcke frei.
- Vorab in Python gespiegelt (Vorher bleibt hängen, Nachher kommt an): Vorschau /root/sb-tools-b/previews/b-stairs22-preview.html.
- sbt: Compile + module-simplelayers fabric + standalone.
