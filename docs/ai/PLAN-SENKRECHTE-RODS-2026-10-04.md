# Plan: Senkrechte Stäbe + Hängematte mittig (Besitzer 2026-10-04, Branch claude-rods3)

Wunsch (Sprachdiktat): Stock, Diamantstab, Lohenrute, Böenrute, Knochen „usw.“ senkrecht aufstellbar, 3D wie die
anderen Rods, aber ohne den oberen Klumpen, genau wie die Item-Textur; Hängematten sollen daran hängen können.
Dazu: „Die Hängematten werden immer noch nicht mittig gerendert“ (Screenshot Testzentrale).

## Ist-Zustand (Recherche)
- Aufstellbare Stäbe heute: Vanilla-Blitzableiter/Endstab, Mod `MetalRodBlock` (Eisen/Gold/Netherit/Enderit) = Kopie
  des Blitzableiters (Stiel 2×2×12 + Kopf 4×4×4 = der „obere Klumpen“). Diamantstab ist reines Item.
- Placeables (`PlacedSmallParts`, Haken `ItemUseOnMixin` am Basis-`Item#useOn`): **Schleichen + Rechtsklick auf eine
  Oberseite** legt Kleinteile flach ab (Stock, Knochen, Lohen-/Böenrute stehen schon im Tag `placeable_small`), an Wand
  und Decke als Vorlage, zu einem Häufchen bis 4 Teile. Optionen `placeVanillaItems`/`placeDisabledItems`.
- Hängematte v2 (Basis-Commit, **noch nicht auf master**): Anker = jeder nicht ersetzbare Block mit nicht-leerer
  Kollisionsform (`HammockLayout.isAnchor`) – es gibt **keinen Anker-Tag**; jeder neue Block mit Kollision ist Anker.
  Seil-„tie“ reicht 6 px in den Ankerblock (bis an einen Zaunpfosten 6..10).
- Screenshot: Seil links lang (über eine ganze Zelle waagrecht), rechts kurz → das ist **v1 auf master**: bei 3 freien
  Blöcken lag die 2 lange Matte an einem Ende und ein Seilstück (`kind=span`) füllte die dritte Zelle am Kopfende.
  v2 (in diesem Branch) rechnet Tuch und Seile in Linien-Einheiten mittig; Prüfung siehe unten.

## Entscheidungen (selbst getroffen)
1. **Ein gemeinsamer Block `standing_rod`** mit Zustand `rod` (stick, bone, blaze_rod, breeze_rod, diamond_rod) +
   `waterlogged`. Begründung: kein Block-Entity/Renderer nötig (statische Modelle je Zustand, wie Vanilla-Varianten),
   ein Registereintrag statt fünf, Licht/Klang/Drop/Form direkt aus dem Zustand; kein BlockItem (das Item bleibt das
   Vanilla-/Mod-Item, wie bei den Häufchen). Flag `McVersion.STANDING_RODS` (26.3 true, 26.2 false).
2. **Welche Items**: Stock, Knochen, Lohenrute, Böenrute, Diamantstab. Nicht: Endstab/Blitzableiter/Metallstäbe/Bambus
   (sind schon Blöcke), Pfeile (Spitze + Befiederung, keine Säule; Munition), Werkzeuge/Angeln (eigene Benutzung).
3. **Platzieren** nach Placeables-Konvention: Schleichen + Rechtsklick auf eine **Oberseite** stellt den Stab auf
   (statt ihn flach hinzulegen). Liegend bleibt möglich: an Wand/Decke (Vorlage) und als Teil eines vorhandenen
   Häufchens (Klick auf ein Häufchen legt weiter dazu). Optionen `placeVanillaItems`/`placeDisabledItems` gelten auch
   hier. Ohne Schleichen: normales Item-Verhalten.
4. **Halt**: braucht eine tragende Mitte darunter (wie Häufchen/Kerze) **oder** einen aufgestellten Stab darunter
   (stapelbar → Pfosten auf Seilhöhe für Hängematten). Fällt der Halt weg, fällt der Stab und droppt das Item.
