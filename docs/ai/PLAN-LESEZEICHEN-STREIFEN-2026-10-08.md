# Plan: Lesezeichen ohne Farbstreifen (26.3), 2026-10-08, Branch bp-guides

Quelle: Aufgabe „Guides: den farbigen Strich an freigeschalteten Lesezeichen entfernen (nur diesen
Strich; Lesezeichen selbst und Freischaltlogik unverändert)".

## Ist-Zustand (gelesen, nichts geändert)

- `common/src/shared/java/com/simplebuilding/client/guide/GuideBookScreen.java:674-677` zeichnet
  im Lesezeichen-Loop genau diesen Strich: `if (open) g.fill(iconX - 2, ty + 2, iconX + 16, ty + 4,
  0xFF000000 | GuideContent.style(b).colour())` – also nur für **freigeschaltete** Lesezeichen,
  Farbe = Buchfarbe. Daneben (`:682-684`) die graue Box über **gesperrten** Icons, aber nur auf der
  Altlince (`!McVersion.MEGA_GUIDES`), und `:680-681` der Kommentar, der „missing colour stripe"
  als Erkennungszeichen gesperrter Lesezeichen nennt (Kommentar wäre danach veraltet).
- `open` = `available(b)` (`:663`), Freischaltung läuft über `GuideUnlocks`/`GuideStatePayload` –
  bleibt unberührt.
- Der Strich ist die einzige Stelle dieser Art: `Inhalt`-Tab (`:689`) und Regal-Tab (`:697`)
  zeichnen keinen; `GuideContent.style().colour()` sonst nur für Tintenfarben (`ink`, `:232`,
  `secondaryColour`) – die bleiben.
- Kopien/andere Linien: `mc1_21_11/shared/.../GuideBookScreen.java:471` ist ein eigener Abzug
  (Port-Run, nicht anfassen), `mc26_3/overlay` hat keinen eigenen Bildschirm, 26.4 erbt die
  26.3-Kette. Wiki/`manual.json`/Lang-Keys nennen den Strich nirgends (nur der alte Plan
  `docs/ai/PLAN-GUIDEUI-2026-10-02.md`, historisch).

## Ursache / Entscheidung

Der Strich ist reine Client-Darstellung, kein Bug: er markierte freigeschaltete Lesezeichen. Der
Besitzer will ihn auf der Hauptlinie nicht mehr sehen.

