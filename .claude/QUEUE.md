# Warteschlange SimpleBuilding (Stand 2026-10-12, Hauptlinie 26.3)

Regeln: AGENTS.md; aktueller Bestand/Belege in docs/HANDOFF.md. Worker committen auf ihrem Branch ohne Push/Merge.
Ein erledigter Codepunkt ersetzt weder Client-Gate noch Besitzer-Abnahme.
Audit 2026-10-09 (claude-q-audit): offene Punkte gegen claude-wave1 9c7929ff4 geprüft; Restliste: docs/ai/ROADMAP-2026-10-09.md.
Queue-Pflege (Besitzer 12.10.): Neue Nachträge OBEN anhängen (direkt unter „Offen – Überblick“), Erledigtes per Haken
`[x]` + Branch/Commit abhaken (und die Zeile im Überblick streichen), neue offene Punkte zugleich in den Überblick
eintragen. Abschnitte ohne offene Punkte wandern monatlich unverändert nach `.claude/QUEUE-ARCHIV.md`
(Reihenfolge erhalten; letzte Archivierung 2026-10-12).

## Besitzer 17.10. – Prinzipien, Octet-Regel, Simple Blocks, ein Tab
Neue Regeln stehen in `docs/ai/PRINZIPIEN.md` (Einstieg), AGENTS.md 3.16/3.18/3.19. Inventare: `docs/ai/INVENTAR-OCTETS-2026-10-17.md` (23 fehlende Octets),
`docs/ai/INVENTAR-BAUBLOECKE-2026-10-17.md` (Verschiebe-Liste + Tab-Plan).
- [x] (B, b-octetrule, 18.10.) (a) Octet-Regel umsetzen **nach Merge von `claude-s6-octets`**: fehlende Octets laut Inventar (13 Vanilla-Bretter + `ashwood`/`duskwood`-Bretter, `cut_sandstone`, `cut_red_sandstone`, `smooth_stone`, `petrified_oak`; Naturstufen Erde/Gras/Sand/Kies nach Besitzer-Entscheidung) im Octet-Generator (Gruppe `planks`), Rezepte, Abdeckungstest „jeder Stair/Slab-Basisblock hat ein Octet oder eine begründete Ausnahme“, Wiki/Guide. **Erledigt 18.10. (b-octetrule):** Bretter-Achtel gab es schon (15, `WOODS`); neu cut_sandstone/cut_red_sandstone/smooth_stone/polished_end_stone; Abdeckungstest `stairs_and_slabs_have_octets`; `petrified_oak_slab` + Naturstufen begründete Ausnahmen. Offen: Formsäge-Exklusivität für Octets (PRINZIPIEN A4b) ist separat.
- [~] (B, b-blocksmove, 18.10., ohne E-B1-Teil; + T2 aus (c); **umgesetzt auf b-blocksmove**, Plan `docs/ai/PLAN-B-BLOCKSMOVE-2026-10-18.md`, Belege im Plan §6; offen Merge+Gate, Client-Sicht) (b) Baublöcke ohne Fachort nach `simpleblocks` verschieben (neue Familienstufe, **nach den Octets**, vor Stufe 7): Schwerkraftblöcke, Naturstufen, Eis-/Schalen-Varianten, Froschlichter (15 Items), nach Entscheidung E-B1 auch Lichtscheiben und Schwefellichter (40); IdMoves/Aliase, Simple Blocks als Pflicht-Sub-Mod von SimpleBuilding. Liste: Baublock-Inventar.
- [~] (B, b-tabt1 b527c896d+, 18.10.) T1 ERLEDIGT (Belege: fabric-263/neoforge-263 *tab* 99/99, integration-263 3/3, module-simpleblocks fabric/neoforge/forge + standalone 110/110, check grün); T2 umgesetzt auf b-blocksmove (die 15 stehen im Tab `simpleblocks:blocks`, SB-Zeilen per T1 dahinter; Wiki-Tabtexte angepasst), offen Merge+Gate (c) Ein Kreativtab für Simple Blocks + SimpleBuilding-Baublöcke: Stufe T1 (mit geladenem `simpleblocks` füllt SimpleBuilding seine Baublöcke in den Tab „Simple Blocks“ und registriert den eigenen nicht; Standalone behält ihn) sofort nach (a) möglich, T2 mit (b). Plan: Baublock-Inventar, Abschnitt „Plan: ein Kreativtab“.
- [~] (A, claude-howler4, 17.10.) (d) Howler-Sounds überarbeiten.
- [x] (A, claude-optconcept, 17.10.) (e) Simple Optimization (`simpleoptimization`) – KONZEPT fertig: `docs/ai/KONZEPT-SIMPLE-OPTIMIZATION-2026-10-17.md` (Bereiche, Kompat-Mechanismus, Prioritätstabelle, Benchmark), Modpack-Liste `docs/ai/MODPACK-BESITZER-2026-10-17.md`. Offen: Besitzer-Review + Antworten auf §8 (Fragen 1–8).
- [x] (A, claude-opt1, 19.10.; Merge offen) (e1) Simple Optimization Stufe 1: Gerüst Super-Mod + Sub-Mods `simpleoptrender`/`simpleoptmemory`/`simpleoptserver` (Fabric 26.3), Bereichs-Tabelle, Mixin-Plugin mit Loader-Erkennung, Config, Status-Befehl; Benchmark-Harness `tools/bench/` (Mess-Mod, Bench-Welt, Kameraweg, results.json + report.html). Erst nach Besitzer-Review.
- [~] (A, claude-opt1, 19.10.; Teile + Bench fertig, offen compat-opt-263-Ziel, 2. Messreihe, Quad-Zähler) (e2) Simple Optimization Stufe 2: O1 Blätter-Culling, O7 Lichtkarten-Cache, S5a Sichtweite nach Serverlast + M1 IP-Ping, M2 Telemetrie; Kompat-Ziel mit Cull Leaves/More Culling/BadOptimizations/ServerCore.
- [ ] (e3) Simple Optimization Stufe 3 (Besitzer): Benchmark auf Besitzer-PC (`py -3 tools\bench\run_bench.py --profile pc ...`), `priorities.json` füllen.
- [ ] (e4) Simple Optimization Stufe 4/5: O3, O4, S1a, O9, S5b (Standard aus), Zielprüfung; danach L-Bereiche nur bei Messbedarf (Konzept §7).
- [ ] (e5) Kompatibilitätstest Modpack des Besitzers: `docs/ai/MODPACK-BESITZER-2026-10-17.md` §6 K1–K23 (26.3-Fassungen auf sb-test, Rest im Port-Run 1.21.11/beim Besitzer).

## Offen – Überblick (Stand 2026-10-13)

Besitzer 13.10.: Elytra-Reparatur erledigt (Werkbank-Rezept bleibt); Sprach-Bridge gestrichen; Modulaufteilung F1–F3 = Empfehlungen angenommen; Claims-Hauptschalter freigegeben (Default an nach Prüfung, claude-claims-on).

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
0. Claim-Urkunde (N54; Plan docs/ai/PLAN-CLAIM-URKUNDE-GUI-2026-10-13.md Fassung 2 freigegeben): Rechte-Code B/D/U/C/R/A/M, Claim-Tisch (Karte+Liste, Erweitern mit steigenden Kosten), Maps-Ebene Claims, Kopie in Werkbank; 4–5 Branches nacheinander
0a. [x] (B, b-claimharden, 17.10.) Claims-Härtung Rest (Knochenmehl/Sculk/Frostläufer, Prüfung Feuer/Fluss/Drache/Wither/Enderman/Kolben): ClaimBonemealMixin (Spreader-Böden nur ohne fremde Chunks im Radius 4/8), ClaimSculkMixin (Ladung stirbt an Claimgrenze), ClaimFrostMixin (Eis nur wo Träger bauen darf), Claims.sameLand; Feuer/Fluss/Drache/Wither/Enderman/Kolben waren schon gehookt + getestet (Stage4/EnvironmentClaimTests). Gametests claims_review_bonemeal/sculk/frost_walker. -- Beleg: simpletweaks:*claims* Fabric 72/72, NeoForge 72/72, Standalone Fabric+NeoForge je 72/72 (inkl. 3 neue Tests); Compile 3 Loader + :compileJava gruen.
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
- Abnahme im Spiel (Client, Testzentrale in der Besitzerwelt neu bauen): Pulsating-Trim, Astralgewölbe, Enderit-Pulver, Mega-Handbücher, Attractor/Echolot, Boost/Pads, Architectury-Mixin-Fix (Welle 25; Dubletten N-alt)
- Abnahme neue Module im Spiel: Models, Fun, Visuals/Sounds, QoL, Riding, Dimensions, Money, Forge-Clients, Mehrspieler (Welle 25)
- Weitere Sichtabnahme Buch-Screen, Truhen, Kern-Animation, Gadgets; Octant-Manager-Kontrast (Offen-Liste)
- Abnahme Zeilen-Layout in allen Kreativreitern (Offen-Liste)
- Abnahme visuelles Hub-Rendering/echte Clients, lokale Modpack-Kombinationen (Offen-Liste)
- Desktop-/Handy-Sichtprüfung von Wiki und Balancing-Zentrale (Offen-Liste)
- Forge: Clientdarstellung, Config-Persistenz, Default-Einschaltung abnehmen (Offen-Liste)
- Abnahme Simple Money; NeoForge-Client/alte Spielwelt prüfen (Offen-Liste)
- Weiter offen als Abnahme (Branches gemergt, Details in Memory uebergabe-2026-10-08.md): UI-Konzepte (claude-ui-concepts), Respawn, Soulbound/Death Vault, Claims, Howler, Shellker, Nibblemite, Texturen N44–N46

### (E) Bewusst später
- Hammer-Tempo später evtl. testen: 1. Block 2x, Block 2–9 je +0,6x, ab 10. je +0,5x oder +0,4x (große Flächen sparen viel, kleine wenig) (N54)
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
- [x] 4 `claude-split-s4-ovens`: Extraktion Ovens nach modules/simpleovens (claude-split-s4-ovens; offen: Merge nach wave1, SB-Config-Ofentempo ausblenden in Stufe 9)
- [x] 5a `claude-split-s5-crucibles`: Tiegel/Fässer/verstärkter Kessel/Enderit-Stufen nach modules/simplecrucibles (IdMoves, Config-Übernahme, Guide, Wiki, Tests; offen: Merge nach wave1)
- [x] (A, claude-s5b-crucibles, gemergt 17.10.) 5b Seelenlava, Eimer-Familie, JEI-/Jade-Seiten der Tiegel nach simplecrucibles (Plan: Stufe 5 Umsetzung, Abweichungen)
- [~] (A, claude-s6-octets, 17.10.) 6 `claude-split-s6-octets`: Octets nach Modulpfad (106 Blöcke, 377 Rezepte)
- [~] (A, claude-s7-hammers, 18.10.) 7 `claude-split-s7-hammers`: Hammers extrahieren (breiteste Kopplung)
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
- [x] (B, b-wikitime, 17.10.) Beschaffungszeit je Item: Szenarien „gezielt“/„normales Spiel“ als Eingaben (Vorbelegung = Annahmen aus sbdev/params.py, gekennzeichnet), Mittel/Median/90 % für k (Standard 1 und 6) je Quelle und kombiniert, Voraussetzungen/Routenschritt („vor Braustand & Tränke“) aus Daten; Item-Seite + Loot-Übersicht (Teilstand: nur Kisten/Tresore/Angeln ohne Verzauberungsbücher; Handel, Mob-Drops, Bücher ohne Zeitmodell). Beleg: wiki/obtain_time.py, wiki/tests/test_obtain_time.py (5), wiki/tests/time_ui.cjs (126 Checks), Plan `docs/ai/PLAN-B-WIKITIME-2026-10-17.md`
  - [x] Belegbare Teilmenge 2026-10-03: erwartete Kistenöffnungen bis zum ersten/sechsten Stück für eindeutige Bernoulli-Quellen; keine erfundenen Stunden. Zeit-/Szenariokonzept: `docs/ai/PLAN-WIKI-BESCHAFFUNGSZEIT-2026-10-03.md`.
- [x] (B, b-wikitime, 17.10.) Diagramm "Zeit bis k Stueck": inline SVG, logarithmische Zeitachse, k-Auswahl, Mittel/Median/90 % benannt, Zeitalter-Linien nur bei Nutzereingabe der Stunden; Item-Seite und Loot-Übersicht (Beleg wie oben)
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
- [ ] Abnahme im Spiel (Client schliessen, Testzentrale in der Besitzerwelt neu bauen): Pulsating-Trim, Astralgewoelbe + Enderit-Pulver, Mega-Handbuecher, Attractor/Echolot, Boost und Pads nach der Haertung, Client-Neustart fuer den Architectury-Mixin-Fix.
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
- [~] (B, b-styleaudit, 17.10.) Andere Mods an SimpleBuilding-Stil angleichen (sauber, einheitlich). Teilstand: Stil-Audit `docs/ai/AUDIT-STIL-MODULE-2026-10-17.md` (b-styleaudit) fertig, kleine Angleichungen umgesetzt (Metadaten, EN amerikanisch, DE-Umlaute, Tab-Schlüssel, Todesvorrat-Kontrast); offen: Befunde ST-10..ST-42 (Icons, FabricExample-Klassen, Config-Lang-Schema, Kreativtab-Baustein, UI von simplestats/tools/respawn/models/beacons auf simplelib, Guides). Merge nach wave1 offen. Teil 2 (b-stylefix2, 17.10.): ST-11 (Einstiegsklassen umbenannt, newmod-Token), ST-40 (Handbuch+Startquest maps/tools/enddragonfight), ST-41 (questionary blocks/stats/wanderers) erledigt, ST-20 nur Plan (Referenz ist Autoconfig-Schema b); offen bleiben ST-10, ST-12..16, ST-21..24, ST-30..35. Merge nach wave1 + Gate offen. Teil 3 (b-stylefix3, 17.10.): ST-16 (riding iconFile/Beschreibung), ST-22 (Schalter war in 8 von 11 Modulen schon da; ergänzt maps/mobs/tweaks + Wiki-Feature), ST-23/24 (Audit-Ausnahmen Model-only/jukebox_song), Fehlalarm mixed_octet (generate.py TECHNICAL_BLOCKS in Gegenrichtung) erledigt; offen bleiben ST-10, ST-12..15, ST-21 (Besitzer-Namenswahl). Merge nach wave1 + Gate offen.
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

