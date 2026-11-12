# Audit simplesandwiches (a-audsb31e1, 12.11.2026)

Basis `hub/b-bundle188` 9dfb3f643, nur MC 26.3 Fabric (Forge/NeoForge nicht Teil des Releases). Aufbau wie `modules/simplepads/AUDIT.md`.
Umfang: 36 Java-Dateien / 3,2k Zeilen (shared + fabric, ohne Tests; Tests zusaetzlich 3 Dateien / 1,4k Zeilen). Risikogeleitet
(P1-Klassen zuerst: Schneidebrett-Block-Entity, Kessel/Trichter, Buendel-Essen, Komponenten-Stapel). Zusatzeingabe BEW39
(`/root/sb-tools-a/bewertung39/simplesandwiches.json`) wurde gegen den Code geprueft.

## Vollstaendig gelesen
- Schneidebrett: `block/CuttingBoardBlock` (Zustandsautomat), `block/CuttingBoardBlockEntity` (Laden/Speichern/Drops).
- Kessel: `block/MilkCauldronBlock` (Reifung, Trichter-Ernte, Migration, Befuellen/Entnehmen), `mixin/CauldronDispatcherAccessor`,
  `registry/SandwichRegistry` (Kessel-Einhaengen, Hand-Hinweis).
- Kaese/Butter: `block/SliceBlock`, `block/SliceLayerBlock`, `block/MixedSliceLayerBlock`, `item/SliceLayerItem`, `item/KnifeItem`
  (Melone, Kuchen), `mixin/CakeKnifeMixin`, `mixin/CandleCakeKnifeMixin`, generierte Beutetabellen (Butter/Kaese/Teppich/Kessel/Brett).
- Sandwich: `sandwich/SandwichFormula`, `SandwichItem`, `SandwichContents`, `EffectMerger`, `SandwichVisuals`, `registry/ModComponents`,
  `ModItems`, `ModBlocks`, `ModEffects`, `ModBlockEntities`, `ModTags`, Tag `sandwich_ingredients` (generiert).
- Buendel-Essen: `item/BundleEating`, `mixin/BundleEatMixin`; Rutschig: `mixin/SlipperyFrictionMixin`, `registry/ModEffects`.
- Konfiguration/Einstieg: `config/SandwichConfig`, `Sandwiches`, `fabric/SandwichesFabric`, `fabric/SandwichesViewer`, `fabric.mod.json`,
  `simplesandwiches.mixins.json`, `fabric/build.gradle`; Handbuch `wiki/manual.json`.
- Nicht gelesen: `SandwichGuide`, `SandwichDiscover`, `CuttingBoardRenderer` und Client-Einstiege, Lang/Texturen/Klaenge, Forge/NeoForge, Tests (nur Muster).

