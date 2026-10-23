# Plan a-megamap23 (23.10.): Mega-Wayfinder-Karte (B23-K1 bis K3)

Queue: `.claude/QUEUE.md`, Abschnitt „Besitzer 23.10. nachmittags – … Mega-Karte (B23)“, Zeilen B23-K1..K3.
Basis claude-wave1 ec9d9c7eb. Nur 26.3 Fabric (Regel 22.10.).

## Ist-Zustand
- `WayfinderMapItem` (Kind OVERWORLD/NETHER/END) bindet eine Karte beim ersten Gebrauch an eine Dimension; Daten
  `WayfinderData` je Karten-Id (`MapsComponents.MAP_ID`), Wegpunkte (`WAYPOINTS`) und Anzeige (`DISPLAY`) liegen am Stack.
- Bildschirm, Hand-/Rahmenbild, Locator-Leiste, Ebenen und Kachel-Netz lesen nur `MAP_ID`/`WAYPOINTS` vom Stack.
- Rezepte formlos: Karte + Kompass + 4 Gold + Echo-Splitter (+1 Echo + Netherit-Schrott Nether, +1 Echo + Shulker-Schale End).
- Enderit gehört SimpleLib (`simplelib:enderite_nugget`/`_scrap`, immer registriert); Rezepte damit laufen nur mit der
  Bedingung `simplelib:material_requested` (`enderite`), d. h. wenn ein Simple-Mod Enderit angefordert hat.

## Entwurf
1. **Neues Item `simplemaps:mega_wayfinder_map`** (`MegaWayfinderMapItem extends WayfinderMapItem`, episch, Stapel 1).
   Neue Komponente `simplemaps:mega` = `MegaMaps(selected, entries)`; Eintrag = Dimension, Karten-Id (-1 = noch nicht
   besucht), Art (Kind), Icon-Item, gespeicherte Wegpunkte. Die **gewählte** Dimension spiegelt ihre Karten-Id in
   `MAP_ID` und ihre Wegpunkte in `WAYPOINTS` → alle vorhandenen Pfade (Bildschirm, Hand, Rahmen, Locator, Ebenen,
   Netz) arbeiten unverändert. Umschalten speichert die Wegpunkte im alten Eintrag und lädt die des neuen.
2. **Erkunden**: in der Hand erkundet die Mega-Karte die Dimension, in der der Halter steht (falls freigeschaltet),
   in deren eigene `WayfinderData`; erste Nutzung legt die Id an. Erste Dimension wird automatisch gewählt; danach
   **merkt die Karte die zuletzt gewählte** (kein Auto-Wechsel). „Nur wo man war“: Knöpfe einer Dimension ohne
   Daten sind deaktiviert.
3. **Rezept (K1)**: geformt `NNN / E N W / SSS` – Mitte waagrecht End-, Nether-, normale Wayfinder-Karte, unten
   3 Echo-Splitter, oben 3 Enderit-Nuggets (nur mit Enderit, Bedingung `simplelib:material_requested`); ohne Enderit
   Ersatzrezept mit 3 Enderaugen (Entscheidung: themennah „Finden“, mittlerer Wert, Modul bleibt allein spielbar).
   Beim Herstellen übernimmt die Mega-Karte die drei Karten (Ids + Wegpunkte, `onCraftedBy` liest das Raster),
   damit nichts Erkundetes verloren geht.
4. **Dimensions-Knöpfe (K1/K2)** rechts in der Titelzeile (16×16, Bronze-Stil der Seitenknöpfe, Item-Icon 12 px):
   Gras (Oberwelt), Netherrack, Endstein, dann freigeschaltete Dimensionen mit ihrem Icon. Gewählt = hell + Balken,
   nicht besucht = abgesenkt + ausgegraut (Tooltip „noch nicht besucht“). Klick → Paket `select_dimension`
   (Server prüft Hand, Item, Eintrag, besucht). Nach dem Wechsel zentriert die Ansicht auf den Spieler
   (Nether 1:8 umgerechnet). **Ab 6 Dimensionen**: 5 Plätze je Seite + Chevron links/rechts; leere Plätze der letzten
   Seite sind deaktivierte Platzhalter (reine Logik `MegaTabs`, JUnit + Gametest).
5. **Amboss (K2)**: Mega-Karte + Item → schaltet eine weitere Dimension frei (Kosten `megaUnlockLevels`, Standard 5
   Stufen, 1 Item). Config `megaUnlockItems` = {Dimension: Item-Id | "any"}, `megaUnlockDefault` (Standard "any"):
   „any“ = beliebiger Block, während man in dieser Dimension steht (damit „nur wo man war“). Oberwelt/Nether/End sind
   immer da und nicht per Amboss. Nur existierende Dimensionen. Icon = das eingesetzte Item.
