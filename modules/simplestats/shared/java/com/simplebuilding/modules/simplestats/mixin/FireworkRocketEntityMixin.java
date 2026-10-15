package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A rocket that boosts a gliding player. */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("TAIL"))
    private void simplestats$boost(Level level, ItemStack source, LivingEntity stuckTo, CallbackInfo ci) {
        if (!level.isClientSide() && stuckTo instanceof ServerPlayer player && player.isFallFlying()) StatsRuntime.onRocket(player);
    }
}
