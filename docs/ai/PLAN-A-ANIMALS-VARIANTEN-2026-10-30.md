# Plan A31: Varianten und Optik von Simple Animals / Simple Dinosaurs (a-animals31, 30.10.2026)

Nur MC 26.3 Fabric. Besitzer-Wahl ABN30-1 (Dinos: Farben je Aufzucht-Biom, realitaetsnah, etwas Variation) und ABN30-2 (Hirsch beide Varianten, Wildschwein dunkel + Biom-Varianten ohne Geschlechter, Igel realistisch braun mit mehreren Arten, Krebs rot, Zebra klassisch gestreift).

## 1. Ist-Zustand
- Beide Module haben je Tier ein `VARIANT`-Int (synchronisiert, NBT `Variant`), Zufall 1:3 beim Spawn, Renderer mit festem Array. Texturen kommen aus `modules/*/tools/gen_models.py` (Wuerfel-Spec, Ebenen per `layers.save`).
- Vanilla 26.3 (Kuh/Schwein): Varianten sind datengetriebene Registry-Eintraege (`CowVariant`, `SpawnPrioritySelectors`, `BiomeCheck`) mit Biom-Tags `spawns_cold/warm_variant_farm_animals`. Ein eigenes dynamisches Register braeuchte loaderspezifische API und liesse sich ohne Compile-Rueckmeldung nicht absichern. Entscheidung: gleiche Idee (Biom-Tag/Biom-Schluessel -> Variante, Prioritaet/Gewicht), aber als kleiner, reiner Katalog je Modul (`AnimalVariants`, `DinoVariants`; Module bleiben allein spielbar, keine gemeinsame Abhaengigkeit). Die Vanilla-Tags `SPAWNS_COLD/WARM_VARIANT_FARM_ANIMALS` werden mitgenutzt.
- Gespeicherte Welten: Indizes 0/1 behalten ihre Bedeutung (Hirsch 0 rot/1 Dam, Wildschwein 0 dunkel/1 hell, Igel 0 braun/1 grau->Wuestenigel, Krebs 0 rot/1 Sand->Korallenrot).

## 2. A12
Nutzen: Vielfalt der Optik, kein Spielwert. Aenderung: nur Aussehen (Fellfarbe nach Biom). Eingriff: keiner (Vanilla unberuehrt, keine Werte, keine Geschlechter).

## 3. Mechanik
`pick(katalog, biomHolder, random)`: jede Variante hat ein `home`-Praedikat (Biom-Tags/-Schluessel), Gewicht 8 in der Heimat, sonst 1 (etwas Variation). Standard-Variante = `rest` (alles, was keine andere Heimat trifft). `clamp(katalog, i)` fuer gespeicherte/ungueltige Werte. Spawn: `finalizeSpawn` waehlt nach Biom am Spawnort; Zucht erbt zufaellig von einem Elternteil; Dinos: Biom des Eis beim Schluepfen (Aufzucht-Biom), Befehl/Spawn: Biom am Ort.
- Hirsch: 0 Rotwild (Heimat Taiga/Berge/Huegel/kalt), 1 Damwild (Rest, jetzt mit hellen Flecken).
- Wildschwein: 0 dunkel (Standard/Rest), 1 sandbraun (warm), 2 frostgrau (kalt/Taiga/Berge), 3 rostbraun (Sumpf/Dschungel/Mangroven). Keine Geschlechter. Frischling bleibt gestreift (eine Textur).
- Igel: 0 braun (Rest, Europaeischer Igel), 1 Wuestenigel (Wueste/Badlands/Savanne), 2 Weissbrustigel (kalt/Taiga/Berge: dunkler, helles Gesicht und Bauch), 3 Weissbauchigel (Dschungel/Sumpf: cremefarbene Stacheln).
- Krebs: 0 rot (Rest), 1 korallenrot (Strand/Ozean/Fluss/warm).
- Zebra: klassisch schwarz-weiss, harte Streifen, Rest weich. Zorse unveraendert.
- Dinos (Aufzucht-Biom): olive (Rest), sand (Wueste/Savanne), rust (Badlands), moss (Dschungel/Sumpf/Mangroven), ash (kalt/Berge/Taiga). Erdige Toene mit Gegenschattierung (dunkler Ruecken, heller Bauch), nach Fossilien-/Rekonstruktionsdarstellungen plausibel (Grau-Braun-Oliv, Sand/Gelb in Trockenraeumen). T-Rex: 5 Felle x 4 Ruestungsstufen. `Dino` bekommt das Varianten-Feld (NBT `CoatVariant`), also gilt es fuer alle kuenftigen Dinos.

## 4. Texturen (A8b)
Alle Texturen neu durch `gen_models.py`: weichere Rauschwerte, Gegenschattierung, danach auf 10-14 Toene quantisiert (`finish`), harte Uebergaenge gemessen und im Lauf ausgewiesen (Ziel ~7 %, Zebra bewusst hart). Vorschau je Variante drehbar mit Anteil harter Uebergaenge, Haken je Variante, Antwortcode `ANIMALS31`. Nicht ausgeliefert ohne Wahl der Vorschau: die Standard-Fassung steckt im Code (Empfehlung), Aenderung nach Wahl nur Palette.

## 5. Dateien
`modules/simpleanimals/**` (AnimalVariants, 4 Entitaeten, Renderer, gen_models, preview, AnimalsTests, ModuleGameTest, Lang/Guide-Text), `modules/simpledinos/**` (DinoVariants, Dino, Trex, DinoEggBlockEntity, TrexRenderer, gen_models, preview, DinosTests, ModuleGameTest), `.claude/QUEUE.md`, Memory.

## 6. Verifikation
Billig lokal: `gen_models.py --check`, Vorschau erzeugen. Ein sbt-Lauf: Compile `:mc26_3:fabric` + `:modules:simpleanimals:fabric` + `:modules:simpledinos:fabric`, Targets `module-simpleanimals-fabric-263`, `module-simpledinos-fabric-263`, beide `-standalone-`, dann `./gradlew check -q`. Gametests (Katalog + Fabric-Adapter): Animals `variant_catalog`, `variant_entities`; Dinos `dino_variant_catalog`, `dino_hatch_variant`.

## 7. Nicht getestet / Rest
Keine Client-Laeufe: echte Optik im Spiel, Forge/NeoForge nicht kompiliert. Naechste Dinos (Triceratops usw.) brauchen nur ihre eigene Palette je Fell.
