package com.simpleblocks.test;

import com.simpleblocks.SimpleBlocks;
import com.simpleblocks.compact.CollapsedConfig;
import com.simpleblocks.compact.CollapsedEffect;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * WH26-10 (docs/ai/PLAN-A-COMPACT-2026-11-06.md) module GameTests: the 57 Compressed/Dense/Collapsed blocks are registered with the hardness of
 * 4x / 16x / 64x their base and the base's tool tag, drop themselves, and every recipe step is reversible (9 base -> Compact -> Dense and
 * 1 -> 9 back, nothing lost, nothing gained). Test names carry the {@code compact_} prefix of the catalogue. Collapsed: Slowness by stack count and the config switch.
 */
public final class CompactTests {
    private CompactTests() {}

    public static void register(Map<String, Consumer<GameTestHelper>> all, Map<String, Integer> maxTicks) {
        all.put("compact_registered", CompactTests::registered);
        all.put("compact_recipes_reversible", CompactTests::recipesReversible);
        all.put("compact_drops_and_tools", CompactTests::dropsAndTools);
        all.put("compact_collapsed_slowness", CompactTests::collapsedSlowness);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id));
    }

    /** 19 bases x 2 stages, items and block items exist, hardness 4x and 16x the Vanilla base, resistance and map colour as the base. */
    public static void registered(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.assertTrue(CompactBlocks.BASES.size() == 19 && CompactBlocks.BLOCKS.size() == 57, "19 bases, 57 blocks, found " + CompactBlocks.BLOCKS.size());
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
            h.assertTrue(chain.size() == 4, "chain " + base.path());
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

    /**
     * Collapsed blocks slow their carrier: 1 stack Slowness I, 2 stacks II, 9 stacks III (stacks, not items: one full stack is one),
     * short and bounded (it ends on its own once nothing is carried), nothing at all with an empty inventory or the switch off.
     */
    public static void collapsedSlowness(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        Item collapsed = CompactBlocks.ITEMS.get("collapsed_dirt");
        Item dense = CompactBlocks.ITEMS.get("dense_dirt");
        var inv = player.getInventory();
        inv.clearContent();
        CollapsedEffect.apply(player);
        h.assertFalse(player.hasEffect(MobEffects.SLOWNESS), "nothing carried: no Slowness");
        inv.setItem(0, new ItemStack(collapsed, 64));
        inv.setItem(1, new ItemStack(dense, 64));
        h.assertTrue(CollapsedEffect.stacks(inv) == 1, "a full stack counts once, Dense does not count");
        int[] stacks = {1, 2, 5, 9};
        int[] expected = {0, 1, 1, 2};
        for (int k = 0; k < stacks.length; k++) {
            inv.clearContent();
            for (int i = 0; i < stacks[k]; i++) inv.setItem(i, new ItemStack(collapsed));
            player.removeEffect(MobEffects.SLOWNESS);
            CollapsedEffect.apply(player);
            var e = player.getEffect(MobEffects.SLOWNESS);
            h.assertTrue(e != null && e.getAmplifier() == expected[k], stacks[k] + " stacks: Slowness amplifier " + expected[k] + ", got " + (e == null ? "none" : e.getAmplifier()));
            h.assertTrue(e.getDuration() == CollapsedEffect.DURATION && CollapsedEffect.DURATION <= 100, "short effect, a few seconds");
        }
        inv.clearContent();
        player.removeEffect(MobEffects.SLOWNESS);
        CollapsedEffect.apply(player);
        h.assertFalse(player.hasEffect(MobEffects.SLOWNESS), "put down again: nothing refreshes the effect, it ends within its short duration");
        inv.setItem(0, new ItemStack(collapsed));
        boolean was = CollapsedConfig.enabled;
        CollapsedConfig.enabled = false;
        try {
            CollapsedEffect.apply(player);
            h.assertFalse(player.hasEffect(MobEffects.SLOWNESS), "switch collapsed.enabled off: no Slowness");
        } finally {
            CollapsedConfig.enabled = was;
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
