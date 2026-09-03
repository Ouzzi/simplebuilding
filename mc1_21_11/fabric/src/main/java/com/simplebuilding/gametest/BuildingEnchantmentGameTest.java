package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/**
 * Fabric adapter for the building tool enchantments.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link BuildingEnchantmentTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class BuildingEnchantmentGameTest {

    @GameTest(rotation = Rotation.NONE)
    public void constructorsTouchUnlocksTheExtraChiselTablesInBothDirections(GameTestHelper helper) {
        BuildingEnchantmentTests.constructorsTouchUnlocksTheExtraChiselTablesInBothDirections(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void fastChiselingShortensTheCooldownAndSpeedsUpMining(GameTestHelper helper) {
        BuildingEnchantmentTests.fastChiselingShortensTheCooldownAndSpeedsUpMining(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void colorPaletteSpreadsTheCarriedBlocksOverTheWandPreview(GameTestHelper helper) {
        BuildingEnchantmentTests.colorPaletteSpreadsTheCarriedBlocksOverTheWandPreview(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void colorPaletteKeepsTheWandBuildingWhenOneBlockRunsOut(GameTestHelper helper) {
        BuildingEnchantmentTests.colorPaletteKeepsTheWandBuildingWhenOneBlockRunsOut(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void linearOnlyShortensTheWandStepDelay(GameTestHelper helper) {
        BuildingEnchantmentTests.linearOnlyShortensTheWandStepDelay(helper);
    }
}
