# Warteschlange (Stand 2026-09-24)

## Laufend
- P9-Gegenproben (18) auf beiden Linien / 4 Client-Zielen -> danach PLAN/Memory, Gate, Push von d63a419
- Recherche-Workflow 1 (Kolben, Rucksack, Rezepte, Wiki, Texturen, Buecher, Bauwerkzeuge, Tests)
- Recherche-Workflow 2 (Welt-Upgrade + Enderit-Blockstufe, Schmelzen, Erzgenerierung)

## Entscheidungen des Besitzers
- Unzerstoerbar = Haerte -1 ohne Technikbloecke (Tag), + verstaerkter Tiefenschiefer; Endportalrahmen per Config (Datei + Screen)
- Netheritkolben bricht Unzerstoerbares: verbraucht Redstoneblock + sich selbst, kein Drop
- Verstaerkter Kolben schiebt max. 1 Unzerstoerbares bei Redstoneblock-Platzierung, verbraucht den Redstoneblock
- Verstaerkter klebriger Kolben neu
- Netherit-Bloecke: 1 Klumpen pro Block, Aufwertung in der Welt (Hammer + Klumpen Nebenhand, ~5 s, Schlag/Sound/Partikel/Haltbarkeit je Sekunde); Enderit analog mit mehr Verschleiss; fehlende Enderit-Varianten neu (Trichter, Ofen, Raeucherofen, Schmelzofen, Kolben)
- Enderitkolben: bricht Unzerstoerbares in die Tiefe, Tiefe "angemessen dem Preis" (3 oder 6 - nach Recherche)
- Verstaerkter Koecher = Stufe zwischen Koecher und Netherit-Koecher; Rezept wie Buendel, Lederplatte Mitte, Diamantklumpen oben rechts
- Lederplatte (9 Leder); verst. Buendel: 1 Lederplatte statt 3 Leder, 1 Diamantklumpen statt 2 Kupferklumpen
- Rucksack 4 Stufen (9 / 18 / 27+6 / 36+14), Ruestung 1-4, Spalte 1 rechts (Netherit), Spalte 2 links (Enderit), Spalten anders markiert als Reihen (spaeter Spezialfelder)
  - Oeffnen nur getragen (Taste, z. B. B) oder abgestellt per Rechtsklick; Abbauen droppt mit Inhalt; Rechtsklick in der Hand = anziehen
  - Taste oeffnet exakt das Vanilla-Inventar, plus Rucksackfelder nur wenn getragen
  - Verzauberbar: Master Builder, Constructor's Touch (Hotbar nachfuellen), Deep Pockets (groessere Stapel), Funnel (wie Buendel)
  - Rezept Basis: oben Kupferklumpen-Faden-Kupferklumpen, Mitte 3 Lederplatten, unten 3 Eisengitter
  - Rezept verstaerkt: oben D-Lederplatte-D, Mitte Lederplatte-Rucksack-Lederplatte, unten 3 Lederplatten (Inhalt bleibt)
  - Netherit/Enderit per Schmiedetisch (Inhalt bleibt)
- Enderit-Schrott: Schmelzzeit 1 h pro Stueck (Netheritofen soll sich lohnen); lohnende Ofen-Upgrades, nichts Nerviges
- Astralit/Nihilith-Generierung beurteilen und anpassen
- Buecher: nur KONZEPT (Intro-Buch mit Rezepten der anderen, Verstaerkung, Werkzeuge, Enderit ...; teasern statt verraten)
- Bauwerkzeuge (Baustab, Oktant, verst. Buendel): Verbesserungsliste als FRAGEBOGEN
- Wiki: bleibt es bei Code-Aenderungen aktuell? Zustand herstellen, kostenloses Hosting vorbereiten
- Fehlende Texturen im Stil des Besitzers NEU zeichnen (keine Umfaerbungen)
- Fehlende Tests schreiben

## Reihenfolge / Parallelitaet
1. Recherche + Gegenpruefung (laeuft)
2. Synthese -> Umsetzungs-Workflow in Worktrees (parallel nach Bereichen), Builds erst nach P9
3. Zusammenfuehren auf master, Datagen beider Linien, Wiki, volle Laeufe, Gegenproben, Push

