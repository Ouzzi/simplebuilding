package com.simplebuilding.modules.simpleoptserver;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric adapter, bodies in {@link ServerOptTests}. */
public final class ModuleGameTest {
    @GameTest public void compatModes(GameTestHelper h) { ServerOptTests.compatModes(h); }
    @GameTest public void s5aBounds(GameTestHelper h) { ServerOptTests.s5aBounds(h); }
    @GameTest public void s5aController(GameTestHelper h) { ServerOptTests.s5aController(h); }
    @GameTest public void configFile(GameTestHelper h) { ServerOptTests.configFile(h); }
}
