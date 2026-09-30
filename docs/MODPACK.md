# SimpleBuilding for modpacks and servers

As of 2026-09-29 (wave 23, run N). Applies to every line and loader: MC 26.2 (Fabric, NeoForge,
Forge), 26.3 (Fabric, NeoForge), 1.21.11 (Fabric, NeoForge) and the 26.4 snapshot line. Game tests:
`ModpackTests` (`simplebuilding:modpack_game_test_*`).

This page lists what a pack author or server admin can change without touching code: which
protection hooks the mod honors, which tags and datapack files it reads, and which statistics it
records.

## 1. Claims and protection

Every tool that changes more than the one block the player clicked asks the loader, **per block**,
with the same events a hand break or hand placement fires. Claim mods (FTB Chunks, Open Parties and
Claims, GriefPrevention-style mods, spawn protection plugins) therefore protect their area without
any SimpleBuilding-specific integration.

| Tool | What is asked, per block |
| --- | --- |
| Building wand (plane, line, bridge, roof) | place event for the cell |
| Blueprint build, octant fill (both run through the wand) | place event for the cell |
| Amethyst Resonance Rod beam: ignite | place event for the fire cell |
| Amethyst Resonance Rod beam: melt, dry, light, prime TNT | break event for the block |
| Sledgehammer area swing, Vein Miner, Strip Miner | break event for each extra block (vanilla `ServerPlayerGameMode#destroyBlock`) |
| Mod pistons (netherite breaker, breach) | piston event and break event with a fake player (config `pistonsFireBreakEvents`) |

The events per loader:

- **Fabric:** `PlayerBlockBreakEvents.BEFORE` (a refusal fires `CANCELED`). Fabric API has no block
  place event; the placement check fires `BEFORE` for the cell that is about to be filled (a
  fake-placement check - claim mods on Fabric guard their land through the break event).
- **NeoForge:** `BreakBlockEvent` (`BlockEvent.BreakEvent` on 1.21.11) and
  `BlockEvent.EntityPlaceEvent`.
- **Forge:** `BlockEvent.BreakEvent` (cancelled or result `DENY`) and `BlockEvent.EntityPlaceEvent`.

The place event is fired *before* the block is set, so its block snapshot still holds the block
that is there now (usually air). On top of the events every cell is checked against vanilla spawn
protection and the world border (`Level#mayInteract`), world height, loaded chunks and
`Player#mayUseItemAt`. A refused cell stays empty and costs neither material nor durability.

While such a check runs, the mod's own break listeners (sledgehammer, Vein Miner, Strip Miner) do
nothing, so asking about a cell never starts an area break.

Code: `platform/BuildGuard` (interface), `util/BuildPermissions` (the one entry point),
`FabricBuildGuard`, `NeoForgeBuildGuard`, `ForgeBuildGuard`.

## 2. Tags

All tags use `"replace": false`; add entries from a datapack.

| Tag | Kind | Default | Effect |
| --- | --- | --- | --- |
| `simplebuilding:vein_miner_ores` | block + item | `#c:ores`, the vanilla ore tags, nether quartz ore, nether gold ore, ancient debris, nihilith ore, astralit ore | what Vein Miner takes with a pickaxe, and what its crack preview outlines |
| `simplebuilding:building_wand_blacklist` | block | `minecraft:structure_void` | never placed by the building wand, a blueprint build or an octant fill |
| `simplebuilding:attractor_ignore` | item | `minecraft:structure_void` | never pulled by the attractor (magnet) |
| `simplebuilding:not_allowed_in_backpack` | item | `minecraft:structure_void` | may not go into a backpack slot |

The default entry of the last three tags is a creative-only technical block; it only shows the
format (and keeps the checks testable).

### Common `c:` tags

The mod adds its materials to the convention tags Fabric, NeoForge and Forge share (`c:` namespace on
every supported line), both the group tag and the per-material tag:

| Tag | Items (block tags too for blocks) |
| --- | --- |
| `c:ingots`, `c:ingots/enderite` | enderite ingot |
| `c:nuggets`, `c:nuggets/enderite`, `c:nuggets/netherite` | enderite nugget, netherite nugget |
| `c:raw_materials`, `c:raw_materials/enderite` | raw enderite |
| `c:gems`, `c:gems/ender_quartz`, `c:gems/nihilith` | ender quartz, nihilith shard |
| `c:dusts`, `c:dusts/astralit` | astralit dust |
| `c:storage_blocks`, `c:storage_blocks/{enderite,cracked_diamond,ender_quartz,astralit,nihilith}` | block of enderite, block of cracked diamond, block of ender quartz, astralit block, nihilith block |
| `c:ores`, `c:ores/nihilith`, `c:ores/astralit`, `c:ores_in_ground/end_stone` | nihilith ore, astralit ore |

## 3. Attractor (magnet)

The attractor pulls loose items to the player who holds it. It leaves these alone:

- items that can never be picked up (`ItemEntity#setNeverPickUp`, pickup delay 32767) - the usual
  mark of another mod's display, pedestal or marker item;
