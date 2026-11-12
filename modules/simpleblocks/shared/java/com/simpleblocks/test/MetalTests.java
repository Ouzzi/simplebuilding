package com.simpleblocks.test;

import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.metal.MetalBlocks;
import com.simplelib.api.aging.Aging;
import com.simplelib.api.aging.AgingChain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * PAL35b (docs/ai/PLAN-A-PAL35-2026-11-12.md part b): copper tiles and pillar weather like Vanilla copper on SimpleLib's aging chains (every
 * stage waxable, the end stage too), the gold/iron families and the chiseled ore blocks behave like their storage block. All on the module's
 * empty 8 x 8 x 8 structure; the aging block in the middle, alone within the 4-block neighbour scan.
 */
public final class MetalTests {
    private static final BlockPos MID = new BlockPos(4, 2, 4);

    private MetalTests() {}

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("metal_copper_weathers", MetalTests::copperWeathers);
        all.put("metal_copper_wax_and_axe", MetalTests::copperWaxAndAxe);
        all.put("metal_copper_blocks", MetalTests::copperBlocks);
        all.put("metal_families", MetalTests::families);
    }

    /** Random ticks age copper tiles through exposed, weathered and oxidized (Vanilla chance and neighbour rule); waxed tiles never age. */
    public static void copperWeathers(GameTestHelper helper) {
        AgingChain tiles = MetalBlocks.COPPER_TILES;
        helper.setBlock(MID, tiles.base());
        helper.assertTrue(tiles.base().defaultBlockState().isRandomlyTicking(), "copper tiles random-tick (aging chain base)");
        for (int i = 0; i < 600 && !helper.getBlockState(MID).is(tiles.stages().get(3)); i++) tick(helper, MID);
        helper.assertTrue(helper.getBlockState(MID).is(tiles.stages().get(3)), "600 random ticks: oxidized copper tiles, is " + helper.getBlockState(MID));
        helper.assertTrue(!helper.getBlockState(MID).isRandomlyTicking(), "the oxidized end stage does not tick");
        helper.setBlock(MID, tiles.waxed().get(1));
        for (int i = 0; i < 200; i++) tick(helper, MID);
        helper.assertTrue(helper.getBlockState(MID).is(tiles.waxed().get(1)), "waxed exposed tiles keep their look, is " + helper.getBlockState(MID));
        helper.assertValueEqual(MetalBlocks.names("copper_tiles").subList(0, 3), List.of("exposed_copper_tiles", "weathered_copper_tiles", "oxidized_copper_tiles"),
                "stage names");
        helper.succeed();
    }

    /** Honeycomb waxes every stage including the oxidized one (Vanilla's waxed_oxidized), the axe takes the wax off, then one stage back; the pillar keeps its axis. */
    public static void copperWaxAndAxe(GameTestHelper helper) {
        AgingChain pillar = MetalBlocks.COPPER_PILLAR;
        helper.assertValueEqual(pillar.waxed().size(), 4, "four waxed pillar stages (end stage too)");
        helper.setBlock(MID, pillar.stages().get(3).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        ServerPlayer player = survivor(helper);
        ItemStack comb = new ItemStack(Items.HONEYCOMB, 2);
        use(helper, player, comb, MID);
        BlockState waxed = helper.getBlockState(MID);
        helper.assertTrue(waxed.is(pillar.waxed().get(3)) && waxed.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
                "honeycomb waxed the oxidized pillar (axis kept), is " + waxed);
        helper.assertValueEqual(comb.getCount(), 1, "one honeycomb used");
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        use(helper, player, axe, MID);
        helper.assertTrue(helper.getBlockState(MID).is(pillar.stages().get(3)), "axe took the wax off, is " + helper.getBlockState(MID));
        use(helper, player, axe, MID);
        helper.assertTrue(helper.getBlockState(MID).is(pillar.stages().get(2)), "axe scraped back to weathered, is " + helper.getBlockState(MID));
        helper.assertTrue(Aging.entry(pillar.waxed().get(3)) != null && Aging.entry(pillar.waxed().get(3)).waxed(), "waxed oxidized pillar is in SimpleLib's chains");
        helper.succeed();
    }

    /** Classes, map colours, tags, tool tier, drops, tab order and the crafting/stonecutter recipes of every copper stage. */
    public static void copperBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MapColor[] colours = {MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.WARPED_STEM, MapColor.WARPED_NYLIUM};
        List<ItemStack> tab = BlocksRegistry.tabStacks();
        int count = 0;
        for (AgingChain chain : MetalBlocks.CHAINS) {
            boolean pillar = chain == MetalBlocks.COPPER_PILLAR;
            List<Block> all = new ArrayList<>(chain.stages());
            all.addAll(chain.waxed());
            for (Block block : all) {
                count++;
                BlockState state = block.defaultBlockState();
                helper.assertTrue(pillar == (block instanceof RotatedPillarBlock), block + ": pillar class only for the pillar");
                helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.is(BlockTags.NEEDS_STONE_TOOL), block + ": stone pickaxe like copper");
                helper.assertTrue(state.getSoundType() == vanilla("cut_copper").defaultBlockState().getSoundType(), block + " sounds like copper");
                Aging.Entry e = Aging.entry(block);
                helper.assertTrue(e != null && state.getMapColor(level, BlockPos.ZERO) == colours[e.stage()], block + ": map colour of its stage");
                List<ItemStack> drops = Block.getDrops(state, level, helper.absolutePos(MID), null, null, new ItemStack(Items.STONE_PICKAXE));
                helper.assertTrue(drops.size() == 1 && drops.getFirst().is(block.asItem()), block + " drops itself: " + drops);
                helper.assertTrue(tab.stream().anyMatch(s -> s.is(block.asItem())), block + " in the Simple Blocks tab");
            }
        }
        helper.assertValueEqual(count, 16, "copper tiles and pillar: 4 stages + 4 waxed each");
        // recipes: 4 cut copper of a stage -> 4 tiles of it, 2 copper blocks of a stage upright -> 2 pillars, waxed from the waxed Vanilla copper
        expectCraft(helper, 2, 2, List.of(stack("exposed_cut_copper"), stack("exposed_cut_copper"), stack("exposed_cut_copper"),
                stack("exposed_cut_copper")), MetalBlocks.COPPER_TILES.stages().get(1), 4);
        expectCraft(helper, 2, 2, List.of(stack("waxed_oxidized_cut_copper"), stack("waxed_oxidized_cut_copper"), stack("waxed_oxidized_cut_copper"),
                stack("waxed_oxidized_cut_copper")), MetalBlocks.COPPER_TILES.waxed().get(3), 4);
        expectCraft(helper, 1, 2, List.of(stack("copper_block"), stack("copper_block")), MetalBlocks.COPPER_PILLAR.base(), 2);
        expectCraft(helper, 1, 2, List.of(stack("waxed_weathered_copper"), stack("waxed_weathered_copper")), MetalBlocks.COPPER_PILLAR.waxed().get(2), 2);
        expectCut(helper, vanilla("oxidized_copper").asItem(), MetalBlocks.COPPER_TILES.stages().get(3), 4);
        expectCut(helper, vanilla("copper_block").asItem(), MetalBlocks.COPPER_PILLAR.base(), 1);
        helper.succeed();
    }

    /** Gold and iron families and the chiseled ore blocks: classes, tool tier of the block, redstone power, fire resistance, recipes. */
    public static void families(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (String m : List.of("gold", "iron")) {
            Block storage = m.equals("gold") ? Blocks.GOLD_BLOCK : Blocks.IRON_BLOCK;
            for (String name : List.of("cut_" + m, "cut_" + m + "_stairs", "cut_" + m + "_slab", m + "_tiles", m + "_pillar", "chiseled_" + m)) {
                Block block = BlocksRegistry.BLOCKS.get(name);
                BlockState state = block.defaultBlockState();
                helper.assertTrue(state.is(m.equals("gold") ? BlockTags.NEEDS_IRON_TOOL : BlockTags.NEEDS_STONE_TOOL), name + ": tool tier like the " + m + " block");
                helper.assertTrue(block.defaultDestroyTime() == storage.defaultDestroyTime() && state.getSoundType() == storage.defaultBlockState().getSoundType(),
                        name + ": hardness and sound of the " + m + " block");
            }
            helper.assertTrue(BlocksRegistry.BLOCKS.get(m + "_pillar") instanceof RotatedPillarBlock, m + " pillar turns like a log");
            Item blockItem = storage.asItem();
            expectCraft(helper, 2, 2, List.of(new ItemStack(blockItem), new ItemStack(blockItem), new ItemStack(blockItem), new ItemStack(blockItem)),
                    BlocksRegistry.BLOCKS.get("cut_" + m), 4);
            ItemStack slab = new ItemStack(BlocksRegistry.ITEMS.get("cut_" + m + "_slab"));
            expectCraft(helper, 1, 2, List.of(slab, slab.copy()), BlocksRegistry.BLOCKS.get("chiseled_" + m), 1);
            expectCut(helper, blockItem, BlocksRegistry.BLOCKS.get(m + "_tiles"), 4);
        }
        Block redstone = BlocksRegistry.BLOCKS.get("chiseled_redstone");
        helper.setBlock(MID, redstone);
        helper.assertTrue(redstone.defaultBlockState().isSignalSource()
                && level.getSignal(helper.absolutePos(MID), Direction.EAST) == 15, "chiseled redstone powers like the redstone block");
        helper.setBlock(MID, Blocks.AIR);
        helper.assertTrue(BlocksRegistry.ITEMS.get("chiseled_netherite").components().has(DataComponents.DAMAGE_RESISTANT), "chiseled netherite survives fire");
        helper.assertTrue(BlocksRegistry.BLOCKS.get("chiseled_netherite").defaultBlockState().is(BlockTags.NEEDS_DIAMOND_TOOL)
                && BlocksRegistry.BLOCKS.get("chiseled_diamond").defaultBlockState().is(BlockTags.NEEDS_IRON_TOOL), "tool tiers of netherite and diamond");
        for (String gem : List.of("chiseled_diamond", "chiseled_emerald", "chiseled_lapis", "chiseled_coal", "chiseled_netherite")) {
            expectCut(helper, BlocksRegistry.BASES.get(gem).asItem(), BlocksRegistry.BLOCKS.get(gem), 1);
            helper.assertTrue(!BlocksRegistry.BLOCKS.get(gem).defaultBlockState().is(BlockTags.BEACON_BASE_BLOCKS), gem + ": no beacon base (A12)");
        }
        helper.succeed();
    }

    // --- helpers --------------------------------------------------------------------------------------------------------

    /** A Vanilla block by id (26.3 groups the copper blocks per weathering state; the ids stay the Vanilla ones). */
    private static Block vanilla(String path) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(path));
    }

    private static ItemStack stack(String vanillaPath) {
        return new ItemStack(vanilla(vanillaPath).asItem());
    }

    private static void tick(GameTestHelper helper, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        helper.getBlockState(pos).randomTick(level, helper.absolutePos(pos), level.getRandom());
    }

    private static ServerPlayer survivor(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** Uses the stack on the top face of {@code target} like a player (the server's full use path: block state, then item). */
    private static void use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos target) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos abs = helper.absolutePos(target);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    private static void expectCraft(GameTestHelper helper, int w, int h, List<ItemStack> grid, Block result, int count) {
        CraftingInput input = CraftingInput.of(w, h, grid);
        ServerLevel level = helper.getLevel();
        ItemStack made = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        helper.assertTrue(made.is(result.asItem()) && made.getCount() == count, grid + " -> " + count + " " + result + ", got " + made);
    }

    private static void expectCut(GameTestHelper helper, Item source, Block result, int count) {
        SingleRecipeInput one = new SingleRecipeInput(new ItemStack(source));
        boolean found = false;
        for (var holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof StonecutterRecipe r && r.matches(one, helper.getLevel())) {
                ItemStack out = r.assemble(one);
                found |= out.is(result.asItem()) && out.getCount() == count;
            }
        }
        helper.assertTrue(found, "stonecutter: " + source + " -> " + count + " " + result);
    }
}
