package com.simplebuilding.modules.simpletweaks.claims;
import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.phys.Vec3;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Findings of the phase 1 review (b-claimreview): one test per closed gap. */
final class ReviewClaimTests {
 /** A vehicle right alone must not open chest boats or chest minecarts; it still moves plain vehicles. */
 static void vehicleContainers(GameTestHelper h) {
  var level=h.getLevel();var server=level.getServer();var previous=Claims.get(server);
  var ground=ToolClaimTests.boundary(h);var key=new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(ground));
  var guest=player(h,ground.east(3));UUID g=guest.getUUID();
  try {
   var chest=EntityTypes.CHEST_MINECART.create(level,EntitySpawnReason.COMMAND);chest.setPos(Vec3.atBottomCenterOf(ground.east(3)));
   var plain=EntityTypes.MINECART.create(level,EntitySpawnReason.COMMAND);plain.setPos(Vec3.atBottomCenterOf(ground.east(3)));
   ClaimRightsTests.seeded(h,temp(),key,ClaimRightsTests.OWNER,new Rules(0,Map.of(g,ClaimRights.ANIMALS)),Map.of());
   yes(h,Claims.allowEntity(guest,plain),"A may move a plain minecart");
   yes(h,!Claims.allowEntity(guest,chest),"A alone does not open a chest minecart");
   yes(h,guest.interactOn(chest,InteractionHand.MAIN_HAND,Vec3.ZERO)==net.minecraft.world.InteractionResult.FAIL,"Real interaction with a chest minecart is refused");
   ClaimRightsTests.seeded(h,temp(),key,ClaimRightsTests.OWNER,new Rules(0,Map.of(g,ClaimRights.CONTAINER)),Map.of());
   yes(h,!Claims.allowEntity(guest,chest),"C alone does not open a chest minecart either (it is a vehicle)");
   ClaimRightsTests.seeded(h,temp(),key,ClaimRightsTests.OWNER,new Rules(0,Map.of(g,ClaimRights.ANIMALS|ClaimRights.CONTAINER)),Map.of());
   yes(h,Claims.allowEntity(guest,chest),"A and C open it");
  } finally { Claims.install(server,previous); }
  h.succeed();
 }
 /** Trees and huge mushrooms do not grow across a claim border; the same plant grows where nothing is claimed nearby. */
 static void growth(GameTestHelper h) {
  var level=h.getLevel();var east=ToolClaimTests.boundary(h);var sapling=east.west(3);
  for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){
   var f=sapling.offset(x,-1,z);level.setBlock(f,Blocks.DIRT.defaultBlockState(),2);
   for(int y=0;y<12;y++)level.setBlock(f.above(y+1),Blocks.AIR.defaultBlockState(),2);
  }
  var state=Blocks.OAK_SAPLING.defaultBlockState();
  with(h,c->{
   int grown=0;
   for(int seed=0;seed<10&&grown==0;seed++){
    level.setBlock(sapling,state,2);
    if(TreeGrower.OAK.growTree(level,level.getChunkSource().getGenerator(),sapling,state,RandomSource.create(seed)))grown++;
    else level.setBlock(sapling,Blocks.AIR.defaultBlockState(),2);
   }
   yes(h,grown==1,"Control: an oak grows without claims");
   for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)for(int y=0;y<12;y++)level.setBlock(sapling.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
   yes(h,Claims.growth(level,sapling),"Plant in wilderness, no claim nearby: allowed");
   var owner=player(h,east.east(8));
   yes(h,c.create(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(east)),owner.getUUID(),100),"Growth claim fixture");
   yes(h,!Claims.growth(level,sapling),"Plant in wilderness next to a claim: refused");
   yes(h,Claims.growth(level,east.east(8)),"Plant inside the claim away from foreign land: allowed");
   level.setBlock(sapling,state,2);
   for(int seed=0;seed<10;seed++){
    yes(h,!TreeGrower.OAK.growTree(level,level.getChunkSource().getGenerator(),sapling,state,RandomSource.create(seed)),"Real tree growth refused next to a claim, seed "+seed);
    yes(h,level.getBlockState(sapling.above()).isAir(),"No log placed, seed "+seed);
   }
  });
  h.succeed();
 }

 private static boolean bonemealOk(net.minecraft.server.level.ServerLevel level,BlockPos pos){
  var state=level.getBlockState(pos);
  return ((net.minecraft.world.level.block.BonemealableBlock)state.getBlock()).isValidBonemealTarget(level,pos,state,net.minecraft.world.level.block.BonemealSource.INTERACTION);
 }
 /** Bone meal on grass does not spread its flowers and grass over a claim border; crops and claim-internal use stay possible. */
 static void bonemeal(GameTestHelper h) {
  var level=h.getLevel();var east=ToolClaimTests.boundary(h);var near=east.west(2);var far=east.west(12);var inner=east.east(8);var crop=east.west();
  for(var p:List.of(near,far,inner,crop)){
   level.setBlock(p.below(),Blocks.DIRT.defaultBlockState(),2);level.setBlock(p,Blocks.AIR.defaultBlockState(),2);level.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2);
  }
  level.setBlock(near,Blocks.GRASS_BLOCK.defaultBlockState(),2);level.setBlock(far,Blocks.GRASS_BLOCK.defaultBlockState(),2);level.setBlock(inner,Blocks.GRASS_BLOCK.defaultBlockState(),2);
  level.setBlock(crop.below(),Blocks.FARMLAND.defaultBlockState(),2);level.setBlock(crop,Blocks.WHEAT.defaultBlockState(),2);
  with(h,c->{
   yes(h,bonemealOk(level,near),"Control: grass takes bone meal without claims");
   var owner=player(h,east.east(8));
   yes(h,c.create(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(east)),owner.getUUID(),100),"Bone meal claim fixture");
   yes(h,!bonemealOk(level,near),"Grass 2 blocks from a foreign claim: refused");
   yes(h,bonemealOk(level,far),"Grass far from the claim: allowed");
   yes(h,bonemealOk(level,inner),"Grass deep inside the claim: allowed");
   yes(h,bonemealOk(level,crop),"A crop next to the claim still takes bone meal (no spread)");
  });
  h.succeed();
 }
 /** A sculk charge from a catalyst outside a claim dies at the border instead of sculking foreign land. */
 static void sculk(GameTestHelper h) {
  var level=h.getLevel();var east=ToolClaimTests.boundary(h);var origin=east.west(6);
  level.setBlock(east,Blocks.STONE.defaultBlockState(),2);level.setBlock(origin,Blocks.STONE.defaultBlockState(),2);
  with(h,c->{
   var spreader=net.minecraft.world.level.block.SculkSpreader.createLevelSpreader();
   spreader.addCursors(east,10);spreader.updateCursors(level,origin,RandomSource.create(1),true);
   yes(h,!spreader.getCursors().isEmpty(),"Control: a charge in wilderness survives an update");
   var owner=player(h,east.east(8));
   yes(h,c.create(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(east)),owner.getUUID(),100),"Sculk claim fixture");
   spreader.clear();spreader.addCursors(east,10);spreader.updateCursors(level,origin,RandomSource.create(1),true);
   yes(h,spreader.getCursors().isEmpty(),"A charge inside a foreign claim is discharged");
   yes(h,!level.getBlockState(east.above()).is(Blocks.SCULK_VEIN),"No vein placed in the claim");
   spreader.clear();spreader.addCursors(origin,10);spreader.updateCursors(level,origin,RandomSource.create(1),true);
   yes(h,!spreader.getCursors().isEmpty(),"A charge on its own side keeps working");
  });
  h.succeed();
 }
 /** Frost Walker ice appears where the wearer may build, not on a foreign claim's water. */
 static void frostWalker(GameTestHelper h) {
  var level=h.getLevel();var east=ToolClaimTests.boundary(h);var edge=east.west();
  for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setBlock(edge.offset(x,0,z),Blocks.WATER.defaultBlockState(),2);
  var disk=new net.minecraft.world.item.enchantment.effects.ReplaceDisk(net.minecraft.world.item.enchantment.LevelBasedValue.constant(3),net.minecraft.world.item.enchantment.LevelBasedValue.constant(1),net.minecraft.core.Vec3i.ZERO,
   Optional.of(net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.matchesBlocks(Blocks.WATER)),
   net.minecraft.core.Holder.direct(new net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider(Blocks.ICE.defaultBlockState())),Optional.empty());
  with(h,c->{
   var stranger=player(h,edge.west(5));var owner=player(h,east.east(8));
   disk.apply(level,1,null,stranger,Vec3.atCenterOf(edge));
   yes(h,level.getBlockState(edge).is(Blocks.ICE),"Control: ice forms without claims");
   for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setBlock(edge.offset(x,0,z),Blocks.WATER.defaultBlockState(),2);
   yes(h,c.create(new ClaimStore.Key(level.dimension().identifier().toString(),ChunkPos.pack(east)),owner.getUUID(),100),"Frost claim fixture");
   disk.apply(level,1,null,stranger,Vec3.atCenterOf(edge));
   yes(h,level.getBlockState(edge).is(Blocks.ICE),"Ice forms on wilderness water");
   yes(h,level.getBlockState(east).is(Blocks.WATER)&&level.getBlockState(east.east()).is(Blocks.WATER),"Foreign claim water stays water for a stranger");
   for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.setBlock(edge.offset(x,0,z),Blocks.WATER.defaultBlockState(),2);
   disk.apply(level,1,null,owner,Vec3.atCenterOf(edge));
   yes(h,level.getBlockState(east).is(Blocks.ICE),"The claim owner freezes their own water");
  });
  h.succeed();
 }
}
