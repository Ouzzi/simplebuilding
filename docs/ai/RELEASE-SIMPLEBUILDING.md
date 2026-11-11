# Release-Checkliste SimpleBuilding (Super-Mod + Sub-Mods)

Lebendes Dokument, Auftrag N31 (Besitzer 11.11.: „Hauptmods nacheinander release-fertig; erst Super-Mod, dann jede Sub-Mod
feinschleifen; offene Entscheidungen als Frage mit Vorschau“). Branch `b-relsb31`, Basis `hub/b-bundle181-gen` 6e6faa936.
Stufe 1 = nur Bestandsaufnahme (kein Code). Fragen an den Besitzer: Vorschau `/root/sb-tools-b/previews/b-relsb31-preview.html`,
Antwortcode **RELSB31**. Arbeitsliste ohne Besitzer-Entscheid: `/root/sb-tools-b/relsb31-lücken.md`.
Nachbar-Dokumente: `RELEASE-CHECK-2026-11-08.md` (Metadaten aller Mods), `RELEASE-ABNAHME-A-2026-11-09.md` (Client-Abnahme A),
`RELEASE-QOL-OPT-AES.md` (QoL/Opt/Aes, Muster dieser Datei), `TEXTUR-BEWERTUNG-2026-11-09.md` (Ampel).
**Veröffentlicht wird nichts ohne ausdrückliches „ja“ des Besitzers.**

## 1. Umfang

`docs/MODSTRUKTUR.md` Abschnitt „SimpleBuilding“: Super-Mod + 18 Pflicht-Sub-Mods (davon Simple Crucibles als Sub-Mod unter
Simple Ovens) + Simple Foundry (optional unter Ovens) + 22 optionale Mods = 42 Zeilen. Dazu gehören zum Release, aber stehen in anderen Familien-Dokumenten:
**SimpleLib** (gebündelt, Pflicht für fast alle; Abschnitt „SimpleLib“) und **Simple Tools** (Pflicht von SB seit WH26-7,
geprüft in `RELEASE-QOL-OPT-AES.md`). Die SB-Datei kann erst „fertig“ sein, wenn beide dort grün sind.

## 2. Methode und Quellen (Stand 11.11., alles lokal, kein sbt)

| Prüfpunkt | Quelle / Werkzeug | Ergebnis gesamt |
|---|---|---|
| Funktionen + A12 | `docs/ai/PRINZIPIEN.md` Teil 2 (Abschnitt je Mod), Store-Seiten, MODSTRUKTUR-Beschreibung | 5 Mods mit A12-Steckbrief, 15 ohne Abschnitt, Rest Abschnitt ohne A12 |
| Texturen | `tools/textures/texrate15_2026_11_09.py --base HEAD --json` (2071 PNG, Ampel A8d) | Zahlen je Mod r/o/g in Tabelle; begründete Rot-Gruppen aus Runde 16/17 |
| Sounds | Memory/Queue (Hörabnahmen) | offen nur Besitzer-Hören (Howler, Trompete, Platten, Presse) |
| UI | `tools/check_ui_contrast.py` (ok, 37 Dateien), Echt-Maus-Probes `modules/*/tools/*_probe.py` | Kontrast ok; Probes nur für 6 Mods |
| Handbuch + Wiki | `tools/guides/module_guides.py --check` (up to date), `wiki/generate.py --check`, `wiki/data/<mod>.json`, `wiki/manual.json` | alle Kapitel EN/DE da; `wiki/data/textures.js` veraltet (Bündel erzeugt neu); manual.json 6 Kapitel mit toten Quellpfaden |
| Config + Lang | `tools/check_lang_keys.py`, `check_tooltip_text.py`, `check_config_commands.py` (55/55), Mod-Menu-Einträge in fabric.mod.json | Lang EN=DE überall; 6 von 42 mit Mod-Menu-Bildschirm |
| Rezepte/JEI | `tools/check_jei_coverage.py` | keine Lücke |
| Daten | alle `modules/*/tools/check_data.py` der Familie | 41/41 gültig |
| Tests | Modul-Gametests (`@GameTest` in den Fabric-Adaptern), Standalone-Ziel je Mod (modules.json), `server-solo-*` (b-server26 afebf8fea grün 360/420, Rest = Vanilla-Client-Join „known“), Kompat `compat26-client-263` (b-compat26 814ad7588 grün: 61 Mods + Lithium/JEI/Mod Menu, mouse_probe 53/53, keys_probe 60/60) | grün; Vanilla-Client-Join offen (VANILLA27) |
| Seitentext | `tools/release_pages/check_pages.py` | **rot: `missing simplevault.md`**; alle anderen Entwurf mit Platzhalter-URLs |
| Changelog | `docs/release/CHANGELOG-<mod>.md` | Entwürfe (b-rel19); Simple Vault fehlt |
| Queue-Reste | `.claude/QUEUE.md` Zeilen `[ ]`/`[~]`, die den Mod nennen; `/root/sb-tools-b/prio27.md`; `/root/b-fertig.md` | Zahl je Mod in der Tabelle; meist „Client-Sicht/Besitzer-Abnahme offen“ |
| Performance | `tools/bench/headless_bench.py` (b-opte4, nur Opt-Mods gemessen) | für die SB-Familie **nicht gemessen** |
| Weitere Checks | `check_dup_impl` (0 Probleme), `check_mod_icons` (62 ok), `check_missing_textures` (0 Fehler, 7 Warnungen: Spatel-Altlast + `simplemobs:forbidden_fire` = umbenannter Feuerball, gewollt) | grün |

