# Offene Besitzer-Wünsche (Stand 24.10.2026, b-openreq24)

Auftrag (Besitzer 24.10.): „Guck nach, ob noch Sachen fehlen, die ich irgendwann requestet habe, die noch offen sind.“
Basis: `claude-wave1` @ `73c0d7be0`. Gelesen: `.claude/QUEUE.md` (alle `[ ]`/`[~]` mit Abschnitt), `.claude/QUEUE-ARCHIV.md` (Stichproben), `docs/HANDOFF.md`,
`docs/ai/KONZEPT-*`/`WUNSCH-*`/`PLAN-*`/`ROADMAP-*`, `docs/ai/AUDIT-BESITZER-WUENSCHE-2026-10-23.md`, Memory (`/root/sb-memory`, A-Konto), `/root/b-auftrag-20261024.md`.
Code-Stichproben per grep/ls (nichts gebaut, nichts im Spiel gesehen). Vorgänger-Audit (alle Besitzer-Nachrichten 17.–23.10.): `AUDIT-BESITZER-WUENSCHE-2026-10-23.md`.

Status: **a** erledigt · **b** teilweise · **c** offen, ohne Besitzer-Wahl baubar · **d** wartet auf Besitzer-Wahl/Abnahme · **e** bewusst später (Ports, Loader, Betrieb).

## Kurzfazit
- Es gehen **keine Besitzer-Wünsche verloren**: jeder offene Wunsch steht in der Queue. Das eigentliche Problem ist **Queue-Hygiene**: rund 25 Zeilen stehen noch auf `[~]`/`[ ]`/„Wahl offen“,
  obwohl der Besitzer sie am 23.10. spätabends („Rest auch als abgenommen sehen und einbauen“, W24) und am 24.10. (WB24/WC24) längst beantwortet hat und der Code sie enthält (Liste unten, Abschnitt „Veraltete Queue-Zeilen“).
- Echte offene **Bauarbeit** (c): Simple Earth (WC24-4), JEI-Lücken (AUDM24-1…3, läuft auf `b-jei24`), Config-Befehle (CMD24-2/3, `b-cmdcfg24`), Modulaufteilung Stufe 6 Simple Gadgets + Stufe 8 Simple Enderite (MA-N70), End-Boss-Material (WA24-7), Aschholz-/Dämmerholz-Items ohne Simple End (AUDM24-4), Rissiges Eis, Webkarte Stufe 4, Testzentrale-Rundgang (TZ22-9), Sandwich-Neuberechnung.
- Echte **Besitzer-Wahlen** (d) sind wenige; der große Rest ist Client-/Hör-Abnahme in einem Termin (CL-1…7, SP22-*, Memory „offen nur Client-Sicht“).

