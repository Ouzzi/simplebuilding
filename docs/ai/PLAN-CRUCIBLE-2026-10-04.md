# Plan „Crucible / Schmelztiegel“ + Seelen-Lava + Kupfer-/Enderit-Eimer + warmes Essen (Queue Nachtrag 9, 2026-10-04)

Status: **nur Konzept + Fragebogen**, kein Code. Quelle: Besitzer-Sprachdiktat 2026-10-04, eingetragen in
`.claude/QUEUE.md` → „Besitzer 2026-10-04 (Nachtrag 9)“.
Runde 1 (F1–F37) ist beantwortet (§2); offen ist der kompakte Fragebogen Runde 2 (§17). Bis zur Antwort gilt jeweils die ★-Empfehlung als Arbeitsannahme.

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
dieser Plan verweist nur darauf; Konflikt und Lösungsvorschlag zur Bibliothek in §3.

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
  6 Schläge, Zwischenstände als Blockzustände statt Risse). Ohne SB gibt es keinen Hammer → die Axt
  übernimmt die Rolle (in der Bibliothek, §3).

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

**Modul-Prinzipien** (`docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`, Branch `claude-modprinciples` @ b35da044, gelesen)
- Regel 1/5: jedes Modul allein voll spielbar, Vanilla-Weg zuerst, höhere Stufen nur mit Partner und ohne ihn gar
  nicht registriert (Vorbild Enderit-Hufeisen). Regel 5 nennt den Crucible-Axt-Weg ausdrücklich als Beispiel.
- Regel 3: keine Klassenimporte über Modulgrenzen außer `framework.api`; `framework` = reine Java-Verträge, **keine
  Registry-Inhalte**, wird von jedem Nutzer selbst gebündelt (Fabric `include`, NeoForge `jarJar`, Forge
  `gradle/forge-framework.gradle` mit Jar-in-Jar-Versionsauswahl).
- Regel 6: **keine gemeinsame „Core“-Jar mit Items und keine Doppel-Items**; jeder Grundstoff hat genau einen
  Besitzer, andere nutzen dessen Tags.
- → Besitzerantwort F1 (Tiegel + Warm-Food in SB **und** Sandwiches, gemeinsamer Kern als Bibliothek) kollidiert mit
  Regel 3 und 6. Lösungsvorschlag siehe §2.

**Dörfer (Vanilla 26.3, beim Bau prüfen)**: fünf Dorftypen (Ebene, Wüste, Savanne, Taiga, Verschneit), Jigsaw-
Pools `minecraft:village/<typ>/houses|streets|decor|…`. Pools sind per Datapack nur **ersetzbar**, nicht ergänzbar →
übliche Lösung: beim Serverstart dem `StructureTemplatePool`-Registry-Eintrag ein zusätzliches Element mit Gewicht
anhängen (Mixin/Accessor auf `templates`/`rawTemplates`, auf allen Loadern gleich).

## 2. Entscheidungen Runde 1 (Besitzer-Antworten F1–F37, 2026-10-04 – gehen allen Abschnitten vor)

| F | Antwort | Folgerung im Plan |
|---|---|---|
| 1 | Warm-Food in SB **und** Sandwiches, Tiegel in beiden; **Enderit-Stufe und In-World-Umwandlung per Vorschlaghammer nur in SB**; gemeinsamer Kern evtl. als Bibliothek | §3 Bibliothek `simplecrucibles` (mit Prinzipien-Konflikt + Lösung) |
| 2 | A | EN „Crucible“ / DE „Schmelztiegel“ |
| 3 | A | Zustand EN „Crucing“ / DE „Im Tiegel“ |
| 4 | **Eisen 6, Verstärkt 9, Netherit 18, Enderit 27** | §4 Stufen, §7 GUI |
| 5 | A | Tempo 1/2/4/8× wie SB-Öfen, eigene Config-Schlüssel |
| 6 | A | Netherit/Enderit doppelte Erfahrung |
| 7 | Rückfrage „was ist das für ein Bonus?“ (Koordinator antwortet) | bleibt offen → Runde 2 Frage 1, präzisiert |
| 8 | A | kein Werkbank-Rezept für den Eisen-Tiegel, nur In-World-Bau |
| 9 | A, aber **teurer, evtl. doppelt** | Aufwertung per Vorschlaghammer mit **doppeltem Material und doppelten Schlägen** (wie Shulkerkisten) |
| 10 | A | Eisen → Verstärkt mit Rissigem Diamant (×2 nach F9) |
| 11 | A | Kette Verstärkt → Netherit-Nugget → Enderit-Nugget (je ×2) |
| 12 | A | je Slot ein Item nach dem anderen, alle Slots parallel |
| 13 | A, **zuerst der Slot direkt darunter**, sonst andere Zeile | §5 Ergebnis-Platzierung |
| 14 | A | Ergebnis-Slots gesperrt bis entnommen |
| 15 | **C** | alle vier Rezepttypen (smelting, blasting, smoking, campfire_cooking), jeweils deren Zeit |
| 16 | **Tempo nach Hitze**: niedrig 0,5×, hoch 0,75×, extrem 1× des Ofen-Äquivalents | Annahme **mittel 0,625×** (§6) |
| 17 | C, **aber extreme Hitze nötig** | Enderit-Schrott ohne Slot-Grenze erlaubt, braucht Seelen-Lava |
| 18 | **Keine Deckelung**; gleiche Items in allen Stapeln gleichzeitig, solange Platz; sonst zuerst Gegartes zuerst; **reservierte Ergebnis-Slots** zeigen das Ergebnis halbdurchsichtig; Item hineinlegen → Fortschritt stoppt, bleibt gespeichert bis zur Entnahme | §5 Reservierung, Config `maxActiveSlots` entfällt |
| 19 | A | Spieler darf Nicht-Garbares hineinlegen (grau), Trichter nicht |
| 20 | Erfahrung beim Entnehmen | wie Vanilla-Ofen |
| 21 | A | kein Multiblock |
| 22 | A (niedrig = Kerze/Fackel/Laterne/Seelenfackel/-laterne/Feuer/Seelenfeuer), aber F16 beachten; **Blasting-Rezepte erst ab hoher Hitze** | §6 Gate-Tabelle |
| 23/24 | siehe 22 und 16 | Zuordnung Rezepttyp → Mindesthitze, Sonderliste extrem |
| 25 | A | Seelen-Lagerfeuer = mittel |
| 26 | **nur direkt darunter oder 2 Blöcke darunter** (dann 10 % langsamer – Platz für einen Trichter dazwischen) | §6 Position |
| 27 | A | Maximum zählt |
| 28 | A | Kerzenzahl egal |
| 29 | A, aber **Nachglühen 2/4/8/16 s je Stufe** nach Entfernen der Quelle | §6 |
| 30 | A; **fließende (Seelen-)Lava eine Stufe niedriger**, im **Kessel dieselbe Stufe wie die Quelle** | §6 |
| 31 | A (pausieren, Fortschritt bleibt) + Nachglühen | §6 |
| 32 | siehe 16 | kein Überschuss-Bonus über die Hitzefaktoren hinaus |
| 33 | A | Prüfung bei Nachbaränderung + alle 20 Ticks |
| 34 | A, **normale Lava behält ihr normales Verhalten** | Tiegel entzündet nichts; Lava selbst unverändert |
| 35 | **B** | im Nether +1 Hitzestufe (Deckel siehe Runde 2) |
| 36 | A, aber **auf „hoch“ gedeckelt, falls nicht spezifiziert** | Datapack-/Fremd-Quellen max. hoch, extrem nur explizit |
| 37 | siehe neue Slotzahlen | §7 Layout 6/9/18/27 |
| neu | **Tiegel spawnen in Dörfern**, auf einem **ausgemachten Lagerfeuer** | §11 |