**Offene Frage selbst entschieden:** 26.2 behält den Strich. Begründung: Aufgabe zielt auf
„26.3 Fabric Client-UI", AGENTS §3.1 (Hauptlinie zuerst, Port-Run danach) und der Präzedenzfall
`PLAN-GUIDEUI-2026-10-02` (graue Box nur auf 26.3 entfernt, „26.2-Zweig bleibt unverändert").

## Änderung (geplant)

1. `common/src/shared/.../guide/GuideContent.java`: neue server-sichere Entscheidung
   `bookmarkStripe()` (liefert `!McVersion.MEGA_GUIDES`), im Muster von `pausesGame()`
   (`:55-61`), damit Server-Spieltests sie prüfen können, ohne Client-Klassen zu laden.
2. `common/src/shared/.../client/guide/GuideBookScreen.java`:
   - `:674-677` → `if (open && GuideContent.bookmarkStripe()) { g.fill(...) }` (Strich entfällt auf
     26.3/26.4, bleibt auf 26.2; Lesezeichen-Sprite, Icon, Klick und `available()` unverändert),
   - Kommentar `:680-681` aktualisieren („missing colour stripe" gibt es auf 26.3 nicht mehr),
   - Klassen-Javadoc-Formulierung „farbiges Lesezeichen" bleibt (meint das Sprite, nicht den Strich).
3. Neuer GameTest `GuideBookTests.theOpenBookmarksDrawNoColorStripeOnTheMainLine` im Stil von
   `readingTheGuideDoesNotPauseTheGame` (`:609-624`): Wert der Entscheidung je Linie + Nachweis im
   Klassenbytecode von `GuideBookScreen`, dass der Bildschirm `bookmarkStripe` auch liest.
   Registrierung: Katalog `SimpleBuildingGameTests` **und** Fabric-Adapter
   `src/main/java/com/simplebuilding/gametest/GuideBookGameTest.java` (Fabric leitet die Id aus dem
   Adapter ab, sonst fehlt der Test auf Fabric).
4. Nicht angefasst: `mc1_21_11/**`, Texturen, Wiki, Freischaltlogik, `GuideUnlocks`.

## Tests

- `python3.12 tools/testrunner/run.py --targets fabric-263 --filter guide` (Aufgabenstellung);
  falls das Muster bei Fabric keinen Test trifft (WORKFLOW.md: Filter ohne Treffer lässt Gradle
  fehlschlagen), zweiter Lauf mit `--filter "simplebuilding:guide_book_game_test_*"` – Abweichung
  wird hier festgehalten.
- Maßgeblich ist die Zeile „alles gruen" / „NICHT gruen". Nur ein Gradle-Lauf gleichzeitig.
- Nicht geplant: Client-Lauf (Klick-/Sichtprüfung, `client-fabric-263`), NeoForge/Forge-Lauf.

## Ergebnis / Bericht

Umgesetzt wie geplant, eine Abweichung beim Filter.

**Geänderte Dateien**
- `common/src/shared/java/com/simplebuilding/guide/GuideContent.java` – `bookmarkStripe()` (+11 Zeilen).
- `common/src/shared/java/com/simplebuilding/client/guide/GuideBookScreen.java` – Strich nur noch bei
  `open && GuideContent.bookmarkStripe()`, zwei Kommentare aktualisiert (−4/+5).
- `common/src/shared/java/com/simplebuilding/gametest/GuideBookTests.java` – neuer Test
  `theOpenBookmarksDrawNoColorStripeOnTheMainLine` (+28).
- `common/src/shared/java/com/simplebuilding/gametest/SimpleBuildingGameTests.java` – Katalogeintrag.
- `src/main/java/com/simplebuilding/gametest/GuideBookGameTest.java` – Fabric-Adapter mit `@GameTest`
  (sonst fehlte der Test auf Fabric, Katalog/Lauf wären auseinander).

**Abweichung vom Plan (Filter):** `--filter guide` trifft bei Fabric keinen Test, der Lauf endet rot:
Log `testing/runs/2026-10-08T12-01-15Z-0054-fabric-263.log` → `Test selection matcher (guide) found no tests`,
Ausgabe `NICHT gruen: 0/0 bestanden, 0 rot`, Fehler „Gradle brach ab, ohne Tests zu starten"
(bestätigt WORKFLOW.md: Filter ohne Treffer lässt Gradle fehlschlagen). Ersatzmuster:
`--filter "simplebuilding:guide_book_game_test_*"`.

**Testläufe (jeweils einzeln, kein Gradle parallel)**
1. `python3.12 tools/testrunner/run.py --targets fabric-263 --filter guide`
   → `NICHT gruen: 0/0 bestanden, 0 rot` (Filter ohne Treffer, s. o.).
2. `python3.12 tools/testrunner/run.py --targets fabric-263 --filter "simplebuilding:guide_book_game_test_*"`
   → **`alles gruen: 16/16 bestanden, 0 rot`**, Exit 0, Datensatz
   `testing/runs/2026-10-08T12-10-16Z-f3ac.json`, `missing: []`, `unexpected: []`.
   JUnit `mc26_3/fabric/build/junit.xml`: u. a.
   `simplebuilding:guide_book_game_test_the_open_bookmarks_draw_no_color_stripe_on_the_main_line pass`
   und alle 15 bisherigen `guide_book_game_test_*` pass.
3. `python3.12 tools/testrunner/run.py --targets neoforge-263 --filter "simplebuilding:guide_book_game_test_*"`
   → **`alles gruen: 16/16 bestanden, 0 rot`**, Datensatz `2026-10-08T12-11-46Z-4c9d`
   (gleiche Ids wie Fabric – Katalog und Adapter decken sich).
4. `python3.12 tools/testrunner/run.py --targets fabric-262 --filter "simplebuilding:guide_book_game_test_*"`
   → **`alles gruen: 16/16 bestanden, 0 rot`**, Datensatz `2026-10-08T12-13-23Z-96e9`
   (beweist: geteilter Code baut weiter für 26.2, und die alte Linie behält ihren Strich).

**Was NICHT getestet wurde**
- Client-Sicht: keine Pixel-/Klickprüfung, `client-fabric-263` nicht gestartet – ob die Lesezeichen
  ohne Strich gut aussehen, ist ungeprüft (nur die Entscheidung und der Klassennachweis laufen serverseitig).
- Volles Gate `./gradlew check` (997 Tests + checkWiki/checkBalance/checkAtlases) nicht gefahren.
- Forge 26.2/26.3 nicht gebaut/getestet, NeoForge/Forge 26.2 nicht; 1.21.11 nicht (eigener Abzug,
  unverändert), 26.4 nicht (Snapshot-Linie, `-Pmc264=true`).
- Kein Wiki-/Lang-/Texturablauf nötig geprüft: keine davon geändert (`wiki/manual.json`, `en_us.json`,
  `de_de.json`, Texturen unberührt).

**Offene Punkte / nächste Schritte**
- `development` → Production ziehen, sobald der Besitzer abgenommen hat (nicht in diesem Run gepusht,
  kein `git push`, Branch `bp-guides` nur lokal committet).
- Wenn der Besitzer den Strich auch auf 26.2 nicht mehr will: im Port-Run `bookmarkStripe()` auf
  `false` stellen (eine Zeile) statt den Code zu löschen; 1.21.11 hat einen eigenen Bildschirm-Abzug
  (`mc1_21_11/shared/.../GuideBookScreen.java:471`), der dort eigens anzufassen wäre.
