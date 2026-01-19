# 🏗️ SimpleBuilding: Construction Evolved
**(Mod ID: `simplebuilding`)**

![Version](https://img.shields.io/badge/Version-1.21-green) ![Loader](https://img.shields.io/badge/Loader-Fabric-blue)

**SimpleBuilding** expands Minecraft's construction capabilities while maintaining a vanilla-friendly feel. It introduces powerful tools, new mechanics, and quality-of-life improvements designed for large-scale building and excavation projects. Whether you're clearing a mountain, building a massive wall, or decorating your base, SimpleBuilding provides the right tool for the job.

## ✨ Core Features
* **Heavy Excavation:** Sledgehammers clear 3x3 areas or more.
* **Rapid Construction:** Building Wands place blocks in grids, lines, or complex shapes.
* **In-World Editing:** Chisels and Spatulas transform blocks without a UI.
* **Utility Items:** Magnets, Rangefinders (Octant), Velocity Gauges, and Quivers.
* **Advanced Storage:** High-capacity Bundles and faster processing blocks.
* **Enchantment Suite:** Over 15 unique enchantments tailored for building and mining.

---

## 🛠️ Tools & Equipment

### 🪄 Building Wands
The essential tool for builders. Extend a face of blocks instantly using materials from your inventory.
* **Usage:** Right-click on a block face to extend it. Sneak + Scroll to change modes (if enchanted).
* **Tiers:**
    * **Copper:** 3x3 Grid (Max 9 blocks)
    * **Iron:** 5x5 Grid (Max 25 blocks)
    * **Gold / Diamond:** 7x7 Grid (Max 49 blocks)
    * **Netherite:** 9x9 Grid (Max 81 blocks)
* **Key Enchantments:** `Linear` (Line placement), `Cover` (Surface placement), `Bridge` (Air placement), `Master Builder` (Pull from bundle).

![Building Wand Overlay](https://cdn.modrinth.com/data/cached_images/a4c4c702d2480a7a621e7d4cc8e66871b7bc5593.png)

### 🔨 Sledgehammers
Heavy mining tools designed for clearing space.
* **Function:** Mines a **3x3** area by default.
* **Durability & Speed:** High durability but slower mining speed than a pickaxe to balance its power.
* **Tiers:** Stone, Copper, Iron, Gold, Diamond, Netherite.
* **Key Enchantments:** `Radius` (Expands to 5x5), `Break Through` (Mines depth/layers), `Override` (Mines mixed block types).

![Sledgehammer Visual](https://cdn.modrinth.com/data/cached_images/df15396a49b78c057b453ac74dc8e4b28d3fe1cb.png)

### 🗿 Transformation Tools: Chisel & Spatula
Modify blocks directly in the world without needing a Stonecutter.
* **Chisel:** Cycles block variants **forward** (e.g., Stone $\rightarrow$ Stone Bricks $\rightarrow$ Chiseled Stone Bricks).
* **Spatula:** Cycles block variants **backward**.
* **Integration:** Works on most vanilla blocks (Logs, Stone, Sandstone, Quartz, etc.).
* **Key Enchantments:** `Fast Chiseling` (Cooldown reduction), `Constructor's Touch` (Unlock special transformations like Stripped Logs or Smooth Stone).

| Default Chisel Map | Constructor's Chisel Map |
| :---: | :---: |
| ![Default Map](https://cdn.modrinth.com/data/cached_images/a1ba5574dd6ee4ae1a15be792232180a1c6e7202.png) | ![Constructors Map](https://cdn.modrinth.com/data/cached_images/0511963208785dad4556472f7a6bd45f50c2640a.png) |

### 🧲 The Magnet
A customizable utility for efficient resource gathering.
* **Basic Use:** Pulls nearby items towards the player when held in Mainhand or Offhand.
* **Sneak to Disable:** Hold Shift to temporarily stop the magnet (useful for dropping items).
* **Advanced Features (with *Constructor's Touch*):**
    * **Increased Range:** Pulls items from further away (~9 blocks).
    * **Item Filtering:** Right-click an **Item Frame** containing an item to set a filter. The magnet will now *only* attract that specific item.
    * **Clear Filter:** Sneak + Right-click air.

### 🏹 Quivers
Dedicated arrow storage to free up inventory space.
* **Function:** Automatically supplies arrows to your bow when equipped in the **Offhand** or **Chestplate Slot**.
* **Standard Quiver:** Holds 64 arrows.
* **Netherite Quiver:** Holds 128 arrows and is fireproof.
* **Key Enchantments:** `Deep Pockets` (Multiplies capacity), `Constructor's Touch` (Access arrows from *any* inventory slot).

### 📏 Octant (Rangefinder)
A measurement and planning tool.
* **Measure:** Right-click two points to measure distance and area.
* **Preview:** Renders 3D wireframes of shapes like **Cuboids, Spheres, Cylinders, Pyramids, and Triangles**.
* **Visuals:** Available in 16 colors.
* **Key Enchantments:** `Constructor's Touch` (Always-on preview without sneaking).

![Enchanted Octant](https://cdn.modrinth.com/data/cached_images/a0e870d7bde3c3971311229f699d4fd8e77459d6.png)

### 📐 Velocity Gauge
* **Display:** Shows real-time speed in Blocks Per Second (m/s) above the hotbar.
* **Advanced Stats:** With *Constructor's Touch*, it also displays Top Speed and Average Speed.

---

## 🧱 Blocks & Aesthetics

### ✨ Glowing Armor Trims
Make your armor stand out in the dark!
* **Function:** Adds an emissive (glowing) effect to armor trims, similar to Glow Squid or Enderman eyes. The armor itself does not emit light levels, but remains fully visible in darkness.
* **Crafting:** Combine **Armor Piece** + **Glowing Trim Template** + **Glow Ink Sac** in a Smithing Table.

### 💡 Construction Light
An industrial floodlight for large builds.
* **Recipe:** Glass, Lapis Lazuli, Torches.
* **Properties:** Emits Light Level 15. Looks like a modern work light. Great for lighting up large halls or caves during construction without placing hundreds of torches.

### ⚙️ Reinforced Machinery
Upgraded versions of vanilla utility blocks.
* **Reinforced / Netherite Furnaces:** Smelt items 2x / 4x faster.
* **Reinforced / Netherite Hoppers:** Transfer items significantly faster.
* **Reinforced / Netherite Chests & Bundles:** Hold more items. Compatible with `Deep Pockets` and `Drawer` enchantments.

### 🧱 The Breaker (Netherite Piston)
A specialized piston that breaks blocks instead of pushing them.
* **Smart Power:** The mining tier depends on the **Redstone Signal Strength** (1-15).
    * **Signal 1-5:** Breaks soft blocks (Dirt, Sand, Wood).
    * **Signal 6-10:** Breaks stone-tier blocks.
    * **Signal 15:** Breaks obsidian and ancient debris.

---

## 🔮 Enchantment Guide

SimpleBuilding adds a comprehensive enchantment system to customize your tools.

### 🏗️ Construction Enchantments
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Linear** | I | Building Wand | Forces block placement along a single axis (Line Mode). |
| **Bridge** | I | Building Wand | Allows placing blocks into the air relative to the face (Bridging). |
| **Cover** | I | Building Wand | Places blocks following the surface terrain contour. |
| **Master Builder** | I | Wand, Storage | Links Wands to enchanted Bundles/Shulkers to pull blocks directly from them. |
| **Color Palette** | I | Wand, Storage | Randomizes block placement using blocks inside the storage container. |

### ⛏️ Mining Enchantments
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Vein Miner** | V | Pickaxe, Axe | Mines connected blocks of the same type (Ores/Logs). **Lvl 1:** 3 blocks ... **Lvl 5:** 18 blocks. |
| **Strip Miner** | III | Pickaxe | Mines a straight tunnel forward (Depth: 2, 3, or 5 blocks) when sneaking. |
| **Radius** | I | Sledgehammer | Increases mining area from **3x3** to **5x5**. |
| **Break Through** | I | Sledgehammer | Increases the **depth** of mining (mines blocks behind the target). |
| **Override** | II | Sledgehammer | **I:** Mines supported mixed materials. **II:** Mines *any* block type in range. |

### 🎒 Utility Enchantments
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Deep Pockets** | II | Bundle, Quiver | Drastically increases item capacity. |
| **Funnel** | I | Storage | Automatically picks up items into the storage container when sneaking. |
| **Drawer** | III | Chest, Bundle | Increases capacity massively (up to 256+ items), but locks storage to a **single item type**. |
| **Fast Chiseling** | II | Chisel, Spatula | Reduces the cooldown between uses. |
| **Constructor's Touch** | I | Various | **Magnet:** Filter/Range. **Octant:** Always-on preview. **Quiver:** Global access. **Chisel:** Special blocks. |
| **Range** | III | Tools | Increases interaction and mining reach distance. |
| **Kinetic Protection** | IV | Armor | Reduces damage taken from flying into walls (Elytra). |
| **Double Jump** | I | Boots | Grants the ability to perform a second jump while in mid-air. |
| **Versatility** | II | Tools | Automatically swaps to the best tool for the block you are looking at when sneaking. |

---

## 🤝 Trading & Economy

SimpleBuilding integrates into the vanilla economy, making villagers more useful.

### 🧑‍🌾 Villager Professions
* **Mason:** Sells **Building Cores** (Copper/Diamond) and **Building Wands**.
* **Librarian:** Sells enchanted books specifically for this mod (e.g., *Color Palette, Linear, Master Builder*).
* **Toolsmith:** Sells enchanted **Chisels** (with *Fast Chiseling*) and **Sledgehammers**.

### 🦙 Wandering Trader
The Wandering Trader now sells unique items that are hard to craft:
* **Building Cores** (Iron, Gold)
* **Reinforced Bundles** & **Quivers**
* **High-Level Enchanted Books** (rarely)

---

## 🌍 Loot Generation

Explore the world to find powerful gear in vanilla structures:

| Structure | Loot Category | Specific Items found here |
| :--- | :--- | :--- |
| **Stronghold Library** | 📚 Knowledge | *Range, Quiver, Master Builder, Bridge* |
| **End City** | 🌌 End Tech | *Override, Master Builder, Double Jump*<br>Diamond Gear, Diamond Core |
| **Ancient City** | 🔇 Deep Dark | *Deep Pockets, Radius*<br>Enchanted Octant, Diamond Sledgehammer, **Enchanted Quiver** |
| **Bastion** | 🐷 Nether | *Funnel, Break Through*<br>Gold Sledgehammer, Gold/Netherite Cores |
| **Nether Fortress** | 🔥 Nether | *Strip Miner, Funnel, Break Through*<br>Gold Core, Enchanted Octant |
| **Pillager Outpost** | ⚔️ Raid | *Color Palette, Cover, Linear*<br>Octant, Quiver |
| **Woodland Mansion** | 🌲 Mansion | *Color Palette, Cover, Linear, **Vein Miner IV***<br>Iron Wand/Core, Quiver |
| **Buried Treasure** | 🏴‍☠️ Pirate | *Constructor's Touch, Fast Chiseling*<br>Gold Chisel, Diamond Spatula |
| **Dungeon / Mineshaft** | 🕸️ Underground | *Fast Chiseling, Strip Miner, Vein Miner*<br>Reinforced Bundles |
| **Trial Vault** | 🗝️ Trial | *Constructor's Touch, Fast Chiseling, Double Jump*<br>Diamond Core |

---

### 🛡️ Armor Trim Effects

SimpleBuilding gives every Armor Trim a unique gameplay purpose.
**Pro Tip:** Using a **Netherite Ingot** as the trim material multiplies the effect strength by **1.5x**!

| Trim Pattern | Effect | Max Bonus (4x Netherite) |
| :--- | :--- | :--- |
| **Sentry** | Projectile Damage Reduction | **9%** |
| **Vex** | Magic Damage Reduction | **12%** |
| **Wild** | Environment Damage Reduction (Cactus/Berry) | **15%** |
| **Coast** | Water Breathing Chance | **60% Chance** |
| **Dune** | Explosion Damage Reduction | **9%** |
| **Wayfinder** | Sprinting Exhaustion Reduction | **18%** |
| **Raiser** | Increased XP Gain | **6%** |
| **Shaper** | Durability Loss Prevention | **6%** |
| **Ward** | Sonic Boom Reduction (Warden) | **30%** |
| **Silence** | Reduced Mob Detection Range | **18%** |
| **Tide** | Swim Speed Boost | **15%** |
| **Rib** | Wither Effect Duration Reduction | **High** |
| **Eye** | Ender Pearl & Fall Damage Reduction | **9%** |
| **Spire** | Fall Damage Reduction | **12%** |


### Armor Trim Material Bonuses

Each material used for an armor trim grants the wearer specific bonuses. The effects stack for each armor piece equipped with the same material.

| Material | Bonus (per piece) | Set Bonus (4 pieces) | Effect Name | Description / Condition |
| :--- | :--- | :--- | :--- | :--- |
| **Diamond** | **-2.5%** Damage | **-10%** Damage | **Hard Shell** | Reduces almost all damage (except Void/Starvation). The best all-around protection. |
| **Gold** | **-5.0%** Magic Dmg | **-20%** Magic Dmg | **Magic Dampening** | Strong protection against Magic, Potions, Dragon Breath, and Wither effect. |
| **Iron** | **-3.0%** Physical Dmg | **-12%** Physical Dmg | **Blunt Resistance** | Reduces damage from direct attacks (Mobs, Players), but not from Explosions or Magic. |
| **Copper** | **-5.0%** Lightning Dmg | **-20%** Lightning Dmg | **Lightning Rod** | Specific protection against Lightning strikes and electrically charged attacks. |
| **Emerald** | **-3.0%** vs Illagers | **-12%** vs Illagers | **Illager Resistance** | Reduces damage from Pillagers, Vindicators, Evokers, Vexes, and Ravagers. |
| **Quartz** | **-3.0%** Fire Dmg | **-12%** Fire Dmg | **Heat Shield** | Active when taking damage from Fire, Lava, or Magma Blocks. |
| **Amethyst** | **-3.0%** Sonic Dmg | **-12%** Sonic Dmg | **Resonance** | Specific protection against the Warden's "Sonic Boom" attack. |
| **Redstone** | **-3.0%** Projectiles | **-12%** Projectiles | **Trap Awareness** | Protects against arrows, tridents, and other projectiles (ideal against dispenser traps). |
| **Lapis** | **-3.0%** Magic/Witch | **-12%** Magic/Witch | **Curse Dampening** | Reduces damage from Witches and general magic (similar to Gold, but weaker/more specific). |
| **Netherite** | **+50%** Pattern Effect | **+50%** Pattern Effect | **Pattern Boost** | Amplifies the effect of the **Trim Pattern** by 1.5x (e.g., Sentry, Vex). |

--- 

## 🔧 Basic Upgrade System

Upgrade your tools to the next tier without losing their enchantments, names, or durability!

### Features
* **Keep Your Data:** Upgrading a tool preserves all NBT data (Enchantments, Custom Names, etc.).
* **One Template for All:** The **Basic Upgrade Template** works for Wood → Stone → Iron → Gold → Diamond.
* **Fair Cost System (The "Tax"):** Upgrading is slightly more expensive than crafting a new tool.
    * **Formula:** `Crafting Cost + 1 Material`
    * *Example:* Upgrading an Iron Pickaxe (usually costs 3 Iron) to Diamond requires **4 Diamonds**.
* **Mod Compatibility:** Works with vanilla tools and SimpleBuilding tools (Hammers, Chisels, etc.).

### How to use
1.  **Find:** Locate the **Basic Upgrade Template** in Dungeons, Mineshafts, or Village Toolsmith chests.
2.  **Duplicate:** Craft a copy using 1 Template + 1 Iron Block + 7 Gold Ingots.
3.  **Smithing Table:**
    * **Slot 1:** Basic Upgrade Template
    * **Slot 2:** Your Tool (e.g., Iron Pickaxe)
    * **Slot 3:** The Material Stack (e.g., 4 Diamonds)
    * *Note:* You must put the exact required amount or more in the slot. The Smithing Table will consume the required amount.

---

## ⚙️ Configuration

The mod is highly configurable via **Mod Menu** and **Cloth Config**.

* **Visuals:** Toggle tool animations (Chisel rotation, Sledgehammer crack).
* **Gameplay:** Enable/Disable Double Jump, adjust magnet range.
* **World Gen:** Toggle Villager trades or Loot Table injections to fit your modpack's balance.

---

## 📥 Installation & Requirements

1.  Install **Minecraft 1.21+**.
2.  Install **Fabric Loader**.
3.  Install **Fabric API** (Required).
4.  Install **Cloth Config** (Required for settings).
5.  *(Optional)* Install **Mod Menu** to access settings in-game.
6.  Download **SimpleBuilding** and place it in the `mods` folder.

---

## ⚖️ License

This mod is available under the MIT License. You are free to include it in your modpacks, edit it, or use it as a base for your own projects.