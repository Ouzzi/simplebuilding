package com.simplebuilding.modules.simplequalityoflife;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h){com.simplequalityoflife.guide.QolGuide.gameTest(h);}
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){com.simplequalityoflife.test.QolTests.launch(h);}
 @GameTest(maxTicks=100) public void configBounds(GameTestHelper h){com.simplequalityoflife.test.QolTests.configBounds(h);}
 @GameTest(maxTicks=100) public void configLang(GameTestHelper h){com.simplequalityoflife.test.QolTests.configLang(h);}
 @GameTest(maxTicks=100) public void crawl(GameTestHelper h){com.simplequalityoflife.test.QolTests.crawl(h);}
 @GameTest(maxTicks=100) public void climbPackets(GameTestHelper h){com.simplequalityoflife.test.QolTests.climbPackets(h);}
 @GameTest(maxTicks=100) public void climbMechanics(GameTestHelper h){com.simplequalityoflife.test.QolTests.climbMechanics(h);}
 @GameTest(maxTicks=100) public void powderSnow(GameTestHelper h){com.simplequalityoflife.test.QolTests.powderSnow(h);}
 @GameTest(maxTicks=100) public void furnaceLava(GameTestHelper h){com.simplequalityoflife.test.QolTests.furnaceLava(h);}
 @GameTest(maxTicks=100) public void permissionsSpam(GameTestHelper h){com.simplequalityoflife.test.QolTests.permissionsSpam(h);}
 @GameTest(maxTicks=100) public void durability(GameTestHelper h){com.simplequalityoflife.test.QolTests.durability(h);}
 @GameTest(maxTicks=100) public void crossMod(GameTestHelper h){com.simplequalityoflife.test.QolTests.crossMod(h);}
 @GameTest(maxTicks=100) public void realMovementPackets(GameTestHelper h){com.simplequalityoflife.test.QolTests.realMovementPackets(h);}
 @GameTest(maxTicks=100) public void thrift(GameTestHelper h){com.simplequalityoflife.test.QolTests.thrift(h);}
 @GameTest(maxTicks=100) public void anvilRepairCost(GameTestHelper h){com.simplequalityoflife.test.QolTests.anvilRepairCost(h);}
 @GameTest(maxTicks=100) public void featureSwitches(GameTestHelper h){com.simplequalityoflife.test.QolTests.featureSwitches(h);}
 @GameTest(maxTicks=100) public void linkedMark(GameTestHelper h){com.simplequalityoflife.test.ContainerTests.linkedMark(h);}
 @GameTest(maxTicks=100) public void linkedRange(GameTestHelper h){com.simplequalityoflife.test.ContainerTests.linkedRange(h);}
 @GameTest(maxTicks=100) public void linkedTransfer(GameTestHelper h){com.simplequalityoflife.test.ContainerTests.linkedTransfer(h);}
 @GameTest(maxTicks=100) public void linkedInventory(GameTestHelper h){com.simplequalityoflife.test.ContainerTests.linkedInventory(h);}
 @GameTest(maxTicks=100) public void portableShulker(GameTestHelper h){com.simplequalityoflife.test.ContainerTests.portableShulker(h);}
 @GameTest(maxTicks=100) public void portableEnderChest(GameTestHelper h){com.simplequalityoflife.test.ContainerTests.portableEnderChest(h);}
 @GameTest public void subModSwitch(GameTestHelper h){com.simplequalityoflife.test.QolTests.subModSwitch(h);}
}