## Nachtrag 71 (2026-10-16, Besitzer)
- [x] Abgenommen: End-Blätter, Karte, 3D-Items, Schwefel-Lichter (16.10.).
- [x] Copper Light Panes: Kupferrand durch die Farbe der jeweiligen Light Pane ersetzen (Rand in der Pane-Farbe statt Kupfer). (claude-copperrim; Abnahme offen)
- [x] Warped Wart (Wirrwarze, neues Item): kann in Bastionen in Kisten liegen; funktioniert exakt wie Netherwarze (auf Seelensand pflanzen, Wachstumsstufen, Ernte), wird aber für andere Tränke genutzt – v. a. alle hinzugekommenen Mod-Tränke. Dazu: Rezept 9 Warped Wart → Warped Wart Block; Blaue Netherziegel (türkis, an Warped Wart erinnernd) aus Warped Wart + Netherziegeln, mit Treppe/Stufe/Mauer (Baublöcke → Simple Blocks). (claude-warpedwart; Abnahme Texturen/Namen offen)

## Nachtrag 70 (2026-10-16, Besitzer)
- [x] (Plan docs/ai/PLAN-MODULFAMILIE-2026-10-16.md, Besitzer-Antworten §5) Modulaufteilungs-Plan (docs/ai/PLAN-MODULAUFTEILUNG-2026-10-11.md) auf die neue Zielstruktur erweitern: SimpleBuilding = Super-Mod mit Sub-Mods Simple Octets, Simple Blocks (neu), Simple End, später Simple Enderite, Simple Tools, Simple Gadgets (neu) usw.; gemeinsame Blöcke (z. B. polierter Endstein) nach simplelib; bedingte Freischaltung (Enderit-Werkzeuge nur mit Simple Enderite) und volle Kompatibilität. Erst Plan an Besitzer, dann Stufen nacheinander nach grünem Gate.
- [~] (A, Stufen 1-3 + Tiegel + Octets fertig 18.10. (master 03f75eebc); weiter Hämmer) Umsetzung Modulfamilie strikt nacheinander, je Stufe grünes Gate: (1) Simple Blocks [läuft], (2) Tiegel → simplecrucibles, (3) polierter Endstein → simplelib, (4) Octets → simpleoctets, (5) Hämmer → simplehammers, (6) Simple Gadgets (Rotator, Messuhr, Detektor, Magnet, Echolot, Attractor aus simpletweaks), (7) Lager → simplestorage, (8) Simple Enderite (Ausrüstung + Astral/Nihil-Endstein + Astralit/Nihilith; Enderit-Stufen anderer Module nur mit Enderite freigeschaltet), (9) Release. Zusätzlich: simpletools als Sub-Mod unter den SimpleBuilding-Super-Mod hängen.
- [x] Neues Sub-Mod Simple Blocks: fehlende Block-Varianten (N69) – läuft (claude-missingblocks). (claude-missingblocks; Besitzer-Abnahme offen)
- [x] GRUNDREGEL (präzisiert): Alle Baublöcke gehören zur SimpleBuilding-Familie, aufgeteilt in Sub-Mods (Simple Octets, Simple Blocks für fehlende Varianten, Simple End für End-Blöcke …); gemeinsam genutzte Blöcke in simplelib (AGENTS.md Regel 16). End-Holz bleibt damit in Simple End.
- [x] (erledigt 16.10.: nur simpleend hat Baublöcke – bleibt als End-Sub-Mod; simplelib nur Material-Blöcke) Bestandsaufnahme: welche Baublöcke liegen heute in anderen Modulen (z. B. simpleaesthetics, simpleend Holz-/Stein-Sets, simplecontainers?) – Liste mit Vorschlag „nach SB verschieben (mit IdMoves/Migration)“ vs. „bleibt (Funktionsblock, kein Baublock)“; Konflikt mit Modul-Unabhängigkeit (z. B. End-Holz ohne SB) je Fall benennen. Erst Liste an Besitzer, dann verschieben.

## Nachtrag 69 (2026-10-16, Besitzer)
- [x] (claude-modref, Konzept KONZEPT-MODREF-BLOECKE-2026-10-16.md, Antworten §6) YouTube-Video einer Mod mit für SimpleBuilding relevanten Blöcken auswerten (LINK FEHLT – beim Besitzer nachfordern): Mod heraussuchen, ihre Texturen als Referenz/Stilguide nutzen (nichts kopieren), für fehlende Blöcke ähnliche eigene Texturen erstellen; zusätzlich 2–3 eigene Blöcke (z. B. Sculk-Deepslate / Deep-Dark-Moos-Variante mit Sculk). Ablauf: erst Konzept mit Vorschau → Besitzer-Freigabe → Umsetzung.

## Nachtrag 68 (2026-10-16, Besitzer)
Wayfinder-Karte (Feinschliff)
- [x] Wegpunkt-Name auf der Karte vertikal zentrieren, darf nicht auf dem Icon liegen. (claude-mappolish)
- [x] Icons (auch 3D-Modelle wie Creeper-Kopf) füllen immer das ganze Quadrat aus, ohne abgeschnitten zu werden – alle Icons einheitlich groß; 1-px-Outline in Schwarz um das Icon; KEIN Hintergrund hinter dem Icon. (claude-mappolish)
- [x] Knöpfe links und rechts: besseres Design, das mehr an den Rahmen erinnert. (claude-mappolish)
End-Biome (Antworten N1–N5 zum Konzept KONZEPT-END-WALD-NEBEL)
- [x] N1: Wald-Nebel von außen – A + C + B umgesetzt (claude-endfog; Besitzer-Abnahme im Spiel offen).
- [x] N3: nur neue Chunks reichen. N5: Nebel so ok.
- [x] N4: Blätter noch überarbeiten – wirken nicht gewollt. Im Internet recherchieren, welche Blatt-Varianten es gibt (Vanilla, gute Ressourcenpakete/Mods), Liste der gut gemachten Designs anlegen und sich daran orientieren. (claude-endleaves2; Besitzer-Abnahme der Blätter offen)
- [x] BUG: Stock-Varianten der End-Bäume sind verbuggt (prüfen und beheben). (claude-endleaves2; Besitzer-Abnahme der Blätter offen)
HUD-Overlay (Rückmeldung zur Version in Abnahme 16.10.)
- [x] Ecken: außen nur 2 px abgerundet, wirkt zu eckig → runder. (claude-hudcopper)
- [x] Text im Overlay wieder im alten Farbstil (wie vorher). (claude-hudcopper)
Copper Light Pane
- [x] Genau wie die normale Light Pane (gleiche Farben/Funktion), nur leicht andere Textur, die die volle Farbe ausnutzt: schwaches Karomuster aus 100 % und 90 % Sättigung der Hauptfarbe mit etwas Noise. (claude-hudcopper)
Minibosse
- [x] Hollow Howler größer machen; als Miniboss etwas stärker als der Warden. (claude-miniboss)
- [x] Hollow Howler und „Knight“ sind Minibosse (Annahme: „Knight“ = Dread, der gepanzerte Ritter mit Kettenmorgenstern – beim Besitzer bestätigen). (claude-miniboss)
- [x] Hollow Howler Phase 2 (wenig Leben): spawnt Sculk Jaws gezielt unter einem Spieler, falls dort noch keiner liegt; liegt schon unter allen Spielern einer, dann zufällig in der Nähe eines Spielers. (claude-miniboss)
- [x] Bossleisten aller Bosse/Minibosse zeigen die lebensbasierten Phasen an (Markierungen/Abschnitte). (claude-miniboss)
Prinzip
- [x] Lesbarkeit als Prinzip festgehalten (AGENTS.md Regel, Kontrast ≥ 4,5:1 bei jeder UI-Änderung).
API
- [x] Besitzer 16.10.: Anthropic-API vorerst NICHT mehr nutzen (Rest-Guthaben aufheben) – nur Claude-Agenten in der Sitzung, bis der Besitzer Bescheid gibt.

## Nachtrag 67 (2026-10-15, Besitzer-Feedback zu den Abnahme-Seiten)
- [x] (oc-ui67) HUD-Overlay (Oktant/Messuhr/Resonanzstab): statt einfachem 1-px-Rand einen doppelten oder dreifachen Rand wie die übrigen Mod-Texturen/UIs; Ecken entsprechend stärker abgerundet (doppelt → 2 px Stufen, dreifach → eine Stufe mehr), jede Rand-Ebene läuft wie ein Mantel um die Ecke.
- [x] 3D-Items Schritt 2 freigegeben – Schritt 3 Einbau; dabei sicherstellen, dass keine Flächen um „oben“ kämpfen (kein Z-Fighting, auch im Häufchen/Stapel).
- [x] (abgenommen 16.10., N71 „Schwefel-Lichter“; claude-n58rest) Schwefel-Fackel/-Laterne/-Lagerfeuer: Texturen sehr stark an Vanilla (Fackel/Seelenfackel, Laterne, Lagerfeuer) orientieren, nur leichte Variationen (Vanilla-Ableitung nur zur Build-Zeit, nichts Vanilla-Abgeleitetes einchecken).
- [x] Leuchtherz (Howler, H4): übernehmen/einbauen. Claim-Tisch-Textur: übernehmen.
- [x] (oc-ui67) REGEL bei jeder UI-Bearbeitung: Lesbarkeit prüfen – Text mit ausreichendem Kontrast zum Hintergrund (z. B. ≥ 4,5:1), kein dunkler Text mit dunklem Schatten auf Grau.
- [x] (oc-ui67) BUG Claim-Urkunde, erster Dialog („Claim the chunk you are standing …“): schwarzer Text mit schwarzem Schatten auf Grau, kaum lesbar → beheben; dabei alle Claim-/Shop-/Karten-Dialoge auf Kontrast prüfen.
- [ ] Weitere Texturen meldet der Besitzer von Fall zu Fall.

## Nachtrag 66 (2026-10-15, Besitzer, beim Spielen im End)
End-Biome
- [x] Nebel viel zu extrem (nur 10–20 Blöcke, man sieht nur Silhouetten anderer Inseln/Bäume, irritierend) → schöner/weniger extrem einstellen. (oc-end66)
- [x] Biome (z. B. Dusk) zu klein – in ca. 1 Minute durchlaufen → größer, über mehrere Inseln, damit man sicher einem Hollow Watcher begegnen kann. (oc-end66)
- [x] Hollow Watcher Spawn-Bedingung: kann (nicht muss) spawnen, wenn man länger als ~2 Minuten im Wald ist – Begegnung schon beim ersten Besuch möglich. (oc-end66)
- [x] Biom-interner Nebel von außen: von außerhalb soll man nicht in den Wald hineinsehen können (dicht und/oder Nebel) – erst KONZEPT, gemeinsam besprechen (Vanilla-Mechaniken prüfen). (oc-end66: Konzept fertig, Besitzer-Entscheidung offen – docs/ai/KONZEPT-END-WALD-NEBEL-2026-10-15.md) (Besitzer 17.10.: Variante A jetzt, B später nur falls A nicht reicht; Elytra-Sicht/Client-Config/nur-neue-Chunks nicht gewählt) – bereits umgesetzt von A (claude-endfog aebed75fe, A+B+C, in master seit Gate 33fdd1a27; Config simpleend forestFogWall); offen nur Besitzer-Abnahme im Spiel.
End-Strukturen
- [x] BUG: Im End-Schiffswrack liegt lila Wolle – nur End-Schiffsblöcke (End-Ship-Palette) erlaubt. (oc-end66)
- [x] Loot der End-Schiffswracks: Verteilung erklären (Besitzer-Eindruck: entweder sehr schlecht – nur Chorus/Purpur – oder sehr gut); Wracks noch versteckter, dafür besserer Loot. (oc-end66)
- [x] End-Brunnen-Strukturen: keine Minecraft-Fackeln (brechen die Illusion) → End Rods statt Fackeln. (oc-end66)
Texturen
- [x] (oc-texaudit2) Im Spiel fehlen immer noch Texturen (viele Sachen verbuggt) – vollständig im Client prüfen (fehlende Sprites/Modelle, „missingno“) und beheben.
Simple Shops – Umbau
- [x] Verkaufsblock: Knopf „Developer: Käuferansicht“ (wie altes/neues Design umschalten) – Besitzer/Dev sieht und bedient den Kauf-Screen wie ein Käufer. (claude-shops2; Client-Optik ungeprüft)
- [x] Vitrine allein = eigene UI, Verkaufsblock allein = eigene UI; verbunden = gemeinsame UI: oben Ware + Bestand, darunter Preis, darunter Einnahmen. Einnahmen-Lager in Truhengröße (27 Slots), Gesamthöhe der UI wie eine Doppeltruhe, Trennstrich zwischen Block-UI und Inventar wie bei den anderen UIs. (claude-shops2; Client-Optik ungeprüft)
- [x] Einnahmen voll (Preis passt nicht mehr hinein) → Kauf deaktiviert. (claude-shops2; Client-Optik ungeprüft)
- [x] Neuer Block „Verkaufsblock-Lager“ (guter Name, z. B. Sales Storage): Truhengröße, gleiche Schutz-Eigenschaften wie Verkaufsblock/Vitrine, zieht automatisch Einnahmen aus dem Verkaufsblock darüber; mehrere übereinander geben wie Trichter nach unten weiter (große Shops). Nur wenn alle Blöcke denselben Besitzer haben. (claude-shops2; Client-Optik ungeprüft)
- [x] Viele Prüfungen/Tests, Missbrauchsfälle durchdenken (Duping, fremde Lager, Explosion, Kolben, Trichter, Chunk-Grenzen, Besitzerwechsel). (claude-shops2; Client-Optik ungeprüft)