5. **Modell**: Säule aus den Pixeln der Item-Textur (Generator `tools/textures/standing_rods_2026_10_04.py`): die
   diagonale Item-Textur wird pixelgenau „entschert“ (Zeile y, Querkoordinate x+y), Höhe = Zahl der Textur-Zeilen
   (Stock 13 px, Stäbe 14 px, Knochen 14 px), Querschnitt 2×2 px wie der Blitzableiter-Stiel; Zeilen mit breiten
   Enden (Knochen-Gelenke) werden 4×4. Lichtseite der Textur (oben links) auf Nord/West, Schattenseite auf Süd/Ost;
   Deckel aus der obersten/untersten Zeile. Kein Kopfstück.
6. **Hitbox/Kollision**: schmal 6..10 × 0..Höhe (wie Blitzableiter), Kollision = Form (macht ihn zum Hängematten-Anker).
7. **Licht**: Lohenrute 5 (leicht), sonst 0. **Klang**: Holz (Stock), Knochenblock (Knochen), Metall sonst.
   Härte 0,3 (bricht fast sofort, kein Werkzeug nötig), Kolben zerstören, Wasser-logging ja (wie Häufchen).
8. **Hängematten**: kein Tag nötig – Stäbe sind über die Kollisionsform Anker; dafür GameTests. Das Seil-„tie“ wird
   auf 7 px (diagonal 7√2) verlängert, damit es einen 2 px dünnen Stab (7..9) erreicht; an Zaunpfosten verschwindet
   1 px im Pfosten.
9. **Hängematte mittig**: v2-Geometrie im Generator prüfen (Rechnung mit derselben Rotationsmatrix wie das Spiel,
   `CuboidRotation$EulerXYZRotation` = `Matrix4f.rotationZYX`, bestätigt per javap): alle Elemente von Tuch, Seilen,
   Knoten und tie für gerade/diagonal 2–4 in Linienkoordinaten um die Mitte zwischen den Ankern spiegeln; Assert im
   Generator. Restasymmetrien beheben. Java: Weltrechnung (Ankermitte = Tuchmitte, Kopfpunkt = Kopfzelle + headShift)
   für alle Richtungen als GameTest.
10. Testzentrale: neben den Hängematten eine Station „Aufgestellte Stäbe“ (5 Stäbe einzeln + Stock-Doppelpfosten mit
    Hängematte 2 frei). Lang EN/DE beide Orte, Wiki-Notiz `standing_rod`, JEI-Infoseite `standing_rods`.

## Dateien
- `mc26_3/overlay/.../version/McVersion.java`, `common/src/mc26_2/.../version/McVersion.java`: `STANDING_RODS`.
- Neu `common/src/shared/java/com/simplebuilding/blocks/custom/StandingRodBlock.java` (Enum, Zustand, Form, Halt,
  Drops, Pick, Wasser, `tryPlace`).
- `mixin/ItemUseOnMixin.java` (Haken vor den Häufchen), `blocks/ModBlocks.java` (Registrierung).
- Assets Overlay: `blockstates/standing_rod.json`, `models/block/standing_<rod>.json`, `textures/block/standing_<rod>.png`
  (Generator + Vorschau `previews/senkrechte-rods-vorschau.png`).
- `tools/textures/hammock.py` (tie 7 px, Symmetrie-Assert, Modelle neu).
- `dev/testcentre/TestCentreSections.java`, `compat/RecipelessJeiInfo.java`, Lang ×4, `wiki/manual.json`.
- Tests `gametest/StandingRodTests.java` + `src/main/java/.../gametest/StandingRodGameTest.java` + Katalog;
  `HammockTests` (Symmetrie-Rechnung).

## Risiken
- Verhaltensänderung: Schleich-Klick mit Stock/Knochen/Lohen-/Böenrute auf den Boden legte bisher flach hin, jetzt
  stellt er auf (liegend nur noch an Wand/Decke/im Häufchen) – bewusst, im Wiki notiert.
