# Warteschlange SimpleBuilding (Stand 2026-10-01, Hauptlinie 26.3)

Regeln: AGENTS.md; aktueller Bestand/Belege in docs/HANDOFF.md. Worker committen auf
ihrem Branch ohne Push/Merge. Alte Wellen und Run-Details sind in der Git-Historie erhalten.
Ein erledigter Codepunkt ersetzt weder Client-Gate noch Besitzer-Abnahme.
Audit 2026-10-09 (claude-q-audit): offene Punkte gegen claude-wave1 9c7929ff4 geprüft, erledigte mit
„Audit 09.10.“ abgehakt, Teilstände als „(teilweise: …)“. Priorisierte Restliste: docs/ai/ROADMAP-2026-10-09.md.

## Worker Befunde 11/12 (2026-10-05)
- [x] Visuals/Sounds auf GameTest-Assertions umgestellt; Riding-Handling prüft alle Vanilla-Stufen standalone, Enderit nur mit SB. Branch `gpt-befunde`: Module 296/296, Kern 1890/1890 grün, Gate mit `-PskipWiki` grün; kein Push.

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
- [x] Launch-Zentrale: alle elf Projektmods standardmäßig ausgewählt, einklappbare Abwahl; tatsächliche Ladung und Abwahl auf Fabric/NeoForge/Forge 26.3 geprüft.
- [x] Bett-/Tür-/Doppelpflanzenplatzierung mit zweiter Zellenprüfung repariert; 54/54 WandMode-Tests. Oktant-Füllungen überspringen diese Materialien ohne Verbrauch.
- [x] Claims-Folgeschutz für Crafter, Kupfergolem-Transfers und Blitz-Blockänderungen; 100/100 Modultests. Claims bleiben wegen weiterer offener Pfade AUS.

## Offen (inklusive Besitzerpunkte)
- [x] 26.2-Nachtserie Folgefix: Crucible-Ausnahme und Maschinenkapitel 9/10 nach Versionsflag getrennt; 26.2 Fabric/NeoForge/Forge 2932/2932, 26.3-Gegenprobe 1954/1954, Gate/Pflicht-Compiles und Wiki gruen. Worker `gpt-line262b`, Plan `docs/ai/PLAN-LINE262B-2026-10-02.md`. Kein Port/Datagen/Client/Push.
- [x] Haengematten-Testcenter: 22 beschriftete Laengen-/Winkel-/Ankerbeispiele, alle 16 Farben, ohne Ueberlappung. Fabric/NeoForge 1932/1932 und Check/Pflicht-Compiles gruen; Worker `gpt-hammocktc`, Plan `docs/ai/PLAN-HAENGEMATTEN-TESTCENTER-2026-10-02.md`. Kein Client/Push; Besitzerwelt-Neubau und Sichtabnahme offen.
- [x] NeoForge-Modul-Gate: implizite Mod-Liste beim SimpleLib-Nachladen erhalten und Spielordner je Modul isolieren. Money/Dimensions zweimal 61/61, alle 21 Modulziele 617/617, Kern 962/962 und `check -q -PskipWiki` samt Pflicht-Compiles gruen. Worker `gpt-neorun`, Plan `docs/ai/PLAN-NEOFORGE-MODULE-RUNS-2026-10-02.md`, kein Push/Client.
- [x] Launch-Hub `client_fresh`: echte frische Welt mit Archiv, sicherer Testzentralen-Spawn und dev-only Experimental-Bestaetigung. Worker `gpt-hubworld`: Server 1922/1922, Gate/Pflicht-Compiles/Wiki gruen; neue Hub-Tests 5/5, volle Hub-Suite 86/87 wegen unveraendertem SimpleLib-Standalone-Manifestfehler. Besitzer-Clientabnahme offen; Plan `docs/ai/PLAN-HUB-TESTWORLD-2026-10-02.md`, kein Push.
- [x] 26.2-Nachtserie ohne Feature-Port repariert: Flags/Erwartungen, Handbuch-/Upgrade-Daten und Forge-Hooks; 26.2 Fabric/NeoForge/Forge 2836/2836 und 26.3-Gegenprobe 1890/1890 gruen; Forge-26.3-Compile und `check -q -PskipWiki` Exit 0. Worker `gpt-line262`, Plan `docs/ai/PLAN-LINE262-2026-10-02.md`; kein Push/Datagen/Client.
- [x] Auto Smither 26.3: entnehmbare, gegen Einlegen gesperrte Ausgabe; Trichterseiten, Kapazitaetsschutz und Vanilla-GUI korrigiert. 1872/1872 Servertests und Compile-/check-Gate gruen. Plan/Vorschau: `docs/ai/PLAN-AUTO-SMITHER-2026-10-02.md`; Client-Sichtabnahme offen, kein Push.
- [x] Besitzer-Abgleich 2026-10-05 (`gpt-gaps`), Messer: vier Nuggets nach Sollmuster, EN/DE und Wiki korrigiert, Generatorcheck und je 18/18 Fabric-/NeoForge-Tests grün. Plan: `docs/ai/PLAN-BESITZER-LUECKEN-2026-10-02.md`; kein Push/Client.
- [x] Besitzer-Abgleich 2026-10-05 (`gpt-gaps`), Schmiedebuch: drei Upgrade-Platzhalter serverseitig ohne Displays, Slot-Rezepte und Handbuchfreischaltung erhalten; Hauptmod je 941/941 und Abschluss-Gate grün. Sichtabnahme offen.
- [x] Besitzer-Abgleich 2026-10-05 (`gpt-gaps`), Money: Nihil-Gewölbe analog Astralgewölbe (37 Scheine, Bestand 1, 10 % Chance), Generator/Daten-/Balancechecks und je 18/18 Fabric-/NeoForge-Tests grün. Abschluss aller drei Aufgaben: 1954/1954 Server, `check -q` und geforderte Compiles grün; kein Push/Client.
- [x] Wackelige GameTests: Testzentrale-Knoepfe, Claims-Attractor, Shulker-Endermiten, Magnet-Pickup und Palette abgesichert; dreifache Wiederholungen je Loader, Basis 1814/1814, SimpleTweaks 104/104 und Gradle-Gate gruen. Belege: `docs/ai/PLAN-FLAKY-GAMETESTS-2026-10-02.md`; Worker-Branch, kein Push/Port.
- [x] Simple Riding: Leaping-/Tailwind-Buecher aus Vorschlag B mit weiteren 25 % Kontrast; Generator, Modulmodelle, Vorschau, Wiki-Modellalias-Vertrag, 78/78 Modultests und volles Gate gruen (2026-10-04, gpt-books). Client-Sichtabnahme und 26.2-Port bleiben offen.
- [x] Launch-Hub offline: unnoetiges Forge 26.2 auslassen, DNS-/erzwungenen Offline-Modus und klare Mavenizer-Meldung; 78 Hub-Tests, Offline-Fabric-Compile und Clientvorbereitung inkl. Assets gruen, kein Clientstart. Worker `gpt-hubclient`, Belege: `docs/ai/PLAN-HUB-OFFLINE-2026-10-02.md`.
- [x] B8: Item-spezifische EN/DE-Zusätze für die 17 Wiki-Familien, einschließlich Alt-IDs; Rezept- und Zahlenkorrekturen gegen 26.3-Quellen. Wiki-Generator und Python-Tests im Worker-Worktree geprüft, kein Port/Push.
- [x] Strahlschalter-Rezeptfilter berücksichtigt amethyst_lens und die alte laser_pointer-ID (RecipeFilter.java).
- [x] Serielle Fabric-/NeoForge-26.3-Clientprüfung und gezielte Nachprüfung der belegten Testfehler; Dimensions/QoL/Sounds/Visuals-Smokes: `docs/ai/CLIENT-ACCEPTANCE-2026-10-01.md`.
- [ ] Weitere Sichtabnahme für Buch-Screen, Truhen, Kern-Animation und neue Gadgets; Octant-Manager-Kontrast verbessern. Bestehende Screenshots sind keine pauschale Abnahme.
- [ ] Testzentrale in der Besitzerwelt neu bauen; automatische GameTest-Welten ersetzen keine Abnahme.
- [x] Resonanzstab auf 26.3: Amethystscherben als Amboss-Reparaturmaterial-Tag, Tooltip EN/DE und Wiki; Rotator bleibt ohne Mending. Worker `gpt-answers`, Plan `docs/ai/PLAN-BESITZER-ANTWORTEN-2026-10-02.md`.
- [x] Rueckfragen Besitzer: alte Excavator/Diamond-Ingots-Texte waren bereits korrigiert; Cover blieb nach der damaligen Besitzerentscheidung im Loot; die neue Option B vom 2026-10-05 ist unten umgesetzt. Belege und Optionen: `docs/ai/RUECKFRAGEN-ERKLAERT-2026-10-05.md`.
- [ ] Liste G (58 Punkte): Run G der Welle 22 belegt, Original der nummerierten Liste in den recherchierten Quellen nicht auffindbar; keine erfundene Rekonstruktion. Siehe Erklaerungsdokument.
- [ ] 12 zusaetzliche Config-Ideen (Run D): Originalliste fehlt weiterhin. Sechs belegte bestehende Run-D-Zahlenoptionen auf 26.3 hart begrenzt, EN/DE, ConfigOptionTests und Wiki-Metadaten ergaenzt; der vollstaendige Zwoelfer-Abgleich bleibt offen. (teilweise: gpt-coverconf 79da90c66 hat zwölf neue Ideen bewertet, vier begrenzt umgesetzt; Original-Zwölferliste fehlt weiter)
- [x] Kern-Vorschlaege abgeglichen: neuere Besitzerentscheidung verbietet Steinmetz-Kerne; zweite Eisenquelle Mine (0,5 %) besteht bereits. Enderit auf 26.3 jetzt 0,5 %, alte Linien unveraendert; Details `docs/KERNE-SELTENHEIT.md`.
- [x] Balancing-Zentrale schreibt unterstützte Java-/JSON-Werte in die Modquellen, einschließlich Vorschau, Konfliktprüfung und Rollback (sbdev/service.py, tests/test_phase2.py). Nicht zugeordnete Werte bleiben ausdrücklich Planwerte; kein automatischer Live-Reload kompilierten Java-Codes.
- [ ] Beschaffungszeit je Item: Zeit bis zum 1. (und k.) Stueck je Quelle, gezielt vs. normales Spiel, mit Zeitalter-Einordnung ("vor Braustand & Traenke") auf Item-Seiten und in der Beschaffungs-Uebersicht (Modell: tools/devserver/sbdev/model.py) (teilweise: Kistenöffnungs-Teilmenge da; Zeit-/Zeitaltermodell fehlt)
  - [x] Belegbare Teilmenge 2026-10-03: erwartete Kistenöffnungen bis zum ersten/sechsten Stück für eindeutige Bernoulli-Quellen; keine erfundenen Stunden. Zeit-/Szenariokonzept: `docs/ai/PLAN-WIKI-BESCHAFFUNGSZEIT-2026-10-03.md`.
- [ ] Diagramm "Zeit bis k Stueck" mit logarithmischer Zeitachse und Zeitalter-Linien auf Item-Seiten
  - Konzept vorhanden; Umsetzung wartet auf belegte oder ausdrücklich eingegebene Ereignisraten und Zeitalter-Annahmen.
- [x] Handel: "im Angebot je Haendler/Dorfbewohner" (aus Poolgroesse, Ziehungen und Angebots-Chance) statt nur der rohen Angebots-Chance. Vanilla-Nachziehen berücksichtigt; bei ungeklärten Kontext-/Item-Ablehnungen belegbare Grenzen statt Scheinpräzision. Standardprofil 26.3 Vanilla + SimpleBuilding.
- [x] Loot: "Ø Stueck je Kiste" neben der Chance; Pools, die im Code fuer mehrere Tabellen gelten (Bastion, Tresore), sichtbar markieren. Tresorwerte ausdrücklich je Untertabellen-Aufruf.
- [x] Deep-Link auf eine einzelne Tabellenzeile (?f=<id>) mit Scrollen und kurzer Hervorhebung; "Seite nicht gefunden" nennt den Pfad und fuehrt zurueck. Filter/geschlossene Abschnitte, Encoding und Hash-Links geprüft.
- [x] Filterzustand je Mod/Liste und Rezept-Ansicht einschließlich Suchtext merken; Browserprüfung mit Neuladen und gesperrtem Storage grün.
- [x] Sticky Tabellenköpfe in langen Tabellen; tastaturbedienbare Scrollbereiche, Desktop-/Mobil-Browserprüfung grün.
- [x] Zaehler je Bereich in der Wiki-Seitenleiste vorhanden.
- [x] Config-Seite: Wertebereich (min ... max aus @BoundedDiscrete/validate()), Client/Server und "wirkt bei /reload" als eigene Spalten. Quellenbasiert; Teilgrenzen und nicht belegte Metadaten ausdrücklich unbekannt. Plan/Prüfstand: `docs/ai/PLAN-WIKI-IDEEN-2026-10-03.md`.
- [x] Leere Listen/Filterergebnisse erklären den nächsten Schritt; Filter zurücksetzen mit Fokuswiederherstellung.
- [x] Kern-Chancen Eisen/Gold/Diamant/Netherit: dokumentierte Werte nach `docs/KERNE-SELTENHEIT.md` §5.3 bestaetigt und beibehalten; Enderit-Anpassung 26.3 separat oben.
- [x] Vorlagen teurer machen (26.3): Mehrfach-Materialien + Diamanten, Kopien nach Vanilla-Muster; 1834/1834 Server grün, Gesamt-Gate grün. Plan/Balance/Offenes: `docs/ai/PLAN-VORLAGEN-TEURER-2026-10-04.md`. Besitzer-Abnahme und Port-Run offen.
- [ ] Punkte 64-69: Sprachen, Attractor mit Ladung, neue Bloecke, Baustab ueber Planer, Kern-Module, Rucksack-Sortierung/Multi-Mod-Repo
- [ ] Port-Run 26.2/1.21.11/26.4 erst nach Besitzer-Abnahme; Faktenpass und aktuelles 26.3-Gate siehe HANDOFF.
- [ ] Besitzer-Abnahme des bereits implementierten Zeilen-Layouts in allen Kreativreitern.
- [ ] 26.4: Forge einschalten sobald Build da (-Pmc264_forge_version), Cloth-Config-Screen/Dev-Mods sobald 26.4-Builds da, NeoForge-26.4-Linie
- [x] Baustab V1: normale Flaechen ueber den Blaupausen-Planer (Schutzpruefung pro Position) - ca. 40 Tests pinnen das heutige Verhalten
- [ ] Kerne als Baustab-Module + eigene Funktionen (Vorschlaege in docs/BAUWERKZEUGE-INTERAKTIONEN.md) - Besitzer: erst spaeter
- [ ] Kerne: Netherstern nur ab Diamant, Netherit-/Enderit-Baustab aus Kern, goldener Baustab mehr Haltbarkeit - nicht gewaehlt, spaeter neu besprechen
- [ ] Rucksack-Sortierung (Reihenfolge vorbereitet), sobald eine Sortierfunktion kommt
- [ ] Besitzer-Abnahme: Mega-Handbuecher im Client ansehen und Testzentrale in der Besitzerwelt neu bauen.
- [ ] Besitzer-Abnahme: visuelles Hub-Rendering/echte Clients, lokale Modpack-Kombinationen
- [x] Weitere Ports als Module: riding, qol, visuals, models, dimensions, tweaks, fun, sounds und Money-Links (siehe Welle 25 unten); Abnahmen im Spiel offen.
- [x] Forge 26.3 einschließlich simpledimensions integriert; experimentell/opt-in. Clientabnahme bleibt offen.
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
- [x] Entscheidungen vom 2026-09-30 Abend getroffen (naechster Block): Claims zuerst als ausgeschaltetes Feature, Echo-Kompasse nicht migrieren, Dimensionen (Bogen bleibt Standard, Einzelschalter in den Einstellungen), QoL-Haltbarkeitsbonus Standard 1, Sounds an Visuals koppeln, Forge spaeter. Noch offen: Echolot 3 s halten oder Ein-Klick.
- [ ] Sprach-Bridge einrichten (docs/ai/VOICE-HANDS-FREE.md): Tailscale am Handy, `tailscale serve`, Anbieter und Modell waehlen, Headset testen; offen ob ein gesprochener Morgenbericht gewuenscht ist.
- [x] Forge für simpledimensions und Claim-Adapter implementiert. Native Forge-Dialoge ersetzen die inkompatible Cloth-Version; GUI-Abnahme bleibt offen.
- [ ] Weitere Wiki-UX-Ideen (oben); Port-Run 26.2/1.21.11/26.4 erst nach Release-Ankuendigung des Besitzers.

