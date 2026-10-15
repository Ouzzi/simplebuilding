package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsRuntime;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A fishing hook spawned from a rod is a cast. */
@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;",
            at = @At("HEAD"))
    private static void simplestats$cast(Projectile projectile, ServerLevel level, ItemStack itemStack, Consumer<?> shoot, CallbackInfoReturnable<Projectile> cir) {
        if (projectile instanceof FishingHook hook && hook.getPlayerOwner() instanceof ServerPlayer player) StatsRuntime.onCast(player, itemStack);
    }
}
