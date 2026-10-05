# Plan „Crucible / Schmelztiegel“ + Seelen-Lava + Kupfer-/Enderit-Eimer + warmes Essen (Queue Nachtrag 9, 2026-10-04)

Status: **nur Konzept + Fragebogen**, kein Code. Quelle: Besitzer-Sprachdiktat 2026-10-04, eingetragen in
`.claude/QUEUE.md` → „Besitzer 2026-10-04 (Nachtrag 9)“.
Fragebogen **abgeschlossen** (Runde 1 §2, Runde 2 §2b, Runde 3 §2c); diese Tabellen gehen dem übrigen Text vor. Status: **Umsetzung läuft** (Phasen §15).

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
| 1 | Warm-Food in SB **und** Sandwiches, Tiegel in beiden; **Enderit-Stufe und In-World-Umwandlung per Vorschlaghammer nur in SB**; gemeinsamer Kern evtl. als Bibliothek | §3 Bibliothek `simplelib` (mit Prinzipien-Konflikt + Lösung) |
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

## 2b. Entscheidungen Runde 2 (Besitzer-Antworten Fragen 1–31, 2026-10-04 – gehen vor)

| Frage | Antwort | Folgerung im Plan |
|---|---|---|
| 1 Erz-Bonus | **nicht übernehmen und aus den anderen SB-Öfen entfernen** | Tiegel ohne Ausbeutebonus. Entfernen aus `FurnaceTierPerks` (Netherit-/Enderit-Schmelzofen) macht **ein anderer Helfer im Code** – hier nur vermerkt; Wiki/Jade/Tests dort mitziehen |
| 2 Prinzipien-Lösung | A | gebündelte Inhalts-Bibliothek |
| 3 Name | B, aber Bibliotheken enden immer auf **„lib“**; besser eine **allgemeine `simplelib`** | §3 auf `simplelib` umgestellt (Namensraum, Inhalt, Bündelung, Prinzipien-Ausnahme als Textvorschlag) |
| 4 eigener Mod sichtbar | A | `simplelib` erscheint in Modlisten + eigener Wiki-Seite |
| 5 Axt-Bau | siehe 3 / 6 | Axt-Bau liegt in `simplelib` |
| 6 Weg ohne SB | **Prinzip: Gibt es keinen funktionierenden Mod-Weg, gibt es einen Vanilla-Weg, bei dem die Axt den Vorschlaghammer ersetzt** | §4: auch die Aufwertungen Eisen → Verstärkt → Netherit gehen ohne SB per Axt mit Vanilla-Material (Material Frage 50) |
| 7 Bau-Material | **B** + in **JEI** und im **Guide** | Schläge 1–4 je eine schwere Wägeplatte, 5–6 je ein Eisenstab (ohne SB/26.2: Eisenbarren) |
| 8 Haltbarkeit | A | Axt 1, Vorschlaghammer 2 je Schlag |
| 9 Abbruch | A | Rohling bleibt, abgebaut Rückgabe aller Teile |
| 10 Stapel Enderit | A | ×2 |
| 11 Abbau | A | Inhalt droppt + Erfahrung, Aufwertung behält alles |
| 12 Rezept-Gate | A, aber durch 13 überholt | siehe neue Hitzetabelle §6 |
| 13 Faktor mittel | Rückfrage: **„Warum mittel und niedrig? Mittel reicht, hoch ist erst Ofen-Niveau. Genau auflisten mit Materialien.“** | §6 neu: ★ drei Stufen mittel/hoch/extrem, Alternative mit niedrig → Frage 51 |
| 14 Sonderliste | mit 13 klären | in Frage 51 enthalten |
| 15 Nether-Deckel | A | Nether +1 höchstens bis hoch |
| 16 zwischen Tiegel und Quelle | A | Trichter, Luft oder nicht voller Block |
| 17 Ergebnis-Item ins reservierte Feld | A | gilt als Ergebnisstapel, läuft weiter |
| 18 gemeinsame Reservierung | A | mehrere Vorgänge teilen einen Ergebnisstapel, solange die Summe passt |
| 19 Indikator | A | Hintergrund getönt, Füllung von unten |
| 20 Farben | A | orange/rot/blau/grüner Rand/grau + Ecksymbole, Tooltip, Farbenblind-Option |
| 21 Anzeigen | A, **Items im Tiegel sichtbar, 3D gestapelt** | kein Rezeptbuch, JEI-Kategorie, Jade; Block-Entity-Renderer |
| 22 Items sichtbar | B | BER zeigt den Inhalt (§7) |
| 23 Trichter | A | oben + Seiten einfügen, Regeln §8 |
| 24 Komparator | A | Füllstand, keine Redstone-Steuerung |
| 25 Fließweite | **2 / 5**, aber **langsamer**; Berühren lässt **doppelt so lange brennen** wie Lava | Overworld 2, Nether 5; Brenndauer ×2 |
| 26 Unersetzbar | A, aber **Kreativ kann ersetzen**; bei Wasserkontakt wird die **Wasserseite umgewandelt**, die Seelen-Lava nicht | §9 |
| 27 Kontakt | siehe 26 | Wasserblock → Schwarzstein (Annahme, Frage 54) |
| 28 Gefahr | siehe 25, danach Effekt **„Seelenbrand“ 1 min**: alle paar Sekunden 50 % Brandschaden; **Feuerschutz verhindert den Schaden, nicht den Effekt** | §9; Begriff „Feuerschutz“ → Frage 55 |
| 29 Optik | A | Türkis-Lava, Licht 15, türkiser Nebel |
| 30 Kessel | A, aber **verstärkter Kessel** als Seelen-Lava-Lager | neuer Block `reinforced_cauldron` (Frage 56) |
| 31 Worldgen | **A und B** | verborgene **und** offene Nether-Lavaquellen, ganzer Festungsbrunnen, nur neue Chunks, keine Erneuerung |

**Neue Besitzer-Wünsche (Runde 2):**
- **Kupfer-Fass** (copper barrel, auch Stufen **Verstärkt** und **Enderit**) umgeht den Trichter-Abzug: per Vorschlaghammer
  (ohne SB Axt) in **6 Schlägen** an den Tiegel angebracht und **sichtbar verbunden**. In der Tiegel-GUI erscheinen
  zusätzlich **9 Fass-Felder**; der Tiegel legt/reserviert Ergebnisse **zuerst im Fass**, dann in eigenen Slots. Unter
  das Fass passt ein Trichter. Fass fest 9 Slots, Anzeige gemeinsam mit der Tiegel-GUI → §8a.
