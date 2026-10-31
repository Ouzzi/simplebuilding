# Audit simplequalityoflife (Super-Mod, 0.9.0)

Branch b-rel32-qola, Basis b-rel32 d9cda1e21. Auditor: Release-Audit-Agent QoL-A, 31.10.2026 (Umfang: MC 26.3 Fabric).
Urteil: **shipbar** (mit den unten genannten Release-Punkten, keine P1/P2).

## Durchgesehene Dateien (Zeilen)

`clienttest/java/com/simplebuilding/modules/simplequalityoflife/`
ModuleClientSmoke.java (91), 
`fabric/`
build.gradle (12), 
`fabric/src/main/java/com/simplebuilding/modules/simplequalityoflife/`
ModuleGameTest.java (11), QolModMenu.java (4), QualityOfLifeFabric.java (17), 
`fabric/src/main/resources/`
fabric.mod.json (47), 
`forge/`
build.gradle (35), 
`forge/src/main/java/com/simplebuilding/modules/simplequalityoflife/forge/`
ModuleForgeTests.java (21), QolForge.java (14), QolForgeConfig.java (23), 
`forge/src/main/java/com/simplebuilding/modules/simplequalityoflife/forge/mixin/`
RegistryLoadTaskMixin.java (54), 
`forge/src/main/resources/META-INF/`
mods.toml (87), 
`forge/src/main/resources/`
simplequalityoflife.forge.mixins.json (12), 
`neoforge/`
build.gradle (47), 
`neoforge/src/main/java/com/simplebuilding/modules/simplequalityoflife/`
ModuleNeoTests.java (15), QolNeoClient.java (6), QualityOfLifeNeoForge.java (16), 
`neoforge/src/main/resources/META-INF/`
neoforge.mods.toml (83), 
`shared/java/com/simplequalityoflife/`
Simplequalityoflife.java (32), 
`shared/java/com/simplequalityoflife/client/`
QolConfigScreen.java (22), 
`shared/java/com/simplequalityoflife/command/`
ModCommands.java (47), 
`shared/java/com/simplequalityoflife/config/`
SimplequalityoflifeConfig.java (43), 
`shared/java/com/simplequalityoflife/discover/`
QolDiscover.java (250), 
`shared/java/com/simplequalityoflife/guide/`
QolGuide.java (137), 
`shared/java/com/simplequalityoflife/test/`
QolTests.java (73), 
`shared/resources/assets/simplequalityoflife/`
icon.layers.json (118), 
`shared/resources/assets/simplequalityoflife/items/`
guide_book.json (6), 
`shared/resources/assets/simplequalityoflife/lang/`
de_de.json (225), en_us.json (225), 
`shared/resources/assets/simplequalityoflife/models/item/`
guide_book.json (6), 
`shared/resources/assets/simplequalityoflife/questionary/`
de_de.json (13), en_us.json (13), 
`shared/resources/assets/simplequalityoflife/textures/item/`
guide_book.layers.json (28), 
`shared/resources/data/simplequalityoflife/advancement/recipes/misc/`
guide_book.json (43), 
`shared/resources/data/simplequalityoflife/recipe/`
guide_book.json (12), 
`tools/`
check_data.py (38)

Zusätzlich gelesen: `framework/.../SubMods.java`, `ConfigFiles.java`, `simplelib/.../ConfigCommands` (nur Aufrufstelle), `tools/guides/module_guides.py` (Rezept-Eintrag), `modules/modules.json` (Eintrag), `fabric.mod.json`.
Nicht gelesen: `wiki/manual.json`, `ftbquests/*.json5` (Daten, von `check_data.py` strukturell geprüft); `forge/*`, `neoforge/*` (nicht im Umfang "nur 26.3 Fabric").

## Prüfliste
- **C1 Code:** `Simplequalityoflife` (Config laden, einmalige Schalter-Umbenennung `enableSimpleCreatures` → `enableSimpleMobManners` vor AutoConfig, 1-MiB-Grenze, Fehler gefangen), `ModCommands` (Level 2, schreibt `getConfig()` und `save()`), `QolGuide` (Vanilla-Buch, 100 übersetzbare Seiten, `resolved=true`), `QolConfigScreen` (reflexives Boolean-Formular über die 7 Schalter), `QualityOfLifeFabric` (Creative-Tab vor dem Schreibbuch), `QolDiscover` (generiert). Keine Null-/Thread-Probleme gefunden. Ergebnis: ok.
- **C2 Mixins:** keine (Super-Mod hat bewusst keine Gameplay-Mixins, `check_data.py` erzwingt das). Ergebnis: ok.
- **C3 Config:** 7 Schalter `enableSimpleXY`, Default true, `@Tooltip` + EN/DE-Namen und -Tooltips mit "Default: true." (von `check_data.py` und Test `config_lang` geprüft); Befehl `/simplequalityoflife config` (ConfigCommands) und `/simplequalityoflife submod <name> <bool>`. Ergebnis: ok.
- **C4 Daten:** Rezept `guide_book` (Buch + Truhe, Kategorie misc), Advancement, Item-Definition, Modell, Textur (+ `layers.json`), Lang EN/DE gleiche 223 Schlüssel, `questionary` EN/DE, FTB-Quests-Startkapitel. Befund QOL-P3-1 (Questionary nannte "Werkbank" statt Truhe) behoben. Ergebnis: ok.
- **C5 Unabhängigkeit:** Super-Mod `depends` auf alle 7 Sub-Mods (per `include` verschachtelt), `suggests` simplebuilding/modmenu; keine Klassenimporte der Sub-Mods (nur `SubMods`-Schalter über die Mod-ID). Lädt ohne SimpleBuilding. Ergebnis: ok.
- **C6 A12:** Eingriff = nur Schalter pro Sub-Mod und ein Buch; kein Vanilla-Verhalten verändert. Ergebnis: ok.
- **C7 Netzwerk:** keine eigenen Pakete. Ergebnis: ok.
- **C8 Lizenz/Assets:** CC0-1.0, keine Pfade/IPs/Schlüssel (grep), Icons 16x16 und 128x128 eigene Pixel. Ergebnis: ok.
- **C9 Offen:** keine TODO/FIXME im Code. Queue: CMD24-4 (Forge-Hook), END-3 (Icon-Hintergründe, später). Ergebnis: ok.

## Befunde
| ID | Schwere | Befund | Stand |
|---|---|---|---|
| QOL-P3-1 | P3 | `questionary/*.json`: Guide-Buch "aus Buch und Werkbank", Rezept ist Buch + Truhe | behoben (Commit "QOL-P3-1") |
| QOL-P3-2 | P3 | `QolGuide.installQuests` schluckt Fehler ohne Log (Rückgabe -1, Aufrufer ignoriert) | Restpunkt, harmlos (nur FTB-Quests-Startkapitel) |
| QOL-P3-3 | P3 | `fabric.mod.json` `icon` zeigt auf die 16x16-Datei; Modrinth/CurseForge-Icon besser `icon_128.png` | Release-Punkt (siehe qol-a.md) |
| QOL-P3-4 | P3 | Versionsschema: Super-Mod 0.9.0, Sub-Mods 0.1.0 (ST-14) | Rückfrage R1 (Hauptsitzung) |

Keine P1, keine P2.

## Urteil
**shipbar.** Offene Release-Punkte: Version/Changelog, Icon-Pfad, Beschreibungstexte (Entwurf in `docs/ai/release-qoa/qol-a.md`), `DevVariants.DEFAULT_ON` (Hauptsitzung; wirkt in diesem Mod nicht direkt).
