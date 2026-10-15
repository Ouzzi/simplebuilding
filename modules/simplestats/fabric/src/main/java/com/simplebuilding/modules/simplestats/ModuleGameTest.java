package com.simplebuilding.modules.simplestats;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
/** Fabric catalogue (Fabric runs only {@code @GameTest} methods); NeoForge registers StatsTests.ALL. */
public final class ModuleGameTest {
 @GameTest(maxTicks=100) public void launch(GameTestHelper h){StatsTests.launch(h);}
 @GameTest(maxTicks=100) public void codecRoundtrip(GameTestHelper h){StatsTests.codecRoundtrip(h);}
 @GameTest(maxTicks=100) public void foreignVersion(GameTestHelper h){StatsTests.foreignVersion(h);}
 @GameTest(maxTicks=100) public void mining(GameTestHelper h){StatsTests.mining(h);}
 @GameTest(maxTicks=100) public void miningBundling(GameTestHelper h){StatsTests.miningBundling(h);}
 @GameTest(maxTicks=100) public void killVariants(GameTestHelper h){StatsTests.killVariants(h);}
 @GameTest(maxTicks=100) public void playerKills(GameTestHelper h){StatsTests.playerKills(h);}
 @GameTest(maxTicks=100) public void arrowAfterHandSwap(GameTestHelper h){StatsTests.arrowAfterHandSwap(h);}
 @GameTest(maxTicks=100) public void armorAbsorbed(GameTestHelper h){StatsTests.armorAbsorbed(h);}
 @GameTest(maxTicks=100) public void fallBoots(GameTestHelper h){StatsTests.fallBoots(h);}
 @GameTest(maxTicks=100) public void shieldBlocked(GameTestHelper h){StatsTests.shieldBlocked(h);}
 @GameTest(maxTicks=100) public void elytraAndBoots(GameTestHelper h){StatsTests.elytraAndBoots(h);}
 @GameTest(maxTicks=100) public void fishing(GameTestHelper h){StatsTests.fishing(h);}
 @GameTest(maxTicks=100) public void toolUses(GameTestHelper h){StatsTests.toolUses(h);}
 @GameTest(maxTicks=100) public void shearing(GameTestHelper h){StatsTests.shearing(h);}
 @GameTest(maxTicks=100) public void brush(GameTestHelper h){StatsTests.brush(h);}
 @GameTest(maxTicks=100) public void creativeAndMachines(GameTestHelper h){StatsTests.creativeAndMachines(h);}
 @GameTest(maxTicks=100) public void switchOff(GameTestHelper h){StatsTests.switchOff(h);}
 @GameTest(maxTicks=100) public void smithingInherits(GameTestHelper h){StatsTests.smithingInherits(h);}
 @GameTest(maxTicks=100) public void resetCommand(GameTestHelper h){StatsTests.resetCommand(h);}
 @GameTest(maxTicks=100) public void names(GameTestHelper h){StatsTests.names(h);}
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h){com.simplebuilding.modules.simplestats.guide.StatsGuide.gameTest(h);}
 // CAT-END
}
