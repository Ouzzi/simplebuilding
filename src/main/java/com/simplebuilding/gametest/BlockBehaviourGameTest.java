package com.simplebuilding.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric adapter for the block behaviour tests.
 *
 * <p>This class holds no test logic. Every method delegates to the loader-neutral body in
 * {@link BlockBehaviourTests}; the annotation only restates the runner parameters, and the tick
 * budgets are shared constants so they cannot drift from the shared catalogue in
 * {@link SimpleBuildingGameTests}.
 *
 * <p>Registered through the {@code fabric-gametest} entrypoint in {@code fabric.mod.json}.
 * Class and method names are load bearing: Fabric derives the test id from them.
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
    public void levitatingSandAndGravelRiseUpwardsInsteadOfStayingPut(GameTestHelper helper) {
        BlockBehaviourTests.levitatingSandAndGravelRiseUpwardsInsteadOfStayingPut(helper);
    }
}
