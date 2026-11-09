# Plan b-guidechap14: Modul-Handbuch öffnet sein Kapitel (Schalter, 09.11.)

Auftrag N7 (Sitzung B, Welle B09), Befund A-Release-Abnahme (`docs/ai/RELEASE-ABNAHME-A-2026-11-09.md`, Vis24 „Klein 2“):
„Jedes Modul-Handbuch öffnet die Mega-Guide-Startseite statt seines Kapitels.“ Basis `b-bundle163`.

## Ist-Stand / Ursache
- GUIDE01 (b-guide01, `docs/ai/PLAN-B-GUIDE01-2026-11-03.md`, Entscheidung 7): die alten Modul-Bücher `<ns>:guide_book`
  (generiert aus `tools/guides/ModuleGuide.java.in`) werden beim Benutzen **durch einen frischen `simplelib:modpack_guide`
  ersetzt** – ohne Herkunft. Der erste Rechtsklick tauscht nur (kein Bildschirm), der zweite öffnet den Mega-Guide.
- `SimpleLibFabricClient` öffnet jeden Mega-Guide mit `ModpackGuideScreens.open(null, hand)` = zuletzt gelesener Abschnitt,
  sonst der erste (SimpleBuilding) → Startseite/Inhalt. Ein Kapitel-Sprung war in GUIDE01 nie geplant.
- Also **kein Fehler, sondern Absicht von GUIDE01** (ein Buch, Startseite); der Kapitel-Bezug ging mit der Umwandlung verloren.

## Entscheidungen (selbst getroffen)
1. **Herkunft am Stapel:** die Umwandlung schreibt Vanilla-`custom_data` `{"simplelib_guide_chapter": "<ns>:guide"}` auf den
   neuen Mega-Guide (nur Vanilla-Klassen im Modul-Code, Regel 11). Ohne Schalter ändert das nichts.
2. **Client-Schalter** `guideOpensChapter` (SimpleLib-Client-Datei `simplelib-client.json`, neuer Cloth-Reiter
   „Simple Modpack Guide“/„Simple-Modpack-Handbuch“, Tooltip EN/DE mit „Default: false.“/„Standard: aus.“, auch per
   `/simpleclient simplelib set guideOpensChapter true`). **Standard aus = wie bisher (Startseite)**; Besitzer wählt GUIDE14.
3. **An:** ein Mega-Guide mit Herkunft öffnet SimpleLibs Kapitel-Bildschirm des Abschnitts, der das Kapitel enthält,
   auf der ersten Seite des Kapitels. Gesperrtes/fehlendes Kapitel → Inhaltsseite dieses Abschnitts (wie bisher).
   Die Herkunft bleibt am Buch (jedes Öffnen springt dorthin); ein gecraftetes Buch hat keine → Startseite.
4. Keine neuen Buchseiten (QoL bleibt bei 100); keine SB-Kern-Änderung (SB-Buch wird beim Einloggen umgewandelt, sein
   Abschnitt ist ohnehin der erste). Schon früher umgewandelte Bücher haben keine Herkunft → Startseite.

## Dateien
- `tools/guides/ModuleGuide.java.in` (+ generierte `*Guide.java` aller Module via `module_guides.py`): Tag bei Umwandlung,
  Gametest prüft den Tag.
- SimpleLib: `api/ModpackGuide.java` (`CHAPTER_TAG`, `chapterOf`, `sectionOf`, Schalter `opensChapter` – hier statt in den Client-Klassen, weil die Client-Datei auch auf dem Server gelesen wird; sbt1 rot: `Screen` auf dem Server geladen), `api/client/ModpackGuideScreens.java`
  (Schalter, `open(ItemStack, hand)`), `client/guide/ModpackGuideScreen.java` (Startkapitel), `SimpleLibFabricClient`,
  `UiBrightness` (Datei), `LibConfigScreen`, Lang EN/DE, `test/ModpackGuideTests.java` + `ModuleGameTest` (neuer Test
  `modpack_guide_chapter_jump`).
- Client-Test: `integration/.../clienttest/GuideChapterClientTest.java` (`-PmoduleClientTest=guidechap`), Schalter aus →
  Startseite, an → erste Seite des Kapitels, je Screenshot.

## Verifikation
Lokal: `python3 tools/guides/module_guides.py --check`, `python3 tools/precheck.py --fast`. Ein sbt: Compile Fabric (Kern,
SimpleLib, simplefun, simplequalityoflife, integration gametest), Gametests `module-simplelib-fabric-263`,
`module-simplefun-standalone-fabric-263`, `module-simplequalityoflife-fabric-263`, Kern `guide_book*`. Client-Lauf
(Screenshots) macht das Bündel (Regel: keine Client-Läufe im Branch).
