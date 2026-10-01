package com.simplebuilding.modules.simpletweaks.mixin.claims;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({net.minecraft.world.entity.item.ItemEntity.class,net.minecraft.world.entity.ExperienceOrb.class})
public abstract class ClaimPickupMixin {
 @Inject(method="playerTouch",at=@At("HEAD"),cancellable=true)
 private void claims$pickup(net.minecraft.world.entity.player.Player player,CallbackInfo ci){if(!Claims.action(player,(net.minecraft.world.entity.Entity)(Object)this))ci.cancel();}
}