Legende: ✓ erfüllt (Beleg in Spalte/Abschnitt 2) · ✗ fehlt · ? ungeprüft oder Rest offen · – entfällt.
Texturen: Zahlen rot/orange/grün nach A8d; „?“ = rot ≤ 3 oder begründet, „✗“ = mehr als 3 rote ohne Begründung.
Tests: „GT“ = Anzahl `@GameTest` im Fabric-Adapter; standalone = Ziel `module-<mod>-standalone-fabric-263`; server-solo = echter
Dedicated Server mit dem Mod allein (b-server26).

## 3. Tabelle je Mod (Super-Mod zuerst)

| Mod | Rolle, Stand, Version | Funktionen + A12 | Texturen r/o/g (+Sounds) | UI | Handbuch + Wiki | Config + Lang | Rezepte/JEI | Tests | Seite | Changelog | Queue [~]/[ ] | Perf. |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| SimpleBuilding `simplebuilding` | Super, fertig, 2.0.0 | ? ohne A12 | ? 50/111/298 (47 rot: 33 tote Pad-Kopien in src/main, Lampen wie Vanilla) | ? 18 Fenster, nur Smoke-Bilder | ✓ | ✓ Mod-Menu, 7336 Keys EN=DE | ✓ Kern-Plugin (rel10 R/U) | ✓ fabric-263 1210/1210 (Gate 159) + server-solo | ✓ Entwurf | ✓ Entwurf | 14 (1 [ ]) | ? |
| Simple Arrows `simplearrows` | Pflicht, neu, 0.1.0 | ? ohne A12 | ? 7/11/0 (Teilebenen (begründet, texrate17)) | ✓ Befiederungstisch, fletching_probe | ✓ | ? nur Datei+Befehl, 68 Keys EN=DE | ✓ | ✓ 29 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 3 (0 [ ]) | ? |
| Simple Blocks `simpleblocks` | Pflicht, fertig, 0.9.0 | ? ohne A12 | ✓ 0/4/7 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 503 Keys EN=DE | ✓ | ✓ 70 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 15 (0 [ ]) | ? |
| Simple Building Tools `simplebuildingtools` | Pflicht, neu, 0.1.0 | ✗ kein Steckbrief | ✓ 0/1/30 | ✓ 6 Fenster, mouse_probe 53/53 | ✓ | ? nur Datei+Befehl, 354 Keys EN=DE | ✓ | ✓ 96 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 2 (0 [ ]) | ? |
| Simple Chisels `simplechisels` | Pflicht, neu, 0.1.0 | ✗ kein Steckbrief | ✓ 0/1/7 | – kein Fenster | ✓ | ? nur Datei+Befehl, 42 Keys EN=DE | ✓ | ✓ 10 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 1 (0 [ ]) | ? |
| Simple Enchanting `simpleenchanting` | Pflicht, neu, 0.1.0 | ✗ kein Steckbrief | ? 2/8/3 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 63 Keys EN=DE | ✓ | ✓ 19 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 4 (0 [ ]) | ? |
| Simple Gadgets `simplegadgets` | Pflicht, fertig, 0.9.0 | ✗ kein Steckbrief | ✓ 0/7/39 | – kein Fenster | ✓ | ? nur Datei+Befehl, 116 Keys EN=DE | ✓ | ✓ 77 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 7 (0 [ ]) | ? |
| Simple Hammers `simplehammers` | Pflicht, fertig, 0.9.0 | ✗ kein Steckbrief | ✓ 0/1/7 | – kein Fenster | ✓ | ? nur Datei+Befehl, 86 Keys EN=DE | ✓ | ✓ 22 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 7 (0 [ ]) | ? |
| Simple Lights `simplelights` | Pflicht, fertig, 0.9.0 | ✗ kein Steckbrief | ? 3/30/76 | – kein Fenster | ✓ | ? nur Datei+Befehl, 93 Keys EN=DE | ✓ | ✓ 17 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 1 (0 [ ]) | ? |
| Simple Machines `simplemachines` | Pflicht, neu, 0.1.0 | ✗ kein Steckbrief | ✓ 0/13/10 | ? 3 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 95 Keys EN=DE | ✓ | ✓ 88 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 3 (0 [ ]) | ? |
| Simple Mobheads `simplemobheads` | Pflicht, fertig, 0.9.0 | ? ohne A12 | ✓ 0/1/0 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ✓ Mod-Menu, 533 Keys EN=DE | ✓ | ✓ 52 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 4 (1 [ ]) | ? |
| Simple Mobs `simplemobs` | Pflicht, neu, 0.1.0 | ✓ A12 | ? 1/29/15; Hörabnahme Howler/Trompete offen | – kein Fenster | ✓ | ? nur Datei+Befehl, 282 Keys EN=DE | ✓ | ✓ 201 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 25 (1 [ ]) | ? |
| Simple Octets `simpleoctets` | Pflicht, fertig, 0.9.0 | ? ohne A12 | ? 1/1/0 | – kein Fenster | ✓ | ? nur Datei+Befehl, 438 Keys EN=DE | ✓ | ✓ 23 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 5 (0 [ ]) | ? |
| Simple Ores `simpleores` | Pflicht, neu, 0.1.0 | ✓ A12 | ? 16/19/44 (Gitter/Stäbe Messing/Bronze/Blei/Nickel + Dim.-Schrott offen) | – kein Fenster | ✓ | ? nur Datei+Befehl, 269 Keys EN=DE | ✓ | ✓ 47 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 13 (0 [ ]) | ? |
| Simple Ovens `simpleovens` | Pflicht, fertig, 0.9.0 | ? ohne A12 | ? 1/12/3 | – kein Fenster | ✓ | ? nur Datei+Befehl, 150 Keys EN=DE | ✓ | ✓ 24 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 8 (0 [ ]) | ? |
| Simple Crucibles `simplecrucibles` | Pflicht, fertig, 0.9.0 | ? ohne A12 | ? 1/9/9 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 92 Keys EN=DE | ✓ | ✓ 64 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 3 (0 [ ]) | ? |
| Simple Foundry `simplefoundry` | optional, neu, 0.1.0 | ✗ kein Steckbrief | ? 1/16/2 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 45 Keys EN=DE | ✓ | ✓ 16 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 2 (0 [ ]) | ? |
| Simple Pads `simplepads` | Pflicht, neu, 0.1.0 | ✗ kein Steckbrief | ? 54/33/16 (Besitzer TEXRATE16: Originale bleiben) | – kein Fenster | ✓ | ? nur Datei+Befehl, 227 Keys EN=DE | ✓ | ✓ 86 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 2 (0 [ ]) | ? |
| Simple Resonance `simpleresonance` | Pflicht, neu, 0.1.0 | ✓ A12 | ✓ 0/1/0 | – kein Fenster | ✓ | ? nur Datei+Befehl, 44 Keys EN=DE | ✓ | ✓ 22 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 5 (0 [ ]) | ? |
| Simple Storage `simplestorage` | Pflicht, fertig, 0.9.0 | ✗ kein Steckbrief | ? 2/22/32 | ✓ 6 Fenster, pipe_probe 30/30 | ✓ | ? nur Datei+Befehl, 275 Keys EN=DE | ✓ | ✓ 172 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 10 (0 [ ]) | ? |
| Simple Animals `simpleanimals` | optional, neu, 0.1.0 | ✓ A12 | ✓ 0/7/3 | – kein Fenster | ✓ | ? nur Datei+Befehl, 49 Keys EN=DE | ✓ | ✓ 28 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 1 (0 [ ]) | ? |
| Simple Beacons `simplebeacons` | optional, neu, 0.1.0 | ? ohne A12 | ✓ 0/7/2 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 108 Keys EN=DE | ✓ | ✓ 38 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 6 (0 [ ]) | ? |
| Simple Challenges `simplechallenges` | optional, neu, 0.1.0 | ✗ kein Steckbrief | – (keine eigenen Block/Item-PNG) | ? 2 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 224 Keys EN=DE | ✓ | ✓ 25 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 3 (0 [ ]) | ? |
| Simple Dimensions `simpledimensions` | optional, neu, 0.1.0 | ? ohne A12 | ✓ 0/2/0 | – kein Fenster | ✓ | ✓ Mod-Menu, 191 Keys EN=DE | ✓ | ✓ 53 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 4 (1 [ ]) | ? |
| Simple Dinosaurs `simpledinos` | optional, neu, 0.1.0 | ✓ A12 | ? 13/7/0 (Fossil-Stufen/Eier (begründet, texrate17)) | – kein Fenster | ✓ | ? nur Datei+Befehl, 41 Keys EN=DE | ✓ | ✓ 23 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 2 (0 [ ]) | ? |
| Simple Earth `simpleearth` | optional, neu, 0.1.0 | ✗ kein Steckbrief | – (keine eigenen Block/Item-PNG) | ✓ 1 Fenster, menu_probe | ✓ | ? nur Datei+Befehl, 254 Keys EN=DE | ✓ | ✓ 16 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 6 (0 [ ]) | ? |
| Simple End `simpleend` | optional, neu, 0.1.0 | ? ohne A12 | ? 2/23/25 | – kein Fenster | ✓ | ? nur Datei+Befehl, 185 Keys EN=DE | ✓ | ✓ 49 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 8 (0 [ ]) | ? |
| Simple End Dragon Fight `simpleenddragonfight` | optional, neu, 0.1.0 | ? ohne A12 | ✓ 0/2/1 | – kein Fenster | ✓ | ? nur Datei+Befehl, 96 Keys EN=DE | ✓ | ✓ 19 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 0 | ? |
| Simple Fun `simplefun` | optional, fertig, 0.9.0 | ? ohne A12 | ? 1/7/5 | – kein Fenster | ✓ | ✓ Mod-Menu, 194 Keys EN=DE | ✓ | ✓ 42 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 6 (2 [ ]) | ? |
| Simple Layers `simplelayers` | optional, neu, 0.1.0 | ✗ kein Steckbrief | ? 3/18/23 | – kein Fenster | ✓ | ? nur Datei+Befehl, 185 Keys EN=DE | ✓ | ✓ 57 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 0 | ? |
| Simple Maps `simplemaps` | optional, neu, 0.1.0 | ? ohne A12 | ✓ 0/5/0 | ✓ 2 Fenster, mapsmenu27 | ✓ | ? nur Datei+Befehl, 131 Keys EN=DE | ✓ | ✓ 54 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 4 (0 [ ]) | ? |
| Simple Money `simplemoney` | optional, fertig, 0.9.0 | ? ohne A12 | ✓ 0/6/2 | – kein Fenster | ✓ | ✓ Mod-Menu, 76 Keys EN=DE | ✓ | ✓ 25 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 4 (3 [ ]) | ? |
| Simple Music `simplemusic` | optional, neu, 0.1.0 | ✗ kein Steckbrief | ? 3/73/26; Hörabnahme Platten/Presse offen | ? 4 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 231 Keys EN=DE | ✓ | ✓ 42 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 9 (0 [ ]) | ? |
| Simple Questionary `simplequestionary` | optional, neu, 0.1.0 | ? ohne A12 | ? 3/2/0 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 37 Keys EN=DE | ✓ | ✓ 15 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 2 (0 [ ]) | ? |
| Simple Respawn `simplerespawn` | optional, neu, 0.1.0 | ? ohne A12 | ✓ 0/1/0 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 60 Keys EN=DE | ✓ | ✓ 19 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 0 | ? |
| Simple Riding `simpleriding` | optional, fertig, 0.9.0 | ? ohne A12 | ✓ 0/9/1 | ? 1 Fenster, nur Smoke-Bilder | ✓ | ✓ Mod-Menu, 114 Keys EN=DE | ✓ | ✓ 44 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 4 (1 [ ]) | ? |
| Simple Sandwiches `simplesandwiches` | optional, neu, 0.1.0 | ? ohne A12 | ✗ 9/48/184 | – kein Fenster | ✓ | ? nur Datei+Befehl, 66 Keys EN=DE | ✓ | ✓ 43 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 3 (1 [ ]) | ? |
| Simple Shops `simpleshops` | optional, neu, 0.1.0 | ? ohne A12 | ? 18/44/1 (Vitrinen-Glas (begründet, texrate17)) | ? 1 Fenster, nur Smoke-Bilder | ✓ | ? nur Datei+Befehl, 140 Keys EN=DE | ✓ | ✓ 77 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 3 (0 [ ]) | ? |
| Simple Trims `simpletrims` | optional, neu, 0.1.0 | ? ohne A12 | ✓ 0/4/1 | – kein Fenster | ✓ | ? nur Datei+Befehl, 65 Keys EN=DE | ✓ | ✓ 22 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 1 (0 [ ]) | ? |
| Simple Tweaks `simpletweaks` | optional, fertig, 0.9.0 | ? ohne A12 | ✓ 0/4/1 | ✓ 3 Fenster, deed_probe 22/22 | ✓ | ? nur Datei+Befehl, 518 Keys EN=DE | ✓ | ✓ 171 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 8 (1 [ ]) | ? |
| Simple Vault `simplevault` | optional, neu, 0.1.0 | ✗ kein Steckbrief | ✓ 0/2/3 | – kein Fenster | ✓ | ? nur Datei+Befehl, 76 Keys EN=DE | ✓ | ✓ 11 GT + standalone + server-solo | ✗ fehlt | ✗ fehlt | 0 | ? |
| Simple Wanderers `simplewanderers` | optional, neu, 0.1.0 | ? ohne A12 | ✓ 0/2/0 | – kein Fenster | ✓ | ? nur Datei+Befehl, 33 Keys EN=DE | ✓ | ✓ 23 GT + standalone + server-solo | ✓ Entwurf | ✓ Entwurf | 0 | ? |

