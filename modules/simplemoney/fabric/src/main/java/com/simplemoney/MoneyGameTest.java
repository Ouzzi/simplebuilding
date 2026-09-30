package com.simplemoney;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class MoneyGameTest {
 @GameTest(maxTicks=100) public void launchSmoke(GameTestHelper h) { com.simplemoney.testing.MoneyTests.launchSmoke(h); }
 @GameTest(maxTicks=100) public void recipes(GameTestHelper h) { com.simplemoney.testing.MoneyTests.recipes(h); }
 @GameTest(maxTicks=100) public void trades(GameTestHelper h) { com.simplemoney.testing.MoneyTests.trades(h); }
 @GameTest(maxTicks=100) public void loot(GameTestHelper h) { com.simplemoney.testing.MoneyTests.loot(h); }
 @GameTest(maxTicks=100) public void config(GameTestHelper h) { com.simplemoney.testing.MoneyTests.config(h); }
 @GameTest(maxTicks=100) public void languagesAndAssets(GameTestHelper h) { com.simplemoney.testing.MoneyTests.languagesAndAssets(h); }
 @GameTest(maxTicks=100) public void simplebuildingStorage(GameTestHelper h) { com.simplemoney.testing.MoneyTests.storage(h); }
 @GameTest(maxTicks=100) public void recipeOutputs(GameTestHelper h) { com.simplemoney.testing.MoneyTests.recipeOutputs(h); }
 @GameTest(maxTicks=100) public void configWorld(GameTestHelper h) { com.simplemoney.testing.MoneyTests.configWorld(h); }
 @GameTest(maxTicks=100) public void billUse(GameTestHelper h) { com.simplemoney.testing.MoneyTests.billUse(h); }
}
