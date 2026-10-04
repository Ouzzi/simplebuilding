# Plan „Crucible / Schmelztiegel“ + Seelen-Lava + Kupfer-/Enderit-Eimer + warmes Essen (Queue Nachtrag 9, 2026-10-04)

Status: **nur Konzept + Fragebogen**, kein Code. Quelle: Besitzer-Sprachdiktat 2026-10-04, eingetragen in
`.claude/QUEUE.md` → „Besitzer 2026-10-04 (Nachtrag 9)“. Stellen, an denen der Besitzer entscheiden soll, sind mit
**[Fx]** markiert (Fragebogen §15). Bis zur Antwort gilt jeweils die ★-Empfehlung als Arbeitsannahme.

Kern des Wunschs (Diktat, sinngemäß):
1. **Crucible** = weitere Ofen-Station, so schnell wie ein normaler Ofen, aber **mehrere verschiedene Dinge parallel**;
   Ergebnisse landen im nächsten freien Slot. Stufen: Basis 9 Slots (wie Crafting-Grid), Reinforced 18, „Enderite 27“,
   Enderite mit doppelter Stackgröße; Tempo analog zu den SB-Ofen-Stufen.
2. **Slot-Indikator** im Slot-Hintergrund: gart („crucing“) = heller + Fortschritt; kein Platz = rot (stoppt);
   zu wenig Hitze = blau.
3. **Kein Brennstoff, sondern Hitzequelle**, gestuft (Fackel/Kerze/Lagerfeuer/Seelenfeuer/Magma/Lava/Seelen-Lava).
4. **Seelen-Lava**: neue Flüssigkeit; weder Quell- noch Fließblock ersetz-/überbaubar; entfernen nur, indem man die
   Quelle mit einem Eimer aufnimmt.
5. **Eimer**: Kupfer-Eimer nimmt keine Seelen-Lava, nur normale Lava, und **zerbricht beim Ausgießen von Lava**.
   Eisen-Eimer (Vanilla) zerbricht beim Ausgießen von Seelen-Lava. Enderit-Eimer (Schmiedetisch, direkt vom
   Eisen-Eimer) zerbricht nicht.
6. **Aufwärmen**: Sandwiches (Modul Simple Sandwiches) und andere warme Speisen; aufgewärmt **15 % schneller essbar**,
   bleiben **ca. einen Tag** warm. Diktat brach ab bei „in einem bundle ca…“ → durch Ergänzung beantwortet (unten).

**Besitzer-Ergänzungen 2026-10-04 (gehen den Abschnitten unten vor, sind dort eingearbeitet):**
7. **Warm-Dauer**: normal ca. **ein halber Tag-Nacht-Zyklus** (12 000 Ticks), im **Bündel ca. 2 Tag-Nacht-Zyklen**
   (48 000 Ticks). Werden Stapel kombiniert, gilt der **Mittelwert der Wärme aller Items**. Ein **Glow um die Items**
   zeigt die Wärme an (Stärke ~ Restwärme).
8. **Herstellung Eisen-Tiegel in der Welt**: Vorschlaghammer auf einen **Eisenblock**, **Eisenbarren in der
   Nebenhand**; die ersten **4 Schläge = 4 Eisen-Druckplatten** (die 4 Wände), dann **2 Schläge = 2 Eisenstäbe**
   (die 2 Griffe). Höhere Stufen analog zu den Ofen-Aufwertungen.
9. **Auch im Modul Simple Sandwiches** sollen Tiegel samt Warm-Food existieren (Modul eigenständig spielbar); ohne SB
   (kein Vorschlaghammer) wird mit der **Axt** geschlagen. Aufteilung der Teile → [F1].
10. **Hitzestufen**: Lagerfeuer und Magma = **mittel**, Lava = **hoch**, Seelen-Lava = **extrem**; niedrige Stufe
    (Fackel/Kerze/Seelenfeuer …) vorschlagen.
11. **Seelen-Lava-Weltgenerierung**: ca. **0,5 %** Chance statt einer Lava-Tasche im Nether; sonst nur in
    **Netherfestungen**: **10 %** Chance, dass die Lava im Raum mit der Lavaquelle ersetzt wird. Nirgends sonst.

Modul-Unabhängigkeit: maßgeblich ist `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` (schreibt ein anderer Helfer);
dieser Plan verweist nur darauf und richtet die Aufteilung [F1] danach aus.

## 1. Ist-Zustand (Recherche im Worktree `cl-crucible` @ 2a01fea6, Sandwiches-Stand `cl-sandwiches` @ e55fe1af)

**SB-Ofen-Stufen** (`common/src/shared/java/com/simplebuilding/blocks/entity/custom/`)
- Drei Familien (Ofen, Räucherofen, Schmelzofen) × drei Stufen **Verstärkt / Netherit / Enderit**
  (`ModBlocks` Z. 97–111). Härte/Explosionsfestigkeit: Verstärkt 3,5; Netherit 5,0/1200; Enderit 6,0/1500,
  Sound METAL bzw. NETHERITE_BLOCK.
- Tempo = Vanilla-`serverTick` + Zusatzticks je Tick: **2× / 4× / 8×** (`FurnaceTierPerks.extraCookTicks` 1/3/7),
  konfigurierbar über `server.machines.{reinforced,netherite,enderite}FurnaceSpeed` (Standard 2/4/8, geklemmt auf
  1..`ServerTuning.MAX_MACHINE_SPEED`), gelesen über `ServerTuning.furnaceExtraTicks(tier)`. Brennstoff wird nur im
  Vanilla-Takt verbraucht („ohne Brennstoffkosten“ für den Boost).
- `FurnaceTierPerks`: Netherit/Enderit **doppelte Erfahrung**; Netherit-/Enderit-Schmelzofen **+25 % / +50 %
  Ausbeute** für Rohmetalle (`simplebuilding:blast_furnace_bonus`), ausgenommen `furnace_bonus_excluded`
  (Kreislauf rissiger Diamant).
- Herstellung Verstärkt: `DDD/FFF/DDD` (D Rissiger Diamant, F Ofen) → 3 Stück (`ModRecipeProvider` Z. ~697).
  Aufwertung Verstärkt→Netherit→Enderit **in der Welt** per Vorschlaghammer + Nugget in der Nebenhand, 5 s halten
  (`util/SledgehammerUpgrades`); Kupfertruhe→Verstärkt mit Rissigem Diamant; Inhalt bleibt (`TieredChests.upgradeInPlace`).
- Enderit-Schrott: **nur** Schmelzofen-Rezept, 144 000 Ticks (2 h Vanilla, 15 min Enderit), Zeiten > 32 767 nur dank
  `AbstractFurnaceBlockEntityMixin`/`AbstractFurnaceMenuMixin` (`ModRecipeProvider` Z. ~878). Rissiger Diamant →
  Diamant ebenfalls nur Schmelzofen.

**Vorschlaghammer in der Welt** (Vorbild für die Tiegel-Herstellung, `common/src/shared/java/com/simplebuilding/`)
- `items/custom/SledgehammerItem`: **Diamantblock zerschlagen** – jeder Schlag zählt, der 8. (`DIAMOND_BLOCK_STRIKES`)
  zerschlägt den Block zu 81 Kieseln; Fortschritt je Block in `util/SledgehammerProgress` (gespeichert, für alle als
  Risse sichtbar, nie Text).
- `util/SledgehammerUpgrades`: Hammer in der Haupthand + Material in der **Nebenhand**, Rechtsklick halten, 1 Schlag/s
  (`HIT_INTERVAL` 20), Abbruch bei losgelassenem Klick/Blick weg, Fortsetzen möglich, Hand-Neigung als Passt-Hinweis
  (`showsUpgradeHint`), „klonk“ bei falschem Material, Haltbarkeit je Schlag 2/4/10.
- `util/InWorldTransformations` exportiert alle Umwandlungen für Wiki + JEI („Umwandlung in der Welt“) → der Tiegel-
  Bau gehört dort hinein.
- `iron_rod` (Eisenstab, aufstellbarer Block) existiert nur auf 26.3 (`McVersion.GADGET_REWORK`); Vanilla-„Eisen-
  Druckplatte“ = `heavy_weighted_pressure_plate` (Wägeplatte schwer).
- → Der Eisen-Tiegel ist eine neue Rezeptart dieser vorhandenen Mechanik (Ziel-Block Eisenblock, Nebenhand Eisenbarren,
  6 Schläge, Zwischenstände als Blockzustände statt Risse). Im Sandwiches-Modul gibt es keinen Hammer → die Axt
  übernimmt die Rolle, Mechanik dort schlank nachgebaut (kein Import).

