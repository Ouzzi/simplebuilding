# Konzept Farbpinsel + Simple Respawn (2026-10-09)

## A. Farbpinsel (SimpleBuilding, Baukasten)
Idee: Blöcke in der Welt umfärben statt abbauen/neu craften.
- Item „Farbpinsel“ (Stock + Wolle/Feder, haltbar 256). Schleich-Rechtsklick auf Farbstoff in der Nebenhand bzw. auf Farbstoff-Stack im Inventar lädt die Farbe (Ladung = Farbstoff-Menge, 1 Farbstoff = 8 Striche).
- Rechtsklick auf färbbaren Block: Block wird zur gleichfarbigen Variante (Wolle, Teppich, Beton, Betonpulver, Terrakotta, glasierte T., Glas, Glasscheibe, Kerzen, Betten, Banner, Shulker, Mod-Blöcke mit Farbvarianten – über Tag `simplebuilding:dyeable_families`, Datapack-erweiterbar). Zustand (Ausrichtung, Wasser, Inhalte) bleibt erhalten.
- Ziehen bei gehaltener Taste: malt jeden Block, über den der Blick wandert (max. 1/Tick, Reichweite normal).
- Schwamm/Wasserflasche auf Pinsel = Farbe auswaschen; Schleich-Rechtsklick auf Block mit leerem Pinsel = Farbe aufnehmen (Pipette).
- Entscheidung Besitzer: (1) auch Holz-„Beize“ (Eiche→Fichte …)? (2) Kreativ unbegrenzt? (3) Partikel/Klang: Pinselstrich + Farbspritzer.

## B. Simple Respawn (neues Modul `simplerespawn`)
Allein spielbar (Prinzip Modul-Unabhängigkeit), Server- und Clientteil, Config je Welt.
- Tödlicher Schaden → Zustand **niedergeschlagen** statt Tod (nur wenn mind. ein anderer Spieler in der Welt/Dimension ist; Einzelspieler: normaler Tod, Config).
  - Spieler liegt (Schwimm-Pose), kriecht langsam, kann nicht angreifen/abbauen/Items benutzen, Sicht abgedunkelt + Herzschlag.
  - Ausblut-Timer 60 s (Config); Schaden im Zustand verkürzt ihn; danach normaler Tod (Drops wie Vanilla). Aufgeben-Taste = sofort sterben.
  - Kein Niederschlagen bei /kill, Void, Totem (Totem wirkt wie Vanilla zuerst).
- **Wiederbeleben:** Mitspieler hält Rechtsklick 5 s (Fortschrittsbalken bei beiden, Abbruch bei Schaden/Entfernung > 3 Blöcke).
  - Danach: 3 Herzen, 0 Hunger (Besitzer-Vorgabe), kurz Schwäche (Config), 3 s Unverwundbarkeit.
- Mobs ignorieren Niedergeschlagene größtenteils (Config), damit Rettung möglich ist.
- Anzeige: Name über dem Spieler rot + Restzeit, Chat-Meldung, Kompass-/Locator-Markierung für Mitspieler.
- Statistik/Fortschritt: „Erste Hilfe“ (jemanden wiederbelebt).
- Tests: GameTests für Zustandswechsel, Timer, Wiederbeleben-Werte (3 Herzen/0 Hunger), Totem-Vorrang, Einzelspieler-Fallback; Client-Smoke für Overlay.
- Entscheidung Besitzer: (1) Einzelspieler-Verhalten (normaler Tod vs. Niederschlagen ohne Rettung = sinnlos → Vorschlag: normaler Tod). (2) Timer 60 s ok? (3) Mobs ignorieren ja/nein. (4) Wiederbeleben mit Item (z. B. Goldener Apfel → sofort, 6 Herzen)?
