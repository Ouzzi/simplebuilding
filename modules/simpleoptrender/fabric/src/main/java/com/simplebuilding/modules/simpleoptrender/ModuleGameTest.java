package com.simplebuilding.modules.simpleoptrender;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric adapter, bodies in {@link RenderOptTests}. */
public final class ModuleGameTest {
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simpleoptrender", true); }
    @GameTest public void compatModes(GameTestHelper h) { RenderOptTests.compatModes(h); }
    @GameTest public void mixinPlugin(GameTestHelper h) { RenderOptTests.mixinPlugin(h); }
    @GameTest(structure = "simpleoptrender:empty") public void leafCullingRule(GameTestHelper h) { RenderOptTests.leafCullingRule(h); }
    @GameTest public void lightmapCache(GameTestHelper h) { RenderOptTests.lightmapCache(h); }
    @GameTest public void fastIpPing(GameTestHelper h) { RenderOptTests.fastIpPing(h); }
    @GameTest public void mixinGuardMissing(GameTestHelper h) { com.simplebuilding.modules.simpleoptrender.fabric.MixinGuardTests.missingHandlers(h); }
    @GameTest public void mixinGuardSwitchesOff(GameTestHelper h) { com.simplebuilding.modules.simpleoptrender.fabric.MixinGuardTests.failureSwitchesAreaOff(h); }
    @GameTest public void telemetryReplacesSender(GameTestHelper h) throws java.io.IOException { com.simplebuilding.modules.simpleoptrender.fabric.MixinGuardTests.telemetryReplacesSender(h); }
    @GameTest public void configFile(GameTestHelper h) { RenderOptTests.configFile(h); }
    @GameTest public void moreCullingRules(GameTestHelper h) { RenderOptTests.moreCullingRules(h); }
    @GameTest public void detailsRules(GameTestHelper h) { RenderOptTests.detailsRules(h); }
    @GameTest public void targetCheck(GameTestHelper h) { RenderOptTests.targetCheck(h); }
    /** N137: the generic config screen names title, area group and every option in English and German (ConfigCommandTests.labels). */
    @GameTest public void configLabels(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.labels(h, "simpleoptrender"); }
    /** N137: every tooltip that names a number or on/off as its default names the real default (EN + DE). */
    @GameTest public void configTooltipDefaults(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.tooltipDefaults(h, "simpleoptrender"); }
}