**Übergroße Stapel** (Vorbild für „Enderit ×2“)
- `ChestTier`: Verstärkt 36, Netherit 45 (**×2**), Enderit 54 (**×4**) – Achtung: in SB ist Enderit sonst ×4.
- `BackpackItem.maxStackSizeIn(stack, mult)` (nicht stapelbare bleiben 1), `splitIntoNormalStacks` beim Droppen,
  `TieredChestBlockEntity.CODEC_MAX_COUNT = 99` + `codecSafeCopy` (Vanilla-Codec kann > 99 nicht speichern),
  `TieredChests.maxStackSize/oversizedStorage/analogSignal` für Trichter und Komparator, `ModHopperBlockEntity`.
- → Alles für einen 27er-Tiegel mit ×2 existiert als Muster; nichts neu erfinden.

**Hitze / Flüssigkeiten / Eimer**
- Kein Hitze-System, keine eigene Flüssigkeit, kein Kupfer-/Enderit-Eimer im Repo (`git grep` auf
  `soul_lava|FlowingFluid|copper_bucket` leer). Einziger Fluid-Bezug: `modules/simpletweaks/.../ClaimFluidMixin`
  (Flüssigkeiten fließen nicht in fremde Claims) und `ClaimBucketMixin`/`ClaimDispenserMixin` → Seelen-Lava muss dort
  automatisch mitlaufen.
- Bauwerkzeuge prüfen `BlockState#canBeReplaced()` (Bauzauberstab, `blueprint/ShapeFill`, `BlueprintBuilder`,
  `HammockLayout`, `EndPistonMoves`) → ein Fluid-Block **ohne** `replaceable()` wird von ihnen automatisch respektiert.
- SB hat `ENDERITE_UPGRADE_TEMPLATE` + `ENDERITE_INGOT`; Schmiede-Rezepte per `createSmithingTransform`
  (z. B. Netherit-Rucksack → Enderit-Rucksack). Ein Eisen-Eimer→Enderit-Eimer ist damit ein Einzeiler im Datagen.

**Vanilla 26.3 (Kenntnisstand, beim Bau gegen `%TEMP%\mcsrc263` prüfen)**
- `AbstractFurnaceBlockEntity`: Standard-Garzeit 200 Ticks (Ofen), Schmelz-/Räucherofen 100, Lagerfeuer 600;
  Erfahrung wird je Rezept gesammelt (`recipesUsed`) und beim Entnehmen/Abbauen als Kugeln ausgezahlt.
- Lava: Licht 15, Fließweite Overworld 3 Blöcke (Abfall 2, Takt 30 Ticks), Nether 7 (Abfall 1, Takt 10);
  Quelle + Wasser → Obsidian, fließend → Bruchstein, über Seelenerde neben Blaueis → Basalt (`LiquidBlock`).
- `LiquidBlock` ist nur ersetzbar, weil seine Properties `replaceable()` tragen; Kolben zerstören Flüssigkeiten
  (`PushReaction.DESTROY`), andere Flüssigkeiten fließen hinein, wenn `FlowingFluid#canBeReplacedWith` es erlaubt.
- NeoForge/Forge verlangen für jede Flüssigkeit einen **`FluidType`** (Rendering, Schwimmen, Temperatur), Fabric
  nutzt `FluidRenderHandlerRegistry` + Fluid-Tags → Loader-spezifische Schicht nötig.
- `Consumable.consumeSeconds` (Standard 1,6 s) → „15 % schneller“ = 1,36 s.
- Eine Minecraft-Tag-Nacht-Runde = 24 000 Ticks (20 min); die Tagphase allein = 12 000 Ticks (10 min).

**Simple Sandwiches** (nur gelesen, `cl-sandwiches`)
- Eigenes Modul `simplesandwiches` (Paket `com.simplesandwiches`), 26.3-only, darf **keine** SB-Klassen importieren und
  SB keine Modulklassen; Kopplung nur über Registry-IDs/Tags (`required:false`).
- Sandwich = Item mit Komponente `sandwich_contents`, schreibt deterministisch `FOOD`/`CONSUMABLE`; Stapel 16.
- Entscheidung F10: **Essen aus dem Vanilla-Bündel** (oberstes essbares Item, Rechtsklick halten) – deshalb zielt
  das abgebrochene „in einem bundle ca…“ sehr wahrscheinlich auf „im Bündel bleibt es ca. X lang warm“.

## 2. Modul- und Versionsrahmen

- Zuordnung **[F1]** (nach Besitzer-Ergänzung 9: Tiegel + Warm-Food auch im Sandwiches-Modul, eigenständig
  spielbar). Randbedingungen: Module dürfen keine SB-Klassen importieren und umgekehrt (Kopplung nur über
  Registry-IDs, Tags, `required:false`, `framework/`-Verträge ohne Minecraft-Klassen) – Details und endgültige
  Regeln in `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`. `framework/` kann keine Block-Entity tragen (kennt keine
  Minecraft-Klassen), also muss der Tiegel-Code in **beiden** Mods liegen, wenn beide allein spielbar sein sollen.
  ★ Empfehlung **„gleicher Baustein, zwei Namensräume, SB übernimmt“**:
  - **Sandwiches-Modul** (allein spielbar): `simplesandwiches:iron_crucible` (9 Slots, 1×), Hitze niedrig/mittel/hoch
    (Tags), Warm-Komponente `simplesandwiches:warm` + Esszeit-Bonus + Bündel-Isolation + Glow, In-World-Herstellung
    mit der **Axt**.
  - **SimpleBuilding** (allein spielbar): eigene Tiegel-Familie `simplebuilding:iron_crucible` / `reinforced_` /
    `netherite_` / `enderite_crucible`, Vorschlaghammer-Herstellung und -Aufwertung, Seelen-Lava (Stufe extrem),
    Kupfer-/Enderit-Eimer, eigene Warm-Komponente `simplebuilding:warm`.
  - **Beide geladen**: Der Modul-Tiegel bleibt registriert (Welten brechen nicht), seine Herstellung ist aber
    abgeschaltet (Bedingung „SB nicht geladen“); die SB-Aufwertung akzeptiert `simplesandwiches:iron_crucible` per
    Registry-ID als Ausgangsstufe und baut ihn mit Inhalt zum SB-Verstärkt-Tiegel um. Warm: beide Ess-Mixins lesen
    beide Komponenten-IDs (Lookup per ID, kein Import); geschrieben wird nur noch `simplebuilding:warm`.
  - Gemeinsame Logik (Slot-Zustände, Hitze-Tabelle, Warm-Mittelwert) als **eine Vorlage** gepflegt und in beide
    Pakete übernommen; ein Abgleich-Check (`tools/` + GameTest-Paar) verhindert Abweichungen.
- Warm-Tag je Namensraum (`<ns>:warmable_food`); Sandwiches landen per `required:false`-Eintrag auch im SB-Tag.
- 26.3 zuerst (Fabric → NeoForge → Forge `-Pforge263=true`). Da SB-Kern auf 26.2 kompilieren muss: Feature hinter
  `McVersion.CRUCIBLE` (26.3 true, 26.2 false), 26.3-Code in `mc26_3/overlay/java`, Zwilling/Stubs in
  `common/src/mc26_2/java` **[F92]**.
- Server-Config `server.crucible` (+ `server.soulLava`, `server.warmFood`) mit harten Grenzen (§11).
- Keine Bildschirmtexte; Rückmeldung über Slot-Indikator, Sound, Partikel, Tooltip, Jade.

## 3. Blöcke, Items, Flüssigkeit (Übersicht, ★-Annahmen)

| ID | Art | Kurz |
|---|---|---|
| `iron_crucible` | Block + BE + Menü | Eisen-Tiegel (Basis), 9 Slots (3×3), 1× **[F4]**; in SB und im Sandwiches-Modul |
| `reinforced_crucible` | dto. | 18 Slots, 2× |
| `netherite_crucible` | dto. | 27 Slots, 4× (Diktat „Enderite 27“ vermutlich Netherit, [F4]) |
| `enderite_crucible` | dto. | 27 Slots, 8×, Stapel ×2 |
| `soul_lava` / `flowing_soul_lava` | Fluid + `LiquidBlock` | nicht ersetzbar, Hitze extrem |
| `soul_lava_bucket` | Item | nur aus Eisen- (bricht beim Ausgießen) oder Enderit-Eimer |
| `soul_lava_cauldron` | Block | Beschaffung/Hitzequelle [F59/F60] |
| `copper_bucket` (+ `copper_water_bucket`, `copper_lava_bucket`, `copper_milk_bucket`, …) | Items | [F68] |
| `enderite_bucket` (+ Inhalts-Varianten inkl. `enderite_soul_lava_bucket`) | Items | unzerbrechlich |
| Komponente `simplebuilding:warm` | Data Component | `warmUntil` (Weltzeit), ggf. `paused`-Restzeit fürs Bündel |

Gemeinsame Block-Entity `CrucibleBlockEntity` mit `CrucibleTier` (Slots, Spalten, Tempo-Stufe, Stapelfaktor) wie
`ChestTier`; Block-Varianten unterscheiden sich nur im Tier.

