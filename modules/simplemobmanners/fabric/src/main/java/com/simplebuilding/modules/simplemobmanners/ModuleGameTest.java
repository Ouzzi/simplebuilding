package com.simplebuilding.modules.simplemobmanners;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge/Forge register MobMannersTests.ALL. */
public final class ModuleGameTest {
 /** N132: every config option, category and the title have an EN and a DE name (configLabels, JSON config). */
 @GameTest(maxTicks = 100) public void configLabels(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.labels(h, "simplemobmanners"); }
 @GameTest public void configCommand(GameTestHelper h) { com.simplelib.test.ConfigCommandTests.module(h, "simplemobmanners", false); }
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){MobMannersTests.launch(h);}
 @GameTest(maxTicks=100) public void config(GameTestHelper h){MobMannersTests.config(h);}
 @GameTest(maxTicks=100) public void qolMigration(GameTestHelper h){MobMannersTests.qolMigration(h);}
 @GameTest(maxTicks=100) public void superSwitch(GameTestHelper h){MobMannersTests.superSwitch(h);}
 @GameTest(maxTicks=100) public void muting(GameTestHelper h){MobMannersTests.muting(h);}
 @GameTest(maxTicks=100) public void baby(GameTestHelper h){MobMannersTests.baby(h);}
 @GameTest(maxTicks=100) public void piglins(GameTestHelper h){MobMannersTests.piglins(h);}
 @GameTest(maxTicks=100) public void goldTrim(GameTestHelper h){MobMannersTests.goldTrim(h);}
 @GameTest(maxTicks=100) public void renameMigration(GameTestHelper h){MobMannersTests.renameMigration(h);}
 @GameTest(maxTicks=100) public void frogBucket(GameTestHelper h){MobMannersTests.frogBucket(h);}
 @GameTest(maxTicks=100) public void jeiFrogBucket(GameTestHelper h){h.assertTrue(com.simplelib.api.RecipeViewer.info(SimpleMobManners.MOD_ID).stream().anyMatch(i->i.items().stream().anyMatch(s->s.is(FrogBucket.item()))&&!i.lines().isEmpty()),"frog bucket info page");h.succeed();}
 // CAT-END
}
