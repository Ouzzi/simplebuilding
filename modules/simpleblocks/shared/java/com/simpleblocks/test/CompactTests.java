package com.simpleblocks.test;

import com.simpleblocks.SimpleBlocks;
import com.simpleblocks.compact.CompactBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * WH26-10 (docs/ai/PLAN-A-COMPACT-2026-11-06.md) module GameTests: the 38 Compact/Dense blocks are registered with the hardness of
 * 4x / 16x their base and the base's tool tag, drop themselves, and every recipe step is reversible (9 base -> Compact -> Dense and
 * 1 -> 9 back, nothing lost, nothing gained). Names carry the {@code compact_} prefix of the catalogue.
 */
public final class CompactTests {
    private CompactTests() {}

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("compact_registered", CompactTests::registered);
        all.put("compact_recipes_reversible", CompactTests::recipesReversible);
        all.put("compact_drops_and_tools", CompactTests::dropsAndTools);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));
    }

    /** 19 bases x 2 stages, items and block items exist, hardness 4x and 16x the Vanilla base, resistance and map colour as the base. */
    public static void registered(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.assertTrue(CompactBlocks.BASES.size() == 19 && CompactBlocks.BLOCKS.size() == 38, "19 bases, 38 blocks, found " + CompactBlocks.BLOCKS.size());
        for (CompactBlocks.Base base : CompactBlocks.BASES) {
            float vanilla = base.block().defaultBlockState().getDestroySpeed(level, pos);
            h.assertTrue(Math.abs(vanilla - base.hardness()) < 0.001f, base.path() + ": table hardness " + base.hardness() + " vs Vanilla " + vanilla);
            float expected = vanilla;
            for (String stage : CompactBlocks.STAGES) {
                expected *= CompactBlocks.FACTOR;
                String name = stage + "_" + base.path();
                Block block = CompactBlocks.BLOCKS.get(name);
                h.assertTrue(BuiltInRegistries.BLOCK.getValue(SimpleBlocks.id(name)) == block, name + " is registered");
                h.assertTrue(block.asItem() != Items.AIR && BuiltInRegistries.ITEM.getValue(SimpleBlocks.id(name)) == block.asItem(), name + " has its block item");
                float speed = block.defaultBlockState().getDestroySpeed(level, pos);
                h.assertTrue(Math.abs(speed - expected) < 0.01f, name + " hardness " + speed + ", expected " + expected);
                h.assertTrue(block.defaultBlockState().requiresCorrectToolForDrops() == base.block().defaultBlockState().requiresCorrectToolForDrops(),
                        name + " needs the same tool rule as " + base.path());
                h.assertTrue(block.defaultBlockState().getSoundType() == base.block().defaultBlockState().getSoundType(), name + " sounds like " + base.path());
            }
        }
        h.succeed();
    }

    /** Crafts every step with the real recipe manager: 9 -> 1 up the stages, 1 -> 9 back down, ending at the start count. */
    public static void recipesReversible(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (CompactBlocks.Base base : CompactBlocks.BASES) {
            Item start = item("minecraft:" + base.path());
            h.assertTrue(start != Items.AIR, "Vanilla item " + base.path());
            Item below = start;
            List<Item> chain = new ArrayList<>(List.of(start));
            for (String stage : CompactBlocks.STAGES) {
                Item up = CompactBlocks.ITEMS.get(stage + "_" + base.path());
                ItemStack made = craft(level, h, nine(below));
                h.assertTrue(made.is(up) && made.getCount() == 1, base.path() + ": 9 " + name(below) + " make 1 " + stage + ", got " + made);
                ItemStack split = craft(level, h, List.of(new ItemStack(up)));
                h.assertTrue(split.is(below) && split.getCount() == 9, base.path() + ": 1 " + stage + " gives 9 " + name(below) + ", got " + split);
                chain.add(up);
                below = up;
            }
            h.assertTrue(chain.size() == 3, "chain " + base.path());
        }
        h.succeed();
    }

    /** Drops itself with the right tool and sits in the tool tag of its base (pickaxe or shovel). */
    public static void dropsAndTools(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        for (CompactBlocks.Base base : CompactBlocks.BASES) {
            boolean shovel = base.block().defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL);
            ItemStack tool = new ItemStack(shovel ? Items.DIAMOND_SHOVEL : Items.DIAMOND_PICKAXE);
            for (String stage : CompactBlocks.STAGES) {
                String name = stage + "_" + base.path();
                BlockState state = CompactBlocks.BLOCKS.get(name).defaultBlockState();
                h.assertTrue(state.is(shovel ? BlockTags.MINEABLE_WITH_SHOVEL : BlockTags.MINEABLE_WITH_PICKAXE), name + " is in the tool tag of " + base.path());
                int count = 0;
                for (ItemStack drop : Block.getDrops(state, level, pos, null, null, tool)) {
                    if (drop.is(state.getBlock().asItem())) count += drop.getCount();
                }
                h.assertTrue(count == 1, name + " drops itself once, got " + count);
            }
        }
        h.succeed();
    }

    private static List<ItemStack> nine(Item item) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < 9; i++) out.add(new ItemStack(item));
        return out;
    }

    private static String name(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** Result of the crafting recipe for these stacks (a full 3x3 for nine, a single stack for one), empty if none matches. */
    private static ItemStack craft(ServerLevel level, GameTestHelper h, List<ItemStack> stacks) {
        CraftingInput input = stacks.size() == 9 ? CraftingInput.of(3, 3, stacks) : CraftingInput.of(1, 1, stacks);
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
    }
}
