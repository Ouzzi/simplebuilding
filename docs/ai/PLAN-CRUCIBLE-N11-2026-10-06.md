# Plan Crucible Nachtrag 11 (2026-10-06)

Branch `claude-crucible4` (Basis 47f392a63), Teilauftrag Jade/JEI parallel auf `claude-crucible4-gpt`
(Plan dort `PLAN-CRUCIBLE-JADEJEI-2026-10-06.md`), danach Merge hierher. Besitzer-Screenshots images/17 (Kessel-Item),
images/18 (Milchkessel „Empty 1B“).

## Interpretation (Sprachdiktat, im Bericht genannt)
- Bild 17 zeigt den **verstärkten Kessel** als Item: flach die Seitentextur (Vanilla-`block/cauldron` hat keine
  GUI-Transformation; Vanilla nutzt ein 2D-Item `item/cauldron`). „Fass-Textur falsch“ meint dieses Item → eigenes
  2D-Item-Sprite im Vanilla-Kessel-Stil. Zusätzlich das verstärkte Fass näher an die SB-Verstärkte-Truhe (heller,
  Steinplatten-Grau, weiße Eckbeschläge, türkises Schloss).
- Netherit-Fass = simplelib-Stufe zwischen Verstärkt und Enderit, 45 Felder (5 Reihen, wie die Truhen-Stufen
  27/36/45/54), Stil der SB-Netherit-Truhe (Netherit-Braungrau, Gold-Schloss). Aufwertung: ohne SB Axt + 1 Netheritbarren,
  mit SB Vorschlaghammer + 2 Netherit-Nuggets; Verstärkt → Netherit → Enderit (statt Verstärkt → Enderit).

## Umsetzung
1. **Fass-Verbindung** (`CrucibleBarrelBlock`, `CrucibleBarrelBlockEntity`, `gen_resources.py`, Enderit-Modell in
   `tools/crucible_sb_resources.py`):
   - Anbringen: jeder der 6 Schläge zeigt Zerstörungs-Risse am Fass (`ServerLevel.destroyBlockProgress`, Stufe
     done*9/6), beim 6. Schlag gelöscht; Vanilla räumt liegengebliebene Risse nach 400 Ticks selbst.
   - Verbundenes Modell: Fass + Kupfer-Flansch + Rohr/Rinne, die über den Tiegelrand ragt (Elemente bis z=-3).
   - Verbunden: `getContainerSize()` = 9; beim Verbinden fallen Felder 10+ heraus (droppen am Fass); Rechtsklick öffnet die
     Tiegel-UI des Tiegels, an dem es hängt. Fass abbauen → Inhalt droppt, Item = normales Fass (Loot unverändert).
     Tiegel abbauen → Fass wird normal, Inhalt bleibt, Stapel über der normalen Grenze droppen.
2. **Netherit-Fass** (`BarrelTier.NETHERITE`, `LibBlocks/LibItems/LibRegistry`, `CrucibleUpgrades`, SB
   `SledgehammerUpgrades`, Lang simplelib, Loot/Tags/Modelle per `gen_resources.py`, Textur im Generator).
3. **Texturen** (`tools/textures/crucible_art_v2_2026_10_05.py` erweitert, `--check`): Netherit-Fass, verstärktes Fass
   heller, Item-Sprite `simplelib:item/reinforced_cauldron` (Vanilla-Kessel-Silhouette, verstärkte Palette + Türkisband).
   Vorschau `previews/crucible-n11-vorschau.png`.
4. **Verstärkter Kessel erbt** (`ReinforcedCauldronBlock`): eigene Behälter (Kupfer/Enderit/Seelen-Lava) zuerst, sonst
   wird der Inhalt auf den gleichwertigen Vanilla-Kessel abgebildet (leer/Wasser+Stufe/Lava/Pulverschnee+Stufe) und dessen
   Vanilla-Dispatcher (`CauldronInteractions.EMPTY/WATER/LAVA/POWDER_SNOW`, inkl. Einträgen anderer Mods wie Milch)
   ausgeführt; das Ergebnis wird zurückgebildet (Vanilla-Kessel → verstärkter Zustand; fremder Block mit
   Boolean-Property `reinforced` → auf true). Neu: `level` 1..3 (Wasser/Pulverschnee wie Vanilla: Flaschen, Waschen,
   Färben). Seelen-Lava bleibt verstärkt-exklusiv.
   Milchkessel (`MilkCauldronBlock`): Property `reinforced`; leeren/entnehmen führt zurück zum verstärkten Kessel
   (per ID, keine Klassenabhängigkeit), Pick-Block/Loot entsprechend; Modell mit verstärkter Textur.
5. **Crucible-UI v2** (`CrucibleMenu`, `CrucibleScreen`): Inhalt horizontal zentriert (Iron-Stufe war links geklebt),
   Fass-Bereich immer sichtbar (ohne Fass: abgedunkelte Platzhalter + Hinweis-Tooltip), Hitze-Säule und Feuerleiste als
   eingelassene Felder wie Vanilla-Slots, Tooltip auf Hitze/Feuer (Stufe, Tempo, Nachglühen), Abstände nach
   Vanilla-Containern (Titel y6, Raster y18, Inventar-Label 11 px über dem Inventar). Vorschau je Stufe
   `previews/crucible-ui-v2-vorschau.png` (vorher/nachher).
6. **Jade/JEI** – Teilauftrag (siehe oben).

## Risiken
- Vanilla-Interaktionen, die `level.getBlockState(pos)` statt des übergebenen Zustands lesen, sähen den verstärkten Block
  (Vanilla selbst nutzt den Parameter). Fremde Ergebnisblöcke ohne `reinforced`-Property verlieren den verstärkten Kessel.
- Optik nur per Vorschau (kein Client).

## Verifikation
GameTests (simplelib LibTests + SB CrucibleTests): Risse/9 Felder/Tiegel-UI/Abbau beider Seiten, Netherit-Fass-Aufwertung,
Kessel: Wasserflasche/Stufen/Färben/Milch → verstärkter Milchkessel → zurück. Gates: fabric/neoforge/forge-263,
module-simplelib-*, module-simplesandwiches-* inkl. standalone, check -q, Datagen 26.3, Wiki --all/--check,
Generator-`--check`.
