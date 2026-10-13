# Plan: Simple End (Sub-Mod, End-Holz, End-Gras, Biome) – 2026-10-12

Branch `claude-simple-end` (Basis `claude-wave1` 0d9d0f121). Grundlage: `KONZEPT-ENDHOLZ-BIOM-2026-10-12.md`
(Abschnitt „Besitzer-Antworten 12.10.“ verbindlich), `PLAN-MODULAUFTEILUNG-2026-10-11.md` §2.3 (Bündelung),
`PRINZIPIEN-MODUL-UNABHAENGIGKEIT.md`. Nur 26.3, alle drei Loader.

## Ist-Zustand
- `simpleenddragonfight` liegt als optionale Sub-Mod direkt unter SB (`modules.json parent=simplebuilding`, SB-Config
  `enableSimpleEndDragonFight`, `include`/`jarJar`/`forgeBundleModules` in `mc26_3/*/build.gradle`).
- Kein End-Holz, kein End-Gras, keine eigenen Biome im Repo. SB-Holzlisten (`OCTET_WOODS`, Woodwork-Generator,
  `BoatWoods`) sind fest auf Vanilla-Hölzer (Namensraum `minecraft`).
- Texturvorschläge Runde 1 (`tools/textures/n44_proposals_2026_10_12.py`, Branch `claude-n44-tex`): Set B1 = Aschholz,
  B3 = Dämmerholz.
- 26.3-Eigenheiten (aus dem Spiel-Jar nachgelesen): Schilder sind Blockmodelle (`template_sign_rot_*`, Textur
  `block/<holz>_sign.png` 32×32), Schild-GUI `minecraft:textures/gui/signs/<WoodType-Name>.png`; Boote je Holz eigener
  `EntityType`; `WoodType.register` privat; Biom-Optik über `attributes` (`minecraft:visual/sky_color` …);
  `TheEndBiomeSource` fest auf 5 Biome.

## Entscheidungen (selbst getroffen, im Bericht genannt)
1. **Modul** `simpleend` (Paket `com.simplebuilding.modules.simpleend`), Super-Mod-Ebene 2: SB bündelt `simpleend`
   (optional, Schalter `enableSimpleEnd`), `simpleend` bündelt `simpleenddragonfight` (optional, Schalter
   `enableSimpleEndDragonFight` in `config/simpleend.json`). SB-Config verliert `enableSimpleEndDragonFight`
   (ersetzt durch `enableSimpleEnd`). Verschachtelter Schalter: Simple End setzt beim Serverstart
   `SubMods.set(dragonfight, eigenerSchalter && SubMods.enabled(simpleend))` – kein neues framework-API.
2. **Biome** über einen gemeinsamen Mixin auf `TheEndBiomeSource` (alle Loader gleich): auf äußeren Inseln ersetzt er in
   Zellen von 12×12 Chunks mit ~40 % Chance einen runden Fleck (Durchmesser 3–6 Chunks) aus Highlands/Midlands durch
   Ash Valley oder Dawnlands; Zufall aus dem welt-abhängigen Erosionsrauschen. `collectPossibleBiomes` wird ergänzt
   (Holder über den Registry-Owner der Vanilla-Holder), damit Features sortiert werden. „Ersetzt Teile der Highlands“ ist
   technisch besser als neue Inseln (kein eigener Dichte-Generator).
3. **Tint** grau-braun über `dry_foliage_color` der eigenen Biome (Blätter, End-Gras, End-Busch); außerhalb gilt der
   Vanilla-Trockenlaub-Ton (bräunlich), nie Grün. Registrierung der Tint-Quellen per Client-Mixin auf
   `BlockColors.createDefault` (alle Loader gleich).
