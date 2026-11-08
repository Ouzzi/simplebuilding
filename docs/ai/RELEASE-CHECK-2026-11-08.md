# Release check 2026-11-08 (MC 26.3, Fabric)

Branch `a-rel08` (base `claude-wave1` 440f5a095). Scope: all 59 mods of `modules/modules.json` with loader fabric (`wiringexample` is the example module, not released). Read only plus small fixes; no sbt run (only JSON and lang text changed, no Java or data).

## Blockers before release

1. **Mojang pixels in the repo** (rule `tools/textures`: derived or copied vanilla pixels are never committed). Exact pixel matches against the 26.3 client jar, tracked in resource paths:
   - `src/main/resources/assets/simplebuilding/textures/entity/equipment/humanoid/netherite.png`, `.../humanoid_leggings/netherite.png` (= vanilla netherite armour layers)
   - `src/main/resources/assets/simplebuilding/textures/block/shulker.png` (= vanilla shulker)
   - `mc26_3/overlay/.../textures/block/auto_smither_bottom.png` (= `crafter_bottom`)
   - `modules/simplecrucibles/.../block/iron_crucible_inner.png`, `reinforced_crucible_inner.png` (= `cauldron_inner`)
   - `modules/simpledinos/.../block/egg_overlay_0.png` (= `redstone_dust_overlay`)
   - `modules/simplefun/.../entity/pig/pig.png` (= `pig_temperate`)
   - plus wiki copies of the same files under `wiki/assets/textures/sheets/...`, `wiki/assets/textures/block/netherite_pressure_plate.png` and tool inputs `tools/textures/hand/pad_base_{diamond,gold,iron}.png`, `pad_res/netherite_pressure_plate.png`.
   Fix (not done, touches models/data): reference the vanilla texture directly in the model JSON where the pixels are identical, or generate via `tools/textures/vanilla_derived.py`; for armour/shulker/pig either drop the override or redraw. Not checked: near-identical recolours (only exact matches were searched) and sounds against vanilla.
2. **simpletools is not yet a required sub-mod of simplebuilding on `claude-wave1`**: `modules.json` `requires` has no `simpletools` here; the change (WH26-7 `672af0ad6`) lives only on `b-bundle129`. The generated `depends` of the SB jar follows `modules.json`, so it follows automatically once b-bundle129 is merged. Release must include that merge.
3. **ElevenLabs provenance and licence**: no credits or provenance file exists in the repo. ElevenLabs output is in `modules/simplemobs` (64 ogg, Hollow Howler takes), `modules/simplemusic` (6 records + press sounds) and `mc26_3/overlay/.../sounds/records` (9 ogg, origin of brimstone/daybreak/driftwood/voidline not documented by any generator in the repo). Commercial redistribution depends on the ElevenLabs plan used at generation time (free plans need attribution and exclude commercial use). Owner must confirm the plan; then add a credits line (sketch in section "Sound and texture provenance").
4. **Open release items from `OFFEN-RELEASE-2026-11-05.md`** still open on this base: 11 listed there (WE25-8, WF25-5, WF25-11, WH26-2, WH26-6, WH26-7, WI26-2, WI26-5, BS25-19 crash with soul-fire blaze head, WP-06, SNOW32-FALL) plus IG31-42, b-qol33 acceptance, RELQOA acceptance page, Sodium/Iris run, e5 modpack compatibility, e3 benchmark (WP-34). Not re-verified here.
5. **`DevVariants.DEFAULT_ON = true`** (`simplelib/api/DevVariants.java`, known from RELEASE-QOL-OPT-AES) must be dev-only before release; not touched here. Check it is closed.

## Only the owner can

- Decide the domain: all contact URLs stay as they are (see "Contact URLs").
- Confirm the ElevenLabs plan / licence (blocker 3) and approve credits wording.
- Provide listing icons: all 59 icons are 16x16 pixel art (by design, `check_mod_icons.py`); Modrinth and CurseForge listings expect a larger image (256x256 or more), a nearest-neighbour upscale is trivial once approved.
- Version scheme: modules ship 0.1.0 / 0.9.0 / 0.4.0 (simplelib) / SB 2.0.0; choose the numbers for the first public files (AUDIT-STIL ST-14).
- Client acceptance, benchmark day (WP-34), Modrinth/CurseForge upload (nothing is uploaded without a yes).

## Table mod x check

Legend: ok / fixed / draft. "Meta" = name, EN description, version, licence, authors, icon file, contact. "Deps" = fabric.mod.json depends/suggests against modules.json (modId mapping, e.g. simpledimensions -> simpledimension, honoured). "Lang" = EN and DE key sets equal and no transliterations. "Keys" = number of EN keys. "Changelog" = `docs/release/CHANGELOG-<mod>.md`, auto-drafted (raw highlights, needs editing).