## Besitzer-Entscheidungen 2026-09-30 Abend (Reihenfolge = Prioritaet; fuer die Laptop-Sitzung, Briefs in docs/ai/briefs/next-*.md)
- Orchestrierung aktiv: `codex-next-claims`, danach `codex-next-dimensions` und `codex-next-small` gestartet. Plan/Verifikation: `docs/ai/CODEX-PLAN.md`; offene Fortsetzung: `docs/ai/CODEX-HANDOVER.md`. Noch keine neue Feature-Abnahme oder Gate-Freigabe.
- Fortsetzung 2026-10-01: Claims-Stufen 1–3 auf `7422a3ab` geprüft (36/36 Claims, 70/70 Hammer-Regressionsfälle). `next-claims` bearbeitet Stufe 4; `next-claims-access` parallel Stufen 5–6. Dimensions (`8657e3d2`) und QoL/Sounds (`7faca095`) geprüft und mergebereit. Noch kein Feature-Merge/Push dieser Welle.
- [ ] 1. CLAIMS (als Allererstes): das Claim-System der Quelle (ClaimState, ClaimProtectionHandler, ClaimDeedItem, /claim-Befehle) jetzt portieren, aber als DEAKTIVIERTES Feature: Hauptschalter standardmaessig aus, Stufe fuer Stufe weiter ausbauen und erst nach Fertigstellung freigeben. Anforderungen, Fallen und Testliste: docs/modules/simpletweaks.md Abschnitt "Sicherheitsbefunde und Entscheidungen" (Caps, atomare Persistenz, Rechte an jedem Ziel, Explosion/Feuer/Kolben/Fluessigkeit, Zwei-Spieler-Tests). Brief: docs/ai/briefs/next-claims.md. Danach ein Claim-Adapter fuer Dimensions. (teilweise: Stufen 1–6 integriert, Hauptschalter AUS; Stufe 4 und Schutzlücken offen, Freigabe fehlt)
- [x] 2. Alte Echo-Library-Kompasse: NICHT migrieren (entschieden: die neue Loesung ersetzt sie, die alten braucht niemand). Keine Arbeit.
- [x] 3. DIMENSIONS: (a) Standardform bleibt der Bogen (sechs Glowstone-Boegen mit Zusatzlicht); keine Kupfer/Blaueis-Variante in den Configs anbieten. (b) Die vorinstallierten Dimensionen (Skyblock, Mining, Travel) sind einzeln in den EINSTELLUNGEN (Server-Optionen, eigener Reiter im Config-Bildschirm) ein- und ausschaltbar, nicht ueber die Config-Dateien der Dimensionen. (c) SPAETER, erst nach dem gemeinsamen Durchgehen mit dem Besitzer: beliebige erlaubte Portalformen frei konfigurierbar und ueber waehlbare Mechaniken aktivierbar (nicht jetzt beginnen). Brief fuer (a) und (b): docs/ai/briefs/next-dimensions-settings.md.
- [x] 4. QUALITY OF LIFE Haltbarkeitsbonus: gilt auch fuer Mod-Werkzeuge (SimpleBuilding und andere), aber der Standard ist 1 (kein Bonus); per Config erhoehbar (serverseitig, harte Obergrenze). Brief: docs/ai/briefs/next-small.md.
- [x] 5. SOUNDS: Intensitaet an die Stufe von Simple Visuals koppeln (Off/Subtle/Normal/Strong/Maximum); ohne Visuals gilt eine eigene Einstellung. Brief: docs/ai/briefs/next-small.md.
- [x] 6. FORGE spaeter: Forge 26.3 auch fuer Dimensions, Cloth-Dialog auf Forge und die Frage der Standardaktivierung kommen nach 1 bis 5.
- [x] Noch offen: Echolot 3 Sekunden halten oder Ein-Klick? Morgenbericht der Sprach-Bridge ja oder nein? → Besitzer: „ja und ja“ (3 s halten bleibt), Morgenbericht vorerst nicht relevant.

## Orchestrator-Abschluss 2026-10-01

- [x] Claims-Stufen 1-6 integriert, Hauptschalter AUS. Stage 4 bleibt teilweise
  offen; keine Freigabe. Grenzen: modules/simpletweaks/CLAIMS-STAGE4.md.
- [x] Dimensions: sechs Glowstone-Boegen, drei persistente Serverschalter im
  eigenen Reiter; Kupfer/Blaueis-Preset entfernt. Freie Formen spaeter.
- [x] QoL: Standard 1, Cap 1.5, auch Mod-Werkzeuge; bestehende Werte erhalten.
- [x] Sounds folgt Visuals optional, eigene Stufe ohne Anbieter.
- [x] Forge integriert mit 39fd85b1 (Worker 6786b545): Dimensions/Claims-Adapter,
  native Dialoge, validierte Persistenz und korrigierte Laufzeitverpackung.
  Forge bleibt experimentell/opt-in; kein passendes Cloth-Forge-Artefakt.
- [x] Default-/Forge-Worker-check gruen; 1039 verschiedene Forge-Faelle belegt,
  finale Drei-Loader-Regression 264/264, Wiederholung 88/88, Settings bytegleich.
  Testzentralen in isolierten Welten neu gebaut, SimpleBuilding-Abdeckung komplett.
- Abschlussgate der zusammengefuehrten SHA: .ai-runs/final-full-gate.log.
  Push ausschliesslich fuer deren GREEN-SHA; Remote-Bestaetigung im Abschlussbericht.
- [ ] Claims-Schutzluecken, Client-/Audio-/GUI-Abnahmen und Besitzerwelt bleiben
  offen. Besitzer-Client laeuft; keine Clients gestartet oder gestoppt.
  Konkrete Fortsetzung: docs/ai/CODEX-HANDOVER.md.
- Pruefbelege: docs/ai/CODEX-VERIFICATION-2026-10-01.md und
  docs/FORGE-FOLLOWUP-RESULTS.md. Erstes rotes Gesamtgate und Korrekturen erhalten.

## B7: End redstone rename (26.3)
- [x] Renamed both End signal powders to Astral Redstone / Nihil Redstone, yield two per recipe, and preserve item and block aliases. Verified on claude-b7: compile targets, 26.3 datagen, wiki, full check, End systems 6/6, migration 1/1, and test centre rebuild/coverage 4/4. No port or push.

## Queue-Ende (Besitzer 2026-10-02): Rezeptbuch in Befiederungs- und Schmiedetisch
- [x] Befiederungstisch-GUI wie der Werkbank-Bildschirm: das Rezeptbuch-Symbol an exakt derselben Stelle wie bei der Werkbank, dieselbe Bedienung (Buch öffnet die Rezeptliste links, Klick legt die Teile ein), nur dass man Spitze/Schaft/Befiederung wählt; Layout wie die Werkbank minus der fehlenden Felder. (Branch claude-workstations, wartet auf Abnahme)
- [x] In den drei Teil-Slots Hintergrund-Silhouetten wie bei den Rüstungsslots (zeigen, was hineingehört: Spitze, Schaft, Befiederung). (Branch claude-workstations, wartet auf Abnahme)
- [x] Titel über den drei Slots: „Fletching“ / „Befiederung“ statt „Fletching Table“. (Branch claude-workstations, wartet auf Abnahme)
- [x] Schmiedetisch: statt des Knopfs für die Besatz-Resonanz-Vorschau ein anklickbares Rezeptbuch (über dem dritten Slot gerendert), das wie bei der Werkbank die möglichen Ergebnisse zeigt und einlegt. (Branch claude-workstations, wartet auf Abnahme)

## Queue-Ende (Besitzer 2026-10-02): seltene verstärkte Shulker in End-Städten
- [x] Spawn: In End-Städten wird ein Shulker sehr selten verstärkt (ca. 2 %) oder zum Enderit-Shulker (ca. 0,5 %); serverseitig konfigurierbar mit Obergrenze.
- [x] Leben: verstärkt 1,5×, Enderit 3× (oder mehr, wenn es spielerisch nötig ist). Die Hülle zeigt die Stufe.
- [x] Drops: 0–2 Schalen ihres eigenen Typs.
- [x] Neue Items: Verstärkte, Netherit- und Enderit-Shulkerschale.
- [x] Schalen in der Welt aufwerten (wie die anderen In-World-Transformationen): Schale auf den Boden legen und mit einem Nugget rechtsklicken. Eisen/verstärkt → verstärkte Schale, Netherit → Netherit-Schale, Enderit → Enderit-Schale; genau ein Nugget pro Schale.
- [x] Rezept der Shulkerkisten-Stufen: Kupfertruhe + eine aufgewertete Schale + eine normale Shulkerschale.
- [x] Geklärt (Besitzer 2026-10-02): Auch die Netherit-Schale entsteht in der Welt mit einem Netherit-Nugget. Das neue Rezept kommt zu den bestehenden Stufenrezepten dazu.
- [x] Umgesetzt auf `claude-loot` (Plan `docs/ai/PLAN-SELTENE-TRUHEN-SHULKER-2026-10-02.md`), inkl. Nachtrag 4 Endermiten je Spezial-Shulker (einmalig bei Spielernähe, nicht dauerhaft). Abnahme im Client offen (Hüllen-Textur, Schalen-Texturen).

## Queue-Ende (Besitzer 2026-10-02 abends, mit Screenshots)
- [x] Guide-Buch: Die Tabs haben eine hässliche graue Box als Overlay (Screenshot: linke und rechte Tab-Leiste) → entfernen bzw. sauber zeichnen. (claude-guideui: Box über gesperrten Icons entfernt)
- [x] Guide-Buch im Kreativmodus: gesperrter Tab zeigt einen Knopf „Trotzdem freischalten“. (claude-guideui: Klick auf gesperrten Tab → Knopf unter dem Buch, Server prüft Kreativ)
- [x] Astral/Nihil-Redstone soll sich wie Vanilla-Redstone verhalten und dieselben Texturarten haben (Punkt, Linie, Verbindungen – Multipart wie Redstone-Draht). Die Pulver-Textur sieht im Spiel falsch aus (Screenshot: großes, verpixeltes violettes Muster).
  Erledigt auf claude-astral: Ursache war das Modell (eine 16x16-Ebene mit dem Kreuzbild statt Multipart). Jetzt `EndSignalPowderBlock` mit Seiten none/side/up, Punkt/Linie/Kreuz, Wand hoch, Signal über Stufen, nur eigener Kanal; Item = umgefärbter Redstone-Haufen. Plan/Details: docs/ai/PLAN-ASTRAL-NIHIL-REDSTONE-2026-10-02.md. Abnahme im Client offen.
- [x] Texturen von Nihil-/Astral-Schalter und -Lampe sind kaputt → reparieren.
  Erledigt auf claude-astral: Schalter = flache Platte (statt schwebender Ebene), Lampe = voller Würfel wie die Redstone-Lampe, Items zeigen das Blockmodell. Abnahme im Client offen.
- [x] Neue Blöcke: Astral-Kolben (drückt) und Nihil-Kolben (zieht). Mit Signal wird jeder Block im Abstand 1 in alle 6 Richtungen gleichzeitig um genau 1 Block gedrückt bzw. gezogen. Nie 2 Blöcke hintereinander in derselben Richtung. Erst als Konzept/Plan. (claude-pistons, master 46329ec8; Textur A, B/C zur Wahl)
  Konzept fertig: docs/ai/PLAN-ASTRAL-KOLBEN-2026-10-02.md (offene Besitzerfragen am Ende). Umsetzung wartet auf Freigabe.
- [x] Bessere Truhen statt normaler Loot-Truhen, je 1 % Chance:
  - Verstärkte Truhe in der Festung (Stronghold), Netherit-Truhe in der Bastion oder der Netherfestung, Enderit-Truhe in der End-Stadt oder auf dem End-Schiff.
  - Inhalt: doppelter oder höherstufiger Loot.
  - Doppeltruhen: Würfelt die erste Hälfte die bessere Truhe, wird die zweite Hälfte mit 1 % neu gewürfelt. Klappt das, werden beide besser, sonst bleiben beide normale Truhen.
- [x] Enderit-Nugget-Textur passend zur Barren-Textur und zu Vanilla überarbeiten (10 Vorschläge). Eingebaut (claude-texprop): Runde-1-Vorschlag G mit sauber geschlossener rechter Spitze (generate_textures.py ENDERITE_NUGGET, Hauptbaum; previews/enderit-nugget-G-eingebaut.png). 1.21.11-Kopie und Pfeilspitzen-Ableitung (arrow_part_textures.py) im Port-Run/bei Bedarf nachziehen.
- [x] Weisheitserz-Textur etwas kleiner und langsamer animieren. claude-texprop: Erzmuster beider Erze ~1/6 kleiner, Weisheitskugel-Puls frametime 1 → 2 (das Erz selbst ist nicht animiert); `sage_ore_smaller_2026_10_02.py`, previews/weisheitserz-vorher-nachher.png. Sichtabnahme im Client offen.
- [x] Die Advancement-Seite sieht falsch aus (Screenshot: Pink-Schwarz-Fehltextur als Hintergrund im Tab „The Two Shelves“) → Hintergrund-Textur reparieren. (claude-guideui: guides/root.json-Pfad + Test für alle Tab-Hintergründe)
- [x] 10 Alternativ-Vorschläge für die eigenen Besatzvorlagen (Glowing, Pulsating, Emitting). Eingebaut 2026-10-03 (claude-texprop): Besitzer-Motive aus Resprite (rekonstruiert, tools/textures/hand/owner/) 1:1 auf Hintergrund A (`trim_templates_owner_2026_10_03.py`, previews/besatz-besitzer-motive-vorschau.png); Sichtabnahme im Client offen.
- [x] 10 Vorschläge für Netherit-Apfel, Enderit-Apfel und Netherit-/Enderit-Karotte. Eingebaut (claude-texprop): Netherit = Satz A, Enderit = Umfärbung + Enderit-Glimmer (`foods_settled_2026_10_02.py`, previews/netherit-enderit-essen-eingebaut.png); 1.21.11-Kopien im Port-Run.
- [x] Pfeile, die einen Mob getroffen haben, sollen wieder aufsammelbar sein, am besten wenn er gestorben ist (teure Pfeile lohnen sich dann). (claude-combat: fallen beim Tod mit allen Teilen; nur Spieler-Pfeile mit Aufheben erlaubt; `server.arrows`; docs/ai/PLAN-COMBAT-2026-10-02.md)
- [x] Das Rezept des Spawn-Elytra-Pads wird nicht angezeigt (JEI zeigt nur die Info). → Fabric synchronisierte eigene Rezept-Serializer nicht an JEI; jetzt angemeldet (claude-pads, Plan docs/ai/PLAN-PADS-2026-10-02.md). Client-Sicht offen.
- [x] Rezepte der Trank-Pads (claude-pads; II jetzt UNCOMMON wie die übrigen Netherit-Stufen):
- [x] Rezepte der Trank-Pads: (claude-pads, master 8d443a67)
  - Verstärktes Trank-Pad: Netherit-Aufwertung + Netherit-Druckplatte.
  - Infundiertes Trank-Pad 3: Enderit-Aufwertung + Enderit-Druckplatte.
