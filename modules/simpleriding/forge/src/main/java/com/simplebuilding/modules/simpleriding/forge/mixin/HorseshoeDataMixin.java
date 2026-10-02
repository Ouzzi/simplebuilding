package com.simplebuilding.modules.simpleriding.forge.mixin;

import com.simpleriding.Horseshoes;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Forge: the hoof code as synced entity data (same as Fabric; NeoForge uses a synced attachment). */
@Mixin(AbstractHorse.class)
public abstract class HorseshoeDataMixin implements Horseshoes.SyncedData {
 @Unique private static final EntityDataAccessor<Integer> SIMPLERIDING$SHOES=SynchedEntityData.defineId(AbstractHorse.class,EntityDataSerializers.INT);
 @Inject(method="defineSynchedData",at=@At("TAIL"))
 private void simpleriding$define(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(SIMPLERIDING$SHOES,0);}
 @Override public int simpleriding$syncedCode(){return ((AbstractHorse)(Object)this).getEntityData().get(SIMPLERIDING$SHOES);}
 @Override public void simpleriding$setSyncedCode(int value){((AbstractHorse)(Object)this).getEntityData().set(SIMPLERIDING$SHOES,value);}
}
