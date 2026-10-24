# Plan B 24.10. – Modul-Audit (b-audmods24, Teil A von plan-b-24-abend)

Auftrag Besitzer: alle Untermods scannen, ob sie vollständig sind oder ihnen etwas aus anderen Mods fehlt
(z. B. JEI-Rezepte, die vorher da waren, jetzt nicht mehr); Grundlage für die Übersicht „alle Mods, welche
Sub-Mods wo enthalten sind, was jede macht“.

## Ist-Zustand (Retrieval)
- `tools/audit_data.py` (11.10.): wirksamer 26.3-Datenbaum, Lang-Parität, Rezept-Erreichbarkeit – kennt die
  Splits nicht (meldet verschobene `simplebuilding:`-IDs als unbekannt). `tools/audit_modules_checks.py` (M-01..05).
- `tools/check_missing_textures.py`: Modelle/Texturen/Blockstates aller Namensräume (heute 0 Fehler).
- `wiki/data/<id>.json`: Items/Blöcke/Rezepte/Loot je Modul (generiert, `checkWiki` hält es aktuell).
- `docs/MODS-UND-FEATURES.md`: Zweck je Mod (DE). `modules/modules.json`: `parent`/`bundle`/`requires`/`tests`.
- Vor-Split-Stand: `134cb5a06^` (11.10., vor „split stage 1“); 26.2-Datagen in `src/main/generated` +
  `mc26_3/generated/removed-on-26.3.txt` (588 auf 26.3 versteckte Kern-Dateien).

## Umsetzung
1. `tools/audit/module_completeness.py` (nur lesen, ohne Gradle, < 1 min): je Modul Namensräume, Blöcke
   (Blockstates), Items (Item-Definitionen), Entities/Configs/Befehle/Tests/Advancements/Rezepte zählen; prüfen
   Lang EN/DE (Parität, Name je Block/Item), Loot je Block, mineable-Tag je Block, Herkunft je Item (Rezept/Loot/
   sonstige Daten, getrennt „allein“ = ohne fremde load_conditions), JEI/REI/EMI-Plugin + Kategorien,
   Rezepttypen ohne eigene JEI-Kategorie, Wiki-Manual, Kreativtab, Config-Tooltips, Standalone-Target.
   Vergleich mit dem Kern vor den Splits: je verschobener ID (alt `simplebuilding:x` → neu `<ns>:x`) Rezepte,
   Loot, Lang-Zusatzschlüssel (Tooltip/JEI-Info), Vanilla-/c-Tags, Advancements, Nennung im JEI/REI-Code.
   Ausgabe: `--json` (Inventar) und `--md` (Bericht), Exit 0 (Audit, kein Gate).
2. Ergebnis: `docs/ai/AUDIT-MODULE-2026-10-24.md` (je Modul ein Abschnitt, Funde nach Schwere, Top-10) +
   `docs/ai/module-inventory-2026-10-24.json`; Zweck DE/EN von Hand aus MODS-UND-FEATURES/modules.json.
3. Kleine eindeutige Fehler (Lang-Schlüssel, Loot-Table, Tag) direkt beheben; Größeres als Queue-Zeilen
   „Modul-Audit 24.10. (AUDM24)“. JEI-Lücken nur listen (b-jei24), Befehle/Config nur listen (b-cmdcfg24).

## Risiken
- Heuristik: Blöcke ohne eigenes Loot (Wand-Varianten, `dropsLike`, Flüssigkeiten) und ohne mineable-Tag
  (Sofortabbau) → vor einer Korrektur im Java prüfen; nur bestätigte Fälle beheben.
- Lang-Dateien/Queue werden von anderen B-Agenten geteilt → nur eigene Zeilen.

## Verifikation
- Skript läuft fehlerfrei, Stichproben der Funde gegen Code geprüft.
- Für behobene Fehler: `module-<mod>-fabric-263` + `module-<mod>-standalone-fabric-263` über `/root/sbt`,
  Generatoren (guides/wiki/discover) + `./gradlew check -q` auf sb-test.
