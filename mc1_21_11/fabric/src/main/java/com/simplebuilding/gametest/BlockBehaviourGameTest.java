package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the block behaviour tests (MC 1.21.11 line). No logic here; see
 * {@link BlockBehaviourTests}.
 */
public final class BlockBehaviourGameTest {

    @GameTest(maxTicks = BlockBehaviourTests.FURNACE_MAX_TICKS)
    public void reinforcedAndNetheriteFurnacesSmeltFasterThanVanilla(GameTestHelper helper) {
        BlockBehaviourTests.reinforcedAndNetheriteFurnacesSmeltFasterThanVanilla(helper);
    }

    @GameTest(maxTicks = BlockBehaviourTests.BLAST_AND_SMOKER_MAX_TICKS)
    public void reinforcedAndNetheriteBlastFurnacesAndSmokersOutpaceVanilla(GameTestHelper helper) {
        BlockBehaviourTests.reinforcedAndNetheriteBlastFurnacesAndSmokersOutpaceVanilla(helper);
    }

    @GameTest(maxTicks = BlockBehaviourTests.HOPPER_MAX_TICKS)
    public void reinforcedAndNetheriteHoppersMoveItemsFasterThanVanilla(GameTestHelper helper) {
        BlockBehaviourTests.reinforcedAndNetheriteHoppersMoveItemsFasterThanVanilla(helper);
    }

    @GameTest(skyAccess = true, maxTicks = BlockBehaviourTests.REINFORCED_PISTON_MAX_TICKS)
    public void reinforcedPistonPushesThirteenBlocksWhereVanillaPistonRefuses(GameTestHelper helper) {
        BlockBehaviourTests.reinforcedPistonPushesThirteenBlocksWhereVanillaPistonRefuses(helper);
    }

    @GameTest(maxTicks = BlockBehaviourTests.NETHERITE_PISTON_MAX_TICKS)
    public void netheritePistonBreaksTheBlockInFrontWhileVanillaPistonPushesIt(GameTestHelper helper) {
        BlockBehaviourTests.netheritePistonBreaksTheBlockInFrontWhileVanillaPistonPushesIt(helper);
    }

    @GameTest(maxTicks = BlockBehaviourTests.SUSPENDED_FALLING_BLOCK_MAX_TICKS)
    public void suspendedSandAndGravelStayInPlaceWhileVanillaOnesFall(GameTestHelper helper) {
        BlockBehaviourTests.suspendedSandAndGravelStayInPlaceWhileVanillaOnesFall(helper);
    }

    @GameTest(maxTicks = BlockBehaviourTests.LEVITATING_BLOCK_MAX_TICKS)
    public void levitatingSandAndGravelRiseAsAnAcceleratingEntity(GameTestHelper helper) {
        BlockBehaviourTests.levitatingSandAndGravelRiseAsAnAcceleratingEntity(helper);
    }

    @GameTest(maxTicks = BlockBehaviourTests.LEVITATING_BLOCK_MAX_TICKS)
    public void levitatingSandTurnsBackIntoABlockUnderACeiling(GameTestHelper helper) {
        BlockBehaviourTests.levitatingSandTurnsBackIntoABlockUnderACeiling(helper);
    }

    @GameTest(maxTicks = BlockBehaviourTests.LEVITATING_BLOCK_MAX_TICKS)
    public void levitatingSandDropsAsAnItemAtTheBuildLimit(GameTestHelper helper) {
        BlockBehaviourTests.levitatingSandDropsAsAnItemAtTheBuildLimit(helper);
    }
}
