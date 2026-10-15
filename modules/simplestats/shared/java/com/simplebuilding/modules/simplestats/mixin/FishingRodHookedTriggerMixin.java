package com.simplebuilding.modules.simplestats.mixin;

import com.simplebuilding.modules.simplestats.StatsRuntime;
import java.util.Collection;
import net.minecraft.advancements.triggers.FishingRodHookedTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The one place every catch passes through, with the real rod and the list of caught items. */
@Mixin(FishingRodHookedTrigger.class)
public abstract class FishingRodHookedTriggerMixin {
    @Inject(method = "trigger", at = @At("HEAD"))
    private void simplestats$caught(ServerPlayer player, ItemStack rod, FishingHook hook, Collection<ItemStack> items, CallbackInfo ci) {
        if (!items.isEmpty()) StatsRuntime.onCatch(player, rod, items);
    }
}