- **Seelen-Lava**: Entflammbarkeit ca. **4× höher** als Lava, **doppelte Reichweite** beim Feuer-Entzünden; „schmilzt
  ca. 10× mehr als Lava“ → ★ Deutung: als **Brennstoff 10× Lava** (§10). Alternative Deutungen Frage 57.

## 2c. Entscheidungen Runde 3 (Besitzer-Antworten Fragen 32–61, 2026-10-04 – Fragebogen abgeschlossen, gehen vor)

| Frage | Antwort | Folgerung im Plan / Umsetzung |
|---|---|---|
| 32 Kupfer-Eimer | **oxidiert beim Platzieren einer Flüssigkeit**; Axt entfernt Oxidation, **Wachsen** möglich; **kann keine Wasserquelle platzieren/geben** | Kupfer-Eimer mit Oxidationsstufe als Komponente (0–3, Optik), Axt schabt eine Stufe ab, Honigwabe wachst (keine Oxidation mehr); Wasser aufnehmen geht, Ausgießen von Wasser setzt **keine Quelle** (Annahme: nur fließendes Wasser, das abläuft) |
| 33 | A | Kupfer-Eimer zerbricht beim Lava-Ausgießen immer, kein Rest |
| 34 | A | Eisen-Eimer nimmt Seelen-Lava, zerbricht beim Ausgießen |
| 35 | **Werkbank: Eisen-Eimer, umringt von 8 Enderit-Nuggets** → Enderit-Eimer | statt Schmiedetisch |
| 36 | A, aber **Enderit-Eimer bricht nicht** | Kupfer-Lava-Eimer als Brennstoff verbraucht; Seelen-Lava-Eimer 10× Lava, Eisen-Eimer bricht, Enderit-Eimer kommt zurück |
| 37 | A – alles, was warm gegessen/serviert wird | Tag `simplelib:warmable_food` |
| 38 | A – alle Bündel inkl. Mod-Bündel | jedes `BundleItem` (Klasse) zählt |
| 39 | **kühlt überall ab, nur nicht im Tiegel** | Warm-Stempel läuft weiter, im Tiegel wird er laufend erneuert/eingefroren |
| 40–45 | A | kalte zählen 0 im Mittelwert; Glow-Saum 4 Stufen überall + Tooltip; nur der Tiegel wärmt; Aufwärmen frischt auf; Feldküche im Häuser-Pool; alle fünf Dorftypen, ca. jedes dritte Dorf |
| 46 | **A, B und C kombiniert** | Feldküche: Tiegel mit 1–3 Sandwiches/gegartem Essen (Loot), Fass daneben mit rohem Fleisch, ausgemachtes Lagerfeuer darunter |
| 47 | **später auch 1.21.11** | offener Port-Run-Punkt (Memory) |
| 48–50 | A | Erfolge/Handbuch/Wiki/Testzentrale; Texturen Eisenkessel-Form; Axt-Aufwertung ohne SB: 2 Diamanten bzw. 1 Netheritbarren |
| 51 | **A (mittel/hoch/extrem)** + **GUI-Hintergrund animiert, „cozy“**: mittel niedrige orange Flammen, hoch hohes Feuer, extrem blaues Feuer | §6 Tabelle A gilt; GUI-Animation §7 |
| 52 | A bzw. für später vorgemerkt | Sonderliste: Enderit-Schrott, Antiker Schrott, Rissiger Diamant |
| 53 | A, **Culling** nicht vergessen | BER nur nahe/sichtbare Items, Distanz-Culling |
| 54 | A, aber **Wasserquelle wird zu Quarzblock** (fließendes Wasser → Schwarzstein); dazu Hammer-Umwandlung **Quarzblock → 4 Quarz** (ohne SB Axt prüfen) | Vorschlaghammer-Umwandlung in SB; Axt-Weg nur, wenn er keinen Vanilla-Exploit schafft (Quarzblock → 4 Quarz ist verlustfrei wie Vanilla-Umkehr, also Axt ok) |
| 55 | **Feuerschutz schützt nur gegen Brennen und Brandschaden**; dauert der Seelenbrand länger als der Feuerschutz, kommt danach wieder Schaden | Seelenbrand-Effekt läuft immer; Schaden wird nur unterdrückt, solange Feuerresistenz bzw. Feuerschutz-Verzauberung wirkt |
| 56 | **B**: verstärkter Kessel per Aufwertung aus **4 Rissigen Diamanten**; ohne SB **4 Diamanten mit der Axt** | In-World-Aufwertung Kessel → verstärkter Kessel; Block in `simplelib` (Vanilla-Weg), SB fügt Hammer-Weg hinzu |
| 57 | A | Brennstoff 10× Lava |
| 58/59 | A, aber **ein nicht verbundenes Fass hat die normale Größe der Truhen-Stufen** | Fass allein: Kupfer 27, Verstärkt 36, Enderit 54 (wie Truhen-Stufen); am Tiegel nur 9 Felder sichtbar/genutzt |
| 60 | A | ein Fass je Tiegel, seitlich, 6 Schläge, Lösen durch Abbauen |
| 61 | **C** | alle dürfen ins Fass legen (Fremditems können Ergebnisplätze belegen) |

**Prinzipien:** Die Ausnahme für gebündelte `*lib`-Bibliotheken (§3) und das Axt-Prinzip (kein funktionierender Mod-Weg →
Vanilla-Weg mit Axt) sind vom Besitzer bestätigt und in `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md` eingetragen.

## 3. Modulrahmen: allgemeine Bibliothek `simplelib` (F1, Runde 2 Fragen 2–6)

**Konflikt.** Der gemeinsame Kern (Tiegel, Fass, Warm-Komponente, Axt-Ersatzwege) ist **Registry-Inhalt**. Er darf
nach Regel 3 nicht in `framework/` (reine Java-Verträge) und nach Regel 6 nicht in eine „Core-Jar mit Items“; zwei
Kopien wären Doppel-Items.

