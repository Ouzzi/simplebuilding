# Simple Music 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Music** for Minecraft 26.3 on Fabric.

Sheet music as a text note code (editor with test listening, .txt/.nbs import), pressed onto blank discs in the Disc Press, and uploaded mono Ogg discs with quota and OP approval, played in the Vanilla jukebox and the Pocket Jukebox.

## Added

- **Sheet Music** - Shapeless from paper, an ink sac and a feather.
- **Note code** - Text lines: "tempo 4" (ticks per step, 1-20), "$h = harp" or "$k = sound minecraft:entity.cat.ambient" (aliases), "$h: c4 e4 g4 - . [c4 e4]" (tracks).
- **Editor** - Coloured code field with line numbers and underlined problems (the blueprint editor's field, now in SimpleLib).
- **Import** - Put .txt note code or Note Block Studio .nbs files into config/simplemusic/sheets/ and pick one in the editor; the import only creates text.
- **Copying at the cartography table** - Like a blueprint: a signed sheet on top, an empty sheet below gives a copy; only the empty sheet is used up.
- **Blank Disc** - Four coal or charcoal and four blackstone around an amethyst shard.
- **Disc Press** - A piston over two iron ingots over three smooth stone.
- **File check** - The server checks every upload itself without decoding: Ogg Vorbis only, one stream, intact pages (checksums), mono only, 22.05-48 kHz, at most 4 MiB and 5 minutes (server settings).
- **Custom Disc** - A pressed disc carries either an uploaded track (hash, title, length, uploader - never audio; amethyst label) or a composition (paper label).
- **Music Discs** - Six music discs by Simple Music, each found only in one kind of structure, like Vanilla's Pigstep or Otherside.
- **Pocket Jukebox** - A bundle for up to 20 discs (Vanilla discs and track discs).
- **Placed Pocket Jukebox** - Sneak + right click with the pocket jukebox on a block puts it down, front to you.
- **Moderation** - Everyone may upload up to 5 tracks; others hear a track only after an operator approves it, until then only the uploader.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simplebuilding, simplemobheads

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemusic config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