4. **Holzset je Holz** (`ashwood`, `duskwood`): log, stripped_log, wood, stripped_wood, planks, stairs, slab, fence,
   fence_gate, door, trapdoor, button, pressure_plate, sign/wall_sign, hanging_sign/wall_hanging_sign, leaves, boat,
   chest_boat, Knospe (`ash_bud`, `dusk_bud`) + Topf, Stöcke-Block (`ashwood_sticks`, `duskwood_sticks`).
   Gemeinsam: `end_grass`, `end_bush`, Frucht `drift_fruit` (Schwebfrucht: 4 s Slow Falling + kleiner Sprung).
   Entrinden über `AxeItem.STRIPPABLES` (Accessor), Brennbarkeit über `FireBlock` (Invoker), Brennstoff über Vanilla-Tags,
   Schild-Blockentities über `BlockEntityType#isValid`-Mixin, Boote: eigene `EntityType`s + kleiner Renderer auf dem
   Vanilla-Bootmodell (kein eigener Model-Layer).
5. **Knospe**: Zufallstick wächst nur bei Licht ≥ 9 (dann großer Baum); Knochenmehl: Licht ≥ 9 → großer Baum, sonst
   Stöcke-Gestrüpp. 2×2 gleiche Knospen + Licht → Riesenbaum (2×2-Stamm). Platzierbar auf Endstein, End-Gras-Boden
   (Tag `simpleend:bud_may_place_on`, enthält Endstein + `#minecraft:dirt`). Bäume als eigener Java-Code
   (`EndTrees`), den auch die Weltgen-Features nutzen.
6. **Gedroppte Items** aller Simple-End-Holz-Items (Item-Tag `simpleend:void_safe`): Alter beim ersten Tick −6000
   (doppelte Despawn-Zeit); unter `minY` schweben sie zurück auf `minY + 1` und bleiben dort (kein Schwerkraft).
7. **Musik**: eigene Sound-Events `simpleend:music.ash_valley`/`music.dawnlands` auf ruhige Vanilla-Musikdateien
   (keine eigene Komposition möglich); Partikel: Vanilla `white_ash` bzw. `spore_blossom_air`-artig sparsam.
8. **Stufe 7 (Achtel/geschnitztes Holz/Holzkessel)**: SB-Generatoren arbeiten mit festen Vanilla-Listen im Namensraum
   `minecraft`; eine Erweiterung braucht SB-Code für fremde Namensräume + bedingte Registrierung. Wird zuletzt
   umgesetzt, falls Zeit bleibt; sonst als offener Punkt in Memory/Bericht.

## Stufen (je Commit, compile 3 Loader + Tests grün)
1. Modul-Gerüst (`tools/newmod.py simpleend`), Verschachtelung (Gradle 3 Loader, Metadaten, `modules.json`
   parent/bundle, SB-Config, `enabled-mods.json`), Zentrale-Baum rekursiv (`modselect.py`, `index.html`),
   `docs/MODS-UND-FEATURES.md`. Tests: `simpleend:submod_switch`, SB `ConfigOptionTests` angepasst.
2. End-Gras/-Busch (Blöcke, Tint, Loot, Modelle, Texturen).
3. Holzsets (Registrierung, Daten per Generator `modules/simpleend/tools/gen_data.py`, Texturen per
   `modules/simpleend/tools/gen_textures.py` aus B1/B3), Item-Rettung.
4. Knospen, Bäume, Stöcke-Gestrüpp. 5. Blätter-Drops + Frucht.
6. Biome (Mixin, Features, Biom-JSON, Musik/Partikel, Tags `is_end`/`has_structure/end_city`).
7. siehe Entscheidung 8. 8. Lang EN/DE, Wiki (`modules/simpleend/wiki/manual.json`), Guide, Fragentisch, Money-Preise,
   Testzentrale-Station, Gametests, Standalone-Targets.

## Tests (Gametests `simpleend:*`)
Holzset vollständig (Registry + Rezepte + Tags + Entrinden + Brennbarkeit), Knospe hell → Baum / dunkel → Gestrüpp,
2×2 → Riesenbaum, Zufallstick ohne Licht wächst nicht, Blätter-Loot, Frucht-Effekt, Item-Rettung + Despawn-Alter,
Biome registriert + in `possibleBiomes` + Fleck-Berechnung deterministisch, Schalter (Sub-Mod aus → keine Funktion).
Läufe nur über `/root/sbt`: Compile 3 Loader, `module-simpleend-*-263`, Standalone, SB-Filter `ConfigOption*`.

