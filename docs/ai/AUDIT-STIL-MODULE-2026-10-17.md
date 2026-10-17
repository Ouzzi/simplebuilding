# Stil-Audit der Module (modules/*) gegen SimpleBuilding - 2026-10-17

Branch `b-styleaudit` (Basis claude-wave1 92ffa4fd7). Referenz: SimpleBuilding (SB) und `AGENTS.md` (EN amerikanisch, DE mit Umlauten,
Tooltip-Regel, Lesbarkeit >= 4,5:1), `docs/ai/UI-BAUSTEINE.md`, Memory `ui-lesbarkeit` und `konsistenz-simplemods`.
Ergänzt `AUDIT-MODULE-2026-10-11.md` (Funktion/Unabhängigkeit) und `AUDIT-DATEN-2026-10-11.md`; hier nur Stil/Einheitlichkeit.
Methode: Skripte über alle 34 Module + SB (Metadaten, Lang-Parität/-Schreibweise, Schlüssel-Schema, Registrierung, UI-Quellen,
Kontrastrechnung WCAG), dazu `tools/audit_data.py all`, `tools/audit_modules_checks.py`.

Aufwand: S = Minuten, M = Stunden, L = Tag+ / Abstimmung mit Besitzer. Status: **erledigt** (in diesem Branch) oder **offen**.

## A. Erledigt in diesem Branch (keine Gameplay-/Textur-Änderung)

| ID | Modul | Befund | Beleg | Umsetzung | Aufwand |
|---|---|---|---|---|---|
| ST-01 | 11 Module | `fabric.mod.json` ohne `license` (dimensions, end, enddragonfight, fun, mobs, models, QoL, respawn, sounds, visuals; wiringexample bleibt als Beispielmodul) bzw. ohne `description` (dimensions, fun, mobs, models, QoL, sounds, tweaks, visuals) | `modules/*/fabric/src/main/resources/fabric.mod.json` Zeile 3-6 | `description` aus `modules/modules.json`, `license` CC0-1.0 ergänzt | S, **erledigt** |
| ST-02 | 34 Module | Nur SB/Riding/Money haben `authors`/`contact`; Mod-Liste zeigte keinen Autor/Link. NeoForge/Forge ohne `authors`/`displayURL`, teils ohne `description` | `neoforge.mods.toml`/`mods.toml` Zeile `displayName` | `authors="Ouzzi"`, `displayURL`, `contact.homepage/sources/issues` (azarfane.de, github.com/Ouzzi/simplebuilding) in allen 3 Loadern; Vorlage `tools/templates/module` angepasst | S, **erledigt** |
| ST-03 | simplemobs | Modulbeschreibung nur "Simple Mobs module." (Platzhalter von `newmod.py`) | `modules/modules.json` (simplemobs) | echte Beschreibung | S, **erledigt** |
| ST-04 | simplequalityoflife, simpleend, simplemaps, SB | Britisches Englisch: "colour", "grey", "coloured", "Synchronise", "centres", "behaviour", "test centre" | z. B. `simplemaps/.../lang/en_us.json` `screen.simplemaps.tab.sync`; SB `testcentre.command.overworld_only`; `enableSimpleMobs.@Tooltip` | auf amerikanisch (nur Werte, Schlüssel/Befehl `/sbtestcentre` unverändert) | S, **erledigt** |
| ST-05 | simplequalityoflife, simplemoney, simpledimensions, simpletweaks, simpleriding | Deutsche Lang-Dateien mit ASCII-Ersatz statt Umlauten (`fuer`, `Behaelter`, `koennen`, `Kaeufe` ... ca. 110 Stellen) | `modules/simplequalityoflife/.../de_de.json` (93), `simplemoney/.../de_de.json` (17), `simpledimension/.../de_de.json` guide.page.12, `simpletweaks/.../de_de.json` guide.page.2/3, `simpleriding/.../de_de.json` | echte Umlaute/ß | S, **erledigt** |
| ST-06 | wiki/manual.json (SB + 14 Module) | Deutsche Handbuch-Texte mit ASCII-Ersatz (ca. 250 Stellen, `fuer`, `oeffnet`, `zaehlen` ...) | `wiki/manual.json`, `modules/simple{interfaces,money,movement,qualityoflife,tools,...}/wiki/manual.json` | echte Umlaute; `wiki/data/*` neu erzeugt | S, **erledigt** |
| ST-07 | simpleblocks, simpleovens | Kreativtab-Titel `itemGroup.<mod>` (Groß-G, ohne Namen) statt `itemgroup.<mod>.<name>` wie alle anderen 12 Module | `BlocksRegistry.java:124`, `OvensRegistry.java:102`, `tools/{generate,check}_data.py` | auf `itemgroup.simpleblocks.blocks` / `itemgroup.simpleovens.ovens` | S, **erledigt** |
| ST-08 | simpletools | Todesvorrat-Fenster: Text `DIM_TEXT` 0x8B8B8B auf Grau 0xC6C6C6 = 1,99:1 (leere Schicksalszeile), `WARN` 0xB85A10 = 2,73:1 (Pausenhinweis) - unter 4,5:1 | `DeathVaultScreen.java:30`, Nutzung `:225`, `:235` | `DIM_TEXT` 0x505050 (4,72:1), `WARN` 0x8A3A00 (4,58:1); Rest war schon >= 6,07:1 | S, **erledigt** |
| ST-09 | simplebuilding:mixed_octet | Lang-Schlüssel (Auftrag nannte ihn als fehlend) | `src/main/resources/assets/simplebuilding/lang/{en_us,de_de}.json:3196`, Overlay `:3182` | **kein Befund**: EN+DE vorhanden | - |

