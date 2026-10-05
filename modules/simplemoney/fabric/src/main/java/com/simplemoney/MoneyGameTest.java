package com.simplemoney;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class MoneyGameTest {
 @GameTest(maxTicks=100) public void launchSmoke(GameTestHelper h) { com.simplemoney.testing.MoneyTests.launchSmoke(h); }
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h) { com.simplemoney.guide.MoneyGuide.gameTest(h); }
 @GameTest(maxTicks=100) public void recipes(GameTestHelper h) { com.simplemoney.testing.MoneyTests.recipes(h); }
 @GameTest(maxTicks=100) public void trades(GameTestHelper h) { com.simplemoney.testing.MoneyTests.trades(h); }
 @GameTest(maxTicks=100) public void loot(GameTestHelper h) { com.simplemoney.testing.MoneyTests.loot(h); }
 @GameTest(maxTicks=100) public void config(GameTestHelper h) { com.simplemoney.testing.MoneyTests.config(h); }
 @GameTest(maxTicks=100) public void languagesAndAssets(GameTestHelper h) { com.simplemoney.testing.MoneyTests.languagesAndAssets(h); }
 @GameTest(maxTicks=100) public void simplebuildingStorage(GameTestHelper h) { com.simplemoney.testing.MoneyTests.storage(h); }
 @GameTest(maxTicks=100) public void recipeOutputs(GameTestHelper h) { com.simplemoney.testing.MoneyTests.recipeOutputs(h); }
 @GameTest(maxTicks=100) public void configWorld(GameTestHelper h) { com.simplemoney.testing.MoneyTests.configWorld(h); }
 @GameTest(maxTicks=100) public void creativeTabs(GameTestHelper h) { com.simplemoney.testing.MoneyTests.creativeTabs(h); }
 @GameTest(maxTicks=100) public void billUse(GameTestHelper h) { com.simplemoney.testing.MoneyTests.billUse(h); }
 @GameTest(maxTicks=100) public void linkConditions(GameTestHelper h) { com.simplemoney.testing.LinkTests.conditions(h); }
 @GameTest(maxTicks=100) public void linkOffers(GameTestHelper h) { com.simplemoney.testing.LinkTests.offers(h); }
 @GameTest(maxTicks=100) public void linkBounds(GameTestHelper h) { com.simplemoney.testing.LinkTests.bounds(h); }
 @GameTest(maxTicks=100) public void linkRarity(GameTestHelper h) { com.simplemoney.testing.LinkTests.rarity(h); }
 @GameTest(maxTicks=100) public void linkBudgets(GameTestHelper h) { com.simplemoney.testing.LinkTests.budgets(h); }
 @GameTest(maxTicks=100) public void linkMenu(GameTestHelper h) { com.simplemoney.testing.LinkTests.menu(h); }
 @GameTest(maxTicks=100) public void linkNoArbitrage(GameTestHelper h) { com.simplemoney.testing.LinkTests.noArbitrage(h); }
}