## Risiken
Biom-Mixin neben Fabric-API-Mixin auf derselben Klasse (beide `RETURN`, verträglich); Feature-Sortierung (gleiche
Reihenfolge in beiden Biomen und den Vanilla-End-Biomen); NeoForge/Forge-Patches an `AxeItem`/Schild-Screen.

## Stufe 9 (Besitzer-Nachtrag 12.10.): Knabbermilbe
- `simpleend:nibblemite` (EN Nibblemite, DE Knabbermilbe), `Animal`, Kategorie CREATURE, kein `Endermite` (keine
  Enderman-Aggro), kein Angriff. Varianten 0/1 nach Spawn-Biom (Aschental blassgrau/Lavendel, Dämmerlande taupe/Rosa),
  Junge erben. Spawn: Biom-Attribut `natural_mob_spawns` creature, Gruppen 2-4, nur auf Endstein (`checkSpawnRules`).
- Knabbern (Goal wie Schaf-Grasen, ~1/600 je Tick): Endstein darunter → `burrowed_end_stone` (Oberseite eigene Textur,
  Seiten Vanilla-Endstein), erholt sich per Zufallstick (1/4 → ~4-5 min). Futter/Zucht: Chorusfrucht; Drop 0-1 Chorusfrucht.
- Tests: Knabbern → Block, Erholung, passiv (kein Enemy/Endermite, CREATURE, Chorus-Futter).

## Umbau 13.10. (Besitzer N50, Branch `claude-endbiome2`)
Besitzer-Vorgaben (N50 + Ergänzung 13.10.): Biome seltener, viel größer, mehr Gras; dichter, unheimlicher Nebel; Kern- und
Randbiom (Kern = Hollow-Watcher-Bereich, Spawn macht Simple Mobs bedingt über einen Tag); im End nur Stöcke-Bäume und
Schwebfrucht, Holz nur aus der Oberwelt; Oberwelt-Bäume gigantisch (2x2 größer, 4x4 = größter Baum im Spiel); Knospen-
Rückgabe ~0,8 je Oberwelt-Baum, ~1,5 je Stöcke-Baum; Früchte gedeihen nur im End, sonst „Spoiled Drift Fruit“ mit Treiben-
Effekt; Blätter durchsichtig/exotisch, kein Vollblock.

Entscheidungen (selbst getroffen):
1. **Biom-Flecken**: Zellen 40x40 Chunks, Standard-Chance 0,3 (Config `biomeChance`, gleiche Taste), Radius 7-12 Chunks
   (Durchmesser 14-24 Chunks statt 3-6) mit welligem Rand (zwei Sinus-Harmonische), auf Quart-Genauigkeit (4 Blöcke) statt
   ganzer Chunks. Ersetzt Highlands/Midlands/Barrens innerhalb des Flecks (kleine Inseln/Void bleiben Vanilla). Innere
   62 % des Radius = **Kern** (`ash_valley`, `dawnlands`), außen = **Saum** (`ash_valley_fringe`/„Aschental-Saum“,
   `dawnlands_fringe`/„Dämmerlande-Saum“). Fläche je Fleck ~17x größer, Flecken ~11x seltener (Abdeckung ~5 %).
2. **Nebel** über Biom-Attribute `visual/fog_start_distance`/`fog_end_distance`: Kern 0-20 Blöcke (dicht), Saum 6-56
   (Übergang); Nebelfarben dunkler. Biom-Tag `simpleend:watcher_core` = nur die beiden Kernbiome (für Simple Mobs);
   `is_end`/`has_structure/end_city` enthalten alle vier.
3. **Weltgen im End**: keine Holzbäume mehr. Kern: Stöcke-Bäume (Größe „groß“ je Chunk 1, „riesig“ 1/3), Gestrüpp,
   dichte Vegetation (10-14 Büschel x 40 Versuche, vorher 3-5 x 24), Chorus 1/4. Saum: Stöcke-Bäume 1/2, Gestrüpp 1-2,
   Vegetation 6-9, Chorus 1/3. Weltgen-Bäume bleiben innerhalb ±16 Blöcke (Schreibbereich des Weltgen-Bereichs).