## B. Offen

### B1. Metadaten, Struktur

| ID | Modul | Befund | Beleg | Vorschlag | Aufwand |
|---|---|---|---|---|---|
| ST-10 | alle außer Money/Riding/Visuals | Kein Mod-Icon (`icon`/`iconFile`/`logoFile`); SB hat `assets/simplebuilding/icon.png`. Nur Money, Riding, Visuals haben eines (alle drei nicht im SB-Stil) | `fabric.mod.json` (Feld `icon` fehlt), SB `src/main/resources/fabric.mod.json:14` | einheitliches Icon-Schema (SB-Icon + Modul-Motiv), dann Feld in allen 3 Loadern; Textur-Arbeit, daher mit Besitzer-Vorschau | M |
| ST-11 | simpleaesthetics, dimensions, mobs, models, QoL, riding, sounds, visuals, wiringexample | Einstiegsklasse heißt noch `FabricExample` (Vorlagenname) statt `<Name>Fabric` wie die übrigen 25 | `fabric.mod.json` `entrypoints.main` (z. B. `simplemobs ... FabricExample`) | umbenennen (Fabric+NeoForge+Forge-Klassen, Tests, Metadaten) und `tools/templates/module` auf `__MODID__Fabric` ändern | M |
| ST-12 | simplefun, simplemobheads, simplemoney | Paketschema weicht ab (`com.simplefun`, `com.simplemobheads.fabric`, `com.simplemoney`) statt `com.simplebuilding.modules.<mod>` (Rest) | `fabric.mod.json` Einstiegspunkte | nur dokumentieren (Umbenennung wäre Port-/Mixin-/IdMoves-Risiko), neue Module nach Schema | L |
| ST-13 | simpledimensions | Mod-ID `simpledimension` (Singular) ≠ Ordner/Name/Projekt `simpledimensions`; Asset-Ordner `assets/simpledimension`, Mixin `simpledimension.mixins.json`; Lang-Präfix `simpledimension.*` | `modules/simpledimensions/fabric/src/main/resources/fabric.mod.json:3` | ID-Migration (IdMoves, Weltdaten) nötig - nur mit Besitzer-Entscheid; bis dahin Ausnahme im Schema notieren | L |
| ST-14 | alle | Versionsschema uneinheitlich: 0.1.0 (24 Module), 1.0.0 (Aesthetics, Crucibles, Ovens, Mobheads), 1.0.5-1.0.7 (Riding, QoL, Visuals), 1.2.x (Tweaks 1.2.12, Money 1.2.16), 1.4.0 (Fun), Lib 0.2.0; SB `${mod_version}` | `modules/modules.json` `version` | Regel festlegen (z. B. 1.0.0 sobald abgenommen/von anderen genutzt) und in AGENTS.md eintragen | S |
| ST-15 | simplemodels | Server-Verzeichnis `config/simplemodels/catalogue` und `CataloguePayload`/`ModelCatalogue` in britischer Schreibweise (Pfad ist API, daher in Lang belassen) | `ModelBrowser.java:79`, `CataloguePayload.java:7` | bei nächster Migration `catalog` + Pfad-Umzug mit Altpfad-Fallback | M |
| ST-16 | simpleriding | NeoForge-Beschreibung ≠ Fabric-Beschreibung; `logoFile` statt `iconFile` (NeoForge 26.x zeigt für `logoFile` eine Warnung, siehe SB-Kommentar) | `simpleriding/neoforge/.../neoforge.mods.toml` | an Vorlage angleichen | S |

