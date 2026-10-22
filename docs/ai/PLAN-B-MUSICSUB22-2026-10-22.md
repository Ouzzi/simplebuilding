# Plan b-musicsub22: Simple Music als Sub-Mod von SimpleBuilding (22.10.)

Queue-Zeilen 110 + 749. Besitzer 21.10.: in Simple Building bündeln, Enable-Schalter, Name bleibt „Simple Music“ (MU21 M).

## Ist-Zustand
- `modules/simplemusic` ist ein eigenes Modul ohne Eltern (`modules.json` ohne `parent`/`bundle`), braucht `simplelib`,
  SB/Mobheads optional (`suggests` / `type="optional"`). Bündelt framework + simplelib selbst → allein spielbar.
- `SimpleMusic.on()` fragt schon `framework SubMods.enabled("simplemusic") && MusicConfig.enabled`; alle Spiel-Hooks
  (Blöcke, Items, Tabs, Jukebox-Mixin, Upload, Presse, Kartentisch) hängen daran. Kein Test schaltet den Super-Schalter.
- SB hatte nie eigene Musik-IDs (Simple Music ist neu, `simplemusic:*` von Anfang an) → keine IdMoves/Aliase.
- Vorbild optionaler Sub-Mod: Simple Wanderers (`d4eefb50e`): Config-Feld `enableSimpleX` (Kategorie modules),
  `applySubModSwitches`, `ConfigOptions.RESTART_REQUIRED`, `ConfigOptionTests` (EXPECTED_OPTIONS + Schalterliste),
  Lang EN/DE (Overlay 26.3), `include`/`jarJar`/`forgeBundleModules`, SB `fabric.mod.json` `suggests`, NeoForge/Forge
  `optional`, `modules.json` SB `optional` + Modul `parent`/`bundle: optional` (`tools/multimod.py validate_bundle`).
- Standalone-NeoForge (`module-simplemusic-standalone-neoforge-263`) existiert in `modules.json`, lief aber nie.

## Änderungen
1. `SimplebuildingConfig`: `enableSimpleMusic = true` + `setSubMod("simplemusic", …)`; `ConfigOptions.RESTART_REQUIRED`.
2. `ConfigOptionTests`: `root.enableSimpleMusic boolean=true` + Paar in `superModSwitchesReachTheBundledSubMods`.
3. Lang `mc26_3/overlay/.../lang/{en_us,de_de}.json`: „Enable Simple Music“ / „Simple Music aktivieren“ + Tooltip.
4. Bündeln: `mc26_3/fabric/build.gradle` include, `mc26_3/neoforge/build.gradle` jarJar, `mc26_3/forge/build.gradle`
   Liste; `src/main/resources/fabric.mod.json` suggests, `neoforge/.../neoforge.mods.toml` + `forge/.../mods.toml` optional.
5. `modules/modules.json`: SB `optional` += simplemusic; simplemusic `parent: simplebuilding`, `bundle: optional`.
6. Modultest `sub_mod_switch` (MusicTests + Fabric-Adapter): Schalter aus → `on()` falsch, Tabs leer, Notenblatt/Orchestrion
   reagieren nicht; danach wieder an.
7. Doku: `docs/MODS-UND-FEATURES.md` (Sub-Mod SB, optional), Modul-Wiki-Satz zum Schalter; Wiki/Guides/Discover neu erzeugen.
8. Standalone-NeoForge laufen lassen, Fehler beheben.

## Risiken
- NeoForge: optionale Sub-Mods nur `jarJar` (keine evaluationDependsOn → sonst zirkulär). Genauso halten.
- Andere B-Agenten (b-pjuke21 schon gemergt) – nur eigene Zeilen in Lang/Queue.

## Verifikation
Compile 3 Loader; run.py module-simplemusic-{fabric,neoforge,standalone-fabric,standalone-neoforge}-263;
SB fabric-263/neoforge-263 `simplebuilding:config_option*` und `simplebuilding:data_integrity*`; integration-263;
`tools/multimod.py`-Prüfung über `check`; Generatoren + `./gradlew check -q` auf sb-test.
