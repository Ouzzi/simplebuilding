SIMPLEBUILDING DOCUMENTATION
===========================

ITEM OVERVIEW
-----------------
- Chisel and Spatula: Transform blocks in-world without a Stonecutter.
    - Chisel: Cycles block variants forward (e.g. Stone -> Bricks -> Chiseled).
    - Spatula: Cycles block variants backward.
    - Enchantments:
        - Fast Chiseling: Reduces cooldown between uses.
        - Constructor's Touch: Better transformations (e.g., Stone to Smooth Stone).
- Sledgehammer: Heavy mining tool that breaks multiple blocks at once.
    - Mining Area: 3x3 by default.
    - Enchantments:
        - Radius: Increases area to 5x5.
        - Break Through: Increases mining depth.
        - Ignore Block Type: Allows mining mixed materials (I supported materials, II any).
- Building Cores: Crafting component for Building Wands.
  - Tiers: Copper, Iron, Gold, Diamond, Netherite.
  - Endgame material.
- Building Wands: Place blocks in patterns instantly.
  - Tiers: Copper (3x3), Iron (5x5), Gold/Diamond (7x7), Netherite (9x9).
  - Usage: Right-click a block to extend the face.
  - Enchantments:
    - Line Place: Forces placement in a single line axis.
    - Bridge: Builds a platform extending into the air from the edge.
    - Surface Place: Builds on the surface layer instead of a flat plane. (in work)
- Octant (Rangefinder): Measure distances and areas.
  - Variants: Available in all 16 colors.
  - Custom Overlay.
  - Enchantments:
    - Constructors Touch: Better Visuals.
- Reinforced Storage: Enhanced Bundles with enchantment support.
  - Enchantments:
    - Deep Pockets: Increases storage capacity.
    - Master Builder: Allows Wands to pull blocks from an enchanted Bundle/Shulker in your inventory.
    - Funnel: Automatically picks up items into the storage when sneaking.
    - Quiver: Bows can shoot arrows directly from this container.
- Custom Enchantments: New enchantments for building and mining tools.
  - Strip Miner: Mines a tunnel (1x2 to 1x5 depending on level) in one go.



Enchantment Overview
-----------------
| Enchantment | Max Lvl | Target Tool | Description |
| :--- | :---: | :--- | :--- |
| **Fast Chiseling** | II | Chisel/Spatula | Reduces cooldown between interactions. |
| **Constructor's Touch** | I | Chisel/Spatula/Octant | Enables special block transformations; improves Octant visuals. |
| **Radius** | I | Sledgehammer | Increases mining area from 3x3 to **5x5**. |
| **Break Through** | I | Sledgehammer | Increases mining **depth**. |
| **Ignore Block Type** | II | Sledgehammer | **I:** Mines supported blocks. **II:** Mines any block in range. |
| **Strip Miner** | III | Pickaxe | Mines a straight tunnel (depth increases with level: 2, 3, or 5 blocks). |
| **Line Place** | I | Building Wand | Forces placement in a single axis (Line). |
| **Bridge** | I | Building Wand | Allows placing blocks into the air (bridging). |
| **Surface Place** | I | Building Wand | Places blocks following the surface terrain. |
| **Master Builder** | I | Wand/Storage | Links Wands to Bundles/Shulkers to use their inventory. |
| **Color Palette** | I | Wand/Storage | Randomizes block placement from the container. |
| **Deep Pockets** | II | Bundle | Increases capacity (I: 128, II: 256 items). |
| **Funnel** | I | Storage | Auto-pickup items when sneaking. |
| **Quiver** | I | Storage | Allows Bows to shoot arrows from this container. |
| **Range** | I | All Tools | Increases interaction range. |
| **



Trade Overview
-----------------
// Villager Trades:
// - (chisels, sledgehammers, building wands, Enchanted Books(fast chisel I, range I, master builder I, color palette I, funnel I, Ignore block types I, strip miner I, line place I)
// - Mason: (Copper Building Core, Diamond Building Core, Copper Building Wand)
// - Librarian: Enchanted Books (Color Palette I, Fast Chiseling I, Line Place I, Master Builder I, Range I, Funnel I, Ignore Block Type I, Strip Miner I)
// - Toolsmith: Enchanted Chisels (Fast Chiseling I, II), Enchanted Sledgehammers (Break Through I, Ignore Block Type I, Range I, Unbreaking II, Efficiency III), Enchanted Diamond Pickaxe (Strip Miner I)
// Wandering Trades: Octant, Reinforced Bundle, Building Cores, Enchanted Books (Bridge I, Radius I, Quiver I)


Loot Table Overview
-----------------
// List all Loot Table types:
// 1. STRONGHOLD LIBRARY CHEST: (RANGE, QUIVER, MASTER_BUILDER, BRIDGE)
// 2. END CITY: (RANGE, QUIVER, MASTER_BUILDER, OVERRIDES, BRIDGE), (DIAMOND_CHISEL_ENCHANTED, DIAMOND_SPATULA_ENCHANTED,DIAMOND_BUILDING_WAND_ENCHANTED, DIAMOND_SLEDGEHAMMER_ENCHANTED, diamond_core)
// 4. ANCIENT CITY: (DEEP POCKETS, RADIUS), (OCTANT_ENCHANTED, DIAMOND_SLEDGEHAMMER)
// 5. BASTION: (FUNNEL, BREAK THROUGH), (GOLD_SLEDGEHAMMER, gold_core, NETHERITE_CORE)
// 6. NETHER BRIDGE: (FUNNEL, BREAK THROUGH, STRIP_MINER), (gold_core, OCTANT_ENCHANTED)
// 7. PILLAGER OUTPOST: (COLOR PALETTE, SURFACE PLACE, LINE PLACE), (OCTANT)
// 8. WOODLAND MANSION: (COLOR PALETTE, SURFACE PLACE, LINE PLACE), (IRON_BUILDING_WAND, iron_core)
// 9. BURIED TREASURE: (CONSTRUCTORS TOUCH, FAST CHISEL), (GOLD_CHISEL, DIAMOND_SPATULA)
// 10. SIMPLE DUNGEON: (FAST CHISEL, FUNNEL, BREAK THROUGH), (REINFORCED_BUNDLE)
// 11. SHIPWRECK TREASURE: (FAST CHISEL), (REINFORCED_BUNDLE)
// 12. IGLOO: (CONSTRUCTORS TOUCH, FAST CHISEL), (DIAMOND_CHISEL, IRON_SPATULA)
// 13. ABANDONED MINESHAFT: (FAST CHISEL, STRIP_MINER I), (REINFORCED_BUNDLE_ENCHANTED)
// 14. VAULT: (CONSTRUCTORS TOUCH, FAST_CHISEL), (diamond_core)