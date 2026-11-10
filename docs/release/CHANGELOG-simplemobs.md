# Simple Mobs 0.1.0 - changelog (Minecraft 26.3, Fabric)

## Release notes

First release of **Simple Mobs** for Minecraft 26.3 on Fabric.

Hollow Howler and Hollow Watcher bosses, Shellker, Soulfire Blaze, Dread and Deceiver mobs, Blighted and Abyssal Wither with Nether Heart and Red Nether Gem, and the Potion of Silence.

## Added

- **Hollow Howler** - A Deep Dark boss you summon yourself: build the figure, blow a goat horn near a sculk sensor in the dark, and it manifests within 5 seconds.
- **Potion of Silence** - Warped Potion (Awkward Potion without SimpleLib) + Echo Core: 3:00 of Silence (redstone 8:00, splash and lingering as usual).
- **Shellker** - An End gateway guard: a Shulker in a bedrock-hard shell.
- **High Gravity and the Shellker Helmet** - High Gravity pulls you down: jumps, Levitation and Elytra lift keep 10 % and falls hurt 50 % more per level (Feather Falling still helps).
- **Combat effects: Volatile, Deflection, Mirror** - Three status effects, levels I-V, for mobs and players: Volatile answers hits with a small explosion, Deflection negates hits, Mirror flips a coin between more damage for the bearer and damage back to the attacker.
- **Danger zones: effects on new monsters** - With Simple Resonance installed and zoneMobsEnabled on, new monsters get status effects (never more health) by the danger level of their spawn place: the farther from the world spawn and the more experienced the nearest player, the more likely and the stronger.
- **Armour trim abilities** - Armour trims do more than look good: every trim pattern and trim material gives its wearer a small ability (less damage of one kind, speed, luck, reach, air, experience).
- **Soulfire Blaze** - A Blaze of soul fire, found rarely in Soul Sand Valleys.
- **Deceiver: cloak, disguise, mirror** - From Deceiver Cloth: a cloak that lets you take any mob's or player's shape, a particle-free invisibility potion and a mirror that sees through deceptions.
- **Dread** - A fear mob you build yourself: a 2.6 block knight of rusted armor with soul flames in its empty helmet and a chain mace.
- **Hollow Watcher** - A 4 block shadow with many eyes on the outer End islands.
- **Shivering aura, Calm Amulet and Focus** - Near a fear mob you shiver: your aim wavers.
- **Blighted Wither, Nether Heart and Red Nether Gem** - The Wither, only stronger: put a Netherite Helmet on the middle skull of the Wither figure before the third skull and the Blighted Wither rises - 600 health, armor 8, three phases, and every special attack is announced before it hits.
- **Wither Wings** - Four Elytra stages that raise only the top speed a strong firework rocket reaches: x2 Wither, x4 Nether, x6 End and x8 Void Wings.
- **Strong Gunpowder, Strong Rocket, Strong TNT** - Strong Gunpowder (3 gunpowder + 1 blaze powder -> 1) makes strong rockets (paper + 1 Strong Gunpowder -> 3, flight duration 4) and Strong TNT (like TNT with Strong Gunpowder, explosion power 6 instead of 4).
- **Weathered Trials** - Rare dungeon-like rooms underground with a Weathered Trial Spawner and Weathered Vaults - the Trial Chambers loop in verdigris copper, with their own loot.
- **Evil Omen** - A stronger Bad Omen for Netherite-age players: brew the Evil Potion with a Nether Heart, drink it, turn a Weathered Trial or a village raid evil, win Evil Trial Keys and open an Evil Vault.
- **Void chain** - The Evil chain one tier up - Void Potion, Void Omen, Void Trial, Void Vault - switched off by default until the Ender Heart has a source.
- **Trials: limits, hits and switches** - What the Weathered Trials and the Evil Omen add on top of Vanilla, and every switch that turns it off.
- **Void Runner** - An End boss you summon: a slim 1.9 block fencer with blades along its forearms and a long scarf.
- **Brimguard** - An End boss you summon: a squat 1.5 block figure under a wide bedrock sombrero.
- **Brood hollow** - Spawn eggs from mob essence, in the world: no block, no screen.

## Requirements

- Minecraft 26.3, Fabric Loader 0.19.5 or newer, Fabric API
- Required mods: SimpleLib, Simple Resonance

## Notes

- All options are in the config screen (Mod Menu / Cloth Config) and in `/simplemobs config` where available.
- Back up your world before updating from an older SimpleBuilding build; old ids are migrated automatically.
