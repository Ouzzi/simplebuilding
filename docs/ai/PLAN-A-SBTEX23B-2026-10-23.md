# Plan a-sbtex23b (W23-1 Texturen einbauen, W23-2 Trübe Erze) – 23.10. abends

Basis: Bündel 50 + claude-wave1, Branch `a-sbtex23b`. Nur MC 26.3 Fabric. Vorgänger: `docs/ai/PLAN-A-SBTEX23-2026-10-23.md`
(Kandidaten in `tools/textures/sbtex23_2026_10_23.py`). Vorschau: EINE Seite `/root/sb-tools-a/previews/a-sbtex23b-preview.html`.

## W23-1 Besitzer-Wahl einbauen (T5=A, T6=C versteckter, T7=C, T8=B, T9=B)
| Punkt | Ziel-Generator | Ausgabe |
|---|---|---|
| T5 A Vanilla-Aufbau | `end_redstone_blocks_2026_10_13.py` zeichnet über `sbtex23.rs_vanilla` | Overlay-PNG + `.layers.json` (layers.save), 3 Blöcke |
| T6 C versteckt | neuer Teil in `sbtex23` (`ore_hidden(kind, grade)`) + `derived(jar)` | Endstein-Basis = Vanilla → `vanilla_derived.OUTPUTS` (`block/astralit_ore.png`, `block/nihilith_ore.png`), alte Repo-PNG + Ein-Ebenen-Quelle weg, `.gitignore` |
| T7 C Fliesen + Wellenring | `end_pistons_2026_10_03.py`: neue Variante `W` (= sbtex23 `piston_waves`), `CHOSEN = "W"` | eigene Pixel → wieder im Overlay (4 PNG + Ebenen); aus `OUTPUTS`, `.gitignore`, `WIKI_RENDERS` |
| T8 B Vier Scheiben | `texture_round7_2026_10_06.py`: `construction_light_strip()` = sbtex23 `construction_light('B', …)` | 12 Bilder, frametime 4, Ebenen je Schritt über den ganzen Streifen |
| T9 B geschliffen | `n46_round3_2026_10_12.game_chests()`: Astral-Motiv 6×6 bei x 46–51 / y 36–41 | vanilla_derived (unverändert der Weg) |

T6 „versteckter“ (Besitzer): gleiche Adern wie C, aber weniger Kontrast, kein/kaum Schein, kleinere sichtbare Erzfläche.
Zwei Abstufungen als Vorschau: **C1 gedämpft** (Adern gekürzt, Schein halb, Glanzpunkte gedämpft) und **C2 versteckt** (nur kurze
Splitter, Farben nah am Endstein, kein Schein, ein Glanzpixel je Ader) → C2 vorläufig eingebaut. Hinweis: Astralit-Erz hat Lichtstufe 5
(Mechanik) – verrät es im Dunkeln weiter; nicht geändert, als Frage vermerkt.

## W23-2 Trübe Erze (Fake-Astralit / Fake-Nihilit)
- Blöcke `dull_astralit_ore` „Trübes Astralit-Erz“, `dull_nihilith_ore` „Trübes Nihilit-Erz“ (McVersion.DULL_ORES, 26.3 an, 26.2 aus).
  Wie die echten (Endstein-Klang, Diamant-Spitzhacke nötig), aber „unraffiniert“: etwas weicher (15 statt 20 bzw. 20 statt 25),
  weniger XP (1–3), trübes Astralit leuchtet schwächer (3 statt 5) – feine Hinweise für aufmerksame Spieler.
- Weltgenerierung (26.3 `ModWorldGen`): **dieselbe Ader** enthält echtes und trübes Erz. `OreFeature` mit zwei Zielen:
  zuerst `random_block_match(end_stone, p_trüb)` → trübes Erz, sonst `random_block_match(end_stone, p_echt)` → echtes Erz, Rest bleibt
  Endstein. Platzierungen (Zahl der Versuche, Höhen, Filter) unverändert; nur die Adergröße steigt.

### Rechnung (Nachbildung von OreFeature 26.3, 10⁶ Adern je Fall, im echten Platzierungs-Umfeld)
| Erz | Umfeld | alt: Größe → Ø Blöcke | neu: Größe → Ø Endstein-Treffer E | p_trüb = 0,25·alt / E | p_echt = alt / (E − 0,25·alt) |
|---|---|---|---|---|---|
| Astralit | Oberfläche (Endstein unter dem Anker) | 4 → 1,888 | 5 → 2,913 | 0,162 | 0,773 |
| Nihilit | Unterseite (Endstein ab Ursprung −1) | 5 → 1,579 | 6 → 2,030 | 0,194 | 0,966 |

