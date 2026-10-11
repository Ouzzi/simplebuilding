# Plan Holzwerk (Queue Nachtrag 25, Branch `claude-q-wood`)

Drei Besitzer-Punkte aus Nachtrag 25 in einem Zug, nur 26.3 (`McVersion.WOODWORK`), alles in SimpleBuilding
(der Meißel gehört SB). Holzarten: Eiche, Fichte, Birke, Tropen, Akazie, Schwarzeiche, Mangrove, Kirsche,
Blasseiche, Karmesin, Wirr, Bambus (12; `woodwork/WoodKind`).

## Entscheidungen (Agent, Besitzer hat volle Autonomie gegeben)

1. **Kisten** (`<holz>_crate`, Form wie der Komposter, Bretter des Holzes): 8 Plätze (= 8 Stapel), nur Essen
   (`FOOD`-Komponente) plus Tag `simplebuilding:crate_storable` (leer, für Packs). Rechtsklick mit Essen legt den
   ganzen Stapel hinein, leere Hand nimmt den obersten Stapel, Schleichen + leere Hand ein Stück. Trichter
   rein/raus (Vanilla-`Container`), Komparator nach Füllstand, Inhalt fällt beim Abbau heraus, die Kiste droppt sich.
   Füllung: statt fester Füll-Texturen zeichnet ein Block-Entity-Renderer die echten Item-Texturen als Lage
   (oberste zwei Lagen, je bis 3×3), Höhe nach Füllstand – deckt jedes Essen ohne eigene Texturen ab.
2. **Ausgehöhlte Stämme** (`hollow_<stamm>`, `hollow_stripped_<stamm>`): Röhre mit 2-Pixel-Wänden (innen 12×12),
   Achse wie ein Stamm. Kleine Mobs passen physikalisch hindurch. Spieler: Schleichen vor/in einer liegenden
   Röhre längs ihrer Achse → Kriech-Haltung (Mixin `Player#updatePlayerPose`), drinnen hält Vanilla das Kriechen.
   Herstellung: 8 Stämme im Ring → 8, oder Meißel (Steinstufe) auf den entrindeten Stamm (Spachtel/Schleichen zurück; Stämme mit Rinde entrindet Constructor’s Touch schon).
3. **Holzplatten** (`<holz>_sheet`, `stripped_<holz>_sheet`): Vanilla-`IronBarsBlock` (verbindet sich wie
   Glasscheibe/Eisengitter, lässt Licht durch). 6 Röhren (2×3) → 16.
4. **Holz-Kessel** (`<holz>_cauldron`): Inhalt leer/Wasser/Lava/Pulverschnee wie der verstärkte Kessel aus
   simplelib (Vanilla-Interaktionen über Abbildung auf Vanilla-Kessel). Brennbar; Lava hinein → brennt ~3 s und
   wird zur Lavaquelle. Karmesin/Wirr brennen nicht und halten Lava. Rezept: Röhre über Platte (gleiches Holz).
5. **Gemeißeltes Holz** (`carved_<stamm>`): Scherbe in der Nebenhand + Meißel in der Haupthand, Klick auf die
   Seite eines stehenden entrindeten Stamms (oder ein gemeißeltes Holz) → Motiv der Scherbe auf dieser Seite.
   Scherbe bleibt, Meißel −1 Haltbarkeit + Abklingzeit. Droppt sich mit Motiv (`copy_state`).
6. **Texturen ohne Vanilla-Kopien:** Modelle zeigen auf Vanilla-Texturen; die Motive entstehen zur Ladezeit über
   die Vanilla-Atlasquelle `paletted_permutations` aus `entity/decorated_pot/*_pottery_pattern` (Hintergrund
   transparent, die drei Motivfarben → drei dunkle Töne des entrindeten Holzes; nur Paletten-PNGs im Repo).
   Generator: `tools/textures/woodwork_2026_10_09.py` (Modelle, Blockstates, Item-Definitionen, Paletten, Atlas).
7. Brennstoff (26.3 `cooking_fuel`) und Brennbarkeit nur für die nicht-Nether-Hölzer.

## Dateien
`common/src/shared/java/com/simplebuilding/woodwork/*` (Blöcke, Registrierung, Kriechen, Schnitzen, Renderer),
Mixin `HollowLogCrawlMixin`, `FireBlockFlammableInvoker`, BE-Registrierung je Loader, Datagen (Tags, Loot,
Rezepte), Lang EN/DE, Wiki, Testzentrale-Station `woodwork`, `WoodworkTests`.

## Verifikation
Compile 3 Loader (+26.2), `simplebuilding:woodwork*` auf fabric/neoforge/forge-263, Datagen, Wiki-Check,
Client-Screenshot nach `<preview-dir>/wood/`.

## Nachtrag Queue N31 Punkt 4 (Branch `claude-q-crates2`): Kisten überarbeitet

- **Ausrichtung:** `CrateBlock.FACING` (6 Richtungen, Standard `up` = alte Welten unverändert), Platzierung wie das
  Fass (Öffnung zum Spieler), `rotate`/`mirror`. Kollisionsform je Richtung: volle Box minus Innenraum
  (`CrateBlock.interior`), offen zur Öffnung. Inhalt bleibt beim Drehen (gleicher Block, Block-Entity bleibt).
- **Modell:** `template_crate` aus dem Generator: 4 Eckpfosten, je Seite 3 Latten (2 px Lücken), 3 Bodenbretter
  (1 px Lücken), alle Flächen mit den Brettern der Holzart; Blockstate-Drehungen wie das Fass. Die Lücken sind echte
  Geometrie statt Alpha-Cutout – gleich durchsichtig, braucht keine eigenen PNGs und keinen Render-Layer.
- **Renderer:** jedes Item in eigener 4-px-Zelle (Größe 0,24 → 3,8 px, berührt weder Wand noch Nachbar), nur
  Vierteldrehungen, zwei Lagen genau 1 px auseinander, oberste Lage nie unter 1,5 px über dem Boden → kein
  Z-Fighting/Flackern. Das Essen liegt immer auf der untersten Innenseite: Boden (oben offen), untere Seitenwand
  (seitlich), Erdboden unter der Kiste (Öffnung nach unten).
- **Trichter:** Vanilla-`Container` – oben füllt, unten leert in jeder Ausrichtung (Test).
