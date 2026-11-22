# Mod-Struktur

Erzeugt aus `modules/modules.json` durch `python tools/modtree/modtree.py` (nicht von Hand bearbeiten). Grafische Fassung: `docs/MODSTRUKTUR.html`.

## Gemeinsame Basis

- **SimpleLib** `simplelib` [fertig]: Shared library of the Simple mods: common materials and upgrade templates, id migrations, config commands, UI building blocks, heat sources and warm food. Bundled by the Simple mods that need it.
  - optional: jei
  - required von: Simple Aesthetics, Simple Animals, Simple Arrows, Simple Beacons, Simple Blocks, Simple Building Tools, Simple Challenges, Simple Chess, Simple Chisels, Simple Crucibles, Simple Dimensions, Simple Dinosaurs, Simple Earth, Simple Enchanting, Simple End, Simple End Dragon Fight, Simple Farming, Simple Foundry, Simple Fun, Simple Gadgets, Simple Hammers, Simple Interfaces, Simple Layers, Simple Lights, Simple Livemap, Simple Loot, Simple Machines, Simple Maps, Simple Mob Manners, Simple Mobheads, Simple Mobs, Simple Money, Simple Movement, Simple Music, Simple Octets, Simple Opt Memory, Simple Opt Render, Simple Opt Server, Simple Optimization, Simple Ores, Simple Ovens, Simple Pads, Simple Questionary, Simple Resonance, Simple Respawn, Simple Riding, Simple Sandwiches, Simple Shops, Simple Slabs, Simple Sounds, Simple Storage, Simple Tools, Simple Trims, Simple Tweaks, Simple Vault, Simple Wanderers, Simple Weather, SimpleBuilding

## SimpleBuilding