## A. Offen und baubar (c) und teilweise (b), nach Wichtigkeit
| # | Wunsch | Quelle / Datum | St. | Beleg | Nächster Schritt |
|---|---|---|---|---|---|
| 1 | **Simple Earth umsetzen**: V1–V9, V6 Standard, je Version Einstellungen, Keybind + Dev-Menü-Knopf, Menü ohne Bildschirm-Sperre (wie Oktanten-Screen) | WC24-4 / WL-1, Besitzer 24.10. nachm. | c | kein `modules/simpleearth` (`ls modules`); nur Konzept + Wunsch (`KONZEPT-/WUNSCH-SIMPLE-EARTH-2026-10-23.md`) | neuer Agent: Plan, Modul `simpleearth` (nur Fabric 26.3), Kantenübergang fließend (Fehler aus WA24-6) |
| 2 | **JEI überall vollständig** (Crafting, Umwandlungen, Mob-Drops; integriert + standalone), Money-Links in JEI | Besitzer 24.10. nachm. (Plan-B-24-Abend), AUDM24-1/-2/-3 | b | `jei_mod_plugin` fehlt in `modules/simplemoney/fabric/.../fabric.mod.json` (grep = 0); 22 Module ohne JEI-Plugin (Modul-Audit) | läuft: Branch `b-jei24`; nach Merge AUDM24-1…3 abhaken |
| 3 | **Zu jeder Config ein Befehl** | Besitzer 24.10. nachm., CMD24-2/3/4, AUDM24-5 | b | `tools/check_config_commands.py` (PENDING-Liste), 13 Module ohne simplelib | läuft: `b-cmdcfg24`; Rest CMD24-2 (simplelib bündeln), CMD24-3 (Tweaks-Records) |
| 4 | **Simple Gadgets** (Stufe 6) und **Simple Enderite** (Stufe 8): Modulfamilie nach Plan | MA-N70 (Nachtrag 70, Besitzer 16./17.10.) | c | `modules/` hat weder `simplegadgets` noch `simpleenderite`; `RotatorItem`, Enderit-Material liegen weiter in `common/src/shared` | Plan `PLAN-MODULFAMILIE-2026-10-16.md` §Stufe 6/8; je ein Bündel, Migration/IdMoves wie Stufe 5 |
| 5 | **End-Boss-Material** (neuer End-Boss, Item daraus, Rezept End-Schwingen = Nether-Schwingen + Material) | WA24-7, Besitzer 24.10. vorm. | c | `end_wings` hat Rezept über Ender-Herz (WB24-2), aber kein End-Boss/Material im Code (grep `end_boss` leer) | Konzept (Boss, Drop, Rezept) mit Vorschau, dann bauen; Abgleich mit WB24-2: das Ender-Herz-Rezept ersetzt das Material evtl. schon, Besitzer fragen/Entscheid begründen |
| 6 | Aschholz-/Dämmerholz-Items **nur mit Simple End** herstellbar/sichtbar (Regel 5) | AUDM24-4 (Modul-Audit 24.10.) | c | Storage-`ashwood_crate`, Sandwich-`*_cutting_board`, SB-`*_cauldron` u. a. immer registriert | ausblenden per `c:hidden_from_recipe_viewers` + Tab-Filter, Test per `module-<mod>-standalone-fabric-263` |
| 7 | **Rissiges Eis** neu texturieren (eigenes Thema) | Besitzer 23.10., „Funde 23.10.“ | c | Block `cracked_ice` in `modules/simpleblocks` unverändert; bei B23-T4 ausgenommen | exemplarisch 2–3 Varianten (Ebenen, Vanilla-nah), Vorschau |
| 8 | Doppelte Implementierungen Kern ↔ Module abbauen; InteractionGuard/Testhelfer nach simplelib (Rest Farming/Dimensions/ModelBrowser) | REV24-F-17, REV24-N-5 | b | `tools/check_dup_impl.py` (114 Paare) da; Löschen wartet auf 26.2-Entscheid | Schritt 2 (Duplikate je Paar prüfen), Farming bekommt simplelib-Abhängigkeit |
| 9 | **Webkarte Stufe 4** (3D) + Dimensionen-Auswahl | WEB (Besitzer 19.10.) | b | Stufen 2/3 gebaut (`a-webmap2/3`), Stufe 4 fehlt | erst Browser-Abnahme Stufe 2/3, dann Konzept Stufe 4 |
| 10 | **Testzentrale neu bauen + Rundgang dokumentieren** | TZ22-9 (Besitzer 22.10.) | c | Queue `[ ]`; TZ22-1…8 in master | Doku/Server-Render ohne Client; Bilder nur wo ohne Client möglich |
| 11 | Sandwiches: alte Stacks bei Config-Änderung neu berechnen | Memory `sandwiches-offen` | c | `SandwichContents.FORMULA = 1` (Komponente trägt Version), keine Neuberechnung (grep) | klein: Neuberechnung beim Laden, Test |
| 12 | Eimer halb gefüllt (Kapazität 2): Halb-Texturen einbauen | Memory `tex7-offen` (09.10.) | b | `enderite_lava_bucket_half.png` + Modelle `enderite_water/milk_bucket` vorhanden, vollständige Abdeckung nicht geprüft | Stichprobe aller Eimer-Stufen/Flüssigkeiten, fehlende nachziehen |
| 13 | Hollow Howler: Visual-/Animationsteil, H4 leuchtendes Herz | HOW-1/HOW-2, KONZEPT-HOLLOW-HOWLER-GRUSEL, KONZEPT-MOB-ANIMATIONEN | b | Klang eingebaut (Modus `eleven`, W24-3); Visual/Animation offen | Herz-Textur-Vorschau (HOW-1 wartet auf Besitzer-OK, s. D) |
| 14 | Mod-Icons: Hintergründe stimmig abstimmen („später“) | END-3 (Besitzer 22.10.) | e | `tools/check_mod_icons.py` vorhanden | erst wenn Mods fertig |
| 15 | Fass-Schlagzähler (Axt-Aufwertung teilt Zähler mit Anbringen) | Verwaiste TODOs 20.10. | a | Archiv-Zeile 1403 `[x]` (b-bugs21) | – |
| 16 | Simple Optimization e3 (Benchmark Besitzer-PC) / e5 (Modpack K1–K23) | END-1/END-2 | e | `tools/bench/` fertig, Lauf nur beim Besitzer | wenn Besitzer-PC frei |
| 17 | Simple Hearts: GitHub-Mod erweitern | END-15 (Besitzer 01.10. „später“) | e | `docs/ai/BACKLOG-2026-10-01.md:83` | später |

