package com.simplebuilding.modules.simpletweaks.claims;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import com.simplebuilding.modules.simpletweaks.mixin.claims.*;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;
final class EnvironmentClaimTests {
 static void explosionFire(GameTestHelper h){with(h,c->{
  var l=h.getLevel();var inside=ToolClaimTests.boundary(h);var outside=inside.west();var owner=player(h,inside.east(8));
  yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Explosion claim fixture");
  l.setBlock(inside,Blocks.STONE.defaultBlockState(),3);l.setBlock(outside,Blocks.STONE.defaultBlockState(),3);
  var protectedCow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);protectedCow.setPos(Vec3.atBottomCenterOf(inside.east()));l.addFreshEntity(protectedCow);float health=protectedCow.getHealth();
  new ServerExplosion(l,null,null,null,Vec3.atCenterOf(outside),4,false,Explosion.BlockInteraction.DESTROY).explode();
  yes(h,l.getBlockState(inside).is(Blocks.STONE)&&l.getBlockState(outside).isAir(),"Real explosion removes wilderness and preserves claimed target");
  yes(h,protectedCow.getHealth()==health&&protectedCow.getDeltaMovement().equals(Vec3.ZERO),"Explosion cannot damage or push a claimed entity");protectedCow.discard();
  l.setBlock(inside,Blocks.OAK_PLANKS.defaultBlockState(),3);l.setBlock(outside.west(),Blocks.OAK_PLANKS.defaultBlockState(),3);
  l.setBlock(outside.below(),Blocks.NETHERRACK.defaultBlockState(),3);l.setBlock(outside,Blocks.FIRE.defaultBlockState(),3);var random=RandomSource.create(0);
  for(int t=0;t<400;t++)l.getBlockState(outside).tick(l,outside,random);
  yes(h,l.getBlockState(inside).is(Blocks.OAK_PLANKS)&&!l.getBlockState(outside.west()).is(Blocks.OAK_PLANKS),"Actual fire ticks preserve claim and burn unclaimed wood");
 });h.succeed();}
 static void fluidPiston(GameTestHelper h){with(h,c->{
  var l=h.getLevel();var inside=ToolClaimTests.boundary(h);var outside=inside.west();var owner=player(h,inside.east(8));
  yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Fluid piston fixture");
  for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++){var p=inside.offset(x,0,z);l.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);l.setBlock(p,Blocks.AIR.defaultBlockState(),3);}
  l.setBlock(outside,Blocks.WATER.defaultBlockState(),3);l.getFluidState(outside).tick(l,outside,l.getBlockState(outside));
  yes(h,l.getFluidState(inside).isEmpty()&&!l.getFluidState(outside.west()).isEmpty(),"Real fluid tick cannot cross ownership boundary; wilderness flow continues");
  l.setBlock(inside.east(2),Blocks.WATER.defaultBlockState(),3);l.getFluidState(inside.east(2)).tick(l,inside.east(2),l.getBlockState(inside.east(2)));
  yes(h,!l.getFluidState(inside.east(3)).isEmpty(),"Fluid flow inside the same owner continues");
  var piston=outside.west();l.setBlock(piston,Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,Direction.EAST),3);l.setBlock(outside,Blocks.STONE.defaultBlockState(),3);l.setBlock(inside,Blocks.AIR.defaultBlockState(),3);
  yes(h,!((ClaimPistonInvoker)Blocks.PISTON).claims$move(l,piston,Direction.EAST,true)&&l.getBlockState(outside).is(Blocks.STONE)&&l.getBlockState(inside).isAir(),"Actual piston movement refuses foreign destination before source mutation");
  var ownPiston=inside.east(3);l.setBlock(ownPiston,Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING,Direction.EAST),3);l.setBlock(ownPiston.east(),Blocks.STONE.defaultBlockState(),3);l.setBlock(ownPiston.east(2),Blocks.AIR.defaultBlockState(),3);
  yes(h,((ClaimPistonInvoker)Blocks.PISTON).claims$move(l,ownPiston,Direction.EAST,true)&&!l.getBlockState(ownPiston.east(2)).isAir(),"Actual piston moves blocks within one owner");
 });h.succeed();}
 @SuppressWarnings({"rawtypes","unchecked"})
 static void hopper(GameTestHelper h){with(h,c->{
  var l=h.getLevel();var inside=ToolClaimTests.boundary(h);var owner=player(h,inside.east(10));
  yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Hopper fixture");
  for(var block:List.of(Blocks.HOPPER,BuiltInRegistries.BLOCK.getValue(Identifier.parse("simplebuilding:reinforced_hopper"))))for(boolean same:new boolean[]{false,true}){
   var from=same?inside.east(3):inside.west();var target=from.east();
   for(var pos:List.of(from,target)){if(l.getBlockEntity(pos) instanceof Container old)old.clearContent();l.setBlock(pos,Blocks.AIR.defaultBlockState(),3);}
   l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(from).inflate(2)).forEach(Entity::discard);
   var state=block.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST);l.setBlock(from,state,3);l.setBlock(target,Blocks.CHEST.defaultBlockState(),3);
   var be=l.getBlockEntity(from);var source=(Container)be;var dest=(Container)l.getBlockEntity(target);source.setItem(0,new ItemStack(Items.APPLE,3));
   var ticker=(BlockEntityTicker<BlockEntity>)(BlockEntityTicker)((EntityBlock)block).getTicker(l,state,be.getType());for(int t=0;t<16;t++)ticker.tick(l,from,state,be);
   yes(h,same?!dest.isEmpty():dest.isEmpty()&&source.getItem(0).getCount()==3,"Real hopper ticker boundary/owner control: "+block+" same="+same);
  }
 });h.succeed();}
 static void projectiles(GameTestHelper h){with(h,c->{
  var l=h.getLevel();var inside=ToolClaimTests.boundary(h).east(2).above(80);var owner=player(h,inside.west(10));var other=player(h,inside.west(10));
  yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Projectile fixture");
  var cow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);cow.setPos(Vec3.atBottomCenterOf(inside));l.addFreshEntity(cow);float health=cow.getHealth();
  for(var actor:List.of(other,owner)){
   var arrow=EntityTypes.ARROW.create(l,EntitySpawnReason.COMMAND);arrow.setOwner(actor);arrow.setNoGravity(true);arrow.setPos(inside.getX()-3,inside.getY()+.7,inside.getZ()+.5);arrow.setDeltaMovement(1,0,0);l.addFreshEntity(arrow);
   for(int t=0;t<8&&!arrow.isRemoved();t++)arrow.tick();arrow.discard();
   yes(h,(cow.getHealth()<health)==(actor==owner),"Actual projectile ticks enforce shooter authority; owner="+(actor==owner)+", health="+cow.getHealth());
  }
  cow.removeAllEffects();yes(h,!cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100),other),"Foreign indirect effect refused at actual effect hook");
  yes(h,cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100),owner),"Owner effect allowed");
  cow.discard();var natural=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);natural.setPos(Vec3.atBottomCenterOf(inside));l.addFreshEntity(natural);
  float before=natural.getHealth();yes(h,natural.hurtServer(l,l.damageSources().magic(),5)&&natural.getHealth()<before,"Unattributed natural magic damage remains Vanilla");natural.discard();
 });h.succeed();}
 static void dispenser(GameTestHelper h){with(h,c->{
  var l=h.getLevel();var inside=ToolClaimTests.boundary(h);var owner=player(h,inside.east(8));
  yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(inside)),owner.getUUID(),100),"Dispenser fixture");
  for(var p:List.of(inside.west(),inside.east(3),inside.west(80))){
   var state=Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST);l.setBlock(p,state,3);var dispenser=(DispenserBlockEntity)l.getBlockEntity(p);dispenser.setItem(0,new ItemStack(Items.PAPER,3));
   state.tick(l,p,RandomSource.create(0));yes(h,dispenser.getItem(0).getCount()==(p.equals(inside.west())?3:2),"Actual dispenser plain ejection respects boundary and allows same-owner/wilderness");
   dispenser.setItem(0,new ItemStack(Items.WATER_BUCKET));l.setBlock(p.east(),Blocks.AIR.defaultBlockState(),3);state.tick(l,p,RandomSource.create(0));
   yes(h,p.equals(inside.west(80))?!l.getFluidState(p.east()).isEmpty():dispenser.getItem(0).is(Items.WATER_BUCKET)&&l.getFluidState(p.east()).isEmpty(),"Nontrivial dispenser behaviors remain conservatively unsupported near claims");
  }
 });h.succeed();}
 static void pickup(GameTestHelper h){with(h,c->{
  var l=h.getLevel();var pos=ToolClaimTests.boundary(h).east(3).above(90);var owner=player(h,pos);var other=player(h,pos);
  yes(h,c.create(new ClaimStore.Key(l.dimension().identifier().toString(),ChunkPos.pack(pos)),owner.getUUID(),100),"Pickup fixture");
  var drop=new net.minecraft.world.entity.item.ItemEntity(l,pos.getX()+.5,pos.getY(),pos.getZ()+.5,new ItemStack(Items.APPLE,3));drop.setNoPickUpDelay();l.addFreshEntity(drop);
  drop.playerTouch(other);yes(h,!drop.isRemoved()&&other.getInventory().countItem(Items.APPLE)==0,"Actual walking pickup denies stranger");
  drop.playerTouch(owner);yes(h,drop.isRemoved()&&owner.getInventory().countItem(Items.APPLE)==3,"Actual walking pickup permits owner");
 });h.succeed();}
 static void disabled(GameTestHelper h){with(h,ClaimConfig.DEFAULT,c->{
  var l=h.getLevel();var pos=ToolClaimTests.boundary(h).above(100);var cow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);cow.setPos(Vec3.atBottomCenterOf(pos));l.addFreshEntity(cow);
  yes(h,cow.hurtServer(l,l.damageSources().magic(),1),"Disabled actual damage hook preserves Vanilla");
  yes(h,cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100),null),"Disabled indirect effect preserves Vanilla");
  var from=new net.minecraft.world.SimpleContainer(1);var into=new net.minecraft.world.SimpleContainer(1);
  yes(h,HopperBlockEntity.addItem(from,into,new ItemStack(Items.APPLE),null).isEmpty(),"Disabled unknown-container transfer preserves Vanilla");
  l.setBlock(pos,Blocks.STONE.defaultBlockState(),3);new ServerExplosion(l,null,null,null,Vec3.atCenterOf(pos),4,false,Explosion.BlockInteraction.DESTROY).explode();
  yes(h,l.getBlockState(pos).isAir()&&!c.dataLoaded(),"Disabled actual explosion changes blocks without reading claim data");cow.discard();
 });h.succeed();}
}
