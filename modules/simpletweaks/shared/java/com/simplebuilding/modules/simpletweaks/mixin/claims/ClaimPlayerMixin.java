package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Player.class)
public abstract class ClaimPlayerMixin {
 @Inject(method="interactOn",at=@At("HEAD"),cancellable=true)
 private void claims$interact(Entity target,InteractionHand hand,Vec3 location,CallbackInfoReturnable<InteractionResult> cir){
  if((Object)this instanceof ServerPlayer p && !Claims.allowEntity(p,target))cir.setReturnValue(InteractionResult.FAIL);
 }
 @Inject(method="attack",at=@At("HEAD"),cancellable=true)
 private void claims$attack(Entity target,CallbackInfo ci){if((Object)this instanceof ServerPlayer p && !Claims.allowEntity(p,target))ci.cancel();}
 @Inject(method="stabAttack",at=@At("HEAD"),cancellable=true)
 private void claims$stab(net.minecraft.world.entity.EquipmentSlot slot,Entity target,float damage,boolean knockback,boolean dismount,boolean pull,CallbackInfoReturnable<Boolean> cir){if((Object)this instanceof ServerPlayer p && !Claims.allowEntity(p,target))cir.setReturnValue(false);}
}