## 3. Modulrahmen: Bibliothek `simplecrucibles` (F1) und Prinzipien-Konflikt

**Konflikt.** F1 will einen gemeinsamen Tiegel-Kern für SB und Simple Sandwiches. Ein Tiegel ist Block + Block-Entity
+ Menü + Item + Komponente, also **Registry-Inhalt**. Das darf weder in `framework/` (Regel 3: reine Java-Verträge)
noch als „Core-Jar mit Items“ (Regel 6) liegen; zwei Kopien (SB- und Modul-Namensraum) wären „Doppel-Items“ (Regel 6).

**Lösungen:**
- ★ **A – eigenes kleines Bibliotheks-Modul `simplecrucibles` mit Inhalt, einem Namensraum, gebündelt.**
  `modules/simplecrucibles` (Mod-ID und Namensraum `simplecrucibles`, Paket `com.simplecrucibles`), 26.3-only.
  SB **und** Simple Sandwiches betten es ein (Fabric `include`, NeoForge `jarJar`, Forge über ein Gegenstück zu
  `gradle/forge-framework.gradle`). Der Loader lädt es genau einmal (höchste Version gewinnt) → **ein** Satz Items,
  keine Doppel-Items. Es ist selbst ein Mod (eigene `fabric.mod.json`/`mods.toml`) und auch allein startbar.
  Es exportiert eine kleine öffentliche API (`com.simplecrucibles.api`: Stufe registrieren, Hitzequelle melden,
  Warm-Komponente lesen/schreiben). **Prinzipien-Änderung nötig** (Vorschlag an den Prinzipien-Helfer, nicht hier
  geschrieben): Regel 3 um „gebündelte Inhalts-Bibliotheken mit eigener `api`“ ergänzen, Regel 6 um die Ausnahme
  „eine Inhalts-Bibliothek mit genau einem Namensraum, die mehrere Mods bündeln; Versionsauswahl über Jar-in-Jar“.
- B – **`framework` erweitern** auf Registry-Inhalte: bricht das Grundprinzip „framework ohne Minecraft-Klassen“
  und zwingt alle fünf framework-Nutzer, Minecraft-abhängig zu werden. Nicht empfohlen.
- C – **ein Besitzer, kein Bündeln**: Tiegel gehört Simple Sandwiches; SB bringt nur Enderit-Stufe/Seelen-Lava, wenn
  Sandwiches geladen ist. Prinzipientreu, aber SB allein hat keinen Tiegel (widerspricht F1).
- D – **zwei Kopien** (erste Fassung dieses Plans): verletzt Regel 6, doppelter Pflegeaufwand.

**Aufteilung bei A:**

| Teil | `simplecrucibles` (gebündelt, allein spielbar) | SimpleBuilding | Simple Sandwiches |
|---|---|---|---|
| Eisen-Tiegel (6) | ✔ Block/BE/Menü, In-World-Bau mit der **Axt** | zusätzlich Bau per Vorschlaghammer (bedingt, `#simplebuilding:sledgehammer_tools`) | bündelt nur |
| Verstärkt (9) / Netherit (18) | ✔ registriert; Weg ohne SB: Runde-2-Frage 6 | Aufwertung per Vorschlaghammer (×2 Material) | – |
| Enderit (27, ×2-Stapel) | – | ✔ `simplebuilding:enderite_crucible` über die Bibliotheks-API; ohne SB nicht registriert | – |
| Hitze niedrig/mittel/hoch | ✔ Tags `simplecrucibles:heat_source/*` | – | – |
| Hitze extrem (Seelen-Lava) | Tag-Eintrag `required:false` | ✔ Seelen-Lava, Kupfer-/Enderit-Eimer | – |
| Warm-Komponente, Esszeit, Bündel, Glow | ✔ `simplecrucibles:warm` | nutzt sie | Sandwiches per Tag `simplecrucibles:warmable_food` |
| Dorf-Tiegel | ✔ | – | – |