## B. Veraltete Queue-Zeilen (erledigt oder beantwortet, Zeile noch offen) → Aufräumen (Queue-Zeile OW24-1)
| Zeile | Warum veraltet |
|---|---|
| WB24-3, WA24-5 (`[~]` Nether-/Ender-Juwel) | WC24-1 `[x]`: Endfassungen eingebaut (`tools/textures/wb24_gems/`) |
| WA24-6 (`[~]` Earth-Vorschau) | durch WC24-4 (Umsetzung) ersetzt; Vorschau-Fehler wandert dorthin |
| W23-1…6, B23-T1…T4, TX22-1/-2/-3/-5, SAGE23, B23-M11/M13, QT23, SP22-3/-8, PJ23 (`[~]`/„Wahl offen“) | W24 (23.10. spätabends): „Rest als abgenommen sehen und einbauen“, ausgerollt in W24-1…6; B23-M13 (Deckel) = W24-6 (Mixin `EnderChestOpenerMixin`); Köcher/Farbkasten = W24-2/F3 |
| AUD-6 (`[~]`) | WB24-7: `pictureMap` Standard an (`[x]`) |
| TEX21 (`[ ]`), AUD-4 | Besitzer 23.10.: „TEX21 erledigt“ |
| AUD-3 (Nebelwand B) | `SimpleEndConfig.forestFogWall` Standard `false` (`modules/simpleend/.../SimpleEndConfig.java:41`) |
| ENDREP (`[ ]` „Netherit-Material behalten?“) | `ModToolMaterials.ENDERITE` repariert nur mit Enderit-Barren (Besitzer 21.10., Tag `simplelib:repairs_enderite_equipment`) |
| Überblick (1)/(A) „In Arbeit“ (Bündel 63, „Laufen: b-revlight24b …“) | Stand überholt (Bündel 65 ist aktuell) |
| `[~] HOW-2` Klangteil, `[~] WEB` Stufe-2/3-Text „offen: Merge+Gate“ | Klang in master (W24-3); a-webmap3 in master |
Hinweis: Es werden hier keine Zeilen geändert (andere B-Agenten arbeiten in `QUEUE.md`); die Bereinigung ist Zeile OW24-1.

