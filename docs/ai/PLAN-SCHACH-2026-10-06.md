# Schachfiguren, Achtelblöcke, Checker-Stufen/-Treppen (Queue Nachtrag 11)

Branch `claude-chess` (Basis 47f392a63). Nur 26.3 (Flag `McVersion.CHESS`: 26.3 true, 26.2 false), Fabric zuerst,
NeoForge/Forge mit. Keine 26.2-Ressourcen, keine 1.21.11-/26.4-Portierung.

## Ist-Zustand
- 12 Quarz-Checker (`ModBlocks.*_CHECKER`, RotatedPillarBlock): Purpur, Lapis, Schwarzstein, Harz, Netherziegel,
  rote Netherziegel, Nihilit, Astralit, Enderquarz, poliertes Astralit/Nihilit/Enderquarz. Textur 16×16 = 2×2-Felder à 8 px
  (Quarz oben links/unten rechts, Material oben rechts/unten links).
- Ein Checker-Feld ist also genau 0,5 Block groß → ein Schachbrett aus Checkern ist 4×4 Blöcke, ein Feld = ein Viertel
  der Blockoberseite. Daraus folgt das Raster der Figuren (Viertel) und der Achtelblöcke (0,5³).
- Vorbild für „mehrere Teile auf einem Fleck“: `PlacedSmallPartsBlock` + Block-Entity + `PlacedSmallPartsRenderer`
  (Item-Modelle mit `ItemDisplayContext.NONE`), Overlay-Blockstates von Hand (`mc26_3/overlay/resources`).

## Entscheidungen (selbst getroffen)
1. **Farben = 12 Checker + Quarz** (13). Quarz ist die „weiße“ Seite eines Schachspiels (aus dem Quarzblock), die
   übrigen die Checker-Materialfarbe (dunkle Hälfte des Checkers).
2. **Achtelblock** `checker_octet`: *ein* Block für alle Farben: `color` (13) + 8 Bits `o<x><y><z>` + `waterlogged`
   = 6656 Zustände, statisch gebacken (Multipart, ein Eckmodell je Farbe, per x/y-Rotation in alle 8 Ecken, uvlock) –
   kein Block-Entity, kein Renderer, gut für große Bauten. Eine Zelle hat eine Farbe (Mischfarben bräuchten ein
   Block-Entity; bewusst nicht). Items: je Farbe ein `<farbe>_octet` (13), platzieren den einen Block.
   - Platzieren nach Trefferpunkt: Ziel = Trefferpunkt + Flächennormale × 0,25 → Zelle + Achtel. Dieselbe Zelle
     gleicher Farbe wird ergänzt, leere/ersetzbare/Wasser-Zelle wird neu angelegt, fremde Farbe abgelehnt.
   - Wasserbindbar, solange < 8/8; bei 8/8 voller Würfel ohne Wasser. **8/8 bleibt Achtelblock** (keine Umwandlung:
     kein Vollblock hat dieselbe Materialfläche, und Abbau gibt so immer genau die eingesetzten 8 zurück).
   - Abbau: droppt so viele Achtel wie gesetzt (`getDrops`, keine Loot-Tabelle). Schleichen + leere Hand nimmt das
     angeklickte Achtel heraus (wie bei Figuren).
   - Licht: Astralit-Farben leuchten 5 wie ihr Checker (aus `color`).
3. **Figuren** `chess_pieces`: *ein* Block (Block-Entity, 4 Viertel-Plätze à Figur + Drehung, `facing` + `waterlogged`
   = 8 Zustände), gezeichnet vom `ChessPiecesRenderer` (Item-Modell je Figur). Bis zu 4 Figuren pro Block, je eine auf
   einem Checker-Feld. Items: 13 Farben × 6 Figuren × {3D, flach} = 156 (`<farbe>_chess_<figur>[_flat]`). Keine 156 Blöcke.
   - Rechtsklick mit Figur: aufs angeklickte Viertel (Oberseite eines tragfähigen Blocks oder freies Viertel einer
     Figurenzelle). Schleichen + Rechtsklick mit Figur auf belegtes Viertel: ersetzt, alte Figur in die Hand (bei
     Stapel 1) sonst ins Inventar. Schleichen + leere Hand: nimmt die Figur auf. Leere Hand ohne Schleichen: dreht die
     Figur um 90° (Springer ausrichten; kleine Zugabe).
   - 3D: klassische Figur aus Cuboids (Fuß 6×6 px, Höhen 7–14 px), 2D: flacher Spielstein (2 px) mit erhabenem
     Symbol in Akzentfarbe, von oben lesbar. Wasserbindbar. Abbau droppt alle Figuren der Zelle.
