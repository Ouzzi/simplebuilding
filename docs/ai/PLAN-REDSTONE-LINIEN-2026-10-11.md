# Plan: Astral und Nihil als 2. und 3. Redstone-Linie, Brücken-Redstone (2026-10-11)

Branch `claude-redlines` (von `claude-wave1`), Queue Nachtrag 36. Ersetzt das Kanalmodell aus
`PLAN-ASTRAL-NIHIL-REDSTONE-2026-10-02.md` (eigene Kanäle, Schalter, Lampen, Polling).

## Besitzer-Wunsch (11.10.)
1. Astral-/Nihil-Redstone verhalten sich 1:1 wie Redstone-Draht: Vanilla-Quellen speisen sie, sie
   powern dieselben Blöcke, Abschwächung 15→0, Schrägen, Verbindungsoptik.
2. Drei getrennte Linien (Redstone, Astral, Nihil): kein Draht speist einen Draht einer anderen Linie,
   auch nicht über einen gemeinsam gepowerten Block; keine optische Verbindung zwischen Linien.
3. Brücken-Redstone: Astralitstaub + Nihilitsplitter + Redstone → 3; verbindet sich mit allen drei
   Linien und reicht Signale weiter wie ein gemeinsamer Draht; Textur aus den drei Farben gemischt.
4. Astral-/Nihil-Schalter und -Lampen entfallen; alte Blöcke/Items werden zu Hebel bzw. Redstone-Lampe.
5. Lang EN/DE, Guide/Wiki, JEI-Info, Simple-Money-Preise.

## Ist-Zustand
- `EndSignalBlock` (Pulver/Schalter/Lampe/Kolben, eigene Kanäle, Polling alle 1–2 Ticks,
  `endSignalRange`), `EndSignalPowderBlock` (Verbindungsoptik wie Vanilla, nur eigener Kanal).
- End-Kolben und End-Schienen empfangen ihren Kanal per Polling (`EndSignalBlock.receiverPower`).
- Vanilla 26.3 `RedstoneWireBlock`: `shouldSignal` schaltet nur die *eigene* Instanz stumm, während
  sie `getBestNeighborSignal` liest; Draht-zu-Draht nur über `RedstoneWireEvaluator.getWireSignal`
  (`is(wireBlock)`); Draht powert den Block unter sich und den, auf den er zeigt, per `getDirectSignal`
  (also auch Lampen neben diesem Block), andere Drähte lesen das nicht (eben wegen `shouldSignal`).

## Entscheidung (Technik)
- Neue Klasse `SignalDustBlock extends Block` (shared) mit `Line` {REDSTONE, ASTRAL, NIHIL, BRIDGE}:
  eine Portierung von Vanillas `RedstoneWireBlock` + `DefaultRedstoneWireEvaluator`, aber
  - Draht-zu-Draht nur zwischen *verbundenen* Linien (gleiche Linie oder eine Seite ist Brücke;
    Vanilla-`redstone_wire` zählt als Linie REDSTONE),
  - Block-Signal ohne jeden Draht: direkte Nachbarn, die Draht sind, und Drähte an einem leitenden
    Nachbarblock werden übersprungen (Vanilla-Regel „Draht liest nicht von Draht“, auf alle Linien
    erweitert),
  - Verbindungsoptik: Drähte nur bei verbundenen Linien, sonst Vanilla-Regel (`WireConnect`, je
    Version: 26.3 `shouldRedstoneWireConnectTo`, 26.2 nachgebaut); End-Kolben/-Schienen der eigenen
    Linie wie bisher.
- Vanilla-Draht liest unsere Drähte nicht: ein kleiner Mixin auf
  `RedstoneWireEvaluator.getBlockSignal` zählt „ein Vanilla-Draht rechnet gerade“; solange gibt unser
  Draht weder `getDirectSignal` (über Blöcke) noch `getSignal` (direkt) ab – außer die Brücke direkt an
  einen Vanilla-Draht (dann Leistung −1 wie Draht-zu-Draht). Vanillas `isRedstoneConductor`-Weg bleibt
  sonst unverändert: Lampen, Kolben, Türen, Repeater usw. neben einem von Astral gepowerten Block
  reagieren wie bei Redstone.
- Nicht getrennt (bewusst, dokumentiert): Repeater, Komparator, Fackeln, Beobachter usw. sind neutrale
  Bauteile; ein Repeater, den Astral speist, kann Redstone speisen. Das ist das Brücken-Prinzip von
  Vanilla-Bauteilen und vom Besitzer gewünscht (alle Vanilla-Quellen speisen jede Linie).
- Ohne Mixin auf `RedstoneWireBlock` selbst: auf 26.2 heißt die Klasse anders und Forge patcht
  `getConnectingSide`. Vanilla-Draht verbindet sich optisch nicht mit unserem Draht über
  `shouldRedstoneWireConnectTo` (26.3) bzw. `canConnectRedstone` (Forge); die Brücke bejaht beides.
  Grenze: Vanilla-Draht liest eine Brücke nur waagerecht direkt (nicht über Schrägen); unser Draht
  liest Vanilla-Draht (Brücke) auch schräg.
- Schalter/Lampen raus: Registrierung, Items, Rezepte, Beute, Modelle, Texturen, Tabs, JEI, Guide,
  Wiki, Preise, Testzentrale. `LegacyItemIds.REPLACED_BY_VANILLA`: `astralit_switch`/`nihilith_switch`
  → `minecraft:lever`, `*_lamp` → `minecraft:redstone_lamp` (Items und Blöcke; Forge-Aliase dazu).
- End-Kolben/End-Schienen: empfangen weiter nur ihre Linie (plus Brücke), jetzt aus der Leistung des
  angrenzenden Drahts (der von Vanilla-Quellen gespeist wird). `endSignalRange` entfällt (Vanilla 15);
  `features.endSignals` aus = Astral/Nihil/Brücke leiten nichts. Astral „doppelt so schnell“ (N24)
  entfällt, Drähte schalten sofort wie Vanilla.
- Texturen: Brücke aus den Vanilla-Grautexturen, Pixel reihum (x+y mod 3) aus Astral-, Nihil- und
  Redstone-Rampe; Item ebenso. Generator `tools/textures/end_signal_wire_2026_10_02.py` erweitert.

## Tests
`RedstoneLinesTests` (+ Fabric-Wrapper, Katalog): alle Linienpaare getrennt (direkt und über einen
gepowerten Block), Vanilla-Quellen (Hebel, Fackel, Redstone-Block, Knopf) speisen Astral/Nihil,
Abschwächung 15→0, Astral powert Lampe und Kolben (auch über Block), Brücke verbindet alle drei,
Migration der alten IDs. Bestehende End-Tests auf Redstone-Block statt Schalter umgestellt.

## Verifikation
Compile 3 Loader 26.3 + 26.2; `run.py --filter 'simplebuilding:end_systems*'`, `'simplebuilding:redstone_lines*'`,
`'simplebuilding:data_integrity*'` auf fabric-263, neoforge-263, forge-263; Client-Screenshot einer
Testschaltung; Texturen-/Wiki-Checks.
