# Inventar aller UIs (Grundlage simplecontainers, 2026-10-08)

Stand claude-wave1, 26.3/shared. Nur **CrucibleScreen** (simplelib) folgt dem N12-Stil (PLAN-CRUCIBLE-N12B: FRAME 5/7, Ecken-Treppe 2 px, Slots 16x16 mit 1-px-Rundung im 18er-Raster, Palette je Tier + INVENTORY + BARREL).

## Container-Screens
| Modul | Screen | Hintergrund | Besonderes | N12-Stil |
|---|---|---|---|---|
| simplelib | CrucibleScreen | Code (box/slot/Palette) | Flammen, Fortschritt je Slot, Fass-Kasten | ja (Referenz) |
| core | TieredChestScreen | Code, Vanilla-Grau | 9–18 Spalten, Tönung Netherit/Enderit, „×N Stapel“ | nein (leicht umbaubar) |
| core | BackpackScreen | PNG backpack/{basic,…}.png + inventory.png | 2x2-Werkbank, Rezeptbuch, TrimStatsPanel | nein |
| core | NetheriteHopperScreen (+ übrige Mod-Trichter: Vanilla-HopperScreen) | Vanilla hopper.png | Filter-Button | nein |
| core | AutoSmitherScreen | PNG auto_smither.png | Fehlerpfeil, Vorlagen-Geister | nein |
| core | FletchingScreen | Vanilla crafting_table.png + Fills | 3 Teile-Slots, Rezeptbuch | nein |
| core | RecipeBookSmithingScreen (ersetzt Vanilla-Schmiedetisch) | Vanilla | Rezeptbuch | nein |
| QoL | LinkedPanel (Zusatzpanel an jedem Container) | generic_54.png + Fills | Scrollbalken | nein |

## Andere Screens
GuideBookScreen (PNG book.png, Tabs), BlueprintScreen (Code, Papier-Optik, Code-Feld, 3D-Vorschau), OctantScreen (popup/background, viele Buttons), BuildingWandScreen (Vanilla-Buttons), TrimReferenceScreen (Scrollliste), simplemodels ModelBrowser (dunkles Code-Theme), simplevisuals ModelBrowser (Vanilla), Config-Screens (Cloth Config / AutoConfig).

## HUD/Overlays
HudPanel (Tooltip-Sprites; Oktant-Entfernung, Messuhr), DoubleJump-Leiste, SpawnElytra-Boost-Leiste, SoulBurn-Filter, TrimStatsPanel (Resonanz, popup/background), simplevisuals VisualsHud, WarmGlowSlotMixin, Tooltips (Rucksack, Guide, Blaupause, Bündel, Erzdetektor, Karten).

## Mixins auf Vanilla-Screens
SmithingScreen ersetzt (GuiSetScreenMixin); Kartografietisch (3D-Blaupause); ContainerScreen (Astral-Gewölbe-Tönung); InventoryScreen (TrimStatsPanel); AbstractContainerScreen (Bündel, WarmGlow, LinkedPanel, Hufeisen, simplemodels); Reittier-Inventar (Hufeisen-Panel PNG); AnvilScreen (ModelBrowser-Knopf); Screen (Modelle-Knopf).

## Stil-Dokumente
PLAN-CRUCIBLE-N12/N12B/N12C (maßgeblich), PLAN-SMITHER-CRAFTER-UI, PLAN-GUIDEUI, PLAN-QOL-VERKNUEPFTE-GUIS, HudPanel-Javadoc.