## C1-C9
| Punkt | Ergebnis |
|---|---|
| C1 Netzwerk | Keine eigenen Payloads. Brett, Kessel, Kaese und Messer laufen ueber Vanilla-Block-/Gegenstandsklicks (Reichweite, Spawnschutz und Claim-Ereignisse prueft der Paket-Handler); das Brett entscheidet nur auf dem Server (`level.isClientSide()` -> nur Vorhersage). S2C: Block-Entity-Update mit hoechstens 5 Zutaten. |
| C2 Server-Autoritaet | Alle Zustandswechsel serverseitig; die Hand-Hinweise (`TransformHints`) rufen nur `CuttingBoardBlock.plan` ohne Nebenwirkung. Client-Klassen nur in `client/` bzw. Client-Einstiegen. |
| C3 Dupes | Kein Dupe gefunden. Brett: Brot/Zutat/Sandwich werden vor dem Zustandswechsel verbraucht (`consume(1)`), Rueckgabe genau eines Stuecks (`give`/`popResource`), beim Abbau gibt `preRemoveSideEffects` den Inhalt genau einmal heraus (Brett selbst per Beutetabelle). Sandwich auf das Brett legen und wieder oeffnen ergibt nur Zutaten gleicher Art (beim Abbau des offenen Bretts geht die Butter verloren, das Brot kommt zurueck - kein Gewinn). Kessel: Eimer-Wechsel 1:1 (`ItemUtils.createFilledResult`), Trichter nimmt das fertige Produkt nur, wenn `HopperBlockEntity.addItem` den ganzen Stapel (1 Stueck) aufnimmt, sonst bleibt es im Kessel; `Protection.allowsAutomation` beachtet. Scheiben: 16 Scheiben = 1 Vollblock (15 Teppichlagen + 1 Scheibe), Beutetabellen zaehlen die Lagen, `copy_state` fuer Teilbloecke; Mischteppich max. 8 Lagen, Abbau gibt je Lage eine Scheibe der richtigen Art. Kuchen: 7 Scheiben je Kuchen = 7 Bisse (je 2 Hunger, gleich). Komponenten-Stapel: Sandwich-Inhalt nur ids (nie Zutaten-Komponenten), gleiche Inhalte = gleiche abgeleitete Komponenten. Buendel: siehe P3-1/P3-2. |
| C4 NBT/Komponenten | Brett: Zutatenliste auf `SandwichContents.MAX` (5) beim Laden gekappt, leere Stapel verworfen, fehlende Schluessel -> Standard; `SandwichContents`-Codec `listOf(0, 5)`, Netz `list(5)`; Kessel nur Blockzustand. |
| C5 Tick-Kosten | Kessel: geplante Ticks (Viertelstufen) + Zufallsticks zum Selbstheilen; Trichter-Versuch nur bei reifem Produkt. `SandwichItem#inventoryTick` rechnet nur bei veralteter Formel neu. Rutschig: `hasEffect` je Lebewesen-Bewegung. Keine Suchen, keine Partikel-Schleifen. |
| C6 Leaks | Einzige statische Tabelle: `BundleEating.STARTED` (`WeakHashMap<LivingEntity, Item>`, synchronisiert). Einen abgebrochenen Mahlzeit-Eintrag raeumt die GC (schwacher Schluessel), siehe P3-2. |
| C7 Sicherheit/Rechte | Kein Befehl ausser der SimpleLib-Config (`/simplesandwiches config`, Stufe 2). Weltaenderungen nur durch Blockklicks (vom Paket-Handler geschuetzt) und den Kessel-Trichter (`Protection.allowsAutomation`). Mixins: `BundleEatMixin` ersetzt das Vanilla-Rechtsklick-Verhalten (P3-1), `CakeKnifeMixin`/`CandleCakeKnifeMixin` nur mit Messer, `SlipperyFrictionMixin` mit `require = 0` (P3-3); alle mit Super-Mod-Schalter. |
| C8 Unabhaengigkeit | Allein spielbar: Standalone nistet SimpleLib und Simple Crucibles (`fabric/build.gradle:6-8`); `simplebuilding`/`simpleend` nur ueber `suggests`/ids. Metadaten-Abweichung P3-5. |
| C9 Doku/Pflege | Keine TODO/FIXME. 5 Kern-Dubletten (`ModBlocks`, `ModEffects`, `ModItems`, `ModSounds`, `ModTags`, Registry-Listen mit Drift). Handbuch: Naehrwert-Formel war falsch (**P2-1, gefixt**). Lang EN=DE (`check_data.py` gruen). Tote Konstante `KNIFE_CUTS_CAKE_DEFAULT` (P3-4). |