**Lösung (Besitzer Runde 2: A + Name mit „lib“, allgemein):** Modul **`simplelib`** (Mod-ID und Namensraum
`simplelib`, Paket `com.simplelib`, öffentliche API `com.simplelib.api`), 26.3-only, selbst ein Mod (eigene Metadaten,
eigene Wiki-Seite, in Modlisten sichtbar). SB und Simple Sandwiches **bündeln** es (Fabric `include`, NeoForge `jarJar`,
Forge analog `gradle/forge-framework.gradle` mit Versions-Range) → der Loader lädt genau eine Fassung (höchste Version),
es gibt jeden Block genau einmal. `simplelib` enthält **nur, was mehrere Mods tatsächlich teilen**; jedes Teil wird erst
aktiv, wenn ein bündelnder Mod es braucht bzw. wenn der bessere Mod-Weg fehlt (z. B. Axt-Aufwertungen nur ohne SB).

| Teil | `simplelib` | SimpleBuilding | Simple Sandwiches |
|---|---|---|---|
| Eisen-Tiegel (6) | ✔ Block/BE/Menü/BER; Bau per Axt (Vanilla-Weg) | Bau per Vorschlaghammer (bedingt) | bündelt |
| Verstärkt (9) / Netherit (18) | ✔ registriert; Aufwertung per **Axt** + Vanilla-Material, nur ohne SB (Prinzip Frage 6) | Aufwertung per Vorschlaghammer (×2 Material) | – |
| Enderit (27, ×2) | – | ✔ `simplebuilding:enderite_crucible` über `simplelib`-API | – |
| Kupfer-Fass + Verstärkt | ✔ (Anbringen per Axt ohne SB) | Anbringen per Vorschlaghammer | – |
| Enderit-Fass | – | ✔ `simplebuilding:enderite_barrel` | – |
| Hitze mittel/hoch | ✔ Tags `simplelib:heat_source/*` | – | – |
| Hitze extrem | Tag-Eintrag `required:false` | ✔ Seelen-Lava, verstärkter Kessel, Kupfer-/Enderit-Eimer | – |
| Warm-Komponente, Esszeit, Bündel, Glow | ✔ `simplelib:warm` | nutzt sie | Sandwiches per Tag `simplelib:warmable_food` |
| Dorf-Feldküche | ✔ | – | – |

**Textvorschlag für die Prinzipien** (zur Übernahme in `docs/ai/PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`; hier nicht dort
eingetragen):

> **Ausnahme zu Regel 3 und 6 – gebündelte Bibliotheken (`*lib`).** Teilen mehrere Mods Inhalte (Blöcke, Items,
> Komponenten), die jeder von ihnen allein braucht, liegen diese in **einer** Bibliothek, deren Mod-ID auf `lib` endet
> (Standard: `simplelib`). Sie hat genau einen Namensraum, ist selbst ein Mod und wird von jedem nutzenden Mod per
> Jar-in-Jar gebündelt (Fabric `include`, NeoForge `jarJar`, Forge Jar-in-Jar mit Versions-Range); der Loader wählt
> eine Fassung. Mods dürfen dafür Klassen aus `com.<lib>.api` importieren – sonst nichts aus der Bibliothek. Die
> Bibliothek kennt keinen ihrer Nutzer (kein Import, keine fremde ID ohne `required:false`/Condition). Sie liefert den
> **Vanilla-Weg** (Axt statt Vorschlaghammer, Vanilla-Material); ein bündelnder Mod mit besserem Weg schaltet den
> Vanilla-Weg über eine Condition ab. Inhalte, die nur einem Mod gehören (Enderit, Hammer), bleiben in diesem Mod.
> `framework/` bleibt inhaltsfrei.

- Versionen: 26.3 zuerst (Fabric → NeoForge → Forge). SB-Kern kompiliert auf 26.2: dort wird `simplelib` nicht
  gebündelt, alle SB-Teile hinter `McVersion.CRUCIBLE` (26.3 true, 26.2 false).
- Server-Config `simplelib-server.json` (Tiegel, Fass, Hitze, Warm, Dorf) + SB `server.soulLava`, harte Grenzen (§13).
- Keine Bildschirmtexte; Rückmeldung über Slot-Indikator, Sound, Partikel, Tooltip, Jade.

## 4. Stufen (F4–F11, Runde 2 Fragen 6–11)

| Stufe | ID | Slots | Layout | Tempo | Stapel | Nachglühen | Besitzer |
|---|---|---|---|---|---|---|---|
| Eisen | `simplelib:iron_crucible` | 6 | 3×2 | 1× | 1× | 2 s | simplelib |
| Verstärkt | `simplelib:reinforced_crucible` | 9 | 3×3 | 2× | 1× | 4 s | simplelib |
| Netherit | `simplelib:netherite_crucible` | 18 | 2 × 3×3 | 4× | 1× | 8 s | simplelib |
| Enderit | `simplebuilding:enderite_crucible` | 27 | 3 × 3×3 | 8× | ×2 | 16 s | SB |

- Härte/Explosionsfestigkeit/Sound je Stufe wie die gleichnamigen Öfen; Netherit/Enderit doppelte Erfahrung; **kein**
  Ausbeutebonus.
- **Bau Eisen-Tiegel in der Welt** (Ergänzung 8 + Frage 7 B): Rechtsklick halten auf **Eisenblock**, 1 Schlag/s;
  Schläge 1–4 verbrauchen je eine **schwere Wägeplatte** aus der Nebenhand (Wände), 5–6 je einen **Eisenstab** (Griffe;
  ohne SB bzw. auf 26.2 ersatzweise **Eisenbarren**). Rohling `simplelib:crucible_blank` mit `stage` 0–6 zeigt den
  Stand; abgebaut gibt er Eisenblock + alle verbrauchten Teile zurück (Frage 9). Werkzeug: **Axt** (Vanilla-Weg, 1
  Haltbarkeit je Schlag) bzw. mit SB **Vorschlaghammer** (2 je Schlag), dann ist der Axt-Weg per Condition aus.
  Beschrieben in **JEI** (In-World-Kategorie) und im **Guide** (Frage 7).
