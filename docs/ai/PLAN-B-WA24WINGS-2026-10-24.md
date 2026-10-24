# Plan B: Schwingen-Optik und Stufe 3 (WA24-2, 24.10.2026)

Ist: Wither-/Nether-Schwingen (EL24-1) mit Look A (eigene Feder-Grundform). Besitzer: Look A gut, Grundform aber
nahe der Vanilla-Elytra, Muster von A darauf; Stufe 1 und 2 je eigenes Muster, Standard eher wie Stufe 2; Stufe 3 (Item, Optik,
Tempo) soll existieren, aber nicht herstellbar sein (Material später von einem neuen End-Boss).

Plan
1. Generator `tools/textures/wa24wings_textures.py`: Vanilla-Silhouetten (Item-Icon, Flügelplatte der 64x32-Sheet) mit wenigen
   Aussparungen, je Stufe zwei Mustervarianten. Vorschau `/root/sb-tools-b/previews/wa24-2-wings.html` (Vergleich Vanilla/alt).
2. Texturen Stufe 1/2 erst nach Besitzer-Wahl ersetzen (Varianten-PNGs unter `tools/textures/wa24wings_variants/`);
   Stufe 3 (`end_wings`, Variante 1 als Platzhalter-Standard) eingebaut.
3. Stufe 3: `WitherWings.END` (x6 `endWingsSpeedFactor`, 960 Haltbarkeit), Items-/Equipment-/Modell-JSON, Lang, Kreativ-Reiter
   (Modul-Reiter + Vanilla-Tab), Testzentrale ueber `unsorted`/Modulstation. KEIN Rezept, KEIN Loot (check_data prueft das).
4. Queue WA24-7 "End-Boss-Material" nur notiert.
Verifikation: compile 26.3 Fabric, module-simplemobs(-standalone)-fabric-263, data_integrity, test_centre*, Generatoren, check.
