# Plan A-STORAGE31 (30.10.2026): Rohr-Rezept/-Fenster, Netz-Fenster, Texturen (ABN30-3), Simple Storage

Agent a-storage31, Branch `a-storage31` (Basis a-bundle80). Nur MC 26.3 Fabric. Aufbau auf `PLAN-A-STORAGE30-2026-10-30.md`.

## Ist-Stand
Rohr (4 Stufen), Kabel, Interface, 4 Controller-Kisten und ein Netz-Fenster (Raster 9x5, Sortierung, Scroll per Tasten/Mausrad, Zahlen als Daten-Slot-Paare,
Linksklick Stapel / Rechtsklick halber / Shift) existieren aus a-storage30. Rezept Rohr: Kupfer um Trichter. Filter-Knopf ab der ersten Stufe. Texturen 15-40 % hart.

## A12 Vanilla-Nutzen
(1) Rohr ersetzt Trichterketten ohne Block-Inventar, Netz ersetzt Kistenwände mit Trichtern; (2) ändert nichts an Vanilla-Trichtern/-Kisten, nur neue Blöcke
(Config `chestNetwork` schaltet das Netz ab); (3) kein starker Eingriff.

## Entscheidungen (Agent, begründet)
1. **Rohr-Rezept: 8 Eisenbarren um 1 Truhe -> 4 Rohre** (3x3 Ring). Trichter = 5 Eisen + Truhe für 1 Block mit 5 Plätzen. Das Rohr hat 1 Platz, Tempo wie der
   Trichter, braucht aber lange Strecken: 4 Stück für 8 Eisen + 1 Truhe = ~2 Eisen und eine Viertel-Truhe je Rohr (Trichter 5 + 1 Truhe): billiger je Block, aber nicht
   gratis; die Aufwertungen kosten weiterhin Rissdiamant/Netherit-/Enderitklumpen. Variante „oben/unten Truhe + 7 Eisen“ verworfen (bricht die Trichter-Optik des Rezepts, Ring ist eindeutig).
   Kupferkabel behält sein eigenes Kupfer-Rezept (3 Kupfer + 2 Wolle -> 3). Erste Rohr-Stufe wird optisch Eisen (nicht mehr Kupfer).
2. **Filter ab Verstärkt.** `PipeTier.canFilter()`; die erste Stufe ignoriert Filter-Modus (`mode()`), Knopf im Fenster ausgeblendet; ein gespeicherter Modus
   wirkt nach der Aufwertung. Ein „Filter-Upgrade“-Item gäbe es nur in Simple Building (Modul-Unabhängigkeit) -> nicht genutzt.
3. **Rohr-Fenster wie das Vanilla-Trichter-Fenster:** 176 x 133, Slotreihe y 20, Inventar y 51; ein Platz in der Mitte (x 80), Knöpfe links/rechts.
4. **Netz-Fenster nach ME-Vorbild:** Suchfeld (EditBox; Text geht ohne eigenes Netzwerkpaket als Menü-Klicks `CHAR + Codepunkt`/Zurück/Leeren zum Server, der filtert;
   Wörter gegen Item-ID und Name, `@mod`, Rechtsklick leert), Bildlaufleiste (ziehen, springt per `SCROLL_TO + Zeile`), Fenster 194 breit, kompakte Zahl `ChestNetwork.shortCount`
   (999, 1k, 1.2k, 32k, 5M), Entnahmemengen `ChestNetwork.takeAmount` (Stapel/halber, nie mehr als vorhanden). Die Logik liegt in `ChestNetwork` (testbar ohne Client).
5. **Texturen:** Generator `tools/textures/storage31_2026_10_30.py` (`--report`): weiche Rampen (8-12 Töne), Licht oben links, Bevel; Pfeil per Farbton statt Helligkeitssprung;
   Controller-Truhen = Truhe der Stufe mit entschärften Kanten (`soften`) + Einlage. Fenster haben keine PNG (UiBoxes).

## Dateien
`modules/simplestorage/shared/java/com/simplestorage/{pipe/PipeTier,pipe/ItemPipeBlockEntity,pipe/PipeMenu,network/ChestNetwork,network/ControllerMenu,client/PipeScreen,client/ControllerScreen,test/*}`,
Rezept + Advancement `pipe`, Lang EN/DE, `modules/simplestorage/wiki/manual.json`, Texturen + `.layers.json`, `tools/textures/storage31_2026_10_30.py`, Vorschau.

## Risiken
EditBox/Maus-API-Namen 26.3 (nach bestehendem Code in simplemaps/simplemodels geschrieben, ein Compile am Ende); Client-Sicht (Fenster, Pfeilrichtung) nicht prüfbar auf sb-agent.
Suche matcht Servernamen (Englisch) + ID, nicht die Clientsprache.

## Verifikation
Ein `/root/sbt a-storage31`-Lauf: Compile `:mc26_3:fabric` + `:modules:simplestorage:fabric`, Targets module-simplestorage-fabric-263 + standalone, Generatoren (guides, wiki, discover), `./gradlew check -q`.