## Nachtrag 65 (2026-10-15, Besitzer; Bilder kamen nicht an – bei Bedarf nachfordern)
Befunde aus den Besitzer-Screenshots 15.10. (Bilder 1–8, nur als Beschreibung verfügbar)
- [x] (claude-n65bilder) Bild 1: Platzierte Barren (lila/Enderit-artig, 3–4 Stück) stecken sichtbar ineinander, Diamant-Kleinteil überlappt mit Barren – bestätigt den Punkt „Platzierte Teile“. [in wave1 bereits behoben, Code geprüft; Client-Abnahme offen]
- [x] (claude-n65bilder) Bild 2: Markierter Container („Marked: Reinforced Chest“) wird über dem Inventar noch im ALTEN grellen Orange gezeichnet → auf neue Bronze-Palette umstellen; im Spieler-Inventar zeigt ein Item das lila-schwarze Fehlende-Textur-Karo (3. Reihe, letzter Platz) → gehört zu „Texturen fehlen im Spiel“. [Palette jetzt Bronze; Fehlende-Textur-Item: check_missing_textures 0 Fehler, Item statisch nicht identifizierbar, Client-Sicht offen]
- [x] (claude-n65bilder) Bild 3/5: Wayfinder-Karte: rechts vom geladenen Kreis senkrechte Streifen/Linien-Artefakte; Rahmen der Karte ebenfalls noch grelles Orange → Bronze. [in wave1 bereits behoben, Code geprüft; Client-Abnahme offen]
- [x] (claude-n65bilder) Bild 4: „Configure waypoint“: die Reihe der Kopf-Icons läuft weit über den rechten Rand des Dialogs hinaus (kein Umbruch/Scrollen) – im neuen Configure-Dialog sauber begrenzen; Farbleiste grell (entfällt laut N65). [in wave1 bereits behoben, Code geprüft; Client-Abnahme offen]
- [x] (claude-n65bilder) Bild 5: Wegpunkt heißt „TEST“ (Tooltip), auf der Karte steht „1“. [in wave1 bereits behoben, Code geprüft; Client-Abnahme offen]
- [x] (claude-endleaves2, N68 N4; End-Blätter abgenommen 16.10. N71) Bild 6: Aschholz-/Dämmerholz-Blätter wirken wie ein Raster gleichmäßiger lila Punkte auf braunem Grund – sehr künstlich (bestätigt N65 End-Bäume).
- [x] (oc-misc65) Bild 7/8: Kopf-Modelle teils abgeschnitten (Shulker-Köpfe in mehreren Farben oben/seitlich gekappt; im Kopf-Raster weitere Köpfe mit gekappten Teilen prüfen).

Platzierte Teile
- [x] Barren & andere platzierte Kleinteile überlappen ab 3–4 Teilen (stecken ineinander, Z-Fighting). Anordnung so ändern, dass sie immer überschneidungsfrei passen. (oc-placedparts)

Markierte Container (QoL verknüpfte GUIs)
- [x] Solange ein Container markiert ist: kleiner Banner rechts im Spielbild (Stil der UI-Elemente/Oktant-Overlay, etwa auf Oktant-Höhe, etwas darüber, damit er nicht mit dem Oktant kollidiert) zeigt, welcher Container markiert ist (Art + Name). (oc-marked)
- [x] Markierung abbrechen mit Escape (wie beschrieben) UND automatisch, sobald man einmal im Container war und ihn wieder verlässt. (oc-marked)
- [x] BUG Kreativ: Markierung bleibt aktiv, wenn man das Kreativinventar öffnet; Shift-Klick ins Inventar oder auf einen Kreativ-Tab (Item löschen) schiebt Items in den unsichtbar markierten Container. Darf nie passieren. (oc-marked)

Wayfinder-Karte
- [x] BUG: Kartenbereiche laden mit Artefakten/Linien (u. a. in einem Kreis) – Laden immer vollständig und korrekt sicherstellen. (claude-mapfix)
- [x] Näher heranzoomen als 1:1 (z. B. ein ganzer Chunk füllt die Ansicht). (claude-mapfix)
- [x] Wegpunkt-Leiste rechts: Zahlen weg; jeder Wegpunkt bekommt beim Anlegen ein zufälliges Icon; Icons auch aus beliebigen Items. Buttons quadratisch mit Padding links wie Lesezeichen, bei großer GUI-Skala größer (Platz oben/unten nutzen); Buttons exakt im UI-Stil (wie neues Oktant-Konzept, Bronze/Rahmen), linke Seite gleich gestaltet. (claude-mapicons)
- [x] Höhenkarte/Höhenlinien als Anzeigemodus, gespeichert; die Karte in Hand/Nebenhand zeigt denselben Modus. Zoom NICHT automatisch übernehmen, sondern eigener Knopf „Zoom synchronisieren“ (UX klären: feste Höhe/Ausschnitt so, dass Hand- und Menükarte dieselbe Ansicht zeigen). (claude-mapicons)
- [x] „Configure Waypoint“ neu: Farben zu grell → keine Farbpalette mehr. Stattdessen: Feld 1 Name (Textfeld); Feld 2 Auswahl wie Kreativ-Suchtab (Block, Item oder Mob-Kopf suchen) als Icon. Anzeigemodus: Blockmodell (3D) – nur wenn Blockmodell vorhanden, sonst deaktiviert – oder eine Seite des Blocks (Standard Vorderseite), Seitenwahl über ausgeklappten Würfel (Netz) zum Anklicken; bei Items ohne Blockmodell keine Seitenwahl. Nicht überladen, aber verständlich. Falls doch Farben: Minecraft-typisch gedämpft/texturiert (Rot wie Netherrack, Orange wie Lava, Magenta wie Purpur …). (claude-mapicons)
- [x] BUG: Wegpunkt-Name wird auf der Karte nicht angezeigt (zeigt „1“ statt „Test“). (claude-mapfix)
- [x] Linke Seite: Knopf „Koordinaten anzeigen“ – neben jedem Wegpunkt und dem Spieler die Koordinaten. (claude-mapfix)
- [x] Ebenen (je in den Karteneinstellungen an/aus): chunkgeladene Bereiche; Chunk-Loader der Mod als Markierung (wie Strukturen, keine Wegpunkte); platzierte aktive Shops mit Hover-Info (Angebot + Preis); weitere sinnvolle Ebenen vorschlagen. (claude-mapfix; Ideen: Claims, Spieler/Team, Tod/Heim/Respawn)

End-Bäume
- [x] Ash- und Duskwood-Blätter sehen unschön aus: Blockmodelle natürlicher – äußere Blätter andere Textur/Anordnung als innere, innere dichter (nicht ganz dicht), nicht wie „schwache Simulation“. (oc-endleaves)
- [x] BUG: Duskwood-Blätter verhalten sich NICHT wie Spinnennetze – beide Blätterarten gleich (Spinnennetz-Verhalten). (oc-endleaves)
- [x] Erklären: was zeigen/testen „Ashwood Boat“/„Duskwood Boat“ im eingebauten Obsidian (Testzentrale?) – Besitzer fragt. (oc-endleaves)
- [x] Besitzer möchte die End-Varianten der Bäume sehen (Vorschau/Screenshots); Baumgrößen gefallen. (oc-endleaves)

Sounds
- [x] (oc-lightpane2) Sculk Jaw: Schließ-Sound fehlt (wurde entfernt?) – wieder einen Sound wie Sculk-Sensor/Shrieker.

Light Pane
- [x] (oc-lightpane2) Redstone-Light-Pane: Standard schwarz (aus), nicht alle Farben.
- [x] (oc-lightpane2) Light Panes nur an, wenn gepowert (Logik umdrehen).
- [x] (oc-lightpane2) Neue Variante „Copper Light Pane“: anderes Design, einheitliche Farbe; Rezept wie Copper Bulb, aber Blaze Powder statt Blaze Rod; schaltet bei jedem Redstone-Puls um (an/aus) wie die Copper Bulb.

Kleinigkeiten
- [x] (oc-misc65) Glasscheibe schwebend von unten: man sieht hindurch, Unter-/Innenseite fehlt (nur Außenseite hat Textur) – prüfen (Vanilla vs. Mod-Scheiben) und beheben.
- [x] (oc-misc65) Enchanter: Tooltip des eingelegten Items verdeckt die Verzauberungen → im Enchanter auf der anderen Mausseite rendern, solange er auf den Bildschirm passt.
- [x] (oc-creativetabs) Kreativ-Tabs aktualisieren, aufräumen, vervollständigen (alle Module).
- [x] (oc-misc65) Abgeschnittene Texturen bei Köpfen (z. B. Shulker-Kopf) beheben.
- [x] (oc-misc65) Simple Fun Schweinekopf: genauso wie die anderen Mob-Köpfe (gleicher Code/Prinzip) – Spielerkopf wird zum Schweinekopf.
- [x] (oc-misc65) Vorschlaghammer-Exploit prüfen/beheben: Abbaufortschritt beim Schleichen (1 Block) und dann Loslassen (Fläche) wird übernommen → fast Instant-Abbau. Fortschritt beim Umschalten anteilig nach der Hammer-Formel auf die neue Blockzahl umrechnen.

## Nachtrag 64 (2026-10-15, Besitzer)
Hollow Watcher – Korrektur
- [x] Reichweite 48 Blöcke: außerhalb verfolgt er nicht mehr; ist man wieder innerhalb, macht er weiter und teleportiert sich regelmäßig zum Spieler. Meiden heißt: ihn ansehen und weglaufen (über 48 Blöcke). (claude-watcher3)
- [x] KORREKTUR Blickverhalten: Wird er DIREKT angesehen (in die Augen/Fadenkreuz), bleibt er NICHT stehen, sondern wird aggressiv. Die Gradzonen (±10° unauffällig, ±25° langsam aus dem Blickfeld, ±35° direkt aus dem Blickfeld, außerhalb von hinten angreifen) gelten für „in seine Nähe schauen“. Ersetzt den Freeze aus N63/claude-watcher2. (claude-watcher3)

Oktant-Manager (UI) – erst Konzept, Umsetzung nach Besitzer-Freigabe
- [x] Farben: das Orange wirkt grell → generell sanftere Farben. (claude-octantui2; Besitzer-Abnahme der Screenshots offen; Fabric-Client-Lauf 10 rote Screenshot-Tests ohne Bezug, Ursache offen)
- [x] Pixelfehler an den Ecken: ein Pixel zu viel, Ecken nicht in Treppenform (Besitzer-Screenshot 15.10.). (claude-octantui2; Besitzer-Abnahme der Screenshots offen; Fabric-Client-Lauf 10 rote Screenshot-Tests ohne Bezug, Ursache offen)
- [x] In-Game-Overlay des Oktanten: gleiche (grobe) Anordnung behalten, nur Hintergrund/Rahmen im neuen UI-Stil, aber schlank (Slim), wenig ablenkend, evtl. geringere Deckkraft; dunkel genug für gut lesbare Schrift; gleiche Farben/Design wie das aktualisierte UI. (claude-octantui2; Besitzer-Abnahme der Screenshots offen; Fabric-Client-Lauf 10 rote Screenshot-Tests ohne Bezug, Ursache offen)
- [x] Ablauf: Konzept + Vorschaubilder → Besitzer-Freigabe 15.10. (O1 B Bronze matt, O2 alle Kästen, O3 einheitlich, O4 78 %, O5 Helligkeit) → Umsetzung läuft (claude-octantui2).

## Nachtrag 63 (2026-10-15, Besitzer)
Simple Shops – Server-Shop
- [x] (oc-servershop) Creative-/Unlimited-/Server-Shop: nur per Befehl von Admins (OP) erstellt und konfiguriert; unbegrenzter Vorrat, alle Spieler (auch OPs) können dort kaufen. Management-Befehle: `list` (IDs s001, s002, …), `list` mit Koordinaten, mit Items/Preisen, info, edit, remove, teleport o. ä. Rechte serverseitig, kein Duping (Bestand unbegrenzt, Einnahmen verschwinden oder in Admin-Kasse – im Plan entscheiden).

