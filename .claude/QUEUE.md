# Warteschlange SimpleBuilding (Stand 2026-09-30, Hauptlinie 26.3)

Regeln: AGENTS.md; aktueller Bestand/Belege in docs/HANDOFF.md. Worker committen auf
ihrem Branch ohne Push/Merge. Alte Wellen und Run-Details sind in der Git-Historie erhalten.
Ein erledigter Codepunkt ersetzt weder Client-Gate noch Besitzer-Abnahme.

## Integriert / implementiert
- [x] Wellen 1–23: Werkzeuge, Lager/Maschinen, Config/Modpack, Erfolge/Quests, Immersion,
  Balancing-Zentrale und frühere Ports (historische Gate-Zahlen nicht als aktuelles Gate verwenden).
- [x] Welle 24 BB–GG: Detector/Rezepte, Handbücher, platzierte Bündel/Oktanten,
  Pad-Aktivzustände, Spawnzielwahl, Kerne und Neunerreihen/Kreativreiter.
- [x] Welle 24 HH/II/JJ: Materialkern-Padrezepte und Mobköpfe, Shulkerkisten-Stufen,
  Resonanzstab, Attraktor/Range/Touch und Gauge. GG+: nur Eichen-Holzdruckplatte im Modreiter.
- [x] Z: Luftsprung 20/10 s, XP-Leisten-Priorität, Linear/Bridge und Oktant-Rezept.
- [x] Welle 25 KK2/FIX16: zwei isolierte End-Signalkanäle, Schalter/Lampen und Astralgewölbe;
  EndSystemsTests, Rezept-/Sprach-/Config-/Tab-Integrität und Testzentralen-Abdeckung.
- [x] BUGS/MM: Hammer-Zeitmodell 1,5/+0,8/+0,7, Oktant 2 je Block, Eckenschlag,
  Transformationshinweise; Basisbuch-Rezeptfreischaltung und Kapitel per Tab/Itemverbrauch.
- [x] Verstaerktes Bündel: Rezept/Datagen/Abdeckung, kein Gameplay-Fix auf Verdacht.
- [x] Spawn: jede Signalstärkeänderung setzt die Ladung sofort zurück (auch kurze Impulse).
- [x] Multimod-Grundlage, Wiki-Modauswahl, getrennte Balancing-Ablagen, Scaffold/Manifest;
  Launch Hub integriert, Forge 26.3 opt-in (`-Pforge263=true`).
- [x] Simple Money als eigenes Modul; Wiki-Vertrag und sieben Item-Notizen korrigiert.
- [x] Amerikanisches Englisch und Zeilen-Layout umgesetzt; laufende Textpflege bleibt nötig.