- Versionen: 26.3 zuerst (Fabric → NeoForge → Forge). SB-Kern kompiliert auf 26.2: dort wird die Bibliothek nicht
  gebündelt, alle SB-Teile (Enderit-Tiegel, Seelen-Lava, Eimer) hinter `McVersion.CRUCIBLE` (26.3 true, 26.2 false).
- Server-Config `simplecrucibles-server.json` (Tiegel, Hitze, Warm) + SB `server.soulLava`, harte Grenzen (§13).
- Keine Bildschirmtexte; Rückmeldung über Slot-Indikator, Sound, Partikel, Tooltip, Jade.

## 4. Stufen (F4–F11)

| Stufe | ID | Slots | Layout | Tempo | Stapel | Nachglühen | Besitzer |
|---|---|---|---|---|---|---|---|
| Eisen | `simplecrucibles:iron_crucible` | 6 | 3×2 | 1× | 1× | 2 s | Bibliothek |
| Verstärkt | `simplecrucibles:reinforced_crucible` | 9 | 3×3 | 2× | 1× | 4 s | Bibliothek |
| Netherit | `simplecrucibles:netherite_crucible` | 18 | 2 × 3×3 | 4× | 1× | 8 s | Bibliothek |
| Enderit | `simplebuilding:enderite_crucible` | 27 | 3 × 3×3 | 8× | ×2 (Runde 2) | 16 s | SB |

- Härte/Explosionsfestigkeit/Sound je Stufe wie die gleichnamigen Öfen; Netherit/Enderit doppelte Erfahrung.
- Bau Eisen-Tiegel in der Welt (Besitzer-Ergänzung 8): Rechtsklick halten auf **Eisenblock**, **Eisenbarren in der
  Nebenhand**, 1 Schlag/s; Schläge 1–4 = 4 Wände (Optik Eisen-Druckplatte), 5–6 = 2 Griffe (Optik Eisenstab),
  Zwischenstände als Rohling `simplecrucibles:crucible_blank` mit Blockzustand `stage`. Werkzeug: Axt (Bibliothek)
  bzw. zusätzlich Vorschlaghammer (SB, bedingt). Materialdetails Runde 2.
- Aufwertung (nur SB): Vorschlaghammer + Material in der Nebenhand wie die Öfen, **doppelt** (2 Stück Material,
  10 Schläge): Eisen → Verstärkt 2 Rissige Diamanten (jeder Hammer), Verstärkt → Netherit 2 Netherit-Nuggets
  (ab Diamant-Hammer), Netherit → Enderit 2 Enderit-Nuggets (ab Netherit-Hammer). Inhalt bleibt; 6→9→18→27 hängt
  Slots hinten an. SB registriert dafür in `SledgehammerUpgrades` die Bibliotheks-Blöcke per Registry-ID.

## 5. Verarbeitung (F12–F21)

- Je Slot ein Garvorgang, aus einem Stapel ein Item nach dem anderen; **alle Slots parallel, keine Deckelung**, auch
  gleiche Items in mehreren Stapeln gleichzeitig (F18).
- **Reservierung**: Beim Start eines Vorgangs reserviert der Slot seinen Ergebnisplatz – bevorzugt der Slot **direkt
  darunter** (gleiche Spalte, nächste Zeile im selben 3er-Gitter), sonst der nächste freie/passende Slot in einer
  anderen Zeile (Lesereihenfolge, umlaufend); ein vorhandener passender Ergebnisstapel geht vor, wenn er Platz hat.
  Ein eigener Slot, der durch das letzte Item frei wird, darf selbst Ergebnisplatz sein.
- Reservierte Slots zeigen das kommende Ergebnis **halbdurchsichtig**. Legt ein Spieler dort etwas hinein, **stoppt**
  der zugehörige Vorgang (rot), Fortschritt bleibt gespeichert, bis das Item wieder entnommen ist. Trichter füllen nie
  in reservierte Slots.
- Kein Platz für eine Reservierung → Slot rot, Vorgang startet nicht; sobald Platz frei wird, startet zuerst der
  Slot, dessen Item am frühesten fertig wäre (F18 „sonst das, was zuerst gegart wird“).
- Ergebnis-Slots gesperrt (nicht erneut gegart) bis entnommen (F14). Nicht-Garbares darf ein Spieler hineinlegen
  (grau), ein Trichter nicht (F19). Erfahrung beim Entnehmen (F20), doppelt ab Netherit (F6).
- Rezepttypen (F15 C): `smelting`, `blasting`, `smoking`, `campfire_cooking`. Passen mehrere, nimmt der Tiegel den
  **schnellsten, den die aktuelle Hitze erlaubt** (Gate §6). Zeit = Rezeptzeit ÷ Hitzefaktor ÷ Stufentempo.
- Enderit-Schrott (2-h-Schmelzofen-Rezept) erlaubt, ohne Slot-Grenze, aber nur bei **extremer** Hitze (F17).
- Rezept-Cache je Slot (letzter Treffer je Item) gegen 27 Lookups pro Tick.

## 6. Hitze (F22–F36)

| Stufe | Quellen (nur entzündet/aktiv) | Faktor (F16) | Erlaubte Rezepte / Vorgänge |
|---|---|---|---|
| kalt | – | – | nichts |
| niedrig | Kerze(n), Kerzenkuchen, Fackel, Laterne, Seelenfackel, Seelenlaterne, Feuer, Seelenfeuer | 0,5× | Aufwärmen, `campfire_cooking` (Annahme, Runde 2) |
| mittel | Lagerfeuer, Seelen-Lagerfeuer, Magmablock, **fließende Lava** | **0,625×** (Annahme: Mitte zwischen 0,5 und 0,75) | + `smelting`, `smoking` |
| hoch | Lava-Quelle, Lavakessel, **fließende Seelen-Lava** | 0,75× | + `blasting` (F22) |
| extrem | Seelen-Lava-Quelle, Seelen-Lava-Kessel (SB) | 1× | + Sonderliste `simplecrucibles:needs_extreme_heat` (Geschichtetes Rohenderit → Enderit-Schrott; weitere Runde 2) |