Rotator – zweite Funktion
- [x] (oc-rotator) Rotator: platzierbaren Block in der Nebenhand + Rechtsklick auf einen Block tauscht den angeklickten Block gegen den aus der Nebenhand (abgebauter Block ins Inventar). Nur Blöcke, die ein Stein-Werkzeug abbauen kann (kein Obsidian o. ä.); wertvolle Blöcke wie Leuchtfeuer ausgeschlossen. Whitelist/Blacklist als Block-Tags bzw. Blocknamen in der Config (Liste hat Vorrang vor der Härte-Regel). Tausch kostet doppelte Haltbarkeit. Immer 1 s Ausholen (Wind-up-Animation), mit Effizienz V sofort; Zeit je nach Blockhärte leicht anpassen, damit Spitzhacke & Co. Vorteil behalten. Claims/Schutz beachten. (Namenspunkt bleibt offen, siehe nächste Zeile)
- [~] (B, b-names, 17.10.) Rotator: besseren Namen vorschlagen (Besitzer wählt). (Besitzer 17.10.: Transposer / Transponierer; nur Anzeigename, Item-ID bleibt rotator)
- [ ] Audit D-05: DE „Enderitklumpen“ vs. „Enderitnugget“ – Besitzer wählte 17.10. „nugget“ auf Basis einer falschen Angabe (Vanilla 26.3 DE heißt „Eisen-/Gold-/Kupferklumpen“, also passt „Klumpen“ zu Vanilla; auch Netheritklumpen betroffen). Rückfrage an Besitzer offen, nichts umbenannt.

Hollow Watcher – Verhalten (Abgrenzung zum Creaking)
- [x] Steht er hinter dem Spieler, streckt er die Hände über die Schultern des Spielers, sodass man sie im Bild sieht (Erschrecken); Hände markanter rendern.
- [x] Zittern (Shivering) V etwas stärker.
- [x] Blickfeld-Logik: friert NUR ein, wenn man ihn genau ansieht (Fadenkreuz). Bis ca. 20° Gesamtwinkel um die Blickmitte (±10°): unauffällig verhalten. Bis ca. 50°: langsam Richtung Blickfeldrand, nie auf dem kürzesten Weg (wirkt passiv). Bis ca. 70° (normales Blickfeld): direkt aus dem Blickfeld laufen und es mit Puffer meiden; dann hinter den Spieler gehen und angreifen. Winkel an das FOV des Spielers anpassen (min. 30, max. 110).
- [x] Bei Treffer: Teleport hinter den Spieler oder Fern-Teleport, um ihn zu erschrecken.
- [x] Mehrspieler-Konzept: Aggro auf alle Spieler in der Nähe; schaut ein Spieler ihn an, friert er; bei Treffer teleportiert er weg und sucht sich ein Opfer (z. B. zufällig) usw. – erst Konzept, dann Umsetzung.
- [x] Transport im Boot möglich (Diktat sagt „Hollow Howler“ – vermutlich Watcher, beim Planen klären), Spieler in seiner Reichweite nimmt aber Schaden; wird er getroffen, teleportiert er sich. (claude-watcher2; Konzept docs/ai/KONZEPT-WATCHER-BLICKFELD-MEHRSPIELER-2026-10-15.md)

Besitzer-Antworten Konzepte (15.10.)
- [x] Dorf-Wanderer: W1 neues Modul Simple Wanderers; W2 auch Seltenes und Mod-Angebote aufteilen, aber beim Dorf-Wanderer nichts allzu Seltenes; W3 Respawn nach 7 Tagen, nur mit Spieler vor Ort; W4 Pferd einfangbar; W5 tötbar wie Vanilla-Händler; W6 Spawn sofort beim ersten Dorfbesuch; W7 Soulbound nur beim fahrenden Händler; W8 Köpfe ja, Kerne nein; W9 Geld-Fund sofort als eigener Auftrag; W10 Biom-Sortiment gleich mit einbauen; W11 eigener Kopf später.
- [x] (oc-moneyfix) Simple Money: 238 Preislisten-Trades des fahrenden Händlers werden nie angeboten (Pool `wandering_trader/rare` wird nicht gelesen) – sofort beheben (W9).
- [x] (oc-wanderers) Dorf-Wanderer bauen nach Konzept docs/ai/KONZEPT-DORF-WANDERER-2026-10-15.md + Antworten W1–W11.
- [~] (A, claude-howler3, 16.10.) Hollow Howler Phase 1+2 umsetzen (H1); danach Phase 3 eigene synthetische Sounds (H2); Darkness-Puls nur in der Wut-Phase (H3); leuchtendes Herz als neue Emissiv-Textur mit Vorschau zur Freigabe (H4); Kamerawackeln mit Schalter „Bewegung reduzieren“ (H5). Konzept KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md. – Phase 1+2, H3, H5 erledigt (claude-howler-n63); H2 Phase-3-Sounds auf `claude-howler3` (Merge offen, Hörabnahme); offen: H4 Herz nach Besitzer-OK
- [x] (B, b-anim3, 5ac8e885d) Mob-Animationen in der vorgeschlagenen Reihenfolge verbessern (A1), Konzept KONZEPT-MOB-ANIMATIONEN-2026-10-15.md. – Schritt 1 erledigt (oc-anim-quick): Dread-Wind-up/Recovery, Nibblemite/Dragonmite-Wackeln, Shellker-Deckel, Deceiver-Idle, Soulfire-Blaze-Takt; Rest offen.; Schritt 3 (Dread-Kette, Deceiver-Flucht/Mantel, Tod) erledigt (claude-anim2); Schritt 3 Rest (b-anim3) erledigt: Nibblemite-Kauzyklus + Tod, Dragonmite-Tod, Deceiver-Idle-One-Shots, Dread-Kettenrasseln im Idle (Vanilla-Chain-Sound); offen nur Client-Sichtabnahme, Merge nach wave1
- [x] Sandwich-Brot-Unterrand genehmigt (oc-breadrim).
- [x] Item-Display Schritt 1 (Text-Vorschlag) freigegeben 15.10.; D8 Lapis/Splitter/Nuggets nach erster Welle; D9 Display-Block später als eigenes Thema.

## Nachtrag 62 (2026-10-15, Besitzer)
Rückport (Prinzip: docs/ai/PRINZIPIEN-VANILLA-RUECKPORTS.md)
- [x] (oc-jukebox) Jukebox mit Kiste darüber (Vanilla 26.4-Snapshot, Okt. 2026) in die Mod holen: liegt eine Schallplatte in der Kiste über der Jukebox, wird sie automatisch eingelegt (wie per Trichter, aber ohne Trichter); ist die Platte zu Ende, wird sie gegen die nächste Platte aus der Kiste getauscht, sonst zurück in die Kiste gelegt. Erst das genaue Vanilla-26.4-Verhalten nachlesen und nachbilden; Versions-Flag, damit es in Versionen mit Vanilla-Umsetzung aus ist; Zusammenspiel mit Jukebox-Verstärker/Schallplatten-Modul prüfen.
- [x] Generelles Prinzip notiert: Rückports vor Einbau und bei jedem Port je Version prüfen (AGENTS.md + docs/ai/PRINZIPIEN-VANILLA-RUECKPORTS.md).

3D-Darstellung platzierter Items (Vorbild: Mod „Items Displayed“)
- [x] (Schritt 1 freigegeben 15.10., Antworten im Vorschlag §7; Schritt 2 Bilder fertig (oc-display-previews), Freigabe Besitzer offen) Platzierte Items (eigene 3D-Darstellung beim Hinlegen) mehr in Richtung „Items Displayed“ bringen; dem Besitzer gefallen dort besonders Armor Trims (Schmiedevorlagen), Pottery Sherds (Töpferscherben) und Music Discs (Schallplatten). Ablauf verbindlich in 3 Schritten: (1) erst TEXT-Vorschlag, was sich je Item-Art wie ändert → Besitzer-Freigabe; (2) dann gerenderte Vorher/Nachher-Bilder → Besitzer-Freigabe; (3) erst dann im Spiel einbauen. Abgenommene Texturen dabei nicht ändern, nur die 3D-Darstellung/Modelle. (Besitzer 17.10.: Schritt 2 freigegeben → Schritt 3 Einbau) – Schritt 3 bereits eingebaut von A (claude-display3 4cf3e21aa, in master seit Gate 33fdd1a27); offen nur Besitzer-Abnahme im Spiel.

Quality of Life
- [x] (oc-qol62) Sprungkraft (Jump Boost, egal ob Trank, Leuchtfeuer oder Befehl): solange der Effekt aktiv ist, Schritthöhe mindestens 1 Block (einen Block hochlaufen ohne Springen). Mit Step-Height-Verzauberung verträglich (Maximum, nicht addieren). Config-Schalter.
- [x] (oc-qol62) Frosch im Eimer: Frösche wie Axolotl mit Wassereimer einfangen und wieder aussetzen (Variante/Name/Alter bleiben erhalten), Eimer-Item je Variante bzw. Variante als Komponente.

Item-Statistiken (Modul noch offen – Vorschlag im Konzept)
- [x] (oc-simplestats) (Konzept + Besitzer-Antworten 15.10. in KONZEPT-ITEM-STATISTIKEN §14; Bau läuft) KONZEPT zuerst: Item-Statistiken je Werkzeug/Ausrüstung tracken, Vorbild Mod „Item Stats Tracker“ (deren Stärken übernehmen, Schwächen verbessern). Beispiele: Spitzhacke – abgebaute Blöcke gesamt und je Blockart; Kills gesamt, je Mob und je Mob-Variante (für jedes Werkzeug/jede Waffe); Elytra – geflogene Strecke; Stiefel – gelaufene Strecke; weitere sinnvolle Werte je Item-Art. Anzeige: Tooltip-Kurzvorschau, Details bei Shift (o. ä.). Konzept klärt: welches Modul (eigenes Sub-Mod vs. bestehendes), Speicherung als Item-Komponente (Größe begrenzen, Top-N je Liste), Performance, Multiplayer, Zurücksetzen, Datenschutz/Anzeige für andere Spieler, Vergleich mit Item Stats Tracker.

## Nachtrag 61 (2026-10-15, Besitzer-Antworten Sammel-Fragebogen 13.10.)
Entscheidungen (E): alle Empfehlungen angenommen (E1 A Sprachen erst beim Release, E2 A Attractor-Ladung gestrichen, E3 A gestrichen, E4–E6 geschlossen, E9 A Echolot 3 s halten, E10 A Schlagzahlen so lassen, E11 A Trick-Spiegel vorerst entfernen, E12 A Gewölbe-Texturen nur zur Build-Zeit), außer:
- [x] E7 Rucksack-Sortierung: KEINE eigene Sortierfunktion – Sortieren bleibt Mods wie Mouse Tweaks / Client-Sort überlassen (Kompatibilität nicht brechen).
- [x] (oc-answers61) E8 Hammer-Tempo bleibt (A); NEU: Oktant-Auswahl bekommt denselben Mengenrabatt wie der normale Hammer-Flächenabbau (1. Block 1,5x, Block 2–9 +0,8x, ab 10 +0,7x – statt fester Faktor je Block).
- [x] (oc-answers61) E11 Trick-Spiegel vorerst entfernen: aus Loot/Rezept/Kreativ-Reiter nehmen, Code bleibt hinter Schalter.
- [x] (oc-answers61) E13 Weder Pipette noch Malerpalette (paint_palette): Malerpalette-Item entfernen (Alt-ID-Migration). Zufallsfarbe nur über die Verzauberung „Farbpalette“ (Color Palette) – anwendbar auf den Pinsel, nicht auf eine Palette.
Texturen (T): T1 Fragentisch R3 bleibt; T6 = Keramik B; T7 Lohen-Obsidian B1 ok; T8 Trainingspuppe bleibt vorerst; T10 ok (abgehakt); T11 lassen; T13–T26 abgenommen (nur eine Variante – Änderungen meldet der Besitzer selbst); T15 Enchanter ok. T2 (Kolben C), T5 (Riding-Bücher B+25 %), T4-Stil (Sandwich v8) waren schon früher entschieden und eingebaut – Fragebogen war veraltet.
- [x] (oc-answers61) T12 Starkes Leuchtfeuer: GUI im Vanilla-Stil wie die Vanilla-Leuchtfeuer-GUI (nicht eigener Container-Stil).
- [x] T3 Brot-Form: v8 bleibt (keine Form A–D) (Besitzer 15.10.).
- [x] (oc-breadrim) Sandwich-Brot v8: Unterseite bekommt einen dunkleren Rand wie die Oberseite (Besitzer 15.10.).
- [x] (oc-cheese) Butter/Käse neu (Besitzer 15.10., ersetzt alte Regel): Milch im Kessel reift 5 min zu Butter, weitere 5 min zu Käse (kein fermentiertes Spinnenauge mehr nötig). Nichts verdirbt (kein Erntefenster). Liegt ein aktiver (nicht per Redstone gesperrter) Trichter unter dem Kessel, erntet er das fertige Produkt sofort (Butter; Käse entsteht also nur ohne aktiven Trichter). Auslegung „Butter bzw. Milch“ im Diktat: Trichter erntet das jeweils fertige Produkt.
REGEL (Besitzer 15.10.): Keine alten, schon entschiedenen Textur-Vorschläge erneut vorlegen oder vermischen; vor jeder Textur-Frage Queue/Archiv/Memory nach der Entscheidung durchsuchen. Eingebaute, abgenommene Texturen nur auf ausdrücklichen Wunsch ändern.

