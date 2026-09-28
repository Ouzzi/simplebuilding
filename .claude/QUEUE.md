# Warteschlange SimpleBuilding (Stand 2026-09-25 spaet, alles gepusht)

Regeln: Umsetzung mit Teiltests (Kompilierung + gefilterte Server-Tests), volles Gate einmal am Ende einer Welle, gruen = direkt pushen.
Verlauf im Detail: git log.

## Erledigt (alle gepusht, letztes Gate 2345/2345 auf 5eb3bbb)
- [x] Welle 1-3: Kolben-Durchbruch, Rucksack (4 Stufen), Enderit-Maschinen, Hammer-Aufwertung in der Welt, Lederplatte, Koecher, Loot-/Handel-/Angel-Balancing, Quarz-Schachbretter, Wiki-Umbau
- [x] Forge-Dev-Instanz laeuft (Paket-Fix, Bus-Registrierung, Buecher-Texturen, HUD, Vorschau, Forge-Testziel, Trade-Schalter, Rohre)
- [x] Vorschlaghammer: Tempo nach Blockzahl, Sneak = 1 Block, Umformen findet Bretter/Ziegel/*_block
- [x] Schere auf Wolle -> 4 Faden; Oktant waschen behaelt Verzauberungen/Haltbarkeit
- [x] Strip-Miner-Verlangsamung auf allen Loadern
- [x] Rucksack: Shift-Klick nahtlos, Fenster ohne harte Kanten, 3D auf dem Ruecken, faerbbar (auch als Block), Rezept mit schweren Waegeplatten
- [x] Buendel faerbbar + Offen-Ansicht; Oktant-Figur-Taste getrennt; toter Netherit-Trichter entfernt
- [x] Hammer-Aufwertung: Ausholen, Fortschritt als Risse am Block, Hinweis-Neigung, kein Aktionsleisten-Text
- [x] Paletten Astralit/Nihilith/Enderquarz (Steinmetz, Umfaerben, Quarz -> Enderquarz), gemeisselte Texturen (9 Runden), schwebender Sand mit Hitbox
- [x] Kreativ-Tabs SimpleTools/Blocks/Materials/Machines + SimpleEnchants (Dev), Vanilla-Maschinen im Tab, Zeilen-Layout (Konzept SimpleMachines)
- [x] Erzsensor: Durchdringung nach Materialdichte, Reichweiten-Upgrade, Randschimmer
- [x] JEI-Plugin (In-World-Umwandlungen inkl. Besatz-Vorlage + Oktant waschen), Dev-Mods (JEI/Jade/Mouse Tweaks/AppleSkin/Mod Menu), Metadaten + docs/PUBLISHING.md
- [x] Upgrade-Kosten 2x Vanilla, Kupferwerkzeuge aufwertbar, Baustab-Upgrade kostet Kern der Zielstufe
- [x] Besatz-/Resonanz-Balancing, Vanilla-Attribute, Radiance-Licht (Rahmen/Staender/Mobs) + Partikel, UI im Vanilla-Stil
- [x] Blaupause komplett (DSL, 3-Spalten-Editor, Hilfe, Einfuege-Suche, Beispiel-Knopf je Biom, Autospeichern, Scan nach Oktant-Form am Kartografentisch, Kopieren, Bauen nur signiert, gestaffelter Bau, rote Fehlstellen mit Zwei-Klick, Wuerfel 16/32/48/64/128/256)
- [x] Wiki: Rezept-Tab mit Karten/Filtern/"alle Rezepte zu X", einklappbar, 3D-Bloecke, Key Feature Tempo
- [x] Texturen Enderquarz (Stern), Enderit-Barren/-Schrott/-Nugget
- [x] Test-Infrastruktur: Server-Ziele parallel, Paket-Barriere in allen Client-Treibern, Port-Werkzeug-Regeln

## Laeuft
- [x] Welle 9: MC 26.3 als dritte Linie (Fabric + NeoForge), alle 13 Ziele gruen, gepusht
- [x] Welle 12 26.3-Reste: Wiki-Umschalter 26.3, Erzdetektor-Kalibrierung aus 26.2-Welten (id statt Name), dunklere Trim-Variante, Jade/AppleSkin/Mouse Tweaks fuer 26.3, NeoForge-Upload als Beta

## Welle 10 (erledigt, auf master, noch nicht gepusht)
- [x] Ausstehende Gegenproben: Hammer-Risse (Client), Anstossen schwebender Bloecke am Sand, Vorwaerts-Umformen Bretter/Ziegel
- [x] Blaupause: Bauauftrag ueberlebt Logout/Neustart (fortsetzen), Materialliste fuer Mehrfach-Bloecke (Kerzen, Seegurken, Schneeschichten), Fehlstellen-Pruefung ohne 400.000er-Grenze
- [x] Kleinkram: ungenutzte Farbwerte des Entfernungsmessers entfernen, Platzhalter-Item aus /give-Vorschlaegen, leerer Tab-Platz des Dev-Tabs auf Fabric pruefen, Wiki-Texturreste aus 1.21.11-Laeufen

## Welle 11 (erledigt, gepusht a33b420b, Gate 3010/3010)
- [x] Visual Armor Trims: Muster auf Ruestungs-Icons (erst Mod-Ruestung, dann Vanilla), Farbe nach Material
- [x] Besatzmuster nachbessern (laeuft): Randpixel dunkler/ausgespart, exakt mittig (z. B. Flow-Brust), nur Overlays
- [x] Radiance-Partikel stark reduzieren; Tooltip "Radiance: 5" statt "5/5"
- [x] Texturen: Enderit-Aufwertung (Enderit-Farbe, Netherit innen + im Pfeil), Einfache Aufwertung (Gold-Stil, Eisenblock innen), Umbenennung "Basic Upgrade"/"Enderite Upgrade", Barren 2 px schmaler, Schrott 1 px breiter, Diamant-Kiesel schaerfer, Lederplatte schoener

- [x] Texturen Runde 7: zwei Alternativ-Sets fuer alle Enderit-Werkzeuge/-Waffen/-Ruestung (Icons + getragen), Hammer, Baustab, Meissel - Besitzer waehlt
- [x] SimpleTools: Vanilla-Werkzeuge/-Waffen/-Ruestungen aller Stufen, Reihenfolge Werkzeuge > Waffen > Ruestung > Geraete > Buecher, zeilenweise
- [x] SimpleMachines: Zeile Bauplanung (Blaupause, Kartografentisch, Oktant, alle Baustaebe)
- [x] Enderquarz-Schachbrett (Bodenblock)

- [x] Texturen Runde 8: Barren-Kante parallel, Aufwertungen = Umfaerbung der alten Besitzer-Umfaerbung, Enderit-Set B mit braunen Griffen uebernehmen
- [x] Texturen Runde 9: eigenes Buch-Icon fuer jede Vanilla-Verzauberung + Config-Schalter (Konflikte mit Ressourcenpaketen)
- [x] Rezepte: Erzdetektor + Echosplitter links/rechts vom Kompass; Oktant mit Gold-/Eisen-Waegeplatten und Blitzableiter
- [x] Experimentell: Hunger-Kosten beim Bauen mit Baustab/Blaupause (Kupfer 16^3 = 1/4 Balken, Enderit 128^3 = voller Balken), per Config abschaltbar
- [x] Doku docs/BAUWERKZEUGE-INTERAKTIONEN.md: Zusammenspiel Baustab/Blaupause/Oktant/Hammer/Meissel/Buendel/Rucksack mit und ohne Constructor's Touch, Verbesserungsvorschlaege, Vorschlaege fuer die Kerne -> danach Besitzer entscheidet, dann Wiki-Kapitel

- [x] Baustab (auf master): Linear/Bridge/Cover/Color Palette wirken, Ausrichtung wie ein Spieler, Rueckgaengig nur in derselben Sitzung, Oktant-Form fuellen, Dach-Modus
- [x] Koecher: 3D auf dem Ruecken (flach mit Tiefe); aktuelles Verhalten dokumentieren

- [x] Hunger beim Bauen entschaerfen: Freibetrag pro Vorgang (>=256 Bloecke), nur sichtbare Hungerleiste als Massstab
- [x] Texturen Runde 10: Barren wie Netherit (1 px niedriger, Glimmer), Werkzeug-Griffe wie Netherit mit lila statt schwarz + Glow, analog Hammer/Baustab/Meissel

## Welle 13 (erledigt, gepusht)
- [x] Config-Schalter: Vanilla-Buecher (gibt es), Mod-Buecher custom/vanilla, sichtbare Besatzmuster Vanilla-Ruestung, sichtbare Besatzmuster Enderit-Ruestung

- [x] 26.4-Snapshot-Linie vorbereiten (Fabric, Forge falls vorhanden), experimentell, blockiert das Gate nicht
- [x] Nahtloses Welt-Upgrade 26.2 -> 26.3: Audit aller Mod-Daten, tolerante Leser, Fixture-Rundlauftest, docs/UPGRADE-26.2-26.3.md

## Welle 14 (erledigt, gepusht): Simple Tweaks uebernehmen
- [x] Inventur + docs/SIMPLETWEAKS-UEBERNAHME.md (inkl. vollstaendiger Claim-Notiz, Claims NICHT portieren)
- [x] Port aller Druckplatten (Chunkloader, Elytra-Pad, Fly-Pad, Spawn-Teleporter + Modi), Spawn-/Erstbeitritt, Spawn-Elytra, XP-Kugeln, Laser, Echo-Kompass, Befehle, Config (jede Variante abschaltbar) - alle Linien/Loader
- [x] Enderit-Stufe nach Netherit, Netherstern-Stufe rueckt eins hoch (z. B. Enderite Elytra Pad IV, Fine Elytra Pad V); Enderit-Platte mit Zusatzfunktion
- [x] Echo-Kompass: Rezept Bergungskompass + Enderit-Kern + Netherit-Druckplatte links/rechts; Unbreaking-Bug fixen
- [x] Tests alle Linien; danach in simpletweaks Branch remove-ported-features (nicht gepusht)

## Welle 15 (erledigt, gepusht): Testzentrale
- [x] /sbtestcentre build: reproduzierbare Testwelt aus Code (Ruestung/Trims/Upgrades, alle Buecher, Werkzeuge plain+verzaubert, Meissel-Tuerme + In-World-Stationen, Lager, Bloecke, Maschinen-Demos, Erzdetektor-Feld, Essen, Oktant/Blaupause/Baustab-Modi, Versatility/Vein/Strip, Command-Block-Knoepfe)
- [x] Abdeckungstest: jedes Mod-Item/-Block steht in der Zentrale (neue Features fallen automatisch auf); docs/TESTZENTRALE.md; Regel: am Ende jedes Runs pruefen

## Welle 16 (erledigt, gepusht; Server-Gate 3096/3096, Client-Gate offen): Fehler + Optik aus dem Testen
- [x] Enderit-Besatzfarbe im Tooltip lesbar (#9A7BD8)
- [x] Bridge geht nicht; B-Taste schliesst Rucksack-Inventar nicht; Dach-Modus mit Enderit-Baustab; Testzentrale-Command-Blocks feuern doppelt/versetzt; Enderit-Kolben in der Zentrale pruefen
- [x] Koecher faerbbar und richtig dargestellt; Faerbe-Tönung sanfter, Stufen bleiben unterscheidbar
- [x] Enderit-Trichter-Item vanilla-nah; verst. klebriger Kolben mit Vanilla-Schleim
- [x] Enderit-Glimmer: Apfel, Karotte, Koecher, Rucksack, Buendel; Enderit-Maschinen und -Kolben im Barren-Look

## Welle 17 (erledigt, gepusht; Server-Gate 3110/3110)
- [x] Eigene Kolbenkoepfe je Stufe (+ klebrig); beim Kolben-Abbau Partikel, Block-Abbau-Sound + eigener Bohr-Sound
- [x] Erstbeitritt: Spawn-Teleporter und Pad standardmaessig 0 (zwei Config-Werte)
- [x] Enderit-Tweaks-Bloecke: neue Pixelart statt Umfaerbung
- [x] Audit: alle Auffaelligkeiten der Mod auflisten -> docs/AUDIT-2026-09-26.md (52 Punkte)
- [ ] Client-Gate (6 Ziele) sobald Besitzer-Client/-Server geschlossen; Testzentrale neu bauen

## Welle 18 (erledigt, gepusht; Server-Gate 3313/3313): Audit-Fixes P1+P2 (docs/AUDIT-2026-09-26.md)
- [x] A1 Blaupause/Oktant/Netzwerk (#2,#8,#9,#11 + Materialverlust)
- [x] A2 Werkzeug-Schutz (#1,#6,#7,#10 + Befehlsbloecke, Undo)
- [x] B Tweaks + Doku (#3,#4,#5,#16,#17,#20,#21 + kleine P3)
- [x] C Maschinen/Lager/Config (#12-#15,#18,#19, Portalrahmen standardmaessig aus)
- [x] Server-Gate, Push
- [x] Nach-Audit: N1 (P1 Hammer-Regression) behoben; N2-N16 in docs/AUDIT-2026-09-26.md

## Welle 19 (erledigt, gepusht; Gate 3523 Tests, Rest-Fehler behoben und gezielt nachgeprueft): Rest-P3/P4 aus Audit + Nach-Audit
- [x] W1 Blaupause/Oktant/Baustab: N2, N3, N8, N9, N13-N16, ShapeFill-Tests
- [x] W2 Kolben/Trichter/Lager: Brecher-Verschleiss (#23), Config-Schalter Fake-Spieler-Guard (N4), Loader-Guard-Tests (N5), N6, N7, #24, #36, #48
- [x] W3 Werkzeuge/Texte/Tasten: #25, #26, #27, #30, #37, #38, #39, #46
- [x] W4 Tweaks-Rest + Hygiene: #35, #51, N10-N12, #43-#45, #49, #52
- [x] wiki/manual.json: 6 doppelte Feature-Eintraege (welcome x4, building_wand, blueprint, enchant_storage_player, enderite_void_protection, configuration) mit abweichendem Text zusammenfuehren (Altlast aus JSON-Merges)
- [ ] Danach: Server-Gate, Push; Client-Gate wenn Besitzer-Spiel zu

## Welle 20 (laeuft)
- [x] Texturen (freigegeben, gepusht): Maschinen (Kolben, Oefen, Raeucherofen, Schmelzofen, Trichter) vanilla-naeher; Enderit-Block + andere Mod-Bloecke neu; Netherit-Griff fuer Netherit-Hammer/-Meissel/-Baustab; Enderquarz lesbarer; Laser-Textur
- [x] Echo-Kompass (gepusht, Gate 3649/3649): 3 s Aktivierung, Sounds/Partikel/FOV, 1. Nutzung -> kaputt, 2. Nutzung doppelt lang + Warnung -> zerbricht, eigene + kaputte Textur, Werkzeug-Tab, Rezept (N N/NRN/NEN, 6 Enderit-Nuggets), Mending: 1500 Punkte Aufladung, Glanz nur repariert; Velocity-Gauge-Rezept (Quarz oben in den Ecken, unten Kupfer-Kern)
- [x] Tweaks-Stufen (gepusht, Gate 3719/3719): Launchpad (4/8/16 Ladungen, doppelte Staerke je Ladung, Shift = alle Windladungen rein), Chunk-Loader (1 / 5 / 3x3), Upgrades kosten Druckplatten, Flypad Stufe 1 mit Elytra (+ ? offen); Rotator-Rezept + Perle in der Textur
- [x] Elytra-Pad 5 Stufen (Elytra+Vorlage 1x1, Diamant 5x5, Netherit+Vorlage 16x16, Enderit 32x32, 128x128); Flypad neu: 3 Stufen aus Enderit-Druckplatte (Kern+Vorlage, +Enderit-Platte, 2x Stufe 2), 4x4x6 / 8x8x12 / 16x16x24; Magnet-Rezept " R "/"I  "/"CIL" (laeuft im Stufen-Agenten)
- [x] Kupfer-Druckplatten: gewachste Varianten (Honigwabe, Axt schabt Wachs ab, wie Vanilla-Kupfer); Namen bleiben (Vanilla-Muster reicht); Kupferplatte: Abschalten dauert so lang wie Einschalten, Platte senkt sich sichtbar wie Vanilla (pressed-Modell); Creative-Tab aufraeumen + Vanilla-Druckplatten dazu (laeuft)
- [x] Echo-Kompass -> "Echo Sounder" (Name), Partikel weiter gestreut (Nutzung + Landung), keine Perle mehr noetig, Rezept + Nugget oben (NNN/NRN/NEN); Laser zuendet auch TNT, verliert auch beim normalen Zielen Haltbarkeit; Velocity Gauge QAQ/NCN/NKN (Kupfer-Nuggets); Kerne in Beutekisten sehr selten (Enderit-Kern besonders) (laeuft)
- [x] Trank-Pad (laeuft): Wurftrank auf Netherit-Pad speichern, Effekt beim Drueberlaufen 30 s / 60 s / 120 s; Stufe I Netherit-Druckplatte + Lohenkopf (Lohe durch geladenen Creeper), II Enderit-Upgrade, III Enderit-Kern; alte Flypad-Texturen in Netherit-Palette
- [x] Easter Egg (laeuft): letzte Stufe -> Stufe 1 im Schmiedetisch = "Don't do it" (Erfolg "What have you done?"), naechste "Seriously?", Stufen 3/4 ausdenken, 5 = Pad-Name verschleiert + Texteffekte, 2x staerker; + Netherit -> "Funny Stick" (Partikel); Erfolge fuer 2x-Stufe-5 und Funny Stick
- [x] Laser (Amethystlinse, gepusht, Gate 3614/3614): in den Werkzeug-Tab; Vanilla-Name, Punktgroesse, Abstand der Meterzahl, Rezept (Eisen-Kern/Amethyst/Eisen-U/Redstone), Eis/Schnee schmelzen, Brennbares entzuenden, Seelenfeuer, Lagerfeuer, kein Netherportal, Ladung statt Bruch, Amboss-Aufladen mit Redstone ohne Level (64 = voll)
- [x] JEI-Infoseiten fuer Items ohne Rezept (+ Test)
- [x] Blaupausen-Code: Formen und Variablen
- [x] 26.4 geprueft 2026-09-27: kein Forge/NeoForge/Cloth/Dev-Mod-Build fuer 26.4-snapshot-1; Fabric Loader/API/ModMenu aktuell; kein neuer Snapshot
- [x] Enderit-Kolben-Verschleiss nach Brecher-Muster + dunklere Enderit-Geraete
- [x] Testzentrale neu (laeuft): Befehlsbloecke wirklich isoliert (Test je Knopf), Give-Knopf an jeder Station (Haupt-/Nebenhand fuer Interaktionstests, ganze Blockpalette ins Inventar), alle Bloecke abgebildet, Flypad-Station sauber
- [x] Pad-Texturen konsistent (laeuft): Basis-Druckplatte des Materials + Overlay; Elytra-Pad blau (Enderit-Stufe mit Enderit-Details, V mit mehr Glanz); Flypads auf Enderit-Platte; Spawn-Teleporter V auf Gold-Platte + Name ohne "Enderite"; Launchpad I Eisen+Diamant, II alte Launchpad-I-Textur, III Eisen+Enderit; Chunk-Loader immer Kupfer-Basis
- [x] Laser: Sounds am Auftreffpunkt (Brummen / Zischen bei Brennbarem), Zeit steigt mit Entfernung (~3 s nah, ~20 s bei 200 m), Lebewesen anzuendbar (2x Zeit, PvP beachten) - im Echo-Sounder-Agenten
- [x] Tabs: SimpleTools-Kompasszeile (Kompass, Bergungskompass, Echo Sounder, Velocity Gauge, Erzdetektor, Magnet, Rotator, Amethystlinse, Oktant) + farbige Oktanten; Spawn-Elytra hinter die Elytra-Pads - im Kupfer-Agenten
- [x] Kartografietisch: signierte Blaupause oben -> Vorschau im Kartenfeld wie im Tooltip (laeuft)
- [x] Rotator wie Linse (nie kaputt, Amboss + 16 Enderperlen = voll), zweiter Ender-Sound + Teleport-Partikel; Magnet-Rezept Lapis rechts Mitte; Erzdetektor-Rezept 6 Echoscherben (4 Ecken dazu); Erzdetektor als Kompass (lila Nadel zum naechsten Erz, heller je naeher, viel weniger Partikel, Nebenhand leiser/schwaecher/langsamer) (laeuft)
- [x] Rucksack: Shift-Tooltip mit Inhalts-Vorschau wie Buendel (laeuft)
- [x] Laufwerk C voll gewesen: 60 gemergte Agenten-Arbeitskopien entfernt
- [ ] Velocity-Gauge-HUD als Tacho (Nadel unten links -> unten rechts), Titel "Velocity"; Namen/Texte aller Werkzeuge konsistent; Linse: normal nur "Laser", Entfernung nur verzaubert + letzte Messung gespeichert (laeuft)
- [ ] Buendel schliesst wie Vanilla; Enderit-Meissel zusaetzliche Bloecke (laeuft)
- [x] Texturen: Enderit-Vorlage (Innenteil dunkler, Glimmer), Kerne, Lederbogen, Diamant-Kiesel runder (laeuft)
- [x] Rotator/Magnet/Erzdetektor ohne HUD-Overlay (im Rotator-Agenten)
- [ ] Erfolge/Advancements mit Hinweisen fuer den naechsten Schritt (ganzer Baum) (laeuft)
- [ ] Schmiedevorlagen platzierbar (flach, 3D, wasserfest), Leucht-Transformation am Boden in 3 Schlaegen, Hinweis-Partikel bei Glowstone/Leuchttinte (laeuft)
- [ ] Anfaengerbuch beim Erstbeitritt + Themenbuecher an der Werkbank (laeuft)
- [ ] Trank-Pad ins Easter Egg aufnehmen (Endstufe doppelt so lange Wirkdauer)
- [ ] 26.3-Absturz Hammer+Ofen: kein Code-Fehler, Build waehrend laufendem Client (Regel gemerkt)

## Wartet auf den Besitzer
- [ ] Zeilen-Layout in SimpleMachines freigeben -> dann fuer alle Tabs

## Spaeter
- [ ] Enderit-Kolben: 3 Bloecke Abbau macht Tunnelbohrer zu leicht - spaeter Balance (z. B. Verschleiss/Schadenszustand)
- [ ] 26.4: Forge einschalten sobald Build da (-Pmc264_forge_version), Cloth-Config-Screen/Dev-Mods sobald 26.4-Builds da, NeoForge-26.4-Linie
- [ ] Baustab V1: normale Flaechen ueber den Blaupausen-Planer (Schutzpruefung pro Position) - ca. 40 Tests pinnen das heutige Verhalten
- [ ] Kerne als Baustab-Module + eigene Funktionen (Vorschlaege in docs/BAUWERKZEUGE-INTERAKTIONEN.md) - Besitzer: erst spaeter
- [ ] Kerne: Netherstern nur ab Diamant, Netherit-/Enderit-Baustab aus Kern, goldener Baustab mehr Haltbarkeit - nicht gewaehlt, spaeter neu besprechen
- [ ] Rucksack-Sortierung (Reihenfolge vorbereitet), sobald eine Sortierfunktion kommt
- [ ] Mehrere Mods in einem Repo (build-logic + framework/)