### B2. Lang und Kreativtabs

| ID | Modul | Befund | Beleg | Vorschlag | Aufwand |
|---|---|---|---|---|---|
| ST-20 | alle mit Config | Vier Schlüssel-Schemata für Config-Texte: (a) `<mod>.option.*`+`.title`+`.tab`+`.tooltip` (farming, interfaces, loot, mobmanners, movement, sounds, tools, weather), (b) `text.autoconfig.<mod>.option.*`+`.@Tooltip` (fun, money, riding, QoL, visuals), (c) `config.<mod>.*` (end, enddragonfight, mobheads, respawn, tweaks), (d) `<mod>.config.*` (aesthetics, dimensions, models, trims) | Lang-Dateien (Zählung `tooltip`/`@Tooltip` siehe Skript im Audit-Lauf) | Zielschema (a) für Framework-Config, (b) bleibt wo AutoConfig-Klassen es erzwingen; Umbenennung Lang+Code pro Modul, mit `check_data.py` absichern | L |
| ST-21 | mobheads, money, riding, sandwiches, tweaks | Kreativtab-Titel uneinheitlich: "Mob Heads", "Currency Materials", "Riding Items", "Simple Sandwiches – Kitchen", "Simple Tweaks - Claims" (Bindestrich vs. Gedankenstrich); übrige "Simple X" | `simplemobheads/.../en_us.json:272`, `simplemoney/.../en_us.json:17`, `simpleriding/.../en_us.json:2`, `simplesandwiches/.../en_us.json:29`, `simpletweaks/.../en_us.json:2` | Namensschema "Simple X" (Hauptmodul) bzw. "Simple X – Teil" mit Gedankenstrich; Besitzer wählt Namen (sichtbar im Spiel) | S |
| ST-22 | beacons, crucibles, fun, maps, mobheads, mobs, ovens, riding, shops, trims, tweaks | Eigene Kreativtabs ohne `CreativeTabSettings` (Schalter "Items in Kreativ-Tabs"): nur simpleblocks, simpleend, simplesandwiches (+SB) haben ihn; alle bauen den Tab per eigener `CreativeModeTab.builder`-Kopie | `BeaconsRegistry.java:93`, `CrucibleRegistry.java:16`, `FunContent.java:45`, `MapsItems.java:52`, `MobHeads.java:235`, `MobsRegistry.java:123`, `OvensRegistry.java:102`, `Riding.java:18`, `ShopsRegistry.java:127`, `TrimsRegistry.java:114`, `LegacyDeed.java`/`FabricSimpleTweaks` (Queue-Nachtrag 07.10. nennt dies als offen) | gemeinsamen Tab-Baustein in simplelib (Titel, Icon, Schalter, Vanilla-Tab-Routing wie `CrucibleRegistry.wantsStacks`) und Module darauf umstellen | L |
| ST-23 | SB, trims, riding, sandwiches | `audit_data.py names`: Items/Blöcke mit Definition aber ohne Namen: `placed_*`/`standing_*`/`held_*` (SB 140, Trims 21), `enchanted_book_leaping/tailwind` (Riding), `board.bread_half` (Sandwiches). Wahrscheinlich technische Blöcke ohne Item/Creative-Eintrag | `python3 tools/audit_data.py names` | pro Gruppe prüfen: Item registriert? sonst Eintrag in Ausnahmenliste `audit_data.py`, sonst Name ergänzen (Jade/Fallback zeigt sonst Schlüssel) | M |
| ST-24 | SB, Money | `audit_data.py` DE: `jukebox_song.simplebuilding.*` identisch zu EN ("SimpleBuilding - Soft Breeze" ...) | `audit_data.py lang` | Titel sind Eigennamen, Ausnahme eintragen | S |

