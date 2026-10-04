# Messuhr und Harz-Schachbrett (Nachtrag 8)

## Plan
- Gemeinsamen ModRecipeProvider gezielt ändern: Messuhr nur mit GADGET_REWORK, Harzziegel auf 26.2/26.3.
- Messuhr: `AN /NCN/ NK`, Leerstellen mit vorhandenen Kupfernuggets N füllen, dann acht Randfelder einen Schritt im Uhrzeigersinn verschieben: `NAN/NCN/NKN`. Mitte bleibt Uhr.
- Vorhandene GameTests und Wiki-Faktentests erweitern; JEI EN/DE in beiden Ressourcenbäumen versionsgerecht halten, Wiki-Prosa aktualisieren.
- Nur 26.3-Datagen ausführen. Die beiden generierten 26.2-Harzdateien (Rezept und Freischaltung) gezielt ändern.
- 1.21.11 besitzt einen eigenen Provider unter mc1_21_11/fabric und bleibt unverändert.

## Risiken und Verifikation
- Keine 90-Grad-Drehung; alter Leerraum und ungedrehtes volles Muster müssen abgelehnt werden. Alle sechs Nuggets sind Pflicht.
- 26.2-Messuhr samt Kompass und Prosa bleibt erhalten. Kein zusätzlicher Buchstabe nötig: N bezeichnet bereits Kupfernuggets.
- Gefilterte Rezepttests, vollständige Fabric-/NeoForge-26.3-Suiten, check, 26.2-/Forge-26.3-Compile; Wiki mit venv --all und uv --all --check.
- Testzentrale über vorhandene Server-GameTests bauen und Item-Abdeckung prüfen. Kein Clientstart, kein Push; Commit nur auf claude-gpt-gauge.
- Vorhandenes unversioniertes .serena/ bleibt unangetastet.

## Ergebnis und Verifikation (2026-10-04)
- Muster umgesetzt: `AN /NCN/ NK` wird nach Auffuellen und Randrotation `NAN/NCN/NKN`. N bleibt Kupfernugget; A Amethystscherbe, C Uhr, K Kupferkern.
- Harz-Schachbrett nutzt auf 26.2/26.3 Harzziegel statt roter Netherziegel. 26.3 erbt das identische Basisrezept; seine eigene Freischaltung wurde durch Datagen aktualisiert.
- JEI, Handbuch und Wiki EN/DE aktualisiert; 26.2-Messuhr-Prosa bleibt versionsgerecht unveraendert. 1.21.11 und 26.4 sind unveraendert.
- Datagen: `BUILD SUCCESSFUL in 8m 21s`; keine Datagen-Ausfuehrung auf 26.2 oder 1.21.11.
- Fabric Tweaks: `alles gruen: 112/112 bestanden, 0 rot` (2026-10-04T15-16-31Z-33a5).
- Fabric Schachbrett: `alles gruen: 1/1 bestanden, 0 rot` (2026-10-04T15-20-32Z-e835).
- Vollstaendige Fabric-/NeoForge-26.3-Suiten: `alles gruen: 1832/1832 bestanden, 0 rot`, je 916 (2026-10-04T15-23-35Z-1fd0), einschliesslich Testzentralen-Neubau und Item-Abdeckung.
- Wiki: venv `--all`, uv `--all --check` erfolgreich; `wiki: up to date, everything documented.` Alle 49 Wiki-Tests gruen. Eine bestehende Harz-Erwartung in test_item_notes.py wurde mit angepasst.
- Handbuchpruefung: zwei bereits bestehende Ueberlaeufe in de_de guide topics 1 (14 Zeilen) und topics 3 (15 Zeilen). Gegenprobe mit HEAD-Sprachdateien im Speicher bestaetigt exakt dieselben Probleme; geaenderte Schachbrett-Seite unauffaellig.
- Keine Minecraft-Clienttests oder Besitzerwelt-Aenderungen; keine Pixelkunst. Kein Push/Merge.
- Vollstaendiges Gate inklusive Zusatz-Compiles: `gradlew.bat check :compileJava :neoforge:compileJava :mc26_3:forge:compileJava -Pforge263=true -q`, `GRADLE_EXIT=0`. Balancing, Modul-Daten, Quests und 49 Wiki-Tests gruen. Starker Speicherdruck verlaengerte den Lauf; er endete regulaer erfolgreich.
