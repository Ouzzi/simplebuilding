# MOB27 – Empfehlungen von Sitzung A (27.10.2026), noch vom Besitzer zu bestätigen

## Wo leben die Effekte?
- Empfehlung: **Simple Mobs** registriert Volatile/Deflection/Spiegel. simplelib enthält nur gemeinsame Helfer (Effekt-Registrierung, Tooltips), keine Spielinhalte – sonst hätte jeder Mod, der simplelib braucht, plötzlich Kampf-Effekte (Prinzip Modul-Unabhängigkeit).
- Brauchen später andere Mods dieselben Effekte (z. B. Besatz-Fähigkeiten), nutzen sie sie nur, wenn Simple Mobs geladen ist.

## Welcher Spieler „besitzt“ einen Spawnversuch?
- Gewicht je Spieler im Spawn-Radius = 1/Abstand (entspricht genau dem Besitzer-Beispiel 75 %/25 %). Liegt der Versuch im nächsten Viertel eines Spielers (Abstandsverhältnis ≤ 1:3), übernimmt dieser Spieler zu 100 %.
- Vorteil: nahe Spieler bestimmen fast immer, ferne Profis „färben“ nur selten auf Anfänger ab.

## Anfänger nicht überfordern
1. Stufe = Zonen-Stufe (Entfernung) + Spieler-Bonus, aber für die Stufe zählt höchstens Zonen-Stufe + 1 über dem eigenen Wert des gewählten Spielers.
2. Schonfrist für neue Spieler (vorhandener Neue-Spieler-Schutz t12pvp): erste X Spielstunden zählen nur Zonen-Stufe 0–1, egal wer in der Nähe ist.
3. Wird ein Anfänger als Besitzer gewählt, gilt nur SEIN Wert – Profis in der Nähe heben ihn nicht an.
4. Spieler-Wert wächst langsam (Spielzeit, Überleben) und sinkt beim Tod teilweise (z. B. −25 %), damit Pechsträhnen nicht eskalieren.
5. Config: Zonenradien, Kurve, Schonfrist, an/aus je Dimension; Anzeige des eigenen Werts (z. B. im Fragentisch oder per Befehl).

## Simple Resonance als Sub-Mod
- Inhalt: Spieler-Wert, Zonen-Berechnung, API (`ResonanceApi.levelFor(player, pos)`). Kein eigener Kampf-Inhalt.
- Simple Mobs benötigt Simple Resonance. Besatz-Fähigkeiten (Resonanz-Teil) wandern nach Simple Mobs; kosmetische Besätze bleiben in Simple Trims.

## Offene Fragen an den Besitzer
1. Spiegel-Effekt: feste 50/50-Chance und die Stufe erhöht nur die Weiterleitungs-Chance (z. B. 50/60/70/80/90 %) – ok?
2. Volatile: keine Blockschäden – ok?
3. Bekommen auch Spieler diese Effekte (Tränke/Leuchtfeuer) oder nur Mobs?

## Antworten Besitzer 27.10. spät
1. Spiegel: Stufe I 20 % Chance / 100 % Schaden an Angreifer … Stufe V 50 % / 200 %.
2. Volatile: keine Blöcke zerstören.
3. Spieler-Effekte über Tränke usw.: ja, aber erst Plan zur Freigabe.
