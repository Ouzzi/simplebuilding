# Plan b-arrows02 (Sitzung B, 03.11.2026) – Simple Arrows Stufe 1, Trank-Slot, Lichtpfeil, Gravur

Auftrag: /root/sb-tools-b/b02-auftrag.md, Konzept docs/ai/KONZEPT-B-ARROWS01-2026-11-01.md, Besitzer ARROWS01 (03.11.):
„lichtpfeil, der soll dann despawnen können wie ein normaler pfeil. der trank slot habe ich schon lange requested.“ Queue WE25-9,
VIS24-9 (FLETCH=gravur), ORES31-PFEIL. Basis hub/b-bundle97-gen 7a5cb2046. NUR 26.3 Fabric.

## 1. Ist-Stand
- Befiederungstisch im SB-Kern (`common/src/shared/java/com/simplebuilding/fletching`): 3 Teil-Slots + Ergebnis (4 Pfeile),
  Rezeptbuch mit 3 Reitern, `CraftedArrow`/`CraftedArrowItem` (Teile in `simplebuilding:arrow_parts`), Screen `FletchingScreen`,
  Stil `ModScreenStyle.fletching` (26.3). Kein Trank-Slot (WE25-9 komplett offen), keine Gravur.
- Silberpfeil fertig in Simple Ores (`OresRegistry`, `SilverArrow`, `SilverArrowItem`, Renderer in `OresFabricClient`, Rezept/
  Fortschritt/Tags aus `tools/gen_data.py`, Texturen Vanilla-abgeleitet `ores_2026_10_30.arrow_images`, Config
  `silverArrowUndeadBonus`, Tests `silver_arrow_recipe`/`silver_arrow_undead_bonus`). Kein Modul `simplearrows`.

## 2. Entscheidungen (selbst getroffen, begründet)
- **Modul `modules/simplearrows`** Pflicht-Sub-Mod (`parent: simplebuilding`, `bundle: required`), **nur Fabric** wie Simple
  Machines (Besitzer „nur 26.3 Fabric“); `requires: [simplelib]`, `optional: [simplebuilding, simpleores]`. Allein spielbar:
  Lichtpfeil braucht nur Vanilla; Silberpfeil-Rezept lädt nur mit Simple Ores (`fabric:load_conditions`), ohne Ores fehlt er
  auch im Tab. Schalter „Enable Simple Arrows“ in SB (`enableSimpleArrows`).
- **Silberpfeil-Umzug (Stufe 1):** `simplearrows:silver_arrow` (Item + Entity-Typ, gleiche Werte). IdMoves `simpleores:` →
  `simplearrows:` für ITEM, ENTITY_TYPE, RECIPE `silver_arrow`, ADVANCEMENT `recipes/combat/silver_arrow`. Config
  `silverArrowUndeadBonus` (3, 0–6) zieht nach `config/simplearrows-server.json`; einmalige Übernahme aus
  `simpleores-server.json` (`IdMoves.adoptConfig`). Ores verliert Pfeil-Code, -Daten, -Config-Schlüssel, -Wiki-Eintrag.
  Texturen bleiben Vanilla-abgeleitet (Generator `arrows_derived.py` nutzt `ores_2026_10_30.arrow_images`).
  Folge: Ores auf NeoForge/Forge hat keinen Silberpfeil mehr (Modul Fabric-only, wie Machines Stufe 2).
- **Trank-Slot (WE25-9) im bestehenden Tisch im SB-Kern**, Tisch-Umzug (Stufe 2) NICHT in diesem Durchgang: der Umzug zieht
  Rezepttyp, 3 Rezeptbuch-Kategorien, Menütyp, Datenkomponente `arrow_parts`, 16 Teil-Rezepte, Screen + Stil-Code im Kern,
  Testzentrale und 8 Kern-Tests mit IdMoves nach (Aufwand L, Risiko mittel); der Trank-Slot hängt nicht vom Ort ab und der
  Besitzer wartet. Umzug bleibt Stufe 2 (Memory/Queue).
  - Vierter Teil-Slot, Menü-Index 40 (hinter der Hotbar; die alten Indizes 0–39 bleiben, Tests/Menü-Probe unverändert),
    Container-Index 3. Nimmt Trank, Wurftrank, Verweiltrank mit Wirkung (`PotionContents.hasEffects()`; Wasser/Seltsam nicht).
  - Ergebnis: dieselben 4 Pfeile mit `minecraft:potion_contents` + `minecraft:potion_duration_scale` 0,125 (Vanilla-Tipped-
    Regel: Wirkdauer 1/8). Leerer Trank-Slot = Pfeile wie bisher. Nehmen verbraucht die Flasche ganz (wie Vanillas Tipped-
    Rezept, keine leere Flasche zurück). Keine Config (B14: alle Tisch-Zahlen fest).
  - `CraftedArrow` wirkt wie Vanillas `Arrow`: Treffer → Effekte × Dauerfaktor, Farbe synchronisiert (Partikel im Flug),
    nach 600 Ticks im Boden verliert der Pfeil den Trank (Vanilla). Item-Modell: getönte Vanilla-Spitze
    (`minecraft:item/tipped_arrow_head`, Tint `minecraft:potion`) über den Teilen, nur mit Trank.
  - Lage: Slot über dem Pfeil-Logo (x 107, y 15), leerer Slot zeigt Vanillas Trank-Silhouette `container/slot/potion`.
