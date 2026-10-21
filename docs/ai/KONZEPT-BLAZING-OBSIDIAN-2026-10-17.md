# Konzept: Lohen-Obsidian (Blazing Obsidian) Rework – 17.10.2026

Auslöser (Besitzer 17.10.): „Blazing Obsidian ist immer noch nicht gut“ – stört **Textur und Verhalten**.
Nur Konzept: kein Spielcode, keine Spieltextur geändert. Umsetzung erst nach Besitzer-Wahl.

## 1. Ist-Zustand
- **Block** `simplebuilding:blazing_obsidian` (nur 26.3, `McVersion.ASTRAL_ENCHANTING`): Eigenschaften = weinender Obsidian
  (Härte 50, Diamant-Spitzhacke), Licht 12, `BlazingObsidianBlock`. Verhalten: bei 1/6 je Client-Tick eine `SMALL_FLAME`
  auf freier Oberseite. Sonst **nichts** – kein Effekt, keine Interaktion.
- **Rezept:** 8 Lohenstaub um 1 weinenden Obsidian (`ModRecipeProvider` ~306). **Splitter:** nur Vorschlaghammer, 9 Schläge
  → 9 `blazing_obsidian_chip`, Rückweg 9 Splitter → Block (N55, `SledgehammerChips`). Loot: `dropSelf`.
- **Nutzung:** einzige Funktion = 5x5-Boden direkt unter dem Astral-Tisch (`AstralEnchanting`): schaltet Stufe 40 (oder
  30 Lohenholz-Regale) und mit beiden Stufe 50. Danach ist der Block ein reiner Deko-Block. Außerdem Wiki, JEI-Info, Guide, Lang.
- **Textur** (`n45_obsidian_2026_10_12.blazing_strip`, B1 nahtlos animiert, Commits 38138b9ec/0a4b8e90f): violett getönter
  Obsidian-Grund + 3 Zufallspfad-Adern, 8 Frames interpolate. Historie: c3d3c3ba8 (Block), f5ccb04e3 (A18 im Spiel),
  2e208d649, 38138b9ec (B1 animiert), 0a4b8e90f (Adern dezenter). Bereits entschieden, **nicht erneut vorgelegt**: B1–B3
  (Tränen-Layout als Recolor, Zufalls-Adern, magmaartig).

### Was heute schwach ist
Textur:
1. Adern sind dünne Diagonalen/Zufallspfade → wirken wie Kratzer oder „zerbrochenes Glas“, nicht wie Glut; im 3x3-Verbund
   (Boden 5x5!) ergibt sich ein Fischgräten-Muster.
2. Der Grund ist **violett**, die Glut orange: Der Block liest sich als „weinender Obsidian mit orangen Strichen“, nicht als
   eigenes Material. Wenig Leuchtkraft trotz Licht 12 – der Block sieht dunkler aus, als er strahlt.
3. Nur Linien-Motiv = Recolor-Idee (Besitzer: eigene Formen statt Recolors).

Verhalten:
1. Nur Kosmetik; außerhalb des Astral-Bodens kein Grund, ihn zu bauen oder zu behalten.
2. Die Flamme (SMALL_FLAME) ist dünn und verschwindet im Tageslicht; kein Ton, keine Wärme.
3. Für 9x Lohenstaub + weinenden Obsidian (Teil von 25 Blöcken Boden = 200 Lohenstaub) wenig Gegenwert.

## 2. Varianten Verhalten (kombinierbar, V1 ist die Basis)
Alle nur 26.3 / SB-Hauptmod; Crucible-Anbindung bleibt Soft-Verbindung (Prinzip Modul-Unabhängigkeit: SB registriert beim
Crucible-API, das Modul kennt SB nicht).

### V1 – Wärme & Licht (Atmosphäre)
- Bessere Partikel: aufsteigende Funken (`LAVA`/`ASH`-Mix) an allen freien Seiten, seltenes Knistern (Vanilla-Sound
  `block.fire.ambient`), Dichte höher bei Nacht; unter aktivem Astral-Tisch (Boden komplett) steigen Funken zum Buch.
- Wärmezone: Spieler direkt darüber verliert Gefrier-Effekt (Pulverschnee) und Schnee/Eis angrenzend schmilzt (Licht 12
  tut das teils schon; dokumentieren statt neu bauen). Kein Schaden, kein Feuer.
- Plus: sehr klein, kein Grief-Risiko, passt zu „glüht“. Minus: kein neuer Nutzen, bleibt Deko. Vanilla: nahe an
  Magmablock/Seelenfeuer-Ambient. Balancing: keines nötig. Aufwand: klein (1 Klasse + 1 Sound, kein neuer Typ).

### V2 – Feuer-Interaktion
- Block kommt in den Tag `minecraft:infiniburn_overworld` (wie Netherrack/Magmablock): Feuer auf der Oberseite brennt
  dauerhaft. Dazu Immunität gegen Item-/Lava-Zerstörung (Obsidian ist es schon). Optional: Feuer auf ihm „blau-orange“ ist
  nicht möglich – bewusst weggelassen.
- Plus: ein Satz Vanilla-Logik, sinnvolle Nutzung (ewiges Feuer als Lichtquelle/Räucherstelle, Kamin-Boden), keine neue
  Mechanik. Minus: Feuerausbreitung auf Holz in der Nähe bleibt Spielerrisiko (wie Netherrack); nur Overworld-Tag, im
  Nether schon überall. Vanilla: sehr konform. Balancing: neutral, Block bleibt teuer. Aufwand: ein Tag-JSON + Doku/Wiki.