4. **Stufen/Treppen** je Checker (24 Blöcke, normale `SlabBlock`/`StairBlock`, Eigenschaften vom Checker,
   Astralit leuchtet 5), Checker-Textur auf allen Seiten.
5. **Rezepte**: Steinmetz Checker → 8 Achtel seiner Farbe, Quarzblock → 8 Quarz-Achtel, Achtel → je 1 Figur (alle
   12 Varianten), Checker → Treppe 1 / Stufe 2; Werkbank Treppe 6→4, Stufe 3→6.
6. Texturen: `tools/textures/chess_2026_10_06.py` leitet je Farbe ein Materialfeld (8×8-Feld des Checkers 2×2 gekachelt,
   Quarz aus dem Lapis-Checker) und ein Akzentfeld (heller bei dunklen, dunkler bei hellen Farben) ab; schreibt auch
   Modelle/Item-Definitionen/Blockstates der Achtel und Figuren ins 26.3-Overlay und die Vorschau
   `previews/schach-vorschau.png`.

## Dateien
- `McVersion` (26.2/26.3): `CHESS`.
- Neu: `chess/ChessColor`, `chess/ChessPiece`, `chess/ChessItems` (Registrierung, Listen), `chess/OctetPlacement`,
  `blocks/custom/CheckerOctetBlock`, `blocks/custom/ChessPiecesBlock`, `blocks/entity/custom/ChessPiecesBlockEntity`,
  `items/custom/CheckerOctetItem`, `items/custom/ChessPieceItem`, `client/render/ChessPiecesRenderer`.
- Registrierung: `ModBlocks`, `ModItems`, Block-Entity + Renderer je Loader (Fabric `ModBlockEntities`/`SimplebuildingClient`,
  NeoForge-/Forge-Registries + Client, 26.3-Forge-Client).
- Datagen: Modelle (Stufen/Treppen), Loot, Block-/Item-Tags, Rezepte.
- Kreativtab (Zeilen je Farbe), SearchTabPlacement falls nötig, JEI-Hinweisseite, Lang EN/DE (beide Orte),
  Wiki `manual.json`, Testzentrale-Station `chess` (Brett 4×4 Checker mit Grundstellung Quarz vs. Schwarzstein, flaches
  Brett, Galerie aller Figuren, Achtel-Beispiele inkl. wassergefüllt), Builder-Fill für Figurenzellen.
- GameTests `ChessTests` + Fabric-Adapter `ChessGameTest`; End-Paletten-Test um Checker-Schnitte bereinigen.

## Risiken
- Zustandszahl Achtel (6656) – vertretbar (Vanilla-Redstone ~1300, Notenblock 1150); Multipart mit 104 Teilen.
- Viele Items (193) → Lang/Modelle generiert; Abdeckung Testzentrale.
- Bestehende Tests mit Checker-Filtern (Testzentrale `blocks` nimmt alle „checker“-Pfade als Boden; Endpaletten-Streuschnitte).
- Renderer nur per Code geprüft (kein Client erlaubt) – Client-Sicht bleibt offen.

## Verifikation
Datagen 26.3, gefilterte Schach-GameTests, `fabric-263` + `neoforge-263` (Zeile „alles gruen“), `check -q`, 26.2-Compile,
Forge-26.3-Compile, Wiki venv `--all` + uv `--all --check`, Texturen `--check`. Kein Client.

## Umgesetzter Stand
- Wie geplant umgesetzt. Abweichungen: Wiki-Renderer (`wiki/model_render.py`) nimmt bei Multipart ohne unbedingten
  Teil den ersten Teil als Bild (sonst hatte `checker_octet` kein Icon); Endpaletten-Test ignoriert Schach-Schnitte
  (prüft `ChessTests`); Testzentrale-Abschnitt `blocks` legt nur noch echte Schachbretter (`*_checker`) als Boden.
- Flache Figuren: 8×8-Spielstein (ganzes Feld, Ecken gekappt) mit 6×6-Symbol im Akzent; Akzent halb geglättet und
  deutlich heller/dunkler, damit das Symbol von oben lesbar ist (Vorschau Spalte C, Draufsicht).
- Lang über `tools/chess_lang_2026_10_06.py` (EN/DE, beide Bäume), Modelle/Texturen/Blockstates über
  `tools/textures/chess_2026_10_06.py` (`--check` vorhanden). `craftable`-Tag um alle 193 Items ergänzt.
- Keine GPT-Auslagerung: die Fleißarbeit (Lang, 156 Modelle, Rezepte) läuft über Schleifen/Generatoren.

## Offen
- Client-Sicht (Renderer-Ausrichtung, GUI-Darstellung der Figuren/Achtel, Multipart-Rotation der Achtel) – kein Client gestartet.
- Forge-26.3 nur kompiliert, keine Forge-Server-Tests.
