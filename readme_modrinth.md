# 🏗️ SimpleBuilding
**(Mod ID: `simplebuilding`)**

![Icon Banner](https://cdn.modrinth.com/data/cached_images/983e18266fd2731fa41a9911d98945837cf2e6bc.png)

SimpleBuilding adds powerful tools and utility items designed to make large-scale construction and terrain modification in Survival Minecraft easier and more enjoyable, while maintaining a vanilla-friendly feel.

### [📚 Read the Full Documentation](https://github.com/Ouzzi/simplebuilding)

## 🛠️ The Tools

### 🪄 Building Wands
Place blocks in patterns instantly. Requires the block in your inventory (or linked Bundle).
* **Tiers:** Copper (3x3), Iron (5x5), Gold/Diamond (7x7), Netherite (9x9).
* **Usage:** Right-click a block to extend the face.
* **Enchantments:**
    * `Cover`: Builds on the surface layer instead of a flat plane.
    * `Linear`: Forces placement in a single line axis.
    * `Bridge`: Builds a platform extending into the air from the edge.

### 🔨 Sledgehammers
Heavy mining tools that break multiple blocks at once. Slower than pickaxes but clears large areas.
* **Tiers:** Stone to Netherite.
* **Mining Area:** 3x3 by default.
* **Enchantments:**
    * `Radius`: Increases area to **5x5**.
    * `Break Through`: Increases mining **depth** (mines blocks behind the target).
    * `Override`: Allows mining mixed materials (e.g. Stone & Dirt at once).

### 🏹 Quivers
Dedicated arrow storage. Automatically supplies your Bow when equipped in **Offhand** or **Chestplate**.
* **Standard Quiver:** 1 Stack (64 Arrows).
* **Netherite Quiver:** 2 Stacks (128 Arrows). Fireproof.
* **Enchantments:**
    * `Deep Pockets`: Multiplies capacity.
    * `Constructor's Touch`: Access arrows from anywhere in inventory.

### 🗿 Chisels & Spatulas
Transform blocks in-world without a Stonecutter.
* **Chisel:** Cycles block variants *forward* (e.g. Stone -> Bricks -> Chiseled).
* **Spatula:** Cycles block variants *backward*.
* **Enchantments:** `Fast Chiseling` (Faster cooldown), `Constructor's Touch` (Better transformations).

### 📏 Octant (Rangefinder) & Velocity Gauge
Measurement tools to help you plan your builds and track your speed.
* **Octant:** Mark two points to measure distance and area. Supports various shapes like **Cuboids, Spheres, Cylinders, and Pyramids** to preview builds. Available in 16 colors.
* **Velocity Gauge:** Displays real-time speed (BPS).

### 🎒 Reinforced Storage
* **Reinforced Bundle:** Can be enchanted with `Deep Pockets` for massive storage capacity.
* **Netherite Bundle:** Highest capacity and durability.

### 🧱 Custom Blocks:
* **Reinforced & Netherite Furnaces, Smokers & Blast Furnaces:** Cook food and smelt ores at significantly higher speeds (2x for Reinforced, 4x for Netherite).
* **Reinforced Piston:** Stronger pushing capabilities.
* **Netherite Piston (The Breaker):** Unlike a normal piston, this block **breaks** the block in front of it when activated based on signal strength.
* **Reinforced Hopper:** Increased transfer speed and item throughput.
* **Netherite Hopper:** Maximum speed for high-performance sorting systems.
* **Construction Light:** A bright, industrial light source crafted with Lapis and Glass. Perfect for preventing mob spawns in large halls without spamming torches.

---

## ✨ Custom Enchantments

SimpleBuilding introduces a massive enchantment system to customize your tools.

### 🏗️ Building
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Master Builder** | I | Wands/Storage | Allows Wands to pull blocks from an enchanted Bundle/Shulker in your inventory. |
| **Color Palette** | I | Wands/Storage | Randomizes block placement from the Bundle (great for texturing). |
| **Cover** | I | Wands | Builds on the surface layer instead of a flat plane. |
| **Linear** | I | Wands | Forces placement in a single line axis. |
| **Bridge** | I | Wands | Builds a platform extending into the air from the edge. |

### ⛏️ Mining
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Vein Miner** | V | Pickaxe/Axe | Mines connected blocks of the same type (Ores/Logs). |
| **Strip Miner** | III | Pickaxe | Mines a straight tunnel forward (Depth: 2, 3, or 5 blocks) when sneaking. |
| **Radius** | I | Sledgehammer | Increases mining area from **3x3** to **5x5**. |
| **Break Through** | I | Sledgehammer | Increases mining depth (mines blocks *behind* the target as well). |
| **Override** | II | Sledgehammer | Allows the hammer to break mixed materials (Lvl 1: Supported, Lvl 2: Any). |

### 🎒 Utility
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Kinetic Protection** | IV | Armor | Reduces damage taken from flying into walls (Elytra). |
| **Funnel** | I | Bundle/Shulker | Automatically picks up items into the container when sneaking. |
| **Deep Pockets** | II | Bundles/Quivers | Significantly increases item capacity. |
| **Drawer** | III | Chest/Bundle | Drastically increases capacity (up to 256+ items), but locks to one item type. |
| **Range** | III | Tools | Increases the reach distance for placing/breaking blocks. |
| **Fast Chiseling** | II | Chisel/Spatula | Reduces cooldown for transformation tools. |
| **Constructor's Touch** | I | Chisel/Quiver | Enables special transformations & global inventory access for Quivers. |
| **Double Jump** | I | Boots | Allows the player to perform a second jump while in mid-air. |
| **Versatility** | II | Tools | Swaps to the best tool when sneaking (Lvl 1: Hotbar, Lvl 2: Inventory). |

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

## 🤝 Trading & Economy Integration
SimpleBuilding integrates seamlessly into the villager economy.

### 🧑‍🌾 Villager Trades
* **Mason:** Sells **Building Cores** and **Building Wands**.
* **Toolsmith:** Sells **Sledgehammers** and **Chisels** with exclusive enchantments.
* **Librarian:** Source for all new enchanted books like *Master Builder* or *Linear*.

### 🦙 Wandering Trader
Don't ignore him! He now sells rare **Building Cores**, **Reinforced Bundles**, **Quivers**, and unique high-level enchanted books not found elsewhere.

---

## 🌍 Loot Generation
Find tools and enchantments in the world!
* **Stronghold Libraries:** High chance for *Master Builder*, *Quiver*, *Range*.
* **End Cities:** The best place for Diamond gear, *Bridge*, and *Override*.
* **Ancient Cities:** *Deep Pockets*, *Radius*, Netherite gear, and **Enchanted Quivers**.
* **Mineshafts & Dungeons:** *Fast Chiseling*, *Strip Miner*, *Vein Miner*, and Bundles.
* **Bastions & Fortresses:** *Funnel*, *Break Through*, Gold/Netherite Cores.

---

## ⚙️ Config & Compatibility

You can configure many aspects of SimpleBuilding to fit your needs:
* **Tool Animations:** Toggle visual feedbacks like the Chisel rotation or Sledgehammer cracks.
* **World Gen:** Disable villager trades or loot table injections if desired.

**(Requires Cloth Config & Mod Menu for in-game configuration)**

---

**Requires:** Fabric API & Cloth Config
**(Optional):** Mod Menu for in-game configuration