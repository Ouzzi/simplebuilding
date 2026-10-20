# Plan: Wiki "Alle Mods" + Super-Mod-Dropdown (b-wikiallmods, 20.10.2026)

- Ist: `wiki/modules.py` schreibt `wiki/data/modules.js` (flache Liste); `wiki/index.html` baut daraus ein flaches Dropdown. `modules/modules.json` kennt `parent`/`bundle`.
- Daten: `modules.js` bekommt `parent`, `bundle`, `counts` (aus den Modul-Wiki-Daten) und für Super-Mods `overview` (Status/offen/fehlt, geparst aus `docs/ai/UEBERSICHT-MODS-*.md` Tabelle A, neueste Datei).
- Frontend: Route `#/allmods` (`pageAllMods`, DE/EN-Strings), Dropdown mit `optgroup` je Super-Mod (Super-Mod + Sub-Mods), erster Eintrag "Alle Mods", Sidebar-Link.
- Test: `wiki/tests/test_allmods.py` (Hierarchie in modules.js, Status-Parser, Seite/Dropdown/Strings DE=EN) – läuft in `testWikiModules`.
- Verifikation: generate.py --all auf sb-test, unittest, `./gradlew check -q`; Browser-Abnahme durch Besitzer.
- Risiko: Ändert sich die Übersichtsdatei, muss das Wiki neu erzeugt werden (checkWiki meldet es).
