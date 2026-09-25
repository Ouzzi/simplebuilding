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

## Welle 13 (laeuft)
- [x] Config-Schalter: Vanilla-Buecher (gibt es), Mod-Buecher custom/vanilla, sichtbare Besatzmuster Vanilla-Ruestung, sichtbare Besatzmuster Enderit-Ruestung

- [x] 26.4-Snapshot-Linie vorbereiten (Fabric, Forge falls vorhanden), experimentell, blockiert das Gate nicht
- [x] Nahtloses Welt-Upgrade 26.2 -> 26.3: Audit aller Mod-Daten, tolerante Leser, Fixture-Rundlauftest, docs/UPGRADE-26.2-26.3.md

## Welle 14 (laeuft): Simple Tweaks uebernehmen
- [ ] Inventur + docs/SIMPLETWEAKS-UEBERNAHME.md (inkl. vollstaendiger Claim-Notiz, Claims NICHT portieren)
- [ ] Port aller Druckplatten (Chunkloader, Elytra-Pad, Fly-Pad, Spawn-Teleporter + Modi), Spawn-/Erstbeitritt, Spawn-Elytra, XP-Kugeln, Laser, Echo-Kompass, Befehle, Config (jede Variante abschaltbar) - alle Linien/Loader
- [ ] Enderit-Stufe nach Netherit, Netherstern-Stufe rueckt eins hoch (z. B. Enderite Elytra Pad IV, Fine Elytra Pad V); Enderit-Platte mit Zusatzfunktion
- [ ] Echo-Kompass: Rezept Bergungskompass + Enderit-Kern + Netherit-Druckplatte links/rechts; Unbreaking-Bug fixen
- [ ] Tests alle Linien; danach in simpletweaks Branch remove-ported-features (nicht gepusht)

## Welle 15 (laeuft): Testzentrale
- [x] /sbtestcentre build: reproduzierbare Testwelt aus Code (Ruestung/Trims/Upgrades, alle Buecher, Werkzeuge plain+verzaubert, Meissel-Tuerme + In-World-Stationen, Lager, Bloecke, Maschinen-Demos, Erzdetektor-Feld, Essen, Oktant/Blaupause/Baustab-Modi, Versatility/Vein/Strip, Command-Block-Knoepfe)
- [x] Abdeckungstest: jedes Mod-Item/-Block steht in der Zentrale (neue Features fallen automatisch auf); docs/TESTZENTRALE.md; Regel: am Ende jedes Runs pruefen

## Wartet auf den Besitzer
- [ ] Zeilen-Layout in SimpleMachines freigeben -> dann fuer alle Tabs

## Spaeter
- [ ] 26.4: Forge einschalten sobald Build da (-Pmc264_forge_version), Cloth-Config-Screen/Dev-Mods sobald 26.4-Builds da, NeoForge-26.4-Linie
- [ ] Baustab V1: normale Flaechen ueber den Blaupausen-Planer (Schutzpruefung pro Position) - ca. 40 Tests pinnen das heutige Verhalten
- [ ] Kerne als Baustab-Module + eigene Funktionen (Vorschlaege in docs/BAUWERKZEUGE-INTERAKTIONEN.md) - Besitzer: erst spaeter
- [ ] Kerne: Netherstern nur ab Diamant, Netherit-/Enderit-Baustab aus Kern, goldener Baustab mehr Haltbarkeit - nicht gewaehlt, spaeter neu besprechen
- [ ] Rucksack-Sortierung (Reihenfolge vorbereitet), sobald eine Sortierfunktion kommt
- [ ] Blaupausen-Code: Formen (sphere(...)) und Variablen
- [ ] JEI-Infoseiten fuer Items ohne Rezept
- [ ] Mehrere Mods in einem Repo (build-logic + framework/)