### B3. UI (gemeinsame simplelib-Bausteine, Rahmen, Kontrast)

Bausteine: `UiBoxes.box/stairFrame`, `UiPalette`, `UiContrast`, `UiBookmarks`, `UiFilterButton` (`docs/ai/UI-BAUSTEINE.md`). Nutzen sie bereits: crucibles, interfaces, maps, questionary, shops, tweaks (Deed/Table), riding (Hufeisen). **Eigene, nicht-simplelib Zeichnung:**

| ID | Modul | Befund | Beleg | Vorschlag | Aufwand |
|---|---|---|---|---|---|
| ST-30 | simplestats | `ItemStatsScreen`: eigenes Vanilla-Grau-Panel mit 1-px-Rand (`BORDER`/`PANEL`/`LIST_BG`), kein Mehrfach-Rand mit stufigen Ecken; Kontrast ok (kleinster Wert `SOFT` 4,55:1 auf `LIST_BG`, sonst 6-8,7:1) | `ItemStatsScreen.java:33-34`, `:79-83` | auf `UiBoxes.box` + `insetColored` umstellen. **Achtung** Modulunabhängigkeit: simplestats hat kein `requires: simplelib` (`modules.json`); Umstellung heißt simplelib bündeln (Fabric `include`, NeoForge `jarJar`, Forge `forgeBundleSimplelib`) und Metadaten/Standalone-Target anpassen | M **ERLEDIGT b-uikit2: `UiBoxes.box` + `insetColored`, simplelib gebündelt (3 Loader, ohne `requires`).** |
| ST-31 | simpletools | `DeathVaultScreen`: eigener 4-Stufen-Rand (schwarz/weiß/dunkelgrau) und Vanilla-Truhen-Textur, keine `UiBoxes`; kein simplelib | `DeathVaultScreen.java:88-113` | `UiBoxes.box` + `bigSlot`/`slot`, gleiche Bündel-Frage wie ST-30 | M **ERLEDIGT b-uikit2: rechtes Panel `UiBoxes.box`; linke Vanilla-Truhen-Textur bleibt bewusst (Slotpositionen hängen daran), simplelib gebündelt.** |
| ST-32 | simplerespawn | `DownedScreen`/`RespawnHud`: nackter Vanilla-Screen, Text direkt auf dunklem Verlauf, Rahmenlose Balken; Kontrast der Texte auf dem Verlauf ok (>= 7:1), aber kein Container-/Kasten-Stil | `DownedScreen.java:68-77`, `RespawnHud.java` (`fill`) | Kasten (UiBoxes) um den Hinweistext, HUD-Balken im Treppenrahmen; simplelib bündeln | M **ERLEDIGT b-uikit2: Textkasten (`UiBoxes.box`, dunkelrot) im DownedScreen, HUD-Balken `UiBoxes.stairFrame`; simplelib gebündelt.** |
| ST-33 | simplemodels, simplevisuals | `ModelsTab` zeichnet den Reiter von Hand (`RIM`, `LIGHT`, `SHADED`, 10 `fill`); `ModelBrowser` (Models/Visuals) plain Screen | `ModelsTab.java:41-49`, `simplevisuals/.../ModelBrowser.java:26,49` | `UiBookmarks.tab/ribbon` und `UiBoxes.box` (simplelib-Bündelung nötig) | M **ERLEDIGT b-uikit2: `ModelsTab` -> `UiBookmarks.tab`; beide `ModelBrowser` nutzten `UiBoxes.box` schon (Befund dort veraltet).** |
| ST-34 | simplebeacons | `StrongBeaconScreen`: Beschriftungen `LABEL` 0xE0E0E0 und `DIM` 0x8A8A8A auf Textur-Hintergrund `strong_beacon.png`; Kontrast gegen den echten Hintergrund **nicht geprüft** (Textur zur Build-Zeit abgeleitet; auf Grau 0xC6C6C6 wären es 2,0:1, auf Dunkelgrau 3,4:1) | `StrongBeaconScreen.java:46-47`, `:139-144` | Client-Screenshot prüfen, `DIM` und ggf. `LABEL` per `UiContrast.ensure` auf >= 4,5:1 | S-M **ERLEDIGT b-uikit2: echter Hintergrund (beacon.png rechtes Feld) ist 0x212121, nicht Grau: LABEL 12,2:1, DIM 4,66:1 -> Farben bleiben; im Lint festgehalten.** |
| ST-35 | alle Module mit Screens | Kein automatischer Kontrast-/Rahmen-Check für Modul-UIs (nur manuelle Prüfung) | - | Test `UiContrast` über alle öffentlichen Farbpaare (`UiPalette`) im Client-Test; für Modul-Konstanten Lint (`tools/check_ui_contrast.py`: `0x..`-Paare je Datei) | M **ERLEDIGT b-uikit2: `tools/check_ui_contrast.py` (Gradle `checkUiContrast`): Text- gegen Face-Konstanten je Client-Datei >= 4,5:1, Texturflächen in Tabelle.** |

