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
- [ ] Weitere Ports als Module: riding, qol, visuals, models, dimensions, tweaks und fun; Money-Implementierung fertig, Abnahme separat offen.
- [ ] Forge 26.3 für weitere Module und modulübergreifende Integrationstests/Gesamtgate; vorhandene Module sind bereits in Wiki und Zentrale.
- [ ] Besitzer-Abnahme: 26.3-Signalkanaele und Astralgewoelbe im Client; Testzentrale in Besitzerwelt neu bauen.
- [ ] Desktop-/Handy-Sichtprüfung von Wiki und Balancing-Zentrale.
- [ ] Forge: echte Clientdarstellung, Config-Persistenz/optionale Integrationen und spätere Default-Einschaltung abnehmen.
- [ ] Besitzer-Abnahme von Simple Money; NeoForge-Client/alte Spielwelt noch prüfen. Forge 26.3 und andere Linien später im Port-Run.
- [x] TASK FACTS PASS: Texte/Code-Rezepte abgeglichen, DE/EN beider Sprachorte, Generatoren aktuell; check und 1562/1562 Server grün. Kein Gameplay, Push oder Merge; Besitzerabnahmen bleiben oben offen.

- Besitzer-Wuensche 2026-09-30 (Briefs im Scratchpad codex/, erzeugt mit mkports2.py; erst NACH dem Plugin-Umbau der Modul-Registrierung, branch codex-port-riding, starten): visuals + mehr Vanilla-Partikel mit Stufen Off/Subtle/Normal/Strong/Maximum; NEUER Mod simplesounds (Klang-Gegenstueck je Visual-Effekt, gleiche Stufen); fun + weitere Spielereien + Schweine-/Kuh-/Huhn-/Schafkoepfe (Charged Creeper); money-links (Simple Money als Addon: zusaetzliche Handelsangebote fuer Items der anderen Mods, alte bleiben, keine Arbitrage); qol (Ideenliste docs/modules/simplequalityoflife-ideas.md, serverseitige Caps, Client darf nicht cheaten); riding (Nautilus falls fehlend, Caps); tweaks gruendlich pruefen; dimensions (konfigurierbare Custom-Dimensionen, nachvollziehbar/mysterioes, schlichte Configs); models (Ordner mit eigenen Modellen, Zuweisung per Amboss, Browser-Screen). Qualitaetslatte fuer alle Mods: Vorbild SimpleBuilding (UI/UX), alle nicht ausnutzbaren Config-Optionen serverseitig mit Caps, umfangreiche Tests gruen.

## Simple Riding 26.3 (Codex)
- [x] Port des read-only Quellrepos 1.0.5 auf 26.3: Fabric/NeoForge, eigener Integrationskatalog, Client-Smoke, Wiki/Balancedaten, Launch-Hub-Testanbindung. Details: docs/modules/simpleriding.md; kein Push/Merge. Forge und andere Linien erst im eigenen Release-Port.

- [x] MERGE-RIDING: master integrieren; Modulregistrierung aus Manifest entdecken,
  beide Modulkataloge/Client-Smokes erhalten, neue Ports ohne gemeinsame Wiring-Bloecke.

## Orchestrator-Stand 2026-09-30 17:00 (Welle 25, Fortsetzung)
- [x] Plugin-Umbau der Modul-Registrierung und Simple Riding nach master gemergt (7ef79212); Gate laeuft. Briefe tragen jetzt die Plugin-Regel (docs/ai/briefs/mm-contract.md).
- [ ] Laufende Laeufe: facts (Faktenpass), voicebridge (Sprach-Bridge), port-visuals, port-fun. Danach in Wellen (max. 4 gleichzeitig): new-sounds (nach visuals), port-qol, port-tweaks, port-dimensions, port-models, riding-followup (Nautilus, Obergrenzen), zuletzt money-links; dann Forge 26.3 fuer alle Module, Gesamtgate.

## VOICEBRIDGE (2026-09-30, codex-voicebridge)
- [x] Besitzerwunsch: Handy/Laptop, eine grosse Sprechtaste, kurze vorgelesene Antworten, mehrere Projekte; stdlib-Server und PWA, lesende Agenten, konkrete Dateivorschlaege mit einmaliger Sprachbestaetigung.
- [ ] Besitzerpruefung: echtes Handy-Mikrofon/Vorlesen, Tailscale HTTPS, Headset-Taste und gewaehlter CLI-Anbieter; Verifikation siehe HANDOFF.
- [ ] Simple Models (codex-port-models): renamed-Skelett nach 26.3 Fabric/NeoForge; servervalidierte Ambossmodelle, Ordnerkatalog, Browser, EN/DE, Modul-/Clienttests. Keine Ports/Push/Merge.
- [x] Simple Models 26.3 umgesetzt und committed; finale Modtests 32/32, Fabric-Client 5/5 gruen. Besitzerabnahme echter Modellpack/UI offen; Forge und andere Linien bleiben eigener Port. Details docs/modules/simplemodels.md.
`n- Simple Fun: 26.3 Fabric/NeoForge port, complete feature/security tests, eight cosmetic delights and charged-creeper farm-animal heads; work branch codex-port-fun, no push/merge.
`n- Simple Fun 26.3 complete on codex-port-fun: 1627/1627 full server tests + 2/2 LAN safety, 4/4 Fabric client checkpoints, final Gradle check Exit 0; see docs/modules/simplefun.md and HANDOFF. Owner visual acceptance and deferred loader/MC ports remain. No push/merge.
## Simple Visuals (Codex, 26.3)
- Port der Quellvisuals als Pluginmodul simplevisuals; zusaetzlich zwoelf Vanilla-Partikeleffekte mit stabiler Registry, Stufen und festen Caps. Eigene Server-/Clienttests, Wiki, Config-/Balancedaten. Kein Push/Merge; andere Linien bleiben separat.
- [x] Simple Visuals 26.3 Fabric/NeoForge: Port, 12 Vanilla-Effekte, getrennte Amboss-Serverpolicy, manifestbasierte Modulziele/Wiki/Balancedaten. Server 1599/1599, Modul 36/36, Client 5/5, Sicherheitsgegenprobe 2/2, Gradle-check Exit 0. Besitzerabnahme der Optik und spaetere Ports bleiben offen.

- Simple Sounds: neues 26.3-Modul, zw?lf datengetriebene Soundgegenst?cke, lokale Stufen/Overrides und harte Spam-/Lautst?rkegrenzen; Besitzerabnahme offen.
- Simple Sounds umgesetzt/verifiziert: 34 Modul-Servertests, drei Fabric-Clientpunkte, zehn Testzentralenfaelle und finales Gradle-Gate gruen; akustische Besitzerabnahme offen, kein Push/Merge.

## Simple Quality of Life 26.3 (Codex, codex-port-qol)
- [ ] Port der aktiven 1.0.6-Multiloaderquelle auf Fabric/NeoForge 26.3, Server-Caps/Anti-Cheat, vollstaendige Modulpruefungen, Wiki/Daten und Ideenliste. Keine neuen Ideen implementieren; Forge und andere Linien bleiben separat.

- [x] Simple Quality of Life 26.3 fertig: Fabric/NeoForge, 48/48 Modul-Servertests und 5/5 Fabric-Client-Pruefpunkte gruen; Bestand 1562/1562, Integration 1/1 und check gruen. Wiki/Config/Balancedaten/Ideenliste vorhanden. Besitzer-Abnahme, NeoForge-Client/alte Welt/Fremd-Claims sowie Forge/andere Linien bleiben offen; kein Push/Merge.