- **Aufwertung, doppelt (F9)**: 2 Stück Material, 10 Schläge, Inhalt/Fortschritt bleiben, Slots hinten angehängt.
  - mit SB (Vorschlaghammer): Eisen → Verstärkt 2 Rissige Diamanten, Verstärkt → Netherit 2 Netherit-Nuggets
    (ab Diamant-Hammer), Netherit → Enderit 2 Enderit-Nuggets (ab Netherit-Hammer).
  - ohne SB (Axt, Prinzip Frage 6): Eisen → Verstärkt ★ 2 Diamanten, Verstärkt → Netherit ★ 1 Netheritbarren
    (Vanilla hat kein Netherit-Nugget) – Frage 50. Enderit gibt es ohne SB nicht.

## 5. Verarbeitung (F12–F21)

- Je Slot ein Garvorgang, aus einem Stapel ein Item nach dem anderen; **alle Slots parallel, keine Deckelung**, auch
  gleiche Items in mehreren Stapeln gleichzeitig (F18).
- **Reservierung**: Beim Start eines Vorgangs reserviert der Slot seinen Ergebnisplatz – ist ein Fass angebracht,
  **zuerst im Fass** (§8a); sonst bevorzugt der Slot **direkt
  darunter** (gleiche Spalte, nächste Zeile im selben 3er-Gitter), sonst der nächste freie/passende Slot in einer
  anderen Zeile (Lesereihenfolge, umlaufend); ein vorhandener passender Ergebnisstapel geht vor, wenn er Platz hat.
  Ein eigener Slot, der durch das letzte Item frei wird, darf selbst Ergebnisplatz sein. Mehrere Vorgänge dürfen
  denselben Ergebnisstapel reservieren, solange die Summe passt (Frage 18); legt ein Spieler genau das Ergebnis-Item
  ins reservierte Feld, gilt es als Ergebnisstapel (Frage 17).
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
- **Kein Ausbeutebonus** (Runde 2 Frage 1); der Bonus der SB-Schmelzöfen wird von einem anderen Helfer entfernt.
- Rezept-Cache je Slot (letzter Treffer je Item) gegen 27 Lookups pro Tick.

## 6. Hitze (F22–F36, Runde 2 Fragen 13–16; Tabelle neu nach Besitzer-Rückfrage zu 13)

Rückfrage des Besitzers zu Frage 13: „Warum mittel und niedrig? Mittel reicht, hoch ist erst Ofen-Niveau.“ Daher:

**★ Vorschlag A – drei Stufen (ohne „niedrig“), hoch = Ofen-Niveau**

| Stufe | Quellen (Blöcke, nur entzündet/aktiv) | Beschaffung / Material | Tempo-Faktor | erlaubte Rezepttypen |
|---|---|---|---|---|
| kalt | Fackel, Kerze, Laterne, Feuer, Seelenfackel/-laterne/-feuer – **geben keine Hitze** | – | – | nichts |
| **mittel** | Lagerfeuer, Seelen-Lagerfeuer, Magmablock, fließende Lava | Lagerfeuer: 3 Stöcke + 1 Kohle/Holzkohle + 3 Stämme (erster Tag); Seelen-Lagerfeuer: Seelensand/-erde statt Kohle (Nether); Magmablock: 4 Magmacreme oder im Nether/Unterwasser-Schluchten abbauen; fließende Lava: Lavaquelle daneben | 0,5× | Aufwärmen, `campfire_cooking`, `smoking` (Essen, wie Lagerfeuer/Räucherofen) |
| **hoch** (Ofen-Niveau) | Lava-Quelle, Lavakessel, fließende Seelen-Lava (SB) | Eimer (3 Eisen) + Lava (Oberflächen-Seen, Höhlen unter Y 0, Nether); Lavakessel: Kessel 7 Eisen + Lavaeimer | 0,75× | + `smelting`, `blasting` (alles, was Ofen und Schmelzofen können) |
| **extrem** (nur SB) | Seelen-Lava-Quelle, Seelen-Lava im verstärkten Kessel | Seelen-Lava: Nether-Lavaquellen 0,5 %, Festungsbrunnen 10 %; Eisen-Eimer bricht beim Ausgießen, sonst Enderit-Eimer; verstärkter Kessel (Frage 56) | 1× | + Sonderliste `simplelib:needs_extreme_heat`: ★ Geschichtetes Rohenderit → Enderit-Schrott, Antiker Schrott → Netheritschrott, Rissiger Diamant → Diamant |

- Zeit = Rezeptzeit des schnellsten erlaubten Typs ÷ Tempo-Faktor ÷ Stufentempo. **Befund beim Bau:** In 26.3 tragen
  Ofen-, Räucherofen- und Schmelzofen-Rezepte alle 200 Ticks (der Tempovorteil liegt im Block, nicht im Rezept).
  Beispiel Roheisen im Eisen-Tiegel auf Lava (hoch): 200 Ticks ÷ 0,75 ≈ 267 Ticks (13,3 s); Enderit-Tiegel 8× → ≈ 34 Ticks.
- Mit SB ohne Seelen-Lava ist alles außer der Sonderliste erreichbar; ohne SB endet `simplelib` bei „hoch“.

**Alternative B – vier Stufen mit „niedrig“** (wie Runde 1): niedrig = Kerze, Fackel, Laterne, Feuer, Seelenfackel/
-laterne/-feuer, Tempo 0,5×, nur Aufwärmen + `campfire_cooking`; mittel 0,625× (+ `smoking`); hoch 0,75× (+ `smelting`,
`blasting`); extrem 1× (+ Sonderliste). Entscheidung → Frage 51.

- **Position (F26, Frage 16)**: Block **direkt darunter** (100 %) oder **zwei Blöcke darunter** (−10 %), dazwischen nur
  Trichter, Luft oder ein nicht voller Block. Mehrere → Maximum. Kerzenzahl egal, ausgeschaltet = keine Hitze.
- **Fließend eine Stufe niedriger, Kessel wie Quelle (F30).**
- **Nachglühen (F29)**: letzte Stufe hält 2/4/8/16 s (Eisen … Enderit), danach Pause (blau), Fortschritt bleibt.
- **Nether +1 Stufe**, höchstens bis **hoch** (F35, Frage 15).
- Fremde/Datapack-Quellen über Tags `simplelib:heat_source/<stufe>`; ohne Angabe höchstens **hoch** (F36).
- Prüfung bei Nachbaränderung + alle 20 Ticks. Kein Überschuss-Bonus. Der Tiegel entzündet nichts; Lava normal.