- [x] Das Rezept des Flypads wird auch nicht angezeigt. Höhe je Stufe = Breite der Grundfläche × 2. → gleiche Sync-Ursache; Höhen 8/16/32 (claude-pads).
- [x] Shulkerkopf: Das 3D-Modell ist nur im Inventar zu groß und wird abgeschnitten (Screenshot). → eigenes GUI-Basismodell 0,8× (claude-pads), Sichtabnahme offen.
- [x] Silberfischkopf viel kleiner: im Inventar, auf dem Kopf und abgestellt. → Originalgröße statt 2× (claude-pads), Sichtabnahme offen.
- [x] Guide-Buch-Texturen überarbeiten (10 Vorschläge). Besitzer wählte J (Prachtband), eingebaut auf 26.3 (claude-guideui), Vorschau previews/guide-buecher-J-eingebaut.png.
- [x] Simple Money: 10 Textur-Vorschläge für Special Fiber, 10 für Resin Fiber. Runde 3 aus dem Vanilla-Faden abgeleitet, je A–J (previews/money-fasern-v3-vorschau.png, `money_fiber_proposals_v3_2026_10_02.py`) – Besitzer wählt. (Audit 09.10.: claude-tex6 33249d183: Fasern I/J-Mix eingebaut)
- [x] Simple Money: Schmelzzeiten der Geld-Teile erhöhen, damit es in SimpleBuilding-Welten balanciert ist. claude-texprop: roher Schein → Geldschein 10000 → 24000 Ticks (Vanilla-Schmelzofen 1 Schein/Spieltag, SB-Öfen 2/4/8); Begründung in docs/modules/simplemoney.md.
- [x] Zusätzlicher Weg zu Diamant-Kieseln (In-World): Fällt ein Amboss auf einen Diamantblock, entstehen Diamant-Kiesel. (GPT anvil: 72 Kiesel je Diamantblock, master 8d443a67)
- [x] Auto-Schmied analog zum Autocrafter: automatisiert den Schmiedetisch. (Branch claude-workstations, wartet auf Abnahme)
- [x] (claude-tabswiki, wiki/base_materials.py; Geldschein-Wert 3–35 Smaragde aus den Handels-JSONs) Wiki: Auf jeder Item-Seite beim Rezept die benötigten Grundmaterialien insgesamt auflisten (ab Barren, Holzstämmen, Zuckerrohr, Wachs, Bruchstein …), damit klar ist, wie viel Rohmaterial ein Item kostet. Beim Geldschein sein Wert.
- [x] Strohpuppe / Trainingspuppe (Branch claude-dummy, docs/ai/PLAN-TRAINING-DUMMY-2026-10-02.md; Client-Sichtabnahme und Besitzer-Abnahme der Texturen offen, Vorschau previews/trainingspuppe-vorschau.png):
- [x] Strohpuppe / Trainingspuppe: (claude-dummy, master 8d443a67; Besitzer: ok)
  - Rezept: Rüstungsständer + Strohballen ergibt einen Stroh-Rüstungsständer.
  - Mit aufgesetztem geschnitzten Kürbis wird daraus eine Trainingspuppe mit gutem Minecraft-Namen. Sie ist unzerstörbar, außer beim Abbauen im Schleichen.
  - Sie zeigt allen Schaden an, auch kritische Treffer.
  - Je nach aufgesetztem Kopf zeigt sie den Schaden gegen diese Mob-Art (Gliederfüßer, Untote, Endermen …).
  - Man kann ihr Rüstung anziehen.
  - Bestehende Konzepte im Internet recherchieren und vervollständigen.
- [x] Testzentrale: eine Pfeil-Station, an der jeder Pfeil getestet werden kann (Abschnitt `archery`: alle Befiederungs-, Vanilla-, Spektral- und Trank-Pfeile gegen acht Trainingspuppen; Bau in der Besitzerwelt offen).
- [x] Schachbrett-Blöcke zusätzlich aus poliertem Astralit, poliertem Nihilit und Enderquarz (mit der Textur der polierten Variante; Screenshot der Schachbrett-Zeile).
  Erledigt auf claude-astral: polished_astralit_checker, polished_nihilith_checker, polished_ender_quartz_checker (Rezept 2 polierter Block + 2 Quarzblock → 4). Vorschau previews/polierte-schachbretter-vorschau.png.

## Besitzer 2026-10-02 (Nachtrag)

- [x] (claude-tabswiki, KeyboardInputMixin; Client-Abnahme offen) Geschwindigkeitsmesser: läuft im Autowalk auch im Inventar und in nicht pausierenden UIs (Chat usw.) weiter.
- [x] Shulkerkiste vorerst nicht verzauberbar machen. (claude-combat: Vanilla-Kiste aus constructors_touch_enchantable; Test über alle Shulkerkisten)
- [x] Erzdetektor: Kompassnadel wirkt nicht zentriert – Animation/Nadel-Frames prüfen und zentrieren. claude-texprop: Nabe 2 px, Drehpunkt auf der Ziffernblatt-Mitte x = 7,5, Frames 17–31 gespiegelt; Nadel breiter (4-verbunden wie der Bergungskompass, 2-px-Schweif); Auswahl-Schimmer läuft jetzt auf der Nadel statt am Slot-Rand (Modell ohne Nachschwingen); ruhende Nadel pulsiert sanft (mcmeta); senkrecht (12/6 Uhr) 2 px breit und damit mittig; `ore_detector_centred_2026_10_02.py`, previews/erzdetektor-vorher-nachher.png. Sichtabnahme im Client offen.
- [ ] Port-Run 26.2: Erzdetektor-Texturen in src/main (älteres Nadeldesign) auf die zentrierte, breitere 26.3-Nadel + Ruhepuls bringen, sonst läuft der Auswahl-Schimmer (`OreDetectorNeedlePath`, gemeinsamer Code) auf 26.2 neben der alten Nadel.
- [x] Resonanzstab: soll auch Entities anzünden bzw. scannen können. (claude-combat: Punkt/HUD treffen Lebewesen, Scannen = Leuchten, `server.laser.scanEntities/scanPlayers`; Client-Sichtabnahme offen)
- [x] (claude-tabswiki: Werkzeuge je Stufe, Zweitplatzierungen Kampf/Redstone, Hufeisen; neue Tabs SimpleCombat/SimpleFood; Sichtabnahme offen) Mod-Items zusätzlich an den richtigen Stellen in die Vanilla-Kreativtabs einsortieren; Simple-Building-Tabs weiter aufteilen (Werkzeuge, Waffen, Rüstung usw. wie in Vanilla).
- [x] Trank „Crafty Shulker“: Effekt – bei Treffer an eine sichere Stelle in der Nähe teleportieren; braubar mit Shulkerkopf, analog zu den anderen Tränken/Effekten. (claude-combat: nur 26.3, Seltsamer Trank + Shulkerkopf, Redstone/Schwarzpulver/Drachenatem; Icon-Abnahme offen: previews/crafty-shulker-vorschau.png)

## Besitzer 2026-10-02 (Nachtrag 2)

- [x] Erzdetektor: Die Auswahl-Animation (läuft heute am Slot-Rand, wenn ein Block gewählt ist) soll stattdessen auf der Nadel laufen. (claude-texprop, master 8d443a67)
- [x] Erzdetektor: Nadel vorher breiter machen wie beim Bergungskompass (falls noch nicht geschehen). (claude-texprop, 12/6 Uhr mittig, master 8d443a67)
- [x] Eigene Schallplatte je Dimension, „gehen ab“ wie Pigstep/Otherside: (claude-audio: Voidline/Driftwood/Daybreak/Brimstone mit Platzhalter-Audio, Fundorte, Texturen; echte Musik per `tools/audio/import_discs.py`, Plan `docs/ai/PLAN-SCHALLPLATTEN-2026-10-03.md`; Musik + Client-Abnahme offen)
  - End: Stil wie das Instrumental von „What I've Done“ (Linkin Park).
  - Oberwelt: zwei Platten, eine wie „Stan“ (Eminem), eine im NCS-/Alan-Walker-Stil.
  - Nether: wie „Thunderstruck“.
  - Musik nur stilistisch angelehnt, keine Melodie- oder Sample-Kopie. Audio als Mono-OGG (positionsabhängig in der Jukebox). Fundorte je Dimension (z. B. End-Stadt, Bastion, Oberwelt-Struktur). Platten-Texturen im Vanilla-Stil (10 Vorschläge?).
- [x] Stille Löwenzahn („Silent Dandelion“), gleiche Logik wie die goldene Variante: Benutzung am Mob schaltet dessen Geräusche um (statt Wachstumssperre).
  - Faden → Wollknäuel (auch platzierbar).
  - 8 Wollknäuel + Löwenzahn = stiller Löwenzahn.
  - Wolle im Crafting = 2 Wollknäuel.
- [x] Hängematte: tagsüber darauf liegen lässt die Tageszeit schneller laufen. (claude-hammock, Plan/Stand: `docs/ai/PLAN-HAENGEMATTE-2026-10-02.md`; Client-Sichtabnahme offen)
  - Korrektur auf `claude-gpt-dandelion` (26.3): Aura/Config entfernt; Benutzung toggelt gespeichertes Vanilla-Silent mit Vanilla-Verbrauch, 40-Tick-Pause, Sounds und Partikeln. Lebende Mobs jeden Alters; Spieler, Rüstungsständer, Wither und Enderdrache ausgeschlossen. Blume/Topf dekorativ; Wollknäuel und Rezepte unverändert. Prüfstand und Historie: `docs/ai/PLAN-SILENT-DANDELION-2026-10-02.md`. Sicht-/Audioabnahme offen, kein Client, kein Push.
  - Nur 1×2×2 platzierbar, zwischen zwei festen Blöcken mit 2–3 Blöcken Abstand (beliebige Blöcke, auch Stäbe).
- [x] Tooltips aufräumen: kurze EN/DE-Zeilen, vorhandene Komponenten-/Font-Umbrüche und beide Hauptmod-Lang-Orte gepflegt. Inventur und Prüfungen: `docs/ai/TOOLTIPS-2026-10-02.md`. Client-Sichtabnahme bleibt offen.
- [x] Spezial-Shulker (verstärkt/Enderit): in derselben Struktur je Spezial-Shulker 4 Endermiten spawnen. (claude-loot, beim Annähern eines Spielers, master 8d443a67)

## Besitzer 2026-10-02 (Nachtrag 3)

- [x] Resonanzstab und Rotator: Idle-Animation und Benutzungs-Animation. (claude-anims: Stab Ruhe-Funkeln + Resonanzwellen bei `using_item`; Rotator Ruhe-Glanz + Drehung, solange ein Klick den anvisierten Block drehen wuerde – `simplebuilding:transform_hint`; leer still. Sichtabnahme im Client offen, `docs/ai/PLAN-GADGET-ANIMATIONEN-2026-10-02.md`)
- [x] Erzdetektor: Idle-Animation, Nadel pulsiert, solange nichts gewählt ist. Ist etwas gewählt, läuft die Auswahl-Animation auf der Nadel (siehe Nachtrag 2). (claude-texprop Ruhepuls, master 8d443a67)
- [x] Attractor: Idle-Animation, das Item selbst bleibt unverändert, nur kurz angedeutete Magnetfeldlinien. (claude-anims: ~0,7 s Feldlinien alle ~7 s; Sichtabnahme im Client offen)
- [x] Hufeisen-Vorlage (simpleriding): Basic-Upgrade-Silhouette mit Eisen-Hufeisen und Kupferplatte, Name „Horseshoe Upgrade“ / „Hufeisen-Aufwertung“, EN/DE-Tooltips nach Rezepten. Generator und 16-fache Vorher-/Nachher-Vorschau vorhanden; Sichtabnahme im Spiel offen. Prüfstand: `docs/ai/PLAN-HORSESHOE-TEMPLATE-2026-10-02.md`.
- [x] Echolot: Haltbarkeit wieder wie früher – eine einmalige Nutzung verbraucht die 1500 Haltbarkeit wie vorher. Aktuell wird Haltbarkeit seltsam verbraucht und angezeigt → Ist-Verhalten gegen die frühere Version (git log) prüfen und zurückführen.
  - Umsetzung auf `claude-gpt-echo`: vollständige Entladung auf 26.3 auch mit Unbreaking; Ladezeit, Riss-Stufen und normale Leiste bleiben. 1728/1728 Server-Tests, 26.2-/Forge-26.3-Compile und vollständiges Gate grün. Historie, Entscheidung und Tests: `docs/ai/PLAN-ECHOLOT-HALTBARKEIT-2026-10-02.md`. Besitzerabnahme im Client bleibt offen.
- [x] Hufeisen-Vorlage (simpleriding): Textur und Name nach derselben Konvention wie die Basic-Upgrade-Vorlage. Der Pfeil wird zu einem Hufeisen; innen Eisen-, außen Kupferfarben. (GPT horseshoe, master 8d443a67)
- [x] Echolot: Haltbarkeit wieder wie früher – eine einmalige Nutzung verbraucht die 1500 Haltbarkeit wie vorher. Aktuell wird Haltbarkeit seltsam verbraucht und angezeigt → Ist-Verhalten gegen die frühere Version (git log) prüfen und zurückführen. (GPT echo: volle Ladung je Sprung auf 26.3, master 8d443a67)

## Worker Amboss-Kiesel 2026-10-02
- [x] Zusaetzlicher 26.3-In-World-Weg: fallender Amboss verbraucht Diamantblock fuer 72 Kiesel; JEI/REI/Wiki, EN/DE. Fabric/NeoForge: 1732/1732 Server-Tests, Wiki: 38 Tests, volles Gate und 26.2-/Forge-Compiles gruen. Plan, Nebenbefunde und offene Client-/Integrationsabnahmen: `docs/ai/PLAN-AMBOSS-KIESEL-2026-10-02.md`. Kein Push.

## Besitzer 2026-10-03 (Material-Stäbe)
- [x] Netherit- und Enderitstab wie der Blitzableiter (aufstellbare Blöcke wie Eisen-/Goldstab, Reichweite 96/128), keine Pfeile mehr daraus; nur der Diamantstab bleibt Item und Pfeilschaft. (claude-rods2: 90 statt 126 Fletching-Rezepte, alte Pfeile laden mit Stock-Schaft; Vorschau previews/netherit-enderit-stab-vorschau.png; Fabric/NeoForge 26.3 je 873/873, 26.2-/Forge-26.3-Compile grün; Plan: docs/ai/PLAN-RODS-2026-10-02.md Nachtrag. Sichtabnahme im Client offen.)

## Besitzer 2026-10-03 (Nachtrag 4)

