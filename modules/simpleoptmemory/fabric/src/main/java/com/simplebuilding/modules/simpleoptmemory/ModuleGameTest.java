package com.simplebuilding.modules.simpleoptmemory;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric adapter, bodies in {@link MemoryOptTests}. */
public final class ModuleGameTest {
    @GameTest public void bootsWithoutAreas(GameTestHelper h) { MemoryOptTests.bootsWithoutAreas(h); }
}