4. **Baumformen** (`EndTrees`, ein Code für Holz und Stöcke): Stamm + 5-16 schräg aufsteigende Äste (6er-verbunden) mit
   Laubkugeln + Kronenkugel + Wurzeln. Groß (1 Knospe) Stamm 15-19, Riesig (2x2) Stamm 23-28, Kolossal (4x4 Knospen) Stamm
   38-46 mit 4x4 bis 60 %, dann 2x2, Äste bis 10 weit, Krone bis ~58 hoch und ~34 breit (größter Baum im Spiel). Stamm
   muss frei sein, Äste werden an Hindernissen gekappt; nur geladene Chunks werden beschrieben.
5. **Knospe**: im End wächst immer ein Stöcke-Baum (gleiche Größenregel 1/2x2/4x4); außerhalb: Licht ≥ 9 Holzbaum,
   darunter weiter Stöcke-Gestrüpp. Selbstwachstum weiter nur bei Licht.
6. **End-Stöcke** bekommen `axis` (Äste liegen waagrecht) und `natural`. Baum-Stöcke (`natural=true`) fallen wie Chorus:
   wird ein Stück entfernt, bricht jeder angrenzende Teil, der über Stöcke keinen festen Boden mehr erreicht (Suche
   begrenzt auf 8192 Blöcke, Drops zusammengefasst zu wenigen Item-Stapeln, Partikel für max. 48 Blöcke, kein
   rekursives Auslösen). Gesetzte Stöcke (`natural=false`) fallen nie. Stöcke stehen in
   `#minecraft:prevents_nearby_leaf_decay`: Laub an Stöcke-Bäumen hält, nach der Kaskade zerfällt es normal (Vanilla).
7. **Rückgabe statt Zufall je Blatt**: Laub hat `drop` (0 nichts, 1 Knospe, 2 Frucht). Beim Wachsen werden unter den
   gesetzten Blättern ⌊m⌋ bzw. ⌊m⌋+1 Blätter markiert (Mittel genau m): Knospen m = 0,8 (Holzbaum), 1,5 (Stöcke-Baum);
   Früchte 1,0 bzw. 2,0. Markierte Blätter droppen ihr Item immer (auch mit Schere/Behutsamkeit; Zerfall genauso).
   Frucht: `location_check` Dimension End → Schwebfrucht, sonst Verdorbene Schwebfrucht. Gesetztes Laub hat `drop=0`.
   Stöcke-Chance (Vanilla wie Eiche) bleibt.
8. **Verdorbene Schwebfrucht** (`spoiled_drift_fruit`, „Spoiled Drift Fruit“/„Verdorbene Schwebfrucht“): 1 Hunger,
   gibt 5 s **Treiben** (`simpleend:drifting`: Schwerkraft −90 %, Bewegung −40 %, alle 10 Ticks ein zufälliger Stoß,
   Fallhöhe wird zurückgesetzt) + 8 s Sanfter Fall (nie tödlich).
9. **Laub** (`EndLeavesBlock`, Unterklasse von Vanillas Partikel-Laub): eigenes Modell aus vier Ranken-Ebenen (Stern)
   und einem kleinen Kristallkern, Cutout (kein Translucent: billiger bei Riesenkronen), keine Verdeckung, selten ein
   End-Stab-Partikel. Kollision bleibt voll (Kronen begehbar).
10. Performance: Bäume setzen Laub/Äste mit Flag 18 (keine Nachbar-Updates, Distanz selbst berechnet), Stamm mit 19.

Tests (`simpleend:*`): Wachstum je Größe (groß/riesig real, kolossal in der Luft + aufgeräumt), Stöcke-Baum im End
ohne Holz, Kaskade (Baum fällt, Wurzeln bleiben, gesetzte Stöcke bleiben, wenige Item-Stapel), Rückgabe statistisch
(Mittelwert 0,8/1,5 über 20 000 Ziehungen, markierte Blätter am echten Baum), Frucht End/Oberwelt über echte Loot,
Treiben-Effekt, Biom-Kern/Saum/Tag, Weltgen-Feature ohne Holz.
