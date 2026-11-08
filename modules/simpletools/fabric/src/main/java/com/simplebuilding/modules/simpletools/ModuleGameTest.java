package com.simplebuilding.modules.simpletools;
import com.simplelib.api.InteractionGuard;
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
 @GameTest(maxTicks=100) public void soulboundHardcore(GameTestHelper h){DeathTests.soulboundHardcore(h);}
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
 @GameTest(maxTicks=100) public void vaultFates(GameTestHelper h){DeathTests.vaultFates(h);}
 @GameTest(maxTicks=100) public void vaultLastDeath(GameTestHelper h){DeathTests.vaultLastDeath(h);}
 @GameTest(maxTicks=100) public void vaultMenuRows(GameTestHelper h){DeathTests.vaultMenuRows(h);}
 @GameTest public void discoverRecipeUnlocks(GameTestHelper h) { com.simplebuilding.modules.simpletools.discover.ToolsDiscover.recipeUnlocks(h); }
 @GameTest public void discoverBehaviourHints(GameTestHelper h) { com.simplebuilding.modules.simpletools.discover.ToolsDiscover.behaviourHints(h); }
 @GameTest(maxTicks=100) public void guideBook(GameTestHelper h){com.simplebuilding.modules.simpletools.guide.ToolsGuide.gameTest(h);}
 @GameTest(maxTicks=100) public void frameInvisible(GameTestHelper h){FrameTests.invisible(h);}
 @GameTest(maxTicks=100) public void frameEmpty(GameTestHelper h){FrameTests.empty(h);}
 @GameTest(maxTicks=100) public void frameReverse(GameTestHelper h){FrameTests.reverse(h);}
 @GameTest(maxTicks=100) public void frameLock(GameTestHelper h){FrameTests.lock(h);}
 @GameTest(maxTicks=100) public void frameGlow(GameTestHelper h){FrameTests.glow(h);}
 @GameTest(maxTicks=100) public void frameGlass(GameTestHelper h){FrameTests.glass(h);}
 @GameTest(maxTicks=100) public void frameMigrate(GameTestHelper h){FrameTests.migrate(h);}
 @GameTest(maxTicks=100) public void frameReveal(GameTestHelper h){FrameTests.reveal(h);}
 @GameTest(maxTicks=100) public void frameConfigOff(GameTestHelper h){FrameTests.configOff(h);}
 @GameTest public void discoverFirstPickupTips(GameTestHelper h) { com.simplebuilding.modules.simpletools.discover.ToolsDiscover.firstPickupTips(h); }
 @GameTest(maxTicks=100) public void biomeBrushItem(GameTestHelper h){ToolsTests.ALL.get("biome_brush_item").accept(h);}
 @GameTest(maxTicks=100) public void biomeBrushPaint(GameTestHelper h){ToolsTests.ALL.get("biome_brush_paint").accept(h);}
 @GameTest(maxTicks=100) public void biomeBrushSneak(GameTestHelper h){ToolsTests.ALL.get("biome_brush_sneak").accept(h);}
 @GameTest(maxTicks=100) public void biomeBrushClaims(GameTestHelper h){ToolsTests.ALL.get("biome_brush_claims").accept(h);}
 @GameTest(maxTicks=100) public void biomeBrushRecipes(GameTestHelper h){ToolsTests.ALL.get("biome_brush_recipes").accept(h);}
 @GameTest(maxTicks=100) public void biomeBrushMoved(GameTestHelper h){ToolsTests.ALL.get("biome_brush_moved").accept(h);}
 @GameTest(maxTicks=100) public void biomeBrushRarity(GameTestHelper h){ToolsTests.ALL.get("biome_brush_rarity").accept(h);}
  // IG31-59: strip station (strip/StripTests)
 @GameTest(maxTicks=100) public void stripStationRegisteredWithRecipe(GameTestHelper h){ToolsTests.ALL.get("strip_station_registered_with_recipe").accept(h);}
 @GameTest(maxTicks=100) public void stripStationTakesAWholeStackAndGivesItBack(GameTestHelper h){ToolsTests.ALL.get("strip_station_takes_a_whole_stack_and_gives_it_back").accept(h);}
 @GameTest(maxTicks=100) public void stripStationStripsAWholeStackForOneDurabilityEach(GameTestHelper h){ToolsTests.ALL.get("strip_station_strips_a_whole_stack_for_one_durability_each").accept(h);}
 @GameTest(maxTicks=100) public void stripStationCopperUnwaxesThenScrapesStepByStep(GameTestHelper h){ToolsTests.ALL.get("strip_station_copper_unwaxes_then_scrapes_step_by_step").accept(h);}
 @GameTest(maxTicks=100) public void stripStationAxeStaysAtOneDurability(GameTestHelper h){ToolsTests.ALL.get("strip_station_axe_stays_at_one_durability").accept(h);}
 @GameTest(maxTicks=200) public void stripStationHoppersFillFromAboveAndEmptyBelow(GameTestHelper h){ToolsTests.ALL.get("strip_station_hoppers_fill_from_above_and_empty_below").accept(h);}
 @GameTest(maxTicks=100) public void stripStationOptionsSwitchItOff(GameTestHelper h){ToolsTests.ALL.get("strip_station_options_switch_it_off").accept(h);}
 @GameTest(maxTicks=100) public void stripStationWorksWoodOfOtherMods(GameTestHelper h){ToolsTests.ALL.get("strip_station_works_wood_of_other_mods").accept(h);}
 // CAT-END
 private static boolean[] probeSeen;
 /** M-11: the permission probe fires the real BEFORE handlers, but area hammers must see it as a veto situation. */
 @GameTest(maxTicks=100) public void permissionProbeVetoesAreas(GameTestHelper h){
  boolean[] seen={false,false};
  if(probeSeen==null){
   probeSeen=seen;
   net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((w,pl,pos,st,be)->{ if(probeSeen!=null){probeSeen[0]=true;probeSeen[1]=com.simplelib.api.HammerActions.areaVetoed();} return true; });
  }
  boolean[] mine=probeSeen; mine[0]=false; mine[1]=false;
  var p=h.makeMockServerPlayerInLevel();
  boolean ok=InteractionGuard.permission.test(p,h.absolutePos(new net.minecraft.core.BlockPos(1,2,1)));
  h.assertTrue(ok&&mine[0]&&mine[1],"probe reached the BEFORE handler with the area veto on: "+mine[0]+"/"+mine[1]);
  h.assertTrue(!com.simplelib.api.HammerActions.areaVetoed(),"veto is off again after the probe");
  h.succeed();
 }
 // WH26-7: Enderite and ceramic buckets (bucket/BucketTests)
 @GameTest(maxTicks=100) public void bucketsRegisteredAndMigrated(GameTestHelper h){ToolsTests.ALL.get("buckets_registered_and_migrated").accept(h);}
 @GameTest(maxTicks=100) public void ceramicBucketHoldsWaterAndBreaksAfterFourPours(GameTestHelper h){ToolsTests.ALL.get("ceramic_bucket_holds_water_and_breaks_after_four_pours").accept(h);}
 @GameTest(maxTicks=100) public void enderiteBucketTakesMilkAndDrinksOneFilling(GameTestHelper h){ToolsTests.ALL.get("enderite_bucket_takes_milk_and_drinks_one_filling").accept(h);}
 @GameTest(maxTicks=100) public void enderiteBucketHoldsTwoBucketsOfOneFluid(GameTestHelper h){ToolsTests.ALL.get("enderite_bucket_holds_two_buckets_of_one_fluid").accept(h);}
 @GameTest(maxTicks=100) public void enderiteBucketTwoBucketsInCauldronsAndDispensers(GameTestHelper h){ToolsTests.ALL.get("enderite_bucket_two_buckets_in_cauldrons_and_dispensers").accept(h);}
 @GameTest(maxTicks=100) public void ceramicLavaBucketScoopsPoursAndWears(GameTestHelper h){ToolsTests.ALL.get("ceramic_lava_bucket_scoops_pours_and_wears").accept(h);}
 @GameTest(maxTicks=100) public void ceramicBucketWithLavaIsBrittleAndBreaksOnPour(GameTestHelper h){ToolsTests.ALL.get("ceramic_bucket_with_lava_is_brittle_and_breaks_on_pour").accept(h);}
 @GameTest(maxTicks=100) public void ceramicBucketWearsThroughStagesAndKeepsItsFilling(GameTestHelper h){ToolsTests.ALL.get("ceramic_bucket_wears_through_stages_and_keeps_its_filling").accept(h);}
}
