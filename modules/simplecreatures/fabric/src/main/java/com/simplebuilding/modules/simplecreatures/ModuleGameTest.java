package com.simplebuilding.modules.simplecreatures;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge/Forge register CreaturesTests.ALL. */
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){CreaturesTests.launch(h);}
 @GameTest(maxTicks=100) public void config(GameTestHelper h){CreaturesTests.config(h);}
 @GameTest(maxTicks=100) public void qolMigration(GameTestHelper h){CreaturesTests.qolMigration(h);}
 @GameTest(maxTicks=100) public void superSwitch(GameTestHelper h){CreaturesTests.superSwitch(h);}
 @GameTest(maxTicks=100) public void muting(GameTestHelper h){CreaturesTests.muting(h);}
 @GameTest(maxTicks=100) public void baby(GameTestHelper h){CreaturesTests.baby(h);}
 @GameTest(maxTicks=100) public void piglins(GameTestHelper h){CreaturesTests.piglins(h);}
 @GameTest(maxTicks=100) public void goldTrim(GameTestHelper h){CreaturesTests.goldTrim(h);}
 // CAT-END
}
