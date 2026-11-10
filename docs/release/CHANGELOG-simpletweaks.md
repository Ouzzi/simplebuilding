# Simple Tweaks 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Tweaks** for Minecraft 26.3 on Fabric.

Legacy compatibility and server-authoritative claims, on by default.

## Added

- **Claims (On by Default)** - Server-authoritative claims, on by default: /simpletweaks claim create (or a legacy deed) claims the chunk you stand in; only you and players you trust may build, break, open containers, use vehicles or take items there.
- **Claim Deed** - Craft paper, an ink sac and honeycomb into an empty Claim Deed.
- **Old Claim Table** - The claim table is gone: claims are seen on the wayfinder map (Simple Maps) and in the deed.
- **Claims on the map** - Claims are seen first on the wayfinder map of Simple Maps: with its Claims layer on, the map shows your own and other people's claims as colored strips on explored terrain, in every dimension.
- **Block and container log (on by default)** - Logs blocks players break or place, items taken from or put into containers, sign edits, explosions and fire (hoppers optional), with actor, time, position and dimension. /simpletweaks inspect shows a block's history, /simpletweaks lookup searches with filters, /simpletweaks rollback undoes (with --preview), /simpletweaks restore redoes the last rollback.
- **Inventory backups on death (on by default)** - Before a player drops anything on death, their inventory, armor, offhand, XP, time, cause and position are saved; the last keep deaths (default 5, 1–50) per player stay. /simpletweaks deaths <player> [n] opens them in the admin inventory screen.
- **/simpletweaks invsee and /simpletweaks endersee (on by default)** - Own admin screen in the SimpleLib box style: the target's armor, offhand, inventory and hotbar (or the ender chest) above your own inventory.
- **Vanish (on by default)** - /simpletweaks vanish [player] makes an admin invisible to players below seePermission (default 2): not sent as an entity, left out of the tab list and the server list count and sample, no join or leave message.
- **Freeze (on by default)** - /simpletweaks freeze <player> holds a player in place: every move is set back, and breaking, using, placing, attacking and the drop key are refused.
- **Restart announcements (on by default)** - Scheduled restart times (times, "HH:mm" in server time) and /simpletweaks restart <minutes> count down in chat at the warning minutes (default 15, 10, 5, 1) and at 30, 10…1 seconds, with a boss bar; at zero everything is saved and the server stops cleanly.
- **Automatic world backups (on by default)** - Every intervalHours (default 6, 1–168) and with /simpletweaks backup now the world is zipped like save-off, save-all flush, save-on: saving pauses, everything is flushed, a worker thread writes the archive, then saving resumes.
- **Maintenance mode (switch on, mode off)** - /simpletweaks maintenance on lets only operators of joinPermission (default 2) and the names or UUIDs in allowed join; everybody else gets kickMessage.
- **Lag finder (on by default)** - /simpletweaks lag [seconds] (default 5, 1–30) measures tick time and TPS, time per block entity type and entity type, the most expensive chunks with their entity and block entity counts, and hints at possible redstone clocks.
- **Share coordinates (T4)** - /simpletweaks coords [message] shares your position with everybody, /simpletweaks coords to <player> [message] with one player.
- **Private death hint (T5)** - After a real death only you get a line with place (copy on click), dimension, cause and how long your items stay (5 minutes while the area is loaded).
- **Chat mentions (T6)** - @Name in chat is highlighted in yellow for everybody, and the mentioned players hear a soft bell.
- **/simpletweaks playtime and /simpletweaks seen (T7)** - /simpletweaks playtime [player] shows the play time from the Vanilla statistics; /simpletweaks seen <player> says whether a player is online or how long ago they left.
- **Mob griefing per mob (T8)** - Listed mobs (blocked, default creeper, enderman, ghast, wither, ravager) cannot change blocks; all other mobs follow the Vanilla rule mob_griefing.
- **PvP per player (T9)** - /simpletweaks pvp on|off (no argument: status).
- **Newcomer protection (T12)** - New players take and deal no PvP damage during their first minutes (default 5) of online play time in this world; then PvP works as usual again.
- **Welcome and announcements (T19)** - A welcome line for joining players ({player} is replaced), rotating tips every tipMinutes and own join/leave lines instead of the Vanilla ones.
- **Daily login reward (T20)** - The first join of a day gives the reward items; days in a row build a streak, a gap starts again at 1, and every bonusEvery days (default 7) there are bonus items.
- **Server events (T21)** - Operators start events with /simpletweaks event start <double_xp|more_loot|blood_moon> [minutes], stop and status; one event at a time with a boss bar.
- **Leaderboard (T22)** - /simpletweaks leaderboard <playtime|deaths|kills|mined> lists the top 10 of every player known to the server.
- **Votes (T23)** - /simpletweaks vote weather clear and /simpletweaks vote event <name> start a vote with clickable Yes/No; /simpletweaks vote yes|no counts.
- **AFK detection (T24)** - After minutes (default 5) without any action, or with /simpletweaks afk, a player is AFK: gray with [AFK] in the tab list and not counted for sleeping.
- **World border per dimension (T25)** - Entries in border.dimensions (size and center per dimension) are applied to that dimension's world border at server start; /simpletweaks border lists all borders.
- **Nether roof lock (T26)** - Survival and adventure players above Y 127 in the Nether are put back below the roof (last place below it, or a free spot under it).
- **Partial keep inventory (T27)** - While the Vanilla rule keep_inventory is off, part of the inventory stays on death: hotbar_armor (hotbar, armor, offhand), xp (experience) or custom (switches per part and keepXpPercent).
- **Dragon egg every fight (T28)** - Every later dragon kill puts a new dragon egg on the exit portal; if an egg already lies there, it drops as an item.
- **Phantoms per player (T30)** - /simpletweaks phantoms off stops phantoms from spawning for you; /simpletweaks phantoms on brings them back.
- **Mob limit per chunk (T31)** - Every scanSeconds the mobs per chunk are counted; over perChunk (default 50) the claim owner or nearby players and operators get a message.
- **Ground item cleanup (T32)** - Every intervalMinutes (default 15) ground items are cleared after a chat countdown; /simpletweaks cleanup now starts it at once (operators).
- **Chunk pregeneration (T33)** - /simpletweaks pregen start <radius> [dimension] generates every chunk around the world border center; pause, resume, stop and status (operators of level 3).
- **Automatic off with partner mods (autoOffWith)** - A feature switches itself off at server start when a mod that does the same job is loaded: Ledger or MineTracer for the block log, InvRestore for death backups, Inv View for /invsee, Chunky for pregeneration, Litematica or Axiom for the action checks.
- **Anti-X-Ray** - Ores that touch no air, water or other see-through block are sent to players as their host stone (stone, deepslate, netherrack, end stone), so x-ray texture packs, x-ray mods and noclip cameras see no hidden ore.
- **Freecam rule** - When the server turns it on (freecam.enabled, default off), players with the Freecam mod may still fly the camera, but not through blocks: Simple Tweaks on the client forces Freecam's block collision while connected to that server.
- **Further protections** - Three more switches, each off by default: seedHash sends a random hashed seed to clients, hideHealth sends other players' health as their maximum, actionChecks reports reach and fast-break attempts to operators (no automatic punishment).

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib
- Optional integrations: simpledimensions, simplebuilding, simplemoney, simpleweather, simplemaps

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpletweaks config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
