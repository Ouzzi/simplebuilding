package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CrackedIceBlock;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Naturvarianten (QUEUE N24/N25, docs/ai/PLAN-Q-BLOCKS-2026-10-09.md): Rezepte, Beute der Gras-Stufe, fallende Sand-/
 * Kies-Stufen und rissiges Eis, das unter einem Lebewesen nach einigen Sekunden zu Wasser wird. Ohne
 * {@link McVersion#NATURE_VARIANTS} (26.2) bestehen die Tests ohne Pruefung.
 */
public final class NatureBlockTests {
    /** Rissiges Eis: vier Stufen a {@link CrackedIceBlock#TICKS_PER_CRACK} plus Fallen und Reserve. */
    public static final int CRACKED_ICE_MAX_TICKS = 200;
    public static final int FALLING_SLAB_MAX_TICKS = 100;

    private NatureBlockTests() {
    }

    /**
     * Werkbank: 3 Erde/Grasblock/Sand/Kies nebeneinander -> 6 Stufen; 2x2 Nautilusschalen -> Block, Block -> 4 Schalen;
     * Froschlicht (Vanilla oder Mod) + Rot/Cyan/Blau -> Scharlach/Aqua/Azur. Steinmetz: Eis -> rissiges Eis,
     * Packeis/Blaueis -> gemeisselt. Die Froschlichter leuchten wie Vanillas mit 15.
     */
    public static void natureVariantsHaveTheirRecipes(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        List<String> missing = new ArrayList<>();
        Object[][] slabs = {{Items.DIRT, ModItems.DIRT_SLAB}, {Items.GRASS_BLOCK, ModItems.GRASS_SLAB},
                {Items.SAND, ModItems.SAND_SLAB}, {Items.GRAVEL, ModItems.GRAVEL_SLAB}};
        for (Object[] pair : slabs) {
            ItemStack base = new ItemStack((Item) pair[0]);
            craft(helper, CraftingInput.of(3, 1, List.of(base, base, base)), "6 " + path((Item) pair[1]), missing);
        }
        ItemStack shell = new ItemStack(Items.NAUTILUS_SHELL);
        craft(helper, CraftingInput.of(2, 2, List.of(shell, shell, shell, shell)), "1 " + path(ModItems.NAUTILUS_SHELL_BLOCK), missing);
        craft(helper, CraftingInput.of(1, 1, List.of(new ItemStack(ModItems.NAUTILUS_SHELL_BLOCK))), "4 nautilus_shell", missing);
        Object[][] dyes = {{Items.RED_DYE, ModItems.SCARLET_FROGLIGHT}, {Items.CYAN_DYE, ModItems.AQUA_FROGLIGHT},
                {Items.BLUE_DYE, ModItems.AZURE_FROGLIGHT}};
        for (Object[] pair : dyes) {
            for (Item froglight : List.of(Items.OCHRE_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, ModItems.AQUA_FROGLIGHT)) {
                craft(helper, CraftingInput.of(2, 1, List.of(new ItemStack(froglight), new ItemStack((Item) pair[0]))),
                        "1 " + path((Item) pair[1]), missing);
            }
        }
        Set<String> cuts = stonecutting(helper.getLevel());
        for (String cut : List.of("ice -> 1 cracked_ice", "packed_ice -> 1 chiseled_packed_ice", "blue_ice -> 1 chiseled_blue_ice")) {
            if (!cuts.contains(cut)) {
                missing.add("stonecutter " + cut);
            }
        }
        for (Block froglight : List.of(ModBlocks.SCARLET_FROGLIGHT, ModBlocks.AQUA_FROGLIGHT, ModBlocks.AZURE_FROGLIGHT)) {
            helper.assertValueEqual(froglight.defaultBlockState().getLightEmission(), 15, "light of " + froglight);
        }
        helper.assertTrue(missing.isEmpty(), "nature variant recipes: " + missing);
        helper.succeed();
    }

    /**
     * Gras-Stufe wie der Grasblock: ohne Behutsamkeit die Erd-Stufe, mit Behutsamkeit sie selbst; als Doppelstufe je zwei.
     */
    public static void grassSlabDropsDirtSlabWithoutSilkTouch(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemStack shovel = new ItemStack(Items.IRON_SHOVEL);
        ItemStack silk = new ItemStack(Items.IRON_SHOVEL);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        BlockState single = ModBlocks.GRASS_SLAB.defaultBlockState();
        BlockState doubled = single.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        helper.assertValueEqual(drops(level, pos, single, shovel), "1 dirt_slab", "grass slab mined with a shovel");
        helper.assertValueEqual(drops(level, pos, doubled, shovel), "2 dirt_slab", "double grass slab mined with a shovel");
        helper.assertValueEqual(drops(level, pos, single, silk), "1 grass_slab", "grass slab mined with silk touch");
        helper.assertValueEqual(drops(level, pos, doubled, silk), "2 grass_slab", "double grass slab mined with silk touch");
        helper.assertValueEqual(drops(level, pos, ModBlocks.CRACKED_ICE.defaultBlockState(), new ItemStack(Items.IRON_PICKAXE)), "",
                "cracked ice mined without silk touch");
        helper.succeed();
    }

    /**
     * Sand- und Kies-Stufe fallen wie Sand: eine untere Stufe landet als untere, eine obere faellt als untere; eine untere
     * Stufe, die auf eine untere derselben Art faellt, wird mit ihr zur Doppelstufe (kein Item). Die Erd-Stufe faellt nicht.
     */
    public static void sandAndGravelSlabsFallAndMerge(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        for (int x = 0; x <= 6; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(1, 5, 1), ModBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(3, 5, 1), ModBlocks.GRAVEL_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        helper.setBlock(new BlockPos(5, 2, 1), ModBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(5, 6, 1), ModBlocks.SAND_SLAB.defaultBlockState());
        helper.setBlock(new BlockPos(6, 5, 1), ModBlocks.DIRT_SLAB.defaultBlockState());
        helper.succeedWhen(() -> {
            expectSlab(helper, new BlockPos(1, 2, 1), ModBlocks.SAND_SLAB, SlabType.BOTTOM);
            expectSlab(helper, new BlockPos(3, 2, 1), ModBlocks.GRAVEL_SLAB, SlabType.BOTTOM);
            expectSlab(helper, new BlockPos(5, 2, 1), ModBlocks.SAND_SLAB, SlabType.DOUBLE);
            expectSlab(helper, new BlockPos(6, 5, 1), ModBlocks.DIRT_SLAB, SlabType.BOTTOM);
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 5, 1)).isAir() && helper.getBlockState(new BlockPos(5, 6, 1)).isAir(),
                    "a falling slab left its old place filled");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds()).isEmpty(),
                    "a landing slab broke into an item");
        });
    }

    /**
     * Rissiges Eis wird unter einem Lebewesen (Ruestungsstaender) erst nach einigen Sekunden zu Wasser, reisst dabei sichtbar
     * (AGE steigt); ohne jemanden darauf bleibt es unversehrt.
     */
    public static void crackedIceMeltsUnderAnEntityAfterSomeSeconds(GameTestHelper helper) {
        if (!McVersion.NATURE_VARIANTS) {
            helper.succeed();
            return;
        }
        BlockPos stoodOn = new BlockPos(1, 1, 1);
        BlockPos alone = new BlockPos(4, 1, 1);
        helper.setBlock(stoodOn, ModBlocks.CRACKED_ICE);
        helper.setBlock(alone, ModBlocks.CRACKED_ICE);
        ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 2, 1));
        helper.assertTrue(stand != null, "no armor stand");
        int[] seenAge = {0};
        helper.runAfterDelay(CrackedIceBlock.TICKS_PER_CRACK / 2, () -> helper.assertTrue(
                helper.getBlockState(stoodOn).is(ModBlocks.CRACKED_ICE), "cracked ice melted at once"));
        helper.onEachTick(() -> {
            BlockState state = helper.getBlockState(stoodOn);
            if (state.is(ModBlocks.CRACKED_ICE)) {
                seenAge[0] = Math.max(seenAge[0], state.getValue(CrackedIceBlock.AGE));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(stoodOn).is(Blocks.WATER), "cracked ice under the stand is "
                    + helper.getBlockState(stoodOn));
            helper.assertValueEqual(seenAge[0], CrackedIceBlock.MAX_AGE, "highest crack seen before melting");
            helper.assertTrue(helper.getBlockState(alone).is(ModBlocks.CRACKED_ICE)
                    && helper.getBlockState(alone).getValue(CrackedIceBlock.AGE) == 0, "cracked ice nobody stood on changed: "
                    + helper.getBlockState(alone));
        });
    }

    private static void expectSlab(GameTestHelper helper, BlockPos pos, Block slab, SlabType type) {
        BlockState state = helper.getBlockState(pos);
        helper.assertTrue(state.is(slab) && state.getValue(SlabBlock.TYPE) == type,
                "expected " + type + " " + slab + " at " + pos + ", found " + state);
    }

    private static String drops(ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, null, null, tool);
        StringBuilder out = new StringBuilder();
        for (ItemStack stack : drops) {
            if (!out.isEmpty()) {
                out.append(", ");
            }
            out.append(stack.getCount()).append(' ').append(path(stack.getItem()));
        }
        return out.toString();
    }

    private static Set<String> stonecutting(ServerLevel level) {
        ContextMap context = SlotDisplayContext.fromLevel(level);
        Set<String> cuts = new TreeSet<>();
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (!(holder.value() instanceof StonecutterRecipe)) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                if (display instanceof StonecutterRecipeDisplay cut) {
                    for (ItemStack in : cut.input().resolveForStacks(context)) {
                        for (ItemStack out : cut.result().resolveForStacks(context)) {
                            cuts.add(path(in.getItem()) + " -> " + out.getCount() + " " + path(out.getItem()));
                        }
                    }
                }
            }
        }
        return cuts;
    }

    private static void craft(GameTestHelper helper, CraftingInput grid, String expected, List<String> missing) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        String got = match.map(m -> {
            ItemStack result = m.value().assemble(grid);
            return result.getCount() + " " + path(result.getItem());
        }).orElse("nothing");
        if (!got.equals(expected)) {
            missing.add("crafting " + expected + " (got " + got + ")");
        }
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
