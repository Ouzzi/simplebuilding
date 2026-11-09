# Mod-Struktur

Erzeugt aus `modules/modules.json` durch `python tools/modtree/modtree.py` (nicht von Hand bearbeiten). Grafische Fassung: `docs/MODSTRUKTUR.html`.

## Gemeinsame Basis

- **SimpleLib** `simplelib` [fertig]: Shared content bundled by SimpleBuilding, Simple Crucibles and Simple Sandwiches: warm food, shared materials (building cores, Strong Upgrade with the Wither drop), id moves, in-world upgrades and UI building blocks.
  - optional: jei
  - required von: Simple Arrows, Simple Beacons, Simple Blocks, Simple Building Tools, Simple Challenges, Simple Chisels, Simple Crucibles, Simple Earth, Simple Enchanting, Simple Gadgets, Simple Hammers, Simple Interfaces, Simple Layers, Simple Lights, Simple Machines, Simple Maps, Simple Mobheads, Simple Mobs, Simple Music, Simple Octets, Simple Ores, Simple Ovens, Simple Pads, Simple Questionary, Simple Shops, Simple Storage, Simple Trims, Simple Tweaks, SimpleBuilding

## SimpleBuilding

- **SimpleBuilding** `simplebuilding` [fertig]: Building tools, storage, machines, and enchantments.
  - required: cloth-config, SimpleLib, Simple Ovens, Simple Mobheads, Simple Crucibles, Simple Octets, Simple Blocks, Simple Hammers, Simple Storage, Simple Lights, Simple Gadgets, Simple Ores, Simple Machines, Simple Arrows, Simple Mobs, Simple Resonance, Simple Enchanting, Simple Pads, Simple Building Tools, Simple Tools, Simple Chisels
  - optional: jei, roughlyenoughitems, modmenu, jade, appleskin, mousetweaks, trinkets_updated, curios, Simple Maps, Simple Questionary, Simple Riding, Simple Sandwiches, Simple Tweaks, Simple Money, Simple Fun, Simple Dimensions, Simple Trims, Simple End, Simple Respawn, Simple Beacons, Simple Shops, Simple Wanderers, Simple Animals, Simple Music, Simple Earth, Simple Challenges, Simple Dinosaurs, Simple Layers
  - **Simple Arrows** `simplearrows` (required) [neu]: Arrows: the Fletching Table (moved from SimpleBuilding on 04.11., ids migrate) builds 4 arrows from a tip, a shaft and a fletching, optionally with a potion; the Light Arrow (glowstone dust, stick, feather) lights the air where it sticks and the light goes when the arrow despawns like a normal arrow or is picked up; the Silver Arrow (moved from Simple Ores on 03.11., ids migrate) hits undead harder and is crafted only with Simple Ores' silver. Works on its own with Vanilla (tips and shafts of other mods appear with them). Fabric only for now.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores
  - **Simple Blocks** `simpleblocks` (required) [fertig]: Missing building blocks: Hushed Deepslate Tiles (dampen vibrations like wool), Sculk Deepslate, the missing Vanilla stairs, slabs and walls and cracked/mossy brick variants, and SimpleBuilding's building blocks without a home of their own (suspended and levitating sand/gravel, dirt/grass/sand/gravel slabs, cracked and chiseled ice, the nautilus shell block, three froglight colors; ids migrate). The Shapecutter cuts stone and wood into every stair, slab and shape. Works on its own with Vanilla; required sub-mod of SimpleBuilding.
    - required: SimpleLib
    - optional: SimpleBuilding, jei
  - **Simple Building Tools** `simplebuildingtools` (required) [neu]: Building tools of the SimpleBuilding family: Building Wands (copper to Enderite and creative), the Octant in 17 colors and the Blueprint with editor, picture import and cartography table copies. Simple Ores adds the Enderite wand, SimpleBuilding the backpack supply and the placed octants.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores, Simple Storage, Simple Hammers, Simple Octets
  - **Simple Chisels** `simplechisels` (required) [neu]: Seven chisels (Stone to Enderite) that move blocks one step along their family - stone to chiseled, bricks to cracked, planks to stairs to slabs - and back with sneak, with the enchantment Fast Chiseling, moved from SimpleBuilding (ids migrate). Other mods add their chains through data files.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores, jei
  - **Simple Enchanting** `simpleenchanting` (required) [neu]: The enchanters above Vanilla's table: the Dark Enchanter (SimpleBuilding's Astral Enchanting Table until 03.11.: three random enchantments with sliders up to Vanilla's maximum, tier 50 with blazewood bookshelves and a Blazing or Nihil Obsidian floor) and the Astral Enchanter (tier 60 on a Nihil Obsidian floor, Blazing works half; Protection V and Feather Falling V). Made in the world with a hammer and 8 nuggets (Netherite; Enderite for the Astral one, needs Simple Ores), breaking gives them back. SimpleBuilding owns the building blocks; alone this module brings its own Blazing and Nihil Obsidian and blazewood planks and bookshelves. Fabric only for now.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores, Simple Hammers
  - **Simple Gadgets** `simplegadgets` (required) [fertig]: Gadgets you hold and use - for now the Gauge (speed dial, altimeter with fall-damage warning, auto-walk), moved from SimpleBuilding (ids migrate); the Transposer, Resonance Rod, Detector, Attractor, Echo Sounder, Color Brush and Octant follow stage by stage. Fabric only for now.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Hammers** `simplehammers` (required) [fertig]: Seven sledgehammers (Stone to Enderite) that mine a 3x3 area, reshape blocks into stairs and slabs, carve octets, crush diamond and quartz blocks and upgrade blocks in the world strike by strike, with the enchantments Break Through, Override and Radius, moved from SimpleBuilding (ids migrate). While it is loaded the in-world ways of the other Simple mods take the hammer instead of the axe.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Octets, Simple Crucibles, Simple Ores, jei
  - **Simple Lights** `simplelights` (required) [fertig]: Light Panes: flat 1 px lamps on any block side - plain, transparent, sixteen colors, a redstone pane whose color follows the signal strength and copper panes that toggle like the Copper Bulb, moved from SimpleBuilding (ids migrate). Fabric only for now.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Machines** `simplemachines` (required) [neu]: The machine parts of the Simple Machines tab: Double Piston (one block, two stages; Vanilla push rules), Signal Dampener (four stages take 0-3 off the signal, one redstone tick late) and, since stage 2, the piston upgrades (Reinforced and Reinforced Sticky Piston push 18 and one paid unbreakable; Netherite and Enderite Piston break blocks, wear out and are repaired with their nugget) and, since stage 3, the mod hoppers (Reinforced, Netherite, Enderite: 2/4/8 times as fast, item filter) with their hopper minecarts. Vanilla and SimpleLib ingredients; the Enderite parts need Simple Ores. Fabric only for now.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores
  - **Simple Mobheads** `simplemobheads` (required) [fertig]: A head for every mob: dropped when a charged creeper's explosion kills it, worn for a small secret, placed, or played on a note block. Required sub-mod of SimpleBuilding; Simple Fun's and SimpleBuilding's heads moved in.
    - required: SimpleLib, cloth_config
    - optional: SimpleBuilding, Simple Mobs, Simple End, Simple End Dragon Fight, jei, modmenu
  - **Simple Mobs** `simplemobs` (required) [neu]: Hollow Howler and Hollow Watcher bosses, Shellker, Soulfire Blaze, Dread and Deceiver mobs, Blighted and Abyssal Wither with Nether Heart and Red Nether Gem, and the Potion of Silence.
    - required: SimpleLib, Simple Resonance
  - **Simple Octets** `simpleoctets` (required) [fertig]: Octets: half-block cubes (0.5 x 0.5 x 0.5) of every wood, every block with stairs and slabs and the melon, which mix in one cell, moved from SimpleBuilding (ids migrate). Cut in the stonecutter, crafted back into the block; the sledgehammer of Simple Hammers carves them. Octets of SimpleBuilding's and Simple End's blocks only with that mod.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple End, Simple Ores, jei
  - **Simple Ores** `simpleores` (required) [neu]: Silver ore in the Overworld and the End, and rose gold: two alloys of gold, copper and silver (hard Rose Copper, soft Rose Gold), pressed into pressings, melted in the blast furnace only, with tools and armor of their own. Since IG31-13 also Enderite (the tier above netherite; other Simple mods show their Enderite variants only with it), Dimensional Scrap and Sage Ore. Works on its own with Vanilla.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Hammers
  - **Simple Ovens** `simpleovens` (required) [fertig]: Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces (2x/4x/8x speed, double experience from Netherite) and furnace minecarts, moved from SimpleBuilding (ids migrate). In-world upgrades with the axe on its own, with the sledgehammer of Simple Hammers.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Foundry, Simple Crucibles, Simple Ores
    - **Simple Crucibles** `simplecrucibles` (optional) [fertig]: Iron, Reinforced, Netherite and Enderite crucibles (a cooking station without fuel; the barrels that dock onto them are Simple Storage's), the reinforced cauldron and village field kitchens, moved from SimpleLib and SimpleBuilding (ids migrate). In-world upgrades with the axe on its own, with the sledgehammer of Simple Hammers.
      - required: SimpleLib
      - optional: SimpleBuilding, jei, jade, Simple Storage, Simple Ores, Simple Tools
    - **Simple Foundry** `simplefoundry` (optional) [neu]: A multiblock foundry for Simple Ovens: a tank of melting chambers in a brick hull, fuel on the desk, ore thrown in, the melt poured by layer through spouts into casting trays. Gold, copper and iron, one ingot per ingot.
      - optional: Simple Ovens
  - **Simple Pads** `simplepads` (required) [neu]: The pads and pressure plates of the Simple Pads tab: Launchpad, Elytra Pad, Flypad, Potion Pad, Spawn Teleporter and Chunk Loader in three tiers each, the Diamond, Netherite, Enderite and copper pressure plates and the Spawn Elytra (moved out of SimpleBuilding, DW04C-5). Requires Simple Ores (Enderite tiers) and Simple Mobheads (the heads of the first tiers). Fabric only for now.
    - required: SimpleLib, Simple Ores, Simple Mobheads
    - optional: SimpleBuilding, jade
  - **Simple Resonance** `simpleresonance` (required) [neu]: Player value, danger zones and spawn owners: how experienced a player is (playing time, survival, bosses, partial loss on death), how dangerous the land is (soft zones from the spawn), which player a monster spawn belongs to and a newcomer protection. An API for other mods (Simple Mobs gives new monsters status effects by it); works on its own with Vanilla.
    - optional: SimpleBuilding, Simple Mobs
  - **Simple Storage** `simplestorage` (required) [fertig]: Crates (open wooden food boxes for every wood, hoppers and comparators work), the Storage Crafting Table (a crafting table that keeps its grid), tiered and trapped chests, trapped copper chests and tier shulker boxes (more slots, bigger stacks, upgraded in the world keeping their contents), moved from SimpleBuilding (ids migrate). Chest carts and chest boats, bundles, quivers, the placed bundle and the storage enchantments Drawer, Deep Pockets and Funnel (8c-1) also moved, then the four backpacks (8c-2). WH26-6: the four barrels (copper to Enderite; docking onto the crucibles of Simple Crucibles) moved here from Simple Crucibles.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple End, trinkets_updated, curios, Simple Ores, Simple Crucibles
  - **Simple Animals** `simpleanimals` (optional) [neu]: Deer, wild boar, hedgehog, crab and zebra (with the infertile zorse): five new animals with low spawn rates, Vanilla goals and no block damage. Works on its own with Vanilla.
    - optional: SimpleBuilding, Simple Money, Simple Mobheads
  - **Simple Beacons** `simplebeacons` (optional) [neu]: Strong Beacon (6 pyramid levels, 10 more effects, range by pyramid material, two-column screen), Strong Conduit (7x7x7 frame, up to 216 blocks under water, Dolphin's Grace) upgraded with SimpleLib's Strong Upgrade template. Axe on its own, sledgehammer with a hammer mod.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores
  - **Simple Challenges** `simplechallenges` (optional) [neu]: Game modes for a world of your choice. S0-S2: the Unlock mode - the world is cut into cells (a block column, a chunk, three chunks) and only free cells can be entered; every cell has a price worked out from the world (items, credits from advancements or levels), with deadlock protection and an emergency unlock. Presets are data files. Without a chosen challenge the mod changes nothing (safety lock). Needs SimpleLib; SimpleBuilding optional.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Dimensions** `simpledimensions` (optional) [neu]: Configurable dimensions, light arches, and safe linked portals.
    - required: cloth_config
    - optional: SimpleBuilding, modmenu, flan, ftbchunks, openpartiesandclaims, griefdefender, claimchunk
  - **Simple Dinosaurs** `simpledinos` (optional) [neu]: Fossil block, dinosaur eggs you brush free and brood by sitting on them, and the first dinosaur: a rideable T. rex that hatches as a hatchling and grows up. Works on its own with Vanilla.
    - optional: SimpleBuilding, Simple Money, Simple Mobheads
  - **Simple Earth** `simpleearth` (optional) [neu]: The Overworld seen from orbit: above a very high activation height the world becomes a slightly rounded cube (27 display versions, standard V25 folding world) whose faces show Vanilla map colours sampled from the world generator (biomes and heights from the seed, no chunks generated); flat sun, cube moon, stars. Below everything stays Vanilla. Needs SimpleLib; SimpleBuilding optional.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple End** `simpleend` (optional) [neu]: End wood (Ashwood, Duskwood) with buds, trees and stick scrub, End grass, the Ash Valley and Dawnlands biomes on the outer End islands; bundles Simple End Dragon Fight.
    - optional: Simple End Dragon Fight
    - **Simple End Dragon Fight** `simpleenddragonfight` (optional) [neu]: Ender Dragon fight: +1 health bar per player on the island, a faster dragon, entry animations and Dragonmites from half health.
      - optional: Simple Mobs
  - **Simple Fun** `simplefun` (optional) [fertig]: Bounded playful mechanics, throwable bricks and cosmetic transformations (the animal heads moved to Simple Mobheads in 1.4.0).
    - required: cloth_config
    - optional: modmenu, jei, jade
  - **Simple Layers** `simplelayers` (optional) [neu]: World layers: the Cloud Sea sky dimension above y 700 and the Backrooms below the bedrock, with cloud blocks, wallpaper, carpet and neon lights.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Maps** `simplemaps` (optional) [neu]: Wayfinder maps: endless maps per dimension with zoom, waypoints in the locator bar, framed views and cartography table upgrades.
    - required: SimpleLib
    - optional: Simple Money, Simple Ores
  - **Simple Money** `simplemoney` (optional) [fertig]: Currency production, weighted villager trades and treasure loot.
    - required: cloth_config
    - optional: jei, modmenu, SimpleBuilding, Simple Fun, Simple Models, Simple Quality of Life, Simple Riding, Simple Sandwiches, Simple Sounds, Simple Tweaks, Simple Visuals, Simple Ores
  - **Simple Music** `simplemusic` (optional) [neu]: Sheet music with a text note code (editor with test listening, .txt/.nbs import), pressed onto blank discs in the Disc Press, and uploaded mono Ogg discs (quota, OP approval), played in the Vanilla jukebox and the Pocket Jukebox. Needs SimpleLib; SimpleBuilding optional.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Mobheads
  - **Simple Questionary** `simplequestionary` (optional) [neu]: The Questionary Table: put any item in and read its lore, its use and a secret in an open book.
    - required: SimpleLib
  - **Simple Respawn** `simplerespawn` (optional) [neu]: Downed instead of dead: a teammate can revive you within the bleed-out time (then 3 hearts, no food); own downed screen with spectate, inventory view and give up.
  - **Simple Riding** `simpleriding` (optional) [fertig]: Saddle speed, horse jumps, nautilus dashes, mount armor utilities, loot, and trades.
    - required: cloth_config
    - optional: SimpleBuilding, modmenu, Simple Ores
  - **Simple Sandwiches** `simplesandwiches` (optional) [neu]: Sandwiches on a cutting board, a kitchen knife, cheese and butter from the cauldron, and eating straight out of bundles.
    - optional: SimpleBuilding, Simple End
  - **Simple Shops** `simpleshops` (optional) [neu]: Player shops: a Sales Block with a price (up to five exact price items) and a Display Case with up to five goods, one piece per trade. Protected from strangers, explosions, pistons and hoppers; no claim right needed to buy.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Money, Simple Tweaks
  - **Simple Trims** `simpletrims` (optional) [neu]: Glowing, Emitting and Pulsating armor trim templates, placeable trim templates and trim upgrades (moved from SimpleBuilding, ids migrate). Axe on its own, sledgehammer with Simple Hammers.
    - required: SimpleLib
    - optional: SimpleBuilding, jei
  - **Simple Tweaks** `simpletweaks` (optional) [fertig]: Legacy compatibility and server-authoritative claims, on by default.
    - required: SimpleLib
    - optional: Simple Dimensions, SimpleBuilding, Simple Money, Simple Weather, Simple Maps
  - **Simple Wanderers** `simplewanderers` (optional) [neu]: Village Wanderer: one Wandering Trader per village with a leashed horse, goods split from the traveling trader, a new stock every 3 days, a restock every 1.5 days and a range by village biome. Works on its own with Vanilla.
    - optional: SimpleBuilding, Simple Money, Simple Mobheads

## Simple Quality of Life

- **Simple Quality of Life** `simplequalityoflife` [fertig]: Super-mod: bundles Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools and Simple Movement with one switch each; guide book and start quest.
  - required: cloth_config, Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools, Simple Movement
  - optional: SimpleBuilding, modmenu
  - **Simple Farming** `simplefarming` (required) [neu]: Feather Falling protects farmland, Sharpness cuts grass, a hoe harvests and replants ripe crops; a sub-mod of Simple QoL, also playable alone.
    - required: cloth_config
    - optional: SimpleBuilding, Simple Quality of Life, modmenu
  - **Simple Interfaces** `simpleinterfaces` (required) [neu]: Restyle of the Vanilla container screens (a coloured box per block, drawn in code), linked containers and Easy Shulkers / Ender Chests; a sub-mod of Simple QoL, also playable alone.
    - required: cloth_config, SimpleLib
    - optional: SimpleBuilding, modmenu
  - **Simple Loot** `simpleloot` (required) [neu]: Vault cooldown in days instead of once per player; a sub-mod of Simple QoL, also playable alone.
    - required: cloth_config
    - optional: Simple Quality of Life, modmenu
  - **Simple Mob Manners** `simplemobmanners` (required) [neu]: Mute mobs by type or name tag, keep named babies young, piglins ignore gold trims and tools; a sub-mod of Simple QoL, also playable alone.
    - required: cloth_config
    - optional: Simple Quality of Life, modmenu
  - **Simple Movement** `simplemovement` (required) [neu]: Manual crawling, server-allowed autowalk, faster ladders and Frost Walker on powder snow; a sub-mod of Simple QoL, also playable alone.
    - required: cloth_config
    - optional: Simple Quality of Life, modmenu
  - **Simple Tools** `simpletools` (required) [neu]: Full-durability bonus, the Thrift and Soulbound enchantments, the Death Vault, the Biome Brush, anvil repairs without extra cost and lava buckets into furnaces; a sub-mod of Simple QoL, also playable alone.
    - required: cloth_config
    - optional: SimpleBuilding, Simple Quality of Life, modmenu
  - **Simple Weather** `simpleweather` (required) [neu]: Weather switch (server) and rain density (client); a sub-mod of Simple QoL, also playable alone.
    - required: cloth_config
    - optional: Simple Quality of Life, modmenu

## Simple Aesthetics

- **Simple Aesthetics** `simpleaesthetics` [fertig]: Super-mod without content of its own: bundles Simple Visuals, Simple Sounds and Simple Models with one on/off switch each.
  - required: Simple Visuals, Simple Sounds, Simple Models
  - **Simple Models** `simplemodels` (required) [neu]: Server-approved custom item models, anvil assignment, and a searchable model browser.
    - optional: SimpleBuilding, modmenu
  - **Simple Sounds** `simplesounds` (required) [neu]: Bounded local Vanilla sound counterparts for the twelve Simple Visuals immersion effects.
    - required: cloth_config
    - optional: Simple Visuals, SimpleBuilding, modmenu
  - **Simple Visuals** `simplevisuals` (required) [fertig]: Bounded client HUD, tooltips, anvil formatting, and Vanilla immersion particles.
    - required: cloth_config
    - optional: SimpleBuilding, Simple Models, modmenu

## Simple Optimization

- **Simple Optimization** `simpleoptimization` [neu]: Super-mod without content of its own: bundles Simple Opt Render, Simple Opt Memory and Simple Opt Server with one on/off switch each.
  - required: Simple Opt Render, Simple Opt Memory, Simple Opt Server
  - **Simple Opt Memory** `simpleoptmemory` (required) [neu]: Memory optimizations (block states, models); stage 1 only holds the switch, config and mod detection.
  - **Simple Opt Render** `simpleoptrender` (required) [neu]: Client optimizations (leaf culling, light-map cache, fast IP ping, telemetry off); each area steps back when a known optimization mod covers it.
  - **Simple Opt Server** `simpleoptserver` (required) [neu]: Server optimizations: view distance follows server load (MSPT) within server.properties bounds; steps back for ServerCore.

## Eigenstaendige Mods

- **Simple Livemap** `simplelivemap` [neu]: Live web map of the world as the players really explored it (visited chunks and held maps), with map relief, optional player positions and an optional built-in web server (off by default, local only, private with a key).
  - optional: Simple Maps
- **Simple Stats** `simplestats` [neu]: Every tool, weapon and piece of armor keeps its own statistics (blocks mined, kills by mob and variant, damage absorbed, distances, catches) with an item-specific statistics screen.
  - optional: SimpleBuilding
- **Wiring Example** `wiringexample` [neu]: Minimal module used to verify loader and integration wiring.
