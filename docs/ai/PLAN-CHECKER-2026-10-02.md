# Netherziegel-Quarz-Schachbretter

## Bestand und Entscheidung
Die bestehende Checker-Familie ist auf 26.2 und 26.3 ohne Flag registriert. Die zwei
neuen Varianten folgen ihr, ohne die 1.21.11-Kopie oder 26.4 zu portieren. Datagen
läuft ausschließlich auf Fabric 26.3; identische neue Checker-Ressourcen werden
für den bestehenden 26.2-Vertrag übernommen. Keine neuen Steinmetz-Rezepte:
Checker haben Crafting-Rezepte; Simple Money bietet sie beim Steinmetz an.

## Umsetzung
Registrierung, kreative Reihenfolge, Suchreiter, Handbuch, Datagen, EN/DE an beiden
Orten, craftable-Tag, Wiki und Money-Preistabelle um beide Varianten ergänzen.
Vorhandenen Texturgenerator mit demselben Quarzfeld und Netherziegel-Feldern
erweitern; 16-fache A/B/C-Vorschau. Bestehenden Checker-GameTest um Rezepte,
Spitzhacke und Drops erweitern, bisherigen Rot-Netherziegel-Negativtest ersetzen.
Die Testzentrale muss beide Items und Blöcke vollständig abdecken.

## Risiken und Verifikation
Explizite Reihenfolgen und Handbuchtests müssen synchron bleiben. Generatoren
dürfen keine Änderungen in Port-Linien eintragen. Datagen nur runDatagen und
syncGenerated263 auf Fabric 26.3. Gefilterter Checker-Test, danach volle Server
Fabric/NeoForge/Forge 26.3 mit gelesener Zeile „alles gruen“, Gradle check -q,
26.2-Compile, Wiki venv --all und uv --all --check. Keine Clients, kein Push.
Eigener Commit auf gpt-checkui mit vorgeschriebenem Co-Author.

## Umgesetzter Stand und Vorprüfungen
- Beide Checker sind wie Harz ohne Versionsflag registriert. 26.2-Ressourcen aus
  bestehenden Harz-Vorlagen ergänzt und mit dem 26.3-Datagen verglichen:
  nur erwartete Unterschiede bei Loot-Bedingungen und Rezept-Freischaltung.
- Kein eigener Steinmetzhandel im Hauptmod; Simple Money übernimmt je Variante
  das bestehende Harz-Angebot (8 Scheine, Steinmetz Stufe 1, Vorrat 2, Chance 1).
  Preisgleichheit und Begrenzungen prüft der bestehende Money-Datencheck.
- Testzentrale übernimmt beide Checker automatisch aus dem Kreativtab.
- Texturen: vorhandenes Quarzfeld, obere linke 8x8-Pixel der Vanilla-Netherziegel,
  gespiegelte Seiten. Neue Texturen bleiben aus der 1.21.11-Kopie ausgeschlossen.
- Vorschau reproduzierbar mit `generate_textures.py --checker-preview <PNG>`:
  `C:/Users/o_o/code/minecraft-mods/previews/netherziegel-checker-vorschau.png`.
- Fabric-26.3-Datagen und syncGenerated263: `DATAGEN_EXIT=0`.
- Texturcheck: `OK: 512 Texturen und 9 .mcmeta in 2 Baeumen aktuell`.
- Money: `Money links: 269 bounded buy-only prices, 12 conditional module tables,
  additive tags and bilingual options`; salvage audit bestanden.
- EN/DE an beiden Orten: neue Schlüssel vorhanden, keine doppelten Schlüssel.
- Checker-Handbuchtexte EN/DE passen. Zwei schon bekannte deutsche Themenlisten
  bleiben zu lang; keine neue Überlänge.
- Fabric-/NeoForge-26.2-Compile: `COMPILE262_EXIT=0`.
- Erster Filterlauf `2026-10-05T06-38-09Z-e28c`: `NICHT gruen: 0/0`, weil
  `*quartz_checkers*` ohne Namespace keine Minecraft-Tests auswählt. Wiederholung
  mit vollständiger `simplebuilding:data_integrity_game_test_quartz_checkers_are_mined_by_pickaxe_and_crafted_from_their_material`.

## Weitere Belege
- Korrigierter Checker-Filter `2026-10-05T06-44-18Z-8d4d`:
  `alles gruen: 3/3 bestanden, 0 rot` (Fabric/NeoForge/Forge 26.3).
- Finale Wiki-Regeneration mit venv --all: `WIKI_GENERATE_EXIT=0`;
  uv --all --check: `WIKI_CHECK_EXIT=0`, `wiki: up to date, everything documented.`

## Vollständige Serverprüfung
Lauf `2026-10-05T06-49-02Z-cb38`: Fabric 960/960, NeoForge 960/960,
Forge 961/961; `alles gruen: 2881/2881 bestanden, 0 rot`.
Die Testzentrale wurde in allen drei isolierten GameTest-Welten neu gebaut.
`every_mod_item_and_block_has_its_place_in_the_test_centre` und
`the_whole_centre_builds_and_matches_its_plan` sind auf jedem Loader bestanden.
Besitzerwelt unberührt, keine Clienttests und kein Push.

## Abschluss-Gate
`gradlew.bat check -q`: `GATE_EXIT=0`, einschließlich 55 Wiki-Unit-Tests (`OK`),
Balance-, Atlas-, Jade- und Modulprüfungen. Vollständiges Log: `.ai-runs/checkui-gate.log`.
Keine Quelländerung unter mc1_21_11 oder mc26_4. Die beiden Aufgaben wurden gemeinsam
im finalen Arbeitsstand geprüft und werden in zwei fachlich getrennten Commits abgelegt.
