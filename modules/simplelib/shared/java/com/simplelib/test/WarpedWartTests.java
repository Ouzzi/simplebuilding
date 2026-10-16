package com.simplelib.test;

import com.simplelib.registry.LibBlocks;
import com.simplelib.registry.LibItems;
import com.simplelib.registry.LibPotions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** GameTests of the warped wart (owner N71): growth, harvest, compost, bastion loot, brewing and the block recipes. */
final class WarpedWartTests {
    static void register() {
        LibTests.ALL.put("warped_wart_grows_like_nether_wart", WarpedWartTests::grows);
        LibTests.ALL.put("warped_wart_harvest_drops", WarpedWartTests::harvest);
        LibTests.ALL.put("warped_wart_is_compostable_and_plantable", WarpedWartTests::compostableAndPlantable);
        LibTests.ALL.put("warped_wart_in_bastion_chests", WarpedWartTests::bastionLoot);
        LibTests.ALL.put("warped_wart_brews_the_warped_potion", WarpedWartTests::brewing);
        LibTests.ALL.put("warped_wart_block_recipes", WarpedWartTests::blockRecipes);
    }

    private static BlockState wart(int age) {
        return LibBlocks.WARPED_WART.defaultBlockState().setValue(NetherWartBlock.AGE, age);
    }

    /** Soul sand only, four ages, random ticks grow it to age 3 and no further, bone meal does nothing (like nether wart). */
    private static void grows(GameTestHelper h) {
        BlockPos rel = new BlockPos(1, 2, 1);
        BlockPos abs = h.absolutePos(rel);
        ServerLevel level = h.getLevel();
        h.setBlock(rel.below(), Blocks.SOUL_SAND);
        h.setBlock(rel, wart(0));
        h.assertTrue(wart(0).canSurvive(level, abs), "warped wart survives on soul sand");
        h.setBlock(rel.below(), Blocks.DIRT);
        h.assertTrue(wart(0).canSurvive(level, abs) == Blocks.NETHER_WART.defaultBlockState().canSurvive(level, abs),
                "on dirt the warped wart behaves like the nether wart (the loader may add its own plant rules)");
        h.setBlock(rel.below(), Blocks.SOUL_SAND);
        h.setBlock(rel, wart(0));
        h.assertTrue(NetherWartBlock.MAX_AGE == 3 && !(LibBlocks.WARPED_WART instanceof BonemealableBlock), "four ages, no bone meal use");
        h.assertTrue(level.getBlockState(abs).isRandomlyTicking(), "random ticking");
        for (int i = 0; i < 600 && level.getBlockState(abs).getValue(NetherWartBlock.AGE) < 3; i++) {
            level.getBlockState(abs).randomTick(level, abs, level.getRandom());
        }
        h.assertTrue(level.getBlockState(abs).getValue(NetherWartBlock.AGE) == 3, "grew to age 3: " + level.getBlockState(abs));
        for (int i = 0; i < 50; i++) level.getBlockState(abs).randomTick(level, abs, level.getRandom());
        h.assertTrue(level.getBlockState(abs).getValue(NetherWartBlock.AGE) == 3, "stays at age 3");
        h.assertTrue(LibBlocks.WARPED_WART.getCloneItemStack(level, abs, wart(3), false).is(LibItems.WARPED_WART), "pick block gives the item");
        h.succeed();
    }

