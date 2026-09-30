package com.simplebuilding.modules.simplevisuals;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest public void launchSmoke(GameTestHelper h){com.simplevisuals.test.VisualsTests.launch(h);}
 @GameTest public void configBoundsAndLegacyKeys(GameTestHelper h){com.simplevisuals.test.VisualsTests.config(h);}
 @GameTest public void configAndLanguageCompleteness(GameTestHelper h){com.simplevisuals.test.VisualsTests.language(h);}
 @GameTest public void particleFloodSafety(GameTestHelper h){com.simplevisuals.test.VisualsTests.budgets(h);}
 @GameTest public void serverAnvilAndCrossMod(GameTestHelper h){com.simplevisuals.test.VisualsTests.formatting(h);}
 @GameTest public void assetDecompressionBounds(GameTestHelper h){com.simplevisuals.test.VisualsTests.assetBounds(h);}
 @GameTest public void effectFootstepDust(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"footstep_dust");}
 @GameTest public void effectColdBreath(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"cold_breath");}
 @GameTest public void effectFireflies(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"fireflies");}
 @GameTest public void effectPollen(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"pollen");}
 @GameTest public void effectFireSparks(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"fire_sparks");}
 @GameTest public void effectWaterRipples(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"water_ripples");}
 @GameTest public void effectWaterDroplets(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"water_droplets");}
 @GameTest public void effectLeafFall(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"leaf_fall");}
 @GameTest public void effectEnchantedItems(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"enchanted_items");}
 @GameTest public void effectBeaconAura(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"beacon_aura");}
 @GameTest public void effectDamageFeedback(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"damage_feedback");}
 @GameTest public void effectHealingFeedback(GameTestHelper h){com.simplevisuals.test.VisualsTests.effect(h,"healing_feedback");}
}
