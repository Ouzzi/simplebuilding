package com.simplebuilding.gametest;

import com.simplebuilding.compat.InWorldRecipeCatalog;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.AnvilDiamondCrushing;
import com.simplebuilding.version.McVersion;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class AnvilCrushingTests {
    private AnvilCrushingTests() {}

    public static void anvilsConsumeOnce(GameTestHelper helper) {
        List<Block> anvils = List.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
        for (int i = 0; i < anvils.size(); i++) {
            int x = 1 + i * 2;
            helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 3), Blocks.DIAMOND_BLOCK);
            drop(helper, new BlockPos(x, i == 0 ? 4 : 5, 3), anvils.get(i));
        }
        helper.startSequence().thenExecuteAfter(45, () -> {
            assertPebbles(helper, McVersion.ANVIL_DIAMOND_CRUSH ? 216 : 0);
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).stream()
                    .noneMatch(e -> e.getItem().is(Items.DIAMOND_BLOCK)), "consumed blocks must not drop themselves");
            // Remove the first output so a second fall must produce exactly zero new pebbles.
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds())
                    .forEach(ItemEntity::discard);
            for (int i = 0; i < anvils.size(); i++) {
                BlockPos target = new BlockPos(1 + i * 2, 2, 3);
                helper.assertTrue(McVersion.ANVIL_DIAMOND_CRUSH
                                ? !helper.getBlockState(target).is(Blocks.DIAMOND_BLOCK)
                                : helper.getBlockState(target).is(Blocks.DIAMOND_BLOCK),
                        "diamond block consumed only on 26.3");
                if (McVersion.ANVIL_DIAMOND_CRUSH && i == 0) {
                    helper.assertTrue(helper.getBlockState(target).is(BlockTags.ANVIL),
                            "the initially intact anvil survives these two short falls");
                }
                // A worn anvil may break on its second fall into the consumed block's space.
                if (McVersion.ANVIL_DIAMOND_CRUSH) {
                    helper.setBlock(target, Blocks.AIR);
                    drop(helper, target.above(3), anvils.get(i));
                }
            }
        }).thenExecuteAfter(45, () -> assertPebbles(helper, 0))
                .thenSucceed();
    }

    public static void catalogAndRecipesConserveDiamonds(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipes = level.getServer().getRecipeManager();
        var grid = CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(ModItems.DIAMOND_PEBBLE)));
        var cracked = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow().value().assemble(grid);
        helper.assertTrue(cracked.is(ModItems.CRACKED_DIAMOND) && cracked.getCount() == 1,
                "nine pebbles must craft exactly one cracked diamond");
        var input = new SingleRecipeInput(cracked);
        var diamond = recipes.getRecipeFor(RecipeType.BLASTING, input, level).orElseThrow().value().assemble(input);
        helper.assertTrue(diamond.is(Items.DIAMOND) && diamond.getCount() == 1,
                "one cracked diamond must blast into exactly one diamond");
        helper.assertTrue(AnvilDiamondCrushing.PEBBLES == 72 && AnvilDiamondCrushing.PEBBLES < 9 * 9,
                "automatic crushing must lose one diamond per block");
        var catalog = InWorldRecipeCatalog.build();
        helper.assertTrue(catalog.problems().isEmpty(), "recipe catalog: " + catalog.problems());
        var entry = catalog.entries().stream().filter(e -> e.id().equals("anvil_diamond_crush/minecraft:diamond_block"))
                .findFirst();
        helper.assertTrue(entry.isPresent() == McVersion.ANVIL_DIAMOND_CRUSH, "anvil recipe follows version flag");
        if (entry.isPresent()) {
            var recipe = entry.get();
            helper.assertTrue(recipe.output().count() == AnvilDiamondCrushing.PEBBLES
                            && recipe.output().items().equals(List.of(ModItems.DIAMOND_PEBBLE))
                            && recipe.inputs().equals(List.of(InWorldRecipeCatalog.Stack.of(Items.DIAMOND_BLOCK, 1)))
                            && recipe.tools().equals(List.of(Items.ANVIL, Items.CHIPPED_ANVIL, Items.DAMAGED_ANVIL)),
                    "JEI and REI must show the actual input, yield and all three anvils");
        }
        helper.succeed();
    }

    public static void invalidFallsDoNotCrush(GameTestHelper helper) {
        for (int x : new int[]{1, 3, 5}) helper.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 2, 3), Blocks.DIAMOND_BLOCK);
        drop(helper, new BlockPos(1, 5, 3), Blocks.SAND);
        helper.setBlock(new BlockPos(3, 2, 3), Blocks.GOLD_BLOCK);
        drop(helper, new BlockPos(3, 5, 3), Blocks.ANVIL);
        helper.setBlock(new BlockPos(5, 2, 3), Blocks.DIAMOND_BLOCK);
        // Even an entity spawned directly on the target is not a fall of at least one block.
        BlockPos touching = new BlockPos(5, 3, 3);
        helper.setBlock(touching, Blocks.ANVIL);
        FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(touching), Blocks.ANVIL.defaultBlockState());
        helper.startSequence().thenExecuteAfter(45, () -> {
            helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(1, 2, 3));
            helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(3, 2, 3));
            helper.assertBlockPresent(Blocks.DIAMOND_BLOCK, new BlockPos(5, 2, 3));
            assertPebbles(helper, 0);
        }).thenSucceed();
    }

    private static void drop(GameTestHelper helper, BlockPos pos, Block block) {
        // Let the scheduled vanilla block tick create the FallingBlockEntity, including anvil damage.
        helper.setBlock(pos, block);
        if (block == Blocks.DAMAGED_ANVIL) {
            // Keep this variant deterministic: a natural damaged anvil can randomly break before onLand.
            FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(pos), block.defaultBlockState());
        }
    }

    private static void assertPebbles(GameTestHelper helper, int expected) {
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds());
        int total = 0;
        for (var drop : drops) {
            if (!drop.getItem().is(ModItems.DIAMOND_PEBBLE)) continue;
            helper.assertTrue(drop.getItem().getCount() <= drop.getItem().getMaxStackSize(), "legal pebble stack size");
            total += drop.getItem().getCount();
        }
        helper.assertValueEqual(total, expected, "pebbles after actual anvil fall");
    }
}
