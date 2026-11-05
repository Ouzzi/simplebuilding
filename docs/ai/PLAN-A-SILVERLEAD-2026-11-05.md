# Plan A silverlead05 (05.11.2026): Silberblech, Silberspiegel, Blei-Pfeilspitze

Branch a-silverlead05, Basis claude-wave1, nur 26.3 Fabric. Besitzer-Wünsche 05.11.

## A) Silver Sheet / Silberblech (Simple Ores)
- `SilverSheetBlock extends LeadPlateBlock`: gleiche Platzierlogik (FACING = angeklickte Seite, alle sechs Seiten, braucht keine Stütze), 1 px statt 2 px. `LeadPlateBlock` bekam Form-Map je Instanz (`shapes(t)`), `SHAPES` bleibt für die Bleiplatte.
- Rezept: 3 Silberbarren in einer Reihe -> 12 Bleche (gleiches Verhältnis wie Bleiplatte 3 -> 6 bei halber Dicke). Kein Schalldämpfer (nur Blei).
- Silberspiegel: Rezept formlos Glasscheibe + Silberblech (altes Rahmenrezept entfällt). Entscheidung: Glasscheibe statt Glasblock, weil der Spiegel dünn ist und das alte Rezept sie auch nutzte.
- Textur (vanilla_derived, nie committet): `ores_2026_10_30.sheet_image`, sechs Töne aus dem Silberbarren (Gold-Barren auf Silber-Rampe), weiche Diagonale, hell oben links / dunkel unten rechts; harte Übergänge 6,3 % (A8b-Richtwert ~7 %).
- Härte 3/6 wie Goldblock, Spitzhacke, Eisenstufe.

## B) Blei-Pfeilspitze (Simple Arrows)
- `Tip.LEAD` (hinten angehängt, wegen Ordinal im Netzwerkcodec), Tag `simplearrows:fletching/tip/lead` = `simpleores:lead_nugget` (required:false), Rezept mit Ladebedingung simpleores.
- Werte: Schwerkraft x1,5 (`LEAD_GRAVITY`; 0,05 -> 0,075), Schaden +0,5 ohne Zielbonus. Begründung: Eisen-Nugget gibt +0,5 plus +2 gegen Zombies; Blei ist ebenso schwer zu holen, bekommt aber Nachteil statt Zielbonus. Mit Phantomhaut (x0,7) bleibt es bei x1,05, die Befiederung gleicht den Nachteil also aus; mit End-Rod-Schaft (x0,5) kein Problem. Reine Zahlen, keine Config (wie alle Teile).
- Textur: tip_iron auf die Blei-Rampe umgefärbt (tip_lead.png, wie die anderen Spitzen committet). Tooltip `container.simplearrows.fletching.effect.tip.lead`, Name `item.simplearrows.crafted_arrow.tip.lead`, EN+DE, Wiki-Manual EN+DE.

## Vanilla-Nutzen (A12)
Silberblech: reiner Baustein/Dekor, kein Eingriff. Bleispitze: nur Zusatzteil im Befiederungstisch, Vanilla-Pfeile unberührt.

## Tests
Katalog `OresTests.ALL` + Adapter in `ModuleGameTest`: `silver_sheet_recipe`, `silver_sheet_placement`, `silver_mirror_recipe` (neu). Pfeile: `FletchingTests` (Schaden, Schwerkraft, Tag-Verfügbarkeit) in bestehenden Katalog-Tests. Läufe: Compile, module-simpleores/-simplearrows (fabric + standalone), data_integrity*, `check`.

## Rückfrage
Soll der Spiegel Glasscheibe oder Glasblock nehmen? (gewählt: Scheibe)