## 4. Befunde je Prüfpunkt (Belege)

### 4.1 Super-Mod SimpleBuilding (Kern)
- **Funktionen:** auf 26.3 sind Öfen, Blöcke, Hämmer, Meißel, Lager, Bau-Werkzeuge, Pads, Gadgets, Trims, Kolben/Trichter,
  Auto-Schmied/-Crafter in Sub-Mods (`mc26_3/overlay/java/com/simplebuilding/version/McVersion.java`: `*_IN_SB=false`). Im
  Kern bleiben u. a. End-Systeme/Gewölbe/Schienen, Schach, Hängematte, Trainingspuppe, Türen, Musikplatten, Kleinteile,
  Holzarbeiten, gestufte Fahrzeuge, Feuer, Mega-Handbücher, Truhen-Shulker → Frage **f3**.
- **A12:** Abschnitt „SimpleBuilding“ in PRINZIPIEN ohne A12-Steckbrief für die Kern-Features.
- **Texturen:** 50 rot, davon 33 Pad-Texturen in `src/main/resources/assets/simplebuilding/textures/block/` (Pads liegen auf 26.3
  in `simplepads`; diese Kopien dienen der 26.2-Linie – ob das 26.3-Jar sie mitliefert, ist ungeprüft → L3), 8 Astralit-/Nihilith-
  Lampen (begründet: wie Vanilla `redstone_lamp`, TEXRATE16), 3 gemeißelte Astralit/Nihilith/Ender-Quarz-Ziegel,
  `hammock_rope`, `paint_box`, `cracked_diamond_block`, `reinforced_furnace_front_on`.