- [x] Redstone-Truhen (trapped chests) für jede der drei Truhen-Varianten (verstärkt, Netherit, Enderit). (`claude-gpt-trapped`: Vanilla-Signal, gleiche Lager-/Upgrade-Eigenschaften, Rezepte/Tags/Loot, EN/DE, Wiki und Texturvorschau; Fabric/NeoForge 26.3 1784/1784 grün, 26.2-/Forge-26.3-Compile und volles check grün. Plan: `docs/ai/PLAN-REDSTONE-TRUHEN-2026-10-02.md`. Client-Sichtabnahme offen; zwei unveränderte Handbuch-Seitenüberläufe separat dokumentiert.)
- [x] Resonanzstab-Rezept: Redstone und Nuggets tauschen und unten rechts jeweils einen davon entfernen. (claude-gpt-recipes: ` NA` / `RC ` / `I  `; Annahme: Felder rechts und unterhalb des Kerns entfernen, da die Ecke bereits leer war. Plan: `docs/ai/PLAN-REZEPTE-PAD-FADE-2026-10-02.md`.)
- [x] Geschwindigkeitsmesser: neues Rezept, gegen den Uhrzeigersinn gedreht. (claude-gpt-recipes: `AN ` / `NCN` / ` NK`; Rezepte, JEI, Wiki und EN/DE in beiden Ressourcenbäumen angepasst.)
- [x] Trainingspuppe neu denken: (claude-dummy Runde 2, master 46329ec8)
- [x] Trainingspuppe neu denken: (claude-dummy Runde 2: Sackkopf, jeder Treffer, Schere, eigenes Item)
  - Ein (verbesserter/Stroh-)Rüstungsständer mit aufgesetztem geschnitzten Kürbis bekommt die Textur einer Trainingspuppe, trägt den Kürbis aber NICHT sichtbar – er wird dadurch zur neuen Entity.
  - Die Entity reagiert bei JEDEM Treffer (aktuell verbuggt: nur beim ersten Treffer eine Anzeige).
  - Mit der Schere rückgängig machen.
  - Beim Abbauen droppt die Trainingspuppe als eigenes Item.
- [x] Stroh-Rüstungsständer: einen Nutzen im Vanilla-Spiel geben. (Vogelscheuche: kein Feld-Zertrampeln im Radius 8, Config 0–16)
- [x] Blaupausen überarbeiten, damit sie besser in Vanilla passen. (Kartenblatt B/C/C-signiert eingebaut, Besitzer zufrieden)
- [x] Pads: Steht man darauf, soll die Textur-Animation einblenden statt mit dem ersten Frame hart zu starten. (claude-gpt-recipes: 26.3, vier Vanilla-Modellstufen über sechs Ticks, keine zusätzlichen Dauerticks; Fabric/NeoForge 1776/1776 grün. Vorschau: `previews/pad-fade-vorschau.png`; Sichtabnahme im Client offen.)
- [x] Raw Enderite Scrap: neue Textur, 10 Vorschläge. (Besitzer-Textur, Variante D dunkler eingebaut)
- [x] Raw Enderite Scrap: neue Textur, 10 Vorschläge. (Besitzer-Textur eingebaut; Farb-Feinschliff siehe Nachtrag 5)
- [x] Astralit und Nihilit: je 3 neue Textur-Vorschläge plus 3 Kontrast-Anpassungen der jetzigen Textur (je 6 Vorschläge). (als Alternativblöcke A–C eingebaut; Material siehe eigener Punkt)
- [x] Mehr Placeables, auch Kerzen und Seegurken; alle Placeables untereinander mischbar machen. (claude-placeables2: 15 neue Teile, Kerzen/Seegurken mischbar)
- [x] Besatzvorlagen Glowing, Pulsating und Emitting: Textur-Animation. (auf Besitzer-Texturen eingebaut)
- [x] Partikeleffekte für manche platzierbare Dinge, z. B. Glowing. (PlacedPartParticles, Client-Option)

## Besitzer 2026-10-03 (claude-texprop)
- [x] Besatzvorlagen Glowing/Pulsating/Emitting: zweite Besitzer-Leinwände 1:1 (eigener Hintergrund), animiert nur auf seinen Motivpixeln (`trim_template_animation_2026_10_03.py`, previews/besatz-besitzer-final-vorschau.png + besatz-animation-*.gif). Sichtabnahme im Client offen.
- [x] Raw Enderite Scrap (layered_raw_enderite): Besitzer-Textur eingebaut, Farben auf die Enderit-Schrott-Rampe (`layered_raw_enderite_owner_2026_10_03.py`, previews/enderit-besitzer-vorschau.png).
- [x] Astralit-/Nihilith-Alternativblöcke A–C eingebaut (veined/crystalline/layered Astralit, veined/crystalline/frosted Nihilith; Meißel-Kette ab Grundblock, Steinmetz, Quadrat-Kette). Grundblock unverändert.
- [x] Astralit-/Nihilith-MATERIAL: je 10 Vorschläge (previews/astralit-nihilit-material-vorschau.png) – Besitzer wählt. (Besitzer-Texturen 1:1 eingebaut, master 46329ec8)
- [x] Blaupause eingebaut (Kartenblatt: frisch B, bearbeitet C, signiert C dunkel + Siegel; previews/blaupausen-eingebaut.png). 1.21.11-Kopien im Port-Run.

## Besitzer 2026-10-03 (Nachtrag 5)

- [x] Wollknäuel: Rezept aus Faden entfernen (nur noch aus Wolle). (master 46329ec8)
- [x] Raw Enderite Scrap (Besitzer-Textur): farblich weiter anpassen, 3 Vorschläge. (überholt durch v3, Wahl D)
- [x] Blaupausen B/C/C-signiert: Besitzer zufrieden.

## Besitzer 2026-10-03 (Nachtrag 6)

- [x] Schallplatten: alternative Track-Variante. (claude-audio: B-Seiten per Vorschlaghammer, nur Mod-Platten; Client-Abnahme offen) Eine abgelegte (platzierte) Platte mit dem Vorschlaghammer schlagen → wird zur Alternativ-Platte (etwas angepasste Optik, sonst gleich, spielt Track 2); erneut schlagen → zurück zum Original (Endlosschleife).
- [x] Lautsprecher-Blöcke: (claude-audio: seit 2026-10-04 Jukebox Amplifier/Musik-Verstärker und Note Amplifier/Noten-Verstärker, Ids jukebox_amplifier/note_amplifier; früher Astralit/Nihilit-Lautsprecher, angrenzend, Config `server.speakers`; Hör-Abnahme im Client offen) Astralit bzw. Nihilit mit Holzbrettern außenrum (Rezept analog Notenblock/Plattenspieler). Astralit-Lautsprecher verstärkt nur Plattenspieler-Signale, Nihilit-Lautsprecher nur Notenblock-Signale: höhere Lautstärke/Reichweite beim Spieler, „unverzögerter Lautsprecher“.
- [x] Raw Enderite Scrap: Variante D leicht dunkler einbauen. (master 46329ec8)

## Besitzer 2026-10-04 (Nachtrag 7)

- [x] Lautsprecher-Texturen dezenter: vom Notenblock ausgehen und daraus eine neue Textur machen; die dunklen Spots des Notenblocks durch Pixel der jeweiligen Rohmaterial-Textur (Astralit/Nihilit) ersetzen. Je 5 Vorschläge, alle Blockseiten gleiche Textur.
- [x] Schallplatten: auch Track 3 und 4 erlauben, falls vorhanden (Vorschlaghammer-Zyklus 1 → 2 → 3 → 4 → 1, nur über vorhandene Tracks). (claude-audio: Track 3/4 entstehen per Import-Skript; echte Musik importiert)
- [x] Lautsprecher verketten: Noten- bzw. Plattenspieler-Verstärker sollen sich gegenseitig weitergeben (nicht unbedingt lauter, aber der Sound erreicht den Spieler über mehrere Lautsprecher hinweg – eine große Villa beschallen).
- [x] Lautsprecher verketten: (claude-audio: BFS-Kette, server.speakers.maxChain 16/64, je Spieler nächster Abspielpunkt; Hör-Abnahme offen) Noten- bzw. Plattenspieler-Verstärker sollen sich gegenseitig weitergeben (nicht unbedingt lauter, aber der Sound erreicht den Spieler über mehrere Lautsprecher hinweg – eine große Villa beschallen).

## Besitzer 2026-10-04 (Nachtrag 8)

## Besitzer 2026-10-04 (claude-texprop)
- [x] Astral-/Nihil-Kolben: Variante C eingebaut (previews/kolben-C-eingebaut.png).
- [x] Texfix-Rücksetzung: spawn_elytra, brick_snowball, alle 7 Simple-Money-Items und die 19 Mod-Verzauberungsbücher wieder auf die Fassung vor dem Textur-Audit (vanilla_style_2026_10_02.py „keep“, generate_textures.py liest hand/q1/books); Vorschau previews/texfix-ruecksetzung.png (inkl. farbreduzierter Vorschläge).
- [ ] Texfix-Entscheidung Besitzer: 52 noch aktive Texfix-Texturen (raw_enderite, Pads, Teleporter, Chunk-Loader, Launchpads, Kupferplatten) – previews/texfix-revert-uebersicht.png, Nummern nennen; Liste: tools/textures/texfix_audit_2026_10_04.py.
- [x] Hufeisen-Vorlage: 10 Vorschläge (previews/hufeisen-vorlage-10-vorschau.png) – Besitzer wählt. → Runde 3 (kleiner, previews/hufeisen-vorlage-runde3-vorschau.png, Branch claude-horseshoe2 aff11c2d) wartet auf Besitzerwahl. (Audit 09.10.: claude-horseshoe2: Motiv H eingebaut 24546fe2e/04a9e1e54)
- [x] Simple-Riding-Bücher (Leaping, Tailwind): Vorschläge aus den alten SB-Büchern (previews/simpleriding-buecher-vorschau.png) – Besitzer wählt; Einbau braucht ein eigenes Buchmodell-Mapping im Modul.
- [x] NEUES MODUL „Simple Sandwiches“ (Konzept + Fragebogen zuerst):
- [x] NEUES MODUL „Simple Sandwiches“ – gebaut auf Branch `claude-sandwiches` (Plan `docs/ai/PLAN-SIMPLE-SANDWICHES-2026-10-04.md` §19), Merge + Client-Abnahme offen. Essenskorb laut Besitzer gestrichen → Essen direkt aus dem Bündel:
  - Sandwiches aus Brot + bis zu 5 Zutaten (Kabeljau, Lachs, Kaninchen, Huhn, Hammel, Schwein, Steak, Kartoffel, Karotte, Apfel, Melone, Spinnenauge, verrottetes Fleisch …, auch goldene/verzauberte Früchte; mit SimpleBuilding auch Netherit-/Enderit-Äpfel usw.). Effekte der Zutaten werden kombiniert (Wahrscheinlichkeiten übernommen), Sättigung/Hunger addiert – mehr auf einmal gegen Zubereitungszeit. Nur gleiche Sandwiches stapelbar. Item-Textur zeigt, was drin ist.
  - Schneidebrett (Block) + Messer (Eisenstufe, wie Schere): Rezept Stock unten links, Nuggets Mitte, rechts oben, unten Mitte, rechts Mitte. ~1/3 Angriffsschaden des Eisenschwerts, wirkt nur auf ausgewählte Dinge: Brot aufschneiden, Butter schmieren, Käse/Melonen/Kuchen schneiden (Kuchenstücke in der Hand essbar), Spinnweben zerstören, Bambus schneller abbauen.
  - Ablauf: Brot aufs Brett, mit Messer aufschneiden, Zutaten stapeln sich im Inneren (voll = keine mehr), mit Messer rückwärts wieder herausnehmen, Butter muss zuerst geschmiert werden; mit leerer Hand zuklappen und herausnehmen, mit Messer wieder öffnen.
  - Butter und Käse im Kessel herstellen (Konzept nötig). Käse = platzierbarer ganzer Block, von oben/seitlich in 16 Scheiben schneidbar, Scheiben stapeln zu 64; Butter ebenso, etwas weniger rutschig als Eis. Butter in Haupt-/Nebenhand + Messer = Brot beschmieren; Butter verstärkt Sandwich-Effekte um 10 % (Dauer, Sättigung, Hunger). Käse = Zutat.
  - Essenskorb (Picknickkorb-Optik): trägt 5 Stacks Essen wie ein Bündel, oberstes Item sichtbar, essbar oder wechselbar, Tooltip wie Bündel.
- [x] Hängematte: bei 3 Blöcken Abstand nicht zentriert → zentrieren; Abstand auf 2–4 erweitern; auch diagonal (erst einen Anker, dann den anderen anklicken); Rezept mit 2. Faden; ersetzt keinen Schlaf, lässt aber auch nachts die Zeit schneller laufen.
- [x] Nihil-Gewölbe („Nihil Vault“): wie das Astralgewölbe, aber eine weltweit geteilte Enderkiste (Größe wie Enderkiste), jeder hat Zugriff.
- [x] Simple QoL: Mit leerer Hand Schleich-Rechtsklick auf eine Truhe merkt sie vor (Partikel an der Hand); öffnet man danach eine 2. Truhe oder GUI (Werkbank usw.), werden beide GUIs untereinander angezeigt (Umräumen, aus Truhe craften). Reichweite ~64 Blöcke. (claude-qolgui: Panel oben/seitlich mit Scrollen, Server-Config 64 (8–128); Rezeptbuch aus Truhe bewusst nicht; Client-Sicht offen)
- [x] Simple QoL: „Easy Shulkers“ und „Easy Ender Chests“ übernehmen (aus dem Inventar öffnen). (claude-qolgui: Luft + Inventar-Rechtsklick, Slot gesperrt, inkl. Stufen-Shulker; Kreativreiter ausgenommen)
- [x] Geschwindigkeitsmesser-Rezept: freie Felder mit Kupfer-Nuggets füllen, dann das Muster um einen Slot im Uhrzeigersinn rotieren. (`claude-gpt-gauge`: 26.3 `NAN / NCN / NKN`, sechs Kupfernuggets; 26.2 unverändert. Prüfstand: `docs/ai/PLAN-GAUGE-RESIN-2026-10-02.md`.)
- [x] Astral-/Nihil-Schienen: bremsen bzw. beschleunigen; Höchstgeschwindigkeit anheben mit realistischer Reibung (je schneller, desto mehr Boost nötig, asymptotische Annäherung); Astral-Schienen boosten stärker als Antriebsschienen.
- [x] Harz-Schachbrett: Rezept nutzt noch den Platzhalter rote Netherziegel → auf Harzziegel (resin_bricks) umstellen. (`claude-gpt-gauge`: 26.2/26.3 inklusive Freischaltung und EN/DE-Prosa; 1.21.11 bleibt Port-Run. Prüfstand wie oben.)

## Besitzer 2026-10-04 (Nachtrag Modul-Unabhängigkeit)
Regeln: `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` (Befunde 1, 4 und 8 auf `claude-modprinciples` gefixt).
- [x] Standalone-Testziel je Modul: `modules.json` `tests.standalone` (nur Modul + harte Libs), Testrunner `module_targets` erzeugt `module-<id>-standalone-fabric-263` (+ NeoForge mit `loadedMods=[nur Modul]`); Integrations-Targets bleiben. (claude-standalone, 20 Targets grün)
- [x] Modul-Tests ohne SB lauffähig machen: SB-Asserts hinter `isModLoaded("simplebuilding")` oder in Integrations-Katalog (QoL, Money, Models, Dimensions, Fun, Riding – Zeilen in der Prinzipien-Datei, Befund 3). (claude-standalone; dabei Befunde 9/10 gefixt)
- [x] Optionale Partner in Loader-Metadaten nachziehen (`suggests` / `type="optional"`, Forge `mandatory=false`) und `tools/multimod.py` prüft Gleichstand mit `modules.json optional` (Befund 5). `gpt-modmeta`: einschließlich ModMenu, Claim-Mod-Erkennung und `simpledimensions`/`simpledimension`-Zuordnung; Prüfstand in `docs/ai/PLAN-MODMETA-2026-10-02.md`.
- [x] Simple Riding: Enderit-Hufeisen über `#c:ingots/enderite` statt fester SB-ID (Condition bleibt; Befund 6). (Audit 09.10.: Duplikat der nächsten Zeile, erledigt)
- [x] Simple Riding: Enderit-Hufeisen-Rezept und optionaler Reparaturtag über `#c:ingots/enderite` statt fester SB-ID (Conditions bleiben; Befund 6). SB liefert den Tag bereits; Forge-Konverter und alle drei Riding-Serverziele geprüft.
- [x] Simple Sandwiches (Branch `claude-sandwiches`): Crucible-Grundrezept mit Axt, SB-Variante mit `#simplebuilding:sledgehammer_tools` nur hinter `mod_loaded` (Regel 5). (Audit 09.10.: überholt: Tiegel liegt in simplelib, Axt-Weg AxeWays, Vorschlaghammer nur mit SB (claude-crucible c2cbe4012))

