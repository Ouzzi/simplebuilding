# Warteschlange SimpleBuilding (Stand 2026-10-12, Hauptlinie 26.3)

Regeln: AGENTS.md; aktueller Bestand/Belege in docs/HANDOFF.md. Worker committen auf ihrem Branch ohne Push/Merge.
Ein erledigter Codepunkt ersetzt weder Client-Gate noch Besitzer-Abnahme.
Audit 2026-10-09 (claude-q-audit): offene Punkte gegen claude-wave1 9c7929ff4 geprüft; Restliste: docs/ai/ROADMAP-2026-10-09.md.
Queue-Pflege (Besitzer 12.10.): Neue Nachträge OBEN anhängen (direkt unter „Offen – Überblick“), Erledigtes per Haken
`[x]` + Branch/Commit abhaken (und die Zeile im Überblick streichen), neue offene Punkte zugleich in den Überblick
eintragen. Abschnitte ohne offene Punkte wandern monatlich unverändert nach `.claude/QUEUE-ARCHIV.md`
(Reihenfolge erhalten; letzte Archivierung 2026-10-12).

## Offen – Überblick (Stand 2026-10-12)

Jeder offene Punkt steht hier genau einmal (Ursprungs-Nachtrag in Klammern). Quelle der Wahrheit bleibt der Haken im
Ursprungsabschnitt weiter unten; beim Erledigen dort `[x]` setzen und die Zeile hier streichen.

### (A) In Arbeit (Branch ungemerged)
- Deceiver-Umhang: Kopf-Umhang mit Kapuze als 3D-Modell auf dem Rücken, Elytra-Zusammenspiel, Deceiver-Mob trägt ihn (N49; claude-deceiver3)
- Deceiver-Umhang am Kopf-Slot, Verkleidung überall sichtbar (Inventar, Ego-Hände), Schlag in die Luft hebt Tarnung auf (N48; claude-deceiver3, Teil auf dem Branch)
- Trick-Spiegel ausbalancieren oder vorerst entfernen (N48; claude-deceiver3)
- Shellker-Schale: Item-Textur wie Vanilla-shulker_shell, angepasst (N48; claude-deceiver3)
- Furcht-Mobs F-A „Dread“ (Golem-Mechanik) und F-C „Hollow Watcher“ (End) mit Zittern-Aura (N47, Dubletten N20/N21/N32; claude-fear-mobs)
- Aschholz/Dämmerholz: alle Holz-Features optional bei geladenem SB (N47; claude-endwood-full, auf dem Branch abgehakt, Merge offen)
- Info ohne Queue-Punkt: claude-vd-windows (generateVanillaDerived Windows-fest, Memory 12.10.) wartet nur auf Merge nach claude-wave1

### (B) Feststehend – als Nächstes bauen (Reihenfolge = Priorität)
1. Prinzip Entdeckbarkeit umsetzen, Stufe 1: Tooltip/Rezept-Freischaltung je Item + Test (N47; Konzept PRINZIPIEN-ENTDECKBARKEIT.md)
2. Seelen-Lohe schießt Seelenfeuer-Kugeln (Soulfire Charge, Seelenbrand-Effekt) (N49)
3. Geschnitztes Holz: Item-Modell an Block-Modell angleichen (N49)
4. Shellker-Helm und Schildkrötenpanzer trimmbar (N49)
5. Inventar-Helligkeit als Config: Light, Dark, Dawn (N49)
6. Extra-Kopf-Slot im B-Menü (nur Mob-Köpfe/Kürbis, Effekte, Helm-Position je Kopf) (N49)
7. Resonanz-Button zwischen Kopf- und Nebenhand-Slot, Herz-Stufen, Tooltip-Format (N49; hängt an 6)
8. ✔ Wayfinder-Karte: Zoom feiner, Navigation verbessern (N48)
9. ✔ Octet-Blöcke untereinander kombinierbar (N48; `claude-octets-mix`, Plan docs/ai/PLAN-OCTETS-MIX-2026-10-13.md)
10. ✔ Simple Creatures umbenennen in „Simple Mob Manners“, Id simplemobmanners, Namespace-/Config-Migration (N42; `claude-mobmanners`)
11. Stärkerer Wither + Biom-Pinsel (N47; Konzept KONZEPT-WITHER-BIOMPINSEL-2026-10-10.md)
13. ✔ Simple Trims Stufen 2–4 (Vorlagen, platzierbar, Axt ohne SB / Hammer mit SB) (N18; `claude-trims2`, Besatz-Boni bleiben vorerst in SB)
14. Stil-Audit: andere Mods an SimpleBuilding-Stil angleichen (N11, Dubletten N11/N14; Teil: Mod-UIs im Container-Stil)

### (C) Modulaufteilung (Plan docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md; Stufen 1a–3b erledigt: claude-split1/2/3)
Eigener Abschnitt „Modulaufteilung“ weiter unten (Stufen 3c, 3d, 4 Ovens, 5 Crucibles, 6 Octets, 7 Hammers, 8 Storage, 9 Release; strikt nacheinander).

### (D) Wartet auf Besitzer (Abnahme im Spiel / Entscheidung)
- Abnahme im Spiel (Client, Testzentrale in der Besitzerwelt neu bauen): Pulsating-Trim, Astralgewölbe, Enderit-Pulver, Mega-Handbücher, Hammer-Tempo (Formel 2/3 oder 3/4?), Attractor/Echolot, Boost/Pads, Architectury-Mixin-Fix (Welle 25; Dubletten N-alt)
- Abnahme neue Module im Spiel: Models, Fun, Visuals/Sounds, QoL, Riding, Dimensions, Money, Forge-Clients, Mehrspieler (Welle 25)
- Weitere Sichtabnahme Buch-Screen, Truhen, Kern-Animation, Gadgets; Octant-Manager-Kontrast (Offen-Liste)
- Abnahme Zeilen-Layout in allen Kreativreitern (Offen-Liste)
- Abnahme visuelles Hub-Rendering/echte Clients, lokale Modpack-Kombinationen (Offen-Liste)
- Desktop-/Handy-Sichtprüfung von Wiki und Balancing-Zentrale (Offen-Liste)
- Forge: Clientdarstellung, Config-Persistenz, Default-Einschaltung abnehmen (Offen-Liste)
- Abnahme Simple Money; NeoForge-Client/alte Spielwelt prüfen (Offen-Liste)
- Elytra-Reparatur im Schmiedetisch: soll das mit eigener Rezept-Zutat je Loader gebaut werden? (N11)
- Sprach-Bridge einrichten (Tailscale, Anbieter/Modell, Headset; Morgenbericht gewünscht?) (Welle 25)
- Modulaufteilung: Fragen F1 Tiegel-Kern, F2 Tweaks-Port, F3 Grenzfälle (Plan §6; Empfehlungen gelten ohne Gegenwort)
- Weiter offen als Abnahme (Branches gemergt, Details in Memory uebergabe-2026-10-08.md): UI-Konzepte (claude-ui-concepts), Respawn, Soulbound/Death Vault, Claims, Howler, Shellker, Nibblemite, Texturen N44–N46