- **UI:** 18 Fenster-Klassen im Kern; Client-Bilder aus `rel10-client-263`/`vis24all` (A, 09.11.), keine Echt-Maus-Probe.
- **Handbuch:** `wiki/manual.json` (42 Kapitel) – Kapitel `chisel`, `ore_generation`, `dynamic_light`, `block_info_jade`,
  `configuration`, `themed_fires` nennen je eine nicht mehr vorhandene Quelldatei; `chisel` zeigt nicht auf `modules/simplechisels`.
- **Config:** Cloth-Bildschirm über Mod Menu (Kern), „Enable Simple XY“-Schalter je Sub-Mod; nur SB braucht noch Cloth Config.
- **Seite:** `docs/release/pages/simplebuilding.md` – Optional-Liste ohne **Simple Vault**; Platzhalter `<WIKI_URL>` usw. hängen
  am Domain-Umzug IG31-55 (Besitzer-Betrieb, Queue Z. 1179).
- **Changelog:** `CHANGELOG-simplebuilding.md` 2.0.0 (Modul-Aufteilung), Entwurf.
- **Queue:** 14 Zeilen nennen SB; offen u. a. MOB27-3 R2 Messungs-Zeiger im Kern (b-trimstats27, Fix-Agent läuft),
  AUDM24-6 Kern-Lang verschobener IDs (Port-Run), REV24-F-17 Schritt 3 (Besitzer).