### B4. Guide, Wiki, Questionary

| ID | Modul | Befund | Beleg | Vorschlag | Aufwand |
|---|---|---|---|---|---|
| ST-40 | simplemaps, simpletools, simpleenddragonfight, simpleinterfaces, simplesounds, simplevisuals | Kein Handbuch (`guide_book`) und keine FTB-Startquest, obwohl Spielinhalt/Bedienung (Wayfinder, Todesvorrat, Drachenkampf). Sub-Mods von QoL (farming, loot, movement, weather, mobmanners) sind über das QoL-Buch abgedeckt, Aesthetics-Teile nicht | `modules/<mod>/shared/resources/**/guide_book*` fehlt | Handbuch + Quest (Vorlage `simpletrims`/`simplebeacons`), `python3 tools/guide_book_pages.py` | M je Modul |
| ST-41 | simpleblocks, simpledimensions, simplestats, simplewanderers | Kein `questionary/`-Ordner (Questionary-Tisch zeigt keine Texte) | `modules/<mod>/shared/resources/assets/<mod>/questionary` fehlt | Einträge aus Wiki-Manual ableiten | M |
| ST-42 | wiringexample | Als einziges Modul ohne `tools/check_data.py` (Beispielmodul, bewusst) | `modules/wiringexample/tools` | Ausnahme dokumentieren | S |

### B5. Sonstiges (aus vorhandenen Audits übernommen, nicht neu bewertet)
`M-05` (Forge-Testziel fehlt für simpleaesthetics, weitere siehe `AUDIT-MODULE-2026-10-11.md`), `M-09` (Dimensions-Config-Beschriftungen = Rohschlüssel), `M-12`.
Nicht prüfbar auf sb-agent: `check_data.py` von simplemaps/enddragonfight/questionary brauchen Pillow (laufen auf sb-test).

## C. Prüflauf
Siehe Abschnitt "Ergebnis" am Ende (Compile, Modul-Targets, Daten-/Wiki-Checks).
