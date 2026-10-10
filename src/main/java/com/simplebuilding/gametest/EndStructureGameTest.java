package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class EndStructureGameTest {
    @GameTest public void structuresAreRegistered(GameTestHelper helper) { EndStructureTests.structuresAreRegistered(helper); }
    @GameTest public void templatesLoad(GameTestHelper helper) { EndStructureTests.templatesLoad(helper); }
    @GameTest public void wellPlacesFromEndstoneBricks(GameTestHelper helper) { EndStructureTests.wellPlacesFromEndstoneBricks(helper); }
    @GameTest public void wreckHasLootChestsAndFrame(GameTestHelper helper) { EndStructureTests.wreckHasLootChestsAndFrame(helper); }
    @GameTest public void gatewayAndPathPlace(GameTestHelper helper) { EndStructureTests.gatewayAndPathPlace(helper); }
}
