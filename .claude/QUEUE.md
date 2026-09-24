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

## Stand 2026-09-24 abends
- Welle 4 zusammengefuehrt (ae8b2d2), Gate laeuft; danach Push + Forge-Client neu
- OFFEN: Gegenprobe Hammer-Risse (Aufwertungsstand) nachholen
- NEU: Forge-Testziel (mind. Server-Gametests :forge:runGameTestServer o. ae. in run.py, Katalog-Paritaet; Regressionen: Paket-Handled, Bus-Registrierung, Buecher-Property, Vorschau)

## Stand 2026-09-24 21:30 - Gate Welle 4: 1806/1820, nur Client NeoForge 26.2 rot
- ERLEDIGT: Aufwertungs-Fortschritt nicht mehr als Aktionsleisten-Text (1cd5799)
- IMPLEMENTIERUNG offen:
  1. Client NeoForge 26.2: Oktant Pos1 nach Strg+Mausrad z=19 statt z=20 (client-bootstrap)
  2. Client NeoForge 26.2: SledgehammerUpgrades.showsUpgradeHint true, Test erwartet false (item-rendering, Diamant-Hammer + Netherit-Nugget auf verstaerktem Ofen)
  3. Forge-Testziel (Server-Gametests in run.py + Katalog-Paritaet)
  4. Rucksack-Sortierung: pruefen, ob vorbereitet/umgesetzt
- TESTEN danach (einmal gebuendelt): Gegenprobe Hammer-Risse, volles Gate, Push, Forge-Client neu

## Spaeter (auf Wunsch des Besitzers, nicht jetzt)
- Mehrere Mods im selben Repo: build-logic + framework/ herausziehen, mods/<name>/, Werkzeuge mod-faehig; erst nach dem Polishing von simplebuilding

## Stand 2026-09-24 22:15
- ERLEDIGT (9c80373, 7848f74): NeoForge-26.2-Clientfehler = Testtreiber-Rennen (Paket-Barriere), Forge-Testziel forge-262 (338/340, 2 bekannte Luecken)
- LAEUFT: Barriere fuer die uebrigen Client-Treiber; Forge Trade-Config-Schalter; Forge ItemAutomation (Rohre)
- DANN TESTEN: Gegenprobe Hammer-Risse, volles Gate (ruhige Maschine, Forge-Client aus), Push

## Welle 5 (2026-09-24 22:40) - erst umsetzen, dann testen
- ERLEDIGT: Vanilla-Maschinen/-Lager im Creative-Tab "Maschinen & Lager"
- Schwebender Sand bekommt Hitbox (Besitzer-Entscheid), Test + Wiki anpassen
- Neues Item Enderquarz (ender_quartz): Werkbank 1 Astralit-Staub + 1 Nihilith-Splitter + 1 Quarz -> 2
- Drei Paletten wie Endstein+Purpur (Block, Ziegel +Treppe/Stufe/Mauer, poliert +Treppe/Stufe/Mauer, Saeule, gemeisselt): Astralit, Nihilith, Enderquarz (lila)
- Gemeisselt: Astralit = Shulker-Motiv, Nihilith = Enderman-Motiv, Enderquarz = Enderdrache
- ALLE Paletten-Texturen neu im Vanilla-Stil (weiche Schattierung wie Endsteinziegel/Purpur)
- Steinmetz wie Vanilla fuer alle drei; Umfaerben an der Werkbank: 8 Endstein-/Purpur-Variante + 1 Material -> 8

## Welle 6 (2026-09-24 spaet)
- ERLEDIGT: Welle 5 (Paletten, Enderquarz, Hitbox schwebender Sand), Tab-Test fuer Vanilla-Gegenstuecke (3b804f3)
- LAEUFT: Dev-Mods JEI/Jade/Mouse Tweaks/AppleSkin (+Mod Menu) nur in Dev-Laeufen, nicht in Testlaeufen
- LAEUFT: JEI-Plugin mit In-World-Umwandlungen (eine Quelle mit den Wiki-Daten)
- LAEUFT: recommends/suggests bzw. optionale Abhaengigkeiten in allen Metadaten, Pflicht-Abhaengigkeiten pruefen, docs/PUBLISHING.md
- Besitzer bestaetigen: Paletten-Rezeptkette (Grund->poliert->Ziegel), 4 Material -> 1 Grundblock, Purpurblock-Umfaerben nur Enderquarz
- DANN TESTEN: Gegenprobe Hammer-Risse + Levitations-Haelfte Sand-Test, volles Gate, Push