## Nachtrag 60 (2026-10-15, Besitzer-Antworten zum Audit N58)
- [x] Eimer: bleibt so, wie er vorher drin war – KEIN breiterer Eimer (Audit-Frage 1).
- [x] Elytra-Reparatur: bleibt beim Werkbank-Rezept, keine Amboss-Reparatur (Audit-Frage 2).
- [x] (oc-answers60) Kartenaufdeckung (Simple Maps revealRadius): Standard auf den Vanilla-Wert setzen (Audit-Frage 3).
- [x] (oc-answers60) Kupfer-Eimer verwittert auch platziert. Mit Honigwabe wachsen → verwittert nicht. Wird etwas eingefüllt, verwittert ein gewachster Eimer halb so schnell, bis er wieder gewachst wird (Audit-Frage 4; Auslegung: Einfüllen „beschädigt“ das Wachs teilweise).
- [x] (oc-answers60) Wollknäuel: zusätzlich 2 Wollknäuel → 1 Wolle (Audit-Frage 5).
- [x] (oc-answers60) Blaze Book: Rezept 8 Lohenstaub (Blaze Powder) + 1 normales Buch (Audit-Frage 6).
- [x] (oc-answers60) Soulbound im Hardcore abschalten; Items bleiben trotzdem damit verzauberbar (wirkungslos wie Bett/Respawn-Anker im Hardcore) – Tooltip/Hinweis im Hardcore (Audit-Frage 7).
- [x] Hufeisen-Upgrade-Tooltip bleibt „Copper–Enderite“ Hufeisen (Audit-Frage 8).

## Nachtrag 59 (2026-10-14, Besitzer)
- [x] (oc-enchants) Neue Verzauberung „Step Height“ (Stufenhöhe) für Stiefel, 2 Stufen: I = 1 Block, II = 1,5 Blöcke (Zäune). Treasure-Verzauberung.
- [x] (oc-enchants) Neue Verzauberung „Sprint“ für Hosen, 3 Stufen: relative Tempo-Steigerung auf die Grund-Laufgeschwindigkeit (wie Swift Sneak beim Schleichen). Treasure-Verzauberung.
- [x] (oc-enchants) Beide Verzauberungen überall passend integrieren: Loot-Pools (Treasure: Truhen-Loot, Bibliothekar-Handel nach Vanilla-Treasure-Regeln), Tags (exclusive/tradeable/in_enchanting_table nach Treasure-Regel), Unverträglichkeiten, Astral-/Enchanter-Kompatibilität, Lang EN+DE, Wiki/Guide, Tests.
- [x] (oc-wanderers) Dorf-Wanderer: weiterer, umtexturierter Wandering Trader mit einem Pferd an der Leine; genau einer pro Dorf (spawnt im/am Dorf). Gegenstück zum Wandering Trader: bietet seltene, aber nicht so seltene Items wie dieser; Angebot wechselt alle 3 Tage, Restock etwa alle 1,5 Tage. Trades zwischen Wandering Trader und Dorf-Wanderer passend aufteilen (Konzept zuerst: Trade-Listen, Spawn-Regel, Pferd, Textur). – Konzept docs/ai/KONZEPT-DORF-WANDERER-2026-10-15.md (oc-wanderer-concept), 11 Besitzer-Fragen offen
- [ ] (ZUM ENDE der Mod-Entwicklung) Audit auf Vanilla-Konformität (Stil, Balancing, Mechaniken, UI, Sounds, Texturen) über alle Mods.
## Nachtrag 58 (2026-10-14, Audit vergessener Besitzer-Wünsche)
Quelle und Belege: `docs/ai/AUDIT-BESITZER-WUENSCHE-2026-10-14.md` (Stand claude-wave1 3cfba3c00, nur gelesen, nichts im Spiel geprüft). Datum in Klammern = Besitzer-Nachricht.
Nicht notiert und nicht gebaut (D):
- [x] (Besitzer 09.10.) Breiterer Eimer: Eimer mit größerer Öffnung als Vorschlag, dazu ein ganz neu gezeichneter breiterer Eimer (Vorschau 16-fach, alt neben neu); bisher nirgends notiert, kein Code. (Besitzer 15.10.: Eimer bleibt wie vorher, kein breiter Eimer)
- [x] (Besitzer 14.10.) Bild vom 14.10. 19:31 (vermutlich Ash-Wood-Texturwahl nach den KI-Prompts): Inhalt beim Besitzer erfragen, dann umsetzen. (Besitzer 17.10.: egal, abgehakt)
- [x] (B, b-forgestack, 17.10.) (Besitzer 10.10.) Mod-Audits: offene Befunde aus `AUDIT-DATEN-/AUDIT-MODULE-2026-10-11.md` abarbeiten (Forge-Standalone-Ziel M-05, leere Tags D-10, doppelte Lang-Kopien D-11, Forge-Stapelkappung Fass/Truhe). – Teil erledigt (oc-n58-small): M-05, D-10, D-11; Forge-Stapelkappung Fass/Truhe: KEIN Befund (b-forgestack: tiered_chest_game_test 17/17 auf Fabric+NeoForge+Forge, Crucibles-Modul 68/68 je Loader inkl. Enderit-Fass 128; Plan docs/ai/PLAN-B-FORGESTACK-2026-10-17.md).
- [x] (Besitzer 09.10./11.10./14.10.) Dauerregeln dauerhaft in `docs/ai/WORKFLOW.md` festhalten: Entscheidungen immer als Auswahl, Textur-Abnahmen mit Link und als Webseite, Fremdanbieter/opencode bei leerem Kontingent, öffentliches Repo ohne interne IPs/Hosts. – erledigt (oc-n58-small, WORKFLOW.md „Standing owner rules“).
- [x] (Besitzer 13.10.) Sammel-Dokument offener Entscheidungen (E1–E13, T1–T26, A1–A52; nur E8 beantwortet) liegt nur in der Memory (Übergabe 13.10. abends): Antworten des Besitzers einholen und hier eintragen. (Besitzer 15.10. beantwortet, siehe Nachtrag 61)
Entscheidung notiert, nicht umgesetzt (E):
- [x] (Besitzer 13.10.) Elytra-Reparatur am Amboss: Wortlaut „am Amboss“ widerspricht dem Vermerk „Werkbank-Rezept bleibt, nichts bauen“ (N11); beim Besitzer klären. (Besitzer 15.10.: Werkbank bleibt)
- [ ] (Besitzer 09.10., später) Astralit-/Nihilit-Fliesen als Boden (nur im Archiv als „später“ vermerkt).
Abgehakt, aber nicht oder nur teilweise im Code (C):
- [x] Nacharbeit (07.10.): Wegpunkt-Icon der Wayfinder-Karte kann nur 7 Vanilla-Köpfe; Köpfe aus simplemobheads in den Tag `waypoint_heads` aufnehmen. (oc-auditfix)
- [x] Nacharbeit (07.10.): Kreativ-Tab-Schalter je Super-/Sub-Mod fehlt in Modulen ohne `CreativeTabSettings` (z. B. simplemaps; vorhanden nur in 8 Modulen + SB). (oc-auditfix)
- [x] Nacharbeit (07.10.): Karten-Aufdeckreichweite „wie Vanilla“: Standard `MapsConfig.revealRadius` ist 96, Vanilla-Wert prüfen und angleichen oder Abweichung bestätigen. (oc-answers60)
- [x] Nacharbeit (07.10.): End-Rudel 3–5 gewünscht, Nibblemite spawnt in 2–4; angleichen oder Abweichung bestätigen. (oc-auditfix)
- [x] Nacharbeit (09.10.): Astral-Enchanter-Abbau soll Tisch plus Enderit-BARREN zurückgeben (Code: Nugget). (oc-auditfix)
- [x] (B, b-n58rest2, d9a4d7bb2) Nacharbeit (11.10.): End-Schiffswracks ca. 80 % vergraben (Code: Starthöhe −8 mit Endsteinhügel; Anteil nicht belegt) – prüfen. – GROUND 9 = 79 % des Schiffs unter Boden (vorher 10 = 94 %), Generator prüft 75-85 %.
- [x] Nacharbeit (11.10.): Platzierter Kupfer-Eimer soll verwittern können (Code: nur Tragen in Hotbar/Nebenhand); `jei.simplebuilding.info.copper_bucket` sagt noch „jedes Ausgießen oxidiert“ – Text korrigieren. (oc-answers60)
- [x] Nacharbeit (12.10.): Wollknäuel zu Wollblock (2 zu 1): Rückrichtung fehlt (vorhanden nur Wolle → 2 Knäuel); Besitzerwortlaut mehrdeutig, Rückfrage. (oc-answers60)
- [x] Nacharbeit (12.10.): Blaze-Book-Rezept „8 Lohenstaub + Buch“ ist nicht umgesetzt (Code: 3 Papier + 1 Lohenstaub → 3 Bücher); Rezept ändern oder Besitzer bestätigen lassen. (oc-answers60)
- [x] Nacharbeit (12.10.): Soulbound im Hardcore-Modus (Besitzersatz abgebrochen) – nachfragen; bisher nur Wiki-Satz. (oc-answers60)
- [x] Nacharbeit (13.10.): Hufeisen-Upgrade-Tooltip nennt „Copper–Enderite“, Besitzer wollte „Iron–Enderite ingots“ – klären. (Besitzer 15.10.: Copper–Enderite bleibt)
- [x] Nacharbeit (14.10.): Shrieker mit Silent Dandelion: nur Ton stumm (85b334cbf), Vibration/Warnstufe bleiben – prüfen, ob Sculk-Sensoren trotzdem auslösen, ggf. Vibration unterdrücken. (oc-auditfix: stumm = komplett untätig)
- [x] Nacharbeit (Memory 12.–13.10., abgehakt aber Teilpunkt offen): Simple Respawn roter Name mit Restzeit + Locator-Markierung; Schwefel-Fackel/-Laterne/-Lagerfeuer (Folgepunkt Schwefelfeuer); Entdeckbarkeit Stufe 2 (Welt-Hinweise). (Respawn via oc-auditfix; Schwefel-Fackel/-Laterne/-Lagerfeuer via claude-n58rest, Textur-Abnahme offen; Entdeckbarkeit Stufe 2 offen – Besitzerfragen)
Aus den Memory-Notizen (nicht in der Queue, Details im Audit-Dokument M02–M18):
- [x] (B, b-claimreview, 17.10.; Plan docs/ai/PLAN-B-CLAIMREVIEW-2026-10-17.md) (Memory) Claim-Urkunde Phase 1: Review, Forge-/Client-/Standalone-Tests, Einzelhooks, Namens-Cache; rote Tests ohne Fix-Beleg (Forge crucible/auto_smither/tiered, Standalone double_cap/claims_grief, every_mod_mob NeoForge) prüfen. -- Beleg: Luecken Truhen-Fahrzeuge (A reichte) + Baumwuchs ueber Claimgrenze gefixt (je Gametest); simpletweaks:*claims* 65/65 auf Fabric/NeoForge/Forge + Standalone Fabric/NeoForge; Forge crucible 16/16, auto_smither 5/5, tiered 29/29; double_cap standalone 3/3; every_mod_mob NeoForge 1/1 (alle gruen, flaky nicht reproduziert).
- [ ] (Memory) Windows-Lauf des Installers beim Besitzer (winget-JDK, Toolchain, Vanilla-derived-Fallback) testen.
- [x] (B, b-trims3, bff18bdc2..bdd73c36b) (Memory) Simple Trims: JEI-Kategorie (oc-n58-small), Hammerneigung (PlateHint) + Ziel-Highlight (pulsierende Platte), Migration alter placed_smithing_template-Blöcke mit Besatzvorlage nach simpletrims. Merge offen; Client-Abnahme offen.
- [x] (B, b-moneyrest, 17.10.) (Memory) Simple Money: End-Holz-Items ohne Preise (Absicht klären); Simple Beacons: Client kennt Server-Config nicht, Strong-Conduit-Tönung; Mobheads-Reste (Varianten-Sounds, Wiki-Renders, alte Fun-Config-Keys). – Teil erledigt (oc-n58-small): Beacons-Client liest Server-Config vom Menü, Mobheads-Reste nur Doku; Rest offen. – ERLEDIGT auf Branch b-moneyrest (Plan docs/ai/PLAN-B-MONEYREST-2026-10-17.md): Money End-Holz-Preise Absicht belegt (simpleend/prices.json: Rohstoffe bepreist, `simpleend:*` bewusst ausgeschlossen, wie Vanilla-Holz) – keine Änderung; Strong-Conduit sternblau getönt (StrongConduitRenderer, Besitzer-Farbabnahme offen); Mobheads: Kopf trägt Tonsatz des Mobs (Katze royal, Schwein big/mini, Wolf 6 Sätze, Kuh moody, Huhn picky) als Notenblock-Klang, alte Fun-Keys ohne Code-Rest (adoptFunConfig); OFFEN: Mobheads-Wiki-Renders (brauchen Client).
- [x] (B, b-n58rest2, d9a4d7bb2) (Memory) Eisen-Tiegel-Bau: Loot-Bedingung `block_state_property(stage)` greift nicht (Umgehung per getDrops); Gemischte Octet-Zellen ohne Bruch-Risse, 26.2-Compile ungeprüft; End-Testzentrale: chunkweise fehlendes Blocklicht. – Teil erledigt (oc-n58-small): Ursache = Loot-Altformat, Rohling-Tabelle neu, getDrops-Umgehung entfernt; neuer Befund 21 Altformat-Tabellen (tools/check_loot_format.py). – b-n58rest2: alle 23 Loot-Altformat-Tabellen + Generatoren umgestellt (Prüfer 0, Baseline weg); Octet-Bruch-Risse im MixedOctetRenderer (Optik am Client ungeprüft); End-Testzentrale-Blocklicht: Ursache war Bau im End, durch N40-Sperre behoben (kein neuer Code); 26.2-Compile nicht geprüft.
- [ ] (Memory) Prod-Übernahme `development` → Production für alle gemergten Wellen erst nach Freigabe des Besitzers; `oc-gatefix6` ist in claude-wave1/master enthalten (B 17.10.).
Im Code vorhanden, aber bisher nirgends in der Queue notiert (D, hier nur nachgetragen):
- [x] (Besitzer 09.10.) Dev-Knopf Mod-UI ↔ Vanilla-UI (Audit 14.10.: UiStyleToggle/StyleToggleButton, Config showStyleToggle). Dev-Queue für Textur-/Code-Wünsche in der Zentrale (Audit 14.10.: tools/zentrale-web/devqueue.py, PLAN-DEV-QUEUE-2026-10-09).
- [x] (Besitzer 08.10./09.10./12.10.) Zentrale: Seite im Repo, Mod-Auswahlbaum, Downloads, Installer-Skript, Wiki-Texturseite (Audit 14.10.: tools/zentrale-web, modselect.py, collect_downloads.py, install.ps1, wiki/textures.py).
- [x] (Besitzer 11.10.) Tiegel-GUIs mit Vanilla-Rand (50d8b4d9c); Blaupausen-Layout Done/Help/Sign (b4e6029ea, c20081c3e); Howler-Textur Variante A (361cbf98d) (Audit 14.10.).
- [x] (Besitzer 11.10./12.10./13.10.) Drache härter je Wiederbeschwörung (7445c9473); Lohen-Obsidian dezenter + Enchanter-Tuch tiefer (0a4b8e90f); Schleichen baut einzelnes Octet ab (363db8097) (Audit 14.10.).