- **Position (F26)**: nur Block **direkt darunter** (100 %) oder **zwei Blöcke darunter** (Tempo −10 %, damit ein
  Trichter dazwischen passt). Mehrere → Maximum (F27). Kerzenzahl egal (F28), ausgeschaltet = keine Hitze (F29).
- **Fließend eine Stufe niedriger, Kessel wie Quelle (F30).**
- **Nachglühen (F29)**: Nach Wegfall der Quelle hält der Tiegel die letzte Stufe 2/4/8/16 s (Eisen/Verstärkt/
  Netherit/Enderit), danach pausieren die Vorgänge (blau), Fortschritt bleibt (F31).
- **Nether +1 Stufe (F35)**; Deckel Runde 2.
- Fremde/Datapack-Quellen über Tags `simplecrucibles:heat_source/<stufe>`; ohne ausdrückliche Stufe höchstens
  **hoch** (F36); extrem nur über den expliziten Tag.
- Prüfung bei Nachbaränderung + alle 20 Ticks (F33). Kein Überschuss-Bonus (F32). Der Tiegel entzündet nichts; Lava
  als Quelle verhält sich normal (F34).

## 7. GUI und Slot-Indikator (F37 + Runde 2)

- Layout: Eisen 3×2 mittig; Verstärkt 3×3; Netherit zwei 3×3 nebeneinander; Enderit drei 3×3 (9 Spalten, Lücken
  zwischen den Gittern, passt in die Vanilla-Breite 176). Links Thermometer (kalt … extrem) mit Symbol der wirksamen
  Quelle und Nachglüh-Anzeige; kein Brennstoffslot.
- Zustände je Slot (Hintergrund): **gart** = aufgehellt + Fortschrittsfüllung; **rot** = kein Platz/Reservierung
  blockiert; **blau** = Hitze zu niedrig; **reserviert** = Ergebnis halbdurchsichtig; **Ergebnis** = gesperrt;
  **grau** = kein Rezept. Darstellung/Farben/Barrierefreiheit Runde 2.
- Synchronisation über `ContainerData` (Fortschritt in Prozent + Zustandscode je Slot, 27 × 2 Werte).
- Block-Zustand `lit` (Licht, Partikel, Knistern), sobald ein Slot gart.

## 8. Automatisierung, Stapel, Abbau

- ★ oben und seitlich einfügen, **unten entnehmen** (nur Ergebnis-Slots) – F26 sieht dafür ausdrücklich den Trichter
  zwischen Tiegel und Hitzequelle vor. Nie in reservierte/Ergebnis-Slots einfügen. Details Runde 2.
- Enderit ×2: `BackpackItem.maxStackSizeIn`-Muster, `codecSafeCopy` (Grenze 99), vorhandene Trichter-/Komparator-
  Logik der Stufentruhen.
- Abbau: Inhalt droppt in normalen Stapeln + Erfahrung als Kugeln (Runde 2).

## 9. Seelen-Lava (nur SB)

- `FlowingFluid`-Paar + `LiquidBlock` ohne `replaceable()`: kein Überbauen, andere Flüssigkeiten fließen nicht hinein,
  Kolben blockiert, Explosionen/Wither/Drache entfernen sie nicht; nur Eimer an der **Quelle** (Eisen- oder
  Enderit-Eimer) entfernt sie, Fließblöcke verschwinden danach.
- Weltgenerierung (Besitzer-Ergänzung 11): Nether-Lava-Taschen 0,5 %, Netherfestungs-Lavaraum 10 %, sonst nirgends.
  Spring-Feature-Zwilling bzw. Mixin; Festung über `NetherFortressPieces$CastleEntrance`.
- Nicht in `minecraft:lava` taggen; Schaden/Feuer/Schwimmen selbst (NeoForge/Forge `FluidType`, Fabric Render-Handler).
- Claims/Spawnschutz: fließt nicht in fremde Claims, Eimer dort gesperrt (simpletweaks-Muster) – Pflicht.
- Als Hitzequelle: Quelle/Kessel extrem, fließend hoch (F30).
- Offene Punkte (Fließweite, Kontakte, Gefahr, Optik, Kessel/Dispenser) → Runde 2.

## 10. Eimer (nur SB)

- Kupfer-Eimer: nimmt Lava, aber **keine Seelen-Lava**; **zerbricht beim Ausgießen von Lava** (Lava wird platziert).
- Eisen-Eimer: nimmt Seelen-Lava, **zerbricht beim Ausgießen** von Seelen-Lava.
- Enderit-Eimer: Schmiedetisch direkt vom Eisen-Eimer, zerbricht nie.
- Details (Rezept, Inhalte, Brennstoff, Extras) → Runde 2.

## 11. Dorf-Tiegel (neu, Bibliothek)

- ★ eigenes Kleinst-Stück **„Feldküche“** (ca. 3×3): Eisen-Tiegel auf einem **ausgemachten Lagerfeuer**
  (`lit=false`), dazu Fass (Loot: rohes Fleisch/Kartoffeln/Brot) und Sitzgelegenheit. Je Dorftyp eine Material-
  Variante (Ebene Eiche, Wüste Sandstein, Savanne Akazie, Taiga Fichte, Verschneit Fichte mit Schnee),
  Gewicht so, dass etwa **jedes dritte Dorf** eine hat.
- Einbau: beim Serverstart dem Pool `minecraft:village/<typ>/houses` ein Element mit kleinem Gewicht anhängen
  (Accessor auf `StructureTemplatePool`, alle Loader gleich); Config-Schalter + Gewicht mit harter Grenze.
- Alternativen und Inhalt → Runde 2.

## 12. Warmes Essen (Bibliothek)