### (E) Bewusst später
- Port-Run 26.2/1.21.11/26.4 erst nach Besitzer-Abnahme (Offen-Liste, Dubletten Welle 25/N32)
- Port-Run 26.2: Erzdetektor-Texturen auf zentrierte 26.3-Nadel bringen (Nachtrag 2)
- 26.4: Forge einschalten, Cloth-Config, NeoForge-26.4-Linie, sobald Builds da sind (Offen-Liste)
- Mega-Handbücher ansehen und Testzentrale neu bauen, erst wenn alle Mods fertig sind (Offen-Liste, Dubletten N47)
- Wiki-Extras: Beschaffungszeit je Item mit Zeitaltern; Diagramm „Zeit bis k Stück“ (log. Achse); weitere Wiki-UX-Ideen (Offen-Liste)
- Punkte 64–69: Sprachen, Attractor mit Ladung, neue Blöcke, Baustab über Planer, Kern-Module, Multi-Mod-Repo (Offen-Liste)
- Rucksack-Sortierung, sobald eine Sortierfunktion kommt (Offen-Liste)

---

## Modulaufteilung (Plan docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md §4; Stufen 1a–3b erledigt)
Jede Stufe eigener Branch von claude-wave1, einzeln gate-grün; ab Stufe 3 strikt nacheinander.
- [x] 3c `claude-split-s3-apis`: SimpleLib-Dienste in_world_upgrades, `#simplelib:upgrade_tools`, isHammer, OctetCells, registerUpgradeHandler, TestCentreStations, GuideChapters (claude-split-s3-apis; offen: Lib-eigener Reload/Sync in Stufe 4, GuideChapters-Leser in Stufe 9)
- [x] 3d `claude-split-s3-materials`: gemeinsame Materialien nach simplelib (§2.5), requestMaterial, Condition material_requested (claude-split-s3-materials; offen: Loot-Einspeisung + Lib-Reload in Stufe 4)
- [ ] 4 `claude-split-s4-ovens`: Extraktion Ovens nach modules/simpleovens
- [ ] 5 `claude-split-s5-crucibles`: Crucibles nach simplecrucibles (Aliase, Seelenlava)
- [ ] 6 `claude-split-s6-octets`: Octets nach Modulpfad (106 Blöcke, 377 Rezepte)
- [ ] 7 `claude-split-s7-hammers`: Hammers extrahieren (breiteste Kopplung)
- [ ] 8 `claude-split-s8-storage`: Storage (~93 Klassen, 36 Blöcke) extrahieren
- [ ] 9 `claude-split-s9-release`: Aufräumen, Wiki-Umzug, Versionen, Changelog, Brücken-Version, volles Gate

---

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
- [x] Testzentrale in der Besitzerwelt neu bauen; automatische GameTest-Welten ersetzen keine Abnahme. (Audit 12.10. Dublette, zusammengeführt mit Zeile 96 / Überblick: (Testzentrale neu bauen))
- [x] Resonanzstab auf 26.3: Amethystscherben als Amboss-Reparaturmaterial-Tag, Tooltip EN/DE und Wiki; Rotator bleibt ohne Mending. Worker `gpt-answers`, Plan `docs/ai/PLAN-BESITZER-ANTWORTEN-2026-10-02.md`.
- [x] Rueckfragen Besitzer: alte Excavator/Diamond-Ingots-Texte waren bereits korrigiert; Cover blieb nach der damaligen Besitzerentscheidung im Loot; die neue Option B vom 2026-10-05 ist unten umgesetzt. Belege und Optionen: `docs/ai/RUECKFRAGEN-ERKLAERT-2026-10-05.md`.
- [x] Liste G (58 Punkte): Run G der Welle 22 belegt, Original der nummerierten Liste in den recherchierten Quellen nicht auffindbar; keine erfundene Rekonstruktion. Siehe Erklaerungsdokument. (aussortiert 10.10.: Originalliste nicht auffindbar, Relevanz nicht prüfbar – Besitzer kann neu diktieren)
- [x] 12 zusaetzliche Config-Ideen (Run D): Originalliste fehlt weiterhin. Sechs belegte bestehende Run-D-Zahlenoptionen auf 26.3 hart begrenzt, EN/DE, ConfigOptionTests und Wiki-Metadaten ergaenzt; der vollstaendige Zwoelfer-Abgleich bleibt offen. (teilweise: gpt-coverconf 79da90c66 hat zwölf neue Ideen bewertet, vier begrenzt umgesetzt; Original-Zwölferliste fehlt weiter) (aussortiert 10.10.: Originalliste nicht auffindbar, Relevanz nicht prüfbar – Besitzer kann neu diktieren)
- [x] Kern-Vorschlaege abgeglichen: neuere Besitzerentscheidung verbietet Steinmetz-Kerne; zweite Eisenquelle Mine (0,5 %) besteht bereits. Enderit auf 26.3 jetzt 0,5 %, alte Linien unveraendert; Details `docs/KERNE-SELTENHEIT.md`.
- [x] Balancing-Zentrale schreibt unterstützte Java-/JSON-Werte in die Modquellen, einschließlich Vorschau, Konfliktprüfung und Rollback (sbdev/service.py, tests/test_phase2.py). Nicht zugeordnete Werte bleiben ausdrücklich Planwerte; kein automatischer Live-Reload kompilierten Java-Codes.
- [ ] Beschaffungszeit je Item: Zeit bis zum 1. (und k.) Stueck je Quelle, gezielt vs. normales Spiel, mit Zeitalter-Einordnung ("vor Braustand & Traenke") auf Item-Seiten und in der Beschaffungs-Uebersicht (Modell: tools/devserver/sbdev/model.py) (teilweise: Kistenöffnungs-Teilmenge da; Zeit-/Zeitaltermodell fehlt) (Relevanz geprüft 10.10.: weiterhin sinnvolles Wiki-Feature, bleibt offen)
  - [x] Belegbare Teilmenge 2026-10-03: erwartete Kistenöffnungen bis zum ersten/sechsten Stück für eindeutige Bernoulli-Quellen; keine erfundenen Stunden. Zeit-/Szenariokonzept: `docs/ai/PLAN-WIKI-BESCHAFFUNGSZEIT-2026-10-03.md`.
