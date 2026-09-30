package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BucketItem.class)
public abstract class ClaimBucketMixin {
 @Inject(method="emptyContents",at=@At("HEAD"),cancellable=true)
 private void claims$empty(LivingEntity actor,Level level,BlockPos pos,BlockHitResult hit,CallbackInfoReturnable<Boolean> cir){
  if(level instanceof ServerLevel server && (actor instanceof ServerPlayer p ? !Claims.allow(p,server,pos) : !Claims.allow(server,null,pos)))cir.setReturnValue(false);
 }
}
