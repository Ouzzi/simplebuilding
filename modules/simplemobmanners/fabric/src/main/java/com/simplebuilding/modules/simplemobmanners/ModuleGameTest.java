package com.simplebuilding.modules.simplemobmanners;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge/Forge register MobMannersTests.ALL. */
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){MobMannersTests.launch(h);}
 @GameTest(maxTicks=100) public void config(GameTestHelper h){MobMannersTests.config(h);}
 @GameTest(maxTicks=100) public void qolMigration(GameTestHelper h){MobMannersTests.qolMigration(h);}
 @GameTest(maxTicks=100) public void superSwitch(GameTestHelper h){MobMannersTests.superSwitch(h);}
 @GameTest(maxTicks=100) public void muting(GameTestHelper h){MobMannersTests.muting(h);}
 @GameTest(maxTicks=100) public void baby(GameTestHelper h){MobMannersTests.baby(h);}
 @GameTest(maxTicks=100) public void piglins(GameTestHelper h){MobMannersTests.piglins(h);}
 @GameTest(maxTicks=100) public void goldTrim(GameTestHelper h){MobMannersTests.goldTrim(h);}
 @GameTest(maxTicks=100) public void renameMigration(GameTestHelper h){MobMannersTests.renameMigration(h);}
 // CAT-END
}
