package com.simplebuilding.modules.simpleoptimization;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
    @GameTest public void subModSwitch(GameTestHelper helper) { OptimizationTests.subModSwitch(helper); }
    @GameTest public void configFile(GameTestHelper helper) { OptimizationTests.configFile(helper); }
}