### 3a. Herstellung in der Welt (Besitzer-Ergänzung 8)

- ★ Ablauf **[F101–F104]**: Vorschlaghammer (SB) bzw. Axt (Sandwiches-Modul ohne SB) in der Haupthand, **Eisenbarren
  in der Nebenhand**, Rechtsklick auf einen **Eisenblock** halten, 1 Schlag/s. Jeder Schlag verbraucht einen
  Eisenbarren und formt sichtbar weiter:
  - Schläge 1–4: je eine Wand (Modell zeigt die Wände als Eisen-Druckplatten-Platten, Blockzustand `stage` 1–4),
  - Schläge 5–6: je ein Griff (Eisenstab-Optik, `stage` 5–6) → nach dem 6. Schlag steht ein fertiger Eisen-Tiegel.
  - Kosten ★ 1 Eisenblock + 6 Eisenbarren (= 15 Barren, etwa Kessel 7 + Ofen-Äquivalent).
- Zwischenstände sind eigene Block-Zustände eines Rohlings `crucible_blank` (abbaubar → gibt Eisenblock + bisher
  verbrauchte Barren zurück) **[F105]**; Fortsetzen jederzeit, wie bei `SledgehammerProgress`.
- Haltbarkeit je Schlag: Hammer wie Kupfertruhen-Aufwertung (2), Axt 1 je Schlag [F104].
- Wiki/JEI über `InWorldTransformations` („Umwandlung in der Welt“), im Modul über dessen eigene JEI-Kategorie.
- Höhere Stufen ★ analog zu den Ofen-Aufwertungen (§ Ist-Zustand): Eisen → Verstärkt mit **Rissigem Diamant**
  (jeder Vorschlaghammer, wie Kupfertruhe → Verstärkte Truhe), Verstärkt → Netherit mit **Netherit-Nugget** (ab
  Diamant-Hammer), Netherit → Enderit mit **Enderit-Nugget** (ab Netherit-Hammer); Inhalt und Fortschritt bleiben.
  Nur in SB (Materialien und Hammer gibt es nur dort) [F9–F11].

## 4. Verarbeitung im Tiegel

- **Je Slot ein eigener Garvorgang**, alle Slots parallel; aus einem Stapel wird ein Item nach dem anderen gegart
  (wie ein Ofen je Slot) **[F11]**.
- Ergebnis: zuerst auf einen passenden, als „Ergebnis“ markierten Stapel, sonst in den **nächsten freien Slot**
  (Lesereihenfolge ab dem eigenen Slot, umlaufend); wird der eigene Slot beim letzten Item leer, landet das Ergebnis
  dort **[F12]**. Kein Platz → Slot rot, Fortschritt bleibt stehen.
- Ergebnis-Slots sind **gesperrt** (werden nicht erneut gegart, z. B. Bruchstein→Stein→Glatter Stein), bis ein
  Spieler/Trichter sie entnimmt **[F13]**. Neu hineingelegte Items sind immer Eingabe.
- Rezeptquelle **[F15]**: `smelting` als Grundlage; Rezepte, die es **nur** als `blasting`/`smoking` gibt, ebenfalls
  (Mod-Rezepte wie Rissiger Diamant). `campfire_cooking` nur, wenn kein anderes passt.
- Garzeit **[F16]**: Zeit des `smelting`-Rezepts (= normaler Ofen, 200 Ticks), Schmelz-/Räucher-only-Rezepte × 2
  (Ofen-Äquivalent), dann Stufen-Tempo (Zusatzticks wie `FurnaceTierPerks`).
- Rezept-Cache je Slot (`RecipeManager.CachedCheck` bzw. letzter Treffer je Item) – 27 Lookups pro Tick vermeiden.
- Erfahrung je Rezept gesammelt wie Vanilla, ausgezahlt beim Entnehmen eines Ergebnis-Slots durch einen Spieler oder
  beim Abbau; Netherit/Enderit doppelt **[F6]**. Kein Erz-Ausbeutebonus **[F7]**.
- Ausgeschlossen per Tag `simplebuilding:crucible_excluded` (★ Geschichtetes Rohenderit → Enderit-Schrott, damit
  27 parallele Plätze die 2-h-Bremse nicht aushebeln) **[F17/F18]**.
- Aufwärmen (§9) ist ein eigener Vorgang: kein Rezept, ganzer Stapel auf einmal, bleibt im Slot und wird Ergebnis.

## 5. Hitze

Besitzer-Vorgabe (Ergänzung 10): Lagerfeuer + Magma = **mittel**, Lava = **hoch**, Seelen-Lava = **extrem**.
★-Tabelle mit vorgeschlagener niedriger Stufe **[F19/F20/F22]** (Block-Tags `<ns>:heat_source/low|medium|high|extreme`,
per Datapack erweiterbar [F31]):

| Stufe | Name | Quellen (nur entzündet/aktiv) | Erlaubt ab dieser Stufe |
|---|---|---|---|
| H0 | kalt | – | nichts |
| H1 | **niedrig** (Vorschlag) | Kerze(n), Kerzenkuchen, Fackel, Laterne, Seelenfackel, Seelenlaterne, Feuer, Seelenfeuer | Aufwärmen |
| H2 | **mittel** (Besitzer) | Lagerfeuer, Seelen-Lagerfeuer, Magmablock | Essen garen + allgemeines Schmelzen (Glas, Stein, Ziegel, Holzkohle, Farbstoffe …) |
| H3 | **hoch** (Besitzer) | Lava (Quelle + fließend), Lavakessel | Erze/Metalle (Rezepte, die es auch als `blasting` gibt) |
| H4 | **extrem** (Besitzer) | Seelen-Lava, Seelen-Lava-Kessel (nur SB) | Tag `<ns>:crucible_needs_extreme_heat` (★ Antiker Schrott, Rissiger Diamant → Diamant) |

- Im Sandwiches-Modul ohne SB gibt es keine Seelen-Lava → H4 leer, der Tag enthält Seelen-Lava als `required:false`.
- Zuordnung automatisch aus den vorhandenen Rezepttypen (blasting-Zwilling → hoch, sonst mittel), überschreibbar durch
  Item-Tags `<ns>:crucible_heat/<stufe>` (Mod-Rezepte, Sonderfälle).
- Position **[F21]**: ★ Block **darunter oder einer der vier seitlichen Nachbarn**, höchster Wert zählt [F22]; oben
  zählt nicht. So bleibt unten Platz für einen Entnahme-Trichter, wenn die Hitze von der Seite kommt.
- Hitze wird bei Nachbaränderung (`neighborChanged`) neu berechnet und zusätzlich alle 20 Ticks kontrolliert
  (Feuer erlischt ohne Blockupdate) [F28]. Sinkt sie unter die Mindesthitze: Slot **blau**, Fortschritt pausiert [F26].
- Kein Verbrauch, kein Tempo-Bonus durch Überschuss [F27], kein Nether-Bonus [F30], der Tiegel entzündet nichts [F29].

## 6. GUI und Slot-Indikator

- Layout **[F32]**: Eisen ein 3×3-Gitter mittig; Verstärkt zwei 3×3-Gitter nebeneinander (4 px Lücke); Netherit/
  Enderit drei 3×3-Gitter (= 9 Spalten × 3 Reihen, mit Lücken zwischen den Gittern, passt in die Vanilla-Breite 176).
  Links ein Thermometer (kalt/niedrig/mittel/hoch/extrem) mit dem Symbol der aktuell wirksamen Quelle; kein Brennstoffslot,
  kein Rezeptbuch [F39]. Spielerinventar wie Vanilla.
- Indikator je Slot **[F33/F34]** (Hintergrund unter dem Item, 16×16):
  - **gart**: Hintergrund aufgehellt, orange Füllung von unten nach oben = Fortschritt,
  - **rot**: kein Platz für das Ergebnis, gestoppt,
  - **blau**: Hitze zu niedrig, pausiert,
  - **Ergebnis** (gesperrt): neutral mit dünnem grünem Rand,
  - **grau**: kein Rezept/nicht aufwärmbar (bleibt liegen, blockiert nur den Platz).
- Barrierefreiheit **[F35]**: zusätzlich zum Farbton je Zustand ein Muster/Symbol in der Ecke (Flamme, Kreuz-
  schraffur rot, Schneeflocke blau, Haken grün) + Tooltip-Zeile („Gart – 45 %“, „Kein Platz“, „Braucht hohe Hitze (Lava)“);
  Client-Option für eine farbenblind-freundliche Palette.
- Synchronisation: Fortschritt/Zustand je Slot als `ContainerData` (27 × 2 Werte) – bei > 32 767 Ticks wie bei den
  Öfen auf Prozent bzw. Zustandscode verdichten.
- Block-Zustand `lit` (Licht 13, Partikel/Knistern wie Ofen), sobald ein Slot gart [F99]; Jade/`BlockInfo`: Stufe,
  Hitze, aktive/blockierte Slots [F38]; JEI/REI-Kategorie „Schmelztiegel“ mit Mindesthitze.

