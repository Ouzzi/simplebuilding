package com.simplebuilding.modules.simpleriding;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class RidingGameTest {
 @GameTest(maxTicks=100) public void bookModels(GameTestHelper h){com.simpleriding.test.RidingBookTests.models(h);}
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h){com.simpleriding.guide.RidingGuide.gameTest(h);}
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
 @GameTest(maxTicks=100) public void vanillaTabPlacement(GameTestHelper h){com.simpleriding.test.RidingTests.vanillaTabPlacement(h);}
 @GameTest(maxTicks=100) public void configAndLang(GameTestHelper h){com.simpleriding.test.RidingTests.configAndLang(h);}
 @GameTest(maxTicks=100) public void crossModStorageAndArmor(GameTestHelper h){com.simpleriding.test.RidingTests.crossModStorageAndArmor(h);} @GameTest(maxTicks=100) public void nautilusSpeedAndArmor(GameTestHelper h){com.simpleriding.test.RidingTests.nautilusSpeedAndArmor(h);}
 @GameTest(maxTicks=100) public void nautilusDash(GameTestHelper h){com.simpleriding.test.RidingTests.nautilusDash(h);}
 @GameTest(maxTicks=100) public void attributeCaps(GameTestHelper h){com.simpleriding.test.RidingTests.attributeCaps(h);}
 @GameTest(maxTicks=100) public void featureSwitches(GameTestHelper h){com.simpleriding.test.RidingTests.featureSwitches(h);}
 @GameTest(maxTicks=100) public void allConfigBounds(GameTestHelper h){com.simpleriding.test.RidingTests.allConfigBounds(h);}
 @GameTest(maxTicks=100) public void movementPackets(GameTestHelper h){com.simpleriding.test.RidingTests.movementPackets(h);}
 @GameTest(maxTicks=100) public void jumpPackets(GameTestHelper h){com.simpleriding.test.RidingTests.jumpPackets(h);}
 @GameTest(maxTicks=100) public void movementBudget(GameTestHelper h){com.simpleriding.test.RidingTests.movementBudget(h);}
 @GameTest(maxTicks=100) public void weightedDataBounds(GameTestHelper h){com.simpleriding.test.RidingTests.weightedDataBounds(h);}
 @GameTest(maxTicks=100) public void steeringAndBorder(GameTestHelper h){com.simpleriding.test.RidingTests.steeringAndBorder(h);}
 @GameTest(maxTicks=100) public void allMountSpeedCaps(GameTestHelper h){com.simpleriding.test.RidingTests.allMountSpeedCaps(h);}
 @GameTest(maxTicks=100) public void camelDashBounds(GameTestHelper h){com.simpleriding.test.RidingTests.camelDashBounds(h);}
 @GameTest(maxTicks=100) public void groundFlags(GameTestHelper h){com.simpleriding.test.RidingTests.groundFlags(h);}
 @GameTest(maxTicks=100) public void horseshoeItems(GameTestHelper h){com.simpleriding.test.HorseshoeTests.items(h);}
 @GameTest(maxTicks=100) public void horseshoeRecipes(GameTestHelper h){com.simpleriding.test.HorseshoeTests.recipes(h);}
 @GameTest(maxTicks=100) public void horseshoePoints(GameTestHelper h){com.simpleriding.test.HorseshoeTests.points(h);}
 @GameTest(maxTicks=100) public void horseshoeEffects(GameTestHelper h){com.simpleriding.test.HorseshoeTests.effects(h);}
 @GameTest(maxTicks=100) public void horseshoeHandling(GameTestHelper h){com.simpleriding.test.HorseshoeTests.handling(h);}
 @GameTest(maxTicks=100) public void horseshoeMenu(GameTestHelper h){com.simpleriding.test.HorseshoeTests.menu(h);}
 @GameTest(maxTicks=100) public void horseshoeSaveAndDrop(GameTestHelper h){com.simpleriding.test.HorseshoeTests.saveAndDrop(h);}
 @GameTest(maxTicks=100) public void horseshoeWear(GameTestHelper h){com.simpleriding.test.HorseshoeTests.wear(h);}
 @GameTest(maxTicks=100) public void horseshoeMending(GameTestHelper h){com.simpleriding.test.HorseshoeTests.mending(h);}
 @GameTest(maxTicks=100) public void horseshoeFall(GameTestHelper h){com.simpleriding.test.HorseshoeTests.fall(h);}
 @GameTest(maxTicks=100) public void horseshoeLoot(GameTestHelper h){com.simpleriding.test.HorseshoeTests.loot(h);}
}
