# Plan B: Achtel ↔ Vollblock (SP22-1) und Mob-Kopf in den Kopf-Slot (SP22-11), 22.10.2026

Branch `b-octfull22`, Queue „Besitzer 22.10. – Spielbefunde (SP22)“.

## Ist-Zustand
- Achtelzellen: `modules/simpleoctets/.../block/OctetCellBlock.java` (Bits, Abbau, Drops) und `OctetCells.java`
  (`set` normalisiert Teile → reine/gemischte Zelle). Eine volle Zelle (8/8) bleibt heute Achtelzelle.
- Zuordnung Vollblock ↔ Zelle: `MaterialOctetBlock.source()`; SimpleLib `com.simplelib.api.OctetCells`
  (`cellFor`/`baseOf`, registriert von `SimpleOctets.registerCells`, genutzt vom Vorschlaghammer).
- Schleichend abbauen nimmt bei Zellen nur das angeschaute Achtel (`playerWillDestroy` → `destroy` → `getDrops`,
  ThreadLocal `BREAKING`). Vollblöcke (Diamantblock …) baut man heute immer ganz ab.
- Kopf-Slot: SimpleBuilding `ExtraHead` (N49, auch im Rucksack-/B-Menü über `InventoryExtras`); Einsetzen spielt den
  Helm-Ton (`PlayerExtraHeadMixin`, `onEquipItem`). Vanilla- und Mod-Köpfe sind `equippableUnswappable`:
  Rechtsklick in die Luft tut nichts.

## SP22-1 Umsetzung (simpleoctets)
a) **Zusammenfügen:** `OctetCells.set` – 8 Achtel eines Materials, dessen Achtel ein `MaterialOctetItem` ist und das
   einen Vollblock (`source()`) hat → `source().defaultBlockState()`. Ohne Verlust (8 Achtel = 1 Block = Rezept
   `<block>_from_octets`). Nicht: Melone (Scheiben ≠ Achtel, Melone droppt 3–7), Schach-Achtel (Kern; ein Schachbrett
   ist zweifarbig, kein Vollblock einer Farbe), gemischte Zellen. Nur mit eingeschaltetem Sub-Mod (`SimpleOctets.on()`).
b) **Ecke abbauen:** Mixin auf `Block` (gemeinsam für alle Loader):
   - `playerWillDestroy` HEAD: schleichender Spieler, Block hat eine Material-Zelle (SimpleLib `cellFor`, gleiche Regel
     wie a), kein Block-Entity, der Blickstrahl (`player.pick`) trifft genau diesen Block, kein Flächenabbau desselben
     Spielers im selben Tick (Aderabbau/Tunnelabbau/Hammer brechen ihre Nachbarn vorher – dann bleibt Vanilla) →
     Welt kurz zur vollen Zelle, `BREAKING` wie beim Zellenabbau (Rest = 7 Achtel). Vanilla läuft weiter (Partikel und
     Ton des Materials, Piglin-Zorn bei Gold, Game-Event); RETURN gibt die volle Zelle als „adjusted state“ zurück.
   - `destroy` HEAD (nur Nicht-Zellen): legt die 7 Achtel zurück (Vanilla/Fabric und Forge rufen `destroy` am
     Vollblock, NeoForge an der Zelle – beide Wege enden in derselben Methode, doppelt ist harmlos).
   - `playerDestroy` HEAD (`@ModifyVariable`): Forge reicht den Original-Zustand weiter → Drop über die Zelle.
   - **Drop:** was der Vollblock mit diesem Werkzeug droppen würde (Loot-Tabelle, Werkzeugstufe des Vollblocks):
     Block selbst → sein Achtel; ein anderer Block mit Achteln (Stein → Bruchstein) → dessen Achtel; sonst nichts.
     Falsches Werkzeug / Kreativ → kein Drop. Erfahrung: keiner der Vollblöcke gibt XP, ein Achtel auch nicht.
   - Schutz: alle Schutz-Hooks (Fabric `PlayerBlockBreakEvents.BEFORE`, NeoForge/Forge `BreakEvent`, Claims,
     Abenteuermodus `blockActionRestricted`) laufen vor `playerWillDestroy` → abgelehnte Abbauten ändern nichts.
   - Wasser: Rest-Zelle trocken (Vanilla füllt beim Abbau auch keine Stufe).
c) **Werkzeugstufe der Achtel (Folgefehler, gleiche Ursache):** Achtel-Zellen haben nur `mineable/*`-Tags, keine
   `needs_*_tool`; mit Holzspitzhacke fiele ein Diamant-Achtel. `MaterialOctetBlock`: Abbautempo und Drop-Prüfung
   über den Zustand des Vollblocks (`getDestroyProgress` delegiert, `getDrops` prüft `isCorrectToolForDrops(source)`).
d) Doku: `modules/simpleoctets/wiki/manual.json`, JEI-Info EN/DE, Klassen-Javadoc.

## SP22-11 Umsetzung (SimpleBuilding-Kern, dort liegt der Slot)
- `ExtraHead.useInAir(Player, hand)`: Kopf (`#minecraft:skulls` + `#simplebuilding:extra_head_slot`; ohne geschnitzten
  Kürbis – der Besitzer sprach vom Mob-Kopf) in der Hand → wie `Equippable#swapWithEquipmentSlot`: leerer Slot =
  einsetzen, sonst tauschen (Stapel > 1: einer in den Slot, alter Kopf ins Inventar/fallen lassen), gleicher Kopf =
  nichts, Fluch der Bindung im Slot (außer Kreativ) = nichts. Ton: der vorhandene Helm-Ton des Slots.
- Mixin `ItemUseExtraHeadMixin` auf `Item.use` HEAD (Rechtsklick in die Luft; auf einen Block setzt `useOn` vorher
  wie Vanilla). Standalone Simple Mob Heads: kein Kopf-Slot → Vanilla (nichts), entfällt sauber.

## Tests
- `simpleoctets` (OctetTests + Fabric-Adapter): `eight_octets_of_one_material_become_the_full_block`
  (Diamant → Diamantblock, Melone bleibt Zelle, Wasser), `sneaking_breaks_one_octet_off_a_full_block` (Diamantblock:
  Zelle 7, Drop genau 1 Achtel; Stein → Bruchstein-Achtel; ohne Schleichen Vanilla), `corner_break_needs_the_tool_and_creative_drops_nothing`
  (Holzspitzhacke: 7, kein Drop; Kreativ: 7, kein Drop), `corner_break_respects_protection` (Abenteuermodus: nichts).
- Kern MobHeadTests: `extra_head_right_click_in_the_air_puts_on_and_swaps`.
- Targets: module-simpleoctets fabric/neoforge/standalone-fabric, `simplebuilding:*octet*`, `simplebuilding:*head*`,
  `simplebuilding:*vein*`/`*strip*` (Flächenabbau mit Schleichen), forge-263 Compile; Generatoren + `check`.

## Risiken
- Flächenabbau beim Schleichen (Ader-/Tunnelabbau, Hammer) – Blickstrahl- und Tick-Regel, Tests der Kern-Miner.
- Mixin auf `Block`: `defaultRequire 1`, Signaturen auf Fabric/NeoForge/Forge 26.3 gleich (unobfuskiert).