## Befunde
| ID | Schwere | Befund (Datei:Zeile) | Stand |
|---|---|---|---|
| simplesandwiches-P2-1 | **P2 (Doku sachlich falsch, BEW39 ROT)** | `wiki/manual.json:12` (EN) und `:22` (DE): "Hunger und Saettigung sind Brot (5 / 6,0) plus alle Zutaten, ohne Formeldeckel". Der Code rechnet seit IG31-28 anders (`SandwichFormula#combine`, SandwichFormula.java:44-51 und :81-85): Brot und Zutaten werden nach Wert absteigend gereiht, der hoechste zaehlt voll, jeder weitere bringt 10 %, 5 %, 2,5 % ... seines Werts; Hunger auf ganze Punkte, Saettigung auf halbe Punkte gerundet und hoechstens 1,2 x Hunger. Eine Summe aller Zutaten wird nie erreicht (z. B. Brot 5 + gebratenes Rindfleisch 8 + Karotte 3 -> 8 + 0,5 + 0,15 = 8,65, gerundet 9). | **gefixt**: beide Absaetze (EN+DE) mit den Werten aus `SandwichFormula` neu geschrieben. **`wiki/generate.py` muss der Hauptagent laufen lassen**: `wiki/data/simplesandwiches.json:27,37` sind die erzeugte Kopie desselben Textes. Keine Gametest-Aenderung noetig (Formel-Tests `hungerSumWithoutCap`, `butterBonus` pruefen den Code). |
| simplesandwiches-P2-2 | **P2 (Absturz bei fehlender Abhaengigkeit)** | `SandwichesFabric.java:20` ruft beim Start `com.simplelib.api.config.ConfigCommands.server(...)` (und Z. 23 `SandwichesViewer.register()` -> `RecipeViewer`), `fabric.mod.json` deklarierte SimpleLib nicht -> ohne installierte SimpleLib `NoClassDefFoundError`-Absturz statt Fabric-Abhaengigkeitsmeldung. Standalone-Tests merken das nicht (SimpleLib liegt dort im Klassenpfad bzw. nistet im Jar). | **gefixt durch Hauptagent** (`fabric.mod.json` `depends simplelib >=0.4.0`); NeoForge/Forge-Metadaten nur Port-TODO. |
| simplesandwiches-P3-1 | P3 (A12) | `BundleEatMixin` (BundleEatMixin.java:27-32) ersetzt das Vanilla-Rechtsklick-Verhalten des Buendels (Inhalt abwerfen), sobald oben essbare Nahrung liegt; Config `bundleEating` Standard an (SandwichConfig.java:23), Entscheidung F10 (04.10.). Starker Eingriff in Vanilla ohne Opt-in (PRINZIPIEN A12). | **Rueckfrage** F2 |
| simplesandwiches-P3-2 | P3 | `BundleEating.finish` (BundleEating.java:74-78): Ein Eintrag in `STARTED` aus einem abgebrochenen Mahlzeit-Versuch bleibt stehen; der naechste **Sofort**-Verzehr (Verbrauchszeit 0, z. B. Kuchenscheibe im Buendel) liest ihn als "begonnenes Item", findet ein anderes Item obenauf und isst nichts (`started != top.getItem()`), entfernt den Eintrag aber - ein Klick geht verloren. Kein Verlust von Items. | Hinweis (Fix: `STARTED.remove` vor dem Sofort-Pfad in `use`) |
| simplesandwiches-P3-3 | P3 | `SlipperyFrictionMixin` (SlipperyFrictionMixin.java:12) mit `require = 0`: findet sich das Ziel (`travelInAir` -> `computeModifiedFriction`) nach einer Versionsaenderung nicht mehr, faellt Rutschig lautlos aus; der Gametest `slipperyFriction` prueft nur die Funktion `ModEffects#friction`, nicht den Mixin. | Hinweis (Gametest ueber echte Bewegung oder `require = 1`) |
| simplesandwiches-P3-4 | P3 | Tote Konstante `KNIFE_CUTS_CAKE_DEFAULT` (SandwichConfig.java:24) ohne Option; veralteter Kommentar "SimpleLib's reinforced cauldron" (MilkCauldronBlock.java:53-55, :120), der Code nutzt `simplecrucibles:reinforced_cauldron` (:62). | Hinweis |
| simplesandwiches-P3-5 | P3 (Metadaten) | `modules/modules.json` fuehrt `simplecrucibles` nicht unter `optional` (BEW39), Standalone nistet es aber (`fabric/build.gradle:8`); `fabric.mod.json` nennt es ebenfalls nicht in `suggests`/`provides`. Bei den Tag-Luecken des Zutaten-Tags (Simple Ores Rosegold-Aepfel, Simple Animals Krabbe/Trueffel, Simple End Driftfrucht, BEW39 "missing") ist nichts gebaut. | Hauptagent (`modules.json`); Tag-Eintraege "nicht gebaut (BEW39 missing)" |
| simplesandwiches-P3-6 | P3 (Besitzer, Balance) | Nutzen des Sandwichs (BEW39 Q): Es bleibt immer unter der Summe seiner Zutaten (siehe P2-1); sein Nutzen sind gebuendelte Effekte und Platz (Stapel 16). | **Rueckfrage** F1 |
| simplesandwiches-P3-7 | P3 | 5 Kern-Dubletten (`tools/dup_pairs.txt`: `ModBlocks`, `ModEffects`, `ModItems`, `ModSounds`, `ModTags`); keiner der Fixes betrifft ein Paar. | Liste |