### 4.2 Funktionen + A12 (alle Sub-Mods)
- A12-Steckbrief vorhanden: Simple Mobs, Ores, Resonance, Animals, Dinosaurs.
- **Kein Abschnitt in PRINZIPIEN Teil 2:** Building Tools, Chisels, Enchanting, Gadgets, Hammers, Lights, Machines, Foundry,
  Pads, Storage, Challenges, Earth, Layers, Music, Vault (→ L2).
- Abschnitt ohne A12: Arrows, Blocks, Octets, Ovens, Crucibles, Beacons, End, Money, Sandwiches, Shops, Trims, Tweaks, sowie
  „keine eigenen Regeln“: Mobheads, Dimensions, End Dragon Fight, Fun, Maps, Questionary, Respawn, Riding, Wanderers.
- Funktionsumfang „neu“ laut MODSTRUKTUR: 27 von 41 Sub-Mods; „fertig“: 14. Bekannte Teil-Umfänge: Simple Dinosaurs nur T. rex
  (DINO-REST wartet auf T-Rex-Abnahme → Frage **f4**), Simple Gadgets „weitere Geräte folgen Stufe für Stufe“, Simple Foundry
  nur Gold/Kupfer/Eisen.

### 4.3 Texturen, Modelle, Sounds (A8c/A8d)
- Rot ohne Begründung, gehäuft: **Simple Ores 16** (Gitter/Stäbe Messing, Bronze, Blei, Nickel, Zink …, Dimensions-Schrott-
  Seiten/-Oberseiten) → L4.