## 7. Automatisierung, Stapel ×2, Abbau

- Trichter/Seiten **[F40–F45]**: oben und seitlich **einfügen** (zuerst auf gleiche Eingabestapel, sonst freier Slot,
  nie in Ergebnis-Slots, nur garbare/aufwärmbare Items, nie den letzten freien Slot belegen → Ergebnisse haben immer
  Platz); unten **entnehmen** nur Ergebnis-Slots. Komparator = Füllstand wie Truhe (`TieredChests.analogSignal`).
- Enderit-Stapel ×2 **[F46]**: `BackpackItem.maxStackSizeIn(stack, 2)`, Speichern über `codecSafeCopy`-Muster,
  Trichter über die vorhandene `oversizedStorage`-Logik.
- Abbau **[F47]**: Inhalt droppt in normalen Stapelgrößen (`splitIntoNormalStacks`), gesammelte Erfahrung als Kugeln;
  laufender Fortschritt geht verloren. Aufwertung per Vorschlaghammer behält Inhalt **und** Fortschritt [F48];
  27er→27er kopiert 1:1, 9→18→27 hängt neue Slots hinten an.
- Härte/Explosionsfestigkeit/Sound je Stufe wie die gleichnamigen Öfen.

## 8. Seelen-Lava

- `FlowingFluid`-Paar + `LiquidBlock` **ohne** `replaceable()` → kein Spieler (★ auch Kreativ), kein Bauzauberstab,
  keine Blaupause, kein Enderman setzt etwas hinein **[F52]**. Andere Flüssigkeiten fließen nicht hinein
  (`canBeReplacedWith` → false). Kolben: `PushReaction.BLOCK` (nicht zerstörbar, nicht schiebbar). Explosionen,
  Wither, Drache entfernen sie nicht. `/setblock`/`/fill` bleiben (Admin-Werkzeug).
- Entfernen: nur Eimer an der **Quelle** (Eisen- oder Enderit-Eimer); Fließblöcke verschwinden danach wie bei Lava.
- Fließweite ★ Overworld 2, Nether 4, Takt 40/20 Ticks (zäher als Lava) **[F50]**; überall platzierbar [F51];
  keine unendliche Quelle [F65].
- Kontakt **[F53/F54]**: ★ keine Umwandlung der Seelen-Lava (passt zu „nur per Eimer entfernbar“); Wasser, das sie
  berührt, verdampft zischend (Rauch), Lava fließt nicht hinein.
- Schaden **[F55]**: 6 je Treffer (1,5× Lava) + Seelenbrand 15 s mit blauen Flammen; Feuerresistenz halbiert nur.
  Items verbrennen wie in Lava, Netherit/Enderit feuerfest [F56]. Entzündet Umgebung wie Lava, auf Seelensand/-erde
  Seelenfeuer [F57]. Licht 15 [F58]. Nebel unter der Oberfläche türkis [F66]. Strider laufen darauf, Boote nicht [F63].
- Beschaffung (Besitzer-Ergänzung 11, **nur** Weltgenerierung im Nether) **[F59, F106–F109]**:
  - Nether-**Lava-Taschen** (die verborgenen Lavaquellen im Netherrack, Vanilla-Features `spring_closed` /
    `spring_closed_double`): je platzierter Tasche ca. **0,5 %** Chance, dass Seelen-Lava statt Lava gesetzt wird
    (eigener Placed-Feature-Zwilling mit Seltenheitsfilter bzw. Mixin auf das Spring-Feature; Biome-Modifier auf
    NeoForge/Forge, `BiomeModifications` auf Fabric).
  - **Netherfestungen**: im Raum mit der Lavaquelle (Vanilla-Stück „Castle Entrance“ mit dem Lava-Brunnen,
    `NetherFortressPieces$CastleEntrance`) **10 %** Chance je Raum, dass dessen Lava durch Seelen-Lava ersetzt wird.
  - **Nirgends sonst**: keine Seen, kein Overworld-/End-Worldgen, keine Kessel-Umwandlung als Quelle (★ – die
    frühere Idee „Lavakessel über Seelenfeuer“ entfällt, damit Seelen-Lava selten bleibt).
  - Weil Seelen-Lava nicht ersetzbar ist, wird eine angeschlagene Tasche beim Graben zur Falle; Fließweite klein halten.
- Kessel **[F60]**: ★ Seelen-Lava-Kessel nur als Lager/Hitzequelle (füllen/leeren nur mit Eisen- – bricht beim
  Füllen – oder Enderit-Eimer), **keine** Vermehrung.
- Dispenser **[F61]**: wie Spieler (Eisen-Eimer gibt aus und bricht; leerer Eisen-/Enderit-Eimer nimmt auf).
  Schwamm saugt nicht [F62]. Claims/Spawnschutz respektiert [F64].
- Loader: NeoForge/Forge `FluidType` (Temperatur 1500, Dichte/Viskosität höher als Lava, `canExtinguish` false,
  `canConvertToSource` false), Fabric `FluidRenderHandlerRegistry` + Tag `simplebuilding:soul_lava`. **Nicht** in
  `minecraft:lava` taggen (sonst zieht Vanilla Obsidian-/Basalt-Umwandlung und Lava-Verhalten automatisch an);
  Schaden/Feuer/Schwimmen selbst über Entity-Mixin bzw. FluidType.

## 9. Eimer

- **Kupfer-Eimer** **[F67–F72]**: Rezept wie Eimer aus 3 Kupferbarren (V-Form), Stapel 16. Kann Wasser, Lava,
  Milch, Pulverschnee, Fische/Axolotl wie der Eisen-Eimer; **Seelen-Lava nicht** (Rechtsklick tut nichts, „dumpfes“
  Geräusch). **Zerbricht beim Ausgießen von Lava** (Welt, Kessel, Dispenser): Lava wird platziert, Eimer weg,
  Brechgeräusch, kein Rest. Keine Oxidation.
- **Eisen-Eimer** (Vanilla) **[F73]**: nimmt Seelen-Lava auf, **zerbricht beim Ausgießen** (Lava wird platziert).
- **Enderit-Eimer** **[F74/F75]**: Schmiedetisch Enderit-Vorlage + Eisen-Eimer + Enderit-Barren; kann alles inkl.
  Seelen-Lava, zerbricht nie, als Item feuerfest. Keine Netherit-Zwischenstufe.
- Exploits **[F76/F77]**: Kupfer-Lava-Eimer als Ofen-Brennstoff → Eimer wird verbraucht (kein leerer Eimer zurück).
  Seelen-Lava-Eimer ist kein Brennstoff. Rezepte mit Eimer-Rest (Kuchen) geben den jeweiligen Eimertyp zurück.

## 10. Warmes Essen

- Aufwärmbar ★ Tag `simplebuilding:warmable_food`: gegartes Fleisch/Fisch, Ofenkartoffel, Brot, Suppen/Eintöpfe,
  Kürbiskuchen, Sandwiches (optional) **[F78]**. Ab „niedrig“ [F79], Aufwärmzeit 100 Ticks (5 s) × Stufentempo, ganzer
  Stapel [F80/F90]. Im Tiegel **gegarte** Speisen kommen ebenfalls warm heraus [F88: nur Tiegel].
- Vorteil ★ Esszeit −15 % (`consumeSeconds × 0,85`) [F81].
- Dauer (Besitzer-Ergänzung 7): normal **12 000 Ticks** (halber Tag-Nacht-Zyklus, 10 min), im **Bündel 48 000 Ticks**
  (zwei Zyklen, 40 min) → im Bündel kühlt es viermal langsamer ab. Erneutes Aufwärmen setzt wieder auf voll.
- Datenmodell ★ **[F83]**: Komponente `warm = (warmUntil, bundledRemaining?)`. Außerhalb von Bündeln zählt der Weltzeit-
  Stempel `warmUntil` (kühlt auch in entladenen Chunks, Truhen, Offline-Inventaren gleichmäßig ab; nichts tickt pro
  Item). Beim Einlegen ins Bündel wird die Restzeit × 4 als `bundledRemaining` + Einlegezeit gespeichert, beim
  Herausnehmen zurückgerechnet (Restzeit ÷ 4 nach Abzug der Bündelzeit).
- **Stapeln = Mittelwert** (Besitzer): Die Warm-Komponente zählt für die Stapelgleichheit **nicht** mit; beim Zusammen-
  legen wird die neue Restwärme als mengengewichteter Mittelwert aller Items gerechnet, **kalte Items zählen mit 0**
  [F89, F110]: z. B. 10 Stück mit 8 min + 10 kalte → 20 Stück mit 4 min. Umsetzung: Mixin auf
  `ItemStack#isSameItemSameComponents` (Warm-Komponente ausblenden) + Hook an den Stellen, die Stapel vereinen
  (Slot/Container/Trichter/Item-Entity-Merge) – größter technischer Eingriff dieses Plans.
