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
