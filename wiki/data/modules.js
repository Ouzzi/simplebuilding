window.WIKI_MODULES = [
  {
    "id": "simplebuilding",
    "displayName": "SimpleBuilding",
    "description": "Building tools, storage, machines, and enchantments.",
    "version": "1.3.1",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth-config",
      "simpleovens",
      "simplemobheads",
      "simplecrucibles",
      "simpleoctets",
      "simpleblocks",
      "simplehammers",
      "simplestorage"
    ],
    "optional": [
      "jei",
      "roughlyenoughitems",
      "modmenu",
      "jade",
      "appleskin",
      "mousetweaks",
      "trinkets_updated",
      "curios",
      "simplemaps",
      "simplequestionary",
      "simpleriding",
      "simplesandwiches",
      "simpletweaks",
      "simplemoney",
      "simplefun",
      "simpledimensions",
      "simpletrims",
      "simplemobs",
      "simpleend",
      "simplerespawn",
      "simplebeacons",
      "simpleshops",
      "simplewanderers"
    ],
    "overview": {
      "status": "in Arbeit",
      "open": "86",
      "missing": "Modulfamilie-Umzug läuft: Blocks, Tiegel, polierter Endstein, Octets fertig; Hämmer in wave1 (noch nicht im master, check rot); offen Gadgets, Lager (Stufe 8), Enderite, Release (Stufe 9); dazu Abnahmen im Spiel.",
      "asOf": "19.10.2026"
    },
    "counts": {
      "items": 542,
      "blocks": 318,
      "recipes": 858,
      "config": 239,
      "features": 48
    }
  },
  {
    "id": "wiringexample",
    "displayName": "Wiring Example",
    "description": "Minimal module used to verify loader and integration wiring.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "counts": {
      "items": 1,
      "features": 1
    },
    "dataHash": "b1da0996bb98"
  },
  {
    "id": "simplemoney",
    "displayName": "Simple Money",
    "description": "Currency production, weighted villager trades and treasure loot.",
    "version": "1.2.16",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "jei",
      "modmenu",
      "simplebuilding",
      "simplefun",
      "simplemodels",
      "simplequalityoflife",
      "simpleriding",
      "simplesandwiches",
      "simplesounds",
      "simpletweaks",
      "simplevisuals",
      "wiringexample"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 8,
      "recipes": 9,
      "features": 73
    },
    "dataHash": "0eb60ca09c8f"
  },
  {
    "id": "simpleriding",
    "displayName": "Simple Riding",
    "description": "Saddle speed, horse jumps, nautilus dashes, mount armor utilities, loot, and trades.",
    "version": "1.0.5",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 8,
      "recipes": 8,
      "features": 35
    },
    "dataHash": "f5b68cb55fb7"
  },
  {
    "id": "simplemodels",
    "displayName": "Simple Models",
    "description": "Server-approved custom item models, anvil assignment, and a searchable model browser.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simpleaesthetics",
    "bundle": "required",
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 14
    },
    "dataHash": "5173fcf52c53"
  },
  {
    "id": "simplefun",
    "displayName": "Simple Fun",
    "description": "Bounded playful mechanics, throwable bricks and cosmetic transformations (the animal heads moved to Simple Mobheads in 1.4.0).",
    "version": "1.4.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "modmenu",
      "jei",
      "jade"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 2,
      "recipes": 2,
      "features": 38
    },
    "dataHash": "883701b8f26c"
  },
  {
    "id": "simplevisuals",
    "displayName": "Simple Visuals",
    "description": "Bounded client HUD, tooltips, anvil formatting, and Vanilla immersion particles.",
    "version": "1.0.7",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplemodels",
      "modmenu"
    ],
    "parent": "simpleaesthetics",
    "bundle": "required",
    "counts": {
      "features": 77
    },
    "dataHash": "01baf1f68bef"
  },
  {
    "id": "simplesounds",
    "displayName": "Simple Sounds",
    "description": "Bounded local Vanilla sound counterparts for the twelve Simple Visuals immersion effects.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplevisuals",
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simpleaesthetics",
    "bundle": "required",
    "counts": {
      "features": 20
    },
    "dataHash": "033c5483dd22"
  },
  {
    "id": "simplequalityoflife",
    "displayName": "Simple Quality of Life",
    "description": "Super-mod: bundles Simple Weather, Simple Interfaces, Simple Loot, Simple Mob Manners, Simple Farming, Simple Tools and Simple Movement with one switch each; guide book and start quest.",
    "version": "1.0.6",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config",
      "simpleweather",
      "simpleinterfaces",
      "simpleloot",
      "simplemobmanners",
      "simplefarming",
      "simpletools",
      "simplemovement"
    ],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "overview": {
      "status": "in Arbeit",
      "open": "16",
      "missing": "Grundstock fertig; neu Item-Rahmen (in wave1) und Container-Tooltips (auf Branch), Bündel-Ansicht geplant; Abnahmen im Spiel offen.",
      "asOf": "19.10.2026"
    },
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 9
    },
    "dataHash": "9064acea586d"
  },
  {
    "id": "simpletweaks",
    "displayName": "Simple Tweaks",
    "description": "Legacy compatibility and server-authoritative claims, on by default.",
    "version": "1.2.12",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simpledimensions",
      "simplebuilding",
      "simplemoney",
      "simpleweather"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 2,
      "blocks": 1,
      "recipes": 4,
      "features": 39
    },
    "dataHash": "8f7275ae7f45"
  },
  {
    "id": "simpledimensions",
    "displayName": "Simple Dimensions",
    "description": "Configurable dimensions, light arches, and safe linked portals.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "modmenu",
      "flan",
      "ftbchunks",
      "openpartiesandclaims",
      "griefdefender",
      "claimchunk"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 1,
      "blocks": 2,
      "recipes": 1,
      "features": 7
    },
    "dataHash": "4d2fff5f6bf4"
  },
  {
    "id": "simplesandwiches",
    "displayName": "Simple Sandwiches",
    "description": "Sandwiches on a cutting board, a kitchen knife, cheese and butter from the cauldron, and eating straight out of bundles.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding",
      "simpleend"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 6,
      "blocks": 18,
      "recipes": 17,
      "features": 6
    },
    "dataHash": "fc7348a0ab85"
  },
  {
    "id": "simplelib",
    "displayName": "SimpleLib",
    "description": "Shared content bundled by SimpleBuilding, Simple Crucibles and Simple Sandwiches: warm food, shared materials, id moves, in-world upgrades and UI building blocks.",
    "version": "0.2.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "overview": {
      "status": "fertig (wächst mit)",
      "open": "4",
      "missing": "Basis für alle Mods; offen Kreativtab-Zeilenlayout-Abnahme und Stil-Bausteine, wächst mit jeder Familienstufe.",
      "asOf": "19.10.2026"
    },
    "counts": {
      "items": 10,
      "blocks": 7,
      "recipes": 29,
      "features": 2
    },
    "dataHash": "315e187b9f71"
  },
  {
    "id": "simpleinterfaces",
    "displayName": "Simple Interfaces",
    "description": "Restyle of the Vanilla container screens (a coloured box per block, drawn in code), linked containers and Easy Shulkers / Ender Chests; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config",
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "counts": {
      "features": 43
    },
    "dataHash": "9e46375155db"
  },
  {
    "id": "simplemaps",
    "displayName": "Simple Maps",
    "description": "Wayfinder maps: endless maps per dimension with zoom, waypoints in the locator bar, framed views and cartography table upgrades.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplemoney"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 4,
      "recipes": 4,
      "features": 7
    },
    "dataHash": "4573d7263297"
  },
  {
    "id": "simpletrims",
    "displayName": "Simple Trims",
    "description": "Glowing, Emitting and Pulsating armor trim templates, placeable trim templates and trim upgrades (moved from SimpleBuilding, ids migrate). Axe on its own, sledgehammer with Simple Hammers.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 4,
      "blocks": 1,
      "recipes": 8,
      "features": 6
    },
    "dataHash": "722f6419ce54"
  },
  {
    "id": "simplemobs",
    "displayName": "Simple Mobs",
    "description": "Hollow Howler and Hollow Watcher bosses, Shellker, Soulfire Blaze, Dread and Deceiver mobs, Blighted and Abyssal Wither with Nether Heart and Nether Gem, and the Potion of Silence.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 20,
      "blocks": 2,
      "recipes": 62,
      "features": 12
    },
    "dataHash": "af33db89594f"
  },
  {
    "id": "simpleweather",
    "displayName": "Simple Weather",
    "description": "Weather switch (server) and rain density (client); a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "counts": {
      "features": 3
    },
    "dataHash": "bc9c18e432a3"
  },
  {
    "id": "simplemovement",
    "displayName": "Simple Movement",
    "description": "Manual crawling, server-allowed autowalk, faster ladders and Frost Walker on powder snow; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "counts": {
      "features": 17
    },
    "dataHash": "63a7f7372029"
  },
  {
    "id": "simpletools",
    "displayName": "Simple Tools",
    "description": "Full-durability bonus, the Thrift and Soulbound enchantments, the Death Vault, the Biome Brush, anvil repairs without extra cost and lava buckets into furnaces; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "counts": {
      "items": 2,
      "blocks": 1,
      "recipes": 4,
      "features": 32
    },
    "dataHash": "b06978c33f7e"
  },
  {
    "id": "simplefarming",
    "displayName": "Simple Farming",
    "description": "Feather Falling protects farmland, Sharpness cuts grass, a hoe harvests and replants ripe crops; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "counts": {
      "features": 8
    },
    "dataHash": "c881b63a533d"
  },
  {
    "id": "simplemobmanners",
    "displayName": "Simple Mob Manners",
    "description": "Mute mobs by type or name tag, keep named babies young, piglins ignore gold trims and tools; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "counts": {
      "items": 1,
      "features": 11
    },
    "dataHash": "564d3430f293"
  },
  {
    "id": "simpleloot",
    "displayName": "Simple Loot",
    "description": "Vault cooldown in days instead of once per player; a sub-mod of Simple QoL, also playable alone.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "cloth_config"
    ],
    "optional": [
      "simplequalityoflife",
      "modmenu"
    ],
    "parent": "simplequalityoflife",
    "bundle": "required",
    "counts": {
      "features": 5
    },
    "dataHash": "13210b0e0ad4"
  },
  {
    "id": "simplequestionary",
    "displayName": "Simple Questionary",
    "description": "The Questionary Table: put any item in and read its lore, its use and a secret in an open book.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "blocks": 1,
      "recipes": 1,
      "features": 2
    },
    "dataHash": "6e84609aa395"
  },
  {
    "id": "simpleaesthetics",
    "displayName": "Simple Aesthetics",
    "description": "Super-mod without content of its own: bundles Simple Visuals, Simple Sounds and Simple Models with one on/off switch each.",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplevisuals",
      "simplesounds",
      "simplemodels"
    ],
    "optional": [],
    "overview": {
      "status": "fertig (Abnahme offen)",
      "open": "6",
      "missing": "Funktionen fertig; offen nur Abnahme im Spiel (Modelle-UI, Stufen/Stärke der Effekte und Sounds) und Stil-Audit-Reste.",
      "asOf": "19.10.2026"
    },
    "counts": {
      "features": 1
    },
    "dataHash": "b443b496e486"
  },
  {
    "id": "simpleenddragonfight",
    "displayName": "Simple End Dragon Fight",
    "description": "Ender Dragon fight: +1 health bar per player on the island, a faster dragon, entry animations and Dragonmites from half health.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleend",
    "bundle": "optional",
    "counts": {
      "items": 3,
      "recipes": 1,
      "features": 6
    },
    "dataHash": "5c8bd4f16cf9"
  },
  {
    "id": "simpleend",
    "displayName": "Simple End",
    "description": "End wood (Ashwood, Duskwood) with buds, trees and stick scrub, End grass, the Ash Valley and Dawnlands biomes on the outer End islands; bundles Simple End Dragon Fight.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simpleenddragonfight"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "overview": {
      "status": "fertig (Abnahme offen)",
      "open": "4",
      "missing": "Funktionen fertig; offen Client-Sicht im Spiel, Stil-Audit-Reste und die Nebelwand-Frage (Lohen-Seite).",
      "asOf": "19.10.2026"
    },
    "counts": {
      "items": 8,
      "blocks": 47,
      "recipes": 35,
      "features": 8
    },
    "dataHash": "8743da5d21e0"
  },
  {
    "id": "simplerespawn",
    "displayName": "Simple Respawn",
    "description": "Downed instead of dead: a teammate can revive you within the bleed-out time (then 3 hearts, no food); own downed screen with spectate, inventory view and give up.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 6
    },
    "dataHash": "0060d25a18d6"
  },
  {
    "id": "simplebeacons",
    "displayName": "Simple Beacons",
    "description": "Strong Beacon (6 pyramid levels, 6 more effects, two-column screen), Strong Conduit (7x7x7 frame, up to 144 blocks under water, Dolphin's Grace) and the Strong Upgrade template (nether star). Axe on its own, sledgehammer with a hammer mod.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 3,
      "blocks": 2,
      "recipes": 6,
      "features": 7
    },
    "dataHash": "405f79eb15ce"
  },
  {
    "id": "simpleovens",
    "displayName": "Simple Ovens",
    "description": "Reinforced, Netherite and Enderite furnaces, smokers and blast furnaces (2x/4x/8x speed, double experience from Netherite) and furnace minecarts, moved from SimpleBuilding (ids migrate). In-world upgrades with the axe on its own, with the sledgehammer of Simple Hammers.",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "counts": {
      "items": 4,
      "blocks": 9,
      "recipes": 9,
      "features": 7
    },
    "dataHash": "e7d902bed624"
  },
  {
    "id": "simplemobheads",
    "displayName": "Simple Mobheads",
    "description": "A head for every mob: dropped when a charged creeper's explosion kills it, worn for a small secret, placed, or played on a note block. Required sub-mod of SimpleBuilding; Simple Fun's and SimpleBuilding's heads moved in.",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib",
      "cloth_config"
    ],
    "optional": [
      "simplebuilding",
      "simplemobs",
      "simpleend",
      "simpleenddragonfight",
      "jei",
      "modmenu"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "counts": {
      "items": 1,
      "blocks": 188,
      "recipes": 1,
      "features": 12
    },
    "dataHash": "0806bcdef644"
  },
  {
    "id": "simpleshops",
    "displayName": "Simple Shops",
    "description": "Player shops: a Sales Block with a price (up to five exact price items) and a Display Case with up to five goods, one piece per trade. Protected from strangers, explosions, pistons and hoppers; no claim right needed to buy.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simplemoney",
      "simpletweaks"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 1,
      "blocks": 5,
      "recipes": 6,
      "features": 10
    },
    "dataHash": "7c13ab3a1cc5"
  },
  {
    "id": "simplewanderers",
    "displayName": "Simple Wanderers",
    "description": "Village Wanderer: one Wandering Trader per village with a leashed horse, goods split from the traveling trader, a new stock every 3 days, a restock every 1.5 days and a range by village biome. Works on its own with Vanilla.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding",
      "simplemoney",
      "simplemobheads"
    ],
    "parent": "simplebuilding",
    "bundle": "optional",
    "counts": {
      "items": 2,
      "recipes": 1,
      "features": 6
    },
    "dataHash": "34ebe8a6188b"
  },
  {
    "id": "simplestats",
    "displayName": "Simple Stats",
    "description": "Every tool, weapon and piece of armor keeps its own statistics (blocks mined, kills by mob and variant, damage absorbed, distances, catches) with an item-specific statistics screen.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [],
    "optional": [
      "simplebuilding"
    ],
    "counts": {
      "items": 1,
      "recipes": 1,
      "features": 10
    },
    "dataHash": "70d5c51abaf6"
  },
  {
    "id": "simpleblocks",
    "displayName": "Simple Blocks",
    "description": "Missing building blocks: Hushed Deepslate Tiles (dampen vibrations like wool), Sculk Deepslate, the missing Vanilla stairs, slabs and walls and cracked/mossy brick variants, and SimpleBuilding's building blocks without a home of their own (suspended and levitating sand/gravel, dirt/grass/sand/gravel slabs, cracked and chiseled ice, the nautilus shell block, three froglight colors; ids migrate). The Shaping Saw cuts stone and wood into every stair, slab and shape. Works on its own with Vanilla; required sub-mod of SimpleBuilding.",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "counts": {
      "items": 1,
      "blocks": 53,
      "recipes": 282,
      "features": 8
    },
    "dataHash": "b95cfeeec4d6"
  },
  {
    "id": "simplecrucibles",
    "displayName": "Simple Crucibles",
    "description": "Iron, Reinforced, Netherite and Enderite crucibles and barrels (a cooking station without fuel), the reinforced cauldron and village field kitchens, moved from SimpleLib and SimpleBuilding (ids migrate). In-world upgrades with the axe on its own, with the sledgehammer of Simple Hammers.",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "jei",
      "jade"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "counts": {
      "items": 27,
      "blocks": 11,
      "recipes": 6,
      "features": 7
    },
    "dataHash": "2fa7157b41ba"
  },
  {
    "id": "simpleoctets",
    "displayName": "Simple Octets",
    "description": "Octets: half-block cubes (0.5 x 0.5 x 0.5) of every wood, every block with stairs and slabs and the melon, which mix in one cell, moved from SimpleBuilding (ids migrate). Cut in the stonecutter, crafted back into the block; the sledgehammer of Simple Hammers carves them. Octets of SimpleBuilding's and Simple End's blocks only with that mod.",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simpleend",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "counts": {
      "items": 1,
      "blocks": 176,
      "recipes": 521,
      "features": 6
    },
    "dataHash": "c1e7c62e3e26"
  },
  {
    "id": "simplehammers",
    "displayName": "Simple Hammers",
    "description": "Seven sledgehammers (Stone to Enderite) that mine a 3x3 area, reshape blocks into stairs and slabs, carve octets, crush diamond and quartz blocks and upgrade blocks in the world strike by strike, with the enchantments Break Through, Override and Radius, moved from SimpleBuilding (ids migrate). While it is loaded the in-world ways of the other Simple mods take the hammer instead of the axe.",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simpleoctets",
      "simplecrucibles",
      "jei"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "counts": {
      "items": 8,
      "recipes": 11,
      "features": 9
    },
    "dataHash": "f0a1c9263c15"
  },
  {
    "id": "simpleoptimization",
    "displayName": "Simple Optimization",
    "description": "Super-mod without content of its own: bundles Simple Opt Render, Simple Opt Memory and Simple Opt Server with one on/off switch each.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [
      "simpleoptrender",
      "simpleoptmemory",
      "simpleoptserver"
    ],
    "optional": [],
    "overview": {
      "status": "in Arbeit",
      "open": "13",
      "missing": "Stufe 1 + Teile von Stufe 2 nur auf claude-wave1 und nur Fabric; offen Kompat-Ziel, 2. Messreihe, Benchmark beim Besitzer (Stufe 3), Stufen 4/5.",
      "asOf": "19.10.2026"
    },
    "counts": {
      "features": 1
    },
    "dataHash": "c6d6d341c731"
  },
  {
    "id": "simpleoptrender",
    "displayName": "Simple Opt Render",
    "description": "Client optimizations (leaf culling, light-map cache, fast IP ping, telemetry off); each area steps back when a known optimization mod covers it.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleoptimization",
    "bundle": "required",
    "counts": {
      "features": 7
    },
    "dataHash": "b2d1f93d90d8"
  },
  {
    "id": "simpleoptmemory",
    "displayName": "Simple Opt Memory",
    "description": "Memory optimizations (block states, models); stage 1 only holds the switch, config and mod detection.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleoptimization",
    "bundle": "required",
    "counts": {
      "features": 2
    },
    "dataHash": "6f05fd404d00"
  },
  {
    "id": "simpleoptserver",
    "displayName": "Simple Opt Server",
    "description": "Server optimizations: view distance follows server load (MSPT) within server.properties bounds; steps back for ServerCore.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric"
    ],
    "requires": [],
    "optional": [],
    "parent": "simpleoptimization",
    "bundle": "required",
    "counts": {
      "features": 2
    },
    "dataHash": "c718c2a98ad5"
  },
  {
    "id": "simplestorage",
    "displayName": "Simple Storage",
    "description": "Crates (open wooden food boxes for every wood, hoppers and comparators work), the Storage Crafting Table (a crafting table that keeps its grid), tiered and trapped chests, trapped copper chests and tier shulker boxes (more slots, bigger stacks, upgraded in the world keeping their contents), moved from SimpleBuilding (ids migrate). Chest carts and chest boats, bundles, quivers, the placed bundle and the storage enchantments Drawer, Deep Pockets and Funnel (8c-1) also moved, then the four backpacks (8c-2).",
    "version": "1.0.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simpleend",
      "trinkets_updated",
      "curios"
    ],
    "parent": "simplebuilding",
    "bundle": "required",
    "counts": {
      "items": 14,
      "blocks": 37,
      "recipes": 174,
      "features": 6
    },
    "dataHash": "579492a35aea"
  },
  {
    "id": "simplemusic",
    "displayName": "Simple Music",
    "description": "Sheet music with a text note code (editor with test listening, .txt/.nbs import), the Orchestrion (note block + jukebox: redstone loop, chest above = next sheet, comparator progress) and uploaded mono Ogg discs (quota, OP approval). Needs SimpleLib; SimpleBuilding optional.",
    "version": "0.1.0",
    "minecraft": "26.3",
    "loaders": [
      "fabric",
      "neoforge",
      "forge"
    ],
    "requires": [
      "simplelib"
    ],
    "optional": [
      "simplebuilding",
      "simplemobheads"
    ],
    "counts": {
      "items": 21,
      "blocks": 2,
      "recipes": 38,
      "features": 16
    },
    "dataHash": "787e9d90d89c"
  }
];