- Abgelaufene Komponente wird beim Inventar-Tick/Öffnen eines Containers entfernt [F85].
- **Glow** (Besitzer): Leuchtsaum um das Item, Stärke ~ Restwärme, in Inventar, Hand und als gedroppter Gegenstand
  [F90, F111, F112]. ★ Umsetzung: eigene Client-Item-Eigenschaft `<ns>:warmth` (0..1) für eigene Items per
  `minecraft:range_dispatch` (4 Stufen eines warmen Orange-Saums als Overlay-Schicht); für Vanilla-/Fremd-Items ein
  Render-Mixin, der den Saum als zusätzliche Schicht zeichnet. Dazu Tooltip „Warm (noch ~7 min)“.
- Bündel-Essen (Sandwiches F10) bekommt den Bonus des obersten Items.

## 11. Server-Config (harte Grenzen)

| Schlüssel | Standard | Grenzen |
|---|---|---|
| `crucible.reinforcedSpeed` / `netheriteSpeed` / `enderiteSpeed` | 2 / 4 / 8 | 1..`MAX_MACHINE_SPEED` |
| `crucible.maxActiveSlots` | 27 | 1..27 (Server-Bremse für Parallelität) |
| `crucible.blastOnlyTimeFactor` | 2,0 | 1..4 |
| `crucible.warmTicks` | 100 | 20..1200 |
| `crucible.heatRecheckTicks` | 20 | 5..200 |
| `crucible.doubleXp` | an | an/aus |
| `soulLava.flowDistanceOverworld` / `Nether` | 2 / 4 | 1..4 / 1..7 (nur Neustart) |
| `soulLava.damage` | 6 | 1..20 |
| `warmFood.durationTicks` | 12 000 | 1200..48 000 |
| `warmFood.bundleDurationTicks` | 48 000 | 1200..96 000 |
| `warmFood.eatSpeedBonus` | 0,15 | 0..0,30 |
| `soulLava.springChance` | 0,005 | 0..0,05 |
| `soulLava.fortressChance` | 0,10 | 0..0,5 |
| `copperBucket.breaksOnLava` | an | an/aus |

## 12. Tests (GameTests, `GameTestSpec.named(...)` sortiert + Fabric-Adapter)

- `crucible_tier_slots_and_speed` (9/18/27, 1/2/4/8×), `crucible_parallel_different_recipes`,
  `crucible_result_to_next_free_slot`, `crucible_result_merges_existing`, `crucible_no_space_stops_red`,
  `crucible_result_slot_not_recooked` (Bruchstein→Stein bleibt Stein), `crucible_xp_double_upper_tiers`,
  `crucible_excluded_enderite_scrap`, `crucible_blast_only_recipe_time`.
- `crucible_heat_levels_from_sources` (je Quelle), `crucible_heat_below_or_side_not_above`,
  `crucible_heat_drop_pauses_progress`, `crucible_unlit_campfire_no_heat`.
- `crucible_hopper_top_side_insert_bottom_results`, `crucible_hopper_keeps_last_slot_free`,
  `crucible_enderite_double_stack_save_load`, `crucible_break_drops_split_stacks_and_xp`,
  `crucible_sledgehammer_upgrade_keeps_contents`.
- `soul_lava_not_replaceable_by_block_or_wand`, `soul_lava_piston_blocked`, `soul_lava_bucket_source_only`,
  `soul_lava_flow_distance`, `soul_lava_water_no_conversion`, `soul_lava_damage_and_fire`,
  `soul_lava_worldgen_spring_and_fortress_chance`, `soul_lava_dispenser_iron_breaks`.
- `copper_bucket_rejects_soul_lava`, `copper_bucket_breaks_on_lava_place`, `copper_bucket_water_reusable`,
  `iron_bucket_breaks_on_soul_lava_place`, `enderite_bucket_smithing_and_never_breaks`,
  `copper_lava_bucket_fuel_no_remainder`.
- `warm_food_eat_speed_bonus`, `warm_food_expires_by_game_time`, `warm_food_stack_merge_average_cold_zero`, `warm_food_glow_level`,
  `warm_food_bundle_two_cycles`, `iron_crucible_inworld_six_strikes`, `iron_crucible_axe_without_sb`, `warm_sandwich_optional_tag_without_module`.
- Gates: `fabric-263`, `neoforge-263`, 26.2-/Forge-26.3-Compile, Wiki `--all --check`, `checkBalance`,
  Lang-Paritäten EN/DE in beiden Ressourcenbäumen, Testzentrale-Station „Schmelztiegel“.

## 13. Phasen und Aufwand (Helfer-Sitzungen)

| Phase | Inhalt | Aufwand |
|---|---|---|
| P0 | Besitzer-Antworten einarbeiten, `McVersion.CRUCIBLE`, Config-Gerüst | 0,25 |
| P1 | Eisen-Tiegel: In-World-Herstellung (Rohling, 6 Schläge), BE, Menü, Screen mit Indikatoren, Rezeptlogik, Hitze-Tags, Tests (Fabric) | 2 |
| P2 | Stufen, Vorschlaghammer-Aufwertung, ×2-Stapel, Trichter/Komparator, Jade/JEI | 1 |
| P3 | Seelen-Lava: Fluid je Loader (`FluidType`), Unersetzbarkeit, Schaden, Worldgen (Taschen 0,5 %, Festung 10 %), Dispenser | 1,5 |
| P4 | Kupfer-/Enderit-Eimer inkl. Varianten, Bruchverhalten, Rezepte/Datagen | 1 |
| P5 | Warm-Komponente, Esszeit-Mixin, Bündel (48 000 Ticks), Stapel-Mittelwert-Mixin, Glow, Sandwich-Kopplung per Tag | 1,5 |
| P5b | Modul-Fassung im Sandwiches-Modul: Eisen-Tiegel + Warm-Food + Axt-Herstellung, Abgleich-Check, Übernahme-Regeln bei geladenem SB | 1,5 |
| P6 | Texturen (Generator + Vorschau A/B/C), Besitzerwahl, Einbau | 1 |
| P7 | NeoForge/Forge-Abgleich, 26.2-Stubs, Wiki/Handbuch/Erfolge, Testzentrale, alle Gates | 1 |

Summe ≈ 10,5–11 Sitzungen (inkl. Modul-Fassung). GPT-geeignet: P0, Lang/Wiki-Noten, Tag-Listen, Eimer-Item-Varianten/Datagen, Testzentrale.

## 14. Risiken

- **Balance/Parallelität**: 27 Plätze × 8× = bis zu 216 Vanilla-Öfen in einem Block. Gegenmittel: Mindesthitze
  (Erze brauchen Lava), `crucible_excluded`, `maxActiveSlots`, [F18].
- **Flüssigkeit über drei Loader**: NeoForge/Forge `FluidType`-Pflicht, Fabric Render-Registrierung; Eimer-Logik
  (`BucketItem`/`BucketPickup`/Dispenser-Verhalten/Kessel) ist an vielen Stellen hartcodiert → mehrere Mixins.
- **Unersetzbarer Fließblock** = Grief-Werkzeug (fließt in Basen, lässt sich nicht abdämmen). Gegenmittel: kurze
  Fließweite, Claims/Spawnschutz, Quelle ist per Eimer immer entfernbar.
- Andere Mods/Mechaniken, die `setBlock` ohne `canBeReplaced`-Prüfung nutzen (Blaupausen-Direktsetzen, fremde Mods)
  überschreiben Seelen-Lava trotzdem – nur dokumentieren.
- Codec-Grenze 99 bei ×2-Stapeln (Muster vorhanden), `ContainerData` als `short` (≤ 32 767) bei 27 Slots.
- Mixin auf Esszeit ist versionsempfindlich (26.2-Zwilling).
- **Stapel-Mittelwert**: Warm-Komponente aus der Stapelgleichheit herauszunehmen berührt Vanilla an vielen Stellen
  (Inventar-Klicks, Trichter, Item-Entity-Merge, Bündel, Rezeptbuch-Füllen); jede übersehene Stelle stapelt dann ohne
  Mittelwert. Gegenmittel: zentrale Merge-Hilfe + GameTests je Pfad; Fallback [F110 C].
- Modulgrenze: SB darf Sandwiches nicht importieren und umgekehrt → nur Registry-ID-/Tag-Kopplung; Bündel-Logik
  (Warm-Bündel in SB und Modul, Bündel-Essen im Modul) muss als Mixin-Paar verträglich sein.
- **Doppelter Code (SB + Modul)**: zwei Fassungen des Tiegels können auseinanderlaufen; Abgleich-Check Pflicht.
  Übernahme bei geladenem SB (Registry-ID-Lookup, abgeschaltete Modul-Herstellung) muss zu
  `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` passen – vor P5b gegenlesen.