Ergebnis je Ader im Erwartungswert: echtes Erz = alt (1,888 bzw. 1,579), trübes = ¼ davon → Anteil trüb = 20 % aller Erzblöcke.
Größe 5/6 bleibt < 8: jeder Kugelradius < 1, die Ader reicht weiter nur Ursprung −3 … Ursprung (Astralit) bzw. U−2 … U+1
(Nihilit) – die dokumentierte Höhenlogik gilt unverändert. Ganzzahlige Größe allein trifft 20 % nicht (Größe 5 ergäbe 35 % bei
gleicher Echtmenge), daher die zweite Wahrscheinlichkeit; bei Astralit bleiben ~19 % der Aderfelder Endstein (lockere Ader).

### Drop – 3 Vorschläge (Vorschau), eingebaut A (austauschbar: nur Loot-Tabelle + Rezept)
- **A ★ Trüber Staub / Trüber Splitter** (`dull_astralit_dust`, `dull_nihilith_shard`): Drop wie echtes Erz (Glück wirkt, Behutsamkeit
  = Block); 4 trübe → 1 echtes (Werkbank, formlos). Wert ¼, fühlt sich wie „unraffiniert“ an, ohne Fremdmodul.
- **B Endschlacke**: ein gemeinsames Item, kein Glück-Bonus, wertlos bis auf Brennstoff/Dünger – reine Täuschung.
- **C Rohes Bruchstück + Tiegel**: unraffiniertes Bruchstück, im Tiegel (Simple Crucibles, nur wenn geladen) 2 → 1 echtes veredelt;
  ohne Tiegel wertlos (Modul-Unabhängigkeit: Rezept bedingt).

### Texturen der trüben Erze (2 Varianten, vanilla_derived wegen Endstein-Basis)
- **F1 ★ trüb**: Aderform wie das echte (C2), Kristalle entsättigt/grau-milchig, ohne Glanzpixel.
- **F2 verunreinigt**: dieselben Adern in Bruchstücke zerfallen, ockerbraune Einschlüsse.
- Items: trüber Staub / Splitter aus den eigenen Item-Texturen (Ebenen „Basis“ + „Trübung“), im Repo.

### Zubehör
Item/Block-Registrierung, Kreativ-Reihe `dull_end_ores` (nach `end_ores`), Suchreiter nach den echten Erzen, Tags (Spitzhacke,
Diamant), Blockstates/Modelle/Items-JSON + Loot + Rezepte im 26.3-Overlay, Lang EN/DE, JEI-Info, Wiki-Manual, `docs/MODS-UND-FEATURES.md`.
Erzdetektor und Aderabbau bleiben bei den echten Erzen (der Detektor „sieht“ trübes Erz nicht – bewusst, als Lohn fürs Werkzeug).

### Gametests (OreGenAndItemFrameTests + Fabric-Adapter + Katalog)
1. `endOreFeaturesCarryTheRightOreBlockAndVeinSize`: neue Größen, zwei Ziele (trüb zuerst, p wie oben, nur Endstein).
2. `dullOresMakeAFifthOfTheVeinAndKeepTheRealAmount`: echte Feature-Konfiguration aus der Registry 600× im Platzierungs-Umfeld
   gesetzt (fester Zufall), Vergleich mit der alten Konfiguration (Größe 4/5, nur echt): Anteil trüb 0,20 ± 0,04, echte Menge ± 10 %.
3. `dullOresDropDullMaterialAndNeedADiamondPickaxe`: Werkzeugstufe (Eisen nein, Diamant ja), Drops ohne/mit Behutsamkeit, Rezept 4→1.

## Verifikation
`/root/sbt a-sbtex23b`: Datagen `:mc26_3:fabric:runDatagen` (eigener Aufruf, Diff prüfen), Compile `:mc26_3:fabric:compileJava`,
`run.py --targets fabric-263` mit Filtern auf Erze/Weltgen/data_integrity, Generatoren (`--check`), `layers.py check`,
`wiki/data/textures.js`, module_guides/wiki/discover, `./gradlew check -q`.

## Plan-Abgleich
(wird nach der Umsetzung gefüllt)
