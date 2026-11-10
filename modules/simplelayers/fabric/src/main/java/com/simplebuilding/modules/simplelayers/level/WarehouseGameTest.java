package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "warehouse" (BACK22, owned by its level branch): one method per {@code warehouse_*} test of {@code Band.tests}. */
public final class WarehouseGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void warehouseRegistered(GameTestHelper h) {
        LayersTests.ALL.get("warehouse_registered").accept(h);
    }
}