- **Lichtpfeil** `simplearrows:light_arrow` (Modul, eigenes Item + Entity): Rezept Glowstonestaub / Stock / Feder → 4 (wie
  Vanillas Pfeil). Schlägt er in einen Block ein, setzt er in den Luftblock vor der getroffenen Fläche einen Vanilla-
  Lichtblock `minecraft:light` Stufe 15 (nur in Luft; unsichtbar, ersetzbar). Das Licht gehört dem Pfeil (`light_pos` im
  Speicherstand) und verschwindet, wenn der Pfeil verschwindet: Despawn wie ein normaler Pfeil (1200 Ticks = 60 s im Boden),
  Aufheben, `/kill`, Herausfallen (Block darunter weg). Beim Entladen des Chunks bleibt beides gespeichert. Stecken zwei
  Lichtpfeile im selben Luftblock, bleibt das Licht bis der letzte weg ist. Treffer auf Wesen: kein Licht. Entfernt wird nur
  ein Block, der noch `minecraft:light` ist. Textur Vanilla-abgeleitet (Pfeilspitze in Glowstone-Tönen, A8b-Rampe). Server-Schalter
  `lightArrow` (Standard an, Konzept: jeder neue Pfeil einzeln abschaltbar).
- **Gravur (VIS24-9, FLETCH=gravur):** Variante A (Pfeil mit Federn, schräg) – Besitzer hat kein Motiv angekreuzt, der
  Antwortcode setzt dann A. Nur im 26.3-Stil (`ModScreenStyle.fletching`, `UiSymbols.engrave`) im Freiraum links über der
  Teile-Reihe; Wahl B/C bleibt in der Vorschau offen.

## 3. Dateien
- Neu: `modules/simplearrows/**` (fabric build/mod.json/Entry/Client/ModuleGameTest, shared java SimpleArrows/ArrowsRegistry/
  ArrowsConfig/entity/item/test, Ressourcen, wiki/manual.json, tools/check_data.py), `tools/textures/arrows_derived.py`,
  Preise `simplemoney/.../money/simplearrows/prices.json`.
- Geändert: modules.json, integration/enabled-mods.json, creative_tab_order.json, mc26_3/fabric/build.gradle, SB-Config
  (`enableSimpleArrows`, ConfigOptions, Lang), simpleores (Registry, Client, Config, Tests, gen_data/check_data, Daten, Wiki,
  Discover), vanilla_derived.py, Fletching (Menu, Screen, Stil, CraftedArrow, Item-Modell, Lang, Tests), mod_icons.py, modtree,
  MODS-UND-FEATURES, PRINZIPIEN, QUEUE.

## 4. Tests
- Modul (`module_game_test_*`): `registered`, `old_silver_arrow_ids_move` (Item/Entity/Rezept/Fortschritt), `silver_arrow_recipe`
  (mit Ores) bzw. kein Rezept standalone, `silver_arrow_undead_bonus`, `light_arrow_recipe`, `light_arrow_sets_and_removes_light`
  (Einschlag setzt Licht, Aufheben/Discard entfernt es), `light_arrow_despawns_like_an_arrow` (life 1200 → weg + Licht weg),
  `light_arrow_shared_light`, `light_arrow_falls_out`.
- Kern: `fletching_game_test_potion_slot_makes_tipped_arrows` (nur Trank mit Wirkung, Ergebnis mit Trank, Dauer 1/8,
  Nehmen verbraucht Flasche, Treffer gibt Effekt).
- Echte-Maus-Test (menu_probe/XTest) des Tisch-Menüs mit Trank-Slot + Screenshots für die Vorschau.

## 5. Risiken
- Ein Compile-Lauf: API-Namen gegen das entpackte 26.3-Jar (javap/vineflower) geprüft.
- IdMoves ENTITY_TYPE: Pfeile in alten Welten laden über `IdMovesRegistryMixin` (Entity-Typ-Lookup); Test deckt Registry-Lookup ab.
- Lichtblöcke bei Absturz zwischen Speichern: Licht und Pfeil werden zusammen im Chunk gespeichert, kein Waisen-Licht.
