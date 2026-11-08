# Credits and provenance

## Sounds generated with ElevenLabs

Part of the sound effects and music were generated with ElevenLabs by the author, on a plan that includes commercial
use (confirmed by the author on 2026-11-08). The files are redistributed with the mods under the repository license.
Affected mods and folders (found with `grep -ril elevenlabs docs/ tools/`; generators and plans are in `tools/sounds/`
and `docs/ai/PLAN-A-SOUND24-2026-10-23.md`):

| Mod | Folder | Content |
|---|---|---|
| Simple Mobs | `modules/simplemobs/shared/resources/assets/simplemobs/sounds/entity/hollow_howler`, `.../sounds/item/trumpet`, `modules/simplemobs/shared/howler-eleven` | Hollow Howler voice takes, trumpet samples |
| Simple Music | `modules/simplemusic/shared/resources/assets/simplemusic/sounds/records`, `.../sounds/press` | six music discs, press sounds |
| Simple Lib | `modules/simplelib/shared/resources/assets/simplelib/sounds/legendary1.ogg`, `legendary2.ogg` | legendary choir sounds |
| Simple Building (26.3 overlay) | `mc26_3/overlay/resources/assets/simplebuilding/sounds/records`, `.../sounds/block/astralit_lamp`, `.../sounds/block/nihilith_lamp` | record tracks, lamp sounds |

All other sounds are procedurally synthesized by scripts in `tools/sounds/` (own work, CC0) or are Minecraft events
reused by name (no Mojang audio files are included).

## Textures

Textures are our own pixel art, or are derived from Minecraft textures at build time only: such outputs are listed in
`tools/textures/vanilla_derived.py`, are generated from the local client jar and are never committed. The repository
contains no Mojang assets. Large listing icons (`docs/release/icons`) are 128 and 512 px nearest-neighbour
copies of our own 16x16 mod icons (`tools/textures/mod_icons_large.py`).

## Libraries

Fabric API (Apache-2.0), Fabric Loader, Cloth Config (LGPL-3.0, optional), Mod Menu (MIT, optional) and, for the other
loaders, NeoForge / Forge and their config APIs, as declared in the mods' dependency metadata. JEI is used only as a
compile-time plugin API. Their licenses stay with their authors.

## Deutsch (Kurzfassung)

Ein Teil der Klaenge und Musik wurde vom Autor mit ElevenLabs erzeugt (kommerziell nutzbarer Plan, bestaetigt am
08.11.2026); betroffen sind die Ordner in der Tabelle oben. Texturen sind eigene Pixelkunst oder werden erst beim
Bauen aus Minecraft-Texturen abgeleitet (nie eingecheckt); keine Mojang-Assets im Repository. Bibliotheken
(Fabric API, Cloth Config, Mod Menu usw.) behalten ihre eigenen Lizenzen.
