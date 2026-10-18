package com.simpleblocks.test;

import com.simpleblocks.BlocksLoot;
import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Module GameTests: registration, tags and block classes, the vibration dampening of the hushed tiles (a sculk sensor behind
 * the block does not hear what the control sensor hears), Sculk Deepslate drops and experience, drops of every block,
 * recipes for every block and the Ancient City chest loot. All on the empty 8 x 8 x 8 structure.
 */
public final class BlocksTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    public static final int BLOCK_COUNT = 37;
    private static final Set<String> HUSHED = Set.of("hushed_deepslate_tiles", "hushed_deepslate_tile_stairs",
            "hushed_deepslate_tile_slab", "hushed_deepslate_tile_wall");

    static {
        ALL.put("registered", h -> {
            h.assertTrue(BlocksRegistry.NAMES.size() == BLOCK_COUNT, "37 blocks, found " + BlocksRegistry.NAMES.size());
            for (String name : BlocksRegistry.NAMES) {
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleBlocks.id(name)), "block " + name);
                h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleBlocks.id(name)), "item " + name);
            }
            h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleBlocks.id("blocks")), "creative tab");
            h.assertTrue(SimpleBlocks.on(), "module switch defaults on");
            h.succeed();
        });
        // Tab merge T1: with SimpleBuilding loaded its building blocks stand in this tab and it registers no tab of its own (registry ids only).
        ALL.put("shared_tab_with_simplebuilding", h -> {
            var bricks = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "astralit_bricks");
            if (BuiltInRegistries.ITEM.containsKey(bricks)) {
                h.assertTrue(!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "building_blocks")),
                        "SimpleBuilding registers no building blocks tab next to this one");
                net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
                var item = BuiltInRegistries.ITEM.getValue(bricks);
                h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(SimpleBlocks.id("blocks")).getDisplayItems().stream().anyMatch(s -> s.is(item)),
                        "astralit bricks stand in the Simple Blocks tab");
            }
            h.succeed();
        });
        ALL.put("guide_book", h -> {
            h.assertTrue(BuiltInRegistries.ITEM.containsKey(SimpleBlocks.id("guide_book")), "guide book");
            h.succeed();
        });
        ALL.put("tags_and_shapes", h -> {
            for (String name : BlocksRegistry.NAMES) {
                Block block = BlocksRegistry.BLOCKS.get(name);
                BlockState state = block.defaultBlockState();
                h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), name + " mines with a pickaxe");
                h.assertTrue((block instanceof StairBlock) == name.endsWith("_stairs") && state.is(BlockTags.STAIRS) == name.endsWith("_stairs"), name + " stairs");
                h.assertTrue((block instanceof SlabBlock) == name.endsWith("_slab") && state.is(BlockTags.SLABS) == name.endsWith("_slab"), name + " slab");
                h.assertTrue((block instanceof WallBlock) == name.endsWith("_wall") && state.is(BlockTags.WALLS) == name.endsWith("_wall"), name + " wall");
                boolean hushed = HUSHED.contains(name);
                h.assertTrue(state.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS) == hushed, name + " occludes vibrations only if hushed");
                h.assertTrue(state.is(BlockTags.DAMPENS_VIBRATIONS) == hushed, name + " dampens vibrations only if hushed");
            }
            h.assertTrue(BlocksRegistry.SCULK_DEEPSLATE instanceof DropExperienceBlock, "sculk deepslate gives experience");
            for (String[] aged : BlocksRegistry.AGED) {
                h.assertTrue(BlocksRegistry.BLOCKS.containsKey(aged[0]) && BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.Identifier.withDefaultNamespace(aged[1])), aged[0] + " ages " + aged[1]);
                h.assertTrue(BlocksRegistry.BASES.get(aged[0]) == BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(aged[1])), aged[0] + " copies " + aged[1]);
            }
            h.succeed();
        });
        MAX_TICKS.put("hushed_blocks_vibrations", 160);
        ALL.put("hushed_blocks_vibrations", h -> {
            ServerLevel level = h.getLevel();
            // Two sensors, the same vibration four blocks away: one with hushed tiles in between, one without.
            // Placing blocks is itself a vibration, so the sensors settle first and the real event comes afterwards.
            h.setBlock(new BlockPos(3, 1, 1), BlocksRegistry.HUSHED_DEEPSLATE_TILES);
            h.setBlock(new BlockPos(1, 1, 1), Blocks.SCULK_SENSOR);
            h.setBlock(new BlockPos(5, 1, 6), Blocks.SCULK_SENSOR);
            h.runAfterDelay(70, () -> {
                h.assertTrue(h.getBlockState(new BlockPos(1, 1, 1)).getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.INACTIVE
                        && h.getBlockState(new BlockPos(5, 1, 6)).getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.INACTIVE, "both sensors settled");
                // One vibration at (5,1,1): the sensor at (1,1,1) has the hushed tiles in its line, the one at (5,1,6) nothing.
                level.gameEvent(null, GameEvent.BLOCK_PLACE, h.absolutePos(new BlockPos(5, 1, 1)));
                h.runAfterDelay(12, () -> {
                    SculkSensorPhase behind = h.getBlockState(new BlockPos(1, 1, 1)).getValue(SculkSensorBlock.PHASE);
                    SculkSensorPhase open = h.getBlockState(new BlockPos(5, 1, 6)).getValue(SculkSensorBlock.PHASE);
                    h.assertTrue(open != SculkSensorPhase.INACTIVE, "the control sensor hears the vibration (" + open + ")");
                    h.assertTrue(behind == SculkSensorPhase.INACTIVE, "the sensor behind the hushed tiles hears nothing (" + behind + ")");
                    h.succeed();
                });
            });
        });
        MAX_TICKS.put("sculk_deepslate_drops_and_xp", 60);
        ALL.put("sculk_deepslate_drops_and_xp", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            BlockState state = BlocksRegistry.SCULK_DEEPSLATE.defaultBlockState();
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
            silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            h.assertTrue(Block.getDrops(state, level, pos, null, null, pickaxe).stream().anyMatch(s -> s.is(Items.COBBLED_DEEPSLATE)), "drops cobbled deepslate");
            h.assertTrue(Block.getDrops(state, level, pos, null, null, silk).stream().anyMatch(s -> s.is(BlocksRegistry.SCULK_DEEPSLATE.asItem())), "silk touch keeps the block");
            // The real break path of the loader (Fabric and NeoForge hand out experience at different places).
            ServerPlayer player = h.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            AABB box = new AABB(pos).inflate(4);
            int[] before = {player.totalExperience};
            player.setItemInHand(InteractionHand.MAIN_HAND, silk);
            h.setBlock(new BlockPos(1, 1, 1), BlocksRegistry.SCULK_DEEPSLATE);
            player.gameMode.destroyBlock(pos);
            h.runAfterDelay(4, () -> {
                int gained = player.totalExperience - before[0] + level.getEntitiesOfClass(ExperienceOrb.class, box).stream().mapToInt(ExperienceOrb::getValue).sum();
                h.assertTrue(gained == 0, "no experience with silk touch, got " + gained);
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
                h.setBlock(new BlockPos(1, 1, 1), BlocksRegistry.SCULK_DEEPSLATE);
                player.gameMode.destroyBlock(pos);
                h.runAfterDelay(4, () -> {
                    int xp = player.totalExperience - before[0] + level.getEntitiesOfClass(ExperienceOrb.class, box).stream().mapToInt(ExperienceOrb::getValue).sum();
                    h.assertTrue(xp >= 1 && xp <= 2, "1-2 experience, got " + xp);
                    h.succeed();
                });
            });
        });
        ALL.put("recipes_and_loot_exist", h -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
            var context = SlotDisplayContext.fromLevel(level);
            Set<Item> obtainable = new HashSet<>();
            for (var holder : level.getServer().getRecipeManager().getRecipes()) {
                for (var display : holder.value().display()) {
                    for (ItemStack stack : display.result().resolveForStacks(context)) obtainable.add(stack.getItem());
                }
            }
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            for (String name : BlocksRegistry.NAMES) {
                Block block = BlocksRegistry.BLOCKS.get(name);
                h.assertTrue(obtainable.contains(block.asItem()), name + " has a recipe");
                if (block == BlocksRegistry.SCULK_DEEPSLATE) continue;
                BlockState state = block.defaultBlockState();
                int expected = 1;
                if (block instanceof SlabBlock) {
                    state = state.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
                    expected = 2;
                }
                int count = 0;
                for (ItemStack drop : Block.getDrops(state, level, pos, null, null, pickaxe)) {
                    if (drop.is(block.asItem())) count += drop.getCount();
                }
                h.assertTrue(count == expected, name + " drops itself x" + expected + ", got " + count);
            }
            h.succeed();
        });
        ALL.put("ancient_city_loot", h -> {
            ServerLevel level = h.getLevel();
            int[] pools = {0};
            BlocksLoot.apply(BuiltInLootTables.ANCIENT_CITY, pool -> pools[0]++);
            BlocksLoot.apply(BuiltInLootTables.END_CITY_TREASURE, pool -> pools[0] += 10);
            h.assertTrue(pools[0] == 1, "the pool is added to the Ancient City table only");
            var table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.ANCIENT_CITY);
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(new BlockPos(1, 1, 1))))
                    .create(LootContextParamSets.CHEST);
            boolean found = false;
            for (int i = 0; i < 600 && !found; i++) {
                for (ItemStack stack : table.getRandomItems(params)) {
                    if (stack.is(BlocksRegistry.SCULK_DEEPSLATE.asItem())) {
                        found = true;
                        h.assertTrue(stack.getCount() >= 2 && stack.getCount() <= 6, "2-6 per find");
                    }
                }
            }
            h.assertTrue(found, "Ancient City chests hold Sculk Deepslate (loader loot hook)");
            h.succeed();
        });
    }

    /** Blue nether bricks (N71): like red nether bricks with SimpleLib's warped wart; Vanilla's warped wart block stands in without SimpleLib. */
    private static void blueNetherBricksRecipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        boolean lib = BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplelib", "warped_wart"));
        ItemStack wart = lib ? new ItemStack(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplelib", "warped_wart")))
                : new ItemStack(Items.WARPED_WART_BLOCK);
        ItemStack brick = new ItemStack(Items.NETHER_BRICK);
        var input = lib ? net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(wart, brick, brick, wart))
                : net.minecraft.world.item.crafting.CraftingInput.of(3, 3, List.of(wart, brick, brick, brick, brick, brick, brick, brick, brick));
        ItemStack made = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        h.assertTrue(made.is(BlocksRegistry.BLUE_NETHER_BRICKS.asItem()) && made.getCount() == (lib ? 1 : 4), "blue nether bricks (simplelib=" + lib + "): " + made);
        h.assertTrue(BlocksRegistry.BLUE_NETHER_BRICK_STAIRS instanceof StairBlock && BlocksRegistry.BLUE_NETHER_BRICK_SLAB instanceof SlabBlock
                && BlocksRegistry.BLUE_NETHER_BRICK_WALL instanceof WallBlock, "stairs, slab and wall classes");
        h.assertTrue(BlocksRegistry.BLUE_NETHER_BRICKS.defaultBlockState().getSoundType() == Blocks.RED_NETHER_BRICKS.defaultBlockState().getSoundType(),
                "sound of the red nether bricks");
        // The stonecutter cuts all three shapes from the new bricks.
        var one = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(BlocksRegistry.BLUE_NETHER_BRICKS));
        int shapes = 0;
        for (var holder : level.getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof net.minecraft.world.item.crafting.StonecutterRecipe r && r.matches(one, level)) shapes++;
        }
        // Simple Octets' octet cut is a Shaping Saw recipe while Simple Blocks is loaded (principle A4b), never the stonecutter's.
        h.assertTrue(shapes == 3, "stonecutter: stairs, slab, wall (the octet only in the Shaping Saw), got " + shapes);
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleoctets", "blue_nether_bricks_octet"))) {
            h.assertTrue(com.simpleblocks.saw.Sawing.serverEntries(level.getServer().getRecipeManager()).stream().anyMatch(e -> e.sawOnly()
                    && e.input().test(new ItemStack(BlocksRegistry.BLUE_NETHER_BRICKS))
                    && "simpleoctets:blue_nether_bricks_octet".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.result().item().value()).toString())),
                    "the Shaping Saw cuts the blue nether bricks octet");
        }
        h.succeed();
    }

    static {
        ALL.put("blue_nether_bricks_recipes", BlocksTests::blueNetherBricksRecipes);
        ALL.put("discover_recipe_unlocks", com.simpleblocks.discover.BlocksDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleblocks.discover.BlocksDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simpleblocks.discover.BlocksDiscover::firstPickupTips);
        com.simpleblocks.saw.SawTests.register(ALL);
    }

    private BlocksTests() {}
}
