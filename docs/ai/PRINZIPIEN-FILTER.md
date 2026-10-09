# Prinzip Filter (Besitzer 2026-10-09)

Filternde Blöcke der Simple-Mods (Mod-Trichter, autonomer Crafter, künftige) verhalten sich gleich:

- Im Filtermodus bleibt **immer ein echtes Item fest im Slot** (kein Schatten-/Ghost-Item). Erst das zweite und
  jedes weitere wird verarbeitet bzw. transportiert.
- Ein **Filter-Knopf** mit zwei Modi: *exakt* (gleiches Item mit gleichen Komponenten) und *gleiche Art* (gleiches
  Item bzw. gleicher Tag-Typ). Überall dieselben Knopf-Texturen (Vorbild Mod-Trichter).
- Gemeinsamer Code und Knopf liegen in simplelib (Konsistenz, Modul-Unabhängigkeit).

## Umsetzung (claude-q-hopper, 2026-10-09)

- Knopf: `com.simplelib.api.client.ui.UiFilterButton` (Taste 18x18, Symbole Aus = rotes Kreuz, Exakt = grünes Häkchen,
  Gleiche Art = drei gelbe Quadrate; Beschriftung davor `UiFilterButton.label` = Trichter-Symbol + Doppelpunkt).
  Genutzt von Mod-Trichtern und Autonomem Crafter (26.3, `ModScreenStyle`).
- Logik: `com.simplebuilding.util.ItemFilter` (matches / accepts / movable) mit `HopperFilterMode`. Liegt in
  SimpleBuilding statt simplelib, weil die Trichter in `common/src/shared` auch für 26.2 kompilieren, wo es simplelib
  nicht gibt. Beim Port-Run bzw. sobald ein zweites Modul filtert: nach `com.simplelib.api` umziehen.
- Geister-Items gibt es nicht mehr; alte `GhostItems`-Daten werden ignoriert.
