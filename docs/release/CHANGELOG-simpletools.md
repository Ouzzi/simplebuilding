# Simple Tools 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Tools** for Minecraft 26.3 on Fabric.

Full-durability bonus, the Thrift and Soulbound enchantments, the Death Vault, the Biome Brush, anvil repairs without extra cost and lava buckets into furnaces; a sub-mod of Simple QoL, also playable alone.

## Added

- **Furnace lava fill** - Either hand inserts one lava bucket into a furnace, blast furnace, or smoker with an empty fuel slot or one leftover empty bucket.
- **Better item frames** - Item frames and glow item frames, only while sneaking and only with an item in the frame: shears hide or show the frame (Vanilla Invisible flag, 1 durability, Unbreaking counts).
- **Durability bonus** - Damageable held Vanilla and mod tools above the durability threshold can gain attack damage and mining speed.
- **Biome Brush** - Brush in the main hand, a biome material in the off hand, right-click a block: the biome around it becomes the material's biome.
- **Thrift** - Enchantment for every item with durability, levels I-III: +1/3 maximum durability per level, level III doubles it.
- **Repairing keeps the anvil cost** - Repairing with material, combining two items without new enchantments or renaming keeps the higher anvil cost of the inputs instead of raising it.
- **Enderite and ceramic buckets** - Enderite holds two, never breaks; ceramic wears out.
- **Soulbound** - Treasure enchantment, one level, for everything that can carry the Curse of Vanishing (the two exclude each other).
- **Death Vault** - Crafted block (obsidian, eye of ender, soul sand or soul soil, chest).
- **(with Simple Money) Money bills as offering** - When Simple Money is installed, its money bills count like diamonds in the Death Vault offering slot (optional tag entry, nothing changes without the mod).
- **Strip Station** - Crafted cage (iron axe, 4 iron bars, 4 planks).

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: Cloth Config
- Optional integrations: simplebuilding, simplequalityoflife, modmenu

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpletools config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