## 7. GUI, Slot-Indikator, Darstellung (F37, Runde 2 Fragen 19–22)

- Layout: Eisen 3×2; Verstärkt 3×3; Netherit zwei 3×3; Enderit drei 3×3 (9 Spalten, Lücken, Breite 176). Links
  Thermometer (kalt … extrem) mit Symbol der wirksamen Quelle und Nachglüh-Anzeige; kein Brennstoffslot. Ist ein Fass
  angebracht, erscheinen dessen **9 Felder** als zusätzliches 3×3 rechts (bzw. unter dem Tiegel-Raster beim Enderit-
  Tiegel; Fensterbreite prüfen, notfalls 3×3 rechts außen wie die Doppeltruhen-Breite) – §8a.
- Zustände je Slot (Frage 19/20 A): Hintergrund getönt, Füllung von unten = Fortschritt; **orange** gart, **rot** kein
  Platz/blockiert, **blau** zu kalt, **grüner Rand** Ergebnis, **grau** kein Rezept, **reserviert** = Ergebnis
  halbdurchsichtig; dazu Ecksymbol (Flamme/Kreuz/Schneeflocke/Haken), Tooltip-Zeile, Client-Option Farbenblind-Palette.
- Außerhalb (Frage 21 A): kein Rezeptbuch; JEI/REI-Kategorie „Schmelztiegel“ mit Mindesthitze, Aufwärmen, Bau und
  Aufwertung; Jade: Stufe, Hitze, Nachglühen, aktive/blockierte Slots, Fass.
- **Block-Entity-Renderer (Frage 21/22)**: Der Inhalt ist im offenen Tiegel **3D gestapelt** sichtbar – je belegtem
  Slot ein kleines Item-Modell (¼-Größe) in Raster-Ordnung, Stapel als 1–3 versetzte Kopien je nach Füllung, garende
  Items leicht glühend, Ergebnisse obenauf. Ab 18/27 Slots zwei/drei Lagen übereinander. Sichtweite und Anzahl
  begrenzt (Client-Option, Standard 16 Blöcke) – Performance.
- Synchronisation über `ContainerData` (Prozent + Zustandscode je Slot); Inhalt für den BER über das Block-Entity-
  Update-Paket (nur bei Änderung).
- Block-Zustand `lit` (Licht, Partikel, Knistern), sobald ein Slot gart.

## 8. Automatisierung, Stapel, Abbau (Runde 2 Fragen 10, 11, 23, 24)

- Trichter (Frage 23 A): oben + Seiten einfügen, zuerst auf gleiche Eingabestapel, sonst freier Slot; nur garbare/
  aufwärmbare Items; nie in reservierte/Ergebnis-Slots; lässt genug frei, dass jeder Vorgang einen Ergebnisplatz findet.
  Unten entnimmt ein Trichter (zwischen Tiegel und Quelle 2 darunter) nur Ergebnis-Slots.
- Komparator: Füllstand wie Truhe, keine Redstone-Steuerung (Frage 24 A).
- Enderit ×2: `BackpackItem.maxStackSizeIn`-Muster, `codecSafeCopy` (Grenze 99), Trichter-/Komparator-Logik der
  Stufentruhen.
- Abbau: Inhalt droppt in normalen Stapeln + Erfahrung; Aufwertung behält alles (Frage 11 A).

### 8a. Kupfer-Fass am Tiegel (neuer Wunsch Runde 2)

- Blöcke: ★ `simplelib:copper_barrel`, `simplelib:reinforced_barrel`, `simplebuilding:enderite_barrel`; je **9 Slots**,
  auch allein als normales Fass nutzbar (Frage 58). Unterschied der Stufen ★ Stapelfaktor 1/1/×2 (wie Tiegel) und
  Explosionsfestigkeit (Frage 59). Rezept Kupfer-Fass ★ Fass in der Mitte, 4 Kupferbarren im Kreuz (Vanilla-Material).
- **Anbringen**: Fass direkt neben (seitlich) den Tiegel stellen, mit Vorschlaghammer (ohne SB: Axt) **6 Schläge** auf das
  Fass → Blockzustand `attached` + Ausrichtung; Modell zeigt einen **Kupfer-Stutzen/Flansch** zum Tiegel und am Tiegel
  einen passenden Anschluss (beidseitiger Blockzustand). Lösen ★ durch Abbauen des Fasses oder erneute 6 Schläge mit
  Schleichen (Frage 60). Höchstens **ein Fass je Tiegel** (★, Frage 60).
- **Logik**: Der Tiegel reserviert und legt Ergebnisse **zuerst ins Fass** (gleiche Stapel zuerst, dann freier Fass-Slot),
  erst danach in die eigenen Slots (dann wie §5, Slot darunter bevorzugt). Das Fass nimmt **nur Ergebnisse** des Tiegels
  an; Spieler/Trichter können dort nichts einfüllen (★, Frage 61). Ein **Trichter unter dem Fass** zieht Ergebnisse ab →
  Hitzequelle bleibt direkt unter dem Tiegel (100 % Tempo statt −10 %).
- **GUI**: Tiegel-GUI zeigt die 9 Fass-Felder mit an (gleiche Indikatoren für reservierte Felder); das Fass allein
  geöffnet zeigt nur seine 9 Felder. Jade nennt das verbundene Fass.
- Aufwertung des Fasses ★ wie die Tiegel (Verstärkt: 2 Rissige Diamanten bzw. ohne SB 2 Diamanten; Enderit: SB,
  2 Enderit-Nuggets; ab Netherit-Hammer) – Frage 59.

## 9. Seelen-Lava (nur SB; Runde 2 Fragen 25–31 + neue Wünsche)

- `FlowingFluid`-Paar + `LiquidBlock` ohne `replaceable()`: Überlebens-Spieler, Bauzauberstab, Blaupausen, Endermen,
  Kolben (blockiert), Explosionen, Wither/Drache entfernen oder überbauen sie nicht; **Kreativ-Spieler dürfen ersetzen**
  (Frage 26). Fließblöcke zerstören Gras/Blumen wie Lava. Entfernen sonst nur per Eimer an der **Quelle**.