- Aufwärmbar: Tag `simplecrucibles:warmable_food` (Sandwiches als `required:false`). Ab Hitze niedrig, ganzer Stapel,
  Grundzeit 100 Ticks × Hitzefaktor/Stufentempo. Im Tiegel gegarte Speisen kommen warm heraus.
- Esszeit −15 %; warm 12 000 Ticks (halber Tag-Nacht-Zyklus), im **Bündel 48 000** (zwei Zyklen).
- Komponente `simplecrucibles:warm` = (`warmUntil` Weltzeit, Bündel-Restzeit); kühlt auch in entladenen Chunks ab.
- **Stapeln = Mittelwert** aller Items (Warm-Komponente aus der Stapelgleichheit ausgeblendet, Merge-Hook).
- **Glow** um das Item, Stärke ~ Restwärme (Item-Modell `range_dispatch` für eigene Items, Render-Mixin für fremde).

## 13. Server-Config (harte Grenzen)

| Schlüssel | Standard | Grenzen |
|---|---|---|
| `crucible.reinforcedSpeed` / `netheriteSpeed` / `enderiteSpeed` | 2 / 4 / 8 | 1..`MAX_MACHINE_SPEED` |
| `heat.factorLow` / `Medium` / `High` / `Extreme` | 0,5 / 0,625 / 0,75 / 1,0 | 0,1..1,0 |
| `heat.twoBelowPenalty` | 0,10 | 0..0,5 |
| `heat.afterglowSeconds` (je Stufe) | 2 / 4 / 8 / 16 | 0..60 |
| `heat.netherBonus` | 1 | 0..1 |
| `heat.recheckTicks` | 20 | 5..200 |
| `warm.baseTicks` | 100 | 20..1200 |
| `warm.durationTicks` / `bundleDurationTicks` | 12 000 / 48 000 | 1200..48 000 / 1200..96 000 |
| `warm.eatSpeedBonus` | 0,15 | 0..0,30 |
| `village.enabled` / `village.weight` | an / 2 | an/aus / 0..10 |
| SB `soulLava.springChance` / `fortressChance` | 0,005 / 0,10 | 0..0,05 / 0..0,5 |
| SB `soulLava.damage` | 6 | 1..20 |

## 14. Tests

- Bibliothek (Standalone-Target nach Prinzip 8): `crucible_tier_slots_6_9_18`, `crucible_parallel_same_item_all_stacks`,
  `crucible_result_prefers_slot_below`, `crucible_reserved_slot_ghost_and_block_pauses`, `crucible_result_not_recooked`,
  `crucible_recipe_type_gate_by_heat`, `crucible_heat_factor_times`, `crucible_heat_two_below_ten_percent_slower`,
  `crucible_flowing_lava_one_level_lower_cauldron_equal`, `crucible_afterglow_per_tier`, `crucible_nether_plus_one`,
  `crucible_foreign_heat_capped_high`, `crucible_hopper_bottom_results_only`, `crucible_axe_build_six_strikes`,
  `warm_*` (Bonus, Ablauf, Bündel, Mittelwert, Glow-Stufe), `village_field_kitchen_in_pool`.
- SB: `enderite_crucible_27_double_stack`, `crucible_sledgehammer_upgrade_double_cost`,
  `crucible_sledgehammer_build_conditional`, `enderite_scrap_needs_extreme_heat`, `soul_lava_*`, `*_bucket_*`.
- Sandwiches: `sandwich_warmable_via_library_tag`, Start ohne SB.
- Gates: `fabric-263`, `neoforge-263`, Modul-Targets, Standalone-Target, 26.2-/Forge-Compile, Wiki, `checkBalance`.

## 15. Phasen und Aufwand

| Phase | Inhalt | Aufwand |
|---|---|---|
| P0 | Prinzipien-Ergänzung abstimmen, Modul `simplecrucibles` anlegen, Bündeln in SB/Sandwiches (3 Loader) | 1 |
| P1 | Eisen-Tiegel, Axt-Bau, BE/Menü/Screen, Reservierung, Indikatoren, Rezept-Gate, Hitze | 2 |
| P2 | Verstärkt/Netherit, Trichter/Komparator, Jade/JEI | 1 |
| P3 | Warm-Food (Komponente, Esszeit, Bündel, Mittelwert, Glow) | 1,5 |
| P4 | Dorf-Feldküche (5 Varianten, Pool-Einhängen) | 0,75 |
| P5 | SB: Enderit-Tiegel, Vorschlaghammer-Bau/-Aufwertung, Seelen-Lava inkl. Worldgen, Eimer | 2,5 |
| P6 | Texturen/Strukturen (Vorschau A/B/C), NeoForge/Forge, Wiki/Handbuch, Gates | 1,5 |

Summe ≈ 10–10,5 Sitzungen. GPT-geeignet: P0-Gerüst, Lang/Wiki, Tag-Listen, Eimer-Varianten, Struktur-NBT-Varianten.

## 16. Risiken

- **Prinzipien**: ohne die Ausnahme für Inhalts-Bibliotheken (Regel 3/6) ist Lösung A nicht zulässig → vor P0 klären.
- **Jar-in-Jar mit Inhalt**: Fabric/NeoForge wählen die höchste Version; SB und Sandwiches müssen kompatible
  Bibliotheksstände bündeln, sonst fehlen Blöcke (API-Version wie bei `framework`, Range in den Metadaten).
- **Balance**: keine Deckelung (F18) – 27 Slots × 8× bei extremer Hitze ≈ 216 Öfen in einem Block; Bremsen sind nur
  Hitzefaktoren und Seelen-Lava-Seltenheit.
