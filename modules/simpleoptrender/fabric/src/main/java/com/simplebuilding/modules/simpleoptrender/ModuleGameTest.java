package com.simplebuilding.modules.simpleoptrender;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric adapter, bodies in {@link RenderOptTests}. */
public final class ModuleGameTest {
    @GameTest public void compatModes(GameTestHelper h) { RenderOptTests.compatModes(h); }
    @GameTest public void mixinPlugin(GameTestHelper h) { RenderOptTests.mixinPlugin(h); }
    @GameTest(structure = "simpleoptrender:empty") public void leafCullingRule(GameTestHelper h) { RenderOptTests.leafCullingRule(h); }
    @GameTest public void lightmapCache(GameTestHelper h) { RenderOptTests.lightmapCache(h); }
    @GameTest public void fastIpPing(GameTestHelper h) { RenderOptTests.fastIpPing(h); }
    @GameTest public void mixinGuardMissing(GameTestHelper h) { com.simplebuilding.modules.simpleoptrender.fabric.MixinGuardTests.missingHandlers(h); }
    @GameTest public void mixinGuardSwitchesOff(GameTestHelper h) { com.simplebuilding.modules.simpleoptrender.fabric.MixinGuardTests.failureSwitchesAreaOff(h); }
    @GameTest public void configFile(GameTestHelper h) { RenderOptTests.configFile(h); }
    @GameTest public void moreCullingRules(GameTestHelper h) { RenderOptTests.moreCullingRules(h); }
    @GameTest public void detailsRules(GameTestHelper h) { RenderOptTests.detailsRules(h); }
    @GameTest public void targetCheck(GameTestHelper h) { RenderOptTests.targetCheck(h); }
}
