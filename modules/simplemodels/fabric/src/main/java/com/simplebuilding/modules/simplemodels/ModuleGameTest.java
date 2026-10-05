package com.simplebuilding.modules.simplemodels;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h) { com.simplebuilding.modules.simplemodels.guide.ModelsGuide.gameTest(h); }
 @GameTest(maxTicks=100) public void launch(GameTestHelper h) { ModelTests.launch(h); }
 @GameTest(maxTicks=100) public void assignment(GameTestHelper h) { ModelTests.assignment(h); }
 @GameTest(maxTicks=100) public void permissions(GameTestHelper h) { ModelTests.permissions(h); }
 @GameTest(maxTicks=100) public void requestBounds(GameTestHelper h) { ModelTests.requestBounds(h); }
 @GameTest(maxTicks=100) public void configBounds(GameTestHelper h) { ModelTests.configBounds(h); }
 @GameTest(maxTicks=100) public void definitionBounds(GameTestHelper h) { ModelTests.definitionBounds(h); }
 @GameTest(maxTicks=100) public void folderScan(GameTestHelper h) { ModelTests.folderScan(h); }
 @GameTest(maxTicks=100) public void reloadFailClosed(GameTestHelper h) { ModelTests.reloadFailClosed(h); }
 @GameTest(maxTicks=100) public void catalogueSync(GameTestHelper h) { ModelTests.catalogueSync(h); }
 @GameTest(maxTicks=100) public void search(GameTestHelper h) { ModelTests.search(h); }
 @GameTest(maxTicks=100) public void anvilCostAndTake(GameTestHelper h) { ModelTests.anvilCostAndTake(h); }
 @GameTest(maxTicks=100) public void staleResult(GameTestHelper h) { ModelTests.staleResult(h); }
 @GameTest(maxTicks=100) public void normalRename(GameTestHelper h) { ModelTests.normalRename(h); }
 @GameTest(maxTicks=100) public void crossMod(GameTestHelper h) { ModelTests.crossMod(h); }
 @GameTest(maxTicks=100) public void legacyNames(GameTestHelper h) { ModelTests.legacyNames(h); }
 @GameTest(maxTicks=100) public void configAndLang(GameTestHelper h) { ModelTests.configAndLang(h); }
}