- **Fließweite Overworld 2, Nether 5, langsamer als Lava** (★ Takt 45/20 Ticks statt 30/10) (Frage 25).
- **Wasserkontakt (Frage 26/27)**: Die Seelen-Lava bleibt; der **Wasserblock** wird umgewandelt – ★ zu **Schwarzstein**
  (Frage 54). Normale Lava fließt nicht hinein.
- **Gefahr (Frage 25/28)**: Kontakt wie Lava, aber man **brennt doppelt so lange** (Lava 15 s → 30 s). Danach Effekt
  **„Seelenbrand“** (eigener `MobEffect`, 1 min): ★ alle 3 s 50 % Chance auf 1 Brandschaden (`soul_burn`-Schadensart,
  Tag `is_fire`). **Feuerschutz verhindert den Schaden, nicht den Effekt** (Begriff → Frage 55). Items verbrennen
  außer Netherit/Enderit; Strider laufen darauf.
- **Entflammbarkeit (neu)**: ca. **4× höher als Lava** und **doppelte Reichweite**. ★ Umsetzung: Vanilla-Lava versucht
  je Zufallstick 0–2 Zündungen in bis zu 2 Schritten Entfernung bzw. prüft 3 Nachbarn; Seelen-Lava macht **4× so viele
  Versuche** mit **bis zu 4 Schritten** (bzw. 6 Nachbarn); auf Seelensand/-erde entsteht Seelenfeuer. Config-Grenzen.
- **Optik (Frage 29 A)**: Lava-Animation in Seelenfeuer-Türkis, Licht 15, türkiser Nebel.
- **Lagerung (Frage 30)**: normaler Kessel nimmt **keine** Seelen-Lava; neuer **verstärkter Kessel**
  `simplebuilding:reinforced_cauldron` (★ Kessel + 4 Rissige Diamanten, Frage 56) nimmt Lava, Wasser, Pulverschnee und
  **Seelen-Lava** (dann Hitze extrem). Dispenser wie Spieler; Schwamm saugt nicht.
- **Weltgenerierung (Frage 31 A + B, Ergänzung 11)**: 0,5 % der Nether-Lavaquellen – **verborgene** (`spring_closed`,
  `_double`) **und offene** (`spring_open`) – werden Seelen-Lava; in Netherfestungen 10 % je Brunnenraum, dann die
  **ganze** Brunnen-Lava; nur neu generierte Chunks; keine Erneuerung.
- Nicht in `minecraft:lava` taggen; Schaden/Feuer/Schwimmen selbst (NeoForge/Forge `FluidType`, Fabric Render-Handler).
- Claims/Spawnschutz respektiert (Pflicht). Als Hitzequelle: Quelle/Kessel extrem, fließend hoch.

## 10. Eimer (nur SB)

- Kupfer-Eimer: nimmt Lava, aber **keine Seelen-Lava**; **zerbricht beim Ausgießen von Lava**.
- Eisen-Eimer: nimmt Seelen-Lava, **zerbricht beim Ausgießen** von Seelen-Lava.
- Enderit-Eimer: Schmiedetisch direkt vom Eisen-Eimer, zerbricht nie.
- **Brennstoff (neu, „schmilzt 10× mehr als Lava“)**: ★ Seelen-Lava-Eimer brennt im Ofen **10× so lange** wie ein
  Lavaeimer (200 000 statt 20 000 Ticks = 1000 statt 100 Items). Rest: Enderit-Eimer kommt leer zurück, Eisen-Eimer
  zerbricht (wie beim Ausgießen). Brennzeiten > 32 767 brauchen die vorhandenen `AbstractFurnace*Mixin`-Pfade. Andere
  Deutungen → Frage 57; Frage 36 dadurch angepasst.
- Details (Rezept, Inhalte, Extras) → Fragen 32–35.

## 11. Dorf-Tiegel (neu, Bibliothek)

- ★ eigenes Kleinst-Stück **„Feldküche“** (ca. 3×3): Eisen-Tiegel auf einem **ausgemachten Lagerfeuer**
  (`lit=false`), dazu Fass (Loot: rohes Fleisch/Kartoffeln/Brot) und Sitzgelegenheit. Je Dorftyp eine Material-
  Variante (Ebene Eiche, Wüste Sandstein, Savanne Akazie, Taiga Fichte, Verschneit Fichte mit Schnee),
  Gewicht so, dass etwa **jedes dritte Dorf** eine hat.
- Einbau: beim Serverstart dem Pool `minecraft:village/<typ>/houses` ein Element mit kleinem Gewicht anhängen
  (Accessor auf `StructureTemplatePool`, alle Loader gleich); Config-Schalter + Gewicht mit harter Grenze.
- Alternativen und Inhalt → Runde 2.

## 12. Warmes Essen (Bibliothek)

- Aufwärmbar: Tag `simplelib:warmable_food` (Sandwiches als `required:false`). Ab Hitze mittel (★A, §6), ganzer Stapel,
  Grundzeit 100 Ticks × Hitzefaktor/Stufentempo. Im Tiegel gegarte Speisen kommen warm heraus.
- Esszeit −15 %; warm 12 000 Ticks (halber Tag-Nacht-Zyklus), im **Bündel 48 000** (zwei Zyklen).
- Komponente `simplelib:warm` = (`warmUntil` Weltzeit, Bündel-Restzeit); kühlt auch in entladenen Chunks ab.
- **Stapeln = Mittelwert** aller Items (Warm-Komponente aus der Stapelgleichheit ausgeblendet, Merge-Hook).
- **Glow** um das Item, Stärke ~ Restwärme (Item-Modell `range_dispatch` für eigene Items, Render-Mixin für fremde).

## 13. Server-Config (harte Grenzen)

