# 🏗️ SimpleBuilding: Construction Evolved
**(Mod ID: `simplebuilding`)**

![Version](https://img.shields.io/badge/Version-1.21-green) ![Loader](https://img.shields.io/badge/Loader-Fabric-blue)

**SimpleBuilding** is a vanilla-friendly expansion designed to take the tediousness out of large-scale construction and mining. It introduces powerful tools like **Sledgehammers** for excavation and **Building Wands** for rapid placement, along with a suite of utility enchantments and upgraded storage options. No complex machinery—just better tools for better builds.

## ✨ Key Features
* **🔨 Sledgehammers:** Mine 3x3 areas (or larger!) to clear space quickly.
* **🪄 Building Wands:** Extend walls, build bridges, or place lines of blocks instantly.
* **🏹 Quivers:** Dedicated arrow storage that works from your chestplate slot or offhand.
* **🗿 Chisels & Spatulas:** Transform blocks in-world (e.g., Stone Bricks → Cracked Bricks) without a Stonecutter.
* **🎒 Reinforced Storage:** Bundles and Shulker Boxes that hold massive amounts of items.
* **⛏️ Vein Mining:** Mine entire ore veins or trees with a single break.

---

## 🛠️ Item Overview

### 🗿 Transformation Tools: Chisel & Spatula
Modify blocks directly in the world without needing a Stonecutter.
* **Chisel:** Cycles block variants **forward** (e.g., Stone $\rightarrow$ Stone Bricks $\rightarrow$ Chiseled Stone Bricks).
* **Spatula:** Cycles block variants **backward**.
* **Supported Enchantments:**
    * *Fast Chiseling:* Reduces the cooldown between uses.
    * *Constructor's Touch:* Enables special transformations (e.g., Logs $\rightarrow$ Stripped Logs, or Stone $\rightarrow$ Smooth Stone).

| Default Chisel Map | Constructor's Chisel Map |
| :---: | :---: |
| ![Default Map](https://cdn.modrinth.com/data/cached_images/a1ba5574dd6ee4ae1a15be792232180a1c6e7202.png) | ![Constructors Map](https://cdn.modrinth.com/data/cached_images/0511963208785dad4556472f7a6bd45f50c2640a.png) |

### 🔨 Sledgehammer
A heavy mining tool designed to clear areas quickly.
* **Default Area:** Mines a **3x3** area centered on the target block.
* **Balancing:** Slower mining speed than a pickaxe to balance its power.
* **Tiers:** Stone, Copper, Iron, Gold, Diamond, Netherite.
* **Supported Enchantments:**
    * *Radius:* Increases mining area to **5x5**.
    * *Break Through:* Increases the mining **depth** (mines blocks behind the target).
    * *Override:*
        * **Lvl I:** Mines different materials if supported (e.g., Stone + Coal Ore).
        * **Lvl II:** Mines **any** block in the radius (except unbreakable).

![Sledgehammer Visual](https://cdn.modrinth.com/data/cached_images/df15396a49b78c057b453ac74dc8e4b28d3fe1cb.png)

### 🔮 Building Cores
Endgame crafting components required to craft Building Wands.
* **Tiers:** Copper, Iron, Gold, Diamond, Netherite.


### 🪄 Building Wands
The ultimate tool for builders. Extends the face of a block using materials from your inventory.
* **Tiers & Range:**
    * **Copper:** 3x3
    * **Iron:** 5x5
    * **Gold / Diamond:** 7x7
    * **Netherite:** 9x9
* **Supported Enchantments:**
    * *Linear:* Forces placement in a single line instead of a grid.
    * *Bridge:* Extends blocks into the air from the edge of a block.
    * *Cover:* Places blocks following the surface terrain contour.
    * *Master Builder:* Pulls blocks from an enchanted Bundle/Shulker Box.
    * *Color Palette:* Randomizes blocks (requires Bundle).

![Building Wand Overlay](https://cdn.modrinth.com/data/cached_images/a4c4c702d2480a7a621e7d4cc8e66871b7bc5593.png)

### 🏹 Quivers
Never run out of ammo again.
* **Function:** Holds arrows. Automatically supplies your bow when equipped in the **Offhand** or **Chestplate Slot**.
* **Variants:**
    * **Standard Quiver:** Holds 1 Stack (64 arrows). Crafted with a Bundle, Leather, and String.
    * **Netherite Quiver:** Holds 2 Stacks (128 arrows). Fireproof. Crafted via Smithing.
* **Enchantment Interaction:**
    * *Deep Pockets:* Multiplies capacity (Lvl 1: x2, Lvl 2: x4).
    * *Constructor's Touch:* Allows the bow to access arrows from the Quiver even if it sits **anywhere** in your inventory.

### 🎒 Reinforced Storage
* **Reinforced Bundle:** Higher capacity than vanilla bundles.
* **Netherite Bundle:** Double the capacity of the Reinforced Bundle and fireproof.
* **Enchantments:**
    * *Deep Pockets:* Drastically increases capacity (up to 256 items for Reinforced, more for Netherite).
    * *Funnel:* Automatically picks up items while sneaking.

![Master Builder Places Directly From Bundle](https://cdn.modrinth.com/data/cached_images/6c03241765cc6497b36beb4035078e1e159447d1.png)

### 📏 Octant (Rangefinder)
A utility tool for planning.
* **Function:** Measure distances and areas between two points.
* **Visuals:** Available in all 16 colors. Custom overlay rendering.
* **Enchantments:** *Constructor's Touch* (improves visuals/usability).

![Enchanted Octant](https://cdn.modrinth.com/data/cached_images/a0e870d7bde3c3971311229f699d4fd8e77459d6.png)

Velocity-Gauge
### 📐 Velocity-Gauge
A measuring tool display your current movement speed.
* **Function:** Shows real-time speed in blocks per second (BPS).
* **Data:** Displays Top Speed and Average Speed.
* **Enchantments:** *Constructor's Touch* (more data).

![Replace this with a description](https://cdn.modrinth.com/data/cached_images/cf6e3261c03c6c906ca836a93b29247de5a50e0a.png)

---

## ✨ Enchantment Guide

| Enchantment | Max Lvl | Target Tool | Effect / Description |
| :--- | :---: | :--- | :--- |
| **Vein Miner** | V | Pickaxe, Axe | Mines connected blocks of the same type (Ores/Logs). **Lvl 1:** 3 blocks ... **Lvl 5:** 18 blocks. |
| **Strip Miner** | III | Pickaxe | Mines a straight tunnel forward (Depth: 2, 3, or 5 blocks). |
| **Kinetic Protection** | IV | Armor | Reduces damage taken from flying into walls (Elytra). |
| **Fast Chiseling** | II | Chisel, Spatula | Reduces the cooldown between uses. |
| **Constructor's Touch** | I | Tools, Octant, Quiver | Enables special block transformations, improved Octant visuals, and global inventory access for Quivers. |
| **Radius** | I | Sledgehammer | Increases mining area from **3x3** to **5x5**. |
| **Break Through** | I | Sledgehammer | Increases the **depth** of mining blocks. |
| **Override** | II | Sledgehammer | **I:** Mines supported materials. **II:** Mines *any* block type in range. |
| **Linear** | I | Building Wand | Forces block placement along a single axis (Line). |
| **Bridge** | I | Building Wand | Allows placing blocks into the air relative to the face (Bridging). |
| **Cover** | I | Building Wand | Places blocks following the surface terrain contour. |
| **Master Builder** | I | Wand, Storage | Links Wands to enchanted Bundles/Shulkers to pull blocks directly from them. |
| **Color Palette** | I | Wand, Storage | Randomizes block placement using blocks inside the storage container. |
| **Deep Pockets** | II | Bundle, Quiver | Increases capacity (Multiplies base storage). |
| **Funnel** | I | Storage | Automatically picks up items into the storage when sneaking. |
| **Range** | III | Tools | Increases interaction and mining reach. |
| **Double Jump** | I | Boots | Grants the ability to perform a second jump while in mid-air. |

![Boots With Double Jump](https://cdn.modrinth.com/data/cached_images/0bc9e907c489fe2aa1fd44d68cb3dc9e43e0ed16.png)

---

## 🤝 Trading Guide

Villagers and Traders are the best way to obtain specific enchantments and cores.

### 🧑‍🌾 Villager Professions
* **Mason:** Sells Building Cores (Copper/Diamond) and Copper Building Wands.
* **Librarian:** Sells enchanted books specifically for this mod (e.g., *Color Palette, Line Place, Master Builder*).
* **Toolsmith:** Sells enchanted Chisels (with *Fast Chiseling*) and Sledgehammers (with *Break Through, Efficiency*).

### 🦙 Wandering Trader
Keep an eye out for the trader! He may sell:
* Octants
* Reinforced Bundles
* Building Cores
* Rare Enchanted Books (*Bridge, Radius, Quiver*)

---

## 🌍 Loot Generation

Explore the world to find unique items and enchantments in vanilla chests:

| Structure | Loot Category | Specific Items / Enchantments found here |
| :--- | :--- | :--- |
| **Stronghold Library** | 📚 Knowledge | *Range, Quiver, Master Builder, Bridge* |
| **End City** | 🌌 End Tech | *Override, Master Builder, Double Jump*<br>Diamond Gear, Diamond Core |
| **Ancient City** | 🔇 Deep Dark | *Deep Pockets, Radius*<br>Enchanted Octant, Diamond Sledgehammer, **Enchanted Quiver** |
| **Bastion** | 🐷 Nether | *Funnel, Break Through*<br>Gold Sledgehammer, Gold/Netherite Cores |
| **Nether Fortress** | 🔥 Nether | *Strip Miner, Funnel, Break Through*<br>Gold Core, Enchanted Octant |
| **Pillager Outpost** | ⚔️ Raid | *Color Palette, Cover, Linear*<br>Octant, Quiver |
| **Woodland Mansion** | 🌲 Mansion | *Color Palette, Cover, Linear, **Vein Miner IV***<br>Iron Wand/Core, Quiver |
| **Buried Treasure** | 🏴‍☠️ Pirate | *Constructor's Touch, Fast Chiseling*<br>Gold Chisel, Diamond Spatula |
| **Dungeon** | 🕸️ Underground | *Fast Chiseling, Funnel, Break Through, **Vein Miner II***<br>Reinforced Bundle |
| **Mineshaft** | ⛏️ Mining | *Fast Chiseling, Strip Miner, **Vein Miner III***<br>Enchanted Reinforced Bundle |
| **Shipwreck** | ⚓ Ocean | *Fast Chiseling*<br>Reinforced Bundle |
| **Igloo** | ❄️ Ice | *Constructor's Touch, Fast Chiseling*<br>Diamond Chisel, Iron Spatula |
| **Trial Vault** | 🗝️ Trial | *Constructor's Touch, Fast Chiseling, Double Jump*<br>Diamond Core |

---

## 📥 Installation

1.  Install **Minecraft 1.21+**
2.  Install the latest **Fabric Loader**.
3.  Download **Fabric API** and put it in your `mods` folder.
4.  Download **SimpleBuilding** and put it in your `mods` folder.

---

## ⚖️ License

This mod is available under the MIT License. Feel free to include it in your modpacks!