6. **Rezepte (K3)**: End-Wayfinder: mit Enderit `… + simplelib:enderite_scrap` statt Shulker-Schale; ohne Enderit
   2× Netherit-Schrott + Shulker-Schale + Echo-Splitter (statt 2 Echo + Schale). Nether-Wayfinder nur 1 Echo-Splitter.
   Lade-Bedingungen `fabric:load_conditions` (+ gleichlautende NeoForge-Bedingungen wie bei SimpleLib).
7. **Kartentisch**: Mega-Karte nur kopieren (Erweitern/Kombinieren lehnt sie ab – mehrere Dimensionen).
8. **Optik exemplarisch**: Item-Textur per `gen_textures.py` (Varianten A/B/C, A vorläufig eingebaut), Knöpfe
   Variante A eingebaut; Vorschau `/root/sb-tools-a/previews/a-megamap23-preview.html` mit B/C.

## Dateien
`MegaWayfinderMapItem.java`, `MegaMaps.java` (Komponente + Regeln), `MegaTabs.java`, `net/SelectDimensionPayload.java`,
`mixin/MegaAnvilMixin.java`, `MapsItems`, `MapsComponents`, `MapsConfig`, `Cartography`, `WayfinderMapItem` (kind/accepts
je Stack), `client/MapsClient`, `client/WayfinderScreen`, `MapsFabric`, Rezepte/Advancements/Tags/Item-Modelle, Lang EN/DE,
`wiki/manual.json`, `tools/gen_textures.py`, `tools/check_data.py`, `test/MapsTests` + Fabric-Adapter, JUnit `MegaTabsTest`,
`docs/MODS-UND-FEATURES.md`.

## Risiken
- NeoForge/Forge: neues Paket nicht registriert (Regel „nur Fabric“) → beim Port nachziehen (offen vermerkt).
- Rezeptbedingung `fabric:not` um eine SimpleLib-Bedingung: SimpleLib ist Pflicht-Abhängigkeit, also immer registriert.

## Verifikation
Compile `:modules:simplemaps:fabric:compileJava` + `:mc26_3:fabric:compileJava`; Gametests `module-simplemaps-fabric-263`,
`module-simplemaps-standalone-fabric-263` (Filter `simplemaps:*`), `fabric-263` gefiltert; `check_data.py`,
`gen_textures.py --check`, Generatoren (module_guides, wiki --all, discover), `./gradlew check -q`.
Nicht testbar hier: Client-Sicht (keine Client-Läufe erlaubt) → Vorschau + Besitzer-Abnahme.

## Plan-Abgleich
1. Item + Komponente `simplemaps:mega` (`MegaMaps`, `MegaWayfinderMapItem`): umgesetzt wie geplant.
2. Erkunden je Dimension, erste Dimension gewählt, danach gemerkt: umgesetzt (`dataFor`/`inventoryTick`, Gametest `mega_explores`).
3. Rezept K1 geformt `TTT/ENW/SSS` mit Enderit-Nuggets bzw. 3 Enderaugen; Übernahme der drei Karten (`onCraftedBy` → `inherit`,
   Gametest `mega_inherit`). Eine leere (nie benutzte) Karte im Raster bringt nichts mit.
4. Knöpfe Variante A in `WayfinderScreen` (Titel wird mit „…“ gekürzt), Paket `select_dimension`, Zentrieren nach Wechsel
   (Nether 1:8), Blättern + Platzhalter (`MegaTabs`, JUnit `MegaTabsTest`, Gametest `mega_unlock`).
5. Amboss `MegaAnvilMixin` + Config `megaUnlockItems`/`megaUnlockDefault`/`megaUnlockLevels` (Gametest `mega_unlock`).
6. K3: Nether 1 Echo-Splitter; End mit Enderit-Schrott (2 Echo + Enderit-Schrott). **Abweichung:** Ersatz ohne Enderit hat
   10 Teile (Karte, Kompass, 4 Gold, 2 Netherit-Schrott, Schale, Echo) – passt nicht ins 3×3-Raster → 3 statt 4 Gold.
   Gametest `mega_recipes` prüft: genau eine Variante geladen, Nether 1 Echo.
7. Kartentisch: nur Kopie (`mega_cartography`).
8. Optik: Icon A eingebaut (`gen_textures.py`, B/C nur Vorschau), Knöpfe A eingebaut; Vorschau
   `/root/sb-tools-a/previews/a-megamap23-preview.html`, Wahl offen.

Nicht umgesetzt / offen: NeoForge/Forge-Registrierung des Pakets `select_dimension` (Regel „nur Fabric“), Client-Sicht
(keine Client-Läufe erlaubt), Besitzer-Wahl Icon/Knöpfe/Ersatzrezept.