## Besitzer-Fragen (fuer die Vorschau AUDSB31E1)
**F1 Nutzen und Wert des Sandwichs (simplesandwiches-P3-6).** Kontext: Nach der Formel IG31-28 ist ein Sandwich weniger wert als die einzelnen Zutaten (hoechster Wert voll, die uebrigen 10/5/2,5 %); der Vorteil sind zusammengefuehrte Effekte, Platz (16 je Stapel) und Butter-Bonus.
- A: lassen, Handbuch/Store-Text nennt den Nutzen offen (Handbuch ist jetzt sachlich).
- B: Bonusanteile anheben (z. B. 25/12,5/6 %), damit sich mehrere Zutaten lohnen.
- C: zusaetzlicher Nutzen statt mehr Naehrwert (z. B. laengere Sattheit/Effekt-Bonus bei voller Schicht).
- Empfehlung: **A** - Balance liegt bei den Besitzer-Entscheidungen IG31-28; ein Nutzen-Satz im Handbuch genuegt, bis der Besitzer eine andere Zahl will.

**F2 Buendel-Essen als Standard (simplesandwiches-P3-1, A12).** Kontext: Rechtsklick auf ein Buendel mit Essen obenauf isst statt den Inhalt abzuwerfen (Vanilla).
- A: Standard an lassen (Entscheidung F10), A12-Satz im Store-Text.
- B: Standard aus (`bundleEating` false), Opt-in.
- C: nur mit Schleichen+Rechtsklick essen, Vanilla-Abwerfen bleibt Standard.
- Empfehlung: **C** - behaelt die Vanilla-Bedeutung des einfachen Rechtsklicks und bleibt trotzdem ohne Config erreichbar (A12 "starke Eingriffe eher nicht"); falls der Besitzer F10 bestaetigt, A.

## Urteil
**shipbar** (0 P1, 2 P2 gefixt - Handbuch-Formel durch mich, `fabric.mod.json` durch den Hauptagent -, 7 P3). Server-Logik des Bretts, Kessels und der Scheiben haelt Dupe-Pruefung stand (1:1 Verbrauch/Rueckgabe, Codec-Grenzen, Selbstheilung).

## Testergebnis
Lokal: keine Java-Datei des Moduls geaendert (kein javac noetig); `modules/simplesandwiches/tools/check_data.py` gruen (nach der `manual.json`-Aenderung), JSON gueltig. sbt: siehe `docs/ai/PLAN-A-AUDSB31E-2026-11-12.md`.
Nicht getestet: Client (Schicht-Modell, Renderer, Klaenge), Mehrspieler-Wettlauf am Brett, echte Claim-Anbieter, Forge/NeoForge. **Folgeschritt:** `wiki/generate.py` laufen lassen (erzeugt `wiki/data/simplesandwiches.json` neu).