## C. Wartet auf Besitzer-Wahl/-Antwort (d)
| # | Wahl | Quelle | Nächster Schritt |
|---|---|---|---|
| 1 | **Simple Earth**: Fragen E1–E12 (Empfehlungen gelten vorerst als Standard) | WL-1, Artifact `Mcv2uit6hGodWNN2yJxRLJ` | Besitzer bestätigt/ändert; Umsetzung (A.1) kann mit Empfehlungen starten |
| 2 | **Sammelseite** (https://claude.ai/artifact/RAjUgcgKEaVmC36M4PLxqr): offene Punkte SP22-5 (Höreindruck Linien), Earth E1–E12 | Queue WB24 „Rest der Seite folgt später“ | Besitzer antwortet; je Antwort `done:` auf der Seite |
| 3 | **HOW-1** H4 leuchtendes Howler-Herz: Besitzer-OK | Überblick (1) | Vorschau zeigen |
| 4 | **VW-1…3** Verwerfen ja/nein (Offline-Testlauf, Tweaks-Ideenliste, Stats-Tragezeit) | Block „Besitzer: verwerfen?“ | Besitzer entscheidet; Empfehlung: alle drei verwerfen |
| 5 | **STIL** ST-13 ID-Migration, ST-10 Icon-Abnahme | Überblick (1) | Besitzer |
| 6 | **Vitrinen-Optik, inverser Besatz, Köcher-Farben, Schwingen, Juwelen, Musik-Editor** (Client-Sicht; Besitzer „Rest ok“, aber ohne Sicht im Spiel) | WB24/WC24 | im Abnahme-Termin mitnehmen |
| 7 | **Claims** UI-Farbe/C1–C13 (Besitzer: Empfehlungen gelten) | SP22-8 | Abnahme im Spiel |

## D. Abnahme im Spiel / Hören (d), gesammelt für einen Termin
CL-1…CL-7 (Buch-Screen/Truhen, Kreativreiter, Hub-Rendering, Wiki/Zentrale am Handy, Simple Money, Welle-25-Punkte, neue Module); SP22-1…11 (Höreindruck SP22-2/-5/-5b);
TX22-4/QT23/a-frames23 (Laub, Fragentisch-B, Rahmen); WEB Stufe 2/3 im Browser; Memory-Zeilen „offen nur Client-Sicht“ (Panes21, EH21, SM21, Rarity21b/Netherherz, Taschen-Jukebox, Kolben/Schienen/Gewölbe/Sandwiches/Schach/Rods/Hängematte/Pads/Trainingspuppe/Workstations/Crucible/Guides);
W24-Folgen (Köcher/Farbkasten abgestellt, Jukebox-Kontakte, Schwingen 1–4, Notenblatt-Editor, Vitrinen oben Glas, Simple Lights). Die Memory-Einträge „Merge offen“ sind überholt (alles in master/wave1; Stand 20.10. b-orphtodos).
Nächster Schritt: ein Abnahme-Termin mit der Client-Sichtprüfung (Plan-B-24-Abend Agent G, Screenshots → Vorschauseite).

## E. Bewusst später (e)
Port-Run 26.2/1.21.11/26.4 + Forge-Client (END-5…8, AUDM24-6, CMD24-4, MA-9 Brücken-Version 1.3.x-bridge erst mit Release-Freigabe), Betrieb (OPS-1…4: sb-test-/sb-web-Platte, IP, Windows-Installer; DIST), Rucksack-Sortierung (END-9), Vanilla-Konformitäts-Audit (END-11), Performance-Recherche (END-12), Astralit-/Nihilit-Fliesen als Boden (END-13), Hammer-Tempo-Variante (END-14), Texturen nach Meldung (END-16), Mega-Handbücher/Testzentrale in Besitzerwelt (END-10).

## F. Erledigt, aber noch als offen geführt (a, Stichprobe im Code)
Nebelwand B aus (AUD-3), Enderit-Reparatur nur mit Enderit (ENDREP), Bild-Kartenwand (AUD-6), Notenblatt-Raster-Editor (`modules/simplemusic/shared/.../NoteGrid.java` + `NoteGridTest`), „Fremde Musik laden“-Client-Schalter (`MusicClientConfig.KEY = "loadForeignMusic"`), Astral-/Nihil-Gewölbe-Deckel (W24-6), Schwingen Stufe 1–4 (`void_wings`), Lichtscheiben-Sub-Mod (`modules/simplelights`), Octet-Regel/Baublöcke/Tab (Archiv).
