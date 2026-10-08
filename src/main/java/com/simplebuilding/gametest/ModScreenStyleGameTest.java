package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the boxes of the mod screens in the container style.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link ModScreenStyleTests}; the catalogue in {@link SimpleBuildingGameTests} names the same tests for
 * NeoForge.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
 */
public final class ModScreenStyleGameTest {

    @GameTest
    public void tieredChestSlotsSitInsideTheirBoxes(GameTestHelper helper) {
        ModScreenStyleTests.tieredChestSlotsSitInsideTheirBoxes(helper);
    }

    @GameTest
    public void machineSlotsSitInsideTheirBoxes(GameTestHelper helper) {
        ModScreenStyleTests.machineSlotsSitInsideTheirBoxes(helper);
    }

    @GameTest
    public void backpackSlotsSitInsideTheOneBox(GameTestHelper helper) {
        ModScreenStyleTests.backpackSlotsSitInsideTheOneBox(helper);
    }
}
