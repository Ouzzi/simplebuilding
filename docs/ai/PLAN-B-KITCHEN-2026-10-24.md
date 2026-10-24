# Plan b-kitchen24 (24.10.2026): NA24-4, NA24-5, NA24-7

Ist-Zustand: Schneidebrett-Rezept `SXX` (Stock + 2 Holzstufen) je Holz in `modules/simplesandwiches/tools/gen_resources.py`;
Küchenmesser-Textur `knife` aus `tools/textures/sandwiches.py` (`KNIVES['S']`); Butterblock hat Reibung 0,9, keinen Effekt;
Butterscheibe ist ein einfaches Item; Spezialfaser-Rezept `CGA/DAG/GDC` in `modules/simplemoney/generated/.../special_fiber_from_crafting_table.json`
(+ `MoneyTests` Zeile 91, `docs/modules/simplemoney.md`, `wiki/manual.json`).

## NA24-4 Schneidebrett + Messer
- Rezept: ` S `/`SXS`/` S ` (Holzstufe je Holzart Mitte, 4 Stöcke) - die Holzart bestimmt das Brett, daher je Holz eine Stufe statt Tag.
  Kollision: keins (Stock-Kreuz mit Stufe in der Mitte kommt sonst nirgends vor).
- Messer-Textur: im Generator `transpose` (= 90 Grad im Uhrzeigersinn, dann horizontal spiegeln) auf das fertige Sprite.
## NA24-5 Slippery
- Eigener MobEffect `simplesandwiches:slippery` (neutral, Icon 18x18 aus `sandwiches.py`), 3 s (60 Ticks), ohne Partikel.
- Mixin auf `LivingEntity#travelInAir` (computeModifiedFriction, wie `simplemobheads`): mit Effekt mindestens Butter-Reibung (`ModBlocks.BUTTER_FRICTION` 0,9) auf jedem Block.
- Auslöser: `stepOn` des Butterblocks und des Butter-Teppichs (nur serverseitig, nur bei `Sandwiches.on()`); erneuert erst, wenn <1 s Rest bleibt (keine Paketflut).
- Butter-Teppich: neuer Block `butter_carpet` (Vanilla `CarpetBlock`, 1 px, Reibung 0,9); Butterscheibe wird zum `BlockItem` dieses Blocks, Drop = Scheibe.
  Optik exemplarisch: Vorschau `/root/sb-tools-b/previews/butter24-preview.html` (A flach, B Rand eingezogen, C Schmier-Hügel); umgesetzt wird A (Empfehlung: Vanilla-Teppich-Anmutung, nutzt die abgenommene Butter-Textur).
## NA24-7 Spezialfaser
- Zutaten gleich (2 Diamant, 3 Goldnugget, 2 Kupfernugget, 2 Amethyst), neue Anordnung `CDA/GGG/ADC` (180-Grad-symmetrisch, Goldreihe in der Mitte; kollidiert mit keinem Rezept, weil die Zutatenmenge einmalig ist).
## Prüfung
Gametests (Effekt per Butterblock/Teppich, Reibung, Teppich platzieren/abbauen, Rezepte Brett + Faser) auf `module-simplesandwiches-fabric-263`, `module-simplemoney-fabric-263`; Generatoren (gen_resources, Texturen, Wiki, Guides, Discover); `./gradlew check`.