| Mod | Version | Meta | Deps | Lang | Keys | Changelog |
|---|---|---|---|---|---|---|
| simplebuilding | 2.0.0 | fixed (desc/author); sources URL -> fabric-example-mod (owner) | ok (generated from modules.json); simpletools NOT yet (only on b-bundle129) | ok | 3575 | draft |
| simplemoney | 0.9.0 | ok | ok | ok | 76 | draft |
| simpleriding | 0.9.0 | ok; own repo URLs (owner) | ok | ok | 114 | draft |
| simplemodels | 0.1.0 | ok | ok | ok | 82 | draft |
| simplefun | 0.9.0 | ok | ok | ok | 101 | draft |
| simplevisuals | 0.9.0 | ok | ok | ok | 257 | draft |
| simplesounds | 0.1.0 | ok | ok | ok | 88 | draft |
| simplequalityoflife | 0.9.0 | ok | ok | fixed (Oefen/gebuendelt) | 220 | draft |
| simpletweaks | 0.9.0 | ok | ok | ok | 502 | draft |
| simpledimensions | 0.1.0 | ok | ok | ok | 169 | draft |
| simplesandwiches | 0.1.0 | ok | ok | ok | 66 | draft |
| simplelib | 0.4.0 | fixed (desc was stale) | ok | ok | 121 | draft |
| simpleinterfaces | 0.1.0 | ok | ok | ok | 167 | draft |
| simplemaps | 0.1.0 | ok | ok | ok | 131 | draft |
| simplelivemap | 0.1.0 | ok | ok | ok | 45 | draft |
| simpletrims | 0.1.0 | ok | ok | ok | 65 | draft |
| simplemobs | 0.1.0 | ok | ok | ok | 282 | draft |
| simpleweather | 0.1.0 | ok | ok | ok | 6 | draft |
| simplemovement | 0.1.0 | ok | ok | ok | 25 | draft |
| simpletools | 0.1.0 | ok | ok | fixed (Oefen/gebuendelt) | 159 | draft |
| simplefarming | 0.1.0 | ok | ok | ok | 17 | draft |
| simplemobmanners | 0.1.0 | ok | ok | ok | 28 | draft |
| simpleloot | 0.1.0 | ok | ok | ok | 8 | draft |
| simplequestionary | 0.1.0 | ok | ok | ok | 26 | draft |
| simpleaesthetics | 0.9.0 | ok | ok | ok | 8 | draft |
| simpleenddragonfight | 0.1.0 | ok | ok | ok | 96 | draft |
| simpleend | 0.1.0 | ok | ok | ok | 137 | draft |
| simplerespawn | 0.1.0 | ok | ok | ok | 60 | draft |
| simplebeacons | 0.1.0 | ok | ok | ok | 108 | draft |
| simpleovens | 0.9.0 | ok | ok | ok | 149 | draft |
| simplemobheads | 0.9.0 | ok | ok | ok | 529 | draft |
| simpleshops | 0.1.0 | ok | ok | ok | 140 | draft |
| simplewanderers | 0.1.0 | ok | ok | ok | 33 | draft |
| simplestats | 0.1.0 | ok | ok | ok | 264 | draft |
| simpleblocks | 0.9.0 | ok | ok | ok | 485 | draft |
| simplecrucibles | 0.9.0 | ok | ok | ok | 121 | draft |
| simpleoctets | 0.9.0 | fixed (authors/contact were missing) | ok | ok | 408 | draft |
| simplehammers | 0.9.0 | ok | ok | ok | 85 | draft |
| simpleoptimization | 0.1.0 | ok | ok | ok | 7 | draft |
| simpleoptrender | 0.1.0 | ok | ok | ok | 7 | draft |
| simpleoptmemory | 0.1.0 | ok | ok | ok | 2 | draft |
| simpleoptserver | 0.1.0 | ok | ok | ok | 5 | draft |
| simplestorage | 0.9.0 | ok | ok | ok | 268 | draft |
| simplemusic | 0.1.0 | ok | ok | ok | 230 | draft |
| simplelights | 0.9.0 | ok | ok | ok | 93 | draft |
| simplegadgets | 0.9.0 | ok | ok | ok | 72 | draft |
| simpleearth | 0.1.0 | ok | ok | ok | 254 | draft |
| simplechallenges | 0.1.0 | ok | ok | ok | 224 | draft |
| simpleanimals | 0.1.0 | ok | ok | ok | 49 | draft |
| simpledinos | 0.1.0 | ok | ok | ok | 41 | draft |
| simpleores | 0.1.0 | ok | ok | ok | 133 | draft |
| simplemachines | 0.1.0 | ok | ok | ok | 74 | draft |
| simplearrows | 0.1.0 | ok | ok | ok | 65 | draft |
| simpleresonance | 0.1.0 | ok | ok | ok | 44 | draft |
| simplefoundry | 0.1.0 | ok | ok | ok | 38 | draft |
| simpleenchanting | 0.1.0 | ok | ok | ok | 63 | draft |
| simplepads | 0.1.0 | ok | ok | ok | 203 | draft |
| simplebuildingtools | 0.1.0 | ok | ok | ok | 353 | draft |
| simplechisels | 0.1.0 | ok | ok | ok | 42 | draft |