| Schlüssel | Standard | Grenzen |
|---|---|---|
| `crucible.reinforcedSpeed` / `netheriteSpeed` / `enderiteSpeed` | 2 / 4 / 8 | 1..`MAX_MACHINE_SPEED` |
| `heat.factorMedium` / `High` / `Extreme` (★A) | 0,5 / 0,75 / 1,0 | 0,1..1,0 |
| `heat.twoBelowPenalty` | 0,10 | 0..0,5 |
| `heat.afterglowSeconds` (je Stufe) | 2 / 4 / 8 / 16 | 0..60 |
| `heat.netherBonus` (Deckel hoch) | 1 | 0..1 |
| `heat.recheckTicks` | 20 | 5..200 |
| `warm.baseTicks` | 100 | 20..1200 |
| `warm.durationTicks` / `bundleDurationTicks` | 12 000 / 48 000 | 1200..48 000 / 1200..96 000 |
| `warm.eatSpeedBonus` | 0,15 | 0..0,30 |
| `village.enabled` / `village.weight` | an / 2 | an/aus / 0..10 |
| SB `soulLava.springChance` / `fortressChance` | 0,005 / 0,10 | 0..0,05 / 0..0,5 |
| `barrel.enabled` | an | an/aus |
| SB `soulLava.flowOverworld` / `flowNether` | 2 / 5 | 1..4 / 1..7 (Neustart) |
| SB `soulLava.burnSecondsMultiplier` | 2,0 | 1..4 |
| SB `soulLava.soulBurnSeconds` / `soulBurnChance` / `soulBurnIntervalTicks` | 60 / 0,5 / 60 | 5..300 / 0..1 / 20..200 |
| SB `soulLava.igniteAttemptsMultiplier` / `igniteRangeMultiplier` | 4 / 2 | 1..8 / 1..3 |
| SB `soulLava.fuelMultiplier` | 10 | 1..10 |

## 14. Tests

- Bibliothek (Standalone-Target nach Prinzip 8): `crucible_tier_slots_6_9_18`, `crucible_parallel_same_item_all_stacks`,
  `crucible_result_prefers_slot_below`, `crucible_reserved_slot_ghost_and_block_pauses`, `crucible_result_not_recooked`,
  `crucible_recipe_type_gate_by_heat`, `crucible_heat_factor_times`, `crucible_heat_two_below_ten_percent_slower`,
  `crucible_flowing_lava_one_level_lower_cauldron_equal`, `crucible_afterglow_per_tier`, `crucible_nether_plus_one`,
  `crucible_foreign_heat_capped_high`, `crucible_hopper_bottom_results_only`, `crucible_axe_build_six_strikes`,
  `crucible_axe_upgrade_without_sb`, `crucible_ber_contents_sync`, `barrel_attach_six_strikes`,
  `barrel_results_first_then_own_slots`, `barrel_rejects_insertion`, `barrel_hopper_below_extracts`,
  `warm_*` (Bonus, Ablauf, Bündel, Mittelwert, Glow-Stufe), `village_field_kitchen_in_pool`.
- SB: `enderite_crucible_27_double_stack`, `crucible_sledgehammer_upgrade_double_cost`,
  `crucible_sledgehammer_build_conditional`, `enderite_scrap_needs_extreme_heat`, `soul_lava_*` (Fließweite 2/5,
  Kreativ ersetzt, Wasser → Schwarzstein, Brand ×2, Seelenbrand, Zündung, Worldgen offen+verborgen), `*_bucket_*`,
  `soul_lava_bucket_fuel_ten_times`, `reinforced_cauldron_holds_soul_lava`, `enderite_barrel_double_stack`.
- Sandwiches: `sandwich_warmable_via_library_tag`, Start ohne SB.
- Gates: `fabric-263`, `neoforge-263`, Modul-Targets, Standalone-Target, 26.2-/Forge-Compile, Wiki, `checkBalance`.

## 15. Phasen und Aufwand

| Phase | Inhalt | Aufwand |
|---|---|---|
| P0 | Prinzipien-Ergänzung abstimmen, Modul `simplelib` anlegen, Bündeln in SB/Sandwiches (3 Loader) | 1 |
| P1 | Eisen-Tiegel, Axt-Bau, BE/Menü/Screen, Reservierung, Indikatoren, Rezept-Gate, Hitze | 2 |
| P2 | Verstärkt/Netherit (Axt-Weg), Trichter/Komparator, Jade/JEI, BER | 1,5 |
| P2b | Kupfer-/Verstärkt-Fass, Anbringen, gemeinsame GUI | 1 |
| P3 | Warm-Food (Komponente, Esszeit, Bündel, Mittelwert, Glow) | 1,5 |
| P4 | Dorf-Feldküche (5 Varianten, Pool-Einhängen) | 0,75 |
| P5 | SB: Enderit-Tiegel/-Fass, Vorschlaghammer-Wege, Seelen-Lava (Worldgen, Seelenbrand, Zündung), verstärkter Kessel, Eimer | 3 |
| P6 | Texturen/Strukturen (Vorschau A/B/C), NeoForge/Forge, Wiki/Handbuch, Gates | 1,5 |

Summe ≈ 12–12,5 Sitzungen. GPT-geeignet: P0-Gerüst, Lang/Wiki, Tag-Listen, Eimer-Varianten, Struktur-NBT-Varianten.

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
- BER mit bis zu 27 (+9) Item-Modellen je Tiegel: Render-Last in Basen mit vielen Tiegeln → Sichtweite/Anzahl begrenzen.
- Fass-GUI neben dem 27er-Raster sprengt evtl. die Fensterbreite bei GUI-Skala 4 (Truhen-Lösung wiederverwenden).
- `simplelib` als allgemeine Bibliothek wächst leicht zur „Core-Jar“ → nur wirklich geteilte Inhalte aufnehmen.
- Seelenbrand + 4× Zündung: Waldbrände im Nether-Rand/Overworld; Fließblöcke unersetzbar → Grief-Potenzial, Claims Pflicht.

## 17. Fragebogen (abgeschlossen – Antworten in §2, §2b, §2c; Text bleibt zur Nachvollziehbarkeit)

Fragen **1–31 sind beantwortet** (Tabelle §2b, eingearbeitet). Offen sind **32–49** (Nummern unverändert, damit die
Antworten zuordenbar bleiben) und die neuen Fragen **50–61** aus den Runde-2-Antworten und -Wünschen.

### F. Eimer (SB)
32. **Kupfer-Eimer: Rezept, Inhalte, Oxidation?** ★A 3 Kupferbarren V-Form; alles wie Eisen-Eimer außer Seelen-Lava;
    keine Oxidation · B nur Wasser/Lava · C oxidiert, oxidiert keine Lava.
