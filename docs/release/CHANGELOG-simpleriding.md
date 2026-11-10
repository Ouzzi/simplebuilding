# Simple Riding 0.9.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Riding** for Minecraft 26.3 on Fabric.

Saddle speed, horse jumps, nautilus dashes, mount armor utilities, loot, and trades.

## Added

- **Tailwind** - Tailwind I–III enhances saddles and all 16 colored harnesses.
- **Leaping** - Leaping I–III increases the jump strength of a player-controlled horse by 20% per level by default.
- **Horse Armor Utilities** - Horse armor accepts Protection, Fire Protection, Blast Protection, Projectile Protection, Feather Falling, and Leaping.
- **Exploration Loot** - Bastion treasure and other chests: 0–2 rolls; Tailwind II/III weights 20/10, Leaping II/III 10/5, empty 30.
- **Librarian Trades** - Librarians at level 2 offer Tailwind I or Leaping I (weights 30/20), cost 10–29 emeralds, 2 uses, 25 XP, reputation discount 0.5.
- **Server Riding Bounds** - Client movement and jump commands are checked on the server thread.
- **Horseshoes** - Horses, donkeys, mules, skeleton and zombie horses get four hoof slots in their inventory.
- **Ghast Speed Multiplier** - enchantments.swiftRide.ghastSpeedMultiplier: Speed bonus per Tailwind level; range 0–1.
- **Horse Speed Multiplier** - enchantments.swiftRide.horseSpeedMultiplier: Speed bonus per Tailwind level; range 0–1.
- **Other Mount Speed Multiplier** - enchantments.swiftRide.otherSpeedMultiplier: Speed bonus per Tailwind level; range 0–1.
- **Jump Strength Multiplier** - enchantments.horseJump.jumpStrengthMultiplier: Jump strength bonus per Leaping level; range 0–0.5.
- **Maximum Total Speed Bonus** - safety.maximumSpeedBonus: Maximum added speed, including combined nautilus Tailwind and dash; range 0-3 (at most 4x base).
- **Maximum Total Jump Bonus** - safety.maximumJumpBonus: Maximum horse jump or nautilus dash bonus; range 0-1.5.
- **Riding Movement Limit** - safety.movementDistancePerTick: Maximum cumulative client vehicle movement in blocks per server tick; range 0.5-4.
- **Riding Packet Limit** - safety.movementPacketsPerTick: Maximum vehicle movement packets per server tick; range 1-20.
- **Nautilus Speed Multiplier** - enchantments.swiftRide.nautilusSpeedMultiplier: Speed bonus per Tailwind level; range 0-1, also limited by the total speed cap.
- **Nautilus Dash Multiplier** - enchantments.horseJump.nautilusDashMultiplier: Dash bonus per Leaping level; range 0-0.5.
- **Horse Feather Falling Reduction** - enchantments.horseJump.featherFallingReduction: Fall damage reduction per level; range 0-0.12, at most level IV. Default: 0.12.
- **Mount Armor Enchantability** - enchantments.horseJump.armorEnchantability: Enchanting table power for mount armor; range 0-15.
- **Terrain Bonus** - horseshoes.terrainBonus: Movement efficiency on slowing blocks such as Soul Sand and Honey at a full Enderite set; weaker sets scale down.
- **Handling Bonus** - horseshoes.handlingBonus: Extra sideways steering at a full Enderite set (Vanilla share 0.5; backward gets half).
- **Full Set Speed Bonus** - horseshoes.fullSetSpeedBonus: Extra speed with all four horseshoes; shares the total speed cap with Tailwind.
- **Full Set Jump Bonus** - horseshoes.fullSetJumpBonus: Extra jump strength with all four horseshoes; shares the total jump cap with Leaping.
- **Full Set Fall Damage Increase** - horseshoes.fullSetFallDamageIncrease: Extra fall damage with all four horseshoes, rounded, so short falls stay the same.
- **Blocks per Durability Point** - horseshoes.blocksPerDurability: Ridden distance on the ground per point of wear on each horseshoe.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: none (Cloth Config optional: config screen only)
- Optional integrations: simplebuilding, modmenu, simpleores

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simpleriding config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