## Nachtrag 57 (2026-10-14, Besitzer)
- [x] (oc-lightpane) Neuer Block „Light Pane“ (Lichtscheibe): Dicke und Platzierbarkeit wie ein Itemrahmen (an jede Blockseite, flach). Varianten je Art: normal, transparent, einfarbig, mehrfarbig. Verhalten je Variante: Licht wie eine Lampe abgeben; den Block dahinter/darunter erhellen (Textur aufhellen o. ä.); eine Farbe abgeben (nicht grell, sondern texturiert); bzw. je nach Redstone-Stärke eine andere Farbe ausgeben – per Rechtsklick wie beim Notenblock einstellbar, welche Farbe bei Stärke 1 kommt (weiter reihum). Solide umsetzen, keine Bugs (Platzieren an allen Seiten, Abbau ohne Stütze, Licht-Updates, Multiloader, Tests).
- [x] (oc-shrieker) Sculk Shrieker mit der Silent Dandelion leiser/stumm stellbar (wie bei Mobs: Rechtsklick an/aus), gilt für alle Spieler, damit er bei Sculk-Sensoren nicht nervt.

## Nachtrag 56 (2026-10-14, Besitzer) – Shop-Vitrine + Verkaufsblock
- [x] (oc-shop-concept) KONZEPT zuerst (docs/ai/KONZEPT-SHOP-VITRINE-2026-10-14.md, Job oc-shop-concept): Grief- und Explosionsschutz, Duping-Sicherheit, Einbindung Simple Money/Claims.
- [x] (oc-shops) Shop-Vitrine: einfach craftbar (Eisennuggets, Glas, …), dazu Glow-Variante; ästhetisch/schön. Besitzer legt ein Item hinein → steht zum Verkauf; Besitzer kann es wieder herausnehmen. Mehrere Items hinein → funktioniert wie ein Verkaufsautomat (Vending Machine).
- [x] (oc-shops) Verkaufsblock: muss unter der Vitrine liegen, damit der Verkauf funktioniert (Diktat „damit der Bernard funktioniert“ – vermutlich „Verkauf/Automat“). Eigene GUI: Preis-Items oben wie beim Filter festlegen (Filter-Prinzip): Rechtsklick addiert die Stapelgröße des Cursor-Stacks, Linksklick addiert 1; Preis entfernen per Klick mit leerem Cursor; leerer Preis = gerade kein Verkauf. Befüllt man den mittleren Slot, erscheinen links und rechts je ein leerer Slot für weitere Preis-Items, max. 5 Slots. Preiszeile nur vom Besitzer bearbeitbar.
- [x] (oc-shops) Verkaufsblock-GUI für Käufer: darunter dieselbe Anzahl Slots; Käufer legt die Preis-Items hinein und tauscht per Button. Nur ein Spieler gleichzeitig in der GUI.
- [x] (oc-shops) Vitrine und Verkaufsblock: explosionsfest und für fremde Spieler nicht abbaubar (Grief-Schutz laut Konzept).

## Nachtrag 55 (2026-10-14, Besitzer)
- [x] (oc-n55-small) Sculk Jaws: spawnen nur mitten in Sculk – Bedingung: 5 Sculk-Blöcke rundherum unten, am unteren Block 4 (genaue Form beim Planen festlegen).
- [x] (oc-missingtex) Rucksack- und Bundle-Färbungen sind nicht korrekt – prüfen und fixen.
- [x] (oc-n55-hammer) Vorschlaghammer in der Welt: rissiger Diamantblock → 9 Schläge → Diamant-Pebbles; Diamantblock → 9 Schläge → Diamant-Pebbles.
- [x] (oc-n55-small) Echolot (Echo Sounder): besserer Fehlschlag-Sound, wenn nicht verbunden.
- [x] (oc-missingtex) Einige Items haben keine Textur mehr (graues Karomuster im Inventar) – alle finden und fixen (vgl. Gate-Rot cracked_ice_0, Job oc-gatefix2).
- [x] (oc-n55-hammer) Obsidian-Splitter nur noch per Vorschlaghammer-Umwandlung (analog Diamant-Pebble); dazu Weinender-Obsidian-Splitter und Blazing-Obsidian-Splitter neu.
- [x] (oc-bricks3d) Ziegel (Bricks) beim Platzieren wie Barren 3D legen; Diamanten und Smaragde bekommen ebenfalls eine 3D-Form. Mit dem Vorschlaghammer auf das platzierte 3D-Item hauen → 2D, nochmal → wieder 3D (nur unterstützte Items).
- [x] (oc-crates) Crates: seitlich bis zu 9 verschiedene enthaltene Items anzeigen. Inhalt wie das Besitzer-Referenzbild rendern (Bild liegt in dieser Sitzung NICHT vor – beim Besitzer anfordern): jedes Item bekommt je nach Anzahl eine Schicht, die aussieht, als bestünde sie aus dem Item; max. 9 verschiedene Items je Crate; zuletzt eingelegtes Item liegt oben, ältere Schichten werden hochgeschoben.
- [x] (oc-n55-small) Enchanter: am Buch-Button nicht die benötigten Level anzeigen (stehen schon oben), sondern wie viele Level abgezogen werden.
- [ ] Blazing Obsidian ist immer noch nicht gut → Rework (Konzept/Textur/Verhalten) notiert. (Besitzer 17.10.: stört Textur + Verhalten → Konzept mit Varianten, Texturvorschläge als Seite)
- [x] (oc-n55-small) Notenblock-Verstärker funktioniert nicht – dasselbe Prinzip wie beim (funktionierenden) Jukebox-Verstärker anwenden.
- [x] (oc-ashleaves) Aschholz-Blätter sollen wie Spinnennetze wirken (verlangsamen; Details beim Planen).
- [x] (oc-blueprint-io) Blaupause: Import-/Export-Button für andere Bau-Formate („block languages“) – Modal mit Kopieren-Button (Export) bzw. Einfügen-Button (Import).
- [x] (oc-n55-small) Hängematte: Einstellung wie playersSleepingPercentage (Anteil Spieler in Hängematte für Nachtsprung o. Ä.).
- [x] (oc-deathvault2) Death Vault umbauen (aktuelle Funktion nicht zuverlässig): überzählige Funktionen (Zurückkaufen usw.) als „zukünftig“ markieren/abschalten. Neue Kernfunktion: anzeigen, welche Items beim Tod gedroppt wurden, farbcodiert nach Verbleib – von anderem Spieler aufgehoben, verbrannt, despawnt, ins Void gefallen, anders verschwunden –, damit man sieht, ob sich der Weg lohnt. Noch liegende Items: Despawn-Timer (Fortschrittsbalken wie Ofen); steht der Timer, weil der Chunk nicht geladen ist, das anzeigen.
- [x] (oc-headslot) Kopf-Slot verhält sich wie der Helm-Slot für Köpfe: Kopf ersetzt mit Item-Model den Kopf, Effekte werden übernommen; Anzieh-Sounds; Item-Model des Kopf-Slots hat Vorrang vor dem Helm, Effekte beider gelten. Kopfgrößen möglichst gleich den Mob-Kopf-Größen an den Mob-Modellen.
- [ ] Hollow Howler gruseliger: Recherche Sounddesign (Horror-Audio, Vanilla-Mob-Sounds) und Visual Design, daraus Verbesserungsplan, dann umsetzen. – Konzept docs/ai/KONZEPT-HOLLOW-HOWLER-GRUSEL-2026-10-15.md (oc-research), Umsetzung nach Besitzer-Freigabe
- [ ] Recherche Mob-Animation und Vanilla-Animationsrichtlinien; alle Custom-Mob-Animationen danach verbessern. – Konzept docs/ai/KONZEPT-MOB-ANIMATIONEN-2026-10-15.md (oc-research), Umsetzung nach Besitzer-Freigabe. Teilstand: Schritt 1 (oc-anim-quick), 3 (claude-anim2), b-anim3 (Nibblemite/Dragonmite/Deceiver/Dread) und Watcher (b-watcheranim, docs/ai/PLAN-B-WATCHERANIM-2026-10-17.md) erledigt; offen nur Hollow Howler (Sitzung A, claude-howler3)
- [ ] (ZULETZT, erst wenn alles andere fertig) Recherche Performance-Optimierung (Minecraft-Mods, 3D-Block-/Open-World-Spiele, Videospiele allgemein) und alle Mods optimieren.

## Nachtrag 54 (2026-10-13, Besitzer)
- [x] (B, b-claimdeed45 21b5a0f4f, 17.10.; Phasen 4-5 grün, offen nur Merge, Client-/Textur-Abnahme) Claim-Urkunde ähnlich wie die Blaupause handhaben: eigene GUI, dort Personen hinzufügen und ihnen Fähigkeiten geben (bauen, zerstören, Container öffnen usw.); Besitzer: Code-Form (B Build, D Destroy, U Use …), dazu Claim-Tisch (Karte/Liste aller Claims), Claims in Simple Maps, Kopie auf leere Urkunde, Erweiterung mit steigenden Kosten, Vanilla-nah. (Plan Fassung 2 freigegeben) – Phase 1 (Rechte/Parser/Ledger v2/Hooks) in wave1 via oc-claimdeed1b; Phasen 2–5 offen – Phase 2 (Editor-GUI) via oc-claimdeed2, Phase 3 (Claim-Tisch) via claude-claimdeed3 – Textur/Client-Abnahme offen; offen Phasen 4–5 (Claim-Tisch, Maps-Ebene, Erweiterung, Texturen)
- [x] Hammer-Tempo: aktuell gut, bleibt so (Formel 1,5x / +0,8x / +0,7x; E8 = A).
- [ ] (SPÄTER, nur als Hinweis) Hammer-Tempo-Variante testen: Basis 1. Block 2x, jeder weitere bis Block 9 +0,6x, ab Block 10 +0,5x oder +0,4x – bei vielen Blöcken viel Ersparnis, bei wenigen wenig.,

- [x] Oktant-Abbau mit Hammer: 0,5x der Spitzhacken-Zeit (statt 2x) (claude-octant-speed)

## Nachtrag 53 (2026-10-13, Besitzer, Korrekturen zu N52)
- [x] Rissiges Eis: an keine Seite kann ein Block platziert werden (normales Eis schon); Silk Touch droppt es; Vorschlaghammer auf normales Eis → rissiges Eis. (claude-n53) (Audit 14.10.: CrackedIceBlock.blocksPlacementAt + BlockItemMixin, ChiselTests)
- [x] (claude-n53) Enchanter: Randlinie NICHT golden wie das Tuch, sondern dunkel (Obsidian-artig).
- [x] (claude-n53) Brücken-Redstone (Pulver/Draht): Textur wie vorher (alte Optik), aber als EINE fertige Bilddatei (umgefärbte Variante als PNG im Repo, nicht generiert, keine Schichten).
- [x] (claude-n53) Rissiges Eis: Texturen wie vorher; Problem war Schatten am Rand (nicht getarnt im Eis) → Schattierung/AO wie Vanilla-Eis, keine Randschatten.
- [x] (claude-n53) Rissiges Eis: dritte Generierungsart – sehr große Flächen in Eiszapfen-Biom (Ice Spikes) u. ä. Packeis-Gegenden; wer hineinläuft, bricht ein und muss Blöcke setzen, um sich zu retten.
- [x] (claude-n53) Rissiges Eis (Zusatz Besitzer): an keine Seite eines rissigen Eisblocks lässt sich ein Block setzen; Behutsamkeit droppt es selbst; Vorschlaghammer macht aus Eis rissiges Eis (weitere Schläge: Eissplitter).

