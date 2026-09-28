# Vorschlaghammer: Tempo und Abnutzung (Besitzer 2026-09-28)

Vorgabe des Besitzers (Welle 23): Die Spitzhacke bleibt das Hauptwerkzeug. 1x1 etwas langsamer als
die Spitzhacke gleicher Stufe; der Flaechenabbau (3x3, 5x5, 3x3x2, 5x5x2) dauert je Block so lange
wie die Spitzhacke eine Stufe darunter (Beispiel: ein Enderit-Hammer, der 9 Bloecke bricht, braucht
so lange wie eine Netherit-Spitzhacke fuer diese 9 nacheinander); der Hammer nutzt sich schneller ab
als eine Spitzhacke. Der Code-Stand hat Vorrang - die Struktur (Item-Tempo = Spitzhacke seines
Materials, Teiler je Schlag in `BlockStateBaseMixin`) bleibt, nur die Zahlen ruecken zur Vorgabe.

Code: `SledgehammerUtils#miningSpeedDivisor`, `#lowerTierSpeed`, `#lowerTierFactor`,
`SledgehammerItem#mineBlock`, `SledgehammerUsageEvent`. Tests: `SledgehammerTests`
(`sledgehammer_game_test_sledgehammer_area_mines_each_block_like_the_pickaxe_one_tier_below`,
`..._speed_and_block_count_scale_with_its_enchantments`,
`..._bills_two_durability_per_block_and_three_for_the_wrong_tool`,
`..._breaks_the_octant_selection_at_twice_the_area_time_per_block`).

## Regeln

| | vorher | nachher |
|---|---|---|
| 1x1 (Schleichen, oder nur der Ursprung passt) | wie die Spitzhacke gleichen Materials | 1,2-mal so lange (`SINGLE_BLOCK_SLOWDOWN`) |
| Flaeche mit `n` wirklich abgebauten Bloecken | `sqrt(min(n, 25))`-mal ein Block der eigenen Spitzhacke | `n` Bloecke der Spitzhacke eine Stufe darunter, keine Obergrenze |
| Oktant-Auswahl (neu) | - | wie die Flaeche, je Block doppelt so lange (`OCTANT_TIME_FACTOR` = 2) |
| Haltbarkeit je Block | Ursprung 1, jeder mitgenommene Block 2 (3 mit falschem Werkzeug) | jeder Block 2 (`WEAR_PER_BLOCK`), mitgenommene mit falschem Werkzeug 3 |

"Eine Stufe darunter" ist das naechst langsamere Material der Leiter Holz 2, Stein 4, Kupfer 5,
Eisen 6, Diamant 8, Netherit 9, Enderit 10, Gold 12. Gold ist das schnellste Material, seine Stufe
darunter also Enderit (Gold hat die Abbaustufe von Holz, darunter gaebe es nichts; die Tempo-Leiter
ist die einzige Lesart, die fuer alle sieben Haemmer eine Antwort hat).

**Effizienz** zaehlt auf beiden Seiten gleich: Teiler = `n * (s + e) / (s_u + e)` mit dem Tempo `s`
des Hammers auf dem Block, dem Tempo `s_u` der Spitzhacke darunter (auf denselben Block umgerechnet)
und der Abbau-Effizienz `e` des Spielers (Effizienz V: 26). Ein Hammer mit Effizienz baut die Flaeche
also je Block so schnell ab wie die Spitzhacke darunter mit derselben Effizienz; beim 1x1 bleibt es
beim festen Faktor 1,2. Eile, Unterwasser und Luft wirken wie bei Vanilla auf den ganzen Abbau.

## Zahlen: Ticks auf Stein (Haerte 1,5), ohne Effizienz

Spitzhacke: `ceil(30 * 1,5 / Tempo)` Ticks je Block. Flaeche = der ganze Schlag.

| Hammer (Tempo) | Spitzhacke 1 Block | 1x1 vorher | 1x1 nachher | 3x3 vorher | 3x3 nachher | 5x5x2 vorher | 5x5x2 nachher |
|---|---|---|---|---|---|---|---|
| Stein (4) | 12 | 12 | 14 | 34 | 203 | 57 | 1125 |
| Kupfer (5) | 9 | 9 | 11 | 27 | 102 | 45 | 563 |
| Eisen (6) | 8 | 8 | 9 | 23 | 81 | 38 | 450 |
| Gold (12) | 4 | 4 | 5 | 12 | 41 | 19 | 225 |
| Diamant (8) | 6 | 6 | 7 | 17 | 68 | 29 | 375 |
| Netherit (9) | 5 | 5 | 6 | 15 | 51 | 25 | 282 |
| Enderit (10) | 5 | 5 | 6 | 14 | 45 | 23 | 250 |

Vorher war ein 3x3 je Block ein Drittel so teuer wie mit der eigenen Spitzhacke und ein 5x5x2 ein
Zehntel - der Hammer schlug die Spitzhacke in jeder Lage. Nachher lohnt er sich fuer Klicks und
Kontrolle (ein Schlag, eine Flaeche), nicht mehr fuer Zeit: Enderit-3x3 = 9 Netherit-Bloecke
(9 * 5 = 45 Ticks), genau das Beispiel des Besitzers.

## Haltbarkeit

Vorher kostete der angeschlagene Block 1 (Vanillas Werkzeug-Komponente) und jeder mitgenommene 2 -
beim 1x1 nutzte sich der Hammer also genau wie eine Spitzhacke ab, bei vierfacher Haltbarkeit.
Nachher kostet jeder Block 2 (`SledgehammerItem#mineBlock` legt einen Punkt auf Vanillas einen), ein
mitgenommener Block mit falschem Werkzeug 3. Die Haltbarkeitswerte (Basis x 4) bleiben unveraendert:
ein Diamant-Hammer (6244) bricht also 3122 Bloecke statt der 1561 einer Diamant-Spitzhacke - bei
Flaechenabbau braucht er dafuer aber ein Vielfaches der Zeit.

## Oktant-Auswahl

Oktant mit beiden Ecken in der Nebenhand, der Schlag trifft einen Block in dessen Figur: der Hammer
bricht die ganze Figur (`SledgehammerUtils#octantSelection`, dieselben Override-Regeln wie die
Flaeche, Spawnschutz/Claims je Block). Haltbarkeit = Summe ueber alle Bloecke, als waeren sie einzeln
abgebaut; Zeit je Block = 2x die Flaechenzeit je Block; die Risse laufen ueber die ganze Auswahl
(`MultiBlockBreakingSupport`, dieselbe Positionsliste). Grenzen: hoechstens 32 Bloecke je Kante und
4096 Plaetze in der Box - groessere Auswahlen zaehlen nicht, der Hammer baut dann sein normales Feld
ab. Schleichen baut weiter genau einen Block ab.
