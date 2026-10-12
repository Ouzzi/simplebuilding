package com.simplebuilding.modules.simpletools;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge/Forge register ToolsTests.ALL. */
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){ToolsTests.launch(h);}
 @GameTest(maxTicks=100) public void config(GameTestHelper h){ToolsTests.config(h);}
 @GameTest(maxTicks=100) public void qolMigration(GameTestHelper h){ToolsTests.qolMigration(h);}
 @GameTest(maxTicks=100) public void superSwitch(GameTestHelper h){ToolsTests.superSwitch(h);}
 @GameTest(maxTicks=100) public void lavaSwitch(GameTestHelper h){ToolsTests.lavaSwitch(h);}
 @GameTest(maxTicks=100) public void sync(GameTestHelper h){ToolsTests.sync(h);}
 @GameTest(maxTicks=100) public void durability(GameTestHelper h){ToolsTests.durability(h);}
 @GameTest(maxTicks=100) public void thrift(GameTestHelper h){ToolsTests.thrift(h);}
 @GameTest(maxTicks=100) public void anvilRepairCost(GameTestHelper h){ToolsTests.anvilRepairCost(h);}
 @GameTest(maxTicks=100) public void furnaceLava(GameTestHelper h){ToolsTests.furnaceLava(h);}
 @GameTest(maxTicks=100) public void permissionsSpam(GameTestHelper h){ToolsTests.permissionsSpam(h);}
 @GameTest(maxTicks=100) public void crossMod(GameTestHelper h){ToolsTests.crossMod(h);}
 @GameTest(maxTicks=100) public void soulboundRegistered(GameTestHelper h){DeathTests.soulboundRegistered(h);}
 @GameTest(maxTicks=100) public void soulboundDeath(GameTestHelper h){DeathTests.soulboundDeath(h);}
 @GameTest(maxTicks=100) public void soulboundSwitch(GameTestHelper h){DeathTests.soulboundSwitch(h);}
 @GameTest(maxTicks=100) public void deathDurabilityCap(GameTestHelper h){DeathTests.durabilityCap(h);}
 @GameTest(maxTicks=100) public void soulboundAnvil(GameTestHelper h){DeathTests.soulboundAnvil(h);}
 @GameTest(maxTicks=100) public void soulboundGrindstone(GameTestHelper h){DeathTests.soulboundGrindstone(h);}
 @GameTest(maxTicks=100) public void soulboundSources(GameTestHelper h){DeathTests.soulboundSources(h);}
 @GameTest(maxTicks=100) public void vaultLying(GameTestHelper h){DeathTests.vaultLying(h);}
 @GameTest(maxTicks=100) public void vaultLost(GameTestHelper h){DeathTests.vaultLost(h);}
 @GameTest(maxTicks=100) public void vaultTaken(GameTestHelper h){DeathTests.vaultTaken(h);}
 @GameTest(maxTicks=100) public void vaultPvp(GameTestHelper h){DeathTests.vaultPvp(h);}
 @GameTest(maxTicks=100) public void vaultWeights(GameTestHelper h){DeathTests.vaultWeights(h);}
 @GameTest(maxTicks=100) public void vaultDupe(GameTestHelper h){DeathTests.vaultDupe(h);}
 @GameTest(maxTicks=100) public void vaultPersistence(GameTestHelper h){DeathTests.vaultPersistence(h);}
 // CAT-END
}
