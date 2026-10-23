package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.HiddenOreBlock;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Owner requests W24-4 (23.10., docs/ai/PLAN-A-MECH24-2026-10-23.md): line End rods, hidden ores freed with the brush. */
public final class Mech24Tests {
    private Mech24Tests() {
    }

    /**
     * The Astralit / Nihilit End rods behave like Vanilla's: they face away from the clicked face, glow like the End rod,
     * a blaze rod over the line material crafts four, and they drop themselves.
     */
    public static void lineEndRodsGlowAndFaceLikeVanilla(GameTestHelper helper) {
        if (ModBlocks.ASTRALIT_END_ROD == null) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos support = new BlockPos(2, 2, 2);
        helper.setBlock(support, Blocks.STONE);
        BlockPos abs = helper.absolutePos(support);
        var recipes = level.getServer().getRecipeManager();
        Object[][] rods = {{ModBlocks.ASTRALIT_END_ROD, ModItems.ASTRALIT_DUST}, {ModBlocks.NIHILITH_END_ROD, ModItems.NIHILITH_SHARD}};
        for (Object[] pair : rods) {
            Block rod = (Block) pair[0];
            Item material = (Item) pair[1];
            helper.assertTrue(rod instanceof EndRodBlock, rod + " is an End rod");
            for (Direction face : Direction.values()) {
                BlockPlaceContext ctx = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(rod),
                        new BlockHitResult(Vec3.atCenterOf(abs), face, abs, false));
                BlockState placed = rod.getStateForPlacement(ctx);
                helper.assertTrue(placed != null && placed.getValue(EndRodBlock.FACING) == face, rod + " clicked on " + face + " faces " + placed);
            }
            BlockState up = rod.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP);
            helper.setBlock(support.above(), up);
            helper.assertTrue(helper.getBlockState(support.above()).getLightEmission() == Blocks.END_ROD.defaultBlockState().getLightEmission(),
                    rod + " glows like the End rod: " + helper.getBlockState(support.above()).getLightEmission());
            var input = CraftingInput.of(1, 2, List.of(new ItemStack(Items.BLAZE_ROD), new ItemStack(material)));
            ItemStack out = recipes.getRecipeFor(RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
            helper.assertTrue(out.is(rod.asItem()) && out.getCount() == 4, "blaze rod over " + material + " gave " + out);
            List<ItemStack> drops = Block.getDrops(up, level, helper.absolutePos(support.above()), null);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(rod.asItem()), rod + " drops " + drops);
        }
        helper.succeed();
    }

    /**
     * A test player stands on the ore looking down and brushes with the real Vanilla brush tick: six strokes free it
     * stage by stage, the last costs the brush one durability, more strokes change nothing, a dull ore stays as it is.
     */
    public static void hiddenOreBrushesFreeInSixStrokes(GameTestHelper helper) {
        if (!McVersion.BRUSHED_ORES) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos ore = new BlockPos(2, 1, 2);
        BlockPos abs = helper.absolutePos(ore);
        List<Block> ores = new java.util.ArrayList<>(List.of(ModBlocks.ASTRALIT_ORE, ModBlocks.NIHILITH_ORE));
        for (Block block : ores) {
            helper.setBlock(ore, block);
            ItemStack brush = new ItemStack(Items.BRUSH);
            for (int stroke = 1; stroke <= HiddenOreBlock.STROKES + 2; stroke++) {
                brushOnce(player, abs, brush);
                int got = level.getBlockState(abs).getValue(HiddenOreBlock.BRUSHED);
                helper.assertTrue(got == Math.min(stroke, HiddenOreBlock.STROKES), block + " after " + stroke + " strokes is at " + got);
                helper.assertTrue(brush.getDamageValue() == (stroke >= HiddenOreBlock.STROKES ? 1 : 0),
                        block + ": brush damage " + brush.getDamageValue() + " after " + stroke + " strokes");
            }
            helper.assertTrue(HiddenOreBlock.freed(level.getBlockState(abs)), block + " is freed");
        }
        if (ModBlocks.DULL_ASTRALIT_ORE != null) {
            helper.setBlock(ore, ModBlocks.DULL_ASTRALIT_ORE);
            BlockState before = level.getBlockState(abs);
            brushOnce(player, abs, new ItemStack(Items.BRUSH));
            helper.assertTrue(level.getBlockState(abs) == before, "a dull ore stays dull under the brush");
        }
        helper.succeed();
    }

    /** One Vanilla brush stroke: the player stands on top of {@code abs} looking straight down; the tick that strokes. */
    private static void brushOnce(ServerPlayer player, BlockPos abs, ItemStack brush) {
        player.snapTo(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 0.5, 0.0F, 90.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, brush);
        // BrushItem strokes when (useDuration - remaining + 1) % 10 == 5
        int remaining = brush.getItem().getUseDuration(brush, player) - 4;
        brush.getItem().onUseTick(player.level(), player, brush, remaining);
    }

    /**
     * Drop amounts (formula in the plan): freed = Vanilla ore drops with Fortune (1 / 2.2 with Fortune III), hidden =
     * the same with a 70 % chance (0.7 / 1.54); freed with Fortune the most. Silk Touch gives the ore.
     */
    public static void hiddenOreDropsLessUnlessBrushed(GameTestHelper helper) {
        if (!McVersion.BRUSHED_ORES) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(new BlockPos(2, 1, 2));
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack plain = new ItemStack(Items.DIAMOND_PICKAXE);
        ItemStack fortune = new ItemStack(Items.DIAMOND_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), 3);
        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        Object[][] ores = {{ModBlocks.ASTRALIT_ORE, ModItems.ASTRALIT_DUST}, {ModBlocks.NIHILITH_ORE, ModItems.NIHILITH_SHARD}};
        for (Object[] pair : ores) {
            Block ore = (Block) pair[0];
            Item drop = (Item) pair[1];
            BlockState hidden = ore.defaultBlockState();
            BlockState freed = hidden.setValue(HiddenOreBlock.BRUSHED, HiddenOreBlock.STROKES);
            BlockState half = hidden.setValue(HiddenOreBlock.BRUSHED, 3);
            double freedPlain = mean(helper, level, freed, abs, plain, drop), freedLuck = mean(helper, level, freed, abs, fortune, drop);
            double hiddenPlain = mean(helper, level, hidden, abs, plain, drop), hiddenLuck = mean(helper, level, hidden, abs, fortune, drop);
            double halfPlain = mean(helper, level, half, abs, plain, drop);
            String all = ore + ": freed " + freedPlain + " / Fortune III " + freedLuck + ", hidden " + hiddenPlain + " / " + hiddenLuck + ", half " + halfPlain;
            helper.assertTrue(near(freedPlain, 1.0) && near(freedLuck, 2.2) && near(hiddenPlain, 0.7) && near(hiddenLuck, 1.54)
                    && near(halfPlain, 0.7), all);
            helper.assertTrue(freedLuck > hiddenLuck && hiddenLuck > freedPlain && freedPlain > hiddenPlain, "order: " + all);
            for (BlockState state : List.of(hidden, freed)) {
                List<ItemStack> silkDrops = Block.getDrops(state, level, abs, null, null, silk);
                helper.assertTrue(silkDrops.size() == 1 && silkDrops.get(0).is(ore.asItem()), ore + " with Silk Touch drops " + silkDrops);
            }
        }
        helper.succeed();
    }

    private static final int ROLLS = 2000;

    private static double mean(GameTestHelper helper, ServerLevel level, BlockState state, BlockPos abs, ItemStack tool, Item drop) {
        long total = 0;
        for (int i = 0; i < ROLLS; i++) {
            for (ItemStack s : Block.getDrops(state, level, abs, null, null, tool)) {
                helper.assertTrue(s.is(drop), state + " dropped " + s);
                total += s.getCount();
            }
        }
        return (double) total / ROLLS;
    }

    private static boolean near(double got, double want) {
        return Math.abs(got - want) <= want * 0.08;
    }
}
