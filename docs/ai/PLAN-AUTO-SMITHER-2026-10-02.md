# Auto Smither: Ausgabe und GUI (2026-10-04)

## Befund und Plan

- Aktueller Branch: `gpt-smither`; nur hier committen, niemals pushen.
- `AutoSmitherMenu` zeigt einen `NonInteractiveResultSlot` als separate Vorschau.
  Der Block besitzt nur drei Eingaben und wirft Ergebnisse wie ein Crafter aus.
  Die gewuenschte entnehmbare Ausgabe erfordert einen gespeicherten vierten Slot.
- Annahme: Fronttransfer in Behaelter bleibt erhalten; nicht transferierte Ergebnisse
  bleiben im Ausgabeslot. Ein belegter, inkompatibler/voller Ausgabeslot stoppt den
  Schmiedevorgang ohne Verbrauch. Keine entnehmbare, kostenlose Rezeptvorschau.
- Menu, BlockEntity und Block entsprechend anpassen. Ausgabe nimmt niemals Items
  an; Eingaben bleiben nach Vanilla-Rezeptmengen gefiltert. Trichter duerfen von
  jeder Seite ausschliesslich die Ausgabe entnehmen. Komparator zaehlt weiter nur Eingaben.
- Vanilla SmithingMenu: Eingaben (8/26/44,48), Ausgabe (98,48), Inventar (8,84).
  Diese Positionen sind bereits korrekt. SmithingScreen-Labels und Geisterbilder
  gegen die lokal vorhandene 26.3-Klasse pruefen.
- Vanilla-Hintergrund enthaelt den grossen Hammer. Eigene, reproduzierbare Textur
  unter `tools/textures/` bereinigt die Dekoration; freie Vorschauflaeche bleibt leer.
  Irrefuehrendes Crafter-Redstone-Symbol entfernen. Vanilla-Fehlerpfeil nur bei
  drei belegten Eingaben ohne gueltiges Rezept, serverseitig synchronisiert.
- Vorher-/Nachher-Vorschau mit Nearest-Neighbor (16-fach), verlangter Vorschaupfad.
- Regressionstests fuer Klickarten, Ergebnisentnahme, Trichterseiten, vollen
  Ausgang, Rezeptfehler und Layout. Bestehenden Pulstest und Fronttransfer erhalten.
- Wiki EN/DE aktualisieren und generieren/pruefen. Queue und Handoff fortschreiben.

## Verifikation und Risiken

Gefilterte und danach volle Fabric-/NeoForge-26.3-GameTests (Ausgabe `alles gruen`),
26.2 `:compileJava :neoforge:compileJava`, Forge 26.3 Compile und `check -q` im
aktuellen isolierten Worktree. Kein Clientstart. Testzentrale/Itemabdeckung durch
Servertests; Besitzerwelt nicht veraendern. UI-Sichtabnahme im Spiel bleibt offen.
Speicherformat behaelt Eingabeslot-IDs 0..2; neuer Ausgang 3 ist in Altdaten leer.
Gemeinsame Klassen bleiben 26.2-kompilierbar; Feature bleibt hinter AUTO_SMITHER.

## Abgleich und Umsetzung

- Vanilla `NonInteractiveResultSlot` sperrt im vorliegenden Quellstand bereits
  `mayPlace`, `mayPickup`, `safeInsert` und Drag-Modifikation. Die gemeldete
  Einlegemoeglichkeit ist damit im Ausgangsstand nicht reproduzierbar; die
  geforderte Entnahme war dagegen durch den reinen Vorschau-Slot unmoeglich.
- `SmithingScreen` fehlt als Java-Datei in mcsrc263. Abgleich deshalb zusaetzlich
  ueber `javap -c -p` aus dem lokalen 26.3-Client-Jar: Titel (44,15), Fehlerpfeil
  (65,46), Groesse 28x21, Sprite `container/smithing/error`. Inventarlabel (8,72).
  Leere Basis-/Materialicons erscheinen wie Vanilla erst mit passender Vorlage.
  Slotpositionen, Labelpositionen und Iconzyklus waren bereits korrekt.
- Abweichungen behoben: eingebrannter Hammer entfernt; Crafter-Redstone-Symbol
  auf (133,48) entfernt; zuvor fehlenden Rezeptfehlerpfeil bedingt ergaenzt.
  Die bewusst fehlende ArmorStand-Vorschau hinterlaesst eine leere Flaeche.
