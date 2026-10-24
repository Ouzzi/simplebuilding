package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Rotation;

/** Fabric adapter for the review 24 core fixes; no test logic, see {@link RevCore24Tests}. */
public final class RevCore24GameTest {

    @GameTest(rotation = Rotation.NONE)
    public void claimsInsideOneChunkAreAskedAtTheRealFootprint(GameTestHelper helper) {
        RevCore24Tests.claimsInsideOneChunkAreAskedAtTheRealFootprint(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void heldAttractorMarksMovingItemsForSyncAndLeavesRestingOnesAlone(GameTestHelper helper) {
        RevCore24Tests.heldAttractorMarksMovingItemsForSyncAndLeavesRestingOnesAlone(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void anvilHammerCombineSkipsIncompatibleEnchantments(GameTestHelper helper) {
        RevCore24Tests.anvilHammerCombineSkipsIncompatibleEnchantments(helper);
    }

    @GameTest(rotation = Rotation.NONE)
    public void dataTablesAreTrimmedToTheSyncPacketLimit(GameTestHelper helper) {
        RevCore24Tests.dataTablesAreTrimmedToTheSyncPacketLimit(helper);
    }
}
