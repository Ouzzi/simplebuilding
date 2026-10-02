package com.simpleriding.mixin;

import com.simpleriding.*;
import java.util.ArrayList;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Four hoof slots on every horse-like mount: storage, save data, drops, synced code and steering. */
@Mixin(AbstractHorse.class)
public abstract class HorseshoeHorseMixin implements HorseshoeHolder {
 @Unique private static final EntityDataAccessor<Integer> SIMPLERIDING$SHOES=SynchedEntityData.defineId(AbstractHorse.class,EntityDataSerializers.INT);
 @Unique private static final String SIMPLERIDING$KEY="simpleriding:horseshoes";
 @Unique private final Horseshoes.Container simpleriding$shoes=new Horseshoes.Container(this::simpleriding$sync);
 @Unique private double simpleriding$travel, simpleriding$lastX, simpleriding$lastZ;
 @Unique private boolean simpleriding$hasLast;

 @Unique private AbstractHorse simpleriding$self(){return (AbstractHorse)(Object)this;}
 @Inject(method="defineSynchedData",at=@At("TAIL"))
 private void simpleriding$define(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(SIMPLERIDING$SHOES,0);}

 @Override public SimpleContainer simpleriding$horseshoes(){return simpleriding$shoes;}
 @Override public int simpleriding$code(){return simpleriding$self().getEntityData().get(SIMPLERIDING$SHOES);}
 @Override public void simpleriding$sync(){
  var horse=simpleriding$self();
  if(horse.level()==null||horse.level().isClientSide())return;
  horse.getEntityData().set(SIMPLERIDING$SHOES,Horseshoes.syncedValue(horse));
 }
 @Override public double simpleriding$travel(){return simpleriding$travel;}
 @Override public void simpleriding$setTravel(double blocks){simpleriding$travel=Double.isFinite(blocks)?Math.max(0,blocks):0;}
 @Override public boolean simpleriding$hasLast(){return simpleriding$hasLast;}
 @Override public double simpleriding$lastX(){return simpleriding$lastX;}
 @Override public double simpleriding$lastZ(){return simpleriding$lastZ;}
 @Override public void simpleriding$setLast(double x,double z,boolean valid){simpleriding$lastX=x;simpleriding$lastZ=z;simpleriding$hasLast=valid;}

 @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
 private void simpleriding$save(ValueOutput output,CallbackInfo ci){
  if(simpleriding$shoes.isEmpty())return;
  var list=new ArrayList<ItemStack>();
  for(int i=0;i<Horseshoes.SLOTS;i++)list.add(simpleriding$shoes.getItem(i).copy());
  output.store(SIMPLERIDING$KEY,ItemStack.OPTIONAL_CODEC.listOf(),list);
 }
 @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
 private void simpleriding$load(ValueInput input,CallbackInfo ci){
  simpleriding$shoes.clearContent();
  input.read(SIMPLERIDING$KEY,ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list->{
   for(int i=0;i<Math.min(Horseshoes.SLOTS,list.size());i++)if(Horseshoes.isHorseshoe(list.get(i)))simpleriding$shoes.setItem(i,list.get(i).copyWithCount(1));
  });
  simpleriding$sync();
 }
 @Inject(method="dropEquipment",at=@At("TAIL"))
 private void simpleriding$drop(ServerLevel level,CallbackInfo ci){Horseshoes.drop(simpleriding$self(),level);}
 @Inject(method="getRiddenInput",at=@At("RETURN"),cancellable=true)
 private void simpleriding$handling(Player controller,Vec3 selfInput,CallbackInfoReturnable<Vec3> cir){
  cir.setReturnValue(Horseshoes.handling(simpleriding$self(),cir.getReturnValue()));
 }
}