## Nachtrag 52 (2026-10-13, Besitzer)
- [x] Astral-Verzauberer umbenennen in „Enchanter“ / „Verzauberer“ (Besitzer-Wahl). Seiten und Unterseite bekommen eine Randlinie bis zum Tuch; Unterseite aus Obsidian. (claude-n52) (Audit 14.10.: e56d64dd1, 2f277c94f)
- [x] Brücken-Redstone-Textur: Schichten z-fighten in der Höhe → einfache Textur ohne Überlagerung. (claude-n52) (Audit 14.10.: 719fcda7a, f6db5c094)
- [x] Dynamisches Licht auch für Fackel in der Hand (wie Laterne/Lava). (claude-n52) (Audit 14.10.: 8ff6e2571, TrimLight.heldLight + Test)
- [x] Rissiges Eis wird nicht richtig gerendert (Besitzer-Screenshot 13.10.: platzierter Block fast unsichtbar, nur Umriss sichtbar, Inhalt wirkt wie durchsichtig/fehlende Textur bzw. falscher Render-Layer). (claude-n52) (Audit 14.10.: HalfTransparentBlockMixin + NatureBlockTests; Optik nur per Client-Abnahme prüfbar)
- [x] (claude-gatefix16, in wave1) Rissiger Diamant + Vorschlaghammer In-World → 9 Diamant-Kiesel (nach Aufteilung 3d, da rissiger Diamant nach SimpleLib zieht).,
- [x] Astral-Verzauberer umbenennen in „Enchanter“ / „Verzauberer“ (Besitzer-Wahl). Seiten und Unterseite bekommen eine Randlinie bis zum Tuch; Unterseite aus Obsidian. (claude-n52)
- [x] Brücken-Redstone-Textur: Schichten z-fighten in der Höhe → einfache Textur ohne Überlagerung. (claude-n52)
- [x] Dynamisches Licht auch für Fackel in der Hand (wie Laterne/Lava). (claude-n52)
- [x] Rissiges Eis wird nicht richtig gerendert (Besitzer-Screenshot 13.10.: platzierter Block fast unsichtbar, nur Umriss sichtbar, Inhalt wirkt wie durchsichtig/fehlende Textur bzw. falscher Render-Layer). (claude-n52)
- [x] (oc-n55-hammer) Rissiger Diamant + Vorschlaghammer In-World → 9 Diamant-Kiesel (nach Aufteilung 3d, da rissiger Diamant nach SimpleLib zieht).

## Nachtrag 51 (2026-10-13, Besitzer)
- [x] Redstone-Kompatibilität: Astral-/Nihil-Geräte akzeptieren alle 3 Redstone-Arten + Brücken-Redstone (Connector); alle Redstone-Geräte (Vanilla + Mod) akzeptieren alle Mod-Redstone-Linien. Fehlende Blöcke ergänzen: Astral-Redstone-Block, Nihil-Redstone-Block (usw., analog Vanilla-Redstone-Block) und Brücken-Redstone-Block. (Audit 14.10.: bcaa3d26e, RedstoneLinesTests.everyDeviceTakes*)
- [x] (claude-astral-cloth, in wave1) Astral-Verzauberer Seitentextur: Tuch (Goldsaum) bis an den Rand ziehen.,
- [x] (claude-redstone-compat) Redstone-Kompatibilität: Astral-/Nihil-Geräte akzeptieren alle 3 Redstone-Arten + Brücken-Redstone (Connector); alle Redstone-Geräte (Vanilla + Mod) akzeptieren alle Mod-Redstone-Linien. Fehlende Blöcke ergänzen: Astral-Redstone-Block, Nihil-Redstone-Block (usw., analog Vanilla-Redstone-Block) und Brücken-Redstone-Block.
- [x] Astral-Verzauberer Seitentextur: Tuch (Goldsaum) bis an den Rand ziehen. (Audit 14.10.: e059f6a5b)
- [x] (claude-crucible-build) Eisen-Tiegel In-World-Bau: Tiegel + 16 Eisenbarren; jede Seitenplatte 2 Barren (erst halbe Platte, dann ganze, jeder Schritt sichtbar) = 8; jeder Griff 2 Schläge/Barren = 12; letzte 4 für die Wölbungen am oberen Rand (je Schlag eine). Jedes weitere Upgrade: 12 Schläge, alle 3 Schläge 1 Material (4 rissige Diamanten / 4 Netherit-Nuggets / 4 Enderit-Nuggets), jeder Schritt sichtbar.
- [x] (claude-crafter-ui) Crafter-, Smither-, Auto-Crafter-UIs verbessern: Pfeil immer als gepunkteter Redstone-Pfeil (vorhandene Textur aus den Redstone-Linien); Filter beim Auto-Crafter über den Pfeil (nicht gedrängt).

## Nachtrag 50 (2026-10-13, Besitzer)
- [x] End-Biome mit dichtem Nebel; Randbiom + Kern, im Kern spawnen Hollow Watcher (Tag simpleend:watcher_core) – gruseliger Bereich. (claude-endbiome2 + claude-n50-small) (Audit 14.10.: EndBiomes.WATCHER_CORE, Biom-JSON fog_*, EndTests)
- [x] Zittern aktiv, solange Furcht-Mob dich ansieht/getriggert ist; 3–5 Stufen nach Nähe. Watcher besser getarnt. (claude-n50-small)
- [x] Sculk Jaws spawnen natürlich im Deep Dark. (claude-n50-small)
- [x] Fragentisch-Rezept: unten Mitte zusätzlich Obsidian. (claude-n50-small)
- [x] (claude-wither2) Verdorrter Wither: Beschwörung per Netherit-Helm auf dem MITTLEREN Schädel (vor dem dritten Schädel aufsetzen) statt Netheritblock; zweite, stärkere Stufe mit Enderit-Helm; bei wenig Leben stärkere Witherskelette (mit Rüstung); Rückstoß unter 15 % stark; Drops: Witherherz, 3–5 Netherit- bzw. Enderit-Nuggets, 1–2 Nethersterne. (claude-wither2)
- [x] (claude-beacons) Netherstern + neue Upgrade-Vorlage („Strong Upgrade“) auf Leuchtfeuer: 6 weitere Effekte, 2 Ebenen größer baubar, entsprechend mehr Reichweite; dasselbe für Conduit soweit möglich. (Konzept fertig, alle Empfehlungen übernommen 13.10.; umgesetzt als Sub-Mod simplebeacons, Plan docs/ai/PLAN-SIMPLE-BEACONS-2026-10-13.md; Merge + Client-Abnahme offen)
- [x] Upgrade-Vorlagen-Tooltips: Hufeisen-Upgrade „applies to: Iron–Enderite ingots“; Enderit-Upgrade „Netherite equipment and blocks“ (+ Diamant-Ausrüstung/-Blöcke). (claude-n50-small)
- [x] Mob-Köpfe in neues Sub-Mod „Simple Mobheads“, alle Mob-Köpfe implementieren; SimpleBuilding requires es; aus Simple Fun entfernen. (`claude-mobheads`, Plan docs/ai/PLAN-SIMPLE-MOBHEADS-2026-10-13.md; Merge nach claude-wave1 und Besitzer-Abnahme offen)
- [x] End-Biome: seltener, viel größer; Grasdichte höher; Stöcke-Bäume wie normale Bäume aus Stöcken, gigantisch; Stamm abbauen → alle Stöcke brechen; im End keine Holzbäume, nur Stöcke-Bäume + Drift-Frucht; Oberwelt-gepflanzte End-Bäume gigantisch, 4×4 → größter Baum im Spiel; Setzling-Rückgabe ~0,8 je Baum (Oberwelt), im End ~1,5 je Stöcke-Baum; Früchte gedeihen nur im End, in der Oberwelt verdorbene Variante (Drifting-Effekt); Blätter durchsichtig/exotisch, kein Vollblock. (claude-endbiome2) (Audit 14.10.: PLAN-SIMPLE-END Umbau 13.10., EndTrees, claude-endbiome2)
- [x] Hollow Watcher teleportiert sich 1 s nach einem Treffer weg. (claude-n50-small)
- [x] Erhöhte Schwerkraft (Gravity-Effekt): mehr Fallschaden (Federfall mit einrechnen), „High Gravity“ gerecht. (claude-n50-small)
- [x] Resonanz-Button: Cap-Zeile im Tooltip weg; Herz-Texturen überarbeiten (richtig aussehen); Button-Textur wie ein deaktivierter Slot. (claude-n50-small),
- [x] End-Biome: seltener, viel größer; Grasdichte höher; Stöcke-Bäume wie normale Bäume aus Stöcken, gigantisch; Stamm abbauen → alle Stöcke brechen; im End keine Holzbäume, nur Stöcke-Bäume + Drift-Frucht; Oberwelt-gepflanzte End-Bäume gigantisch, 4×4 → größter Baum im Spiel; Setzling-Rückgabe ~0,8 je Baum (Oberwelt), im End ~1,5 je Stöcke-Baum; Früchte gedeihen nur im End, in der Oberwelt verdorbene Variante (Drifting-Effekt); Blätter durchsichtig/exotisch, kein Vollblock. (claude-endbiome2: Plan PLAN-SIMPLE-END „Umbau 13.10.“; + Kern/Saum-Biome mit dichtem Nebel, Tag simpleend:watcher_core für Hollow Watcher)
- [x] Hollow Watcher teleportiert sich 1 s nach einem Treffer weg. (claude-n50-small) (Audit 14.10.: FearLogic.HIT_TELEPORT_TICKS, FearTests, 6a0f98fd8)
- [x] Erhöhte Schwerkraft (Gravity-Effekt): mehr Fallschaden (Federfall mit einrechnen), „High Gravity“ gerecht. (claude-n50-small) (Audit 14.10.: HighGravityEffect, 6a0f98fd8)
- [x] Resonanz-Button: Cap-Zeile im Tooltip weg; Herz-Texturen überarbeiten (richtig aussehen); Button-Textur wie ein deaktivierter Slot. (claude-n50-small) (Audit 14.10.: 6a0f98fd8, resonance_heart_*, TrimWiringTests)

