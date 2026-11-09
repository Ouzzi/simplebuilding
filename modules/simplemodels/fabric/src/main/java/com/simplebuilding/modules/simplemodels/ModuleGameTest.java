package com.simplebuilding.modules.simplemodels;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h) { com.simplebuilding.modules.simplemodels.guide.ModelsGuide.gameTest(h); }
 @GameTest(maxTicks=100) public void subModSwitch(GameTestHelper h) { ModelTests.subModSwitch(h); }
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
 @GameTest(maxTicks=100) public void browserQuery(GameTestHelper h) { ModelTests.browserQuery(h); }
 @GameTest(maxTicks=100) public void examples(GameTestHelper h) { ModelTests.examples(h); }
 @GameTest(maxTicks=100) public void editableRoundTrip(GameTestHelper h) { ModelTests.editableRoundTrip(h); }
 @GameTest(maxTicks=100) public void editableBounds(GameTestHelper h) { ModelTests.editableBounds(h); }
 @GameTest(maxTicks=100) public void editHistory(GameTestHelper h) { ModelTests.editHistory(h); }
 @GameTest(maxTicks=100) public void editorLayout(GameTestHelper h) { EditorTests.layout(h); }
 @GameTest(maxTicks=100) public void editorGeometry(GameTestHelper h) { EditorTests.geometry(h); }
 @GameTest(maxTicks=100) public void editorGizmo(GameTestHelper h) { EditorTests.gizmo(h); }
 @GameTest(maxTicks=100) public void editorState(GameTestHelper h) { EditorTests.state(h); }
 @GameTest(maxTicks=100) public void editorSnapHelp(GameTestHelper h) { EditorTests.snapHelp(h); }
 @GameTest(maxTicks=100) public void editorUv(GameTestHelper h) { EditorTests.uv(h); }
 @GameTest(maxTicks=100) public void editorTextures(GameTestHelper h) { EditorTests.textures(h); }
 @GameTest(maxTicks=100) public void editorExport(GameTestHelper h) { EditorTests.export(h); }
    @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplebuilding.modules.simplemodels.discover.ModelsDiscover.recipeUnlocks(h); }
    @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplebuilding.modules.simplemodels.discover.ModelsDiscover.behaviourHints(h); }
}
