package com.simplebuilding.modules.simplemovement;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge/Forge register MovementTests.ALL. */
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){MovementTests.launch(h);}
 @GameTest(maxTicks=100) public void config(GameTestHelper h){MovementTests.config(h);}
 @GameTest(maxTicks=100) public void qolMigration(GameTestHelper h){MovementTests.qolMigration(h);}
 @GameTest(maxTicks=100) public void superSwitch(GameTestHelper h){MovementTests.superSwitch(h);}
 @GameTest(maxTicks=100) public void keyMigration(GameTestHelper h){MovementTests.keyMigration(h);}
 @GameTest(maxTicks=100) public void sync(GameTestHelper h){MovementTests.sync(h);}
 @GameTest(maxTicks=100) public void configBounds(GameTestHelper h){MovementTests.configBounds(h);}
 @GameTest(maxTicks=100) public void crawl(GameTestHelper h){MovementTests.crawl(h);}
 @GameTest(maxTicks=100) public void climbPackets(GameTestHelper h){MovementTests.climbPackets(h);}
 @GameTest(maxTicks=100) public void climbMechanics(GameTestHelper h){MovementTests.climbMechanics(h);}
 @GameTest(maxTicks=100) public void powderSnow(GameTestHelper h){MovementTests.powderSnow(h);}
 @GameTest(maxTicks=100) public void realMovementPackets(GameTestHelper h){MovementTests.realMovementPackets(h);}
 @GameTest(maxTicks=100) public void featureSwitches(GameTestHelper h){MovementTests.featureSwitches(h);}
 // CAT-END
}