- Fluid über drei Loader (`FluidType`), Eimer-/Kessel-/Dispenser-Logik hartcodiert → viele Mixins.
- Unersetzbarer Fließblock als Grief-/Falle (verborgene Taschen) → kurze Fließweite, Claims.
- Stapel-Mittelwert greift tief in Vanilla-Stapellogik ein; jede übersehene Stelle stapelt ohne Mittelwert.
- Reservierung + halbdurchsichtige Ghost-Items brauchen eigene Slot-Darstellung (Client-Code je Loader gleich halten).
- Dorf-Pools: Accessor-Mixin auf `StructureTemplatePool` ist versionsempfindlich; andere Mods, die Pools ersetzen.
- Texturen: „eigene Formen statt Recolors“.

## 17. Fragebogen Runde 2 (offen, kompakt; Empfehlung ★)

Die Fragen 38–112 der ersten Fassung sind hier gesichtet: Duplikate gestrichen, Nahes zusammengefasst; Fragen, die
durch Runde 1 bzw. die Besitzer-Ergänzungen schon entschieden sind, stehen unten unter „✔ erledigt“.

### A. Rahmen, Stufen, Bau
1. **Erz-Ausbeutebonus (F7, präzisiert).** Heute (`FurnaceTierPerks`, `bonusPeriod`/`earnsOutputBonus`): Nur der
   Netherit- und Enderit-**Schmelzofen** gibt bei jedem 4. bzw. 2. passenden Schmelzvorgang **einen Gegenstand mehr**
   (+25 % / +50 % Ausbeute), und nur für Rezepte, deren Zutat ausschließlich **Roheisen, Rohgold oder Rohkupfer** ist
   (Tag `simplebuilding:blast_furnace_bonus`); der Rissige Diamant ist ausgeschlossen (`furnace_bonus_excluded`).
   Soll der Tiegel das auch? A ja, Netherit +25 %, Enderit +50 % bei Blasting-Rezepten · ★B nein (Parallelität ist der
   Vorteil) · C nur Enderit-Tiegel bei extremer Hitze.
2. **Prinzipien-Lösung für den gemeinsamen Kern?** ★A Bibliotheks-Modul `simplecrucibles` mit Inhalt, gebündelt von
   SB und Sandwiches, Prinzipien-Ausnahme für Inhalts-Bibliotheken · B `framework` um Inhalte erweitern · C Tiegel
   gehört Sandwiches, SB ergänzt nur mit Sandwiches · D zwei Kopien.
3. **Bibliotheks-Name?** ★A `simplecrucibles` „Simple Crucibles“ · B `simplekitchen` (Platz für Küchen-Kram) ·
   C `simplecore` (allgemeine Inhalts-Bibliothek für künftige geteilte Blöcke).
4. **Taucht die Bibliothek als eigener Mod in Modlisten/Wiki auf?** ★A ja, als „Simple Crucibles (gebündelt)“ mit
   eigener Wiki-Seite · B versteckt, Inhalte erscheinen unter SB bzw. Sandwiches.
5. **Axt-Bau „In-World-Transformation nur in SB“?** F1 sagt „In-World-Umwandlung (Vorschlaghammer) nur in SB“, Ergänzung
   9 wollte ohne SB die Axt. ★A Axt-Bau bleibt in der Bibliothek (Bau ist kein Vorschlaghammer-Vorgang), Vorschlag-
   hammer-Bau und alle Aufwertungen nur mit SB · B ohne SB Werkbank-Rezept statt Axt-Bau · C Axt nur, wenn SB fehlt.
6. **Verstärkt/Netherit ohne SB (Prinzip 1: jedes Item braucht einen Vanilla-Weg)?** ★A Werkbank: Eisen-Tiegel +
   4 Diamanten → Verstärkt; Schmiedetisch: Netherit-Vorlage + Verstärkt-Tiegel + Netherit-Barren → Netherit
   (mit SB zusätzlich Vorschlaghammer) · B ohne SB nur der Eisen-Tiegel (Verstärkt/Netherit dann nicht registriert)
   · C ohne SB Aufwertung per Axt-Schlägen mit Diamant/Netherit-Barren in der Nebenhand.
7. **Material beim Eisen-Tiegel-Bau?** ★A 1 Eisenbarren je Schlag (6), Wände/Griffe nur Optik · B Schläge 1–4 je eine
   schwere Wägeplatte, 5–6 je ein Eisenstab aus der Nebenhand (Eisenstab nur 26.3/SB → ohne SB Eisenbarren) ·
   C 1 Eisenbarren für alles.
8. **Werkzeug-Haltbarkeit beim Bau?** ★A jede Axt bzw. jeder Vorschlaghammer, 1 bzw. 2 je Schlag · B ab Eisen, 2 je
   Schlag · C Axt 5 je Schlag.
9. **Abbruch mitten im Bau?** ★A Rohling bleibt stehen, später fortsetzen; abgebaut gibt er Eisenblock + verbrauchte
   Barren zurück · B nur Eisenblock zurück · C kein Rohling, Stand verfällt.
10. **Stapelfaktor Enderit?** ★A ×2 (wie gewünscht) · B ×4 wie die Enderit-Truhe · C ×2 nur für Ergebnisse.
11. **Abbau mit Inhalt / Fortschritt bei Aufwertung?** ★A droppt Inhalt (normale Stapel) + Erfahrung, Fortschritt
    weg; bei Aufwertung bleibt alles · B Inhalt bleibt im Block-Item (wie Shulker) · C droppt, Erfahrung verloren.

### B. Garen, Hitze
12. **Rezept-Gate bestätigen:** ★A niedrig = Aufwärmen + Lagerfeuer-Rezepte (600 Ticks ÷ 0,5), mittel = + Ofen/
    Räucherofen, hoch = + Schmelzofen, extrem = + Sonderliste · B Lagerfeuer-Rezepte erst ab mittel (niedrig nur
    Aufwärmen) · C Räucherofen-Rezepte schon ab niedrig.
