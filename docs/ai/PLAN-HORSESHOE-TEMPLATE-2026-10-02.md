# Hufeisen-Vorlage: Plan, 2026-10-02

## Ist-Zustand und Entscheidungen
- Referenz: `simplebuilding:basic_upgrade_template`, Textur unter
  `src/main/resources/assets/simplebuilding/textures/item/basic_upgrade_template.png`.
  EN `Basic Upgrade`, DE `Basis-Aufwertung`; Sprachwerte in Hauptmod und 26.3-Overlay identisch.
  `ModItems` registriert ein normales Item; vorhandene `applies_to`, `ingredients` und
  `upgrade_description` sind Sprachdaten, kein eigener Tooltip-Renderer.
- SimpleRiding: `simpleriding:horseshoe_smithing_template`, registriert als
  Vanilla `SmithingTemplateItem` in `Horseshoes.java`; Vanilla liefert die Überschriften
  „Applies to“ / „Ingredients“ und die Slot-Hinweise.
- Das Modul hat genau einen geladenen Sprachort: `shared/resources/assets/simpleriding/lang`
  mit EN und DE. Alle drei Loader nutzen diesen über `gradle/module-*.gradle`.
  Abweichung von der pauschalen Zwei-Orte-Regel: keine ungenutzten src/main-/Overlay-Kopien anlegen.
- Annahme zum Namensschema: `Horseshoe Upgrade` / `Hufeisen-Aufwertung`, ohne „Smithing Template“.
  Tooltips nennen die tatsächlichen Rezeptbasen und Zutaten, Listen wie bei der Basic-Vorlage.

## Umsetzung
1. EN/DE-Namen und Tooltips sowie die namentliche Wiki-Referenz angleichen.
2. Python-Generator `tools/textures/horseshoe_template_2026_10_02.py`: Basic-PNG als Basis,
   unveränderte Silhouette und Plattenschattierung, Pfeil durch Hufeisen ersetzen.
   Paletten direkt aus Vanilla 26.3 `iron_ingot` und `copper_ingot` ableiten.
   Bestehenden Hufeisen-Generator anbinden, damit Regeneration die Änderung erhält.
3. Vorschau A vorher / B nachher / C Basic-Referenz, 16-fach Nearest Neighbor.
   Besitzerpfad: `C:/Users/o_o/code/minecraft-mods/previews/hufeisen-vorlage-vorschau.png`.
4. Bestehenden Modul-GameTest zur Vorlage um Sprach-/Tooltip-Verifikation ergänzen.
   Wiki mit venv `--all` regenerieren und mit uv-Python `--all --check` prüfen.

## Risiken und Verifikation
- Keine Item-ID-, Rezept- oder Gameplayänderung. Keine neuen Abhängigkeiten.
- Generator-Selbstprüfung: 16x16, identische Alpha-Maske, transparente Ränder,
  nur Vanilla-Materialfarben, reproduzierbare Ausgabe; Vorschau visuell prüfen.
- Modulserver: `tools/testrunner/run.py --targets module-simpleriding-fabric-263`.
- Zusätzlich gemäß Wellenregeln `neoforge-263`, shared/NeoForge-26.2-Compile,
  Forge-26.3-Compile und volles `check -q` in diesem Worktree.
- Ergebniszeilen lesen; Exit 0 allein zählt nicht. Kein Minecraft-Client.
- Nur aktueller Branch `claude-gpt-horseshoe`, kein Push/Merge/master.
- Sichtabnahme im Spiel und Testzentrale in der Besitzerwelt bleiben offen.

## Umsetzung und Plan-Abgleich
- Sprachdateien am oben belegten gemeinsamen Modulpfad aktualisiert; Vanilla-Tooltipmechanik,
  IDs und Rezepte unverändert. Vorhandenen `configAndLang`-GameTest um Namen und Rezeptlisten ergänzt.
- Neuer Generator übernimmt die Basic-PNG und deren bestehende Pixelkarte; die sechs Kupfer-
  und vier Eisenstufen stammen direkt aus den Vanilla-Barren. Alte Generatorvarianten bleiben
  verfügbar; neuer Standard D erzeugt dieselbe neue Textur.
- Vorschau am geforderten Besitzerpfad visuell geprüft (A vorher, B nachher, C Basic-Referenz).
- `wiki/generate.py --all` zieht zusätzlich bereits vorhandene, bisher nicht regenerierte
  SimpleMoney- und Hauptmod-Quellen nach (u. a. 24.000-Tick-Geldscheinrezept und seltene
  Strukturbeute/Schalen). Diese ausschließlich abgeleiteten Daten bleiben für einen konsistenten
  Gesamt-Wiki-Check enthalten; keine fachfremden Quellen geändert.
- RTK und rg sind in dieser Sitzung nicht auf PATH; gezielte native Git-Suchen verwendet.

## Verifikation
- Neuer Texturgenerator `--check`: `Horseshoe template: OK (Basic silhouette, Vanilla palettes, transparent borders)`.
- Bestehender `horseshoe_textures.py --check`: Exit 0, keine veralteten Texturen.
- Modulprüfung: `Simple Riding: manifest, bilingual wiki/config, legacy IDs, horseshoes, and 26.3 trade data valid`.
- Wiki: venv `--all`, anschließend uv-Python `--all --check`, Exit 0:
  `wiki: up to date, everything documented.`
- `git diff --cached --check`: ohne Befund.
- Erstlauf Server `2026-10-02T21-53-54Z-5171`: `NICHT gruen: 0/0 bestanden, 0 rot`.
  Vor Testbeginn durch Dateisperre auf `.gradle/mavenizer/repo/net/minecraft/client-extra/...jar`
  abgebrochen (Server-Gradle und Gate parallel). Wiederholung erfolgt nach Gate-Ende.
- Volles Gate einschließlich angeforderter Compile-Ziele: `GRADLE_EXIT=1`, nach 37m 52s.
  `:testWikiModules` meldet 1 Fehler in 38 Tests:
  `test_all_target_items_have_bilingual_unique_sourced_sentences`, Familie `*_quartz_checker`;
  der vorhandene Block `simplebuilding:polished_ender_quartz_checker` fehlt in
  `wiki/manual.json` unter den item-spezifischen Familiennotizen. Der separate Wiki-Check
  ist dennoch grün. Test, Hauptmod-Manual und Blockquellen sind in diesem Run unverändert;
  die Gesamtregeneration macht die bestehende Lücke sichtbar. Kein fachfremder Quellenfix.
- Server-Wiederholung `2026-10-02T22-34-54Z-866f`: `alles gruen: 901/901 bestanden, 0 rot`.
  SimpleRiding Fabric 26.3: 38/38; Hauptmod NeoForge 26.3: 863/863; beide Exit 0.
- Separater Compile-Nachweis nach den Server-Tests:
  `gradlew.bat :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava -q`:
  `COMPILE_EXIT=0`.
- Offen: oben dokumentierter projektweiter Wiki-Testfehler; keine Minecraft-Clienttests,
  keine Sichtabnahme im Spiel und kein Neubau der Testzentrale in der Besitzerwelt.
  Kein Push oder Merge; Commit nur auf `claude-gpt-horseshoe`.
