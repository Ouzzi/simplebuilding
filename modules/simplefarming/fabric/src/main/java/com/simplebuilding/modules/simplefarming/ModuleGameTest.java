package com.simplebuilding.modules.simplefarming;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge/Forge register FarmingTests.ALL. */
public final class ModuleGameTest {
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simplefarming", false); }
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){FarmingTests.launch(h);}
 @GameTest(maxTicks=100) public void config(GameTestHelper h){FarmingTests.config(h);}
 @GameTest(maxTicks=100) public void qolMigration(GameTestHelper h){FarmingTests.qolMigration(h);}
 @GameTest(maxTicks=100) public void superSwitch(GameTestHelper h){FarmingTests.superSwitch(h);}
 @GameTest(maxTicks=100) public void hoeSwitch(GameTestHelper h){FarmingTests.hoeSwitch(h);}
 @GameTest(maxTicks=100) public void crossMod(GameTestHelper h){FarmingTests.crossMod(h);}
 @GameTest(maxTicks=100) public void sync(GameTestHelper h){FarmingTests.sync(h);}
 @GameTest(maxTicks=100) public void farmland(GameTestHelper h){FarmingTests.farmland(h);}
 @GameTest(maxTicks=100) public void hoeHarvest(GameTestHelper h){FarmingTests.hoeHarvest(h);}
 @GameTest(maxTicks=100) public void vegetation(GameTestHelper h){FarmingTests.vegetation(h);}
 @GameTest(maxTicks=100) public void sharpnessAction(GameTestHelper h){FarmingTests.sharpnessAction(h);}
 @GameTest(maxTicks=100) public void grassThrough(GameTestHelper h){FarmingTests.grassThrough(h);}
 // CAT-END
}
