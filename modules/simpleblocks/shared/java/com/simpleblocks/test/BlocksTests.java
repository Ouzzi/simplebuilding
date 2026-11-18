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
    // PAL35b: + 20 of the metal row (BlocksRegistry.METALS); the 12 ice bricks (a-icebricks38) were lost in the bundle-188 merge (125 -> 137);
    // PLAN40 simpleblocks-3: + 3 sculk tile shapes + chiseled blue nether bricks.
    public static final int BLOCK_COUNT = 158; // N77: + 16 concrete walls; N89b: + nether brick fence gate
    private static final Set<String> HUSHED = Set.of("hushed_deepslate_tiles", "hushed_deepslate_tile_stairs",
            "hushed_deepslate_tile_slab", "hushed_deepslate_tile_wall");

    static {
        ALL.put("registered", h -> {
            h.assertTrue(BlocksRegistry.NAMES.size() == BLOCK_COUNT, BLOCK_COUNT + " blocks, found " + BlocksRegistry.NAMES.size());
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
        // N48 (14.11.): this module requests SimpleLib's group nether_bookshelves (fabric.mod.json "simplelib:content", Neo/Forge in the
        // constructor): the plain Crimson/Warped blazewood bookshelves (simplelib:) exist as building blocks with a Vanilla-cost recipe
        // (Simple Enchanting crafts them with its blazewood planks instead) and, without SimpleBuilding, stand in this module's tab.
        ALL.put("nether_bookshelves_requested", h -> {
            h.assertTrue(com.simplelib.api.SimpleLibApi.materialRequested("nether_bookshelves"), "group nether_bookshelves is requested");
            boolean enchanting = BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleenchanting", "dark_enchanter"));
            var recipes = h.getLevel().getServer().getRecipeManager();
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
            var tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValue(SimpleBlocks.id("blocks")).getDisplayItems();
            for (String shelf : com.simplelib.registry.LibMaterials.NETHER_BOOKSHELVES) {
                var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("simplelib", shelf);
                var block = BuiltInRegistries.BLOCK.getValue(id);
                h.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR && block.defaultBlockState().is(net.minecraft.tags.BlockTags.ENCHANTMENT_POWER_PROVIDER), shelf + " exists and counts at the Vanilla table");
                h.assertTrue(recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, id)).isPresent() == !enchanting,
                        shelf + ": Vanilla-cost recipe exactly without Simple Enchanting");
                boolean simplebuilding = BuiltInRegistries.ITEM.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplebuilding", "astralit_bricks"));
                boolean here = tab.stream().anyMatch(st -> st.is(block.asItem()));
                // SimpleLib's tab rules may move a SimpleLib item from a module tab into the Simple Lib tab: that counts as listed.
                var libTab = BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath("simplelib", "lib"));
                boolean inLib = libTab.isPresent() && libTab.get().getDisplayItems().stream().anyMatch(st -> st.is(block.asItem()));
                h.assertTrue(simplebuilding ? !here : here || inLib, shelf + ": listed (this tab or the Simple Lib tab) without SimpleBuilding, not here with it"
                        + " (here=" + here + ", lib tab=" + inLib + ")");
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
            for (String[] aged : java.util.stream.Stream.concat(BlocksRegistry.AGED.stream(), BlocksRegistry.CHISELED.stream()).toList()) {
                h.assertTrue(BlocksRegistry.BLOCKS.containsKey(aged[0]) && BuiltInRegistries.BLOCK.containsKey(net.minecraft.resources.Identifier.parse(aged[1])), aged[0] + " ages " + aged[1]);
                h.assertTrue(BlocksRegistry.BASES.get(aged[0]) == BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(aged[1])), aged[0] + " copies " + aged[1]);
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
            // PAL14: the ice members drop only with Silk Touch, like Vanilla ice.
            ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
            silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
            for (String name : BlocksRegistry.NAMES) {
                Block block = BlocksRegistry.BLOCKS.get(name);
                h.assertTrue(obtainable.contains(block.asItem()), name + " has a recipe");
                boolean ice = BlocksRegistry.ICE.contains(name);
                if (block == BlocksRegistry.SCULK_DEEPSLATE) continue;
                BlockState state = block.defaultBlockState();
                int expected = 1;
                if (block instanceof SlabBlock) {
                    state = state.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
                    expected = 2;
                }
                int count = 0;
                for (ItemStack drop : Block.getDrops(state, level, pos, null, null, ice ? silk : pickaxe)) {
                    if (drop.is(block.asItem())) count += drop.getCount();
                }
                h.assertTrue(count == expected, name + " drops itself x" + expected + ", got " + count);
                if (ice) {
                    h.assertTrue(Block.getDrops(state, level, pos, null, null, pickaxe).isEmpty(), name + " drops nothing without Silk Touch");
                }
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
        // Simple Octets' octet cut is a Shapecutter recipe while Simple Blocks is loaded (principle A4b), never the stonecutter's.
        h.assertTrue(shapes == 4, "stonecutter: stairs, slab, wall, chiseled (the octet only in the Shapecutter), got " + shapes);
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleoctets", "blue_nether_bricks_octet"))) {
            h.assertTrue(com.simpleblocks.saw.Sawing.serverEntries(level.getServer().getRecipeManager()).stream().anyMatch(e -> e.sawOnly()
                    && e.input().test(new ItemStack(BlocksRegistry.BLUE_NETHER_BRICKS))
                    && "simpleoctets:blue_nether_bricks_octet".equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.result().item().value()).toString())),
                    "the Shapecutter cuts the blue nether bricks octet");
        }
        h.succeed();
    }

    /**
     * PLAN40 simpleblocks-3: the chiseled blue nether bricks like the chiseled red ones - 2 blue nether brick slabs on top of each other
     * make 1, the stonecutter (and so the Shapecutter) cuts 1 from 1 block; red nether brick properties; in the Vanilla tab right behind
     * the cracked blue nether bricks; no fence of the family.
     */
    private static void chiseledBlueNetherBricks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Block chiseled = BlocksRegistry.CHISELED_BLUE_NETHER_BRICKS;
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleBlocks.id("chiseled_blue_nether_bricks")) == chiseled && chiseled.asItem() != Items.AIR,
                "block and item registered");
        h.assertTrue(chiseled.defaultBlockState().getSoundType() == Blocks.RED_NETHER_BRICKS.defaultBlockState().getSoundType()
                && chiseled.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "red nether brick sound, mined with a pickaxe");
        ItemStack slab = new ItemStack(BlocksRegistry.BLUE_NETHER_BRICK_SLAB);
        expectCraft(h, level, 1, 2, List.of(slab, slab.copy()), chiseled, 1);
        h.assertTrue(cuts(level, new ItemStack(BlocksRegistry.BLUE_NETHER_BRICKS)).getOrDefault(chiseled.asItem(), 0) == 1,
                "stonecutter: blue nether bricks -> 1 chiseled");
        h.assertTrue(com.simpleblocks.saw.Sawing.forInput(com.simpleblocks.saw.Sawing.entries(level), new ItemStack(BlocksRegistry.BLUE_NETHER_BRICKS)).stream()
                .anyMatch(e -> e.result().item().value() == chiseled.asItem() && e.result().count() == 1), "the Shapecutter cuts it too");
        List<String> placed = BlocksRegistry.placements().stream().flatMap(p -> p.names().stream()).toList();
        h.assertTrue(placed.indexOf("chiseled_blue_nether_bricks") == placed.indexOf("cracked_blue_nether_bricks") + 1,
                "chiseled right behind the cracked blue nether bricks in the Vanilla tab: " + placed);
        h.assertTrue(!BuiltInRegistries.BLOCK.containsKey(SimpleBlocks.id("blue_nether_brick_fence")), "no fence (Vanilla's red nether bricks have none)");
        h.succeed();
    }

    /**
     * PLAN40 simpleblocks-3: stairs, slab and wall of the sculk tiles like the hushed tiles' shapes - their classes, deepslate tile
     * properties, crafting (stairs 6 -> 4, slab 3 -> 6, wall 6 -> 6), the stonecutter (and so the Shapecutter) 1 / 2 / 1 from a block,
     * the same animated texture as the tiles, and their place right behind the tiles (own tab and Vanilla tab).
     */
    private static void sculkTileShapes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.assertTrue(BlocksRegistry.SCULK_TILE_STAIRS instanceof StairBlock && BlocksRegistry.SCULK_TILE_SLAB instanceof SlabBlock
                && BlocksRegistry.SCULK_TILE_WALL instanceof WallBlock, "stairs, slab and wall classes");
        for (Block shape : List.of(BlocksRegistry.SCULK_TILE_STAIRS, BlocksRegistry.SCULK_TILE_SLAB, BlocksRegistry.SCULK_TILE_WALL)) {
            h.assertTrue(shape.defaultBlockState().getSoundType() == Blocks.DEEPSLATE_TILES.defaultBlockState().getSoundType()
                    && shape.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), shape + ": deepslate tile sound, pickaxe");
            h.assertTrue(!shape.defaultBlockState().is(BlockTags.DAMPENS_VIBRATIONS), shape + " does not dampen vibrations (unlike the hushed tiles)");
        }
        ItemStack tiles = new ItemStack(BlocksRegistry.SCULK_TILES);
        ItemStack air = ItemStack.EMPTY;
        expectCraft(h, level, 3, 3, List.of(tiles, air, air, tiles, tiles, air, tiles, tiles, tiles), BlocksRegistry.SCULK_TILE_STAIRS, 4);
        expectCraft(h, level, 3, 1, java.util.Collections.nCopies(3, tiles), BlocksRegistry.SCULK_TILE_SLAB, 6);
        expectCraft(h, level, 3, 2, java.util.Collections.nCopies(6, tiles), BlocksRegistry.SCULK_TILE_WALL, 6);
        java.util.Map<Item, Integer> made = cuts(level, tiles);
        h.assertTrue(made.getOrDefault(BlocksRegistry.SCULK_TILE_STAIRS.asItem(), 0) == 1 && made.getOrDefault(BlocksRegistry.SCULK_TILE_SLAB.asItem(), 0) == 2
                && made.getOrDefault(BlocksRegistry.SCULK_TILE_WALL.asItem(), 0) == 1, "stonecutter: 1 stairs, 2 slabs, 1 wall from sculk tiles: " + made);
        int at = BlocksRegistry.NAMES.indexOf("sculk_tiles");
        h.assertTrue(BlocksRegistry.NAMES.subList(at + 1, at + 4).equals(List.of("sculk_tile_stairs", "sculk_tile_slab", "sculk_tile_wall")),
                "own tab: the shapes right behind the tiles");
        List<String> placed = BlocksRegistry.placements().stream().flatMap(p -> p.names().stream()).toList();
        h.assertTrue(placed.indexOf("sculk_tile_stairs") == placed.indexOf("sculk_tiles") + 1 && placed.indexOf("sculk_tile_wall") == placed.indexOf("sculk_tiles") + 3,
                "Vanilla tab: the shapes right behind the tiles");
        h.succeed();
    }

    /** Sculk Tiles (W23-3): registered with its item, drops itself, stands in the Simple Blocks tab, 2 tiles + 2 sculk make 2. */
    private static void sculkTiles(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Block block = BlocksRegistry.SCULK_TILES;
        h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleBlocks.id("sculk_tiles")) == block && block.asItem() != Items.AIR, "block and item registered");
        h.assertTrue(block.defaultBlockState().getSoundType() == Blocks.DEEPSLATE_TILES.defaultBlockState().getSoundType()
                && block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE), "deepslate tile sound, mined with a pickaxe");
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        var drops = Block.getDrops(block.defaultBlockState(), level, pos, null, null, new ItemStack(Items.WOODEN_PICKAXE));
        h.assertTrue(drops.size() == 1 && drops.get(0).is(block.asItem()) && drops.get(0).getCount() == 1, "drops itself: " + drops);
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
        h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(SimpleBlocks.id("blocks")).getDisplayItems().stream().anyMatch(st -> st.is(block.asItem())),
                "in the Simple Blocks tab");
        ItemStack tiles = new ItemStack(Items.DEEPSLATE_TILES);
        ItemStack sculk = new ItemStack(Items.SCULK);
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(tiles, sculk, sculk, tiles));
        ItemStack made = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        h.assertTrue(made.is(block.asItem()) && made.getCount() == 2, "2 deepslate tiles + 2 sculk -> 2 sculk tiles: " + made);
        h.succeed();
    }

    /**
     * PAL14 (owner 09.11.): the missing palette members - sandstone pillars (pillar class, 2 sandstone -> 2), chiseled dark
     * prismarine (2 slabs -> 1), the ice families (4 ice -> 4 polished, 4 polished -> 4 bricks, stonecutter cuts) - registered with
     * their items and in the Vanilla tabs behind their family.
     */
    private static void pal14Members(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.assertTrue(BlocksRegistry.SANDSTONE_PILLAR instanceof net.minecraft.world.level.block.RotatedPillarBlock
                && BlocksRegistry.RED_SANDSTONE_PILLAR instanceof net.minecraft.world.level.block.RotatedPillarBlock, "sandstone pillars are pillars");
        h.assertTrue(BlocksRegistry.BASES.get("sandstone_pillar") == Blocks.SANDSTONE && BlocksRegistry.BASES.get("red_sandstone_pillar") == Blocks.RED_SANDSTONE
                && BlocksRegistry.BASES.get("chiseled_dark_prismarine") == Blocks.DARK_PRISMARINE, "properties copied from the Vanilla family");
        expectCraft(h, level, 1, 2, List.of(new ItemStack(Items.SANDSTONE), new ItemStack(Items.SANDSTONE)), BlocksRegistry.SANDSTONE_PILLAR, 2);
        expectCraft(h, level, 1, 2, List.of(new ItemStack(Items.RED_SANDSTONE), new ItemStack(Items.RED_SANDSTONE)), BlocksRegistry.RED_SANDSTONE_PILLAR, 2);
        expectCraft(h, level, 1, 2, List.of(new ItemStack(Items.DARK_PRISMARINE_SLAB), new ItemStack(Items.DARK_PRISMARINE_SLAB)),
                BlocksRegistry.BLOCKS.get("chiseled_dark_prismarine"), 1);
        for (Block ice : List.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE)) {
            String kind = BuiltInRegistries.BLOCK.getKey(ice).getPath();
            Block polished = BlocksRegistry.BLOCKS.get("polished_" + kind);
            Block bricks = BlocksRegistry.BLOCKS.get(kind + "_bricks");
            expectCraft(h, level, 2, 2, java.util.Collections.nCopies(4, new ItemStack(ice)), polished, 4);
            expectCraft(h, level, 2, 2, java.util.Collections.nCopies(4, new ItemStack(polished)), bricks, 4);
            Set<Item> cuts = new HashSet<>();
            var one = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(ice));
            for (var holder : level.getServer().getRecipeManager().getRecipes()) {
                if (holder.value() instanceof net.minecraft.world.item.crafting.StonecutterRecipe r && r.matches(one, level)) cuts.add(r.assemble(one).getItem());
            }
            for (String member : List.of("polished_" + kind, kind + "_bricks", kind + "_stairs", kind + "_slab", kind + "_wall")) {
                h.assertTrue(cuts.contains(BlocksRegistry.ITEMS.get(member)), "stonecutter: " + kind + " -> " + member + " (" + cuts + ")");
            }
            if (ice == Blocks.ICE) h.assertTrue(cuts.contains(BlocksRegistry.CHISELED_ICE.asItem()), "stonecutter: ice -> chiseled ice");
        }
        List<String> placed = BlocksRegistry.placements().stream().flatMap(p -> p.names().stream()).toList();
        for (String name : List.of("sandstone_pillar", "red_sandstone_pillar", "chiseled_dark_prismarine", "chiseled_ice", "blue_ice_wall")) {
            h.assertTrue(placed.contains(name), name + " has a place in a Vanilla tab");
        }
        h.succeed();
    }

    /**
     * PAL14 ice: every member is as slippery as its ice and sounds like it; only the members of normal ice melt (full blocks are
     * Vanilla ice blocks, the shapes tick randomly and melt to water), the packed and blue ice members never tick.
     */
    private static void pal14Ice(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (String name : BlocksRegistry.ICE) {
            Block block = BlocksRegistry.BLOCKS.get(name);
            Block base = BlocksRegistry.BASES.get(name);
            h.assertTrue(base == Blocks.ICE || base == Blocks.PACKED_ICE || base == Blocks.BLUE_ICE, name + " copies an ice");
            h.assertTrue(block.getFriction() == base.getFriction(), name + " slippery like " + base + ": " + block.getFriction());
            h.assertTrue(block.defaultBlockState().getSoundType() == base.defaultBlockState().getSoundType(), name + " sounds like its ice");
            boolean melts = base == Blocks.ICE;
            h.assertTrue(block.defaultBlockState().isRandomlyTicking() == melts, name + (melts ? " melts (random ticks)" : " never melts"));
            if (melts && !(block instanceof StairBlock || block instanceof SlabBlock || block instanceof WallBlock)) {
                h.assertTrue(block instanceof net.minecraft.world.level.block.IceBlock, name + " is a Vanilla ice block");
            }
        }
        h.assertTrue(BlocksRegistry.BLOCKS.get("blue_ice_slab").getFriction() > BlocksRegistry.BLOCKS.get("ice_slab").getFriction(), "blue ice slides furthest");
        // In the dark a random tick keeps the ice stairs; melting turns them into water (the overworld does not evaporate water).
        BlockPos rel = new BlockPos(2, 1, 2);
        h.setBlock(rel, BlocksRegistry.BLOCKS.get("ice_stairs"));
        BlockPos pos = h.absolutePos(rel);
        h.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) <= 11 - h.getBlockState(rel).getLightDampening(),
                "test spot is dark enough");
        h.getBlockState(rel).randomTick(level, pos, level.getRandom());
        h.assertTrue(h.getBlockState(rel).is(BlocksRegistry.BLOCKS.get("ice_stairs")), "dark: the ice stairs stay");
        com.simpleblocks.nature.IceShapes.melt(level, pos);
        h.assertTrue(h.getBlockState(rel).is(Blocks.WATER), "melted ice stairs become water: " + h.getBlockState(rel));
        h.setBlock(rel, Blocks.AIR);
        h.succeed();
    }

    /**
     * a-icebricks38 (owner 12.11.): the ice bricks are a family of their own - the stonecutter cuts the bricks into cracked bricks,
     * stairs, slabs (2), walls and chiseled ice, the ice and the polished ice straight into the brick shapes; two brick slabs make
     * the chiseled ice of the kind; the cracked bricks copy their ice, are ice (tag) and stand in the Vanilla tab behind the bricks.
     */
    private static void iceBrickFamily(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (Block ice : List.of(Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE)) {
            String kind = BuiltInRegistries.BLOCK.getKey(ice).getPath();
            Block cracked = BlocksRegistry.BLOCKS.get("cracked_" + kind + "_bricks");
            h.assertTrue(cracked != null && BlocksRegistry.BASES.get("cracked_" + kind + "_bricks") == ice, "cracked " + kind + " bricks copy " + kind);
            h.assertTrue(cracked.defaultBlockState().is(net.minecraft.tags.BlockTags.ICE), "cracked " + kind + " bricks are #ice");
            h.assertTrue((cracked instanceof net.minecraft.world.level.block.IceBlock) == (ice == Blocks.ICE), "only cracked normal ice bricks melt");
            Item bricks = BlocksRegistry.ITEMS.get(kind + "_bricks");
            Item chiseled = BuiltInRegistries.ITEM.getValue(com.simpleblocks.SimpleBlocks.id("chiseled_" + kind));
            java.util.Map<Item, Integer> fromBricks = cuts(level, new ItemStack(bricks));
            h.assertTrue(fromBricks.getOrDefault(cracked.asItem(), 0) == 1, "stonecutter: " + kind + " bricks -> cracked (" + fromBricks + ")");
            h.assertTrue(fromBricks.getOrDefault(chiseled, 0) == 1, "stonecutter: " + kind + " bricks -> chiseled " + kind);
            for (Item source : List.of(bricks, ice.asItem(), BlocksRegistry.ITEMS.get("polished_" + kind))) {
                java.util.Map<Item, Integer> made = source == bricks ? fromBricks : cuts(level, new ItemStack(source));
                for (String shape : List.of("stairs", "slab", "wall")) {
                    Item out = BlocksRegistry.ITEMS.get(kind + "_brick_" + shape);
                    h.assertTrue(made.getOrDefault(out, 0) == (shape.equals("slab") ? 2 : 1), "stonecutter: " + source + " -> " + out + " (" + made + ")");
                }
            }
            Item slab = BlocksRegistry.ITEMS.get(kind + "_brick_slab");
            expectCraft(h, level, 1, 2, List.of(new ItemStack(slab), new ItemStack(slab)), BuiltInRegistries.BLOCK.getValue(com.simpleblocks.SimpleBlocks.id("chiseled_" + kind)), 1);
            expectCraft(h, level, 3, 1, java.util.Collections.nCopies(3, new ItemStack(bricks)), BlocksRegistry.BLOCKS.get(kind + "_brick_slab"), 6);
        }
        List<String> placed = BlocksRegistry.placements().stream().flatMap(p -> p.names().stream()).toList();
        for (String name : List.of("cracked_ice_bricks", "ice_brick_wall", "cracked_packed_ice_bricks", "packed_ice_brick_stairs", "cracked_blue_ice_bricks", "blue_ice_brick_slab")) {
            h.assertTrue(placed.contains(name), name + " has a place in a Vanilla tab");
        }
        h.assertTrue(placed.indexOf("cracked_ice_bricks") == placed.indexOf("ice_bricks") + 1, "cracked ice bricks right behind the ice bricks");
        h.succeed();
    }

    /** Stonecutter results (item -> count) of one input. */
    private static java.util.Map<Item, Integer> cuts(ServerLevel level, ItemStack input) {
        java.util.Map<Item, Integer> out = new java.util.HashMap<>();
        var one = new net.minecraft.world.item.crafting.SingleRecipeInput(input);
        for (var holder : level.getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof net.minecraft.world.item.crafting.StonecutterRecipe r && r.matches(one, level)) {
                ItemStack made = r.assemble(one);
                out.merge(made.getItem(), made.getCount(), Math::max);
            }
        }
        return out;
    }

    /**
     * N89b: the nether brick fence gate - a real fence gate with the nether brick fence's values (hardness, blast resistance, sound,
     * pickaxe, correct tool needed), not axe-mineable (so not in {@code #fence_gates}); the nether brick fence connects to it, it opens and
     * lets mobs through when open; "#W#" twice (nether brick, nether bricks) crafts one; right behind the fence in the Vanilla tab.
     */
    private static void netherBrickFenceGate(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Block gate = BlocksRegistry.NETHER_BRICK_FENCE_GATE;
        BlockState fence = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
        BlockState state = gate.defaultBlockState();
        h.assertTrue(gate instanceof net.minecraft.world.level.block.FenceGateBlock && BuiltInRegistries.BLOCK.getValue(SimpleBlocks.id("nether_brick_fence_gate")) == gate
                && gate.asItem() != Items.AIR, "a fence gate block with its item");
        h.assertTrue(gate.defaultDestroyTime() == Blocks.NETHER_BRICK_FENCE.defaultDestroyTime()
                && gate.getExplosionResistance() == Blocks.NETHER_BRICK_FENCE.getExplosionResistance()
                && state.getSoundType() == fence.getSoundType() && state.requiresCorrectToolForDrops(), "the nether brick fence's hardness, resistance, sound, tool rule");
        h.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE) && !state.is(BlockTags.MINEABLE_WITH_AXE) && !state.is(BlockTags.FENCE_GATES)
                && !state.ignitedByLava(), "pickaxe only, not a wooden gate, does not burn");
        BlockPos gatePos = new BlockPos(2, 1, 2);
        BlockPos fencePos = new BlockPos(1, 1, 2);
        h.setBlock(gatePos, state.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
        h.setBlock(fencePos, fence);
        BlockState connected = Block.updateFromNeighbourShapes(h.getBlockState(fencePos), level, h.absolutePos(fencePos));
        h.assertTrue(connected.getValue(net.minecraft.world.level.block.FenceBlock.EAST), "the nether brick fence connects to the gate");
        BlockState closed = h.getBlockState(gatePos);
        BlockState opened = closed.setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, true);
        h.assertTrue(!closed.getCollisionShape(level, h.absolutePos(gatePos)).isEmpty() && opened.getCollisionShape(level, h.absolutePos(gatePos)).isEmpty(),
                "closed it blocks, open it lets through");
        ItemStack brick = new ItemStack(Items.NETHER_BRICK);
        ItemStack bricks = new ItemStack(Items.NETHER_BRICKS);
        expectCraft(h, level, 3, 2, List.of(brick, bricks, brick.copy(), brick.copy(), bricks.copy(), brick.copy()), gate, 1);
        List<String> placed = BlocksRegistry.placements().stream().filter(p -> p.before() == Items.NETHER_BRICK_FENCE && p.after())
                .flatMap(p -> p.names().stream()).toList();
        h.assertTrue(placed.equals(List.of("nether_brick_fence_gate")), "right behind the nether brick fence in the Vanilla tab: " + placed);
        h.succeed();
    }

    private static void expectCraft(GameTestHelper h, ServerLevel level, int w, int hgt, List<ItemStack> grid, Block result, int count) {
        var input = net.minecraft.world.item.crafting.CraftingInput.of(w, hgt, grid);
        ItemStack made = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        h.assertTrue(made.is(result.asItem()) && made.getCount() == count, grid + " -> " + count + " " + result + ", got " + made);
    }

    static {
        ALL.put("pal14_members", BlocksTests::pal14Members);
        ALL.put("pal14_ice", BlocksTests::pal14Ice);
        ALL.put("icebricks38_family", BlocksTests::iceBrickFamily);
        ALL.put("blue_nether_bricks_recipes", BlocksTests::blueNetherBricksRecipes);
        ALL.put("sculk_tiles", BlocksTests::sculkTiles);
        ALL.put("sculk_tile_shapes", BlocksTests::sculkTileShapes);   // PLAN40 simpleblocks-3
        ALL.put("chiseled_blue_nether_bricks", BlocksTests::chiseledBlueNetherBricks);
        ALL.put("nether_brick_fence_gate", BlocksTests::netherBrickFenceGate);   // N89b
        ALL.put("discover_recipe_unlocks", com.simpleblocks.discover.BlocksDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleblocks.discover.BlocksDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simpleblocks.discover.BlocksDiscover::firstPickupTips);
        com.simpleblocks.saw.SawTests.register(ALL, MAX_TICKS);
        // The building blocks that moved here from SimpleBuilding (b-blocksmove).
        NatureTests.register(ALL, MAX_TICKS);
        SnowTests.register(ALL, MAX_TICKS);
        PaleTests.register(ALL, MAX_TICKS); // DW04B pale blocks on SimpleLib's aging chains
        CompactTests.register(ALL, MAX_TICKS); // WH26-10 Compact and Dense blocks
        MetalTests.register(ALL, MAX_TICKS); // PAL35b copper tiles/pillar, gold/iron families, chiseled ore blocks
    }

    private BlocksTests() {}
}