- items reserved for another player (vanilla's `Owner` field);
- **death drops of other players.** Vanilla leaves no mark on death drops, so the mod tags every item
  a dying player drops with the entity tag `simplebuilding.death_drop.<uuid>`. The owner's own
  attractor still collects them; graves and corpse mods that keep the items never produce a drop;
- items in `simplebuilding:attractor_ignore`.

## 4. Datapack tables

Both tables are loaded on every datapack (re)load (`/reload` included) by a server reload listener
on every loader, and sent to each client on join and after every reload (payload
`simplebuilding:data_tables_sync`), so the chisel, the upgrade prediction on the client and JEI show
what the server does. Files in the `simplebuilding` namespace are applied first, all other
namespaces after them in id order; override a shipped file by using the same path, or add your own.

### 4.1 Chisel and spatula chains: `data/<namespace>/chisel_transformations/<name>.json`

```json
{
  "tier": "stone",
  "table": "chisel",
  "chains": [
    { "blocks": ["minecraft:smooth_sandstone", "minecraft:cut_sandstone", "minecraft:sandstone"] },
    { "cyclic": true, "blocks": ["minecraft:netherrack", "minecraft:nether_bricks"] }
  ],
  "remove": ["minecraft:stone"]
}
```

- `tier`: `stone` (stone chisel), `iron` (copper and iron), `diamond` (gold and diamond),
  `netherite`, `enderite`. Every tier also has all lower tiers.
- `table`: `chisel` (always) or `touch` (only with Constructor's Touch, on top of `chisel`).
- A chain moves each block one step forward with the chisel and one step back with the spatula or a
  sneaking chisel; `cyclic` links the last block back to the first.
- `remove` (optional): drops what this tier and table say about these blocks.
- Unknown block ids (a mod that is not installed) are skipped with a warning, together with the
  links that touch them; a broken file is skipped as a whole.

Shipped: `stone_chisel`, `stone_touch`, `iron_chisel`, `iron_touch`, `diamond_chisel`,
`diamond_touch`, `netherite_chisel`, `netherite_touch`, `enderite_chisel`.

### 4.2 Sledgehammer upgrades: `data/<namespace>/sledgehammer_upgrades/<name>.json`

```json
{
  "upgrades": [
    {
      "from": "simplebuilding:reinforced_hopper",
      "to": "simplebuilding:netherite_hopper",
      "material": "simplebuilding:netherite_nugget",
      "min_hammer": "diamond",
      "damage_per_hit": 4,
      "tier": "netherite"
    }
  ],
  "remove": ["simplebuilding:reinforced_piston"]
}
```

- `material`: held in the off hand, used up when the fifth blow lands.
- `min_hammer`: `any`, `diamond`, `netherite`, `enderite`.
- `damage_per_hit`: sledgehammer durability per blow (five blows).
- `tier`: `reinforced`, `netherite` or `enderite` (which advancement the upgrade counts for).
- The block keeps its properties and its block entity; the target block has to allow that (every
  mod machine and chest does).

Shipped: `reinforced` (the eight copper chests), `netherite`, `enderite`.

The shipped files are generated from the built-in tables (`ModDataTablesProvider`, `runDatagen`); a
game test checks that the loaded files equal the code.

## 5. Loot

The mod's chest, vault, fishing and charged-creeper loot is no longer written into the vanilla
tables. Each vanilla table the mod has loot for gets one pool that rolls a loot table of the mod,
`simplebuilding:inject/<path of the vanilla table>`:

```
data/simplebuilding/loot_table/inject/chests/end_city_treasure.json
data/simplebuilding/loot_table/inject/chests/woodland_mansion.json
data/simplebuilding/loot_table/inject/gameplay/fishing/treasure.json
data/simplebuilding/loot_table/inject/charged_creeper/root.json
...
```

Override or empty such a file in a datapack to change the mod's loot for that chest; the vanilla
table stays untouched. The chances are those of `docs/LOOT-BALANCE.md` and
`docs/KERNE-SELTENHEIT.md`. The building core pools use the loot condition
`simplebuilding:core_chance` (`{"chance": 0.008}`), which multiplies the chance with the config
factor `worldGen.buildingCoreLootChanceMultiplier` when the chest is rolled. The config switch
`worldGen.enableLootTableChanges` still removes the reference pools from every chest (the blaze and
enderman heads of charged creepers stay).

The files are generated from `ModLootTableModifications#apply`; a game test compares the loaded
tables with the code.

## 6. Statistics

Custom statistics, listed in the vanilla statistics screen (General) and usable as scoreboard
criteria (`minecraft.custom:simplebuilding.<name>`):

| Statistic | Counts |
| --- | --- |
| `simplebuilding:wand_blocks_placed` | blocks the building wand placed (plane, line, bridge, blueprint, octant fill) |
| `simplebuilding:chisel_uses` | blocks the chisel or spatula moved one step |
| `simplebuilding:teleports` | teleports by the spawn teleporter and the echo sounder |
