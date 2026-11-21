package com.simplebuilding.modules.simpleoptimization;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleoptimization", false); }
    @GameTest public void subModSwitch(GameTestHelper helper) { OptimizationTests.subModSwitch(helper); }
    @GameTest public void configLabels(GameTestHelper helper) { OptimizationTests.configLabels(helper); }
    @GameTest public void configFile(GameTestHelper helper) { OptimizationTests.configFile(helper); }
    @GameTest(maxTicks = OptimizationTests.BENCH_MAX_TICKS) public void benchHeadless(GameTestHelper helper) { OptimizationTests.benchHeadless(helper); }
    /** N137: every tooltip that names a number or on/off as its default names the real default (EN + DE). */
    @GameTest public void configTooltipDefaults(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.tooltipDefaults(h, "simpleoptimization"); }
}