33. **Kupfer-Eimer beim Lava-Ausgießen?** ★A zerbricht immer (Welt, Kessel, Dispenser), kein Rest · B 1 Kupfernugget
    bleibt · C 4 Ladungen Haltbarkeit.
34. **Eisen-Eimer bei Seelen-Lava?** ★A Aufnehmen geht, zerbricht beim Ausgießen · B zerbricht schon beim Aufnehmen ·
    C 50 % beim Ausgießen.
35. **Enderit-Eimer?** ★A Schmiedetisch Enderit-Vorlage + Eisen-Eimer + Enderit-Barren; unzerbrechlich, als Item
    feuerfest · B über Netherit-Eimer · C zusätzlich 4 Ladungen.
36. **Brennstoff Kupfer-Lava-Eimer?** (Seelen-Lava-Eimer = 10× Lava, siehe neuer Wunsch / Frage 57) ★A brennt wie
    Lava, Eimer wird verbraucht · B kein Brennstoff · C brennt, Kupfer-Eimer kommt zurück.

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

### J. Neu aus Runde 2
50. **Axt-Aufwertung ohne SB – Material (Prinzip Frage 6)?** ★A Eisen → Verstärkt 2 Diamanten, Verstärkt → Netherit
    1 Netheritbarren (je Nebenhand, 10 Axt-Schläge) · B Verstärkt 4 Diamanten, Netherit 2 Netheritbarren ·
    C Verstärkt 1 Diamantblock, Netherit 1 Netheritbarren + Netherit-Vorlage.
51. **Hitzestufen (Rückfrage zu 13/14, Tabelle §6)?** ★A drei Stufen: mittel (Lagerfeuer, Seelen-Lagerfeuer, Magma,
    fließende Lava; 0,5×; Aufwärmen, Lagerfeuer-, Räucherofen-Rezepte) · hoch = Ofen-Niveau (Lava, Lavakessel,
    fließende Seelen-Lava; 0,75×; + Ofen, Schmelzofen) · extrem (Seelen-Lava; 1×; + Enderit-Schrott, Antiker Schrott,
    Rissiger Diamant); Fackel/Kerze geben keine Hitze · B vier Stufen mit „niedrig“ (Kerze/Fackel/Feuer, 0,5×, nur
    Aufwärmen + Lagerfeuer-Rezepte; mittel 0,625×) · C wie A, aber Räucherofen-Rezepte erst ab hoch.
52. **Sonderliste „extrem“ (ehem. 14)?** ★A Enderit-Schrott, Antiker Schrott, Rissiger Diamant · B nur Enderit-Schrott ·
    C zusätzlich alle Rezepte mit Netherit-/Enderit-Ergebnis.
53. **BER: wie viele Items sichtbar?** ★A je belegtem Slot ein Modell, bis 27 (+ Fass nicht), Sichtweite 16 Blöcke
    (Client-Option) · B höchstens 9 (oberste Lage) · C alle inkl. Fass-Inhalt am Fass.
54. **Wasser an Seelen-Lava – Wasserseite wird zu?** ★A Schwarzstein · B Basalt · C Weinender Obsidian (Quelle) /
    Schwarzstein (fließend).
55. **„Feuerschutz“ beim Seelenbrand = ?** ★A Verzauberung Feuerschutz (je Stufe −25 % Chance, IV = kein Schaden) und
    Trank der Feuerresistenz (ganz) · B nur der Trank · C nur die Verzauberung (Summe über Rüstung).
56. **Verstärkter Kessel – Rezept/Besitz?** ★A SB: Kessel + 4 Rissige Diamanten (Werkbank, Kreuz) · B Vorschlaghammer-
    Aufwertung des Kessels mit 2 Rissigen Diamanten · C in `simplelib` mit 4 Diamanten (auch ohne SB, dann ohne
    Seelen-Lava sinnlos).
57. **„Schmilzt 10× mehr als Lava“ – Deutung?** ★A Brennstoff: Seelen-Lava-Eimer brennt 10× Lava (200 000 Ticks);
    Eisen-Eimer zerbricht dabei, Enderit-Eimer kommt zurück · B im Tiegel: extreme Hitze gart zusätzlich 10× schneller
    (Faktor 10 statt 1) · C Items/Blöcke verbrennen darin 10× schneller (Kontakt-Zerstörung) · D A und C.
58. **Fass auch allein nutzbar?** ★A ja, normales 9er-Fass (öffnen, Trichter) · B nur am Tiegel (allein ohne GUI).
59. **Unterschied der Fass-Stufen (immer 9 Slots)?** ★A Stapelfaktor Kupfer 1×, Verstärkt 1×, Enderit 2× + Härte
    wie Tiegel; Aufwertung wie Tiegel (2 Material) · B Verstärkt 2×, Enderit 4× · C Stufen nur optisch/explosionsfest.
60. **Anbringen/Lösen und Anzahl?** ★A Fass seitlich neben den Tiegel, 6 Schläge aufs Fass; Lösen durch Abbauen;
    höchstens ein Fass je Tiegel · B bis zu vier Fässer (je Seite eins, nacheinander gefüllt) · C Fass oben auf dem
    Tiegel statt seitlich.
61. **Wer darf ins Fass legen?** ★A nur der Tiegel (Spieler/Trichter nur entnehmen) · B auch Spieler, aber nicht
    Trichter · C alle (dann blockieren Fremditems Ergebnisse).

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
- Runde-2-Fragen 1–31 beantwortet (§2b); 13/14 als Frage 51/52 neu gestellt.

## 18. Arbeitsannahmen (ohne eigene Frage)

Tiegel-Slots teilen Eingabe und Ausgabe; Reservierungen und Ergebnis-Markierungen als BE-Bitmasken; Lesereihenfolge
Gitter für Gitter; Hitzefaktor „mittel“ 0,5× (★A ohne „niedrig“, sonst 0,625×); Tiegel-Licht 13 + Ofen-Partikel wenn aktiv; Seelen-Lava nicht im
Tag `minecraft:lava`; Eimer-Varianten als eigene Items; Warm-Komponente nur serverseitig gesetzt; kein Tiegel im
1.21.11-Zweig; Simple-Money-Preise nach der Besitzerwahl.