## Offen (inklusive Besitzerpunkte)
- [ ] Separater Gameplay-Fix: Strahlschalter-Rezeptfilter von laser_pointer auf aktuelles amethyst_lens prüfen/umstellen; Faktenpass hat nur die tatsächliche Grenze dokumentiert.
- [ ] Client-Gate Fabric/NeoForge 26.3 seriell bei geschlossenen Besitzer-Clients; Buch-Screen, Truhen, platzierte Bündel, Kern-Animation und neue Gadgets prüfen.
- [ ] Testzentrale in der Besitzerwelt neu bauen; automatische GameTest-Welten ersetzen keine Abnahme.
- [ ] Besitzerentscheidung zu Reparatur/Haltbarkeit des Resonanzstabs; Rotator sperrt Mending bereits.
- [ ] Rueckfragen Besitzer: Excavator/Diamond Ingots im Vorlagen-Tooltip, Cover-Buecher im Loot (Code vs HANDOFF), Liste G (58 Punkte) (Rotator hat bereits kein Mending)
- [ ] Rueckfragen neu: 12 Config-Ideen (Run D), Kern-Vorschlaege (Maurer-Diamantkern, 2. Eisenkern-Quelle, Enderit 0,5 %)
- [ ] W2: Balancing-Zentrale Phase 2 nach Merge von M/N/P2/R: gespeicherte Werte wirken im Mod (Konstanten -> Variablen/Datapack) - Plan: docs/BALANCING-ZENTRALE.md Abschnitt 5
- [ ] Beschaffungszeit je Item: Zeit bis zum 1. (und k.) Stueck je Quelle, gezielt vs. normales Spiel, mit Zeitalter-Einordnung ("vor Braustand & Traenke") auf Item-Seiten und in der Beschaffungs-Uebersicht (Modell: tools/devserver/sbdev/model.py)
- [ ] Diagramm "Zeit bis k Stueck" mit logarithmischer Zeitachse und Zeitalter-Linien auf Item-Seiten
- [ ] Handel: "im Angebot je Haendler/Dorfbewohner" (aus Poolgroesse, Ziehungen und Angebots-Chance) statt nur der rohen Angebots-Chance
- [ ] Loot: "Ø Stueck je Kiste" neben der Chance; Pools, die im Code fuer mehrere Tabellen gelten (Bastion, Tresore), sichtbar markieren
- [ ] Deep-Link auf eine einzelne Tabellenzeile (?f=<id>) mit Scrollen und kurzer Hervorhebung; "Seite nicht gefunden" nennt den Pfad und fuehrt zurueck
- [ ] Filter- und Ansichtszustand je Liste merken (Suchtext, Umschalter) wie schon die Faltungen
- [ ] Sticky Tabellenkoepfe in langen Tabellen (Rezepte, Loot, Config)
- [ ] Zaehler je Bereich in der Seitenleiste
- [ ] Config-Seite: Wertebereich (min ... max aus @BoundedDiscrete/validate()), Client/Server und "wirkt bei /reload" als eigene Spalten
- [ ] Leere Zustaende mit Handlungsanweisung statt nur "keine Eintraege"
- [ ] Offen beim Besitzer: Kern-Chancen-Vorschlag fuer Eisen/Gold/Diamant/Netherit uebernehmen?
- [ ] Vorlagen teurer machen (mehrere Materialien statt 1 Glowstone/Tintenbeutel/Echoscherbe)
- [ ] Punkte 64-69: Sprachen, Attractor mit Ladung, neue Bloecke, Baustab ueber Planer, Kern-Module, Rucksack-Sortierung/Multi-Mod-Repo
- [ ] Port-Run 26.2/1.21.11/26.4 erst nach Besitzer-Abnahme; Faktenpass und aktuelles 26.3-Gate siehe HANDOFF.
- [ ] Besitzer-Abnahme des bereits implementierten Zeilen-Layouts in allen Kreativreitern.
- [ ] 26.4: Forge einschalten sobald Build da (-Pmc264_forge_version), Cloth-Config-Screen/Dev-Mods sobald 26.4-Builds da, NeoForge-26.4-Linie
- [ ] Baustab V1: normale Flaechen ueber den Blaupausen-Planer (Schutzpruefung pro Position) - ca. 40 Tests pinnen das heutige Verhalten
- [ ] Kerne als Baustab-Module + eigene Funktionen (Vorschlaege in docs/BAUWERKZEUGE-INTERAKTIONEN.md) - Besitzer: erst spaeter
- [ ] Kerne: Netherstern nur ab Diamant, Netherit-/Enderit-Baustab aus Kern, goldener Baustab mehr Haltbarkeit - nicht gewaehlt, spaeter neu besprechen
- [ ] Rucksack-Sortierung (Reihenfolge vorbereitet), sobald eine Sortierfunktion kommt
- [ ] Besitzer-Abnahme: Mega-Handbuecher im Client ansehen und Testzentrale in der Besitzerwelt neu bauen.
- [ ] Besitzer-Abnahme: visuelles Hub-Rendering/echte Clients, lokale Modpack-Kombinationen
- [x] Weitere Ports als Module: riding, qol, visuals, models, dimensions, tweaks, fun, sounds und Money-Links (siehe Welle 25 unten); Abnahmen im Spiel offen.
- [x] Forge 26.3 fuer acht Module (experimentell, opt-in) und modulübergreifende Integrationstests/Gesamtgate; offen nur simpledimensions.
- [ ] Besitzer-Abnahme: 26.3-Signalkanaele und Astralgewoelbe im Client; Testzentrale in Besitzerwelt neu bauen.
- [ ] Desktop-/Handy-Sichtprüfung von Wiki und Balancing-Zentrale.
- [ ] Forge: echte Clientdarstellung, Config-Persistenz/optionale Integrationen und spätere Default-Einschaltung abnehmen.
- [ ] Besitzer-Abnahme von Simple Money; NeoForge-Client/alte Spielwelt noch prüfen. Forge 26.3 und andere Linien später im Port-Run.
- [x] TASK FACTS PASS: Texte/Code-Rezepte abgeglichen, DE/EN beider Sprachorte, Generatoren aktuell; check und 1562/1562 Server grün. Kein Gameplay, Push oder Merge; Besitzerabnahmen bleiben oben offen.