13. **Hitzefaktor „mittel“?** ★A 0,625× (Mitte) · B 0,5× wie niedrig · C 0,75× wie hoch.
14. **Sonderliste „extrem“ außer Enderit-Schrott?** ★A + Antiker Schrott, Rissiger Diamant → Diamant · B nur
    Enderit-Schrott · C + alle Netherit-/Enderit-Rezepte.
15. **Nether +1 – Deckel?** ★A höchstens bis hoch (extrem nur durch Seelen-Lava) · B bis extrem (Lava im Nether =
    extrem) · C nur niedrig → mittel.
16. **Was darf zwischen Tiegel und Quelle 2 Blöcke darunter stehen?** ★A Trichter, Luft oder ein nicht voller Block
    (Stufe, Gitter) · B nur Trichter · C jeder Block.
17. **Reserviertes Feld: Spieler legt genau das kommende Ergebnis-Item hinein?** ★A wird wie ein Ergebnisstapel
    behandelt, Vorgang läuft weiter · B stoppt trotzdem (immer stoppen, einfache Regel).
18. **Reservierung bei gleichem Item in vielen Slots** – Ergebnisse zusammenlegen? ★A ja, mehrere Vorgänge dürfen
    denselben Ergebnisstapel reservieren, solange die Summe hineinpasst · B jeder Vorgang eigener Ergebnisplatz.

### C. GUI, Anzeige
19. **Indikator-Darstellung + Richtung?** ★A ganzer Slot-Hintergrund getönt, Fortschritt füllt von unten nach oben ·
    B Rand + 2-px-Balken unten · C Tönung + Kreis.
20. **Farben + Barrierefreiheit?** ★A gart orange, rot kein Platz, blau zu kalt, grüner Rand Ergebnis, grau kein
    Rezept; dazu Ecksymbol (Flamme/Kreuz/Schneeflocke/Haken), Tooltip-Zeile, Client-Option Farbenblind-Palette ·
    B nur die drei genannten Farben · C Farben + Tooltip.
21. **Anzeigen außerhalb der GUI?** ★A kein Rezeptbuch; JEI/REI-Kategorie „Schmelztiegel“ mit Mindesthitze; Jade:
    Stufe, Hitze, Nachglühen, aktive/blockierte Slots · B Vanilla-Rezeptbuch dazu · C nur Jade.
22. **Items im Block sichtbar?** ★A nein, nur Glut/Leuchten wenn aktiv · B bis zu 9 Items oben sichtbar.

### D. Automatisierung
23. **Trichter einfügen?** ★A oben + Seiten, erst gleiche Eingabestapel, sonst freier Slot; nur garbare/aufwärmbare
    Items; nie reservierte/Ergebnis-Slots; lässt so viel frei, dass jeder Vorgang einen Ergebnisplatz findet ·
    B nur oben, keine Reserve · C feste Aufteilung (oberes Gitter Eingabe, unteres Ausgabe).
24. **Komparator/Redstone?** ★A Füllstand wie Truhe, keine Redstone-Steuerung · B Anzahl garender Slots · C Signal
    pausiert den Tiegel.

### E. Seelen-Lava (SB)
25. **Fließweite/Dimensionen?** ★A Overworld 2, Nether 4, zäher als Lava, überall platzierbar · B wie Lava (3/7) ·
    C immer 1.
26. **„Nicht ersetzbar“ – Umfang?** ★A alles inkl. Kreativ-Spieler, Kolben, Explosionen; nur `/setblock`/`/fill`;
    Fließblöcke zerstören Gras/Blumen wie Lava · B Kreativ darf überbauen · C zusätzlich Schwamm entfernt Fließblöcke.
27. **Kontakt mit Wasser/Lava?** ★A keine Umwandlung, Wasser verdampft, Lava fließt nicht hinein · B Quelle →
    Weinender Obsidian, fließend → Basalt · C Seelen-Lava bleibt, Wasser wird Schwarzstein.
28. **Gefahr (Schaden, Items, Feuer, Strider)?** ★A Schaden 6 + Seelenbrand 15 s, Feuerresistenz halbiert; Items
    verbrennen außer Netherit/Enderit; entzündet wie Lava (auf Seelensand Seelenfeuer); Strider laufen darauf ·
    B wie Lava · C doppelt, Feuerresistenz nutzlos.
29. **Optik?** ★A Lava-Animation in Seelenfeuer-Türkis, Licht 15, türkiser Nebel · B dunkelblau mit Seelen-Gesichtern ·
    C hellblau-weiß, Licht 10.
30. **Kessel/Dispenser/Schwamm?** ★A Seelen-Lava-Kessel als Lager + Hitze (extrem), Dispenser wie Spieler, Schwamm
    saugt nicht · B kein Kessel · C Dispenser kann Seelen-Lava nicht.
31. **Weltgenerierung im Detail?** ★A nur verborgene Taschen (`spring_closed`/`_double`); im Festungsraum die ganze
    Brunnen-Lava; nur neu generierte Chunks; keine Erneuerung (endlich) · B auch offene Lavafälle · C Festung nur
    zentrale Quelle · D alte Chunks nachträglich.

### F. Eimer (SB)
32. **Kupfer-Eimer: Rezept, Inhalte, Oxidation?** ★A 3 Kupferbarren V-Form; alles wie Eisen-Eimer außer Seelen-Lava;
    keine Oxidation · B nur Wasser/Lava · C oxidiert, oxidiert keine Lava.
33. **Kupfer-Eimer beim Lava-Ausgießen?** ★A zerbricht immer (Welt, Kessel, Dispenser), kein Rest · B 1 Kupfernugget
    bleibt · C 4 Ladungen Haltbarkeit.
34. **Eisen-Eimer bei Seelen-Lava?** ★A Aufnehmen geht, zerbricht beim Ausgießen · B zerbricht schon beim Aufnehmen ·
    C 50 % beim Ausgießen.
