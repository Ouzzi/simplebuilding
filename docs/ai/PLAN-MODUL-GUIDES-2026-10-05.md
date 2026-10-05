# Plan – Guide-Buch je Modul + FTB-Startquest (Schritt 3/4 aus PLAN-GUIDES-2026-10-02, 2026-10-05)

Besitzer 2026-10-01: „Zu jeder Mod ein herstellbarer Guide wie in Simple Building, mit FTB Quests am Anfang gratis.“
Branch `claude-guides2` (Basis `20919e57`). Regeln: `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`.

## Ist-Zustand
- SimpleBuilding: Mega-Handbücher mit Reitern (`GuideBooks`, `GuideTabs`, `GuideUnlocks`, `GuideBookScreen`, eigene
  Payloads/Mixin) – alles in `com.simplebuilding.*`, darf von Modulen nicht importiert werden.
- `framework/` ist reines Java ohne Minecraft (keine Items, kein Bildschirm). `simplelib` (Crucible-Branch) ist noch
  nicht gemergt → keine Abhängigkeit.
- Module haben keine Bücher, keine FTB-Quests. Inhalte stehen code-belegt in `modules/<id>/wiki/manual.json`
  (`features[].{en,de}.{title,summary}`).

## Entscheidung: Vanilla-Buchansicht statt eigener Bildschirm
Jedes Modulbuch ist ein eigenes Item `<ns>:guide_book` der Vanilla-Klasse `WrittenBookItem` mit Standard-Komponente
`written_book_content` (Titel leer → Itemname aus Lang, Autor = Modname, `resolved=true`). Seiten sind
`Component.translatable(...)` → der Client zeigt sie in seiner Sprache (EN/DE). Rechtsklick ruft Vanilla
`Player#openItemGui` → `ClientboundOpenBookPacket` → `BookViewScreen`.
- Vorteile: kein Client-Code, keine Payloads/Mixins, identisch auf Fabric/NeoForge/Forge, keine SB-Abhängigkeit.
  `resolved=true` verhindert, dass das Öffnen eine Kopie der Seiten in den Stapel schreibt → Texte bleiben bei
  Updates aktuell (nur die Standard-Komponente).
- Abweichung vom Ursprungsplan: keine Reiter/Tor-Freischaltung für Modulbücher (die Module sind klein; ein Buch
  mit Inhaltsseite reicht „so simpel wie möglich“). Reiter bleiben SB-eigen, bis `simplelib` sie anbietet.

## Wiederverwendung / Weg nach simplelib
- Eine Quelle: `tools/guides/module_guides.py` (Tabelle je Modul) erzeugt pro Modul
  `<pkg>/guide/<X>Guide.java` aus einer Vorlage, Seiten-Lang-Keys EN/DE aus `manual.json`, Rezept + Rezept-Erfolg,
  Itemmodell, FTB-Quest-Dateien. `--check` als Gradle-Task `checkModuleGuides` in `check`.
- Pro Modul eine generierte Klasse (eigenes Paket – ein geteiltes Paket in mehreren Mod-Jars bricht NeoForge-JPMS).
- FTB-Installer (Dateien kopieren, reines Java) liegt in `framework` (`ModuleQuestDefaults`), Module bündeln
  `framework` (Fabric `include`, NeoForge `jarJar`, Forge schon über `forge-framework.gradle`).
- Später: Vorlage wird `com.simplelib.guide.ModuleGuideBook` (gleiche API `register/stack/installQuests`), die
  generierten Klassen entfallen, Module rufen die Lib. Nur der Generator ändert sich.

## Module (Entscheidungen)
| Modul | Buch | Rezept (formlos) | Creative |
|---|---|---|---|
| simpleriding | Item | Buch + Heuballen | Riding-Reiter |
| simplefun | Item | Buch + Ziegel | Fun-Reiter (Tierköpfe) |
| simplemoney | Item | Buch + Goldnugget | Money-Reiter |
| simplesandwiches | Item | Buch + Brot | Kitchen-Reiter |
| simplequalityoflife | Item (kein Item bisher, Servermechaniken) | Buch + Truhe | Vanilla Werkzeuge |
| simplemodels | Item (Server: Amboss/Katalog) | Buch + Rahmen | Vanilla Werkzeuge |
| simpledimensions (`simpledimension`) | Item | Buch + Feuerzeug | Vanilla Werkzeuge |
| simpletweaks | Item (Add-on zu SB, Buch trotzdem im Modul) | Buch + Papier | Vanilla Werkzeuge |
| simplevisuals | **kein Item**, Client-Befehl `/simplevisuals guide` | – | – |
| simplesounds | **kein Item**, Client-Befehl `/simplesounds guide` | – | – |

