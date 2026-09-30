package com.simplebuilding.modules.simpledimensions;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
public final class ModuleGameTest {
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void launch(GameTestHelper h){DimensionTests.launch(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void worldGeneration(GameTestHelper h){DimensionTests.worldGeneration(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void sixArchesBothAxes(GameTestHelper h){DimensionTests.sixArchesBothAxes(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void recipesAndMutations(GameTestHelper h){DimensionTests.recipesAndMutations(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void separateLight(GameTestHelper h){DimensionTests.separateLight(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void ignitionCosts(GameTestHelper h){DimensionTests.ignitionCosts(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void portalDecay(GameTestHelper h){DimensionTests.portalDecay(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void configBounds(GameTestHelper h){DimensionTests.configBounds(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void hostileConfig(GameTestHelper h){DimensionTests.hostileConfig(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void configFiles(GameTestHelper h){DimensionTests.configFiles(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void legacyEntity(GameTestHelper h){DimensionTests.legacyEntity(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void coordinateRules(GameTestHelper h){DimensionTests.coordinateRules(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void platformGeometry(GameTestHelper h){DimensionTests.platformGeometry(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void safeGround(GameTestHelper h){DimensionTests.safeGround(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void realTravelAndReturn(GameTestHelper h){DimensionTests.realTravelAndReturn(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void disabledReturn(GameTestHelper h){DimensionTests.disabledReturn(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void claimsAndCosts(GameTestHelper h){DimensionTests.claimsAndCosts(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void passengers(GameTestHelper h){DimensionTests.passengers(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void unknownTarget(GameTestHelper h){DimensionTests.unknownTarget(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void cooldownTicks(GameTestHelper h){DimensionTests.cooldownTicks(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void crossModStorage(GameTestHelper h){DimensionTests.crossModStorage(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void vanillaPortals(GameTestHelper h){DimensionTests.vanillaPortals(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void restartReturn(GameTestHelper h){DimensionTests.restartReturn(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void borderAndHeight(GameTestHelper h){DimensionTests.borderAndHeight(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void warmupAndNoBounce(GameTestHelper h){DimensionTests.warmupAndNoBounce(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void noOverwrite(GameTestHelper h){DimensionTests.noOverwrite(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void outOfRangeIgnition(GameTestHelper h){DimensionTests.outOfRangeIgnition(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void removedDefinitionReturn(GameTestHelper h){DimensionTests.removedDefinitionReturn(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void playerInventoryRoundtrip(GameTestHelper h){DimensionTests.playerInventoryRoundtrip(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void configAndLang(GameTestHelper h){DimensionTests.configAndLang(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void miningTravel(GameTestHelper h){DimensionTests.miningTravel(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void compressedTravel(GameTestHelper h){DimensionTests.compressedTravel(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void exactOriginLinks(GameTestHelper h){DimensionTests.exactOriginLinks(h);}
 @GameTest(structure="simpledimensions:empty",maxTicks=240) public void destinationClaim(GameTestHelper h){DimensionTests.destinationClaim(h);}
}
