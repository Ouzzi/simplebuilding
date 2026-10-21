package com.simplestorage.mixin;

import com.simplestorage.rare.RareShulkers;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A rare shulker calls its endermites once when a player comes near ({@link RareShulkers#tickEscort}). */
@Mixin(Shulker.class)
public abstract class ShulkerEscortMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplestorage$escort(CallbackInfo ci) {
        RareShulkers.tickEscort((Shulker) (Object) this);
    }
}