- Rot mit Begründung (Runde 16/17 „nicht angefasst“ bzw. Besitzer): Simple Pads 54 (Besitzer TEXRATE16: Originale bleiben),
  Simple Shops 18 (Vitrinen-Glas), Simple Dinosaurs 13 (Fossil-Stufen, Eier), Simple Arrows 7 (Schaft-/Feder-Teilebenen),
  Lampen im Kern 8 → Frage **f6** (als begründet für den Release akzeptieren?).
- Einzelne rote: Enchanting 2 (Tisch-Oberseiten), End 2 (Stöcke-Oberseiten), Lights 3 (transparente Scheibe), Music 3
  (Presse oben, Etiketten), Storage 2 (Fass-Flansche), Layers 3 (Wolke, gestörtes Grundgestein), Sandwiches 9, Questionary 3,
  je 1 Crucibles/Foundry/Mobs/Octets/Ovens/Lib/Tools → L5 (erst prüfen, ob in `texrate16/17_targets.json` `not_improved`).
- Sounds: offen sind nur Besitzer-Hörabnahmen (Howler-Mix, Trompete, Platten, Presse PRESS07) – nicht erneut fragen.

### 4.4 UI
- Kontrast-Prüfer grün. Echt-Maus-Probes vorhanden und grün: Building Tools (`mouse_probe` 53/53, b-compat26), Storage
  (`pipe_probe` 30/30, a-rel10), Tweaks (`deed_probe` 22/22), Earth (`menu_probe`), Arrows (`fletching_probe`), Maps (b-mapsmenu27).
- **Ohne Probe** (nur Smoke-Bilder aus rel10/vis24all): Kern (18 Fenster), Blocks (Formsäge), Enchanting, Machines (Schmied,
  Crafter, Trichter), Crucibles, Foundry, Mobheads, Beacons, Music (4), Challenges (2), Questionary, Respawn, Riding, Shops → L6.

### 4.5 Handbuch + Wiki EN/DE
- `module_guides.py --check`: up to date, jedes Modul hat ein Kapitel EN+DE (Abdeckungsprüfung `coverage()`).
- `wiki/generate.py --check`: „everything documented“, nur `wiki/data/textures.js` veraltet (erzeugt das Bündel).
- GUIDE14 (Handbuch öffnet Start oder Kapitel) wartet auf Besitzer – nicht erneut fragen.

### 4.6 Config + Lang
- Lang EN/DE vollständig (62 Module), Tooltip-Prüfer grün, Config-Befehl in allen 55 Mods mit Config.
- **Mod-Menu-Bildschirm** nur: SimpleBuilding, Mobheads, Dimensions, Fun, Money, Riding. Alle anderen (u. a. Hammers, Storage,
  Ores, Pads, Machines, Gadgets, Ovens, Enchanting, Building Tools) nur `config/<mod>-server.json` + `/<mod> config` → Frage **f2**.
- **Stand 11.11. (b-cfgscreen31, f2=A):** jede geladene Mod mit angemeldeter Config bekommt über SimpleLibs Mod-Menu-Eintrag
  (`getProvidedConfigScreenFactories`) einen Bildschirm: Kategorien = erste JSON-Ebene, Tooltip aus Lang + „Standard:/Default:“
  (+ „Bereich:“, „Neustart“), mit Cloth der Cloth-Builder, ohne Cloth Vanilla-Widgets. Eigene Bildschirme bleiben vorne.
  Server-Werte nur ohne Welt, in eigener Welt mit Operator-Rechten, auf fremden Servern nur lesbar.

### 4.7 Rezepte/JEI
- `check_jei_coverage.py`: keine Lücke; Sonder-Rezepttypen alle mit Ansicht (Formsäge 401, Befiederung 17, Trims, Rucksack,
  Pads). Eigene JEI-Tabs je Station (Entrindungs-Station, Plattenpresse) über SimpleLib `RecipeViewer.station` (B-Fix nach a-rel10).

### 4.8 Tests
- Jede Sub-Mod hat Modul-Gametests (10 bis 201) und ein Standalone-Ziel; volle Läufe macht das Bündel-Gate (A).
- Dedicated Server je Mod allein: alle 60 starten (b-server26); **alle weisen Vanilla-Clients ab** (Fabric Registry Sync,
  Konzept `KONZEPT-B-VANILLACLIENT-2026-11-11.md`, Antwort VANILLA27 offen – nicht erneut fragen).
- Kompatibilität Lithium/JEI/Mod Menu: grün (b-compat26). Sodium/Iris nicht prüfbar auf Xvfb (Queue Z. 1086).
- Offen aus b-dimid27: `simpledimensions:module_game_test_legacy_dimensions`-Fix ungetestet; Tweaks-Claim-Configs mit
  `simpledimension:`-Dimension werden nicht umgeschrieben → L8.