## Nachtrag 10 (Besitzer 2026-10-04 nachts)
- [x] Hängematte in beliebigem Winkel platzierbar (nicht nur gerade/45°). (Audit 09.10.: claude-hammock3 02b0df0eb)
- [x] Trainingspuppe-Icon A–C und Sage Orb kleiner A–C (previews/trainingspuppe-textur-vorschau.png, previews/sage-orb-vorschau.png, Branch claude-tex5 7686802f) – Besitzer wählt. (Audit 09.10.: claude-tex6 33249d183: Puppe C, Sage Orb A)
- [x] Auto Smither: Ergebnis-Slot nicht befüllbar, UI an Vanilla-Schmiedetisch angleichen (GPT gpt-smither). (Audit 09.10.: gpt-smither 50b61c1f3, gpt-checkui ada721570)
- [x] Tooltips Basic/Enderite Upgrade Template; Amplifier immer volle Lautstärke; Erz-Ausbeutebonus der Öfen entfernen (GPT gpt-small9). (Audit 09.10.: gpt-small9, Abschnitt small9)
- [x] Senkrechte Stäbe (Stock, Knochen, Lohen-, Böen-, Diamantstab) inkl. Hängematten-Anker (claude-rods3 f01726cb, gemergt).
- [x] Crucible (Plan docs/ai/PLAN-CRUCIBLE-2026-10-04.md, Fragebogen komplett beantwortet) – Umsetzung läuft auf claude-crucible. (Audit 09.10.: claude-crucible 6e82038b4 gemergt)
- [x] Guide-Buch je Modul + FTB-Quest „Buch gratis“ je Modul + gemeinsame Bibliothek (Guide-Plan Schritt 3/4) – Agent claude-guides2. (Audit 09.10.: claude-guides2 69c6c7443; Abnahmen offen)
- [x] Messer-Rezept wie Besitzer-Diktat (`  N / NN / SN `), Schmiedetisch-Rezeptbuch ohne `*_armor_upgrade_dummy`, Nihil-Gewölbe-Preis/Handel in Simple Money – GPT gpt-gaps. (Audit 09.10.: gpt-gaps 93eb23898/312898459/250e82998)
## Besitzerauftrag small9 (2026-10-04)
- [x] Basic-/Enderite-Tooltips wie Vanilla, EN/DE, Strukturtest.
- [x] Verstärker innerhalb der Hörweite immer voller Pegel.
- [x] Ofen-Ausbeutebonus entfernen; doppelte XP behalten, Altwelten-Fixtures geprüft.
  Belege: `docs/ai/PLAN-SMALL9-2026-10-02.md`; 1870/1870 Server grün, Gesamt-Gate grün.
  Sicht-/Hörabnahme und Testzentrale in der Besitzerwelt bleiben offen; kein Client/Push.

- [x] Crucible / Schmelztiegel (Konzept + Fragebogen zuerst: `docs/ai/PLAN-CRUCIBLE-2026-10-04.md`, Branch `claude-crucible`): (Audit 09.10.: claude-crucible 6e82038b4 + simplelib c2cbe4012, P5/P6; Client-Abnahme offen)
  - [x] Neue Ofen-Station, so schnell wie ein normaler Ofen, gart mehrere verschiedene Dinge parallel; Ergebnisse in den nächsten freien Slot. (Audit 09.10.: claude-crucible)
  - [x] Stufen (Runde 1): Eisen 6, Verstärkt 9, Netherit 18, Enderit 27 (nur SB, doppelte Stackgröße); Tempo wie die SB-Ofen-Stufen (1×/2×/4×/8×), Hitzefaktor niedrig 0,5× … extrem 1×. (Audit 09.10.: claude-crucible)
  - [x] Herstellung in der Welt: Vorschlaghammer auf Eisenblock, Eisenbarren in der Nebenhand; 4 Schläge = 4 Wände (Eisen-Druckplatten), 2 Schläge = 2 Griffe (Eisenstäbe). Höhere Stufen wie die Ofen-Aufwertungen. (Audit 09.10.: claude-crucible, Forge gpt-forgecrucible)
  - [x] Slot-Indikator im Slot-Hintergrund: gart = heller + Fortschritt; kein Platz = rot (gestoppt); zu wenig Hitze = blau. (Audit 09.10.: claude-crucible, später N12b)
  - [x] Kein Brennstoff, sondern Hitzequelle: Lagerfeuer/Magma = mittel, Lava = hoch, Seelen-Lava = extrem; niedrige Stufe (Fackel/Kerze/Seelenfeuer) vorgeschlagen. (Audit 09.10.: claude-crucible)
  - [x] Seelen-Lava (neue Flüssigkeit): Quell- und Fließblock nicht ersetz-/überbaubar, entfernen nur durch Aufnehmen der Quelle mit Eimer. Weltgenerierung nur im Nether: ca. 0,5 % statt einer Lava-Tasche, in Netherfestungen 10 % Chance je Lavaquellen-Raum; sonst nirgends. (Audit 09.10.: claude-crucible P5 5155aee59, SoulLava/SoulLavaFortressMixin)
  - [x] Kupfer-Eimer: nimmt keine Seelen-Lava, nur normale Lava, zerbricht beim Ausgießen von Lava. Eisen-Eimer zerbricht beim Ausgießen von Seelen-Lava. Enderit-Eimer (Schmiedetisch, direkt vom Eisen-Eimer) zerbricht nicht. (Audit 09.10.: claude-crucible P5, ModBucketItem)
  - [x] Warmes Essen: Tiegel wärmt Sandwiches und andere warme Speisen auf; warm 15 % schneller essbar; bleibt ca. einen halben Tag-Nacht-Zyklus warm, im Bündel ca. 2 Zyklen; beim Stapeln Mittelwert der Wärme; Glow um die Items (Stärke ~ Restwärme). (Audit 09.10.: simplelib 4e055c063 warm/)
  - [x] Auch im Modul Simple Sandwiches (eigenständig spielbar): Tiegel + Warm-Food; ohne SB mit der Axt statt dem Vorschlaghammer. Aufteilung SB/Modul/Bibliothek `simplelib` siehe Plan §3 und `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`. (Audit 09.10.: f789ae6f9 warme Sandwiches über SimpleLib)
  - [x] Fragebogen Runde 1 (F1–F37) beantwortet, eingearbeitet (Plan §2; gemeinsamer Kern als Bibliothek, seit Runde 2 `simplelib`, Dorf-Feldküche neu).
  - [x] Fragebogen Runde 2, Fragen 1–31 beantwortet und eingearbeitet (Plan §2b: Bibliothek `simplelib`, Axt als Vanilla-Ersatz für den Vorschlaghammer, Hitzestufen neu als Frage 51, BER-Inhalt sichtbar, Seelen-Lava 2/5, Seelenbrand, verstärkter Kessel).
  - [x] Ofen-Ausbeutebonus (Netherit-/Enderit-Schmelzofen, `FurnaceTierPerks`) aus SB entfernen – macht ein anderer Helfer im Code (Runde 2 Frage 1). (Audit 09.10.: 19b4e9e2a)
  - [x] Kupfer-Fass (Kupfer/Verstärkt/Enderit, 9 Slots) per Vorschlaghammer (ohne SB Axt) in 6 Schlägen an den Tiegel anbringen, sichtbar verbunden; Tiegel-GUI zeigt die 9 Fass-Felder; Ergebnisse zuerst ins Fass; Trichter unter dem Fass (Plan §8a). (Audit 09.10.: 4e055c063, claude-crucible4 7c9db58e9)
  - [x] Seelen-Lava: 4× entflammbarer als Lava, doppelte Zündreichweite; „schmilzt 10× mehr“ (★ Brennstoff 10× Lava, Plan §9/§10). (Audit 09.10.: 3e6221811 Seelenlava-Tuning)
  - [x] Besitzer beantwortet die offenen Fragen 32–61, danach Umsetzung. (Audit 09.10.: fe98860e0)
  - [x] P5/P6 Textur-Platzhalter (2026-10-05, `claude-crucible-gpt`): 34 PNGs und zwei Vanilla-Animationsmetadaten; Enderit-Tiegel/Fass, Seelenlava, Kupfer-/Enderit-Eimer, Seelenbrand und verstaerkter Kessel. Vier A/B/C-Vergleichstafeln nur in `C:/Users/o_o/code/minecraft-mods/previews/`; Auswahl und Ingame-Abnahme offen.
  - [x] Eimer-Nacharbeit 2026-10-05 (`claude-crucible-gpt`): Kupfer-/Enderit-Eimer erhalten Vanilla-Kontur und alle Schattierungen der Oeffnung; Wasser/Lava unveraendert. Pixelchecks, Texturgenerator --check, Wiki --check und Gradle check -q gruen (Java 25, vorgegebenes Python im PATH). 16x Alt/Neu und deutlich eigene A/B/C-Beschlaege unter `previews/eimer-*-vorschau.png`; Vorschlaege nicht eingebaut, Besitzer-Abnahme offen.
## Forge Auto Smither (Besitzer 2026-10-05)
- [x] Forge 26.3 Auto Smither: Forge-Capability nutzt vorhandene Seitenregeln; Trichter entnehmen nur Ergebnisse, alle drei Eingaben bleiben erhalten. Filter 6/6, volle Server 2839/2839 und `check -q -PskipWiki` gruen. Branch `gpt-forgesmither`, Belege: `docs/ai/PLAN-FORGE-AUTO-SMITHER-2026-10-02.md`; kein Push/Client.

## Guides Schritt 3/4 (Besitzer 2026-10-01: „Zu jeder Mod ein herstellbarer Guide, mit FTB Quests am Anfang gratis“)
- [x] Guide-Buch je Modul (Branch `claude-guides2`, Plan `docs/ai/PLAN-MODUL-GUIDES-2026-10-05.md`): Vanilla-Buchansicht mit EN/DE-Seiten aus `wiki/manual.json`, Rezept Buch + Vanilla-Item, FTB-Startquest schenkt das Buch (nur mit FTB Quests), Texturen A–H (`previews/modul-buecher-vorschau.png`); Visuals/Sounds als Client-Mods ohne Item über `/simplevisuals guide` bzw. `/simplesounds guide`. Generator `tools/guides/module_guides.py` (`checkModuleGuides`). Offen: Merge, Client-Sichtabnahme (Seitenumbruch, Befehle), FTB-Quests im echten Spiel, Umzug der Vorlage nach `simplelib`.
## Besitzer 2026-10-04 (Nachtrag 9)

## Forge Crucible (Besitzer 2026-10-05)
- [x] Forge 26.3: fehlende SimpleLib-Pack-Metadaten ergaenzt; Vorschlaghammer baut Crucible wieder. Bautest prueft Tags, echten Item-Aufruf und jeden Schlag. Forge-Filter 14/14, volle drei Serverziele 2881/2881, Gate und Pflicht-Compiles gruen. Branch gpt-forgecrucible; Plan docs/ai/PLAN-FORGE-CRUCIBLE-2026-10-02.md; kein Push/Client.

## Crucible P6 (2026-10-05)
- [x] `claude-crucibleart-gpt`: Seelen-Lava-Server-Config, Jade-Tiegelstatus und JEI-Schmelztiegelkategorie umgesetzt. Fabric-Tests Crucible 16/16, Config 15/15, Testzentrale 7/7; Pflicht-Compiles, Wiki, JUnit (inkl. Jade-Split) und einzelne Gate-Prüfungen grün. Gesamtcheck dreimal am 600-s-Limit beendet, kein grüner Gesamt-Gate-Abschluss. Plan/Belege: `docs/ai/PLAN-CRUCIBLE-P6-2026-10-05.md`. Kein Push/Client/Artwork.
## Checker und Crafter-UI (2026-10-05, gpt-checkui)
- [x] Netherziegel-/rote-Netherziegel-Quarz-Checker vollständig integriert; EN/DE, Wiki, Money und Testzentralen-Abdeckung. 2881/2881 Server grün, check -q und 26.2-Compile grün. Plan PLAN-CHECKER-2026-10-02.md.
- [x] Auto Smither im Crafter-Stil: zentrierter Titel, drei Geisterbild-Eingaben, großer Ergebnisrahmen, exakte Slotpositionen. Alle Auto-Smither-Tests auf drei Loadern und volles Gate grün. Plan PLAN-SMITHER-CRAFTER-UI-2026-10-02.md. Keine Clienttests, kein Push; Sichtabnahme offen.

## Riding-Bücher und Hammer-Splitter (2026-10-05, gpt-chips2)
- [x] Leaping C / Tailwind A; Hammer: Eis 4, Packeis 9, Obsidian 9, abgelegte Feuerkugel 4 Splitter. Plan: `docs/ai/PLAN-CHIPS2-2026-10-02.md`. Nur Branch-Commits, kein Push/Client.
  Belege: 1986/1986 alles gruen (Fabric/NeoForge 973 je Loader, Riding 40), Gesamt-Gate und Pflicht-Compiles Exit 0, Wiki/Texturen gruen. Sieben neue GameTests je Hauptloader; Testzentralen-Aufbau und Abdeckung gruen.

## Worker Cover/Config 2026-10-05
- [x] Cover Option B auf 26.3: beide Bucheinträge entfernt, keine Survival-Ersatzquelle. Zwölf neue Config-Ideen bewertet, vier begrenzt umgesetzt (Auto-Schmied, Diamantkiesel, Pfeilchance, Scan). Fabric/NeoForge 1954/1954, alles gruen; check/checkBalance/Pflicht-Compiles/Wiki grün. Branch `gpt-coverconf`, kein Push/Client. Plan: `docs/ai/PLAN-COVER-CONFIG-2026-10-02.md`.

