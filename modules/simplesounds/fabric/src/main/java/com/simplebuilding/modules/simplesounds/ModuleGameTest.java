package com.simplebuilding.modules.simplesounds;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest public void guideBook(GameTestHelper h){com.simplebuilding.modules.simplesounds.guide.SoundsGuide.gameTest(h);}
 @GameTest public void launchSmoke(GameTestHelper h){SoundTests.launch(h);}
 @GameTest public void configBounds(GameTestHelper h){SoundTests.bounds(h);}
 @GameTest public void soundFloodSafety(GameTestHelper h){SoundTests.flood(h);}
 @GameTest public void cooldownAndWorldReset(GameTestHelper h){SoundTests.cooldown(h);}
 @GameTest public void simpleBuildingIntegration(GameTestHelper h){
  h.assertTrue(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplevisuals") ==
   (com.simplebuilding.framework.api.CosmeticIntensity.current("simplevisuals") != null), "Loaded optional Visuals publishes through the shared API");
  SoundTests.integration(h);
 }
 @GameTest public void effectFootstepDust(GameTestHelper h){SoundTests.effect(h,"footstep_dust");}
 @GameTest public void effectColdBreath(GameTestHelper h){SoundTests.effect(h,"cold_breath");}
 @GameTest public void effectFireflies(GameTestHelper h){SoundTests.effect(h,"fireflies");}
 @GameTest public void effectPollen(GameTestHelper h){SoundTests.effect(h,"pollen");}
 @GameTest public void effectFireSparks(GameTestHelper h){SoundTests.effect(h,"fire_sparks");}
 @GameTest public void effectWaterRipples(GameTestHelper h){SoundTests.effect(h,"water_ripples");}
 @GameTest public void effectWaterDroplets(GameTestHelper h){SoundTests.effect(h,"water_droplets");}
 @GameTest public void effectLeafFall(GameTestHelper h){SoundTests.effect(h,"leaf_fall");}
 @GameTest public void effectEnchantedItems(GameTestHelper h){SoundTests.effect(h,"enchanted_items");}
 @GameTest public void effectBeaconAura(GameTestHelper h){SoundTests.effect(h,"beacon_aura");}
 @GameTest public void effectDamageFeedback(GameTestHelper h){SoundTests.effect(h,"damage_feedback");}
 @GameTest public void effectHealingFeedback(GameTestHelper h){SoundTests.effect(h,"healing_feedback");}
}
