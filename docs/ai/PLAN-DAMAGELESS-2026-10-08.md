# Plan: Damageless enchantment name

## Ursache

Die stabile Verzauberungs-ID `simplefun:no_damage` wird in den Modul-
Sprachdateien noch mit den bisherigen Anzeigenamen übersetzt. Die Guide-/Wiki-Texte werden aus
`modules/simplefun/wiki/manual.json` und den Modul-Sprachdateien erzeugt;
deshalb erscheinen die alten Namen auch in den generierten Wiki-Daten.

## Änderung

- Die ID `no_damage` unverändert lassen.
- Den Verzauberungsnamen auf Englisch auf „Damageless“ und auf Deutsch auf
  „Schadlos“ ändern.
- Abgeleitete Wiki-Daten mit `python3.12 wiki/generate.py --all` erzeugen.
- Sichtbare Guide-, Buch-, Wiki- und Modul-Dokumentationstexte auf den neuen
  Namen aktualisieren.
- Den Modul-Datencheck um eine Prüfung des stabilen Übersetzungsschlüssels und
  der erwarteten EN/DE-Anzeigenamen erweitern.

## Dateien

- `modules/simplefun/shared/resources/assets/simplefun/lang/en_us.json`
- `modules/simplefun/shared/resources/assets/simplefun/lang/de_de.json`
- `modules/simplefun/wiki/manual.json`
- `docs/modules/simplefun.md`
- `modules/simplefun/tools/check_data.py`
- generiert: `wiki/data/simplefun.json`

## Tests

Geplant:

- `python3.12 modules/simplefun/tools/check_data.py`
- `python3.12 wiki/generate.py --all`
- `python3.12 wiki/generate.py --all --check`
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter lang_config`

Nicht geplant: Gradle-Testlauf sowie Client-, NeoForge- und Forge-Tests.

## Bericht

Ausgeführt:

- `python3.12 modules/simplefun/tools/check_data.py` — `Simple Fun: 5 items, 8 skull blocks, 30 bilingual config options, wiki and data integrity valid`
- `python3.12 wiki/generate.py --all` — Wiki-Daten erzeugt; vorhandene Generator-Hinweise zu Vanilla-Katalogen und nicht registrierten Guide-Items blieben ohne Fehler.
- `python3.12 wiki/generate.py --all --check` — `wiki: up to date, everything documented.`
- `python3.12 tools/testrunner/run.py --list` — Ziel `module-simplefun-fabric-263` und Test-ID `simplefun:module_game_test_lang_config` bestätigt.
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter lang_config` — `NICHT gruen: 0/0 bestanden, 0 rot`; der unqualifizierte Filter fand im Kernziel keinen Test.
- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter simplefun:module_game_test_lang_config` — ebenfalls `NICHT gruen: 0/0 bestanden, 0 rot`, weil das Kernziel `simplefun` nicht lädt.
- `python3.12 tools/testrunner/run.py --targets module-simplefun-fabric-263 --filter simplefun:module_game_test_lang_config` — `alles gruen: 1/1 bestanden, 0 rot`.

Nicht getestet: Client-Sicht sowie NeoForge und Forge. Kein vollständiger
Gradle-Lauf wurde ausgeführt.
