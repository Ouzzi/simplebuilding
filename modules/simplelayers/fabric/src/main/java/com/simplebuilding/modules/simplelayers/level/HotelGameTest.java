package com.simplebuilding.modules.simplelayers.level;

import com.simplebuilding.modules.simplelayers.test.LayersTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric adapter of level "hotel" (BACK22, owned by its level branch): one method per {@code hotel_*} test of {@code Band.tests}. */
public final class HotelGameTest {
    @GameTest(structure = "simplelayers:empty")
    public void hotelRegistered(GameTestHelper h) {
        LayersTests.ALL.get("hotel_registered").accept(h);
    }
}