Begründung Visuals/Sounds: reine Client-Mods (Sounds `side="CLIENT"`, Visuals ohne Netzwerkcode). Ein
registriertes Item zwänge die Mod auf den Server und bräche Client-only-Installationen. Sie zeigen dieselben
Seiten in derselben Vanilla-Buchansicht über einen lokalen Befehl. Keine FTB-Belohnung (kein Item).

## Inhalte
- Seite 1: Modulname + Kurzbeschreibung (EN/DE im Generator, aus `modules.json description` abgeleitet).
- Danach je Feature aus `manual.json` (ohne `config_*`/`forge_*`): fetter Titel + `summary`. Zu lange Texte werden
  nach Pixelbreite (Vanilla-Schrift, 114 px × 14 Zeilen) auf Folgeseiten umgebrochen. Max. 100 Seiten.
- Letzte Seite: Hinweis auf Einstellungen (Config-Bildschirm), falls `config_*`-Einträge existieren.
- Nur belegte Aussagen: Texte 1:1 aus `manual.json` (dort code-belegt).

## FTB Quests
Je Modul `data/<ns>/ftbquests/` (JSON5 wie SB): eigenes Kapitel `<ns>_start` mit einer Quest (Aufgabe Werkbank,
wie SB `stage_1.welcome`), Belohnung = Modulbuch; Titel/Text als `{"translate": …}` in den Modul-Lang-Dateien.
Installation nur, wenn `ftbquests` geladen ist (Loader prüft), kopiert nur fehlende Dateien, Marker
`<ns>.installed` mit Datenversion (gleiche Semantik wie `FtbQuestsDefaults`).

## Tests
- Je Modul GameTest `guide_book`: Item registriert, Rezept (Buch + Vanilla-Item) ergibt das Buch, Stapel hat
  `PAGES` Seiten, Benutzen liefert SUCCESS und schreibt keine Seitenkopie in den Stapel; Quest-Ressourcen vorhanden.
- Client-Bücher (Visuals/Sounds): Test, dass die Seitenliste vollständig ist (ohne Client: reine Datenprüfung).
- Gates: Modul-Targets fabric/neoforge-263, fabric-263, `check -q`, Compile 26.2 + Forge 26.3, Wiki.

## Risiken
- FTB-Quests-Format ohne installierte FTB Quests nicht im Spiel prüfbar (Format von SB übernommen).
- Kapitel ohne Gruppe: FTB legt es in die Standardgruppe (angenommen, nicht geprüft).
- Recipe-Konflikte: alle Kombinationen Buch + X sind frei (Vanilla und SB geprüft).

## Ergebnis (2026-10-05, Branch `claude-guides2`)
- Umgesetzt wie geplant. Abweichungen: Rezepte/Itemmodelle der Module von `tools/guides/module_guides.py` geschrieben
  (keine Modul-Datagen); der Generator pflegt auch die Wiki-Notiz `<ns>:guide_book` in `manual.json`; vier
  Modul-`check_data.py` und vier Launch-Tests erlauben das Guide-Item; Riding/Money/Fun/Sandwiches zeigen das Buch
  zuerst im eigenen Reiter, QoL/Models/Dimensions/Tweaks am Ende von „Werkzeuge & Hilfsmittel“.
- Tests: Modul-Targets fabric/neoforge-263 aller zehn Module grün (586 Tests, nach Fix der Launch-Tests 178/178 im
  Nachlauf), fabric-263 940/940, `gradlew check` grün (inkl. checkModuleData, checkModuleGuides), 26.2- und
  Forge-26.3-Compile grün, Wiki `--all` + `--all --check` grün.
- Nicht getestet: Client (Buchansicht, Seitenumbruch, `/simplevisuals guide`, `/simplesounds guide`), echte FTB-Quests-
  Installation, Forge-/neoforge-263-Kerntargets und Forge-Modultests.
