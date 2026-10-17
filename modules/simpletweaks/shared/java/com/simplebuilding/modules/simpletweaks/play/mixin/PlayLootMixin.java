package com.simplebuilding.modules.simpletweaks.play.mixin;

import com.simplebuilding.modules.simpletweaks.play.events.ServerEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** More loot event (T21): a mob killed by a player rolls its loot table again. */
@Mixin(LivingEntity.class)
public abstract class PlayLootMixin {
    @Shadow protected abstract void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled);

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At("RETURN"))
    private void simpletweaks$playMoreLoot(ServerLevel level, DamageSource source, boolean playerKilled, CallbackInfo ci) {
        int rolls = ServerEvents.extraLootRolls((LivingEntity) (Object) this, playerKilled);
        ServerEvents.extra(rolls, () -> dropFromLootTable(level, source, playerKilled));
    }
}
