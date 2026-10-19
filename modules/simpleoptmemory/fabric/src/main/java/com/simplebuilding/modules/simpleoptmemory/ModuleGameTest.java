package com.simplebuilding.modules.simpleoptmemory;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric adapter, bodies in {@link MemoryOptTests}. */
public final class ModuleGameTest {
    @GameTest public void compatModes(GameTestHelper h) { MemoryOptTests.compatModes(h); }
    @GameTest public void sharingRule(GameTestHelper h) { MemoryOptTests.sharingRule(h); }
    @GameTest public void sharedStates(GameTestHelper h) { MemoryOptTests.sharedStates(h); }
}
