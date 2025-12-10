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
    * `Surface Place`: Builds on the surface layer instead of a flat plane.
    * `Line Place`: Forces placement in a single line axis.
    * `Bridge`: Builds a platform extending into the air from the edge.

### 🔨 Sledgehammers
Heavy mining tools that break multiple blocks at once. Slower than pickaxes but clears large areas.
* **Tiers:** Stone to Netherite.
* **Mining Area:** 3x3 by default.
* **Enchantments:**
    * `Radius`: Increases area to **5x5**.
    * `Break Through`: Increases mining **depth**.
    * `Ignore Block Type`: Allows mining mixed materials (e.g. Stone & Dirt at once).

### 🗿 Chisels & Spatulas
Transform blocks in-world without a Stonecutter.
* **Chisel:** Cycles block variants *forward* (e.g. Stone -> Bricks -> Chiseled).
* **Spatula:** Cycles block variants *backward*.
* **Enchantments:** `Fast Chiseling` (Faster cooldown), `Constructor's Touch` (Better transformations).

### 📏 Octant (Rangefinder)
A measuring tool to help you plan your builds.
* **Usage:** Mark two points to measure distance and area.
* **Variants:** Available in all 16 colors.

### 🎒 Reinforced Storage
* **Reinforced Bundle:** Can be enchanted with `Deep Pockets` for massive storage capacity.
* **Netherite Bundle:** Highest capacity and durability.

---

## ✨ Custom Enchantments

SimpleBuilding introduces a massive enchantment system to customize your tools.

### 🏗️ Building
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Master Builder** | I | Wands/Storage | Allows Wands to pull blocks from an enchanted Bundle/Shulker in your inventory. |
| **Color Palette** | I | Wands/Storage | Randomizes block placement from the Bundle (great for texturing). |
| **Surface Place** | I | Wands | Builds on the surface layer instead of a flat plane. |
| **Line Place** | I | Wands | Forces placement in a single line axis. |
| **Bridge** | I | Wands | Builds a platform extending into the air from the edge. |

### ⛏️ Mining
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Radius** | I | Sledgehammer | Increases mining area from **3x3** to **5x5**. |
| **Break Through** | I | Sledgehammer | Increases mining depth (mines blocks *behind* the target as well). |
| **Ignore Block Type** | II | Sledgehammer | Allows the hammer to break mixed materials (Lvl 1: Supported, Lvl 2: Any). |
| **Strip Miner** | III | Pickaxe | Mines a tunnel (1x2 to 3x3 depending on level) in one go. |

### 🎒 Utility
| Enchantment | Max Lvl | Target | Description |
| :--- | :---: | :--- | :--- |
| **Quiver** | I | Bundle/Shulker | Bows can shoot arrows directly from this container. |
| **Funnel** | I | Bundle/Shulker | Automatically picks up items into the container when sneaking. |
| **Deep Pockets** | II | Bundles | Significantly increases item capacity (up to 256 slots). |
| **Range** | III | Tools | Increases the reach distance for placing/breaking blocks. |
| **Fast Chiseling** | II | Chisel/Spatula | Reduces cooldown for transformation tools. |
| **Constructor's Touch** | I | Chisel/Spatula | Enables special transformations (e.g. Netherite Obsidian -> Crying Obsidian). |
| **Double Jump** | I | Boots | Allows the player to perform a second jump while in mid-air. |

---

## 🤝 Trading & Economy Integration
SimpleBuilding integrates seamlessly into the villager economy.

### 🧑‍🌾 Villager Trades
* **Mason:** Sells **Building Cores** and **Building Wands**.
* **Toolsmith:** Sells **Sledgehammers** and **Chisels** with exclusive enchantments.
* **Librarian:** Source for all new enchanted books like *Master Builder* or *Line Place*.

### 🦙 Wandering Trader
Don't ignore him! He now sells rare **Building Cores**, **Reinforced Bundles**, and unique high-level enchanted books not found elsewhere.

---

## 🌍 Loot Generation
Find tools and enchantments in the world!
* **Stronghold Libraries:** High chance for *Master Builder*, *Quiver*, *Range*.
* **End Cities:** The best place for Diamond gear, *Bridge*, and *Ignore Block Type*.
* **Ancient Cities:** *Deep Pockets*, *Radius*, and Netherite gear.
* **Mineshafts & Dungeons:** *Fast Chiseling*, *Strip Miner*, and Bundles.

---

**Requires:** Fabric API
**(Optional):** Mod Menu & Cloth Config for in-game configuration.