## Neu (2026-09-24 mittags)
- Rucksack-Rezept: Eisengitter -> Druckplatten; verst. Rucksack: 3 Lederplatten, 2 Diamantklumpen, Rucksack Mitte, oben Faden
- Alle Enderit-Items/-Bloecke uebernehmen Netherit-Eigenschaften (feuerfest usw.); Enderit ab Barren-Form (alles daraus) doppelt so lange bis Despawn
- Queue: Construction Light laesst Mobspawn zu, emittiert aber Licht
- Balancing: Trades, Kisten, Vaults, Angeln - lohnend, abwechslungsreich, nicht zu viel/wenig
- Quarz-Schachbrett: Nihilith- und Astralit-Variante
- Wiki: Rezepte vor Beschreibung, dann Drops/Trades/Used in; leere Kategorien unten ("no recipe"); neue Kategorie In-World-Transformation mit Zeit/Schlaegen/Haltbarkeit/Anzahl; (JEI-Rezepte spaeter auf Anfrage)
- Wiki: Vanilla-Items "How to get" je MC-Version + Crafting-Tree (Kinder-Rezepte, Varianten zusammengefasst), auch fuer Mod-Items; nur per Rechtsklick-Kontextmenue "Crafting tree"

## Neu (2026-09-24 12:20, Forge-Test des Besitzers)
- Vorschlaghammer 3x3 zu schnell (so schnell wie Spitzhacke 1x1) -> Verlangsamung nach Blockzahl (laeuft, Agent)
- Sneak = nur ein Block (laeuft)
- Forge: Abbau-Vorschau fehlt (laeuft)
- In-World: Schere auf Wolle -> 4 Faden (laeuft)
- Kreativmenue-Absturz = veraltete Klassen waehrend Gate-Kompilierung, kein Mod-Fehler
- ERLEDIGT (c06f30b, lokal): Hammer-Tempo /sqrt(n), Sneak = 1 Block, Forge-Vorschau per Mixin, Schere auf Wolle -> 4 Faden
- ERLEDIGT (6f4cdd2): Strip-Miner-Verlangsamung haengt nur an Player#getDestroySpeed(state); NeoForge/Forge bauen ueber getDestroySpeed(state,pos) ab -> dort vermutlich wirkungslos. In getDestroyProgress-Hook (BlockStateBaseMixin) verlegen + Test ueber den 2-Arg-Pfad

## Neu (2026-09-24 nachmittags, Welle 4)
A Inventar/Rucksack: Shift-Klick nahtlos (Hotbar -> Inventar+Rucksack in logischer Reihenfolge, keine Unterscheidung), spaeter Sortierung; Optik ohne harte Kanten; 3D-Rucksack auf dem Ruecken
B Forge: Buchtexturen fehlen (enchant_type); Enderit-Oefen nehmen keine Trichter-Eingaenge (alle Enderit-Maschinen pruefen)
C Hammer-Aufwertung: Bogen-Spannen bis kurz vor Schlag; sichtbarer Aufwertungsstand bei Abbruch; Neige-Indikator wie Meissel
D Astralit/Nihilith-Baubloecke (Endstein/Purpur-Palette); mehrere Creative-Tabs (Werkzeuge+Verzauberungen+verzauberbares / Bloecke / Materialien ...); Erzsensor-Nerf + Reichweiten-Upgrade + subtiles Funkel-Overlay am Item-Rand in Blockfarbe

## Stand Welle 4 (Wochenlimit erreicht, Agenten abgebrochen)
- B Forge-Buecher + Trichter/Rohre: FERTIG, auf master (f46adea), nicht gepusht
- A Rucksack: 4 Commits auf worktree-agent-a0bd5d59d2395c3ec, Client-Laeufe waren noch offen
- C Hammer-Aufwertung: 3 Commits auf worktree-agent-a938103e7e8c48724, letzte Client-Gegenprobe (Risse) offen
- D Bloecke/Tabs/Erzsensor: 2 Commits + 2 uncommittete Dateien auf worktree-agent-a8baa63b7fe733bce, Laeufe offen
- Danach: zusammenfuehren, Datagen+Wiki, volles Gate, Push, Forge-Client neu starten
- Test-Luecke: Forge hat kein Testziel (Paket-Fix, Bus-Fixes, Vorschau, Buecher nur per Hand geprueft)