    private static int dropped(ServerLevel level, BlockPos abs, BlockState state, ItemStack tool) {
        int count = 0;
        for (ItemStack stack : state.getDrops(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(abs))
                .withParameter(LootContextParams.TOOL, tool))) {
            if (stack.is(LibItems.WARPED_WART)) count += stack.getCount();
        }
        return count;
    }

    /** Young: exactly one; grown: 2-4 (all values over many harvests). */
    private static void harvest(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos abs = h.absolutePos(new BlockPos(1, 2, 1));
        int min = 99, max = 0;
        for (int i = 0; i < 300; i++) {
            h.assertTrue(dropped(level, abs, wart(0), ItemStack.EMPTY) == 1 && dropped(level, abs, wart(2), ItemStack.EMPTY) == 1, "young: one");
            int grown = dropped(level, abs, wart(3), ItemStack.EMPTY);
            min = Math.min(min, grown);
            max = Math.max(max, grown);
        }
        h.assertTrue(min == 2 && max == 4, "grown drops 2-4: " + min + ".." + max);
        h.succeed();
    }

    private static void compostableAndPlantable(GameTestHelper h) {
        ItemStack stack = new ItemStack(LibItems.WARPED_WART);
        h.assertTrue(stack.has(DataComponents.COMPOSTABLE), "compostable like the nether wart");
        h.assertTrue(new ItemStack(Items.NETHER_WART).has(DataComponents.COMPOSTABLE), "(nether wart is compostable too)");
        h.assertTrue(stack.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() == LibBlocks.WARPED_WART, "item plants the block");
        h.succeed();
    }

    /** The real tables of the running loader (Fabric event, NeoForge event, Forge mixin): treasure chests nearly always carry some. */
    private static void bastionLoot(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var tables = level.getServer().reloadableRegistries();
        int treasure = 0, other = 0, other2 = 0;
        for (int i = 0; i < 300; i++) {
            treasure += count(level, tables.getLootTable(BuiltInLootTables.BASTION_TREASURE));
            other += count(level, tables.getLootTable(BuiltInLootTables.BASTION_OTHER));
            other2 += count(level, tables.getLootTable(BuiltInLootTables.BASTION_BRIDGE)) + count(level, tables.getLootTable(BuiltInLootTables.BASTION_HOGLIN_STABLE));
        }
        h.assertTrue(treasure > 100 && other > 50 && other2 > 100, "bastion chests carry warped wart: " + treasure + "/" + other + "/" + other2);
        h.assertTrue(count(level, tables.getLootTable(BuiltInLootTables.SIMPLE_DUNGEON)) == 0, "other chests do not");
        h.succeed();
    }

    private static int count(ServerLevel level, net.minecraft.world.level.storage.loot.LootTable table) {
        int n = 0;
        for (ItemStack stack : table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST))) {
            if (stack.is(LibItems.WARPED_WART)) n += stack.getCount();
        }
        return n;
    }

    private static ItemStack brew(ServerLevel level, ItemStack input, ItemStack reagent) {
        BrewingInput brewing = new BrewingInput(input, reagent);
        return level.recipeAccess().getRecipeFor(RecipeType.BREWING, brewing, level).map(r -> r.value().assemble(brewing)).orElse(ItemStack.EMPTY);
    }

    /** Water + warped wart makes the warped potion in all three bottles; the nether wart still makes the awkward potion. */
    private static void brewing(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        h.assertTrue(LibPotions.WARPED != null && LibPotions.WARPED.value().getEffects().isEmpty(), "the warped potion has no effect");
        for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            ItemStack out = brew(level, PotionContents.createItemStack(container, Potions.WATER), new ItemStack(LibItems.WARPED_WART));
            PotionContents contents = out.get(DataComponents.POTION_CONTENTS);
            h.assertTrue(out.is(container) && contents != null && contents.is(LibPotions.WARPED), container + " + warped wart: " + out);
            ItemStack awkward = brew(level, PotionContents.createItemStack(container, Potions.WATER), new ItemStack(Items.NETHER_WART));
            PotionContents awkwardContents = awkward.get(DataComponents.POTION_CONTENTS);
            h.assertTrue(awkwardContents != null && awkwardContents.is(Potions.AWKWARD), "nether wart still brews the awkward potion");
        }
        h.succeed();
    }

    /** 9 warped wart <-> Vanilla's warped wart block. */
    private static void blockRecipes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var nine = CraftingInput.of(3, 3, java.util.Collections.nCopies(9, new ItemStack(LibItems.WARPED_WART)));
        ItemStack block = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, nine, level).map(r -> r.value().assemble(nine)).orElse(ItemStack.EMPTY);
        h.assertTrue(block.is(Items.WARPED_WART_BLOCK) && block.getCount() == 1, "9 warped wart -> warped wart block: " + block);
        var one = CraftingInput.of(1, 1, List.of(new ItemStack(Items.WARPED_WART_BLOCK)));
        ItemStack back = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, one, level).map(r -> r.value().assemble(one)).orElse(ItemStack.EMPTY);
        h.assertTrue(back.is(LibItems.WARPED_WART) && back.getCount() == 9, "warped wart block -> 9 warped wart: " + back);
        h.succeed();
    }

    private WarpedWartTests() {}
}