## Nachtrag 49 (2026-10-12, Besitzer)
- [x] (claude-n49-small) Seelen-Lohe (Soul Blaze) schießt Seelenfeuer-Kugeln (Soulfire Charge, craftbar mit Seelen-Lohenstaub); Treffer gibt Seelenbrand-Effekt.
- [x] (claude-deceiver3, in wave1) Deceiver-Umhang: Kopf-Umhang mit Kapuze, liegt als 3D-Modell auf dem Rücken (ähnlich Elytra); mit Elytra: Elytra darunter gerendert, beim Fliegen ragen die Flügel seitlich heraus, Umhang bleibt auf dem Rücken. Deceiver-Mob trägt ähnlichen Umhang. (claude-deceiver3)
- [x] (claude-n49-inv, in wave1) Extra-Kopf-Slot im Inventar (B-Menü) über dem Nebenhand-Slot, auf Höhe des Helm-Slots: nur Mob-Köpfe oder geschnitzter Kürbis; deren Effekte gelten; Helm-Positionierung je Kopf anpassen (Kürbis, Silberfisch-Kopf usw.: Helm sitzt richtig, sichtbar, kein Z-Fighting).
- [x] Resonanz-Button: zwischen neuem Kopf-Slot und Nebenhand-Slot, slotgroß, nur Herz (Hover); Stufen Cobble, Stone, Copper, Iron, Gold, Diamond, Netherite, Enderite mit je eigenem Herz-Indikator. Tooltip ohne Fettschrift: Zeile 1 „Resonance: x“ orange (uncommon), dann „(max 2.0x)“ grau; Rest analog, erster Buchstabe fett, L→E (E, S, C); Cap-Zeile analog, letzte Zeile und „the better of …“-Zeile streichen; S = Distanz+Zeit, je max 0.5. (Audit 14.10.: TrimStatsLayout.heartSprite, claude-n49-inv)
- [x] Inventar-Helligkeit als Config: Light (aktuell), Dark, Dawn (zwischen Dark und Light). (Audit 14.10.: UiBrightness (simplelib), Option inventoryBrightness)
- [x] (claude-n49-small) Geschnitztes Holz (carved logs): Item-Modell entspricht nicht dem Block-Modell → angleichen.
- [x] (claude-n49-small) Shellker-Helm und Schildkrötenpanzer trimmbar (beide waren schon über #head_armor in #trimmable_armor; Shellker jetzt explizit im Tag + Test, Schildkröte hatte Icon-Muster schon).,
- [x] Extra-Kopf-Slot im Inventar (B-Menü) über dem Nebenhand-Slot, auf Höhe des Helm-Slots: nur Mob-Köpfe oder geschnitzter Kürbis; deren Effekte gelten; Helm-Positionierung je Kopf anpassen (Kürbis, Silberfisch-Kopf usw.: Helm sitzt richtig, sichtbar, kein Z-Fighting). (claude-n49-inv: Slot 46 über Nebenhand, auch Rucksack/Kreativ; Effekte Kürbis/Mob-Sicht/Kopf-Fähigkeiten; Helm je Kopf skaliert, Silberfisch oben drauf; Client-Abnahme offen)
- [x] Resonanz-Button: zwischen neuem Kopf-Slot und Nebenhand-Slot, slotgroß, nur Herz (Hover); Stufen Cobble, Stone, Copper, Iron, Gold, Diamond, Netherite, Enderite mit je eigenem Herz-Indikator. Tooltip ohne Fettschrift: Zeile 1 „Resonance: x“ orange (uncommon), dann „(max 2.0x)“ grau; Rest analog, erster Buchstabe fett, L→E (E, S, C); Cap-Zeile analog, letzte Zeile und „the better of …“-Zeile streichen; S = Distanz+Zeit, je max 0.5. (claude-n49-inv: 8 Herz-Stufen, Tooltip neu, S als Summe)
- [x] Inventar-Helligkeit als Config: Light (aktuell), Dark, Dawn (zwischen Dark und Light). (claude-n49-inv: SimpleLib UiBrightness, Einstellung im Simple-Interfaces-Bildschirm)
- [x] Geschnitztes Holz (carved logs): Item-Modell entspricht nicht dem Block-Modell → angleichen. (Audit 14.10.: 1c6a5fecf, 34ac9d9cb)
- [x] Shellker-Helm und Schildkrötenpanzer trimmbar. (Audit 14.10.: Tag trimmable_armor, 1c6a5fecf)

## Nachtrag 48 (2026-10-12, Besitzer)
- [x] Wayfinder-Karte: Zoom feiner abstufen, Navigation verbessern. (claude-maps-zoom: Stufen 1,2,4,8,16,32,64, Pfeile/WASD, Home/C, 1-8)
- [x] Octet-Blöcke untereinander kombinierbar (verschiedene Materialien in einer Zelle). (Audit 14.10.: MixedOctetBlock, OctetMixTests, aaa8f3937)
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

- [x] Umsetzung N69: Hushed Deepslate Tiles (zuerst), Sculk Deepslate (nur herstellbar + Ancient-City-Loot), fehlende Treppe/Stufe/Mauer + rissig/bemoost laut Konzept §3/§6. (claude-missingblocks; Besitzer-Abnahme offen)
### Besitzer 17.10. – Simple Tweaks als Server-Tweaks-Mod
- [x] Simple Tweaks um Server-Tweaks erweitern: Ideenliste (34 Punkte T1–T34) liegt dem Besitzer als Seite vor (https://claude.ai/artifact/EbuYc7eRm1QhdqaC4oAPfa); nach seiner Auswahl (ja/vielleicht/nein) planen und umsetzen, jede Idee mit eigenem Server-Config-Schalter. (Auswahl 17.10. erhalten)
- [x] (A, claude-tweaks-admin, gemergt 17.10.) Simple Tweaks Teil 1 Admin/Schutz: T10 Block-/Truhen-Protokoll, T11 Inventar-Sicherung beim Tod, T13 /invsee+/endersee (eigene sichtbare UI, Admin kann Items nehmen; Vorbild invsee/InvRestore auf Modrinth, nur Idee, eigener Code), T14 Vanish, T15 Freeze, T16 Neustart-Ankündigung, T17 Auto-Backups, T18 Wartungsmodus, T34 Lag-Finder. Default an: T10 T11 T13–T18 und T34 (Besitzer 17.10.: nur-auf-Befehl-Features sind an; /lag misst nur während des Befehls).
- [x] (A, claude-tweaks-play, gemergt 17.10.) Simple Tweaks Teil 2 Spieler/Welt/Events: T4 Koordinaten teilen, T5 Todes-Hinweis, T6 Erwähnungen, T7 Spielzeit//seen, T8 Mob-Griefing je Mob, T9 PvP je Spieler, T19 Willkommen/Ankündigungen, T20 Login-Belohnung, T21 Server-Events, T22 Bestenliste, T23 Abstimmungen (Tag/Nacht-Abstimmung default aus), T24 AFK, T25 Weltgrenze je Dimension, T26 Nether-Dach, T27 Teil-Keep-Inventory, T28 Drachenei je Kampf, T30 Phantome je Spieler, T31 Entity-Grenze je Chunk, T32 Bodenitems mit Ansage, T33 Chunk-Vorgenerierung. Default an: T4 T5 T6 T7 T9 T23 T24 T25 T27 T28; alle anderen aus.
- Regel (Besitzer 17.10.): alles, was nur per Befehl wirkt, ist standardmäßig AN (T33 /pregen, T34 /lag, T21 Events per Befehl, T22 Bestenliste per Befehl, T30 /phantoms); automatische Teile wie angegeben.
- [ ] (Besitzer 17.10., vielleicht) T12 Neue-Spieler-Schutz. Nein: T1 /home, T2 /tpa, T3 Warps, T29 End-Reset.
- [x] (Besitzer 17.10.) Entscheidungen: Naturstufen (Erde, Gras, Sand, Kies) ohne Octets; Lichtscheiben + Schwefellichter bleiben in SimpleBuilding (kein Simple Blocks), Sub-Mod-Frage später.
- [x] (A, claude-formsaege, gemergt 18.10.; Besitzer-Abnahme offen) Fortgeschrittene Säge („Formsäge“, Name zur Wahl) in Simple Blocks: ersetzt die Vanilla-Steinsäge in der Rolle, schneidet Stein UND Holz (fehlende Holz-Schnittrezepte) und alle Blöcke mit Treppen/Stufen/Octets, die die Steinsäge nicht kann; Rezept ähnlich Vanilla, aber abgehoben; Textur nah an Vanilla, Nutzen sichtbar – Varianten als Seite zur Wahl.

### Besitzer 17.10. (spät)
- [x] Admin-Fenster (Simple Tweaks /invsee, Todes-Sicherungen) abgenommen.
- [x] Fehlende-Textur-Item aus Bild 2: Bild nicht nachlieferbar → abgehakt; stattdessen eigener Audit (nächster Punkt).
- [x] (B, b-colorbrush, 18.10., Color-Brush-Fix + Screenshot-Audit per xvfb) Textur-Audit per Client-Screenshots aller Kreativtabs + Inventar (fehlende Texturen/Modelle finden, z. B. Enderit-Truhe o. ä.); sicher kaputt: SimpleBuilding Color Brush (ColorBrushItem/BrushInkModelProperty) → fixen. [b-colorbrush 18.10.: Brush per xvfb-Screenshot in allen Tinten (none/rot/weiss/schwarz/Palette) korrekt, Brush-Tests 22/22; Fehlursache nicht reproduzierbar. Statik: check_item_properties.py neu, check_missing_textures + diese in gradle check; audit-models-263 0 Fehler (1184 Items); SB_TABSHOTS=1 liefert 36 Tab-Screenshots. Offen: Tabs nur 1. Seite, NeoForge-/Forge-Client-Sicht, Brush-Schwenkanimation fehlt (Vanilla use_cycle)]
- [x] (B, b-brushanim, 18.10., umgesetzt: select(brush_ink) -> je Tinte range_dispatch use_cycle -> color_brush[_tip|_palette]_brushing_0..2, Parent item/brush_brushing_N; offen Client-Sicht) (Fund b-colorbrush) Farbpinsel: Vanilla-Schwenkanimation beim Benutzen fehlt (`use_cycle`, `brush_brushing_0..2` wie Vanilla-Pinsel) → `ModModelProvider.generateColorBrush` + mc26_3/generated.
- [x] (B, b-octetrule, 18.10.) Polierter Endstein: Treppe, Stufe, Mauer (simplelib, bei der Materialgruppe) + Octet (simpleoctets) – nach Merge der Octets-Stufe. **Erledigt 18.10. (b-octetrule):** `simplelib:polished_end_stone_stairs/_slab/_wall` (Werkbank + Steinsäge, Loot, Tags, Lang) + `simpleoctets:polished_end_stone_octet`; offen nur Client-Sicht.
- [x] (A, claude-tweaks-cmd, gemergt 18.10.) Alle Simple-Tweaks-Befehle zusätzlich unter /simpletweaks <befehl> (+ /simpletweaks help).
- [x] (A, claude-formsaege, gemergt 18.10.; Besitzer-Abnahme offen) Formsäge-Änderung: = Steinsäge retexturiert mit Kupfer-Highlights (Sockel + Sägeblatt), Rezept mit Kupfer; Upgrade einer platzierten Steinsäge: ohne SimpleBuilding mit Axt, mit SimpleBuilding mit Hammer + 4 Eisennuggets.
- [ ] sb-test-Platte vergrößern (Besitzer auf suleiman: pct resize 103 rootfs +NG, wenn Speicher frei), sonst nur bereinigen.
- [x] (A, claude-octets2, gemergt 18.10.) (Besitzer 17.10.) Octets (Stein + Holz) und alle Holzvarianten EXKLUSIV in der Formsäge; Vanilla-Steinsäge bleibt Vanilla. Nach Merge der Octets-Stufe: Octet-Rezepte → simpleblocks:sawing, Fallback Steinsäge nur ohne Simple Blocks (Modul-Unabhängigkeit).
- [x] (A, claude-tweaks-anticheat, gemergt 18.10.) Simple Tweaks Schutz: Anti-X-Ray (serverseitig Erze verschleiern, wirkt auch gegen X-Ray-Texturpakete) – Standard AN; Freecam-Regel „nicht durch Blöcke“ (Freecam fliegen erlaubt, Durchfliegen verboten, wenn Admin es einschaltet) – Standard AUS; weitere Schutzideen (z. B. Seed-Verschleierung) als Schalter, Standard AUS.
- [x] (Besitzer 17.10.) Simple Optimization Konzept: alle Empfehlungen übernommen; Benchmarks auf sb-test statt Besitzer-PC (KONZEPT §9). Umsetzung Stufen (e1)–(e4) freigegeben.
- [x] (B, b-autooff, 18.10., Beleg: Plan docs/ai/PLAN-B-AUTOOFF-2026-10-18.md, GameTest protect_game_test_auto_off_with_partner_mods, Fabric+NeoForge 141/141; Forge nur Compile; Merge+Gate offen) (Plan Schutz 17.10.) Simple Tweaks: Feature automatisch aus, wenn ein Partner-Mod aus dem Modpack geladen ist (Schalter `autoOffWith`, Modliste beim Start, Log-Hinweis): Ledger ↔ T10 Block-Protokoll, InvRestore/Inv View ↔ T11/T13, Chunky ↔ T33 Pregen, Freecam ↔ Freecam-Regel (nur Hinweis), Litematica/Axiom ↔ actionChecks/Anti-X-Ray. Details `docs/ai/PLAN-TWEAKS-PROTECT-2026-10-17.md`.
- [x] (A, claude-feinschliff, gemergt 18.10.; Besitzer 19.10.: abgenommen) Formsäge-Textur: Kupfer-Pixel mit weichem Übergang in die Vanilla-Pixel; Soulbound und Fluch des Verschwindens gegenseitig exklusiv.
- [x] (A, claude-howler5, gemergt 18.10.; Besitzer 19.10.: Empfehlung übernommen → mode mix) Howler-Sounds: je Ereignis 3–5 ähnliche Varianten (Vanilla-Stil), A und B, Vorschlag je Ereignis, Wahl per Build-Schalter; Ziel: sehr Vanilla-nah.
- [x] (A, claude-octets2, gemergt 18.10.) Fehlende Octet-Familien nach INVENTAR-OCTETS (15 Bretter-Arten inkl. Aschholz/Dämmerholz + 4 Vanilla-Stufen-Familien; Naturstufen ausgenommen).
- [x] (A, claude-opt1, gemergt 19.10.; 2. Messreihe offen) Simple Optimization Stufe 1: Gerüst simpleoptimization + 3 Sub-Mods, Kompat-Erkennung (auto/ours/theirs/off), Benchmark-Harness auf sb-test; erste Teile laut KONZEPT §6/§9.
- [x] (Fund claude-octets2 18.10., behoben claude-gatefix16) Simple Blocks: eigene sawing-Rezepte nutzen `forge:conditions` (Forge 26.3 liest nur ein `forge:condition`, mehrere per `forge:and`) → laden auf Forge auch ohne Partner-Mod; Fix in modules/simpleblocks/tools/generate_data.py wie im Octet-Generator.

### Besitzer 19.10. – neue geplante Umsetzungen (je erst Konzept, dann umsetzen)
- [~] (B, b-concepts19, 19.10., Konzept) Better Item Frames (Ideen übernehmen): Rahmen unsichtbar/fest, Items drehen, … – Konzept, Zielmod festlegen. Konzept: docs/ai/KONZEPT-BETTER-ITEM-FRAMES-2026-10-19.md (B 19.10.), Umsetzung nach Besitzer-Freigabe.
- [~] (B, b-concepts19, 19.10., Konzept) Shulker-Box-Tooltip (Inhalt beim Hovern) + Endertruhen-Tooltip (eigener Endertruhen-Inhalt) – vermutlich Simple Interfaces. Konzept: docs/ai/KONZEPT-CONTAINER-TOOLTIPS-2026-10-19.md (B 19.10.), Umsetzung nach Besitzer-Freigabe.
- [~] (B, b-concepts19b, 19.10., Konzept) MC-Paint-Idee: „Bild-Bauplan“ wie Blaupause – Code für ein Bild eingeben (Import) oder Bild hochladen mit automatischer Umwandlung in Blöcke/Karten-Farben; Konzept ausarbeiten (Simple Building Blaupausen / Simple Maps?). Konzept: docs/ai/KONZEPT-BILD-BAUPLAN-2026-10-19.md (B 19.10.), Umsetzung nach Besitzer-Freigabe.
- [~] (B, b-concepts19, 19.10., Konzept) Bundles Beyond (Ideen übernehmen: größere/farbige Bündel, Bündel-Komfort) – Konzept, Abgleich mit SB-Rucksäcken und Simple Sandwiches (Essen aus Bündeln). Konzept: docs/ai/KONZEPT-BUNDLES-BEYOND-2026-10-19.md (B 19.10.), Umsetzung nach Besitzer-Freigabe.
- [~] (B, b-concepts19b, 19.10., Konzept) Simple Maps: Webkarte (wie JourneyMap Web Map / Dynmap/BlueMap), wenn möglich 3D-Karte wie auf SMP-Servern – Konzept (Server-Render, Auslieferung über sb-web?, Datenschutz/Claims). Konzept: docs/ai/KONZEPT-WEBKARTE-2026-10-19.md (B 19.10.), Umsetzung nach Besitzer-Freigabe.
- [~] (B, b-concepts19b, 19.10., Konzept) MineTracer: Vorteile übernehmen und in Simple Tweaks T10 (Block-/Truhen-Protokoll) einbauen – Funktionsvergleich Ledger/MineTracer vs. T10. Konzept: docs/ai/KONZEPT-MINETRACER-T10-2026-10-19.md (B 19.10.), Umsetzung nach Besitzer-Freigabe.
