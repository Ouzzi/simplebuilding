# Plan B-FOUNDRY32 (Sitzung B, 31.10.2026) - ABN30-13 / WP-23 / HS29-1 Simple Foundry

Ist-Stand: nur Konzept (`docs/ai/KONZEPT-A-WP23-2026-10-29.md`), kein Code, keine Ofen-Teile einer Schmelzanlage in simpleovens (also keine IdMoves). Besitzer ABN30-13 entscheidet S1: eigener Sub-Mod "Simple Foundry" von Simple Ovens. Rest der Rueckfragen S2-S10: Empfehlungen des Konzepts (Namen N-A "Huette", Gussziel G-A Barren-Gefaess, Twist T-A Schichtschmelze, Tankfassung 27 Barren, Dach Pflicht, Erz-Bonus aus).

## Modulzuschnitt
- `modules/simplefoundry` (Fabric 26.3, Pfad wie `simpleoptserver`): `parent=simpleovens`, `bundle=optional`. Ovens listet es unter `optional` und nestet es (`include project(...)`, Supermod-Konzept). Begruendung gegen `required`: Ovens hat NeoForge/Forge-Projekte, Foundry ist bis zur Anforderung Fabric-only (Memory nur-263-fabric); die Bundle-Pruefung (`tools/multimod.py validate_bundle`) verlangt Nesting nur in gemeinsamen Loadern.
- Foundry selbst ist allein spielbar: nur Vanilla + simplelib + framework (kein Klassenimport aus Ovens). Schalter "Enable Simple Foundry" (`enableSimpleFoundry` in `simpleovens-server.json`, spiegelt `SubMods`), eigene Config `simplefoundry-server.json`.

## Umfang Stufe 1 (Prototyp des Konzepts, Abschnitt 11)
Bausteine: Huettenziegel (Huelle), Schmelzkammer (Tank), Huettenpult (Steuerblock + BE), Feuerrost (Heizer, LIT), Gusstuelle (Ausguss, Schicht nach Hoehe), Gussschale (Barren-Gefaess, 9 Barren). Stoffe Gold/Kupfer/Eisen (Erz, Rohstoff, Barren, Nugget, Block, 1:1, Eisen braucht HIGH). Schichtschmelze: Dichtefolge Gold unten, Kupfer, Eisen oben; Hoehe der Tuelle waehlt die Schicht (`Layers.at`, rein, getestet). Flood-Fill nur durch Schmelzkammern, Hülle muss geschlossen sein (Tag `simplefoundry:hull`), Obergrenze `maxChambers` (256, hart 512) und nur geladene Chunks.
Abweichungen vom Konzept (bewusst, offen in Memory): kein Steuerblock-Screen (Bedienung per Rechtsklick: Brennstoff/Erz einwerfen, leere Hand = Status im Chat; Vorschau `/root/sb-tools-b/previews/b-foundry32-preview.html` zeigt UI Variante A/B/C zur Wahl), Pruefung nicht ereignisgetrieben/SavedData sondern nur alle 100 Ticks solange das Pult aktiv ist (O(n), n<=512; ein ruhendes Pult kostet nichts), kein Barren-Gefaess-Renderer (Fuellstand als Blockstate `content`), nur Barren-Guss (kein Block-Modus), Einschmelzen von Geraet/Amethyst/Quarz/Glas/Legierungen = Stufe 3/4.

## A12 Vanilla-Nutzen und Eingriffsstaerke
Nutzen: Erz/Barren in grosser Menge in einem Rutsch verarbeiten (Alternative zu Ofen-Reihen), sichtbarer Mehrblock-Bau, Sichtbarkeit der Schichten. Eingriff: schwach - zusaetzlicher Weg, Ofen/Tiegel/Kessel unveraendert, Ausbeute 1:1 wie Vanilla, keine Vanilla-Rezepte/-Bloecke veraendert, kein Mixin.

## Dateien
`modules/simplefoundry/**` (shared Java, Fabric-Eintrag + GameTest-Adapter, Ressourcen, tools/check_data.py + gen_data.py + Texturskript), `modules/modules.json`, `modules/simpleovens/fabric/build.gradle` (+fabric.mod.json suggests), `OvensConfig` (Schalter), `mc26_3/fabric/build.gradle` (nicht noetig: Ovens nestet es), `integration/enabled-mods.json`, `TcContext`, `tools/audit/module_purposes.json`, `tools/textures/mod_icons.py`, `docs/MODS-UND-FEATURES.md`, `docs/MODSTRUKTUR.*` (modtree), `.gitignore` nicht noetig (eigene Texturen).

## Risiken / Verifikation
Risiko: Mehrblock-Logik und BlockEntity-API von 26.3 nur per Compile pruefbar -> EIN sbt-Lauf (compile + Standalone-Gametests). Gametests: registered, layers (rein), melts/fuels table, scan valid/hole/L-form/cap, desk melt+pour end-to-end, tray cooling, config round trip, sub-mod switch. Nicht getestet wird Client-Optik.