## Nachtrag 11 (Besitzer 2026-10-06, Screenshots images/16–19)
- [x] Seelenbrand: Dauer verdoppeln; sichtbarer Statuseffekt (leichter Blau-/Dunkelfilter); auf Kaltem (Eis, Schnee, Wasser …) Schaden im halben Intervall. (Audit 09.10.: claude-oc-tweaks11 2e67933c2 (N11 P1))
- [x] Magnet: höhere Reichweite. (Audit 09.10.: 9d313e818 (N11 P2))
- [x] Vorschlaghammer + Besatz-Interaktion: Rechtsklick wie alle In-World-Umwandlungen; alle In-World-Umwandlungen auf Konsistenz prüfen. (Audit 09.10.: claude-hammer12 5db8a809a; Inventur docs/ai/INWORLD-UMWANDLUNGEN-2026-10-06.md)
- [x] Crucible-UI scannen und verbessern: v2 zentriert, Hitze/Feuer eingelassen + Tooltip, Fass-Platzhalter; Vorschau `previews/crucible-ui-v2-vorschau.png` (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [x] Crucible↔Kupfer-Fass-Verbindung (Risse je Schlag, Flansch+Rinne, 9 Felder/Rest droppt, Tiegel-UI, Abbau beider Seiten) (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen): Zerstörungs-Indikatoren beim Anbringen, neues verbundenes Modell, verbundenes Fass nur 9 Felder und öffnet die Crucible-UI; Abbau Fass → Inhalt droppt, Fass wird normal (analog Crucible).
- [ ] simplelib-Elemente (Kessel usw.) immer in die Kreativtabs der jeweiligen Mods verteilen.
- [x] Item-Texturen aus Screenshot überarbeiten (images/16); Speer nur Enderit-Glimmern; Kupfer-Eimer runder und mehr Kupfer statt Porzellan; neuer Keramik-Eimer (3 Ton → roh, brennen; 16 bzw. 32 Füllvorgänge, dann kaputt); Kupfer-Eimer höchster Oxidation nicht nutzbar. → `claude-tex7` (Plan `docs/ai/PLAN-TEX7-EIMER-KERNE-2026-10-06.md`, Generator `texture_round7_2026_10_06.py`); offen Besitzer-Abnahme/Client-Sicht.
- [x] Schachfiguren in Checker-Farben (Steinmetz): 1/8-Block (0,5³) im Sub-Raster platzierbar, wasserbindbar solange < 8/8; daraus Figuren craftbar; Checker-Stufen und -Platten; Schleichen+Rechtsklick ersetzt Figur (alte in die Hand) bzw. nimmt sie auf; je Figur 2D- (von oben lesbar) und 3D-Variante.
  Erledigt auf `claude-chess` (Plan docs/ai/PLAN-SCHACH-2026-10-06.md): 13 Farben (12 Checker + Quarz), ein Block `checker_octet` (Farbe + 8 Bits), ein Block `chess_pieces` (Block-Entity, 4 Figuren je Block auf den Checker-Feldern), 156 Figuren-Items, 24 Treppen/Stufen; Station `chess`. Vorschau previews/schach-vorschau.png. Offen: Client-Sicht (Renderer, GUI-Modelle).
- [x] Raw-Enderite-Scrap-Rezept: 4 Fragmente statt 3. (Audit 09.10.: 7f811a0a3 (N11 P3))
- [x] Kessel-In-World-Umwandlung in JEI (Kategorie `cauldron_world`); Bild 17 = verstärkter Kessel als flache Seitentextur → 2D-Item-Sprite; verstärktes Fass zusätzlich heller wie Truhe (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [ ] Andere Mods an SimpleBuilding-Stil angleichen (sauber, einheitlich). (teilweise: Mod-UIs im Container-Stil (simplecontainers G4 d5ec6b840); Stil-Audit der Module fehlt)
- [x] Verstärkter Kessel erbt vom Kessel (alle Funktionen inkl. Milch). (Audit 09.10.: Duplikat, erledigt claude-crucible4 (unten))
- [x] Milchkessel-JEI/Jade fixen (images/18: „Empty 1B“). (Audit 09.10.: Duplikat, erledigt claude-crucible4 (unten))
- [x] Netherit-Fass fehlt. (Audit 09.10.: Duplikat, erledigt claude-crucible4 (unten))
- [x] Eimer mit Seelen-Lava vanilla-näher; Enderit-Eimer: Eimer/Glimmern animieren, nicht den Inhalt (images/19). → `claude-tex7`.
- [x] Verstärkter Kessel erbt vom Kessel (Vanilla-Interaktionstabellen, Stufen, Regen/Tropfstein, Milch → verstärkter Milchkessel) (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [x] Milchkessel-JEI/Jade fixen (Fluid-Zeile entfernt, Topic `cauldron`: Inhalt/Reife/Füllstand; nur mit SB, Restzeit ohne Server-Daten nicht möglich) (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [x] Netherit-Fass (45 Felder, Netherit-Truhen-Stil, Verstärkt → Netherit → Enderit) (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [x] TODO mit Rückfrage später: Blaupause überarbeiten. (Audit 09.10.: claude-q-blueprint df6856c0f (N23-Umbau))
- [x] Resonanzstab: bei Nutzung weiter nach vorne neigen (Laser aus der Spitze), Laser amethystfarben. (Audit 09.10.: d11d6a506 (N11 P7))
- [x] Kern-Items: Schimmer-Animation. → `claude-tex7`.
- [x] Elytra-Pad drei Stufen: 5 / 32 (Netherit) / 128 (Enderit, Höhe ggf. 1,5×128). (Audit 09.10.: 0db3f38cc (N11 P4))
- [x] Trank-Pad etwas buffen. (Audit 09.10.: af35d7f6c (N11 P5))
- [x] Kreativ-Abstandshalter nur wo nötig, Lücken größtenteils schließen. (Audit 09.10.: 45f36c970 (N11 P8), ersetzt durch N22 ff143698a)
- [x] Crucible betretbar; ab hoher Hitze Schaden wie Magma. (Audit 09.10.: 5dea165f4 (N11 P6))
- [x] Kern-Animationen mit Seltenheit (drehen, Bumerang, hochsteigen …; je cooler desto seltener), eigene längere Animation bei Erz-Umwandlung. → `claude-tex7` (`CoreHandMotion`, Erste Person, Config `tools.enableCoreAnimations`); Client-Sicht offen.
- [ ] Rückfrage beantwortet: Elytra mit Reparatur im Schmiedetisch – Vanilla-Schmiederezepte prüfen nur Items, keine Verzauberungen; möglich nur mit eigener Rezept-Zutat je Loader (offen: soll das gebaut werden?).

## Nachtrag 12 (2026-10-06, Besitzer, Referenzbilder in minecraft-mods/previews/refs-n12)
- [x] Keramik-Lavaeimer fehlt (claude-crucible5) (Audit 09.10.: claude-crucible5 041b4bec6)
- [x] Verstärker (Amplifier) funktionieren nicht (claude-hammer12) (Audit 09.10.: claude-hammer12 a6ae00f92)
- [ ] Kern-Item-Animation ruhiger: nur etwa alle 10 s einmal (claude-tex8)
- [ ] Verstärkter Kessel: Item-Textur verbessern (claude-tex8)
- [x] Kessel doppelt so teuer (z. B. 2 rissige Diamanten statt 1 usw.) (claude-hammer12) (Audit 09.10.: claude-hammer12 df97edc09)
- [x] Alle In-World-Umwandlungen schrittweise: Ergebnis-Items erscheinen nacheinander je Schlag (pro Schlag ein Item) (claude-hammer12) (Audit 09.10.: claude-hammer12 9c49fc10f)
- [x] Tiegel-Fortschrittsbalken dezent: untere 2 Pixelreihen des Slots als Fortschritt (Vorschlag) (claude-crucible5) (Audit 09.10.: claude-crucible5 041b4bec6, N12b 3f79a6241 (Ofen-artige Füllung))
- [x] Tiegel-Feuer im Stil von Bild 1/2 (claude-crucible5) (Audit 09.10.: claude-crucible5 041b4bec6)
- [x] Tiegel-GUI: unterer Trennstrich trennt die beiden Container, Stil wie Bild 3/4; je ein Vorschlag zu Bild-4-Stil und Slot-Fortschritt (claude-crucible5) (Audit 09.10.: claude-crucible5 041b4bec6/3f79a6241)
- [x] Alle jetzt entwickelten UIs im Stil von Bild 3/4 (nach Freigabe des Vorschlags) (Audit 09.10.: simplecontainers W0 + G1–G4 717b55ea1…d5ec6b840, cp-scfix 080287f54; Besitzer-Abnahme Runde 2 offen)
- [x] TODO neue Mod „simplecontainers“: clientseitig, verschönert alle GUI-Container im Stil Bild 3/4, mit Parität für Mod-UIs (Audit 09.10.: 717b55ea1 ff.; braucht simplelib, also nicht rein clientseitig)
- [x] Angedocktes Fass in jeder Dimension 1 px kleiner, näher an den Tiegel (claude-crucible5) (Audit 09.10.: claude-crucible5 041b4bec6)
- [x] Speer glimmert immer noch nicht, mit anderen Enderit-Werkzeugen/Barren vergleichen (claude-tex8) (Audit 09.10.: a94ee72d8 sichtbarer Glanz; Glimmerpunkte-Wunsch siehe N21)
- [ ] Platzierter Knochen 3D: wie Knochen, oben/unten symmetrisch (claude-tex8)
- [ ] Rest Nachtrag 11: Gameplay-Punkte (claude-agy-tweaks11, Antigravity-Test), In-World-Konsistenz (claude-hammer12), Stil-Audit (wartet auf codex login) (teilweise: Gameplay claude-oc-tweaks11 P1–P8 und claude-hammer12 gemergt; Stil-Audit fehlt)

## Nachtrag 14 (2026-10-06, Besitzer) – nur starten, wenn kaum Claude-Tokens nötig (Helfer-CLI opencode/agy/codex); sonst hier liegen lassen
- [x] **opencode-Probe:** Nachtrag-11-Gameplay (Prompt scratchpad gpt-tweaks11.md) mit `opencode run -m opencode/big-pickle --auto` im eigenen Worktree ab origin/master; braucht Claude-Code-Neustart (Erlaubnisregel). Danach N13 Testzentralen ebenso. (Audit 09.10.: claude-oc-tweaks11 49ab76a2d; N13 Testzentralen weiter offen)
- [ ] **Tiegel-Feuer:** Mischung aus spitzer (POINTED) und breiter (BROAD) Form; Zungen animiert zwischen spitz und stumpf wechselnd (Verhalten/Rhythmus von Anime-Feuer-Referenzen: uppbeat.io anime-fire-transition-6851 und animated-nime-electricity-element-6826 – nur Bewegungsverhalten übernehmen, Stil bleibt Pixel). Jede Hitzestufe etwas größer; die niedrige Flamme ist zu klein. (teilweise: Form „Mittel“ eingebaut 5037a180a; größere Flammen je Hitzestufe nicht belegt)
- [ ] **Tiegel-Fenster ab Enderit:** Tiegel- und Fass-Kasten untereinander statt nebeneinander (Fenster zu breit). Fass-Kasten bekommt Titel „Crucible Barrel“ bzw. „Crucible Storage“ (DE: „Tiegel-Fass“/„Tiegel-Lager“) – Name wählen. (teilweise: Fass-Titel entfällt (Besitzer 09.10.); Kästen untereinander ab Enderit fehlt, Fass-Kasten steht daneben)
- [ ] **Vorschau:** Bilder der Tiegel-Hintergründe aller Stufen (ohne/mit Fass) ganz ohne gerenderte Items.
- [x] **Super-/Sub-Mod-Konzept** festgehalten: docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md. Simple QoL wird Super-Mod; simplecontainers wird Sub-Mod (standalone, Super-Mod requires sie). Config der Super-Mod: oberster Punkt je Sub-Mod „Enable Simple XY“. (Audit 09.10.: Doku docs/ai/KONZEPT-SUPERMOD-SUBMOD-2026-10-06.md)
- [ ] **simplecontainers – Verknüpfte Container neu:** erster Container per Schleich-Rechtsklick vormerken → kleines HUD-Element (nur HUD, kein Screen) oben links: ein Slot im allgemeinen GUI-Stil (Bild 3/4) mit dem Item/Block des vorgemerkten Containers. Inventar öffnen → vorgemerkte GUI wird zusammen mit dem Inventar angezeigt. Anderen Container (normaler Rechtsklick, ohne Schleichen) öffnen → zweite GUI oben, vorgemerkte GUI unten statt Inventar. Nur die Vormerkung braucht Schleichen. Abbruch-Vorschläge (einfach, nicht störend), Besitzer wählt: (a) erneuter Schleich-Rechtsklick auf denselben Container, (b) Schleich-Rechtsklick in die Luft/auf Nicht-Container, (c) Entfernung > Reichweite oder Dimensionswechsel hebt automatisch auf, (d) Zeitlimit (z. B. 60 s, HUD-Slot verblasst), (e) Taste (nicht belegt, im Steuerungsmenü frei wählbar). Empfehlung: a + c als Standard, e optional.
- [x] **simplecontainers – Stil:** alle Container-GUIs im Stil Bild 3/4 (refs-n12) inkl. Mod-UI-Parität (siehe simplecontainers-todo). (Audit 09.10.: simplecontainers W0 + G1–G4, cp-scfix)
- [ ] **QoL-Aufteilung:** Besitzer entscheidet über Sub-Mods (Vorschlag im Chat 2026-10-06): Container, Bewegung, Landwirtschaft, Werkzeuge/Haltbarkeit, Kreaturen, Wetter, Tresor.

### Nachtrag 14 – Besitzer-Entscheidungen (2026-10-06)
- Vormerkung abbrechen: a (Schleich-Rechtsklick auf denselben Container), b (Schleich-Rechtsklick in die Luft/Nicht-Container), c (außer Reichweite/Dimensionswechsel), d (Zeitlimit, großzügig bemessen, per Config einstellbar mit harten Grenzen).
- Kreaturen stummschalten bzw. mit Löwenzahn jung halten → Logik in simplelib, Integration aktiv, wenn simplequalityoflife oder „simple mobs“ installiert ist.
- Tresor-Cooldown → eigene Sub-Mod „simple loot“/„simple looting“ (Name final wählen).
- Schärfe/Schwert schneidet Gras → Sub-Mod „simple combat“ (passt nicht zu Farming).
- [ ] **Übersichtsdokument** im Repo: alle Mods und Sub-Mods mit detaillierten Features (z. B. docs/MODS-UND-FEATURES.md); muss bei jeder Feature-Änderung aktualisiert werden (Regel in den Projektregeln verankern, ideal mit Prüf-Gate gegen modules.json). Grundlage für die Entscheidung, wohin Features gehören. (teilweise: docs/MODS-UND-FEATURES.md + Regel in AGENTS.md (claude-q-audit); Prüf-Gate gegen modules.json fehlt)

## Nachtrag 15 (2026-10-07, Besitzer)
- [x] **Bug: Alle Mod-Eimer verschwinden nach dem Benutzen** (Ausgießen/Schöpfen soll leeren bzw. gefüllten Eimer zurückgeben; Keramik nur Abnutzungsstufe). Höchste Priorität. (Audit 09.10.: 6f552f7fc + NeoForge e436faeee, Tests 1cf77e030)
- [x] **Tiegel-Fass:** Hitbox des angedockten Fasses korrigieren (Outline/Kollision passend zum kleineren Modell); Wallhack/X-Ray-Effekt (durchsichtige Nachbarflächen, falsches Culling/Render-Layer/Occlusion) beheben. (Audit 09.10.: 81a26d24a)
- [ ] **In-World-Umwandlung vereinheitlichen:** Animation und Ablauf (Rechtsklick, Risse, Partikel, Klang, schrittweise Teil-Ergebnisse) exakt wie SimpleBuilding; Logik in simplelib verschieben (InWorldStrikes o. ä.), damit alle Mods dieselbe Implementierung nutzen. (teilweise: gemeinsames InWorldStrikes (Zählung, Risse, Partikel, Klang) 5db8a809a, liegt aber in SB common/src/shared/.../util; Umzug nach simplelib fehlt)
- [ ] **Schrittweiser Umbau je Schlag (universell):** Jeder Schlag verändert sichtbar Richtung Ziel. Fass am Tiegel: bei jedem Schlag ein Stück näher am fertigen angedockten Fass (Zwischenmodelle). Gleiches Modell → Textur-Overlay der Zieltextur, Stück für Stück in einem Anbau-Muster aufgedeckt. Verändertes Modell → je Schlag ein Zwischenmodell oder Keyframes über mehrere Schläge, aber jeder Schlag bringt eine Veränderung. Als universelles System in simplelib für alle In-World-Umwandlungen. (teilweise: Ergebnis-Items je Schlag 9c49fc10f; Zwischenmodelle/Overlay-Aufdeckung fehlen)
- [ ] **Schach:** 0,125er Schachfiguren (0,5×0,5×0,5) rendern nicht bzw. falsch. Checker-Treppen und -Stufen haben im Inventar das falsche Blockmodell (vermutlich Seiten vertauscht). (teilweise: Checker-Treppen/-Stufen gespiegelt e27188645; Achtel-Figuren rendern weiter falsch (Screenshot 08.10.))
- [ ] **Mehr 0,125er Blöcke** als Erweiterung der Farbpalette: Teile von Vanilla-Blöcken.
- [ ] **Enderit-Tiegel Stapelgröße:** zeigt 64 statt 128 an. Modifizierte Stapelgrößen als gemeinsame Lösung in simplelib (Anzeige, Slot-Limit, Transfer automatisch konsistent). (teilweise: Tiegel + angedocktes Fass über simplelib StackLimits 0a84dab08; loses Enderit-Fass, Trichter-Transfers und TieredChests noch auf 64)
- [x] **Fass erbt Stapelgröße:** Hat ein Tiegel modifizierte Stapelgröße, hat das angedockte Fass dieselbe. (Audit 09.10.: 0a84dab08)

## Nachtrag 16 (2026-10-07, Besitzer; Screenshot previews/refs-n12/screenshot-n16-eisentiegel.png)
- [x] **Tiegel-UI:** gelbe Linie über/auf dem Feuer entfernen (unterer Rand des Flammenbands, siehe Screenshot). (Audit 09.10.: 3c119ed23)
- [x] **Tiegel-Logik:** Items, die schon im Tiegel liegen, bevor das Fass angebaut wird, verhalten sich falsch; reservierte Slots und Berechnung spinnen danach. Reproduzieren (GameTest: erst Items einlegen, dann Fass anbauen, Reservierung/Ergebnis-Slots prüfen) und beheben. (Audit 09.10.: b185d64a6)
- [x] **Enderit-Tiegel:** Hinweis „2× Stacks“ entfernen (selbst entdeckbar). (Audit 09.10.: 3c119ed23)
- [ ] **Fletching Table – Rezeptbuch-GUI neu:** 3 Kategorien (1. Spitze, 2. Stab, 3. Feder); je Kategorie nur die Wahlmöglichkeiten zum Zusammenstellen des Pfeils; je Material sehr kurzer Tooltip mit den Vorteilen. (teilweise: Befiederung einzeilig wie die Werkbank bed08725f; Rezeptbuch mit 3 Kategorien und Material-Tooltips fehlt)
- [ ] **UI-Konzepte:** für jede Mod-UI ein Konzept ausarbeiten (Vorschaubilder), Aussehen streng wie Referenzbilder (refs-n12 Bild 3/4, Rahmenmaße aus PLAN-CRUCIBLE-N12B). (teilweise: Container-/Mod-UIs über simplecontainers (PLAN-SIMPLECONTAINERS-2026-10-08.md, Vorschauen); Einzelkonzepte übriger UIs fehlen)
- [ ] **Baulicht:** Motiv nicht mittig → zentrieren.
- [ ] **Neu: Trapped Copper Chest** (Redstone-Signal wie Vanilla-Trapped-Chest, Kupfer-Stil, Oxidation wie Kupfertruhe falls vorhanden).
- [ ] **Nihil-Gewölbe:** Doppeltruhen-Größe wie das Astral-Gewölbe.
- [ ] **Netherit-Shulker:** dunkle Highlights statt heller (Netherit-Farben); dasselbe für alle anderen Netherit-Maschinen/-Blöcke.
- [ ] **Hängematte wie Leine:** erster Befestigungsklick hängt die Hängematte wie eine Leine (mit Hängematten-Textur) an; Modell sichtbar wie Leine/Lichterketten-Mods während des Ziehens; entfernt man sich z. B. > 10 Blöcke, löst sich die Verbindung von der ersten Seite wieder.
- [ ] **Senkrecht platzierte Knochen/Stöcke/Ruten** (Diamant-, Lohen-, Böen-Rute …) verbinden sich nicht: übereinander platzierte sollen nahtlos verbunden sein (lang genug, keine Lücke).

## Nachtrag 17 (2026-10-07, Besitzer – Nachricht unterbrochen, ggf. Fortsetzung folgt)
- [ ] **Shulker-Zustand:** platzierte Shulkerkiste schließt sich bei Rechtsklick; zwei Item-Zustände (offen/geschlossen) wie die Blume im Creaking-Wald (Augenblüte).
- [ ] **Neue Mobs:** 1. niedlicher End-Mob, der im End liegt (eigener Mob, Textur ähnlich Endstein zur Tarnung), spawnt sehr selten in kleinen Rudeln von 3–5. (Weitere Mobs folgen.)

## Nachtrag 18 (2026-10-07, Besitzer) – Plan: docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md
- [ ] Simple Trims als Sub-Mod von SB (Vorlagen, platzierbar, Axt ohne SB / Hammer mit SB) – Frage F1
- [ ] Simple Maps als Sub-Mod von SB: Wegfinder-Karte (unendlich, Spieler mittig, erweitern/kopieren/kombinieren, GUI mit Lesezeichen, Wegpunkte 1–8, Kontextmenü, Snap/Raster, Locator-Bar) – Fragebogen F2–F10, Feature-Vorschläge 1–8
- [ ] Config je Super-/Sub-Mod: Items in Kreativ-Tabs an/aus (simplelib) (teilweise: SB-Schalter addItemsToVanillaTabs ff143698a; simplelib-Gerüst je Mod fehlt)
- [x] Guides: farbigen Strich an freigeschalteten Lesezeichen entfernen (Audit 09.10.: 33b051693)
- [x] Dev-Kreativtabs immer ans Ende der Reihenfolge (Audit 09.10.: ff143698a (Test: Dev-Tab zuletzt))
- [ ] Sandwiches appetitlicher (Vorschau-Varianten)
- [ ] 0,125er-Blöcke: maximale Stapelgröße 128
- Grundsatz festgehalten: Konsistenz zwischen allen Simple-Mods, Gemeinsames in simplelib, UI-Bausteine (inkl. Kontextmenü) dokumentieren (docs/ai/UI-BAUSTEINE.md anlegen)

## Nachtrag 19 (2026-10-07, Besitzer)
- [ ] **Stufen für Kistenboote, Kistenloren (Chest Boat / Chest Minecart) und Ofenloren (Furnace Minecart)** – analog zu den Truhen-/Ofen-Stufen (Verstärkt/Netherit/Enderit): mehr Slots bzw. Ofenlore mit stärkerem Antrieb/Brenndauer; Rezepte per Schmiedetisch wie die übrigen Stufen, Kreativtab, JEI, Wiki.
- [ ] **0,125er-Block (Achtel) für jeden Block, der Stufen und Treppen hat** (Vanilla + Mod), ohne Schachfiguren. Generator-basiert (Datagen), Stapelgröße 128 (siehe N18), Kreativtab-Einordnung neben Stufe/Treppe.

## Nachtrag 20 (2026-10-07, Besitzer) – Konzept: docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md
- [ ] Simple Maps: Rezept A + seltene Fundorte (auch Nether-/End-Karte) – in PLAN-N18 festgehalten
- [ ] Neue Effekte (zuerst als Tränke): Zittern I/II, Trugbild, Umgekehrtes Trugbild, Verblasst (Graustufen inkl. Inventare, ohne Esc-Menü)
- [ ] Mob Deceiver: Endgame-Gegner, Name, Tarnumhang, Spawns, Eskalation, Top-Animationen (Kupfergolem-Vorbild) – Konzept freigegeben, Umsetzung offen
- [ ] Später: Furcht-Mob (extrem stark, verursacht Zittern) – Konzept folgt

## Nachtrag 21 (2026-10-07, Besitzer)
- [x] **Enderit-Eimer:** (→ `claude-q-ebucket`, Seelenlava ebenfalls 2 Eimer laut N28/Auftrag) Kapazität genau 2 Eimer. Rechtsklick nur aufnehmen; wenn voll, Rechtsklick platziert wieder. Schleich-Rechtsklick platziert einen halben Eimer (nicht einen vollen). Seelenlava nur einfach aufnehmbar (begrenzt, hebt sich ab). Eigene Texturen für die Zwischenstufen (halbvoll je Flüssigkeit).
- [ ] **Enderit-Speer:** statt des Eimer-Glanzes die hellen Glimmerpunkte auf der Enderit-Textur wie Schwert und die übrigen Enderit-Werkzeuge.
- [x] **Puppen/Ständer:** (claude-q-stands; Inventar docs/ai/PLAN-PUPPE-INTERAKTIONEN.md + PLAN-STAENDER-2026-10-09.md, Dreizack/Windladung/Streitkolben/Namensschild ergänzt) mehrere Interaktionen Spieler ↔ Trainingspuppe/Ständer sind unsauber oder funktionieren nicht (z. B. Speer) – inventarisieren, reproduzieren (GameTests), beheben.
- [ ] **XP-Orbs:** bei Nutzung ähnliche Animation wie die Kerne (gleiches Prinzip, eigene Bewegung).
- [ ] **Prinzip Entdeckbarkeit:** Jedes herstellbare oder umwandelbare Item soll in erster Linie intuitiv sein und zusätzlich im Spiel gehintet werden (z. B. Tiegel in Dörfern mit erloschenem Lagerfeuer zeigt die Nutzung). Crafting/Umwandlungen bisher nirgends gehintet → Konzept erarbeiten (Ideen: Bücher in Struktur-Truhen, Bilderrahmen/Gemälde mit Rezept, Dorfbewohner-Werkstätten als Vorführung, Fortschritts-Hinweise, Guide-Seiten). Erst Konzept vorlegen.

## Nachtrag 22 (2026-10-07, Besitzer)
- [x] **Kreativ-Tabs wieder normal, aber sauber:** Kreativ-Abstandshalter (Spacer/Lücken) entfernen; die Spacer-Logik im Code behalten (abschaltbar, z. B. Konstante/Config), falls sie später wieder gebraucht wird. Ersetzt N11 P8 (eine Lücke zwischen Kategorien). Datenintegritätstests der Tabs entsprechend. (Audit 09.10.: ff143698a (CreativeTabLayout.SPACERS_ENABLED))
- [ ] **Kreativ-Tab-Struktur:** jede Super-Mod hat einen eigenen Tab; die einzelnen Items werden zusätzlich in die passenden Vanilla-Tabs einsortiert. Config (simplelib-Gerüst, je Mod): „in Vanilla-Tabs einsortieren“ an/aus – aus = Vanilla-Tabs bleiben unverändert (Stock). (teilweise: Schalter „in Vanilla-Tabs einsortieren“ nur in SB (ff143698a); eigener Tab je Super-Mod und simplelib-Gerüst fehlen)

## Nachtrag 23 (2026-10-07 nachts, Besitzer)
- [ ] **Mob „Shellker“** (End, Gateway-Wächter; Konzept in docs/ai/KONZEPT-MOBS-2026-10-07.md ergänzen, dann Vorschau): Shulker-Variante mit harter Schale; 1×1×1, öffnet zum Schießen alle Seiten, wird dabei NICHT größer (schießt aus 1×1-Loch). Geschlossen kein Schaden (wie Gürteltier), offen normal. Leben 4× Shulker. Teleportiert nie; nur per (Klebe-)Kolben verschiebbar. Je End-Gateway genau 4 Stück rund um die Öffnung (Zugang erst nach Besiegen/Wegschieben). Projektile wie Shulker, 1,5× schneller, Effekt **Schwerkraft (High Gravity)** statt Schweben: alle Aufwärtskräfte (Sprung, Levitation, Elytra-Auftrieb) auf 10 %, Elytra gleitet 10× schlechter. Schwerkraft auch als Trank: Brauzutat **Shellker-Schale**. Vermehrung wie Shulker (Shulker trifft Shulker-Kugel). Erschaffen: Shulker Schale „anziehen“ (Rechtsklick) → wird Shellker. Drop Ø 2,5 Schalen; Farmen nur über Umwandlung von Shulkern.
- [ ] **End-Struktur Brunnen:** wie der Ausgangsportal-Brunnen (Drachenei-Sockel) der Hauptinsel aus Endsteinziegeln/Endstein; 1 intakte Grundvariante + 2 kaputte Varianten.
- [ ] **End-Struktur Fake-Gateway:** aus Endsteinziegeln, gleiche Generierungsregeln/Form wie echtes Gateway, aber verstreut, natürlich wirkend; ohne Funktion.
- [ ] **End-Schiffswracks:** selten, Varianten analog Vanilla-Shipwrecks (Bug/Heck/kaputt/gekippt); 1–2 Kisten mit ca. 30 % des End-Schiff-Loots; 50 % Rahmen, davon 10 % (absolut) mit kaputter Elytra im Rahmen; sonst ohne Rahmen.
- [ ] **End-Pfad:** Formen wie Dorf-Erdpfade, aus Endsteinziegeln (Anspielung End-Dorf).
- [x] **Verzauberung umbenennen:** `enchantment.simplefun.no_damage` „Damageless“ (DE „Schadlos“, Wiki/Guides mitgezogen; ID unverändert).
- [ ] **Furcht-/Zitter-Mob:** Vorschläge im Konzept docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md (Abschnitt „Furcht-Mob Vorschläge“) – Besitzer-Entscheidung offen.
- [x] **Magnete, Konflikt mehrerer Magnete:** liegt ein Item im Bereich mehrerer Magnete, Zielpunkt = Mittelpunkt (Schwerpunkt) aller beteiligten Magnete (1,2,3,…), dort Anziehung mit toter Zone. Dazu Sweetspot für fallende Items: Item tariert sich auf richtiger Höhe aus und bleibt stehen (z. B. Magnet darüber), kein Zittern/Buggen. (Audit 09.10.: 0b238b6ba)
- [x] (claude-q-blueprint) **Blaupause am Boden:** Rechtsklick auf liegende Blaupause, wenn keine andere Aktion greift → Blaupausen-UI öffnen und bearbeiten.
- [x] (claude-q-blueprint) **Blaupausen-UI Umbau** (Stil-Guide beachten, Stil ungefähr gleich):
  - 3D-Vorschau: Drag-Rotation reparieren; Strg+Drag verschiebt; Icon-Button „Ansicht zurücksetzen“ im Vorschaufenster.
  - Layout: Überschrift, dann 3 Bereiche. (1) **Materials:** „X×Y×Z“ Zeilenumbruch „= n Blöcke“ ohne Überlappung; darunter Stab-Icon + Fortschrittsbalken; Materialliste schmaler (nur Icon + Zahl). (2) **Code:** Codeblock, darunter „Code ok“ Zeilenumbruch „xyz/XYZ Zeichen“; rechtsbündig Buch-Button (Hilfe) – Zeichenzeile wandert dafür unter den Codeblock. (3) **Preview:** breiter, mit Reset-Icon; darunter Signieren und Fertig.
  - Insert-Feld wandert in das Hilfe-Buch: Tabs tauschen, „Blocks“ zuerst und Standard; im Blocks-Tab statt normaler Suche Textfeld + Insert-Button daneben; beim Öffnen des Buchs ist Insert vorausgewählt. Tab „Guide“: vollständige, leicht verständliche Erklärung, ganz unten Copy-Button (Text kopieren, z. B. für KI-Fragen).
- [ ] **Simple Models überarbeiten:** Modul insgesamt verbessern (Audit + Vorschläge zuerst). Models-Button im Stil der Guide-Lesezeichen, immer an der Inventar-UI über den Rüstungsslots angehängt, Icon statt Text (Rüstungsständer oder Namensschild, ggf. Besseres).
- [x] **Resonanz-Statusfeld:** rechts neben das Buch-Icon verlagern; statt Vorlage ein Herz-Symbol in Steinfarben, Resonanzwert grau daneben, schmalerer Rahmen → kompakter, vanilla-näher. (Audit 09.10.: 916e4d203, claude-brush2 29771970c; Feinschliff N29 offen)
- [x] (claude-q-hopper) **Truhen:** Fallen-Truhen ohne „Trapped“ im GUI-Titel; generell keine Stapelgröße o. ä. in Truhen-/Container-GUIs; Mod-Fallen-Truhen-Texturen viel zu auffällig → dezenter wie Vanilla (Vorschau).
- [x] (claude-q-hopper; Lore-Trichter offen) **Trichter:** im GUI statt Text „Filter“: Lücke ca. 1 Slot breiter zwischen den 5 Trichterslots, darin Filter-Icon + Doppelpunkt; Gesamtblock mittig (nach links verschieben). Fehlende Lore-Trichter der Mod-Trichter ergänzen. Rezept verstärkter Trichter: Trichter + gesprungener Diamant + Namensschild.

## Nachtrag 24 (2026-10-08 abends, Besitzer)
- [ ] Dunkelheits-Trank aus neuem Warden-Drop (Warden-Item als Brauzutat).
- [ ] Schnelleres Redstone (Astralit).
- [ ] Schnellere Elytra (Astralit).
- [ ] Shellker-Textur wie Grundgestein (Bedrock-Tarnung).
- [ ] Sculk-Kiefer (Falle): lautlos, verhält sich wie Spinnennetz, greift mit Fangzähnen an.
- [ ] Froschlichter in zusätzlichen Farben.
- [ ] Ziegenhorn platzierbar; Fackeln oder stabartige Items hineinstecken.
- [ ] Schildkröten-Helm-Äquivalent aus Shellker-Schale.
- [x] Trainingspuppe mit Spielernamen umbenennen → Skin wechselt. (claude-q-stands; Client-Sicht mit echtem Profil offen)
- [ ] Glitzernde Melone essbar; Melone auch als 0,125er-Block (platzierte Melonenscheibe = 0,125er-Block).
- [x] Farbpinsel? (Idee, offen). (Audit 09.10.: claude-brush3 fa6aa3cf6 (Goldpinsel, Farbkasten 4 Stufen); Textur N28/N29 offen)
- [x] Rüstungsständer per Redstone wie Item-Displays (Schleich-Rechtsklick?): tauscht die ganze Rüstung mit den Rüstungsslots des Spielers. (claude-q-stands: Schleich-Rechtsklick leere Hand oder Rechtsklick auf bestromten Ständer)
- [x] Rüstungsständer sollen Arme haben. (claude-q-stands, Config server.features.armorStandArms)
- [x] Weitere Rüstungsständer: mittel (Pferderüstung, oder zwei Rüstungsteile oben/unten), klein (nur ein Teil, z. B. Stiefel oder Nautilus-Rüstung). (claude-q-stands: mittel = Hose+Stiefel, klein = Stiefel; Pferde-/Nautilus-Rüstung offen)
- [ ] Speer im Spender: bei Aktivierung wie Stachelfalle.
- [ ] Simple Respawn (neues Modul?): beim Tod niedergeschlagen, Mitspieler kann wiederbeleben; danach 3 Herzen und 0 Hunger.
- [ ] Barren als 3D-Modell platzierbar.
- [ ] Schwefelwürfel (Sulfur Cubes) befüllbar mit allen Ofen-Varianten, Tischen usw. (Easter Egg); dann schwer wie Eisen (schwer zu verschieben).
- [ ] Übelkeits-Trank.
- [ ] Holz auch als 0,125er-Blöcke (falls noch nicht).
- [ ] Mob „Seelenfeuer-Lohe“ (Soulfire Blaze).
- [ ] Trims bis zu 4 platzierbar; alle anderen so platzierbaren Items ebenfalls bis 4 Stück („Plex“).
- [ ] Hammer: Normal-Rechtsklick-Halten (Block → Treppe → Stufe) deaktivieren; nur Schleich-Rechtsklick-Halten transformiert (Teile werden abgebaut). Erreicht die Form eine nicht unterstützte Gestalt (z. B. oben und unten je ein Achtel entfernt), wird sie in 0,125er-Blöcke zerlegt. Hammer wackelt (Hinweis „transformierbar“) nur, wenn transformiert werden kann – also nur beim Schleichen.
- Konzept Farbpinsel/Respawn: docs/ai/KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md. Farbpinsel in Arbeit (v3: Goldpinsel, Farbkasten mit Stufen).

**Besitzer-Antworten 09.10. (auf Respawn-Fragen bezogen):** 1 ja (Einzelspieler = normaler Tod), 2 Timer: Besitzer hat eigenes älteres Revive-Konzept, sucht Daten raus → Respawn-Modul WARTET darauf, 3 ja (Mobs ignorieren Niedergeschlagene), 4 nein (keine Item-Wiederbelebung vorerst). Farbpinsel-Fragen unbeantwortet → Besitzer: KEINE Holz-Beize, Kreativ unbegrenzt, Pinselstrich-Partikel/Klang ja.

## Nachtrag 25 (2026-10-09, Besitzer)
- [ ] **Crates/Körbe** (Art Komposter/Kessel): Items hineinlegen → eigene Füll-Texturen sichtbar; voll = Aufbewahrung, insgesamt 8 Stacks einlagerbar. Start mit allen Essens-Items.
- [ ] **Ausgehöhlte Stämme** (auch entrindete Variante): Spieler und kleine Mobs können hindurchkriechen. Craftbar zu Platten/Brettern („sheets/plates“) je Holzart in Normal- und entrindeter Variante; diese verhalten sich wie Eisengitter/Glas(scheibe) und lassen Licht durch. Ausgehöhlter Stamm + Holzplatte → Holz-Kessel: wie normaler Kessel, aber brennbar; Lava kann ihn entzünden (verbrennt → Lava wird frei).
- [ ] **Gemeißeltes Packeis, gemeißeltes Blaueis, rissiges Eis** (rissiges Eis wird nach ein paar Sekunden Draufstehen zu Wasser).
- [ ] **Töpferscherben-Meißel:** Scherbe in der Nebenhand + Meißel in der Haupthand auf entrindeten Stamm → gemeißeltes Holz mit dem Motiv der Scherbe (jede Scherbe eigenes Motiv, Overlay in dunklerer Holzfarbe).
- [ ] **Nautilusschalen-Block.**
- [ ] **Stufen aus Erde und Gras**, ebenso Sand und Kies.

**Simple Maps Antworten 09.10. (in docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md unter „Antworten Besitzer“ ergänzen):** Feature 1 ja (Wegpunkte beim Kopieren), Feature 2 entfällt (Todespunkt nur über Feature 7: Bergungskompass in der Hand), Feature 6 = F8 (eigene Karte je Dimension), Feature 7 ja, Feature 8 ja; Feature 4 (Karte im Rahmen zeigt Umgebung) erklärt, Antwort offen (Empfehlung weglassen). WICHTIG: Besitzer-Antworten stehen meist schon in docs/ai/PLAN-*.md „Antworten Besitzer“ – vor Rückfragen dort nachlesen!
- Simple Maps Feature 4 (Besitzer 09.10.): Karte im Gegenstandsrahmen → Rechtsklick öffnet die Karten-UI; dort scrollbar; der Rahmen zeigt danach den Ausschnitt, zu dem man gescrollt hat.
- Tiegel-Flammen: Besitzer will 4. Vorschlag „Mittel“ (50 % zwischen spitz und rund, kein Hybrid) → cp-previews (p-flame3).

## Nachtrag 26 (2026-10-09, Besitzer)
- [x] (claude-q-hopper) **Autonomer Crafter** (abgewandelter Vanilla-Crafter): craftet automatisch das vorgegebene Rezept, solange ein Trichter darunter liegt; ohne Trichter darunter oder bei Redstone-Signal craftet er nicht. UI: 3x3-Grid, gleiche abschaltbaren Slots wie der Crafter, Filter-Knopf wie beim Mod-Trichter (Modi: exakt / gleiche Art; dieselben Knopf-Texturen). Takt wie Crafter, ca. 4 Ticks Abklingzeit.
- [x] (claude-q-hopper) **Filter-Prinzip umsetzen** bei Mod-Trichtern und autonomem Crafter: im Filtermodus bleibt immer ein echtes Item fest im Slot (statt Schatten-Item); erst ab dem 2. wird verarbeitet/transportiert. Inklusive Filter-Knopf. (Prinzip: docs/ai/PRINZIPIEN-FILTER.md.)
- [ ] **Konzept stärkerer Wither**: droppt ein Item, das später für ein Biom-Werkzeug dient („Biom-Pinsel“: Pinsel in der Haupthand, biomspezifisches Material in der Nebenhand; Haltbarkeit, verzauberbar). Erst Konzept vorlegen.
- [ ] **Werkbank mit Lager** (verbesserte Werkbank): wie Werkbank, aber Items bleiben beim Schließen im 3x3-Feld liegen und werden auf dem Block angezeigt.

## Weitere Besitzer-Entscheidungen 09.10. (noch in Queue/Roadmap übernehmen)
- [x] Tiegel-UI: Fass-Titel weglassen (N14). (Audit 09.10.: Fass-Kasten ohne Titel (N12c 1b50ab578))
- [ ] Config-Migration: alte Optionsnamen einmalig beim ersten Start übernehmen (ja).
- [x] Keramik-Eimer 4× Ausgießen (in Arbeit, Review-Agent). (Audit 09.10.: cf841a41a)
- [x] Furcht-Mob: später. (Audit 09.10.: entschieden; Umsetzung siehe N20/N23)
- [x] Tiegel-Flammenform: Besitzer wählt nach Vorschlag „Mittel“ (spitz/mittel/breit). (Audit 09.10.: „Mittel“ 5037a180a)
- [x] Hufeisen-Reiter: muss gut aussehen (Runde 2 simplecontainers). (Audit 09.10.: cp-scfix Runde 2 20d8b3376; Besitzer-Abnahme offen)

## Nachtrag 27 (2026-10-09, Besitzer)
- [x] **Befiederungstisch** wie die Werkbank in einer Zeile: Rezeptbuch links, dann Feder, Stock, Spitze, Pfeil, Ergebnis.
- [ ] **Astral-Verzauberungstisch** (Konzept docs/ai/KONZEPT-ASTRAL-VERZAUBERUNG-2026-10-09.md):
  - Herstellung: Enderit-Nugget in der Nebenhand, mindestens Netherit-Hammer in der Haupthand, ca. 20 Schläge auf einen Verzauberungstisch.
  - UI nach den Prinzipien des normalen Tisches, aber: die 3 vorgeschlagenen Verzauberungen sind zufällig (passend zum Item, z. B. Spitzhacke: Haltbarkeit, Effizienz, Glück). Jede hat einen Regler (Stufe 0..max).
  - Regler-Grenzen je Bücherregal-Stärke: ohne Regale nur Stufe 1 der niedrigen Verzauberungen, hohe ausgegraut (0). Volle Regale: Regler zusammen bis 30 Level wählbar, Kosten skalieren mit der Wahl (0–3 Level verbraucht).
  - Höhere Stufen: bessere Regale + besonderer Boden 5×5 aus Lohen-Obsidian (leuchtende Variante des weinenden Obsidians, 8 Lohenstaub + 1 weinender Obsidian; später Astralit-/Nihilit-Fliesen): Zwischenstufe 40 (4 Level Verbrauch), Maximum 50 (alle gewählten Verzauberungen bis Max, 5 Level Verbrauch).
  - Abbauen dauert doppelt so lange wie beim normalen Tisch; Drop: normaler Tisch + eingesetztes Enderit-Teil.
  - Lagert Lapislazuli und Lohenstaub (je bis 1 Stack), bleibt beim Verlassen erhalten. Lohenstaub wird doppelt so viel benötigt wie Lapis.
- [ ] **Lohenholz** (Blazewood): 8 Lohenstaub + 1 Nether-Holz (Varianten je Nether-Holzart: Karmesin, Wirr, …).
- [ ] **Lohenbuch** (Blaze Book): analog zum Buch aus Lohen-Zutaten.
- [ ] **Lohen-Bücherregal** (Varianten je Lohenholz): doppelter Verzauberungswert eines normalen Regals; ermöglicht die höheren Astral-Stufen.

## Nachtrag 28 (2026-10-09, Besitzer)
- [x] Farbkasten (paint_box, 4 Stufen) so übernommen.
- [ ] **Farbkasten-Textur überarbeiten** (Besitzer: Funktion ok, Textur soll später neu).
- [x] Enderit-Eimer Variante A: Voll-Wasser bedeckt den Rand und läuft wie Lava über.
- [x] (→ `claude-q-ebucket`) Enderit-Eimer halb-Texturen (Wasser/Lava/Seelenlava: wie voll, nur die zwei obersten äußeren Flüssigkeitspixel zeigen den Eimer) mit der Kapazität „2 Eimer“ aus Nachtrag 21 einbauen.
- [x] Seelenlava Textur + Animation neu, fließende Seelenlava vor- und rückwärts (in Arbeit, claude-soullava). (Audit 09.10.: claude-soullava 5037a180a; Rückwärtslauf siehe N29)
- [x] Platzierte Bündel von oben leicht abgerundet (in Arbeit, claude-soullava). (Audit 09.10.: claude-soullava 5037a180a)
- [x] Tiegel-Flammen Form „Mittel“ (in Arbeit, claude-soullava). (Audit 09.10.: claude-soullava 5037a180a)

## Nachtrag 29 (2026-10-09 nachmittags, Besitzer; Referenzbilder Ständer: /root/previews/refs-stands/ 2–4)
- [ ] Kreativ-Blaupause und Kreativ-Bauzauberstab: unbegrenzte Reichweite; Kreativ-Blaupause nach dem Signieren nicht mehr bearbeitbar.
- [ ] Blaupausen-UI: im Hilfe-Bereich Reiter „Blocks“/„Guide“ als Icons, „Text kopieren“ ebenfalls als Icon.
- [ ] Enderit-Eimer voll: bis zum Rand gefüllt, KEINE Tropfen; wie beim Axolotl-Eimer. Halber Lava-/Seelenlava-Eimer nach derselben Regel wie der halbe Wassereimer.
- [ ] Stein-Herz wieder 1 px schmaler (9×9); bei maximaler Resonanz ein Diamant-Herz.
- [ ] Rüstungsständer: mittleren Ständer entfernen; nur noch der kleine Ständer (Pfosten mit Querholz auf Steinplatte, wie Referenz) für genau ein Item: ein Rüstungsteil oder eine Tier-Rüstung (Pferd/Wolf/Nautilus, Referenz Pferderüstung auf Pfosten).
- [x] Tiegel-Flamme „Mittel“ (bestätigt).
- [ ] Farbkasten-Textur überarbeiten: jede Farbe der Palette als ein Pixel in der Kasten-Textur.
- [ ] Fließende Seelenlava läuft immer noch vor und zurück → nur in eine Richtung fließen (wie Vanilla-Lava).