### 4.9 Seitentext + Changelog
- 61 Seiten-Entwürfe, **Simple Vault fehlt** (check_pages rot) → L1; SB-Seite ohne Simple Vault in „Optional“.
- Platzhalter-URLs bis Domain-Umzug (IG31-55, Besitzer). Changelog-Entwürfe vorhanden außer Simple Vault.
- Versionen: Besitzer-Regel „0.x bis Abnahme, dann 1.0.0“ (ST-14, A11.3); SB bleibt 2.0.0.

### 4.10 Bekannte Fehler / Queue-Reste
- Offene `[ ]` der Familie: BS25-19 Absturz mit Seelenfeuer-Lohen-Kopf (Z. 731, serverseitig nicht reproduzierbar), CMD24-4
  (Port-Run), CL-5/CL-7 (Besitzer-Abnahme Money/neue Module), DINO-REST (Z. 377), BS28-5 Trompete herstellbar (Wahl offen).
- Viele `[~]` tragen nur noch „Client-Sicht/Besitzer-Abnahme offen“. Besitzer 08.11.: Release-Abnahmen macht die KI selbst →
  gegen vorhandene Client-Bilder (rel10, vis24all, compat26) abhaken oder konkreten Rest benennen → L7.
- Laufend (nicht doppelt starten): b-trimstats27 (MOB27-3 R2), b-pal28/b-pal30 (Paletten), b-hamenderite27 (Bündel 182).

### 4.11 Performance (opte4)
- `bench-opt-headless-263` misst nur Simple Optimization. Für SB + Pflicht-Sub-Mods gibt es keinen Messwert (MSPT, Heap nach
  GC, Weltladezeit) gegen Vanilla → L9, Grenzwert Frage **f5**.

## 5. Lückenliste

| Nr | Mod | Was | Umfang | Branch | Besitzer? |
|---|---|---|---|---|---|
| L1 | Simple Vault, SB | Store-Seite + Changelog fehlen (`check_pages.py`: missing simplevault.md; `pages_data.py` ohne Eintrag); SB-Seite: Vault in „Optional“ ergänzen | S | b-vaultpage31 | nein |
| L2 | 15 ohne Abschnitt + 21 ohne A12 | A12-Steckbrief (Nutzen, Änderung, Eingriffsstärke) je Mod in PRINZIPIEN Teil 2; ein Satz „Vanilla use“ auf jede Store-Seite (`pages_data.py`) | M | b-a12sb31 | nein |
| L3 | SimpleBuilding | Prüfen, ob das 26.3-Jar tote Kern-Assets verschobener Inhalte ausliefert (33 Pad-PNGs, Ofen, Trims …); falls ja Ressourcen-Ausschluss nur für 26.3 (26.2 braucht sie bis Port-Run) | M | b-deadassets31 | nein |
| L4 | Simple Ores | 16 rote Texturen (Metall-Gitter/-Stäbe, Dimensions-Schrott) in A8c-Runden, Vorschau mit Varianten | M | b-texores31 | nein (Bildabnahme über Vorschau) |
| L5 | mehrere (Abschnitt 4.3) | einzelne rote Texturen: zuerst gegen `not_improved`/`excluded` abgleichen, Rest A8c-Runde | M | b-texsb31 | nein (Vorschau) |
| L6 | Kern + 13 Sub-Mods | Echt-Maus-Probes (Muster `pipe_probe.py`) für Fenster ohne Probe; Client-Läufe nur einzeln bei leerem sb-test | M–L | b-probesb31 | nein |
| L7 | alle | Queue-`[~]` der Familie mit Rest „nur Client-Sicht“ gegen rel10/vis24all/compat26-Bilder prüfen → `[x]` mit Beleg oder konkreter Rest | M | b-queuesb31 | nein |
| L8 | Simple Dimensions, Tweaks | legacy_dimensions-Test im Bündel bestätigen; Tweaks-Claim-Configs mit `simpledimension:` beim Laden umschreiben | S | b-dimrest31 | nein |
| L9 | SB + Pflicht-Sub-Mods | Headless-Messung MSPT/Heap SB-Familie gegen Vanilla (Muster `tools/bench/headless_bench.py`, Profil „sb“) | M | b-benchsb31 | Grenzwert f5 |
| L10 | SimpleBuilding | `wiki/manual.json`: 6 Kapitel mit toten Quellpfaden, `chisel` auf `modules/simplechisels` umstellen | S | b-manualsb31 | nein |
| L11 | alle 41 Sub-Mods | Code-Audit je Mod wie b-rel32 (Zeile für Zeile, P1–P3, `modules/<mod>/AUDIT.md`, Fixes klein) – kein SB-Familien-Mod hat ein AUDIT.md | L | b-audsb31-<gruppe> (5 Gruppen) | nein |
| L12 | Simple Mobheads, Tweaks | BS25-19 Absturz Seelenfeuer-Lohen-Kopf im echten Client nachstellen (Client-Lauf) | M | b-bs25crash31 | nein |
| L13 | Sub-Mods ohne Bildschirm | Config-Bildschirm je nach f2 → **f2=A umgesetzt** (b-cfgscreen31, 11.11.): SimpleLib baut ihn aus dem `ConfigCommands`-Spec jeder Mod, Mod Menu für jede Mod, Cloth optional; offen: Lang-Texte der Optionen bei 10 Pflicht-Mods ohne Options-Lang (Chisels, Enchanting, Gadgets, Lights, Machines, Mobs, Octets, Ovens, Crucibles, Resonance; Storage 1 von ~10) – dort lesbare Pfadnamen, Besitzer-Darstellung CFGSCREEN31 | M | b-cfgscreen31 | erledigt (f2=A); Darstellung CFGSCREEN31 |
| L14 | SimpleBuilding | Kern-Inhalt abspalten oder nicht | L | – | **ja (f3)** |
| L15 | Release-Welle | Reihenfolge/Umfang der Veröffentlichung | – | – | **ja (f1, f4)** |
| L16 | 4 Familien | begründet rote Texturen für den Release akzeptieren | – | – | **ja (f6)** |

