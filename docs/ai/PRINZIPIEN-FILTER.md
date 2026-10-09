# Prinzip Filter (Besitzer 2026-10-09)

Filternde Blöcke der Simple-Mods (Mod-Trichter, autonomer Crafter, künftige) verhalten sich gleich:

- Im Filtermodus bleibt **immer ein echtes Item fest im Slot** (kein Schatten-/Ghost-Item). Erst das zweite und
  jedes weitere wird verarbeitet bzw. transportiert.
- Ein **Filter-Knopf** mit zwei Modi: *exakt* (gleiches Item mit gleichen Komponenten) und *gleiche Art* (gleiches
  Item bzw. gleicher Tag-Typ). Überall dieselben Knopf-Texturen (Vorbild Mod-Trichter).
- Gemeinsamer Code und Knopf liegen in simplelib (Konsistenz, Modul-Unabhängigkeit).
