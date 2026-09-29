package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the optional accessory slots (Curios/Trinkets seam).
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link AccessorySlotTests}; the catalogue in {@link SimpleBuildingGameTests} names the same tests for
 * NeoForge.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class AccessorySlotGameTest {

    @GameTest
    public void backpackLookupGoesChestThenAccessoryThenInventory(GameTestHelper helper) {
        AccessorySlotTests.backpackLookupGoesChestThenAccessoryThenInventory(helper);
    }

    @GameTest
    public void backpackKeyOpensAndWritesBackTheAccessoryBackpack(GameTestHelper helper) {
        AccessorySlotTests.backpackKeyOpensAndWritesBackTheAccessoryBackpack(helper);
    }

    @GameTest
    public void quiverLookupGoesChestThenAccessoryThenHotbar(GameTestHelper helper) {
        AccessorySlotTests.quiverLookupGoesChestThenAccessoryThenHotbar(helper);
    }

    @GameTest
    public void masterBuilderAndFunnelReadTheAccessoryBackpack(GameTestHelper helper) {
        AccessorySlotTests.masterBuilderAndFunnelReadTheAccessoryBackpack(helper);
    }

    @GameTest
    public void anIncompatibleAccessoryModIsDroppedInsteadOfCrashing(GameTestHelper helper) {
        AccessorySlotTests.anIncompatibleAccessoryModIsDroppedInsteadOfCrashing(helper);
    }
}
