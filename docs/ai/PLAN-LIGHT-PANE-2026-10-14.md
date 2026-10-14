# Plan: Light Pane (Lichtscheibe), Nachtrag 57 (2026-10-14)

Besitzer-Text (`.claude/QUEUE.md`, Nachtrag 57): flacher Block wie ein Itemrahmen, an jede Blockseite; Varianten
normal, transparent, einfarbig, mehrfarbig; Lampenlicht, den Block dahinter erhellen, eine Farbe (texturiert, nicht
grell), Redstone-Farbe nach Staerke, per Rechtsklick wie beim Notenblock einstellbar. Solide: Platzieren an allen
Seiten, Abbau ohne Stuetze, Licht-Updates, Multiloader, Tests. Nur 26.3 (`McVersion.LIGHT_PANES`, auf 26.2 aus).

## Ist-Zustand (grep)
- Flache Waende/Boeden: `GoatHornHolderBlock` (FaceAttached, Wand + Boden), Pads (nur Boden), `PlacedSmallPartsBlock`.
  Kein vorhandener 6-Seiten-Flachblock -> eigene Klasse, aber Muster uebernommen: `updateShape` -> Luft ohne Stuetze
  (`SculkJawBlock`), Wasser wie `WaterloggedPadBlock`, `neighborChanged`/Orientation-Signatur wie `SpawnTeleporterBlock`.
- Licht: `lightLevel(state -> ...)` (Konstruktionslicht, Astral-Lampe, Stehende Stangen). Farben: 16 `DyeColor`.
- Datagen (Fabric, `src/main/java/.../datagen`) schreibt Rezepte/Loot/Tags nach `mc26_3/generated`; Blockstates, Modelle,
  Items, Texturen der 26.3-Linie liegen handgeschrieben (Generator-Skript) in `mc26_3/overlay/resources`.
- Kreativ-Tab-Zeilen: `ModItemGroupsContent.natureRows` + Spiegel in `DataIntegrityTests`; Suchtab: `SearchTabPlacement`.

## Entscheidungen
1. **Bloecke** (alle `LightPaneBlock`, ein `Kind`): `light_pane` (normal), `transparent_light_pane`,
   `<farbe>_light_pane` x16, `redstone_light_pane` (mehrfarbig). 19 Bloecke, 19 Items (BlockItem). Pro Farbe ein eigener Block,
   damit Item, Loot, Rezept und Name wie bei Betten/Haengematten einfach bleiben.
2. **Form/Platzierung:** 16x16x1 px (Dicke wie Itemrahmen), `facing` = Seite, zu der die Scheibe zeigt (6 Werte). Gesetzt wird an
   die angeklickte Seite; Stuetze = Block dahinter mit voller, tragender Flaeche (`isFaceSturdy(FULL)`). `updateShape` gibt
   Luft zurueck, wenn die Stuetze fehlt (faellt als Item ab, Loot = sie selbst). `noCollision` (wie ein Rahmen begehbar),
   Auswahlbox = die 1-px-Platte. `waterlogged`. `pushReaction DESTROY`.
3. **Licht:** Lampe = 15. Blockzustand `lit` (normal/transparent/einfarbig): leuchtet, solange kein Redstone-Signal anliegt
   (Besitzer: "wie eine Lampe"; abschaltbar ist sinnvoll und gibt `lit` einen Zweck). Mehrfarbig: aus ohne Signal, an mit Signal.
   Licht ueber `lightLevel(state)`; die Engine rechnet bei jedem `setBlock` mit geaendertem Licht neu (kein eigener Code).
4. **Transparent (Loesung):** Die Stuetzflaeche wird schon vom Licht 15 auf der Scheiben-Position voll aufgehellt (Flaechen
   werden vom Nachbarfeld beleuchtet). Optisch zusaetzlich: eine durchscheinende weisse Schleier-Textur (Alpha, ab Textur
   automatisch Translucent-Layer in 26.x, kein Render-Layer-Eintrag) ueber der Flaeche = aufgehellte Ueberlagerung der
   Stuetz-Textur. Aus: nur ein feiner Rahmen. Kein Mixin, kein Loader-Code.
5. **Einfarbig:** je Farbe eigene, texturierte Platte (entsaettigt, gedithert, Rahmen), an und aus (dunkler).
6. **Mehrfarbig/Redstone:** Zustand `start` (0..15) und `color` (0..16, 16 = aus). Farbe fuer Staerke s (1..15) =
   `(start + s - 1) mod 16` in Dye-Reihenfolge (Weiss, Orange, ...). Rechtsklick (ohne Schleichen; wie Notenblock auch mit
   Item in der Hand): `start + 1`, Ton + Notenpartikel in der Farbe (kein Text); danach Farbe neu aus dem aktuellen Signal.
   Das Modell haengt nur an `color` (17 x 6 Schluessel statt 3000). Signal = `getBestNeighborSignal`.
7. **Rezepte** (Datagen): Leuchtscheibe 2x = Glasscheibe + 2 Glowstone-Staub; transparent 2x = 2 Glasscheiben + 1 Glowstone-Staub;
   einfarbig = Leuchtscheibe (Tag `simplebuilding:light_panes`: normal + 16 Farben) + Farbstoff (auch umfaerben); Redstone =
   Leuchtscheibe + Redstone. Alle ueber Fortschritt freigeschaltet (Datagen), im Tag `craftable`.
8. **Texturen:** `tools/textures/light_panes_2026_10_14.py` (schreibt Block-/Item-Texturen, Modelle, Blockstates, Items,
   Vorschau, `--check`). Vorschau 8x nach `docs/ai/previews/oc-lightpane/`.
9. **Rest:** Kreativ-Tab (Zeilen `light_panes`, `light_panes_colored`), Suchtab hinter der Konstruktionsleuchte, Lang EN+DE
   (Tooltip nur beim Redstone-Modell: Rechtsklick-Bedienung), Wiki-/Guide-Eintrag (`wiki/manual.json`), `docs/MODS-UND-FEATURES.md`,
   Tag `mineable`-frei (Glas, Handabbau), Testzentrale (Galerie nimmt Bloecke selbst).

## Risiken
- Zustandsanzahl Redstone-Scheibe: 6 x 2 x 17 x 16 = 3264 (vertretbar, Notenblock 800).
- Die Variantenschluessel nutzen Teilmengen der Properties (wie `goat_horn_holder.json`); Modelle fuer Rotation x/y nach Vanilla-Knopf.
- Testzentrale/Discover/Wiki-Checks verlangen Eintraege fuer jeden neuen Block (siehe Verifikation).

## Verifikation
Compile Fabric/NeoForge/Forge 26.3; GameTests `simplebuilding:lightpane*`: Platzieren an allen 6 Seiten, Abfallen ohne Stuetze,
Wasser, Lichtwert je Variante/Signal, Redstone-Farbe je Staerke 1-15, Rechtsklick-Zyklus; dazu `DataIntegrity`/Kreativ-Tab-Test,
Testzentrale-Abdeckung; `wiki/generate.py --all --check --strict`, `module_guides.py --check`, `discover.py --check`,
`check_tooltip_text.py`, `check_missing_textures.py`.