- [ ] Diagramm "Zeit bis k Stueck" mit logarithmischer Zeitachse und Zeitalter-Linien auf Item-Seiten (Relevanz geprüft 10.10.: weiterhin sinnvolles Wiki-Feature, bleibt offen)
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
- [ ] (ERST AM ENDE, nur auf ausdrückliche Anforderung des Besitzers – 13.10.) Port-Run 26.2/1.21.11/26.4 erst nach Besitzer-Abnahme; Faktenpass und aktuelles 26.3-Gate siehe HANDOFF.
- [ ] Besitzer-Abnahme des bereits implementierten Zeilen-Layouts in allen Kreativreitern.
- [ ] (ERST AM ENDE, nur auf ausdrückliche Anforderung des Besitzers – 13.10.) 26.4: Forge einschalten sobald Build da (-Pmc264_forge_version), Cloth-Config-Screen/Dev-Mods sobald 26.4-Builds da, NeoForge-26.4-Linie
- [x] Baustab V1: normale Flaechen ueber den Blaupausen-Planer (Schutzpruefung pro Position) - ca. 40 Tests pinnen das heutige Verhalten
- [x] Kerne als Baustab-Module + eigene Funktionen (Vorschlaege in docs/BAUWERKZEUGE-INTERAKTIONEN.md) - Besitzer: erst spaeter (Audit 12.10. erledigt: Besitzer 12.10. (N47): Kerne bleiben reine Crafting-Zutat, keine Zusatzfunktionen)
- [x] Kerne: Netherstern nur ab Diamant, Netherit-/Enderit-Baustab aus Kern, goldener Baustab mehr Haltbarkeit - nicht gewaehlt, spaeter neu besprechen (Audit 12.10. erledigt: Besitzer 12.10. (N47): Kerne bleiben reine Crafting-Zutat, keine Zusatzfunktionen)
- [ ] Rucksack-Sortierung (Reihenfolge vorbereitet), sobald eine Sortierfunktion kommt
- [ ] (SPÄTER, erst wenn die Mods fertig sind – Besitzer 12.10.) Besitzer-Abnahme: Mega-Handbuecher im Client ansehen und Testzentrale in der Besitzerwelt neu bauen.
- [ ] Besitzer-Abnahme: visuelles Hub-Rendering/echte Clients, lokale Modpack-Kombinationen
- [x] Weitere Ports als Module: riding, qol, visuals, models, dimensions, tweaks, fun, sounds und Money-Links (siehe Welle 25 unten); Abnahmen im Spiel offen.
- [x] Forge 26.3 einschließlich simpledimensions integriert; experimentell/opt-in. Clientabnahme bleibt offen.
- [x] Besitzer-Abnahme: 26.3-Signalkanaele und Astralgewoelbe im Client; Testzentrale in Besitzerwelt neu bauen. (Audit 12.10. Dublette, zusammengeführt mit Zeile 96 / Überblick: (Astralgewölbe/Client; Signalkanäle durch N36 ersetzt))
- [ ] Desktop-/Handy-Sichtprüfung von Wiki und Balancing-Zentrale.
- [ ] (ERST AM ENDE, nur auf ausdrückliche Anforderung des Besitzers – 13.10.) Forge: echte Clientdarstellung, Config-Persistenz/optionale Integrationen und spätere Default-Einschaltung abnehmen.
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
- [x] (Besitzer 13.10.: gestrichen) Sprach-Bridge einrichten (docs/ai/VOICE-HANDS-FREE.md): Tailscale am Handy, `tailscale serve`, Anbieter und Modell waehlen, Headset testen; offen ob ein gesprochener Morgenbericht gewuenscht ist.
- [x] Forge für simpledimensions und Claim-Adapter implementiert. Native Forge-Dialoge ersetzen die inkompatible Cloth-Version; GUI-Abnahme bleibt offen.
- [x] Weitere Wiki-UX-Ideen (oben); Port-Run 26.2/1.21.11/26.4 erst nach Release-Ankuendigung des Besitzers. (Audit 12.10. Dublette, zusammengeführt mit Zeile 73 / Überblick: (Port-Run) und 58/60 (Wiki-Ideen))

## Besitzer 2026-10-02 (Nachtrag)

- [x] (claude-tabswiki, KeyboardInputMixin; Client-Abnahme offen) Geschwindigkeitsmesser: läuft im Autowalk auch im Inventar und in nicht pausierenden UIs (Chat usw.) weiter.
- [x] Shulkerkiste vorerst nicht verzauberbar machen. (claude-combat: Vanilla-Kiste aus constructors_touch_enchantable; Test über alle Shulkerkisten)
- [x] Erzdetektor: Kompassnadel wirkt nicht zentriert – Animation/Nadel-Frames prüfen und zentrieren. claude-texprop: Nabe 2 px, Drehpunkt auf der Ziffernblatt-Mitte x = 7,5, Frames 17–31 gespiegelt; Nadel breiter (4-verbunden wie der Bergungskompass, 2-px-Schweif); Auswahl-Schimmer läuft jetzt auf der Nadel statt am Slot-Rand (Modell ohne Nachschwingen); ruhende Nadel pulsiert sanft (mcmeta); senkrecht (12/6 Uhr) 2 px breit und damit mittig; `ore_detector_centred_2026_10_02.py`, previews/erzdetektor-vorher-nachher.png. Sichtabnahme im Client offen.
- [ ] (ERST AM ENDE, nur auf ausdrückliche Anforderung des Besitzers – 13.10.) Port-Run 26.2: Erzdetektor-Texturen in src/main (älteres Nadeldesign) auf die zentrierte, breitere 26.3-Nadel + Ruhepuls bringen, sonst läuft der Auswahl-Schimmer (`OreDetectorNeedlePath`, gemeinsamer Code) auf 26.2 neben der alten Nadel.
- [x] Resonanzstab: soll auch Entities anzünden bzw. scannen können. (claude-combat: Punkt/HUD treffen Lebewesen, Scannen = Leuchten, `server.laser.scanEntities/scanPlayers`; Client-Sichtabnahme offen)
- [x] (claude-tabswiki: Werkzeuge je Stufe, Zweitplatzierungen Kampf/Redstone, Hufeisen; neue Tabs SimpleCombat/SimpleFood; Sichtabnahme offen) Mod-Items zusätzlich an den richtigen Stellen in die Vanilla-Kreativtabs einsortieren; Simple-Building-Tabs weiter aufteilen (Werkzeuge, Waffen, Rüstung usw. wie in Vanilla).
- [x] Trank „Crafty Shulker“: Effekt – bei Treffer an eine sichere Stelle in der Nähe teleportieren; braubar mit Shulkerkopf, analog zu den anderen Tränken/Effekten. (claude-combat: nur 26.3, Seltsamer Trank + Shulkerkopf, Redstone/Schwarzpulver/Drachenatem; Icon-Abnahme offen: previews/crafty-shulker-vorschau.png)