### V3 – Nutzen: Dauerhitze für Tiegel und Öfen
- Der Block zählt als Hitzequelle **hoch (HIGH)** über `SimpleCruciblesApi.registerHeatSource` (Tiegel unten drunter wie bei
  Lava/Magma); gleiche Anbindung für Ofenmodul falls dort ein Gegenstück existiert (Prüfung offen). Kein Treibstoff nötig.
- Plus: gibt dem Block ein klares Endgame-Ziel außerhalb des Astral-Tischs; passt zu „Lohen“. Minus: Berührt Tiegel-Balance
  (Seelenlava bleibt EXTREME als nächste Stufe); doppelter Wert im Gameplay nötig → Test + Config `blazingHeat`.
  Vanilla: eigene Mod-Mechanik, aber ähnlich Magmablock/Lagerfeuer-Idee. Balancing: Preis (8 Lohenstaub + weinender
  Obsidian) ist vergleichbar mit Lava-Eimer-Dauerbetrieb → fair; Config zum Abschalten. Aufwand: mittel (API-Eintrag, Tests
  Crucible+Loader, Wiki/JEI).

## 3. Empfehlung
**V1 + V2** (Atmosphäre + ewiges Feuer), beide klein und Vanilla-nah; **V3 als Option** erst, wenn der Besitzer dem Block
eine Rolle im Tiegel-System geben will (Balancing/Test größer). Textur: siehe Abschnitt 4, **C3 (Glutkörner)** als sichere
Wahl (liest sich als eigenes Material, kachelt ruhig, passt zu Erz-Optik von Vanilla), **C1 (Kruste)** als
„lavahafteste“, **C2 (Flammenband)** nur wenn der Besitzer eine klare Streifen-Optik will.

## 4. Texturvorschläge (neu, 16x16, je 4 Frames interpolate, frametime 10)
Warmer schwarzer Grund (kein Violett), Tonanteile wie Vanilla-Obsidian (25/29/26/13/7 %); eigene Pixel, kein Vanilla-Pixel.
Generator: `tools/textures/blazing_obsidian_c_2026_10_17.py`. Dateien in `docs/ai/previews/b-blazing/`:

| Vorschlag | Idee | Dateien |
|---|---|---|
| **C1_crust** | abgekühlte Lavakruste: Obsidian-Platten, dünne glühende Nähte, die atmen | `C1_crust.png` (+`.mcmeta`), `_tile3x3_8x.png`, `_frames.png` |
| **C2_flames** | Hitzeband am Fuß, darüber Flammenzungen, Spitzen flackern | `C2_flames.png`, `_tile3x3_8x.png`, `_frames.png` |
| **C3_embers** | Lohenstaub-Körner im Gestein wie Erz, jedes pulsiert in eigener Phase | `C3_embers.png`, `_tile3x3_8x.png`, `_frames.png` |

Vergleich mit Vanilla (Obsidian | Weinender | Magmablock | heutiges Blazing | C1 | C2 | C3, je 3x3): Vanilla-Pixel dürfen
nicht committet werden → nur auf sb-test: `/root/previews/b-blazing/compare.png`.

Kritische Selbstprüfung: **C1** liest sich als Lavakruste, Nähte noch leicht rechtwinklig, nähert sich Magmablock an
(Abstand: viel mehr dunkles Gestein). **C2** ist die eigenständigste Form, aber auf einem 5x5-Boden ergibt sich ein
Streifenmuster alle 16 Pixel (Zeilenband) – als Boden evtl. unruhig, an Wänden stark. **C3** ist am ruhigsten und kachelt
unauffällig; Gefahr: erinnert an Erz (ggf. „Lohenstaub-Erz“-Eindruck, bewusst) und glitzert bei 6 Körnern gleichmäßig.
Keine ist Zufallsader – alle fassen das Motiv anders an als B1–B3.

## 5. Offene Fragen an den Besitzer (ankreuzbar)
- Textur: [ ] C1 Kruste  [ ] C2 Flammenband  [ ] C3 Glutkörner  [ ] keine, neue Richtung: ______
- Verhalten: [ ] V1 allein  [ ] V1+V2 (Empfehlung)  [ ] V1+V2+V3  [ ] nur V3  [ ] nichts ändern
- Falls V3: Tiegel-Hitze [ ] HIGH  [ ] EXTREME  [ ] mit Config-Schalter
- Soll der Boden-Effekt (Funken zum Astral-Buch) nur bei vollständigem 5x5 sichtbar sein? [ ] ja  [ ] immer
- Licht 12 behalten? [ ] ja  [ ] 15 (heller)  [ ] 10 (wie weinender Obsidian)

## 6. Umsetzungsskizze (nach Wahl)
Textur: gewählte Datei nach `mc26_3/overlay/resources/assets/simplebuilding/textures/block/blazing_obsidian.png` (+`.mcmeta`)
und in `astral_enchanting_textures.py` einhängen; Wiki-Textur regenerieren. V1: `BlazingObsidianBlock.animateTick`. V2:
`data/minecraft/tags/block/infiniburn_overworld.json` (datagen Tag-Provider). V3: `registerHeatSource` im SB-Init, Tests
(Tiegel mit Block darunter = HIGH). Prüfung: Gametests 26.3 Fabric/NeoForge/Forge, Klick-Test auf Development.