- DataIntegrity-Tests (Blockstates/Modelle/Lang/Loot) müssen den neuen Block kennen (`noLootTable`, Drops per Code).
- 26.2: Flag aus → Block null, Code nur gemeinsame API.

## Verifikation
GameTests: jeder Stab aufstellbar (Schleichen, Oberseite, Item verbraucht im Überleben), ohne Schleichen/Seite nicht,
Häufchen bekommt weiter Teile, Drops (Abbau + Halt weg), Stapeln, Licht nur Lohenrute, Wasser, Pick-Item, Option
`placeDisabledItems`; Hängematte zwischen gestapelten Stöcken bei 2/3/4 frei; Symmetrie-Rechnung. Gates: fabric-263,
neoforge-263, 26.2-Compile, Forge-26.3-Compile, `check -q`, Datagen 26.3, Wiki venv `--all` + uv `--all --check`.
Nicht ohne Client prüfbar: Aussehen im Spiel (nur Vorschau mit derselben Modell-Mathematik).

## Stand nach Umsetzung (2026-10-04)
- Umgesetzt wie geplant (Block `standing_rod`, Generator + Vorschau, Haken im `ItemUseOnMixin`, Testzentrale-Station
  hinter der diagonalen Hängematte, Lang EN/DE beide Orte, JEI-Infoseite `standing_rods`, Wiki-Notizen
  `standing_rod` + Hinweis bei `placed_small_parts`).
- **Hängematte mittig – Befund**: Der Screenshot zeigt **v1 (master)**: bei 3 freien Blöcken hing die 2 Blöcke lange
  Matte an einem Ende, die dritte Zelle füllte ein waagrechtes Seilstück am Kopfende → Seil links eine ganze Zelle
  lang, rechts nur der 6-px-Knoten. v2 (dieser Branch, noch nicht auf master) ist rechnerisch exakt mittig:
  `hammock.py` prüft jetzt alle Eckpunkte (Tuch, Spreizhölzer, beide Seilenden, Knoten, tie) mit der Spiel-Matrix
  (javap: `CuboidRotation$EulerXYZRotation` → `Matrix4f.rotationZYX`, Blockstate-y im Uhrzeigersinn) und bricht bei
  > 0,01 px Abweichung ab: gerade/diagonal 2–4 alle 0,0000 px (Tuch ±15,78 px um die Ankermitte, Seile ±23/31/39 px
  gerade, ±32,5/43,8/55,2 px diagonal). Java-GameTest rechnet Kopfpunkt/Tuchzellen gegen die Ankermitte für alle
  Richtungen. Einzige Änderung am Modell: tie 6 → 7 px, damit das Seil dünne Stäbe (2 px) berührt statt 1 px davor
  zu enden. Folge: v2 muss nach master, dann ist der Besitzer-Befund behoben.
- Abweichung vom Plan: die Datenintegritätstests kennen `standing_rod` jetzt als Block ohne Item/ohne Loot-Tabelle;
  der Placeables-Test legt statt eines Stocks einen Eisenbarren flach hin, der v2-Test legt die Feder zuerst
  (Knochen kommt dazu).
- Tests: 6 neue GameTests (4 `standing_rod_*`, `hammock_*_hangs_between_standing_rod_posts`,
  `hammock_*_cloth_middle_sits_between_the_anchors_in_every_direction`); fabric-263 + neoforge-263
  **alles gruen: 1880/1880**; Compile 26.2 (`:compileJava :neoforge:compileJava`) und Forge 26.3 grün; Datagen 26.3
  (nur `generated/wiki/items.json`); Wiki venv `--all` + uv `--all --check` grün; `check -q -x checkModuleData` grün
  (`checkModuleData` scheitert im Worktree an simplesandwiches-Dateien mit CRLF durch `core.autocrlf` – unberührt).
- Nicht getestet: Client-Sicht (Säulen, Seil am Stab, Licht), Forge-26.3-GameTests (nur Compile).