- **Worldgen**: Spring-Feature-/Festungs-Mixin je Loader; nicht ersetzbare Seelen-Lava in verborgenen Taschen kann
  Spieler beim Graben einsperren – kurze Fließweite, gut sichtbare Optik.
- Texturen: Besitzer-Feedback „eigene Formen statt Recolors“ – Tiegel-Stufen nicht nur umfärben.

## 15. Fragebogen an den Besitzer (Empfehlung jeweils ★)

### A. Rahmen
1. **Welche Teile liegen wo? (Weiche; Tiegel + Warm-Food sollen auch im Sandwiches-Modul allein laufen, Regeln laut
   `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`)** ★A gleicher Baustein in beiden Mods: Modul = Eisen-Tiegel + Hitze
   bis „hoch“ + Warm-Food (Axt); SB = komplette Stufenfamilie + Seelen-Lava + Eimer (Hammer); beide geladen → Modul-
   Herstellung aus, SB-Aufwertung übernimmt den Modul-Tiegel per Registry-ID, nur noch `simplebuilding:warm` wird
   geschrieben (§2) · B Kern nur im Sandwiches-Modul, SB liefert lediglich Seelen-Lava/Eimer und **keine** Tiegel-Stufen
   (SB allein hat keinen Tiegel) · C Kern nur in SB, Sandwiches-Modul ohne Tiegel (verstößt gegen „eigenständig
   spielbar“) · D eigenes drittes Modul `simplecrucible`, beide Mods nennen es nur optional.
2. **Name?** ★A EN „Crucible“ / DE „Schmelztiegel“ · B „Melting Pot“ / „Schmelztopf“ · C „Kochtiegel“.
3. **Name des Gar-Zustands (Tooltip/Jade)?** ★A EN „Crucing“ (dein Kunstwort) / DE „Im Tiegel“ · B „Smelting“ /
   „Schmilzt“ · C „Heating“ / „Erhitzt“.

### B. Stufen und Werte
4. **Stufenleiter?** ★A vier Stufen: Eisen 9 (1×), Verstärkt 18 (2×), **Netherit** 27 (4×), Enderit 27 (8×) mit
   Stapel ×2 – „Enderite 27“ im Diktat vermutlich Netherit · B drei Stufen wörtlich: Basis 9, Verstärkt 18, Enderit 27
   mit ×2 · C fünf Stufen: … Enderit 27 und eigene „Enderit+“ mit ×2.
5. **Tempo je Stufe?** ★A wie die SB-Öfen 1/2/4/8×, eigene Config-Schlüssel · B alle 1×, nur mehr Slots ·
   C halbe Ofen-Werte (1/1,5/2/4×) wegen Parallelität.
6. **Doppelte Erfahrung Netherit/Enderit wie Öfen?** ★A ja · B nein · C nur Enderit.
7. **Erz-Ausbeutebonus wie Netherit-/Enderit-Schmelzofen?** A ja · ★B nein (Parallelität ist der Vorteil).
8. **Zusätzliches Werkbank-Rezept für den Eisen-Tiegel (neben der Herstellung in der Welt)?** ★A nein, nur in der Welt
   (Besitzer-Vorgabe) · B ja, `I.I/IKI/III` (Eisen + Kessel) als Ausweichweg für Automatisierung/Modpacks.
9. **Aufwertung?** ★A Vorschlaghammer in der Welt wie die Öfen, Inhalt bleibt · B Schmiedetisch mit Vorlagen ·
   C Werkbank (Tiegel + Material).
10. **Material Basis → Verstärkt?** ★A Rissiger Diamant (wie Kupfertruhe → verstärkte Truhe) · B 4 Rissige Diamanten
    in der Werkbank · C Eisenblock.
11. **Netherit → Enderit nötig, oder Verstärkt direkt zu Enderit?** ★A Kette wie Öfen (Netherit-, dann Enderit-Nugget)
    · B Verstärkt → Enderit direkt.

### C. Garen und Slots
12. **Wie gart ein Slot?** ★A ein Item nach dem anderen aus dem Stapel, alle Slots parallel · B ganzer Stapel als ein
    Vorgang (Zeit × Anzahl) · C nur 1 Item je Slot erlaubt.
13. **Wohin mit dem Ergebnis?** ★A erst auf gleichen Ergebnisstapel, sonst nächster freier Slot (umlaufend), eigener
    Slot wenn leer · B immer zuerst der eigene Slot, sonst nichts (rot) · C nur in ein festes Ergebnis-Gitter.
14. **Ergebnisse weitergaren (Bruchstein→Stein→Glatter Stein)?** ★A nein, Ergebnis-Slots gesperrt bis entnommen ·
    B ja, Ketten gewollt · C gesperrt, per Klick freigebbar.
15. **Welche Rezepttypen?** ★A `smelting`, dazu Schmelzofen-/Räucherofen-only-Rezepte (Mod-Rezepte) · B nur
    `smelting` · C alle vier inkl. Lagerfeuer, jeweils deren Zeit.
16. **Garzeit?** ★A wie normaler Ofen (smelting-Zeit), Schmelzofen-only × 2 · B kürzeste Zeit aller passenden
    Rezepte (Essen/Erze doppelt so schnell) · C Ofenzeit, Hitze-Überschuss beschleunigt.
17. **Enderit-Schrott (2-h-Rezept) im Tiegel?** ★A ausgeschlossen (Tag `crucible_excluded`) · B erlaubt, aber nur
    ein Slot gleichzeitig · C erlaubt ohne Grenze.
18. **Gleiches Item in mehreren Slots parallel? (Weiche)** ★A ja, gebremst durch Hitze-Pflicht und Config
    `maxActiveSlots` · B nein – gleiche Rezepte teilen sich einen Garplatz („verschiedene Dinge parallel“ wörtlich),
    weitere warten grau · C ja, aber Gesamttempo des Tiegels gedeckelt (z. B. max. 9 Items gleichzeitig).
19. **Nicht garbare Items im Tiegel?** ★A darf ein Spieler hineinlegen (grau, blockiert Platz), Trichter nicht · B nie
    erlaubt · C erlaubt, werden ausgeworfen.
20. **Erfahrung wann?** ★A beim Entnehmen eines Ergebnis-Slots durch Spieler oder beim Abbau · B sofort beim Garen
    als Kugeln über dem Block · C gar keine.
21. **Mehrere Tiegel zu einem verbinden (Multiblock)?** ★A nein · B ja, nebeneinander teilen sie Hitze.

### D. Hitze
22. **Niedrige Stufe (unter „mittel“) – ✔ Stufen mittel/hoch/extrem vom Besitzer festgelegt.** ★A niedrig =
    Kerze, Fackel, Laterne, Seelenfackel/-laterne, Feuer, Seelenfeuer → nur Aufwärmen · B keine niedrige Stufe
    (Aufwärmen ab „mittel“) · C niedrig ohne Seelenfeuer; Seelenfeuer = mittel.
23. **Rezept → Mindesthitze?** ★A automatisch: Aufwärmen niedrig, Essen + Allgemein mittel, Erze/Metalle hoch,
    Sonderliste extrem; per Tag überschreibbar · B Essen mittel, Allgemein + Erze hoch · C nur per Datapack-Tags.
24. **Was braucht „extrem“ (nur Seelen-Lava)?** ★A Antiker Schrott, Rissiger Diamant → Diamant · B nur Antiker
    Schrott · C nichts, „extrem“ heißt nur „alles geht“ (ggf. mit Tempo-Bonus).
25. **Seelen-Lagerfeuer wo? (Lagerfeuer/Magma = mittel ist gesetzt)** ★A mittel wie Lagerfeuer · B hoch.
26. **Wo muss die Hitzequelle sein?** A nur direkt darunter · ★B darunter oder seitlich, höchste zählt (unten bleibt
    für Trichter frei, wenn die Hitze seitlich kommt) · C alle sechs Seiten · D 3×3 Fläche darunter.
27. **Mehrere Quellen?** ★A Maximum zählt · B Summe (4 Kerzen = Lagerfeuer), gedeckelt · C Maximum, +1 ab drei
    gleichen.
28. **Kerzen-Anzahl (1–4) relevant?** ★A nein, entzündet = niedrig · B 4 Kerzen = mittel.
29. **Gelöschtes Lagerfeuer / aus­gemachte Kerze?** ★A keine Hitze · B halbe Stufe (nur Aufwärmen).
30. **Lava fließend oder nur Quelle?** ★A Quelle, fließend und Lavakessel = hoch · B nur Quelle und Kessel.
31. **Hitze fällt mitten im Garen?** ★A pausieren, Fortschritt bleibt (blau) · B Fortschritt sinkt langsam wie Ofen
    ohne Brennstoff · C sofort auf 0.
