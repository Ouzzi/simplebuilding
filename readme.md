# 🏗️ SimpleBuilding: Construction Evolved
**(Mod ID: `simplebuilding`)**

![Version](https://img.shields.io/badge/Version-1.21-green) ![Loader](https://img.shields.io/badge/Loader-Fabric-blue)

**SimpleBuilding** is a vanilla-friendly expansion designed to take the tedium out of large-scale construction and mining. It introduces powerful tools like **Sledgehammers** for excavation and **Building Wands** for rapid placement, along with a suite of utility enchantments and upgraded storage options. No complex machinery—just better tools for better builds.

## ✨ Key Features
* **🔨 Sledgehammers:** Mine 3x3 areas (or larger!) to clear space quickly.
* **🪄 Building Wands:** Extend walls, build bridges, or place lines of blocks instantly.
* **🗿 Chisels & Spatulas:** Transform blocks in-world (e.g., Stone Bricks → Cracked Bricks) without a Stonecutter.
* **📏 Octant (Rangefinder):** Measure distances between two points to plan your builds perfectly.
* **🎒 Reinforced Storage:** Bundles and Shulker Boxes that hold more items and interact intelligently with your tools.

---

## 🛠️ Item Overview

### 🗿 Transformation Tools: Chisel & Spatula
Modify blocks directly in the world without needing a Stonecutter.
* **Chisel:** Cycles block variants **forward** (e.g., Stone $\rightarrow$ Stone Bricks $\rightarrow$ Chiseled Stone Bricks).
* **Spatula:** Cycles block variants **backward**.
* **Supported Enchantments:**
    * *Fast Chiseling:* Reduces the cooldown between uses.
    * *Constructor's Touch:* Enables special transformations (e.g., turning Netherite into Crying Obsidian or Stone into Smooth Stone).

| Default Chisel Map | Constructor's Chisel Map |
| :---: | :---: |
| ![Default Map](https://cdn.modrinth.com/data/cached_images/a1ba5574dd6ee4ae1a15be792232180a1c6e7202.png) | ![Constructors Map](https://cdn.modrinth.com/data/cached_images/0511963208785dad4556472f7a6bd45f50c2640a.png) |


### 🔨 Sledgehammer
A heavy mining tool designed to clear areas quickly.
* **Default Area:** Mines a **3x3** area centered on the target block.
* **Balancing:** Slower mining speed than a pickaxe to balance its power.
* **Tiers:** Available from Stone to Netherite.
* **Supported Enchantments:**
    * *Radius:* Increases mining area to **5x5**.
    * *Break Through:* Increases the mining **depth** (mines blocks behind the target).
    * *Ignore Block Type:*
        * **Lvl I:** Mines different blocks if they are supported by the tool (e.g., Stone + Coal Ore).
        * **Lvl II:** Mines **any** block in the radius (except unbreakable ones).

![Sledgehammer Visual](https://cdn.modrinth.com/data/cached_images/df15396a49b78c057b453ac74dc8e4b28d3fe1cb.png)

### 🔮 Building Cores
Endgame crafting components required to craft Building Wands.
* **Tiers:** Copper, Iron, Gold, Diamond, Netherite.


### 🪄 Building Wands
The ultimate tool for builders. It extends the face of the block you are looking at using materials from your inventory.
* **Tiers & Range:**
    * **Copper:** 3x3
    * **Iron:** 5x5
    * **Gold / Diamond:** 7x7
    * **Netherite:** 9x9
* **Supported Enchantments:**
    * *Line Place:* Forces placement in a single line instead of a grid.
    * *Bridge:* Extends blocks into the air from the edge of a block.
    * *Surface Place:* (WIP) Places blocks on the surface layer relative to the terrain.
    * *Master Builder:* Pulls blocks from an enchanted Bundle/Shulker Box.
    * *Color Palette:* Randomizes blocks (requires Bundle).

![Building Wand Overlay](https://cdn.modrinth.com/data/cached_images/a4c4c702d2480a7a621e7d4cc8e66871b7bc5593.png)

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

### 🎒 Reinforced Storage
Upgraded storage options that interact with your tools.
* **Reinforced Bundle:** High capacity.
* **Supported Enchantments:**
    * *Deep Pockets:* Drastically increases capacity (up to 256 items).
    * *Funnel:* Automatically picks up items while sneaking.
    * *Quiver:* Allows Bows to shoot arrows directly from this container.

![Master Builder Places Directly From Bundle](https://cdn.modrinth.com/data/cached_images/6c03241765cc6497b36beb4035078e1e159447d1.png)

---

## ✨ Enchantment Guide

SimpleBuilding adds a robust system of enchantments to tailor your tools to your needs.

| Enchantment | Max Lvl | Target Tool             | Effect / Description |
| :--- | :---: |:------------------------| :--- |
| **Fast Chiseling** | II | Chisel, Spatula         | Reduces the cooldown between uses. |
| **Constructor's Touch** | I | Chisel, Spatula, Octant | Enables special block transformations (e.g., Logs to Stripped Logs) and improves Octant visuals. |
| **Radius** | I | Sledgehammer            | Increases mining area from **3x3** to **5x5**. |
| **Break Through** | I | Sledgehammer            | Increases the **depth** of mining blocks. |
| **Override** | II | Sledgehammer            | **I:** Mines supported materials. **II:** Mines *any* block type in range. |
| **Strip Miner** | III | Pickaxe                 | Mines a straight tunnel forward (Depth: 2, 3, or 5 blocks). |
| **Linear** | I | Building Wand           | Forces block placement along a single axis (Line). |
| **Bridge** | I | Building Wand           | Allows placing blocks into the air relative to the face (Bridging). |
| **Cover** | I | Building Wand           | Places blocks following the surface terrain contour. |
| **Master Builder** | I | Wand, Storage           | Links Wands to enchanted Bundles/Shulkers to pull blocks directly from them. |
| **Color Palette** | I | Wand, Storage           | Randomizes block placement using blocks inside the storage container. |
| **Deep Pockets** | II | Bundle                  | Increases capacity (**I:** 128 items, **II:** 256 items). |
| **Funnel** | I | Storage                 | Automatically picks up items into the storage when sneaking. |
| **Quiver** | I | Storage                 | Allows Bows to shoot arrows directly from this container. |
| **Range** | III | Some Tools              | Increases interaction and mining reach. |
| **Double Jump** | I | Boots                   | Grants the ability to perform a second jump while in mid-air. |


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

Specific items and enchantments can be found in vanilla chests:

| Structure | Loot Category | Specific Items / Enchantments |
| :--- | :--- | :--- |
| **Stronghold Library** | 📚 Knowledge | *Range, Quiver, Master Builder, Bridge* |
| **End City / Ship** | 🌌 End Tech | *Ignore Block Type, Bridge, Master Builder*<br>Diamond Wands, Sledgehammers, Chisels, Spatulas |
| **Ancient City** | 🔇 Deep Dark | *Deep Pockets, Radius*<br>Enchanted Octant, Diamond Sledgehammer |
| **Bastion** | 🐷 Nether | *Funnel, Break Through*<br>Gold Sledgehammer, High-Tier Building Cores |
| **Nether Fortress** | 🔥 Nether | *Strip Miner, Funnel, Break Through*<br>Gold Building Core, Octant |
| **Pillager Outpost** | ⚔️ Raid | *Color Palette, Surface Place, Line Place*<br>Octant |
| **Woodland Mansion** | 🌲 Mansion | *Color Palette, Surface Place, Line Place*<br>Iron Wand, Iron Core |
| **Buried Treasure** | 🏴‍☠️ Pirate | *Constructor's Touch, Fast Chiseling*<br>Gold Chisel, Diamond Spatula |
| **Dungeon / Mineshaft** | 🕸️ Underground | *Fast Chiseling, Strip Miner*<br>Reinforced Bundle |
| **Shipwreck** | ⚓ Ocean | *Fast Chiseling*<br>Reinforced Bundle |
| **Igloo** | ❄️ Ice | *Constructor's Touch, Fast Chiseling*<br>Diamond Chisel, Iron Spatula |
| **Trial Vault** | 🗝️ Trial | *Constructor's Touch, Fast Chiseling*<br>Diamond Building Core |

---

## 📥 Installation

1.  Install **Minecraft 1.21+**
2.  Install the latest **Fabric Loader**.
3.  Download **Fabric API** and put it in your `mods` folder.
4.  Download **SimpleBuilding** and put it in your `mods` folder.

---

## ⚖️ License

This mod is available under the MIT License. Feel free to include it in your modpacks!