## Welle 25 (2026-09-30): Multi-Mod, Forge 26.3, Haertung - Stand Abend
- [x] Neun Module (Fabric + NeoForge 26.3, Forge experimentell opt-in ausser simpledimensions): simplemoney, simpleriding (+ Nautilus, 18 Serveroptionen), simplemodels, simplefun (8 Mechaniken, 4 Tierkoepfe, 26 Optionen), simplevisuals (12 Partikeleffekte, 5 Stufen, 50 Optionen), simplesounds (12 Klanggegenstuecke), simplequalityoflife, simpletweaks (Kompatibilitaet, Claims inaktiv), simpledimensions (Skyblock, Mining, Travel).
- [x] Simple Money verknuepft die anderen Mods: 243 bedingte reine Kaufangebote, serverseitige Preisgrenzen, dauerhafte Kaufgrenzen, kein Rueckkauf.
- [x] SimpleBuilding-Haertung (Boost-Paketbudget, Geschwindigkeitscap, Config-Obergrenzen, Laser-Rezeptfilter), Faktenpass, Sprach-Bridge (tools/voicebridge), Plugin-Registrierung der Module (tools/newmod.py, aitool merge-module).
- [x] Gate auf 7c540a06: 1600/1600 Server (fabric-263 + neoforge-263), 393/393 Integration + alle Modulsuiten, alles gruen.
- [x] Alle Codex-Laeufe sind gemergt; keine offenen Branches mit ungemergter Arbeit (codex-* lokal, Stand in master).
- [ ] Abnahme im Spiel (Client schliessen, Testzentrale in der Besitzerwelt neu bauen): Pulsating-Trim, Astralgewoelbe + Enderit-Pulver, Mega-Handbuecher, Hammer-Tempo (Formel 2/3 oder 3/4 der Pickaxe-Geschwindigkeit?), Attractor/Echolot, Boost und Pads nach der Haertung, Client-Neustart fuer den Architectury-Mixin-Fix.
- [ ] Abnahme neue Module im Spiel: Simple Models (UI, eigenes Modellpack), Simple Fun (Koepfe, Effekte; Glasbruch nur Einzelspieler), Simple Visuals/Simple Sounds (Stufen, Staerke), Simple Quality of Life, Simple Riding (Nautilus, Fahrgrenzen bei Latenz), Simple Dimensions (Portal bauen, reisen, zurueck), Simple Money (Preise, Haendlerpools der 243 Kaufangebote), Forge-Clients und Mehrspieler.
- [ ] Entscheidungen: Claim-System portieren oder zurueckstellen (alte Urkunden schuetzen nichts)? Alte Echo-Library-Compasse zum Echolot migrieren? Dimensions-Standardform: sechs Glowstone-Boegen (aktuell) oder Kupfer/Blau-Eis? QoL-Haltbarkeitsbonus 1,5x wirkt auch auf SimpleBuilding-Werkzeuge: behalten? Sounds-Intensitaet an Visuals koppeln? Echolot 3 s halten oder Ein-Klick? Forge 26.3 spaeter als Standard?
- [ ] Sprach-Bridge einrichten (docs/ai/VOICE-HANDS-FREE.md): Tailscale am Handy, `tailscale serve`, Anbieter und Modell waehlen, Headset testen; offen ob ein gesprochener Morgenbericht gewuenscht ist.
- [ ] Technisch offen: Forge fuer simpledimensions, Claim-Adapter fuer Dimensions, Cloth-Config-Dialog auf Forge, Wiki-UX-Ideen (oben), Port-Run 26.2/1.21.11/26.4 erst nach Release-Ankuendigung des Besitzers.