Wartet schon auf den Besitzer (nicht erneut fragen): VANILLA27, GUIDE14, TEXRATE17/20, PAL28, VAULTBOOK26, MACH32S4,
WE25-8 Teil 2, REV2-B-10, REV24-F-17 Schritt 3, Hörabnahmen (Howler, PRESS07, Trompete), Domain IG31-55, T-Rex-Abnahme.

## 6. Fragen an den Besitzer (RELSB31)

Vorschau mit Kontext, Optionen und vorausgewählter Empfehlung: `/root/sb-tools-b/previews/b-relsb31-preview.html`.

| Frage | Kurz | Empfehlung |
|---|---|---|
| f1 | Release-Umfang: Welle 1 = SB + seine 20 Pflicht-Abhängigkeiten (18 Sub-Mods + SimpleLib + Simple Tools), optionale je Mod danach (A) / alle 42 zusammen (B) / nur Pflicht, optionale erst nach Phase 2 (C) | A |
| f2 | Config-Bildschirm für Sub-Mods ohne eigenen: generischer SimpleLib-Bildschirm aus der Server-JSON (A) / Reiter im SB-Bildschirm (B) / so lassen (C) | A |
| f3 | Kern-Inhalt von SB: so lassen, später abspalten (A) / vor Release abspalten (B) | A |
| f4 | Simple Dinosaurs: erst mit allen 5 Dinos (A) / mit T. rex allein (B) / aus der ersten Welle nehmen (C) | A |
| f5 | Performance-Grenze SB-Familie gegen Vanilla: ≤ 5 % MSPT (A) / ≤ 10 % MSPT (B) / nur messen (C) | B |
| f6 | Begründet rote Texturen (Pad-Originale, Vitrinen-Glas, Fossilien, Pfeil-Teile, Lampen): akzeptieren (A) / vor Release neu (B) | A |

Antwort z. B. `RELSB31: f1=A f2=A f3=A f4=A f5=B f6=A`.

**Antwort des Besitzers (11.11. ~15:30, Auftrag N31b):** `RELSB31: f1=A f2=A f3=A f4=A f5=B f6=A`
- f1=A: Welle 1 = SB + 20 Pflicht-Mods; optionale je einzeln, sobald ihre Checkliste grün ist.
- f2=A: gemeinsamer SimpleLib-Baustein baut den Config-Bildschirm aus der Server-/Client-JSON, Mod Menu für jede Mod, Cloth optional
  → L13 umgesetzt auf b-cfgscreen31 (Plan docs/ai/PLAN-B-CFGSCREEN31-2026-11-11.md).
- f3=A: Kern-Inhalt bleibt in SB 2.0.0, Abspaltung später (L14 ruht).
- f4=A: Simple Dinosaurs erst mit allen fünf Dinos → DINO-REST in die Liste der optionalen Mods nach Welle 1.
- f5=B: Grenze ≤ 10 % mehr MSPT als Vanilla (Heap nur berichten) → L9 b-benchsb31.
- f6=A: begründet rote Texturen blockieren den Release nicht (L16 erledigt).

## 7. Nächste Schritte
1. Lücken ohne Besitzer (L1–L12) nach `/root/sb-tools-b/relsb31-lücken.md` abarbeiten; kleine Doku-/Daten-Branches ohne eigenen
   sbt (Brief-Regel 17), L6/L9/L12 brauchen sb-test (einzeln).
2. Nach RELSB31-Antwort: L13–L16 umsetzen, dann diese Datei aktualisieren (Tabelle neu: `/root/scratch/relsb31/gather.py` +
   `table.py` – Skripte liegen außerhalb des Repos, Muster für die nächsten Familien).
3. Danach Familie 2 (Simple Quality of Life) mit eigener `RELEASE-<MOD>.md`.