32. **Hitze-Überschuss beschleunigt?** ★A nein · B +25 % je Stufe über Minimum (max. ×2) · C nur Seelen-Lava ×1,5.
33. **Hitze-Prüfung?** ★A bei Nachbaränderung + alle 20 Ticks · B jeden Tick.
34. **Tiegel entzündet Umgebung?** ★A nein · B bei Lava/Seelen-Lava wie Lava.
35. **Nether heißer?** ★A nein · B +1 Stufe im Nether · C Nether ersetzt fehlende Hitze bis „mittel“.
36. **Hitzequellen anderer Mods?** ★A Block-Tags `heat_source/<stufe>`, per Datapack erweiterbar · B fest im Code.

### E. GUI und Indikator
37. **Layout 9/18/27?** ★A Gitter: 1 / 2 / 3 Stück 3×3 nebeneinander mit Lücken, Thermometer links · B 27 als
    durchgehende 3×9-Truhenreihe · C 3×3-Gitter untereinander (höheres Fenster).
38. **Indikator-Darstellung?** ★A ganzer Slot-Hintergrund getönt, Fortschritt füllt von unten · B Rand + 2-px-Balken
    unten im Slot · C Tönung + Flammen-Symbol, Fortschritt als Kreis.
39. **Farben?** ★A gart = aufgehellt + orange Füllung; rot = kein Platz; blau = zu wenig Hitze; grüner Rand =
    Ergebnis; grau = kein Rezept · B nur die drei genannten · C wie A, Ergebnis ohne Markierung.
40. **Barrierefreiheit?** ★A Farbe + Ecksymbol (Flamme/Kreuz/Schneeflocke/Haken) + Tooltip-Zeile + Client-Option
    Farbenblind-Palette · B nur Farbe · C Farbe + Tooltip.
41. **Fortschrittsrichtung?** ★A von unten nach oben (wie „füllt sich“) · B links → rechts · C kreisförmig.
42. **Rezeptbuch im Tiegel?** ★A nein · B Vanilla-Ofen-Rezeptbuch.
43. **JEI/REI?** ★A eigene Kategorie „Schmelztiegel“ mit Mindesthitze und Aufwärmen · B nur als Katalysator der
    Ofen-Kategorie.
44. **Jade/Blockinfo?** ★A Stufe, Hitze, aktive/blockierte Slots · B nur Hitze · C nichts.
45. **Items im Block sichtbar (Renderer)?** ★A nein, nur Glut/Leuchten wenn aktiv · B bis zu 9 Items oben sichtbar.

### F. Automatisierung
46. **Trichter oben?** ★A erst gleiche Eingabestapel, sonst freier Slot, nie Ergebnis-Slots · B nur leere Slots.
47. **Seiten/unten?** ★A oben + Seiten einfügen, unten nur Ergebnisse entnehmen · B oben einfügen, Seiten und unten
    entnehmen · C wie Ofen (oben Eingabe, Seiten nichts, unten Ergebnisse).
48. **Was darf der Trichter einfüllen?** ★A nur garbare/aufwärmbare Items · B alles.
49. **Platzreserve für Ergebnisse?** ★A Trichter belegt nie den letzten freien Slot · B keine Reserve · C feste
    Aufteilung (z. B. linkes Gitter Eingabe, rechtes Ausgabe).
50. **Komparator?** ★A Füllstand wie Truhe · B Anzahl garender Slots · C 15 sobald ein Slot rot ist.
51. **Redstone-Steuerung?** ★A keine · B Signal pausiert den Tiegel.

### G. Stapel ×2, Abbau
52. **Stapelfaktor Enderit?** ★A ×2 wie gewünscht (Netherit ×1) · B ×4 wie die Enderit-Truhe (Netherit ×2) ·
    C ×2 nur für Ergebnisse.
53. **Abbau mit Inhalt?** ★A Inhalt droppt (normale Stapel) + Erfahrung, Fortschritt verloren · B Inhalt bleibt im
    Block-Item (wie Shulker) · C droppt, Erfahrung verloren.
54. **Fortschritt bei Aufwertung?** ★A bleibt erhalten · B wird zurückgesetzt.

### H. Seelen-Lava
55. **Fließweite?** ★A Overworld 2, Nether 4, zäher als Lava · B wie Lava (3/7) · C immer 1 (fast nur Quelle).
56. **Dimensionen?** ★A überall platzierbar · B nur Nether/End, in der Overworld verdampft sie wie Wasser im Nether ·
    C Overworld nur als Quelle ohne Fließen.
57. **„Nicht ersetzbar“ gilt für …?** ★A alles inkl. Kreativ-Spieler, Kolben (blockiert), Explosionen, Wither/Drache;
    nur `/setblock`/`/fill` gehen · B wie A, aber Kreativ-Spieler dürfen überbauen · C wie A, zusätzlich entfernt ein
    Schwamm Fließblöcke.
58. **Fließblöcke: entstehen sie überhaupt in fremde Blöcke hinein (Gras, Blumen)?** ★A ja, zerstören sie wie Lava ·
    B nur in Luft.
59. **Kontakt mit Wasser?** ★A keine Umwandlung der Seelen-Lava, Wasser verdampft zischend (passt zu „nur per Eimer“)
    · B Quelle → Weinender Obsidian, fließend → Basalt (Seelen-Lava wird entfernt) · C Seelen-Lava bleibt, der
    Wasserblock wird zu Schwarzstein (Generator möglich).
60. **Kontakt mit normaler Lava?** ★A nichts, Lava fließt nicht hinein · B Lava wird zu Basalt · C Lava wird zu
    Seelen-Lava (breitet sich aus – gefährlich).
61. **Schaden?** ★A 6 (1,5× Lava) + Seelenbrand 15 s, Feuerresistenz halbiert · B wie Lava, Feuerresistenz schützt ·
    C doppelt, Feuerresistenz nutzlos.
62. **Items in Seelen-Lava?** ★A verbrennen wie in Lava, Netherit/Enderit feuerfest · B alles verbrennt · C nichts.
63. **Setzt sie Feuer?** ★A wie Lava, auf Seelensand/-erde Seelenfeuer · B gar nicht · C häufiger als Lava.
64. **Licht?** ★A 15 · B 10 (wie Seelenfackel).
65. **Beschaffung? – ✔ vom Besitzer festgelegt: 0,5 % der Nether-Lava-Taschen, 10 % der Festungs-Lavaräume, sonst
    nirgends.** Restfragen 106–109.
66. **Seelen-Lava-Kessel?** ★A ja, als Lager/Hitzequelle (extrem), nur Eisen-/Enderit-Eimer, keine Vermehrung ·
    B nein, nur Quellblöcke.
67. **Dispenser?** ★A wie Spieler (Eisen-Eimer gießt aus und bricht, leere Eimer nehmen auf) · B Dispenser kann
    Seelen-Lava gar nicht · C nur mit Enderit-Eimer.
68. **Schwamm?** ★A saugt nicht (wie Lava) · B saugt Fließblöcke.
69. **Strider/Boote?** ★A Strider laufen darauf, Boote verbrennen · B wie Lava (Strider ja).
70. **Claims/Spawnschutz?** ★A Pflicht: fließt nicht in fremde Claims, Eimer dort gesperrt (simpletweaks-Muster) ·
    B egal.
71. **Unendliche Quelle (2 Quellen → neue)?** ★A nein · B ja (wie Wasser).
72. **Sicht unter Seelen-Lava?** ★A wie Lava, türkiser Nebel · B klarer (Seelen-Lava „durchsichtiger“).
73. **Textur?** ★A Lava-Animation in Seelenfeuer-Türkis · B dunkelblau mit Seelen-Gesichtern (wie Seelensand) ·
    C hellblau-weiß glühend.

### I. Eimer
74. **Kupfer-Eimer-Rezept?** ★A 3 Kupferbarren V-Form · B 3 Kupferbarren + Kupfernugget unten · C 5 Kupferbarren.
75. **Was kann der Kupfer-Eimer?** ★A alles wie der Eisen-Eimer (Wasser, Lava, Milch, Pulverschnee, Fische) außer
    Seelen-Lava · B nur Wasser und Lava · C Wasser, Lava, Milch.
76. **Kupfer-Eimer beim Lava-Ausgießen?** ★A zerbricht immer (Welt, Kessel, Dispenser), Lava wird platziert, kein
    Rest · B zerbricht, 1 Kupfernugget bleibt · C hält 4 Lava-Ladungen (Haltbarkeit).
77. **Kupfer-Eimer oxidiert?** ★A nein · B ja, nur optisch · C ja, oxidiert fasst er keine Lava mehr.
78. **Eisen-Eimer bei Seelen-Lava?** ★A Aufnehmen geht, zerbricht beim Ausgießen (Seelen-Lava wird platziert) ·
    B zerbricht schon beim Aufnehmen (Seelen-Lava verloren) · C 50 % Bruchchance beim Ausgießen.