35. **Enderit-Eimer?** ★A Schmiedetisch Enderit-Vorlage + Eisen-Eimer + Enderit-Barren; unzerbrechlich, als Item
    feuerfest · B über Netherit-Eimer · C zusätzlich 4 Ladungen.
36. **Brennstoff?** ★A Kupfer-Lava-Eimer brennt, Eimer wird verbraucht; Seelen-Lava-Eimer ist kein Brennstoff ·
    B beide kein Brennstoff · C Seelen-Lava 2× Lava.

### G. Warmes Essen
37. **Was ist aufwärmbar?** ★A gegartes Fleisch/Fisch, Ofenkartoffel, Brot, Suppen/Eintöpfe, Kürbiskuchen,
    Sandwiches · B nur Sandwiches · C jedes Essen.
38. **Welche Behälter zählen als „Bündel“ (48 000 Ticks)?** ★A alle Vanilla-Bündel + SB-Verstärktes Bündel · B nur
    Vanilla-Bündel · C zusätzlich Rucksäcke/Shulker.
39. **Zeitbasis?** ★A Weltzeit-Stempel (kühlt auch in Truhen/entladenen Chunks) · B nur im Spielerinventar ·
    C Ingame-Uhrzeit.
40. **Mittelwert mit kalten Items?** ★A kalte zählen als 0 · B kalt und warm stapeln getrennt · C Mittelwert nur
    zwischen warmen Stapeln.
41. **Glow: Form und Orte?** ★A Orange-Saum um die Silhouette, 4 Stufen, überall (GUI, Hand, gedroppt) + Tooltip-
    Restzeit · B pulsierendes Glint-Leuchten · C Saum + Dampf-Pixel; nur GUI.
42. **Andere Garstationen machen warm?** ★A nur der Tiegel · B alle · C Räucherofen ja.
43. **Erneutes Aufwärmen?** ★A frischt auf volle Dauer auf · B nimmt warmes Essen nicht an.

### H. Dorf-Tiegel (neu)
44. **Form?** ★A eigenes Kleinst-Stück „Feldküche“ im Häuser-Pool · B Prozessor ersetzt in Metzger-Häusern den
    Räucherofen durch Tiegel + Lagerfeuer · C Deko-Element an Straßen (wie Laternen).
45. **Welche Dorftypen, wie häufig?** ★A alle fünf, etwa jedes dritte Dorf · B alle fünf, jedes Dorf mindestens eine ·
    C nur Ebene/Taiga/Verschneit (kalt = warmes Essen), jedes zweite.
46. **Inhalt?** ★A Tiegel leer, Fass mit rohem Essen · B Tiegel enthält 2–4 Sandwiches/gegartes Essen (Loot) · C
    Tiegel mit rohem Fleisch, das nach Anzünden des Lagerfeuers gart.

### I. Version, Begleitmaterial, Texturen
47. **26.2?** ★A Bibliothek 26.3-only, SB-Teile auf 26.2 hinter `McVersion.CRUCIBLE` aus · B auch 26.2 vollständig.
48. **Begleitmaterial?** ★A Erfolge (Tiegel bauen, Seelen-Lava schöpfen, Enderit-Eimer), Handbuch-Kapitel, Wiki
    (nur Code-belegt), Testzentrale-Station · B nur Wiki.
49. **Texturen?** ★A Tiegel: Eisenkessel-Form mit Griffen, Stufen mit eigenen Formdetails (Nieten/Kanten) statt
    Recolor; Eimer Kupfer-/Enderit-Palette mit sichtbarem Inhalt; Vorschau A/B/C vor Einbau · B schwarzer Gusstopf ·
    C Ofenfront mit Gitterfenster.

**✔ erledigt (aus der ersten Fassung, keine Frage mehr):**
- alt 37 Layout → neue Slotzahlen (F4, F37): 3×2 / 3×3 / 2×3×3 / 3×3×3.
- alt 47 Seiten/unten → unten entnehmen folgt aus F26 (Trichter zwischen Tiegel und Quelle); Rest in Frage 23.
- alt 49 Platzreserve → durch Reservierung (F18) ersetzt; Rest in Frage 23.
- alt 54 Fortschritt bei Aufwertung → in Frage 11.
- alt 65 Beschaffung → Besitzer-Ergänzung 11 (nur Worldgen); Detail in Frage 31; alt 71 und 109 (Erneuerung) dort.
- alt 70 Claims/Spawnschutz → Pflicht (Repo-Regel), keine Wahl.
- alt 85 Vorteil → 15 % schneller essen (Diktat); Extras gestrichen.
- alt 86/87 Bündel und Dauer → 48 000 / 12 000 Ticks (Ergänzung 7).
- alt 89 Stapeln → Mittelwert (Ergänzung 7); Rest Frage 40.
- alt 90/111/112 Anzeige → Glow (Ergänzung 7); Rest Frage 41.
- alt 94 Server-Grenzen → Tabelle §13 (Repo-Regel harte Grenzen).
- alt 98 Licht/Sound → Annahme wie Ofen (§7), keine Frage.
- alt 101–105 → Fragen 7–9; alt 103 → Frage 5; alt 106–108 → Frage 31; alt 110 → Frage 40.
- Ersetzt durch Runde-1-Antworten: alt 22–36 (Hitze), 12–21 (Garen), 4–11 (Stufen), 1–3 (Rahmen).

## 18. Arbeitsannahmen (ohne eigene Frage)

Tiegel-Slots teilen Eingabe und Ausgabe; Reservierungen und Ergebnis-Markierungen als BE-Bitmasken; Lesereihenfolge
Gitter für Gitter; Hitzefaktor „mittel“ 0,625×; Tiegel-Licht 13 + Ofen-Partikel wenn aktiv; Seelen-Lava nicht im
Tag `minecraft:lava`; Eimer-Varianten als eigene Items; Warm-Komponente nur serverseitig gesetzt; kein Tiegel im
1.21.11-Zweig; Simple-Money-Preise nach der Besitzerwahl.
