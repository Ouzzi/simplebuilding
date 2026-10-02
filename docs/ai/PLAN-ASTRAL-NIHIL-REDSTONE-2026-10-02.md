# Plan: Astral/Nihil-Redstone wie Vanilla-Draht, Schalter/Lampe, polierte Schachbretter (2026-10-02)

Branch `claude-astral`. Queue-Punkte (Besitzer 2026-10-02 abends): Pulver wie Redstone-Draht, kaputte
Schalter-/Lampentexturen, Schachbrett-Blöcke aus poliertem Astralit/Nihilith/Enderquarz. Kolben-Konzept
getrennt in `PLAN-ASTRAL-KOLBEN-2026-10-02.md`.

## Ist-Zustand (erhoben)
- Logik: `common/src/shared/java/com/simplebuilding/blocks/custom/EndSignalBlock.java`, eine Klasse für
  POWDER/SWITCH/LAMP, Zustände `power` + `enabled`, Polling-Tick alle 2 Ticks, nur horizontale Nachbarn.
- Modelle/Blockstates von Hand in `mc26_3/overlay/resources/assets/simplebuilding/` (kein Datagen):
  - Pulver: `variants` nach `power`, Modell = **eine 16x16-Ebene** mit der Textur `block/astral_redstone`
    (B7b-Variante B = Punkt + vier Arme als *ein* Bild). Ergebnis im Spiel: jedes Pulver ist ein großer
    Stern über den ganzen Block, unabhängig von Nachbarn; das Item zeigt dasselbe Bild flach
    (`item/generated` mit Blocktextur) → „großes, verpixeltes violettes Muster“. **Ursache** = Modell, nicht
    das Bild: Vanilla setzt Punkt und Linien je Verbindung per Multipart zusammen.
  - Schalter: Modell = eine 16x16-Ebene **in 4 px Höhe schwebend**, nur Ober-/Unterseite, keine Seiten,
    passt nicht zur Hitbox (3..13 × 0..4). Item flach aus Blocktextur.
  - Lampe: 14er-Quader mit UV 1..15 auf allen Seiten (Rahmen abgeschnitten), Item flach.
- Vanilla 26.3 (`minecraft-client.jar`): `redstone_wire` Multipart mit `redstone_dust_dot`, `_side0/1`,
  `_side_alt0/1`, `_up`; Texturen `redstone_dust_dot`, `_line0`, `_line1` (grau, per Tint gefärbt),
  Textur-Referenzen mit `force_translucent`. Lampe = `cube_all`.
- Schachbretter: `ModBlocks` (*_QUARTZ_CHECKER, `RotatedPillarBlock`, keine Spawns), Modelle
  `registerMirroredChecker`, Rezept `createCheckerRecipe` (2 Material diagonal + 2 Quarzblöcke → 4),
  Texturen in `tools/textures/generate_textures.py` (`checker_textures`, 8x8-Quarzfeld + Materialfeld).
  Polierte Texturen bestehen bereits aus vier 8x8-Kacheln.

## Umsetzung
### 1. Pulver = Redstone-Draht im eigenen Kanal
- Neue Klasse `EndSignalPowderBlock extends EndSignalBlock` (shared) mit `north/east/south/west`
  (`RedstoneSide`: none/side/up). Switch/Lampe behalten ihre Zustände (keine Blockstate-Explosion).
- Verbindungen (vanilla-gleich, aber nur gleicher Kanal): Pulver/Schalter/Lampe desselben Kanals
  waagerecht → `side`; Pulver des Kanals schräg oben (Nachbar oben, wenn über diesem Pulver kein Leiter)
  → `up` an einer festen Wand, sonst `side`; Pulver schräg unten (Nachbar kein Leiter) → `side`.
  Genau eine Verbindung → Gegenseite ebenfalls `side` (gerade Linie wie Vanilla); keine → Punkt.
  Annahme: Lampen werden sichtbar angebunden (Kanal sonst unsichtbar); kein Punkt/Kreuz-Umschalten per
  Rechtsklick (nicht verlangt).
- Signal: wie bisher je Schritt −1, Schalter liefert die Reichweite (1..15, Server-Deckel), jetzt auch über
  die Schrägen (Treppen) wie Vanilla. Lampen weiter nur waagerecht. Kein Vanilla-Signal rein/raus.
  Verbindungen werden im Polling-Tick nachgeführt (deckt auch schräge Nachbarn ab) und beim Setzen
  (`getStateForPlacement`) direkt berechnet.
- Assets: Blockstate als Multipart (je Teil zusätzlich `power=0` bzw. `1..15` → aus/an), eigene Modelle
  `<pulver>_dot/_side0/_side1/_side_alt0/_side_alt1/_up` (+ `_on`), ohne Tint, fest eingefärbte Texturen
  `<pulver>_dot/_line0/_line1` (+`_on`), aus den Vanilla-Grautexturen über die B7-Rampe (gleiche
  Helligkeitsstufen wie die Besitzerwahl B). Item: eigenes `item/<pulver>.png` aus Vanilla `item/redstone`
  umgefärbt. Generator: `tools/textures/round10_settled_2026_10_02.py` erweitert.

### 2. Schalter und Lampe
- Lampe: `cube_all` (wie Redstone-Lampe), Hitbox voller Block; Item = Blockmodell.
- Schalter: flache Platte 2..14 × 0..2 (`thin_block`-Elternmodell für Inventar), Oberseite zeigt das
  Juwel (4x4 Mitte), Seiten aus dem Rand; Hitbox passend. Item = Blockmodell.

### 3. Polierte Schachbretter
- `polished_astralit_checker`, `polished_nihilith_checker`, `polished_ender_quartz_checker` in `ModBlocks`
  /`ModItems`, Eigenschaften vom polierten Block (Astralit-Licht 5 wie das andere Astralit-Schachbrett),
  Tab-Zeile „checkers“ + Vanilla-Suchtab hinter dem Enderquarz-Schachbrett, Pickaxe-Tag, Beute, Modelle
  (gespiegelt), Rezept 2 polierter Block diagonal + 2 Quarzblöcke → 4, Fortschritt „checkmate“, Guide-Kapitel,
  Lang EN/DE in beiden Bäumen, Textur aus Quarzfeld + den Kacheln der polierten Textur, Vorschau.
- Tests: `quartzCheckersAreMinedByPickaxeAndCraftedFromTheirMaterial`, Tab-Layout, Guide-Liste erweitern.

## Risiken
- Polling-Tick setzt bei jeder Verbindungsänderung den Block neu (nur Client-Update, keine Nachbar-Updates)
  → kein Update-Sturm. Ausgerissene Pulver unter Wänden: `canSurvive` unverändert.
- Bestehende Tests (Kanaltrennung, Reichweite, Abfall nach Abschalten) müssen grün bleiben.
- Lang-/Wiki-/Simple-Money-Preise: neue Items können Abdeckungstests (Preise, Wiki, Guide) auslösen.

## Verifikation
- Neuer GameTest: Pulver-Verbindungen (Punkt, Linie, Ecke, Wand hoch), Signal über Stufe, kein
  Verbinden mit fremdem Kanal/Vanilla-Draht.
- `run.py --targets fabric-263` (+ `neoforge-263`), Compile 26.2 und Forge 26.3, Datagen, Wiki-Check,
  Texturen `--check`, Vorschau-PNGs unter `previews/`.
- NICHT möglich: Sichtprüfung im Client (kein Client starten erlaubt).