- **SimpleBuilding** `simplebuilding` [fertig]: Building tools, storage, machines and enchantments for Minecraft; the super mod of the Simple family, bundling its required sub-mods.
  - required: cloth-config, SimpleLib, Simple Ovens, Simple Mobheads, Simple Crucibles, Simple Octets, Simple Blocks, Simple Hammers, Simple Storage, Simple Lights, Simple Gadgets, Simple Ores, Simple Machines, Simple Arrows, Simple Mobs, Simple Resonance, Simple Enchanting, Simple Pads, Simple Building Tools, Simple Tools, Simple Chisels, Simple Slabs
  - optional: jei, roughlyenoughitems, modmenu, jade, appleskin, mousetweaks, trinkets_updated, curios, Simple Maps, Simple Questionary, Simple Riding, Simple Sandwiches, Simple Tweaks, Simple Money, Simple Fun, Simple Dimensions, Simple Trims, Simple End, Simple Respawn, Simple Beacons, Simple Shops, Simple Wanderers, Simple Animals, Simple Music, Simple Earth, Simple Challenges, Simple Dinosaurs
  - **Simple Arrows** `simplearrows` (required) [neu]: Arrows: the Fletching Table (tip, shaft and fletching, an optional potion: 4 arrows with their own damage and flight), the Light Arrow, which lights the spot where it sticks until it despawns like a normal arrow, and the Silver Arrow, which hits undead harder (crafted with Simple Ores' silver). Works on its own.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores
  - **Simple Blocks** `simpleblocks` (required) [fertig]: Missing building blocks: Hushed Deepslate Tiles that dampen vibrations like wool, Sculk Deepslate, the missing Vanilla stairs, slabs and walls, cracked/mossy brick variants, suspended and levitating sand and gravel, dirt, grass, sand and gravel slabs, cracked and chiseled ice, the nautilus shell block and three more froglight colors. The Shapecutter cuts stone and wood into every stair, slab and shape. Works on its own with Vanilla.
    - required: SimpleLib
    - optional: SimpleBuilding, jei
  - **Simple Building Tools** `simplebuildingtools` (required) [neu]: Building tools of the SimpleBuilding family: Building Wands (copper to Enderite and creative), the Octant in 17 colors and the Blueprint with editor, picture import and cartography table copies. Simple Ores adds the Enderite wand, SimpleBuilding the backpack supply and the placed octants.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores, Simple Storage, Simple Hammers, Simple Octets
  - **Simple Chisels** `simplechisels` (required) [neu]: Seven chisels (Stone to Enderite) that move a block one step along its family - and back with sneak - with the enchantment Fast Chiseling; other mods add chains through data files.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores, jei
  - **Simple Enchanting** `simpleenchanting` (required) [neu]: Two enchanters above Vanilla's table: the Dark Enchanter (three random enchantments with sliders up to Vanilla's maximum, tier 50 with blazewood bookshelves and an obsidian floor) and the Astral Enchanter (tier 60 on Nihil Obsidian, Protection V and Feather Falling V). Made in the world with a hammer and eight nuggets. Works on its own; the Astral Enchanter needs Simple Ores.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores, Simple Hammers
  - **Simple Gadgets** `simplegadgets` (required) [fertig]: Gadgets you hold and use: the Gauge (speed dial, altimeter with fall-damage warning, auto-walk), the Transposer (turns and swaps blocks in place), the Resonance Rod, the Detector, the Attractor and the Echo Sounder; the Octant lives in Simple Building Tools.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Hammers** `simplehammers` (required) [fertig]: Seven sledgehammers (Stone to Enderite) that mine a 3x3 area, reshape blocks into stairs and slabs, carve octets, crush diamond and quartz blocks and upgrade blocks in the world strike by strike, with the enchantments Break Through, Override and Radius. While it is loaded the in-world ways of the other Simple mods take the hammer instead of the axe.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Octets, Simple Crucibles, Simple Ores, jei
  - **Simple Lights** `simplelights` (required) [fertig]: Light Panes: flat 1 px lamps on any block side - plain, transparent, sixteen colors, a redstone pane whose color follows the signal strength and copper panes that toggle like the Copper Bulb.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Machines** `simplemachines` (required) [neu]: The machine parts of the Simple Machines tab: Double Piston (one block, two stages), Signal Dampener (takes 0-3 off the signal), and the piston upgrades - Reinforced (Sticky) Piston (pushes 18, one paid unbreakable), Netherite and Enderite Piston (break the block in front, with durability, repaired with their nugget), Reinforced, Netherite and Enderite Hopper (2/4/8 times as fast, item filter) and their hopper minecarts, the Auto Smither (runs the smithing table on a redstone pulse) and the Auto Crafter (crafts by itself into a hopper below). Works on its own; the Enderite parts need Simple Ores.
    - required: SimpleLib
    - optional: jade, SimpleBuilding, Simple Ores, jei
  - **Simple Mobheads** `simplemobheads` (required) [fertig]: A head for every mob: dropped when a charged creeper's explosion kills it, worn for a small secret, placed, or played on a note block.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Mobs, Simple End, Simple End Dragon Fight, jei, modmenu, cloth_config
  - **Simple Mobs** `simplemobs` (required) [neu]: New mobs (Hollow Howler, Hollow Watcher, Shellker, Soulfire Blaze, Dread, Deceiver) and summoned bosses (Void Runner, Brimguard, the armored Blighted and Abyssal Wither). Weathered Trials with the Evil and Void Omen chain (trials, raids, vaults, keys), Wings tiers 1 to 4 with Nether Heart, Red Nether Gem, Ender Heart and Ender Gem, Spawner Fragments and the Makeshift Spawner, Strong Gunpowder, TNT and Rocket, the Trumpet, the Cloak of Deception and Trick Mirror, combat effects, armor trim abilities, the brood hollow with Mob Essence and the Potion of Silence. Requires Simple Resonance.
    - required: SimpleLib, Simple Resonance
  - **Simple Octets** `simpleoctets` (required) [fertig]: Octets: half-block cubes (0.5 x 0.5 x 0.5) of every wood, every block with stairs and slabs and the melon, which mix in one cell. Cut in the Shapecutter of Simple Blocks (in the stonecutter without it), crafted back into the block; the sledgehammer of Simple Hammers carves them and breaks one corner off a full block. Unwaxed copper octets age like Vanilla copper. Melon placing and corner breaking can be switched off (simpleoctets-server.json). Octets of SimpleBuilding's and Simple End's blocks only with that mod.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple End, Simple Ores, Simple Enchanting, jei
  - **Simple Ores** `simpleores` (required) [neu]: Silver ore in the Overworld and the End, lead (blocks that muffle sound and stop the Sculk), the silver mirror, and rose gold: two alloys of gold, copper and silver (hard Rose Copper, soft Rose Gold), pressed into pressings, melted in the blast furnace only, with tools and armor of their own. Also Enderite, the tier above netherite (other Simple mods show their Enderite variants only with it), Dimensional Scrap and Sage Ore. Works on its own with Vanilla.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Hammers
  - **Simple Ovens** `simpleovens` (required) [fertig]: Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces (2x/4x/8x speed, double experience from Netherite) and furnace minecarts. Upgrades in the world with the axe on its own, with the sledgehammer of Simple Hammers.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Foundry, Simple Crucibles, Simple Ores
    - **Simple Crucibles** `simplecrucibles` (optional) [fertig]: Iron, Reinforced, Netherite and Enderite crucibles (a cooking station without fuel; the barrels that dock onto them are Simple Storage's), the reinforced cauldron and village field kitchens. Upgrades in the world with the axe on its own, with the sledgehammer of Simple Hammers.
      - required: SimpleLib
      - optional: SimpleBuilding, jei, jade, Simple Storage, Simple Ores, Simple Tools
    - **Simple Foundry** `simplefoundry` (optional) [neu]: A multiblock foundry: build a tank of melting chambers in a brick hull, throw in ore and fuel, and pour the melt by layer through spouts into casting trays. Gold, copper and iron, one ingot per ingot. Works on its own with Vanilla; a sub-mod of Simple Ovens.
      - required: SimpleLib
      - optional: Simple Ovens
  - **Simple Pads** `simplepads` (required) [neu]: The pads and pressure plates of the Simple Pads tab: Launchpad, Elytra Pad, Flypad, Potion Pad, Spawn Teleporter and Chunk Loader in three tiers, the Diamond, Netherite, Enderite and copper pressure plates and the Spawn Elytra. Needs Simple Ores (Enderite) and Simple Mobheads (heads of the first tiers).
    - required: SimpleLib, Simple Ores, Simple Mobheads
    - optional: SimpleBuilding, jade
  - **Simple Resonance** `simpleresonance` (required) [neu]: How experienced a player is and how dangerous the land is: a player value from playing time and survival, danger zones that grow softly with the distance from the spawn, the owner of a monster spawn and a newcomer protection. An API for other mods; allows Simple Mobs to give new monsters status effects instead of more health.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Mobs
  - **Simple Slabs** `simpleslabs` (required) [neu]: Half slabs (4 pixels) of every block with a slab: stack them freely in the four 4-pixel layers of a cell, lean them against a wall as side half slab or side slab. Sand, red sand and gravel half slabs fall, grass takes the biome color, copper ages like Vanilla copper. Cut in the Shapecutter of Simple Blocks (in the stonecutter without it), crafted back into the slab. Half slabs of another mod's block only with that mod; shares SimpleLib's shape core with Simple Octets.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Blocks, Simple End, Simple Ores, Simple Enchanting, jei
  - **Simple Storage** `simplestorage` (required) [fertig]: Crates for every wood (open food boxes for nine stacks, hoppers and comparators work), the Storage Crafting Table that keeps its grid, tiered chests, trapped chests and tier shulker boxes with more slots and bigger stacks, chest minecarts and chest boats of the chest tiers, bundles and quivers in three tiers with the storage enchantments Drawer, Deep Pockets and Funnel, bundles set down on a block, the four backpacks (worn or placed) and four barrels from copper to Enderite that dock onto the crucibles of Simple Crucibles.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple End, trinkets_updated, curios, Simple Ores, Simple Crucibles
  - **Simple Animals** `simpleanimals` (optional) [neu]: Five animals Vanilla does not have: the shy deer, the wild boar that defends its piglets, the hedgehog that hunts silverfish and rolls up in a flash, the rare sideways-walking crab and the zebra, a striped horse whose cross with a horse is the infertile zorse. Works on its own with Vanilla.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Money, Simple Mobheads
  - **Simple Beacons** `simplebeacons` (optional) [neu]: Strong Beacon (5 pyramid levels, new powers, range by pyramid material, Vanilla-style screen), Mighty and Evil beacons, Strong Conduit (7x7x7 frame, up to 216 blocks under water, Dolphin's Grace) and its Mighty/Evil kinds, upgraded with SimpleLib's Strong Upgrade template. Works on its own with the axe; a hammer mod adds the sledgehammer.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Ores, Simple Mobs
  - **Simple Challenges** `simplechallenges` (optional) [neu]: Game modes for a world of your choice: unlock the world cell by cell (chunks, blocks or three chunks), paid with items, credits or levels. Without a chosen challenge the mod changes nothing.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Chess** `simplechess` (optional) [neu]: Chess: 156 pieces (13 colors, six kinds, 3D and flat) that stand up to four on a block, 12 quartz checker boards with stairs and slabs and, with Simple Octets, the chess octets.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Octets
  - **Simple Dimensions** `simpledimensions` (optional) [neu]: Configurable dimensions, light arches, and safe linked portals.
    - required: SimpleLib
    - optional: SimpleBuilding, modmenu, cloth_config
  - **Simple Dinosaurs** `simpledinos` (optional) [neu]: Fossil block, dinosaur eggs you brush free and brood by sitting on them, and the first dinosaur: a rideable T. rex that hatches as a hatchling and grows up. Works on its own with Vanilla.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Money, Simple Mobheads
  - **Simple Earth** `simpleearth` (optional) [neu]: The Overworld seen from orbit: from the render height (default 1536) the world folds into a cube (27 display versions, standard V25) whose faces show Vanilla map colors read from the world generator - no chunks generated. Flat sun, cube moon, stars; below everything stays Vanilla.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple End** `simpleend` (optional) [neu]: End wood (Ashwood, Duskwood) with buds, trees and stick scrub, End grass, the Ash Valley and Dawnlands biomes on the outer End islands; bundles Simple End Dragon Fight.
    - required: SimpleLib
    - optional: Simple End Dragon Fight
    - **Simple End Dragon Fight** `simpleenddragonfight` (optional) [neu]: Ender Dragon fight: +1 health bar per player on the island, a faster dragon, entry animations and Dragonmites from half health.
      - required: SimpleLib
      - optional: Simple Mobs
  - **Simple Fun** `simplefun` (optional) [fertig]: Bounded playful mechanics, throwable bricks and cosmetic transformations (the animal heads moved to Simple Mobheads in 1.4.0).
    - required: SimpleLib
    - optional: modmenu, jei, jade
  - **Simple Layers** `simplelayers` (optional) [neu]: World layers: the Cloud Sea, a sky dimension above y 700 of the Overworld, and the Backrooms - their own dimension of ten stacked levels, starting with endless yellow rooms with wallpaper, damp carpet and neon lights.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Maps** `simplemaps` (optional) [neu]: Wayfinder maps: endless maps per dimension with zoom, waypoints in the locator bar, framed views and cartography table upgrades.
    - required: SimpleLib
    - optional: Simple Money, Simple Ores
  - **Simple Money** `simplemoney` (optional) [fertig]: Currency production, weighted villager trades and treasure loot.
    - required: SimpleLib
    - optional: jei, modmenu, SimpleBuilding, Simple Fun, Simple Models, Simple Quality of Life, Simple Riding, Simple Sandwiches, Simple Sounds, Simple Tweaks, Simple Visuals, Simple Ores
  - **Simple Music** `simplemusic` (optional) [neu]: Sheet music as a text note code (editor with test listening, .txt/.nbs import), pressed onto blank discs in the Disc Press, and uploaded mono Ogg discs with quota and OP approval, played in the Vanilla jukebox and the Pocket Jukebox.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Mobheads
  - **Simple Questionary** `simplequestionary` (optional) [neu]: The Questionary Table: put any item in and read its lore, its use and a secret in an open book.
    - required: SimpleLib
  - **Simple Respawn** `simplerespawn` (optional) [neu]: Downed instead of dead: a teammate can revive you within the bleed-out time (then 3 hearts, no food); own downed screen with spectate, inventory view and give up.
    - required: SimpleLib
  - **Simple Riding** `simpleriding` (optional) [fertig]: Saddle speed, horse jumps, nautilus dashes, mount armor utilities, horseshoes, loot, and trades.
    - required: SimpleLib
    - optional: SimpleBuilding, modmenu, Simple Ores
  - **Simple Sandwiches** `simplesandwiches` (optional) [neu]: Sandwiches built on a cutting board, a kitchen knife, cheese and butter from the cauldron, and eating straight out of bundles.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple End, Simple Crucibles, Simple Ores, Simple Animals
  - **Simple Shops** `simpleshops` (optional) [neu]: Player shops: a Sales Block with a price (up to five exact price items) and a Display Case with up to five goods, one piece per trade; Sales Storage, Trade Terminal and shop music. Protected from strangers, explosions, pistons and hoppers; no claim right needed to buy.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Money, Simple Tweaks
  - **Simple Trims** `simpletrims` (optional) [neu]: Glowing, Emitting, Pulsating and Inverting armor trim templates, placeable trim templates and trim upgrades. Works on its own with the axe; Simple Hammers adds the sledgehammer.
    - required: SimpleLib
    - optional: SimpleBuilding, jei
  - **Simple Tweaks** `simpletweaks` (optional) [fertig]: Legacy compatibility, server-authoritative claims, admin tools (block log, backups, anti-X-ray) and player and protection tweaks, on by default.
    - required: SimpleLib
    - optional: Simple Dimensions, SimpleBuilding, Simple Money, Simple Weather, Simple Maps
  - **Simple Vault** `simplevault` (optional) [neu]: The Bedrock Vault (Grundgewölbe): a rare gap in the Overworld bedrock leads into quiet halls below it, enclosed by bedrock; three riddles and the Ascent Altar are the only way out.
    - required: SimpleLib
    - optional: SimpleBuilding
  - **Simple Wanderers** `simplewanderers` (optional) [neu]: One Village Wanderer per village: a Wandering Trader in an ochre travel cloak with a leashed horse. Offers common-to-uncommon goods, a fresh stock every 3 days, a restock every 1.5 days, and a range that follows the village biome. Works on its own with Vanilla.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Money, Simple Mobheads

## Simple Quality of Life

- **Simple Quality of Life** `simplequalityoflife` [fertig]: Super-mod: bundles Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools and Simple Movement with one switch each; guide book and start quest.
  - required: Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools, Simple Movement
  - optional: SimpleBuilding, modmenu
  - **Simple Farming** `simplefarming` (required) [fertig]: Feather Falling protects farmland, Sharpness cuts grass, a hoe harvests and replants ripe crops; a sub-mod of Simple QoL, also playable alone.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Quality of Life, modmenu
  - **Simple Interfaces** `simpleinterfaces` (required) [fertig]: Restyle of the Vanilla container screens (a colored box per block, drawn in code), linked containers and Easy Shulkers / Ender Chests; a sub-mod of Simple QoL, also playable alone.
    - required: SimpleLib
    - optional: SimpleBuilding, modmenu
  - **Simple Loot** `simpleloot` (required) [fertig]: Vault cooldown in days instead of once per player; a sub-mod of Simple QoL, also playable alone.
    - required: SimpleLib
    - optional: Simple Quality of Life, modmenu
  - **Simple Mob Manners** `simplemobmanners` (required) [fertig]: Mute mobs by type or name tag, keep named babies young, piglins ignore gold trims and tools; a sub-mod of Simple QoL, also playable alone.
    - required: SimpleLib
    - optional: Simple Quality of Life, modmenu
  - **Simple Movement** `simplemovement` (required) [fertig]: Manual crawling, server-allowed autowalk, faster ladders (Vanilla 0.2, default 0.4) and Frost Walker on powder snow, plus the Step Height (I-II) and Sprint (I-III) treasure enchantments and a Jump Boost step-up; a sub-mod of Simple QoL, also playable alone.
    - required: SimpleLib
    - optional: Simple Quality of Life, modmenu
  - **Simple Tools** `simpletools` (required) [fertig]: Full-durability bonus, the Thrift and Soulbound enchantments, the Death Vault, the Biome Brush, anvil repairs without extra cost and lava buckets into furnaces; a sub-mod of Simple QoL, also playable alone.
    - required: SimpleLib
    - optional: SimpleBuilding, Simple Quality of Life, modmenu
  - **Simple Weather** `simpleweather` (required) [fertig]: Weather switch (server) and rain density (client); a sub-mod of Simple QoL, also playable alone.
    - required: SimpleLib
    - optional: Simple Quality of Life, modmenu

## Simple Aesthetics

- **Simple Aesthetics** `simpleaesthetics` [fertig]: Super-mod: bundles Simple Visuals, Simple Sounds and Simple Models, with one on/off switch each.
  - required: SimpleLib, Simple Visuals, Simple Sounds, Simple Models
  - **Simple Models** `simplemodels` (required) [fertig]: Server-approved custom item models, anvil assignment, and a searchable model browser.
    - optional: SimpleBuilding, modmenu
  - **Simple Sounds** `simplesounds` (required) [fertig]: Bounded local Vanilla sound counterparts for the twelve Simple Visuals immersion effects.
    - required: SimpleLib
    - optional: Simple Visuals, SimpleBuilding, modmenu
  - **Simple Visuals** `simplevisuals` (required) [fertig]: Bounded client HUD, tooltips, anvil formatting, and Vanilla immersion particles.
    - optional: SimpleBuilding, Simple Models, modmenu

## Simple Optimization

- **Simple Optimization** `simpleoptimization` [fertig]: Super-mod: bundles Simple Opt Render, Simple Opt Memory and Simple Opt Server, with one on/off switch each.
  - required: SimpleLib, Simple Opt Render, Simple Opt Memory, Simple Opt Server
  - **Simple Opt Memory** `simpleoptmemory` (required) [fertig]: Memory optimizations of Simple Optimization: block states share their read-only property and face tables (steps back for FerriteCore); model baking later.
    - required: SimpleLib
  - **Simple Opt Render** `simpleoptrender` (required) [fertig]: Client optimizations of Simple Optimization: leaf culling, light-map cache, more culling (sign text, item frames, beacon beams), fast IP ping, telemetry off, optional detail switches (particles, texture animations, air fog). Each area steps back when a known optimization mod covers it.
    - required: SimpleLib
  - **Simple Opt Server** `simpleoptserver` (required) [fertig]: Server optimizations of Simple Optimization: view distance follows server load (MSPT) within the server.properties bounds; optional activation range for far mobs (off by default); steps back when ServerCore is installed.
    - required: SimpleLib

## Eigenstaendige Mods

- **Simple Livemap** `simplelivemap` [fertig]: Live web map of the world as the players really explored it (visited chunks and held maps): top-down tiles with map relief, player positions on request, built-in web server (on out of the box, only on this computer, private with a key; own website possible).
  - required: SimpleLib
  - optional: Simple Maps
- **Simple Stats** `simplestats` [fertig]: Every tool, weapon and piece of armor keeps its own statistics: blocks mined, kills by mob and variant, damage absorbed, distances, catches and more, with an item-specific statistics screen.
  - optional: SimpleBuilding
- **Wiring Example** `wiringexample` [neu]: Minimal module used to verify loader and integration wiring.

## Release-Planung

Mods mit `releasePhase` in modules.json: gebaut, aber erst nach dem Release (published) freigegeben; bis dahin nicht in SimpleBuildings `bundleOptional`.

- Phase 3: **Simple Chess** `simplechess`
- Phase 3: **Simple Layers** `simplelayers`

## Nicht im Release-Paket

Mods mit `"release": false` (oder `example`) in modules.json: Code bleibt, wird gebaut und getestet, aber nicht veröffentlicht (keine Store-Seite, kein Download, keine Landing-Kachel).

- **Simple Chess** `simplechess`
- **Simple Layers** `simplelayers`
- **Simple Vault** `simplevault`
- **Wiring Example** `wiringexample`