- Ausgabeslot 3 ist gespeichert, nur entnehmbar und fuer alle Trichterseiten
  erreichbar. Fronttransfer bleibt bestehen; kein freier Item-Auswurf mehr.
  Bereits gefuellter Ausgang muss genuegend kompatiblen Platz bieten, auch
  wenn eine Fronttruhe noch Platz haette. Dadurch kein Verbrauch ohne Reserve.
- GUI-Generator prueft unveraenderte Pixel aller 40 Slotfelder. Vorschau ist eine
  16-fache Layoutmontage mit Beispielbeschriftung, kein Minecraft-Client-Screenshot.
- Wiki-Regeneration holt auch bereits im Ausgangsbranch vorhandene Vorlagen-
  und Schienen-Daten nach; keine fremden Gameplay-Dateien geaendert.
- Erster Filterlauf `2026-10-04T19-23-38Z-9d6f`: NICHT gruen, keine JUnit-Berichte
  wegen falschem Test-API-Namen `ClickType`; auf vorhandenes `ContainerInput`
  korrigiert. Der Runner meldete fuer Gradle Exit 0 trotz Compilefehlern; deshalb
  sind JUnit-Berichte und die abschliessende Verdict-Zeile entscheidend.

## Verifizierte Zwischenstaende

- Filterlauf `2026-10-04T19-35-28Z-6491`: Fabric 4/4, NeoForge 4/4;
  `alles gruen: 8/8 bestanden, 0 rot`. Deckt echte Klickhandler (inklusive
  Offhand und aller drei Drag-Modi), Entnahme, Hopper-Transfer, Seitenregeln,
  Ausgangskapazitaet, Rezeptfehler, Slotlayout und Redstone-Fronttransfer ab.
- `tools/textures/auto_smither_gui.py --check`:
  `Auto Smither GUI: pixels and 40 slot fields OK`.
- `tools/textures/generate_textures.py --check`:
  `OK: 508 Texturen und 9 .mcmeta in 2 Baeumen aktuell`, `TEXTURES_EXIT=0`.
- `wiki/generate.py --all` und `--all --check`: Exit 0,
  `wiki: up to date, everything documented.`
- Vorschau: `C:/Users/o_o/code/minecraft-mods/previews/auto-smither-gui-vorschau.png`.
- Vollstaendiger Serverlauf `2026-10-04T19-40-05Z-7e33`: Fabric 936/936,
  NeoForge 936/936; `alles gruen: 1872/1872 bestanden, 0 rot`.
  In beiden JUnit-Berichten bestanden:
  `test_centre_game_test_the_whole_centre_builds_and_matches_its_plan` und
  `test_centre_game_test_every_mod_item_and_block_has_its_place_in_the_test_centre`.
- Fabric-/NeoForge-Buildressourcen enthalten bytegleich die generierte GUI-Textur.
- Erstes Gesamt-Gate: `GRADLE_EXIT=1` bei `checkModuleData`. Der Sandwich-Generator
  vergleicht Bytes; 373 eingecheckte LF-Dateien lagen durch Windows-Checkout als
  CRLF vor. Alle 373 Inhalte gegen `resources()` verglichen: ausschliesslich
  CRLF-Abweichungen. Nur lokale LF-Normalisierung, kein Git-Inhaltsdiff unter
  `modules/simplesandwiches`. Einzelcheck danach:
  `simplesandwiches: generated resources, 20 items, 16 blocks, EN/DE and wiki valid`.
- Abschliessendes Gate, unveraenderter Gameplay-Code:
  `gradlew.bat :compileJava :neoforge:compileJava -Pforge263=true :mc26_3:forge:compileJava check -q`.
  `GRADLE_EXIT=0`; `checkBalance` prueft 223 Stellen ohne Fehler; alle Modulchecks,
  Wiki-/Atlas-/Jade-Pruefungen und 54 Wiki-Unit-Tests gruen. Vollstaendige Ausgabe
  unter `.ai-runs/smither-gate-retry.log`, erster Versuch unter `.ai-runs/smither-gate.log`.

## Offen

Kein Minecraft-Client gestartet: Sichtabnahme mit echter Schrift, Hover und
animierten Geisterbildern bleibt beim Besitzer. Keine Besitzerwelt veraendert;
Testzentralen-Aufbau ausschliesslich in separaten GameTest-Welten. Keine Ports,
kein Merge und kein Push. Vorbestehendes unversioniertes `.serena/` nicht angefasst.
