package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A thrown trident carries the item (and its statistics) with it; a throw counts on that carried item. */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void simplestats$thrown(Level level, LivingEntity owner, ItemStack tridentItem, CallbackInfo ci) {
        if (!level.isClientSide() && owner instanceof ServerPlayer player) StatsRuntime.onThrow(player, ((ThrownTrident) (Object) this).getPickupItemStackOrigin());
    }
}
