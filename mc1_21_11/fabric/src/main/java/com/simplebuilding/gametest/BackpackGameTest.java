package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the backpacks.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link BackpackTests}; the catalogue in {@link SimpleBuildingGameTests} names the same tests for
 * NeoForge.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class BackpackGameTest {

    @GameTest
    public void rightClickWearsTheBackpackAndSwapsItWithTheChestplate(GameTestHelper helper) {
        BackpackTests.rightClickWearsTheBackpackAndSwapsItWithTheChestplate(helper);
    }

    @GameTest
    public void tiersCarryTheirArmorSlotCountAndSlotLayout(GameTestHelper helper) {
        BackpackTests.tiersCarryTheirArmorSlotCountAndSlotLayout(helper);
    }

    @GameTest
    public void contentsSurviveTheReinforcedRecipeAndBothSmithingUpgrades(GameTestHelper helper) {
        BackpackTests.contentsSurviveTheReinforcedRecipeAndBothSmithingUpgrades(helper);
    }

    @GameTest
    public void sneakRightClickPlacesTheBackpackAndBreakingItDropsEverything(GameTestHelper helper) {
        BackpackTests.sneakRightClickPlacesTheBackpackAndBreakingItDropsEverything(helper);
    }

    @GameTest
    public void openKeyOpensTheMenuOnlyForTheWornBackpack(GameTestHelper helper) {
        BackpackTests.openKeyOpensTheMenuOnlyForTheWornBackpack(helper);
    }

    @GameTest
    public void shiftClickFillsTheBackpackFirstAndEmptiesItIntoTheMainInventory(GameTestHelper helper) {
        BackpackTests.shiftClickFillsTheBackpackFirstAndEmptiesItIntoTheMainInventory(helper);
    }

    @GameTest
    public void deepPocketsRaisesStackLimitsOnlyForStackables(GameTestHelper helper) {
        BackpackTests.deepPocketsRaisesStackLimitsOnlyForStackables(helper);
    }

    @GameTest
    public void funnelPullsPickedUpItemsIntoTheWornBackpack(GameTestHelper helper) {
        BackpackTests.funnelPullsPickedUpItemsIntoTheWornBackpack(helper);
    }

    @GameTest
    public void masterBuilderOpensTheBackpackOnlyWhenTheBackpackCarriesIt(GameTestHelper helper) {
        BackpackTests.masterBuilderOpensTheBackpackOnlyWhenTheBackpackCarriesIt(helper);
    }

    @GameTest
    public void constructorsTouchRefillsTheEmptyHandFromTheWornBackpack(GameTestHelper helper) {
        BackpackTests.constructorsTouchRefillsTheEmptyHandFromTheWornBackpack(helper);
    }

    @GameTest
    public void backpacksTakeTheirFourEnchantmentsButNeitherDrawerNorColorPalette(GameTestHelper helper) {
        BackpackTests.backpacksTakeTheirFourEnchantmentsButNeitherDrawerNorColorPalette(helper);
    }

    @GameTest
    public void upperTiersSurviveFireAndExplosionsAndLowerOnesSpillTheirContents(GameTestHelper helper) {
        BackpackTests.upperTiersSurviveFireAndExplosionsAndLowerOnesSpillTheirContents(helper);
    }
}
