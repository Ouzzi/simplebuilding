package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsRuntime;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bows and crossbows: arrows fired (before the arrows copy the weapon, so the item already carries its id). */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
    @Inject(method = "shoot", at = @At("HEAD"))
    private void simplestats$shot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> projectiles,
                                  float power, float uncertainty, boolean isCrit, @Nullable LivingEntity targetOverride, CallbackInfo ci) {
        if (shooter instanceof ServerPlayer player) StatsRuntime.onShoot(player, weapon, projectiles);
    }
}