79. **Enderit-Eimer-Herstellung?** ★A Schmiedetisch: Enderit-Vorlage + Eisen-Eimer + Enderit-Barren · B über Netherit-
    Eimer als Zwischenstufe · C Werkbank (Eimer + 3 Enderit-Barren).
80. **Enderit-Eimer-Extras?** ★A nur unzerbrechlich + feuerfest als Item · B zusätzlich 4 Ladungen derselben
    Flüssigkeit · C zusätzlich nicht stapelbar (Stapel 1).
81. **Kupfer-Lava-Eimer als Ofen-Brennstoff?** ★A ja, Eimer wird verbraucht (kein leerer Eimer zurück) · B kein
    Brennstoff · C ja, gibt Kupfer-Eimer zurück (widerspricht „zerbricht“).
82. **Seelen-Lava-Eimer als Brennstoff?** ★A nein · B ja, 2× Lava-Brenndauer.

### J. Warmes Essen
83. **Was ist aufwärmbar?** ★A Tag: gegartes Fleisch/Fisch, Ofenkartoffel, Brot, Suppen/Eintöpfe, Kürbiskuchen,
    Sandwiches · B nur Sandwiches · C jedes Essen.
84. **Aufwärmzeit?** ★A 5 s × Stufentempo, ganzer Stapel auf einmal · B 10 s je Stück · C wie Garen.
85. **Vorteil?** ★A 15 % schneller essen · B zusätzlich +1 Sättigung · C zusätzlich Kälteschutz (Pulverschnee) kurz.
86. **Bündel? – ✔ beantwortet: im Bündel ca. 2 Tag-Nacht-Zyklen warm (48 000 Ticks).** Restfrage: gilt das auch für
    das SB-Verstärkte Bündel, Köcher, Rucksäcke? ★A alle Bündel-artigen Behälter (Vanilla-Bündel aller Farben + SB-
    Verstärktes Bündel) · B nur Vanilla-Bündel · C zusätzlich Rucksäcke und Shulkerkisten.
87. **Wie lange warm? – ✔ beantwortet: ca. halber Tag-Nacht-Zyklus (12 000 Ticks = 10 min).**
88. **Zeitbasis?** ★A Weltzeit-Stempel: kühlt auch in Truhen/entladenen Chunks ab · B nur im Spielerinventar ·
    C Ingame-Uhrzeit (Schlafen kühlt sofort ab).
89. **Stapeln? – ✔ beantwortet: Mittelwert aller Items.** Restfrage 110.
90. **Anzeige? – ✔ Glow um die Items, Stärke ~ Restwärme.** ★A Glow + Tooltip mit Restzeit · B nur Glow · C Glow +
    Tooltip + Dampfpartikel beim Halten. (Form des Glows: Frage 111.)
91. **Machen auch Vanilla-Räucherofen/Lagerfeuer warm?** ★A nein, nur der Tiegel · B ja, alle Garstationen ·
    C Räucherofen ja, Ofen nein.
92. **Bereits warmes Essen erneut in den Tiegel?** ★A frischt die Zeit auf · B nimmt der Tiegel nicht an.

### K. Version, Config, Texturen, Rest
93. **26.2?** ★A 26.3 zuerst, auf 26.2 hinter `McVersion.CRUCIBLE` aus (Stubs) · B auch 26.2 vollständig ·
    C nur 26.3, 26.2 ohne jede Spur.
94. **Server-Grenzen?** ★A wie Tabelle §11 (harte Klemmung) · B weniger Schalter (nur Tempo + Warmdauer).
95. **Tiegel-Optik?** ★A Kessel-artiger Ziegelblock mit sichtbarer Glut oben, Stufen mit eigenen Formdetails
    (Nieten/Kanten) in Verstärkt-/Netherit-/Enderit-Paletten · B schwarzer Gusstopf · C Ofenfront mit 3×3-Fenster.
    (Vorschau mit A/B/C kommt vor dem Einbau.)
96. **Eimer-Optik?** ★A Kupfer-Eimer in Kupferfarben, Enderit-Eimer in der Enderit-Palette, je mit sichtbarem Inhalt ·
    B Kupfer mit Grünspan-Rand.
97. **Erfolge?** ★A drei: Tiegel bauen, Seelen-Lava schöpfen, Enderit-Eimer · B keine · C nur Seelen-Lava.
98. **Leuchten/Sound beim Garen?** ★A Licht 13 + Knistern/Partikel wie Ofen, sobald ein Slot gart · B kein Licht.
99. **Handbuch/Wiki?** ★A eigenes Kapitel im Maschinen-Handbuch + Wiki-Seiten (nur Code-belegte Aussagen) ·
    B nur Wiki.
100. **Testzentrale?** ★A eigene Station „Schmelztiegel“ mit allen Hitzequellen und Seelen-Lava-Becken · B keine.

### L. Nachtrag zu den Besitzer-Ergänzungen 2026-10-04
101. **Material beim Tiegel-Bau in der Welt?** ★A Eisenbarren in der Nebenhand, **je Schlag 1 Barren** (6 Stück);
     die Wände sehen wie Eisen-Druckplatten aus, die Griffe wie Eisenstäbe (nur Optik) · B Schläge 1–4 verbrauchen je
     eine **schwere Wägeplatte**, Schläge 5–6 je einen **Eisenstab** aus der Nebenhand (Eisenbarren nur zum Starten) ·
     C nur 1 Eisenbarren für den ganzen Vorgang (Eisenblock ist der Preis).
102. **Welcher Hammer genügt?** ★A jeder Vorschlaghammer (wie Kupfertruhe → Verstärkt) · B ab Eisen-Vorschlaghammer.
103. **Axt im Sandwiches-Modul – auch wenn SB geladen ist?** ★A nein: mit SB nur Vorschlaghammer (Axt-Weg samt Modul-
     Tiegel-Herstellung dann aus) · B Axt und Hammer gehen beide · C Axt nur, wenn SB fehlt, Modul-Tiegel bleibt aber
     herstellbar.
104. **Welche Axt, wie viel Haltbarkeit?** ★A jede Axt, 1 Haltbarkeit je Schlag · B ab Eisenaxt, 2 je Schlag ·
     C jede Axt, 5 je Schlag (Axt ist kein Schmiedewerkzeug).
105. **Abbruch mitten im Bau?** ★A Rohling bleibt als Block mit sichtbarem Stand stehen, später fortsetzen; abgebaut
     gibt er Eisenblock + verbrauchte Barren zurück · B Rohling gibt nur den Eisenblock zurück (Barren verloren) ·
     C Stand verfällt wie ein abgebrochener Hammer-Vorgang, kein Rohling.
106. **Lava-Taschen genau welche?** ★A nur die verborgenen Quellen im Netherrack (`spring_closed`, `_double`) ·
     B zusätzlich die offenen Lavafälle (`spring_open`) · C zusätzlich Lava-Seen in Basaltdeltas.
107. **Festungs-Lavaraum: wie viel wird ersetzt?** ★A die komplette Lava des Brunnens in diesem Raum · B nur die
     zentrale Quelle · C jede Lavaquelle der Festung einzeln mit 10 %.
108. **Gilt die Chance auch für schon erzeugte Chunks?** ★A nein, nur neu generierte · B ja, beim ersten Laden alter
     Festungen nachträglich (aufwendig, unsicher).
109. **Erneuerung?** ★A keine: endliche Ressource (selten = wertvoll) · B ein Seelen-Lava-Kessel füllt sich unter
     einer Seelen-Lava-Quelle sehr langsam nach (Tropfstein-Prinzip).
110. **Mittelwert mit kalten Items?** ★A kalte zählen als 0 (10 warme + 10 kalte = halbe Restzeit für alle) · B kalte
     und warme bleiben getrennt, nur warme mitteln · C Mittelwert nur, wenn beide Stapel warm sind.
111. **Form des Glows?** ★A warmer Orange-Saum um die Item-Silhouette, 4 Helligkeitsstufen · B pulsierendes
     Leuchten (Glint-artig, orange) · C Glow + aufsteigende Dampf-Pixel im Icon.
112. **Glow auch für gedroppte Items und in der Hand (3D)?** ★A ja überall · B nur in GUIs.

## 16. Getroffene Arbeitsannahmen (ohne eigene Frage)

Tiegel-Slots teilen Eingabe und Ausgabe (kein separates Ergebnisfeld); Lesereihenfolge links→rechts, oben→unten,
Gitter für Gitter; Ergebnis-Markierung wird mit dem Item gespeichert (BE-Bitmaske, nicht am Item); Enderit-Tiegel
explosionsfest wie Enderit-Ofen; Seelen-Lava nicht im Tag `minecraft:lava`; Eimer-Varianten als eigene Items (wie
Vanilla `lava_bucket`) statt Komponente; Warm-Komponente nur am Server gesetzt, Client liest sie für Tooltip/Partikel;
kein Tiegel-Rezept im 1.21.11-Zweig (Port-Run später); Simple-Money-Preise erst nach der Besitzerwahl.
