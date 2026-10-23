# Plan A: Container-Tooltips Stufe 3 (a-tooltips3, 22.10.2026)

Nur MC 26.3 Fabric. Basis: Stufe 1/2 (Plan B 19.10.), `ContainerTooltips`/`ContainerPreview` in Simple Interfaces.

## Ist-Zustand
- Rucksaecke (Simple Storage): haben schon einen eigenen Inhalts-Tooltip (`BackpackTooltipData`/`BackpackTooltip`, Shift-Raster, simplelib `UiShiftTooltip`). Nichts zu tun, kein zweites Raster.
- Stufen-Truhen (`TieredChestItem`): Truhen-Items tragen wie Vanilla keinen Inhalt -> kein Inhalts-Tooltip moeglich/noetig (nur Slot-/Stapel-Zeilen, vorhanden).
- Stufen-Shulker (`TieredShulkerBoxBlock extends ShulkerBoxBlock`): bekommen das Interfaces-Raster schon (instanceof ShulkerBoxBlock), aber (a) Zaehlungen ueber 99 stehen nur in `simplestorage:container_counts` (Raster zeigt gekappte 64/99), (b) Farbe liegt in `BASE_COLOR`, `getColor()` ist null -> keine Faerbung.

## Umsetzung (gleiche Optik, kein Abnahmeschritt)
1. `ContainerTooltips.applyOversized`: liest die optionale Komponente `simplestorage:container_counts` per Registry-ID + Codec (JSON `[{slot,count}]`) -> keine Modulabhaengigkeit; fehlt sie, nichts.
2. Tönung: `box.getColor()` sonst `BASE_COLOR` des Stacks.
3. Test `tooltip_tier_shulker` (+ Fabric-Adapter): echte Zaehlung 128, Farbe aus BASE_COLOR; ohne Simple Storage trivial gruen.

## Nicht
Neue Optik, Truhen-Item-Inhalt (existiert nicht), Rucksack-Umbau.

## Verifikation
sbt: compile `:mc26_3:fabric` + `:modules:simpleinterfaces:fabric`, Targets module-simpleinterfaces-fabric-263, -standalone-fabric-263, integration-263 (Filter tooltip), Generatoren, `check`.
