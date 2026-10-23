package com.simplebuilding.gametest;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class Mech24GameTest {
    @GameTest public void lineEndRodsGlowAndFaceLikeVanilla(GameTestHelper helper) { Mech24Tests.lineEndRodsGlowAndFaceLikeVanilla(helper); }
    @GameTest public void hiddenOreBrushesFreeInSixStrokes(GameTestHelper helper) { Mech24Tests.hiddenOreBrushesFreeInSixStrokes(helper); }
    @GameTest(maxTicks = 100) public void hiddenOreDropsLessUnlessBrushed(GameTestHelper helper) { Mech24Tests.hiddenOreDropsLessUnlessBrushed(helper); }
}
