package com.simplebuilding.mixin;

import com.simplebuilding.util.RareShulkers;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ein seltener Shulker ruft einmalig seine Endermiten, wenn ein Spieler naht ({@link RareShulkers#tickEscort}). */
@Mixin(Shulker.class)
public abstract class ShulkerEscortMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void simplebuilding$escort(CallbackInfo ci) {
        RareShulkers.tickEscort((Shulker) (Object) this);
    }
}