Results across all mods:
- Metadata: name, description (EN), licence CC0-1.0, icon file, `minecraft ~26.3`, `fabricloader >=0.19.5`, `java >=25`, environment `*` identical for all 59. Fixed: SimpleBuilding description "This is a simple building mod." and author "Me!", SimpleLib description (only crucibles named), Simple Octets had no authors and no contact.
- Lang: EN and DE key sets are equal for all 59 mods (SimpleBuilding merged with the 26.3 overlay: 3672 keys each), no empty or TODO values. Fixed three DE transliterations (`Oefen` x2, `gebuendelt`). Remaining ae/oe/ue hits were checked by hand and are legitimate words (Feuer, neue, Steuer, Quelle, zuerst, Lavaeimer, ...).
- Deps: every required sub-mod of SimpleBuilding is injected from `modules.json` at build (`mc26_3/fabric/build.gradle`), so no drift. 22 mods depend on `simplelib` in fabric.mod.json although `modules.json` `requires` does not list it (it is bundled; documentation drift only, not a runtime issue). `modules.json` uses `cloth_config` in `requires` while fabric.mod.json uses the real id `cloth-config` (cosmetic). SimpleBuilding optional `curios` and `trinkets_updated`: `curios` is in `modules.json` optional but not in `suggests` (cosmetic).
- Changelog: the root `CHANGELOG.md` covers only SimpleBuilding 2.0.0 (module split). No per-mod changelog existed. Drafts for 59 mods are in `docs/release/`; they are raw (feature commit subjects, German internals partly rewritten) and must be edited by hand before upload.
- TODO/FIXME in 26.3 Java: one: `modules/simplehammers/shared/java/com/simplehammers/SimpleHammers.java:48` (Simple Enderite stage P4, not release relevant). The other hits are recipe patterns (`XXX`) and a TODO detector in Questionary.

## Contact URLs (not changed)

| Field | URL | Mods |
|---|---|---|
| homepage | https://azarfane.de/ | 58 mods (all except simpleriding) |
| sources | https://github.com/Ouzzi/simplebuilding | 56 |
| issues | https://github.com/Ouzzi/simplebuilding/issues | 56 |
| sources | https://github.com/Ouzzi/fabric-example-mod | simplebuilding (looks like a left-over from the template; decide with the domain) |
| homepage/sources/issues | https://github.com/Ouzzi/simpleriding (+ /issues) | simpleriding |

## Sound and texture provenance

- Sound files in the repo: 3 piston bore (synth, `tools/sounds/make_bore_sound.py`), 6 sandwich sounds (`make_sandwich_sounds.py`), 2 legendary (`make_legendary_sound.py`), redstone lines / lamps / pistons in the overlay (`make_redstone_line_sounds.py`), 64 Hollow Howler takes (ElevenLabs via `eleven_howler_takes.py`, synth variants `make_howler_sounds.py`), trumpet samples, records and press sounds (ElevenLabs, see blocker 3). Procedural sounds are our own work (CC0). Sound effect files of `simplesounds` do not exist: it only maps vanilla events (`modules/simplesounds/AUDIT.md`).
- Suggested credit line once the plan is confirmed: "Music discs, the Hollow Howler voice and some effects were generated with ElevenLabs under a commercial licence."
- Textures: vanilla-derived outputs are listed in `tools/textures/vanilla_derived.py` and gitignored. This check found 3 tracked wiki copies of such outputs (hopper minecarts of Simple Machines): untracked and added to `.gitignore` here. The 1.21.11 line (`mc1_21_11`) also tracks `netherite_apple.png`, `enderite_apple.png`, `astralit_ore.png`, `nihilith_ore.png`; out of scope (no release of that line), listed for later.

## What was changed on this branch

- `fix(lang)`: DE `Oefen` x2 and `gebuendelt` in Simple Tools and Simple QoL.
- `fix(octets)`: authors and contact in fabric.mod.json.
- `fix(meta)`: SimpleBuilding description and author, SimpleLib description.
- `fix(textures)`: three vanilla-derived wiki PNGs untracked and ignored.
- `docs(release)`: 59 changelog drafts in `docs/release/`.