## Nachtrag 11 (Besitzer 2026-10-06, Screenshots images/16–19)
- [x] Seelenbrand: Dauer verdoppeln; sichtbarer Statuseffekt (leichter Blau-/Dunkelfilter); auf Kaltem (Eis, Schnee, Wasser …) Schaden im halben Intervall. (Audit 09.10.: claude-oc-tweaks11 2e67933c2 (N11 P1))
- [x] Magnet: höhere Reichweite. (Audit 09.10.: 9d313e818 (N11 P2))
- [x] Vorschlaghammer + Besatz-Interaktion: Rechtsklick wie alle In-World-Umwandlungen; alle In-World-Umwandlungen auf Konsistenz prüfen. (Audit 09.10.: claude-hammer12 5db8a809a; Inventur docs/ai/INWORLD-UMWANDLUNGEN-2026-10-06.md),
- [x] Seelenbrand: Dauer verdoppeln; sichtbarer Statuseffekt (leichter Blau-/Dunkelfilter); auf Kaltem (Eis, Schnee, Wasser …) Schaden im halben Intervall. – 120 s, Filter `SoulBurnOverlay`, kalter Grund = Wurf alle 1,5 s (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Magnet: höhere Reichweite. – Basis 4, +2 je Reichweite, Deckel 9, platziert 8 (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Crucible-UI scannen und verbessern: v2 zentriert, Hitze/Feuer eingelassen + Tooltip, Fass-Platzhalter; Vorschau `previews/crucible-ui-v2-vorschau.png` (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [x] Crucible↔Kupfer-Fass-Verbindung (Risse je Schlag, Flansch+Rinne, 9 Felder/Rest droppt, Tiegel-UI, Abbau beider Seiten) (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen): Zerstörungs-Indikatoren beim Anbringen, neues verbundenes Modell, verbundenes Fass nur 9 Felder und öffnet die Crucible-UI; Abbau Fass → Inhalt droppt, Fass wird normal (analog Crucible).
- [x] (claude-q-small: Tiegel/Fässer/Kessel zusätzlich in Vanilla-Funktionsblöcke (Schalter simplelib) und simplebuilding:functional; Test creative_tab_routing) simplelib-Elemente (Kessel usw.) immer in die Kreativtabs der jeweiligen Mods verteilen.
- [x] Item-Texturen aus Screenshot überarbeiten (images/16); Speer nur Enderit-Glimmern; Kupfer-Eimer runder und mehr Kupfer statt Porzellan; neuer Keramik-Eimer (3 Ton → roh, brennen; 16 bzw. 32 Füllvorgänge, dann kaputt); Kupfer-Eimer höchster Oxidation nicht nutzbar. → `claude-tex7` (Plan `docs/ai/PLAN-TEX7-EIMER-KERNE-2026-10-06.md`, Generator `texture_round7_2026_10_06.py`); offen Besitzer-Abnahme/Client-Sicht.
- [x] Schachfiguren in Checker-Farben (Steinmetz): 1/8-Block (0,5³) im Sub-Raster platzierbar, wasserbindbar solange < 8/8; daraus Figuren craftbar; Checker-Stufen und -Platten; Schleichen+Rechtsklick ersetzt Figur (alte in die Hand) bzw. nimmt sie auf; je Figur 2D- (von oben lesbar) und 3D-Variante.
  Erledigt auf `claude-chess` (Plan docs/ai/PLAN-SCHACH-2026-10-06.md): 13 Farben (12 Checker + Quarz), ein Block `checker_octet` (Farbe + 8 Bits), ein Block `chess_pieces` (Block-Entity, 4 Figuren je Block auf den Checker-Feldern), 156 Figuren-Items, 24 Treppen/Stufen; Station `chess`. Vorschau previews/schach-vorschau.png. Offen: Client-Sicht (Renderer, GUI-Modelle).
- [x] Raw-Enderite-Scrap-Rezept: 4 Fragmente statt 3. (Audit 09.10.: 7f811a0a3 (N11 P3))
- [x] Kessel-In-World-Umwandlung in JEI (Kategorie `cauldron_world`); Bild 17 = verstärkter Kessel als flache Seitentextur → 2D-Item-Sprite; verstärktes Fass zusätzlich heller wie Truhe (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [ ] Andere Mods an SimpleBuilding-Stil angleichen (sauber, einheitlich). (teilweise: Mod-UIs im Container-Stil (simplecontainers G4 d5ec6b840); Stil-Audit der Module fehlt)
- [x] Verstärkter Kessel erbt vom Kessel (alle Funktionen inkl. Milch). (Audit 09.10.: Duplikat, erledigt claude-crucible4 (unten))
- [x] Milchkessel-JEI/Jade fixen (images/18: „Empty 1B“). (Audit 09.10.: Duplikat, erledigt claude-crucible4 (unten))
- [x] Netherit-Fass fehlt. (Audit 09.10.: Duplikat, erledigt claude-crucible4 (unten)),
- [x] Raw-Enderite-Scrap-Rezept: 4 Fragmente statt 3. – formlos 4 Rohenderit; Leitfaden-/Erfolgstexte nachgezogen (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Kessel-In-World-Umwandlung in JEI (Kategorie `cauldron_world`); Bild 17 = verstärkter Kessel als flache Seitentextur → 2D-Item-Sprite; verstärktes Fass zusätzlich heller wie Truhe (Branch `claude-crucible4`, Plan `docs/ai/PLAN-CRUCIBLE-N11-2026-10-06.md`; Client-Abnahme offen).
- [x] Andere Mods an SimpleBuilding-Stil angleichen (sauber, einheitlich). (Audit 12.10. Dublette, zusammengeführt mit Zeile 396 / Überblick: (Stil-Angleichung))
- [x] Verstärkter Kessel erbt vom Kessel (alle Funktionen inkl. Milch). – Duplikat, siehe `claude-crucible4` unten (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Milchkessel-JEI/Jade fixen (images/18: „Empty 1B“). – Duplikat, siehe `claude-crucible4` unten (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Netherit-Fass fehlt. – Duplikat, siehe `claude-crucible4` unten; Vorschau previews/gadgets (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
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
- [x] Crucible betretbar; ab hoher Hitze Schaden wie Magma. (Audit 09.10.: 5dea165f4 (N11 P6)),
- [x] Resonanzstab: bei Nutzung weiter nach vorne neigen (Laser aus der Spitze), Laser amethystfarben. – Neigung −60°, Punkt #B38EF3, alte Datei mit Rot wechselt (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Kern-Items: Schimmer-Animation. → `claude-tex7`.
- [x] Elytra-Pad drei Stufen: 5 / 32 (Netherit) / 128 (Enderit, Höhe ggf. 1,5×128). – 5×5×5 / 32×32×32 / 128×128×192, alte Pads migrieren (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Trank-Pad etwas buffen. – Wirkdauer 45/90/180 s (statt 30/60/120), Abklingfaktor 1,5 (statt 2) (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Crucible betretbar; ab hoher Hitze Schaden wie Magma. – Kollision bis 9/16, Brand ab HIGH, Option `crucibleBurnDamage` (1, 0 = aus) (P1–P8 in `claude-wave1` via `claude-oc-tweaks11`, Plan `docs/ai/PLAN-OC-TWEAKS-2026-10-06.md`; Reste/Nachprüfung `claude-q-gadgets`; Client-Abnahme offen)
- [x] Kern-Animationen mit Seltenheit (drehen, Bumerang, hochsteigen …; je cooler desto seltener), eigene längere Animation bei Erz-Umwandlung. → `claude-tex7` (`CoreHandMotion`, Erste Person, Config `tools.enableCoreAnimations`); Client-Sicht offen.
- [x] (Besitzer 13.10.: Werkbank-Rezept bleibt, nichts bauen) Rückfrage beantwortet: Elytra mit Reparatur im Schmiedetisch – Vanilla-Schmiederezepte prüfen nur Items, keine Verzauberungen; möglich nur mit eigener Rezept-Zutat je Loader (offen: soll das gebaut werden?).

## Nachtrag 18 (2026-10-07, Besitzer) – Plan: docs/ai/PLAN-N18-SIMPLEMAPS-TRIMS-2026-10-07.md
- [x] Simple Trims als Sub-Mod von SB (Vorlagen, platzierbar, Axt ohne SB / Hammer mit SB) – Frage F1 (claude-q-trims: Stufe 1; claude-trims2: Stufen 2–4, Plan-Abschnitt „Stufen 2–4“; Besatz-Boni/Stats-Panel bleiben vorerst in SB)
- [x] Simple Maps als Sub-Mod von SB: Wegfinder-Karte (unendlich, Spieler mittig, erweitern/kopieren/kombinieren, GUI mit Lesezeichen, Wegpunkte 1–8, Kontextmenü, Snap/Raster, Locator-Bar) – Fragebogen F2–F10, Feature-Vorschläge 1–8 (claude-q-maps (Modul simplemaps; Sub-Mod-Bündelung offen))
- [x] (schon erledigt durch claude-q-blocks/CreativeTabSettings, in wave1) Config je Super-/Sub-Mod: Items in Kreativ-Tabs an/aus (simplelib) (teilweise: SB-Schalter addItemsToVanillaTabs ff143698a; simplelib-Gerüst je Mod fehlt)
- [x] Guides: farbigen Strich an freigeschalteten Lesezeichen entfernen (Audit 09.10.: 33b051693)
- [x] Dev-Kreativtabs immer ans Ende der Reihenfolge (Audit 09.10.: ff143698a (Test: Dev-Tab zuletzt)),
- [x] (claude-q-maps; offen: SB-Bündelung als Super-Mod, Feature 3, Forge-Testziel, Besitzer-Abnahme) Simple Maps als Sub-Mod von SB: Wegfinder-Karte (unendlich, Spieler mittig, erweitern/kopieren/kombinieren, GUI mit Lesezeichen, Wegpunkte 1–8, Kontextmenü, Snap/Raster, Locator-Bar) – Fragebogen F2–F10, Feature-Vorschläge 1–8
- [x] Config je Super-/Sub-Mod: Items in Kreativ-Tabs an/aus (simplelib) (claude-q-blocks/small)
- [x] (schon erledigt 2026-10-08, GuideContent.bookmarkStripe; Test GuideBookTests) Guides: farbigen Strich an freigeschalteten Lesezeichen entfernen
- [x] (schon erledigt ff143698a, Test in DataIntegrityTests) Dev-Kreativtabs immer ans Ende der Reihenfolge
- [x] Sandwiches appetitlicher (Vorschau-Varianten) (claude-q-texrest: A/B/C, A eingebaut; Besitzer-Abnahme offen)
- [x] 0,125er-Blöcke: maximale Stapelgröße 128 (claude-q-storage: 99 = Engine-Grenze, 128 nicht speicherbar; Schach-Achtel, Material-Achtel aus claude-q-hammer nachziehen)
- Grundsatz festgehalten: Konsistenz zwischen allen Simple-Mods, Gemeinsames in simplelib, UI-Bausteine (inkl. Kontextmenü) dokumentieren (docs/ai/UI-BAUSTEINE.md anlegen)

## Nachtrag 20 (2026-10-07, Besitzer) – Konzept: docs/ai/KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md
- [x] Simple Maps: Rezept A + seltene Fundorte (auch Nether-/End-Karte) – in PLAN-N18 festgehalten (claude-q-maps)
- [x] (claude-q-brew) Neue Effekte (zuerst als Tränke): Zittern I/II, Trugbild, Umgekehrtes Trugbild, Verblasst (Graustufen inkl. Inventare, ohne Esc-Menü),
- [x] (claude-q-maps) Simple Maps: Rezept A + seltene Fundorte (auch Nether-/End-Karte) – in PLAN-N18 festgehalten
- [x] Neue Effekte (zuerst als Tränke): Zittern I/II, Trugbild, Umgekehrtes Trugbild, Verblasst (Graustufen inkl. Inventare, ohne Esc-Menü) (claude-q-brew)
- [x] Mob Deceiver, Stufe 1 (claude-q-deceiver): Modul `simplemobs`, Entity auf 3 Loadern, Modell/Renderer/6 Animationen, Textur, Wellen/Eskalation/Teleport/Mirage-Puls, Spawns, Loot (Täuscherstoff), Spawn-Ei
- [x] Deceiver Stufe 2: Verwandlung in beliebige Mobs (Renderer-Delegation), Tarnumhang (Drop), Schattenlos-Fakes, Config (Wellen-Obergrenze), Spiegelbild, falsche Truhe, Wiki-Seite/Testzentrale, Client-Abnahme (claude-deceiver2, in claude-wave1 seit 0d9d0f121)
- [x] (claude-deceiver2) Deceiver Stufe 2 Teil Besitzer 11.10.: Tarnumhang (3-Block-Erkennung, Verwandlung in Mobs/Spieler mit Skin+Rüstung, Enttarnung 5x5), Trugbild-Spiegel, stille Unsichtbarkeit, Spezialangriff Verdoppeln, Brustplatten-Deceiver (280/6, doppelte Trugsplitter), Trugbild-Trank aus Trugsplitter (mit SB). Offen: Verwandlung des Deceivers in beliebige Mobs per Tag, Schattenlos-Fakes, falsche Truhe, Config.
- [x] Später: Furcht-Mob (extrem stark, verursacht Zittern) – Konzept folgt (Audit 12.10. Dublette, zusammengeführt mit Zeile 824 / Überblick: (Furcht-Mobs, A))

## Nachtrag 32 (2026-10-10, Besitzer-Entscheidungen)
- [x] Enderit-Eimer nimmt auch Milch auf (2 Füllungen wie die anderen; Trinken = 1 Füllung). (claude-q-milk: `enderite_milk_bucket` halb + `_full`, Melken Kuh/Ziege/Pilzkuh, Trinken wie Vanilla-Milch, Milchkessel über Item-Tag `simplesandwiches:milk_buckets` + Crafting-Rest, Texturen im Generator; Client-Abnahme offen)
- [x] Seelenlava im Enderit-Eimer: 2 Füllungen bleibt.
- [x] Guide-Titel DE „Astral-Verzauberer“ (statt „Astraler Verzauberer“), Text kürzen damit es passt. (claude-q-milk)
- [x] Kreativ-Bauzauberstab ohne Material: bleibt; im Survival nicht herstellbar (kein Rezept).
- [x] QoL-Aufteilung in Sub-Mods (Container/GUI → „Simple QoG“ o. ä., Bewegung, Landwirtschaft, Werkzeuge/Haltbarkeit, Kreaturen, Wetter, Tresor); jeder Name beginnt mit „Simple“. Name für simplecontainers: „Simple Interfaces“ (Besitzer 10.10.). (claude-q-qolsplit: Umbenennung simpleinterfaces + Migration, Simple Weather als 1. Sub-Mod mit Schalter „Simple Weather aktivieren“; Namen Movement/Farming/Tools/Creatures/Loot im Plan; restliche Sub-Mods auf `claude-qol4`)
- [x] (claude-q-maps2) Simple Maps als Sub-Mod in SimpleBuilding bündeln (Schalter „Simple Maps aktivieren“; Config-Tab „Simple Mods“, Neustart nötig).
- [x] (claude-q-maps2) Simple Maps Feature 3: Struktur-Markierungen (nur entdeckte Strukturen), im Karten-GUI über die linken Lesezeichen abschaltbar.
- [x] Shellker: Konzept + Vorschau; Schale ergibt eine NEUE Helm-Variante (kein Ersatz des Schildkrötenhelms). (Konzept: docs/ai/KONZEPT-MOBS-2026-10-07.md) (claude-mobs2)
- [x] Seelenfeuer-Lohe: Konzept + Vorschau. (Konzept: docs/ai/KONZEPT-MOBS-2026-10-07.md), (claude-mobs2)
- [x] Simple Trims als Sub-Mod starten (Plan N18). (claude-q-trims: Stufe 1; Rest Stufen 2–4 siehe Plan)
- [x] Shellker: Konzept + Vorschau; Schale ergibt eine NEUE Helm-Variante (kein Ersatz des Schildkrötenhelms). (claude-mobs2)
- [x] Seelenfeuer-Lohe: Konzept + Vorschau. (claude-mobs2)
- [x] Deceiver umsetzen (Konzept freigegeben) – Stufe 1 fertig (claude-q-deceiver), Rest siehe Nachtrag 20.
- [x] Furcht-Mob und niedlicher End-Mob: später. (Audit 12.10. Dublette, zusammengeführt mit Zeile 824 / Überblick: (Furcht-Mobs; End-Mob erledigt))
- [x] Stärkerer Wither + Biom-Pinsel: Konzept. (Konzept: docs/ai/KONZEPT-WITHER-BIOMPINSEL-2026-10-10.md) (Audit 12.10. erledigt: Konzept liegt vor (7c51f290b); Umsetzung im Überblick (B))
- [x] Simple Respawn: noch nicht. (Audit 12.10. erledigt: Simple Respawn in claude-wave1 gemergt (8632d24a7))
- [x] Prinzip Entdeckbarkeit: Konzept. (Audit 12.10. erledigt: Konzept liegt vor (7c51f290b); Umsetzung im Überblick (B))
- [x] KI-Queue: Lauf per Button in der Zentrale freigeben → automatisch an Claude geben (Besitzer 10.10.)., (Audit 12.10. erledigt: entschieden: keine automatische Weitergabe an Claude (siehe Zeile darunter))
- [x] Prinzip Entdeckbarkeit: Konzept. (Konzept: docs/ai/PRINZIPIEN-ENTDECKBARKEIT.md) (Audit 12.10. erledigt: Konzept liegt vor (7c51f290b); Umsetzung im Überblick (B))
- [x] KI-Queue: keine automatische Weitergabe an Claude (Abarbeitung auf Zuruf).
- [x] Port 26.2/1.21.11/26.4 erst nach Besitzer-Abnahme im Spiel. (Audit 12.10. Dublette, zusammengeführt mit Zeile 73 / Überblick: (Port-Run))

- [x] (bereits in wave1 via claude-q-astral3, geprüft 13.10. astral* 21/21) Astral Enchanter Stufenregel (Besitzer 10.10.): 0–30 normale Regale; 30–40 mit Lohen-Regalen ODER Lohen-Obsidian-Boden; 50 nur mit beidem; je Stufe mehr Punkte verteilbar; Konzept erweitern (claude-q-astral3).
- [x] Alte Verschleiß-Optionen der Brecher-Kolben nicht migrieren (Besitzer: ok).

## Nachtrag 38b (2026-10-11, Besitzer, Deceiver Stufe 2 – in Arbeit auf claude-deceiver2)
- [x] Tarn-Umhang (Brustplatz): Erkennung erst im 3-Block-Radius; Rechtsklick auf Mob/Spieler → Verwandlung (Spieler mit Skin + Rüstung); Schlag/Abbauen → Rückverwandlung; Enttarnung: angegriffener Mob + 5x5, danach erhöhter, aber verringerter Radius. (claude-deceiver2, in claude-wave1 seit 0d9d0f121)
- [x] Unsichtbarkeitstrank ohne Partikel; Trugbild-Spiegel. (claude-deceiver2, in claude-wave1 seit 0d9d0f121)
- [x] Spezialangriff: Mob-Anzahl verdoppeln, Chance 5 % → 80 % bei wenig Leben. (claude-deceiver2, in claude-wave1 seit 0d9d0f121)
- [x] Gepanzerter Deceiver: etwas schwerer, doppelter Neben-Drop; Täuscherstoff ~50 %, nie doppelt. (claude-deceiver2, in claude-wave1 seit 0d9d0f121)
- [x] Täuschungstrank (friedlich/feindlich, verlängert/verstärkt) aus dem Neben-Drop. (claude-deceiver2, in claude-wave1 seit 0d9d0f121)

## Nachtrag 52 (2026-10-13, Besitzer)
- [ ] Astral-Verzauberer umbenennen in „Enchanter“ / „Verzauberer“ (Besitzer-Wahl). Seiten und Unterseite bekommen eine Randlinie bis zum Tuch; Unterseite aus Obsidian. (claude-n52)
- [ ] Brücken-Redstone-Textur: Schichten z-fighten in der Höhe → einfache Textur ohne Überlagerung. (claude-n52)
- [ ] Dynamisches Licht auch für Fackel in der Hand (wie Laterne/Lava). (claude-n52)
- [ ] Rissiges Eis wird nicht richtig gerendert (Besitzer-Screenshot 13.10.: platzierter Block fast unsichtbar, nur Umriss sichtbar, Inhalt wirkt wie durchsichtig/fehlende Textur bzw. falscher Render-Layer). (claude-n52)
- [x] (claude-gatefix16, in wave1) Rissiger Diamant + Vorschlaghammer In-World → 9 Diamant-Kiesel (nach Aufteilung 3d, da rissiger Diamant nach SimpleLib zieht).,
- [x] Astral-Verzauberer umbenennen in „Enchanter“ / „Verzauberer“ (Besitzer-Wahl). Seiten und Unterseite bekommen eine Randlinie bis zum Tuch; Unterseite aus Obsidian. (claude-n52)
- [x] Brücken-Redstone-Textur: Schichten z-fighten in der Höhe → einfache Textur ohne Überlagerung. (claude-n52)
- [x] Dynamisches Licht auch für Fackel in der Hand (wie Laterne/Lava). (claude-n52)
- [x] Rissiges Eis wird nicht richtig gerendert (Besitzer-Screenshot 13.10.: platzierter Block fast unsichtbar, nur Umriss sichtbar, Inhalt wirkt wie durchsichtig/fehlende Textur bzw. falscher Render-Layer). (claude-n52)
- [ ] Rissiger Diamant + Vorschlaghammer In-World → 9 Diamant-Kiesel (nach Aufteilung 3d, da rissiger Diamant nach SimpleLib zieht).

## Nachtrag 51 (2026-10-13, Besitzer)
- [ ] Redstone-Kompatibilität: Astral-/Nihil-Geräte akzeptieren alle 3 Redstone-Arten + Brücken-Redstone (Connector); alle Redstone-Geräte (Vanilla + Mod) akzeptieren alle Mod-Redstone-Linien. Fehlende Blöcke ergänzen: Astral-Redstone-Block, Nihil-Redstone-Block (usw., analog Vanilla-Redstone-Block) und Brücken-Redstone-Block.
- [x] (claude-astral-cloth, in wave1) Astral-Verzauberer Seitentextur: Tuch (Goldsaum) bis an den Rand ziehen.
- [ ] Eisen-Tiegel In-World-Bau: Tiegel + 16 Eisenbarren; jede Seitenplatte 2 Barren (erst halbe Platte, dann ganze, jeder Schritt sichtbar) = 8; jeder Griff 2 Schläge/Barren = 12; letzte 4 für die Wölbungen am oberen Rand (je Schlag eine). Jedes weitere Upgrade: 12 Schläge, alle 3 Schläge 1 Material (4 rissige Diamanten / 4 Netherit-Nuggets / 4 Enderit-Nuggets), jeder Schritt sichtbar.
- [ ] Crafter-, Smither-, Auto-Crafter-UIs verbessern: Pfeil immer als gepunkteter Redstone-Pfeil (vorhandene Textur aus den Redstone-Linien); Filter beim Auto-Crafter über den Pfeil (nicht gedrängt).

## Nachtrag 50 (2026-10-13, Besitzer)
- [ ] End-Biome mit dichtem Nebel; Randbiom + Kern, im Kern spawnen Hollow Watcher (Tag simpleend:watcher_core) – gruseliger Bereich. (claude-endbiome2 + claude-n50-small)
- [x] Zittern aktiv, solange Furcht-Mob dich ansieht/getriggert ist; 3–5 Stufen nach Nähe. Watcher besser getarnt. (claude-n50-small)
- [x] Sculk Jaws spawnen natürlich im Deep Dark. (claude-n50-small)
- [x] Fragentisch-Rezept: unten Mitte zusätzlich Obsidian. (claude-n50-small)
- [x] (claude-wither2) Verdorrter Wither: Beschwörung per Netherit-Helm auf dem MITTLEREN Schädel (vor dem dritten Schädel aufsetzen) statt Netheritblock; zweite, stärkere Stufe mit Enderit-Helm; bei wenig Leben stärkere Witherskelette (mit Rüstung); Rückstoß unter 15 % stark; Drops: Witherherz, 3–5 Netherit- bzw. Enderit-Nuggets, 1–2 Nethersterne. (claude-wither2)
- [ ] Netherstern + neue Upgrade-Vorlage („Strong Upgrade“) auf Leuchtfeuer: 6 weitere Effekte, 2 Ebenen größer baubar, entsprechend mehr Reichweite; dasselbe für Conduit soweit möglich. (Konzept fertig, alle Empfehlungen übernommen 13.10.; Umsetzung offen)
- [x] Upgrade-Vorlagen-Tooltips: Hufeisen-Upgrade „applies to: Iron–Enderite ingots“; Enderit-Upgrade „Netherite equipment and blocks“ (+ Diamant-Ausrüstung/-Blöcke). (claude-n50-small)
- [ ] Mob-Köpfe in neues Sub-Mod „Simple Mobheads“, alle Mob-Köpfe implementieren; SimpleBuilding requires es; aus Simple Fun entfernen. (Konzept fertig, alle Empfehlungen übernommen 13.10.; Umsetzung offen)
- [ ] End-Biome: seltener, viel größer; Grasdichte höher; Stöcke-Bäume wie normale Bäume aus Stöcken, gigantisch; Stamm abbauen → alle Stöcke brechen; im End keine Holzbäume, nur Stöcke-Bäume + Drift-Frucht; Oberwelt-gepflanzte End-Bäume gigantisch, 4×4 → größter Baum im Spiel; Setzling-Rückgabe ~0,8 je Baum (Oberwelt), im End ~1,5 je Stöcke-Baum; Früchte gedeihen nur im End, in der Oberwelt verdorbene Variante (Drifting-Effekt); Blätter durchsichtig/exotisch, kein Vollblock. (claude-endbiome2)
- [x] Hollow Watcher teleportiert sich 1 s nach einem Treffer weg. (claude-n50-small)
- [x] Erhöhte Schwerkraft (Gravity-Effekt): mehr Fallschaden (Federfall mit einrechnen), „High Gravity“ gerecht. (claude-n50-small)
- [x] Resonanz-Button: Cap-Zeile im Tooltip weg; Herz-Texturen überarbeiten (richtig aussehen); Button-Textur wie ein deaktivierter Slot. (claude-n50-small)

## Nachtrag 49 (2026-10-12, Besitzer)
- [x] (claude-n49-small) Seelen-Lohe (Soul Blaze) schießt Seelenfeuer-Kugeln (Soulfire Charge, craftbar mit Seelen-Lohenstaub); Treffer gibt Seelenbrand-Effekt.
- [x] (claude-deceiver3, in wave1) Deceiver-Umhang: Kopf-Umhang mit Kapuze, liegt als 3D-Modell auf dem Rücken (ähnlich Elytra); mit Elytra: Elytra darunter gerendert, beim Fliegen ragen die Flügel seitlich heraus, Umhang bleibt auf dem Rücken. Deceiver-Mob trägt ähnlichen Umhang. (claude-deceiver3)
- [x] (claude-n49-inv, in wave1) Extra-Kopf-Slot im Inventar (B-Menü) über dem Nebenhand-Slot, auf Höhe des Helm-Slots: nur Mob-Köpfe oder geschnitzter Kürbis; deren Effekte gelten; Helm-Positionierung je Kopf anpassen (Kürbis, Silberfisch-Kopf usw.: Helm sitzt richtig, sichtbar, kein Z-Fighting).
- [ ] Resonanz-Button: zwischen neuem Kopf-Slot und Nebenhand-Slot, slotgroß, nur Herz (Hover); Stufen Cobble, Stone, Copper, Iron, Gold, Diamond, Netherite, Enderite mit je eigenem Herz-Indikator. Tooltip ohne Fettschrift: Zeile 1 „Resonance: x“ orange (uncommon), dann „(max 2.0x)“ grau; Rest analog, erster Buchstabe fett, L→E (E, S, C); Cap-Zeile analog, letzte Zeile und „the better of …“-Zeile streichen; S = Distanz+Zeit, je max 0.5.
- [ ] Inventar-Helligkeit als Config: Light (aktuell), Dark, Dawn (zwischen Dark und Light).
- [x] (claude-n49-small) Geschnitztes Holz (carved logs): Item-Modell entspricht nicht dem Block-Modell → angleichen.
- [x] (claude-n49-small) Shellker-Helm und Schildkrötenpanzer trimmbar (beide waren schon über #head_armor in #trimmable_armor; Shellker jetzt explizit im Tag + Test, Schildkröte hatte Icon-Muster schon).,
- [x] Extra-Kopf-Slot im Inventar (B-Menü) über dem Nebenhand-Slot, auf Höhe des Helm-Slots: nur Mob-Köpfe oder geschnitzter Kürbis; deren Effekte gelten; Helm-Positionierung je Kopf anpassen (Kürbis, Silberfisch-Kopf usw.: Helm sitzt richtig, sichtbar, kein Z-Fighting). (claude-n49-inv: Slot 46 über Nebenhand, auch Rucksack/Kreativ; Effekte Kürbis/Mob-Sicht/Kopf-Fähigkeiten; Helm je Kopf skaliert, Silberfisch oben drauf; Client-Abnahme offen)
- [x] Resonanz-Button: zwischen neuem Kopf-Slot und Nebenhand-Slot, slotgroß, nur Herz (Hover); Stufen Cobble, Stone, Copper, Iron, Gold, Diamond, Netherite, Enderite mit je eigenem Herz-Indikator. Tooltip ohne Fettschrift: Zeile 1 „Resonance: x“ orange (uncommon), dann „(max 2.0x)“ grau; Rest analog, erster Buchstabe fett, L→E (E, S, C); Cap-Zeile analog, letzte Zeile und „the better of …“-Zeile streichen; S = Distanz+Zeit, je max 0.5. (claude-n49-inv: 8 Herz-Stufen, Tooltip neu, S als Summe)
- [x] Inventar-Helligkeit als Config: Light (aktuell), Dark, Dawn (zwischen Dark und Light). (claude-n49-inv: SimpleLib UiBrightness, Einstellung im Simple-Interfaces-Bildschirm)
- [ ] Geschnitztes Holz (carved logs): Item-Modell entspricht nicht dem Block-Modell → angleichen.
- [ ] Shellker-Helm und Schildkrötenpanzer trimmbar.

## Nachtrag 48 (2026-10-12, Besitzer)
- [x] Wayfinder-Karte: Zoom feiner abstufen, Navigation verbessern. (claude-maps-zoom: Stufen 1,2,4,8,16,32,64, Pfeile/WASD, Home/C, 1-8)
- [ ] Octet-Blöcke untereinander kombinierbar (verschiedene Materialien in einer Zelle).
- [x] Deceiver-Umhang am Kopf (Kopf-Slot statt Brust); Verkleidung überall sichtbar: Spielermodell im Inventar, Hände in der Ego-Ansicht; auch Schlag in die Luft (Versuch zu hauen) hebt Tarnung auf. (claude-deceiver3: Kopf-Slot = Kapuze + 3D-Umhang auf dem Rücken über Elytren, Inventar/Ego/3rd, Luftschlag; Deceiver mit Rückenumhang)
- [x] Trick-Spiegel ausbalancieren: Kampf weniger beeinflussen, nur leicht erleichtern; notfalls vorerst entfernen. (claude-deceiver3: bleibt, 3 s/60 s/20°/12 Blöcke/2 s Leuchten/max. 3)
- [x] Shellker-Schale: Item-Textur wie Vanilla shulker_shell, angepasst (Mojang-abgeleitet → beim Build erzeugen). (claude-deceiver3: vanilla_derived MODULE_OUTPUTS)

## Nachtrag 47 (2026-10-12, Besitzer)
- [x] (claude-discover1) Prinzip Entdeckbarkeit umsetzen (docs/ai/PRINZIPIEN-ENTDECKBARKEIT.md): Stufe 1 jedes Item Tooltip/Rezept-Freischaltung + Test; später Welt-Hinweise (Stufe 2 offen).
- [x] (claude-endwood-full) Aschholz/Dämmerholz: alles, was andere Holzarten haben (Regal, Octets, geschnitztes Holz, Holzkessel normal+entrindet, Schach/Checker?, alle SB-Holz-Features) – optional, wenn SB geladen.
- [x] Death Vault: kein Geld-Shop-Kauf, nur Craften (Besitzer 12.10.).
- [x] Portierung 26.2/1.21.11/26.4 erst nach Besitzer-Abnahme (bestätigt 12.10.).
- [x] Furcht-Mobs (Konzept KONZEPT-DECEIVER-EFFEKTE-2026-10-07.md „Furcht-Mob – Vorschläge“): F-A „Dread“ – spawnt NICHT natürlich, wird per Golem-Mechanik aus Blöcken gebaut/beschworen; F-C „Hollow Watcher“ im End (Außeninseln). Beide mit Zittern-Aura. (claude-fear-mobs)
- [x] Jar-Weitergabe an Zentrale-Downloads eingerichtet (nach grünem Gate: dist-publish.sh → sb-agent → sb-web, atomar; Backups *.bak-20261012). (claude-jar-dist)
- [x] Stärkerer Wither + Biom-Pinsel umsetzen (Konzept docs/ai/KONZEPT-WITHER-BIOMPINSEL-2026-10-10.md, Wither „halt stärker“). (claude-wither: Verdorrter Wither + Witherherz + Biom-Pinsel in Simple Mobs, Abschnitt „Umsetzung 13.10.“)
- [x] Niedlicher End-Mob: passive Endermite-Varianten in Ash Valley/Dawnlands, knabbern Endstein → durchwühlter Endstein (erholt sich wie Gras nach Schaf). (in claude-simple-end) (Audit 12.10. erledigt: Nibblemite in claude-wave1 (cc79c81b9))
- [x] Simple Respawn umsetzen (Besitzer 12.10.): niedergeschlagen statt tot; eigener Todesbildschirm mit Buttons Zuschauen (Bildschirm zu, Kamera 360° drehbar), Aufgeben (erst dann echter Tod), Inventar (nur ansehen, nicht bearbeiten; auf/zu); Mitspieler kann innerhalb einer Frist wiederbeleben (danach 3 Herzen, 0 Hunger). Konzept KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md Teil B. (claude-respawn) (Audit 12.10. erledigt: Simple Respawn in claude-wave1 gemergt (8632d24a7))
- [x] Baustab-Kerne: Besitzer 12.10. – bleiben reine Crafting-Zutat, keine Zusatzfunktionen.,
- [x] Simple Respawn umsetzen (Besitzer 12.10.): niedergeschlagen statt tot; eigener Todesbildschirm mit Buttons Zuschauen (Bildschirm zu, Kamera 360° drehbar), Aufgeben (erst dann echter Tod), Inventar (nur ansehen, nicht bearbeiten; auf/zu); Mitspieler kann innerhalb einer Frist wiederbeleben (danach 3 Herzen, 0 Hunger). Konzept KONZEPT-FARBPINSEL-RESPAWN-2026-10-09.md Teil B. (claude-respawn: neues Sub-Mod simplerespawn, Plan docs/ai/PLAN-SIMPLE-RESPAWN-2026-10-12.md)
- [x] Baustab-Kerne: Besitzer-Entscheidung zu Mechanik/Funktionen (docs/BAUWERKZEUGE-INTERAKTIONEN.md Teil 4). (Audit 12.10. erledigt: Besitzer 12.10. (N47): Kerne bleiben reine Crafting-Zutat, keine Zusatzfunktionen)
- [x] Mega-Handbücher erst, wenn alle Mods fertig sind. (Audit 12.10. Dublette, zusammengeführt mit Zeile 80 / Überblick: (Mega-Handbücher, E))

## Nachtrag 42 (2026-10-12, Besitzer)
- [x] End-Gras: wie totes Gras/tote Büsche (Vanilla dead bush / short dry grass), Farbe wie Endstein; wächst/platzierbar auf Endstein, Weltgenerierung auf den End-Inseln. (Audit 12.10. erledigt: End-Gras in simpleend (Zwilling oben ist abgehakt))
- [x] Death Vault (Besitzer 12.10.: bauen in Simple Tools, Konzept-Antworten verbindlich): nach dem Tod 3–5 zufällige Items des letzten Todes gegen Bezahlung zurückholbar; gegen Duping gesichert (nur Items, die beim Tod verloren gingen und nicht aufgehoben wurden; einmalig je Tod, serverseitig). Konzept: `docs/ai/KONZEPT-DEATH-VAULT-2026-10-12.md` (claude-concepts42), offene Besitzer-Fragen dort. (Audit 12.10. erledigt: Death Vault gebaut (claude-soulbound, bc4d2309c))
- [x] Soulbound-Verzauberung: Item bleibt beim Respawn; sehr selten und teuer (z. B. 35 Level), Konzept zuerst. Konzept: `docs/ai/KONZEPT-SOULBOUND-2026-10-12.md` (claude-concepts42), offene Besitzer-Fragen dort., (Audit 12.10. erledigt: Soulbound gebaut (claude-soulbound, bc4d2309c))
- [x] (claude-simple-end: end_grass/end_bush in simpleend, Biom-Tint) End-Gras: wie totes Gras/tote Büsche (Vanilla dead bush / short dry grass), Farbe wie Endstein; wächst/platzierbar auf Endstein, Weltgenerierung auf den End-Inseln.
- [x] Death Vault (NUR KONZEPT, nicht umsetzen): nach dem Tod 3–5 zufällige Items des letzten Todes gegen Bezahlung zurückholbar; gegen Duping gesichert (nur Items, die beim Tod verloren gingen und nicht aufgehoben wurden; einmalig je Tod, serverseitig). (Audit 12.10. erledigt: Zwilling: Death Vault gebaut (claude-soulbound))
- [x] Soulbound-Verzauberung: Item bleibt beim Respawn; sehr selten und teuer (z. B. 35 Level), Konzept zuerst., (Audit 12.10. erledigt: Zwilling: Soulbound gebaut (claude-soulbound))
- [x] (claude-soulbound) Death Vault (Besitzer 12.10.: bauen in Simple Tools, Konzept-Antworten verbindlich): nach dem Tod 3–5 zufällige Items des letzten Todes gegen Bezahlung zurückholbar; gegen Duping gesichert (nur Items, die beim Tod verloren gingen und nicht aufgehoben wurden; einmalig je Tod, serverseitig). Konzept: `docs/ai/KONZEPT-DEATH-VAULT-2026-10-12.md` (claude-concepts42), offene Besitzer-Fragen dort.
- [x] (claude-soulbound) Soulbound-Verzauberung: Item bleibt beim Respawn; sehr selten und teuer (z. B. 35 Level), Konzept zuerst. Konzept: `docs/ai/KONZEPT-SOULBOUND-2026-10-12.md` (claude-concepts42), offene Besitzer-Fragen dort.
- [x] (`claude-mobmanners`) Simple Creatures umbenennen in „Simple Mob Manners“ (Besitzer-Wahl 12.10.), Id simplemobmanners, mit Namespace-/Config-Migration (simplecreatures → simplemobmanners), Zentrale/Wiki/Lang anpassen.
