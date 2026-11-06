# Plan A-BLOCKS07 (06.11.2026): Pressling-Blöcke, neue Speicherblöcke, Rose-Karotten

Wünsche 06.11. Punkte 2, 3, 6 (/root/sb-tools-a/wuensche-061106.md). Branch a-blocks07, Basis hub b-bundle116. Nur 26.3 Fabric.

## Ist-Zustand
Simple Ores hat Rose Copper (`rose_bronze_*`) und Rose Gold mit Barren, Block, Pressling, Werkzeugen, Äpfeln. Es gab keine Pressling-Blöcke, keine Nuggets, keine Karotten; Netherit-Schrott-, Enderit-Schrott- und Sage-Orb-Block fehlten. Alles Datengetriebene kommt aus `modules/simpleores/tools/gen_data.py`; Texturen sind Vanilla-abgeleitet (`tools/textures/ores_2026_10_30.py`, nie committet).

## Umsetzung
1. Blöcke `rose_bronze_pressling_block`, `rose_gold_pressling_block`, `netherite_scrap_block`, `enderite_scrap_block`, `sage_orb_block`; je 9 Material <-> Block (umkehrbar). Reine Lagerung: kein Rezept schmilzt einen Block. Scrap-Blöcke wie Antiker Schrott (Härte 30, 1200), Diamantspitzhacke, Item feuerfest; Orb-Block wie Smaragdblock, Eisenspitzhacke.
2. Pressling-Texturen neu (Variante A/B, A eingebaut: `PRESS_VARIANT`), Block-Nähte ~8 % hart (A8b). Vorschau `/root/sb-tools-b/previews/a-blocks07-preview.html`, Antwortcode `BLOCKS07|item=..|block=..`.
3. Karotten `rose_bronze_carrot` (7 Hunger) und `rose_gold_carrot` (8), Sättigungsfaktor 1,2 (goldene Karotte 6/1,2), keine Effekte, nicht verzaubert, nicht immer essbar. Rezept 8 Klumpen um eine Karotte wie Vanilla. Begründung: 8 Barren würden die Karotte so teuer wie den Apfel machen. Dafür neu `rose_*_nugget` (9 <-> Barren). Textur = Umfärbung der goldenen Karotte. Tag `c:foods/golden`.
4. Ränge: RarityRules vergibt nichts an Rose-Gold-Dinge (wie die Äpfel); `enderite_scrap_block` gehört zu den rohen Enderit-Dingen (kein LEGENDARY wie `enderite_scrap`).
5. Lang EN/DE, Wiki-Notizen (manual.json), `OresDiscover.DOCUMENTED`, Money (excluded), Tags, Loot, Tab-Arrays, .gitignore der abgeleiteten Texturen.

## Tests
Gametests `storage_block_recipes`, `rose_carrots` (+ Zählung in `registered`), `check_data.py`, `gen_data.py --check`; ein sbt-Lauf: Compile + `simpleores:*` + LibTests Rarity.

## Rückfragen
- Rose-Nuggets neu eingeführt (Besitzer schrieb „Presslinge-Nuggets“): ok oder 8 Barren gewünscht?
- Netherit-Karotte (SimpleBuilding) hat nur 6 Hunger wie die goldene; die Rose-Karotten liegen deshalb knapp darüber.
