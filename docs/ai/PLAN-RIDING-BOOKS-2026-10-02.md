# Simple Riding: beschlossene Buchtexturen (2026-10-04)

## Auftrag und Annahme
Leaping nutzt das alte Besitzer-Doppelsprungbuch, Tailwind das alte Reichweitebuch.
Der Vorschlag bietet mehrere Varianten: Als leicht kontrastverstaerkter Vorschlag
gilt B (+12 %). Auf dessen gerundete Pixel wird nochmals Faktor 1,25 um den
RGB-Mittelwert der deckenden Pixel angewandt. Alpha und Formen bleiben erhalten.
Die Vorschau zeigt A: alt, B: Vorschlag, C: weitere +25 %, jeweils 16-fach.

## Umsetzung
- Neuer Generator `tools/textures/riding_books_settled_2026_10_04.py`, mit reproduzierbarem Check.
- Texturen und passende `item/generated`-Modelle unter `modules/simpleriding/shared/resources/assets/simpleriding/`.
- Auswahl aus `stored_enchantments` wie bei SimpleBuilding, aber ohne dessen
  globale `minecraft:enchanted_book`-Definition zu ersetzen: modulinterner
  Client-Hook am ItemModelResolver, serverseitig testbare Auswahlfunktion.
  Nur normale verzauberte Buecher mit Riding-Verzauberung erhalten ein anderes
  Modell; eigene explizite Itemmodelle bleiben erhalten. Leaping hat bei beiden Vorrang.
- Modul-GameTests und statischer Datencheck pruefen Auswahl und Ressourcenketten.
- Vorschau nach `C:/Users/o_o/code/minecraft-mods/previews/riding-books-settled-vorschau.png`.

## Grenzen und Risiken
Simple Riding besitzt nur 26.3-Projekte, keinen 26.2-Zwilling. Daher kein neuer
26.2-Port; SimpleBuilding/shared bleibt unveraendert. Kein Gameplay.
Client-Hook muss gegen lokale 26.3-Klassen geprueft werden; keine Minecraft-Clients starten.
Sichtabnahme im Spiel bleibt beim Besitzer. Keine neuen Formen oder Palette-Quantisierung.

## Verifikation und Abschluss
Generatorcheck, Modul-Datencheck, Serverziele `module-simpleriding-fabric-263,module-simpleriding-neoforge-263`
(Ausgabe `alles gruen` lesen), `gradlew.bat check -q`, 26.2 Fabric/NeoForge-Compile,
26.3 Forge-Compile. Testzentrale in separaten Server-Testwelten pruefen.
Nur Branch `gpt-books` committen, niemals pushen. Vorgeschriebener Co-Autor.

## Nachweise
- Modulsuiten: Lauf `2026-10-04T16-28-21Z-9a27`, Fabric 39/39, NeoForge 39/39,
  `alles gruen: 78/78 bestanden, 0 rot`. Neuer Test: `riding_game_test_book_models`.
- Generator inklusive unabhaengiger Sollwerte fuer Kontrast/Clamping/Alpha:
  `Riding books: 2/2 pixels, 16x16 and unchanged alpha OK`.
- Modul-Datencheck erfolgreich; vorhandener Texturcheck:
  `OK: 508 Texturen und 9 .mcmeta in 2 Baeumen aktuell`.
- Vorschau am verlangten externen Pfad erzeugt und visuell geprueft.
- Erstes Gate scheiterte beim Lesen des gerade erzeugten Framework-JARs in
  der Projektkonfiguration. Anschliessende ZIP-Integritaetspruefung erfolgreich.
  Ein weiterer Gate-Lauf fand die unten behobene Wiki-Fehlklassifikation.
  Das anschliessende serielle Abschlussgate ist gruen.
- Abschlussgate: `gradlew.bat check -q :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava --max-workers=2 -PwikiPython=C:/Users/o_o/code/simplebuilding/.ai-runs/venv/Scripts/python.exe`, `GRADLE_EXIT=0`.
  Einschliesslich 26.2 Fabric/NeoForge, Forge 26.3 und 51 Wiki-Tests (`OK`).
- Testzentralen: Lauf `2026-10-04T17-43-20Z-1096`, Filter `simplebuilding:*test_centre*`,
  Fabric/NeoForge 26.3 je 8/8, `alles gruen: 16/16 bestanden, 0 rot`.
  Enthalten: kompletter Aufbau, jede Kreativreiter-Wand, jede Item-/Block-Position,
  Stationsszenarien und isolierte Befehlsbloecke. Ausschliesslich separate Testwelten.
  Fuer diesen Serverlauf wurden nur die unbeteiligten Forge-Projekte bei der Konfiguration
  ausgelassen (`org.gradle.project.forge263=false`, `org.gradle.project.skipForge262=true`);
  das anschliessende vollstaendige Gate pruefte Forge wieder mit.
- Kein Minecraft-Client gestartet (Wellenregel); Renderer/Sichtabnahme im Spiel
  und Testzentrale in der Besitzerwelt bleiben offen. 26.2-Modulport bleibt offen.
- Wiki-Abweichung vom Erstplan: Der Modul-Extractor wertete Modell-IDs als echte Items.
  Validiertes `modelOnly` in der Modul-Wiki markiert die beiden Render-Aliase;
  Sprachschluessel, Registry-Exporte und Java-Registrierungen bleiben beweiskraeftig.
  21 Wiki-Modultests gruen, inklusive Schutz vor dem Verbergen echter Items.
  Wiki mit venv --all erzeugt: `WIKI_GENERATE_EXIT=0`; uv --all --check: `WIKI_CHECK_EXIT=0`.
  Keine inhaltlichen Aenderungen an generierten Wiki-Daten oder Bildern.

## Geaenderte Dateien (20)
- `.claude/QUEUE.md`
- `docs/MULTIMOD.md`
- `docs/ai/PLAN-RIDING-BOOKS-2026-10-02.md`
- `modules/simpleriding/fabric/src/main/java/com/simplebuilding/modules/simpleriding/RidingGameTest.java`
- `modules/simpleriding/shared/java/com/simpleriding/RidingBookModels.java`
- `modules/simpleriding/shared/java/com/simpleriding/mixin/client/RidingBookModelMixin.java`
- `modules/simpleriding/shared/java/com/simpleriding/test/RidingBookTests.java`
- `modules/simpleriding/shared/java/com/simpleriding/test/RidingTests.java`
- `modules/simpleriding/shared/resources/assets/simpleriding/items/enchanted_book_leaping.json`
- `modules/simpleriding/shared/resources/assets/simpleriding/items/enchanted_book_tailwind.json`
- `modules/simpleriding/shared/resources/assets/simpleriding/models/item/enchanted_book_leaping.json`
- `modules/simpleriding/shared/resources/assets/simpleriding/models/item/enchanted_book_tailwind.json`
- `modules/simpleriding/shared/resources/assets/simpleriding/textures/item/enchanted_book_leaping.png`
- `modules/simpleriding/shared/resources/assets/simpleriding/textures/item/enchanted_book_tailwind.png`
- `modules/simpleriding/shared/resources/simpleriding.mixins.json`
- `modules/simpleriding/tools/check_data.py`
- `modules/simpleriding/wiki/manual.json`
- `tools/textures/riding_books_settled_2026_10_04.py`
- `wiki/modules.py`
- `wiki/tests/test_modules.py`
