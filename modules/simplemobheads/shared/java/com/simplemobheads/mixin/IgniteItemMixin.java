package com.simplemobheads.mixin;

import com.simplemobheads.ritual.HerobrineRitual;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Herobrine ritual trigger: a player's flint and steel or fire charge has just set fire (the result used the item). Only
 * here the pattern is checked - at that one spot, server side; fire from anything else never starts the ritual.
 */
@Mixin({FlintAndSteelItem.class, FireChargeItem.class})
public abstract class IgniteItemMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void simplemobheads$herobrineRitual(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player
                && cir.getReturnValue().consumesAction()) {
            HerobrineRitual.onIgnite(level, context.getClickedPos().relative(context.getClickedFace()), player);
        }
    }
}
