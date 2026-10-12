package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Every entity-attributed block destruction (Wither, Ravager, Silverfish, villagers, boats, mods) passes the claim check. */
@Mixin(Level.class)
public abstract class ClaimGriefMixin {
 @Inject(method="destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",at=@At("HEAD"),cancellable=true)
 private void claims$destroy(BlockPos pos,boolean drop,Entity breaker,int limit,CallbackInfoReturnable<Boolean> cir){
  if(breaker!=null&&(Object)this instanceof ServerLevel level&&!Claims.mob(breaker,level,pos))cir.setReturnValue(false);
 }
}
