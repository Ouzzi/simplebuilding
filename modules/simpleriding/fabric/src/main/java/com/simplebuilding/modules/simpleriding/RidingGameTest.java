package com.simplebuilding.modules.simpleriding;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class RidingGameTest {
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){com.simpleriding.test.RidingTests.launch(h);}
 @GameTest(maxTicks=100) public void armorAndAnvil(GameTestHelper h){com.simpleriding.test.RidingTests.armorAndAnvil(h);}
 @GameTest(maxTicks=100) public void horseSpeedAndCleanup(GameTestHelper h){com.simpleriding.test.RidingTests.horseSpeedAndCleanup(h);}
 @GameTest(maxTicks=100) public void pigSpeed(GameTestHelper h){com.simpleriding.test.RidingTests.pigSpeed(h);}
 @GameTest(maxTicks=100) public void striderSpeed(GameTestHelper h){com.simpleriding.test.RidingTests.striderSpeed(h);}
 @GameTest(maxTicks=100) public void camelSpeed(GameTestHelper h){com.simpleriding.test.RidingTests.camelSpeed(h);}
 @GameTest(maxTicks=100) public void ghastHarness(GameTestHelper h){com.simpleriding.test.RidingTests.ghastHarness(h);}
 @GameTest(maxTicks=100) public void leapingAndCleanup(GameTestHelper h){com.simpleriding.test.RidingTests.leapingAndCleanup(h);}
 @GameTest(maxTicks=100) public void armorDefense(GameTestHelper h){com.simpleriding.test.RidingTests.armorDefense(h);}
 @GameTest(maxTicks=100) public void trades(GameTestHelper h){com.simpleriding.test.RidingTests.trades(h);}
 @GameTest(maxTicks=100) public void lootAndToggle(GameTestHelper h){com.simpleriding.test.RidingTests.lootAndToggle(h);}
 @GameTest(maxTicks=100) public void configAndLang(GameTestHelper h){com.simpleriding.test.RidingTests.configAndLang(h);}
 @GameTest(maxTicks=100) public void crossModStorageAndArmor(GameTestHelper h){com.simpleriding.test.RidingTests.crossModStorageAndArmor(h);}
}
